package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.preference.*;

/**
	@author Keith Cascio
	@since 080404
*/
public class AnimationPreferenceHandler implements PreferenceMediator
{
	public static final long LONG_DELAY_MS_DEFAULT = (long)1;
	public static final long LONG_DELAY_MS_FLOOR = (long)0;
	public static final long LONG_DELAY_MS_CEILING = (long)1000;
	private long myLongDelayMS = LONG_DELAY_MS_DEFAULT;

	public static final double DOUBLE_MIN_SCALE_FACTOR_DEFAULT = (double)0.75;
	public static final double DOUBLE_MIN_SCALE_FACTOR_FLOOR = (double)0.01;
	public static final double DOUBLE_MIN_SCALE_FACTOR_CEILING = (double)0.99;

	public static final double DOUBLE_MAX_SCALE_FACTOR_DEFAULT = (double)2;
	public static final double DOUBLE_MAX_SCALE_FACTOR_FLOOR = (double)1.01;
	public static final double DOUBLE_MAX_SCALE_FACTOR_CEILING = (double)5;

	private double myScaleFactor;
	private double myOffset;

	public static final int INT_STEPS_DEFAULT = (int)50;
	public static final int INT_STEPS_FLOOR = (int)1;
	public static final int INT_STEPS_CEILING = (int)1000;
	private int mySteps = INT_STEPS_DEFAULT;

	public static final boolean FLAG_INTENSIFY_ENTROPY_DEFAULT = true;
	private boolean myFlagIntensifyEntropy = FLAG_INTENSIFY_ENTROPY_DEFAULT;

	public static final boolean FLAG_SCALE_IMPACT_DEFAULT = true;
	private boolean myFlagScaleImpact = FLAG_SCALE_IMPACT_DEFAULT;

	public static final boolean FLAG_LOCK_STEP_DEFAULT = true;
	private boolean myFlagLockStep = FLAG_LOCK_STEP_DEFAULT;

	public static final boolean FLAG_REFLECT_DEFAULT = false;
	private boolean myFlagReflect = FLAG_REFLECT_DEFAULT;

	public static final boolean FLAG_EXPONENTIATE_DEFAULT = true;
	private boolean myFlagExponentiate = FLAG_EXPONENTIATE_DEFAULT;

	public static final boolean FLAG_RESET_FIRST_DEFAULT = true;
	private boolean myFlagResetFirst = FLAG_RESET_FIRST_DEFAULT;

	public static final long LONG_PAUSE_DEFAULT = (long)500;
	public static final long LONG_PAUSE_FLOOR = (long)0;
	public static final long LONG_PAUSE_CEILING = (long)10000;
	private long myInterveningPause = LONG_PAUSE_DEFAULT;

	public static final float FLOAT_HUE_SCALE_DEFAULT = (float)0.73;
	public static final float FLOAT_HUE_OFFSET_DEFAULT = (float)-0.03;
	private static float FLOAT_HUE_SCALE = FLOAT_HUE_SCALE_DEFAULT;
	private static float FLOAT_HUE_OFFSET = FLOAT_HUE_OFFSET_DEFAULT;

	/** @since 080504 */
	public static abstract class AnimationColorHandler
	{
		public AnimationColorHandler( String name ){
			myName = name;
		}

		public String toString(){
			return myName;
		}

		abstract public float getCurrentIntensity( float[] HSBVals );
		abstract public void updateValues( float[] virtualVals, float intensity, float[] newValues );

		private String myName;
	}

	/** @since 081004 */
	public static float adjustHue( float valueIn ){
		float valueOut = (valueIn * FLOAT_HUE_SCALE) + FLOAT_HUE_OFFSET;
		/* note: no need to take the remainder - Color.getHSBColor() handles that */
		//while( valueOut > ColorIntensitySample.FLOAT_ONE ) valueOut -= ColorIntensitySample.FLOAT_ONE;
		//while( valueOut < ColorIntensitySample.FLOAT_ZERO ) valueOut += ColorIntensitySample.FLOAT_ONE;
		//valueOut = (float) Math.IEEEremainder( (double)valueOut, IntensifyEntropyThread.DOUBLE_ONE );
		return valueOut;
	}

	/** @since 081104 */
	public static void setHueAdjustment( float scale, float offset ){
		FLOAT_HUE_SCALE = scale;
		FLOAT_HUE_OFFSET = offset;
	}

