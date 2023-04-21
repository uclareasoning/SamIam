package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;



public class CachingGreedy extends CachingScheme {


	static private boolean DEBUG_Greedy = false;


	Set availableCacheNodes = null;
	Set cachedNodes = null;


    public CachingGreedy( ) {
        this( 1.0); //full caching is default
    }

    public CachingGreedy( double cacheFactor) {
        super( cacheFactor);
    }

    public String toString() {
        StringBuffer ret = new StringBuffer("CachingGreedy");
        return ret.toString();
    }


	public void allocateMemory( RC rc, RCCreateListener listnr) {
        if( DEBUG_Greedy) { Definitions.STREAM_VERBOSE.println("\n\nBegin CachingGreedy.allocateMemoryGreedy " + cacheFactor);}

        long maxAcceptableCache;
		{
        	CachingDFBnB.DFBnB_TmpVars tmpVars = new CachingDFBnB.DFBnB_TmpVars( rc, CachingDFBnB.OrderingAlgo.defaultVal());
			maxAcceptableCache = (long)Math.floor( cacheFactor * tmpVars.sg().numCachingFullMinusWorthless());
		}
        if( DEBUG_Greedy){ Definitions.STREAM_VERBOSE.println("maxAcceptableCache: " + maxAcceptableCache); }

		allocateMemory(rc, maxAcceptableCache);
	}

	/**This function ignores the cache factor, and uses the integer cachesRequested as the maximum Acceptable number of caches.*/
	public void allocateMemory( RC rc, long cachesRequested) {
        if( DEBUG_Greedy) { Definitions.STREAM_VERBOSE.println("\n\nBegin CachingGreedy.allocateMemoryGreedy " + cacheFactor);}

		availableCacheNodes = new HashSet();
		{
			RCIterator itr = rc.getIterator();  //Iterates over entire tree or graph (so doing rc on graph not memory efficient)
			while( itr.hasNext()) {
				availableCacheNodes.add( itr.nextNode());
			}
		}

		long availableSpace = cachesRequested;
		cachedNodes = new HashSet( availableCacheNodes.size());
		Set candidates = new HashSet( availableCacheNodes);
		Map calls = new HashMap( availableCacheNodes.size());
		Map cpc = new HashMap( availableCacheNodes.size());
		Map scores = new HashMap( availableCacheNodes.size());

		while( availableSpace > 0 && candidates.size() > 0) {
			double bestScore = 0;
			RCNode bestNode = null;

			for(Iterator iter=candidates.iterator(); iter.hasNext();) {
				RCNode node = (RCNode)iter.next();
				if( availableSpace < node.contextInstantiations) {
					iter.remove();
					continue;
				}
				double score = getScore( scores, cpc, calls, node);
				if( score==0) {
					iter.remove();
				}
				else if( score < 0) {
					throw new IllegalStateException(); //TODO
//					System.err.println("negative score");
//					score = Double.POSITIVE_INFINITY;
				}
				else if( score > bestScore) {
					bestScore = score;
					bestNode = node;
				}
			}
			if( bestNode != null) {
				cachedNodes.add( bestNode);
				candidates.remove( bestNode);
				invalidateScores( scores, calls, cpc, bestNode);
				availableSpace -= bestNode.contextInstantiations;
		        if( DEBUG_Greedy) { Definitions.STREAM_VERBOSE.println("BestScore = " + bestScore);}
			}
		}


		setCacheFactorsAllNodes( cachedNodes, availableCacheNodes);
		availableCacheNodes = null;
		cachedNodes = null;

        if( DEBUG_Greedy) {
			Definitions.STREAM_VERBOSE.println("Expected Calls_all = " + rc.statsAll().expectedNumberOfRCCalls());
			double tmem = rc.statsAll().numCacheEntries();
			Definitions.STREAM_VERBOSE.println("Expected Cache Entries = " + tmem);
			Definitions.STREAM_VERBOSE.println("Expected CacheMB = " + (tmem * 8.0 / 1048576.0));
		}


        if( DEBUG_Greedy) { Definitions.STREAM_VERBOSE.println("End allocateMemoryGreedy");}

    }//end allocateMemoryDFBnB





	static private void setCacheFactorsAllNodes( Set cachedNodes, Set allNodes) {
		for( Iterator iter=allNodes.iterator(); iter.hasNext();) {
			RCNode node = (RCNode)iter.next();
			if( cachedNodes.contains( node)) {
				node.changeCacheFactor( 1.0);
			}
			else if( !node.isLeaf()){
				node.changeCacheFactor( 0.0);
			}
		}
	}




