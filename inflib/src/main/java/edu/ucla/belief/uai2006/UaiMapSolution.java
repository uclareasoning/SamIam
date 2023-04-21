package edu.ucla.belief.uai2006;
import java.util.*;

/**
 * A solution for a problem.  The competition now only uses MPE or P (e).
 * 
 * @author Mark Chavira and Arthur Choi
 */

public class UaiMapSolution {

  /**
   * Additional information from the algorithm in the form of <key,value>
   * pairs.  This information is optional, and so this field may be null.
   */
   
   public Map<String,Object> info;

  /**
   * The instantiation of MAP variables.  This field will be null if the
   * query is P (e) or if algorithm did not compute an instantiation (the
   * evaluation does not require algorithms to compute instantiations).
   */
   
  public Map<edu.ucla.belief.FiniteVariable,Object> instantiation;
    
  /**
   * The computed P (e) or null if computing MPE.
   */
   
  public java.math.BigDecimal probability;

  /**
   * The computed mpe probability in log_e space or Double.NaN if computing
   * P (e).
   */
  
  public double log_probability;

  /**
   * The constructor, which sets instantiation, probability, and info to null
   * and log_probability to Double.NaN.
   */

  public UaiMapSolution() {
    this.info = null;
    this.instantiation = null;
    this.probability = null;
    this.log_probability = Double.NaN;
  }
  
  /**
   * Converts a double to a big decimal.
   *
   * @param d the given double.
   * @return the big decimal.
   */

  public static java.math.BigDecimal toBigDecimal(double d) {
    java.math.MathContext mc = java.math.MathContext.DECIMAL64;
    return new java.math.BigDecimal(d,mc);
  }

}
