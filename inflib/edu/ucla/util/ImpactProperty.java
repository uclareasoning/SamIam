package edu.ucla.util;

//{superfluous} import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.FlagProperty;

/**
	@author Keith Cascio
	@since 091603
*/
public class ImpactProperty extends FlagProperty
{
	public static final ImpactProperty PROPERTY = new ImpactProperty();

	private ImpactProperty() {}

	public String getName()
	{
		return "impact";
	}

	public EnumValue getDefault()
	{
		return TRUE;
	}

	public String getID()
	{
		return "isimpactvariable";
	}
}
