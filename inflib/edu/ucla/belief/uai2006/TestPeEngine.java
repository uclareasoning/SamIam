package edu.ucla.belief.uai2006;

import edu.ucla.belief.*;
import edu.ucla.belief.inference.*;

import il2.inf.*;
import il2.model.*;
//{superfluous} import il2.bridge.Converter;

import java.util.*;


/**
 * An interface for an algorithm that computes MAP for the 2006 UAI
 * evaluation.
 * 
 * @author Arthur Choi
 */

public class TestPeEngine implements UaiPeEngine {

  /**
   * Computes MAP for the given network, evidence, and map variables.
   *
   * @param bn the given network.
   * @param e the given evidence.
   * @param m the given map variables.
   * @return the map solution.
   */

  public UaiMapSolution computePe (
   Random r, edu.ucla.belief.BeliefNetwork bn,
   java.util.Map<edu.ucla.belief.FiniteVariable,Object> e,
   UaiPreprocessResult pr) throws Exception {

	  // return 1.0 for empty evidence
	  if ( e.size() == 0 ) {
		  UaiMapSolution solution = new UaiMapSolution();
		  solution.instantiation = Collections.EMPTY_MAP;
		  solution.probability = java.math.BigDecimal.ONE;
		  return solution;
	  }

	  // prune network
	  Set queryVars = Collections.EMPTY_SET;
	  HashMap prunede = new HashMap(e.size());
	  BeliefNetwork prunedbn = Prune.prune(bn, queryVars, e, new HashMap(), new HashMap(), new HashSet(), prunede);

	  /*
	  Map prunede = new HashMap<FiniteVariable,Object>(e);
	  BeliefNetwork prunedbn = bn;
	  */

	  // start inference engine
	  ZCEngineGenerator dynamator = new ZCEngineGenerator();
	  InferenceEngine ie = dynamator.manufactureInferenceEngine( prunedbn );
	  EvidenceController ec = prunedbn.getEvidenceController();
	  ec.setObservations(prunede);

	  // compute solution
	  UaiMapSolution solution = new UaiMapSolution();
	  solution.instantiation = Collections.EMPTY_MAP;
	  solution.probability = UaiMapSolution.toBigDecimal(ie.probability());
	  return solution;
  }

}
