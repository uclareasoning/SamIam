package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.io.*;
import edu.ucla.util.*;
import edu.ucla.belief.*;



/*  This class has multiple uses.
 *  1) Stanard RC: Uses cacheRC to store regular RC computations.
 *  2) Original MPE: Uses cacheMPEc and cacheMPEArr to store MPE computations
 *  4) MPE3: Uses cacheMPEc to store MPE computations
 */


/*  This class forces discrete caching (i.e. either a cache factor of 1 or 0).
 */


/** This class represents Caches for RCNodes.
 *
 * @author David Allen
 */
final public class RCNodeCache {
//    static final private boolean DEBUG_RCNodeCache = false;

	public static final boolean DEBUG_CACHE_SIZE = false;
	public static final long LARGEST_ACCEPTABLE_CACHE = Integer.MAX_VALUE;
	public static final double DoNotCache = -2.0;
	public static final double ShouldBeCached = -3.0;
	public static final double MB = 8.0 / 1048576.0; //8.0 for double, 1048576 for Byte->MB (2^20)



//Cached Values for MPE
//[0.0 .. 1.0] actual values
//(-0.0 .. -1.0] LookAhead bounds (upper bounds)
//[-2.0] DoNotCache
//[-3.0] ShouldBeCached
//These can be changed and no other code should rely on these particular values.

	final RCNode nodeWhereCaching;
	final int[] instantiation;


	final public boolean thisCacheOverFlowed;
	boolean cached; //allow other to look at it, but it should only be changed through changeCacheFactor!
	final int totalSize;  //size requirement under full caching (if overflowed, it is set to 0, as no caching is allowed)



	final int[] contextIndices;
	final int[] contextSizes;

	double[] cacheRC;      //only create array if actually doing rc
	double[][] cacheMPEc;  //orig MPE will use [0][...], others use [numVars][indx]
	int[][] cacheMPEArr;   //only create array if actually doing mpe

	int lastIndex = -1;
	int numInstVars = -1;


	/** @param context Must be ordered such that cutset vars are in order from root to this
	 *   node for the MPE3 to work.
	 */
	public RCNodeCache( RCNode nd, ArrayList context, double cf){
		nodeWhereCaching = nd;
		instantiation = nodeWhereCaching.rc.instantiation;

//	//would want this for non-discrete caching only
//		//randomize contextVars so cache is over random variables & values (which are already random from RC)
//		ArrayList cntx = new ArrayList( context);
//		Collections.shuffle( cntx);
		FiniteVariable[] contextVars = (FiniteVariable[])context.toArray( new FiniteVariable[context.size()]);

		contextIndices = new int[contextVars.length];
		contextSizes = new int[contextVars.length];

		long tmpSize = 1;

		if( DEBUG_CACHE_SIZE) { Definitions.STREAM_VERBOSE.println("num context vars: " + contextVars.length);}
		for( int i=0; i<contextVars.length; i++) {
			contextIndices[i] = nodeWhereCaching.rc.vars.indexOf( contextVars[i]);
			contextSizes[i] = (int)tmpSize; //this could overflow, but code below will "correct" it by zeroing out the totalSize
			if( DEBUG_CACHE_SIZE) { Definitions.STREAM_VERBOSE.print( i + ": " + tmpSize + " * " + contextVars[i].size());}
			tmpSize *= contextVars[i].size();
			if( DEBUG_CACHE_SIZE) { Definitions.STREAM_VERBOSE.println( " = " + tmpSize + "  MB: " + (tmpSize * MB));}
		}

		if( tmpSize < 0) {
			System.err.println("RCCache could not cache all " + tmpSize + " values, as it overflowed long.");
			totalSize = 0;
			cf = 0.0;
			System.err.println("contextInstantiations: " + nodeWhereCaching.contextInstantiations + "\n" + nodeWhereCaching);
			thisCacheOverFlowed = true;
		}
		else if( tmpSize > LARGEST_ACCEPTABLE_CACHE) {
			System.err.println("RCCache could not cache all " + tmpSize + " values, as it was larger than " + LARGEST_ACCEPTABLE_CACHE + ".");
			totalSize = 0;
			cf = 0.0;
			System.err.println("contextInstantiations: " + nodeWhereCaching.contextInstantiations + "\n" + nodeWhereCaching);
			thisCacheOverFlowed = true;
		}
		else if( tmpSize > Integer.MAX_VALUE) { //TODO: current code limit since caches are single dimension arrays
			System.err.println("RCCache could not cache all " + tmpSize + " values, as it overflowed int.");
			totalSize = 0;
			cf = 0.0;
			System.err.println("contextInstantiations: " + nodeWhereCaching.contextInstantiations + "\n" + nodeWhereCaching);
			thisCacheOverFlowed = true;
		}
		else {
			totalSize = (int)tmpSize;
			thisCacheOverFlowed = false;
		}

		changeCacheFactor( cf);
	}


