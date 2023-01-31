package edu.ucla.belief.uai2006;

import edu.ucla.belief.*;
import java.util.*;

/**
 * UAI-06 Evaluation : Main Program
 * 
 * todo: load evidence
 * todo: load MAP variables
 *
 * @author Arthur Choi
 */

public abstract class UaiMain {
	// can be used for debug output
	public static boolean normalize = false;
	public static boolean verbose = false;
	public static boolean verboseEngine = false;
	public static boolean testMode = false;
	public static boolean aceMode = false;
	public static boolean rcMode = false;
	public static boolean rclinkMode = false;
	public static boolean rclink2Mode = false;
	public static boolean bnbMode = false;

	public BeliefNetwork bn;
	public Map<FiniteVariable,Object> evidence;
	public Set<FiniteVariable> mapVars;

	public long startTime1; // total time
	public long startTime2; // total time - load time

	public static final String STR_ARG_HELP      = "-help";
	public static final String STR_ARG_NETWORK   = "-n";
	public static final String STR_ARG_NORMALIZE = "-z";
	// verbosity switches
	public static final String STR_ARG_VERBOSE   = "-v";
	public static final String STR_ARG_VERBOSE1  = "-v1";
	// engine switches
	public static final String STR_ARG_TEST      = "-t";
	public static final String STR_ARG_ACE       = "-a";
	public static final String STR_ARG_RC        = "-r";
	public static final String STR_ARG_RCLINK    = "-l";
	public static final String STR_ARG_RCLINK2   = "-l2";
	public static final String STR_ARG_BNB       = "-b";

	public static final String STR_XBIF = ".XBIF";

	/* args is command line arguments */
	public UaiMain(String[] args) {
		this(UaiMain.loadCommandLineOptions(args));
	}

	/* filename is parsed from command line arguments */
	public UaiMain(String filename) {
		// save start time
		this.startTime1 = System.currentTimeMillis();

		//verbosePrintln("=====begin loading:======");
		loadBeliefNetwork(filename);
		loadEvidence();
		loadMapVariables(); // sets MAP variables to empty map
		//verbosePrintln("=====end loading:========");

		// save start time, after loading network
		this.startTime2 = System.currentTimeMillis();
	}


	/****************************************
	 * Helper methods 
	 ****************************************/

	/* these functions are for debug purposes.  When this.verbose =
	true, debug messages are displayed.  Otherwise, only the query
	result is displayed. */
	public static void verbosePrint(String s) { 
		if ( verbose ) System.out.print(s);
	}

	public static void verbosePrintln(String s) {
		if ( verbose ) System.out.println(s);
	}

	public static void verboseEnginePrintln(String s) {
		if ( verbose ) System.out.println(s);
		else if ( verboseEngine ) System.out.print(s + "\t");
	}

	/****************************************
	 * Load methods 
	 ****************************************/

	/* command line arguments.  Expecting exactly network filename. */
	public static String loadCommandLineOptions(String[] args) {
		String filename = null;

		// read in command line options
		for (int i = 0; i < args.length; i++) {
			if ( args[i].equals(STR_ARG_HELP) ||
				 args[i].charAt(0) != '-' ) {
				System.out.println("UAI-06 Evaluation");
				System.out.println();
				System.out.println("Usage: uai2006 -n <network filename>");
				System.out.println("Optional Switches:");
				System.out.println("  -n          precedes network filename");
				System.out.println("  -z          normalize network CPTs");
				System.out.println("  -v          verbose mode");
				System.out.println("  -v1         verbose mode, engine only");
				System.out.println("  -t          use test engine (MPE&PE)");
				System.out.println("  -a          use Ace (MPE&PE)");
				System.out.println("  -r          use RC (MPE&PE)");
				System.out.println("  -l          use RC_Link (PE only)");
				System.out.println("  -l2         use RC_Link non-genetic (PE only)");
				System.out.println("  -b          use BnB (MPE only)");
				System.exit(0);
			} else {
				if ( ( i+1 == args.length ) &&
					 ( args[i].equals(STR_ARG_NETWORK) ) ) {
					System.err.println("Missing network filename.");
					System.err.println("Option -help for usage.");
					System.exit(1);
				}

				if      ( args[i].equals(STR_ARG_NETWORK) )
					filename = args[++i];
				else if ( args[i].equals(STR_ARG_NORMALIZE) )
					UaiMain.normalize = true;
				else if ( args[i].equals(STR_ARG_TEST) )
					UaiMain.testMode = true;
				else if ( args[i].equals(STR_ARG_VERBOSE) )
					UaiMain.verbose = true;
				else if ( args[i].equals(STR_ARG_VERBOSE1) )
					UaiMain.verboseEngine = true;
				else if ( args[i].equals(STR_ARG_ACE) )
					UaiMain.aceMode = true;
				else if ( args[i].equals(STR_ARG_RC) )
					UaiMain.rcMode = true;
				else if ( args[i].equals(STR_ARG_RCLINK) )
					UaiMain.rclinkMode = true;
				else if ( args[i].equals(STR_ARG_RCLINK2) )
					UaiMain.rclink2Mode = true;
				else if ( args[i].equals(STR_ARG_BNB) )
					UaiMain.bnbMode = true;
				else {
					System.err.println("Unrecognized option: " + args[i]);
					System.err.println("Option -help for usage.");
					System.exit(1);
				}
			}
		}

		return filename;
	}

