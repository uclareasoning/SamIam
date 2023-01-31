package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.networkdisplay.*;
import edu.ucla.belief.ui.util.Util;

import java.util.*;
import java.util.List;
import java.awt.*;

/**
	@author Keith Cascio
	@since 072804
*/
public class SomethingScaled implements SomethingAnimated
{
	public static final double DOUBLE_DELTA_TRIVIAL = (double)1.09;
	public static final double DOUBLE_EPSILON = (double)0.09;

	public SomethingScaled( CoordinateVirtual object, double factor, CoordinateTransformer xformer, Animator anim )
	{
		myCoordinateVirtual = object;
		myFactor = factor;
		myCoordinateTransformer = xformer;
		myAnimator = anim;
	}

	/** @since 080404 */
	public Animator getAnimator(){
		return myAnimator;
	}

	public void init( int steps )
	{
		//System.out.println( "(SomethingScaled)"+myCoordinateVirtual+".init()" );

		myFlagFinished = false;

		double myDivisor = (double)steps;

		if( actual != null ) actual.setSize( 0,0 );
		if( virtual != null ) virtual.setSize( 0,0 );
		myVirtualWidth = width = height = targetWidth = targetHeight = stepWidth = stepHeight = (double)0;
		scale = stepScale = (float)0;
		myFlagTrivial = false;

		scale = myCoordinateTransformer.virtualToActual( (float)1 );
		actual = myCoordinateVirtual.getActualSize( actual );
		virtual = myCoordinateVirtual.getVirtualSize( virtual );
		width = actual.getWidth();
		height = actual.getHeight();
		targetWidth = myCoordinateTransformer.virtualToActual( virtual.getWidth()*myFactor );
		targetHeight = myCoordinateTransformer.virtualToActual( virtual.getHeight()*myFactor );
		myVirtualWidth = virtual.getWidth();
		//targetWidth = width*myFactor;
		//targetHeight = height*myFactor;
		double deltaWidth = targetWidth - width;
		double deltaHeight = targetHeight - height;

		//System.out.println( "\t deltaWidth " + deltaWidth + " deltaHeight " + deltaHeight );

		if( (Math.abs(deltaWidth) < DOUBLE_DELTA_TRIVIAL) && (Math.abs(deltaHeight) < DOUBLE_DELTA_TRIVIAL) ){
			//System.out.println( "\t trivial" );
			myFlagTrivial = true;
			return;
		}

		stepWidth = deltaWidth/myDivisor;
		stepHeight = deltaHeight/myDivisor;
		stepScale = (float)(stepWidth/myVirtualWidth);

		if( stepWidth < 0 ){
			myCoordinateVirtual.hintScale( myCoordinateTransformer.virtualToActual( (float)myFactor ) );
		}
	}

	/** @since 080804 */
	public double getMaxStep(){
		return Math.max( stepWidth, stepHeight );
	}

	/** @since 080804 */
	public static void lockStep( SomethingAnimated[] array )
	{
		//System.out.print( "SomethingScaled.lockStep( |"+array.length+"| )" );

		double maxStepWidth = IntensifyEntropyThread.DOUBLE_ZERO;
		double maxStepHeight = IntensifyEntropyThread.DOUBLE_ZERO;

		SomethingScaled current;
		double stepWidthAbs, stepHeightAbs;
		for( int i = 0; i<array.length; i++ ){
			current = (SomethingScaled)array[i];
			stepWidthAbs = Math.abs( current.stepWidth );
			stepHeightAbs = Math.abs( current.stepHeight );
			if( stepWidthAbs > maxStepWidth ) maxStepWidth = stepWidthAbs;
			if( stepHeightAbs > maxStepHeight ) maxStepHeight = stepHeightAbs;
		}

		//System.out.println( " max = {" + maxStepWidth + ", " + maxStepHeight + " }" );

		double maxStepWidthNeg = -maxStepWidth;
		double maxStepHeightNeg = -maxStepHeight;

		for( int i = 0; i<array.length; i++ ){
			current = (SomethingScaled)array[i];
			current.stepWidth = ( current.stepWidth >= IntensifyEntropyThread.DOUBLE_ZERO ) ? maxStepWidth : maxStepWidthNeg;
			current.stepHeight = ( current.stepHeight >= IntensifyEntropyThread.DOUBLE_ZERO ) ? maxStepHeight : maxStepHeightNeg;
			current.stepScale = (float)(current.stepWidth/current.myVirtualWidth);
		}
	}

	public void step()
	{
		//if( myFlagVerbose ) System.out.println( "actual.setSize( "+width+"+"+stepWidth+", "+height+"+"+stepHeight+" );" );

		if( myFlagFinished || myFlagTrivial ) return;
		step( width += stepWidth, height += stepHeight, scale += stepScale );
	}

	/** @since 080504 */
	public void step( double destwidth, double destheight, float destScale )
	{
		//if( finished() ) throw new IllegalStateException();
		if( finished() ) return;

		destwidth = adjust( destwidth, targetWidth, stepWidth );
		destheight = adjust( destheight, targetHeight, stepHeight );

		actual.setSize( destwidth, destheight );
		myCoordinateVirtual.setActualSize( actual );
		//myCoordinateVirtual.setActualSizeAndHintScale( actual, destScale );

		//myFlagFinished = ( Util.epsilonEquals( destwidth, targetWidth, DOUBLE_EPSILON ) && Util.epsilonEquals( destheight, targetHeight, DOUBLE_EPSILON ) );
		myFlagFinished = calculateFinished( destwidth, destheight );
	}

	/** @since 080804 */
	public double adjust( double dest, double target, double step )
	{
		//if( ((step > 0)&&(dest > target)) || (dest < target) ) return target;
		//else return dest;
		double ret = dest;
		if( step >= 0 ) { if( dest > target ) ret = target; }
		else if( dest < target ) ret = target;
		return ret;
	}

	/** @since 080804 */
	public boolean calculateFinished( double destwidth, double destheight )
	{
		boolean ret = true;

		if( stepWidth > 0 ) ret &= ( destwidth >= targetWidth );
		else ret &= ( destwidth <= targetWidth );

		if( stepHeight > 0 ) ret &= ( destheight >= targetHeight );
		else ret &= ( destheight <= targetHeight );

		return ret;
	}

	/** @since 080504 */
	public void finish()
	{
		myCoordinateVirtual.hintScale( myCoordinateTransformer.virtualToActual( Math.min( (float)myFactor, IntensifyEntropyThread.FLOAT_ONE ) ) );

		if( finished() || myFlagTrivial ) return;
		step( width = targetWidth, height = targetHeight, scale = (float)myFactor );
	}

	/** @since 080504 */
	public boolean finished(){
		return myFlagFinished;
	}

	public void setVerbose( boolean flag ){
		myFlagVerbose = flag;
	}

	protected Animator myAnimator;
	protected CoordinateTransformer myCoordinateTransformer;
	protected boolean myFlagVerbose = false;
	protected boolean myFlagFinished = false;

	protected CoordinateVirtual myCoordinateVirtual;
	protected double myFactor;

	protected Dimension actual;
	protected Dimension virtual;
	protected float scale;
	protected float stepScale;
	protected double myVirtualWidth;
	protected double width;
	protected double height;
	protected double targetWidth;
	protected double targetHeight;
	protected double stepWidth;
	protected double stepHeight;
	protected boolean myFlagTrivial = false;
}