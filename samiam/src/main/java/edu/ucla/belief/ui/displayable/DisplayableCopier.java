package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.io.*;
import edu.ucla.belief.*;

import edu.ucla.belief.ui.NetworkInternalFrame;

/**
	Copier for DisplayableBeliefNetworks.  Makes DisplayableFiniteVariableImpl copies,
	wrapped around the correct sub variable type, as copied by the sub copier.
	@author Keith Cascio
	@since 021804
*/
public class DisplayableCopier extends AbstractCopier
{
	public DisplayableCopier( Copier sub, NetworkInternalFrame nif )
	{
		this.mySubCopier = sub;
		this.myNetworkInternalFrame = nif;
	}

	public FiniteVariable copyFiniteVariable( FiniteVariable var )
	{
		FiniteVariable copiedFiniteVariable = mySubCopier.copyFiniteVariable( var );
		return wrap( copiedFiniteVariable );
	}

	public FiniteVariable copyFiniteVariable( FiniteVariable var, BeliefNetwork from, BeliefNetwork to )
	{
		FiniteVariable copiedFiniteVariable = mySubCopier.copyFiniteVariable( var, from, to );
		return wrap( copiedFiniteVariable );
	}

	private DisplayableFiniteVariableImpl wrap( FiniteVariable copiedFiniteVariable )
	{
		return new DisplayableFiniteVariableImpl( copiedFiniteVariable, myNetworkInternalFrame );
	}

	private Copier mySubCopier;
	private NetworkInternalFrame myNetworkInternalFrame;
}
