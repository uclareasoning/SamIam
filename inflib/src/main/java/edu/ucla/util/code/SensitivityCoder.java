package edu.ucla.util.code;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;
//{superfluous} import edu.ucla.belief.Variable;
import edu.ucla.belief.Dynamator;
import edu.ucla.belief.InferenceEngine;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.sensitivity.*;

import java.io.*;
import java.util.*;

/**
	@author Keith Cascio
	@since 060204
*/
public class SensitivityCoder extends AbstractCodeGenius implements CodeGenius
{
	public static final String STR_NAME_OUTPUT_CLASS = "SensitivityTutorial";
	public static final String STR_METHOD_NAME = "getSuggestions";
	public static final OptionBreadth OPTION_AMOUNT = new OptionBreadth( STR_METHOD_NAME );
	public static final CodeOption OPTION_WITH_COMMENTS = OptionWithComments.getInstance();
	private CodeOption[] myOptions;

	/** @since 022305 */
	public String getIconFileName(){
		return "Sensitivity16.gif";
	}

	public String describe(){
		return "Java code that demonstrates using the sensitivity engine to suggest parameter changes that satisfy a constraint.";
	}

	public String getShortDescription(){
		return "sensitivity, " + getOption( OPTION_AMOUNT ) + ", " + OPTION_WITH_COMMENTS.describe( getFlag( OPTION_WITH_COMMENTS ) );
	}

	public String describeDependencies(){
		return "null";
	}

	public void describeDependencies( Tree tree )
	{
		tree.addChildOfRootNode( "asserted evidence" );
		tree.addChildOfRootNode( "inference algorithm" );
		tree.addChildOfLastNode( "compile settings" );
		tree.addChildOfRootNode( "sensitivity tool settings" );
		tree.addChildOfLastNode( "constraint" );
		tree.addChildOfLastNode( "event1" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "arithmetic operator" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "event2" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "comparison operator" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "constant" );
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
		if( myOptions == null ) myOptions = new CodeOption[] { OPTION_WITH_COMMENTS, OPTION_AMOUNT };
		return myOptions;
	}

	public SensitivityCoder(){
		super();
	}

	/** @since 20060327 */
	public OptionBreadth getOptionBreadth(){
		return OPTION_AMOUNT;
	}

	public void setEvidence( Map evidence ){
		myEvidence = evidence;
	}