	public static final AnimationColorHandler HUE = new AnimationColorHandler( "hue" )
	{
		public float getCurrentIntensity( float[] HSBVals ){
			return HSBVals[0];
		}
		public void updateValues( float[] virtualVals, float intensity, float[] newValues ){
			//newValues[0] = intensity;
			newValues[0] = adjustHue( intensity );
			newValues[1] = virtualVals[1];
			newValues[2] = virtualVals[2];
		}
	};
	public static final AnimationColorHandler SATURATION = new AnimationColorHandler( "saturation" )
	{
		public float getCurrentIntensity( float[] HSBVals ){
			return HSBVals[1];
		}
		public void updateValues( float[] virtualVals, float intensity, float[] newValues ){
			newValues[0] = virtualVals[0];
			newValues[1] = intensity;
			newValues[2] = virtualVals[2];
		}
	};
	public static final AnimationColorHandler BRIGHTNESS = new AnimationColorHandler( "brightness" )
	{
		public float getCurrentIntensity( float[] HSBVals ){
			return HSBVals[2];
		}
		public void updateValues( float[] virtualVals, float intensity, float[] newValues ){
			newValues[0] = virtualVals[0];
			newValues[1] = virtualVals[1];
			newValues[2] = intensity;
		}
	};
	public static final AnimationColorHandler ANIMATIONCOLORHANDLER_DEFAULT = SATURATION;
	public static final AnimationColorHandler[] ARRAY_ANIMATIONCOLORHANDLERS = new AnimationColorHandler[]{ SATURATION, BRIGHTNESS, HUE };
	private AnimationColorHandler myAnimationColorHandler;

	/** @since 080504 */
	public static AnimationColorHandler forString( String name )
	{
		for( int i=0; i<ARRAY_ANIMATIONCOLORHANDLERS.length; i++ ){
			if( ARRAY_ANIMATIONCOLORHANDLERS[i].toString().equals( name ) ) return ARRAY_ANIMATIONCOLORHANDLERS[i];
		}
		return (AnimationColorHandler)null;
	}

	private AnimationPreferenceHandler()
	{
		setScaleMinMax( DOUBLE_MIN_SCALE_FACTOR_DEFAULT, DOUBLE_MAX_SCALE_FACTOR_DEFAULT );
	}

	private static AnimationPreferenceHandler INSTANCE;

	/** @since 080504 */
	public static AnimationPreferenceHandler getInstance()
	{
		if( INSTANCE == null ) INSTANCE = new AnimationPreferenceHandler();
		return INSTANCE;
	}

	public long getDelay(){
		return myLongDelayMS;
	}

	public int getSteps(){
		return mySteps;
	}

	public double getScaleFactor(){
		return myScaleFactor;
	}

	public double getOffset(){
		return myOffset;
	}

	/** @since 080504 */
	public AnimationColorHandler getAnimationColorHandler(){
		return myAnimationColorHandler;
	}

	/** @since 080804 */
	public boolean getScale(){
		return myFlagScaleImpact;
	}

	/** @since 080804 */
	public boolean getIntensify(){
		return myFlagIntensifyEntropy;
	}

	/** @since 080804 */
	public boolean getLockStep(){
		return myFlagLockStep;
	}

	/** @since 081004 */
	public boolean getReflect(){
		return myFlagReflect;
	}

	/** @since 081004 */
	public boolean getExponentiate(){
		return myFlagExponentiate;
	}

	/** @since 081004 */
	public boolean getResetFirst(){
		return myFlagResetFirst;
	}

	/** @since 081004 */
	public long getInterveningPause(){
		return myInterveningPause;
	}

	public void setScaleMinMax( double minScale, double maxScale )
	{
		myOffset = minScale;
		myScaleFactor = maxScale - minScale;
	}

	public void updatePreferences( SamiamPreferences prefs )
	{
		PreferenceGroup animationPrefs = prefs.getPreferenceGroup( SamiamPreferences.AnimationDspNme );
		if( animationPrefs.isRecentlyCommittedValue() ) updateAnimationPreferences( prefs, false );
	}

