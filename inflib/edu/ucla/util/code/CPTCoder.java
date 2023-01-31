package edu.ucla.util.code;

//import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.io.dsl.DSLNodeType;

import java.io.*;
import java.util.*;

/** @author Keith Cascio
	@since 032205 */
public class CPTCoder extends AbstractCodeGenius implements CodeGenius
{
	public static final String STR_NAME_OUTPUT_CLASS = "CPTTutorial";
	public static final String STR_METHOD_NAME = "doCPTDemo";
	public static final String STR_METHOD_FOUNDATION = "createModel";
	public static final OptionBreadth OPTION_AMOUNT = new OptionBreadth( STR_METHOD_NAME );
	public static final CodeOption OPTION_WITH_COMMENTS = OptionWithComments.getInstance();
	private CodeOption[] myOptions;

	public String getIconFileName(){
		return "CPT16.gif";
	}

	public String describe(){
		return "Java code demonstrates how to create a CPT and edit its parameters dynamically.";
	}

	public String getShortDescription(){
		return "cpt demo, " + getOption( OPTION_AMOUNT ) + ", " + OPTION_WITH_COMMENTS.describe( getFlag( OPTION_WITH_COMMENTS ) );
	}

	public String describeDependencies(){
		return "no deps";
	}

	public void describeDependencies( Tree tree ){
		//tree.addChildOfRootNode( "no deps" );
	}

	public String getOutputClassNameDefault(){
		return STR_NAME_OUTPUT_CLASS;
	}

	public void writeCode( PrintStream out )
	{
		CodeOptionValue breadth = getOption( OPTION_AMOUNT );
		boolean flagWithComments = getFlag( OPTION_WITH_COMMENTS );

		writePre( flagWithComments, breadth, out );
		writeDemo( flagWithComments, out );
		writePost( flagWithComments, breadth, out );
	}

	public CodeOption[] getOptions(){
		if( myOptions == null ) myOptions = new CodeOption[] { OPTION_WITH_COMMENTS, OPTION_AMOUNT };
		return myOptions;
	}

	public CPTCoder(){
		super();
	}

	/** @since 20060327 */
	public OptionBreadth getOptionBreadth(){
		return OPTION_AMOUNT;
	}

	public static String createCompilationComment( CodeGenius genius ){
		StringBuffer buffer = new StringBuffer( 256 );
		buffer.append( "  To compile this class, make sure\n" );
		buffer.append( "  inflib.jar occurs in the command line classpath,\n" );
		buffer.append( "  e.g. javac -classpath inflib.jar " );
		buffer.append( genius.getOutputClassName() );
		buffer.append( ".java\n\n" );
		buffer.append( "  To run it, do the same,\n" );
		buffer.append( "  but also include the path to\n" );
		buffer.append( "  the compiled class,\n" );
		buffer.append( "  e.g. java -classpath ." );
		buffer.append( File.pathSeparatorChar );
		buffer.append( "inflib.jar " );
		buffer.append( genius.getOutputClassName() );
		return buffer.toString();
	}

	public static String createStandardComments( CodeGenius genius ){
		return createCompilationComment(genius)+"\n\n  @author Keith Cascio\n  @since "+ModelCoder.makeDate();
	}

	public static String createClassJavadocComment( CodeGenius genius, String descrip ){
		return "/**\n  "+descrip+"\n\n"+createStandardComments(genius)+"\n*/";
	}

