package edu.ucla.belief.tree;
import edu.ucla.belief.*;
import edu.ucla.structure.*;
import java.util.*;
import edu.ucla.belief.inference.ArithmeticExpression;
public class EliminationTree {
    public static final int MULTIPLICATION =
            ArithmeticExpression.MULTIPLICATION;
    public static final int ADDITION = ArithmeticExpression.ADDITION;
    public static final int VALUE = ArithmeticExpression.VALUE;
    private DirectedGraph tree;
    private TableIndex[] indices;
    private int[] type;
    /**
     * Creates an Elimination Tree.
     * @param tree A tree whose nodes are integers, that are ordered in
     * a topological ordering where all of the leaves appear as 0 to N-1 where
     * N is the number of leaves.
     * @param inds A TableIndex for each of the nodes.
     * @param type An integer describing the type of operation(One of ADDITION,
     MULTIPLICATION, or VALUE).
     */
    public EliminationTree(DirectedGraph tree, TableIndex[] inds,
            int[] type) {
        this.tree = new HashDirectedGraph(tree);
        indices = (TableIndex[]) inds.clone();
        this.type = (int[]) type.clone();
    }
    /**
     * @return -1 if parent is the root, the parent of node otherwise.
     */
    public int parent(int node) {
        Collection parent = tree.outGoing(new Integer(node));
        if (parent.size() == 0) {
            return -1;
        } else {
            return ((Integer) parent.iterator().next()).intValue();
        }
    }
    public int[] children(int node) {
        Collection children = tree.inComing(new Integer(node));
        int[] result = new int[children.size()];
        Iterator iter = children.iterator();
        for (int i = 0; i < result.length; i++) {
            result[i] = ((Integer) iter.next()).intValue();
        }
        return result;
    }
    public TableIndex index(int node) {
        return indices[node];
    }
    public int type(int node) {
        return type[node];
    }
    public int size() {
        return tree.size();
    }
}
