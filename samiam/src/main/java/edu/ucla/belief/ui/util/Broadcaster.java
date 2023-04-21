package edu.ucla.belief.ui.util;

import edu.ucla.util.WeakLinkedList;

import java.util.Iterator;
import java.awt.event.*;

/** Simple class handles registering ActionListeners
	to a weak list.

	@author Keith Cascio
	@since 030905 */
public class Broadcaster
{
	public Broadcaster(){
		myEvent = new ActionEvent( this, (int)0, this.getClass().getName() );
	}

	public Broadcaster( Object source ){
		myEvent = new ActionEvent( source, (int)0, source.getClass().getName() );
	}

	public void addListener( ActionListener listener ){
		if( myListeners == null ) myListeners = new WeakLinkedList();
		else if( myListeners.contains( listener ) ) return;
		myListeners.add( listener );
	}

	public boolean removeListener( ActionListener listener ){
		if( myListeners == null ) return false;
		else return myListeners.remove( listener );
	}

	public void fireListeners(){
		if( myListeners == null ) return;
		ActionListener next;
		for( Iterator it = myListeners.iterator(); it.hasNext(); ){
			next = (ActionListener) it.next();
			if( next == null ) it.remove();
			else next.actionPerformed( myEvent );
		}
	}

	private WeakLinkedList myListeners;
	private ActionEvent myEvent;
}
