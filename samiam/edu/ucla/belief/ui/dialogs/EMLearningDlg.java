package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.statusbar.StatusBar;
import edu.ucla.belief.ui.actionsandmodes.*;
import edu.ucla.belief.ui.toolbar.MainToolBar;
import edu.ucla.belief.ui.internalframes.CodeToolInternalFrame;

import edu.ucla.belief.learn.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.InflibFileFilter;
import edu.ucla.util.code.*;

import javax.swing.ProgressMonitor;
import java.awt.*;
import java.awt.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;

/**
	Changed by Keith Cascio 051502
*/
public class EMLearningDlg extends JDialog implements ActionListener, EMThread.LearningListener//, Simulator.SimulationListener
{
	public static final String STR_MSG_LEARN = "learning parameters...";
	public static final String STR_TEXT_BUTTON_LEARN = "Learn";

	public static final double DOUBLE_THRESHOLD_FLOOR = (double)0.00000000001;
	public static final double DOUBLE_THRESHOLD_CEILING = (double)0.999999999999;;

	//Learn Panel
	private JLabel lblThreshold = new JLabel( "Log-likelihood threshold:" );
	private DecimalField dfTheshold = new DecimalField( 0.05, 5, DOUBLE_THRESHOLD_FLOOR, DOUBLE_THRESHOLD_CEILING );
	private JLabel lblIterations = new JLabel( "Max iterations:" );
	private WholeNumberField wnfIterations = new WholeNumberField (5, 4);
	private JCheckBox myCbBias = new JCheckBox( "Use bias to prevent divide by zero" );
	private JLabel lblFileLearn = new JLabel( "Input data file:" );
	private JButton btnBrowseLearn = new JButton("Browse");
	private JTextField tfBrowseLearn = new JTextField (10);
	protected JButton btnLearn = new JButton( STR_TEXT_BUTTON_LEARN );

	// Go/Cancel Panel
	protected JPanel pnlButtons = new JPanel();
	protected JButton btnCancel = new JButton( "Close" );

	protected JPanel parameterPane = new JPanel();
	protected File myCaseFileInput = null;
	protected NetworkInternalFrame hnInternalFrame = null;
	protected Object mySynchronization = new Object();
	private JOptionResizeHelper.JOptionResizeHelperListener myListener;
	private JPanel myPanelMain;

