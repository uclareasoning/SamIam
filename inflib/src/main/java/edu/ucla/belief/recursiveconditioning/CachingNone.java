package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;



public class CachingNone extends CachingScheme {

    public CachingNone() {
        super( 0.0);
    }

    public String toString() { return "No Caching";}

    public void setCacheFactor( double cacheFactor) {
        if( cacheFactor != 0.0 /*&& DEBUG_RCDtree*/) {
            System.err.println("Warning: Tried to call setCacheFactor on the NoCaching CachingScheme. (" + cacheFactor + ")");
        }
        super.setCacheFactor(0.0);
    }

    public void allocateMemory( RC rc, RCCreateListener listnr) {
        RCIterator itr = rc.getIterator();
        while( itr.hasNext()) {
            RCNode n = itr.nextNode();
            if( n.numCacheEntries_local_total() > 0) {
                n.changeCacheFactor(0.0);
            }
        }
    }

}//end class CachingNone
