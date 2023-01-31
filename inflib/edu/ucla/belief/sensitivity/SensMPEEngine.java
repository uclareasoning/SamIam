package edu.ucla.belief.sensitivity;

import edu.ucla.belief.*;
import edu.ucla.util.*;

import il2.inf.structure.EliminationOrders;
import il2.bridge.*;
import il2.inf.structure.*;
import il2.inf.jointree.*;
import il2.model.*;
import il2.util.MPE;
import il2.util.IntSet;

import java.util.List;
import java.util.*;

/** @author Hei Chan
	@since 20051121
	@author Keith Cascio
*/
public class SensMPEEngine
{
	/** @author keith cascio
		@since 20060123 */
	public SensMPEEngine( BeliefNetwork bn, EliminationHeuristic heuristic ){
		this.myBeliefNetwork = bn;
		this.myHeuristic = heuristic;

		this.myConverter = new Converter();
		this.myBayesianNetwork = myConverter.convert( bn );
		List eliminationOrder = heuristic.getEliminationOrder( bn );
		il2.util.IntList order = myConverter.convert( eliminationOrder );

		this.myJoinTree = EliminationOrders.traditionalJoinTree( myBayesianNetwork, myConverter, order );

		this.myMPEAlgorithm = MPEAlgorithm.createMPEAlgorithm( myBayesianNetwork, myJoinTree );
	}

	/** @author keith cascio
		@since 20060123 */
	public SensMPEReport getResults()
	{
                EvidenceController ec = myBeliefNetwork.getEvidenceController();
                myMPEAlgorithm.setEvidence(myConverter.convert(ec.evidence()));
		double[][][] flipPoints = myMPEAlgorithm.flipPoints();

		MPE result = myMPEAlgorithm.getMPE();
		Map[] mpe = result.convertToIL1( this.myConverter );
		Map resultMap = new HashMap( myBeliefNetwork.size() );
		il2.model.Table[] il2Tables = myBayesianNetwork.cpts();
		for( int var=0; var < flipPoints[0].length; var++ ){
			double[] values = il2Tables[var].values();
			IntSet il2Vars = il2Tables[var].vars();
			int il2VarPos = il2Tables[var].vars().indexOf( var );
			Interval[] il2Data = new Interval[values.length];

			FiniteVariable il1Var = myConverter.convert( var );
                        int varSize = il1Var.size();
			Index parents = il2Tables[var].forgetIndex( var );
			int[] currentParents = new int[il2Vars.size()-1];
			int[] current = new int[il2Vars.size()];
			for (int i = 0; i < parents.sizeInt(); i++)
			{
				for (int j = 0; j < il2VarPos; j++)
					current[j] = currentParents[j];
				for (int j = il2VarPos; j < currentParents.length; j++)
					current[j+1] = currentParents[j];

				int[] blockIndexes = new int[varSize];
				Interval[] blockIntervals = new Interval[varSize];
				double[] blockVals = new double[varSize];
				double[] blockRatios = new double[varSize];
				double[] blockFlips = new double[varSize];
				boolean[] blockSigns = new boolean[varSize];
				boolean allNegative = true;
				boolean tied = false;
				for (int k = 0; k < varSize; k++)
				{
					current[il2VarPos] = k;
					blockIndexes[k] = il2Tables[var].getIndexFromFullInstance(current);
					blockVals[k] = values[blockIndexes[k]];
                                        blockRatios[k] = flipPoints[1][var][blockIndexes[k]];
					blockFlips[k] = flipPoints[0][var][blockIndexes[k]];
					if (blockVals[k] >= blockFlips[k])
					{
						blockSigns[k] = true;
						allNegative = false;
					}
					else if (blockVals[k] == blockFlips[k])
					{
						blockSigns[k] = true;
						tied = true;
					}
					else
						blockSigns[k] = false;
				}
				if (tied)
				{
					for (int k = 0; k < varSize; k++)
						if (blockSigns[k])
							blockIntervals[k] = new Interval(Double.NEGATIVE_INFINITY, 0.0);
						else
							blockIntervals[k] = new Interval(0.0, Double.POSITIVE_INFINITY);
				}
				else if (allNegative)
				{
					for (int k = 0; k < varSize; k++)
						blockIntervals[k] = new Interval(blockFlips[k] - blockVals[k], Double.POSITIVE_INFINITY);
				}
				else
				{
					double[] ratios = new double[varSize];
					int winner = winner(ratios, blockVals, blockSigns);
					double max = blockFlips[winner];
					for (int k = 0; k < varSize; k++)
					{
						if (k == winner)
							continue;
						double flip = blockVals[winner]
							- (blockRatios[winner] * blockVals[winner] - blockRatios[k] * blockVals[k]) 
							/ (blockRatios[winner] * ratios[winner] -  blockRatios[k] * ratios[k]);
						if (flip > max)
							max = flip;
					}
					for (int k = 0; k < varSize; k++)
					{
						if (k == winner)
							blockIntervals[k] = new Interval(Double.NEGATIVE_INFINITY, max - blockVals[k]);
						else
							blockIntervals[k] = new Interval(1.0, 0.0);
					}
				/*
					double sum = 0.0;
					for (int k = 0; k < varSize; k++)
					{
						sum += ratios[k] / (blockFlips[k] - blockVals[k]);
					}
					double offset = 1 / sum;
					for (int k = 0; k < varSize; k++)
						if (blockSigns[k])
							blockIntervals[k] = new Interval(Double.NEGATIVE_INFINITY, offset * ratios[k]);
						else
							blockIntervals[k] = new Interval(offset * ratios[k], Double.POSITIVE_INFINITY);
				*/
				}

				for (int k = 0; k < varSize; k++)
					il2Data[blockIndexes[k]] = blockIntervals[k];
				parents.next(currentParents);
			}

			List il1VarsInIL2Order = myConverter.convertToList( il2Vars );
			CPTShell shell = il1Var.getCPTShell();
                        TableIndex tableIndex = new TableIndex(il1VarsInIL2Order);
			int[] intoMapping = shell.index().intoMapping( tableIndex );

			Interval[] il1Data = new Interval[ il2Data.length ];
                        int[] il2Current = new int[il2Vars.size()];
			for( int i=0; i<intoMapping.length; i++ ){
				il1Data[ intoMapping[i] ] = il2Data[tableIndex.index(il2Current)];
                                il2Tables[var].next(il2Current);
			}
			boolean[] excludeArray = ExcludePolicy.getExcludeArray( il1Var );
			resultMap.put( il1Var, new SensitivityTable( shell, il1Data, excludeArray ) );
		}

		return new SensMPEReport( mpe, resultMap );
	}

