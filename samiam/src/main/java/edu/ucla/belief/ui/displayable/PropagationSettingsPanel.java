package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.approx.*;
import edu.ucla.belief.*;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;

import edu.ucla.belief.ui.util.ResizeAdapter;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.util.WholeNumberField;
import edu.ucla.belief.ui.util.DecimalField;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
//import javax.swing.border.*;
//import java.lang.ref.SoftReference;

/**
	Based on BeliefPropagationSettingsPanael

	@author Keith Cascio
	@since 062105
*/
public class PropagationSettingsPanel extends JPanel implements ActionListener, Dynamator.Commitable//, ChangeListener, MenuListener
{
	public PropagationSettingsPanel()
	{
		init();
	}

	/** interface Dynamator.Commitable */
	public void commitChanges()
	{
		if( (myActualSettings != null) && (myVirtualSettings != null) )
		{
			//System.out.println( "PropagationSettingsPanel.commitChanges()" );
			myActualSettings.copy( myVirtualSettings );
			//System.out.println( "myActualSettings.getMaxIterations()  == " + myActualSettings.getMaxIterations() );
			//System.out.println( "myVirtualSettings.getMaxIterations() == " + myVirtualSettings.getMaxIterations() );
		}
	}

	/** interface Dynamator.Commitable */
	public JComponent asJComponent(){
		return (JComponent)this;
	}

	/** interface Dynamator.Commitable */
	public void copyToSystemClipboard(){
		StringBuffer buffer = new StringBuffer( 256 );
		buffer.append( "Compile settings - "+myPropagationEngineGenerator.getDisplayName()+"\n" );

		buffer.append( "Convergence threshold: " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myTfConvergence );

 		buffer.append( "\nMax iterations bound: " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myTfMaxIterations );

		buffer.append( "\nTimeout bound: " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myTfTimeout );

		buffer.append( "\nMessage passing schedule: " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myComboScheduler );

		Util.copyToSystemClipboard( buffer.toString() );
	}

	public void setNetworkInternalFrame( NetworkInternalFrame hnInternalFrame, PropagationEngineGenerator peg )
	{
		if( hnInternalFrame != myNetworkInternalFrame )
		{
			myNetworkInternalFrame = hnInternalFrame;
			set( myNetworkInternalFrame.getBeliefNetwork() );
			myPropagationEngineGenerator = peg;
		}

		BeliefPropagationSettings actual = peg.getSettings( peg.choosePropertySuperintendent( myBeliefNetwork ) );
		set( actual );
	}

	public void set( DisplayableBeliefNetwork bn )
	{
		myBeliefNetwork = bn;
	}

	protected void set( BeliefPropagationSettings actual )
	{
		//System.out.println( "PropagationSettingsPanel.set( "+actual+" )" );
		myActualSettings = actual;

		myVirtualSettings = new BeliefPropagationSettings();
		//myVirtualSettings.setDebugID( "VirtualSettings" + String.valueOf( COUNTER++ ) );
		myVirtualSettings.copy( myActualSettings );
		//myVirtualSettings.addChangeListener( this );

		//System.out.println( "myActualSettings.getMaxIterations()  == " + myActualSettings.getMaxIterations() );
		//System.out.println( "myVirtualSettings.getMaxIterations() == " + myVirtualSettings.getMaxIterations() );
		myTfMaxIterations.setValue( myVirtualSettings.getMaxIterations() );
		myTfTimeout.setValue( (int) myVirtualSettings.getTimeoutMillis() );
		myTfConvergence.setValue( myVirtualSettings.getConvergenceThreshold() );
		myComboScheduler.setSelectedItem( myVirtualSettings.getScheduler() );
	}

	public void setParentWindow( Window parent )
	{
		myParent = parent;
	}

