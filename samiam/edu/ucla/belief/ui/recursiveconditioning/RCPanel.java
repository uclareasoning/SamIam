package edu.ucla.belief.ui.recursiveconditioning;

import edu.ucla.structure.*;
import edu.ucla.belief.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.belief.recursiveconditioning.*;
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
	@since  20030122 */
public class RCPanel extends JPanel implements
  EvidenceChangeListener,
  javax.swing.event.ChangeListener,
  RC.RecCondThreadListener,
//RC.ProgressEventListener,
  CachingScheme.RCCreateListener,
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
	protected Settings             mySettings;
	protected boolean              myFlagGeneratorInterface;
	protected ActionListener       myActionListener;
	protected NetworkInternalFrame myNetworkInternalFrame;
	protected Object               mySynchronization = new Object();

	public RCPanel( NetworkInternalFrame hnInternalFrame, Container parent )
	{
		myFlagGeneratorInterface = false;

		mySettings = new Settings();
		mySettings.setOutStream( hnInternalFrame.console );
		mySettings.setRCFactory( new RCDtreeFactory() );
		mySettings.setRCCreateListener( this );
		myActionListener = new ToolActionListener();

		myNetworkInternalFrame = hnInternalFrame;
		if( hnInternalFrame != null ) myBeliefNetwork = hnInternalFrame.getBeliefNetwork();

		setListening( hnInternalFrame, myBeliefNetwork, true );

		init( hnInternalFrame, parent, true, true );
	}

	/** @since 20030318 */
	public static class RCDtreeFactory implements Settings.RCFactory
	{
		public RC manufactureRC( Dtree dtree, BeliefNetwork bn, CachingScheme cs, boolean useKB )
		{
			return RCEngineGenerator.createRCDtree( dtree, bn, /*scalar*/1.0, useKB, cs, /*includeMPE*/true);
		}
	}

	/** @since 20030124 */
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

	protected JTabbedPane myTabbedPane;
	protected JComponent myAllocationPanel;
	protected static NumberFormat theProbabilityFormat = new DecimalFormat( "0.0###############################################################################" );
	protected JComponent myResizePanel;
	protected ComponentListener myResizeAdapter;
	protected ContainerAbstraction myContainerAbstraction;

	protected void init( NetworkInternalFrame hnInternalFrame, Container parent, boolean showCalculationPanel, boolean showRCDtreeFilePanel )
	{
		//mySettingsMightHaveChanged = true;

		if( hnInternalFrame != null ) setBaseFileName( hnInternalFrame.getFileName() );

		myTabbedPane = new JTabbedPane();
		myTabbedPane.addChangeListener( this );

		myAllocationPanel = makeAllocationPanel( showCalculationPanel, showRCDtreeFilePanel );
		JComponent settPanel = makeSettingsPanel();

		String textAllocationTab;
		if( myFlagGeneratorInterface ) textAllocationTab = "Compile Settings";
		else textAllocationTab = "Calculation";
		myTabbedPane.add( myAllocationPanel, textAllocationTab );
		myTabbedPane.add( settPanel, "Dtree Settings" );
		if( FLAG_DEBUG ) myTabbedPane.add( makeDebugPanel(), "DEBUG" );

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

	/** @since 20030127 */
	public void setContainer( Container parent )
	{
		myContainerAbstraction.setContainer( parent );
	}

	/** @since 20030122 */
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

	/** @since 20030122 */
	protected void doParentClosing()
	{
		if( myFlagRecursiveConditioningRunning )
		{
			RCPanel.this.resumeRecursiveConditioning();
			RCPanel.this.stopRuns();
		}
		//System.out.println( "internalFrameClosing" );
	}

	protected String myBaseFileName;

	/** @since 20030122 */
	public void setBaseFileName( String str )
	{
		myBaseFileName = str;
	}

	protected JLabel myLabelOptimalMemory = null;
	protected JLabel myLabelUserMemory = null;
	protected JSlider mySliderUserMemory = null;
	protected JCheckBox myCBuseKB;
	protected JLabel myLabelEstimatedTime = null;
	protected JLabel myLabelPercentComplete = null;
	protected JLabel myLabelProbability = null;
	protected JButton myButtonRun = null;
	protected JButton myButtonCancel = null;
	protected JButton myButtonPause = null;
	protected JButton myButtonResume = null;

	public static final int INT_MIN_SLIDER = 0;
	public static final int INT_MAX_SLIDER = 100;

	/** @since 20030124 */
	protected void resetSlider()
	{
		//System.out.println( "RCPanel.resetSlider()" );
		myFlagListenEvents = false;
		mySliderUserMemory.setValue( (int)((double)INT_MAX_SLIDER * mySettings.getUserMemoryProportion()) );
		myFlagListenEvents = true;
	}

	/** @since 20030501 */
	protected void resetDtreeMethod()
	{
		Dtree dtree = mySettings.getDtree();
		if( dtree != null && dtree.myCreationMethod != null )
		{
			//myFlagListenEvents = false;
			myComboBoxMethod.setSelectedItem( dtree.myCreationMethod );
			//myFlagListenEvents = true;
		}
	}

	protected boolean myFlagListenEvents = true;

	protected boolean mySettingsMightHaveChanged = false;
	protected Color myColorBackgroundNormal = null;
	protected Color myColorBackgroundPausedThread = null;

	protected ActionListener myGeneralActionListener = new GeneralActionListener();

	/** @since 20030124 */
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
			else if( src == myCBuseKB && mySettings != null ) mySettings.setUseKB( myCBuseKB.isSelected() );
			else if( src == myButtonOpenDtree ) doOpenDtree();
			else if( src == myButtonSaveDtree ) doSaveDtree();
			else if( src == myBtnBrowse ) doBrowse();
			else if( src == myButtonGenerateDtree )
			{
				if( mySettings != null )
				{
					Dtree oldDtree = mySettings.getDtree();
					Stats stats = null;
					if( oldDtree != null ) stats = mySettings.getBundle().getStats( true );
					Dtree newDtree;
					Dtree temp = mySettings.generateDtree( myBeliefNetwork );

					if( temp == null ) showErrorMessage( "Failed to generate Dtree.", "Dtree Generation Error" );
					else
					{
						if( myCheckBoxKeepBest.isSelected() ) newDtree = mySettings.selectBetterDtree( temp, oldDtree, stats );
						else newDtree = temp;

						if( newDtree != oldDtree )
						{
							mySettings.setDtree( newDtree );
							if( newDtree != null )
							{
								updateDtreeStatsDisplay();
								myRCDtree = null;
								updateOptimalMemoryNumber();
								resetSlider();
							}
						}
					}

					mySettingsMightHaveChanged = false;
				}
			}
			else if( src == myButtonSaveRCDgraph ) doSaveRC();
			else if( src == myButtonOpenRCDgraph ) doOpenRC();
			else if( src == myButtonVCG1 )
			{
				if( myRCDtree != null && myBeliefNetwork != null) {
					RCUtilitiesIO.writeToVCGFileBasic( myBaseFileName + "1.vcg", myRCDtree);
					JOptionPane.showMessageDialog( RCPanel.this, "VCG1 written.");
				}
			}
			else if( src == myButtonVCG2 )
			{
				if( myRCDtree != null && myBeliefNetwork != null && mySettings.getDtree() != null) {
					RCUtilitiesIO.writeToVCGFileDetailed( myBaseFileName + "2.vcg", myRCDtree);
					JOptionPane.showMessageDialog( RCPanel.this, "VCG2 written.");
				}
			}
			else if( myFlagListenEvents )
			{
				if( src == myComboBoxElimAlgo )
				{
					changed = true;
					if( mySettings != null ) mySettings.setElimAlgo( (EliminationHeuristic) myComboBoxElimAlgo.getSelectedItem() );
				}
				else if( src == myComboBoxHMETISAlgo )
				{
					changed = true;
					if( mySettings != null ) mySettings.setHMeTiSAlgo( (MethodHmetis.Algorithm) myComboBoxHMETISAlgo.getSelectedItem() );
				}
				else if( src == myWNFNumberDtrees )
				{
					changed = true;
					if( mySettings != null ) mySettings.setNumDtrees( myWNFNumberDtrees.getValue() );
				}
				else if( src == myWNFNumberPartitions )
				{
					changed = true;
					if( mySettings != null ) mySettings.setNumPartitions( myWNFNumberPartitions.getValue() );
				}
				else if( src == myComboBoxBalanceFactors )
				{
					changed = true;
					if( mySettings != null ) mySettings.setBalanceFactor( myComboBoxBalanceFactors.getSelectedItem() );
				}
				else if( src == myCbHuginLogStyle )
				{
					changed = true;
					if( mySettings != null ) mySettings.setDtreeStyle( (MethodHuginLog.Style) myCbHuginLogStyle.getSelectedItem() );
				}
				else if( src == myTfHuginLogFile )
				{
					changed = true;
					if( mySettings != null ) mySettings.setTentativeHuginLogFilePath( myTfHuginLogFile.getText() );
				}
				else if( src == myComboBoxCacheSchemes )
				{
					changed = true;
					if( mySettings != null ) mySettings.setCachingScheme( (CachingScheme) myComboBoxCacheSchemes.getSelectedItem() );
				}
			}

			if( changed )
			{
				mySettingsMightHaveChanged = true;
				//if( myFlagGeneratorInterface ) myRCEngineGenerator.fireSettingsChanged();
			}
		}
	}

	/** @since 20030124 */
	protected class ToolActionListener implements ActionListener
	{
		public void actionPerformed( ActionEvent event )
		{
			Object src = event.getSource();

			if( src == myCheckBoxEstimatedTime )
			{
				toggleEstimatedTime();
			}
			else if( src == myButtonAllocateMemory )
			{
				myButtonAllocateMemory.setEnabled( false );
				setWaitCursor();
				myRunningCacheAllocationThread = mySettings.allocRCDtreeInThread( myBeliefNetwork );
				setAllocationInterferingWidgetsEnabled();
				//myButtonStopMemoryAllocation.setEnabled( true );
			}
			else if( src == myButtonStopMemoryAllocation )
			{
				myFlagStopCacheAllocation = true;
				myButtonStopMemoryAllocation.setEnabled( false );
			}
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
			else if( src == myMPEPanelActionSource )
			{
				clobberMPETab();
			}
			else if( src == myButtonShowMPE )
			{
				selectMPETab();
			}
			else if( src == myButtonRun )
			{
				safeRunRecursiveConditioning();
			}
			else if( src == myButtonCancel )
			{
				confirmCancelRecursiveConditioning();
			}
			else if( src == myButtonPause )
			{
				pauseRecursiveConditioning();
			}
			else if( src == myButtonResume )
			{
				resumeRecursiveConditioning();
			}
			else myGeneralActionListener.actionPerformed( event );
		}
	}

	/** @since 20020809 */
	protected void safeRunRecursiveConditioning()
	{
		synchronized( mySynchronization )
		{

		myButtonRun.setEnabled( false );
		if( !myFlagRecursiveConditioningRunning )
		{
			if( myRadioPRE.isSelected() ) runRecursiveConditioning( false );
			else if( myRadioMPE.isSelected() ) runRecursiveConditioning( true );
		}

		}
	}

	protected Thread myRunningCacheAllocationThread = null;

	/*Will call this function during the creation of a dtree whenever the new best cost
	 * changes by more than rcCreateUpdateThreshold() from the last call to update.
	 */
	public void rcCreateUpdate( double bestCost )
	{
		//System.out.println( "RCPanel.rcCreateUpdate("+bestCost+")" );
		Computation comp = mySettings.getBundle().getPe();
		comp.setExpectedNumberOfRCCalls( bestCost );
		updateEstimatedMinutesDisplay( comp );
	}

	public static double DOUBLE_CACHE_ALLOCATE_UPDATE_THRESHOLD = (double)100;
	/*The "frequency" which the listener wants to hear about updates.*/
	public double rcCreateUpdateThreshold()
	{
		return DOUBLE_CACHE_ALLOCATE_UPDATE_THRESHOLD;
	}

	protected boolean myFlagStopCacheAllocation = false;
	/*Return true to "stop" current search and return best result so far.  This function will tell the
	* search algorithm to stop, but the user should wait for calls to the rcCreateDone functions to
	* actually know when the search finishes.
	*/
	public boolean rcCreateStopRequested()
	{
		return myFlagStopCacheAllocation;
	}

	protected boolean myFlagLastCacheAllocationWasOptimal = false;

	/*Will be called upon "completion of search" or upon "stop requested"
	 * (optimal=true means completion, optimal = false means stopped) when
	 * the RCDtree constructor is finishing.
	 */
	public void rcCreateDone( double bestCost, boolean optimal )
	{
		synchronized( mySynchronization )
		{

		myFlagLastCacheAllocationWasOptimal = optimal;

		}
	}

	/*Will be called by the static method which creates a RC in a separate thread.
	 * The standard constructor will not call this method.
	 */
	public void rcCreateDone( RC rc )
	{
		synchronized( mySynchronization )
		{

		//System.out.println( "RCPanel.rcCreateDone( "+rc+" )" );

		myFlagStopCacheAllocation = false;
		myButtonAllocateMemory.setEnabled( true );
		myButtonStopMemoryAllocation.setEnabled( false );

		setDefaultCursor();

		if( rc == null ) showErrorMessage( "Failed to allocate memory.", "Error" );
		else
		{
			//updateEstimatedMinutesDisplay_Pe( rc );
			if( myFlagGeneratorInterface ) mySettings.setRC( null );
			else
			{
				myRCDtree = (RCDtree)rc;
				myRCDtree.outputConsole = myNetworkInternalFrame.console;
			}
			mySettings.refresh( rc );

			Computation comp = mySettings.getBundle().getPe();
			updateEstimatedMinutesDisplay( comp );

			updateUserMemoryDisplay( comp );
			resetSlider();
		}

		resetRCWidgets();

		myRunningCacheAllocationThread = null;
		setAllocationInterferingWidgetsEnabled();

		}
	}

	/** @since 20030603 */
	public void rcConstructionDone(){
		synchronized( mySynchronization )
		{

		myButtonStopMemoryAllocation.setEnabled( true );

		}
	}

	/** @since 20030603 */
	public void rcCreateError( String msg )
	{
		synchronized( mySynchronization )
		{

		myFlagStopCacheAllocation = false;
		myButtonAllocateMemory.setEnabled( true );
		myButtonStopMemoryAllocation.setEnabled( false );

		setDefaultCursor();

		System.err.println( msg );
		showErrorMessage( "Failed to allocate memory.\n" + msg, "Error" );

		resetRCWidgets();

		myRunningCacheAllocationThread = null;
		setAllocationInterferingWidgetsEnabled();

		}
	}

	protected JComboBox myComboBoxCacheSchemes = null;

	protected RCDtree myRCDtree = null;
	//protected Dtree myDtree = null;
	protected Thread myRecursiveConditioningThread = null;
	protected boolean myFlagRecursiveConditioningRunning = false;

	protected void pauseRecursiveConditioning()
	{
		synchronized( mySynchronization )
		{

		if( myRCDtree == null ) System.err.println( "RCPanel.pauseRecursiveConditioning() called with null RCDtree" );
		else
		{
			//myPanelResults.setBackground( myColorBackgroundPausedThread );
			alertPause( true );
			myButtonRun.setEnabled( false );
			myButtonPause.setEnabled( false );
			myButtonResume.setEnabled( true );
			myButtonCancel.setEnabled( true );
			myRCDtree.pauseRecCondAsThread();
		}

		}
	}

	public static final String STR_TITLE_NORMAL = "Recursive Conditioning";
	public static final String STR_TITLE_PAUSED = "Recursive Conditioning - PAUSED";
	public static final String STR_TITLE_GENERATOR = "Compile Settings - Recursive Conditioning";

	protected void alertPause( boolean alert )
	{
		Color background = null;
		String title = null;
		if( alert )
		{
			background = myColorBackgroundPausedThread;
			title = STR_TITLE_PAUSED;
		}
		else
		{
			background = myColorBackgroundNormal;
			title = STR_TITLE_NORMAL;
		}
		setResultsPanelColor( background );
		myContainerAbstraction.setTitle( title );
	}

	protected void resumeRecursiveConditioning()
	{
		synchronized( mySynchronization )
		{

		if( myRCDtree == null )  System.err.println( "RCPanel.resumeRecursiveConditioning() called with null RCDtree" );
		else
		{
			alertPause( false );
			myButtonRun.setEnabled( false );
			myButtonPause.setEnabled( true );
			myButtonResume.setEnabled( false );
			myButtonCancel.setEnabled( false );
			myRCDtree.resumeRecCondAsThread();
		}

		}
	}

	/** @since 20020809 */
	protected void confirmCancelRecursiveConditioning()
	{
		int result = JOptionPane.showConfirmDialog( this, "Are you sure you want to\ncancel the current recursive conditioning\ncalculation?", "Confirm Cancel", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
		if( result == JOptionPane.OK_OPTION ) cancelRecursiveConditioning();
	}

	protected void cancelRecursiveConditioning()
	{
		synchronized( mySynchronization )
		{

		if( myRCDtree == null )  System.err.println( "RCPanel.cancelRecursiveConditioning() called with null RCDtree" );

		alertPause( false );
		myButtonRun.setEnabled( true );
		myButtonPause.setEnabled( false );
		myButtonResume.setEnabled( false );
		myButtonCancel.setEnabled( false );

		resumeRecursiveConditioning();
		stopRuns();

		JOptionPane.showMessageDialog( this, "Recursive conditioning computation cancelled.", "Computation cancelled.", JOptionPane.WARNING_MESSAGE );

		}
	}

	protected Thread runRecursiveConditioning( final boolean MPE )
	{
		synchronized( mySynchronization )
		{

		stopRuns();
		forceCompletePercentComplete( STR_ZERO_PERCENT );
		myFlagRecursiveConditioningRunning = true;

		Thread ret = new Thread(){
			public void run()
			{
				if( myRunningCacheAllocationThread != null )
				{
					if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Before running rc, joining on existing cache allocation thread." );
					myButtonStopMemoryAllocation.doClick();
					try{
						myRunningCacheAllocationThread.join();
						myRunningCacheAllocationThread = null;
					}catch( InterruptedException e ){
						System.err.println( "Thread interrupted while joining on cache allocation thread." );
					}
					if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "DONE joining on existing cache allocation thread." );
				}

				if( mySettings.getDtree() == null || myRCDtree == null )
				{
					try{
						Thread tojoin = mySettings.allocRCDtreeInThread( myBeliefNetwork );
						if( tojoin != null ) tojoin.join();
					}catch( InterruptedException e ){
						System.err.println( "Thread interrupted while joining on cache allocation thread." );
					}
				}

				if( mySettings.getDtree() != null && myRCDtree != null )
				{
					//TODO: Can be drastically sped up if it had more info
					//  (e.g. could get which evidence changed)
					myRCDtree.resetEvidence();

					Map evid = myBeliefNetwork.getEvidenceController().evidence();
					for( Iterator itr = evid.keySet().iterator(); itr.hasNext();) {
						Object var = itr.next();
						Object val = evid.get( var);
						myRCDtree.observe((FiniteVariable)var, val);
					}

					alertPause( false );
					myProgressCount = 0;
					//myRCDtree.addProgressEventListener( RCPanel.this );

					setCalculationInterferingWidgetsEnabled();
					setResultProbabilityEnabled( false );
					myButtonPause.setEnabled( true );
					myButtonResume.setEnabled( false );
					myButtonCancel.setEnabled( false );

					//do recursive conditioning in another thread since it may take some time
					if( MPE )
					{
						clobberMPETab();
						//myRecursiveConditioningThread =
						myRCDtree.recCond_MPEAsThread( RCPanel.this );
					}
					else /*myRecursiveConditioningThread = */ myRCDtree.recCond_PeAsThread( RCPanel.this );

					RCPanel.this.new Poll().run();
				}
				else{
					showErrorMessage( "Failed to run recursive conditioning: failed to create Dtree/RCDtree.", "Error" );
					myButtonRun.setEnabled( true );
					myFlagRecursiveConditioningRunning = false;
				}
			}
		};
		ret.start();
		return ret;

		}
	}

	/** @since 20030917 */
	public class Poll extends Thread
	{
		public void run()
		{
			try{
				while( myFlagRecursiveConditioningRunning )
				{
					Thread.sleep( RCProgressMonitor.LONG_POLL_PERIOD );
					progressMade( myRCDtree.counters.totalCalls() );
				}
			}catch( InterruptedException e ){
				System.err.println( "Warning: RCPanel.Poll.run() interrupted by " + e );
			}
			//System.out.println( "RCPanel.Poll.run() returning" );
		}
	}

	protected DecimalFormat myPercentCompleteFormat = new DecimalFormat( "###%" );
	protected long myProgressCount = (long)0;

	public void progressMade( long progressCount )
	{
		synchronized( mySynchronization )
		{
			myProgressCount = progressCount;
			if( myFlagRecursiveConditioningRunning )
			{
				//lblPercentCompleteCaption
				double expectedCalls = mySettings.getBundle().getPe().getExpectedNumberOfRCCalls();
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
		boolean flagNotRunning = myRunningCacheAllocationThread == null;

		mySliderUserMemory.setEnabled( flagNotRunning );
		myCBuseKB.setEnabled( flagNotRunning );
		myButtonRun.setEnabled( flagNotRunning );
		myRadioPRE.setEnabled( flagNotRunning );
		myRadioMPE.setEnabled( flagNotRunning );
		myButtonSaveRCDgraph.setEnabled( flagNotRunning && ( (myFlagGeneratorInterface && getRCDgraph() != null) || myRCDtree != null ) );
		myButtonOpenRCDgraph.setEnabled( flagNotRunning );
		for( int i = 0; i < myTabbedPane.getTabCount(); i++ ) myTabbedPane.setEnabledAt( i, flagNotRunning );
	}

	protected void setCalculationInterferingWidgetsEnabled()
	{
		//if( !myFlagRecursiveConditioningRunning )
		//{
			//System.out.println( "CCCCCCCCCCCCCCCC" );
			//new Throwable().printStackTrace();
		//}
		if( myButtonAllocateMemory != null && myRunningCacheAllocationThread == null ) myButtonAllocateMemory.setEnabled( !myFlagRecursiveConditioningRunning );
		setPercentCompleteEnabled( myFlagRecursiveConditioningRunning );
		mySliderUserMemory.setEnabled( !myFlagRecursiveConditioningRunning );
		myCBuseKB.setEnabled( !myFlagRecursiveConditioningRunning );
		myButtonRun.setEnabled( !myFlagRecursiveConditioningRunning );
		myRadioPRE.setEnabled( !myFlagRecursiveConditioningRunning );
		myRadioMPE.setEnabled( !myFlagRecursiveConditioningRunning );
		myButtonSaveRCDgraph.setEnabled( !myFlagRecursiveConditioningRunning && ( (myFlagGeneratorInterface && getRCDgraph() != null) || myRCDtree != null ) );
		myButtonOpenRCDgraph.setEnabled( !myFlagRecursiveConditioningRunning );
		for( int i = 0; i < myTabbedPane.getTabCount(); i++ ) myTabbedPane.setEnabledAt( i, !myFlagRecursiveConditioningRunning );
	}

	/**
		For interface RCDtree.RecCondThreadListener
		@author Keith Cascio
		@since 20020724 */
	public void rcFinished( double result, long time_ms )
	{
		synchronized( mySynchronization )
		{

		myFlagRecursiveConditioningRunning = false;
		//myRCDtree.removeProgressEventListener( this );
		myRecursiveConditioningThread = null;

		if( result < (double)0 ) showErrorMessage( "Recursive conditioning out of memory.", "Error" );
		else
		{
			forceCompletePercentComplete( STR_ONEHUNDRED_PERCENT );

			double millisPerRCCall = (double)time_ms/(double)myProgressCount;
			if( !mySettings.getUseKB() && time_ms > (long)1000 )
			{
				mySettings.setMillisPerRCCall( millisPerRCCall );
			}


			if( myRCDtree != null && myRCDtree.outputConsole != null) {
				myRCDtree.outputConsole.println( "\n\nFinished running RC");
				myRCDtree.outputConsole.println( " Time (cpu time if jvm_profiler): " + time_ms + " ms" );
				myRCDtree.outputConsole.println( " Number of RC calls: " + myProgressCount );
			}


			if( FLAG_DEBUG_VERBOSE )
			{
				java.io.PrintStream stream = System.out;
				stream.println("\n\n==============");
				stream.println( "Time (cpu time if jvm_profiler): " + time_ms + " ms" );
				stream.println( "Number of RC calls (event driven counter): " + myProgressCount );
				myStatisticsFormat.setMinimumFractionDigits( 18 );
				stream.println( "ms per RC call: " + myStatisticsFormat.format( millisPerRCCall ) );
				stream.println( "calls/sec: " + (1.0/millisPerRCCall*1000));
				stream.println( "New Minutes Per RC Call: " + myStatisticsFormat.format( Settings.getMinutesPerRCCall() ) );
				printStatistics( myRCDtree.getStatistics() );
			}

			if( myFlagDtreesInvalid )
			{
				mySettings.setDtree( null );
				myRCDtree = null;
				myFlagDtreesInvalid = false;
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

		}
	}

	/**
		interface RC.RecCondThreadListener
		@author Keith Cascio
		@since 20030610 */
	public void rcComputationError( String msg )
	{
		synchronized( mySynchronization )
		{

		myFlagRecursiveConditioningRunning = false;
		//myRCDtree.removeProgressEventListener( this );
		myRecursiveConditioningThread = null;

		showErrorMessage( msg, "RC Computation Error" );

		myButtonRun.setEnabled( true );
		myButtonPause.setEnabled( false );
		myButtonResume.setEnabled( false );
		myButtonCancel.setEnabled( false );
		setCalculationInterferingWidgetsEnabled();

		}
	}

	protected NumberFormat myStatisticsFormat = NumberFormat.getNumberInstance();

	protected void printStatistics( String res )
	{
		Computation comp = mySettings.getBundle().getPe();
		myStatisticsFormat.setMaximumFractionDigits( 0 );
		java.io.PrintStream stream = System.out;
		stream.println("==============");
		stream.println("Expected RC-Calls: " + myStatisticsFormat.format( comp.getExpectedNumberOfRCCalls() ) );
		stream.println("Actual:");
		stream.println( res);
		stream.println("Created " + myStatisticsFormat.format( myRCDtree.statsPe().numCacheEntries()) +
		       " cache entries, max is " + myStatisticsFormat.format( comp.getNumMaxCacheEntries() ) );
		stream.println("==============");
	}

	/** interface RCDtree.RecCondThreadListener
		@since 20020724 */
	public void rcFinishedMPE( double result, Map instantiation, long time_ms )
	{
		synchronized( mySynchronization )
		{

		myFlagRecursiveConditioningRunning = false;
		//myRCDtree.removeProgressEventListener( this );
		myRecursiveConditioningThread = null;

		if( result < (double)0 )
		{
			showErrorMessage( "Recursive conditioning MPE out of memory.", "Error" );
			setCalculationInterferingWidgetsEnabled();
		}
		else
		{
			forceCompletePercentComplete( STR_ONEHUNDRED_PERCENT );

			if( myFlagDtreesInvalid )
			{
				mySettings.setDtree( null );
				myRCDtree = null;
				myFlagDtreesInvalid = false;
			}

			setCalculationInterferingWidgetsEnabled();
			myLabelProbability.setText( theProbabilityFormat.format( result ) );

			MPEPanel newPanel = new MPEPanel( result, instantiation, myBeliefNetwork );
			myMPEPanelActionSource = newPanel.addActionListener( myActionListener );
			newPanel.setClipBoard( myNetworkInternalFrame.getParentFrame().getInstantiationClipBoard(), myBeliefNetwork.getEvidenceController() );
			myTabbedPane.add( newPanel, STR_TAB_TITLE_MPE );

			setResultProbabilityEnabled( true );

			updateElapsedTimeDisplay( (double)time_ms );

			if( FLAG_DEBUG_VERBOSE ) printStatistics( myRCDtree.getStatistics() );
		}

		myButtonRun.setEnabled( true );
		myButtonPause.setEnabled( false );
		myButtonResume.setEnabled( false );
		myButtonCancel.setEnabled( false );

		}
	}

	protected static final String STR_TAB_TITLE_MPE = "MPE";
	protected Object myMPEPanelActionSource = null;

	/** @since 20020808 */
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

	/** @since 20020808 */
	protected void selectMPETab()
	{
		int indexMPETab = myTabbedPane.indexOfTab( STR_TAB_TITLE_MPE );
		if( indexMPETab != (int)-1 ) myTabbedPane.setSelectedIndex( indexMPETab );
	}

	private void stopRuns()
	{
		synchronized( mySynchronization )
		{

		//stop any previously running threads
		if( /*myRecursiveConditioningThread != null && */ myRCDtree != null ) {
			//myRCDtree.stopAndWaitRecCondAsThread( myRecursiveConditioningThread );
			myRCDtree.stopAndWaitRecCondAsThread();
			myRecursiveConditioningThread = null;
			//myRCDtree.removeProgressEventListener( this );
		}

		myFlagRecursiveConditioningRunning = false;

		if( myButtonRun != null )
		{
			myButtonRun.setEnabled( true );
			myButtonPause.setEnabled( false );
			myButtonResume.setEnabled( false );
			myButtonCancel.setEnabled( false );

			setCalculationInterferingWidgetsEnabled();
		}

		}
	}

	protected void setWaitCursor()
	{
		setCursor( UI.CURSOR_WAIT );
		//if( hnInternalFrame != null ) hnInternalFrame.getParentFrame().setWaitCursor();
	}

	protected void setDefaultCursor()
	{
		setCursor( UI.CURSOR_DEFAULT );
		//if( hnInternalFrame != null ) hnInternalFrame.getParentFrame().setDefaultCursor();
	}

	/** @since 20020807 */
	protected void generateDtreeAndUpdateDisplay()
	{
		//System.out.println( "generateDtreeAndUpdateDisplay(), isVisisble() = " + isVisible() );

		if( mySettings != null )
		{
			if( mySettings.validateRC( myBeliefNetwork ) )
			{
				updateDtreeStatsDisplay();
				updateOptimalMemoryNumber();
				mySettingsMightHaveChanged = false;
			}
			else showErrorMessage( "Failed to generate new Dtree based on settings!", "Error" );
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

	/** @since 20020919 */
	public void setVisibleSafe( boolean to, boolean from )
	{
		synchronized( mySynchronization )
		{

		//System.out.println( "RCPanel.setVisibleSafe( "+to+", "+from+" )" );

		if( to && !from )
		{ //if it has been hidden, it may not be up to date
			myResizeAdapter.componentResized( new ComponentEvent( this, 0 ) );
			generateDtreeAndUpdateDisplay();
		}
		else if( from )
		{ //if hiding, stop recCond from running
			//hideMPE();
			stopRuns();
		}

		setVisible( to );

		}
	}

	protected boolean generateTrees()
	{
		synchronized( mySynchronization )
		{

		String errmsg = null;

		if( myFlagRecursiveConditioningRunning ) errmsg = "Error: call to RCPanel.generateTrees() while a calculation was running.";
		else if( myBeliefNetwork == null ) errmsg = "RCPanel.generateTrees(), null BeliefNetwork";
		else if( myRunningCacheAllocationThread != null ) myRunningCacheAllocationThread = mySettings.allocRCDtreeInThread( myBeliefNetwork );

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
			if( !myFlagRecursiveConditioningRunning )
			{
				double newMemoryProportion = (double)mySliderUserMemory.getValue()/(double)INT_MAX_SLIDER;

				if( mySettings.setUserMemoryProportion( newMemoryProportion ) )
				{
					myRCDtree = null;
					updateUserMemoryDisplay( mySettings.getBundle().getPe() );
				}

				if( !mySliderUserMemory.getValueIsAdjusting() && myCheckBoxEstimatedTime.isSelected() )
				{
					if( myRunningCacheAllocationThread != null && myRCDtree == null ) myRunningCacheAllocationThread = mySettings.allocRCDtreeInThread( myBeliefNetwork );
				}
			}
		}
		else if( src == myTabbedPane )
		{
			if( myTabbedPane.getSelectedComponent() == myAllocationPanel && mySettingsMightHaveChanged )
			//if( myTabbedPane.getSelectedIndex() == (int)0 && mySettingsMightHaveChanged )
			{
				if( !myFlagRecursiveConditioningRunning )
				{
					int result = JOptionPane.showConfirmDialog( this, "You have changed one of the Dtree settings.\nWould you like to generate a new Dtree now?", "Generate new Dtree?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );
					if( result == JOptionPane.OK_OPTION ) generateDtreeAndUpdateDisplay();
					mySettingsMightHaveChanged = false;
				}
			}
		}
	}

	protected void updateEstimatedMinutesDisplay( Computation comp )
	{
		myLabelTimeCaption.setText( STR_ESTIMATED_TIME_CAPTION );

		String[] newVals = comp.updateEstimatedMinutesDisplay();
		myLabelEstimatedTime.setText( newVals[0] );
		lblEstimatedTimeUnits.setText( newVals[1] );
	}

	/** @since 20020813 */
	protected void updateElapsedTimeDisplay( double milliseconds )
	{
		String[] newVals = mySettings.updateElapsedTimeDisplay( milliseconds );

		myLabelTimeCaption.setText( STR_ELAPSED_TIME_CAPTION );
		myLabelEstimatedTime.setText( newVals[0] );
		lblEstimatedTimeUnits.setText( newVals[1] );
	}

	protected String myMemoryUnit = Settings.STR_KILOBYTE_UNIT;

	protected void updateOptimalMemoryNumber()
	{
		myLabelEstimatedTime.setText( "?" );

		//if( myRCDtree == null ) mySettings.updateOptimalMemoryNumber( myBeliefNetwork );
		//else mySettings.updateOptimalMemoryNumber( myRCDtree );

		updateMemoryDisplay();
	}

	protected void updateMemoryDisplay()
	{
		if( mySettings != null && mySettings.validateRC( myBeliefNetwork ) )
		{
			Computation comp = mySettings.getBundle().getPe();

			String[] newVals = mySettings.updateOptimalMemoryDisplay( comp );

			if( myMemoryUnit != newVals[1] )
			{
				myMemoryUnit = newVals[1];
				updateMemoryUnitsDisplay();
			}

			myLabelOptimalMemory.setText( newVals[0] );
			updateUserMemoryDisplay( comp );
		}
	}

	protected void updateUserMemoryDisplay( Computation comp )
	{
		myLabelUserMemory.setText( mySettings.updateUserMemoryDisplay( comp ) );
	}

	protected void updateMemoryUnitsDisplay()
	{
		myLabelOptimalMemoryUnits.setText( myMemoryUnit );
		myLabelUserMemoryUnits.setText( myMemoryUnit );
	}

	protected JCheckBox myCheckBoxEstimatedTime;
	protected JButton   myButtonAllocateMemory, myButtonStopMemoryAllocation;
	protected JPanel    myPanelMemoryAllocationButtons;
	protected JLabel    lblEstimatedTimeUnits;

	public static final String STR_ESTIMATED_TIME_DISABLED = "?";

	protected void toggleEstimatedTime()
	{
		synchronized( mySynchronization )
		{

		if( myCheckBoxEstimatedTime.isSelected() )
		{
			setEstimatedTimeEnabled( true );
			if( myRunningCacheAllocationThread != null && myRCDtree == null ) mySettings.allocRCDtreeInThread( myBeliefNetwork );
		}
		else setEstimatedTimeEnabled( false );

		}
	}

	protected void setEstimatedTimeEnabled( boolean enabled )
	{
		if( !enabled ) myLabelEstimatedTime.setText( STR_ESTIMATED_TIME_DISABLED );
		myLabelEstimatedTime.setEnabled( enabled );
		lblEstimatedTimeUnits.setEnabled( enabled );
	}

	/** @since 20021213 */
	protected void doOpenDtree()
	{
		JFileChooser chooser = getFileChooser( myDtreeFileFilter, myHuginLogFileFilter, null );
		if( chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION )
		{
			File fileSelected = chooser.getSelectedFile();
			Dtree newDtree = null;
			try{
				newDtree = mySettings.doOpenDtree( myBeliefNetwork, fileSelected );
			}catch( Exception e ){
				showErrorMessage( "Could not read dtree from " + fileSelected.getPath() + "\n" + e.getMessage(), "Dtree file error" );
			}
			if( newDtree != null ) mySettings.setDtree( newDtree );
		}
	}

	protected void doSaveDtree()
	{
		JFileChooser chooser = getFileChooser( myDtreeFileFilter, null, null );
		if( chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION )
		{
			File fileSelected = chooser.getSelectedFile();
			mySettings.doSaveDtree( fileSelected );
		}
	}

	/** @since 20030129 */
	protected void doOpenRC()
	{
		JFileChooser chooser = getFileChooser( myRCDtreeFileFilter, null, null );
		if( chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION )
		{
			File fileSelected = chooser.getSelectedFile();
			if( mySettings.doOpenRC( myBeliefNetwork, fileSelected ) )
			{
				resetSlider();
				resetDtreeMethod();
				RC newRC = mySettings.getRC();
				if( newRC == null ) System.err.println( "Warning: doOpenRC() called but no RC object recovered." );
				else
				{
					updateMemoryDisplay();
					//updateEstimatedMinutesDisplay_All( newRC );
					updateEstimatedMinutesDisplay( mySettings.getBundle().getAll() );
				}
				if( myLabelFilePath != null ) myLabelFilePath.setText( mySettings.getBundle().getRCFilePath() );
				resetRCWidgets();
			}
		}
	}

	/** @since 20030131 */
	protected RCDgraph getRCDgraph()
	{
		if( myNetworkInternalFrame != null )
		{
			InferenceEngine ie = myNetworkInternalFrame.getCanonicalInferenceEngine();
			if( ie instanceof RCInferenceEngine ) return ((RCInferenceEngine) ie).underlyingCompilation();
		}

		return null;
	}

	/** @since 20030129 */
	protected void doSaveRC()
	{
		RC rc = myRCDtree;

		if( rc == null ) rc = getRCDgraph();

		if( rc != null )
		{
			JFileChooser chooser = getFileChooser( myRCDtreeFileFilter, null, null );
			if( chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION )
			{
				File fileSelected = chooser.getSelectedFile();
				String networkName = null;
				if( myNetworkInternalFrame != null ) networkName = myNetworkInternalFrame.getFileNameSansPath();
				if( mySettings.doSaveRC( null, rc, mySettings.getBundle().getPe(), fileSelected, networkName ) )
				{
					if( myLabelFilePath != null ) myLabelFilePath.setText( mySettings.getBundle().getRCFilePath() );
					return;
				}
			}
		}

		System.err.println( "Warning: RCPanel.doSaveRC() failed" );
	}

	/** @since 20021213 */
	protected void showErrorMessage( String message, String title )
	{
		JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
	}

	protected javax.swing.filechooser.FileFilter myDtreeFileFilter = new InflibFileFilter( new String[]{ ".dtree" }, UI.STR_SAMIAM_ACRONYM + " Dtree Files (*.dtree)" );
	protected javax.swing.filechooser.FileFilter myHuginLogFileFilter = new InflibFileFilter( new String[]{ ".hlg" }, "Hugin Log Files (*.hlg)" );
	protected javax.swing.filechooser.FileFilter myRCDtreeFileFilter = new InflibFileFilter( new String[]{ ".rc" }, UI.STR_SAMIAM_ACRONYM + " RC Files (*.rc)" );
	protected RCAccessory myRCAccessory;
	protected boolean myFlagXMLAvailable = true;

	/** @since 20021213 */
	protected JFileChooser getFileChooser( javax.swing.filechooser.FileFilter filter1, javax.swing.filechooser.FileFilter filter2, javax.swing.filechooser.FileFilter filter3 )
	{
		JFileChooser chooser = new JFileChooser( myNetworkInternalFrame.getParentFrame().getSamiamPreferences().defaultDirectory );
		//JFileChooser chooser = new JFileChooser();

		if( myFlagXMLAvailable && myRCAccessory == null )
		{
			myFlagXMLAvailable = NetworkIO.xmlAvailable();
			if( myFlagXMLAvailable ) myRCAccessory = new RCAccessory();
		}

		if( myRCAccessory != null )
		{
			myRCAccessory.setFileFilter( filter1 );
			chooser.addPropertyChangeListener( myRCAccessory );
			chooser.setAccessory( myRCAccessory );
			myRCAccessory.clear();
		}

		chooser.setAcceptAllFileFilterUsed( false );

		//chooser.addChoosableFileFilter( myDtreeFileFilter );
		//if( addHuginLogFilter ) chooser.addChoosableFileFilter( myHuginLogFileFilter );

		if( filter1 != null ) chooser.addChoosableFileFilter( filter1 );
		if( filter2 != null ) chooser.addChoosableFileFilter( filter2 );
		if( filter3 != null ) chooser.addChoosableFileFilter( filter3 );

		return chooser;
	}

	protected JLabel myLabelOptimalMemoryUnits = null;
	protected JLabel myLabelUserMemoryUnits = null;
	protected JComponent myPanelResults = null;
	protected static final boolean FLAG_SHOW_CHECKBOX_ESTIMATED_TIME = false;

	protected JLabel myLabelTimeCaption = null;
	protected static String STR_ESTIMATED_TIME_CAPTION = "Estimated Pr(e) run time: ";
	protected static String STR_ELAPSED_TIME_CAPTION = (JVMProfiler.profilerRunning()) ? "Elapsed time (thread profile): " : "Elapsed time (system clock): ";

	protected JButton myButtonVCG1;
	protected JButton myButtonVCG2;

	protected JComponent makeAllocationPanel( boolean showCalculationPanel, boolean showRCDtreeFilePanel )
	{
		myLabelOptimalMemory = new JLabel( "?", JLabel.RIGHT );
		Dimension dim  = myLabelOptimalMemory.getPreferredSize();
		dim.width = 64;
		myLabelOptimalMemory.setPreferredSize( dim );

		if( FLAG_DEBUG_BORDERS ) myLabelOptimalMemory.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		JLabel lblMemoryCaption = new JLabel( "Memory required for optimal Pr(e) running time: " );
		if( FLAG_DEBUG_BORDERS ) lblMemoryCaption.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		JLabel lblMemoryQuestion = new JLabel( "How much memory would you like to use? " );
		if( FLAG_DEBUG_BORDERS ) lblMemoryQuestion.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		myLabelUserMemory = new JLabel( "?", JLabel.RIGHT );
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

		myCBuseKB = new JCheckBox( "take advantage of determinism" );
		myCBuseKB.addActionListener( myActionListener );

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

			String textAllocateMemory = "Allocate Memory";
			//if( myFlagGeneratorInterface ) textAllocateMemory = "Enter Query Mode (New RCDgraph)";
			//else textAllocateMemory = "Allocate Memory";
			myButtonAllocateMemory = new JButton( textAllocateMemory );
			myButtonAllocateMemory.addActionListener( myActionListener );
			myButtonStopMemoryAllocation = new JButton( "Stop" );
			myButtonStopMemoryAllocation.addActionListener( myActionListener );
			myButtonStopMemoryAllocation.setEnabled( false );

			myPanelMemoryAllocationButtons = new JPanel();
			myPanelMemoryAllocationButtons.add( myButtonAllocateMemory );
			myPanelMemoryAllocationButtons.add( myButtonStopMemoryAllocation );
		}

		myLabelTimeCaption = new JLabel( STR_ESTIMATED_TIME_CAPTION, JLabel.RIGHT );
		dim  = myLabelTimeCaption.getPreferredSize();
		dim.width = 190;
		myLabelTimeCaption.setPreferredSize( dim );

		myLabelEstimatedTime = new JLabel( STR_ESTIMATED_TIME_DISABLED, JLabel.RIGHT );
		if( FLAG_DEBUG_BORDERS ) myLabelEstimatedTime.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		lblEstimatedTimeUnits = new JLabel( Settings.STR_MINUTE_UNIT, JLabel.LEFT );
		if( FLAG_DEBUG_BORDERS ) lblEstimatedTimeUnits.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		setEstimatedTimeEnabled( true );

		dim  = lblEstimatedTimeUnits.getPreferredSize();
		dim.width = 75;
		lblEstimatedTimeUnits.setPreferredSize( dim );

		if( showCalculationPanel ) myPanelResults = makeResultsPanel();

		if( mySettings.validateRC( myBeliefNetwork ) ) updateMemoryDisplay();

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );

		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.ipadx = 0;
		c.ipady = 16;
		c.weightx = 0;

		c.gridwidth = 2;
		gridbag.setConstraints( lblMemoryCaption, c );
		ret.add( lblMemoryCaption );

		c.weightx = 1;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints( myLabelOptimalMemory, c );
		ret.add( myLabelOptimalMemory );

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myLabelOptimalMemoryUnits, c );
		ret.add( myLabelOptimalMemoryUnits );

		//c.gridy++;
		c.gridwidth = 2;
		gridbag.setConstraints( lblMemoryQuestion, c );
		ret.add( lblMemoryQuestion );

		c.weightx = 1;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints( myLabelUserMemory, c );
		ret.add( myLabelUserMemory );

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myLabelUserMemoryUnits, c );
		ret.add( myLabelUserMemoryUnits );

		//c.gridy++;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints( mySliderUserMemory, c );
		ret.add( mySliderUserMemory );

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		ret.add( myCBuseKB, c );

		//c.gridy++;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		if( FLAG_SHOW_CHECKBOX_ESTIMATED_TIME )
		{
			gridbag.setConstraints( myCheckBoxEstimatedTime, c );
			ret.add( myCheckBoxEstimatedTime );
		}
		else
		{
			c.ipady = (int)0;
			gridbag.setConstraints( myPanelMemoryAllocationButtons, c );
			ret.add( myPanelMemoryAllocationButtons );
			c.ipady = (int)16;
		}

		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints( myLabelTimeCaption, c );
		ret.add( myLabelTimeCaption );

		c.weightx = 1;
		gridbag.setConstraints( myLabelEstimatedTime, c );
		ret.add( myLabelEstimatedTime );

		c.weightx = 0.1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( lblEstimatedTimeUnits, c );
		ret.add( lblEstimatedTimeUnits );

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.ipady = 0;
		Component strut = Box.createVerticalStrut( 10 );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		if( showRCDtreeFilePanel )
		{
			JComponent myRCDtreeFilePanel = makeRCDTreeFilePanel();
			c.weightx = 1;
			c.ipadx = 32;
			gridbag.setConstraints( myRCDtreeFilePanel, c );
			ret.add( myRCDtreeFilePanel );

			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 0;
			c.ipady = 0;
			c.ipadx = 0;
			strut = Box.createVerticalStrut( 16 );
			gridbag.setConstraints( strut, c );
			ret.add( strut );
		}

		if( showCalculationPanel )
		{
			c.weightx = 1;
			gridbag.setConstraints( myPanelResults, c );
			ret.add( myPanelResults );
		}

		ret.setBorder( BorderFactory.createEmptyBorder( 8,8,8,8 ) );

		resetRCWidgets();

		return ret;
	}

	protected JLabel myLabelFilePath = null;
	protected JButton myButtonSaveRCDgraph = null;
	protected JButton myButtonOpenRCDgraph = null;

	/** @since 20030131 */
	protected void resetRCWidgets()
	{
		myButtonSaveRCDgraph.setEnabled( ( (myFlagGeneratorInterface && getRCDgraph() != null) || myRCDtree != null ) );
		if( mySettings != null )
		{
			String pathRC = mySettings.getBundle().getRCFilePath();
			if( pathRC != null ) myLabelFilePath.setText( pathRC );
		}
	}

	/** @since 20030128 */
	protected JComponent makeRCDTreeFilePanel()
	{
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel myRCDTreeFilePanel = new JPanel( gridbag );

		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.ipadx = 0;
		c.ipady = 0;
		c.weightx = 0;

		String textSaveRC;
		String textOpenRC;
		if( myFlagGeneratorInterface ){
			textSaveRC = "Save RCDgraph";
			textOpenRC = "Open RCDgraph";
		}
		else{
			textSaveRC = "Save RCDtree";
			textOpenRC = "Open RCDtree";
		}

		Insets insets = new Insets( 0, 10, 0, 10 );

		myButtonSaveRCDgraph = new JButton( textSaveRC );
		Font fontSmall = myButtonSaveRCDgraph.getFont().deriveFont( (float)11 );
		myButtonSaveRCDgraph.setFont( fontSmall );
		myButtonSaveRCDgraph.setMargin( insets );
		myButtonSaveRCDgraph.addActionListener( myActionListener );

		myButtonOpenRCDgraph = new JButton( textOpenRC );
		myButtonOpenRCDgraph.setFont( fontSmall );
		myButtonOpenRCDgraph.setMargin( insets );
		myButtonOpenRCDgraph.addActionListener( myActionListener );
		int gheight;
		if( FLAG_DEBUG )
		{
			myButtonVCG1 = new JButton( "VCG1");
			myButtonVCG1.setFont( fontSmall );
			myButtonVCG1.setMargin( insets );
			myButtonVCG1.addActionListener( myActionListener );

			myButtonVCG2 = new JButton( "VCG2");
			myButtonVCG2.setFont( fontSmall );
			myButtonVCG2.setMargin( insets );
			myButtonVCG2.addActionListener( myActionListener );
			gheight = (int)4;
		}
		else gheight = (int)2;

		JPanel pnlUsing = new JPanel();

		Component strut = Box.createHorizontalStrut( 16 );
		pnlUsing.add( strut );

		JLabel lblRCDTreeFileCaption = new JLabel( " using: " );
		pnlUsing.add( lblRCDTreeFileCaption );

		myLabelFilePath = new JLabel();
		pnlUsing.add( myLabelFilePath );

		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints( myButtonSaveRCDgraph, c );
		myRCDTreeFilePanel.add( myButtonSaveRCDgraph );

		c.gridheight = gheight;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 1;
		c.gridx = 1;
		c.gridy = 0;
		gridbag.setConstraints( pnlUsing, c );
		myRCDTreeFilePanel.add( pnlUsing );

		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints( myButtonOpenRCDgraph, c );
		myRCDTreeFilePanel.add( myButtonOpenRCDgraph );

		if( FLAG_DEBUG )
		{
			c.gridx = 0;
			c.gridy = 2;
			gridbag.setConstraints( myButtonVCG1, c );
			myRCDTreeFilePanel.add( myButtonVCG1 );
			c.gridx = 0;
			c.gridy = 3;
			gridbag.setConstraints( myButtonVCG2, c );
			myRCDTreeFilePanel.add( myButtonVCG2 );
		}

		return myRCDTreeFilePanel;
	}

	protected JLabel myLabelDtreeHeight = null;
	protected JLabel myLabelDtreeMaxCluster = null;
	protected JLabel myLabelDtreeMaxCutset = null;
	protected JLabel myLabelDtreeMaxContext = null;

	/** @since 20020822 */
	protected void updateDtreeStatsDisplay()
	{
		myLabelDtreeHeight.setText( String.valueOf( mySettings.getDtreeHeight() ) );
		myLabelDtreeMaxCluster.setText( String.valueOf( mySettings.getDtreeMaxCluster() ) );
		myLabelDtreeMaxCutset.setText( String.valueOf( mySettings.getDtreeMaxCutset() ) );
		myLabelDtreeMaxContext.setText( String.valueOf( mySettings.getDtreeMaxContext() ) );
	}

	/** @since 20020822 */
	protected JComponent makeDtreeStatsPanel()
	{
		//JLabel lblDtreeStatsCaption = new JLabel( "Dtree properties: height = " );
		JLabel lblDtreeStatsCaption = new JLabel( " height = " );
		JLabel lblClusterCaption = new JLabel( " max cluster = " );
		JLabel lblCutsetCaption = new JLabel( " cutset = " );
		JLabel lblContextCaption = new JLabel( " context = " );
		myLabelDtreeHeight = new JLabel( "?" );
		Dimension dim = myLabelDtreeHeight.getPreferredSize();
		dim.width = 16;
		myLabelDtreeHeight.setPreferredSize( dim );
		myLabelDtreeMaxCluster = new JLabel( "?" );
		myLabelDtreeMaxCluster.setPreferredSize( dim );
		myLabelDtreeMaxCutset = new JLabel( "?" );
		myLabelDtreeMaxCutset.setPreferredSize( dim );
		myLabelDtreeMaxContext = new JLabel( "?" );
		myLabelDtreeMaxContext.setPreferredSize( dim );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );

		c.gridwidth =(int)1;
		c.weightx = 0;
		gridbag.setConstraints( lblDtreeStatsCaption, c );
		ret.add( lblDtreeStatsCaption );

		c.weightx = 1;
		gridbag.setConstraints( myLabelDtreeHeight, c );
		ret.add( myLabelDtreeHeight );

		c.weightx = 0;
		gridbag.setConstraints( lblClusterCaption, c );
		ret.add( lblClusterCaption );

		c.weightx = 1;
		gridbag.setConstraints( myLabelDtreeMaxCluster, c );
		ret.add( myLabelDtreeMaxCluster );

		c.weightx = 0;
		gridbag.setConstraints( lblCutsetCaption, c );
		ret.add( lblCutsetCaption );

		c.weightx = 1;
		gridbag.setConstraints( myLabelDtreeMaxCutset, c );
		ret.add( myLabelDtreeMaxCutset );

		c.weightx = 0;
		gridbag.setConstraints( lblContextCaption, c );
		ret.add( lblContextCaption );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		gridbag.setConstraints( myLabelDtreeMaxContext, c );
		ret.add( myLabelDtreeMaxContext );

		Border b = null;
		if( FLAG_DEBUG_BORDERS ) b = BorderFactory.createLineBorder( Color.red, (int)1 );
		else b = BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Current Dtree Properties" );

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

	protected JComponent myPanelResultsButtons = null;
	protected JComponent myPanelPercentComplete = null;

	protected void setResultsPanelColor( Color c )
	{
		myPanelResults.setBackground( c );
		myPanelResultsButtons.setBackground( c );
		myRadioPRE.setBackground( c );
		myRadioMPE.setBackground( c );
		myPanelShowMPE.setBackground( c );
		//myPanelPercentComplete.setBackground( c );
	}

	protected JRadioButton myRadioPRE = null;
	protected JRadioButton myRadioMPE = null;
	protected JButton myButtonShowMPE = null;
	protected JPanel myPanelShowMPE = null;

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
		myLabelProbability = new JLabel( "?" );

		dim = myLabelProbability.getPreferredSize();
		dim.width += 128;
		myLabelProbability.setPreferredSize( dim );

		myLabelProbability.setFont( fontProbability );
		//if( FLAG_DEBUG_BORDERS )
			//myLabelProbability.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		setResultProbabilityEnabled( false );

		myButtonRun = new JButton( "Run" );
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
		myPanelResultsButtons.add( myButtonPause );
		myPanelResultsButtons.add( myButtonResume );
		if( FLAG_DEBUG_BORDERS ) myPanelResultsButtons.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );

		c.fill = GridBagConstraints.NONE;
		c.ipadx = 0;
		c.gridwidth = 1;
		c.weightx = 0.001;
		c.anchor = GridBagConstraints.EAST;
		Component strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		gridbag.setConstraints( myRadioPRE, c );
		ret.add( myRadioPRE );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		gridbag.setConstraints( lblPercentCompleteCaption, c );
		ret.add( lblPercentCompleteCaption );

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		//gridbag.setConstraints( myPanelPercentComplete, c );
		//ret.add( myPanelPercentComplete );
		gridbag.setConstraints( myLabelPercentComplete, c );
		ret.add( myLabelPercentComplete );

		c.anchor = GridBagConstraints.EAST;
		c.weightx = 0.001;
		c.fill = GridBagConstraints.NONE;
		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		gridbag.setConstraints( myPanelShowMPE, c );
		ret.add( myPanelShowMPE );

		c.gridwidth = GridBagConstraints.REMAINDER;
		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		c.fill = GridBagConstraints.NONE;
		c.ipadx = 0;
		c.gridwidth = 1;
		c.weightx = 0.001;
		c.anchor = GridBagConstraints.WEST;
		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		gridbag.setConstraints( myRadioMPE, c );
		ret.add( myRadioMPE );

		//c.gridwidth = GridBagConstraints.REMAINDER;
		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		gridbag.setConstraints( lblProbabilityCaption, c );
		ret.add( lblProbabilityCaption );

		c.anchor = GridBagConstraints.WEST;
		c.weightx = 10;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints( myLabelProbability, c );
		ret.add( myLabelProbability );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0.001;
		c.fill = GridBagConstraints.NONE;
		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		//c.gridy++;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0.001;
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints( vstrut, c );
		ret.add( vstrut );

		//c.gridy++;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.CENTER;
		gridbag.setConstraints( myPanelResultsButtons, c );
		ret.add( myPanelResultsButtons );

		ret.setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Results" ) );

		myColorBackgroundNormal = ret.getBackground();

		return ret;
	}

	/** @since 20020819 */
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
	protected JButton myButtonOpenDtree = null;
	protected JButton myButtonSaveDtree = null;

	protected static boolean FLAG_HMETIS_LOADED = Hmetis.loaded();

	protected JComponent makeSettingsPanel()
	{
		JLabel lblMethod = new JLabel( "Dtree Method: ", JLabel.RIGHT );
		Object[] arrayMethods = null;
		if( FLAG_HMETIS_LOADED )
		{
			myPanelHMETIS = makeHMETISPanel();
			//arrayMethods = Settings.ARRAY_THREE_METHODS;
		}
		//else arrayMethods = Settings.ARRAY_THREE_METHODS_NOT_LOADED;
		arrayMethods = CreationMethod.getArray();

		myComboBoxMethod = new JComboBox( arrayMethods );
		myComboBoxMethod.addActionListener( myActionListener );
		myPanelMethod = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		myPanelMethod.setPreferredSize( new Dimension( 375, 101 ) );
		if( FLAG_DEBUG_BORDERS ) myPanelMethod.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		myPanelEliminationOrder = makeEliminationOrderPanel();
		myPanelMethod.add( myPanelEliminationOrder );

		myPanelHuginLog = makeHuginLogPanel();

		myCheckBoxKeepBest = new JCheckBox( "Keep Best" );
		myCheckBoxKeepBest.setSelected( true );
		myButtonGenerateDtree = new JButton( "Generate" );
		myButtonGenerateDtree.addActionListener( myActionListener );

		//myButtonOpenDtree = new JButton( "Open" );
		//myButtonOpenDtree.addActionListener( myActionListener );

		//myButtonSaveDtree = new JButton( "Save" );
		//myButtonSaveDtree.addActionListener( myActionListener );

		JPanel pnlButtons = new JPanel();
		//pnlButtons.add( myButtonOpenDtree );
		//pnlButtons.add( myButtonSaveDtree );
		Component strut = Box.createHorizontalStrut( (int)64 );
		pnlButtons.add( strut );
		pnlButtons.add( myCheckBoxKeepBest );
		pnlButtons.add( myButtonGenerateDtree );

		JComponent dtreeStatsPanel = makeDtreeStatsPanel();

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );

		//c.anchor = GridBagConstraints.NORTHWEST;
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = 1;
		gridbag.setConstraints( lblMethod, c );
		ret.add( lblMethod );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints( myComboBoxMethod, c );
		ret.add( myComboBoxMethod );

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myPanelMethod, c );
		ret.add( myPanelMethod );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		strut = Box.createVerticalStrut( 16 );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		c.weightx = 1;
		gridbag.setConstraints( dtreeStatsPanel, c );
		ret.add( dtreeStatsPanel );

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		strut = Box.createVerticalStrut( 16 );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		gridbag.setConstraints( pnlButtons, c );
		ret.add( pnlButtons );

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
				myComboBoxMethod.setSelectedItem( mySettings.getDtreeMethod() );
				myFlagListenEvents = true;
			}
			return;
		}

		if( mySettings != null ) mySettings.setDtreeMethod( (CreationMethod) newObjMethod );

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
		JLabel lblAlgorithm = new JLabel( "Algorithm: " );
		myComboBoxElimAlgo = new JComboBox( EliminationHeuristic.ARRAY );
		myComboBoxElimAlgo.addActionListener( myActionListener );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );
		Component strut = null;

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		gridbag.setConstraints( lblAlgorithm, c );
		ret.add( lblAlgorithm );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myComboBoxElimAlgo, c );
		ret.add( myComboBoxElimAlgo );

		if( FLAG_DEBUG_BORDERS ) ret.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );

		return ret;
	}

	protected JComponent makeDebugPanel()
	{
		//ARRAY_CACHE_SCHEMES = new Object[6];
		//Object[] ARRAY_CACHE_SCHEMES = new Object[2];

		//int indexcounter = (int)0;
		//mySettings.ARRAY_CACHE_SCHEMES[indexcounter++] = mySettings.CACHE_SCHEME_UNIFORM = new CachingUniform();
		//mySettings.ARRAY_CACHE_SCHEMES[indexcounter++] = mySettings.CACHE_SCHEME_DFBnB;


		JLabel lblCacheScheme = new JLabel( "Cache Scheme: " );
		myComboBoxCacheSchemes = new JComboBox( Settings.ARRAY_CACHE_SCHEMES );

		if( mySettings != null ) myComboBoxCacheSchemes.setSelectedItem( mySettings.getCachingScheme() );

		myComboBoxCacheSchemes.addActionListener( myActionListener );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );
		Component strut = null;

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		gridbag.setConstraints( lblCacheScheme, c );
		ret.add( lblCacheScheme );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myComboBoxCacheSchemes, c );
		ret.add( myComboBoxCacheSchemes );

		if( FLAG_DEBUG_BORDERS ) ret.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );

		return ret;
	}

	/** @since 20030422 */
	protected void doBrowse()
	{
		if( myFileChooser == null )
		{
			if( myDefaultDirectory == null ) myDefaultDirectory = new File( "." );
			myFileChooser = new JFileChooser( myDefaultDirectory );

			hlgFilter = new InflibFileFilter( new String[]{ STR_EXTENSION_HLG }, "HUGIN Log v5.7 (*.hlg)" );
			myFileChooser.addChoosableFileFilter( hlgFilter );
		}

		if( myFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION )
		{
			File selected = myFileChooser.getSelectedFile();

			if( selected.exists() )
			{
				if( hlgFilter.accept( selected ) )
				{
					myFlagListenEvents = false;
					myTfHuginLogFile.setText( selected.getPath() );
					myFlagListenEvents = true;
					mySettings.setHuginLogFile( selected );
				}
				else showErrorMessage( selected.getPath() + " incorrect file type. Must have extension " + STR_EXTENSION_HLG );
			}
			else showErrorMessage( selected.getPath() + " does not exist." );
		}
	}


	/** @since 20030422 */
	public void showErrorMessage( String txt )
	{
		JOptionPane.showMessageDialog( this, txt, DtreeSettingsPanel.STR_GENERIC_TITLE, JOptionPane.ERROR_MESSAGE );
	}

	/** @since 20030422 */
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

	/** @since 20030422 */
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
		gridbag.setConstraints( lblAlgo, c );
		ret.add( lblAlgo );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myComboBoxHMETISAlgo, c );
		ret.add( myComboBoxHMETISAlgo );

		c.gridwidth = 1;
		gridbag.setConstraints( lblNumDt, c );
		ret.add( lblNumDt );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myWNFNumberDtrees, c );
		ret.add( myWNFNumberDtrees );

		c.gridwidth = 1;
		gridbag.setConstraints( lblNumPart, c );
		ret.add( lblNumPart );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myWNFNumberPartitions, c );
		ret.add( myWNFNumberPartitions );

		c.gridwidth = 1;
		gridbag.setConstraints( lblBal, c );
		ret.add( lblBal );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myComboBoxBalanceFactors, c );
		ret.add( myComboBoxBalanceFactors );

		if( FLAG_DEBUG_BORDERS ) ret.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );

		return ret;
	}

	/** @since 20020809 */
	protected boolean myFlagDtreesInvalid = false;

	public static boolean FLAG_DEBUG_LISTEN_CHANGES = false;

	/** interface EvidenceChangeListener
		@since 20030710 */
	public void warning( EvidenceChangeEvent ece ) {}

	/** @since 20020809 */
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

	/** @since 20021119 */
	protected void somethingChangedSansDiscard()
	{
		synchronized( mySynchronization )
		{

		if( myFlagRecursiveConditioningRunning )
		{
			if( FLAG_DEBUG_LISTEN_CHANGES ) Util.STREAM_VERBOSE.println( "\tmyFlagRecursiveConditioningRunning" );
			myFlagDtreesInvalid = true;
			cancelRecursiveConditioning();
		}
		setResultProbabilityEnabled( false );
		clobberMPETab();

		}
	}

	/** @since 20020809 */
	protected void somethingChanged()
	{
		synchronized( mySynchronization )
		{

		if( FLAG_DEBUG_LISTEN_CHANGES ) Util.STREAM_VERBOSE.println( "RCPanel.somethingChanged()" );
		if( myFlagRecursiveConditioningRunning )
		{
			if( FLAG_DEBUG_LISTEN_CHANGES ) Util.STREAM_VERBOSE.println( "\tmyFlagRecursiveConditioningRunning" );
			myFlagDtreesInvalid = true;
			cancelRecursiveConditioning();
		}
		else
		{
			mySettings.setDtree( null );
			myRCDtree = null;
		}
		setResultProbabilityEnabled( false );
		clobberMPETab();

		}
	}
}
