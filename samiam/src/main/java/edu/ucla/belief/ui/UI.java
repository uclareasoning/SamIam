package edu.ucla.belief.ui;

import edu.ucla.belief.ui.dialogs.*;
import edu.ucla.belief.ui.dialogs.Pollster;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.toolbar.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.clipboard.*;
import edu.ucla.belief.ui.statusbar.*;
import edu.ucla.belief.ui.actionsandmodes.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.internalframes.*;
import edu.ucla.belief.ui.event.DynamatorListener;
import edu.ucla.belief.ui.rc.DisplayableRCEngineGenerator;
import edu.ucla.belief.ui.recursiveconditioning.RCProgressMonitor;
import edu.ucla.belief.ui.recursiveconditioning.PMCloser;
import edu.ucla.belief.ui.recursiveconditioning.CachePreviewDialog;
import edu.ucla.belief.ui.primula.*;
import edu.ucla.belief.ui.internalframes.CodeToolInternalFrame;
import edu.ucla.belief.ui.animation.Animator;
import edu.ucla.belief.ui.tabledisplay.CPTEditor;
import edu.ucla.belief.ui.internalframes.Bridge2Tiger.Troll;

//Keith Cascio 031902 for DEBUG
import edu.ucla.belief.*;
import edu.ucla.belief.inference.*;
import edu.ucla.structure.*;
import edu.ucla.belief.ui.networkdisplay.*;
//import il2.inf.rc.*;
import il2.inf.structure.DGraph;
import edu.ucla.belief.ui.animation.*;
import edu.ucla.belief.decision.Optimizer;
//import edu.ucla.util.JVMTI;
//Keith Cascio 031902 for DEBUG

import edu.ucla.util.ProgressMonitorable;
import edu.ucla.util.CompoundTask;
import edu.ucla.util.WeakLinkedList;
import edu.ucla.util.Interruptable;
import edu.ucla.belief.approx.PropagationInferenceEngine;
import edu.ucla.belief.dtree.*;
import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.io.NodeLinearTask;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;

import java.util.List;
import javax.swing.filechooser.FileFilter;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.lang.reflect.Field;
import javax.swing.border.BevelBorder;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;
import java.net.URL;
import java.beans.*;

//More look and feel

//import com.l2fprod.gui.plaf.skin.Skin;
//import com.l2fprod.gui.plaf.skin.SkinLookAndFeel;
//import com.incors.plaf.kunststoff.KunststoffLookAndFeel;

/** User interface. */
public class UI extends JFrame implements SamiamUIInt, ActionListener, WindowListener, MenuListener, /*NetworkIO.BeliefNetworkIOListener,*/ NetworkDisplay.UserInputListener, edu.ucla.belief.recursiveconditioning.RC.RecCondThreadListener, SamiamUserModal
{
	private   LinkedList                         myListInternalFrames;
	private   SamiamPreferences                  mySamiamPreferences;
	private   JDesktopPane                       myJDesktopPane;
	private   JPanel                             myCenterPain;
	private   MainToolBar                        myToolBar;
	private   InstantiationClipboardToolBar      myInstantiationClipboardToolBar;
	private   NetworkClipBoard                   myNetworkClipBoard       = new NetworkClipBoardImpl();
	private   InstantiationClipBoard             myInstantiationClipBoard = new InstantiationClipBoardImpl( this );
	private   StatusBar                          myStatusBar;
	private   Collection                         myListDynamators;
	private   Dynamator                          myDynamator, myEDBPDynamator;
	private   boolean                            myFlagSystemExitEnabled = true;
	private   String                             myStreamInputName;
	private   PrimulaManager                     myPrimulaManager;
	private   DotLayoutManager                   myDotLayoutManager;
	private   String                             myInvokerName;
	private   Collection                         myPreferenceMediators;
	private   ThreadGroup                        myThreadGroup;
	protected int                                layer = 0;

	protected static String DEFAULT_PATH = ".";
	public    static boolean
	  FLAG_GENIE_STYLE_CPT_EDIT          =  true,
	  FLAG_ENABLE_PRIMULA                = false,
	  FLAG_ENABLE_EDBP_MANUAL            = false,
	  FLAG_FORCE_WELCOME                 = false;
	public    static String
	  LAUNCH_COMMAND, LAUNCH_SCRIPT;
	public    static UI     STATIC_REFERENCE;
	public    static final long LONG_SPLASH_MILLIS = (long)4000;
	private   static Pattern
	  PATTERN_DEBUG, PATTERN_NOSPLASH;

	public    static final String
	  PATH_HTML_HELP_INDEX               = "htmlhelp" + File.separator + "index.html",
	  URL_HTML_HELP_INDEX                = "file:" + PATH_HTML_HELP_INDEX,
	  PATH_HTML_HELP_CODETOOL            = "htmlhelp" + File.separator + "codebandit.html",
	  URL_HELP_LIVE                      = "http://reasoning.cs.ucla.edu/samiam/help/",
	  URL_ARGROUP                        = "http://reasoning.cs.ucla.edu/",
	  URL_SAMIAM                         = "http://reasoning.cs.ucla.edu/samiam/",
	  URL_TUTORIALS                      = "http://reasoning.cs.ucla.edu/samiam/videos.html",
	  STR_ARG_DEBUG                      = "debug",
	  STR_REGEX_DEBUG                    = "-?debug",
	  STR_ARG_VERBOSE                    = "verbose",
	  STR_ARG_NOSPLASH                   = "nosplash",
	  STR_REGEX_NOSPLASH                 = "-?nosplash",
	  STR_ARG_FORCEWELCOME               = "welcome",
	  STR_ARG_DIMENSION                  = "-dim",
	  STR_ARG_DEV_IMAGE_PATH             = "-images",
	  STR_ARG_PREF_PATH                  = "-p",
	  STR_ARG_EVIDENCE_PATH              = "-e",
	  STR_ARG_SET                        = "-set",
	  STR_ARG_ACT                        = "-act",
	  STR_ARG_LAUNCH_COMMAND             = "-launchcommand",
	  STR_ARG_LAUNCH_SCRIPT              = "-launchscript",
	  STR_ARG_ENABLE_DECISION_TREE       = "decisiontree",
	  STR_ARG_ENABLE_ANIMATION           = "visual",
	  STR_ARG_ENABLE_PRIMULA             = "primula",
	  STR_ARG_ENABLE_CPT_IMPORT          = "cptimport",
	  STR_ARG_ENABLE_EDBP_MANUAL         = "edbpmanual",
	  STR_KEY_VISIBLE_STATUSBAR          = "visiblestatusbar",
	  STR_KEY_VISIBLE_TOOLBAR            = "visibletoolbar",
	  STR_KEY_VISIBLE_ICBTOOLBAR         = "visibleinstantiationclipboardtoolbar",
	  STR_KEY_ICBTCONSTRAINTS            = "constraintsinstantiationclipboardtoolbar",
	  STR_KEY_ICBTORIENTATION            = "orientationinstantiationclipboardtoolbar",
	  STR_SAMIAM_ACRONYM                 = "SamIam";

	public static void main(String[] args)
	{
		mainImpl( args );
	}

