package edu.ucla.belief.decision;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.StateNotFoundException;
import java.util.*;

/** @author Keith Cascio
	@since 120804 */
public class DecisionInternal extends DecisionNodeAbstract implements DecisionNode
{
	public DecisionInternal( FiniteVariable var, DecisionLeaf defaultleaf )
	{
		this.myVariable = var;
		this.myDefault = defaultleaf;
		this.myMap = new HashMap( myVariable.size() );
		myFlagChildrenDirty = true;
		setID( "[ " + myVariable.toString() + " ]" );
		myDefault.addParent( this );
	}

	/** @since 011105 */
	public DecisionInternal( DecisionInternal toCopy ){
		super( toCopy );
		this.myVariable = toCopy.myVariable;
		this.myDefault = toCopy.myDefault;
		this.myFlagChildrenDirty = true;
		this.myMap = new HashMap( myVariable.size() );

		try{
			Object instance;
			for( Iterator it = myVariable.instances().iterator(); it.hasNext(); ){
				instance = it.next();
				this.setNext( instance, toCopy.getNext( instance ) );
			}
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: " + statenotfoundexception );
		}
	}

	/** @since 011105 */
	public Object clone(){
		return new DecisionInternal( (DecisionInternal)this );
	}

	/** @since 011105 */
	public DecisionNode deepClone( Factory factory ){
		DecisionInternal ret = (DecisionInternal) factory.clone( this );//new DecisionInternal( (DecisionInternal)this );

		Map alreadycloned = new HashMap( myMap.values().size() );
		Object instance;
		DecisionNode origoutcome;
		DecisionNode clonedoutcome;
		for( Iterator it = myVariable.instances().iterator(); it.hasNext(); ){
			instance = it.next();
			origoutcome = (DecisionNode) myMap.get( instance );
			if( alreadycloned.containsKey( origoutcome ) )
				clonedoutcome = (DecisionNode) alreadycloned.get( origoutcome );
			else
				alreadycloned.put( origoutcome, clonedoutcome = origoutcome.deepClone( factory ) );
			try{
				setNext( instance, clonedoutcome );
			}catch( StateNotFoundException statenotfoundexception ){
				System.err.println( "Warning: " + statenotfoundexception );
			}
		}

		return ret;
	}

	/** @since 011005 */
	public DecisionBackup deflate(){
		DecisionBackup ret = super.deflate();
		ret.deflateOutcomes( myMap, (Map)null );
		return ret;
	}

	/** @since 011005 */
	public DecisionBackup deflate( Map alreadydeflated ){
		DecisionBackup ret = super.deflate( alreadydeflated );
		ret.deflateOutcomes( myMap, alreadydeflated );
		return ret;
	}

	/** @since 020205 */
	protected boolean isDeeplyEquivalentHook( DecisionNode node, double epsilon, Map checked )
	{
		DecisionInternal other = (DecisionInternal) node;
		if( this.myVariable != other.myVariable ) return false;
		int size = myVariable.size();

		Object instance;
		DecisionNode thisoutcome, otheroutcome;
		try{
			for( int i=0; i<size; i++ ){
				instance = myVariable.instance(i);
				thisoutcome = this.getNext(instance);
				otheroutcome = other.getNext(instance);
				if( !thisoutcome.isDeeplyEquivalent( otheroutcome, epsilon, checked ) ) return false;
			}
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: " + statenotfoundexception );
			return false;
		}

		/*Map thisgrouping = this.groupInstancesByOutcome( new HashMap( myMap.size() ) );
		Map othergrouping = other.groupInstancesByOutcome( new HashMap( other.myMap.size() ) );

		DecisionNode thisoutcome;
		Collection thisinstances, otherinstances;
		for( Iterator it = thisgrouping.keySet().iterator(); it.hasNext(); ){
			thisoutcome = (DecisionNode) it.next();
			if( !othergrouping.containsKey( thisoutcome ) ) return false;
			thisinstances = (Collection) thisgrouping.get( thisoutcome );
			otherinstances = (Collection) othergrouping.get( thisoutcome );
			if( !otherinstances.equals( thisinstances ) ) return false;
		}*/

		return true;
	}

	public boolean equivales( DecisionNode node )
	{
		DecisionInternal other = (DecisionInternal) node;
		if( this.myVariable != other.myVariable ) return false;

		int size = myVariable.size();
		Object instance;
		try{
			for( int i=0; i<size; i++ ){
				instance = myVariable.instance(i);
				if( this.getNext(instance) != other.getNext(instance) ) return false;
			}
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: " + statenotfoundexception );
			return false;
		}

		return true;
	}

	public int equivalenceHashCode(){
		return 31 * myMap.hashCode() + myVariable.hashCode();
	}

