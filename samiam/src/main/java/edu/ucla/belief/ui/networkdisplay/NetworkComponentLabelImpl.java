package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.util.HiddenProperty;

/** @author keith cascio
	@since 20060522 */
public abstract class NetworkComponentLabelImpl extends JLabel implements NetworkComponentLabel
{
	protected static float  FLOAT_INVALID           = (float)-1;
	protected static float  FLOAT_BASE_FONT_SIZE    = FLOAT_INVALID;
	private   static Font   FONT_DEFAULT            = null;
	private   static String STR_FONT_FAMILY_DEFAULT = "lucida sans";//"times new roman";//"courier";//"arial";

	//protected SamiamPreferences myNetPrefs;

	/** List of Arrow objects that come into this node. */
	protected ArrayList inComingEdgeList = new ArrayList( 2 );
	/** List of Arrow objects that leave this node. */
	protected ArrayList outBoundEdgeList = new ArrayList( 2 );

	protected SelectionListener mySelectionListener;

	public NetworkComponentLabelImpl(	String text,
						NodeIcon image,
						CoordinateTransformer xformer,
						SelectionListener list,
						SamiamPreferences netPrefs,
						Point initVirtualLocation,
						boolean sample )
	{
		super( text, image, AbstractButton.CENTER );

		this.myCoordinateTransformer = xformer;
		NetworkComponentLabelImpl.validatePreferenceBundle( netPrefs );
		this.mySelectionListener     = list;

		init( text, initVirtualLocation, sample );
	}

	/** @since 20040813 */
	public NetworkComponentLabelImpl(	String text,
						CoordinateTransformer xformer,
						SelectionListener list,
						SamiamPreferences netPrefs,
						Point initVirtualLocation,
						boolean sample )
	{
		this( text, (NodeIcon)null, xformer, list, netPrefs, initVirtualLocation, sample );
	}

	/** @since 20060522 */
	protected void init( String text, Point initVirtualLocation, boolean sample )
	{
		if( FLOAT_BASE_FONT_SIZE == FLOAT_INVALID )
		{
			FLOAT_BASE_FONT_SIZE = this.getFont().getSize2D();
			String encoding = STR_FONT_FAMILY_DEFAULT + "-plain-" + Integer.toString( (int)FLOAT_BASE_FONT_SIZE );
			FONT_DEFAULT = Font.decode( encoding );
			//System.out.println( "default font ("+encoding+"): " + FONT_DEFAULT.getFontName() );
			if( FONT_DEFAULT.getFamily().toLowerCase().indexOf( STR_FONT_FAMILY_DEFAULT ) == -1 ) FONT_DEFAULT = (Font)null;
		}

		//NetworkComponentLabelImpl.this.setShapePreference();
		BUNDLE_OF_PREFERENCES.setPreference( NetworkComponentLabelImpl.this, 3 );
		if( !sample ) setActualLocation( initVirtualLocation );
	}

	/** @since 20060522 */
	public void initLazy(){
		setVerticalTextPosition(   AbstractButton.CENTER );
		setHorizontalTextPosition( AbstractButton.CENTER );

		if( FONT_DEFAULT != null ) this.setFont( FONT_DEFAULT );

		//setText( text );
		///////////////////////////////////////////////////////////////////
		//
		// Profiling shows that setToolTipText() causes a major performance
		// drag on large networks.  If we need tooltips, set them later
		// in a low-priority thread, not here at construction time.
		// keith cascio 20060521
		//
		//setToolTipText( text );
		//
		///////////////////////////////////////////////////////////////////

		setPreferences();

		//if( !sample && FLAG_VERBOSE_SETFONTSIZE ){
		//	flagVerboseSetFontSize = true;
		//	FLAG_VERBOSE_SETFONTSIZE = false;
		//}
	}

	/** @since 20060522 */
	public JLabel asJLabel(){
		return this;
	}

