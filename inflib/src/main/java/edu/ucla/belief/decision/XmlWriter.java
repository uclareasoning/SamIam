package edu.ucla.belief.decision;

import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.io.dsl.DSLNodeType;
import edu.ucla.belief.*;
import edu.ucla.util.AbstractStringifier;

import java.util.*;
import java.io.*;
import java.text.NumberFormat;

/** @author Keith Cascio
	@since 013105 */
public class XmlWriter
{
	static public void write( DecisionTree tree, PrintStream out ){
		new XmlWriter( out ).write( tree );
	}

	static public void write( DecisionTree tree, PrintWriter out ){
		new XmlWriter( out ).write( tree );
	}

	private XmlWriter( PrintStream out ){
		this();
		this.myUniWriter = new PrintStreamWriter( out );
	}

	private XmlWriter( PrintWriter out ){
		this();
		this.myUniWriter = new PrintWriterWriter( out );
	}

	private XmlWriter(){
		this.myWritten = new HashMap();
	}

	private void write( DecisionTree tree ){
		TableIndex index = tree.getIndex();
		String attributes = attr( STR_ATTR_JOINT, index.getJoint().getID() );
		attributes += attr( STR_ATTR_PARENTORDER, AbstractStringifier.VARIABLE_ID.collectionToString( index.variables() ) );
		if( tree instanceof DecisionTreeImpl ){
			attributes += attr( STR_ATTR_EPSILON, getFormatValue().format( ((DecisionTreeImpl)tree).recallOptimizationEpsilon() ) );
		}

		openTag( STR_ELEMENT_ROOT, attributes, INT_INDENT_BASE );
		writeDecision( tree.getRoot(), (List)null, INT_INDENT_BASE );
		closeTag( STR_ELEMENT_ROOT, INT_INDENT_BASE );
	}

	public static final char CHAR_TAB = ' ';//'\t'
	public static final int INT_INDENT_BASE = 0;
	public static final int INT_INDENT_DELTA = 2;

	public static final String STR_ELEMENT_ROOT = "decisiontree";
	public static final String STR_ELEMENT_NODE_GENERIC = "n";
	public static final String STR_ELEMENT_NODE_LEAF = "l";
	public static final String STR_ELEMENT_NODE_INTERNAL = "d";
	public static final String STR_ELEMENT_PARAMETER = "p";
	//public static final String STR_ELEMENT_OUTCOME = "o";

	public static final String STR_ATTR_SERIAL = "z";
	public static final String STR_ATTR_ID = "id";
	public static final String STR_ATTR_EDITABLE = "edit";
	public static final String STR_ATTR_INDEX = "i";
	public static final String STR_ATTR_JOINT = "joint";
	public static final String STR_ATTR_VARIABLE = "var";
	public static final String STR_ATTR_VALUE = "val";
	public static final String STR_ATTR_INSTANCELIST = "insts";
	public static final String STR_ATTR_PARENTORDER = "parentorder";
	public static final String STR_ATTR_EPSILON = "epsilon";

	private String attr( String name, String value ){
		return " " + name + "=\"" + value + "\"";
	}

	private void openTag( String name, String attributes, int indent ){
		//return "<" + name + " " + attributes + ">\n";
		myUniWriter.write( getTab(indent) );
		myUniWriter.write( "<" );
		myUniWriter.write( name );
		myUniWriter.write( attributes );
		myUniWriter.write( ">\n" );
	}

	private void closeTag( String name, int indent ){
		//return "</" + name + ">\n";
		myUniWriter.write( getTab(indent) );
		myUniWriter.write( "</" );
		myUniWriter.write( name );
		myUniWriter.write( ">\n" );
	}

	private void leafTag( String name, String attributes, int indent ){
		//return "<" + name + " " + attributes + " />\n";
		myUniWriter.write( getTab(indent) );
		myUniWriter.write( "<" );
		myUniWriter.write( name );
		myUniWriter.write( attributes );
		myUniWriter.write( " />\n" );
	}

