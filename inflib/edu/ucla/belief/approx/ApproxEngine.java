package edu.ucla.belief.approx;

import edu.ucla.belief.*;
import edu.ucla.util.*;
import java.io.*;
import java.util.*;

public class ApproxEngine implements QuantitativeDependencyHandler
{
	private BeliefNetwork myBeliefNetwork;
	private InferenceEngine myControlInferenceEngine;
	private ApproxReport latestReport;
	private PrintWriter pw;
	private double[] safeKL;
	public static double negl = 1e-7;

	public static final java.io.PrintStream STREAM_VERBOSE = System.out;

	public ApproxEngine(BeliefNetwork bn, InferenceEngine ie )
	{
		this.myBeliefNetwork = bn;
		this.myControlInferenceEngine = ie;
		latestReport = null;
		pw = null;
		safeKL = new double[500];
		for (int i = 0; i < safeKL.length; i++)
			safeKL[i] = safeKL((double)i / (safeKL.length*2));
	}

	public ApproxEngine( BeliefNetwork bn, InferenceEngine ie, PrintWriter pw )
	{
		this.myBeliefNetwork = bn;
		this.myControlInferenceEngine = ie;
		latestReport = null;
		this.pw = pw;
		safeKL = new double[500];
		for (int i = 0; i < safeKL.length; i++)
			safeKL[i] = safeKL((double)i / (safeKL.length*2));
	}

	public ApproxReport getLatestReport() {
		return latestReport;
	}

	/**
		interface QuantitativeDependencyHandler
		@since 061404
	*/
	public CPTShell getCPTShell( FiniteVariable var )
	{
		//System.out.println( "ApproxEngine.getCPTShell( "+var.getID()+" )" );
		//if( oncegetCPTShell ){
		//	new Throwable().printStackTrace();
		//	oncegetCPTShell = false;
		//}
		//ApproxResult result = getApproxResult( var );
		//if( result == null ) return var.getCPTShell( var.getDSLNodeType() );
		//else return result.getNewCPTShell();
		return var.getCPTShell( var.getDSLNodeType() );
	}
	//private boolean oncegetCPTShell = true;

	/** @since 061804 */
	public void reset()
	{
		latestReport = null;
	}

	/** @since 061704 */
	public InferenceEngine getControlInferenceEngine(){
		return this.myControlInferenceEngine;
	}

	public void setPrintWriter(PrintWriter pw) {
		this.pw = pw;
	}

	public void printToWriter(String s) {
		if( pw != null ) pw.println(s);
		else STREAM_VERBOSE.println(s);
	}