	/** for the specific case, this is likely faster than sun's drawing routines
		because
		(1) it uses only a Rectangle for to calculate clipping
		(2) it does nothing with the Graphics object but translate()
		(3) it assumes JLabel can paint itself correctly (SunGraphicsCallback seems wary)

		based on sun.awt.SunGraphicsCallback jdk1.5.0_06\jdksrc\j2se\src\share\classes\sun\awt\SunGraphicsCallback.java
		based on java.awt.GraphicsCallback   jdk1.5.0_06\jdksrc\j2se\src\share\classes\java\awt\GraphicsCallback.java or jdk1.5.0_06\src\java\awt\GraphicsCallback.java
		@since 20060731 */
	public void paintNetworkComponent( Graphics g, Rectangle viewRect ){
		if( !isVisible() ) return;
		Rectangle bounds = getBounds( RECTANGLE_PAINT_UTIL );
		if( viewRect.intersects( bounds ) ){
			Graphics cg = g.create();
			try{
				g.translate( bounds.x, bounds.y );//constrainGraphics( cg, bounds );
				super.paint( cg );
			}finally{
				cg.dispose();
			}
		}
	}
	private static Rectangle RECTANGLE_PAINT_UTIL = new Rectangle();
	/*public void constrainGraphics( Graphics g, Rectangle bounds ){
		g.translate( bounds.x, bounds.y );
		g.clipRect( 0, 0, bounds.width, bounds.height );
	}
	public void constrainGraphics( Graphics g, Rectangle bounds ){
        if( g instanceof sun.awt.ConstrainableGraphics ){
            ((sun.awt.ConstrainableGraphics)g).constrain( bounds.x, bounds.y, bounds.width, bounds.height );
        }
        else g.translate( bounds.x, bounds.y );
        g.clipRect(0, 0, bounds.width, bounds.height);
    }*/

	/** @since 031003 */
	public Rectangle getBoundsManaged( Rectangle rv ){
		return getBounds( rv );
	}

	/** interface SamiamUserModal
		@since 060805 */
	public void setSamiamUserMode( SamiamUserMode mode ){/*noop*/}

	/** interface EvidenceChangeListener
		@author Keith Cascio
		@since 071003 */
	public void warning( EvidenceChangeEvent ece ) {}

	/** interface EvidenceChangeListener */
	public void evidenceChanged( EvidenceChangeEvent ece ){/*noop*/}

	public void evidenceChanged( EvidenceChangeEvent ece, double globalMaximumProbability )
	{
		evidenceChanged( ece );
	}

	/** interface NetworkComponentLabel
		@since 060905 */
	public boolean isEvidenceDialogShown() { return false; }

	/** interface NetworkComponentLabel */
	public void hideEvidenceDialog(){/*noop*/}

	/** interface NetworkComponentLabel */
	public DisplayableFiniteVariable getFiniteVariable(){ return null; }

	/** Size of the icon that it uses.*/
	public int getHeight()
	{
		Icon icon = getIcon();
		if( icon != null ){ return icon.getIconHeight(); }
		DisplayableFiniteVariable var = getFiniteVariable();
		if( var  != null ){ return var.getDimension( new Dimension() ).height; }
		return 0x10;
	}

	/** Size of the icon that it uses.*/
	public int getWidth()
	{
		Icon icon = getIcon();
		if( icon != null ){ return icon.getIconWidth(); }
		DisplayableFiniteVariable var = getFiniteVariable();
		if( var  != null ){ return var.getDimension( new Dimension() ).width; }
		return 0x10;
	}

	/** Return the nodeIcon that this label uses.*/
	public NodeIcon getNodeIcon()
	{
		return (NodeIcon)getIcon();
	}

	/** @return true if the selection status changed as a result of this call */
	public boolean setSelected( boolean selected ){
		return setSelected( selected, true );
	}

	/** @return true if the selection status changed as a result of this call
		@param notify notify selection listeners and repaint() */
	public boolean setSelected( boolean selected, boolean notify ){
		if( isSelected() != selected ){
			getNodeIcon().setSelected( selected );
			if( notify ){
				repaint();
				mySelectionListener.selectionChanged( this );
			}
			return true;
		}
		else return false;
	}

	public void selectionSwitch()
	{
		setSelected( !isSelected() );
	}

	public boolean isSelected()
	{
		return getNodeIcon().isSelected();
	}