	private static int winner(double[] ratios, double[] values, boolean[] signs)
	{
		int winner = -1;
		double sum = 0.0;
		for (int i = 0; i < ratios.length; i++)
			if (signs[i])
				winner = i;
			else
				sum += values[i];
		for (int i = 0; i < ratios.length; i++)
			if (signs[i])
				ratios[i] = 1.0;
			else
				ratios[i] = -values[i] / sum;
		return winner;
	}

	/** @author hei chan
		@since 20051121 */
	private SensMPEEngine( BeliefNetwork bn, InferenceEngine ie )
	{
		this.myBeliefNetwork = bn;
		this.myInferenceEngine = ie;
	}

	/** @author hei chan
		@since 20051121 */
	public SensMPEReport getResultsOld()
	{
	/* Randomize MPE and suggestions */
		Object[] array = myBeliefNetwork.toArray();
		Map mpe = new Hashtable();
		Map resultMap = new Hashtable();
		for (int i = 0; i < array.length; i++)
		{
			FiniteVariable var = (FiniteVariable)array[i];
			mpe.put(var, var.instance(Prob.random(var.size())));
		}
		for (int i = 0; i < array.length; i++)
		{
			FiniteVariable var = (FiniteVariable)array[i];
			CPTShell shell = var.getCPTShell();

			double[] theta = shell.getCPT().dataclone();
			Interval[] intervals = new Interval[theta.length];
			for (int j = 0; j < theta.length; j++)
			{
				boolean sign = Prob.random(2) == 1 ? true : false;
				if (sign)
					intervals[j] = new Interval(
						Prob.random(0.0, 1.0-theta[j]),
						Double.POSITIVE_INFINITY);
				else
					intervals[j] = new Interval(
						Double.NEGATIVE_INFINITY,
						-Prob.random(0.0, theta[j]));
			}
			boolean[] excludeArray = ExcludePolicy.getExcludeArray(var);
			resultMap.put(var, new SensitivityTable(shell, intervals,
				excludeArray));
		}

		return new SensMPEReport(new Map[1], resultMap);
	}

	private Converter myConverter;
	private BayesianNetwork myBayesianNetwork;
	private EliminationOrders.JT myJoinTree;
	private EliminationHeuristic myHeuristic;
	private MPEAlgorithm myMPEAlgorithm;

	protected BeliefNetwork myBeliefNetwork;
	protected InferenceEngine myInferenceEngine;
}