	public ApproxReport findFixedPoint(List parentList, List
		childList, List startValueList, int maxLoops) {
		FiniteVariable[] parents = new
			FiniteVariable[parentList.size()];
		FiniteVariable[] children = new
			FiniteVariable[childList.size()];
		FiniteVariable[][] familiesNet = new
			FiniteVariable[parents.length][];
		FiniteVariable[][] familiesRun = new
			FiniteVariable[parents.length][];
		CPTShell[] origCPTShells = new CPTShell[parents.length];
		double[][] origThetas = new double[parents.length][];
		for (int i = 0; i < parents.length; i++) {
			parents[i] = (FiniteVariable)parentList.get(i);
			children[i] = (FiniteVariable)childList.get(i);
			origCPTShells[i] = children[i].getCPTShell( children[i].getDSLNodeType() );
			Table cpt = origCPTShells[i].getCPT();
			familiesNet[i] = family(cpt);
			familiesRun[i] = familyPutVarFirst(cpt,
				parents[i]);
			origThetas[i] = cpt.permute(familiesRun[i]).
				dataclone();
		}
		double origProbE = myControlInferenceEngine.probability();
		Map conditionalMap = new Hashtable();
		Object[] vars = myBeliefNetwork.vertices().toArray();

		double sumENT = 0.0, sumCondEdgeKL = 0.0;
		double[][] condPoint = new double[parents.length][];
		CPTShell[] condCPTShells = new CPTShell[parents.length];
		double[][] familyConditional = new
			double[parents.length][];
		for (int i = 0;i < parents.length; i++) {
			condPoint[i] = myControlInferenceEngine.
				conditional(parents[i]).dataclone();
			sumENT += ent(condPoint[i]);
			double[] condTheta = approxTheta(origThetas[i],
				condPoint[i]);
			familyConditional[i] = myControlInferenceEngine.
				familyConditional(children[i]).
				permute(familiesRun[i]).dataclone();
			sumCondEdgeKL += edgeKL(origThetas[i], condTheta,
				familyConditional[i]);
		}
		for (int i = 0; i < parents.length; i++) {
			double[] condTheta = approxTheta(origThetas[i],
				condPoint[i]);
			condCPTShells[i] = new TableShell(new
				Table(familiesRun[i], condTheta).
				permute(familiesNet[i]));
			children[i].setCPTShell(condCPTShells[i]);
			myControlInferenceEngine.setCPT(children[i]);
		}

		double condProbE = myControlInferenceEngine.probability();
		double condLogProbERatio = Math.log(condProbE /
			origProbE);
		double condKLTotal = condLogProbERatio + sumCondEdgeKL;
		double condKLBound = condLogProbERatio + sumENT;
		ApproxCut condCut = new ApproxCut(parents, children,
			condPoint, condCPTShells, condProbE, condKLTotal,
			condKLBound);

		double[][] point = new double[parents.length][];
		for (int i = 0; i < parents.length; i++) {
			children[i].setCPTShell(origCPTShells[i]);
			myControlInferenceEngine.setCPT(children[i]);
			point[i] = new double[parents[i].size()];
			point[i][0] = ((Double)startValueList.get(i)).
				doubleValue();
			for (int j = 1; j < point[i].length; j++)
				point[i][j] = (1-point[i][0]) /
					(point[i].length-1);
		}
		Stack cutStack = new Stack();
		boolean conv = false;
		int loops = 0;
		double fixedProbE = -1.0;
		while (true) {
			double sumKLDiff = 0.0, sumEdgeKL = 0.0;
			CPTShell[] cptShells = new
				CPTShell[parents.length];
			for (int i = 0; i < parents.length; i++) {
				sumKLDiff += Prob.kl(condPoint[i],
					point[i]);
				double[] theta = approxTheta(
					origThetas[i], point[i]);
				sumEdgeKL += edgeKL(origThetas[i], theta,
					familyConditional[i]);
				cptShells[i] = new TableShell(new
					Table(familiesRun[i], theta).
					permute(familiesNet[i]));
				children[i].setCPTShell(
					cptShells[i]);
				myControlInferenceEngine.
					setCPT(children[i]);
			}
			double probE = myControlInferenceEngine.
				probability();
			double logProbERatio = Math.log(probE /
				origProbE);
			double klTotal = logProbERatio + sumEdgeKL;
			double klBound = logProbERatio + sumENT +
				sumKLDiff;
			cutStack.push(new ApproxCut(parents, children,
				point, cptShells, probE, klTotal,
				klBound));

			if (conv || ++loops == maxLoops) {
				for (int i = 0; i < vars.length; i++)
					conditionalMap.put(vars[i],
						myControlInferenceEngine.
						conditional(
						(FiniteVariable)vars[i]));
				break;
			}
			conv = true;
			double[][] nextPoint = new
				double[parents.length][];
			for (int i = 0; i < parents.length; i++) {
				nextPoint[i] = myControlInferenceEngine.
					conditional(parents[i]).
					dataclone();
				if (!neglError(point[i], nextPoint[i],
					negl))
					conv = false;
			}
			point = nextPoint;
		}

		for (int i = 0; i < parents.length; i++) {
			children[i].setCPTShell(origCPTShells[i]);
			myControlInferenceEngine.setCPT(children[i]);
		}
		latestReport = new ApproxReport(parentList, childList,
			conv, loops, origProbE, origCPTShells, condCut,
			cutStack, conditionalMap);
		return latestReport;
	}

	public static double[] approxTheta(double[] origTheta, double[]
		yConditional) {
		double[] newTheta = new double[origTheta.length];
		int uxSize = origTheta.length / yConditional.length;
		for (int i = 0; i < uxSize; i++) {
			double sum = 0.0;
			for (int j = 0; j < yConditional.length; j++)
				sum += origTheta[j * uxSize + i] *
					yConditional[j];
			for (int j = 0; j < yConditional.length; j++)
				newTheta[j * uxSize + i] = sum;
		}
		return newTheta;
	}

	private static boolean neglError(double[] d1, double[] d2, double
		negl) {
		for (int i = 0; i < d1.length; i++)
			if (Math.abs(d1[i] - d2[i]) >= negl)
				return false;
		return true;
	}

