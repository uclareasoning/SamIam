package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;



public class CachingUniform extends CachingScheme {

    public CachingUniform( ) {
        this( 0.75);
    }
    public CachingUniform( double cf) {
        super( cf);
    }

    public String toString() { return "Uniform Caching(" + cacheFactor + ")";}

    public void allocateMemory( RC rc, RCCreateListener listnr) {
        RCIterator itr = rc.getIterator();
        while( itr.hasNext()) {
            RCNode n = itr.nextNode();
            if( n.numCacheEntries_local_total() > 0) {
                n.changeCacheFactor(cacheFactor);
            }
        }
    }

}//end class CachingUniform
