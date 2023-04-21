package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.util.*;

import edu.ucla.util.code.*;

import java.util.List;
import java.util.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.io.*;

/** @author Keith Cascio
	@since 032605 */
public class ScriptGeniusWizard extends AbstractWizard implements Wizard
{
	public ScriptGeniusWizard(){
		super();
		init();
	}

	private void init(){
		staticInit();
		STAGE_CHOOSE_SCRIPTS.setNext( STAGE_RESOLVE );
		STAGE_RESOLVE.setNext( STAGE_CHOOSE_DESTINATION );
		STAGE_CHOOSE_DESTINATION.setNext( STAGE_CHOOSE_LANGUAGES );
	}

	private static void staticInit(){
		if( GENII != null ) return;

		GENII = new ScriptGenius[] {
			GENIUS_WINDOWS_DEFAULT = new BatchScriptGenius(),
			GENIUS_UNIX_DEFAULT = new BourneScriptGenius(),
			new CShellScriptGenius(),
			new PerlScriptGenius()};
		SCRIPTS = new Script[] { getJDKCompile(), getJDKRun() };
	}

	/** @since 033105 */
	public static JDKCompile getJDKCompile(){
		if( SCRIPT_JDKCOMPILE == null ) SCRIPT_JDKCOMPILE = new JDKCompile();
		return SCRIPT_JDKCOMPILE;
	}

	/** @since 033105 */
	public static JDKRun getJDKRun(){
		if( SCRIPT_JDKRUN == null ) SCRIPT_JDKRUN = new JDKRun();
		return SCRIPT_JDKRUN;
	}

	private static ScriptGenius[] GENII;
	private static Script[] SCRIPTS;
	private static ScriptGenius GENIUS_WINDOWS_DEFAULT, GENIUS_UNIX_DEFAULT, GENIUS_RECOMMENDED;
	private static JDKCompile SCRIPT_JDKCOMPILE;
	private static JDKRun SCRIPT_JDKRUN;

	public void actionPerformed( ActionEvent evt ){
		Object src = evt.getSource();
		if( src == myButtonBrowseDest ) doBrowseDest();
		else if( src == myButtonFinish ) doFinish();
	}

	public Stage getFirst(){
		return STAGE_CHOOSE_SCRIPTS;
	}

	public static final Dimension DIM_WINDOW_DEFAULT = new Dimension( 500, 290 );

	/** @since 032805 */
	public Dimension getPreferredSize(){
		return DIM_WINDOW_DEFAULT;
	}

	private void doFinish(){
		try{
			ScriptSelector ss;
			//Script script;
			//LanguageSelector ls;
			ScriptGenius genius;
			LinkedList written = new LinkedList();
			for( int i=0; i<myScriptSelectors.length; i++ ){
				if( myScriptSelectors[i].isSelected() ){
					ss = myScriptSelectors[i];
					for( int j=0; j<myLanguageSelectors.length; j++ ){
						if( myLanguageSelectors[j].isSelected() ){
							genius = myLanguageSelectors[j].getGenius();
							written.add( write( ss, genius ) );
						}
					}
				}
			}
			myButtonFinish.setEnabled( false );
			String message = "Wrote:\n";
			for( Iterator it = written.iterator(); it.hasNext(); ){
				message += ((File)it.next()).getAbsolutePath() + "\n";
			}
			String title = Integer.toString( written.size() )  + " scripts written";
			JOptionPane.showMessageDialog( myPanelLanguages, message, title, JOptionPane.PLAIN_MESSAGE );
			getWizardPanel().setTextCancelButton( "Close", "Exit wizard" );
		}catch( Throwable throwable ){
			//System.err.println( throwable );
			String errormsg = throwable.getMessage();
			if( errormsg == null ) errormsg = throwable.toString();
			JOptionPane.showMessageDialog( myPanelLanguages, errormsg, "Error writing scripts", JOptionPane.ERROR_MESSAGE );
		}
	}

	private File write( ScriptSelector ss, ScriptGenius genius ) throws Exception {
		String path = myDestination.getAbsolutePath() + File.separator + ss.getFileName() + "." + genius.getScriptFileExtension();
		File ofile = new File( path );
		ss.getScript().write( genius, getSystemSoftwareSource(), ofile );
		return ofile;
	}

