package il2.inf.structure.minfill2;

/**
 * A base class for algorithms that produce an elimination order.  See class
 * Util for some definitions.
 *
 * @author Mark Chavira
 */

public abstract class EliminationOrderEngine {

  /**
   * Computes the minfill cost of the given node using a brute appraoch.
   *
   * @param adj the map from node to adjacency list.
   * @param adjSize the map from node to adjacency list size.
   * @param marked a map from each node to boolean that maps every node to
   *   false, may be used as scratch, and must be returned to an all false
   *   state upon exit.
   * @param n the given node.
   * @return the cost.
   */

  protected abstract double bruteCost (
   int[][] adj, int[] adjSize, boolean[] marked, int n);

  /**
   * Allocates subclass resources.
   *
   * @param cardinalities maps each node to its cardinality.
   * @param adj maps each node to its adjacency list.
   * @param adjSize maps each node to the size of its adjacency list.
   * @param marked a map from each node to boolean that maps every node to
   *   false, may be used as scratch, and must be returned to an all false
   *   state upon exit.
   */

  protected abstract void begin (
   int[] cardinalities, int[][] adj, int[] adjSize, boolean[] marked);

  /**
   * Frees subclass resources.
   *
   * @param N the number of nodes.
   */

  protected abstract void end ();

  /**
   * Updates the costs of nodes after the removal of edges extending from
   * neighbors of the removed node to the removed node.
   *
   * @param adj the map from node to adjacency list.
   * @param adjSize the map from node to adjacency list size.
   * @param removed the node that was removed.
   * @param marked a map from each node to whether or not it is the removed
   *   node or it is a one-hop neighbor of the removed node.
   */

  protected abstract void updateCosts (
   int[][] adj, int[] adjSize, int removed, boolean[] marked,
   boolean[][] marked2, IntPriorityQueue pq) throws Exception;

  /**
   * Throws an exception if some element has a score that is not the expected
   * score.  This method has high overhead and should only be called while
   * debugging.
   *
   * @param adj the map from node to adjacency list.
   * @param adjSize the map from node to adjacency list size.
   * @pq the priority queue.
   */

  protected void validateScores (
   int[][] adj, int[] adjSize, boolean[] marked,
   IntPriorityQueue pq) throws Exception {
    for (int i = 0; i < pq.size (); i++) {
      double expectedCost =
        bruteCost (adj, adjSize, marked, pq.nthElement (i));
      double actualCost = pq.nthKey (i);
      if (Math.abs (actualCost - expectedCost) > 0.0000001) {
        throw new Exception (
          "Score for " + i + " is " + actualCost + " but expected " +
          expectedCost);
      }
    }
  }

  /**
   * Computes a "restricted" elimination order for a subset of nodes.  The
   * parameter nodes specifies an ordered partition of the subset.  It must
   * have dimension >0.  Variables in nodes[0] appear first in the order,
   * in nodes[1] second, etc.  Nodes within an element of the partition are
   * ordered in a way dictated by the subclass.  For a normal elimination
   * order, nodes will have only one dimension.  For MAP, it will have two
   * dimensions.  When constructing an ordering on clauses from an ordering
   * on bn variables in the context of AC inference, the nodes representing
   * clauses will be partitioned according to the bn variables wherein they
   * derive, and the partitions will be ordered according to the bn variable
   * ordering.  Other uses for this feature abound.  The method allows the
   * client to specifies a random generator.  This generator is used to
   * shuffle the nodes in the parameter named nodes, which has the effect of
   * breaking ties differently.  The random generator may be specified as
   * null, in which case no shuffling occurs.
   *
   * @param r a random generator used to shuffle the order of the nodes; may
   *   be null, in which case the nodes are not shuffled.  Shuffling has the
   *   effect of breaking ties differently.
   * @param cardinalities maps each node to its cardinality.
   * @param maps each node to its adjacency list.
   * @param nodes an ordered partition of the nodes to order.
   * @return the order.
   */

