package edu.ucla.belief;

import java.util.*;
import java.io.*;
import java.lang.reflect.Array;

import edu.ucla.structure.*;
import edu.ucla.util.UserObject;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.UserEnumProperty;
import edu.ucla.util.UserEnumValue;
import edu.ucla.util.InOutDegreeProperty;
import edu.ucla.util.InferenceValidProperty;
import edu.ucla.util.CPTShells;
//import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.io.NodeLinearTask;

import edu.ucla.belief.io.hugin.HuginNet;
import edu.ucla.belief.io.PropertySuperintendent;
import edu.ucla.belief.io.AbstractCopier;
import edu.ucla.belief.io.dsl.DSLNodeType;

/**
* Encapsulates information necessary to specify a belief network. A belief
* network consists of a Directed graph whose nodes are the Variables
* defining the structure of the network, and a mapping from Variable to
* Table representing the CPT of that variable.
*/
public class BeliefNetworkImpl implements BeliefNetwork, PropertySuperintendent
{
	public static boolean FLAG_DEBUG = Definitions.DEBUG;

	/** @since 20080219 */
	public FiniteVariable newFiniteVariable( Map properties ){
		properties = minimalProperties( properties );
		return new FiniteVariableImpl( (String) properties.get( KEY_HUGIN_ID ), (List) properties.get( KEY_HUGIN_STATES ) );
	}

	/** @since 20080219 */
	static public Map minimalProperties( Map properties ){
		if( properties == null ){ properties = new HashMap( 2 ); }

		Object value = null;
		try{
			if(        (value = properties.get( KEY_HUGIN_ID     )) == null ){ properties.put( KEY_HUGIN_ID,     "v_" +    Long.toString( new Random().nextLong()            ) ); }
			else if( ! (value instanceof String)                            ){ properties.put( KEY_HUGIN_ID, properties.get( KEY_HUGIN_ID ).toString() ); }
		}catch( Throwable thrown ){
			System.err.println( "warning: BeliefNetworkImpl.minimalProperties() caught " + thrown );
		}

		try{
			if(        (value = properties.get( KEY_HUGIN_STATES )) == null ){ properties.put( KEY_HUGIN_STATES, java.util.Arrays.asList( new String[]{ "state0", "state1" } ) ); }
			else if(    value.getClass().isArray()                          ){
				final List  list = new LinkedList();
				final int length = Array.getLength( value );
				for( int i=0; i<length; i++ ){ list.add( Array.get( value, i ) ); }
				properties.put( KEY_HUGIN_STATES, list );
			}
			else if( ! (value instanceof List)                              ){ throw new IllegalArgumentException( "cannot convert " + value.getClass().getName() + " to a List" ); }
		}catch( Throwable thrown ){
			System.err.println( "warning: BeliefNetworkImpl.minimalProperties() caught " + thrown );
		}

		return properties;
	}

	public UserObject userobject = null;
	public UserObject getUserObject()
	{
		return userobject;
	}
	public void setUserObject( UserObject obj )
	{
		userobject = obj;
	}

	public UserObject userobject2 = null;
	public UserObject getUserObject2()
	{
		return userobject2;
	}
	public void setUserObject2( UserObject obj )
	{
		userobject2 = obj;
	}

	/** The name value pairs.
		@since 021605 */
	private final Map params = new HashMap();

	/* @since 021605 */
	public Map getProperties(){
		return params;
	}

	/**
		@author Keith Cascio
		@since 112603
	*/
	public static void putAll( Map from, Map to )
	{
		Object keyFrom;
		Object valueFrom;
		Object valueTo;
		for( Iterator it = from.keySet().iterator(); it.hasNext(); )
		{
			keyFrom = it.next();
			valueFrom = valueTo = from.get( keyFrom );
			if( valueFrom instanceof UserObject ) valueTo = ((UserObject)valueFrom).onClone();
			to.put( keyFrom, valueTo );
		}
	}

	/**
		@author Keith Cascio
		@since 101003
	*/
	public boolean forAll( EnumProperty property, EnumValue value )
	{
		boolean isDefault = property.getDefault() == value;
		EnumValue temp;
		for( Iterator it = iterator(); it.hasNext(); )
		{
			temp = ((Variable)it.next()).getProperty( property );
			if( !(temp == null && isDefault) && temp != value ) return false;
		}

		return true;
	}

	/**
		@author Keith Cascio
		@since 101403
	*/
	public boolean thereExists( EnumProperty property, EnumValue value )
	{
		boolean isDefault = property.getDefault() == value;
		EnumValue temp;
		for( Iterator it = iterator(); it.hasNext(); )
		{
			temp = ((Variable)it.next()).getProperty( property );
			if( (temp == null && isDefault) || temp == value ) return true;
		}

		return false;
	}

	/**
		@author Keith Cascio
		@since 101003
	*/
	public Collection findVariables( EnumProperty property, EnumValue value )
	{
		LinkedList ret = new LinkedList();
		boolean isDefault = property.getDefault() == value;
		Variable var;
		EnumValue temp;
		for( Iterator it = iterator(); it.hasNext(); )
		{
			var = (Variable)it.next();
			temp = var.getProperty( property );
			if( temp == null && isDefault || temp == value ) ret.add( var );
		}
		return ret;
	}

	/**
		@author Keith Cascio
		@since 102003
	*/
	public void setAutoCPTInvalidation( boolean flag )
	{
		myFlagAutoCPTInvalidation = flag;
	}
	public boolean getAutoCPTInvalidation()
	{
		return myFlagAutoCPTInvalidation;
	}
	private boolean myFlagAutoCPTInvalidation = true;

	/** @since 010904 */
	public int countUserEnumProperties()
	{
		if( myPropertiesArray == null ) return (int)0;
		else return myPropertiesArray.length - VariableImpl.getNumProperties();
	}
	public EnumProperty[] propertiesAsArray()
	{
		if( myPropertiesArray == null ){
			myPropertiesArray = new EnumProperty[ VariableImpl.getNumProperties() ];
			VariableImpl.propertiesArrayCopy( myPropertiesArray );
		}
		return myPropertiesArray;
	}
	public void setUserEnumProperties( Collection userProperties )
	{
		int numSystemProps = VariableImpl.getNumProperties();
		myPropertiesArray = new EnumProperty[ numSystemProps + userProperties.size() ];
		//System.arraycopy( VariableImpl.PROPERTIES, 0, myPropertiesArray, 0, numSystemProps );
		VariableImpl.propertiesArrayCopy( myPropertiesArray );

		for( Iterator it = userProperties.iterator(); it.hasNext(); )
		{
			myPropertiesArray[numSystemProps++] = (EnumProperty)it.next();
		}
	}
	public Collection getUserEnumProperties()
	{
		EnumProperty[] array = propertiesAsArray();
		int numSystemProps = VariableImpl.getNumProperties();
		int numUserProps = array.length - numSystemProps;
		ArrayList ret = new ArrayList( numUserProps );
		for( int i=numSystemProps; i<array.length; i++ ) ret.add( array[i] );
		return ret;
	}

