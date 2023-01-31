/*
* Graphs.java
*
* Created on September 17, 1999, 8:44 AM
*/
package edu.ucla.structure;
import java.util.*;
import edu.ucla.belief.*;
/**
* A set of static routines for Graphs and DirectedGraphs.
* @author jpark
* @version
*/
public class Graphs extends Object {
    /** Creates new Graphs */
    private Graphs() {
    }

    /**
     * Returns the moral graph of dg.
     */
    public static Graph moralGraph( DirectedGraph dg ) {
    	return moralGraph( dg, dg, (java.io.PrintStream)null );
    }

    /** @since 20051017 */
    public static Graph moralGraph( DirectedGraph dg, Collection variables, java.io.PrintStream streamDebug ) {
        Graph result = new HashGraph();
        Iterator iter = dg.vertices().iterator();
        while (iter.hasNext()) {
            Object vertex = iter.next();
            result.add(vertex);
            Set parents = dg.inComing(vertex);

            if( (streamDebug != null) && !variables.containsAll( parents ) ){
            	streamDebug.println( "Invalid parents returned: " + parents );
        	}

            for (Iterator piter = parents.iterator(); piter.hasNext();) {
                Object par = piter.next();
                result.addEdge(vertex, par);
                for (Iterator iter2 = parents.iterator();
                        iter2.hasNext();) {
                    Object par2 = iter2.next();
                    if (!par.equals(par2))
                        result.addEdge(par2, par);
                }
            }
        }
        return result;
    }

    /**
     * Returns the width of the graph using the elimination order specified.
     */
    public static int width(DirectedGraph g, List order) {
        return width(moralGraph(g), order);
    }
    /**
     * Returns the width of the graph using the elimination order specified.
     */
    public static int width(Graph g, List order) {
        int w = 0;
        Graph g2 = new HashGraph(g);
        Iterator iter = order.iterator();
        while (iter.hasNext()) {
            Object node = iter.next();
            int n = g2.degree(node);
            if (n > w)
                w = n;
            Set s = new HashSet(g2.neighbors(node));
            g2.remove(node);
            for (Iterator i1 = s.iterator(); i1.hasNext();) {
                Object v1 = i1.next();
                for (Iterator i2 = s.iterator(); i2.hasNext();) {
                    Object v2 = i2.next();
                    if (!v1.equals(v2))
                        g2.addEdge(v1, v2);
                }
            }
        }
        return w;
    }

    /**
     * Returns the weighted width of the graph using the elimination order specified (as specified in
     *  Darwiche & Hopkins 2001).
     */
    public static double weightedwidth(DirectedGraph g, List order) {
        return weightedwidth(moralGraph(g), order);
    }
    /**
     * Returns the weighted width of the graph using the elimination order specified (as specified in
     *  Darwiche & Hopkins 2001).
     */
    public static double weightedwidth(Graph g, List order) {
        double w = 0;
        Graph g2 = new HashGraph(g);
        Iterator iter = order.iterator();
        while (iter.hasNext()) {
            Object node = iter.next();

            double n = ((FiniteVariable)node).size();
            for (Iterator niter = g2.neighbors(node).iterator(); niter.hasNext();) {
                n *= ((FiniteVariable)niter.next()).size();
            }

            if (n > w) {
                w = n;
            }
            Set s = new HashSet(g2.neighbors(node));
            g2.remove(node);
            for (Iterator i1 = s.iterator(); i1.hasNext();) {
                Object v1 = i1.next();
                for (Iterator i2 = s.iterator(); i2.hasNext();) {
                    Object v2 = i2.next();
                    if (!v1.equals(v2))
                        g2.addEdge(v1, v2);
                }
            }
        }

        return Math.log( w) / Math.log(2);  //take the log-base2 of w.
    }