	public void updateAnimationPreferences( SamiamPreferences animationPrefs, boolean force )
	{
		Preference min = animationPrefs.getMappedPreference( SamiamPreferences.animationMinimumScale );
		Preference max = animationPrefs.getMappedPreference( SamiamPreferences.animationMaximumScale );
		Preference slowdown = animationPrefs.getMappedPreference( SamiamPreferences.animationSlowdownMilliseconds );
		Preference steps = animationPrefs.getMappedPreference( SamiamPreferences.animationSteps );
		ObjectPreference ach = (ObjectPreference) animationPrefs.getMappedPreference( SamiamPreferences.animationColorComponent );
		Preference intensify = animationPrefs.getMappedPreference( SamiamPreferences.animationIntensifyEntropy );
		Preference scale = animationPrefs.getMappedPreference( SamiamPreferences.animationScaleImpact );
		Preference lockstep = animationPrefs.getMappedPreference( SamiamPreferences.animationLockStep );
		Preference reflect = animationPrefs.getMappedPreference( SamiamPreferences.animationReflect );
		Preference exponentiate = animationPrefs.getMappedPreference( SamiamPreferences.animationExponentiate );
		Preference pausemillis = animationPrefs.getMappedPreference( SamiamPreferences.animationInterveningPauseMillis );
		Preference resetfirst = animationPrefs.getMappedPreference( SamiamPreferences.animationResetFirst );

		if( min.isRecentlyCommittedValue() || max.isRecentlyCommittedValue() || force ){
			double minScale = ((Number)min.getValue()).doubleValue();
			double maxScale = ((Number)max.getValue()).doubleValue();
			setScaleMinMax( minScale, maxScale );
		}

		if( slowdown.isRecentlyCommittedValue() || force ){
			myLongDelayMS = ((Number)slowdown.getValue()).longValue();
		}

		if( steps.isRecentlyCommittedValue() || force ){
			mySteps = ((Number)steps.getValue()).intValue();
		}

		if( ach.isRecentlyCommittedValue() || force ){
			myAnimationColorHandler = (AnimationColorHandler)ach.getValue();
		}

		if( intensify.isRecentlyCommittedValue() || force ){
			myFlagIntensifyEntropy = ((Boolean)intensify.getValue()).booleanValue();
		}

		if( scale.isRecentlyCommittedValue() || force ){
			myFlagScaleImpact = ((Boolean)scale.getValue()).booleanValue();
		}

		if( lockstep.isRecentlyCommittedValue() || force ){
			myFlagLockStep = ((Boolean)lockstep.getValue()).booleanValue();
		}

		if( reflect.isRecentlyCommittedValue() || force ){
			myFlagReflect= ((Boolean)reflect.getValue()).booleanValue();
		}

		if( exponentiate.isRecentlyCommittedValue() || force ){
			myFlagExponentiate = ((Boolean)exponentiate.getValue()).booleanValue();
		}

		if( pausemillis.isRecentlyCommittedValue() || force ){
			myInterveningPause = ((Integer)pausemillis.getValue()).longValue();
		}

		if( resetfirst.isRecentlyCommittedValue() || force ){
			myFlagResetFirst = ((Boolean)resetfirst.getValue()).booleanValue();
		}
	}

	/** @since 080504 */
	public void setPreferences( SamiamPreferences prefs )
	{
		PreferenceGroup animationPrefs = prefs.getPreferenceGroup( SamiamPreferences.AnimationDspNme );
		updateAnimationPreferences( prefs, true );
	}

	/** @since 080504 */
	public void massagePreferences( SamiamPreferences prefs )
	{
		PreferenceGroup animationPrefs = prefs.getPreferenceGroup( SamiamPreferences.AnimationDspNme );
		DoublePreference min = (DoublePreference) prefs.getMappedPreference( SamiamPreferences.animationMinimumScale );
		DoublePreference max = (DoublePreference) prefs.getMappedPreference( SamiamPreferences.animationMaximumScale );
		IntegerPreference slowdown = (IntegerPreference) prefs.getMappedPreference( SamiamPreferences.animationSlowdownMilliseconds );
		IntegerPreference steps = (IntegerPreference) prefs.getMappedPreference( SamiamPreferences.animationSteps );
		IntegerPreference pausemillis = (IntegerPreference) prefs.getMappedPreference( SamiamPreferences.animationInterveningPauseMillis );

		min.setBounds( DOUBLE_MIN_SCALE_FACTOR_FLOOR, DOUBLE_MIN_SCALE_FACTOR_CEILING );
		max.setBounds( DOUBLE_MAX_SCALE_FACTOR_FLOOR, DOUBLE_MAX_SCALE_FACTOR_CEILING );
		slowdown.setBounds( (int)LONG_DELAY_MS_FLOOR, (int)LONG_DELAY_MS_CEILING );
		steps.setBounds( INT_STEPS_FLOOR, INT_STEPS_CEILING );
		pausemillis.setBounds( (int)LONG_PAUSE_FLOOR, (int)LONG_PAUSE_CEILING );
	}
}
