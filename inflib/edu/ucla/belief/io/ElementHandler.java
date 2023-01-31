package edu.ucla.belief.io;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

/** Moved from SamIam package edu.ucla.belief.ui.util since 020105
	@author Keith Cascio
	@since 051903 */
public interface ElementHandler
{
	public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException;
	public void endElement(String uri,String localName,String qName) throws SAXException;
}