	private void writeDecision( DecisionNode node, Collection instances, int indent ){
		if( isIDed( node ) ){
			leafTag( STR_ELEMENT_NODE_GENERIC, getOutcomeBasics( node, instances ), indent );
		}
		else{
			if( node.isLeaf() ) writeLeaf( (DecisionLeaf)node, instances, indent );
			else writeInternal( (DecisionInternal)node, instances, indent );
		}
	}

	private String getOutcomeBasics( Object outcome, Collection instances ){
		String ret = attr( STR_ATTR_SERIAL, getID(outcome) );
		if( (instances != null) && (!instances.isEmpty()) ) ret += attr( STR_ATTR_INSTANCELIST, AbstractStringifier.BASICOBJECT.collectionToString( instances ) );
		return ret;
	}

	private void writeLeaf( DecisionLeaf node, Collection instances, int indent ){
		openTag( STR_ELEMENT_NODE_LEAF, getDecisionBasics( node, instances ), indent );
		writeOutcomes( node, indent + INT_INDENT_DELTA );
		closeTag( STR_ELEMENT_NODE_LEAF, indent );
	}

	private void writeParameter( Parameter param, int index, int indent ){
		boolean flagIDed = isIDed( param );
		writeParameter( param, getParameterBasics( param, index ), flagIDed, indent );
	}

	private void writeParameter( Parameter param, Collection instances, int indent ){
		boolean flagIDed = isIDed( param );
		writeParameter( param, getOutcomeBasics( param, instances ), flagIDed, indent );
	}

	private void writeParameter( Parameter param, String basics, boolean flagIDed, int indent ){
		String attributes = basics;
		if( !flagIDed ){
			attributes = basics + attr( STR_ATTR_ID, param.getID() ) +
				attr( STR_ATTR_VALUE, getFormatValue().format( param.getValue() ) );
		}
		leafTag( STR_ELEMENT_PARAMETER, attributes, indent );
	}

	private void writeInternal( DecisionInternal node, Collection instances, int indent ){
		openTag( STR_ELEMENT_NODE_INTERNAL, getDecisionBasics( node, instances ) + attr( STR_ATTR_VARIABLE, node.getVariable().getID() ), indent );
		writeOutcomes( node, indent + INT_INDENT_DELTA );
		closeTag( STR_ELEMENT_NODE_INTERNAL, indent );
	}

	private void writeOutcomes( DecisionNode node, int indent ){
		if( node.isLeaf() ){
			if( myGroupedParameterWriter == null ) myGroupedParameterWriter = new GroupedParameterWriter();
			myGroupedParameterWriter.writeOutcomes( node, indent );
		}
		else{
			if( myGroupedDecisionWriter == null ) myGroupedDecisionWriter = new GroupedDecisionWriter();
			myGroupedDecisionWriter.writeOutcomes( node, indent );
		}
	}

	/** @since 020105 */
	public abstract class GroupedOutcomeWriter
	{
		abstract public Object getOutcome( DecisionNode node, int index ) throws StateNotFoundException;
		abstract public void writeOutcome( Object outcome, Collection instances, int indent );

		/** override here */
		public boolean shouldWriteFlat( DecisionNode node ){
			return false;
		}

		/** override here */
		public void writeOutcomesFlat( DecisionNode node, int indent ){
			writeOutcomesGrouped( node, indent );
		}

		final public void writeOutcomes( DecisionNode node, int indent ){
			if( shouldWriteFlat( node ) ) writeOutcomesFlat( node, indent );
			else writeOutcomesGrouped( node, indent );
		}

