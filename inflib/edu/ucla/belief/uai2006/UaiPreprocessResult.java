package edu.ucla.belief.uai2006;
import java.util.*;

/**
 * The result of preprocessing a network.
 * 
 * @author Mark Chavira
 */

public class UaiPreprocessResult {

  /**
   * The simplified potentials.
   */
  
  public Set simplifiedPotentials;
  
  /**
   * The elimination order.
   */
  
  public List eliminationOrder;
  
  /**
   * The log max cluster size of the elimination order.
   */
  
  public double logMaxClusterSize;
 
  /**
   * Additional information returned by an algorithm in <key,value> form.
   * This field is optional and so may be null.
   */
  
  public Map<String,Object> info;
  
}
