package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.*;
import edu.ucla.belief.approx.ApproxEngine;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.preference.*;

import java.text.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This class maintains the dialog windows that show up in the NetworkDisplay
 * window to show the state values of nodes.
 * (They will be called Monitors to the users instead of EvidenceDialogs)
 */
public class EvidenceDialog extends JInternalFrame implements Monitor//, ComponentListener
{
	protected DisplayableFiniteVariable dspVar;
	protected CoordinateTransformer myCoordinateTransformer;
	//private SamiamPreferences monitorPrefs;

	//Ordered the same as the instance function in the FiniteVariable class
	protected ArrayList evidLabels;

	//Very limited usage by preferences
	protected boolean sampleMode;
	protected boolean myFlagDoZoom = false;

	/** interface Monitor
		@since  20040614 */
	public int rotate(){ return 0; }

	/** interface Monitor
		@since  20040614 */
	public void setApprox( ApproxEngine ae ){}

	/** interface Monitor
		@since  20030310 */
	public JComponent asJComponent(){ return this; }

	/** interface Monitor
		@since  20030310 */
	public void notifyBoundsChange(){
		NetworkInternalFrame nif = dspVar.getNetworkInternalFrame();
		if( nif != null )
		{
			NetworkDisplay display = nif.getNetworkDisplay();
			if( display.testExceedsBounds( dspVar.getNodeLabel() ) ) display.recalculateDesktopSize();
		}
	}

	public EvidenceDialog(	SamiamPreferences monitorPrefs,
				DisplayableFiniteVariable dspV,
				CoordinateTransformer xformer )
	{
		//do not allow closing in sample mode
		super( dspV.toString(), true, !dspV.isSampleMode(), false, false);

		//System.out.print( "EvidenceDialog()..." );

		setDefaultCloseOperation( HIDE_ON_CLOSE);

		sampleMode = dspV.isSampleMode();
		dspVar = dspV;
		if( dspVar != null && !dspVar.isSampleMode() ) dspVar.getNetworkInternalFrame().addNodePropertyChangeListener( this );
		this.myCoordinateTransformer = xformer;
		//this.monitorPrefs            = monitorPrefs;

		EvidenceDialog.validatePreferenceBundle( monitorPrefs );

		if( sampleMode) {
			evidLabels = new ArrayList( 2);

			JPanel pn = new JPanel();
			pn.setLayout( new BoxLayout( pn, BoxLayout.Y_AXIS));
			JScrollPane sp = new JScrollPane( pn);
			setContentPane( sp);

			EvidenceLabel el = new EvidenceLabel( monitorPrefs, dspVar, 0, null, -1 );
			evidLabels.add( el);
			pn.add( el.asComponent() );

			el = new EvidenceLabel( monitorPrefs, dspVar, 1, null, -1 );
			evidLabels.add( el);
			pn.add( el.asComponent() );
		}
		else {
			InferenceEngine ie = dspVar.getInferenceEngine();
			Table conditional = null;
			if( ie != null ) conditional = ie.conditional( dspVar );
			int observedIndex = dspVar.getObservedIndex();

			evidLabels = new ArrayList( dspVar.size() );

			JPanel pn = new JPanel();
			pn.setLayout( new BoxLayout( pn, BoxLayout.Y_AXIS));
			JScrollPane sp = new JScrollPane( pn);
			setContentPane( sp);

			for( int i = 0; i < dspVar.size(); i++) {
				EvidenceLabel el = new EvidenceLabel( monitorPrefs, dspVar, i, conditional, observedIndex );
				evidLabels.add( el );
				pn.add( el.asComponent() );
			}
		}

		setVisible( false);

		//addComponentListener( this );
		//getContentPane().addMouseListener( new MouseMouse() );

		setPreferences();
		recalculateActual();
	}

	/*
	public class MouseMouse extends MouseAdapter
	{
		public void mouseReleased(MouseEvent e)
		{
			//System.out.println( "mouseReleased" );
			//confirmActualLocation();
		}
	}*/

	/**
		@author Keith Cascio
		@since 030703
	*/
	public void pack()
	{
		super.pack();

		Rectangle bounds = getBounds();

		if( myCoordinateTransformer.virtualToActual( (double)1 ) <= (double)1.5 )
		{
			bounds.width = (int)( (double)bounds.width * (double)1.1 );//add extra border space to dialog
			bounds.height = (int)( bounds.height + 8 );
		}

		setBounds( bounds );
		//myVirtualSize.setSize( bounds.getSize() );
		setActualSize( bounds.getSize() );
		confirmActualSize();
		//myVirtualLocation.setLocation( bounds.getLocation() );
		setActualLocation( bounds.getLocation() );

		//System.out.println( bounds.getSize() + " " + bounds.getLocation() );
	}

