/*
* Graph.java
*
* Created on August 31, 1999, 7:46 AM
*/
package edu.ucla.structure;
import java.util.*;
/**
* The interface describing undirected graphs. Any object may be a vertex.
* Following the convention of the Collection interface, some operations
* are optional. In this case the modification methods are all optionali
* allowing for mutable or immutable graphs. Typically the definition of
* a graph requires a non-empty set of vertices. We will slightly abuse the
* term by allowing empty graphs(graphs with no vertices). This is done in
* order to simplify incremental creation and destruction of the graph, as
* well as to conform to the conventions of the Collection interface.
* The inspiration for this interface comes from the book Structures in Java.i
* In many respects the implementation is similar.
*/
public interface Graph extends Collection {
    /**
     * Return the set of verticies.
     * @returns the set of vertices.
     */
    public Set vertices();
    /**
    * Construct an Iterator over the vertices adjacent to vertex.
    * <p><dd><dl>
    * <dt> <b>Precondition:</b>
    * <dd> vertex is in the graph(tested by "equals").
    * </dl></dd>
    * @param vertex- An Object which is in the graph.
    * @returns Iterator over the vertices adjacent to vertex.
    */
    public Set neighbors(Object vertex);
    public Set neighboringEdges(Object vertex);
    /**
    * Returns the degree of vertex. If vertex is not in the graph,i
    * the behavior is undefined.
    * <p><dd><dl>
    * <dt> <b>Precondition:</b>
    * <dd> vertex is in the graph(tested by "equals").
    * </dl></dd>
    * @param vertex- An Object which is in the graph.
    * @returns the number of vertices adjacent to vertex.
    */
    public int degree(Object vertex);
    /**
    * Returns whether or not a particular edge is in the graph.
    * <p><dd><dl>
    * <dt> <b>Precondition:</b>
    * <dd> vertex1 and vertex2 are in the graph(tested by "equals").
    * </dl></dd>
    * @param vertex1- An Object which is in the graph.
    * @param vertex2- An Object which is in the graph.
    * @returns true if edge (vertex1,vertex2) is in the graph, false otherwise.
    */
    public boolean containsEdge(Object vertex1, Object vertex2);
    public boolean containsEdge(Edge e);
    /**
    * Returns whether or not a particular Object is a vertex in the graph.
    * @param vertex- Any Object.
    * @returns true if vertex is in the graph(tested by "equals"),
    * false otherwise.
    */
    public boolean contains(Object vertex);
    /**
    * Returns the number of vertices in the graph.
    * @returns the number of vertices in the graph.
    */
    public int size();
    /**
    * Determines whether or not the graph is acyclic.
    * @returns false if the graph contains a cycle, true otherwise.
    * By convention the empty graph is connected.
    */
    public boolean isAcyclic();
    /**
    * Determines whether or not the graph is connected.
    * @returns true if the graph is connected, false otherwise.
    * By convention the empty graph is connected.
    */
    public boolean isConnected();
    /**
    * Determines if there is a path from vertex1 to vertex2.
    * <p><dd><dl>
    * <dt> <b>Precondition:</b>
    * <dd> vertex1 and vertex2 are in the graph(tested by "equals").
    * </dl></dd>
    * @param vertex1- An Object which is in the graph.
    * @param vertex2- An Object which is in the graph.
    * @returns true if vertex1 and vertex2 are in the same connected
    * component, false otherwise.
    */
    public boolean isConnected(Object vertex1, Object vertex2);
    /**
    * Determines whether or not the graph is a tree.
    * @returns true if the graph is a tree(i.e. it is connected and acyclic),
    * false otherwise.
    * By convention the empty graph is a tree.
    */
    public boolean isTree();
    /**
    * Adds vertex to the graph(Optional operation).
    * <p><dd><dl>
    * <dt> <b>Precondition:</b>
    * <dd> vertex is not in the graph(as tested by "equals").
    * <dt> <b>Postcondition:</b>
    * <dd> vertex is in the graph(as tested by "equals").
    * </dl></dd>
    * @param vertex- An Object which is not in the graph.
    */
    public boolean add(Object vertex);
    /**
    * Removes vertex from the graph(Optional operation).
    * <p><dd><dl>
    * <dt> <b>Precondition:</b>
    * <dd> vertex is an Object in the graph(as tested by "equals")
    * <dt> <b>Postcondition:</b>
    * <dd> vertex is not in the graph(as tested by "equals").
    * </dl></dd>
    * @param vertex- An Object which is currently in the graph.
    */
    public boolean remove(Object vertex);
    /**
    * Adds edge to the graph(Optional operation).
    * <p><dd><dl>
    * <dt> <b>Precondition:</b>
    * <dd> vertex1 and vertex2 are in the graph and the edge (vertex1,vertex2)
    * is not.
    * <dt> <b>Postcondition:</b>
    * <dd> the edge (vertex1,vertex2) is in the graph.
    * </dl></dd>
    * @param vertex1-An Object which is currently in the graph.
    * @param vertex2-An Object which is currently in the graph.
    */
    public boolean addEdge(Object vertex1, Object vertex2);
    /**
    * Removes edge from the graph(Optional operation).
    * <p><dd><dl>
    * <dt> <b>Precondition:</b>
    * <dd> vertex1 and vertex2 are both vertices of the graph and are adjacent.
    * <dt> <b>Postcondition:</b>
    * <dd> the edge (vertex1,vertex2) is in the graph.
    * </dl></dd>
    * @param vertex1-An Object which is currently in the graph.
    * @param vertex2-An Object which is currently in the graph.
    */
    public boolean removeEdge(Object vertex1, Object vertex2);

    public Graph createIsomorphic(Map mapping);
}
