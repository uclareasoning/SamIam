package il2.util;

/**
 * A <i>sorted</i> pair of ints.
 * <p>
 * This class conforms to the immutable design pattern.
 *
 * @author James Park, Mark Chavira
 */

public final class UPair{

  /**
   * The smaller int.
   */

  public final int s1;

  /**
   * The larger int.
   */

  public final int s2;
  
  /**
   * Initializes the upair from another upair.
   *
   * @param p the other upair.
   */
  
  public UPair (Pair p) {
    this (p.s1, p.s2);
  }
  
  /**
   * Initializes the upair; sorts the ints before storing them.
   *
   * @param i1 one of the ints.
   * @param i2 the other int.
   */
  
  public UPair (Integer i1,Integer i2) {
    this (i1.intValue(), i2.intValue());
  }
  
  /**
   * Initializes the upair; sorts the ints before storing them.
   *
   * @param i1 one of the ints.
   * @param i2 the other int.
   */
  
  public UPair (int i1, int i2) {
    if(i1 <= i2) {
      s1 = i1;
      s2 = i2;
    } else {
      s1 = i2;
      s2 = i1;
    }
  }

  /**
   * Fulfills java.lang.Object.
   */

  public boolean equals (Object obj) {
    if(this == obj){
      return true;
    }
    if(!(obj instanceof UPair)){
      return false;
    }
    UPair up = (UPair)obj;
    return s1 == up.s1 && s2 == up.s2;
  }
  
  /**
   * Fulfills java.lang.Object.
   */

  public int hashCode(){
      return (s1<<16) ^ (s2);
  }

}
