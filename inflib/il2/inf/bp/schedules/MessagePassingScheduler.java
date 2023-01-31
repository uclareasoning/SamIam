package il2.inf.bp.schedules;

import il2.inf.edgedeletion.EDEdgeDeleter;
import il2.model.Domain;
import il2.model.Table;
import il2.util.Graph;
import il2.util.IntSet;
import il2.util.Pair;

import java.util.ArrayList;

public abstract class MessagePassingScheduler {
    Domain domain;
    int[][] edges; // (variable,table) edges
    Pair[] pairs; // factor graph pairs: (var,table) or (table,var)
    ArrayList<ArrayList<Pair>> incoming;

    public MessagePassingScheduler(Table[] tables) {
        this.domain = tables[0].domain();
        this.edges = EDEdgeDeleter.getAllEdges(tables);
        this.pairs = computeFactorGraphPairs(edges);
        int fgSize = domain.size()+tables.length;
        this.incoming = computeIncoming(fgSize,pairs);
    }

    /**
     * This returns an array of message indices to be passed at the
     * next iteration.
     */
    public abstract Iterable<Pair> nextIteration();
    public abstract boolean isAsynchronous();

    public Pair[] fgPairs() { return pairs; }

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
    private Pair[] computeFactorGraphPairs(int[][] edges) {
        int n = domain.size();
        int m = edges.length;
        Pair[] pairs = new Pair[2*m];
        for (int i = 0; i < m; i++) {
            // var -> table
            pairs[i]   = new Pair(edges[i][0],edges[i][1] + n);
            // table -> var
            pairs[m+i] = new Pair(edges[i][1] + n,edges[i][0]);
        }
        return pairs;
    }

    public int tableOfPair(Pair pair) {
        return pair.s1 > pair.s2 ? pair.s1 : pair.s2;
    }

    public int varOfPair(Pair pair) {
        return pair.s1 < pair.s2 ? pair.s1 : pair.s2;
    }

    public ArrayList<Pair> messagesIncoming(int node) {
        return incoming.get(node);
    }

    private static ArrayList<ArrayList<Pair>>
        computeIncoming(int size, Pair[] pairs) {

        ArrayList<ArrayList<Pair>> incoming = 
            new ArrayList<ArrayList<Pair>>(size);
        for (int i = 0; i < size; i++)
            incoming.add(new ArrayList<Pair>());
        for (Pair pair: pairs)
            incoming.get(pair.s2).add(pair);
        return incoming;
    }
}
