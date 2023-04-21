package il2.inf.edgedeletion;

import il2.util.*;
import il2.model.*;

public class EDEdgeDeleter {

	public static String edgeToString(int[] edge) {
		return String.format("%d -> %d",edge[0],edge[1]);
	}
    public static String edgeToString(int[] edge, double score) {
		return String.format("%d -> %d [%.6g]",edge[0],edge[1],score);
    }

	public static void printEdges(int[][] edges) { 
        printEdges(edges, "", System.err);
    }
	public static void printEdges(int[][] edges, String prefix, 
                                  java.io.PrintStream stream ) {
		for (int edge = 0; edge < edges.length; edge++)
			stream.println(prefix + edgeToString(edges[edge]));
	}
    public static void printEdges(int[][] edges, double[] scores,
                                  String prefix, java.io.PrintStream stream) {
		for (int edge = 0; edge < edges.length; edge++)
			stream.println(prefix + edgeToString(edges[edge],scores[edge]));
    }

	/**
	 * Sort edges, in-place, in topological order.
	 * Returns a map from old edge index to new edge index
	 */
	public static int[] sortEdges(int[][] edges) {
		EdgeComparator ec = new EdgeComparator();
		int[][] input = edges.clone();
		int[] index = new int[edges.length];
		java.util.Arrays.sort(edges,ec);

		// AC: when I have time, I should have the indices sorted with
		// the edges.  Will save roughly half the running time.
		java.util.HashMap<int[],Integer> map =
			new java.util.HashMap<int[],Integer>(edges.length);
		for (int i = 0; i < edges.length; i++)
			map.put(edges[i],new Integer(i));
		for (int i = 0; i < edges.length; i++)
			index[i] = map.get(input[i]).intValue();

		return index;
	}

	/**
	 * returns an int[numEdges][2] of all edges.  Edges are enumerated
	 * by identifying parent/child relationships in network cpts.
	 * Edges are topologically sorted.
	 */
	public static int[][] getAllEdges(BayesianNetwork bn) {
		Table[] cpts = bn.cpts();
		int numEdges = 0;
		for (int i = 0; i < cpts.length; i++) // count edges
			numEdges += cpts[i].vars().size()-1;

		int[][] edges = new int[numEdges][2];
		int edge = numEdges-1;
		for (int i = cpts.length-1; i >= 0; i--)
			for (int j = cpts[i].vars().size()-2; j >= 0; j--) {
				edges[edge][0] = cpts[i].vars().get(j);
				edges[edge][1] = cpts[i].vars().largest();
				edge--;
			}
		sortEdges(edges);
		return edges;
	}

	/**
	 * returns an int[numEdges][2] of all edges.  An edge is a
	 * variable -> table relationship, as in a factor graph.  Edges
	 * are sorted, first by variable, then by table.
	 */
	public static int[][] getAllEdges(Table[] tables) {
		int numEdges = 0;
		for (int i = 0; i < tables.length; i++) // count edges
			numEdges += tables[i].vars().size();

		int[][] edges = new int[numEdges][2];
		int edge = 0;
		for (int table = 0; table < tables.length; table++) {
			IntSet vars = tables[table].vars();
			for (int var = 0; var < vars.size(); var++) {
				edges[edge][0] = vars.get(var);
				edges[edge][1] = table;
				edge++;
			}
		}
		sortEdges(edges);
		return edges;
	}

	private static int[][] complementEdgeList(int[][] allEdges, int[][] edges) {
		int[][] cEdges = new int[allEdges.length - edges.length][2];
		int i = 0, j = 0, k = 0;
		while ( i < allEdges.length ) {
			if ( j < edges.length &&
				 allEdges[i][0]==edges[j][0] && allEdges[i][1]==edges[j][1] ) {
				i++; j++;
			} else {
				cEdges[k][0] = allEdges[i][0];
				cEdges[k][1] = allEdges[i][1];
				i++; k++;
			}
		}
		return cEdges;
	}

