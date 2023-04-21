package edu.ucla.belief.ui.preference;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.UIManager;
import java.awt.event.ActionListener;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.dialogs.*;
import edu.ucla.belief.ui.util.Util;

/** This class represents a group of Preference objects.
	PreferenceGroup makes it easier to perform operations
	on a set of Preferences.

	@author keith cascio
	@since 20020712 */
public class PreferenceGroup
{
	public    List    preferences   = new LinkedList();
	protected String  myName        = null;
	protected boolean myFlagVisible = true;
	//protected Map myMapKeysToPreferences = new HashMap();

	/**
		Construct a PreferenceGroup with a unique name.
	*/
	public PreferenceGroup( String name )
	{
		myName = name;
	}

	/** @since 20070403 */
	public boolean isVisible(){
		return this.myFlagVisible;
	}

	/** @since 20070403 */
	public boolean setVisible( boolean flag ){
		return this.myFlagVisible = flag;
	}

	/**
		Convenience method.
		Registers AL as a listener on every
		constituent Preference in this PreferenceGroup.

		@author Keith Cascio
		@since 071802
	*/
	public void addActionListener( ActionListener AL )
	{
		for( Iterator it = preferences.iterator(); it.hasNext(); )
		{
			((Preference)it.next()).addActionListener( AL );
		}
	}

	/**
		Allows a user program to associate some Object,
		perhaps a JComponent, with a PreferenceGroup.

		@author Keith Cascio
		@since 071802
	*/
	public Object userobject = null;

	/**
		Call this method to ask a PreferenceGroup
		whether any of its constituent Preference objects
		have had their GUI component 'edited' by the user.
	*/
	public boolean isComponentEdited()
	{
		for( Iterator it = preferences.iterator(); it.hasNext(); )
		{
			if( ((Preference) it.next()).isComponentEdited() ) return true;
		}

		return false;
	}

	/**
		PreferenceGroup will call commitValue()
		on its constituent Preference objects
		if they have had their GUI component 'edited' by the user.
		@return Whether any Preferences were, in fact, edited.
	*/
	public boolean commitValues()
	{
		Preference tempPreference = null;
		boolean changed = false;

		for( Iterator it = preferences.iterator(); it.hasNext(); )
		{
			tempPreference = (Preference) it.next();
			if( tempPreference.isComponentEdited() )
			{
				changed = true;
				tempPreference.commitValue();
			}
		}

		return changed;
	}

	/**
		Call this method to ask a PreferenceGroup
		whether commitValue() has been called on any of its
		constituent Preference objects since the last call
		to resetRecentlyCommittedFlags().
	*/
	public boolean isRecentlyCommittedValue()
	{
		for( Iterator it = preferences.iterator(); it.hasNext(); )
		{
			if( ((Preference) it.next()).isRecentlyCommittedValue() ) return true;
		}

		return false;
	}

	/**
		Calls resetRecentlyCommittedFlag() on all
		constituent Preference objects.
	*/
	public void setRecentlyCommittedFlags( boolean flag )
	{
		for( Iterator it = preferences.iterator(); it.hasNext(); )
		{
			((Preference) it.next()).setRecentlyCommittedFlag( flag );
		}
	}

	/**
		Adds a Preference that can be retrieved with a
		call to getMappedPreference().
	*/
	//public void addMappedPreference( Preference pref )
	public Preference add( Preference pref )
	{
		/*Object key = pref.getKey();
		Preference existing = null;
		if( myMapKeysToPreferences.containsKey( key ) )
		{
			existing = findPreferenceForKey( key );
			if( existing != null )
			{
				if( SamiamPreferences.FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "\tReplacing \"" + existing.getDisplayName() + "\"" );
				preferences.remove( existing );
			}
		}*/

		if( !preferences.contains( pref ) ) preferences.add( pref );
		//myMapKeysToPreferences.put( key, pref );

		return pref;
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
		Preference ret = findPreferenceForKey( key );//getMappedPreference( key );
		if( ret == null ) return null;
		else
		{
			ret.parseValue( strvalue );
			if( SamiamPreferences.FLAG_DEBUG_PREFERENCES ) Util.STREAM_VERBOSE.println( "\t" + key + "-(new value)->'" + strvalue + "'" );
			return ret;
		}
	}

	protected Preference findPreferenceForKey( Object key )
	{
		Preference temp = null;
		for( Iterator it = preferences.iterator(); it.hasNext(); )
		{
			temp = (Preference) it.next();
			if( temp.getKey().equals( key ) ) return temp;
		}

		return null;
	}

	/**
		Retrieve a Preference obejct that was
		added to the PreferenceGroup via a call to
		addMappedPreference().
	*/
	/*public Preference getMappedPreference( Object key )
	{
		return (Preference) myMapKeysToPreferences.get( key );
	}*/

	/**
		A convenience method for retrieving the
		value of a ColorPreference.
	*/
	/*public Color getColor( Object key )
	{
		return (Color) getMappedPreference( key ).getValue();
	}*/

	public String getName(){ return myName; }

	/**
		@return An XML string representation of this PreferenceGroup
			made by calling toStringXML() on all constituent
			Preference objects.
	*/
	public StringBuffer appendXML( StringBuffer buff ){
		buff.append( "\t<PreferenceGroup name=\"" ).append( getName() ).append( "\">\n" );
		for( Iterator it = preferences.iterator(); it.hasNext(); ){
			buff.append( "\t\t" );
			((Preference) it.next()).appendXML( buff ).append( '\n' );
		}
		return buff.append( "\t</PreferenceGroup>" );
	}

	/**
		Test/debug method.
		@author Keith Cascio
		@since 071202
	*/
	public static void main(String[] args)
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( e );
		}

		PreferenceGroup PG = new PreferenceGroup( "DEBUG" );
		Collection C = PG.preferences;

		C.add( new BooleanPreference( "skyBlue", "Is the sky blue?", new Boolean( true ) ) );
		C.add( new IntegerPreference( "numFingers", "Number of fingers", new Integer( 5 ) ) );
		C.add( new DoublePreference( "piValue", "Value of pi", new Double( Math.PI ) ) );
		C.add( new ColorPreference( "bullColor", "Bull fighting color", Color.red ) );
		C.add( new DimensionPreference( "roomSize", "Size of your room", new Dimension( 11,14 ) ) );

		UIManager.LookAndFeelInfo[] lookAndFeelInfos = javax.swing.UIManager.getInstalledLookAndFeels();
		//Object[] suitableForMenu = new Object[ lookAndFeelInfos.length ];
		//for( int i=0; i<suitableForMenu.length; i++ ) suitableForMenu[i] = lookAndFeelInfos[i].getName();
		C.add( new ObjectPreference( "STR_LOOKANDFEEL_CLASSNAME", "User interface look and feel", lookAndFeelInfos[0], lookAndFeelInfos, new SamiamPreferences.LookAndFeelConverter() ) );

		Util.STREAM_TEST.println( "\n\nDebug/test of package edu.ucla.belief.ui.preference:\n\n" );
		Util.STREAM_TEST.println( PG.appendXML( new StringBuffer() ).toString() );

		PackageOptionsDialog POD = new PackageOptionsDialog();
		POD.addPreferenceGroup( PG );

		POD.pack();
		Util.centerWindow( POD );
		POD.setVisible( true );
	}
}
