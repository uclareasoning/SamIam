/*
* RecursiveEliminationTreeGenerator.java
*
* Created on July 14, 2000, 2:22 PM
*/
package edu.ucla.belief.tree;
import java.util.*;
import edu.ucla.belief.*;
import edu.ucla.structure.*;
import edu.ucla.util.*;
/**
*
* @author unknown
* @version
*/
public class RecursiveEliminationTreeGenerator extends AbstractEliminationTreeGenerator {
    /** Creates new RecursiveEliminationTreeGenerator */
    public RecursiveEliminationTreeGenerator(TableIndex[] leaves,
            List eliminationOrder) {
        super(leaves, eliminationOrder);
    }
    protected Object combine(Set vertices) {
        Set s = getVariables(vertices);
        s.remove(currentVariable());
        return combineRecursively(vertices, s);
    }
    private Set getVariables(Set vertices) {
        HashSet result = new HashSet();
        for (Iterator iter = vertices.iterator(); iter.hasNext();) {
            result.addAll((Set) clusters.get(iter.next()));
        }
        return result;
    }
    private Object combineRecursively(Set vertices, Set relevantVariables) {
        if (vertices.size() == 1) {
            return vertices.iterator().next();
        } else if (vertices.size() == 2) {
            Iterator iter = vertices.iterator();
            return join(iter.next(), iter.next());
        } else if (relevantVariables.size() == 0) {
            return combineBySize(vertices);
        }
        Graph moralGraph = createMoralGraph(vertices);
        List eliminationOrder = eliminateWithoutForgetting(moralGraph,
                relevantVariables, scoreFunction);
        RecursiveEliminator eliminator =
                new RecursiveEliminator(vertices, eliminationOrder);
        return eliminator.getResult();
    }
    private Object combineBySize(Set vertices) {
        double[] sizes = new double[vertices.size()];
        Object[] verts = new Object[vertices.size()];
        int i = 0;
        for (Iterator iter = vertices.iterator(); iter.hasNext(); i++) {
            verts[i] = iter.next();
            sizes[i] = -FiniteVariableImpl.size((Set) clusters.get(verts[i]));
        }
        Heap h = new Heap(verts, sizes);
        while (h.size() > 1) {
            Object o1 = h.extractMax().element();
            Object o2 = h.extractMax().element();
            Object result = join(o1, o2);
            double score = -FiniteVariableImpl.size((Set) clusters.get(result));
            h.insert(result, score);
        }
        return h.extractMax().element();
    }
    private Graph createMoralGraph(Set vertices) {
        Graph g = new HashGraph();
        for (Iterator iter = vertices.iterator(); iter.hasNext();) {
            Object vert = iter.next();
            Set vars = (Set) clusters.get(vert);
            g.addAll(vars);
            connectAll(g, vars);
        }
        return g;
    }
    private class RecursiveEliminator extends BucketEliminator {
        Set relevantVariables;
        RecursiveEliminator(Set vertices, List eliminationOrder) {
            relevantVariables = new HashSet(eliminationOrder);
            initialize(eliminationOrder, vertices, true);
        }
        protected Collection variables(Object obj) {
            return (Set) clusters.get(obj);
        }
        protected Set combine(Set objects, Object bucket) {
            if (objects.size() == 0) {
                return Collections.EMPTY_SET;
            } else {
                Set variables;
                if (bucket == null) {
                    variables = Collections.EMPTY_SET;
                } else {
                    variables = getVariables(objects);
                    variables.retainAll(relevantVariables);
                    variables.remove(bucket);
                }
                return Collections.singleton(
                        combineRecursively(objects, variables));
            }
        }
        protected Object getResult() {
            return eliminate().iterator().next();
        }
    }
    private static EliminationOrders.ScoreFunction scoreFunction =
            EliminationOrders.MIN_SIZE;
    private static List eliminateWithoutForgetting(Graph moralGraph,
            Set vars, EliminationOrders.ScoreFunction scoreFunction) {
        Set variables = new HashSet(vars);
        Graph g = new HashGraph(moralGraph);
        double[] scores = new double[variables.size()];
        Object[] varArray = new Object[variables.size()];
        Iterator iter = variables.iterator();
        for (int i = 0; i < scores.length; i++) {
            varArray[i] = iter.next();
            scores[i] = -scoreFunction.score(varArray[i], g);
        }
        Heap heap = new Heap(varArray, scores);
        List result = new ArrayList(scores.length);
        for (int i = 0; i < scores.length; i++) {
            Object v = heap.extractMax().element();
            result.add(v);
            variables.remove(v);
            Set s = new HashSet(g.neighbors(v));
            connectNeighbors(g, v);
            for (Iterator siter = s.iterator(); siter.hasNext();) {
                Object var = siter.next();
                if (variables.contains(var)) {
                    heap.setValue(var, -scoreFunction.score(var, g));
                }
            }
        }
        return result;
    }
    private static void connectNeighbors(Graph g, Object v) {
        connectAll(g, g.neighbors(v));
    }
    private static void connectAll(Graph g, Set s) {
        for (Iterator i1 = s.iterator(); i1.hasNext();) {
            Object v1 = i1.next();
            for (Iterator i2 = s.iterator(); i2.hasNext();) {
                Object v2 = i2.next();
                if (!v1.equals(v2)) {
                    g.addEdge(v1, v2);
                }
            }
        }
    }
}
