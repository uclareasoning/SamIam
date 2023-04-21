package edu.ucla.belief.ui.preference;

import edu.ucla.belief.ui.util.*;
//import edu.ucla.belief.recursiveconditioning.Settings;
import edu.ucla.belief.inference.RCSettings;

//Add these lines for the exceptions that can be thrown when the XML document is parsed:
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.*;
import org.xml.sax.Attributes;

import java.io.File;
import java.io.IOException;

/**
	@author Keith Cascio
	@since 050603
*/
public class PreferencesHandler extends DefaultHandler
{
	public PreferencesHandler()
	{
	}

	public void setSamiamPreferences( SamiamPreferences prefs )
	{
		mySamiamPreferences = prefs;
	}

	public void startDocument() throws SAXException
	{
		mySubHandler = theRootCheckHandler;
		myCharactersHandler = theCharactersNoop;
	}

	public void startElement( String uri,String localName,String qName,Attributes attributes ) throws SAXException
	{
		//System.out.println( "PreferencesHandler.startElement( "+uri+", "+localName+", "+qName+" )" );
		mySubHandler.startElement( uri, localName, qName, attributes );
	}

	public void endElement( String uri,String localName,String qName ) throws SAXException
	{
		//System.out.println( "PreferencesHandler.endElement( "+uri+", "+localName+", "+qName+" )" );
		mySubHandler.endElement( uri, localName, qName );
	}

	public void characters(	char[] ch,int start,int length) throws SAXException
	{
		//System.out.println( "PreferencesHandler.characters( "+new String(ch,start,length)+", "+start+", "+length+" )" );
		myCharactersHandler.characters( ch, start, length );
	}

	public void endDocument() throws SAXException
	{
		//System.out.println( "PreferencesHandler.endDocument()" );
		mySubHandler = null;
		myCharactersHandler = null;
	}




	/** temporarily allow a third-party handler to handle elements,
		until it returns false
		@since 20070404 */
	public class WrapperHandler implements ElementHandler{
		public WrapperHandler( ElementHandler toWrap, ElementHandler toReturn ){
			myWrapped = toWrap;
			myReturn  = toReturn;
		}

		public boolean startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException{
			boolean result = myWrapped.startElement( uri, localName, qName, attributes );
			if( ! result ) return (mySubHandler = myReturn).startElement( uri, localName, qName, attributes );
			else return true;
		}

		public boolean   endElement( String uri, String localName, String qName ) throws SAXException{
			boolean result = myWrapped.endElement( uri, localName, qName );
			if( ! result ) return (mySubHandler = myReturn).endElement( uri, localName, qName );
			else return true;
		}

		public boolean wrap( boolean result ){
			if( ! result ) mySubHandler = myReturn;
			return result;
		}

		private ElementHandler myWrapped, myReturn;
	}

	public class GroupHandler implements ElementHandler
	{
		public boolean startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if( currentPreferenceGroup == null ) return true;

		  //if( qName.indexOf( PreferenceReader.STR_CLASSNAME_PREFIX ) == (int)-1 ) throw new SAXException( "invalid preference class name" );
			if( ! qName.equals( "pref" ) ) throw new SAXException( "invalid preference element" );

			String className = attributes.getValue( PreferenceReader.STR_ATTR_CLASS );
			String strkey    = attributes.getValue( PreferenceReader.STR_ATTR_KEY   );
			String strvalue  = attributes.getValue( PreferenceReader.STR_ATTR_VALUE );
			String strname   = attributes.getValue( PreferenceReader.STR_ATTR_NAME  );

			Preference pref = myPreferenceReader.readAndAdd( className, strkey, strvalue, strname, mySamiamPreferences );
			if( pref != null ){
				currentPreferenceGroup.add( pref );
				if( pref.getElementHandler() != null ) mySubHandler = new WrapperHandler( pref.getElementHandler(), GroupHandler.this );
			}
			return true;
		}