	/** @since 020904 */
	protected void setupUserEnumProperties( Collection oldProperties )
	{
		//System.out.println( "BeliefNetworkImpl.setupUserEnumProperties( "+oldProperties+" )" );
		Map propertiesOldToNew = new HashMap( oldProperties.size() );
		Collection newProperties = new ArrayList( oldProperties.size() );
		UserEnumProperty oldProp;
		UserEnumProperty newProp;
		for( Iterator propIt = oldProperties.iterator(); propIt.hasNext(); ){
			oldProp = (UserEnumProperty) propIt.next();
			newProp = new UserEnumProperty( oldProp );
			newProperties.add( newProp );
			propertiesOldToNew.put( oldProp, newProp );
		}
		this.setUserEnumProperties( newProperties );
		newProperties = null;

		Variable variable;
		EnumValue evOld;
		EnumValue evNew;
		for( Iterator varIt = iterator(); varIt.hasNext(); ){
			variable = (Variable) varIt.next();
			//System.out.println( variable.getID() + " has EnumProperties " + variable.getEnumProperties() );
			for( Iterator propIt = oldProperties.iterator(); propIt.hasNext(); )
			{
				oldProp = (UserEnumProperty) propIt.next();
				if( (evOld = variable.getProperty(oldProp)) != null )
				{
					//System.out.println( "\t " + variable.getID() + ".getProperty("+oldProp+") == " + evOld );
					newProp = (UserEnumProperty) propertiesOldToNew.get( oldProp );
					//System.out.println( "\t newProp == " + newProp + " " + Arrays.asList(newProp.valuesAsArray()) );
					evNew = newProp.forString( evOld.toString() );
					//System.out.println( "\t evNew == " + evNew );
					variable.delete( oldProp );
					variable.setProperty( newProp, evNew );
				}
			}
		}
		propertiesOldToNew = null;
	}

	private EnumProperty[] myPropertiesArray;

	/** @since 010804 */
	public boolean thereExistsModifiedUserEnumProperty()
	{
		if( myPropertiesArray == null ) return false;
		int numSystemProps = VariableImpl.getNumProperties();
		for( int i=numSystemProps; i<myPropertiesArray.length; i++ )
			if( myPropertiesArray[i].isModified() ) return true;

		return false;
	}

	/** @since 010804 */
	public void setUserEnumPropertiesModified( boolean flag )
	{
		if( myPropertiesArray == null ) return;
		int numSystemProps = VariableImpl.getNumProperties();
		for( int i=numSystemProps; i<myPropertiesArray.length; i++ )
			((UserEnumProperty)myPropertiesArray[i]).setModified( flag );
		return;
	}

	public void makeUserEnumProperties( Map inputParams )
	{
		//System.out.println( "BeliefNetworkImpl.makeUserEnumProperties() <- " + inputParams );
		if( inputParams.containsKey( PropertySuperintendent.KEY_USERPROPERTIES ) )
		{
			String strPropertyIDs = (String) inputParams.get( PropertySuperintendent.KEY_USERPROPERTIES );
			if( strPropertyIDs.length() > 0 )
			{
				Collection properties = new LinkedList();
				String id;
				String name;
				UserEnumProperty property;
				String data;
				String strDefault;
				String strFlagFlag;
				EnumValue[] array;
				EnumValue valueDefault = null;
				String temp = null;
				int lenData;
				for( StringTokenizer toker = new StringTokenizer( strPropertyIDs, "," ); toker.hasMoreTokens(); )
				{
					id = toker.nextToken();
					property = new UserEnumProperty();
					property.setID( id );
					data = (String) inputParams.get( id );
					if( data == null )
					{
						System.err.println( "Warning: missing property definition: " + id );
						continue;
					}

					StringTokenizer dataTokenizer = new StringTokenizer( data, "," );
					lenData = dataTokenizer.countTokens();
					if( lenData < 5 )
					{
						System.err.println( "Warning: invalid property definition found." );
						continue;
					}

					name = dataTokenizer.nextToken();
					strDefault = dataTokenizer.nextToken();
					strFlagFlag = dataTokenizer.nextToken();
					array = new EnumValue[ lenData - 3 ];
					for( int i=0; i<array.length && dataTokenizer.hasMoreTokens(); i++ )
					{
						temp = dataTokenizer.nextToken();
						array[i] = new UserEnumValue( temp, property );
						if( strDefault.equals( temp ) ) valueDefault = array[i];
					}
					property.setName( name );
					property.setValues( array );
					if( valueDefault != null ) property.setDefault( valueDefault );
					property.setIsFlag( strFlagFlag.equals( PropertySuperintendent.VALUE_TRUE ) );
					property.setModified( false );

					properties.add( property );
					inputParams.remove( id );
				}
				if( !properties.isEmpty() ) setUserEnumProperties( properties );
			}

			inputParams.remove( PropertySuperintendent.KEY_USERPROPERTIES );
		}
		//System.out.println( "(inputParams/post)"+System.identityHashCode(inputParams)+": " + inputParams );
	}

	protected DirectedGraph structure = null;

