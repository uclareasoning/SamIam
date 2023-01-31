package il2.inf.structure.minfill2;

/**
 * A priority queue implemented with a heap where elements are ints in
 * [0,N-1] for some N, where keys are doubles, where lower keys are higher
 * priority, and where the queue may not contain duplicate items.  The size
 * of the queue is linear in N, so N should not be outrageous.
 *
 * @author Mark Chavira
 */

public class IntPriorityQueue extends PriorityQueue {

  protected int fNumElements;
  protected int[] fElementToSlot;
  protected int[] fElements;
  protected double[] fKeys;

  private void addElementsWhenEmpty (
   int[] elems, double[] keys) throws Exception {
    fNumElements = elems.length;
    for (int i = 0; i < elems.length; i++) {
      create (i, elems[i], keys[i]);
    }
    for (int i = fNumElements / 2 - 1; i >= 0; i--) {
      pushDown (i);
    }
  }

  /**
   * @see PriorityQueue
   */
  
  protected void exchange (int s1, int s2) {
    int e = fElements[s1];
    double k = fKeys[s1];
    set (s1, fElements[s2], fKeys[s2]);
    set (s2, e, k);
  }

  /**
   * @see PriorityQueue
   */
  
  protected boolean higherPriority (int slot1, int slot2) {
    return fKeys[slot1] < fKeys[slot2];
  }

  /**
   * @see PriorityQueue
   */
  
  public int size () {
    return fNumElements;
  }

  /**
   * @see PriorityQueue
   */
  
  public void clear () {
    for (int i = 0; i < size (); i++) {
      fElementToSlot[fElements[i]] = -1;
    }
    fNumElements = 0;
  }
  
  /**
   * Clears the priority queue and then adds the given elements.
   *
   * @param elems the given elements.
   * @param keys the given elements' keys.
   */
  
  public void clear (int[] elems, double[] keys) throws Exception {
    clear ();
    addElementsWhenEmpty (elems, keys);
  }

  /**
   * Fills the given slot with the given element after verifying that it is
   * not already in the queue.
   *
   * @param e the given element.
   */
  
  protected void create (int s, int e, double k) throws Exception {
    if (fElementToSlot[e] != -1) {
      throw new Exception ("Attempt to insert element already in queue.");
    }
    set (s, e, k);
  }

  /**
   * Fills the given slot with the given element.
   *
   * @param s the given slot.
   * @param e the given element.
   * @param k the given element's key.
   */
  
  protected void set (int s, int e, double k) {
    fElements[s] = e;
    fKeys[s] = k;
    fElementToSlot[e] = s;
  }
  
  /**
   * Initializes the priority queue with the given elements.  This method
   * runs in time linear in the number of elements, and is thus faster than
   * creating a priority queue and then adding the elements.
   *
   * @param N defines the elements that may be entered into the queue.
   * @param elems the given elements.
   * @param keys the given elements' keys.
   */

  public IntPriorityQueue (int N, int[] elems, double[] keys)
   throws Exception {
    this (N);
    addElementsWhenEmpty (elems, keys);
  }

  /**
   * Initializes an empty priority queue.
   *
   * @param N defines the elements that may be entered into the queue.
   */
  
  public IntPriorityQueue (int N) {
    fNumElements = 0;
    fElements = new int[N];
    fKeys = new double[N];
    fElementToSlot = new int[N];
    java.util.Arrays.fill (fElementToSlot, -1);
  }

  /**
   * Inserts the given element into the queue.
   *
   * @param e the given element.
   * @param k the given element's key.
   */

  public void add (int e, double k) throws Exception {
    ++fNumElements;
    create (fNumElements - 1, e, k);
    pushUp (fNumElements - 1);
  }

  /**
   * Returns the highest priority element.
   *
   * @return the highest priority element.
   */

  public int highest () {
    return fElements[0];
  }
  
  /**
   * Returns the highest priority key.
   *
   * @return the highest priority key.
   */
  
  public double highestKey () {
    return fKeys[0];
  }

  /**
   * Returns whether the queue contains the given element.
   *
   * @param e the given element.
   * @return whether the queue constains the given element.
   */

  public boolean contains (int e) {
    return fElementToSlot[e] != -1;
  }
  
  /**
   * Sets the key of the given element to the given value.
   *
   * @param e the given element.
   * @param k the given key.
   */
  
  public void setKey (int e, double k) throws Exception {
    int s = fElementToSlot[e];
    if (s == -1) {
      throw new Exception ("Element not a member of queue.");
    }
    double oldKey = fKeys[s];
    fKeys[s] = k;
    if (k < oldKey) {
      pushUp (s);
    } else {
      pushDown (s);
    }
  }

  /**
   * Changes the key of the given element by the given value.
   *
   * @param e the given element.
   * @param delta the given value.
   */
  
  public void incrementKey (int e, double delta) throws Exception {
    int s = fElementToSlot[e];
    if (s == -1) {
      throw new Exception ("Element not a member of queue.");
    }
    fKeys[s] += delta;
    if (delta < 0.0) {
      pushUp (s);
    } else {
      pushDown (s);
    }
  }

  /**
   * Removes and returns the highest priority element.  This method runs in
   * time logarithmic in the size of the heap.
   *
   * @return the removed integer.
   */
  
  public int remove () {
    int ans = highest ();
    --fNumElements;
    set (0, fElements[fNumElements], fKeys[fNumElements]);
    fElementToSlot[ans] = -1;
    pushDown (0);
    return ans;
  }

  /**
   * Returns the nth element in the priority queue, where n indicates how
   * the elements are stored not their priority.
   *
   * @param n identifies the element.
   * @return the nth element.
   */
  
  public int nthElement (int n) {
    return fElements[n];
  }
  
  /**
   * Returns the nth key in the priority queue, where n indicates how the
   * elements are stored not their priority.
   *
   * @param n identifies the element.
   * @return the nth key.
   */
  
  public double nthKey (int n) {
    return fKeys[n];
  }
  
  /**
   * @see java.lang.Object
   */
  
  public String toString () {
    StringBuffer ans = new StringBuffer ();
    ans.append ("[");
    for (int i = 0; i < size (); i++) {
      if (i > 0) {
        ans.append (",");
      }
      ans.append ("(" + fElements[i] + "->" + fKeys[i] + ")");
    }
    ans.append ("]");
    return ans.toString ();
  }

}
