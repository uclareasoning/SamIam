package edu.ucla.belief;

/**
	@author Keith Cascio
	@since 052203
*/
public class StateNotFoundException extends Exception
{
	public StateNotFoundException( FiniteVariable var, Object value )
	{
		super( var.getID() + " does not contain state " + ((value==null)? "null" : value.toString()) );
		this.var = var;
		this.value = value;
	}

	public FiniteVariable var;
	public Object value;
}