	/** Allow options to change.*/
	public void changeTextColor( Color clr )
	{
		//System.out.println( "NetworkComponentLabelImpl.changeTextColor()" );

		setForeground( clr );
		repaint();
	}

	/**
		Allow options to change.
	*/
	public void changePackageOptions()// SamiamPreferences netPrefs, SamiamPreferences monitorPrefs )
	{
		updatePreferencesImpl();
	}

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the COMMITTED values
	*/
	public void updatePreferences(){
		updatePreferencesImpl();
	}

	/** @since 20030320 */
	private final void updatePreferencesImpl()
	{
		getNodeIcon().updatePreferences();

		BUNDLE_OF_PREFERENCES.updatePreferences( NetworkComponentLabelImpl.this );
	}

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the EDITED values
	*/
	public void previewPreferences()
	{
		BUNDLE_OF_PREFERENCES.previewPreferences( NetworkComponentLabelImpl.this );

		getNodeIcon().previewPreferences();
	}

	/**
		Call this method to force a PreferenceListener to
		reset itself
	*/
	public void setPreferences()
	{
		BUNDLE_OF_PREFERENCES.setPreferences( NetworkComponentLabelImpl.this );

		NodeIcon icon = getNodeIcon();
		if( icon != null ) icon.setPreferences();
	}

	/** @since 20040813 */
	protected void setShapePreference( IconFactory factory )
	{
		/*if( mynodeShape == null ){
			if( this.myNetPrefs == null ) return;
			getPreferences();
		}

		IconFactory factory = (IconFactory) mynodeShape.getValue();*/

		NodeIcon current = getNodeIcon();

		if( !factory.corresponds( current ) ){
			NodeIcon newIcon = factory.makeIcon( getFiniteVariable() );
			if( current != null ){
				newIcon.setSelected( current.isSelected() );
				newIcon.setObserved( current.isObserved() );
				newIcon.setHidden( current.isHidden() );
			}
			setIcon( newIcon );
			this.recalculateActualSize();
		}
	}

	/** @since 081704 */
	public boolean shouldHide()
	{
		DisplayableFiniteVariable dvar = getFiniteVariable();
		if( (dvar != null) && (!dvar.isSampleMode()) ){
			boolean flagOverride = dvar.getNetworkInternalFrame().getSamiamUserMode().contains( SamiamUserMode.HIDE );
			return ( flagOverride && (dvar.getProperty( HiddenProperty.PROPERTY ) == HiddenProperty.PROPERTY.TRUE) );
		}
		return false;
	}

	/** @since 081704 */
	public void handleHidden()
	{
		NodeIcon icon = getNodeIcon();
		if( icon != null ){
			icon.setHidden( shouldHide() );
			icon.recalculateHidden();
		}
		repaint();
	}

	/** @since 080804 */
	private void setBoldPreference( Object value )
	{
		boolean isBold = ((Boolean)value).booleanValue();
		Font font = getFont();

		//System.out.println( "(NetworkComponentLabelImpl)"+getText()+".setBoldPreference( "+isBold+" <-> "+font.isBold()+" )" );

		if( font.isBold() != isBold ){
			int style = isBold ? Font.BOLD : Font.PLAIN;
			setFont( font.deriveFont( style ) );
		}
	}

	protected static       TargetedBundle BUNDLE_OF_PREFERENCES;
	public    static final String         STR_KEY_PREFERENCE_BUNDLE = NetworkComponentLabelImpl.class.getName();