	private double getScore( Map scores, Map cpc, Map calls, RCNode node) {
		Object result = scores.get( node);
		if( result != null) {
			return ((Double)result).doubleValue();
		}
		else {
			double score = computeLocalScore( cpc, calls, node);
			scores.put( node, new Double( score));
			return score;
		}
	}
	private double computeLocalScore( Map cpc, Map calls, RCNode node) {
		if( node.isLeaf()) {
			return 0;
		}
		RCNode[] c = ((RCNodeInternalBinaryCache)node).childrenArr();

		double cls   = getCalls( calls, node);
		double cntx  = node.contextInstantiations;
		double cut   = node.cutsetInstantiations;

		double ret = cut * ( cls / cntx - 1);
		double ret2 = 0;
		for( int i=0; i<c.length; i++) {
			ret2 += getCPC( cpc, c[i]);
		}
		return ret * ret2;
	}


	private void invalidateScores( Map scores, Map calls, Map cpc, RCNode bestNode) {
		Set descendants = descendants( bestNode);
		calls.keySet().removeAll( descendants);
		scores.keySet().removeAll( descendants);
		Set ancestors = ancestors( bestNode);
		cpc.keySet().removeAll( ancestors);
		scores.keySet().removeAll( ancestors);
	}


	/** Returns the descendants of the node (self excluded).*/
	private Set descendants( RCNode node) {
		Set result = new HashSet();
		descendants( result, node);
		return result;
	}
	private void descendants( Set s, RCNode node) {
		if( node.isLeaf()) {
			return;
		}
		else {
			RCIterator iter = ((RCNodeInternalBinaryCache)node).childIterator();
			while( iter.hasNext()) {
				RCNode chi = iter.nextNode();
				s.add( chi);
				descendants( s, chi);
			}
		}
	}

	/** Returns the ancestors of the node (self included).*/
	private Set ancestors( RCNode node) {
		Set result = new HashSet();
		ancestors( result, node);
		return result;
	}
	private void ancestors( Set s, RCNode node) {
		s.add( node);
		for( Iterator iter = node.parentNodes.iterator(); iter.hasNext();) {
			RCNode par = (RCNode)iter.next();
			ancestors( s, par);
		}

	}


	private double getCalls(Map calls, RCNode t) {
		if( !availableCacheNodes.contains(t)) {
			System.err.println("node not in availableCacheNodes"); //TODO
			return 0;
		}
		Object obj = calls.get(t);
		if( obj != null) {
			return ((Double)obj).doubleValue();
		}
		else {
			double c = computeLocalCalls( calls, t);
			calls.put( t, new Double(c));
			return c;
		}
	}
	/**Returns the number of times that rc is called on the dtree node "node".*/
	private double computeLocalCalls( Map calls, RCNode node) {
		if( node.isRoot) {
			return node.contextInstantiations;
		}
		else {
			double ret = 0;
			for( Iterator iter = node.parentNodes.iterator(); iter.hasNext();) {
				RCNode p1 = (RCNode)iter.next();
				ret += parentCallContribution( calls, p1);
			}
			return ret;
		}
	}

	private double parentCallContribution( Map calls, RCNode parent) {
		if( cachedNodes.contains( parent)) {
			return parent.cutsetInstantiations * parent.contextInstantiations;
		}
		else {
			return getCalls(calls, parent) * parent.cutsetInstantiations;
		}
	}

	private double getCPC( Map cpc, RCNode node) {
		Object obj = cpc.get(node);
		if( obj != null) {
			return ((Double)obj).doubleValue();
		}
		else {
			double c = computeLocalCPC( cpc, node);
			cpc.put( node, new Double(c));
			return c;
		}
	}

	/** Returns the number of recursive calls generated by a call to the node
	 *  including the call itself.
	 */
	private double computeLocalCPC( Map cpc, RCNode node) {
		if( cachedNodes.contains( node)) {
			return 1;
		}
		else if( node.isLeaf()){
			return 1;
		}
		else {
			double ret2 = 0;
			RCIterator iter = ((RCNodeInternalBinaryCache)node).childIterator();
			while( iter.hasNext()) {
				ret2 += getCPC( cpc, iter.nextNode());
			}
			return 1 + node.cutsetInstantiations * ret2;
		}
	}
}