	protected void init()
	{
		myPanelMain = new JPanel( myGridBagMain = new GridBagLayout() );

		myTfMaxIterations = new WholeNumberField(       BeliefPropagationSettings.INT_MAX_ITERATIONS_DEFAULT,  16, 0, Integer.MAX_VALUE );
		myTfTimeout       = new WholeNumberField( (int) BeliefPropagationSettings.LONG_TIMEOUT_MILLIS_DEFAULT, 16, 0, Integer.MAX_VALUE );
		myTfConvergence   = new DecimalField(           BeliefPropagationSettings.DOUBLE_THRESHOLD_DEFAULT,    16, (double)0, (double)1 );
		myComboScheduler  = new JComboBox( MessagePassingScheduler.ARRAY );

		myTfMaxIterations.addActionListener( this );
		myTfTimeout.addActionListener( this );
		myTfConvergence.addActionListener( this );
		myComboScheduler.addActionListener( this );

		//myButtonPreview = new JButton( "Preview" );
		//myButtonPreview.addActionListener( this );

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridwidth = 1;
		myPanelMain.add( new JLabel( "<html><nobr><b>Bound, maximum iterations:" ), c );
		myPanelMain.add( Box.createHorizontalStrut( 8 ), c );
		myPanelMain.add( myTfMaxIterations, c );
		myPanelMain.add( Box.createHorizontalStrut( 8 ), c );
		myPanelMain.add( new JLabel( "(0 = unbounded)" ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelMain.add( Box.createHorizontalGlue(), c );

		myPanelMain.add( Box.createVerticalStrut( 8 ), c );

		c.gridwidth = 1;
		myPanelMain.add( new JLabel( "<html><nobr><b>Time out (milliseconds):" ), c );
		myPanelMain.add( Box.createHorizontalGlue(), c );
		myPanelMain.add( myTfTimeout, c );
		myPanelMain.add( Box.createHorizontalGlue(), c );
		myPanelMain.add( new JLabel( "(0 = no time out)" ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelMain.add( Box.createHorizontalGlue(), c );

		myPanelMain.add( Box.createVerticalStrut( 8 ), c );

		c.gridwidth = 1;
		myPanelMain.add( new JLabel( "<html><nobr><b>Convergence threshold:" ), c );
		myPanelMain.add( Box.createHorizontalGlue(), c );
		myPanelMain.add( myTfConvergence, c );
		myPanelMain.add( Box.createHorizontalGlue(), c );
		myPanelMain.add( new JLabel( "(0 <= threshold)" ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelMain.add( Box.createHorizontalGlue(), c );

		myPanelMain.add( Box.createVerticalStrut( 8 ), c );

		c.gridwidth = 1;
		myPanelMain.add( new JLabel( "<html><nobr><b>Message passing schedule:" ), c );
		myPanelMain.add( Box.createHorizontalGlue(), c );
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelMain.add( myComboScheduler, c );
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		//myPanelMain.add( Box.createHorizontalGlue(), c );
		//myPanelMain.add( Box.createHorizontalGlue(), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelMain.add( Box.createHorizontalGlue(), c );

		myResizePanel = this;
		myResizePanel.add( myPanelMain );
		myResizeAdapter = new ResizeAdapter( myPanelMain );
		myResizePanel.addComponentListener( myResizeAdapter );
		Dimension minSize = myPanelMain.getPreferredSize();
		minSize.width += 70;
		minSize.height += 20;
		setMinimumSize( minSize );
		setPreferredSize( minSize );

		//if( FLAG_DEBUG_BORDERS ) myResizePanel.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );
		//if( FLAG_DEBUG_BORDERS ) myPanelMain.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
	}

	public void actionPerformed( ActionEvent e )
	{
		Object src = e.getSource();
		if(      src == myTfMaxIterations ) myVirtualSettings.setMaxIterations(      myTfMaxIterations.getValue() );
		else if( src == myTfTimeout )       myVirtualSettings.setTimeoutMillis(     (long) myTfTimeout.getValue() );
		else if( src == myTfConvergence )   myVirtualSettings.setConvergenceThreshold( myTfConvergence.getValue() );
		else if( src == myComboScheduler )  myVirtualSettings.setScheduler( (MessagePassingScheduler) myComboScheduler.getSelectedItem() );
		//if( src == myButtonPreview ) doPreview();
	}

	protected JButton myButtonPreview;

	private WholeNumberField myTfMaxIterations, myTfTimeout;
	private DecimalField myTfConvergence;
	private JComboBox myComboScheduler;

	protected JPanel myPanelMain, myPanelInner;
	private GridBagLayout myGridBagInner, myGridBagMain;
	protected JComponent myResizePanel;
	protected ComponentListener myResizeAdapter;
	private Window myParent;

	protected BeliefPropagationSettings myActualSettings, myVirtualSettings;
	protected ActionListener myActionListener;
	protected NetworkInternalFrame myNetworkInternalFrame;
	protected PropagationEngineGenerator myPropagationEngineGenerator;
	protected DisplayableBeliefNetwork myBeliefNetwork;
	//private SoftReference myTempEngine;

	//protected static int COUNTER = (int)0;
	//public static boolean FLAG_DEBUG_BORDERS = false;
}