	private static TargetedBundle validatePreferenceBundle( SamiamPreferences prefs ){
		if( BUNDLE_OF_PREFERENCES != null ) return BUNDLE_OF_PREFERENCES.validate( prefs );

		NodeIcon.validatePreferenceBundle( prefs );

		String   key  = STR_KEY_PREFERENCE_BUNDLE;
		String[] keys = new String[] { SamiamPreferences.nodeTextClr, SamiamPreferences.nodeBkgndClr, SamiamPreferences.nodeFontBold, SamiamPreferences.nodeShape };

		BUNDLE_OF_PREFERENCES = new TargetedBundle( key, keys, prefs ){
			public void begin( Object me, boolean force ){
				this.networkcomponentlabelimpl = (NetworkComponentLabelImpl) me;
			}

			public void setPreference( int index, Object value ){
				switch( index ){
					case 0:
						networkcomponentlabelimpl.changeTextColor( (Color)value );
						break;
					case 1:
						networkcomponentlabelimpl.setVirtualColor( (Color)value );
						break;
					case 2:
						networkcomponentlabelimpl.setBoldPreference( value );
						break;
					case 3:
						networkcomponentlabelimpl.setShapePreference( (IconFactory) value );
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			private NetworkComponentLabelImpl networkcomponentlabelimpl;
		};

		return BUNDLE_OF_PREFERENCES;
	}

	/** Edge list manipulator */
	public void addInComingEdge( Arrow ar)
	{
		inComingEdgeList.add( ar);
	}

	/** Edge list manipulator */
	public void addOutBoundEdge( Arrow ar)
	{
		outBoundEdgeList.add( ar);
	}

	/** Edge list manipulator */
	public void removeInComingEdge( Arrow ar)
	{
		inComingEdgeList.remove( ar);
	}

	/** Edge list manipulator */
	public void removeOutBoundEdge( Arrow ar)
	{
		outBoundEdgeList.remove( ar);
	}

	/** Edge list manipulator */
	public void removeAllInEdges()
	{
		inComingEdgeList.clear();
	}

	/** Edge list manipulator */
	public void removeAllOutEdges()
	{
		outBoundEdgeList.clear();
	}

	/** Returns ArrayList of Arrow objects.	Edge list manipulator */
	public java.util.List getAllInComingEdges()
	{
		return (ArrayList)inComingEdgeList.clone();
	}

	/** Returns ArrayList of Arrow objects.	Edge list manipulator */
	public java.util.List getAllOutBoundEdges()
	{
		return (ArrayList)outBoundEdgeList.clone();
	}

	/**
		@author Keith Cascio
		@since 080602
	*/
	public void updateArrows( boolean xlate )
	{
		updateArrows( inComingEdgeList, xlate );
		if( !xlate ) updateArrows( outBoundEdgeList, xlate );
	}

	/**
		@author Keith Cascio
		@since 080602
	*/
	protected void updateArrows( Collection arrows, boolean xlate )
	{
		Arrow arr = null;
		for( Iterator it = arrows.iterator(); it.hasNext(); )
		{
			arr = ((Arrow) it.next());
			if( xlate ) arr.updateActualLocation();
			else arr.updateArrow();
		}
	}

	//public void setSize( Dimension d )
	//{
	//	super.setSize( d );
	//	(new Throwable()).printStackTrace();
	//}

	protected CoordinateTransformer myCoordinateTransformer = null;

	protected abstract void  setVirtualLocationHook( Point p );
	protected abstract Point getVirtualLocationHook( Point p );

	public void		setVirtualLocation( Point p )
	{
		setVirtualLocationHook( p );
		theUtilPoint.setLocation( p );
		myCoordinateTransformer.virtualToActual( theUtilPoint );
		setActualLocation( theUtilPoint );
	}

	public Point		getVirtualLocation( Point p )
	{
		if( p == null ) p = new Point();
		getVirtualLocationHook( p );
		return p;
	}

	public void		setActualLocation( Point p )
	{
		super.setLocation( p );
		updateArrows( false );
	}

	public Point		getActualLocation( Point p )
	{
		return super.getLocation( p );
	}

	public void		confirmActualLocation()
	{
		//System.out.println( "(NetworkComponentLabelImpl)"+getText()+".confirmActualLocation()" );
		getActualLocation( theUtilPoint );
		//System.out.println( "\tactual loc:"+theUtilPoint );
		myCoordinateTransformer.actualToVirtual( theUtilPoint );
		//System.out.println( "\tvirtual loc:"+theUtilPoint );
		setVirtualLocationHook( theUtilPoint );
	}

	public void		translateActualLocation( int deltaX, int deltaY )
	{
		getActualLocation( theUtilPoint );
		theUtilPoint.x += deltaX;
		theUtilPoint.y += deltaY;
		super.setLocation( theUtilPoint );
		updateArrows( true );
	}

	protected abstract void		setVirtualSizeHook( Dimension d );
	protected abstract Dimension	getVirtualSizeHook( Dimension d );

	public void		setVirtualSize( Dimension d )
	{
		setVirtualSizeHook( d );
		theUtilDimension.setSize( d );
		myCoordinateTransformer.virtualToActual( theUtilDimension );
		setActualSize( theUtilDimension );
	}
	public Dimension	getVirtualSize( Dimension d )
	{
		if( d == null ) d = new Dimension();
		getVirtualSizeHook( d );
		return d;
	}

	public void		setActualSize( Dimension d )
	{
		getNodeIcon().changeSize( d );
		setSize( d );
		updateArrows( false );
	}

	public Dimension	getActualSize( Dimension d )
	{
		return getNodeIcon().getSize( d );
	}

	public void		confirmActualSize()
	{
		getActualSize( theUtilDimension );
		myCoordinateTransformer.actualToVirtual( theUtilDimension );
		setVirtualSizeHook( theUtilDimension );
	}

	public void		setActualScale( double factor )
	{
		setFontSize( ((float)factor)*FLOAT_BASE_FONT_SIZE );

		getVirtualSizeHook( theUtilDimension );
		theUtilDimension.width *= factor;
		theUtilDimension.height *= factor;
		setActualSize( theUtilDimension );

		//getVirtualLocationHook( theUtilPoint );
		//theUtilPoint.x *= factor;
		//theUtilPoint.y *= factor;
		//setActualLocation( theUtilPoint );
	}

	/** @since 081104 */
	public void		hintScale( float scale ){
		setFontSize( scale*FLOAT_BASE_FONT_SIZE );
	}

	/** @since 081104 */
	public void		setFontSize( float size ){
		//if( flagVerboseSetFontSize ) System.out.println( "(NetworkComponentLabelImpl)"+getText()+".setFontSize( "+size+" )" );
		setFont( getFont().deriveFont( size ) );
	}

	//private static boolean FLAG_VERBOSE_SETFONTSIZE = true;
	//private boolean flagVerboseSetFontSize = false;

	public void		recalculateActual()
	{
		//System.out.println( "(NetworkComponentLabelImpl)"+getText()+".recalculateActual()" );

		recalculateActualSize();

		setFontSize( myCoordinateTransformer.virtualToActual( FLOAT_BASE_FONT_SIZE ) );

		getVirtualLocationHook( theUtilPoint );
		//System.out.println( "\tvirtual loc:"+theUtilPoint );
		myCoordinateTransformer.virtualToActual( theUtilPoint );
		//System.out.println( "\tactual  loc:"+theUtilPoint );
		setActualLocation( theUtilPoint );
	}

	/** @since 081304 */
	public void recalculateActualSize()
	{
		getVirtualSizeHook( theUtilDimension );
		//System.out.println( "\tvirtual size:"+theUtilDimension );
		myCoordinateTransformer.virtualToActual( theUtilDimension );
		//System.out.println( "\tactual size:"+theUtilDimension );
		setActualSize( theUtilDimension );
	}

	/** Will modify pt and then return it, so no new allocations take place.*/
	public Point getActualCenter( Point pt )
	{
		if( pt == null ) pt = new Point();
		getActualLocation( pt );
		pt.translate( getWidth()/2, getHeight()/2 );
		return pt;
	}

	/** interface ColorVirtual
		@since 080504 */
	public void		setVirtualColor( Color c ){
		myVirtualColor = c;
	}
	public Color	getVirtualColor(){
		return myVirtualColor;
	}
	public void		setActualColor( Color c ){
		getNodeIcon().changeBackgroundColor( c );
	}
	public Color	getActualColor(){
		return getNodeIcon().getBackground();
	}
	public void		confirmActualColor(){
	}
	/** @since 080904 */
	public void		recalculateActualColor(){
		setActualColor( getVirtualColor() );
	}
	public void		paintImmediately()
	{
		NodeIcon icon = getNodeIcon();
		paintImmediately( 0,0,icon.getIconWidth(),icon.getIconHeight() );
	}

	private Color myVirtualColor;

	protected static Point theUtilPoint = new Point();
	//protected Point theUtilPoint = new Point();
	protected static Dimension theUtilDimension = new Dimension();
}
