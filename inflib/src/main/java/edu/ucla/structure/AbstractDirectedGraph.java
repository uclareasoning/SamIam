package edu.ucla.structure;

import edu.ucla.belief.io.NodeLinearTask;

import java.util.*;

/** Convenience class for implementing DirectedGraph.
	You must implement only 5 methods,
	but you probably want to override others
	for performance reasons.

	@author Keith Cascio
	@since 121504 */
public abstract class AbstractDirectedGraph implements DirectedGraph, Collection, Cloneable
{
	public static final java.io.PrintStream STREAM_DEBUG = System.out;

	/*	YOU MUST IMPLEMENT */
	abstract protected Set verticesProtected();
	/** interface Collection */
	//public void clear();
	/** interface DirectedGraph */
	abstract public Object clone();
	//public Set inComing(Object vertex);
	//public Set outGoing(Object vertex);
	//public boolean contains(Object vertex);

	/*	YOU MIGHT IMPLEMENT */
	//public int hashCode();
	//public boolean equals(final java.lang.Object p);

	public int degree(Object vertex){
		return inDegree(vertex) + outDegree(vertex);
	}

	public int inDegree(Object vertex){
		return inComing(vertex).size();
	}

	public int outDegree(Object vertex){
		return outGoing(vertex).size();
	}

	public boolean containsEdge( Object vertex1, Object vertex2 ){
		return outGoing( vertex1 ).contains( vertex2 );
	}

	public int numEdges(){
		int count = 0;
		Set vertices = verticesProtected();
		for( Iterator it = vertices.iterator(); it.hasNext(); ){
			count += outDegree( it.next() );
		}
		return count;
	}

	/**
		@return An iterator over the vertices in the graph,
		guaranteed to return the vertices in depth first order.
		@author Keith Cascio
		@since 031502
	*/
	public Iterator depthFirstIterator(){
		return new DepthFirstIterator( this );
	}

	/**
		<p>
		Check whether adding the proposed edge
		would keep the graph acyclic.
		<p>
		Precondition: the graph is acyclic.

		@author Keith Cascio
		@since 080602
	*/
	public boolean maintainsAcyclicity( Object vertex1, Object vertex2 )
	{
		return !hasPath( vertex2, vertex1 );
	}

	public Set vertices(){
		return Collections.unmodifiableSet( verticesProtected() );
	}

	public int size() { return verticesProtected().size(); }

	/** Determines whether or not the graph is acyclic.
	* Cycles are considered in the dirrected sense.
	* Ex A->B,A->C,B->C is acyclic while A->B,B->C,C->A is not.
	* It is equivalent to asking if it is a DAG.
	* By convention the empty graph is connected.
	* @return false if the graph contains a cycle, true otherwise.
	*/
	public boolean isAcyclic(){
		return !((new RecursiveDepthFirstIterator(this)).isCyclic());
	}

	public void replaceVertex( Object oldVertex, Object newVertex ){ unsupported(); }

	public void replaceVertices( Map verticesOldToNew, NodeLinearTask task ){ unsupported(); }

	/** Determines whether or not the graph is weakly connected.
	* This is the same as asking if the undirected graph produced by eliminating
	* the directionality of the edges is connected.
	* By convention the empty graph is connected.
	* @return true if the graph is connected, false otherwise.
	*/
	public boolean isWeaklyConnected(){ return unsupported(); }

	/** Determines if there is an undirected path from vertex1 to vertex2.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex1 and vertex2 are in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex1- An Object which is in the graph.
	* @param vertex2- An Object which is in the graph.
	* @return true if there is an undirected path from vertex1 to vertex2.
	*/
	public boolean isWeaklyConnected(Object vertex1, Object vertex2){ return unsupported(); }

	/** Determines if there is a directed path from vertex1 to vertex2.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex1 and vertex2 are in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex1- An Object which is in the graph.
	* @param vertex2- An Object which is in the graph.
	* @return true if there is a directed path from vertex1 to vertex2.
	*/
	public boolean hasPath( Object vertex1, Object vertex2 )
	{
		Set out = outGoing( vertex1 );
		if( out.contains( vertex2 ) ) return true;
		else
		{
			Object tempVertex = null;
			for( Iterator it = out.iterator(); it.hasNext(); )
			{
				tempVertex = it.next();
				if( hasPath( tempVertex, vertex2 ) ) return true;
			}
		}

		return false;
	}

	/** Determines whether or not the graph is a singly connected.
	* A singly connected digraph is a directed graph in which there is exactly one
	* undirected path between any two vertices.
	* By convention the empty graph is singly connected.
	* @return true if the graph is a singly connected, false otherwise.
	*/
	public boolean isSinglyConnected(){ return unsupported(); }

	public boolean addVertex(Object vertex){ return unsupported(); }

	public boolean removeVertex(Object vertex){ return unsupported(); }

	public boolean addEdge(Object vertex1, Object vertex2){ return unsupported(); }

