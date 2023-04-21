package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.preference.*;

import javax.swing.Icon;
import java.awt.*;

/** This class draws a rectanglular icon
	showing the certainty of evidence.

	The evidence can be set as automatic
	evidence or manually selected evidence. */
public class EvidenceIcon implements Icon, PreferenceListener
{
	public    static   final   int
	  HORIZONTAL  = 1,
	  VERTICAL    = 2,
	  /** Size of the border on each side.*/
	  BORDER      = 2,
	  BORDER_DUB  = BORDER << 1;
	protected static   final   Color
	  borderColor = Color.black;
	public    static   final   double[]
	  VALUES_WARN = new double[]{ 1.0 };

	/** Size of the icon not including the border.*/
	protected Dimension   myVirtualSize  = new Dimension(),
	                      myActualSize   = new Dimension();
	/** Determines whether the evidence was set manually or automatically.*/
	private   boolean     myFlagObserved = false,
	                      myFlagWarn     = false;
	/** Certainty of the evidence.*/
	private   double[]    values         = new double[]{ 0.0 };
	/** Direction to draw (vert vs hor) */
	private   int         direction      = HORIZONTAL,
	                      cardinality    = 1;
	private   Color[]     manualColors, myWarnColors, autoColors;

	/** @since 20030307 */
	public void recalculateActual( CoordinateTransformer xformer )
	{
		myActualSize.setSize(   myVirtualSize );
		xformer.virtualToActual( myActualSize );
	}

	/** @since 20030307 */
	public void setDoZoom( boolean flag )
	{
		if( ! flag ){ myActualSize.setSize( myVirtualSize ); }
	}

	protected EvidenceIcon( Dimension sz, Color man, Color auto )
	{
		this( sz.width, sz.height, man, auto);
	}

	protected EvidenceIcon( int w, int h, Color man, Color auto )
	{
		changeVirtualSize( w, h );
		manualColors = new Color[]{ man  };
		autoColors   = new Color[]{ auto };
	}

	public EvidenceIcon( SamiamPreferences monitorPrefs )
	{
		this( 5, 5, Color.red, Color.blue );

		EvidenceIcon.validatePreferenceBundle( monitorPrefs );
		setPreferences();
	}

	/** Sets the certainty of the evidence.	Will print out an error on
	    System.err if out of range. */
	public EvidenceIcon setValue( double v )
	{
		cardinality = 1;
		if( ((v >= 0.0) && (v <= 1.0)) || Double.isNaN( v ) )
		{
			if(   values   == null ){ values = new double[]{ v }; }
			else{ values[0] = v; }
		}
		else{ throw new IllegalArgumentException( "EvidenceIcon.setValue( "+v+" ) is out of range." ); }

		return this;
	}

	/** @since 20080226 */
	public EvidenceIcon setValues( double[] vals, int card ){
		cardinality = card;
		if( (values == null) || (values.length < cardinality) ){ values = new double[ cardinality ]; }

		double v;
		for( int i=0; i<cardinality; i++ ){
			v = vals[i];
			if( ((v >= 0.0) && (v <= 1.0)) || Double.isNaN( v ) ){ values[i] = v; }
			else{ throw new IllegalArgumentException( "EvidenceIcon.setValue["+i+"]( "+v+" ) is out of range." ); }
		}

		return this;
	}

	/** Used to set manual vs automatic evidence selection.*/
	public void setManuallySetEvid( boolean in)
	{
		setWarn( false );
		myFlagObserved = in;
	}

	/** @since 20030710 */
	public boolean isObserved()
	{
		return myFlagObserved;
	}

	/** @since 20030710 */
	public void setWarn( boolean flag )
	{
		myFlagWarn = flag;
	}

	/** Size of the icon including the border.*/
	public int getIconHeight()
	{
		return myActualSize.height + BORDER_DUB;
	}

	/** Size of the icon including the BORDER.*/
	public int getIconWidth()
	{
		return myActualSize.width + BORDER_DUB;
	}