	/** @since 033105 */
	public SystemSoftwareSource getSystemSoftwareSource(){
		if( mySystemSoftwareSource == null ) mySystemSoftwareSource = new GuessingSource();
		return mySystemSoftwareSource;
	}

	private SystemSoftwareSource mySystemSoftwareSource;

	private Stage STAGE_CHOOSE_LANGUAGES = new Stage( "Choose Languages" ){
		public JComponent refresh() throws Exception {
			return ScriptGeniusWizard.this.refreshPanelLanguages();
		}

		public boolean isGreenLightNext(){
			return super.isGreenLightNext() && ScriptGeniusWizard.this.isLanguageSelected();
		}

		public Stage previous(){
			ScriptGeniusWizard.this.decideFinishEnabled();
			return super.previous();
		}

		public JComponent getView( boolean validate ) throws Exception {
			ScriptGeniusWizard.this.decideFinishEnabled();
			return super.getView( validate );
		}

		public String getProgressMessage(){
			return "choose languages";
		}
	};

	private JComponent refreshPanelLanguages() throws Exception {
		if( myPanelLanguages == null ){
			LanguageSelector[] selectors = getLanguageSelectors();

			myPanelLanguages = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.WEST;

			c.weighty = 1;
			myPanelLanguages.add( Box.createVerticalStrut(1), c );
			c.weighty = 0;

			for( int i=0; i<selectors.length; i++ ){
				c.gridwidth = 1;
				c.weightx = 1;
				c.fill = GridBagConstraints.HORIZONTAL;
				myPanelLanguages.add( Box.createHorizontalStrut(1), c );
				//myPanelLanguages.add( Box.createHorizontalGlue(), c );
				c.weightx = 0;
				c.fill = GridBagConstraints.NONE;
				myPanelLanguages.add( selectors[i], c );
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.weightx = 2;
				c.fill = GridBagConstraints.HORIZONTAL;
				myPanelLanguages.add( Box.createHorizontalStrut(1), c );
				//myPanelLanguages.add( Box.createHorizontalGlue(), c );
			}

			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			c.weighty = 1;
			myPanelLanguages.add( Box.createVerticalStrut(1), c );
			c.weighty = 0;

			c.gridwidth = 1;
			c.weightx = 1;
			myPanelLanguages.add( Box.createHorizontalStrut(1), c );
			c.weightx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.EAST;
			myPanelLanguages.add( myButtonFinish = new JButton( "Write Scripts" ), c );
			myButtonFinish.addActionListener( (ActionListener)this );

			c.weighty = 1;
			myPanelLanguages.add( Box.createVerticalStrut(1), c );
			c.weighty = 0;

			border( myPanelLanguages );
		}
		setLanguageSelected( getRecommendedLanguage(), /*selected*/true, /*additive*/false, /*recommended*/true );
		decideFinishEnabled();
		return myPanelLanguages;
	}

	/** @since 033105 */
	public ScriptGenius getRecommendedLanguage(){
		if( GENIUS_RECOMMENDED == null ){
			GENIUS_RECOMMENDED = BrowserControl.isWindowsPlatform() ? GENIUS_WINDOWS_DEFAULT : GENIUS_UNIX_DEFAULT;
		}
		return GENIUS_RECOMMENDED;
	}

	private JPanel myPanelLanguages;
	private LanguageSelector[] myLanguageSelectors;
	private JButton myButtonFinish;

	private void decideFinishEnabled(){
		if( myButtonFinish == null ) return;
		myButtonFinish.setEnabled( ScriptGeniusWizard.this.isLanguageSelected() );
	}

	private ActionListener getFinishActionListener(){
		if( myFinishActionListener == null ){
			myFinishActionListener = new ActionListener(){
				public void actionPerformed( ActionEvent e ){
					ScriptGeniusWizard.this.decideFinishEnabled();
				}
			};
		}
		return myFinishActionListener;
	}
	private ActionListener myFinishActionListener;

	private void setLanguageSelected( ScriptGenius genius, boolean flag, boolean additive, boolean recommended ){
		if( myLanguageSelectors == null ) return;
		for( int i=0; i<myLanguageSelectors.length; i++ ){
			if( myLanguageSelectors[i].getGenius() == genius ){
				myLanguageSelectors[i].setSelected( true );
				if( recommended ) myLanguageSelectors[i].setRecommended( true );
			}
			else if( !additive ){
				myLanguageSelectors[i].setSelected( false );
				if( recommended ) myLanguageSelectors[i].setRecommended( false );
			}
		}
		ScriptGeniusWizard.this.fireResetNavigation();
	}

