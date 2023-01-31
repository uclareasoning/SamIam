package edu.ucla.belief;

import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;

import java.util.Map;

/**
	Define variables.

	@author Keith Cascio
	@since 093002
*/
public interface Variable extends Cloneable, Comparable
{
    public String getID();
    public void setID( String id );
    public Object instance( String instanceString );
    public Object clone();
    public Object getUserObject();
    public void setUserObject( Object obj );
    public EnumValue getProperty( EnumProperty property );
    public void setProperty( EnumProperty property, EnumValue value );
    public void delete( EnumProperty property );
    public Map getEnumProperties();
}
