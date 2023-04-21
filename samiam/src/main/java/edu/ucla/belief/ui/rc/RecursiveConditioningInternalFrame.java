package edu.ucla.belief.ui.rc;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

/**
	@author Keith Cascio
	@since 111003
*/
public class RecursiveConditioningInternalFrame extends JInternalFrame
{
	public RecursiveConditioningInternalFrame( NetworkInternalFrame hnInternalFrame )
	{
		super( STR_TITLE_NORMAL, true, true, true, true );

		init( hnInternalFrame );
	}

	public static final String STR_TITLE_NORMAL = "Recursive Conditioning, Pr(e) only";
	public static final String STR_TITLE_PAUSED = "Recursive Conditioning - PAUSED";
	public static final String STR_FILENAME_ICON = "RC16.gif";

	public static void main( String[] args )
	{
		if( args.length > 0 )
		{
			if( args[0].equals( "debug" ) ) RCPanel.FLAG_DEBUG = Util.DEBUG = true;
		}

		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		final JFrame frame = new JFrame( "Test/Debug Frame" );
		frame.setBounds( 0,0,700,600 );

		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		JDesktopPane desktopPane = new JDesktopPane();

		final RecursiveConditioningInternalFrame RCIF = new RecursiveConditioningInternalFrame( null );
		RCIF.setBounds( 20, 40, 600, 375 );
		RCIF.pack();
		RCIF.setDefaultCloseOperation( HIDE_ON_CLOSE );
		desktopPane.add( RCIF );

		frame.getContentPane().add( desktopPane );
		frame.addWindowListener( new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				Util.STREAM_TEST.println( RCIF.getSize() );//debug
				//System.exit(0);
			}
		});

		Util.centerWindow( frame );
		RCIF.setVisible( true );
		frame.setVisible( true );
	}

	protected void init( NetworkInternalFrame hnInternalFrame )
	{
		Icon iconFrame = MainToolBar.getIcon( STR_FILENAME_ICON );
		if( iconFrame != null ) setFrameIcon( iconFrame );

		myRCPanel = new RCPanel( hnInternalFrame, this );

		getContentPane().add( myRCPanel );

		Dimension dim = myRCPanel.getPreferredSize();
		dim.width += 10;
		dim.height += 20;
		setPreferredSize( dim );
	}

	public void setVisible( boolean flag )
	{
		if( myRCPanel != null ) myRCPanel.setVisibleSafe( flag, isVisible() );
		super.setVisible( flag );
	}

	/** @since 021505 */
	public RCPanel getSubComponent(){
		return myRCPanel;
	}

	protected RCPanel myRCPanel;
}
