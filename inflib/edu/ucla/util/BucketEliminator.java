/*
* BucketEliminator.java
*
* Created on February 24, 2000, 3:32 PM
*/
package edu.ucla.util;
import java.util.*;
import edu.ucla.structure.MappedList;
/**
*
* @author unknown
* @version
*/
public abstract class BucketEliminator extends Object {
    protected Set[] buckets;
    protected MappedList order;
    protected Set forgotten;
    boolean combineFinalBucket;
    /** Creates new BucketEliminator */
    public BucketEliminator() {
    }
    public BucketEliminator(List order, Collection elements,
            boolean combineFinalBucket) {
        initialize(order, elements, combineFinalBucket);
    }
    public void initialize(List order, Collection elements,
            boolean combineFinalBucket) {
        this.combineFinalBucket = combineFinalBucket;
        this.order = new MappedList(order);
        buckets = new Set[order.size() + 1];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new HashSet();
        }
        Iterator iter = elements.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            buckets[getIndex(obj, -1)].add(obj);
        }
        forgotten = new HashSet();
    }
    protected Set eliminate() {
        for (int i = 0; i < buckets.length - 1; i++) {
            Set newVals = combine(buckets[i], order.get(i));
            Iterator iter = newVals.iterator();
            while (iter.hasNext()) {
                Object newVal = iter.next();
                int ind = getIndex(newVal, i);
                buckets[ind].add(newVal);
            }
            buckets[i].clear();
            forgotten.add(order.get(i));
        }
        if (combineFinalBucket) {
            buckets[buckets.length - 1] =
                    combine(buckets[buckets.length - 1], null);
        }
        return buckets[buckets.length - 1];
    }
    private int getIndex(Object obj, int minval) {
        Collection vars = variables(obj);
        int bestBucket = buckets.length - 1;
        Iterator iter = vars.iterator();
        while (iter.hasNext()) {
            int num = order.indexOf(iter.next());
            if (num != -1) {
                if (minval < num && num < bestBucket) {
                    bestBucket = num;
                }
            }
        }
        return bestBucket;
    }
    protected abstract Set combine(Set elements, Object eliminate);
    protected abstract Collection variables(Object element);
}
