package edu.ucla.util.code;

import edu.ucla.belief.BeliefNetwork;
//{superfluous} import edu.ucla.belief.FiniteVariable;
//{superfluous} import edu.ucla.belief.Variable;
//{superfluous} import edu.ucla.belief.EliminationHeuristic;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.inference.map.*;

import java.io.*;
import java.util.*;

/**
	@author Keith Cascio
	@since 060204
*/
public class MPECoder extends AbstractCodeGenius implements CodeGenius
{
	public static final String STR_NAME_OUTPUT_CLASS = "MPETutorial";
	public static final String STR_METHOD_NAME = "doMPE";
	public static final OptionBreadth OPTION_AMOUNT = new OptionBreadth( STR_METHOD_NAME );
	public static final CodeOption OPTION_WITH_COMMENTS = OptionWithComments.getInstance();
	private CodeOption[] myOptions;

	/** @since 022305 */
	public String getIconFileName(){
		return "MPE16.gif";
	}

	public String describe(){
		return "Java code to execute MPE programatically.";
	}

	public String getShortDescription(){
		return "mpe, " + getOption( OPTION_AMOUNT ) + ", " + OPTION_WITH_COMMENTS.describe( getFlag( OPTION_WITH_COMMENTS ) );
	}

	public String describeDependencies(){
		return "(1) asserted evidence";
	}

	public void describeDependencies( Tree tree )
	{
		tree.addChildOfRootNode( "asserted evidence" );
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

	public MPECoder(){
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

	public void writePre( boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_AMOUNT.FULL_CLASS ){
			if( withComments ) out.println( "/**\n  Import statements for il1 classes.\n*/" );
			out.println( "import edu.ucla.belief.*;\nimport edu.ucla.belief.inference.*;\nimport edu.ucla.belief.inference.map.*;\nimport edu.ucla.belief.io.NetworkIO;\nimport edu.ucla.util.*;\n" );

			if( withComments ) out.println( "/**\n  Import statements for standard Java classes.\n*/" );
			out.println( "import java.util.*;\n" );

			if( withComments ) out.println( CPTCoder.createClassJavadocComment( this, "This class demonstrates code for an MPE query" ) );
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
		if( withComments ) out.println( "  /**\n    Demonstrates an MPE query.\n  */" );
		out.println( "  public void "+STR_METHOD_NAME+"( BeliefNetwork bn )\n  {" );

		MAPCoder.writeEvidence( myEvidence, withComments, out );
		out.println();

		writeMapEngineQuery( withComments, out );
		out.println();

		out.println( "    return;\n  }" );
	}

	public void writeMapEngineQuery( boolean withComments, PrintStream out )
	{
		//if( withComments ) out.println( "    /* Calculate MAP exactly. */\n" );

		if( withComments ) out.println( "    /* Create a new set of variables and remove those whose evidence have set keys. */" );
		out.println( "    Set allVarsMinusEvidence = new HashSet( bn );" );
		out.println( "    allVarsMinusEvidence.removeAll( evidence.keySet() );" );
		out.println();

		if( withComments ) out.println( "    /* Initialize a MapEngine and perform the mpe computation. */" );
		out.println( "    MapEngine mpe = new MapEngine( bn, allVarsMinusEvidence, evidence );" );
		out.println();

		if( withComments ) out.println( "    /* Get the results. */" );
		out.println( "    double score = mpe.probability();" );
		out.println( "    Map instantiation = mpe.getInstance();" );
		out.println();

		if( withComments ) out.println( "    /* Print the results. */" );
		out.println( "    System.out.println( \"MPE, P(MPE,e)= \" + score );" );
		out.println( "    VariableImpl.setStringifier( AbstractStringifier.VARIABLE_ID );" );
		out.println( "    System.out.println( \"\\t instantiation: \" + instantiation );" );
	}

	private Map myEvidence;
	private String myPathInputFile;
}
