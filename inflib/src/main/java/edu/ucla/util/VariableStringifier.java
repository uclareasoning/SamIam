package edu.ucla.util;

import edu.ucla.belief.Variable;

/**
	@author Keith Cascio
	@since 052004
*/
public interface VariableStringifier extends Stringifier
{
	public String variableToString( Variable var );
}
