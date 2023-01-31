package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.learn.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.InflibFileFilter;
import edu.ucla.belief.ui.statusbar.StatusBar;

import javax.swing.ProgressMonitor;
import java.awt.*;
import java.awt.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;
import java.text.*;

/** @author keith cascio
	@since 20040217 */
public class SimulationPanel extends JPanel implements ActionListener
{
	public static final String STR_MSG_SIMULATE = "simulating data...";

	public SimulationPanel( NetworkInternalFrame nif )
	{
		super( new GridBagLayout() );
		this.myNetworkInternalFrame = nif;
		init();
	}

	protected JComponent init()//makeSimulateComponent()
	{
		Dimension dim = tfBrowseSimulate.getPreferredSize();
		dim.width = 0x100;
		tfBrowseSimulate.setPreferredSize( dim );
		btnBrowseSimulate.addActionListener( this );

		dim = dfPercentMissing.getPreferredSize();
		dim.width = 60;
		dfPercentMissing.setMinimumSize( dim );

		dim = wnfNumCases.getPreferredSize();
		dim.width = 80;
		wnfNumCases.setMinimumSize( dim );

		//GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel pnlMain = this;//JPanel pnlMain = new JPanel( gridbag );

		//c.gridx = GridBagConstraints.RELATIVE;
		//c.gridy = (int)0;
		c.ipadx     = 0;
		c.ipady     = 0;
		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		c.anchor    = GridBagConstraints.WEST;
		c.gridwidth = GridBagConstraints.REMAINDER;

		//c.weightx = (double)1;
		//pnlMain.add( makeNumFilesComponent(), c );
		//c.weightx = (double)0;
		//pnlMain.add( Box.createVerticalStrut( 16 ), c );

		//JComponent pnlBrowse = EMLearningDlg.makeBrowseComponent( new JLabel( "Ouput file(s) path:" ), tfBrowseSimulate, btnBrowseSimulate );

		c.weightx = (double)1;
		c.fill = GridBagConstraints.BOTH;
		//pnlMain.add( pnlBrowse, c );
		pnlMain.add( makeBrowseComponent(), c );

		//c.gridy++;
		c.weightx = (double)0;
		pnlMain.add( Box.createVerticalStrut( 16 ), c );
		pnlMain.add( Box.createHorizontalStrut( 0x200 ), c );

		wnfNumCases.setHorizontalAlignment( JTextField.RIGHT );
		dfPercentMissing.setHorizontalAlignment( JTextField.RIGHT );
		JComponent pnlArguments = EMLearningDlg.makeArgumentsComponent( new JLabel( "Number of cases per file:" ), wnfNumCases, new JLabel( "Fraction missing values:" ), dfPercentMissing );

		//c.gridy++;
		c.weightx = (double)1;
		pnlMain.add( pnlArguments, c );

		c.weightx = (double)0;
		pnlMain.add( Box.createVerticalStrut( 16 ), c );

		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		c.weightx = (double)0;
		pnlMain.add( btnSimulate, c );

		btnSimulate.addActionListener( this );

		Border titled = BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Simulate" );
		pnlMain.setBorder( BorderFactory.createCompoundBorder( titled, BorderFactory.createEmptyBorder( 4,4,4,4 ) ) );

		return pnlMain;
	}

