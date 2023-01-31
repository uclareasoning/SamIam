package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 *	@author Taylor Curtis
 */
public class RCFormInferenceEngineImpl extends AbstractInferenceEngine implements RCFormInferenceEngine, EvidenceChangeListener
{
	//Need some kind of dtree member, that comes with pre-processing already done
	//Maybe also need a beliefnetwork member

	public RCFormInferenceEngineImpl(Dynamator dyn)
	{
		super(dyn);
		System.out.println("In RCFormInferenceEngineImpl constructor");
	}

	public InferenceEngine handledClone( QuantitativeDependencyHandler handler )
	{
		throw new UnsupportedOperationException();
	}

	public void printTables( java.io.PrintWriter out )
	{
		throw new UnsupportedOperationException();
	}

	public int random( FiniteVariable var )
	{
		throw new UnsupportedOperationException();
	}

	//This can probably stay unsupported
	public void setCPT( FiniteVariable var )
	{
		throw new UnsupportedOperationException();
	}

	public double probability()
	{
		throw new UnsupportedOperationException();
	}

	//Delete this once probability() is supported
	public boolean probabilitySupported()
	{
		return false;
	}

	/**
	* Returns P(var,observations).
	*/
	public Table joint(FiniteVariable var)
	{
		throw new UnsupportedOperationException();
	}

	/**
	* Returns P(var | observations).
	*/
	public Table conditional(FiniteVariable var)
	{
		throw new UnsupportedOperationException();
	}

	/**
	* Returns P(Family(var),evidence) where Family(var) is the set containing
	* var and its parents.
	*/
	public Table familyJoint(FiniteVariable var)
	{
		throw new UnsupportedOperationException();
	}

	/**
	* Returns P(Family(var) | evidence) where Family(var) is the set containing
	* var and its parents.
	*/
	public Table familyConditional(FiniteVariable var)
	{
		throw new UnsupportedOperationException();
	}

	/**
	* Returns the set of all of the variables.
	*/
	public java.util.Set variables()
	{
		throw new UnsupportedOperationException();
	}

	public boolean isExhaustive()
	{
		throw new UnsupportedOperationException();
	}

	/** interface EvidenceChangeListener */
	public void warning( EvidenceChangeEvent ece )
	{
		throw new UnsupportedOperationException();
	}

	/** interface EvidenceChangeListener */
	public void evidenceChanged( EvidenceChangeEvent ece )
	{
		throw new UnsupportedOperationException();
	}

	/** interface ActionListener *//*
	public void actionPerformed( ActionEvent event ){
		//setting changed
	}*/
}