	public final boolean add( Object obj )
	{
		return addVertex( obj );
	}
	public final boolean remove( Object obj )
	{
		return removeVertex( obj );
	}
	public boolean retainAll(final java.util.Collection p1)
	{
		return structure.retainAll(p1);
	}
	public java.lang.Object[] toArray(java.lang.Object[] p)
	{
		return structure.toArray(p);
	}
	public java.lang.Object[] toArray()
	{
		return structure.toArray();
	}
	public boolean removeAll(final java.util.Collection p)
	{
		return structure.removeAll(p);
	}
	public void clear()
	{
		if( myEvidenceController != null ) myEvidenceController.resetEvidence();
		if( myMapIDsToVariables != null ) myMapIDsToVariables.clear();
		//if( tables != null ) tables.clear();
		if( structure != null ) structure.clear();
	}
	public int hashCode()
	{
		return structure.hashCode();
	}
	public boolean addAll(final java.util.Collection p)
	{
		return structure.addAll(p);
	}
	public boolean containsAll(final java.util.Collection p)
	{
		return structure.containsAll(p);
	}
	public boolean equals(final java.lang.Object p)
	{
		return structure.equals(p);
	}
	public java.util.Iterator iterator()
	{
		return structure.iterator();
	}
	public boolean isEmpty()
	{
		return structure.isEmpty();
	}
	public List topologicalOrder()
	{
		return structure.topologicalOrder();
	}
	public void replaceVertex( Object oldVertex, Object newVertex )
	{
		if( mayContain( oldVertex ) && mayContain( newVertex ) ) structure.replaceVertex( (Variable)oldVertex, (Variable)newVertex );
		else throw new IllegalArgumentException( "BeliefNetworkImpl may contain only Variables.  Attempt to replace " + oldVertex.getClass().getName() + " with " + newVertex.getClass().getName() );
	}
	public void replaceVertices( Map verticesOldToNew, NodeLinearTask task ){
		structure.replaceVertices( verticesOldToNew, task );
	}
	public boolean maintainsAcyclicity( Object vertex1, Object vertex2 )
	{
		return structure.maintainsAcyclicity(vertex1,vertex2);
	}
	public Set vertices()
	{
		return structure.vertices();
	}
	public Set inComing(Object vertex)
	{
		return structure.inComing(vertex);
	}
	public Set outGoing(Object vertex)
	{
		return structure.outGoing(vertex);
	}
	public int degree(Object vertex)
	{
		return structure.degree(vertex);
	}
	public int inDegree(Object vertex)
	{
		return structure.inDegree(vertex);
	}
	public int outDegree(Object vertex)
	{
		return structure.outDegree(vertex);
	}
	public boolean containsEdge(Object vertex1, Object vertex2)
	{
		return structure.containsEdge(vertex1,vertex2);
	}
	public boolean contains(Object vertex)
	{
		return structure.contains(vertex);
	}
	public int size()
	{
		return structure.size();
	}
	public int numEdges()
	{
		return structure.numEdges();
	}
	public boolean isAcyclic()
	{
		return structure.isAcyclic();
	}
	public boolean isWeaklyConnected()
	{
		return structure.isWeaklyConnected();
	}
	public boolean isWeaklyConnected(Object vertex1, Object vertex2)
	{
		return structure.isWeaklyConnected(vertex1,vertex2);
	}
	public boolean hasPath(Object vertex1, Object vertex2)
	{
		return structure.hasPath(vertex1,vertex2);
	}
	public boolean isSinglyConnected()
	{
		return structure.isSinglyConnected();
	}
	public boolean addVertex(Object vertex)
	{
		if( mayContain( vertex ) ) return addVariable( (Variable)vertex, true );
		else throw new IllegalArgumentException( "Only Variables can be added and removed from BeliefNetworkImpl.  Attempt to add " + vertex.getClass().getName() );
	}
	public boolean removeVertex(Object vertex)
	{
		if( mayContain( vertex ) ) return removeVariable( (Variable)vertex );
		else throw new IllegalArgumentException( "Only Variables can be added and removed from BeliefNetworkImpl.  Attempt to remove " + vertex.getClass().getName() );
	}
	public boolean addEdge(Object vertex1, Object vertex2)
	{
		if( mayContain( vertex1 ) && mayContain( vertex2 ) ) return addEdge( (Variable)vertex1, (Variable)vertex2, true );
		else throw new IllegalArgumentException( "BeliefNetworkImpl may contain only Variables.  Attempt to add edge between " + vertex1.getClass().getName() + " and " + vertex2.getClass().getName() );
	}
	public boolean removeEdge(Object vertex1, Object vertex2)
	{
		if( mayContain( vertex1 ) && mayContain( vertex2 ) ) return removeEdge( (Variable)vertex1, (Variable)vertex2 );
		else throw new IllegalArgumentException( "BeliefNetworkImpl may contain only Variables.  Attempt to remove edge between " + vertex1.getClass().getName() + " and " + vertex2.getClass().getName() );
	}

	public boolean mayContain( Object obj )
	{
		return obj instanceof Variable;
	}

	/**
		@author Keith Cascio
		@since 072903
	*/
	public void setScalars( double scalar )
	{
		FiniteVariable next;
		CPTShell shell;
		for( Iterator it = iterator(); it.hasNext(); )
		{
			next = (FiniteVariable) it.next();
			shell = next.getCPTShell( DSLNodeType.CPT );
			if( shell instanceof TableShell ) next.setCPTShell( new TableShell( new TableScaled( shell.getCPT(), scalar ) ) );
		}
	}

	//protected Map tables;
	protected Map myMapIDsToVariables;

	public void identifierChanged( String oldID, Variable var )
	{
		if( myMapIDsToVariables != null && myMapIDsToVariables.containsKey( oldID ) && myMapIDsToVariables.get( oldID ) == var )
		{
			myMapIDsToVariables.remove( oldID );
			myMapIDsToVariables.put( var.getID(), var );
		}
	}

	/**
		@author Keith Cascio
		@since 071902
	*/
	public EvidenceController getEvidenceController()
	{
		return myEvidenceController;
	}
	public void setEvidenceController( EvidenceController EC )
	{
		myEvidenceController = EC;
		EC.setBeliefNetwork( this );
	}
	private EvidenceController myEvidenceController = null;