	private boolean isLanguageSelected(){
		if( myLanguageSelectors != null ){
			for( int i=0; i<myLanguageSelectors.length; i++ ){
				if( myLanguageSelectors[i].isSelected() ) return true;
			}
		}
		return false;
	}

	private LanguageSelector[] getLanguageSelectors(){
		if( myLanguageSelectors == null ){
			myLanguageSelectors = new LanguageSelector[ GENII.length ];
			for( int i=0; i<GENII.length; i++ ) myLanguageSelectors[i] = new LanguageSelector( GENII[i] );
		}
		return myLanguageSelectors;
	}

	public static final Color COLOR_RECOMMENDED = new Color( 0x00, 0xcc, 0x00 );

	public class LanguageSelector extends JPanel
	{
		public LanguageSelector( ScriptGenius genius ){
			super( new GridBagLayout() );
			this.myGenius = genius;
			init();
		}

		private void init(){
			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = 1;
			this.add( myBox = new JCheckBox( myGenius.getScriptLanguageDescription() ), c );
			myBox.setFont( myBox.getFont().deriveFont( (float) 20 ) );
			myBox.addActionListener( getNavActionListener() );
			myBox.addActionListener( getFinishActionListener() );
			this.add( Box.createHorizontalStrut(4), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			this.add( myLabelRecommended = new JLabel(), c );
			myLabelRecommended.setForeground( COLOR_RECOMMENDED );
		}

		public boolean isSelected(){
			return myBox.isSelected();
		}

		public void setSelected( boolean flag ){
			myBox.setSelected( flag );
		}

		public ScriptGenius getGenius(){
			return myGenius;
		}

		public void setRecommended( boolean flag ){
			String text = "";
			if( flag ) text = "(recommended for your system)";
			myLabelRecommended.setText( text );
		}

		private ScriptGenius myGenius;
		private JCheckBox myBox;
		private JLabel myLabelRecommended;
	}

	private ActionListener getNavActionListener(){
		if( myNavActionListener == null ){
			myNavActionListener = new ActionListener(){
				public void actionPerformed( ActionEvent e ){
					ScriptGeniusWizard.this.fireResetNavigation();
					ScriptGeniusWizard.this.STAGE_RESOLVE.invalidate();
				}
			};
		}
		return myNavActionListener;
	}
	private ActionListener myNavActionListener;

	private Stage STAGE_CHOOSE_SCRIPTS = new Stage( "Choose Scripts" ){
		public JComponent refresh() throws Exception {
			return ScriptGeniusWizard.this.refreshPanelScripts();
		}

		public boolean isGreenLightNext(){
			return super.isGreenLightNext() && ScriptGeniusWizard.this.isScriptValid();
		}

		public String getProgressMessage(){
			return "choose scripts";
		}
	};

