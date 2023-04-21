package edu.ucla.belief.ui.rc;

import edu.ucla.belief.dtree.*;
//import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.*;

import edu.ucla.belief.ui.util.ResizeAdapter;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.displayable.DisplayableBeliefNetwork;
import edu.ucla.belief.ui.NetworkInternalFrame;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
//import java.beans.PropertyChangeListener;
//import java.beans.PropertyChangeEvent;

/** @author keith cascio
	@since 20031107 */
public class SettingsPanel extends JPanel implements
 //ActionListener,
 //PropertyChangeListener,
   Dynamator.Commitable,
   MenuListener,
   edu.ucla.util.ChangeListener
{
	public SettingsPanel( RCEngineGenerator rceg )
	{
		myRCEngineGenerator = rceg;
		init( null );
		initJMenu();
	}

	/** interface Dynamator.Commitable
		@since 20050204 */
	public void commitChanges()
	{
		if( myActualSettings != null && myVirtualSettings != null )
		{
			//System.out.println( "SettingsPanel.commitChanges()" );
			myActualSettings.copy( myVirtualSettings );
		}
	}

	/** interface Dynamator.Commitable
		@since 20050204 */
	public JComponent asJComponent(){
		return (JComponent)this;
	}

	/** interface Dynamator.Commitable
		@since 20050204 */
	public void copyToSystemClipboard(){
		StringBuffer buffer = new StringBuffer( 256 );
		buffer.append( "Compile settings - Recursive Conditioning (il2)\n" );
		buffer.append( myAllocationPanel.getClipboardInfo() );
		Util.copyToSystemClipboard( buffer.toString() );
	}

	public void setNetworkInternalFrame( NetworkInternalFrame hnInternalFrame )
	{
		if( hnInternalFrame != myNetworkInternalFrame )
		{
			myNetworkInternalFrame = hnInternalFrame;
			set( myNetworkInternalFrame.getBeliefNetwork() );
		}

		RCSettings actual = RCEngineGenerator.getSettings( myRCEngineGenerator == null ? myBeliefNetwork : myRCEngineGenerator.choosePropertySuperintendent( myBeliefNetwork ) );
		//actual.setOutStream( hnInternalFrame.console );
		set( actual );

		java.io.File defaultDirectory = myNetworkInternalFrame.getParentFrame().getSamiamPreferences().defaultDirectory;
		myAllocationPanel.setDefaultDirectory( defaultDirectory );
		myAllocationPanel.setNetworkName( myNetworkInternalFrame.getFileNameSansPath() );
	}

	private static int COUNTER = (int)0;

	public void set( DisplayableBeliefNetwork bn )
	{
		myBeliefNetwork = bn;
	}

	private void set( RCSettings actual )
	{
		myActualSettings = actual;

		myVirtualSettings = new RCSettings();
		myVirtualSettings.setDebugID( "VirtualSettings" + String.valueOf( COUNTER++ ) );
		myVirtualSettings.copy( myActualSettings );
		myVirtualSettings.addChangeListener( this );

		myAllocationPanel.setBeliefNetwork( myBeliefNetwork );
		myAllocationPanel.set( myVirtualSettings );
	}

	private CacheSettingsPanel myAllocationPanel;
	private JComponent myResizePanel;
	private ComponentListener myResizeAdapter;

	private void init( Container parent )
	{
		myAllocationPanel = new CacheSettingsPanel();

		myResizePanel = this;
		myResizePanel.add( myAllocationPanel );//myTabbedPane );
		myResizeAdapter = new ResizeAdapter( myAllocationPanel );//myTabbedPane );
		myResizePanel.addComponentListener( myResizeAdapter );
		Dimension minSize = myAllocationPanel.getPreferredSize();//myTabbedPane.getPreferredSize();
		minSize.width = 400;
		minSize.height += 20;
		setMinimumSize( minSize );
		setPreferredSize( minSize );
	}

	private JMenu myJMenu;

	private void initJMenu()
	{
		myJMenu = new JMenu( "Recursive Conditioning" );
		myJMenu.setEnabled( false );
		myJMenu.addMenuListener( this );
	}

	/** interface MenuListener
		@since 20030304 */
	public void menuSelected(MenuEvent e)
	{
		//myAllocationPanel.validateActionsEnabled();
	}
	public void menuDeselected(MenuEvent e) {}
	public void menuCanceled(MenuEvent e) {}

	public JMenu getJMenu()
	{
		return myJMenu;
	}

	public void addNotify()
	{
		super.addNotify();
		myJMenu.setEnabled( true );
	}
	public void removeNotify()
	{
		super.removeNotify();
		myJMenu.setEnabled( false );
	}

  /*public void actionPerformed( ActionEvent evt )
	{
		Object src = evt.getSource();
		if( src == myVirtualSettings )
		{
			if( myVirtualSettings.getInfo() == null )
			{
				//System.out.println( "SettingsPanel dtree setting changed." );
				myFlagSettingsChanged = true;
			}
		}
	}*/

	/** interface ChangeListener
		@since    20081128 */
	public edu.ucla.util.ChangeListener settingChanged( edu.ucla.util.ChangeEvent evt ){
		if( (evt.getSource() == myVirtualSettings) && (myVirtualSettings.getInfo() == null) ){ myFlagSettingsChanged = true; }
		return this;
	}

	private boolean myFlagSettingsChanged = false;

	/** test/debug */
	public static void main( String[] args )
	{
		FLAG_TEST = true;

		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		SettingsPanel SP = new SettingsPanel( null );

		JFrame frame = new JFrame( "DEBUG Recursive Conditioning Settings" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( SP );
		frame.pack();

		Util.centerWindow( frame );
		frame.setVisible( true );
	}

	private RCEngineGenerator        myRCEngineGenerator;
	private RCSettings               myActualSettings, myVirtualSettings;
	private ActionListener           myActionListener;
	private NetworkInternalFrame     myNetworkInternalFrame;
	private DisplayableBeliefNetwork myBeliefNetwork;

	public static boolean
	  FLAG_DEBUG_BORDERS = false,
	  FLAG_TEST          = false;
}
