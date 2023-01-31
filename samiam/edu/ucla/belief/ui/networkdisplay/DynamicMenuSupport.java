package edu.ucla.belief.ui.networkdisplay;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

/** Provides support for a JMenu with ever-changing
	contents, that acts as a mechanism to select some
	object on which to perform a given action.

	Override itemSelected() to specify behavior.

	@author keith cascio
	@since 20060530 */
public class DynamicMenuSupport implements MenuListener, ActionListener
{
	public DynamicMenuSupport( String caption ){
		myName = caption;
	}

	public JMenu getJMenu(){
		if( myMenu == null ){
			myMenu = new JMenu( myName );
			myMenu.addMenuListener( DynamicMenuSupport.this );
		}
		return myMenu;
	}

	/** override this method to specify behavior */
	public void itemSelected( Object obj ){
		System.err.println( "itemSelected( " + obj.toString() + " )" );
	}

	public void setContents( Collection contents ){
		if( myContents != null ) myContents.clear();
		addAll( contents );
	}

	public void addAll( Collection contents ){
		DynamicMenuSupport.this.myFlagValid = false;
		if( myContents == null ) myContents = new ArrayList( contents );
		else myContents.addAll( contents );
		getJMenu().setEnabled( !myContents.isEmpty() );
	}

	public void setSort( boolean flag ){
		myFlagSort = flag;
	}

	public void menuSelected(   MenuEvent e ){
		validate();
	}
	public void menuDeselected( MenuEvent e ){}
	public void menuCanceled(   MenuEvent e ){}

	public void actionPerformed( ActionEvent actionevent ){
		TargetedMenuItem src = (TargetedMenuItem) actionevent.getSource();
		itemSelected( src.myTarget );
	}

	private void validate(){
		if( DynamicMenuSupport.this.myFlagValid ) return;

		boolean empty = myContents.isEmpty();
		myMenu.setEnabled( !empty );
		if( empty ) return;

		if( myFlagSort ) Collections.sort( myContents );

		myMenu.removeAll();
		LinkedList remaining = new LinkedList( myPool );
		for( Iterator it = myContents.iterator(); it.hasNext(); ){
			myMenu.add( getItem( it.next(), remaining ) );
		}
		DynamicMenuSupport.this.myFlagValid = true;
	}

	private JMenuItem getItem( Object obj, LinkedList remaining ){
		JMenuItem ret = null;
		if( remaining.isEmpty() ) myPool.add( ret = new TargetedMenuItem( obj ) );
		else ret = ((TargetedMenuItem) remaining.removeFirst()).set( obj );//poll();
		return ret;
	}

	public class TargetedMenuItem extends JMenuItem{
		public TargetedMenuItem( Object obj ){
			super( obj.toString() );
			myTarget = obj;
			addActionListener( DynamicMenuSupport.this );
		}

		public TargetedMenuItem set( Object obj ){
			TargetedMenuItem.this.setText( (myTarget = obj).toString() );
			return TargetedMenuItem.this;
		}

		private Object myTarget;
	}

	private String     myName;
	private ArrayList  myContents;
	private JMenu      myMenu;
	private boolean    myFlagValid;
	private LinkedList myPool = new LinkedList();
	private boolean    myFlagSort = true;
}