	/**
		For interface ComponentListener
		@author Keith Cascio
		@since 030703
	*/
	/*
	public void componentMoved(ComponentEvent e)
	{
		//confirmActualLocation();
	}
	public void componentResized(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentHidden(ComponentEvent e) {}
	*/

	/**
		interface EvidenceChangeListener
		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ece ) {}

	public void evidenceChanged( EvidenceChangeEvent ECE )
	{
		evidenceChanged( ECE, Double.NaN );
	}

	public void evidenceChanged( EvidenceChangeEvent ece, double globalMaximumProbability )
	{
		if( NetworkInternalFrame.DEBUG_CPTCHANGED ) Util.STREAM_DEBUG.println( "EvidenceDialog.evidenceChanged() for " + dspVar );
		for( Iterator itr = evidLabels.iterator(); itr.hasNext(); ) {
			EvidenceLabel el = (EvidenceLabel)itr.next();
			el.drawEvidence();
		}
		repaint();
	}

	/** Allow options to change.*/
	/*
	public void changePackageOptions( boolean bShowLabel, Color m, Color a, int w, int h, Color t) {
		dspVar.changeDisplayText( bShowLabel);
		setTitle( dspVar.toString());
		for( Iterator itr = evidLabels.iterator(); itr.hasNext(); ) {
			EvidenceLabel el = (EvidenceLabel)itr.next();
			el.changePackageOptions( m, a, w, h, t);
		}
	}*/

