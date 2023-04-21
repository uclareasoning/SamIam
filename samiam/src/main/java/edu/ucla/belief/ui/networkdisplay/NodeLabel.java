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
import edu.ucla.util.FlagProperty;

/**
* This class draws the belief nodes on the screen with their labels.
*/
public class NodeLabel extends NetworkComponentLabelImpl implements NodePropertyChangeListener
{
	private static boolean
	  FLAG_LIKELIEST_BREAKS_LINE = false,
	  FLAG_DISPLAY_LIKELIEST     = true;
	private static String
	  STRING_HTMLColorObserved   = "FF0000",
	  STRING_HTMLColorLikeliest  = "006600",
	  STRING_HTMLColorFlip       = "FFFFFF";

	private   Object                    cptmonitorsynch = new Object();
	private   Component                 cptmonitor;
	protected Monitor                   myMonitor;
	protected DisplayableFiniteVariable myDVar;
	protected JComponent                myParent;//protected JDesktopPane myParent = null;
	protected SamiamPreferences         myMonitorPrefs;
	protected Object                    myLastLikeliest;
	private   boolean                   myFlagDisableActions = false;
	private   boolean                   flagTextInvalid      = true;

	public NodeLabel(	DisplayableFiniteVariable dv,
				NodeIcon image,
				JComponent desktop,//JDesktopPane desktop,
				CoordinateTransformer xformer,
				SelectionListener list,
				SamiamPreferences monitorPrefs,
				boolean sample,
				boolean diligent )
	{
		super( dv.toString(), image, xformer, list, monitorPrefs, dv.getLocation( new Point() ), sample );

		this.myDVar         = dv;
		this.myParent       = desktop;

		this.myMonitorPrefs = monitorPrefs;
		this.myDVar.getDimension( myVirtualSize );

		if( diligent ) initLazy();
		//else NodeLabel.this.setShapePreference();
		else BUNDLE_OF_PREFERENCES.setPreference( NodeLabel.this, 3 );
	}

	/** @since 20040813 */
	public NodeLabel(	DisplayableFiniteVariable dv,
				JComponent desktop,//JDesktopPane desktop,
				CoordinateTransformer xformer,
				SelectionListener list,
				SamiamPreferences monitorPrefs,
				boolean sample,
				boolean diligent )
	{
		this( dv, (NodeIcon)null, desktop, xformer, list, monitorPrefs, sample, diligent );
		//super( dv.toString(), (NodeIcon)null, xformer, list, netPrefs, dv.getLocation( new Point() ), sample );
	}

	/** @since 20080302 */
	public Component setCPTMonitor( Component cptmonitor ){
		checkLock();
		synchronized( cptmonitorsynch ){
			return this.cptmonitor = cptmonitor;
		}
	}

	/** @since 20080302 */
	public Component getCPTMonitor(){
		checkLock();
		synchronized( cptmonitorsynch ){
			return this.cptmonitor;
		}
	}

	/** @since 20080302 */
	private boolean checkLock(){
		if( ! Thread.currentThread().holdsLock( this.cptmonitorsynch ) ){
			throw new IllegalStateException( "must hold lock on Object returned by NodeLabel.getCPTMonitorSynch() before calling NodeLabel.set/getCPTMonitor()" );
		}
		return true;
	}

	/** @since 20080302 */
	public Object   getCPTMonitorSynch(){
		return cptmonitorsynch;
	}

	/** @since 20060522 */
	public void initLazy(){
		super.initLazy();
		if( myDVar != null && !myDVar.isSampleMode() ) myDVar.getNetworkInternalFrame().addNodePropertyChangeListener( this );
		//setPreferences();
		NodeLabel.this.handleHidden();
		NodeLabel.this.updateNodeIconObserved();
	}

	/** @since 20050607 */
	public void updateUI(){
		super.updateUI();
		if( this.myDVar != null ) this.myDVar.updateUI();
	}

