/*
* HashGraph.java
*
* Created on August 31, 1999, 7:53 AM
*/
package edu.ucla.structure;
import java.util.*;
import edu.ucla.util.*;
/**
* An implementation of a Graph which stores the nodes and edges in a hash table
* to allow O(1) testing for inclusion,insertion, removal etc.
* @author jpark
* @version
*/
public class HashGraph implements Graph {
    protected HashSet vertices;
    protected HashMap neighbors;
    /** Creates new HashGraph */
    public HashGraph() {
        vertices = new HashSet();
        neighbors = new HashMap();
    }
    public HashGraph(Graph g) {
        vertices = new HashSet();
        neighbors = new HashMap();
        Iterator verts = g.vertices().iterator();
        while (verts.hasNext()) {
            Object v = verts.next();
            add(v);
            Iterator ins = g.neighbors(v).iterator();
            while (ins.hasNext())
                addEdge(ins.next(), v);
        }
    }

    private HashGraph(HashSet v,HashMap n){
	vertices=v;
	neighbors=n;
    }
    public HashGraph(int size) {
        vertices = new HashSet(size);
        neighbors = new HashMap(size);
    }

    public Graph createIsomorphic(Map mapping){
	HashSet nv=Maps.map(mapping,vertices);
	HashMap nneighbs=new HashMap(nv.size());
	for(Iterator iter=neighbors.keySet().iterator();iter.hasNext();){
	    Object obj=iter.next();
	    Object nobj=mapping.get(obj);
	    Set neighbs=(Set)neighbors.get(obj);
	    nneighbs.put(nobj,Maps.map(mapping,neighbs));
	}
	return new HashGraph(nv,nneighbs);
    }
    public Object clone() {
        HashGraph result = new HashGraph();
        result.vertices = (HashSet) vertices.clone();
        result.neighbors = (HashMap) neighbors.clone();
        return result;
    }
    /** Construct an Iterator over all vertices.
     * @returns Iterator over all vertices.
     */
    public Set vertices() {
        return Collections.unmodifiableSet(vertices);
    }
    /** Construct an Iterator over the vertices adjacent to vertex.
     * <p><dd><dl>
     * <dt> <b>Precondition:</b>
     * <dd> vertex is in the graph(tested by "equals").
     * </dl></dd>
     * @param vertex- An Object which is in the graph.
     * @returns Iterator over the vertices adjacent to vertex.
     */
    public Set neighbors(Object vertex) {
        Set s = (Set) neighbors.get(vertex);
        if (s == null)
            return Collections.EMPTY_SET;
        else
            return Collections.unmodifiableSet(s);
    }
    public Set neighboringEdges(Object vertex) {
        Set s = neighbors(vertex);
        Set result = new HashSet(s.size());
        for (Iterator iter = s.iterator(); iter.hasNext();) {
            result.add(new Edge(vertex, iter.next()));
        }
        return result;
    }
    /** Returns the degree of vertex. If vertex is not in the graph, the behavior is undefined.
     * <p><dd><dl>
     * <dt> <b>Precondition:</b>
     * <dd> vertex is in the graph(tested by "equals").
     * </dl></dd>
     * @param vertex- An Object which is in the graph.
     * @returns the number of vertices adjacent to vertex.
     */
    public int degree(Object vertex) {
        Set s = (Set) neighbors.get(vertex);
        if (s == null)
            return 0;
        else
            return s.size();
    }
    /** Returns whether or not a particular edge is in the graph.
     * <p><dd><dl>
     * <dt> <b>Precondition:</b>
     * <dd> vertex1 and vertex2 are in the graph(tested by "equals").
     * </dl></dd>
     * @param vertex1- An Object which is in the graph.
     * @param vertex2- An Object which is in the graph.
     * @returns true if edge (vertex1,vertex2) is in the graph, false otherwise.
     */
    public boolean containsEdge(Object vertex1, Object vertex2) {
        Set s = (Set) neighbors.get(vertex1);
        if (s == null)
            return false;
        else
            return s.contains(vertex2);
    }
    public boolean containsEdge(Edge e) {
        return containsEdge(e.v1(), e.v2());
    }
    /** Returns whether or not a particular Object is a vertex in the graph.
     * @param vertex- Any Object.
     * @returns true if vertex is in the graph(tested by "equals"), false otherwise.
     */
    public boolean contains(Object vertex) {
        return vertices.contains(vertex);
    }
    /** Returns the number of vertices in the graph.
     * @returns the number of vertices in the graph.
     */
    public int size() {
        return vertices.size();
    }
    /** Determines whether or not the graph is acyclic.
     * @returns false if the graph contains a cycle, true otherwise.
     * By convention the empty graph is connected.
     */
    public boolean isAcyclic() {
        throw new UnsupportedOperationException();
    }
    /** Determines whether or not the graph is connected.
     * @returns true if the graph is connected, false otherwise.
     * By convention the empty graph is connected.
     */
    public boolean isConnected() {
        throw new UnsupportedOperationException();
    }
    /** Determines if there is a path from vertex1 to vertex2.
     * <p><dd><dl>
     * <dt> <b>Precondition:</b>
     * <dd> vertex1 and vertex2 are in the graph(tested by "equals").
     * </dl></dd>
     * @param vertex1- An Object which is in the graph.
     * @param vertex2- An Object which is in the graph.
     * @returns true if vertex1 and vertex2 are in the same connected component, false otherwise.
     */
    public boolean isConnected(Object vertex1, Object vertex2) {
        throw new UnsupportedOperationException();
    }
    /** Determines whether or not the graph is a tree.
     * @returns true if the graph is a tree(i.e. it is connected and acyclic), false otherwise.
     * By convention the empty graph is a tree.
     */
    public boolean isTree() {
        throw new UnsupportedOperationException();
    }
    /** Adds vertex to the graph(Optional operation).
     * <p><dd><dl>
     * <dt> <b>Precondition:</b>
     * <dd> vertex is not in the graph(as tested by "equals").
     * <dt> <b>Postcondition:</b>
     * <dd> vertex is in the graph(as tested by "equals").
     * </dl></dd>
     * @param vertex- An Object which is not in the graph.
     */
    public boolean add(Object vertex) {
        boolean result = vertices.add(vertex);
        if (result) {
            neighbors.put(vertex, new HashSet());
        }
        return result;
    }
    /** Removes vertex from the graph(Optional operation).
     * <p><dd><dl>
     * <dt> <b>Precondition:</b>
     * <dd> vertex is an Object in the graph(as tested by "equals")
     * <dt> <b>Postcondition:</b>
     * <dd> vertex is not in the graph(as tested by "equals").
     * </dl></dd>
     * @param vertex- An Object which is currently in the graph.
     */
    public boolean remove(Object vertex) {
        boolean result = vertices.remove(vertex);
        if (result) {
            Set neighborset = (Set) neighbors.get(vertex);
            Iterator iter = neighborset.iterator();
            while (iter.hasNext()) {
                Object vert = iter.next();
                Set s = (Set) neighbors.get(vert);
                s.remove(vertex);
            }
            neighbors.remove(vertex);
        }
        return result;
    }
    /** Adds edge to the graph(Optional operation).
     * <p><dd><dl>
     * <dt> <b>Precondition:</b>
     * <dd> vertex1 and vertex2 are in the graph and the edge (vertex1,vertex2) is not.
     * <dt> <b>Postcondition:</b>
     * <dd> the edge (vertex1,vertex2) is in the graph.
     * </dl></dd>
     * @param vertex1-An Object which is currently in the graph.
     * @param vertex2-An Object which is currently in the graph.
     */
    public boolean addEdge(Object vertex1, Object vertex2) {
        if (vertex1.equals(vertex2)) {
            throw new IllegalArgumentException();
        }
        boolean result = add(vertex1);
        result |= add(vertex2);
        Set s = (Set) neighbors.get(vertex1);
        result |= s.add(vertex2);
        s = (Set) neighbors.get(vertex2);
        result |= s.add(vertex1);
        return result;
    }
    /** Removes edge from the graph(Optional operation).
     * <p><dd><dl>
     * <dt> <b>Precondition:</b>
     * <dd> vertex1 and vertex2 are both vertices of the graph and are adjacent.
     * <dt> <b>Postcondition:</b>
     * <dd> the edge (vertex1,vertex2) is in the graph.
     * </dl></dd>
     * @param vertex1-An Object which is currently in the graph.
     * @param vertex2-An Object which is currently in the graph.
     */
    public boolean removeEdge(Object vertex1, Object vertex2) {
        if (!contains(vertex1) || !contains(vertex2))
            return false;
        Set s = (Set) neighbors.get(vertex1);
        boolean result = s.remove(vertex2);
        if (result) {
            s = (Set) neighbors.get(vertex2);
            s.remove(vertex1);
        }
        return result;
    }
    public boolean retainAll(final java.util.Collection p) {
        throw new UnsupportedOperationException();
    }
    public java.lang.Object[] toArray(java.lang.Object[] p) {
        return vertices.toArray(p);
    }
    public java.lang.Object[] toArray() {
        return vertices.toArray();
    }
    public boolean removeAll(final java.util.Collection p) {
        boolean result = false;
        Iterator iter = p.iterator();
        while (iter.hasNext())
            result |= remove(iter.next());
        return result;
    }
    public java.util.Iterator iterator() {
        return vertices.iterator();
    }
    public void clear() {
        vertices.clear();
        neighbors.clear();
    }
    public int hashCode() {
        return vertices.hashCode() ^ neighbors.hashCode();
    }
    public boolean addAll(final java.util.Collection p) {
        Iterator iter = p.iterator();
        boolean result = false;
        while (iter.hasNext())
            result |= add(iter.next());
        return result;
    }
    public boolean containsAll(final java.util.Collection p) {
        return vertices.containsAll(p);
    }
    public boolean isEmpty() {
        return vertices.isEmpty();
    }
    public boolean equals(final java.lang.Object p) {
        if (p instanceof HashGraph) {
            HashGraph g = (HashGraph) p;
            return vertices.equals(g.vertices) &&
                    neighbors.equals(g.neighbors);
        } else
            return false;
    }
    public static void main(String[] args) {
        HashGraph g = new HashGraph();
        g.add(new Integer(5));
        g.add(new Integer(7));
        g.addEdge(new Integer(3), new Integer(4));
        g.addEdge(new Integer(2), new Integer(5));
        g.addEdge(new Integer(7), new Integer(4));
        g.addEdge(new Integer(3), new Integer(4));
        g.addEdge(new Integer(4), new Integer(2));
        g.addEdge(new Integer(2), new Integer(3));
        g.addEdge(new Integer(3), new Integer(5));
        g.remove(new Integer(5));
        g.add(new Integer(5));
        Graphs.print( g, System.out );
    }
}
