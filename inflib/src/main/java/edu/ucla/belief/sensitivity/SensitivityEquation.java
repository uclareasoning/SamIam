package edu.ucla.belief.sensitivity;

import edu.ucla.belief.*;
import java.util.*;

public class SensitivityEquation
{
	protected PartialDerivativeEngine pde;
	protected double prob;
	protected Map alphasMap;
	protected Map excludeMap;

	public SensitivityEquation(PartialDerivativeEngine pde, double
		prob, Map alphasMap, Map excludeMap) {
		this.pde = pde;
		this.prob = prob;
		this.alphasMap = alphasMap;
		this.excludeMap = excludeMap;
	}

	public SensitivityEquation(List vars, InferenceEngine ie,
		PartialDerivativeEngine pde) {
		this.pde = pde;
		prob = ie.probability();
		alphasMap = new Hashtable();
		excludeMap = new Hashtable();
		for (int i = 0; i < vars.size(); i++) {
			FiniteVariable varX = (FiniteVariable)vars.get(i);
			alphasMap.put(varX, computeAlphas(pde, varX));
			ExcludePolicy policy = ExcludePolicy.
				getExcludePolicy(varX);
			if (policy == ExcludePolicy.INCLUDE)
				excludeMap.put(varX, policy);
			else
				excludeMap.put(varX, ExcludePolicy.
					getExcludeArray(varX));
		}
	}

	public double getProb() {
		return prob;
	}

	private static boolean matchExclude(Object obj1, Object obj2) {
		if (obj1 == null || obj2 == null)
			return false;
		if (obj1 instanceof ExcludePolicy)
			if (obj2 instanceof ExcludePolicy)
				return true;
			else if (obj2 instanceof boolean[]) {
				boolean[] excludeArray = (boolean[])obj2;
				boolean[] falseArray = new
					boolean[excludeArray.length];
				Arrays.fill(falseArray, false);
				return Arrays.equals(excludeArray,
					falseArray);
			}
		else if (obj1 instanceof boolean[])
			if (obj2 instanceof ExcludePolicy) {
				boolean[] excludeArray = (boolean[])obj1;
				boolean[] falseArray = new
					boolean[excludeArray.length];
				Arrays.fill(falseArray, false);
				return Arrays.equals(excludeArray,
					falseArray);
			}
			else if (obj2 instanceof boolean[])
				return Arrays.equals((boolean[])obj1,
					(boolean[])obj2);
		return false;
	}

	public double[] getAlphas(FiniteVariable varX) {
		Object obj1 = excludeMap.get(varX), obj2;
		ExcludePolicy policy = ExcludePolicy.
			getExcludePolicy(varX);
		if (policy == ExcludePolicy.INCLUDE)
			obj2 = policy;
		else
			obj2 = ExcludePolicy.getExcludeArray(varX);
		if (matchExclude(obj1, obj2))
			return (double[])alphasMap.get(varX);
		double[] alphas = computeAlphas(pde, varX);
		alphasMap.put(varX, alphas);
		if (policy == ExcludePolicy.INCLUDE)
			excludeMap.put(varX, policy);
		else
			excludeMap.put(varX, ExcludePolicy.
				getExcludeArray(varX));
		return alphas;
	}

/*	public boolean skip(FiniteVariable varX) {
		EvidenceController ec = bn.getEvidenceController();
		if (ec.getValue(varX) == null)
			return false;

		boolean flag = true;
		List cptVars = varX.getCPTShell( varX.getDSLNodeType() ).getCPT().variables();
		for (Iterator it = cptVars.iterator(); it.hasNext(); ) {
			if (ec.getValue((FiniteVariable)it.next()) ==
				null)
			{
				flag = false;
				break;
			}
		}
		return flag;
	}
*/
	public static double[] computeAlphas(PartialDerivativeEngine pde,
		FiniteVariable varX) {
		CPTShell shell = varX.getCPTShell( varX.getDSLNodeType() );
		if (shell instanceof NoisyOrShellPearl)
			return computeNoisyOrPearlAlphas(pde, varX);
		if (shell instanceof NoisyOrShellHenrion)
			return computeNoisyOrHenrionAlphas(pde, varX);

		double[] partial = (double[])pde.familyPartial(varX).
			dataclone();
		double[] thetas = (double[])shell.getCPT().dataclone();
		double[] alphas = new double[thetas.length];
		Arrays.fill(alphas, 0.0);
		boolean[] excludeArray = ExcludePolicy.getExcludeArray(varX);
		int sizeVarX = varX.size();

		for (int i = 0; i < thetas.length; i += sizeVarX )
		{
			int nonExclude = 0;
			double sumTheta = 0.0;
			double sum = 0.0;

			for( int j = i; j < i + sizeVarX; j++ )
			{
				if( excludeArray[j] )
					continue;
				nonExclude++;
				sumTheta += thetas[j];
				sum += thetas[j] * partial[j];
			}

			if( nonExclude < 2 )
				continue;
			for (int j = i; j < i + sizeVarX; j++)
			{
				if( excludeArray[j] )
					continue;
				if (thetas[j] == sumTheta)
				{
					for (int k = i; k < i + sizeVarX; k++)
					{
						if(k != j) alphas[j] -= partial[k];
					}
					alphas[j] /= nonExclude - 1;
					alphas[j] += partial[j];
				}
				else alphas[j] = (sumTheta * partial[j] - sum) / (sumTheta - thetas[j]);
			}
		}
		return alphas;
	}

