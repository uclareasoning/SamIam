package edu.ucla.belief.inference;

import java.util.*;
import edu.ucla.belief.tree.*;
import edu.ucla.belief.*;
import edu.ucla.structure.IntGraph;

public class S4Compiler
{
	public static BeliefCompilation compile( BeliefNetwork bn, List eliminationOrder){
		return compile( bn, eliminationOrder, (QuantitativeDependencyHandler)null );
	}

	public static BeliefCompilation compile( BeliefNetwork bn, List eliminationOrder, QuantitativeDependencyHandler handler )
	{
		JoinTree jt = Trees.traditionalJoinTree(bn, eliminationOrder);
		return compile( bn, jt, handler );
	}

	/** @since 012904 */
	public static BeliefCompilation compile( BeliefNetwork bn, JoinTree jt ){
		return compile( bn, jt, (QuantitativeDependencyHandler)null );
	}

	public static BeliefCompilation compile( BeliefNetwork bn, JoinTree jt, QuantitativeDependencyHandler handler )
	{
		int varCount = bn.size();
		Table[] tables = new Table[2 * varCount];
		TableIndex[] indices = new TableIndex[2 * varCount];
		Map varLoc = new HashMap(varCount);
		Map famLoc = new HashMap(varCount);
		Iterator vars = bn.iterator();
		CPTShell effective = null;

		for (int i = 0; i < varCount; i++)
		{
			FiniteVariable var = (FiniteVariable) vars.next();
			tables[2 * i] = createLikelihood(var);

			if( handler == null ) effective = var.getCPTShell( var.getDSLNodeType() );
			else effective = handler.getCPTShell( var );

			tables[2 * i + 1] = effective.getCPT();//(Table) bn.getCPT(var);
			indices[2 * i] = tables[2 * i].index();
			indices[2 * i + 1] = tables[2 * i + 1].index();
			varLoc.put(var, new Integer(2 * i));
			famLoc.put(var, new Integer(2 * i + 1));
		}

		EliminationTree tree = new JTtoET(jt, indices).getEliminationTree();
		//EliminationTree tree=new NaiveETGenerator().create(indices,eliminationOrder);
		ArithmeticExpression expr = compileExpression(tree, tables, bn);
		return new BeliefCompilation( bn, expr, indices, varLoc, famLoc, jt, tables );
	}

	private static ArithmeticExpression compileExpression(EliminationTree tree,
		Table[] tables, Collection variables)
	{
		Map varInd = new HashMap(variables.size());
		int[] varSizes = new int[variables.size()];
		for (Iterator iter = variables.iterator(); iter.hasNext();)
		{
			FiniteVariable var = (FiniteVariable) iter.next();
			varSizes[varInd.size()] = var.size();
			varInd.put(var, new Integer(varInd.size()));
		}

		int[][] nodeVars = new int[tree.size()][];
		for (int i = 0; i < nodeVars.length; i++)
		{
			nodeVars[i] = getVarsInds(varInd, tree.index(i));
		}

		int clusterCount = 0;
		int[] newValue = new int[tree.size()];
		for (int i = 0; i < newValue.length; i++)
		{
			if (tree.type(i) == EliminationTree.ADDITION && tree.parent(i) >= 0)
			{
				newValue[i] = -1;
			}
			else
			{
				newValue[i] = clusterCount++;
			}
		}

		int[][] clusterVars = new int[clusterCount][];
		int[][] neighbors = new int[clusterCount][];
		int[][][] sepVars = new int[clusterCount][][];
		for (int i = 0; i < newValue.length; i++)
		{
			if (newValue[i] >= 0)
			{
				clusterVars[newValue[i]] = nodeVars[i];
				if (tree.parent(i) < 0)
				{
					neighbors[newValue[i]] = new int[1];
					neighbors[newValue[i]][0] =
					newValue[tree.children(i)[0]];
					sepVars[newValue[i]] = new int[1][0];
				}
				else if (tree.type(i) == EliminationTree.VALUE)
				{
					neighbors[newValue[i]] = new int[1];
					neighbors[newValue[i]][0] = newValue[tree.parent(i)];
					sepVars[newValue[i]] = new int[][]
					{
						nodeVars[i]
					};
				}
				else
				{
					int[] children = tree.children(i);
					neighbors[newValue[i]] = new int[children.length + 1];
					sepVars[newValue[i]] = new int[children.length + 1][];
					for (int j = 0; j < children.length; j++)
					{
						neighbors[newValue[i]][j] = newValue[
						getChildNeighbor(children[j], tree)];
						sepVars[newValue[i]][j] = nodeVars[children[j]];
					}

					neighbors[newValue[i]][children.length] = newValue[
					getParentNeighbor(tree.parent(i), tree)];
					sepVars[newValue[i]][children.length] =
					nodeVars[tree.parent(i)];
				}
			}
		}

		ArithmeticExpression result;
		result = new S4DoubleExpression(new IntGraph(neighbors),clusterVars,		      sepVars, varSizes);

		for (int i = 0; i < tables.length; i++)
		{
			result.setParameter(i, tables[i].dataclone());
		}

		return result;
	}

	private static int[] getVarsInds(Map varInds, TableIndex ind)
	{
		//FiniteVariable[] vars = ind.getVariableArray();
		//int[] result = new int[vars.length];
		//for (int i = 0; i < vars.length; i++) {
		//    result[i] = ((Integer) varInds.get(vars[i])).intValue();
		//}
		List vars = ind.variables();
		int[] result = new int[vars.size()];
		int counter = 0;
		for( Iterator it = vars.iterator(); it.hasNext(); )
		{
			result[counter++] = ((Integer) varInds.get(it.next())).intValue();
		}

		return result;
	}

	private static int getParentNeighbor(int node, EliminationTree tree)
	{
		if(tree.parent(node) < 0) return node;
		else return tree.parent(node);
	}

	private static int getChildNeighbor(int ind, EliminationTree tree)
	{
		if (tree.children(ind).length == 0) return ind;
		else return tree.children(ind)[0];
	}

	private static Table createLikelihood(FiniteVariable var)
	{
		Table result = new Table(new FiniteVariable[]{ var } );
		result.fill(1);
		return result;
	}
}

