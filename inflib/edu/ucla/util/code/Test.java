package edu.ucla.util.code;

import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.*;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.inference.map.*;
import edu.ucla.belief.sensitivity.*;
import edu.ucla.belief.learn.Learning;

import java.util.*;
import java.io.*;

/** Tests each of the CodeGenius sub classes
	by writing one or more Java files intended
	to be compiled and run.

	@author Keith Cascio
	@since 021005 */
public class Test
{
	public static final String STR_FLAG_NETWORK_FILE = "-in";
	public static final String STR_FLAG_EM_DATA_FILE = "-em";
	public static final String STR_FLAG_MINUS = "--";
	public static final String STR_FLAG_PLUS = "++";
	public static final String[] ARRAY_FLAGS = new String[] { STR_FLAG_NETWORK_FILE, STR_FLAG_MINUS, STR_FLAG_PLUS, STR_FLAG_EM_DATA_FILE };

	public static final String STR_FILENAME_JAVAFILELIST = "javafiles.testdata";
	public static final String STR_FILENAME_CLASSFILELIST = "classfiles.testdata";

	public static void main( String[] args )
	{
		Map parsed = NetworkIO.parseArgs( ARRAY_FLAGS, args, (Map)null );

		File ifile = null;
		String inputPath = (String) parsed.get( STR_FLAG_NETWORK_FILE );
		if( inputPath != null ){
			ifile = new File( inputPath );
		}

		if( ifile == null ){
			System.err.println( "usage: " + Test.class.getName() + " " + STR_FLAG_NETWORK_FILE + "<path to network file>" );
			System.exit( fail( null, null ) );
		}
		else if( !ifile.exists() ){
			System.err.println( inputPath + " not found" );
			System.exit( fail( null, null ) );
		}

		Test test = null;
		Throwable thrown = null;
		try{
			(test = new Test( parsed )).testRoot( ifile );
		}catch( Throwable throwable ){
			thrown = throwable;
		}

		try{
			test.writeFileLists();
		}catch( Throwable throwable ){
			thrown = throwable;
		}

		if( thrown != null ) System.exit( fail( null, thrown ) );
	}

	public Test( Map arguments ){
		this.arguments = arguments;
	}

	public void testRoot( File ifile ) throws Exception
	{
		this.inputFile = ifile;

		init();

		if( shouldRun( "MAPCoder" ) ) testMAPCoder();

		if( shouldRun( "ModelCoder" ) ) testModelCoder();

		if( shouldRun( "CPTCoder" ) ) testCPTCoder();

		if( shouldRun( "MPECoder" ) ) testMPECoder();

		if( shouldRun( "ProbabilityQueryCoder" ) ) testProbabilityQueryCoder();

		if( shouldRun( "SensitivityCoder" ) ) testSensitivityCoder();

		if( shouldRun( "EMCoder" ) ) testEMCoder();
	}

	public boolean shouldRun( String classname ){
		if( arguments == null ) return true;

		if( arguments.get( classname ) == Boolean.TRUE ) return true;
		else if( arguments.get( "-" + classname ) == Boolean.TRUE ) return true;
		else if( arguments.get( "test" + classname ) == Boolean.TRUE ) return true;
		else if( arguments.get( "-test" + classname ) == Boolean.TRUE ) return true;
		else if( classname.equals( arguments.get( STR_FLAG_MINUS ) ) ) return false;
		else if( classname.equals( arguments.get( STR_FLAG_PLUS ) ) ) return true;

		return !arguments.containsKey( STR_FLAG_PLUS );
	}

	public void init() throws Exception {
		fileJavaFileNames = new File( STR_FILENAME_JAVAFILELIST );
		fileJavaFileNames.delete();
		fileClassFileNames = new File( STR_FILENAME_CLASSFILELIST );
		fileClassFileNames.delete();
		bn = NetworkIO.read( inputFile );
		sizeNetwork = bn.size();
		randomEvidence();
	}