	/** This does not guarantee a clean cache after you finish if the cf did not change from its previous cf.*/
	void changeCacheFactor( double cf) {
		if( cf != 0.0 && cf != 1.0) {
			throw new IllegalArgumentException("cache factor must be either 0 or 1 (" + cf + ")");
		}

		if(( cf == 1.0 && !cached) || ( cf == 0.0 && cached)) {
			clearCache();
			cached = (cf == 1.0);
		}
	}


	double cf() { if( cached) { return 1.0;} else { return 0.0;}}
	int cacheEntries_used() { if( cached) { return totalSize;} else { return 0;}}
	int cacheEntries_total() { return totalSize;}
	int cacheEntriesMpe_used() {
		if( cached) { return cacheEntriesMpe_total();}
		else { return 0;}
	}
	int cacheEntriesMpe_total() {
		if( nodeWhereCaching.vars_to_save_mpe != null) {
			return totalSize * nodeWhereCaching.vars_to_save_mpe.length;
		}
		else { return 0;} //no mpe cache
	}



	/**If returned value is >= 0.0 then it is the actual value.  Otherwise it could be
	 *  DoNotCache or ShouldBeCached.
	 */
	final double lookupRC() {
		numInstVars = -1;
		if( !cached) { lastIndex = -1; return DoNotCache;}

		lastIndex = 0;
		int length = contextIndices.length;
		for( int i=0; i<length; i++) {
			lastIndex += ( instantiation[contextIndices[i]] * contextSizes[i]);
		}

		if( cacheRC == null) { allocRCCache(); return ShouldBeCached;}
		else { return cacheRC[lastIndex];}
	}
	/**If returned value is >= 0.0 then it is the actual value.  Otherwise it could be
	 *  DoNotCache or ShouldBeCached.
	 */
	final double lookupMPE1() {
		numInstVars = -1;
		if( !cached) { lastIndex = -1; return DoNotCache;}

		lastIndex = 0;
		int length = contextIndices.length;
		for( int i=0; i<length; i++) {
			lastIndex += ( instantiation[contextIndices[i]] * contextSizes[i]);
		}

		if( cacheMPEc == null) { allocRCMPECache(); return ShouldBeCached;}
		else { return cacheMPEc[0][lastIndex];}
	}


	/**If returned value is [0.0 1.0] then it is the actual value.
	 *  Otherwise it could be DoNotCache or ShouldBeCached.
	 */
	final double lookupMPE3_actual() {
		if( !cached) { return DoNotCache;}

		if( numInstVars == -1) {
			lastIndex = 0;
			numInstVars = 0;
		}

		int length = contextIndices.length;
		while( numInstVars < length) {
			int inst = instantiation[contextIndices[numInstVars]];
			if( inst >= 0) {
				lastIndex += ( inst * contextSizes[numInstVars]);
				numInstVars++;
			}
			else {
				break;
			}
		}

		if( cacheMPEc == null) { allocRCMPE3Cache(); return ShouldBeCached;}
		else {
			double ret = cacheMPEc[numInstVars][lastIndex];
			if( ret < 0 && ret >= -1.0) { //then we have a bound, not an actual value.  Tell user to calculate it.
				ret = ShouldBeCached;
			}
			else if( ret >= 0 && ret <= 1) { //if have actual value, clear indexing
				lastIndex = -1;				 //  otherwise addToCache will do it or user must
				numInstVars = -1;
			}
			return ret;
		}
	}
	/**If returned value is [0.0 1.0] then it is a bound or actual value.
	 *  Otherwise it could be DoNotCache or ShouldBeCached.
	 */
	final double lookupMPE3_bound() {
		if( !cached) { return DoNotCache;}

		if( numInstVars == -1) {
			lastIndex = 0;
			numInstVars = 0;
		}

		int length = contextIndices.length;
		while( numInstVars < length) {
			int inst = instantiation[contextIndices[numInstVars]];
			if( inst >= 0) {
				lastIndex += ( inst * contextSizes[numInstVars]);
				numInstVars++;
			}
			else {
				break;
			}
		}

		if( cacheMPEc == null) { allocRCMPE3Cache(); return ShouldBeCached;}
		else {
			double ret = cacheMPEc[numInstVars][lastIndex];
			if( ret >= -1.0 && ret < 0.0) { //if it is a bound, return the bound
				ret = -ret;
			}
			return ret;
		}
	}



