package il2.util;

/**
 * An <i>ordered</i> pair of ints.
 * <p>
 * This class conforms to the immutable design pattern.
 *
 * @author James Park, Mark Chavira
 */

public final class Pair {

  /**
   * The first int.
   */

  public final int s1;

  /**
   * The second int.
   */

  public final int s2;
  
  /**
   * Initializes the pair.
   *
   * @param i1 the first int.
   * @param i2 the second int.
   */
  
  public Pair (Integer i1, Integer i2) {
    this (i1.intValue(), i2.intValue());
  }
    
  /**
   * Initializes the pair.
   *
   * @param i1 the first int.
   * @param i2 the second int.
   */
  
  public Pair(int i1, int i2) {
    s1 = i1;
    s2 = i2;
  }

  /**
   * Fulfills java.lang.Object.
   */

  public boolean equals(Object obj){
    if(this==obj){
      return true;
    }
    if(!(obj instanceof Pair)){
      return false;
    }
    Pair p = (Pair)obj;
    return s1 == p.s1 && s2 == p.s2;
  }
  
  /**
   * Fulfills java.lang.Object.
   */

  public int hashCode(){
      return (s1 << 16) ^ (s2);
  }

}
