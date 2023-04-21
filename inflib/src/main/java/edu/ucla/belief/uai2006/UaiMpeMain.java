package edu.ucla.belief.uai2006;

import edu.ucla.belief.*;
//{superfluous} import edu.ucla.belief.rc2.tools.RC_Link_UAI2006;
import java.util.*;

/**
 * UAI-06 Evaluation : MAP Main Program
 * 
 * @author Arthur Choi
 */

public class UaiMpeMain extends UaiMain {
	/*
	  from UaiMain:
	public BeliefNetwork bn;
	public Map<FiniteVariable,Object> evidence;
	public Set<FiniteVariable> mapVars;
	*/

	public static void main(String[] args) {
		UaiMpeMain uai = new UaiMpeMain(args);
		UaiMapSolution sol = uai.computeQuery();
		uai.outputSolution(sol);
	}

	public UaiMpeMain(String[] args) {
		this(UaiMain.loadCommandLineOptions(args));
	}

	public UaiMpeMain(String filename) {
		super(filename);
	}

	/*****************************************/
	/* begin coding network code
	 */
	public static boolean isCodingNetwork(BeliefNetwork bn) {
		List vars = bn.topologicalOrder();
		int numx = 0;
		int numy1 = 0, numy2 = 0;
		int nums = 0;

		int numother = 0;

		for (Iterator it = vars.iterator(); it.hasNext(); ) {
			FiniteVariable var = (FiniteVariable)it.next();
			if ( bn.inDegree(var) == 0 ) { // is root node
				if ( isCodingInfoNode(var) ) 
					numx++;
				else
					return false;
					//numother++;
			} else if ( bn.outDegree(var) == 0 ) { // is leaf node
				if ( bn.inDegree(var) == 1 ) { // soft evidence on unique parent
					FiniteVariable parent = (FiniteVariable)bn.inComing(var).iterator().next();
					if ( bn.inDegree(parent) == 0 ) // soft evidence on root
						numy1++;
					else // soft evidence on something else ... (AC)
						numy2++;
				} else 
					return false;
					// numother++;
			} else { // internal node
				Set parents = bn.inComing(var);
				boolean checknode = true;
				for (Iterator pit = parents.iterator(); pit.hasNext(); ) {
					FiniteVariable parent = (FiniteVariable)pit.next();
					// all parents must be roots
					if (bn.inDegree(parent) != 0) {
						checknode = false;
						break;
					}
				}
				if ( checknode && isCodingCheckNode(var) )
					nums++;
				else 
					return false;
					//numother++;
			}
		}
		return true;
	}

	public static boolean isCodingInfoNode(FiniteVariable var) {
		double[] t = var.getCPTShell().getCPT().dataclone();
		for (int i = 0; i < t.length; i++)
			if ( t[i] != 0.5 ) return false;
		return true;
	}

	public static double[] xorCpt = 
	{ 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0,
	  0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0,
	  0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0,
	  1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0 };

	/* this is not thorough */
	public static boolean isCodingCheckNode(FiniteVariable var) {
		double[] t = var.getCPTShell().getCPT().dataclone();
		if ( t.length != xorCpt.length ) return false;
		for (int i = 0; i < t.length; i++)
			if ( t[i] != xorCpt[i] )
				return false;
		return true;
	}

	public boolean isCoding(BeliefNetwork bn) {
		long t1 = System.currentTimeMillis();
		boolean iscoding = isCodingNetwork(bn);;
		t1 = System.currentTimeMillis()-t1;
		verbosePrintln("isCoding: " + t1 + "ms");
		return iscoding;
	}

	/*****************************************/
	/* begin xor network code
	 */
	public static boolean isDeterministicCpt(double[] cpt) {
		for (int i = 0; i < cpt.length; i++)
			if ( cpt[i] != 0.0 && cpt[i] != 1.0 ) return false;
		return true;
	}

	/* assumes determinstic cpt, binary variable, at least 1 parent */
	public static boolean isXorCpt(double[] cpt, int size) {
		if ( size == 1 ) return true;
		for ( int i = 0; i < size/2; i++ )
			if ( cpt[i] == cpt[size/2+i] ) return false;
		return isXorCpt(cpt,size/2);
	}

	public static boolean hasBinaryParents(BeliefNetwork bn,
										   FiniteVariable var) {
		boolean binParents = true;
		for ( Iterator it = bn.inComing(var).iterator(); it.hasNext(); ) {
			FiniteVariable parent = (FiniteVariable)it.next();
			if ( parent.size() != 2 ) {
				binParents = false;
				break;
			}
		}
		return binParents;
	}

