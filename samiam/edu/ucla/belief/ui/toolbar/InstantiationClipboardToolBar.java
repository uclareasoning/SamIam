package edu.ucla.belief.ui.toolbar;

import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;
import edu.ucla.belief.ui.UI;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** @since 20030707 */
public class InstantiationClipboardToolBar extends JToolBar
{
	public InstantiationClipboardToolBar( UI u )
	{
		super( "Instantiation Clipboard Tool Bar" );
		ui = u;
		init();
		return;
	}

	/** @since 021405 Valentine's Day! */
	public JButton forAction( Action action ){
		Component[] components = this.getComponents();
		JButton button;
		for( int i=0; i<components.length; i++ ){
			if( components[i] instanceof JButton ){
				button = (JButton) components[i];
				if( button.getAction() == action ) return button;
			}
		}
		return (JButton) null;
	}

	protected void init()
	{
		add( MainToolBar.initButton( ui.action_COPYEVIDENCE ) );
		add( MainToolBar.initButton( ui.action_CUTEVIDENCE ) );
		add( MainToolBar.initButton( ui.action_PASTEEVIDENCE ) );
		add( MainToolBar.initButton( ui.action_VIEWINSTANTIATIONCLIPBOARD ) );
		if( NetworkIO.xmlAvailable() )
		{
			add( MainToolBar.initButton( ui.action_LOADINSTANTIATIONCLIPBOARD ) );
			add( MainToolBar.initButton( ui.action_SAVEINSTANTIATIONCLIPBOARD ) );
		}
		add( MainToolBar.initButton( ui.action_IMPORTINSTANTIATION ) );
		add( MainToolBar.initButton( ui.action_EXPORTINSTANTIATION ) );

		//addSeparator();

		setFloatable( true );
		putClientProperty( "JToolBar.isRollover", Boolean.TRUE );

		this.addComponentListener( new ComponentAdapter(){
			public void componentHidden( ComponentEvent e ){
				try{
					Window window = SwingUtilities.getWindowAncestor( InstantiationClipboardToolBar.this );
					if( window instanceof JDialog ){ window.setVisible( false ); }
				}catch( Throwable thrown ){
					System.err.println( "warning: InstantiationClipboardToolBar.xx.componentHidden() caught " + thrown );
				}
			}
			public void componentShown( ComponentEvent e ){
				try{
					Window window = SwingUtilities.getWindowAncestor( InstantiationClipboardToolBar.this );
					if( window instanceof JDialog ){ window.pack(); window.setVisible( true ); }
				}catch( Throwable thrown ){
					System.err.println( "warning: InstantiationClipboardToolBar.xx.componentShown() caught " + thrown );
				}
			}
		} );

		return;
	}

	protected UI ui;
}
