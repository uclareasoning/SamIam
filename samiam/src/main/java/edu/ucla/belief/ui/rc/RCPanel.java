package edu.ucla.belief.ui.rc;

import edu.ucla.structure.*;
import edu.ucla.belief.*;
import edu.ucla.belief.dtree.*;
//import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.ui.recursiveconditioning.CachePreviewDialog;
import edu.ucla.belief.ui.recursiveconditioning.DtreeSettingsPanel;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.io.hugin.HuginLogReader;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.io.InflibFileFilter;
import edu.ucla.util.*;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.internalframes.MPEPanel;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.statusbar.StatusBar;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;
import java.text.*;
import javax.swing.filechooser.FileFilter;

/** @author keith cascio
	@since  20031110 */
public class RCPanel extends JPanel implements
  EvidenceChangeListener,
  javax.swing.event.ChangeListener,
//RC.RecCondThreadListener,
//RC.ProgressEventListener,
//CachingScheme.RCCreateListener,
  CPTChangeListener,
  NetStructureChangeListener,
  NodePropertyChangeListener
{
	public static boolean
	  FLAG_DEBUG_BORDERS = false,
	  FLAG_DEBUG_VERBOSE = Util.DEBUG_VERBOSE,
	  FLAG_DEBUG         = Util.DEBUG;

	protected BeliefNetwork        myBeliefNetwork;
	protected RCEngineGenerator    myRCEngineGenerator;
	protected RCSettings           mySettings;
	protected ActionListener       myActionListener;
	protected NetworkInternalFrame myNetworkInternalFrame;
	protected Object               mySynchronization = new Object();

	public RCPanel( NetworkInternalFrame hnInternalFrame, Container parent )
	{
		mySettings = new RCSettings();
		mySettings.setPrEOnly( true );
	  //mySettings.setOutStream( hnInternalFrame.console );
		myActionListener = new ToolActionListener();

		myNetworkInternalFrame = hnInternalFrame;
		if( hnInternalFrame != null ) myBeliefNetwork = hnInternalFrame.getBeliefNetwork();

		setListening( hnInternalFrame, myBeliefNetwork, true );

		init( hnInternalFrame, parent, true, true );
	}

	public void setListening( NetworkInternalFrame nif, BeliefNetwork bn, boolean listen )
	{
		if( nif != null )
		{
			if( listen )
			{
				nif.addCPTChangeListener(this);
				nif.addNetStructureChangeListener(this);
				nif.addNodePropertyChangeListener(this);
			}
			else
			{
				nif.removeCPTChangeListener(this);
				nif.removeNetStructureChangeListener(this);
				nif.removeNodePropertyChangeListener(this);
			}
		}

		if( bn != null )
		{
			if( listen ) bn.getEvidenceController().addEvidenceChangeListener( this );
			else bn.getEvidenceController().removeEvidenceChangeListener( this );
		}
	}

	public static void main( String[] args )
	{
		if( args.length > 0 )
		{
			if( args[0].equals( "debug" ) ) FLAG_DEBUG = true;
		}

		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		final JFrame frame = new JFrame( "Test/Debug Frame" );
		frame.setBounds( 0,0,700,600 );

		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		JDesktopPane desktopPane = new JDesktopPane();

		//final RCPanel RCIF = new RCPanel( null );
		final JInternalFrame RCIF = new JInternalFrame( STR_TITLE_GENERATOR, true, true, true, true );
		//RCIF.getContentPane().add( new RCPanel( new RCEngineGenerator(), RCIF ) );
		RCIF.getContentPane().add( new RCPanel( (NetworkInternalFrame)null, RCIF ) );

		RCIF.setBounds( 20, 40, 600, 375 );
		RCIF.pack();
		RCIF.setDefaultCloseOperation( JInternalFrame.HIDE_ON_CLOSE );
		desktopPane.add( RCIF );

		frame.getContentPane().add( desktopPane );
		frame.addWindowListener( new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				Util.STREAM_TEST.println( RCIF.getSize() );//debug
				//System.exit(0);
			}
		});

		Util.centerWindow( frame );
		RCIF.setVisible( true );
		frame.setVisible( true );
	}

	protected JTabbedPane myTabbedPane = null;
	protected JComponent myAllocationPanel;
	//protected NumberFormat theProbabilityFormat = NumberFormat.getInstance();
	protected static NumberFormat theProbabilityFormat = new DecimalFormat( "0.0###############################################################################" );
	protected JComponent myResizePanel = null;
	protected ComponentListener myResizeAdapter = null;
	protected ContainerAbstraction myContainerAbstraction;
	private JComponent myPanelDgraphSettings;

	public static final String STR_TAB_CALCULATION = "Calculation";
	public static final String STR_TAB_DGRAPH_SETTINGS = "Dgraph Settings";

	/** @since 20050215 */
	public boolean setTabSelectedForTitle( String title ){
		if( myTabbedPane == null ) return false;
		int index = myTabbedPane.indexOfTab( title );
		if( index >= (int)0 ){
			myTabbedPane.setSelectedIndex( index );
			return true;
		}
		return false;
	}

	/** @since 20050215 */
	public JComponent getPanelDgraphSettings(){
		return myPanelDgraphSettings;
	}

	protected void init( NetworkInternalFrame hnInternalFrame, Container parent, boolean showCalculationPanel, boolean showRCDtreeFilePanel )
	{
		//mySettingsMightHaveChanged = true;

		if( hnInternalFrame != null ) setBaseFileName( hnInternalFrame.getFileName() );

		myTabbedPane = new JTabbedPane();
		myTabbedPane.addChangeListener( this );

		myAllocationPanel = makeAllocationPanel( showCalculationPanel, showRCDtreeFilePanel );
		myPanelDgraphSettings = makeSettingsPanel();

		myTabbedPane.add( myAllocationPanel, STR_TAB_CALCULATION );
		myTabbedPane.add( myPanelDgraphSettings, STR_TAB_DGRAPH_SETTINGS );

		myResizePanel = this;
		myResizePanel.add( myTabbedPane );
		myResizeAdapter = new ResizeAdapter( myTabbedPane );
		myResizePanel.addComponentListener( myResizeAdapter );
		Dimension minSize = myTabbedPane.getPreferredSize();
		minSize.width += 15;
		minSize.height += 20;
		setMinimumSize( minSize );
		setPreferredSize( minSize );

		if( hnInternalFrame != null )
		{
			RCPanel.this.setParent( hnInternalFrame.getThreadGroup() );

			SamiamPreferences pref = hnInternalFrame.getPackageOptions();
			PreferenceGroup netPrefs = pref.getPreferenceGroup( SamiamPreferences.NetDspNme );

			Preference netBkgdClrNeedsCompile = pref.getMappedPreference( SamiamPreferences.netBkgdClrNeedsCompile );
			myColorBackgroundPausedThread = (Color) netBkgdClrNeedsCompile.getValue();

			java.io.File defaultDirectory = pref.defaultDirectory;
			setDefaultDirectory( defaultDirectory );
		}

		myContainerAbstraction = new ContainerAbstraction( parent );

		//setVisible( false );
	}

	public void setContainer( Container parent )
	{
		myContainerAbstraction.setContainer( parent );
	}

	public class ContainerAbstraction extends WindowAdapter implements WindowListener, InternalFrameListener, ComponentListener
	{
		protected JInternalFrame myJInternalFrame;
		protected Dialog myDialog;
		protected Frame myFrame;
		protected Container myContainer;

		public ContainerAbstraction( Container parent )
		{
			setContainer( parent );
		}

		public void setContainer( Container parent )
		{
			if( myContainer != null ) myContainer.removeComponentListener( this );

			if( myJInternalFrame != null ) myJInternalFrame.removeInternalFrameListener( this );

			if( myDialog != null ) myDialog.removeWindowListener( this );

			if( myFrame != null ) myFrame.removeWindowListener( this );

			myContainer = parent;
			//myContainer.addComponentListener( this );

			if( parent instanceof JInternalFrame )
			{
				myJInternalFrame = (JInternalFrame)parent;
				myJInternalFrame.addInternalFrameListener( this );
			}
			else if( parent instanceof Dialog )
			{
				myDialog = (Dialog)parent;
				myDialog.addWindowListener( this );
			}
			else if( parent instanceof Frame )
			{
				myFrame = (Frame)parent;
				myFrame.addWindowListener( this );
			}
		}

		public boolean isVisible()
		{
			if( myContainer != null ) return myContainer.isVisible();
			else return false;
		}

		public void setVisible( boolean flag )
		{
			if( myContainer != null ) myContainer.setVisible( flag );
		}

		public void setTitle( String title )
		{
			if( myJInternalFrame != null ) myJInternalFrame.setTitle( title );
			else if( myDialog != null ) myDialog.setTitle( title );
			else if( myDialog != null ) myFrame.setTitle( title );
		}

		public String getTitle()
		{
			if( myJInternalFrame != null ) return myJInternalFrame.getTitle();
			else if( myDialog != null ) return myDialog.getTitle();
			else if( myDialog != null ) return myFrame.getTitle();
			else return null;
		}

		public void internalFrameClosing( InternalFrameEvent e )
		{
			RCPanel.this.doParentClosing();
		}
		public void internalFrameOpened(InternalFrameEvent e) {}
		public void internalFrameClosed(InternalFrameEvent e) {}
		public void internalFrameIconified(InternalFrameEvent e) {}
		public void internalFrameDeiconified(InternalFrameEvent e) {}
		public void internalFrameActivated(InternalFrameEvent e) {}
		public void internalFrameDeactivated(InternalFrameEvent e) {}

		public void windowClosing( WindowEvent e )
		{
			RCPanel.this.doParentClosing();
		}

		public void componentResized( ComponentEvent e ){}
		public void componentMoved(ComponentEvent e){}

		public void componentShown( ComponentEvent e )
		{
			RCPanel.this.setVisible( true );
		}

		public void componentHidden( ComponentEvent e )
		{
			RCPanel.this.setVisible( false );
		}
	}

	protected void doParentClosing()
	{
	}

	protected String myBaseFileName;

	public void setBaseFileName( String str )
	{
		myBaseFileName = str;
	}

	private JLabel myLabelOptimalMemory = null;
	private JLabel myLabelUserMemory = null;
	private JSlider mySliderUserMemory = null;
	//private JCheckBox myCBuseKB;
	private JLabel myLabelEstimatedTime = null;
	private JLabel myLabelPercentComplete = null;
	private JLabel myLabelProbability = null;
	private JButton myButtonRun;
	private JButton myButtonCancel;
	private JButton myButtonPause;
	private JButton myButtonResume;

	public static final int INT_MIN_SLIDER = 0;
	public static final int INT_MAX_SLIDER = 100;

	/** @since 20030124 */
	public void resetSlider()
	{
		//System.out.println( "RCPanel.resetSlider()" );
		myFlagListenEvents = false;
		mySliderUserMemory.setValue( (int)((double)INT_MAX_SLIDER * mySettings.getUserMemoryProportion()) );
		myFlagListenEvents = true;
	}

	/** @since 20050215 */
	public RCSettings getSettings(){
		return mySettings;
	}

	protected boolean myFlagListenEvents = true;

	protected boolean mySettingsMightHaveChanged = false;
	protected Color myColorBackgroundNormal = null;
	protected Color myColorBackgroundPausedThread = null;

	protected ActionListener myGeneralActionListener = new GeneralActionListener();

	protected class GeneralActionListener implements ActionListener
	{
		public void actionPerformed( ActionEvent event )
		{
			Object src = event.getSource();

			boolean changed = false;

			if( src == myComboBoxMethod )
			{
				changed = true;
				toggleMethod();
			}
			else if( src == myButtonGenerateDtree ) doGenerateInfo();
			else if( myFlagListenEvents ){
				if( src == myComboBoxElimAlgo ){ changed = true; }
			}

			if( changed ) mySettingsMightHaveChanged = true;
		}
	}

	/** @since 20050215 */
	public void allocateMemory(){
		if( !myButtonAllocateMemory.isEnabled() ) throw new IllegalStateException( "illegal to allocate memory now" );

		myButtonAllocateMemory.setEnabled( false );

		//myRunningCacheAllocationThread = mySettings.allocRCDtreeInThread( myBeliefNetwork );
		edu.ucla.belief.ui.util.Util.pushStatusWest( myNetworkInternalFrame, CacheSettingsPanel.STR_MSG_ALLOCATE );
		setCursor( UI.CURSOR_WAIT_RC );
		setAllocationInterferingWidgetsEnabled();
		doAllocate();
		setDefaultCursor();
		edu.ucla.belief.ui.util.Util.popStatusWest( myNetworkInternalFrame, CacheSettingsPanel.STR_MSG_ALLOCATE );

		myButtonAllocateMemory.setEnabled( true );
	}

	/** @since 20030124 */
	protected class ToolActionListener implements ActionListener
	{
		public void actionPerformed( ActionEvent event )
		{
			Object src = event.getSource();

			if( src == myButtonAllocateMemory ) allocateMemory();
			else if( src == myRadioPRE )
			{
				setResultProbabilityEnabled( false );
				lblProbabilityCaption.setText( STR_PRE_CAPTION );
				myPanelShowMPE.removeAll();
				myPanelResults.revalidate();
				myPanelResults.repaint();
			}
			else if( src == myRadioMPE )
			{
				setResultProbabilityEnabled( false );
				lblProbabilityCaption.setText( STR_MPE_CAPTION );
				myPanelShowMPE.add( myButtonShowMPE );
				myPanelResults.revalidate();
				myPanelResults.repaint();
			}
			else if( src == myButtonRun ) safeRunRecursiveConditioning();
			else if( src == myButtonCancel ) confirmCancelRecursiveConditioning();
			else myGeneralActionListener.actionPerformed( event );
		}
	}

	private void doGenerateInfo()
	{
		if( Util.DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\n****************************\nRCPanel.doGenerateInfo()" );

		if( mySettings != null && myBeliefNetwork != null )
		{
			EliminationHeuristic current = mySettings.getEliminationHeuristic();
			EliminationHeuristic selected = (EliminationHeuristic) myComboBoxElimAlgo.getSelectedItem();

			//if( current == selected ) return;

			RCInfo oldRCInfo = mySettings.getInfo();
			boolean oldFlagStale = mySettings.isStale();

			boolean success = false;
			String msgThrowable = "";
			RCInfo temp = null;
			try{
				mySettings.setEliminationHeuristic( selected );
				temp = mySettings.generateInfoOrDie( myBeliefNetwork );
				success = true;
			}catch( Throwable throwable ){
				msgThrowable = throwable.getMessage();
				if( msgThrowable == null ) msgThrowable = throwable.toString();
				success = false;
			}

			if( !success ){
				temp = null;
				try{
					mySettings.setEliminationHeuristic( current );
					mySettings.setInfo( oldRCInfo );
				}catch( Throwable throwable ){
					System.err.println( throwable );
				}
			}

			if( Util.DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "existing  RCInfo object: " + oldRCInfo + "\ngenerated RCInfo object: " + temp );

			RCInfo newRCInfo;
			if( temp == null ){
				showErrorMessage( "Failed to generate Dgraph\n" + msgThrowable, "Dgraph Generation Error" );
			}
			else
			{
				if( myCheckBoxKeepBest.isSelected() ) newRCInfo = mySettings.selectBetterRCInfo( temp, oldRCInfo );
				else newRCInfo = temp;

				if( newRCInfo != mySettings.getInfo() )
				{
					mySettings.setInfo( newRCInfo );
					double newMemoryProportion = (double)mySliderUserMemory.getValue()/(double)INT_MAX_SLIDER;
					mySettings.setUserMemoryProportion( Table.ONE );
					mySettings.setUserMemoryProportion( Table.ZERO );
					mySettings.setUserMemoryProportion( newMemoryProportion );
				}

				if( newRCInfo != oldRCInfo && newRCInfo != null )
				{
					updateDgraphStatsDisplay();
					updateOptimalMemoryNumber();
					updateEstimatedTime();
					resetSlider();
				}
			}

			mySettingsMightHaveChanged = false;
		}

		if( Util.DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "****************************\n" );
	}

	private boolean doAllocate()
	{
		//System.out.println( "RCPanel.doAllocate()" );

		boolean flagPerformedSearch = false, flagAllocationValid = false, flagSearchNeeded = false;
		String msgThrowable = null;
		if( mySettings != null && myBeliefNetwork != null )
		{
			flagSearchNeeded = (mySettings.getInfo() == null) || mySettings.isStale();

			try{
				flagAllocationValid = mySettings.validateAllocation( myBeliefNetwork );
				updateEstimatedTime();
			}catch( Throwable throwable ){
				flagAllocationValid = false;
				msgThrowable = throwable.getMessage();
				if( msgThrowable == null ) msgThrowable = throwable.toString();
			}

			flagPerformedSearch = flagSearchNeeded && flagAllocationValid;
		}

		if( Thread.currentThread().isInterrupted() ) return false;

		if( flagPerformedSearch )
		{
			double proportion = mySettings.synchronizeMemoryProportion();
			updateUserMemoryDisplay();
			resetSlider();
		}

		if( !flagAllocationValid ){
			String title = "Failed to allocate caches.";
			String message = title;
			if( msgThrowable != null ) message += "\n" + msgThrowable;
			showErrorMessage( message, title );
		}

		return flagAllocationValid;
	}

	private void updateEstimatedTime()
	{
		//System.out.println( "RCPanel.updateEstimatedTime() stale? " + mySettings.isStale() );

		if( mySettings.isStale() )
		{
			if( mySliderUserMemory.getValue() == INT_MAX_SLIDER ) initEstimatedTimeDisplay( mySettings );
			else myLabelEstimatedTime.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		}
		else updateEstimatedMinutesDisplay();
	}

	/** @since 20050215 */
	public void safeRunRecursiveConditioning()
	{
		synchronized( mySynchronization ){
			myButtonRun.setEnabled( false );
			//if( myNumRunning < 1 )
			startRecursiveConditioning();// ! myRadioPRE.isSelected() );
		}
	}

	protected JComboBox myComboBoxCacheSchemes = null;
	private volatile int myNumRunning = 0;

	public static final String
	  STR_TITLE_NORMAL    = "Recursive Conditioning",
	  STR_TITLE_PAUSED    = "Recursive Conditioning - PAUSED",
	  STR_TITLE_GENERATOR = "Compile RCSettings - Recursive Conditioning";

	protected void confirmCancelRecursiveConditioning()
	{
		int result = JOptionPane.showConfirmDialog( this, "Are you sure you want to\ncancel the current recursive conditioning\ncalculation?", "Confirm Cancel", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
		if( result == JOptionPane.OK_OPTION ) cancelRecursiveConditioning();
	}

	protected void cancelRecursiveConditioning()
	{
		synchronized( mySynchronization ){
			//if( myRCDtree == null )  System.err.println( "RCPanel.cancelRecursiveConditioning() called with null RCDtree" );

			//alertPause( false );
			myButtonRun.setEnabled( true );
			myButtonPause.setEnabled( false );
			myButtonResume.setEnabled( false );
			myButtonCancel.setEnabled( false );

			//resumeRecursiveConditioning();
			try{
				stopRuns();
			}catch( InterruptedException interruptedexception ){
				System.err.println( "Warning: " + interruptedexception );
			}

			//JOptionPane.showMessageDialog( this, "Recursive conditioning computation cancelled.", "Computation cancelled.", JOptionPane.WARNING_MESSAGE );
		}
	}

	/** @since 20050516 */
	private void doRecursiveConditioning( boolean flagMPE )
	{
		boolean success = false;
		boolean clean_up = false;
		double result = (double)-1;
		long answerMillis = (long)-1;

		synchronized( mySynchComputation ){
		try{
			try{
				Thread.sleep( 4 );

				if( !doAllocate() ){
					showErrorMessage( "Failed to run recursive conditioning: failed to allocate DGraph.", "Error" );
					myButtonRun.setEnabled( true );
					return;
				}

				RCInfo info = mySettings.getInfo();
				Map evid = myBeliefNetwork.getEvidenceController().evidence();
				RCEngine engine = new RCEngine( mySettings, null );
				engine.evidenceChanged( new EvidenceChangeEvent( myBeliefNetwork ) );
				clean_up = true;

				//alertPause( false );
				myProgressCount = 0;
				//myRCDtree.addProgressEventListener( RCPanel.this );

				setCalculationInterferingWidgetsEnabled();
				setResultProbabilityEnabled( false );
				myButtonPause.setEnabled( false );//true );
				myButtonResume.setEnabled( false );
				myButtonCancel.setEnabled( true );//false );

				String msg = "recursive conditioning tool ";
				msg += flagMPE ? "calculating mpe..." : UI.STR_MSG_PRE;

				edu.ucla.belief.ui.util.Util.pushStatusWest( myNetworkInternalFrame, msg );

				Thread.sleep( 4 );

				result = (double)-1;
				long start_ms = System.currentTimeMillis();
				long start_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();

				if( flagMPE )
				{
					throw new IllegalArgumentException( "RCPanel.runRecursiveConditioning() does not yet support flagMPE." );
					//clobberMPETab();
					//myRCDtree.recCond_MPEAsThread( RCPanel.this );
				}
				else result = engine.probability();

				long end_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
				long end_ms = System.currentTimeMillis();

				Thread.sleep( 4 );

				long threadCPUMS = end_cpu_ms - start_cpu_ms;
				long elapsedMillis = end_ms - start_ms;

				edu.ucla.belief.ui.util.Util.popStatusWest( myNetworkInternalFrame, msg );

				answerMillis = JVMProfiler.profilerRunning() ? threadCPUMS : elapsedMillis;

				myProgressCount = info.recursiveCalls();
				engine.setValid( false );
				mySettings.removeChangeListener( engine );
			}finally{
				--RCPanel.this.myNumRunning;
			}

			rcFinished( result, answerMillis );
			success = true;

			if( clean_up ){
				ThreadGroup gCleanup = getCleanupThreadGroup();
				gCleanup.interrupt();
				Runnable runnable = new edu.ucla.util.SystemGCThread();
				String name = "rc System.gc() cleanup #" + Integer.toString( INT_COUNTER_NAMES_CLEANUP++ );
				new Thread( gCleanup, runnable, name ).start();
			}
		}catch( InterruptedException interruptedexception ){
			success = true;
			++myNumInterrupted;
			//System.out.println( Thread.currentThread().getName() + " returning from interrupt" );
			RCPanel.this.rcComputationStop( "Computation interrupted.", "Computation Cancelled" );
			Thread.currentThread().interrupt();
			return;
		}catch( Exception exception ){
			String msg = exception.getMessage();
			if( msg == null ) msg = exception.toString();
			RCPanel.this.rcComputationError( msg );
			return;
		}catch( ThreadDeath death ){
			success = true;
			++myNumInterrupted;
			//System.out.println( Thread.currentThread().getName() + " returning from death" );
			RCPanel.this.rcComputationStop( "Computation killed.", "Computation Cancelled" );
			Thread.currentThread().interrupt();
			throw death;
		}finally{
			if( !success ) RCPanel.this.rcComputationError( "unknown error" );
			mySynchComputation.notifyAll();
			//return;//causes javac 1.4.2 warning
		}
		}
	}

	public static final int INT_PRIORITY_COMPUTATION = ((Thread.NORM_PRIORITY - Thread.MIN_PRIORITY)/(int)2) + Thread.MIN_PRIORITY;
	private static int INT_COUNTER_NAMES_COMPUTATION = (int)0;
	private static int INT_COUNTER_NAMES_CLEANUP = (int)0;

	/** @since 20050516 */
	protected Thread startRecursiveConditioning()// final boolean flagMPE )
	{
		synchronized( mySynchronization ){
			try{
				stopRuns();
			}catch( InterruptedException interruptedexception ){
				System.err.println( "Warning: " + interruptedexception );
				return (Thread)null;
			}

			forceCompletePercentComplete( STR_ZERO_PERCENT );

			ThreadGroup group = getComputationThreadGroup();
			Runnable runnable = getComputationRunnable();
			String name = "rc pr(e) only thread #" + Integer.toString( INT_COUNTER_NAMES_COMPUTATION++ );
			Thread ret =
				new Thread(	group, runnable, name );
			ret.setPriority( INT_PRIORITY_COMPUTATION );
			ret.start();

			++RCPanel.this.myNumRunning;
			return ret;
		}
	}

	/** @since 20050516 */
	private Runnable getComputationRunnable(){
		synchronized( mySynchronization ){
			if( myComputationRunnable == null ){
				myComputationRunnable = new Runnable(){
					public void run(){
						RCPanel.this.doRecursiveConditioning( /*flagMPE*/false );
					}
				};
			}
		}
		return myComputationRunnable;
	}
	private Runnable myComputationRunnable;

	/** @since 20050514 */
	private ThreadGroup getCleanupThreadGroup(){
		if( myCleanupThreadGroup == null ){
			ThreadGroup gp = null;
			try{
				gp = getCurrentParent().getParent();
				if( gp == null ){ gp = getCurrentParent(); }
			}catch( Throwable thrown ){
				System.err.println( "warning: RCPanel.getCleanupThreadGroup() caught " + thrown );
			}
			myCleanupThreadGroup = new ThreadGroup( gp, "rc pr(e) cleanup threads" );
		}
		return myCleanupThreadGroup;
	}

	/** @since 20050514 */
	private ThreadGroup getComputationThreadGroup(){
		synchronized( mySynchronization ){
			if( myComputationThreadGroup == null ) myComputationThreadGroup = new ThreadGroup( getCurrentParent(), "rc pr(e) threads" );
			return myComputationThreadGroup;
		}
	}

	/** @since 20060719 */
	public ThreadGroup getCurrentParent(){
		if( myParent == null ) return Thread.currentThread().getThreadGroup();
		else return myParent;
	}

	/** @since 20060719 */
	public void setParent( ThreadGroup group ){
		myParent = group;
	}

	private ThreadGroup myComputationThreadGroup, myCleanupThreadGroup, myParent;
	private int         myNumInterrupted = (int)0;
	private Object      mySynchComputation = new Object();

	protected DecimalFormat myPercentCompleteFormat = new DecimalFormat( "###%" );
	protected double myProgressCount = 0;

	public void progressMade( double progressCount )
	{
		synchronized( mySynchronization )
		{
			myProgressCount = progressCount;
			if( myNumRunning > 0 )
			{
				//lblPercentCompleteCaption
				double expectedCalls = mySettings.getInfo().recursiveCalls();
				myLabelPercentComplete.setText( myPercentCompleteFormat.format( (double)progressCount / expectedCalls ) );
				//lblPercentCompleteUnits
			}
		}
	}

	public static String STR_ONEHUNDRED_PERCENT = "100%";
	public static String STR_ZERO_PERCENT = "0%";

	public void forceCompletePercentComplete( String text )
	{
		myLabelPercentComplete.setText( text );
	}

	/** @since 20030605 */
	protected void setAllocationInterferingWidgetsEnabled()
	{
		boolean flagNotRunning = myNumRunning < 1;

		mySliderUserMemory.setEnabled( flagNotRunning );
		//myCBuseKB.setEnabled( flagNotRunning );
		myButtonRun.setEnabled( flagNotRunning );
		myRadioPRE.setEnabled( flagNotRunning );
		myRadioMPE.setEnabled( false );//flagNotRunning );
		//myButtonSaveRCDgraph.setEnabled( flagNotRunning && ( myRCDtree != null ) );
		//myButtonOpenRCDgraph.setEnabled( flagNotRunning );
		for( int i = 0; i < myTabbedPane.getTabCount(); i++ ) myTabbedPane.setEnabledAt( i, flagNotRunning );
	}

	protected void setCalculationInterferingWidgetsEnabled()
	{
		//System.out.println( "RCPanel.sCIWE(), myNumRunning? " + myNumRunning );
		boolean flagNotRunning = myNumRunning < 1;

		if( myButtonAllocateMemory != null ) myButtonAllocateMemory.setEnabled( flagNotRunning );
		setPercentCompleteEnabled( !flagNotRunning );
		mySliderUserMemory.setEnabled( flagNotRunning );
		//myCBuseKB.setEnabled( flagNotRunning );
		myButtonRun.setEnabled( flagNotRunning );
		myRadioPRE.setEnabled( flagNotRunning );
		myRadioMPE.setEnabled( false );//flagNotRunning );
		//myButtonSaveRCDgraph.setEnabled( flagNotRunning && ( myRCDtree != null ) );
		//myButtonOpenRCDgraph.setEnabled( flagNotRunning );
		for( int i = 0; i < myTabbedPane.getTabCount(); i++ ) myTabbedPane.setEnabledAt( i, flagNotRunning );
	}

	/** @since 20050215 */
	public void addListener( edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener listener ){
		if( myListeners == null ) myListeners = new WeakLinkedList();
		else if( myListeners.contains( listener ) ) return;
		myListeners.add( listener );
	}

	/** @since 20050215 */
	public boolean removeListener( edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener listener ){
		if( myListeners == null ) return false;
		else return myListeners.remove( listener );
	}

	private WeakLinkedList myListeners;

	/** @since 20050215 */
	private void fireRcFinished( double result, long time_ms ){
		if( myListeners == null ) return;
		edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener listener;
		for( Iterator it = myListeners.iterator(); it.hasNext(); ){
			listener = (edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener) it.next();
			if( listener == null ) it.remove();
			else listener.rcFinished( result, time_ms );
		}
	}

	/** @since 20050215 */
	private void fireRcComputationError( String msg ){
		if( myListeners == null ) return;
		edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener listener;
		for( Iterator it = myListeners.iterator(); it.hasNext(); ){
			listener = (edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener) it.next();
			if( listener == null ) it.remove();
			else listener.rcComputationError( msg );
		}
	}

	/** @since 20050215 */
	private void fireRcFinishedMPE( double result, Map instantiation, long time_ms ){
		if( myListeners == null ) return;
		edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener listener;
		for( Iterator it = myListeners.iterator(); it.hasNext(); ){
			listener = (edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener) it.next();
			if( listener == null ) it.remove();
			else listener.rcFinishedMPE( result, instantiation, time_ms );
		}
	}

	/** interface RCDtree.RecCondThreadListener
		@since 20020724 */
	public void rcFinished( double result, long time_ms )
	{

	synchronized( mySynchronization )
		{

		if( result < (double)0 ) showErrorMessage( "Recursive conditioning out of memory.", "Error" );
		else
		{
			forceCompletePercentComplete( STR_ONEHUNDRED_PERCENT );

			double millisPerRCCall = (double)time_ms/(double)myProgressCount;
			if( time_ms > (long)1000 )
			{
				mySettings.setMillisPerRCCall( millisPerRCCall );
			}

			setResultProbabilityEnabled( true );
			myLabelProbability.setText( theProbabilityFormat.format( result ) );

			updateElapsedTimeDisplay( (double)time_ms );
		}

		myButtonRun.setEnabled( true );
		myButtonPause.setEnabled( false );
		myButtonResume.setEnabled( false );
		myButtonCancel.setEnabled( false );
		setCalculationInterferingWidgetsEnabled();

		fireRcFinished( result, time_ms );

		}
	}

	/** interface RC.RecCondThreadListener
		@since 20030610 */
	public void rcComputationError( String msg ){
		this.rcComputationStop( msg, "RC Computation Error" );
	}

	/** @since 20050519 */
	public void rcComputationStop( String msg, String title )
	{
		boolean interrupted = Thread.interrupted();
		showErrorMessage( msg, title );

		synchronized( mySynchronization )
		{

		myButtonRun.setEnabled( true );
		myButtonPause.setEnabled( false );
		myButtonResume.setEnabled( false );
		myButtonCancel.setEnabled( false );
		setCalculationInterferingWidgetsEnabled();

		fireRcComputationError( msg );

		}

		if( interrupted ) Thread.currentThread().interrupt();
	}

	/** interface RCDtree.RecCondThreadListener
		@since 20020724 */
	public void rcFinishedMPE( double result, Map instantiation, long time_ms )
	{
		synchronized( mySynchronization )
		{

		if( result < (double)0 )
		{
			showErrorMessage( "Recursive conditioning MPE out of memory.", "Error" );
			setCalculationInterferingWidgetsEnabled();
		}
		else
		{
			forceCompletePercentComplete( STR_ONEHUNDRED_PERCENT );

			setCalculationInterferingWidgetsEnabled();
			myLabelProbability.setText( theProbabilityFormat.format( result ) );

			MPEPanel newPanel = new MPEPanel( result, instantiation, myBeliefNetwork );
			myMPEPanelActionSource = newPanel.addActionListener( myActionListener );
			newPanel.setClipBoard( myNetworkInternalFrame.getParentFrame().getInstantiationClipBoard(), myBeliefNetwork.getEvidenceController() );
			myTabbedPane.add( newPanel, STR_TAB_TITLE_MPE );

			setResultProbabilityEnabled( true );

			updateElapsedTimeDisplay( (double)time_ms );
		}

		myButtonRun.setEnabled( true );
		myButtonPause.setEnabled( false );
		myButtonResume.setEnabled( false );
		myButtonCancel.setEnabled( false );

		fireRcFinishedMPE( result, instantiation, time_ms );

		}
	}

	protected static final String STR_TAB_TITLE_MPE = "MPE";
	protected Object myMPEPanelActionSource = null;

	protected void clobberMPETab()
	{
		int indexMPETab = myTabbedPane.indexOfTab( STR_TAB_TITLE_MPE );
		if( indexMPETab != (int)-1 )
		{
			if( myTabbedPane.getSelectedIndex() == indexMPETab ) myTabbedPane.setSelectedComponent( myAllocationPanel );
			myTabbedPane.remove( indexMPETab );
		}
		myButtonShowMPE.setEnabled( false );
	}

	protected void selectMPETab()
	{
		int indexMPETab = myTabbedPane.indexOfTab( STR_TAB_TITLE_MPE );
		if( indexMPETab != (int)-1 ) myTabbedPane.setSelectedIndex( indexMPETab );
	}

	public static final long LONG_PAUSE_GRACE_MILLIS = (long)64;
	public static final long LONG_PAUSE_GRUDGE_MILLIS = (long)512;
	public static final int INT_PAUSES_BEFORE_STOP = (int)8;
	public static final int INT_PAUSES_BEFORE_TIMEOUT = (int)16;

	private void stopRuns() throws InterruptedException
	{
		getComputationThreadGroup().interrupt();
		Thread.sleep( LONG_PAUSE_GRACE_MILLIS );

		int waits = 0;
		while( (myNumRunning > 0) && (waits++ < INT_PAUSES_BEFORE_STOP) ){
			Thread.sleep( LONG_PAUSE_GRUDGE_MILLIS );
		}

		if( myNumRunning > 0 ){
			//System.out.println( "    stopRuns() calling stop after " + (waits*LONG_PAUSE_GRUDGE_MILLIS) + " ms" );

			getComputationThreadGroup().stop();

			while( myNumRunning > 0 ){
				if( waits++ > INT_PAUSES_BEFORE_TIMEOUT ){
					//System.out.println( "    stopRuns() timeout after " + (waits*LONG_PAUSE_GRUDGE_MILLIS) + " ms" );
					Thread.currentThread().interrupt();
				}
				Thread.sleep( LONG_PAUSE_GRUDGE_MILLIS );
			}
		}

		synchronized( mySynchronization ){
			//myFlagRecursiveConditioningRunning = false;
			if( myButtonRun == null ) return;

			myButtonRun.setEnabled( true );
			myButtonPause.setEnabled( false );
			myButtonResume.setEnabled( false );
			myButtonCancel.setEnabled( false );

			setCalculationInterferingWidgetsEnabled();
		}
	}

	protected void setWaitCursor()
	{
		setCursor( UI.CURSOR_WAIT );
	}

	protected void setDefaultCursor()
	{
		setCursor( UI.CURSOR_DEFAULT );
	}

	protected void generateDgraphAndUpdateDisplay()
	{
		String msgThrowable = null;
		try{
			if( mySettings == null ){
				msgThrowable = "null RCSettings";
				return;
			}

			if( myBeliefNetwork == null ){
				msgThrowable = "null BeliefNetwork";
				return;
			}

			if( mySettings.validateRC( myBeliefNetwork ) ){
				updateDgraphStatsDisplay();
				updateOptimalMemoryNumber();
				updateEstimatedTime();
				mySettingsMightHaveChanged = false;
			}
		}catch( Throwable throwable ){
			msgThrowable = throwable.getMessage();
			if( msgThrowable == null ) msgThrowable = throwable.toString();
		}finally{
			if( msgThrowable != null )
				showErrorMessage( "Failed to generate new Dgraph based on settings!\n" + msgThrowable, "Error" );
		}
	}

	/** Overridden to do nothing
		@since 20030326 */
	public void setVisible(boolean aFlag)
	{
		//System.out.println( "RCPanel.setVisible("+aFlag+")" );
		//new Throwable().printStackTrace();

		//super.setVisible( aFlag );

		//setVisibleSafe( aFlag, myContainerAbstraction.isVisible() );
		//myContainerAbstraction.setVisible( aFlag );
	}

	public void setVisibleSafe( boolean to, boolean from )
	{
		synchronized( mySynchronization )
		{

		//System.out.println( "RCPanel.setVisibleSafe( "+to+", "+from+" )" );

		if( to && !from )
		{ //if it has been hidden, it may not be up to date
			myResizeAdapter.componentResized( new ComponentEvent( this, 0 ) );
			generateDgraphAndUpdateDisplay();
			//if( FLAG_DEBUG ) showDebugControlPanel();
		}
		else if( from )
		{ //if hiding, stop recCond from running
			//hideMPE();
			try{
				stopRuns();
			}catch( InterruptedException interruptedexception ){
				System.err.println( "Warning: " + interruptedexception );
			}
		}

		setVisible( to );

		}
	}

	protected boolean generateTrees()
	{
		synchronized( mySynchronization )
		{

		String errmsg = null;

		if( myNumRunning > 0 ) errmsg = "Error: call to RCPanel.generateTrees() while a calculation was running.";
		else if( myBeliefNetwork == null ) errmsg = "RCPanel.generateTrees(), null BeliefNetwork";
		//else if( myRunningCacheAllocationThread != null ) myRunningCacheAllocationThread = mySettings.allocRCDtreeInThread( myBeliefNetwork );

		if( errmsg != null )
		{
			System.err.println( errmsg );
			return false;
		}
		else return true;

		}
	}

	public void stateChanged( javax.swing.event.ChangeEvent e )
	{
		Object src = e.getSource();
		if( src == mySliderUserMemory && myFlagListenEvents )
		{
			if( myNumRunning < 1 )
			{
				double newMemoryProportion = (double)mySliderUserMemory.getValue()/(double)INT_MAX_SLIDER;

				myLabelUserMemory.setText( mySettings.updateUserMemoryDisplay( newMemoryProportion ) );

				if( !mySliderUserMemory.getValueIsAdjusting() )
				{
					if( mySettings.setUserMemoryProportion( newMemoryProportion ) )
					{
						updateUserMemoryDisplay();
						updateEstimatedTime();
					}
					//if( myRunningCacheAllocationThread != null && myRCDtree == null ) myRunningCacheAllocationThread = mySettings.allocRCDtreeInThread( myBeliefNetwork );
				}
			}
		}
		else if( src == myTabbedPane )
		{
			try{
				Object selected = myTabbedPane.getSelectedComponent();
				if( (selected == myAllocationPanel) && mySettingsMightHaveChanged )
				//if( myTabbedPane.getSelectedIndex() == (int)0 && mySettingsMightHaveChanged )
				{
					if( myNumRunning < 1 )
					{
						int result = JOptionPane.showConfirmDialog( this, "You changed one of the Dtree settings.\nWould you like to generate a new Dtree now?", "Generate new Dtree?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );
						if( result == JOptionPane.OK_OPTION ) doGenerateInfo();//generateDgraphAndUpdateDisplay();
						mySettingsMightHaveChanged = false;
					}
				}
				else if( selected == myPanelDgraphSettings ){
					EliminationHeuristic heuristic = mySettings.getEliminationHeuristic();
					Object current = myComboBoxElimAlgo.getSelectedItem();
					if( current != heuristic ) myComboBoxElimAlgo.setSelectedItem( heuristic );
				}
			}catch( Exception exception ){
				System.err.println( exception );
			}
		}
	}

	/** @since 20031125 */
	private void initEstimatedTimeDisplay( RCSettings settings )
	{
		myLabelTimeCaption.setText( STR_ESTIMATED_TIME_CAPTION );
		String[] newVals = settings.updateEstimatedTimeDisplay( settings.getInfo().recursiveCallsFullCaching() );
		myLabelEstimatedTime.setText( newVals[0] );
		lblEstimatedTimeUnits.setText( newVals[1] );
	}

	protected void updateEstimatedMinutesDisplay()
	{
		myLabelTimeCaption.setText( STR_ESTIMATED_TIME_CAPTION );

		String[] newVals = mySettings.updateEstimatedMinutesDisplay();
		myLabelEstimatedTime.setText( newVals[0] );
		lblEstimatedTimeUnits.setText( newVals[1] );
	}

	protected void updateElapsedTimeDisplay( double milliseconds )
	{
		String[] newVals = mySettings.updateEstimatedMillisDisplay( milliseconds );

		myLabelTimeCaption.setText( STR_ELAPSED_TIME_CAPTION );
		myLabelEstimatedTime.setText( newVals[0] );
		lblEstimatedTimeUnits.setText( newVals[1] );
	}

	protected void updateOptimalMemoryNumber()
	{
		myLabelEstimatedTime.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );

		//if( myRCDtree == null ) mySettings.updateOptimalMemoryNumber( myBeliefNetwork );
		//else mySettings.updateOptimalMemoryNumber( myRCDtree );

		updateMemoryDisplay();
	}

	protected void updateMemoryDisplay()
	{
		if( mySettings == null ) return;

		boolean valid = false;
		try{ valid = mySettings.validateRC( myBeliefNetwork ); }
		catch( Throwable throwable ){
			valid = false;
			System.err.println( throwable );
			return;
		}

		if( valid ){
			String[] newVals = mySettings.updateOptimalMemoryDisplay();

			if( myMemoryUnit != newVals[1] )
			{
				myMemoryUnit = newVals[1];
				updateMemoryUnitsDisplay();
			}

			myLabelOptimalMemory.setText( newVals[0] );
			updateUserMemoryDisplay();
		}
	}

	protected void updateUserMemoryDisplay()
	{
		myLabelUserMemory.setText( mySettings.updateUserMemoryDisplay() );
	}

	protected String myMemoryUnit = RCSettings.STR_KILOBYTE_UNIT;
	private   Font   myFontNormal, myFontBold;

	private void updateMemoryUnitsDisplay()
	{
		if( myMemoryUnit.equals( RCSettings.STR_MEGABYTE_UNIT ) )
		{
			if( myFontBold == null )
			{
				myFontNormal = myLabelOptimalMemoryUnits.getFont();
				myFontBold = myFontNormal.deriveFont( Font.BOLD );
			}
			myLabelOptimalMemoryUnits.setFont( myFontBold );
			myLabelUserMemoryUnits.setFont( myFontBold );

			myLabelOptimalMemoryUnits.setForeground( Color.red );
			myLabelUserMemoryUnits.setForeground( Color.red );
		}
		else if( myFontBold != null )
		{
			myLabelOptimalMemoryUnits.setFont( myFontNormal );
			myLabelUserMemoryUnits.setFont( myFontNormal );

			myLabelOptimalMemoryUnits.setForeground( Color.black );
			myLabelUserMemoryUnits.setForeground( Color.black );
		}

		myLabelOptimalMemoryUnits.setText( myMemoryUnit );
		myLabelUserMemoryUnits.setText( myMemoryUnit );
	}

	protected JCheckBox myCheckBoxEstimatedTime;
	protected JButton   myButtonAllocateMemory, myButtonStopMemoryAllocation;
	protected JPanel    myPanelMemoryAllocationButtons;
	protected JLabel    lblEstimatedTimeUnits;

	public static final String STR_ESTIMATED_TIME_DISABLED = CachePreviewDialog.STR_TEXT_UNAVAILABLE;

	protected void setEstimatedTimeEnabled( boolean enabled )
	{
		if( !enabled ) myLabelEstimatedTime.setText( STR_ESTIMATED_TIME_DISABLED );
		myLabelEstimatedTime.setEnabled( enabled );
		lblEstimatedTimeUnits.setEnabled( enabled );
	}

	protected void showErrorMessage( String message, String title )
	{
		JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
	}

	protected JLabel myLabelOptimalMemoryUnits = null;
	protected JLabel myLabelUserMemoryUnits = null;
	protected JComponent myPanelResults = null;
	protected static final boolean FLAG_SHOW_CHECKBOX_ESTIMATED_TIME = false;

	protected JLabel myLabelTimeCaption = null;
	protected static String STR_ESTIMATED_TIME_CAPTION = "Estimated Pr(e) run time: ";
	protected static String STR_ELAPSED_TIME_CAPTION = (JVMProfiler.profilerRunning()) ? "Elapsed time (thread profile): " : "Elapsed time (system clock): ";

	protected JComponent makeAllocationPanel( boolean showCalculationPanel, boolean showRCDtreeFilePanel )
	{
		myLabelOptimalMemory = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE, JLabel.RIGHT );
		Dimension dim  = myLabelOptimalMemory.getPreferredSize();
		dim.width = 64;
		myLabelOptimalMemory.setPreferredSize( dim );

		if( FLAG_DEBUG_BORDERS ) myLabelOptimalMemory.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		JLabel lblMemoryCaption = new JLabel( "Memory required for optimal Pr(e) running time: " );
		if( FLAG_DEBUG_BORDERS ) lblMemoryCaption.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		JLabel lblMemoryQuestion = new JLabel( "How much memory would you like to use? " );
		if( FLAG_DEBUG_BORDERS ) lblMemoryQuestion.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		myLabelUserMemory = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE, JLabel.RIGHT );
		if( FLAG_DEBUG_BORDERS ) myLabelUserMemory.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		myLabelOptimalMemoryUnits = new JLabel( " KB", JLabel.LEFT );
		if( FLAG_DEBUG_BORDERS ) myLabelOptimalMemoryUnits.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		myLabelUserMemoryUnits = new JLabel( " KB", JLabel.LEFT );
		if( FLAG_DEBUG_BORDERS ) myLabelUserMemoryUnits.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		mySliderUserMemory = new JSlider( JSlider.HORIZONTAL, INT_MIN_SLIDER, INT_MAX_SLIDER, INT_MAX_SLIDER );
		mySliderUserMemory.setMajorTickSpacing(25);
		mySliderUserMemory.setMinorTickSpacing(5);
		mySliderUserMemory.setPaintTicks(true);
		mySliderUserMemory.addChangeListener( this );

		//myCBuseKB = new JCheckBox( "take advantage of determinism" );
		//myCBuseKB.addActionListener( myActionListener );

		myCheckBoxEstimatedTime = new JCheckBox( "Estimated running time: " );
		if( FLAG_SHOW_CHECKBOX_ESTIMATED_TIME )
		{
			myCheckBoxEstimatedTime.setSelected( true );
			if( FLAG_DEBUG_BORDERS ) myCheckBoxEstimatedTime.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
			myCheckBoxEstimatedTime.addActionListener( myActionListener );
		}
		else
		{
			myCheckBoxEstimatedTime.setSelected( false );

			myButtonAllocateMemory = new JButton( CacheSettingsPanel.STR_TEXT_SEARCH_BUTTON );
			myButtonAllocateMemory.addActionListener( myActionListener );
			myButtonStopMemoryAllocation = new JButton( "Stop" );
			//myButtonStopMemoryAllocation.addActionListener( myActionListener );
			myButtonStopMemoryAllocation.setEnabled( false );

			myPanelMemoryAllocationButtons = new JPanel();
			myPanelMemoryAllocationButtons.add( myButtonAllocateMemory );
			//myPanelMemoryAllocationButtons.add( myButtonStopMemoryAllocation );
		}

		myLabelTimeCaption = new JLabel( STR_ESTIMATED_TIME_CAPTION, JLabel.RIGHT );
		dim  = myLabelTimeCaption.getPreferredSize();
		dim.width = 190;
		myLabelTimeCaption.setPreferredSize( dim );

		myLabelEstimatedTime = new JLabel( STR_ESTIMATED_TIME_DISABLED, JLabel.RIGHT );
		if( FLAG_DEBUG_BORDERS ) myLabelEstimatedTime.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		lblEstimatedTimeUnits = new JLabel( RCSettings.STR_MINUTE_UNIT, JLabel.LEFT );
		if( FLAG_DEBUG_BORDERS ) lblEstimatedTimeUnits.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		setEstimatedTimeEnabled( true );

		dim  = lblEstimatedTimeUnits.getPreferredSize();
		dim.width = 75;
		lblEstimatedTimeUnits.setPreferredSize( dim );

		if( showCalculationPanel ) myPanelResults = makeResultsPanel();

		if( (myBeliefNetwork != null) && (myBeliefNetwork.size() > 1) ){
			boolean valid = false;
			try{ valid = mySettings.validateRC( myBeliefNetwork ); }
			catch( Throwable throwable ){
				valid = false;
				System.err.println( "Warning: RCPanel.makeAllocationPanel() caught " + throwable );
				//throwable.printStackTrace();
			}
			if( valid ) updateMemoryDisplay();
		}

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );

		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.ipadx = 0;
		c.ipady = 16;
		c.weightx = 0;

		c.gridwidth = 2;
		ret.add( lblMemoryCaption, c );

		c.weightx = 1;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( myLabelOptimalMemory, c );

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelOptimalMemoryUnits, c );

		//c.gridy++;
		c.gridwidth = 2;
		ret.add( lblMemoryQuestion, c );

		c.weightx = 1;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( myLabelUserMemory, c );

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelUserMemoryUnits, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( mySliderUserMemory, c );

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		//ret.add( myCBuseKB, c );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		if( FLAG_SHOW_CHECKBOX_ESTIMATED_TIME )
		{
			ret.add( myCheckBoxEstimatedTime, c );
		}
		else
		{
			c.ipady = (int)0;
			ret.add( myPanelMemoryAllocationButtons, c );
			c.ipady = (int)16;
		}

		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( myLabelTimeCaption, c );

		c.weightx = 1;
		ret.add( myLabelEstimatedTime, c );

		c.weightx = 0.1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( lblEstimatedTimeUnits, c );

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.ipady = 0;
		Component strut = Box.createVerticalStrut( 10 );
		ret.add( strut, c );

		if( showCalculationPanel )
		{
			c.weightx = 1;
			ret.add( myPanelResults, c );
		}

		ret.setBorder( BorderFactory.createEmptyBorder( 8,8,8,8 ) );

		return ret;
	}

	protected JLabel myLabelDgraphHeight, myLabelDgraphMaxCluster, myLabelDgraphMaxCutset, myLabelDgraphMaxContext;

	private void updateDgraphStatsDisplay()
	{
		RCInfo info = mySettings.getInfo();
		if( info == null )
		{
			clearDgraphStatsDisplay();
			return;
		}

		//System.out.println( "DgraphPreviewDialogNew.updateDgraphStatsDisplay() info.height() " + info.height() + " info.maxClusterSize() " + info.maxClusterSize() + " info.maxCutsetSize() " + info.maxCutsetSize() + " info.maxContextSize() " + info.maxContextSize() );

		myLabelDgraphHeight.setText( String.valueOf( info.height() ) );
		//myLabelDgraphHeight.setMinimumSize( myLabelDgraphHeight.getPreferredSize() );
		myLabelDgraphMaxCluster.setText( String.valueOf( info.maxClusterSize() ) );
		//myLabelDgraphMaxCluster.setMinimumSize( myLabelDgraphMaxCluster.getPreferredSize() );
		myLabelDgraphMaxCutset.setText( String.valueOf( info.maxCutsetSize() ) );
		myLabelDgraphMaxContext.setText( String.valueOf( info.maxContextSize() ) );
		//myLabelDgraphDiameter.setText( String.valueOf( info.diameter() ) );
	}

	private void clearDgraphStatsDisplay()
	{
		myLabelDgraphHeight.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxCluster.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxCutset.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxContext.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		//myLabelDgraphDiameter.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
	}

	protected JComponent makeDtreeStatsPanel()
	{
		//JLabel lblDtreeStatsCaption = new JLabel( "Dtree properties: height = " );
		JLabel lblDtreeStatsCaption = new JLabel( " height = " );
		JLabel lblClusterCaption = new JLabel( " max cluster = " );
		JLabel lblCutsetCaption = new JLabel( " cutset = " );
		JLabel lblContextCaption = new JLabel( " context = " );
		myLabelDgraphHeight = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE);
		Dimension dim = myLabelDgraphHeight.getPreferredSize();
		dim.width = 16;
		myLabelDgraphHeight.setMinimumSize( dim );
		myLabelDgraphMaxCluster = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxCluster.setMinimumSize( dim );
		myLabelDgraphMaxCutset = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxCutset.setMinimumSize( dim );
		myLabelDgraphMaxContext = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxContext.setMinimumSize( dim );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );

		c.gridwidth =(int)1;
		c.weightx = 0;
		ret.add( lblDtreeStatsCaption, c );

		c.weightx = 1;
		ret.add( myLabelDgraphHeight, c );

		c.weightx = 0;
		ret.add( lblClusterCaption, c );

		c.weightx = 1;
		ret.add( myLabelDgraphMaxCluster, c );

		c.weightx = 0;
		ret.add( lblCutsetCaption, c );

		c.weightx = 1;
		ret.add( myLabelDgraphMaxCutset, c );

		c.weightx = 0;
		ret.add( lblContextCaption, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		ret.add( myLabelDgraphMaxContext, c );

		Border b = null;
		if( FLAG_DEBUG_BORDERS ) b = BorderFactory.createLineBorder( Color.red, (int)1 );
		else b = BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Current Dgraph Properties" );

		ret.setBorder( b );

		return ret;
	}

	protected JLabel lblPercentCompleteCaption = null;
	//protected JLabel lblPercentCompleteUnits = null;

	protected void setPercentCompleteEnabled( boolean enabled )
	{
		lblPercentCompleteCaption.setEnabled( enabled );
		myLabelPercentComplete.setEnabled( enabled );
		//lblPercentCompleteUnits.setEnabled( enabled );
	}

	protected JLabel lblProbabilityCaption = null;
	protected static final String STR_PRE_CAPTION = "Pr(e) = ";
	protected static final String STR_MPE_CAPTION = "Pr(mpe) = ";

	protected void setResultProbabilityEnabled( boolean enabled )
	{
		lblProbabilityCaption.setEnabled( enabled );
		myLabelProbability.setEnabled( enabled );
		myButtonShowMPE.setEnabled( myTabbedPane.indexOfTab( STR_TAB_TITLE_MPE ) != (int)-1 );
	}

	protected JComponent myPanelResultsButtons;
	protected JComponent myPanelPercentComplete;

	protected void setResultsPanelColor( Color c )
	{
		if( myPanelResults != null ) myPanelResults.setBackground( c );
		if( myPanelResultsButtons != null ) myPanelResultsButtons.setBackground( c );
		if( myRadioPRE != null ) myRadioPRE.setBackground( c );
		if( myRadioMPE != null ) myRadioMPE.setBackground( c );
		if( myPanelShowMPE != null ) myPanelShowMPE.setBackground( c );
		//myPanelPercentComplete.setBackground( c );
	}

	protected JRadioButton myRadioPRE = null;
	protected JRadioButton myRadioMPE = null;
	protected JButton myButtonShowMPE = null;
	protected JPanel myPanelShowMPE = null;

	public static final String STR_TEXT_BUTTON_RUN_RC = "Run";

	/** @since 20050215 */
	public JComponent getPanelResults(){
		return myPanelResults;
	}

	protected JComponent makeResultsPanel()
	{
		lblPercentCompleteCaption = new JLabel( "Percent complete: ", JLabel.RIGHT );
		if( FLAG_DEBUG_BORDERS ) lblPercentCompleteCaption.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		myLabelPercentComplete = new JLabel( "------", JLabel.LEFT );
		if( FLAG_DEBUG_BORDERS ) myLabelPercentComplete.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		myButtonShowMPE = new JButton( "View MPE Result" );
		myButtonShowMPE.addActionListener( myActionListener );
		myPanelShowMPE = new JPanel( new GridLayout() );
		myPanelShowMPE.setPreferredSize( myButtonShowMPE.getPreferredSize() );
		if( FLAG_DEBUG_BORDERS ) myPanelShowMPE.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );

		Dimension dim = myLabelPercentComplete.getPreferredSize();
		dim.width += 16;
		myLabelPercentComplete.setPreferredSize( dim );

		//lblPercentCompleteUnits = new JLabel( "" );
		setPercentCompleteEnabled( false );

		myRadioPRE = new JRadioButton( "Pr(e)" );
		myRadioPRE.addActionListener( myActionListener );
		myRadioMPE = new JRadioButton( "MPE" );
		myRadioMPE.addActionListener( myActionListener );
		myRadioMPE.setEnabled( false );
		ButtonGroup bg = new ButtonGroup();
		bg.add( myRadioPRE );
		bg.add( myRadioMPE );
		myRadioPRE.setSelected( true );

		lblProbabilityCaption = new JLabel( STR_PRE_CAPTION, JLabel.RIGHT );
		Font fontProbability = lblProbabilityCaption.getFont();
		fontProbability = fontProbability.deriveFont( (float)18 );
		fontProbability = fontProbability.deriveFont( Font.BOLD );
		lblProbabilityCaption.setFont( fontProbability );
		if( FLAG_DEBUG_BORDERS ) lblProbabilityCaption.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		myLabelProbability = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE );

		dim = myLabelProbability.getPreferredSize();
		dim.width += 128;
		myLabelProbability.setPreferredSize( dim );

		myLabelProbability.setFont( fontProbability );
		//if( FLAG_DEBUG_BORDERS )
			//myLabelProbability.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		setResultProbabilityEnabled( false );

		myButtonRun = new JButton( STR_TEXT_BUTTON_RUN_RC );
		myButtonRun.addActionListener( myActionListener );
		myButtonRun.setEnabled( true );
		myButtonCancel = new JButton( "Cancel" );
		myButtonCancel.addActionListener( myActionListener );
		myButtonCancel.setEnabled( false );
		myButtonPause = new JButton( "Pause" );
		myButtonPause.addActionListener( myActionListener );
		myButtonPause.setEnabled( false );
		myButtonResume = new JButton( "Resume" );
		myButtonResume.addActionListener( myActionListener );
		myButtonResume.setEnabled( false );
		Component vstrut = Box.createVerticalStrut( 16 );

		myPanelResultsButtons = new JPanel();
		myPanelResultsButtons.add( myButtonRun );
		myPanelResultsButtons.add( myButtonCancel );
		//myPanelResultsButtons.add( myButtonPause );
		//myPanelResultsButtons.add( myButtonResume );
		if( FLAG_DEBUG_BORDERS ) myPanelResultsButtons.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );

		c.fill = GridBagConstraints.NONE;
		c.ipadx = 0;
		c.gridwidth = 1;
		c.weightx = 0;
		c.anchor = GridBagConstraints.EAST;
		ret.add( Box.createHorizontalStrut( INT_STRUT_SIZE ), c );

		c.gridwidth = 1;
		c.weightx = 0;
		ret.add( lblProbabilityCaption, c );

		c.anchor = GridBagConstraints.WEST;
		c.weightx = 10;
		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( myLabelProbability, c );

		/*c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		ret.add( myButtonRun );*/

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		ret.add( myPanelResultsButtons, c );

		/*c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.NONE;
		ret.add( Box.createHorizontalStrut( INT_STRUT_SIZE ), c );*/

		ret.setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Results" ) );

		myColorBackgroundNormal = ret.getBackground();

		return ret;
	}

	public void updateUI()
	{
		super.updateUI();
		myColorBackgroundNormal = UIManager.getDefaults().getColor( "Panel.background" );
		if( myPanelHMETIS != null ) SwingUtilities.updateComponentTreeUI( myPanelHMETIS );
		if( myPanelEliminationOrder != null ) SwingUtilities.updateComponentTreeUI( myPanelEliminationOrder );
		if( myButtonShowMPE != null ) myButtonShowMPE.updateUI();
	}

	protected JComboBox myComboBoxMethod = null;
	protected JPanel myPanelMethod = null;
	protected JComponent myPanelHMETIS = null;
	protected JComponent myPanelEliminationOrder = null;
	protected JComponent myPanelHuginLog = null;
	protected JCheckBox myCheckBoxKeepBest = null;
	protected JButton myButtonGenerateDtree = null;
	//protected JButton myButtonOpenDtree = null;
	//protected JButton myButtonSaveDtree = null;

	protected static boolean FLAG_HMETIS_LOADED = Hmetis.loaded();

	public static final String STR_TEXT_BUTTON_GENERATE_DGRAPH = "Generate";

	protected JComponent makeSettingsPanel()
	{
		JLabel lblMethod = new JLabel( "Dgraph Method: ", JLabel.RIGHT );
		Object[] arrayMethods = null;
		if( false )//FLAG_HMETIS_LOADED )
		{
			myPanelHMETIS = makeHMETISPanel();
			//arrayMethods = RCSettings.ARRAY_THREE_METHODS;
		}
		//else arrayMethods = RCSettings.ARRAY_THREE_METHODS_NOT_LOADED;
		arrayMethods = CreationMethod.getArray();

		myComboBoxMethod = new JComboBox( arrayMethods );
		myComboBoxMethod.addActionListener( myActionListener );
		myComboBoxMethod.setEnabled( false );
		myPanelMethod = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		myPanelMethod.setPreferredSize( new Dimension( 375, 101 ) );
		if( FLAG_DEBUG_BORDERS ) myPanelMethod.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		myPanelEliminationOrder = makeEliminationOrderPanel();
		myPanelMethod.add( myPanelEliminationOrder );

		//myPanelHuginLog = makeHuginLogPanel();

		myCheckBoxKeepBest = new JCheckBox( "Keep Best" );
		myCheckBoxKeepBest.setSelected( true );
		myButtonGenerateDtree = new JButton( STR_TEXT_BUTTON_GENERATE_DGRAPH );
		myButtonGenerateDtree.addActionListener( myActionListener );

		JPanel pnlButtons = new JPanel();
		//pnlButtons.add( myButtonOpenDtree );
		//pnlButtons.add( myButtonSaveDtree );
		Component strut = Box.createHorizontalStrut( (int)64 );
		pnlButtons.add( strut );
		pnlButtons.add( myCheckBoxKeepBest );
		pnlButtons.add( myButtonGenerateDtree );

		JComponent dtreeStatsPanel = makeDtreeStatsPanel();

		GridBagConstraints c = new GridBagConstraints();
		JPanel ret = new JPanel( new GridBagLayout() );

		/*
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = 1;
		ret.add( lblMethod, c );

		ret.add( Box.createHorizontalStrut( INT_STRUT_SIZE ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		ret.add( myComboBoxMethod, c );
		*/

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myPanelMethod, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		ret.add( Box.createVerticalStrut( 16 ), c );

		c.weightx = 1;
		ret.add( dtreeStatsPanel, c );

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		ret.add( Box.createVerticalStrut( 16 ), c );

		ret.add( pnlButtons, c );

		Border b = null;
		if( FLAG_DEBUG_BORDERS ) b = BorderFactory.createLineBorder( Color.green, 1 );
		else b = BorderFactory.createEmptyBorder( 8,16,8,16 );

		ret.setBorder( b );

		return ret;
	}

	protected void toggleMethod()
	{
		Object newObjMethod = myComboBoxMethod.getSelectedItem();

		JComponent compNew = null;
		if( newObjMethod instanceof MethodEliminationOrder ) compNew = myPanelEliminationOrder;
		else if( newObjMethod instanceof MethodHuginLog ) compNew = myPanelHuginLog;
		else if( newObjMethod instanceof MethodHmetis ) compNew  = myPanelHMETIS;
		else
		{
			if( mySettings != null )
			{
				myFlagListenEvents = false;
				//myComboBoxMethod.setSelectedItem( mySettings.getDtreeMethod() );
				myFlagListenEvents = true;
			}
			return;
		}

		//if( mySettings != null ) mySettings.setDtreeMethod( (CreationMethod) newObjMethod );

		if( !myPanelMethod.isAncestorOf( compNew ) )
		{
			myPanelMethod.removeAll();
			myPanelMethod.add( compNew );
		}

		myPanelMethod.validate();
		myPanelMethod.repaint();
	}

	protected JComboBox myComboBoxElimAlgo = null;

	protected JComponent makeEliminationOrderPanel()
	{
		JLabel lblAlgorithm = new JLabel( "Elimination heuristic: " );
		myComboBoxElimAlgo = new JComboBox( EliminationHeuristic.ARRAY );
		myComboBoxElimAlgo.addActionListener( myActionListener );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );
		Component strut = null;

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		ret.add( lblAlgorithm, c );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		ret.add( strut, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myComboBoxElimAlgo, c );

		if( FLAG_DEBUG_BORDERS ) ret.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );

		return ret;
	}

	public void showErrorMessage( String txt )
	{
		JOptionPane.showMessageDialog( this, txt, DtreeSettingsPanel.STR_GENERIC_TITLE, JOptionPane.ERROR_MESSAGE );
	}

	public void setDefaultDirectory( File defaultDirectory )
	{
		myDefaultDirectory = defaultDirectory;
	}

	protected JFileChooser myFileChooser = null;
	public static final String STR_EXTENSION_HLG = ".hlg";
	private FileFilter hlgFilter = null;
	protected File myDefaultDirectory;

	protected JTextField myTfHuginLogFile = null;
	protected JButton myBtnBrowse = null;
	protected JComboBox myCbHuginLogStyle = null;

	protected JComponent makeHuginLogPanel()
	{
		JLabel lblCaptionPath = new JLabel( "File path: " );
		myTfHuginLogFile = new NotifyField( "",(int)17 );
		Dimension dim = myTfHuginLogFile.getPreferredSize();
		dim.width = (int)50;
		myTfHuginLogFile.setMinimumSize( dim );
		myTfHuginLogFile.addActionListener( myActionListener );

		myBtnBrowse = new JButton( "Browse" );
		Dimension btnDim = myBtnBrowse.getPreferredSize();
		btnDim.height = dim.height;
		myBtnBrowse.setPreferredSize( btnDim );
		myBtnBrowse.addActionListener( myActionListener );

		JLabel lblCaptionStyle = new JLabel( "Style: " );
		myCbHuginLogStyle = new JComboBox( MethodHuginLog.ARRAY_DTREE_STYLES );
		myCbHuginLogStyle.addActionListener( myActionListener );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );
		Component strut = null;

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = 1;
		ret.add( lblCaptionPath, c );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		ret.add( strut, c );

		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( myTfHuginLogFile, c );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		ret.add( strut, c );

		c.fill = GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myBtnBrowse, c );

		strut = Box.createVerticalStrut( (int)4 );
		ret.add( strut, c );

		c.gridwidth = 1;
		ret.add( lblCaptionStyle, c );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		ret.add( strut, c );

		c.fill = GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myCbHuginLogStyle, c );

		if( FLAG_DEBUG_BORDERS ) ret.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );

		return ret;
	}

	protected JComboBox myComboBoxHMETISAlgo = null;
	protected WholeNumberField myWNFNumberDtrees = null;
	protected WholeNumberField myWNFNumberPartitions = null;

	protected JComboBox myComboBoxBalanceFactors = null;
	public static final int INT_STRUT_SIZE = (int)16;

	public static final int INT_NUM_DTREES_DEFAULT = (int)3;
	public static final int INT_NUM_DTREES_FLOOR = (int)1;
	public static final int INT_NUM_DTREES_CEILING = (int)999;

	public static final int INT_NUM_PARTITIONS_DEFAULT = (int)3;
	public static final int INT_NUM_PARTITIONS_FLOOR = (int)0;
	public static final int INT_NUM_PARTITIONS_CEILING = (int)999;

	protected JComponent makeHMETISPanel()
	{
		JLabel lblAlgo = new JLabel("Algorithm: ");
		JLabel lblNumDt = new JLabel("Number of dtrees to generate: ");
		JLabel lblNumPart = new JLabel("Number of partitions to generate: ");
		JLabel lblBal = new JLabel("Balance Factor: ");

		myComboBoxHMETISAlgo = new JComboBox( MethodHmetis.ARRAY_HMETIS_ALGOS );
		myComboBoxHMETISAlgo.addActionListener( myActionListener );

		myWNFNumberDtrees = new WholeNumberField( INT_NUM_DTREES_DEFAULT, 0, INT_NUM_DTREES_FLOOR, INT_NUM_DTREES_CEILING );
		myWNFNumberDtrees.addActionListener( myActionListener );
		myWNFNumberPartitions = new WholeNumberField( INT_NUM_PARTITIONS_DEFAULT, 0, INT_NUM_PARTITIONS_FLOOR, INT_NUM_PARTITIONS_CEILING );
		myWNFNumberPartitions.addActionListener( myActionListener );

		myComboBoxBalanceFactors = new JComboBox( MethodHmetis.ARRAY_BALANCE_FACTOR_ITEMS );
		myComboBoxBalanceFactors.addActionListener( myActionListener );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );
		Component strut = null;

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		ret.add( lblAlgo, c );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		ret.add( strut, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myComboBoxHMETISAlgo, c );

		c.gridwidth = 1;
		ret.add( lblNumDt, c );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		ret.add( strut, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myWNFNumberDtrees, c );

		c.gridwidth = 1;
		ret.add( lblNumPart, c );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		ret.add( strut, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myWNFNumberPartitions, c );

		c.gridwidth = 1;
		ret.add( lblBal, c );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		ret.add( strut, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myComboBoxBalanceFactors, c );

		if( FLAG_DEBUG_BORDERS ) ret.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );

		return ret;
	}


	protected boolean myFlagDtreesInvalid = false;

	public static boolean FLAG_DEBUG_LISTEN_CHANGES = false;

	/**
		interface EvidenceChangeListener
	*/
	public void warning( EvidenceChangeEvent ece ) {}

	/**
		interface EvidenceChangeListener
	*/
	public void evidenceChanged( EvidenceChangeEvent ECE )
	{
		if( FLAG_DEBUG_LISTEN_CHANGES ) Util.STREAM_VERBOSE.println( "RCPanel.evidenceChanged()" );
		somethingChangedSansDiscard();
	}

	public void cptChanged( CPTChangeEvent evt )
	{
		if( FLAG_DEBUG_LISTEN_CHANGES ) Util.STREAM_VERBOSE.println( "RCPanel.cptChanged()" );
		somethingChanged();
	}

	public void netStructureChanged( NetStructureEvent event )
	{
		if( FLAG_DEBUG_LISTEN_CHANGES ) Util.STREAM_VERBOSE.println( "RCPanel.netStructureChanged()" );
		somethingChanged();

		if( myContainerAbstraction != null ) myContainerAbstraction.setVisible( false );
	}

	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		if( FLAG_DEBUG_LISTEN_CHANGES ) Util.STREAM_VERBOSE.println( "RCPanel.nodePropertyChanged("+e+")" );
		if( e.property == EvidenceAssertedProperty.PROPERTY ) return;
		if( e.property == ImpactProperty.PROPERTY ) return;
		somethingChangedSansDiscard();
	}

	protected void somethingChangedSansDiscard()
	{
		synchronized( mySynchronization )
		{

		if( myNumRunning > 0 )
		{
			if( FLAG_DEBUG_LISTEN_CHANGES ) Util.STREAM_VERBOSE.println( "\tmyFlagRecursiveConditioningRunning" );
			myFlagDtreesInvalid = true;
			//cancelRecursiveConditioning();
		}
		setResultProbabilityEnabled( false );
		clobberMPETab();

		}
	}

	protected void somethingChanged()
	{
		synchronized( mySynchronization )
		{

		if( FLAG_DEBUG_LISTEN_CHANGES ) Util.STREAM_VERBOSE.println( "RCPanel.somethingChanged()" );
		if( myNumRunning > 0 )
		{
			if( FLAG_DEBUG_LISTEN_CHANGES ) Util.STREAM_VERBOSE.println( "\tmyFlagRecursiveConditioningRunning" );
			myFlagDtreesInvalid = true;
			//cancelRecursiveConditioning();
		}
		else
		{
			mySettings.setInfo( null );
		}
		setResultProbabilityEnabled( false );
		clobberMPETab();

		}
	}
}
