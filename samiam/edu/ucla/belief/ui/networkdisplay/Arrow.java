package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.util.HiddenProperty;

import java.awt.*;
import edu.ucla.belief.*;

/** This class will store and draw a directed arrow from start to end. */
public class Arrow implements CoordinateVirtual, PreferenceListener
{
	protected Point                   startLoc = new Point(), endLoc = new Point();
	private   double                  theta;
	private   boolean                 myFlagVisible = true, myFlagRecoverable = false, myFlagDrawPolygon = true;
	protected Dimension               myVirtualSize, myActualSize = new Dimension();
	protected CoordinateTransformer   myCoordinateTransformer;
	private   Polygon                 myPolygon = new Polygon( new int[3], new int[3], 3 );
	private   Color                   myPreferenceDrawColor, myDrawColor;
	protected float                   myFloatVirtualStrokeWidth;
	protected BasicStroke             myEdgeStroke;
	private   NetworkComponentLabel[] nodes;//0=start, 1=end

	public static final BasicStroke
	  STROKE_RECOVERABLE = new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0f, new float[]{ 4f, 4f }, 0 );

	/** @since 20080221 */
	public Arrow setRecoverable( boolean flag ){
		myFlagRecoverable = flag;
		return this;
	}

	/** @since 20080221 */
	public Arrow die(){
		try{
			if( nodes != null ){
				if( nodes.length > 0 ){ nodes[0].removeOutBoundEdge( this ); }
				if( nodes.length > 1 ){ nodes[1].removeInComingEdge( this ); }
			}

			endLoc                  = startLoc = null;
			myVirtualSize           = myActualSize = null;
			myCoordinateTransformer = null;
			myPolygon               = null;
			myPreferenceDrawColor   = myDrawColor = null;
			myEdgeStroke            = null;
			nodes                   = null;
		}catch( Throwable thrown ){
			System.err.println( "warning: Arrow.die() caught " + thrown );
		}
		return this;
	}

	public Arrow( NetworkComponentLabel st,
	              NetworkComponentLabel en,
	              CoordinateTransformer xformer,
	              SamiamPreferences     netPrefs )
	{
		if( st == null || en == null ) System.err.println( "Java warning Arrow(null)" );
		nodes                   = new NetworkComponentLabel[]{ st, en };
		myCoordinateTransformer = xformer;
		Arrow.validatePreferenceBundle( netPrefs );
		setPreferences();
		updateArrow();
	}

	public boolean isEqual( NetworkComponentLabel st, NetworkComponentLabel en)
	{
		return isEqual( st.getFiniteVariable(), en.getFiniteVariable());
	}

	public boolean isEqual( FiniteVariable st, FiniteVariable en ){
		if( st == null || en == null ) return false;
		return (nodes[0].getFiniteVariable() == st) && (nodes[1].getFiniteVariable() == en);
	}

	/** Change the start node of the arrow.*/
	public void setStart( NetworkComponentLabel st)
	{
		nodes[0] = st;
		updateArrow();
	}

	public NetworkComponentLabel getStart()
	{
		return nodes[0];
	}

	public NetworkComponentLabel getEnd()
	{
		return nodes[1];
	}

	/** @since 20060731 */
	public Point getOrigin( Point point ){
		point.setLocation( Arrow.this.startLoc );
		return point;
	}

	/** @since 20060731 */
	public Point getDestination( Point point ){
		point.setLocation( Arrow.this.endLoc );
		return point;
	}

	/** @since 20060731 */
	public boolean isVisible(){
		return Arrow.this.myFlagVisible;
	}

	/** @since 20060731 */
	public boolean setVisible( boolean flag ){
		boolean old = Arrow.this.myFlagVisible;
		Arrow.this.myFlagVisible = flag;
		return old;
	}

	/** Change the end node of the arrow.*/
	public void setEnd( NetworkComponentLabel en)
	{
		nodes[1] = en;
		updateArrow();
	}

	// Recalculate theta and create new myPolygon.
	/** Needs to be called whenever the nodeLabels change (size, moved, ...)*/
	public void updateArrow()
	{
		//(new Throwable()).printStackTrace();

		//update locations
		nodes[0].getActualCenter( startLoc );
		nodes[1].getActualCenter( endLoc );

		if( myFlagDrawPolygon )
		{
			theUtilPoint.setLocation( startLoc );
			nodes[0].getNodeIcon().modifyStartPoint( startLoc, endLoc );
			nodes[1].getNodeIcon().modifyEndPoint( theUtilPoint, endLoc );
			//nodes[1].getNodeIcon().modifyEndPoint( startLoc, endLoc );

			//update theta
			theta = Math.atan2( startLoc.y - endLoc.y, startLoc.x - endLoc.x);

			//update myPolygon
			Point tempPt = new Point();
			//myPolygon = new Polygon();

			tempPt.setLocation( endLoc );
			//myPolygon.addPoint( tempPt.x, tempPt.y );
			myPolygon.xpoints[0] = tempPt.x;
			myPolygon.ypoints[0] = tempPt.y;

			tempPt.setLocation( endLoc.x + myActualSize.width, endLoc.y + myActualSize.height);
			//tempPt.x += newSize.width;
			//tempPt.y += newSize.height;
			rotate( endLoc.x, endLoc.y, tempPt);
			//myPolygon.addPoint( tempPt.x, tempPt.y );
			myPolygon.xpoints[1] = tempPt.x;
			myPolygon.ypoints[1] = tempPt.y;

			tempPt.setLocation( endLoc.x + myActualSize.width, endLoc.y - myActualSize.height);
			//tempPt.y = endLoc.y - newSize.height;
			rotate( endLoc.x, endLoc.y, tempPt);
			//myPolygon.addPoint( tempPt.x, tempPt.y );
			myPolygon.xpoints[2] = tempPt.x;
			myPolygon.ypoints[2] = tempPt.y;
		}
	}

	protected static Point theUtilPoint = new Point();

	/**
		@author Keith Cascio
		@since 101702
	*/
	public void updateActualLocation()
	{
		theUtilPoint2.setLocation( endLoc );

		//update locations
		nodes[0].getActualCenter( startLoc );
		nodes[1].getActualCenter( endLoc );

		if( myFlagDrawPolygon )
		{
			theUtilPoint.setLocation( startLoc );
			nodes[0].getNodeIcon().modifyStartPoint( startLoc, endLoc );
			nodes[1].getNodeIcon().modifyEndPoint( theUtilPoint, endLoc );

			myPolygon.translate( endLoc.x - theUtilPoint2.x, endLoc.y - theUtilPoint2.y );
		}
	}

	protected static Point theUtilPoint2 = new Point();

	public void		setVirtualLocation( Point p )
	{
		throw new UnsupportedOperationException();
	}
	public Point		getVirtualLocation( Point p )
	{
		throw new UnsupportedOperationException();
	}

	public void		setActualLocation( Point p )
	{
		throw new UnsupportedOperationException();
	}
	public Point		getActualLocation( Point p )
	{
		if( p == null ) p = new Point();
		p.setLocation( endLoc );
		return p;
	}
	public void		confirmActualLocation()
	{
		throw new UnsupportedOperationException();
	}
	public void		translateActualLocation( int deltaX, int deltaY )
	{
		throw new UnsupportedOperationException();
	}

	public void		setVirtualSize( Dimension d )
	{
		myVirtualSize.setSize( d );
		theUtilDimension.setSize( d );
		myCoordinateTransformer.virtualToActual( theUtilDimension );
		setActualSize( theUtilDimension );
	}
	public Dimension	getVirtualSize( Dimension d )
	{
		if( d == null ) d = new Dimension();
		d.setSize( myVirtualSize );
		return d;
	}

	/** @since 081104 */
	public void		hintScale( float scale ){}

	public void		setActualSize( Dimension d )
	{
		myActualSize.setSize( d );
		if( d.height > 1 )
		{
			//if( !myFlagDrawPolygon )
			//{
			//	myFlagDrawPolygon = true;
			//	updateArrow();
			//}
			myFlagDrawPolygon = true;
			updateArrow();
		}
		else
		{
			myFlagDrawPolygon = false;
			//System.out.println( "myFlagDrawPolygon == false" );
		}
	}
	public Dimension	getActualSize( Dimension d )
	{
		if( d == null ) d = new Dimension();
		d.setSize( myActualSize );
		return d;
	}
	public void		confirmActualSize()
	{
		getActualSize( theUtilDimension );
		myCoordinateTransformer.actualToVirtual( theUtilDimension );
		setVirtualSize( theUtilDimension );
	}

	public Point		getActualCenter( Point pt )
	{
		return getActualLocation( pt );
	}

	public void		setActualScale( double factor )
	{
		getVirtualSize( theUtilDimension );
		theUtilDimension.width *= factor;
		theUtilDimension.height *= factor;
		setActualSize( theUtilDimension );

		//getVirtualLocation( theUtilPoint );
		//theUtilPoint.x *= factor;
		//theUtilPoint.y *= factor;
		//setActualLocation( theUtilPoint );
	}

	public void		recalculateActual()
	{
		//System.out.println( "old actual: " + myActualSize );
		getVirtualSize( theUtilDimension );
		myCoordinateTransformer.virtualToActual( theUtilDimension );
		setActualSize( theUtilDimension );
		//System.out.println( "new actual: " + myActualSize );

		float newFloatWidth = myCoordinateTransformer.virtualToActual( myFloatVirtualStrokeWidth );
		if( newFloatWidth != LAST_STROKE_WIDTH )
		{
			LAST_STROKE_WIDTH = newFloatWidth;
			LAST_STROKE = new BasicStroke( LAST_STROKE_WIDTH );
		}
		myEdgeStroke = LAST_STROKE;
		//myEdgeStroke = new BasicStroke( myCoordinateTransformer.virtualToActual( myFloatVirtualStrokeWidth ) );

		//getVirtualLocationHook( theUtilPoint );
		//myCoordinateTransformer.virtualToActual( theUtilPoint );
		//setActualLocation( theUtilPoint );
	}

	protected static float LAST_STROKE_WIDTH = (float)-1;
	protected static BasicStroke LAST_STROKE;

	protected static Dimension theUtilDimension = new Dimension();

	/** Draws the arrow using the graphics object.*/
	public void paint( Graphics g, Rectangle viewRect, boolean hideHidden )
	{
		if( ! Arrow.this.myFlagVisible ){ return; }

		if( hideHidden && (HiddenProperty.isHidden( nodes[0].getFiniteVariable() ) || HiddenProperty.isHidden( nodes[1].getFiniteVariable() )) ){ return; }

		//System.out.println( "Arrow.paint()" );
		g.setColor( getEffectiveDrawColor() );

		if( myFlagDrawPolygon && viewRect.contains( endLoc ) ){
			if( myFlagRecoverable ){ g.drawPolygon( myPolygon ); }
			else{                    g.fillPolygon( myPolygon ); }
		}

		//if( !viewRect.intersectsLine( startLoc.getX(), startLoc.getY(), endLoc.getX(), endLoc.getY() ) ) return;

		Graphics2D g2 = (Graphics2D)g;

		Stroke oldStroke = g2.getStroke();
		g2.setStroke( myFlagRecoverable ? STROKE_RECOVERABLE : myEdgeStroke );
		g2.drawLine( startLoc.x, startLoc.y, endLoc.x, endLoc.y );
		g2.setStroke( oldStroke );
	}

	/** Rotate Point pt around point (xr, yr) using theta.*/
	private void rotate( int xr, int yr, Point pt)
	{
		int newx = (int)(xr + (pt.x - xr) * Math.cos(theta) - (pt.y - yr) * Math.sin(theta));
		int newy = (int)(yr + (pt.x - xr) * Math.sin(theta) + (pt.y - yr) * Math.cos(theta));
		pt.x = newx;
		pt.y = newy;
	}

	/** Allow options to change.*/
	//public void changeArrowTipSize( Dimension sz)
	//{
	//	myVirtualSize.setSize( sz);
	//	updateArrow( myVirtualSize );
	//}

	/**
		@author Keith Cascio
		@since 101702
	*/
	//public void scale( double factor )
	//{
	//	theTempDimension.setSize( myVirtualSize );
	//	NetworkDisplay.scale( theTempDimension, factor );
	//	updateArrow( theTempDimension );
	//}

	protected static Dimension theTempDimension = new Dimension();

	/** Allow options to change.*/
	public void changeArrowColor( Color clr ){
		myPreferenceDrawColor = clr;
	}

	/** @since 061304 */
	public void setDrawColor( Color clr ){
		myDrawColor = clr;
	}

	/** @since 061304 */
	public Color getEffectiveDrawColor(){
		if( myDrawColor != null ) return myDrawColor;
		else return myPreferenceDrawColor;
	}

	/** @since 20030219 */
	public void changeEdgeWidth( float newStroke )
	{
		myFloatVirtualStrokeWidth = newStroke;
		recalculateActual();
	}

	/** Allow options to change.*/
	public void changePackageOptions()
	{
		updatePreferences();
	}

	private static       TargetedBundle BUNDLE_OF_PREFERENCES;
	public  static final String         STR_KEY_PREFERENCE_BUNDLE = Arrow.class.getName();

	public static TargetedBundle validatePreferenceBundle( SamiamPreferences prefs ){
		if( BUNDLE_OF_PREFERENCES != null ) return BUNDLE_OF_PREFERENCES.validate( prefs );

		String   key  = STR_KEY_PREFERENCE_BUNDLE;
		String[] keys = new String[] { SamiamPreferences.edgeArrowTipSize, SamiamPreferences.edgeClr, SamiamPreferences.arrowStroke };

		BUNDLE_OF_PREFERENCES = new TargetedBundle( key, keys, prefs ){
			public void begin( Object me, boolean force ){
				this.arrow = (Arrow) me;
			}

			public void setPreference( int index, Object value ){
				switch( index ){
					case 0:
						arrow.setVirtualSize( arrow.myVirtualSize = (Dimension) value );
						break;
					case 1:
						arrow.changeArrowColor( (Color) value );
						break;
					case 2:
						arrow.changeEdgeWidth( ((Number)value).floatValue() );
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			private Arrow arrow;
		};

		return BUNDLE_OF_PREFERENCES;
	}

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the COMMITTED values of
		the Preferences in PreferenceGroup prefs.
	*/
	public void updatePreferences()
	{
		BUNDLE_OF_PREFERENCES.updatePreferences( Arrow.this );
	}

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the EDITED values of
		the Preferences in PreferenceGroup prefs.
	*/
	public void previewPreferences()
	{
		BUNDLE_OF_PREFERENCES.previewPreferences( Arrow.this );
	}

	/**
		Call this method to force a PreferenceListener to
		reset itself according to the Preferences in
		PreferenceGroup prefs.
	*/
	public void setPreferences()
	{
		BUNDLE_OF_PREFERENCES.setPreferences( Arrow.this );
	}
}
