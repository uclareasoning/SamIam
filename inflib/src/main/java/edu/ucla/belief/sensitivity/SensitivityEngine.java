package edu.ucla.belief.sensitivity;

import edu.ucla.belief.*;
import edu.ucla.util.*;

import java.io.*;
import java.util.*;

public class SensitivityEngine
{
	protected BeliefNetwork myBeliefNetwork;
	protected InferenceEngine myInferenceEngine;
	protected PartialDerivativeEngine myPartialDerivativeEngine;
	protected PrintWriter myPrintWriter;
	protected SensitivityEquation evidenceEq;
	protected Map jointEqMap;

	public static final java.io.PrintStream STREAM_VERBOSE = System.out;

	public static final String DIFFERENCE = " - ";
	public static final String RATIO = " / ";

	public static final Object[] ARITHMETIC_OPERATORS = { DIFFERENCE, RATIO };

	public static final String OPERATOR_EQUALS = "=";
	public static final String OPERATOR_GTE = ">=";
	public static final String OPERATOR_LTE = "<=";

	public static final Object[] COMPARISON_OPERATORS = { OPERATOR_GTE, OPERATOR_EQUALS, OPERATOR_LTE };

	public SensitivityEngine(BeliefNetwork bn, InferenceEngine ie,
		PartialDerivativeEngine pde)
	{
		this.myBeliefNetwork = bn;
		this.myInferenceEngine = ie;
		this.myPartialDerivativeEngine = pde;
		this.myPrintWriter = null;
		resetEquations();
	}

	public SensitivityEngine(BeliefNetwork bn, InferenceEngine ie,
		PartialDerivativeEngine pde, PrintWriter pw)
	{
		this.myBeliefNetwork = bn;
		this.myInferenceEngine = ie;
		this.myPartialDerivativeEngine = pde;
		this.myPrintWriter = pw;
		resetEquations();
	}

	/**
		@author Keith Cascio
		@since 092903
	*/
	public void setInferenceEngine( InferenceEngine ie )
	{
		myInferenceEngine = ie;
		myPartialDerivativeEngine = (PartialDerivativeEngine) ie;
	}

	public void setPrintWriter(PrintWriter pw) {
		this.myPrintWriter = pw;
	}

	public void printToWriter( String s ) {
		if( myPrintWriter != null ) myPrintWriter.println(s);
		else STREAM_VERBOSE.println(s);
	}

	public void resetEquations() {
		evidenceEq = null;
		jointEqMap = new Hashtable();
	}

	public SensitivityTable singleParamSolve(FiniteVariable varX,
		double probD, double[] alphaDs, double probN, double[]
		alphaNs, Object comparisonOp, double epsilon) {

		double lhs = probN - epsilon * probD;
		Interval[] intervals = new Interval[alphaNs.length];
		for (int i = 0; i < intervals.length; i++) {
			double rhs = alphaNs[i] - epsilon * alphaDs[i];
			if (rhs == 0.0)
				intervals[i] = new Interval(1.0, 0.0);
			else if (comparisonOp == OPERATOR_EQUALS)
				intervals[i] = new Interval(lhs / -rhs);
			else if ((comparisonOp == OPERATOR_GTE && rhs <
				0.0) || (comparisonOp == OPERATOR_LTE &&
				rhs > 0.0))
				intervals[i] = new Interval(Double.
					NEGATIVE_INFINITY, lhs / -rhs);
			else if ((comparisonOp == OPERATOR_GTE && rhs >
				0.0) || (comparisonOp == OPERATOR_LTE &&
				rhs < 0.0))
				intervals[i] = new Interval(lhs / -rhs,
					Double.POSITIVE_INFINITY);
		}
		return new SensitivityTable(varX.getCPTShell( varX.getDSLNodeType() ), intervals,
			ExcludePolicy.getExcludeArray(varX));
	}

