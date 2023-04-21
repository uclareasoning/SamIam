package edu.ucla.belief.rc2.caching;

import java.util.*;

import edu.ucla.belief.rc2.structure.*;


/** Utility functions for Caching Scheme objects.*/
public class RC2CachingSchemeUtils {

	private RC2CachingSchemeUtils(){}


	/** Remove nodes which have caches which are too large for a cache to be created.
	 */
	final public static void removeLargeCaches(RC2 rc, Collection cachedNodes) {
		if(cachedNodes.size()==0) { return;}

		int numNodes = rc.getNumRCNodes_All();
		for(int indx = 0; indx<numNodes; indx++) {
			RC2Node nd = rc.getRCNode_All(indx);
			if(nd.context().totalStateSpaceLargerThanInt()) {
				cachedNodes.remove(nd);
				rc.outputInfo("Node " + nd + " cannot be cached because of a large context.");
			}
		}
	}

	/** Will traverse all nodes in rc and attempt to remove any
	 *  worthless caches from cachedNodes (will recognize when a state
	 *  space is too large and won't be cached and will adjust usefulness
	 *  based on them).
	 */
	final public static void removeWorthlessCaches(RC2 rc, Collection cachedNodes) {
		if(cachedNodes.size()==0) { return;}

		int numNodes = rc.getNumRCNodes_All();
		for(int indx = 0; indx<numNodes; indx++) {
			RC2Node nd = rc.getRCNode_All(indx);
			if(!nd.isLeaf()) {
				RC2NodeInternal ndi = (RC2NodeInternal)nd;
				RC2Node l = ndi.left;
				RC2Node r = ndi.right;

				//if "parent" is too big to cache, don't make children worthless
				//any child of a big cache will be useful if it is not a big cache, since the
				//  parent has something in its context that this one doesn't.

				if(ndi.context().totalStateSpaceLargerThanInt()) {
					//in this case can't cache at nd and children are not worthless
					cachedNodes.remove(nd);
					rc.outputInfo("Node " + nd + " cannot be cached because of a large context.");
				}
				else {
					if(l.numParentNodes()==1 &&
						l.context().isSuperSetOf(ndi.context())) {
						cachedNodes.remove(l);
					}
					if(r.numParentNodes()==1 &&
						r.context().isSuperSetOf(ndi.context())) {
						cachedNodes.remove(r);
					}
					if(nd.isRoot()) { cachedNodes.remove(nd);}
				}
			}
			else {
				cachedNodes.remove(nd);
			}
		}
	}


	/** Will traverse the RC2Nodes in cachedNodes and count up the size of their
	 *  expected caches.
	 */
	 final public static long expectedMemoryUsage(Collection cachedNodes) {
		 long ret = 0;
		 for(Iterator itr = cachedNodes.iterator(); itr.hasNext();) {
			 ret += ((RC2Node)itr.next()).context().memoryUsage();
		 }
		 return ret;
	 }





} //end class RC2CachingSchemeUtils


