/*
* EliminationOrders.java
*
* Created on February 24, 2000, 1:15 PM
*/
package edu.ucla.belief;
import java.util.*;
import edu.ucla.structure.*;
/**
*
* @author unknown
* @version
*/
public class EliminationOrders extends Object {
    private EliminationOrders() {
    }
    public static Graph moralGraph(Collection variablePotentials) {
        Graph g = new HashGraph();
        for (Iterator iter = variablePotentials.iterator();
                iter.hasNext();) {
            //Set vars = ((Potential) iter.next()).variables();
            List vars = ((Potential) iter.next()).variables();
            for (Iterator v1iter = vars.iterator(); v1iter.hasNext();) {
                Object v1 = v1iter.next();
                g.add(v1);
                for (Iterator v2iter = vars.iterator(); v2iter.hasNext();) {
                    Object v2 = v2iter.next();
                    if (!v1.equals(v2)) {
                        g.addEdge(v1, v2);
                    }
                }
            }
        }
        return g;
    }

    //==================================================================

    public static List oldEliminateForMinFill(Graph moralGraph, Set variables) {
        Graph g = new HashGraph(moralGraph);
        double[] scores = new double[variables.size()];
        Object[] varArray = new Object[variables.size()];
	ArrayList randomOrder = new ArrayList( variables);
	Collections.shuffle( randomOrder);
        Iterator iter = randomOrder.iterator();
        for (int i = 0; i < scores.length; i++) {
            varArray[i] = iter.next();
            scores[i] = -MIN_FILL.score(varArray[i], g);
        }
        Heap heap = new Heap(varArray, scores);
        List result = new ArrayList(scores.length);
        for (int i = 0; i < scores.length; i++) {
            Object v = heap.extractMax().element();
            result.add(v);
            Set s = getMinFillUpdateNodes(g,v);
            Graphs.removeAndConnect(g, v);
            for (Iterator siter = s.iterator(); siter.hasNext();) {
                Object var = siter.next();
                if (variables.contains(var)) {
                    heap.setValue(var, -MIN_FILL.score(var, g));
                }
            }
        }
        return result;
    }

    public static List eliminateForMinFill( Graph moralGraph, Set variables, int reps, Random seed ) {

      if( reps < 1 ) throw new IllegalArgumentException( "reps must be >= 1" );

      // Prepare the input to the il2 method.  Create a converter, prepare
      // cardinalities, the moral graph, and the partition.

      il2.bridge.Converter c = new il2.bridge.Converter ();
      c.init (new java.util.ArrayList (moralGraph));
      int[] cardinalities = new int[c.getDomain ().size ()];
      for (int i = 0; i < cardinalities.length; i++) {
        cardinalities[i] = c.getDomain ().size (i);
      }
      int[][] g = new int[moralGraph.size ()][];
      for (int i = 0; i < g.length; i++) {
        g[i] = c.convert (moralGraph.neighbors (c.convert (i))).toArray ();
      }
      int[][] partition = new int[][] {c.convert (variables).toArray ()};

      // Call the il2 method, and convert answer back.

      if( seed == null ) seed = new Random();

      try {
        il2.inf.structure.minfill2.MinfillEoe engine = new il2.inf.structure.minfill2.MinfillEoe();
        int[] il2Ans = null, best = null, worst = null;
        double logmaxclustersize = Double.NaN, min = Double.MAX_VALUE, max = (double)0;
        for( int i=0; i<reps; i++ ){
          il2Ans = engine.order( seed, cardinalities, g, partition );
          logmaxclustersize = il2.inf.structure.minfill2.Util.logMaxClusterSize( il2Ans, cardinalities, g );
          if( logmaxclustersize < min ){
            min = logmaxclustersize;
            best = il2Ans;
          }
          if( logmaxclustersize > max ){
            max = logmaxclustersize;
            worst = il2Ans;
          }
        }
        //System.out.println( "minfill2, best of " + reps + ": " + min + ", worst: " + max );

        java.util.ArrayList ans = new java.util.ArrayList( best.length );
        for( int i = 0; i < best.length; i++ ){
          ans.add( c.convert( best[i] ) );
        }
        return ans;
      }catch( Exception e ){
        System.err.println( e );
        return null;
      }

    }

    public static List oldMinFill(Set variablePotentials) {
        return oldMinFill(moralGraph(variablePotentials));
    }
    public static List oldMinFill(Collection variablePotentials,
            Set variables) {
        return oldMinFill(moralGraph(variablePotentials), variables);
    }
    public static List oldMinFill(BeliefNetwork bn) {
        return oldMinFill(Graphs.moralGraph(bn));
    }
    public static List oldMinFill(Graph moralGraph) {
        return oldEliminateForMinFill(moralGraph,new HashSet(moralGraph));
    }
    public static List oldMinFill(BeliefNetwork bn, Set variables) {
        return oldEliminateForMinFill(Graphs.moralGraph(bn), variables);
    }
    public static List oldMinFill(Graph g, Set variables) {
        return oldEliminateForMinFill(g, variables);
    }

