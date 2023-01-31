package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.preference.*;

import java.awt.*;

/**
	@author Keith Cascio
	@since 031103
*/
public class SizeManager implements CoordinateTransformer
{
	public SizeManager( SamiamPreferences monitorPrefs )
	{
		setPreferences( monitorPrefs );
	}

	public Point		virtualToActual( Point p )
	{
		return p;
	}
	public Point		actualToVirtual( Point p )
	{
		return p;
	}

	public Dimension	virtualToActual( Dimension d )
	{
		if( myFlagZoom ) NetworkDisplay.scale( d, myZoomFactor );
		return d;
	}
	public Dimension	actualToVirtual( Dimension d )
	{
		if( myFlagZoom ) NetworkDisplay.scale( d, myZoomFactorInverse );
		return d;
	}

	public float		virtualToActual( float f )
	{
		if( myFlagZoom ) return f*((float)myZoomFactor);
		else return f;
	}
	public double		virtualToActual( double d )
	{
		if( myFlagZoom ) return d*myZoomFactor;
		else return d;
	}

	public void setPreferences( SamiamPreferences prefs )
	{
		getPreferences( prefs );
		setZoomFactor( ((Double)myevidDlgZoomFactor.getValue()).doubleValue() * DOUBLE_ONE_HUNDRETH );
	}

	public void updatePreferences( SamiamPreferences prefs )
	{
		getPreferences( prefs );
		if( myevidDlgZoomFactor.isRecentlyCommittedValue() ) setZoomFactor( ((Double)myevidDlgZoomFactor.getValue()).doubleValue() * DOUBLE_ONE_HUNDRETH );
	}

	protected void getPreferences( SamiamPreferences monitorPrefs )
	{
		myevidDlgZoomFactor = monitorPrefs.getMappedPreference( SamiamPreferences.evidDlgZoomFactor );
	}

	protected static Preference myevidDlgZoomFactor;

	public void setZoomFactor( double newZoom )
	{
		double oldZoom = myZoomFactor;

		if( DOUBLE_ZERO < newZoom && newZoom < DOUBLE_ONEHUNDRED )
		{
			myZoomFactor = newZoom;
			myZoomFactorInverse = (double)1/newZoom;

			myFlagZoom = myZoomFactor < DOUBLE_LOWER_BOUND || myZoomFactor > DOUBLE_UPPER_BOUND;
		}
	}

	protected double myZoomFactor = (double)1;
	protected double myZoomFactorInverse = (double)1;
	protected boolean myFlagZoom = false;

	//constants
	public static final double DOUBLE_ZERO = (double)0;
	public static final double DOUBLE_ONE = (double)1;
	public static final double DOUBLE_ONEHUNDRED = (double)100;
	public static final double DOUBLE_EPSILON = (double)0.001;

	//calculated constants
	public static final double DOUBLE_LOWER_BOUND = DOUBLE_ONE - DOUBLE_EPSILON;
	public static final double DOUBLE_UPPER_BOUND = DOUBLE_ONE + DOUBLE_EPSILON;
	public static final double DOUBLE_ONE_HUNDRETH = DOUBLE_ONE/DOUBLE_ONEHUNDRED;
}
