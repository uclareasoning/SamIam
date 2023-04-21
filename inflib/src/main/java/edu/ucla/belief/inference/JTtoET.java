/*
* JTtoET.java
*
* Created on June 26, 2000, 8:34 AM
*/
package edu.ucla.belief.inference;
import edu.ucla.belief.*;
import edu.ucla.belief.tree.*;
import edu.ucla.structure.*;
import edu.ucla.util.Assert;

import java.util.*;
/**
*
* @author unknown
* @version
*/
public class JTtoET extends Object
{
	/**
		@author Keith Cascio
		@since 081502
	*/
	public static boolean FLAG_DEBUG = false;

    int root;
    int[] nodeLabels;
    int[][] separatorLabels;
    int[] assignments;
    EliminationTree et;
    /** Creates new JTtoET */
    public JTtoET(JoinTree jt, TableIndex[] leaves) {
        root = 0;
        int bestSize = (int) FiniteVariableImpl.size(jt.cluster(0));
        for (int i = 1; i < jt.tree().size(); i++) {
            if (FiniteVariableImpl.size(jt.cluster(i)) < bestSize) {
                bestSize = (int) FiniteVariableImpl.size(jt.cluster(i));
                root = i;
            }
        }
        relabelNodes(jt.tree(), leaves.length);
        computeAssignments(jt, leaves);
        generateTree(jt, leaves);
    }
    public EliminationTree getEliminationTree() {
        return et;
    }
    private void generateTree(JoinTree jt, TableIndex[] leaves) {
        DirectedGraph dg = new HashDirectedGraph();
        for (int i = 0; i < nodeLabels.length; i++) {
            dg.add(new Integer(nodeLabels[i]));
            for (int j = 0; j < separatorLabels[i].length; j++) {
                if (separatorLabels[i][j] > nodeLabels[i]) {
                    dg.addEdge(new Integer(nodeLabels[i]),
                            new Integer(separatorLabels[i][j]));
                } else {
                    dg.addEdge(new Integer(separatorLabels[i][j]),
                            new Integer(nodeLabels[i]));
                }
            }
        }
        for (int i = 0; i < assignments.length; i++) {
            dg.addEdge(new Integer(i), new Integer(assignments[i]));
        }
        int topNode = dg.size();
        dg.addEdge(new Integer(topNode - 1), new Integer(topNode));
        TableIndex[] indices = new TableIndex[dg.size()];
        int[] type = new int[dg.size()];
        for (int i = 0; i < leaves.length; i++) {
            indices[i] = leaves[i];
            type[i] = EliminationTree.VALUE;
        }
        for (int i = 0; i < nodeLabels.length; i++) {
            indices[nodeLabels[i]] = new TableIndex(jt.cluster(i));
            type[nodeLabels[i]] = EliminationTree.MULTIPLICATION;
        }
        for (int i = 0; i < separatorLabels.length; i++) {
            for (int j = 0; j < separatorLabels[i].length; j++) {
                indices[separatorLabels[i][j]] = new TableIndex(
                        jt.separator(i, jt.tree().neighbors(i)[j]));
                type[separatorLabels[i][j]] = EliminationTree.ADDITION;
            }
        }
        indices[indices.length - 1] =
                new TableIndex(java.util.Collections.EMPTY_SET);
        type[indices.length - 1] = EliminationTree.ADDITION;
        et = new EliminationTree(dg, indices, type);
    }

	/**
		Modified by Keith Cascio 081502
		@author JD Park
	*/
	protected void computeAssignments(JoinTree jt, TableIndex[] leaves)
	{
		assignments = new int[leaves.length];
		Collection vars = null;
		Collection cluster = null;
		double size = (double)0;
		double bestSize = Double.POSITIVE_INFINITY;
		int bestInd = (int)-1;

		for (int i = 0; i < leaves.length; i++)
		{
			bestInd = (int)-1;
			bestSize = Double.POSITIVE_INFINITY;
			vars = leaves[i].variables();
			if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "**********JTtoET.computeAssignments(), leaf " +i+ " **************************\nvariables()   : " + vars );
			for (int j = 0; j < jt.tree().size(); j++)
			{
				cluster = jt.cluster(j);
				if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.print( "trying cluster: " + cluster );
				if( cluster.containsAll( vars ) )
				{
					if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( " accepted!" );
					size = FiniteVariableImpl.size( jt.cluster(j) );
					if (size < bestSize) {
						bestSize = size;
						bestInd = j;
					}
				}
				else if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println();
			}
			if( FLAG_DEBUG && bestInd == (int)-1 ) Definitions.STREAM_VERBOSE.println( "Failed to find cluster.\n\n" );

			if( FLAG_DEBUG )
			{
				//System.out.println( "i=" + i + " assignments.length=" + assignments.length + " bestInd=" + bestInd + " nodeLabels.length=" + nodeLabels.length );
				Assert.condition( i >= 0 && i < assignments.length, "assignments["+i+"]");
				Assert.condition( bestInd >= 0 && bestInd < nodeLabels.length, "nodeLabels["+bestInd+"]");
			}

			assignments[i] = nodeLabels[bestInd];
		}
	}

    private void relabelNodes(IntGraph tree, int offset) {
        nodeLabels = new int[tree.size()];
        java.util.Arrays.fill(nodeLabels, -1);
        separatorLabels = new int[tree.size()][];
        for (int i = 0; i < separatorLabels.length; i++) {
            separatorLabels[i] = new int[tree.degree(i)];
            java.util.Arrays.fill(separatorLabels[i], -1);
        }
        int endSize = relabel(tree, root, 2 * tree.size() - 1 + offset - 1);
    }
    private int relabel(IntGraph tree, int node, int index) {
        nodeLabels[node] = index;
        index--;
        int[] neighbors = tree.neighbors(node);
        for (int i = 0; i < neighbors.length; i++) {
            if (nodeLabels[neighbors[i]] == -1) {
                separatorLabels[node][i] = index;
                separatorLabels[neighbors[i]]
                        [tree.neighborIndex(neighbors[i], node)] = index;
                index--;
                index = relabel(tree, neighbors[i], index);
            }
        }
        return index;
    }
}
