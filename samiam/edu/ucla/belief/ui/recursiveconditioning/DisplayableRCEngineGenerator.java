package edu.ucla.belief.ui.recursiveconditioning;

import edu.ucla.belief.*;
import edu.ucla.belief.inference. SynchronizedInferenceEngine;
import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.io.PropertySuperintendent;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.util.Util;

import javax.swing.JComponent;
import javax.swing.JMenu;
import java.awt.Container;
import java.util.Collection;

/** @author keith cascio
	@since  20030127 */
public class DisplayableRCEngineGenerator extends Dynamator
{
	public DisplayableRCEngineGenerator( RCEngineGenerator rc, UI ui )
	{
		myRCEngineGenerator = rc;
		myUI                = ui;
	}

	/** @since 20051017 */
	public boolean probabilitySupported(){
		return myRCEngineGenerator.probabilitySupported();
	}

	protected void compileHook( BeliefNetwork bn, DynaListener cl )
	{
		Settings settings = myRCEngineGenerator.getSettings( myRCEngineGenerator.choosePropertySuperintendent( (PropertySuperintendent) bn ) );

		if( cl instanceof NetworkInternalFrame )
		{
			//System.out.println( "cl instanceof NetworkInternalFrame" );
			settings.setOutStream( ((NetworkInternalFrame)cl).console );
		}

		if( settings.ensureRCExists( bn ) )
		{
			//RC rc = settings.getRC();
			//if( rc == null ) settings.updateOptimalMemoryNumber( bn );
			//else settings.updateOptimalMemoryNumber( rc );

			CachePreviewDialog CPD = new CachePreviewDialog( settings, null, true );
			//Util.centerWindow( CPD, Util.convertBoundsToScreen( myUI ) );
			Util.centerWindow( CPD, myUI.getBounds() );

			if( validate( bn, cl ) ){ super.compileHook( bn, cl ); }

			CPD.setVisible( true );
		}
		else cl.handleError( DtreeSettingsPanel.STR_GENERIC_ERROR );
		//else Util.showErrorMessage( DtreeSettingsPanel.STR_GENERIC_ERROR, DtreeSettingsPanel.STR_GENERIC_TITLE );
	}

	public String getDisplayName()
	{
		return myRCEngineGenerator.getDisplayName();
	}

	public Object getKey()
	{
		return myRCEngineGenerator.getKey();
	}

	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ) throws Throwable{
		return SynchronizedInferenceEngine.decorate( myRCEngineGenerator.manufactureInferenceEngineOrDie( bn, dyn ) );
	}

	public boolean isEditable() { return true; }

	public Commitable getEditComponent( Container cont )
	{
		validateSettingsPanel();

		mySettingsPanel.setNetworkInternalFrame( myUI.getActiveHuginNetInternalFrame() );

		//mySettingsPanel.setVisibleSafe( true, false );

		return mySettingsPanel;
	}

	public void commitEditComponent()
	{
		if( mySettingsPanel != null ) mySettingsPanel.commitChanges();
	}

	protected void validateSettingsPanel()
	{
		if( mySettingsPanel == null )
		{
			mySettingsPanel = new SettingsPanel( myRCEngineGenerator );//, cont );
			mySettingsPanel.setMinimumSize( mySettingsPanel.getPreferredSize() );
		}
		//else mySettingsPanel.setContainer( cont );
	}

	/** @since 20030304 */
	public JMenu getJMenu()
	{
		validateSettingsPanel();
		return mySettingsPanel.getJMenu();

	}

	/** @since 20030129 */
	public boolean equals( Object obj )
	{
		return obj == myRCEngineGenerator || obj == this;
	}

	/** @since 20031210 */
	public void killState( PropertySuperintendent bn )
	{
		myRCEngineGenerator.killState( bn );
	}

	/** @since 20081029 */
	public Object retrieveState( PropertySuperintendent bn ){
		return myRCEngineGenerator.retrieveState( bn );
	}

	/** @since 20040520 */
	public Collection getClassDependencies(){
		return myRCEngineGenerator.getClassDependencies();
	}

	/** @since 20040520 */
	public Dynamator getCanonicalDynamator() { return myRCEngineGenerator.getCanonicalDynamator(); }

	protected SettingsPanel mySettingsPanel;
	protected RCPanel myRCPanel;
	protected RCEngineGenerator myRCEngineGenerator;
	protected UI myUI;
}
