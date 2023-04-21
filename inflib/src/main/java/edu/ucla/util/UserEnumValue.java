package edu.ucla.util;

/** @author keith cascio
	@since 20030828 */
public class UserEnumValue implements EnumValue
{
	public UserEnumValue( Object name, EnumProperty property ){
		myName          = name;
		if( (myProperty = property) == null ) throw new IllegalArgumentException( "attempt to create UserEnumValue \""+name+"\" with null EnumProperty" );
	}

	/** @since 20040209 */
	public UserEnumValue( EnumValue toCopy ){
		myName     = toCopy.toString();
		myProperty = toCopy.property();
	}

	public String toString()
	{
		return myName.toString();
	}

	public void setName( Object name )
	{
		myName = name;
	}

	public int hashCode()
	{
		return myName.hashCode();
	}

	public boolean equals( Object o )
	{
		//System.out.println( "(UserEnumValue)"+this+".equals("+o+")" );
		if( o == null ) return false;
		else return o.toString().equals( toString() );
	}

	/** @since 20070420 */
	public EnumProperty property(){
		return myProperty;
	}

	private Object       myName;
	private EnumProperty myProperty;
}
