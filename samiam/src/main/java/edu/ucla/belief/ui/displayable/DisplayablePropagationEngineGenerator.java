package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.*;
import edu.ucla.belief.inference. SynchronizedInferenceEngine;
import edu.ucla.belief.approx.*;
import edu.ucla.belief.io.PropertySuperintendent;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;

import javax.swing.JComponent;
import javax.swing.JMenu;
import java.awt.Container;
import java.util.Collection;

/**
	Based on DisplayableJTEngineGenerator

	@author Keith Cascio
	@since 062105
*/
public class DisplayablePropagationEngineGenerator extends Dynamator
{
	public DisplayablePropagationEngineGenerator( PropagationEngineGenerator peg, UI ui )
	{
		this.myPropagationEngineGenerator = peg;
		this.myUI = ui;
	}

	/** @since 20051017 */
	public boolean probabilitySupported(){
		return myPropagationEngineGenerator.probabilitySupported();
	}

	public String getDisplayName(){
		return myPropagationEngineGenerator.getDisplayName();
	}

	public Object getKey(){
		return myPropagationEngineGenerator.getKey();
	}

	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ) throws Throwable{
		InferenceEngine ie = SynchronizedInferenceEngine.decorate( myPropagationEngineGenerator.manufactureInferenceEngineOrDie( bn, dyn ) );
		return ie;
	}

	public boolean isEditable() { return true; }

	public Commitable getEditComponent( Container cont )
	{
		validateSettingsPanel();

		//mySettingsPanel.setParentWindow( cont );
		mySettingsPanel.setNetworkInternalFrame( myUI.getActiveHuginNetInternalFrame(), myPropagationEngineGenerator );
		return mySettingsPanel;
	}

	public void commitEditComponent(){
		if( mySettingsPanel != null ) mySettingsPanel.commitChanges();
	}

	protected void validateSettingsPanel(){
		if( mySettingsPanel == null ){
			mySettingsPanel = new PropagationSettingsPanel();
			//mySettingsPanel.setMinimumSize( mySettingsPanel.getPreferredSize() );
		}
	}

	public JMenu getJMenu(){
		//validateSettingsPanel();
		//return mySettingsPanel.getJMenu();
		return null;
	}

	public boolean equals( Object obj ){
		return obj == myPropagationEngineGenerator || obj == this;
	}

	public void killState( PropertySuperintendent bn ){
		myPropagationEngineGenerator.killState( bn );
	}

	/** @since 20081029 */
	public Object retrieveState( PropertySuperintendent bn ){
		return myPropagationEngineGenerator.retrieveState( bn );
	}

	public Collection getClassDependencies(){
		return myPropagationEngineGenerator.getClassDependencies();
	}

	public Dynamator getCanonicalDynamator() { return myPropagationEngineGenerator.getCanonicalDynamator(); }

	protected PropagationSettingsPanel mySettingsPanel;
	protected PropagationEngineGenerator myPropagationEngineGenerator;
	protected UI myUI;
}
