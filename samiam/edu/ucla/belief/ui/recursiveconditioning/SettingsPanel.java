package edu.ucla.belief.ui.recursiveconditioning;

import edu.ucla.belief.dtree.*;
import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.PropertySuperintendent;

import edu.ucla.belief.ui.util.ResizeAdapter;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.NetworkInternalFrame;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
//import java.beans.PropertyChangeListener;
//import java.beans.PropertyChangeEvent;

/** @author keith cascio
	@since  20030207 */
public class SettingsPanel extends JPanel implements
//ActionListener,
//PropertyChangeListener,
  Dynamator.Commitable,
  javax.swing.event.ChangeListener,
  edu.ucla.util.ChangeListener,
  MenuListener
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
		buffer.append( "Compile settings - Recursive Conditioning (il1)\n" );
		buffer.append( myAllocationPanel.getClipboardInfo() );
		Util.copyToSystemClipboard( buffer.toString() );
	}

	/** @since 20030124 */
	public void setNetworkInternalFrame( NetworkInternalFrame hnInternalFrame )
	{
		myTabbedPane.setSelectedComponent( myAllocationPanel );

		if( hnInternalFrame != myNetworkInternalFrame )
		{
			setListening( myNetworkInternalFrame, myBeliefNetwork, false );

			myNetworkInternalFrame = hnInternalFrame;
			set( myNetworkInternalFrame.getBeliefNetwork() );

			setListening( myNetworkInternalFrame, myBeliefNetwork, true );
		}

		PropertySuperintendent bn = (PropertySuperintendent) myBeliefNetwork;
		Settings actual = RCEngineGenerator.getSettings( myRCEngineGenerator == null ? bn : myRCEngineGenerator.choosePropertySuperintendent( bn ) );
		actual.setOutStream( hnInternalFrame.console );
		set( actual );

		java.io.File defaultDirectory = myNetworkInternalFrame.getParentFrame().getSamiamPreferences().defaultDirectory;
		myAllocationPanel.setDefaultDirectory( defaultDirectory );
		myAllocationPanel.setNetworkName( myNetworkInternalFrame.getFileNameSansPath() );
		myDtreePanel.setDefaultDirectory( defaultDirectory );
	}

	protected static int COUNTER = (int)0;

	public void set( BeliefNetwork bn )
	{
		myBeliefNetwork = bn;
	}

	/** @since 20030225 */
	protected void set( Settings actual )
	{
		myActualSettings = actual;

		myVirtualSettings = new Settings();
		myVirtualSettings.setDebugID( "VirtualSettings" + String.valueOf( COUNTER++ ) );
		myVirtualSettings.copy( myActualSettings );
		myVirtualSettings.addChangeListener( this );

		myAllocationPanel.setBeliefNetwork( myBeliefNetwork );
		myAllocationPanel.set( myVirtualSettings );

		myDtreePanel.setBeliefNetwork( myBeliefNetwork );
		myDtreePanel.set( myVirtualSettings );
	}

	/**
			SHOULD NOT actually need to listen, since I
			designed SettingsPanel to live inside a MODAL dialog,
			i.e. the user should not be able to trigger events
			that could invalidate SettingsPanel.

		@since 20030124 */
	public void setListening( NetworkInternalFrame nif, BeliefNetwork bn, boolean listen )
	{
		if( nif != null )
		{
			if( listen )
			{
				//nif.addCPTChangeListener(this);
				//nif.addNetStructureChangeListener(this);
				//nif.addNodePropertyChangeListener(this);
			}
			else
			{
				//nif.removeCPTChangeListener(this);
				//nif.removeNetStructureChangeListener(this);
				//nif.removeNodePropertyChangeListener(this);
			}
		}

		if( bn != null )
		{
			//if( listen ) bn.getEvidenceController().addEvidenceChangeListener( this );
			//else bn.getEvidenceController().removeEvidenceChangeListener( this );
		}
	}

	protected JTabbedPane myTabbedPane;
	protected CacheSettingsPanel myAllocationPanel;
	protected DtreeSettingsPanel myDtreePanel;
	protected JComponent myResizePanel;
	protected ComponentListener myResizeAdapter;

	protected void init( Container parent )
	{
		myTabbedPane = new JTabbedPane();
		myTabbedPane.addChangeListener( this );

		myAllocationPanel = new CacheSettingsPanel();
		myDtreePanel = new DtreeSettingsPanel();

		myTabbedPane.add( myAllocationPanel, "Cache" );
		myTabbedPane.add( myDtreePanel, "Dtree" );

		myResizePanel = this;
		myResizePanel.add( myTabbedPane );
		myResizeAdapter = new ResizeAdapter( myTabbedPane );
		myResizePanel.addComponentListener( myResizeAdapter );
		Dimension minSize = myTabbedPane.getPreferredSize();
		minSize.width += 15;
		minSize.height += 20;
		setMinimumSize( minSize );
		setPreferredSize( minSize );

		//addPropertyChangeListener( this );

		//myContainerAbstraction = new ContainerAbstraction( parent );

		//setVisible( false );
	}

	protected JMenu myJMenu;

	/** @since 20030304 */
	protected void initJMenu()
	{
		myJMenu = new JMenu( "Recursive Conditioning" );
		myJMenu.add( myAllocationPanel.action_OPENRC );
		myJMenu.add( myAllocationPanel.action_SAVERC );
		myJMenu.setEnabled( false );
		myJMenu.addMenuListener( this );
	}

	/**
		For interface MenuListener
		@author Keith Cascio
		@since 20030304 */
	public void menuSelected(MenuEvent e)
	{
		myAllocationPanel.validateActionsEnabled();
	}
	public void menuDeselected(MenuEvent e) {}
	public void menuCanceled(MenuEvent e) {}

	/** @since 20030304 */
	public JMenu getJMenu()
	{
		return myJMenu;
	}

	/** @since 20030304 */
	//public void propertyChange( PropertyChangeEvent evt )
	//{
		//System.out.println( "SettingsPanel.propertyChange(" +evt.getPropertyName() + ")" );
		//if( evt.getPropertyName().equals( "ancestor" ) )
		//{
		//	//myJMenu.setEnabled( getParent() != null );
		//	myJMenu.setEnabled( isShowing() );
		//}
	//}

	/** @since 20030304 */
	public void addNotify()
	{
		super.addNotify();
		myJMenu.setEnabled( true );
	}

	/** @since 20030304 */
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
			boolean flagDtreeChanged = myVirtualSettings.isDtreeSettingChanged();

			if( flagDtreeChanged )
			{
				//System.out.println( "SettingsPanel dtree setting changed." );
				myFlagDtreeSettingsChanged = true;
			}

			if( flagDtreeChanged || myVirtualSettings.getRC() == null )
			{
				myAllocationPanel.setFileName( (String)null );
			}
		}
	}*/

	/** interface ChangeListener
		@since    20081128 */
	public edu.ucla.util.ChangeListener settingChanged( edu.ucla.util.ChangeEvent evt ){
		if( evt.getSource() != myVirtualSettings ){ return this; }

		boolean flagDtreeChanged = myVirtualSettings.isDtreeSettingChanged();

		if(     flagDtreeChanged ){ myFlagDtreeSettingsChanged = true; }

		if(     flagDtreeChanged || (myVirtualSettings.getRC() == null) ){
			myAllocationPanel.setFileName( (String)null );
		}

		return this;
	}

	protected boolean myFlagDtreeSettingsChanged = false;

	/** @since 20030225 */
	public void stateChanged( ChangeEvent e )
	{
		Object src = e.getSource();
		if( src == myTabbedPane )
		{
			if( myVirtualSettings != null )
			{
				if( myTabbedPane.getSelectedComponent() == myAllocationPanel )
				{
					if( myVirtualSettings.isDtreeSettingChanged() )
					{
						//System.out.println( "SettingsPanel validating dtree." );
						if( !myVirtualSettings.validateDtree( myBeliefNetwork ) ) Util.showErrorMessage( DtreeSettingsPanel.STR_GENERIC_ERROR, DtreeSettingsPanel.STR_GENERIC_TITLE );
					}

					if( myFlagDtreeSettingsChanged )
					{
						//System.out.println( "SettingsPanel updating memory display." );
						myAllocationPanel.updateMemoryDisplay();
						myFlagDtreeSettingsChanged = false;
					}
				}
			}
		}
	}

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

	protected RCEngineGenerator    myRCEngineGenerator;
	protected Settings             myActualSettings, myVirtualSettings;
	protected ActionListener       myActionListener;
	protected NetworkInternalFrame myNetworkInternalFrame;
	protected BeliefNetwork        myBeliefNetwork;

	public static boolean
	  FLAG_DEBUG_BORDERS = false,
	  FLAG_TEST          = false;
}
