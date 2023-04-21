package edu.ucla.belief.decision;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.StateNotFoundException;
import edu.ucla.util.WeakLinkedList;
import edu.ucla.util.Stringifier;
import edu.ucla.structure.RecursiveDepthFirstIterator;

import java.util.*;

/** @author Keith Cascio
	@since 120804 */
public class DecisionNodeAbstract implements DecisionNode
{
	public DecisionNodeAbstract(){}

	/** @since 011105 */
	public DecisionNodeAbstract( DecisionNodeAbstract toCopy ){
		this.myID = toCopy.myID;
		this.myFlagEditable = toCopy.myFlagEditable;
		if( (toCopy.myListeners != null) && !toCopy.myListeners.isEmpty() )
			this.myListeners = new WeakLinkedList( toCopy.myListeners );
	}

	/** @since 011105 */
	public Object clone(){
		return new DecisionNodeAbstract( (DecisionNodeAbstract)this );
	}

	/** @since 011105 */
	public DecisionNode deepClone( Factory factory ){
		return factory.clone( this );
	}

	public void setID( String id ){
		if( myID == id ) return;
		if( (myID != null) && myID.equals(id) ) return;
		this.myID = id;
		fireDecisionEvent( new DecisionEvent( (DecisionNode)this, DecisionEvent.RENAME ) );
	}

	public String toString(){
		return myID;
	}

	/** @since 020205 */
	final public boolean isDeeplyEquivalent( DecisionNode node, double epsilon, Map checked ){
		if( isChecked( node, checked ) ) return true;
		if( !isDeeplyEquivalentHook( node, epsilon, checked ) ) return false;
		return yesDeeplyEquivalent( node, checked );
	}

	/** override here
		@since 020205 */
	protected boolean isDeeplyEquivalentHook( DecisionNode node, double epsilon, Map checked ){
		return this.equivales( node );
	}

	/** @since 020205 */
	private boolean isChecked( DecisionNode node, Map checked )
	{
		if( checked == null ) return false;

		DecisionNode key = null, value = null;
		if( checked.containsKey( this ) ){
			key = this;
			value = node;
		}
		else if( checked.containsKey( node ) ){
			key = node;
			value = this;
		}

		return ( (key != null) && ((Collection)checked.get(key)).contains( value ) );
	}

	/** @since 020205 */
	private boolean yesDeeplyEquivalent( DecisionNode node, Map checked ){
		//System.out.println( this.toString() + ".yesDeeply( "+node.toString()+" )" );
		if( checked != null )
		{
			DecisionNode key = null;
			Collection collection;
			if( checked.containsKey( this ) ) collection = (Collection) checked.get( key = this );
			else if( checked.containsKey( node ) ) collection = (Collection) checked.get( key = node );
			else checked.put( key = this, collection = new HashSet(1) );

			collection.add( (key==this) ? node : this );
		}

		return true;
	}

	/** @since 010905 */
	public boolean equivales( DecisionNode node ){
		return this.equals( node );
	}

	/** @since 010905 */
	public int equivalenceHashCode(){
		return this.hashCode();
	}

	/** @since 011005 */
	public DecisionBackup deflate(){
		DecisionBackup ret = new DecisionBackup( myID, getVariable(), myFlagEditable );
		ret.setListeners( myListeners );
		return ret;
	}

	/** @since 011005 */
	public DecisionBackup deflate( Map alreadydeflated ){
		return deflate();
	}

	public boolean isLeaf() { return true; }
	public DecisionNode getNext( int index ) throws StateNotFoundException { return (DecisionNode)null; }
	public DecisionNode getNext( Object value ) throws StateNotFoundException { return (DecisionNode)null; }
	public void setNext( int index, DecisionNode next ) throws StateNotFoundException {
		throw new UnsupportedOperationException();
	}
	public void setNext( Object value, DecisionNode next ) throws StateNotFoundException {
		throw new UnsupportedOperationException();
	}
	public boolean removeChild( DecisionNode child ){
		throw new UnsupportedOperationException();
	}
	public Parameter getParameter( int index ) { return (Parameter)null; }
	public Parameter getParameter( Object value ) throws StateNotFoundException { return (Parameter)null; }
	public Set getChildDecisionNodes() { return Collections.EMPTY_SET; }
	public Set getOutcomes( Set container ) { return container; }
	public int numOutcomes() { return 0; }
	public boolean hasOutcome( Object outcome ) { return false; }
	public FiniteVariable getVariable() { return (FiniteVariable)null; }

