package edu.ucla.belief.ui.rc;

import edu.ucla.belief.dtree.*;
//import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.NetworkIO;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.displayable.DisplayableBeliefNetwork;
import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.recursiveconditioning.CachePreviewDialog;
import edu.ucla.belief.ui.statusbar.StatusBar;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.File;

/**
	@author Keith Cascio
	@since 110703
*/
public class CacheSettingsPanel extends JPanel implements ActionListener, ChangeListener
{
	public static final int INT_MIN_SLIDER = 0;
	public static final int INT_MAX_SLIDER = 100;
	public static final int INT_TICK_MAJOR = 25;
	public static final int INT_TICK_MINOR = 5;
	public static final String STR_TEXT_SEARCH_BUTTON = "Allocate Caches";//"Allocate Memory";//"Preview";

	public CacheSettingsPanel()
	{
		init();
	}

	protected void init()
	{
		myActionListener = this;
		makeAllocationPanel();
	}

	/**
		Call before set()
	*/
	public void setBeliefNetwork( DisplayableBeliefNetwork bn )
	{
		myBeliefNetwork = bn;
	}

	/**
		Call after setBeliefNetwork()
	*/
	public void set( RCSettings settings )
	{
		mySettings = settings;

		updateMemoryDisplay();
		updateDgraphStatsDisplay();
		updateHeuristic();
		updateEstimatedTime();
		//setFileName( settings.getBundle().getRCFilePath() );
	}

	private void resetSlider( double proportion )
	{
		//System.out.println( "CacheSettingsPanel.resetSlider() " + mySettings.getUserMemoryProportion() );
		myFlagListenSlider = false;
		mySliderUserMemory.setValue( (int)((double)INT_MAX_SLIDER * proportion) );
		myFlagListenSlider = true;
	}

	private void updateEstimatedTime()
	{
		if( mySettings.isStale() )
		{
			if( mySliderUserMemory.getValue() == INT_MAX_SLIDER ) initEstimatedTimeDisplay( mySettings, myLabelEstimatedTime_All );
			else myLabelEstimatedTime_All.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		}
		else updateEstimatedMinutesDisplay( mySettings, myLabelEstimatedTime_All );
	}

	private void updateHeuristic()
	{
		myFlagIgnoreActions = true;
		myComboBoxOrderHeuristic.setSelectedItem( mySettings.getEliminationHeuristic() );
		myFlagIgnoreActions = false;
	}

	private boolean myFlagListenSlider = true;
	private boolean myFlagIgnoreActions = false;

	private JLabel myLabelOptimalMemory;
	private JLabel myLabelUserMemory;
	private JSlider mySliderUserMemory;
	private JLabel myLabelOptimalMemoryUnits;
	private JLabel myLabelUserMemoryUnits;
	private JButton myButtonPreview;
	//private JButton myButtonStopMemoryAllocation;
	private JPanel myPanelMemoryAllocationButtons;
	private JComboBox myComboBoxOrderHeuristic;
	private JLabel myLabelEstimatedCaption_All;
	private JLabel myLabelEstimatedTime_All;

	/** @since 020405 */
	public String getClipboardInfo(){
		StringBuffer buffer = new StringBuffer( 256 );
		buffer.append( "Maximum memory needed: " );
		append( buffer, myLabelOptimalMemory );
		append( buffer, myLabelOptimalMemoryUnits );
		buffer.append( "\nMemory to use? " );
		append( buffer, myLabelUserMemory );
		append( buffer, myLabelUserMemoryUnits );
		buffer.append( "\nElimination order heuristic: " );
		append( buffer, myComboBoxOrderHeuristic );
		buffer.append( "\n" );
		append( buffer, myLabelEstimatedCaption_All );
		buffer.append( " " );
		append( buffer, myLabelEstimatedTime_All );

		buffer.append( "\ndgraph height: " );
		append( buffer, myLabelDgraphHeight );

		buffer.append( "\ndgraph max cluster size: " );
		append( buffer, myLabelDgraphMaxCluster );

		buffer.append( "\ndgraph max cutset size: " );
		append( buffer, myLabelDgraphMaxCutset );

		buffer.append( "\ndgraph max context size: " );
		append( buffer, myLabelDgraphMaxContext );

		buffer.append( "\ndgraph diameter: " );
		append( buffer, myLabelDgraphDiameter );
		return buffer.toString();
	}

