package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;



public class CachingNonOptimalSize extends CachingScheme {


	static private boolean DEBUG_nonOptimal = false;
	static private boolean DEBUG_nonOptimal_verbose = false;

    static private boolean UselessCacheTest_Static = true;  //should find all



	/**From brief experimentation, Low_To_High=true tends to do better than false,
	 *  however, sometimes false can do better.
	 */
	public boolean CF_LOW_TO_HIGH = true;


    public CachingNonOptimalSize( boolean CF_LOW_TO_HIGH) {
        this( 1.0); //full caching is default
        this.CF_LOW_TO_HIGH = CF_LOW_TO_HIGH;
    }

    public CachingNonOptimalSize( ) {
        this( 1.0); //full caching is default
    }

    public CachingNonOptimalSize( double cacheFactor) {
        super( cacheFactor);
    }

    public String toString() {
		if( CF_LOW_TO_HIGH) {
			return "CachingNonOptimalSize(LOW_TO_HIGH)";
		}
		else {
			return "CachingNonOptimalSize(HIGH_TO_LOW)";
		}
    }




    public void allocateMemory( RC rc, RCCreateListener listnr) {

        if( DEBUG_nonOptimal) { Definitions.STREAM_VERBOSE.println("\n\nBegin CachingNonOptimalSize.allocateMemoryNonOptimalSize " + cacheFactor);}

        CachingDFBnB.DFBnB_TmpVars tmpVars = new CachingDFBnB.DFBnB_TmpVars( rc, CachingDFBnB.OrderingAlgo.defaultVal());
		CachingDFBnB.SearchGraph sg = tmpVars.sg();



        long maxAcceptableCache;
		{
			maxAcceptableCache = (long)Math.floor( cacheFactor * tmpVars.sg().numCachingFullMinusWorthless());

//			long maxCache = 0;
//			RCIterator itr = rc.getIterator();
//			while( itr.hasNext()) {
//				maxCache += itr.nextNode().contextInstantiations();
//			}
//			maxAcceptableCache = (long)Math.floor( cacheFactor * maxCache);
		}
        if( DEBUG_nonOptimal) Definitions.STREAM_VERBOSE.println("CachingNonOptimalSize:maxAcceptableCache: " + maxAcceptableCache);


		//order variables
        if( DEBUG_nonOptimal_verbose) Definitions.STREAM_VERBOSE.println("order variables");

		ArrayList order = new ArrayList( sg.nodes);
		Collections.sort( order, rcNodeComparator_Size);  //sort variables in increasing order

        if( DEBUG_nonOptimal_verbose) {
			Definitions.STREAM_VERBOSE.println("order: " + order);
	        for( int i=0; i<order.size(); i++) {
				Definitions.STREAM_VERBOSE.print(( (RCNode)order.get(i)).contextInstantiations() + ",");
			}
			Definitions.STREAM_VERBOSE.println("");
		}


		//start setting cf until out of memory
        if( DEBUG_nonOptimal_verbose) Definitions.STREAM_VERBOSE.println("assign cf");


		long usedMemory = 0;

		int indx;
		if( CF_LOW_TO_HIGH)	{ indx = 0;}
		else { indx = order.size()-1;}

		while( true) {
			RCNode nd = (RCNode)order.get( indx);

			if( nd.numCacheEntries_local_total() > 0) {
				//if not worthless & not too much memory, then set cf=1
				int sgInd = sg.nodes().indexOf( nd);
				if( sg.currCF[sgInd] != 0) { //not worthless
					if( usedMemory + nd.numCacheEntries_local_total() <= maxAcceptableCache) { //not too much memory
						usedMemory += nd.numCacheEntries_local_total();
						if( DEBUG_nonOptimal_verbose) Definitions.STREAM_VERBOSE.println("Node " + nd + " was cached, now using " + usedMemory + " out of (allowed) " + maxAcceptableCache);
						nd.changeCacheFactor( 1);
					}
					else {
						if( DEBUG_nonOptimal_verbose) Definitions.STREAM_VERBOSE.println("Node " + nd + " was NOT cached, now using " + usedMemory + " out of (allowed) " + maxAcceptableCache);
						nd.changeCacheFactor( 0);
//						if( CF_LOW_TO_HIGH) { break;}
					}
				}
				else {
					nd.changeCacheFactor( 0);
					if( DEBUG_nonOptimal_verbose) {
						Definitions.STREAM_VERBOSE.println("Node " + nd + " was worthless");
					}
				}
			}


			//determine when to stop
			if( CF_LOW_TO_HIGH) {indx++; if( indx >= order.size()) break;}
			else {indx--; if( indx < 0) break;}
		}


        if( DEBUG_nonOptimal) {
			Definitions.STREAM_VERBOSE.println("Expected Calls_all = " + rc.statsAll().expectedNumberOfRCCalls());
			double tmem = rc.statsAll().numCacheEntries();
			Definitions.STREAM_VERBOSE.println("Expected Cache Entries = " + tmem);
			Definitions.STREAM_VERBOSE.println("Expected CacheMB = " + (tmem * 8.0 / 1048576.0));
		}


        if( DEBUG_nonOptimal_verbose) { Definitions.STREAM_VERBOSE.println("End allocateMemoryNonOptimalSize");}

    }//end allocateMemoryDFBnB




    public static final RCNodeComparator_Size rcNodeComparator_Size
        = new RCNodeComparator_Size();

    /**Not consistent with equals!*/
    static public class RCNodeComparator_Size
        implements Comparator {

        public int compare(Object o1, Object o2) {
            RCNode n1 = (RCNode)o1;
            RCNode n2 = (RCNode)o2;

            long num1 = n1.contextInstantiations();
            long num2 = n2.contextInstantiations();
            if( num1 < num2) { return -1;}
            else if( num1 > num2) { return 1;}

            num1 = n1.cutsetInstantiations();
            num2 = n2.cutsetInstantiations();
            if( num1 < num2) { return -1;}
            else if( num1 > num2) { return 1;}
            else { return 0;} //possibly inconsistent with equals...
        }
    }
}





