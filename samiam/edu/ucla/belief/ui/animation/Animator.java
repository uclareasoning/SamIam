package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.networkdisplay.*;

import edu.ucla.belief.*;

import java.util.*;
import java.util.List;
import java.awt.*;

/**
	@author Keith Cascio
	@since 072304
*/
public class Animator
{
	public static boolean FLAG_ENABLE_ANIMATION = false;

	public Animator( NetworkDisplay parent )
	{
		myParent = parent;
		myAnimationPreferenceHandler = AnimationPreferenceHandler.getInstance();
	}

	//private ScaleMarginalsThread myScaleMarginalsThread;
	private IntensifyEntropyThread myIntensifyEntropyThread;
	private ScaleImpactThread myScaleImpactThread;

	/** @since 080804 */
	public void killState()
	{
		//myScaleMarginalsThread = null;
		myIntensifyEntropyThread = null;
		myScaleImpactThread = null;
	}

	/** @since 080804 */
	public void animate( final DisplayableBeliefNetwork bn, final InferenceEngine ie )
	{
		//System.out.println( "Animator.animate()" );

		if( !myAnimationPreferenceHandler.getScale() && !myAnimationPreferenceHandler.getIntensify() ) return;

		Runnable runnable = new Runnable(){
			public void run()
			{
				if( myAnimationPreferenceHandler.getResetFirst() )
				{
					myParent.recalculateActuals( true, false );
					try{
						Thread.sleep( myAnimationPreferenceHandler.getInterveningPause() );
					}catch( InterruptedException e ){
						System.err.println( "Warning: animation thread interrupted" );
						Thread.currentThread().interrupt();
						return;
					}
				}

				//if( myScaleMarginalsThread == null ) myScaleMarginalsThread = myAnimator.scaleMarginals( bn, ie );
				//else{
				//	(myScaleMarginalsThread = myScaleMarginalsThread.clone( ie )).start();
				//}

				if( myAnimationPreferenceHandler.getScale() ){
					if( myScaleImpactThread == null ) myScaleImpactThread = scaleImpact( bn, ie );
					//else (myScaleImpactThread = myScaleImpactThread.clone( ie )).start();
					else myScaleImpactThread.start();
					//System.out.println( "\t" + myScaleImpactThread.getName() + ".start()" );
				}
				else myScaleImpactThread = null;

				if( myAnimationPreferenceHandler.getIntensify() ){
					if( myIntensifyEntropyThread == null ) myIntensifyEntropyThread = intensifyEntropy( bn, ie );
					//else (myIntensifyEntropyThread = myIntensifyEntropyThread.clone( ie )).start();
					else myIntensifyEntropyThread.start();
					//System.out.println( "\t" + myIntensifyEntropyThread.getName() + ".start()" );
				}
				else myIntensifyEntropyThread = null;
			}
		};
		new Thread( runnable ).start();
	}

	/** @since 080904 */
	public void instantReplay()
	{
		if( myAnimationPreferenceHandler.getScale() && (myScaleImpactThread != null ) ){
			//myScaleImpactThread = myScaleImpactThread.clone( (InferenceEngine)null );
			myScaleImpactThread.setReplay( true );
			myScaleImpactThread.start();
		}

		if( myAnimationPreferenceHandler.getIntensify() && (myIntensifyEntropyThread != null ) ){
			//myIntensifyEntropyThread = myIntensifyEntropyThread.clone( (InferenceEngine)null );
			myIntensifyEntropyThread.setReplay( true );
			myIntensifyEntropyThread.start();
		}
	}

	/** @since 080404 */
	public long getDelay(){
		return myAnimationPreferenceHandler.getDelay();
	}

	/** @since 080404 */
	public int getSteps(){
		return myAnimationPreferenceHandler.getSteps();
	}

	/** @since 080504 */
	public AnimationPreferenceHandler.AnimationColorHandler getAnimationColorHandler(){
		return myAnimationPreferenceHandler.getAnimationColorHandler();
	}

	/** @since 080704 */
	public double getScaleFactor(){
		return myAnimationPreferenceHandler.getScaleFactor();
	}

	/** @since 080704 */
	public double getOffset(){
		return myAnimationPreferenceHandler.getOffset();
	}

	/** @since 080804 */
	public boolean getLockStep(){
		return myAnimationPreferenceHandler.getLockStep();
	}

	/** @since 081004 */
	public boolean getReflect(){
		return myAnimationPreferenceHandler.getReflect();
	}

	/** @since 081004 */
	public boolean getExponentiate(){
		return myAnimationPreferenceHandler.getExponentiate();
	}