	/** @since 20070205 */
	private JComponent makeBrowseComponent(){
		wnfNumFiles.setHorizontalAlignment( JTextField.RIGHT );

		JPanel panel = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		c.gridwidth = 1;
		panel.add( new JLabel( "Write" ), c );
		panel.add( Box.createHorizontalStrut( 8 ), c );
		panel.add( wnfNumFiles, c );
		panel.add( Box.createHorizontalStrut( 8 ), c );
		panel.add( new JLabel( "case file(s), named like" ), c );
		panel.add( Box.createHorizontalStrut( 8 ), c );
		c.weightx   = 1;
		c.fill      = GridBagConstraints.HORIZONTAL;
		panel.add( tfBrowseSimulate,  c );
		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		panel.add( Box.createHorizontalStrut( 4 ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		panel.add( btnBrowseSimulate, c );

		return panel;
	}

	private void browseFileSimulate()
	{
		setButtonsEnabled( false );

		File DEFAULT_DIR = ( myNetworkInternalFrame == null ) ? new File( "." ) : myNetworkInternalFrame.getPackageOptions().defaultDirectory;//Keith Cascio 052002

		JFileChooser fileChooser = new JFileChooser( DEFAULT_DIR );
		fileChooser.addChoosableFileFilter( theFileFilter );
		fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setAcceptAllFileFilterUsed(false);

		int result = fileChooser.showSaveDialog( this );
		if( result == JFileChooser.APPROVE_OPTION )
		{
			tfBrowseSimulate.setText( theFileFilter.validateExtension( fileChooser.getSelectedFile() ).getAbsolutePath() );
		}

		setButtonsEnabled( true );
	}

	protected void goSimulate()
	{
		boolean err = false;
		String errMsg = null;

		try{

		String path = tfBrowseSimulate.getText().trim();
		if( path.length() < (int)1 ){
			err = true;
			errMsg = "Please choose an output data file.";
			return;//finally
		}
		else myOutputCaseFile = new File( path );

		if( myOutputCaseFile.isFile() && myOutputCaseFile.exists() )
		{
			errMsg = "Overwrite " + myOutputCaseFile.getPath() + "?";
			int result = JOptionPane.showConfirmDialog( this, errMsg, "EM Learning: Overwrite?", JOptionPane.WARNING_MESSAGE );
			if( result != JOptionPane.OK_OPTION ) return;
		}

		double fractionmissing = -1;
		int    numcases        = -1;
		int    numfiles        = -1;
		try {
			fractionmissing = dfPercentMissing.getValue();
			numcases        = wnfNumCases.getValue();
			numfiles        = wnfNumFiles.getValue();
		}
		catch( NumberFormatException e ){
			err = true;
			errMsg = "Please enter valid values for \nnumber of files, number of cases and percent missing.";
			return;//finally
		}
		catch( Exception e ){
			err = true;
			errMsg = e.getMessage();
			return;//finally
		}

		if(      fractionmissing < (double)0 ) fractionmissing = (double)0;
		else if( fractionmissing > (double)1 ) fractionmissing = (double)1;

		Thread thread = new RunSimulation( myNetworkInternalFrame.getBeliefNetwork(), numcases, fractionmissing, numfiles, myOutputCaseFile ).start();

		}
		finally{
			if( err ){
				goSimulateError( errMsg );
				return;
			}
		}
	}

	/** @since 20070205 */
	public static class FilenameIncrementer
	{
		public FilenameIncrementer( File path ){
			if( path.isDirectory() ){
				FilenameIncrementer.this.myParent = path;
				FilenameIncrementer.this.myName   = "simulated_cases.dat";
				FilenameIncrementer.this.myPrefix = "simulated_cases";
				FilenameIncrementer.this.mySuffix =                ".dat";
				FilenameIncrementer.this.myPath   = new File( path, myName );
			}
			else{
				FilenameIncrementer.this.myPath   = path;
				FilenameIncrementer.this.myParent = path.getParentFile();
				FilenameIncrementer.this.myName   = path.getName();
				int indexDot = myName.lastIndexOf( '.' );
				FilenameIncrementer.this.myPrefix = myName.substring( 0, indexDot );
				FilenameIncrementer.this.mySuffix = myName.substring( indexDot );
			}
			FilenameIncrementer.this.increment();
		}

		public File getFile(){
			return myCurrentFile;
		}

		/*public String getNote(){
			return "writing " + myCurrentFile.getName();
		}*/

		public int increment(){
			myCurrentFile = new File( myParent, myPrefix + FORMAT.format( ++myIndex ) + mySuffix );
			return myIndex;
		}

		private File   myPath, myParent;
		private String myName, myPrefix, mySuffix;

		private int  myIndex = -1;
		private File myCurrentFile;

		private static DecimalFormat FORMAT = new DecimalFormat( "###000" );
	}

	/** @since 20070205 */
	public class RunSimulation implements Runnable, Simulator.SimulationListener
	{
		public RunSimulation( BeliefNetwork bn, int numcases, double fractionmissing, int numfiles, File pathout )
		{
			RunSimulation.this.myBeliefNetwork      = bn;
			RunSimulation.this.mynumcases           = numcases;
			RunSimulation.this.mynumfiles           = numfiles;
			RunSimulation.this.myfractionmissing    = fractionmissing;
			RunSimulation.this.myPathOut            = pathout;
		}

		public Thread start(){
			ThreadGroup         group =   SimulationPanel.this.getThreadGroup();
			if( group == null ) group = Thread.currentThread().getThreadGroup();
			Thread ret = new Thread( group, (Runnable) RunSimulation.this, getSimulator().getDescription() );
			ret.start();
			return ret;
		}

		public Simulator getSimulator(){
			if( mySimulator == null ) mySimulator = new Simulator( myBeliefNetwork );
			return mySimulator;
		}

		public void run(){
			try{
				setButtonsEnabled( false );
				setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
				edu.ucla.belief.ui.util.Util.pushStatusWest( myNetworkInternalFrame, STR_MSG_SIMULATE );

				myPollster = new Pollster( "simulating " + mynumcases + " cases X " + mynumfiles + " files", getSimulator(), (Component) SimulationPanel.this );
				myPollster.setThreadToInterrupt( Thread.currentThread() );
				myPollster.start();

				if( mynumfiles  == 1    ) runOne();
				else                      runMany();
			}catch( Exception e ){
				SimulationPanel.this.simulationError( e.toString() );
				e.printStackTrace();
			}finally{
				edu.ucla.belief.ui.util.Util.popStatusWest(  myNetworkInternalFrame, STR_MSG_SIMULATE );
				setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
				setButtonsEnabled( true  );
			}
		}

		public void runMany() throws Exception{
			myFilenameIncrementer = new FilenameIncrementer( myPathOut );
			getSimulator().simulate( mynumcases, myfractionmissing, mynumfiles, (Simulator.SimulationListener) RunSimulation.this );
		}

		public void simulationDone( LearningData data ){
			try{
				data.writeData( myFilenameIncrementer.getFile() );
				myFilenameIncrementer.increment();
			}catch( Exception exception ){
				throw new RuntimeException( exception );
			}
		}

		public void runOne() throws Exception{
			getSimulator().simulate( mynumcases, myfractionmissing ).writeData( myPathOut );
		}

		public Pollster            myPollster;
		public File                myPathOut;
		public FilenameIncrementer myFilenameIncrementer;
		public Simulator           mySimulator;
		public BeliefNetwork       myBeliefNetwork;
		public int                 mynumcases, mynumfiles;
		public double              myfractionmissing;
	}

	/** interface Simulator.SimulationListener */
	public void simulationError( String message )
	{
		goSimulateError( message );
		myOutputCaseFile = null;
	}

	/** @since 20060719 */
	public ThreadGroup getThreadGroup(){
		return (myNetworkInternalFrame == null) ? null : myNetworkInternalFrame.getThreadGroup();
	}

	protected void goSimulateError( String errMsg )
	{
		tfBrowseSimulate.setText( "" );
		setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
		setButtonsEnabled( true );
		edu.ucla.belief.ui.util.Util.popStatusWest( myNetworkInternalFrame, STR_MSG_SIMULATE );

		JOptionPane.showMessageDialog( this, errMsg, "Simulation Error", JOptionPane.ERROR_MESSAGE );
		return;
	}

	public void actionPerformed( ActionEvent e ){
		synchronized( mySynchronization ){
			Object src = e.getSource();
			if(      src == btnSimulate       ) { goSimulate(); }
			else if( src == btnBrowseSimulate ) { browseFileSimulate(); }
		}
	}

	protected void setButtonsEnabled( boolean flag ){
		btnSimulate.setEnabled(       flag );
		btnBrowseSimulate.setEnabled( flag );
	}

	private static InflibFileFilter theFileFilter = new InflibFileFilter( new String[]{ ".dat" }, "Hugin Case Files (*.dat)" );

	private NetworkInternalFrame myNetworkInternalFrame;
	private Object               mySynchronization = new Object();
	private File                 myOutputCaseFile;

	private WholeNumberField     wnfNumCases       = new WholeNumberField( Simulator.INT_DEFAULT_NUM_CASES, 6, Simulator.INT_NUM_CASES_FLOOR, Simulator.INT_NUM_CASES_CEILING );
	private WholeNumberField     wnfNumFiles       = new WholeNumberField( Simulator.INT_NUM_FILES_DEFAULT, 3, Simulator.INT_NUM_FILES_FLOOR, Simulator.INT_NUM_FILES_CEILING );
	private DecimalField         dfPercentMissing  = new DecimalField( Simulator.DOUBLE_DEFAULT_FRACTION_MISSING_VALUES, 5, Simulator.DOUBLE_FRACTION_MISSING_FLOOR, Simulator.DOUBLE_FRACTION_MISSING_CEILING );
	private JButton              btnBrowseSimulate = new JButton( "Browse" );
	private JTextField           tfBrowseSimulate  = new JTextField( 0x10 );
	private JButton              btnSimulate       = new JButton( "Simulate" );
}
