package edu.ucla.belief.io.hugin;

//import edu.ucla.belief.io.*;
//import edu.ucla.belief.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
//{superfluous} import edu.ucla.util.EnumProperty;
//{superfluous} import edu.ucla.util.EnumValue;
//{superfluous} import edu.ucla.util.AbstractEnumProperty;

/**
	A class that represents an enumerated type
	over Hugin file versions.

	@author Keith Cascio
	@since 012204
*/
public class HuginFileVersion
{
	private static List myValues = null;

	public static final HuginFileVersion V57 =
		new HuginFileVersion( "v57","V57 description", true );
	public static final HuginFileVersion V61 =
		new HuginFileVersion( "v61","V61 description", false );
	public static final HuginFileVersion UNSPECIFIED =
		new HuginFileVersion( "unspecified","unspecified", false );

	/** Call this method to get the standardized string representation of a HuginFileVersion. */
	public String toString()
	{
		return myCode;
	}

	public boolean shouldReflectY()
	{
		return myFlagReflectY;
	}

	private HuginFileVersion(){}

	private HuginFileVersion( String code, String description, boolean reflectY )
	{
		if ( myValues == null ) myValues = new ArrayList();
		this.myCode = code;
		this.myDescription = description;
		this.myFlagReflectY = reflectY;
		myValues.add( this );
	}

	public static HuginFileVersion[] valuesAsArray()
	{
		return _array_type = (HuginFileVersion[])(myValues.toArray( _array_type ));
	}

	public static Iterator iterator()
	{
		return myValues.iterator();
	}

	public static HuginFileVersion forString( String code )
	{
		Iterator it = myValues.iterator();
		HuginFileVersion temp = null;
		while( it.hasNext() )
		{
			temp = (HuginFileVersion)(it.next());
			if( code.equals( temp.myCode ) )
			{
				return temp;
			}
		}
		return null;
	}

	private static HuginFileVersion[] _array_type = { new HuginFileVersion() };
	private String myCode = null;
	private String myDescription = null;
	private boolean myFlagReflectY = false;
}