	public boolean isEditable(){
		return myFlagEditable;
	}

	public void setEditable( boolean flag ){
		this.myFlagEditable = flag;
	}

	public Set getLeaves( Set container ){
		return getDescendants( container, Classifier.LEAVES );
	}

	public Set getAncestors( Set container, Classifier classifier )
	{
		if( container == null ) container = new HashSet();
		if( classifier.isMember( (DecisionNode)this ) ) container.add( this );
		for( Iterator it = getParents().iterator(); it.hasNext(); ){
			((DecisionNode) it.next()).getAncestors( container, classifier );
		}
		return container;
	}

	public Set getDescendants( Set container, Classifier classifier )
	{
		//System.out.println( "DecisionNodeAbstract.getDescendants()" );
		if( container == null ) container = new HashSet();
		if( classifier.isMember( (DecisionNode)this ) ) container.add( this );
		for( Iterator it = getChildDecisionNodes().iterator(); it.hasNext(); ){
			((DecisionNode) it.next()).getDescendants( container, classifier );
		}
		return container;
	}

	public boolean isDescendant( DecisionNode node )
	{
		if( this == node ) return true;
		for( Iterator it = getChildDecisionNodes().iterator(); it.hasNext(); ){
			if( ((DecisionNode) it.next()).isDescendant( node ) ) return true;
		}
		return false;
	}

	/** @since 020205 */
	public Map groupInstancesByOutcome( Map map )
	{
		FiniteVariable var = getVariable();
		int sizeVar = var.size();

		if( map == null ) map = new HashMap( sizeVar );
		Object outcome;
		//List outcomes = new LinkedList();
		Object instance;
		List instances;//Set instances;
		boolean flagLeaf = isLeaf();
		try{
		for( int i=0; i<sizeVar; i++ ){
			outcome = flagLeaf ? (Object)getParameter(i) : (Object)getNext(i);
			instance = var.instance( i );
			if( map.containsKey( outcome ) ) ((Collection)map.get( outcome )).add( instance );
			else{
				//outcomes.add( outcome );
				(instances = new LinkedList()).add( instance );//(instances = new HashSet()).add( instance );
				map.put( outcome, instances );
			}
		}
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: " + statenotfoundexception );
			return (Map) null;
		}

		return map;
	}

	public boolean insertParent( FiniteVariable var, DecisionNode oldParent, Factory factory )
	{
		//DecisionInternal newparent = new DecisionInternal( var, factory.getDefault() );
		DecisionInternal newparent = factory.newInternal( var );
		try{
			newparent.setNext( this );

			//DecisionNode oldParent = getParent();
			if( oldParent != null ){
				FiniteVariable oldParentVar = oldParent.getVariable();
				Object value;
				for( Iterator it = oldParentVar.instances().iterator(); it.hasNext(); ){
					if( oldParent.getNext( value = it.next() ) == this )
						oldParent.setNext( value, newparent );
				}

			}
		}catch( StateNotFoundException statenotfoundexception ){
			return false;
		}
		//removeParent( oldParent );//taken care of by setNext()
		//addParent( newparent );//taken care of by setNext()

		fireDecisionEvent( new DecisionEvent( (DecisionNode)this, newparent ) );

		return true;
	}

	public Set getParents() {
		if( myParents == null ) return Collections.EMPTY_SET;
		else return myParents;
	}

	protected void addParent( DecisionNode parent ){
		if( myParents == null ) myParents = new HashSet(1);
		//if( parent.toString().endsWith( "]" ) )
		//System.out.print( this + ".ap("+parent+"), " );
		myParents.add( parent );
	}

	protected boolean removeParent( DecisionNode parent ){
		//System.out.print( this + ".rp("+parent+"), " );
		if( myParents == null ) return false;
		else return myParents.remove( parent );
	}

	public DecisionNodeAbstract getRoot(){
		if( getParents().isEmpty() ) return this;
		else return ((DecisionNodeAbstract)getParents().iterator().next()).getRoot();
	}

