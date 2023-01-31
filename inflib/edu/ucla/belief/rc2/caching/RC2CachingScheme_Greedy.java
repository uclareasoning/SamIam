package edu.ucla.belief.rc2.caching;

import java.util.*;

import edu.ucla.belief.rc2.structure.*;



/** This class generates greedy caching schemes for RC2 Objects.
 */
final public class RC2CachingScheme_Greedy implements RC2.CachingScheme {

	final double cf; //use either this or cachesRequested
	final long cachesRequested; //use either this or cf

	HashSet cachedNodes = null;;

	public RC2CachingScheme_Greedy(double cf){
		this.cf=cf;
		cachesRequested=0;
		if(cf<0 || cf>1) { throw new IllegalArgumentException("Cache factor " + cf + " was not between 0 and 1");}
	}
	public RC2CachingScheme_Greedy(long cachesRequested){
		this.cachesRequested=cachesRequested;
		cf=0.0;
	}

	public String toString() {return "Greedy Caching (" + (cf!=0 ? cf : cachesRequested) + ")";}

	public Collection getCachingScheme(RC2 rc) {

		Collection availableCacheNodes = new RC2CachingScheme_Full().getCachingScheme(rc);

		long memoryRemaining;
		if(cachesRequested==0) {
			memoryRemaining = RC2CachingSchemeUtils.expectedMemoryUsage(availableCacheNodes);
			memoryRemaining = (long)Math.floor(cf * memoryRemaining);
		}
		else {
			memoryRemaining = cachesRequested;
		}

		rc.outputInfo("Greedy Caching max cache: " + memoryRemaining);

		cachedNodes = new HashSet(availableCacheNodes.size());
		Map calls = new HashMap(availableCacheNodes.size());
		Map cpc = new HashMap(availableCacheNodes.size());
		Map scores = new HashMap(availableCacheNodes.size());


		while(memoryRemaining>0 && !availableCacheNodes.isEmpty()) {
			double bestScore = 0;
			RC2Node bestNode = null;

			for(Iterator iter=availableCacheNodes.iterator(); iter.hasNext();) {
				RC2Node node = (RC2Node)iter.next();
				if(memoryRemaining < node.context().memoryUsage()) {
					iter.remove();
					continue;
				}
				double score = getScore(scores, cpc, calls, node);
				if(score==0) {
					iter.remove();
				}
				else if(score < 0) {
					throw new IllegalStateException();
				}
				else if(score > bestScore) {
					bestScore = score;
					bestNode = node;
				}
			}
			if(bestNode != null) {
				cachedNodes.add(bestNode);
				availableCacheNodes.remove(bestNode);
				invalidateScores(scores, calls, cpc, bestNode);
				memoryRemaining -= bestNode.context().memoryUsage();
			}
		}

		HashSet ret = cachedNodes;
		cachedNodes = null;
		return ret;
	}


	private double getScore(Map scores, Map cpc, Map calls, RC2Node node) {
		Object result = scores.get(node);
		if(result!=null) {
			return ((Double)result).doubleValue();
		}
		else {
			double score = computeLocalScore(cpc, calls, node);
			scores.put(node, new Double(score));
			return score;
		}
	}

	private double computeLocalScore(Map cpc, Map calls, RC2Node node_in) {
		if(node_in.isLeaf()) {
			return 0;
		}
		RC2NodeInternal node = (RC2NodeInternal)node_in;

		double cls = getCalls(calls, node);
		double cntx = node.context().totalStateSpace().doubleValue();
		double cut = node.numCutsetInstantiations();

		double ret = cut * (cls / cntx -1);
		double ret2 = getCPC(cpc, node.left) + getCPC(cpc, node.right);

		return ret * ret2;
	}

	private void invalidateScores(Map scores, Map calls, Map cpc, RC2Node node) {
		Set descendants = descendants(node);
		calls.keySet().removeAll(descendants);
		scores.keySet().removeAll(descendants);
		Set ancestors = ancestors(node);
		cpc.keySet().removeAll(ancestors);
		scores.keySet().removeAll(ancestors);
	}

	/*self excluded.*/
	private Set descendants(RC2Node node) {
		Set result = new HashSet();
		descendants(result, node);
		return result;
	}
	private void descendants(Set s, RC2Node node_in) {
		if(node_in.isLeaf()) {
			return;
		}
		else {
			RC2NodeInternal node = (RC2NodeInternal)node_in;
			s.add(node.left);
			descendants(s, node.left);
			s.add(node.right);
			descendants(s, node.right);
		}
	}

	/*self included.*/
	private Set ancestors(RC2Node node) {
		Set result = new HashSet();
		ancestors(result, node);
		return result;
	}
	private void ancestors(Set s, RC2Node node) {
		s.add(node);
		for(Iterator iter = node.parentNodes().iterator(); iter.hasNext();) {
			RC2Node par = (RC2Node)iter.next();
			ancestors(s, par);
		}
	}

	private double getCalls(Map calls, RC2Node t) {
		Object obj = calls.get(t);
		if(obj!=null) {
			return ((Double)obj).doubleValue();
		}
		else {
			double c = computeLocalCalls(calls, t);
			calls.put(t, new Double(c));
			return c;
		}
	}
	/**Returns the number of times that rc is called on the dtree node "node".*/
	private double computeLocalCalls(Map calls, RC2Node node) {
		if(node.isRoot()) {
			return node.context().totalStateSpace().doubleValue();
		}
		else {
			double ret = 0;
			for(Iterator iter = node.parentNodes().iterator(); iter.hasNext();) {
				RC2NodeInternal p1 = (RC2NodeInternal)iter.next();
				ret += parentCallContribution(calls, p1);
			}
			return ret;
		}
	}
	private double parentCallContribution(Map calls, RC2NodeInternal parent) {
		if(cachedNodes.contains(parent)) {
			return parent.numCutsetInstantiations() * parent.context().totalStateSpace().doubleValue();
		}
		else {
			return getCalls(calls, parent) * parent.numCutsetInstantiations();
		}
	}

	private double getCPC(Map cpc, RC2Node node) {
		Object obj = cpc.get(node);
		if(obj!=null) {
			return ((Double)obj).doubleValue();
		}
		else {
			double c = computeLocalCPC(cpc, node);
			cpc.put(node, new Double(c));
			return c;
		}
	}

	/** Returns the number of recursive calls generated by a call to the node
	 *  including the call itself.
	 */
	private double computeLocalCPC(Map cpc, RC2Node node_in) {
		if(cachedNodes.contains(node_in) || node_in.isLeaf()) {
			return 1;
		}
		else {
			RC2NodeInternal node = (RC2NodeInternal)node_in;
			double ret2 = getCPC(cpc, node.left) + getCPC(cpc, node.right);
			return 1 + node.numCutsetInstantiations() * ret2;
		}
	}

} //end class RC2CachingScheme_Greedy


