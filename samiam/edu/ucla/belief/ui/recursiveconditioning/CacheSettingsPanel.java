package edu.ucla.belief.ui.recursiveconditioning;

import edu.ucla.belief.dtree.*;
import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.NetworkIO;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.File;

/**
	@author Keith Cascio
	@since 020703
*/
public class CacheSettingsPanel extends JPanel implements ActionListener, ChangeListener
{
	public CacheSettingsPanel()
	{
		init();
	}

	/**
		@author Keith Cascio
		@since 031903
	*/
	public void setDefaultDirectory( File defaultDirectory )
	{
		myDefaultDirectory = defaultDirectory;
	}

	/**
		@author Keith Cascio
		@since 031903
	*/
	public void setNetworkName( String networkName )
	{
		myNetworkName = networkName;
	}

	protected void init()
	{
		myActionListener = this;
		makePopupMenu();
		makeAllocationPanel();
	}

	/**
		Call before set()
	*/
	public void setBeliefNetwork( BeliefNetwork bn )
	{
		myBeliefNetwork = bn;
	}

	/**
		Call after setBeliefNetwork()
	*/
	public void set( Settings settings )
	{
		mySettings = settings;
		//resetSlider();now called in updateMemoryDisplay()
		resetCheckBox();
		updateMemoryDisplay();
		validateActionsEnabled();
		setFileName( settings.getBundle().getRCFilePath() );
	}

	public static final String STR_NO_FILE = null;
	public static final String STR_OPEN_FILE = "RC file: ";

	/**
		@author Keith Cascio
		@since 033103
	*/
	public void setFileName( File openFile )
	{
		if( openFile == null ) setFileName( STR_NO_FILE );
		else setFileName( STR_OPEN_FILE + openFile.getPath() );
	}

	/**
		@author Keith Cascio
		@since 040303
	*/
	public void setFileName( String fname )
	{
		setToolTipText( fname );
	}

	protected JLabel myLabelOptimalMemory = null;
	protected JLabel myLabelUserMemory = null;
	protected JSlider mySliderUserMemory = null;
	protected JLabel myLabelOptimalMemoryUnits = null;
	protected JLabel myLabelUserMemoryUnits = null;
	protected JCheckBox myCBuseKB;
	protected JButton myButtonPreview = null;
	protected JButton myButtonFile = null;
	//protected JButton myButtonStopMemoryAllocation = null;
	protected JPanel myPanelMemoryAllocationButtons = null;

	public static final int INT_MIN_SLIDER = 0;
	public static final int INT_MAX_SLIDER = 100;
	public static final int INT_TICK_MAJOR = 25;
	public static final int INT_TICK_MINOR = 5;

	/**
		@author Keith Cascio
		@since 012403
	*/
	protected void resetSlider()
	{
		//System.out.println( "CacheSettingsPanel.resetSlider() " + mySettings.getUserMemoryProportion() );
		myFlagListenSlider = false;
		mySliderUserMemory.setValue( (int)((double)INT_MAX_SLIDER * mySettings.getUserMemoryProportion()) );
		myFlagListenSlider = true;
	}

	protected boolean myFlagListenSlider = true;

	/**
		@author Keith Cascio
		@since 053003
	*/
	protected void resetCheckBox()
	{
		myFlagIgnoreActions = true;
		myCBuseKB.setSelected( mySettings.getUseKB() );
		myFlagIgnoreActions = false;
	}

	protected boolean myFlagIgnoreActions = false;