	/**
		interface NodePropertyChangeListener
		@author Keith Cascio
		@since 070703
	*/
	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		if( e.variable == dspVar ) setTitle( dspVar.toString() );
	}

	/** Allow options to change.*/
	public void changePackageOptions()
	{
		updatePreferences();
	}

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the COMMITTED values of
		the Preferences in PreferenceGroup prefs.
	*/
	public void updatePreferences()
	{
		setTitle( dspVar.toString() );
		for( Iterator itr = evidLabels.iterator(); itr.hasNext(); ) {
			((EvidenceLabel)itr.next()).updatePreferences();
		}

		BUNDLE_OF_PREFERENCES.updatePreferences( EvidenceDialog.this );
	}
	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the EDITED values of
		the Preferences in PreferenceGroup prefs.
	*/
	public void previewPreferences()
	{
		for( Iterator itr = evidLabels.iterator(); itr.hasNext(); ){
			((EvidenceLabel)itr.next()).previewPreferences();
		}

		BUNDLE_OF_PREFERENCES.previewPreferences( EvidenceDialog.this );
	}
	/**
		Call this method to force a PreferenceListener to
		reset itself according to the Preferences in
		PreferenceGroup prefs.
	*/
	public void setPreferences()
	{
		for( Iterator itr = evidLabels.iterator(); itr.hasNext(); ) {
			((EvidenceLabel)itr.next()).setPreferences();
		}

		BUNDLE_OF_PREFERENCES.setPreferences( EvidenceDialog.this );
	}

	public void setDoZoom( boolean newFlag )
	{
		if( myFlagDoZoom != newFlag )
		{
			for( Iterator it = evidLabels.iterator(); it.hasNext(); )
			{
				((EvidenceLabel)it.next()).setDoZoom( newFlag );
			}
			myFlagDoZoom = newFlag;
			recalculateActual();
		}
	}

	private static       TargetedBundle BUNDLE_OF_PREFERENCES;
	public  static final String         STR_KEY_PREFERENCE_BUNDLE = EvidenceDialog.class.getName();

	public static TargetedBundle validatePreferenceBundle( SamiamPreferences prefs ){
		if( BUNDLE_OF_PREFERENCES != null ) return BUNDLE_OF_PREFERENCES.validate( prefs );

		String   key  = STR_KEY_PREFERENCE_BUNDLE;
		String[] keys = new String[] { SamiamPreferences.evidDlgZooms, SamiamPreferences.evidDlgRectSize };

		BUNDLE_OF_PREFERENCES = new TargetedBundle( key, keys, prefs ){
			public void begin( Object me, boolean force ){
				this.evidencedialog = (EvidenceDialog) me;
			}

			public void setPreference( int index, Object value ){
				switch( index ){
					case 0:
						evidencedialog.setDoZoom( ((Boolean)value).booleanValue() );
						break;
					case 1:
						evidencedialog.pack();
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			private EvidenceDialog evidencedialog;
		};

		return BUNDLE_OF_PREFERENCES;
	}

	protected Point myVirtualLocation = new Point();

	public void		setVirtualLocation( Point p )
	{
		myVirtualLocation.setLocation( p );
		theUtilPoint.setLocation( p );
		myCoordinateTransformer.virtualToActual( theUtilPoint );
		setActualLocation( theUtilPoint );
	}

	public Point		getVirtualLocation( Point p )
	{
		if( p == null ) p = new Point();
		p.setLocation( myVirtualLocation );
		return p;
	}

	public void		setActualLocation( Point p )
	{
		super.setLocation( p );
	}

	public Point		getActualLocation( Point p )
	{
		return super.getLocation( p );
	}

	public void		confirmActualLocation()
	{
		//System.out.println( "(EvidenceDialog)"+getText()+".confirmActualLocation()" );
		getActualLocation( theUtilPoint );
		//System.out.println( "\tactual loc:"+theUtilPoint );
		myCoordinateTransformer.actualToVirtual( theUtilPoint );
		//System.out.println( "\tvirtual loc:"+theUtilPoint );
		myVirtualLocation.setLocation( theUtilPoint );
	}

	public void		translateActualLocation( int deltaX, int deltaY )
	{
		getActualLocation( theUtilPoint );
		theUtilPoint.x += deltaX;
		theUtilPoint.y += deltaY;
		super.setLocation( theUtilPoint );
	}

	protected Dimension myVirtualSize = new Dimension();

	public void		setVirtualSize( Dimension d )
	{
		myVirtualSize.setSize( d );
		theUtilDimension.setSize( d );
		if( myFlagDoZoom ) myCoordinateTransformer.virtualToActual( theUtilDimension );
		setActualSize( theUtilDimension );
	}
	public Dimension	getVirtualSize( Dimension d )
	{
		if( d == null ) d = new Dimension();
		d.setSize( myVirtualSize );
		return d;
	}

	public void		setActualSize( Dimension d )
	{
		setSize( d );
	}

	/** @since 081104 */
	public void		hintScale( float scale ){}

	public Dimension	getActualSize( Dimension d )
	{
		return getSize( d );
	}

	public void		confirmActualSize()
	{
		getActualSize( theUtilDimension );
		if( myFlagDoZoom ) myCoordinateTransformer.actualToVirtual( theUtilDimension );
		myVirtualSize.setSize( theUtilDimension );
	}

	/**
		@deprecated
	*/
	public void		setActualScale( double factor )
	{
		//theUtilDimension.setSize( myVirtualSize );
		//theUtilDimension.width *= factor;
		//theUtilDimension.height *= factor;
		//setActualSize( theUtilDimension );

		//theUtilPoint.setLocation( myVirtualLocation );
		//theUtilPoint.x *= factor;
		//theUtilPoint.y *= factor;
		//setActualLocation( theUtilPoint );
	}

	public void		recalculateActual()
	{
		//System.out.println( "(EvidenceDialog)"+getTitle()+".recalculateActual()...myFlagDoZoom=" + myFlagDoZoom );

		if( myFlagDoZoom )
		{
			for( Iterator it = evidLabels.iterator(); it.hasNext(); )
			{
				((EvidenceLabel)it.next()).recalculateActual( myCoordinateTransformer );
			}
		}

		theUtilDimension.setSize( myVirtualSize );
		//System.out.println( "\tvirtual size:"+theUtilDimension );
		if( myFlagDoZoom ) myCoordinateTransformer.virtualToActual( theUtilDimension );
		//System.out.println( "\tactual size:"+theUtilDimension );
		setActualSize( theUtilDimension );

		theUtilPoint.setLocation( myVirtualLocation );
		//System.out.println( "\tvirtual loc:"+theUtilPoint );
		myCoordinateTransformer.virtualToActual( theUtilPoint );
		//System.out.println( "\tactual  loc:"+theUtilPoint );
		setActualLocation( theUtilPoint );
	}

	/** Will modify pt and then return it, so no new allocations take place.*/
	public Point getActualCenter( Point pt )
	{
		if( pt == null ) pt = new Point();
		getActualLocation( pt );
		pt.translate( getWidth()/2, getHeight()/2 );
		return pt;
	}

	protected static Point theUtilPoint = new Point();
	//protected Point theUtilPoint = new Point();
	protected static Dimension theUtilDimension = new Dimension();
}