		final public void writeOutcomesGrouped( DecisionNode node, int indent )
		{
			FiniteVariable var = node.getVariable();
			int sizeVar = var.size();

			//if( myMapOutcomeToInstanceList == null ) myMapOutcomeToInstanceList = new HashMap( sizeVar );
			//else myMapOutcomeToInstanceList.clear();

			Map map = node.groupInstancesByOutcome( (Map)null );//Map map = new HashMap( sizeVar );//Map map = myMapOutcomeToInstanceList;
			Object outcome;
			Collection outcomes = map.keySet();//List outcomes = new LinkedList();
			//Object instance;
			Collection instances;
			/*
			try{
			for( int i=0; i<sizeVar; i++ ){
				outcome = GroupedOutcomeWriter.this.getOutcome( node, i );
				instance = var.instance( i );
				if( map.containsKey( outcome ) ) ((Collection)map.get( outcome )).add( instance );
				else{
					outcomes.add( outcome );
					(instances = new LinkedList()).add( instance );
					map.put( outcome, instances );
				}
			}
			}catch( edu.ucla.belief.StateNotFoundException statenotfoundexception ){
				System.err.println( "Warning: " + statenotfoundexception );
				return;
			}*/

			for( Iterator itOutcomes = outcomes.iterator(); itOutcomes.hasNext(); ){
				instances = (Collection) map.get( outcome = itOutcomes.next() );
				GroupedOutcomeWriter.this.writeOutcome( outcome, instances, indent );
			}
		}
	}

	/** @since 020105 */
	public class GroupedParameterWriter extends GroupedOutcomeWriter{
		public Object getOutcome( DecisionNode node, int index ) throws StateNotFoundException {
			return node.getParameter( index );
		}
		public void writeOutcome( Object outcome, Collection instances, int indent ){
			XmlWriter.this.writeParameter( (Parameter)outcome, instances, indent );
		}
		public boolean shouldWriteFlat( DecisionNode node ){
			float numInstances = (float) node.getVariable().size();
			float numOutcomes = (float) node.numOutcomes();
			return ( (numOutcomes / numInstances) > ((float)0.8) );
		}
		public void writeOutcomesFlat( DecisionNode node, int indent ){
			int sizeJoint = node.getVariable().size();
			for( int i=0; i<sizeJoint; i++ ){
				XmlWriter.this.writeParameter( node.getParameter(i), i, indent );
			}
		}
	}

	/** @since 020105 */
	public class GroupedDecisionWriter extends GroupedOutcomeWriter{
		public Object getOutcome( DecisionNode node, int index ) throws StateNotFoundException {
			return node.getNext( index );
		}
		public void writeOutcome( Object outcome, Collection instances, int indent ){
			XmlWriter.this.writeDecision( (DecisionNode)outcome, instances, indent );
		}
	}

	private void writeBasics( DecisionNode node ){
		myUniWriter.write( attr( STR_ATTR_SERIAL, getID(node) ) );
		myUniWriter.write( attr( STR_ATTR_ID, node.toString() ) );
		myUniWriter.write( attr( STR_ATTR_EDITABLE, Boolean.toString( node.isEditable() ) ) );
	}

	private String getDecisionBasics( DecisionNode node, Collection instances ){
		return getOutcomeBasics( node, instances ) +
			attr( STR_ATTR_ID, node.toString() ) +
			attr( STR_ATTR_EDITABLE, Boolean.toString( node.isEditable() ) );
	}

	private void writeBasics( Parameter param, int index ){
		myUniWriter.write( attr( STR_ATTR_SERIAL, getID(param) ) );
		if( index >= 0 ) myUniWriter.write( attr( STR_ATTR_INDEX, Integer.toString( index ) ) );
	}

	private String getParameterBasics( Parameter param, int index ){
		String ret = attr( STR_ATTR_SERIAL, getID(param) );
		if( index >= 0 ) ret += attr( STR_ATTR_INDEX, Integer.toString( index ) );
		return ret;
	}

	private boolean isIDed( Object obj ){
		return myWritten.containsKey( obj );
	}

	private String getID( Object obj ){
		if( myWritten.containsKey( obj ) ) return myWritten.get(obj).toString();
		else{
			Integer id = new Integer( myIDCounter++ );
			myWritten.put( obj, id );
			return id.toString();
		}
	}

	public interface UniWriter{
		public void write( String s );
	}

	public class PrintStreamWriter implements UniWriter{
		public PrintStreamWriter( PrintStream out ){
			this.myPrintStream = out;
		}

