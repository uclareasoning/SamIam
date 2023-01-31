package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.*;
import edu.ucla.belief.decision.DecisionNode;
//import edu.ucla.belief.decision.DecisionLeaf;
import edu.ucla.belief.decision.DecisionTree;
import edu.ucla.belief.decision.Factory;
import edu.ucla.belief.decision.Classifier;
import edu.ucla.util.WeakLinkedList;

import java.util.List;
//import java.util.ArrayList;
import java.util.Iterator;
//import java.util.Collection;
import java.util.HashSet;
//import java.util.Comparator;
//import java.util.Collections;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
//import javax.swing.event.ListDataListener;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
//import java.awt.Color;
//import java.awt.GridBagLayout;
//import java.awt.GridBagConstraints;
//import java.awt.Dimension;

/** @author Keith Cascio
	@since 120904 */
public class MenuAssignment extends JMenu implements MenuNodeSelector.Listener, MenuVariableSelector.Listener,
		ActionListener, ListSelectionListener//, MenuListener//, Comparator
{
	public final Key NEWLEAF = new Key( "new" );
	public final Key NEWLEAFEACHDISTINCT = new Key( "new (each distinct)" );
	public final Key UNIFORM = new Key( "uniform" );
	public final NodeSelectorKey CHILDLEAF = new NodeSelectorKey( "existing child" )
	{
		protected void validateHook(){
			if( myNodeLast == null ) return;
			synchronized( myUtilSet ){
			myUtilSet.clear();
			DecisionNode child;
			for( Iterator it = myNodeLast.getChildDecisionNodes().iterator(); it.hasNext(); ){
				if( (child = (DecisionNode) it.next()).isLeaf() ) myUtilSet.add( child );
			}
			myUtilSet.remove( myFactory.getDefault() );
			myMenu.setContents( myUtilSet );
			}
		}
	};
	public final NodeSelectorKey EXISTINGLEAF = new NodeSelectorKey( "existing global" )
	{
		protected void validateHook(){
			//System.out.println( "MenuAssignment.validateMenuExisting()" );
			synchronized( EXISTINGLEAF ){
			if( mySetExistingLeaves == null ) mySetExistingLeaves = new HashSet();
			mySetExistingLeaves.clear();
			myDecisionTree.getRoot().getLeaves( mySetExistingLeaves );
			mySetExistingLeaves.remove( myFactory.getDefault() );
			myMenu.setContents( mySetExistingLeaves );
			}
		}
	};
	public final NodeSelectorKey ORPHANLEAF = new NodeSelectorKey( "orphaned" )
	{
		protected void validateHook(){
			MenuAssignment.this.EXISTINGLEAF.validate();
			synchronized( myUtilSet ){
			myUtilSet.clear();
			myUtilSet.addAll( myFactory.getLeafHistory() );
			myUtilSet.removeAll( mySetExistingLeaves );
			myMenu.setContents( myUtilSet );
			}
		}
	};

	public final VariableSelectorKey NEWINTERNAL = new VariableSelectorKey( "new" );
	public final VariableSelectorKey NEWINTERNALEACHDISTINCT = new VariableSelectorKey( "new (each distinct)" );
	public final NodeSelectorKey CHILDINTERNAL = new NodeSelectorKey( "existing child" )
	{
		protected void validateHook(){
			if( myNodeLast == null ) return;
			synchronized( myUtilSet ){
			myUtilSet.clear();
			DecisionNode child;
			for( Iterator it = myNodeLast.getChildDecisionNodes().iterator(); it.hasNext(); ){
				if( !(child = (DecisionNode) it.next()).isLeaf() ) myUtilSet.add( child );
			}
			myUtilSet.remove( myFactory.getDefault() );
			myMenu.setContents( myUtilSet );
			}
		}
	};
	public final NodeSelectorKey EXISTINGINTERNAL = new NodeSelectorKey( "existing non-ancestor" )
	{
		protected void validateHook(){
			synchronized( EXISTINGLEAF ){
				if( mySetExistingInternals == null ) mySetExistingInternals = new HashSet();
				mySetExistingInternals.clear();
				myDecisionTree.getRoot().getDescendants( mySetExistingInternals, Classifier.INTERNALS );
				if( mySetAncestors == null ) mySetAncestors = new HashSet();
				mySetAncestors.clear();
				myNodeLast.getAncestors( mySetAncestors, Classifier.ALL );
				synchronized( myUtilSet ){
					myUtilSet.clear();
					myUtilSet.addAll( mySetExistingInternals );
					myUtilSet.removeAll( mySetAncestors );
					myMenu.setContents( myUtilSet );
				}
			}
		}
	};
	public final NodeSelectorKey ORPHANINTERNAL = new NodeSelectorKey( "orphaned" )
	{
		protected void validateHook(){
			MenuAssignment.this.EXISTINGINTERNAL.validate();
			synchronized( myUtilSet ){
			myUtilSet.clear();
			myUtilSet.addAll( myFactory.getInternalHistory() );
			myUtilSet.removeAll( mySetExistingInternals );
			myMenu.setContents( myUtilSet );
			}
		}
	};

	public final Key[] ARRAY_LEAF = new Key[] { NEWLEAF, NEWLEAFEACHDISTINCT, UNIFORM, CHILDLEAF, EXISTINGLEAF, ORPHANLEAF };
	public final Key[] ARRAY_INTERNAL = new Key[] { NEWINTERNAL, NEWINTERNALEACHDISTINCT, CHILDINTERNAL, EXISTINGINTERNAL, ORPHANINTERNAL };
	public final Key[] ARRAY_KEYS = new Key[] { NEWLEAF, NEWLEAFEACHDISTINCT, UNIFORM, CHILDLEAF, EXISTINGLEAF, ORPHANLEAF, NEWINTERNAL, NEWINTERNALEACHDISTINCT, CHILDINTERNAL, EXISTINGINTERNAL, ORPHANINTERNAL };

	public MenuAssignment( String title, DecisionTree decisiontree, Factory factory, GroupingColoringJTable jtable ){
		super( title );
		this.myDecisionTree = decisiontree;
		this.myFactory = factory;
		this.myGroupingColoringJTable = jtable;
		init();
	}

	public MenuAssignment( String title, DecisionTree decisiontree, Factory factory ){
		this( title, decisiontree, factory, (GroupingColoringJTable)null );
	}

	public void configure( DecisionNode node )
	{
		//System.out.println( "MenuAssignment.configure( "+node+" ), ARRAY_KEYS.length: " + ARRAY_KEYS.length );
		this.myNodeLast = node;

		for( int i=0; i<ARRAY_KEYS.length; i++ ) ARRAY_KEYS[i].configure();

		if( myListModel != null ) myListModel.configure( myNodeLast );
		if( myMenuList != null ) validateMenus();
	}

	private void init()
	{
		if( myGroupingColoringJTable == null ){
			this.add( myMenuList = new MenuList( myListModel = new Model() ) );
			myMenuList.addListSelectionListener( (ListSelectionListener)this );
			this.addSeparator();
		}
		this.add( myMenuAssignToLeaf = new JMenu( "to leaf" ) );
		this.add( myMenuAssignToInternal = new JMenu( "to internal" ) );

		for( int i=0; i<ARRAY_LEAF.length; i++ ) ARRAY_LEAF[i].init( myMenuAssignToLeaf );
		for( int i=0; i<ARRAY_INTERNAL.length; i++ ) ARRAY_INTERNAL[i].init( myMenuAssignToInternal );
	}

/*	private void validateMenuChildren(){
		if( myFlagLeafChildrenClean ) return;
		if( myNodeLast == null ) return;
		synchronized( myUtilSet ){
		myUtilSet.clear();
		DecisionNode child;
		for( Iterator it = myNodeLast.getChildDecisionNodes().iterator(); it.hasNext(); ){
			if( (child = (DecisionNode) it.next()).isLeaf() ) myUtilSet.add( child );
		}
		myUtilSet.remove( myFactory.getDefault() );
		//validateMenu( myMenuLeafChildren, CHILDLEAF, myUtilSet );
		myMenuLeafChildren.setContents( myUtilSet );
		myFlagLeafChildrenClean = true;
		}
	}

	private void validateMenuExisting(){
		if( myFlagLeafExistingClean ) return;
		//System.out.println( "MenuAssignment.validateMenuExisting()" );
		synchronized( EXISTINGLEAF ){
		if( mySetExistingLeaves == null ) mySetExistingLeaves = new HashSet();
		mySetExistingLeaves.clear();
		myDecisionTree.getRoot().getLeaves( mySetExistingLeaves );
		mySetExistingLeaves.remove( myFactory.getDefault() );
		//validateMenu( myMenuLeafExisting, EXISTINGLEAF, mySetExistingLeaves );
		myMenuLeafExisting.setContents( myUtilSet );
		myFlagLeafExistingClean = true;
		}
	}

	private void validateMenuOrphans(){
		if( myFlagLeafOrphansClean ) return;
		validateMenuExisting();
		synchronized( myUtilSet ){
		myUtilSet.clear();
		myUtilSet.addAll( myFactory.getLeafHistory() );
		myUtilSet.removeAll( mySetExistingLeaves );
		//validateMenu( myMenuLeafOrphans, ORPHANLEAF, myUtilSet );
		myMenuLeafOrphans.setContents( myUtilSet );
		myFlagLeafOrphansClean = true;
		}
	}*/

	/** interface ListSelectionListener */
	public void valueChanged(ListSelectionEvent e){
		validateMenus();
	}

	public void validateMenus(){
		boolean enabled = true;
		if( myMenuList != null ) enabled = !myMenuList.isSelectionEmpty();
		else if( myGroupingColoringJTable != null ) enabled = !myGroupingColoringJTable.isSelectionEmpty();
		myMenuAssignToLeaf.setEnabled( enabled );
		myMenuAssignToInternal.setEnabled( enabled );
	}

	/** interface ActionListener */
	public void actionPerformed( ActionEvent e ){
		Item vi = (Item) e.getSource();
		fireMenuAssignmentSelected( vi.key, vi.node, (FiniteVariable)null );
	}

	/** interface MenuNodeSelector.Listener */
	public void menuNodeSelected( MenuNodeSelector menu, Key key, DecisionNode node ){
		fireMenuAssignmentSelected( key, node, (FiniteVariable)null );
	}

	/** interface MenuVariableSelector.Listener */
	public void menuVariableSelected( MenuVariableSelector menu, FiniteVariable var, DecisionNode node, DecisionNode parent ){
		fireMenuAssignmentSelected( menu.getKey(), (DecisionNode)null, var );
	}

	public void fireMenuAssignmentSelected( Key key, DecisionNode selected_existing, FiniteVariable variable )
	{
		if( myListeners != null ){
			Object[] selectedValues = null;
			if( myMenuList != null ) selectedValues = myMenuList.getSelectedValues();
			else if( myGroupingColoringJTable != null ) selectedValues = myGroupingColoringJTable.getSelectedInstancesArray();
			Listener next;
			for( Iterator it = myListeners.iterator(); it.hasNext(); ){
				next = (Listener)it.next();
				if( next == null ) it.remove();
				else next.menuAssignmentSelected( (MenuAssignment)this, myNodeLast, selectedValues, key, selected_existing, variable );
			}
		}
	}

	public void addListener( Listener list ){
		if( myListeners == null ) myListeners = new WeakLinkedList();
		myListeners.addFirst( list );
	}

	public boolean removeListener( Listener list ){
		if( myListeners != null ) return myListeners.remove( list );
		else return false;
	}

	public interface Listener{
		public void menuAssignmentSelected( MenuAssignment menu, DecisionNode node, Object[] selectedValues, Key key, DecisionNode selected, FiniteVariable variable );
	}

	public class Key
	{
		public Key( String name ){
			this.myName = name;
		}

		public String toString(){
			return myName;
		}

		public boolean isExisting(){
			return false;
		}

		protected void init( JMenu menu ) { menu.add( new Item( (Key)this ) ); }
		protected void configure() {}
		//protected void validate() {}

		private String myName;
	}

	public abstract class MenuKey extends Key implements MenuListener
	{
		public MenuKey( String name ){
			super( name );
		}

		synchronized final protected void validate(){
			if( myFlagClean ) return;
			validateHook();
			myFlagClean = true;
		}

		abstract protected void validateHook();

		protected final void configure() {
			myFlagClean = false;
			configureHook();
		}

		abstract protected void configureHook();

		/** interface MenuListener */
		public void menuSelected(MenuEvent e){
			validate();
		}
		/** interface MenuListener */
		public void menuDeselected(MenuEvent e){}
		/** interface MenuListener */
		public void menuCanceled(MenuEvent e){}

		private boolean myFlagClean = false;
	}

	public class VariableSelectorKey extends Key
	{
		public VariableSelectorKey( String name ){
			super( name );
		}

		protected void init( JMenu menu ){
			menu.add( myMenu = new MenuVariableSelector( this.toString(), myDecisionTree.getIndex() ) );
			myMenu.setKey( (Key)this );
			//myMenu.addMenuListener( (MenuListener)this );
			myMenu.addListener( (MenuVariableSelector.Listener)MenuAssignment.this );
		}

		protected void configure(){
			//myMenu.setEnabled( true );
			//System.out.println( "VariableSelectorKey.configure()" );
			if( myNodeLast == null ) return;
			myMenu.configureForNewChild( myNodeLast );
		}

		//protected void validateHook(){
		//	if( myNodeLast == null ) return;
		//	myMenu.configureForNewChild( myNodeLast );
		//}

		protected MenuVariableSelector myMenu;
	}

	public abstract class NodeSelectorKey extends MenuKey
	{
		public NodeSelectorKey( String name ){
			super( name );
		}

		public boolean isExisting(){
			return true;
		}

		protected void init( JMenu menu ) {
			menu.add( myMenu = new MenuNodeSelector( this.toString(), (Key)this ) );
			myMenu.addMenuListener( (MenuListener)this );
			myMenu.addListener( (MenuNodeSelector.Listener)MenuAssignment.this );
		}
		protected void configureHook() {
			myMenu.setEnabled( true );
		}

		protected MenuNodeSelector myMenu;
	}

	public class Item extends JMenuItem
	{
		public Item( Key key ){
			super( key.toString() );
			this.key = key;
			addActionListener( MenuAssignment.this );
		}
		public Item( Key key, DecisionNode node ){
			super( node.toString() );
			this.key = key;
			this.node = node;
			addActionListener( MenuAssignment.this );
		}
		public Key key;
		public DecisionNode node;
	}

	public class Model extends AbstractListModel implements ListModel
	{
		public Model(){}

		public void configure( DecisionNode dn ){
			this.myVariable = dn.getVariable();
			this.myInstances = myVariable.instances();
			fireContentsChanged( (ListModel)this, 0, getSize()-1 );
		}

		public int getSize(){
			if( myInstances == null ) return 0;
			else return myInstances.size();
		}

		public Object getElementAt(int index){
			if( myInstances == null ) return null;
			else return myInstances.get( index );
		}

		//public void addListDataListener(ListDataListener l){}
		//public void removeListDataListener(ListDataListener l){}

		transient private FiniteVariable myVariable;
		transient private List myInstances;
	}

	transient private DecisionNode myNodeLast;

	private HashSet myUtilSet = new HashSet();
	private Set mySetExistingLeaves;
	private Set mySetExistingInternals;
	private Set mySetAncestors;

	private DecisionTree myDecisionTree;
	private Factory myFactory;
	private Model myListModel;
	private MenuList myMenuList;
	private GroupingColoringJTable myGroupingColoringJTable;
	private WeakLinkedList myListeners;
	private Item[] myItems;
	private JMenu myMenuAssignToLeaf;
	private JMenu myMenuAssignToInternal;
}
