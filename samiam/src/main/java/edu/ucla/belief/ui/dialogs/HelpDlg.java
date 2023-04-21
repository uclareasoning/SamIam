package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.net.URL;

/**
 *  This class creates a "help" dialog.
 */

public class HelpDlg extends JDialog {
    public HelpDlg( UI owner) {
	super( owner, UI.STR_SAMIAM_ACRONYM + " Help", true);  //modal dialog

        setDefaultCloseOperation( DISPOSE_ON_CLOSE);

        Border padding = BorderFactory.createEmptyBorder(5,5,5,5);


	//Find help file
	String helpString = "";
	try {
		//URL textURL = ClassLoader.getSystemResource("images/samiamhelp.txt");
		URL textURL = edu.ucla.belief.ui.toolbar.MainToolBar.findImageURL( "samiamhelp.txt" );

		InputStream in = null;
		if( textURL != null) in = textURL.openStream();

		if( in != null)
		{
			InputStreamReader inr = new InputStreamReader( in);

			char buff[] = new char[100];
			for(;;)
			{
				int ret = inr.read( buff);
				if( ret >= 0) helpString = helpString + new String(buff, 0, ret);
				else break;
			}
		}

	}
	catch( Exception e) {
		helpString = "";
	}


	if( helpString == null || helpString.equals("")) {
	    helpString = "No help information is available.";
	}


	//Text Panel
	JTextArea textArea = new JTextArea( helpString, 32, 80 );
//        textArea.setFont(new Font("Serif", Font.ITALIC, 16));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
	textArea.setEnabled( false);
	textArea.setBorder( padding);


	JScrollPane textPane = new JScrollPane( textArea);
	textPane.setBorder( padding);


	//Button Panel
	JPanel buttonPane = new JPanel();

	buttonPane.setLayout( new BoxLayout( buttonPane, BoxLayout.Y_AXIS));
	buttonPane.setBorder( padding);

	JButton button = new JButton( "Close");
	buttonPane.add( button);
	button.addActionListener( new ActionListener() {
	    public void actionPerformed(ActionEvent e){
		dispose();
	    }
	});


	Container cnt = getContentPane();
        cnt.setLayout( new BorderLayout());
	cnt.add( textPane, BorderLayout.CENTER);
	cnt.add( buttonPane, BorderLayout.SOUTH);

	pack();
	if( owner != null) {
	    setLocationRelativeTo( owner);
	}
    }

    public static final String
      STR_MESSAGE_RUNSCRIPT = "Please run "+UI.STR_SAMIAM_ACRONYM+" using the provided run script";

    /** @since 20100106 */
    public static void main( String[] args ){
		String scriptname = null, message = STR_MESSAGE_RUNSCRIPT;
		try{
			scriptname = edu.ucla.belief.ui.util.BrowserControl.isWindowsPlatform() ? "samiam.bat" : "runsamiam";
			scriptname = " `" + scriptname + "`";
			message    = STR_MESSAGE_RUNSCRIPT + scriptname + ".";
		}catch( Throwable thrown ){
			message    = STR_MESSAGE_RUNSCRIPT;
			System.err.println( thrown );
		}
		try{
			if( java.awt.GraphicsEnvironment.isHeadless() ){
				java.io.PrintStream stream = System.out;
				stream.println( message );
			}else{
				Panel         panel = null;
				final Dialog dialog = new Dialog( (Frame) null, UI.STR_SAMIAM_ACRONYM+" Restart", true );
				try{
					panel = new Panel();
					panel.add( new Label( message ) );

					dialog.addWindowListener( new WindowAdapter(){
						public void windowClosing( WindowEvent windowevent ){
							dialog.dispose();
						}
					} );

					Button button = new Button( "OK" );
					button.addActionListener( new ActionListener(){
						public void actionPerformed( ActionEvent actionevent ){
							dialog.dispose();
						}
					} );
					panel.add(  button );

					dialog.setUndecorated( false );
				}catch( Throwable thrown ){
					System.err.println( thrown );
				}
				try{
				  //dialog.setIconImage();
					java.lang.reflect.Method meth = Dialog.class.getMethod( "setIconImage", new Class[]{ Image.class } );
					meth.invoke( dialog, new Object[]{ edu.ucla.belief.ui.toolbar.MainToolBar.getIcon( "ARGroupOnline16.gif" ).getImage() } );
				}catch( Throwable thrown ){}
				try{
					dialog.add( panel  );
					dialog.pack();
					edu.ucla.belief.ui.util.Util.centerWindow( dialog );
				}catch( Throwable thrown ){
					System.err.println( thrown );
				}
				dialog.setVisible( true );
			}
		}catch( Throwable thrown ){
			System.err.println( thrown );
		}
		System.exit(9);
	}
}
