package edu.ucla.belief.uai2006;
import java.util.*;

/**
 * Utilities to make avaiable to the uai main program.
 * 
 * @author chavira
 */

public interface UaiUtilsIf {

  /**
   * Preprocess the given network according to the given evidence and returns
   * the result.
   * 
   * @param r a random generator.
   * @param bn the given network.
   * @param e the given evidence.
   * @return the result of preprocessing. 
   */
  
  public UaiPreprocessResult preprocessNet (
   Random r, edu.ucla.belief.BeliefNetwork bn,
   Map<edu.ucla.belief.FiniteVariable,Object> e,
   boolean mpe) throws Exception;
  
}
