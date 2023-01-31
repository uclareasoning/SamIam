/*
* Trees.java
*
* Created on February 23, 2000, 2:55 PM
*/
package edu.ucla.belief.tree;

import edu.ucla.util.*;
import java.util.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;

/**
* A collection of static routines for creation and conversion of the various
* structured tree representations.
* @author unknown
* @version
*/
public final class Trees extends Object
{
	/**
		@author Keith Cascio
		@since 081502
	*/
	public static boolean FLAG_DEBUG = false;

	/** Creates new Trees */
	private Trees() {}

    /**
     * Generates an elimination tree.
     * @param potentials The potentials that form the basis for it.
     * @param eliminationOrder A list of variables in the order that they should be removed.
     * @param potentialAssignments A Reference, which if non null will point to a Map which maps
     * from leaf nodes to the potential assigned to it.
     */
    public static EliminationTree eliminationTree(TableIndex[] leaves,
            List eliminationOrder) {
        MinPairCostTreeGenerator gen =
                new MinPairCostTreeGenerator(leaves, eliminationOrder,
                UNION_SIZE_COST);
        return gen.tree();
    }
    public static EliminationTree recursiveEliminationTree(
            TableIndex[] leaves, List eliminationOrder) {
        RecursiveEliminationTreeGenerator gen =
                new RecursiveEliminationTreeGenerator(leaves,
                eliminationOrder);
        return gen.tree();
    }

    public static JoinTree traditionalJoinTree( final BeliefNetwork bn, int reps, Random seed ) {
        return traditionalJoinTree( bn, EliminationOrders.minFill( bn, reps, seed ) );
    }

	/**
	* Generates a traditional JoinTree using the method described in [1]. This method
	* works only for discrete networks.
	*/
	public static JoinTree traditionalJoinTree(	final BeliefNetwork bn,
							final List eliminationOrder )
	{
		java.io.PrintStream out = ( Definitions.DEBUG ) ? System.out : null;
		Graph moral = Graphs.moralGraph( bn, bn, out );
		JoinTree ret = traditionalJoinTree( moral, eliminationOrder, SUM_SIZE_COST );
		ret.setBeliefNetwork( bn );
		return ret;
	}

    /**
     * A cost function which is the size of the union of the variables in the cluster.
     * This method is intended for use in binary join tree construction.
     */
    public static final CostFunction UNION_SIZE_COST = new CostFunction() {
                public double cost(Set cluster1, Set cluster2) {
                    Set temp = new HashSet(cluster1);
                    temp.addAll(cluster2);
                    return FiniteVariableImpl.size(temp);
                }
            };
    /**
     * A cost function which is the sum of the sizes(# of instantiations) of the
     * two cliques. This is the method described in [1].
     */
    public static final CostFunction SUM_SIZE_COST = new CostFunction() {
                public double cost(Set cluster1, Set cluster2) {
                    return FiniteVariableImpl.size(cluster1) +
                            FiniteVariableImpl.size(cluster2);
                }
            };