	/**If returned value is >= 0.0 then it is the actual value.
	 *  Otherwise it could be DoNotCache or ShouldBeCached.
	 */
	final double lookupMPE4_actual() {
		if( !cached) { return DoNotCache;}

		if( numInstVars == -1) {
			lastIndex = 0;
			numInstVars = 0;
		}

		int length = contextIndices.length;
		while( numInstVars < length) {
			int inst = instantiation[contextIndices[numInstVars]];
			lastIndex += ( inst * contextSizes[numInstVars]);
			numInstVars++;
//			if( inst < 0) { throw new IllegalStateException("");}
		}


		if( cacheMPEc == null) { allocRCMPECache(); return ShouldBeCached;}
		else {
			double ret = cacheMPEc[0][lastIndex];
			if( ret < 0 && ret >= -1) { //then have a bound, not an actual value
				ret = ShouldBeCached;
			}
			else if( ret >= 0 && ret <= 1) { //have actual value
				lastIndex = -1;
				numInstVars = -1;
			}
			return ret;
		}
	}
	/**If returned value is >= 0.0 then it is a bound or the actual value.
	 *  Otherwise it could be DoNotCache or ShouldBeCached.
	 */
	final double lookupMPE4_bound() {
		if( !cached) { return DoNotCache;}

		if( numInstVars == -1) {
			lastIndex = 0;
			numInstVars = 0;
		}

		int length = contextIndices.length;
		while( numInstVars < length) {
			int inst = instantiation[contextIndices[numInstVars]];
			lastIndex += ( inst * contextSizes[numInstVars]);
			numInstVars++;
//			if( inst < 0) { throw new IllegalStateException("");}
		}

		if( cacheMPEc == null) { allocRCMPE3Cache(); return ShouldBeCached;}
		else {
			double ret = cacheMPEc[0][lastIndex];
			if( ret >= -1.0 && ret < 0.0) { //if it is a bound, return the bound
				ret = -ret;
			}
			return ret;
		}
	}




	/**Will return the MPE of vars from the last call to lookup
	 * (should only be used if the return value was already cached).
	 */
	final int[] lookupMPE1Arr() {
		//numInstVars is not used by the original MPE
//		if( numInstVars != -1) { System.err.println("Err lookupMPEArr"); return null;}
		if( cacheMPEArr == null) {
			cacheMPEArr = new int[totalSize][];

			for( int i=0; i<cacheMPEArr.length; i++) {
				cacheMPEArr[i] = new int[nodeWhereCaching.vars_to_save_mpe.length];
				Arrays.fill( cacheMPEArr[i], -1);
			}
		}
		return cacheMPEArr[lastIndex];
	}


	final void allocRCCache() {
		if( cacheRC == null && cached) {
			cacheRC = new double[totalSize];
			Arrays.fill( cacheRC, ShouldBeCached);
		}
	}

	final void allocRCMPECache() {
		if( nodeWhereCaching.vars_to_save_mpe == null) {
			throw new IllegalStateException("Node was created without MPE information");
		}
		if( cacheMPEc == null && cached) {
			cacheMPEc = new double[1][];
			cacheMPEc[0] = new double[totalSize];
			Arrays.fill( cacheMPEc[0], ShouldBeCached);
		}
	}

	final void allocRCMPE3Cache() {
		if( nodeWhereCaching.vars_to_save_mpe == null && !nodeWhereCaching.isLeaf()) {
			throw new IllegalStateException("Node was created without MPE information");
		}
		if( cacheMPEc == null && cached) {

			cacheMPEc = new double[contextIndices.length+1][];

			int tmpSize = 1;
			for( int i=0; i<cacheMPEc.length; i++) {
				cacheMPEc[i] = new double[tmpSize];
				Arrays.fill( cacheMPEc[i], ShouldBeCached);
				if( i < contextIndices.length) {
					tmpSize *= ((FiniteVariable)nodeWhereCaching.rc.vars.get( contextIndices[i])).size();
				}
			}
		}
	}

	/**Will add res to the cache as the last value which called lookup
	 * (should only be used if the return value was ShouldBeCached, otherwise it will
	 * throw an array error).
	 */
	final void addToCacheRC( double res) {
		cacheRC[lastIndex] = res;
		numInstVars = -1;
		lastIndex = -1;
	}

	/**Will add res and resVars to the cache as the last value which called lookup
	 * (should only be used if the return value was ShouldBeCached, otherwise it will
	 * throw an array error).
	 */
	final void addToCacheMPE1( double res, int[] inst) {
		cacheMPEc[0][lastIndex] = res;
		cacheMPEArr[lastIndex] = inst;
		numInstVars = -1;
		lastIndex = -1;
	}


