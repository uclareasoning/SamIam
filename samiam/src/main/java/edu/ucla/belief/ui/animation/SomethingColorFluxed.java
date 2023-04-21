package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.networkdisplay.*;
import edu.ucla.belief.ui.util.Util;

import java.util.*;
import java.util.List;
import java.awt.*;

/**
	@author Keith Cascio
	@since 080504
*/
public class SomethingColorFluxed implements SomethingAnimated
{
	public static final float FLOAT_DELTA_TRIVIAL = (float)0.000001;
	public static final java.io.PrintStream STREAM_VERBOSE = System.out;

	public SomethingColorFluxed( ColorVirtual object, float intensity, Animator anim )
	{
		myColorVirtual = object;
		myIntensity = intensity;
		myAnimator = anim;
	}

	public Animator getAnimator(){
		return myAnimator;
	}

	public void finish()
	{
		if( myFlagTrivial ) return;
		step( myCurrentIntensity = myTargetIntensity );
	}

	public void init( int steps )
	{
		if( myFlagVerbose ) STREAM_VERBOSE.println( "(SomethingColorFluxed)"+myColorVirtual+".init()" );

		myFlagFinished = false;

		float myDivisor = (float)steps;

		myAnimationColorHandler = getAnimator().getAnimationColorHandler();

		if( actual != null ) actual = Color.black;
		if( virtual != null ) virtual = Color.black;
		myCurrentIntensity = myTargetIntensity = myStepIntensity = (float)0;
		myFlagTrivial = false;

		actual = myColorVirtual.getActualColor();
		virtual = myColorVirtual.getVirtualColor();

		float[] actualVals = Color.RGBtoHSB( actual.getRed(), actual.getGreen(), actual.getBlue(), myUtilHSBVals );
		myHSBvals = Color.RGBtoHSB( virtual.getRed(), virtual.getGreen(), virtual.getBlue(), new float[3] );

		myCurrentIntensity = myAnimationColorHandler.getCurrentIntensity( actualVals );

		myTargetIntensity = myIntensity;
		float deltaIntensity = myTargetIntensity - myCurrentIntensity;

		if( myFlagVerbose ) STREAM_VERBOSE.println( "\t deltaIntensity "+deltaIntensity );

		if( (Math.abs(deltaIntensity) < FLOAT_DELTA_TRIVIAL) ){
			myFlagTrivial = true;
			return;
		}

		myStepIntensity = deltaIntensity/myDivisor;

		if( myFlagVerbose ) STREAM_VERBOSE.println( "\t myStepIntensity "+myStepIntensity );
	}

	public void step()
	{
		if( myFlagTrivial ) return;
		step( myCurrentIntensity += myStepIntensity );
	}

	public void step( float intensity )
	{
		//if( finished() ) throw new IllegalStateException();
		if( finished() ) return;

		myAnimationColorHandler.updateValues( myHSBvals, intensity, myUtilHSBVals );
		actual = Color.getHSBColor( myUtilHSBVals[0], myUtilHSBVals[1], myUtilHSBVals[2] );
		myColorVirtual.setActualColor( actual );
		//myColorVirtual.paintImmediately();

		//if( myFlagVerbose ) STREAM_VERBOSE.println( "actual = " + actual );

		myFlagFinished = Util.epsilonEquals( intensity, myTargetIntensity, FLOAT_DELTA_TRIVIAL );
	}

	/** @since 080504 */
	public boolean finished(){
		return myFlagFinished;
	}

	public void setVerbose( boolean flag ){
		myFlagVerbose = flag;
	}

	protected Animator myAnimator;
	protected boolean myFlagVerbose = false;
	protected boolean myFlagFinished = false;

	protected ColorVirtual myColorVirtual;
	protected float myIntensity;

	protected Color actual;
	protected Color virtual;
	protected float[] myHSBvals;
	protected float myCurrentIntensity;
	protected float myTargetIntensity;
	protected float myStepIntensity;
	protected boolean myFlagTrivial = false;
	protected float[] myUtilHSBVals = new float[3];
	protected AnimationPreferenceHandler.AnimationColorHandler myAnimationColorHandler;
}
