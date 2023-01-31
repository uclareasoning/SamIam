package edu.ucla.belief.inference;

import edu.ucla.belief.*;
import il2.inf.structure.JoinTreeStats;
import il2.inf.jointree.JoinTreeAlgorithm;

import java.util.*;
//{superfluous} import java.math.BigInteger;

/**
	@author JD Park
*/
public abstract class WrapperInferenceEngine extends AbstractInferenceEngine implements JoinTreeInferenceEngine, JoinTreeStats.StatsSource
{
	protected JointWrapper comp;
	protected BeliefNetwork bn;
	protected boolean isExhaustive;

	/** @since 061404 */
	//public InferenceEngine handledClone( QuantitativeDependencyHandler handler ){
	//	return (InferenceEngine)null;
	//}

	/** @since 20060321 */
	public void die(){
		super.die();
		WrapperInferenceEngine.this.comp                         = null;
		if( myMapVariablesToConditionals != null ) myMapVariablesToConditionals.clear();
		WrapperInferenceEngine.this.myMapVariablesToConditionals = null;
		WrapperInferenceEngine.this.myRandom                     = null;
		WrapperInferenceEngine.this.bn                           = null;
	}

	/** @since 061404 */
	public BeliefNetwork getBeliefNetwork(){
		return this.bn;
	}

	/** @since 112503 */
	public JointWrapper getJointWrapper(){
		return comp;
	}

	public JoinTreeStats.StatsSource getJoinTreeStats(){
		return this;
	}

	/** @since 061504 */
	public void printTables( java.io.PrintWriter out ){
		if( comp.engine() instanceof il2.inf.jointree.JoinTreeAlgorithm ){
			il2.model.Table[] il2Tables = ((JoinTreeAlgorithm)comp.engine()).getOriginalTables();
			edu.ucla.belief.Table[] il1Tables = comp.converter().convert( il2Tables );
			printTables( il1Tables, out );
		}
		else throw new UnsupportedOperationException();
		//printTables( comp.getTables(), out );
	}

	public void printInfoCompilation(java.io.PrintWriter stream)
	{
		stream.println( STR_CONSOLE_MESSAGE_COMP_TIME + getCompilationTime() );
		stream.println( STR_CONSOLE_MESSAGE_COMP_MEM + getMemoryRequirements() );
	}

	public void printInfoPropagation(java.io.PrintWriter stream)
	{
		double proptime = getPropagationTime();
		if( !Double.isNaN( proptime ) ) stream.println( STR_CONSOLE_MESSAGE_PROP_TIME + proptime );
	}

	protected static Random myRandom = new Random();
	WrapperInferenceEngine(JointWrapper comp, BeliefNetwork bn, Dynamator dyn){
		this(comp,bn,dyn,true);
	}

	WrapperInferenceEngine( JointWrapper comp,BeliefNetwork bn, Dynamator dyn , boolean exhaustive)
	{
		super( dyn );
		isExhaustive=exhaustive;
		this.comp = comp;
		this.bn = bn;
		bn.getEvidenceController().addPriorityEvidenceChangeListener( this );
	}

	/**
	   For interface EvidenceChangeListener
	   @author Keith Cascio
	   @since 071902
	*/
	protected void updateEvidence( FiniteVariable var, Object value )
	{
		if( value == null ){
			comp.removeEvidence(var);
		}else{
			comp.addEvidence(var,value);
		}
	}

	/**
		For interface EvidenceChangeListener
		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ece ) {}

	/**
	   For interface EvidenceChangeListener
	   @author Keith Cascio
	   @since 071902
	*/
	public void evidenceChanged( EvidenceChangeEvent ECE )
	{
		clearCache();
		EvidenceController controller = bn.getEvidenceController();
		FiniteVariable var = null;
		for( Iterator it = ECE.recentEvidenceChangeVariables.iterator(); it.hasNext(); )
		{
			var = (FiniteVariable) it.next();
			updateEvidence( var, controller.getValue( var ) );
		}
	}

	/**
	 * Sets the CPT associated with var to the values in vals.
	 * The ordering of vars must be consistent with the ordering
	 * returned by getCPT(var).
	 * @param var The variable whose CPT we want to set.
	 * @param vals The values of the entries in the cpt.
	 */
	public void setCPT( FiniteVariable var )//, double[] vals)
	{
		clearCache();
		comp.setCPT( var, getEffectiveCPTData( var ) );//var.getCPTShell( var.getDSLNodeType() ).getCPT().dataclone() );
	}

	/**
	   @author Keith Cascio
	   @since 102902
	*/
	public double probability()
	{
		if( bn.getEvidenceController().isEmpty() ) return (double)1;
		else return comp.prEvidence();
	}

	/**
	 * Returns P(var,observations).
	 */
	public Table joint(FiniteVariable var)
	{
		return comp.varJoint(var);
	}

	/**
	 * Returns P(var | observations).
	 */
	public Table conditional(FiniteVariable var)
	{
		if( myMapVariablesToConditionals.containsKey( var ) )
		{
			return (Table) myMapVariablesToConditionals.get( var );
		}
		else
		{
			Table ret = Table.normalize(joint(var));
			myMapVariablesToConditionals.put( var, ret );
			return ret;
		}
	}

	/**
	   @author Keith Cascio
	   @since 082002
	*/
	protected void clearCache()
	{
		myMapVariablesToConditionals.clear();
	}

	protected Map myMapVariablesToConditionals = new HashMap();

	/**
	 * Returns P(Family(var),evidence) where Family(var) is the set containing
	 * var and its parents.
	 */
	public Table familyJoint(FiniteVariable var)
	{
		return comp.familyJoint(var);
	}

	/**
	 * Returns P(Family(var) | evidence) where Family(var) is the set containing
	 * var and its parents.
	 */
	public Table familyConditional(FiniteVariable var)
	{
		return Table.normalize(familyJoint(var));
	}

	public double getValue()
	{
		return comp.prEvidence();
	}

	/**
	 * Returns the set of all of the variables.
	 */
	public Set variables()
	{
		return bn.vertices();
	}

	//public Table retractedJoint(FiniteVariable var){
	//	throw new UnsupportedOperationException();
	//}

	//public Table retractedConditional(FiniteVariable var){
	//	return Table.normalize(retractedJoint(var));
	//}
	public JoinTreeStats.Stat getClusterStats(){
		return comp.getClusterStats();
	}

	public JoinTreeStats.Stat getSeparatorStats(){
		return comp.getSeparatorStats();
	}

	public double getCompilationTime(){
		return comp.getCompilationTime();
	}
	public double getPropagationTime(){
		return comp.getPropagationTime();
	}
	public double getMemoryRequirements(){
		return comp.getMemoryRequirements();
	}
	public boolean isExhaustive(){
		return false;
	}
}
