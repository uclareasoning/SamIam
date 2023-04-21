package edu.ucla.belief.io.dsl;

//{superfluous} import edu.ucla.structure.DirectedGraph;
import edu.ucla.belief.*;
import edu.ucla.belief.io.NodeLinearTask;

//{superfluous} import java.awt.Point;
//{superfluous} import java.awt.Dimension;
import java.util.*;

public class GenieNetImpl extends BeliefNetworkImpl implements GenieNet
{
	public static boolean FLAG_DEBUG = Definitions.DEBUG;

	/** @since 20080219 */
	public FiniteVariable newFiniteVariable( Map properties ){
		properties = minimalProperties( properties );
		return new DSLNodeImpl( (String) properties.get( KEY_HUGIN_ID ), properties );
	}

	public GenieNetImpl()
	{
		super( true );
		initGenieNetImpl();
	}

	public GenieNetImpl( Map mapVariablesToPotentials )
	{
		super( mapVariablesToPotentials );
		initGenieNetImpl();
	}

	protected void initGenieNetImpl()
	{
		myDSLSubmodelFactory = new DSLSubmodelFactory();
		myMapSubmodelsToVariableCollections = new HashMap();
	}

	protected GenieNetImpl( GenieNetImpl toCopy )
	{
		super( toCopy );
		this.myDSLSubmodelFactory = toCopy.myDSLSubmodelFactory;
		this.myMapSubmodelsToVariableCollections = cloneMapSubmodelsToVariableCollections( toCopy.myMapSubmodelsToVariableCollections );
		BeliefNetworkImpl.putAll( toCopy.getProperties(), this.getProperties() );//this.getProperties().putAll( toCopy.getProperties() );
	}

	protected Map cloneMapSubmodelsToVariableCollections( Map mapSubmodelsToVariableCollections )
	{
		Map ret = new HashMap();
		Object submodelTemp = null;
		Collection oldCollection = null;
		Collection newCollection = null;
		for( Iterator it = mapSubmodelsToVariableCollections.keySet().iterator(); it.hasNext(); )
		{
			submodelTemp = it.next();
			oldCollection = (Collection) mapSubmodelsToVariableCollections.get( submodelTemp );
			newCollection = new TreeSet( oldCollection );
			ret.put( submodelTemp, newCollection );
		}
		return ret;
	}

	public boolean mayContain( Object obj )
	{
		return obj instanceof DSLNode;
	}

	/**
		@author Keith Cascio
		@since 061002
	*/
	public String toString()
	{
		return "One GenieNetImpl";//super.toString().substring( 19 );
	}

