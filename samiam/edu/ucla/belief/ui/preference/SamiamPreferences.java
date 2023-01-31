package edu.ucla.belief.ui.preference;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.dialogs.*;
//import edu.ucla.belief.ui.internalframes.RecursiveConditioningInternalFrame;
import edu.ucla.belief.ui.recursiveconditioning.RCPanel;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.networkdisplay.EvidenceLabel;
import edu.ucla.belief.ui.networkdisplay.IconFactory;
import edu.ucla.belief.ui.animation.AnimationPreferenceHandler;
import edu.ucla.belief.ui.animation.ColorIntensitySample;
import edu.ucla.belief.ui.animation.Animator;

//import edu.ucla.belief.recursiveconditioning.Settings;
import edu.ucla.belief.inference.RCSettings;
import edu.ucla.belief.io.dsl.DiagnosisType;
import edu.ucla.util.InOutDegreeProperty;
import edu.ucla.belief.VariableImpl;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

/** <p>
	This class represents the set of all PreferenceGroups for
	the samiam program.  It's responsibilities are to maintain a
	Collection of preference groups, to construct the default preferences
	and to read/write a preference
	file in XML format.
	<p>
	Based on edu.ucla.belief.ui.PackageOptions.

	@author keith cascio
	@since  20020712 */
public class SamiamPreferences
{
	public static final String STR_PREFERENCES_FILE_NAME = "samiamrc.xml";

	public static final String STR_GREP_DISPLAY_NAME_LOWER       = "find",//"grep"
	                           STR_GREP_DISPLAY_NAME_UPPER       = STR_GREP_DISPLAY_NAME_LOWER.toUpperCase(),
	                           STR_GREP_DISPLAY_NAME_CAPITALIZED = STR_GREP_DISPLAY_NAME_UPPER.substring(0,1) + STR_GREP_DISPLAY_NAME_LOWER.substring(1);

	public static final String PkgDspNme                = "Global",
	                           NetDspNme                = "Network",
	                           TreeDspNme               = "Tree",
	                           MonitorsDspNme           = "Monitors",
	                           AnimationDspNme          = "Animation",
	                           STR_KEY_GROUP_GREP       = STR_GREP_DISPLAY_NAME_CAPITALIZED,
	                           STR_KEY_GROUP_INFERENCE  = "Inference";

	public File defaultDirectory = new File( STR_DEFAULT_PATH );
	public static final String STR_DEFAULT_PATH_TOKEN  = "DefaultPath";
	public static final String STR_DEFAULT_PATH = ".";

	/** @since 050603 */
	public void setDefaultDirectory( File newFile )
	{
		if( newFile != null && newFile.exists() ) defaultDirectory = newFile;
	}

	/** @since 042104 */
	//private File myUserPrimulaLocation;
	//public static final String STR_USER_PRIMULA_LOCATION_TOKEN = "UserPrimulaLocation";
	//public void setUserPrimulaLocation( File newFile ){
	//	//System.out.println( "SamiamPreferences.setUserPrimulaLocation( "+newFile+" )" );
	//	if( (newFile == null) || newFile.exists() ) myUserPrimulaLocation = newFile;
	//}
	//public File getUserPrimulaLocation(){
	//	return myUserPrimulaLocation;
	//}

	/** @since 052004 */
	//private File myLastCodeBanditLocation;
	//public static final String STR_LAST_CODEBANDIT_LOCATION_TOKEN = "LastCodeBanditLocation";
	//public void setLastCodeBanditLocation( File newFile ){
	//	//System.out.println( "SamiamPreferences.setLastCodeBanditLocation( "+newFile+" )" );
	//	if( (newFile == null) || (newFile.exists() && newFile.isDirectory()) ) myLastCodeBanditLocation = newFile;
	//}
	//public File getLastCodeBanditLocation(){
	//	return myLastCodeBanditLocation;
	//}

	//private File myUserDotExecutableLocation;
	//public static final String STR_USER_DOT_EXECUTABLE_LOCATION_TOKEN = "UserDotExecutableLocation";
	//public void setUserDotExecutableLocation( File newFile ){
	//	if( (newFile == null) || newFile.exists() ) myUserDotExecutableLocation = newFile;
	//}
	//public File getUserDotExecutableLocation(){
	//	return myUserDotExecutableLocation;
	//}

	public static final String STR_TOKEN_GENERIC = "Property";
	public static final String STR_ATTR_KEY = "key";

	/** @since 020905 */
	private static String propertyToString( Object property ){
		if( property instanceof File ) return ((File)property).getAbsolutePath();
		else if( property == null ) return "";
		else return property.toString();
	}

	/** @since 020905 */
	public File getFile( String token ){
		Object property = getProperty( token );
		if( property == null ) return (File) null;
		else if( property instanceof File ) return (File) property;
		else return new File( property.toString() );
	}

	/** @since 020905 */
	public void putProperty( String token, Object property ){
		if( myMapTokensToObjects == null ) myMapTokensToObjects = new HashMap();
		myMapTokensToObjects.put( token, property );
	}

	/** @since 020905 */
	public Object getProperty( String token ){
		if( myMapTokensToObjects == null ) return null;
		else return myMapTokensToObjects.get( token );
	}

	private Map myMapTokensToObjects;

	public static final int INT_DEAFULT_MAX_RECENT_DOCUMENTS = (int)4;
	private int myNumMaxRecentDocuments = INT_DEAFULT_MAX_RECENT_DOCUMENTS;
	public static final String STR_RECENT_DOCUMENTS_TOKEN  = "RecentDocuments";
	public final LinkedList myListRecentDocuments = new LinkedList();

	public String lastEncounteredFileExtension = STR_DEFAULT_EXTENSION;
	public static final String STR_LAST_FILE_EXTENSION_TOKEN  = "FileExtension";
	public static final String STR_DEFAULT_EXTENSION = ".net";

	public String strLastDynamator;
	public static final String STR_LAST_DYNAMATOR_TOKEN  = "Algorithm";

	//public double secondsPerRCCall = (double)0.00000028571428571424;
	public static final String STR_SECONDS_PER_RC_CALL_TOKEN  = "SecondsPerRCCall";
	//public static final String STR_SECONDS_PER_RC_CALL = "0.00000028571428571424";

	public static boolean FLAG_DEBUG_PREFERENCES = false;//UI.DEBUG_VERBOSE;
	protected static int INT_READ_AHEAD_LIMIT = (int)1000;

	protected Map myMapPreferenceGroupNamesToPreferenceGroups = new HashMap();
	protected Map myMapKeysToPreferences                      = new HashMap();
	protected Collection myKeysUpdated;

	protected boolean myFlagFileIOSuccess = false;
	private static boolean flagAutoPreferenceFile = false;
	private File myFilePreferences;
	private boolean myFlagCalledCreateDefaultPreferences = false;

	/** @since 031103 */
	public void setLastDynamator( edu.ucla.belief.Dynamator dyn )
	{
		strLastDynamator = dyn.getKey().toString();
	}
	public String getLastDynamator()
	{
		return strLastDynamator;
	}

