package edu.ucla.belief;

/** @author keith cascio
	@since  20030115 */
public interface InferenceEngine extends EvidenceChangeListener
{
	/** @since 20091226 */
	public InferenceEngine canonical();
	/** @since 20091226 */
	public String  compilationStatus( edu.ucla.belief.io.PropertySuperintendent bn );

	/** @since 20091218 */
	public java.util.Collection/*<DirectedEdge>*/ notoriousEdges();

	/** @since 061304 */
	public void setQuantitativeDependencyHandler( QuantitativeDependencyHandler handler );
	public QuantitativeDependencyHandler getQuantitativeDependencyHandler();
	public boolean isQuantitativeDependencyHandled();

	/** @since 061404 */
	public InferenceEngine handledClone( QuantitativeDependencyHandler handler );

	/** @since 091803 */
	public void printInfoCompilation( java.io.PrintWriter out );
	public void printInfoPropagation( java.io.PrintWriter out );
	public void printTables( java.io.PrintWriter out );

	/** @since 030603 */
	public int random( FiniteVariable var );

	/** @since 012103 */
	public void setDynamator( Dynamator dyn );

	/** @since 012103 */
	public Dynamator getDynamator();

	/** @since 012103 */
	public void setValid( boolean flag );

	/** @since 090704 */
	public void die();

	/** @since 012103 */
	public boolean getValid();

	/**
	* Sets the CPT associated with var to the values in vals.
	* The ordering of vars must be consistent with the ordering
	* returned by getCPT(var).
	* @param var The variable whose CPT we want to set.
	* @param vals The values of the entries in the cpt.
	*/
	public void setCPT( FiniteVariable var );

	/** @since 20021029 */
	public double probability();

	/** @since 20080221 */
	public char   probabilityDisplayOperatorUnicode();

	/** @since 20050830 */
	public boolean probabilitySupported();

	/**
	* Returns P(var,observations).
	*/
	public Table joint(FiniteVariable var);

	/**
	* Returns P(var | observations).
	*/
	public Table conditional(FiniteVariable var);

	/** In case this InferenceEngine wants to report two sets of answers,
		for example, approximate and exact.
		@since 20080226 */
	public Table[] conditionals( FiniteVariable var, Table[] buckets );

	/** In case this InferenceEngine wants to report two sets of answers,
		for example, approximate and exact.
		@since 20080226 */
	public String[] describeConditionals();

	/** @since 20080227 */
	public java.awt.Component getControlPanel();
	/** @since 20080227 */
	public InferenceEngine    setControlPanel( java.awt.Component panel );

	/**
		@ret The maximum single conditional probability
		value < 1, over all variables.
		@author Keith Cascio
		@since 071003
	*/
	public double max();

	/**
	* Returns P(var,(observations-evidence(var)))
	*/
	//public Table retractedJoint(FiniteVariable var);

	/**
	* Returns P(var | observations-evidence(var)). For example, if the
	* evidence set is {X=a,Y=b,Z=c}, calling this function with X returns
	* P(X | Y=b,Z=c).
	*/
	//public Table retractedConditional(FiniteVariable var);

	/**
	* Returns P(Family(var),evidence) where Family(var) is the set containing
	* var and its parents.
	*/
	public Table familyJoint(FiniteVariable var);

	/**
	* Returns P(Family(var) | evidence) where Family(var) is the set containing
	* var and its parents.
	*/
	public Table familyConditional(FiniteVariable var);

	//public double getValue();

	/**
	* Returns the set of all of the variables.
	*/
	public java.util.Set variables();

	public boolean isExhaustive();
}
