package edu.ucla.belief;

/**
	At inception, this interface abstracts the process of copying (cloning)
	variable objects, which is likely to be a BeliefNetwork-subclass-specific
	process.  In the future, it may serve to abstract other copying functions.

	@author Keith Cascio
	@since 021804
*/
public interface Copier
{
	public Variable copyVariable( Variable var );
	public FiniteVariable copyFiniteVariable( FiniteVariable var );
	public FiniteVariable copyFiniteVariable( FiniteVariable var, BeliefNetwork from, BeliefNetwork to );
}
