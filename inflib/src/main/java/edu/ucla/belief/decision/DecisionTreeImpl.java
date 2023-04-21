package edu.ucla.belief.decision;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.StateNotFoundException;
import edu.ucla.belief.Table;
import edu.ucla.belief.TableIndex;
import edu.ucla.util.WeakLinkedList;
import edu.ucla.structure.DirectedGraph;
import edu.ucla.structure.AbstractDirectedGraph;

import java.util.*;

/** @author Keith Cascio
	@since 120804 */
public class DecisionTreeImpl extends AbstractDirectedGraph implements DecisionTree, Factory, DecisionListener, DirectedGraph
{
	public DecisionTreeImpl( TableIndex index ){
		this.myTableIndex = index;
		init();
	}

	public DecisionTreeImpl( DecisionNode root ){
		this.myTableIndex = (TableIndex)null;
		initRoot( root );
	}

	/** @since 011605 */
	public void noteOptimizationEpsilon( double epsilon ){
		this.myOptimizationEpsilon = epsilon;
	}

	/** @since 011605 */
	public double recallOptimizationEpsilon(){
		return this.myOptimizationEpsilon;
	}

	/** interface DecisionTree */
	public void snapshot(){
		mySnapshot = ((DecisionNodeAbstract)getRoot()).deflate();
	}

	/** interface DecisionTree */
	public boolean restoreSnapshot()
	{
		if( mySnapshot == null ) return false;
		DecisionNode oldRoot = getRoot();
		removeHistoryAllDescendants( oldRoot );
		initRoot( mySnapshot.inflate( (Factory)this ) );
		fireDecisionEvent( new DecisionEvent( (DecisionTree)this, getRoot(), DecisionEvent.ASSIGNMENT_CHANGE ) );
		return true;
	}

	/** interface DecisionTree */
	public void ensureSnapshot(){
		if( mySnapshot == null ) snapshot();
	}

	/** interface DecisionTree */
	public DecisionBackup getSnapshot(){
		return this.mySnapshot;
	}

	/** interface DecisionTree */
	public void setSnapshot( DecisionBackup snaoshot ){
		this.mySnapshot = snaoshot;
	}

	/** interface DecisionTree */
	public void normalize(){
		int counter = 0;
		Collection rents;
		while( !((rents = myRoot.getParents()).isEmpty()) ){
			myRoot = (DecisionNode) rents.iterator().next();
			if( counter > 1000 ) throw new IllegalStateException( "probable infinite loop due to cycle in decision tree" );
		}
	}

	/** interface DecisionTree */
	public DecisionNode getRoot(){
		normalize();
		return this.myRoot;
	}

	/** interface DecisionTree */
	public TableIndex getIndex(){
		return this.myTableIndex;
	}

	/** interface DecisionTree */
	public Table expand(){
		double[] data = new double[ myTableIndex.size() ];
		fillDataFromRoot( data );
		return new Table( myTableIndex, data );
	}

	/** @since 011405 */
	private void fillDataFromRoot( double[] data ){
		FiniteVariable[] variables = (FiniteVariable[]) myTableIndex.variables().toArray( new FiniteVariable[ myTableIndex.getNumVariables() ] );
		int[] instantiation = new int[ myTableIndex.getNumVariables() ];
		instantiation[ instantiation.length-1 ] = 0;
		fillData( variables, instantiation, 0, data );
	}

	/** @since 011405 */
	private void fillData( FiniteVariable[] variables, int[] instantiation, int index, double[] data ){
		int nextindex = index + 1;
		if( nextindex == variables.length ){
			DecisionLeaf leaf = getLeaf( instantiation );
			//System.out.print( "instantiation: " );
			//println( instantiation, System.out );
			int segment = myTableIndex.index( instantiation );
			//System.out.println( "leaf.copyParametersInto( "+segment+" )" );
			leaf.copyParametersInto( data, segment );
		}
		else{
			for( int i=0; i<variables[index].size(); i++ ){
				instantiation[index] = i;
				fillData( variables, instantiation, nextindex, data );
			}
		}
	}

