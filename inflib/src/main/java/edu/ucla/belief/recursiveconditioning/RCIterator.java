package edu.ucla.belief.recursiveconditioning;

import java.util.*;



/** This abstract class iterates through trees & graphs but does not guarantee
 *  any particular node ordering.
 *
 * @author David Allen
 */
abstract public class RCIterator
    implements Iterator {

    abstract public boolean hasNext();

    /** Does not guarantee any particular ordering of the nodes returned.*/
    public Object next() { return nextNode();}

    /** Does not guarantee any particular ordering of the nodes returned.*/
    abstract public RCNode nextNode();

    public void remove() {
        throw new UnsupportedOperationException("Can't remove");
    }

    abstract public void restart();

}//end class RCIterator

