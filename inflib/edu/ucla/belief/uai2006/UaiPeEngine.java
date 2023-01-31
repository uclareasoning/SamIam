package edu.ucla.belief.uai2006;
import java.util.*;

/**
 * An interface for an algorithm that computes P(e) for the 2006 UAI
 * evaluation.
 * 
 * @author Mark Chavira
 */

public interface UaiPeEngine {

  /**
   * Computes P(e) for the given network and evidence.
   *
   * @param r a random generator.
   * @param bn the given network.
   * @param e the given evidence.
   * @param pr the preprocess result.
   * @return the solution (instantiation will be an empty map).
   */

  public UaiMapSolution computePe (
   Random r, edu.ucla.belief.BeliefNetwork bn,
   java.util.Map<edu.ucla.belief.FiniteVariable,Object> e,
   UaiPreprocessResult pr) throws Exception;

}
