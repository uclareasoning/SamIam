package edu.ucla.belief.io;

//Add these lines for the exceptions that can be thrown when the XML document is parsed:
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
//import javax.xml.validation.*;//uncomment this for java 5

import org.xml.sax.helpers.*;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

/** @author keith cascio
	@since 20050201 */
public abstract class AbstractSaxHandler extends DefaultHandler
{
	public AbstractSaxHandler(){}

	abstract public boolean        isValidRootElementName( String qname );
	abstract public ElementHandler getValidRootHandler(    String qName, Attributes attributes );
	abstract public boolean        isValidating();

	/** override this method to make syntax errors non-fatal */
	public boolean isValidationErrorFatal(){
		return true;
	}
	/** override this method if, e.g., you need to install an xml validation schema */
	public void configureFactory( SAXParserFactory factory ){
		/* uncomment this for java 5
		Schema schema = getSchema();
		if( schema != null ) myFactory.setSchema( schema );
		*/
	}

	/*/ uncomment this for java 5
		@since 20060525 //
	public Schema getSchema(){
		return null;
	}*/

	public void setElementHandler( ElementHandler elementhandler ){
		this.mySubHandler = elementhandler;
	}

	public ElementHandler getElementHandler(){
		return this.mySubHandler;
	}

	public void setCharactersHandler( CharactersHandler charactershandler ){
		this.myCharactersHandler = charactershandler;
	}

	public CharactersHandler getCharactersHandler(){
		return this.myCharactersHandler;
	}

	public void startDocument() throws SAXException
	{
		this.mySubHandler = theRootCheckHandler;
		this.myCharactersHandler = theCharactersNoop;
	}

	public void startElement( String uri,String localName,String qName,Attributes attributes ) throws SAXException
	{
		//System.out.println( "AbstractSaxHandler.startElement( "+uri+", "+localName+", "+qName+" )" );
		mySubHandler.startElement( uri, localName, qName, attributes );
	}

	public void endElement( String uri,String localName,String qName ) throws SAXException
	{
		//System.out.println( "AbstractSaxHandler.endElement( "+uri+", "+localName+", "+qName+" )" );
		mySubHandler.endElement( uri, localName, qName );
	}

	public void characters(	char[] ch,int start,int length) throws SAXException
	{
		//System.out.println( "AbstractSaxHandler.characters( "+new String(ch,start,length)+", "+start+", "+length+" )" );
		myCharactersHandler.characters( ch, start, length );
	}

	public void endDocument() throws SAXException
	{
		//System.out.println( "AbstractSaxHandler.endDocument()" );
		mySubHandler = null;
		myCharactersHandler = null;
	}

	public final ElementHandler theRootCheckHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if( isValidRootElementName( qName ) ) setElementHandler( getValidRootHandler( qName, attributes ) );
			else throw new SAXException( "invalid root: \"" +qName+ "\" (xml is case sensitive)" );
		}

		public void endElement(String uri,String localName,String qName) throws SAXException {}
	};

	public final CharactersHandler theCharactersNoop = new CharactersHandler()
	{
		public void characters(	char[] ch,int start,int length) throws SAXException{}
	};

	/** @since 20060524 */
	public SAXParserFactory getFactory(){
		if( myFactory == null ){
			myFactory = SAXParserFactory.newInstance();
			myFactory.setValidating( isValidating() );
			configureFactory( myFactory );
		}
		return myFactory;
	}

	/** @since 20060524 */
	public SAXParser getSAXParser() throws ParserConfigurationException,SAXException {
		if( mySAXParser == null ) mySAXParser = getFactory().newSAXParser();
		return mySAXParser;
	}

	public void parse( File infile ) throws IOException, SAXException, ParserConfigurationException {
		cleanupAbstractSaxHandler();
		getSAXParser().parse( infile, this );
	}

	public void parse( InputStream is ) throws IOException, SAXException, ParserConfigurationException {
		cleanupAbstractSaxHandler();
		getSAXParser().parse( is, this );
	}

	public void parse( InputSource is ) throws IOException, SAXException, ParserConfigurationException {
		cleanupAbstractSaxHandler();
		getSAXParser().parse( is, this );
	}

	public void cleanupAbstractSaxHandler(){
		if( myErrorLog != null ) myErrorLog.clear();
	}

	public void error( SAXParseException e ) throws SAXParseException {
		if( isValidationErrorFatal() ) throw e;

		if( myErrorLog == null ) myErrorLog = new LinkedList();

		myBuff.setLength(0);
		myBuff.append( "error at ln " );
		myBuff.append( e.getLineNumber() );
		myBuff.append( " col " );
		myBuff.append( e.getColumnNumber() );
		myBuff.append( ", " );
		myBuff.append( e.getMessage() );

		myErrorLog.add( myBuff.toString() );
	}

	public String[] getSyntaxErrors(){
		if( myErrorLog == null ) return (String[]) null;
		else                     return (String[]) myErrorLog.toArray( new String[myErrorLog.size()] );
	}

	public static void printAll( Throwable throwable, PrintStream stream ){
		stream.println( throwable );
		if( throwable.getCause() != null ){
			stream.println( "caused by:" );
			printAll( throwable.getCause(), stream );
		}
	}

	protected SAXParserFactory  myFactory;
	protected SAXParser         mySAXParser;
	protected ElementHandler    mySubHandler;
	protected CharactersHandler myCharactersHandler;
	private   List              myErrorLog;
	private   StringBuffer      myBuff = new StringBuffer();
}
