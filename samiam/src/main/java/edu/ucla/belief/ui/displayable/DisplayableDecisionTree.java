package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.util.Util;
//import edu.ucla.belief.ui.util.Interruptable;

import edu.ucla.belief.*;
import edu.ucla.belief.decision.*;
import edu.ucla.util.Stringifier;
import edu.ucla.structure.RecursiveDepthFirstIterator;
import edu.ucla.belief.io.dsl.DSLNodeType;
import edu.ucla.util.Interruptable;

import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.awt.Component;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionListener;

/** @author Keith Cascio
	@since 120804 */
public class DisplayableDecisionTree extends JPanel
	implements DecisionTree, MenuVariableSelector.Listener, MenuAssignment.Listener,
		GroupingColoringTableModel.Listener,
		DecisionListener,
		Comparator,
		MenuNodeSelector.Listener,
		MouseListener, ActionListener, PopupMenuListener, CellEditorListener
{
	public static final String STR_ICONLEAF_FILENAME = "icon_decisiontree_leaf_16x13.gif";
	public static final String STR_ICONOPEN_FILENAME = "icon_decisiontree_open_16x13.gif";
	public static final String STR_ICONCLOSED_FILENAME = "icon_decisiontree_closed_16x13.gif";

	public DisplayableDecisionTree( TableIndex tableindex ){
		this( new DecisionTreeImpl( tableindex ) );
	}

	public DisplayableDecisionTree( DecisionTreeImpl dti ){
		super( new GridBagLayout() );
		this.myTree = dti;
		this.myFactory = dti;
		init();
	}

	public Factory getFactory(){
		return this.myFactory;
	}

	/** @since 011005 */
	public boolean isEditable(){
		return this.myFlagEditable;
	}

	/** @since 011005 */
	public void setEditable( boolean flag ){
		this.myFlagEditable = flag;
	}

	/** interface DecisionTree */
	public void snapshot(){
		myTree.snapshot();
	}

	/** interface DecisionTree */
	public boolean restoreSnapshot(){
		return myTree.restoreSnapshot();
	}

	/** interface DecisionTree */
	public void ensureSnapshot(){
		myTree.ensureSnapshot();
	}

	/** interface DecisionTree */
	public DecisionBackup getSnapshot(){
		return myTree.getSnapshot();
	}

	/** interface DecisionTree */
	public void setSnapshot( DecisionBackup snaoshot ){
		myTree.setSnapshot( snaoshot );
	}

	/** interface DecisionTree */
	public double getParameter( final int[] indices ){
		return myTree.getParameter( indices );
	}

	/** interface DecisionTree */
	public DecisionLeaf getLeaf( final int[] indices ){
		return myTree.getLeaf( indices );
	}

	/** interface DecisionTree */
	public DecisionNode getRoot(){
		return myTree.getRoot();
	}

	/** interface DecisionTree */
	public TableIndex getIndex(){
		return myTree.getIndex();
	}

	/** interface DecisionTree */
	public Table expand(){
		return myTree.expand();
	}

	/** interface DecisionTree */
	public void normalize(){
		myTree.normalize();
	}

	/** interface DecisionTree */
	public void addListener( DecisionListener listener ){
		myTree.addListener( listener );
	}

	/** interface DecisionTree */
	public boolean removeListener( DecisionListener listener ){
		return myTree.removeListener( listener );
	}

	/** JTree convenience */
	public void addTreeSelectionListener( TreeSelectionListener tsl ){
		myJTree.addTreeSelectionListener( tsl );
	}

	/** JTree convenience */
	public void removeTreeSelectionListener( TreeSelectionListener tsl ){
		myJTree.removeTreeSelectionListener( tsl );
	}

	/** @author Keith Cascio @since 120904 */
	public class TreeNode extends DefaultMutableTreeNode
	{
		public TreeNode( DecisionNode decisionnode ){
			super();
			setDecisionNode( decisionnode );
		}

		public void setDecisionNode( DecisionNode decisionnode ){
			this.myDecisionNode = decisionnode;
			super.setUserObject( myDecisionNode );
		}

		public void setUserObject( Object userObject )
		{
			if( myDecisionNode == null ){
				super.setUserObject( userObject );
				return;
			}

			String id = userObject.toString();
			if( id.equals( myDecisionNode.toString() ) ) return;

			if( myFactory != null ){
				if( !myFactory.isValidID( id ) ){
					DisplayableDecisionTree.warnInvalidID( id, "node", (Component)DisplayableDecisionTree.this );
					return;
				}
				boolean unique = myFactory.isUniqueNodeID( id );
				if( !unique ) if( !DisplayableDecisionTree.promptNonUniqueID( id, "node", (Component)DisplayableDecisionTree.this ) ) return;
			}
			myDecisionNode.setID( id );
		}

		public DecisionNode getDecisionNode(){
			return myDecisionNode;
		}

		private DecisionNode myDecisionNode;
	}

	public static boolean promptNonUniqueID( String id, String objecttype, Component parent )
	{
		String message = "A "+objecttype+" with id \"" +id+ "\" already exists.\nAre you sure you want to give this "+objecttype+" the identical name?";
		String title = "Warning: Non-Unique ID";
		int result = JOptionPane.showConfirmDialog( parent, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
		return result == JOptionPane.YES_OPTION;
	}

	public static void warnInvalidID( String id, String objecttype, Component parent )
	{
		String message = "Id \"" +id+ "\" is invalid.";
		String title = "Error: Invalid ID";
		JOptionPane.showMessageDialog( parent, message, title, JOptionPane.ERROR_MESSAGE );
	}

	private void init()
	{
		myTree.addListener( (DecisionListener)this );

		TreeNode root = initRec( getRoot() );
		initJTree( root );

		this.myJScrollPane = new JScrollPane( myJTree );
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		this.add( myJScrollPane, c );
	}

	/** interface DecisionListener */
	public void decisionEvent( DecisionEvent e ){
		//System.out.println( "DisplayableDecisionTree.decisionEvent( "+e+" )" );
		if( e.type == DecisionEvent.RENAME ){ return; }
		//else if( e.type == DecisionEvent.ASSIGNMENT_CHANGE ){}
		//else if( e.type == DecisionEvent.PARENT_INSERTED ){}
		//else if( e.type == DecisionEvent.DERACINATED ){}
		reinit();
	}

	/** @since 011405 */
	private void reinit(){
		if( myReinitRunner == null ) myReinitRunner = new ReinitRunner();
		myReinitRunner.start();
	}

	/** @since 011405 */
	private class ReinitRunner extends Interruptable{
		public void runImpl( Object arg1 ) throws InterruptedException{
			DisplayableDecisionTree.this.reinitImpl();
		}
		//public boolean debug() { return true; }
		//public boolean verbose() { return true; }
		//public String getNameMethodOfInterest(){ return "reinitImpl"; }
	}

	/** @since 011405 */
	private static void ci() throws InterruptedException{
		Thread.sleep(4);//Interruptable.checkInterrupted();
	}

	/** @since 011405 */
	private void reinitImpl() throws InterruptedException
	{
		ci();
		DecisionTreeImpl dti = (DecisionTreeImpl)myTree;
		if( !dti.isAcyclic() ){
			RecursiveDepthFirstIterator rdfi = new RecursiveDepthFirstIterator( dti );
			if( rdfi.isCyclic() ){
				System.out.println( "DisplayableDecisionTree.reinit() found cycle:" );
				System.out.println( rdfi.cycleToString( rdfi.getCycle(), (Stringifier)null ) );
			}
			return;
		}
		ci();
		recordSelectionPath();
		ci();
		recordExpandedPaths();
		ci();
		recordViewRect();
		ci();
		//normalize();//taken care of in getRoot()
		TreeNode root = initRec( getRoot() );
		ci();
		myJTree.setModel( new DefaultTreeModel( root ) );
		ci();
		rememberExpandedPaths();
		ci();
		rememberSelectionPath();
		ci();
		rememberViewRect();
	}

	/** @since 011405 */
	private void recordViewRect(){
		myViewRect = myJScrollPane.getViewport().getViewRect();
	}

	/** @since 011405 */
	private void rememberViewRect(){
		if( myViewRect != null ) myJScrollPane.getViewport().scrollRectToVisible( myViewRect );
	}

	/** @since 011405 */
	private void recordSelectionPath(){
		mySelectionPath = myJTree.getSelectionPath();
	}

	/** @since 011405 */
	private void rememberSelectionPath(){
		if( mySelectionPath == null ) return;
		TreePath match = match( mySelectionPath, (DefaultMutableTreeNode) myJTree.getModel().getRoot() );
		if( match != null ) myJTree.setSelectionPath( match );
	}

	/** @since 011405 */
	private void recordExpandedPaths(){
		//System.out.println( "DDT.recordExpandedPaths()" );
		if( myExpandedPaths == null ) myExpandedPaths = new LinkedList();
		else myExpandedPaths.clear();

		TreePath pathroot = new TreePath( myJTree.getModel().getRoot() );

		Enumeration enumer = myJTree.getExpandedDescendants( pathroot );
		if( enumer == null ) return;
		while( enumer.hasMoreElements() ){
			myExpandedPaths.add( enumer.nextElement() );
		}
		//System.out.println( "    recorded " + myExpandedPaths.size() + " expanded paths" );
	}

	/** @since 011405 */
	private void rememberExpandedPaths(){
		//System.out.println( "DDT.recordExpandedPaths()" );
		if( (myExpandedPaths == null) || myExpandedPaths.isEmpty() ) return;

		DefaultMutableTreeNode newroot = (DefaultMutableTreeNode) myJTree.getModel().getRoot();
		int count = 0;
		TreePath match;
		for( Iterator it = myExpandedPaths.iterator(); it.hasNext(); ){
			match = match( (TreePath) it.next(), newroot );
			if( match != null ){
				myJTree.expandPath( match );
				++count;
			}
		}
		//System.out.println( "    remembered " + count + " expanded paths" );
	}

	/** @since 011405 */
	private static TreePath match( TreePath original, DefaultMutableTreeNode newroot )
	{
		synchronized( SYNCH_MATCH )
		{
		if( LIST_UTIL_MATCH == null ) LIST_UTIL_MATCH = new LinkedList();
		else LIST_UTIL_MATCH.clear();

		Object[] arrayoriginal = original.getPath();

		DefaultMutableTreeNode mtnmatch = newroot;
		DefaultMutableTreeNode mtnoriginal = (DefaultMutableTreeNode) arrayoriginal[0];

		if( mtnmatch.getUserObject() != mtnoriginal.getUserObject() ) return null;

		LIST_UTIL_MATCH.add( mtnmatch );

		for( int i=1; i<arrayoriginal.length; i++ ){
			mtnoriginal = (DefaultMutableTreeNode) arrayoriginal[i];
			mtnmatch = findChild( mtnmatch, mtnoriginal.getUserObject() );
			if( mtnmatch == null ) break;
			else LIST_UTIL_MATCH.add( mtnmatch );
		}

		//System.out.println( "    matched leaf " + LIST_UTIL_MATCH.getLast() );

		return new TreePath( LIST_UTIL_MATCH.toArray( new DefaultMutableTreeNode[LIST_UTIL_MATCH.size()] ) );
		}
	}

	/** @since 011405 */
	public static DefaultMutableTreeNode findChild( DefaultMutableTreeNode parent, Object userobject ){
		DefaultMutableTreeNode child;
		for( Enumeration enumer = parent.children(); enumer.hasMoreElements(); ){
			child = (DefaultMutableTreeNode) enumer.nextElement();
			if( child.getUserObject() == userobject ) return child;
		}
		return null;
	}

	private void initJTree( DefaultMutableTreeNode root )
	{
		this.myJTree = new JTree( root );
		myJTree.setRootVisible( true );
		myJTree.setEditable( true );
		myJTree.getCellEditor().addCellEditorListener( (CellEditorListener)this );
		myJTree.setEditable( false );
		myJTree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );

		DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer)myJTree.getCellRenderer();

		//Icon iconleaf = renderer.getLeafIcon();
		//Icon iconopen = renderer.getOpenIcon();
		//Icon iconclosed = renderer.getClosedIcon();

		Icon iconleaf = MainToolBar.getIcon( STR_ICONLEAF_FILENAME );
		if( iconleaf != null ) renderer.setLeafIcon( iconleaf );
		Icon iconOpen = MainToolBar.getIcon( STR_ICONOPEN_FILENAME );
		if( iconOpen != null ) renderer.setOpenIcon( iconOpen );
		Icon iconClosed = MainToolBar.getIcon( STR_ICONCLOSED_FILENAME );
		if( iconClosed != null ) renderer.setClosedIcon( iconClosed );

		//System.out.println( "JTree icon leaf: " + iconleaf.getIconWidth() + "x" + iconleaf.getIconHeight() );
		//System.out.println( "JTree icon open: " + iconopen.getIconWidth() + "x" + iconopen.getIconHeight() );
		//System.out.println( "JTree icon closed: " + iconclosed.getIconWidth() + "x" + iconclosed.getIconHeight() );

		initPopupMenus();
		myJTree.addMouseListener( (MouseListener)this );
	}

	private void initPopupMenus(){
		myPopupMenuTree = new JPopupMenu( "tree" );
		myPopupMenuTree.add( myItemStats = new JMenuItem( "show stats" ) );
		myItemStats.addActionListener( (ActionListener)this );
		myPopupMenuTree.add( myItemCompareCPT = new JMenuItem( "compare to CPT" ) );
		myItemCompareCPT.addActionListener( (ActionListener)this );
		myPopupMenuTree.add( myItemSaveImage = new JMenuItem( "save image" ) );
		myItemSaveImage.addActionListener( (ActionListener)this );
		myItemSaveImage.setEnabled( false );

		myPopupMenuLeaf = new JPopupMenu( "leaf" );
		myPopupMenuLeaf.addPopupMenuListener( (PopupMenuListener)this );

		myMenuInsertBeforeLeaf = new MenuVariableSelector( "insert decision before", getIndex() );
		myMenuInsertBeforeLeaf.addListener( (MenuVariableSelector.Listener)this );
		myPopupMenuLeaf.add( myMenuInsertBeforeLeaf );

		myPopupMenuLeaf.add( myMenuFindParentLeaf = new MenuNodeSelector( "parents", (MenuAssignment.Key)null ) );
		myMenuFindParentLeaf.addListener( (MenuNodeSelector.Listener)this );

		myPopupMenuLeaf.add( myItemRenameLeaf = new JMenuItem( "rename" ) );
		myItemRenameLeaf.addActionListener( (ActionListener)this );

		myPopupMenuLeaf.add( myItemMakeRootLeaf = new JMenuItem( "make root" ) );
		myItemMakeRootLeaf.addActionListener( (ActionListener)this );

		myPopupMenuInternal = new JPopupMenu( "internal" );
		myPopupMenuInternal.addPopupMenuListener( (PopupMenuListener)this );

		myMenuAssignDecision = new MenuAssignment( "assign decision", (DecisionTree)this, myFactory );
		myMenuAssignDecision.addListener( (MenuAssignment.Listener)this );
		myPopupMenuInternal.add( myMenuAssignDecision );

		myMenuInsertBeforeInternal = new MenuVariableSelector( "insert decision before", getIndex() );
		myMenuInsertBeforeInternal.addListener( (MenuVariableSelector.Listener)this );
		myPopupMenuInternal.add( myMenuInsertBeforeInternal );

		myPopupMenuInternal.add( myMenuFindParentInternal = new MenuNodeSelector( "parents", (MenuAssignment.Key)null ) );
		myMenuFindParentInternal.addListener( (MenuNodeSelector.Listener)this );

		myPopupMenuInternal.add( myItemRenameInternal = new JMenuItem( "rename" ) );
		myItemRenameInternal.addActionListener( (ActionListener)this );

		myPopupMenuInternal.add( myItemMakeRootInternal = new JMenuItem( "make root" ) );
		myItemMakeRootInternal.addActionListener( (ActionListener)this );
	}

	/** @since 011605
		Allows user to expand/select/scroll to the first
		occurrence of an arbitrary node in a breadth-first
		traversal of the tree. */
	public void goToBreadthFirst( DecisionNode dnode ){
		javax.swing.tree.TreeNode[] arraypath = breadthFirstPathForNode( dnode );
		if( arraypath == null ) return;

		TreePath treepath = new TreePath( arraypath );

		myJTree.expandPath( treepath );
		myJTree.setSelectionPath( treepath );
		Rectangle bounds = myJTree.getRowBounds( myJTree.getRowForPath( treepath ) );
		myJScrollPane.getViewport().scrollRectToVisible( bounds );
	}

	/** @since 011605 */
	public javax.swing.tree.TreeNode[] breadthFirstPathForNode( DecisionNode dnode ){
		if( (dnode == null) || !myTree.contains( dnode ) ) return (javax.swing.tree.TreeNode[])null;

		DefaultMutableTreeNode root = (DefaultMutableTreeNode)myJTree.getModel().getRoot();
		DefaultMutableTreeNode child;
		for( Enumeration enumer = root.breadthFirstEnumeration(); enumer.hasMoreElements(); ){
			child = (DefaultMutableTreeNode) enumer.nextElement();
			if( child.getUserObject() == dnode ) return child.getPath();
		}

		return (javax.swing.tree.TreeNode[])null;
	}

	private TreeNode initRec( DecisionNode node ){
		TreeNode ret = new TreeNode( node );

		ArrayList sortable = new ArrayList();
		if( !node.isLeaf() ){
			sortable.clear();
			sortable.addAll( node.getChildDecisionNodes() );
			Collections.sort( sortable, (Comparator)this );
			for( Iterator it = sortable.iterator(); it.hasNext(); ){
				ret.add( initRec( (DecisionNode) it.next() ) );
			}
		}

		return ret;
	}

	/** interface Comparator */
	public int compare( Object o1, Object o2 ){
		return COLLATOR.compare( o1.toString(), o2.toString() );
	}

	public static final java.text.Collator COLLATOR = java.text.Collator.getInstance();

	/*
	private void refreshMenu( JMenu menu, DecisionNode dncurrent ){
		int itemcount = menu.getItemCount();
		for( int i=0; i<itemcount; i++ ) menu.getItem(i).setEnabled( true );
		setItemEnabled( menu, dncurrent, false );
		DecisionNode dnparent = dncurrent.getParent();
		if( dnparent != null ) setItemEnabled( menu, dnparent, false );
	}
	private void setItemEnabled( JMenu menu, DecisionNode dn, boolean flag ){
		FiniteVariable var = dn.getVariable();
		int ind = getIndex().variableIndex( var );
		menu.getItem( ind ).setEnabled( flag );
	}*/

	/** interface PopupMenuListener */
	public void popupMenuWillBecomeVisible(PopupMenuEvent e){
		Object src = e.getSource();
		boolean flagEditable = this.isEditable() && myLastNode.isEditable();
		if( src == myPopupMenuLeaf ){
			//myMenuInsertBeforeLeaf.setEnabled( flagEditable );
			myItemRenameLeaf.setEnabled( flagEditable );
			myItemMakeRootLeaf.setEnabled( this.isEditable() );
			validateMenuNodeSelector( myMenuFindParentLeaf, myLastNode );
		}
		else if( src == myPopupMenuInternal ){
			myMenuAssignDecision.setEnabled( flagEditable );
			//myMenuInsertBeforeInternal.setEnabled( this.isEditable() );
			myItemMakeRootInternal.setEnabled( this.isEditable() );
			myItemRenameInternal.setEnabled( flagEditable );
			validateMenuNodeSelector( myMenuFindParentInternal, myLastNode );
		}
	}
	/** interface PopupMenuListener */
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e){}
	/** interface PopupMenuListener */
	public void popupMenuCanceled(PopupMenuEvent e) {}

	/** @since 011605 */
	private static void validateMenuNodeSelector( MenuNodeSelector menuFindParent, DecisionNode node ){
		if( node == null ) menuFindParent.setEnabled( false );
		Set parents = node.getParents();
		if( (parents == null) || (parents.isEmpty()) ) menuFindParent.setEnabled( false );
		menuFindParent.setEnabled( true );
		menuFindParent.setContents( parents );
	}

	/** interface ActionListener */
	public void actionPerformed( ActionEvent e )
	{
		Object src = e.getSource();
		if( (src == myItemRenameLeaf) || (src == myItemRenameInternal) ) doRename();
		else if( (src == myItemMakeRootInternal) || (src == myItemMakeRootLeaf) ) doMakeRoot();
		else if( src == myItemSaveImage ) Util.saveImage( (Component)myJTree, "png" );
		else if( src == myItemStats ) doShowStats();
		else if( src == myButtonCopy ) copyStatsToSystemClipboard();
		else if( src == myItemCompareCPT ) doCompareCPT();
	}

	/** interface MenuVariableSelector.Listener */
	public void menuVariableSelected( MenuVariableSelector menu, FiniteVariable var, DecisionNode node, DecisionNode parent )
	{
		if( (var!=null) && (node!=null) ){
			if( (menu == myMenuInsertBeforeLeaf) || (menu == myMenuInsertBeforeInternal) ){
				myTree.ensureSnapshot();
				//if( node.insertParent( var, parent, myFactory ) ) reinit();
				node.insertParent( var, parent, myFactory );
			}
		}
	}

	/** interface MenuNodeSelector.Listener */
	public void menuNodeSelected( MenuNodeSelector menu, MenuAssignment.Key key, DecisionNode node ){
		if( (menu == myMenuFindParentLeaf) || (menu == myMenuFindParentInternal) ){
			goToBreadthFirst( node );
		}
	}

	/** interface GroupingColoringTableModel.Listener */
	public void tableEditWarning(){
		myTree.ensureSnapshot();
	}

	/** interface MenuAssignment.Listener */
	public void menuAssignmentSelected( MenuAssignment menu, DecisionNode node, Object[] selectedValues, MenuAssignment.Key key, DecisionNode selected, FiniteVariable variable )
	{
		//System.out.println( "DDT.mAssSel( n:"+node+", "+selectedValues.length+" vals, key:"+key+", sltd:"+selected+", var:"+variable+")" );
		if( (node!=null) && (selectedValues!=null) && (selectedValues.length>0) && (key!=null) )
		{
			myTree.ensureSnapshot();

			DecisionNode newnext = null;
			if( key == menu.NEWLEAF ) newnext = myFactory.newLeaf( getIndex().getJoint() );
			else if( key == menu.NEWINTERNAL ) newnext = myFactory.newInternal( variable );
			else if( key == menu.UNIFORM ) newnext = myFactory.getDefault();
			//else if( (key == menu.CHILDLEAF) || (key == menu.EXISTINGLEAF) || (key == menu.ORPHANLEAF) ) newnext = selected;
			else if( key.isExisting() ) newnext = selected;

			if( (newnext != null) || (key == menu.NEWLEAFEACHDISTINCT) || (key == menu.NEWINTERNALEACHDISTINCT) ){
				try{
					for( int i=0; i<selectedValues.length; i++ ){
						if( key == menu.NEWLEAFEACHDISTINCT ) newnext = myFactory.newLeaf( getIndex().getJoint() );
						else if( key == menu.NEWINTERNALEACHDISTINCT ) newnext = myFactory.newInternal( variable );
						node.setNext( selectedValues[i], newnext );
					}
				}catch( Exception e ){
					System.err.println( e );
					return;
				}
				//reinit();
			}
		}
	}

	//private DecisionInternal newInternal( FiniteVariable var ){
	//	DecisionInternal ret = myFactory.newInternal( var );
	//	ret.setNext( myFactory.getDefault() );
	//	return ret;
	//}

	/** @since 010905 */
	public void copyStatsToSystemClipboard(){
		if( myStats == null ) return;
		Util.copyToSystemClipboard( myStats.toString() );
	}

	/** @since 010905 */
	private void doShowStats(){
		if( myStats == null ) myStats = new DisplayableDecisionStats();
		myStats.analyzeDescendants( getRoot() );

		//JOptionPane.showMessageDialog( this, myStats.getGUI(), "Decision Tree Statistics", JOptionPane.PLAIN_MESSAGE );
		if( myButtonCopy == null ){
			myButtonCopy = new JButton( "Copy" );
			myButtonCopy.addActionListener( (ActionListener)this );
			ARRAY_OPTIONS[0] = myButtonCopy;
		}
		JOptionPane.showOptionDialog( this, myStats.getGUI(), "Decision Tree Statistics", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, (Icon)null, ARRAY_OPTIONS, STR_OPTION_OK );
	}

	/** @since 011405 */
	private void doCompareCPT(){
		Table cpt = this.getIndex().getJoint().getCPTShell( DSLNodeType.CPT ).getCPT();
		Table expansion = this.expand();
		double epsilon = (double)0.00000000000009;
		boolean flagee = cpt.epsilonEquals( expansion, epsilon );
		System.out.println( "cpt.epsilonEquals( expansion, "+epsilon+" ) ? " + flagee );
		if( !flagee ){
			System.out.println( "trouble is:" );
			System.out.println( "cpt -\n" + cpt.tableString() );
			System.out.println( "\n" );
			System.out.println( "expansion -\n" + expansion.tableString() );
		}
	}

	private boolean doRename()
	{
		if( myLastNode == null ) return false;
		myTree.ensureSnapshot();
		myJTree.setEditable( true );
		myJTree.startEditingAtPath( myLastPath );
		//myJTree.setEditable( false );
		return true;
	}

	private boolean doMakeRoot()
	{
		if( (myLastNode == null) || (myLastNode == getRoot()) ) return false;
		myTree.ensureSnapshot();
		myLastNode.deracinate();
		//reinit();
		return true;
	}

	/** interface CellEditorListener */
	public void editingStopped(ChangeEvent e){
		myJTree.setEditable( false );
	}
	/** interface CellEditorListener */
	public void editingCanceled(ChangeEvent e){
		myJTree.setEditable( false );
	}

	protected boolean showPopup( MouseEvent e )
	{
		if(	e.isPopupTrigger() ){
			TreePath path = myJTree.getPathForLocation( e.getX(), e.getY() );
			this.myLastPath = path;
			if( path != null ){
				Object last = path.getLastPathComponent();
				if( last instanceof TreeNode ){
					TreeNode lastdmtn = (TreeNode)last;
					DecisionNode dn = lastdmtn.getDecisionNode();

					DecisionNode parentdn = null;
					TreeNode parentdmtn = (TreeNode) lastdmtn.getParent();
					if( parentdmtn != null ) parentdn = parentdmtn.getDecisionNode();

					return showPopup( dn, parentdn, e );
				}
			}
			else myPopupMenuTree.show( myJTree, e.getX(), e.getY() );
		}

		return false;
	}

	protected boolean showPopup( DecisionNode dn, DecisionNode parent, MouseEvent e )
	{
		this.myLastNode = dn;
		this.myLastParent = parent;

		JPopupMenu pmenu = null;
		if( dn.isLeaf() ){
			pmenu = myPopupMenuLeaf;
			//refreshMenu( myMenuInsertBeforeLeaf, dn );
			myMenuInsertBeforeLeaf.configureForNewParent( dn, parent );
		}
		else{
			pmenu = myPopupMenuInternal;
			myMenuInsertBeforeInternal.configureForNewParent( dn, parent );
			myMenuAssignDecision.configure( dn );
		}

		pmenu.show( myJTree, e.getX(), e.getY() );
		return true;
	}

	/** interface MouseListener */
	public void mouseClicked(MouseEvent e){
		if( showPopup(e) ) return;
	}
	public void mousePressed(MouseEvent e){
		if( showPopup(e) ) return;
	}
	public void mouseReleased(MouseEvent e){
		if( showPopup(e) ) return;
	}
	public void mouseEntered(MouseEvent e){
	}
	public void mouseExited(MouseEvent e){
	}

	transient private TreePath myLastPath;
	transient private DecisionNode myLastNode;
	transient private DecisionNode myLastParent;

	private boolean myFlagEditable = false;
	private JPopupMenu myPopupMenuLeaf;
	private MenuVariableSelector myMenuInsertBeforeLeaf;
	private MenuNodeSelector myMenuFindParentLeaf;
	private JMenuItem myItemRenameLeaf;
	private JMenuItem myItemMakeRootLeaf;

	private JPopupMenu myPopupMenuInternal;
	private MenuVariableSelector myMenuInsertBeforeInternal;
	private MenuAssignment myMenuAssignDecision;
	private MenuNodeSelector myMenuFindParentInternal;
	private JMenuItem myItemRenameInternal;
	private JMenuItem myItemMakeRootInternal;

	private JPopupMenu myPopupMenuTree;
	private JMenuItem myItemSaveImage, myItemStats, myItemCompareCPT;
	private DisplayableDecisionStats myStats;
	private JButton myButtonCopy;
	public static final String STR_OPTION_OK = "OK";
	private static final Object[] ARRAY_OPTIONS = new Object[] { null, STR_OPTION_OK };

	private static LinkedList LIST_UTIL_MATCH;
	private static Object SYNCH_MATCH = new Object();

	private JTree myJTree;
	private JScrollPane myJScrollPane;
	private Collection myExpandedPaths;
	private TreePath mySelectionPath;
	private Rectangle myViewRect;
	private ReinitRunner myReinitRunner;
	private DecisionTreeImpl myTree;
	private Factory myFactory;
}
