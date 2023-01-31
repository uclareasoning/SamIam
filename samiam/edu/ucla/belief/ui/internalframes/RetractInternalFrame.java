package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.actionsandmodes.*;

import edu.ucla.belief.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class RetractInternalFrame extends JInternalFrame implements
	ActionListener, RecompilationListener, EvidenceChangeListener,
	SamiamUserModal,
	NetStructureChangeListener, CPTChangeListener
{
	private NetworkInternalFrame hnInternalFrame;
	protected PreferenceGroup myGlobalPrefs = null;

	private JPanel mainPanel;
	private Container myContentPain;
	private PickInstancesPanel pickInstancesPanel;
	private RetractTable retractTable;

	public RetractInternalFrame(	NetworkInternalFrame hnInternalFrame,
					PreferenceGroup globalPrefs )
	{
		super("Evidence retraction", true, true, true, true);
		this.hnInternalFrame = hnInternalFrame;
		this.myGlobalPrefs = globalPrefs;

		hnInternalFrame.addRecompilationListener(this);
		hnInternalFrame.addEvidenceChangeListener(this);
		hnInternalFrame.addNetStructureChangeListener(this);
		hnInternalFrame.addCPTChangeListener( this );

		myContentPain = getContentPane();
		myContentPain.setLayout( new BorderLayout() );

		//pickInstancesPanel = new PickInstancesPanel( hnInternalFrame );
		//pickInstancesPanel.addActionListener( this );
		//myContentPain.add( pickInstancesPanel, BorderLayout.NORTH );

		mainPanel = new JPanel( new BorderLayout() );
		Border b = BorderFactory.createEtchedBorder();
		b = BorderFactory.createTitledBorder( b, "Conditional probabilities after evidence retraction" );
		mainPanel.setBorder( b );
		myContentPain.add( mainPanel, BorderLayout.CENTER );

		//revalidateMainPanel();
	}

	/** interface SamiamUserModal
		@since 20051017 */
	public void setSamiamUserMode( SamiamUserMode mode ){
		if( mode.contains( SamiamUserMode.EVIDENCEFROZEN ) ) RetractInternalFrame.this.setVisible( false );
	}

	/** @since 2002110502 */
	public void setVisible( boolean flag )
	{
		if( flag && !isVisible() )
		{
			//System.out.println( "Refreshing RetractInternalFrame" );
			revalidateEverything();
		}

		super.setVisible( flag );
	}

	public void revalidateMainPanel()
	{
		mainPanel.removeAll();

		if( pickInstancesPanel != null && hnInternalFrame.getInferenceEngine() != null && hnInternalFrame.getPartialDerivativeEngine() != null )
		{
			retractTable = new RetractTable( hnInternalFrame, pickInstancesPanel.getPickedInstances() );
			mainPanel.add( new JScrollPane( retractTable ) );
		}

		mainPanel.revalidate();
		mainPanel.repaint();
	}

	public void revalidateEverything()
	{
		if( pickInstancesPanel != null ) myContentPain.remove( pickInstancesPanel );

		pickInstancesPanel = new PickInstancesPanel( hnInternalFrame );
		pickInstancesPanel.addActionListener( this );
		myContentPain.add( pickInstancesPanel, BorderLayout.NORTH );

		revalidateMainPanel();
	}

	/**
		@author Keith Cascio
		@since 081302
	*/
	public void cptChanged( CPTChangeEvent evt ){
		if( isVisible() ) revalidateMainPanel();
	}

	public void networkRecompiled(){
		if( isVisible() ) revalidateMainPanel();
	}

	/**
		interface EvidenceChangeListener
		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ece ) {}

	public void evidenceChanged( EvidenceChangeEvent ECE ){
		if( isVisible() ) revalidateMainPanel();
	}

	public void netStructureChanged( NetStructureEvent event ){
		//System.out.println( "RetractInternalFrame.netStructureChanged()" );
		//if( event.eventType == NetStructureEvent.NODES_REMOVED ) pickInstancesPanel.removePicks( event.finiteVars.toArray() );
		if( isVisible() ) revalidateEverything();
	}

	public void changePackageOptions( PreferenceGroup globalPrefs ){
		if( isVisible() ) revalidateEverything();
	}

	public void actionPerformed(ActionEvent event){
		revalidateMainPanel();
	}
}