	public void cutEdges(ApproxReport report) {
		for (int i = 0; i < report.childList.size(); i++) {
			FiniteVariable child = (FiniteVariable)
				report.childList.get(i);
			child.setCPTShell(report.getCPTShell(child));
			myControlInferenceEngine.setCPT(child);
		}
	}

	public void restoreEdges(ApproxReport report) {
		for (int i = 0; i < report.childList.size(); i++) {
			FiniteVariable child = (FiniteVariable)
				report.childList.get(i);
			child.setCPTShell(report.origCPTShells[i]);
			myControlInferenceEngine.setCPT(child);
		}
	}

/*	public void report() {
		printToWriter("\nApproximation report");
		printToWriter("================================");
		EvidenceController ec = myBeliefNetwork.getEvidenceController();
		Hashtable evidence = new Hashtable(ec.evidence());

		double origProbE = myControlInferenceEngine.probability();
		Object[] allVars = myBeliefNetwork.vertices().toArray();
		Vector noEvidenceVarList = new Vector();
		for (int i = 0; i < allVars.length; i++)
			if (evidence.get(allVars[i]) == null)
				noEvidenceVarList.add(allVars[i]);
		FiniteVariable[] marginalVars = new
			FiniteVariable[noEvidenceVarList.size()];
		for (int i = 0; i < marginalVars.length; i++)
			marginalVars[i] = (FiniteVariable)
				noEvidenceVarList.get(i);

		double[][] origMarginals = new double[marginalVars.length][];
		double[][] newMarginals = new double[marginalVars.length][];
		//double[][] handledMarginals = new double[marginalVars.length][];

		//for (int i = 0; i < marginalVars.length; i++)
		//	handledMarginals[i] = this.conditional( (FiniteVariable)marginalVars[i]).dataclone();

		for (int i = 0; i < marginalVars.length; i++)
			origMarginals[i] = myControlInferenceEngine.conditional(
				(FiniteVariable)marginalVars[i]).
				dataclone();
		cutEdges();
		double newProbE = myControlInferenceEngine.probability();
		for (int i = 0; i < marginalVars.length; i++)
			newMarginals[i] = myControlInferenceEngine.conditional(
				(FiniteVariable)marginalVars[i]).
				dataclone();
		restoreEdges();
		printToWriter("Report of marginals");
		printToWriter("================================");
		int flipCount = 0;
		double klSum = 0.0, distSum = 0.0;
		for (int i = 0; i < marginalVars.length; i++) {
			printToWriter("Variable " +
				marginalVars[i].toString());
			printToWriter("Orig marginals in N = " + DblArrays.
				convertToString(origMarginals[i]));
			printToWriter("New marginals in N' = " + DblArrays.
				convertToString(newMarginals[i]));
			//printToWriter("Handled marginals in N' = " +
			//DblArrays.convertToString(handledMarginals[i]));
			boolean flip = flipsOutput(origMarginals[i],
				newMarginals[i]);
			flipCount += flip ? 1 : 0;
			printToWriter("Flip = " + new
				Boolean(flip).toString());
			double kl = Prob.kl(origMarginals[i],
				newMarginals[i]);
			klSum += kl;
			printToWriter("KL = " + new
				Double(kl).toString());
			double dist = Prob.distance(origMarginals[i],
				newMarginals[i]);
			distSum += dist;
			printToWriter("Dist = " + new
				Double(dist).toString());
			printToWriter("================================");
		}
		printToWriter("Summary of marginals");
		printToWriter("Total flips = " + new Integer(flipCount).
			toString());
		printToWriter("Avg KL = " + new Double(klSum /
			marginalVars.length).toString());
		printToWriter("Avg Dist = " + new Double(distSum /
			marginalVars.length).toString());
		printToWriter("================================\n");

		printToWriter("Report of fixed point");
		printToWriter("================================");
		double sumYKL = 0.0, sumYConditionalENT = 0.0,
			sumYConditionalKL = 0.0, sumFixedPointKL = 0.0;
		for (int i = 0; i < results.length; i++) {
			FiniteVariable varY = results[i].getParent();
			FiniteVariable varX = results[i].getChild();
			FiniteVariable[] family = results[i].getFamily();
			boolean conv = results[i].converges();
			int convLoops = results[i].getConvLoops();
			double[] fixedPoint = results[i].getFixedPoint();
			double[] origTheta = results[i].getOrigTheta();
			double[] newTheta = results[i].getNewTheta();
			double[] yConditional = myControlInferenceEngine.conditional(varY).
				dataclone();
			double[] yuxJoint = myControlInferenceEngine.familyJoint(varX).
				permute(family).dataclone();
			double[] yuxConditional = myControlInferenceEngine.familyConditional(
				varX).permute(family).dataclone();
			double[] yuConditional = sumOverLastVar(
				yuxConditional, varX.size());
			double[] uConditional = sumOverFirstVar(
				yuConditional, varY.size());

			printToWriter("Edge " + varY.toString() + " -> "
				+ varX);
			printToWriter("Fixed point found = " + conv);
			printToWriter("Loops until convergence = " +
				convLoops);
			printToWriter("Fixed point: Pr^(Y|e) = " +
				DblArrays.convertToString(fixedPoint));
			printToWriter("Marginals:    Pr(Y|e) = " +
				DblArrays.convertToString(yConditional));

			double yKL = Prob.kl(yConditional, fixedPoint);
			sumYKL += yKL;
			printToWriter("KL(Pr(Y|e),Pr^(Y|e)) = " + yKL);
			printToWriter("--------------------------------");

			double yConditionalENT = ent(yConditional);
			sumYConditionalENT += yConditionalENT;
			printToWriter("ENT(Y|e)               = " +
				yConditionalENT);

			double yConditionalKL = ykl(origTheta,
				approxCPT(origTheta, yConditional),
				yuxConditional);
			sumYConditionalKL += yConditionalKL;
			printToWriter("KL(Y->X) using Pr(Y|e) = " +
				yConditionalKL);
			printToWriter("--------------------------------");

			printToWriter("ENT(Y|e)+KL(Pr(Y|e),Pr^(Y|e)) = " +
				(yConditionalENT + yKL));
			double fixedPointKL = ykl(origTheta, newTheta,
				yuxConditional);
			sumFixedPointKL += fixedPointKL;
			printToWriter("KL(Y->X) using Pr^(Y|e)       = " +
				fixedPointKL);
			printToWriter("================================");
		}

		mySumYKL = sumYKL;
		mySumYConditionalENT = sumYConditionalENT;
		mySumYConditionalKL = sumYConditionalKL;
		mySumFixedPointKL = sumFixedPointKL;

		printToWriter("Summary of network");
		printToWriter("Orig Pr(e) = " + origProbE);
		printToWriter("New Pr'(e) = " + newProbE);
		double logProbERatio = Math.log(newProbE / origProbE);
		printToWriter("log(Pr'(e)/Pr(e)) = " + logProbERatio);
		printToWriter("--------------------------------");

		printToWriter("Approx using marginals Pr(Y|e)");
		printToWriter("log(Pr'(e)/Pr(e) + Sum {ENT(Y|e)} = " +
			(logProbERatio + sumYConditionalENT));

		double netYConditionalKL = logProbERatio +
			sumYConditionalKL;
		printToWriter("KL(N,N',e) using Pr(Y|e)          = " +
			netYConditionalKL);

		double netYConditionalSafeProb =
			safeProb(netYConditionalKL);
		printToWriter("Safe probs = " + netYConditionalSafeProb +
			", " + (1-netYConditionalSafeProb));
		printToWriter("--------------------------------");

		printToWriter("Approx using fixed point Pr^(Y|e)");
		printToWriter(
		"log(Pr'(e)/Pr(e)) + Sum {ENT(Y|e)+KL(Pr(Y|e),Pr^(Y|e))} = "
			+ (logProbERatio + sumYConditionalENT + sumYKL));

		double netFixedPointKL = logProbERatio + sumFixedPointKL;
		printToWriter(
		"KL(N,N',e) using Pr^(Y|e)                               = "
			+ netFixedPointKL);

		double netFixedPointSafeProb = safeProb(netFixedPointKL);
		printToWriter("Safe probs = " + netFixedPointSafeProb +
			", " + (1-netFixedPointSafeProb));
		printToWriter("================================");
	}
*/
	public static boolean flipsOutput(double[] conditional1, double[]
		conditional2) {
		return (conditional1[0] > 0.5 && conditional2[0] < 0.5) ||
			(conditional1[0] < 0.5 && conditional2[0] > 0.5);
	}
/*
	public double edgeMI(double[] parentJoint, double[] parentsJoint,
		double[] otherParentsJoint) {
		double sum = 0.0;
		for (int i = 0; i < parentsJoint.length; i++) {
			// int parentsIndex = i;
			int parentIndex = i / otherParentsJoint.length;
			int otherParentsIndex = i %
				otherParentsJoint.length;
			sum += parentsJoint[i] * Math.log(parentsJoint[i]
				/ parentJoint[parentIndex]
				/ otherParentsJoint[otherParentsIndex]);
		}
		return sum;
	}
*/
	private static double ent(double[] conditional) {
		double sum = 0.0;
		for (int i = 0; i < conditional.length; i++) {
			if (conditional[i] == 0.0)
				continue;
			sum -= conditional[i] * Math.log(conditional[i]);
		}
		return sum;
	}

