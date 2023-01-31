package edu.ucla.belief.inference;

import edu.ucla.belief.*;
import edu.ucla.belief.tree.JoinTree;

import java.util.*;

public class BeliefCompilation implements java.io.Serializable
{
    private ArithmeticExpression expr;
    private Map likelihoodLocations;
    private Map familyLocations;
    private TableIndex[] indices;
    private Set variables;
    protected BeliefNetwork myBeliefNetwork;
    public final JoinTree myJoinTree;
    private final Table[] myTables;

	/**
	* Creates a new BeliefCompilation. This method is not intended for
	* general use, but is provided for any brave souls who wish to
	* perform custom compilations. Typical users should use the compilation
	* routine provided in BeliefNetworks.
	* @param expr An ArithmeticExpression that encapsulates the dependencies
	* between the network parameters.
	* @param inds The TableIndex for each of the parameters in expr.
	* @param likelihoodIndices A mapping from each variable to the index of
	* that parameter in expr.
	* @param familyIndices A mapping from each variable to the corresponding
	* index of its family in expr.
	*/
	public BeliefCompilation(	BeliefNetwork bn,
					ArithmeticExpression expr,
					TableIndex[] inds,
					Map likelihoodIndices,
					Map familyIndices )
	{
		this( bn, expr, inds, likelihoodIndices, familyIndices, (JoinTree)null, (Table[])null );
	}

	/**
		@author Keith Cascio
		@since 110102
	*/
	public BeliefCompilation(	BeliefNetwork bn,
					ArithmeticExpression expr,
					TableIndex[] inds,
					Map likelihoodIndices,
					Map familyIndices,
					JoinTree jt,
					Table[] tables )
	{
		this.myBeliefNetwork = bn;
		this.expr = expr;
		this.indices = (TableIndex[]) inds.clone();
		this.likelihoodLocations = new HashMap(likelihoodIndices);
		this.familyLocations = new HashMap(familyIndices);
		this.variables = Collections.unmodifiableSet( new HashSet( familyLocations.keySet() ) );
		this.myJoinTree = jt;
		this.myTables = tables;
	}

	/**
		@author Keith Cascio
		@since 061504
	*/
	public Table[] getTables(){
		return myTables;
	}

	/**
		@author Keith Cascio
		@since 071902
	*/
	public BeliefNetwork getBeliefNetwork(){
		return myBeliefNetwork;
	}

	/**
		@author Keith Cascio
		@since 110102
	*/
	public JoinTree getJoinTree(){
		return myJoinTree;
	}

    /**
    *  Returns the likelihoods(Evidence indicators) for the given variable.
    */
    public Table getLikelihood(FiniteVariable var) {
        int index = getIndex(likelihoodLocations, var);
        if( index == INDEX_UNKNOWN_VARIABLE ) return null;
        else return new Table(indices[index], expr.getParameter(index));
    }
    public Map lambdaLocations(){
        return new HashMap(likelihoodLocations);
    }
    public Map thetaLocations(){
        return new HashMap(familyLocations);
    }

    /**
    *  Returns the value of the CPT for the variable.
    */
    public Table getFamilyTable(FiniteVariable var) {
        int index = getIndex(familyLocations, var);
        if( index == INDEX_UNKNOWN_VARIABLE ) return null;
        else return new Table(indices[index], expr.getParameter(index));
    }

    /**
    *  Returns the partial derivative of the likelihoods for the current
    *  values of the parameters.
    */
    public Table getPartial(FiniteVariable var) {
        int index = getIndex(likelihoodLocations, var);
        if( index == INDEX_UNKNOWN_VARIABLE ) return null;
        else return new Table(indices[index], expr.getPartial(index));
    }

    /**
    *  Returns the partial derivative of the CPT for the current values of
    *  the parameters.
    */
    public Table getFamilyPartial(FiniteVariable var)
    {
        int index = getIndex(familyLocations, var);
        if( index == INDEX_UNKNOWN_VARIABLE ) return null;
        else
        {
        	double[] partial = expr.getPartial(index);
        	//System.out.println( "BeliefCompilation.getFamilyPartial()" + var + "  length=" + partial.length );
        	return new Table(indices[index], partial );
        }
    }

    /**
    *  Sets the likelihood parameters for the variable supplied.  The length
    *  of vals should be the same as the the number of instantiations var
    *  can take on.
    */
    public void setLikelihood(FiniteVariable var, double[] vals) {
        int index = getIndex(likelihoodLocations, var);
        if( index != INDEX_UNKNOWN_VARIABLE )
        {
        	expr.setParameter(index, vals);
	}
    }

    /**
    *  Sets the family parameters for the variable supplied.  The ordering in
    *  vals should correspond to the ordering in the table returned from
    *  getFamily.
    */
    public void setFamily(FiniteVariable var, double[] vals) {
        int index = getIndex(familyLocations, var);
        if( index != INDEX_UNKNOWN_VARIABLE )
        {
        	expr.setParameter(index, vals);
	}
    }

    /** @since 20020701 */
    protected static final int INDEX_UNKNOWN_VARIABLE = (int)-1;

    private int getIndex(Map m, FiniteVariable var)
    {
		Integer ret = (Integer) m.get(var);
		if( ret == null ){
			if( Definitions.DEBUG ){ Definitions.STREAM_VERBOSE.println( "Warning: BeliefCompilation.getIndex( " + var + " )" ); }
			return INDEX_UNKNOWN_VARIABLE;
		}
		return ret.intValue();
    }

    public double getValue() {
        return expr.getValue();
    }

    public Set variables() {
        return variables;
    }

    /**
     *@deprecated
     */
    public int nodeCount() {
        return expr.nodeCount();
    }
    /**
     *@deprecated
     */
    public int edgeCount() {
        return expr.edgeCount();
    }
    public ArithmeticExpression getExpression(){
        return expr;
    }

    public double getPropagationTime(){
	return expr.getPropagationTime();
    }

    public double getMemoryRequirements(){
	return expr.getMemoryRequirements();
    }
}
