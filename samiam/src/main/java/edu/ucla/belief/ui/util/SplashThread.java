package edu.ucla.belief.ui.util;

import edu.ucla.belief.ui.UI;

import javax.swing.*;
import java.net.URL;
import java.awt.Color;
import java.awt.Font;

/**
	Show the samiam splash graphic.
	@author Keith Cascio
	@since 052302
*/
public class SplashThread implements Runnable
{
	/**
		@author Keith Cascio
		@since 052302
		@param millis Number of milliseconds to show the splash graphic.
	*/
	public SplashThread( long millis )
	{
		LONG_SPLASH_MILLIS = millis;
	}

	/** @since 051305 */
	public Thread newThread(){
		return new Thread( (Runnable)this );
	}

	public void run()
	{
		JWindow wndSplash = new JWindow();
		//wndSplash.setUndecorated( true );//jdk1.4 only
		JPanel pnlSplash = new JPanel();
		JLabel titleLbl = null;
		ImageIcon titleGif = null;

		//URL iconURL = ClassLoader.getSystemResource( "images/samiamsplash.gif" );
		URL iconURL = edu.ucla.belief.ui.toolbar.MainToolBar.findImageURL( "samiamsplash.gif" );
		if( iconURL != null) titleGif = new ImageIcon( iconURL, UI.STR_SAMIAM_ACRONYM );
		else titleGif = new ImageIcon( "images/samiamsplash.gif", UI.STR_SAMIAM_ACRONYM );

		if( titleGif == null )
		{
		    System.err.println("ERROR: Could not find title icon.");
		    titleLbl = new JLabel( UI.STR_SAMIAM_ACRONYM+": Sensitivity Analysis, Modeling, Inference And More");
		    titleLbl.setForeground( Color.blue);
		    titleLbl.setFont( new Font("SansSerif", Font.BOLD, 16) );
		}

		if( titleLbl == null) {  //this is normal operation
		    titleLbl = new JLabel( titleGif);
		}
		pnlSplash.add( titleLbl);
		pnlSplash.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		wndSplash.getContentPane().add( pnlSplash );


		//wndSplash.setSize( pnlSplash.getPreferredSize().width, 100 );
		wndSplash.setSize( pnlSplash.getPreferredSize() );
		Util.centerWindow( wndSplash );
		wndSplash.setVisible(true);
		try{
			//(new Throwable()).printStackTrace();//debug
			Thread.sleep( LONG_SPLASH_MILLIS );
		}catch( InterruptedException e ){
			System.err.println( "Warning: splash thread interrupted." );
			Thread.currentThread().interrupt();
		}finally{
			wndSplash.setVisible(false);
			wndSplash.dispose();
		}
	}

	private long LONG_SPLASH_MILLIS = (long)3000;
}
