package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.NetworkInternalFrame;

//import java.awt.*;
import javax.swing.*;
//import java.util.*;

/**
	@author Keith Cascio
	@since 20051007
*/
public abstract class LockableHandledModalAction extends HandledModalAction implements SamiamUserModal
{
	public LockableHandledModalAction(	String name,
					String descrip,
					char mnemonic,
					Icon icon,
					ModeHandler handler )
	{
		this( name, descrip, mnemonic, icon, handler, true );
	}

	public LockableHandledModalAction(	String name,
					String descrip,
					char mnemonic,
					Icon icon,
					ModeHandler handler,
					boolean register )
	{
		super( name, descrip, mnemonic, icon, handler, register );
		this.myName       = name;
		this.myNameLocked = name + " (locked)";
	}

	public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
		String name = mode.contains( SamiamUserMode.MODELOCK ) ? myNameLocked : myName;
		this.setName( name );
		return super.decideEnabled( mode, nif );
	}

	private String myName;
	private String myNameLocked;
}
