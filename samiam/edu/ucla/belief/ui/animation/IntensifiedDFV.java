package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.networkdisplay.*;

import edu.ucla.belief.*;

/**
	@author Keith Cascio
	@since 080504
*/
public class IntensifiedDFV extends SomethingColorFluxed implements SomethingAnimated
{
	public IntensifiedDFV( DisplayableFiniteVariable dVar, Animator anim )
	{
		super( dVar.getNodeLabel(), (float)1, anim );
		myDisplayableFiniteVariable = dVar;
	}

	public void init( int steps, float intensity )
	{
		//System.out.println( "(IntensifiedDFV)"+myDisplayableFiniteVariable.getID()+".init( "+intensity+" )" );
		myColorVirtual = myDisplayableFiniteVariable.getNodeLabel();
		myIntensity = intensity;//(float) getAnimator().calculateIntensityEntropy( myDisplayableFiniteVariable, ie );
		super.init( steps );
	}

	public DisplayableFiniteVariable getVariable(){
		return myDisplayableFiniteVariable;
	}

	protected DisplayableFiniteVariable myDisplayableFiniteVariable;
}
