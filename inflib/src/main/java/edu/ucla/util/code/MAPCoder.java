package edu.ucla.util.code;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.Variable;
import edu.ucla.belief.EliminationHeuristic;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.inference.map.*;

//import il2.bridge.*;
//import il2.model.*;
//import il2.util.*;

import java.io.*;
import java.util.*;
//import java.text.DateFormat;
//import java.util.Date;

/**
	@author Keith Cascio
	@since 050604
*/
public class MAPCoder extends AbstractCodeGenius implements CodeGenius
{
	public static final String STR_NAME_OUTPUT_CLASS = "MAPTutorial";
	public static final String STR_METHOD_NAME = "doMAP";

	public static final class OptionPrune extends AbstractCodeOption implements CodeOption
	{
		public String describe(){
			return "Prune";
		}
		public String describe( boolean flag ){
			return flag ? "pruned" : "unpruned";
		}
		public String getHelpText(){
			return "Prune? - Choose whether or not to write code to prune the join tree.  Pruning is an extra step that saves memory and time by eliminating parts of the join tree unneeded for your specific query.";
		}
		public boolean isFlag(){
			return true;
		}
		public boolean getDefaultFlag(){
			return true;
		}
	}

	public static final class OptionTimings extends AbstractCodeOption implements CodeOption
	{
		public String describe(){
			return "Output timings";
		}
		public String describe( boolean flag ){
			return flag ? "timed" : "untimed";
		}
		public String getHelpText(){
			return "Timings? - Choose whether or not to write code to output timings for each phase of the MAP computation: (1) pruning, (2) initialization, (3) search.";
		}
		public boolean isFlag(){
			return true;
		}
		public boolean getDefaultFlag(){
			return true;
		}
	}

	public static final OptionTimings OPTION_TIMINGS = new OptionTimings();
	public static final OptionPrune OPTION_PRUNE = new OptionPrune();
	public static final OptionBreadth OPTION_AMOUNT = new OptionBreadth( STR_METHOD_NAME );
	public static final CodeOption OPTION_WITH_COMMENTS = OptionWithComments.getInstance();
	private CodeOption[] myOptions;

	/** @since 022305 */
	public String getIconFileName(){
		return "MAP16.gif";
	}

	public String describe(){
		return "Java code to execute MAP programatically.";
	}

	public String getShortDescription(){
		return "map, " + getOption( OPTION_AMOUNT ) + ", " + OPTION_WITH_COMMENTS.describe( getFlag( OPTION_WITH_COMMENTS ) );
	}

	public String describeDependencies(){
		return "(1) asserted evidence\n(2) MAP variables list\n(3) all MAP Tool settings, including:\n(a) approximate {search method, initialization method, steps limit}\n(b) exact {time out, slop}";
	}

