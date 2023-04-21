package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.statusbar.StatusBar;
import edu.ucla.belief.ui.dialogs.EMLearningDlg;
import edu.ucla.belief.ui.dialogs.*;
import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.actionsandmodes.InterruptableAction;
import edu.ucla.belief.ui.preference.SamiamPreferences;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.util.ChangeListener;
import edu.ucla.util.ChangeEvent;
import edu.ucla.util.code.*;
import edu.ucla.util.Interruptable;

import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;
import javax.swing.border.*;
import java.io.*;

/** @author keith cascio
	@since  20040506 */
public class CodeToolInternalFrame extends JInternalFrame implements
	ActionListener,
	ChangeListener,
	FileLocationBrowser.FileInformationSource,
	ScriptExecution.ProcessCustodian,
	Interruptable.Veto
{
	public static final String STR_MSG_CODE_PREFIX                = "writing code, ";
	public static final String STR_MSG_CODE_POSTFIX               = "...";
	public static final String STR_MSG_RESOLVING_PREFIX           = "searching system for software needed by \"";
	public static final String STR_MSG_RESOLVING_POSTFIX          = "\"...";

	public static final String STR_DISPLAY_NAME_1CAP              = "Code bandit";
	public static final String STR_DISPLAY_NAME                   = "Code Bandit";
	public static final String STR_FILENAME_ICON                  = "CodeBandit16.gif";
	public static final String STR_FILENAME_BUTTON_ICON           = "CodeBandit16x13.gif";
	public static final String STR_TOKEN_LAST_CODEBANDIT_LOCATION = "LastCodeBanditLocation";
	public static final String STR_TOKEN_VIEWER_APPLICATION       = "CodeBanditViewerApplication";

	public static final String STR_WARNING_QUESTION               = "\nContinue writing code?";
	public static final String STR_WARNING_TITLE                  = "Warnings";

	public static final int INT_PREF_WIDTH = 566;
	//public static final Dimension DIM_PREF_SIZE = new Dimension( 566,328 );//new Dimension( 566,296 );

	public CodeToolInternalFrame( NetworkInternalFrame fn )
	{
		super( STR_DISPLAY_NAME, true, true, true, true);

		hnInternalFrame = fn;
		//hnInternalFrame.addEvidenceChangeListener( this );
		//hnInternalFrame.addCPTChangeListener( this );
		//hnInternalFrame.addNetStructureChangeListener( this );
		//hnInternalFrame.addNodePropertyChangeListener( this );

		initStepActions();
		action_WRITECODE   = myActionWRITECODE;
		action_VIEWCODE    = myActionVIEWCODE;
		action_COMPILECODE = myActionCOMPILECODE;
		action_RUNCODE     = myActionRUNCODE;

		init();

		return;
	}

	/** @since 20060327 */
	public boolean failOnMissingNetworkFile( String path ){
		boolean ret = (path != null) && (!(new File(path).exists()));
		if( ret && (hnInternalFrame != null) ){
			hnInternalFrame.getParentFrame().showErrorDialog( "Please save network file \"" +path+ "\" before running code bandit." );
		}
		return ret;
	}

	/** @since 040705 */
	public Dimension calculatePreferredSize( Dimension dim ){
		if( dim == null ) dim = new Dimension();
		dim.width = INT_PREF_WIDTH;
		dim.height = getPanelNorth().getPreferredSize().height + 212;
		return dim;
	}

	/** @since 021505 */
	public void setOutputFile( File outputfile ){
		if( myTFBrowse != null ) myTFBrowse.setText( outputfile.getAbsolutePath() );
	}

	/** @since 021505 */
	public CodeOptionsPanel getCodeOptionsPanel(){
		return this.myCodeOptionsPanel;
	}

	/** @since 032305 */
	public void setResultIcon( Icon icon ){
		if( (myLabelDescriptionIcon == null) || (myLabelDescriptionCaption == null) ) return;

		if( myTADescription.getPreferredSize().height > 32 ){
			myLabelDescriptionIcon.setIcon( icon );
			myLabelDescriptionIcon.setVisible( true );
			myLabelDescriptionCaption.setIcon( null );
		}
		else{
			myLabelDescriptionCaption.setIcon( icon );
			myLabelDescriptionIcon.setIcon( null );
			myLabelDescriptionIcon.setVisible( false );
		}
	}

	/** @since 032305 */
	public static Icon getIcon( CodeGenius codegenius ){
		Icon ret = null;
		if( codegenius == null ) return ret;
		String fname = codegenius.getIconFileName();
		if( fname == null ) return ret;
		return MainToolBar.getIcon( fname );
	}

	private void init()
	{
		Icon myIconFrame = MainToolBar.getIcon( STR_FILENAME_ICON );
		if( myIconFrame != null ) setFrameIcon( myIconFrame );

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar( menuBar );

		JMenu menuActions = new JMenu( "Actions" );
		menuActions.add( action_WRITECODE );
		menuActions.add( action_SHOWDEPENDENCIES );
		menuActions.addSeparator();
		menuActions.add( action_CLOSE );
		menuBar.add( menuActions );

		JMenu menuTools = new JMenu( "Tools" );
		menuTools.add( action_GENERATESCRIPTS );
		menuTools.add( action_VIEWCODE );
		menuTools.addSeparator();
		menuTools.add( action_COMPILESETTINGS );
		menuTools.add( action_COMPILECODE );
		menuTools.addSeparator();
		menuTools.add( action_RUNSETTINGS );
		menuTools.add( action_RUNCODE );
		menuBar.add( menuTools );

		JMenu menuHelp = new JMenu( "Help" );
		menuHelp.add( hnInternalFrame.getParentFrame().action_HELPLOCALCODETOOL );
		if( FileSystemUtil.getAPIAction() != null ) menuHelp.add( FileSystemUtil.getAPIAction() );
		menuBar.add( menuHelp );

		getContentPane().removeAll();

		myPanelOptions = new JPanel( new GridBagLayout() );
		//JScrollPane pain = new JScrollPane( myPanelOptions );
		//JPanel panelEnclosesPain = new JPanel( new GridLayout() );
		//panelEnclosesPain.add( pain );

		myPanelOptions.setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Options" ) );

		JPanel pnlMain = new JPanel( new BorderLayout() );
		pnlMain.add( getPanelNorth(), BorderLayout.NORTH );
		pnlMain.add( myPanelOptions, BorderLayout.CENTER );
		pnlMain.add( getButtonsComponent(), BorderLayout.SOUTH );

		getContentPane().add( pnlMain );

		setPreferredSize( calculatePreferredSize( new Dimension() ) );
	}

	/** @since 040605 */
	private JComponent getPanelNorth(){
		if( myPanelNorth == null ){
			myPanelNorth = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();

			myLabelDescriptionCaption = makeCaption( "Result:" );
			myTADescription = new JTextArea();
			myTADescription.setEditable( false );
			myTADescription.setLineWrap( true );
			myTADescription.setWrapStyleWord( true );
			myTADescription.setBackground( Color.lightGray );
			Border borderExists = myTADescription.getBorder();
			Border borderOutside = BorderFactory.createLineBorder( Color.gray, 1 );
			Border borderCompound = BorderFactory.createCompoundBorder( borderOutside, borderExists );
			myTADescription.setBorder( borderCompound );
			myTADescription.setMargin( new Insets( 0, 4, 0, 2 ) );

			JPanel pnlResultCaption = new JPanel( new GridBagLayout() );
			GridBagConstraints constraintsCaption = new GridBagConstraints();
			constraintsCaption.gridwidth = GridBagConstraints.REMAINDER;
			constraintsCaption.anchor = GridBagConstraints.NORTHEAST;
			pnlResultCaption.add( myLabelDescriptionCaption, constraintsCaption );
			//pnlResultCaption.add( Box.createVerticalStrut( 4 ), constraintsCaption );
			constraintsCaption.anchor = GridBagConstraints.SOUTHEAST;
			pnlResultCaption.add( myLabelDescriptionIcon = new JLabel(), constraintsCaption );
			//pnlResultCaption.add( Box.createVerticalStrut( 4 ), constraintsCaption );

			Border inner = BorderFactory.createEmptyBorder( /*top*/2, /*left*/2, /*bottom*/2, /*right*/2 );
			Border outer = BorderFactory.createLineBorder( Color.black, 1 );
			Border compoundinner = BorderFactory.createCompoundBorder( /*outside*/outer, /*inside*/inner );
			Border emptyouter = BorderFactory.createEmptyBorder( /*top*/4, /*left*/0, /*bottom*/4, /*right*/0 );
			Border compound = BorderFactory.createCompoundBorder( /*outside*/emptyouter, /*inside*/compoundinner );
			myLabelDescriptionIcon.setBorder( compound );

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.NORTHWEST;
			myPanelNorth.add( pnlResultCaption, c );
			myPanelNorth.add( Box.createHorizontalStrut( 8 ), c );
			//c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			c.anchor = GridBagConstraints.NORTH;
			c.fill = GridBagConstraints.BOTH;//HORIZONTAL;
			//c.gridheight = 2;
			myPanelNorth.add( myTADescription, c );
			//c.anchor = GridBagConstraints.SOUTHWEST;
			//c.gridwidth = c.gridheight = 1;
			//c.fill = GridBagConstraints.NONE;
			//c.weightx = 0;
			//myPanelNorth.add( myLabelDescriptionIcon = new JLabel(), c );

			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			myPanelNorth.add( getStepsComponent(), c );

			JLabel lblFileCaption = new JLabel( "Output file: " );
			myTFBrowse = new NotifyField( "",10 );
			Dimension dim = myTFBrowse.getPreferredSize();
			dim.width = 200;
			myTFBrowse.setPreferredSize( dim );
			myTFBrowse.addActionListener( this );
			myButtonBrowse = new JButton( "Browse" );
			myButtonBrowse.setForeground( Color.darkGray );
			myButtonBrowse.addActionListener( this );

			myPanelNorth.add( Box.createVerticalStrut( 8 ), c );

			JComponent pnlBrowse = EMLearningDlg.makeBrowseComponent( lblFileCaption, myTFBrowse, myButtonBrowse );

			c.gridwidth = GridBagConstraints.REMAINDER;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			myPanelNorth.add( pnlBrowse, c );

			myPanelNorth.setBorder( BorderFactory.createEmptyBorder( 8,8,2,8 ) );
		}
		return myPanelNorth;
	}

	/** @since 040405 */
	private JComponent getButtonsComponent(){
		if( myPanelButtons == null ){
			JButton myButtonClose = new JButton( action_CLOSE );
			myButtonClose.setForeground( Color.darkGray );
			myButtonClose.setIcon( (Icon)null );
			//myButtonClose.addActionListener( this );

			myPanelButtons = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();

			c.gridwidth = 1;
			c.weightx = 1;
			myPanelButtons.add( Box.createHorizontalStrut(32), c );

			c.weightx = 0;
			c.anchor = GridBagConstraints.SOUTHWEST;
			myPanelButtons.add( myButtonClose, c );

			c.weightx = 0;//1;
			myPanelButtons.add( Box.createHorizontalStrut(16), c );

			/*c.weightx = 0;
			c.anchor = GridBagConstraints.SOUTHEAST;
			myPanelButtons.add( getStepsComponent(), c );

			c.weightx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			myPanelButtons.add( Box.createHorizontalStrut(32), c );*/

			//myPanelButtons = new JPanel( new FlowLayout( FlowLayout.TRAILING ) );
			//myPanelButtons.add( myButtonClose );
		}
		setGeniusButtonsEnabled( false );
		return myPanelButtons;
	}

	/** @since 040405 */
	private JComponent getStepsComponent(){
		if( myPanelSteps == null ){
			//JButton myButtonWriteCode = new JButton( action_WRITECODE );
			////myButtonWriteCode.setOpaque( true );
			////myButtonWriteCode.setBackground( myButtonWriteCode.getBackground().brighter() );
			////myButtonWriteCode.setBackground( new Color( 255, 100, 100 ) );
			////myButtonWriteCode.setForeground( Color.red );
			//myButtonWriteCode.addActionListener( this );

			//JButton myButtonShowDeps = new JButton( action_SHOWDEPENDENCIES );
			//myButtonShowDeps.setForeground( Color.darkGray );
			//myButtonShowDeps.addActionListener( this );

			//myPanelSteps.add( myButtonShowDeps );
			int step      = 1;
			myStepWrite   = new Step( action_WRITECODE,   step++ );
			myStepView    = new Step( action_VIEWCODE,    step++ );
			myStepCompile = new Step( action_COMPILECODE, step++ );
			myStepRun     = new Step( action_RUNCODE,     step++ );

			mySteps       = new Steps( new Step[] { myStepWrite, myStepView, myStepCompile, myStepRun } );

			myPanelSteps  = mySteps.makePanel();
		}
		return myPanelSteps;
	}

	private Steps mySteps;
	private Step myStepWrite, myStepView, myStepCompile, myStepRun;

	/** @since 20050406 */
	public static class Steps
	{
		public Steps( Step[] steps ){
			this.myArraySteps = steps;
			init();
		}

		public void memorize(){
			for( int i=0; i<Steps.this.myArraySteps.length; i++ )
				Steps.this.myArraySteps[i].memorize();
		}

		public void recall( boolean force ){
			for( int i=0; i<Steps.this.myArraySteps.length; i++ )
				Steps.this.myArraySteps[i].recall( force );
		}

		private void init(){
			int ceil = myArraySteps.length - 1;
			for( int i=0; i<ceil; i++ ){
				myArraySteps[i].setNext( myArraySteps[i+1] );
			}
			begin();
		}

		public void begin(){
			highlight( myArraySteps[0] );
		}

		public void highlight( Step step ){
			//System.out.println( "Steps.highlight( "+step+" )" );
			boolean highlight = false;
			for( int i=0; i<myArraySteps.length; i++ ){
				highlight = (myArraySteps[i] == step) && myArraySteps[i].isEnabled();
				myArraySteps[i].setHighlighted( highlight );
			}
		}

		public JPanel makePanel(){
			JPanel ret = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			JLabel label = makeCaption( "Steps:" );
			label.setBorder( BorderFactory.createEmptyBorder( /*top*/0, /*left*/12, /*bottom*/4, /*right*/8 ) );

			Component leftmost = label;
			for( int i=0; i<myArraySteps.length; i++ ){
				c.gridwidth = 1;
				c.fill = GridBagConstraints.NONE;
				ret.add( leftmost, c );
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.fill = GridBagConstraints.HORIZONTAL;
				ret.add( myArraySteps[i], c );
				leftmost = Box.createHorizontalGlue();
			}

			return ret;
		}

		private Step[] myArraySteps;
	}

	/** @since 040605 */
	public static class Step extends JPanel// implements ActionListener
	{
		public Step( StepAction action, int num ){
			super( new GridBagLayout() );
			this.myAction = action;
			this.myNum = num;
			init();
		}

		public boolean isEnabled(){
			return Step.this.myAction.isEnabled();
		}

		public boolean memorize(){
			return Step.this.myAction.memorize();
		}

		public boolean recall( boolean force ){
			return Step.this.myAction.recall( force );
		}

		public String toString(){
			return "Step["+myAction.getValue( Action.NAME )+"]";
		}

		//public void actionPerformed( ActionEvent actionevent ){
		//	this.setHighlighted( false );
		//	if( myNext != null ) myNext.setHighlighted( true );
		//}

		private void init(){
			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = 1;
			c.weightx = 0;
			this.add( myLabelNum = getLabelStep( myNum ), c );

			//this.add( Box.createHorizontalGlue(), c );

			myButton = new JButton( myAction );
			//myButton.addActionListener( (ActionListener)this );
			myButton.setHorizontalAlignment( SwingConstants.LEFT );
			myPanel = makeHighlightPanel( myButton );

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			this.add( myPanel, c );

			myFlagHighlighted = true;
			setHighlighted( false );
		}

		private JPanel makeHighlightPanel( JComponent comp ){
			/*JPanel ret = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();

			c.weightx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			ret.add( Box.createVerticalStrut(1), c );

			c.gridwidth = 1;
			ret.add( Box.createHorizontalStrut(8), c );

			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			ret.add( comp, c );

			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			c.gridwidth = GridBagConstraints.REMAINDER;
			ret.add( Box.createHorizontalStrut(8), c );

			ret.add( Box.createVerticalStrut(1), c );

			myBackground = ret.getBackground();*/

			JPanel ret = new JPanel( new GridLayout() );
			ret.add( comp );

			return ret;
		}

		private void highlight(){
			this.setHighlighted( true );
		}

		private void setHighlighted( boolean flag ){
			if( flag == myFlagHighlighted ) return;
			myFlagHighlighted = flag;
			//staticInit();
			String prefix = STR_PREFIX_NORMAL;
			//String postfix = STR_POSTFIX_NORMAL;
			Border border = BORDER_NORMAL;
			//Color background = myBackground;
			if( flag ){
				prefix = STR_PREFIX_HIGH;
				//postfix = STR_POSTFIX_HIGH;
				border = BORDER_HIGH;
				//background = BACKGROUND_HIGH;
			}
			myLabelNum.setText( prefix + myTextNum /*+ postfix*/ );
			myPanel.setBorder( border );
			//myPanel.setBackground( background );
		}

		/*private void staticInit(){
			if( FONT_NORMAL != null ) return;

			FONT_NORMAL = myLabelNum.getFont();
			FONT_BOLD = FONT_NORMAL.deriveFont( Font.BOLD );
		}*/

		private JLabel getLabelStep( int num ){
			myTextNum = "(" + Integer.toString( num ) + ")";
			JLabel label = new JLabel( "<html>", JLabel.LEFT );
			label.setBorder( BorderFactory.createEmptyBorder( /*top*/2, /*left*/2, /*bottom*/2, /*right*/6 ) );
			label.setText( STR_PREFIX_HIGH + myTextNum );
			Dimension dim = label.getPreferredSize();
			label.setPreferredSize( dim );
			label.setText( STR_PREFIX_NORMAL + myTextNum );
			return label;
		}

		public void setNext( Step step ){
			myNext = step;
		}

		private StepAction myAction;
		private int myNum;
		String myTextNum;
		private JLabel myLabelNum;
		private JButton myButton;
		private Step myNext;
		private JPanel myPanel;
		private boolean myFlagHighlighted;
		//private Color myBackground;

		//private static Font FONT_NORMAL;
		//private static Font FONT_BOLD;

		private static Border BORDER_HIGH = BorderFactory.createMatteBorder( 1,8,1,8,Color.yellow );
		private static Border BORDER_NORMAL = BorderFactory.createEmptyBorder( 1,8,1,8 );

		//private static Color FOREGROUND_NORMAL = Color.black;
		//private static Color FOREGROUND_HIGH = new Color( 0x99, 0x99, 0x00 );

		//private static Color BACKGROUND_HIGH = Color.yellow;

		public static final String STR_PREFIX_NORMAL = "<html><nobr><font color=\"#333333\">";
		public static final String STR_PREFIX_HIGH = "<html><nobr><b><font color=\"#000000\">";

		//public static final String STR_POSTFIX_NORMAL = "&nbsp;";
		//public static final String STR_POSTFIX_HIGH = "";
	}

	/** InterruptableAction with special handling of enabled state.

		@author keith cascio
		@since 20060327 */
	public abstract class StepAction extends InterruptableAction
	{
		abstract public boolean isEnabledHook();

		public StepAction( String name, String descrip, char mnemonic, Icon icon ){
			super( name, descrip, mnemonic, icon );

			Interruptable interruptable = getInterruptable();
			interruptable.setName( name );
			interruptable.setVeto( (Interruptable.Veto) CodeToolInternalFrame.this );
			if( CodeToolInternalFrame.this.hnInternalFrame != null ) interruptable.setParent( CodeToolInternalFrame.this.hnInternalFrame.getThreadGroup() );
		}

		public void setEnabled( boolean flag ){
			memorize();
			StepAction.this.enabled = flag;
			recall( false );
		}

		public boolean isEnabled(){
			boolean ret = super.isEnabled();
			if( !ret ) return false;

			return isEnabledHook();
		}

		public boolean memorize(){
			//System.out.println( super.getValue( Action.NAME ) + ".memorize() <- " + StepAction.this.isEnabled() );
			return StepAction.this.myMemory = StepAction.this.isEnabled();
		}

		public boolean recall( boolean force ){
			boolean oldValue = StepAction.this.myMemory;
			boolean newValue = StepAction.this.isEnabled();

			boolean diff = (oldValue != newValue);
			if( force || diff ){
				boolean before = diff ? oldValue : (!newValue);
				StepAction.this.firePropertyChange( "enabled", Boolean.valueOf(before), Boolean.valueOf(newValue) );
			}

			//System.out.println( super.getValue( Action.NAME ) + ".recall() <- " + diff );

			return diff;
		}

		private boolean myMemory = false;
	}

	/** interface Interruptable.Veto
		@since 20060328 */
	public boolean vetoInterruption( Interruptable interruptable ){
		String name    = interruptable.getName();
		String title   = "Job \"" + name + "\" already running";
		String message = "Really cancel job \"" + name + "\" and start over?";
		Component parentComponent = CodeToolInternalFrame.this;
		int result = JOptionPane.showConfirmDialog( parentComponent, message, title, JOptionPane.YES_NO_OPTION );
		return result != JOptionPane.YES_OPTION;
	}

	/** @since 040605 */
	private static JLabel makeCaption( String text ){
		JLabel ret = new JLabel( text, JLabel.LEFT );
		ret.setFont( ret.getFont().deriveFont( (float)16 ).deriveFont( Font.BOLD ) );
		return ret;
	}

	public void setVisible( boolean flag ){
		super.setVisible( flag );
		toFront();
	}

	/*
	public void reInitialize(){
		setCodeGenius( (CodeGenius)null );
	}*/

	/** @since 20060327 */
	public boolean isCompilable(){
		if( CodeToolInternalFrame.this.myCodeGenius == null ) return false;
		return CodeToolInternalFrame.this.myCodeGenius.isCompilable();
	}

	public void setCodeGenius( CodeGenius genius )
	{
		if( myCodeGenius == genius ) return;

		if( myCodeGenius != null ) myCodeGenius.removeChangeListener( this );

		myCodeGenius = genius;
		if( myCodeGenius == null ){
			myTADescription.setText( "empty" );
			myPanelOptions.removeAll();
		}
		else{
			myTADescription.setText( myCodeGenius.describe() );
			setResultIcon( getIcon( myCodeGenius ) );
			showOptions( myCodeGenius );
			myCodeGenius.addChangeListener( this );

			String initialPath = "";
			if( hnInternalFrame != null ){
				File directory = hnInternalFrame.getPackageOptions().getFile( STR_TOKEN_LAST_CODEBANDIT_LOCATION );
				if( directory != null && directory.exists() && directory.isDirectory() ) initialPath += directory.getPath() + File.separator;
			}
			initialPath += myCodeGenius.getOutputClassName() + ".java";
			myTFBrowse.setText( initialPath );
			setCloseText( STR_TEXT_CANCEL, STR_TOOLTIP_CANCEL );

			setGeniusButtonsEnabled( true );
			//action_VIEWCODE.setEnabled( false );
			//action_COMPILECODE.setEnabled( false );
			//action_RUNCODE.setEnabled( false );
			restart();

			//new Thread(){
			//	public void run(){
			//		try{ Thread.sleep( 500 ); }catch( InterruptedException e ){ System.err.println( e ); }
			//		System.out.println( "rows? " + myTADescription.getRows() );
			//		System.out.println( "pref? " + myTADescription.getPreferredSize() );
			//	}
			//}.start();
		}
	}

	/** @since 20060327 */
	private void restart(){
		if( mySteps == null ) return;
		mySteps.memorize();
		myFlagCodeCompiled = myFlagCodeWritten = false;
		mySteps.recall( true );
		mySteps.begin();
	}

	public void showOptions( CodeGenius genius )
	{
		myPanelOptions.removeAll();

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		myPanelOptions.add( Box.createHorizontalStrut( 4 ) );
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		myPanelOptions.add( myCodeOptionsPanel = new CodeOptionsPanel( genius ), c );

		myPanelOptions.revalidate();
		myPanelOptions.repaint();
	}

	/** @since 20040604 */
	public final StepAction action_WRITECODE;
	/** @since 20050209 */
	public final StepAction action_VIEWCODE;
	/** @since 20050401 */
	public final StepAction action_COMPILECODE, action_RUNCODE;

	/** @since 20061016 */
	private StepAction myActionWRITECODE, myActionVIEWCODE, myActionCOMPILECODE, myActionRUNCODE;
	/** @since 20061016 */
	private void initStepActions(){
		myActionWRITECODE = new StepAction( "Write Code", "Output Java code to the file you specify", 'w', MainToolBar.getIcon( STR_FILENAME_BUTTON_ICON ) ){
			public void runImpl( Object arg1 ){
				CodeToolInternalFrame.this.doWriteCode();
			}

			public boolean isEnabledHook(){
				return (!CodeToolInternalFrame.this.myFlagCodeWritten);
			}
		};

		myActionVIEWCODE = new StepAction( "View Java Code", "Invoke external program to view code", 'v', MainToolBar.getIcon( "Find16.gif" ) ){
			public void runImpl( Object arg1 ){
				CodeToolInternalFrame.this.doViewCode();
			}

			public boolean isEnabledHook(){
				return CodeToolInternalFrame.this.myFlagCodeWritten;
			}
		};

		myActionCOMPILECODE = new StepAction( "Compile Java Class", "<html><nobr>Invoke <b>javac</b> to compile (see console window for output)", 'c', MainToolBar.getIcon( "Hammer16.gif" ) ){
			public void runImpl( Object arg1 ){
				CodeToolInternalFrame.this.doCompileCode();
			}

			public boolean isEnabledHook(){
				boolean ret = CodeToolInternalFrame.this.myFlagCodeWritten;
				if( !ret ) return false;

				ret &= CodeToolInternalFrame.this.isCompilable();
				//if( !ret ) return false;

				return ret;
			}
		};

		myActionRUNCODE = new StepAction( "Run Java Class", "<html><nobr>Invoke <b>java</b> to run (see console window for output)", 'r', MainToolBar.getIcon( "Console16.gif" ) ){
			public void runImpl( Object arg1 ){
				CodeToolInternalFrame.this.doRunCode();
			}

			public boolean isEnabledHook(){
				return CodeToolInternalFrame.this.myFlagCodeCompiled;
			}
		};
	}

	public static final String STR_TEXT_CLOSE     = "Close";
	public static final String STR_TEXT_CANCEL    = "Cancel";

	public static final String STR_TOOLTIP_CLOSE  = "Exit "+STR_DISPLAY_NAME;
	public static final String STR_TOOLTIP_CANCEL = "Close "+STR_DISPLAY_NAME+" without writing code";

	/** @since 040705 */
	public void setCloseText( String text, String tooltip ){
		action_CLOSE.putValue( Action.NAME, text );
		action_CLOSE.putValue( Action.SHORT_DESCRIPTION, tooltip );
	}

	/** @since 060404 */
	public final AbstractAction action_CLOSE = new AbstractAction( STR_TEXT_CANCEL, MainToolBar.getBlankIcon() ){
		//{
		//	super.putValue( Action.NAME, STR_TEXT_CANCEL );
		//	super.putValue( Action.SHORT_DESCRIPTION, STR_TOOLTIP_CANCEL );
		//}

		public void actionPerformed( ActionEvent e ){
			CodeToolInternalFrame.this.setVisible( false );
		}
	};

	/** @since 060404 */
	public final SamiamAction action_SHOWDEPENDENCIES = new SamiamAction( "Describe Dependencies", "Display a tree structured list of the "+UI.STR_SAMIAM_ACRONYM+" features that influence output code", 'd', MainToolBar.getBlankIcon() ){
		public void actionPerformed( ActionEvent e ){
			CodeToolInternalFrame.this.doShowDependencies();
		}
	};

	/** @since 040405 */
	public final InterruptableAction action_COMPILESETTINGS = new InterruptableAction( "Compile Settings", "<html><nobr>Edit compile settings, including the locations of <b>inflib</b> and <b>javac</b>", 'p', (Icon)null ){
		public void runImpl( Object arg1 ){
			CodeToolInternalFrame.this.doCompileSettings();
		}
	};

	/** @since 040405 */
	public final InterruptableAction action_RUNSETTINGS = new InterruptableAction( "Run Settings", "<html><nobr>Edit run settings, including the locations of <b>inflib</b> and <b>java</b>", 'u', (Icon)null ){
		public void runImpl( Object arg1 ){
			CodeToolInternalFrame.this.doRunSettings();
		}
	};

	/** @since 032305 */
	public final InterruptableAction action_GENERATESCRIPTS = new InterruptableAction( "Create Scripts", "Write executable scripts suitable to compile/run Code Bandit's Java output.", 'c', MainToolBar.getIcon( "CursiveS16.gif" ) ){
		public void runImpl( Object arg1 ) throws InterruptedException {
			//File inflibclasspath = ScriptGenius.guessInflibClasspath();
			//System.out.println( "inflibclasspath : \"" +inflibclasspath.getAbsolutePath()+ "\"" );
			//System.out.println( "javaw executable: \"" +ScriptGenius.guessJavaExecutablePath( "javaw" )+ "\"" );
			try{
				File directory = null;
				if( hnInternalFrame != null ){
					File lastloc = hnInternalFrame.getPackageOptions().getFile( STR_TOKEN_LAST_CODEBANDIT_LOCATION );
					if( lastloc != null && lastloc.exists() && lastloc.isDirectory() ) directory = lastloc;
				}
				if( directory == null ) directory = new File( "." );
				Thread.sleep(4);//Interruptable.checkInterrupted();
				ScriptGeniusWizard wizard = getScriptGeniusWizard();
				wizard.setDefaultDirectory( directory );
				wizard.getWizardPanel().showDialog( CodeToolInternalFrame.this );
			}catch( Exception exception ){
				if( (exception instanceof InterruptedException) ) throw (InterruptedException)exception;
				else exception.printStackTrace();
			}
		}
	};

	/** @since 032405 */
	public ScriptGeniusWizard getScriptGeniusWizard(){
		if( myScriptGeniusWizard == null ){
			myScriptGeniusWizard = new ScriptGeniusWizard();
		}
		return myScriptGeniusWizard;
	}

	public void actionPerformed( ActionEvent e )
	{
		//action_WRITECODE.setEnabled( true );
		//action_VIEWCODE.setEnabled( false );
		//action_COMPILECODE.setEnabled( false );
		//action_RUNCODE.setEnabled( false );
		restart();

		Object src = e.getSource();
		if( src == myButtonBrowse ) browseFileOutput();
		else if( src == myTFBrowse ) myFileOutput = null;
	}

	/** interface ChangeListener
		@since    20081128 */
	public ChangeListener settingChanged( ChangeEvent event ){
		restart();
		return this;
	}

	private void setGeniusButtonsEnabled( boolean flag ){
		myButtonBrowse.setEnabled( flag );
		action_WRITECODE.setEnabled( flag );
		action_VIEWCODE.setEnabled( flag );
		action_COMPILECODE.setEnabled( flag );
		action_RUNCODE.setEnabled( flag );
		action_SHOWDEPENDENCIES.setEnabled( flag );
	}

	private void doShowDependencies(){
		//myCodeOptionsPanel.setHelpText( myCodeGenius.describeDependencies() );
		myCodeOptionsPanel.describeDependencies( myCodeGenius );
	}

	public static final long LONG_TOOL_TIMEOUT = (long)30000;

	/** @since 20050331 */
	public void processOutcome( ScriptExecution execution ){
		myScriptRunning = null;
		boolean isError = execution.isError();
		int type = isError ? JOptionPane.ERROR_MESSAGE : JOptionPane.PLAIN_MESSAGE;
		String message = execution.getMessage() + "\nSee console window for output.";
		String title = "Outcome of \"" + execution.getScript().getName() + "\"";
		JOptionPane.showMessageDialog( this, message, title, type );

		if( !isError ){
			ScriptGeniusWizard wizard = getScriptGeniusWizard();
			Script script = execution.getScript();
			if( script == wizard.getJDKCompile() ){
				//action_RUNCODE.setEnabled( true );
				mySteps.memorize();
				myFlagCodeWritten = myFlagCodeCompiled = true;
				mySteps.recall( false );
				mySteps.highlight( myStepRun );
			}
			else if( script == wizard.getJDKRun() ){
				mySteps.highlight( (Step)null );
			}
		}
	}

	/** @since 040405 */
	private void doCompileSettings(){
		try{
			ScriptGeniusWizard wizard = getScriptGeniusWizard();
			myScriptRunning = wizard.getJDKCompile();
			new Thread( myRunMonitorParanoid ).start();
			doResolve( myScriptRunning );
		}catch( Throwable throwable ){
			int type = JOptionPane.ERROR_MESSAGE;
			String title = "Failed to open compile settings";
			String msgException = throwable.getMessage();
			if( msgException == null ) msgException = throwable.toString();
			String message = title + ": " + msgException;
			JOptionPane.showMessageDialog( this, message, title, type );
		}
	}

	/** @since 040405 */
	private void doRunSettings(){
		try{
			ScriptGeniusWizard wizard = getScriptGeniusWizard();
			myScriptRunning = wizard.getJDKRun();
			new Thread( myRunMonitorParanoid ).start();
			doResolve( myScriptRunning );
		}catch( Throwable throwable ){
			int type = JOptionPane.ERROR_MESSAGE;
			String title = "Failed to open compile settings";
			String msgException = throwable.getMessage();
			if( msgException == null ) msgException = throwable.toString();
			String message = title + ": " + msgException;
			JOptionPane.showMessageDialog( this, message, title, type );
		}
	}

	/** @since 033105 */
	private void doCompileCode(){
		int type = JOptionPane.ERROR_MESSAGE;
		String title = "Compilation failed";

		if( (myFileOutput == null) || (!myFileOutput.exists()) ){
			JOptionPane.showMessageDialog( this, title + ": write code first", title, type );
			return;
		}

		String path = myFileOutput.getAbsolutePath();
		String[] args = new String[] { path };
		String messageConsole = "Compiling " + path + "...";

		ScriptGeniusWizard wizard = getScriptGeniusWizard();
		myScriptRunning = wizard.getJDKCompile();

		new Thread( myRunMonitorParanoid ).start();
		run( myScriptRunning, args, myFileOutput.getParentFile(), messageConsole );
	}

	/** @since 20091219 */
	private Runnable myRunMonitorParanoid = new Runnable(){
		public void run(){
			JDialog dialog = null;
			try{
				Thread.sleep( 0x80 );
				ParanoidFileFinder finder = JDKTool.getJavacTool().getParanoidFileFinder();
				if(     finder == null      ){ return; }
				Script  script  = myScriptRunning;
				if(     script == null      ){ return; }
				if(     script.isResolved() ){ return; }
				JLabel   label  = new JLabel( "path" );
				Object    ref   = new edu.ucla.belief.ui.clipboard.ClipboardHelper( label, null, label );
				dialog          = new JDialog( hnInternalFrame.getParentFrame(), false );
				dialog.setTitle( "searching your system for " + finder.getFileName() + " executable" );
				dialog.getContentPane().add( label );
				label.setPreferredSize( new Dimension( 0x200, label.getPreferredSize().height ) );
				dialog.pack();
				Util.centerWindow( dialog, Util.convertBoundsToScreen( CodeToolInternalFrame.this ) );
				if( myScriptRunning == null ){ return; }
				dialog.setVisible( true );
				File location = null;
				while( myScriptRunning != null ){
					Thread.sleep( 0x80 );
					label.setText( (location = finder.pole()) == null ? "????" : location.getAbsolutePath() );
				}
				Color         fore = null, back = Color.black;
				if( script.isResolved() ){ fore = Color.green; }
				else{                      fore = Color.red;   }
				label.setForeground( fore );
				label.setBackground( back );
				dialog.getContentPane().setBackground( back );
				Thread.sleep( 0x800 );
			}catch( Throwable thrown ){
				System.err.println( "warning: CodeToolInternalFrame.myRunMonitorParanoid.run() caught " + thrown );
			}finally{
				if( dialog != null ){ dialog.dispose(); }
			}
		}
	};

	/** @since 040105 */
	private void doRunCode(){
		int type = JOptionPane.ERROR_MESSAGE;
		String title = "Execution failed";

		if( (myFileOutput == null) || (!myFileOutput.exists()) ){
			JOptionPane.showMessageDialog( this, title + ": write and compile code first", title, type );
			return;
		}

		JDKRun.CompiledClassInfo info = new JDKRun.CompiledClassInfo( myFileOutput );

		if( info.error != null ){
			JOptionPane.showMessageDialog( this, info.error, title, type );
			return;
		}

		String[] args = new String[] { info.classname };
		String messageConsole = "Executing " + info.classname + "...";

		ScriptGeniusWizard wizard = getScriptGeniusWizard();
		myScriptRunning = wizard.getJDKRun();

		new Thread( myRunMonitorParanoid ).start();
		run( myScriptRunning, args, info.directory, messageConsole );
	}

	/** @since 20060328 */
	private boolean isResolved( Script script, SystemSoftwareSource source ){
		boolean flagResolved = false;
		String msg = STR_MSG_RESOLVING_PREFIX + script.getName() + STR_MSG_RESOLVING_POSTFIX;
		try{
			Util.pushStatusWest( hnInternalFrame, msg );
			flagResolved = script.isResolved( source );
		}catch( Throwable throwable ){
			flagResolved = false;
		}finally{
			Util.popStatusWest( hnInternalFrame, msg );
		}
		return flagResolved;
	}

	/** @since 20050401 */
	private void run( Script script, String[] args, File dir, String messageConsole ){
		String name = "";
		try{
			name = script.getName();
			ScriptGeniusWizard wizard = getScriptGeniusWizard();
			SystemSoftwareSource source = wizard.getSystemSoftwareSource();

			if( !isResolved( script, source ) ) doResolve( script );

			if( !isResolved( script, source ) ){
				System.err.println( "no resolution" );
				return;
			}

			ScriptGenius genius = wizard.getRecommendedLanguage();
			ScriptExecution scriptexecution = script.exec( args, dir, genius, source );

			if( hnInternalFrame != null ){
				hnInternalFrame.console.println();
				hnInternalFrame.console.println( messageConsole );
				hnInternalFrame.console.println( scriptexecution.getCommand() );
				scriptexecution.pipe( hnInternalFrame.console );
			}

			scriptexecution.sit( (ScriptExecution.ProcessCustodian)this, LONG_TOOL_TIMEOUT );
		}catch( Exception exception ){
			//System.err.println( "Warning: " + exception );
			int type = JOptionPane.ERROR_MESSAGE;
			String title = "\"" + name + "\" failed";
			String msgException = exception.getMessage();
			if( msgException == null ) msgException = exception.toString();
			String message = title + ": " + msgException;
			JOptionPane.showMessageDialog( this, message, title, type );
		}
	}

	/** @since 033105 */
	private void doResolve( Script script ) throws Exception {
		if( myResolutionPanel == null ){ myResolutionPanel = new ResolutionPanel(); }
		myResolutionPanel.setDependencies( script );
		myResolutionPanel.fill();
		myScriptRunning          = null;

		String title = "Resolve System Software Dependencies for Script \"" + script.getName() + "\"";

		new JOptionResizeHelper( myResolutionPanel, true, (long)10000 ).start();
		JOptionPane.showMessageDialog( this, myResolutionPanel, title, JOptionPane.PLAIN_MESSAGE );

		try{
			myResolutionPanel.validateDependencies();
		}catch( Exception exception ){
			System.err.println( "Warning: " + exception );
		}finally{
			myScriptRunning          = null;
		}
	}

	private ResolutionPanel myResolutionPanel;

	/** @since 020905 */
	private void doViewCode(){
		try{
			//String cmd = "start \"codebandit\" \"" + myFileOutput.getAbsolutePath() + "\"";
			File app = getFileLocationBrowser().getFile();
			if( app == null ) return;
			if( !validateOutputFile( false ) ) return;
			String codepath = myFileOutput.getAbsolutePath();
			Runtime.getRuntime().exec( "\"" + app.getAbsolutePath() + "\" \"" + codepath + "\"" );
			mySteps.highlight( myStepCompile );
		}catch( Exception exception ){
			System.err.println( "Warning: " + exception );
		}catch( Error error ){
			System.err.println( "Warning: " + error );
		}
	}

	/** @since 032405 */
	public void forgetToolLocations(){
		if( myFileLocationBrowser != null ) myFileLocationBrowser.setFile( (File)null );
	}

	/** @since 020905 */
	private FileLocationBrowser getFileLocationBrowser(){
		if( myFileLocationBrowser == null ){
			myFileLocationBrowser = new FileLocationBrowser( (FileLocationBrowser.FileInformationSource)this );
		}
		return myFileLocationBrowser;
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public FileFilter getFilter( FileLocationBrowser.Platform platform ){
		if( platform == FileLocationBrowser.Platform.WINDOWS ) return new InflibFileFilter( new String[] { ".exe" }, "Windows Executable (*.exe)" );
		else return null;
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public String getDescription( FileLocationBrowser.Platform platform ){
		if( platform == FileLocationBrowser.Platform.WINDOWS ) return "Viewer Application (Windows Executable *.exe)";
		else return "Viewer Program Executable";
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public JComponent getAccessory( FileLocationBrowser.Platform platform ){
		return (JComponent)null;
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public SamiamPreferences getPreferences(){
		UI ui = getParentFrame();
		if( ui != null ) return ui.getSamiamPreferences();
		else return new SamiamPreferences( true );
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public String getPreferenceFileToken(){
		return STR_TOKEN_VIEWER_APPLICATION;
	}

	/** interface FileLocationBrowser.FileInformationSource */
	public Component getDialogParent(){
		return getParentFrame();
	}

	/** @since 020905 */
	public UI getParentFrame(){
		if( hnInternalFrame != null ) return hnInternalFrame.getParentFrame();
		else return (UI) null;
	}

	private void doWriteCode()
	{
		if( !validateOutputFile( true ) ) return;

		PrintStream out = null;
		try{
			out = new PrintStream( new FileOutputStream( myFileOutput ) );
		}catch( Exception e ){
			e.printStackTrace();
			return;
		}

		Object warnings = myCodeGenius.getWarnings();
		if( warnings != null ){
			warnings = warnings.toString() + STR_WARNING_QUESTION;
			int result = JOptionPane.showConfirmDialog( this, warnings, STR_WARNING_TITLE, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );
			if( result != JOptionPane.OK_OPTION ) return;
		}

		try{
			myMessageStatusBar = STR_MSG_CODE_PREFIX + myCodeGenius.getShortDescription() + STR_MSG_CODE_POSTFIX;
			Util.pushStatusWest( hnInternalFrame, myMessageStatusBar );
			myCodeGenius.writeCode( out );
			out.close();
			setCloseText( STR_TEXT_CLOSE, STR_TOOLTIP_CLOSE );
			//action_WRITECODE.setEnabled( false );
			//action_VIEWCODE.setEnabled( true );
			//action_COMPILECODE.setEnabled( true );
			//action_RUNCODE.setEnabled( false );
			mySteps.memorize();
			myFlagCodeWritten  = true;
			myFlagCodeCompiled = false;
			mySteps.recall( false );

			hnInternalFrame.getPackageOptions().putProperty( STR_TOKEN_LAST_CODEBANDIT_LOCATION, myFileOutput.getParentFile() );
			mySteps.highlight( myStepView );
		}catch( Exception e ){
			//Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
			//e.printStackTrace();
			int type = JOptionPane.ERROR_MESSAGE;
			String title = "Problem writing code";
			String msgException = e.getMessage();
			if( msgException == null ) msgException = e.toString();
			String message = title + ": " + msgException;
			JOptionPane.showMessageDialog( this, message, title, type );
		}finally{
			Util.popStatusWest( hnInternalFrame, myMessageStatusBar );
		}
	}

	private boolean validateOutputFile( boolean toWrite )
	{
		//System.out.println( "CodeToolInternalFrame.validateOutputFile()" );
		if( myFileOutput == null ){
			String path = myTFBrowse.getText();
			if( path.length() > 1 ) myFileOutput = new File( path );
		}

		if( myFileOutput == null ){//|| (!myFileOutput.isFile()) ){
			//System.out.println( myFileOutput );
			String message = "Please select an output file.";
			String title = "No output file selected";
			JOptionPane.showMessageDialog( this, message, title, JOptionPane.ERROR_MESSAGE );
			return false;
		}

		try{
			if( myCodeGenius != null ){
				String basename  = myFileOutput.getName();
				String candidate = basename.substring( 0, basename.lastIndexOf( "." ) ).replaceAll( "\\W+", "" );
				if( candidate.length() > 0 ){ myCodeGenius.setOutputClassName( candidate ); }
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: CTIF.validateOutputFile() caught " + thrown );
		}

		if( toWrite && myFileOutput.exists() ){
			String message = "Overwrite?";
			String title = "File exists";
			int result = JOptionPane.showConfirmDialog( this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
			if( result != JOptionPane.YES_OPTION ) return false;
		}

		return true;
	}

	private void browseFileOutput()
	{
		setAllButtonsEnabled( false );

		File DEFAULT_DIR = ( hnInternalFrame == null ) ? new File( "." ) : hnInternalFrame.getPackageOptions().defaultDirectory;//Keith Cascio 052002

		JFileChooser fileChooser = new JFileChooser( DEFAULT_DIR );
		//fileChooser.addChoosableFileFilter( theFileFilter );
		fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		fileChooser.setMultiSelectionEnabled(false);
		//fileChooser.setAcceptAllFileFilterUsed(false);

		int result = fileChooser.showSaveDialog( this );
		if( result == JFileChooser.APPROVE_OPTION )
		{
			myFileOutput = fileChooser.getSelectedFile();
			myTFBrowse.setText( myFileOutput.getAbsolutePath() );
		}

		setAllButtonsEnabled( true );
	}

	public void setAllButtonsEnabled( boolean flag ){
		action_WRITECODE.setEnabled( flag );
		action_VIEWCODE.setEnabled( flag );
		action_COMPILECODE.setEnabled( flag );
		action_RUNCODE.setEnabled( flag );
		myButtonBrowse.setEnabled( flag );
		action_CLOSE.setEnabled( flag );
		action_SHOWDEPENDENCIES.setEnabled( flag );
	}

	/**
		Test/debug
	*/
	public static void main( String[] args )
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		final String finalpath = "c:\\keithcascio\\networks\\tutorial.net";
		//final String finalpath = "c:\\keith\\programming\\argroup\\inflib\\network_samples\\cancer.net";
		String path = finalpath;
		if( args.length > 0 ) path = args[0];

		BeliefNetwork bn = null;

		try{
			bn = NetworkIO.read( path );
		}catch( Exception e ){
			System.err.println( "Error: Failed to read " + path );
			return;
		}

		final BeliefNetwork finalbn = bn;

		//final il2.bridge.Converter converter = new il2.bridge.Converter();
		//final il2.model.BayesianNetwork il2network = converter.convert( bn );

		final CodeToolInternalFrame ctif = new CodeToolInternalFrame( (NetworkInternalFrame)null );
		ctif.addInternalFrameListener( new InternalFrameAdapter()
		{
			public void internalFrameClosing(InternalFrameEvent e)
			{
				Util.STREAM_TEST.println( "CodeToolInternalFrame size: " + ctif.getSize() );
			}
		} );

		JButton btnCommit = new JButton( "ModelCoder" );
		btnCommit.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				if( ctif.isVisible() )
				{
					ModelCoder mc = new ModelCoder( finalbn, finalpath );
					//mc.setConverter( converter );
					ctif.setCodeGenius( mc );
					ctif.myTFBrowse.setText( "c:\\keithcascio\\dev\\inflib\\ModelTutorial.java" );
					//ctif.myTFBrowse.setText( "c:\\keith\\programming\\argroup\\inflib\\ModelTutorial.java" );
				}
				else ctif.setVisible( true );
			}
		} );
		JPanel pnlButtons = new JPanel();
		pnlButtons.add( btnCommit );

		JDesktopPane pain = new JDesktopPane();
		pain.add( ctif );


		ctif.setBounds( new Rectangle( new Point(10,10), ctif.calculatePreferredSize( new Dimension() ) ) );
		//ctif.reInitialize();
		ctif.setVisible( true );

		JFrame frame = new JFrame( "DEBUG CodeToolInternalFrame" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		Container contentPain = frame.getContentPane();
		contentPain.setLayout( new BorderLayout() );
		contentPain.add( pain, BorderLayout.CENTER );
		contentPain.add( pnlButtons, BorderLayout.EAST );
		contentPain.add( new JLabel( "network: " + path ), BorderLayout.SOUTH );
		//frame.pack();
		frame.setSize( 800,700 );
		Util.centerWindow( frame );
		frame.setVisible( true );

		try{ ctif.setSelected( true ); }
		catch( java.beans.PropertyVetoException e ) { e.printStackTrace(); }
	}

	protected NetworkInternalFrame hnInternalFrame;
	//private Icon myIconFrame;
	private JTextArea myTADescription;
	private JTextField myTFBrowse;
	private JButton myButtonBrowse;
	private JPanel myPanelSteps, myPanelButtons, myPanelNorth;
	private JLabel myLabelDescriptionCaption, myLabelDescriptionIcon;
	private JComponent myPanelOptions;
	private CodeOptionsPanel myCodeOptionsPanel;
	private CodeGenius myCodeGenius;
	private File myFileOutput;
	private String myMessageStatusBar;
	private FileLocationBrowser myFileLocationBrowser;
	private ScriptGeniusWizard myScriptGeniusWizard;
	private Script             myScriptRunning;

	private boolean myFlagCodeWritten  = false;
	private boolean myFlagCodeCompiled = false;
}