	public void testEMCoder() throws Exception {
		EMCoder emcoder = new EMCoder();

		double threshold = (double)0.05;
		int maxIterations = (int)5;
		boolean flagWithBias = true;

		File fileData = null;
		String pathDataFile = (String) arguments.get( STR_FLAG_EM_DATA_FILE );
		if( pathDataFile != null ){
			fileData = new File( pathDataFile );
		}
		else throw new Exception( "EM test requires data file.  Please specify with command line option \"" + STR_FLAG_EM_DATA_FILE + "\"." );

		if( !fileData.exists() ) throw new Exception( "EM data file \"" +fileData.getAbsolutePath()+ "\" does not exist." );

		emcoder.set( threshold, maxIterations, flagWithBias );

		String pathInput = inputFile.getAbsolutePath();
		emcoder.setInputNetwork( bn, pathInput );
		emcoder.setPathDataFile( pathDataFile );
		emcoder.setPathOutputNetworkFile( Learning.renamePathForEmOutput( pathInput ) );

		emcoder.setDynamator( new JEngineGenerator() );

		writeCode( emcoder, "EMTutorial" );
	}

	public void testMAPCoder() throws Exception {
		MAPCoder mapcoder = new MAPCoder();

		mapcoder.setEvidence( evidence );
		mapcoder.setPathInputFile( inputFile.getAbsolutePath() );
		mapcoder.setVariables( randomSubset( evidenceComplement, (double)0.67 ) );

		mapcoder.setFlag( MAPCoder.OPTION_PRUNE, true );
		mapcoder.setFlag( MAPCoder.OPTION_TIMINGS, true );

		mapcoder.setExactOrNot( true );
		mapcoder.setExactParameters(/*timeoutsecs*/120,/*widthbarrier*/-1,/*sloppy*/true,/*slop*/(double)0.5);

		writeCode( mapcoder, "MAPTutorialExact" );

		mapcoder.setExactOrNot( false );
		mapcoder.setApproximationParameters(
			(SearchMethod) randomElement( SearchMethod.ARRAY ),
			(InitializationMethod) randomElement( InitializationMethod.ARRAY ),
			/*steps*/25 );

		writeCode( mapcoder, "MAPTutorialApproximate" );
	}

	public void testCPTCoder() throws Exception {
		CPTCoder cptcoder = new CPTCoder();
		writeCode( cptcoder, CPTCoder.STR_NAME_OUTPUT_CLASS );
	}

	public void testModelCoder() throws Exception {
		ModelCoder modelcoder = new ModelCoder( bn, inputFile.getAbsolutePath() );

		modelcoder.setOption( ModelCoder.OPTION_LIBRARY_VERSION, ModelCoder.OPTION_LIBRARY_VERSION.BOTH );

		writeCode( modelcoder, "ModelTutorialBoth" );
	}

	public void testMPECoder() throws Exception {
		MPECoder mpecoder = new MPECoder();

		mpecoder.setPathInputFile( inputFile.getAbsolutePath() );
		mpecoder.setEvidence( evidence );

		writeCode( mpecoder, "MPETutorial" );
	}

	/** @since 20100108 */
	public           static      final String[]
	  ARRAY_DYNAMATOR_CLASSNAMES = new String[]{
	    "edu.ucla.belief.inference.RCEngineGenerator",
	    "edu.ucla.belief.inference.JEngineGenerator",
	    "edu.ucla.belief.inference.HuginEngineGenerator",
	    "edu.ucla.belief.inference.ZCEngineGenerator",
	    "edu.ucla.belief.inference.SSEngineGenerator",
	    "edu.ucla.belief.recursiveconditioning.RCEngineGenerator",
	    "edu.ucla.belief.approx.PropagationEngineGenerator",
	    "edu.ucla.belief.approx.EdgeDeletionEngineGenerator",
	    "edu.ucla.belief.approx.RecoveryEngineGenerator"
	  };

	/** @since 20100108 */
	public static Dynamator dynamatorForClass( String className ){
		try{
			Class clazz = Class.forName( className );
			return (Dynamator) clazz.newInstance();
		}catch( Throwable thrown ){
			System.err.println( "warning: failed to construct " + className );
			System.err.println( "    " + thrown );
		}
		return null;
	}