	/** Draws the icon using the graphics object.*/
	public void paintIcon( Component c, Graphics g, int x, int y )
	{
		int      card   = this.cardinality;
		double[] vals   = this.values;
		Color[]  colors = this.autoColors;

		if(      myFlagWarn     ){
			card        = 1;
			colors      = this.myWarnColors;
			vals        = VALUES_WARN;
		}
		else if( myFlagObserved ){
			card        = 1;
			colors      = manualColors;
		}

		double displayVal;
		int    cx = x+BORDER, cy = y, width = 1, height = 1, widthpart = myActualSize.width, heightpart = myActualSize.height;
		if(      direction == HORIZONTAL ){ heightpart /= card; cy = y+BORDER; height = heightpart; }
		else if( direction == VERTICAL   ){  widthpart /= card;                width  =  widthpart; }
		for( int i=0; i<card; i++ ){
			displayVal = Double.isNaN( values[i] ) ? 0.0 : values[i];

			if(      direction == HORIZONTAL ){  width = (int)(displayVal* widthpart); }
			else if( direction == VERTICAL   ){ height = (int)(displayVal*heightpart); cy = (y+heightpart+BORDER)-(int)(displayVal*heightpart); }

			g.setColor( colors[ i % colors.length ] );
			g.fillRect( cx, cy, width, height );

			if(      direction == HORIZONTAL ){ cy += heightpart; }
			else if( direction == VERTICAL   ){ cx +=  widthpart; }
		}
		g.setColor( borderColor );
		g.drawRect( x+BORDER, y+BORDER, myActualSize.width, myActualSize.height );
	}

	/**
	* Change the direction.	The parameter should be either
	* HORIZONTAL (default) or VERTICAL.
	*/
	public void setDirection( int d)
	{
		if( d == HORIZONTAL || d == VERTICAL)
		{
			direction = d;
		}
	}

	/** Allow options to change.*/
	public void changeManualColor( Color clr )
	{
		if(   manualColors == null ){ manualColors = new Color[]{ clr }; }
		else{ manualColors[0] = clr; }
	}

	/** Allow options to change.*/
	public void changeAutoColor( int index, Color clr ){
		int size = index + 1;
		if( (autoColors == null) || (autoColors.length < size) ){
			Color[] old = autoColors;
			autoColors = new Color[ size ];
			if( old != null ){ System.arraycopy( old, 0, autoColors, 0, old.length ); }
		}
		autoColors[ index ] = clr;
	}

	/** @since 20080226 */
	public Color[] getAutoColors(){
		return autoColors;
	}

	/** Allow options to change.*/
	public void changeVirtualSize( int w, int h)
	{
		myVirtualSize.setSize( w, h);
		myActualSize.setSize( myVirtualSize );
	}

	/** Allow options to change.*/
	/*public void changeAllOptions( Color man, Color warn, Color auto, int w, int h )
	{
		changeManualColor( man );
		myWarnColors = warn;
		changeAutoColor( auto );
		changeVirtualSize( w, h );
	}*/

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the COMMITTED values
	*/
	public void updatePreferences()
	{
		BUNDLE_OF_PREFERENCES.updatePreferences( EvidenceIcon.this );
	}

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the EDITED values
	*/
	public void previewPreferences()
	{
		BUNDLE_OF_PREFERENCES.previewPreferences( EvidenceIcon.this );
	}

	/**
		Call this method to force a PreferenceListener to
		reset itself
	*/
	public void setPreferences()
	{
		BUNDLE_OF_PREFERENCES.setPreferences( EvidenceIcon.this );
	}

	private static       TargetedBundle BUNDLE_OF_PREFERENCES;
	public  static final String         STR_KEY_PREFERENCE_BUNDLE = EvidenceIcon.class.getName();

	public static TargetedBundle validatePreferenceBundle( SamiamPreferences prefs ){
		if( BUNDLE_OF_PREFERENCES != null ) return BUNDLE_OF_PREFERENCES.validate( prefs );

		String   key  = STR_KEY_PREFERENCE_BUNDLE;
		String[] keys = new String[] { SamiamPreferences.evidDlgRectSize, SamiamPreferences.evidDlgManualClr, SamiamPreferences.evidDlgWarnClr, SamiamPreferences.evidDlgAutoClr, SamiamPreferences.evidDlgAutoClr2 };

		BUNDLE_OF_PREFERENCES = new TargetedBundle( key, keys, prefs ){
			public void begin( Object me, boolean force ){
				this.evidenceicon = (EvidenceIcon) me;
			}

			public void setPreference( int index, Object value ){
				switch( index ){
					case 0:
						Dimension dimEvidDlgRectSize = (Dimension) value;
						evidenceicon.changeVirtualSize( dimEvidDlgRectSize.width, dimEvidDlgRectSize.height );
						break;
					case 1:
						evidenceicon.changeManualColor( (Color) value );
						break;
					case 2:
						evidenceicon.myWarnColors = new Color[]{ (Color) value };
						break;
					case 3:
						evidenceicon.changeAutoColor( 0, (Color) value );
						break;
					case 4:
						evidenceicon.changeAutoColor( 1, (Color) value );
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			private EvidenceIcon evidenceicon;
		};

		return BUNDLE_OF_PREFERENCES;
	}
}