	/** @since 020405 */
	public static void append( StringBuffer buffer, JLabel label ){
		if( label != null ) buffer.append( label.getText() );
	}

	/** @since 020405 */
	public static void append( StringBuffer buffer, JComboBox box ){
		if( box != null ) buffer.append( box.getSelectedItem().toString() );
	}

	/** @since 062105 */
	public static void append( StringBuffer buffer, JTextField textfield ){
		if( textfield != null ) buffer.append( textfield.getText() );
	}

	protected JComponent makeAllocationPanel()
	{
		myLabelOptimalMemory = new JLabel( "?", JLabel.RIGHT );
		Dimension dim  = myLabelOptimalMemory.getPreferredSize();
		dim.width = 64;
		myLabelOptimalMemory.setPreferredSize( dim );

		if( FLAG_DEBUG_BORDERS ) myLabelOptimalMemory.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		JLabel lblMemoryCaption = new JLabel( "Maximum memory needed: " );
		if( FLAG_DEBUG_BORDERS ) lblMemoryCaption.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		JLabel lblMemoryQuestion = new JLabel( "Memory to use? " );
		if( FLAG_DEBUG_BORDERS ) lblMemoryQuestion.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		myLabelUserMemory = new JLabel( "?", JLabel.RIGHT );
		if( FLAG_DEBUG_BORDERS ) myLabelUserMemory.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		myLabelOptimalMemoryUnits = new JLabel( " KB", JLabel.LEFT );
		if( FLAG_DEBUG_BORDERS ) myLabelOptimalMemoryUnits.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		myLabelUserMemoryUnits = new JLabel( " KB", JLabel.LEFT );
		if( FLAG_DEBUG_BORDERS ) myLabelUserMemoryUnits.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		mySliderUserMemory = new JSlider( JSlider.HORIZONTAL, INT_MIN_SLIDER, INT_MAX_SLIDER, INT_MAX_SLIDER );
		mySliderUserMemory.setMajorTickSpacing( INT_TICK_MAJOR );
		mySliderUserMemory.setMinorTickSpacing( INT_TICK_MINOR );
		mySliderUserMemory.setPaintTicks(true);
		mySliderUserMemory.addChangeListener( this );

		JLabel lblOrderHeuristic = new JLabel( "Elimination order heuristic: " );
		myComboBoxOrderHeuristic = new JComboBox( EliminationHeuristic.ARRAY );
		myComboBoxOrderHeuristic.addActionListener( myActionListener );
		myComboBoxOrderHeuristic.setPreferredSize( myComboBoxOrderHeuristic.getPreferredSize() );

		myLabelEstimatedCaption_All = new JLabel( "Estimated run time, marginals:", JLabel.LEFT );
		myLabelEstimatedTime_All = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE );

		myButtonPreview = new JButton( STR_TEXT_SEARCH_BUTTON );
		myButtonPreview.addActionListener( myActionListener );

		GridBagConstraints cButtons = new GridBagConstraints();
		cButtons.gridwidth = 1;
		cButtons.weightx = 0;
		cButtons.fill = GridBagConstraints.NONE;
		myPanelMemoryAllocationButtons = new JPanel( new GridBagLayout() );
		myPanelMemoryAllocationButtons.add( myButtonPreview, cButtons );
		cButtons.weightx = 1;
		cButtons.fill = GridBagConstraints.HORIZONTAL;
		cButtons.gridwidth = GridBagConstraints.REMAINDER;
		myPanelMemoryAllocationButtons.add( Box.createHorizontalStrut( 1 ), cButtons );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = this;
		ret.setLayout( gridbag );

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

