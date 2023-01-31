package edu.ucla.belief.ui.dialogs;

import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.net.URL;

import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.actionsandmodes.SystemCallAction;
import edu.ucla.belief.ui.actionsandmodes.TutorialAction;
import edu.ucla.belief.ui.util.JOptionResizeHelper;
import edu.ucla.belief.ui.util.BrowserControl;
import edu.ucla.belief.ui.statusbar.StatusBar;

/**
	@author Keith Cascio
	@since 121802
*/
public class Tutorials implements HyperlinkListener, ItemListener
{
	public static final String URL_ACE = "http://reasoning.cs.ucla.edu/ace";
	public static final String URL_SAMIAM_READTHIS = "http://reasoning.cs.ucla.edu/samiam/researchguidelines.php";

	public static final String STR_WINDOWS_SCRIPT_FILENAME = "showtutorial.bat";
	public static final String RELATIVE_PATH_TUTORIALS = "htmlhelp/tutorials/";
	public static final String[] TUTORIALS_NAMES = new String[] {
				"tutorial_intro",
				"tutorial_create",
				"tutorial_inference",
				"tutorial_sensitivity",
				"tutorial_timespace",
				"tutorial_mapmpe",
				"tutorial_em",
				"tutorial_genie_files" };
	public static final String[] TUTORIALS_DISPLAY_NAMES = new String[] {
				"Introduction",
				"Creating Networks",
				"Inference",
				"Sensitivity Analysis",
				"Time-Space Tradeoffs",
				"MAP/MPE",
				"EM Learning",
				"Genie Files" };
	public static final String[] STR_STATEMENT = new String[] {
				"If this is your first time using "+UI.STR_SAMIAM_ACRONYM+",",
				"you may want to take a few minutes",
				"to view the introductory video tutorial",
				"or one of the other video tutorials",
				"about a specific topic that interests you." };

	public static final String STR_EXCLAMATION             = "Welcome to "+UI.STR_SAMIAM_ACRONYM+"!";
	public static final String STR_STATEMENT_TUTORIALS     = "If this is your first time using "+UI.STR_SAMIAM_ACRONYM+", you may want to take a few minutes to view the introductory video tutorial or one of the other video tutorials about a specific topic that interests you.";
	public static final String HTML_STATEMENT_CAPTION      = "Before you use "+UI.STR_SAMIAM_ACRONYM+", please read these <b>important messages</b>, which will be displayed only once:";
	public static final String HTML_STATEMENT_BENCHMARKING = "If you are using "+UI.STR_SAMIAM_ACRONYM+" for research purposes and plan to <b>benchmark</b> it against other tools, please <a href=\""+URL_SAMIAM_READTHIS+"\">read this</a>.";
	public static final String HTML_STATEMENT_ACE_PROMO    = "If you need xxxx xxxx <b>XXXX</b> xxxx xxxx xxxx, please consider using <a href=\""+URL_ACE+"\">ACE</a>, a new software package we provide designed to complement "+UI.STR_SAMIAM_ACRONYM+".";
	//public static final String HTML_MESSAGES = "<html><p>" + HTML_STATEMENT_CAPTION + "</p><ul><li>" + HTML_STATEMENT_BENCHMARKING + "</li><li>" + HTML_STATEMENT_ACE_PROMO + "</li><ul></html>";
	public static final String HTML_MESSAGES = HTML_STATEMENT_CAPTION + "<ul><li>" + HTML_STATEMENT_BENCHMARKING + "</li><li>" + HTML_STATEMENT_ACE_PROMO + "</li><ul>";

	protected TutorialAction[] myArrayActions;
	protected boolean myFlagOneFileExists = false;
	protected Icon myIcon;
	private StatusBar myStatusBar;

	/** @since 030804 */
	public static final String getWindowsScriptCommand()
	{
		File fileScript = new File( RELATIVE_PATH_TUTORIALS + STR_WINDOWS_SCRIPT_FILENAME );
		if( fileScript.exists() )
		{
			try{
				return fileScript.getCanonicalPath();
			}catch( Exception e ){
				return fileScript.getAbsolutePath();
			}
		}
		else return null;
	}

	public Tutorials(){
		//myIcon = MainToolBar.getIcon( "Tutorials16.gif" );
	}

	/** @since 20051012 */
	private TutorialAction[] getArrayActions(){
		if( myArrayActions == null ){
			myArrayActions = new TutorialAction[ TUTORIALS_NAMES.length ];
			for( int i=0; i<myArrayActions.length; i++ )
			{
				myArrayActions[i] = new TutorialAction( "Play!", RELATIVE_PATH_TUTORIALS, TUTORIALS_NAMES[i], 'p', myIcon );
			}
		}
		return myArrayActions;
	}

