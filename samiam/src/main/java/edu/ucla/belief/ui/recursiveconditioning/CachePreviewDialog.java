package edu.ucla.belief.ui.recursiveconditioning;

import edu.ucla.belief.dtree.*;
import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
	@author Keith Cascio
	@since 020703
*/
public class CachePreviewDialog extends JDialog implements CachingScheme.RCCreateListener
{
	public CachePreviewDialog( Settings settings, BeliefNetwork bn, boolean modal )//, RC rc )
	{
		super( (Frame)null, modal );
		init( settings, bn );//, rc );
	}

	/**
		@author Keith Cascio
		@since 053003
	*/
	public void setVisible( boolean visible )
	{
		theVisibleCachePreviewDialog = visible ? this : null;
		super.setVisible( visible );
	}

	public static CachePreviewDialog theVisibleCachePreviewDialog;

	public void rcCreateUpdate( double bestCost )
	{
		//System.out.println( "CachePreviewDialog.rcCreateUpdate("+bestCost+")..." );
		myComputation.setExpectedNumberOfRCCalls( bestCost );
		updateEstimatedMinutesDisplay( myComputation, myLabelEstimatedTime_All );
	}
	public double rcCreateUpdateThreshold(){ return (double)1024; }
	public boolean rcCreateStopRequested(){ return myFlagStop; }
	public void rcCreateDone( double bestCost, boolean optimal ){}
	public void rcCreateDone( RC rc )
	{
		//System.out.println( "CachePreviewDialog.rcCreateDone( "+rc+" )" );
		if( rc == null ) dispose();
		else
		{
			myFlagPerformedSearch = true;
			//updateEstimatedMinutesDisplay_Pe( rc );
			//updateEstimatedMinutesDisplay_All( rc );
			myCachePreviewAction.setOK( true );
			mySettings.refresh( rc );
			Bundle bundle = mySettings.getBundle();
			updateEstimatedMinutesDisplay( bundle.getPe(), myLabelEstimatedTime_Pe );
			updateEstimatedMinutesDisplay( bundle.getAll(), myLabelEstimatedTime_All );
		}
	}

	public void rcConstructionDone(){
		//System.out.println( "CachePreviewDialog.rcConstructionDone()" );
		myCachePreviewAction.rcConstructionDone();
	}

	public void rcCreateError( String msg ){
		System.err.println( msg );
		dispose();
	}

	/**
		@author Keith Cascio
		@since 070703
	*/
	public boolean performedSearch()
	{
		return myFlagPerformedSearch;
	}

	protected void updateEstimatedMinutesDisplay( Computation comp, JLabel label )
	{
		String[] newVals = comp.updateEstimatedMinutesDisplay();
		label.setText( newVals[0] + " " + newVals[1] );
	}

	protected void init( Settings settings, BeliefNetwork bn )//, RC rc )
	{
		//System.out.println( "CachePreviewDialog.init()..." );

		setTitle( "Cache Preview" );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );

		RC rc = null;

		mySettings = settings;
		boolean flagDtree = false;
		if( mySettings != null )
		{
			//System.out.println( "mySettings.setRCCreateListener( this )" );
			mySettings.setRCCreateListener( this );
			rc = mySettings.getRC();
			flagDtree = mySettings.getDtree() != null;
		}

		myComputation = new Computation.All( rc );

		boolean flagNotAllocating = (settings==null) ? true : !settings.getBundle().isStale();

		initEstimatedTimeDisplay( flagNotAllocating, settings, rc );
		if( flagDtree ) initDtreeStats( settings );
		initMemoryDisplay( settings );
		initButton( flagNotAllocating );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel pnlMain = new JPanel( gridbag );
		Component strut;

		c.gridwidth = GridBagConstraints.REMAINDER;

		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints( myPanelEstimatedTime, c );
		pnlMain.add( myPanelEstimatedTime );

		strut = Box.createVerticalStrut( 16 );
		gridbag.setConstraints( strut, c );
		pnlMain.add( strut );

		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints( myLabelMemoryDisplay, c );
		pnlMain.add( myLabelMemoryDisplay );

		strut = Box.createVerticalStrut( 16 );
		gridbag.setConstraints( strut, c );
		pnlMain.add( strut );

		if( flagDtree )
		{
			c.fill = GridBagConstraints.HORIZONTAL;
			pnlMain.add( myPanelDtreeStats, c );

			strut = Box.createVerticalStrut( 16 );
			pnlMain.add( strut, c );
		}

		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints( myButton, c );
		pnlMain.add( myButton );

		pnlMain.setBorder( BorderFactory.createEmptyBorder( 16,16,16,16 ) );

		getContentPane().add( pnlMain );

		pack();

