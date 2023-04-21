package edu.ucla.belief.ui.recursiveconditioning;

import edu.ucla.util.*;

import edu.ucla.belief.dtree.*;
import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.InflibFileFilter;

import edu.ucla.belief.ui.util.WholeNumberField;
import edu.ucla.belief.ui.util.NotifyField;
import edu.ucla.belief.ui.util.Util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.File;
import javax.swing.filechooser.FileFilter;

/** @author keith cascio
	@since  20030207 */
public class DtreeSettingsPanel extends JPanel implements
  ActionListener,
  ChangeListener
{
	public DtreeSettingsPanel()
	{
		init();
	}

	protected void init()
	{
		myActionListener = this;
		makeSettingsPanel();
	}

	public void setBeliefNetwork( BeliefNetwork bn )
	{
		myBeliefNetwork = bn;
		myButtonPreview.setEnabled( myBeliefNetwork != null );
	}

	public void set( Settings settings )
	{
		if( mySettings != null ) mySettings.removeNewDtreeListener( this );
		mySettings = settings;

		myFlagListenEvents = false;
		myComboBoxMethod.setSelectedItem( mySettings.getDtreeMethod() );
		myComboBoxOrderHeuristic.setSelectedItem( mySettings.getElimAlgo() );
		if( FLAG_HMETIS_LOADED )
		{
			myComboBoxHMETISAlgo.setSelectedItem( mySettings.getHMeTiSAlgo() );
			myWNFNumberDtrees.setValue( mySettings.getNumDtrees() );
			myWNFNumberPartitions.setValue( mySettings.getNumPartitions() );
			myComboBoxBalanceFactors.setSelectedItem( mySettings.getBalanceFactor() );
		}
		myCbHuginLogStyle.setSelectedItem( mySettings.getDtreeStyle() );
		String filePath = "";
		String tentative = mySettings.getTentativeHuginLogFilePath();
		File setFile = mySettings.getHuginLogFile();
		if( setFile != null ) filePath = setFile.getPath();
		else if( tentative != null ) filePath = tentative;
		myTfHuginLogFile.setText( filePath );
		myComboRCComparator.setSelectedItem( settings.getRCComparator() );

		mySettings.addNewDtreeListener( this );
		myFlagListenEvents = true;
	}

	public void actionPerformed( ActionEvent evt )
	{
		Object src = evt.getSource();

		if( src == myComboBoxMethod ) toggleMethod();
		else if( src == myButtonPreview ) doPreview();
		else if( src == myBtnBrowse ) doBrowse();
		else if( myFlagListenEvents && mySettings != null )
		{
			if( src == myComboBoxOrderHeuristic ) mySettings.setElimAlgo( (EliminationHeuristic) myComboBoxOrderHeuristic.getSelectedItem() );
			else if( src == myComboBoxHMETISAlgo ) mySettings.setHMeTiSAlgo( (MethodHmetis.Algorithm) myComboBoxHMETISAlgo.getSelectedItem() );
			else if( src == myWNFNumberDtrees ) mySettings.setNumDtrees( myWNFNumberDtrees.getValue() );
			else if( src == myWNFNumberPartitions ) mySettings.setNumPartitions( myWNFNumberPartitions.getValue() );
			else if( src == myComboBoxBalanceFactors ) mySettings.setBalanceFactor( myComboBoxBalanceFactors.getSelectedItem() );
			//else if( src == myCheckBoxKeepBest ) mySettings.setKeepBest( myCheckBoxKeepBest.isSelected() );
			else if( src == myComboRCComparator ) mySettings.setRCComparator( (RCComparator) myComboRCComparator.getSelectedItem() );
			else if( src == myCbHuginLogStyle ) mySettings.setDtreeStyle( (MethodHuginLog.Style) myCbHuginLogStyle.getSelectedItem() );
			else if( src == myTfHuginLogFile ) mySettings.setTentativeHuginLogFilePath( myTfHuginLogFile.getText() );
		}
	}

	/** interface ChangeListener
		@since 20081128 */
	public ChangeListener settingChanged( ChangeEvent evt ){
		if( (evt.getSource() == mySettings) && (evt == mySettings.EVENT_NEW_DTREE) ){ resetDtreeMethod(); }
		return this;
	}

	protected boolean myFlagListenEvents = true;

	protected JComboBox myComboBoxMethod = null;
	protected JPanel myPanelMethod = null;
	protected JComponent myPanelHMETIS = null;
	protected JComponent myPanelEliminationOrder = null;
	protected JComponent myPanelHuginLog = null;
	//protected JCheckBox myCheckBoxKeepBest = null;
	protected JComboBox myComboRCComparator;
	protected JButton myButtonPreview = null;
	protected JButton myButtonOpenDtree = null;
	protected JButton myButtonSaveDtree = null;

	protected static boolean FLAG_HMETIS_LOADED = Hmetis.loaded();

	protected JComponent makeSettingsPanel()
	{
		JLabel lblMethod = new JLabel( "Dtree Method: ", JLabel.RIGHT );
		Object[] arrayMethods = null;
		if( FLAG_HMETIS_LOADED )
		{
			//arrayMethods = Settings.ARRAY_THREE_METHODS;
			myPanelHMETIS = makeHMETISPanel();
		}
		//else arrayMethods = Settings.ARRAY_THREE_METHODS_NOT_LOADED;
		arrayMethods = CreationMethod.getArray();

		myComboBoxMethod = new JComboBox( arrayMethods );
		myComboBoxMethod.addActionListener( this );
		myPanelMethod = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		myPanelMethod.setPreferredSize( new Dimension( 375, 101 ) );
		if( FLAG_DEBUG_BORDERS ) myPanelMethod.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		myPanelEliminationOrder = makeEliminationOrderPanel();
		myPanelMethod.add( myPanelEliminationOrder );

		myPanelHuginLog = makeHuginLogPanel();

		//myCheckBoxKeepBest = new JCheckBox( "Keep Best" );
		//myCheckBoxKeepBest.setSelected( true );
		//myCheckBoxKeepBest.addActionListener( myActionListener );
		myComboRCComparator = new JComboBox( RCComparator.ARRAY );
		myComboRCComparator.setSelectedItem( RCComparator.getDefault() );
		myComboRCComparator.addActionListener( myActionListener );
		myButtonPreview = new JButton( "Preview" );
		myButtonPreview.setEnabled( false );
		myButtonPreview.addActionListener( myActionListener );

		//myButtonOpenDtree = new JButton( "Open" );
		//myButtonOpenDtree.addActionListener( myActionListener );

		//myButtonSaveDtree = new JButton( "Save" );
		//myButtonSaveDtree.addActionListener( myActionListener );

		JPanel pnlButtons = new JPanel();
		//pnlButtons.add( myButtonOpenDtree );
		//pnlButtons.add( myButtonSaveDtree );
		Component strut = Box.createHorizontalStrut( (int)64 );
		pnlButtons.add( strut );
		//pnlButtons.add( myCheckBoxKeepBest );
		pnlButtons.add( myComboRCComparator );
		pnlButtons.add( myButtonPreview );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		//JPanel ret = new JPanel( gridbag );
		JPanel ret = this;
		ret.setLayout( gridbag );

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

		gridbag.setConstraints( pnlButtons, c );
		ret.add( pnlButtons );

		Border b = null;
		if( FLAG_DEBUG_BORDERS ) b = BorderFactory.createLineBorder( Color.green, 1 );
		else b = BorderFactory.createEmptyBorder( 8,16,8,16 );

		ret.setBorder( b );

		return ret;
	}

	protected void doPreview()
	{
		mySettings.setDtreeRequired( true );
		if( mySettings.validateDtree( myBeliefNetwork ) )
		{
			DtreePreviewDialog DPD = new DtreePreviewDialog( mySettings );
			Util.centerWindow( DPD, Util.convertBoundsToScreen( this ) );
			DPD.setVisible( true );
		}
		else showErrorMessage( STR_GENERIC_ERROR );
	}

	/** @since 20030501 */
	public void resetDtreeMethod()
	{
		Dtree dtree = mySettings.getDtree();
		if( dtree != null && dtree.myCreationMethod != null && dtree.myCreationMethod != myComboBoxMethod.getSelectedItem() )
		{
			//myFlagListenEvents = false;
			myComboBoxMethod.setSelectedItem( dtree.myCreationMethod );
			//myFlagListenEvents = true;
		}
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

	protected JComboBox myComboBoxOrderHeuristic = null;

	protected JComponent makeEliminationOrderPanel()
	{
		JLabel lblOrderHeuristic = new JLabel( "Heuristic: " );
		myComboBoxOrderHeuristic = new JComboBox( EliminationHeuristic.ARRAY );
		myComboBoxOrderHeuristic.addActionListener( myActionListener );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );
		Component strut = null;

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		gridbag.setConstraints( lblOrderHeuristic, c );
		ret.add( lblOrderHeuristic );

		strut = Box.createHorizontalStrut( INT_STRUT_SIZE );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myComboBoxOrderHeuristic, c );
		ret.add( myComboBoxOrderHeuristic );

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
		JOptionPane.showMessageDialog( this, txt, STR_GENERIC_TITLE, JOptionPane.ERROR_MESSAGE );
	}

	public static final String STR_GENERIC_ERROR = "Failed to create Dtree based on current settings.";
	public static final String STR_GENERIC_TITLE = "Dtree settings error";

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

		strut = Box.createVerticalStrut( (int)8 );
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
		JLabel lblHeuristic = new JLabel("Heuristic: ");
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
		gridbag.setConstraints( lblHeuristic, c );
		ret.add( lblHeuristic );

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

	/** test/debug */
	public static void main( String[] args )
	{
		FLAG_TEST = true;

		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		DtreeSettingsPanel DSP = new DtreeSettingsPanel();

		JFrame frame = new JFrame( "DEBUG Dtree Settings" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( DSP );
		frame.pack();
		frame.setVisible( true );
	}

	protected Settings          mySettings;
	protected ActionListener    myActionListener;
	protected BeliefNetwork     myBeliefNetwork;
	protected RCEngineGenerator myRCEngineGenerator;

	public static boolean
	  FLAG_DEBUG_BORDERS = false,
	  FLAG_TEST          = false;
}
