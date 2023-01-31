package edu.ucla.belief.recursiveconditioning;

import edu.ucla.belief.dtree.*;

//Add these lines for the exceptions that can be thrown when the XML document is parsed:
import org.xml.sax.SAXException;
//{superfluous} import org.xml.sax.SAXParseException;

import org.xml.sax.helpers.*;
import org.xml.sax.Attributes;

/**
	@author Keith Cascio
	@since 031903
*/
public class StatsHandler extends DefaultHandler
{
	public StatsHandler()
	{
	}

	public void setFileInfo( FileInfo info )
	{
		myFileInfo = info;
		myStats = info.stats;
	}

	public void startDocument() throws SAXException
	{
		myFlagNotEncounteredDtreeElement = true;
		myFlagNotEncounteredRCDElement = true;
		myFlagNotEncounteredRootElement = true;
	}

	public void startElement( String uri,
				String localName,
				String qName,
				Attributes attributes) throws SAXException
	{
		//System.out.println( "StatsHandler.startElement( "+uri+", "+localName+", "+qName+" )" );

		if( myFlagNotEncounteredDtreeElement && qName.equals( Dtree.getTagName() ) )
		{
			String strMethod = attributes.getValue( Xmlizer.STR_ATTR_CREATIONMETHOD_NAME );
			String strMaxCluster = attributes.getValue( Xmlizer.STR_ATTR_DTREEMAXCLUSTER_NAME );
			String strHeight = attributes.getValue( Xmlizer.STR_ATTR_DTREEHEIGHT_NAME );
			String strMaxCutset = attributes.getValue( Xmlizer.STR_ATTR_DTREECUTSET_NAME );
			String strMaxContext = attributes.getValue( Xmlizer.STR_ATTR_DTREECONTEXT_NAME );

			if( strMethod		!= null ) myFileInfo.dtreeMethod = strMethod;
			if( strMaxCluster	!= null ) myStats.maxCluster	= Integer.parseInt( strMaxCluster );
			if( strHeight		!= null ) myStats.height	= Integer.parseInt( strHeight );
			if( strMaxCutset	!= null ) myStats.maxCutset	= Integer.parseInt( strMaxCutset );
			if( strMaxContext	!= null ) myStats.maxContext	= Integer.parseInt( strMaxContext );

			myFlagNotEncounteredDtreeElement = false;
		}
		else if( myFlagNotEncounteredRootElement && qName.equals( Xmlizer.STR_ELT_ROOT_NAME ) )
		{
			myFileInfo.networkName = attributes.getValue( Xmlizer.STR_ATTR_NETWORKNAME_NAME );
			myFlagNotEncounteredRootElement = false;
		}
		else if( myFlagNotEncounteredRCDElement )
		{
			if( qName.equals( RCDtree.getStaticTagName() ) )
			{
				myFileInfo.rcType = "dtree";
				myFlagNotEncounteredRCDElement = false;
				getRCDAttributes( attributes );
			}
			else if( qName.equals( RCDgraph.getStaticTagName() ) )
			{
				myFileInfo.rcType = "dgraph";
				myFlagNotEncounteredRCDElement = false;
				getRCDAttributes( attributes );
			}
		}
	}

	/**
		@author Keith Cascio
		@since 070703
	*/
	protected void getRCDAttributes( Attributes attributes )
	{
		String strUserMemory = attributes.getValue( Xmlizer.STR_ATTR_USERMEMORY_NAME );
		String strEstimatedTime = attributes.getValue( Xmlizer.STR_ATTR_ESTIMATEDTIME_NAME );

		if( strUserMemory	!= null ) myFileInfo.userMemory = strUserMemory;
		if( strEstimatedTime	!= null ) myFileInfo.estimatedTime = strEstimatedTime;
	}

	//public void endDocument() throws SAXException
	//{
	//	System.out.println( "StatsHandler parsed: " + myFileInfo );
	//}

	protected FileInfo myFileInfo;
	protected Stats myStats;
	protected boolean myFlagNotEncounteredDtreeElement = true;
	protected boolean myFlagNotEncounteredRCDElement = true;
	protected boolean myFlagNotEncounteredRootElement = true;
}