	public void testProbabilityQueryCoder() throws Exception {
		ProbabilityQueryCoder probabilityquerycoder = new ProbabilityQueryCoder();

		probabilityquerycoder.setOption( ProbabilityQueryCoder.OPTION_MARGINALSCOPE, ProbabilityQueryCoder.OPTION_MARGINALSCOPE.ALL );

		probabilityquerycoder.setBeliefNetwork( bn );
		probabilityquerycoder.setEvidence( evidence );
		probabilityquerycoder.setPathInputFile( inputFile.getAbsolutePath() );

		ArrayList dynamators = new ArrayList( ARRAY_DYNAMATOR_CLASSNAMES.length );
		Dynamator dyn = null;
		for( int i=0; i<ARRAY_DYNAMATOR_CLASSNAMES.length; i++ ){
			if( (dyn = dynamatorForClass( ARRAY_DYNAMATOR_CLASSNAMES[i] )) != null ){ dynamators.add( dyn ); }
		}

		for( Iterator it = dynamators.iterator(); it.hasNext(); ){
			probabilityquerycoder.setDynamator( dyn = (Dynamator) it.next() );
			writeCode( probabilityquerycoder, "ProbabilityTutorial" + dyn.getKey() );
		}
	}

	public void testSensitivityCoder() throws Exception {
		SensitivityCoder sensitivitycoder = new SensitivityCoder();

		sensitivitycoder.setBeliefNetwork( bn );
		sensitivitycoder.setEvidence( evidence );
		sensitivitycoder.setPathInputFile( inputFile.getAbsolutePath() );

		Dynamator dynamator = new SSEngineGenerator();
		sensitivitycoder.setDynamator( dynamator );
		InferenceEngine ie = dynamator.manufactureInferenceEngine( bn );
		bn.getEvidenceController().setObservations( evidence );
		sensitivitycoder.setInferenceEngine( ie );

		Random random = getRandom();
		FiniteVariable var1 = (FiniteVariable) randomElement( evidenceComplement );
		int index1 = random.nextInt( var1.size() );
		Object value1 = var1.instance( index1 );
		Table posterior1 = ie.conditional( var1 );
		double prob1 = posterior1.getCP( index1 );

		FiniteVariable var2 = null;
		Object value2 = null;

		Object opArithmetic = randomElement( SensitivityEngine.ARITHMETIC_OPERATORS );
		Object opComparison = randomElement( SensitivityEngine.COMPARISON_OPERATORS );
		double constant = random.nextDouble();

		//double delta = (prob1*((double)0.1));
		if( (opComparison == SensitivityEngine.OPERATOR_GTE) || (opComparison == SensitivityEngine.OPERATOR_EQUALS) ) constant = prob1 + ((1.0-prob1)*0.1);
		else if( opComparison == SensitivityEngine.OPERATOR_LTE ) constant = prob1 - (prob1*0.1);

		sensitivitycoder.setConstraint(
			/*var1*/var1,
			/*value1*/value1,
			/*var2*/var2,
			/*value2*/value2,
			/*opComparison*/opComparison,
			/*opArithmetic*/opArithmetic,
			/*constant*/constant );

		writeCode( sensitivitycoder, "SensitivityTutorialSSOneEvent" );

		Set leftover = new HashSet( evidenceComplement );
		leftover.remove( var1 );
		if( leftover.isEmpty() ) return;

		opArithmetic = randomElement( SensitivityEngine.ARITHMETIC_OPERATORS );
		opComparison = randomElement( SensitivityEngine.COMPARISON_OPERATORS );
		constant = random.nextDouble();

		var2 = (FiniteVariable) randomElement( leftover );
		int index2 = random.nextInt( var2.size() );
		value2 = var2.instance( index2 );
		Table posterior2 = ie.conditional( var2 );
		double prob2 = posterior2.getCP( index2 );

		if( opArithmetic == SensitivityEngine.DIFFERENCE ){
			double larger = Math.max( prob1, prob2 );
			double smaller = Math.min( prob1, prob2 );
			constant = (larger - smaller) + 0.00001;
			opComparison = SensitivityEngine.OPERATOR_GTE;
		}
		else if( opArithmetic == SensitivityEngine.RATIO ){
			constant = (prob1/prob2) * 1.1;
			opComparison = SensitivityEngine.OPERATOR_GTE;
		}

		sensitivitycoder.setConstraint(
			/*var1*/var1,
			/*value1*/value1,
			/*var2*/var2,
			/*value2*/value2,
			/*opComparison*/opComparison,
			/*opArithmetic*/opArithmetic,
			/*constant*/constant );

		writeCode( sensitivitycoder, "SensitivityTutorialSSTwoEvents" );
	}

