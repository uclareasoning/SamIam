package edu.ucla.belief.ui.internalframes;

import edu.ucla.util.code.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.util.*;

/**
	@author Keith Cascio
	@since 050604
*/
public class CodeOptionsPanel extends JPanel implements edu.ucla.util.code.Tree// implements ActionListener
{
	public CodeOptionsPanel( CodeGenius genius )
	{
		super( new GridBagLayout() );
		myCodeGenius = genius;
		init();
		return;
	}

	public static boolean FLAG_DEBUG_BORDERS = false;
	public static final Color COLOR_BACKGROUND_TEXT = Color.lightGray;
	public static final Color COLOR_BACKGROUND_TREE = Color.lightGray;

	public void init()
	{
		myPanelPlaceholder = new JPanel( new GridLayout() );
		myPanelPlaceholder.add( myHelpPanel = makeHelpPanel() );

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		this.add( makeOptionsListPanel(), c );
		this.add( Box.createHorizontalStrut(4), c );
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.weighty = 1;
		this.add( myPanelPlaceholder, c );

		if( FLAG_DEBUG_BORDERS ) this.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );
	}

	public JComponent makeHelpPanel()
	{
		JPanel helpPanel = new JPanel( new GridLayout() );// new GridBagLayout() );
		//GridBagConstraints c = new GridBagConstraints();
		//c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.BOTH;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		//c.weightx = 1;
		//c.weighty = 1;

		myTextAreaHelp = new JTextArea();
		myTextAreaHelp.setEditable( false );
		myTextAreaHelp.setLineWrap( true );
		myTextAreaHelp.setWrapStyleWord( true );
		myTextAreaHelp.setBackground( COLOR_BACKGROUND_TEXT );
		//Border borderExists = myTextAreaHelp.getBorder();
		//Border borderOutside = BorderFactory.createLineBorder( Color.gray, 1 );
		//Border borderCompound = BorderFactory.createCompoundBorder( borderOutside, borderExists );
		//myTextAreaHelp.setBorder( borderCompound );
		myTextAreaHelp.setMargin( new Insets( 0, 4, 0, 2 ) );
		myTextAreaHelp.setText( "Click an option for help." );

		myJScrollPaneText = new JScrollPane( myTextAreaHelp );
		helpPanel.add( myJScrollPaneText );//, c );

		if( FLAG_DEBUG_BORDERS ) helpPanel.setBorder( BorderFactory.createLineBorder( Color.green, 1 ) );

		return helpPanel;
	}

	public void scrollToBeginning(){
		//RECT_UTIL.setBounds( 1,1,2,2 );//(x,y,width,height)
		//scrollHelpTo( RECT_UTIL );
		myJScrollPaneText.getViewport().setViewPosition( new Point(0,0) );
		myTextAreaHelp.repaint();
		//System.out.println( "view position: " + myJScrollPaneText.getViewport().getViewPosition() );
		//System.out.println( "view rect: " + myJScrollPaneText.getViewport().getViewRect() );
	}

	public void scrollToEnd(){
		int heightText = myTextAreaHelp.getHeight();
		RECT_UTIL.setBounds( 0,heightText,0,heightText );//(x,y,width,height)
		scrollHelpTo( RECT_UTIL );
	}

	public void scrollHelpTo( Rectangle rectangle )
	{
		if( myJScrollPaneText != null ){
			myJScrollPaneText.getViewport().scrollRectToVisible( rectangle );
		}
	}

	public void setHelp( CodeOption option ){
		setHelpText( option.getHelpText() );
	}

	public void setHelpText( String text )
	{
		if( !myPanelPlaceholder.isAncestorOf( myHelpPanel ) ){
			myPanelPlaceholder.removeAll();
			myPanelPlaceholder.add( myHelpPanel );
			myPanelPlaceholder.revalidate();
			myPanelPlaceholder.repaint();
		}

		myTextAreaHelp.setText( text );
		scrollToBeginning();
	}

	public void describeDependencies( CodeGenius genius )
	{
		if( myPanelPlaceholder.isAncestorOf( myPanelDependencies ) ) return;

		if( myJTree == null ){
			myPanelDependencies = makePanelDependencies();
		}

		if( myCurrentlyDescribing != genius ){
			myTreeRoot = new DefaultMutableTreeNode();
			genius.describeDependencies( (edu.ucla.util.code.Tree)this );
			myTreeModel = new DefaultTreeModel( myTreeRoot );
			myJTree.setModel( myTreeModel );
			myCurrentlyDescribing = genius;
		}

		expandAllTree();
		myPanelPlaceholder.removeAll();
		myPanelPlaceholder.add( myPanelDependencies );
		myPanelPlaceholder.revalidate();
		myPanelPlaceholder.repaint();
	}

	public JComponent makePanelDependencies()
	{
		myJTree = new JTree();
		myJTree.putClientProperty( "JTree.lineStyle", "Angled" );
		myJTree.setRootVisible( false );//false );
		myJTree.setEditable( false );
		myJTree.setShowsRootHandles( true );
		myJTree.setCellRenderer( new DependencyListTreeCellRenderer() );
		myJScrollPaneTree = new JScrollPane( myJTree );

		myJTree.setBackground( COLOR_BACKGROUND_TREE );
		myJScrollPaneTree.setBackground( COLOR_BACKGROUND_TREE );

		JComponent ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( new JLabel( "The following influence code output:" ), c );//determine,alter,influence,change,induce

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		ret.add( myJScrollPaneTree, c );

		return ret;
	}

	public void addChildOfRootNode( Object value ){
		myTreeCurrent = new DefaultMutableTreeNode( value );
		myTreeRoot.add( myTreeCurrent );
	}

	public void addChildOfLastNode( Object value ){
		DefaultMutableTreeNode child = new DefaultMutableTreeNode( value );
		myTreeCurrent.add( child );
		myTreeCurrent = child;
	}

	public void lastNodeGetsParentLastNode(){
		if( myTreeCurrent != null && myTreeCurrent != myTreeRoot ){
			myTreeCurrent = (DefaultMutableTreeNode) myTreeCurrent.getParent();
		}
	}

	public void expandAllTree(){
		//int count = myJTree.getRowCount();
		//for( int i = 0; i<count; i++ ){
		//	myJTree.expandPath( myJTree.getPathForRow( i ) );
		//}
		expandLeavesBelow( myTreeRoot );
	}

	public void expandLeavesBelow( TreeNode node )
	{
		//System.out.println( "CodeOptionsPanel.expandLeavesBelow( "+node+" )" );
		//if( node.isLeaf() ){
		//	System.out.println( "\t node.isLeaf()" );
			myJTree.expandPath( new TreePath( ((DefaultMutableTreeNode)node).getPath() ) );
		//}
		//else{
			for( Enumeration enumeration = node.children(); enumeration.hasMoreElements(); ){
				expandLeavesBelow( (TreeNode) enumeration.nextElement() );
			}
		//}
	}

	private static final Rectangle RECT_UTIL = new Rectangle(0,0,0,0);

	public JComponent makeOptionsListPanel()
	{
		JPanel optionsListPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		CodeOption[] options = myCodeGenius.getOptions();
		if( myMapOptionsToComponents == null ) myMapOptionsToComponents = new HashMap();
		else myMapOptionsToComponents.clear();

		CodeOption option = null;
		JLabel label = null;
		OptionHelpListener listener = null;
		for( int i=0; i<options.length; i++ )
		{
			option = options[i];
			listener = new OptionHelpListener( option );
			c.gridwidth = 1;
			c.fill = GridBagConstraints.NONE;
			label = new JLabel( option.describe() + ": " );
			label.addMouseListener( listener );
			optionsListPanel.add( label, c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.fill = GridBagConstraints.HORIZONTAL;
			optionsListPanel.add( makeComponent( option, listener ), c );
		}

		if( FLAG_DEBUG_BORDERS ) optionsListPanel.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );

		return optionsListPanel;
	}

	/** @since 021505 */
	public JComponent getComponent( CodeOption option ){
		if( myMapOptionsToComponents == null ) return (JComponent)null;
		else return (JComponent) myMapOptionsToComponents.get( option );
	}

	public JComponent makeComponent( CodeOption option, OptionHelpListener listener )
	{
		JComponent ret = null;
		if( option.isFlag() )
		{
			JCheckBox box = new JCheckBox();
			box.setSelected( option.getDefaultFlag() );
			box.addActionListener( listener );//this );
			ret = box;
		}
		else
		{
			JComboBox box = new JComboBox( option.getValues() );
			box.setSelectedItem( option.getDefault() );
			box.addActionListener( listener );//this );
			box.addPopupMenuListener( listener );
			ret = box;
		}

		myMapOptionsToComponents.put( option, ret );
		return ret;
	}

	public class DependencyListTreeCellRenderer extends DefaultTreeCellRenderer
	{
		public DependencyListTreeCellRenderer()
		{
			//Icon iconLeaf = getLeafIcon();
			//setOpenIcon( iconLeaf );
			//setClosedIcon( iconLeaf );
			//setLeafIcon( null );
			this.setOpenIcon( null );
			this.setClosedIcon( null );
			this.setLeafIcon( null );
			this.setBackground( COLOR_BACKGROUND_TREE );
			this.setBackgroundNonSelectionColor( COLOR_BACKGROUND_TREE );
		}
	}

	public class OptionHelpListener extends MouseAdapter implements PopupMenuListener, ActionListener
	{
		public OptionHelpListener( CodeOption option ){
			myCodeOption = option;
		}

		public void mouseClicked(MouseEvent e){
			foo();
		}

		public void popupMenuWillBecomeVisible(PopupMenuEvent e){
			foo();
		}
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e){}
		public void popupMenuCanceled(PopupMenuEvent e){}

		public void actionPerformed( ActionEvent e ){
			if( myCodeOption.isFlag() ){
				setHelp( myCodeOption );
				JCheckBox box = (JCheckBox)e.getSource();
				myCodeGenius.setFlag( myCodeOption, box.isSelected() );
			}
			else{
				JComboBox box = (JComboBox)e.getSource();
				myCodeGenius.setOption( myCodeOption, (CodeOptionValue) box.getSelectedItem() );
			}
		}

		public void foo(){
			setHelp( myCodeOption );
		}

		private CodeOption myCodeOption;
	}

	private CodeGenius myCodeGenius;
	private JTextArea myTextAreaHelp;
	private JScrollPane myJScrollPaneText;
	private JComponent myHelpPanel;
	private JTree myJTree;
	private TreeModel myTreeModel;
	private JScrollPane myJScrollPaneTree;
	private JComponent myPanelPlaceholder;
	private DefaultMutableTreeNode myTreeRoot;
	private DefaultMutableTreeNode myTreeCurrent;
	private CodeGenius myCurrentlyDescribing;
	private JComponent myPanelDependencies;
	private Map myMapOptionsToComponents;
}