	/**
		GenieNet's primary responsibility as of 061002 is to
		keep track of which variables belong to which submodels.
		Call this method as part of deep cloning a BeliefNetwork,
		to ask for a new GenieNetImpl that keeps track of the new
		variables.

                @param variablesOldToNew A mapping from Variables this GenieNetImpl knows about
                to the new Variables you would like the cloned GenieNetImpl to know about.

		@author Keith Cascio
		@since 061002
	*/
	public BeliefNetwork seededClone( Map variablesOldToNew )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "\nGenieNetImpl.seededClone( " +variablesOldToNew+ " )" );
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "\n\nGenieNetImpl(copy)" );
		GenieNetImpl ret = new GenieNetImpl( this );
		ret.replaceVariables( variablesOldToNew );
		return ret;
	}

	/** @since 20021001 */
	public void replaceVariables( Map variablesOldToNew, NodeLinearTask task )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "GenieNetImpl.replaceVariables()" );

		super.replaceVariables( variablesOldToNew, task );

		DSLSubmodel subTemp = null;
		Variable varOld = null;
		Variable varNew = null;
		Set setTemp = null;
		Set setToRemove =  new HashSet();
		Set setToAdd =  new HashSet();
		for( Iterator it = myMapSubmodelsToVariableCollections.keySet().iterator(); it.hasNext(); )
		{
			subTemp = (DSLSubmodel) it.next();
			setTemp = (Set) myMapSubmodelsToVariableCollections.get( subTemp );

			if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "old: " + subTemp + " -> " + setTemp );

			setToRemove.clear();
			setToAdd.clear();
			for( Iterator iteratorVars = setTemp.iterator(); iteratorVars.hasNext(); )
			{
				varOld = (Variable) iteratorVars.next();
				varNew = (Variable) variablesOldToNew.get( varOld );
				if( varNew != null )
				{
					setToAdd.add( varNew );
					setToRemove.add( varOld );
				}
				//else throw new RuntimeException( "GenieNetImpl.seededClone() called with incomplete Map: missing variable " + varOld );
			}

			setTemp.removeAll( setToRemove );
			setTemp.addAll( setToAdd );

			if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "new: " + subTemp + " -> " + setTemp );
		}
	}

	/** @since 20051114 */
	public void clear(){
		super.clear();

		if( myMapSubmodelsToVariableCollections != null ) myMapSubmodelsToVariableCollections.clear();
		if( myMapSubmodelsToDeepSets != null ) myMapSubmodelsToDeepSets.clear();
	}

	/**
		@author Keith Cascio
		@since 041902
	*/
	public Collection getVariables( DSLSubmodel forModel )
	{
		Collection ret = (Collection)(myMapSubmodelsToVariableCollections.get( forModel ));
		return ( ret == null ) ? Collections.EMPTY_LIST : ret;
	}

	/**
		The first call is expensive.
		@author Keith Cascio
		@since 050202
	*/
	public Set getDeepVariables( DSLSubmodel forModel )
	{
		if( myMapSubmodelsToDeepSets == null ) myMapSubmodelsToDeepSets = new HashMap();
		else
		{
			if( myMapSubmodelsToDeepSets.containsKey( forModel ) )
			{
				return (Set) myMapSubmodelsToDeepSets.get( forModel );
			}
		}
		Set ret = new HashSet();
		addDeepVariables( ret, forModel );
		myMapSubmodelsToDeepSets.put( forModel, ret );
		return ret;
	}

	/**
		The first call is expensive.
		@author Keith Cascio
		@since 050202
	*/
	public boolean isAnscestor( DSLSubmodel forModel, Variable var )
	{
		return getDeepVariables( forModel ).contains( var );
	}

	/**
		Recusively traverse submodel tree rooted at forModel and add all variables to ret.

		@param forModel The root of the submodel tree to traverse.
		@param ret The Collection to which all variables will be added.
		@author Keith Cascio
		@since 050202
	*/
	public void addDeepVariables( Collection ret, DSLSubmodel forModel )
	{
		ret.addAll( getVariables( forModel ) );
		for( Iterator it = forModel.getChildSubmodels(); it.hasNext(); )
		{
			addDeepVariables( ret, (DSLSubmodel)it.next() );
		}
	}

	public DSLSubmodelFactory getDSLSubmodelFactory()
	{
		return myDSLSubmodelFactory;
	}

	public boolean addVariable( Variable var, boolean createCPT )
	{
		if( super.addVariable( var, createCPT ) )
		{
			DSLNode dNode = (DSLNode) var;

			DSLSubmodel submodel = dNode.getDSLSubmodel();

			if( submodel == null )
			{
				//default to MAIN
				submodel = myDSLSubmodelFactory.MAIN;
				dNode.setDSLSubmodel( myDSLSubmodelFactory.MAIN );
			}

			Collection colSubmodel = null;
			if( myMapSubmodelsToVariableCollections.containsKey( submodel ) )
			{
				colSubmodel = (Collection) myMapSubmodelsToVariableCollections.get( submodel );
			}
			else
			{
				colSubmodel = new TreeSet( VariableComparator.getInstance() );
				myMapSubmodelsToVariableCollections.put( submodel, colSubmodel );
			}
			colSubmodel.add( dNode );

			//System.out.println( "Java GenieNetImpl.add() " + node.name + " in submodel " + node.getDSLSubmodel() );//debug

			return true;
		}
		else return false;
	}

	public boolean removeVariable( Variable var )
	{
		if( super.removeVariable( var ) )
		{
			DSLNode dNode = (DSLNode) var;

			Collection submodelNodes = getVariables( dNode.getDSLSubmodel() );
			if( submodelNodes != null )
			{
				submodelNodes.remove( dNode );
			}
			else System.err.println( "Java warning: GenieNetImpl.removeVariable() called - DSLSubmodel not found." );

			return true;
		}
		return true;
	}

	/**
	* Sets the net parameters to the name value pairs contained in params.
	*/
	public void setParams( Map inputParams )
	{
		this.getProperties().clear();
		this.getProperties().putAll( inputParams );

		makeUserEnumProperties( this.getProperties() );
	}

  	//private

	private Map myMapSubmodelsToDeepSets = null;
	private DSLSubmodelFactory myDSLSubmodelFactory = null;
	private Map myMapSubmodelsToVariableCollections = null;
}