	/**
	* Creates an empty network. The structure and tables must be supplied before
	* anything useful can be done.
	*/
	public BeliefNetworkImpl( boolean construct )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "\n\n" + this.getClass().getName() + "() -> BeliefNetworkImpl( "+construct+" )" );

		if( construct )
		{
			structure = new HashDirectedGraph();
			//tables = new HashMap();
		}
		init();
	}

	/**
		@author Keith Cascio
		@since 110702
	*/
	public BeliefNetworkImpl( BeliefNetworkImpl toCopy )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "\n\n" + this.getClass().getName() + "() -> BeliefNetworkImpl( "+toCopy.getClass().getName()+" )" );

		this.structure = (DirectedGraph) toCopy.structure.clone();
		this.userobject = (toCopy.userobject == null ) ? null : toCopy.userobject.onClone();
		this.userobject2 = (toCopy.userobject2 == null ) ? null : toCopy.userobject2.onClone();
		this.myFlagDomainCardinalityValid = toCopy.myFlagDomainCardinalityValid;
		this.myMaxDomainCardinality = toCopy.myMaxDomainCardinality;
		this.myMinDomainCardinality = toCopy.myMinDomainCardinality;
		this.myFlagTheoreticalCPTSizeValid = toCopy.myFlagTheoreticalCPTSizeValid;
		this.myMaxTheoreticalCPTSize = toCopy.myMaxTheoreticalCPTSize;
		this.myMinTheoreticalCPTSize = toCopy.myMinTheoreticalCPTSize;
		//this.setupUserEnumProperties( toCopy.getUserEnumProperties() );

		init();
	}

	public BeliefNetworkImpl()
	{
		this( true );
	}

	/**
	* Creates a BeliefNetwork by inducing the graph structure from
	* a mapping from the Variable to its CPT.
	*/
	public BeliefNetworkImpl( Map mapVariablesToPotentials )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "\n\n" + this.getClass().getName() + "() -> BeliefNetworkImpl(Map)" );
		//this.structure = inducedGraph( mapVariablesToPotentials );
		//this.tables = mapVariablesToPotentials;
		induceGraph( mapVariablesToPotentials );
		init();
	}

	/**
	* Creates a belief network.
	* @param structure A directed graph specifying the structure of the
	* network.
	* @param tables - A mapping from the Variable to its CPT.
	*/
	public BeliefNetworkImpl( DirectedGraph structure, Map tables )
	{
		this( structure );
		//this.tables = tables;

		FiniteVariable fVar = null;
		for( Iterator it = structure.iterator(); it.hasNext(); )
		{
			fVar = (FiniteVariable) it.next();
			fVar.setCPTShell( new TableShell( (Table) tables.get( fVar ) ) );
		}
	}

	public BeliefNetworkImpl( DirectedGraph structure )
	{
		this.structure = structure;
		init();
	}

	protected final void init()
	{
		//System.out.println( "(BeliefNetworkImpl)"+getClass().getName()+".init()" + hashCode() );
		if( myEvidenceController == null ) myEvidenceController = new EvidenceController( this );
	}

	/**
		@author Keith Cascio
		@since 100402
	*/
	public void replaceAllPotentials( Map mapVariablesToPotentials )
	{
		if( mapVariablesToPotentials.keySet().containsAll( this ) )
		{
			FiniteVariable fVar = null;
			for( Iterator it = iterator(); it.hasNext(); )
			{
				fVar = (FiniteVariable) it.next();
				fVar.setCPTShell( new TableShell( (Table) mapVariablesToPotentials.get( fVar ) ) );
			}
		}
		else throw new IllegalArgumentException( "BeliefNetworkImpl.replacePotentials() called with an incomplete Map." );
	}

	/** @since 20030225 */
	public void cloneAllCPTShells()
	{
		new CPTShells( "BeliefNetworkImpl.cloneAllCPTShells()" ){
			public void doTask( FiniteVariable var, DSLNodeType type, CPTShell shell ){
				if( shell != null )
					var.setCPTShell( type, (CPTShell) shell.clone() );
			}
		}.forAllFiniteVariables( iterator() );
	}

	/** @since 20021015 */
	public boolean insertState( final FiniteVariable var, final int index, Object instance )
	{
		if( var.insert( index, instance ) )
		{
			new CPTShells( "BeliefNetworkImpl.insertState( "+var.getID()+", "+index+", '"+instance+"' )" ){
				public void doTask( FiniteVariable shellsvar, DSLNodeType type, CPTShell shell ){
					if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( shellsvar + ".parentStateInserted( "+var+", "+index+" )" );
					if( shell != null )
						shell.parentStateInserted( var, index );
				}
			}.forAllFiniteVariables( outGoing( var ).iterator() );

			//int newSize = var.size();
			//if( newSize > myMaxDomainCardinality ) myMaxDomainCardinality = newSize;
			//else myFlagDomainCardinalityValid = false;
			myFlagTheoreticalCPTSizeValid = myFlagDomainCardinalityValid = false;

			return true;
		}
		else return false;
	}

	/** @since 20021015 */
	public Object removeState( final FiniteVariable var, final int index )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "BeliefNetworkImpl.removeState( "+var+", "+index+" )" );

		Object ret = var.remove( index );
		if( ret != null ){
			new CPTShells( "BeliefNetworkImpl.removeState( "+var.getID()+", "+index+" )" ){
				public void doTask( FiniteVariable shellsvar, DSLNodeType type, CPTShell shell ){
					if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( shellsvar + ".parentStateRemoved( "+var+", "+index+" )" );
					if( shell != null )
						shell.parentStateRemoved( var, index );
				}
			}.forAllFiniteVariables( outGoing( var ).iterator() );
		}

		//int newSize = var.size();
		//if( newSize < myMinDomainCardinality ) myMinDomainCardinality = newSize;
		//else myFlagDomainCardinalityValid = false;
		myFlagTheoreticalCPTSizeValid = myFlagDomainCardinalityValid = false;

		return ret;
	}

	/** @since 20020524 */
	public String toString(){
		StringBuffer buff = new StringBuffer( 0x100 );
		buff.append( "BeliefNetworkImpl structure:" );
		buff.append( structure );
		return buff.toString();
	}

	/** @since 021804 */
	public Copier getCopier()
	{
		return AbstractCopier.STANDARD;
	}

	/**
		As of 061002, this method simply calls deepClone().
		@author Keith Cascio
		@since 052402
	*/
	public final Object clone()
	{
		return deepClone();
		//return shallowClone();
	}

	/**
		Call this method to perform a deep clone on a BeliefNetworkImpl (i.e., will
		clone the structure, Tables and all Variable objects. )

		@author Keith Cascio
		@since 060302
	*/
	public final BeliefNetwork deepClone()
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "\n\nBeliefNetworkImpl.deepClone() - \ncurrent: " + this );

		Variable oldVar = null;
		Variable newVar = null;
		Map oldToNew = new HashMap();

		for( Iterator it = vertices().iterator(); it.hasNext(); )
		{
			oldVar = (Variable) it.next();
			newVar = (Variable) oldVar.clone();
			oldToNew.put( oldVar, newVar );
		}

		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "seeds: " + oldToNew.values() );

		BeliefNetwork ret = seededClone( oldToNew );
		if( ret.getEvidenceController() == null )
		{
			ret.setEvidenceController( (EvidenceController) myEvidenceController.clone() );
		}

		if( ret instanceof BeliefNetworkImpl ) ((BeliefNetworkImpl)ret).setupUserEnumProperties( this.getUserEnumProperties() );

		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "deepClone() returning "+  ret.getClass().getName() +" with: " + ret.vertices() );

		return ret;
	}

	/**
		<p>
		Call this method to clone a BeliefNetworkImpl when for whatever
		reason you have already cloned all the Variables it contains,
		and you have a mapping from the Variables it contains to
		Variables that you would like the cloned BeliefNetworkImpl to
		contain.
		<p>
		NOTE: If the BeliefNetworkImpl points to a HuginNet "userobject", this method will
		clone that HuginNet also.  You are not resposible for cloning the HuginNet yourself
		if you call this method.
		<p>
		@param variablesOldToNew A mapping from variables this BeliefNetworkImpl contains
				to the new ones you would like the cloned BeliefNetworkImpl to contain.
		@author Keith Cascio
		@since 061002
	*/
	public BeliefNetwork seededClone( Map variablesOldToNew )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "BeliefNetworkImpl.seededClone()" );

		if( !this.getClass().equals( BeliefNetworkImpl.class ) )
		{
			throw new RuntimeException( "Sub-class " + this.getClass().getName() + " called non-overridden version of BeliefNetworkImpl.seededClone()" );
		}

		DirectedGraph newStructure = (DirectedGraph) structure.clone();
		//Map newTables = new HashMap( tables );

		/*
		Map newTables = new HashMap();

		Variable oldVar = null;
		Variable newVar = null;
		Table oldTable = null;
		Table newTable = null;

		for( Iterator it = vertices().iterator(); it.hasNext(); )
		{
			oldVar = (Variable) it.next();
			newVar = (Variable) variablesOldToNew.get( oldVar );
			if( newVar == null )
			{
				throw new RuntimeException( "BeliefNetworkImpl.seededClone() called with incomplete Map: missing variable " + oldVar );
			}
			newStructure.replaceVertex( oldVar, newVar );
		}

		for( Iterator it = variablesOldToNew.keySet().iterator(); it.hasNext(); )
		{
			oldVar = (Variable) it.next();
			oldTable = (Table) tables.get( oldVar );
			newTable = (Table) oldTable.clone();
			newTable.replaceVariables( variablesOldToNew );
			newTables.put( variablesOldToNew.get( oldVar ), newTable );
		}
		*/

		BeliefNetworkImpl ret = new BeliefNetworkImpl( newStructure );//, newTables );

		ret.replaceVariables( variablesOldToNew );
		ret.userobject = (this.userobject == null ) ? null : this.userobject.onClone();
		ret.userobject2 = (this.userobject2 == null ) ? null : this.userobject2.onClone();
		ret.myFlagDomainCardinalityValid = this.myFlagDomainCardinalityValid;
		ret.myMaxDomainCardinality = this.myMaxDomainCardinality;
		ret.myMinDomainCardinality = this.myMinDomainCardinality;
		ret.myFlagTheoreticalCPTSizeValid = this.myFlagTheoreticalCPTSizeValid;
		ret.myMaxTheoreticalCPTSize = this.myMaxTheoreticalCPTSize;
		ret.myMinTheoreticalCPTSize = this.myMinTheoreticalCPTSize;

		return ret;
	}

	/** @author Keith Cascio
		@since 20021001 */
	public void replaceVariables( final Map variablesOldToNew ){
		BeliefNetworkImpl.this.replaceVariables( variablesOldToNew, (NodeLinearTask) null );
	}

	/** @author keith cascio
		@since 20060519 */
	public void replaceVariables( final Map variablesOldToNew, NodeLinearTask task )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "BeliefNetworkImpl.replaceVariables( "+variablesOldToNew+" )" );

		myMapIDsToVariables = null;

		// uncomment to profile HashDirectedGraph ...
		//if( complete ){//profile script is designed to work with complete data
		//	((HashDirectedGraph)structure).runProfileScript( variablesOldToNew, task );
		//	Thread.currentThread().interrupt();
		//	if( Thread.currentThread().isInterrupted() ) return;
		//}
		// ... uncomment to profile HashDirectedGraph

		structure.replaceVertices( variablesOldToNew, task );
		if( Thread.currentThread().isInterrupted() ){
			try{ BeliefNetworkImpl.this.clear(); }
			catch( Exception exception ){
				System.err.println( "warning: BeliefNetworkImpl.replaceVariables() interrupted but failed emergency cleanup" );
			}
			return;
		}

		new CPTShells( "BeliefNetworkImpl.replaceVariables()" ){
			public void doTask( FiniteVariable shellsvar, DSLNodeType type, CPTShell shell ){
				if( shell != null )
					shell.replaceVariables( variablesOldToNew, false );
			}
		}.forAllFiniteVariables( iterator() );

		myEvidenceController.replaceVariables( variablesOldToNew );
	}

	/**
		@author Keith Cascio
		@since 060402
	*/
	public BeliefNetwork shallowClone()
	{
		if( !this.getClass().equals( BeliefNetworkImpl.class ) )
		{
			System.err.println( "Warning: sub-class " + this.getClass().getName() + " calling non-overridden version of BeliefNetworkImpl.shallowClone()." );
		}

		/*
		Map newTables = new HashMap();
		Variable varTemp = null;
		Table tableTemp = null;
		for( Iterator it = tables.keySet().iterator(); it.hasNext(); )
		{
			varTemp = (Variable) it.next();
			tableTemp = (Table) tables.get( varTemp );
			newTables.put( varTemp, (Table) tableTemp.clone() );
		}

		BeliefNetworkImpl ret = new BeliefNetworkImpl( (DirectedGraph) structure.clone(), newTables );
		ret.userobject = this.userobject;
		return ret;
		*/
		return null;
	}

	/** @since 20091124 */
	protected Collection myAuditors;
	/** @since 20091124 */
	public boolean    addAuditor( Auditor auditor ){
		if( myAuditors == null ){ myAuditors = new edu.ucla.util.WeakLinkedList(); }
		else if( myAuditors.contains( auditor ) ){ return false; }
		return myAuditors.add( auditor );
	}
	/** @since 20091124 */
	public boolean removeAuditor( Auditor auditor ){
		if( myAuditors == null ){ return false; }
		return myAuditors.remove( auditor );
	}
	/** @since 20091124 */
	public BeliefNetwork fireAudit( Variable from, Variable to, Collection targets, Auditor.Deed deed ){
		String errmsg = null;
		if( myAuditors == null ){ return this; }
		try{
			Auditor  auditor = null;
			for( Iterator it = myAuditors.iterator(); it.hasNext(); ){
				if(      (auditor =  (Auditor) it.next()) == null ){ it.remove(); }
				else if( (errmsg  =   auditor.audit( this, from, to, targets, deed )) != null ){ break; }
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: BeliefNetworkImpl.\""+deed.intention.verb+" "+deed.unit.noun+"\" audit caught " + thrown );
		}
		if( errmsg != null ){ throw new IllegalArgumentException( errmsg ); }
		return this;
	}

	/**
	 * Adds an edge to the belief network. Both variables must already
	 * be part of the network, and it must leave the graph acyclic.
	 * The CPT will be expanded to include the new parent, and the values
	 * set will be independant of it. The relations for the other parents
	 * will remain as they were before.
	 */
	public boolean addEdge( final Variable from, Variable to, boolean expandCPT )
	{
		String errmsg = null;
		if(    ! structure.contains(     from ) || ! structure.contains( to ) ){ errmsg = "Does not contain those variables"; }
		else if( structure.containsEdge( from,                           to ) ){ errmsg = "Already contains edge"; }
		else{ fireAudit( from, to, null, Auditor.Deed.CREATE_EDGE ); }
		if( errmsg != null ){ throw new IllegalArgumentException( errmsg ); }

		if( structure.addEdge(from, to) )
		{
			if( to instanceof FiniteVariable ) recordTheoreticalCPTSize( (FiniteVariable)to );

			InOutDegreeProperty.setValue( from, this );
			InOutDegreeProperty.setValue( to, this );

			if( expandCPT && to instanceof FiniteVariable && from instanceof FiniteVariable )
			{
				new CPTShells( "BeliefNetworkImpl.addEdge( "+from.getID()+", "+to.getID()+" ) expanding from.getID()" ){
					public void doTask( FiniteVariable shellsvar, DSLNodeType type, CPTShell shell ){
						if( shell != null )
							shell.expand( (FiniteVariable)from );
					}
				}.forAllDSLNodeTypes( (FiniteVariable)to );

				if( myFlagAutoCPTInvalidation ) to.setProperty( InferenceValidProperty.PROPERTY, InferenceValidProperty.PROPERTY.FALSE );
			}
			return true;
		}
		else return false;
	}

