package il2.inf.structure.minfill2;

/**
 * A base class for priority queues.
 *
 * @author Mark Chavira
 */

public abstract class PriorityQueue {

  /**
   * Exchanges the contents of the two given slots.
   *
   * @param s1 the first given slot.
   * @param s2 the second given slot.
   */
    
  protected abstract void exchange (int s1, int s2);

  /**
   * Returns whether the content of the first given slot is higher priority
   * than the content of the second given slot.
   *
   * @param s1 the first given slot.
   * @param s2 the second given slot.
   * @return the comparison.
   */
  
  protected abstract boolean higherPriority (int s1, int s2);

  /**
   * Returns the parent slot of the given slot.
   *
   * @param slot the given slot.
   * @return the parent slot.
   */

  protected int parent (int s) {
    return (s - 1) / 2;
  }
  
  /**
   * Returns the left child slot of the given slot.
   *
   * @param slot the given slot.
   * @return the left child slot.
   */
  
  
  protected int leftChild (int s) {
    return (2 * s) + 1;
  }
  
  /**
   * Returns the right child slot of the given slot.
   *
   * @param slot the given slot.
   * @return the right child slot.
   */
  
  protected int rightChild (int s) {
    return (2 * s) + 2;
  }
  
  /**
   * Ensures that the tree is a heap, assuming that the only possible
   * violation is that the content of the given slot may need to be pushed
   * lower.
   *
   * @param s the given slot.
   */

  protected void pushDown (int s) {
    for (;;) {
      int l = leftChild (s);
      int highest = s;
      if (l >= size ()) {
        break;
      }
      if (higherPriority (l, s)) {
        highest = l;
      }
      int r = rightChild (s);
      if (r < size () && higherPriority (r, highest)) {
        highest = r;
      }
      if (highest == s) {
        break;
      }
      exchange (s, highest);
      s = highest;
    }
  }

  /**
   * Ensures that the tree is a heap assuming that the only possible
   * violation is that the content of the given slot may need to be pushed
   * higher.
   *
   * @param s the given slot.
   */
  
  protected void pushUp (int s) {
    for (;;) {
      if (s == 0) {
        break;
      }
      int p = parent (s);
      if (!higherPriority (s, p)) {
        break;
      }
      exchange (s, p);
      s = p;
    }
  }

  /**
   * Throws an exception if the heap property is not satisfied.  This method
   * is for debugging.
   */
  
  public void validateHeapProperty () throws Exception {
    if (size () == 0) {
      return;
    }
    for (int s = 0; ; s++) {
      int l = leftChild (s);
      if (l >= size ()) {
        break;
      }
      if (higherPriority (l, s)) {
        throw new Exception ("Heap property violated!");
      }
      int r = rightChild (s);
      if (r >= size ()) {
        break;
      }
      if (higherPriority (r, s)) {
        throw new Exception ("Heap property violated!");
      }
    }
  }

  /**
   * Returns the number of elements in the queue.
   *
   * @return the number of elements.
   */
  
  public abstract int size ();

  /**
   * Resets the priority queue.
   */
  
  public abstract void clear ();

}