	public static boolean isXorNetwork(BeliefNetwork bn) {
		List vars = bn.topologicalOrder();
		int numXor = 0;
		int numOther = 0;

		FiniteVariable var;
		for ( int i = 0; i < vars.size(); i++ ) {
			var = (FiniteVariable)vars.get(i);
			if ( bn.inDegree(var) <= 1 ) continue;
			if ( var.size() != 2 ) continue;
			if ( !hasBinaryParents(bn,var) ) continue;
			double[] cpt = var.getCPTShell().getCPT().dataclone();
			if ( !isDeterministicCpt(cpt) ) continue;
			if ( isXorCpt(cpt,cpt.length) ) numXor++;
			else numOther++;
		}
		return numXor > numOther;
	}

	public boolean isXor(BeliefNetwork bn) {
		long t1 = System.currentTimeMillis();
		boolean iscoding = isXorNetwork(bn);;
		t1 = System.currentTimeMillis()-t1;
		verbosePrintln("isXor: " + t1 + "ms");
		return iscoding;
	}

	/*****************************************/
	/* begin determinism network code
	 */
	public boolean isHighlyDeterministic(BeliefNetwork bn) {
		long t1 = System.currentTimeMillis();

		int count01 = 0;
		int countother = 0;
		List vars = bn.topologicalOrder();
		FiniteVariable var;
		double[] cpt;
		for ( int i = 0; i < vars.size(); i++ ) {
			var = (FiniteVariable)vars.get(i);
			cpt = var.getCPTShell().getCPT().dataclone();
			for ( int j = 0; j < cpt.length; j++ )
				if ( cpt[j] == 0.0 || cpt[j] == 1.0 ) count01++;
				else countother++;
		}

		t1 = System.currentTimeMillis()-t1;
		verbosePrintln("isHighDeterminism: " + t1 + "ms");

		return count01 > countother;
	}

	public UaiMapSolution computeQuery() {
		UaiMpeEngine engine = null;
		UaiMapSolution solution = null;
		UaiPreprocessResult pr = null;
		UaiUtilsIf utils = null;

		try {
			verbosePrint("Engine: ");
			if ( UaiMain.testMode ) {
				verboseEnginePrintln("MPE-Force-Test");
				engine = new TestMpeEngine();
			} else {
				utils = (UaiUtilsIf)Class.forName
					("mark.bridge.samiam.UaiUtils").getEnumConstants()[0];
				pr = utils.preprocessNet(new Random(), bn, evidence, true);

				if ( UaiMain.aceMode ) {
					verboseEnginePrintln("MPE-Force-Ace ");
					engine = (UaiMpeEngine)Class.forName  
						("mark.bridge.samiam.UaiEngineAce").getEnumConstants()[0];
				} else if ( UaiMain.rcMode ) {
					verboseEnginePrintln("MPE-Force-RC  ");
					engine = (UaiMpeEngine)Class.forName  
						("mark.bridge.samiam.UaiEngineRc").getEnumConstants()[0];
				} else if ( UaiMain.bnbMode ) {
					verboseEnginePrintln("MPE-Force-B&B ");
					engine = (UaiMpeEngine)Class.forName  
						("mark.bridge.samiam.UaiMpeEngineBnb").getEnumConstants()[0];
				} else if ( pr.logMaxClusterSize < 25.00001 ) {
					verboseEnginePrintln("MPE-Easy-RC   ");
					engine = (UaiMpeEngine)Class.forName  
						("mark.bridge.samiam.UaiEngineRc").getEnumConstants()[0];
// 				} else if ( isCoding(bn) ) {
// 					verboseEnginePrintln("MPE-Coding-B&B");
// 					engine = (UaiMpeEngine)Class.forName  
// 						("mark.bridge.samiam.UaiMpeEngineBnb").getEnumConstants()[0];
// 				} else if ( isHighlyDeterministic(bn) ) {
// 					verboseEnginePrintln("MPE-Hi-Det-Ace");
// 					engine = (UaiMpeEngine)Class.forName  
// 						("mark.bridge.samiam.UaiEngineAce").getEnumConstants()[0];
				} else if ( isHighlyDeterministic(bn) && 
							!isXor(bn) ) {
					verboseEnginePrintln("MPE-Hi-Det-Ace");
					engine = (UaiMpeEngine)Class.forName  
						("mark.bridge.samiam.UaiEngineAce").getEnumConstants()[0];
				} else {
					verboseEnginePrintln("MPE-Lo-Det-B&B");
					engine = (UaiMpeEngine)Class.forName  
						("mark.bridge.samiam.UaiMpeEngineBnb").getEnumConstants()[0];
				}
			}
			//solution = new UaiMapSolution();
			solution = engine.computeMpe(new Random(),bn,evidence,pr);
			printInfo(pr,solution);
		} catch ( Exception e ) {
			System.err.println(e);
			e.printStackTrace();
			return null;
		}

		return solution;
	}

}
