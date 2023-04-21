package edu.ucla.structure;
import java.util.*;
/**
* A class implementing a binary heap.
*/
public class Heap {
    protected MappedList elements;
    protected int heapsize;
    /**
     * Creates an empty heap.
     */
    public Heap() {
        elements = new MappedList();
        heapsize = 0;
    }
    /**
     * Creates a heap consisting made up of the objects, with corresponding
     * priorities.
     */
    public Heap(Object[] objs, double[] scores) {
        elements = new MappedList(objs.length);
        for (int i = 0; i < objs.length; i++) {
            elements.add(new HeapElement(objs[i], scores[i]));
        }
        heapsize = objs.length;
        for (int i = heapsize - 1; i >= 0; i--) {
            heapify(i);
        }
    }
    /**
     * Adds element to heap.
     */
    public void insert(Object obj, double score) {
        HeapElement he = new HeapElement(obj, score);
        int i = heapsize;
        elements.set(i, he);
        heapsize++;
        propagateUp(i);
    }
    /**
     * Modifies the priority of one of the heap elements.
     */
    public void setValue(Object obj, double score) {
        HeapElement he = new HeapElement(obj, score);
        int index = elements.indexOf(he);
        boolean increased = score > score(index);
        elements.set(index, he);
        if (increased) {
            propagateUp(index);
        } else {
            heapify(index);
        }
    }
    /**
     * Returns whether or not the heap is empty.
     */
    public boolean isEmpty() {
        return heapsize <= 0;
    }
    /**
     * Returns the number of elements currently in the heap.
     */
    public int size() {
        return heapsize;
    }
    /**
     * Removes the highest priority element and returns it along with its
     * priority.
     */
    public HeapElement extractMax() {
        HeapElement result = (HeapElement) elements.get(0);
        heapsize--;
        elements.swap(0, heapsize);
        elements.clear(heapsize);
        heapify(0);
        return result;
    }
    /**
     *Returns the maximum score in the heap.  Added by Mark Chavira 2004-06-17.
     */

    public double maxScore () {
      return ((HeapElement)elements.get(0)).score ();
    }

    /**
     * Helper class for implementing an object priority pair.
     */
    public static final class HeapElement {
        Object obj;
        double score;
        private HeapElement(Object obj, double score) {
            this.obj = obj;
            this.score = score;
        }
        /**
         * Returns the hashCode of the object it contains.
         */
        public int hashCode() {
            return obj.hashCode();
        }
        public boolean equals(Object o) {
            if (o instanceof HeapElement) {
                HeapElement he = (HeapElement) o;
                return obj.equals(he.obj);
            } else {
                return false;
            }
        }
        /**
         * returns the object the HeapElement contains.
         */
        public Object element() {
            return obj;
        }
        /**
         * returns the priority associated with the element.
         */
        public double score() {
            return score;
        }
        public String toString() {
            return "HeapElement["+obj + ","+score + "]";
        }
    }
    /**
     * returns the priority of the element at location i in the heap.
     */
    private double score(int i) {
        HeapElement he = (HeapElement) elements.get(i);
        return he.score;
    }
    /**
     * makes the heap starting at i satisfy the heap property, assuming that
     * left(i) and right(i) already satisfy it.
     */
    private void heapify(int i) {
        int bestind;
        if (left(i) >= heapsize) {
            return;
        } else if (score(left(i)) > score(i)) {
            bestind = left(i);
        } else {
            bestind = i;
        }
        if (right(i) < heapsize && score(right(i)) > score(bestind)) {
            bestind = right(i);
        }
        if (bestind != i) {
            elements.swap(bestind, i);
            heapify(bestind);
        }
    }
    private void propagateUp(int i) {
        double score = score(i);
        while (score > score(parent(i))) {
            elements.swap(i, parent(i));
            i = parent(i);
        }
    }
    private static final int parent(int i) {
        return (i - 1) / 2;
    }
    private static final int left(int i) {
        return 2 * i + 1;
    }
    private static final int right(int i) {
        return 2 * i + 2;
    }
}