	//public static void println( int[] array, java.io.PrintStream out ){
	//	for( int i=0; i<array.length; i++ ) out.print( array[i] + "," );
	//	out.println();
	//}

	/** interface DecisionTree
		@since 011405 */
	public DecisionLeaf getLeaf( final int[] indices ){
		return getLeaf( getRoot(), indices );
	}

	/** @since 011405 */
	public DecisionLeaf getLeaf( DecisionNode node, int[] instantiation ){
		if( node.isLeaf() ) return (DecisionLeaf) node;
		FiniteVariable var = node.getVariable();
		int index = myTableIndex.variableIndex( var );
		DecisionNode next = null;
		try{
			next = node.getNext( instantiation[index] );
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: DecisionTreeImpl.getLeaf() caused " + statenotfoundexception );
			return (DecisionLeaf) null;
		}
		//if( next == null ) throw new IllegalStateException( "node("+node+"), instance("+instantiation[index]+")" );
		return getLeaf( next, instantiation );
	}

	/** interface DecisionTree */
	public void addListener( DecisionListener listener ){
		if( myListeners == null ) myListeners = new WeakLinkedList();
		if( !myListeners.contains(listener) ) myListeners.addLast( listener );
	}

	/** interface DecisionTree */
	public boolean removeListener( DecisionListener listener ){
		if( myListeners == null ) return false;
		else return myListeners.remove( listener );
	}

	protected void fireDecisionEvent( DecisionEvent e ){
		if( myListeners == null ) return;
		if( e.tree == null ) e.tree = (DecisionTree)this;
		DecisionListener next;
		for( Iterator it = myListeners.iterator(); it.hasNext(); ){
			next = (DecisionListener)it.next();
			if( next == null ) it.remove();
			else next.decisionEvent( e );
		}
	}

	/** interface DecisionListener */
	public void decisionEvent( DecisionEvent e ){
		//System.out.println( "DecisionTreeImpl.decisionEvent( "+e+" )" );
		if( e.type == DecisionEvent.ASSIGNMENT_CHANGE ){
			for( Iterator it = e.node.getChildDecisionNodes().iterator(); it.hasNext(); ){
				((DecisionNode)it.next()).addListener( (DecisionListener)this );
			}
		}
		else if( e.type == DecisionEvent.PARENT_INSERTED ){
			e.parent.addListener( this );
		}
		else if( e.type == DecisionEvent.DERACINATED ){
			initRoot( e.node );
		}
		fireDecisionEvent( e );
	}

	private void init(){
		FiniteVariable joint = myTableIndex.getJoint();
		myDefault = new DecisionLeaf( joint, (Factory)this );
		myDefault.addListener( this );
		myDefault.setID( "uniform" );
		myDefault.setEditable( false );
		initRoot( myDefault );
	}

	public void initRoot( DecisionNode root ){
		//System.out.println( "DecisionTreeImpl.initRoot( "+root+" )" );
		this.myRoot = root;
		normalize();
		if( myRoot != root ) throw new IllegalArgumentException();

		Set all = myRoot.getDescendants( new HashSet(), Classifier.ALL );
		for( Iterator it = all.iterator(); it.hasNext(); )
			((DecisionNode)it.next()).addListener( (DecisionListener)this );
	}

	/** interface DecisionTree */
	public double getParameter( final int[] indices ){
		try{
			return getParameter( indices, getIndex(), getRoot() );
		}catch( StateNotFoundException statenotfoundexception ){
			return (double)-1;
		}
	}

	private double getParameter( final int[] indices, TableIndex tableindex, DecisionNode node ) throws StateNotFoundException
	{
		if( tableindex == null ) return (double)-1;
		FiniteVariable var = node.getVariable();
		int index = indices[ tableindex.variableIndex(var) ];
		if( node.isLeaf() ) return node.getParameter(index).getValue();
		else{
			DecisionNode next = node.getNext(index);
			return getParameter( indices, tableindex, next );
		}
	}

	private TableIndex myTableIndex;
	private DecisionNode myRoot;
	private DecisionLeaf myDefault;

	/** interface AbstractDirectedGraph */
	protected Set verticesProtected(){
		return getRoot().getDescendants( new HashSet(), Classifier.ALL );
	}

