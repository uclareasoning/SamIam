package edu.ucla.belief.ui.internalframes;

import edu.ucla.structure.SetOperation;
import edu.ucla.belief.*;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.UserEnumProperty;

import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.toolbar.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.tree.Hierarchy;

import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/** @author keith cascio
	@since  20030821 */
public class SelectionInternalFrame extends JInternalFrame implements NodePropertyChangeListener, ActionListener, ChangeListener
{
	public SelectionInternalFrame( NetworkInternalFrame nif )
	{
		this( nif.getBeliefNetwork(), nif );
	}

	public SelectionInternalFrame( BeliefNetwork bn )
	{
		this( bn, null );
	}

	private SelectionInternalFrame( BeliefNetwork bn, NetworkInternalFrame nif )
	{
		super( "Variable Selection Tool", true, true, true, true );

		this.myBeliefNetwork = bn;
		this.myNIF = nif;

		if( myNIF != null ) myNIF.addNodePropertyChangeListener( this );

		init();
	}

	public void reInitialize()
	{
		setAssignTabEnabled( true );
		myJTabbedPane.setSelectedIndex( (int)0 );
		myFlagCommitUPEP = false;
		if( myLeftVSP != null )
		{
			myLeftVSP.reInitialize();
		}
		if( myRightVSP != null )
		{
			myRightVSP.reInitialize();
		}
		myEPEP.reInitialize();
		myCurrentUserProperties = myBeliefNetwork.getUserEnumProperties();
		myUPEP.editProperties( myCurrentUserProperties );
	}

	/** @since 010904 */
	public UserPropertyEditPanel getUserPropertyEditPanel()
	{
		return myUPEP;
	}

	/** @since 071304 */
	public EnumPropertyEditPanel getEnumPropertyEditPanel(){
		return myEPEP;
	}

	public static final String STR_FILENAME_ICON = "VariableSelectionTool16.gif";
	public static final Dimension DIM_PREF_SIZE = new Dimension( 558,440 );
	public static final Insets INSETS_BUTTONS = new Insets(0,8,0,8);

	public final SamiamAction action_TOGGLE = new SamiamAction( "selection panel", "Toggle selection panel view", 't', null )
	{
		public void actionPerformed( ActionEvent e )
		{
			toggleSelectionPanel();
			setEnabled( false );
			if( myCBToggle != null ) myCBToggle.setSelected( myOuterSelectionPanel.isAncestorOf( myInnerSelectionPanel ) );
			setEnabled( true );
		}
	};
	public final SamiamAction action_COMMIT = new SamiamAction( "Commit values", "Commit edited values", 'c', null )
	{
		public void actionPerformed( ActionEvent e )
		{
			if( myEPEP != null ) myEPEP.commitValues();
			if( myUPEP != null && myFlagCommitUPEP ) myUPEP.commit( myBeliefNetwork, myCurrentUserProperties );
			myNIF.getTreeScrollPane().refreshPropertyChanges();
		}
	};
	public final SamiamAction action_SELECT = new SamiamAction( "Select", "Select variables based on chosen set operation", 's', null )
	{
		public void actionPerformed( ActionEvent e )
		{
			select();
		}
	};
	public final SamiamAction action_OK = new SamiamAction( "OK", "Commit edited values", 'o', null )
	{
		public void actionPerformed( ActionEvent e )
		{
			action_COMMIT.actionPerformed( e );
			setVisible( false );
		}
	};
	public final SamiamAction action_CANCEL = new SamiamAction( "Cancel", "Destroy changes", 'c', null )
	{
		public void actionPerformed( ActionEvent e )
		{
			setVisible( false );
		}
	};