	public SingleCPTSuggestion singleCPTSolve(FiniteVariable varX,
		double probD, double[] alphaDs, double probN, double[]
		alphaNs, Object comparisonOp, double epsilon,
		SensitivityTable sTable) {
		double prec = 1e-9, negl = 1e-9, maxLoops = 1e7;

		CPTShell shell = varX.getCPTShell( varX.getDSLNodeType() );
		double[] thetas = shell.getCPT().dataclone();
		int varXSize = varX.size();
		boolean incr = (probN / probD) < epsilon;
		double[] consts = new double[thetas.length];
		for (int i = 0; i < consts.length; i++) {
			double deriv = probD * alphaNs[i] - probN *
				alphaDs[i];
			if (Math.abs(deriv) < negl)
				consts[i] = 0.0;
			else
				consts[i] = incr ^ (deriv < 0.0) ? 1.0 :
					-1.0;
		}
		double[] deltas = new double[thetas.length];
		double minDist = 0.0, minValue = probN / probD, prevValue
			= -1.0;
		double maxDist = getMaxDist(sTable);
		double maxValue = singleCPTValue(deltas, thetas, consts,
			maxDist, varXSize, probD, alphaDs, probN,
			alphaNs);
		if (Math.abs(maxValue - epsilon) < prec)
		{
			return new SingleCPTSuggestion(shell,
				computeIntervals(varX, deltas),
				ExcludePolicy.getExcludeArray(varX),
				maxDist);
		}
		if (maxDist == Double.POSITIVE_INFINITY) {
			if (Double.isNaN(maxValue))
				return null;
			if (incr)
			{
				if (maxValue < epsilon)
					return null;
			}
			else
			{
				if (maxValue > epsilon)
					return null;
			}
			maxDist = 10;
			maxValue = singleCPTValue(deltas, thetas, consts,
				maxDist, varXSize, probD, alphaDs, probN,
				alphaNs);
			if (Math.abs(maxValue - epsilon) < prec)
				return new SingleCPTSuggestion(shell,
					computeIntervals(varX, deltas),
					ExcludePolicy.getExcludeArray(varX),
					maxDist);
		}
		for (int i = 0; i < maxLoops; i++) {
			double dist = minDist + (epsilon-minValue) *
				(maxDist-minDist) / (maxValue-minValue);
			if (dist < 0.0)
				dist = 0.0;
			double value = singleCPTValue(deltas, thetas,
				consts, dist, varXSize, probD, alphaDs,
				probN, alphaNs);
			if (Math.abs(value - epsilon) < prec)
			{
				return new SingleCPTSuggestion(shell,
					computeIntervals(varX, deltas),
					ExcludePolicy.getExcludeArray(varX),
					dist);
			}
			if (incr ^ (value > epsilon)) {
				maxDist = dist;
				maxValue = value;
			}
			else {
				minDist = dist;
				minValue = value;
			}
			/*if (Math.abs(value - prevValue) <= negl){
				//System.out.println("N: Loops = " + i);
				return null;
			}*/
			prevValue = value;
		}
		System.err.println("Variable " + varX + " run out of loops.");
		return null;
	}

	private static double singleCPTValue(double[] deltas, double[]
		thetas, double[] consts, double dist, int varXSize, double
		probD, double[] alphaDs, double probN, double[] alphaNs) {

		Arrays.fill(deltas, 0.0);
		double newProbD = probD, newProbN = probN;

		if (varXSize == 2) {
			for (int i = 0; i < deltas.length; i += 2) {
				double newTheta = Prob.applyLogOddsChange(thetas[i],
					dist * consts[i]);
				deltas[i] = Prob.applyLogOddsChange(thetas[i],
					dist * consts[i]) - thetas[i];
				//System.out.println(newTheta + " - " + thetas[i] + " = " + (newTheta - thetas[i]));
				//System.out.println("1.0 - " + thetas[i] + " = " + (1.0 - thetas[i]));
				//System.out.println("1.2 - " + thetas[i] + " = " + (1.2 - thetas[i]));
				//System.out.println("1.5 - " + thetas[i] + " = " + (1.5 - thetas[i]));
				newProbD += deltas[i] * alphaDs[i];
				newProbN += deltas[i] * alphaNs[i];
			}
			return newProbN / newProbD;
		}

		for (int i = 0; i < deltas.length; i += varXSize) {
			int k = -1;
			double delta = 0.0, dProbD = 0.0, dProbN = 0.0,
				dProb = 0.0;
			for (int j = i; j < i+varXSize; j++) {
				double d = Prob.applyLogOddsChange(
					thetas[j], dist * consts[j]) -
					thetas[j];
				double dPD = d * alphaDs[j];
				double dPN = d * alphaNs[j];
				double dP = (newProbN + dPN) / (newProbD +
					dPD) - newProbN / newProbD;
				if (Math.abs(dP) > Math.abs(dProb)) {
					k = j;
					delta = d;
					dProbD = dPD;
					dProbN = dPN;
					dProb = dP;
				}
			}
			if (k != -1) {
				deltas[k] = delta;
				newProbD += dProbD;
				newProbN += dProbN;
			}
		}
		return newProbN / newProbD;
	}