	private static double[] computeNoisyOrHenrionAlphas(
		PartialDerivativeEngine pde, FiniteVariable varX) {
		NoisyOrShellHenrion shell =
			(NoisyOrShellHenrion) varX.getCPTShell( edu.ucla.belief.io.dsl.DSLNodeType.NOISY_OR );

		double[] partial = (double[])pde.familyPartial(varX).
			dataclone();
		double[] thetas = (double[])shell.getCPT().dataclone();
		double[] alphas = new double[thetas.length];
		Arrays.fill(alphas, 0.0);
		if (varX.size() != 2)
			return alphas;

/*		boolean[] excludeArray = ExcludePolicy.getExcludeArray(varX);
		boolean[] trueArray = new boolean[excludeArray.length];
		Arrays.fill(trueArray, true);
		if (Arrays.equals(excludeArray, trueArray))
			return alphas;
*/
		double[][][] cParams = shell.getParams();
		double[][] params = cParams[0];
		double[][] paramAlphas = new double[params.length][];
		for (int i = 0; i < params.length; i++) {
			paramAlphas[i] = new double[params[i].length];
			Arrays.fill(paramAlphas[i], 0.0);
		}

		TableIndex index = shell.getCPT().index();
		for (int i = 1; i < thetas.length; i += 2) {
			int[] mindex = index.mindex(i, null);
			for (int j = 0; j < params.length; j++) {
				if (mindex[j] == params[j].length)
					continue;
				double cpd;
				if (params[j][mindex[j]] == 0.0)
					cpd = shell.getCPD(j, mindex);
				else
					cpd = thetas[i] / params[j][mindex[j]];
				paramAlphas[j][mindex[j]] += cpd *
					(partial[i] - partial[i-1]);
			}
		}

		int d = 2;
		for (int i = params.length-1; i >= 0; i--) {
			for (int j = 0; j < params[i].length; j++) {
				int pos = thetas.length-1 - d * (params[i].length - j);
				alphas[pos] = paramAlphas[i][j];
				alphas[pos-1] = -paramAlphas[i][j];
			}
			d *= (params[i].length+1);
		}
		return alphas;

/*		double[] params = new double[size / 2 - 1];
		for (int i = 0; i < params.length; i++)
			params[i] = ((Double)noisyOrWeights.get(size - 2 *
				i - 3)).doubleValue();

		TableIndex index = cpt.index();
		int offset = 2;
		for (int i = 0; i < params.length; i++) {
			int pos = alphas.length - 1 - offset;
			for (int j = 0; j < thetas.length; j += 2) {
				int[] mindex = index.mindex(j, null);
				if (mindex[params.length - i - 1] == 1)
					continue;
				alphas[pos] += thetas[j + 1] / params[i] *
					(partial[j + 1] - partial[j]);
			}
			alphas[pos - 1] = -alphas[pos];
			offset *= 2;
		}

		for (int i = 0; i < thetas.length; i += 2) {
			int[] mindex = index.mindex(i, null);
			int k = -1;
			for (int j = 0; j < mindex.length - 1; j++)
				if (mindex[j] == 0)
					k++;
			alphas[alphas.length - 1] += thetas[i + 1] / leak
				* k * (partial[i] - partial[i + 1]);
		}
		alphas[alphas.length - 2] = -alphas[alphas.length - 1];

		return alphas;
*/
	}