//	/**
//	 * Removes an edge from the network. The resulting CPT for the variable to
//	 * is formed from taking the original CPT and summing out the from variable.
//	 */
//	public boolean removeEdgeSpecial(Variable from, Variable to)
//	{
//		if (!structure.containsEdge(from, to)) {
//			throw new IllegalArgumentException("Doesn't contain that edge");
//		}
//		if( structure.removeEdge(from, to) )
//		{
//			/*
//			Table t = forget(getCPT(to), from);
//			double[] vals = t.data();
//			int vsize = ((FiniteVariable) from).size();
//			for (int i = 0; i < vals.length; i++) {
//				vals[i] /= vsize;
//			}
//			tables.put(to, t);
//			*/
//			if( to instanceof FiniteVariable )
//			{
//				CPTShell shell = ((FiniteVariable)to).getCPTShell( ((FiniteVariable)to).getDSLNodeType() );
//				shell.multiplyInto( ((FiniteVariable)from).getCPTShell( ((FiniteVariable)from).getDSLNodeType() ).getCPT());
//				shell.forgetNoScale( from );
//			}
//			return true;
//		}
//		else {
//			//System.out.println("could not remove edge " + from + " -> " + to);
//			return false;
//		}
//
//	}


	/** Removes an edge from the network. The resulting CPT for the variable to
		is formed from taking the original CPT and summing out the from variable. */
	public boolean removeEdge( final Variable from, Variable to ){
		return this.removeEdge( from, to, true );
	}

	public boolean removeEdge( final Variable from, final Variable to, boolean forget )
	{
		if (!structure.containsEdge(from, to)) {
			throw new IllegalArgumentException("Doesn't contain that edge");
		}

		fireAudit( from, to, null, Auditor.Deed.DROP_EDGE );

		if( structure.removeEdge(from, to) )
		{
			InOutDegreeProperty.setValue( from, this );
			InOutDegreeProperty.setValue( to, this );

			if( forget && (to instanceof FiniteVariable) )
			{
				new CPTShells( "BeliefNetworkImpl.addEdge( "+from.getID()+", "+to.getID()+" ) forgetting from.getID()" ){
					public void doTask( FiniteVariable shellsvar, DSLNodeType type, CPTShell shell ){
						if( shell != null )
							shell.forget( from );
					}
				}.forAllDSLNodeTypes( (FiniteVariable)to );

				recordTheoreticalCPTSize( (FiniteVariable)to );
			}
			return true;
		}
		else return false;
	}

	/** Removes an edge from the network. */
	public boolean removeEdgeNoCPTChanges( Variable from, Variable to ){
		return this.removeEdge( from, to, false );
	}

	/**
	 * Adds a new node to the graph, with no parents. The CPT
	 * created will be uniform.
	 */
	public boolean addVariable( Variable newNode, boolean createCPT )
	{
		if( !mayContain( newNode ) ) throw new IllegalArgumentException( "Attempt to add unsupported type to BeliefNetwork " + newNode.getClass().getName() );
		if( structure.addVertex(newNode) )
		{
			if( newNode instanceof FiniteVariable )
			{
				FiniteVariable var = (FiniteVariable) newNode;

				int varsize = var.size();
				myMaxDomainCardinality = Math.max( myMaxDomainCardinality, varsize );
				myMinDomainCardinality = Math.min( myMinDomainCardinality, varsize );
				if( myMinDomainCardinality == (int)0 ) myMinDomainCardinality = varsize;
				recordTheoreticalCPTSize( var );

				if( createCPT )
				{
					double[] vals = new double[var.size()];
					java.util.Arrays.fill(vals, 1.0 / vals.length);
					Table t = new Table( new ArrayList(Collections.singleton(var)),vals );
					var.setCPTShell( new TableShell( t ) );
					if( myFlagAutoCPTInvalidation ) var.setProperty( InferenceValidProperty.PROPERTY, InferenceValidProperty.PROPERTY.FALSE );
				}
			}

			if( myMapIDsToVariables != null ){
				myMapIDsToVariables.put( newNode.getID(), newNode );
			}

			return true;
		}
		else return false;
	}

	protected boolean myFlagDomainCardinalityValid = true;
	protected int myMaxDomainCardinality = (int)0;
	protected int myMinDomainCardinality = (int)0;
	private boolean myFlagTheoreticalCPTSizeValid = true;
	private int myMaxTheoreticalCPTSize = (int)0;
	private int myMinTheoreticalCPTSize = (int)0;

	/** @author Keith Cascio
		@since 120704 */
	public int getMaxTheoreticalCPTSize()
	{
		if( !myFlagTheoreticalCPTSizeValid ) recalculateTheoreticalCPTSize();
		return myMaxTheoreticalCPTSize;
	}

	/** @author Keith Cascio
		@since 120704 */
	public int getMinTheoreticalCPTSize()
	{
		if( !myFlagTheoreticalCPTSizeValid ) recalculateTheoreticalCPTSize();
		return myMinTheoreticalCPTSize;
	}

	/** @author Keith Cascio
		@since 120704 */
	public int getTheoreticalCPTSize( FiniteVariable fVar ){
		int theoretical = fVar.size();
		Set parents = inComing( fVar );
		for( Iterator parentsit = parents.iterator(); parentsit.hasNext(); ){
			theoretical *= ((FiniteVariable)parentsit.next()).size();
		}
		return theoretical;
	}

	/** @author Keith Cascio
		@since 120704 */
	private void recordTheoreticalCPTSize( FiniteVariable var ){
		int theoretical = getTheoreticalCPTSize( var );
		myMaxTheoreticalCPTSize = Math.max( myMaxTheoreticalCPTSize, theoretical );
		myMinTheoreticalCPTSize = Math.min( myMinTheoreticalCPTSize, theoretical );
		if( myMinTheoreticalCPTSize == (int)0 ) myMinTheoreticalCPTSize = theoretical;
	}

	/** @author Keith Cascio
		@since 120704 */
	protected void recalculateTheoreticalCPTSize()
	{
		if( isEmpty() ) myMinTheoreticalCPTSize = myMaxTheoreticalCPTSize = (int)0;
		else
		{
			myMaxTheoreticalCPTSize = Integer.MIN_VALUE;
			myMinTheoreticalCPTSize = Integer.MAX_VALUE;

			int current = 0;
			for( Iterator it = iterator(); it.hasNext(); )
			{
				current = getTheoreticalCPTSize( (FiniteVariable) it.next() );
				myMaxTheoreticalCPTSize = Math.max( myMaxTheoreticalCPTSize, current );
				myMinTheoreticalCPTSize = Math.min( myMinTheoreticalCPTSize, current );
			}
		}

		myFlagTheoreticalCPTSizeValid = true;
	}

	/** @author Keith Cascio
		@since 102902 */
	public int getMaxDomainCardinality()
	{
		if( !myFlagDomainCardinalityValid ) recalculateDomainCardinality();
		return myMaxDomainCardinality;
	}

	/** @author Keith Cascio
		@since 102902 */
	public int getMinDomainCardinality()
	{
		if( !myFlagDomainCardinalityValid ) recalculateDomainCardinality();
		return myMinDomainCardinality;
	}

	/** @author Keith Cascio
		@since 102902 */
	protected void recalculateDomainCardinality()
	{
		if( isEmpty() ) myMinDomainCardinality = myMaxDomainCardinality = (int)0;
		else
		{
			myMaxDomainCardinality = Integer.MIN_VALUE;
			myMinDomainCardinality = Integer.MAX_VALUE;

			FiniteVariable fVar = null;
			for( Iterator it = iterator(); it.hasNext(); )
			{
				fVar = (FiniteVariable) it.next();
				myMaxDomainCardinality = Math.max( myMaxDomainCardinality, fVar.size() );
				myMinDomainCardinality = Math.min( myMaxDomainCardinality, fVar.size() );
			}
		}

		myFlagDomainCardinalityValid = true;
	}


