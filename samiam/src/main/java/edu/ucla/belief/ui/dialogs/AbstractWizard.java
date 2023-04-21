package edu.ucla.belief.ui.dialogs;

import edu.ucla.util.WeakLinkedList;

import java.awt.event.WindowEvent;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.ActionListener;

/** @author Keith Cascio
	@since 032605 */
public abstract class AbstractWizard extends Object implements Wizard, ActionListener
{
	//public Stage getFirst();

	public void windowClosing( WindowEvent e ){}

	public WizardPanel getWizardPanel(){
		if( myWizardPanel == null ){
			myWizardPanel = new WizardPanel( this );
		}
		return myWizardPanel;
	}

	public void addWizardListener( WizardListener listener ){
		if( myListeners == null ) myListeners = new WeakLinkedList();
		else if( myListeners.contains( listener ) ) return;
		myListeners.add( listener );
	}

	public boolean removeWizardListener( WizardListener listener ){
		if( myListeners == null ) return false;
		else return myListeners.remove( listener );
	}

	protected void fireWizardFinished(){
		if( myListeners == null ) return;
		WizardListener next;
		for( Iterator it = myListeners.iterator(); it.hasNext(); ){
			next = (WizardListener) it.next();
			if( next == null ) it.remove();
			next.wizardFinished();
		}
	}

	protected void fireResetNavigation(){
		if( myListeners == null ) return;
		WizardListener next;
		for( Iterator it = myListeners.iterator(); it.hasNext(); ){
			next = (WizardListener) it.next();
			if( next == null ) it.remove();
			next.resetNavigation();
		}
	}

	protected JButton makeButton( String text, String tooltip ){
		JButton ret = new JButton( text );
		ret.setToolTipText( tooltip );
		ret.addActionListener( (ActionListener)this );
		return ret;
	}

	public static void border( JComponent comp ){
		Border emptyinner = BorderFactory.createEmptyBorder( /*top*/4, /*left*/4, /*bottom*/4, /*right*/4 );
		Border etched = BorderFactory.createEtchedBorder();
		Border compoundinner = BorderFactory.createCompoundBorder( /*outside*/etched, /*inside*/emptyinner );
		Border emptyouter = BorderFactory.createEmptyBorder( /*top*/8, /*left*/8, /*bottom*/4, /*right*/8 );
		Border compoundouter = BorderFactory.createCompoundBorder( /*outside*/emptyouter, /*inside*/compoundinner );
		comp.setBorder( compoundouter );
	}

	private WeakLinkedList myListeners;
	private WizardPanel myWizardPanel;
}