	/** @since 071202 */
	public SamiamPreferences( boolean doFileIO )
	{
		this( decideAutoFile( doFileIO ) );
	}

	/** @since 021104 */
	public SamiamPreferences( File fileInput )
	{
		createDefaultPreferences();
		myKeysUpdated  = new ArrayList();
		if( fileInput != null ){ myFlagFileIOSuccess = readOptionsFromFile( decideAutoFile( fileInput ) ); }
		//flagAutoPreferenceFile = false;
		finishingTouches();
		myKeysUpdated.clear();
		myKeysUpdated  = null;
	}

	/** @since 20050811 */
	private void finishingTouches(){
		addSimpleNetworks();
		addColorIntensitySample();
		addIrregularities();
		try{
			if( myKeysUpdated != null ){
				if( ! myKeysUpdated.contains( evidDlgAutoClr2 ) ){
					Preference preference = this.getMappedPreference( evidDlgAutoClr );
					preference.setValue( preference.getDefault() );
				}
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: SamiamPreferences.finishingTouches() caught " + thrown );
		}
	}

	/** @since 20100115 */
	public static File homeFile(){
		File ret = null;
		try{
			String classpath       = System.getProperty( "java.class.path", null );
			if( classpath         != null ){
				Matcher  match     = Pattern.compile( "(^|"+File.pathSeparator+")([^"+File.pathSeparator+"]*)samiam[.]jar" ).matcher( classpath );
				if( match.find() ){
					String    path = match.group( 2 );
					File   dirname = null;
					if( (path     != null) && (dirname = new File( path )).exists() && dirname.isDirectory() ){
						ret        = new File( dirname, STR_PREFERENCES_FILE_NAME );
					}
				}
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: SamiamPreferences.homeFile() caught " + thrown );
		}
		if( ret == null ){ ret = new File( STR_PREFERENCES_FILE_NAME ); }
		return ret;
	}

	/** @since 20040211 */
	public static File decideAutoFile( boolean doFileIO ){
		if( doFileIO ){
			flagAutoPreferenceFile = true;
			return homeFile();
		}
		else return null;
	}

	/** @since 20040211 */
	public static File decideAutoFile( File fileInput ){
		if( !flagAutoPreferenceFile && (fileInput == null || (!fileInput.exists())) ){
			System.err.println( "Warning: preferences file " + fileInput.getPath() + " does not exist." );
			flagAutoPreferenceFile = true;
			return homeFile();
		}
		else return fileInput;
	}

	/** @since 121902 */
	public boolean wasFileIOSuccessful()
	{
		return myFlagFileIOSuccess;
	}

	/** @since 071202 */
	public Iterator getPreferenceGroups()
	{
		return myMapPreferenceGroupNamesToPreferenceGroups.values().iterator();
	}

	/** @since 050603 */
	public Object putPreferenceGroup( PreferenceGroup pg )
	{
		return myMapPreferenceGroupNamesToPreferenceGroups.put( pg.getName(), pg );
	}

	/** @since 071502 */
	public void setRecentlyCommittedFlags( boolean flag )
	{
		for( Iterator it = getPreferenceGroups(); it.hasNext(); )
		{
			((PreferenceGroup)it.next()).setRecentlyCommittedFlags( flag );
		}
	}

	public static final String STR_ROOT_TAG = "SamiamPreferences";
	public static final String STR_GROUP_TAG = "PreferenceGroup";
	public static final String STR_NAME_ATTR = "name";


	/** @since 20020712 */
	public String toStringXML()
	{
		StringBuffer buff = new StringBuffer( 0x2000 );
		try{
			buff.append(   "<" ).append( STR_ROOT_TAG                  ).append( ">\n" );
			buff.append( "\t<" ).append( STR_DEFAULT_PATH_TOKEN        ).append( ">" ).append( defaultDirectory.getPath()       ).append( "</" ).append( STR_DEFAULT_PATH_TOKEN        ).append( ">\n" );
			buff.append( "\t<" ).append( STR_LAST_FILE_EXTENSION_TOKEN ).append( ">" ).append( lastEncounteredFileExtension     ).append( "</" ).append( STR_LAST_FILE_EXTENSION_TOKEN ).append( ">\n" );
			buff.append( "\t<" ).append( STR_SECONDS_PER_RC_CALL_TOKEN ).append( ">" ).append( RCSettings.getSecondsPerRCCall() ).append( "</" ).append( STR_SECONDS_PER_RC_CALL_TOKEN ).append( ">\n" );
			buff.append( "\t<" ).append( STR_RECENT_DOCUMENTS_TOKEN    ).append( ">" ).append( recentDocumentsToStringXML()     ).append( "</" ).append( STR_RECENT_DOCUMENTS_TOKEN    ).append( ">\n" );
			buff.append( "\t<" ).append( STR_LAST_DYNAMATOR_TOKEN      ).append( ">" ).append( strLastDynamator                 ).append( "</" ).append( STR_LAST_DYNAMATOR_TOKEN      ).append( ">\n" );
		}catch( Exception exception ){
			System.err.println( "warning: SamiamPreferences.toStringXML() caught " + exception );
		}

		try{
			if( myMapTokensToObjects != null ){
				Object key;
				for( Iterator it = myMapTokensToObjects.keySet().iterator(); it.hasNext(); ){
					key = it.next();
					buff.append( "\t<" ).append( STR_TOKEN_GENERIC ).append( " " ).append( STR_ATTR_KEY ).append( "=\"" ).append( key.toString() ).append( "\">" ).append( propertyToString( myMapTokensToObjects.get( key ) ) ).append( "</" ).append( STR_TOKEN_GENERIC ).append( ">\n" );
				}
			}
		}catch( Exception exception ){
			System.err.println( "warning: SamiamPreferences.toStringXML() caught " + exception );
		}

		try{
			for( Iterator it = getPreferenceGroups(); it.hasNext(); ){
				((PreferenceGroup)it.next()).appendXML( buff ).append( "\n" );
			}
		}catch( Exception exception ){
			System.err.println( "warning: SamiamPreferences.toStringXML() caught " + exception );
		}

		try{
			buff.append( "</" ).append( STR_ROOT_TAG ).append( ">\n" );
		}catch( Exception exception ){
			System.err.println( "warning: SamiamPreferences.toStringXML() caught " + exception );
		}
		return buff.toString();
	}

	/** @since 120502 */
	protected String recentDocumentsToStringXML()
	{
		String ret = "";
		String path;
		int numToSkip = myListRecentDocuments.size() - myNumMaxRecentDocuments;
		for( Iterator it = myListRecentDocuments.iterator(); it.hasNext(); )
		{
			path = it.next().toString();

			if( numToSkip > 0 ) --numToSkip;
			else ret +=  path + ";";
		}
		return ret;
	}

	/** @since 120502 */
	protected void setRecentDocuments( String xml )
	{
		myListRecentDocuments.clear();
		for( StringTokenizer toker = new StringTokenizer( xml, ";" ); toker.hasMoreTokens(); )
		{
			myListRecentDocuments.addLast( toker.nextToken() );
		}
	}

	/** @since 120502 */
	public boolean addRecentDocument( File doc )
	{
		String strPath = doc.getPath();
		if( myListRecentDocuments.contains( strPath ) )
		{
			if( myListRecentDocuments.getLast().equals(strPath) ) return false;
			else myListRecentDocuments.remove( strPath );
		}

		myListRecentDocuments.addLast( strPath );
		//if( myListRecentDocuments.size() > myNumMaxRecentDocuments ) myListRecentDocuments.removeFirst();

		saveOptionsToFile();
		return true;
	}

	/** @since 120502 */
	public void clearRecentDocuments()
	{
		myListRecentDocuments.clear();
		saveOptionsToFile();
	}

	/** @since 120502 */
	public boolean setMaxRecentDocuments( int newval )
	{
		if( myNumMaxRecentDocuments == newval ) return false;
		else
		{
			boolean ret;
			if( newval < myNumMaxRecentDocuments ) ret = newval < myListRecentDocuments.size();
			else ret = myListRecentDocuments.size() > myNumMaxRecentDocuments;

			myNumMaxRecentDocuments = newval;
			//boolean removed = false;
			//while( myListRecentDocuments.size() > myNumMaxRecentDocuments )
			//{
			//	myListRecentDocuments.removeFirst();
			//	removed = true;
			//}
			//return removed;
			return ret;
		}
	}

	/** @since 031803 */
	public int getMaxRecentDocuments()
	{
		return myNumMaxRecentDocuments;
	}

	/**
		Test/debug method.
		@author Keith Cascio
		@since 071202 */
	public static void main(String[] args)
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( e );
		}

		SamiamPreferences SP = new SamiamPreferences( false );

		Util.STREAM_TEST.println( "\n\nDebug/test of package edu.ucla.belief.ui.preference:\n\n" );
		Util.STREAM_TEST.println( "Writing file " + STR_PREFERENCES_FILE_NAME );
		SP.saveOptionsToFile();

		PackageOptionsDialog POD = new PackageOptionsDialog();
		for( Iterator it = SP.getPreferenceGroups(); it.hasNext(); )
		{
			POD.addPreferenceGroup( (PreferenceGroup) it.next() );
		}

		POD.pack();
		Util.centerWindow( POD );
		POD.setVisible( true );
	}

