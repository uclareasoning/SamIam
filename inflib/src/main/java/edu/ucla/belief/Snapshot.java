package edu.ucla.belief;

import java.util.*;

/** Takes a snapshot of the current network state, including evidence and its
probability, the cpts, the conditionals, and the partial derivatives */
public class Snapshot
{
	private double probEvidence;
	private Hashtable evidenceMap, cptMap, conditionalMap, cptPartialMap;

	public Snapshot(BeliefNetwork bn, InferenceEngine ie,
		PartialDerivativeEngine pde)
	{
		Object[] vertices = bn.vertices().toArray();
		probEvidence = ie.probability();
		evidenceMap = new Hashtable(bn.getEvidenceController().evidence());
		cptMap = new Hashtable();
		conditionalMap = new Hashtable();
		cptPartialMap = new Hashtable();
		for (int i = 0; i < vertices.length; i++)
		{
			FiniteVariable var = (FiniteVariable)vertices[i];
			cptMap.put(var, var.getCPTShell( var.getDSLNodeType() ).getCPT().dataclone());
			conditionalMap.put(var, ie.conditional(var).dataclone());
			cptPartialMap.put(var, pde.familyPartial(var).dataclone());
		}
	}

	public double getProbEvidence()
	{
		return probEvidence;
	}

	public Map getEvidenceMap()
	{
		return Collections.unmodifiableMap(evidenceMap);
	}

	public Map getCPTMap()
	{
		return Collections.unmodifiableMap(cptMap);
	}

	public Map getConditionalMap()
	{
		return Collections.unmodifiableMap(conditionalMap);
	}

	public Map getCPTPartialMap()
	{
		return Collections.unmodifiableMap(cptPartialMap);
	}

}