	private static double getMaxDist(SensitivityTable table) {
		double maxDist = Double.POSITIVE_INFINITY;
		if (table == null)
			return maxDist;
		for (int i = 0; i < table.size(); i++) {
			double theta = table.getProb(i);
			ProbabilityInterval probInterval =
				table.probabilityInterval(i);
			if (probInterval.isEmpty())
				continue;
			double dist = Math.min(
				Math.abs(Prob.logOddsDiff(theta,
				probInterval.getLowerBound())),
				Math.abs(Prob.logOddsDiff(theta,
				probInterval.getUpperBound())));
			if (dist < maxDist)
				maxDist = dist;
		}
		return maxDist;
	}

	private static Interval[] computeIntervals(FiniteVariable
		varX, double[] deltas) {
		int varXSize = varX.size();
		double[] thetas = varX.getCPTShell( varX.getDSLNodeType() ).
			getCPT().dataclone();
		boolean[] excludeArray =
			ExcludePolicy.getExcludeArray(varX);
		Interval[] intervals = new Interval[deltas.length];
		for (int i = 0; i < intervals.length; i += varXSize) {
			int index = -1;
			double delta = 0.0;
			double[] subThetas = new double[varXSize];
			for (int j = i; j < i + varXSize; j++) {
				subThetas[j-i] = thetas[i];
				if (deltas[j] != 0.0) {
					index = j-i;
					delta = deltas[j];
				}
			}
			double[] changes = Prob.proportionalChanges(index,
				delta, subThetas, excludeArray);
			for (int j = i; j < i + varXSize; j++)
				intervals[j] = new Interval(changes[j-i]);
		}
		return intervals;
	}

	public SensitivityEquation getEvidenceEq(List vars) {
		if (evidenceEq != null)
			return evidenceEq;
		evidenceEq = new SensitivityEquation(vars,
			myInferenceEngine, myPartialDerivativeEngine);
		return evidenceEq;
	}

	public SensitivityEquation getJointYEq(List vars, FiniteVariable
		varY, Object valueY) {
		int index = varY.index(valueY);
		SensitivityEquation[] jointYEqs = (SensitivityEquation[])
			jointEqMap.get(varY);
		SensitivityEquation jointYEq;
		if (jointYEqs == null) {
			jointYEqs = new SensitivityEquation[varY.size()];
			jointEqMap.put(varY, jointYEqs);
		}
		else {
			if (jointYEqs[index] != null)
				return jointYEqs[index];
			jointYEq = SensitivityEquation.
				complement(evidenceEq, jointYEqs, index);
			if (jointYEq != null) {
				jointYEqs[index] = jointYEq;
				return jointYEq;
			}
		}

		EvidenceController ec = myBeliefNetwork.getEvidenceController();
		try{
			ec.observeNotifyOnlyPriorityListeners(varY, valueY);
			jointYEq = new SensitivityEquation(vars,
				myInferenceEngine, myPartialDerivativeEngine);
			ec.unobserveNotifyOnlyPriorityListeners(varY);

		}catch( StateNotFoundException snfe ){
			System.err.println( "SensitivityEngine.getJointYEq() caught " + snfe );
			if( Definitions.DEBUG )
			{
				System.err.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				snfe.printStackTrace();
			}
			return null;
		}
		jointYEqs[index] = jointYEq;
		return jointYEq;
	}

