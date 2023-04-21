package il2.inf.structure.minfill2;

/**
 * An EliminationOrderEngine that uses minfill.
 *
 * @author Mark Chavira
 */

public class MinfillEoe extends EliminationOrderEngine {

  /**
   * @see EliminationOrderAlgorithm
   */

  protected double bruteCost (
   int[][] adj, int[] adjSize, boolean[] marked, int n) {
    double ans = 0.0;
    for (int i = 0; i < adjSize[n]; i++) {
      int oneHop1 = adj[n][i];
      Util.fillNeighborMap (adj, adjSize, oneHop1, marked, true);
      for (int j = i + 1; j < adjSize[n]; j++) {
        int oneHop2 = adj[n][j];
        if (!marked[oneHop2]) {
          ans += 1.0;
        }
      }
      Util.fillNeighborMap (adj, adjSize, oneHop1, marked, false);
    }
    return ans;
  }
  
  /**
   * @see EliminationOrderAlgorithm
   */
  
  public void begin (
   int[] cardinalities, int[][] adj, int[] adjSize, boolean[] marked) {}
  
  /**
   * @see EliminationOrderAlgorithm
   */
  
  public void end () {}

  /**
   * @see EliminationOrderAlgorithm
   */
  
  public void updateCosts (
   int[][] adj, int[] adjSize, int removed, boolean[] marked,
   boolean[][] marked2, IntPriorityQueue pq) throws Exception {
    for (int i = 0; i < adjSize[removed]; i++) {
      int oneHop = adj[removed][i];
      if (pq.contains (oneHop)) {
        for (int j = 0; j < adjSize[oneHop]; j++) {
          int twoHop = adj[oneHop][j];
          if (!marked[twoHop]) {
//System.out.println ("  decrementing1 " + oneHop);
            pq.incrementKey (oneHop, -1.0);
          }
        }
      }
    }
    for (int i = 0; i < adjSize[removed]; i++) {
      int oneHop1 = adj[removed][i];
      for (int j = i + 1; j < adjSize[removed]; j++) {
        int oneHop2 = adj[removed][j];
        if (!marked2[i][oneHop2]) {
          for (int k = 0; k < adjSize[oneHop1]; k++) {
            int twoHop = adj[oneHop1][k];
            if (marked2[j][twoHop] && pq.contains (twoHop)) {
//System.out.println ("  decrementing2 " + twoHop);
              pq.incrementKey (twoHop, -1.0);
            }
          }
          if (pq.contains (oneHop1)) {
            for (int k = 0; k < adjSize[oneHop1]; k++) {
              int twoHop = adj[oneHop1][k];
              if (!marked[twoHop] && !marked2[j][twoHop]) {
//System.out.println ("  incrementing1 " + oneHop1);
                pq.incrementKey (oneHop1, +1.0);
              }
            }
          }
          if (pq.contains (oneHop2)) {
            for (int k = 0; k < adjSize[oneHop2]; k++) {
              int twoHop = adj[oneHop2][k];
              if (!marked[twoHop] && !marked2[i][twoHop]) {
//System.out.println ("  incrementing2 " + oneHop2);
                pq.incrementKey (oneHop2, +1.0);
              }
            }
          }
        }
      }
    }
  }

}
