package edu.ucla.belief.ui;

import edu.ucla.belief.ui.actionsandmodes.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.dialogs.*;
import edu.ucla.belief.ui.internalframes.*;
import edu.ucla.belief.ui.rc.RecursiveConditioningInternalFrame;
import edu.ucla.belief.ui.tree.*;
import edu.ucla.belief.ui.networkdisplay.*;
import edu.ucla.belief.sensitivity.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.statusbar.StatusBar;
import edu.ucla.belief.ui.animation.*;

import edu.ucla.belief.*;
import edu.ucla.belief.approx.ApproxEngine;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.structure.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.InferenceValidProperty;
//import edu.ucla.util.JVMTI;
import edu.ucla.util.code.*;
import edu.ucla.belief.io.NodeLinearTask;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.util.WeakLinkedList;

import il2.bridge.Converter;

import java.util.List;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/** InternalFrame for a Hugin Net. */
public class NetworkInternalFrame	extends JInternalFrame
					implements InternalFrameListener,
					DynaListener,
					EvidenceChangeListener,
					SamiamUserModal
{
	protected UI ui = null;
	private   NodeLinearTask myTask;
	protected SamiamPreferences mySamiamPreferences = null;
	protected DisplayableBeliefNetwork myBeliefNetwork = null;
	protected InferenceEngine myInferenceEngine = null;
	private ApproxEngine myApproxEngine;
	private ComputationCache myComputationCache;
	protected Map myMapDvarsToVariableInstanceArrays = null;
	protected Object mySynchronization = new Object();
	protected Object mySynchPM = new Object();
	private String myPushedStatusMessage;
	private boolean myFlagNeverSaved = false;

	protected JCheckBoxMenuItem menuItem;
	protected WeakLinkedList cptChangeListeners, netStructureChangeListeners;
	protected List varBoxes;

	protected JDesktopPane desktopPane;
	private JSplitPane myJSplitPane;
	protected EvidenceTreeScrollPane evidenceTreeScrollPane;
	protected NetworkDisplay networkDisplay;
	protected JInternalFrame myJIFInferenceEngineControl;

	protected JInternalFrame networkInternalFrame;
	protected PartialInternalFrame partialInternalFrame;
	protected ApproxInternalFrame approxInternalFrame;
	protected SensitivityInternalFrame sensitivityInternalFrame;
	protected ConflictInternalFrame conflictInternalFrame;
	//protected RecursiveConditioningFrame recCondInternalFrame;
	protected RetractInternalFrame retractInternalFrame;
	protected MAPInternalFrame mapInternalFrame;
    protected SDPInternalFrame sdpInternalFrame;
	protected MPEInternalFrame mpeInternalFrame;
	protected edu.ucla.belief.ui.rc.RecursiveConditioningInternalFrame myRCIF = null;
	protected ImpactInternalFrame impactInternalFrame;
	protected SelectionInternalFrame selectionInternalFrame;
	protected CodeToolInternalFrame myCodeToolInternalFrame;
	private Animator myAnimator;

	private ConsoleFrameWriter myConsoleFrameWriter;
	public PrintWriter console = myConsoleFrameWriter = new ConsoleFrameWriter();
	private JInternalFrame myConsoleInternalFrame;
	private ThreadGroup myThreadGroup;

	/*/ @since 20060720 //
	public void repaint(){
	  //System.out.println( "NetworkInternalFrame.repaint()" );
	  //super.repaint();
	}*/

	/** @since 20040113 */
	public void dispose()
	{
		if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "(NetworkInternalFrame)"+myFileName+".dispose()" );

		super.dispose();

		try{
			if( ui != null ){
				ui.setDefaultCursor();
				if( myPushedStatusMessage != null ){
					StatusBar bar = ui.getStatusBar();
					if( bar != null ) bar.popText( myPushedStatusMessage, StatusBar.WEST );
				}
			}
			myPushedStatusMessage = null;
			// swing workaround, attempt to force swing to //
			// remove all references to this NetworkInternalFrame //
			setRootPaneCheckingEnabled( false );
			javax.swing.plaf.ComponentUI cui = getUI();
			if( cui != null ) cui.uninstallUI( this );
			if( this.networkDisplay != null ) this.networkDisplay.dispose();

			if( myThreadGroup != null ){
				edu.ucla.util.Interruptable.kill( myThreadGroup );
			}
		}catch( Exception exception ){
			System.err.println( exception );
		}catch( Error error ){
			System.err.println( error );
		}

		//null out (almost) all state for benefit of garbage collection
		this.approxInternalFrame = null;
		this.conflictInternalFrame = null;
		this.console = null;
		this.cptChangeListeners = null;
		this.desktopPane = null;
		this.evidenceTreeScrollPane = null;
		this.impactInternalFrame = null;
		this.mapInternalFrame = null;
        this.sdpInternalFrame = null;
		this.menuItem = null;
		this.mpeInternalFrame = null;
		this.myAnimator = null;
		this.myApproxEngine = null;
		this.myBeliefNetwork = null;
		this.myCodeToolInternalFrame = null;
		this.myComputationCache = null;
		this.myConsoleFrameWriter = null;
		this.myConsoleInternalFrame = null;
		//this.myFileName = null;
		//this.myFilePath = null;
		//this.myFlagNeverSaved = null;
		this.myInferenceEngine = null;
		this.myJSplitPane = null;
		this.myLastCalculatedPrE = null;
		this.myMapDvarsToVariableInstanceArrays = null;
		//this.myNetStructureHasChanged = null;
		this.myNodePropertyChangeListeners = null;
		this.myPushedStatusMessage = null;
		this.myRCIF = null;
		this.myRecompilationListeners = null;
		this.mySamiamPreferences = null;
		//this.mySamiamUserMode = null;
		this.mySamiamUserModePrevious = null;
		this.mySynchPM = null;
		this.mySynchronization = null;
		this.netStructureChangeListeners = null;
		this.networkDisplay = null;
		this.networkInternalFrame = null;
		this.partialInternalFrame = null;
		this.retractInternalFrame = null;
		this.selectionInternalFrame = null;
		this.sensitivityInternalFrame = null;
		this.ui = null;
		this.varBoxes = null;
	}

	/** @since 011304 */
	protected void finalize() throws Throwable
	{
		if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "(NetworkInternalFrame)"+myFileName+".finalize()" );
		super.finalize();
	}

	/** @since 071503 */
	public void setConsoleVisible( boolean flag )
	{
		if( flag )
		{
			if( myConsoleInternalFrame == null )
			{
				myConsoleInternalFrame = myConsoleFrameWriter.getFrame();
				initConsole();
				desktopPane.add( myConsoleInternalFrame );
				myConsoleInternalFrame.setLocation( 8, 32 );
			}

			selectInternalFrame( myConsoleInternalFrame );
		}
		else if( myConsoleInternalFrame != null ) myConsoleInternalFrame.setVisible( flag );
	}

	/** @since 20051010 */
	public ConsoleFrameWriter getConsoleFrameWriter(){
		return this.myConsoleFrameWriter;
	}

	public SamiamPreferences getPackageOptions()
	{
		return this.mySamiamPreferences;
	}

	protected static int newNetCount = 1;
	public static int INT_COUNTER_NEW_VARIABLES = (int)0;
	/** @author Keith Cascio
		@since 070802 */
	public static boolean FLAG_AUTO_ARRANGE = false;
	/** @author Keith Cascio
		@since 070802 */
	public static boolean FLAG_DEBUG_VERBOSE = Util.DEBUG_VERBOSE;
	/** @author Keith Cascio
		@since 061702 */
	protected SamiamUserMode mySamiamUserMode;
	/** @author Keith Cascio
		@since 072904 */
	private SamiamUserMode mySamiamUserModePrevious;
	/** @author Keith Cascio
		@since 062102 */
	protected boolean myNetStructureHasChanged = false;

	/** @author Keith Cascio
		@since 102402 */
	private String myFilePath = null;
	private String myFileName = null;

	/**
		@author Keith Cascio
		@since 071502
	*/
	public String toString()
	{
		return "NetworkInternalFrame - \"" + getCurrentFileName() + "\"";
	}

	/**
		@author Keith Cascio
		@since 041802
	*/
	public String getCurrentFileName()
	{
		return myFilePath;
	}

	/**
		@author Keith Cascio
		@since 030802
	*/
	public void updateComponentTreeUI()
	{
		SwingUtilities.updateComponentTreeUI( desktopPane );
		SwingUtilities.updateComponentTreeUI( networkDisplay );
		if( networkInternalFrame != null ) SwingUtilities.updateComponentTreeUI( networkInternalFrame );
		if( partialInternalFrame != null ) SwingUtilities.updateComponentTreeUI( partialInternalFrame );
		if( sensitivityInternalFrame != null ) SwingUtilities.updateComponentTreeUI( sensitivityInternalFrame );
		if( conflictInternalFrame != null ) SwingUtilities.updateComponentTreeUI( conflictInternalFrame );
		//if( recCondInternalFrame != null ) SwingUtilities.updateComponentTreeUI( recCondInternalFrame );
		if( myRCIF != null ) SwingUtilities.updateComponentTreeUI( myRCIF );
		if( retractInternalFrame != null ) SwingUtilities.updateComponentTreeUI( retractInternalFrame );
		if( mapInternalFrame != null ) SwingUtilities.updateComponentTreeUI( mapInternalFrame );
        if( sdpInternalFrame != null ) SwingUtilities.updateComponentTreeUI( sdpInternalFrame );
		if( mpeInternalFrame != null ) SwingUtilities.updateComponentTreeUI( mpeInternalFrame );
		if( impactInternalFrame != null ) SwingUtilities.updateComponentTreeUI( impactInternalFrame );
		if( selectionInternalFrame != null ) SwingUtilities.updateComponentTreeUI( selectionInternalFrame );
		if( myCodeToolInternalFrame != null ) SwingUtilities.updateComponentTreeUI( myCodeToolInternalFrame );
		SwingUtilities.updateComponentTreeUI( this );
	}

	protected NetworkInternalFrame(BeliefNetwork bn,
					UI ui,
					String fileName,
					NodeLinearTask task,
					boolean wrap,
					ThreadGroup threadgroup ) throws Exception
	{
		super(fileName, true, true, true, true);

		if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\n\nNetworkInternalFrame( "+fileName+", wrap=="+wrap+" )" );

		this.myTask              = task;
		this.ui                  = ui;
		this.mySamiamPreferences = ui.getPackageOptions();
		this.myThreadGroup       = threadgroup;

		if( bn == null ){
			this.myFlagNeverSaved = true;
			this.myBeliefNetwork = Bridge2Tiger.Troll.solicit().newDisplayableBeliefNetworkImpl( new HuginNetImpl(), this );
		}
		else if( wrap ) this.myBeliefNetwork = Bridge2Tiger.Troll.solicit().newDisplayableBeliefNetworkImpl( bn, this );
		else this.myBeliefNetwork = (DisplayableBeliefNetwork)bn;
		if( Thread.currentThread().isInterrupted() ) return;

		setFilePath( fileName );

		if ( !BeliefNetworks.ensureCPTProperty( myBeliefNetwork ) ) ui.showErrorDialog( "Family probabilities sum to 0" );

		init();
	}
	/* profiled
	{
		super(fileName, true, true, true, true);

		if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\n\nNetworkInternalFrame( "+fileName+", wrap=="+wrap+" )" );

		long start = JVMTI.getCurrentThreadCpuTimeUnsafe();

		this.myTask = task;
		this.ui = ui;
		this.mySamiamPreferences = ui.getPackageOptions();

		if( bn == null ){
			this.myFlagNeverSaved = true;
			this.myBeliefNetwork = new DisplayableBeliefNetworkImpl( new HuginNetImpl(), this );
		}
		else if( wrap ) this.myBeliefNetwork = new DisplayableBeliefNetworkImpl( bn, this );
		else this.myBeliefNetwork = (DisplayableBeliefNetwork)bn;

		long mid0 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		setFilePath( fileName );

		if ( !BeliefNetworks.ensureCPTProperty( myBeliefNetwork ) ) ui.showErrorDialog( "Family probabilities sum to 0" );

		long mid1 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		init();

		long end =  JVMTI.getCurrentThreadCpuTimeUnsafe();
		long dbni   = mid0 - start;
		long ensure = mid1 - mid0;
		long init   = end  - mid1;
		double total = (double) (end - start);

		double dbniFrac   = ((double)dbni) / total;
		double ensureFrac = ((double)ensure) / total;
		double initFrac   = ((double)init) / total;

		Util.STREAM_DEBUG.println( "NetworkInternalFrame(), total: " + NetworkIO.formatTime((long)total) );
		Util.STREAM_DEBUG.println( "    new DisplayableBeliefNetworkImpl: " + NetworkIO.FORMAT_PROFILE_PERCENT.format(dbniFrac) + " (" + NetworkIO.formatTime(dbni)
		              + "),\n    ensureCPTProperty()             : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(ensureFrac) + " (" + NetworkIO.formatTime(ensure)
		              + "),\n    init()                          : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(initFrac) + " (" + NetworkIO.formatTime(init) + ")" );

	}*/

	public NetworkInternalFrame( BeliefNetwork bn, UI ui, String fileName ) throws Exception
	{
		this( bn, ui, fileName, (NodeLinearTask)null, true, (ThreadGroup)null );
		//System.out.println( "NetworkInternalFrame1( "+fileName+" )" );
	}

	public NetworkInternalFrame( DisplayableBeliefNetwork bn, UI ui, String fileName ) throws Exception
	{
		this( bn, ui, fileName, (NodeLinearTask)null, false, (ThreadGroup)null );
		//System.out.println( "NetworkInternalFrame2( "+fileName+" )" );
	}

	public NetworkInternalFrame( UI ui ) throws Exception
	{
		this( null, ui, "untitled" + newNetCount++ + ".net", (NodeLinearTask)null, false, (ThreadGroup)null );
		//System.out.println( "NetworkInternalFrame3( untitled )" );
	}

	/** @since 20060519 */
	public NetworkInternalFrame( BeliefNetwork bn, UI ui, String fileName, NodeLinearTask task, ThreadGroup threadgroup ) throws Exception
	{
		this( bn, ui, fileName, task, true, threadgroup );
		//System.out.println( "NetworkInternalFrame4( "+task+" )" );
	}

	/** @since 20060519 */
	public NodeLinearTask getConstructionTask(){
		return NetworkInternalFrame.this.myTask;
	}

	/** @since 20051031 */
	private void setSMILEMode(){
		try{
			if( this.mySamiamUserMode == null ) return;

			boolean flagSMILEFile = false;
			if( this.myFilePath != null ){
				FileType type = FileType.getTypeForFile( new File( this.myFilePath ) );
				if( type.isSMILEType() ) flagSMILEFile = true;
			}
			if( (myBeliefNetwork != null ) && myBeliefNetwork.isGenieNet() ) flagSMILEFile = true;

			this.mySamiamUserMode.setModeEnabled( SamiamUserMode.SMILEFILE, flagSMILEFile );
		}catch( Exception exception ){
			System.err.println( "Warning: NetworkInternalFrame.setSMILEMode() caught " + exception );
		}
	}

	protected void init()
	{
		//long start = JVMTI.getCurrentThreadCpuTimeUnsafe();

		mySamiamUserMode = new SamiamUserMode();
		mySamiamUserMode.setModeEnabled( SamiamUserMode.EDIT, true );
		mySamiamUserMode.setModeEnabled( SamiamUserMode.OPENFILE, true );
		mySamiamUserMode.setModeEnabled( SamiamUserMode.NEEDSCOMPILE, true );
		NetworkInternalFrame.this.setSMILEMode();

		myMapDvarsToVariableInstanceArrays = new HashMap( myBeliefNetwork.size() );

		PreferenceGroup globalPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.PkgDspNme );
		PreferenceGroup netPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.NetDspNme );
		PreferenceGroup treePrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.TreeDspNme );
		PreferenceGroup monitorPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.MonitorsDspNme );

		myBeliefNetwork.setAutoCPTInvalidation( ((Boolean) mySamiamPreferences.getMappedPreference( SamiamPreferences.autoCPTInvalid ).getValue()).booleanValue() );

		DisplayableFiniteVariable dVar = null;
		for( DFVIterator it = myBeliefNetwork.dfvIterator(); it.hasNext(); )
		{
			dVar = it.nextDFV();
			VariableInstance[] instances = new VariableInstance[ dVar.size() ];
			for (int j = 0; j < dVar.size(); j++) instances[j] = new VariableInstance( dVar, dVar.instance(j) );
			myMapDvarsToVariableInstanceArrays.put( dVar, instances );
		}

		//Keith Cascio 070802
		//if( FLAG_AUTO_ARRANGE && myBeliefNetwork.getDSLSubmodelFactory().getNumSubmodels() < 2 )
		//{
		//	autoArrange( myBeliefNetwork, getPreferredNodeSize(), INT_AUTO_ARRANGE_NODE_SPACING, INT_AUTO_ARRANGE_NETWORK_WIDTH );
		//}

		initMenuItem();
		cptChangeListeners = new WeakLinkedList();
		netStructureChangeListeners = new WeakLinkedList();
		varBoxes = new LinkedList();

		pack();
		setBounds( 5, 5, 750, 490 );
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLayer(new Integer(ui.layer));
		addInternalFrameListener(this);

		myComputationCache = new ComputationCache( this );

		desktopPane = new JDesktopPane();
		//desktopPane.setDesktopManager( new SamiamDesktopManager() );
		//desktopPane = new SafeDesktopPane();
		myJSplitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
		setContentPane( myJSplitPane );

		//long mid0 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		networkDisplay = new NetworkDisplay( this, mySamiamPreferences, myBeliefNetwork.getMainDSLSubmodel() );
		if( Thread.currentThread().isInterrupted() ) return;
		//System.out.println( "Java NetworkInternalFrame contructing main NetworkDisplay with submodel " + hn.getDSLSubmodelFactory().MAIN );//debug

		//long mid1 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		evidenceTreeScrollPane = new EvidenceTreeScrollPane( this, mySamiamPreferences );

		//long mid2 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		networkInternalFrame = networkDisplay;//new JInternalFrame("Network", true,false, true, true);
		setInternalFrame( networkInternalFrame, 0, 0, 575, 460 );
		//networkInternalFrame.setContentPane( new JScrollPane(networkDisplay) );
		networkInternalFrame.setVisible(true);

		//recCondInternalFrame = new RecursiveConditioningFrame(this);
		//setInternalFrame(recCondInternalFrame, 35, 35,
		//			recCondInternalFrame.getPreferredSize().width,
		//			recCondInternalFrame.getPreferredSize().height);

		myJSplitPane.setLeftComponent(evidenceTreeScrollPane);
		myJSplitPane.setRightComponent(desktopPane);
		myJSplitPane.resetToPreferredSizes();

		//initKeyStrokes();

		//Keith Cascio 031302 Set default evidence on open of a network.
		setDefaultEvidence( (Component) NetworkInternalFrame.this );

		setSamiamUserMode( mySamiamUserMode );

		if( Util.DEBUG )
		{
			getPartialInternalFrame();
			getSensitivityInternalFrame();
			getConflictInternalFrame();
			getRetractInternalFrame();
			getMAPInternalFrame();
			getMPEInternalFrame();
			getRCInternalFrame();
		}

		//long mid3 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		ui.initNewInternalFrame(this);

		networkDisplay.requestFocus();

		//long end =  JVMTI.getCurrentThreadCpuTimeUnsafe();
		//long first  = mid0 - start;
		//long netds  = mid1 - mid0;
		//long etree  = mid2 - mid1;
		//long umode  = mid3 - mid2;
		//long uiinit = end  - mid3;
		//double total = (double) (end - start);

		//double firstFrac  = ((double)first) / total;
		//double netdsFrac  = ((double)netds) / total;
		//double etreeFrac  = ((double)etree) / total;
		//double umodeFrac  = ((double)umode) / total;
		//double uiinitFrac = ((double)uiinit) / total;

		//System.out.println( "NetworkInternalFrame.init(), total: " + NetworkIO.formatTime((long)total) );
		//System.out.println( "    first                     : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(firstFrac) + " (" + NetworkIO.formatTime(first)
		//              + "),\n    new NetworkDisplay        : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(netdsFrac) + " (" + NetworkIO.formatTime(netds)
		//              + "),\n    new EvidenceTreeScrollPane: " + NetworkIO.FORMAT_PROFILE_PERCENT.format(etreeFrac) + " (" + NetworkIO.formatTime(etree)
		//              + "),\n    setSamiamUserMode()       : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(umodeFrac) + " (" + NetworkIO.formatTime(umode)
		//              + "),\n    ui.initNewInternalFrame() : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(uiinitFrac) + " (" + NetworkIO.formatTime(uiinit) + ")" );

		if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "DONE NetworkInternalFrame.init()\n\n" );
	}

	/** @since 062304 */
	private void initConsole(){
		//System.out.println( "NetworkInternalFrame.initConsole()" );
		if( myConsoleFrameWriter != null ){
			//System.out.println( "\t myConsoleFrameWriter != null" );
			JToolBar toolbar = myConsoleFrameWriter.getJToolBar();
			if( toolbar != null ){
				//System.out.println( "\t myConsoleFrameWriter.getJToolBar() != null" );
				toolbar.add( MainToolBar.initButton( ui.action_PRINTSELECTEDNODES ) );
			}
		}
	}

	/** @since 012104 */
	private void initMenuItem()
	{
		menuItem = new JCheckBoxMenuItem( getFileName() );
		menuItem.addActionListener( new ActionListener(){
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame.this.ui.toFront( NetworkInternalFrame.this );
				NetworkInternalFrame.this.ui.restore( NetworkInternalFrame.this );
			}
		} );
	}

	/**
		<p>
		Overrides JInternalFrame method as a no-op.
		<p>
		A workaround

		@author Keith Cascio
		@since 073002
	*/
	//public void restoreSubcomponentFocus(){}
	//public Component getFocusOwner(){ return null; }
	protected void firePropertyChange( String propertyName,Object oldValue,Object newValue )
	{
		if( !propertyName.equals( "ancestor" ) ) super.firePropertyChange( propertyName, oldValue, newValue );
	}

	/** @author Keith Cascio
		@since 090204 */
	public void bestWindowArrangement()
	{
		try{
			this.setMaximum( false );
			this.setBounds( new Rectangle( new Point(0,0), this.getParent().getSize() ) );
			this.networkDisplay.setMaximum( false );
			this.networkDisplay.setBounds( new Rectangle( new Point(0,0), this.desktopPane.getSize() ) );
			this.networkDisplay.fitOnScreen();
			this.networkDisplay.setSelected( true );
		}catch( Exception e ){
			System.err.println( "Warning: NetworkInternalFrame.bestWindowArrangement() caught Exception: " + e );
		}
	}

	/** @author Keith Cascio
		@since 021405 Valentine's Day! */
	public JDesktopPane getRightHandDesktopPane(){
		return this.desktopPane;
	}

	/** @author Keith Cascio
		@since 013103
	*/
	public JInternalFrame getPartialInternalFrame()
	{
		if( partialInternalFrame == null )
		{
			PreferenceGroup globalPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.PkgDspNme );
			partialInternalFrame = new PartialInternalFrame( this, globalPrefs );
			setInternalFrame(partialInternalFrame, 45, 45, 550, 450);
		}
		return partialInternalFrame;
	}

	/** @author Hei Chan
		@since 061004
	*/
	public JInternalFrame getApproxInternalFrame()
	{
		if( approxInternalFrame == null )
		{
			PreferenceGroup globalPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.PkgDspNme );
			approxInternalFrame = new ApproxInternalFrame( this, globalPrefs );
			setInternalFrame( approxInternalFrame, 15, 15, 400, 200 );
		}
		return approxInternalFrame;
	}

	/** @since 013103 */
	public SensitivityInternalFrame getSensitivityInternalFrame()
	{
		if( sensitivityInternalFrame == null )
		{
			PreferenceGroup globalPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.PkgDspNme );
			sensitivityInternalFrame = new SensitivityInternalFrame( this, globalPrefs );
			setInternalFrame(sensitivityInternalFrame, 15, 15, 605, 450);
		}
		return sensitivityInternalFrame;
	}

	/** @author Keith Cascio
		@since 013103
	*/
	public JInternalFrame getConflictInternalFrame()
	{
		if( conflictInternalFrame == null )
		{
			PreferenceGroup globalPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.PkgDspNme );
			conflictInternalFrame = new ConflictInternalFrame( this, globalPrefs );
			setInternalFrame(conflictInternalFrame, 20, 20, 550, 450);
		}
		return conflictInternalFrame;
	}

	/** @author Keith Cascio
		@since 013103
	*/
	public JInternalFrame getRetractInternalFrame()
	{
		if( retractInternalFrame == null )
		{
			PreferenceGroup globalPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.PkgDspNme );
			retractInternalFrame = new RetractInternalFrame( this, globalPrefs );
			setInternalFrame(retractInternalFrame, 25, 25, 550, 450);
		}
		return retractInternalFrame;
	}

	/** @author Keith Cascio
		@since 013103
	*/
	public MAPInternalFrame getMAPInternalFrame()
	{
		if( mapInternalFrame == null )
		{
			mapInternalFrame = new MAPInternalFrame( this );
			setInternalFrame(mapInternalFrame, 30, 30, 550, 400 );
		}
		return mapInternalFrame;
	}

    public SDPInternalFrame getSDPInternalFrame()
	{
		if( sdpInternalFrame == null )
		{
			sdpInternalFrame = new SDPInternalFrame( this );
			setInternalFrame(sdpInternalFrame, 30, 30, 550, 400 );
		}
		return sdpInternalFrame;
	}
    
	/** @since 20030131 */
	public MPEInternalFrame getMPEInternalFrame(){
		if( mpeInternalFrame == null ){
			mpeInternalFrame = new MPEInternalFrame( this );
			setInternalFrame( mpeInternalFrame, 30, 36, 400, 300 );
		}
		return mpeInternalFrame;
	}

	/** @since 082003 */
	public JInternalFrame getImpactInternalFrame()
	{
		if( impactInternalFrame == null )
		{
			impactInternalFrame = new ImpactInternalFrame( this );
			setInternalFrame(impactInternalFrame, 30, 30, 400, 300 );
		}
		return impactInternalFrame;
	}

	/** @since 082003 */
	public CodeToolInternalFrame getCodeToolInternalFrame()
	{
		if( myCodeToolInternalFrame == null )
		{
			myCodeToolInternalFrame = new CodeToolInternalFrame( this );
			setInternalFrame( myCodeToolInternalFrame, new Point( 6,16 ) );
		}
		return myCodeToolInternalFrame;
	}

	/** @since 082503 */
	public JInternalFrame getSelectionInternalFrame()
	{
		if( selectionInternalFrame == null )
		{
			selectionInternalFrame = new SelectionInternalFrame( this );
			setInternalFrame(selectionInternalFrame, new Point( 21,16 ) );
		}
		return selectionInternalFrame;
	}

	/** @since 010904 */
	public boolean wasUserPropertyDeleted()
	{
		if( selectionInternalFrame == null ) return false;
		UserPropertyEditPanel upep = selectionInternalFrame.getUserPropertyEditPanel();
		if( upep == null ) return false;
		else return upep.wasPropertyDeleted();
	}

	/** @since 010904 */
	public boolean userPropertiesModified()
	{
		return (( (getBeliefNetwork().countUserEnumPropertiesInitial() > (int)0) && wasUserPropertyDeleted() )
			|| getBeliefNetwork().thereExistsModifiedUserEnumProperty() );
	}

	/** @since 013103 */
	public edu.ucla.belief.ui.rc.RecursiveConditioningInternalFrame getRCInternalFrame()
	{
		if( myRCIF == null )
		{
			myRCIF = new edu.ucla.belief.ui.rc.RecursiveConditioningInternalFrame( this );
			setInternalFrame( myRCIF, new Point( 10, 10 ) );//, 505, 420 );
		}
		return myRCIF;
	}


	/*
	// @since 102202
	public void initKeyStrokes()
	{
		//initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_C, InputEvent.CTRL_MASK ), ui.action_COPY );
		//initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_X, InputEvent.CTRL_MASK ), ui.action_CUT );
		//initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_V, InputEvent.CTRL_MASK ), ui.action_PASTE );
		//initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_P, InputEvent.CTRL_MASK ), ui.action_PASTESPECIAL );

		//networkDisplay.getInputMap().setParent( getInputMap() );
		//networkDisplay.getActionMap().setParent( getActionMap() );

		//initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_EQUALS, InputEvent.CTRL_MASK ), ui.action_ZOOMIN );
		//initKeyStroke( KeyStroke.getKeyStroke( KeyEvent.VK_MINUS, InputEvent.CTRL_MASK ), ui.action_ZOOMOUT );
	}

	// @since 102402
	protected void initKeyStroke( KeyStroke stroke, Action action )
	{
		Object actionKey = new Object();
		getInputMap().put( stroke, actionKey );
		getActionMap().put( actionKey, action );
	}*/

	/** @since 102202 */
	public boolean isFocusTraversable()
	{
		return true;
	}

	/** @since 101002 */
	public void repaintEvidenceTree()
	{
		//System.out.println( "NIF.repaintEvidenceTree()" );
		evidenceTreeScrollPane.getEvidenceTree().doLayout();
		evidenceTreeScrollPane.repaint();
	}

	/** @since 20050921 */
	public void cramEvidenceTree(){
		int            bestLocation = 0x40;
		EvidenceTree   tree         = null;
		Dimension      effective;
		Insets         insets;
		try{
			(tree         = evidenceTreeScrollPane.getEvidenceTree()).doLayout();
			effective     = tree.getPreferredSize();
			insets        = evidenceTreeScrollPane.getJScrollPane().getInsets();
			bestLocation  = effective.width + insets.left + insets.right + 1;
		}catch( Exception exception ){
			System.err.println( "Warning: NetworkInternalFrame.cramEvidenceTree() failed, caught " + exception );
			return;
		}
		myJSplitPane.setDividerLocation( bestLocation );

	  /*final EvidenceTree ftree    = tree;
		final Runnable     runnable = new Runnable(){
			public void run(){
				try{
					Thread.sleep( 0x80 );
					Util.STREAM_DEBUG.println( "    after" );
					Util.STREAM_DEBUG.println( "    tree.getPreferredSize()? " + ftree.getPreferredSize() );
					Util.STREAM_DEBUG.println( "    tree.         getSize()? " + ftree.getSize() );
					Util.STREAM_DEBUG.println( "    pain.getPreferredSize()? " + evidenceTreeScrollPane.getJScrollPane().getPreferredSize() );
					Util.STREAM_DEBUG.println( "    pain.         getSize()? " + evidenceTreeScrollPane.getJScrollPane().getSize() );
					Util.STREAM_DEBUG.println( "    pnl .getPreferredSize()? " + evidenceTreeScrollPane.getPreferredSize() );
					Util.STREAM_DEBUG.println( "    pnl .         getSize()? " + evidenceTreeScrollPane.getSize() );
				}catch( Throwable thrown ){}
			}
		};
		new Thread( runnable ).start();*/
	}

	/** @since 091103 */
	public EvidenceTreeScrollPane getTreeScrollPane()
	{
		return evidenceTreeScrollPane;
	}

	/** @since 012004 */
	public ComputationCache getComputationCache(){
		return myComputationCache;
	}

	/** @since 101002 */
	public void fireNodePropertyChangeEvent( DisplayableFiniteVariable dVar )
	{
		fireNodePropertyChangeEvent( new NodePropertyChangeEvent( dVar ) );
	}

	/** @since 100703 */
	public void fireNodePropertyChangeEvent( NodePropertyChangeEvent e )
	{
		//System.out.println( "NIF.fireNodePropertyChangeEvent( "+dVar+" )" );
		/*
			not needed because user changes to node states trigger
			net structure change events.

			//clearRCSettings( false, true );
		*/

		if( !e.isEnumPropertyChange() )
		{
			VariableInstance[] instances = new VariableInstance[ e.variable.size() ];
			for( int j = 0; j < e.variable.size(); j++ ){
				instances[j] = new VariableInstance( e.variable, e.variable.instance(j) );
			}
			myMapDvarsToVariableInstanceArrays.put(e.variable, instances);

			refreshVariableComboBoxes();
		}

		myNodePropertyChangeListeners.cleanClearedReferences();
		NodePropertyChangeListener[] array = (NodePropertyChangeListener[]) myNodePropertyChangeListeners.toArray( new NodePropertyChangeListener[myNodePropertyChangeListeners.size()] );
		for( int i=0; i<array.length; i++ ) array[i].nodePropertyChanged( e );
	}
	public void addNodePropertyChangeListener( NodePropertyChangeListener listener )
	{
		if( myNodePropertyChangeListeners == null ) myNodePropertyChangeListeners = new WeakLinkedList();
		myNodePropertyChangeListeners.add(listener);
	}
	public boolean removeNodePropertyChangeListener( NodePropertyChangeListener listener)
	{
		if( myNodePropertyChangeListeners != null ) return myNodePropertyChangeListeners.remove(listener);
		else return false;
	}
	protected WeakLinkedList myNodePropertyChangeListeners;

	/**
		interface EvidenceChangeListener
		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ece )
	{
		if( ui == null || mySynchronization == null ) return;//disposed

		StatusBar bar = ui.getStatusBar();
		if( bar != null ) bar.pushText( (myPushedStatusMessage = STR_MSG_PROPAGATE), StatusBar.WEST );
		ui.setCursor( UI.CURSOR_WAIT_EVIDENCE );
	}

	/**
		interface EvidenceChangeListener
		@author Keith Cascio
		@since 102902
	*/
	public void evidenceChanged( EvidenceChangeEvent ECE )
	{
		//System.out.println( "NIF.evidenceChanged()" );

		if( ui == null || mySynchronization == null ) return;//disposed

		animate();

		//getTreeScrollPane().refreshPropertyChanges();
		ui.setDefaultCursor();
		StatusBar bar = ui.getStatusBar();
		if( bar != null ) bar.popText( STR_MSG_PROPAGATE, StatusBar.WEST );
		myPushedStatusMessage = null;
		myLastCalculatedPrE = null;
		if( myInferenceEngine != null ){
			ui.newPrE( this );
			updateCompilationDisplayMessage();
		}
	}

	/** @since 20051022 */
	public void updateCompilationDisplayMessage(){
		try{
			if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) ) return;
			if( ui.getActiveHuginNetInternalFrame() != NetworkInternalFrame.this ) return;
			if( myInferenceEngine == null ) return;
			ui.displayCompilationStatus( NetworkInternalFrame.this );
		}catch( Exception exception ){
			System.err.println( "Warning: NetworkInternalFrame.updateCompilationStatus() caught " + exception );
		}
	}

	/** @author Keith Cascio
		@since 072904 */
	public void animate(){
		animate( false );
	}

	/** @author Keith Cascio
		@since 072904 */
	public void animate( boolean force )
	{
		//System.out.println( "NetworkInternalFrame.animate( "+(force?"forced":"")+" )" );
		if( myInferenceEngine == null ) return;

		SamiamUserMode mode = getSamiamUserMode();
		if( force || ( mode.contains( SamiamUserMode.QUERY ) && mode.contains( SamiamUserMode.ANIMATE )) ){
			myAnimator.animate( myBeliefNetwork, myInferenceEngine );
		}
	}

	/** @author Keith Cascio
		@since 080904 */
	public void instantReplay(){
		//System.out.println( "NetworkInternalFrame.instantReplay()" );
		networkDisplay.recalculateActuals();

		if( myInferenceEngine == null ) return;

		SamiamUserMode mode = getSamiamUserMode();
		if( ( mode.contains( SamiamUserMode.QUERY ) && mode.contains( SamiamUserMode.ANIMATE )) ){
			myAnimator.instantReplay();
		}
	}

	public static final String STR_MSG_PROPAGATE = "propagating evidence...";

	/**
		@author Keith Cascio
		@since 053003
	*/
	public String getLastCalculatedPrE()
	{
		return myLastCalculatedPrE;
	}
	public void setLastCalculatedPrE( String newVal )
	{
		//System.out.println( "NIF.setLastCalculatedPrE("+newVal+")" );
		myLastCalculatedPrE = newVal;
	}
	protected String myLastCalculatedPrE;

	public static int INT_AUTO_ARRANGE_NODE_SPACING = (int)20;
	public static int INT_AUTO_ARRANGE_NETWORK_WIDTH = (int)750;

	public static final String HTML_MESSAGE_AUTO_ARRANGE =
		"<html>Auto-arrange will only move nodes positioned <b>exactly on top of each other</b>, so networks with unspecified position information are the best candidates. The only way to 'undo' this edit is to close your network and reopen it. Also note: currently "+UI.STR_SAMIAM_ACRONYM+" does not support saving node location changes to .dsl files.  Click 'OK' to proceed with auto-arrange.";

	/**
		@author Keith Cascio
		@since 070802
	*/
	public void doAutoArrange()
	{
		JPanel pnlMessage = new JPanel( new GridBagLayout() );
		JLabel lblMessage1 = new JLabel( "<html><nobr>Auto-arrange will only move nodes positioned <b>exactly on top of each other</b>," );
		JLabel lblMessage2 = new JLabel( "so networks with unspecified position information are the best candidates." );
		JLabel lblMessage3 = new JLabel( "The only way to 'undo' this edit is to close your network and reopen it." );
		JLabel lblMessage4 = new JLabel( "Also note: currently "+UI.STR_SAMIAM_ACRONYM+" does not support saving node location changes" );
		JLabel lblMessage5 = new JLabel( "to .dsl files.  Click 'OK' to proceed with auto-arrange." );

		//    attempt using <html> to line-wrap instead of multiple JLabels
		//JLabel lblMessage = new JLabel( HTML_MESSAGE_AUTO_ARRANGE );

		//    attempt SpringLayout - failed 060705 - because there seems not to be any way to leave the height unconstrained while constraining the width
		//SpringLayout layout = new SpringLayout();
		//JPanel pnlSize = new JPanel( layout );
		//pnlSize.add( lblMessage );
		//SpringLayout.Constraints springc = layout.getConstraints( lblMessage );
		//springc.setWidth(  Spring.constant( 1, 375, 375 ) );
		//springc.setHeight( Spring.constant( 16, 128, 10000 ) );
		//layout.putConstraint( SpringLayout.EAST,  pnlSize,      2, SpringLayout.EAST,  lblMessage );
		//layout.putConstraint( SpringLayout.SOUTH, pnlSize,      2, SpringLayout.SOUTH, lblMessage );

		//    attempt BoxLayout - failed 060705 - because JLabel returns an un-line-wrapped preferred size
		//JPanel pnlSize = new JPanel();
		//pnlSize.setLayout( new BoxLayout( pnlSize, BoxLayout.X_AXIS ) );
		//lblMessage.setMaximumSize( new Dimension( 375, 1000 ) );
		//lblMessage.setPreferredSize( new Dimension( 375, 14 ) );
		//pnlSize.add( lblMessage );

		JLabel lblNodeSpacing = new JLabel( "Space between nodes: " );
		WholeNumberField tfNodeSpacing = new WholeNumberField( INT_AUTO_ARRANGE_NODE_SPACING, 5, 1, 1000 );
		Dimension dim = tfNodeSpacing.getPreferredSize();
		dim.width = 75;
		tfNodeSpacing.setMinimumSize( dim );
		tfNodeSpacing.setMaximumSize( dim );
		JLabel lblNetworkWidth = new JLabel( "Approximate network width: " );
		WholeNumberField tfNetworkWidth = new WholeNumberField( INT_AUTO_ARRANGE_NETWORK_WIDTH, 5, 100, 1000000 );
		dim = tfNetworkWidth.getPreferredSize();
		dim.width = 75;
		tfNetworkWidth.setMinimumSize( dim );
		tfNetworkWidth.setMaximumSize( dim );

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.ipadx = 25;
		c.ipady = 0;
		c.fill = GridBagConstraints.NONE;

		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMessage.add( lblMessage1, c );
		pnlMessage.add( lblMessage2, c );
		pnlMessage.add( lblMessage3, c );
		pnlMessage.add( lblMessage4, c );
		pnlMessage.add( lblMessage5, c );
		//pnlMessage.add( pnlSize, c );
		pnlMessage.add( Box.createVerticalStrut( 16 ), c );

		c.gridwidth = 1;
		pnlMessage.add( lblNodeSpacing, c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMessage.add( tfNodeSpacing, c );

		c.gridwidth = 1;
		pnlMessage.add( lblNetworkWidth, c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMessage.add( tfNetworkWidth, c );

		pnlMessage.add( Box.createVerticalStrut( 16 ), c );

		//    debug
		//System.out.println( "lblMessage pref? " + lblMessage.getPreferredSize() );
		//System.out.println( "pnlSize    pref? " + pnlSize.getPreferredSize() );
		//System.out.println( "pnlMessage pref? " + pnlMessage.getPreferredSize() );

		int result = JOptionPane.showConfirmDialog( this, pnlMessage, "Confirm Irrevocable Changes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );
		if( result == JOptionPane.OK_OPTION )
		{
			int userspacing = tfNodeSpacing.getValue();
			int userwidth = tfNetworkWidth.getValue();
			if( userspacing < 1 ) userspacing = INT_AUTO_ARRANGE_NODE_SPACING;
			if( userwidth < 1 ) userwidth = INT_AUTO_ARRANGE_NETWORK_WIDTH;
			networkDisplay.autoArrange( userspacing, userwidth );
		}
	}

	/** @since 070802 */
	public static List toList( Point p )
	{
		List ret = new ArrayList( 2 );
		ret.add( new Integer( p.x ) );
		ret.add( new Integer( p.y ) );
		return ret;
	}

	/** @since 20020624 //
	public static Variable getAVariableNoisyOr( DisplayableBeliefNetwork bnet ){
		DisplayableFiniteVariable dVar = null;
		for( DFVIterator it = bnet.dfvIterator(); it.hasNext(); ){
			dVar = it.nextDFV();
			if( dVar.getDSLNodeType() == DSLNodeType.NOISY_OR ) return dVar;
		}
		return null;
	}*/

	public JInternalFrame showInternalFrame( String title, JComponent comp, Dimension size )
	{
		boolean resizable = true;
		boolean closable = true;
		boolean maximizable = true;
		boolean iconifiable = true;
		JInternalFrame internalFrame = new JInternalFrame( title, resizable, closable, maximizable, iconifiable);
		internalFrame.getContentPane().add( comp );
		desktopPane.add(internalFrame);
		internalFrame.pack();
		internalFrame.setDefaultCloseOperation(HIDE_ON_CLOSE);
		internalFrame.setBounds( new Rectangle( new Point( 25,25 ), size ) );
		return internalFrame;
	}

	/**
		@author Keith Cascio
		@since 091802
	*/
	protected void setInternalFrame( JInternalFrame internalFrame, int x, int y, int width, int height )
	{
		setInternalFrame( internalFrame, new Rectangle( x,y,width,height ) );
	}

	/**
		@author Keith Cascio
		@since 091802
	*/
	public void setInternalFrame( JInternalFrame internalFrame, Rectangle bounds )
	{
		internalFrame.setVisible( false );
		internalFrame.pack();
		internalFrame.setDefaultCloseOperation( HIDE_ON_CLOSE );
		internalFrame.setBounds( bounds );
		desktopPane.add(internalFrame);
	}

	/**
		@author Keith Cascio
		@since 012903
	*/
	protected void setInternalFrame( JInternalFrame internalFrame, Point upperleft )
	{
		setInternalFrame( internalFrame, new Rectangle( upperleft, internalFrame.getPreferredSize() ) );
	}

	public void addToDesktopPane( JInternalFrame infr) {
		desktopPane.add( infr);
	}

	/** @since 011404 */
	public boolean shouldRecompile( SamiamUserMode newMode )
	{
		if( FLAG_DEBUG_VERBOSE )
		{
			java.io.PrintStream stream = Util.STREAM_VERBOSE;
			stream.println( "NetworkInternalFrame.shouldRecompile():" );
			stream.println( newMode.contains( SamiamUserMode.NEEDSCOMPILE ) + " <- newMode.contains( SamiamUserMode.NEEDSCOMPILE )" );
			if( myInferenceEngine != null )
			{
				stream.println( !myInferenceEngine.getValid() + " <- !myInferenceEngine.getValid()" );
				boolean flagDyn = !ui.getDynamator().equals( myInferenceEngine.getDynamator() );
				stream.println( flagDyn + " <- !ui.getDynamator().equals( myInferenceEngine.getDynamator() )" );
				if( flagDyn )
				{
					stream.println( ui.getDynamator() + " <- ui.getDynamator()" );
					stream.println( myInferenceEngine.getDynamator() + " <- myInferenceEngine.getDynamator()" );
				}
			}
		}

		return ( newMode.contains( SamiamUserMode.NEEDSCOMPILE ) ||
				( (myInferenceEngine != null) &&
			(!myInferenceEngine.getValid()) || (!ui.getDynamator().equals(myInferenceEngine.getDynamator())) ) );
	}

	/** @since 20081024 */
	private WeakLinkedList mySamiamUserModals;

	/** @since 20081024 */
	public SamiamUserModal addSamiamUserModal( SamiamUserModal modal ){
		if( mySamiamUserModals == null ){ (mySamiamUserModals = new WeakLinkedList()).add( modal ); }
		else if( ! mySamiamUserModals.contains( modal ) ){ mySamiamUserModals.add( modal ); }
		return modal;
	}

	/** @since 20081024 */
	public boolean removeSamiamUserModal( SamiamUserModal modal ){
		if( mySamiamUserModals == null ){ return false; }
		else{ return mySamiamUserModals.remove( modal ); }
	}

	/** @since 20081024 */
	private int fireSamiamUserModals( SamiamUserMode mode ){
		if( mySamiamUserModals == null ){ return 0; }
		SamiamUserModal modal;
		int             count = 0;
		for( Iterator it = mySamiamUserModals.iterator(); it.hasNext(); ){
			if( (modal = (SamiamUserModal) it.next()) == null ){ it.remove(); }
			else{
				try{
					modal.setSamiamUserMode( mode );
					++count;
				}catch( Throwable thrown ){
					System.err.print( "warning: NIF.fireSamiamUserModals() caught " );
					System.err.println( thrown );
					if( Util.DEBUG_VERBOSE ){
						System.err.println( Util.STR_VERBOSE_TRACE_MESSAGE );
						thrown.printStackTrace( System.err );
					}
				}
			}
		}
		return count;
	}

	/** @since 20020617 */
	public void setSamiamUserMode( SamiamUserMode newMode )
	{
		boolean myQuerySetting = newMode.contains( SamiamUserMode.QUERY );
		boolean animateSetting = newMode.contains( SamiamUserMode.ANIMATE );
		if( myQuerySetting )
		{
			if( shouldRecompile( newMode ) )
			{
				newMode.setModeEnabled( SamiamUserMode.COMPILING, true );
				mySamiamUserMode = newMode;
				networkDisplay.setSamiamUserMode( newMode );
				if( ui.getActiveHuginNetInternalFrame() == this ) ui.quickModeWarning( mySamiamUserMode, this );
				recompile();
				return;
			}
			else if( myBeliefNetwork.thereExists( InferenceValidProperty.PROPERTY, InferenceValidProperty.PROPERTY.FALSE ) )
			{
				handleError( InferenceValidProperty.createErrorMessage( myBeliefNetwork ) );
				return;
			}
			else{//20080227
				try{
					InferenceEngine ie = getInferenceEngine();
					if( ie != null ){
						Component comp = ie.getControlPanel();
						if( comp != null ){
							JInternalFrame jif = myJIFInferenceEngineControl;
							String         title = ie.getDynamator().getDisplayName() + " control panel";
							if( jif == null ){
								boolean resizable   = false,
										closable    = false,
										maximizable = false,
										iconifiable = false;
								jif = myJIFInferenceEngineControl = new JInternalFrame( title, resizable, closable, maximizable, iconifiable );
								this.desktopPane.setLayer( jif, JDesktopPane.DEFAULT_LAYER.intValue() + 1 );
								this.desktopPane     .add( jif );
							}
							else{
								jif.setVisible( false );
								jif.setTitle( title );
								jif.getContentPane().removeAll();
							}
							jif.getContentPane().add( comp );
							jif.pack();
							jif.setLocation( 0x10, 0x10 );
							jif.setVisible(  true );
							jif.setSelected( true );
						}
					}
				}catch( Throwable thrown ){
					System.err.println( "warning: NetworkInternalFrame.setSamiamUserMode() caught " + thrown );
				}
			}
		}
		else
		{
			if( partialInternalFrame != null ) partialInternalFrame.setVisible( false );
			if( sensitivityInternalFrame != null ) sensitivityInternalFrame.setVisible( false );
			if( conflictInternalFrame != null ) conflictInternalFrame.setVisible( false );
			//recCondInternalFrame.setVisible( false );
			if( retractInternalFrame != null ) retractInternalFrame.setVisible( false );
			if( mapInternalFrame != null ) mapInternalFrame.setVisible( false );
            if( sdpInternalFrame != null ) sdpInternalFrame.setVisible( false );
			if( mpeInternalFrame != null ) mpeInternalFrame.setVisible( false );
			if( impactInternalFrame != null ) impactInternalFrame.setVisible( false );
			if( selectionInternalFrame != null ) selectionInternalFrame.setVisible( false );
			if( myCodeToolInternalFrame != null ) myCodeToolInternalFrame.setVisible( false );
			if( mySamiamUserMode.contains( SamiamUserMode.QUERY ) )
			{
				if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Leaving query mode, hiding all monitors." );
				ui.action_HIDEALL.actionPerformed( new ActionEvent(this, 0, "") );
			}
			try{
				if( myJIFInferenceEngineControl != null ){
					myJIFInferenceEngineControl.setVisible( false );
					myJIFInferenceEngineControl.getContentPane().removeAll();
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: NetworkInternalFrame.setSamiamUserMode() caught " + thrown );
			}
		}

		if( ui.getActiveHuginNetInternalFrame() == this ) ui.setSamiamUserMode( newMode );
		networkDisplay.setSamiamUserMode( newMode );
		if( sensitivityInternalFrame != null ) sensitivityInternalFrame.setSamiamUserMode( newMode );
		if( conflictInternalFrame    != null ) conflictInternalFrame.setSamiamUserMode( newMode );
		if( retractInternalFrame     != null ) retractInternalFrame.setSamiamUserMode( newMode );

		try{
			fireSamiamUserModals( newMode );
		}catch( Throwable thrown ){
			System.err.print( "warning: NIF.setSamiamUserMode() [fireSamiamUserModals()] caught " );
			System.err.println( thrown );
			if( Util.DEBUG_VERBOSE ){
				System.err.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				thrown.printStackTrace( System.err );
			}
		}

		mySamiamUserMode = newMode;
		setTitle( makeTitle() );

		if( !newMode.agreesModulo( mySamiamUserModePrevious, SamiamUserMode.EVIDENCEFROZEN ) ){
			if( myBeliefNetwork != null ){
				EvidenceController ec = myBeliefNetwork.getEvidenceController();
				if( ec != null ) ec.setFrozen( newMode.contains( SamiamUserMode.EVIDENCEFROZEN ) );
			}
		}

		if( animateSetting ){
			if( myAnimator == null ) myAnimator = new Animator( networkDisplay );
			if( myQuerySetting ) animate( true );
			else networkDisplay.recalculateActuals();
		}
		else if( (mySamiamUserModePrevious != null) && mySamiamUserModePrevious.contains( SamiamUserMode.ANIMATE ) ){
			networkDisplay.recalculateActuals();
		}

		boolean flagHandleHidden = ( mySamiamUserModePrevious == null ) ?
			newMode.contains( SamiamUserMode.HIDE ) :
		   !newMode.agreesModulo( mySamiamUserModePrevious, SamiamUserMode.HIDE );
		if( flagHandleHidden ){
			networkDisplay.handleHidden();
		}

		mySamiamUserModePrevious = new SamiamUserMode( newMode );
	}

	/** @since 062602 */
	protected String makeTitle()
	{
		boolean myQuerySetting = mySamiamUserMode.contains( SamiamUserMode.QUERY );
		boolean flagReadOnly = mySamiamUserMode.contains( SamiamUserMode.READONLY );
		boolean flagAnimate = mySamiamUserMode.contains( SamiamUserMode.ANIMATE );
		String strTitlePrefix = myQuerySetting ? "Query Mode" : "Edit Mode";
		String strReadOnly = flagReadOnly ? " (read only)" : "";
		String strAnimate = flagAnimate ? " (animation on)" : "";
		return strTitlePrefix + strReadOnly + strAnimate + " - [" + myFilePath + "]";
	}

	/** @since 062602 */
	public String getFileName()
	{
		return this.myFilePath;
	}

	/** @since 102402 */
	public String getFileNameSansPath()
	{
		return myFileName;
	}

	/** @since 021104 */
	protected void setFile( File newFile )
	{
		setFilePath( newFile.getPath() );
		setTitle( makeTitle() );
		menuItem.setText( this.myFilePath );
		myFlagNeverSaved = false;
	}
	protected void setFilePath( String path )
	{
		myFilePath = path;
		myFileName = NetworkIO.extractFileNameFromPath( myFilePath );
		NetworkInternalFrame.this.setSMILEMode();
	}

	/** @since 061702 */
	public SamiamUserMode getSamiamUserMode()
	{
		return new SamiamUserMode( mySamiamUserMode );
	}

	/** @since 030702 */
	public DSLNodeType getCPTType( DisplayableFiniteVariable var )
	{
		return var.getDSLNodeType();
	}

	/** @since 031902 */
	/*
	public java.util.List getNoisyOrWeights( DisplayableFiniteVariable var )
	{
		return var.getNoisyOrWeights();
	}*/

	public UI getParentFrame() {
		return ui;
	}

	public DisplayableBeliefNetwork getBeliefNetwork() {
		return myBeliefNetwork;
	}

	/** @since 061704 */
	public ApproxEngine getApproxEngine() {
		return myApproxEngine;
	}

	/** @since 061704 */
	public void setApproxEngine( ApproxEngine ae )
	{
		myApproxEngine = ae;
		if( myBeliefNetwork == null ) return;

		DisplayableFiniteVariable dVar;
		NodeLabel nl;
		Monitor monitor;
		for( Iterator it = myBeliefNetwork.iterator(); it.hasNext(); ){
			dVar = (DisplayableFiniteVariable) it.next();
			nl = dVar.getNodeLabel();
			if( nl != null ){
				monitor = nl.getEvidenceDialog();
				if( monitor != null ) monitor.setApprox( myApproxEngine );
			}
		}
	}

	public InferenceEngine getInferenceEngine() {
		return myInferenceEngine;
	}

	/** @since 20091226 */
	public InferenceEngine getCanonicalInferenceEngine(){
		return myInferenceEngine == null ? null : myInferenceEngine.canonical();
	}

	/** @since 20030117 */
	protected void killInferenceEngine(){
	  //System.out.println( "NIF.killInferenceEngine(), size? " + myBeliefNetwork.size() );
		myLastCalculatedPrE = null;

		if( myInferenceEngine != null ){
			EvidenceController ec = myBeliefNetwork.getEvidenceController();
			ec.removePriorityEvidenceChangeListener( myInferenceEngine );
			ec.removeEvidenceChangeListener(         myInferenceEngine );

			myInferenceEngine.die();
		  //System.out.println( "   ...("+myInferenceEngine.getClass().getSimpleName()+")myInferenceEngine.die(), size? " + myBeliefNetwork.size() );
			myInferenceEngine = null;
		}
	}

	/** @since 121003 */
	protected void killDynamatorsState()
	{
		killDynamatorsState( null );
	}

	protected void killDynamatorsState( Dynamator toExclude )
	{
		//System.out.println( "NIF.killDynamatorsState( "+toExclude+" )" );
		Dynamator next;
		for( Iterator it = ui.getDynamators().iterator(); it.hasNext(); ){
		 	next = (Dynamator)it.next();
			if( ! next.equals( toExclude ) ) next.killState( myBeliefNetwork );
		}
	}

	/** @since 022503 */
	protected void clearRCSettings( boolean clearDtree, boolean clearRC )
	{
		//System.out.println( "NetworkInternalFrame.clearRCSettings( "+clearDtree+", "+clearRC+" )" );

		Settings settings = edu.ucla.belief.recursiveconditioning.RCEngineGenerator.getSettings( myBeliefNetwork, false );
		if( settings != null )
		{
			if( clearDtree ) settings.setDtree( null );
			if( clearRC ) settings.setRC( null );
		}
	}

	/** @since 011503 */
	public PartialDerivativeEngine getPartialDerivativeEngine()
	{
		if( myInferenceEngine instanceof PartialDerivativeEngine )
		{
			return (PartialDerivativeEngine) myInferenceEngine;
		}
		else return null;
	}

	public DisplayableFiniteVariable[] getVariableArray()
	{
		DisplayableFiniteVariable[] dvars = new DisplayableFiniteVariable[ myBeliefNetwork.size() ];

		int count = (int)0;
		for( DFVIterator it = myBeliefNetwork.dfvIterator(); it.hasNext(); ){
			dvars[count++] = it.nextDFV();
		}

		Arrays.sort( dvars, VariableComparator.getInstance() );
		return dvars;
	}

	public VariableComboBox createVariableComboBox()
	{
		VariableComboBox varBox = new VariableComboBox( myBeliefNetwork );
		varBoxes.add(varBox);
		return varBox;
	}

	/** @since 061903 */
	protected void refreshVariableComboBoxes()
	{
		ComboBoxModel model;// = new SortedListModel( myBeliefNetwork, null, myBeliefNetwork.size() );
		VariableComboBox next;
		Object selected;
		for( Iterator it = varBoxes.iterator(); it.hasNext(); )
		{
			next = (VariableComboBox) it.next();
			selected = next.getSelectedItem();

			model = new SortedListModel( myBeliefNetwork, null, myBeliefNetwork.size() );
			next.setModel( model );
			next.recalculateDisplayValues( getBeliefNetwork() );

			if( getBeliefNetwork().contains( selected ) ) next.setSelectedItem( selected );
			else if( next.getItemCount() > (int)0 ) next.setSelectedIndex( (int)0 );
			else next.setSelectedItem( null );
		}
	}

	public VariableInstance[] getVariableInstances(	DisplayableFiniteVariable var )
	{
		return (VariableInstance[])myMapDvarsToVariableInstanceArrays.get(var);
	}

	public VariableInstance getVariableInstance( DisplayableFiniteVariable var, Object instance )
	{
		return getVariableInstances(var)[var.index(instance)];
	}

	public JMenuItem getMenuItem() {
		return menuItem;
	}

	public NetworkDisplay getNetworkDisplay() {
		return networkDisplay;
	}

	/** Save the Hugin Net file to the specified file name. */
	public boolean saveTo( boolean asCopy, File ofile, FileType type ) throws Exception
	{
		if( myBeliefNetwork.isGenieNet() && !Util.DEBUG ){
			throw new IllegalStateException( "Error saving .net:\nconversion from SMILE type to .net currently not supported.\nNo file written." );
		}

		boolean ret = type.save( ofile, getBeliefNetwork() );
		if( ! asCopy ) doSavePostOp( ofile, true );
		return ret;
	}

	/** @since 021104 */
	private void doSavePostOp( File ofile, boolean flagForceSetFile )
	{
		String oldPath = this.myFilePath;
		if( flagForceSetFile || myFlagNeverSaved ){
			setFile( ofile );
			myFlagNeverSaved = false;
		}
		if( oldPath != null && oldPath.equals( ofile.getPath() ) ) myBeliefNetwork.setUserEnumPropertiesModified( false );
	}

	/** @since 20091225 */
	public interface MonitorsHelper{
		public DisplayableFiniteVariable    help( DisplayableFiniteVariable var );
		public boolean                    effect();
		public boolean                   display();
	}

	/** @since 20091225 */
	public class MonitorsForType implements MonitorsHelper{
		public   MonitorsForType( DiagnosisType type, boolean show ){
			this.type =                         type;
			this.show =                                       show;
		}
		public boolean                    effect(){ return effect; }
		public boolean                   display(){ return show; }
		public DisplayableFiniteVariable    help( DisplayableFiniteVariable var ){
			if( var.getDiagnosisType() == type ){
				var.getNodeLabel().setEvidenceDialogShown( show );
				effect = true;
			}
			return var;
		}
		public DiagnosisType type;
		public boolean       show, effect;
	}

	/** @since 20091225 */
	public class MonitorsForSelection implements MonitorsHelper{
		public   MonitorsForSelection( boolean selected, boolean show ){
			this.selected =                    selected;
			this.show     =                                      show;
		}
		public boolean                    effect(){ return effect; }
		public boolean                   display(){ return show && selected; }
		public DisplayableFiniteVariable    help( DisplayableFiniteVariable var ){
			NodeLabel label = var.getNodeLabel();
			if( label.isSelected() == selected ){
				label.setEvidenceDialogShown(   show );
				effect = true;
			}else if( destructive ){
				label.setEvidenceDialogShown( ! show );
				effect = true;
			}
			return var;
		}
		public boolean selected, show, effect, destructive = ! NetworkDisplay.FLAG_ADDITIVE_SHOW_MONITOR_SEMANTICS;
	}

	/** @since 20091225 */
	public class MonitorsForAll implements MonitorsHelper{
		public   MonitorsForAll( boolean show ){
			this.show                  = show;
		}
		public boolean                    effect(){ return true; }
		public boolean                   display(){ return show; }
		public DisplayableFiniteVariable    help( DisplayableFiniteVariable var ){
			var.getNodeLabel().setEvidenceDialogShown( show );
			return var;
		}
		public boolean show;
	}

	/** @since 20020306 */
	private NetworkInternalFrame monitorsImpl( MonitorsHelper helper, String message, String console0, String console1 ){
		if( ! getSamiamUserMode().contains( SamiamUserMode.QUERY ) ){
			throw new RuntimeException( "error: called NetworkInternalFrame.monitorsImpl() not in SamiamUserMode.QUERY" ); }

		StatusBar                      bar = null;
		if( helper.display() ){
			try{
				bar = ui.getStatusBar();
				console.println( console0 );
				if( bar != null ){ bar.pushText( myPushedStatusMessage = message, StatusBar.WEST ); }
			}catch( Throwable thrown ){
				System.err.println( "warning: NetworkInternalFrame.monitorsImpl() caught " + thrown );
			}
		}

		Throwable                   caught = null;
		try{
			for( DFVIterator it = myBeliefNetwork.dfvIterator(); it.hasNext(); ){ helper.help( it.nextDFV() ); }
		}catch( Throwable thrown ){ caught = thrown; }

		if( helper.effect() ){ try{ networkDisplay.recalculateDesktopSize(); }catch( Throwable thrown ){ System.err.println( "warning: NetworkInternalFrame.monitorsImpl() caught " + thrown ); } }

		if( helper.display() ){
			try{
				if( bar != null ){ bar.popText( message, StatusBar.WEST ); }
				myPushedStatusMessage = null;
				console.println( console1 );
				for( int i=0; (myInferenceEngine == null) && (i < 8); i++ ){ Thread.sleep( 0x40 ); }
				myInferenceEngine.printInfoPropagation( console );
			}catch( Throwable thrown ){
				System.err.println( "warning: NetworkInternalFrame.monitorsImpl() caught " + thrown );
			}
		}

		if( caught != null ){
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				caught.printStackTrace( Util.STREAM_VERBOSE );
			}
			handleError( caught );
		}

		return this;
	}

	/** @since 20020306 */
	public NetworkInternalFrame setMonitorsForTypeShown( DiagnosisType type, boolean show ){
		return monitorsImpl(
		  new MonitorsForType( type, show ),
		  "calculating marginal probabilities for " + type + "...",
		                "Showing monitors for all " + type + " variables.",
		           "Done showing monitors for all " + type + " variables." );
	}

	/** @since 20020920 */
	public NetworkInternalFrame setMonitorsVisible( boolean selected, boolean show ){
		return monitorsImpl(
		  new MonitorsForSelection( selected, show ),
		  "calculating marginal probabilities...",
		                "Showing monitors for all selected variables.",
		           "Done showing monitors for all selected variables." );
	}

	/** @since 20020311 */
	public NetworkInternalFrame setAllMonitorsVisible( boolean show ){
		return monitorsImpl(
		  new MonitorsForAll( show ),
		  "calculating marginal probabilities...",
		                "Showing monitors for all variables.",
		           "Done showing monitors for all variables." );
	}

	/** @since 20040521 */
	public Collection getVariablesMonitorsVisible( boolean visible )
	{
		LinkedList ret = new LinkedList();
		DisplayableFiniteVariable dVar = null;
		for( DFVIterator it = myBeliefNetwork.dfvIterator(); it.hasNext(); )
		{
			dVar = it.nextDFV();
			if( dVar.getNodeLabel().isEvidenceDialogShown() == visible ) ret.add( dVar );
		}
		return ret.isEmpty() ? (Collection)Collections.EMPTY_SET : (Collection)ret;
	}

	/** @since 20021204 */
	public boolean areMonitorsForTypeShown( DiagnosisType type )
	{
		boolean typePresent = false;

		DisplayableFiniteVariable dVar = null;
		DiagnosisType tempType = null;
		for( DFVIterator it = myBeliefNetwork.dfvIterator(); it.hasNext(); )
		{
			dVar = it.nextDFV();
			tempType = dVar.getDiagnosisType();
			if( tempType == type )
			{
				typePresent = true;
				if( !dVar.getNodeLabel().isEvidenceDialogShown() ) return false;
			}
		}

		return typePresent;
	}

	/** @since 20080529 */
	public int setCPTMonitorsVisible( boolean selected, boolean show ){
		boolean hadEffect = false;

		String    message = "showing cpt monitors...";
		StatusBar bar     = ui.getStatusBar();
		if( show && selected ){
			console.println( "Showing cpt monitors for all selected variables." );
			if( bar != null ){ bar.pushText( (myPushedStatusMessage = message), StatusBar.WEST ); }
		}

		int                       count = 0;
		NodeLabel                 label = null;
		Bridge2Tiger              b2t   = Bridge2Tiger.Troll.solicit();
		for( DFVIterator it = myBeliefNetwork.dfvIterator(); it.hasNext(); ){
			label = it.nextDFV().getNodeLabel();
			if( label.isSelected() == selected ){
				b2t.setCPTMonitorShown( label, true );
				hadEffect = true;
				++count;
			}
			else if( ! NetworkDisplay.FLAG_ADDITIVE_SHOW_MONITOR_SEMANTICS ){
				b2t.setCPTMonitorShown( label, ! show );
				hadEffect = true;
			}
		}

	  //if( hadEffect ){ networkDisplay.recalculateDesktopSize(); }

		if( show && selected ){
			if( bar != null ){ bar.popText( message, StatusBar.WEST ); }
			myPushedStatusMessage = null;
			console.println( "Done showing cpt monitors for all selected variables." );
		}

		return count;
	}

	/** Export the Hugin Net to a DSL format file of the specified file name.
		@author keith cascio
		@since  20020215 */
	public void saveToSMILEType( boolean asCopy, File oldFile, File newFile, FileType smileType ) throws IllegalStateException, IOException
	{
		saveToSMILEType( asCopy, oldFile, newFile, smileType, false );
	}

	/**
		A version of saveToSMILEType() that allows the caller to override
		the prohibition against saving a belief network to .DSL
		format when there have been network structure changes.  Any
		network structure changes will be lost.  Only changes to
		probability values will be saved.

		@param oldFile A File object representing the original Genie .dsl
			description of this network.
		@param newFile A File object representing the file to write.
		@param smileType See SMILE C++ header file, 'constants.h'
		@param override Controls whether or not this method throws an
			IllegalStateException if the user has changed the
			structure of this network.
		@throws IllegalStateException Thrown if the user has changed
			the structure of this network and override == false.
		@throws IOException Thrown if there is a problem with the files.
		@author keith cascio
		@since  20020621
	*/
	public void saveToSMILEType( boolean asCopy, File oldFile, File newFile, FileType smileType, boolean override ) throws IllegalStateException, IOException
	{
		String typeName = smileType.getName();

		if( !override && myNetStructureHasChanged ) throw new IllegalStateException( "Error saving in "+typeName+" format.  Changes to network structure not allowed.  No file written." );

		FileType typeOldFile = FileType.getTypeForFile( oldFile );
		if( typeOldFile == null || !typeOldFile.isSMILEType() ) throw new IllegalStateException( "Error saving in "+typeName+" format.  Original network incompatible format (not SMILE type).  No file written." );

		Set modifiedFVars = new TreeSet();
		DisplayableFiniteVariable tempDVar = null;
		for( DFVIterator it = myBeliefNetwork.dfvIterator(); it.hasNext(); )
		{
			tempDVar = it.nextDFV();
			if( tempDVar.isUserModified() ) modifiedFVars.add( tempDVar );
		}

		//Map tempDVarMap = new HashMap();
		//archiveDisplayableFiniteVariables( modifiedFVars, tempDVarMap );

		boolean okayToRetitle = (smileType == typeOldFile);

		if( (new SMILEReader()).updateSMILE( oldFile, typeOldFile, newFile, smileType, myBeliefNetwork, modifiedFVars ) )
		{
			if( ! asCopy ) doSavePostOp( newFile, okayToRetitle );
			if( (!okayToRetitle) && (ui!=null) && (smileType!=null)) ui.showMessageDialog( "To edit the saved "+smileType.getName()+" file,\nopen "+newFile.getPath(), "Save As: Warning", JOptionPane.WARNING_MESSAGE );
		}
		else
		{
			JOptionPane.showMessageDialog(	getTopLevelAncestor(),
						"Error saving in "+typeName+" format. No file written. (The network may not be of SMILE origin.)",
						"Save Error",
						JOptionPane.ERROR_MESSAGE );
		}

		//restoreDisplayableFiniteVariables( modifiedFVars, tempDVarMap );
	}

	private void selectInternalFrame( JInternalFrame internalFrame ){
		try{
			NetworkInternalFrame.this.ui.select( internalFrame );
			internalFrame.setVisible( true );
		}catch( Exception e ){
			System.err.println( "Warning: NetworkInternalFrame.selectInternalFrame() caught " + e );
		}
	}

	/** Invoked when the display network tool is called. */
	public void networkTool() {
		selectInternalFrame(networkInternalFrame);
	}

	/** Invoked when the partial derivatives tool is called. */
	public void partialTool() {
		if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.partialTool() not in SamiamUserMode.QUERY" );
		getPartialInternalFrame();
		partialInternalFrame.reInitialize();
		selectInternalFrame(partialInternalFrame);
	}

	/** @author Hei Chan
		@since 20040610 */
	public void approxTool()
	{
		if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.approxTool() not in SamiamUserMode.QUERY" );

		getApproxInternalFrame();
		selectInternalFrame( getApproxInternalFrame() );
	}

	/** Invoke the sensitivity analysis tool. */
	public NetworkInternalFrame sensitivityTool( boolean arrange )
	{
		if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.sensitivityTool() not in SamiamUserMode.QUERY" );

		getSensitivityInternalFrame();
		try{
			sensitivityInternalFrame.reInitialize();
		}catch( Throwable thrown ){
			System.err.println( "warning: NetworkInternalFrame.sensitivityTool() caught " + thrown );
			thrown.printStackTrace();
		}
		try{
			selectInternalFrame( sensitivityInternalFrame );
		}catch( Throwable thrown ){
			System.err.println( "warning: NetworkInternalFrame.sensitivityTool() caught " + thrown );
			thrown.printStackTrace();
		}
		if( arrange ){
			try{
				sensitivityInternalFrame.action_BESTWINDOWARRANGEMENT.actionP( NetworkInternalFrame.this );
			}catch( Throwable thrown ){
				System.err.println( "warning: NetworkInternalFrame.sensitivityTool() caught " + thrown );
				thrown.printStackTrace();
			}
		}

		return this;
	}

	/** @since 072402 */
	public void recursiveConditioningTool()
	{
		synchronized( mySynchronization )
		{
			if( myBeliefNetwork.size() < Dynamator.INT_MINIMUM_VARIABLES ){
				ui.showErrorDialog( "Please add at least "+(Dynamator.INT_MINIMUM_VARIABLES-myBeliefNetwork.size())+" variable(s) to the network before running recursive conditioning." );
				return;
			}
			getRCInternalFrame();
			/*if( !myRCIF.isVisible() ) */selectInternalFrame( myRCIF );
		}
	}

	//public void debugRecursiveConditioningTool()
	//{
		//if( recCondInternalFrame == null ) System.err.println( "NetworkInternalFrame.debugRecursiveConditioningTool() failed," );
		//else selectInternalFrame( recCondInternalFrame );
	//}

	/** Invoked when the MAP computation tool is called. */
	public void mapTool() {
		//if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
		//	throw new RuntimeException( "Error: called NetworkInternalFrame.mapTool() not in SamiamUserMode.QUERY" );
		getMAPInternalFrame();
		selectInternalFrame(mapInternalFrame);
		mapInternalFrame.reInitialize();
	}

    /** Invoked when the SDP computation tool is called */

    public void sdpTool() {
        getSDPInternalFrame();
		selectInternalFrame(sdpInternalFrame);
		sdpInternalFrame.reInitialize();

    }
    
	/** Invoked when the MPE computation tool is called. */
	public void mpeTool() {
		if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.mpeTool() not in SamiamUserMode.QUERY" );
		getMPEInternalFrame();
		if ( !mpeInternalFrame.isVisible() ) mpeInternalFrame.reInitialize();
		selectInternalFrame(mpeInternalFrame);
	}

	/**
		@author Keith Cascio
		@since 073003
	*/
	public void impactTool()
	{
		if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.impactTool() not in SamiamUserMode.QUERY" );
		getImpactInternalFrame();
		if ( !impactInternalFrame.isVisible() ) impactInternalFrame.reInitialize();
		selectInternalFrame(impactInternalFrame);
	}

	/** @since 082503 */
	public void selectionTool()
	{
		getSelectionInternalFrame();
		if ( !selectionInternalFrame.isVisible() ) selectionInternalFrame.reInitialize();
		selectInternalFrame( selectionInternalFrame );
	}

	/** @since 071304 */
	public void selectionTool( EnumProperty property )
	{
		if( property != null ){
			getSelectionInternalFrame();
			selectionInternalFrame.getEnumPropertyEditPanel().setProperty( property );
		}
		selectionTool();
	}

	/** Invoked when the EM learning tool is called. */
	public void emTool() {
		emTool( (JOptionResizeHelper.JOptionResizeHelperListener)null );
	}

	/** @since 021505 */
	public void emTool( JOptionResizeHelper.JOptionResizeHelperListener listener ){
		if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.emTool() not in SamiamUserMode.QUERY" );

		//Variable var = getAVariableNoisyOr( myBeliefNetwork );
		Variable var = StandardNodeImpl.thereExists( myBeliefNetwork, DSLNodeType.NOISY_OR );
		if( var == null )
		{
			EMLearningDlg learnDlg = new EMLearningDlg( getParentFrame(), this, listener );
			learnDlg.setVisible( true );
		}
		else ui.showErrorDialog( "Currently, "+UI.STR_SAMIAM_ACRONYM+" does not support learning\nnetworks containing nodes of type Noisy Or.\nThis network contains Noisy Or node " + var + "." );
	}

	/** @since 032205 */
	public void codeToolCPTDemo()
	{
		CPTCoder coder = new CPTCoder();
		getCodeToolInternalFrame().setCodeGenius( coder );
		myCodeToolInternalFrame.setVisible( true );
	}

	/** @since 050604 */
	public void codeToolNetwork()
	{
		if( myBeliefNetwork.size() > (int)8 ){
			String title = "Large network";
			String message = "Are you sure you would like to run " + CodeToolInternalFrame.STR_DISPLAY_NAME + "\non this network ("+Integer.toString(myBeliefNetwork.size())+" nodes)?\nThe output will be inconveniently large.";
			int result = JOptionPane.showConfirmDialog( this, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );
			if( result != JOptionPane.OK_OPTION ) return;
		}

		ModelCoder mc = new ModelCoder( myBeliefNetwork, getFileName() );

		Converter converter = null;
		if( getCanonicalInferenceEngine() instanceof WrapperInferenceEngine ){
			converter = ((WrapperInferenceEngine) getCanonicalInferenceEngine()).getJointWrapper().getConverter();
		}
		if( converter != null ){ mc.setConverter( converter ); }

		//il2.model.BayesianNetwork il2network = converter.convert( myBeliefNetwork );

		getCodeToolInternalFrame().setCodeGenius( mc );
		myCodeToolInternalFrame.setVisible( true );
	}

	/** @since 20040520 */
	public void codeToolProbability()
	{
		if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.codeToolProbability() not in SamiamUserMode.QUERY" );

		try{
			String pathInput = getFileName();
			if( getCodeToolInternalFrame().failOnMissingNetworkFile( pathInput ) ) return;

			ProbabilityQueryCoder pqc = new ProbabilityQueryCoder();

			pqc.setPathInputFile( getFileName() );
			pqc.setBeliefNetwork( myBeliefNetwork );
			pqc.setInferenceEngine( myInferenceEngine );
			pqc.setDynamator( ui.getDynamator().getCanonicalDynamator() );
			pqc.setVariables( getVariablesMonitorsVisible( true ) );
			pqc.setEvidence( myBeliefNetwork.getEvidenceController().evidence() );

			getCodeToolInternalFrame().setCodeGenius( pqc );
			myCodeToolInternalFrame.setVisible( true );
		}catch( Exception exception ){
			String msg = exception.getMessage();
			if( msg == null ) msg = exception.toString();
			this.getParentFrame().showErrorDialog( msg );
		}
	}

	/** @since 20040217 */
	public void simulateTool()
	{
		Variable var = StandardNodeImpl.thereExists( myBeliefNetwork, DSLNodeType.NOISY_OR );
		if( var == null ){
			SimulationPanel sp           = new SimulationPanel( this );
			String          strOption    = "Close";
			Object[]        arrayOptions = new Object[] { strOption };
			Icon            icon         = MainToolBar.getIcon( "Simulate16.gif" );
			JOptionPane.showOptionDialog( this, sp, "Simulate: Generate Hugin Case Files", (int)1, JOptionPane.PLAIN_MESSAGE, icon, arrayOptions, strOption );
		}
		else ui.showErrorDialog( "Currently, "+UI.STR_SAMIAM_ACRONYM+" does not support simulation given\na network that contains nodes of type Noisy Or.\nThis network contains Noisy Or node " + var + "." );
	}

	/** Invoked when the evidence conflict tool is called. */
	public void conflictTool() {
		if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.conflictTool() not in SamiamUserMode.QUERY" );
		getConflictInternalFrame();
		selectInternalFrame(conflictInternalFrame);
	}

	/** Invoked when the evidence retraction tool is called. */
	public void retractTool() {
		if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.retractTool() not in SamiamUserMode.QUERY" );
		getRetractInternalFrame();
		selectInternalFrame(retractInternalFrame);
	}

	/** Invoked when the show monitors tool is called. */
	public void monitorTool() {
		if( !getSamiamUserMode().contains( SamiamUserMode.QUERY ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.monitorTool() not in SamiamUserMode.QUERY" );
		//networkDisplay.showEvidenceDialogs();
		setMonitorsVisible( true, true );
	}

	public void addNode() {
		if( !getSamiamUserMode().contains( SamiamUserMode.EDIT ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.addNode() not in SamiamUserMode.EDIT" );
		networkDisplay.createNewNode();
	}

	public void deleteNodes() {
		if( !getSamiamUserMode().contains( SamiamUserMode.EDIT ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.deleteNodes() not in SamiamUserMode.EDIT" );
		networkDisplay.deleteSelectedNodes();
	}

	public void addEdge() {
		if( !getSamiamUserMode().contains( SamiamUserMode.EDIT ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.addEdge() not in SamiamUserMode.EDIT" );
		networkDisplay.createNewEdge();
	}

	public void deleteEdge() {
		if( !getSamiamUserMode().contains( SamiamUserMode.EDIT ) )
			throw new RuntimeException( "Error: called NetworkInternalFrame.deleteEdge() not in SamiamUserMode.EDIT" );
		networkDisplay.userDeleteEdge();
	}

	/** @since 20080219 */
	public void replaceEdge(){
		if( ! getSamiamUserMode().contains( SamiamUserMode.EDIT ) ){
		  throw new RuntimeException( "Error: called NetworkInternalFrame.replaceEdge() not in SamiamUserMode.EDIT" ); }
		networkDisplay.userReplaceEdge();
	}

	/** @since 20080219 */
	public void recoverEdge(){
		if( ! getSamiamUserMode().contains( SamiamUserMode.EDIT ) ){
		  throw new RuntimeException( "Error: called NetworkInternalFrame.recoverEdge() not in SamiamUserMode.EDIT" ); }
		networkDisplay.userRecoverEdge();
	}

	/** @since 20071211 */
	public NetworkInternalFrame copyCPT(){
		networkDisplay.initiateCPTCopy();
		return this;
	}

	public void changePackageOptions( SamiamPreferences pref )
	{
		if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\n\n(" + this + ").changePackageOptions()" );

		PreferenceGroup globalPrefs = pref.getPreferenceGroup( SamiamPreferences.PkgDspNme );
		PreferenceGroup netPrefs = pref.getPreferenceGroup( SamiamPreferences.NetDspNme );
		boolean forceNetPrefs = false;
		PreferenceGroup treePrefs = pref.getPreferenceGroup( SamiamPreferences.TreeDspNme );
		boolean forceTreePrefs = false;
		PreferenceGroup monitorPrefs = pref.getPreferenceGroup( SamiamPreferences.MonitorsDspNme );



		if( globalPrefs.isRecentlyCommittedValue() )
		{
			if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\tChanges to global preferences." );

			myBeliefNetwork.setAutoCPTInvalidation( ((Boolean) pref.getMappedPreference( SamiamPreferences.autoCPTInvalid ).getValue()).booleanValue() );

			//Perhaps the user changed the setting for
			//displaying node ids vs. node labels
			if( pref.getMappedPreference( SamiamPreferences.displayNodeLabelIfAvail ).isRecentlyCommittedValue() )
			{
				for( Iterator it = getBeliefNetwork().iterator(); it.hasNext(); )
				{
					((DisplayableFiniteVariable)it.next()).changePackageOptions();
				}
				forceNetPrefs = true;
				forceTreePrefs = true;
			}

			if( pref.getMappedPreference( SamiamPreferences.STR_LOOKANDFEEL_CLASSNAME ).isRecentlyCommittedValue() )
			{
				forceTreePrefs = true;
			}

			if( partialInternalFrame != null ) partialInternalFrame.changePackageOptions( globalPrefs );
			if( sensitivityInternalFrame != null ) sensitivityInternalFrame.changePackageOptions( globalPrefs );
			if( conflictInternalFrame != null ) conflictInternalFrame.changePackageOptions( globalPrefs );
			if( retractInternalFrame != null ) retractInternalFrame.changePackageOptions( globalPrefs );

			refreshVariableComboBoxes();
		}

		if( netPrefs.isRecentlyCommittedValue() || forceNetPrefs || monitorPrefs.isRecentlyCommittedValue() || globalPrefs.isRecentlyCommittedValue() )
		{
			if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\tChanges to network display preferences." );
			networkDisplay.changePackageOptions( mySamiamPreferences );
		}

		if( treePrefs.isRecentlyCommittedValue() || forceTreePrefs )
		{
			if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\tChanges to tree display preferences." );

			//evidenceTreeScrollPane.changePackageOptions(opt.trOpt);
			evidenceTreeScrollPane.changePackageOptions();
		}
	}

	public void addEvidenceChangeListener( EvidenceChangeListener listener )
	{
		myBeliefNetwork.getEvidenceController().addEvidenceChangeListener( listener );
		//evidenceChangeListeners.add(listener);
	}

	public boolean removeEvidenceChangeListener( EvidenceChangeListener listener ) {
		return myBeliefNetwork.getEvidenceController().removeEvidenceChangeListener( listener );
		//evidenceChangeListeners.remove(listener);
	}

	//private void fireEvidenceChanged( EvidenceChangeEvent ECE ) {
	//	for (int i = 0; i < evidenceChangeListeners.size(); i++)
	//		((EvidenceChangeListener)evidenceChangeListeners.get(i)).evidenceChanged( ECE );
	//}

	public void addCPTChangeListener(CPTChangeListener listener) {
		cptChangeListeners.add(listener);
	}

	public boolean removeCPTChangeListener(CPTChangeListener listener) {
		return cptChangeListeners.remove(listener);
	}

	/** @since 080902 */
	public void addRecompilationListener( RecompilationListener l ){
		myRecompilationListeners.add( l );
	}

	/** @since 080902 */
	public boolean removeRecompilationListener( RecompilationListener l ){
		return myRecompilationListeners.remove( l );
	}

	protected WeakLinkedList myRecompilationListeners = new WeakLinkedList();

	/** @since 080902 */
	protected void fireNetworkRecompiled()
	{
		RecompilationListener next;
		for( ListIterator it = myRecompilationListeners.listIterator(); it.hasNext(); )
		{
			next = (RecompilationListener)it.next();
			if( next == null ) it.remove();
			else next.networkRecompiled();
		}
	}

	public static boolean DEBUG_CPTCHANGED = Util.DEBUG_VERBOSE;

	public void fireCPTChanged( CPTChangeEvent evt )
	{
		CPTChangeListener temp = null;
		int i = 0;
		for( ListIterator it = cptChangeListeners.listIterator(); it.hasNext(); i++ )
		{
			temp = (CPTChangeListener) it.next();
			if( temp == null ) it.remove();
			else{
				if( DEBUG_CPTCHANGED ) Util.STREAM_VERBOSE.println( temp.getClass().getName() + ".cptChanged()" );
				temp.cptChanged( evt );
			}
		}
		updateCompilationDisplayMessage();
		animate();
	}

	public void addNetStructureChangeListener( NetStructureChangeListener listener ) {
		netStructureChangeListeners.add(listener);
	}

	public boolean removeNetStructureChangeListener( NetStructureChangeListener listener ) {
		if( netStructureChangeListeners != null ){ return netStructureChangeListeners.remove(listener); }
		else return false;
	}

	private void fireNetStructureChanged( NetStructureEvent event )
	{
		NetStructureChangeListener next;
		for( ListIterator it = netStructureChangeListeners.listIterator(); it.hasNext(); ){
			next = (NetStructureChangeListener) it.next();
			if( next == null ) it.remove();
			else next.netStructureChanged( event );
		}
	}

	/** @since 062802 */
	protected void fireNetStructureChanged( java.util.List events )
	{
		if( events != null )
		{
			for( Iterator it = events.iterator(); it.hasNext(); )
			{
				fireNetStructureChanged( (NetStructureEvent)it.next() );
			}
		}
	}

	/*
	public void cptChanged()
	{
		SamiamUserMode mode = getSamiamUserMode();
		mode.setModeEnabled( SamiamUserMode.NEEDSCOMPILE, true );
		setSamiamUserMode( mode );
		//recompile();
	}*/

	/**
		@author Keith Cascio
		@since 072902
	*/
	public void setCPT( FiniteVariable var )
	{
		//System.out.println( "NetworkInternalFrame.setCPT( "+var+" )" );

		//if( getCanonicalInferenceEngine() instanceof JoinTreeInferenceEngine )
		//{
			//System.out.println( "call InferenceEngine.setCPT()" );
			//long start_ms = System.currentTimeMillis();
		//	myInferenceEngine.setCPT( var, var.getCPTShell().getCPT().dataclone() );
			//long end_ms = System.currentTimeMillis();
			//long elapsed_ms = end_ms - start_ms;
			//System.out.println( "DONE InferenceEngine.setCPT(): " + (double)elapsed_ms/(double)1000 + "seconds" );
		//}
		//else if( myInferenceEngine != null ) myInferenceEngine.setCPT( var, null );

		myLastCalculatedPrE = null;

		if( myInferenceEngine != null )
		{
			myInferenceEngine.setCPT( var );
			killDynamatorsState( myInferenceEngine.getDynamator() );
		}
		clearRCSettings( false, true );

		fireCPTChanged( new CPTChangeEvent( var ) );

		if( ui.getActiveHuginNetInternalFrame() == this ) ui.newPrE( this );
	}

	/**
		@author Keith Cascio
		@since 062802
	*/
	public void recompile()
	{
		//System.out.println( "NetworkInternalFrame.recompile()" );

		//BeliefNetworks.compile( myBeliefNetwork, this );

		StatusBar bar = ui.getStatusBar();
		if( bar != null ) bar.pushText( (myPushedStatusMessage = STR_MSG_COMPILE), StatusBar.WEST );
		ui.setCursor( UI.CURSOR_WAIT_COMPILE );
		ui.getDynamator().compile( myBeliefNetwork, this );
	}

	public static final String STR_MSG_COMPILE = "compiling...";

	/** interface DynaListener
		@author keith cascio
		@since  20020701 */
	public void handleInferenceEngine( InferenceEngine IE )
	{
		if( mySynchronization == null ){//disposed
			if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "NetworkInternalFrame.handleInferenceEngine(), frame already disposed" );
			return;
		}

		synchronized( mySynchronization )
		{

		//System.out.println( "NetworkInternalFrame.handleInferenceEngine("+IE+")" );

		killInferenceEngine();
		myInferenceEngine = IE;

		myInferenceEngine.printInfoCompilation( console );

		EvidenceController EC = myBeliefNetwork.getEvidenceController();
		EC.removeEvidenceChangeListener( this );
		EC.addEvidenceChangeListener( this );

		Set evidence = EC.evidence().keySet();
		if( !evidence.isEmpty() )
		{
			IE.evidenceChanged( new EvidenceChangeEvent( evidence ) );
			myInferenceEngine.printInfoPropagation( console );
		}

		//fireCPTChanged( new CPTChangeEvent( null ) );
		fireNetworkRecompiled();

		ui.setDefaultCursor();

		StatusBar bar = ui.getStatusBar();
		if( bar != null ) bar.popText( STR_MSG_COMPILE, StatusBar.WEST );
		myPushedStatusMessage = null;

		SamiamUserMode mode = getSamiamUserMode();
		mode.setModeEnabled( SamiamUserMode.NEEDSCOMPILE, false );
		mode.setModeEnabled( SamiamUserMode.COMPILING, false );
		setSamiamUserMode( mode );

		}
	}

	/** interface DynaListener
		@author keith cascio
		@since  20030117 */
	public void handleError( final String msg ){
		handleErrorImpl( msg, false );
	}

	/** @since 20091225 */
	public NetworkInternalFrame handleError( Throwable caught ){
		String errmsg = caught instanceof OutOfMemoryError ? Dynamator.STR_OOME : caught.getMessage();
		return handleErrorImpl( errmsg == null ? caught.toString() : errmsg, true );
	}

	/** @since 20091225 */
	private NetworkInternalFrame handleErrorImpl( final String msg, boolean panic ){
		if( mySynchronization == null ){//disposed
			if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "NetworkInternalFrame.handleError( "+msg+" ), frame already disposed" );
			return this;
		}

		ui.setDefaultCursor();

		JOptionPane.showMessageDialog(	getTopLevelAncestor(),
					processErrorMessage( msg ),
					"Query Mode Error",
					JOptionPane.ERROR_MESSAGE );

		if( panic ){ killInferenceEngine(); }

		System.gc();
		new edu.ucla.util.SystemGCThread().start();

		SamiamUserMode mode = getSamiamUserMode();
		mode.setModeEnabled( SamiamUserMode.EDIT,      true  );
		mode.setModeEnabled( SamiamUserMode.QUERY,     false );
		mode.setModeEnabled( SamiamUserMode.COMPILING, false );
		if( panic ){ mode.setModeEnabled( SamiamUserMode.NEEDSCOMPILE, true ); }
		setSamiamUserMode( mode );

		return this;
	}

	/** interface DynaListener
		@author keith cascio
		@since  20030117 */
	public ThreadGroup getThreadGroup(){
		ThreadGroup parent = null;
		if( NetworkInternalFrame.this.ui != null ) parent = ui.getThreadGroup();
		if( parent                       == null ) parent = Thread.currentThread().getThreadGroup();
		if( myThreadGroup == null ) myThreadGroup = new ThreadGroup( parent, getFileNameSansPath() + " threads" );
		return myThreadGroup;
	}

	/** @since 120803 */
	private String processErrorMessage( final String msg )
	{
		if( msg == Dynamator.STR_OOME ){
			String invoker = (ui == null) ? (String)null : ui.getInvokerName();
			return msg + "\n" + Util.makeOutOfMemoryMessage( invoker );
		}
		return msg;
	}

	public void netStructureChanged( NetStructureEvent event )
	{
		myNetStructureHasChanged = true;

		killInferenceEngine();
		killDynamatorsState();
		clearRCSettings( true, true );
		if( myAnimator != null ) myAnimator.killState();

		SamiamUserMode mode = getSamiamUserMode();
		mode.setModeEnabled( SamiamUserMode.NEEDSCOMPILE, true );
		setSamiamUserMode( mode );

		//PreferenceGroup globalPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.PkgDspNme );

		if( (event == null) || (event.eventType == NetStructureEvent.GENERIC) )
		{
			/*	This code now handled in fireNodePropertyChangeEvent()	*/
			/*
			DisplayableFiniteVariable dVar = null;
			for( Iterator it = event.finiteVars.iterator(); it.hasNext(); )
			{
				dVar = (DisplayableFiniteVariable) it.next();

				VariableInstance[] instances = new VariableInstance[dVar.size()];
				for( int j = 0; j < dVar.size(); j++ ){
					instances[j] = new VariableInstance( dVar, dVar.instance(j) );
				}

				myMapDvarsToVariableInstanceArrays.put(dVar, instances);
			}
			*/
		}
		else if(event.eventType == NetStructureEvent.NODES_ADDED)
		{
			DisplayableFiniteVariable dVar = null;
			for( Iterator it = event.finiteVars.iterator(); it.hasNext(); )
			{
				dVar = (DisplayableFiniteVariable) it.next();
				//System.out.println( "Java new DisplayableFiniteVariable in NetworkInternalFrame.netStructureChanged()" );//debug
				//DisplayableFiniteVariable dVar = new DisplayableFiniteVariable( dVar, this, opt.pkgOpt);
				//DisplayableFiniteVariable dVar = new DisplayableFiniteVariableImpl( dVar, this );

				VariableInstance[] instances = new VariableInstance[dVar.size()];
				for (int j = 0; j < dVar.size(); j++)
				{
					instances[j] = new VariableInstance(dVar, dVar.instance(j));
				}
				myMapDvarsToVariableInstanceArrays.put(dVar, instances);
			}

			refreshVariableComboBoxes();
		}
		else if(event.eventType == NetStructureEvent.NODES_REMOVED)
		{
			DisplayableFiniteVariable dVar = null;
			for( Iterator it = event.finiteVars.iterator(); it.hasNext(); )
			{
				dVar = (DisplayableFiniteVariable) it.next();
				myMapDvarsToVariableInstanceArrays.remove(dVar);
			}

			refreshVariableComboBoxes();
		}

		//myInferenceEngine.setObservations(evidence);
		if( event != null ){ fireNetStructureChanged( event ); }

		//recompile();
	}

	/*/// @since 20020425 //
	public void setDefaultEvidence(){
		DisplayableFiniteVariable dVar = null;
		Integer tempIndex = null;

		try{
			for( DFVIterator it = myBeliefNetwork.dfvIterator(); it.hasNext(); ){
				dVar = it.nextDFV();
				tempIndex = dVar.getDefaultStateIndex();
				if( tempIndex != null ){
					//unobserve( fVar );
					observe( dVar, dVar.instance( tempIndex.intValue() ) );
				}
			}
		}catch( StateNotFoundException e ){
			System.err.println( "NetworkInternalFrame.setDefaultEvidence() caught " + e );
			if( FLAG_DEBUG_VERBOSE ){
				System.err.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
		}
	}
	public boolean resetEvidence(){
		myBeliefNetwork.getEvidenceController().resetEvidence();
		return true;
	}
	public boolean setObservations(Map evidence) throws StateNotFoundException{
		myBeliefNetwork.getEvidenceController().setObservations( evidence );
		return true;
	}
	public boolean observe(FiniteVariable var, Object value) throws StateNotFoundException{
		myBeliefNetwork.getEvidenceController().observe( var, value );
		return true;
	}
	public boolean observe(Map evidence) throws StateNotFoundException{
		myBeliefNetwork.getEvidenceController().observe( evidence );
		return true;
	}
	public boolean unobserve(FiniteVariable var){
		myBeliefNetwork.getEvidenceController().unobserve( var );
		return true;
	}*/

	/** @since 20051006 */
	public boolean setDefaultEvidence( Component parentComponent ){
		boolean flagUnobserve = false, flagSuccess = false;
		Throwable caught = null;
		EvidenceController ec = null;
		try{
			ec = myBeliefNetwork.getEvidenceController();
			StandardNodeImpl.setDefaultEvidence( ec );
			flagSuccess = true;
		}catch( Exception exception ){
			caught = exception;
			if( Util.DEBUG_VERBOSE ){
				System.err.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				exception.printStackTrace();
			}
		}finally{
			if( ! flagSuccess ){ showEvidenceWarning( caught, /*flagUnobserve*/ false, null, null, parentComponent == null ? evidenceTreeScrollPane : parentComponent ); }
		}
		return flagSuccess;
	}

	/** @since 20051006 */
	public boolean resetEvidence( Component parentComponent ){
		boolean flagUnobserve = false, flagSuccess = false;
		Throwable caught = null;
		EvidenceController ec = null;
		try{
			ec = myBeliefNetwork.getEvidenceController();
			ec.resetEvidence();
			flagSuccess = true;
		}catch( Exception exception ){
			caught = exception;
			if( Util.DEBUG_VERBOSE ){
				System.err.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				exception.printStackTrace();
			}
		}finally{
			if( ! flagSuccess ){ showEvidenceWarning( caught, /*flagUnobserve*/ false, null, null, parentComponent == null ? evidenceTreeScrollPane : parentComponent ); }
		}
		return flagSuccess;
	}

	/** @since 20051006 */
	public boolean evidenceRequest( FiniteVariable var, Object value, Component parentComponent ){
		boolean flagUnobserve = false;
		boolean flagSuccess = false;
		Throwable caught = null;
		Object valueOld = null;
		EvidenceController ec = null;
		try{
			ec = myBeliefNetwork.getEvidenceController();
			valueOld = ec.getValue( var );
			flagUnobserve = (valueOld != null) && (valueOld == value);//(var.index(getEvidence(var)) == var.index(instance))
			if( flagUnobserve ) ec.unobserve( var );
			else ec.observe( var, value );
			flagSuccess = true;
		}catch( Exception exception ){//StateNotFoundException e ){
			caught = exception;
			//System.err.println( "NetworkInternalFrame.handleEvidenceRequest() caught " + exception );
			if( Util.DEBUG_VERBOSE ){
				System.err.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				exception.printStackTrace();
			}
		}finally{
			if( !flagSuccess ) showEvidenceWarning( caught, flagUnobserve, var, value, parentComponent );
		}
		return flagSuccess;
	}

	/** @since 20051006 */
	public static void showEvidenceWarning( Throwable caught, boolean flagUnobserve, FiniteVariable var, Object instance, Component parentComponent ){
		String verb = flagUnobserve ? STR_VERB_UNOBSERVE : STR_VERB_OBSERVE;
		String strVarValue = ((var == null) || (instance == null)) ? " values" : (" <b>" + var.toString() + "</b> == <b>" + instance.toString() + "</b>");
		String description = "<html><nobr>Failed to " + verb + strVarValue;
		String reason = STR_MSG_WARNING_GENERIC_FAILED_OBSERVATION;
		if( caught != null ){
			String reasonCaught = caught.getMessage();
			if( reasonCaught == null ) reasonCaught = caught.toString();
			reason = reasonCaught;
		}
		String message = description + "<br><font color=\"#990000\">" + reason;
		String title = STR_TITLE_WARNING_FAILED_OBSERVATION;//description;
		int type = JOptionPane.WARNING_MESSAGE;
		JOptionPane.showMessageDialog( parentComponent, message, title, type );
	}

	public static final String STR_VERB_OBSERVE   = "assert";
	public static final String STR_VERB_UNOBSERVE = "retract";
	public static final String STR_MSG_WARNING_GENERIC_FAILED_OBSERVATION = "unknown system error";
	public static final String STR_TITLE_WARNING_FAILED_OBSERVATION       = "Warning: Evidence";

	/** interface InternalFrameListener
		@since 112202 */
	public void internalFrameActivated( InternalFrameEvent event )
	{
		ui.toFront( this );
		MainToolBar MTB = ui.getMainToolBar();
		if( MTB != null ) MTB.zoomed( networkDisplay.getZoomFactor() );
		menuItem.setSelected( true );
	}

	public void internalFrameClosed(InternalFrameEvent event){
		//recCondInternalFrame.doDefaultCloseAction();
	}

	public void internalFrameClosing(InternalFrameEvent event)
	{
		//System.out.println( "NetworkInternalFrame.internalFrameClosing()" );
		//ui.toFront(this);//bad bad bad bad bad idea
		Object ret = ui.closeFilePromptPlusCleanup( this );

		//System.out.println( "    ret == " + ret.toString() );
		if( !(ret == UI.STR_CANCEL || ret == null) ) setVisible( false );
		else if( !this.isIcon() ){
			if( !ui.containsInternalFrame( this ) ) ui.addInternalFrame( this );
			setVisible( true );
		}
	}

	public void internalFrameDeactivated(InternalFrameEvent event){
		menuItem.setSelected( false );
	}

	public void internalFrameDeiconified(InternalFrameEvent event) {
		ui.toFront(this);
	}

	public void internalFrameIconified(InternalFrameEvent event) {
		//ui.toBack(this);//bad bad bad bad bad idea
	}

	public void internalFrameOpened(InternalFrameEvent event) { }
}
