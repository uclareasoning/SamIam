package edu.ucla.belief.ui.displayable;

import java.util.List;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import edu.ucla.structure.*;
import edu.ucla.util.UserObject;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.AbstractStringifier;
import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.belief.io.dsl.*;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.Util;

/** @author keith cascio
	@since 20021001 */
public class DisplayableBeliefNetworkImpl implements DisplayableBeliefNetwork
{
	public static boolean FLAG_DEBUG_VERBOSE = Util.DEBUG_VERBOSE;

	public DisplayableBeliefNetworkImpl( BeliefNetwork toDecorate, NetworkInternalFrame hnif )
	{
		myBeliefNetwork = toDecorate;
		hnInternalFrame = hnif;
		if( myFlagIsHuginNet = ( myBeliefNetwork instanceof HuginNet ) ){
			myHuginNet = (HuginNet) myBeliefNetwork;
		}
		if( myFlagIsGenieNet = ( myBeliefNetwork instanceof GenieNet ) ){
			myGenieNet = (GenieNet) myBeliefNetwork;
		}
		if( myFlagIsPropertySuperintendent = ( myBeliefNetwork instanceof PropertySuperintendent ) ){
			myPropertySuperintendent = (PropertySuperintendent) myBeliefNetwork;
		}

		if( FLAG_DEBUG_VERBOSE )
		{
			Util.STREAM_VERBOSE.println( "DisplayableBeliefNetworkImpl("+System.identityHashCode(this)+")( "+toDecorate.getClass().getName()+" )\n\tmyFlagIsHuginNet=="+myFlagIsHuginNet+", myFlagIsGenieNet=="+myFlagIsGenieNet );
			//Util.STREAM_VERBOSE.println( "(params)" + System.identityHashCode(getProperties()) + ": " + getProperties() );
		}

		myNumUserPropertiesInitial = toDecorate.countUserEnumProperties();
		makeDisplayableVariables();
		if( Thread.currentThread().isInterrupted() ) return;
		launderProperties();
	}

	/** @since 20080219 */
	public FiniteVariable newFiniteVariable( Map properties ){
		return new DisplayableFiniteVariableImpl( myBeliefNetwork.newFiniteVariable( properties ), hnInternalFrame );
	}

	/** @since 110204 */
	public void investigateCycles(){
		investigateCycles( (BeliefNetwork)this, hnInternalFrame.getFileName(), hnInternalFrame.getParentFrame() );
	}

	/** @since 110204 */
	public static void investigateCycles( BeliefNetwork bn, String filepath, UI ui )
	{
		RecursiveDepthFirstIterator rdfi = new RecursiveDepthFirstIterator( bn );
		if( rdfi.isCyclic() ){
			List cycle = rdfi.getCycle();
			if( (cycle != null) && (!cycle.isEmpty()) ){
				final String strCycle = RecursiveDepthFirstIterator.cycleToString( cycle, AbstractStringifier.VARIABLE_ID );
				final String strCycleTruncated = strCycle.substring( 0, 54 );
				boolean flagTruncated = strCycleTruncated.length() != strCycle.length();
				String strCycleDisplay = strCycleTruncated;
				String infix = "";
				if( flagTruncated ){
					infix = " entire cycle";
					strCycleDisplay += "... ]";
				}

				String textButton = "Copy" + infix + " to system clipboard";
				JButton button = new JButton( textButton );
				button.setToolTipText( textButton );
				button.addActionListener( new ActionListener(){
					public void actionPerformed( ActionEvent e ){
						Util.copyToSystemClipboard( strCycle );
					}
				} );

				JPanel panel = new JPanel( new GridBagLayout() );
				GridBagConstraints c = new GridBagConstraints();

				JLabel label;

				c.anchor = GridBagConstraints.WEST;
				c.gridwidth = GridBagConstraints.REMAINDER;
				panel.add( new JLabel( "The file " +filepath+ " contains at least one cycle:" ), c );
				panel.add( Box.createVerticalStrut( 16 ), c );
				c.gridwidth = 1;
				panel.add( label = new JLabel( strCycleDisplay ), c );
				panel.add( Box.createHorizontalStrut( 8 ), c );
				c.gridwidth = GridBagConstraints.REMAINDER;
				panel.add( button, c );

				label.setToolTipText( strCycle );
				label.setForeground( Color.red );

				ui.showErrorDialog( panel );
			}
		}
	}

