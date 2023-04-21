package edu.ucla.util;

import java.util.Iterator;

/** @author keith cascio
	@since  20030819 */
public interface EnumProperty
{
	public String getName();
	public String getID();
	public boolean isFlag();
	public boolean isUserEditable();
	public boolean isTransient();
	public boolean isModified();
	public boolean toBoolean( EnumValue value );
	public EnumValue valueOf( boolean flag );
	public boolean contains( EnumValue val );
	public EnumValue forString( String str );
	public Iterator iterator();
	public EnumValue[] valuesAsArray();
	public int size();
	public EnumValue getDefault();
	public int indexOf( EnumValue value );
	public EnumValue forIndex( int index );
}
