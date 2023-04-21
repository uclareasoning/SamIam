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

public class TestMpeEngine implements UaiMpeEngine {

  /**
   * Computes MAP for the given network, evidence, and map variables.
   *
   * @param bn the given network.
   * @param e the given evidence.
   * @param m the given map variables.
   * @return the map solution.
   */

  public UaiMapSolution computeMpe (
   Random r, edu.ucla.belief.BeliefNetwork bn,
   java.util.Map<edu.ucla.belief.FiniteVariable,Object> e,
   UaiPreprocessResult pr) throws Exception {

	  Map prunede = new HashMap<FiniteVariable,Object>(e);
	  BeliefNetwork prunedbn = bn;

	  Set allVars = new HashSet( prunedbn );
	  allVars.removeAll( prunede.keySet() );
	  edu.ucla.belief.inference.map.MapEngine mpe =
		  new edu.ucla.belief.inference.map.MapEngine(prunedbn,allVars,prunede);

	  // compute solution
	  UaiMapSolution solution = new UaiMapSolution();
	  solution.instantiation = Collections.EMPTY_MAP;
	  solution.log_probability = Math.log(mpe.probability());
	  return solution;
  }

}
