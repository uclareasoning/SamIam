package edu.ucla.belief.decision;

import edu.ucla.belief.io.*;
import edu.ucla.belief.*;
//{superfluous} import edu.ucla.util.AbstractStringifier;

//Add these lines for the exceptions that can be thrown when the XML document is parsed:
import org.xml.sax.SAXException;
//{superfluous} import org.xml.sax.SAXParseException;
//{superfluous} import javax.xml.parsers.ParserConfigurationException;
//{superfluous} import javax.xml.parsers.SAXParser;
//{superfluous} import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.*;
import org.xml.sax.Attributes;

//{superfluous} import java.io.File;
//{superfluous} import java.io.IOException;

import java.util.*;

/** @author Keith Cascio
	@since 020105 */
public class XmlElementHandler implements ElementHandler
{
	public XmlElementHandler( XmlDocumentHandler dochandler ){
		this.myDocHandler = dochandler;
	}

	public XmlElementHandler reset( String qName, Attributes attributes ){
		if( qName.equals( XmlWriter.STR_ELEMENT_ROOT ) ){
			this.myBuilder = new Builder( myDocHandler.getBeliefNetwork(), attributes );
		}
		else this.myBuilder = null;
		return this;
	}

	public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
	{
		if( qName.equals( XmlWriter.STR_ELEMENT_NODE_INTERNAL ) ){
			//System.out.println( "start internal" );
			myBuilder.internal( attributes );
		}
		else if( qName.equals( XmlWriter.STR_ELEMENT_NODE_LEAF ) ){
			//System.out.println( "start leaf" );
			myBuilder.leaf( attributes );
		}
		else if( qName.equals( XmlWriter.STR_ELEMENT_NODE_GENERIC ) ){
			//System.out.println( "start generic" );
			myBuilder.generic( attributes );
		}
		else if( qName.equals( XmlWriter.STR_ELEMENT_PARAMETER ) ){
			//System.out.println( "start parameter" );
			myBuilder.parameter( attributes );
		}
		else throw new IllegalStateException( "unrecognized tag " + qName );
	}

	public void endElement(String uri,String localName,String qName) throws SAXException
	{
		if( qName.equals( XmlWriter.STR_ELEMENT_NODE_INTERNAL ) ){
			//System.out.println( "end internal" );
			DecisionNode current = myBuilder.internal( (Attributes)null );
			putIfRoot( current );
		}
		else if( qName.equals( XmlWriter.STR_ELEMENT_NODE_LEAF ) ){
			//System.out.println( "end leaf" );
			DecisionNode current = myBuilder.leaf( (Attributes)null );
			putIfRoot( current );
		}
		else if( qName.equals( XmlWriter.STR_ELEMENT_NODE_GENERIC ) ){
			//System.out.println( "end generic" );
			myBuilder.generic( (Attributes)null );
		}
		else if( qName.equals( XmlWriter.STR_ELEMENT_PARAMETER ) ){
			//System.out.println( "end parameter" );
			myBuilder.parameter( (Attributes)null );
		}
		else if( qName.equals( XmlWriter.STR_ELEMENT_ROOT ) ){
			myDocHandler.setElementHandler( myDocHandler.getValidRootHandler( (String)null, (Attributes)null ) );
		}
		else throw new IllegalStateException( "unrecognized tag " + qName );
	}

	private void putIfRoot( DecisionNode current ){
		if( current == myBuilder.getRoot() ){
			DecisionTreeImpl result = myBuilder.result();
			if( result != null ){
				myDocHandler.putTree( result.getIndex().getJoint(), result );
			}
		}
	}

	private XmlDocumentHandler myDocHandler;
	private Builder myBuilder;
}