		public void write( String s ){
			myPrintStream.print( s );
		}

		private PrintStream myPrintStream;
	}

	public class PrintWriterWriter implements UniWriter{
		public PrintWriterWriter( PrintWriter out ){
			this.myPrintWriter = out;
		}

		public void write( String s ){
			myPrintWriter.print( s );
		}

		private PrintWriter myPrintWriter;
	}

	private static String[] ARRAY_TAB_STRINGS;

	public static String getTab( int length ){
		ensureArrayTabStrings( length );
		return ARRAY_TAB_STRINGS[ length ];
	}

	private static void ensureArrayTabStrings( int length ){
		if( (ARRAY_TAB_STRINGS != null) && (ARRAY_TAB_STRINGS.length > length) ) return;
		length += 5;
		char[] chararray = new char[ length ];
		Arrays.fill( chararray, CHAR_TAB );
		ARRAY_TAB_STRINGS = new String[ length ];
		for( int i=0; i<length; i++ )
			ARRAY_TAB_STRINGS[i] = new String( chararray, 0, i );
	}

	private static NumberFormat FORMAT_VALUE;

	private static NumberFormat getFormatValue(){
		if( FORMAT_VALUE == null ){
			FORMAT_VALUE = NumberFormat.getNumberInstance();
			FORMAT_VALUE.setParseIntegerOnly(false);
			FORMAT_VALUE.setMaximumFractionDigits( 200 );
		}
		return FORMAT_VALUE;
	}

	/** test/debug */
	public static void main( String[] args ){
		//String pathNetwork = "C:\\keith\\code\\argroup\\networks\\cancer.net";
		String pathNetwork = "C:\\keith\\code\\argroup\\networks\\Barley.net";
		//String pathNetwork = "C:\\keithcascio\\networks\\cancer.net";
		//String pathNetwork = "C:\\keithcascio\\networks\\Barley.net";

		//String idVar = "D";
		String idVar = "jordn";

		boolean useTempFile = false;

		if( args.length > 0 ) pathNetwork = args[0];
		if( args.length > 1 ) idVar = args[1];

		BeliefNetwork bn = null;

		try{
			bn = NetworkIO.read( pathNetwork );
		}catch( Exception e ){
			System.err.println( "Error: Failed to read " + pathNetwork );
			return;
		}

		FiniteVariable fVar = (FiniteVariable) bn.forID( idVar );
		TableShell shell = (TableShell) fVar.getCPTShell( DSLNodeType.CPT );
		DecisionTreeImpl tree = (DecisionTreeImpl) new Optimizer( Double.MIN_VALUE ).optimize( shell );

		try{
			File tempfile = null;

			if( useTempFile ){
				File.createTempFile( "decisiontree_" + idVar, "xml" );
				tempfile.deleteOnExit();
			}
			else tempfile = new File( ".\\decisiontree_" + idVar + ".xml" );

			PrintWriter writer = new PrintWriter( new FileWriter( tempfile ) );
			//PrintStream writer = new PrintStream( new FileOutputStream( tempfile ) );
			XmlWriter.write( tree, writer );
			writer.close();

			XmlDocumentHandler handler = new XmlDocumentHandler( bn );
			handler.parse( tempfile );

			DecisionTree treeagain = handler.getTree( fVar );

			if( treeagain == null ) System.err.println( "Failed to parse tree for " + idVar );
			else{
				System.out.println( "Parsed tree for " + idVar );
				boolean deeply = tree.getRoot().isDeeplyEquivalent( treeagain.getRoot(), Double.MIN_VALUE, new HashMap() );
				System.out.println( "    deeply equivalent? " + deeply );
			}
		}catch( Exception exception ){
			System.err.println( exception );
		}
	}

	private Map myWritten;
	private UniWriter myUniWriter;
	private int myIDCounter = 0;
	//private Map myMapOutcomeToInstanceList;
	private GroupedParameterWriter myGroupedParameterWriter;
	private GroupedDecisionWriter myGroupedDecisionWriter;
}
