package edu.ucla.belief.io;

import edu.ucla.belief.*;

/**
	Abstract implementation of Copier with a default implementation of copyVariable().
	@author Keith Cascio
	@since 021804
*/
public abstract class AbstractCopier implements Copier
{
	public Variable copyVariable( Variable var )
	{
		if( var instanceof FiniteVariable ) return copyFiniteVariable( (FiniteVariable)var );
		else return null;
	}

	public FiniteVariable copyFiniteVariable( FiniteVariable var, BeliefNetwork from, BeliefNetwork to ){
		return copyFiniteVariable( var );
	}

	abstract public FiniteVariable copyFiniteVariable( FiniteVariable var );

	/**
		An instantiable AbstractCopier that makes FiniteVariableImpl copies.
	*/
	public static AbstractCopier STANDARD = new AbstractCopier()
	{
		public FiniteVariable copyFiniteVariable( FiniteVariable var ){
			return new FiniteVariableImpl( var );
		}
	};
}
