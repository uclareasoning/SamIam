package edu.ucla.belief.io;

//import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.Variable;
import edu.ucla.belief.Definitions;
import edu.ucla.belief.recursiveconditioning.Xmlizer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.io.*;
import java.text.DateFormat;
import java.util.Date;

//Add these lines to import the JAXP APIs you'll be using:
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
//{superfluous} import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

//{superfluous} import javax.xml.transform.Transformer;
//{superfluous} import javax.xml.transform.TransformerFactory;
//{superfluous} import javax.xml.transform.TransformerException;
//{superfluous} import javax.xml.transform.TransformerConfigurationException;
//{superfluous} import javax.xml.transform.OutputKeys;

//{superfluous} import javax.xml.transform.dom.DOMSource;
//{superfluous} import javax.xml.transform.stream.StreamSource;
//{superfluous} import javax.xml.transform.stream.StreamResult;

//Add these lines for the exceptions that can be thrown when the XML document is parsed:
import org.xml.sax.SAXException;
//{superfluous} import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.*;
import org.xml.sax.Attributes;

//Finally, import the W3C definition for a DOM and DOM exceptions:
import org.w3c.dom.Document;
//{superfluous} import org.w3c.dom.Node;
import org.w3c.dom.Element;
//{superfluous} import org.w3c.dom.Attr;
//{superfluous} import org.w3c.dom.Text;
//{superfluous} import org.w3c.dom.NodeList;
//{superfluous} import org.w3c.dom.DOMException;
//{superfluous} import org.w3c.dom.DocumentType;

/** @author Keith Cascio
	@since 20030519, 20060210 */
public class InstantiationXmlizer extends DefaultHandler
{
	public static final String STR_TAG_ROOT      = "instantiation";
	public static final String STR_ATTR_DATE     = "date";
	public static final String STR_TAG_INST      = "inst";
	public static final String STR_ATTR_ID       = "id";
	public static final String STR_ATTR_VALUE    = "value";
	public static final String STR_ATTR_NEGATIVE = "negative";

	public static final String STR_VALUE_TRUE    = "true";

	/*public boolean load( InstantiationClipBoard clipboard, File fileInput ) throws IOException
	{
		if( load( fileInput ) ){
			clipboard.copy( myMap );
			return true;
		}
		else return false;
	}*/

	/** @since 20060214 VALENTINE'S DAY!!! */
	public Map getMap( File fileInput ) throws IOException {
		if( load( fileInput ) ){
			return Collections.unmodifiableMap( myMap );
		}
		else return (Map)null;
	}

	/** @since 20050204 */
	public boolean loadMap( Map clipboard, File fileInput ) throws IOException {
		return loadMap( clipboard, (Map)null, fileInput );
	}

	/** @since 20050204 */
	public boolean loadMap( Map positive, Map negative, File fileInput ) throws IOException
	{
		if( load( fileInput ) ){
			if( positive != null ) positive.putAll( myMap );
			if( negative != null ) negative.putAll( myMapNegative );
			return true;
		}
		else return false;
	}

	/** @since 20050204 */
	private boolean load( File fileInput ) throws IOException
	{
		//System.out.println( "InstantiationXmlizer.load( "+fileInput.getPath()+" )" );

		if( mySAXParser == null )
		{
			try{
				mySAXParser = SAXParserFactory.newInstance().newSAXParser();
			}catch( ParserConfigurationException e ){
				System.err.println( "Warning: InstantiationXmlizer.load() caused " + e );
				return false;
			}catch( SAXException e ){
				System.err.println( "Warning: InstantiationXmlizer.load() caused " + e );
				return false;
			}
		}

		try{
			mySAXParser.parse( fileInput, this );
		}catch( SAXException e ){
			System.err.println( "Warning: InstantiationXmlizer.load() caused " + e );
			return false;
		}

		return true;
	}

	public void startElement( String uri,String localName,String qName,Attributes attributes ) throws SAXException
	{
		//System.out.println( "InstantiationXmlizer.startElement( "+uri+", "+localName+", "+qName+" )" );
		mySubHandler.startElement( uri, localName, qName, attributes );
	}

	public void endElement( String uri,String localName,String qName ) throws SAXException
	{
		//System.out.println( "InstantiationXmlizer.endElement( "+uri+", "+localName+", "+qName+" )" );
		mySubHandler.endElement( uri, localName, qName );
	}

	public void startDocument() throws SAXException
	{
		//System.out.println( "InstantiationXmlizer.startDocument()" );
		if( myMap == null ) myMap = new HashMap();
		else myMap.clear();
		if( myMapNegative == null ) myMapNegative = new HashMap();
		else myMapNegative.clear();
		mySubHandler = theRootCheckHandler;
	}

	public void endDocument() throws SAXException
	{
		//System.out.println( "InstantiationXmlizer.endDocument()" );
		mySubHandler = null;
	}


