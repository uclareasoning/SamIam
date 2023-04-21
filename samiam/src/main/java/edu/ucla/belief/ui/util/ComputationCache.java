package edu.ucla.belief.ui.util;

import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.displayable.*;

import edu.ucla.belief.*;

import java.text.*;
import java.util.Map;
import java.util.HashMap;

/**
	@author Keith Cascio
	@since 012004
*/
public class ComputationCache implements EvidenceChangeListener, CPTChangeListener, NetStructureChangeListener, NodePropertyChangeListener, RecompilationListener
{
	public static int MAXIMUM_FRACTION_DIGITS = 6;
	public static String STR_CONDITIONAL_FORMAT = "0.0#####";
	public static NumberFormat FORMAT_CONDITIONAL = new DecimalFormat( STR_CONDITIONAL_FORMAT );
	public static final String STR_GETCONDITIONAL_GENERIC = " calculating conditional probability for variable ";

	public ComputationCache( NetworkInternalFrame nif )
	{
		this.myNetworkInternalFrame = nif;
		initMap();
	}

	private void initMap()
	{
		int initSizeMap = (myNetworkInternalFrame == null) ? (int)0 : (myNetworkInternalFrame.getBeliefNetwork().size() * (int)2);
		this.myMapVIsToStrings = new HashMap( initSizeMap );

		if( myNetworkInternalFrame != null ){
			myNetworkInternalFrame.addEvidenceChangeListener( this );
			myNetworkInternalFrame.addCPTChangeListener( this );
			myNetworkInternalFrame.addNetStructureChangeListener( this );
			myNetworkInternalFrame.addNodePropertyChangeListener( this );
			myNetworkInternalFrame.addRecompilationListener( this );
			this.myVerboseFileName = myNetworkInternalFrame.getFileNameSansPath();
		}
	}

	/** @since 070705 */
	public boolean isNaN(){
		if( myFlagIsNaN != null ) return myFlagIsNaN.booleanValue();
		else{
			boolean ret = computeIsNaN();
			this.myFlagIsNaN = Boolean.valueOf( ret );
			return ret;
		}
	}

	/** @since 070705 */
	private boolean computeIsNaN(){
		boolean ret = false;
		try{
			if( this.myNetworkInternalFrame == null ) return ret = false;
			DisplayableBeliefNetwork bn = myNetworkInternalFrame.getBeliefNetwork();
			if( (bn == null) || bn.isEmpty() ) return ret = false;
			DisplayableFiniteVariable dvar = bn.dfvIterator().nextDFV();
			if( dvar == null ) return ret = false;
			Object instance = dvar.instance(0);
			if( instance == null ) return ret = false;
			double pr = getConditional( dvar, instance, (String)null );
			return ret = Double.isNaN( pr );
		}catch( Exception e ){
			System.err.println( "Warning: ComputationCache.computeIsNaN() caught " + e );
			ret = false;
		}//finally{}

		return ret;
	}

	/** @since 031302 */
	public double getConditional( FiniteVariable var, Object instance, String message_status )
	{
		double pr = (double)0;
		InferenceEngine engine = myNetworkInternalFrame.getInferenceEngine();
		if( engine != null )
		{
			if( message_status == null ) message_status = STR_GETCONDITIONAL_GENERIC + var.toString() + "...";
			Util.pushStatusWest( myNetworkInternalFrame, message_status );

			int evidenceIndex = var.index( instance );
			if( evidenceIndex != (int)-1 )
			{
				Table tbl = engine.conditional( var );
				if( tbl != null ) pr = tbl.value( new int[]{ evidenceIndex } );
			}

			Util.popStatusWest( myNetworkInternalFrame, message_status );
		}
		this.myFlagIsNaN = Boolean.valueOf( Double.isNaN( pr ) );
		return pr;
	}

	/** @since 031302 */
	public String getConditionalString( FiniteVariable var, Object instance, String message_status )
	{
		if( var == null || instance == null ) return "null variable/instance";
		else
		{
			String strInstance = instance.toString();
			if( myNetworkInternalFrame.getSamiamUserMode().contains( SamiamUserMode.EDIT ) ) return strInstance + "                        ";
			else
			{
				double value = getConditional( var, instance, message_status );
				String strNumber;
				if( Double.isNaN( value ) ) strNumber = "NaN";
				else strNumber = FORMAT_CONDITIONAL.format( value );
				return "Pr( "+strInstance+" )="+strNumber;
			}
		}
	}

	/** @since 012004 */
	private String getConditionalString( VariableInstance inst ){
		return getConditionalString( inst, STR_GETCONDITIONAL_GENERIC );
	}

	/** @since 012004 */
	public String getConditionalString( VariableInstance inst, String message_status_pre )
	{
		++myCountConditionals;
		String text = null;
		if( myMapVIsToStrings.containsKey( inst ) ) text = (String) myMapVIsToStrings.get( inst );
		else
		{
			++myCountConditionalsNotCached;
			String message_status = message_status_pre + inst.getVariable().toString() + "...";
			text = getConditionalString( inst.getVariable(), inst.getInstance(), message_status );
			myMapVIsToStrings.put( inst.clone(), text );
		}

		return text;
	}

	/** @since 012004 */
	protected void finalize() throws Throwable {
		if( Util.DEBUG_VERBOSE ){
			int countcached = myCountConditionals-myCountConditionalsNotCached;
			Util.STREAM_VERBOSE.println( "(ComputationCache)"+myVerboseFileName+".finalize()"
				+" { (cached)"+countcached
				+" + (non-cached)"+myCountConditionalsNotCached
				+" = (total)"+myCountConditionals+", "
				+ ((((double)countcached)/((double)myCountConditionals))*((double)100)) +"% }" );
		}
		super.finalize();
	}

	/**
		interface EvidenceChangeListener
		@since 012004
	*/
	public void evidenceChanged( EvidenceChangeEvent ece ){
		clearMap();
	}
	public void warning( EvidenceChangeEvent ece ) {}

	/**
		interface CPTChangeListener
		@since 012004
	*/
	public void cptChanged( CPTChangeEvent evt ){
		clearMap();
	}

	/**
		interface NetStructureChangeListener
		@since 012004
	*/
	public void netStructureChanged( NetStructureEvent ev){
		clearMap();
	}

	/**
		interface NodePropertyChangeListener
		@since 012004
	*/
	public void nodePropertyChanged( NodePropertyChangeEvent e ){
		clearMap();
	}

	/**
		interface RecompilationListener
		@since 012004
	*/
	public void networkRecompiled(){
		clearMap();
	}

	/** @since 012004 */
	private void clearMap(){
		myMapVIsToStrings.clear();
		myFlagIsNaN = null;
	}

	private String myVerboseFileName = "";
	private int myCountConditionals = (int)0;
	private int myCountConditionalsNotCached = (int)0;
	private Map myMapVIsToStrings;
	private Boolean myFlagIsNaN;
	private VariableInstance myUtilVariableInstance = new VariableInstance( null, null );
	private NetworkInternalFrame myNetworkInternalFrame;
}
