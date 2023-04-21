package edu.ucla.belief.inference.map;

//{superfluous} import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.FlagProperty;

/**
	@author Keith Cascio
	@since 082003
*/
public class MapProperty extends FlagProperty
{
	public static final MapProperty PROPERTY = new MapProperty();

	private MapProperty() {}

	public String getName()
	{
		return "map";
	}

	public EnumValue getDefault()
	{
		return FALSE;
	}

	public String getID()
	{
		return "ismapvariable";
	}
}