	/**Will add res and resVars to the cache as the last value which called lookup
	 * (should only be used if the return value was ShouldBeCached, otherwise it will
	 * throw an array error).
	 */
	final void addToCacheMPE4( double res) {
		cacheMPEc[0][lastIndex] = res;
		numInstVars = -1;
		lastIndex = -1;
	}

	/**Will add res to the cache as the last value which called lookup
	 * (should only be used if the return value was ShouldBeCached, otherwise it will
	 * throw an array error).  Does not try to propagate up tree, as MPE4 doesn't use
	 * partial context caching.
	 */
	final void addToCacheMPE4Bound( double res) {

		double oldReal = cacheMPEc[0][lastIndex];
		if( oldReal >= -1.0 && oldReal < 0.0) { oldReal = -oldReal;}

		if( oldReal < 0.0 || res < oldReal) {

			if( res == 0) {
				cacheMPEc[0][lastIndex] = 0;
			}
			else {
				cacheMPEc[0][lastIndex] = -res;
			}
		}
		lastIndex = -1;
		numInstVars = -1;
	}


	/**Will add res to the cache as the last value which called lookup
	 * (should only be used if the return value was ShouldBeCached, otherwise it will
	 * throw an array error).
	 */
	final void addToCacheMPE3Bound( double res) {

		double oldReal = cacheMPEc[numInstVars][lastIndex];
		if( oldReal >= -1.0 && oldReal < 0.0) { oldReal = -oldReal;}

		if( oldReal < 0.0 || res < oldReal) {

			if( res == 0) {
				cacheMPEc[numInstVars][lastIndex] = 0;
			}
			else {
				cacheMPEc[numInstVars][lastIndex] = -res;
			}
			propagateUpTree( lastIndex, numInstVars);
		}
		lastIndex = -1;
		numInstVars = -1;
	}



	/**Will add res to the cache as the last value which called lookup
	 * (should only be used if the return value was ShouldBeCached, otherwise it will
	 * throw an array error).
	 */
	final void addToCacheMPE3( double res) {
		cacheMPEc[numInstVars][lastIndex] = res;
		propagateUpTree( lastIndex, numInstVars);
		lastIndex = -1;
		numInstVars = -1;
	}



	final void propagateUpTree( int lastI, int numIV) {
if( !RC.TODO_REMOVE_DO_PROP_UP) { return;}
		if( numIV > 0) {

			int branchFactor1 = contextSizes[numIV-1];
			int prevIndx = lastI % branchFactor1;

			double newValue = cacheMPEc[numIV][lastI];
			double parentOldValue = cacheMPEc[numIV-1][prevIndx];

			if( newValue != parentOldValue) {  		//it could be equal to it, in which case it it still the max

				// see if it is the max & pass up the tree
				double maxReal = -1;
				double max = -1;

				for( int ci = prevIndx; ci < cacheMPEc[numIV].length; ci+=branchFactor1) {
					double tmpReal = cacheMPEc[numIV][ci];
					if( tmpReal >= -1.0 && tmpReal < 0.0) { tmpReal = -tmpReal;}
					else if( tmpReal < 0) { //something is unknown, don't pass up
						maxReal = -5; max = -5;
						break;
					}
					if( tmpReal > maxReal) { //find maxReal (could be an old value if just replaced a ShouldBeCached)
						maxReal = tmpReal;
						max = cacheMPEc[numIV][ci];
					}
				}
				double parentOldValueReal = (parentOldValue >= -1.0 && parentOldValue < 0) ? -parentOldValue : parentOldValue;
				if( max >= 0.0 && max != parentOldValue && ((maxReal <= parentOldValueReal)||(parentOldValueReal < 0))) {
					cacheMPEc[numIV-1][prevIndx] = max;
					propagateUpTree( prevIndx, numIV-1);
				}
			}
		}
	}



	final void printCacheMPEc() {
		for( int i=0; i<cacheMPEc.length; i++) {
			System.err.print(i + ": [");
			for( int j=0; j<cacheMPEc[i].length; j++) {
				System.err.print("[" + cacheMPEc[i][j] + "]");
			}
			System.err.println("]");
		}
	}


	final void clearCache() {
		cacheRC = null;     //delete memory it allocated
		cacheMPEc = null;   //delete memory it allocated
		cacheMPEArr = null; //delete memory it allocated
		lastIndex = -1;     //disallow addToCache
		numInstVars = -1;
	}



	List getContext( List ret) {
		if( ret == null) { ret = new ArrayList();}

		for( int i=0; i<contextIndices.length; i++) {
			ret.add(nodeWhereCaching.rc.vars.get(contextIndices[i]));
		}
		return ret;
	}

}//end class RCNodeCache
