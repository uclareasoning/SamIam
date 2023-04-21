/*
* MinPairCostTreeGenerator.java
*
* Created on July 14, 2000, 2:16 PM
*/
package edu.ucla.belief.tree;
import edu.ucla.belief.*;
import edu.ucla.util.*;
import java.util.*;
import edu.ucla.structure.*;
/**
*
* @author unknown
* @version
*/
public class MinPairCostTreeGenerator extends AbstractEliminationTreeGenerator {
    CostFunction cost;
    public MinPairCostTreeGenerator(TableIndex[] leaves,
            List eliminationOrder, CostFunction cost) {
        super(leaves, eliminationOrder);
        this.cost = cost;
    }
    protected Object combine(Set vertices) {
        List remaining = new ArrayList(vertices);
        Map remainingVars = new HashMap(vertices.size() + 1);
        for (Iterator iter = remaining.iterator(); iter.hasNext();) {
            Object vertex = iter.next();
            remainingVars.put(vertex, remainingVariables(vertex));
        }
        while (remaining.size() > 1) {
            int best1 = 0;
            int best2 = 1;
            double bestCost = Double.POSITIVE_INFINITY;
            for (int i = 0; i < remaining.size(); i++) {
                for (int j = i + 1; j < remaining.size(); j++) {
                    double currentCost = cost.cost(
                            (Set) remainingVars.get(remaining.get(i)),
                            (Set) remainingVars.get(remaining.get(j)));
                    if (currentCost < bestCost) {
                        best1 = i;
                        best2 = j;
                        bestCost = currentCost;
                    }
                }
            }
            Object vertex =
                    join(remaining.get(best1), remaining.get(best2));
            remainingVars.remove(remaining.get(best2));
            remainingVars.remove(remaining.get(best1));
            remaining.remove(best2);
            remaining.remove(best1);
            remaining.add(vertex);
            remainingVars.put(vertex, variables(vertex));
        }
        return remaining.get(0);
    }
}
