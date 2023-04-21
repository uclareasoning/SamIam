package edu.ucla.util.code;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;
//{superfluous} import edu.ucla.belief.Variable;
import edu.ucla.belief.Dynamator;
import edu.ucla.belief.InferenceEngine;
import edu.ucla.belief.EliminationHeuristic;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.io.PropertySuperintendent;

//import il2.bridge.*;
//import il2.model.*;
//import il2.util.*;

import java.io.*;
import java.util.*;

/** @author keith cascio
	@since  20040520 */
public class ProbabilityQueryCoder extends AbstractCodeGenius implements CodeGenius
{
	public static final String STR_NAME_OUTPUT_CLASS = "ProbabilityQueryTutorial";
	public static final String STR_METHOD_NAME = "doProbabilityQuery";
	public static final String STR_METHOD_READNETWORK_NAME = "readNetworkFile";

	public static final class MarginalScopeOption extends AbstractCodeOption
	{
		public String describe(){
			return "Marginals for";
		}
		public String getHelpText(){
			return "Calculate marginals for? - Choose which variables the marginal probability calculation includes.";
		}
		public CodeOptionValue getDefault(){
			return MONITOR;
		}
		public CodeOptionValue[] getValues(){
			return ARRAY_VALUES;
		}

		public final CodeOptionValue MONITOR = new CodeOptionValue( "only variables with monitors visible" );
		public final CodeOptionValue ALL = new CodeOptionValue( "all variables" );
		public final CodeOptionValue NONE = new CodeOptionValue( "none" );
		public final CodeOptionValue[] ARRAY_VALUES = new CodeOptionValue[] { MONITOR, ALL, NONE };
	};
	public static final MarginalScopeOption OPTION_MARGINALSCOPE = new MarginalScopeOption();

	public static final OptionBreadth OPTION_BREADTH = new OptionBreadth( STR_METHOD_NAME );
	public static final CodeOption OPTION_WITH_COMMENTS = OptionWithComments.getInstance();
	private CodeOption[] myOptions;

	/** @since 022305 */
	public String getIconFileName(){
		return "Selected16.gif";
	}

	public String describe(){
		return "Java code to calculate marginal probabilities and Pr(e).";
	}

	public String getShortDescription(){
		return "probability query, " + getOption( OPTION_BREADTH ) + ", " + OPTION_WITH_COMMENTS.describe( getFlag( OPTION_WITH_COMMENTS ) );
	}

	public String describeDependencies(){
		return "(1) asserted evidence\n(2) visible monitors\n(3) inference algorithm\n(4) compile settings";
	}

	public void describeDependencies( Tree tree )
	{
		tree.addChildOfRootNode( "asserted evidence" );
		tree.addChildOfRootNode( "visible monitors" );
		tree.addChildOfRootNode( "inference algorithm" );
		tree.addChildOfLastNode( "compile settings" );
	}

	public String getOutputClassNameDefault(){
		return STR_NAME_OUTPUT_CLASS;
	}

	public void writeCode( PrintStream out )
	{
		CodeOptionValue breadth = getOption( OPTION_BREADTH );
		boolean flagWithComments = getFlag( OPTION_WITH_COMMENTS );

		writePre( flagWithComments, breadth, out );
		writeQuery( flagWithComments, out );
		writePost( flagWithComments, breadth, out );
	}

	public CodeOption[] getOptions(){
		if( myOptions == null ) myOptions = new CodeOption[] { OPTION_WITH_COMMENTS, OPTION_BREADTH, OPTION_MARGINALSCOPE };
		return myOptions;
	}

	public ProbabilityQueryCoder(){
		super();
	}

	/** @since 20060327 */
	public OptionBreadth getOptionBreadth(){
		return OPTION_BREADTH;
	}

	public void setEvidence( Map evidence ){
		myEvidence = evidence;
	}

	public void setVariables( Collection vars ){
		myMarginalVariables = vars;
	}