	private static double edgeKL(double[] origTheta, double[]
		newTheta, double[] familyConditional) {
		double sum = 0.0;
		for (int i = 0; i < familyConditional.length; i++) {
			if (familyConditional[i] == 0.0)
				continue;
			sum -= familyConditional[i] * Math.log(newTheta[i]
				/ origTheta[i]);
		}
		return sum;
	}

	private static double maxRatio(double[] conditional1, double[]
		conditional2) {
		double max = 1.0;
		for (int i = 0; i < conditional1.length; i++) {
			double ratio = conditional1[i] / conditional2[i];
			if (ratio > max)
				max = ratio;
		}
		return max;
	}

	private static FiniteVariable[] family(Table table) {
		Vector varList = new Vector(table.variables());
		FiniteVariable[] family = new
			FiniteVariable[varList.size()];
		for (int i = 0; i < family.length; i++)
			family[i] = (FiniteVariable)varList.get(i);
		return family;
	}

	private static FiniteVariable[] familyPutVarFirst(Table table,
		FiniteVariable var) {
		Vector varList = new Vector(table.variables());
		FiniteVariable[] family = new
			FiniteVariable[varList.size()];
		varList.remove(var);
		varList.add(0, var);
		for (int i = 0; i < family.length; i++)
			family[i] = (FiniteVariable)varList.get(i);
		return family;
	}