	/** interface Collection */
	public void clear(){
		myDefault.deracinate();
		myRoot = myDefault;
	}

	/** interface DirectedGraph */
	public Object clone(){
		throw new UnsupportedOperationException();
	}
	/** interface DirectedGraph */
	public Set inComing( Object vertex ){
		return ((DecisionNode)vertex).getParents();
	}
	/** interface DirectedGraph */
	public Set outGoing( Object vertex ){
		return ((DecisionNode)vertex).getChildDecisionNodes();
	}
	/** interface DirectedGraph */
	public boolean contains( Object vertex ){
		return getRoot().isDescendant( (DecisionNode)vertex );
	}

	/** interface Factory */
	public Parameter newParameter( double value ){
		ParameterImpl ret = new ParameterImpl( myParameterCounter++, value );
		newParameter( ret );
		return ret;
	}

	/** interface Factory */
	public Parameter newParameter( String id, double value ){
		ParameterImpl ret = new ParameterImpl( id, value );
		newParameter( ret );
		return ret;
	}

	private void newParameter( Parameter param ){
		if( myParameterHistory == null ) myParameterHistory = new LinkedList();
		myParameterHistory.addLast( param );
	}

	/** interface Factory */
	public Parameter clone( Parameter parameter ){
		ParameterImpl ret = new ParameterImpl( parameter.getID() + "_" + ParameterImpl.makeID( myParameterCounter++ ), parameter.getValue() );
		newParameter( ret );
		return ret;
	}

	/** interface Factory */
	public DecisionNode clone( DecisionNode node ){
		DecisionNode ret = (DecisionNode) node.clone();
		String modifiedid = ret.toString();
		if( ret.isLeaf() ){
			addHistory( (DecisionLeaf) ret );
			modifiedid += "_" + ParameterImpl.makeID( myLeafCounter++ );
		}
		else{
			addHistory( (DecisionInternal) ret );
			modifiedid += "_" + ParameterImpl.makeID( myInternalCounter++ );
		}
		ret.setID( modifiedid );
		return ret;
	}

	/** interface Factory */
	public Collection getParameterHistory(){
		if( myParameterHistory == null ) return Collections.EMPTY_LIST;
		else return Collections.unmodifiableList( myParameterHistory );
	}

	/** interface Factory */
	public DecisionLeaf getDefault(){
		return myDefault;
	}

	/** interface Factory */
	public DecisionLeaf newLeaf( FiniteVariable var ){
		DecisionLeaf ret = new DecisionLeaf( var, (Factory)this );
		newLeaf( ret );
		return ret;
	}

	/** interface Factory */
	public DecisionLeaf newLeaf( FiniteVariable var, Parameter[] params ){
		DecisionLeaf ret = new DecisionLeaf( var, params, (Factory)this );
		newLeaf( ret );
		return ret;
	}

	private void newLeaf( DecisionLeaf leaf ){
		leaf.setID( "L" + ParameterImpl.makeID( myLeafCounter++ ) );
		addHistory( leaf );
	}

	/** interface Factory */
	public boolean removeHistory( DecisionLeaf leaf ){
		if( myLeafHistory == null ) return false;
		else if( myLeafHistory.getLast() == leaf ){
			myLeafHistory.removeLast();
			return true;
		}
		else return myLeafHistory.remove( leaf );
	}

	/** interface Factory */
	public void adopt( DecisionNode node )
	{
		if( node == null ) return;
		else if( node.isLeaf() ) newLeaf( (DecisionLeaf)node );
		else newInternal( (DecisionInternal)node );
	}

	public void addHistory( DecisionLeaf leaf ){
		if( myLeafHistory == null ) myLeafHistory = new LinkedList();
		myLeafHistory.addLast( leaf );
	}

	public void addHistory( DecisionInternal internal ){
		if( myInternalHistory == null ) myInternalHistory = new LinkedList();
		myInternalHistory.addLast( internal );
	}

	/** interface Factory */
	public Collection getLeafHistory(){
		if( myLeafHistory == null ) return Collections.EMPTY_LIST;
		else return Collections.unmodifiableList( myLeafHistory );
	}