	protected void checkExistence()
	{
		myFlagOneFileExists = false;

		String pathTest;
		File fileTest;
		boolean tempExists;
		TutorialAction[] array = getArrayActions();
		for( int i=0; i<array.length; i++ )
		{
			pathTest = (String) array[i].getValue( Action.SHORT_DESCRIPTION );
			fileTest = new File( pathTest );
			tempExists = fileTest.exists();
			myFlagOneFileExists |= tempExists;
			array[i].setEnabled( tempExists );
		}
	}

	protected void checkEnabled()
	{
		myFlagOneFileExists = false;

		TutorialAction[] array = getArrayActions();
		for( int i=0; i<array.length; i++ )
		{
			array[i].checkExistence();
			if( array[i].isEnabled() ){
				myFlagOneFileExists = true;
				break;
			}
		}
	}

	public boolean showDialog( boolean welcome, String title, Component parent )
	{
		//checkExistence();
		checkEnabled();

		if( welcome || myFlagOneFileExists )
		{
			JComponent comp = welcome ? getWelcomeComponent() : getInfoComponent();
			new JOptionResizeHelper( comp, true, (long)10000 ).start();
			JOptionPane.showMessageDialog( parent, comp, title, JOptionPane.PLAIN_MESSAGE );
			return true;
		}
		else return false;
	}

	public boolean showDialog( Component parent )
	{
		return showDialog( true, STR_EXCLAMATION, parent );
	}

	public JComponent getWelcomeComponent()
	{
		if( myWelcomePanel == null ) myWelcomePanel = makePanel( true );
		return myWelcomePanel;
	}

	public JComponent getInfoComponent()
	{
		if( myInfoPanel == null ) myInfoPanel = makePanel( false );
		return myInfoPanel;
	}

	protected JComponent myInfoPanel = null;
	protected JComponent myWelcomePanel = null;

	protected JComponent makePanel( boolean welcome )
	{
		JPanel pnlInner = new JPanel( new GridBagLayout() );

		//Color background = pnlInner.getBackground();

		JLabel lbl = null;
		JButton btn = null;
		Action action = null;
		GridBagConstraints c = new GridBagConstraints();

		if( welcome )
		{
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			lbl = new JLabel( STR_EXCLAMATION, JLabel.CENTER );
			lbl.setFont( lbl.getFont().deriveFont( (float)18 ) );
			pnlInner.add( lbl, c );

			if( myFlagOneFileExists ){
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.anchor = GridBagConstraints.WEST;
				c.weightx = 0;
				c.fill = GridBagConstraints.NONE;

				//pnlInner.add( Box.createHorizontalStrut( 512 ), c );

				//for( int i=0; i<STR_STATEMENT.length; i++ )
				//{
				//	lbl = new JLabel( STR_STATEMENT[i], JLabel.LEFT );
				//	pnlInner.add( lbl, c );
				//}

				c.weightx = 1;
				c.fill = GridBagConstraints.HORIZONTAL;
				//Font fontStatementTutorials = new Font( "arial", Font.PLAIN, 12 );
				pnlInner.add( makeWrappingTextComponent( STR_STATEMENT_TUTORIALS ), c );//, fontStatementTutorials, background ), c );

				c.weightx = 0;
				c.fill = GridBagConstraints.NONE;
				pnlInner.add( Box.createVerticalStrut( INT_SIZE_STRUT ), c );
			}
		}

		if( myFlagOneFileExists ){
			if( welcome ) addConciseTutorialButtons( pnlInner, c );
			else addSimpleTutorialButtons( pnlInner, c );
		}

		if( welcome )
		{
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			pnlInner.add( Box.createVerticalStrut( 16 ), c );

			c.weightx = c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			JEditorPane editorpain = new JEditorPane( "text/html", HTML_MESSAGES );
			editorpain.setEditable( false );
			editorpain.addHyperlinkListener( (HyperlinkListener)Tutorials.this );
			//editorpain.setBackground( pnlInner.getBackground() );
			JScrollPane scrollpain = new JScrollPane( editorpain );
			pnlInner.add( scrollpain, c );

			c.weightx = c.weighty = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			pnlInner.add( myStatusBar = new StatusBar( (double)16, (double)1 ), c );

			c.fill = GridBagConstraints.NONE;
		}

		Border outsideBorder = BorderFactory.createEtchedBorder();
		Border insideBorder = BorderFactory.createEmptyBorder( 8, 16, 8, 16 );
		Border compoundBorder = BorderFactory.createCompoundBorder( outsideBorder, insideBorder);

		pnlInner.setBorder( compoundBorder );

		JPanel ret = new JPanel( new GridBagLayout() );
		c = new GridBagConstraints();
		c.weightx = c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( pnlInner, c );

		if( welcome ) ret.setPreferredSize( new Dimension( 460, 350 ) );//560, 450 ) );

		return ret;
	}