//	/**
//	* Removes a variable from the network. Any children of that variable
//	* are first disconnected by calling removeEdgeSpecial(var,child).
//	*/
//	public boolean removeVariableSpecial( Variable var )
//	{
//		if( !mayContain( var ) ) throw new IllegalArgumentException( "Attempt to add unsupported type to BeliefNetwork " + var.getClass().getName() );
//
//		myEvidenceController.removeVariable( var );
//		Set s = new HashSet(structure.outGoing(var));
//		for (Iterator iter = s.iterator(); iter.hasNext();) {
//			removeEdgeSpecial(var, (Variable) iter.next());
//		}
//		//tables.remove(var);
//		if(myMapIDsToVariables!=null) myMapIDsToVariables.remove(var.getID());
//
//		if( structure.removeVertex(var) )
//		{
//			int card = ((FiniteVariable) var).size();
//			if( card == myMinDomainCardinality || card == myMaxDomainCardinality ) myFlagDomainCardinalityValid = false;
//			return true;
//		}
//		else return false;
//	}

	/**
	* Removes a variable from the network. Any children of that variable
	* are first disconnected by calling removeEdge(var,child).
	*/
	public boolean removeVariable( Variable var )
	{
		if( !mayContain( var ) ) throw new IllegalArgumentException( "Attempt to remove unsupported type from BeliefNetwork " + var.getClass().getName() );

		fireAudit( var, null, null, Auditor.Deed.DROP_NODE );

		myEvidenceController.removeVariable( var );
		if(myMapIDsToVariables!=null) myMapIDsToVariables.remove(var.getID());

		Collection incoming = new ArrayList( structure.inComing(var) );
		Collection outgoing = new ArrayList( structure.outGoing(var) );
		for (Iterator iter = outgoing.iterator(); iter.hasNext();) {
			removeEdge(var, (Variable) iter.next());
		}

		if( structure.removeVertex(var) )
		{
			myFlagTheoreticalCPTSizeValid = false;
			InOutDegreeProperty.setAllValues( incoming, this );

			int card = ((FiniteVariable) var).size();
			if( card == myMinDomainCardinality || card == myMaxDomainCardinality ) myFlagDomainCardinalityValid = false;
			return true;
		}
		else return false;
	}

	/**
	 * Generates the graph induced by the potential map. The nodes in the graph
	 * are the keys in the mapping. The values in the mapping are Potentials.
	 */
	 /*
	public static DirectedGraph inducedGraph( Map mapVariablesToPotentials )
	{
		DirectedGraph dg = new HashDirectedGraph(mapVariablesToPotentials.size());
		for( Iterator iter = mapVariablesToPotentials.keySet().iterator(); iter.hasNext(); )
		{
			Object node = iter.next();
			Potential p = (Potential) mapVariablesToPotentials.get(node);
			dg.add(node);
			for( Iterator viter = p.variables().iterator(); viter.hasNext(); )
			{
				Object n2 = viter.next();
				if (!node.equals(n2)) {
					dg.addEdge(n2, node);
				}
			}
		}
		return dg;
	}*/

	/** @author keith cascio
		@since 20021003 */
	public void induceGraph( Map mapVariablesToCPTShells ){
		BeliefNetworkImpl.this.induceGraph( mapVariablesToCPTShells, (NodeLinearTask)null );
	}

	/** @author keith cascio
		@since 20060519 */
	public void induceGraph( Map mapVariablesToCPTShells, NodeLinearTask task )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "BeliefNetworkImpl.induceGraph()" );

		//long start = JVMTI.getCurrentThreadCpuTimeUnsafe();
		//long pre = 0;
		//long elapsedAddVariable = 0;
		//long elapsedAddEdges    = 0;

		if( this.structure == null ) this.structure = new HashDirectedGraph();
		else clear();

		//long mid0 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		String errmsg = null;
		Variable node = null;
		Variable n2 = null;
		FiniteVariable fVar = null;
		CPTShell p = null;
		int sizeEdgesToAddLater = (mapVariablesToCPTShells.size() << 1);
		//System.out.println( "sizeEdgesToAddLater == " +  sizeEdgesToAddLater );
		List listEdgesToAddLater = new ArrayList( sizeEdgesToAddLater );
		boolean result = false;

		for( Iterator iter = mapVariablesToCPTShells.keySet().iterator(); iter.hasNext(); )
		{
			node = (Variable) iter.next();
			p = (CPTShell) mapVariablesToCPTShells.get(node);
			//pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
			result = addVariable( node, false );
			//elapsedAddVariable += JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;
			if( result )
			{
				//pre = JVMTI.getCurrentThreadCpuTimeUnsafe();
				for( Iterator viter = p.variables().iterator(); viter.hasNext(); )
				{
					n2 = (Variable) viter.next();
					if( node.equals( n2) )
					{
						//errmsg = "Attempt to add edge between variable and itself.";
					}
					else if( contains( n2 ) )
					{
						if( !addEdge(n2, node,false) ) errmsg = "Failed to add edge between " + n2.toString() + " and " + node.toString();
					}
					else
					{
						listEdgesToAddLater.add( n2 );
						listEdgesToAddLater.add( node );
					}
				}
				//elapsedAddEdges += JVMTI.getCurrentThreadCpuTimeUnsafe() - pre;

				if( node instanceof FiniteVariable )
				{
					fVar = (FiniteVariable) node;
					fVar.setCPTShell( p );
				}
			}
			else errmsg = "Failed to add variable " + node.toString();

			if( errmsg != null ) throw new IllegalArgumentException( "Error in BeliefNetworkImpl.induceGraph()\n" + errmsg );

			if( Thread.currentThread().isInterrupted() ){
				try{ BeliefNetworkImpl.this.clear(); }
				catch( Exception exception ){
					System.err.println( "warning: BeliefNetworkImpl.induceGraph() interrupted but failed emergency cleanup" );
				}
				return;
			}
			if( task != null ) task.touch();
		}

		//long mid1 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		//System.out.println( "Adding " + (listEdgesToAddLater.size()/2) + " edges later." );

		for( Iterator it = listEdgesToAddLater.iterator(); it.hasNext(); )
		{
			n2 = (Variable) it.next();
			node = (Variable) it.next();

			if( !addEdge(n2, node, false) )
			{
				errmsg = "Failed to add edge between " + n2.toString() + " and " + node.toString();
				throw new IllegalArgumentException( "Error in BeliefNetworkImpl.induceGraph()\n" + errmsg );
			}
		}

		//long end =  JVMTI.getCurrentThreadCpuTimeUnsafe();
		//long struct = mid0 - start;
		//long addall = mid1 - mid0;
		//long addlat = end  - mid1;
		//double total = (double) (end - start);

		//double structFrac = ((double)struct) / total;
		//double addallFrac = ((double)addall) / total;
		//double addnodFrac = ((double)elapsedAddVariable) / total;
		//double addedgFrac = ((double)elapsedAddEdges) / total;
		//double addlatFrac = ((double)addlat) / total;

		//this.tables = mapVariablesToCPTShells;

		//System.out.println( "BeliefNetworkImpl.induceGraph(), total: " + NetworkIO.formatTime((long)total) );
		//System.out.println( "    init structure: " + NetworkIO.FORMAT_PROFILE_PERCENT.format(structFrac) + " (" + NetworkIO.formatTime(struct)
		//              + "),\n    add majority  : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(addallFrac) + " (" + NetworkIO.formatTime(addall)
		//              + "),\n         variables: " + NetworkIO.FORMAT_PROFILE_PERCENT.format(addnodFrac) + " (" + NetworkIO.formatTime(elapsedAddVariable)
		//              + "),\n         edges    : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(addedgFrac) + " (" + NetworkIO.formatTime(elapsedAddEdges)
		//              + "),\n    add reminaing : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(addlatFrac) + " (" + NetworkIO.formatTime(addlat) + ")" );
	}

	/** @return the mapping from Variables to their CPTs */
	/*
	public Map getTableMapping() {
		return tables;
	}*/

	/** @return the CPT associated with the supplied variable */
	/*
	public Table getCPT(Object var) {
		return (Table) tables.get(var);
	}*/

	/** @since 20020606 */
	public boolean checkValidProbabilities()
	{
		/*
		Collection allTables = tables.values();
		for( Iterator it = allTables.iterator(); it.hasNext(); )
		{
			if( !((Table) it.next()).isValidProbability() ) return false;
		}
		*/

		return true;
	}

	/** @since 20020429 */
	public Collection tables()
	{
		//return tables.values();
		Collection ret = new ArrayList( size() );

		FiniteVariable fVar = null;
		CPTShell shell = null;
		for( Iterator it = iterator(); it.hasNext(); )
		{
			fVar = (FiniteVariable) it.next();
			shell = fVar.getCPTShell( fVar.getDSLNodeType() );
			if( shell != null ) ret.add( shell );
		}

		return ret;
	}

	private void createIDToVariable()
	{
		myMapIDsToVariables = new HashMap(structure.size());
		for(Iterator iter = structure.iterator(); iter.hasNext();)
		{
			Variable v = (Variable) iter.next();
			myMapIDsToVariables.put(v.getID(), v);
		}
	}

	/** Returns the variable with the id supplied */
	public Variable forID( String id )
	{
		if( myMapIDsToVariables == null ) createIDToVariable();

		Variable ret = (Variable) myMapIDsToVariables.get( id );
		if( ret != null ) return ret;
		else{
			for( Iterator it = iterator(); it.hasNext(); ){
				ret = (Variable) it.next();
				myMapIDsToVariables.put( ret.getID(), ret );
				if( ret.getID().equals( id ) ) return ret;
			}
		}

		return (Variable)null;
	}


	public void writeToVCGFile(File file)
	throws IOException {
		PrintWriter stream = new PrintWriter(new BufferedWriter(new FileWriter(file)));

		stream.print("graph: { title: \"Bayesian Network\"\n");
		stream.print("layoutalgorithm: minbackward\n");
		stream.print("treefactor: 0.9\n");

		//write nodes
		for(Iterator itr=iterator(); itr.hasNext();) {
			FiniteVariable fv = (FiniteVariable)itr.next();

			StringBuffer label = new StringBuffer(fv.getID() + "\n");
			for(int i=0; i<fv.size(); i++) { label.append(fv.instance(i) + "\n");}

			stream.print("node: { title: \"" + fv.getID() + "\"\n");
			stream.print("label: \"" + label + "\"\n");
			stream.print("shape: box}\n");
		}

		//write edges
		for(Iterator itr=iterator(); itr.hasNext();) {
			FiniteVariable fv = (FiniteVariable)itr.next();

			for(Iterator itr_ch=outGoing(fv).iterator(); itr_ch.hasNext();) {
				FiniteVariable chi = (FiniteVariable)itr_ch.next();

				stream.print("edge: {\n");
				stream.print("sourcename: \"" + fv.getID()+"\"\n");
				stream.print("targetname: \"" + chi.getID()+"\"\n");
				stream.print("}\n");
			}
		}

		stream.print("}\n");
		stream.flush();
	}


}
