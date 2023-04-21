package edu.ucla.belief.ui.util;

import org.xml.sax.SAXException;

/**
	@author Keith Cascio
	@since 051903
*/
public interface CharactersHandler
{
	public void characters(	char[] ch,int start,int length) throws SAXException;
}
