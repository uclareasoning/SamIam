package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.networkdisplay.*;

import edu.ucla.belief.*;

import java.util.ArrayList;

/**
	@author Keith Cascio
	@since 072904
*/
public class ScaleMarginalsThread extends AnimationThread
{
	public ScaleMarginalsThread( int steps, InferenceEngine ie, Animator anim )
	{
		super( steps, anim, "ScaleMarginalsThread" );
		myInferenceEngine = ie;
	}

	public ScaleMarginalsThread( int steps, InferenceEngine ie, DisplayableBeliefNetwork bn, CoordinateTransformer xformer, Animator anim )
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
	}

	public ScaleMarginalsThread( ScaleMarginalsThread toCopy, InferenceEngine ie )
	{
		super( toCopy );
		myInferenceEngine = ie;
		mySteps = getAnimator().getSteps();
		myCloneGeneration = ++toCopy.myCloneGeneration;
		//setName( makeClonedName( toCopy.getName(), myCloneGeneration ) );
	}

	public static String makeClonedName( String base, int clonegeneration )
	{
		int index = base.indexOf( STR_NAME_TOKEN_CLONE );
		String stem = null;
		if( index < 0 ) stem = base;
		else stem = base.substring( 0, index );
		return stem + STR_NAME_TOKEN_CLONE + Integer.toString( clonegeneration );
	}

	public ScaleMarginalsThread clone( InferenceEngine ie )
	{
		return new ScaleMarginalsThread( this, ie );
	}

	/** overridden */
	protected void init( SomethingAnimated[] array ){
		//System.out.println( "("+ getName() + ").init()" );
		try{
			if( getReplay() ){
				super.init( array );
				return;
			}

			for( int i=0; i<array.length; i++ ) ((ScaledDFV)array[i]).init( mySteps, myInferenceEngine );
		}
		catch( Exception e ){
			die();
			e.printStackTrace();
		}
	}

	public static final String STR_NAME_TOKEN_CLONE = " clone ";

	protected InferenceEngine myInferenceEngine;
	protected int myCloneGeneration;
}
