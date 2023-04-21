package edu.ucla.util;

//{superfluous} import java.util.Iterator;

/** @author keith cascio
	@since 20031009 */
public abstract class AbstractEnumProperty implements EnumProperty
{
	public String toString()
	{
		return getName();
	}
	public boolean isFlag()
	{
		return false;
	}
	public boolean isUserEditable()
	{
		return true;
	}
	public boolean isTransient()
	{
		return false;
	}
	public boolean isModified()
	{
		return false;
	}
	public boolean toBoolean( EnumValue value )
	{
		return value == getDefault();
	}

	/** @since 20070324 */
	static public EnumValue rotate( EnumProperty property, EnumValue current ) throws Exception{
		int index = property.indexOf( current );
		int size  = property.size();
		if( (index < 0) || (size <= index) ) return null;
		++index;
		if( index == size ) index = 0;
		return property.forIndex( index );
	}
}