	public static UI mainImpl(String[] args)
	{
		if( !isJavaVersionOK() ) System.exit(1);

		System.setProperty( "sun.java2d.d3d", "false" );

		File      fileToOpen                      = null;
		File      filePreferences                 = null, fileEvidence = null;
		boolean   doSplash                        = true;
		Dimension dimension                       = null;
		java.io.PrintStream argumentMessageStream = System.out;
		List      actions                         = null;

		for( int i=0; i < args.length; i++ )
		{
			if( args[i].equals( STR_ARG_VERBOSE ) )
			{
				Util.DEBUG_VERBOSE = true;
				edu.ucla.belief.Definitions.DEBUG = true;
				doSplash = false;
				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " running in verbose mode." );
			}
			else if( args[i].equals( STR_ARG_FORCEWELCOME ) )
			{
				UI.FLAG_FORCE_WELCOME = true;
				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " forcing welcome screen." );
			}
			else if( args[i].equals( STR_ARG_ENABLE_DECISION_TREE ) )
			{
				DSLNodeType.FLAG_ENABLE_DECISIONTREE = true;
				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " enabling decision trees." );
			}
			else if( args[i].equals( STR_ARG_ENABLE_ANIMATION ) )
			{
				Animator.FLAG_ENABLE_ANIMATION = true;
				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " enabling visualization." );
			}
			else if( args[i].equals( STR_ARG_ENABLE_PRIMULA ) )
			{
				FLAG_ENABLE_PRIMULA = true;
				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " enabling primula." );
			}
			else if( args[i].equals( STR_ARG_ENABLE_CPT_IMPORT ) )
			{
				CPTEditor.FLAG_ENABLE_CPT_IMPORT = true;
				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " enabling cpt import." );
			}
			else if( args[i].equals( STR_ARG_ENABLE_EDBP_MANUAL ) ){
				FLAG_ENABLE_EDBP_MANUAL = true;
				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " enabling manual edbp." );
			}
			else if( ((PATTERN_DEBUG == null) ? PATTERN_DEBUG = Pattern.compile(STR_REGEX_DEBUG) : PATTERN_DEBUG).matcher( args[i] ).matches() )
			{
				Util.DEBUG = true;
				Dynamator.FLAG_DEBUG_DISPLAY_NAMES = true;
				doSplash = false;
				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " running in debug mode." );
			}
			else if( ((PATTERN_NOSPLASH == null) ? PATTERN_NOSPLASH = Pattern.compile(STR_REGEX_NOSPLASH) : PATTERN_NOSPLASH).matcher( args[i] ).matches() )
			{
				doSplash = false;
				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " skipping splash screen." );
			}
			else if( args[i].startsWith( STR_ARG_LAUNCH_SCRIPT ) )
			{
				String launchScript = args[i].substring( STR_ARG_LAUNCH_SCRIPT.length() );
				if( launchScript.length() < 1 ){
					launchScript = args[++i];
				}

				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " launched by \"" + launchScript + "\"" );
				LAUNCH_SCRIPT = launchScript;
			}
			else if( args[i].startsWith( STR_ARG_LAUNCH_COMMAND ) )
			{
				String launchCommand = args[i].substring( STR_ARG_LAUNCH_COMMAND.length() );
				if( launchCommand.length() < 1 ){
					launchCommand = args[++i];
				}

				argumentMessageStream.println( STR_SAMIAM_ACRONYM + " launch command \"" + launchCommand + "\"" );
				LAUNCH_COMMAND = launchCommand;
			}
			else if( args[i].startsWith( STR_ARG_DEV_IMAGE_PATH ) )
			{
				String potentialDevImagePath = args[i].substring( STR_ARG_DEV_IMAGE_PATH.length() );
				if( potentialDevImagePath.length() > 0 )
				{
					argumentMessageStream.println( STR_SAMIAM_ACRONYM + " using dev image path " + potentialDevImagePath );
					MainToolBar.DEV_IMAGE_PATH = potentialDevImagePath;
				}
			}
			else if( args[i].startsWith( STR_ARG_PREF_PATH ) )
			{
				String potentialPrefPath = args[i].substring( STR_ARG_PREF_PATH.length() );
				if( potentialPrefPath.length() > 0 )
				{
					argumentMessageStream.println( STR_SAMIAM_ACRONYM + " using preference file " + potentialPrefPath );
					filePreferences = new File( potentialPrefPath );
				}
			}
			else if( args[i].startsWith( STR_ARG_SET ) ){
				try{
					String strSet = args[i].substring( STR_ARG_SET.length() );
					if(       (strSet == null ) || (strSet.length() < 1) ){ strSet = args[++i]; }
					Util.set( strSet );
				}catch( Throwable thrown ){
					System.err.println( "warning: error processing command line argument " + STR_ARG_SET + ": " + thrown );
				}
			}
			else if( args[i].startsWith( STR_ARG_EVIDENCE_PATH ) ){
				String pathE = args[i].substring( STR_ARG_EVIDENCE_PATH.length() );
				if( pathE.length() > 0 ){
					argumentMessageStream.println( STR_SAMIAM_ACRONYM + " using evidence file " + pathE );
					fileEvidence = new File( pathE );
				}
			}
			else if( args[i].startsWith( STR_ARG_ACT ) ){
				try{
					String strAct = args[i].substring( STR_ARG_ACT.length() );
					if(       (strAct == null ) || (strAct.length() < 1) ){ strAct = args[++i]; }
					if( actions == null ){ actions = new LinkedList(); }
					actions.add( strAct );
				}catch( Throwable thrown ){
					System.err.println( "warning: error processing command line argument " + STR_ARG_ACT + ": " + thrown );
				}
			}
			else if( args[i].startsWith( STR_ARG_DIMENSION ) )
			{
				String strArgDimension = args[i].substring( STR_ARG_DIMENSION.length() );
				StringTokenizer toker = new StringTokenizer( strArgDimension, "xX" );
				String strWidth = null;
				String strHeight = null;
				if( toker.hasMoreTokens() ) strWidth = toker.nextToken();
				if( toker.hasMoreTokens() ) strHeight = toker.nextToken();
				if( strWidth != null && strHeight != null )
				{
					dimension = new Dimension( Integer.parseInt(strWidth), Integer.parseInt(strHeight) );
					argumentMessageStream.println( STR_SAMIAM_ACRONYM + " user-specified window dimension " + dimension );
				}
			}
			else{
				fileToOpen = new File( args[i] );
				if( !fileToOpen.exists() ) fileToOpen = null;
			}
		}

		Thread tSplash = null;

		if( doSplash )
		{
			tSplash = new SplashThread( LONG_SPLASH_MILLIS ).newThread();
			tSplash.start();
		}

		UI ui = new UI( filePreferences );
		if( dimension != null ){ ui.setSize( dimension ); }
		Util.centerWindow( ui );
		STATIC_REFERENCE = ui;

		if( doSplash )
		{
			try{
				tSplash.join( LONG_SPLASH_MILLIS );
			}
			catch( InterruptedException e ){
				Thread.currentThread().interrupt();
				System.err.println( "Warning: edu.ucla.belief.ui.UI.mainImpl() interrupted." );
				ui.dispose();
				return ui = (UI)null;
			}
		}

		ui.setVisible( true );
		if( fileToOpen != null ){
			ui.openFile( fileToOpen );
			try{
				for( int i=0; i<0x80 && (ui.getActiveHuginNetInternalFrame() == null); i++ ){ Thread.sleep( 0x40 ); }
			}catch( Throwable thrown ){
				System.err.println( "warning: UI.mainImpl() caught  " + thrown );
			}
		}

		if( fileEvidence != null ){
			try{
				ui.myInstantiationClipBoard.load( fileEvidence );
			}catch( Throwable thrown ){
				System.err.println( "warning: UI.mainImpl() caught  " + thrown );
			}
		}

		if( actions != null ){
			try{
				String        strAct;
				Field         field;
				SamiamAction  action;
				for( Iterator itActs = actions.iterator(); itActs.hasNext(); ){
					strAct = itActs.next().toString();
					field  = UI.class.getField( strAct );
					action = (SamiamAction) field.get( ui );
					for( int i=0; (! action.isEnabled()) && i<8; i++ ){ Thread.sleep( 0x100 ); }//busy wait, spin
					action.actionP( strAct );
					Thread.sleep( 0x40 );
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: caught processing actions: " + thrown );
				thrown.printStackTrace();
			}
		}

		return ui;
	}

	/** @since 102202 */
	public UI()
	{
		this( (File)null );
	}

	/** @since 021104 */
	public UI( File preferenceFile )
	{
		super( STR_SAMIAM_ACRONYM + ": Sensitivity Analysis, Modeling, Inference and More" );

		initStatic();

		action_PRINTSELECTEDNODES = new HandledModalAction( "Print Selected", "Print the list of selected nodes to the console", 'p', MainToolBar.getIcon( "DisplayNodesSelected16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				if( nif != null ) nif.console.println( "Selected variables: " + nif.getNetworkDisplay().getSelectedVariables().toString() );
			}
		};
		action_INFORMATION = new HandledModalAction( "Network Information", "Show network statistics", 'i', MainToolBar.getIcon( "Information16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				myNetworkInformation.showDialog( nif.getBeliefNetwork(), nif.getInferenceEngine(), nif.getFileNameSansPath(), UI.this );
			}
		};
		action_SELECTEDNODESRIGHT = new HandledModalAction( "Move Right", "Move selected nodes right", 'r', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				nif.getNetworkDisplay().moveSelectedNodes( SwingConstants.EAST );
			}
		};
		action_SELECTEDNODESLEFT = new HandledModalAction( "Move Left", "Move selected nodes left", 'r', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				nif.getNetworkDisplay().moveSelectedNodes( SwingConstants.WEST );
			}
		};
		action_SELECTEDNODESUP = new HandledModalAction( "Move Up", "Move selected nodes up", 'r', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				nif.getNetworkDisplay().moveSelectedNodes( SwingConstants.NORTH );
			}
		};
		action_SELECTEDNODESDOWN = new HandledModalAction( "Move Down", "Move selected nodes down", 'r', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				nif.getNetworkDisplay().moveSelectedNodes( SwingConstants.SOUTH );
			}
		};
		action_ZOOMIN = new HandledModalAction( "Zoom In    (Ctrl +)", "Zoom In", 'i', MainToolBar.getIcon( "ZoomIn16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				nif.getNetworkDisplay().zoomIn();
			}
		};
		action_ZOOMOUT = new HandledModalAction( "Zoom Out (Ctrl -)", "Zoom Out", 'o', MainToolBar.getIcon( "ZoomOut16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				nif.getNetworkDisplay().zoomOut();
			}
		};
		action_ACTUALPIXELS = new HandledModalAction( "Actual Pixels (1/1)", "Zoom to normal size", 'a', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				nif.getNetworkDisplay().setZoomFactor( (double)1 );
			}
		};
		action_FITONSCREEN = new HandledModalAction( "Fit on Screen", "Try to fit the entire network on screen", 'f', MainToolBar.getIcon( "AlignCenter16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				nif.getNetworkDisplay().fitOnScreen();
			}
		};
		action_SHOWSTATUSBAR = new SamiamAction( "Status bar", "Show the status bar", 's', MainToolBar.getBlankIcon() )
		{
			public void actionPerformed( ActionEvent e ){
				setVisibleStatusBar( !myStatusBar.isVisible() );
			}
		};
		action_SHOWTOOLBAR = new SamiamAction( "Tool bar", "Show the tool bar", 't', MainToolBar.getBlankIcon() )
		{
			public void actionPerformed( ActionEvent e ){
				setVisibleToolbarMain( !myToolBar.isVisible() );
			}
		};
		action_SHOWINSTANTIATIONTOOLBAR = new SamiamAction( "Inst. clipboard tool bar", "Show the instantiation clipboard tool bar", 'i', MainToolBar.getBlankIcon() )
		{
			public void actionPerformed( ActionEvent e ){
				setVisibleToolbarInstantiation( !myInstantiationClipboardToolBar.isVisible() );
			}
		};
		action_REFRESH = new HandledModalAction( "Refresh", "Refresh the network display window", 'r', MainToolBar.getIcon( "Properties16.gif" ), ModeHandler.OPENBUTNOTCOMPILING )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				nif.getNetworkDisplay().refresh();
			}
		};
		action_COPYEVIDENCE = new HandledModalAction( "Copy Evidence", "Copy evidence", 'c', MainToolBar.getIcon( "CopyAlt16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				myInstantiationClipBoard.copy( nif.getBeliefNetwork().getEvidenceController().evidence() );
				action_PASTEEVIDENCE.setSamiamUserMode( UI.this.getSamiamUserMode() );
			}
		};
		action_CUTEVIDENCE = new HandledModalAction( "Cut Evidence", "Cut evidence", 'u', MainToolBar.getIcon( "CutAlt16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				EvidenceController controller = nif.getBeliefNetwork().getEvidenceController();
				myInstantiationClipBoard.cut( controller.evidence(), controller );
				action_PASTEEVIDENCE.setSamiamUserMode( UI.this.getSamiamUserMode() );
			}
		};
		action_PASTEEVIDENCE = new PasteAction( "Paste Evidence", "Paste evidence", 'p', MainToolBar.getIcon( "PasteAlt16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public int sizeClipboard(){
				return (((UI.this==null) || (UI.this.myInstantiationClipBoard==null)) ? (int)0 : UI.this.myInstantiationClipBoard.size());
			}

			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				if( nif != null ) myInstantiationClipBoard.paste( nif.getBeliefNetwork() );
			}
		};
		action_VIEWINSTANTIATIONCLIPBOARD = new SamiamAction( "View", "View instantiation clipboard", 'v', MainToolBar.getIcon( "HistoryAlt16.gif" ) )
		{
			public void actionPerformed( ActionEvent e )
			{
				JComponent comp = myInstantiationClipBoard.view();
				showMessageDialog( comp, "Instantiation Clipboard" );
			}
		};
		action_LOADINSTANTIATIONCLIPBOARD = new SamiamAction( "Load", "Load instantiation file to clipboard", 'l', MainToolBar.getIcon( "OpenAlt16.gif" ) )
		{
			public void actionPerformed( ActionEvent evt )
			{
				boolean success;
				String errormsg = "Failed to load instantiation from file.";
				try{
					success = myInstantiationClipBoard.load();
				}catch( Exception e ){
					success = false;
					errormsg += "\n" + e.getMessage();
				}

				if( !success ) showErrorDialog( errormsg );
			}
		};
		action_SAVEINSTANTIATIONCLIPBOARD = new SamiamAction( "Save", "Save the instantiation clipboard to file", 's', MainToolBar.getIcon( "SaveAlt16.gif" ) )
		{
			public void actionPerformed( ActionEvent evt )
			{
				boolean success;
				String errormsg = "Failed to save instantiation to file.";
				try{
					success = myInstantiationClipBoard.save();
				}catch( UnsupportedOperationException e ){
					success = false;
					errormsg += "\n" + e.getMessage();
				}

				if( !success ) showErrorDialog( errormsg );
			}
		};
		/** @since 20070904 */
		action_IMPORTINSTANTIATION = new SamiamAction( "Import", "Import instantiation from the system clipboard", 'i', MainToolBar.getIcon( "Import16.gif" ) ){
			public void actionPerformed( ActionEvent evt ){
				int count = myInstantiationClipBoard.importFromSystem();
				action_PASTEEVIDENCE.setSamiamUserMode( UI.this.getSamiamUserMode() );
				myStatusBar.setText( " read " + Integer.toString( count ) + " values from the system clipboard", StatusBar.WEST );
			}
		};
		/** @since 20070904 */
		action_EXPORTINSTANTIATION = new SamiamAction( "Export", "Export instantiation to the system clipboard", 'e', MainToolBar.getIcon( "Export16.gif" ) ){
			public void actionPerformed( ActionEvent evt ){
				myInstantiationClipBoard.exportToSystem();
			}
		};
		//"Copy   ctrl-c"
		action_COPY = new HandledModalAction( "Copy", "Copy selected subnetwork", 'c', MainToolBar.getIcon( "Copy16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				myNetworkClipBoard.copy( nif.getBeliefNetwork(), nif.getNetworkDisplay().getSelectedNodes( null ) );
				setNetworkPasteEnabled( true, nif.getSamiamUserMode(), nif );
			}
		};
		//"Cut      ctrl-x"
		action_CUT = new HandledModalAction( "Cut", "Cut selected subnetwork", 'u', MainToolBar.getIcon( "Cut16.gif" ), ModeHandler.EDIT_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				NetworkDisplay nd = nif.getNetworkDisplay();
				myNetworkClipBoard.cut( nif.getBeliefNetwork(), nd, nd.getSelectedNodes( null ) );
				setNetworkPasteEnabled( true, nif.getSamiamUserMode(), nif );
			}
		};
		//"Paste  ctrl-v"
		action_PASTE = new PasteAction( "Paste", "Paste subnetwork w/ edges and probabilities", 'p', MainToolBar.getIcon( "Paste16.gif" ), ModeHandler.EDIT_MODE )
		{
			public int sizeClipboard(){
				return (((UI.this==null) || (UI.this.myNetworkClipBoard==null)) ? (int)0 : UI.this.myNetworkClipBoard.size());
			}

			public void actionPerformed( ActionEvent e ){
				initiatePaste( false, "Please select a central point for your paste action." );
			}
		};
		//"Paste Special ctrl-p"
		action_PASTESPECIAL = new PasteAction( "Paste Special", "Paste with options", 's', MainToolBar.getIcon( "Paste16.gif" ), ModeHandler.EDIT_MODE )
		{
			public int sizeClipboard(){
				//return action_PASTE.sizeClipboard();
				return (((UI.this==null) || (UI.this.myNetworkClipBoard==null)) ? (int)0 : UI.this.myNetworkClipBoard.size());
			}

			public void actionPerformed( ActionEvent e ){
				initiatePaste(  true, "Please select a central point for your special paste action." );
			}
		};
		action_NEW = new SamiamAction( "New", "Create a new network", 'n', MainToolBar.getIcon( "New16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				newFile();
			}
		};
		action_OPEN = new SamiamAction( "Open", "Open an existing network", 'o', MainToolBar.getIcon( "Open16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				openFile();
			}
		};
		action_SAVE = new HandledModalAction( "Save", "Save the current network", 's', MainToolBar.getIcon( "Save16.gif" ), ModeHandler.SAVE_OKAY )
		{
			public void actionPerformed( ActionEvent e ){
				saveFile(getActiveHuginNetInternalFrame());
			}
		};
		action_SAVEAS = new HandledModalAction( "Save As", "Save As", 'a', MainToolBar.getIcon( "SaveAs16.gif" ), ModeHandler.SAVE_OKAY )
		{
			public void actionPerformed( ActionEvent e ){
				saveFileAs(getActiveHuginNetInternalFrame());
			}
		};
		action_CLOSE = new HandledModalAction( "Close", "Close", 'c', MainToolBar.getBlankIcon(), ModeHandler.OPENBUTNOTCOMPILING )
		{
			public void actionPerformed( ActionEvent e ){
				closeFilePromptPlusCleanup( getActiveHuginNetInternalFrame() );
				closeFileCleanup();
			}
		};
		action_CLOSEALL = new HandledModalAction( "Close All", "Close all open network files", 'l', MainToolBar.getBlankIcon(), ModeHandler.OPENBUTNOTCOMPILING )
		{
			public void actionPerformed( ActionEvent e ){
				closeAll();
				closeFileCleanup();
			}
		};
		action_EXIT = new SamiamAction( "Exit", "Exit", 'x', MainToolBar.getBlankIcon() )
		{
			public void actionPerformed( ActionEvent e ){
				exitProgram();
			}
		};
		action_NETWORKDISPLAY = new HandledModalAction( "Show", "Display the network", 's', MainToolBar.getIcon( NetworkDisplay.STR_FILENAME_ICON ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().networkTool();
			}
		};
		action_NETWORKDISPLAYCRINGE = new HandledModalAction( "Cram", "Size the network display window efficiently", 'c', MainToolBar.getIcon( NetworkDisplay.STR_FILENAME_ICON ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().getNetworkDisplay().cringe( /* not aggressive */false );
			}
		};
		action_EVIDENCETREECRAM = new HandledModalAction( "Cram", "Size the evidence tree window efficiently", 'c', MainToolBar.getIcon( "Cram16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F4, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK ) ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().cramEvidenceTree();
			}
		};
		action_EVIDENCETREEEXPANDVARIABLES = new HandledModalAction( "Expand Variables", "Expand all variable branches to show values", 'v', MainToolBar.getIcon( "TreeExpanded16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().getTreeScrollPane().getEvidenceTree().setExpandVariableBranches( true );
			}
		};
		action_BESTWINDOWARRANGEMENT = new HandledModalAction( "Arrange", "Arrange the current window nicely", 'r', (Icon)null, ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				if( nif == null ) return;

				nif.bestWindowArrangement();
			}
		};
		if( Troll.solicit().isTiger() ){
			action_SCREENSHOTSCRIPTS = new HandledModalAction( "Screenshot Scripts", "Write scripts for screenshot image processing", 's', MainToolBar.getIcon( "CursiveS16.gif" ), ModeHandler.INDEPENDANT ){
				public void actionPerformed( ActionEvent e ){
					Troll.solicit().screenshotScripts( UI.this );
				}
			};
		}else{
			action_SCREENSHOTSCRIPTS = null;
		}
		action_MAXIMIZE = new SamiamAction( "Maximize", "Maximize " + STR_SAMIAM_ACRONYM, 'm', (Icon)null )
		{
			public void actionPerformed( ActionEvent e ){
				try{
					UI.this.setExtendedState( MAXIMIZED_BOTH );
				}catch( Exception exception ){
					System.err.println( "warning: action_MAXIMIZE.actionPerformed() caught + " + exception );
				}
			}
		};
		action_PARTIALS = new HandledModalAction( "Partial Derivatives", "Partial Derivatives", 'p', MainToolBar.getBlankIcon(), ModeHandler.QUERY_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				if( nif == null ) return;

				nif.partialTool();
			}
		};
		action_TESTDSL = new HandledModalAction( "SMILEReader.testDSL()", "SMILEReader.testDSL()", 'z', MainToolBar.getBlankIcon(), ModeHandler.QUERY_MODE )
		{
			public void init(){
				setEnabled( false );
			}

			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				if( nif == null ) return;

				BeliefNetwork bn = nif.getBeliefNetwork().getSubBeliefNetwork();
				//if( bn instanceof GenieNetImpl ) ((GenieNetImpl)bn).reader.testDSL();
				//NetworkIO.testDSL();
			}
		};
		action_APPROX = new SamiamAction( "Approximation", "Approximation by edge removal", 'a', MainToolBar.getBlankIcon() )
		{
			public void actionPerformed ( ActionEvent e ){
				getActiveHuginNetInternalFrame().approxTool();
			}
		};
		action_SENSITIVITY = new HandledModalAction( "Sensitivity Analysis", "Sensitivity Analysis", 's', MainToolBar.getIcon( SensitivityInternalFrame.STR_FILENAME_ICON ), ModeHandler.QUERY_THAWED_PARTIAL_ENGINE )
		{ { putValueProtected( SamiamAction.KEY_EPHEMERAL, Boolean.TRUE ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().sensitivityTool( SensitivityInternalFrame.FLAG_CRAM );
			}
		};
		action_MAP = new HandledModalAction( "MAP", "MAP Computation", 'm', MainToolBar.getIcon( MAPInternalFrame.STR_FILENAME_ICON ), ModeHandler.OPENBUTNOTCOMPILING )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().mapTool();
			}

			//public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
			//	return ModeHandler.PARTIAL_ENGINE.decideEnabled( mode, nif ) && (nif.getCanonicalInferenceEngine() instanceof JoinTreeInferenceEngineImpl);
			//}
		};
        action_SDP = new HandledModalAction( "SDP", "SDP Computation", 'm', MainToolBar.getIcon( SDPInternalFrame.STR_FILENAME_ICON ), ModeHandler.OPENBUTNOTCOMPILING )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().sdpTool();
			}

		
		};
       
		action_MPE = new ModalAction( "MPE", "MPE Computation", 'p', MainToolBar.getIcon( MPEInternalFrame.STR_FILENAME_ICON ) )
		{ { putValueProtected( SamiamAction.KEY_EPHEMERAL, Boolean.TRUE ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().mpeTool();
			}

			public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
				return ModeHandler.QUERY_MODE.decideEnabled( mode, nif ) && (nif.getCanonicalInferenceEngine() instanceof JoinTreeInferenceEngineImpl);
			}
		};
		action_IMPACT = new HandledModalAction( "Evidence Impact", "Evidence Impact", 'i', MainToolBar.getIcon( ImpactInternalFrame.STR_FILENAME_ICON ), ModeHandler.QUERY_MODE )
		{ { putValueProtected( SamiamAction.KEY_EPHEMERAL, Boolean.TRUE ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().impactTool();
			}
		};
		action_VARIABLESELECTION = new HandledModalAction( "Variable Selection", "Variable Selection", 'v', MainToolBar.getIcon( SelectionInternalFrame.STR_FILENAME_ICON ), ModeHandler.OPENBUTNOTCOMPILING )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				if( e.getSource() instanceof edu.ucla.util.EnumProperty ) nif.selectionTool( (edu.ucla.util.EnumProperty)e.getSource() );
				else nif.selectionTool();
			}
		};
		action_EM = new HandledModalAction( "EM Learning", "EM Learning", 'e', MainToolBar.getIcon( "EM16.gif" ), ModeHandler.QUERY_MODE_SUPPORTS_PRE )
		{ { putValueProtected( SamiamAction.KEY_EPHEMERAL, Boolean.TRUE ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().emTool();
			}
		};
		action_SIMULATE = new HandledModalAction( "Generate Simulated Cases", "Simulate: Generate a Hugin Case File", 's', MainToolBar.getIcon( "Simulate16.gif" ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().simulateTool();
			}
		};
		action_CONFLICT = new HandledModalAction( "Evidence Conflict", "Display Evidence Conflicts", 'c', MainToolBar.getIcon( "Conflict16.gif" ), ModeHandler.QUERY_THAWED_PARTIAL_ENGINE )
		{ { putValueProtected( SamiamAction.KEY_EPHEMERAL, Boolean.TRUE ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().conflictTool();
			}
		};
		action_RETRACT = new HandledModalAction( "Evidence Retraction", "Evidence Retraction", 'r', MainToolBar.getIcon( "Retraction16.gif" ), ModeHandler.QUERY_THAWED_PARTIAL_ENGINE )
		{ { putValueProtected( SamiamAction.KEY_EPHEMERAL, Boolean.TRUE ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().retractTool();
			}
		};
		action_PRECOMPUTE = new ModalAction( "Precompute Marginals", "Precompute Marginals", 'm', MainToolBar.getBlankIcon() )
		{
			public void actionPerformed( ActionEvent e )
			{
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				InferenceEngine ie = nif.getInferenceEngine();
				if( ie == null){ return; }
				InferenceEngine canonical = ie.canonical();
				if( canonical instanceof RCInferenceEngine ){
					setWaitCursor();
					RCInferenceEngine rcie = (RCInferenceEngine) canonical;
					RCDgraph dg = rcie.underlyingCompilation();
					synchronized( ie ){ dg.recCond_PreCompute(); }
					setDefaultCursor();
				}
			}

			public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
				return ModeHandler.QUERY_MODE.decideEnabled( mode ) && (nif.getCanonicalInferenceEngine() instanceof RCInferenceEngine);
			}
		};
		action_HIDEALL = new HandledModalAction( "Hide All", "Hide all monitors", 'h', MainToolBar.getIcon( "Hide16.gif" ), ModeHandler.QUERY_MODE )
		{ { putValueProtected( SamiamAction.KEY_EPHEMERAL, Boolean.TRUE ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().setAllMonitorsVisible( false );
			}
		};
		action_SHOWALL = new HandledModalAction( "Show All", "Show all monitors", 'm', MainToolBar.getBlankIcon(), ModeHandler.QUERY_MODE )
		{ { putValueProtected( SamiamAction.KEY_EPHEMERAL, Boolean.TRUE ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().setAllMonitorsVisible( true );
			}
		};
		action_SHOWSELECTED = new HandledModalAction( "Selected", "Show monitors for selected nodes", 's', MainToolBar.getIcon( "Selected16.gif" ), ModeHandler.QUERY_MODE )
		{ { putValueProtected( SamiamAction.KEY_EPHEMERAL, Boolean.TRUE ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().monitorTool();
			}
		};
		action_ADDNODE = new HandledModalAction( "Add Node", "Add a node", 'n', MainToolBar.getIcon( "AddNode16.gif" ), ModeHandler.EDIT_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().addNode();
			}
		};
		action_DELETENODES = new HandledModalAction( "Delete Nodes", "Delete selected nodes", 'd', MainToolBar.getIcon( "DelNode16.gif" ), ModeHandler.EDIT_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().deleteNodes();
			}

			public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
				return super.decideEnabled( mode, nif ) && (nif != null) && (nif.getBeliefNetwork() != null) && (!nif.getBeliefNetwork().isEmpty());
			}
		};
		action_ADDEDGE = new HandledModalAction( "Add Edge", "Add an edge", 'e', MainToolBar.getIcon( "AddEdge16.gif" ), ModeHandler.EDIT_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().addEdge();
			}

			public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
				return super.decideEnabled( mode, nif ) && (nif != null) && (nif.getBeliefNetwork() != null) && (nif.getBeliefNetwork().size() >= (int)2);
			}
		};
		action_DELETEEDGE = new HandledModalAction( "Delete Edge", "Delete an edge", 'g', MainToolBar.getIcon( "DelEdge16.gif" ), ModeHandler.EDIT_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().deleteEdge();
			}

			public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
				return super.decideEnabled( mode, nif ) && (nif != null) && (nif.getBeliefNetwork() != null) && (nif.getBeliefNetwork().numEdges() > (int)0);
			}
		};

		if( Troll.solicit().isTiger() ){
			action_COPYCPT = new HandledModalAction( "Copy CPT", "Copy probability values from one cpt to another", 'p', MainToolBar.getIcon( "CPTCopy16.gif" ), ModeHandler.OPENANDNOTBUSYLOCAL ){
				{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_C, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ) ); }

				public void actionPerformed( ActionEvent e ){
					getActiveHuginNetInternalFrame().copyCPT();
				}

				public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
					return super.decideEnabled( mode, nif ) && (nif != null) && (nif.getBeliefNetwork() != null) && (nif.getBeliefNetwork().size() >= (int)2);
				}
			};

			action_REPLACEEDGE = new HandledModalAction( "Replace Edge", "Simplify the network by replacing the edge with auxiliary variables", 'r', MainToolBar.getIcon( "ReplaceEdge16.gif" ), ModeHandler.EDIT_MODE ){
				{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_E, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) ); }

				public void actionPerformed( ActionEvent e ){
					getActiveHuginNetInternalFrame().replaceEdge();
				}

				public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
					return super.decideEnabled( mode, nif ) && (myDynamator == myEDBPDynamator) && (nif != null) && (nif.getBeliefNetwork() != null) && (nif.getBeliefNetwork().numEdges() > (int)0);
				}
			};

			action_RECOVEREDGE = new HandledModalAction( "Recover Edge", "Restore an edge that was previously simplified and delete the auxiliary variables S and U'", 'e', MainToolBar.getIcon( "RecoverEdge16.gif" ), ModeHandler.EDIT_MODE ){
				{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) ); }

				public void actionPerformed( ActionEvent e ){
					getActiveHuginNetInternalFrame().recoverEdge();
				}

				public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
					DisplayableBeliefNetwork bn = null;
					return super.decideEnabled( mode, nif ) &&
					  (myDynamator == myEDBPDynamator) &&
					  (nif != null) &&
					  ((bn = nif.getBeliefNetwork()) != null) &&
					  (bn.getProperties().get( edu.ucla.belief.io.PropertySuperintendent.KEY_RECOVERABLES ) != null);
				}
			};

			action_RANDOMSPANNINGFOREST = new HandledModalAction( "Make Random Spanning Forest", "Simplify the network by replacing edges with auxiliary variables until a spanning forest remains.", 's', MainToolBar.getIcon( "ReplaceEdge16.gif" ), ModeHandler.EDIT_MODE ){
				{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, InputEvent.CTRL_MASK + InputEvent.ALT_MASK + InputEvent.SHIFT_MASK ) ); }

				public void actionPerformed( ActionEvent e ){
					Bridge2Tiger.Troll.solicit().randomSpanningForest( getActiveHuginNetInternalFrame() );
				}

				public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
					return action_REPLACEEDGE.decideEnabled( mode, nif );
				}
			};

			action_FINDBURNTBRIDGES = new HandledModalAction( "Find Burnt Bridges", "Find all replaced (\"deleted\") edges that, considered alone, disconnect the graph.", 'b', MainToolBar.getBlankIcon(), ModeHandler.EDIT_MODE ){
				{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_B, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK ) ); }
				public void actionPerformed( ActionEvent e ){
					Bridge2Tiger.Troll.solicit().findBurntBridges( getActiveHuginNetInternalFrame() );
				}

				public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
					return action_RECOVEREDGE.decideEnabled( mode, nif );
				}
			};

			action_AUTOMATICEDGERECOVERY = new HandledModalAction( "Open Control Panel", "Open automatic edge recovery in its own sub-window.", 'c', MainToolBar.getIcon( "RecoverEdge16.gif" ), ModeHandler.EDIT_MODE ){
				{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ) ); }

				public void actionPerformed( ActionEvent e ){
					Bridge2Tiger.Troll.solicit().edgeRecoveryControlPanel( getActiveHuginNetInternalFrame() );
				}

				public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
					return action_RECOVEREDGE.decideEnabled( mode, nif );
				}
			};
		}
		else{
			action_COPYCPT     = null;
			action_REPLACEEDGE = action_RECOVEREDGE = action_RANDOMSPANNINGFOREST = action_FINDBURNTBRIDGES = action_AUTOMATICEDGERECOVERY = null;
		}

		action_AUTOARRANGE = new HandledModalAction( "Simple Topological", "Automatically arrange the network in a simple topological flow", 't', MainToolBar.getIcon( "SimpleTopological16.gif" ), ModeHandler.EDIT_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().doAutoArrange();
			}
		};
		action_SQUEEZE = new HandledModalAction( "Squeeze", "Expand or contract the nodes around a given point", 's', MainToolBar.getIcon( "Squeeze16.gif" ), ModeHandler.EDIT_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				myHNIFToSqueeze = getActiveHuginNetInternalFrame();
				//NetworkDisplay fitOnScreen()
				myHNIFToSqueeze.getNetworkDisplay().promptUserActualPoint( "Please select a squeeze point.", (NetworkDisplay.UserInputListener)UI.this );
			}
		};
		action_DOTLAYOUT = new HandledModalAction( "Invoke Graphviz \"Dot\"", "Use the \"dot\" program (http://graphviz.org) to layout the network", 'd', MainToolBar.getIcon( "graphviz_dot_15x16.gif" ), ModeHandler.EDIT_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				if( nif == null ) return;
				if( myDotLayoutManager == null ) myDotLayoutManager = new DotLayoutManager();
				myDotLayoutManager.dotLayout( nif.getBeliefNetwork() );
			}
			//private DotLayoutManager myDotLayoutManager;
		};
		action_TOGGLEMODE = new HandledModalAction( "Toggle Mode", "Toggle Mode", 't', MainToolBar.getBlankIcon(), ModeHandler.OPENANDNOTBUSYLOCAL )
		{
			public void init(){
				myQueryModeIcon = MainToolBar.getIcon( "Recompile16.gif" );
				myEditModeIcon = MainToolBar.getIcon( "Edit16.gif" );
			}

			public void actionPerformed( ActionEvent e ){
				toggleSamiamUserMode();
			}

			public void setSamiamUserMode( SamiamUserMode mode ){
				super.setSamiamUserMode( mode );
				setValues( mode );
			}

			public void setMode( SamiamUserMode mode, NetworkInternalFrame nif ){
				super.setMode( mode, nif );
				setValues( mode );
			}

			public void setValues( SamiamUserMode mode )
			{
				Icon icon = myQueryModeIcon;
				String tooltip = MainToolBar.STR_QueryModeToolTipText;

				if( mode.contains( SamiamUserMode.QUERY ) && (!mode.contains( SamiamUserMode.EDIT )) ){
					icon = myEditModeIcon;
					tooltip = MainToolBar.STR_EditModeToolTipText;
				}

				setIcon( icon );
				setToolTipText( tooltip );
			}

			public Icon myQueryModeIcon;
			public Icon myEditModeIcon;
		};
		action_HIDE = new HandledModalAction( SamiamUserMode.HIDE.getName(), SamiamUserMode.HIDE.getDescription(), 'h', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				toggleMode( SamiamUserMode.HIDE );
			}
		};
		action_HIDEHIDDENEDGES = new HandledModalAction( SamiamUserMode.HIDEHIDDENEDGES.getName(), SamiamUserMode.HIDEHIDDENEDGES.getDescription(), 'h', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK ){
			public void actionPerformed( ActionEvent e ){
				toggleMode( SamiamUserMode.HIDEHIDDENEDGES );
			}
		};
		action_SHOWEDGES = new HandledModalAction( SamiamUserMode.SHOWEDGES.getName(), SamiamUserMode.SHOWEDGES.getDescription(), 'e', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				toggleMode( SamiamUserMode.SHOWEDGES );
			}
		};
		action_SHOWRECOVERABLES = new HandledModalAction( SamiamUserMode.SHOWRECOVERABLES.getName(), SamiamUserMode.SHOWRECOVERABLES.getDescription(), 'r', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				toggleMode( SamiamUserMode.SHOWRECOVERABLES );
			}
		};
		action_READONLY = new LockableHandledModalAction( SamiamUserMode.READONLY.getName(), "Disallow editing actions", 'd', MainToolBar.getBlankIcon(), ModeHandler.OPENBUTNOTMODELOCKED )
		{
			public void actionPerformed( ActionEvent e ){
				toggleMode( SamiamUserMode.READONLY );
			}
		};
		action_EVIDENCEFROZEN = new LockableHandledModalAction( SamiamUserMode.EVIDENCEFROZEN.getName(), "Disallow evidence assertions/retractions", 'e', MainToolBar.getIcon( "Snowflake16.gif" ), ModeHandler.OPENBUTNOTMODELOCKED )
		{
			{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F, InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK ) ); }
			public void actionPerformed( ActionEvent e ){
				toggleMode( SamiamUserMode.EVIDENCEFROZEN );
			}
		};
		action_LOCATIONSFROZEN = new LockableHandledModalAction( SamiamUserMode.LOCATIONSFROZEN.getName(), "Disallow changing node locations", 'l', MainToolBar.getIcon( "Snowflake16.gif" ), ModeHandler.OPENBUTNOTMODELOCKED ){
			public void actionPerformed( ActionEvent e ){
				toggleMode( SamiamUserMode.LOCATIONSFROZEN );
			}
		};

		if( Animator.FLAG_ENABLE_ANIMATION )
		{

		action_ANIMATE = new HandledModalAction( "Animate", "Animate", '1', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				toggleMode( SamiamUserMode.ANIMATE );
			}
		};
		action_RESETANIMATION = new ModalAction( "Reset Animation", "Reset animation changes", 'r', MainToolBar.getIcon( "UndoAnimation16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().getNetworkDisplay().fireRecalculateActual();
			}

			public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
				return ModeHandler.QUERY_MODE.decideEnabled( mode, nif ) && mode.contains( SamiamUserMode.ANIMATE );
			}
		};
		action_INSTANTREPLAY = new ModalAction( "Instant Replay", "Replay the last animation", 'i', MainToolBar.getIcon( "RedoAnimation16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().instantReplay();
			}

			public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
				return ModeHandler.QUERY_MODE.decideEnabled( mode, nif ) && mode.contains( SamiamUserMode.ANIMATE );
			}
		};

		}
		else{
			action_ANIMATE        = (HandledModalAction) null;
			action_RESETANIMATION = (ModalAction) null;
			action_INSTANTREPLAY  = (ModalAction) null;
		}

		action_QUERYMODE = new HandledModalAction( "Query Mode", "Query Mode", 'y', MainToolBar.getIcon( "Recompile16.gif" ), ModeHandler.OPENANDNOTBUSYLOCAL )
		{
			public void actionPerformed( ActionEvent e ){
				toggleSamiamUserMode();
			}

			public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
				return super.decideEnabled( mode, nif ) && mode.contains( SamiamUserMode.EDIT );
			}

			public void setEnabled( boolean flag ){
				super.setEnabled( flag );
				if( UI.this != null && UI.this.myQueryModeCheckBox != null ) UI.this.myQueryModeCheckBox.setSelected( !flag );
			}
		};
		action_EDITMODE = new HandledModalAction( "Edit Mode", "Edit Mode", 'e', MainToolBar.getIcon( "Edit16.gif" ), ModeHandler.QUERY_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				toggleSamiamUserMode();
			}

			public void setEnabled( boolean flag ){
				super.setEnabled( flag );
				if( UI.this != null && UI.this.myEditModeCheckBox != null ) UI.this.myEditModeCheckBox.setSelected( !flag );
			}
		};
		action_CHOOSEDYNAMATOR = new SamiamAction( "Choose Algorithm", "Choose a different inference algorithm", 'a', MainToolBar.getIcon( "Hammer16.gif" ) ){
			public void actionPerformed( ActionEvent e ){
				reselectDynamator();
			}
		};
		action_COMPILESETTINGS = new ModalAction( "Compile Settings", "Compile Settings", 's', MainToolBar.getIcon( "CompileSettings16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				CompileSettings.showDialog( UI.this );
			}

		  /*public void setSamiamUserMode( SamiamUserMode mode ){
				resetCompileSettingsEnabled( mode );
			}

			public void setMode( SamiamUserMode mode, NetworkInternalFrame nif ){
				resetCompileSettingsEnabled( mode );
			}*/

			public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
				boolean flag = false;
				if( mode != null ) flag = (mode.contains( SamiamUserMode.EDIT ) && (!mode.contains( SamiamUserMode.COMPILING )));
				flag &= myDynamator != null && myDynamator.isEditable();
				return flag;
			}
		};
		action_RESETEVIDENCE = new HandledModalAction( "Reset Evidence", "Unobserve all current evidence observations", 'r', MainToolBar.getIcon( "16px-Empty_set.svg.png" ), ModeHandler.OPENTHAWED ){
			{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_N, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK ) ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().resetEvidence( null );
			}
		};
		action_DEFAULTEVIDENCE = new HandledModalAction( "Default Evidence", "Set default evidence", 'd', MainToolBar.getIcon( "EvidenceSet16.png" ), ModeHandler.OPENTHAWED ){
			{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_D, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK ) ); }
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().setDefaultEvidence( null );
			}

			public boolean decideEnabled( SamiamUserMode mode, NetworkInternalFrame nif ){
				return super.decideEnabled( mode, nif ) && StandardNodeImpl.seenDefaultEvidence( nif.getBeliefNetwork(), nif.getBeliefNetwork() );
			}
		};
		action_PREFERENCES = new HandledModalAction( "Preferences", "Preferences", 'p', MainToolBar.getIcon( "Preferences16.gif" ), ModeHandler.NOTBUSYGLOBAL )
		{
			public void actionPerformed( ActionEvent e ){
				new Thread( run ).start();
			}

			private Runnable run = new Runnable(){
				public void run(){
					try{
						myPackageOptionsDialog = new PackageOptionsDialog( mySamiamPreferences, UI.this );
						Util.centerWindow( myPackageOptionsDialog );
						myPackageOptionsDialog.setVisible( true );
					}catch( Throwable thrown ){
						System.err.println( "warning: UI.action_PREFERENCES.run.run() caught " + thrown );
						if( Util.DEBUG_VERBOSE ){
							Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
							thrown.printStackTrace( Util.STREAM_VERBOSE );
						}
					}finally{
						myPackageOptionsDialog = null;
					}
				}
			};
		};
		action_DEFAULTPREFERENCES = new HandledModalAction( "Reset Default Preferences", "Revert to default preference values", 'd', MainToolBar.getBlankIcon(), ModeHandler.NOTBUSYGLOBAL )
		{
			public void actionPerformed( ActionEvent e ){
				mySamiamPreferences.resetDefaults();
				mySamiamPreferences.setRecentlyCommittedFlags( true );
				changePackageOptions( mySamiamPreferences );
				mySamiamPreferences.setRecentlyCommittedFlags( false );
			}
		};
		action_CLEARRECENTDOCUMENTS = new SamiamAction( "Clear Recent Documents", "Clear recent documents list in file menu", 'c', MainToolBar.getBlankIcon() )
		{
			public void actionPerformed( ActionEvent e ){
				mySamiamPreferences.clearRecentDocuments();
				updateRecentDocuments();
			}
		};
		action_FORGETTOOLLOCATIONS = new SamiamAction( "Forget Tool Locations", "Reset the locations of external tools such as Primula and Graphviz Dot", 'f', MainToolBar.getBlankIcon() )
		{
			public void actionPerformed( ActionEvent e ){
				getPrimulaManager().forgetLocation();
				if( myDotLayoutManager == null ) myDotLayoutManager = new DotLayoutManager();
				myDotLayoutManager.forgetLocation();

				NetworkInternalFrame nif;
				for( Iterator it = myListInternalFrames.iterator(); it.hasNext(); ){
					nif = (NetworkInternalFrame) it.next();
					nif.getCodeToolInternalFrame().forgetToolLocations();
				}
			}
		};
		action_HELPLOCAL = new SamiamAction( "Help", "View local help files in your browser", 'h', MainToolBar.getIcon( "Help16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				setWaitCursor();
				BrowserControl.displayRelativePath( PATH_HTML_HELP_INDEX );
				setDefaultCursor();
			}
		};
		action_HELPLIVE = new SamiamAction( "Live! Help", "View help files over the internet", 'e', MainToolBar.getIcon( "HelpOnline16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				setWaitCursor();
				BrowserControl.displayURL( URL_HELP_LIVE );
				setDefaultCursor();
			}
		};
		action_WEBSAMIAM = new SamiamAction( STR_SAMIAM_ACRONYM + " Online", "Visit "+STR_SAMIAM_ACRONYM+"'s internet home page", 'a', MainToolBar.getIcon( "SamiamOnline16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				setWaitCursor();
				BrowserControl.displayURL( URL_SAMIAM );
				setDefaultCursor();
			}
		};
		action_WEBARGROUP = new SamiamAction( "Automated Reasoning Group Online", "Visit the Automated Reasoning Group's home page", 'a', MainToolBar.getIcon( "ARGroupOnline16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				setWaitCursor();
				BrowserControl.displayURL( URL_ARGROUP );
				setDefaultCursor();
			}
		};
		action_ABOUT = new SamiamAction( "About "+STR_SAMIAM_ACRONYM, "About "+STR_SAMIAM_ACRONYM, 'a', MainToolBar.getIcon( "About16.gif" ), KeyStroke.getKeyStroke( KeyEvent.VK_A, InputEvent.CTRL_MASK ) )
		{
			public void actionPerformed( ActionEvent e ){
				if( myAboutDlg == null ){
					myAboutDlg = new AboutDlg( UI.this );
				}
				myAboutDlg.setVisible( true );
			}

			private AboutDlg myAboutDlg;
		};
		action_TUTORIALS = new SamiamAction( "Video Tutorials", "View local video tutorials", 't', MainToolBar.getIcon( "Tutorials16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				if( !showTutorialsDialog( false ) ) showWarningDialog( "Tutorials not found in htmlhelp/tutorials.\nAvailable for download at http://reasoning.cs.ucla.edu/samiam", JOptionPane.DEFAULT_OPTION );
			}
		};
		action_TUTORIALSLIVE = new SamiamAction( "Live! Video Tutorials", "Download video tutorials from the "+STR_SAMIAM_ACRONYM+" home page", 'o', MainToolBar.getIcon( "TutorialsOnline16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				setWaitCursor();
				BrowserControl.displayURL( URL_TUTORIALS );
				setDefaultCursor();
			}
		};
		action_RC = new HandledModalAction( "Recursive Conditioning, Pr(e) Only", "Calculate Pr(e) using the recursive conditioning tool", 'r', MainToolBar.getIcon( edu.ucla.belief.ui.rc.RecursiveConditioningInternalFrame.STR_FILENAME_ICON ), ModeHandler.OPENBUTNOTCOMPILING )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().recursiveConditioningTool();
			}
		};
		action_CONSOLE = new HandledModalAction( "Console", "View the console window", 'c', MainToolBar.getIcon( ConsoleFrameWriter.STR_FILENAME_ICON ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().setConsoleVisible( true );
			}
		};
		action_CODETOOLNETWORK = new HandledModalAction( "Encode network", CodeToolInternalFrame.STR_DISPLAY_NAME_1CAP+": encode network", 'n', MainToolBar.getIcon( NetworkDisplay.STR_FILENAME_ICON ), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				if( nif != null ) nif.codeToolNetwork();
			}
		};
		action_CODETOOLPROBABILITY = new HandledModalAction( "Encode probability query", CodeToolInternalFrame.STR_DISPLAY_NAME_1CAP+": encode probability query", 'n', MainToolBar.getIcon( "Selected16.gif" ), ModeHandler.QUERY_MODE )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				if( nif != null ) nif.codeToolProbability();
			}
		};
		action_CODETOOLCPTDEMO = new SamiamAction( "Encode cpt demo", CodeToolInternalFrame.STR_DISPLAY_NAME_1CAP+": encode cpt demo", 'n', MainToolBar.getIcon( "CPT16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				if( nif != null ) nif.codeToolCPTDemo();
			}
		};
		action_HELPLOCALCODETOOL = new SamiamAction( CodeToolInternalFrame.STR_DISPLAY_NAME+" Help", "View local help for "+CodeToolInternalFrame.STR_DISPLAY_NAME+" in your browser", 'h', MainToolBar.getIcon( "Help16.gif" ) )
		{
			public void actionPerformed( ActionEvent e ){
				setWaitCursor();
				BrowserControl.displayRelativePath( PATH_HTML_HELP_CODETOOL );
				setDefaultCursor();
			}
		};
		action_RCTempTimeGraph = new HandledModalAction( "Time Graph", "Time Graph", 'r', MainToolBar.getIcon( "RC16.gif" ), ModeHandler.QUERY_MODE )
		{
			public void actionPerformed( ActionEvent e )
			{
				NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
				InferenceEngine ie = nif.getInferenceEngine();
				if( ie == null){ return; }
				InferenceEngine canonical = ie.canonical();
				if( canonical instanceof RCInferenceEngine ){
					setWaitCursor();
					RCInferenceEngine rcie = (RCInferenceEngine) canonical;
					RCDgraph dg = rcie.underlyingCompilation();
					synchronized( ie ){ dg.recCond_All( true, true, true); }
					setDefaultCursor();
				}
			}
		};
		action_SELECTALL = new HandledModalAction( "Select All", "Select All", 'a', MainToolBar.getBlankIcon(), ModeHandler.OPEN_NETWORK )
		{
			public void actionPerformed( ActionEvent e ){
				getActiveHuginNetInternalFrame().getNetworkDisplay().selectAll();
			}
		};
		if( Troll.solicit().isTiger() ){
			action_CPTMONITORS = new HandledModalAction( "CPT Monitors", "Show CPT monitors for selected nodes", 't', MainToolBar.getIcon( "CPTCopy16.gif" ), ModeHandler.OPEN_NETWORK ){
				{ this.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_T, InputEvent.CTRL_MASK ) ); }
				public void actionPerformed( ActionEvent e ){
					getActiveHuginNetInternalFrame().setCPTMonitorsVisible( true, true );
				}
			};
		}else{
			action_CPTMONITORS = null;
		}

		if( FLAG_ENABLE_PRIMULA ){
			action_PRIMULA = new HandledModalAction( "Primula", "Invoke Primula", 'p', MainToolBar.getIcon( "Primula16.png" ), ModeHandler.INDEPENDANT )
			{
				public void actionPerformed( ActionEvent e ){
					try{
						getPrimulaManager().openPrimula();
					}catch( UnsatisfiedLinkError unsatisfiedlinkerror ){
						showErrorDialog( "Failed to invoke Primula because:\n" + unsatisfiedlinkerror.getMessage() );
					}
				}
			};
		}else{
			action_PRIMULA = (HandledModalAction)null;
		}

		action_PRUNE = new ActionPrune(){
			public NetworkInternalFrame getNIFToPrune(){
				return UI.this.getActiveHuginNetInternalFrame();
			}
		};
		action_KILLLOCAL  = new HandledModalAction( "Stop local tasks",  "Kill all tasks running on the current network", 'k', MainToolBar.getIcon( "Stop16.gif" ), ModeHandler.BUSYLOCAL )
		{
			public void actionPerformed( ActionEvent e ){
				UI.this.killAll( true );
			}
		};
		action_KILLGLOBAL = new HandledModalAction( "Stop global tasks", "Kill all "+STR_SAMIAM_ACRONYM+" tasks",         'g', MainToolBar.getIcon( "Stop16.gif" ), ModeHandler.BUSYGLOBAL )
		{
		  //public void setEnabled( boolean flag ){
		  //	System.out.println( "action_KILLGLOBAL.setEnabled( "+flag+" )" );
		  //super.setEnabled( flag );
		  //}

			public void actionPerformed( ActionEvent e ){
				UI.this.killAll( false );
			}
		};

		if( isJavaVersionOK() ) init( preferenceFile );
	}

	/** @since 20021022 */
	protected void init( File preferenceFile )
	{
		UI.this.myThreadGroup = new ThreadGroup( STR_SAMIAM_ACRONYM + " threads" );
		edu.ucla.util.SystemGCThread.getThreadGroup();//assures correctly rooted parent
		myListInternalFrames = new LinkedList();
		mySamiamPreferences = (preferenceFile == null ) ? new SamiamPreferences( true ) : new SamiamPreferences( preferenceFile );

		myListDynamators = new ArrayList( 9 );
		myListDynamators.add( myDynamator = new DisplayableRCEngineGenerator( new edu.ucla.belief.inference.RCEngineGenerator(), this ) );
		myListDynamators.add( new DisplayableJTEngineGenerator( new JEngineGenerator(), this ) );
		myListDynamators.add( new DisplayableJTEngineGenerator( new HuginEngineGenerator(), this ) );
		myListDynamators.add( new DisplayableJTEngineGenerator( new ZCEngineGenerator(), this ) );
		if( Util.DEBUG ) myListDynamators.add( new DisplayableJTEngineGenerator( new SSEngineGenerator(), this ) );
		if( Util.DEBUG ) myListDynamators.add( myDynamator = new edu.ucla.belief.ui.recursiveconditioning.DisplayableRCEngineGenerator( new edu.ucla.belief.recursiveconditioning.RCEngineGenerator(), this ) );
		myListDynamators.add( myDynamator = new DisplayablePropagationEngineGenerator( new edu.ucla.belief.approx.PropagationEngineGenerator(), this ) );
		if( Util.DEBUG ) myListDynamators.add( new edu.ucla.belief.inference.RandomEngineGenerator() );
		Troll.solicit().dynamators( myListDynamators, this );
		Dynamator dyn;
		for( Iterator it = myListDynamators.iterator(); it.hasNext(); ){
			if( (dyn = (Dynamator) it.next()).getCanonicalDynamator().getClass().getPackage().getName().equals( "edu.ucla.belief.approx" ) &&
			    (((String)dyn.getKey()).indexOf( "edgedeletionenginegenerator" ) >= 0) ){ myEDBPDynamator = dyn; }
		}
		myListDynamators = Collections.unmodifiableCollection( myListDynamators );

		initLookAndFeel();

		setBounds( 0, 0, 800, 600 );
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(this);

		myJDesktopPane = new JDesktopPane();
		//myJDesktopPane = new SafeDesktopPane();

		// Initialize the toolbars
		myToolBar = new MainToolBar(this);
		myInstantiationClipboardToolBar = new InstantiationClipboardToolBar(this);
		//auxToolBar = new AuxiliaryToolBar(this);
		//JPanel toolBarPane = new JPanel();
		//toolBarPane.setLayout( new BorderLayout() );
		//toolBarPane.add( auxToolBar, BorderLayout.SOUTH );
		//toolBarPane.add( myToolBar, BorderLayout.NORTH );

		try{
			rememberLastDynamator();
		}catch( java.beans.PropertyVetoException pve ){
			System.err.println( "warning: UI.init() [rememberLastDynamator()] caught " + pve );
		}

		initStatusBar();

		Container myContentPane = getContentPane();
		myContentPane.setLayout( new BorderLayout() );

		myCenterPain = new JPanel( new BorderLayout() );

		myCenterPain.add( myJDesktopPane, BorderLayout.CENTER );
		Object blConstraints   = BorderLayout.EAST;
		int    icbtOrientation =     JToolBar.VERTICAL;
		try{
			Object candidate   = mySamiamPreferences.getProperty( STR_KEY_ICBTCONSTRAINTS );
			if(    candidate  != null ){ blConstraints   = candidate.toString(); }
		  //candidate          = mySamiamPreferences.getProperty( STR_KEY_ICBTORIENTATION );
		  //if(    candidate  != null ){ icbtOrientation = Integer.parseInt( candidate.toString() ); }
			if(      "North".equals( blConstraints ) ){ icbtOrientation = JToolBar.HORIZONTAL; }
			else if( "South".equals( blConstraints ) ){ icbtOrientation = JToolBar.HORIZONTAL; }
			else if(  "East".equals( blConstraints ) ){ icbtOrientation = JToolBar.VERTICAL;   }
			else if(  "West".equals( blConstraints ) ){ icbtOrientation = JToolBar.VERTICAL;   }
		}catch( Throwable thrown ){
			System.err.println( "warning: UI.init() caught " + thrown );
		}
		myInstantiationClipboardToolBar.setOrientation( icbtOrientation );
		myCenterPain.add( myInstantiationClipboardToolBar, blConstraints );

		myContentPane.add( myCenterPain, BorderLayout.CENTER );
		myContentPane.add( myToolBar, BorderLayout.NORTH );
		myContentPane.add( myStatusBar, BorderLayout.SOUTH );

		initJMenuBar();

		initPreferenceMediators();
		mySamiamPreferences.setRecentlyCommittedFlags( true );
		changePackageOptions( mySamiamPreferences );
		mySamiamPreferences.setRecentlyCommittedFlags( false );
		//setMaxRecentDocuments();
		updateRecentDocuments();

		setSamiamUserMode( new SamiamUserMode() );
		try{
			setDynamator( myDynamator );
		}catch( java.beans.PropertyVetoException pve ){
			System.err.println( "warning: UI.init() [setDynamator()] caught " + pve );
		}
	}

	/** interface SamiamUIInt
		@since 20040408 */
	public JFrame asJFrame(){
		return this;
	}
	public void setPrimulaUIInstance( PrimulaUIInt ui ){
		getPrimulaManager().setPrimulaUIInstance( ui );
	}
	public PrimulaManager getPrimulaManager(){
		if( myPrimulaManager == null ) myPrimulaManager = new PrimulaManager( this );
		return myPrimulaManager;
	}
	public void setInvokerName( String invoker ){
		myInvokerName = invoker;
	}
	public String getInvokerName(){
		return myInvokerName;
	}

	/** @since 20050211 */
	public void setVisibleStatusBar( boolean newSetting ){
		if( myStatusBar.isVisible() == newSetting ) return;
		myStatusBar.setVisible( newSetting );
		myShowStatusBarItem.setSelected( newSetting );
		if( newSetting ) setShowPrE();
		else myFlagDefeatPrE = true;
	}

	/** @since 20050211 */
	public void setVisibleToolbarMain( boolean newSetting ){
		if( myToolBar.isVisible() == newSetting ) return;
		myToolBar.setVisible( newSetting );
		myShowToolBarItem.setSelected( newSetting );
	}

	/** @since 20050211 */
	public void setVisibleToolbarInstantiation( boolean newSetting ){
		if( myInstantiationClipboardToolBar.isVisible() == newSetting ) return;
		myInstantiationClipboardToolBar.setVisible( newSetting );
		myShowInstantiationToolBarItem.setSelected( newSetting );
	}

	/** @since 20040805 */
	private void initPreferenceMediators()
	{
		if( Animator.FLAG_ENABLE_ANIMATION )
			addPreferenceMediator( AnimationPreferenceHandler.getInstance() );

		PreferenceMediator next = null;
		for( Iterator it = getPreferenceMediators().iterator(); it.hasNext(); ){
			next = (PreferenceMediator) it.next();
			next.massagePreferences( mySamiamPreferences );
			next.setPreferences( mySamiamPreferences );
		}
	}

	/** @since 20030311 */
	protected void rememberLastDynamator() throws java.beans.PropertyVetoException{
		String    strKey = mySamiamPreferences.getLastDynamator();
		Dynamator next;
		for( Iterator it = myListDynamators.iterator(); it.hasNext(); ){
			next = (Dynamator) it.next();
			if( next.getKey().equals( strKey ) ){
				setDynamator( next );
				return;
			}
		}
	}

	/** @since 20030117 */
	public Collection getDynamators()
	{
		return myListDynamators;
	}

	/** @since 20030529 */
	protected Dynamator validate( Dynamator dyn )
	{
		Dynamator validated;
		for( Iterator it = myListDynamators.iterator(); it.hasNext(); )
		{
			validated = (Dynamator) it.next();
			if( validated.equals( dyn ) ) return validated;
		}
		return null;
	}

	/** @since 20030117 */
	public void setDynamator( Dynamator d ) throws PropertyVetoException{
	  //System.out.println( "UI.setDynamator( " + d.getDisplayName() + " )" );
		if(         myDynamator.equals( d ) ){ return; }
		Dynamator    valid  = validate( d );
		if(          valid != null ){
			if( (myDynamatorListeners != null) && (! myDynamatorListeners.isEmpty()) ){
				PropertyChangeEvent pce = new PropertyChangeEvent( (UI) this, "dynamator", myDynamator, valid );
				DynamatorListener vcl;
				for( Iterator it = myDynamatorListeners.iterator(); it.hasNext(); ){
					if( (vcl = (DynamatorListener) it.next()) == null ){ it.remove(); }
					else{ vcl.vetoableChange( pce ); }
				}
			}

			myDynamator           = valid;
			myToolBar.setDynamator( valid );

			SamiamUserMode       mode = null;
			NetworkInternalFrame nif  = getActiveHuginNetInternalFrame();
			if( nif != null ){
				mode = nif.getSamiamUserMode();
				nif.console.println( "\n\nchoosing " + valid.getDisplayName() );
			}
			ModalAction.setModeAllRegistered( mode == null ? new SamiamUserMode() : mode, nif );//setSamiamUserMode( mode );//resetCompileSettingsEnabled( mode );
		}
	}

	/** @since 20091111 */
	public SamiamUserMode getSamiamUserMode(){
		NetworkInternalFrame nif  = getActiveHuginNetInternalFrame();
		return nif == null ? new SamiamUserMode() : nif.getSamiamUserMode();
	}

	/** @since 20081022 */
	public UI addDynamatorListener( DynamatorListener vcl ){
		if( myDynamatorListeners == null ){ myDynamatorListeners = new WeakLinkedList(); }
		else if( myDynamatorListeners.contains( vcl ) ){ return this; }
		myDynamatorListeners.add( vcl );
		return this;
	}

	/** @since 20081022 */
	public boolean removeDynamatorListener( DynamatorListener vcl ){
		if( myDynamatorListeners == null ){ return false; }
		else return myDynamatorListeners.remove( vcl );
	}

	private Collection myDynamatorListeners;

	/** @since 20030523 *//*
	protected void resetCompileSettingsEnabled( SamiamUserMode mode )
	{
		boolean flag = false;
		if( mode != null ) flag = (mode.contains( SamiamUserMode.EDIT ) && (!mode.contains( SamiamUserMode.COMPILING )));
		flag &= myDynamator != null && myDynamator.isEditable();
		action_COMPILESETTINGS.setEnabled( flag );
	}*/

	//protected JMenu myJMenuCurrentDynamator;

	/** @since 20030117 */
	public Dynamator getDynamator()
	{
		return myDynamator;
	}

	/** @since 20021219 */
	protected boolean showTutorialsDialog( boolean welcome )
	{
		if( myTutorials == null ) myTutorials = new Tutorials();
		if( welcome ) return myTutorials.showDialog( this );
		else return myTutorials.showDialog( false, STR_SAMIAM_ACRONYM + " Video Tutorials", this );
	}

	protected Tutorials myTutorials;

	/** @since 20021219 */
	public void setVisible( boolean flag )
	{
		super.setVisible( flag );
		if( flag && (UI.FLAG_FORCE_WELCOME /*|| (! mySamiamPreferences.wasFileIOSuccessful())*/) ){ showTutorialsDialog( true ); }
	}

	/** @arg flag Sets whether the JVM should terminate when the user closes SamIam.  Set this to false if you call SamIam from another Java program and you want to prevent Java from exiting when the user exits SamIam.
		@since 20040210 */
	public void setSystemExitEnabled( boolean flag ){
		myFlagSystemExitEnabled = flag;
	}

	/** @ret true if a user action that closes SamIam will cause the JVM to terminate as well.
		@since 20040210 */
	public boolean isSystemExitEnabled(){
		return myFlagSystemExitEnabled;
	}

	/** @since 20021022 */
	public static boolean isJavaVersionOK()
	{
		String version = System.getProperty("java.version");
		if( Double.parseDouble(version.substring(0, 3)) >= (double)1.4 ) return true;
		else
		{
			System.err.println( "You are using Java version " + version + "." );
			System.err.println( STR_SAMIAM_ACRONYM + " requires Java version 1.4 or higher." );
			//System.exit(0);
			return false;
		}
	}

	/** @since 20021122 */
	public MainToolBar getMainToolBar()
	{
		return myToolBar;
	}

	/** @since 20050214 valentine's day! */
	public InstantiationClipboardToolBar getInstantiationToolBar(){
		return myInstantiationClipboardToolBar;
	}

	/** @since 102202 */
	protected void initLookAndFeel()
	{
		setPkgDspOptLookAndFeel( true );//keith
		/*
		try
		{
			//Skin skin = SkinLookAndFeel.loadThemePack( "c:\\keithcascio\\downloads\\xplunathemepack.zip" );
			//Skin skin = SkinLookAndFeel.loadThemePack( "c:\\keithcascio\\downloads\\macosthemepack.zip" );
			//Skin skin = SkinLookAndFeel.loadThemePack( "c:\\keithcascio\\downloads\\aquathemepack.zip" );
			//SkinLookAndFeel.setSkin( skin );
			//UIManager.setLookAndFeel( new SkinLookAndFeel() );
			//UIManager.setLookAndFeel( new com.incors.plaf.kunststoff.KunststoffLookAndFeel() );
		}catch( Exception e ) {

			e.printStackTrace();
		}*/
	}

	/** @since 20021022 */
	protected void initJMenuBar()
	{
		menuBar = new JMenuBar();
		setJMenuBar( menuBar );

		// Create the menus at the top of the screen
		fileMenu        = createMenu( "File"       , 'f' );
		editMenu        = createMenu( "Edit"       , 'e' );
		modeMenu        = createMenu( "Mode"       , 'm' );
		queryMenu       = createMenu( "Query"      , 'q' );
		toolsMenu       = createMenu( "Tools"      , 't' );
		viewMenu        = createMenu( "View"       , 'v' );
		preferencesMenu = createMenu( "Preferences", 'p' );
		windowMenu      = createMenu( "Window"     , 'w' );
		helpMenu        = createMenu( "Help"       , 'h' );

		windowMenu.add( action_MAXIMIZE              ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F11, (int)0 ) );
		windowMenu.add( action_BESTWINDOWARRANGEMENT ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F4,  (int)0 ) );
		if( action_SCREENSHOTSCRIPTS != null ){ windowMenu.add( action_SCREENSHOTSCRIPTS ); }
		windowMenu.addSeparator();
		JMenu killMenu = createMenu( "Stop tasks", 's' );
		killMenu.setIcon( MainToolBar.getIcon( "Stop16.gif" ) );
		killMenu.add( action_KILLGLOBAL ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_PAUSE, (int)0 ) );
		killMenu.add( action_KILLLOCAL  ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_PAUSE, InputEvent.CTRL_MASK ) );
		windowMenu.add( killMenu );
		windowMenu.addSeparator();

		//View menu
		//using setAccelerator() for zoom in/zoom out
		//seems like a performance bottleneck under Java 1.3.1
		viewMenu.add( action_ZOOMIN );//.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_EQUALS, InputEvent.CTRL_MASK ) );
		viewMenu.add( action_ZOOMOUT );//.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_MINUS, InputEvent.CTRL_MASK ) );
		menuZoom = createMenu( "Zoom",'z' );
		menuZoom.setIcon( MainToolBar.getIcon( "Zoom16.gif" ) );
		menuZoom.add( action_ACTUALPIXELS );
		menuZoom.add( action_FITONSCREEN );
		viewMenu.add( menuZoom );
		viewMenu.add( action_REFRESH ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F5, (int)0 ) );
		if( action_INSTANTREPLAY != null ) viewMenu.add( action_INSTANTREPLAY );
		viewMenu.add( action_CONSOLE );

		JMenu menuTree = createMenu( "Tree", 't' );
		menuTree.setIcon( MainToolBar.getIcon( "Tree16.gif" ) );
		menuTree.add( action_EVIDENCETREECRAM );
		menuTree.add( action_EVIDENCETREEEXPANDVARIABLES );
		viewMenu.add( menuTree );

		JMenu menuNetworkDisplay = createMenu( "Network Display", 'd' );
		menuNetworkDisplay.setIcon( MainToolBar.getIcon( NetworkDisplay.STR_FILENAME_ICON ) );
		menuNetworkDisplay.add( action_NETWORKDISPLAY );
		menuNetworkDisplay.add( action_NETWORKDISPLAYCRINGE );
		viewMenu.add( menuNetworkDisplay );

		myShowStatusBarItem = createCheckBoxMenuItem( viewMenu, action_SHOWSTATUSBAR );
		myShowStatusBarItem.setSelected( true );
		myShowToolBarItem = createCheckBoxMenuItem( viewMenu, action_SHOWTOOLBAR  );
		myShowToolBarItem.setSelected( true );
		myShowInstantiationToolBarItem = createCheckBoxMenuItem( viewMenu, action_SHOWINSTANTIATIONTOOLBAR  );
		myShowInstantiationToolBarItem.setSelected( true );

		// File menu
		fileMenu.add( action_NEW      ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F2, (int)0 ) );
		fileMenu.add( action_OPEN     ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_O, InputEvent.CTRL_MASK ) );
		fileMenu.add( action_SAVE     ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, InputEvent.CTRL_MASK ) );
		fileMenu.add( action_SAVEAS   ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK ) );
		fileMenu.add( action_CLOSE    ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_W, InputEvent.CTRL_MASK ) );
		fileMenu.add( action_CLOSEALL ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_W, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK ) );
		fileMenu.addSeparator();
		fileMenu.addSeparator();
		fileMenu.add( action_EXIT );

		//Tools menu
		toolsMenu.addMenuListener( this );
		toolsMenu.add( action_INFORMATION ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_I, InputEvent.CTRL_MASK ) );
		toolsMenu.add( action_RC ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_MASK ) );
		toolsMenu.add( action_MAP );
        toolsMenu.add( action_SDP );
		toolsMenu.add( action_SIMULATE ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_W, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) );

		toolsMenu.add( action_VARIABLESELECTION ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_J, InputEvent.CTRL_MASK ) );
		instantiationMenu = createMenu( "Instantiation clipboard",'i' );
		instantiationMenu.setIcon( MainToolBar.getIcon( InstantiationClipBoard.STR_FILENAME_ICON ) );
		instantiationMenu.addMenuListener( this );
		toolsMenu.add( instantiationMenu );
		instantiationMenu.add( action_COPYEVIDENCE ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_C, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) );
		instantiationMenu.add( action_CUTEVIDENCE ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_X, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) );
		instantiationMenu.add( action_PASTEEVIDENCE ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_V, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) );
		instantiationMenu.add( action_VIEWINSTANTIATIONCLIPBOARD ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_B, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) );
		if( NetworkIO.xmlAvailable() )
		{
			instantiationMenu.add( action_LOADINSTANTIATIONCLIPBOARD ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_O, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) );
			instantiationMenu.add( action_SAVEINSTANTIATIONCLIPBOARD ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_S, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) );
		}
		instantiationMenu.add( action_IMPORTINSTANTIATION ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_I, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) );
		instantiationMenu.add( action_EXPORTINSTANTIATION ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_E, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) );

		codeToolMenu = createMenu( CodeToolInternalFrame.STR_DISPLAY_NAME,'c' );//CodeToolInternalFrame.STR_DISPLAY_NAME_1CAP
		codeToolMenu.setIcon( MainToolBar.getIcon( CodeToolInternalFrame.STR_FILENAME_ICON ) );
		codeToolMenu.addMenuListener( this );
		toolsMenu.add( codeToolMenu );
		codeToolMenu.add( action_CODETOOLNETWORK );//.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_C, InputEvent.CTRL_MASK + InputEvent.ALT_MASK ) );
		codeToolMenu.add( action_CODETOOLPROBABILITY );
		codeToolMenu.add( action_CODETOOLCPTDEMO );
		codeToolMenu.add( action_HELPLOCALCODETOOL );
		if( FileSystemUtil.getAPIAction() != null ) codeToolMenu.add( FileSystemUtil.getAPIAction() );
		if( action_PRIMULA != null ) toolsMenu.add( action_PRIMULA );//.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_I, InputEvent.CTRL_MASK ) );
		if( action_PRUNE   != null ) toolsMenu.add( action_PRUNE   ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_P, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK ) );
		if( action_COPYCPT != null ) toolsMenu.add( action_COPYCPT );

		//Query Menu
		queryMenu.add( action_SENSITIVITY );
		//queryMenu.add( action_MAP );
		queryMenu.add( action_MPE );
		queryMenu.add( action_EM );
		queryMenu.add( action_IMPACT );
		queryMenu.addSeparator();
		queryMenu.add( action_CONFLICT );
		queryMenu.add( action_RETRACT );
		queryMenu.addSeparator();
		if( Util.DEBUG ) queryMenu.add( action_APPROX );

		// Show monitors (submenu of Query menu)
		menuMonitors = createMenu( "Show monitors",'o' );
		menuMonitors.addMenuListener( this );
		menuMonitors.setIcon( MainToolBar.getIcon( "Selected16.gif" ) );
		queryMenu.add( menuMonitors );
		menuMonitors.add( action_SHOWSELECTED ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F, InputEvent.CTRL_MASK ) );
		menuMonitors.add( action_HIDEALL );
		menuMonitors.add( action_SHOWALL );
		//myShowTargetMonitorsItem = createCheckBoxMenuItem( menuMonitors, action_SHOWTARGET );
		//myShowObservationMonitorsItem = createCheckBoxMenuItem( menuMonitors, action_SHOWOBSERVATION );

		//Edit Menu
		editMenu.add( action_ADDNODE ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_N, InputEvent.CTRL_MASK ) );
		editMenu.add( action_DELETENODES ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, (int)0 ) );
		editMenu.add( action_ADDEDGE ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_E, InputEvent.CTRL_MASK ) );
		editMenu.add( action_DELETEEDGE ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_D, InputEvent.CTRL_MASK ) );
		if( FLAG_ENABLE_EDBP_MANUAL && (action_REPLACEEDGE != null) ){
			editMenu.addSeparator();
			editMenu.add( action_REPLACEEDGE             );
			editMenu.add( action_RECOVEREDGE             );
			editMenu.add( action_RANDOMSPANNINGFOREST    );
			editMenu.add( action_FINDBURNTBRIDGES        );
			final JMenu menuAutomaticEdgeRecovery = new JMenu( "Automatic Edge Recovery" );
			menuAutomaticEdgeRecovery.add( action_AUTOMATICEDGERECOVERY   );
			menuAutomaticEdgeRecovery.addSeparator();
			menuAutomaticEdgeRecovery.setIcon( MainToolBar.getIcon( "RecoverEdge16.gif" ) );
			editMenu.add( menuAutomaticEdgeRecovery );
			editMenu.addMenuListener( new MenuListener(){
				public void menuCanceled(   MenuEvent e ){}
				public void menuDeselected( MenuEvent e ){}
				public void menuSelected(   MenuEvent e ){
					menuAutomaticEdgeRecovery.setEnabled( action_RECOVEREDGE.isEnabled() );
				}
			} );
			Bridge2Tiger.Troll.solicit().addEdgeRecovery( menuAutomaticEdgeRecovery, UI.this );
		}
		editMenu.addSeparator();
		editMenu.add( action_COPY ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_C, InputEvent.CTRL_MASK ) );
		editMenu.add( action_CUT ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_X, InputEvent.CTRL_MASK ) );
		editMenu.add( action_PASTE ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_V, InputEvent.CTRL_MASK ) );
		editMenu.add( action_PASTESPECIAL ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_P, InputEvent.CTRL_MASK ) );
		editMenu.addSeparator();
		menuAutoArrange = createMenu( "Arrange",'a' );
		editMenu.add( menuAutoArrange );
		menuAutoArrange.add( action_AUTOARRANGE );
		menuAutoArrange.add( action_SQUEEZE );
		menuAutoArrange.add( action_DOTLAYOUT );
		editMenu.add( action_SELECTALL        ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_A, InputEvent.CTRL_MASK ) );
		if( action_CPTMONITORS != null ){ editMenu.add( action_CPTMONITORS ); }

		//Mode menu
		try{
			modeMenu.add( action_CHOOSEDYNAMATOR ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F7, 0 ) );
		}catch( Throwable thrown ){
			System.err.println( "warning: UI.initJMenuBar() caught " + thrown );
		}
		(myQueryModeCheckBox = createCheckBoxMenuItem( modeMenu, action_QUERYMODE )).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_M, InputEvent.CTRL_MASK ) );
		//"Query Mode", 'q', modeMenu, action_QUERYMODE );
		//if( myToolBar.myQueryModeIcon != null ) myQueryModeCheckBox.setIcon( myToolBar.myQueryModeIcon );
		(myEditModeCheckBox = createCheckBoxMenuItem( modeMenu, action_EDITMODE )).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_M, InputEvent.CTRL_MASK ) );
		//"Edit Mode", 'e', modeMenu, action_EDITMODE );
		//if( myToolBar.myEditModeIcon != null ) myEditModeCheckBox.setIcon( myToolBar.myEditModeIcon );
		modeMenu.add( action_COMPILESETTINGS ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F3, (int)0) );
		modeMenu.add( action_RESETEVIDENCE   );
		modeMenu.add( action_DEFAULTEVIDENCE );

		(myReadOnlyCheckBox = createCheckBoxMenuItem( modeMenu, action_READONLY )).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_E, InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK ) );
		 myEvidenceFrozenCheckBox = createCheckBoxMenuItem( modeMenu, action_EVIDENCEFROZEN  );
		(myLocationFrozenCheckBox = createCheckBoxMenuItem( modeMenu, action_LOCATIONSFROZEN )).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_L, InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK ) );
		if( action_ANIMATE != null ) (myAnimateCheckBox = createCheckBoxMenuItem( modeMenu, action_ANIMATE )).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_A, InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK ) );

		JMenu menuShow = createMenu( "Show", 's' );
		modeMenu.add( menuShow );
		(myShowEdgesCheckBox        = createCheckBoxMenuItem( menuShow, action_SHOWEDGES        )).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_E, InputEvent.CTRL_MASK+InputEvent.ALT_MASK+InputEvent.SHIFT_MASK ) );
		(myShowRecoverablesCheckBox = createCheckBoxMenuItem( menuShow, action_SHOWRECOVERABLES )).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_MASK+InputEvent.ALT_MASK+InputEvent.SHIFT_MASK ) );
		(myHideCheckBox             = createCheckBoxMenuItem( modeMenu, action_HIDE             )).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_H, InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK ) );
		(myHHECheckBox              = createCheckBoxMenuItem( modeMenu, action_HIDEHIDDENEDGES  )).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_H, InputEvent.CTRL_MASK+InputEvent.SHIFT_MASK+InputEvent.ALT_MASK ) );

		//Preferences menu
		preferencesMenu.add( action_PREFERENCES ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F9, (int)0 ) );
		preferencesMenu.add( action_DEFAULTPREFERENCES );
		preferencesMenu.add( action_CLEARRECENTDOCUMENTS );
		preferencesMenu.add( action_FORGETTOOLLOCATIONS );

		//Help menu
		helpMenu.add( action_HELPLOCAL ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F1, (int)0 ) );
		helpMenu.add( action_HELPLIVE );
		helpMenu.add( action_TUTORIALS ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F12, (int)0 ) );
		helpMenu.add( action_TUTORIALSLIVE );
		if( FileSystemUtil.getAPIAction() != null ) helpMenu.add( FileSystemUtil.getAPIAction() );
		helpMenu.addSeparator();
		helpMenu.add( action_WEBSAMIAM );
		helpMenu.add( action_WEBARGROUP );
		helpMenu.addSeparator();
		helpMenu.add( action_ABOUT ).setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F1, InputEvent.SHIFT_MASK ) );

		if( Util.DEBUG )
		{
			queryMenu.add( action_PRECOMPUTE );
			helpMenu.add( action_PARTIALS );
			helpMenu.add( action_TESTDSL );
			//debugDFSItem = createMenuItem("DFS DEBUG",'d', helpMenu );
			//debugCPTEditItem = createMenuItem("TOGGLE GENIE STYLE CPT EDIT",'t', helpMenu );
			//debugSubmodelItem = createMenuItem("SUBMODEL",'s', helpMenu );
			//debugRecursiveConditioningItem = createMenuItem("RECURSIVE CONDITIONING",'r', helpMenu );
			debugCloneItem = createMenuItem("CLONE NETWORK",'c', helpMenu );
			//debugUserModeItem = createMenuItem("TOGGLE USER MODE",'m', helpMenu );
			//(debugRoundComplementItem = createMenuItem("TOGGLE ROUND COMPLEMENT",'r', helpMenu )).setEnabled( false );
			//debugInferenceEngineCacheHitsItem = createMenuItem("INFERENCE ENGINE CACHE HITS", 'h', helpMenu);
			//debugRepaintEvidenceTreeItem = createMenuItem("REPAINT EVIDENCE TREE", 'e', helpMenu);
			debugWindowDimensionsItem = createMenuItem("print window dims & System.gc()", 'w', helpMenu);
			//debugCursorItem = createMenuItem("CURSOR", 'u', helpMenu);
			helpMenu.add( action_RCTempTimeGraph );
			debugTestEnginesItem = createMenuItem("TEST ENGINES", 'g', helpMenu);
			debugJT2Item = createMenuItem("JT2", '2', helpMenu);
			debugDgraphWriteItem = createMenuItem("Write Dgraph", 'p', helpMenu );
			//debugReadOnly = createMenuItem("Read Only", 'y', helpMenu );
			debugAnimation = createMenuItem("Animation", 'a', helpMenu );
			debugDecisionTreeOptimization = createMenuItem( "Print DT stats to console", 't', helpMenu );
			debugVariance                 = createMenuItem( "Variance (Health)",         'v', helpMenu );
			debugScreenshot = createMenuItem( "Screen shot", 'e', helpMenu );
		}

		try{
			String strue = "true";
			Object value = null;
			setVisibleStatusBar(            ((value = mySamiamPreferences.getProperty( STR_KEY_VISIBLE_STATUSBAR  )) == null) || strue.equalsIgnoreCase( value.toString() ) );
			setVisibleToolbarMain(          ((value = mySamiamPreferences.getProperty( STR_KEY_VISIBLE_TOOLBAR    )) == null) || strue.equalsIgnoreCase( value.toString() ) );
			setVisibleToolbarInstantiation( ((value = mySamiamPreferences.getProperty( STR_KEY_VISIBLE_ICBTOOLBAR )) == null) || strue.equalsIgnoreCase( value.toString() ) );
		}catch( Throwable thrown ){
			System.err.println( "warning: UI.initJMenuBar() caught " + thrown );
		}
	}

	protected JMenuBar menuBar;
	protected JMenu fileMenu, queryMenu, toolsMenu, editMenu, modeMenu, preferencesMenu, windowMenu, helpMenu, viewMenu, instantiationMenu, codeToolMenu;

	public static final int INT_INDEX_FIRST_RECENT_DOCUMENT_ITEM = (int)7;

	protected JMenuItem	debugDFSItem,
				debugCPTEditItem,
				//debugSubmodelItem,
				debugCloneItem,
				debugUserModeItem,
				debugRoundComplementItem,
				//debugRecursiveConditioningItem,
				debugInferenceEngineCacheHitsItem,
				debugRepaintEvidenceTreeItem,
				debugWindowDimensionsItem,
				debugCursorItem,
				debugTestEnginesItem,
				debugJT2Item,
				debugDgraphWriteItem,
				debugReadOnly,
				debugAnimation,
				debugDecisionTreeOptimization,
				debugVariance,
				debugScreenshot;

	// Submenu (of tools menu) showMonitors and menu items
	protected JMenu menuMonitors, menuAutoArrange;
	//protected JCheckBoxMenuItem myShowTargetMonitorsItem, myShowObservationMonitorsItem;
	protected JCheckBoxMenuItem myShowStatusBarItem, myShowToolBarItem, myShowInstantiationToolBarItem;
	// Menu items for mode menu
	private JCheckBoxMenuItem myQueryModeCheckBox, myEditModeCheckBox, myReadOnlyCheckBox, myAnimateCheckBox, myHideCheckBox, myHHECheckBox, myEvidenceFrozenCheckBox, myLocationFrozenCheckBox, myShowEdgesCheckBox, myShowRecoverablesCheckBox;
	// Zoom submenu
	protected JMenu menuZoom;

	/** @since 102902 */
	public void initStatusBar()
	{
		myStatusBar = new StatusBar();

		PreferenceGroup globalPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.PkgDspNme );
		myStatusBar.setPreferences( mySamiamPreferences );

		Preference prefPrE = mySamiamPreferences.getMappedPreference( SamiamPreferences.autoCalculatePrE );
		myFlagDefeatPrE = !((Boolean) prefPrE.getValue()).booleanValue();
	}

	/** @since 120403 */
	public StatusBar getStatusBar()
	{
		return myStatusBar;
	}

	public static final String
	  STR_PR_OF_E_PREFIX = "<html><nobr>&nbsp;Pr&nbsp;(e)&nbsp;",
	  STR_PR_OF_E__INFIX = "&nbsp;",
	  STR_PR_OF_E_ERROR  = "<font color=\"#cc0000\">ERROR",
	  STR_MSG_PRE        = "calculating Pr(e)...";
	protected boolean myFlagShowPrE = false, myFlagDefeatPrE = false;

	/** @since 20021029 */
	public void newPrE( NetworkInternalFrame nif )
	{
		//System.out.println( "UI.newPrE()" );

		synchronized( mySynchonizationPrE ){

		clearPrE();
		myNIFnewPrE = null;

		if( myFlagShowPrE && !myFlagDefeatPrE && getActiveHuginNetInternalFrame() == nif )
		{
			String strLastCalculated = nif.getLastCalculatedPrE();
			if( strLastCalculated == null )
			{
				myStatusBar.pushText( STR_MSG_PRE, StatusBar.WEST );

				InferenceEngine ie = nif.getInferenceEngine();
				if( (ie != null) && ie.probabilitySupported() )
				{
					if( ie.canonical() instanceof RCInferenceEngine ){
						Settings settings = edu.ucla.belief.recursiveconditioning.RCEngineGenerator.getSettings( (PropertySuperintendent) nif.getBeliefNetwork() );
						RCProgressMonitor.probability( this, settings, (RCInferenceEngine) ie.canonical() );
						myNIFnewPrE = nif;
						return;
					}
					else{
						double value = (double)-1;
						Throwable throwable = null;
						try{
							value = ie.probability();
						}catch( RuntimeException runtimeexception ){
							value = (double)-1;
							throwable = runtimeexception;
							System.err.println( "Warning: failed to calculate pr(e)" );
							Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
							runtimeexception.printStackTrace();
						}
						String strValue = displayPrE( makePrEString( value, ie.probabilityDisplayOperatorUnicode(), throwable ) );
						if( value >= (double)0 ){
							nif.setLastCalculatedPrE( strValue );
							ie.printInfoPropagation( nif.console );
						}
					}
				}

				myStatusBar.popText( STR_MSG_PRE, StatusBar.WEST );
			}
			else displayPrE( strLastCalculated );
		}

		}
	}

	//protected Thread myTheadRunningPrE;
	protected RC myRCRunningPrE;
	protected RCProgressMonitor myRCProgressMonitorRunningPrE;
	protected NetworkInternalFrame myNIFnewPrE;

	/** @since 20030404 */
	public void setRunningPrE( RC rc, RCProgressMonitor monitor )
	{
		synchronized( mySynchonizationPrE ){

		if( myRCProgressMonitorRunningPrE != null )
		{
			new PMCloser( myRCProgressMonitorRunningPrE ).start();

		}

		//myTheadRunningPrE = thread;
		myRCRunningPrE = rc;
		myRCProgressMonitorRunningPrE = monitor;

		}
	}

	/** @since 20030403 */
	public String makePrEString( double value, char probabilityDisplayOperatorUnicode, Throwable throwable ){
		StringBuffer buff = new StringBuffer( 0x40 );
		buff.append( STR_PR_OF_E_PREFIX );
		buff.append( probabilityDisplayOperatorUnicode );
		buff.append( STR_PR_OF_E__INFIX );
		String post = STR_PR_OF_E_ERROR;
		if( value >= (double)0 ) post = Double.toString( value );
		buff.append( post );
		if( throwable != null ){ buff.append( ": " ).append( Util.getSimpleName( throwable.getClass() ) ); }
		return buff.toString();
	}

	protected String displayPrE( String strValue ){
		myStatusBar.setText( strValue, StatusBar.EAST );
		return strValue;
	}

	/** interface RC.RecCondThreadListener
 		@since 20030404 */
	public void rcFinished( double result, long time_ns )
	{
		synchronized( mySynchonizationPrE ){

		if( result == RC.DOUBLE_OUTOFMEMORY )
		{
			if( CachePreviewDialog.theVisibleCachePreviewDialog != null ) CachePreviewDialog.theVisibleCachePreviewDialog.dispose();
			showErrorDialog( "The RC Pr(e) computation ran out of memory." );
		}
		else if( myNIFnewPrE != null )
		{
			char probabilityDisplayOperatorUnicode = '=';
			InferenceEngine ie = myNIFnewPrE.getInferenceEngine();
			if( ie != null ){ probabilityDisplayOperatorUnicode = ie.probabilityDisplayOperatorUnicode(); }
			String strValue = makePrEString( result, probabilityDisplayOperatorUnicode, (Throwable)null );
			myNIFnewPrE.setLastCalculatedPrE( strValue );
			if( getActiveHuginNetInternalFrame() == myNIFnewPrE ){ displayPrE( strValue ); }
			if( ie != null ){ ie.printInfoPropagation( myNIFnewPrE.console ); }
		}
		myNIFnewPrE = null;

		//setRunningPrE( null, null, null );
		setRunningPrE( null, null );

		}
	}

	public void rcFinishedMPE( double result, Map instantiation, long time_ms){}

	/** interface RC.RecCondThreadListener
		@since 20030610 */
	public void rcComputationError( String msg )
	{
		if( CachePreviewDialog.theVisibleCachePreviewDialog != null ) CachePreviewDialog.theVisibleCachePreviewDialog.dispose();
		showErrorDialog( msg );
		myNIFnewPrE = null;
		setRunningPrE( null, null );
	}

	/** @since 20021029 */
	public void clearPrE()
	{
		synchronized( mySynchonizationPrE ){

		myStatusBar.setText( "", StatusBar.EAST );

		}
	}

	/** @since 20030218 */
	public UI displayCompilationStatus( NetworkInternalFrame nif ){
		String          newText  = "<html><nobr>&nbsp;";
		InferenceEngine ie       = nif.getInferenceEngine();
		if(             ie      != null ){
			String      status   = ie.compilationStatus( nif.getBeliefNetwork() );
			if(         status  != null ){ newText += status; }
		}
		myStatusBar.setText( newText, StatusBar.WEST );
		return this;
	}

	/** @since 20030218 */
	public void clearCacheFactor()
	{
		this.clearCompilationStatus();
	}

	/** @since 20050623 */
	public void clearCompilationStatus(){
		myStatusBar.setText( "", StatusBar.WEST );
	}

	/** @since 20021029 */
	public void setNetworkPasteEnabled( boolean enabled, SamiamUserMode mode, NetworkInternalFrame nif )
	{
		action_PASTE.setEnabled(        enabled &&        action_PASTE.decideEnabled( mode, nif ) );
		action_PASTESPECIAL.setEnabled( enabled && action_PASTESPECIAL.decideEnabled( mode, nif ) );
	}

	/** @since 20020612 */
	private void setOpenNetDependantItemsEnabled( boolean enabled )
	{
		menuZoom.setEnabled( enabled );
		editMenu.setEnabled( enabled );
	}

	private JMenu createMenu(String name, char mnemonic)
	{
		JMenu menu = new ModalMenu( name, UI.this );//new JMenu(name);
		menu.setMnemonic(mnemonic);
		menuBar.add(menu);
		return menu;
	}

	private JMenuItem createMenuItem( String name, char mnemonic, JMenu menu )
	{
		JMenuItem item = new JMenuItem(name);
		item.setMnemonic(mnemonic);
		item.addActionListener(this);
		menu.add(item);
		return item;
	}

	/** @since 102202 */
	/*
	private JCheckBoxMenuItem createCheckBoxMenuItem( String name, char mnemonic, JMenu menu, Action action ){
		JCheckBoxMenuItem item = new JCheckBoxMenuItem( action );
		item.setText( name );
		item.setMnemonic(mnemonic);
		menu.add(item);
		return item;
	}*/

	/** @since 102202 */
	private JCheckBoxMenuItem createCheckBoxMenuItem( JMenu menu, Action action )
	{
		JCheckBoxMenuItem item = new JCheckBoxMenuItem( action );
		menu.add(item);
		return item;
	}

	public SamiamPreferences getPackageOptions()
	{
		return mySamiamPreferences;
	}

	public SamiamPreferences getSamiamPreferences()
	{
		return mySamiamPreferences;
	}

	/** @since 080404 */
	public void addPreferenceMediator( PreferenceMediator med ){
		if( myPreferenceMediators == null ) myPreferenceMediators = new LinkedList();
		if( !myPreferenceMediators.contains( med ) ) myPreferenceMediators.add( med );
	}

	/** @since 041405 */
	public Collection getPreferenceMediators(){
		if( (myPreferenceMediators == null) || myPreferenceMediators.isEmpty() ) return Collections.EMPTY_SET;
		else return myPreferenceMediators;
	}

	/** Change the package options. */
	public void changePackageOptions( SamiamPreferences pref ){
		setPkgDspOptLookAndFeel( false );//keith
		setShowPrE();
		setMaxRecentDocuments();
		setInferenceOptions( pref.getPreferenceGroup( SamiamPreferences.STR_KEY_GROUP_INFERENCE ) );
		PreferenceGroup globalPrefs = pref.getPreferenceGroup( SamiamPreferences.PkgDspNme );
		myStatusBar.updatePreferences( pref );
		NodeLabel.updatePreferencesStatic( pref );

		for( Iterator it = getPreferenceMediators().iterator(); it.hasNext(); ){
			((PreferenceMediator) it.next()).updatePreferences( pref );
		}

		for(Iterator it = myListInternalFrames.iterator(); it.hasNext(); )
		{
			((NetworkInternalFrame) it.next()).changePackageOptions( pref );
		}

		UI.this.firePreferenceListeners();
	}

	/** @since 20070402 */
	public boolean addPreferenceListener( PreferenceListener pl ){
		try{
			if( myPreferenceListeners == null ) myPreferenceListeners = new WeakLinkedList();
			else if( myPreferenceListeners.contains( pl ) ) return false;
			return myPreferenceListeners.add( pl );
		}catch( Exception exception ){
			System.err.println( "warning: UI.addPreferenceListener() caught " + exception );
		}
		return false;
	}
	/** @since 20070402 */
	public boolean removePreferenceListener( PreferenceListener pl ){
		try{
			if( myPreferenceListeners == null ) return false;
			else return myPreferenceListeners.remove( pl );
		}catch( Exception exception ){
			System.err.println( "warning: UI.removePreferenceListener() caught " + exception );
		}
		return false;
	}
	/** @since 20070402 */
	private int firePreferenceListeners(){
		int count = 0;
		try{
			if( myPreferenceListeners == null ) return count;

			PreferenceListener pl = null;
			for( Iterator it = myPreferenceListeners.iterator(); it.hasNext(); ){
				if( (pl = (PreferenceListener) it.next()) == null ) it.remove();
				else{
					pl.updatePreferences();
					count++;
				}
			}
		}catch( Exception exception ){
			System.err.println( "warning: UI.firePreferenceListeners() caught " + exception );
		}
		return count;
	}
	private Collection myPreferenceListeners;

	/** @since 20050811 */
	protected void setInferenceOptions( PreferenceGroup groupInference ){
		if( !groupInference.isRecentlyCommittedValue() ) return;

		Preference prefRepetitions = mySamiamPreferences.getMappedPreference( SamiamPreferences.inferenceMinFillRepetitions );
		int reps = ((Integer)prefRepetitions.getValue()).intValue();
		int currentValue = EliminationHeuristic.MIN_FILL.getRepetitions();
		if( reps != currentValue ){
			EliminationHeuristic.MIN_FILL.setRepetitions( reps );
		}

		Preference prefSeed = mySamiamPreferences.getMappedPreference( SamiamPreferences.inferenceMinFillSeed );
		if( prefSeed.isRecentlyCommittedValue() ){
			EliminationHeuristic.MIN_FILL.setSeed( new Random( ((Number)prefSeed.getValue()).longValue() ) );
		}
	}

	/** @since 102902 */
	protected void setShowPrE()
	{
		Preference prefPrE = mySamiamPreferences.getMappedPreference( SamiamPreferences.autoCalculatePrE );
		boolean newFlagDefeat = !((Boolean) prefPrE.getValue()).booleanValue();
		if( myFlagDefeatPrE != newFlagDefeat )
		{
			myFlagDefeatPrE = newFlagDefeat;
			if( myFlagDefeatPrE ) clearPrE();
			else newPrE( getActiveHuginNetInternalFrame() );
		}
	}

	/** @since 102902 */
	protected void setMaxRecentDocuments()
	{
		Preference prefmaxRecentDocuments = mySamiamPreferences.getMappedPreference( SamiamPreferences.maxRecentDocuments );
		int newval = ((Integer) prefmaxRecentDocuments.getValue()).intValue();
		if( mySamiamPreferences.setMaxRecentDocuments( newval ) ) updateRecentDocuments();
	}

	/** Return the active NetworkInternalFrame, null if none active. */
	public NetworkInternalFrame getActiveHuginNetInternalFrame()
	{
		if( myListInternalFrames.isEmpty() ) return null;
		return (NetworkInternalFrame) myListInternalFrames.getFirst();
	}

	/** Bring the JInternalFrame to the front. */
	public void toFront(JInternalFrame frame)
	{
		if( myListInternalFrames.contains( frame ) ){
			myListInternalFrames.remove(frame);
			myListInternalFrames.addFirst(frame);
		}
		select(frame);
	}

	/** Put the JInternalFrame to the back. */
	public void toBack(JInternalFrame frame)
	{
		myListInternalFrames.remove(frame);
		myListInternalFrames.addLast(frame);
		select(getActiveHuginNetInternalFrame());
	}

	/** select the JInternalFrame
		@since 20020617 */
	public void select( JInternalFrame frame ){
		synchronized( mySynchFrame ){
			if(   myFrameEnTrainDeSelecting == frame ){ return; }
			else{ myFrameEnTrainDeSelecting =  frame; }

			try{
				frame.setIcon(    false );
				frame.setSelected( true );
				if( frame instanceof NetworkInternalFrame ){
					NetworkInternalFrame nif = (NetworkInternalFrame) frame;
					setSamiamUserMode(   nif.getSamiamUserMode() );
					InferenceEngine ie = nif.getInferenceEngine();
					if( ie != null ){
						Dynamator dyn = ie.getDynamator();
						if( dyn != null ){ setDynamator( dyn ); }
					}else{
						try{
							if( (myDynamatorListeners != null) && (! myDynamatorListeners.isEmpty()) ){
								DisplayableBeliefNetwork dbn  = nif.getBeliefNetwork();
								ArrayList                dibs = new ArrayList( myDynamatorListeners.size() );
								DynamatorListener        vcl;
								Dynamator                dibber;
								for( Iterator it = myDynamatorListeners.iterator(); it.hasNext(); ){
									if( (vcl = (DynamatorListener) it.next()) == null ){ it.remove(); }
									else if( (dibber = vcl.dibs( (BeliefNetwork) dbn, (PropertySuperintendent) dbn )) != null ){ dibs.add( dibber ); }
								}
								if( ! dibs.isEmpty() ){
									setDynamator( (Dynamator) dibs.get(0) );
									if( dibs.size() > 1 ){
										StringBuffer buff = new StringBuffer(0x20 * dibs.size());
										for( Iterator it = dibs.iterator(); it.hasNext(); ){
											buff.append( it.next().toString() ).append( ", " );
										}
										buff.setLength( buff.length() - 2 );
										System.err.println( "warning: multiple dynamators called dibs" );
										JOptionPane.showMessageDialog( UI.this, "Algorithm conflict between { " +buff.toString()+ " }.", "algorithm conflict", JOptionPane.ERROR_MESSAGE );
									}
								}
							}
						}catch( Throwable thrown ){
							System.err.println( "warning: UI.select() caught " + thrown );
						}
					}
				}
			}catch( java.beans.PropertyVetoException pve ){
				System.err.println( "Warning: PropertyVetoException while selecting NetworkInternalFrame" );
			}finally{
				myFrameEnTrainDeSelecting = null;
			}
		}
	}

	protected Object
	  myFrameEnTrainDeSelecting,
	  mySynchonization                       = new Object(),
	  mySynchFrame                           = new Object(),
	  mySynchonizationPrE                    = new Object(),
	  mySynchOpenFile                        = new Object();
	private   static       int
	  INT_THREAD_COUNTER                     = 0;
	public    static final long
	  LONG_TIMEOUT_SETSAMIAMUSERMODE         = 0x800;
	private   LinkedList myThreadsSetMode    = new LinkedList();

	/** @since 20080317 */
	public boolean joinThreadsSetMode() throws InterruptedException{
		Thread[] threads = null;
		while( ! myThreadsSetMode.isEmpty() ){
			synchronized( myThreadsSetMode ){ threads = (Thread[]) myThreadsSetMode.toArray( new Thread[ myThreadsSetMode.size() ] ); }
			for( int i=0; i<threads.length; i++ ){
				if( threads[i] != null ){
					threads[i].join( 0x1000 );
					if( threads[i].isAlive() ){ return false; }
					else{ popThreadsSetMode( threads[i] ); }
				}
			}
		}
		return true;
	}

	/** @since 20080317 */
	private Thread peekThreadsSetMode(){
		synchronized( myThreadsSetMode ){
			if( myThreadsSetMode.isEmpty() ){ return null; }
			else{ return (Thread) myThreadsSetMode.getFirst(); }
		}
	}

	/** @since 20080317 */
	private Thread popThreadsSetMode( Thread dead ){
		synchronized( myThreadsSetMode ){
			if( ! dead.isAlive() ){ myThreadsSetMode.remove( dead ); }
		}
		return dead;
	}

	/** @since 20020617 */
	public void setSamiamUserMode( SamiamUserMode mode ){
		UI.this.setModeAsynchronous( mode );
	}

	/** @since 20060720 */
	public Thread setModeAsynchronous( final SamiamUserMode samiamusermode ){
		int changed = ModalAction.setEnabledAllRegistered( false );//safety
		//System.out.println( "safety disabled " + changed + " ModalActions" );
		Runnable runnable = new Runnable(){
			public void run(){
				try{
					UI.this.setModeSynchronous( later );
				}catch( InterruptedException interruptedexception ){
					if( Util.DEBUG_VERBOSE ){ System.err.println( "UI.setModeSynchronous() threw " + interruptedexception ); }
				}
			}
			private SamiamUserMode later = new SamiamUserMode( samiamusermode );
		};
		synchronized( mySynchonization ){
			Thread ret = new Thread( UI.this.myThreadGroup.getParent(), runnable, "UI setSamiamUserMode " + Integer.toString( INT_THREAD_COUNTER++ ) );
			myThreadsSetMode.add( ret );
			ret.start();
			return ret;
		}
	}

	/** @since 20060720 */
	public boolean setModeSynchronous( SamiamUserMode mode ) throws InterruptedException
	{
		Thread current = Thread.currentThread(), next;
		while( (next = peekThreadsSetMode()) != current ){
			next.interrupt();
			next.join( 0x1000 );
			popThreadsSetMode( next );
		}

		synchronized( mySynchonization ){

	  //System.out.println( "\n\n*****************************" );
	  //System.out.println( "UI.setModeSynchronous( "+mode+" )" );
	  //(new Throwable()).printStackTrace();

		NetworkInternalFrame nif      = null;
		try{
			nif                       = getActiveHuginNetInternalFrame();
			ThreadGroup group         = (nif == null) ? null : nif.getThreadGroup();
			if( group != null ){
				int      count        = -1, capacity = -1, useralives = 1;
				Thread[] threads      = null;
				long     begin        = System.currentTimeMillis();
				while(   useralives > 0 ){
					if( (System.currentTimeMillis() - begin) > LONG_TIMEOUT_SETSAMIAMUSERMODE ){
						System.err.println( "warning! " + current.getName() + " joining timed out ("+LONG_TIMEOUT_SETSAMIAMUSERMODE+"ms), proceeding to set enabled state anyway" );
						if( (threads == null) || (threads.length < (capacity = (count = group.activeCount()) + 4)) ){ threads = new Thread[capacity]; }
						group.enumerate( threads );
						for( int i=0; i<threads.length; i++ ){
							if( (threads[i] != null) && (! threads[i].isDaemon()) ){
								threads[i].stop(  );
							}
						}
						group.list();
						break;
					}
					capacity          = (count        = group.activeCount()) + 4;
					if( (threads == null) || (threads.length < capacity) ){
						threads       = new Thread[capacity]; }
					group.enumerate( threads );
					useralives = 0;
					for( int i=0; i<threads.length; i++ ){
						if( (threads[i] != null) && (! threads[i].isDaemon()) ){
							threads[i].join( 0x400 );
							if( threads[i].isAlive() ){ ++useralives; }
						}
					}
				}
			  //System.out.println( "finished join in " +(System.currentTimeMillis()-begin)+ "ms" );
			}
		}catch( InterruptedException interruptedexception ){
			Thread.currentThread().interrupt();
			throw interruptedexception;
		}catch( Throwable throwable ){
			System.err.println( "warning! " + current.getName() + " caught: " + throwable );
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				throwable.printStackTrace();
			}
			return false;
		}

		boolean openfilesetting = mode.contains( SamiamUserMode.OPENFILE );
		boolean querysetting = mode.contains( SamiamUserMode.QUERY );
		//boolean editsetting = mode.contains( SamiamUserMode.EDIT );
		//boolean dslsetting = mode.contains( SamiamUserMode.DSLFILE );
		boolean compilingsetting = mode.contains( SamiamUserMode.COMPILING );
		//boolean flagPartialEngine = ( querysetting && nif != null && nif.getPartialDerivativeEngine() != null );
		//boolean flagMAPEnabled = (flagPartialEngine && (nif.getCanonicalInferenceEngine() instanceof JoinTreeInferenceEngineImpl));
		//boolean flagPRECOMPUTEEnabled = ((nif != null) && (nif.getCanonicalInferenceEngine() instanceof RCInferenceEngine));
		boolean         readonlysetting = mode.contains( SamiamUserMode.READONLY         );
		boolean          animatesetting = mode.contains( SamiamUserMode.ANIMATE          );
		boolean             hidesetting = mode.contains( SamiamUserMode.HIDE             );
		boolean              hhesetting = mode.contains( SamiamUserMode.HIDEHIDDENEDGES  );
		boolean        showedgessetting = mode.contains( SamiamUserMode.SHOWEDGES        );
		boolean showrecoverablessetting = mode.contains( SamiamUserMode.SHOWRECOVERABLES );
		boolean   evidencefrozensetting = mode.contains( SamiamUserMode.EVIDENCEFROZEN   );
		boolean   locationfrozensetting = mode.contains( SamiamUserMode.LOCATIONSFROZEN  );

		if( current.isInterrupted() ){ return false; }

		setOpenNetDependantItemsEnabled( openfilesetting );

		if( current.isInterrupted() ){ return false; }

		if( querysetting ){ displayCompilationStatus( nif ); }
		else{                 clearCompilationStatus(     ); }

		if( current.isInterrupted() ){ return false; }

		queryMenu.setEnabled( querysetting );

		if( myFlagShowPrE = querysetting ){   newPrE( nif ); }
		else{                               clearPrE(     ); }

		if( current.isInterrupted() ){ return false; }

		ModalAction.setModeAllRegistered( mode, nif );

		if( current.isInterrupted() ){ return false; }

	  //modeMenu.setEnabled( openfilesetting && !compilingsetting );
		if( myReadOnlyCheckBox         != null ){ myReadOnlyCheckBox         .setSelected(         readonlysetting ); }
		if( myAnimateCheckBox          != null ){ myAnimateCheckBox          .setSelected(          animatesetting ); }
		if( myHideCheckBox             != null ){ myHideCheckBox             .setSelected(             hidesetting ); }
		if( myHHECheckBox              != null ){ myHHECheckBox              .setSelected(              hhesetting ); }
		if( myShowEdgesCheckBox        != null ){ myShowEdgesCheckBox        .setSelected(        showedgessetting ); }
		if( myShowRecoverablesCheckBox != null ){ myShowRecoverablesCheckBox .setSelected( showrecoverablessetting ); }
		if( myEvidenceFrozenCheckBox   != null ){ myEvidenceFrozenCheckBox   .setSelected(   evidencefrozensetting ); }
		if( myLocationFrozenCheckBox   != null ){ myLocationFrozenCheckBox   .setSelected(   locationfrozensetting ); }

		if( current.isInterrupted() ){ return false; }

		myToolBar.setSamiamUserMode( mode );

		if( current.isInterrupted() ){ return false; }

		repaintMenusAndToolbars();

		return ! Thread.interrupted();
		}
	}

	/** @since 20040115 */
	public void quickModeWarning( SamiamUserMode mode, NetworkInternalFrame nif )
	{
		synchronized( mySynchonization ){

		boolean openfilesetting = mode.contains( SamiamUserMode.OPENFILE );
		boolean querysetting = mode.contains( SamiamUserMode.QUERY );
		boolean compilingsetting = mode.contains( SamiamUserMode.COMPILING );

		setOpenNetDependantItemsEnabled( openfilesetting );
		queryMenu.setEnabled( querysetting );
		ModalAction.setModeAllRegistered( mode, nif );
	  //modeMenu.setEnabled( openfilesetting && !compilingsetting );

		repaintMenusAndToolbars();

		}
	}

	/**
		First new method of '04!

		@author Keith Cascio
		@since 010704
	*/
	private void repaintMenusAndToolbars()
	{
		if( menuBar != null ) menuBar.repaint();
		if( myToolBar != null ) myToolBar.repaint();
		if( myInstantiationClipboardToolBar != null ) myInstantiationClipboardToolBar.repaint();
	}

	/** @since 061802 */
	public void toggleSamiamUserMode()
	{
		NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
		SamiamUserMode mode = nif.getSamiamUserMode();
		boolean querysetting = mode.contains( SamiamUserMode.QUERY );

		mode.setModeEnabled( SamiamUserMode.QUERY, !querysetting );
		mode.setModeEnabled( SamiamUserMode.EDIT, querysetting );

		nif.setSamiamUserMode( mode );
		//setSamiamUserMode( mode );//now called by NetworkInternalFrame.setSamiamUserMode()
	}

	/** @since 032904 */
	public void toggleReadOnly()
	{
		toggleMode( SamiamUserMode.READONLY );
	}

	/** @since 072904 */
	public void toggleMode( SamiamUserMode mask )
	{
		NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
		if( nif == null ) return;

		SamiamUserMode mode = nif.getSamiamUserMode();
		boolean maskSetting = mode.contains( mask );
		mode.setModeEnabled( mask, !maskSetting );
		nif.setSamiamUserMode( mode );
	}

	/** Restore the JInternalFrame. */
	public static void restore(JInternalFrame frame)
	{
		try{
			frame.setIcon(false);
		}catch (java.beans.PropertyVetoException pve){
			System.err.println( "Warning, UI.restore() caught " + pve );
		}
	}

	/** Minimize the JInternalFrame. */
	public static void minimize(JInternalFrame frame)
	{
		try{
			frame.setIcon(true);
		}catch (java.beans.PropertyVetoException pve){
			System.err.println( "Warning, UI.minimize() caught " + pve );
		}
	}

	/** Maximize the JInternalFrame. */
	public static void maximize(JInternalFrame frame)
	{
		try{
			frame.setMaximum(true);
		}catch (java.beans.PropertyVetoException pve){
			System.err.println( "Warning, UI.maximize() caught " + pve );
		}
	}

	/** Open a new Hugin Net file. Returns true. */
	public boolean newFile()
	{
		myToolBar.enable();
		try{
			NetworkInternalFrame hnInternalFrame = new NetworkInternalFrame( this );
		}catch( Exception e ){
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
			showErrorDialog( "Failed to create new Hugin network." );
		}
		return true;
	}

	/**
		@author Keith Cascio
		@since 030802
	*/
	public void setLookAndFeel( String classname )
	{
		//System.out.println( "UI.setLookAndFeel("+classname+")" );
		String currentLFClassName = UIManager.getLookAndFeel().getClass().getName();
		//System.out.println( "\tcurrent: \"" +currentLFClassName+ "\"" );

		if( !classname.equals( currentLFClassName ) )
		{
			try
			{
				UIManager.setLookAndFeel( classname );
				UIManager.put( "InternalFrame.useTaskBar", Boolean.FALSE );
				for( Iterator it = myListInternalFrames.iterator(); it.hasNext(); )
				{
					((NetworkInternalFrame)(it.next())).updateComponentTreeUI();
				}

				SwingUtilities.updateComponentTreeUI( this );
			}
			catch( Exception e )
			{
				System.err.println( "There was an error setting the look and feel:" );
				if( Util.DEBUG_VERBOSE )
				{
					Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
					e.printStackTrace();
				}
			}
		}
	}

	/** @since 20020308 */
	public void setPkgDspOptLookAndFeel( boolean force ){
		if( mySamiamPreferences == null ){ return; }
		Preference prefLookAndFeel = mySamiamPreferences.getMappedPreference( SamiamPreferences.STR_LOOKANDFEEL_CLASSNAME );
		if( force || prefLookAndFeel.isRecentlyCommittedValue() ) setLookAndFeel( (String) prefLookAndFeel.getValue() );
	}

	/**
		@author Keith Cascio
		@since 052102
	*/
	private void rememberParentDirectory( File f )
	{
		File parent = f.getParentFile();
		if( parent != null ) mySamiamPreferences.defaultDirectory = parent;
	}

	/**
		@author Keith Cascio
		@since 120502
	*/
	protected void addRecentDocument( File f )
	{
		if( mySamiamPreferences.addRecentDocument( f ) ) updateRecentDocuments();
	}

	/**
		@author Keith Cascio
		@since 120502
	*/
	protected void updateRecentDocuments()
	{
		for( Iterator it = myRecentDocumentMenuItems.iterator(); it.hasNext(); ) fileMenu.remove( (JMenuItem) it.next() );
		myRecentDocumentMenuItems.clear();

		String path = null;
		String displayname = null;
		RecentDocumentAction action = null;
		int index = Math.min( mySamiamPreferences.myListRecentDocuments.size(), mySamiamPreferences.getMaxRecentDocuments() );
		int numToSkip = mySamiamPreferences.myListRecentDocuments.size() - mySamiamPreferences.getMaxRecentDocuments();
		for( Iterator it = mySamiamPreferences.myListRecentDocuments.iterator(); it.hasNext(); )
		{
			path = it.next().toString();

			if( numToSkip > 0 ) --numToSkip;
			else
			{
				displayname = RecentDocumentAction.makeDisplayName( path, index-- );
				action = new RecentDocumentAction( displayname, path, this );
				myRecentDocumentMenuItems.add( fileMenu.insert( action, INT_INDEX_FIRST_RECENT_DOCUMENT_ITEM ) );
			}
		}
	}

	protected Collection myRecentDocumentMenuItems = new ArrayList( (int)8 );

	/** @since 20020523 */
	private void rememberFileExtension( File f )
	{
		if( f != null && !f.isDirectory() )
		{
			String name = f.getName();
			int dotIndex = name.lastIndexOf( '.' );
			String extension = name.substring( dotIndex );
			mySamiamPreferences.lastEncounteredFileExtension = extension;
		}
	}

	/** @since 200606013 */
	private boolean isOpening( String name ){
		return callbacksForName( name ) != null;
	}

	/** @since 200606013 */
	private LoadCallbacks callbacksForName( String name ){
		if( myLoadCallbacks == null ) return null;

		LoadCallbacks loadcallbacks;
		for( Iterator it = myLoadCallbacks.iterator(); it.hasNext(); ){
			loadcallbacks = (LoadCallbacks) it.next();
			if(      loadcallbacks == null ) it.remove();
			else if( loadcallbacks.alive && name.equals( loadcallbacks.myName ) ) return loadcallbacks;
		}
		return null;
	}

	private WeakLinkedList myLoadCallbacks = new WeakLinkedList();

	/** @author keith cascio
		@since 20060519 */
	private class LoadCallbacks implements NetworkIO.BeliefNetworkIOListener
	{
		private LoadCallbacks( String nameModel ){
			LoadCallbacks.this.myName = nameModel;
			UI.this.myLoadCallbacks.add( this );
		}

		//public void finalize(){
		//	System.out.println( this + ".finalize()" );
		//}

		/** @since 20020521 */
		public void handleNewBeliefNetwork( BeliefNetwork newBN, File selectedFile )
		{
			//long start = JVMTI.getCurrentThreadCpuTimeUnsafe();

			String path = null;
			try{
			try{
				if( selectedFile != null ) path = selectedFile.getPath();
				else if( myStreamInputName != null ){
					path = myStreamInputName;
					myStreamInputName = null;
				}
				else path = "null file name";
				myNetworkInternalFrame = new NetworkInternalFrame( newBN, UI.this, path, LoadCallbacks.this.myLoadModelTask, LoadCallbacks.this.myThreadGroup );
				if( Thread.currentThread().isInterrupted() ) return;
			}
			catch( Exception e ){
				setDefaultCursor();
				if( Util.DEBUG_VERBOSE )
				{
					Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
					e.printStackTrace();
				}

				showErrorDialog( path +
					" not supported by "+STR_SAMIAM_ACRONYM+".\n"+ e.getMessage() +"\n(See stderr for description.)");
				return;
			}

			//long mid0 = JVMTI.getCurrentThreadCpuTimeUnsafe();

			/*  @since 20051031 SMILE mode now handled
				 in NetworkInternalFrame constructor, where it belongs */
			//boolean flagSMILEFile = false;

			if( selectedFile != null ){
				//final FileType type = FileType.getTypeForFile( selectedFile );
				//if( type.isSMILEType() ) flagSMILEFile = true;

				rememberParentDirectory( selectedFile );
				rememberFileExtension( selectedFile );
				addRecentDocument( selectedFile );
			}

			//if( newBN instanceof edu.ucla.belief.io.dsl.GenieNet ) flagSMILEFile = true;
			//if( flagSMILEFile ){
			//	SamiamUserMode mod = myNetworkInternalFrame.getSamiamUserMode();
			//	mod.setModeEnabled( SamiamUserMode.SMILEFILE, true );
			//	myNetworkInternalFrame.setSamiamUserMode( mod );
			//}

			ensureSystemReadyFor( newBN );

			((DisplayableBeliefNetworkImpl)myNetworkInternalFrame.getBeliefNetwork()).investigateCycles();

			//decidePromptAutoArrange( myNetworkInternalFrame );

			myToolBar.enable();

			//long end = JVMTI.getCurrentThreadCpuTimeUnsafe();
			//long first = mid0 - start;
			//long last  = end - mid0;
			//double total = (double) (end - start);

			//double firstFrac = ((double)first) / total;
			//double lastFrac = ((double)last) / total;

			//System.out.println( "UI.handleNewBeliefNetwork()\n    new NetworkInternalFrame: " + NetworkIO.FORMAT_PROFILE_PERCENT.format(firstFrac) + " (" + NetworkIO.formatTime(first) + "),\n    other                   : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(lastFrac) + " (" + NetworkIO.formatTime(last) + ")" );

			}finally{
				LoadCallbacks.this.alive = false;
				setDefaultCursor();
				if( LoadCallbacks.this.myLoadModelTask != null ) LoadCallbacks.this.myLoadModelTask.setFinished( true );
			}
		}

		/** @since 20020521 */
		public void handleBeliefNetworkIOError( String msg )
		{
			LoadCallbacks.this.alive = false;
			setDefaultCursor();
			if( LoadCallbacks.this.myLoadModelTask != null ) LoadCallbacks.this.myLoadModelTask.setFinished( true );
			showErrorDialog( msg );
		}

		/** @since 20060519 */
		public void handleProgress( ProgressMonitorable readTask, Estimate estimate ){
			myReadTask      = readTask;

			//String[] notes = new String[] { "Graph.replaceVertex...", "NetworkDisplay.makeNodes..." };
			//String[] notes = new String[] { "creating displayable belief network...", "drawing the belief network..." };
			//String[] notes = HashDirectedGraph.ARRAY_PROFILING_NOTES;
			String[] notes = new String[] { "drawing the belief network..." };
			myLoadModelTask = new NodeLinearTask( "initialize gui for one network", estimate, notes.length, notes );

			String description = "loading " + myName;
			if( myReadTask != null ){
				int maxRead = readTask.getProgressMax();
				int maxLoad = myLoadModelTask.getProgressMax();

				float ratio = (float)((double)maxLoad/(double)maxRead);
				//System.out.println( "handleProgress() " + maxLoad + "/" + maxRead + " = " + ratio );

				float weightRead = ratio;
				float weightLoad =     1;

				if( weightRead < 0.5 ) weightRead *= 1.2621134;//heuristic: read task should be scaled this way

				if( maxRead < 0x80 ){//ensure progress isn't overly downscaled
					weightLoad = 1/weightRead;
					weightRead = 1;
				}

				ProgressMonitorable[] tasks   = new ProgressMonitorable[] { myReadTask, myLoadModelTask };
			  //float[]               weights = new               float[] {          1,               8 };
				float[]               weights = new               float[] { weightRead,      weightLoad };
				myPolledTask = myCompoundTask = new CompoundTask( description, tasks, weights );
			}
			else myPolledTask = myLoadModelTask;

			myPollster = new Pollster( description, myPolledTask, (Component)UI.this );
			myPollster.setThreadToInterrupt( Thread.currentThread() );
			if( Thread.currentThread().isInterrupted() ) return;
			myPollster.start();
		}

		/** @since 20060520 */
		public void handleCancelation(){
			LoadCallbacks.this.alive = false;
			setDefaultCursor();
			if( LoadCallbacks.this.myLoadModelTask != null ) LoadCallbacks.this.myLoadModelTask.setFinished( true );
		}

		/** @since 20060525 */
		public void handleSyntaxErrors( String[] errors, FileType filetype ){
			SyntaxErrorNotification.getInstance().handleSyntaxErrors( myNetworkInternalFrame, myName, errors, filetype );
		}

		/** @since 20060719 */
		public ThreadGroup getThreadGroup(){
			if( LoadCallbacks.this.myThreadGroup == null ){
				String name = LoadCallbacks.this.myName.substring( LoadCallbacks.this.myName.lastIndexOf( File.separator ) + 1 );
				LoadCallbacks.this.myThreadGroup = new ThreadGroup( UI.this.myThreadGroup, name + " threads" );
			}
			return LoadCallbacks.this.myThreadGroup;
		}

		public  boolean             alive = true;

		private String              myName;
		private NetworkInternalFrame myNetworkInternalFrame;
		private ProgressMonitorable myReadTask;
		private NodeLinearTask      myLoadModelTask;
		private CompoundTask        myCompoundTask;
		private ProgressMonitorable myPolledTask;
		private Pollster            myPollster;
		private ThreadGroup         myThreadGroup;
	}

	/** @since 20060720 */
	public ThreadGroup getThreadGroup(){
		return UI.this.myThreadGroup;
	}

	/** @return number of active threads
		@since 20060720 */
	public int killAll( boolean local ){
		ThreadGroup              group = null;
		if( local ){
			NetworkInternalFrame nif   = UI.this.getActiveHuginNetInternalFrame();
			if( nif != null )    group = nif.getThreadGroup();
		}
		else                     group = UI.this.getThreadGroup();

		if( group == null ) return -1;
		else                return edu.ucla.util.Interruptable.kill( group );
	}

	/** @since 20040109 */
	private void decidePromptAutoArrange( NetworkInternalFrame nif )
	{
		if( StandardNodeImpl.countMaxPositionCoincidence( nif.getBeliefNetwork() ) > (int)5 )
		{
			int result = JOptionPane.showConfirmDialog( this,
				"The network file " + nif.getFileName() + " contains nodes\nwith overlapping position information.  Use the auto-arrange tool to better space the nodes.\nClick \"OK\" to invoke the auto-arrange tool.",
				"Warning: nodes overlap",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE );
			if( result == JOptionPane.OK_OPTION ) nif.doAutoArrange();
		}
	}

	/** @since 20031208 */
	public static void ensureSystemReadyFor( BeliefNetwork newBN ){
		MonitorImpl.ensureBufferCapacity( newBN );
	}

	/** @since 20020523 */
	public JFileChooser getFileChooser(){
		return getFileChooser( null );
	}

	/** @since 20030819 */
	public JFileChooser getFileChooser( File oldFile ){
		JFileChooser chooser = new JFileChooser( mySamiamPreferences.defaultDirectory );
		if( oldFile == null )
		{
			FileType.load( chooser );
			File dummyFile = new File( "aaaa" + mySamiamPreferences.lastEncounteredFileExtension );
			FileType typeLastEncountered = FileType.getTypeForFile(dummyFile);
			if( typeLastEncountered != null ) chooser.setFileFilter( typeLastEncountered.getFileFilter() );
		}
		else{
			chooser.setSelectedFile( oldFile );
			FileType.loadForSaveAs( chooser, oldFile );
		}

		chooser.setMultiSelectionEnabled(false);
		if( !BrowserControl.isMacPlatform() ) chooser.setAcceptAllFileFilterUsed(false);

		return chooser;
	}

	/** @since 20020521 */
	public boolean openFile(){
		JFileChooser chooser = getFileChooser();

		while( chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION )
		{
			if( openFile( chooser.getSelectedFile() ) ) return true;
		}

		return false;
	}

	/** @since 20021105 */
	public boolean openFile( File selectedFile ){
		//System.out.println( "UI.openFile( "+selectedFile.getPath()+" )" );
		synchronized( mySynchOpenFile ){
			String selectedPath = "";
			try{
				selectedPath = selectedFile.getCanonicalFile().getAbsolutePath();
				if( !selectedFile.exists() ){
					showErrorDialog( "The file " + selectedPath + " does not exist." );
					return false;
				}

				if( isOpening( selectedPath ) ){
					showErrorDialog( "Already opening file \"" + selectedPath + "\".  Please wait." );
					return false;
				}

				if( pathConflicts( selectedPath ) ){
					showErrorDialog( "A file with the path \"" + selectedPath + "\"\nis already open. Please choose another\nfile or rename/copy the file you would like to open." );
					return false;
				}

				FileType type = FileType.getTypeForFile( selectedFile );
				if( type == null )
				{
					showErrorDialog( selectedPath + " unrecognized file extension." );
					return false;
				}
				else{
					setWaitCursor();
					type.openFile( selectedFile, new LoadCallbacks( selectedPath ) );
					return true;
				}
			}catch( Throwable throwable ){
				showErrorDialog( "Error opening \"" +selectedPath+ "\": " + throwable.toString() );
				throwable.printStackTrace();
				return false;
			}
		}
	}

	/** @since 021004 */
	public boolean openHuginNet( Reader input, String networkName )
	{
		if( isOpening(     networkName ) ) return false;
		if( pathConflicts( networkName ) ) return false;
		myStreamInputName = networkName;
		setWaitCursor();
		NetworkIO.readHuginNet( input, networkName, new LoadCallbacks( networkName ) );
		return true;
	}

	/** @since 021004 */
	public boolean openHuginNet( InputStream stream, String networkName )
	{
		if( isOpening(     networkName ) ) return false;
		if( pathConflicts( networkName ) ) return false;
		myStreamInputName = networkName;
		setWaitCursor();
		NetworkIO.readHuginNet( stream, networkName, new LoadCallbacks( networkName ) );
		return true;
	}

	/**
		@ret true if a file with path selectedPath is already open.
		@since 021004
	*/
	public boolean pathConflicts( String selectedPath ){
		return ( forPath( selectedPath ) != null );
	}

	/** @since 042604 */
	public NetworkInternalFrame forPath( String selectedPath )
	{
		NetworkInternalFrame nif = null;
		for( Iterator it = myListInternalFrames.iterator(); it.hasNext(); )
		{
			nif = (NetworkInternalFrame)it.next();
			if( nif.getCurrentFileName().endsWith( selectedPath ) ) return nif;
		}
		return (NetworkInternalFrame)null;
	}

	/** @since 042604 */
	public boolean closeFilePath( String selectedPath )
	{
		NetworkInternalFrame nif = forPath( selectedPath );
		if( nif != null ) closeFile( nif );
		return pathConflicts( selectedPath );
	}

	public void initNewInternalFrame(NetworkInternalFrame hnInternalFrame)
	{
		if( Util.DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "UI.initNewInternalFrame()" );

		hnInternalFrame.setVisible(true);
		myListInternalFrames.addFirst( hnInternalFrame );

		/*	Redundant - accomplished by select(hnInternalFrame);		*/
		//setSamiamUserMode( hnInternalFrame.getSamiamUserMode() );

		JMenuItem item = hnInternalFrame.getMenuItem();
		//item.addActionListener(this);
		windowMenu.add(item);

		myJDesktopPane.add(hnInternalFrame);
		select(hnInternalFrame);

		NetworkDisplay ND = hnInternalFrame.getNetworkDisplay();
		ND.setZoomListener( myToolBar );
		myToolBar.zoomed( ND.getZoomFactor() );

		if( Util.DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Number of nodes in " + hnInternalFrame.getCurrentFileName() + ": " + hnInternalFrame.getBeliefNetwork().size() );
	}

	/** @since 091902 */
	public void addInternalFrame( NetworkInternalFrame hnInternalFrame )
	{
		myJDesktopPane.add(hnInternalFrame);
	}

	/** @since 091902 */
	public boolean containsInternalFrame( NetworkInternalFrame hnInternalFrame )
	{
		return (myJDesktopPane==null) ? false : myJDesktopPane.isAncestorOf( hnInternalFrame );
	}

	/** @since 062102 */
	public static final Object MESSAGE_SMILE_STRUCTURE_CHANGE_OVERRIDE_PRE = "Error saving to ";
	public static final Object MESSAGE_SMILE_STRUCTURE_CHANGE_OVERRIDE_POST = ": changes to the structure of the network are not allowed.\nTo override this error and save only changes to probability values, click 'Yes.'\nTo cancel this save operation, click 'No.'";
	public static final Object SAVE_SUCCESS = "SAVE_SUCCESS";
	public static final Object SAVE_FAILURE = "SAVE_FAILURE";
	public static final Object SAVE_CANCELED = "SAVE_CANCELED";

	/** @since 041802 */
	public Object saveFile( NetworkInternalFrame hnInternalFrame )
	{
		String currentFileName = hnInternalFrame.getCurrentFileName();
		final File currentFile = new File( currentFileName );
		final FileType type = FileType.getTypeForFile( currentFile );

		if( !currentFile.exists() )
		{
			if( type.isHuginType() ) return saveFileAs( hnInternalFrame );
			else if( type.isSMILEType() )
			{
				File oldFile = EMLearningDlg.findFileFromPath( currentFileName );
				if( oldFile != null )
				{
					return saveFileAs( hnInternalFrame, oldFile, /* asCopy */ false );
				}
				else
				{
					showErrorDialog("The file " + currentFile.getPath() + " cannot be updated because the original file no longer exists.");
					return SAVE_FAILURE;
				}
			}
			else return SAVE_FAILURE;
		}
		else
		{
			try{
				boolean success = false;
				if( type != null && type.isSMILEType() ) success = saveToSMILEType( currentFile, currentFile, type, hnInternalFrame, /* asCopy */ false );
				else success = hnInternalFrame.saveTo( /* asCopy */ false, currentFile, type );//type.save( currentFile );

				if( success )
				{
					rememberParentDirectory( currentFile );
					addRecentDocument( currentFile );
					return SAVE_SUCCESS;
				}
				else return SAVE_FAILURE;

				//return success;
			}catch ( Exception exception ){
				if( Util.DEBUG_VERBOSE )
				{
					Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
					exception.printStackTrace();
				}

				showErrorDialog( "The file " + currentFile.getPath() + " cannot be saved." );
				return SAVE_FAILURE;
			}
		}
	}

	/** Save the active Hugin Net file. Returns true if successful. */
	public Object saveFileAs( NetworkInternalFrame hnInternalFrame )
	{
		return saveFileAs( hnInternalFrame, false );
	}

	/** @since 20071211 */
	public Object saveFileAs( NetworkInternalFrame hnInternalFrame, boolean asCopy ){
		return saveFileAs( hnInternalFrame, null, asCopy );
	}

	/** @since 20020903 */
	protected Object saveFileAs( NetworkInternalFrame hnInternalFrame, File oldFile, boolean asCopy )
	{
		//System.out.println( "UI.saveFileAs()" );

		String currentFilePath = hnInternalFrame.getCurrentFileName();
		if( oldFile == null ) oldFile = new File( currentFilePath );

		JFileChooser chooser = getFileChooser( oldFile );
		chooser.setDialogTitle( "Save As" );
		//FileType.trimForSaveAs( chooser, oldFile );

		boolean success       = false;
		boolean saveAsNet     = false;
		boolean saveAsSMILE   = false;
		int     resultChooser = -999;
		File    newFile       = null;
		String  pathAbsolute  = null;
		while( (resultChooser = chooser.showSaveDialog(this)) == JFileChooser.APPROVE_OPTION )
		{
			try{
				saveAsNet = saveAsSMILE = false;
				newFile = chooser.getSelectedFile().getCanonicalFile();

				if( newFile.exists() )
				{
					if( isOpening( pathAbsolute = newFile.getAbsolutePath() ) ){
						showMessageDialog( "Already opening file \"" + pathAbsolute + "\".  Please wait.", "Cannot Save", JOptionPane.WARNING_MESSAGE );
						continue;
					}

					if( showWarningDialog( "Overwrite " + newFile.getPath() + "?", JOptionPane.YES_NO_OPTION )
					!= JOptionPane.YES_OPTION )
					continue;
				}

				final FileType typeExplicit = FileType.getTypeForFile( newFile );
				FileType typeImplicit = null;
				FileType typeEffective = null;

				//System.out.println( "\t decided typeExplicit == " + typeExplicit );
				if( typeExplicit == null )
				{
					//System.out.println( "\t typeExplicit == null, perhaps file filter ("+chooser.getFileFilter()+") defines type implicitly..." );
					typeImplicit = FileType.getTypeForFilter( chooser.getFileFilter() );
					//System.out.println( "\t decided typeImplicit == " + typeImplicit );
					if( typeImplicit == null ) continue;
					else{
						typeEffective = typeImplicit;
						newFile = typeImplicit.appendExtension( newFile );
						if( newFile == null ) continue;
						saveAsNet = !(saveAsSMILE = typeImplicit.isSMILEType());
					}
				}
				else
				{
					typeEffective = typeExplicit;
					saveAsNet = !(saveAsSMILE = typeExplicit.isSMILEType());
				}

				//System.out.println( "\t ultimate typeExplicit == " + typeExplicit );
				//System.out.println( "\t ultimate typeImplicit == " + typeImplicit );
				//System.out.println( "\t ultimate typeEffective == " + typeEffective );

				if( saveAsNet )
				{
					try
					{
						hnInternalFrame.saveTo( asCopy, newFile, typeEffective );
						success = true;
					}
					catch( IllegalStateException ise )
					{
						showErrorDialog( ise.getMessage() );
						return SAVE_FAILURE;
					}
				}
				else if( saveAsSMILE )
				{
					final FileType typeOldFile = FileType.getTypeForFile( oldFile );
					//System.out.println( "saveAsSMILE, oldFile " + oldFile.getPath() + ", type: " + typeOldFile );
					if( !typeOldFile.isSMILEType() )
					{
						showErrorDialog( "The file " + hnInternalFrame.getCurrentFileName() + " cannot be saved to " + newFile.getName() + "\nCannot convert .net to "+typeEffective.getName()+" format." );
						return SAVE_FAILURE;
					}

					if( !oldFile.exists() ) oldFile = EMLearningDlg.findFileFromPath( currentFilePath );

					if( oldFile != null && typeOldFile.isSMILEType() )
					{
						FileType effectiveType = typeExplicit;
						if( effectiveType == null ) effectiveType = typeImplicit;
						success = saveToSMILEType( oldFile, newFile, effectiveType, hnInternalFrame, asCopy );
					}
				}
			}
			catch( Exception exception )
			{
				if( Util.DEBUG_VERBOSE )
				{
					Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
					exception.printStackTrace();
				}
			}

			if( success )
			{
				rememberParentDirectory( newFile );
				addRecentDocument( newFile );
				return SAVE_SUCCESS;
			}
			else showErrorDialog( "The file " + hnInternalFrame.getCurrentFileName() + " cannot be saved to " + newFile.getName() );
		}

		//return SAVE_FAILURE;//SAVE_CANCELED;
		return (resultChooser == JFileChooser.CANCEL_OPTION) ? SAVE_CANCELED : SAVE_FAILURE;
	}

	/** @author keith cascio
		@since 20020903 */
	protected boolean saveToSMILEType( File oldFile, File newFile, FileType smileType, NetworkInternalFrame hnInternalFrame, boolean asCopy ) throws IOException
	{
		boolean success = false;
		try
		{
			hnInternalFrame.saveToSMILEType( asCopy, oldFile, newFile, smileType );
			success = true;
		}
		catch( IllegalStateException ise )
		{
			String typeName = (smileType==null) ? "SMILE" : smileType.getName();
			int result = JOptionPane.showConfirmDialog( this,
				MESSAGE_SMILE_STRUCTURE_CHANGE_OVERRIDE_PRE+typeName+MESSAGE_SMILE_STRUCTURE_CHANGE_OVERRIDE_POST,
				typeName+" Save Error",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.ERROR_MESSAGE );
			if( result == JOptionPane.YES_OPTION )
			{
				hnInternalFrame.saveToSMILEType( asCopy, oldFile, newFile, smileType, true );
				success = true;
			}
		}

		return success;
	}

	public static final String STR_YES = "Yes";
	public static final String STR_YESTOALL = "Yes to all";
	public static final String STR_NO = "No";
	public static final String STR_NOTOALL = "No to all";
	public static final String STR_CANCEL = "Cancel";
	public static final String[] ARRAY_CLOSEFILES_TEXT = new String[] { STR_YES,STR_YESTOALL,STR_NO,STR_NOTOALL,STR_CANCEL };
	public static final String[] ARRAY_CLOSEFILE_TEXT = new String[] { STR_YES,STR_NO,STR_CANCEL };
	private static Object OBJ_QUERYMODE;// = MainToolBar.getIcon( "Recompile16.gif" );//myToolBar.myQueryModeIcon;//"Run";
	public static final Object OBJ_SAVE = "Save";
	private static Object[] ARRAY_COMPILESETTINGS_TEXT;// = new Object[] { OBJ_QUERYMODE,OBJ_SAVE,STR_CANCEL };
	private int myNumFilesClosedSinceLastCleanup = (int)0;

	/** @since 021205 */
	private static void initStatic(){
		if( OBJ_QUERYMODE == null ){
			OBJ_QUERYMODE = MainToolBar.getIcon( "Recompile16.gif" );//myToolBar.myQueryModeIcon;//"Run";
			ARRAY_COMPILESETTINGS_TEXT = new Object[] { OBJ_QUERYMODE,OBJ_SAVE,STR_CANCEL };
		}
		initCursors();
	}

	/** @since 20030328 */
	protected Object showCloseFileDialog( String fileName, Object[] array )
	{
		return showCloseWarningDialog( "Save " + fileName + " before closing?", array );
	}

	/** @since 20040108 */
	protected Object showCloseWarningDialog( Object msg, Object[] array )
	{
		return showOptionDialog( msg, "Warning", JOptionPane.WARNING_MESSAGE, array );
	}

	/** @since 20040109 */
	public Object showOptionDialog( Object msg, String title, int message_type, Object[] array )
	{
		int returnValue = JOptionPane.showOptionDialog( this,
				msg,
				title,
				(int)0,
				message_type,
				(Icon)null,
				array,
				array[array.length-1] );
		if( returnValue == JOptionPane.CLOSED_OPTION ) return null;
		else return array[returnValue];
	}

	/** @since 20040108 */
	private boolean testAndConfirmSave( NetworkInternalFrame hnInternalFrame )
	{
		boolean proceedWithSave = false;
		if( hnInternalFrame.userPropertiesModified() )
		{
			int warnResult = JOptionPane.showConfirmDialog( this, "If you close " +hnInternalFrame.getFileName()+ " without saving,\nyou will lose user property information.  Save?", "Warning: user properties edited", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
			if( warnResult == JOptionPane.YES_OPTION ) proceedWithSave = true;
		}
		return proceedWithSave;
	}

	/** Close the active Hugin Net file. Returns true if successful */
	public Object closeFilePromptPlusCleanup( NetworkInternalFrame hnInternalFrame )
	{
		Object ret = closeFilePrompt( hnInternalFrame, ARRAY_CLOSEFILE_TEXT );
		if( !((ret == STR_CANCEL) || (ret == null)) ) closeFileCleanup();
		return ret;
	}

	/** @since 20030328 */
	public Object closeFilePrompt( NetworkInternalFrame hnInternalFrame, String[] array )
	{
		boolean proceedWithClose = true;
		Object returnValue = null;

		if( hnInternalFrame.getSamiamUserMode().contains( SamiamUserMode.READONLY ) ) returnValue = STR_NO;
		else
		{
			String strFileName = hnInternalFrame.getFileName();
			returnValue = showCloseFileDialog( strFileName, array );

			boolean proceedWithSave = ( returnValue == STR_YES || returnValue == STR_YESTOALL );
			if( returnValue == STR_NO || returnValue == STR_NOTOALL ) proceedWithSave = testAndConfirmSave( hnInternalFrame );

			if( proceedWithSave )
			{
				Object saveResult = saveFile( hnInternalFrame );
				proceedWithClose = (saveResult == SAVE_SUCCESS);// || saveResult == SAVE_CANCELED);
				if( (saveResult == SAVE_FAILURE) || (saveResult == SAVE_CANCELED) ) returnValue = STR_CANCEL;
			}
			else if( returnValue == STR_CANCEL || returnValue == null ) proceedWithClose = false;
		}

		if( proceedWithClose ) closeFile( hnInternalFrame );

		return returnValue;
	}

	/** @since 20030327 */
	public void closeFile( NetworkInternalFrame hnInternalFrame )
	{
		hnInternalFrame.setVisible( false );
		myJDesktopPane.remove( hnInternalFrame );
		windowMenu.remove( hnInternalFrame.getMenuItem() );
		myListInternalFrames.remove( hnInternalFrame );
		hnInternalFrame.dispose();
		++myNumFilesClosedSinceLastCleanup;
	}

	/** @since 20040115 */
	private void closeFileCleanup()
	{
		//System.out.println( "UI.closeFileCleanup()" );

		if( myNumFilesClosedSinceLastCleanup > (int)0 )
		{
			//debugJDesktopPane();
			//debugEliminateReferences();

			/*   refreshJDesktopPane();    */

			if( myListInternalFrames.isEmpty() ){
				setSamiamUserMode( new SamiamUserMode() );//061202
				myJDesktopPane.setSelectedFrame( (JInternalFrame)null );
			}
			//else select( getActiveHuginNetInternalFrame() );

			new edu.ucla.util.SystemGCThread().start();//010904
			myNumFilesClosedSinceLastCleanup = (int)0;
		}
	}

	/** @since 20040113 ?/
	private void refreshJDesktopPane(){
		if( Util.DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "UI.refreshJDesktopPane()" );

		/?
		Collection iconified = new LinkedList();
		Collection maximized = new LinkedList();
		JInternalFrame next;
		for( Iterator it = myListInternalFrames.iterator(); it.hasNext(); ){
			next = (JInternalFrame) it.next();
			if( next.isIcon() ) iconified.add( next );
			else if( next.isMaximum() ) maximized.add( next );
		}
		//System.out.println( "iconified: " + iconified );
		//System.out.println( "maximized: " + maximized );
		?/

		myJDesktopPane.removeAll();
		myCenterPain.remove( myJDesktopPane );//myCenterPain.add( myJDesktopPane, BorderLayout.CENTER );
		myJDesktopPane = null;

		myJDesktopPane = new JDesktopPane();
		for( Iterator it = myListInternalFrames.iterator(); it.hasNext(); )
			myJDesktopPane.add( (JInternalFrame)it.next() );
		myCenterPain.add( myJDesktopPane, BorderLayout.CENTER );

		/?
		try{
			for( Iterator it = iconified.iterator(); it.hasNext(); )
				((JInternalFrame)it.next()).setIcon( true );
		}catch( java.beans.PropertyVetoException e ){
			System.err.println( e );
		}
		iconified.clear();
		iconified = null;
		try{
			for( Iterator it = maximized.iterator(); it.hasNext(); )
				((JInternalFrame)it.next()).setMaximum( true );
		}catch( java.beans.PropertyVetoException e ){
			System.err.println( e );
		}
		maximized.clear();
		maximized = null;
		?/
	}*/

	/** @since 011304 */
	private void debugEliminateReferences()
	{
		Util.STREAM_DEBUG.println( "UI.debugEliminateReferences()" );

		myCenterPain.remove( myJDesktopPane );//myCenterPain.add( myJDesktopPane, BorderLayout.CENTER );
		myJDesktopPane = null;
		getContentPane().remove( myToolBar );//myContentPane.add( myToolBar, BorderLayout.NORTH );
		myToolBar = null;
		setJMenuBar( null );//setJMenuBar( menuBar );
		menuBar = null;

		//LookAndFeel laf = UIManager.getLookAndFeel();
		//laf.uninitialize();
		//laf.initialize();
	}

	/** @since 021205 */
	public boolean closeAllNoPrompt(){
		for( NetworkInternalFrame nif = getActiveHuginNetInternalFrame(); nif != null; nif = getActiveHuginNetInternalFrame() ){
			closeFile( nif );
		}
		closeFileCleanup();
		return true;
	}

	/** @since 011504 */
	public boolean closeAll()
	{
		String[] array = ( myListInternalFrames.size() > 1 ) ? ARRAY_CLOSEFILES_TEXT : ARRAY_CLOSEFILE_TEXT;
		Object ret = null;
		boolean flagYesToAll = false;
		boolean flagNoToAll = false;

		for( NetworkInternalFrame current = getActiveHuginNetInternalFrame(); current != null; current = getActiveHuginNetInternalFrame() )
		{
			if( current.getSamiamUserMode().contains( SamiamUserMode.READONLY ) ) closeFile( current );
			else if( flagYesToAll || ( flagNoToAll && testAndConfirmSave( current ) ) )
			{
				Object saveResult = saveFile( current );
				if( saveResult == SAVE_SUCCESS || saveResult == SAVE_CANCELED ) closeFile( current );
			}
			else if( flagNoToAll ) closeFile( current );
			else if( !flagYesToAll && !flagNoToAll )
			{
				ret = closeFilePrompt( current, array );

				if( ret == STR_YESTOALL ) { flagYesToAll = true; }
				else if( ret == STR_NOTOALL ) { flagNoToAll = true; }
				else if( ret == STR_CANCEL || ret == null ) {
					closeFileCleanup();
					return false;
				}
			}

			array = ( myListInternalFrames.size() > 1 ) ? ARRAY_CLOSEFILES_TEXT : ARRAY_CLOSEFILE_TEXT;
		}

		return true;
	}

	/**
		Exit the program.
		Call System.exit() only if enabled.
	*/
	public void exitProgram()
	{
		if( closeAll() && myListInternalFrames.isEmpty() )
		{
			try{
				mySamiamPreferences.putProperty( STR_KEY_VISIBLE_STATUSBAR,  new Boolean(            myShowStatusBarItem.isSelected() ) );
				mySamiamPreferences.putProperty( STR_KEY_VISIBLE_TOOLBAR,    new Boolean(              myShowToolBarItem.isSelected() ) );
				mySamiamPreferences.putProperty( STR_KEY_VISIBLE_ICBTOOLBAR, new Boolean( myShowInstantiationToolBarItem.isSelected() ) );
				mySamiamPreferences.putProperty( STR_KEY_ICBTCONSTRAINTS,    edu.ucla.util.AbstractStringifier.reflect( BorderLayout.class, null, "getConstraints", new Class[]{ java.awt.Component.class }, myCenterPain.getLayout(), new Object[]{ myInstantiationClipboardToolBar }, BorderLayout.EAST ) );//((BorderLayout) myCenterPain.getLayout()).getConstraints( myInstantiationClipboardToolBar ) );
				mySamiamPreferences.putProperty( STR_KEY_ICBTORIENTATION,    new Integer( myInstantiationClipboardToolBar.getOrientation() ) );
			}catch( Throwable thrown ){
				System.err.println( "warning: UI.exitProgram() caught " + thrown );
			}
			mySamiamPreferences.setLastDynamator( getDynamator() );
			mySamiamPreferences.saveOptionsToFile();
			setVisible( false );
			if( myFlagSystemExitEnabled ) System.exit(0);
		}
	}

	/** Set the cursor type to Default. */
	public void setDefaultCursor()
	{
		//setCursor( CURSOR_DEFAULT );
		setCursor( null );
	}

	/** Set the cursor type to Wait. */
	public void setWaitCursor()
	{
		setCursor( CURSOR_WAIT );
	}

	/** @author Keith Cascio
		@since 070202 */
	public static Cursor CURSOR_WAIT = Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR );
	public static Cursor CURSOR_DEFAULT = Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR );
	public static Cursor CURSOR_HAND = Cursor.getPredefinedCursor( Cursor.HAND_CURSOR );
	public static Cursor CURSOR_WAIT_MONITORS;
	public static Cursor CURSOR_WAIT_EVIDENCE;
	public static Cursor CURSOR_WAIT_COMPILE;
	public static Cursor CURSOR_WAIT_RC;

	/** @since 021105 */
	private static void initCursors()
	{
		if( CURSOR_WAIT_MONITORS != null ) return;

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Point hotspot = new Point(0,0);

		URL url = MainToolBar.findImageURL( "cursor_wait_monitors_32x32.gif" );
		Image image = toolkit.createImage( url );
		CURSOR_WAIT_MONITORS = toolkit.createCustomCursor( image, hotspot, "waiting for monitors" );

		url = MainToolBar.findImageURL( "cursor_wait_evidence_32x32.gif" );
		image = toolkit.createImage( url );
		CURSOR_WAIT_EVIDENCE = toolkit.createCustomCursor( image, hotspot, "waiting for evidence propogation" );

		url = MainToolBar.findImageURL( "cursor_wait_compile_32x32.gif" );
		image = toolkit.createImage( url );
		CURSOR_WAIT_COMPILE = toolkit.createCustomCursor( image, hotspot, "waiting for compile" );

		url = MainToolBar.findImageURL( "cursor_wait_rc_32x32.gif" );
		image = toolkit.createImage( url );
		CURSOR_WAIT_RC = toolkit.createCustomCursor( image, hotspot, "waiting for rc" );
	}

	/** @since 20090127 */
	private void superSetCursor( final Cursor curse ){
		super.setCursor( curse );
	}

	/** Set the cursor type
		@since 20020702 */
	public void       setCursor( final Cursor curse ){
		delayedCursor.start( curse );
	}

	/** @since 20090429 */
	private UI deepSetCursor( final Cursor curse ){
		try{
			superSetCursor( curse );
			if( myJDesktopPane != null ){ myJDesktopPane.setCursor( curse ); }
			if(        menuBar != null ){        menuBar.setCursor( curse ); }
			for( int i = 0; i < myListInternalFrames.size(); i++ ){
				((Component)myListInternalFrames.get(i)).setCursor( curse );
			}
			getRootPane().paintImmediately( getRootPane().getVisibleRect() );
		}catch( Throwable thrown ){
			if( Util.DEBUG_VERBOSE ){
				System.err.println( "warning: UI.deepSetCursor() caught " + thrown );
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				thrown.printStackTrace( Util.STREAM_VERBOSE );
			}
		}
		return this;
	}

	/** @since 20090429 */
	private Interruptable delayedCursor = new Interruptable(){
		{ setName( "UI delayed cursor setter" ); }
		public void runImpl( final Object arg1 ) throws InterruptedException{
			if(   follower ){ Thread.sleep( 0x40 ); }
			else{ follower = true; }
			SwingUtilities.invokeLater( new Runnable(){
				public void run(){
					UI.this.deepSetCursor( (Cursor) arg1 );
				}
			} );
			Thread.sleep( 0x200 );
			follower = false;
		}
		private boolean follower = false;
	};

	/** Show an error dialog box. */
	public void showErrorDialog( Object message ){
		showMessageDialog( message, "Error", JOptionPane.ERROR_MESSAGE );
	}

	/** @since 051903 */
	public void showMessageDialog( Object message, String title ){
		showMessageDialog( message, title, JOptionPane.PLAIN_MESSAGE );
	}

	/** @since 012804 */
	public void showMessageDialog( Object message, String title, int type ){
		JOptionPane.showMessageDialog( this, message, title, type );
	}

	/** Show an warning dialog box. */
	public int showWarningDialog(Object message, int optionType)
	{
		return JOptionPane.showConfirmDialog(this, message,
			"Warning", optionType,
			JOptionPane.WARNING_MESSAGE);
	}

	/** interface MenuListener

		@author keith cascio
		@since  20021204 */
	public void menuSelected( MenuEvent e )
	{
		Object src = e.getSource();
		if( src == toolsMenu ){
			action_PASTEEVIDENCE.setSamiamUserMode( UI.this.getSamiamUserMode() );
			codeToolMenu.setEnabled( getActiveHuginNetInternalFrame() != null );
		}
	}
	public void menuDeselected(MenuEvent e){}
	public void menuCanceled(MenuEvent e){}

	/** Respond to an action performed. */
	public void actionPerformed( ActionEvent event )
	{
		Object src = event.getSource();
		//031902
		if (src == debugDFSItem)
		{
			NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
			if( nif == null ) return;

			DisplayableBeliefNetwork bn = nif.getBeliefNetwork();
			Util.STREAM_DEBUG.println( "Debug depth first order on " + bn.size() + " nodes.\n" );//debug

			//Iterator foo = ((HashDirectedGraph)(bn.getGraph())).depthFirstIterator();
			RecursiveDepthFirstIterator foo = new RecursiveDepthFirstIterator( bn );

			if( foo.isCyclic() ) Util.STREAM_DEBUG.println( "WARNING: CYCLIC GRAPH\n" );//debug
			FiniteVariable fVar = null;

			while( foo.hasNext() )
			{
				fVar = (FiniteVariable) foo.next();
				//getActiveHuginNetInternalFrame().getNetworkDisplay().nodeSelectionClearAll();
				//getActiveHuginNetInternalFrame().getNetworkDisplay().ensureNodeIsVisible( fVar );
				Util.STREAM_DEBUG.println( fVar );//debug
			}

			Util.STREAM_DEBUG.println( "\n\n" );

			Iterator foo2 = new DepthFirstIterator( bn );
			while( foo2.hasNext() )
			{
				fVar = (FiniteVariable) foo2.next();
				getActiveHuginNetInternalFrame().getNetworkDisplay().ensureNodeIsVisible( (DisplayableFiniteVariable)fVar, true );
				Util.STREAM_DEBUG.println( fVar );//debug
			}
		}
		//041802
		else if( src == debugCPTEditItem )
		{
			FLAG_GENIE_STYLE_CPT_EDIT = !FLAG_GENIE_STYLE_CPT_EDIT;
		}
		//062002
		else if( src == debugRoundComplementItem )
		{
			//DisplayableFiniteVariable.FLAG_ROUND_COMPLEMENT = !DisplayableFiniteVariable.FLAG_ROUND_COMPLEMENT;
		}
		//060702
		else if( src == debugCloneItem )
		{
			NetworkInternalFrame AHNIF = getActiveHuginNetInternalFrame();
			if( AHNIF == null ) return;
			BeliefNetwork clone = (BeliefNetwork) AHNIF.getBeliefNetwork().getSubBeliefNetwork().clone();
			try
			{
				NetworkInternalFrame newFrame = new NetworkInternalFrame( clone, AHNIF.getParentFrame(), "DEBUG CLONE" );
			}
			catch( Exception e )
			{
				System.err.println( "Warning: debug clone NetworkInternalFrame() failed:" );
				e.printStackTrace();
			}
		}
		//061702
		else if( src == debugUserModeItem )
		{
			NetworkInternalFrame HNIF = getActiveHuginNetInternalFrame();
			if( HNIF == null ) return;
			SamiamUserMode mode = HNIF.getSamiamUserMode();
			boolean querysetting = mode.contains( SamiamUserMode.QUERY );

			mode.setModeEnabled( SamiamUserMode.QUERY, !querysetting );
			mode.setModeEnabled( SamiamUserMode.EDIT, querysetting );

			//Util.STREAM_DEBUG.println( "SamiamUserMode is now " + (querysetting ? SamiamUserMode.EDIT : SamiamUserMode.QUERY) );//debug
			Util.STREAM_DEBUG.println( "Debug: SamiamUserMode currently " + mode );//debug

			HNIF.setSamiamUserMode( mode );
			setSamiamUserMode( mode );
		}
		//082002
		else if( src == debugInferenceEngineCacheHitsItem )
		{
			NetworkInternalFrame hnif = getActiveHuginNetInternalFrame();
			if( hnif != null )
			{
				InferenceEngine canonical = hnif.getCanonicalInferenceEngine();
				if( canonical instanceof JoinTreeInferenceEngineImpl )
				{
					int cacheHits = ((JoinTreeInferenceEngineImpl) canonical).getCacheHits();
					String message = String.valueOf( cacheHits ) + " cache hits";
					Util.STREAM_DEBUG.println( message );
					JOptionPane.showMessageDialog( this, message, "Debug Information", JOptionPane.PLAIN_MESSAGE );
				}
			}
		}
		else if( src == debugRepaintEvidenceTreeItem )
		{
			NetworkInternalFrame HNIF = getActiveHuginNetInternalFrame();
			if( HNIF != null ) HNIF.repaintEvidenceTree();
		}
		else if( src == debugWindowDimensionsItem )
		{
			Util.STREAM_DEBUG.println( "UI size: " + getSize() );
			System.gc();
		}
		else if( src == debugCursorItem )
		{
			setCursor( CURSOR_WAIT_EVIDENCE );
		}
		else if( src == debugTestEnginesItem )
		{
			NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
			if( nif != null )
			{
				BeliefNetwork bn = nif.getBeliefNetwork();
				EvidenceController ec = bn.getEvidenceController();
				InferenceEngine ie1 = new edu.ucla.belief.recursiveconditioning.RCEngineGenerator().manufactureInferenceEngine( bn );
				InferenceEngine ie2 = new JEngineGenerator().manufactureInferenceEngine( bn );

				double epsilon = (double)0.000009;
				int numEvidence = (int)4;

				String message = null;
				String strEpsilon = JOptionPane.showInputDialog( this, "Please choose value for epsilon.", new Double( epsilon ) );
				String strNumEvidence = JOptionPane.showInputDialog( this, "Please choose number of\nrandom evidence values to set.", new Integer( numEvidence ) );

				try{
					epsilon = Double.parseDouble( strEpsilon );
				}catch( NumberFormatException e ){
					message = "incorrectly formatted value for epsilon: " + strEpsilon;
				}

				try{
					numEvidence = Integer.parseInt( strNumEvidence );
				}catch( NumberFormatException e ){
					message = "incorrectly formatted value for number random evidence: " + strNumEvidence;
				}

				if( message == null )
				{
					if( AbstractInferenceEngine.test( ie1, ie2, ec, epsilon, numEvidence ) ) message = "passed";
					else message = "failed";
				}

				JOptionPane.showMessageDialog( this, message, "test result", JOptionPane.PLAIN_MESSAGE );

				ec.removePriorityEvidenceChangeListener( ie1 );
				ec.removePriorityEvidenceChangeListener( ie2 );
			}
		}
		else if( src == debugJT2Item )
		{
			NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
			if( nif != null )
			{
				BeliefNetwork bn = nif.getBeliefNetwork();
				BeliefCompilation bc = new JEngineGenerator().compile( bn, 1, (Random)null );
				Settings settings = edu.ucla.belief.recursiveconditioning.RCEngineGenerator.getSettings( bn );
				settings.rcFromJT2( bn, bc );
			}
		}
		else if( src == debugDgraphWriteItem )
		{
			NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
			if( nif == null ) return;

			BeliefNetwork bn = nif.getBeliefNetwork();
			InferenceEngine canonical = nif.getCanonicalInferenceEngine();

			if( bn == null || !(canonical instanceof edu.ucla.belief.inference.RCEngine)) return;

			edu.ucla.belief.inference.RCEngine rcengine = (edu.ucla.belief.inference.RCEngine) canonical;
			il2.inf.rc.RC core = rcengine.rcCore();
			if( core == null ) return;

			RCSettings settings = edu.ucla.belief.inference.RCEngineGenerator.getSettings( (PropertySuperintendent)bn, false );
			if( settings == null ) return;
			RCInfo info = settings.getInfo();
			if( info == null ) return;
			DGraph dgraph = info.dgraph();
			if( dgraph == null ) return;

			PrintWriter out = null;
			//PrintWriter out2 = null;
			try{
				out = new PrintWriter( new FileOutputStream( "rcinfo_vicky.txt" ) );//"dgraph_vicky.txt" ) );
				//out2 = new PrintWriter( new FileOutputStream( "rc_vicky.txt" ) );
			}catch( Exception e ){
				e.printStackTrace();
				return;
			}

			//dgraph.write( out );
			info.write( out );
			out.close();

			//core.write( out2 );
			//out2.close();
		}
		else if( src == debugReadOnly )
		{
			NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
			if( nif == null ) return;

			SamiamUserMode mode = nif.getSamiamUserMode();
			mode.setModeEnabled( SamiamUserMode.READONLY, true );
			nif.setSamiamUserMode( mode );
		}
		else if( src == debugAnimation )
		{
			NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
			if( nif == null ) return;
			new Animator( nif.getNetworkDisplay() ).scaleRandom( nif.getBeliefNetwork() );
		}
		else if( src == debugDecisionTreeOptimization )
		{
			NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
			if( nif == null ) return;

			/*DisplayableBeliefNetwork bn = nif.getBeliefNetwork();
			new Optimizer( (double)-1 ).printOptimizedStats( bn, nif.console );*/

			NetworkDisplay nd = nif.getNetworkDisplay();
			nd.listZorder( System.out );
			//nd.testOverlap();
		}
		else if( src == debugVariance ){
			NetworkInternalFrame nif = getActiveHuginNetInternalFrame();
			if( nif == null ) return;

			BeliefNetwork bn = nif.getBeliefNetwork();
			il2.inf.experimental.VEVariance.manual_variance( bn, bn.getEvidenceController().evidence(), nif.console );
		}
		else if( src == debugScreenshot ){
			Util.screenshot( (Component)this, "png", new File( "." + File.separator + "UIscreenshot.png" ) );
		}
		/*else{
			JMenuItem item = (JMenuItem)src;
			String name = item.getText();
			for (int i = 0; i < myListInternalFrames.size(); i++){
				NetworkInternalFrame hnInternalFrame = (NetworkInternalFrame)myListInternalFrames.get(i);
				if (item == hnInternalFrame.getMenuItem()){
					toFront(hnInternalFrame);
					restore(hnInternalFrame);
				}
			}
		}*/
	}

	protected PackageOptionsDialog myPackageOptionsDialog = null;

	/** @since 021505 */
	public PackageOptionsDialog getPackageOptionsDialog(){
		return myPackageOptionsDialog;
	}

	/** @since 091702 */
	public void windowActivated( WindowEvent event ){
		if( myPackageOptionsDialog != null && myPackageOptionsDialog.isVisible() ) myPackageOptionsDialog.toFront();
	}

	public void windowClosing(WindowEvent event){
		exitProgram();
	}

	public void windowClosed(WindowEvent event){}
	public void windowDeactivated(WindowEvent event){}
	public void windowDeiconified(WindowEvent event){}
	public void windowIconified(WindowEvent event){}
	public void windowOpened(WindowEvent event){}

	/** @author Keith Cascio
		@since 111802 */
	public void setActiveZoomFactor( double newZoomFactor )
	{
		NetworkInternalFrame HNIF = getActiveHuginNetInternalFrame();
		if( HNIF != null ) HNIF.getNetworkDisplay().setZoomFactor( newZoomFactor );
	}

	transient protected edu.ucla.belief.ui.dialogs.NetworkInformation myNetworkInformation = new edu.ucla.belief.ui.dialogs.NetworkInformation();

	/** @since 021405 Valentine's Day! */
	public NetworkInformation getNetworkInformation(){
		return myNetworkInformation;
	}

	public final HandledModalAction action_PRINTSELECTEDNODES;
	public final HandledModalAction action_INFORMATION;
	public final HandledModalAction action_SELECTEDNODESRIGHT;
	public final HandledModalAction action_SELECTEDNODESLEFT;
	public final HandledModalAction action_SELECTEDNODESUP;
	public final HandledModalAction action_SELECTEDNODESDOWN;
	public final HandledModalAction action_ZOOMIN;
	public final HandledModalAction action_ZOOMOUT;
	public final HandledModalAction action_ACTUALPIXELS;
	public final HandledModalAction action_FITONSCREEN;
	public final SamiamAction       action_SHOWSTATUSBAR;
	public final SamiamAction       action_SHOWTOOLBAR;
	public final SamiamAction       action_SHOWINSTANTIATIONTOOLBAR;
	public final HandledModalAction action_REFRESH;
	public final HandledModalAction action_COPYEVIDENCE;
	public final HandledModalAction action_CUTEVIDENCE;
	public final PasteAction        action_PASTEEVIDENCE;
	public final SamiamAction       action_VIEWINSTANTIATIONCLIPBOARD;
	public final SamiamAction       action_LOADINSTANTIATIONCLIPBOARD;
	public final SamiamAction       action_SAVEINSTANTIATIONCLIPBOARD;
	public final SamiamAction       action_IMPORTINSTANTIATION;
	public final SamiamAction       action_EXPORTINSTANTIATION;
	public final HandledModalAction action_COPY;
	public final HandledModalAction action_CUT;
	public final PasteAction        action_PASTE;
	public final PasteAction        action_PASTESPECIAL;
	public final SamiamAction       action_NEW;
	public final SamiamAction       action_OPEN;
	public final HandledModalAction action_SAVE;
	public final HandledModalAction action_SAVEAS;
	public final HandledModalAction action_CLOSE;
	public final HandledModalAction action_CLOSEALL;
	public final SamiamAction       action_EXIT;
	public final HandledModalAction action_NETWORKDISPLAY;
	public final HandledModalAction action_NETWORKDISPLAYCRINGE;
	public final HandledModalAction action_EVIDENCETREECRAM;
	public final HandledModalAction action_EVIDENCETREEEXPANDVARIABLES;
	public final HandledModalAction action_BESTWINDOWARRANGEMENT;
	public final HandledModalAction action_SCREENSHOTSCRIPTS;
	public final SamiamAction       action_MAXIMIZE;
	public final HandledModalAction action_PARTIALS;
	public final HandledModalAction action_TESTDSL;
	public final SamiamAction       action_APPROX;
	public final HandledModalAction action_SENSITIVITY;
	public final HandledModalAction action_MAP;
    public final HandledModalAction action_SDP;
	public final ModalAction        action_MPE;
	public final HandledModalAction action_IMPACT;
	public final HandledModalAction action_VARIABLESELECTION;
	public final HandledModalAction action_EM;
	public final HandledModalAction action_SIMULATE;
	public final HandledModalAction action_CONFLICT;
	public final HandledModalAction action_RETRACT;
	public final ModalAction        action_PRECOMPUTE;
	public final HandledModalAction action_HIDEALL;
	public final HandledModalAction action_SHOWALL;
	public final HandledModalAction action_SHOWSELECTED;
	public final HandledModalAction action_ADDNODE;
	public final HandledModalAction action_DELETENODES;
	public final HandledModalAction action_ADDEDGE;
	public final HandledModalAction action_DELETEEDGE;
	public final HandledModalAction action_REPLACEEDGE, action_RECOVEREDGE, action_RANDOMSPANNINGFOREST, action_FINDBURNTBRIDGES, action_AUTOMATICEDGERECOVERY;
	public final HandledModalAction action_COPYCPT;
	public final HandledModalAction action_AUTOARRANGE;
	public final HandledModalAction action_SQUEEZE;
	public final HandledModalAction action_DOTLAYOUT;
	public final HandledModalAction action_TOGGLEMODE;
	public final HandledModalAction action_HIDE, action_HIDEHIDDENEDGES, action_SHOWEDGES, action_SHOWRECOVERABLES;
	public final LockableHandledModalAction action_READONLY;
	public final LockableHandledModalAction action_EVIDENCEFROZEN;
	public final LockableHandledModalAction action_LOCATIONSFROZEN;
	public final HandledModalAction action_ANIMATE;
	public final ModalAction        action_RESETANIMATION;
	public final ModalAction        action_INSTANTREPLAY;
	public final HandledModalAction action_QUERYMODE;
	public final HandledModalAction action_EDITMODE;
	public final SamiamAction       action_CHOOSEDYNAMATOR;
	public final ModalAction        action_COMPILESETTINGS;
	public final HandledModalAction action_RESETEVIDENCE, action_DEFAULTEVIDENCE;
	public final HandledModalAction action_PREFERENCES;
	public final HandledModalAction action_DEFAULTPREFERENCES;
	public final SamiamAction       action_CLEARRECENTDOCUMENTS;
	public final SamiamAction       action_FORGETTOOLLOCATIONS;
	public final SamiamAction       action_HELPLOCAL;
	public final SamiamAction       action_HELPLIVE;
	public final SamiamAction       action_WEBSAMIAM;
	public final SamiamAction       action_WEBARGROUP;
	public final SamiamAction       action_ABOUT;
	public final SamiamAction       action_TUTORIALS;
	public final SamiamAction       action_TUTORIALSLIVE;
	public final HandledModalAction action_RC;
	public final HandledModalAction action_CONSOLE;
	public final HandledModalAction action_CODETOOLNETWORK;
	public final HandledModalAction action_CODETOOLPROBABILITY;
	public final SamiamAction       action_CODETOOLCPTDEMO;
	public final SamiamAction       action_HELPLOCALCODETOOL;
	public final HandledModalAction action_RCTempTimeGraph;
	public final HandledModalAction action_SELECTALL;
	public final HandledModalAction action_CPTMONITORS;
	public final HandledModalAction action_PRIMULA;
	public final ActionPrune        action_PRUNE;
	public final HandledModalAction action_KILLLOCAL;
	public final HandledModalAction action_KILLGLOBAL;

	/** @since 20091116 */
	public UI reselectDynamator(){
		try{
			if( myToolBar        == null ){ return this; }
			if( myComboDynamator == null ){ myComboDynamator = myToolBar.makeAlgorithmComponent();
				myDialogDynamator = new JDialog( UI.this, "Algorithm", true );
				myDialogDynamator.getContentPane().add( myComboDynamator );
				myDialogDynamator.pack();
				myComboDynamator.addActionListener( new ActionListener(){
					public void actionPerformed( ActionEvent event ){
						myDialogDynamator.setVisible( false );
					}
				} );
			}
			myComboDynamator.setSelectedItem( getDynamator() );
			Util.centerWindow( myDialogDynamator );
			myDialogDynamator.setVisible( true );
		}catch( Throwable thrown ){
			System.err.println( "warning: UI.reselectDynamator() caught " + thrown );
		}
		return this;
	}
	protected JComboBox myComboDynamator;
	protected JDialog   myDialogDynamator;

	/** @since 051603 */
	public InstantiationClipBoard getInstantiationClipBoard()
	{
		return myInstantiationClipBoard;
	}

	transient protected boolean myFlagPasteSpecial = false;
	transient protected NetworkInternalFrame myHNIFToPaste = null;
	transient protected NetworkInternalFrame myHNIFToSqueeze;

	/** interface NetworkDisplay.UserInputListener
		@since 101702 */
	public void handleUserEdge( DisplayableFiniteVariable source, DisplayableFiniteVariable sink ){}

	/** interface NetworkDisplay.UserInputListener
		@since 101702 */
	public void handleUserActualPoint( Point pActual )
	{
		NetworkInternalFrame nif = null;
		boolean fps = false;
		Point actual = null;

		boolean paste = false, squeeze = false;

		synchronized( mySynchonization ){
			actual = new Point( pActual );

			if( myHNIFToPaste != null ){
				paste = true;
				nif = myHNIFToPaste;
				fps = myFlagPasteSpecial;
				myHNIFToPaste = null;
				myFlagPasteSpecial = false;
			}
			else if( myHNIFToSqueeze != null ){
				squeeze = true;
				nif = myHNIFToSqueeze;
				myHNIFToSqueeze = null;
			}
		}

		if( paste ) doPasteSubnetwork( actual, nif, fps );
		else if( squeeze ) Squeeze.doSqueeze( nif, actual );
	}

	/** @since 20090420 */
	private NetworkInternalFrame initiatePaste( boolean special, String message ){
		(myHNIFToPaste = getActiveHuginNetInternalFrame()).getNetworkDisplay().promptUserActualPoint( message, (NetworkDisplay.UserInputListener)UI.this );
		myFlagPasteSpecial = special;
		return myHNIFToPaste;
	}

	/** @since 021405 Valentine's Day! */
	public void doPasteSubnetwork( Point pActual, NetworkInternalFrame nif, boolean special ){
		DisplayableBeliefNetwork bn = nif.getBeliefNetwork();
		if( special ) myNetworkClipBoard.promptPaste( bn, nif, pActual, this );
		else myNetworkClipBoard.paste( bn, nif, pActual );
	}

	/** @since 021405 Valentine's Day! */
	public NetworkClipBoard getNetworkClipBoard(){
		return this.myNetworkClipBoard;
	}

	/** interface NetworkDisplay.UserInputListener
		@since 101702 */
	public void userInputCancelled()
	{
		synchronized( mySynchonization ){

		myFlagPasteSpecial = false;
		myHNIFToPaste = null;
		myHNIFToSqueeze = null;

		}
	}
}