    public static List minFill( Set variablePotentials, int reps, Random seed ) {
        return minFill( moralGraph(variablePotentials), reps, seed );
    }
    public static List minFill( Collection variablePotentials, Set variables, int reps, Random seed ) {
        return minFill( moralGraph(variablePotentials), variables, reps, seed );
    }
    public static List minFill( BeliefNetwork bn, int reps, Random seed ) {
        return minFill( Graphs.moralGraph(bn), reps, seed );
    }
    public static List minFill(Graph moralGraph, int reps, Random seed ) {
        return eliminateForMinFill( moralGraph, new HashSet(moralGraph), reps, seed );
    }
    public static List minFill(BeliefNetwork bn, Set variables, int reps, Random seed ) {
        return eliminateForMinFill( Graphs.moralGraph(bn), variables, reps, seed );
    }
    public static List minFill(Graph g, Set variables, int reps, Random seed ) {
        return eliminateForMinFill( g, variables, reps, seed );
    }

    //==================================================================

    public static List bogusMinFill(BeliefNetwork bn){
        return eliminate(bn,MIN_FILL);
    }
    public static List minSize(BeliefNetwork bn) {
        return eliminate(bn, MIN_SIZE);
    }
    public static List minDegree(BeliefNetwork bn) {
        return eliminate(bn, MIN_DEGREE);
    }
    public static List minSize(Graph moralGraph) {
        return eliminate(moralGraph, MIN_SIZE);
    }
    public static List minDegree(Graph moralGraph) {
        return eliminate(moralGraph, MIN_DEGREE);
    }
    public static List minSize(Graph g, Set variables) {
        return eliminate(g, variables, MIN_SIZE);
    }
    public static List minDegree(Graph g, Set variables) {
        return eliminate(g, variables, MIN_DEGREE);
    }
    public static List minSize(BeliefNetwork bn, Set variables) {
        return eliminate(bn, variables, MIN_SIZE);
    }
    public static List minDegree(BeliefNetwork bn, Set variables) {
        return eliminate(bn, variables, MIN_DEGREE);
    }
    public static List mapOrder(Collection tables, Set mapvars) {
        Graph g = moralGraph(tables);
        Set variables = new HashSet(g);
        MapScoreFunction f =
                new MapScoreFunction(mapvars, variables.size());
        return eliminate(g, variables, f);
    }
    public static interface ScoreFunction {
        public double score(Object node, Graph g);
    }
    public static final ScoreFunction MIN_DEGREE = new ScoreFunction() {
                public double score(Object obj, Graph g) {
                    return (double) g.neighbors(obj).size();
                }
            };
    public static final ScoreFunction MIN_SIZE = new ScoreFunction() {
                public double score(Object obj, Graph g) {
                    double size = (double)((FiniteVariable) obj).size();
                    Iterator iter = g.neighbors(obj).iterator();
                    while (iter.hasNext()) {
                        size *= (double)
                                ((FiniteVariable) iter.next()).size();
                    }
                    return size;
                }
            };
    public static final ScoreFunction MIN_FILL = new MinFillScore(true);
    public static final ScoreFunction MIN_FILL_NO_SIZE =
            new MinFillScore(false);
    private static class MinFillScore implements ScoreFunction {
        boolean measureSize;
        public MinFillScore() {
            measureSize = true;
        }
        public MinFillScore(boolean useSizeTieBreaker) {
            measureSize = useSizeTieBreaker;
        }
        public double score(Object obj, Graph g) {
            Set neighbors = g.neighbors(obj);
            Iterator iter = neighbors.iterator();
            double total = 0;
            double size;
            if (measureSize) {
                size = ((FiniteVariable) obj).size();
            } else {
                size = 1;
            }
            while (iter.hasNext()) {
                Object var1 = iter.next();
                Iterator iter2 = neighbors.iterator();
                while (iter2.hasNext()) {
                    Object var2 = iter2.next();
                    if (!var1.equals(var2) && !g.containsEdge(var1, var2)) {
                        total++;
                    }
                }
                if (measureSize) {
                    size *= ((FiniteVariable) var1).size();
                }
            }
            if (measureSize) {
                return (total + (1 - 1.0 / (2 * size)));
                //return total+nameScore(((FiniteVariable)obj).getName());
            } else {
                return total;
            }
        }
        private static double nameScore(String name) {
            String uname = name.toUpperCase();
            byte[] bytes = uname.getBytes();
            double total = 0;
            double base = 1.0 / 256 / 256;
            for (int i = 0; i < bytes.length; i++) {
                total += base * (0xff & (int) bytes[i]);
                base /= 256;
            }
            return total;
        }
    }
    private static class MapScoreFunction implements ScoreFunction {
        private int n;
        Set s;
        MapScoreFunction(Set s, int size) {
            n = size * size * size;//this will always be bigger than the largest score assuming size(the size of the original graph)>1.
            this.s = s;
        }
        public double score(Object obj, Graph g) {
            double initialscore = MIN_FILL.score(obj, g);
            if (s.contains(obj)) {
                return n * (initialscore + 1);
            } else {
                return initialscore;
            }
        }
    }
    public static List eliminate(BeliefNetwork bn,
            ScoreFunction scoreFunction) {
        return eliminate(Graphs.moralGraph( bn ), scoreFunction);
    }
    public static List eliminate(Graph moralGraph,
            ScoreFunction scoreFunction) {
        return eliminate(moralGraph, new HashSet(moralGraph),
                scoreFunction);
    }
    public static List eliminate(BeliefNetwork bn, Set variables,
            ScoreFunction scoreFunction) {
        return eliminate(Graphs.moralGraph( bn ), variables,
                scoreFunction);
    }