	/**
	 * Returns all BN edges not given in inputEdges
	 */
	public static int[][] complementEdgeList(BayesianNetwork bn,
											 int[][] inputEdges){
		int[][] edges = inputEdges.clone();
		sortEdges(edges);
		int[][] allEdges = getAllEdges(bn);
		return complementEdgeList(allEdges,edges);
	}

	/**
	 * Returns all factor graph edges not given in inputEdges
	 */
	public static int[][] complementEdgeList(Table[] tables,
											 int[][] inputEdges) {
		int[][] edges = inputEdges.clone();
		sortEdges(edges);
		int[][] allEdges = getAllEdges(tables);
		return complementEdgeList(allEdges,edges);
	}

	/**
	 * Identify a set of connected components.  Uses unionfind for
	 * more efficient algorithm.
	 */
	private static IntSet[] getConnectedComponents(BayesianNetwork bn) {
		int numVars = bn.domain().size();
		int[][] edgeList = getAllEdges(bn);

		edu.ucla.structure.UnionFind cs =
			new edu.ucla.structure.UnionFind(numVars);
		for (int edge = 0; edge < edgeList.length; edge++)
			cs.union(edgeList[edge][0],edgeList[edge][1]);

		// prepare index and initialize return value
		IntSet cci = new IntSet(); // an index of connected components
		for (int var = 0; var < numVars; var++) cci.add(cs.find(var));
		IntSet[] cc = new IntSet[cci.size()]; // list of connected components
		for (int i = 0; i < cc.length; i++) cc[i] = new IntSet();

		// each IntSet of cc represents a connected component where
		// each IntSet is a set of the corresponding variables
		for (int var = 0; var < numVars; var++)
			cc[cci.indexOf(cs.find(var))].add(var);

		return cc;
	}

	/**
	 * AC: need to check to make sure this works for Table[] only
	 */
	private static Graph getNetworkGraph(BayesianNetwork bn) {
		int numVars = bn.domain().size();
		Graph g = new Graph(numVars);
		Table[] cpts = bn.cpts();

		for (int i = cpts.length-1; i >= 0; i--)
			for (int j = cpts[i].vars().size()-2; j >= 0; j--) {
				g.addEdge(cpts[i].vars().get(j),cpts[i].vars().largest());
			}
		return g;
	}

	/**
	 * Samples a random spanning forest uniformly from the set of all
	 * spanning forests of the undirected network.  Uses a random
	 * walk.  This takes _expected_ O(n log n) time, worst case O(n^3)
	 * time (for lollipop graph?).
	 */
	public static int[][] getRandomSpanningTree(BayesianNetwork bn,
												java.util.Random rand) {
		if ( rand == null ) rand = new java.util.Random(0);

		// get connected component, and graph: we need to identify
		// neighbors of a node
		IntSet[] cc = getConnectedComponents(bn);
		//Graph g = bn.generateGraph();
		Graph g = getNetworkGraph(bn);

		// count number of edges in spanning tree
		int numEdges = 0;
		for (int i = 0; i < cc.length; i++) numEdges += cc[i].size()-1;
		int[][] treeEdges = new int[numEdges][2];

		// perform random walk in each connected component
		int curvar, lastvar, curedge = 0;
		IntSet neighbors;
		for (int i = 0; i < cc.length; i++) {
			curvar = cc[i].get(0); // AC: biased? do random?
			cc[i].remove(curvar);
			while ( !cc[i].isEmpty() ) {
				// choose random neighbor
				neighbors = g.neighbors(curvar);
				lastvar = curvar;
				curvar = neighbors.get(rand.nextInt(neighbors.size()));

				// if we havn't visited the neighbor yet, the edge we
				// entered is an edge for a random spanning tree
				if ( cc[i].contains(curvar) ) {
					cc[i].remove(curvar);
					treeEdges[curedge][0] = curvar<lastvar ? curvar:lastvar;
					treeEdges[curedge][1] = curvar>lastvar ? curvar:lastvar;
					curedge++;
				}
			}
		}

		return treeEdges;
	}

