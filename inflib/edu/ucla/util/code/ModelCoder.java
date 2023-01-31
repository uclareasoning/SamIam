package edu.ucla.util.code;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.FiniteVariableImpl;
import edu.ucla.belief.TableIndex;
import edu.ucla.belief.io.dsl.DSLNodeType;

import il2.bridge.*;
import il2.model.*;
import il2.util.*;

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.util.Date;

/** @since 20040506 */
public class ModelCoder extends AbstractCodeGenius implements CodeGenius
{
	public static final String STR_NAME_OUTPUT_CLASS = "ModelTutorial",
	                           STR_NAME_METHOD       = "createModel",
	                           STR_WARNING_NOISY_OR  = "This network contains noisy-or nodes that will be encoded as fully expanded conditional probability tables.";

	public static final int    INT_CEILING_CPT_SIZE  =  (int) 2049;
	public static final long   LONG_CEILING_CPT_SIZE = (long) 2049;

	public static final class LibraryVersionOption extends AbstractCodeOption
	{
		public String describe(){
			return "Library version";
		}
		public String getHelpText(){
			return "Library version? - Our inference and modeling library (inflib.jar) contains two separate sets of classes that you can use to represent and query a Bayesian network. We call them: { IL1, IL2 }.  IL1 uses Java Objects to represent variables, whereas IL2 uses integers.  There are also many other differences.  If you would like to compare the two representations, select '"+BOTH.toString()+"'.";
		}
		public CodeOptionValue getDefault(){
			return IL2;
		}
		public CodeOptionValue[] getValues(){
			return ARRAY_VALUES;
		}

		//public void writeCode( CodeOptionValue breadth, boolean flagWithComments, PrintStream out ){}

		public final CodeOptionValue   IL1          = new CodeOptionValue( "edu.ucla.belief classes (il1)" ),
		                               IL2          = new CodeOptionValue( "il2.model classes (il2)" ),
		                               BOTH         = new CodeOptionValue( "both" );
		public final CodeOptionValue[] ARRAY_VALUES = new CodeOptionValue[]{ IL1, IL2, BOTH };
	};
	public   static  final  LibraryVersionOption  OPTION_LIBRARY_VERSION  =  new LibraryVersionOption();
	public   static  final  OptionBreadth         OPTION_AMOUNT           =  new OptionBreadth( STR_NAME_METHOD );
	public   static  final  CodeOption            OPTION_WITH_COMMENTS    =  OptionWithComments .getInstance();
	private                 CodeOption[]          myOptions;
	private  static  final  DateFormat            DATEFORMAT_DT           =  DateFormat .getDateTimeInstance();

	/** @since 022305 */
	public String getIconFileName(){
		return "Display16.gif";
	}

	public String describe(){
		return "Java code to build "+myPathNetwork+" programatically.";
	}

	public String getShortDescription(){
		return "model, using " + getOption( OPTION_LIBRARY_VERSION ) + ", " + getOption( OPTION_AMOUNT ) + ", " + OPTION_WITH_COMMENTS.describe( getFlag( OPTION_WITH_COMMENTS ) );
	}

	public String describeDependencies(){
		return "(1) network structure\n(2) conditional probability tables (CPTs)";
	}

	public void describeDependencies( Tree tree )
	{
		tree.addChildOfRootNode( "network structure" );
		tree.addChildOfLastNode( "variable IDs" );
		tree.lastNodeGetsParentLastNode();
		tree.addChildOfLastNode( "connectivity" );
		tree.addChildOfRootNode( "conditional probability tables (CPTs)" );
	}

	public String getOutputClassNameDefault(){
		return STR_NAME_OUTPUT_CLASS;
	}

	public void writeCode( PrintStream out )
	{
		CodeOptionValue version = getOption( OPTION_LIBRARY_VERSION );
		CodeOptionValue breadth = getOption( OPTION_AMOUNT );
		boolean flagWithComments = getFlag( OPTION_WITH_COMMENTS );

		writePre( myPathNetwork, flagWithComments, version, breadth, out );

		if( version == OPTION_LIBRARY_VERSION.IL1 || version == OPTION_LIBRARY_VERSION.BOTH ) modelToJavaCodeIL1( getBeliefNetwork(), flagWithComments, out );
		if( version == OPTION_LIBRARY_VERSION.BOTH ) out.println();
		if( version == OPTION_LIBRARY_VERSION.IL2 || version == OPTION_LIBRARY_VERSION.BOTH ) modelToJavaCodeIL2( getBayesianNetwork(), flagWithComments, out );

		writePost( breadth, out );
	}