	private static double[] computeNoisyOrPearlAlphas(
		PartialDerivativeEngine pde, FiniteVariable varX) {
		NoisyOrShellPearl shell =
			(NoisyOrShellPearl) varX.getCPTShell( edu.ucla.belief.io.dsl.DSLNodeType.NOISY_OR );

		double[] partial = (double[])pde.familyPartial(varX).
			dataclone();
		double[] thetas = shell.getCPT().dataclone();
		double[] weights = shell.weightsClone();
		int[] strengths = shell.strengthsClone();
		double[] alphas = new double[weights.length];
		Arrays.fill(alphas, 0.0);
		if (varX.size() != 2)
			return alphas;

/*		boolean[] excludeArray = ExcludePolicy.getExcludeArray(varX);
		boolean[] trueArray = new boolean[excludeArray.length];
		Arrays.fill(trueArray, true);
		if (Arrays.equals(excludeArray, trueArray))
			return alphas;
*/
		int[][] cIndexes = shell.getIndexes();
		double[][][] cParams = shell.getParams();
		double[] cLeaks = shell.getLeaks();
		double[][] params = cParams[0];
		double leak = cLeaks[0];
		double[][] paramAlphas = new double[params.length][];
		for (int i = 0; i < params.length; i++) {
			paramAlphas[i] = new double[params[i].length];
			Arrays.fill(paramAlphas[i], 0.0);
		}
		double leakAlpha = 0.0;

		TableIndex index = shell.getCPT().index();
		for (int i = 1; i < thetas.length; i += 2) {
			int[] mindex = index.mindex(i, null);
			for (int j = 0; j < params.length; j++) {
				int rIndex = cIndexes[j][mindex[j]];
				if (rIndex == params[j].length)
					continue;
				double cpd;
				if (params[j][rIndex] == 0.0)
					cpd = shell.getCPD(rIndex, mindex);
				else
					cpd = thetas[i] / params[j][rIndex];
				paramAlphas[j][rIndex] += cpd *
					(partial[i] - partial[i-1]);
			}
			double cpdl;
			if (leak == 0.0)
				cpdl = shell.getCPD(-1, mindex);
			else
				cpdl = thetas[i] / leak;
			leakAlpha += cpdl * (partial[i] - partial[i-1]);
		}

		int wp = 0;
		for (int i = 0; i < params.length; i++) {
			for (int j = 0; j < params[i].length; j++) {
				alphas[wp++] = -paramAlphas[i][j];
				alphas[wp++] = paramAlphas[i][j];
			}
			wp += 2;
		}
		alphas[wp++] = -leakAlpha;
		alphas[wp++] = leakAlpha;
		return alphas;
	}

	public static SensitivityEquation
		scalarMultiply(SensitivityEquation eq, double c) {
		if (eq == null)
			return null;
		double prob = eq.prob * c;
		Hashtable alphasMap = new Hashtable();
		Hashtable excludeMap = new Hashtable();
		Object[] array = eq.alphasMap.keySet().toArray();
		for (int i = 0; i < array.length; i++) {
			FiniteVariable varX = (FiniteVariable)array[i];
			double[] alphas = (double[])eq.alphasMap.
				get(varX);
			double[] newAlphas = new double[alphas.length];
			for (int j = 0; j < alphas.length; j++)
				newAlphas[j] = alphas[j] * c;
			Object obj = eq.excludeMap.get(varX);
			alphasMap.put(varX, newAlphas);
			excludeMap.put(varX, obj);
		}
		return new SensitivityEquation(eq.pde, prob, alphasMap,
			excludeMap);
	}

	public static SensitivityEquation diff(SensitivityEquation eq1,
		SensitivityEquation eq2) {
		if (eq1 == null || eq2 == null)
			return null;
		double prob = eq1.prob - eq2.prob;
		Hashtable alphasMap = new Hashtable();
		Hashtable excludeMap = new Hashtable();
		Object[] array = eq1.alphasMap.keySet().toArray();
		for (int i = 0; i < array.length; i++) {
			FiniteVariable varX = (FiniteVariable)array[i];
			double[] alpha1s = (double[])eq1.alphasMap.
				get(varX);
			double[] alpha2s = (double[])eq2.alphasMap.
				get(varX);
			Object obj1 = eq1.excludeMap.get(varX);
			Object obj2 = eq2.excludeMap.get(varX);
			if (!matchExclude(obj1, obj2))
				continue;
			double[] alphas = new double[alpha1s.length];
			for (int j = 0; j < alphas.length; j++)
				alphas[j] = alpha1s[j] - alpha2s[j];
			alphasMap.put(varX, alphas);
			excludeMap.put(varX, obj1);
		}
		return new SensitivityEquation(eq1.pde, prob,
			alphasMap, excludeMap);
	}

	public static SensitivityEquation complement(SensitivityEquation
		evidenceEq, SensitivityEquation[] jointEqs, int index) {
		if (jointEqs == null)
			return null;
		SensitivityEquation eq = evidenceEq;
		for (int i = 0; i < jointEqs.length; i++) {
			if (i == index)
				continue;
			SensitivityEquation newEq = diff(eq, jointEqs[i]);
			if (newEq == null)
				return null;
			eq = newEq;
		}
		return eq;
	}

	public String toString() {
		String s = "Prob = " + String.valueOf(prob) + "\n";
		Object[] array = alphasMap.keySet().toArray();
		for (int i = 0; i < array.length; i++) {
			FiniteVariable varX = (FiniteVariable)array[i];
			double[] alphas = (double[])alphasMap.get(varX);
			s += "Alphas of " + varX.toString() + ": [";
			for (int j = 0; j < alphas.length; j++) {
				s += String.valueOf(alphas[j]);
				if (j != alphas.length - 1)
					s += "; ";
				else
					s += "]\n";
			}
		}
		return s;
	}
}
