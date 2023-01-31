package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.util.Util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
	@author Keith Cascio
	@since 121902
*/
public class SystemCallAction extends SamiamAction
{
	public SystemCallAction( String name, String descrip, char mnemonic, Icon icon )
	{
		super( name, descrip, mnemonic, icon );
	}

	public void actionPerformed( ActionEvent evt )
	{
		String cmd = (String) getValue( Action.SHORT_DESCRIPTION );
		Process p;
		try{
			p = Runtime.getRuntime().exec( cmd );
		}catch( Exception e ){
			if( Util.DEBUG_VERBOSE )
			{
				System.err.println( "Warning: SystemCallAction " + cmd + " failed." );
			}
		}
	}
}