	/**
		Adds a Preference that can be retrieved with a
		call to getMappedPreference().
		@since 20060522 */
	public Preference addMappedPreference( Preference pref ){
		return (Preference) myMapKeysToPreferences.put( pref.getKey(), pref );
	}

	/**
		Retrieve a Preference obejct that was
		added to the PreferenceGroup via a call to
		addMappedPreference().
		@since 20060522 */
	public Preference getMappedPreference( Object key )
	{
		return (Preference) myMapKeysToPreferences.get( key );
	}

	/**
		Tries to find a constituent Preference with
		the specified key and then call parseValue( strValue ).
		@return The Preference object that was successfully updated, or null
			if the PreferenceGroup did not contain a Preference with
			the specified key.
	*/
	public Preference updatePreferenceValue( Object key, String strvalue ) throws Exception
	{
		Preference ret = getMappedPreference( key );
		if( ret == null ){ return null; }
		else{
			if( myKeysUpdated != null ){ myKeysUpdated.add( key ); }
			ret.parseValue( strvalue );
			return ret;
		}
	}

	/** @since 20020712 */
	protected void createDefaultPreferences()
	{
		if( myFlagCalledCreateDefaultPreferences ) throw new IllegalStateException();
		myFlagCalledCreateDefaultPreferences = true;

		this.add( createGlobalPreferenceGroup()    );
		this.add( createNetworkPreferenceGroup()   );
		this.add( createTreePreferenceGroup()      );
		this.add( createMonitorPreferenceGroup()   );
		this.add( createAnimationPreferenceGroup() );
		this.add( createInferencePreferenceGroup() );
		this.add( createGrepPreferenceGroup()      );
	}

	/** @since 20070402 */
	private PreferenceGroup add( PreferenceGroup temp ){
		if( temp != null ) myMapPreferenceGroupNamesToPreferenceGroups.put( temp.getName(), temp );
		return temp;
	}

	/** @since 20021104 */
	public void resetDefaults()
	{
		//myMapPreferenceGroupNamesToPreferenceGroups.clear();
		//createDefaultPreferences();
		//finishingTouches();

		for( Iterator it = myMapKeysToPreferences.values().iterator(); it.hasNext(); ){
			((Preference)it.next()).revert();
		}

		saveOptionsToFile();
	}

	/** @since 20020712 */
	public PreferenceGroup getPreferenceGroup( String namePreferenceGroup )
	{
		if( myMapPreferenceGroupNamesToPreferenceGroups.containsKey( namePreferenceGroup ) )
		{
			return (PreferenceGroup) myMapPreferenceGroupNamesToPreferenceGroups.get( namePreferenceGroup );
		}
		else return null;
	}

	/** @since 20050414 */
	public boolean isPreferenceGroupNameReserved( String namePreferenceGroup )
	{
		if( PkgDspNme.equals( namePreferenceGroup ) ) return true;
		else if( NetDspNme.equals( namePreferenceGroup ) ) return true;
		else if( TreeDspNme.equals( namePreferenceGroup ) ) return true;
		else if( MonitorsDspNme.equals( namePreferenceGroup ) ) return true;
		else if( AnimationDspNme.equals( namePreferenceGroup ) ) return true;
		else if( STR_KEY_GROUP_INFERENCE.equals( namePreferenceGroup ) ) return true;

		return false;
	}

	/** Read all the options.
		@author keith cascio
		@since  20030506 */
	private boolean readOptionsFromFile( File fileInput )
	{
		if( fileInput == null || (!fileInput.exists()) ) return false;
		else myFilePreferences = fileInput;

		boolean useSAX = true;
		try{
			Class.forName( "javax.xml.parsers.SAXParser" );
		}catch( ClassNotFoundException e ){
			useSAX = false;
		}

		if( useSAX ) return readOptionsFromFileSAX( fileInput );
		else return readOptionsFromFileHack( fileInput );
	}

	/** @since 20030506 */
	private boolean readOptionsFromFileSAX( File fileInput )
	{
		if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "SamiamPreferences.readOptionsFromFileSAX()" );

		PreferencesHandler ph = new PreferencesHandler();
		ph.setSamiamPreferences( this );

		try{
			ph.parse( fileInput );
		}catch( IOException e ){
			if( FLAG_DEBUG_PREFERENCES )
			{
				System.err.println( "WARNING: Package Options file error, using defaults.");
				System.err.println( e );
			}
			return false;
		}