	public boolean removeEdge(Object vertex1, Object vertex2){ return unsupported(); }

	/**
		@return A List over the vertices of the graph, in topological order.
		@author Keith Cascio
		@since 031502
	*/
	public List topologicalOrder()
	{
		//boolean debug = true;

		//if( debug ) STREAM_DEBUG.println( "Topological ordering:" );//debug

		LinkedList K = new LinkedList();
		List ret = new ArrayList( this.size() );

		Set vertices = this.vertices();
		LinkedList verticesList = new LinkedList();
		int[] I = new int[ this.size() ];

		//setup K, I
		Iterator it = vertices.iterator();
		Object currentVertex = null;
		int currentInDegree = (int)0;
		int currentIndex = (int)0;
		while( it.hasNext() )
		{
			currentVertex = it.next();
			currentInDegree = this.inDegree( currentVertex );
			verticesList.add( currentVertex );
			I[currentIndex++] = currentInDegree;
			if( currentInDegree == (int)0 ) K.add( currentVertex );
		}

		//if( debug ) STREAM_DEBUG.println( "initial K: " + K );//debug

		Set outgoing = null;
		Object currentChild = null;

		while( !K.isEmpty() )
		{
			currentVertex = K.removeFirst();
			ret.add( currentVertex );
			//if( debug ) STREAM_DEBUG.println( "Adding to order: " + currentVertex );//debug

			outgoing = this.outGoing( currentVertex );
			//if( debug ) STREAM_DEBUG.println( currentVertex + " outgoing: " + outgoing );//debug
			it = outgoing.iterator();
			while( it.hasNext() )
			{
				currentChild = it.next();
				if( --I[ verticesList.indexOf(currentChild) ] == (int)0 )
				{
					//if( debug ) STREAM_DEBUG.println( "Adding to K: " + currentChild );//debug
					K.add( currentChild );
				}
			}
		}

		return ret;
	}

	/** @return A List over the vertices of the graph, in topological order.
		@author keith cascio
		@since  2002031402
		@deprecated	This version is slow.	Replaced by {@link #topologicalOrder()} */
  /*public List topologicalOrder2(){
		boolean debug = false;

		if( debug ) STREAM_DEBUG.println( "Topological ordering:" );//debug

		HashDirectedGraph dummy = (HashDirectedGraph)(this.clone());

		Set K = new HashSet();
		List ret = new ArrayList( dummy.size() );

		Set vertices = dummy.vertices();

		//setup K
		Iterator it = vertices.iterator();
		Object currentVertex = null;
		while( it.hasNext() )
		{
			currentVertex = it.next();
			if( dummy.inDegree( currentVertex ) == (int)0 ) K.add( currentVertex );
		}

		if( debug ) STREAM_DEBUG.println( "initial K:" + K );//debug

		Set outgoing = null;
		Object currentChild = null;

		while( !dummy.isEmpty() )
		{
			currentVertex = K.iterator().next();
			K.remove( currentVertex );
			outgoing = dummy.outGoing( currentVertex );
			if( debug ) STREAM_DEBUG.println( currentVertex + " outgoing: " + outgoing );//debug
			it = outgoing.iterator();
			dummy.remove( currentVertex );
			while( it.hasNext() )
			{
				currentChild = it.next();
				if( dummy.inDegree( currentChild ) == (int)0 )
				{
					if( debug ) STREAM_DEBUG.println( "Adding to K:" + currentChild );//debug
					K.add( currentChild );
				}
			}

			ret.add( currentVertex );
			if( debug ) STREAM_DEBUG.println( "Adding to order:" + currentVertex );//debug
		}

		return ret;
	}*/

	/** @author Keith Cascio
		@since 100102 */
	public final boolean add( Object obj ){
		return addVertex( obj );
	}

	/** @author Keith Cascio
		@since 100102 */
	public final boolean remove( Object obj ){
		return removeVertex( obj );
	}

	public boolean retainAll(final java.util.Collection p1){ return unsupported(); }

	public java.lang.Object[] toArray(java.lang.Object[] p){
		return verticesProtected().toArray(p);
	}

	public java.lang.Object[] toArray(){
		return verticesProtected().toArray();
	}

	public boolean removeAll(final java.util.Collection p)
	{
		Iterator iter = p.iterator();
		boolean result = false;
		while (iter.hasNext())
		result |= remove(iter.next());
		return result;
	}

	public java.util.Iterator iterator(){
		return verticesProtected().iterator();
	}

	public boolean addAll(final java.util.Collection p)
	{
		Iterator iter = p.iterator();
		boolean result = false;
		while (iter.hasNext())
		result |= add(iter.next());
		return result;
	}

	public boolean containsAll(final java.util.Collection p)
	{
		Iterator iter = p.iterator();
		while (iter.hasNext())
		if (!contains(iter.next()))
		return false;
		return true;
	}

	public boolean isEmpty(){
		return verticesProtected().isEmpty();
	}

	protected boolean unsupported(){
		throw new UnsupportedOperationException();
	}
}
