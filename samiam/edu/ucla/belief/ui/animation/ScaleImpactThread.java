package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.networkdisplay.*;

import edu.ucla.belief.*;

import java.util.ArrayList;

/**
	@author Keith Cascio
	@since 080704
*/
public class ScaleImpactThread extends AnimationThread
{
	public static final double DOUBLE_SUPPRESS_NON_INFINITE = (double)0.8;

	public ScaleImpactThread( int steps, InferenceEngine ie, Animator anim )
	{
		super( steps, anim, "ScaleImpactThread" );
		myInferenceEngine = ie;
	}

	public ScaleImpactThread( int steps, InferenceEngine ie, DisplayableBeliefNetwork bn, CoordinateTransformer xformer, Animator anim )
	{
		this( steps, ie, anim );

		ArrayList aos = new ArrayList( bn.size() );

		DisplayableFiniteVariable dVar = null;
		for( DFVIterator it = bn.dfvIterator(); it.hasNext(); ){
			dVar = it.nextDFV();
			aos.add( new ScaledDFV( dVar, xformer, anim ) );
		}

		//((ScaledDFV)aos.get(0)).setVerbose( true );

		addAll( aos );

		validateArray();

		myLastConditionals = new Table[ myArray.length ];
		myNewConditionals = new Table[ myArray.length ];

		getConditionals( myArray, myLastConditionals );
	}

	public ScaleImpactThread( ScaleImpactThread toCopy, InferenceEngine ie )
	{
		super( toCopy );

		this.myLastConditionals = toCopy.myLastConditionals;
		this.myNewConditionals = toCopy.myNewConditionals;

		myInferenceEngine = ie;
		mySteps = getAnimator().getSteps();
		myCloneGeneration = ++toCopy.myCloneGeneration;
		//setName( ScaleMarginalsThread.makeClonedName( toCopy.getName(), myCloneGeneration ) );
	}

	public ScaleImpactThread clone( InferenceEngine ie )
	{
		return new ScaleImpactThread( this, ie );
	}

	/** @since 080704 */
	protected void getConditionals( SomethingAnimated[] array, Table[] toFill )
	{
		if( array.length != toFill.length ) throw new IllegalArgumentException();

		for( int i=0; i<array.length; i++ ){
			toFill[i] = myInferenceEngine.conditional( ((ScaledDFV)array[i]).getVariable() );
		}
	}

	/** overridden */
	protected void init( SomethingAnimated[] array )
	{
		//System.out.println( "("+ getName() + ").init()" );
		try{
			if( getReplay() ){
				super.init( array );
				if( getAnimator().getLockStep() ) SomethingScaled.lockStep( array );
				return;
			}

			getConditionals( array, myNewConditionals );

			double[] impacts = new double[ array.length ];

			double max = IntensifyEntropyThread.DOUBLE_ZERO;//Double.MIN_VALUE;
			FiniteVariable fVar = null;
			double impact;
			boolean flagNoImpact = true;
			boolean flagExistsInfiniteImpact = false;
			for( int i=0; i<array.length; i++ ){
				fVar = ((ScaledDFV)array[i]).getVariable();
				impact = impacts[i] = myNewConditionals[i].distanceMeasure( myLastConditionals[i] );
				flagNoImpact &= (impact == IntensifyEntropyThread.DOUBLE_ZERO);
				flagExistsInfiniteImpact |= (impact == Double.POSITIVE_INFINITY);
				//System.out.println( "\t impact( "+fVar.getID()+" ) = " + impact );
				if( (!Double.isInfinite(impact)) && (!Double.isNaN(impact)) && (impact>max) ) max = impact;
			}

			if( flagNoImpact ){
				//System.out.println( "\t no impact" );
				for( int i=0; i<array.length; i++ ) ((ScaledDFV)array[i]).init( mySteps, IntensifyEntropyThread.FLOAT_ONE );
			}
			else
			{
				//System.out.println( "\t max entropy = " + max );
				double inverse = (max==IntensifyEntropyThread.DOUBLE_ZERO) ? IntensifyEntropyThread.DOUBLE_ONE : (IntensifyEntropyThread.DOUBLE_ONE/max);
				//System.out.println( "\t inverse = " + inverse );

				double scaleFactor = getAnimator().getScaleFactor();
				double offset = getAnimator().getOffset();
				double scaleMin = offset;
				double scaleMax = scaleFactor+offset;
				double scaleNormal = inverse*scaleFactor;
				if( flagExistsInfiniteImpact ) scaleNormal *= DOUBLE_SUPPRESS_NON_INFINITE;

				//System.out.println( "\t scaleNormal = " + scaleNormal );

				for( int i=0; i<impacts.length; i++ ){
					impact = impacts[i];
					if( impact == Double.POSITIVE_INFINITY ) impact = scaleMax;
					else if( (impact == IntensifyEntropyThread.DOUBLE_ZERO) || (impact == Double.NEGATIVE_INFINITY) || Double.isNaN(impact) ) impact = scaleMin;
					else impact = Math.abs(impact*scaleNormal) + offset;
					impacts[i] = impact;
					//System.out.println( "\t impact( "+((ScaledDFV)array[i]).getVariable().getID()+" ) = " + impact );
					//impacts[i] = IntensifyEntropyThread.DOUBLE_ONE - Math.abs( impacts[i]*inverse );
				}

				for( int i=0; i<array.length; i++ ) ((ScaledDFV)array[i]).init( mySteps, impacts[i] );

				if( getAnimator().getLockStep() ) SomethingScaled.lockStep( array );
			}

			Table[] swap = myLastConditionals;
			myLastConditionals = myNewConditionals;
			myNewConditionals = swap;
		}catch( Exception e ){
			die();
			e.printStackTrace();
		}
	}

	/** overridden */
	//protected void animate(){
	//	Animator.finish( myArray );
	//}

	protected InferenceEngine myInferenceEngine;
	protected int myCloneGeneration;
	protected Table[] myLastConditionals;
	protected Table[] myNewConditionals;
}
