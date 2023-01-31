package il2.inf.bp.schedules;

import il2.inf.edgedeletion.EDEdgeDeleter;
import il2.model.Domain;
import il2.model.Table;
import il2.util.Graph;
import il2.util.IntSet;
import il2.util.Pair;

import java.util.Iterator;

public class TreeSchedule extends MessagePassingScheduler {
    int[][] tree_edges;
    int[][] off_edges;
    Pair[] order;
    int switch_index;

    MessageIterable iterable;
    
    public TreeSchedule(Table[] tables, int[][] tedges) {
        super(tables);
        this.tree_edges = tedges.clone();
        EDEdgeDeleter.sortEdges(this.tree_edges);
        this.off_edges = complement(edges,this.tree_edges);
        this.tree_edges = toFactorGraphEdges(this.tree_edges);
        this.off_edges = toFactorGraphEdges(this.off_edges);
        int fgSize = domain.size()+tables.length;
        this.order = constructOrder(fgSize);
        this.iterable = new MessageIterable(this.order,this.switch_index);
    }

    public Iterable<Pair> nextIteration() {
        iterable.reset();
        return iterable;
    }
    public boolean isAsynchronous() { return true; }//{ ! iterable.switched(); }

    class MessageIterable implements Iterable<Pair>, Iterator<Pair> {
        Pair[] pairs;
        int index;
        int switch_index;
        public MessageIterable(Pair[] p, int si)
            { pairs = p; index = -1; switch_index = si; }
        public Iterator<Pair> iterator() { return this; }
        public boolean hasNext() { return index+1 < pairs.length; }
        public Pair next() { index++; return pairs[index]; }
        public void remove() {}
        public void reset() { index = -1; }
        public boolean switched() { return index >= switch_index; }
    }

    public Pair[] constructOrder(int fgSize) {
        Pair[] order = new Pair[pairs.length];
        Graph graph = getFactorGraph(fgSize,tree_edges);
        IntSet[] ccs = getConnectedComponents(fgSize,tree_edges);
        int index = 0;

        for ( IntSet cc : ccs ) {
            int root = cc.get(0);
            if ( ! graph.contains(root) ) continue;
            int start = index;
            int end = pullOrder(graph,root,-1,order,index);
            index = pushOrder(order,start,end);
        }
        this.switch_index = index;
        offTreeOrder(order,index,off_edges);

        return order;
    }

	private static int pullOrder(Graph graph, int cur, int prev,
                                 Pair[] order, int index) {
        IntSet neighbors = graph.neighbors(cur);
        for (int i = 0; i < neighbors.size(); i++) {
            int neighbor = neighbors.get(i);
            if ( neighbor == prev ) continue;
            index = pullOrder(graph,neighbor,cur,order,index);
            order[index] = new Pair(neighbor,cur);
            index++;
        }
        return index;
	}

    private static int pushOrder(Pair[] order, int start, int end) {
        int head = end;
        int tail = end-1;
        while ( tail >= start ) {
            order[head] = new Pair(order[tail].s2,order[tail].s1);
            head++; tail--;
        }
        return head;
    }

    private static void offTreeOrder(Pair[] order, int index, int[][] edges){
        for (int[] edge : edges) {
            order[index++] = new Pair(edge[0],edge[1]);
            order[index++] = new Pair(edge[1],edge[0]);
        }
    }

    private int[][] toFactorGraphEdges(int[][] edges) {
        int[][] fg_edges = new int[edges.length][2];
        int n = domain.size();
        for (int i = 0; i < edges.length; i++) {
            fg_edges[i][0] = edges[i][0];
            fg_edges[i][1] = edges[i][1] + n;
        }
        return fg_edges;
    }

    /**
     * Let n be # of variables
     *     t be # of tables
     *     m be # of factor graph edges
     *
     * factor graph nodes [0,n) are variables
     * factor graph nodes [n,n+t) are tables
     *
     * the ordering is specified in terms of edges in the factor graph
	 */ 
	private static Graph getFactorGraph(int size,int[][] edges) {
		Graph g = new Graph(size);
        for (int[] edge : edges ) g.addEdge(edge[0],edge[1]);
		return g;
	}

	/**
	 * Identify a set of connected components.  Uses unionfind for
	 * more efficient algorithm.
     *
     * I decide to check if edges are a tree here...
	 */
	private static IntSet[] getConnectedComponents(int size, int[][] edges) {
		edu.ucla.structure.UnionFind cs = 
			new edu.ucla.structure.UnionFind(size);
        for (int[] edge : edges) {
            if (cs.find(edge[0]) == cs.find(edge[1]))
                throw new IllegalStateException("edge list must be a tree");
            cs.union(edge[0],edge[1]);
        }

        // count connected components
		IntSet cci = new IntSet();
		for (int i = 0; i < size; i++) cci.add(cs.find(i));
        // collect connected components
		IntSet[] cc = new IntSet[cci.size()];
		for (int i = 0; i < cc.length; i++) cc[i] = new IntSet();
		for (int i = 0; i < size; i++) cc[cci.indexOf(cs.find(i))].add(i);

		return cc;
	}

    /**
     * copied from il2.inf.edgedeletion.EDEdgeDeleter
	 */ 
	private static int[][] complement(int[][] allEdges, int[][] edges) {
		int[][] cEdges = new int[allEdges.length - edges.length][2];
		int i = 0, j = 0, k = 0;
		while ( i < allEdges.length ) {
			if ( j < edges.length && 
				 allEdges[i][0]==edges[j][0] && 
                 allEdges[i][1]==edges[j][1] ) {
				i++; j++;
			} else {
				cEdges[k][0] = allEdges[i][0];
				cEdges[k][1] = allEdges[i][1];
				i++; k++;
			}
		}
		return cEdges;
	}

}
