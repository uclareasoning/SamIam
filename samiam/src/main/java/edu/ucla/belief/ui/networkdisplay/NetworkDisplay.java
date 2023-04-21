package edu.ucla.belief.ui.networkdisplay;

import        edu.ucla.belief.ui.internalframes.Bridge2Tiger;
import        edu.ucla.belief.ui.internalframes.Bridge2Tiger.Troll;
import        edu.ucla.belief.ui.internalframes.Bridge2Tiger.ProbabilityRewriteArgs;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.actionsandmodes.*;
import edu.ucla.belief.io.NodeLinearTask;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.List;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.*;
import java.lang.reflect.Method;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.structure.*;
import edu.ucla.util.WeakLinkedList;
import edu.ucla.util.Interruptable;
//import edu.ucla.util.JVMTI;

/**
 * This class draws a belief network on its JDesktopPane.
 * It registers itself as an EvidenceChangeListener and a
 * RecompilationListener on the NetworkInternalFrame.
*/
public class NetworkDisplay extends JInternalFrame implements
	CoordinateTransformer, EvidenceChangeListener, RecompilationListener,
	CPTChangeListener, NetStructureChangeListener, SelectionListener,
	SamiamUserModal//, ActionListener
{
	public NetworkDisplay(  NetworkInternalFrame doc,
				SamiamPreferences sPrefs,
				DSLSubmodel submodel )
	{
		super( "Network", true,false, true, true );
		this.mySamiamPreferences = sPrefs;
		this.myNetPrefs		= sPrefs.getPreferenceGroup( SamiamPreferences.NetDspNme );
		this.myMonitorPrefs	= sPrefs.getPreferenceGroup( SamiamPreferences.MonitorsDspNme );
		this.hnInternalFrame	= doc;
		this.hnInternalFrame.addEvidenceChangeListener( this );
		this.hnInternalFrame.addRecompilationListener( this );
		this.hnInternalFrame.addNetStructureChangeListener( this );
		this.hnInternalFrame.addCPTChangeListener( this );
		this.mySubmodel		= submodel;
		this.myBeliefNetwork	= hnInternalFrame.getBeliefNetwork();
		this.myIsMainSubmodel	= ( mySubmodel == myBeliefNetwork.getMainDSLSubmodel() );
		//this.myIsDSL		= myBeliefNetwork.getProperties().containsKey( DSLConstants.keyISDSL );
		this.myIsDSL		= myBeliefNetwork.isGenieNet();

		init();
	}

	public boolean DEBUG_VERBOSE = Util.DEBUG_VERBOSE;
	public static final String STR_FILENAME_ICON = "Display16.gif";

	/* ------------------
	* public interface
	* ------------------ */

	/** @since 082604 */
	public Collection getAllComponents(){
		return myComponentList;
	}

	/** @since 081704 */
	public void handleHidden(){
		for( Iterator it = myComponentList.iterator(); it.hasNext(); ){
			((NetworkComponentLabel)it.next()).handleHidden();
		}
		repaint();

		for( Iterator it = getSubmodelDisplays().iterator(); it.hasNext(); ){
			((NetworkDisplay) it.next()).handleHidden();
		}
	}

	/** @since 080904 */
	public void recalculateActuals( boolean coordinates, boolean color )
	{
		if( !(coordinates || color) ) return;

		NetworkComponentLabel next;
		for( Iterator it = myComponentList.iterator(); it.hasNext(); ){
			next = (NetworkComponentLabel)it.next();
			if( coordinates ) next.recalculateActual();
			if( color ) next.recalculateActualColor();
		}

		for( Iterator it = getSubmodelDisplays().iterator(); it.hasNext(); ){
			((NetworkDisplay) it.next()).recalculateActuals( coordinates, color );
		}
	}

	/** @since 081004 */
	public void recalculateActuals(){
		this.recalculateActuals( true, true );
	}

	/** @since 080404 */
	public NetworkInternalFrame getNetworkInternalFrame(){
		return hnInternalFrame;
	}

	//public void setMaximum(boolean b) throws PropertyVetoException
	//{
	//	String nom = ( hnInternalFrame == null ) ? "" : hnInternalFrame.getFileNameSansPath();
		//System.out.println( "(NetworkDisplay)"+nom+".setMaximum(" +b+ ")" );
	//	//(new Throwable()).printStackTrace();
	//	super.setMaximum(b);
	//}

	//protected void fireVetoableChange(String propertyName,Object oldValue,Object newValue)throws java.beans.PropertyVetoException
	//{
	//	String nom = ( hnInternalFrame == null ) ? "" : hnInternalFrame.getFileNameSansPath();
		//System.out.println( "(NetworkDisplay)"+nom+".fireVetoableChange(" +propertyName+ ")" );
	//	super.fireVetoableChange( propertyName, oldValue, newValue );
	//}

	protected void firePropertyChange( String propertyName,Object oldValue,Object newValue )
	{
		String nom = ( hnInternalFrame == null ) ? "" : hnInternalFrame.getFileNameSansPath();
		//System.out.println( "(NetworkDisplay)"+nom+".firePropertyChange(" +propertyName+ ")" );

		//if( propertyName.equals( "_WhenInFocusedWindow" ) || propertyName.equals( "ancestor" ) ) //System.out.println( "!!!!!!_WhenInFocusedWindow || ancestor" );
		//else super.firePropertyChange( propertyName, oldValue, newValue );

		/*
		if( propertyName.equals( "ancestor" ) )
		{
			//System.out.println( "!!!!!!ancestor" );
			if( nom.equals("cancer.net") )
			{
				//(new Throwable()).printStackTrace();
				//System.out.println( "...pre" );
				//super.firePropertyChange( propertyName, oldValue, newValue );
				//System.out.println( "post..." );
			}
		}
		else super.firePropertyChange( propertyName, oldValue, newValue );*/

		if( !propertyName.equals( "ancestor" ) ) super.firePropertyChange( propertyName, oldValue, newValue );
	}

	public static int INT_TRANSLATION_INCREMENT = (int)8;

	/**
		@author Keith Cascio
		@since 021903
	*/
	public void moveSelectedNodes( int direction )
	{
		int deltaX = (int)0;
		int deltaY = (int)0;

		switch( direction )
		{
			case SwingConstants.NORTH:
				deltaY = -INT_TRANSLATION_INCREMENT;
				break;
			case SwingConstants.SOUTH:
				deltaY = INT_TRANSLATION_INCREMENT;
				break;
			case SwingConstants.EAST:
				deltaX = INT_TRANSLATION_INCREMENT;
				break;
			case SwingConstants.WEST:
				deltaX = -INT_TRANSLATION_INCREMENT;
				break;
			default:
				break;
		}

		moveSelectedNodes( deltaX, deltaY );

		confirmActualLocations( mySelectedComponents );
	}

	public JComponent getJDesktopPane()
	{
		return myDesktopPane;
	}

	/** @since 20060721 */
	public void listZorder( java.io.PrintStream stream ){
		/* java 5 only
		for( Component comp : myDesktopPane.getComponents() ){
			String text = null;
			if(      comp instanceof JLabel  ) text = ((JLabel )comp).getText();
			else if( comp instanceof Monitor ) text = ((Monitor)comp).getTitle();
			else                               text =           comp .toString();
			stream.println( text + ": " + myDesktopPane.getComponentZOrder( comp ) );
		}*/
		//java 4 ok
		for( int i=0; i<myDesktopPane.getComponentCount(); i++ ){
			Component comp = myDesktopPane.getComponent(i);
			String text = null;
			if(      comp instanceof JLabel  ) text = ((JLabel )comp).getText();
			else if( comp instanceof Monitor ) text = ((Monitor)comp).getTitle();
			else                               text =           comp .toString();
			stream.println( text + ": " + i );
		}
	}

	/** @since 20020709 */
	public void setSamiamUserMode( SamiamUserMode newmode )
	{
		//System.out.println( "NetworkDisplay.setSamiamUserMode( "+newmode+" ), compiling? " + newmode.contains( SamiamUserMode.COMPILING ) );

		String keyBackgroundColor = SamiamPreferences.netBkgdClr;
		if( newmode.contains( SamiamUserMode.NEEDSCOMPILE ) ) keyBackgroundColor = SamiamPreferences.netBkgdClrNeedsCompile;

		myDesktopPane.setBackground( (Color) mySamiamPreferences.getMappedPreference( keyBackgroundColor ).getValue() );

		if( myActionDelete         != null ){ myActionDelete        .setMode( newmode, hnInternalFrame ); }
		if( myActionShowMonitor    != null ){ myActionShowMonitor   .setMode( newmode, hnInternalFrame ); }
		if( myActionShowCPTMonitor != null ){ myActionShowCPTMonitor.setMode( newmode, hnInternalFrame ); }
		if( myActionNodeProperties != null ){ myActionNodeProperties.setMode( newmode, hnInternalFrame ); }

		if( newmode.contains( SamiamUserMode.QUERY ) ) mouseSel.forceDefaultMode();

		this.myLocationsFrozen = newmode.contains( SamiamUserMode.LOCATIONSFROZEN );

		for( Iterator it = getSubmodelDisplays().iterator(); it.hasNext(); ){
			((NetworkDisplay)it.next()).setSamiamUserMode( newmode );
		}

		//update labels
		NetworkComponentLabel next;
		for( Iterator itr = myComponentList.iterator(); itr.hasNext(); ){
			next = (NetworkComponentLabel) itr.next();
			if( next == null ) itr.remove();
			else next.setSamiamUserMode( newmode );
		}

		repaint();
	}

	/** @since 072304 */
	public void repaintTree()
	{
		for( Iterator it = getSubmodelDisplays().iterator(); it.hasNext(); ){
			((NetworkDisplay) it.next()).repaintTree();
		}

		repaint();
	}

	/**
		@author Keith Cascio
		@since 070802
	*/
	final public void refresh()
	{
		//System.out.println( "NetworkDisplay.refresh()" );
		//debugPrintNodeLocations();

		Collection[] result        = refreshPre();
		Collection   selected      = result[0];
		Collection   monitorsshown = result[1];

		for( Iterator it = getSubmodelDisplays().iterator(); it.hasNext(); )
		{
			((NetworkDisplay) it.next()).refresh();
		}

		refreshPost( selected, monitorsshown );
	}

	/** @since 20030328 */
	protected Collection[] refreshPre()
	{
		//System.out.println( "(NetworkDisplay)" + getTitle() + ".refreshPre()...num components == " + myDesktopPane.countComponents()  );
		//Util.printStats( this, "(NetworkDisplay)"+getTitle() );

		//this.myDesktopPane.removeAll();

		Collection setSelectedDVars = new ArrayList( mySelectedComponents.size() );
		Collection setMonitorsShown = new LinkedList();
		NetworkComponentLabel next;
		for( Iterator it = mySelectedComponents.iterator(); it.hasNext(); ){
			next = (NetworkComponentLabel) it.next();
			if( next.getFiniteVariable() != null ) setSelectedDVars.add( next.getFiniteVariable() );
			if( next.isEvidenceDialogShown() ) setMonitorsShown.add( next );
		}

		//setMonitorsVisible( false );
		for( Iterator it = myComponentList.iterator(); it.hasNext(); )
		{
			((NetworkComponentLabel) it.next()).removeFromParentManaged();
		}

		this.myComponentList.clear();
		synchronized( mySynchronizationEdgeList ){
			this.myEdgeList.clear();
			if( myListRecoverableArrows != null ){ this.myListRecoverableArrows.clear(); }//20080221
		}

		return new Collection[] { setSelectedDVars, setMonitorsShown };
	}

	/** @author keith cascio
		@since  20030328 */
	protected void refreshPost( Collection selected, Collection monitorsshown )
	{
		//System.out.print( "(NetworkDisplay)" + getTitle() + ".refreshPost()...num components == " + myDesktopPane.countComponents()  );
		//myDesktopPane.list( System.out, (int)0 );

		double oldZoomFactor = myZoomFactor;
		setZoomFactor( (double)1 );

		initComponents();
		try{ refreshNotoriousEdges(); }
		catch( Throwable thrown ){ System.err.println( "warning: NetworkDisplay.refreshPost() caught " + thrown ); }
		initGraphicsHelpers();
		fireSelectionReset();

		setZoomFactor( oldZoomFactor );

		setSamiamUserMode( hnInternalFrame.getSamiamUserMode() );

		for( Iterator it = selected.iterator(); it.hasNext(); ){
			((DisplayableFiniteVariable)it.next()).getNodeLabel().setSelected( true );
		}

		try{
			for( Iterator it = monitorsshown.iterator(); it.hasNext(); ){
				((NodeLabel)it.next()).displayEvidenceDialog();
			}
		}catch( Exception exception ){
			System.err.println( "Warning in NetworkDisplay.refreshPost(): " + exception );
		}

		//revalidate();
		//boolean oldVisible = isVisible();
		//System.out.print( "oldVisible==" + oldVisible );
		//setVisible( true );
		//try{
		//	setSelected( true );
		//}catch( java.beans.PropertyVetoException e ){
		//	System.err.println( "Java warning failed to give focus to internal frame: " + getTitle() );
		//}
		//setVisible( oldVisible );

		//debugPrintNodeLocations();
		//myDesktopPane.revalidate();
		//myDesktopPane.repaint();

		//System.out.println();
		//Util.printStats( this, "(NetworkDisplay)"+getTitle() );
	}

	/**
		@author Keith Cascio
		@since 042202
	*/
	public void debugPrintNodeLocations()
	{
		Util.STREAM_DEBUG.println( "(NetworkDisplay)" + mySubmodel.getName() + " node locations:" );
		NetworkComponentLabel cv = null;
		Point p = new Point();
		for( Iterator itr = myComponentList.iterator(); itr.hasNext(); )
		{
			cv = (NetworkComponentLabel) itr.next();
			cv.getVirtualLocation( p );
			Util.STREAM_DEBUG.print( cv.getText() + " virtual" + formatPoint(p) + " actual" );
			cv.getActualLocation( p );
			Util.STREAM_DEBUG.println( formatPoint(p) );
		}
	}

	/** @since 20021106 */
	public static String formatPoint( Point p )
	{
		return "( " + String.valueOf( p.x ) + ", " + String.valueOf( p.y ) + " )";
	}

	/** @since 20090429 */
	private Interruptable delayedCursor = new Interruptable(){
		{ setName( "NetworkDisplay delayed cursor setter" ); }
		public void runImpl( Object arg1 ) throws InterruptedException{
			Thread.sleep( 0x40 );
			NetworkDisplay.this.setCursor( (Cursor) arg1 );
		}
	};

	/** @since 20080228 */
	public NodeLabel setCPTMonitorShown( NodeLabel label, boolean show ){
		return Troll.solicit().setCPTMonitorShown( label, show );
	}

	/** @return the resulting scroll bounds, local to the submodel containing dVar
		@since 20020312 */
	public Rectangle ensureNodeIsVisible( DisplayableFiniteVariable dVar, boolean additive ){
		try{
			DSLSubmodel subm = dVar.getDSLSubmodel();
			if( subm == null || subm == mySubmodel ){
				NodeLabel label        = dVar.getNodeLabel();
			  //Dimension sizeNode     = label.getActualSize(     new Dimension() );
			  //Point     locationNode = label.getActualLocation( new Point() );
				Rectangle union        = new Rectangle( label.getActualLocation( new Point() ), label.getActualSize( new Dimension() ) );
				//int halfWidth = (int)(size.width*0.5);
				//int halfHeight = (int)(size.height*0.5);
				//int x = location.x+halfWidth;
				//int y = location.y+halfHeight;
				//nodeSelectionClearAndAdd( new Rectangle( x, y, x+1,y+1 ) );

				//if( !additive ) nodeSelectionClearAll();
				//label.setSelected( true );
				//label.scrollRectToVisible( new Rectangle( sizeNode ) );//new Rectangle( -0x20,-0x20,sizeNode.width+0x40,sizeNode.height+0x40 ) );
				return center( union );
			}
			else{
				SubmodelLabel sl = (SubmodelLabel) subm.userobject;
				return sl.showNetworkDisplay().ensureNodeIsVisible( dVar, additive );
			}
		}catch( Exception exception ){
			System.err.println( "warning: NetworkDisplay.ensureNodeIsVisible() caught " + exception );
		}
		return null;
	}

	/** @return the resulting scroll bounds, local if at least one node is in the local submodel
		@since 20070402 */
	public Rectangle ensureNodesVisible( Collection/*<DisplayableFiniteVariable>*/ dVars ){
		return ensureNodesVisible( dVars, false );
	}

	/** @return the resulting scroll bounds, local if at least one node is in the local submodel
		@since 20070402 */
	private Rectangle ensureNodesVisible( Collection/*<DisplayableFiniteVariable>*/ dVars, boolean canonicalized ){
		try{
			if( ! canonicalized ){
				Map/*<DSLSubmodel,Collection<DisplayableFiniteVariable>>*/ bySubmodel = new HashMap/*<DSLSubmodel,Collection<DisplayableFiniteVariable>>*/(1);
				DSLSubmodel                           subm   = null;
				Collection/*<DisplayableFiniteVariable>*/ bucket = null;
				DisplayableFiniteVariable dVar = null;
				for( Iterator it = dVars.iterator(); it.hasNext(); ){
					subm = (dVar = (DisplayableFiniteVariable) it.next()).getDSLSubmodel();
					if( subm == null ) subm = mySubmodel;
					if( (bucket = (Collection) bySubmodel.get( subm )) == null ) bySubmodel.put( subm, bucket = new LinkedList/*<DisplayableFiniteVariable>*/() );
					bucket.add( dVar );
				}
				dVars = (Collection) bySubmodel.get( mySubmodel );
				bySubmodel.remove(      mySubmodel );
				Rectangle ret = null;
				DSLSubmodel submodel = null;
				for( Iterator it = bySubmodel.keySet().iterator(); it.hasNext(); ){
					submodel = (DSLSubmodel) it.next();
					ret = ((SubmodelLabel) submodel.userobject).showNetworkDisplay().ensureNodesVisible( (Collection) bySubmodel.get( submodel ), true );
				}
				if( dVars == null || dVars.isEmpty() ) return ret;
			}

			NodeLabel label        = null;
			Rectangle bounds       = new Rectangle(), union = null;
			Dimension sizeNode     = new Dimension();
			Point     locationNode = new Point();
			for( Iterator it = dVars.iterator(); it.hasNext(); ){
				label = ((DisplayableFiniteVariable) it.next()).getNodeLabel();
				label.getActualSize(     sizeNode     );
				label.getActualLocation( locationNode );
				bounds.setBounds( locationNode.x, locationNode.y, sizeNode.width, sizeNode.height );
				union = (union == null) ? new Rectangle( bounds ) : union.union( bounds );
			}

			return center( union );
		}catch( Exception exception ){
			System.err.println( "warning: NetworkDisplay.ensureNodesVisible() caught " + exception );
			exception.printStackTrace();
		}
		return null;
	}

	/** @return the resulting local scroll bounds
		@since 20070402 */
	public Rectangle center( Rectangle union ) throws Exception{
		Dimension sizeExtent     = myJScrollPane.getViewport().getExtentSize();
		Point     locationScroll = new Point(
			Math.max( 0, union.x - Math.max( 0, (int)((sizeExtent.width  - union.width ) * 0.5 ) ) ),
			Math.max( 0, union.y - Math.max( 0, (int)((sizeExtent.height - union.height) * 0.5 ) ) ) );

		Rectangle ret = new Rectangle( locationScroll, sizeExtent );
		myDesktopPane.scrollRectToVisible( ret );
		return ret;
	}

	/**
		@author Keith Cascio
		@since 101602
	*/
	/*
	public void translate( Point p )
	{
		if( netBounds != null)
		{
			p.x += netBounds.x;
			p.y = -p.y;
			if( DEBUG_COORDINATES ) Util.STREAM_VERBOSE.print( "relative to " + netBounds + ": modified location = " + p );
		}
	}*/

	/** @since 20021016 */
	public void promptUserActualPoint( String msg, UserInputListener list )
	{
		synchronized( mySynchronization )
		{

		if( ! isNormalMode() ){ mouseSel.forceDefaultMode(); }

		if( isNormalMode() )
		{
			myCurrentUserInputListener = list;
			myEditMode = EditMode.MODE_PROMPT_USER_POINT;
			mouseSel.setMode( SelectionMode.MODE_LOCATION_SELECTION );
			myStatusBar.displayMessage( msg + " (Right-click to cancel.)");
		}

		}
	}

	transient protected UserInputListener myCurrentUserInputListener = null;

	/**
		@author Keith Cascio
		@since 061102
	*/
	public static final boolean FLAG_ADDITIVE_SHOW_MONITOR_SEMANTICS = false;

	/**
	* This function will cause all selected nodes to have their Evidence
	* dialog boxes displayed and any non-selected node will have its
	* Evidence dialog hidden.
	*/
	public void showEvidenceDialogs()
	{
		if( !isNormalMode()) return;

		edu.ucla.belief.ui.statusbar.StatusBar bar = hnInternalFrame.getParentFrame().getStatusBar();
		String message = "showing monitors in submodel " + mySubmodel.getName();
		if( bar != null ) bar.pushText( message, StatusBar.WEST );

		for( ListIterator itr = myComponentList.listIterator(); itr.hasNext();)
		{
			//Object obj = (NodeLabel)itr.next();
			Object obj = itr.next();
			if( obj instanceof NodeLabel )
			{
				NodeLabel lbl = (NodeLabel)obj;
				if( lbl.isSelected()) lbl.displayEvidenceDialog();				else if( !FLAG_ADDITIVE_SHOW_MONITOR_SEMANTICS )
				{
					//Keith Cascio 030802: we want additive semantics so do not hide
					lbl.hideEvidenceDialog();
				}
			}
		}

		if( bar != null ) bar.popText( message, StatusBar.WEST );

		for( Iterator it = getSubmodelDisplays().iterator(); it.hasNext(); )
		{
			((NetworkDisplay)it.next()).showEvidenceDialogs();
		}
	}

	public static final String STR_ERR_DELETE_NODES_MSG = "Please select the node(s) first.";
	public static final String STR_ERR_DELETE_NODES_TITLE = "No Selection Error";

	/**
	* This function will cause all currently selected nodes to be
	* deleted.
	*/
	public void deleteSelectedNodes()
	{
		synchronized( mySynchronization )
		{

		if( ! isNormalMode() ){ mouseSel.forceDefaultMode(); }

		if( isNormalMode() )
		{
			mySelectedDVars = getSelectedNodes( mySelectedDVars );
			if( mySelectedDVars.isEmpty() ) hnInternalFrame.getParentFrame().showMessageDialog( STR_ERR_DELETE_NODES_MSG, STR_ERR_DELETE_NODES_TITLE, JOptionPane.ERROR_MESSAGE );
			else deleteNodes( mySelectedDVars );
		}

		}
	}

	/** @since 20071211 */
	public NetworkDisplay initiateCPTCopy(){
		synchronized( mySynchronization ){
			if( ! isNormalMode() ) return this;

			myEditMode = EditMode.MODE_COPY_CPT;
			mouseSel.setMode( SelectionMode.MODE_NODE_SELECTION );
			myStatusBar.displayMessage( "Please select a source node. (Right-click to cancel.)" );
		}
		return this;
	}

	public void createNewEdge()
	{
		synchronized( mySynchronization )
		{

		if( ! isNormalMode() ){ mouseSel.forceDefaultMode(); }

		if( isNormalMode() )
		{
			//addEdgeMode = true;
			myEditMode = EditMode.MODE_ADD_EDGE;
			mouseSel.setMode( SelectionMode.MODE_NODE_SELECTION );
			myStatusBar.displayMessage( "Please select starting node for new edge. (Right-click to cancel.)" );
		}

		}
	}

	public void createNewNode()
	{
		synchronized( mySynchronization )
		{

		if( ! isNormalMode() ){ mouseSel.forceDefaultMode(); }

		if( isNormalMode() )
		{
			//addNodeMode = true;
			myEditMode = EditMode.MODE_ADD_NODE;
			mouseSel.setMode( SelectionMode.MODE_LOCATION_SELECTION );
			myStatusBar.displayMessage( "Please select location for new node. (Right-click to cancel.)" );
		}

		}
	}

	public void userDeleteEdge()
	{
		synchronized( mySynchronization )
		{

		if( ! isNormalMode() ){ mouseSel.forceDefaultMode(); }

		if( isNormalMode() )
		{
			myEditMode = EditMode.MODE_DELETE_EDGE;
			mouseSel.setMode( SelectionMode.MODE_NODE_SELECTION );
			myStatusBar.displayMessage( "Please select the source node of the edge to delete. (Right-click to cancel.)" );
		}

		}
	}

	/** @since 20080219 */
	public void userReplaceEdge(){
		synchronized( mySynchronization ){
			if( isNormalMode() ){
				myEditMode = EditMode.MODE_REPLACE_EDGE;
				mouseSel.setMode( SelectionMode.MODE_NODE_SELECTION );
				myStatusBar.displayMessage( STR_PROMPT_EDGE_SOURCE );
			}
		}
	}

	/** @since 20080221 */
	public void userRecoverEdge(){
		synchronized( mySynchronization ){
			if( isNormalMode() ){
				myEditMode = EditMode.MODE_RECOVER_EDGE;
				mouseSel.setMode( SelectionMode.MODE_NODE_SELECTION );
				myStatusBar.displayMessage( STR_PROMPT_EDGE_SOURCE );
			}
		}
	}

	/** @since 061704 */
	public void promptUserEdge( String msg, UserInputListener list ){
		synchronized( mySynchronization ){
			if( isNormalMode() ){
				myCurrentUserInputListener = list;
				myEditMode = EditMode.MODE_PROMPT_USER_EDGE;
				mouseSel.setMode( SelectionMode.MODE_NODE_SELECTION );
				myStatusBar.displayMessage( STR_PROMPT_EDGE_SOURCE );
			}
		}
	}

	public static final String STR_PROMPT_EDGE_SOURCE = "Please select node where the edge begins. (Right-click to cancel.)";

	/** @since 102302 */
	public void fireRecalculateActual()
	{
		//System.out.println( "NetworkDisplay.fireRecalculateActual()" );

		SubmodelLabel smlabel = null;
		DSLSubmodel model = null;
		for( Iterator it = mySubmodel.getChildSubmodels(); it.hasNext(); )
		{
			model = (DSLSubmodel) it.next();

			if( model.userobject instanceof SubmodelLabel )
			{
				smlabel = (SubmodelLabel) model.userobject;
				smlabel.recalculateActual();
				//smlabel.getChildNetworkDisplay().setZoomFactor( newZoom );
				smlabel.getChildNetworkDisplay().fireRecalculateActual();
			}
			else if( model.userobject instanceof CoordinateVirtual )
			{
				((CoordinateVirtual) model.userobject).recalculateActual();
			}
		}

		Collection variablesMySubmodel = myBeliefNetwork.getVariables( mySubmodel );
		//System.out.println( "\t variablesMySubmodel = " + variablesMySubmodel );

		NodeLabel nl;
		DisplayableFiniteVariable dVar;
		for( Iterator it = variablesMySubmodel.iterator(); it.hasNext(); ){
			dVar = (DisplayableFiniteVariable) it.next();
			nl = dVar.getNodeLabel();
			if( nl == null ) System.err.println( "WARNING: in NetworkDisplay.fireRecalculateActual(), DFV " + dVar + " encountered, missing node label." );
			else nl.recalculateActual();
		}

		synchronized( mySynchronizationEdgeList ){
			for( Iterator it = myEdgeList.iterator(); it.hasNext(); ){
				((CoordinateVirtual) it.next()).recalculateActual();
			}
			for( Iterator it = myListRecoverableArrows.iterator(); it.hasNext(); ){
				((CoordinateVirtual) it.next()).recalculateActual();
			}
		}

		myDesktopPane.revalidate();
		myDesktopPane.repaint();
	}

	/** @since 101702 */
	public void setZoomFactor( double newZoom )
	{
		for( Iterator it = getSubmodelDisplays().iterator(); it.hasNext(); )
		{
			((NetworkDisplay) it.next()).setZoomFactor( newZoom );
		}

		double oldZoom = myZoomFactor;

		myZoomFactor = newZoom;
		myZoomFactorInverse = (double)1/newZoom;

		//netBounds.x = (int)( ((double)netBounds.x) * newZoom );
		//netBounds.y = (int)( ((double)netBounds.y) * newZoom );
		//netBounds.width = (int)( ((double)netBounds.width) * newZoom );
		//netBounds.height = (int)( ((double)netBounds.height) * newZoom );

		//double ratioNewToOld = newZoom / oldZoom;
		//scale( mySizeDesktopPane, ratioNewToOld );
		//resizeDesktop();

		fireRecalculateActual();

		recalculateDesktopSize();

		if( myZoomListener != null ) myZoomListener.zoomed( myZoomFactor );
	}

	public void setZoomListener( ZoomListener list )
	{
		myZoomListener = list;
	}

	protected ZoomListener myZoomListener;

	public interface ZoomListener
	{
		public void zoomed( double value );
	}

	/** @since 101702 */
	public double getZoomFactor()
	{
		return myZoomFactor;
	}

	/** @since 102102 */
	public void zoomIn()
	{
		double newZoom = myZoomFactor*(double)1.1;
		if( newZoom < DOUBLE_MAX_ZOOM_FACTOR ) setZoomFactor( newZoom );
	}

	/** @since 102102 */
	public void zoomOut()
	{
		double newZoom = myZoomFactor*(double)0.9;
		if( newZoom >= DOUBLE_MIN_ZOOM_FACTOR ) setZoomFactor( newZoom );
	}

	/** @since 102402 */
	public void fitOnScreen()
	{
		//System.out.println( "NetworkDiplay.fitOnScreen()" );
		double ratioMin;
		String msgWarning = null;

		if( myBeliefNetwork.isEmpty() ) ratioMin = (double)1;
		else
		{
			recalculateDesktopSize();

			Dimension dimNetworkVirtual = new Dimension( mySizeDesktopPane );
			actualToVirtual( dimNetworkVirtual );
			double widthNetwork = dimNetworkVirtual.getWidth();
			double heightNetwork = dimNetworkVirtual.getHeight();

			Dimension dimContentPane = getContentPane().getSize();
			double widthScreen = dimContentPane.getWidth();
			double heightScreen = dimContentPane.getHeight();

			double ratioWidth = widthScreen / widthNetwork;
			double ratioHeight = heightScreen / heightNetwork;

			ratioMin = Math.min( ratioWidth, ratioHeight );
			ratioMin *= DOUBLE_ZOOM_FUDGE_FACTOR;

			//ratioMin = Math.max( ratioMin, DOUBLE_MIN_ZOOM_FACTOR );
			if( ratioMin <= DOUBLE_MIN_ZOOM_FACTOR ){
				ratioMin = DOUBLE_MIN_ZOOM_FACTOR;
				msgWarning = "Warning: network too large to fit on screen.";
			}

		}

		setZoomFactor( ratioMin );
		//scale( mySizeDesktopPane, DOUBLE_ZOOM_FUDGE_FACTOR );
		//resizeDesktop();

		if( msgWarning != null ) hnInternalFrame.getParentFrame().showMessageDialog( msgWarning, "Fit on screen warning", JOptionPane.WARNING_MESSAGE );
	}

	/** @since 20050921
		Synonyms: scrunch, combine, compress, concentrate, condense, consolidate, contract, cram, integrate, pack, set, solidify, stuff, unify, unite
	*/
	public void cringe( boolean aggressive )
	{
		this.setLocation( new Point(0,0) );

		Rectangle effective = null;
		try{
			recalculateDesktopSize();
			effective = new Rectangle( 0, 0, this.getSize().width, mySizeDesktopPane.height );

			int titleheight = 64;
			javax.swing.plaf.InternalFrameUI ifui = this.getUI();
			if( ifui instanceof javax.swing.plaf.basic.BasicInternalFrameUI ){
				titleheight = ((javax.swing.plaf.basic.BasicInternalFrameUI)ifui).getNorthPane().getMinimumSize().height;
				if( aggressive ) effective.y = -titleheight;
			}

			effective.height += titleheight;

			Insets insets = this.getInsets();
			effective.height += (insets.top + insets.bottom);

			if( effective.height < 64 ) effective.height = 64;
			int heightScreen = this.hnInternalFrame.getRightHandDesktopPane().getSize().height;
			if( (effective.height + effective.y) > heightScreen ) effective.height = (heightScreen - effective.y);
		}catch( Exception exception ){
			System.err.println( "Warning: NetworkDisplay.cringe() failed, caught " + exception );
			return;
		}

		this.setBounds( effective );
	}

	protected static double DOUBLE_ZOOM_FUDGE_FACTOR = (double)0.97;

	protected double myZoomFactor = (double)1;
	protected double myZoomFactorInverse = (double)1;
	protected static double DOUBLE_MIN_ZOOM_FACTOR = (double)0.025031555049932444;
	protected static double DOUBLE_MAX_ZOOM_FACTOR = (double)235.60559825561234;

	/** @since 101702 */
	public static void scale( Point p, double factor )
	{
		p.x = (int)( ((double)p.x) * factor );
		p.y = (int)( ((double)p.y) * factor );
	}

	/** @since 101702 */
	public static void scale( Dimension d, double factor )
	{
		d.width = (int)( ((double)d.width) * factor );
		d.height = (int)( ((double)d.height) * factor );
	}

	/** invert y coordinate */
	public static void reflectY( Point p, int extreme )
	{
		//System.out.println( "NetworkDisplay.reflectY()" );
		p.y = extreme - p.y;
	}

	//CoordinateTransformer
	public Point		virtualToActual( Point p )
	{
		if( myBeliefNetwork.isHuginNet() && myBeliefNetwork.getVersion().shouldReflectY() ) reflectY( p, myExtremeVirtualYCoodinate );
		p.x += myActualTranslationDelta.width;
		p.y += myActualTranslationDelta.height;
		scale( p, myZoomFactor );
		return p;
	}
	public Point		actualToVirtual( Point p )
	{
		scale( p, myZoomFactorInverse );
		p.y -= myActualTranslationDelta.height;
		p.x -= myActualTranslationDelta.width;
		if( myBeliefNetwork.isHuginNet() && myBeliefNetwork.getVersion().shouldReflectY() ) reflectY( p, myExtremeVirtualYCoodinate );
		return p;
	}
	public Dimension	virtualToActual( Dimension d )
	{
		scale( d, myZoomFactor );
		return d;
	}
	public Dimension	actualToVirtual( Dimension d )
	{
		scale( d, myZoomFactorInverse );
		return d;
	}
	public float		virtualToActual( float f )
	{
		return f*((float)myZoomFactor);
	}
	public double		virtualToActual( double d )
	{
		return d*myZoomFactor;
	}

	/** @since 031103 */
	public CoordinateTransformer getMonitorSizeManager()
	{
		return myMonitorSizeManager;
	}
	public SizeManager myMonitorSizeManager;

	/** @since 070802 */
	public void autoArrange( int nodespacingVirtual, int netwidthVirtual )
	{
		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Auto-arranging:" );

		List topo = myBeliefNetwork.topologicalOrder();
		Set allPositions = new HashSet();
		Point lastAutoPosition = new Point( 0, 0 );
		Dimension sizeVirtual = new Dimension();
		Dimension sizeActual = new Dimension();
		int currentMaxRowHeightActual = 0;
		Point currentPosition = new Point();

		DisplayableFiniteVariable dVar = null;
		NodeLabel nl = null;

		Dimension dimGlobalActual;
		if( myFlagFileOverridesPref )
		{
			dimGlobalActual = myBeliefNetwork.getGlobalNodeSize( null );
			if( dimGlobalActual.equals( DIM_ZERO ) ) dimGlobalActual = myPreferenceNodeSize;
		}
		else dimGlobalActual = myPreferenceNodeSize;

		virtualToActual( dimGlobalActual );
		int nodespacingActual = (int)((double)nodespacingVirtual * myZoomFactor);

		for( Iterator it = topo.iterator(); it.hasNext(); )
		{
			dVar = (DisplayableFiniteVariable) it.next();
			nl = dVar.getNodeLabel();
			currentPosition = nl.getVirtualLocation( currentPosition );
			sizeVirtual = nl.getVirtualSize( sizeVirtual );
			sizeActual = nl.getActualSize( sizeActual );

			if( allPositions.contains( currentPosition ) )
			{
				lastAutoPosition.x += nodespacingVirtual + sizeVirtual.width;
				if( lastAutoPosition.x > netwidthVirtual )
				{
					virtualToActual( lastAutoPosition );

					lastAutoPosition.x = 0;
					lastAutoPosition.y += currentMaxRowHeightActual + nodespacingActual;

					actualToVirtual( lastAutoPosition );

					currentMaxRowHeightActual = 0;
				}

				if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Moving " + dVar + " from " + currentPosition + " to " + lastAutoPosition );

				nl.setVirtualLocation( lastAutoPosition );
				allPositions.add( new Point( lastAutoPosition ) );
			}
			else allPositions.add( new Point( currentPosition ) );

			if( sizeVirtual == null ) currentMaxRowHeightActual = Math.max( dimGlobalActual.height, currentMaxRowHeightActual );
			else currentMaxRowHeightActual = Math.max( sizeActual.height, currentMaxRowHeightActual );
		}

		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "DONE Auto-arranging" );

		refresh();
	}

	/* ------------------
	* Construction
	* ------------------ */

	/** @since 20020422 */
	protected void init()
	{
		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\n\nNetworkDisplay.init( submodel \"" + mySubmodel + "\", " + myBeliefNetwork.getClass().getName() + " )" );

		Icon iconFrame = MainToolBar.getIcon( STR_FILENAME_ICON );
		if( iconFrame != null ) setFrameIcon( iconFrame );

		initJDesktopPane();
		initStatusBar();
		initMouseHandler();
		initComponents();
		if( Thread.currentThread().isInterrupted() ) return;
		initGraphicsHelpers();
		initPopup();
		initKeyStrokes();
		revalidate();  //recalculate based on new preferred size
		//requestFocus();

		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "DONE NetworkDisplay.init( submodel \"" + mySubmodel + "\" )\n\n" );
	}
	/* profiled
	protected void init()
	{
		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\n\nNetworkDisplay.init( submodel \"" + mySubmodel + "\", " + myBeliefNetwork.getClass().getName() + " )" );

		long start = JVMTI.getCurrentThreadCpuTimeUnsafe();

		Icon iconFrame = MainToolBar.getIcon( STR_FILENAME_ICON );
		if( iconFrame != null ) setFrameIcon( iconFrame );

		initJDesktopPane();
		initStatusBar();
		initMouseHandler();

		long mid0 = JVMTI.getCurrentThreadCpuTimeUnsafe();
		initComponents();
		long mid1 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		initPopup();
		initKeyStrokes();
		revalidate();  //recalculate based on new preferred size
		//requestFocus();

		long end = JVMTI.getCurrentThreadCpuTimeUnsafe();
		long first = mid0 - start;
		long middl = mid1 - mid0;
		long last  = end  - mid1;
		double total = (double) (end - start);

		double firstFrac = ((double)first) / total;
		double middlFrac = ((double)middl) / total;
		double lastFrac = ((double)last) / total;

		Util.STREAM_TESTprintln( "NetworkDisplay.init()" );
		Util.STREAM_TESTprintln( "    head            : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(firstFrac) + " (" + NetworkIO.formatTime(first)
		              + "),\n    initComponents(): " + NetworkIO.FORMAT_PROFILE_PERCENT.format(middlFrac) + " (" + NetworkIO.formatTime(middl)
		              + "),\n    tail            : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(lastFrac) + " (" + NetworkIO.formatTime(last) + ")" );

		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "DONE NetworkDisplay.init( submodel \"" + mySubmodel + "\" )\n\n" );
	}*/

	/** @since 20021021 */
	public boolean isFocusTraversable()
	{
		return true;
	}

	/** @since 20021021 */
	protected void initKeyStrokes()
	{
		//deprecated method
		//registerKeyboardAction( action_CTRL_MINUS, CTRL_MINUS, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
		//registerKeyboardAction( action_CTRL_PLUS, CTRL_PLUS, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );

		/*
		//strategy #1
		InputMap IM = getInputMap();
		ActionMap AM = getActionMap();
		KeyStroke CTRL_MINUS = KeyStroke.getKeyStroke( KeyEvent.VK_MINUS, InputEvent.CTRL_MASK );
		KeyStroke CTRL_PLUS = KeyStroke.getKeyStroke( KeyEvent.VK_EQUALS, InputEvent.CTRL_MASK );
		Object actionKey_CTRL_MINUS = new Object();
		Object actionKey_CTRL_PLUS = new Object();
		IM.put( CTRL_MINUS, actionKey_CTRL_MINUS );
		IM.put( CTRL_PLUS, actionKey_CTRL_PLUS );
		Action action_CTRL_MINUS = new AbstractAction(){
			public void actionPerformed( ActionEvent e )
			{
				zoomOut();
			}
		};
		Action action_CTRL_PLUS = new AbstractAction(){
			public void actionPerformed( ActionEvent e )
			{
				zoomIn();
			}
		};
		AM.put( actionKey_CTRL_MINUS, action_CTRL_MINUS );
		AM.put( actionKey_CTRL_PLUS, action_CTRL_PLUS );*/

		//strategy #2
		UI ui = hnInternalFrame.getParentFrame();
		initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_EQUALS, InputEvent.CTRL_MASK ), ui.action_ZOOMIN );
		initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_MINUS, InputEvent.CTRL_MASK ), ui.action_ZOOMOUT );
		initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT, (int)0 ), ui.action_SELECTEDNODESRIGHT );
		initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_LEFT, (int)0 ), ui.action_SELECTEDNODESLEFT );
		initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_UP, (int)0 ), ui.action_SELECTEDNODESUP );
		initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_DOWN, (int)0 ), ui.action_SELECTEDNODESDOWN );
	}

	/**
		@author Keith Cascio
		@since 102402
	*/
	protected void initKeyStroke( KeyStroke stroke, Action action )
	{
		Object actionKey = new Object();
		getInputMap().put( stroke, actionKey );
		getActionMap().put( actionKey, action );
	}

	public static final boolean FLAG_DESKTOP_CONTAINS_LABELS = true;

	/** @since 20020422 */
	protected void initJDesktopPane()
	{
		myDesktopPane = new JPanel()//JDesktopPane()//java.awt.JDesktopStreamlined()//
		{
			/** Paint background & arrows.*/
			public void paintComponent( Graphics g )
			{
				super.paintComponent( g );//paint background

				Rectangle rect             = myJScrollPane.getViewport().getViewRect();
				boolean   hideedges        = false;
				boolean   hiderecoverables = false;
				boolean   hidehiddenedges  = false;

				try{
					if( NetworkDisplay.this.hnInternalFrame != null ){
						SamiamUserMode sum = NetworkDisplay.this.hnInternalFrame.getSamiamUserMode();
						hideedges          = ! sum.contains( SamiamUserMode.SHOWEDGES        );
						hiderecoverables   = ! sum.contains( SamiamUserMode.SHOWRECOVERABLES );
						hidehiddenedges    =   sum.contains( SamiamUserMode.HIDEHIDDENEDGES  );
					}
				}catch( Throwable thrown ){
					System.err.println( "warning: NetworkDisplay.myDesktopPane.paintComponent() caught " + thrown );
				}

				if( ! hideedges ){
					synchronized( mySynchronizationEdgeList ){
						for( Iterator itr = myEdgeList.iterator(); itr.hasNext();){
							((Arrow)itr.next()).paint( g, rect, hidehiddenedges );
						}
					}
				}

				if( ! hiderecoverables ){
					try{
						synchronized( mySynchronizationEdgeList ){
							for( Iterator itr = myListRecoverableArrows.iterator(); itr.hasNext();){
								((Arrow)itr.next()).paint( g, rect, hidehiddenedges );
							}
						}
					}catch( Throwable thrown ){
						System.err.println( "warning: NetworkDisplay.myDesktopPane.paintComponent() caught " + thrown );
					}
				}

				if( !FLAG_DESKTOP_CONTAINS_LABELS ){
					for( Iterator itr = myComponentList.iterator(); itr.hasNext();){
						((NetworkComponentLabel)itr.next()).paintNetworkComponent( g, rect );
					}
				}
			}
			//public void revalidate()
			//{
				//System.out.println( "myDesktopPane.revalidate()" );
			//	super.revalidate();
			//}
			//public void repaint()
			//{
				//System.out.println( "myDesktopPane.repaint()" );
			//	super.repaint();
			//}
		};

		//myDesktopPane.setDesktopManager( new VirtualDesktopManager() );

		//no layout manager, use absolute positioning
		myDesktopPane.setLayout(null);
		myDesktopPane.putClientProperty( "JDesktopPane.dragMode", "outline" );

		myJScrollPane = new JScrollPane( myDesktopPane );

		//myContentPane = new JPanel( new BorderLayout() );
		myContentPane = getContentPane();
		myContentPane.setLayout( new BorderLayout() );
		myContentPane.add( myJScrollPane, BorderLayout.CENTER );
		//setContentPane( new JScrollPane( myDesktopPane ) );
		//setContentPane( myContentPane );

		myMonitorSizeManager = new SizeManager( mySamiamPreferences );
	}

	protected JComponent myDesktopPane = null;
	protected Container myContentPane = null;
	protected JScrollPane myJScrollPane = null;

	/** @since 20060721 */
	protected void initGraphicsHelpers(){
		//myOpacityConsultant = new OpacityConsultant();
		//((OpacityConsultant)myOpacityConsultant).configure( NetworkDisplay.this );
		String className = null;
		try{
			if( METHOD_OPACITYCONSULTANT_INSTALLIFHELPFUL == null ){
				if( CLAZZ_OPACITYCONSULTANT == null ) CLAZZ_OPACITYCONSULTANT = Class.forName( className = "edu.ucla.belief.ui.networkdisplay.OpacityConsultant" );
				METHOD_OPACITYCONSULTANT_INSTALLIFHELPFUL = CLAZZ_OPACITYCONSULTANT.getMethod( "installIfHelpful", new Class[] { NetworkDisplay.class } );
			}
			//myOpacityConsultant = CLAZZ_OPACITYCONSULTANT.newInstance();
			//Method method = CLAZZ_OPACITYCONSULTANT.getMethod( "configure", new Class[] { NetworkDisplay.class } );
			//method.invoke( myOpacityConsultant, new Object[] { NetworkDisplay.this } );

			myOpacityConsultant = METHOD_OPACITYCONSULTANT_INSTALLIFHELPFUL.invoke( null, new Object[] { NetworkDisplay.this } );
		}catch( UnsupportedClassVersionError unsupportedclassversionerror ){
			System.err.println( "warning! optional network display graphics optimizations require java version 5 or higher. please update to the latest version JRE." );
		}catch( Throwable throwable ){
			System.err.println( "warning! could not instantiate " + className + ", " + throwable.getMessage() );
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				throwable.printStackTrace();
			}
		}
	}

	protected      Object myOpacityConsultant;
	private static Class  CLAZZ_OPACITYCONSULTANT;
	private static Method METHOD_OPACITYCONSULTANT_INSTALLIFHELPFUL;

	/**
		@author Keith Cascio
		@since 042202
	*/
	protected void initStatusBar()
	{
		myStatusBar = new StatusBar();
		//addComponentListener( myStatusBar );
		//add( myStatusBar );
		myContentPane.add( myStatusBar, BorderLayout.SOUTH );
	}

	/**
		@author Keith Cascio
		@since 092002
	*/
	protected Collection getSubmodelDisplays()
	{
		if( myCollectionSubmodelDisplays == null )
		{
			if( mySubmodel.getNumChildSubmodels() < (int)1 ) myCollectionSubmodelDisplays = Collections.EMPTY_SET;
			else{
				Collection ret = new LinkedList();
				DSLSubmodel tempSub = null;
				SubmodelLabel tempLabel = null;
				NetworkDisplay tempDisplay = null;
				for( Iterator it = mySubmodel.getChildSubmodels(); it.hasNext(); )
				{
					tempSub = (DSLSubmodel)it.next();
					if( tempSub.userobject instanceof SubmodelLabel )
					{
						tempLabel = (SubmodelLabel) tempSub.userobject;
						tempDisplay = tempLabel.getChildNetworkDisplay();
						if( tempDisplay != null ) ret.add( tempDisplay );
					}
				}
				myCollectionSubmodelDisplays = new ArrayList( ret );
			}
		}

		return myCollectionSubmodelDisplays;
	}

	/**
		@author Keith Cascio
		@since 042202
	*/
	protected void initMouseHandler()
	{
		//Mouse listener
		this.mouseSel = new NodeMouseSelector(this);
		myDesktopPane.addMouseListener( mouseSel);
		myDesktopPane.addMouseMotionListener( mouseSel);
	}

	/** @since 061304 */
	public void addDecorator( Decorator dec )
	{
		if( myListDecorators == null ) myListDecorators = new LinkedList();
		removeDecorator( dec );
		myListDecorators.addFirst( dec );
	}

	/** @since 061304 */
	public boolean removeDecorator( Decorator dec )
	{
		if( myListDecorators == null ) return false;
		else return myListDecorators.remove( dec );
	}

	/** @since 061304 */
	public boolean hasDecorators(){
		return (myListDecorators != null) && (!myListDecorators.isEmpty());
	}

	/** @since 20020422 */
	protected void initComponents()
	{
		Collection finiteVariables = myBeliefNetwork.getVariables( mySubmodel );

		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "NetworkDisplay.initComponents( " + finiteVariables + " )" );

		int numVariables = finiteVariables.size();

		mySelectedComponents = new HashSet( numVariables );
		myUnselectedComponents = new HashSet( numVariables );

		makeNodes( finiteVariables );
		if( Thread.currentThread().isInterrupted() ) return;

		makeSubmodels();

		sortComponents();

		//translate();
		if( myBeliefNetwork.isHuginNet() ) calculateHuginReflectAxis();
		calculateActualTranslationDelta();

		handleNodeSizePreferences( myNetPrefs, true );
		setZoomFactor( myZoomFactor );
		//recalculateDesktopSize();

		//Create all edges
		DirectedGraph edgeGraph = hnInternalFrame.getBeliefNetwork();
		makeVariableSourceEdges( finiteVariables, edgeGraph );
		makeRecoverableArrows(   finiteVariables, edgeGraph );
		makeSubmodelSourceEdges(                  edgeGraph );
	}
	/* profiled
	protected void initComponents()
	{
		ThreadGroup group  = edu.ucla.util.SystemGCThread.getThreadGroup();
		group.interrupt();
		while( group.activeCount() > 0 ) Thread.yield();

		long start = JVMTI.getCurrentThreadCpuTimeUnsafe();

		Collection finiteVariables = myBeliefNetwork.getVariables( mySubmodel );

		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "NetworkDisplay.initComponents( " + finiteVariables + " )" );

		int numVariables = finiteVariables.size();

		mySelectedComponents = new HashSet( numVariables );
		myUnselectedComponents = new HashSet( numVariables );

		long mid0 = JVMTI.getCurrentThreadCpuTimeUnsafe();
		makeNodes( finiteVariables );
		long mid1 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		makeSubmodels();

		sortComponents();

		//translate();
		if( myBeliefNetwork.isHuginNet() ) calculateHuginReflectAxis();
		calculateActualTranslationDelta();

		handleNodeSizePreferences( myNetPrefs, true );
		setZoomFactor( myZoomFactor );
		//recalculateDesktopSize();

		long mid2 = JVMTI.getCurrentThreadCpuTimeUnsafe();
		//Create all edges
		DirectedGraph edgeGraph = hnInternalFrame.getBeliefNetwork();
		makeVariableSourceEdges( finiteVariables, edgeGraph );
		makeRecoverableArrows(   finiteVariables, edgeGraph );
		makeSubmodelSourceEdges( edgeGraph );

		long end =  JVMTI.getCurrentThreadCpuTimeUnsafe();
		long first  = mid0 - start;
		long maknod = mid1 - mid0;
		long sort   = mid2 - mid1;
		long makegs = end  - mid2;
		double total = (double) (end - start);

		double firstFrac  = ((double)first) / total;
		double maknodFrac = ((double)maknod) / total;
		double sortFrac   = ((double)sort) / total;
		double makegsFrac = ((double)makegs) / total;

		Util.STREAM_TESTprintln( "NetworkDisplay.initComponents()" );
		Util.STREAM_TESTprintln( "    first           : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(firstFrac) + " (" + NetworkIO.formatTime(first)
		              + "),\n    makeNodes()     : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(maknodFrac) + " (" + NetworkIO.formatTime(maknod)
		              + "),\n    sortComponents(): " + NetworkIO.FORMAT_PROFILE_PERCENT.format(sortFrac) + " (" + NetworkIO.formatTime(sort)
		              + "),\n    make edges      : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(makegsFrac) + " (" + NetworkIO.formatTime(makegs) + ")" );

	}*/

	/**
		@author Keith Cascio
		@since 102102
	*/
	protected void calculateHuginReflectAxis()
	{
		myExtremeVirtualYCoodinate = (int)10;

		NetworkComponentLabel nl = null;
		for( Iterator it = myComponentList.iterator(); it.hasNext(); )
		{
			nl = (NetworkComponentLabel) it.next();
			nl.getVirtualLocation( theUtilPoint );
			if( theUtilPoint.y > myExtremeVirtualYCoodinate )
			{
				myExtremeVirtualYCoodinate = theUtilPoint.y;
				//System.out.println( "NetworkDisplay, new extreme virtual Y: " + myExtremeVirtualYCoodinate );
			}
		}
	}

	/**
		@author Keith Cascio
		@since 102302
	*/
	protected void calculateActualTranslationDelta()
	{
		//System.out.println( "calculateActualTranslationDelta()" );
		//System.out.println( "\told myActualTranslationDelta = " + myActualTranslationDelta );

		myActualTranslationDelta.setSize( 0, 0 );
		for( Iterator it = myComponentList.iterator(); it.hasNext(); )
		{
			updateActualTranslationDelta( (CoordinateVirtual) it.next() );
		}

		//scale( myActualTranslationDelta, myZoomFactorInverse );

		//System.out.println( "\tnew myActualTranslationDelta = " + myActualTranslationDelta );
	}

	/**
		@author Keith Cascio
		@since 102302
	*/
	protected void updateActualTranslationDelta( CoordinateVirtual cv )
	{
		//System.out.println( "updateActualTranslationDelta("+cv+")" );
		//cv.getVirtualLocation( theUtilPoint );
		//System.out.println( "\tvirtual: " + theUtilPoint );
		cv.recalculateActual();
		cv.getActualLocation( theUtilPoint );
		//System.out.println( "\tactual: " + theUtilPoint );
		updateActualTranslationDelta( theUtilPoint );
	}

	/**
		@author Keith Cascio
		@since 102302
	*/
	//protected boolean updateActualTranslationDelta( Point pActual, Dimension deltadelta )
	protected boolean updateActualTranslationDelta( Point pActual )
	{
		//System.out.println( "updateActualTranslationDelta("+pActual+")" );
		boolean ret = false;
		//deltadelta.width = 0;
		//deltadelta.height = 0;

		if( pActual.x < 0 )
		{
			ret = true;
			//deltadelta.width = -pActual.x;
			//myActualTranslationDelta.width += deltadelta.width;
			myActualTranslationDelta.width -= pActual.x;
			//System.out.println( "NetworkDisplay, new trans delta x: " + myActualTranslationDelta.width );
		}
		if( pActual.y < 0 )
		{
			ret = true;
			//deltadelta.height = -pActual.y;
			//myActualTranslationDelta.height += deltadelta.height;
			myActualTranslationDelta.height -= pActual.y;
			//System.out.println( "NetworkDisplay, new trans delta y: " + myActualTranslationDelta.height );
		}
		return ret;
	}



	/**
		@author Keith Cascio
		@since 102302

		@ret TRUE iff the "anchor" changed
	*/
	protected boolean checkBounds( Rectangle rectActual, Dimension deltadelta )
	{
		//System.out.println( "updateActualTranslationDelta("+rectActual+")" );

		boolean ret = false;
		deltadelta.width = 0;
		deltadelta.height = 0;
		boolean expanded = false;

		if( rectActual.x < 0 )
		{
			ret = true;
			expanded = true;
			deltadelta.width = -rectActual.x;
			myActualTranslationDelta.width += deltadelta.width;
			//myActualTranslationDelta.width -= rectActual.x;
			//System.out.println( "NetworkDisplay, new trans delta x: " + myActualTranslationDelta.width );
		}
		if( rectActual.y < 0 )
		{
			ret = true;
			expanded = true;
			deltadelta.height = -rectActual.y;
			myActualTranslationDelta.height += deltadelta.height;
			//myActualTranslationDelta.height -= rectActual.y;
			//System.out.println( "NetworkDisplay, new trans delta y: " + myActualTranslationDelta.height );
		}

		//if( !ret )
		//{
			//Dimension oldPrefSize = myDesktopPane.getPreferredSize();
			int newExtremeX = rectActual.x + rectActual.width;
			int newExtremeY = rectActual.y + rectActual.height;

			if( newExtremeX > mySizeDesktopPane.width )
			{
				expanded = true;
				mySizeDesktopPane.width = newExtremeX;
			}
			if( newExtremeY > mySizeDesktopPane.height )
			{
				expanded = true;
				mySizeDesktopPane.height = newExtremeY;
			}

			mySizeDesktopPane.width += deltadelta.width;
			mySizeDesktopPane.height += deltadelta.height;

			if( expanded ) resizeDesktop();
		//}

		return ret;
	}

	protected Dimension mySizeDesktopPane = new Dimension();

	/** @since 102302 */
	protected void resizeDesktop()
	{
		myDesktopPane.setPreferredSize( mySizeDesktopPane );
		myJScrollPane.revalidate();
		//System.out.println( "expanded desktop: " + mySizeDesktopPane );
	}

	/** @since 102302 */
	protected boolean updateActualTranslationDelta( Rectangle rect )
	{
		//System.out.println( "updateActualTranslationDelta("+rect+")" );
		boolean ret = false;
		if( rect.x < 0 )
		{
			ret = true;
			myActualTranslationDelta.width -= rect.x;
		}
		if( rect.y < 0 )
		{
			ret = true;
			myActualTranslationDelta.height -= rect.y;
		}
		return ret;
	}

	protected Dimension myDeltaDelta = new Dimension();
	protected Dimension myActualTranslationDelta = new Dimension();

	/** @since 042202 */
	/*protected void translate()
	{
		//Keith Cascio
		//042202
		//if( myIsDSL && myIsMainSubmodel )
		if( false )
		{
			//deal with DSL models specifying negative coordinates
			//This is necessary because we are not translating DSL networks
			//without this, the preferred size could be too small
			//resulting in some part of thr rightmost or bottommost
			//nodes not being displayed.
			Dimension dimDSL = netBounds.getSize();
			dimDSL.width += netBounds.x;
			dimDSL.height += netBounds.y;
			myDesktopPane.setPreferredSize( dimDSL );
		}
		else
		{
			//Translate node labels to screen coordinates so that the
			//upper left corner of the screen (0,0) is the upper left corner
			//of the network.
			//Keith Cascio
			//041802
			//this is what crams the network up into
			//the upper left corner.
			NetworkComponentLabel nl = null;
			Point p = new Point();
			for( Iterator itr = myComponentList.iterator(); itr.hasNext(); )
			{
				nl = (NetworkComponentLabel) itr.next();
				//nl.getActualLocation( p );
				//p.x -= netBounds.x;
				//p.y -= netBounds.y;
				//nl.setActualLocation( p );
				nl.translateActualLocation( -netBounds.x, -netBounds.y );
				nl.confirmActualLocation();
			}

			myDesktopPane.setPreferredSize( netBounds.getSize() );
		}
	}*/

	/** @since 042202 */
	protected void makeSubmodels()
	{
		DSLSubmodelFactory fact = myBeliefNetwork.getDSLSubmodelFactory();
		Iterator submodelIterator = mySubmodel.getChildSubmodels();
		DSLSubmodel tempDSLSubmodel = null;
		while( submodelIterator.hasNext() )
		{
			tempDSLSubmodel = (DSLSubmodel) submodelIterator.next();
			if( tempDSLSubmodel != fact.MAIN ) add( tempDSLSubmodel );
		}
	}

	/** create all nodes
		@since 20020422 */
	protected void makeNodes( Collection finiteVariables )
	{
		NodeLinearTask task = hnInternalFrame.getConstructionTask();

		boolean diligent = true;
		int size = finiteVariables.size();
		if( (size > 32) && (hnInternalFrame != null) ){
			Preference pref = hnInternalFrame.getPackageOptions().getMappedPreference( SamiamPreferences.lazyInitialization );
			diligent =  !( (pref != null) && ((Boolean) pref.getValue()).booleanValue() );
		}

		//System.out.println( diligent ? "diligent" : "lazy" );

		Collection/*<NodeLabel>*/ lazy = null;
		if( !diligent ) lazy = new ArrayList/*<NodeLabel>*/( size );

		DisplayableFiniteVariable dVar;
		NodeLabel nodelabel;
		for( Iterator itr = finiteVariables.iterator(); itr.hasNext(); )
		{
			dVar = (DisplayableFiniteVariable) itr.next();
			nodelabel = makeNode( dVar, diligent );
			if( lazy != null ) lazy.add( nodelabel );

			if( Thread.currentThread().isInterrupted() ) return;
			if( task != null ) task.touch();
		}

		if( lazy != null ) new RunLazyInitialization( lazy ).start();
	}

	//protected transient int myYCoodinateReflection = (int)10;

	/** @since 20020425 */
	protected NodeLabel makeNode( DisplayableFiniteVariable newDVar, boolean diligent )
	{
		NodeLabel label =
		        new NodeLabel(
				(DisplayableFiniteVariable) newDVar,
				//(NodeIcon) new NodeIconOval( newDVar, myNetPrefs ),
				/*(JDesktopPane)*/ myDesktopPane,
				(CoordinateTransformer) this,
				(SelectionListener) this,
				mySamiamPreferences,
				false,
				diligent );

		addLabel( label, diligent );
		newDVar.setNodeLabel( label );

		if( diligent ){
			makeNodeLazy( label );
			myUnselectedComponents.add( label );
		}

		return label;
	}

	/** @since 20060522 */
	private void makeNodeLazy( NetworkComponentLabel label ){
		label.recalculateActual();
	}

	/** @since 20060522 */
	private void initLazy( NetworkComponentLabel label ){
		label.initLazy();
		addLabelLazy( label.asJLabel() );
		makeNodeLazy( label );
	}

	/** @since 20060522 */
	public void dispose(){
		if( NetworkDisplay.this.myGroupLazy != null ) NetworkDisplay.this.myGroupLazy.interrupt();
	}

	/** @author keith cascio
		@since 20060522 */
	private class RunLazyInitialization implements Runnable{
		public RunLazyInitialization( Collection/*<NodeLabel>*/ lazy ){
			RunLazyInitialization.this.myLazy = lazy;
		}

		public void run(){
			if( (myLazy == null) || myLazy.isEmpty() ) return;

			Thread.yield();

			int size = myLazy.size();
			//long sleepytime = (long)(4096/size);
			//if( sleepytime < 1 ) sleepytime = 1;
			long sleepytime = 8;

			long start = System.currentTimeMillis();

			//System.out.println( "starting lazy initialization of " + size + " labels" );
			//System.out.println( "sleepytime? " + sleepytime );
			String msg = "finished";
			try{
				NodeLinearTask task = hnInternalFrame.getConstructionTask();
				if( task != null ){
					while( !task.isFinished() ) task.join( 0x2000 );
					Thread.sleep( 0x200 );
				}
				else Thread.sleep( (long)size + 0x400 );

				//for( NodeLabel nodelabel : myLazy ){
				for( Iterator it = myLazy.iterator(); it.hasNext(); ){
					NetworkDisplay.this.initLazy( (NodeLabel)it.next() );//nodelabel );
					Thread.sleep( sleepytime );
				}
			}catch( InterruptedException interruptedexception ){
				msg = "interrupted";
			}finally{
				long finish = System.currentTimeMillis();
				long elapsed = finish - start;

				//System.out.println( msg + " lazy initialization of " + size + " labels in " + elapsed + " ms" );
			}
		}

		public Thread start(){
			if( NetworkDisplay.this.myGroupLazy == null ){
				ThreadGroup parent = null;
				if( (hnInternalFrame != null) && (hnInternalFrame.getParentFrame() != null) && (hnInternalFrame.getParentFrame().getThreadGroup() != null) )
					parent = hnInternalFrame.getParentFrame().getThreadGroup().getParent();
				if( parent == null ) parent = Thread.currentThread().getThreadGroup();
				NetworkDisplay.this.myGroupLazy = new ThreadGroup( parent, "NetworkDisplay lazy initialization" );
			}
			Thread ret = new Thread( NetworkDisplay.this.myGroupLazy, RunLazyInitialization.this, "NetworkDisplay lazy initialization " + Integer.toString( INT_COUNTER++ ) );
			//ret.setPriority( Thread.MAX_PRIORITY );
			ret.start();
			return ret;
		}

		private Collection/*<NodeLabel>*/ myLazy;
	}
	private transient ThreadGroup myGroupLazy;
	private static    int         INT_COUNTER = 0;

	protected static Point theUtilPoint = new Point();
	protected transient int myExtremeVirtualYCoodinate = (int)10;

	/** @since 042202 */
	protected void makeSubmodelSourceEdges( DirectedGraph edgeGraph )
	{
		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "NetworkDisplay.makeSubmodelSourceEdges()" );
		DSLSubmodel submodel = null;
		Collection submodelvariables = null;
		DisplayableFiniteVariable dVar = null;
		NetworkComponentLabel tempStart = null;
		DSLSubmodelFactory fact = myBeliefNetwork.getDSLSubmodelFactory();
		for( Iterator it = mySubmodel.getChildSubmodels(); it.hasNext(); )
		{
			submodel = (DSLSubmodel) it.next();
			if( submodel != fact.MAIN )
			{
				tempStart = (SubmodelLabel) submodel.userobject;
				submodelvariables = myBeliefNetwork.getDeepVariables( submodel );
				for( Iterator varsIt = submodelvariables.iterator(); varsIt.hasNext(); )
				{
					dVar = (DisplayableFiniteVariable) varsIt.next();
					if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\tmaking submodel source edges for " + dVar.getID() + ", in submodel " + submodel );
					makeOutgoingEdges( tempStart, dVar, edgeGraph );
				}
			}
		}
		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "DONE NetworkDisplay.makeSubmodelSourceEdges()" );
	}

	/** @since 20080221 */
	protected int makeRecoverableArrows( Collection listNewDVars, DirectedGraph edgeGraph ){
		int count = 0;
		DisplayableFiniteVariable sink, source;
		NetworkComponentLabel     lblSource, lblSink;
		Map                       properties;
		String                    value;
		String[]                  split;
		try{
			for( Iterator itr   = listNewDVars.iterator();    itr.hasNext(); ){
				sink            = (DisplayableFiniteVariable) itr   .next();
				if( (properties = sink.getProperties()).containsKey( PropertySuperintendent.KEY_IDS_RECOVERABLE_PARENTS ) ){
				  //System.out.println( "making recoverable arrows for sink " + sink.getID() );
					value       = (String) properties.get( PropertySuperintendent.KEY_IDS_RECOVERABLE_PARENTS );
					if( value  == null ){ continue; }
					split = value.split( "," );
					for( int i=0; i<split.length; i++ ){
						if( (split[i] == null) || (split[i].length() < 1) ){ continue; }
						source      = (DisplayableFiniteVariable) myBeliefNetwork.forID( split[i] );
						if( source == null ){
							System.err.println( "source variable "+ split[i] +" not found when attempting to make recoverable arrow" );
							return count;
						}
						lblSink     = (NetworkComponentLabel)   sink.getNodeLabel();
						lblSource   = (NetworkComponentLabel) source.getNodeLabel();
						makeArrow( lblSource, lblSink, true );
						++count;
					}
				}
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: NetworkDisplay.makeRecoverableArrows() caught " + thrown );
			thrown.printStackTrace( System.err );
		}
		return count;
	}

	/** @since 20020422 */
	protected void makeVariableSourceEdges( Collection listNewDVars, DirectedGraph edgeGraph )
	{
		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "NetworkDisplay.makeVariableSourceEdges( " + listNewDVars.size() + " variables )" );
		DisplayableFiniteVariable dVar = null;
		NetworkComponentLabel lblSource = null;
		for( Iterator itr = listNewDVars.iterator(); itr.hasNext(); )
		{
			dVar = (DisplayableFiniteVariable)itr.next();
			if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\texamining " + dVar );
			lblSource = (NetworkComponentLabel) dVar.getNodeLabel();
			makeOutgoingEdges( lblSource, dVar, edgeGraph );
		}

		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "DONE NetworkDisplay.makeVariableSourceEdges()" );
	}

	/** @since 20020422 */
	protected void makeOutgoingEdges( NetworkComponentLabel lblSource, DisplayableFiniteVariable varSource, DirectedGraph edgeGraph )
	{
		Set outEdgeSet = edgeGraph.outGoing( varSource );
		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "NetworkDisplay.makeOutgoingEdges( " + varSource  + " ) -> " + outEdgeSet );

		DisplayableFiniteVariable dVar = null;
		NetworkComponentLabel lblSink = null;
		for( Iterator edgeItr = outEdgeSet.iterator(); edgeItr.hasNext();)
		{
			dVar = (DisplayableFiniteVariable) edgeItr.next();
			lblSink = findDesinationLabel( dVar );
			makeArrow( lblSource, lblSink, false );
		}
	}

	/** @since 20020422 */
	protected NetworkComponentLabel findDesinationLabel( DisplayableFiniteVariable dVar )
	{
		DSLSubmodel submodelDest = dVar.getDSLSubmodel();
		if( submodelDest == null || submodelDest == mySubmodel ) return (NetworkComponentLabel) dVar.getNodeLabel();		else if( submodelDest.getParent() == mySubmodel )
		{
			return (SubmodelLabel) submodelDest.userobject;
		}
		else
		{
			//HuginNet.isAnscestor() is expensive
			for( Iterator it = mySubmodel.getChildSubmodels(); it.hasNext(); )
			{
				submodelDest = (DSLSubmodel) it.next();
				if( myBeliefNetwork.isAnscestor( submodelDest, dVar ) ) return (SubmodelLabel) submodelDest.userobject;
			}

			return null;
		}
	}

	/** @since 20020422 */
	protected void makeArrow( NetworkComponentLabel source, NetworkComponentLabel termination, boolean recoverable )
	{
		if( DEBUG_VERBOSE ){
			String lsinkd = ( termination == null ) ? "null" : termination.getText();
			String lsrcd  = ( source      == null ) ? "null" :      source.getText();
			Util.STREAM_VERBOSE.println( "NetworkDisplay.makeArrow( " + lsrcd + " -> " + lsinkd + " )" );
		}

		if( termination == null || source == null ){ return; }

		final Arrow ar = new Arrow( source, termination, this, mySamiamPreferences );
		source      .addOutBoundEdge( ar );
		termination .addInComingEdge( ar );

		if( recoverable ){
			synchronized( mySynchronizationEdgeList ){
				myListRecoverableArrows.add( ar.setRecoverable( true ) );
			}
		}
		else{
			if( this.hasDecorators() ){
				for( Iterator it = myListDecorators.iterator(); it.hasNext(); ){
					((Decorator)it.next()).decorateArrow( ar, hnInternalFrame );
				}
			}
			synchronized( mySynchronizationEdgeList ){
				myEdgeList.add( ar );
			}
		}
	}

	/** @since 20020422 */
	protected void initPopup()
	{
		myPopup = new JPopupMenu();
		myPopup.add( myPopupLabel = new JLabel() );
		if( myNetPrefs != null )
			myPopupLabel.setForeground( (Color) mySamiamPreferences.getMappedPreference( SamiamPreferences.nodeTextClr ).getValue() );
		myPopup.add( Box.createVerticalStrut( 8 ) );
		myPopup.add( myShowNodePropertiesMenuItem = new JMenuItem( myActionNodeProperties ) );
		myPopup.add(     mySelectChildrenMenuItem = new JMenuItem( myActionSelectChildren ) );
		myPopup.add(        myShowMonitorMenuItem = new JMenuItem( myActionShowMonitor    ) );
		if( Troll.solicit().isTiger() ){ myPopup.add(              myActionShowCPTMonitor   ); }
		myPopup.addSeparator();
		myPopup.add( myGotoParent.getJMenu() );
		myPopup.add( myGotoChild.getJMenu()  );
		myPopup.addSeparator();
		//myPopup.add( new JLabel( "}" ) );
		myPopup.add( myDeleteNodeMenuItem = new JMenuItem( myActionDelete ) );
	}

	/** @since 20020419 */
	protected Component addLabel( JLabel comp, boolean diligent )
	{
		if( FLAG_DESKTOP_CONTAINS_LABELS ) myDesktopPane.add(   comp );
		myComponentList.add( comp );
		if( diligent ) addLabelLazy( comp );

		return comp;
	}

	/** @since 20060522 */
	private void addLabelLazy( JLabel comp ){
		comp.addMouseListener(       mouseSel );
		comp.addMouseMotionListener( mouseSel );
		Rectangle rect = new Rectangle( comp.getLocation(), new Dimension( comp.getWidth(), comp.getHeight() ) );
		comp.setBounds( rect );
		//netBounds.setBounds( (Rectangle)netBounds.createUnion(rect));
	}

	/** @since 041902 */
	protected void add( DSLSubmodel submodel )
	{
		SubmodelLabel SL = new SubmodelLabel(	submodel,
					hnInternalFrame,
					new NodeIconSquare( null ),//, mySamiamPreferences ),
					this, mySamiamPreferences );
		boolean diligent = true;
		addLabel( SL, diligent );
		submodel.userobject = SL;
	}

	/* ------------------
	* Call back functions for the mouse to use
	* ------------------ */

	public void locationSelectedByMouse( Point p )
	{
		if( myEditMode == EditMode.MODE_ADD_NODE )
		{
			//addNodeMode = false;
			myStatusBar.clearMessage();
			myEditMode = EditMode.MODE_DEFAULT;
			mouseSel.setMode( SelectionMode.MODE_DEFAULT );
			createNewNodeAt( p );
		}
		else if( myEditMode == EditMode.MODE_PROMPT_USER_POINT )
		{
			myStatusBar.clearMessage();
			myEditMode = EditMode.MODE_DEFAULT;
			mouseSel.setMode( SelectionMode.MODE_DEFAULT );
			myCurrentUserInputListener.handleUserActualPoint( p );
		}
	}

	/** @author keith cascio */
	public JPopupMenu nodeLabelPopup( NodeLabel nl )
	{
		myPopupLabel.setText( " " + nl.getFiniteVariable().toString() );// + " {" );
		myPopup.setSelected( null );
		//SamiamUserMode mode = this.hnInternalFrame.getSamiamUserMode();
		//myShowMonitorMenuItem.setEnabled( mode.contains( SamiamUserMode.QUERY ) && !mode.contains( SamiamUserMode.NEEDSCOMPILE ) );
		//myDeleteNodeMenuItem.setEnabled( mode.contains( SamiamUserMode.EDIT ) && !mode.contains( SamiamUserMode.SMILEFILE ) );

		DisplayableFiniteVariable dvar = nl.getFiniteVariable();
		myGotoParent.setContents( myBeliefNetwork.inComing( dvar ) );
		myGotoChild.setContents(  myBeliefNetwork.outGoing( dvar ) );

		myPopup.show( nl, nl.getWidth()/2, 0 );//myPopup.show( nl, nl.getWidth(), nl.getHeight() );
		return myPopup;
	}

	/** @since 20060530 */
	public class GotoSupport extends DynamicMenuSupport{
		public GotoSupport( String name ){
			super( name );
		}

		public void itemSelected( Object obj ){
			DisplayableFiniteVariable var = (DisplayableFiniteVariable) obj;
			var.getNodeLabel().setSelected( true );
			NetworkDisplay.this.ensureNodeIsVisible( var, false );
		}
	}

	protected JPopupMenu myPopup;
	private JLabel myPopupLabel;
	protected JMenuItem myDeleteNodeMenuItem, myShowNodePropertiesMenuItem, myShowMonitorMenuItem, mySelectChildrenMenuItem;
	private GotoSupport myGotoParent = new GotoSupport( "goto parent" ), myGotoChild = new GotoSupport( "goto child" );

	/**
		@author Keith Cascio
		@since 072902
	*/
	public void forceDefaultMode()
	{
		synchronized( mySynchronization )
		{

		//addNodeMode = false;
		//addEdgeMode = false;
		//deleteEdgeMode = false;
		if( myCurrentUserInputListener != null ){
			//myCurrentUserInputListener.userInputCancelled();
			myCurrentUserInputListener = (UserInputListener)null;
		}
		myEditMode = EditMode.MODE_DEFAULT;
		firstNodeLabelWhenSelecting = null;
		myStatusBar.clearMessage();

		}
	}

	/** @since 20071211 */
	public void nodeLabelSelectedByMouse( NodeLabel nl )
	{
		if( ! myEditMode.nodeselection ) return;

		if( firstNodeLabelWhenSelecting == null ){
			firstNodeLabelWhenSelecting = nl;
			myStatusBar.displayMessage(  myEditMode.getHalfwayMessage() );
		}
		else{
			myEditMode.finish( nl, this );
			mouseSel.setMode( SelectionMode.MODE_DEFAULT );
			forceDefaultMode();
		}
	}

	/* ------------------
	* Misc Helper Functions
	* ------------------ */

	/** Paint background & arrows.*/
	/*
	public void paintComponent(Graphics g){
		super.paintComponent( g );//paint background
		for( ListIterator itr = myEdgeList.listIterator(); itr.hasNext();) {
		((Arrow)itr.next()).paint( g );
		}
	}*/

	/** Determine if in a "special mode"*/
	protected boolean isNormalMode()
	{
		//return ( !addNodeMode && !addEdgeMode && !deleteEdgeMode);
		return (myEditMode == EditMode.MODE_DEFAULT);
	}

	/**
		Relatively expensive - so we prefer not to call this while,
		for example, dragging objects across the screen.

		Call after network size changes so that scroll bars are updated.
	*/
	public void recalculateDesktopSize()
	{
		theUtilRectangle.setBounds( 0, 0, 1, 1 );

		for( Iterator itr = myComponentList.iterator(); itr.hasNext(); )
		{
			theUtilRectangle.add( ((NetworkComponentLabel)itr.next()).getBoundsManaged( theUtilRectangle2 ) );
		}

		mySizeDesktopPane.setSize( theUtilRectangle.getSize() );
		myDesktopPane.setPreferredSize( mySizeDesktopPane );
		myJScrollPane.revalidate();
		myDesktopPane.repaint();
	}

	/** @since 021205 */
	public Dimension getNetworkSize( Dimension dim ){
		if( dim == null ) dim = new Dimension( mySizeDesktopPane );
		else dim.setSize( mySizeDesktopPane );
		return dim;
	}

	/** @since 060805 */
	public HandledModalAction myActionDelete = new HandledModalAction( "Delete", "Delete selected nodes", 'd', (Icon)null, ModeHandler.EDIT_MODE, false ){
		public void actionPerformed( ActionEvent evt ){
			Set toDelete = NetworkDisplay.this.getSelectedNodes( new TreeSet() );
			//if( toDelete.isEmpty() )
				toDelete.add( ((NodeLabel)myPopup.getInvoker()).getFiniteVariable() );
			deleteNodes( toDelete );
		}
	};

	/** @since 060805 */
	public HandledModalAction myActionShowMonitor = new HandledModalAction( "        Monitor", "Show monitor", 'm', (Icon)null, ModeHandler.QUERY_MODE, false ){
		public void actionPerformed( ActionEvent evt ){
			NodeLabel label = (NodeLabel) myPopup.getInvoker();
			try{
				label.setEvidenceDialogShown( true );
			}catch( Throwable thrown ){
				if( Util.DEBUG_VERBOSE ){
					Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
					thrown.printStackTrace( Util.STREAM_VERBOSE );
				}
				if( hnInternalFrame != null ){ hnInternalFrame.handleError( thrown ); }
				return;
			}
			if( testExceedsBounds( label ) ){ recalculateDesktopSize(); }
		}
	};

	/** @since 20080228 */
	public HandledModalAction myActionShowCPTMonitor = new HandledModalAction( "        CPT monitor", "Show cpt monitor", 't', (Icon)null, ModeHandler.OPEN_NETWORK, false ){
		public void actionPerformed( ActionEvent evt ){
			NodeLabel label = (NodeLabel) myPopup.getInvoker();
			setCPTMonitorShown( label, true );
		  //if( testExceedsBounds( label ) ){ recalculateDesktopSize(); }
		}
	};

	/** @since 060805 */
	public SamiamAction myActionSelectChildren = new SamiamAction( "        Select Children", "Select all direct child nodes", 'c', (Icon)null ){
		public void actionPerformed( ActionEvent evt ){
			NodeLabel label = (NodeLabel)myPopup.getInvoker();
			Set outgoing = myBeliefNetwork.outGoing( label.getFiniteVariable() );
			select( outgoing );
		}
	};

	/** @since 060805 */
	public HandledModalAction myActionNodeProperties = new HandledModalAction( "        Properties", "Show node properties dialog", 'p', (Icon)null, ModeHandler.OPENBUTNOTCOMPILING, false ){
		public void actionPerformed( ActionEvent evt ){
			((NodeLabel)myPopup.getInvoker()).showNodePropertiesDialog();
		}
	};

	/** interface ActionListener
		@author Keith Cascio */
	//public void actionPerformed( ActionEvent evt ){}

	/**
		@author Keith Cascio
		@since 031003
	*/
	public boolean testExceedsBounds( NetworkComponentLabel label )
	{
		theUtilRectangle.setLocation( 0,0 );
		theUtilRectangle.setSize( myDesktopPane.getSize() );
		return !theUtilRectangle.contains( label.getBoundsManaged( theUtilRectangle2 ) );
	}

	/* ------------------
	* Functions dealing with node selection
	* ------------------ */

	/**
	* Returns a newly created Set of FiniteVariable objects which are
	* currently selected.
	* The NetworkDisplay does not retain a pointer to the set, so any
	* modifications made to the set will not affect the NetworkDisplay
	* and future changes will not be made to the returned Set.
	*/
	public Set getSelectedNodes( Set ret )
	{
		if( ret == null ) ret = new HashSet();
		else ret.clear();
		Object obj = null;
		for( ListIterator itr = myComponentList.listIterator(); itr.hasNext();)
		{
			obj = itr.next();
			if( obj instanceof NodeLabel )
			{
				NodeLabel lbl = (NodeLabel)obj;
				if( lbl.isSelected() ) ret.add( lbl.getFiniteVariable() );
			}
		}

		return ret;
	}

	protected Set mySelectedDVars = null;

	/**
		@author Keith Cascio
		@since 102302
	*/
	//protected Set getSelectedComponents( Set ret, Rectangle boundsActual )
	protected void sortComponents()
	{
		//if( ret == null ) ret = new HashSet();
		//else ret.clear();

		mySelectedComponents.clear();
		myUnselectedComponents.clear();

		theUtilRectangle.setBounds( 0,0,0,0 );
		NetworkComponentLabel label = null;
		Iterator itr = myComponentList.iterator();

		while( itr.hasNext() )
		{
			label = (NetworkComponentLabel) itr.next();
			if( label.isSelected() )
			{
				//ret.add( label );
				mySelectedComponents.add( label );
				theUtilRectangle.setBounds( label.getBounds( theUtilRectangle2 ) );
				break;
			}
			else myUnselectedComponents.add( label );
		}

		while( itr.hasNext() )
		{
			label = (NetworkComponentLabel) itr.next();
			if( label.isSelected() )
			{
				//ret.add( label );
				mySelectedComponents.add( label );
				theUtilRectangle.add( label.getBounds( theUtilRectangle2 ) );
			}
			else myUnselectedComponents.add( label );
		}

		myActualSelectedBounds.setBounds( theUtilRectangle );
		myFlagActualSelectedBoundsValid = true;

		//return ret;
	}

	/**
		interface SelectionListener
		@since 103002
	*/
	public void selectionChanged( NetworkComponentLabel label )
	{
		if( label.isSelected() )
		{
			myUnselectedComponents.remove( label );
			if( mySelectedComponents.isEmpty() ) label.getBounds( myActualSelectedBounds );
			else myActualSelectedBounds.add( label.getBounds( theUtilRectangle2 ) );
			mySelectedComponents.add( label );
		}
		else
		{
			myFlagActualSelectedBoundsValid = false;
			mySelectedComponents.remove( label );
			myUnselectedComponents.add( label );
		}

		fireSelectionChanged( label );
	}

	/**
		interface SelectionListener
		@since 011504
	*/
	public void selectionReset(){
		throw new UnsupportedOperationException();
		//fireSelectionReset();
	}

	/** @since 060903 */
	private void fireSelectionReset()
	{
		if( mySelectionListeners != null ){
			SelectionListener next;
			for( ListIterator it = mySelectionListeners.listIterator(); it.hasNext(); ){
				next = (SelectionListener)it.next();
				if( next == null ) it.remove();
				else next.selectionReset();
			}
		}
	}

	/** @since 060903 */
	private void fireSelectionChanged( NetworkComponentLabel label )
	{
		if( mySelectionListeners != null ){
			SelectionListener next;
			for( ListIterator it = mySelectionListeners.listIterator(); it.hasNext(); ){
				next = (SelectionListener)it.next();
				if( next == null ) it.remove();
				else next.selectionChanged( label );
			}
		}
	}

	/** @since 060903 */
	public void addSelectionListener( SelectionListener listener )
	{
		if( mySelectionListeners == null ) mySelectionListeners = new WeakLinkedList();
		else if( mySelectionListeners.contains( listener ) ) return;
		mySelectionListeners.add( listener );
	}

	/** @since 060903 */
	public void removeSelectionListener( SelectionListener listener )
	{
		if( mySelectionListeners != null ) mySelectionListeners.remove( listener );
	}

	private transient WeakLinkedList mySelectionListeners;

	/**
		@author Keith Cascio
		@since 103002
	*/
	public void validateSelectedBounds()
	{
		if( !myFlagActualSelectedBoundsValid )
		{
			theUtilRectangle.setBounds( 0,0,0,0 );
			NetworkComponentLabel label = null;
			Iterator itr = mySelectedComponents.iterator();

			if( itr.hasNext() )
			{
				label = (NetworkComponentLabel) itr.next();
				theUtilRectangle.setBounds( label.getBounds( theUtilRectangle2 ) );
			}

			while( itr.hasNext() )
			{
				label = (NetworkComponentLabel) itr.next();
				theUtilRectangle.add( label.getBounds( theUtilRectangle2 ) );
			}

			myActualSelectedBounds.setBounds( theUtilRectangle );
			myFlagActualSelectedBoundsValid = true;
		}
	}

	protected Rectangle theUtilRectangle = new Rectangle();
	protected Rectangle theUtilRectangle2 = new Rectangle();
	protected Set mySelectedComponents;// = new HashSet();
	protected Set myUnselectedComponents;// = new HashSet();
	protected Rectangle myActualSelectedBounds = new Rectangle();
	protected boolean myFlagActualSelectedBoundsValid = false;

	/**
		@author Keith Cascio
		@since 102302
	*/
	/*
	protected Point getActualUpperLeft( Set components, Point upperleft )
	{
		if( upperleft == null ) upperleft = new Point();

		upperleft.x = Integer.MAX_VALUE;
		upperleft.y = Integer.MAX_VALUE;

		NetworkComponentLabel label = null;
		for( Iterator it = components.iterator(); it.hasNext(); )
		{
			label = (NetworkComponentLabel) it.next();
			label.getActualLocation( theUtilPoint );
			if( theUtilPoint.x < upperleft.x ) upperleft.x = theUtilPoint.x;
			if( theUtilPoint.y < upperleft.y ) upperleft.y = theUtilPoint.y;
		}

		return upperleft;
	}
	*/

	/** @since 062304 */
	public void select( Collection variables )
	{
		for( Iterator it = variables.iterator(); it.hasNext(); ){
			((DisplayableFiniteVariable)it.next()).getNodeLabel().setSelected( true );
		}
	}

	/** @since 062304 */
	public Collection getSelectedVariables()
	{
		HashSet ret = new HashSet( myBeliefNetwork.size() >> 1 );
		NetworkComponentLabel label;
		for( Iterator itr = mySelectedComponents.iterator(); itr.hasNext(); ){
			label = ((NetworkComponentLabel)itr.next());
			if( label.getFiniteVariable() != null ) ret.add( label.getFiniteVariable() );
		}
		return ret;
	}

	/**
	* Removes the node selection on all nodes.
	*/
	public void nodeSelectionClearAll()
	{
		for( ListIterator itr = myComponentList.listIterator(); itr.hasNext();)
		{
			NetworkComponentLabel node = ((NetworkComponentLabel)itr.next());
			node.setSelected( false );
		}
		myDesktopPane.repaint();
	}

	/**
		@author Keith Cascio
		@since 082102
	*/
	public void selectAll()
	{
		NetworkComponentLabel node = null;
		for( Iterator itr = myComponentList.iterator(); itr.hasNext(); )
		{
			node = ((NetworkComponentLabel)itr.next());
			node.setSelected( true );
		}

		myDesktopPane.repaint();
	}

	/**
	* Adds nodes to the "set" of selected nodes based on the rectangle passed in.
	*/
	public void nodeSelectionAdd( Rectangle rect)
	{
		Rectangle labelRect = new Rectangle();

		for( ListIterator itr = myComponentList.listIterator(); itr.hasNext();)
		{
			NetworkComponentLabel node = (NetworkComponentLabel)itr.next();
			labelRect = node.getBounds( labelRect);
			if( rect.intersects( labelRect))
			{
				node.setSelected( true );
			}
		}

		myDesktopPane.repaint();
	}

	/**
	* Inverts the selection status of nodes based on the rectangle passed in.
	*/
	public void nodeSelectionInvert( Rectangle rect)
	{
		Rectangle labelRect = new Rectangle();

		for( ListIterator itr = myComponentList.listIterator(); itr.hasNext();)
		{
			NetworkComponentLabel node = (NetworkComponentLabel)itr.next();
			labelRect = node.getBounds( labelRect);
			if( rect.intersects( labelRect))
			{
				node.selectionSwitch();
			}
		}

		myDesktopPane.repaint();
	}

	/**
	* Unselects all nodes and then selects nodes based on the rectangle passed in.
	*/
	public void nodeSelectionClearAndAdd( Rectangle rect)
	{
		nodeSelectionClearAll();
		nodeSelectionAdd( rect);
	}

	/* ------------------
	* Functions dealing with node movement
	* ------------------ */

	/** @since 20060721 */
	public interface ComponentMovementListener{
		public void componentsMoved( Set/*<Component>*/ components );
	}

	/** @since 20060721 */
	public void addComponentMovementListener( ComponentMovementListener listener ){
		if( listener                == null ) return;
		//if( myNodeMovementEvent     == null ) myNodeMovementEvent     = new ChangeEvent( NetworkDisplay.this );
		if( myComponentMovementListeners == null ) myComponentMovementListeners = new ComponentMovementListener[] { listener };
		else{
			ArrayList/*<ComponentMovementListener>*/ list = new ArrayList/*<ComponentMovementListener>*/( myComponentMovementListeners.length + 1 );
			list.add( listener );
			ComponentMovementListener changelistener;
			//for( ComponentMovementListener changelistener : myComponentMovementListeners )
			for( int i=0; i<myComponentMovementListeners.length; i++ ){
				changelistener = myComponentMovementListeners[i];
				if( changelistener != null ) list.add( changelistener );
			}
			myComponentMovementListeners = (ComponentMovementListener[]) list.toArray( new ComponentMovementListener[ list.size() ] );
		}
	}

	/** @since 20060721 */
	public boolean removeComponentMovementListener( ComponentMovementListener listener ){
		if( myComponentMovementListeners == null ) return false;
		List/*<ComponentMovementListener>*/ list = Arrays.asList( myComponentMovementListeners );
		boolean ret = list.remove( listener );
		if( list.isEmpty() ) myComponentMovementListeners = null;
		else                 myComponentMovementListeners = (ComponentMovementListener[]) list.toArray( new ComponentMovementListener[ list.size() ] );
		return ret;
	}

	/** @since 20060721 */
	protected void fireNodeMovement( Set/*<Component>*/ components ){
		if( myComponentMovementListeners == null ) return;
		//for( ComponentMovementListener changelistener : myComponentMovementListeners ) changelistener.componentsMoved( components );//myNodeMovementEvent );
		for( int i=0; i<myComponentMovementListeners.length; i++ ) myComponentMovementListeners[i].componentsMoved( components );
	}

	private ComponentMovementListener[] myComponentMovementListeners;

	/** This function will cause all selected nodes to be transposed by the values specified in x and y. */
	protected void moveSelectedNodes( int deltaX, int deltaY )
	{
		if( myLocationsFrozen ){ return; }//20080529
		//System.out.print( "moveSelectedNodes("+deltaX+","+deltaY+")" );

		Set/*<Component>*/ moved = null;

		myActualSelectedBounds.translate( deltaX, deltaY );
		if( checkBounds( myActualSelectedBounds, myDeltaDelta ) )
		{
			//confirmActualLocations( mySelectedComponents );
			//fireRecalculateActual();
			recalculateActualLocations( moved = myUnselectedComponents );
			myActualSelectedBounds.translate( myDeltaDelta.width, myDeltaDelta.height );
		}
		else
		{
			translateActualLocations( moved = mySelectedComponents, deltaX, deltaY );
			myDesktopPane.repaint();
			//myJScrollPane.repaint();
		}
		if( moved != null ) fireNodeMovement( moved );
	}

	/** @since 20021030 */
	public void recalculateActualLocations( Collection CVs )
	{
		//System.out.println( "NetworkDisplay.recalculateActualLocations()" );

		for( Iterator itr = CVs.iterator(); itr.hasNext(); )
		{
			((CoordinateVirtual)itr.next()).recalculateActual();
		}
	}

	/** @since 20021018 */
	public void confirmActualLocations( Collection CVs )
	{
		//System.out.println( "NetworkDisplay.confirmActualLocations()" );

		for( Iterator itr = CVs.iterator(); itr.hasNext(); )
		{
			((CoordinateVirtual)itr.next()).confirmActualLocation();
		}
	}

	/** @since 20021023 */
	public void translateActualLocations( Collection CVs, int deltaX, int deltaY )
	{
		CoordinateVirtual cv = null;
		for( Iterator itr = CVs.iterator(); itr.hasNext(); )
		{
			cv = (CoordinateVirtual) itr.next();
			cv.getActualLocation( theUtilPoint );
			theUtilPoint.translate( deltaX, deltaY );
			cv.setActualLocation( theUtilPoint );
			//cv.translateActualLocation( deltaX, deltaY );
		}
	}

	/**
	* This function will cause all nodes to be transposed
	* by the values specified in x and y.
	*/
	public void moveAllNodes( int x, int y )
	{
		Point pt = new Point();
		NetworkComponentLabel lbl = null;
		for( ListIterator itr = myComponentList.listIterator(); itr.hasNext();)
		{
			lbl = (NetworkComponentLabel)itr.next();
			lbl.translateActualLocation( x, y );
		}

		fireNodeMovement( null );

		// no good recalculateActuals( true, false );
		recalculateDesktopSize();
		repaint();
	}

	/**
	* Draw a rectange where newRect is located and "cover" the oldRect.
	*/
	protected void mouseDrag( Rectangle oldRect, Rectangle newRect )
	{
		Graphics g = myDesktopPane.getGraphics();
		g.setColor( Color.black);
		g.setXORMode( Color.white);
		if( oldRect != null)
		g.drawRect( oldRect.x, oldRect.y, oldRect.width, oldRect.height);
		if( newRect != null)
		g.drawRect( newRect.x, newRect.y, newRect.width, newRect.height);
	}

	/* ------------------
	* Option changes
	* ------------------ */

	/** Allow options to change.*/
	public void changePackageOptions( SamiamPreferences sPrefs )
	{
		//this.myNetPrefs = netPrefs;
		this.mySamiamPreferences = sPrefs;
		this.myNetPrefs = sPrefs.getPreferenceGroup( SamiamPreferences.NetDspNme );
		this.myMonitorPrefs = sPrefs.getPreferenceGroup( SamiamPreferences.MonitorsDspNme );
		PreferenceGroup globalPrefs = sPrefs.getPreferenceGroup( SamiamPreferences.PkgDspNme );

		boolean flagNetOrMonitorPrefsRecentlyCommitted = myNetPrefs.isRecentlyCommittedValue() || myMonitorPrefs.isRecentlyCommittedValue();
		boolean flagSpecialNodeLabelOverride = NodeLabel.isPreferenceStaticRecentlyCommitted( sPrefs );

		if( !( flagNetOrMonitorPrefsRecentlyCommitted || flagSpecialNodeLabelOverride ) ){
			//System.out.println( "NetworkDisplay.changePackageOptions() nothing to do" );
			return;
		}

		if( flagNetOrMonitorPrefsRecentlyCommitted )
		{
		if( myPopupLabel != null ) myPopupLabel.setForeground( (Color) sPrefs.getMappedPreference( SamiamPreferences.nodeTextClr ).getValue() );
		handleNodeSizePreferences( myNetPrefs, false );
		setSamiamUserMode( hnInternalFrame.getSamiamUserMode() );
		myMonitorSizeManager.updatePreferences( mySamiamPreferences );
		}

		//update labels
		for( Iterator itr = myComponentList.iterator(); itr.hasNext(); )
		{
			NetworkComponentLabel lbl = (NetworkComponentLabel)itr.next();
			lbl.changePackageOptions();
		}

		if( flagNetOrMonitorPrefsRecentlyCommitted )
		{
		//update edges
		synchronized( mySynchronizationEdgeList ){
			for( Iterator itr = myEdgeList.iterator(); itr.hasNext(); ){
				((Arrow)itr.next()).changePackageOptions();
			}
		}

		SubmodelLabel smlabel = null;
		DSLSubmodel model = null;
		for( Iterator it = mySubmodel.getChildSubmodels(); it.hasNext(); )
		{
			model = (DSLSubmodel) it.next();
			if( model.userobject instanceof SubmodelLabel )
			{
				smlabel = (SubmodelLabel) model.userobject;
				smlabel.getChildNetworkDisplay().changePackageOptions( sPrefs );
			}
		}
		}

		myDesktopPane.repaint();
	}

	protected Dimension myPreferenceNodeSize = new Dimension();
	protected boolean myFlagFileOverridesPref = false;

	/** @since 20021031 */
	protected void handleNodeSizePreferences( PreferenceGroup netPrefs, boolean force )
	{
		if( netPrefs.isRecentlyCommittedValue() || force )
		{
			boolean changed = false;

			Preference prefSize     = mySamiamPreferences.getMappedPreference( SamiamPreferences.nodeDefaultSize       );
			Preference prefOverride = mySamiamPreferences.getMappedPreference( SamiamPreferences.netSizeOverRidesPrefs );

			try{
				Map  bnProps  = myBeliefNetwork.getProperties();
				if( (bnProps != null) && (bnProps.get( PropertySuperintendent.KEY_HUGIN_NODE_SIZE ) == null) ){
					Dimension dimGlobal = myBeliefNetwork.getGlobalNodeSize( new Dimension() );
					if( (dimGlobal == null) || (dimGlobal.width < 1) || (dimGlobal.height < 1) ){
						Dimension dim = (Dimension) prefSize.getValue();
						bnProps.put( PropertySuperintendent.KEY_HUGIN_NODE_SIZE, Arrays.asList( new Integer[]{ new Integer( dim.width ), new Integer( dim.height ) } ) );
					}
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: NetworkDisplay.handleNodeSizePreferences() caught " + thrown );
			}

			if( force ||     prefSize.isRecentlyCommittedValue() ){
				changed = true;
				Dimension dim = (Dimension) prefSize.getValue();
				myPreferenceNodeSize.setSize( dim );
			}
			if( force || prefOverride.isRecentlyCommittedValue() ){
				changed = true;
				Boolean bool = (Boolean) prefOverride.getValue();
				myFlagFileOverridesPref = bool.booleanValue();
			}

			if( changed ){ resizeAll( null ); }
		}
	}

	public static final Dimension DIM_ZERO = new Dimension();

	/** @since 20021031 */
	protected int resizeAll( Collection variables ){
		int count = 0;
		if( myFlagFileOverridesPref ){
			Dimension dimGlobal = myBeliefNetwork.getGlobalNodeSize( null );
			if( dimGlobal.equals( DIM_ZERO ) ){ dimGlobal = myPreferenceNodeSize; }

			NodeLabel                 nodelabel = null;
			DisplayableFiniteVariable      dVar = null;
			Dimension                   dimNode = new Dimension();
			Collection                   target = variables == null ? myBeliefNetwork.getVariables( mySubmodel ) : variables;
			for( Iterator it = target.iterator(); it.hasNext(); ){
				if( (nodelabel = (dVar = (DisplayableFiniteVariable) it.next()).getNodeLabel()) != null ){
					++count;
					if( dVar.getDimension( dimNode ).equals( DIM_ZERO ) ){ dimNode.setSize( dimGlobal ); }
					nodelabel.setVirtualSize( dimNode );
				}
			}
			DSLSubmodel                   model = null;
			if( variables == null ){
				for( Iterator it = mySubmodel.getChildSubmodels(); it.hasNext(); ){
					if( (model = (DSLSubmodel) it.next()).userobject instanceof CoordinateVirtual ){
						((CoordinateVirtual) model.userobject).setVirtualSize( myPreferenceNodeSize );
					}
				}
			}
		}
		else{
			if( variables == null ){
				for( Iterator it = myComponentList.iterator(); it.hasNext(); ){
					++count;
					((CoordinateVirtual) it.next()).setVirtualSize( myPreferenceNodeSize );
				}
			}else{
				for( Iterator it = variables.iterator(); it.hasNext(); ){
					++count;
					((DisplayableFiniteVariable) it.next()).getNodeLabel().setVirtualSize( myPreferenceNodeSize );
				}
			}
		}

		recalculateDesktopSize();
		return count;
	}

	/* ------------------
	* Functions to change network Structure
	* ------------------ */

	/**
		This function will delete the nodes in the Set.
		The Set should be a set of FiniteVariable objects.

		@author Keith Cascio
		@since 090502
	*/
	public void deleteNodes( Set nodes ){
		this.deleteNodes( nodes, /*force*/false );
	}

	/** @since 021405 Valentine's Day! */
	public void deleteNodes( Set nodes, boolean force )
	{
		if( force ){
			deleteNodesImpl( nodes );
			return;
		}

		int result = JOptionPane.showConfirmDialog( null,
			"Are you sure you want to delete these " +
			nodes.size() + " nodes?", "Confirm Delete",
			JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );

		if( result == JOptionPane.YES_OPTION ) deleteNodesImpl( nodes );
	}

	/** @since 20050214 Valentine's Day! */
	private void deleteNodesImpl( Set nodes )
	{
		DisplayableBeliefNetwork bn = hnInternalFrame.getBeliefNetwork();
		for( Iterator itr = nodes.iterator(); itr.hasNext(); ){
			try{
				bn.removeVariable( (DisplayableFiniteVariable) itr.next() );
			}catch( Throwable thrown ){
				hnInternalFrame.getParentFrame().showMessageDialog( thrown.getMessage(), "Error removing variable", JOptionPane.ERROR_MESSAGE );
			}
		}

		//notify all others of change  (recompile, update tree, ...)
		hnInternalFrame.netStructureChanged( new NetStructureEvent( NetStructureEvent.NODES_REMOVED, nodes));
	}

	/** @since 20071211 */
	protected NetworkDisplay copyCPT( DisplayableFiniteVariable source, DisplayableFiniteVariable destination ){
		Troll.solicit().probabilityRewrite( new ProbabilityRewriteArgs( source, destination, hnInternalFrame ) );
		return this;
	}

	protected void deleteEdge( DisplayableFiniteVariable st, DisplayableFiniteVariable en)
	{
		BeliefNetwork bn = hnInternalFrame.getBeliefNetwork();
		HuginNet hn = myBeliefNetwork;

		//if edge exists
		if( bn.containsEdge( st, en) ){
			try{
				bn.removeEdge(st,en);
				//notify all others of change  (recompile, update tree, ...)
				hnInternalFrame.netStructureChanged( new NetStructureEvent( NetStructureEvent.EDGE_REMOVED, null ) );
			}catch( Throwable thrown ){
				hnInternalFrame.getParentFrame().showMessageDialog( thrown.getMessage(), "Error removing edge", JOptionPane.ERROR_MESSAGE );
			}
		}
	}

	/** @since 20080219 */
	protected Thread replaceEdge( DisplayableFiniteVariable st, DisplayableFiniteVariable en ){
		return Troll.solicit().replaceEdge( new ProbabilityRewriteArgs( st, en, this.hnInternalFrame ) );
	}

	/** @since 20080221 */
	protected Thread recoverEdge( DisplayableFiniteVariable st, DisplayableFiniteVariable en ){
		return Troll.solicit().recoverEdge( new ProbabilityRewriteArgs( st, en, this.hnInternalFrame ) );
	}

	/** @since 080602 */
	public void addEdge( DisplayableFiniteVariable st, DisplayableFiniteVariable en )
	{
		BeliefNetwork bn = hnInternalFrame.getBeliefNetwork();
		DirectedGraph graph = bn;
		HuginNet hn = myBeliefNetwork;

		String errmsg = null;
		if( st == en )
		{
		}
		else if(   graph.containsEdge(        st, en ) ){
			errmsg = "Graph already contains an edge between " + st + " and " + en + ".";
		}
		else if( ! graph.maintainsAcyclicity( st, en ) ){
			errmsg = "Cannot add edge between " + st + " and " + en + ": would create a cycle.";
		}
		else{
			try{
				//if not exist yet & not loop to itself
				bn.addEdge( st, en );
				//notify all others of change  (recompile, update tree, ...)
				hnInternalFrame.netStructureChanged( new NetStructureEvent( NetStructureEvent.EDGE_ADDED, null ) );
			}catch( Throwable thrown ){ errmsg = thrown.getMessage(); if( errmsg == null ){ errmsg = thrown.toString(); } }
		}

		if( errmsg != null ) hnInternalFrame.getParentFrame().showMessageDialog( errmsg, "Error adding edge", JOptionPane.ERROR_MESSAGE );
	}

	/** @since 070102 */
	protected static boolean DEBUG_COORDINATES = false;

	public static final String STR_NEW_VARIABLE_ID = "variable";

	/** @since 021405 Valentine's Day! */
	public DisplayableFiniteVariable createNewNode( Point pActual, String id, String label )
	{
		//if( DEBUG_COORDINATES )

		Point pVirtual = new Point( pActual );
		actualToVirtual( pVirtual );
		//translate( p );

		//System.out.println( "\n\nJava NetworkDisplay adding node at actual: (" + pActual + "), virtual: (" + pVirtual + ")"  );

		ArrayList loc = new ArrayList(2);
		loc.add( new Integer( pVirtual.x ) );
		loc.add( new Integer( pVirtual.y ) );

		if( DEBUG_COORDINATES ) Util.STREAM_VERBOSE.println();

		List statelist = Arrays.asList( new String[]{ "state0", "state1" } );

		//add extra params to hugin node
		Map values = new HashMap();

		values.put( PropertySuperintendent.KEY_HUGIN_ID,       id         );
		values.put( PropertySuperintendent.KEY_HUGIN_STATES,   statelist  );
		values.put( PropertySuperintendent.KEY_HUGIN_LABEL,    label      );
		values.put( PropertySuperintendent.KEY_HUGIN_POSITION, loc        );
		values.put(           DSLConstants.KEY_SUBMODEL,       mySubmodel );

	  //HuginNode hNode = new HuginNodeImpl( id, statelist, values );
	  //DisplayableFiniteVariable dVar = new DisplayableFiniteVariableImpl( hNode, hnInternalFrame );
		DisplayableFiniteVariable dVar = (DisplayableFiniteVariable) myBeliefNetwork.newFiniteVariable( values );
	  //dVar.setDSLSubmodel( mySubmodel );

		return dVar;
	}

	/** @since 20050214 Valentine's Day! */
	public void addNewNode( DisplayableFiniteVariable dVar ){
		//add variables to networks
		myBeliefNetwork.addVariable( dVar, true );

		//notify all others of change  (recompile, update tree, ...)
		hnInternalFrame.netStructureChanged( new NetStructureEvent( NetStructureEvent.NODES_ADDED, Collections.singleton( dVar ) ) );
	}

	/** @since 20021015 */
	protected void createNewNodeAt( Point pActual )
	{
		//if( DEBUG_COORDINATES )

		Point pVirtual = new Point( pActual );
		actualToVirtual( pVirtual );
		//translate( p );

		//System.out.println( "\n\nJava NetworkDisplay adding node at actual: (" + pActual + "), virtual: (" + pVirtual + ")"  );

		ArrayList loc = new ArrayList(2);
		loc.add( new Integer( pVirtual.x ) );
		loc.add( new Integer( pVirtual.y ) );

		if( DEBUG_COORDINATES ) Util.STREAM_VERBOSE.println();

		List statelist = Arrays.asList( new String[]{ "state0", "state1" } );
		String newName = STR_NEW_VARIABLE_ID + Integer.toString( NetworkInternalFrame.INT_COUNTER_NEW_VARIABLES++ );
		while( myBeliefNetwork.forID( newName ) != null ) newName = STR_NEW_VARIABLE_ID + Integer.toString( NetworkInternalFrame.INT_COUNTER_NEW_VARIABLES++ );

		//add extra params to hugin node
		Map values = new HashMap();

		values.put( PropertySuperintendent.KEY_HUGIN_ID,       newName    );
		values.put( PropertySuperintendent.KEY_HUGIN_STATES,   statelist  );
		values.put( PropertySuperintendent.KEY_HUGIN_LABEL,    newName    );
		values.put( PropertySuperintendent.KEY_HUGIN_POSITION, loc        );
		values.put(           DSLConstants.KEY_SUBMODEL,       mySubmodel );

	  //HuginNode hNode = new HuginNodeImpl( newName, statelist, values );
	  //DisplayableFiniteVariable dVar = new DisplayableFiniteVariableImpl( hNode, hnInternalFrame );
		DisplayableFiniteVariable dVar = (DisplayableFiniteVariable) myBeliefNetwork.newFiniteVariable( values );
	  //dVar.setDSLSubmodel( mySubmodel );

		//add variables to networks
		myBeliefNetwork.addVariable( dVar, true );

		//notify all others of change  (recompile, update tree, ...)
		hnInternalFrame.netStructureChanged( new NetStructureEvent( NetStructureEvent.NODES_ADDED, Collections.singleton( dVar ) ) );
	}

	/* ------------------
	* Update NetworkDisplay to do changes
	* ------------------ */

	/**
		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ece )
	{
		warnMonitors( ece );
	}

	public void evidenceChanged( EvidenceChangeEvent ece )
	{
		updateMonitors( ece );
	}

	/** interface RecompilationListener */
	public void networkRecompiled()
	{
		try{ this.notorious = this.hnInternalFrame.getInferenceEngine().notoriousEdges(); refreshNotoriousEdges(); }
		catch( Throwable thrown ){ System.err.println( "warning: NetworkDisplay.networkRecompiled() caught " + thrown ); }
		updateMonitors( null );
	}

	/** @since 20091218 */
	public NetworkDisplay refreshNotoriousEdges(){
		for( Iterator it = myEdgeList.iterator(); it.hasNext(); ){ ((Arrow) it.next()).setRecoverable( false ); }

		if( (this.notorious == null) || this.notorious.isEmpty() ){ return this; }

		Map                        map = new HashMap();
		Arrow                     next = null;
		for( Iterator it = myEdgeList.iterator(); it.hasNext(); ){
			next = (Arrow) it.next();
			map.put( new DirectedEdge( next.getStart().getFiniteVariable(), next.getEnd().getFiniteVariable() ), next );
		}

		for( Iterator it = this.notorious.iterator(); it.hasNext(); ){
			if( (next = (Arrow) map.get( it.next() )) == null ){ throw new IllegalStateException( "missing arrow for edge in NetworkDisplay.refreshNotoriousEdges()" ); }
			next.setRecoverable( true );
		}

		return this;
	}

	/**
		@author Keith Cascio
		@since 081302
	*/
	public void cptChanged( CPTChangeEvent evt )
	{
		updateMonitors( null );
	}

	/**
		@author Keith Cascio
		@since 071003
	*/
	protected void warnMonitors( EvidenceChangeEvent ece )
	{
		for( ListIterator itr = myComponentList.listIterator(); itr.hasNext();)
		{
			((NetworkComponentLabel)itr.next()).warning( ece );
		}
	}

	//pass message on to labels to update their probabilities
	protected void updateMonitors( EvidenceChangeEvent ece )
	{
		double globalMaximumProbability = Double.NaN;

		if( MonitorImpl.FLAG_ODDS_NORMALIZE_NETWORK )
		{
			InferenceEngine ie = hnInternalFrame.getInferenceEngine();
			if( ie != null ) globalMaximumProbability = ie.max();
		}

		for( ListIterator itr = myComponentList.listIterator(); itr.hasNext();)
		{
			((NetworkComponentLabel)itr.next()).evidenceChanged( ece, globalMaximumProbability );
		}
	}

	/**
	* Update all the local variables because the network structure changed.
	* Currently does not take advantage of the parameter since that was added
	* after the function was created.
	*/
	public void netStructureChanged( NetStructureEvent ev )
	{
		//System.out.println( "NetworkDisplay.netStructureChanged()" );

		BeliefNetwork bn = hnInternalFrame.getBeliefNetwork();
		Point nodeLoc = new Point();
		Rectangle rect = new Rectangle();

		DisplayableFiniteVariable dVar = null;
		DSLSubmodel newNodeDSLSubmodel = null;
		boolean diligent = true;

		//boolean nodesAddedRemoved = false;

		//look for new nodes
		if( ev.eventType == NetStructureEvent.NODES_ADDED && ev.finiteVars != null )
		{
			//System.out.println( "\t ev.eventType == NetStructureEvent.NODES_ADDED, mySubmodel = "+mySubmodel+"\n\t {" );
			DSLSubmodel DSM = null;
			Object obj = null;
			for( Iterator itr = ev.finiteVars.iterator(); itr.hasNext(); )
			{
				obj = itr.next();
				if( hnInternalFrame.getBeliefNetwork().mayContain( obj ) )
				{
					//System.out.println( "\t\t encountering ("+obj.getClass()+")" + obj );
					dVar = (DisplayableFiniteVariable)obj;
					DSM = dVar.getDSLSubmodel();
					//System.out.println( "\t\t DSM = " + DSM );
					if( DSM == null || DSM == mySubmodel )
					{
						//System.out.println( "\t\t DSM == null || DSM == mySubmodel" );
						NodeLabel newNL = makeNode( dVar, diligent );
						updateActualTranslationDelta( newNL );
						if( ! myFlagFileOverridesPref ){ newNL.setVirtualSize( myPreferenceNodeSize ); }
						//nodesAddedRemoved = true;
					}
					else System.err.println( "WARNING: in NetworkDisplay.netStructureChanged(), DFV " + dVar + " encountered, sub variable "+dVar.getSubvariable().getClass()+", not member of root submodel." );
				}
				else System.err.println( "WARNING: unknown class " + obj.getClass().getName() + " was added as a vertex." );
			}
			try{ resizeAll( ev.finiteVars ); }catch( Throwable thrown ){ System.err.println( "warning: NetworkDisplay.netStructureChanged() caught " + thrown ); }
			//System.out.println( "\t }" );
		}

		//look for deleted nodes
		if( ev.eventType == NetStructureEvent.NODES_REMOVED )
		{
			Set ver = bn.vertices();
			Object obj = null;
			for( ListIterator itr = myComponentList.listIterator(); itr.hasNext();)
			{
				obj = itr.next();
				if( obj instanceof NodeLabel )
				{
					NodeLabel lbl = (NodeLabel)obj;
					if( !ver.contains( lbl.getFiniteVariable() ) )
					{
						itr.remove();  //remove label from myComponentList
						lbl.hideEvidenceDialog();
						if( FLAG_DESKTOP_CONTAINS_LABELS ) myDesktopPane.remove( lbl );
						if( mySelectedComponents.contains( lbl ) ) mySelectedComponents.remove( lbl );
						if( myUnselectedComponents.contains( lbl ) ) myUnselectedComponents.remove( lbl );
						//nodesAddedRemoved = true;
					}
				}
			}
		}

		//look for new edges
		if( ev.eventType == NetStructureEvent.EDGE_ADDED )
		{
			for( Iterator itr = bn.vertices().iterator(); itr.hasNext();)
			{
				DisplayableFiniteVariable fv_out = (DisplayableFiniteVariable)itr.next();
				Set out = bn.outGoing( fv_out);  //set of adjacent variables
				for( Iterator itr2 = out.iterator(); itr2.hasNext();)
				{
					DisplayableFiniteVariable fv_in = (DisplayableFiniteVariable)itr2.next();

					boolean found = false;
					synchronized( mySynchronizationEdgeList ){
						for( Iterator itr3 = myEdgeList.iterator(); itr3.hasNext(); ){
							if( ((Arrow)itr3.next()).isEqual( fv_out, fv_in ) ){
								found = true;
								break;
							}
						}
					}

					if( ! found ){
						NodeLabel st = fv_out.getNodeLabel(), en = fv_in.getNodeLabel();
						if( st == null || en == null ){ System.err.println( "warning: NetworkDisplay.netStructureChanged() encountered missing NodeLabel(s)" ); }
						else{ makeArrow( st, en, false ); }
					}
				}
			}

			try{
				synchronized( mySynchronizationEdgeList ){
					int result = cleanseArrows( myListRecoverableArrows, true );
				  //System.out.println( "cleansed " + result + " recoverable arrows" );
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: NetworkDisplay.netStructureChanged() caught " + thrown );
			}
		}

		this.notorious = null;

		//look for deleted edges
		if( ev.eventType == NetStructureEvent.EDGE_REMOVED || ev.eventType == NetStructureEvent.NODES_REMOVED )
		{
			synchronized( mySynchronizationEdgeList ){ cleanseArrows( myEdgeList, false ); }
		}

		Collection variablesMySubmodel = myBeliefNetwork.getVariables( mySubmodel );
		try{
			assertValid( variablesMySubmodel );
		}catch( Exception e ){
			System.err.println( "WARNING: in NetworkDisplay.netStructureChanged(), submodel variables failed validation with message:\n" + e.getMessage() );
			return;
		}

		synchronized( mySynchronizationEdgeList ){ clearArrows( myEdgeList ); }
		makeVariableSourceEdges( variablesMySubmodel, bn );
		makeSubmodelSourceEdges(                      bn );

		synchronized( mySynchronizationEdgeList ){ clearArrows( myListRecoverableArrows ); }
		makeRecoverableArrows(   variablesMySubmodel, bn );

		try{ refreshNotoriousEdges(); }
		catch( Throwable thrown ){ System.err.println( "warning: NetworkDisplay.netStructureChanged() caught " + thrown ); }

		updateMonitors( null );
		fireRecalculateActual();
		recalculateDesktopSize();
	}

	/** @since 20080221 */
	private int clearArrows( List arrows ){
		if( arrows == null ){ return 0; }
		int            count = 0;
		Arrow          ar;
		for( ListIterator itr = arrows.listIterator(); itr.hasNext(); ){
			ar = (Arrow) itr.next();
			ar.die();
			++count;
		}
		arrows.clear();
		return count;
	}

	/** @since 20080221 */
	private int cleanseArrows( List arrows, boolean present ){
		int            count = 0;
		Arrow          ar;
		FiniteVariable st, en;
		for( ListIterator itr = arrows.listIterator(); itr.hasNext(); ){
			ar = (Arrow) itr.next();
			st = ar.getStart().getFiniteVariable();
			en = ar  .getEnd().getFiniteVariable();
			if( myBeliefNetwork.containsEdge( st, en ) == present ){
				itr.remove();
				ar.die();
				++count;
			}
		}
		return count;
	}

	/** @since 021804 */
	public void assertValid( Collection variables ) throws IllegalStateException
	{
		String message = null;
		try{
			Object var;
			DisplayableFiniteVariable dVar;
			NodeLabel nl;
			for( Iterator it = variables.iterator(); it.hasNext(); ){
				var = it.next();
				if( !(var instanceof DisplayableFiniteVariable) ){
					message = "Encountered non-DisplayableFiniteVariable " +var.toString()+ " of type " + var.getClass().getName();
					return;
				}
				dVar = (DisplayableFiniteVariable) var;
				nl = dVar.getNodeLabel();
				if( nl == null ){
					message = "DisplayableFiniteVariable " +dVar.toString()+ " lacks NodeLabel.";
					return;
				}
			}
		}finally{
			if( message != null ){
				throw new IllegalStateException( message );
			}
		}
	}

	/* ------------------
	* Internal classes
	* ------------------ */

	public interface UserInputListener
	{
		public void handleUserActualPoint( Point pActual );
		public void userInputCancelled();
		public void handleUserEdge( DisplayableFiniteVariable source, DisplayableFiniteVariable sink );
	}

	protected class StatusBar extends JLabel
	{
		// implements ComponentListener
		protected int height = 25;

		public StatusBar()
		{
			super();
			setForeground( Color.black);
			setBackground( Color.white);
			setOpaque( true);
			setBorder( new EtchedBorder());
			setVisible( false);
		}

		public void displayMessage( String txt)
		{
			setText( txt);
			setVisible( true);
		}

		public void clearMessage()
		{
			setVisible( false);
		}

		/*
		public void componentHidden( ComponentEvent e) {}
		public void componentShown( ComponentEvent e) {}
		public void componentMoved( ComponentEvent e) {}
		public void componentResized( ComponentEvent e) {
		Component parent = e.getComponent();
		Rectangle rect = ((JComponent)parent).getVisibleRect();
		setBounds( rect.x, rect.y + rect.height - height, rect.width, height);
		}

		*/
	}

	/** An enumerated type over edit modes.

		@author keith cascio
		@since 20021017 */
	private static class EditMode
	{
		public static final EditMode
			MODE_DEFAULT             =   new EditMode(),
			MODE_COPY_CPT            =   new EditMode( true ){
				public String   getHalfwayMessage(){ return "Please a destination node. (Right-click to cancel.)"; }

				public EditMode finish( NodeLabel nl, NetworkDisplay networkdisplay ){
					networkdisplay        .copyCPT( networkdisplay.firstNodeLabelWhenSelecting.getFiniteVariable(), nl.getFiniteVariable() );
					return this;
				}
			},
			MODE_ADD_NODE            =   new EditMode(),
			MODE_ADD_EDGE            =   new EditMode( true ){
				public EditMode finish( NodeLabel nl, NetworkDisplay networkdisplay ){
					networkdisplay        .addEdge( networkdisplay.firstNodeLabelWhenSelecting.getFiniteVariable(), nl.getFiniteVariable() );
					return this;
				}
			},
			MODE_DELETE_EDGE         =   new EditMode( true ){
				public EditMode finish( NodeLabel nl, NetworkDisplay networkdisplay ){
					networkdisplay     .deleteEdge( networkdisplay.firstNodeLabelWhenSelecting.getFiniteVariable(), nl.getFiniteVariable() );
					return this;
				}
			},
			MODE_REPLACE_EDGE        =   new EditMode( true ){
				public EditMode finish( NodeLabel nl, NetworkDisplay networkdisplay ){
					networkdisplay    .replaceEdge( networkdisplay.firstNodeLabelWhenSelecting.getFiniteVariable(), nl.getFiniteVariable() );
					return this;
				}
			},
			MODE_RECOVER_EDGE        =   new EditMode( true ){
				public EditMode finish( NodeLabel nl, NetworkDisplay networkdisplay ){
					networkdisplay    .recoverEdge( networkdisplay.firstNodeLabelWhenSelecting.getFiniteVariable(), nl.getFiniteVariable() );
					return this;
				}
			},
			MODE_PROMPT_USER_POINT   =   new EditMode(),
			MODE_PROMPT_USER_EDGE    =   new EditMode( true ){
				public EditMode finish( NodeLabel nl, NetworkDisplay networkdisplay ){
					networkdisplay .myCurrentUserInputListener
					               .handleUserEdge( networkdisplay.firstNodeLabelWhenSelecting.getFiniteVariable(), nl.getFiniteVariable() );
					return this;
				}
			};

		protected EditMode(){ this( false ); }

		protected EditMode( boolean nodeselection ){ this.nodeselection = nodeselection; }

		public String   getHalfwayMessage(){ return this.nodeselection ? "Please select the node where the edge ends. (Right-click to cancel.)" : null; }

		public EditMode            finish( NodeLabel nl, NetworkDisplay networkdisplay ){ return this; }

		public final boolean nodeselection;
	}

	transient protected EditMode myEditMode = EditMode.MODE_DEFAULT;


	//Modes
	//protected boolean addNodeMode = false;
	//protected boolean addEdgeMode = false;
	//protected boolean deleteEdgeMode = false;

	/**
		An enumerated type over the selection
		modes that a NodeMouseSelector can
		assume.

		@author Keith Cascio
		@since 040202
	*/
	public static class SelectionMode
	{
		protected SelectionMode( String name ) { myName = name; }

		protected String myName;

		public String toString()
		{
			return myName;
		}

		public static final SelectionMode MODE_DEFAULT = new SelectionMode( "MODE_DEFAULT" );
		public static final SelectionMode MODE_NODE_SELECTION = new SelectionMode( "MODE_NODE_SELECTION" );
		public static final SelectionMode MODE_LOCATION_SELECTION = new SelectionMode( "MODE_LOCATION_SELECTION" );
	}

	/**
		<p>
		This class handles the mouse movements in a Network Display window.
		All coordinates are relative to the NetworkDisplay component.
		<p>
		Changed by Keith Cascio 040202
	*/
	protected class NodeMouseSelector extends MouseInputAdapter
	{
		/**
		*/
		public NodeMouseSelector( NetworkDisplay display )
		{
			this.display = display;
		}

		/**
		   @param newMode The NodeMouseSelector will enter this mode.
		   @author Keith Cascio
		   @since 040202
		*/
		public void setMode( SelectionMode newMode )
		{
			//System.out.println( "NodeMouseSelector.setMode("+newMode+")" );

			myMode = newMode;

			Cursor newCursor = ( myMode == SelectionMode.MODE_DEFAULT ) ? UI.CURSOR_DEFAULT : UI.CURSOR_HAND;

			//System.out.println( "\tcursor: "+newCursor );
			display.delayedCursor.start( newCursor );//setCursor( newCursor );
		}

		/**
		   @return The mode this NodeMouseSelector is in currently.
		   @author Keith Cascio
		   @since 040202
		*/
		public SelectionMode getMode()
		{
			return myMode;
		}

		/**
		   @author Keith Cascio
		*/
		protected boolean showPopup( MouseEvent e )
		{
			if(	(e.isPopupTrigger()) &&
			((componentClicked = e.getComponent()) instanceof edu.ucla.belief.ui.networkdisplay.NodeLabel)
			)
			{
				display.nodeLabelPopup( (edu.ucla.belief.ui.networkdisplay.NodeLabel)componentClicked );
				return true;
			}
			else return false;
		}

		//MouseListener
		/**
		   @author Keith Cascio
		   @since 041902
		*/
		public void mouseClicked( MouseEvent e )
		{
			componentClicked = e.getComponent();

			if( componentClicked instanceof edu.ucla.belief.ui.networkdisplay.NetworkComponentLabel )
			{
				//double-click
				if( e.getClickCount() > 1 )
				{
					((NetworkComponentLabel)componentClicked).doDoubleClick();
				}
			}

			NetworkDisplay.this.requestFocus();
		}

		/**
			@author Keith Cascio
			@since 082602
		*/
		public void forceDefaultMode()
		{
			setMode( SelectionMode.MODE_DEFAULT );
			display.forceDefaultMode();
		}

		//MouseListener
		public void mousePressed(MouseEvent e)
		{
			dragHappened = false;

			boolean isRightMouseButton = SwingUtilities.isRightMouseButton( e );

			if( showPopup( e ) ) return;
			else if( isRightMouseButton &&
			( myMode == SelectionMode.MODE_NODE_SELECTION || myMode == SelectionMode.MODE_LOCATION_SELECTION ) )
			{
				if( display.myCurrentUserInputListener != null ) display.myCurrentUserInputListener.userInputCancelled();
				forceDefaultMode();
				return;
			}

			if( isRightMouseButton ) return;

			componentClicked = e.getComponent();

			MouseEvent ne = SwingUtilities.convertMouseEvent( e.getComponent(), e, display.myDesktopPane );
			dragStart.setLocation( ne.getX(), ne.getY() );
			isInitialClickOnNode = ( componentClicked instanceof edu.ucla.belief.ui.networkdisplay.NodeLabel );
			isDraggable = ( componentClicked instanceof edu.ucla.belief.ui.networkdisplay.NetworkComponentLabel );

			if( myMode == SelectionMode.MODE_NODE_SELECTION  && isInitialClickOnNode )
			{
				display.nodeLabelSelectedByMouse( (NodeLabel)componentClicked );
			}
			else if( myMode == SelectionMode.MODE_LOCATION_SELECTION )
			{
				//display.locationSelectedByMouse( ne.getX(), ne.getY() );
				display.locationSelectedByMouse( ne.getPoint() );
			}
			else
			{
				if( isDraggable )
				{
					NetworkComponentLabel NCL = (NetworkComponentLabel)componentClicked;

					//Keith Cascio 052102 new selection semantics
					unmodified = false;
					if( e.isShiftDown() )
					{
						NCL.setSelected( true );
						isDraggable = false;
					}
					else if( e.isControlDown() )
					{
						//System.out.println( "mousePressed/CTRL on draggable" );
						NCL.selectionSwitch();
						isDraggable = false;
					}
					else if( !NCL.isSelected() )
					{
						display.nodeSelectionClearAll();
						NCL.setSelected( true );
						//display.myDesktopPane.repaint();
					}
					else unmodified = true;
				}
				else
				{
					if( !e.isShiftDown() && !e.isControlDown() ) display.nodeSelectionClearAll();
				}
			}
		}

		public void mouseReleased(MouseEvent e)
		{
			if( !showPopup( e ) && !SwingUtilities.isRightMouseButton( e ) )
			{
				if( myMode == SelectionMode.MODE_NODE_SELECTION )
				{
				}
				else if( myMode == SelectionMode.MODE_LOCATION_SELECTION )
				{
					setMode( SelectionMode.MODE_DEFAULT );
				}
				else if( isDraggable )
				{
					if( unmodified && !dragHappened )//&& componentClicked instanceof NetworkComponentLabel )
					{
						//if( !e.isShiftDown() ) display.nodeSelectionClearAll();
						//((NetworkComponentLabel)componentClicked).setSelected( true );
						//display.myDesktopPane.repaint();
					}
					else if( dragHappened )
					{
						confirmActualLocations( mySelectedComponents );
					}
				}
				else if( dragHappened )
				{
					//select new
					finalSelectionRect.setLocation( dragStart );
					finalSelectionRect.setSize(0,0);
					finalSelectionRect.add( SwingUtilities.convertPoint(
						e.getComponent(), e.getX(), e.getY(), display.myDesktopPane));

					if( e.isControlDown() ) nodeSelectionInvert( finalSelectionRect );
					else nodeSelectionAdd( finalSelectionRect );

					//Delete last outline rect
					display.mouseDrag( oldDragRect, null);
					oldDragRect.setSize( 0,0);
				}
			}

			isDraggable = false;
			dragHappened = false;
		}

		//MouseMotionListener
		public void mouseDragged(MouseEvent e)
		{
			if( SwingUtilities.isRightMouseButton( e ) ) return;

			if( !dragHappened )
			{
				dragHappened = true;
				if( isDraggable ) validateSelectedBounds();//sortComponents();
				//System.out.println( "drag initiated at " + myActualSelectedBounds + " with " + mySelectedComponents );
			}
			MouseEvent ne = SwingUtilities.convertMouseEvent( e.getComponent(), e, display.myDesktopPane );

			if( myMode == SelectionMode.MODE_DEFAULT )
			{
				if( isDraggable )
				{
					//move nodes
					deltaX = ne.getX() - dragStart.x;
					deltaY = ne.getY() - dragStart.y;
					display.moveSelectedNodes( deltaX, deltaY );
					dragStart.setLocation( ne.getX(), ne.getY() );
				}
				else
				{
					//draw rect for selecting nodes
					dragRect.setLocation( dragStart);
					dragRect.setSize( 0, 0);
					dragRect.add( ne.getX(), ne.getY());

					display.mouseDrag( oldDragRect, dragRect);
					oldDragRect.setBounds( dragRect);
				}
			}
		}

		//protected data
		protected NetworkDisplay display = null;
		protected Point dragStart = new Point();
		protected Rectangle finalSelectionRect = new Rectangle();
		protected boolean isInitialClickOnNode = false;
		protected boolean isDraggable = false;
		protected SelectionMode myMode = SelectionMode.MODE_DEFAULT;
		protected boolean dragHappened = false;
		protected boolean unmodified = false;
		protected int deltaX = (int)0;
		protected int deltaY = (int)0;

		//Temp Values so that don't reallocate memory
		protected Rectangle dragRect = new Rectangle();
		protected Rectangle oldDragRect = new Rectangle();
		protected Component componentClicked = null;
	}

	/**
		@author Keith Cascio
		@since 091003
	*/
	public DisplayableBeliefNetwork getBeliefNetwork()
	{
		return myBeliefNetwork;
	}

	/** @since 20060723 */
	public List getNetworkComponentLabels(){
		return myComponentList;
	}

	/** @since 20060731 */
	public List getArrows(){
		return myEdgeList;
	}

	/** List of edges (Arrow objects) in the network.*/
	protected ArrayList myEdgeList = new ArrayList(), myListRecoverableArrows = new ArrayList();
	/** List of nodes (NetworkComponentLabel objects) in the network.*/
	protected ArrayList myComponentList = new ArrayList();

	private Collection myCollectionSubmodelDisplays;

	//parent
	protected NetworkInternalFrame hnInternalFrame;
	protected DisplayableBeliefNetwork myBeliefNetwork;
	protected Collection/*<DirectedEdge>*/ notorious;
	protected NodeMouseSelector mouseSel;
	private Object mySynchronization = new Object();
	private Object mySynchronizationEdgeList = new Object();
	private LinkedList myListDecorators;

	//Misc Variables
	protected NodeLabel firstNodeLabelWhenSelecting = null;
	protected StatusBar myStatusBar = null;

	//A rectangle that bounds the Hugin network, so that the nodes can be translated
	//  to screen coordinates.
	//protected Rectangle netBounds = new Rectangle(0,0,400,200);

	protected SamiamPreferences mySamiamPreferences = null;
	protected PreferenceGroup myNetPrefs = null;
	protected PreferenceGroup myMonitorPrefs = null;

	protected DSLSubmodel mySubmodel;
	protected boolean myIsDSL = false;
	protected boolean myIsMainSubmodel = false;
	protected boolean myLocationsFrozen = false;
}
