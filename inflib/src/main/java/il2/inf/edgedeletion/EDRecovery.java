package il2.inf.edgedeletion;

import il2.util.*;
import il2.model.*;

import il2.inf.Algorithm;
import il2.inf.Algorithm.Setting;
import il2.inf.Algorithm.Result;

import java.util.Map;
import java.util.Random;

public class EDRecovery extends EDAlgorithm {

    /**
     * This class is basically EDAlgorithm, except that it includes a
     * new constructor that allows edges to be deleted automatically.
     */

    /*public enum Recovery {
        RANDOM,
        MUTUAL_INFORMATION;

        public static Recovery def() { return MUTUAL_INFORMATION; }
    }*/

    protected RankingHeuristic method; // default
    protected int numRecovered; // # in edges
    // protected double edgerankingTime; // this is in parent class

    public RankingHeuristic getMethod() { return method; }
    public int getNumRecovered() { return numRecovered; }

    public EDRecovery die() {
        super.die();
    	return this;
    }

    /**
     * num is the # of edges you want to _recover_
     */
	public EDRecovery( BayesianNetwork bn, IntMap e,
                       RankingHeuristic meth, int num,
                       int mi, long tm, double ct,
                       Algorithm alg, Map<Setting,?> settings, Random r) {
        super(bn,deleteAndRecover(bn,e,meth,num,mi,tm,ct,alg,settings,r),
              mi,tm,ct,alg,settings);
        // printEdges( System.out );
    }

    /**
     * AC: need to update, this counts the number of edges that can be
     * recovered
     */
    public static int countRecoverable(BayesianNetwork bn) {
        Random r = new java.util.Random(0);
        int[][] offTreeEdges = EDEdgeDeleter.
            getEdgesToDeleteForRandomSpanningTree(bn,r);
        return offTreeEdges.length;
    }

    protected static int[][] deleteAndRecover( BayesianNetwork bn, IntMap e,
            RankingHeuristic meth, int num, int mi, long tm, double ct,
            Algorithm alg, Map<Setting,?> settings, Random r) {
		long start = System.nanoTime();

        int[][] offTreeEdges = EDEdgeDeleter.
			getEdgesToDeleteForRandomSpanningTree(bn,r);
        int[][] edges;
        if ( num == 0 )
            edges = offTreeEdges;
        else if ( num >= offTreeEdges.length )
            edges = new int[0][];
        else if ( meth == RankingHeuristic.random )
            edges = recoverRandomly(offTreeEdges,num,r);
        else if ( meth == RankingHeuristic.mi )
            edges = recoveryByMI(bn,e,offTreeEdges,num,mi,tm,ct,alg,settings);
        else if ( meth == RankingHeuristic.residual )
            edges = recoveryByResidual(bn,e,offTreeEdges,num,mi,tm,ct,alg,settings);
        else edges = offTreeEdges;

		long finish = System.nanoTime();
		// edgeRankingTime=(finish-start)*1e-6;

        return edges;
    }

    protected static int[][] recoverRandomly(int[][] edges, int num, Random r) {
        java.util.List<int[]> shuffled = java.util.Arrays.asList(edges);
        java.util.Collections.shuffle(shuffled);
        int len = edges.length - num;
        if ( len < 0 ) len = 0;
        return shuffled.subList(0,len).toArray(new int[0][]);
    }

    protected static int[][] recoveryByMI(BayesianNetwork bn, IntMap e,
                                      int[][] edges, int num,
                                      int mi, long tm, double ct,
                                      Algorithm alg, Map<Setting,?> settings){
        EDAlgorithm edie = new EDAlgorithm(bn,edges,mi,tm,ct,alg,settings);
        edie.setEvidence(e);
        edie.logPrEvidence(); // update engine
        int[][] ranked = edie.rankEdgesByMi();
        int len = ranked.length - num;
        if ( len < 0 ) len = 0;
        int[][] unrecovered = new int[len][];
        for (int i = 0; i < len; i++)
            unrecovered[i] = ranked[i];
        return unrecovered;
    }

    protected static int[][] recoveryByResidual(BayesianNetwork bn, IntMap e,
                                      int[][] edges, int num,
                                      int mi, long tm, double ct,
                                      Algorithm alg, Map<Setting,?> settings){
        EDAlgorithm edie = new EDAlgorithm(bn,edges,mi,tm,ct,alg,settings);
        edie.doConvergenceUpdates = true;
        edie.setEvidence(e);
        edie.logPrEvidence(); // update engine
        int[][] ranked = edie.rankEdgesByConvergence();
        int len = ranked.length - num;
        if ( len < 0 ) len = 0;
        int[][] unrecovered = new int[len][];
        for (int i = 0; i < len; i++)
            unrecovered[i] = ranked[i];
        return unrecovered;
    }

    private void printEdges( java.io.PrintStream stream ) {
        Domain d = edNet.oldDomain();
        stream.println("edges: ");
        for (int[] edge: edgesDeleted) {
            String u = d.name(edge[0]);
            String x = d.name(edge[1]);
            stream.println(u + "->" + x);
        }
    }
}