	public class RootCheckHandler implements ElementHandler
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if( qName.equals( STR_TAG_ROOT ) ) mySubHandler = theValidRootHandler;
			else throw new SAXException( "invalid root" );
		}

		public void endElement(String uri,String localName,String qName) throws SAXException {}
	}
	public final RootCheckHandler theRootCheckHandler = new RootCheckHandler();

	public class ValidRootHandler implements ElementHandler
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if( qName.equals( STR_TAG_INST ) )
			{
				String strid = attributes.getValue( STR_ATTR_ID );
				String strvalue = attributes.getValue( STR_ATTR_VALUE );

				if( strid != null && strvalue != null ){
					boolean flagNegative = false;
					if( attributes.getIndex( STR_ATTR_NEGATIVE ) >= 0 ){
						String strnegative = attributes.getValue( STR_ATTR_NEGATIVE );
						flagNegative = ( (strnegative != null) && ((strnegative.length()==0) || (strnegative.equals( STR_VALUE_TRUE )) ) );
					}
					if( flagNegative ) recordNegative( strid, strvalue );
					else myMap.put( strid, strvalue );
				}
			}
		}

		public void endElement(String uri,String localName,String qName) throws SAXException {}
	}
	public final ValidRootHandler theValidRootHandler = new ValidRootHandler();

	protected SAXParser mySAXParser;
	protected Map myMap, myMapNegative;
	protected ElementHandler mySubHandler;

	/** @since 20050204 */
	private void recordNegative( String strid, String strvalue ){
		Collection values = null;
		if( myMapNegative.containsKey( strid ) ) values = (Collection) myMapNegative.get( strid );
		else myMapNegative.put( strid, values = new HashSet(1) );
		if( !values.contains( strvalue ) ) values.add( strvalue );
	}

	public boolean save( Map clipboard, File fileOutput )
	{
		return save( clipboard, (Map)null, fileOutput );
	}

	/** @since 20050204 */
	public boolean save( Map positive, Map negative, File fileOutput ){
		return this.save( positive, negative, (Map)null, fileOutput );
	}

	/** @since 20060210 */
	public boolean save( Map positive, Map negative, Map extraNameToValues, File fileOutput )
	{
		DocumentBuilder builder = null;
		Document document = null;
		try{
			builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
			document = builder.newDocument();
		}catch( javax.xml.parsers.ParserConfigurationException e ){
			if( Definitions.DEBUG ) e.printStackTrace();
			return false;
		}

		Element eltRoot = document.createElement( STR_TAG_ROOT );
		eltRoot.setAttribute( STR_ATTR_DATE, makeDate() );

		Object next;
		if( positive != null ){
			for( Iterator it = positive.keySet().iterator(); it.hasNext(); ){
				next = it.next();
				append( document, eltRoot, next, positive.get( next ), false );
			}
		}

		Object val;
		if( negative != null ){
			for( Iterator it = negative.keySet().iterator(); it.hasNext(); ){
				next = it.next();
				val = negative.get( next );
				if( val instanceof Collection ) appendAll( document, eltRoot, next, (Collection)val, true );
				else append( document, eltRoot, next, val, true );
			}
		}

		if( extraNameToValues != null ){
			for( Iterator it = extraNameToValues.keySet().iterator(); it.hasNext(); ){
				next = it.next();
				val = extraNameToValues.get( next );
				append( document, eltRoot, next.toString(), val.toString() );
			}
		}

		document.appendChild( eltRoot );

		Xmlizer.writeXML( document, fileOutput );
		return true;
	}

	/** @since 20050204 */
	private void appendAll( Document document, Element eltRoot, Object var, Collection values, boolean flagNegative )
	{
		for( Iterator it = values.iterator(); it.hasNext(); ){
			append( document, eltRoot, var, it.next(), flagNegative );
		}
	}

	/** @since 20050204 */
	private Element append( Document document, Element eltRoot, Object var, Object value, boolean flagNegative )
	{
		Element eltInst = document.createElement( STR_TAG_INST );

		eltInst.setAttribute( STR_ATTR_ID, getID( var ) );
		eltInst.setAttribute( STR_ATTR_VALUE, value.toString() );
		if( flagNegative ) eltInst.setAttribute( STR_ATTR_NEGATIVE, STR_VALUE_TRUE );

		eltRoot.appendChild( eltInst );

		return eltInst;
	}

	/** @since 200602010 */
	private Element append( Document document, Element eltRoot, String eltName, String data ){
		Element elt = document.createElement( eltName );
		elt.appendChild( document.createTextNode( data ) );
		eltRoot.appendChild( elt );
		return elt;
	}

	public String makeDate(){
		return myDateFormat.format( new Date( System.currentTimeMillis() ) );
	}

	protected DateFormat myDateFormat = DateFormat.getDateTimeInstance();

	public static String getID( Object obj ){
		if( obj instanceof Variable ) return ((Variable)obj).getID();
		else return obj.toString();
	}
}
