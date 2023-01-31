package edu.ucla.belief.tree;
import java.util.Set;
/**
* A cost function for the edges in a JoinTree. This can be used by
* traditionalJoinTree(Graph,List,CostFunction) to generate a join tree whose
* edges are chosen so as to minimize the cost. This is described in [2].
*/
public interface CostFunction {
    /**
     * A non-negative function representing the cost of the edge connecting the
     * two clusters.
     */
    public double cost(Set cluster1, Set cluster2);
}