	/* given filename, read in network, evidence (and MAP
	 * variables?) */
	public void loadBeliefNetwork(String filename) {
		this.bn = null;

		if ( filename == null ) {
			System.err.println("Input network not specified.");
			System.exit(1);
		}

		// will check for XBIF extention, and warn if not XBIF
		String fileExt = null;
		if ( filename.lastIndexOf('.') >= 0 ) 
			fileExt = filename.substring(filename.lastIndexOf('.'));

		verbosePrintln("Opening network : " + filename);
		try {
			if ( fileExt.equalsIgnoreCase( STR_XBIF ) ) {
				this.bn = new edu.ucla.belief.io.xmlbif.XmlbifParser().
					beliefNetwork( new java.io.File(filename), null );
			} else {
				System.err.println("Input file not XBIF, continuing ...");
				this.bn = edu.ucla.belief.io.NetworkIO.read(filename);
			}
		} catch ( Exception e ) {
			System.err.println("Failed loading " + filename + ": " + e);
			System.exit(1);
		}
 
		if ( this.bn == null) {
			System.err.println("Failed loading " + filename);
			System.exit(1);
		}

		if ( UaiMain.normalize ) {
			verbosePrint("Normalizing... ");
			double error = UaiMain.ensureCPTProperty(this.bn);
			verbosePrintln(" max abs error is " + error);
		}

		//verbosePrintln("Network: " + this.bn);
	}

	// Adapted from class BeliefNetworks
	public static double ensureCPTProperty(BeliefNetwork bn) {
		double error = 0.0;
		for (Iterator iter = bn.iterator(); iter.hasNext();) {
			FiniteVariable var = (FiniteVariable) iter.next();
			error = Math.max(error,UaiMain.ensureCPTProperty( var ));
		}
		return error;
	}

	// Adapted from class BeliefNetworks.
	// returns max error
	public static double ensureCPTProperty(FiniteVariable var)
	{
		Table t = var.getCPTShell().getCPT();
		double error = 0.0;
		for (int i = 0; i < t.getCPLength(); i += var.size())
		{
			double sum = 0.0;
			for (int j = i; j < i + var.size(); j++)
				sum += t.getCP(j);

			error = Math.max(error,Math.abs(1.0-sum));

			for (int j = i; j < i + var.size(); j++)
				t.setCP(j,t.getCP(j)/sum);
		}

		return error;
	}

	public void loadEvidence() {
		this.evidence = this.bn.getEvidenceController().evidence();
		
		//verbosePrintln("Evidence: " + this.evidence);
	}

	public void loadMapVariables() {
		// AC: MAP variables? no MAP queries
		this.mapVars = Collections.EMPTY_SET;

		//verbosePrintln("MAP variables: " + this.mapVars);
	}

	/****************************************
	 * Query methods 
	 ****************************************/

	public void printInfo(UaiPreprocessResult pr,UaiMapSolution sol) {
		if ( verbose == false ) return;

		verbosePrintln("=========================");
		if ( pr == null || pr.info == null ) 
			verbosePrintln("PreProcess : no info");
		else {
			verbosePrintln("PreProcess :");
			String s; Object o;
			for ( Iterator<String> it = pr.info.keySet().iterator(); it.hasNext(); ) {
				s = it.next();
				o = pr.info.get(s);
				verbosePrintln("  " + s + ": " + o);
			}
		}
		verbosePrintln("=========================");
		if ( sol == null || sol.info == null )
			verbosePrintln("Solution   : no info");
		else {
			verbosePrintln("Solution   :");
			String s; Object o;
			for ( Iterator<String> it = sol.info.keySet().iterator(); it.hasNext(); ) {
				s = it.next();
				o = sol.info.get(s);
				verbosePrintln("  " + s + ": " + o);
			}
		}
		verbosePrintln("=========================");
	}

	public abstract UaiMapSolution computeQuery(); 

	public void outputSolution(UaiMapSolution sol) {
		if ( sol == null ) {
			System.out.print("FAIL\tFAIL\tFAIL");
			return;
		}

		verbosePrintln("=========================");
		verbosePrintln("Solution Instantiation   : " + sol.instantiation);
		verbosePrintln("Solution Probability     : " + sol.probability);
		verbosePrintln("Solution Log Probability : " + sol.log_probability);

		verbosePrintln("=====begin UAI Solution:=");
		long stopTime = System.currentTimeMillis();
		double log_probability;
		if ( Double.isNaN(sol.log_probability) && sol.probability == null ) 
			log_probability = Double.NaN;
		else if ( ! Double.isNaN(sol.log_probability) )
			log_probability = sol.log_probability;
		else
			log_probability = this.bigDecimalLog(sol.probability);
		System.out.print(log_probability + "\t");
		System.out.print(((stopTime-startTime1)/1000.0) + "\t");
		System.out.print(((stopTime-startTime2)/1000.0) + "\n");
		verbosePrintln("=====end UAI Solution:===");
	}

	public static final double LOG10 = Math.log(10);
	public static double bigDecimalLog(java.math.BigDecimal bd) {
		/* convert bd to scientific notation a x 10^b
		   bd = a * 10^-b
		   note that 1 <= a < 10
		*/
		int b = bd.scale()-bd.precision()+1;
		double a = bd.movePointRight(b).doubleValue();

		/*
		  log(bd) = log(a * 10^-b)
		          = log(a) + -b*log(10)
		*/
		return Math.log(a) - b*LOG10;
	}
}
