package edu.ucla.belief.io;

import org.xml.sax.SAXException;

/** Moved from SamIam package edu.ucla.belief.ui.util since 020105
	@author Keith Cascio
	@since 051903 */
public interface CharactersHandler
{
	public void characters(	char[] ch, int start, int length ) throws SAXException;
}