	protected void makeDisplayableVariables()
	{
		Map oldVariablesToDVars = new HashMap( DisplayableBeliefNetworkImpl.this.size() );
		FiniteVariable fVar = null;
		DisplayableFiniteVariable dVar = null;
		for( Iterator it = iterator(); it.hasNext(); )
		{
			fVar = (FiniteVariable) it.next();

			dVar = new DisplayableFiniteVariableImpl( fVar, hnInternalFrame );
			if( dVar.getDSLSubmodel() == null ) dVar.setDSLSubmodel( getMainDSLSubmodel() );

			oldVariablesToDVars.put( fVar, dVar );
		}

		replaceVariables( oldVariablesToDVars, (NodeLinearTask) null );//, hnInternalFrame.getConstructionTask() );
	}

	/** @since 20031208 */
	protected void launderProperties()
	{
		Map to = getProperties();
		if( to != null )
		{
			Map from = new HashMap( to );
			to.clear();
			BeliefNetworkImpl.putAll( from, to );
		}
	}

	public DSLSubmodel getMainDSLSubmodel()
	{
		if( isGenieNet() ) return myGenieNet.getDSLSubmodelFactory().MAIN;
		else
		{
			if( myRootDSLSubmodel == null )
			{
				myRootDSLSubmodel = new DSLSubmodel( 0 );
				myRootDSLSubmodel.setName( "Root Submodel" );
			}
			return myRootDSLSubmodel;
		}
	}

	private DSLSubmodel myRootDSLSubmodel = null;
	protected NetworkInternalFrame hnInternalFrame;

	public NetworkInternalFrame getNetworkInternalFrame()
	{
		return hnInternalFrame;
	}
	public boolean isHuginNet()
	{
		return myFlagIsHuginNet;
	}
	public boolean isGenieNet()
	{
		return myFlagIsGenieNet;
	}

	public DFVIterator dfvIterator()
	{
		return new DFVIterator( iterator() );
	}

	public BeliefNetwork getSubBeliefNetwork()
	{
		return myBeliefNetwork;
	}

	/** @since 010904 */
	public int countUserEnumPropertiesInitial()
	{
		return myNumUserPropertiesInitial;
	}

	private BeliefNetwork myBeliefNetwork = null;
	private int myNumUserPropertiesInitial = (int)0;

