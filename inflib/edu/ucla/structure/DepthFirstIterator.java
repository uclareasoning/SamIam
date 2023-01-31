package edu.ucla.structure;
import java.util.*;


/**
@author Keith Cascio
@since 031502
*/
public class DepthFirstIterator implements Iterator
{
	private static class Mark{}

	private static final Mark WHITE	= new Mark();
	private static final Mark GRAY	= new Mark();
	private static final Mark BLACK	= new Mark();

	public static final java.io.PrintStream STREAM_DEBUG = System.out;

	private void mark( Object target, Mark mark )
	{
		myMarkMap.put( target, mark );
	}

	private Object getMark( Object target )
	{
		return myMarkMap.get( target );
	}

	private boolean DEBUG = false;

	public DepthFirstIterator( DirectedGraph G )
	{
		myGraph = G;
		init();
	}

	private void init()
	{
		if( DEBUG ) STREAM_DEBUG.println( "Iterative DFS debug information:" );
		myStack = new LinkedList();
		myWhites = new HashSet();
		myMarkMap = new HashMap();
		Iterator it = myGraph.iterator();
		Object obj = null;
		while( it.hasNext() )
		{
			obj = it.next();
			mark( obj, WHITE );
			myWhites.add( obj );
		}
	}

	public boolean hasNext()
	{
		return( !myStack.isEmpty() || !myWhites.isEmpty() );
	}

	public Object next()// throws NoSuchElementException
	{
		if( !myStack.isEmpty() )
		{
			ExplicitStackFrame esf = pop();
			if( DEBUG ) STREAM_DEBUG.println( "Popping iterator over " +esf.obj+ "'s children." );
			Object white = nextWhite( esf.it );
			if( white == null )
			{
				if( DEBUG ) STREAM_DEBUG.println( "Blackening (done chilluns) " + esf.obj );
				mark( esf.obj, BLACK );
				return esf.obj;
			}
			else
			{
				push( esf );
				if( DEBUG ) STREAM_DEBUG.println( "Visiting " +white+ " recursively." );
				return descend( white );
			}
		}
		else if( !myWhites.isEmpty() )
		{
			Object obj = myWhites.iterator().next();
			myWhites.remove( obj );
			if( DEBUG ) STREAM_DEBUG.println( "Visiting " +obj+ " from main loop." );
			return descend( obj );
		}
		else throw new NoSuchElementException();
	}

	private Object descend( Object white )
	{
		mark( white, GRAY );
		Object current = white;
		Object temp = null;
		Iterator newIt = null;
		boolean stop = false;

		while( !stop )
		{
			newIt = myGraph.outGoing( current ).iterator();

			temp = nextWhite( newIt );
			if( temp == null ) stop = true;
			else
			{
				if( DEBUG ) STREAM_DEBUG.println( "Visiting " +temp+ " recursively." );
				mark( temp, GRAY );
				push( new ExplicitStackFrame( current, newIt ) );
				current = temp;
			}
		}

		if( DEBUG ) STREAM_DEBUG.println( "Blackening (no white chilluns) " + current );
		mark( current, BLACK );
		return current;
	}

	private Object nextWhite( Iterator it )
	{
		Object current = null;
		while( it.hasNext() )
		{
			current = it.next();
			if( getMark( current ) == WHITE )
			{
				myWhites.remove( current );
				return current;
			}
		}
		return null;
	}

	private class ExplicitStackFrame
	{
		public ExplicitStackFrame( Object obj, Iterator it )
		{
			this.obj = obj;
			this.it = it;
		}
		public Object obj;
		public Iterator it;
	}

	private ExplicitStackFrame pop()
	{
		return (ExplicitStackFrame) myStack.removeFirst();
	}

	private void push( ExplicitStackFrame obj )
	{
		myStack.addFirst( obj );
	}

	public void remove() throws UnsupportedOperationException, IllegalStateException
	{
		throw new UnsupportedOperationException( "DepthFirstIterator does not implement remove()." );
	}

	private Set myWhites = null;
	private Map myMarkMap = null;
	private LinkedList myStack = null;
	private DirectedGraph myGraph = null;
}