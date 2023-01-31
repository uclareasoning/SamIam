package edu.ucla.belief.decision;

import edu.ucla.belief.io.*;
import edu.ucla.belief.*;
import edu.ucla.structure.IdentityArrayMap;

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

/** @author keith cascio
	@since 20050201 */
public class XmlDocumentHandler extends AbstractSaxHandler
{
	public XmlDocumentHandler( BeliefNetwork bn ){
		super();
		myBeliefNetwork = bn;
	}

	/** @since 20060524 */
	public boolean isValidating(){
		return false;
	}

	public BeliefNetwork getBeliefNetwork(){
		return myBeliefNetwork;
	}

	public boolean isValidRootElementName( String name ){
		return true;
	}

	public ElementHandler getValidRootHandler( String qName, Attributes attributes ){
		if( (qName == null) || (!qName.equals( XmlWriter.STR_ELEMENT_ROOT )) ) return theValidRootHandler;
		else return resetXmlElementHandler( qName, attributes );
	}

	public XmlElementHandler resetXmlElementHandler( String qName, Attributes attributes ){
		if( myXmlElementHandler == null ) myXmlElementHandler = new XmlElementHandler( XmlDocumentHandler.this );
		return myXmlElementHandler.reset( qName, attributes );
	}

	public final ElementHandler theValidRootHandler = new ElementHandler()
	{
		public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException{
			if( qName.equals( XmlWriter.STR_ELEMENT_ROOT ) ){
				setElementHandler( resetXmlElementHandler( qName, attributes ) );
			}
		}

		public void endElement(String uri,String localName,String qName) throws SAXException{
			myCharactersHandler = theCharactersNoop;
		}
	};

	public void putTree( FiniteVariable var, DecisionTree tree ){
		if( myMapVariablesToTrees == null ) myMapVariablesToTrees = new IdentityArrayMap();
		myMapVariablesToTrees.put( var, tree );
	}

	public DecisionTree getTree( FiniteVariable var ){
		if( myMapVariablesToTrees == null ) return (DecisionTree) null;
		return (DecisionTree) myMapVariablesToTrees.get( var );
	}

	private XmlElementHandler myXmlElementHandler;
	private BeliefNetwork myBeliefNetwork;
	private IdentityArrayMap myMapVariablesToTrees;
}