	/** @since 020405 */
	public String getClipboardInfo(){
		StringBuffer buffer = new StringBuffer( 256 );
		buffer.append( "Maximum memory needed: " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myLabelOptimalMemory );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myLabelOptimalMemoryUnits );
		buffer.append( "\nMemory to use? " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myLabelUserMemory );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myLabelUserMemoryUnits );
		buffer.append( "\ntake advantage of determinism? " );
		if( myCBuseKB != null )
			buffer.append( Boolean.toString( myCBuseKB.isSelected() ) );
		return buffer.toString();
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

		myCBuseKB = new JCheckBox( "take advantage of determinism" );
		myCBuseKB.addActionListener( this );

		String textAllocateMemory = "Preview";
		myButtonPreview = new JButton( textAllocateMemory );
		myButtonPreview.addActionListener( myActionListener );
		//myButtonStopMemoryAllocation = new JButton( "Stop" );
		//myButtonStopMemoryAllocation.addActionListener( myActionListener );
		//myButtonStopMemoryAllocation.setEnabled( false );
		myButtonFile = new JButton( "Open/Save" );
		myButtonFile.addActionListener( myActionListener );

		GridBagLayout gridbagButtons = new GridBagLayout();
		GridBagConstraints cButtons = new GridBagConstraints();
		cButtons.gridwidth = 1;
		cButtons.weightx = 0;
		cButtons.fill = GridBagConstraints.NONE;
		myPanelMemoryAllocationButtons = new JPanel( gridbagButtons );
		myPanelMemoryAllocationButtons.add( myButtonPreview, cButtons );
		cButtons.weightx = 1;
		cButtons.fill = GridBagConstraints.HORIZONTAL;
		myPanelMemoryAllocationButtons.add( Box.createHorizontalStrut( 1 ), cButtons );
		cButtons.weightx = 0;
		cButtons.fill = GridBagConstraints.NONE;
		cButtons.gridwidth = GridBagConstraints.REMAINDER;
		myPanelMemoryAllocationButtons.add( myButtonFile, cButtons );

		//myPanelMemoryAllocationButtons = new JPanel();
		//myPanelMemoryAllocationButtons.add( myButtonPreview );
		//myPanelMemoryAllocationButtons.add( Box.createHorizontalGlue() );
		//myPanelMemoryAllocationButtons.add( myButtonFile );
		//myPanelMemoryAllocationButtons.add( myButtonStopMemoryAllocation );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		//JPanel ret = new JPanel( gridbag );
		JPanel ret = this;
		ret.setLayout( gridbag );

		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.ipadx = 0;
		c.ipady = 16;
		c.weightx = 0;
		/*
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints( myLabelOptimalMemory, c );
		ret.add( myLabelOptimalMemory );

		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints( lblMemoryCaption, c );
		ret.add( lblMemoryCaption );
		*/

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

		//c.gridwidth = 1;
		//gridbag.setConstraints( pnlUserMemory, c );
		//ret.add( pnlUserMemory );

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
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipady = (int)0;
		c.weightx = 1;
		gridbag.setConstraints( myPanelMemoryAllocationButtons, c );
		ret.add( myPanelMemoryAllocationButtons );
		c.ipady = (int)16;

		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.ipady = 0;
		Component strut = Box.createVerticalStrut( 16 );
		gridbag.setConstraints( strut, c );
		ret.add( strut );

		ret.setBorder( BorderFactory.createEmptyBorder( 8,8,8,8 ) );

		return ret;
	}

	public void stateChanged( ChangeEvent e )
	{
		Object src = e.getSource();
		if( src == mySliderUserMemory && myFlagListenSlider && mySettings != null )
		{
			double newMemoryProportion = (double)mySliderUserMemory.getValue()/(double)INT_MAX_SLIDER;

			myLabelUserMemory.setText( mySettings.updateUserMemoryDisplay( mySettings.getBundle().getAll(), newMemoryProportion ) );

			if( !mySliderUserMemory.getValueIsAdjusting() )
			{
				if( mySettings.setUserMemoryProportion( newMemoryProportion ) )
				{
					updateUserMemoryDisplay();
				}
			}
		}
	}

	public void updateMemoryDisplay()
	{
		//mySettings.updateOptimalMemoryNumber( myBeliefNetwork );
		//if( mySettings.validateDtree( myBeliefNetwork ) )
		if( mySettings.validateRC( myBeliefNetwork ) )
		{
			resetSlider();
			updateOptimalMemoryDisplay();
		}
	}

	/**
		@author Keith Cascio
		@since 022803
	*/
	protected void updateOptimalMemoryDisplay()
	{
		String[] newVals = mySettings.updateOptimalMemoryDisplay( mySettings.getBundle().getAll() );

		if( myMemoryUnit != newVals[1] )
		{
			myMemoryUnit = newVals[1];
			updateMemoryUnitsDisplay();
		}

		myLabelOptimalMemory.setText( newVals[0] );
		updateUserMemoryDisplay();
	}

	protected void updateUserMemoryDisplay()
	{
		myLabelUserMemory.setText( mySettings.updateUserMemoryDisplay( mySettings.getBundle().getAll() ) );
	}

	protected String myMemoryUnit = Settings.STR_KILOBYTE_UNIT;

	protected void updateMemoryUnitsDisplay()
	{
		myLabelOptimalMemoryUnits.setText( myMemoryUnit );
		myLabelUserMemoryUnits.setText( myMemoryUnit );
	}

	public void actionPerformed( ActionEvent evt )
	{
		if( myFlagIgnoreActions ) return;

		Object src = evt.getSource();

		if( src == myButtonPreview ) doPreview();
		else if( src == myButtonFile )
		{
			Point p = myButtonFile.getLocation();
			SwingUtilities.convertPointToScreen( p,myPanelMemoryAllocationButtons );
			showPopup( p );
		}
		else if( src == myCBuseKB && mySettings != null ) mySettings.setUseKB( myCBuseKB.isSelected() );
		//else if( src == myOpenRCItem ) doOpenRC();
		//else if( src == mySaveRCItem ) doSaveRC();
	}

	/**
		@author Keith Cascio
		@since 012903
	*/
	protected boolean doOpenRC()
	{
		JFileChooser chooser = getFileChooser();
		if( chooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION )
		{
			File fileSelected = chooser.getSelectedFile();
			if( mySettings.doOpenRC( myBeliefNetwork, fileSelected ) )
			{
				if( mySettings.getRC() instanceof RCDgraph )
				{
					updateOptimalMemoryDisplay();
					resetSlider();
					setFileName( mySettings.getBundle().getRCFilePath() );
					//setFileName( fileSelected );
					//if( myLabelFilePath != null ) myLabelFilePath.setText( mySettings.getBundle().getRCFilePath() );
					//resetRCWidgets();
					return true;
				}
				else
				{
					showErrorMessage( "The RC file you selected did not contain a dgraph.", "Error: insufficient data" );
					mySettings.setRC( null );
					return false;
				}
			}
		}
		return false;
	}

	/**
		@author Keith Cascio
		@since 032003
	*/
	protected void showErrorMessage( String message, String title )
	{
		JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
	}

	/**
		@author Keith Cascio
		@since 030403
	*/
	public final Action action_OPENRC = new SamiamAction( "Open cache allocation", "Open a saved recursive conditioning cache allocation.", 'o', MainToolBar.getBlankIcon() )
	{
		public void actionPerformed( ActionEvent e )
		{
			doOpenRC();
		}
	};
	/**
		@author Keith Cascio
		@since 030403
	*/
	public final Action action_SAVERC = new SamiamAction( "Save cache allocation", "Save the open recursive conditioning cache allocation.", 's', MainToolBar.getBlankIcon() )
	{
		public void actionPerformed( ActionEvent e )
		{
			doSaveRC();
		}
	};
	/**
		@author Keith Cascio
		@since 030403
	*/
	public void validateActionsEnabled()
	{
		if( mySettings != null )
		{
			action_SAVERC.setEnabled( mySettings.getRC() != null );
		}
	}

	/**
		@author Keith Cascio
		@since 012903
	*/
	protected boolean doSaveRC()
	{
		RC rc = mySettings.getRC();

		if( rc != null )
		{
			JFileChooser chooser = getFileChooser();
			if( chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION )
			{
				File fileSelected = chooser.getSelectedFile();
				mySettings.doSaveRC( null, rc, mySettings.getBundle().getAll(), fileSelected, myNetworkName );
				setFileName( mySettings.getBundle().getRCFilePath() );
				//setFileName( fileSelected );
				//if( myLabelFilePath != null ) myLabelFilePath.setText( mySettings.getBundle().getRCFilePath() );
				return true;
			}
		}

		System.err.println( "Warning: RCPanel.doSaveRC() failed" );
		return false;
	}

	protected javax.swing.filechooser.FileFilter myRCDtreeFileFilter = new edu.ucla.belief.io.InflibFileFilter( new String[]{ ".rc" }, UI.STR_SAMIAM_ACRONYM + " RC Files (*.rc)" );
	protected JFileChooser myJFileChooser;
	protected RCAccessory myRCAccessory;

	/**
		@author Keith Cascio
		@since 121302
	*/
	protected JFileChooser getFileChooser()
	{
		//JFileChooser chooser = new JFileChooser( hnInternalFrame.getParentFrame().getSamiamPreferences().defaultDirectory );
		if( myJFileChooser == null ){
			if( myDefaultDirectory == null ) myDefaultDirectory = new File( "." );
			myJFileChooser = new JFileChooser( myDefaultDirectory );
			myJFileChooser.setAcceptAllFileFilterUsed( false );
			myJFileChooser.addChoosableFileFilter( myRCDtreeFileFilter );
			if( NetworkIO.xmlAvailable() )
			{
				myRCAccessory = new RCAccessory();
				myRCAccessory.setFileFilter( myRCDtreeFileFilter );
				myJFileChooser.addPropertyChangeListener( myRCAccessory );
				myJFileChooser.setAccessory( myRCAccessory );
			}
		}

		if( !myDefaultDirectory.equals( myLastDefaultDirectory ) )
		{
			myLastDefaultDirectory = myDefaultDirectory;
			myJFileChooser.setCurrentDirectory( myDefaultDirectory );
		}

		if( myRCAccessory != null ) myRCAccessory.clear();

		return myJFileChooser;
	}

	protected void doPreview()
	{
		CachePreviewDialog CPD = new CachePreviewDialog( mySettings, myBeliefNetwork, true );
		Util.centerWindow( CPD, Util.convertBoundsToScreen( this ) );
		CPD.setVisible( true );
		if( CPD.performedSearch() )
		{
			updateUserMemoryDisplay();
			resetSlider();
		}
	}

	protected JPopupMenu myPopupMenu = null;
	//protected JMenuItem myOpenRCItem = null;
	//protected JMenuItem mySaveRCItem = null;
	protected MouseListener myMouseListener = null;

	protected void makePopupMenu()
	{
		//myOpenRCItem = new JMenuItem( action_OPENRC );
		//mySaveRCItem = new JMenuItem( action_SAVERC );

		//myOpenRCItem.addActionListener( myActionListener );
		//mySaveRCItem.addActionListener( myActionListener );

		myPopupMenu = new JPopupMenu();
		myPopupMenu.setInvoker( this );

		//myPopupMenu.add( myOpenRCItem );
		//myPopupMenu.add( mySaveRCItem );
		myPopupMenu.add( action_OPENRC );
		myPopupMenu.add( action_SAVERC );

		myMouseListener = new MouseAdapter()
		{
			public void mousePressed( MouseEvent e )
			{
				showPopup(e);
			}

			public void mouseClicked( MouseEvent e )
			{
				showPopup(e);
			}

			public void mouseReleased( MouseEvent e )
			{
				showPopup(e);
			}
		};

		addMouseListener( myMouseListener );
	}

	/**
		@author Keith Cascio
		@since 030502
	*/
	protected void showPopup( MouseEvent e )
	{
		if( e.isPopupTrigger() )
		{
			Point p = e.getPoint();
			SwingUtilities.convertPointToScreen( p,e.getComponent() );
			showPopup( p );
		}
	}

	/**
		@author Keith Cascio
		@since 030502
	*/
	protected void showPopup( Point p )
	{
		myPopupMenu.setLocation( p );
		//myPopupMenu.setInvoker( this );
		showPopup();
	}

	/**
		@author Keith Cascio
		@since 030502
	*/
	protected void showPopup()
	{
		//myOpenRCItem.setEnabled( true );
		//mySaveRCItem.setEnabled( mySettings.getRC() != null );
		validateActionsEnabled();
		myPopupMenu.setVisible( true );
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

	protected Settings mySettings;
	protected ActionListener myActionListener;
	protected BeliefNetwork myBeliefNetwork;
	protected File myDefaultDirectory;
	protected File myLastDefaultDirectory;
	protected String myNetworkName;

	public static boolean FLAG_DEBUG_BORDERS = false;
	public static boolean FLAG_TEST = false;
}
