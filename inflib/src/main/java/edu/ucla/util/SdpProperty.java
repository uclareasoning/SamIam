package edu.ucla.util;

//{superfluous} import edu.ucla.util.EnumProperty;
//import edu.ucla.util.EnumValue;
//import edu.ucla.util.FlagProperty;

/**
	@author Suming Chen
    @decision var
*/
public class SdpProperty extends FlagProperty
{
	public static final SdpProperty PROPERTY = new SdpProperty();

	private SdpProperty() {}

	public String getName()
	{
		return "decision_var";
	}

	public EnumValue getDefault()
	{
		return FALSE;
	}

	public String getID()
	{
		return "isdecisionvariable";
	}
}