	/** interface NodePropertyChangeListener
		@since 20040817 */
	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		if( e.variable == myDVar ){
			if( e.property == HiddenProperty.PROPERTY ) handleHidden();
			setDVarText();
		}
	}

	/** @since 20040817 */
	public void handleHidden(){
		super.handleHidden();
		setDVarText();
	}

	/** @since 20040813 */
	protected void setShapePreference( IconFactory factory ){
		if( getFiniteVariable() != null ) super.setShapePreference( factory );
	}

	/** @since 20040804 */
	public static void updatePreferencesStatic( SamiamPreferences prefs ){
		validateTargetedBundle( prefs ).setPreferences( null );
	}

	/** @since 20040804 */
	public static boolean isPreferenceStaticRecentlyCommitted( SamiamPreferences prefs ){
		return validateTargetedBundle( prefs ).isRecentlyCommittedValue();
	}

	private static       TargetedBundle BUNDLE_OF_PREFERENCES_NODELABEL;
	public  static final String         STR_KEY_PREFERENCE_BUNDLE = NodeLabel.class.getName();

	private static TargetedBundle validateTargetedBundle( SamiamPreferences prefs ){
		if( BUNDLE_OF_PREFERENCES_NODELABEL != null ) return BUNDLE_OF_PREFERENCES_NODELABEL.validate( prefs );

		String   key  = STR_KEY_PREFERENCE_BUNDLE;
		String[] keys = new String[] { SamiamPreferences.nodesDisplayLikeliestValue, SamiamPreferences.nodeBorderClrObserved, SamiamPreferences.nodeTextLikeliestValueClr, SamiamPreferences.nodeTextFlippedValueClr, SamiamPreferences.nodeLikeliestBreakLine, SamiamPreferences.displayNodeLabelIfAvail };

		BUNDLE_OF_PREFERENCES_NODELABEL = new TargetedBundle( key, keys, prefs ){
			public void setPreference( int index, Object value ){
				switch( index ){
					case 0:
						FLAG_DISPLAY_LIKELIEST = ((Boolean)value).booleanValue();
						break;
					case 1:
						STRING_HTMLColorObserved = Util.htmlEncode( (Color) value );
						break;
					case 2:
						STRING_HTMLColorLikeliest = Util.htmlEncode( (Color) value );
						break;
					case 3:
						STRING_HTMLColorFlip = Util.htmlEncode( (Color) value );
						break;
					case 4:
						FLAG_LIKELIEST_BREAKS_LINE = ((Boolean)value).booleanValue();
						break;
					case 5:
						break;
					default:
						throw new IllegalArgumentException();
				}
			}
		};

		return BUNDLE_OF_PREFERENCES_NODELABEL;
	}

	/** @since 20030331 */
	public void removeFromParentManaged()
	{
		if( myMonitor != null ) myParent.remove( myMonitor.asJComponent() );
		myParent.remove( this );
	}

	public String toString()
	{
		return "NodeLabel-" + myDVar.toString();
	}

	protected void setVirtualLocationHook( Point p )
	{
		myDVar.setLocation( p );
	}

	protected Point getVirtualLocationHook( Point p )
	{
		return myDVar.getLocation( p );
	}

	protected void setVirtualSizeHook( Dimension d )
	{
		//System.out.println( "NodeLabel.setVirtualSizeHook("+d+")" );
		//myDVar.setDimension( d );
		myVirtualSize.setSize( d );
	}

	protected Dimension getVirtualSizeHook( Dimension d )
	{
		//return myDVar.getDimension( d );
		if( d == null ) d = new Dimension();
		d.setSize( myVirtualSize );
		return d;
	}

	protected Dimension myVirtualSize = new Dimension();

	/** @since 20020702 */
	protected void makeEvidenceDialog()
	{
		//this.myMonitor = new EvidenceDialog( myMonitorPrefs, myDVar, myCoordinateTransformer );
		this.myMonitor = new MonitorImpl( myMonitorPrefs, myDVar, myCoordinateTransformer );
		if( (!myDVar.isSampleMode()) && myDVar.getNetworkInternalFrame().getApproxEngine() != null ) myMonitor.setApprox( myDVar.getNetworkInternalFrame().getApproxEngine() );

		this.myParent.add( myMonitor.asJComponent(), 0 );//, JLayeredPane.PALETTE_LAYER );
		myMonitor.setVirtualLocation( myDVar.getLocation( new Point() ) );
	}

	/** @since 20080228 */
	public JComponent getMonitorParent(){
		return myParent;
	}

	/** @since 20030310 */
	public Rectangle getBoundsManaged( Rectangle rv )
	{
		rv = getBounds( rv );
		if( myMonitor != null && myMonitor.isVisible() ) rv.add( myMonitor.getBounds() );
		return rv;
	}

	/** @since 20030305 */
	public void		setActualScale( double factor )
	{
		super.setActualScale( factor );
		if( myMonitor != null ) myMonitor.setActualScale( factor );
	}
	/**
		@author Keith Cascio
		@since 030503
	*/
	public void		recalculateActual()
	{
		super.recalculateActual();
		if( myMonitor != null ) myMonitor.recalculateActual();
	}

	/** @since 081304 */
	public void recalculateActualSize(){
		if( myVirtualSize != null ) super.recalculateActualSize();
	}

	/** @since 042502 */
	public void showNodePropertiesDialog(){
		if( myFlagDisableActions ) return;
		myDVar.showNodePropertiesDialog( getTopLevelAncestor() , false );
	}

	public void doDoubleClick(){
		showNodePropertiesDialog();
	}

	/** interface SamiamUserModal
		@since 060805 */
	public void setSamiamUserMode( SamiamUserMode mode ){
		myFlagDisableActions = mode.contains( SamiamUserMode.COMPILING );
	}

	/**
		interface EvidenceChangeListener
		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ece )
	{
		if( myMonitor != null ) myMonitor.warning( ece );
	}

	/**
		@author Keith Cascio
		@since 070202
	*/
	public void evidenceChanged( EvidenceChangeEvent ECE )
	{
		evidenceChanged( ECE, Double.NaN );
	}

	/**
		@author Keith Cascio
		@since 071003
	*/
	public void evidenceChanged( EvidenceChangeEvent ece, double globalMaximumProbability )
	{
		invalidateMonitor( ece, globalMaximumProbability );
		if( ece != null && ece.recentEvidenceChangeVariables.contains( myDVar ) ) updateNodeIconObserved();
		setDVarText();//reText();
		repaint();
	}

	/** @since 011403 */
	private void updateNodeIconObserved()
	{
		NodeIcon icon = getNodeIcon();
		if( icon != null ) icon.setObserved( myDVar != null && myDVar.getObservedValue() != null );
	}

	/**
		@author Keith Cascio
		@since 070202
	*/
	protected boolean myIsEvidenceDialogValid = false;

	/**
		@author Keith Cascio
		@since 070202
	*/
	public void invalidateMonitor( EvidenceChangeEvent ece, double globalMaximumProbability )
	{
		//if( NetworkInternalFrame.DEBUG_CPTCHANGED ) Util.STREAM_DEBUG.println( "(" + myDVar + ").invalidateMonitor()" );
		myIsEvidenceDialogValid = false;
		if( myMonitor != null && myMonitor.isVisible() == true ) myMonitor.evidenceChanged( ece, globalMaximumProbability );
	}

	/**
		@author Keith Cascio
		@since 103102
	*/
	public void killEvidenceDialog()
	{
		myIsEvidenceDialogValid = false;
		myMonitor = null;
	}

	public DisplayableFiniteVariable getFiniteVariable()
	{
		return myDVar;
	}

	public void moveEvidenceDialog( Point p )
	{
		if( myMonitor != null ) myMonitor.setVirtualLocation( p );
	}

	public Monitor getEvidenceDialog()
	{
		return myMonitor;
	}

	/** @since 20020702 */
	public void displayEvidenceDialog()
	{
	  //System.out.println( "(NodeLabel)"+getText()+".displayEvidenceDialog()..." );

		NetworkInternalFrame HNIF = myDVar.getNetworkInternalFrame();
		UI ui = null;
		if( HNIF != null )
		{
			ui = HNIF.getParentFrame();
			ui.setCursor( UI.CURSOR_WAIT_MONITORS );
		}

		//System.out.print( "myMonitor == null==" + (myMonitor == null) );

		if( myMonitor == null ) makeEvidenceDialog();
		else
		{
			if( !myIsEvidenceDialogValid ) myMonitor.evidenceChanged( null );
			boolean parented = myParent.isAncestorOf( myMonitor.asJComponent() );
			//System.out.print( "...parented==" + parented );
			//System.out.print( "...parent==" + myParent.getClass() );
			if( !parented ) myParent.add( myMonitor.asJComponent(), 0 );//, JLayeredPane.PALETTE_LAYER );
		}

		myIsEvidenceDialogValid = true;

		if( ! myMonitor.isVisible() ){
			Dimension  mSize = myMonitor.getActualSize(  new Dimension() );
			Dimension  nSize =      this.getActualSize(  new Dimension() );
			Point      loc   =      this.getActualLocation(  new Point() );
			loc.x           += (nSize.width  - mSize.width ) >> 1;
			loc.y           += (nSize.height - mSize.height) >> 1;
			loc.x            = Math.max( 0, loc.x );
			loc.y            = Math.max( 0, loc.y );
		  //System.out.println( "     loc1? " + loc );
			myMonitor.setActualLocation( loc );
		}
		myMonitor.setVisible( true );

		if( ui != null ) ui.setDefaultCursor();

		//System.out.println();

		//Util.printStats( myMonitor.asJComponent(), "(Monitor)"+getText() );
		//Util.printStats( this, "(NodeLabel)"+getText() );
		//Util.printStats( myParent, "(JDesktopPane)"+getText() );
	}

	public void hideEvidenceDialog()
	{
		if( myMonitor != null ) myMonitor.setVisible( false );
	}

	/**
		@author Keith Cascio
		@since 092002
	*/
	public void setEvidenceDialogShown( boolean show )
	{
		if( show ) displayEvidenceDialog();
		else hideEvidenceDialog();
	}

	/**
		@author Keith Cascio
		@since 120402
	*/
	public boolean isEvidenceDialogShown()
	{
		return ( myMonitor != null && myMonitor.isVisible() );
	}

	/** @since 080304 */
	public void reText(){
		setDVarText();
	}

	public static final String STR_OPERATOR_IDENTICAL_TO = Util.htmlEncode( " \u2261 " );
	public static final String STR_OPERATOR_SIMILAR_TO = Util.htmlEncode( " \u2245 " );