	public void writePre( boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_AMOUNT.FULL_CLASS ){
			if( withComments ) out.println( "/** Import statements for il1 classes. */" );
			out.println( "import edu.ucla.belief.*;\nimport edu.ucla.belief.io.dsl.DSLNodeType;" );
			out.println();

			if( withComments ) out.println( "/** Import statements for standard Java classes. */" );
			out.println( "import java.util.*;" );
			out.println();

			if( withComments ) out.println( createClassJavadocComment( this, "This class demonstrates code to create a CPT and edit its parameters" ) );
			out.println( "public class "+getOutputClassName()+"\n{" );
			if( withComments ) out.println( "  /** Test. */" );
			out.println( "  public static void main(String[] args){\n    "+getOutputClassName()+" T = new "+getOutputClassName()+"();" );
			out.println( "    T."+STR_METHOD_NAME+"();" );
			out.println( "  }\n" );
		}
	}

	public void writePost( boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_AMOUNT.FULL_CLASS ){
			//out.println();
			//ProbabilityQueryCoder.writeReadNetworkFile( myPathInputFile, withComments, out );
			out.println( "}" );
		}
	}

	public static String stepToString( int step ){
		return "(" + Integer.toString( step ) + ")";
	}

	public void writeDemo( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "  /** Create the model; create the new cpt; edit the new cpt. */" );
		out.println( "  public void "+STR_METHOD_NAME+"()\n  {" );

		out.println( "    BeliefNetwork model = this."+STR_METHOD_FOUNDATION+"();" );
		out.println( "    this.createCPT( model );" );
		out.println( "    this.editParameters( model );" );
		out.println();

		out.println( "    return;\n  }" );
		out.println();

		writeEditParameters( withComments, out );
		out.println();

		writeCreateCPT( withComments, out );
		out.println();

		writeFoundationNetwork( withComments, out );
	}

	private void writeEditParameters( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "  /** Demonstrates how to edit parameters. */" );
		out.println( "  public void editParameters( BeliefNetwork model )\n  {" );
		out.println( "    System.out.println( \"Editing CPT...\" );" );
		out.println();

		int step = (int)1;

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Retrieve child variable" );
		}
		out.println( "    FiniteVariable newChild = (FiniteVariable) model.forID( \"newchild\" );" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Get the CPTShell (TableShell)" );
		}
		out.println( "    TableShell shell = (TableShell) newChild.getCPTShell( DSLNodeType.CPT );" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Get the TableIndex" );
		}
		out.println( "    TableIndex index = shell.index();" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Get the Table" );
		}
		out.println( "    Table table = shell.getCPT();" );
		out.println();

		writeRetrieveParents( step++, withComments, out );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Create an int[] to" );
			out.println( "    //hold the multi-dimensional indices" );
		}
		out.println( "    int mindex[] = new int[ index.getNumVariables() ];" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Choose the desired condition" );
			out.println( "    //{ parent00 = false, parent01 = true, parent02 = false, parent03 = false, parent04 = false }" );
		}
		out.println( "    mindex[ index.variableIndex( parent00 ) ] = parent00.index( \"false\" );" );
		out.println( "    mindex[ index.variableIndex( parent01 ) ] = parent01.index( \"true\" );" );
		out.println( "    mindex[ index.variableIndex( parent02 ) ] = parent02.index( \"false\" );" );
		out.println( "    mindex[ index.variableIndex( parent03 ) ] = parent03.index( \"false\" );" );
		out.println( "    mindex[ index.variableIndex( parent04 ) ] = parent04.index( \"false\" );" );
		out.println( "    mindex[ index.variableIndex( newChild ) ] = 0;" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Use TableIndex to calculate" );
			out.println( "    //the first linear index for the desired condition" );
		}
		out.println( "    int linear = index.index( mindex );" );
		out.println( "    System.out.println( \"Linear index? \" + linear );" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" For the purposes of demonstation," );
			out.println( "    //set this condition deterministically for value \"medium\"" );
		}
		out.println( "    double zero = (double)0.0;" );
		out.println( "    double one  = (double)1.0;" );
		out.println( "    table.setCP( linear + newChild.index( \"low\" ), zero );" );
		out.println( "    table.setCP( linear + newChild.index( \"medium\" ), one );" );
		out.println( "    table.setCP( linear + newChild.index( \"high\" ), zero );" );
		out.println();

		if( withComments ){
			out.println( "    //Print out information about the edited cpt." );
		}
		out.println( "    System.out.println( \"Edited cpt has min value: \" + table.min() );" );
		out.println( "    System.out.println( \"               max value: \" + table.max() );" );
		out.println( "    System.out.println( \"               entropy:   \" + table.entropy() );" );

		out.println( "    return;\n  }" );
	}

	private void writeRetrieveParents( int step, boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "    //"+stepToString(step)+" Retrieve parent variables" );
		writeRetrieveVariable( "parent00", out );
		writeRetrieveVariable( "parent01", out );
		writeRetrieveVariable( "parent02", out );
		writeRetrieveVariable( "parent03", out );
		writeRetrieveVariable( "parent04", out );
	}

	private void writeRetrieveVariable( String id, PrintStream out ){
		out.println( "    FiniteVariable "+id+" = (FiniteVariable) model.forID( \""+id+"\" );" );
	}

	private void writeCreateCPT( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "  /** Demonstrates how to create a CPT. */" );
		out.println( "  public void createCPT( BeliefNetwork model )\n  {" );

		int step = (int)1;

		if( withComments ) out.println( "    //"+stepToString(step++)+" Create child variable with id \"newchild\"" );
		out.println( "    String[] values = new String[] { \"low\", \"medium\", \"high\" };" );
		out.println( "    FiniteVariable newChild = new FiniteVariableImpl( \"newchild\", values );" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Add newChild to the model," );
			out.println( "    //but pass second argument false" );
			out.println( "    //because we will create the cpt ourselves" );
		}
		out.println( "    model.addVariable( newChild, false );" );
		out.println();

		writeRetrieveParents( step++, withComments, out );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Add edges," );
			out.println( "    //but pass third argument false" );
			out.println( "    //because we will create the cpt ourselves" );
		}
		out.println( "    model.addEdge( parent00, newChild, false );" );
		out.println( "    model.addEdge( parent01, newChild, false );" );
		out.println( "    model.addEdge( parent02, newChild, false );" );
		out.println( "    model.addEdge( parent03, newChild, false );" );
		out.println( "    model.addEdge( parent04, newChild, false );" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Define a List" );
			out.println( "    //containing the parents (in order!)," );
			out.println( "    //and finally the child" );
		}
		out.println( "    LinkedList variables = new LinkedList();" );
		out.println( "    variables.add( parent00 );" );
		out.println( "    variables.add( parent01 );" );
		out.println( "    variables.add( parent02 );" );
		out.println( "    variables.add( parent03 );" );
		out.println( "    variables.add( parent04 );" );
		out.print  ( "    variables.add( newChild );" );
		out.println( withComments ? "//add child last" : "" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Create a TableIndex" );
			out.println( "    //TableIndex is a very useful class that" );
			out.println( "    //helps you calculate indices into the cpt." );
		}
		out.println( "    TableIndex index = new TableIndex( variables );" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Create an array to hold the parameters," );
			out.println( "    //and fill it with a uniform distribution" );
		}
		out.println( "    double[] data = new double[ index.size() ];" );
		out.println( "    double uniformValue = ((double)1.0)/((double)newChild.size());" );
		out.println( "    Arrays.fill( data, uniformValue );" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Create a Table" );
			out.println( "    //In this demo, the Table _is_ the CPT - it represents the probability data" );
		}
		out.println( "    Table t = new Table( index, data );" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Create a TableShell" );
			out.println( "    //TableShell is the simplest implementation of" );
			out.println( "    //CPTShell: it is simply a wrapper around a Table" );
		}
		out.println( "    TableShell shell = new TableShell( t );" );
		out.println();

		if( withComments ){
			out.println( "    //"+stepToString(step++)+" Last, set \"shell\"" );
			out.println( "    //as the CPTShell for newChild." );
			out.println( "    //You may associate more than one" );
			out.println( "    //CPTShell with a FiniteVariable - " );
			out.println( "    //one for each of the "+Integer.toString( DSLNodeType.valuesAsArray().length )+" DSLNodeTypes." );
			out.println( "    //You should always associate a TableShell" );
			out.println( "    //with type CPT, as demonstrated here:" );
		}
		out.println( "    newChild.setCPTShell( DSLNodeType.CPT, shell );" );
		out.println();

		if( withComments ){
			out.println( "    //Print out information about the new cpt." );
		}
		out.println( "    System.out.println( \"Created a new cpt for variable \" + newChild.getID() );" );
		out.println( "    System.out.println( \"with \"+ model.inComing(newChild).size() +\" parents.\" );" );
		out.println( "    System.out.println( \"The new cpt has \" +shell.index().size()+ \" parameters.\" );" );
		out.println( "    System.out.println( \"The class of the shell is \\\"\" +shell.getClass().getName()+ \"\\\".\" );" );

		out.println( "    return;\n  }" );
	}

	public void writeFoundationNetwork( boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "  /** Creates a model with five unconnected variables. */" );
		out.println( "  public BeliefNetwork "+STR_METHOD_FOUNDATION+"()\n  {" );
		out.println( "    BeliefNetwork model = new BeliefNetworkImpl();\n" );

		out.println( "    String[] binary = new String[] { \"true\", \"false\" };\n" );

		out.println( "    model.addVariable( new FiniteVariableImpl( \"parent00\", binary ), true );" );
		out.println( "    model.addVariable( new FiniteVariableImpl( \"parent01\", binary ), true );" );
		out.println( "    model.addVariable( new FiniteVariableImpl( \"parent02\", binary ), true );" );
		out.println( "    model.addVariable( new FiniteVariableImpl( \"parent03\", binary ), true );" );
		out.println( "    model.addVariable( new FiniteVariableImpl( \"parent04\", binary ), true );" );
		out.println();

		out.println( "    return model;\n  }" );
	}
}
