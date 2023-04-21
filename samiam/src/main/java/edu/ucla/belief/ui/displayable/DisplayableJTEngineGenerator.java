package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.*;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.io.PropertySuperintendent;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;

import javax.swing.JComponent;
import javax.swing.JMenu;
import java.awt.Container;
import java.util.Collection;

/** Based on DisplayableRCEngineGenerator

	@author keith cascio
	@since  20030923 */
public class DisplayableJTEngineGenerator extends Dynamator
{
	public DisplayableJTEngineGenerator( DefaultGenerator dg, UI ui )
	{
		myDefaultGenerator = dg;
		myUI = ui;
	}

	/** @since 20051017 */
	public boolean probabilitySupported(){
		return myDefaultGenerator.probabilitySupported();
	}

	public String getDisplayName()
	{
		return myDefaultGenerator.getDisplayName();
	}

	public Object getKey()
	{
		return myDefaultGenerator.getKey();
	}

	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ) throws Throwable{
		InferenceEngine ie = SynchronizedInferenceEngine.decorate( myDefaultGenerator.manufactureInferenceEngineOrDie( bn, dyn ) );
		return ie;
	}

	public boolean isEditable() { return true; }

	public Commitable getEditComponent( Container cont )
	{
		validateSettingsPanel();

		//mySettingsPanel.setParentWindow( cont );
		mySettingsPanel.setNetworkInternalFrame( myUI.getActiveHuginNetInternalFrame(), myDefaultGenerator );
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
			mySettingsPanel = new JoinTreeSettingsPanel();
			mySettingsPanel.setMinimumSize( mySettingsPanel.getPreferredSize() );
		}
	}

	public JMenu getJMenu()
	{
		//validateSettingsPanel();
		//return mySettingsPanel.getJMenu();
		return null;
	}

	public boolean equals( Object obj )
	{
		return obj == myDefaultGenerator || obj == this;
	}

	/** @since 101403 */
	//public void killInferenceEngine( PropertySuperintendent bn ){
	//	myDefaultGenerator.killInferenceEngine( bn );
	//}

	/** @since 121003 */
	public void killState( PropertySuperintendent bn )
	{
		myDefaultGenerator.killState( bn );
	}

	/** @since 20081029 */
	public Object retrieveState( PropertySuperintendent bn ){
		return myDefaultGenerator.retrieveState( bn );
	}

	/** @since 052004 */
	public Collection getClassDependencies(){
		return myDefaultGenerator.getClassDependencies();
	}

	/** @since 052004 */
	public Dynamator getCanonicalDynamator() { return myDefaultGenerator.getCanonicalDynamator(); }

	protected JoinTreeSettingsPanel mySettingsPanel;
	protected DefaultGenerator myDefaultGenerator;
	protected UI myUI;
}
