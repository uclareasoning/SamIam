package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import java.util.LinkedList;

/** supports lazy handling of the enabled state of modal actions belonging to one menu
	@author keith cascio
	@since  20060719 */
public class ModalMenu extends JMenu// implements MenuListener
{
	public ModalMenu( String text, UI ui ){
		super( text );
		myUI = ui;
		//this.addMenuListener( (MenuListener)this );
	}

	public JMenuItem add( Action a ){
		if( a instanceof ModalAction ) myList.add( (ModalAction)a );
		return super.add( a );
	}

	public JMenuItem add( JMenuItem menuItem ){
		Action a = menuItem.getAction();
		if( a instanceof ModalAction ) myList.add( (ModalAction)a );
		return super.add( menuItem );
	}

	protected void fireMenuSelected(){
		this.menuSelected( (MenuEvent)null );
		super.fireMenuSelected();
	}

	public void menuSelected( MenuEvent e ){
		if( myUI == null ) return;
		NetworkInternalFrame nif  = myUI.getActiveHuginNetInternalFrame();

		SamiamUserMode       mode = null;
		if( nif  == null )   mode = new SamiamUserMode();
		else                 mode = nif.getSamiamUserMode();

		this.setMode( mode, nif );
	}

	public void setMode( SamiamUserMode mode, NetworkInternalFrame nif ){
		//for( ModalAction modalaction : getArray() ) if( modalaction != null ) modalaction.setMode( mode, nif );
		ModalAction[] array = getArray();
		for( int i = 0; i<array.length; i++ ) if( array[i] != null ) array[i].setMode( mode, nif );
	}

	public ModalAction[] getArray(){
		if( myList != null ){
			if( myArray != null ){
				for( int i=myArray.length-1; i>=0; i-- ) myList.addFirst( myArray[i] );
			}
			myArray = (ModalAction[]) myList.toArray( new ModalAction[ myList.size() ] );
			myList.clear();
			myList = null;
		}
		return myArray;
	}

	private UI                      myUI;
	private LinkedList/*<ModalAction>*/ myList = new LinkedList();
	private ModalAction[]           myArray;
}
