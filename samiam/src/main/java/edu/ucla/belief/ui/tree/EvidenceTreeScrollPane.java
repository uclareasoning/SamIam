package edu.ucla.belief.ui.tree;

import edu.ucla.belief.EvidenceController;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.io.dsl.DiagnosisType;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;

import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.displayable.DisplayableBeliefNetwork;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.util.Util;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.lang.reflect.*;

/**
<p>
	JPanel containing the EvidenceTree of a NetworkInternalFrame.
<p>
	Modified by Keith Cascio 030502

*/
public class EvidenceTreeScrollPane extends JPanel implements
	ActionListener, ItemListener, NetStructureChangeListener
{
	protected NetworkInternalFrame hnInternalFrame;
	protected SamiamPreferences myTreePrefs;

	protected JButton resetButton, expandButton, collapseButton;
	protected EvidenceTree tree;

	protected JScrollPane myJScrollPane;
	protected JComboBox myJComboBox;
	protected boolean myFlagListenCombo = true;

	/** @since 20030318 */
	public EvidenceTree getEvidenceTree()
	{
		return tree;
	}

	/** @since 20070326 */
	public JScrollPane getJScrollPane(){
		return myJScrollPane;
	}

	public EvidenceTreeScrollPane(	NetworkInternalFrame hnInternalFrame,
					SamiamPreferences treePrefs )
	{
		super( new BorderLayout() );
		this.hnInternalFrame = hnInternalFrame;
		this.myTreePrefs = treePrefs;
		hnInternalFrame.addNetStructureChangeListener(this);
		init();
	}

	protected JPopupMenu myPopupMenu;
	protected JMenuItem myResetEvidenceItem, mySetDefaultsItem;

	protected JMenu myExpandMenu;
	protected JCheckBoxMenuItem myVariablesItem, myTypesItem, mySubmodelsItem;
	protected MouseListener myMouseListener;

	public static final String STR_TEXT_RESET_ENABLED   = "Reset Evidence";
	public static final String STR_TEXT_FROZEN          = " (frozen)";
	public static final String STR_TEXT_RESET_FROZEN    = STR_TEXT_RESET_ENABLED + STR_TEXT_FROZEN;
	public static final String STR_TEXT_DEFAULT_ENABLED = "Set Default Evidence";
	public static final String STR_TEXT_DEFAULT_FROZEN  = STR_TEXT_DEFAULT_ENABLED + STR_TEXT_FROZEN;

	/** @since 20020305 */
	protected void init()
	{
		myJScrollPane = new JScrollPane();
		myJScrollPane.setPreferredSize( new Dimension( 150, 600 ) );

		EvidenceTreeScrollPane.this.add( myJScrollPane, BorderLayout.CENTER );

		UI ui = hnInternalFrame.getParentFrame();

	   (myExpandMenu =                              new JMenu(        "Expand" )).setIcon( MainToolBar.getIcon(              "Tree16.gif" )          );
		myExpandMenu.add( myVariablesItem = new JCheckBoxMenuItem( "Variables",            MainToolBar.getIcon(      "TreeExpanded16.gif" ), false ) ).addActionListener( (ActionListener) this );
		myExpandMenu.add( myTypesItem     = new JCheckBoxMenuItem(     "Types",            MainToolBar.getIcon(              "Tree16.gif" ),  true ) ).addActionListener( (ActionListener) this );
		if( hnInternalFrame.getBeliefNetwork().isGenieNet() ){
		myExpandMenu.add( mySubmodelsItem = new JCheckBoxMenuItem( "Submodels",            MainToolBar.getIcon( "SubmodelsExpanded16.gif" ), false ) ).addActionListener( (ActionListener) this );
		}

		myPopupMenu = new JPopupMenu();

		myPopupMenu.add( ui.action_RESETEVIDENCE   );//myResetEvidenceItem = new JMenuItem( STR_TEXT_RESET_ENABLED   ) ).addActionListener( (ActionListener) this );
		myPopupMenu.add( ui.action_DEFAULTEVIDENCE );//mySetDefaultsItem   = new JMenuItem( STR_TEXT_DEFAULT_ENABLED ) ).addActionListener( (ActionListener) this );
		myPopupMenu.add( new JCheckBoxMenuItem( ui.action_EVIDENCEFROZEN ) );
		myPopupMenu.add( myExpandMenu );

		resetEnumPropertiesDisplay();
		getGrep();

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

		setEvidenceTree( new EvidenceTree( hnInternalFrame, myTreePrefs ) );
	}

	/** @since 20030911 */
	public void resetEnumPropertiesDisplay()
	{
		DefaultComboBoxModel model = new DefaultComboBoxModel( hnInternalFrame.getBeliefNetwork().propertiesAsArray() );

		if( myJComboBox == null )
		{
			myJComboBox = new JComboBox( model );
			myJComboBox.setSelectedItem( DiagnosisType.PROPERTY );
			myJComboBox.addItemListener( this );
			//myJScrollPane.setColumnHeaderView( myJComboBox );
			EvidenceTreeScrollPane.this.add( myJComboBox, BorderLayout.NORTH );
		}
		else
		{
			Object selected = myJComboBox.getSelectedItem();
			myJComboBox.setModel( model );
			if( model.getIndexOf(selected) >= 0 ) myJComboBox.setSelectedItem( selected );
			else doItemSelected();
		}
	}

	/** @since 20070326 */
	public SamiamAction getGrep(){
		if( myFlagGrep ) return myActionGrep;
		try{
			Class        clazzGrepAction = Class.forName( "edu.ucla.belief.ui.tree.NetworkGrep" );
			Class        clazzGrepable   = Class.forName( "edu.ucla.belief.ui.actionsandmodes.Grepable" );
			Class[]      arrayGrepable   = new Class[]{ clazzGrepable };
			Constructor  uctor           = clazzGrepAction.getConstructor( arrayGrepable );
			SamiamAction agrep           = (SamiamAction) uctor.newInstance( new Object[]{ hnInternalFrame.getBeliefNetwork() } );

			try{
				Method   methodContextua = clazzGrepAction.getMethod( "contextualize", new Class[]{ NetworkInternalFrame.class } );
				methodContextua.invoke( agrep, new Object[]{ hnInternalFrame } );
			}catch( Throwable throwable ){
				if( Util.DEBUG_VERBOSE ) System.err.println( "warning: EvidenceTreeScrollPane.getGrep() caught " + throwable );
			}

			try{
				agrep.setPreferences( hnInternalFrame.getPackageOptions() );
				hnInternalFrame.getParentFrame().addPreferenceListener( agrep );
			}catch( Throwable throwable ){
				if( Util.DEBUG_VERBOSE ) System.err.println( "warning: EvidenceTreeScrollPane.getGrep() caught " + throwable );
			}

			Method       methodNP        = clazzGrepAction.getMethod( "newPanel", (Class[]) null );
			JComponent   pnl             = (JComponent) methodNP.invoke( agrep, (Object[]) null );
			pnl.setBorder( BorderFactory.createEmptyBorder(2,1,2,1) );

			EvidenceTreeScrollPane.this.add( pnl, BorderLayout.SOUTH );
			myActionGrep                 = agrep;
		}catch( UnsupportedClassVersionError exception ){
			if( Util.DEBUG_VERBOSE ) System.err.println( "evidence tree "+SamiamPreferences.STR_GREP_DISPLAY_NAME_LOWER+" functionality requires java version 5 or later runtime" );
		}catch( Exception exception ){
			if( Util.DEBUG_VERBOSE ) System.err.println( "warning: EvidenceTreeScrollPane.getGrep() caught " + exception );
		}finally{
			myFlagGrep = true;
		}
		return myActionGrep;
	}
	private SamiamAction myActionGrep;
	private boolean      myFlagGrep = false;

	/** @since 20051006 */
	private boolean isEvidenceEnabled(){
		DisplayableBeliefNetwork bn = hnInternalFrame.getBeliefNetwork();
		if( bn == null ) return false;
		EvidenceController ec = bn.getEvidenceController();
		if( ec == null ) return false;
		return !ec.isFrozen();
	}

	/** @since 20051006 */
	private void setEvidenceItemsEnabled(){
		boolean flagEnabled = isEvidenceEnabled();
		if( myResetEvidenceItem != null ){ myResetEvidenceItem.setEnabled( flagEnabled ); }
		if( mySetDefaultsItem   != null ){ mySetDefaultsItem  .setEnabled( flagEnabled ); }
		String textReset   = flagEnabled ? STR_TEXT_RESET_ENABLED   : STR_TEXT_RESET_FROZEN;
		String textDefault = flagEnabled ? STR_TEXT_DEFAULT_ENABLED : STR_TEXT_DEFAULT_FROZEN;
		if( myResetEvidenceItem != null ){ myResetEvidenceItem.setText( textReset   ); }
		if( mySetDefaultsItem   != null ){ mySetDefaultsItem  .setText( textDefault ); }
	}

	/** @since 20020305 */
	protected void showPopup( MouseEvent e )
	{
		if( e.isPopupTrigger() )
		{
			setEvidenceItemsEnabled();
			myVariablesItem.setSelected( tree.areVariableBranchesExpanded() );
			myTypesItem.setSelected( tree.areTypeBranchesExpanded() );
			if( mySubmodelsItem != null ){ mySubmodelsItem.setSelected( tree.areSubmodelBranchesExpanded() ); }

			Point p = e.getPoint();
			SwingUtilities.convertPointToScreen( p,e.getComponent() );
			showPopup( p );
		}
	}

	/** @since 20050212 */
	public JPopupMenu showPopup( Point coordScreen ){
		myPopupMenu.setLocation( coordScreen );
		myPopupMenu.setInvoker( myJScrollPane );
		myPopupMenu.setVisible( true );
		return myPopupMenu;
	}

	public void netStructureChanged( NetStructureEvent event ) {
		revalidateEvidenceTree();
	}

	public void changePackageOptions()
	{
		EnumProperty property = null;
		Preference treeSortDefault = myTreePrefs.getMappedPreference( SamiamPreferences.treeSortDefault );
		if( treeSortDefault != null && treeSortDefault.isRecentlyCommittedValue() ) property = (EnumProperty) treeSortDefault.getValue();

		if( property == null ) revalidateEvidenceTree();
		else revalidateEvidenceTree( property );
	}

	/** @since 20030910 */
	public void refreshPropertyChanges()
	{
		boolean fatal = tree.isPropertyChangeFatal();
		if( fatal ) revalidateEvidenceTree();
		else tree.refreshNonFatalPropertyChanges();
		//System.out.print( "EvidenceTreeScrollPane.refreshPropertyChanges() " );
		//System.out.println( fatal ? "fatal" : "non-fatal" );
	}

	protected void revalidateEvidenceTree()
	{
		if( tree == null ) setEvidenceTree( new EvidenceTree( hnInternalFrame, myTreePrefs ) );
		else revalidateEvidenceTree( tree.getEnumProperty() );
	}

	protected void revalidateEvidenceTree( EnumProperty property )
	{
		setEvidenceTree( new EvidenceTree( hnInternalFrame, myTreePrefs, property ) );
	}

	private final void setEvidenceTree( EvidenceTree newTree )
	{
		Set expandedSet = Collections.EMPTY_SET;
		if( tree != null ){
			expandedSet = tree.getExpandedSet();
			tree.die();
			tree = null;
		}

		tree = newTree;

		tree.setExpandedSet(expandedSet);
		hnInternalFrame.addEvidenceChangeListener(tree);
		myJScrollPane.setViewportView( tree );
		tree.addMouseListener( myMouseListener );

		myFlagListenCombo = false;
		myJComboBox.setSelectedItem( tree.getEnumProperty() );
		myFlagListenCombo = true;
	}

	public void actionPerformed( ActionEvent event )
	{
		Object source = event.getSource();
		if( source == myResetEvidenceItem ){
			hnInternalFrame.resetEvidence( (Component)EvidenceTreeScrollPane.this.myJScrollPane );
		}
		else if( source == mySetDefaultsItem ){
			hnInternalFrame.setDefaultEvidence( (Component)EvidenceTreeScrollPane.this.myJScrollPane );
		}
		else if( source == myVariablesItem ){
			if( mySubmodelsItem != null ){ mySubmodelsItem.setState( true ); }
			myTypesItem.setState( true );
			tree.setExpandVariableBranches( myVariablesItem.getState() );
		}
		else if( source == mySubmodelsItem ){
			tree.setExpandSubmodelBranches( mySubmodelsItem.getState() );
		}
		else if( source == myTypesItem ){
			if( mySubmodelsItem != null ){ mySubmodelsItem.setState( true ); }
			tree.setExpandTypeBranches( myTypesItem.getState() );
		}
	}

	/** interface ItemListener
		@since 20030911 */
	public void itemStateChanged(ItemEvent e)
	{
		Object source = e.getSource();
		if( source == myJComboBox && myFlagListenCombo && e.getStateChange() == ItemEvent.SELECTED )
		{
			doItemSelected();
		}
	}

	/** @since 20030911 */
	private void doItemSelected()
	{
		EnumProperty selected = (EnumProperty) myJComboBox.getSelectedItem();
		revalidateEvidenceTree( selected );
	}
}