		c.ipadx = c.ipady = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		//ret.add( myCBuseKB, c );
		ret.add( lblOrderHeuristic, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.NONE;
		ret.add( myComboBoxOrderHeuristic, c );

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.ipady = 0;
		ret.add( Box.createVerticalStrut( 16 ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipady = (int)0;
		c.weightx = 1;
		ret.add( myPanelMemoryAllocationButtons, c );
		c.ipady = (int)16;

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.ipady = 0;
		ret.add( Box.createVerticalStrut( 16 ), c );

		c.gridwidth = 1;
		ret.add( myLabelEstimatedCaption_All, c );

		ret.add( Box.createHorizontalStrut( 8 ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelEstimatedTime_All, c );

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.ipady = 0;
		ret.add( Box.createVerticalStrut( 16 ), c );

		c.weightx = 1;
		ret.add( makeDgraphStatsPanel(), c );

		ret.setBorder( BorderFactory.createEmptyBorder( 8,8,8,8 ) );

		return ret;
	}

	private JLabel myLabelDgraphDiameter;
	private JLabel myLabelDgraphHeight;
	private JLabel myLabelDgraphMaxCluster;
	private JLabel myLabelDgraphMaxCutset;
	private JLabel myLabelDgraphMaxContext;

	private void updateDgraphStatsDisplay()
	{
		RCInfo info = null;
		if( (!validateRCSafe( (String)null )) || ((info = mySettings.getInfo()) ==  null) ){
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
		myLabelDgraphDiameter.setText( String.valueOf( info.diameter() ) );
	}

	private void clearDgraphStatsDisplay()
	{
		myLabelDgraphHeight.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxCluster.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxCutset.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxContext.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphDiameter.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
	}

	private JComponent makeDgraphStatsPanel()
	{
		JLabel lblDiameterCaption = new JLabel( "diameter = ", JLabel.LEFT );
		JLabel lblDtreeStatsCaption = new JLabel( "height = ", JLabel.LEFT );
		JLabel lblMaxCaption = new JLabel( " max ", JLabel.RIGHT );
		JLabel lblClusterCaption = new JLabel( "cluster = ", JLabel.LEFT );
		JLabel lblCutsetCaption = new JLabel( "cutset = ", JLabel.LEFT );
		JLabel lblContextCaption = new JLabel( "context = ", JLabel.LEFT );
		myLabelDgraphHeight = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		Dimension dim = myLabelDgraphHeight.getPreferredSize();
		dim.width = 16;
		myLabelDgraphHeight.setMinimumSize( dim );
		myLabelDgraphMaxCluster = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxCluster.setMinimumSize( dim );
		myLabelDgraphMaxCutset = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxCutset.setMinimumSize( dim );
		myLabelDgraphMaxContext = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphMaxContext.setMinimumSize( dim );
		myLabelDgraphDiameter = new JLabel( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
		myLabelDgraphDiameter.setMinimumSize( dim );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );

		c.gridwidth =(int)1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		ret.add( lblDtreeStatsCaption, c );

		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( myLabelDgraphHeight, c );

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		ret.add( lblMaxCaption, c );

		ret.add( lblClusterCaption, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( myLabelDgraphMaxCluster, c );

		c.gridwidth =(int)1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		ret.add( lblDiameterCaption, c );

		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( myLabelDgraphDiameter, c );

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		ret.add( Box.createHorizontalStrut(1), c );

		ret.add( lblCutsetCaption, c );

		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelDgraphMaxCutset, c );

		c.gridwidth =(int)1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		ret.add( Box.createHorizontalStrut(1), c );
		ret.add( Box.createHorizontalStrut(1), c );
		ret.add( Box.createHorizontalStrut(1), c );
		ret.add( lblContextCaption, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( myLabelDgraphMaxContext, c );

		Border b = null;
		if( FLAG_DEBUG_BORDERS ) b = BorderFactory.createLineBorder( Color.red, (int)1 );
		else b = BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Dgraph properties" );

		ret.setBorder( b );

		return ret;
	}

	public void stateChanged( ChangeEvent e )
	{
		Object src = e.getSource();
		if( src == mySliderUserMemory && myFlagListenSlider && mySettings != null )
		{
			double newMemoryProportion = (double)mySliderUserMemory.getValue()/(double)INT_MAX_SLIDER;

			//myLabelUserMemory.setText( mySettings.updateUserMemoryDisplay( mySettings.getBundle().getAll(), newMemoryProportion ) );
			myLabelUserMemory.setText( mySettings.updateUserMemoryDisplay( newMemoryProportion ) );

			if( !mySliderUserMemory.getValueIsAdjusting() )
			{
				if( mySettings.setUserMemoryProportion( newMemoryProportion ) )
				{
					updateUserMemoryDisplay( newMemoryProportion );
					updateEstimatedTime();
				}
			}
		}
	}

	public void updateMemoryDisplay()
	{
		if( validateRCSafe( "Failed to Create DGraph" ) )
		{
			double proportion = ( mySettings.isStale() ) ? mySettings.getUserMemoryProportion() : mySettings.getActualMemoryProportion();
			resetSlider( proportion );
			updateOptimalMemoryDisplay( proportion );
		}
	}

	/** @since 042005 */
	private boolean validateRCSafe( String errorContext ){
		if( mySettings == null ) return false;
		boolean valid = false;
		try{
			valid = mySettings.validateRC( myBeliefNetwork );
		}catch( Throwable throwable ){
			valid = false;
			if( errorContext == null ) return false;
			String msqThrowable = throwable.getMessage();
			if( msqThrowable == null ) msqThrowable = throwable.toString();
			showErrorMessage( errorContext + "\n" + msqThrowable, errorContext );
		}
		return valid;
	}

	private void updateOptimalMemoryDisplay( double proportion )
	{
		String[] newVals = mySettings.updateOptimalMemoryDisplay();

		if( (myMemoryUnit != newVals[1]) && (newVals[1] != null) )
		{
			myMemoryUnit = newVals[1];
			updateMemoryUnitsDisplay();
		}

		myLabelOptimalMemory.setText( newVals[0] );
		updateUserMemoryDisplay( proportion );
	}

	private void updateUserMemoryDisplay( double proportion )
	{
		myLabelUserMemory.setText( mySettings.updateUserMemoryDisplay( proportion ) );
	}

	private String myMemoryUnit = RCSettings.STR_KILOBYTE_UNIT;
	private Font myFontNormal;
	private Font myFontBold;

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

	public void actionPerformed( ActionEvent evt )
	{
		if( myFlagIgnoreActions ) return;

		Object src = evt.getSource();

		if( src == myButtonPreview ) doPreview();
		if( src == myComboBoxOrderHeuristic ) doHeuristic();
	}

	private void showErrorMessage( String message, String title )
	{
		JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
	}

	/** @since 041405 */
	private void doHeuristic(){
		doHeuristicImproved();
	}

	/** @since 041405 */
	private void doHeuristicImproved()
	{
		EliminationHeuristic current = mySettings.getEliminationHeuristic();
		EliminationHeuristic selected = (EliminationHeuristic) myComboBoxOrderHeuristic.getSelectedItem();

		if( current == selected ) return;

		boolean success = false;
		String msgThrowable = "";
		try{
			success = mySettings.setHeuristicAndValidateOrRollback( selected, myBeliefNetwork );
		}catch( Throwable throwable ){
			msgThrowable = throwable.getMessage();
			if( msgThrowable == null ) msgThrowable = throwable.toString();
		}

		if( success ){
			updateDgraphStatsDisplay();
			updateMemoryDisplay();
			updateEstimatedTime();
		}
		else{
			if( msgThrowable.length() < 1 ) msgThrowable = "Failed to set heuristic " + selected.toString();
			showErrorMessage( msgThrowable, "Error setting heuristic \"" + selected.toString() + "\"" );
			updateHeuristic();
		}
	}

	/** @since 041405 */
	private void doHeuristicOnhold()
	{
		EliminationHeuristic current = mySettings.getEliminationHeuristic();
		EliminationHeuristic selected = (EliminationHeuristic) myComboBoxOrderHeuristic.getSelectedItem();

		if( current == selected ) return;

		mySettings.setEliminationHeuristic( selected );

		String errorContext = "Error setting heuristic " + selected.toString();

		if( validateRCSafe( errorContext ) ) mySettings.fireSettingChanged();
		else{
			showErrorMessage( "Failed to set heuristic " + selected.toString(), errorContext );
			mySettings.setEliminationHeuristic( current );
			updateHeuristic();
		}

		updateDgraphStatsDisplay();
		updateMemoryDisplay();
		updateEstimatedTime();
	}

	private void doPreview()
	{
		edu.ucla.belief.ui.util.Util.pushStatusWest( myBeliefNetwork.getNetworkInternalFrame(), STR_MSG_ALLOCATE );
		setCursor( UI.CURSOR_WAIT_RC );

		boolean flagSearchNeeded = false, flagAllocationValid = false, flagPerformedSearch = false;
		String msgThrowable = null;
		if( mySettings != null && myBeliefNetwork != null )
		{
			flagSearchNeeded = (mySettings.getInfo() == null) || mySettings.isStale();

			try{
				flagAllocationValid = mySettings.validateAllocation( myBeliefNetwork );
				updateEstimatedMinutesDisplay( mySettings, myLabelEstimatedTime_All );
			}catch( Throwable throwable ){
				flagAllocationValid = false;
				msgThrowable = throwable.getMessage();
				if( msgThrowable == null ) msgThrowable = throwable.toString();
			}

			flagPerformedSearch = flagSearchNeeded && flagAllocationValid;
		}

		if( flagPerformedSearch )
		{
			double proportion = mySettings.synchronizeMemoryProportion();
			updateUserMemoryDisplay( proportion );
			resetSlider( proportion );
		}

		if( !flagAllocationValid ){
			String msgUser = "Failed to allocate caches.";
			if( msgThrowable != null ) msgUser += "\n" + msgThrowable;
			showErrorMessage( msgUser, STR_TEXT_SEARCH_BUTTON + " Failed" );
		}

		setCursor( UI.CURSOR_DEFAULT );
		edu.ucla.belief.ui.util.Util.popStatusWest( myBeliefNetwork.getNetworkInternalFrame(), STR_MSG_ALLOCATE );
	}

	public static final String STR_MSG_ALLOCATE = " recursive conditioning allocating caches...";

	private void initEstimatedTimeDisplay( RCSettings settings, JLabel label )
	{
		if( validateRCSafe( (String)null ) ){
			String[] newVals = settings.updateEstimatedTimeDisplay( settings.getInfo().recursiveCallsFullCaching() );
			label.setText( newVals[0] + " " + newVals[1] );
		}
		else label.setText( CachePreviewDialog.STR_TEXT_UNAVAILABLE );
	}

	private void updateEstimatedMinutesDisplay( RCSettings settings, JLabel label )
	{
		String[] newVals = settings.updateEstimatedMinutesDisplay();
		label.setText( newVals[0] + " " + newVals[1] );
	}

	public void setFileName( File openFile )
	{
		if( openFile == null ) setFileName( edu.ucla.belief.ui.recursiveconditioning.CacheSettingsPanel.STR_NO_FILE );
		else setFileName( edu.ucla.belief.ui.recursiveconditioning.CacheSettingsPanel.STR_OPEN_FILE + openFile.getPath() );
	}

	public void setFileName( String fname )
	{
		setToolTipText( fname );
	}

	public void setDefaultDirectory( File defaultDirectory )
	{
		myDefaultDirectory = defaultDirectory;
	}

	public void setNetworkName( String networkName )
	{
		myNetworkName = networkName;
	}

	/**
		test/debug
	*/
	public static void main( String[] args )
	{
		FLAG_TEST = true;

		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		CacheSettingsPanel CSP = new CacheSettingsPanel();

		JFrame frame = new JFrame( "DEBUG Cache Settings" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( CSP );
		frame.pack();
		frame.setVisible( true );
	}

	protected RCSettings mySettings;
	protected ActionListener myActionListener;
	protected DisplayableBeliefNetwork myBeliefNetwork;
	protected File myDefaultDirectory;
	protected File myLastDefaultDirectory;
	protected String myNetworkName;

	public static boolean FLAG_DEBUG_BORDERS = false;
	public static boolean FLAG_TEST = false;
}
