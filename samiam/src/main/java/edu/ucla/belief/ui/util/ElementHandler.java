package edu.ucla.belief.ui.util;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

/** @author keith cascio
	@since 20030519 */
public interface ElementHandler
{
	/** @return true if success in handling element, false otherwise */
	public boolean startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException;
	/** @return true if success in handling element, false otherwise */
	public boolean endElement(   String uri, String localName, String qName                        ) throws SAXException;
}
