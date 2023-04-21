package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.NetworkInternalFrame;

import java.awt.*;
import javax.swing.*;
import java.util.*;

/** @author Keith Cascio
	@since 20040115 */
public abstract class PasteAction extends HandledModalAction implements SamiamUserModal
{
	public PasteAction( String name, String descrip, char mnemonic, Icon icon, ModeHandler handler ){
		super( name, descrip, mnemonic, icon, handler );
	}

	abstract public int sizeClipboard();

	public void init(){
		setEnabled( decideEnabled() );
	}

	public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
		return handler().decideEnabled( mode, nif ) && decideEnabled();
	}

	public boolean decideEnabled(){
		return sizeClipboard() > (int)0;
	}
}
