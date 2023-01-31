package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;



public class CachingFull extends CachingUniform {
    public CachingFull() {
        super( 1.0);
    }

    public String toString() { return "Full Caching";}

    public void setCacheFactor( double cacheFactor) {
        if( cacheFactor != 1.0 /*&& DEBUG_RCDtree*/) {
            System.err.println("Warning: Tried to call setCacheFactor on the FullCaching CachingScheme. (" + cacheFactor + ")");
        }
        super.setCacheFactor(1.0);
    }

}//end class CachingFull