	public void setPathInputFile( String path ){
		ProbabilityQueryCoder.assertPathExists( path );
		myPathInputFile = path;
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

	public boolean isMultiEvent(){
		return myVarEvent2 != null && myValueEvent2 != null && myArithmeticOperator != null;
	}

	public void setConstraint(
		FiniteVariable var1, Object value1,
		FiniteVariable var2, Object value2,
		Object opComparison,
		Object opArithmetic,
		double constant )
	{
		this.myVarEvent1 = var1;
		this.myValueEvent1 = value1;
		this.myVarEvent2 = var2;
		this.myValueEvent2 = value2;
		this.myComparisonOperator = opComparison;
		this.myArithmeticOperator = opArithmetic;
		this.myConstant = constant;
	}

	public void writePre( boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_AMOUNT.FULL_CLASS ){
			if( withComments ) out.println( "/** Import il1 classes. */" );
			out.println( "import edu.ucla.belief.sensitivity.*;" );
			out.println( "import edu.ucla.belief.*;" );
			out.println( "import edu.ucla.belief.inference.*;" );
			out.println( "import edu.ucla.belief.io.NetworkIO;" );
			out.println( "import edu.ucla.belief.io.PropertySuperintendent;" );
			out.println( "import edu.ucla.util.ProbabilityInterval;" );
			out.println();

			if( myDynamator != null ){
				ProbabilityQueryCoder.writeInferenceImports( myDynamator, withComments, out );
				out.println();
			}

			if( withComments ) out.println( "/** Import standard Java classes. */" );
			out.println( "import java.util.*;" );
			out.println( "import java.io.PrintWriter;" );
			out.println();

			if( withComments ) out.println( CPTCoder.createClassJavadocComment( this, "This class demonstrates code for a sensitivity query" ) );
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
		if( withComments ) out.println( "  /**\n    Demonstrates a single sensitivity query.\n  */" );
		out.println( "  public void "+STR_METHOD_NAME+"( BeliefNetwork bn )\n  {" );

		MAPCoder.writeEvidence( myEvidence, withComments, out );
		out.println();

		writeConstraint( withComments, out );
		out.println();

		ProbabilityQueryCoder.writeInferenceEngineCreation( myDynamator, myInferenceEngine, myBeliefNetwork, withComments, out );
		out.println();

		ProbabilityQueryCoder.writeSetEvidence( withComments, out );
		out.println();

		writeSensitivityRequest( withComments, out );
		out.println();

		if( withComments ) out.println( "    /* Clean up to avoid memory leaks. */" );
		out.println( "    engine.die();" );
		out.println();

		out.println( "    return;\n  }" );
	}

	public void writeSensitivityRequest( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Create the SensitivityEngine. */" );
		out.println( "    SensitivityEngine sensitivityengine = new SensitivityEngine( bn, engine, (PartialDerivativeEngine)engine, new PrintWriter( System.out ) );" );
		out.println();

		if( withComments ) out.println( "    /* Get the results. */" );
		if( isMultiEvent() ){
			out.println( "    SensitivityReport report = sensitivityengine.getResults( varEvent1, valueEvent1, varEvent2, valueEvent2, opArithmetic, opComparison, constant, /* flagSingleParameter */ true, /* flagSingleCPT */ true );" );
		}else{
			out.println( "    SensitivityReport report = sensitivityengine.getResults( varEvent1, valueEvent1, (FiniteVariable)null, (Object)null, (Object)null, opComparison, constant, /* flagSingleParameter */ true, /* flagSingleCPT */ true );" );
		}
		out.println();

		if( withComments ) out.println( "    /* Print the results. */" );
		out.println( "    if( report == null ){" );
		out.println( "      System.out.println( \"The current belief network already satisfies the specified constraint.\" );" );
		out.println( "      return;" );
		out.println( "    }" );
		out.println();

		out.println( "    List singleParameterSuggestions = report.generateSingleParamSuggestions();" );
		out.println( "    Map mapFiniteVariablesToSingleCPTSuggestions = report.getSingleCPTMap();" );
		out.println();

		out.println( "    if( singleParameterSuggestions.isEmpty() && mapFiniteVariablesToSingleCPTSuggestions.isEmpty() ){" );
		out.println( "      System.out.println( \"The specified constraint is unsatisfiable.\" );" );
		out.println( "      return;" );
		out.println( "    }" );
		out.println();

		out.println( "    System.out.println( \"Single parameter suggestions:\\n{\" );" );
		out.println( "    SingleParamSuggestion current = null;" );
		out.println( "    for( Iterator iterator = singleParameterSuggestions.iterator(); iterator.hasNext(); ){" );
		out.println( "      current = (SingleParamSuggestion)iterator.next();" );
		out.println( "      System.out.print( \"\\t\" + current.getCPTParameter().toString() );" );
		out.println( "      System.out.print( \" == \" + current.getCurrentValue() );" );
		out.println( "      System.out.print( \", suggested: \" + current.getSuggestedValue() );" );
		out.println( "      System.out.print( \", log odds change: \" + current.getLogOddsChange() );" );
		out.println( "      System.out.println();" );
		out.println( "    }" );
		out.println( "    System.out.println( \"}\" );" );
		out.println( "    System.out.println();" );
		out.println();

		out.println( "    System.out.println( \"Single CPT (multiple parameter) suggestions:\\n{\" );" );
		out.println( "    FiniteVariable target = null;" );
		//out.println( "    Table cpt = null;" );
		out.println( "    SingleCPTSuggestion suggestion = null;" );
		out.println( "    ProbabilityInterval[] intervals = null;" );
		out.println( "    for( Iterator iterator = mapFiniteVariablesToSingleCPTSuggestions.keySet().iterator(); iterator.hasNext(); ){" );
		out.println( "      target = (FiniteVariable)iterator.next();" );
		//out.println( "      cpt = target.getCPTShell( edu.ucla.belief.io.dsl.DSLNodeType.CPT ).getCPT();" );
		out.println( "      suggestion = (SingleCPTSuggestion) mapFiniteVariablesToSingleCPTSuggestions.get( target );" );
		out.println( "      intervals = suggestion.probabilityIntervals();" );
		out.println( "      System.out.println( \"Suggestion for \" + target.getID() + \"'s CPT, log odds change \" + suggestion.getLogOddsChange() );" );
		out.println( "      System.out.print( suggestion.toString() );" );
		out.println( "      System.out.println();" );
		out.println( "    }" );
		out.println( "    System.out.println( \"}\" );" );
		out.println( "    System.out.println();" );
		out.println();
	}

	public void writeConstraint( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Define the constraint, by variable id. */\n" );
		if( withComments ) out.println( "    /* Define event 1. */" );
		out.println( "    FiniteVariable varEvent1 = (FiniteVariable) bn.forID( \""+myVarEvent1.getID()+"\" );" );
		out.println( "    Object valueEvent1 = varEvent1.instance( \""+myValueEvent1.toString()+"\" );" );
		out.println();

		String strFieldToken = null;
		if( isMultiEvent() ){
			if( withComments ) out.println( "    /* This is a 2-event constraint, so define event 2. */" );
			out.println( "    FiniteVariable varEvent2 = (FiniteVariable) bn.forID( \""+myVarEvent2.getID()+"\" );" );
			out.println( "    Object valueEvent2 = varEvent2.instance( \""+myValueEvent2.toString()+"\" );" );
			out.println();

			if( withComments ){
				out.println( "    /* This is a 2-event constraint, so define the arithmetic operator, one of:" );
				out.println( "       SensitivityEngine.DIFFERENCE (-), SensitivityEngine.RATIO (/)" );
				out.println( "    */" );
			}
			strFieldToken = null;
			if( myArithmeticOperator == SensitivityEngine.DIFFERENCE ) strFieldToken = "DIFFERENCE";
			else if( myArithmeticOperator == SensitivityEngine.RATIO ) strFieldToken = "RATIO";
			out.println( "    Object opArithmetic = SensitivityEngine." + strFieldToken + ";" );
			out.println();
		}

		if( withComments ){
			out.println( "    /* Define the comparison operator, one of:" );
			out.println( "       SensitivityEngine.OPERATOR_EQUALS (=), SensitivityEngine.OPERATOR_GTE (>=), SensitivityEngine.OPERATOR_LTE (<=)" );
			out.println( "    */" );
		}
		if( myComparisonOperator == SensitivityEngine.OPERATOR_EQUALS ) strFieldToken = "OPERATOR_EQUALS";
		else if( myComparisonOperator == SensitivityEngine.OPERATOR_GTE ) strFieldToken = "OPERATOR_GTE";
		else if( myComparisonOperator == SensitivityEngine.OPERATOR_LTE ) strFieldToken = "OPERATOR_LTE";
		out.println( "    Object opComparison = SensitivityEngine." + strFieldToken + ";" );
		out.println();

		if( withComments ) out.println( "    /* Define the constraint constant. */" );
		out.println( "    double constant = (double)"+myConstant+";" );
	}

	private Map myEvidence;
	private String myPathInputFile;
	private Dynamator myDynamator;
	private InferenceEngine myInferenceEngine;
	private BeliefNetwork myBeliefNetwork;

	private FiniteVariable myVarEvent1;
	private Object myValueEvent1;
	private FiniteVariable myVarEvent2;
	private Object myValueEvent2;
	private Object myComparisonOperator;
	private Object myArithmeticOperator;
	private double myConstant;
}
