package edu.ucla.util;

import java.util.*;

/** @author keith cascio
	@since  20030820 */
public abstract class FlagProperty extends AbstractEnumProperty
{
	public boolean isFlag()
	{
		return true;
	}
	public boolean contains( EnumValue val )
	{
		return myList.contains( val );
	}
	public EnumValue forString( String str )
	{
		return str.equals( "true" ) ? TRUE : FALSE;
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
	public boolean toBoolean( EnumValue value )
	{
		if( value == null ){
			EnumValue defaultval = this.getDefault();
			if( defaultval == null ) return false;
			else return toBoolean( defaultval );
		}
		else return value == TRUE;
	}
	public EnumValue valueOf( boolean flag )
	{
		return flag ? TRUE : FALSE;
	}

	public class FlagValue implements EnumValue
	{
		public FlagValue( boolean flag )
		{
			value = flag ? Boolean.TRUE : Boolean.FALSE;
		}

		public String toString()
		{
			return value.toString();
		}

		public boolean equals( Object o )
		{
			//System.out.println( "(FlagValue)"+this+".equals("+o+")" );
			if( o == null ) return false;
			else return o.toString().equals( value.toString() );
		}

		public int hashCode()
		{
			return value.toString().hashCode();
		}

		/** @since 20070420 */
		public EnumProperty property(){
			return FlagProperty.this;
		}

		private Boolean value;
	}

	public FlagValue getValue( boolean flag )
	{
		return flag ? TRUE : FALSE;
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

	public final FlagValue   TRUE    = new FlagValue( true  );
	public final FlagValue   FALSE   = new FlagValue( false );
	public final EnumValue[] myArray = new EnumValue[]{ TRUE, FALSE };
	public final List        myList  = Arrays.asList( myArray );
}
