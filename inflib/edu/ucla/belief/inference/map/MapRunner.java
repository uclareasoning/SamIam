package edu.ucla.belief.inference.map;

import edu.ucla.belief.*;
import edu.ucla.belief.inference.BeliefCompilation;
//{superfluous} import edu.ucla.belief.inference.JoinTreeInferenceEngine;
import edu.ucla.belief.inference.JoinTreeInferenceEngineImpl;
import edu.ucla.util.JVMProfiler;

import java.util.*;

public class MapRunner
{

        public static class MapResult {
          public long pruneDurationMillisElapsed = (long)-1;
          public long pruneDurationMillisProfiled = (long)-1;
          public long initDurationMillisElapsed = (long)-1;
          public long initDurationMillisProfiled = (long)-1;
          public long searchDurationMillisElapsed = (long)-1;
          public long searchDurationMillisProfiled = (long)-1;
          //public long initStart;
          //public long initStop;
          //public long searchStart;
          //public long searchStop;
          public Map instantiation;
          public double score;
          public MapResult (
           long iElapsed, long iProfiled, long sElapsed, long sProfiled,
           Map inst, double s) {
            initDurationMillisElapsed = iElapsed;
            initDurationMillisProfiled = iProfiled;
            searchDurationMillisElapsed = sElapsed;
            searchDurationMillisProfiled = sProfiled;
            instantiation = inst;
            score = s;
          }
        }

	public MapResult approximateMap( BeliefNetwork bn,JoinTreeInferenceEngineImpl ie,
					Set mapvars,Map evidence,SearchMethod searchMethod,
					InitializationMethod initializationMethod,int evaluations)
	{
		//Map oldEvidence=new HashMap(ie.evidence());
		Map oldEvidence = new HashMap( bn.getEvidenceController().evidence() );
		MapResult result=approximateMap(bn,ie.underlyingCompilation(),mapvars,evidence,searchMethod,initializationMethod,evaluations);
		setEvidence(ie.underlyingCompilation(),oldEvidence);
		return result;
	}

	public MapResult approximateMap( BeliefNetwork bn,BeliefCompilation comp,
					Set mapvars,Map evidence,SearchMethod searchMethod,
					InitializationMethod initializationMethod,int evaluations)
	{

                long iStart = System.currentTimeMillis ();
                long istart_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();

		Set test=new HashSet(mapvars);
		test.retainAll(evidence.keySet());
		if( test.size()!=0 ) throw new IllegalArgumentException("The set of map variables and the set of evidence variables must be disjoint");

		setEvidence(comp,evidence);

		Map initial = initializationMethod.getInitial( bn, evidence, mapvars, comp );
		int penalty = initializationMethod.getPenalty();

		MapApproximator approximator = searchMethod.getApproximator();

		approximator.init(comp,mapvars);
		MapApproximator.Instance instance= approximator.new Instance(initial);

                long istop_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
                long iStop = System.currentTimeMillis ();
                long sStart = System.currentTimeMillis ();
                long sstart_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();

		approximator.run(instance,evaluations-penalty);

                long sstop_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
                long sStop = System.currentTimeMillis ();

		myLastScore = approximator.bestInstance().score();

      long iElapsed = iStop - iStart;
      long iProfiled = istop_cpu_ms - istart_cpu_ms;
      long sElapsed = sStop - sStart;
      long sProfiled = sstop_cpu_ms - sstart_cpu_ms;

		return
                  new MapResult (
                    iElapsed, iProfiled, sElapsed, sProfiled,
                    approximator.bestInstance().mapping(), myLastScore);

	}

	/**
		@author Keith Cascio
		@since 021903
	*/
	public double getLastScore()
	{
		return myLastScore;
	}

	protected double myLastScore = (double)-1;

	private void setEvidence(BeliefCompilation comp,Map evidence)
	{
		for(Iterator iter=comp.variables().iterator();iter.hasNext();)
		{
			FiniteVariable var=(FiniteVariable)iter.next();
			double[] lik=new double[var.size()];
			if(evidence.containsKey(var))
			{
				Object value=evidence.get(var);
				lik[var.index(value)]=1;
			}
			else java.util.Arrays.fill(lik,1);

			comp.setLikelihood(var,lik);
		}
	}
}
