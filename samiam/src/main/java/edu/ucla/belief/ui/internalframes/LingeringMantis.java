package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.displayable.DisplayableBeliefNetworkImpl;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.networkdisplay. NodeLabel;

import edu.ucla.belief.BeliefNetwork;

import java.util.Collection;
import java.util.Map;

/** Factory for java 4 version objects.
	Assume only java 4 mantis runtime, java 5 tiger enhanced features disabled.
	@author keith cascio
	@since  20070321 */
class LingeringMantis implements Bridge2Tiger
{
	/** @since 20071211 */
	public boolean      isTiger(){ return false; }

	/** assume only java 4 mantis runtime, java 5 tiger enhanced features disabled */
	public String describe(){
		return "assume only java 4 mantis runtime, java 5 tiger enhanced features disabled";
	}

	public OutputPanel newOutputPanel( Map data, Collection variables, boolean useIDRenderer ){
		return new OutputPanel( data, variables, useIDRenderer );
	}

	public EnumTableModel        newEnumTableModel(        BeliefNetwork bn ){
		return new EnumTableModel( bn );
	}

	public EnumPropertyEditPanel newEnumPropertyEditPanel( BeliefNetwork bn ){
		return new EnumPropertyEditPanel( bn );
	}

	/** @since 20070326 */
	public DisplayableBeliefNetworkImpl newDisplayableBeliefNetworkImpl( BeliefNetwork toDecorate, NetworkInternalFrame hnif ){
		return new DisplayableBeliefNetworkImpl( toDecorate, hnif );
	}

	/** @since 20071211 */
	public Thread probabilityRewrite( ProbabilityRewriteArgs args ){
		return null;
	}

	/** @since 20080221 */
	public Thread        replaceEdge( ProbabilityRewriteArgs args ){
		return null;
	}

	/** @since 20080221 */
	public Thread        recoverEdge( ProbabilityRewriteArgs args ){
		return null;
	}

	/** @since 20081022 */
	public Thread   randomSpanningForest( NetworkInternalFrame    nif ){
		return null;
	}

	/** @since 20081023 */
	public Thread       findBurntBridges( NetworkInternalFrame    nif ){
		return null;
	}

	/** @since 20081023 */
	public Bridge2Tiger  addEdgeRecovery( javax.swing.JMenu menu, edu.ucla.belief.ui.UI ui ){ return this; }

	/** @since 20081024 */
	public Bridge2Tiger edgeRecoveryControlPanel( NetworkInternalFrame nif ){ return this; }

	/** @since 20080225 */
	public Collection     dynamators( Collection dynamators, edu.ucla.belief.ui.UI ui ){ return dynamators; }

	/** @since 20080123 */
	public void   screenshotScripts( edu.ucla.belief.ui.UI ui ){}

	/** @since 20080228 */
	public NodeLabel setCPTMonitorShown( NodeLabel label, boolean show ){ return label; }
}
