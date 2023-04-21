package il2.util;

import java.math.BigInteger;

/**
 * Static utility methods acting on arrays of primitive types.
 *
 * @author James Park, Mark Chavira, Keith Cascio
 */
public class ArrayUtils{

  private static final class DoubleIndQuickSort extends IndQuickSort{
    private final double[] vals;
    public DoubleIndQuickSort (double[] vals) {
      super (vals.length);
      this.vals = vals;
    }
    protected final boolean isLess (int i,int j) {
      return vals[inds[i]] < vals[inds[j]];
    }
  }

  private static final class IntIndQuickSort extends IndQuickSort{
    private final int[] vals;
    public IntIndQuickSort (int[] vals) {
      super (vals.length);
      this.vals = vals;
    }
    protected final boolean isLess (int i, int j) {
      return vals[inds[i]] < vals[inds[j]];
    }
  }

  private static abstract class IndQuickSort extends QuickSort{
    protected final int[] inds;
    public IndQuickSort (int count) {
      super (count);
      inds = sequence (count);
    }
    protected final void swap (int i, int j) {
      int temp = inds[i];
      inds[i] = inds[j];
      inds[j] = temp;
    }
  }

  /**
   * Returns the cumulative product of the given array.  The cumulative
   * product of array a is an array ans such that:
   * <ul>
   * <li> ans.length = a.length + 1
   * <li> ans[0] = 1
   * <li> ans[i+1] = a[i] * ans[i]
   * </ul>
   *
   * @param vals the given array.
   * @return the cumulative product.
   */

  public static int[] cumProd (int[] vals) {
    int[] result = new int[vals.length+1];
    result[0] = 1;
    for(int i = 0; i < vals.length; i++) {
      result[i+1] = result[i] * vals[i];
    }
    return result;
  }

  /** @author Keith Cascio
      @since 020705 */
  public static long[] cumProdAsLong( int[] vals ){
    long[] result = new long[vals.length+1];
    result[0] = 1;
    for(int i = 0; i < vals.length; i++) {
      result[i+1] = result[i] * ((long)vals[i]);
    }
    return result;
  }

  /** @author Keith Cascio
      @since 020305 */
  public static long[] cumProd (long[] vals) {
    long[] result = new long[vals.length+1];
    result[0] = 1;
    for(int i = 0; i < vals.length; i++) {
      result[i+1] = result[i] * vals[i];
    }
    return result;
  }

  /** @author Keith Cascio
      @since 020305 */
  public static long cumProdLastLong( int[] vals ){
    long result = 1;
    for( int i = 0; i < vals.length; i++ ) {
      result = result * ((long)vals[i]);
    }
    return result;
  }

  /** @author Keith Cascio
      @since 020305 */
  public static BigInteger cumProdLastBigInteger( int[] vals ){
    BigInteger result = BigInteger.ONE;
    for( int i = 0; i < vals.length; i++ ) {
      result = result.multiply( BigInteger.valueOf( (long)vals[i] ) );
    }
    return result;
  }

  /**
   * Returns an array that contains selected values from the given array.
   *
   * @param vals the given array.
   * @param inds the indices of the values to select.
   * @return an array ans of length inds.length where ans[i] = vals[inds[i]]
   */

  public static int[] select (int[] vals, int[] inds) {
    int[] result = new int[inds.length];
    for(int i=0; i < inds.length; i++) {
      result[i] = vals[inds[i]];
    }
    return result;
  }

  /**
   * Returns an array that contains selected values from the given array.
   *
   * @param vals the given array.
   * @param inds the indices of the values to select.
   * @return an array ans of length inds.length where ans[i] = vals[inds[i]]
   */

  public static double[] select (double[] vals, int[] inds) {
    double[] result = new double[inds.length];
    for (int i = 0; i < inds.length; i++) {
      result[i] = vals[inds[i]];
    }
    return result;
  }

  /** @author Keith Cascio
      @since 020305 */
  public static long[] select( long[] vals, int[] inds ){
    long[] result = new long[inds.length];
    for( int i = 0; i < inds.length; i++ ){
      result[i] = vals[inds[i]];
    }
    return result;
  }

