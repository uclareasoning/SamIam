package edu.ucla.belief.ui.rc;

import edu.ucla.belief.*;
//import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.inference.*;
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
		myUI = ui;
	}

	/** @since 20051017 */
	public boolean probabilitySupported(){
		return myRCEngineGenerator.probabilitySupported();
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
		NetworkInternalFrame nif = myUI.getActiveHuginNetInternalFrame();

		if( (nif == null) || (nif.getBeliefNetwork() == null) ) return null;

		if( nif.getBeliefNetwork().size() < Dynamator.INT_MINIMUM_VARIABLES ){
			throw new IllegalStateException( "Please add at least "+(Dynamator.INT_MINIMUM_VARIABLES-nif.getBeliefNetwork().size())+" variable(s) to this network before editing recursive conditioning compile settings." );
		}

		validateSettingsPanel();
		mySettingsPanel.setNetworkInternalFrame( nif );
		//mySettingsPanel.setVisibleSafe( true, false );

		return mySettingsPanel;
	}

	public void commitEditComponent()
	{
		if( mySettingsPanel != null ) mySettingsPanel.commitChanges();
	}

	protected void validateSettingsPanel()
	{
		if( mySettingsPanel == null ){
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
	public void killRCInfo( PropertySuperintendent bn )
	{
		myRCEngineGenerator.killRCInfo( bn );
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
	protected RCEngineGenerator myRCEngineGenerator;
	protected UI myUI;
}