    /**
     * Produces an elimination order whose order is chosen by always choosing
     * to eliminate the node with the highest score.  The score function must be
     * computable from a node and its neighbors.  Min-Fill for example can not
     * be used because it depends on neighbors of neighbors.
     * @param moralGraph a graph whose vertices are the variables.
     * @param variables the set of variables to be eliminated.
     * @param scoreFunction a function that produces a score given a node that is
     * based only on that node and its neighbors. Each time a vertex is eliminated,
     * the score for that vertex is recomputed. Scores are assumed to be costs, so
     * the variable with the smallest remaining score is eliminated each time.
     */
    public static List eliminate(Graph moralGraph, Set variables,
            ScoreFunction scoreFunction) {
        Graph g = new HashGraph(moralGraph);
        double[] scores = new double[variables.size()];
        Object[] varArray = new Object[variables.size()];


		ArrayList randomOrder = new ArrayList( variables);
		Collections.shuffle( randomOrder);
        Iterator iter = randomOrder.iterator();

        for (int i = 0; i < scores.length; i++) {
            varArray[i] = iter.next();
            scores[i] = -scoreFunction.score(varArray[i], g);
        }
        Heap heap = new Heap(varArray, scores);
        List result = new ArrayList(scores.length);
        for (int i = 0; i < scores.length; i++) {
            Object v = heap.extractMax().element();
            result.add(v);
            Set s = new HashSet(g.neighbors(v));
            Graphs.removeAndConnect(g, v);
            for (Iterator siter = s.iterator(); siter.hasNext();) {
                Object var = siter.next();
                if (variables.contains(var)) {
                    heap.setValue(var, -scoreFunction.score(var, g));
                }
            }
        }
        return result;
    }



    private static Set getMinFillUpdateNodes(Graph g,Object v){
        List neighbors=new ArrayList(g.neighbors(v));
        Set result=new HashSet(2*neighbors.size());
        for(ListIterator iter1=neighbors.listIterator();iter1.hasNext();){
            Object v1=iter1.next();
            Set v1Neighbors=new HashSet(g.neighbors(v1));
            for(ListIterator iter2=neighbors.listIterator(iter1.nextIndex());iter2.hasNext();){
                Object v2=iter2.next();

                if(!g.containsEdge(v1,v2)){
                    Set v2Neighbors=new HashSet(g.neighbors(v2));
                    v2Neighbors.retainAll(v1Neighbors);
                    result.addAll(v2Neighbors);
                    result.add(v1);
                    result.add(v2);
                }
            }
        }
        result.remove(v);
        return result;
    }

    public static List eliminateBruteForce(Graph moralGraph,
            List variables, ScoreFunction scoreFunction) {
        Graph g = new HashGraph(moralGraph);
        List l = new ArrayList(variables);
        List result = new ArrayList();
        /**Collections.sort(l,new Comparator(){
         public int compare(Object o1,Object o2){
         String s1=((FiniteVariable)o1).getName();
         String s2=((FiniteVariable)o2).getName();
         return s1.compareTo(s2);
         }});*/
        List ties = new ArrayList();
        while (l.size() > 0) {
            double bestScore = Double.NEGATIVE_INFINITY;
            int bestInd = -1;
            ties.clear();
            for (int i = 0; i < l.size(); i++) {
                double score = -scoreFunction.score(l.get(i), g);
                if (score > bestScore) {
                    bestScore = score;
                    bestInd = i;
                    ties.clear();
                } else if (score == bestScore) {
                    ties.add(l.get(i));
                }
            }
            Object var = l.get(bestInd);
            result.add(var);
            int ncount = g.neighbors(var).size();
            //System.out.print(((FiniteVariable) var).getID() + "\t"+ bestScore + "\t"+(ncount * (ncount - 1)) + "\t");
            //for (int i = 0; i < ties.size(); i++) {
                //System.out.print(((FiniteVariable) ties.get(i)).getID() + "\t");
            //}
            //System.out.println("");
            Graphs.removeAndConnect(g, var);
            l.remove(bestInd);
        }
        return result;
    }
}
