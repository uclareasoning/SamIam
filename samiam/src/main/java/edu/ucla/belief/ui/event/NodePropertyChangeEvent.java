package edu.ucla.belief.ui.event;

import edu.ucla.util.EnumProperty;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariable;

/**
	@author Keith Cascio
	@since 100703
*/
public class NodePropertyChangeEvent
{
	private NodePropertyChangeEvent() {}

	public NodePropertyChangeEvent( DisplayableFiniteVariable var )
	{
		this.variable = var;
	}

	public NodePropertyChangeEvent( DisplayableFiniteVariable var, EnumProperty property )
	{
		this.variable = var;
		this.property = property;
		this.flagEnumPropertyChange = true;
	}

	public boolean isEnumPropertyChange()
	{
		return flagEnumPropertyChange;
	}

	public String toString()
	{
		String strVar = (variable == null) ? "null" : variable.toString();
		return super.toString().substring( (int)25 ) + "{ " + strVar + " }";
	}

	public DisplayableFiniteVariable variable;
	public EnumProperty property;
	private boolean flagEnumPropertyChange = false;

	public static final NodePropertyChangeEvent EMPTY = new NodePropertyChangeEvent();
}
