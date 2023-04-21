package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.clipboard.ClipboardHelper;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.util.BrowserControl;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.net.URL;

/**
 *  This class creates an "about" dialog listing the version numbers, credits, and
 *  program icon.
 */
public class AboutDlg extends JDialog
{
	public AboutDlg( UI owner )
	{
		super( owner, "About "+UI.STR_SAMIAM_ACRONYM, true );//modal dialog
		init();
	}

	public static final int INT_STRUT_SIZE = 16;

	protected void init()
	{
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		//Border padding = BorderFactory.createEmptyBorder(5,5,5,5);
		Font creditFont = new Font("SansSerif", Font.BOLD, 16);

		JPanel pnlMain = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		JLabel titleLbl = null;
		ImageIcon titleGif = null;

		//URL iconURL = ClassLoader.getSystemResource("images/samiamtitle.gif");
		URL iconURL = edu.ucla.belief.ui.toolbar.MainToolBar.findImageURL( "images/samiamtitle.gif" );
		if( iconURL != null) titleGif = new ImageIcon( iconURL, UI.STR_SAMIAM_ACRONYM );
		else titleGif = new ImageIcon( "images/samiamtitle.gif", UI.STR_SAMIAM_ACRONYM );

		if( titleGif == null )
		{
			System.err.println("ERROR: Could not find title icon.");
			titleLbl = new JLabel( UI.STR_SAMIAM_ACRONYM+": Sensitivity Analysis, Modeling, Inference And More");
			titleLbl.setForeground( Color.blue);
			titleLbl.setFont( creditFont );
		}

		if( titleLbl == null) {  //this is normal operation
			titleLbl = new JLabel( titleGif);
		}

		JComponent creditPane = makeCreditPane();

		//Button Panel
		JPanel buttonPane = new JPanel();
		JButton button = new JButton( "Close");
		buttonPane.add( button);
		button.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e){
				dispose();
			}
		});

		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMain.add( titleLbl, c );

		pnlMain.add( Box.createVerticalStrut( INT_STRUT_SIZE ), c );

		c.ipadx = 16;

		String implTitle, implVersion;
		//Does not work yet.
		// Currently these are null when the package is split up
		//    into multiple files, but when in a single file with
		//    correct manifest it works fine.
		Package pkg = Package.getPackage("edu.ucla.belief.ui");
		if( pkg != null){
			implTitle = pkg.getImplementationTitle();
			if( implTitle == null ) implTitle = UI.STR_SAMIAM_ACRONYM + " Version";

			implVersion = pkg.getImplementationVersion();
			if( implVersion == null ) implVersion = "Preliminary Version";

			newRowSmall( implTitle + ":", implTitle, implVersion, c, pnlMain );
		}

		c.weightx = 0;

		pkg = Package.getPackage( "edu.ucla.belief" );
		if( pkg != null ){
			implTitle = pkg.getImplementationTitle();
			if( implTitle == null ) implTitle = "Inflib Version";

			implVersion = pkg.getImplementationVersion();
			if( implVersion == null ) implVersion = "Preliminary Version";

			newRowSmall( implTitle + ":", implTitle, implVersion, c, pnlMain );
		}

		//Other useful information
		newRowSmall( "Java Runtime Environment version:",               "JRE version",      System.getProperty( "java.version", "Unknown"),                  c, pnlMain );
		newRowSmall( "Java Runtime Environment specification version:", "JRE spec version", System.getProperty( "java.specification.version", "Unknown"),    c, pnlMain );
		newRowSmall( "Java Virtual Machine implementation version:",    "JVM impl version", System.getProperty( "java.vm.version", "Unknown"),               c, pnlMain );
		newRowSmall( "Java Virtual Machine specification version:",     "JVM spec version", System.getProperty( "java.vm.specification.version", "Unknown"), c, pnlMain );
		newRowSmall( "Operating system name:",                          "OS name",          System.getProperty( "os.name", "Unknown"),                       c, pnlMain );
		newRowSmall( "Operating system architecture:",                  "OS arch",          System.getProperty( "os.arch", "Unknown"),                       c, pnlMain );
		newRowSmall( "Operating system version:",                       "OS version",       System.getProperty( "os.version", "Unknown"),                    c, pnlMain );

		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMain.add( Box.createVerticalStrut( 8 ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		pnlMain.add( makeEnvironmentPane(), c );

		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.ipadx = 0;

		pnlMain.add( Box.createVerticalStrut( INT_STRUT_SIZE ), c );

		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMain.add( creditPane, c );

		pnlMain.add( Box.createVerticalStrut( INT_STRUT_SIZE ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMain.add( buttonPane, c );

		pnlMain.setBorder( BorderFactory.createEmptyBorder( 8,22,8,22 ) );

		getContentPane().add( pnlMain );

		pack();

		Window owner = getOwner();
		if( owner != null) {
			setLocationRelativeTo( owner);
		}
	}

	/** @since 20060118 */
	private static JLabel newRowSmall( String caption, String tip, String data, GridBagConstraints c, JComponent pnlMain ){
		JLabel label = null;
		if( (data == null) || (data.length() < 1) ) return label;

		int    gridwidth = c.gridwidth;
		int    anchor    = c.anchor;
		//double weightx   = c.weightx;
		//int    fill      = c.fill;

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;//k
		pnlMain.add( label = new JLabel( caption ), c );
		label.setToolTipText( tip );
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMain.add( label = new JLabel( data ), c );
		label.setToolTipText( data );

		//c.fill      = fill;
		//c.weightx   = weightx;
		c.anchor    = anchor;
		c.gridwidth = gridwidth;

		return label;
	}

	/** @author Keith Cascio
		@since 20060110 */
	private JComponent makeEnvironmentPane(){
		JPanel pnl = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		final JTextField tfLaunchScriptFinal  = newRow( "launch program:", UI.STR_SAMIAM_ACRONYM + " was invoked using this program",                   UI.LAUNCH_SCRIPT,                                    c, pnl );
		final JTextField tfLaunchCommandFinal = newRow( "launch command:", "Full command line statement, invoked java to run " + UI.STR_SAMIAM_ACRONYM, UI.LAUNCH_COMMAND,                                   c, pnl );
		final JTextField tfClasspath          = newRow( "class path:",     "java class search path",                                                    System.getProperty( "java.class.path", "Unknown"),   c, pnl );
		String envVarName = BrowserControl.isWindowsPlatform() ? "PATH" : "LD_LIBRARY_PATH";
		final JTextField tfLibrarypathFinal   = newRow( "library path:",   "library search path ("+envVarName+" environment variable)",                 System.getProperty( "java.library.path", "Unknown"), c, pnl );

		Runnable scrollFixer = new Runnable(){
			public void run(){
				try{
					Thread.sleep( 1000 );
				}catch( InterruptedException interruptedexception ){
					return;
				}
				if( tfLaunchScriptFinal  != null )  tfLaunchScriptFinal.setScrollOffset(0);
				if( tfLaunchCommandFinal != null ) tfLaunchCommandFinal.setScrollOffset(0);
				if( tfClasspath          != null )          tfClasspath.setScrollOffset(0);
				if( tfLibrarypathFinal   != null )   tfLibrarypathFinal.setScrollOffset(0);
			}
		};
		new Thread( scrollFixer, "about dialog jtextfield scroll adjustment" ).start();
		//SwingUtilities.invokeLater( scrollFixer );//no go

		return pnl;
	}

	/** @since 20060118 */
	private static JTextField newRow( String caption, String tip, String data, GridBagConstraints c, JComponent pnl ){
		JTextField ret = null;
		if( (data == null) || (data.length() < 1) ) return ret;

		JLabel label;
		Dimension dim;

		int    gridwidth = c.gridwidth;
		double weightx   = c.weightx;
		int    anchor    = c.anchor;
		int    fill      = c.fill;

		c.gridwidth = 1;
		c.weightx = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		pnl.add( label = new JLabel( caption ), c );
		label.setToolTipText( tip );

		pnl.add( Box.createHorizontalStrut( 16 ), c );

		ret = new JTextField( data );
		ret.setEditable( false );
		dim = ret.getPreferredSize();
		dim.width = 32;
		ret.setPreferredSize( dim );
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		pnl.add( ret, c );
		//ret.setScrollOffset( 0 );
		ret.setToolTipText( data );
		new ClipboardHelper( ret, ret );

		c.fill      = fill;
		c.anchor    = anchor;
		c.weightx   = weightx;
		c.gridwidth = gridwidth;

		return ret;
	}

	protected JComponent makeCreditPane()
	{
		JPanel creditPane = new JPanel();
		creditPane.setLayout( new BoxLayout( creditPane, BoxLayout.Y_AXIS));

		JLabel t = new JLabel(UI.STR_SAMIAM_ACRONYM + " has been developed by the Automated Reasoning Group");
		Font creditFont = t.getFont();
		if( creditFont != null ) creditFont = creditFont.deriveFont( Font.BOLD );

		t.setForeground( Color.black);
		if( creditFont != null ) t.setFont( creditFont );
		creditPane.add( t);
		t = new JLabel("of Professor Adnan Darwiche at UCLA.");
		t.setForeground( Color.black);
		if( creditFont != null ) t.setFont( creditFont );
		creditPane.add( t);

		t = new JLabel(" "); //blank line
		creditPane.add( t);

		t = new JLabel("This version is licensed only for educational and research use.");
		t.setForeground( Color.black);
		if( creditFont != null ) t.setFont( creditFont );
		creditPane.add( t);

		return creditPane;
	}

	/**
		Tes/debug method.
		@author Keith Cascio
		@since 092602
	*/
	public static void main( String[] args )
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "There was an error setting the look and feel:" );
			e.printStackTrace();
		}

		AboutDlg AD = new AboutDlg( null );
		AD.show();

		System.exit(0);
	}
}