		return true;
	}

	/** @since 20020712 */
	private boolean readOptionsFromFileHack( File fileInput )
	{
		if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "SamiamPreferences.readOptionsFromFileHack()" );

		BufferedReader in = null;
		try
		{
			in = new BufferedReader( new FileReader( fileInput ) );
		}
		catch( FileNotFoundException e) {
			if( FLAG_DEBUG_PREFERENCES )  System.err.println( "WARNING: No Package Options file found, using defaults.");
			return false;
		}
		catch( IOException e) {
			if( FLAG_DEBUG_PREFERENCES )  System.err.println( "WARNING: Package Options file error, using defaults.");
			if( FLAG_DEBUG_PREFERENCES ) System.err.println( e );
			return false;
		}
		catch( Exception e) {
			if( FLAG_DEBUG_PREFERENCES )  System.err.println( "WARNING: Package Options read error, using defaults.");
			if( FLAG_DEBUG_PREFERENCES )  System.err.println( e );
			return false;
		}

		try
		{
			return parseXML( in );
		}
		catch( IOException e ){
			if( FLAG_DEBUG_PREFERENCES )  System.err.println( "WARNING: Package Options file error, using defaults.");
			if( FLAG_DEBUG_PREFERENCES ) System.err.println( e );
			return false;
		}

	}

	/** This is NOT robust.

		@author keith cascio
		@since 20020717 */
	protected boolean parseXML( BufferedReader in ) throws IOException
	{
		if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "\n\nSamiamPreferences DEBUG information:\n" );
		if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "reading " + STR_PREFERENCES_FILE_NAME + " ...\n" );

		String tempLine = in.readLine();

		if( tempLine == null )
		{
			if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( STR_PREFERENCES_FILE_NAME + " is empty.\n" );
			return false;
		}

		if( !tempLine.equals("<" + STR_ROOT_TAG + ">") )
		{
			if( FLAG_DEBUG_PREFERENCES ) System.err.println( "Bad root tag line = '" + tempLine + "'" );
			return false;
		}
		in.mark( INT_READ_AHEAD_LIMIT );
		tempLine = in.readLine();
		int index1 = tempLine.indexOf( STR_DEFAULT_PATH_TOKEN );
		int index2 = tempLine.lastIndexOf( STR_DEFAULT_PATH_TOKEN );
		if( index1 > (int)-1 && index2 > (int)-1 ){
			String newpath = tempLine.substring( index1 + STR_DEFAULT_PATH_TOKEN.length() + 1, index2 - 2 );
			if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "defaultDirectory = '" + newpath + "'" );
			defaultDirectory = new File( newpath );
		}
		else
		{
			if( FLAG_DEBUG_PREFERENCES ) System.err.println( "Bad defaultDirectory line." );
			in.reset();
			//return false;
		}
		in.mark( INT_READ_AHEAD_LIMIT );
		tempLine = in.readLine();
		index1 = tempLine.indexOf( STR_LAST_FILE_EXTENSION_TOKEN );
		index2 = tempLine.lastIndexOf( STR_LAST_FILE_EXTENSION_TOKEN );
		if( index1 > (int)-1 && index2 > (int)-1 ){
			lastEncounteredFileExtension = tempLine.substring( index1 + STR_LAST_FILE_EXTENSION_TOKEN.length() + 1, index2 - 2 );
			if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "lastEncounteredFileExtension = '" + lastEncounteredFileExtension + "'" );
		}
		else
		{
			if( FLAG_DEBUG_PREFERENCES ) System.err.println( "Bad lastEncounteredFileExtension line." );
			in.reset();
			//return false;
		}
		in.mark( INT_READ_AHEAD_LIMIT );
		tempLine = in.readLine();
		index1 = tempLine.indexOf( STR_SECONDS_PER_RC_CALL_TOKEN );
		index2 = tempLine.lastIndexOf( STR_SECONDS_PER_RC_CALL_TOKEN );
		if( index1 > (int)-1 && index2 > (int)-1 ){
			String strSecondsPerRCCall = tempLine.substring( index1 + STR_SECONDS_PER_RC_CALL_TOKEN.length() + 1, index2 - 2 );
			double secondsPerRCCall = Double.parseDouble( strSecondsPerRCCall );
			RCSettings.setSecondsPerRCCall( secondsPerRCCall );
			if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "secondsPerRCCall = " + secondsPerRCCall );
		}
		else
		{
			if( FLAG_DEBUG_PREFERENCES ) System.err.println( "Bad secondsPerRCCall line." );
			in.reset();
			//return false;
		}
		in.mark( INT_READ_AHEAD_LIMIT );
		tempLine = in.readLine();
		index1 = tempLine.indexOf( STR_RECENT_DOCUMENTS_TOKEN );
		index2 = tempLine.lastIndexOf( STR_RECENT_DOCUMENTS_TOKEN );
		if( index1 > (int)-1 && index2 > (int)-1 ){
			String strRD = tempLine.substring( index1 + STR_RECENT_DOCUMENTS_TOKEN.length() + 1, index2 - 2 );
			setRecentDocuments( strRD );
			if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "recent documents = " + myListRecentDocuments );
		}
		else
		{
			if( FLAG_DEBUG_PREFERENCES ) System.err.println( "Bad recent documents line." );
			in.reset();
			//return false;
		}
		in.mark( INT_READ_AHEAD_LIMIT );
		tempLine = in.readLine();
		index1 = tempLine.indexOf( STR_LAST_DYNAMATOR_TOKEN );
		index2 = tempLine.lastIndexOf( STR_LAST_DYNAMATOR_TOKEN );
		if( index1 > (int)-1 && index2 > (int)-1 ){
			strLastDynamator = tempLine.substring( index1 + STR_LAST_DYNAMATOR_TOKEN.length() + 1, index2 - 2 );
			if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "last dynamator = " + strLastDynamator );
		}
		else
		{
			if( FLAG_DEBUG_PREFERENCES ) System.err.println( "Bad last dynamator line." );
			in.reset();
			//return false;
		}

		myPreferenceReader = new PreferenceReader();
		PreferenceGroup currentPreferenceGroup = null;
		String tempName = null;
		boolean not_reached_end = true;
		while( not_reached_end )
		{
			tempLine = in.readLine();
			index1 = tempLine.indexOf( "<PreferenceGroup name=\"" );
			if( index1 == (int)-1 )
			{
				index1 = tempLine.indexOf( STR_ROOT_TAG );
				if( index1 == (int)-1 ) continue;
				else
				{
					if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "Reached end of " + STR_PREFERENCES_FILE_NAME );
					not_reached_end = false;
				}
			}
			else
			{
				tempName = tempLine.substring( index1 + 23, tempLine.lastIndexOf( '"' ) );
				if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "\n\nFound PreferenceGroup '" + tempName + "'" );
				currentPreferenceGroup = getPreferenceGroup( tempName );
				if( currentPreferenceGroup == null )
				{
					currentPreferenceGroup = new PreferenceGroup( tempName );
					myMapPreferenceGroupNamesToPreferenceGroups.put( tempName, currentPreferenceGroup );
				}
				setPreferences( currentPreferenceGroup, in );
			}
		}

		if( FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "\n\nDONE SamiamPreferences DEBUG\n\n" );

		return true;
	}

	/** @since 071702 */
	protected PreferenceReader myPreferenceReader = null;

	/** @since 071702 */
	protected void setPreferences( PreferenceGroup currentPreferenceGroup, BufferedReader in ) throws IOException
	{
		String tempLine = null;
		boolean not_reached_end = true;
		int index1 = (int)0;
		Preference currentPreference = null;
		while( not_reached_end )
		{
			tempLine = in.readLine();
			index1 = tempLine.indexOf( "</PreferenceGroup>" );
			if( index1 == (int)-1 )
			{
				currentPreference = myPreferenceReader.readAndAdd( tempLine, this );
				currentPreferenceGroup.add( currentPreference );
				if( FLAG_DEBUG_PREFERENCES && currentPreference == null ) System.err.println( "Bad preference line: '" + tempLine + "'" );
			}
			else not_reached_end = false;
		}
	}

	/** Save all the options.
		@since 071202 */
	public void saveOptionsToFile()
	{
		if( Util.DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "SamiamPreferences.saveOptionsToFile()" );
		try {
			if( myFilePreferences == null ) myFilePreferences = decideAutoFile( true );
			BufferedWriter out = new BufferedWriter(new FileWriter( myFilePreferences ));
			out.write( toStringXML() );
			out.flush();
			out.close();
		}
		catch( Exception e) {
			System.err.println("WARNING: Package Options could not be written to the file.");
			if( FLAG_DEBUG_PREFERENCES )
			{
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
		}
	}

	public static final String
	  animationMinimumScale           = "animationMinimumScale",
	  animationMaximumScale           = "animationMaximumScale",
	  animationSlowdownMilliseconds   = "animationSlowdownMilliseconds",
	  animationSteps                  = "animationSteps",
	  animationColorComponent         = "animationColorComponent",
	  animationIntensifyEntropy       = "animationIntensifyEntropy",
	  animationScaleImpact            = "animationScaleImpact",
	  animationLockStep               = "animationLockStep",
	  animationReflect                = "animationReflect",
	  animationExponentiate           = "animationExponentiate",
	  animationResetFirst             = "animationResetFirst",
	  animationInterveningPauseMillis = "animationInterveningPauseMillis";

	/** Animation options
		@since 080404 */
	public PreferenceGroup createAnimationPreferenceGroup()
	{
		if( !Animator.FLAG_ENABLE_ANIMATION ) return (PreferenceGroup)null;

		PreferenceGroup ret = new PreferenceGroup( AnimationDspNme );

		this.addMappedPreference( ret.add( new IntegerPreference( animationSlowdownMilliseconds, "Slowdown (milliseconds)", new Integer( (int)AnimationPreferenceHandler.LONG_DELAY_MS_DEFAULT ), (int)AnimationPreferenceHandler.LONG_DELAY_MS_FLOOR, (int)AnimationPreferenceHandler.LONG_DELAY_MS_CEILING ) ) );
		this.addMappedPreference( ret.add( new IntegerPreference( animationSteps, "Steps per animation", new Integer( AnimationPreferenceHandler.INT_STEPS_DEFAULT ), AnimationPreferenceHandler.INT_STEPS_FLOOR, AnimationPreferenceHandler.INT_STEPS_CEILING ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( animationResetFirst, "Reset first*", AnimationPreferenceHandler.FLAG_RESET_FIRST_DEFAULT ? Boolean.TRUE : Boolean.FALSE ) ) );
		this.addMappedPreference( ret.add( new IntegerPreference( animationInterveningPauseMillis, "Intervening pause (milliseconds) (* checked)", new Integer( (int)AnimationPreferenceHandler.LONG_PAUSE_DEFAULT ), (int)AnimationPreferenceHandler.LONG_PAUSE_FLOOR, (int)AnimationPreferenceHandler.LONG_PAUSE_CEILING ) ) );

		this.addMappedPreference( ret.add( new BooleanPreference( animationScaleImpact, "Scale node size for evidence impact", AnimationPreferenceHandler.FLAG_SCALE_IMPACT_DEFAULT ? Boolean.TRUE : Boolean.FALSE ) ) );
		this.addMappedPreference( ret.add( new DoublePreference( animationMinimumScale, "Minimum scale factor", new Double( AnimationPreferenceHandler.DOUBLE_MIN_SCALE_FACTOR_DEFAULT ), AnimationPreferenceHandler.DOUBLE_MIN_SCALE_FACTOR_FLOOR, AnimationPreferenceHandler.DOUBLE_MIN_SCALE_FACTOR_CEILING ) ) );
		this.addMappedPreference( ret.add( new DoublePreference( animationMaximumScale, "Maximum scale factor", new Double( AnimationPreferenceHandler.DOUBLE_MAX_SCALE_FACTOR_DEFAULT ), AnimationPreferenceHandler.DOUBLE_MAX_SCALE_FACTOR_FLOOR, AnimationPreferenceHandler.DOUBLE_MAX_SCALE_FACTOR_CEILING ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( animationLockStep, "Scale at uniform rate", AnimationPreferenceHandler.FLAG_LOCK_STEP_DEFAULT ? Boolean.TRUE : Boolean.FALSE ) ) );

		this.addMappedPreference( ret.add( new BooleanPreference( animationIntensifyEntropy, "Intensify node color for entropy", AnimationPreferenceHandler.FLAG_INTENSIFY_ENTROPY_DEFAULT ? Boolean.TRUE : Boolean.FALSE ) ) );
		this.addMappedPreference( ret.add( new ObjectPreference( animationColorComponent, "Intensified color component", AnimationPreferenceHandler.ANIMATIONCOLORHANDLER_DEFAULT, AnimationPreferenceHandler.ARRAY_ANIMATIONCOLORHANDLERS, new ColorAnimationConverter() ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( animationReflect, "Reflect entropy", AnimationPreferenceHandler.FLAG_REFLECT_DEFAULT ? Boolean.TRUE : Boolean.FALSE ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( animationExponentiate, "Exponentiate entropy", AnimationPreferenceHandler.FLAG_EXPONENTIATE_DEFAULT ? Boolean.TRUE : Boolean.FALSE ) ) );

		return ret;
	}

	/** @since 080504 */
	public static class ColorAnimationConverter implements ObjectPreference.DomainConverter
	{
		public Object getDisplayName( Object obj ){
			return obj.toString();
		}

		public Object getValue( Object obj ){
			return obj;
		}

		public Object parseValue( String toParse ) throws Exception {
			return AnimationPreferenceHandler.forString( toParse );
		}

		public String valueToString( Object obj ){
			return obj.toString();
		}
	}

	public static final String
	  displayNodeLabelIfAvail      =    "displayNodeLabelIfAvail",
	  autoCalculatePrE             =    "autoCalculatePrE",
	  STR_LOOKANDFEEL_CLASSNAME    =    "lookAndFeelClassName",
	  maxRecentDocuments           =    "maxRecentDocuments",
	  statusBarPointSize           =    "statusBarPointSize",
	  autoCPTInvalid               =    "autoCPTInvalid",
	  nodesDisplayLikeliestValue   =    "nodesDisplayLikeliestValue",
	  animateStatusBarWest         =    "animateStatusBarWest",
	  animateStatusBarEast         =    "animateStatusBarEast",
	  STR_ASK_BEFORE_CPTCOPY       =    "askBeforeCPTCopy",
	  lazyInitialization           =    "lazyInitialization";

	/** contains global level options
		@since  20020712 */
	public PreferenceGroup createGlobalPreferenceGroup()
	{
		PreferenceGroup ret = new PreferenceGroup( PkgDspNme );

		this.addMappedPreference( ret.add( new BooleanPreference( displayNodeLabelIfAvail, "Show node labels instead of identifiers (if available)", Boolean.TRUE ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( autoCalculatePrE, "Show Pr(e) in status bar", Boolean.TRUE ) ) );
		this.addMappedPreference( ret.add( new IntegerPreference( statusBarPointSize, "Status bar point size", new Integer( (int)10 ) ) ) );

		UIManager.LookAndFeelInfo[] lookAndFeelInfos = javax.swing.UIManager.getInstalledLookAndFeels();
		//Object[] suitableForMenu = new Object[ lookAndFeelInfos.length ];
		//for( int i=0; i<suitableForMenu.length; i++ ) suitableForMenu[i] = lookAndFeelInfos[i].getClassName();
		this.addMappedPreference( ret.add( new ObjectPreference( STR_LOOKANDFEEL_CLASSNAME, "User interface look and feel", javax.swing.UIManager.getSystemLookAndFeelClassName(), lookAndFeelInfos, new LookAndFeelConverter() ) ) );

		this.addMappedPreference( ret.add( new IntegerPreference( maxRecentDocuments, "Maximum recent documents in File menu", new Integer( INT_DEAFULT_MAX_RECENT_DOCUMENTS ) ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( autoCPTInvalid, "Automatically invalidate CPTs", Boolean.FALSE ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( nodesDisplayLikeliestValue, "Nodes display likeliest value", Boolean.TRUE ) ) );

		this.addMappedPreference( ret.add( new BooleanPreference( animateStatusBarWest, "Animate status bar, west", Boolean.FALSE ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( animateStatusBarEast, "Animate status bar, east", Boolean.FALSE ) ) );

	  //this.addMappedPreference( ret.add( new BooleanPreference( lazyInitialization,     "Initialize network graphics lazily (speedup load time)", Boolean.FALSE ) ) );

		return ret;
	}

	/** @since 20020715 */
	public static class LookAndFeelConverter implements ObjectPreference.DomainConverter
	{
		public Object getDisplayName( Object obj )
		{
			return ((UIManager.LookAndFeelInfo)obj).getName();
		}

		public Object getValue( Object obj )
		{
			return ((UIManager.LookAndFeelInfo)obj).getClassName();
		}

		public Object parseValue( String toParse ) throws Exception
		{
			return toParse;
		}

		public String valueToString( Object obj )
		{
			return (String)obj;
		}
	}

	public static final String
	  inferenceMinFillRepetitions           =   "inferenceMinFillRepetitions",
	  inferenceMinFillSeed                  =   "inferenceMinFillSeed",
	  inferenceMAPExplorerFullScreenWidth   =   "inferenceMAPExplorerFullScreenWidth",
	  inferenceUnifyCompileSettings         =   "inferenceUnifyCompileSettings",
	  STR_CASTE                             =   "inferenceCaste";

	/** @since 20050811 */
	public PreferenceGroup createInferencePreferenceGroup()
	{
		PreferenceGroup ret = new PreferenceGroup( STR_KEY_GROUP_INFERENCE );

		this.addMappedPreference( ret.add( new IntegerPreference( inferenceMinFillRepetitions, "Min fill, best of N repetitions", new Integer( edu.ucla.belief.EliminationHeuristic.INT_MINFILL_REPS_DEFAULT ), 1, 999999 ) ) );
		this.addMappedPreference( ret.add( new IntegerPreference( inferenceMinFillSeed, "Min fill, new random seed (not saved)", new Integer( new Random().nextInt( Integer.MAX_VALUE ) ) ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( inferenceMAPExplorerFullScreenWidth, "MAP Solution Explorer, maximize width", Boolean.TRUE ) ) );

		this.addMappedPreference( ret.add( new BooleanPreference( STR_ASK_BEFORE_CPTCOPY, "Ask before modifying cpt (cpt copy tool)", Boolean.TRUE ) ) );
		try{
			Class        clazz       =   Class.forName( "edu.ucla.belief.ui.preference.EnumPreference$Caste" );
			Constructor  uctor       =   clazz.getConstructor( new Class[]{ String.class, String.class } );
			Preference   prefCaste   =   (Preference) uctor.newInstance( new Object[]{ STR_CASTE, "User level (cpt copy tool)" } );

			if( prefCaste != null ){ this.addMappedPreference( ret.add( prefCaste ) ); }
		}catch( Throwable throwable ){
			if( Util.DEBUG_VERBOSE ){
				System.err.println( "warning: SamiamPreferences.createInferencePreferenceGroup() caught " + throwable );
				throwable.printStackTrace();
			}
		}
		this.addMappedPreference( ret.add( new BooleanPreference( inferenceUnifyCompileSettings, "Couple sub-algorithm settings", Boolean.FALSE ).describe( "Yoke the compile settings for an approximate algorithm's sub-algorithm to the main settings for that algorithm" ) ) );
		//Compile settings affect all of one algorithm
		//Compile settings per network algorithm

		return ret;
	}

	/** @since 20050811 */
	private void addIrregularities(){
		//PreferenceGroup infGroup = this.getPreferenceGroup( STR_KEY_GROUP_INFERENCE );
		IntegerPreference pref = (IntegerPreference) this.getMappedPreference( inferenceMinFillSeed );
		pref.setValue( new Integer( new Random().nextInt( Integer.MAX_VALUE ) ) );
	}

	//net
	public static final String netBkgdClr = "netBkgdClr";
	public static final String netBkgdClrNeedsCompile = "netBkgdClrNeedsCompile";
	//node
	public static final String nodeShape = "nodeShape";
	public static final String nodeBorderClr = "nodeBorderClr";
	public static final String nodeBorderClrObserved = "nodeBorderClrObserved";
	public static final String nodeBkgndClr = "nodeBkgndClr";
	public static final String nodeTextClr = "nodeTextClr";
	public static final String nodeTextLikeliestValueClr = "nodeTextLikeliestValueClr";
	public static final String nodeTextFlippedValueClr = "nodeTextFlippedValueClr";
	public static final String nodeFontBold = "nodeFontBold";
	public static final String nodeLikeliestBreakLine = "nodeLikeliestBreakLine";
	public static final String netSizeOverRidesPrefs = "netSizeOverRidesPrefs";
	public static final String nodeDefaultSize = "nodeDefaultSize";
	public static final String nodeNormalStroke = "nodeNormalStroke";
	public static final String nodeObservedStroke = "nodeObservedStroke";
	public static final String nodeWideStroke = "nodeWideStroke";
	//edge
	public static final String edgeArrowTipSize = "edgeArrowTipSize";
	public static final String edgeClr = "edgeClr";
	public static final String arrowStroke = "arrowStroke";

	/** This class contains the options for the nework display window.
		@since 071202 */
	public PreferenceGroup createNetworkPreferenceGroup()
	{
		PreferenceGroup ret = new PreferenceGroup( NetDspNme );
		//net
		this.addMappedPreference( ret.add( new ColorPreference( netBkgdClr, "Network background color (compiled)", Color.lightGray ) ) );
		this.addMappedPreference( ret.add( new ColorPreference( netBkgdClrNeedsCompile, "Network background color (needs compile)", new Color( 150,127,143 ) ) ) );
		//node
		this.addMappedPreference( ret.add( new ObjectPreference( nodeShape, "Node shape", IconFactory.DEFAULT, IconFactory.ARRAY, new IconFactoryConverter() ) ) );
		this.addMappedPreference( ret.add( new ColorPreference( nodeBorderClr, "Node border color", Color.black ) ) );
		this.addMappedPreference( ret.add( new ColorPreference( nodeBorderClrObserved, "Node border color (observed)", Color.red ) ) );
		this.addMappedPreference( ret.add( new ColorPreference( nodeBkgndClr, "Node background color", new Color( 255,255,204 ) ) ) );
		this.addMappedPreference( ret.add( new ColorPreference( nodeTextClr, "Node name color", Color.blue ) ) );
		this.addMappedPreference( ret.add( new ColorPreference( nodeTextLikeliestValueClr, "Likeliest value color (stable)", new Color( 00,102,00 ) ) ) );
		this.addMappedPreference( ret.add( new ColorPreference( nodeTextFlippedValueClr, "Likeliest value color (flipped)", new Color( 255,255,255 ) ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( nodeLikeliestBreakLine, "Likeliest value always second line", Boolean.FALSE ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( nodeFontBold, "Nodes use bold font", Boolean.FALSE ) ) );

		this.addMappedPreference( ret.add( new DimensionPreference( nodeDefaultSize, "Default node size", new Dimension( 130,55 ) ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( netSizeOverRidesPrefs, "Use size in network file if available", Boolean.FALSE ) ) );
		this.addMappedPreference( ret.add( new DoublePreference( nodeNormalStroke, "Node border width (unselected)", new Double( (double)1.0 ), (double)0.1, (double)16 ) ) );
		this.addMappedPreference( ret.add( new DoublePreference( nodeObservedStroke, "Node border width (observed)", new Double( (double)3.0 ), (double)0.1, (double)16 ) ) );
		this.addMappedPreference( ret.add( new DoublePreference( nodeWideStroke, "Node border width (selected)", new Double( (double)5.0 ), (double)0.1, (double)16 ) ) );
		//edge
		this.addMappedPreference( ret.add( new DimensionPreference( edgeArrowTipSize, "Arrow tip size", new Dimension( 10, 6) ) ) );
		this.addMappedPreference( ret.add( new ColorPreference( edgeClr, "Edge color", Color.black ) ) );
		this.addMappedPreference( ret.add( new DoublePreference( arrowStroke, "Edge width", new Double( (double)1.0 ), (double)0.1, (double)16 ) ) );

		//((ColorPreference)this.getMappedPreference( nodeBkgndClr )).debug = true;

		return ret;
	}

	/** @since 081304 */
	public static class IconFactoryConverter extends ColorAnimationConverter implements ObjectPreference.DomainConverter
	{
		public Object parseValue( String toParse ) throws Exception {
			return IconFactory.forString( toParse );
		}
	}

	protected void addSimpleNetworks()
	{
		try{
		PreferenceGroup netPrefs = getPreferenceGroup( NetDspNme );
		PreferenceGroup monitorPrefs = getPreferenceGroup( MonitorsDspNme );

		SimpleNetwork simp = new SimpleNetwork( this );
		SimpleNetwork simp2 = new SimpleNetwork( this );

		netPrefs.userobject = simp;
		netPrefs.addActionListener( simp );
		netPrefs.addActionListener( simp2 );

		monitorPrefs.userobject = simp2;
		monitorPrefs.addActionListener( simp );
		monitorPrefs.addActionListener( simp2 );
		}catch( Exception exception ){
			System.err.println( "Warning: SamiamPreferences.addSimpleNetworks() caught: " + exception );
			exception.printStackTrace();
		}catch( Error error ){
			System.err.println( "Warning: SamiamPreferences.addSimpleNetworks() caught: " + error );
		}
	}

	/** @since 081004 */
	protected void addColorIntensitySample()
	{
		if( !Animator.FLAG_ENABLE_ANIMATION ) return;

		try{
		PreferenceGroup animationPrefs = getPreferenceGroup( AnimationDspNme );

		ColorIntensitySample sample = new ColorIntensitySample( (SamiamPreferences)this );

		animationPrefs.userobject = sample;
		animationPrefs.addActionListener( sample );
		getPreferenceGroup( NetDspNme ).addActionListener( sample );
		}catch( Exception exception ){
			System.err.println( "Warning: SamiamPreferences.addColorIntensitySample() caught: " + exception );
		}catch( Error error ){
			System.err.println( "Warning: SamiamPreferences.addColorIntensitySample() caught: " + error );
		}
	}

	//evidence dialog boxes (Monitors)
	public static final String
	  evidDlgManualClr             = "evidDlgManualClr",
	  evidDlgWarnClr               = "evidDlgWarnClr",
	  evidDlgAutoClr               = "evidDlgAutoClr",
	  evidDlgAutoClr2              = "evidDlgAutoClr2",
	  evidDlgRectSize              = "evidDlgRectSize",
	  evidDlgTextClr               = "evidDlgTextClr",
	  evidDlgView                  = "evidDlgView",
	  evidDlgMinimumFractionDigits = "evidDlgMinimumFractionDigits",
	  evidDlgZooms                 = "evidDlgZooms",
	  evidDlgZoomFactor            = "evidDlgZoomFactor";

	/** @since 20030703 */
	public static class FormatManagerConverter implements ObjectPreference.DomainConverter
	{
		public Object getDisplayName( Object obj )
		{
			return ((EvidenceLabel.FormatManager)obj).getDisplayName();
		}

		public Object getValue( Object obj )
		{
			return obj;
		}

		public Object parseValue( String toParse ) throws Exception
		{
			return EvidenceLabel.forString( toParse );
		}

		public String valueToString( Object obj )
		{
			return ((EvidenceLabel.FormatManager)obj).getDisplayName();
		}
	}

	/** @since  20021029 */
	public PreferenceGroup createMonitorPreferenceGroup()
	{
		PreferenceGroup ret = new PreferenceGroup( MonitorsDspNme );

		//evidence dialog boxes (Monitors)
		this.addMappedPreference( ret.add( new     ColorPreference( evidDlgManualClr,             "Observed evidence color",       Color.red                     ) ) );
		this.addMappedPreference( ret.add( new     ColorPreference( evidDlgWarnClr,               "Computation in progress color", Color.pink                    ) ) );
		this.addMappedPreference( ret.add( new     ColorPreference( evidDlgAutoClr,               "First unobserved color",        Color.green.darker()          ) ) );
		this.addMappedPreference( ret.add( new     ColorPreference( evidDlgAutoClr2,              "Second unobserved color",       Color.green.darker().darker().darker().darker() ) ) );
		this.addMappedPreference( ret.add( new DimensionPreference( evidDlgRectSize,              "Evidence bar size",         new Dimension( 25, 12 )           ) ) );
		this.addMappedPreference( ret.add( new     ColorPreference( evidDlgTextClr,               "Text color",                    Color.blue                    ) ) );
		this.addMappedPreference( ret.add( new    ObjectPreference( evidDlgView,                  "View", EvidenceLabel.PERCENT_MANAGER, EvidenceLabel.ARRAY_MANAGERS, new FormatManagerConverter() ) ) );
		this.addMappedPreference( ret.add( new   IntegerPreference( evidDlgMinimumFractionDigits, "Probability precision",     new Integer( 2 )                  ) ) );
		this.addMappedPreference( ret.add( new   BooleanPreference( evidDlgZooms,                 "Zoom with network*",            Boolean.FALSE                 ) ) );
		this.addMappedPreference( ret.add( new    DoublePreference( evidDlgZoomFactor,            "Zoom % (* not checked)", new Double( (double)100 ), (double)0.00000000023283064365386962890625, (double)65536 ) ) );

		return ret;
	}

	public static final String
	                           manualClr =       "manualClr",
	                             warnClr =         "warnClr",
	                       treeNormalClr =   "treeNormalClr",
	  STR_SHOW_TARGET_PROBABILITIES      =        "showTargetProbabilities",
	  STR_SHOW_AUXILIARY_PROBABILITIES   =     "showAuxiliaryProbabilities",
	  STR_SHOW_OBSERVATION_PROBABILITIES =   "showObservationProbabilities",
	  treeSortDefault                    =   "treeSortDefault";

	/** @since 20030910 */
	public static class EnumPropertyConverter implements ObjectPreference.DomainConverter
	{
		public Object getDisplayName( Object obj )
		{
			return ((EnumProperty)obj).getName();
		}

		public Object getValue( Object obj )
		{
			return obj;
		}

		public Object parseValue( String toParse ) throws Exception
		{
			return VariableImpl.forID( toParse );
		}

		public String valueToString( Object obj )
		{
			return ((EnumProperty)obj).getID();
		}
	}

	/** contains the options for the tree display window
		@since 20020712 */
	public PreferenceGroup createTreePreferenceGroup()
	{
		PreferenceGroup ret = new PreferenceGroup( TreeDspNme );
		this.addMappedPreference( ret.add( new ColorPreference( treeNormalClr, "Normal color",                  Color.black ) ) );
		this.addMappedPreference( ret.add( new ColorPreference(       warnClr, "Computation in progress color", Color.pink  ) ) );
		this.addMappedPreference( ret.add( new ColorPreference(     manualClr, "Observed evidence color",       Color.red   ) ) );
		//this.addMappedPreference( ret.add( new ColorPreference( autoClr, "Automatically Set Evidence Color", Color.green ) ) );
		//this.addMappedPreference( ret.add( new DimensionPreference( rectSize, "Evidence Bar size in tree", new Dimension( 25, 12) ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( STR_SHOW_TARGET_PROBABILITIES, "Show target probabilities", Boolean.TRUE ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( STR_SHOW_AUXILIARY_PROBABILITIES, "Show auxiliary probabilities", Boolean.FALSE ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( STR_SHOW_OBSERVATION_PROBABILITIES, "Show observation probabilities", Boolean.FALSE ) ) );

		EnumProperty[] copyPROPERTIES = new EnumProperty[ VariableImpl.getNumProperties() ];
		VariableImpl.propertiesArrayCopy( copyPROPERTIES );
		this.addMappedPreference( ret.add( new ObjectPreference( treeSortDefault, "Sort by (default)", InOutDegreeProperty.PROPERTY, copyPROPERTIES, new EnumPropertyConverter() ) ) );
		return ret;
	}

	public static final String STR_SHOW_GREP_BUTTON = "showGrepBtn",
	                           STR_GREP_VOCABULARY  = "grepVocabulary",
	                           STR_GREP_HIGHLIGHT   = "grepHightlightToggles",
	                           STR_GREP_TRIM        = "grepTrim",
	                           STR_GREP_AUTOSUGGEST = "grepAutosuggest";

	/** @since 20070402 */
	public PreferenceGroup createGrepPreferenceGroup()
	{
		PreferenceGroup ret = new PreferenceGroup( STR_KEY_GROUP_GREP );
		ret.setVisible( false );
		this.addMappedPreference( ret.add( new BooleanPreference( STR_SHOW_GREP_BUTTON, "Show \""+STR_GREP_DISPLAY_NAME_LOWER+"\"/\"grep\" button", Boolean.TRUE ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( STR_GREP_HIGHLIGHT,   "Highlight toggle buttons",                                 Boolean.TRUE ) ) );
		this.addMappedPreference( ret.add( new BooleanPreference( STR_GREP_TRIM,        "Omit leading and trailing whitespace",                     Boolean.TRUE ) ) );
	  //this.addMappedPreference( ret.add( new BooleanPreference( STR_GREP_AUTOSUGGEST, "Turn on \"auto-suggest\" (interactive help)",              Boolean.TRUE ) ) );
		try{
			//this.addMappedPreference( ret.add( new LingoPreference(    STR_GREP_VOCABULARY,  "vocab",                         edu.ucla.belief.ui.actionsandmodes.Grepable.Vocabulary.basic() ) ) );
			Class      clazz      = Class.forName( "edu.ucla.belief.ui.preference.LingoPreference" );
			Method     method     = clazz.getMethod( "bootstrap", (Class[]) null );
			Preference prefLingo  = (Preference) method.invoke( null, (Object[]) null );

			clazz                 = Class.forName( "edu.ucla.belief.ui.preference.EnumPreference$Autosuggest" );
			Constructor uctor     = clazz.getConstructor( new Class[]{ String.class, String.class } );
			Preference  prefAutos = (Preference) uctor.newInstance( new Object[]{ STR_GREP_AUTOSUGGEST, "Auto-suggest (interactive help)" } );

			if( prefAutos != null ) this.addMappedPreference( ret.add( prefAutos ) );
			if( prefLingo != null ) this.addMappedPreference( ret.add( prefLingo ) );

			ret.setVisible( true );
		}catch( Throwable throwable ){
			if( Util.DEBUG_VERBOSE ){
				System.err.println( "warning: SamiamPreferences.createGrepPreferenceGroup() caught " + throwable );
				throwable.printStackTrace();
			}
		}
		return ret;
	}
}
