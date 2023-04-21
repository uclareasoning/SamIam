package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.actionsandmodes.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.networkdisplay.NetworkDisplay;
import edu.ucla.belief.ui.statusbar.StatusBar;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import edu.ucla.belief.*;
import edu.ucla.belief.sensitivity.*;
import edu.ucla.util.code.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;

/**
	@author david allen
	@author hei chan
	@author keith cascio
*/
public class SensitivityInternalFrame extends JInternalFrame implements
	NetStructureChangeListener, ItemListener,
	SamiamUserModal,
	SensitivitySuggestionTable.TableModelListener, ListSelectionListener,
	//MouseListener,
	EvidenceChangeListener, CPTChangeListener, ChangeListener//, ActionListener
{
	private   NetworkInternalFrame       hnInternalFrame;
	private   SensitivityEngine          se;
	protected PreferenceGroup            myGlobalPrefs;
	private   SensitivitySuggestionTable mySensitivitySuggestionTable;
	private   ChooseInstancePanel2       yPanel, zPanel;
	private   JComboBox                  cbArithmeticOperators, cbComparisonOperators;
	private   DecimalField               constField;
	private   JPanel                     mainPanel, bottomPanel, pnlEvent2, pnlEvent2PlaceHolder;
	private   JComponent                 ctrlPanel, myComponentBlank, myComponentImpossible;
	private   JScrollPane                myPainSingleParam;
	private   JTabbedPane                myResultsTabbed;
	private   SingleCPTSuggestionPanel   myPanelSingleCPT;
	private   JMenu                      myMenuTools, myMenuAdopt, myMenuSettings;
	private   boolean                    myTableDetailsVisible         = false;

	public    static         boolean     DEBUG_BORDERS                 = false,
	                                     FLAG_CRAM                     = false;
	public    static  final  int         INT_MIN_BEST_HEIGHT           = 211,
	                                     INT_BUTTON_PANEL_GRAY_LEVEL   = 196;
	public    static  final  Color       COLOR_BUTTON_PANEL            = new Color( INT_BUTTON_PANEL_GRAY_LEVEL, INT_BUTTON_PANEL_GRAY_LEVEL, INT_BUTTON_PANEL_GRAY_LEVEL );

	public    static  final  String      STR_FILENAME_ICON             = "Sensitivity16.gif",
	                                     STR_FILENAME_BUTTON_ICON      = "Sensitivity16x13.gif",
	                                     STR_MSG_SENSITIVITY           = "computing sensitivity results...",
	                                     STR_MSG_IMPOSSIBLE            = "Current evidence setting is impossible.",
	                                     STR_MSG_ERROR_PARTIALS        = "Sensitivity analysis requires partial derivatives.",
	                                     STR_MSG_SATISFIED             = "The current belief network already satisfies the specified constraint.",
	                                     STR_MSG_UNSATISFIABLE         = "The specified constraint is unsatisfiable.",
	                                     STR_START_VERBOSE             = "Start sensitivity analysis",
	                                     STR_START_TERSE               = "Start";

	public SensitivityInternalFrame(	NetworkInternalFrame hnInternalFrame,
						PreferenceGroup globalPrefs )
	{
		super("Sensitivity Analysis", true, true, true, true);
		this.hnInternalFrame = hnInternalFrame;
		this.myGlobalPrefs = globalPrefs;
		hnInternalFrame.addNetStructureChangeListener( this );
		hnInternalFrame.addEvidenceChangeListener( this );
		hnInternalFrame.addCPTChangeListener( this );
		se = new SensitivityEngine(
			hnInternalFrame.getBeliefNetwork(),
			hnInternalFrame.getInferenceEngine(),
			hnInternalFrame.getPartialDerivativeEngine(),
			hnInternalFrame.console);
		init();
	}

	/** @since 021405 Valentine's Day! */
	public ChooseInstancePanel2 getPanelY(){
		return this.yPanel;
	}

	/** @since 021405 Valentine's Day! */
	public ChooseInstancePanel2 getPanelZ(){
		return this.zPanel;
	}

	/** @since 021405 Valentine's Day! */
	public void setOperatorArithmetic( Object op ){
		if( cbArithmeticOperators != null ) cbArithmeticOperators.setSelectedItem( op );
	}

	/** @since 021405 Valentine's Day! */
	public void setOperatorComparison( Object op ){
		if( cbComparisonOperators != null ) cbComparisonOperators.setSelectedItem( op );
	}

	/** @since 021405 Valentine's Day! */
	public JComboBox getComboArithmetic(){
		return cbArithmeticOperators;
	}

	/** @since 021405 Valentine's Day! */
	public JComboBox getComboComparison(){
		return cbComparisonOperators;
	}

	/** @since 021505 */
	public void setConstValue( double value ){
		if( constField != null ) constField.setValue( value );
	}

	/** @since 20020515 */
	private void init()
	{
		Icon iconFrame = MainToolBar.getIcon( STR_FILENAME_ICON );
		if( iconFrame != null ) setFrameIcon( iconFrame );

		JPanel headerPanel = new JPanel( new BorderLayout() );
		headerPanel.add( new JScrollPane( ctrlPanel = makeControlPanel() ), BorderLayout.NORTH  );
		headerPanel.add(                               makeButtonPanel()  , BorderLayout.CENTER );

		JPanel controlPane = new JPanel( new BorderLayout() );
		controlPane.add( headerPanel,                                       BorderLayout.NORTH  );
		controlPane.add( mainPanel = new JPanel( new BorderLayout() ),      BorderLayout.CENTER );
		getContentPane().add( controlPane );

		this.setSamiamUserMode( hnInternalFrame.getSamiamUserMode() );
	}

	/** @since 20050921 */
	public void bestWindowArrangement(){
		Rectangle bestBounds = (Rectangle)null;
		try{
			UI ui = hnInternalFrame.getParentFrame();
			ui.setVisibleToolbarMain( false );
			ui.setVisibleToolbarInstantiation( false );
			ui.setVisibleStatusBar( false );
			ui.validate();

			hnInternalFrame.bestWindowArrangement();
			hnInternalFrame.getTreeScrollPane().getEvidenceTree().setExpandVariableBranches( true );
			hnInternalFrame.cramEvidenceTree();
			hnInternalFrame.bestWindowArrangement();
			NetworkDisplay nd = hnInternalFrame.getNetworkDisplay();
			nd.setZoomFactor( (double)1 );
			nd.cringe( /* aggressive */ true );

			Rectangle boundsNetwork = nd.getBounds();
			Rectangle boundsDesktop = hnInternalFrame.getRightHandDesktopPane().getBounds();
			Insets insetsNetwork = nd.getInsets();

			int x = 0;
			int y = boundsNetwork.y + boundsNetwork.height - insetsNetwork.bottom;
			int width = boundsDesktop.width;
			int height = boundsDesktop.height - y;

			if( height < INT_MIN_BEST_HEIGHT ){
				height = INT_MIN_BEST_HEIGHT;
				y = boundsDesktop.height - height;
			}

			bestBounds = new Rectangle( x, y, width, height );

			this.setMaximum( false );
			this.setBounds( bestBounds );
			this.setSelected( true );
		}catch( Exception exception ){
			System.err.println( "Warning: SensitivityInternalFrame.bestWindowArrangement() failed, caught " + exception );
			//exception.printStackTrace();
			return;
		}
	}

	/** @since 20040603 */
	public void codeBandit(){
		CodeToolInternalFrame ctif;
		try{
			ctif = hnInternalFrame.getCodeToolInternalFrame();
			if( ctif != null ){
				SensitivityCoder coder = setupSensitivityCoder();
				if( coder != null ){
					setMaximumSafe( false );
					ctif.setCodeGenius( coder );
					ctif.setVisible( true );
				}
			}
		}catch( Exception exception ){
			String msg = exception.getMessage();
			if( msg == null ) msg = exception.toString();
			hnInternalFrame.getParentFrame().showErrorDialog( msg );
		}
	}

	/** @since 20040603 */
	public SensitivityCoder setupSensitivityCoder()
	{
		String pathInput = null;
		if( this.hnInternalFrame != null ){
			pathInput = hnInternalFrame.getFileName();
			if( hnInternalFrame.getCodeToolInternalFrame().failOnMissingNetworkFile( pathInput ) ) return (SensitivityCoder) null;
		}

		DisplayableFiniteVariable dvarY = yPanel.getVariable();
		Object valueY = yPanel.getInstance();
		DisplayableFiniteVariable dvarZ = null;
		Object valueZ = null;

		if( pnlEvent2PlaceHolder.isAncestorOf( pnlEvent2 ) ){//2-event constraint
			dvarZ = zPanel.getVariable();
			valueZ = zPanel.getInstance();
		}

		Object opComparison = cbComparisonOperators.getSelectedItem();
		Object opArithmetic = cbArithmeticOperators.getSelectedItem();

		double c = parseConstField();
		if( c < 0 ) return (SensitivityCoder)null;

		SensitivityCoder sensitivitycoder = new SensitivityCoder();
		Map evidence = new HashMap( hnInternalFrame.getBeliefNetwork().getEvidenceController().evidence() );

		sensitivitycoder.setPathInputFile( pathInput );
		sensitivitycoder.setEvidence( evidence );
		sensitivitycoder.setConstraint( dvarY, valueY, dvarZ, valueZ, opComparison, opArithmetic, c );
		sensitivitycoder.setBeliefNetwork( hnInternalFrame.getBeliefNetwork() );
		sensitivitycoder.setDynamator( hnInternalFrame.getParentFrame().getDynamator().getCanonicalDynamator() );
		sensitivitycoder.setInferenceEngine( hnInternalFrame.getInferenceEngine() );

		return sensitivitycoder;
	}

	/** interface SamiamUserModal
		@since 20050818 */
	public void setSamiamUserMode( SamiamUserMode mode ){
		action_ADOPTCHANGE.setMode( mode, hnInternalFrame );
		action_EDITCPT.setMode( mode, hnInternalFrame );
		this.setStartEnabled( mode );
	}

	/** @since 20040603 */
	public final SamiamAction action_CODEBANDIT = new SamiamAction( CodeToolInternalFrame.STR_DISPLAY_NAME, CodeToolInternalFrame.STR_DISPLAY_NAME_1CAP+": encode sensitivity query", 'c', MainToolBar.getIcon( "CodeBandit16.gif" ) ){
		public void actionPerformed( ActionEvent e ){
			codeBandit();
		}
	};

	/** @since 20050921 */
	public final SamiamAction action_BESTWINDOWARRANGEMENT = new SamiamAction( "Arrange Windows", "Arrange windows efficiently", 'a', MainToolBar.getBlankIcon() ){
		public void actionPerformed( ActionEvent e ){
			bestWindowArrangement();
		}
	};

	/** @since 20040603 */
	public final HandledModalAction action_ADOPTCHANGE = new HandledModalAction( "Adopt suggested change automatically", "Automatically adopt parameter change", 'a', (Icon)null, ModeHandler.WRITABLE, false ){
		//{ this.setSamiamUserMode( hnInternalFrame.getSamiamUserMode() ); }

		public void actionPerformed( ActionEvent e ){
			doAdoptChange();
		}
	};

	/** @since 20040603 */
	public final HandledModalAction action_EDITCPT = new HandledModalAction( "Edit CPT by hand", "Open CPT edit dialog", 'e', (Icon)null, ModeHandler.WRITABLE, false ){
		//{ this.setSamiamUserMode( hnInternalFrame.getSamiamUserMode() ); }

		public void actionPerformed( ActionEvent e ){
			DisplayableFiniteVariable dvar = getVariableForSelectedSuggestion();
			if( dvar != null ) dvar.showNodePropertiesDialog( hnInternalFrame, true );
		}
	};

	/** @since 20040603 */
	public final HandledModalAction action_START = new HandledModalAction( FLAG_CRAM ? STR_START_TERSE : STR_START_VERBOSE, "Query the sensitivity engine for suggestions", 's', (Icon)null, ModeHandler.QUERY_THAWED_PARTIAL_ENGINE, false ){
		//{ this.setSamiamUserMode( hnInternalFrame.getSamiamUserMode() ); }

		public void actionPerformed( ActionEvent e ){
			doStartSensitivity();
		}
	};
	private boolean myMaskStartEnabled = false;

	/** @since 20040603 */
	public final BooleanStateAction action_TOGGLEEVENT2 = new BooleanStateAction( "Constrain Two Events", "Constrain Two Events", (Icon)null, false ){
		public void actionPerformed( boolean state ){
			setEvent2( state );
		}
	};

	/** @since 20040603 */
	public final BooleanStateAction action_TOGGLETABLEDETAILS = new BooleanStateAction( "Show Table Details", "Show Table Details", (Icon)null, false ){
		public void actionPerformed( boolean state ){
			myTableDetailsVisible = state;
			setTableDetailsVisible();
		}
	};

	/** @since 20060209 */
	public final BooleanStateAction action_TOGGLESINGLEPARAMETERSUGGESTIONS = new BooleanStateAction( "generate single parameter suggestions", "Suggest single parameter changes that satisfy your constraint", (Icon)null, true ){
		public void actionPerformed( boolean state ){
			SensitivityInternalFrame.this.handleSuggestionTypes();
		}
	};

	/** @since 20060209 */
	public final BooleanStateAction action_TOGGLESINGLECPTSUGGESTIONS = new BooleanStateAction( "generate single CPT suggestions", "Suggest changes to the parameters of single cpts that satisfy your constraint", (Icon)null, true ){
		public void actionPerformed( boolean state ){
			SensitivityInternalFrame.this.handleSuggestionTypes();
		}
	};

	/** @since 20060209 */
	private final BooleanStateAction[] myArraySuggestionTypeActions = new BooleanStateAction[] { action_TOGGLESINGLEPARAMETERSUGGESTIONS, action_TOGGLESINGLECPTSUGGESTIONS };

	/** @since 20060209 */
	private void handleSuggestionTypes(){
		try{
			BooleanStateAction selected = null;
			int numSelected = 0;
			for( int i=0; i<myArraySuggestionTypeActions.length; i++ ){
				if( myArraySuggestionTypeActions[i].getState() ){
					++numSelected;
					selected = myArraySuggestionTypeActions[i];
				}
			}
			if( numSelected < 1 ){
				(selected = myArraySuggestionTypeActions[0]).setState( true );
				numSelected = 1;
			}
			for( int i=0; i<myArraySuggestionTypeActions.length; i++ ){
				myArraySuggestionTypeActions[i].setEnabled( (myArraySuggestionTypeActions[i] != selected) || (numSelected > 1) );
			}
		}catch( Exception exception ){
			System.err.println( "Warning: SensitivityInternalFrame.handleSuggestionTypes() caught " + exception );
		}
	}

	/** @since 20020515 */
	public void toggleEvent2(){
		setEvent2( !pnlEvent2PlaceHolder.isAncestorOf( pnlEvent2 ) );
	}

	/** @since 20040603 */
	public void setEvent2( boolean flag )
	{
		if( flag && (!pnlEvent2PlaceHolder.isAncestorOf( pnlEvent2 )) ){
			pnlEvent2PlaceHolder.add( pnlEvent2 );
			yPanel.setTitle( "Event 1" );
		}
		else if( (!flag) && pnlEvent2PlaceHolder.isAncestorOf( pnlEvent2 ) ){
			pnlEvent2PlaceHolder.remove( pnlEvent2 );
			yPanel.setTitle( "Event" );
		}
		ctrlPanel.revalidate();
		//cbToggleEvent2.setSelected( flag );//make sure the checkbox corresponds
		action_TOGGLEEVENT2.setState( flag );
	}

	/** @since 20020515 */
	public void toggleTableDetails()
	{
		myTableDetailsVisible = !myTableDetailsVisible;
		setTableDetailsVisible();
	}

	/** @since 20020813 */
	public void setTableDetailsVisible(){
		if( mySensitivitySuggestionTable == null ) return;

		/*TableColumnModel tcm = mySensitivitySuggestionTable.getColumnModel();
		if( myTableDetailsVisible ){
			tcm.addColumn( myAbsolutChangeColumn );
			tcm.addColumn( myLogOddsChangeColumn );
		}
		else{
			tcm.removeColumn( myAbsolutChangeColumn );
			tcm.removeColumn( myLogOddsChangeColumn );
		}

		mySensitivitySuggestionTable.prettySize();*/

		mySensitivitySuggestionTable.setTableDetailsVisible( myTableDetailsVisible );

		//cbToggleTableDetails.setSelected( myTableDetailsVisible );//make sure the checkbox corresponds
		action_TOGGLETABLEDETAILS.setState( myTableDetailsVisible );
	}

	/** @since 20050215 */
	public JMenu getMenuAdopt(){
		return myMenuAdopt;
	}

	/** @since 20020515 */
	private JComponent makeButtonPanel()
	{
		JButton btnStart = new JButton( action_START );
		Font fontStart = btnStart.getFont();
		btnStart.setFont( fontStart.deriveFont( Font.BOLD ) );

		JCheckBox cbToggleEvent2 = new JCheckBox( action_TOGGLEEVENT2 );
		Font fontCheckBoxes = cbToggleEvent2.getFont().deriveFont( (float)9 );
		cbToggleEvent2.setFont( fontCheckBoxes );
		action_TOGGLEEVENT2.addListener( cbToggleEvent2 );

		JCheckBox cbToggleTableDetails = new JCheckBox( action_TOGGLETABLEDETAILS );
		cbToggleTableDetails.setFont( fontCheckBoxes );
		action_TOGGLETABLEDETAILS.addListener( cbToggleTableDetails );

		JMenuBar bar = new JMenuBar();
		bar.add( myMenuSettings = new JMenu( "<html><u>S</u>ettings" ) );
		JCheckBoxMenuItem item;
		myMenuSettings.add( item = new JCheckBoxMenuItem( action_TOGGLESINGLEPARAMETERSUGGESTIONS ) );
		item.setSelected( action_TOGGLESINGLEPARAMETERSUGGESTIONS.getState() );
		myMenuSettings.add( item = new JCheckBoxMenuItem( action_TOGGLESINGLECPTSUGGESTIONS ) );
		item.setSelected( action_TOGGLESINGLECPTSUGGESTIONS.getState() );
		bar.add( myMenuTools = new JMenu( "<html><u>T</u>ools" ) );
		myMenuTools.add( action_CODEBANDIT );
		myMenuTools.add( action_BESTWINDOWARRANGEMENT );
		bar.add( cbToggleEvent2 );
		bar.add( cbToggleTableDetails );
		bar.add( myMenuAdopt = new JMenu( "<html><u>E</u>dit CPT" ) );
		myMenuAdopt.add( action_EDITCPT );
		myMenuAdopt.add( action_ADOPTCHANGE );
		myMenuAdopt.setToolTipText( "(disabled in read-only mode)" );
		bar.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		//bar.setBorderPainted( false );
		Dimension mbPreferredSize = bar.getPreferredSize();
		mbPreferredSize.width    += 4;
		//bar.setPreferredSize( mbPreferredSize );

		JPanel    pnlButtons = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
	  //c.gridx     = GridBagConstraints.RELATIVE;

		c.insets    = new Insets( 4,8,4,0 );
		pnlButtons.add( btnStart, c );

		c.weightx   = 1;
		c.insets    = new Insets( 4,0,4,0 );
		pnlButtons.add( Box.createHorizontalStrut( FLAG_CRAM ? 4 : 0x10 ), c );

		c.weightx   = 0;
		c.insets    = new Insets( 4,0,4,2 );
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlButtons.add( bar, c );

		pnlButtons.setBackground( COLOR_BUTTON_PANEL );
		return pnlButtons;
	}

	/** @since 20020522 */
	public void reInitialize()
	{
		se.setInferenceEngine( hnInternalFrame.getInferenceEngine() );

		setEnabled( true );
		mySensitivitySuggestionTable = null;
		//mainPanel.removeAll();mainPanel.invalidate();
		clear();
		setEditActionsEnabled( false );

		//Set of FiniteVariables
		Set selectedNodes = hnInternalFrame.getNetworkDisplay().getSelectedNodes( null );
		DisplayableFiniteVariable varSelected1 = null;
		DisplayableFiniteVariable varSelected2 = null;
		Iterator it = selectedNodes.iterator();
		if( it.hasNext() ) varSelected1 = (DisplayableFiniteVariable)it.next();
		if( it.hasNext() ) varSelected2 = (DisplayableFiniteVariable)it.next();
		yPanel.setSelectedVariable( varSelected1 );
		zPanel.setSelectedVariable( varSelected2 == null ? varSelected1 : varSelected2 );

		yPanel.recalculateWidth();
		zPanel.assumeSize( yPanel );

		/*	BEGIN Workarounds for jdk1.4.0 bug	*/

		//(1) doesn't work
		//constField.grabFocus();
		//(2) doesn't work
		//constField.requestFocus();
		//(3) doesn't work
		//itemStateChanged( new ItemEvent( cbArithmeticOperators, (int)0, cbArithmeticOperators.getSelectedItem(), (int)0) );

		/*	END Workarounds for jdk1.4.0 bug	*/

		maximumWindowArrangement();
	}

	/** @since 20040603 */
	public void setMaximumSafe( boolean flag )
	{
		try{
			setMaximum( flag );
		}catch( java.beans.PropertyVetoException e ){
			System.err.println( "Warning: SensitivityInternalFrame failed setMaximum()." );
		}
	}

	/** @since 20050922 */
	public void maximumWindowArrangement(){
		Rectangle maxBounds = (Rectangle)null;
		try{
			Dimension sizeDesktop = hnInternalFrame.getRightHandDesktopPane().getSize();
			maxBounds = new Rectangle( new Point(0,0), sizeDesktop );

			this.setMaximum( false );
			this.setBounds( maxBounds );
			this.setSelected( true );
		}catch( Exception exception ){
			System.err.println( "Warning: SensitivityInternalFrame.maximumWindowArrangement() failed, caught " + exception );
			//exception.printStackTrace();
			return;
		}
	}

	/**
		<p>
		Overrides JInternalFrame method as a no-op.
		<p>
		A workaround for bug B0005 (JRE 1.4.0) - the constraint decimal field
		otherwise becomes uneditable.
		<p>
		Only seems necessary when:
		(a) reInitialize() calls setMaximum( true ),
		(b) network display was maximized.

		@author Keith Cascio
		@since 073002
	*/
	public void restoreSubcomponentFocus(){}

	/**
		@author Keith Cascio
		@since 081402
	*/
	/*
	public void setVisible( boolean flag )
	{
		super.setVisible( flag );
		if( yPanel != null ) yPanel.debugValueBoxSize();
	}*/

	/**
		@author Keith Cascio
		@since 081902
	*/
	public void updateUI()
	{
		super.updateUI();
		if( pnlEvent2 != null ) SwingUtilities.updateComponentTreeUI( pnlEvent2 );
	}

	/** @since 20020507 */
	private JComponent makeControlPanel()
	{
		yPanel = new ChooseInstancePanel2( hnInternalFrame, "Event", null, null );
		yPanel.setBorder( DEBUG_BORDERS ? BorderFactory.createLineBorder( Color.green, 1 ) : BorderFactory.createEmptyBorder( 1,0,0,8 ) );

		cbArithmeticOperators = new JComboBox( SensitivityEngine.ARITHMETIC_OPERATORS );
		cbArithmeticOperators.addItemListener( this );

		cbComparisonOperators = new JComboBox( SensitivityEngine.COMPARISON_OPERATORS );
		Dimension cbcoPreferredSize = cbComparisonOperators.getPreferredSize();
		cbcoPreferredSize.width    += 4;
		cbComparisonOperators.setPreferredSize( cbcoPreferredSize );

		zPanel = new ChooseInstancePanel2( hnInternalFrame, "Event 2", null, null );
		if( DEBUG_BORDERS ) zPanel.setBorder( BorderFactory.createLineBorder( Color.green, 1 ) );//debug

		pnlEvent2PlaceHolder = new JPanel();
		FlowLayout fl = (FlowLayout) pnlEvent2PlaceHolder.getLayout();
		fl.setVgap( 0 );
		fl.setHgap( 0 );

		if( DEBUG_BORDERS ) pnlEvent2PlaceHolder.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );//debug
		pnlEvent2 = new JPanel( new GridBagLayout() );
		if( DEBUG_BORDERS ) pnlEvent2.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );//debug

		GridBagConstraints cEvent2 = new GridBagConstraints();
		cEvent2.gridx = 0;
		cEvent2.gridy = 0;
		cEvent2.insets = new Insets(0,8,0,8);

		cEvent2.anchor = GridBagConstraints.CENTER;
		//cEvent2.weighty = (double)1;
		Component strutCombo = Box.createVerticalStrut( 32 );
		pnlEvent2.add( strutCombo, cEvent2 );

		cEvent2.gridx = 1;
		cEvent2.weighty = (double)0;
		cEvent2.anchor = GridBagConstraints.NORTH;
		cEvent2.gridheight = 2;
		pnlEvent2.add( zPanel, cEvent2 );

		cEvent2.gridy++;
		cEvent2.gridx = 0;
		cEvent2.anchor = GridBagConstraints.NORTH;
		pnlEvent2.add( cbArithmeticOperators, cEvent2 );

		//ctrlPanel.add(zPanel);

		constField = new DecimalField((double)0, 5);

		/*	Workarounds for jdk1.4.0 bug	*/

		//(1) doesn't work
		//constField.setEnabled( false );

		//(2) doesn't work
		//constField.setEnabled( false );
		//constField.setEnabled( true );

		//(3) doesn't work
		//constField.grabFocus();

		//(4) doesn't work
		//constField.setBackground( Color.red );
		//constField.setBackground( Color.white );

		/*	Workarounds for jdk1.4.0 bug	*/

		JPanel pnlConstraint = new JPanel( new GridLayout(2, 0) );
		if( DEBUG_BORDERS ) pnlConstraint.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );//debug
		else pnlConstraint.setBorder( BorderFactory.createEmptyBorder( 2,8,0,0 ) );//debug

		JLabel lblConstraint = new JLabel( "Constraint" );
		lblConstraint.setFont( lblConstraint.getFont().deriveFont( Font.BOLD ) );
		pnlConstraint.add( lblConstraint );
		//pnlConstraint.add( cbArithmeticOperators );

		JPanel pnlConst = new JPanel( null );
		pnlConst.setLayout( new BoxLayout( pnlConst, BoxLayout.X_AXIS ) );

		//pnlConst.add( new JLabel( " >= " ) );
		pnlConst.add( cbComparisonOperators );
		pnlConst.add( constField );

		pnlConstraint.add( pnlConst );

		JPanel ctrlPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = GridBagConstraints.RELATIVE;
		c.anchor = GridBagConstraints.NORTH;

		/*
		Component strutOuter1 = Box.createVerticalStrut(2);
		Component strutInner = Box.createVerticalStrut(0);
		Component strutOuter2 = Box.createVerticalStrut(2);

		gridbag.setConstraints( strutOuter1, c );
		ctrlPanel.add( strutOuter1 );
		gridbag.setConstraints( strutInner, c );
		ctrlPanel.add( strutInner );
		gridbag.setConstraints( strutOuter2, c );
		ctrlPanel.add( strutOuter2 );

		c.gridy++;
		*/

		ctrlPanel.add( yPanel,               c );
		ctrlPanel.add( pnlEvent2PlaceHolder, c );
		ctrlPanel.add( pnlConstraint,        c );

		if( !   FLAG_CRAM ){
			Dimension size = ctrlPanel.getPreferredSize();
			size.height += 0x10;//35;
			//Doing this messes things up for scrolling and resizing.
			//Don't do it.
			//ctrlPanel.setPreferredSize( size );
			ctrlPanel.setMinimumSize( size );
		}

		if( !   FLAG_CRAM ){ ctrlPanel.setBorder( BorderFactory.createEmptyBorder( 0x10,0x10,0x10,0x10 ) ); }
		if( DEBUG_BORDERS ){
			Border linebo = BorderFactory.createLineBorder( Color.blue, 1 );
			ctrlPanel.setBorder( ctrlPanel.getBorder() == null ? linebo : BorderFactory.createCompoundBorder( ctrlPanel.getBorder(), linebo ) );
		}

		return ctrlPanel;
	}

	public void itemStateChanged(ItemEvent event)
	{
		Object item = event.getItem();
		if( item == SensitivityEngine.DIFFERENCE )
		{
			constField.setText( "0.0" );
		}
		else if( item == SensitivityEngine.RATIO )
		{
			constField.setText( "1.0" );
		}
	}

	/** @since 060304 */
	public double parseConstField()
	{
		double c = (double)-32;
		try {
			c = constField.getValue();
		}catch( Exception exception ){
			hnInternalFrame.getParentFrame().showErrorDialog( "Invalid constant parameter." );
			return (double)-64;
		}
		return c;
	}

	/** @since 070705 */
	private JComponent getBlankComponent(){
		if( myComponentBlank == null ) myComponentBlank = new JPanel();
		return myComponentBlank;
	}

	/** @since 070705 */
	private JComponent getImpossibleComponent(){
		if( myComponentImpossible == null ){
			JLabel lblImpossible = getErrorMessageLabel( STR_MSG_IMPOSSIBLE );
			JPanel pnlImpossible = new JPanel( new GridBagLayout() );
			pnlImpossible.add( lblImpossible, new GridBagConstraints() );

			myComponentImpossible = pnlImpossible;
		}
		return myComponentImpossible;
	}

	/**
		pulled from actionPerformed()
		@since 120403
	*/
	public void doStartSensitivity()
	{
		InferenceEngine ie = hnInternalFrame.getInferenceEngine();
		UI ui = hnInternalFrame.getParentFrame();
		PartialDerivativeEngine pde = hnInternalFrame.getPartialDerivativeEngine();

		if( pde == null )
		{
			showSensitivityResultsMessage( STR_MSG_ERROR_PARTIALS, (StatusBar)null, ui );
			return;
		}

		double c = parseConstField();
		if( c < 0 ) return;

		this.setStartEnabled(false);
		mainPanel.removeAll();
		mainPanel.invalidate();

		DisplayableFiniteVariable dvarY = yPanel.getVariable();
		Object valueY = yPanel.getInstance();
		DisplayableFiniteVariable dvarZ = zPanel.getVariable();
		Object valueZ = zPanel.getInstance();

		if (dvarY == null || valueY == null || dvarZ == null ||
			valueZ == null) {
			mainPanel.add( getBlankComponent(), BorderLayout.CENTER );
			mainPanel.validate();
			this.setStartEnabled(true);
			return;
		}

		setEditActionsEnabled( false );//Keith Cascio 082002
		mySensitivitySuggestionTable = null;//Keith Cascio 082002

		myTableDetailsVisible = false;

		//FiniteVariable dvarY = dvarY.getFiniteVariable();
		//FiniteVariable dvarZ = dvarZ.getFiniteVariable();
		Object opArithmetic = cbArithmeticOperators.getSelectedItem();
		Object opComparison = cbComparisonOperators.getSelectedItem();
		//double c = Double.parseDouble(constField.getText());

		StatusBar bar = ui.getStatusBar();
		if( bar != null ) bar.pushText( STR_MSG_SENSITIVITY, StatusBar.WEST );
		ui.setWaitCursor();
		SensitivityReport sr = null;

		boolean flagSingleParameter = action_TOGGLESINGLEPARAMETERSUGGESTIONS.getState();
		boolean flagSingleCPT = action_TOGGLESINGLECPTSUGGESTIONS.getState();

		if( pnlEvent2PlaceHolder.isAncestorOf( pnlEvent2 ) )//2-event constraint
		{
/* Hei 051702: Consider case that dvarY == dvarZ in SensitivityEngine;
	don't delete!
			if (dvarY == dvarZ)
				sr = se.getResults(dvarY, valueY,
					valueZ, opArithmetic,
					opComparison, c);
*/
			sr = se.getResults(dvarY, valueY,
				dvarZ, valueZ, opArithmetic,
				opComparison, c, flagSingleParameter, flagSingleCPT );
		}
		else sr =  se.getResults(dvarY, valueY, null,
			null, null, opComparison, c, flagSingleParameter, flagSingleCPT );

		if (sr == null)
		{
			showSensitivityResultsMessage( STR_MSG_SATISFIED, bar, ui );
			return;
		}

		java.util.List suggestions = sr.generateSingleParamSuggestions();
		Map singleCPTSuggestions = sr.getSingleCPTMap();

		boolean flagSingleParamSuggestions = !suggestions.isEmpty();
		boolean flagSingleCPTSuggestions = !singleCPTSuggestions.isEmpty();

		if( !(flagSingleParamSuggestions || flagSingleCPTSuggestions) )
		{
			showSensitivityResultsMessage( STR_MSG_UNSATISFIABLE, bar, ui );
			return;
		}

		if( flagSingleParamSuggestions )
		{
			initTable( suggestions );
			myPainSingleParam = new JScrollPane( mySensitivitySuggestionTable );
			myPainSingleParam.setPreferredSize(new Dimension(550, 250));
			mySensitivitySuggestionTable.setJScrollPane( myPainSingleParam );
		}
		else myPainSingleParam = null;
		if( flagSingleCPTSuggestions ) initSingleCPTPanel( singleCPTSuggestions );
		else myPanelSingleCPT = null;

		ui.setDefaultCursor();
		if( bar != null ) bar.popText( STR_MSG_SENSITIVITY, StatusBar.WEST );
		this.setStartEnabled(true);

		myResultsTabbed = new JTabbedPane();
		if( flagSingleParamSuggestions ) myResultsTabbed.addTab( "Single parameter suggestions", myPainSingleParam );
		if( flagSingleCPTSuggestions ) myResultsTabbed.addTab( "Multiple parameter suggestions (single CPT)", myPanelSingleCPT );
		myResultsTabbed.addChangeListener( this );

		mainPanel.add( myResultsTabbed, BorderLayout.CENTER );
		mainPanel.validate();

		if( flagSingleParamSuggestions ) mySensitivitySuggestionTable.prettySize();
	}

	/** @since 021505 */
	public SensitivitySuggestionTable getSensitivitySuggestionTable(){
		return mySensitivitySuggestionTable;
	}

	/** @since 021505 */
	public SingleCPTSuggestionPanel getSingleCPTSuggestionPanel(){
		return myPanelSingleCPT;
	}

	/** @since 100303 */
	public void doAdoptChange()
	{
		if( myResultsTabbed != null )
		{
			Component selected = myResultsTabbed.getSelectedComponent();
			SensitivitySuggestion ss = null;

			if( selected == myPainSingleParam ) ss = mySensitivitySuggestionTable.getCurrentlySelectedSuggestion();
			else if( selected == myPanelSingleCPT ) ss = myPanelSingleCPT.getCurrentlySelectedSuggestion();

			if( ss == null ) return;
			else
			{
				setEditActionsEnabled( false );
				try {
					ss.adoptChange();
				}catch( Exception e ){
					hnInternalFrame.getParentFrame().showErrorDialog(e.getMessage());
				}

				if( selected == myPainSingleParam && mySensitivitySuggestionTable != null ) mySensitivitySuggestionTable.flagValid = false;
				if( selected == myPanelSingleCPT && myPanelSingleCPT != null ) myPanelSingleCPT.flagValid = false;

				hnInternalFrame.setCPT( ss.getVariable() );
			}
			//hnInternalFrame.getParentFrame().showErrorDialog( "WARNING: Single CPT adopot change not yet implemented." );
		}
	}

	/**
		@author Keith Cascio
		@since 100303
	*/
	public DisplayableFiniteVariable getVariableForSelectedSuggestion()
	{
		if( myResultsTabbed != null )
		{
			Component selected = myResultsTabbed.getSelectedComponent();
			if( selected == myPainSingleParam )
			{
				SensitivitySuggestion ss = mySensitivitySuggestionTable.getCurrentlySelectedSuggestion();
				if( ss == null ) return null;
				else return (DisplayableFiniteVariable)
					((SingleParamSuggestion)ss).getCPTParameter().getVariable();
			}
			else if( selected == myPanelSingleCPT )
			{
				return myPanelSingleCPT.getCurrentlySelectedVariable();
			}
		}

		return null;
	}

	/** @since 20020619 */
	public void setEnabled( boolean enabled )
	{
		yPanel.setEnabled( enabled );
		zPanel.setEnabled( enabled );
		cbArithmeticOperators.setEnabled( enabled );
		cbComparisonOperators.setEnabled( enabled );
		constField.setEnabled( enabled );
		this.setStartEnabled( enabled );
		action_TOGGLEEVENT2.setEnabled( enabled );
	}

	/** @since 20051018 */
	private void setStartEnabled(){
		this.setStartEnabled( this.myMaskStartEnabled, (SamiamUserMode)null );
	}

	/** @since 20051018 */
	private void setStartEnabled( SamiamUserMode mode ){
		this.setStartEnabled( this.myMaskStartEnabled, mode );
	}

	/** @since 20051018 */
	private void setStartEnabled( boolean enabled ){
		this.setStartEnabled( enabled, (SamiamUserMode)null );
	}

	/** @since 20051018 */
	private void setStartEnabled( boolean enabled, SamiamUserMode mode ){
		this.myMaskStartEnabled = enabled;
		NetworkInternalFrame nif = this.hnInternalFrame;
		if( (nif != null) && (mode == null) ) mode = nif.getSamiamUserMode();
		boolean flagOverrideStart = (nif != null) && action_START.decideEnabled( mode, nif );
		action_START.setEnabled( this.myMaskStartEnabled && flagOverrideStart );
	}

	/** @since 20020619 */
	protected void forceEditMode()
	{
		setEnabled( false );
		if( !hnInternalFrame.getSamiamUserMode().contains( SamiamUserMode.EDIT ) )
		{
			hnInternalFrame.getParentFrame().toggleSamiamUserMode();
			setVisible( true );
		}
	}

	public static final Color COLOR_RESULTS_MESSAGE = new Color( 128,0,0 );
	public static final float SIZE_FONT_RESULTS_MESSAGE = (float)14;

	/** @since 20030225 */
	public static JLabel getErrorMessageLabel( String text )
	{
		return getErrorMessageLabel( text, SIZE_FONT_RESULTS_MESSAGE );
	}

	/** @since 20030225 */
	public static JLabel getErrorMessageLabel( String text, float fontSize )
	{
		JLabel lblUnsatisfiable = new JLabel( text );

		lblUnsatisfiable.setVerticalAlignment( JLabel.CENTER );
		lblUnsatisfiable.setVerticalTextPosition( JLabel.CENTER );

		Font fontMessage = lblUnsatisfiable.getFont();
		fontMessage = fontMessage.deriveFont( fontSize );
		fontMessage = fontMessage.deriveFont( Font.BOLD );
		lblUnsatisfiable.setFont( fontMessage );
		lblUnsatisfiable.setForeground( COLOR_RESULTS_MESSAGE );

		return lblUnsatisfiable;
	}

	/** @since 20020610 */
	protected void showSensitivityResultsMessage( String msg, StatusBar bar, UI ui )
	{
		JPanel pnlUnsatisfiable = new JPanel( new GridBagLayout() );
		JLabel lblUnsatisfiable = getErrorMessageLabel( msg );

		if( DEBUG_BORDERS )
		{
			pnlUnsatisfiable.setBorder( BorderFactory.createLineBorder( Color.yellow, 1 ) );
			lblUnsatisfiable.setBorder( BorderFactory.createLineBorder( Color.green, 1 ) );
		}

		GridBagConstraints c = new GridBagConstraints();
		pnlUnsatisfiable.add( lblUnsatisfiable, c );
		//*/

		mainPanel.add( pnlUnsatisfiable, BorderLayout.CENTER );
		mainPanel.validate();
		this.setStartEnabled(true);

		if( bar != null ) bar.popText( STR_MSG_SENSITIVITY, StatusBar.WEST );
		if( ui != null ) ui.setDefaultCursor();

		return;
	}

	/** @since 20030929 */
	private void initSingleCPTPanel( Map singleCPTSuggestions )
	{
		if( myPanelSingleCPT == null ) myPanelSingleCPT = new SingleCPTSuggestionPanel();
		myPanelSingleCPT.setSuggestions( singleCPTSuggestions );
		myPanelSingleCPT.getListTable().getSelectionModel().addListSelectionListener( this );
	}

	/** @since 20020603 */
	private void initTable( SensitivityReport sr )
	{
		//mySensitivitySuggestionTable = new SensitivitySuggestionTable( hnInternalFrame, pkgDspOpt, seResults, true );
		mySensitivitySuggestionTable = new
			SensitivitySuggestionTable( hnInternalFrame,
			myGlobalPrefs, sr, true );
		initTable();
	}

	/** @since 20020610 */
	private void initTable( java.util.List suggestions )
	{
		//mySensitivitySuggestionTable = new SensitivitySuggestionTable( hnInternalFrame, pkgDspOpt, suggestions, true );
		mySensitivitySuggestionTable = new
			SensitivitySuggestionTable( hnInternalFrame,
			myGlobalPrefs, suggestions, true );
		initTable();
	}

	/** @since 20020610 */
	private void initTable(){
		//mySensitivitySuggestionTable.getTableHeader().addMouseListener( this );
		mySensitivitySuggestionTable.addTableModelListener( this );
		mySensitivitySuggestionTable.getSelectionModel().addListSelectionListener( this );
		tableModelChanged( mySensitivitySuggestionTable );

		myTableDetailsVisible = false;
		//System.out.println( "initTable() -> setTableDetailsVisible()" );
		setTableDetailsVisible();

		setEditActionsEnabled( false );
	}

	/**
		interface ListSelectionListener
		@author Keith Cascio
		@since 060702
	*/
	public void valueChanged(ListSelectionEvent e)
	{
		setEditActionsEnabled();
	}

	/**
		interface ChangeListener
		@author Keith Cascio
		@since 100303
	*/
	public void stateChanged( ChangeEvent e )
	{
		setEditActionsEnabled();
	}

	/** @since 100303 */
	private void setEditActionsEnabled()
	{
		if( myResultsTabbed == null ) setEditActionsEnabled( false );
		else
		{
			Component selected = myResultsTabbed.getSelectedComponent();
			if( selected == null ) return;
			else if( selected == myPainSingleParam ){
				setEditActionsEnabled( mySensitivitySuggestionTable.getSelectedRow() != (int)-1 && mySensitivitySuggestionTable.flagValid );
			}
			else if( selected == myPanelSingleCPT ){
				setEditActionsEnabled( myPanelSingleCPT.getListTable().getSelectedRow() != (int)-1 && myPanelSingleCPT.flagValid );
			}
		}
	}

	/** @since 20020607 */
	private void setEditActionsEnabled( boolean enabled )
	{
		//btnEdit.setEnabled( enabled );
		//btnAdoptChange.setEnabled( enabled );
		////action_ADOPTCHANGE.setEnabled( enabled );
		////action_EDITCPT.setEnabled( enabled );
		if( myMenuAdopt != null ) myMenuAdopt.setEnabled( enabled );
	}

	/** @since 20020603 */
	public void tableModelChanged( SensitivitySuggestionTable sst ){
		//TableColumnModel TCM = mySensitivitySuggestionTable.getColumnModel();
		//myAbsolutChangeColumn = TCM.getColumn( SensitivitySuggestionTableModel.ABSOLUTE_CHANGE_COLUMN_INDEX );
		//myLogOddsChangeColumn = TCM.getColumn( SensitivitySuggestionTableModel.LOG_ODDS_CHANGE_COLUMN_INDEX );
		//System.out.println( "tableModelChanged() -> setTableDetailsVisible()" );
		setTableDetailsVisible();
	}

	//public void changePackageOptions(PackageOptions.PkgDspOpt pkgDspOpt)
	public void changePackageOptions( PreferenceGroup globalPrefs )
	{
		//this.pkgDspOpt = pkgDspOpt;
		this.myGlobalPrefs = globalPrefs;
	}

	/** interface CPTChangeListener
		@since 081302 */
	public void cptChanged( CPTChangeEvent evt ){
		se.resetEquations();
		clear();
	}

	/** interface NetStructureChangeListener
		@since 081302 */
	public void netStructureChanged( NetStructureEvent evt ) {
		se.resetEquations();
		clear();
	}

	/** interface EvidenceChangeListener
		@since 071003 */
	public void warning( EvidenceChangeEvent ece ) {}

	/** interface EvidenceChangeListener
		@since 082602 */
	public void evidenceChanged( EvidenceChangeEvent ECE ){
		se.resetEquations();
		clear();
	}

	/** @since 081302 */
	protected void clear()
	{
		mainPanel.invalidate();
		mainPanel.removeAll();

		boolean impossible = false;
		ComputationCache cache = null;
		if( this.hnInternalFrame != null ) cache = this.hnInternalFrame.getComputationCache();
		if( cache != null ) impossible = cache.isNaN();

		this.setStartEnabled( !impossible );
		JComponent toAdd = impossible ? getImpossibleComponent() : getBlankComponent();
		mainPanel.add( toAdd, BorderLayout.CENTER );

		mainPanel.validate();
		mainPanel.repaint();

		setEditActionsEnabled( false );
		mySensitivitySuggestionTable = null;
	}

	/*/ @since 20020813 //
	public void mouseClicked( MouseEvent event ){
		mySensitivitySuggestionTable.sort( event );
	}

	public void mouseEntered( MouseEvent event ){}
	public void mouseExited( MouseEvent event ){}
	public void mousePressed( MouseEvent event ){}
	public void mouseReleased( MouseEvent event ){}*/
}