	public void describeDependencies( Tree tree )
	{
		tree.addChildOfRootNode( "asserted evidence" );
		tree.addChildOfRootNode( "MAP variables list" );
		tree.addChildOfRootNode( "MAP tool settings" );
		tree.addChildOfLastNode( "approximate" );
		tree.addChildOfLastNode( "search method" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "initialization method" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "steps limit" );
		tree.lastNodeGetsParentLastNode();
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "exact" );
		tree.addChildOfLastNode( "time out" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "sloppy?" );
		tree.addChildOfLastNode( "slop" );
		tree.lastNodeGetsParentLastNode();
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "width barrier" );
	}

	public String getOutputClassNameDefault(){
		return STR_NAME_OUTPUT_CLASS;
	}

	public void writeCode( PrintStream out )
	{
		CodeOptionValue breadth = getOption( OPTION_AMOUNT );
		boolean flagWithComments = getFlag( OPTION_WITH_COMMENTS );

		writePre( flagWithComments, breadth, out );
		writeQuery( flagWithComments, out );
		writePost( flagWithComments, breadth, out );
	}

	public CodeOption[] getOptions(){
		if( myOptions == null ) myOptions = new CodeOption[] { OPTION_PRUNE, OPTION_TIMINGS, OPTION_WITH_COMMENTS, OPTION_AMOUNT };
		return myOptions;
	}

	public MAPCoder(){
		super();
	}

	/** @since 20060327 */
	public OptionBreadth getOptionBreadth(){
		return OPTION_AMOUNT;
	}

	public void setEvidence( Map evidence ){
		myEvidence = evidence;
	}

	public void setVariables( Collection vars ){
		myMAPVariables = vars;
	}

	public void setExactOrNot( boolean flag ){
		myFlagApproximate = !flag;
	}

	public void setApproximationParameters( SearchMethod sm, InitializationMethod im, int steps ){
		mySearchMethod = sm;
		myInitializationMethod = im;
		mySteps = steps;
	}

	public void setExactParameters( int timeoutsecs, int widthbarrier, boolean sloppy, double slop ){
		myTimeOutSecs = timeoutsecs;
		myWidthBarrier = widthbarrier;
		myFlagSloppy = sloppy;
		mySlop = slop;
	}

	public void setPathInputFile( String path ){
		ProbabilityQueryCoder.assertPathExists( path );
		myPathInputFile = path;
	}

	public void writePre( boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_AMOUNT.FULL_CLASS ){
			if( withComments ) out.println( "/** Import statements for il1 classes. */" );
			out.println( "import edu.ucla.belief.*;\nimport edu.ucla.belief.inference.*;\nimport edu.ucla.belief.inference.map.*;\nimport edu.ucla.belief.io.NetworkIO;\nimport edu.ucla.util.*;\n" );

			if( !myFlagApproximate ){
				if( withComments ) out.println( "/** Import statements for il2 classes. */" );
				out.println( "import il2.inf.map.MapSearch;\n" );
			}

			if( withComments ) out.println( "/** Import statements for standard Java classes. */" );
			out.println( "import java.util.*;\n" );

			if( withComments ) out.println( CPTCoder.createClassJavadocComment( this, "This class demonstrates code for a MAP query" ) );
			out.println( "public class "+getOutputClassName()+"\n{" );
			if( withComments ) out.println( "  /** Test. */" );
			out.println( "  public static void main(String[] args){\n    "+getOutputClassName()+" T = new "+getOutputClassName()+"();" );
			out.println( "    T."+STR_METHOD_NAME+"( T."+ProbabilityQueryCoder.STR_METHOD_READNETWORK_NAME+"() );" );
			out.println( "  }\n" );
		}
	}

	public void writePost( boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_AMOUNT.FULL_CLASS ){
			out.println();
			ProbabilityQueryCoder.writeReadNetworkFile( myPathInputFile, withComments, out );
			out.println( "}" );
		}
	}

	public void writeQuery( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "  /**\n    Demonstrates a MAP query.\n  */" );
		out.println( "  public void "+STR_METHOD_NAME+"( BeliefNetwork bn )\n  {" );

		writeEvidence( myEvidence, withComments, out );
		out.println();

		if( withComments ) out.println( "    /* Define the set of MAP variables, by id. */" );
		out.println( "    Set setMAPVariables = new HashSet();" );

		out.print( "    String[] arrayMAPVariableIDs = new String[] { " );
		printCollectionOfIDs( myMAPVariables, out );
		out.println( " };" );

		out.println( "    for( int i=0; i<arrayMAPVariableIDs.length; i++ ){" );
		out.println( "      setMAPVariables.add( bn.forID( arrayMAPVariableIDs[i] ) );" );
		out.println( "    }" );
		out.println();

		if( myFlagApproximate ) writeApproximationQuery( withComments, out );
		else writeExactQuery( withComments, out );
		out.println();

		out.println( "    return;\n  }" );
	}

	public static void writeEvidence( Map evidence, boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Define evidence, by id. */" );
		if( evidence == null || evidence.isEmpty() ){
			out.println( "    Map evidence = Collections.EMPTY_MAP;" );
			return;
		}

		out.println( "    Map evidence = new HashMap("+evidence.size()+");" );
		out.println( "    FiniteVariable var = null;" );
		FiniteVariable fVar = null;
		Object value = null;
		for( Iterator it = evidence.keySet().iterator(); it.hasNext(); )
		{
			fVar = (FiniteVariable) it.next();
			value = evidence.get( fVar );
			out.println( "    var = (FiniteVariable) bn.forID( \""+fVar.getID()+"\" );" );
			out.println( "    evidence.put( var, var.instance( \""+value.toString()+"\" ) );" );
		}
	}

	public void writeExactQuery( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Calculate MAP exactly. */\n" );

		if( withComments ) out.println( "    /* Define a time limit in seconds (default 60). 0 means no limit. */" );
		out.println( "    int timeoutsecs = "+myTimeOutSecs+";" );
		out.println();

		if( withComments ) out.println( "    /* Define a width barrier (default 0). 0 means no limit. */" );
		out.println( "    int widthbarrier = "+myWidthBarrier+";" );
		out.println();

		if( myFlagSloppy ) writeSloppy( withComments, out );
		else writeUnsloppy( withComments, out );
	}

	/** @since 062204 */
	private void writeSloppy( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Define a slop value (default 0). 0 means unlimited slop, i.e. return all results. */" );
		out.println( "    double slop = "+ Double.toString( mySlop) +";" );
		out.println();

		if( withComments ) out.println( "    /* Call static ExactMap method (sloppy version). */" );
		out.println( "    MapSearch.MapInfo mapinfo = ExactMap.computeMapSloppy( bn, setMAPVariables, evidence, timeoutsecs, widthbarrier, slop );" );
		out.println();

		if( getFlag( OPTION_TIMINGS ) ){
			writeExactTimings( withComments, out );
			out.println();
		}

		if( withComments ) out.println( "    /* Print the results. */" );
		out.println( "    if( mapinfo.finished ) System.out.println( \"Results are guaranteed exact.\" );" );
		out.println( "    else System.out.println( \"Results are not guaranteed exact.\" );" );
		out.println( "    VariableImpl.setStringifier( AbstractStringifier.VARIABLE_ID );" );
		out.println( "    int i=0;" );
		out.println( "    for( Iterator it = mapinfo.results.iterator(); it.hasNext(); i++ )" );
		out.println( "    {" );
		out.println( "      MapSearch.MapResult mapresult = (MapSearch.MapResult) it.next();" );
		out.println( "      System.out.println( \"Exact MAP result \"+i+\", P(MAP,e)= \" + mapresult.score );" );
		out.println( "      System.out.println( \"\\t instantiation: \" + mapresult.getConvertedInstatiation() );" );
		out.println( "    }" );
	}

	/** @since 062204 */
	private void writeUnsloppy( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Call static ExactMap method (unsloppy version). */" );
		out.println( "    MapSearch.MapInfo mapinfo = ExactMap.computeMap( bn, setMAPVariables, evidence, timeoutsecs, widthbarrier );" );
		out.println( "    MapSearch.MapResult exactmapresult = (MapSearch.MapResult) mapinfo.results.iterator().next();" );
		out.println( "    Map instantiation = exactmapresult.getConvertedInstatiation();" );
		out.println( "    double score = exactmapresult.score;" );
		out.println( "    boolean flagExact = mapinfo.finished;" );
		out.println();

		if( withComments ) out.println( "    /* Print the results. */" );
		out.println( "    System.out.println( \"Exact MAP, P(MAP,e)= \" + score );" );
		out.println( "    VariableImpl.setStringifier( AbstractStringifier.VARIABLE_ID );" );
		out.println( "    System.out.println( \"\\t instantiation: \" + instantiation );" );
		out.println( "    if( flagExact ) System.out.println( \"\\t Result is guaranteed exact.\" );" );
		out.println( "    else System.out.println( \"\\t Result is not guaranteed exact.\" );" );

		if( getFlag( OPTION_TIMINGS ) ){
			out.println();
			writeExactTimings( withComments, out );
		}
	}

	/** @since 100404 */
	private void writeExactTimings( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Print timings. */" );
		out.println( "    System.out.println();" );
		out.println( "    System.out.println( \"Pruning time, cpu: \" + mapinfo.pruneDurationMillisProfiled + \", elapsed: \" + mapinfo.pruneDurationMillisElapsed );" );
		out.println( "    System.out.println( \"Initialization time, cpu: \" + mapinfo.initDurationMillisProfiled + \", elapsed: \" + mapinfo.initDurationMillisElapsed );" );
		out.println( "    System.out.println( \"Search time, cpu: \" + mapinfo.searchDurationMillisProfiled + \", elapsed: \" + mapinfo.searchDurationMillisElapsed );" );
		out.println( "    System.out.println();" );
	}

	/** @since 100404 */
	private void writeApproximationTimings( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Print timings. */" );
		out.println( "    System.out.println();" );
		//out.println( "    System.out.println( \"Pruning time, cpu: \" + mapresult.pruneDurationMillisProfiled + \", elapsed: \" + mapresult.pruneDurationMillisElapsed );" );
		out.println( "    System.out.println( \"Initialization time, cpu: \" + mapresult.initDurationMillisProfiled + \", elapsed: \" + mapresult.initDurationMillisElapsed );" );
		out.println( "    System.out.println( \"Search time, cpu: \" + mapresult.searchDurationMillisProfiled + \", elapsed: \" + mapresult.searchDurationMillisElapsed );" );
		out.println( "    System.out.println();" );
	}

	public void writeApproximationQuery( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Approximate MAP. */\n" );

		String identifierEvidence = "evidence";

		boolean flagPrune = getFlag( OPTION_PRUNE );
		if( flagPrune )
		{
			identifierEvidence = "evidencePruned";

			if( withComments ) out.println( "    /* Prune first. */" );

			out.println( "    BeliefNetwork networkUnpruned = bn;" );
			out.println( "    Set varsUnpruned = setMAPVariables;" );
			out.println( "    Map evidenceUnpruned = evidence;" );
			out.println();

			out.println( "    Map oldToNew = new HashMap( networkUnpruned.size() );" );
			out.println( "    Map newToOld = new HashMap( networkUnpruned.size() );" );
			out.println( "    Set queryVarsPruned = new HashSet( varsUnpruned.size() );" );
			out.println( "    Map evidencePruned = new HashMap( evidenceUnpruned.size() );" );
			out.println( "    BeliefNetwork networkPruned = Prune.prune( networkUnpruned, varsUnpruned, evidenceUnpruned, oldToNew, newToOld, queryVarsPruned, evidencePruned );" );
			out.println();

			out.println( "    bn = networkPruned;" );
			out.println( "    setMAPVariables = queryVarsPruned;" );
			out.println( "    evidence = evidencePruned;" );
			out.println();
		}

		if( withComments ) out.println( "    /* Construct the right kind of inference engine. */" );
		out.println( "    JEngineGenerator generator = new JEngineGenerator();" );
		//out.println( "    JoinTreeSettings settings = generator.getSettings( bn );" );
		out.println( "    JoinTreeInferenceEngineImpl engine = generator.makeJoinTreeInferenceEngineImpl( bn, new JoinTreeSettings() );" );
		out.println();

		/* Below does not work. */
		//out.println( "    EvidenceChangeEvent event = new EvidenceChangeEvent( evidence.keySet() );" );
		//out.println( "    engine.evidenceChanged( event );" );
		/* Above does not work. */

		ProbabilityQueryCoder.writeSetEvidence( identifierEvidence, withComments, out );
		out.println();

		if( withComments ){
			out.println( "    /*\n      Define the search method, one of:" );
			out.print( "        " );
			arrayToCodePlusString( SearchMethod.ARRAY, out );
			out.println( "\n    */" );
		}
		out.println( "    SearchMethod searchmethod = SearchMethod." + toCode( mySearchMethod ) + ";" );
		out.println();

		if( withComments ){
			out.println( "    /*\n      Define the initialization method, one of:" );
			out.print( "        " );
			arrayToCodePlusString( InitializationMethod.ARRAY, out );
			out.println( "\n    */" );
		}
		out.println( "    InitializationMethod initializationmethod = InitializationMethod." + toCode( myInitializationMethod ) + ";" );
		out.println();

		if( withComments ) out.println( "    /* Define a limit on the number of search steps (default 25). */" );
		out.println( "    int steps = "+mySteps+";" );
		out.println();

		if( withComments ) out.println( "    /* Construct a MapRunner and run the query. */" );
		out.println( "    MapRunner maprunner = new MapRunner();" );
		out.println( "    MapRunner.MapResult mapresult = maprunner.approximateMap( bn, engine, setMAPVariables, evidence, searchmethod, initializationmethod, steps );" );
		out.println( "    Map instantiation = mapresult.instantiation;" );
		out.println( "    double score = mapresult.score;" );
		out.println();

		if( withComments ) out.println( "    /* Print the results. */" );
		out.println( "    System.out.println( \"Approximate MAP, P(MAP,e)= \" + score );" );
		out.println( "    System.out.println( \"\\t P(MAP|e)= \" + ( score/engine.probability() ) );" );
		out.println( "    VariableImpl.setStringifier( AbstractStringifier.VARIABLE_ID );" );
		out.println( "    System.out.println( \"\\t instantiation: \" + instantiation );" );
		out.println();

		if( getFlag( OPTION_TIMINGS ) ){
			writeApproximationTimings( withComments, out );
			out.println();
		}

		if( withComments ) out.println( "    /* Clean up to avoid memory leaks. */" );
		out.println( "    engine.die();" );
	}

	public static void printCollectionOfIDs( Collection list, PrintStream out )
	{
		if( list == null ) return;

		StringBuffer buffer = new StringBuffer( list.size()*20 );
		for( Iterator it = list.iterator(); it.hasNext(); ){
			buffer.append( "\"" );
			buffer.append( ((Variable)it.next()).getID() );
			buffer.append( "\", " );
		}
		int lenBuffer = buffer.length();
		int indexEnd = ( lenBuffer > 2 ) ? lenBuffer-2 : lenBuffer;
		out.print( buffer.substring( 0, indexEnd ) );
	}

	public static void arrayToCode( Object[] values, PrintStream out )
	{
		int length = values.length;
		int lastIndex = length-1;
		for( int i=0; i<lastIndex; i++ ){
			out.print( toCode( values[i] ) + ", " );
		}
		out.print( toCode( values[lastIndex] ) );
	}

	public static void arrayToCodePlusString( Object[] values, PrintStream out )
	{
		int length = values.length;
		int lastIndex = length-1;
		for( int i=0; i<lastIndex; i++ ){
			out.print( toCode( values[i] ) + " (" + values[i].toString() + "), " );
		}
		out.print( toCode( values[lastIndex] ) + " (" + values[lastIndex].toString() + ")" );
	}

	public static String toCode( Object obj ){
		if( obj instanceof SearchMethod ) return ((SearchMethod)obj).getJavaCodeName();
		else if( obj instanceof InitializationMethod ) return ((InitializationMethod)obj).getJavaCodeName();
		else if( obj instanceof EliminationHeuristic ) return ((EliminationHeuristic)obj).getJavaCodeName();
		else return "Unknown_Object";
	}

	private Map myEvidence;
	private Collection myMAPVariables;
	private SearchMethod mySearchMethod = SearchMethod.getDefault();
	private InitializationMethod myInitializationMethod = InitializationMethod.getDefault();
	private int mySteps = (int)25;
	private boolean myFlagApproximate = true;
	private int myTimeOutSecs = (int)60;
	private int myWidthBarrier = (int)0;
	private boolean myFlagSloppy = false;
	private double mySlop = (double)0.5;
	private String myPathInputFile;
}