	public void deracinate()
	{
		//System.out.println( "BEGI DecisionNodeAbstract.deracinate()" );

		if( getParents().isEmpty() ) return;

		severNonTreeNodes( this );

		DecisionNode parent;
		for( Iterator it = getParents().iterator(); it.hasNext(); ){
			(parent = (DecisionNode)it.next()).removeChild( (DecisionNode)this );
		}
		if( !getParents().isEmpty() ) throw new IllegalStateException();

		fireDecisionEvent( new DecisionEvent( (DecisionNode)this, DecisionEvent.DERACINATED ) );

		//System.out.println( "DONE DecisionNodeAbstract.deracinate()" );
	}

	public static void severNonTreeNodes( DecisionNodeAbstract newroot )
	{
		//Set setnotrooted = new HashSet();
		//Set setrooted = new HashSet();
		//findAllNotRootedAt( newroot, setnotrooted, setrooted );
		DecisionNode oldRoot = newroot.getRoot();
		if( newroot == oldRoot ) return;

		Set all = oldRoot.getDescendants( new HashSet(), Classifier.ALL );
		Set setrooted = newroot.getDescendants( new HashSet(), Classifier.ALL );
		all.removeAll( setrooted );
		Set setnotrooted = all;

		if( setnotrooted.isEmpty() ) return;

		DecisionNode notrooted;
		Object instance;
		DecisionNode outcome;
		try{
		for( Iterator iterToSever = setnotrooted.iterator(); iterToSever.hasNext(); ){
			notrooted = (DecisionNode) iterToSever.next();
			for( Iterator iterInstances = notrooted.getVariable().instances().iterator(); iterInstances.hasNext(); ){
				instance = iterInstances.next();
				if( setrooted.contains( outcome = notrooted.getNext( instance ) ) ){
					notrooted.removeChild( outcome );
				}
			}
		}
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: severNonTreeNodes() caught: " + statenotfoundexception );
		}

		DecisionTreeImpl dummy = new DecisionTreeImpl( oldRoot );
		RecursiveDepthFirstIterator rdfi = new RecursiveDepthFirstIterator( dummy, setnotrooted );
		if( rdfi.isCyclic() ){
			System.out.println( "severNonTreeNodes() created cycle:" );
			System.out.println( rdfi.cycleToString( rdfi.getCycle(), (Stringifier)null ) );
		}
	}

	public static void findAllNotRootedAt( DecisionNode root, Set setnotrooted, Set setrooted ){
		System.out.println( "findAllNotRootedAt( root:"+root+" )" );
		Set leaves = root.getLeaves( new HashSet() );
		for( Iterator it = leaves.iterator(); it.hasNext(); ){
			((DecisionNodeAbstract)it.next()).findAncestorsNotRootedAt( root, setnotrooted, setrooted );
		}
		System.out.println( "    rooted " + setrooted );
		System.out.println( "       not " + setnotrooted );
	}

	public void findAncestorsNotRootedAt( DecisionNode root, Set setnotrooted, Set setrooted )
	{
		System.out.println( this.toString() + ".fANotRootedAt() parents: " + getParents() );
		DecisionNodeAbstract parent;
		for( Iterator it = getParents().iterator(); it.hasNext(); ){
			parent = (DecisionNodeAbstract)it.next();
			if( (!setnotrooted.contains(parent)) && (!setrooted.contains(parent)) )
				parent.findAncestorsNotRootedAt( root, setnotrooted, setrooted );
		}

		if( setnotrooted.containsAll( getParents() ) && (this != root) ){
			System.out.println( "    " + this.toString() + " -> setnotrooted" );
			setnotrooted.add( this );
		}
		else{
			System.out.println( "    " + this.toString() + " -> setrooted" );
			setrooted.add( this );
		}
	}

	public void addListener( DecisionListener listener ){
		if( myListeners == null ) myListeners = new WeakLinkedList();
		if( !myListeners.contains(listener) ) myListeners.addLast( listener );
	}

	public boolean removeListener( DecisionListener listener ){
		if( myListeners == null ) return false;
		else return myListeners.remove( listener );
	}

	protected void fireDecisionEvent( DecisionEvent e ){
		//System.out.println( "DecisionNodeAbstract.fireDecisionEvent( "+e+" )" );
		if( myListeners == null ) return;
		DecisionListener next;
		for( Iterator it = myListeners.iterator(); it.hasNext(); ){
			next = (DecisionListener)it.next();
			if( next == null ) it.remove();
			else next.decisionEvent( e );
		}
	}

	private WeakLinkedList myListeners;
	private Set myParents;
	private String myID;
	private boolean myFlagEditable = true;
}
