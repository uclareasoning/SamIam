package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.networkdisplay.*;

import edu.ucla.belief.*;

import java.util.ArrayList;

/**
	@author Keith Cascio
	@since 080504
*/
public class IntensifyEntropyThread extends AnimationThread
{
	public static final double DOUBLE_ZERO = (double)0;
	public static final double DOUBLE_ONE = (double)1;
	public static final double DOUBLE_TWO = (double)2;
	public static final float FLOAT_ONE = (float)1;

	public IntensifyEntropyThread( int steps, InferenceEngine ie, Animator anim )
	{
		super( steps, anim, "IntensifyEntropyThread" );
		myInferenceEngine = ie;
	}

	public IntensifyEntropyThread( int steps, InferenceEngine ie, DisplayableBeliefNetwork bn, Animator anim )
	{
		this( steps, ie, anim );

		ArrayList aos = new ArrayList( bn.size() );

		DisplayableFiniteVariable dVar = null;
		for( DFVIterator it = bn.dfvIterator(); it.hasNext(); ){
			dVar = it.nextDFV();
			aos.add( new IntensifiedDFV( dVar, anim ) );
		}

		//((IntensifiedDFV)aos.get(0)).setVerbose( true );

		addAll( aos );
	}

	public IntensifyEntropyThread( IntensifyEntropyThread toCopy, InferenceEngine ie )
	{
		super( toCopy );
		myInferenceEngine = ie;
		mySteps = getAnimator().getSteps();
		myCloneGeneration = ++toCopy.myCloneGeneration;
		//setName( ScaleMarginalsThread.makeClonedName( toCopy.getName(), myCloneGeneration ) );
	}

	public IntensifyEntropyThread clone( InferenceEngine ie )
	{
		return new IntensifyEntropyThread( this, ie );
	}

	/** overridden */
	protected void init( SomethingAnimated[] array )
	{
		//System.out.println( "("+ getName() + ").init()" );
		try{
			if( getReplay() ){
				super.init( array );
				return;
			}

			double[] entropies = new double[ array.length ];

			double max = DOUBLE_ZERO;
			FiniteVariable fVar = null;
			for( int i=0; i<array.length; i++ ){
				fVar = ((IntensifiedDFV)array[i]).getVariable();
				entropies[i] = myInferenceEngine.conditional( fVar ).entropy();
				//System.out.println( "\t entropy( "+fVar.getID()+" ) = " + entropies[i] );
				if( entropies[i] > max ) max = entropies[i];
			}

			boolean exponentiate = getAnimator().getExponentiate();
			boolean reflect = getAnimator().getReflect();

			//System.out.println( "\t max entropy = " + max );
			//double inverse = DOUBLE_ONE/max;
			double inverse = Double.NaN;
			if( max == DOUBLE_ZERO ) inverse = DOUBLE_ONE;
			else{
				if( exponentiate ) max = Math.pow( DOUBLE_TWO, max ) - DOUBLE_ONE;
				inverse = DOUBLE_ONE/max;
			}

			for( int i=0; i<entropies.length; i++ ){
				if( exponentiate ) entropies[i] = Math.pow( DOUBLE_TWO, entropies[i] ) - DOUBLE_ONE;
				entropies[i] = Math.abs( entropies[i]*inverse );
				if( reflect ) entropies[i] = DOUBLE_ONE - entropies[i];
			}

			for( int i=0; i<array.length; i++ ) ((IntensifiedDFV)array[i]).init( mySteps, (float)entropies[i] );
		}catch( Exception e ){
			die();
			e.printStackTrace();
		}
	}

	/** overridden */
	protected void animate(){
		Animator.finish( myArray );
	}

	protected InferenceEngine myInferenceEngine;
	protected int myCloneGeneration;
}
