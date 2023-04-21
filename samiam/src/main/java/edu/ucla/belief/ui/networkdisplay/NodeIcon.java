package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.preference.*;

import edu.ucla.util.HiddenProperty;

import javax.swing.Icon;
import java.awt.*;

/**
 * This abstract class is the base class for different shaped
 * node icons.
 *
 * It can be drawn as selected or unselected.
*/
abstract public class NodeIcon implements Icon, PreferenceListener
{
	public static final double NEGATIVE_ONE = (double) -1;
	public static final double ZERO = (double) 0;

	public static final int INT_OPAQUE_ALPHA = (int)255;
	public static final int INT_HIDDEN_ALPHA = (int)100;
	public static final Dimension DIMENSION_HIDDEN = new Dimension( 10,5 );
	public static final BasicStroke STROKE_HIDDEN = new BasicStroke( (float)1 );

	/* Need a border, otherwise entire shape does not show up.*/
	private int myStrokeWidth = (int)1;
	/** Is the icon selected or not.*/
	private boolean isSelected = false;
	/** Is the icon observed or not.*/
	private boolean isObserved = false;
	protected boolean myFlagHidden = false;

	/** Size of the icon .*/
	protected Dimension mySize = new Dimension( 16,16 );
	protected Dimension mySizeEffective = new Dimension( 15,15 );
	protected int myTranslationEffective = (int)1;
	protected Dimension myImageRadius = new Dimension( 8,8 );
	protected double mySlope;

	//other options
	protected Color borderColor;
	protected Color borderColorObserved;
	protected Color backgroundColor;
	protected BasicStroke borderStroke;
	protected BasicStroke observedBorderStroke;
	protected BasicStroke selectedBorderStroke;

	protected DisplayableFiniteVariable myDVar = null;
	//protected SamiamPreferences myNetPrefs;

	/**
		@author Keith Cascio
		@since 050602
	*/
	public Dimension getSize( Dimension d )
	{
		if( d == null ) d = new Dimension();
		d.setSize( mySize );
		return d;
	}

	public NodeIcon( DisplayableFiniteVariable dVar )
	{
		myDVar = dVar;
		//this.myNetPrefs = null;//netPrefs;
		setPreferences();
		setSelected( isSelected );
	}

	/** Size of the icon including the border.*/
	public int getIconHeight()
	{
		return mySize.height;// + myStrokeWidth;
	}

	/** Size of the icon including the border.*/
	public int getIconWidth()
	{
		return mySize.width;// + myStrokeWidth;
	}

	/** Draws the icon using the graphics object.*/
	abstract public void paintIcon(Component c, Graphics g, int x, int y);
	/** Modifies the startpoint of a line from st to en so that it will appear
	* on the border of the shape instead of the center of the shape.*/
	abstract public void modifyStartPoint( Point st, Point en );
	/** Modifies the endpoint of a line from st to en so that it will appear
	* on the border of the shape instead of the center of the shape.*/
	abstract public void modifyEndPoint( Point st, Point en );

	/** Is the node selected.*/
	public boolean isSelected()
	{
		return isSelected;
	}

	public void setSelected( boolean selected )
	{
		isSelected = selected;

		if( isSelected )
		{
			myStrokeWidth = (int)( selectedBorderStroke.getLineWidth() );
			recalculateEffective();
		}
		else setObserved( isObserved );
	}

	public boolean isObserved()
	{
		return isObserved;
	}

	public void setObserved( boolean observed )
	{
		isObserved = observed;

		if( !isSelected() )
		{
			BasicStroke newStroke;
			if( isObserved ) newStroke = observedBorderStroke;
			else newStroke = borderStroke;

			myStrokeWidth = (int)( newStroke.getLineWidth() );
			recalculateEffective();
		}
	}

	/** @since 20021106 */
	protected void recalculateEffective()
	{
		int newWidth = Math.max( mySize.width - myStrokeWidth, (int)1 );
		int newHeight = Math.max( mySize.height - myStrokeWidth, (int)1 );
		mySizeEffective.setSize( newWidth, newHeight );

		int minRadius = Math.min( myImageRadius.width, myImageRadius.height );
		myTranslationEffective = Math.min( myStrokeWidth >> 1, minRadius );
	}

	/** Tells this icon to invert its current selection status.*/
	public void selectionSwitch()
	{
		isSelected = !isSelected;
	}

	/** Allow options to change.*/
	public void changeSize( Dimension sz )
	{
		mySize.setSize( sz );
		//myImageRadius.setSize( (mySize.width >> 1) + mySizeBorder, (mySize.height >> 1) + mySizeBorder );
		myImageRadius.setSize( (mySize.width >> 1), (mySize.height >> 1) );

		recalculateEffective();

		mySlope = ((double)mySize.height / (double)mySize.width);

		updateArrows();
	}

