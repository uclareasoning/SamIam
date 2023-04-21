package il2.inf.structure.minfill2;

/**
 * Static utility routines.
 * <p>
 * We represent nodes as integers 0..N-1.  We represent a graph as an array
 * that maps each node to its adjacency list.  We represent an adjacency as
 * an int array containing one-hop neighbors in no particular order.
 * <p>
 * Internally, we work with a different graph representation.  This
 * representation is the same as the previous, except that the length of each
 * adjacency list is not necessarily the same as the number of elements in
 * the list.  The second representation can be identified, because it is
 * always be accompanied by an array that maps from each node to the size of
 * its adjacency list.
 *
 * @author Mark Chavira
 */

public class Util {

  /**
   * A constant for the largest cluster possible.  Minfill will fail if a
   * variable is eliminated that has a cluster bigger.
   */

 public static final int BIGGEST_POSSIBLE_CLUSTER = 2000;

  /**
   * Returns a map from each variable to the log base 2 of its cardinality.
   *
   * @param cardinalities a map from each variable to its cardinality.
   * @return the map from each variable to the log of its cardinality.
   */
  
  public static double[] logCardinalities (int[] cardinalities) {
    double LOG2 = Math.log (2.0);
    double[] ans = new double[cardinalities.length];
    for (int i = 0; i < ans.length; i++) {
      ans[i] = Math.log (cardinalities[i]) / LOG2;
    }
    return ans;
  }
  
  /**
   * Returns the log base two of the maximum cluster size of the given
   * elimination order when applied to the given graph.
   *
   * @param eo the given elimination order.
   * @param cardinalities a map from each node to its cardinality.
   * @param g the given graph.
   * @return the log base two of the maximum cluster size.
   */
  
  public static double logMaxClusterSize (
   int[] eo, int[] cardinalities, int[][] g) {
    int N = g.length;
    boolean[] marked = new boolean[N];
    boolean[][] marked2 = new boolean[BIGGEST_POSSIBLE_CLUSTER - 1][N];
    int[][] adj = new int[N][];
    int[] adjSize = new int[N];
    initGraph (g, adj, adjSize);
    double[] logCardinalities = logCardinalities (cardinalities);
    double ans = Double.MIN_VALUE;
    for (int eoIndex = 0; eoIndex < eo.length; eoIndex++) {
      int removed = eo[eoIndex];
      double clusterSize = logCardinalities[removed];
      for (int j = 0; j < adjSize[removed]; j++) {
        int oneHop = adj[removed][j];
        clusterSize += logCardinalities[oneHop];
      }
      ans = Math.max (ans, clusterSize);
      Util.prepareForUpdate (adj, adjSize, removed, marked, marked2);
      Util.update (adj, adjSize, removed, marked, marked2);
    }
    return ans;
  }

  /**
   * Fills the given neighbor map for the given node.
   *
   * @param adj a map from each node to its adjacency list.
   * @param adjSize a map from each node to the size of its adjacency list.
   * @param n the given node.
   * @param map the given neighbor map.
   * @param val the given value.
   */

  public static void fillNeighborMap (
   int[][] adj, int[] adjSize, int n, boolean[] map, boolean val) {
    for (int i = 0; i < adjSize[n]; i++) {
      int oneHop = adj[n][i];
      map[oneHop] = val;
    }
  }

  /**
   * Fills the given map with the results of calling fillNeigbhorMap on each
   * neighbor of the given node.
   *
   * @param adj a map from each node to its adjacency list.
   * @param adjSize a map from each node to the size of its adjacency list.
   * @param n the given node.
   * @param map the given neighbor map.
   * @param val the given value.
   */
   
  public static void fillNeighborMap (
   int[][] adj, int[] adjSize, int n, boolean[][] map, boolean val) {
    for (int i = 0; i < adjSize[n]; i++) {
      int oneHop = adj[n][i];
      fillNeighborMap (adj, adjSize, oneHop, map[i], val);
    }
  }

  /**
   * Initializes a graph in the internal representation from a graph in the
   * external representation.
   * 
   * @param g the given external graph.
   * @param on entry, an array of the same dimension as ext; on exit, a map
   *   from each node to its adjacency list.
   * @param adjSize on entry, an array of the same dimension as ext; on
   *   exit, a map from each node to the size of its adjacency list.
   */
  
  public static void initGraph (int[][] g, int[][] adj, int[] adjSize) {
    for (int n = 0; n < adj.length; n++) {
      adj[n] = new int[g[n].length * 2];
      adjSize[n] = g[n].length;
      System.arraycopy (g[n], 0, adj[n], 0, adjSize[n]);
    }
  }

  public static void prepareForUpdate (
   int[][] adj, int[] adjSize, int removed, boolean[] marked,
   boolean[][] marked2) {
    fillNeighborMap (adj, adjSize, removed, marked, true);
    marked[removed] = true;
    fillNeighborMap (adj, adjSize, removed, marked2, true);
  }

  /**
   * Removes edges in one direction from each one-hop neighbor to the given
   * node.
   *
   * @param adj a map from each node to its adjacency list.
   * @param adjSize a map from each node to the size of its adjacency list.
   * @param removed the given node.
   * @param marked2 on entry, an int[BIGGEST_POSSIBLE_CLUSTER - 1][N] that
   *   is all false; on exit, marked2[i] maps each node to whether it is a
   *   neighbor of the former ith neigbhor of the removed node.
   */
  
  public static void update (
   int[][] adj, int[] adjSize, int removed, boolean[] marked,
   boolean[][] marked2) {
    fillNeighborMap (adj, adjSize, removed, marked, false);
    marked[removed] = false;
    for (int i = 0; i < adjSize[removed]; i++) {
      int oneHop = adj[removed][i];
      for (int j = 0; j < adjSize[oneHop]; j++) {
        if (adj[oneHop][j] == removed) {
          adj[oneHop][j] = adj[oneHop][--adjSize[oneHop]];
          break;
        }
      }
      marked2[i][removed] = false;
    }
    for (int i = 0; i < adjSize[removed]; i++) {
      int oneHop1 = adj[removed][i];
      fillNeighborMap (adj, adjSize, oneHop1, marked2[i], false);
      for (int j = i + 1; j < adjSize[removed]; j++) {
        int oneHop2 = adj[removed][j];
        if (!marked2[j][oneHop1]) {
          if (adjSize[oneHop1] == adj[oneHop1].length) {
            int[] scratch = new int[adjSize[oneHop1] * 3 / 2];
            System.arraycopy (adj[oneHop1], 0, scratch, 0, adjSize[oneHop1]);
            adj[oneHop1] = scratch;
          }
          if (adjSize[oneHop2] == adj[oneHop2].length) {
            int[] scratch = new int[adjSize[oneHop2] * 3 / 2];
            System.arraycopy (adj[oneHop2], 0, scratch, 0, adjSize[oneHop2]);
            adj[oneHop2] = scratch;
          }
          adj[oneHop1][adjSize[oneHop1]++] = oneHop2;
          adj[oneHop2][adjSize[oneHop2]++] = oneHop1;
        }
      }
    }
  }

}
