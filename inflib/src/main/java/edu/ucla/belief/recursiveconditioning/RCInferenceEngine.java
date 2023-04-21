package edu.ucla.belief.recursiveconditioning;

import java.util.*;

import edu.ucla.belief.*;

public class RCInferenceEngine extends AbstractInferenceEngine implements InferenceEngine
{
	protected boolean RCIE_Debug = BeliefNetworkImpl.FLAG_DEBUG;

	private RCDgraph rcdgraph;
	protected BeliefNetwork myBeliefNetwork;

	/** @since 20091226 */
	public String compilationStatus( edu.ucla.belief.io.PropertySuperintendent bn ){
		return "recursive conditioning " + edu.ucla.belief.recursiveconditioning.RCEngineGenerator.getSettings( bn ).describeUserMemoryProportion();
	}

	/** @since 20060321 */
	public void die(){
		super.die();
		RCInferenceEngine.this.rcdgraph        = null;
		RCInferenceEngine.this.myRandom        = null;
		RCInferenceEngine.this.myBeliefNetwork = null;
	}

 	/** @since 061404 */
 	public InferenceEngine handledClone( QuantitativeDependencyHandler handler ){
 		return (InferenceEngine)null;
	}

	/** @since 061504 */
	public void printTables( java.io.PrintWriter out ){
		throw new UnsupportedOperationException();
		//printTables( comp.getTables(), out );
	}

	/** @since 20030314 */
  /*private boolean verifyVariableConsistency( java.io.PrintStream stream ){
		stream.println( "RCInferenceEngine.verifyVariableConsistency()" );
		Collection rcvars = rcdgraph.vars;
		boolean ret = myBeliefNetwork.containsAll( rcvars ) && rcvars.containsAll( myBeliefNetwork );
		if( ! ret ){ stream.println( "inconsistent! rcdgraph" + rcdgraph.hashCode() + ", ("+myBeliefNetwork.getClass().getName()+")myBeliefNetwork:" + myBeliefNetwork.hashCode() ); }
		return ret;
	}*/

	protected static Random myRandom = new Random();

    /**
        Constructs a new RCInferenceEngine that wraps a RCDgraph.
        It is intended to provide a convinient wrapper for RCDgraph
        which allows most of the common functions to be performed in a straight
        forward way.

        A given RCDgraph must be used with only one RCInferenceEngine
        since the inference engine modifies the state of the RCDgraph
        in the process of performing queries. Multiple engines would conflict
        and produce incorrect results.
    */
    public RCInferenceEngine( RCDgraph rcdgraph, Dynamator dyn, BeliefNetwork beliefNetwork )
    {
        super( dyn );

//        System.out.println( "RCInferenceEngine()" );
        //new Throwable().printStackTrace();

        this.rcdgraph = rcdgraph;
        this.myBeliefNetwork = beliefNetwork;

        rcdgraph.resetEvidence();
        myBeliefNetwork.getEvidenceController().addPriorityEvidenceChangeListener( this );
    }


    public RCDgraph underlyingCompilation()
    {
        return rcdgraph;
    }

	/**
		For interface EvidenceChangeListener
		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ece ) {}

    /**
    *   For interface EvidenceChangeListener.
    */
    public void evidenceChanged( EvidenceChangeEvent ECE )
    {
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: evidenceChanged");}
        EvidenceController controller = myBeliefNetwork.getEvidenceController();
        FiniteVariable var = null;
        for( Iterator it = ECE.recentEvidenceChangeVariables.iterator(); it.hasNext(); )
        {
            var = (FiniteVariable) it.next();
            rcdgraph.observe( var, controller.getValue( var));
        }
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: end evidenceChanged");}
    }


	/**
	* This function tells the dgraph to clear all caches above where var's leaf
	*  node is.
	* @param var The variable whose CPT we want to set.
	* @param vals The values of the entries in the cpt.
	*/
	public void setCPT( FiniteVariable var )
	{
		if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: setCPT on " + var); }
		rcdgraph.setCPT( var );
	}

    /**
    * Returns P(e).
    */
    public double probability()
    {
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: probability");}
        return rcdgraph.recCond_Pe();
    }

    /**
    * Returns P(var,observations).
    */
    public Table joint(FiniteVariable var)
    {
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: joint on " + var);}
        Table ret = rcdgraph.joint( var);
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: end joint on " + var);}
        return ret;
    }

    /**
    * Returns P(var | observations).
    */
    public Table conditional(FiniteVariable var)
    {
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: conditional on " + var);}
        return Table.normalize( joint(var));
    }

    /**
    * Returns P(var,(observations-evidence(var))).
    */
    /*
    public Table retractedJoint(FiniteVariable var)
    {
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: retractedJoint on " + var);}
        EvidenceController controller = myBeliefNetwork.getEvidenceController();

        Object value = controller.getValue( var);
        rcdgraph.observe( var, null);
        Table ret = joint( var);
        rcdgraph.observe( var, value);
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: end retractedJoint on " + var);}
        return ret;
    }*/

    /**
    * Returns P(var | observations-evidence(var)). For example, if the
    * evidence set is {X=a,Y=b,Z=c}, calling this function with X returns
    * P(X | Y=b,Z=c).
    */
    /*
    public Table retractedConditional(FiniteVariable var)
    {
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: retractedConditional on " + var);}
        return Table.normalize(retractedJoint(var));
    }*/

    /**
    * Returns P(Family(var),evidence) where Family(var) is the set containing
    * var and its parents.
    */
    public Table familyJoint(FiniteVariable var)
    {
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: familyJoint on " + var);}
        return rcdgraph.familyJoint( var);
    }

    /**
    * Returns P(Family(var) | evidence) where Family(var) is the set containing
    * var and its parents.
    */
    public Table familyConditional(FiniteVariable var)
    {
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: familyConditional on " + var);}
        return Table.normalize(familyJoint(var));
    }

    public double getValue()
    {
        if( RCIE_Debug) { Definitions.STREAM_VERBOSE.println("RCIE: getValue");}
        return rcdgraph.recCond_Pe();
    }

    /**
    * Returns the set of all of the variables.
    */
    public Set variables()
    {
        return new HashSet( rcdgraph.vars);
    }

    public boolean isExhaustive(){
	return true;
    }
}