	private static double[] sumOverLastVar(double[] values, int
		varSize) {
		double[] sums = new double[values.length / varSize];
		Arrays.fill(sums, 0.0);
		for (int i = 0; i < sums.length; i++)
			for (int j = 0; j < varSize; j++)
				sums[i] += values[i * varSize + j];
		return sums;
	}

	private static double[] sumOverFirstVar(double[] values, int
		varSize) {
		double[] sums = new double[values.length / varSize];
		Arrays.fill(sums, 0.0);
		for (int i = 0; i < sums.length; i++)
			for (int j = 0; j < varSize; j++)
				sums[i] += values[j * sums.length + i];
		return sums;
	}

	public double safeKL(double p) {
		if (p == 0.0)
			return Math.log(2);
		return Math.log(2) + (p * Math.log(p) + (1-p) *
			Math.log(1-p));
	}

	public double safeProb(double kl) {
		for (int i = 0; i < safeKL.length; i++) {
			if (kl > safeKL[i])
				return (double)(i-1) / (safeKL.length*2);
		}
		return (double)(safeKL.length-1) / (safeKL.length*2);
	}

/*
	public void randomExperiment(RandomCircuit circuit) {
		EvidenceController ec = circuit.getNet().
			getEvidenceController();
		Map evidence = new Hashtable();
		for (int i = 0; i < circuit.getN(); i++) {
			FiniteVariable varB = circuit.forID("B", i);
			Object instance = varB.instance(Prob.random(2));
			evidence.put(varB, instance);
		}
		for (int i = 0; i < circuit.getM(); i++) {
			FiniteVariable varA = circuit.forID("A", i);
			Object instance = varA.instance(Prob.random(2));
			evidence.put(varA, instance);
		}
		try {
			ec.setObservations(evidence);
		} catch (Exception e) {
			return;
		}

		Vector parentList = new Vector();
		Vector childList = new Vector();
		Vector startList = new Vector();
		for (int i = 0; i < circuit.getN(); i++) {
			FiniteVariable varY = circuit.forID("Y", i);
			double p = myControlInferenceEngine.
				conditional(varY).dataclone()[0];
			int[] fromY = circuit.getFromY(i);
			for (int j = 0; j < fromY.length; j++) {
                                FiniteVariable varX = circuit.
                                        forID("X", fromY[i][j]);
                                if (!childList.contains(varX)) {
                                        parentList.add(varY);
                                        childList.add(varX);
                                        startList.add(new Double(p));
                                        break;
                                }
                        }
                }
		findFixedPoint(parentList, childList, startList, 100);
	}
*/
}
