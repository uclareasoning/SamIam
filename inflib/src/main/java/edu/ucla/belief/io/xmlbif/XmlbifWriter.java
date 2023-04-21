package edu.ucla.belief.io.xmlbif;

import edu.ucla.belief.io.*;
import edu.ucla.belief.*;

import java.util.*;
import java.io.*;
import java.awt.Point;
import java.text.DateFormat;
import java.util.Date;

/**
	<a href=
	"http://www.cs.cmu.edu/~fgcozman/Research/InterchangeFormat">
	                xml bif format described

	</a>
	<br />

	<a href=
	"http://www.w3.org/TR/xmlschema-0">
	                xml schema validation primer

	</a>
	<br />

	<a href=
	"http://w3.org/2000/04/schema_hack/dtd2xsd.pl">
	                perl script to convert DTD to schema

	</a>

	@author keith cascio
	@since  20060525 */
public class XmlbifWriter implements RunWriteBIF.BeliefNetworkWriter
{
	public static final String STR_XML_VERSION = "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>";

	public static final String STR_COMMENT
		= "<!--\n  Bayesian network in XMLBIF v0.3 (BayesNet Interchange Format)\n  Produced by SamIam http://reasoning.cs.ucla.edu/samiam\n  Output created %s\n-->\n";

	public enum Element{
		BIF, NETWORK, VARIABLE, NAME, OUTCOME, OBSERVED, PROPERTY, DEFINITION, FOR, GIVEN, TABLE;

		static{
			BIF.setOpenAttr(      "VERSION=\"0.3\"" );
			VARIABLE.setOpenAttr( "TYPE=\"nature\"" );
		}

		private Element(){
			myTagOpen  = "<"  + name() + ">";
			myTagClose = "</" + name() + ">";
		}

		public void open( PrintStream out ){
			out.print( INDENTATION[INDENT] );
			out.print( myTagOpen );
		}

		public void close( PrintStream out ){
			out.print( INDENTATION[INDENT] );
			out.print( myTagClose );
		}

		public void close( PrintStream out, boolean indent ){
			if( indent ) out.print( INDENTATION[INDENT] );
			out.print( myTagClose );
		}

		public void write( String value, PrintStream out ){
			out.print( INDENTATION[INDENT] );
			out.print( myTagOpen );
			escape( value, out );
			out.print( myTagClose );
		}

		public void setOpenAttr( String open ){
			myTagOpen = "<" + name() + " " + open + ">";
		}

		/** see http://www.w3schools.com/xml/xml_cdata.asp
			see http://www.w3.org/TR/html4/types.html
			@since 20060615 */
		public void escape( String text, PrintStream out ){
			boolean escape = true;
			if( text == null ) return;
			else if( text.length() < 1 ) escape = false;
			else if( (text.indexOf( '<' ) < 0) && (text.indexOf( '&' ) < 0) ) escape = false;
			if( escape ) out.print( "<![CDATA[" );
			out.print( text );
			if( escape ) out.print( "]]>" );
		}

		private String myTagOpen;
		private String myTagClose;
	};