	public void setDynamator( Dynamator dyn ){
		myDynamator = dyn;
	}

	public void setInferenceEngine( InferenceEngine ie ){
		myInferenceEngine = ie;
	}

	public void setBeliefNetwork( BeliefNetwork bn ){
		myBeliefNetwork = bn;
	}

	public void setPathInputFile( String path ){
		assertPathExists( path );
		myPathInputFile = path;
	}

	public void writePre( boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_BREADTH.FULL_CLASS ){
			if( withComments ) out.println( "/** Import il1 classes. */" );
			out.println( "import edu.ucla.belief.*;\nimport edu.ucla.belief.inference.*;\nimport edu.ucla.belief.io.PropertySuperintendent;\nimport edu.ucla.belief.io.NetworkIO;\n" );

			writeInferenceImports( myDynamator, withComments, out );
			out.println();

			if( withComments ) out.println( "/** Import standard Java classes. */" );
			out.println( "import java.util.*;\n" );

			if( withComments ) out.println( CPTCoder.createClassJavadocComment( this, "This class demonstrates code for a probability query" ) );
			out.println( "public class "+getOutputClassName()+"\n{" );
			if( withComments ) out.println( "  /** Test. */" );
			out.println( "  public static void main(String[] args){\n    "+getOutputClassName()+" T = new "+getOutputClassName()+"();" );
			out.println( "    T."+STR_METHOD_NAME+"( T."+STR_METHOD_READNETWORK_NAME+"() );" );
			out.println( "  }\n" );
		}
	}

	public void writePost( boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_BREADTH.FULL_CLASS ){
			out.println();
			writeReadNetworkFile( myPathInputFile, withComments, out );
			out.println( "}" );
		}
	}

	/** @since 20100108 */
	public static String simple( Class clazz ){
		String nameclass = clazz.getName();
		return nameclass.substring( Math.max( nameclass.lastIndexOf( '.' ), nameclass.lastIndexOf( '$' ) ) + 1 );
	}

	/** @since 20060327 */
	public static int writeInferenceImports( Dynamator dyn, boolean withComments, PrintStream out ){
		int count = 0;
		if( (dyn  == null)                   ){ return count; }
		Collection deps = dyn.getClassDependencies();
		if( (deps == null) || deps.isEmpty() ){ return count; }
		if( withComments ) out.println( "/** Import classes for "+dyn.getDisplayName()+". */" );
		//avoid Xenginegenerator000L.java:11: edu.ucla.belief.inference.XXEngine is already defined in a single-type import
		Set singleTypeImports = new HashSet( deps.size() );
		Class  clazz  = null;
		String simple = null, prefix = null;
		for( Iterator it = deps.iterator(); it.hasNext(); ){
			clazz  = (Class) it.next();
			if(   singleTypeImports.contains( simple = simple( clazz ) ) ){ prefix = "// ";          }
			else{ singleTypeImports.add(      simple                   );   prefix =    ""; ++count; }
			out.println( prefix + "import " + clazz.getName().replace( '$', '.' ) + ";" );
		}
		return count;
	}

	/** @since 20060327 */
	public static void assertPathExists( String pathInputFile ) throws IllegalArgumentException{
		if( (pathInputFile != null) && !(new File(pathInputFile).exists()) ) throw new IllegalArgumentException( "ProbabilityQueryCoder.writeReadNetworkFile() called with path that does not exist \"" +pathInputFile+ "\"" );
	}

	public static void writeReadNetworkFile( String pathInputFile, boolean withComments, PrintStream out )
	{
		assertPathExists( pathInputFile );

		if( withComments ) out.println( "  /**\n    Open the network file used to create this tutorial.\n  */" );
		out.println( "  public BeliefNetwork "+STR_METHOD_READNETWORK_NAME+"()\n  {" );

		if( pathInputFile != null )
		{
			out.println( "    String path = \""+pathInputFile.replaceAll( "\\\\", "\\\\\\\\" )+"\";" );
			out.println();
			out.println( "    BeliefNetwork ret = null;" );
			out.println( "    try{" );
			if( withComments ) out.println( "      /* Use NetworkIO static method to read network file. */" );
			out.println( "      ret = NetworkIO.read( path );" );
			out.println( "    }catch( Exception e ){" );
			out.println( "      System.err.println( \"Error, failed to read \\\"\" + path + \"\\\": \" + e );" );
			out.println( "      return (BeliefNetwork)null;" );
			out.println( "    }" );
			out.println( "    return ret;" );
		}
		else
		{
			out.println( "    //returning null (should call ProbabilityQueryCoder.setPathInputFile())" );
			out.println( "    return (BeliefNetwork)null;" );
		}
		out.println( "  }" );
	}

	/** @since 060304 */
	public static void writeSetEvidence( boolean withComments, PrintStream out ){
		writeSetEvidence( "evidence", withComments, out );
	}

	public static void writeSetEvidence( String identifier, boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Set evidence. */" );
		out.println( "    try{" );
		out.println( "      bn.getEvidenceController().setObservations( "+identifier+" );" );
		out.println( "    }catch( StateNotFoundException e ){" );
		out.println( "      System.err.println( \"Error, failed to set evidence: \" + e );" );
		out.println( "      return;" );
		out.println( "    };" );
	}

	public void writeQuery( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "  /**\n    Demonstrates a probability query.\n  */" );
		out.println( "  public void "+STR_METHOD_NAME+"( BeliefNetwork bn )\n  {" );

		MAPCoder.writeEvidence( myEvidence, withComments, out );
		out.println();

		writeInferenceEngineCreation( withComments, out );
		out.println();

		writeSetEvidence( withComments, out );
		out.println();

		if( withComments ) out.println( "    /* Calculate Pr(e). */" );
		out.println( "    double pE = engine.probability();" );
		out.println( "    System.out.println( \"Pr(e): \" + pE );" );
		out.println( "    System.out.println();" );
		out.println();

		Collection varsMarginals = Collections.EMPTY_SET;
		CodeOptionValue marginalscope = getOption( OPTION_MARGINALSCOPE );
		if( (marginalscope == OPTION_MARGINALSCOPE.ALL) && (myBeliefNetwork != null) ) varsMarginals = myBeliefNetwork;
		else if( (marginalscope == OPTION_MARGINALSCOPE.MONITOR) && (myMarginalVariables != null) ){
			if( withComments && myMarginalVariables.isEmpty() ) out.println( "    /*\n      Warning: you chose to calculate marginals for only those\n      variables for which monitors were visible, but there were\n      no such variables.\n    */" );
			varsMarginals = myMarginalVariables;
		}

		if( !varsMarginals.isEmpty() )
		{
			if( withComments ) out.println( "    /* Define the set of "+varsMarginals.size()+" variables for which we want marginal probabilities, by id. */" );
			out.println( "    Set setMarginalVariables = new HashSet();" );
			out.print( "    String[] arrayMarginalVariableIDs = new String[] { " );
			MAPCoder.printCollectionOfIDs( varsMarginals, out );
			out.println( " };" );
			out.println( "    for( int i=0; i<arrayMarginalVariableIDs.length; i++ ){" );
			out.println( "      setMarginalVariables.add( bn.forID( arrayMarginalVariableIDs[i] ) );" );
			out.println( "    }" );
			out.println();

			if( withComments ) out.println( "    /* Calculate marginals. */" );
			out.println( "    System.out.println( \"Marginal probability tables:\" );" );
			out.println( "    FiniteVariable varMarginal = null;" );
			out.println( "    Table answer = null;" );
			out.println( "    for( Iterator iterator = setMarginalVariables.iterator(); iterator.hasNext(); ){" );
			out.println( "      varMarginal = (FiniteVariable)iterator.next();" );
			out.println( "      answer = engine.conditional( varMarginal );" );
			//out.println( "      System.out.println( varMarginal.getID() + \", marginal:\\n\" + answer.tableString((String)null) + \"\\n\" );" );
			out.println( "      System.out.println( answer.tableString() );" );
			out.println( "      System.out.println();" );
			out.println( "    }" );
			out.println();
		}

		if( withComments ) out.println( "    /* Clean up to avoid memory leaks. */" );
		out.println( "    engine.die();" );
		out.println();

		out.println( "    return;\n  }" );
	}

	public void writeInferenceEngineCreation( boolean withComments, PrintStream out ){
		writeInferenceEngineCreation( myDynamator, myInferenceEngine, myBeliefNetwork, withComments, out );
	}

	public static void writeInferenceEngineCreation( Dynamator dynamator, InferenceEngine inferenceengine, BeliefNetwork beliefnetwork, boolean withComments, PrintStream out )
	{
		writeDynamatorCreation( dynamator, inferenceengine, beliefnetwork, withComments, out );

		if( withComments ) out.println( "    /* Create the InferenceEngine. */" );
		out.println( "    InferenceEngine engine = dynamator.manufactureInferenceEngine( bn );" );
	}

	/** @since 052105 */
	public static void writeDynamatorCreation( Dynamator dynamator, InferenceEngine inferenceengine, BeliefNetwork beliefnetwork, boolean withComments, PrintStream out ){
		if( withComments ){
			out.print( "    /* Create the Dynamator" );
			if( inferenceengine != null ) out.print( "("+inferenceengine.getClass().getName()+")" );
			out.println( ". */" );
		}
		String strClassNameDynamator = dynamator.getClass().getName();
		out.println( "    "+strClassNameDynamator+" dynamator = new "+strClassNameDynamator+"();" );
		out.println();

		writeSettingsManipulation( dynamator, beliefnetwork, withComments, out );
	}

	public static void writeSettingsManipulation( Dynamator dynamator, BeliefNetwork beliefnetwork, boolean withComments, PrintStream out )
	{
		dynamator = (dynamator == null ? null : dynamator.getCanonicalDynamator());
		if( dynamator != null ){ dynamator.writeJavaCodeSettingsManipulation( beliefnetwork, withComments, out ); }
	}

	/** Test/debug. */
	public static void main( String[] args )
	{
		String pathNetwork = "c:\\keithcascio\\networks\\cancer.net";
		String pathCode = "c:\\keithcascio\\dev\\inflib\\" +STR_NAME_OUTPUT_CLASS+ ".java";

		BeliefNetwork bn;
		try{ bn = NetworkIO.read( pathNetwork ); }
		catch( Exception e ){
			e.printStackTrace();
			return;
		}

		Map evidence = new HashMap();
		Collection vars = new LinkedList();

		FiniteVariable fVar;
		int i=0;
		Iterator it = bn.iterator();
		for( ;it.hasNext() && i<2; i++ ){
			fVar = (FiniteVariable) it.next();
			evidence.put( fVar, fVar.instance(0) );
		}
		while( it.hasNext() ){
			vars.add( it.next() );
		}

		ProbabilityQueryCoder pqc = new ProbabilityQueryCoder();
		pqc.setPathInputFile( pathNetwork );
		pqc.setEvidence( evidence );
		pqc.setVariables( vars );
		pqc.setBeliefNetwork( bn );
		pqc.setDynamator( new JEngineGenerator() );

		try{ pqc.writeCode( new PrintStream( new FileOutputStream( new File( pathCode ) ) ) ); }
		catch( Exception e ){
			e.printStackTrace();
			return;
		}
	}

	private Map myEvidence;
	private Collection myMarginalVariables;
	private Dynamator myDynamator;
	private InferenceEngine myInferenceEngine;
	private BeliefNetwork myBeliefNetwork;
	private String myPathInputFile;
}
