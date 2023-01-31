package edu.ucla.structure;

import edu.ucla.belief.io.NodeLinearTask;

import java.util.*;

/**
* The interface describing directed graphs. Any object may be a vertex. Following
* the convention of the Collection interface, some operations are optional. In this case
* the modification methods are all optional allowing for mutable or immutable graphs. Typically
* the definition of a directed graph requires a non-empty set of vertices. We will slightly abuse the
* term by allowing empty directed graphs(graphs with no vertices). This is done in order to simplify
* incremental creation and destruction of the graph, as well as to conform to the conventions
* of the Collection interface.
* The inspiration for this interface comes from the book Structures in Java. In many respects
* the implementation is similar.
*
* Passing an Object that is not a vertex when a vertex is called for is considered bad
* form, so behavior may vary from implementation to implementation.
*/
public interface DirectedGraph extends Collection, Cloneable
{
	/** @since 20020524 */
	public Object clone();

	/**
		@return A List over the vertices of the graph, in topological order.
		@author Keith Cascio
		@since 031502
	*/
	public List topologicalOrder();

	/** @since 20020603 */
	public void replaceVertex( Object oldVertex, Object newVertex );

	/** @since 20060520 */
	public void replaceVertices( Map verticesOldToNew, NodeLinearTask task );

	/**
		<p>
		Check whether adding the proposed edge
		would keep the graph acyclic.
		<p>
		Precondition: the graph is acyclic.

		@author Keith Cascio
		@since 080602
	*/
	public boolean maintainsAcyclicity( Object vertex1, Object vertex2 );

	/**
	* Constructs an Iterator over all vertices.
	* @return Iterator over all vertices.
	*/
	public Set vertices();
	/**
	* Constructs an Iterator over the vertices adjacent to edges entering the specified vertex.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex is in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is in the graph.
	* @return Iterator over the vertices adjacent to edges entering vertex.
	*/
	public Set inComing(Object vertex);
	/**
	* Constructs an Iterator over the vertices adjacent to edges leaving the specified vertex.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex is in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is in the graph.
	* @return Iterator over the vertices adjacent to edges leaving vertex.
	*/
	public Set outGoing(Object vertex);
	/**
	* Returns the degree of the vertex. This includes both in and out edges
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex is in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is in the graph.
	* @return the number of vertices adjacent to vertex.
	*/
	public int degree(Object vertex);
	/**
	* Returns the number of edges entering the vertex.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex is in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is in the graph.
	* @return the number of edges entering the vertex.
	*/
	public int inDegree(Object vertex);
	/**
	* Returns the number of edges leaving the vertex.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex is in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is in the graph.
	* @return the number of edges leaving the vertex.
	*/
	public int outDegree(Object vertex);
	/**
	* Returns whether or not a particular edge is in the graph.
	* The edge leaves vertex1 and enters vertex2.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex1 and vertex2 are in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex1- An Object which is in the graph.
	* @param vertex2- An Object which is in the graph.
	* @return true if edge (vertex1,vertex2) is in the graph, false otherwise.
	*/
	public boolean containsEdge(Object vertex1, Object vertex2);
	/**
	* Returns whether or not a particular Object is a vertex in the graph.
	* @param vertex- Any Object.
	* @return true if vertex is in the graph(tested by "equals"), false otherwise.
	*/
	public boolean contains(Object vertex);
	/**
	* Returns the number of vertices in the graph.
	* @return the number of vertices in the graph.
	*/
	public int size();

	public int numEdges();
	/**
	* Determines whether or not the graph is acyclic.
	* Cycles are considered in the dirrected sense.
	* Ex A->B,A->C,B->C is acyclic while A->B,B->C,C->A is not.
	* It is equivalent to asking if it is a DAG.
	* By convention the empty graph is connected.
	* @return false if the graph contains a cycle, true otherwise.
	*/
	public boolean isAcyclic();
	/**
	* Determines whether or not the graph is weakly connected.
	* This is the same as asking if the undirected graph produced by eliminating
	* the directionality of the edges is connected.
	* By convention the empty graph is connected.
	* @return true if the graph is connected, false otherwise.
	*/
	public boolean isWeaklyConnected();
	/**
	* Determines if there is an undirected path from vertex1 to vertex2.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex1 and vertex2 are in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex1- An Object which is in the graph.
	* @param vertex2- An Object which is in the graph.
	* @return true if there is an undirected path from vertex1 to vertex2.
	*/
	public boolean isWeaklyConnected(Object vertex1, Object vertex2);
	/**
	* Determines if there is a directed path from vertex1 to vertex2.
	* <p><dd><dl>
	* <dt> <b>Precondition:</b>
	* <dd> vertex1 and vertex2 are in the graph(tested by "equals").
	* </dl></dd>
	* @param vertex1- An Object which is in the graph.
	* @param vertex2- An Object which is in the graph.
	* @return true if there is a directed path from vertex1 to vertex2.
	*/
	public boolean hasPath(Object vertex1, Object vertex2);
	/**
	* Determines whether or not the graph is a singly connected.
	* A singly connected digraph is a directed graph in which there is exactly one
	* undirected path between any two vertices.
	* By convention the empty graph is singly connected.
	* @return true if the graph is a singly connected, false otherwise.
	*/
	public boolean isSinglyConnected();
	/**
	* Adds vertex to the graph(Optional operation). If the vertex is already a member
	* of the graph, the graph is unchanged and the method returns false, following the
	* Collection convention.
	* <p><dd><dl>
	* <dt> <b>Postcondition:</b>
	* <dd> vertex is in the graph(as tested by "equals").
	* </dl></dd>
	* @param vertex- An Object to be added as a vertex.
	* @return true if the graph was modified(i.e. vertex was not
	* a vertex already) false otherwise.
	*/
	public boolean addVertex(Object vertex);
	/**
	* Removes vertex from the graph(Optional operation). If the vertex is not a member
	* of the graph, the method returns false and leaves the graph unchanged. If the
	* parameter is a vertex of the graph, it is removed and the method returns true.
	* <p><dd><dl>
	* <dt> <b>Postcondition:</b>
	* <dd> vertex is not in the graph(as tested by "equals").
	* </dl></dd>
	* @param vertex- An Object which is currently in the graph.
	*/
	public boolean removeVertex(Object vertex);
	/**
	* Adds the directed edge to the graph(Optional operation). If either of the vertices
	* are not in the graph, they are added, and the edge is added. If the edge was
	* already in the graph, it returns false, otherwise it returns true.
	* <p><dd><dl>
	* <dt> <b>Postcondition:</b>
	* <dd> the edge (vertex1,vertex2) is in the graph.
	* </dl></dd>
	*/
	public boolean addEdge(Object vertex1, Object vertex2);
	/**
	* Removes the directed edge from the graph(Optional operation). If the edge is
	* not in the graph when the call is made, it returns false, otherwise it returns true.
	* <p><dd><dl>
	* <dt> <b>Postcondition:</b>
	* <dd> the edge (vertex1,vertex2) is in the graph.
	* </dl></dd>
	*/
	public boolean removeEdge(Object vertex1, Object vertex2);
}
