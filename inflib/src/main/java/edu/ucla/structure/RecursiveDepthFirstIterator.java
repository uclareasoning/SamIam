package edu.ucla.structure;

import edu.ucla.util.Stringifier;

import java.util.*;

/**
@author Keith Cascio
@since 032002
*/
public class RecursiveDepthFirstIterator implements Iterator
{
	private static class Mark{}

	private static final Mark WHITE	= new Mark();
	private static final Mark GRAY	= new Mark();
	private static final Mark BLACK	= new Mark();

	private boolean DEBUG = false;
	public static final java.io.PrintStream STREAM_DEBUG = System.out;

	public static final String STR_EDGE = " -> ";

	public RecursiveDepthFirstIterator( DirectedGraph graph )
	{
		this( graph, graph );
	}

	public RecursiveDepthFirstIterator( DirectedGraph graph, Collection vertices )
	{
		this.myGraph = graph;
		this.myVertices = vertices;
		init();
	}

	public boolean isCyclic()
	{
		return this.isCyclic;
	}

	/** @since 110204 */
	public static String cycleToString( List cycle, Stringifier fier )
	{
		if( (cycle==null) || (cycle.isEmpty()) ) return "no cycle found";

		java.io.StringWriter writer = new java.io.StringWriter( cycle.size() * 20 );

		writer.write( "[" );
		Object next;
		String stringified;
		for( Iterator it = cycle.iterator(); it.hasNext(); ){
			next = it.next();
			if( fier == null ) stringified = next.toString();
			else stringified = fier.objectToString( next );
			writer.write( STR_EDGE + stringified );
		}
		writer.write( STR_EDGE + "]" );

		return writer.toString();
	}

	/** @since 110204 */
	public List getCycle(){
		if( (!isCyclic()) || (myCycleMembers==null) || (myCycleMembers.isEmpty()) ) return Collections.EMPTY_LIST;
		return getCycle( myCycleMembers.iterator().next() );
	}

	/** @since 110204 */
	public List getCycle( Object cycleMember ){
		if( !isCyclic() ) return Collections.EMPTY_LIST;
		LinkedList cycle = new LinkedList();
		if( findCycle( cycleMember, cycleMember, cycle, new HashSet() ) ) return cycle;
		else return Collections.EMPTY_LIST;
	}

	/** @since 110204 */
	public boolean findCycle( Object cycleMember, Object current, LinkedList cycle, Set visited )
	{
		Object next;
		for( Iterator it = myGraph.outGoing( current ).iterator(); it.hasNext(); ){
			next = it.next();
			if( !visited.contains(next) ){
				visited.add( next );
				if( (next == cycleMember) || findCycle( cycleMember, next, cycle, visited ) ){
					cycle.addFirst( current );
					return true;
				}
			}
		}
		return false;
	}

	/** @since 110204 */
	public Collection getCycleMembers(){
		if( myCycleMembers == null ) return Collections.EMPTY_SET;
		else return Collections.unmodifiableCollection( myCycleMembers );
	}

	private void mark( Object target, Mark mark )
	{
		myMarkMap.put( target, mark );
	}

	private Mark getMark( Object target )
	{
		return (Mark) myMarkMap.get( target );
	}

	private void init()
	{
		if( DEBUG ) STREAM_DEBUG.println( "Trivial (recursive) DFS debug information:" );
		myMarkMap = new HashMap();
		myOrder = new LinkedList();
		Iterator it = myVertices.iterator();
		Object obj = null;
		while( it.hasNext() )
		{
			mark( it.next(), WHITE );
		}
		it = myVertices.iterator();
		while( it.hasNext() )
		{
			obj = it.next();
			if( getMark( obj ) == WHITE )
			{
				if( DEBUG ) STREAM_DEBUG.println( "Visiting " +obj+ " from main loop." );
				DFS_visit( obj );
			}
		}
		myUnderlyingIterator = myOrder.iterator();
	}

	private void DFS_visit( Object obj )
	{
		mark( obj, GRAY );

		Iterator it = myGraph.outGoing( obj ).iterator();
		Object current = null;
		Object currentMark = null;
		while( it.hasNext() )
		{
			current = it.next();
			currentMark = getMark( current );
			if( currentMark == WHITE )
			{
				if( DEBUG ) STREAM_DEBUG.println( "Visiting " +current+ " recursively." );
				DFS_visit( current );
			}
			else if( currentMark == GRAY )
			{
				isCyclic = true;
				//System.out.println( "Cycle found at: \"" + ((edu.ucla.belief.Variable)current).getID() + "\"" );
				if( myCycleMembers == null ) myCycleMembers = new LinkedList();
				myCycleMembers.add( current );
			}
		}
		if( DEBUG ) STREAM_DEBUG.println( "Blackening " +obj );
		mark( obj, BLACK );
		myOrder.add( obj );
	}

	public boolean hasNext()
	{
		return myUnderlyingIterator.hasNext();
	}

	public Object next()
	{
		return myUnderlyingIterator.next();
	}

	public void remove() throws UnsupportedOperationException, IllegalStateException
	{
		throw new UnsupportedOperationException( "RecursiveDepthFirstIterator does not implement remove()." );
	}

	private boolean isCyclic = false;

	private LinkedList myOrder;
	private Iterator myUnderlyingIterator;
	private Map myMarkMap;
	private DirectedGraph myGraph;
	private Collection myVertices;
	private Collection myCycleMembers;
}