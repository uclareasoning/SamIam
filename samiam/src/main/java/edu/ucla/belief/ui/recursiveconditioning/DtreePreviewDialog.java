package edu.ucla.belief.ui.recursiveconditioning;

import edu.ucla.belief.dtree.*;
import edu.ucla.belief.recursiveconditioning.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
	@author Keith Cascio
	@since 020703
*/
public class DtreePreviewDialog extends JDialog
{
	public DtreePreviewDialog( Settings settings )
	{
		super( (Frame)null, true );
		init( settings );
	}
	
	protected void init( Settings settings )
	{
		setTitle( "Dtree Preview" );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		
		initDtreeStats( settings );
		initButton();
		
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		
		JPanel pnlMain = new JPanel( gridbag );
		Component strut;
		
		c.gridwidth = GridBagConstraints.REMAINDER;
		
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints( myPanelDtreeStats, c );
		pnlMain.add( myPanelDtreeStats );
		
		strut = Box.createVerticalStrut( 16 );
		gridbag.setConstraints( strut, c );
		pnlMain.add( strut );
		
		c.fill = GridBagConstraints.NONE;
		gridbag.setConstraints( myButton, c );
		pnlMain.add( myButton );
		
		pnlMain.setBorder( BorderFactory.createEmptyBorder( 16,16,16,16 ) );
		
		getContentPane().add( pnlMain );
		
		pack();
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
	
	public class DtreePreviewAction extends AbstractAction implements WindowListener
	{
		public DtreePreviewAction()
		{
			String text = "OK";
			super.putValue( Action.NAME, text );
			super.putValue( Action.SHORT_DESCRIPTION, text );
			DtreePreviewDialog.this.addWindowListener( this );
		}
		
		public void actionPerformed( ActionEvent e )
		{
			ok();
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
	};
	
	protected void initButton()
	{
		myDtreePreviewAction = new DtreePreviewAction();
		myButton = new JButton( myDtreePreviewAction );
	}
	
	protected DtreePreviewAction myDtreePreviewAction;
	protected JButton myButton;
	
	protected void initDtreeStats( Settings settings )
	{
		myPanelDtreeStats = makeDtreeStatsPanel();
		if( settings != null ) updateDtreeStatsDisplay( settings );
	}
	
	protected JComponent myPanelDtreeStats;
	
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
		myLabelDtreeHeight = new JLabel( "?" );
		Dimension dim = myLabelDtreeHeight.getPreferredSize();
		dim.width = 16;
		myLabelDtreeHeight.setPreferredSize( dim );
		myLabelDtreeMaxCluster = new JLabel( "?" );
		myLabelDtreeMaxCluster.setPreferredSize( dim );
		myLabelDtreeMaxCutset = new JLabel( "?" );
		myLabelDtreeMaxCutset.setPreferredSize( dim );
		myLabelDtreeMaxContext = new JLabel( "?" );
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
		
		DtreePreviewDialog DPD = new DtreePreviewDialog( null );
		DPD.setVisible( true );
	}
	
	protected Object mySynchronization = new Object();
	
	public static boolean FLAG_DEBUG_BORDERS = false;
	public static boolean FLAG_TEST = false;
}
