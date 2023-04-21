package edu.ucla.belief.inference;

import edu.ucla.belief.*;
import edu.ucla.belief.tree.JoinTree;
import il2.inf.structure.JoinTreeStats;

import java.util.*;

public class JoinTreeInferenceEngineImpl extends AbstractInferenceEngine implements JoinTreeInferenceEngine, PartialDerivativeEngine
{
	private BeliefCompilation comp;
	protected BeliefNetwork myBeliefNetwork;
	private double compilationTime;

	/** @since 20060321 */
	public void die(){
		super.die();
		JoinTreeInferenceEngineImpl.this.comp                         = null;
		if( myMapVariablesToConditionals != null ) myMapVariablesToConditionals.clear();
		JoinTreeInferenceEngineImpl.this.myMapVariablesToConditionals = null;
		JoinTreeInferenceEngineImpl.this.myRandom                     = null;
		JoinTreeInferenceEngineImpl.this.myBeliefNetwork              = null;
	}

	public void printInfoCompilation(java.io.PrintWriter stream){
		stream.println(STR_CONSOLE_MESSAGE_COMP_TIME+compilationTime);
		stream.println(STR_CONSOLE_MESSAGE_COMP_MEM+comp.getMemoryRequirements());
	}
	public void printInfoPropagation(java.io.PrintWriter stream){
		stream.println(STR_CONSOLE_MESSAGE_PROP_TIME+comp.getPropagationTime());
	}
	/** @since 061504 */
	public void printTables( java.io.PrintWriter stream ){
		printTables( comp.getTables(), stream );
	}

	/** @since 061404 */
	public InferenceEngine handledClone( QuantitativeDependencyHandler handler )
	{
		JoinTree jt = comp.getJoinTree();
		JoinTreeInferenceEngineImpl ret = JEngineGenerator.createInferenceEngine( this.myBeliefNetwork, this.getDynamator(), jt, handler );
		ret.setQuantitativeDependencyHandler( handler );
		return ret;
	}

	public JoinTreeStats.StatsSource getJoinTreeStats()
	{
		return comp.getJoinTree();
	}

	protected static Random myRandom = new Random();

	/**
		Constructs a new JoinTreeInferenceEngineImpl that wraps a BeliefCompilation.
		It is intended to provide a convinient wrapper for BeliefCompilation
		which allows most of the common functions to be performed in a straight
		forward way.

		A given BeliefCompilation must be used with only one JoinTreeInferenceEngineImpl
		since the inference engine modifies the state of the BeliefCompilation
		in the process of performing queries. Multiple engines would conflict
		and produce incorrect results.
	*/
	public JoinTreeInferenceEngineImpl( BeliefCompilation comp, Dynamator dyn )
	{
		super( dyn );
		this.comp = comp;
		this.myBeliefNetwork = comp.getBeliefNetwork();
		myBeliefNetwork.getEvidenceController().addPriorityEvidenceChangeListener( this );
	}

	/**
		@author Keith Cascio
		@since 011703
	*/
	/*
	protected void finalize() throws Throwable
	{
		//System.out.println( "JoinTreeInferenceEngineImpl.finalize()" );
	}*/

	public BeliefCompilation underlyingCompilation()
	{
		return comp;
	}

	/**
		For interface EvidenceChangeListener
		@author Keith Cascio
		@since 071902
	*/
	protected void updateLikelihood( FiniteVariable var, Object value )
	{
		double[] values = new double[ var.size() ];
		if( value == null )
		{
			Arrays.fill(values, 1);
		}
		else
		{
			int ind = var.index(value);
			values[ind] = 1;
		}

		comp.setLikelihood(var, values);
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
		EvidenceController controller = myBeliefNetwork.getEvidenceController();
		FiniteVariable var = null;
		for( Iterator it = ECE.recentEvidenceChangeVariables.iterator(); it.hasNext(); )
		{
			var = (FiniteVariable) it.next();
			updateLikelihood( var, controller.getValue( var ) );
		}
	}

	/**
		@author Keith Cascio
		@since 060702
	*/
	private void checkValidVariables( Set varsToCheck )
	{
		Set networkVariables = comp.variables();
		if( !networkVariables.containsAll( varsToCheck ) )
		{
			throw new RuntimeException( "Variable not contained in this belief network." );
		}
	}

	/**
	* Sets the CPT associated with var to the values in vals.
	* The ordering of vars must be consistent with the ordering
	* returned by getCPT(var).
	* @param var The variable whose CPT we want to set.
	* @param vals The values of the entries in the cpt.
	*/
	public void setCPT( FiniteVariable var )
	{
		clearCache();
		comp.setFamily( var, getEffectiveCPTData( var ) );//var.getCPTShell( var.getDSLNodeType() ).getCPT().dataclone() );
	}

	/**
		@author Keith Cascio
		@since 102902
	*/
	public double probability()
	{
		if(	myBeliefNetwork.getEvidenceController().isEmpty() ) return (double)1;
		else return comp.getValue();
	}

	/**
	* Returns P(var,observations).
	*/
	public Table joint(FiniteVariable var)
	{
		Table partial = comp.getPartial(var);
		Table likeli = comp.getLikelihood(var);
		if( partial == null || likeli == null ) return null;
		else return Table.innerProduct( partial, likeli );
	}

	/**
	* Returns P(var | observations).
	*/
	public Table conditional(FiniteVariable var)
	{
		if( myMapVariablesToConditionals.containsKey( var ) )
		{
			++myCacheHits;
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

	/**
		@author Keith Cascio
		@since 082002
	*/
	public int getCacheHits()
	{
		return myCacheHits;
	}

	protected int myCacheHits = (int)0;
	protected Map myMapVariablesToConditionals = new HashMap();

	/**
	* Returns P(var,(observations-evidence(var)))
	*/
	//public Table retractedJoint(FiniteVariable var)
	//{
	//	return comp.getPartial(var);
	//}

	/**
	* Returns P(var | observations-evidence(var)). For example, if the
	* evidence set is {X=a,Y=b,Z=c}, calling this function with X returns
	* P(X | Y=b,Z=c).
	*/
	//public Table retractedConditional(FiniteVariable var)
	//{
	//	return Table.normalize(retractedJoint(var));
	//}

	/**
	* Returns P(Family(var),evidence) where Family(var) is the set containing
	* var and its parents.
	*/
	public Table familyJoint(FiniteVariable var)
	{
		Table fampartial = comp.getFamilyPartial(var);
		Table famtable = comp.getFamilyTable(var);
		if( fampartial == null || famtable == null ) return null;
		else return Table.innerProduct( fampartial, famtable );
	}

	/**
	* Returns P(Family(var) | evidence) where Family(var) is the set containing
	* var and its parents.
	*/
	public Table familyConditional(FiniteVariable var)
	{
		return Table.normalize(familyJoint(var));
	}

	/**
	* Returns the partial derivatives of the probability function with respect
	* to the variable var.
	*/
	public Table partial(FiniteVariable var)
	{
		return comp.getPartial(var);
	}

	/**
	* Returns the partial derivatives of the probability function with respect
	* to the family table of var.
	*/
	public Table familyPartial(FiniteVariable var)
	{
		return comp.getFamilyPartial(var);
	}

	public double getValue()
	{
		return comp.getValue();
	}

	/**
	* Returns the set of all of the variables.
	*/
	public Set variables()
	{
		return comp.variables();
	}

        void setCompilationTime(double t){
	    compilationTime=t;
        }

        public boolean isExhaustive(){
	    return true;
        }
}