	public void writeCode( CodeGenius genius, String className ) throws Exception
	{
		genius.setOutputClassName( className );
		genius.setFlag( OptionWithComments.getInstance(), getRandom().nextBoolean() );

		String javaFileName = className + ".java";
		PrintStream out = new PrintStream( new FileOutputStream( new File( javaFileName ) ) );
		genius.writeCode( out );
		out.close();
		if( listJavaFileNames == null ) listJavaFileNames = new LinkedList();
		listJavaFileNames.add( javaFileName );
		if( listClassFileNames == null ) listClassFileNames = new LinkedList();
		listClassFileNames.add( className );
	}

	public Random getRandom(){
		if( this.random == null ) this.random = new Random();
		return this.random;
	}

	public static Random getRandomStatic(){
		if( RANDOM == null ) RANDOM = new Random();
		return RANDOM;
	}
	public static Random RANDOM;

	public void writeFileLists() throws Exception {
		writeFileList( listJavaFileNames, fileJavaFileNames );
		writeFileList( listClassFileNames, fileClassFileNames );
	}

	public void writeFileList( List list, File file ) throws Exception {
		if( (list == null) || list.isEmpty() ) return;
		PrintStream out = new PrintStream( new FileOutputStream( file ) );
		for( Iterator it = list.iterator(); it.hasNext(); ){
			out.println( it.next() );
		}
		out.close();
	}

	public void randomEvidence(){
		int count = sizeNetwork / 2;
		evidence = new HashMap( count );
		Random random = getRandom();
		ArrayList variables = new ArrayList( bn );
		int numVars = variables.size();
		FiniteVariable var;
		Object value;
		for( int i=0; i<count; i++ ){
			var = (FiniteVariable) variables.get( random.nextInt( numVars ) );
			value = var.instance( random.nextInt( var.size() ) );
			evidence.put( var, value );
		}
		evidenceComplement = new HashSet( bn );
		evidenceComplement.removeAll( evidence.keySet() );
	}

	public static Set randomSubset( Collection superset, double fraction ){
		int count = (int) Math.ceil( ((double)superset.size())*fraction );
		Set ret = new HashSet( count );
		Random random = getRandomStatic();
		ArrayList superlist = new ArrayList( superset );
		int superSize = superlist.size();
		for( int i=0; i<count; i++ ){
			ret.add( superlist.get( random.nextInt( superSize ) ) );
		}
		return ret;
	}

	public static Object randomElement( Collection collection ){
		int index = getRandomStatic().nextInt( collection.size() );
		int i=0;
		for( Iterator it = collection.iterator(); it.hasNext(); ){
			if( (i++) == index ) return it.next();
			it.next();
		}
		return null;
	}

	public static Object randomElement( Object[] array ){
		return array[ getRandomStatic().nextInt( array.length ) ];
	}

	public static int fail( String message, Throwable throwable ){
		System.err.println( "Fail" );
		if( throwable != null ){
			throwable.printStackTrace();
			System.err.println( "Fail" );
		}
		return -1;
	}

	public Map arguments;
	public File inputFile;
	public BeliefNetwork bn;
	public int sizeNetwork;
	public Map evidence;
	public Set evidenceComplement;
	public List listJavaFileNames;
	public List listClassFileNames;
	public File fileJavaFileNames;
	public File fileClassFileNames;
	public Random random;
}