		//if( mySettings != null && settings.getBundle().isStale() ) settings.allocRCDgraphInThread( bn );//settings.createRCDgraphInThread( bn );
		if( mySettings != null && bn != null ) mySettings.validateRC( bn );
	}

	public boolean myFlagStop = false;

	public void stop()
	{
		synchronized( mySynchronization )
		{

		//System.out.println( "stop()" );
		myFlagStop = true;
		myCachePreviewAction.setOK( true );

		}
	}

	public void ok()
	{
		synchronized( mySynchronization )
		{

		if( flagNotOKCalled )
		{
			//System.out.println( "ok()" );
			flagNotOKCalled = false;
			dispose();
			if( FLAG_TEST ) System.exit(0);
		}

		}
	}

	protected boolean flagNotOKCalled = true;

	public class CachePreviewAction extends AbstractAction implements WindowListener
	{
		public CachePreviewAction( boolean okay )
		{
			setOK( okay );
			CachePreviewDialog.this.addWindowListener( this );
			setEnabled( myFlagOK );
		}

		public void rcConstructionDone()
		{
			setEnabled( true );
		}

		public void actionPerformed( ActionEvent e )
		{
			if( myFlagOK ) ok();
			else stop();
		}

		public void setOK( boolean flag )
		{
			myFlagOK = flag;

			String text = myFlagOK ? STR_TEXT_OK : STR_TEXT_STOP;
			super.putValue( Action.NAME, text );
			super.putValue( Action.SHORT_DESCRIPTION, text );
		}

		public void windowOpened(WindowEvent e){}
		public void windowClosing(WindowEvent e)
		{
			//System.out.println( "windowClosing()" );
			myButton.doClick();
		}
		public void windowClosed(WindowEvent e)
		{
			//System.out.println( "windowClosed()" );
			ok();
		}
		public void windowIconified(WindowEvent e){}
		public void windowDeiconified(WindowEvent e){}
		public void windowActivated(WindowEvent e){}
		public void windowDeactivated(WindowEvent e){}

		protected boolean myFlagOK;
	};

	protected void initButton( boolean okay )
	{
		myCachePreviewAction = new CachePreviewAction( okay );
		myButton = new JButton( myCachePreviewAction );
	}

	protected CachePreviewAction myCachePreviewAction;
	protected JButton myButton;

	public static final String STR_TEXT_OK = "OK";
	public static final String STR_TEXT_STOP = "Stop";
	public static final String STR_TEXT_UNAVAILABLE = ";";

	protected void initEstimatedTimeDisplay( boolean flagNotAllocating, Settings settings, RC rc )
	{
		myLabelEstimatedCaption_Pe = new JLabel( "Estimated runtime, Pr(e):", JLabel.LEFT );
		myLabelEstimatedTime_Pe = new JLabel( STR_TEXT_UNAVAILABLE );
		myLabelEstimatedCaption_All = new JLabel( "Estimated runtime, marginals:", JLabel.LEFT );
		myLabelEstimatedTime_All = new JLabel( STR_TEXT_UNAVAILABLE );
		//lblEstimatedTimeUnits = new JLabel( Settings.STR_MINUTE_UNIT, JLabel.LEFT );

		//Dimension dim  = lblEstimatedTimeUnits.getPreferredSize();
		//dim.width = 75;
		//lblEstimatedTimeUnits.setPreferredSize( dim );

		if( flagNotAllocating && rc != null && settings != null )
		{
			updateEstimatedMinutesDisplay( mySettings.getBundle().getPe(), myLabelEstimatedTime_Pe );
			updateEstimatedMinutesDisplay( mySettings.getBundle().getAll(), myLabelEstimatedTime_All );
			//String[] formatted = settings.updateEstimatedMinutesDisplay( rc );
			//myLabelEstimatedTime_Pe.setText( formatted[0] + " " + formatted[1] );
		}

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		myPanelEstimatedTime = new JPanel( gridbag );

		Component strut;
		c.weightx = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints( myLabelEstimatedCaption_Pe, c );
		myPanelEstimatedTime.add( myLabelEstimatedCaption_Pe );

		strut = Box.createHorizontalStrut( 16 );
		c.weightx = 1;
		gridbag.setConstraints( strut, c );
		myPanelEstimatedTime.add( strut );

		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myLabelEstimatedTime_Pe, c );
		myPanelEstimatedTime.add( myLabelEstimatedTime_Pe );

		c.weightx = 0;
		c.gridwidth = 1;
		gridbag.setConstraints( myLabelEstimatedCaption_All, c );
		myPanelEstimatedTime.add( myLabelEstimatedCaption_All );

		strut = Box.createHorizontalStrut( 16 );
		c.weightx = 1;
		gridbag.setConstraints( strut, c );
		myPanelEstimatedTime.add( strut );

		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myLabelEstimatedTime_All, c );
		myPanelEstimatedTime.add( myLabelEstimatedTime_All );

		//c.weightx = 0.1;
		//c.fill = GridBagConstraints.HORIZONTAL;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		//gridbag.setConstraints( lblEstimatedTimeUnits, c );
		//myPanelEstimatedTime.add( lblEstimatedTimeUnits );
	}

	protected JComponent myPanelEstimatedTime;

	protected JLabel myLabelEstimatedCaption_Pe;
	protected JLabel myLabelEstimatedTime_Pe;
	protected JLabel myLabelEstimatedCaption_All;
	protected JLabel myLabelEstimatedTime_All;
	//protected JLabel lblEstimatedTimeUnits;

	protected void initDtreeStats( Settings settings )
	{
		myPanelDtreeStats = makeDtreeStatsPanel();
		if( settings != null ) updateDtreeStatsDisplay( settings );
	}

	protected JComponent myPanelDtreeStats;

	protected void initMemoryDisplay( Settings settings )
	{
		String text = "(using 1000 MB out of -10000)";
		if( settings != null )
		{
			//String[] formatted = settings.updateOptimalMemoryDisplay();
			//String userFormatted = settings.updateUserMemoryDisplay();
			//text = "(using " + userFormatted + " " + formatted[1] + " out of " + formatted[0] + ")";
			text = "(" + settings.describeUserMemoryProportion() + ")";
		}
		myLabelMemoryDisplay = new JLabel( text );
	}

	protected JLabel myLabelMemoryDisplay;

	protected JLabel myLabelDtreeHeight;
	protected JLabel myLabelDtreeMaxCluster;
	protected JLabel myLabelDtreeMaxCutset;
	protected JLabel myLabelDtreeMaxContext;

	/**
		@author Keith Cascio
		@since 082202
	*/
	protected void updateDtreeStatsDisplay( Settings settings )
	{
		myLabelDtreeHeight.setText( String.valueOf( settings.getDtreeHeight() ) );
		myLabelDtreeMaxCluster.setText( String.valueOf( settings.getDtreeMaxCluster() ) );
		myLabelDtreeMaxCutset.setText( String.valueOf( settings.getDtreeMaxCutset() ) );
		myLabelDtreeMaxContext.setText( String.valueOf( settings.getDtreeMaxContext() ) );
	}

	/**
		@author Keith Cascio
		@since 082202
	*/
	protected JComponent makeDtreeStatsPanel()
	{
		JLabel lblDtreeStatsCaption = new JLabel( " height = " );
		JLabel lblClusterCaption = new JLabel( " max cluster = " );
		JLabel lblCutsetCaption = new JLabel( " cutset = " );
		JLabel lblContextCaption = new JLabel( " context = " );
		myLabelDtreeHeight = new JLabel( STR_TEXT_UNAVAILABLE );
		Dimension dim = myLabelDtreeHeight.getPreferredSize();
		dim.width = 16;
		myLabelDtreeHeight.setPreferredSize( dim );
		myLabelDtreeMaxCluster = new JLabel( STR_TEXT_UNAVAILABLE );
		myLabelDtreeMaxCluster.setPreferredSize( dim );
		myLabelDtreeMaxCutset = new JLabel( STR_TEXT_UNAVAILABLE );
		myLabelDtreeMaxCutset.setPreferredSize( dim );
		myLabelDtreeMaxContext = new JLabel( STR_TEXT_UNAVAILABLE );
		myLabelDtreeMaxContext.setPreferredSize( dim );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel ret = new JPanel( gridbag );

		c.gridwidth =(int)1;
		c.weightx = 0;
		gridbag.setConstraints( lblDtreeStatsCaption, c );
		ret.add( lblDtreeStatsCaption );

		c.weightx = 1;
		gridbag.setConstraints( myLabelDtreeHeight, c );
		ret.add( myLabelDtreeHeight );

		c.weightx = 0;
		gridbag.setConstraints( lblClusterCaption, c );
		ret.add( lblClusterCaption );

		c.weightx = 1;
		gridbag.setConstraints( myLabelDtreeMaxCluster, c );
		ret.add( myLabelDtreeMaxCluster );

		c.weightx = 0;
		gridbag.setConstraints( lblCutsetCaption, c );
		ret.add( lblCutsetCaption );

		c.weightx = 1;
		gridbag.setConstraints( myLabelDtreeMaxCutset, c );
		ret.add( myLabelDtreeMaxCutset );

		c.weightx = 0;
		gridbag.setConstraints( lblContextCaption, c );
		ret.add( lblContextCaption );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		gridbag.setConstraints( myLabelDtreeMaxContext, c );
		ret.add( myLabelDtreeMaxContext );

		Border b = null;
		if( FLAG_DEBUG_BORDERS ) b = BorderFactory.createLineBorder( Color.red, (int)1 );
		else b = BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Current Dtree Properties" );

		ret.setBorder( b );

		return ret;
	}

	/**
		test/debug
	*/
	public static void main( String[] args )
	{
		FLAG_TEST = true;

		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		CachePreviewDialog CPD = new CachePreviewDialog( null, null, true );
		CPD.setVisible( true );
	}

	protected Settings mySettings;
	protected Computation myComputation;
	protected boolean myFlagPerformedSearch = false;
	protected Object mySynchronization = new Object();

	public static boolean FLAG_DEBUG_BORDERS = false;
	public static boolean FLAG_TEST = false;
}
