package edu.ucla.belief.uai2006;

import edu.ucla.belief.*;
import edu.ucla.belief.rc2.tools.RC_Link_UAI2006;
import java.util.*;

/**
 * UAI-06 Evaluation : MAP Main Program
 * 
 * @author Arthur Choi
 */

public class UaiPeMain extends UaiMain {
	/*
	  from UaiMain:
	public BeliefNetwork bn;
	public Map<FiniteVariable,Object> evidence;
	public Set<FiniteVariable> mapVars;
	*/

	public static void main(String[] args) {
		UaiPeMain uai = new UaiPeMain(args);
		UaiMapSolution sol = uai.computeQuery();
		uai.outputSolution(sol);
	}

	public UaiPeMain(String[] args) {
		this(UaiMain.loadCommandLineOptions(args));
	}

	public UaiPeMain(String filename) {
		super(filename);
	}

	HashMap rc_evidence;
	public boolean isGenetic() {
		long t1 = System.currentTimeMillis();
		this.rc_evidence = new HashMap<FiniteVariable, Object>(evidence);
		boolean isgenetic = RC_Link_UAI2006.isGeneticNetwork(bn,this.rc_evidence);
		t1 = System.currentTimeMillis()-t1;
		verbosePrintln("isGenetic: " + t1 + "ms");
		return isgenetic;
	}

	public UaiMapSolution computeQuery() {
		UaiPeEngine engine = null;
		UaiMapSolution solution = null;
		UaiPreprocessResult pr = null;
		UaiUtilsIf utils = null;

		try {
			verbosePrint("Engine: ");
			if ( UaiMain.testMode ) {
				verboseEnginePrintln("PrE-Force-Test");
				engine = new TestPeEngine();
			} else if ( UaiMain.aceMode ) {
				verboseEnginePrintln("PrE-Force-Ace ");
				utils = (edu.ucla.belief.uai2006.UaiUtilsIf)Class.forName
					("mark.bridge.samiam.UaiUtils").getEnumConstants()[0];
				pr = utils.preprocessNet(new Random(), bn, evidence, false);
				engine = (UaiPeEngine)Class.forName  
					("mark.bridge.samiam.UaiEngineAce").getEnumConstants()[0];
			} else if ( UaiMain.rcMode ) {
				verboseEnginePrintln("PrE-Force-RC  ");
				utils = (UaiUtilsIf)Class.forName
					("mark.bridge.samiam.UaiUtils").getEnumConstants()[0];
				pr = utils.preprocessNet(new Random(), bn, evidence, false);
				engine = (UaiPeEngine)Class.forName  
					("mark.bridge.samiam.UaiEngineRc").getEnumConstants()[0];
			} else if ( UaiMain.rclinkMode ) { 
				isGenetic();
				verboseEnginePrintln("PrE-Force-RC_L");
				engine = new RC_Link_UAI2006();
				evidence = this.rc_evidence;
			} else if ( UaiMain.rclink2Mode ) { 
				verboseEnginePrintln("PrE-Force-RC_2");
				engine = new RC_Link2PeEngine();
			} else if ( isGenetic() ) { 
				verboseEnginePrintln("PrE-Genet-RC_L");
				engine = new RC_Link_UAI2006();
				evidence = this.rc_evidence;
			} else {
				utils = (UaiUtilsIf)Class.forName
					("mark.bridge.samiam.UaiUtils").getEnumConstants()[0];
				pr = utils.preprocessNet(new Random(), bn, evidence, false);

				if ( pr.logMaxClusterSize < 25.00001 ) {
					verboseEnginePrintln("PrE-Easy-RC   ");
					engine = (UaiPeEngine)Class.forName  
						("mark.bridge.samiam.UaiEngineRc").getEnumConstants()[0];
				} else {
					verboseEnginePrintln("PrE-Hard-Ace  ");
					engine = (UaiPeEngine)Class.forName  
						("mark.bridge.samiam.UaiEngineAce").getEnumConstants()[0];
				}
			}
			try {
				//solution = new UaiMapSolution();
				solution = engine.computePe(new Random(),bn,evidence,pr);
			} catch ( UaiUnderflowException e ) {
				// We only expect mark.bridge.samiam.UaiEngineRc to
				// throw such an exception
				verboseEnginePrintln("PrE-Force-RC_2");
				engine = new RC_Link2PeEngine();
				solution = engine.computePe(new Random(),bn,evidence,pr);
			}
			printInfo(pr,solution);
		} catch ( Exception e ) {
			System.err.println(e);
			e.printStackTrace();
			// System.exit(1);
			return null;
		}

		return solution;
	}

}
