/*
* AbstractEliminationTreeGenerator.java
*
* Created on July 14, 2000, 2:10 PM
*/
package edu.ucla.belief.tree;
import edu.ucla.belief.*;
import edu.ucla.util.*;
import java.util.*;
import edu.ucla.structure.*;
public abstract class AbstractEliminationTreeGenerator extends BucketEliminator {
    int next = 0;
    // Holds the forrest of eliminations that eventually becomes the tree.
    protected DirectedGraph g;
    // Holds the Mapping from leaf nodes to the potentials they contain.
    protected Map leafPotentials;
    // Holds the mapping from nodes to their clusters.
    protected Map clusters;
    protected Set forgotten;
    boolean initialized = false;
    private List eliminationOrder;
    private TableIndex[] leaves;
    private Object currentVariable;
    public AbstractEliminationTreeGenerator(TableIndex[] leaves,
            List eliminationOrder) {
        this.leaves = leaves;
        generateNodes(leaves);
        forgotten = new HashSet();
        this.eliminationOrder = eliminationOrder;
    }
    private void initialize() {
        initialize(eliminationOrder, g, true);
        eliminate();
        initialized = true;
    }
    public EliminationTree tree() {
        if (!initialized) {
            initialize();
        }
        return createEliminationTree();
    }
    private EliminationTree createEliminationTree() {
        TableIndex[] inds = new TableIndex[g.size()];
        int[] type = new int[inds.length];
        for (int i = 0; i < leaves.length; i++) {
            type[i] = EliminationTree.VALUE;
            inds[i] = leaves[i];
        }
        for (Iterator iter = clusters.keySet().iterator();
                iter.hasNext();) {
            Object key = iter.next();
            int vert = ((Integer) key).intValue();
            if (vert < leaves.length) {
                continue;
            }
            inds[vert] = new TableIndex((Set) clusters.get(key));
            type[vert] = (g.inDegree(key) > 1) ?
                    EliminationTree.MULTIPLICATION :
                    EliminationTree.ADDITION;
        }
        return new EliminationTree(g, inds, type);
    }
    private void generateNodes(TableIndex[] leaves) {
        g = new HashDirectedGraph(2 * leaves.length - 1);
        clusters = new HashMap(2 * leaves.length - 1);
        for (int i = 0; i < leaves.length; i++) {
            Object vertex = new Integer(i);
            g.add(vertex);
            clusters.put(vertex, new HashSet(leaves[i].variables()));
        }
        next = leaves.length;
    }
    protected Collection variables(Object vertex) {
        return (Set) clusters.get(vertex);
    }
    protected Set remainingVariables(Object vertex) {
        Set result = new HashSet((Set) clusters.get(vertex));
        result.removeAll(forgotten);
        return result;
    }
    protected Object currentVariable() {
        return currentVariable;
    }
    protected Set combine(Set vertices, Object variable) {
        Set result;
        currentVariable = variable;
        if (vertices.isEmpty()) {
            result = Collections.EMPTY_SET;
        } else {
            Object head;
            if (vertices.size() > 1) {
                head = combine(vertices);
            } else {
                head = vertices.iterator().next();
            }
            result = Collections.singleton(head);
            result = Collections.singleton(
                    forget(result.iterator().next(), variable));
        }
        if (variable != null) {
            forgotten.add(variable);
        }
        return result;
    }
    // Returns the root of the tree formed by combining the vertices.
    protected abstract Object combine(Set vertices);
    protected Object join(Set vertices) {
        Object parent = new Integer(next++);
        Set cluster = new HashSet();
        for (Iterator iter = vertices.iterator(); iter.hasNext();) {
            Object vertex = iter.next();
            g.addEdge(vertex, parent);
            cluster.addAll((Set) clusters.get(vertex));
        }
        cluster.removeAll(forgotten);
        clusters.put(parent, cluster);
        return parent;
    }
    private Object forget(Object vertex, Object var) {
        Object parent = new Integer(next++);
        g.addEdge(vertex, parent);
        Set cluster = new HashSet((Set) clusters.get(vertex));
        cluster.remove(var);
        clusters.put(parent, cluster);
        return parent;
    }
    protected Object join(Object v1, Object v2) {
        Object parent = new Integer(next++);
        g.addEdge(v1, parent);
        g.addEdge(v2, parent);
        Set cluster = new HashSet((Set) clusters.get(v1));
        cluster.addAll((Set) clusters.get(v2));
        cluster.removeAll(forgotten);
        clusters.put(parent, cluster);
        return parent;
    }
}
