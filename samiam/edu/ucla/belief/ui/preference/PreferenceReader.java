package edu.ucla.belief.ui.preference;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.*;

/**
	A temporary (parsing not robust) class for parsing XML representations of
	Preference objects.
	@author Keith Cascio
	@since 071702
*/
public class PreferenceReader
{
	public PreferenceReader(){}

	public static final String STR_CLASSNAME_PREFIX = "edu.ucla.belief.ui.preference.",
	                           STR_ATTR_KEY         = "key",
	                           STR_ATTR_VALUE       = "value",
	                           STR_ATTR_NAME        = "name",
	                           STR_ATTR_CLASS       = "class";

	protected static final String TOKEN_KEY = " "+STR_ATTR_KEY+"=\"";
	protected static final String TOKEN_VALUE = "\" "+STR_ATTR_VALUE+"=\"";
	protected static final String TOKEN_NAME = "\" "+STR_ATTR_NAME+"=\"";
	protected static final String TOKEN_POSTFIX = "\" />";
	protected static final int INT_SUBSTRING_NOT_FOUND = (int)-1;
	protected static final Class[] ARG_TYPES_CONSTRUCTOR = { String.class, String.class, String.class };

	protected static Map theMapClassesToConstructors = new HashMap();

	/** @author keith cascio
		@since  20030506 */
	public Preference readAndAdd( String className, String strkey, String strvalue, String strname, SamiamPreferences PG )
	{
		Class theClass = null;
		try{
			theClass = Class.forName( className );
		}catch( Throwable e ){
			if( SamiamPreferences.FLAG_DEBUG_PREFERENCES ) edu.ucla.belief.ui.util.Util.STREAM_VERBOSE.println( e );
			return null;
		}

		Preference ret = null;
		if( PG != null )
		{
			try{
				ret = PG.updatePreferenceValue( strkey, strvalue );
			}catch( Exception e ){
				if( SamiamPreferences.FLAG_DEBUG_PREFERENCES ) System.err.println( e );
				return null;
			}
		}
		if( ret == null )
		{
			ret = makePreference( theClass, strkey, strname, strvalue );
			if( ret != null && PG != null ) PG.addMappedPreference( ret );
		}
		return ret;
	}

	public Preference readAndAdd( String line, SamiamPreferences PG )
	{
		int index1 = line.indexOf( STR_CLASSNAME_PREFIX );
		if( index1 == INT_SUBSTRING_NOT_FOUND ) return null;
		else
		{
			int index2 = line.indexOf( TOKEN_KEY );
			if( index2 == INT_SUBSTRING_NOT_FOUND ) return null;
			else
			{
				String className = line.substring( index1 + STR_CLASSNAME_PREFIX.length(), index2 );
				if( SamiamPreferences.FLAG_DEBUG_PREFERENCES ) edu.ucla.belief.ui.util.Util.STREAM_VERBOSE.println( "\treading " + className );

				Class theClass = null;
				try{
					theClass = Class.forName( STR_CLASSNAME_PREFIX + className );
				}catch( ClassNotFoundException e ){
					if( SamiamPreferences.FLAG_DEBUG_PREFERENCES ) edu.ucla.belief.ui.util.Util.STREAM_VERBOSE.println( e );
					return null;
				}

				int index3 = line.indexOf( TOKEN_VALUE );
				if( index3 == INT_SUBSTRING_NOT_FOUND ) return null;
				else
				{
					String strkey = line.substring( index2 + TOKEN_KEY.length(), index3 );
					int index4 = line.indexOf( TOKEN_NAME );
					if( index4 == INT_SUBSTRING_NOT_FOUND ) return null;
					else
					{
						String strvalue = line.substring( index3 + TOKEN_VALUE.length(), index4 );
						int index5 = line.indexOf( TOKEN_POSTFIX );
						if( index5 == INT_SUBSTRING_NOT_FOUND ) return null;
						else
						{
							String strname = line.substring( index4 + TOKEN_NAME.length(), index5 );
							Preference ret = null;
							if( PG != null )
							{
								try{
									ret = PG.updatePreferenceValue( strkey, strvalue );
								}catch( Exception e ){
									if( SamiamPreferences.FLAG_DEBUG_PREFERENCES ) System.err.println( e );
									return null;
								}
							}
							if( ret == null )
							{
								ret = makePreference( theClass, strkey, strname, strvalue );
								if( ret != null && PG != null ) PG.addMappedPreference( ret );
							}
							return ret;
						}
					}
				}
			}
		}
	}

	static protected Preference makePreference( Class theClass, String strkey, String strname, String strvalue )
	{
		Constructor c = getConstructor( theClass );
		if( c == null ) return null;
		else
		{
			String[] initargs = { strkey, strname, strvalue };
			try{
				Preference ret = (Preference) c.newInstance( (Object[])initargs );//strkey, strname, strvalue );
				if( SamiamPreferences.FLAG_DEBUG_PREFERENCES ) edu.ucla.belief.ui.util.Util.STREAM_VERBOSE.println( "\tnew " + getClassNameLastComponent( theClass ) + "( \"" + strkey + "\", \"" + strname + "\", \"" + strvalue + "\" )" );
				return ret;
			}catch( Exception e ){
				if( SamiamPreferences.FLAG_DEBUG_PREFERENCES ) System.err.println( "Constructor: " + c + " failed." );
				return null;
			}
		}
	}

	static protected Constructor getConstructor( Class theClass )
	{
		Constructor c = (Constructor) theMapClassesToConstructors.get( theClass );
		if( c == null )
		{
			try{
				c = theClass.getConstructor( ARG_TYPES_CONSTRUCTOR );
				theMapClassesToConstructors.put( theClass, c );
				return c;
			}catch( Exception e ){
				if( SamiamPreferences.FLAG_DEBUG_PREFERENCES ) System.err.println( e );
				return null;
			}
		}
		else return c;
	}

	/**
		@author Keith Cascio
		@since 071702
	*/
	static public String getClassNameLastComponent( Class c )
	{
		String name = c.getName();
		return name.substring( name.lastIndexOf( '.' ) + (int)1 );
	}
}
