package edu.ucla.util.code;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.Dynamator;
import edu.ucla.belief.InferenceEngine;

import java.io.*;
import java.util.Iterator;

/** @author Keith Cascio
	@since 053105 */
public class EMCoder extends AbstractCodeGenius implements CodeGenius
{
	public static final String STR_NAME_OUTPUT_CLASS = "EMTutorial";
	public static final String STR_METHOD_NAME = "doEM";
	public static final OptionBreadth OPTION_AMOUNT = new OptionBreadth( STR_METHOD_NAME );
	public static final CodeOption OPTION_WITH_COMMENTS = OptionWithComments.getInstance();
	private CodeOption[] myOptions;

	public String getIconFileName(){
		return "EM16.gif";
	}

	public String describe(){
		return "Java code to perform expectation maximization (EM) learning.";
	}

	public String getShortDescription(){
		return "em, " + getOption( OPTION_AMOUNT ) + ", " + OPTION_WITH_COMMENTS.describe( getFlag( OPTION_WITH_COMMENTS ) );
	}

	public String describeDependencies(){
		return "EM tool settings";
	}

	public void describeDependencies( Tree tree )
	{
		tree.addChildOfRootNode( "EM tool settings" );
		tree.addChildOfLastNode( "input data file" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "log-likelihood threshold" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "bound maximum iterations" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "bias to prevent divide by zero?" );
		tree.addChildOfRootNode( "inference algorithm" );
		tree.addChildOfLastNode( "compile settings" );
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

	public EMCoder(){
		super();
	}

	/** @since 20060327 */
	public OptionBreadth getOptionBreadth(){
		return OPTION_AMOUNT;
	}

	public void set( double threshold, int maxIterations, boolean withBias ){
		this.myThreshold = threshold;
		this.myMaxIterations = maxIterations;
		this.myFlagWithBias = withBias;
	}

	public void setDynamator( Dynamator dyn ){
		myDynamator = dyn;
	}

	public void setInputNetwork( BeliefNetwork bn, String path ){
		myBeliefNetwork = bn;
		myPathInputNetworkFile = path;
	}

	public void setPathDataFile( String path ){
		myPathDataFile = path;
	}

	public void setPathOutputNetworkFile( String path ){
		myPathOutputNetworkFile = path;
	}

	public void writePre( boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_AMOUNT.FULL_CLASS ){
			if( withComments ) out.println( "/** Import statements for il1 classes. */" );
			out.println( "import edu.ucla.belief.*;\nimport edu.ucla.belief.inference.*;\nimport edu.ucla.belief.learn.*;\nimport edu.ucla.belief.io.*;\n" );

			writeInferenceImports( withComments, out );
			out.println();

			if( withComments ) out.println( "/* Import statements for standard Java classes. */" );
			out.println( "import java.util.*;" );
			out.println( "import java.io.*;\n" );

			if( withComments ) out.println( CPTCoder.createClassJavadocComment( this, "This class demonstrates code to perform expectation maximization learning." ) );
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
			ProbabilityQueryCoder.writeReadNetworkFile( myPathInputNetworkFile, withComments, out );
			out.println( "}" );
		}
	}

	public void writeQuery( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "  /**\n    Demonstrates EM.\n  */" );
		out.println( "  public void "+STR_METHOD_NAME+"( BeliefNetwork bn )\n  {" );

		//MAPCoder.writeEvidence( myEvidence, withComments, out );//out.println();

		writeEM( withComments, out );
		out.println();

		out.println( "    return;\n  }" );
	}

	public void writeInferenceImports( boolean withComments, PrintStream out )
	{
		if( myDynamator == null ) return;
		ProbabilityQueryCoder.writeInferenceImports( myDynamator, withComments, out );
	}

	public void writeEM( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    /* Locate data file. */" );
		out.println( "    File fileData = new File( \""+myPathDataFile.replaceAll( "\\\\", "\\\\\\\\" )+"\" );" );
		out.println( "    if( !fileData.exists() ){" );
		out.println( "        System.err.println( fileData.getAbsolutePath() + \" does not exist.\" );" );
		out.println( "        return;" );
		out.println( "    }" );
		out.println();

		if( withComments ) out.println( "    /* Attempt to open the file and read the data. */" );
		out.println( "    LearningData data = null;" );
		out.println( "    try{" );
		out.println( "        data = new LearningData();" );
		out.println( "        data.readData( fileData, bn );" );
		out.println( "    }catch( Exception exception ){" );
		out.println( "        System.err.println( \"Data error in \" + fileData.getAbsolutePath() + \".\" );" );
		out.println( "        return;" );
		out.println( "    }" );
		out.println();

		ProbabilityQueryCoder.writeDynamatorCreation( myDynamator, (InferenceEngine)null, myBeliefNetwork, withComments, out );

		if( withComments ) out.println( "    /* Learn. */" );
		out.println( "    BeliefNetwork learned =" );
		out.println( "        Learning.learnParamsEM( bn, data, (double)"+Double.toString(myThreshold)+", (int)"+Integer.toString(myMaxIterations)+", dynamator, "+Boolean.toString(myFlagWithBias)+" );" );
		out.println();

		if( withComments ) out.println( "    /* Save results. */" );
		out.println( "    File oldFile = new File( \""+myPathInputNetworkFile.replaceAll( "\\\\", "\\\\\\\\" )+"\" );" );
		out.println( "    String pathOutputFile = \""+myPathOutputNetworkFile.replaceAll( "\\\\", "\\\\\\\\" )+"\";" );
		out.println( "    File newFile = new File( pathOutputFile );" );
		//out.println( "    FileType typeExplicit = FileType.getTypeForFile( newFile );" );
		out.println( "    try{" );
		out.println( "      NetworkIO.saveFileAs( learned, newFile, oldFile );" );
		out.println( "      System.out.println( \"Wrote \" + pathOutputFile );" );
		out.println( "    }catch( IOException ioe ){" );
		out.println( "      System.err.println( \"Failed to save \" + pathOutputFile );" );
		out.println( "    }" );
	}

	private BeliefNetwork myBeliefNetwork;
	private Dynamator myDynamator;
	private String myPathInputNetworkFile, myPathDataFile, myPathOutputNetworkFile;
	private double myThreshold;
	private int myMaxIterations;
	private boolean myFlagWithBias;
}