  /**
   * Fills an array with selected values from the given array.
   *
   * @param vals the given array.
   * @param inds the indices, which together with offset, define the values
   *   to select.
   * @param offset the value, which together with indices, defines the values
   *   to select.
   * @return dest an array with the same length as indices; upon exit,
   *   dest[i] = vals[offset + inds[i]].
   */

  public static void selectWithOffset (
   double[] vals, int[] inds, int offset, double[] dest) {
    for (int i=0; i<inds.length; i++) {
      dest[i] = vals[inds[i] + offset];
    }
  }

  /**
   * Returns an array that contains selected values from the given array.
   *
   * @param vals the given array.
   * @param inds the indices, which together with offset, define the values
   *   to select.
   * @param offset the value, which together with indices, defines the values
   *   to select.
   * @return an array ans of length inds.length where
   *   ans[i] = vals[offset + inds[i]]
   */

  public static double[] selectWithOffset (
   double[] vals, int[] inds, int offset) {
    double[] result = new double[inds.length];
    for (int i = 0; i < inds.length; i++) {
      result[i] = vals[inds[i] + offset];
    }
    return result;
  }

  /**
   * Returns a string representation of the given array.
   *
   * @param vals the given array.
   * @return the string representation.
   */

  public static String toString (int[] vals) {
    StringBuffer b = new StringBuffer (vals.length * 3 + 5);
    b.append ('[');
    for (int i = 0; i < vals.length; i++) {
      b.append (' ');
      b.append (vals[i]);
    }
    b.append (']');
    return b.toString ();
  }

  /**
   * Returns a string representation of the given array.
   *
   * @param vals the given array.
   * @return the string representation.
   */

  public static String toString (double[] vals) {
    StringBuffer b = new StringBuffer (vals.length*7+5);
    b.append('[');
    for (int i = 0; i < vals.length; i++) {
      b.append (' ');
      b.append (vals[i]);
    }
    b.append (']');
    return b.toString();
  }

  /**
   * Returns the number of occurrences of the given value in the given array.
   *
   * @param vals the given array.
   * @param val the given value.
   * @return the number of occurrences.
   */

  public static int count (boolean[] vals, boolean val) {
    int count = 0;
    for(int i = 0; i < vals.length; i++) {
      if (vals[i] == val) {
        count++;
      }
    }
    return count;
  }

  /**
   * Returns the sum of the values in the given array.
   *
   * @param vals the given array.
   * @return the sum.
   */

  public static double sum (double[] vals) {
    double result = 0;
    for (int i = 0; i < vals.length; i++) {
      result += vals[i];
    }
    return result;
  }

  /**
   * Returns the sum of the given consecutive range of values in the given
   * array.  The range is [start, bound).
   *
   * @param vals the given array.
   * @param start the first element to sum (inclusive).
   * @param bound the last element to sum (exclusive).
   * @return the sum.
   */

  public static double intervalSum (double[] vals,int start,int bound){
    double total = 0;
    for (int i = start; i < bound; i++) {
      total += vals[i];
    }
    return total;
  }

  /**
   * Returns the minimum of the values in the given array.  If the array is
   * empty, returns Integer.MAX_VALUE.
   *
   * @param vals the given array.
   * @return the minimum.
   */

  public static int min (int[] vals) {
    int min=Integer.MAX_VALUE;
    for (int i=0; i < vals.length; i++) {
      if (vals[i] < min) {
        min = vals[i];
      }
    }
    return min;
  }

  /**
   * Returns the maximum of the values in the given array.  If the array is
   * empty, returns Integer.MIN_VALUE.
   *
   * @param vals the given array.
   * @return the maximum.
   */

  public static int max (int[] vals) {
    int max=Integer.MIN_VALUE;
    for (int i = 0; i < vals.length; i++) {
      if (vals[i] > max) {
        max = vals[i];
      }
    }
    return max;
  }

  /**
   * Returns the index of the minimum value in the given array.  If the
   * array is empty, returns -1.
   *
   * @param vals the given array.
   * @return the index or -1.
   */

  public static int minInd (int[] vals) {
    if (vals.length == 0) {
      return -1;
    }
    int ind = 0;
    for (int i = 1; i < vals.length; i++) {
      if (vals[i] < vals[ind]) {
        ind = i;
      }
    }
    return ind;
  }
  /**
   * Returns the index of the maximum value in the given array.  If the
   * array is empty, returns -1.
   *
   * @param vals the given array.
   * @return the index or -1.
   */