	/** @since 072904 */
	/*public ScaleMarginalsThread scaleMarginals( DisplayableBeliefNetwork bn, InferenceEngine ie )
	{
		//ArrayList aos = new ArrayList( bn.size() );
		//DisplayableFiniteVariable dVar = null;
		//for( DFVIterator it = bn.dfvIterator(); it.hasNext(); ){
		//	dVar = it.nextDFV();
		//	aos.add( new SomethingScaled( dVar.getNodeLabel(), calculateScaleMarginal( dVar, ie ), myParent ) );
		//}
		//animate( aos, INT_STEPS_DEFAULT );

		ScaleMarginalsThread ret = new ScaleMarginalsThread( getSteps(), ie, bn, myParent, (Animator)this );
		animate( ret );
		return ret;
	}*/

	/** @since 072904 */
	public IntensifyEntropyThread intensifyEntropy( DisplayableBeliefNetwork bn, InferenceEngine ie )
	{
		IntensifyEntropyThread ret = new IntensifyEntropyThread( getSteps(), ie, bn, (Animator)this );
		animate( ret );
		return ret;
	}

	/** @since 072904 */
	public ScaleImpactThread scaleImpact( DisplayableBeliefNetwork bn, InferenceEngine ie )
	{
		ScaleImpactThread ret = new ScaleImpactThread( getSteps(), ie, bn, myParent, (Animator)this );
		animate( ret );
		return ret;
	}

	/** @since 080504 */
	public double calculateIntensityEntropy( FiniteVariable fVar, InferenceEngine ie )
	{
		Table conditional = ie.conditional( fVar );
		double entropy = conditional.entropy();
		//System.out.println( "entropy( "+fVar.getID()+" ) = " + entropy );
		return myRand.nextFloat();
	}

	/** @since 072904 */
	public double calculateScaleMarginal( FiniteVariable fVar, InferenceEngine ie )
	{
		double[] conditional = ie.conditional( fVar ).dataclone();

		double sum1 = (double)0;
		double sum2 = (double)0;

		if( conditional.length == 2 )
		{
			sum1 = conditional[0];
			sum2 = conditional[1];
			if( sum1 > sum2 ){
				sum1 = conditional[1];
				sum2 = conditional[0];
			}
		}
		else
		{
			Arrays.sort( conditional );
			int countHalf = (int) Math.floor( (((double)conditional.length)/((double)2)) );
			for( int i=0; i<countHalf; i++ ) sum1 += conditional[i];
			for( int i=conditional.length-1; i>countHalf; i-- ) sum2 += conditional[i];
		}

		double difference_scaled = (sum2 - sum1) * myAnimationPreferenceHandler.getScaleFactor();

		return difference_scaled + myAnimationPreferenceHandler.getOffset();
	}

	public void scaleRandom( DisplayableBeliefNetwork bn )
	{
		ArrayList aos = new ArrayList( bn.size() );

		Random rand = new Random();
		for( DFVIterator it = bn.dfvIterator(); it.hasNext(); ){
			aos.add( new SomethingScaled( it.nextDFV().getNodeLabel(), rand.nextDouble()*myAnimationPreferenceHandler.getScaleFactor(), myParent, (Animator)this ) );
		}

		animate( aos, getSteps() );
	}

	/** @since 072904 */
	public void animate( AnimationThread at ){
		at.setNetworkDisplay( myParent );
		at.start();
	}

	public void animate( Collection animatedObjects, int steps )
	{
		AnimationThread at = new AnimationThread( steps, (Animator)this );
		at.addAll( animatedObjects );
		at.setNetworkDisplay( myParent );
		at.start();
	}

	/*public void scaleSize( CoordinateVirtual object, double factor, int steps ){
		AnimationThread at = new AnimationThread( steps, (Animator)this );
		SomethingScaled so = new SomethingScaled( object, factor, myParent, (Animator)this );
		at.add( so );
		at.setNetworkDisplay( myParent );
		at.start();
	}*/

	public static void init( SomethingAnimated[] array, int steps ){
		for( int i=0; i<array.length; i++ ) array[i].init( steps );
	}

	public static void step( SomethingAnimated[] array ){
		//System.out.println( "Animator.step()" );
		for( int i=0; i<array.length; i++ ) array[i].step();
	}

	/** @since 080504 */
	public static void finish( SomethingAnimated[] array ){
		for( int i=0; i<array.length; i++ ) array[i].finish();
	}

	private NetworkDisplay myParent;
	private AnimationPreferenceHandler myAnimationPreferenceHandler;
	private Random myRand = new Random();
}