	/** interface Factory */
	public DecisionInternal newInternal( FiniteVariable var ){
		DecisionInternal ret = new DecisionInternal( var, getDefault() );
		newInternal( ret );
		return ret;
	}

	private void newInternal( DecisionInternal ret ){
		ret.setID( ret.toString() + ParameterImpl.makeID( myInternalCounter++ ) );
		addHistory( ret );
	}

	/** interface Factory */
	public Collection getInternalHistory(){
		if( myInternalHistory == null ) return Collections.EMPTY_LIST;
		else return Collections.unmodifiableList( myInternalHistory );
	}

	/** interface Factory */
	public boolean isUniqueNodeID( String id ){
		if( id == null ) return true;
		if( !isUniqueNodeID( id, myInternalHistory ) ) return false;
		if( !isUniqueNodeID( id, myLeafHistory ) ) return false;
		return true;
	}

	private boolean isUniqueNodeID( String id, Collection history ){
		if( history == null ) return true;
		for( Iterator it = history.iterator(); it.hasNext(); ){
			if( id.equals( it.next().toString() ) ) return false;
		}
		return true;
	}

	/** interface Factory */
	public boolean isUniqueParameterID( String id ){
		if( id == null ) return true;
		if( myParameterHistory == null ) return true;
		for( Iterator it = myParameterHistory.iterator(); it.hasNext(); ){
			if( id.equals( ((Parameter)it.next()).getID() ) ) return false;
		}
		return true;
	}

	/** interface Factory */
	public boolean isValidID( String id ){
		return (id != null) && (id.length() > 0);
	}

	/** interface Factory */
	public Parameter parameterForID( String id ){
		if( id == null ) return (Parameter)null;
		if( myParameterHistory == null ) return (Parameter)null;
		Parameter next;
		for( Iterator it = myParameterHistory.iterator(); it.hasNext(); ){
			if( id.equals( (next = (Parameter)it.next()).getID() ) ) return next;
		}
		return (Parameter)null;
	}

	/** interface Factory */
	public DecisionLeaf leafForID( String id ){
		return (DecisionLeaf) nodeForID( id, myLeafHistory );
	}

	/** interface Factory */
	public DecisionInternal internalForID( String id ){
		return (DecisionInternal) nodeForID( id, myInternalHistory );
	}

	/** interface Factory */
	public DecisionNode nodeForID( String id ){
		DecisionNode ret;
		if( (ret = nodeForID( id, myInternalHistory )) != null ) return ret;
		if( (ret = nodeForID( id, myLeafHistory )) != null ) return ret;
		return (DecisionNode)null;
	}

	private DecisionNode nodeForID( String id, Collection history ){
		if( id == null ) return (DecisionNode)null;
		if( history == null ) return (DecisionNode)null;
		DecisionNode next;
		for( Iterator it = history.iterator(); it.hasNext(); ){
			if( id.equals( (next = (DecisionNode)it.next()).toString() ) ) return next;
		}
		return (DecisionNode)null;
	}

	public void removeHistoryAllDescendants( DecisionNode oldRoot )
	{
		if( (myLeafHistory == null) && (myInternalHistory == null) && (myParameterHistory == null) ) return;
		Set all = oldRoot.getDescendants( new HashSet(), Classifier.ALL );
		if( myLeafHistory != null ) myLeafHistory.removeAll( all );
		if( myInternalHistory != null ) myInternalHistory.removeAll( all );
		if( myParameterHistory == null ) return;
		Set allParameters = new HashSet();
		Object next;
		for( Iterator it = all.iterator(); it.hasNext(); ){
			next = it.next();
			if( next instanceof DecisionLeaf ) ((DecisionLeaf)next).getOutcomes( allParameters );
		}
		myParameterHistory.removeAll( allParameters );
	}

	private int myInternalCounter = (int)0;
	private int myLeafCounter = (int)0;
	private int myParameterCounter = (int)0;
	private LinkedList myLeafHistory, myInternalHistory, myParameterHistory;
	private WeakLinkedList myListeners;
	private DecisionBackup mySnapshot;
	private double myOptimizationEpsilon;
}
