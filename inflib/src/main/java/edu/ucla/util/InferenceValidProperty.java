package edu.ucla.util;

//{superfluous} import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.FlagProperty;

import edu.ucla.belief.BeliefNetwork;

import java.util.Collection;

/** @author keith cascio
	@since  20031010 */
public class InferenceValidProperty extends FlagProperty
{
	public static final InferenceValidProperty PROPERTY = new InferenceValidProperty();

	private InferenceValidProperty() {}

	public String getName()
	{
		return "cpt valid";
	}

	public EnumValue getDefault()
	{
		return TRUE;
	}

	public String getID()
	{
		return "iscptvalid";
	}

	public static final String createErrorMessage( BeliefNetwork bn )
	{
		Collection invalid = bn.findVariables( PROPERTY, PROPERTY.FALSE );
		return "CPTs invalid for variable(s):\n" + invalid.toString() + "\nValidate CPTs first using the variable properties dialog\nor the variable selection tool.";
	}
}
