package edu.ucla.belief.ui.actionsandmodes;

import java.util.LinkedList;
import java.util.Iterator;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.AbstractButton;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

/**
	@author Keith Cascio
	@since 060304
*/
public abstract class BooleanStateAction extends SamiamAction implements Action
{
	public BooleanStateAction( String name, String descrip, Icon icon, boolean state ){
		super( name, descrip, (char)0, icon );
		setState( state );
	}

	public final void actionPerformed( ActionEvent e ){
		Object src = e.getSource();
		if( src instanceof AbstractButton ){
			setState( ((AbstractButton)src).isSelected() );
		}
		actionPerformed( myState );
	}

	public void setState( boolean flag ){
		myState = flag;
		if( myListeners != null ){
			for( Iterator it = myListeners.iterator(); it.hasNext(); ){
				((AbstractButton)it.next()).setSelected( myState );
			}
		}
	}

	public boolean getState(){
		return myState;
	}

	abstract public void actionPerformed( boolean state );

	/*
	public void addPropertyChangeListener( PropertyChangeListener listener ){
		//System.out.println( "BooleanStateAction.addPropertyChangeListener( "+listener+" )" );
		super.addPropertyChangeListener( listener );
		if( listener instanceof AbstractButton ) addListener( (AbstractButton)listener );
	}*/

	public final void addListener( AbstractButton button ){
		if( myListeners == null ) myListeners = new LinkedList();
		if( !myListeners.contains(button) ) myListeners.add( button );
	}

	private LinkedList myListeners;
	private boolean myState;
}