    public static double width(DirectedGraph g, List order,
            DoubleFunction weight) {
        return width(moralGraph(g), order, weight);
    }
    public static double width(Graph g, List order, DoubleFunction weight) {
        double w = 0;
        Graph g2 = new HashGraph(g);
        Iterator iter = order.iterator();
        while (iter.hasNext()) {
            Object node = iter.next();
            double n = weight.getDouble(node) - 1;
            for (Iterator niter = g2.neighbors(node).iterator();
                    niter.hasNext();) {
                n += weight.getDouble(niter.next());
            }
            if (n > w)
                w = n;
            Set s = new HashSet(g2.neighbors(node));
            g2.remove(node);
            for (Iterator i1 = s.iterator(); i1.hasNext();) {
                Object v1 = i1.next();
                for (Iterator i2 = s.iterator(); i2.hasNext();) {
                    Object v2 = i2.next();
                    if (!v1.equals(v2))
                        g2.addEdge(v1, v2);
                }
            }
        }
        return w;
    }
    /**
     * Removes node n and connects all of its neighbors.
     */
    public static void removeAndConnect(Graph g, Object n) {
        Set s = new HashSet(g.neighbors(n));
        g.remove(n);
	makeClique(g,s);
    }

    public static void makeClique(Graph g,Set s){
        Iterator i1 = s.iterator();
        while (i1.hasNext()) {
            Object n1 = i1.next();
            Iterator i2 = s.iterator();
            while (i2.hasNext()) {
                Object n2 = i2.next();
                if (!n1.equals(n2))
                    g.addEdge(n1, n2);
            }
        }
    }
    public static void print( Graph g, java.io.PrintStream stream ) {
        Iterator iter = g.iterator();
        while (iter.hasNext()) {
            Object v = iter.next();
            //a new set is created because some sets don't have a good toString capability
            stream.println(v + ":\t:"+new HashSet(g.neighbors(v)));
        }
    }
    /**
     * Returns the number of edges in g.
     */
    public static int edgeCount(Graph g) {
        Iterator iter = g.iterator();
        int total = 0;
        while (iter.hasNext()) {
            total += g.degree(iter.next());
        }
        return total / 2;
    }
    /**
     * Returns the family of vertex. The family consists of the node, and its parents.
     */
    public static Set family(DirectedGraph g, Object vertex) {
        Set result = new HashSet(g.inComing(vertex));
        result.add(vertex);
        return result;
    }
    public static void print( DirectedGraph g, java.io.PrintStream stream ) {
        Iterator iter = g.iterator();
        while (iter.hasNext()) {
            Object v = iter.next();
            stream.println(v + ":\t"+new HashSet(g.outGoing(v)));
        }
    }
    public static String toString(DirectedGraph g) {
        Iterator iter = g.iterator();
        StringBuffer result = new StringBuffer(g.size() * 2);
        while (iter.hasNext()) {
            Object v = iter.next();
            result.append(v + "\t"+new HashSet(g.outGoing(v)) + "\n");
        }
        return result.toString();
    }
    /**
     * Returns true if the graph is connected.
     */
    public static boolean isConnected(Graph g) {
        Set found = new HashSet();
        Set unprocessed = new HashSet();
        Object node = g.vertices().iterator().next();
        found.add(node);
        unprocessed.add(node);
        while (!unprocessed.isEmpty()) {
            node = unprocessed.iterator().next();
            unprocessed.remove(node);
            Set neighbors = new HashSet(g.neighbors(node));
            neighbors.removeAll(found);
            found.addAll(neighbors);
            unprocessed.addAll(neighbors);
        }
        if (g.vertices().size() == found.size()) {
            return true;
        } else {
            return false;
        }
    }
    public static boolean isTree(Graph g) {
        if (!isConnected(g)) {
            return false;
        }
        int totalDegree = 0;
        for (Iterator iter = g.iterator(); iter.hasNext();) {
            totalDegree += g.degree(iter.next());
        }
        return (totalDegree / 2) == g.size() - 1;
    }
    /**
     * Generates a Dag, where there is an edge from (nodes[i],nodes[j]) with probability prob, where i<j.
     */
    public static DirectedGraph randomDAG(Object[] nodes, double prob) {
        DirectedGraph result = new HashDirectedGraph();
        result.addAll(java.util.Arrays.asList(nodes));
        for (int i = 0; i < nodes.length; i++) {
            for (int j = i + 1; j < nodes.length; j++) {
                double x = Math.random();
                if (x <= prob) {
                    result.addEdge(nodes[i], nodes[j]);
                }
            }
        }
        return result;
    }
    /**
     * Generates a graph generated by adding edge {nodes[i],nodes[j]) with probability prob.
     */
    public static Graph randomGraph(Object[] nodes, double prob) {
        Graph result = new HashGraph();
        result.addAll(java.util.Arrays.asList(nodes));
        for (int i = 0; i < nodes.length; i++) {
            for (int j = i + 1; j < nodes.length; j++) {
                double x = Math.random();
                if (x <= prob) {
                    result.addEdge(nodes[i], nodes[j]);
                }
            }
        }
        return result;
    }
    /**
     * This method assumes the graph is connected.
     */
    public static DirectedGraph directAway(Graph g, Object node) {
        DirectedGraph result = new HashDirectedGraph(g.size());
        result.addAll(g);
        Set marked = new HashSet();
        directAway(g, marked, node, result);
        return result;
    }
    private static void directAway(Graph g, Set marked, Object vertex,
            DirectedGraph dg) {
        marked.add(vertex);
        for (Iterator iter = g.neighbors(vertex).iterator();
                iter.hasNext();) {
            Object neighbor = iter.next();
            if (!marked.contains(neighbor)) {
                dg.addEdge(vertex, neighbor);
                directAway(g, marked, neighbor, dg);
            }
        }
    }
    public static Graph undirect(DirectedGraph dg) {
        Graph g = new HashGraph(dg.size());
        for (Iterator iter = dg.iterator(); iter.hasNext();) {
            Object vertex = iter.next();
            g.add(vertex);
            for (Iterator edgeIter = dg.inComing(vertex).iterator();
                    edgeIter.hasNext();) {
                g.addEdge(vertex, edgeIter.next());
            }
            for (Iterator edgeIter = dg.outGoing(vertex).iterator();
                    edgeIter.hasNext();) {
                g.addEdge(vertex, edgeIter.next());
            }
        }
        return g;
    }
    public static void collapse(Graph g, Object keepNode,
            Object otherNode) {
        Set neighbors = g.neighbors(otherNode);
        g.remove(otherNode);
        for (Iterator iter = neighbors.iterator(); iter.hasNext();) {
            Object vertex = iter.next();
            if (!vertex.equals(keepNode)) {
                g.addEdge(vertex, keepNode);
            }
        }
    }
    public static IntGraph createIntGraph(Graph g,
            edu.ucla.util.Reference nodeMap) {
        Map m = new HashMap(g.size());
        int i = 0;
        for (Iterator iter = g.iterator(); iter.hasNext();) {
            m.put(iter.next(), new Integer(i));
            i++;
        }
        int[][] neighbors = new int[i][];
        for (Iterator iter = g.iterator(); iter.hasNext();) {
            Object vertex = iter.next();
            int ind = ((Integer) m.get(vertex)).intValue();
            neighbors[ind] = toArray(g.neighbors(vertex), m);
        }
        if (nodeMap != null) {
            nodeMap.object = m;
        }
        return new IntGraph(neighbors);
    }
    public static IntDirectedGraph createIntDirectedGraph(
            DirectedGraph g, edu.ucla.util.Reference nodeMap) {
        Map m = new HashMap(g.size());
        int i = 0;
        for (Iterator iter = g.iterator(); iter.hasNext();) {
            m.put(iter.next(), new Integer(i));
            i++;
        }
        int[][] inComing = new int[i][];
        int[][] outGoing = new int[i][];
        for (Iterator iter = g.iterator(); iter.hasNext();) {
            Object vertex = iter.next();
            int ind = ((Integer) m.get(vertex)).intValue();
            Set s = g.inComing(vertex);
            inComing[ind] = toArray(g.inComing(vertex), m);
            outGoing[ind] = toArray(g.outGoing(vertex), m);
        }
        if (nodeMap != null) {
            nodeMap.object = m;
        }
        return new IntDirectedGraph(inComing, outGoing);
    }
    private static int[] toArray(Set s, Map conversion) {
        int[] result = new int[s.size()];
        Iterator iter = s.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = ((Integer) conversion.get(iter.next())).intValue();
        }
        java.util.Arrays.sort(result);
        return result;
    }
    public static Set sources(DirectedGraph dag){
        HashSet result=new HashSet();
        Iterator iter=dag.iterator();
        while(iter.hasNext()){
            Object node=iter.next();
            if(dag.inDegree(node)==0){
                result.add(node);
            }
        }
        return result;
    }
    public static Set sinks(DirectedGraph dag){
        HashSet result=new HashSet();
        Iterator iter=dag.iterator();
        while(iter.hasNext()){
            Object node=iter.next();
            if(dag.outDegree(node)==0){
                result.add(node);
            }
        }
        return result;
    }
}