	//interface DirectedGraph
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
		return myBeliefNetwork.retainAll(p1);
	}
	public java.lang.Object[] toArray(java.lang.Object[] p)
	{
		return myBeliefNetwork.toArray(p);
	}
	public java.lang.Object[] toArray()
	{
		return myBeliefNetwork.toArray();
	}
	public boolean removeAll(final java.util.Collection p)
	{
		return myBeliefNetwork.removeAll(p);
	}
	public void clear()
	{
		myBeliefNetwork.clear();
	}
	//public int hashCode()
	//{
	//	return myBeliefNetwork.hashCode();
	//}
	public boolean addAll(final java.util.Collection p)
	{
		return myBeliefNetwork.addAll(p);
	}
	public boolean containsAll(final java.util.Collection p)
	{
		return myBeliefNetwork.containsAll(p);
	}
	public boolean equals(final java.lang.Object p)
	{
		return myBeliefNetwork.equals(p);
	}
	public java.util.Iterator iterator()
	{
		return myBeliefNetwork.iterator();
	}
	public boolean isEmpty()
	{
		return myBeliefNetwork.isEmpty();
	}
	public List topologicalOrder()
	{
		return myBeliefNetwork.topologicalOrder();
	}
	public void replaceVertex( Object oldVertex, Object newVertex )
	{
		myBeliefNetwork.replaceVertex(oldVertex,newVertex);
	}
	public void replaceVertices( Map verticesOldToNew, NodeLinearTask task )
	{
		myBeliefNetwork.replaceVertices( verticesOldToNew, task );
	}
	public boolean maintainsAcyclicity( Object vertex1, Object vertex2 )
	{
		return myBeliefNetwork.maintainsAcyclicity(vertex1,vertex2);
	}
	public Set vertices()
	{
		return myBeliefNetwork.vertices();
	}
	public Set inComing(Object vertex)
	{
		return myBeliefNetwork.inComing(vertex);
	}
	public Set outGoing(Object vertex)
	{
		return myBeliefNetwork.outGoing(vertex);
	}
	public int degree(Object vertex)
	{
		return myBeliefNetwork.degree(vertex);
	}
	public int inDegree(Object vertex)
	{
		return myBeliefNetwork.inDegree(vertex);
	}
	public int outDegree(Object vertex)
	{
		return myBeliefNetwork.outDegree(vertex);
	}
	public boolean containsEdge(Object vertex1, Object vertex2)
	{
		return myBeliefNetwork.containsEdge(vertex1,vertex2);
	}
	public boolean contains(Object vertex)
	{
		return myBeliefNetwork.contains(vertex);
	}
	public int size()
	{
		return myBeliefNetwork.size();
	}
	public int numEdges()
	{
		return myBeliefNetwork.numEdges();
	}
	public boolean isAcyclic()
	{
		return myBeliefNetwork.isAcyclic();
	}
	public boolean isWeaklyConnected()
	{
		return myBeliefNetwork.isWeaklyConnected();
	}
	public boolean isWeaklyConnected(Object vertex1, Object vertex2)
	{
		return myBeliefNetwork.isWeaklyConnected(vertex1,vertex2);
	}
	public boolean hasPath(Object vertex1, Object vertex2)
	{
		return myBeliefNetwork.hasPath(vertex1,vertex2);
	}
	public boolean isSinglyConnected()
	{
		return myBeliefNetwork.isSinglyConnected();
	}
	public boolean addVertex(Object vertex)
	{
		return myBeliefNetwork.addVertex(vertex);
	}
	public boolean removeVertex(Object vertex)
	{
		return myBeliefNetwork.removeVertex(vertex);
	}
	public boolean addEdge(Object vertex1, Object vertex2)
	{
		return myBeliefNetwork.addEdge(vertex1,vertex2);
	}
	public boolean removeEdge(Object vertex1, Object vertex2)
	{
		return myBeliefNetwork.removeEdge(vertex1,vertex2);
	}

	//interface BeliefNetwork
	public void setScalars( double scalar )
	{
		myBeliefNetwork.setScalars( scalar );
	}
	public void replaceAllPotentials( Map mapVariablesToPotentials )
	{
		myBeliefNetwork.replaceAllPotentials( mapVariablesToPotentials );
	}
	public void cloneAllCPTShells()
	{
		myBeliefNetwork.cloneAllCPTShells();
	}
	public void induceGraph( Map mapVariablesToPotentials )
	{
		myBeliefNetwork.induceGraph( mapVariablesToPotentials );
	}
	public boolean mayContain( Object obj )
	{
		return obj instanceof DisplayableFiniteVariable;
	}
	public void replaceVariables( Map variablesOldToNew, NodeLinearTask task )
	{
		myBeliefNetwork.replaceVariables( variablesOldToNew, task );
	}
	//public UserObject getUserObject()
	//{
	//	return myBeliefNetwork.getUserObject();
	//}
	//public void setUserObject( UserObject obj )
	//{
	//	myBeliefNetwork.setUserObject(obj);
	//}
	//public UserObject getUserObject2()
	//{
	//	return myBeliefNetwork.getUserObject2();
	//}
	//public void setUserObject2( UserObject obj )
	//{
	//	myBeliefNetwork.setUserObject2(obj);
	//}
	public boolean forAll( EnumProperty property, EnumValue value )
	{
		return myBeliefNetwork.forAll( property, value );
	}
	public boolean thereExists( EnumProperty property, EnumValue value )
	{
		return myBeliefNetwork.thereExists( property, value );
	}
	public Collection findVariables( EnumProperty property, EnumValue value )
	{
		return myBeliefNetwork.findVariables( property, value );
	}
	public void setAutoCPTInvalidation( boolean flag )
	{
		myBeliefNetwork.setAutoCPTInvalidation( flag );
	}
	public boolean getAutoCPTInvalidation()
	{
		return myBeliefNetwork.getAutoCPTInvalidation();
	}
	public EnumProperty[] propertiesAsArray()
	{
		return myBeliefNetwork.propertiesAsArray();
	}
	public int countUserEnumProperties()
	{
		return myBeliefNetwork.countUserEnumProperties();
	}
	public Collection getUserEnumProperties()
	{
		return myBeliefNetwork.getUserEnumProperties();
	}
	public void setUserEnumProperties( Collection userProperties )
	{
		myBeliefNetwork.setUserEnumProperties( userProperties );
		hnInternalFrame.getTreeScrollPane().resetEnumPropertiesDisplay();
	}
	public void makeUserEnumProperties( Map params )
	{
		myBeliefNetwork.makeUserEnumProperties( params );
	}
	public boolean thereExistsModifiedUserEnumProperty()
	{
		return myBeliefNetwork.thereExistsModifiedUserEnumProperty();
	}
	public void setUserEnumPropertiesModified( boolean flag )
	{
		myBeliefNetwork.setUserEnumPropertiesModified( flag );
	}
	public EvidenceController getEvidenceController()
	{
		return myBeliefNetwork.getEvidenceController();
	}
	public void setEvidenceController( EvidenceController EC )
	{
		myBeliefNetwork.setEvidenceController( EC );
	}

	/** @since 021804 */
	public Copier getCopier()
	{
		if( myDisplayableCopier == null ) myDisplayableCopier = new DisplayableCopier( myBeliefNetwork.getCopier(), hnInternalFrame );
		return myDisplayableCopier;
	}
	private DisplayableCopier myDisplayableCopier;

	/**
		Warning: this method returns a clone of the "Sub BeliefNetwork".
		@ret An Object of type BeliefNetwork.
	*/
	public Object clone()
	{
		return myBeliefNetwork.clone();
	}
	/**
		Warning: this method returns a clone of the "Sub BeliefNetwork".
		@ret An Object of type BeliefNetwork.
	*/
	public BeliefNetwork deepClone()
	{
		return myBeliefNetwork.deepClone();
	}
	/**
		Warning: this method returns a clone of the "Sub BeliefNetwork".
		@ret An Object of type BeliefNetwork.
	*/
	public BeliefNetwork seededClone( Map variablesOldToNew )
	{
		return myBeliefNetwork.seededClone( variablesOldToNew );
	}
	/**
		Warning: this method returns a clone of the "Sub BeliefNetwork".
		@ret An Object of type BeliefNetwork.
	*/
	public BeliefNetwork shallowClone()
	{
		return myBeliefNetwork.shallowClone();
	}
	public void identifierChanged( String oldID, Variable var )
	{
		myBeliefNetwork.identifierChanged(oldID, var);
	}
	/** @since 20091124 */
	public boolean    addAuditor(    Auditor  auditor ){
		return myBeliefNetwork.   addAuditor( auditor );
	}
	/** @since 20091124 */
	public boolean removeAuditor(    Auditor  auditor ){
		return myBeliefNetwork.removeAuditor( auditor );
	}
	/** @since 20091130 */
	public BeliefNetwork fireAudit( Variable from, Variable to, Collection targets, Auditor.Deed deed ){
		 myBeliefNetwork.fireAudit(          from,          to,            targets,              deed );
		return this;
	}
	public boolean addEdge(Variable from, Variable to, boolean expandCPT )
	{
		return myBeliefNetwork.addEdge(from, to, expandCPT);
	}
	//public Table expandTable(Table t, FiniteVariable var)
	//{
	//	return myBeliefNetwork.expandTable(t,var);
	//}
	public boolean removeEdge( Variable from, Variable to, boolean forget )
	{
		return myBeliefNetwork.removeEdge( from, to, forget );
	}
	//public Table forget(Table t, Variable from)
	//{
	//	return myBeliefNetwork.forget(t,from);
	//}
	public boolean addVariable( Variable newNode, boolean createCPT )
	{
		return myBeliefNetwork.addVariable(newNode, createCPT);
	}
	public boolean removeVariable(Variable var)
	{
		return myBeliefNetwork.removeVariable(var);
	}
	public int getMaxDomainCardinality()
	{
		return myBeliefNetwork.getMaxDomainCardinality();
	}
	public int getMinDomainCardinality()
	{
		return myBeliefNetwork.getMinDomainCardinality();
	}
	public int getTheoreticalCPTSize( FiniteVariable fVar ){
		return myBeliefNetwork.getTheoreticalCPTSize( fVar );
	}
	public int getMaxTheoreticalCPTSize(){
		return myBeliefNetwork.getMaxTheoreticalCPTSize();
	}
	public int getMinTheoreticalCPTSize(){
		return myBeliefNetwork.getMinTheoreticalCPTSize();
	}
	public boolean insertState( FiniteVariable var, int index, Object instance )
	{
		return myBeliefNetwork.insertState(var,index,instance);
	}
	public Object removeState( FiniteVariable var, int index )
	{
		return myBeliefNetwork.removeState(var,index);
	}
	public boolean checkValidProbabilities()
	{
		return myBeliefNetwork.checkValidProbabilities();
	}
	public Collection tables()
	{
		return myBeliefNetwork.tables();
	}
	public Variable forID( String id )
	{
		return myBeliefNetwork.forID( id );
	}

	protected boolean myFlagIsHuginNet               = false;
	protected boolean myFlagIsGenieNet               = false;
	protected boolean myFlagIsPropertySuperintendent = false;

	protected HuginNet               myHuginNet;
	protected GenieNet               myGenieNet;
	protected PropertySuperintendent myPropertySuperintendent;

	//interface HuginNet
	public HuginFileVersion getVersion()
	{
		if( myFlagIsHuginNet ) return myHuginNet.getVersion();
		else return null;
	}
	public void setVersion( HuginFileVersion version )
	{
		if( myFlagIsHuginNet ) myHuginNet.setVersion( version );
	}
	public Dimension getGlobalNodeSize( Dimension dim )
	{
		if( myFlagIsHuginNet ) return myHuginNet.getGlobalNodeSize( dim );
		else
		{
			if( dim == null ) dim = new Dimension();
			return dim;
		}
	}
	public void setParams( Map params )
	{
		//System.out.println( "DisplayableBeliefNetworkImpl.setParams("+System.identityHashCode(params)+")" );
		if( myFlagIsHuginNet ) myHuginNet.setParams( params );
		else if( myFlagIsGenieNet ) myGenieNet.setParams( params );
	}
	public Map getProperties()
	{
		//System.out.println( "DisplayableBeliefNetworkImpl.getProperties()" );
		if( myFlagIsPropertySuperintendent ) return myPropertySuperintendent.getProperties();
		else return null;
	}
	/*
	public void add( HuginNode var ){
		if( myFlagIsHuginNet ) myHuginNet.add( var );
	}
	public void remove( HuginNode var ){
		if( myFlagIsHuginNet ) myHuginNet.remove( var );
	}*/

	//interface GenieNet
	public Collection getVariables( DSLSubmodel forModel )
	{
		if( myFlagIsGenieNet ) return myGenieNet.getVariables( forModel );
		else return vertices();
	}
	public Set getDeepVariables( DSLSubmodel forModel )
	{
		if( myFlagIsGenieNet ) return myGenieNet.getDeepVariables( forModel );
		else return vertices();
	}
	public boolean isAnscestor( DSLSubmodel forModel, Variable var )
	{
		if( myFlagIsGenieNet ) return myGenieNet.isAnscestor( forModel, var );
		else return false;
	}
	public void addDeepVariables( Collection ret, DSLSubmodel forModel )
	{
		if( myFlagIsGenieNet ) myGenieNet.addDeepVariables( ret, forModel );
	}
	public DSLSubmodelFactory getDSLSubmodelFactory()
	{
		if( myFlagIsGenieNet ) return myGenieNet.getDSLSubmodelFactory();
		else return null;
	}
	/*
	public boolean addVariable( Variable var ){
		if( myFlagIsGenieNet ) return myGenieNet.addVariable( var );
		else return myBeliefNetwork.addVariable( var );
	}
	public boolean removeVariable( Variable var ){
		if( myFlagIsGenieNet ) return myGenieNet.removeVariable( var );
		else return myBeliefNetwork.removeVariable( var );
	}*/
}
