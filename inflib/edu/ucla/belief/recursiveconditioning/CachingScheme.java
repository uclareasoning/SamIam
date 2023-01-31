package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.hugin.*;


/** This abstract class creates RC objects (RCDtrees and RCDgraphs) by giving assigning
 *  the caching to the nodes.
 *
 * @author David Allen
 */

abstract public class CachingScheme {

    static final private boolean DEBUG_CS = false;

    //For now, force user to use the double cacheFactor.  TODO: Eventually would like them to
    // be able to specify an actual upper bound on the number of cache entries.
    protected double cacheFactor;

    public CachingScheme( double cacheFactor) {
        setCacheFactor( cacheFactor);
    }

    public String toString() { return "CachingScheme";}

    public double getCacheFactor() { return cacheFactor;}

    /** Fraction of maximum possible cache to use.*/
    public void setCacheFactor( double cacheFactor) {
        if( cacheFactor < 0.0 || cacheFactor > 1.0) {
            throw new IllegalArgumentException("Illegal Cache Factor: " + cacheFactor);
        }
        this.cacheFactor = cacheFactor;
    }


	/** Have this CachingScheme allocate the memory for the RC object.
	  * @param listnr Some CachingSchemes use this to return updates to (can be null).
	  */
    public void allocateMemory( RCDtree tree, RCCreateListener listnr) {allocateMemory( (RC)tree, listnr);}
	/** Have this CachingScheme allocate the memory for the RC object.
	  * @param listnr Some CachingSchemes use this to return updates to (can be null).
	  */
    public void allocateMemory( RCDgraph graph, RCCreateListener listnr) {allocateMemory( (RC)graph, listnr);}
	/** Have this CachingScheme allocate the memory for the RC object.
	  * @param listnr Some CachingSchemes use this to return updates to (can be null).
	  */
    abstract public void allocateMemory( RC rc, RCCreateListener listnr);





    /*An interface which the longer "search" CachingSchemes will use to report the progress
     * of their search.  Look at the CachingScheme of interest to determine whether it uses this
     * interface or not.
     */
    public interface RCCreateListener {

	/*Will call this function during the creation of an RC object whenever the new best cost
	* changes by more than rcCreateUpdateThreshold() from the last call to update.
	*/
	void rcCreateUpdate( double bestCost);

	/*The "frequency" which the listener wants to hear about updates.*/
	double rcCreateUpdateThreshold();

	/*Return true to "stop" current search and return best result so far.  This function will tell the
	* search algorithm to stop, but the user should wait for calls to the rcCreateDone functions to
	* actually know when the search finishes.
	*/
	boolean rcCreateStopRequested();

	/*Will be called upon "completion of search" or upon "stop requested"
	* (optimal=true means completion, optimal = false means stopped).
	*/
	void rcCreateDone( double bestCost, boolean optimal);

	/*Will be called by the static method which creates a RC in a separate thread.
	* The standard constructor will not call this method.
	*/
	void rcCreateDone( RC rc);

	/**
		@author Keith Cascio
		@since 060303
	*/
	void rcConstructionDone();

	/**
		@author Keith Cascio
		@since 060303
	*/
	void rcCreateError( String msg );
    }


//    /** For each DtreeNode in nodeToVars, it will calculate Vars - Context and store
//     *   that in a map.
//     */
//    final static public Map createVarsToSaveMPE( Map contexts, Map nodeToVars) {
//        Map mpeVars = new HashMap();
//        for( Iterator itr = nodeToVars.keySet().iterator(); itr.hasNext();) {
//            Object dtn = itr.next();
//            HashSet vars = new HashSet( (Collection)nodeToVars.get(dtn));
//            vars.removeAll( (Collection)contexts.get(dtn));
//            mpeVars.put( dtn, vars);
//        }
//        return mpeVars;
//    }



}//end class CachingScheme

