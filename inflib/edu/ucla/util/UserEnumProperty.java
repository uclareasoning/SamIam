package edu.ucla.util;

import java.util.*;

/** @author keith cascio
	@since 20030827 */
public class UserEnumProperty extends AbstractEnumProperty
{
	public UserEnumProperty()
	{
		myName = "property" + Integer.toString( INT_NAME_COUNTER++ );
		myID = myName;
		myIsFlag = true;
	  //myArray = (EnumValue[]) FlagProperty.PROPERTY.myArray.clone();
	    myArray = new EnumValue[]{ new UserEnumValue( "true", this ), new UserEnumValue( "false", this ) };
		myList = Arrays.asList( myArray );
		myFlagValueTrue = myArray[0];
		myDefaultValue = myFlagValueFalse = myArray[1];
		myFlagModified = true;
	}

	public static String createString( EnumProperty property )
	{
		String ret = property.getName().toString() + "," + property.getDefault().toString() + "," + Boolean.toString( property.isFlag() ) + ",";
		for( Iterator it = property.iterator(); it.hasNext(); )
		{
			ret += it.next().toString() + ",";
		}
		return ret;
	}

	public UserEnumProperty( UserEnumProperty toCopy )
	{
		assume( toCopy );
		this.myArray = new EnumValue[ toCopy.myArray.length ];
		EnumValue valToCopy;
		EnumValue valCopied;
		for( int i=0; i<myArray.length; i++ ){
			valToCopy = toCopy.myArray[i];
			this.myArray[i] = valCopied = new UserEnumValue( valToCopy );
			if( valToCopy == toCopy.myFlagValueTrue ) this.myFlagValueTrue = valCopied;
			if( valToCopy == toCopy.myFlagValueFalse ) this.myFlagValueFalse = valCopied;
			if( valToCopy == toCopy.myDefaultValue ) this.myDefaultValue = valCopied;
		}
		if( this.myFlagValueTrue == null ) this.myFlagValueTrue = myArray[0];
		if( this.myFlagValueFalse == null ) this.myFlagValueFalse = myArray[1];
		if( this.myDefaultValue == null ) this.myDefaultValue = myArray[1];

		this.myList = Arrays.asList( this.myArray );
		this.myFlagModified = false;
	}

	public void assume( UserEnumProperty toCopy )
	{
		//System.out.println( "(UserEnumProperty)"+this.getDebugLabel()+".assume( "+getDebugLabel(toCopy)+" )" );
		myName = toCopy.myName;
		myID = toCopy.myID;
		myIsFlag = toCopy.myIsFlag;
		myArray = toCopy.myArray;
		myList = toCopy.myList;
		myFlagValueTrue = toCopy.myFlagValueTrue;
		myFlagValueFalse = toCopy.myFlagValueFalse;
		myDefaultValue = toCopy.myDefaultValue;
		myFlagModified = true;
	}

	public void setValues( EnumValue[] values )
	{
		//System.out.print( "(UserEnumProperty)"+this.getDebugLabel()+".setValues( (EnumValue[])[ " );
		//for( int i=0; i<values.length; i++ ) System.out.print( values[i] + " " );
		//System.out.println( "] )" );

		if( values.length < 2 ) return;

		myArray = values;
		myList = Arrays.asList( myArray );
		myDefaultValue = myFlagValueTrue = myArray[0];
		myFlagValueFalse = myArray[1];

		setModified( true );
	}

	public void setValues( List values )
	{
		//System.out.println( "(UserEnumProperty)"+this.getDebugLabel()+".setValues( List )" );
		if( values.size() < 2 ) return;

		myList = values;
		myArray = (EnumValue[]) values.toArray( new EnumValue[myList.size()] );
		myDefaultValue = myFlagValueTrue = myArray[0];
		myFlagValueFalse = myArray[1];

		setModified( true );
	}

	public String getName()
	{
		return myName;
	}

	public String getID()
	{
		return myID;
	}

	public void setName( String name )
	{
		myName = name;
		if( myID == null ) myID = name;
		setModified( true );
	}

	public void setID( String id )
	{
		myID = id;
		setModified( true );
	}

	public boolean isFlag()
	{
		return myIsFlag;
	}

	public void setIsFlag( boolean is )
	{
		//System.out.println( "(UserEnumProperty)"+this.getDebugLabel()+".setIsFlag( "+is+" )" );
		myIsFlag = is;
		setModified( true );
	}

	public boolean toBoolean( EnumValue value )
	{
		return value == myFlagValueTrue;
	}

	public EnumValue valueOf( boolean flag )
	{
		return flag ? myFlagValueTrue : myFlagValueFalse;
	}

	public boolean contains( EnumValue val )
	{
	  //return myList.contains( val );
		for( int i=0; i<myArray.length; i++ ) if( myArray[i] == val ) return true;
		return false;
	}

	public EnumValue forString( String str )
	{
		EnumValue ret = null;
		for( int i=0; i<myArray.length; i++ ){
			if( myArray[i].toString().equals( str ) ){
				ret = myArray[i];
				break;
			}
		}
		//System.out.println( "(UserEnumProperty)"+this.getDebugLabel()+".forString("+str+") == " + ret );
		return ret;
	}

	public Iterator iterator()
	{
		return myList.iterator();
	}

	public EnumValue[] valuesAsArray()
	{
		return myArray;
	}

	public int size()
	{
		return myArray.length;
	}

	public EnumValue getDefault()
	{
		return myDefaultValue;
	}

	public void setDefault( EnumValue def )
	{
		//System.out.println( "(UserEnumProperty)"+this.getDebugLabel()+".setDefault( "+def+" )" );
		myDefaultValue = def;
		setModified( true );
	}

	/** @since 010804 */
	public boolean isModified()
	{
		return myFlagModified;
	}

	/** @since 010804 */
	public void setModified( boolean flag )
	{
		myFlagModified = flag;
	}

	/** @since 020904 */
	public String getDebugLabel(){
		return myName + myDebugID;
	}

	/** @since 020904 */
	public void setDebugID( String id ){
		myDebugID = id;
	}

	/** @since 020904 */
	public static String getDebugLabel( UserEnumProperty property ){
		return (property==null) ? null : property.getDebugLabel();
	}

	/** @since 20050823 */
	public int indexOf( EnumValue value ){
		for( int i=0; i<myArray.length; i++ ){
			if( myArray[i] == value ) return i;
		}
		return -1;
	}

	/** @since 20050823 */
	public EnumValue forIndex( int index ){
		if( (-1 < index) && (index < myArray.length) ) return myArray[index];
		else return (EnumValue) null;
	}

	private String myName;
	private String myID;
	private boolean myIsFlag;
	private EnumValue myFlagValueTrue;
	private EnumValue myFlagValueFalse;
	private EnumValue myDefaultValue;
	private List myList;
	private EnumValue[] myArray;
	private boolean myFlagModified = true;
	private String myDebugID = "";

	public static int INT_NAME_COUNTER = (int)0;
}