	/** @since 20051012 */
	private void addSimpleTutorialButtons( Container pnlInner, GridBagConstraints c ){
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;

		c.gridwidth = 1;
		c.weightx = 1;
		pnlInner.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		c.weightx = 0;
		pnlInner.add( Box.createHorizontalStrut( 128 ), c );

		c.weightx = 3;
		pnlInner.add( Box.createHorizontalStrut( 64 ), c );

		c.weightx = 0;
		pnlInner.add( Box.createHorizontalStrut( 32 ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 2;
		pnlInner.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		TutorialAction[] array = getArrayActions();
		for( int i=0; i<TUTORIALS_DISPLAY_NAMES.length; i++ )
		{
			c.gridwidth = 1;
			c.weightx = 1;
			pnlInner.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

			c.weightx = 0;
			pnlInner.add( new JLabel( TUTORIALS_DISPLAY_NAMES[i], JLabel.LEFT ), c );

			c.weightx = 3;
			pnlInner.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

			c.weightx = 0;
			pnlInner.add( new JButton( array[i] ), c );

			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 2;
			pnlInner.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );
		}
	}

	/** @since 20051012 */
	private void addConciseTutorialButtons( Container pnlInner, GridBagConstraints c ){
		myComboConcise = new JComboBox( TUTORIALS_DISPLAY_NAMES );
		myComboConcise.addItemListener( (ItemListener)Tutorials.this );

		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;

		c.gridwidth = 1;
		c.weightx = 1;
		pnlInner.add( Box.createHorizontalStrut( 8 ), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 3;
		pnlInner.add( myComboConcise, c );

		c.fill = GridBagConstraints.NONE;
		c.weightx = 1;
		pnlInner.add( Box.createHorizontalStrut( 64 ), c );

		c.weightx = 0;
		pnlInner.add( myButtonConcise = new JButton( myActionConcise = new TutorialAction( "Play!" ) ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		pnlInner.add( Box.createHorizontalStrut( 8 ), c );

		refreshActionConcise();
	}

	private JComboBox myComboConcise;
	private JButton myButtonConcise;
	private TutorialAction myActionConcise;

	/** interface ItemListener
		@since 20051005 */
	public void itemStateChanged( ItemEvent e ){
		//System.out.println( "Tutorials.itemStateChanged()" );
		if( e.getStateChange() == ItemEvent.SELECTED ) refreshActionConcise();
	}

	/** @since 20051005 */
	private void refreshActionConcise(){
		//System.out.println( "Tutorials.refreshActionConcise()" );
		int index = myComboConcise.getSelectedIndex();
		myActionConcise.setPath( RELATIVE_PATH_TUTORIALS, TUTORIALS_NAMES[index] );
	}

	/** interface HyperlinkListener
		@since 20051005 */
	public void hyperlinkUpdate( HyperlinkEvent e ){
		HyperlinkEvent.EventType type = e.getEventType();
		if( type == HyperlinkEvent.EventType.ACTIVATED ){
			URL url = e.getURL();
			if( url != null ) BrowserControl.displayURL( url );
		}
		else if( (type == HyperlinkEvent.EventType.ENTERED) && (myStatusBar != null) ){
			URL url = e.getURL();
			if( url != null ) myStatusBar.setText( url.toString(), StatusBar.WEST );
		}
	}

	/** @since 20051005 */
	private static JComponent makeWrappingTextComponent( String text ){//, Font font, Color background ){
		//JTextArea ta = new JTextArea( text );
		//ta.setFont( font );
		//ta.setBackground( background );
		//ta.setEditable( false );
		//ta.setLineWrap( true );
		//ta.setWrapStyleWord( true );
		//JScrollPane pain = new JScrollPane( ta );
		//pain.setBorder( (Border)null );
		//return pain;
		JLabel ret = new JLabel( "<html>" + text );
		return ret;
	}

	/**
		Test/debug method.
	*/
	public static void main( String[] args )
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		Tutorials T = new Tutorials();
		T.showDialog( null );

		System.exit(0);
	}

	public static int INT_SIZE_STRUT = (int)8;
}