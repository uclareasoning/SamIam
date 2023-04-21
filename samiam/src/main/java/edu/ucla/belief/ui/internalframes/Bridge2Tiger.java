package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.util.JOptionResizeHelper.JOptionResizeHelperListener;
import edu.ucla.belief.ui.displayable.DisplayableBeliefNetworkImpl;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.networkdisplay. NodeLabel;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;

import java.util.Collection;
import java.util.Map;

/** java 4/java 5 factories

	@author keith cascio
	@since  20070321 */
public interface Bridge2Tiger
{
	/** @since 20071211 */
	public boolean      isTiger();
	/** describe the purpose of this factory */
	public String      describe();
	/** manufacture a new OutputPanel */
	public OutputPanel           newOutputPanel(           Map data, Collection variables, boolean useIDRenderer );
	/** manufacture a new EnumTableModel */
	public EnumTableModel        newEnumTableModel(        BeliefNetwork bn );
	/** manufacture a new EnumPropertyEditPanel */
	public EnumPropertyEditPanel newEnumPropertyEditPanel( BeliefNetwork bn );
	/** manufacture a new DisplayableBeliefNetworkImpl
		@since 20070326 */
	public DisplayableBeliefNetworkImpl newDisplayableBeliefNetworkImpl( BeliefNetwork toDecorate, NetworkInternalFrame hnif );

	/** @since 20071211 */
	public static class ProbabilityRewriteArgs{
		public ProbabilityRewriteArgs( FiniteVariable source, FiniteVariable destination, NetworkInternalFrame hnInternalFrame ){
			this.source          = source;
			this.destination     = destination;
			this.hnInternalFrame = hnInternalFrame;
		}

		public   final    FiniteVariable                 source, destination;
		public   final    NetworkInternalFrame           hnInternalFrame;
		public            String                         title;
		public            JOptionResizeHelperListener    listener;
	}

	/** @since 20071211 */
	public Thread     probabilityRewrite( ProbabilityRewriteArgs args );

	/** @since 20080221 */
	public Thread            replaceEdge( ProbabilityRewriteArgs args );

	/** @since 20080221 */
	public Thread            recoverEdge( ProbabilityRewriteArgs args );

	/** @since 20081022 */
	public Thread   randomSpanningForest( NetworkInternalFrame    nif );

	/** @since 20081023 */
	public Thread       findBurntBridges( NetworkInternalFrame    nif );

	/** @since 20081023 */
	public Bridge2Tiger  addEdgeRecovery( javax.swing.JMenu menu, UI ui );

	/** @since 20081023 */
	public Bridge2Tiger edgeRecoveryControlPanel( NetworkInternalFrame nif );

	/** @since 20080225 */
	public Collection     dynamators( Collection dynamators, UI ui );

	/** @since 20080123 */
	public void   screenshotScripts( UI ui );

	/** @since 20080228 */
	public NodeLabel setCPTMonitorShown( NodeLabel label, boolean show );

	/** Ask the 'bridge troll': "Can I pass?";
		He will say: "Who is your classloader?" "What is your major-minor version?" */
	public static class Troll{
		/** get a singleton factory instance */
		public static Bridge2Tiger solicit(){
			if( INSTANCE != null ) return INSTANCE;

			try{
				Class clazz = Class.forName( "edu.ucla.belief.ui.internalframes.CrouchingTiger" );
				INSTANCE    = (Bridge2Tiger) clazz.newInstance();
			}catch( UnsupportedClassVersionError exception ){
				if( Util.DEBUG_VERBOSE ) System.err.println( "Bridge2Tiger.Troll solicited by unworthy runtime environment: " + exception );
			}catch( Exception exception ){
				if( Util.DEBUG_VERBOSE ) System.err.println( "warning: Bridge2Tiger.Troll.solicit() caught " + exception );
			}
			if( INSTANCE == null ) INSTANCE = new LingeringMantis();

			return INSTANCE;
		}

		private static Bridge2Tiger INSTANCE;
	}
}