	public CodeOption[] getOptions(){
		if( myOptions == null ) myOptions = new CodeOption[]{ OPTION_WITH_COMMENTS, OPTION_LIBRARY_VERSION, OPTION_AMOUNT };
		return myOptions;
	}

	public ModelCoder( BeliefNetwork bn, String strPathNetwork ){
		super();
		this.myBeliefNetwork = bn;
		this.myPathNetwork = strPathNetwork;
	}

	public ModelCoder( BayesianNetwork bn, String strPathNetwork ){
		super();
		this.myBayesianNetwork = bn;
		this.myPathNetwork = strPathNetwork;
	}

	/** @since 20060327 */
	public OptionBreadth getOptionBreadth(){
		return OPTION_AMOUNT;
	}

	public BeliefNetwork getBeliefNetwork()
	{
		if( myBeliefNetwork != null ) return myBeliefNetwork;
		//else if( myBayesianNetwork != null ){
		//	return getConverter().convert( myBayesianNetwork );
		//}
		else return (BeliefNetwork)null;
	}

	public BayesianNetwork getBayesianNetwork()
	{
		if( myBayesianNetwork != null ) return myBayesianNetwork;
		else if( myBeliefNetwork != null ){
			return getConverter().convert( myBeliefNetwork );
		}
		else return (BayesianNetwork)null;
	}

	public Converter getConverter(){
		if( myConverter == null ) myConverter = new Converter();
		return myConverter;
	}

	public void setConverter( Converter converter ){
		this.myConverter = converter;
	}