	private void init()
	{
		Icon iconFrame = MainToolBar.getIcon( STR_FILENAME_ICON );
		if( iconFrame != null ) setFrameIcon( iconFrame );

		setDefaultCloseOperation( HIDE_ON_CLOSE );

		myEPEP = Bridge2Tiger.Troll.solicit().newEnumPropertyEditPanel( myBeliefNetwork );
		myOuterSelectionPanel = new JPanel( new GridBagLayout() );
		if( FLAG_DEBUG_BORDERS ) myOuterSelectionPanel.setBorder( BorderFactory.createLineBorder( Color.green, 1 ) );
		myCBToggle = new JCheckBox( action_TOGGLE );
		//myCBToggle.addActionListener( this );
		JComponent containerButtons = makeButtonContainer();
		JComponent contOK = makeOKContainer();

		Container contentPain = getContentPane();
		contentPain.setLayout( new BorderLayout() );

		JPanel             pnlMain = new JPanel( myMainGridBag = new GridBagLayout() );
		GridBagConstraints c       = new GridBagConstraints();

		c.weightx   = 0.01;
		pnlMain.add( Box.createHorizontalStrut( 0x20 ), c );
		c.weightx   = 1.00;
		pnlMain.add( Box.createHorizontalStrut( 0x80 ), c );
		c.weightx   = 0.00;
		pnlMain.add( Box.createHorizontalStrut( 0x04 ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx   = 0.01;
		pnlMain.add( Box.createHorizontalStrut( 0x20 ), c );

		c.weightx   = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMain.add( Box.createVerticalStrut(4), c );

		c.weighty = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		pnlMain.add( myOuterSelectionPanel, c );

		c.weighty = 0;
		pnlMain.add( Box.createVerticalStrut(8), c );

		//c.anchor = GridBagConstraints.NORTHWEST;
		//c.gridwidth = 1;
		//pnlMain.add( myCBToggle, c );

		c.weightx   = 0.01;
		c.gridwidth = 1;
		c.fill      = GridBagConstraints.HORIZONTAL;
		pnlMain.add( Box.createHorizontalStrut(0x20), c );

		c.anchor    = GridBagConstraints.EAST;
		c.weighty   = c.weightx = 1;
		c.fill      = GridBagConstraints.BOTH;
		pnlMain.add( myEPEP, c );

		c.weighty   = c.weightx = 0;
		c.fill      = GridBagConstraints.NONE;
		pnlMain.add( Box.createHorizontalStrut(4), c );

		c.anchor    = GridBagConstraints.NORTH;
		c.weighty   = 0;
		c.weightx   = 0.01;
		c.fill      = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMain.add( containerButtons, c );

		c.anchor = GridBagConstraints.CENTER;
		c.weighty = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		pnlMain.add( Box.createVerticalStrut(4), c );

		myUPEP = new UserPropertyEditPanel();
		myUPEP.setEditListener( this );

		myJTabbedPane = new JTabbedPane();
		myJTabbedPane.addTab( "Assign Values", pnlMain );
		myJTabbedPane.addTab( "Edit User Properties", myUPEP );
		myJTabbedPane.addChangeListener( this );

		setPreferredSize( DIM_PREF_SIZE );

		contentPain.add( myJTabbedPane, BorderLayout.CENTER );
		contentPain.add( contOK, BorderLayout.SOUTH );
	}

	private JComponent makeButtonContainerOld()
	{
		/*
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable( false );
		toolbar.putClientProperty( "JToolBar.isRollover", Boolean.FALSE );
		toolbar.add( myCBToggle );
		toolbar.add( action_REMOVE );
		toolbar.add( action_COMMIT );
		return toolbar;
		*/
		/*
		FlowLayout flow = new FlowLayout();
		flow.setAlignment( FlowLayout.RIGHT );
		JPanel ret = new JPanel( flow );
		ret.add( myCBToggle );
		ret.add( initAction( action_EDITALL ) );
		ret.add( initAction( action_REMOVE ) );
		ret.add( initAction( action_COMMIT ) );
		*/
		JPanel ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		/*c.fill = GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myCBToggle, c );

		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( initAction( action_EDITALL, INSETS_BUTTONS ), c );

		ret.add( Box.createVerticalStrut(4), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		ret.add( initAction( action_REMOVE, INSETS_BUTTONS ), c );

		SamiamAction[] actions = myEPEP.getActions();
		if( actions != null ){
			c.fill = GridBagConstraints.HORIZONTAL;
			for( int i=0; i<actions.length; i++ ){
				ret.add( Box.createVerticalStrut(4),               c );
				ret.add( initAction( actions[i], INSETS_BUTTONS ), c );
			}
		}

		Hierarchy arch = myEPEP.getTargetHierarchy();
		if( arch != null ) ret.add( arch, c );

		ret.setBorder( BorderFactory.createEmptyBorder( 0,4,0,4 ) );*/

		return ret;
	}

	/** @since 20070324 */
	private JComponent makeButtonContainer(){
		Hierarchy hierarchy  = new Hierarchy();
		Hierarchy arch = null;

		if( (arch = myEPEP.getHierarchyView()    ) != null ) hierarchy.add( arch );
		arch.add( myCBToggle );
		if( (arch = myEPEP.getTargetHierarchy()  ) != null ) hierarchy.add( arch );
		if( (arch = myEPEP.getHierarchySelect()  ) != null ) hierarchy.add( arch );
		if( (arch = myEPEP.getHierarchySelected()) != null ) hierarchy.add( arch );

		JScrollPane pain = new JScrollPane( hierarchy );
		pain.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );

		Dimension dim = hierarchy.getPreferredSize();
		dim.height = 0x20;
		pain.setMinimumSize( dim );

		return pain;
	}

	private JComponent makeOKContainer()
	{
		FlowLayout flow = new FlowLayout();
		flow.setAlignment( FlowLayout.RIGHT );
		JPanel ret = new JPanel( flow );
		ret.add( initAction( action_OK, null ) );
		ret.add( initAction( action_CANCEL, null ) );
		return ret;
	}

	/** @since 20021022 */
	public static JComponent initAction( SamiamAction action, Insets insets )
	{
   		JButton         button =  new JButton( action );
		button.setText( action.getValue( Action.NAME ).toString() );
		if( insets != null ) button.setMargin( insets );

		JComponent         aux = action.getInputComponent();
		if( aux == null ) return button;

		JPanel             ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c   = new GridBagConstraints();
		c.fill                 = GridBagConstraints.HORIZONTAL;
		c.gridwidth            = GridBagConstraints.REMAINDER;
		c.weightx              = 1;
		ret.add( aux, c );
		ret.add( button, c );

		ret.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(2,2,2,2) ) );

