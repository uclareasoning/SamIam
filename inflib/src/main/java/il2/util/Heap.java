package il2.util;
import java.util.*;
/**
* A class implementing the logic for a binary heap.
*/
public abstract class Heap {
    private int heapsize;
    /**
     * Creates an empty heap.
     */
    public Heap() {
        heapsize = 0;
    }
    protected abstract boolean isBetter(int i,int j);
    protected abstract void swap(int i,int j);
    protected void initialize(int size){
        heapsize=size;
        for(int i=heapsize-1;i>=0;i--){
            heapify(i);
        }
    }
    /**
     * needs to be called when an element is added to the heap.
     */
    protected final void elementAdded(){
        int i = heapsize;
        heapsize++;
        propagateUp(i);
    }
    /**
     * Needs to be called if the priority of an element changes.
     */
    protected final void valueChanged(int index) {
        if(index>0 && isBetter(index,parent(index))) {
            propagateUp(index);
        } else if(heapsize>0) {
            heapify(index);
        }
    }
    /**
     * Returns whether or not the heap is empty.
     */
    public final boolean isEmpty() {
        return heapsize <= 0;
    }
    /**
     * Returns the number of elements currently in the heap.
     */
    public final int size() {
        return heapsize;
    }
    protected final void removeTop() {
        heapsize--;
        swap(0, heapsize);
        heapify(0);
    }
    /**
     * makes the heap starting at i satisfy the heap property, assuming that
     * left(i) and right(i) already satisfy it.
     */
    private void heapify(int i) {
        int bestind;
        if (left(i) >= heapsize) {
            return;
        } else if(isBetter(left(i),i)) {
            bestind = left(i);
        } else {
            bestind = i;
        }
        if (right(i) < heapsize && isBetter(right(i),bestind)) {
            bestind = right(i);
        }
        if (bestind != i) {
            swap(bestind, i);
            heapify(bestind);
        }
    }
    private void propagateUp(int i) {
        while (i>0 && isBetter(i,parent(i))) {
            swap(i,parent(i));
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


    public void sanityCheck(){
	for(int i=1;i<heapsize;i++){
	    if(!isBetter(i,parent(i))){
		throw new IllegalStateException("Heap property violated");
	    }
	}
    }
}