  public int[] order (
   java.util.Random r, int[] cardinalities, int[][] g, int[][] nodes)
   throws Exception {

    // Create some data structures for use throughout.  N is the number of
    // nodes.  At various times, we will mark a certain nodes n by setting
    // mark[n].  Each time we remove a node from the graph, we will have need for an
    // array of such markings: one for each one-hop neighbor of the removed
    // node.  The array will be parallel in its first index to the adjacency
    // list of the removed node.  marked is this array.  We create our own
    // graph data structure.  adj maps each variable to its ajadjacency list.
    // Each adjacency list is implemented with an array of size N - 1.  The
    // variables in the list are stored at the front of the array.  adjSize
    // maps each variable to the size of its adjacency list.  ans is the
    // order being constructed.
    // Mark the removed node and its one hop neighbors.  For each one-hop
    // neighbor, set up a marking of its one-hop neighbors.
    // pq is the priority queue.

    int N = g.length;
    boolean[] marked = new boolean[N];
    boolean[][] marked2 = new boolean[Util.BIGGEST_POSSIBLE_CLUSTER - 1][N];
    int[][] adj = new int[N][];
    int[] adjSize = new int[N];
    Util.initGraph (g, adj, adjSize);
    int[] ans;
    {
      int l = 0;
      for (int i = 0; i < nodes.length; i++) {
        l += nodes[i].length;
      }
      ans = new int[l];
    }
    int ansIndex = 0;
    IntPriorityQueue pq =
      new IntPriorityQueue (N);
    begin (cardinalities, adj, adjSize, marked);

    // For each element of the partition of the subset of nodes we are to
    // order, first add the nodes of the element to a priority queue.  Then
    // enter another loop.  For each interation of the inner loop, choose a
    // variable to remove, prepare to remove and connect, update the costs,
    // and finally remove and connect.
    for (int pe = 0; pe < nodes.length; pe++) {

      // Shuffle the nodes.

      if (r != null) {
        java.util.ArrayList l = new java.util.ArrayList ();
        for (int i = 0; i < nodes[pe].length; i++) {
          l.add (new Integer (nodes[pe][i]));
        }
        java.util.Collections.shuffle (l, r);
        for (int i = 0; i < nodes[pe].length; i++) {
          nodes[pe][i] = ((Integer)l.get (i)).intValue ();
        }
      }

      // Compute the initial costs.

      {
        double[] costs = new double[nodes[pe].length];
        for (int i = 0; i < nodes[pe].length; i++) {
          int n = nodes[pe][i];
          costs[i] = bruteCost (adj, adjSize, marked, n);
        }
        pq.clear (nodes[pe], costs);
        //pq.validateHeapProperty ();
        //validateScores (adj, adjSize, marked, pq);
      }

      while (pq.size () > 0) {

        // Remove the next node and update the costs.

        int removed = pq.remove ();
        ans[ansIndex++] = removed;

        //java.io.PrintStream out = System.out;
        //out.println ("Removing " + removed);
        Util.prepareForUpdate (adj, adjSize, removed, marked, marked2);
        updateCosts (adj, adjSize, removed, marked, marked2, pq);
        Util.update (adj, adjSize, removed, marked, marked2);
        /*
        out.println (pq);
        for (int n = 0; n < adj.length; n++) {
          if (pq.contains (n)) {
            out.print (n + ":");
            for (int i = 0; i < adjSize[n]; i++) {
              out.print (" " + adj[n][i]);
            }
            out.println ();
          }
        }
        */

        //pq.validateHeapProperty ();
        //validateScores (adj, adjSize, marked, pq);

      }
    }

    // Free subclass resources and return the result.

    end ();
    return ans;

  }