	public boolean write( BeliefNetwork bn, PrintStream out ){
		INDENT = 0;

		writeHead( out );

		String nameNetwork = "bayesiannetwork";
		try{
			Map properties  = ((PropertySuperintendent)bn).getProperties();
			Object objName  = properties.get( PropertySuperintendent.KEY_HUGIN_NAME );
			String name     = null;
			if( objName  != null ) name  = objName.toString();
			Object objLabel = properties.get( PropertySuperintendent.KEY_HUGIN_LABEL );
			String label    = null;
			if( objLabel != null ) label = objLabel.toString();

			if(      (name != null) && (label == null) ) nameNetwork = name;
			else if( (name == null) && (label != null) ) nameNetwork = label;
			else if(  name.equals( label )             ) nameNetwork = name;
			else if( name.equals( XmlbifParser.translateToIdentifier( label ) ) ) nameNetwork = label;
			else nameNetwork = name;
		}catch( Throwable throwable ){
			System.err.println( "warning: XmlbifWriter.write() failed to write network name: " + throwable );
		}

		++INDENT;
		Element.NAME.write( nameNetwork, out );
		out.println();
		out.println();

		Map evidence = null;
		try{
			evidence = bn.getEvidenceController().evidence();
		}catch( Throwable throwable ){
			System.err.println( "warning: XmlbifWriter.write() failed to enumerate evidence: " + throwable );
		}

		myBuff.setLength(0);
		myBuff.append( STR_POSITION_PREFIX );
		Table[] tables = new Table[ bn.size() ];
		int i = 0;
		FiniteVariable var;
		for( Object next : bn ){
			var = (FiniteVariable) next;
			tables[ i++ ] = var.getCPTShell().getCPT();
			writeVariable( var, evidence.get( var ), out );
			out.println();
		}

		for( Table table : tables ){
			writeDefinition( table, out );
			out.println();
		}

		--INDENT;

		writeTail( out );
		out.flush();

		return true;
	}

	public void writeVariable( FiniteVariable var, Object value, PrintStream out ){
		Element.VARIABLE.open( out );
		out.println();
		++INDENT;

		Element.NAME.write( var.getID(), out );
		out.println();
		for( Object outcome : var.instances() ){
			Element.OUTCOME.write( outcome.toString(), out );
			out.println();
		}

		if( value != null ){
			Element.OBSERVED.write( Integer.toString( var.index( value ) ), out );
			out.println();
		}

		myPoint.setLocation(0,0);
		//var.getLocation( myPoint );
		myBuff.setLength( INT_POSITION_PREFIX );
		myBuff.append( myPoint.x );
		myBuff.append( STR_POSITION_INFIX );
		myBuff.append( myPoint.y );
		myBuff.append( STR_POSITION_POSTFIX );

		Element.PROPERTY.write( myBuff.toString(), out );
		out.println();

		--INDENT;
		Element.VARIABLE.close( out );
		out.println();
	}

	public static final String STR_POSITION_PREFIX  = "position = (";
	public static final String STR_POSITION_INFIX   = ", ";
	public static final String STR_POSITION_POSTFIX = ")";
	public static final int    INT_POSITION_PREFIX  = STR_POSITION_PREFIX.length();

	private Point         myPoint = new Point();
	private StringBuilder myBuff  = new StringBuilder( STR_POSITION_PREFIX.length() + 16 );

	public void writeDefinition( Table table, PrintStream out ){
		Element.DEFINITION.open( out );
		out.println();
		++INDENT;

		TableIndex index = table.index();
		Element.FOR.write( index.getJoint().getID(), out );
		out.println();

		int stop = index.getNumVariables() - 2;
		for( int i=0; i <= stop; i++ ){
			Element.GIVEN.write( index.variable(i).getID(), out );
			out.println();
		}

		Element.TABLE.open( out );
		int cptLength = table.getCPLength();
		for( int i=0; i<cptLength; i++ ){
			out.print( table.getCP(i) );
			out.print( ' ' );
		}
		Element.TABLE.close( out, false );
		out.println();

		--INDENT;
		Element.DEFINITION.close( out );
		out.println();
	}

	public void writeHead( PrintStream out ){
		out.println( STR_XML_VERSION );
		out.println();
		out.printf( STR_COMMENT, DateFormat.getDateTimeInstance().format( new Date( System.currentTimeMillis() ) ) );
		out.println();
		Element.BIF.open( out );
		out.println();
		Element.NETWORK.open( out );
		out.println();
	}

	public void writeTail( PrintStream out ){
		Element.NETWORK.close( out );
		out.println();
		Element.BIF.close( out );
		out.println();
	}

	private static       int      INDENT      = 0;
	public  static final String[] INDENTATION = new String[]{
	"",
	"  ",
	"    ",
	"      ",
	"        ",
	"          ",
	"            "
	};
}