	public boolean isLeaf() { return false; }

	public DecisionNode getNext( int index ) throws StateNotFoundException {
		if( (index < 0) || (myVariable.size() <= index) ) throw new IllegalArgumentException();
		Object value = myVariable.instance( index );
		return getNext( value );
	}

	public DecisionNode getNext( Object value ) throws StateNotFoundException {
		if( !myVariable.contains( value ) ) throw new StateNotFoundException( myVariable, value );
		if( myMap.containsKey( value ) ) return (DecisionNode) myMap.get( value );
		else return myDefault;
	}

	public void setNext( int index, DecisionNode next ) throws StateNotFoundException {
		if( (index < 0) || (myVariable.size() <= index) ) throw new IllegalArgumentException();
		Object value = myVariable.instance( index );
		setNext( value, next );
	}
	public void setNext( Object value, DecisionNode next ) throws StateNotFoundException
	{
		if( !myVariable.contains( value ) ) throw new StateNotFoundException( myVariable, value );

		DecisionNodeAbstract oldNext = (DecisionNodeAbstract) getNext( value );
		myMap.put( value, next );
		((DecisionNodeAbstract)next).addParent( (DecisionNode)this );

		if( oldNext != null ){
			recreateChildrenSet();
			if( !myChildren.contains( oldNext ) ) oldNext.removeParent( (DecisionNode)this );
		}
		else myFlagChildrenDirty = true;

		fireDecisionEvent( new DecisionEvent( (DecisionNode)this, DecisionEvent.ASSIGNMENT_CHANGE ) );
	}

	public void setNext( DecisionNode next )
	{
		LinkedList oldNexts = new LinkedList();
		Object value;
		Object mapping;
		try{
		for( Iterator it = myVariable.instances().iterator(); it.hasNext(); ){
			value = it.next();
			mapping = getNext(value);
			if( mapping != null ) oldNexts.add( mapping );
			myMap.put( value, next );
		}
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: DecisionInternal.setNext() caught: " + statenotfoundexception );
			return;
		}
		((DecisionNodeAbstract)next).addParent( (DecisionNode)this );

		if( myChildren == null ) myChildren = new HashSet( getVariable().size() );
		else myChildren.clear();
		myChildren.add( next );
		myFlagChildrenDirty = false;

		DecisionNodeAbstract oldNext;
		for( Iterator it = oldNexts.iterator(); it.hasNext(); ){
			oldNext = (DecisionNodeAbstract) it.next();
			if( oldNext != next ) oldNext.removeParent( (DecisionNode)this );
		}
	}

	public boolean removeChild( DecisionNode child )
	{
		if( getChildDecisionNodes().contains( child ) ){
			Object value;
			for( Iterator it = myVariable.instances().iterator(); it.hasNext(); ){
				if( myMap.get( value = it.next() ) == child ) myMap.remove( value );
			}
			((DecisionNodeAbstract)child).removeParent( this );
			fireDecisionEvent( new DecisionEvent( (DecisionNode)this, DecisionEvent.ASSIGNMENTS_DELETED ) );
			return true;
		}
		else return false;
	}

	public Set getChildDecisionNodes() {
		//return myMap.values();
		if( myFlagChildrenDirty ) recreateChildrenSet();
		return Collections.unmodifiableSet( myChildren );
	}

	/** @since 020105 */
	public int numOutcomes(){
		if( myFlagChildrenDirty ) recreateChildrenSet();
		return myChildren.size();
	}

	/** @since 011605 */
	public Set getOutcomes( Set container ){
		if( myFlagChildrenDirty ) recreateChildrenSet();
		if( container == null ) container = new HashSet( myChildren );
		else container.addAll( myChildren );
		return container;
	}

	/** @since 011605 */
	public boolean hasOutcome( Object outcome ){
		return myMap.values().contains( outcome );
	}

	private void recreateChildrenSet()
	{
		if( myChildren == null ) myChildren = new HashSet( getVariable().size() );
		else myChildren.clear();
		//Collection values = myMap.values();
		//myChildren.addAll( values );
		//myChildren.add( myDefault );
		Object mapping;
		for( Iterator it = myVariable.instances().iterator(); it.hasNext(); ){
			mapping = myMap.get( it.next() );
			if( (mapping == null) && (myDefault!=null) ) myChildren.add( myDefault );
			else myChildren.add( mapping );
		}
		myFlagChildrenDirty = false;
	}

	public FiniteVariable getVariable() { return myVariable; }

	private FiniteVariable myVariable;
	private Map myMap;
	private Set myChildren;
	private boolean myFlagChildrenDirty = true;
	private DecisionLeaf myDefault;
}
