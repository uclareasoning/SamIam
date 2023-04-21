package edu.ucla.belief.io.xmlbif;

import edu.ucla.belief.io.*;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

/** This class is intentionally stripped down, with the hope
	of achieving the fastest possible performance on the UAI '06
	input files.

	<br />
	http://www.cs.cmu.edu/~fgcozman/Research/InterchangeFormat
	<br />
	updated 20060531 for version 0.3a proposed by Jeff Bilmes <bilmes@cuba.ee.washington.edu> 20060529

	@author keith cascio
	@since  20060622 */
public class UAI06Parser extends XmlbifParser
{
	public static final String UAI06_ROOT = STR_TAG_ROOT_0_3.toUpperCase().intern();//STR_TAG_ROOT_0_3A.toUpperCase().intern();
	public static final String UAI06_VAR  = STR_TAG_VARIABLE.toUpperCase().intern();
	public static final String UAI06_TYPE = STR_ATTR_TYPE_U.toUpperCase().intern();
	public static final String UAI06_PROB = STR_TAG_PROBABILITY_0_3A.toUpperCase().intern();
	public static final String UAI06_NAME = STR_TAG_NAME.toUpperCase().intern();
  //public static final String UAI06_PROP = STR_TAG_PROPERTY.toUpperCase().intern();
	public static final String UAI06_ANON = STR_TAG_ANONYMOUS_VALUES.toUpperCase().intern();
	public static final String UAI06_OUTC = STR_TAG_OUTCOME.toUpperCase().intern();
	public static final String UAI06_OBSE = STR_TAG_OBSERVATION.toUpperCase().intern();
	public static final String UAI06_FOR  = STR_TAG_FOR.toUpperCase().intern();
	public static final String UAI06_GIVE = STR_TAG_GIVEN.toUpperCase().intern();
	public static final String UAI06_TABL = STR_TAG_TABLE.toUpperCase().intern();

	public boolean versionSupported( String version ){
		return version.intern() == STR_VERSION_0_3;
	}

	public boolean isValidRootElementName( String qname ){
		return qname == UAI06_ROOT;
	}

	/*public static boolean isOneOf( String qname, String[] alternatives ){
		for( String alternative : alternatives ){
			if( qname == alternative ) return true;
		}
		return false;
	}*/

	/** interface RunReadBIF.MonitorableReusableParser */
	public void setHighPerformance( boolean flag ){
		throw new UnsupportedOperationException( "UAI06Parser is always high performance" );
	}

	public void configureFactory( SAXParserFactory factory ){
		try{
			factory.setFeature( "http://xml.org/sax/features/string-interning", true );

			for( String name : ARRAY_SAX_AVOIDABLE_PROPERTIES ){
				factory.setFeature( name, false );
			}
		}catch( ParserConfigurationException parserconfigurationexception ){
			System.err.println( "warning: unable to configure sax factory to intern strings, " + parserconfigurationexception );
		}catch( SAXException saxexception ){
			System.err.println( "warning: unable to configure sax factory to intern strings, " + saxexception );
		}

		factory.setSchema( null );

		factory.setValidating(     false );
		factory.setNamespaceAware( false );
		factory.setXIncludeAware(  false );
	}

	public ElementHandler getValidRootHandler( String qName, Attributes attributes ){
		//printCounts();
		return uai06ValidRootHandler;
	}

	private ElementHandler uai06ValidRootHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if(      qName == UAI06_VAR ){
				myVariables.add( myVariable = new Variable() );
				//String type = attributes.getValue( UAI06_TYPE );
				//if( type != null ){
				//	myVariable.type = VariableType.valueOf( type );
				//	if( !myVariable.type.isNatural() ) throw new RuntimeException( "unsupported variable type \"" + myVariable.type + "\"" );
				//}
				mySubHandler = uai06VariableHandler;
				//++vars;
			}
			else if( qName == UAI06_PROB ){
				myDefinitions.add( myDefinition = new Definition() );
				mySubHandler = uai06DefinitionHandler;
				//++probs;
			}
			else if( qName == UAI06_NAME ){
				setAccumulator( myName );
				//++names;
			}
			//else if( qName == UAI06_PROP ){
			//	myProperties.add( newAccumulator() );
			//}
		}

		public void endElement(String uri,String localName,String qName) throws SAXException {
			disableAccumulator();
		}
	};

	private ElementHandler uai06VariableHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if(      qName == UAI06_NAME ){
				setAccumulator( myVariable.id );
				//++vnames;
			}
			else if( qName == UAI06_ANON ){
				myVariable.anonymousCount = initAccumulator( myVariable.anonymousCount, 4 );
				//++anons;
			}
			else if( qName == UAI06_OBSE ){
				myVariable.indexObserved  = initAccumulator( myVariable.indexObserved,  4 );
				//++obses;
			}
			else if( qName == UAI06_OUTC ){
				myVariable.outcomes.add( newAccumulator() );
				//++outcs;
			}
			//else if( qName == UAI06_PROP ){
			//	myVariable.properties.add( newAccumulator() );
			//}
			else disableAccumulator();
		}

		public void endElement(String uri,String localName,String qName) throws SAXException {
			disableAccumulator();
			if( qName == UAI06_VAR ){
				myVariable          = null;
				mySubHandler        = uai06ValidRootHandler;

				//if( myTask != null ) myTask.touch();
			}
		}
	};

	private ElementHandler uai06DefinitionHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if(      qName == UAI06_FOR ){
				setAccumulator( myDefinition.joint );
				//++fors;
			}
			else if( qName == UAI06_GIVE ){
				myDefinition.givens.add( newAccumulator() );
				//++gives;
			}
			else if( qName == UAI06_TABL ){
				setAccumulator( myDefinition.table );
				//++tabls;
			}
			//else if( qName == UAI06_PROP ){
			//	myDefinition.properties.add( newAccumulator() );
			//}
			else disableAccumulator();
		}

		public void endElement(String uri,String localName,String qName) throws SAXException {
			disableAccumulator();
			if( qName == UAI06_PROB ){
				myDefinition        = null;
				mySubHandler        = uai06ValidRootHandler;

				//if( myTask != null ) myTask.touch();
			}
		}
	};

	/*
	int vars, probs, names, vnames, anons, obses, outcs, fors, gives, tabls;
	private void printCounts(){
		System.out.println( "vars?   " + vars );
		System.out.println( "probs?  " + probs );
		System.out.println( "names?  " + names );
		System.out.println( "vnames? " + vnames );
		System.out.println( "anons?  " + anons );
		System.out.println( "obses?  " + obses );
		System.out.println( "outcs?  " + outcs );
		System.out.println( "fors?   " + fors );
		System.out.println( "gives?  " + gives );
		System.out.println( "tabls?  " + tabls );
	}*/

	/** test/debug */
	public static void main( String[] args ){
		if( args.length < 1 ){
			System.err.println( "usage: "+UAI06Parser.class.getName()+" <path of file to parse>" );
			System.exit(1);
		}

		XmlbifParser parserLP = new XmlbifParser();
		parserLP.setHighPerformance( true );
		XmlbifParser parserHP = new UAI06Parser();

		mainImpl( args[0], parserLP, parserHP );
	}
}