	private static double[] diff(double[] d1, double[] d2) {
		double[] d = new double[d1.length];
		for (int i = 0; i < d1.length; i++)
			d[i] = d1[i] - d2[i];
		return d;
	}

	public SensitivityReport getResults( FiniteVariable varY,
				Object valueY,
				FiniteVariable varZ,
				Object valueZ,
				Object arithmeticOp,
				Object comparisonOp,
				double epsilon,
				boolean flagSingleParameter,
				boolean flagSingleCPT )
	{
		return getResults( new SensitivityConstraint( varY, valueY,
			varZ, valueZ, arithmeticOp, comparisonOp,
			epsilon, flagSingleParameter, flagSingleCPT ) );
	}

	public SensitivityReport getResults( SensitivityConstraint constraint ){
/*
		boolean version2 = true;
		if (version2) {
			SensitivityEngine2 se2 = new SensitivityEngine2(
				myBeliefNetwork, myInferenceEngine,
				myPartialDerivativeEngine, myPrintWriter);
			return se2.getResults(constraint);
		}
*/
		FiniteVariable varY = constraint.varY;
		Object valueY = constraint.valueY;
		FiniteVariable varZ = constraint.varZ;
		Object valueZ = constraint.valueZ;
		Object arithmeticOp = constraint.arithmeticOp;
		Object comparisonOp = constraint.comparisonOp;
		double epsilon = constraint.epsilon;

		EvidenceController ec = myBeliefNetwork.getEvidenceController();
		//Turn off evidence change events
		//EC.setNotifyEnabled( false );

		Vector vars = new Vector();
		Object[] array = myBeliefNetwork.toArray();
		for (int i = 0; i < array.length; i++)
		{
			if (ExcludePolicy.exclude((FiniteVariable)array[i]))
				continue;
			List familyVars = ((FiniteVariable)array[i]).getCPTShell().
				variables();
			boolean evidenceAllSet = true;
			for (int j = 0; j < familyVars.size(); j++)
				if (ec.getValue((FiniteVariable)familyVars.get(j))
					== null)
				{
					evidenceAllSet = false;
					break;
				}
			if (!evidenceAllSet)
				vars.add(array[i]);
		}

		if(ec.getValue(varY) != null || ec.getValue(varZ) != null)
		{
			printToWriter("Sensitivity analysis called on evidence variable." );
			return null;
		}

		if (constraint.satisfied(myInferenceEngine)) {
			printToWriter("Sensitivity analysis constraint already satisifed.");
			return null;
		}

		printToWriter("Sensitivity analysis started.");
		Hashtable singleParamMap = new Hashtable();
		Hashtable singleCPTMap = new Hashtable();
		SensitivityEquation eqD, eqN;

		long start_ms = System.currentTimeMillis();
		long start_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();

		if (arithmeticOp == DIFFERENCE) {
			eqD = getEvidenceEq(vars);
			eqN = SensitivityEquation.diff(getJointYEq(vars,
				varY, valueY), getJointYEq(vars, varZ,
				valueZ));
		}
		else if (arithmeticOp == RATIO) {
			eqD = getJointYEq(vars, varZ, valueZ);
			eqN = getJointYEq(vars, varY, valueY);
		}
		else {
			eqD = getEvidenceEq(vars);
			eqN = getJointYEq(vars, varY, valueY);
		}

		double probD = eqD.getProb();
		double probN = eqN.getProb();
		for (int i = 0; i < vars.size(); i++) {
			FiniteVariable varX = (FiniteVariable)vars.get(i);
			double[] alphaDs = eqD.getAlphas(varX);
			double[] alphaNs = eqN.getAlphas(varX);

			SensitivityTable table1 = null;
			if( constraint.flagSingleParameter ){
				table1 = singleParamSolve(varX,
					probD, alphaDs, probN, alphaNs,
					comparisonOp, epsilon);
				singleParamMap.put(varX, table1);
			}
			if( constraint.flagSingleCPT ){
				SensitivityTable table2 = singleCPTSolve(varX,
					probD, alphaDs, probN, alphaNs,
					comparisonOp, epsilon, table1);
				if (table2 != null) {
					//System.out.println(table2);
					singleCPTMap.put(varX, table2);
				}
			}
		}

		long end_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
		long end_ms = System.currentTimeMillis();

		long threadCPUMS = end_cpu_ms - start_cpu_ms;
		long elapsedMillis = end_ms - start_ms;

		long answerMillis = (long)0;
		String caption = null;
		if( JVMProfiler.profilerRunning() )
		{
			answerMillis = threadCPUMS;
			caption = "(thread profile)";
		}
		else
		{
			answerMillis = elapsedMillis;
			caption = "(system elapsed, thread profile not available)";
		}

		//Turn evidence change events back on again
		//EC.setNotifyEnabled( true );

		printToWriter("Sensitivity analysis finished in "+answerMillis+" ms "+caption+"." );
		return new SensitivityReport(singleParamMap,
			singleCPTMap);
	}

/* Hei 051602: Disabled; don't delete
	public Map getResults(FiniteVariable var, Object value1,
		Object value2, Object opArithmetic, Object opComparison, double c)
	{
		//System.out.println( "getResults( same variable 2 events )" );//debug

		FiniteVariable[] vars = variables();
		Map results = getResults(var, value1, var,
			value2, opArithmetic, opComparison, c);

		double prob1 = conditional(var, value1);
		double prob2 = conditional(var, value2);
		for (int i = 0; i < var.size(); i++) {
			Object instance = var.instance(i);
			if (instance == value1 || instance == value2)
				continue;
			double prob = conditional(var, instance);
			Map results1, results2;
			if (prob > prob1 && prob > prob2) {
				results1 = getResults(var, instance, var,
					value1, RATIO, opComparison, 1.0);
				results2 = getResults(var, instance, var,
					value2, RATIO, opComparison, 1.0);
			}
			else if (prob < prob1 && prob < prob2) {
				results1 = getResults(var, value1,
					var, instance, RATIO, opComparison, 1.0);
				results2 = getResults(var, value2,
					var, instance, RATIO, opComparison, 1.0);
			}
			else
				continue;

			for (int j = 0; j < vars.length; j++) {
				SensitivityTable table =
					(SensitivityTable)results.
					get(vars[j]);
				if (table == null)
					continue;
				table.intersect((SensitivityTable)
					results1.get(vars[j]));
				table.intersect((SensitivityTable)
					results2.get(vars[j]));
			}
		}
		return results;
	}
*/
/**/
/* Hei 051602: Disabled; don't delete
	public Map getResults(FiniteVariable var, Object value1,
		Object value2, String op, double c) {
		FiniteVariable[] vars = variables();
		Map results = getResults(var, value1, var,
			value2, op, c);

				constraints[i] = new SensitivityConstrint(
					varX, varX.instance(0), null,
					null, null, OPERATOR_LTE, 0.5);
		double prob1 = conditional(var, value1);
		double prob2 = conditional(var, value2);
		for (int i = 0; i < var.size(); i++) {
			Object instance = var.instance(i);
			if (instance == value1 || instance == value2)
				continue;
			double prob = conditional(var, instance);
			Map results1, results2;
			if (prob > prob1 && prob > prob2) {
				results1 = getResults(var, instance, var,
					value1, RATIO, 1.0);
				results2 = getResults(var, instance, var,
					value2, RATIO, 1.0);
			}
			else if (prob < prob1 && prob < prob2) {
				results1 = getResults(var, value1,
					var, instance, RATIO, 1.0);
				results2 = getResults(var, value2,
					var, instance, RATIO, 1.0);
			}
			else
				continue;

			for (int j = 0; j < vars.length; j++) {
				SensitivityTable table =
					(SensitivityTable)results.
					get(vars[j]);
				if (table == null)
					continue;
				table.intersect((SensitivityTable)
					results1.get(vars[j]));
				table.intersect((SensitivityTable)
					results2.get(vars[j]));
			}
		}
		return results;
	}
*/
}