/* profiling
not optimized
NetworkDisplay.initComponents()
    first           : 00.00% (            0),
    makeNodes()     : 96.88% (   9703125000),
    sortComponents(): 01.87% (    187500000),
    make edges      : 01.25% (    125000000)
NetworkDisplay.initComponents()
    first           : 00.00% (            0),
    makeNodes()     : 97.11% (   8390625000),
    sortComponents(): 01.99% (    171875000),
    make edges      : 00.90% (     78125000)
NetworkDisplay.initComponents()
    first           : 00.00% (            0),
    makeNodes()     : 97.15% (   9578125000),
    sortComponents(): 02.06% (    203125000),
    make edges      : 00.79% (     78125000)
NetworkDisplay.initComponents()
    first           : 00.00% (            0),
    makeNodes()     : 97.88% (   8640625000),
    sortComponents(): 01.24% (    109375000),
    make edges      : 00.88% (     78125000)

optimized
NetworkDisplay.initComponents()
    first           : 00.00% (            0),
    makeNodes()     : 97.48% (   9656250000),
    sortComponents(): 01.58% (    156250000),
    make edges      : 00.95% (     93750000)
NetworkDisplay.initComponents()
    first           : 00.00% (            0),
    makeNodes()     : 96.70% (   8687500000),
    sortComponents(): 02.43% (    218750000),
    make edges      : 00.87% (     78125000)
NetworkDisplay.initComponents()
    first           : 00.00% (            0),
    makeNodes()     : 97.23% (   9328125000),
    sortComponents(): 01.95% (    187500000),
    make edges      : 00.81% (     78125000)
NetworkDisplay.initComponents()
    first           : 00.00% (            0),
    makeNodes()     : 97.59% (   8859375000),
    sortComponents(): 01.72% (    156250000),
    make edges      : 00.69% (     62500000)
*/

	/** @since 20060521 */
	public void paint( Graphics g ){
		if( NodeLabel.this.flagTextInvalid ) doSetDVarText();
		super.paint( g );
	}

	/** @since 20060521 */
	public void setDVarText(){
		NodeLabel.this.flagTextInvalid = true;
		NodeLabel.this.repaint();
	}

	/** @since 20021106 */
	private void doSetDVarText(){
		if( myDVar == null ) return;

		String text = null, tooltip = null;
		try{
			text = myDVar.toString();
			tooltip = text;
			if( myDVar.isSampleMode() ) {}
			else if( shouldHide() ) text = "";
			else if( FLAG_DISPLAY_LIKELIEST ){
				Object likeliest = null;
				String operator = null;
				String color = null;

				DisplayableBeliefNetwork bn = myDVar.getBeliefNetwork();
				EvidenceController ec = bn.getEvidenceController();
				Object evidenceValue = ec.getValue( myDVar );
				if( evidenceValue != null ){
					likeliest = evidenceValue;
					operator = STR_OPERATOR_IDENTICAL_TO;
					color = STRING_HTMLColorObserved;
					myLastLikeliest = null;
				}
				else{
					InferenceEngine ie = myDVar.getInferenceEngine();
					if( ie != null ){
						Table conditional = ie.conditional( myDVar );
						int index = conditional.maxInd();
						if( index >= 0 ){
							likeliest = myDVar.instance( index );
							operator = STR_OPERATOR_SIMILAR_TO;
							if( (myLastLikeliest == null) || (likeliest == myLastLikeliest) ) color = STRING_HTMLColorLikeliest;
							else color = STRING_HTMLColorFlip;
							myLastLikeliest = likeliest;
						}
					}
				}
				if( likeliest != null ){
					String maybeBreak = FLAG_LIKELIEST_BREAKS_LINE ? "<br>" : "";
					text = "<html>" +
						Util.htmlEncode( text ) +
						maybeBreak +
						operator +
						"<font color=\"#" +
						color + "\">" +
						Util.htmlEncode( likeliest.toString() );
					tooltip = tooltip + " - " + likeliest.toString();
				}
			}
		}catch( Exception throwable ){
			String strerror = "error. ";
			text = (text == null) ? strerror : strerror + text;
			tooltip = throwable.toString();
			System.err.println( "Warning: error in setDVarText() for " + myDVar + ": " + tooltip );
		}finally{
			//////////////////////////////////////////////////
			//
			// dangerous, see NetworkComponentLabelImpl.init()
			// keith cascio 20060521
			//
			//setToolTipText( tooltip );
			//
			//////////////////////////////////////////////////
			setText( text );
		}

		NodeLabel.this.flagTextInvalid = false;
	}

	/**
		Allow options to change.
	*/
	public void changePackageOptions()
	{
		updatePreferences();
	}

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the COMMITTED values of
		the Preferences in SamiamPreferences prefs.
	*/
	public void updatePreferences()
	{
		if( getFiniteVariable() == null ) return;

		super.updatePreferences();
		if( myMonitor != null ) myMonitor.updatePreferences();
		setDVarText();
	}

	/**
		Call this method to ask a PreferenceListener to
		respond to RECENT changes in the EDITED values of
		the Preferences in SamiamPreferences prefs.
	*/
	public void previewPreferences()
	{
		if( getFiniteVariable() == null ) return;

		super.previewPreferences();
		if( myMonitor != null ) myMonitor.previewPreferences();
		setDVarText();
	}

	/**
		Call this method to force a PreferenceListener to
		reset itself according to the Preferences in
		SamiamPreferences prefs.
	*/
	public void setPreferences()
	{
		if( getFiniteVariable() == null ) return;

		super.setPreferences();
		if( myMonitor != null ) myMonitor.setPreferences();
		setDVarText();
	}
}