  public int[] safeOrder ( int[] cardinalities, int[][] g, int[][] nodes,
						   double low ) throws Exception {
	  // this is copied from order
    int N = g.length;
    boolean[] marked = new boolean[N];
    boolean[][] marked2 = new boolean[Util.BIGGEST_POSSIBLE_CLUSTER - 1][N];
    int[][] adj = new int[N][];
    int[] adjSize = new int[N];
    Util.initGraph (g, adj, adjSize);
    int[] ans;
    {
      int l = 0;
      for (int i = 0; i < nodes.length; i++) {
        l += nodes[i].length;
      }
      ans = new int[l];
    }
    int ansIndex = 0;
    IntPriorityQueue pq = new IntPriorityQueue (N);
    begin (cardinalities, adj, adjSize, marked);

	// we need to maintain low for almost simplicial rules
	//low = 0.0;
	//System.out.print("("+low+")");
    for (int pe = 0; pe < nodes.length; pe++) {

		double[] costs = new double[nodes[pe].length];
        for (int i = 0; i < nodes[pe].length; i++) {
			int n = nodes[pe][i];
			costs[i] = bruteCost (adj, adjSize, marked, n);
        }
        pq.clear (nodes[pe], costs);

		// We iterate as long as simplifications are possible.  We
		// apply simplicial rules repeatedly until we can not.  We
		// then apply a single almost simplicial rule (with the hope
		// that it triggers other simplicial rules).  We stop when we
		// can apply no further rules.
		boolean simplified = true;
		while ( simplified ) {
			simplified = false;
			if ( pq.size() == 0 ) {
			} else if ( pq.highestKey() <= 0.0 ) {
				// This happens naturally when we have a simplicial
				// node.  This will be forced to happen with almost
				// simplicial nodes.

				int removed = pq.remove ();
				ans[ansIndex++] = removed;

				double cliqueSize = logSize(cardinalities,adj[removed],adjSize[removed]);
				if ( cliqueSize > low ) {
					low = cliqueSize;
					//System.out.print("("+low+")");
				}

				Util.prepareForUpdate (adj, adjSize, removed, marked, marked2);
				updateCosts (adj, adjSize, removed, marked, marked2, pq);
				Util.update (adj, adjSize, removed, marked, marked2);
				simplified = true;

				//System.out.print(" ..." + removed);
			} else {
				// When we can not apply a simplicial rule, we look
				// for an application of an almost simplicial rule.
				// If we identify a case, we force it's score to zero,
				// so that it'll be removed at the top of the loop.
				int cur, numn;
				double curCliqueSize;
				for (int i = 0; i < pq.size(); i++) {
					cur = pq.nthElement(i);
					numn = adjSize[cur]; // number of neighbors of cur
					curCliqueSize = logSize(cardinalities,adj[cur],adjSize[cur]);
					if ( low >= curCliqueSize /* &&
												 pq.nthKey(i) <= numn */ ) { // AC: what is key

						// to identify an almost simplicial node, we
						// need to identify if the neighbors of cur
						// form a clique of size numn-1.  To do so, we
						// count the total number of edges between the
						// neighbors.  For each neighbor, we subtract
						// its degree, and if the number of edges
						// among the remaining edges is the number of
						// edges in a clique of size numn-1, they must
						// form a clique.

						il2.util.IntSet neighbors = adjToIntSet(adj[cur],numn);
						il2.util.IntSet neighborsNeighbors;
						int[] edgeCounts = new int[numn];
						int totalEdgeCount = 0, neighbor;
						// count the number of edges among the
						// neighbors of cur
						for (int j = 0; j < numn; j++) {
							neighbor = adj[cur][j];
							neighborsNeighbors =
								adjToIntSet(adj[neighbor],adjSize[neighbor]);
							neighborsNeighbors =
								neighborsNeighbors.intersection(neighbors);
							totalEdgeCount += neighborsNeighbors.size();
							edgeCounts[j] = neighborsNeighbors.size();
						}
						totalEdgeCount /= 2;

						// the number of edges in a clique of size
						// numn-1 is sum_{i=1}^{n-2} i.  this is simply
						// (numn-1)*(numn-2)/2.

						int cliqueSize = (numn-1)*(numn-2)/2;
						for (int j = 0; j < numn; j++) {
							// the first condition checks for clique,
							// the second condition checks applicability of almost simplicial rule
							if (((totalEdgeCount-edgeCounts[j]) == cliqueSize)
								&& (logSize(cardinalities,cur) >= logSize(cardinalities,adj[cur][j])) ) {
								//System.out.println("\n4:" + cliqueSize);
								//System.out.print(" ???" + cur);
								pq.setKey(cur,0.0);
								simplified = true;
								break;
							}
						}
						if ( simplified ) break;
					} // end if
				} // end for
			} // end else
			/*
			if ( ! simplified ) {
				// try to increase low
				int min = 0,cur,numn;
				for (int i = 0; i < pq.size(); i++) {
					cur = pq.nthElement(i);
					numn = adjSize[cur]; // number of neighbors of cur
					if ( numn < min ) min = numn;
				}
				if ( low < min ) {
					low = min;
					simplified = true;
				  //System.out.print("["+low+"]");
				}
			}
			*/
		} // end while
		//System.out.print("("+low+")");
    }

	// this copies the result into its own properly sized array
	int[] finalans = new int[ansIndex];
	for (int i = 0; i < ansIndex; i++)
		finalans[i] = ans[i];

    end ();

    return finalans;
  }

	private il2.util.IntSet adjToIntSet(int[] adj, int adjSize) {
		il2.util.IntSet s = new il2.util.IntSet(adjSize);
		for (int i = 0; i < adjSize; i++)
			s.add(adj[i]);
		return s;
	}

    private static final double LOG2=Math.log(2);
	private double logSize(int[] cardinalities, int[] neighbors, int numn) {
		double size = 0.0;
		for (int i = 0; i < numn; i++)
			size += Math.log(cardinalities[neighbors[i]])/LOG2;
		return size;
	}

	private double logSize(int[] cardinalities, int neighbor) {
		double size = Math.log(cardinalities[neighbor])/LOG2;
		return size;
	}

}
