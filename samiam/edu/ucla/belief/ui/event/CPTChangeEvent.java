package edu.ucla.belief.ui.event;

import edu.ucla.belief.FiniteVariable;

/**
	@author Keith Cascio
	@since 072902
*/
public class CPTChangeEvent
{
	public CPTChangeEvent( FiniteVariable var )
	{
		this.var = var;
	}

	public FiniteVariable var = null;
}
