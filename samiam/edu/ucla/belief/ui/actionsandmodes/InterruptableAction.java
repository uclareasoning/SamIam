package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.util.Interruptable;

import java.awt.event.ActionEvent;
import javax.swing.Icon;

/** Action that executes in a separate Thread.

	@author Keith Cascio
	@since 032405 */
public abstract class InterruptableAction extends SamiamAction
{
	public InterruptableAction( String name, String descrip, char mnemonic, Icon icon ){
		super( name, descrip, mnemonic, icon );
	}

	/** @since 20060328 */
	public Interruptable getInterruptable(){
		return myInterruptable;
	}

	abstract public void runImpl( Object arg1 ) throws InterruptedException;

	public void actionPerformed( ActionEvent evt ){
		myInterruptable.start();
	}

	private Interruptable myInterruptable = new Interruptable(){
		public void runImpl( Object arg1 ) throws InterruptedException{
			InterruptableAction.this.runImpl( arg1 );
		}
	};
}