	/** @since 081704 */
	public void updateArrows(){
		if( myDVar != null && myDVar.getNodeLabel() != null ){
			myDVar.getNodeLabel().updateArrows( false );
		}
	}

	/** @since 081704 */
	public void setHidden( boolean flag ){
		myFlagHidden = flag;
	}

	/** @since 081704 */
	public boolean isHidden(){
		//return isHidden( myDVar );
		return myFlagHidden;
	}

	/** @since 081704 */
	public static boolean isHidden( DisplayableFiniteVariable var ){
		if( var == null || var.isSampleMode() ) return false;
		else return HiddenProperty.isHidden( var );
	}

	/** @since 081704 */
	public static Color adjustForHidden( DisplayableFiniteVariable var, Color color )
	{
		int alpha = isHidden( var ) ? INT_HIDDEN_ALPHA : INT_OPAQUE_ALPHA;
		return new Color( color.getRed(), color.getGreen(), color.getBlue(), alpha );
	}

	/** @since 081704 */
	public void recalculateHidden()
	{
		//changeBorderColor( adjustForHidden( myDVar, borderColor ) );
		//changeBackgroundColor( adjustForHidden( myDVar, backgroundColor ) );
		updateArrows();
	}

	/** Allow options to change.*/
	public void changeBorderColor( Color clr)
	{
		borderColor = clr;
	}

	/** Allow options to change.*/
	public void changeBackgroundColor( Color clr)
	{
		backgroundColor = clr;
	}

	/** @since 080504 */
	public Color getBackground(){
		return backgroundColor;
	}

	public void changeBorderStroke( BasicStroke st )
	{
		borderStroke = st;
		setSelected( isSelected );
	}

	public void changeObservedBorderStroke( BasicStroke st )
	{
		observedBorderStroke = st;
		setObserved( isObserved );
	}

	public void changeSelectedBorderStroke( BasicStroke st )
	{
		selectedBorderStroke = st;
		setSelected( isSelected );
	}

	public static final Dimension DIM_FALLBACK_NODE_SIZE = new Dimension( (int)80, (int)40 );

	/** @since 20020806 */
	protected boolean validateDimension( Dimension dim )
	{
		if( dim == null ) return false;
		else if( dim.width > (int)0 && dim.height > (int)0 ) return true;
		else return false;
	}

	public void changePackageOptions()
	{
		updatePreferences();
	}

	private static       TargetedBundle BUNDLE_OF_PREFERENCES;
	public  static final String         STR_KEY_PREFERENCE_BUNDLE = NodeIcon.class.getName();

	public static TargetedBundle validatePreferenceBundle( SamiamPreferences prefs ){
		if( BUNDLE_OF_PREFERENCES != null ) return BUNDLE_OF_PREFERENCES.validate( prefs );

		String   key  = STR_KEY_PREFERENCE_BUNDLE;
		String[] keys = new String[] { SamiamPreferences.nodeBorderClr, SamiamPreferences.nodeBorderClrObserved, SamiamPreferences.nodeBkgndClr, SamiamPreferences.nodeNormalStroke, SamiamPreferences.nodeObservedStroke, SamiamPreferences.nodeWideStroke };

		BUNDLE_OF_PREFERENCES = new TargetedBundle( key, keys, prefs ){
			public void begin( Object me, boolean force ){
				this.nodeicon = (NodeIcon) me;
			}

			public void setPreference( int index, Object value ){
				switch( index ){
					case 0:
						nodeicon.changeBorderColor( (Color) value );
						break;
					case 1:
						nodeicon.borderColorObserved = (Color) value;
						break;
					case 2:
						nodeicon.changeBackgroundColor( (Color) value );
						break;
					case 3:
						nodeicon.changeBorderStroke( new BasicStroke( ((Number)value).floatValue() ) );
						break;
					case 4:
						nodeicon.changeObservedBorderStroke( new BasicStroke( ((Number)value).floatValue() ) );
						break;
					case 5:
						nodeicon.changeSelectedBorderStroke( new BasicStroke( ((Number)value).floatValue() ) );
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			private NodeIcon nodeicon;
		};

		return BUNDLE_OF_PREFERENCES;
	}

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the COMMITTED values
	*/
	public void updatePreferences()
	{
		BUNDLE_OF_PREFERENCES.updatePreferences( NodeIcon.this );
	}

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the EDITED values
	*/
	public void previewPreferences()
	{
		BUNDLE_OF_PREFERENCES.previewPreferences( NodeIcon.this );
	}

	/**
		Call this method to force a PreferenceListener to
		reset itself
	*/
	public void setPreferences()
	{
		BUNDLE_OF_PREFERENCES.setPreferences( NodeIcon.this );
	}
}
