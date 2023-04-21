package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.NetworkInternalFrame;

import java.awt.*;
import javax.swing.*;
import java.util.*;

/**
	@author Keith Cascio
	@since 011404
*/
public abstract class HandledModalAction extends ModalAction implements SamiamUserModal
{
	public HandledModalAction(	String name,
					String descrip,
					char mnemonic,
					Icon icon,
					ModeHandler handler )
	{
		this( name, descrip, mnemonic, icon, handler, true );
	}

	public HandledModalAction(	String name,
					String descrip,
					char mnemonic,
					Icon icon,
					ModeHandler handler,
					boolean register )
	{
		super( name, descrip, mnemonic, icon, (KeyStroke)null, register );
		myHandler = (handler == null) ? ModeHandler.INDEPENDANT : handler;
	}

	public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
		return myHandler.decideEnabled( mode, nif );
	}

	public ModeHandler handler(){
		return myHandler;
	}

	private ModeHandler myHandler;
}
