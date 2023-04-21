package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.*;
import edu.ucla.belief.decision.DecisionNode;
//import edu.ucla.belief.decision.DecisionLeaf;
//import edu.ucla.belief.decision.DecisionTree;
//import edu.ucla.belief.decision.Factory;
import edu.ucla.util.WeakLinkedList;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
//import java.util.HashSet;
import java.util.Comparator;
import java.util.Collections;
//import java.util.Set;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListDataListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
//import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
//import java.awt.Dimension;

/** @author Keith Cascio
	@since 121104 */
public class MenuNodeSelector extends JMenu implements ActionListener, Comparator, ListSelectionListener
{
	public static final int INT_THRESHOLD_DEFAULT = (int)10;
	public static final java.text.Collator COLLATOR = java.text.Collator.getInstance();

	public MenuNodeSelector( String title, MenuAssignment.Key key ){
		super( title );
		this.myKey = key;
		init();
	}

	public void setThreshold( int thres ){
		this.myThreshold = thres;
	}

	public int getThreshold(){
		return this.myThreshold;
	}

	private void init()
	{
	}

	public void setContents( Collection nodes ){
		this.removeAll();
		this.setEnabled( !nodes.isEmpty() );
		synchronized( myArrayList )
		{
		myArrayList.clear();
		myArrayList.addAll( nodes );
		Collections.sort( myArrayList, (Comparator)this );

		if( myArrayList.size() < myThreshold ){
			for( Iterator it = myArrayList.iterator(); it.hasNext(); ){
				this.add( new Item( (DecisionNode) it.next() ) );
			}
		}
		else{
			if( myListModel == null ) myListModel = new Model();
			myListModel.setContents( myArrayList );
			if( myMenuList == null ){
				myMenuList = new MenuList( myListModel );
				myMenuList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
				myMenuList.addListSelectionListener( (ListSelectionListener)this );
			}
			this.add( myMenuList );
			if( myItemFromList == null ) myItemFromList = new Item( "OK" );
			this.add( myItemFromList );
			myItemFromList.setEnabled( false );
		}
		}
		revalidate();
		repaint();
	}

	/** interface ListSelectionListener */
	public void valueChanged( ListSelectionEvent e ){
		if( (myMenuList != null) && (myItemFromList != null) )
			myItemFromList.setEnabled( !myMenuList.isSelectionEmpty() );
	}

	/** interface Comparator */
	public int compare( Object o1, Object o2 ){
		return COLLATOR.compare( o1.toString(), o2.toString() );
	}

	public void actionPerformed( ActionEvent e )
	{
		Item vi = (Item) e.getSource();
		DecisionNode node = null;
		if( vi == myItemFromList ) node = (DecisionNode) myMenuList.getSelectedValue();
		else node = vi.node;

		if( myListeners != null ){
			Listener next;
			for( Iterator it = myListeners.iterator(); it.hasNext(); ){
				next = (Listener)it.next();
				if( next == null ) it.remove();
				else next.menuNodeSelected( (MenuNodeSelector)this, myKey, node );
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
		public void menuNodeSelected( MenuNodeSelector menu, MenuAssignment.Key key, DecisionNode node );
	}

	public class Item extends JMenuItem
	{
		public Item( String title ){
			super( title );
			addActionListener( MenuNodeSelector.this );
		}
		public Item( DecisionNode node ){
			super( node.toString() );
			this.node = node;
			addActionListener( MenuNodeSelector.this );
		}
		public DecisionNode node;
	}

	public class Model extends AbstractListModel implements ListModel
	{
		public Model(){}

		public void setContents( List list ){
			this.myList = list;
			fireContentsChanged( (ListModel)this, 0, getSize()-1 );
		}

		public int getSize(){
			if( myList == null ) return 0;
			else return myList.size();
		}

		public Object getElementAt(int index){
			if( myList == null ) return null;
			else return myList.get( index );
		}

		//public void addListDataListener(ListDataListener l){}
		//public void removeListDataListener(ListDataListener l){}

		transient private List myList;
	}

	private MenuAssignment.Key myKey;

	private WeakLinkedList myListeners;
	private Model myListModel;
	private MenuList myMenuList;
	private Item myItemFromList;
	private ArrayList myArrayList = new ArrayList();
	private int myThreshold = INT_THRESHOLD_DEFAULT;
}
