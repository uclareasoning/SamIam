package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;

import edu.ucla.belief.io.FileType;

import javax.swing.*;
import java.awt.*;

/** used by UI.LoadCallbacks.handleSyntaxErrors()

	@author keith cascio
	@since 20060525 */
public class SyntaxErrorNotification
{
	private SyntaxErrorNotification(){}

	public static SyntaxErrorNotification getInstance(){
		if( INSTANCE == null ) INSTANCE = new SyntaxErrorNotification();
		return INSTANCE;
	}
	private static SyntaxErrorNotification INSTANCE;

	public void handleSyntaxErrors( NetworkInternalFrame nif, String description, String[] errors, FileType filetype ){
		if( nif    == null ) return;
		if( errors == null ) return;

		myNetworkInternalFrame = nif;
		printErrors( description, errors );
		setURL( filetype );
		show( description, filetype );
	}

	private void show( String description, FileType filetype ){
		if( myNetworkInternalFrame == null ) return;
		JPanel panel = getPanel();
		if( filetype    != null ) myLabelFormat.setText(      "<html><nobr><b>" + filetype.getName() );
		if( description != null ) myLabelDescription.setText( "<html><nobr><b>" + description );
		JOptionPane.showMessageDialog( myNetworkInternalFrame, panel, "warning: syntax errors", JOptionPane.WARNING_MESSAGE );
	}

	private JPanel getPanel(){
		if( myPanel == null ){
			JLabel[] line1 = new JLabel[] {
				new JLabel( "Syntax errors in " ),
				myLabelDescription = new JLabel( "???" ),
				new JLabel( " printed to the " ),
				myLabelConsole,
				new JLabel( " ; " ) };
			JLabel[] line4 = new JLabel[] {
				new JLabel( "for a description of the " ),
				myLabelFormat = new JLabel( "???" ),
				new JLabel( " format." ) };

			myPanel = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = GridBagConstraints.REMAINDER;

			myPanel.add( concat( line1 ), c );
			myPanel.add( new JLabel( "Please refer to " ), c );
			myPanel.add( myLabelFormatHomeURL, c );
			myPanel.add( concat( line4 ), c );
		}
		return myPanel;
	}

	private JPanel concat( JLabel[] labels ){
		JPanel ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.anchor    = GridBagConstraints.WEST;
		c.gridwidth = 1;
		for( int i=0; i<labels.length; i++ ) ret.add( labels[i], c );
		return ret;
	}

	private void printErrors( String description, String[] errors ){
		if( myNetworkInternalFrame == null ) return;
		if( errors                 == null ) return;

		myNetworkInternalFrame.console.println( "warning: encountered "+errors.length+" syntax errors while parsing " + description );
		myNetworkInternalFrame.console.println();
		for( int i=0; i<errors.length; i++ ){
			myNetworkInternalFrame.console.println( errors[i] );
		}
	}

	private void setURL( FileType filetype ){
		setURL( ( filetype == null ) ? null : filetype.getURL() );
	}

	private void setURL( String url ){
		if( url == null ) url = UI.URL_SAMIAM;
		myLabelFormatHomeURL.setText( myFormatHomeURL = url );
	}

	private Runnable myRunFormatHomePage = new Runnable(){
		public void run(){
			if( myFormatHomeURL != null ) BrowserControl.displayURL( myFormatHomeURL );
		}
	};

	private Runnable myRunConsole        = new Runnable(){
		public void run(){
			if( myNetworkInternalFrame != null ) myNetworkInternalFrame.setConsoleVisible( true );
		}
	};

	private HyperLabel myLabelFormatHomeURL = new HyperLabel( "", myRunFormatHomePage );
	private HyperLabel myLabelConsole       = new HyperLabel( "console", myRunConsole );
	private String myFormatHomeURL;
	private NetworkInternalFrame myNetworkInternalFrame;
	private JPanel myPanel;
	private JLabel myLabelDescription, myLabelFormat;
}