		public boolean endElement(String uri,String localName,String qName) throws SAXException
		{
			if( qName.equals( SamiamPreferences.STR_GROUP_TAG ) )
			{
				mySubHandler = theValidRootHandler;
			}
			return true;
		}
	}
	public final GroupHandler theGroupHandler = new GroupHandler();

	protected PreferenceGroup currentPreferenceGroup;
	protected PreferenceReader myPreferenceReader = new PreferenceReader();

	public class RootCheckHandler implements ElementHandler
	{
		public boolean startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if( qName.equals( SamiamPreferences.STR_ROOT_TAG ) ) mySubHandler = theValidRootHandler;
			else throw new SAXException( "invalid root" );
			return true;
		}

		public boolean endElement(String uri,String localName,String qName) throws SAXException
		{
			return true;
		}
	}
	public final RootCheckHandler theRootCheckHandler = new RootCheckHandler();

	public class ValidRootHandler implements ElementHandler
	{
		public boolean startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException
		{
			if( qName.equals( SamiamPreferences.STR_DEFAULT_PATH_TOKEN ) )
			{
				myCharactersHandler = theCharactersDefaultPath;
			}
			else if( qName.equals( SamiamPreferences.STR_LAST_FILE_EXTENSION_TOKEN ) )
			{
				myCharactersHandler = theCharactersLastFileExtension;
			}
			else if( qName.equals( SamiamPreferences.STR_SECONDS_PER_RC_CALL_TOKEN ) )
			{
				myCharactersHandler = theCharactersSecondsPer;
			}
			else if( qName.equals( SamiamPreferences.STR_RECENT_DOCUMENTS_TOKEN ) )
			{
				myCharactersHandler = theCharactersRecentDocuments;
			}
			else if( qName.equals( SamiamPreferences.STR_LAST_DYNAMATOR_TOKEN ) )
			{
				myCharactersHandler = theCharactersLastDynamator;
			}
			else if( qName.equals( SamiamPreferences.STR_TOKEN_GENERIC ) ){
				myCharactersHandler = theCharactersGenericProperty;
				myCurrentGenericKey = attributes.getValue( SamiamPreferences.STR_ATTR_KEY );
			}
			else if( qName.equals( SamiamPreferences.STR_GROUP_TAG ) )
			{
				mySubHandler = theGroupHandler;

				String tempName = attributes.getValue( SamiamPreferences.STR_NAME_ATTR );
				currentPreferenceGroup = mySamiamPreferences.getPreferenceGroup( tempName );
				if( (currentPreferenceGroup == null) && (!mySamiamPreferences.isPreferenceGroupNameReserved( tempName )) )
				{
					currentPreferenceGroup = new PreferenceGroup( tempName );
					mySamiamPreferences.putPreferenceGroup( currentPreferenceGroup );
				}
			}
			return true;
		}

		public boolean endElement(String uri,String localName,String qName) throws SAXException
		{
			myCharactersHandler = theCharactersNoop;
			return true;
		}
	}
	public final ValidRootHandler theValidRootHandler = new ValidRootHandler();



	public class CharactersNoop implements CharactersHandler
	{
		public void characters(	char[] ch,int start,int length) throws SAXException
		{}
	}
	public final CharactersNoop theCharactersNoop = new CharactersNoop();

	public class CharactersDefaultPath implements CharactersHandler
	{
		public void characters(	char[] ch,int start,int length) throws SAXException
		{
			//System.out.println( "CharactersDefaultPath" );
			String value = new String(ch,start,length);
			mySamiamPreferences.setDefaultDirectory( new File( value ) );
		}
	}
	public final CharactersDefaultPath theCharactersDefaultPath = new CharactersDefaultPath();

	public final CharactersHandler theCharactersGenericProperty = new CharactersHandler(){
		public void characters(	char[] ch, int start, int length ) throws SAXException {
			//System.out.println( "theCharactersUserDotExecutableLocation" );
			String value = new String(ch,start,length);
			mySamiamPreferences.putProperty( myCurrentGenericKey, value );
		}
	};
	private String myCurrentGenericKey;

	public class CharactersLastFileExtension implements CharactersHandler
	{
		public void characters(	char[] ch,int start,int length) throws SAXException
		{
			//System.out.println( "CharactersLastFileExtension" );
			String value = new String(ch,start,length);
			mySamiamPreferences.lastEncounteredFileExtension = value;
		}
	}
	public final CharactersLastFileExtension theCharactersLastFileExtension = new CharactersLastFileExtension();

	public class CharactersSecondsPer implements CharactersHandler
	{
		public void characters(	char[] ch,int start,int length) throws SAXException
		{
			//System.out.println( "CharactersSecondsPer" );
			String value = new String(ch,start,length);
			double secondsPerRCCall = Double.parseDouble( value );
			RCSettings.setSecondsPerRCCall( secondsPerRCCall );//Settings.setSecondsPerRCCall( secondsPerRCCall );
		}
	}
	public final CharactersSecondsPer theCharactersSecondsPer = new CharactersSecondsPer();

	public class CharactersRecentDocuments implements CharactersHandler
	{
		public void characters(	char[] ch,int start,int length) throws SAXException
		{
			//System.out.println( "CharactersRecentDocuments" );
			String value = new String(ch,start,length);
			mySamiamPreferences.setRecentDocuments( value );
		}
	}
	public final CharactersRecentDocuments theCharactersRecentDocuments = new CharactersRecentDocuments();

	public class CharactersLastDynamator implements CharactersHandler
	{
		public void characters(	char[] ch,int start,int length) throws SAXException
		{
			//System.out.println( "CharactersLastDynamator" );
			String value = new String(ch,start,length);
			mySamiamPreferences.strLastDynamator = value;
		}
	}
	public final CharactersLastDynamator theCharactersLastDynamator = new CharactersLastDynamator();



	protected SamiamPreferences mySamiamPreferences;

	public void parse( File infile ) throws IOException
	{
		if( mySAXParser == null )
		{
			try{
				mySAXParser = SAXParserFactory.newInstance().newSAXParser();
			}catch( ParserConfigurationException e ){
				System.err.println( "Warning: PreferencesHandler.parse() caused " + e );
				return;
			}catch( SAXException e ){
				System.err.println( "Warning: PreferencesHandler.parse() caused " + e );
				return;
			}
		}

		try{
			mySAXParser.parse( infile, this );
		}catch( SAXException e ){
			System.err.println( "Warning: PreferencesHandler.parse() caused " + e );
			return;
		}
	}

	protected SAXParser mySAXParser;
	protected ElementHandler mySubHandler;
	protected CharactersHandler myCharactersHandler;
}
