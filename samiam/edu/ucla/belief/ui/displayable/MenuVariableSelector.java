package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.*;
import edu.ucla.belief.decision.DecisionNode;
import edu.ucla.util.WeakLinkedList;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/** @author Keith Cascio
	@since 120904 */
public class MenuVariableSelector extends JMenu implements ActionListener
{
	public MenuVariableSelector( String title, TableIndex index ){
		super( title );
		this.myVariables = new ArrayList( index.variables() );
		myVariables.remove( myVariables.size()-1 );
		init();
	}

	public void setKey( MenuAssignment.Key key ){
		this.myKey = key;
	}

	public MenuAssignment.Key getKey(){
		return this.myKey;
	}

	public void configureForNewParent( DecisionNode node, DecisionNode parent )
	{
		setAllVisible( true );
		setVisible( myNodeLast = node, false );
		setVisible( myParentLast = parent, false );
		configurePost();
	}

	public void configureForNewChild( DecisionNode node )
	{
		//System.out.println( "MenuVariableSelector.configureForNewChild( "+node+" )" );
		setAllVisible( true );
		setVisible( myNodeLast = node, false );
		//for( Iterator it = node.getChildDecisionNodes().iterator(); it.hasNext(); ){
		//	setVisible( (DecisionNode)it.next(), false );
		//}
		configurePost();
	}

	private void configurePost(){
		boolean flagExistsVisible = existsVisible( true );
		if( flagExistsVisible ){
			revalidate();
			repaint();
		}
		this.setEnabled( flagExistsVisible );
	}

	public void setVisible( DecisionNode node1, boolean flag )
	{
		if( node1 == null ) return;
		FiniteVariable var = node1.getVariable();
		if( !myVariables.contains( var ) ) return;
		for( int i=0; i<myItems.length; i++ ){
			if( myItems[i].variable == var ){
				myItems[i].setEnabled( flag );
				myItems[i].setVisible( flag );
				break;
			}
		}
	}

	public void setAllVisible( boolean flag )
	{
		for( int i=0; i<myItems.length; i++ ){
			myItems[i].setEnabled( flag );
			myItems[i].setVisible( flag );
		}
	}

	public boolean existsVisible( boolean flag )
	{
		for( int i=0; i<myItems.length; i++ ){
			if( myItems[i].isVisible() == flag ) return true;
		}
		return false;
	}

	private void init(){
		myItems = new VariableItem[myVariables.size()];
		for( int i=0; i<myItems.length; i++ ){
			add( myItems[i] = new VariableItem( (FiniteVariable) myVariables.get(i) ) );
		}
	}

	public void actionPerformed( ActionEvent e ){
		VariableItem vi = (VariableItem) e.getSource();
		if( myListeners != null ){
			Listener next;
			for( Iterator it = myListeners.iterator(); it.hasNext(); ){
				next = (Listener)it.next();
				if( next == null ) it.remove();
				else next.menuVariableSelected( (MenuVariableSelector)this, vi.variable, myNodeLast, myParentLast );
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
		public void menuVariableSelected(
			MenuVariableSelector menu,
			FiniteVariable var,
			DecisionNode node,
			DecisionNode parent );
	}

	public class VariableItem extends JMenuItem
	{
		public VariableItem( FiniteVariable var ){
			super( var.toString() );
			this.variable = var;
			addActionListener( MenuVariableSelector.this );
		}
		public FiniteVariable variable;
	}

	transient private DecisionNode myNodeLast;
	transient private DecisionNode myParentLast;

	private WeakLinkedList myListeners;
	private VariableItem[] myItems;
	private List myVariables;
	private MenuAssignment.Key myKey;
}
