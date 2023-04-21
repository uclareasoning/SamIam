package edu.ucla.belief;

/**
	@author Keith Cascio
	@since 011503
*/
public interface PartialDerivativeEngine
{
	/**
	* Returns the partial derivatives of the probability function with respect
	* to the variable var.
	*/
	public Table partial(FiniteVariable var);

	/**
	* Returns the partial derivatives of the probability function with respect
	* to the family table of var.
	*/
	public Table familyPartial(FiniteVariable var);
}