	/**
		Test debug method.
		@author Keith Cascio
		@since 051502
	*/
	public static void main( String[] args )
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}
		final EMLearningDlg DLG = new EMLearningDlg( (Frame)null, (NetworkInternalFrame)null, (JOptionResizeHelper.JOptionResizeHelperListener)null );
		//DLG.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );//jdk1.4
		DLG.addWindowListener( new WindowAdapter()
		{
			public void windowClosed(WindowEvent e)
			{
				Util.STREAM_TEST.println( DLG.getSize() );
				System.exit(0);
			}
		});
		Util.centerWindow( DLG );
		DLG.show();
	}

   	public EMLearningDlg( Frame f, NetworkInternalFrame hn, JOptionResizeHelper.JOptionResizeHelperListener listener )
   	{
		super( f, "EM Learning", true);//modal dialog

		this.hnInternalFrame = hn;
		this.myListener = listener;

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		Border padding = BorderFactory.createEmptyBorder(16,8,16,8);//(32,32,16,32);(top,left,bottom,right)
		/*
		Border padding = BorderFactory.createLineBorder(Color.black,16);//debug
		lblThreshold.setBorder( BorderFactory.createLineBorder(Color.red,1) );//debug
		lblFileLearn.setBorder( BorderFactory.createLineBorder(Color.red,1) );//debug
		*/
		Component strut = null;

		// Set up a panel for the btnLearn/btnCancel buttons
		//pnlButtons.setBorder( padding );
		//pnlButtons.setLayout( new GridLayout( 1, btnLearn.getWidth() + btnCancel.getWidth() ) );
		//pnlButtons.add( btnLearn );
		pnlButtons.add( btnCancel );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		myPanelMain = new JPanel( gridbag );

		//c.gridx = GridBagConstraints.RELATIVE;
		//c.gridy = (int)0;
		c.ipadx = (int)0;
		c.ipady = (int)0;
		c.weightx = (double)0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = GridBagConstraints.REMAINDER;
		//strut = Box.createVerticalStrut( 16 );
		//gridbag.setConstraints( strut, c );
		//myPanelMain.add( strut );

		JComponent pnlLearn = makeLearnComponent();
		myPanelMain.add( pnlLearn, c );

		//strut = Box.createVerticalStrut( 16 );
		//myPanelMain.add( strut, c );
		//JComponent pnlSimulate = makeSimulateComponent();
		//myPanelMain.add( pnlSimulate, c );

		//c.gridy++;
		c.weightx = (double)0;
		c.weighty = (double)0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = GridBagConstraints.REMAINDER;
		strut = Box.createVerticalStrut( 16 );
		myPanelMain.add( strut, c );

		//c.gridy++;
		c.weighty = (double)0;
		myPanelMain.add( pnlButtons, c );

		//c.gridy++;
		//strut = Box.createVerticalStrut( 16 );
		//gridbag.setConstraints( strut, c );
		//myPanelMain.add( strut );

		myPanelMain.setBorder( padding );
		getContentPane().add( myPanelMain );

		btnCancel.addActionListener( this );

		initMenu();

		pack();
		if( f != null) {
		    setLocationRelativeTo( f );
		}
		//setSize( 400, 150 );
		Dimension dim = new Dimension( myPanelMain.getPreferredSize() );
		dim.width += 64;
		dim.height += 32;
		setSize( dim );
	}

	/** @since 021505 */
	public void setVisible( boolean flag ){
		if( flag && (myListener != null) ){
			JOptionResizeHelper helper = new JOptionResizeHelper( myPanelMain, true, (long)60000 );
			helper.setListener( myListener );
			helper.start();
		}
		super.setVisible( flag );
	}

	/** @since 021505 */
	public void setDataFile( File datafile ){
		if( tfBrowseLearn != null ) tfBrowseLearn.setText( datafile.getAbsolutePath() );
	}

	/** @since 021704 */
	private void initMenu(){
		JMenuBar menuBar = new JMenuBar();

		JMenu menuTools = new JMenu( "Tools" );
		if( hnInternalFrame != null ){
			menuTools.add( hnInternalFrame.getParentFrame().action_SIMULATE );//.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_W, InputEvent.CTRL_MASK ) );
		}
		menuTools.add( ACTION_CODEBANDIT );
		menuBar.add( menuTools );

		setJMenuBar( menuBar );
	}

	public SamiamAction ACTION_CODEBANDIT = new SamiamAction( "Code Bandit", "Code Bandit", 'c', MainToolBar.getIcon( CodeToolInternalFrame.STR_FILENAME_BUTTON_ICON ) ){
		public void actionPerformed( ActionEvent actionevent ){
			EMLearningDlg.this.codeBandit();
		}
	};

	/** @since 060204 */
	public void codeBandit()
	{
		if( hnInternalFrame == null ) return;

		CodeToolInternalFrame ctif = hnInternalFrame.getCodeToolInternalFrame();
		if( ctif == null ) return;

		EMCoder emcoder = setupEMCoder();
		if( emcoder == null ) return;

		ctif.setCodeGenius( emcoder );

		doCancel();
		ctif.setVisible( true );
	}

	/** @since 060204 */
	public EMCoder setupEMCoder()
	{
		boolean err = false;
		String errMsg = null;

		double threshold = (double)-1;
		int maxIterations = (int)-1;
		boolean flagWithBias = false;
		String pathDataFile = null;

		try {
			threshold = dfTheshold.getValue();
			maxIterations = wnfIterations.getValue();
			flagWithBias = myCbBias.isSelected();
			pathDataFile = tfBrowseLearn.getText();
			if( (pathDataFile.length() < 1) || (!(new File(pathDataFile).exists())) ){
				err = true;
				errMsg = "Data file \"" + pathDataFile + "\" does not exist.";
			}
		}
		catch( NumberFormatException numberformatexception ){
			err = true;
			errMsg = STR_MESSAGE_NUMBER_FORMAT_ERROR;
		}catch( Exception exception ){
			err = true;
			errMsg = exception.getMessage();
			if( errMsg == null ) errMsg = exception.toString();
		}finally{
			if( err == true ){
				tfBrowseLearn.setText( "" );
				JOptionPane.showMessageDialog( this, errMsg, "Settings Error", JOptionPane.ERROR_MESSAGE );
				return (EMCoder)null;
			}
		}

		EMCoder emcoder = new EMCoder();

		emcoder.set( threshold, maxIterations, flagWithBias );

		String oldHuginName = new String( hnInternalFrame.getFileName() );
		emcoder.setInputNetwork( hnInternalFrame.getBeliefNetwork(), oldHuginName );
		emcoder.setPathDataFile( pathDataFile );
		emcoder.setPathOutputNetworkFile( Learning.renamePathForEmOutput( oldHuginName ) );

		UI ui = hnInternalFrame.getParentFrame();
		emcoder.setDynamator( ui.getDynamator().getCanonicalDynamator() );

		return emcoder;
	}

	public static final String STR_MESSAGE_NUMBER_FORMAT_ERROR = "Please enter valid values for the\nlog-likelihood theshold and maximum iterations.";

	//private JComponent makeSimulateComponent(){
	//	return new SimulationPanel( hnInternalFrame );
	//}

	private JComponent makeLearnComponent()
	{
		Component strut = null;

		Dimension dim = tfBrowseLearn.getPreferredSize();
		dim.width = 100;
		tfBrowseLearn.setPreferredSize( dim );
		btnBrowseLearn.addActionListener( this );

		dim = dfTheshold.getPreferredSize();
		dim.width = 75;
		dfTheshold.setMinimumSize( dim );

		dim = wnfIterations.getPreferredSize();
		dim.width = 60;
		wnfIterations.setMinimumSize( dim );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel pnlMain = new JPanel( gridbag );

		//c.gridx = GridBagConstraints.RELATIVE;
		//c.gridy = (int)0;
		c.ipadx = (int)0;
		c.ipady = (int)0;
		c.weightx = (double)0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = GridBagConstraints.REMAINDER;

		JComponent pnlBrowse = makeBrowseComponent( lblFileLearn, tfBrowseLearn, btnBrowseLearn );

		c.weightx = (double)1;
		c.fill = GridBagConstraints.BOTH;
		pnlMain.add( pnlBrowse, c );

		//c.gridy++;
		c.weightx = (double)0;
		strut = Box.createVerticalStrut( 16 );
		pnlMain.add( strut, c );

		JComponent pnlArguments = makeArgumentsComponent( lblThreshold, dfTheshold, lblIterations, wnfIterations );

		//c.gridy++;
		c.weightx = (double)1;
		pnlMain.add( pnlArguments, c );

		c.weightx = (double)0;
		strut = Box.createVerticalStrut( 8 );
		pnlMain.add( strut, c );

		pnlMain.add( myCbBias, c );

		c.weightx = (double)0;
		strut = Box.createVerticalStrut( 16 );
		pnlMain.add( strut, c );

		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		c.weightx = (double)0;
		pnlMain.add( btnLearn, c );

		btnLearn.addActionListener( this );

		Border titled = BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Learn" );
		pnlMain.setBorder( BorderFactory.createCompoundBorder( titled, BorderFactory.createEmptyBorder( 4,4,4,4 ) ) );

		return pnlMain;
    	}

    	public static JComponent makeBrowseComponent( JComponent label1, JComponent textfield1, JComponent button1 )
    	{
		JPanel pnlBrowse = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		pnlBrowse.setLayout( gridbag );
		//pnlBrowse.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );//debug

		c.gridx = GridBagConstraints.RELATIVE;
		c.gridy = (int)0;
		c.ipadx = (int)0;
		c.ipady = (int)0;
		c.weightx = (double)0;
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		pnlBrowse.add( label1, c );

		c.weightx = (double)1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets( 0,8,0,8 );
		pnlBrowse.add( textfield1, c );

		c.weightx = (double)0;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets( 0,0,0,0 );
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlBrowse.add( button1, c );

		return pnlBrowse;
	}

    	public static JComponent makeArgumentsComponent( JComponent label1, JComponent textfield1, JComponent lable2, JComponent textfield2 )
    	{
		JPanel pnlArguments = new JPanel( new GridBagLayout() );
		GridBagConstraints contraintArgs = new GridBagConstraints();
		//pnlArguments.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );//debug

		contraintArgs.gridx = GridBagConstraints.RELATIVE;
		contraintArgs.gridy = (int)0;
		contraintArgs.ipadx = (int)0;
		contraintArgs.ipady = (int)0;
		contraintArgs.weightx = (double)0;
		contraintArgs.insets = new Insets(0,0,0,0);
		contraintArgs.fill = GridBagConstraints.NONE;
		contraintArgs.gridwidth = 1;
		contraintArgs.anchor = GridBagConstraints.WEST;
		pnlArguments.add( label1, contraintArgs );

		pnlArguments.add( Box.createHorizontalStrut( 8 ), contraintArgs );

		contraintArgs.fill = GridBagConstraints.HORIZONTAL;
		pnlArguments.add( textfield1, contraintArgs );

		contraintArgs.fill = GridBagConstraints.NONE;
		contraintArgs.weightx = (double)1;
		pnlArguments.add( Box.createHorizontalStrut( 45 ), contraintArgs );

		contraintArgs.weightx = (double)0;
		pnlArguments.add( lable2, contraintArgs );

		pnlArguments.add( Box.createHorizontalStrut( 8 ), contraintArgs );

		contraintArgs.fill = GridBagConstraints.HORIZONTAL;
		contraintArgs.anchor = GridBagConstraints.EAST;
		pnlArguments.add( textfield2, contraintArgs );

		//dim = new Dimension( pnlArguments.getPreferredSize() );
		//pnlArguments.setMinimumSize( dim );

		return pnlArguments;
	}

	protected static InflibFileFilter theFileFilter = new InflibFileFilter( new String[]{ ".dat" }, "Hugin Case Files (*.dat)" );

	private void browseFileLearn()
	{
		setButtonsEnabled( false );

		File DEFAULT_DIR = ( hnInternalFrame == null ) ? new File( "." ) : hnInternalFrame.getPackageOptions().defaultDirectory;//Keith Cascio 052002

		JFileChooser fileChooser = new JFileChooser( DEFAULT_DIR );
		fileChooser.addChoosableFileFilter( theFileFilter );
		fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setAcceptAllFileFilterUsed(false);

		int result = fileChooser.showOpenDialog( this );
		if( result == JFileChooser.APPROVE_OPTION )
		{
			myCaseFileInput = fileChooser.getSelectedFile();
			tfBrowseLearn.setText( myCaseFileInput.getAbsolutePath() );
		}

		setButtonsEnabled( true );
	}

	//protected void browseFileSimulate()

	private ProgressMonitor myProgressMonitor = null;

	private void goLearn()
	{
		boolean err = false;
		String errMsg = null;

		UI ui = hnInternalFrame.getParentFrame();
		Dynamator dynamator = ui.getDynamator();
		if( !dynamator.getCanonicalDynamator().probabilitySupported() ){
			errMsg = "Dynamator \"" + dynamator.getDisplayName() + "\" does not support pr(e)";
			JOptionPane.showMessageDialog( this, errMsg, "EM Learning Error", JOptionPane.ERROR_MESSAGE );
			return;
		}

		//if( myCaseFileInput == null )
		myCaseFileInput = new File( tfBrowseLearn.getText() );

		if( !myCaseFileInput.exists() )
		{
			errMsg = "Please specify a valid data file.";
			JOptionPane.showMessageDialog( this, errMsg, "EM Learning Error", JOptionPane.ERROR_MESSAGE );
			return;
		}

		// Change the cursor to the hourglass
		setButtonsEnabled( false );
		edu.ucla.belief.ui.util.Util.pushStatusWest( hnInternalFrame, STR_MSG_LEARN );
		setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ));

		BeliefNetwork beliefNetwork = null;
		LearningData learningData2 = null;

		myProgressMonitor = new ProgressMonitor( this, "Learning:", "reading " + myCaseFileInput.getPath(), 0, 10 );
		myProgressMonitor.setProgress(1);
		myProgressMonitor.setMillisToDecideToPopup(1);
		myProgressMonitor.setMillisToPopup(1);

		double threshold = (double)-1;
		int maxiterations = (int)-1;

		// Attempt to open the file and read in the data
		try {
			threshold = dfTheshold.getValue();
			maxiterations = wnfIterations.getValue();
			//beliefNetwork = (BeliefNetwork ) hnInternalFrame.getBeliefNetwork();
			if( Util.DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "EMLearningDlg cloning " + hnInternalFrame.getBeliefNetwork().getClass().getName() );
			beliefNetwork = (BeliefNetwork) hnInternalFrame.getBeliefNetwork().clone();
			if( Util.DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "EMLearningDlg clone instanceof " + beliefNetwork.getClass().getName() );

			// create the learning data object.
			learningData2 = new LearningData();
			learningData2.readData( myCaseFileInput, beliefNetwork );
		}
		catch( NumberFormatException e )
		{
			err = true;
			errMsg = STR_MESSAGE_NUMBER_FORMAT_ERROR;
		}
		catch( FileNotFoundException fnfe ) {
			err = true;
			errMsg = "Cannot find file named: " + myCaseFileInput.getName() + "!";
		}
		catch( RuntimeException rte ) {
			err = true;
			errMsg = "Error: Unable to learn parameters from " + myCaseFileInput.getName() + "!\n" + rte.getMessage();
		}
		catch( Exception e ) {
			err = true;
			errMsg = e.getMessage();
		}
		finally{
			if( err == true )
			{
				myCaseFileInput = null;
				tfBrowseLearn.setText( "" );
				setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
				setButtonsEnabled( true );
				edu.ucla.belief.ui.util.Util.popStatusWest( hnInternalFrame, STR_MSG_LEARN );

				JOptionPane.showMessageDialog( this, errMsg, "Learning Error", JOptionPane.ERROR_MESSAGE );
				return;
			}
		}

		myProgressMonitor.setProgress( 5 );

		//System.out.println( "\tinput vertices():\n" + beliefNetwork.vertices() );

		// No exceptions occurred. Now we attempt to learn the parameters from the
		//  data set. This sets the EM Learning Algorithm in motion.
		Learning.learnParamsEM( beliefNetwork, learningData2, dynamator,
					threshold,
					maxiterations,
					myCbBias.isSelected(),
					myProgressMonitor,
					this );
		/*
		BeliefNetwork new_bn =
			Learning.learnParamsEM( beliefNetwork, learningData,
						Double.parseDouble( dfTheshold.getText() ),
						Integer.parseInt( wnfIterations.getText() ),
						myProgressMonitor
		);

		String oldHuginName = new String( hnInternalFrame.getFileName() );
		String newHuginName = new String( "" );
		StringTokenizer tokens = new StringTokenizer( oldHuginName, "\\" );

		while( tokens.countTokens() > 1 )
			newHuginName = new String( newHuginName + tokens.nextToken() + "\\" );
		newHuginName = new String( newHuginName + "EM_" + tokens.nextToken() );

		myProgressMonitor.setMinimum(0);
		myProgressMonitor.setMaximum(10);
		myProgressMonitor.setNote( "creating " + newHuginName );

		try {
			NetworkInternalFrame newFrame
				= new NetworkInternalFrame( hnInternalFrame.getParentFrame(),
						     	     hnInternalFrame.getHuginNet(),
						     	     new_bn,
							     newHuginName
			);
		}catch( Exception e ) {
			System.err.println( "Error in NetworkInternalFrame():" + e.toString() );
		}

		myProgressMonitor.setProgress( 5 );

		// Change the cursor back to the arrow
		setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ));
		myProgressMonitor.close();
		dispose();
		*/
	}

	/** @since 090302 */
	public static File findFileFromPath( String EMPath )
	{
		int index_last_separator = EMPath.lastIndexOf( File.separator );
		if( index_last_separator < (int)0 ) index_last_separator = (int)0;
		String filePath = EMPath.substring( (int)0, index_last_separator );
		String fileName = EMPath.substring( index_last_separator+1 );
		//System.out.println( "path:" + filePath + "\nname:" + fileName );
		if( fileName.startsWith( Learning.STR_EM_FILENAME_PREFIX ) )
		{
			String reconstructed = filePath + File.separator + fileName.substring( Learning.STR_EM_FILENAME_PREFIX.length() );
			//System.out.println( "reconstructed:" + reconstructed );
			File ret = new File( reconstructed );
			if( ret.exists() ) return ret;
			else return null;
		}
		else return null;
	}

	/** interface EMThread.LearningListener
		@since 20020520 */
	public void setBeliefNetwork( BeliefNetwork new_bn )
	{
		//System.out.println( "EM: output belief network:\n" + new_bn );//debug

		String oldHuginName = new String( hnInternalFrame.getFileName() );
		String newHuginName = Learning.renamePathForEmOutput( oldHuginName );

		myProgressMonitor.setMinimum(0);
		myProgressMonitor.setMaximum(10);
		myProgressMonitor.setNote( "creating " + newHuginName );

		NetworkInternalFrame newFrame = null;
		try {
			//new_bn.userobject = hnInternalFrame.getHuginNet();
			newFrame
				= new NetworkInternalFrame(	new_bn,
								hnInternalFrame.getParentFrame(),
								newHuginName );
		}catch( Exception e ) {
			System.err.println( "Error in NetworkInternalFrame():" + e.toString() );
			if( Util.DEBUG_VERBOSE )
			{
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
		}

		try{
			if( newFrame != null ){
				DisplayableBeliefNetwork dbn = newFrame.getBeliefNetwork();
				DisplayableFiniteVariable dvar;
				for( DFVIterator it = dbn.dfvIterator(); it.hasNext(); ){
					dvar = it.nextDFV();
					dvar.setCPTShell( dvar.getCPTShell() );
				}
			}
		}catch( Throwable throwable ){
			System.err.println( "Warning in EMLearningDlg.setBeliefNetwork(): " + throwable );
		}

		myProgressMonitor.setProgress( 5 );

		if( myProgressMonitor.isCanceled() )
		{
			JOptionPane.showMessageDialog( this, "A progress monitor was canceled.  This has no effect.", "Progress Monitor Warning", JOptionPane.WARNING_MESSAGE );
		}

		myProgressMonitor.close();
		myProgressMonitor = null;

		// Change the cursor back to the arrow
		setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ));
		setButtonsEnabled( true );
		edu.ucla.belief.ui.util.Util.popStatusWest( hnInternalFrame, STR_MSG_LEARN );

		dispose();
	}

	/** interface EMThread.LearningListener
		@since 20020520 */
	public void handleLearningError( String msg )
	{
		myProgressMonitor.setProgress( 0 );
		myProgressMonitor.close();
		myProgressMonitor = null;

		// Change the cursor back to the arrow
		setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ));
		setButtonsEnabled( true );
		edu.ucla.belief.ui.util.Util.popStatusWest( hnInternalFrame, STR_MSG_LEARN );

		JOptionPane.showMessageDialog( this, msg, "Learning Error", JOptionPane.ERROR_MESSAGE );
	}

	/** interface EMThread.LearningListener
		@since 20060719 */
	public ThreadGroup getThreadGroup(){
		return (hnInternalFrame == null) ? null : hnInternalFrame.getThreadGroup();
	}

	private void doCancel()
	{
		setButtonsEnabled( false );
		dispose();
	}

	//protected void goSimulate()
	//protected File myOutputCaseFile;
	//public void simulationDone( LearningData data )
	//public void simulationError( String message )
	//protected void goSimulateError( String errMsg )

	/** @since 031803 */
	protected void setButtonsEnabled( boolean flag )
	{
		btnLearn.setEnabled( flag );
		btnCancel.setEnabled( flag );
		btnBrowseLearn.setEnabled( flag );
		//btnSimulate.setEnabled( flag );
		//btnBrowseSimulate.setEnabled( flag );
	}

	public void actionPerformed( ActionEvent e )
	{
		synchronized( mySynchronization )
		{
			Object src = e.getSource();
			if( src == btnBrowseLearn ) { browseFileLearn(); 	}
			else if ( src == btnLearn ) { goLearn(); 	}
			else if ( src == btnCancel ) { doCancel(); 	}
			//else if( src == btnSimulate ) { goSimulate(); }
			//else if( src == btnBrowseSimulate ) { browseFileSimulate(); }
		}
	}
}
