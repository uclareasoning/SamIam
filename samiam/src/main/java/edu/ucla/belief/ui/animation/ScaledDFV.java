package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.networkdisplay.*;

import edu.ucla.belief.*;

/**
	@author Keith Cascio
	@since 072904
*/
public class ScaledDFV extends SomethingScaled implements SomethingAnimated
{
	public ScaledDFV( DisplayableFiniteVariable dVar, CoordinateTransformer xformer, Animator anim )
	{
		super( dVar.getNodeLabel(), (double)1, xformer, anim );
		myDisplayableFiniteVariable = dVar;
	}

	public void init( int steps, InferenceEngine ie )
	{
		myCoordinateVirtual = myDisplayableFiniteVariable.getNodeLabel();
		myFactor = getAnimator().calculateScaleMarginal( myDisplayableFiniteVariable, ie );
		super.init( steps );
	}

	public void init( int steps, double factor )
	{
		myCoordinateVirtual = myDisplayableFiniteVariable.getNodeLabel();
		myFactor = factor;
		super.init( steps );
	}

	public DisplayableFiniteVariable getVariable(){
		return myDisplayableFiniteVariable;
	}

	protected DisplayableFiniteVariable myDisplayableFiniteVariable;
}