	private JComponent refreshPanelScripts() throws Exception {
		if( myPanelScripts == null ){
			ScriptSelector[] selectors = getScriptSelectors();

			myPanelScripts = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;

			c.gridwidth = 1;
			myPanelScripts.add( new JLabel( "<html><nobr><b>Script" ), c );
			myPanelScripts.add( Box.createHorizontalStrut(32), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			myPanelScripts.add( new JLabel( "<html><nobr><b>File Name</b> (no extension)" ), c );

			myPanelScripts.add( Box.createVerticalStrut(32), c );

			for( int i=0; i<selectors.length; i++ ){
				selectors[i].addTo( myPanelScripts, c );
			}

			border( myPanelScripts );
		}
		refreshScriptSelectors();
		return myPanelScripts;
	}

	private JPanel myPanelScripts;
	private ScriptSelector[] myScriptSelectors;

	private void refreshScriptSelectors(){
		if( myScriptSelectors == null ) return;
		for( int i=0; i<myScriptSelectors.length; i++ ){
			myScriptSelectors[i].refresh();
		}
	}

	private boolean isScriptValid(){
		if( myScriptSelectors != null ){
			for( int i=0; i<myScriptSelectors.length; i++ ){
				if( myScriptSelectors[i].isValid() ) return true;
			}
		}
		return false;
	}

	private ScriptSelector[] getScriptSelectors(){
		if( myScriptSelectors == null ){
			myScriptSelectors = new ScriptSelector[ SCRIPTS.length ];
			for( int i=0; i<SCRIPTS.length; i++ ) myScriptSelectors[i] = new ScriptSelector( SCRIPTS[i] );
		}
		return myScriptSelectors;
	}

	public class ScriptSelector extends Object implements ActionListener
	{
		public ScriptSelector( Script script ){
			//super( new GridBagLayout() );
			this.myScript = script;
			init();
		}

		private void init(){
			myBox = new JCheckBox( myScript.getName() );
			myBox.setFont( myBox.getFont().deriveFont( (float)22 ) );
			myBox.setToolTipText( myScript.getDescriptionComment() );
			myBox.addActionListener( getNavActionListener() );
			myBox.addActionListener( (ActionListener) this );
			myTf = new NotifyField( "", 10 );
			myTf.addActionListener( getNavActionListener() );
		}

		public void actionPerformed( ActionEvent e ){
			validateEnabled();
		}

		private void validateEnabled(){
			myTf.setEnabled( myBox.isSelected() );
		}

		public void addTo( Container container, GridBagConstraints c ){
			c.gridwidth = 1;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			container.add( myBox, c );
			container.add( Box.createHorizontalStrut(1), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			container.add( myTf, c );
		}

		public void refresh(){
			myBox.setSelected( true );
			myTf.setText( myScript.getDefaultFileName() );
			validateEnabled();
		}

		public String getFileName(){
			return myTf.getText();
		}

		public boolean isValid(){
			return isSelected() && (getFileName().length() > 1);
		}

		public boolean isSelected(){
			return myBox.isSelected();
		}

		public void setSelected( boolean flag ){
			myBox.setSelected( flag );
		}

		public Script getScript(){
			return myScript;
		}

		private Script myScript;
		private JCheckBox myBox;
		private JTextField myTf;
	}

	private void getDependencies(){
		if( myScriptSelectors == null ) return;
		myPanelResolve.clearDependencies();
		Script script;
		SoftwareEntity[] deps;
		for( int i=0; i<myScriptSelectors.length; i++ ){
			if( myScriptSelectors[i].isValid() ){
				script = myScriptSelectors[i].getScript();
				deps = script.getDependencies();
				if( deps != null ){
					for( int j=0; j<deps.length; j++ ){
						//container.add( deps[j] );
						myPanelResolve.addDependency( deps[j], script );
					}
				}
			}
		}
	}

	private Stage STAGE_RESOLVE = new Stage( "Resolve System Software Dependencies" ){
		public JComponent refresh() throws Exception {
			return ScriptGeniusWizard.this.refreshPanelResolve();
		}

		//public boolean isGreenLightNext(){
		//	return false;
		//}

		public Stage next() throws Exception {
			ScriptGeniusWizard.this.myPanelResolve.validateDependencies();
			return super.next();
		}

		public String getProgressMessage(){
			return "searching for required system software";
		}

		public boolean isDelayLikely(){
			return (ScriptGeniusWizard.this.myPanelResolve == null) || ScriptGeniusWizard.this.myPanelResolve.isDelayLikely();
		}
	};

	private ResolutionPanel myPanelResolve;
	private Set myDependencies;

	private JComponent refreshPanelResolve() throws Exception {
		if( myPanelResolve == null ){
			myPanelResolve = new ResolutionPanel();
			border( myPanelResolve );
		}
		getDependencies();
		myPanelResolve.fill();
		return myPanelResolve;
	}

	private Stage STAGE_CHOOSE_DESTINATION = new Stage( "Choose Destination Directory" ){
		public JComponent refresh() throws Exception {
			return ScriptGeniusWizard.this.refreshPanelDestination();
		}

		//public boolean isGreenLightNext(){
		//	return super.isGreenLightNext() && ScriptGeniusWizard.this.getValidDestination();
		//}

		public Stage next() throws Exception {
			myDestination = ScriptGeniusWizard.this.getValidDestination();
			return super.next();
		}

		public String getProgressMessage(){
			return "choose destination";
		}
	};

	private JPanel myPanelDestination;
	private NotifyField myTfDest;
	private JButton myButtonBrowseDest;
	private File myDestination;

	private JComponent refreshPanelDestination() throws Exception {
		if( myPanelDestination == null ){
			myPanelDestination = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;

			c.gridwidth = 1;
			myPanelDestination.add( new JLabel( "Destination:" ), c );
			myPanelDestination.add( Box.createHorizontalStrut(8), c );

			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			myPanelDestination.add( myTfDest = new NotifyField( "", 10 ), c );
			myTfDest.addActionListener(	new ActionListener(){
				public void actionPerformed( ActionEvent event ){
					myDestination = (File)null;
				}
			}
			);

			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			c.gridwidth = GridBagConstraints.REMAINDER;
			myPanelDestination.add( myButtonBrowseDest = new JButton( "Browse" ), c );
			myButtonBrowseDest.addActionListener( (ActionListener) ScriptGeniusWizard.this );

			border( myPanelDestination );
		}
		if( (myTfDest.getText().trim().length() < 1) && (myDefaultDirectory != null) && (myDefaultDirectory.exists()) ){
			myTfDest.setText( myDefaultDirectory.getAbsolutePath() );
		}
		return myPanelDestination;
	}

	private void doBrowseDest(){
		JFileChooser chooser = getChooserDest();
		chooser.showOpenDialog( myPanelDestination );
		File file = chooser.getSelectedFile();
		if( (file != null) && (file.exists()) ){
			if( myTfDest != null ) myTfDest.setText( file.getAbsolutePath() );
		}
	}

	private File getValidDestination() throws Exception{
		String msgerror = null;
		try{
			if( myTfDest == null ){
				msgerror = "myTfDest == null";
				return (File)null;
			}
			String path = myTfDest.getText().trim();
			if( path.length() < 1 ){
				msgerror = "destination directory required";
				return (File)null;
			}
			File dest = new File( path );
			if( !dest.exists() ){
				msgerror = "\"" + path + "\" does not exist";
				return (File)null;
			}
			if( !dest.isDirectory() ){
				msgerror = "\"" + path + "\" is not a directory";
				return (File)null;
			}
			return dest;
		}finally{
			if( msgerror != null ) throw new Exception( msgerror );
		}
	}

	private JFileChooser myJFileChooser;
	private javax.swing.filechooser.FileFilter myFilterDest;
	private File myDefaultDirectory;

	public void setDefaultDirectory( File dir ){
		this.myDefaultDirectory = dir;
	}

	private JFileChooser getChooserDest(){
		if( myJFileChooser == null ){
			JFileChooser chooser = myJFileChooser = new JFileChooser();
			chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
			chooser.setAcceptAllFileFilterUsed( true );

			chooser.addChoosableFileFilter( getFilterDest() );
			chooser.setFileFilter( getFilterDest() );

			//chooser.setAccessory();

			chooser.setApproveButtonText( "Select" );
			//chooser.setApproveButtonToolTipText( STR_TOOLTIP_PRE + descrip );
			chooser.setDialogTitle( "Select destination directory for scripts" );
			chooser.setMultiSelectionEnabled( false );
		}

		String path = null;
		if( (myTfDest != null) && ((path = myTfDest.getText().trim()).length() > 0 ) ){
			File current = new File( path );
			if( current.exists() ){
				if( !current.isDirectory() ) current = current.getParentFile();
				myJFileChooser.setCurrentDirectory( current );
			}
		}
		else if( myDefaultDirectory != null ){
			myJFileChooser.setCurrentDirectory( myDefaultDirectory );
			myDefaultDirectory = null;
		}

		return myJFileChooser;
	}

	private javax.swing.filechooser.FileFilter getFilterDest(){
		if( myFilterDest == null ){
			myFilterDest = new javax.swing.filechooser.FileFilter(){
				public boolean accept( File f ){
					return f.isDirectory();
				}

				public String getDescription(){
					return "directories";
				}
			};
		}
		return myFilterDest;
	}

	/** test/debug */
	public static void main( String[] args ){
		String pathDefault = "C:\\keith\\code\\argroup\\inflib";
		Util.setLookAndFeel();

		ScriptGeniusWizard wizard = new ScriptGeniusWizard();
		wizard.setDefaultDirectory( new File( pathDefault ) );
		WizardPanel wp = wizard.getWizardPanel();

		JFrame frame = Util.getDebugFrame( "?", wp );
		frame.setSize( DIM_WINDOW_DEFAULT );
		wp.reset();
		frame.setVisible( true );
	}
}