  public static int maxInd (int[] vals) {
    if (vals.length == 0) {
      return -1;
    }
    int ind = 0;
    for (int i = 1; i < vals.length; i++) {
      if (vals[i] > vals[ind]) {
        ind = i;
      }
    }
    return ind;
  }

  /**
   * Returns the minimum of the values in the given array.  If the array is
   * empty, returns Double.POSITIVE_INFINITY.
   *
   * @param vals the given array.
   * @return the maximum.
   */


  public static double min (double[] vals) {
    double min = Double.POSITIVE_INFINITY;
    for(int i = 0; i < vals.length; i++) {
      if (vals[i] < min) {
        min = vals[i];
      }
    }
    return min;
  }

  /**
   * Returns the maximum of the values in the given array.  If the array is
   * empty, returns Double.NEGATIVE_INFINITY.
   *
   * @param vals the given array.
   * @return the maximum.
   */

  public static double max (double[] vals) {
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < vals.length; i++) {
      if (vals[i] > max) {
        max = vals[i];
      }
    }
    return max;
  }

  /**
   * Returns the index of the minimum value in the given array.  If the
   * array is empty, returns -1.
   *
   *
   * @param vals the given array.
   * @return the index or -1.
   */

  public static int minInd (double[] vals) {
    if (vals.length == 0) {
      return -1;
    }
    int ind = 0;
    for (int i = 1; i < vals.length; i++) {
      if (vals[i] < vals[ind]) {
        ind=i;
      }
    }
    return ind;
  }

  /**
   * Returns the index of the maximum value in the given array.  If the
   * array is empty, returns -1.
   *
   * @param vals the given array.
   * @return the index or -1.
   */

  public static int maxInd (double[] vals) {
    if (vals.length == 0) {
      return -1;
    }
    int ind = 0;
    for (int i = 1;i < vals.length; i++) {
      if (vals[i] > vals[ind]) {
        ind = i;
      }
    }
    return ind;
  }

  /**
   * Returns an array of doubles containing the same values as the given
   * array of ints.
   *
   * @param vals the given array of ints.
   * @return the array of doubles.
   */

  public static double[] toDoubles (int[] vals) {
    double[] result = new double[vals.length];
    for (int i = 0; i < vals.length; i++) {
      result[i] = vals[i];
    }
    return result;
  }

  /**
   * Returns an array of ints containing the same values as the given array
   * of doubles after the double values are rounded.
   *
   * @param vals the given array of doubles.
   * @return the array of ints.
   */

  public static int[] toInts(double[] vals){
      int[] result=new int[vals.length];
      for(int i=0;i<vals.length;i++){
          result[i]=(int)Math.round(vals[i]);
      }
      return result;
  }

  /**
   * Returns an array of indices of the the values in the given array
   * sorted according to the values they index.
   *
   * @param vals the given array.
   * @return the indices.
   */

  public static int[] sortedInds (double[] vals) {
    DoubleIndQuickSort sorter = new DoubleIndQuickSort (vals);
    sorter.sort ();
    return sorter.inds;
  }

  /**
   * Returns an array of indices of the the values in the given array
   * sorted according to the values they index.
   *
   * @param vals the given array.
   * @return the indices.
   */

  public static int[] sortedInds (int[] vals) {
    IntIndQuickSort sorter = new IntIndQuickSort (vals);
    sorter.sort ();
    return sorter.inds;
  }

  /**
   * Returns an array that contains the same values as the given array but
   * reversed.
   *
   * @param vals the given array.
   * @return the reversed array.
   */

  public static int[] reversed (int[] vals) {
    int[] result = new int[vals.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = vals[vals.length - 1 - i];
    }
    return result;
  }

  /**
   * Returns an array of ints [0, 1, 2, ..., L - 1], where L is the given
   * length.
   *
   * @param len the given length.
   * @return the array.
   */

  public static int[] sequence (int len){
    int[] result = new int[len];
    for (int i=0; i<len; i++) {
      result[i] = i;
    }
    return result;
  }

}