	public void writePre( String strPathNetwork, boolean withComments, CodeOptionValue version, CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_AMOUNT.FULL_CLASS ){
			if( version == OPTION_LIBRARY_VERSION.IL1 || version == OPTION_LIBRARY_VERSION.BOTH ){
				if( withComments ) out.println( "/** Import statements necessary for il1 classes. */" );
				out.println( "import edu.ucla.belief.*;\n\n" );
			}

			if( version == OPTION_LIBRARY_VERSION.IL2 || version == OPTION_LIBRARY_VERSION.BOTH ){
				if( withComments ) out.println( "/** Import statements necessary for il2 classes. */" );
				out.println( "import il2.model.*;\nimport il2.model.Table;\nimport il2.util.*;\n\n" );
			}

			if( withComments ) out.println( CPTCoder.createClassJavadocComment( this, "This class hard codes the network\n  "+strPathNetwork ) );
			out.println( "public class "+getOutputClassName()+"\n{" );
			if( withComments ) out.println( "  /** Test. */" );
			out.println( "  public static void main( String[] args ){\n    "+getOutputClassName()+" T = new "+getOutputClassName()+"();" );
			if( version == OPTION_LIBRARY_VERSION.IL1 || version == OPTION_LIBRARY_VERSION.BOTH ) out.println( "    T.createBeliefNetwork();" );
			if( version == OPTION_LIBRARY_VERSION.IL2 || version == OPTION_LIBRARY_VERSION.BOTH ) out.println( "    T.createBayesianNetwork();" );
			out.println( "  }\n" );
		}
	}

	public void writePost( CodeOptionValue breadth, PrintStream out )
	{
		if( breadth == OPTION_AMOUNT.FULL_CLASS ) out.println( "}" );
	}

	/** @since 20051107 */
	public Object getWarnings(){
		BeliefNetwork bn = ModelCoder.this.getBeliefNetwork();
		if( bn == null ) return null;
		if( FiniteVariableImpl.thereExists( bn, DSLNodeType.NOISY_OR ) != null ){ return STR_WARNING_NOISY_OR; }
		return null;
	}

	public void toJavaCodeIL1( BeliefNetwork bn, String strPathNetwork, boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		writePre( strPathNetwork, withComments, OPTION_LIBRARY_VERSION.IL1, breadth, out );
		modelToJavaCodeIL1( bn, withComments, out );
		writePost( breadth, out );
	}

	/** @since 20080116 */
	public static String[] indicesAsStrings( int count, int radix, char left, String prefix ){
		String[]     ret   = new String[ count ];
		int          width = Integer.toString( count, radix ).length();
		String       raw;
		StringBuffer buff  = new StringBuffer( width );
		for( int i=0; i<count; i++ ){
			buff.setLength(0);
			raw = Integer.toString( i, radix );
			if( prefix != null ){ buff.append( prefix ); }
			for( int j  = width - raw.length(); j > 0; --j ){ buff.append( left ); }
			ret[i] = buff.append( raw ).toString();
		}
		return ret;
	}

	public void modelToJavaCodeIL1( BeliefNetwork bn, boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "  /**\n    Builds a new model from scratch, as simply as possible, using classes in package edu.ucla.belief (il1).\n  */" );
		out.println( "  public BeliefNetwork createBeliefNetwork()\n  {" );

		if( withComments ) out.println( "    /* Contruct an empty BeliefNetwork. */" );
		out.println( "    BeliefNetwork model = new BeliefNetworkImpl();\n" );

		List     topological              = bn.topologicalOrder();
		Map      mapVariablesToCodeTokens = new HashMap(      bn.size() );
		String[] stris                    = indicesAsStrings( bn.size(), 10, '0', null );

		int               i =    0;
		String         stri = null, strID = null;
		FiniteVariable fVar = null;
		for( Iterator it = topological.iterator(); it.hasNext(); ++i )
		{
			stri  = stris[i];
			fVar  = (FiniteVariable) it.next();
			strID = fVar.getID();
			if( withComments ){ out.print( "    /* Setup a discrete variable called \""+strID+"\",\n       with states " );
			                    printListOfStrings( fVar.instances(), out );
			                    out.println( ". */" ); }
			out.println( "    String              id"+stri+" = \""+strID+"\";" );
			out.print(   "    String[]        values"+stri+" = new String[]{ " );
			printListOfStrings( fVar.instances(), out );
			out.println( " };" );
			String codeToken = "var"+stri;
			mapVariablesToCodeTokens.put( fVar, codeToken );
			out.println( "    FiniteVariable     "+codeToken+" = new FiniteVariableImpl( id"+stri+", values"+stri+" );" );
			//out.println( "    model.addVariable( var"+stri+" );" );
			out.print( "    model.addVariable( var"+stri+", true );" );
			if( withComments ) out.println( "//pass second argument true, to construct a default CPTShell (TableShell) for var"+stri );
			else out.println();
			//out.print( "    model.addVariable( var"+stri+", false );" );
			//if( withComments ) out.println( "//pass second argument false, to avoid constructing a default cpt for var"+stri );
			//else out.println();
			out.println();
		}

		i = 0;
		Set            incoming;
		String         codeTokenSource, codeTokenSink;
		FiniteVariable sink;
		TableIndex     index;
		int            count_parents;
		for( Iterator      itTopo = topological.iterator(); itTopo.hasNext(); ){
			if( !       (incoming = bn.inComing( sink = (FiniteVariable) itTopo.next() )).isEmpty() ){
				codeTokenSink     = mapVariablesToCodeTokens.get( sink ).toString();
				if( withComments ){ out.println( "    /* Add edges stopping at "+codeTokenSink+" (\""+sink.getID()+"\") */" ); }
				index             = sink.getCPTShell().index();
				count_parents     = index.getNumVariables() - 1;
				for( int      par = 0; par < count_parents; par++ ){
				  codeTokenSource = mapVariablesToCodeTokens.get( index.variable( par ) ).toString();
				  out.println( "    model.addEdge( "+codeTokenSource+", "+codeTokenSink+" );" );
				}
				out.println();
			}
			++i;
		}

		if( withComments ) out.println( "    /* For the cpts, create arrays of double-precision floating point values. */" );

		i=0;
		for( Iterator itTopo = topological.iterator(); itTopo.hasNext(); )
		{
			stri = stris[i];
			fVar = (FiniteVariable) itTopo.next();
			edu.ucla.belief.CPTShell cptshell = fVar.getCPTShell();// edu.ucla.belief.io.dsl.DSLNodeType.CPT );
			String errmsg = null;
			if( cptshell == null ) errmsg = "no CPTShell";
			else{
				int size = cptshell.index().size();
				if( size > INT_CEILING_CPT_SIZE ) errmsg = ("cpt size " + size + " too large, > ceiling [" + INT_CEILING_CPT_SIZE + "]");
			}
			if( errmsg != null ) throw new IllegalStateException( "variable '"+fVar.getID()+"', " + errmsg );
			edu.ucla.belief.Table il1Table = cptshell.getCPT();
			if( withComments ) out.println( il1Table.tableString( "    //" ) );
			out.print( "    double[] cpt"+stri+" = new double[]{ " );
			printDoubleArray( il1Table.dataclone(), out );
			out.println( " };" );
			++i;
		}

		out.println();
		if( withComments ){ out.println( "    /* Specify parameters as full conditional probability tables. */" ); }
		out.println( "    edu.ucla.belief.io.dsl.DSLNodeType cpt_type = edu.ucla.belief.io.dsl.DSLNodeType.CPT;\n" );
		if( withComments ){ out.println( "    /* Set the cpt data for each variable. */" ); }

		i=0;
		for( Iterator itTopo = topological.iterator(); itTopo.hasNext(); )
		{
			stri = stris[i];
			fVar = (FiniteVariable) itTopo.next();
			codeTokenSource = mapVariablesToCodeTokens.get( fVar ).toString();
			out.println( "    TableShell      tShell"+stri+" = (TableShell) " + codeTokenSource + ".getCPTShell( cpt_type );" );
			out.println( "    tShell"+stri+".setValues( cpt"+stri+" );" );
			++i;
		}

		out.println();
		if( withComments ) out.println( "    /* Done. */" );
		out.println( "    return model;\n  }" );
	}

	public void toJavaCodeIL2( BayesianNetwork bn, String strPathNetwork, boolean withComments, CodeOptionValue breadth, PrintStream out )
	{
		writePre( strPathNetwork, withComments, OPTION_LIBRARY_VERSION.IL2, breadth, out );
		modelToJavaCodeIL2( bn, withComments, out );
		writePost( breadth, out );
	}

	public void modelToJavaCodeIL2( BayesianNetwork bn, boolean withComments, PrintStream out )
	{
		if( withComments ) out.println( "  /**\n    Builds a new model from scratch, as simply as possible, using classes in package il2.model (il2).\n  */" );
		out.println( "  public BayesianNetwork createBayesianNetwork()\n  {" );

		Domain domain = bn.domain();
		if( withComments ) out.println( "    /* Create a domain of size "+domain.size()+". */" );
		out.println( "    Domain domain = new Domain("+domain.size()+");\n" );

		Table[]  cpts                      = bn.cpts();
		Map      mapIndicesToVariableNames = new HashMap(      cpts.length );
		String[] stris                     = indicesAsStrings( cpts.length, 10, '0', null );
		String   stri;
		for( int i=0; i<cpts.length; i++ ){
			stri = stris[i];
			Table table = cpts[i];
			IntSet vars = table.vars();
			int indexVar = vars.get( vars.size()-1 );
			String strName = domain.name( indexVar );
			long size = table.sizeLong();
			if( size > LONG_CEILING_CPT_SIZE ){
				String errmsg = errmsg = ("cpt size " + size + " too large, > ceiling [" + LONG_CEILING_CPT_SIZE + "]");
				throw new IllegalStateException( "variable '"+strName+"', " + errmsg );
			}

			if( withComments ) out.print( "    /* Add a discrete variable called \""+strName+"\" to the domain,\n       with states " );
			if( withComments ) printInstanceNames( domain, indexVar, out );
			if( withComments ) out.println( ". */" );
			out.println( "    String     name"+stri+" = \""+strName+"\";" );
			out.print(   "    String[] values"+stri+" = new String[]{ " );
			printInstanceNames( domain, indexVar, out );
			out.println( " };" );
			String strVariableName = "id"+stri;
			out.println( "    int          "+strVariableName+" = domain.addDim( name"+stri+", values"+stri+" );" );
			mapIndicesToVariableNames.put( new Integer(indexVar), strVariableName );
			out.println();
		}

		if( withComments ) out.println( "    /* For the cpts, create arrays of double-precision floating point values. */" );

		for( int i=0; i<cpts.length; i++ ){
			stri = stris[i];
			Table table = cpts[i];
			if( withComments ){
				edu.ucla.belief.Table il1Table = myConverter.convert( table );
				//out.println( "    /*\n" +il1Table.tableString( "    " )+ "\n    */" );
				out.println( il1Table.tableString( "    //" ) );
			}
			out.print( "    double[] cpt"+stri+" = new double[]{ " );
			printDoubleArray( table.values(), out );
			out.println( " };" );
		}

		out.println();
		if( withComments ) out.println( "    /*\n      Create a IL2 Table for each cpt.\n      The parameters to the Table constructor are:\n      (1) the domain,\n      (2) the variable ids that name the dimensions of the table (in the form of an IntSet),\n      (3) the cpt data.\n    */" );

		for( int i=0; i<cpts.length; i++ ){
			stri = stris[i];
			Table table = cpts[i];
			out.print( "    Table table"+stri+" = new Table( domain, new IntSet( new int[]{ " );
			printIntSetValues( table.vars(), mapIndicesToVariableNames, out );
			out.println( " } ), cpt"+stri+" );" );
		}

		if( withComments ) out.print( "\n    /* Create an array of all the Tables. */" );
		out.print( "\n    " );
		out.print( "Table[] tables = new Table[]{ " );
		printRepeat( "table", cpts.length, stris, out );
		out.println( " }; " );

		out.println();
		if( withComments ) out.println( "    /*\n      The simple BayesianNetwork constructor takes only one argument:\n      an array of Tables.\n    */" );
		out.println( "    BayesianNetwork model = new BayesianNetwork( tables );" );
		out.println( "    return model;\n  }" );
	}

	public static void printListOfStrings( List list, PrintStream out )
	{
		StringBuffer buffer = new StringBuffer( list.size()*20 );
		for( Iterator it = list.iterator(); it.hasNext(); ){
			buffer.append( "\"" );
			buffer.append( it.next().toString() );
			buffer.append( "\", " );
		}
		int lenBuffer = buffer.length();
		int indexEnd = ( lenBuffer > 2 ) ? lenBuffer-2 : lenBuffer;
		out.print( buffer.substring( 0, indexEnd ) );
	}

	public static void printInstanceNames( Domain domain, int indexVar, PrintStream out )
	{
		int sizeVar = domain.size( indexVar );
		int lastIndex = sizeVar-1;
		for( int i=0; i<lastIndex; i++ ){
			out.print( "\"" + domain.instanceName( indexVar, i ) + "\", " );
		}
		out.print( "\"" + domain.instanceName( indexVar, lastIndex ) + "\"" );
	}

	public static void printDoubleArray( double[] values, PrintStream out )
	{
		int length = values.length;
		int lastIndex = length-1;
		for( int i=0; i<lastIndex; i++ ){
			out.print( values[i] + ", " );
		}
		out.print( values[lastIndex] );
	}

	public static void printIntSetValues( IntSet set, Map mapIndicesToVariableNames, PrintStream out )
	{
		int length = set.size();
		int lastIndex = length-1;
		for( int i=0; i<lastIndex; i++ ){
			out.print( mapIndicesToVariableNames.get( new Integer( set.get(i) ) ) + ", " );
		}
		out.print( mapIndicesToVariableNames.get( new Integer( set.get(lastIndex) ) ) );
	}

	public static void printRepeat( String toRepeat, int num, String[] stris, PrintStream out )
	{
		int lastIndex = num-1;
		String stri;
		for( int i=0; i<lastIndex; i++ ){
			stri = stris == null ? Integer.toString(i) : stris[i];
			out.print( toRepeat + stri + ", " );
		}
		out.print( toRepeat+lastIndex );
	}

	public static String makeDate(){
		return DATEFORMAT_DT.format( new Date( System.currentTimeMillis() ) );
	}

	private String myPathNetwork;
	private BayesianNetwork myBayesianNetwork;
	private BeliefNetwork myBeliefNetwork;
	private Converter myConverter;
}
