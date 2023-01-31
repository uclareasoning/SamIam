package edu.ucla.belief.recursiveconditioning;

import java.util.*;

import edu.ucla.util.*;



/** This class iterates through trees & graphs but does not guarantee
 *  any particular node ordering.
 *
 * <p> This one is designed to use more memory but run faster than RCIteratorTraversal.
 *
 * @author David Allen
 */
public class RCIteratorArray extends RCIterator {

    static final private boolean DEBUG_RCIteratorArray = false;

    RCNode arr[];
    int indx = 0;


    public RCIteratorArray( RCNode root) {
        this( new RCIteratorTraversal( root));
    }

    public RCIteratorArray( RCNode[] roots) {
        this( new RCIteratorTraversal( roots));
    }

    public RCIteratorArray( Collection roots) {
        this( new RCIteratorTraversal( roots));
    }

    public RCIteratorArray( RCIterator itr) {
        ArrayList tmp = new ArrayList();
        while( itr.hasNext()) {
            tmp.add( itr.next());
        }
        arr = new RCNode[tmp.size()];
        arr = (RCNode[])tmp.toArray( arr);
    }

    /**Will iterate using nodesToIterate (no copies are made).
     *
     * <p>Guarantees ordering in nodesToIterate.
     */
    public RCIteratorArray( RCNode[] nodesToIterate, Object dummy) {
        Assert.notNull( nodesToIterate, "nodesToIterate cannot be null");
        arr = nodesToIterate;
    }

    public void restart() { indx = 0;}

    public boolean hasNext() { 
        if( indx >= arr.length) { return false;}
        else { return true;}
    }

    /** Does not guarantee any particular ordering of the nodes returned.*/
    public RCNode nextNode() {
        if( !hasNext()) {
            throw new NoSuchElementException();
        }
        indx++;
        return arr[indx-1];
    }

}//end class RCIteratorArray

