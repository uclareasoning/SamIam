package edu.ucla.util;

import edu.ucla.belief.Variable;

/** @author keith cascio
	@since  20040817 */
public class HiddenProperty extends FlagProperty
{
	public static final HiddenProperty PROPERTY = new HiddenProperty();

	private HiddenProperty() {}

	public String getName()
	{
		return "hidden";
	}

	public EnumValue getDefault()
	{
		return FALSE;
	}

	public String getID()
	{
		return "ishiddenvariable";
	}

	public static boolean isHidden( Variable var )
	{
		if( var == null ) return false;
		else return var.getProperty( PROPERTY ) == PROPERTY.TRUE;
	}
}