	/**
	 * Identify a set of connected components.  Uses unionfind for
	 * more efficient algorithm.
	 */
	private static IntSet[] getConnectedComponents(Table[] tables,
												   int[][] edges) {
		int numVars = tables[0].domain().size();
		int numTables = tables.length;

		edu.ucla.structure.UnionFind cs =
			new edu.ucla.structure.UnionFind(numVars + numTables);
		for (int edge = 0; edge < edges.length; edge++)
			cs.union(edges[edge][0],numVars+edges[edge][1]);

		// prepare index and initialize return value
		IntSet cci = new IntSet(); // index set of connected components
		for (int i = 0; i < numVars; i++) cci.add(cs.find(i));
		for (int i = numVars; i < numVars+numTables; i++) cci.add(cs.find(i));
		IntSet[] cc = new IntSet[cci.size()]; // list of connected components
		for (int i = 0; i < cc.length; i++) cc[i] = new IntSet();

		// each IntSet of cc represents a connected component where
		// each IntSet is a set of the corresponding factor graph nodes
		for (int i = 0; i < numVars; i++)
			cc[cci.indexOf(cs.find(i))].add(i);
		for (int i = numVars; i < numVars+numTables; i++)
			cc[cci.indexOf(cs.find(i))].add(i);

		return cc;
	}

	/**
	 * induces factor graph from tables.  vars are indexed from
	 * 0...numVars-1 and tables are indexed from
	 * numVars...numVars+numTables-1
	 */
	private static Graph getFactorGraph(Table[] tables, int[][] edges) {
		int numVars = tables[0].domain().size();
		int numTables = tables.length;
		Graph g = new Graph(numVars+numTables);
		for (int i = 0; i < edges.length; i++)
			g.addEdge( edges[i][0], numVars+edges[i][1] );
		return g;
	}

	/* Samples a random spanning forest uniformly from the set of all
	 * spanning forests of the undirected network.  Uses a random
	 * walk.  This takes _expected_ O(n log n) time, worst case O(n^3)
	 * time (for lollipop graph?).
	 */
	public static int[][] getRandomSpanningTree(Table[] tables,
												java.util.Random r) {
		if ( r == null ) r = new java.util.Random(0);

		// get connected component, and graph: we need to identify
		// neighbors of a node
		int[][] edges = getAllEdges(tables);
		IntSet[] cc = getConnectedComponents(tables,edges);
		Graph g = getFactorGraph(tables,edges);

		// count number of edges in spanning forest:
		// the sum of spanning tree size of each connected component
		int numEdges = 0;
		for (int i = 0; i < cc.length; i++) numEdges += cc[i].size()-1;
		int[][] treeEdges = new int[numEdges][2];

		// perform random walk in each connected component
		int curvar, lastvar, curedge = 0;
		int var, table;
		int numVars = tables[0].domain().size();
		IntSet neighbors;
		for (int i = 0; i < cc.length; i++) {
			curvar = cc[i].get(0);
			cc[i].remove(curvar);
			while ( !cc[i].isEmpty() ) {
				// choose random neighbor
				neighbors = g.neighbors(curvar);
				lastvar = curvar;
				curvar = neighbors.get(r.nextInt(neighbors.size()));

				// if we havn't visited the neighbor yet, the edge we
				// entered is an edge for a random spanning tree
				if ( cc[i].contains(curvar) ) {
					cc[i].remove(curvar);
					var = Math.min(curvar,lastvar);
					table = Math.max(curvar,lastvar) - numVars;
					treeEdges[curedge][0] = var;
					treeEdges[curedge][1] = table;
					curedge++;
				}
			}
		}

		return treeEdges;
	}

	public static int[][] getEdgesToDeleteForRandomSpanningTree
		(Table[] tables, java.util.Random r) {
		int[][] treeEdges = getRandomSpanningTree(tables,r);
		int[][] edges = complementEdgeList(tables,treeEdges);
		return edges;
	}

	public static int[][] getEdgesToDeleteForRandomSpanningTree
		(BayesianNetwork bn, java.util.Random r) {
		int[][] treeEdges = getRandomSpanningTree(bn,r);
		int[][] edges = complementEdgeList(bn,treeEdges);
		return edges;
	}

}
