package edu.ucla.belief.uai2006;
import java.util.*;

/**
 * An interface for an algorithm that computes MAP for the 2006 UAI
 * evaluation.
 * 
 * @author Mark Chavira
 */

public interface UaiMapEngine {

  /**
   * Computes MAP for the given network, evidence, and map variables.
   *
   * @param r a random generator.
   * @param bn the given network.
   * @param e the given evidence.
   * @param m the given map variables.
   * @return the map solution.
   */

  public UaiMapSolution computeMap (
   Random r,
   edu.ucla.belief.BeliefNetwork bn,
   java.util.Map<edu.ucla.belief.FiniteVariable,Object> e,
   java.util.Set<edu.ucla.belief.FiniteVariable> m) throws Exception;

}