	/**
		Warning: It is safe to use this method only in the absence of a BeliefNetwork.

		Generates a JoinTree by generating the clusters through elimination, then choosing
  		the edges so as to minimize the cost. This is described in [2].

  		@since 021004
	*/
    public static JoinTree traditionalJoinTree(Graph moralGraph,
            List eliminationOrder, CostFunction edgeCost) {
        List clusters =
                generateTraditionalClusters(moralGraph, eliminationOrder);
        Edge[] edges =
                new Edge[(clusters.size() * (clusters.size() - 1)) / 2];
        double[] scores = new double[edges.length];
        int current = 0;
        for (int i = 0; i < clusters.size(); i++) {
            for (int j = i + 1; j < clusters.size(); j++, current++) {
                edges[current] = new Edge(i, j);
                Set s = new HashSet((Set) clusters.get(i));
                s.retainAll((Set) clusters.get(j));
                double mass = s.size();
                scores[current] = mass + 1.0 /
                        (2.0 + edgeCost.cost((Set) clusters.get(i),
                        (Set) clusters.get(j)));
            }
        }
        Heap h = new Heap(edges, scores);
        UnionFind forrest = new UnionFind(clusters.size());
        Graph tree = new HashGraph(clusters.size());
        Map clusterMap = new HashMap(clusters.size());
        for (int i = 0; i < clusters.size(); i++) {
            tree.add(new Integer(i));
            clusterMap.put(new Integer(i), clusters.get(i));
        }
        for (int i = 0; i < clusters.size() - 1;) {
            Edge e = (Edge) h.extractMax().element();
            if (forrest.find(e.v1) != forrest.find(e.v2)) {
                forrest.union(e.v1, e.v2);
                tree.addEdge(new Integer(e.v1), new Integer(e.v2));
                i++;
            }
        }
        return createJoinTree( eliminationOrder, tree, clusterMap );
    }
    private static JoinTree createJoinTree( List eliminationOrder, Graph tree, Map m ){
        Reference ref = new Reference();
        IntGraph resultTree = Graphs.createIntGraph(tree, ref);
        Map nodeToInt = (Map) ref.object;
        Set[] clusters = new Set[resultTree.size()];
        for (Iterator iter = nodeToInt.keySet().iterator();
                iter.hasNext();) {
            Object key = iter.next();
            int val = ((Integer) nodeToInt.get(key)).intValue();
            clusters[val] = (Set) m.get(key);
        }
        return new JoinTree( eliminationOrder, resultTree, clusters, (BeliefNetwork)null );
    }
    /**
     * Returns a list of clusters(sets of variables) generated by eliminating the
     * variables in the order specified. Redundant clusters(those which are a
     * proper subset of another cluster) are removed.
     */
    private static List generateTraditionalClusters(Graph moralGraph,
            List order) {
        Graph g = new HashGraph(moralGraph);
        Graph triangulated = new HashGraph(moralGraph);
        List candidates = new ArrayList(order.size());
        for (int i = 0; i < order.size(); i++) {
            Object node = order.get(i);
            List neighbors = new ArrayList(g.neighbors(node));
            for (Iterator iter1 = neighbors.iterator(); iter1.hasNext();) {
                Object n1 = iter1.next();
                for (Iterator iter2 = neighbors.iterator();
                        iter2.hasNext();) {
                    Object n2 = iter2.next();
                    if (!n1.equals(n2)) {
                        g.addEdge(n1, n2);
                        triangulated.addEdge(n1, n2);
                    }
                }
            }
            g.remove(node);
            Set currentCluster = new HashSet(neighbors);
            currentCluster.add(node);
            candidates.add(currentCluster);
        }
        List ret = reduceClusters(candidates);
        if( FLAG_DEBUG ){ Definitions.STREAM_VERBOSE.println( "\n\nGenerated clusters: " + ret ); }
        return ret;
    }
    /**
     * Removes the redundant clusters.
     */
    private static List reduceClusters(List candidateClusters) {
        List clusters = new ArrayList();
        for (int i = 0; i < candidateClusters.size(); i++) {
            Set currentCluster = (Set) candidateClusters.get(i);
            boolean redundant = false;
            for (int j = clusters.size() - 1; j >= 0; j--) {
                if (((Set) clusters.get(j)).containsAll(currentCluster)) {
                    redundant = true;
                    break;
                }
            }
            if (!redundant) {
                clusters.add(currentCluster);
            }
        }
        return clusters;
    }
    /**
     * The edge representation used for the tree generation.
     */
    private static class Edge {
        int v1, v2;
        Edge(int i, int j) {
            v1 = i;
            v2 = j;
        }
        public String toString() {
            return "["+v1 + ","+v2 + "]";
        }
    }
    /*
     public static DoubleLabel clusterSizes(JoinTree jt){
     return getSizes(jt.clusters());
     }
     */
    /*
    public static DoubleLabel getSizes(Map clusters){
    DoubleLabel result=new DoubleLabel();
    for (Iterator iter=clusters.keySet().iterator();iter.hasNext();) {
    Object vertex=iter.next();
    result.put(vertex,FiniteVariable.size((Set)clusters.get(vertex)));
}
    return result;
}
    public static DoubleLabel separatorSizes(JoinTree jt){
    return getSizes(jt.separators());
}
    */
}
