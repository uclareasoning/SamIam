package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.statusbar.StatusBar;
import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.actionsandmodes.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariable;

import edu.ucla.belief.*;
import edu.ucla.belief.sensitivity.*;
import edu.ucla.belief.inference.map.*;
import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.util.code.*;
import edu.ucla.util.AbstractStringifier;

import java.util.List;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.net.*;
import javax.swing.border.*;

public class MPEInternalFrame extends javax.swing.JInternalFrame implements EvidenceChangeListener, CPTChangeListener, RC.RecCondThreadListener
	//ActionListener
{
	public static final String STR_MSG_MPE = "calculating most probable explanation (mpe)...";
	public static final String STR_FILENAME_ICON = "MPE16.gif";
	public static final String STR_TITLE_RESULTS = "Instantiation", STR_TITLE_SENSITIVITY = "Sensitivity";
	public static final String STR_TAB_SENSITIVITY_TOOLTIP = "Parameter-by-parameter sensitivity values";
	public static final String STR_TAB_RESULTS_TOOLTIP = "Most probable explanation result instantiation";
	public static final String STR_BUTTON_SENSITIVITY_TEXT = "Sensitivity", STR_BUTTON_SENSITIVITY_TOOLTIP = "Compute the sensitivity of this MPE", STR_NAME_COLUMN_NEW_VALUE = "<html><nobr><font color=\"#660066\">To flip MPE";

	public static final int INT_TAB_INDEX_RESULTS = 0, INT_TAB_INDEX_SENSITIVITY = 1;

	public static final double DOUBLE_EPSILON_SOUNDNESS = (double) 0.000001;

	public MPEInternalFrame( NetworkInternalFrame fn )
	{
		// Call JInternalFrame's constructor
		super( "MPE Computation", true, true, true, true );

		hnInternalFrame = fn;
		if( hnInternalFrame != null ){
			hnInternalFrame.addEvidenceChangeListener( this );
			hnInternalFrame.addCPTChangeListener( this );
		}

		Icon iconFrame = MainToolBar.getIcon( STR_FILENAME_ICON );
		if( iconFrame != null ) setFrameIcon( iconFrame );

		MPEInternalFrame.this.init();

		return;
	}

	/** @since 20051102 */
	private void init(){
		Container content = getContentPane();
		content.setLayout( new BorderLayout() );

		JPanel myPanelButtons = null;
		content.add( myPanelButtons = new JPanel( new GridBagLayout() ), BorderLayout.SOUTH );
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelButtons.add( Box.createVerticalStrut(1), c );
		c.gridwidth = 1;
		//myPanelButtons.add( MPEPanel.configure( new JButton( myRunSensitivity ) ) );
		//myPanelButtons.add( MPEPanel.configure( new JButton( myActionBandit ) ) );
		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelButtons.add( MPEPanel.configure( new JButton( myActionClose ) ) );
		myPanelButtons.add( Box.createVerticalStrut(1), c );

		content.add( myPanelMain = new JPanel( new GridLayout(1,1) ), BorderLayout.CENTER );

		JMenuBar menuBar = new JMenuBar();
		MPEInternalFrame.this.setJMenuBar( menuBar );

		JMenu menuFile = new JMenu( "File" );
		menuBar.add( menuFile );
		menuFile.add( new JMenuItem( myActionClose ) );

		myMenuEdit = new JMenu( "Edit" );
		menuBar.add( myMenuEdit );

		myMenuTools = new JMenu( "Tools" );
		menuBar.add( myMenuTools );
		myMenuTools.add( new JMenuItem( myActionBandit ) );

		JMenu menuSensitivity=  new JMenu( "Sensitivity" );
		menuBar.add( menuSensitivity );
		menuSensitivity.add( new JMenuItem( myRunSensitivity ) );

	  //JMenu menuView = new JMenu( "View" );
	  //menuBar.add( menuView );
		JMenu menuView = menuSensitivity;
		menuView.add( new JCheckBoxMenuItem( myActionToggleTableDetails ) );

	  //JMenu menuDebug = new JMenu( "Debug" );
	  //menuBar.add( menuDebug );
		JMenu menuDebug = menuSensitivity;
		menuDebug.add( action_ADOPTCHANGE );
		menuDebug.add( action_EDITCPT );
		if( Util.DEBUG ){ menuDebug.add( action_TESTSOUNDNESS ); }
		menuDebug.addMenuListener(
			new MenuListener(){
				public void menuSelected(MenuEvent e){
					boolean enabled = (mySensitivitySuggestionTable != null) && (mySensitivitySuggestionTable.getCurrentlySelectedSuggestion() != null);
					enabled &= MPEInternalFrame.this.myFlagEnableEditActions;

					action_ADOPTCHANGE  .setEnabled( enabled );
					action_EDITCPT      .setEnabled( enabled );
					action_TESTSOUNDNESS.setEnabled( enabled );
				}
				public void menuDeselected(MenuEvent e){}
				public void menuCanceled(MenuEvent e){}
			}
		);
	}

	/**
		interface EvidenceChangeListener
		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ece ) {}

	/**
		interface EvidenceChangeListener
 		@since 20020528 */
	public void evidenceChanged( EvidenceChangeEvent ece ){
		MPEInternalFrame.this.doClose();
	}

	/** @since 20020813 */
	public void cptChanged( CPTChangeEvent evt ){
		MPEInternalFrame.this.doClose();
	}

	/**
		fir interface RC.RecCondThreadListener
		@author Keith Cascio
		@since 020503
	*/
	public void rcFinished( double result, long time_ns )
	{
		throw new IllegalStateException( "MPEInternalFrame should not invoke Pr(e) computations." );
	}

	/**
		fir interface RC.RecCondThreadListener
		@author Keith Cascio
		@since 020503
	*/
	public void rcFinishedMPE( double result, Map instantiation, long time_ns )
	{
		myProbabilityMPE = result;
		myMapMPEinstantiation = instantiation;
	}

	/**
		interface RC.RecCondThreadListener
		@author Keith Cascio
		@since 061003
	*/
	public void rcComputationError( String msg )
	{
		myProbabilityMPE = (double)-1;
		myMapMPEinstantiation = Collections.EMPTY_MAP;
		JOptionPane.showMessageDialog( this, msg, "RC Computation Error", JOptionPane.ERROR_MESSAGE );
	}

	protected double myProbabilityMPE = (double)0;
	protected Map myMapMPEinstantiation = null;

	/** @since 20060223 */
	private MapEngine makeMapEngine(){
		// Get the belief network and evidence information. This data must
		// be passed to the MapEngine to perform the MPE computation
		BeliefNetwork bn = this.getBeliefNetwork();
		Map evidence = bn.getEvidenceController().evidence();

		// Create a new set of variables and remove those whose evidence have
		// set keys (why remove, I don't know)
		Set allVars=new HashSet( bn );
		allVars.removeAll( evidence.keySet() );

		// Initialize a MapEngine and perform the mpe computation
		MapEngine mpe=new MapEngine(bn, allVars, evidence);

		return mpe;
	}

	// This function is called externally every time someone clicks on MPE Computation
	// in the Tools menu at the top of the screen
	public void reInitialize()
	{
		edu.ucla.belief.ui.util.Util.pushStatusWest( hnInternalFrame, STR_MSG_MPE );
		setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ));

		clear();

		myProbabilityMPE = (double)0;
		myMapMPEinstantiation = null;
		InferenceEngine ie = this.getInferenceEngine();

		boolean flagProbabilitySupported = ie.probabilitySupported();
		double prE = ( flagProbabilitySupported ) ? ie.probability() : Double.MAX_VALUE;

		if( (prE > (double)0) || (!flagProbabilitySupported) )
		{
			if( ie.canonical() instanceof RCInferenceEngine )
			{
				RCDgraph graph = ((RCInferenceEngine) ie.canonical()).underlyingCompilation();
				try{
					graph.recCond_MPEAsThread( this ).join();
				}catch( InterruptedException e ){
					System.err.println( "Warning: MPEInternalFrame.reInitialize() interrupted." );
					Thread.currentThread().interrupt();
					return;
				}
			}
			else{
				MapEngine mpe = makeMapEngine();

				myProbabilityMPE = mpe.probability();
				myMapMPEinstantiation = mpe.getInstance();
			}

			if( myProbabilityMPE >= (double)0 && myMapMPEinstantiation != null )
			{
				MPEPanel pnlMain = new MPEPanel( myProbabilityMPE, myMapMPEinstantiation, this.getBeliefNetwork(), /*flagAddCloseButton*/ false, /*flagAddCopyButtons*/ false );
				if( flagProbabilitySupported ) pnlMain.addResult( myProbabilityMPE / prE, "P(mpe|e)=" );
				//btnClose = pnlMain.addActionListener( this );
				if( this.getParentFrame() != null ) pnlMain.setClipBoard( this.getParentFrame().getInstantiationClipBoard(), this.getBeliefNetwork().getEvidenceController() );
				//myButtonCodeBandit = pnlMain.addButton( "Code Bandit", this );
				//myButtonCodeBandit.setIcon( MainToolBar.getIcon( CodeToolInternalFrame.STR_FILENAME_BUTTON_ICON ) );
				//pnlMain.addButton( myRunSensitivity );

				MPEInternalFrame.this.setResultPanel( pnlMain );
				pack();

				//Keith Cascio 060302
				//a temporary fix for the "intractable MPE window" bug
				try{
					setMaximum( false );
				}catch( java.beans.PropertyVetoException e ){
					System.err.println( "Java warning: " + e );
				}
			}
		}
		else
		{
			JPanel pnlMain = new JPanel( new GridBagLayout() );
			JLabel lblError = SensitivityInternalFrame.getErrorMessageLabel( "Impossible evidence." );
			GridBagConstraints c = new GridBagConstraints();
			pnlMain.add( lblError, c );
			myPanelMain.add( pnlMain );
		}

		// Change the cursor back to the arrow
		setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
		edu.ucla.belief.ui.util.Util.popStatusWest( hnInternalFrame, STR_MSG_MPE );

		return;
	}

	/** @since 20040602 */
	public void codeBandit()
	{
		if( hnInternalFrame == null ) return;
		CodeToolInternalFrame ctif;
		try{
			ctif = hnInternalFrame.getCodeToolInternalFrame();
			if( ctif != null ){
				ctif.setCodeGenius( setupMPECoder() );
				ctif.setVisible( true );
			}
		}catch( Exception exception ){
			String msg = exception.getMessage();
			if( msg == null ) msg = exception.toString();
			hnInternalFrame.getParentFrame().showErrorDialog( msg );
		}
	}

	/** @since 20040602 */
	public MPECoder setupMPECoder()
	{
		String pathInput = null;
		if( this.hnInternalFrame != null ){
			pathInput = hnInternalFrame.getFileName();
			if( hnInternalFrame.getCodeToolInternalFrame().failOnMissingNetworkFile( pathInput ) ) return (MPECoder) null;
		}

		MPECoder mpecoder = new MPECoder();

		Map evidence = new HashMap( this.getBeliefNetwork().getEvidenceController().evidence() );
		mpecoder.setEvidence( evidence );

		if( pathInput != null ){
			mpecoder.setPathInputFile( pathInput );
		}

		return mpecoder;
	}

	/** @since 20051102 */
	private void clear(){
		MPEInternalFrame.this.myPanelMain.removeAll();
		MPEInternalFrame.this.myTabbedPane = null;
		MPEInternalFrame.this.clearSensitivity();
	}

	/** @since 20051129 */
	private void clearSensitivity(){
		MPEInternalFrame.this.mySensMPEEngine = null;
		MPEInternalFrame.this.mySensMPEReport = null;
		MPEInternalFrame.this.mySensitivityComponent = null;
		MPEInternalFrame.this.mySensitivitySuggestionTable = null;
		MPEInternalFrame.this.myPain = null;
		//MPEInternalFrame.this.myLogOddsChangeColumn = MPEInternalFrame.this.myAbsolutChangeColumn = null;
		myActionToggleTableDetails.setEnabled( false );
	}

	/** @since 20051102 */
	private void setResultPanel( MPEPanel panel ){
		this.myResultsPanel = panel;

		if( myMenuEdit != null ){
			myMenuEdit.removeAll();
			myMenuEdit.add( new JMenuItem( myResultsPanel.getActionCopy() ) );
			myMenuEdit.add( new JMenuItem( myResultsPanel.getActionCopyPlus() ) );
		}

		SamiamAction adiff = myResultsPanel.getDiff();
		if( (adiff != null) && (myMenuTools != null) ){
			if( myButtonDiff != null ) myMenuTools.remove( myButtonDiff );
			myMenuTools.add( myButtonDiff = new JCheckBoxMenuItem( adiff ) );
		}

		if( myTabbedPane == null ){
			myPanelMain.add( panel );
			MPEInternalFrame.this.pack();
		}
		else myTabbedPane.setComponentAt( INT_TAB_INDEX_RESULTS, this.myResultsPanel );
	}

	/** @since 20051102 */
	private void setSensitivityComponent( JComponent panel ){
		this.mySensitivityComponent = panel;

		boolean flagPack = ( myTabbedPane == null );
		getTabbedPane().setComponentAt( INT_TAB_INDEX_SENSITIVITY, this.mySensitivityComponent );

		if( flagPack ) MPEInternalFrame.this.bestWindowArrangement();//MPEInternalFrame.this.pack();
	}

	/** @since 20051102 */
	private JTabbedPane getTabbedPane(){
		if( myTabbedPane == null ){
			myTabbedPane = new JTabbedPane();

			JComponent results = this.myResultsPanel;
			if( results == null ) results = new JLabel( "no results" );
			myTabbedPane.addTab( STR_TITLE_RESULTS, MainToolBar.getIcon( MPEInternalFrame.STR_FILENAME_ICON ), results, STR_TAB_RESULTS_TOOLTIP );

			JComponent sensitivity = this.mySensitivityComponent;
			if( sensitivity == null ) sensitivity = new JLabel( "no sensitivity" );
			myTabbedPane.addTab( STR_TITLE_SENSITIVITY, MainToolBar.getIcon( SensitivityInternalFrame.STR_FILENAME_ICON ), sensitivity, STR_TAB_SENSITIVITY_TOOLTIP );

			myPanelMain.removeAll();
			myPanelMain.add( myTabbedPane );

			MPEInternalFrame.this.validate();
		}
		return myTabbedPane;
	}

	/** @since 20051102 */
	private void doClose(){
		MPEInternalFrame.this.setVisible( false );
	}

	/** @since 20051102 */
	private SamiamAction myActionClose = new SamiamAction( "Close", "Hide the MPE tool", 'c', (Icon)null ){
		public void actionPerformed( ActionEvent e ){
			MPEInternalFrame.this.doClose();
		}
	};

	/** @since 20051102 */
	private SamiamAction myActionBandit = new SamiamAction( "Code Bandit", "Code Bandit creates an example MPE java program", 'b', MainToolBar.getIcon( CodeToolInternalFrame.STR_FILENAME_BUTTON_ICON ) ){
		public void actionPerformed( ActionEvent e ){
			MPEInternalFrame.this.codeBandit();
		}
	};

	/** @since 20051102 */
	private InterruptableAction myRunSensitivity = new InterruptableAction( STR_BUTTON_SENSITIVITY_TEXT, STR_BUTTON_SENSITIVITY_TOOLTIP, 's', MainToolBar.getIcon( SensitivityInternalFrame.STR_FILENAME_BUTTON_ICON ) ){
		public void runImpl( Object arg1 ){
			MPEInternalFrame.this.doSensitivity();
		}
	};

	/** @since 20051129 */
	private BooleanStateAction myActionToggleTableDetails = new BooleanStateAction( "Show sensitivity details", "<html><nobr>Show { <font color=\"#009900\">absolute change</font>, <font color=\"000099\">log odds change</font> } in sensitivity results table", (Icon)null, false ){
		public void actionPerformed( boolean state ){
			MPEInternalFrame.this.setTableDetailsVisible();
		}
	};

	/** @since 20060223 */
	public final HandledModalAction action_ADOPTCHANGE = new HandledModalAction( "Adopt suggested change automatically", "Automatically adopt parameter change", 'a', (Icon)null, ModeHandler.WRITABLE, false ){
		//{ this.setSamiamUserMode( hnInternalFrame.getSamiamUserMode() ); }

		public void actionPerformed( ActionEvent e ){
			doAdoptChange();
		}
	};

	/** @since 20060223 */
	public final HandledModalAction action_EDITCPT = new HandledModalAction( "Edit CPT by hand", "Open CPT edit dialog", 'e', (Icon)null, ModeHandler.WRITABLE, false ){
		//{ this.setSamiamUserMode( hnInternalFrame.getSamiamUserMode() ); }

		public void actionPerformed( ActionEvent e ){
			DisplayableFiniteVariable dvar = getVariableForSelectedSuggestion();
			if( dvar != null ) dvar.showNodePropertiesDialog( hnInternalFrame, true );
		}
	};

	/** @since 20060223 */
	public final HandledModalAction action_TESTSOUNDNESS = new HandledModalAction( "Test soundness", "Test whether adopting the change flips the MPE answer", 't', (Icon)null, ModeHandler.WRITABLE, false ){
		public void actionPerformed( ActionEvent e ){
			doTestSoundness();
		}
	};

	/** @since 20060223 */
	private void setEditActionsEnabled( boolean enabled )
	{
		////action_ADOPTCHANGE.setEnabled( enabled );
		////action_EDITCPT.setEnabled( enabled );
		//if( myMenuAdopt != null ) myMenuAdopt.setEnabled( enabled );
		MPEInternalFrame.this.myFlagEnableEditActions = enabled;
	}

	/** @since 20060223 */
	public void doTestSoundness(){
		if( mySensitivitySuggestionTable == null ) return;

		SingleParamSuggestion ss = (SingleParamSuggestion) mySensitivitySuggestionTable.getCurrentlySelectedSuggestion();
		if( ss == null ) return;

		try{
			if( myMapMPEinstantiation == null ) throw new RuntimeException( "no mpe instantiation" );
			if( myMapMPEinstantiation.isEmpty() ) throw new RuntimeException( "empty mpe instantiation" );
			Map before = new HashMap( myMapMPEinstantiation );

			setEditActionsEnabled( false );

			ss.adoptChange( DOUBLE_EPSILON_SOUNDNESS );
			mySensitivitySuggestionTable.flagValid = false;
			//hnInternalFrame.setCPT( ss.getVariable() );

			MapEngine mpe = MPEInternalFrame.this.makeMapEngine();

			double prMPE = mpe.probability();
			Map after    = mpe.getInstance();

			ss.undo();
			mySensitivitySuggestionTable.flagValid = true;
			//hnInternalFrame.setCPT( ss.getVariable() );

			Map diff = diff( before, after, null );

			StringBuffer buff = new StringBuffer( 128 );

			int type = JOptionPane.WARNING_MESSAGE;
			if( diff.isEmpty() ){
				type = JOptionPane.ERROR_MESSAGE;
				buff.append( "Failed to flip MPE!" );
			}
			else{
				type = JOptionPane.PLAIN_MESSAGE;
				buff.append( "Flipped MPE in " );
				buff.append( diff.size() );
				buff.append( " values: " );
				buff.append( AbstractStringifier.VARIABLE_ID.mapToString( diff ) );
			}

			hnInternalFrame.getParentFrame().showMessageDialog( buff.toString(), "Sensitivity Correctness Test", type );
		}catch( Exception exception ){
			hnInternalFrame.getParentFrame().showErrorDialog( exception.getMessage() );
		}finally{
			setEditActionsEnabled( true );
		}
	}

	/** @since 20060223 */
	public static Map diff( Map map1, Map map2, Map out ){
		if( (map1 == null) || (map2 == null) ) throw new IllegalArgumentException();
		if( map1.size() != map2.size() ) throw new IllegalArgumentException();

		Set keys1, keys2;
		if( !(keys1 = map1.keySet()).equals( keys2 = map2.keySet() ) ) throw new IllegalArgumentException();

		if( out == null ) out = new HashMap( map1.size() );

		Object var1, value1, value2;
		for( Iterator it = keys1.iterator(); it.hasNext(); ){
			value1 = map1.get( var1 = it.next() );
			value2 = map2.get( var1 );

			if( value1 != value2 ) out.put( var1, value2 );
		}

		return out;
	}

	/** @since 20060223 */
	public void doAdoptChange()
	{
		if( mySensitivitySuggestionTable == null ) return;

		SensitivitySuggestion ss = mySensitivitySuggestionTable.getCurrentlySelectedSuggestion();
		if( ss == null ) return;
		else{
			setEditActionsEnabled( false );
			try {
				ss.adoptChange();
			}catch( Exception e ){
				hnInternalFrame.getParentFrame().showErrorDialog( e.getMessage() );
			}

			mySensitivitySuggestionTable.flagValid = false;
			hnInternalFrame.setCPT( ss.getVariable() );
		}
	}

	/** @since 20060223 */
	public DisplayableFiniteVariable getVariableForSelectedSuggestion()
	{
		if( mySensitivitySuggestionTable == null ) return null;

		SensitivitySuggestion ss = mySensitivitySuggestionTable.getCurrentlySelectedSuggestion();
		if( ss == null ) return null;
		else return (DisplayableFiniteVariable) ((SingleParamSuggestion)ss).getCPTParameter().getVariable();
	}

	/** @since 20051102 */
	private void doSensitivity(){
		try{
			if( this.mySensMPEEngine == null ){
				this.mySensMPEEngine = new SensMPEEngine( MPEInternalFrame.this.getBeliefNetwork(), EliminationHeuristic.MIN_FILL );
			}

			if( this.mySensMPEReport == null ){
				this.mySensMPEReport = this.mySensMPEEngine.getResults();
			}

			if( this.mySensitivityComponent == null ){
				initSensitivityTable();
			}

			this.getTabbedPane().setSelectedIndex( INT_TAB_INDEX_SENSITIVITY );
		}catch( Exception exception ){
			System.err.println( "Warning: MPEInternalFrame.doSensitivity() caught " + exception );
			exception.printStackTrace();
		}
	}

	/** @since 20051129 */
	public void bestWindowArrangement(){
		try{
			Point locBefore = this.getLocation();
			Dimension sizeBefore = this.getSize();
			Dimension sizeParent = this.getParent().getSize();

			Point locAfter = new Point( 0, locBefore.y );
			Dimension sizeAfter = new Dimension( sizeParent.width, sizeBefore.height );

			this.setMaximum( false );
			this.setBounds( new Rectangle( locAfter, sizeAfter ) );
		}catch( Exception exception ){
			System.err.println( "Warning: MPEInternalFrame.bestWindowArrangement() caught " + exception );
		}
	}

	/** @since 20051129 */
	private void initSensitivityTable(){
		List sps = this.mySensMPEReport.generateSingleParamSuggestions();
		PreferenceGroup globalPrefs = hnInternalFrame.getPackageOptions().getPreferenceGroup( SamiamPreferences.PkgDspNme );
		mySensitivitySuggestionTable = new SensitivitySuggestionTable( hnInternalFrame, globalPrefs, sps, true );
		mySensitivitySuggestionTable.setAutoCreateColumnsFromModel( false );
		myPain = new JScrollPane( mySensitivitySuggestionTable );

		//this.setSensitivityComponent( new JLabel( "temporary sensitivity placeholder" ) );
		this.setSensitivityComponent( myPain );

		tableModelChanged();
	}

	/** @since 20051129 */
	private void tableModelChanged(){
		mySensitivitySuggestionTable.getSensitivitySuggestionTableModel().setColumnName( SensitivitySuggestionTableModel.NEW_VALUE_COLUMN_INDEX, STR_NAME_COLUMN_NEW_VALUE );
		setTableDetailsVisible();

		/*JViewport port = myPain.getColumnHeader();
		if( port != null ){
			Component compColumnHeader = port.getView();
			if( compColumnHeader != null ){
				//System.out.println( "compColumnHeader is " + compColumnHeader.getClass().getName() );
				compColumnHeader.validate();
			}
		}*/

		/*JTableHeader header = mySensitivitySuggestionTable.getTableHeader();
		if( header != null ){
			header.resizeAndRepaint();//header.updateUI();
		}*/
	}

	/** @since 20051129 */
	private void setTableDetailsVisible(){
		if( mySensitivitySuggestionTable == null ) return;
		myActionToggleTableDetails.setEnabled( true );
		mySensitivitySuggestionTable.setTableDetailsVisible( myActionToggleTableDetails.getState() );
	}

	/** @since 20051102 */
	private BeliefNetwork getBeliefNetwork(){
		if( hnInternalFrame != null ) return hnInternalFrame.getBeliefNetwork();
		else return myDebugBeliefNetwork;
	}

	/** @since 20051102 */
	private InferenceEngine getInferenceEngine(){
		if( hnInternalFrame != null ) return hnInternalFrame.getInferenceEngine();
		else return myDebugInferenceEngine;
	}

	/** @since 20051102 */
	private UI getParentFrame(){
		if( hnInternalFrame != null ) return hnInternalFrame.getParentFrame();
		else return myDebugUI;
	}

	/*public void actionPerformed( ActionEvent e ){
		Object src = e.getSource();
		if( src == btnClose ) setVisible( false );// If the btnClose button is clicked, hide the internal frame.
		else if( src == myButtonCodeBandit ) codeBandit();
		return;
	}*/

	/** test/debug
		@since 20051102 */
	public static void main( String[] args ){
		//String path = "c:\\keithcascio\\networks\\cancer.net";
		String path = "c:\\keith\\code\\argroup\\networks\\cancer.net";
		try{
			Util.setLookAndFeel();

			MPEInternalFrame mpeif = new MPEInternalFrame( (NetworkInternalFrame)null );

			mpeif.myDebugBeliefNetwork = edu.ucla.belief.io.NetworkIO.read( path );
			mpeif.myDebugInferenceEngine = new edu.ucla.belief.inference.JEngineGenerator().manufactureInferenceEngine( mpeif.myDebugBeliefNetwork );
			mpeif.reInitialize();

			JDesktopPane desk = new JDesktopPane();
			desk.add( mpeif );
			mpeif.setLocation( 50,50 );
			mpeif.setVisible( true );

			JFrame frame = Util.getDebugFrame( "MPEInternalFrame TEST/DEBUG", desk );
			frame.setVisible( true );

			mpeif.setSelected( true );
		}catch( Exception exception ){
			System.err.println( "Error in MPEInternalFrame.main():" );
			exception.printStackTrace();
			return;
		}
	}

	protected NetworkInternalFrame hnInternalFrame;
	protected Object btnClose = new Object();
	private JButton myButtonCodeBandit;
	private JTabbedPane myTabbedPane;
	private MPEPanel myResultsPanel;
	private JComponent mySensitivityComponent;
	private JPanel myPanelMain;
	private JMenu myMenuEdit, myMenuTools;
	private AbstractButton myButtonDiff;

	private BeliefNetwork myDebugBeliefNetwork;
	private InferenceEngine myDebugInferenceEngine;
	private UI myDebugUI;

	private SensMPEEngine mySensMPEEngine;
	private SensMPEReport mySensMPEReport;
	private SensitivitySuggestionTable mySensitivitySuggestionTable;
	private JScrollPane myPain;
	//private TableColumn myLogOddsChangeColumn, myAbsolutChangeColumn;
	private boolean myFlagEnableEditActions = true;
}