		return ret;
	}

	public void toggleSelectionPanel()
	{
		//System.out.println( "SelectionInternalFrame.toggleSelectionPanel()" );

		myInnerSelectionPanel = getInnerSelectionPanel();
		if( myOuterSelectionPanel.isAncestorOf( myInnerSelectionPanel ) )
		{
			//System.out.println( "isAncestorOf()" );
			myOuterSelectionPanel.setMinimumSize( DIM_MIN_SIZE_EMPTY );
			myOuterSelectionPanel.remove( myInnerSelectionPanel );
			GridBagConstraints c = myMainGridBag.getConstraints( myOuterSelectionPanel );
			c.weightx = c.weighty = 0;
			c.fill = GridBagConstraints.NONE;
			myMainGridBag.setConstraints( myOuterSelectionPanel, c );
		}
		else
		{
			//System.out.println( "!isAncestorOf()" );
			//myOuterSelectionPanel.setMinimumSize( DIM_MIN_SIZE_FULL );
			Dimension dim = getSize();
			dim.width = Math.max( dim.width, DIM_PREF_SIZE.width );
			dim.height = Math.max( dim.height, DIM_PREF_SIZE.height );
			setSize( dim );
			mySelectionPanelConstraints.fill = GridBagConstraints.BOTH;
			mySelectionPanelConstraints.weightx = 1;
			mySelectionPanelConstraints.weighty = 1.1;
			mySelectionPanelConstraints.gridwidth = GridBagConstraints.REMAINDER;
			myOuterSelectionPanel.add( myInnerSelectionPanel, mySelectionPanelConstraints );
			myLeftVSP.reInitialize();
			myRightVSP.reInitialize();
			GridBagConstraints c = myMainGridBag.getConstraints( myOuterSelectionPanel );
			c.weightx = 0;
			c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			myMainGridBag.setConstraints( myOuterSelectionPanel, c );
		}
		revalidate();
		repaint();
	}

	public static final Dimension DIM_MIN_SIZE_EMPTY = new Dimension( 0,0 );
	public static final Dimension DIM_MIN_SIZE_FULL = new Dimension( 64,200 );

	public void select()
	{
		if( myComboSetOperation != null && myLeftVSP != null && myRightVSP != null )
		{
			SetOperation operation = (SetOperation) myComboSetOperation.getSelectedItem();
			List left = myLeftVSP.getVariables();
			Collection right;
			if( operation == SetOperation.COMPLEMENT ) right = myBeliefNetwork;
			else right = myRightVSP.getVariables();
			Set result = new HashSet( left.size() + right.size() );
			operation.exec( left, right, result );
			myEPEP.setVariables( result );
		}
	}

	private JComponent getInnerSelectionPanel()
	{
		if( myInnerSelectionPanel == null )
		{
			myInnerSelectionPanel = new JPanel( new GridBagLayout() );

			myLeftVSP = new VariableSelectionPanel( myBeliefNetwork );
			myRightVSP = new VariableSelectionPanel( myBeliefNetwork );
			myNIF.addNodePropertyChangeListener( myLeftVSP );
			myNIF.addNodePropertyChangeListener( myRightVSP );

			JPanel panelSelection = new JPanel( new GridLayout() );
			myLeftVSP.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 8) );
			myRightVSP.setBorder( BorderFactory.createEmptyBorder( 0, 8, 0, 0) );
			panelSelection.add( myLeftVSP );
			panelSelection.add( myRightVSP );
			if( FLAG_DEBUG_BORDERS ) panelSelection.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );

			myComboSetOperation = new JComboBox( SetOperation.ARRAY );
			myComboSetOperation.addActionListener( this );

			JPanel pnlButton = new JPanel();
			pnlButton.add( Box.createHorizontalStrut(8) );
			pnlButton.add( initAction( action_SELECT, INSETS_BUTTONS ) );

			GridBagConstraints c = new GridBagConstraints();

			c.weightx = 1;
			c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			c.gridwidth = GridBagConstraints.REMAINDER;
			myInnerSelectionPanel.add( panelSelection, c );
			/*
			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			myInnerSelectionPanel.add( myLeftVSP, c );

			c.weightx = .01;
			c.fill = GridBagConstraints.NONE;
			myInnerSelectionPanel.add( Box.createHorizontalStrut(16), c );

			c.weightx = 1;
			c.fill = GridBagConstraints.BOTH;
			c.gridwidth = GridBagConstraints.REMAINDER;
			myInnerSelectionPanel.add( myRightVSP, c );
			*/

			c.weightx = 0;
			c.weighty = 0;
			c.fill = GridBagConstraints.NONE;
			c.gridwidth = GridBagConstraints.REMAINDER;
			myInnerSelectionPanel.add( Box.createVerticalStrut(4), c );

			c.weightx = 1.05;
			c.gridwidth = 1;
			myInnerSelectionPanel.add( Box.createHorizontalStrut(16), c );

			c.weightx = 0;
			myInnerSelectionPanel.add( myComboSetOperation, c );

			c.weightx = 0;
			c.anchor = GridBagConstraints.WEST;
			myInnerSelectionPanel.add( pnlButton, c );

			c.weightx = 0.75;
			c.gridwidth = GridBagConstraints.REMAINDER;
			myInnerSelectionPanel.add( Box.createHorizontalStrut(8), c );

			if( FLAG_DEBUG_BORDERS ) myInnerSelectionPanel.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );
		}

		return myInnerSelectionPanel;
	}

	/**
		interface NodePropertyChangeListener
		@since 100703
	*/
	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		//System.out.println( "SelectionInternalFrame.nodePropertyChanged( "+e+" )" );
		if( myEPEP != null ) myEPEP.nodePropertyChanged( e );
	}

	private boolean myFlagListenToComboValue = true;

	public void actionPerformed( ActionEvent e )
	{
		Object src = e.getSource();
		if( src == myCBToggle ) toggleSelectionPanel();
		else if( src == myComboSetOperation )
		{
			myRightVSP.setEnabled( !( myComboSetOperation.getSelectedItem() == SetOperation.COMPLEMENT ) );
		}
		else if( e == myUPEP.EVENT_PANEL_EDITED ) setAssignTabEnabled( false );
	}

	/**
		@since 110403
	*/
	public void setAssignTabEnabled( boolean flag )
	{
		myJTabbedPane.setEnabledAt( (int)0, flag );
	}

	public void stateChanged( ChangeEvent e )
	{
		Object src = e.getSource();
		if( src == myJTabbedPane )
		{
			if( myJTabbedPane.getSelectedComponent() == myUPEP )
			{
				//System.out.println( "myJTabbedPane.getSelectedComponent() == myUPEP" );
				myFlagCommitUPEP = true;
			}
		}
	}

	/**
		Test/debug
	*/
	public static void main( String[] args )
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		String path = "c:\\keithcascio\\networks\\cancer_test.net";
		String envPathNetworks = System.getenv( "NETWORKSPATH" );
		if( envPathNetworks != null && envPathNetworks.length() > 0 ){
			path = new File( envPathNetworks, "cancer.net" ).getAbsolutePath();
		}

		if( args.length > 0 ) path = args[0];

		BeliefNetwork bn = null;
		try{
			bn = NetworkIO.read( path );
		}catch( Exception e ){
			System.err.println( "Error: Failed to read " + path );
			return;
		}

		bn.setUserEnumProperties( Arrays.asList( new EnumProperty[] { new UserEnumProperty(), new UserEnumProperty() } ) );

		final SelectionInternalFrame sif = new SelectionInternalFrame( bn );
		sif.addInternalFrameListener( new InternalFrameAdapter()
		{
			public void internalFrameClosing(InternalFrameEvent e)
			{
				Util.STREAM_TEST.println( "SelectionInternalFrame size: " + sif.getSize() );
			}
		} );

		JButton btnCommit = new JButton( "show" );
		btnCommit.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				sif.reInitialize();
				sif.setVisible( true );
			}
		} );
		JPanel pnlButtons = new JPanel( new GridLayout( 2, 1 ) );
		pnlButtons.add( btnCommit );
		JButton btnShot = new JButton( "screenshot" );
		btnShot.addActionListener( new ActionListener(){
			public void actionPerformed( ActionEvent e ){
				try{
					File out = new File( System.getenv( "SAMIAMPATH" ) + File.separator + "edu/ucla/belief/ui/internalframes", "screenshot_vst_"+edu.ucla.util.AbstractStringifier.DateFormatFilename.now()+".png");
					Util.screenshot( sif, "png", out );
				}catch( Exception exception ){
					exception.printStackTrace();
				}
			}
		} );
		pnlButtons.add( btnShot );

		JDesktopPane pain = new JDesktopPane();
		pain.add( sif );

		sif.setBounds( new Rectangle( new Point(10,10), DIM_PREF_SIZE ) );
		sif.reInitialize();
		sif.setVisible( true );

		JFrame frame = new JFrame( "DEBUG SelectionInternalFrame" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		Container contentPain = frame.getContentPane();
		contentPain.setLayout( new BorderLayout() );
		contentPain.add( pain, BorderLayout.CENTER );
		contentPain.add( pnlButtons, BorderLayout.EAST );
		contentPain.add( new JLabel( "network: " + path ), BorderLayout.SOUTH );
		//frame.pack();
		frame.setSize( 800,700 );
		Util.centerWindow( frame );
		frame.setVisible( true );
	}

	public static final boolean FLAG_DEBUG_BORDERS = false;

	private GridBagLayout myMainGridBag;
	private JCheckBox myCBToggle;
	private JComponent myOuterSelectionPanel;
	private JComponent myInnerSelectionPanel;
	private GridBagConstraints mySelectionPanelConstraints = new GridBagConstraints();
	private VariableSelectionPanel myLeftVSP;
	private VariableSelectionPanel myRightVSP;
	private JComboBox myComboSetOperation;
	private EnumPropertyEditPanel myEPEP;
	private UserPropertyEditPanel myUPEP;
	private boolean myFlagCommitUPEP = false;;
	private JTabbedPane myJTabbedPane;
	private Collection myCurrentUserProperties;

	private BeliefNetwork myBeliefNetwork;
	private NetworkInternalFrame myNIF;
}
