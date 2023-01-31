package edu.ucla.belief.rc2.structure;

import java.util.*;
import java.math.*;
import java.io.*;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.rc2.kb.KBMap;


/** This class contains utility functions for RC2 objects.
 *
 * @author David Allen
 */
public class RC2Utils {

	private RC2Utils() {} //don't allow creation of objects



	/** When finished, ret will include a mapping from all nodes to their parents in the
	 *  subtree rooted at rt.
	 */
	final static public void computeParentsInTree(RC2Node rt, Map ret) {
		if(ret == null) { ret = new HashMap();}

		if(rt.isLeaf()) {
		}
		else {
			RC2NodeInternal nd = (RC2NodeInternal)rt;
			ret.put(nd.left, rt);
			ret.put(nd.right, rt);
			computeParentsInTree(nd.left, ret);
			computeParentsInTree(nd.right, ret);
		}
	}

	/** Compute leaf nodes in the subtree rooted at rt (will include rt if it is a leaf node).*/
	final static public void computeLeafs(RC2Node rt, Collection ret) {
		if(ret == null) { ret = new HashSet();}

		if(rt.isLeaf()) {
			ret.add(rt);
		}
		else {
			RC2NodeInternal nd = (RC2NodeInternal)rt;
			computeLeafs(nd.left, ret);
			computeLeafs(nd.right, ret);
		}
	}


	/** Add rt and all descendants to ret (stops descending when any node is already
	 *  in ret.
	 */
	final static public Collection addAllNodes(RC2Node rt, Collection ret) {
		if(ret==null) { ret = new HashSet();}

		if(!ret.contains(rt)) {
			ret.add(rt);
			if(!rt.isLeaf()) {
				RC2NodeInternal nd = (RC2NodeInternal)rt;
				addAllNodes(nd.left, ret);
				addAllNodes(nd.right, ret);
			}
		}
		return ret;
	}




	/**Calculates the cutset of a variable based only on its children.  To calculate
	 * a "local cutset", acutset can be Collections.EMPTY_SET.
	 */
	final static public Collection calculateCutsetFromChildren(RC2NodeInternal nd, Collection acutset) {
		Collection left = new HashSet(nd.left.vars);
		Collection right = nd.right.vars;

		left.retainAll(right); //intersect left and right
		left.removeAll(acutset); //remove acutset vars

		return left;
	}


//    /** The ArrayList ord will contain the vars in an
//     *   elimination order determined by the subtree rooted at rt (it is not unique).
//     *   This assumes that leaf nodes have their contxt set and internal nodes have
//     *   their iterators created.
//     */
//    final static public void getElimOrder(RCNode rt, ArrayList ord) {
//        if(ord == null) { ord = new ArrayList();}
//
//
//		if(rt.isLeaf()) {
//			RCNodeLeaf rtleaf = (RCNodeLeaf)rt;
//			Collection elim = rtleaf.vars();
//			elim.removeAll(rtleaf.context);
//			ord.addAll(elim);
//		}
//		else {
//			RCNodeInternalBinaryCache rtibc = (RCNodeInternalBinaryCache)rt;
//			getElimOrder(rtibc.left, ord);
//			getElimOrder(rtibc.right, ord);
//
//			//add own vars
//			ord.addAll(rtibc.itr.getVars(null));//add cutset variables
//		}
//	}
//
//
//	final static public int clusterSize(RCNode rt) {
//		int ret;
//		if(rt.isLeaf()) {
//			ret = rt.vars().size();
//		}
//		else {
//			RCNodeInternalBinaryCache rtibc = (RCNodeInternalBinaryCache)rt;
//
//			Collection cutset = rtibc.getCutset(new HashSet());
//			Collection context = rtibc.cache().getContext(null);
//			Collection cluster = new HashSet(cutset.size() + context.size());
//			cluster.addAll(cutset);
//			cluster.addAll(context);
//			ret = cluster.size();
//
//			ret = Math.max(ret, clusterSize(rtibc.left()));
//			ret = Math.max(ret, clusterSize(rtibc.right()));
//		}
//		return ret;
//	}


    /** Will return the state space of all the variables in the collection, and if the collection
     *  is empty it will return 1.
     */
    final static public double collOfFinVarsToStateSpace( Collection col) {
        double ret = 1;
        for( Iterator itr = col.iterator(); itr.hasNext();) {
            FiniteVariable fv = (FiniteVariable)itr.next();
            ret *= fv.size();

            if( fv.size() == 0) { throw new IllegalStateException("fv.size() == 0 " + fv);}
        }
        return ret;
    }

	/** Compute ln(a-b) from ln(a) and ln(b) using the log sub equation.
	  * Its possible that lna and/or lnb might be NaN, or pos/neg infin.
	  * If lna or lnb are NaN, the result is NaN.
	  * If lna=lnb=negInf, then (a=b=0 and ln(0)=negInf) return negInf.
	  * If lna=negInf or lnb=negInf return other one (possibly negated).
	  * If lna or lnb are posInf, then it is an error.
	  */
	final static public double logsub(double lna, double lnb) {
		double ret;

		//NaN will propagate by itself

		//Handle infinite values
		if(lna == Double.NEGATIVE_INFINITY && lnb == Double.NEGATIVE_INFINITY) { return Double.NEGATIVE_INFINITY;} //A=0 && B=0
		else if(lna == Double.NEGATIVE_INFINITY) { return -lnb;} //A=0
		else if(lnb == Double.NEGATIVE_INFINITY) { return lna;} //B=0
		else if(lna == Double.POSITIVE_INFINITY || lnb == Double.POSITIVE_INFINITY) { throw new IllegalStateException("Positive Infinity");}

		//use log sub equation
		// ln(a-b) = ln(a) + ln(1.0 - e ^ (ln(b)-ln(a)))
		ret = lna + Math.log(1.0 - Math.exp((lnb-lna)));

		//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is -lnB or lnA
		if(ret == Double.POSITIVE_INFINITY) { if(lna>lnb) {ret = lna;} else { ret = -lnb;}}

		//return result
		return ret;
	}

	/** Compute ln(a-b) from ln(a) and ln(b) using the log sub equation.
	  * Its possible that lna and/or lnb might be NaN, or pos/neg infin.
	  * If lna or lnb are NaN, the result is NaN.
	  * If lna=lnb=negInf, then (a=b=0 and ln(0)=negInf) return negInf.
	  * If lna=negInf or lnb=negInf return other one (possibly negated).
	  * If lna or lnb are posInf, then it is an error.
	  */
	final static public double logsub2(double lna, double lnb) {
		double ret;

		//NaN will propagate by itself

		//Handle infinite values
		if(lna == Double.NEGATIVE_INFINITY && lnb == Double.NEGATIVE_INFINITY) { return Double.NEGATIVE_INFINITY;} //A=0 && B=0
		else if(lna == Double.NEGATIVE_INFINITY) { return -lnb;} //A=0
		else if(lnb == Double.NEGATIVE_INFINITY) { return lna;} //B=0
		else if(lna == Double.POSITIVE_INFINITY || lnb == Double.POSITIVE_INFINITY) { throw new IllegalStateException("Positive Infinity");}

		//use log sub equation
		// ln(a-b) = ln(b) + ln(e ^ (ln(a)-ln(b)) - 1.0)
		ret = lnb + Math.log(Math.exp((lna-lnb)) - 1.0);

		//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is -lnB or lnA
		if(ret == Double.POSITIVE_INFINITY) { if(lna>lnb) {ret = lna;} else { ret = -lnb;}}

		//return result
		return ret;
	}

//	public final static class ArrayIteratorForward implements Iterator {
//		int nextNode;
//		final Object arr[];
//		public ArrayIteratorForward(Object arr[]) {
//			nextNode = 0;
//			this.arr = arr;
//		}
//		public boolean hasNext() { return nextNode < arr.length;}
//		public Object next() {
//			if(nextNode>=arr.length) { throw new NoSuchElementException();}
//			return arr[nextNode++];
//		}
//		public void remove() { throw new UnsupportedOperationException();}
//	}
//	public final static class ArrayIteratorBackward implements Iterator {
//		int nextNode;
//		final Object arr[];
//		public ArrayIteratorBackward(Object arr[]) {
//			nextNode = arr.length-1;
//			this.arr = arr;
//		}
//		public boolean hasNext() { return nextNode >= 0;}
//		public Object next() {
//			if(nextNode<0) { throw new NoSuchElementException();}
//			return arr[nextNode--];
//		}
//		public void remove() { throw new UnsupportedOperationException();}
//	}


	public final static Collection getSetOfAllNodes(RC2 rc) {
		int numNodes = rc.getNumRCNodes_All();
		HashSet cachedNodes = new HashSet(numNodes);

		//add all nodes
		for(int i=0; i<numNodes; i++) {
			RC2Node nd = rc.getRCNode_All(i);
			cachedNodes.add(nd);
		}
		return cachedNodes;
	}

	public final static Collection getSetOfAllNonLeafNodes(RC2 rc) {
		int numNodes = rc.getNumRCNodes_All();
		HashSet cachedNodes = new HashSet(numNodes);

		//find and add all nonLeaf nodes
		for(int i=0; i<numNodes; i++) {
			RC2Node nd = rc.getRCNode_All(i);
			if(!nd.isLeaf()) {
				cachedNodes.add(nd);
			}
		}
		return cachedNodes;
	}

	public final static HashSet getSetOfAllCachedNodes(RC2 rc) {
		HashSet ret = new HashSet(rc.compStats().currentCaching().numNodesCached);
		int numNodes = rc.getNumRCNodes_All();

		for(int i=0; i<numNodes; i++) {
			RC2Node nd = rc.getRCNode_All(i);
			if(!nd.isLeaf()) {
				RC2NodeInternal ndi = (RC2NodeInternal)nd;
				if(ndi.actualMemoryAllocated() > 0) {
					ret.add(ndi);
				}
			}
		}
		return ret;
	}

	/**If nodesCached is null, it will look to the nodes themselves to see if memory has been
	 *  allocated, however if nodesCached is non-null, it will ignore the RC object and determine
	 *  the number of calls based on nodesCached.  This allows for computations to be determined
	 *  even on Full Caching without ever allocating any memory.
	 */
	public final static double expectedRCCalls_All(RC2 rc, Collection nodesCached) {
		double ret = 0;
		int numNodes = rc.getNumRCNodes_All();
		Map callsToNode = new HashMap(numNodes);

		for(int indx = numNodes-1; indx>=0; indx--) { //ordering must be parent-child
			RC2Node nd = rc.getRCNode_All(indx);
			ret += expectedRCCalls(nd,callsToNode,nodesCached);
		}
		return ret;
	}
	/**If nodesCached is null, it will look to the nodes themselves to see if memory has been
	 *  allocated, however if nodesCached is non-null, it will ignore the RC object and determine
	 *  the number of calls based on nodesCached.  This allows for computations to be determined
	 *  even on Full Caching without ever allocating any memory.
	 */
	public final static double expectedRCCalls_Pe(RC2 rc, Collection nodesCached) {
		double ret = 0;
		int numNodes = rc.getNumRCNodes_Pe();
		Map callsToNode = new HashMap(numNodes);

		for(int indx = numNodes-1; indx>=0; indx--) { //ordering must be parent-child
			RC2Node nd = rc.getRCNode_Pe(indx);
			ret += expectedRCCalls(nd,callsToNode,nodesCached);
		}
		return ret;
	}
	private final static double expectedRCCalls(RC2Node nd, Map callsToNode, Collection nodesCached) {
		double ret = 0;

		if(nd.isRoot()) { //one call from caller
			callsToNode.put(nd, new Double(1));
		}
		if(!nd.isLeaf()) { //internal node (ancestors already done)
			RC2NodeInternal ndi = (RC2NodeInternal)nd;
			Double d = (Double)callsToNode.get(nd);
			if(d!=null) { //if making any calls to this node (should be)
				double callsHere = d.doubleValue();
				ret += callsHere;//add calls made to this node

				Double callsToChild;
				boolean isCached;
				if(nodesCached!=null) {
					isCached = nodesCached.contains(ndi);
				}
				else {
					isCached = (ndi.actualMemoryAllocated()!=0);
				}

				if(isCached) { //cached here
					callsToChild = new Double(ndi.context().totalStateSpace.doubleValue() * ndi.numCutsetInstantiations());
				}
				else {
					callsToChild = new Double(callsHere * ndi.numCutsetInstantiations());
				}

				//update callsToNode for children
				Double l = (Double)callsToNode.get(ndi.left);
				Double r = (Double)callsToNode.get(ndi.right);

				if(l==null) { callsToNode.put(ndi.left, callsToChild);}
				else { callsToNode.put(ndi.left, new Double(callsToChild.doubleValue() + l.doubleValue()));}

				if(r==null) { callsToNode.put(ndi.right, callsToChild);}
				else { callsToNode.put(ndi.right, new Double(callsToChild.doubleValue() + r.doubleValue()));}
			}
		}
		else {
			Double d = (Double)callsToNode.get(nd);
			ret += d.doubleValue();
		}
		return ret;
	}

	public final static void printRC2(PrintStream out, RC2 rc) {
		int numNodes = rc.getNumRCNodes_Pe();
		for(int indx = numNodes-1; indx>=0; indx--) {
			RC2Node nd = rc.getRCNode_Pe(indx);
			out.println(nd.toString());
		}
	}
	public final static void printRC2(PrintWriter out, RC2 rc) {
		int numNodes = rc.getNumRCNodes_Pe();
		for(int indx = numNodes-1; indx>=0; indx--) {
			RC2Node nd = rc.getRCNode_Pe(indx);
			out.println(nd.toString());
		}
	}

	public final static void writeDtreeToVCGFile(File outFile, RC2 rc)
	throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)),true);

		out.write("\ngraph: { title: \"rc\" \nlayoutalgorithm: tree\ntreefactor: 0.9\n");

		//Nodes
		for(int i=0; i<rc.getNumRCNodes_Pe(); i++) {
			RC2Node nd = (RC2Node)rc.getRCNode_Pe(i);
			out.write("node: { title: \"" + nd.nodeID + "\"" + "\n");
			if(nd.isLeaf()) {
				out.write("label: \"L "+ nd.nodeID +"\n" +
							"vars: " + nd.vars +
							"\"\n");
				out.write("shape: box}" + "\n");
			}
			else {
				out.write("label: \"I " + nd.nodeID + "\n" +
							"cutset: " + collOfFinVarsToStateSpace(((RC2NodeInternal)nd).getCutsetVars(null)) + ": " + ((RC2NodeInternal)nd).getCutsetVars(null) + "\n" +
							"context: " + collOfFinVarsToStateSpace(nd.context.vars) + ": " + nd.context.vars + "\n" +
							"\"\n");
				if(nd.getCaching()) {
	                out.write( "color: darkgreen \n");
				}
				out.write("shape: ellipse}" + "\n");
			}
		}

		//Edges
		for(int i=0; i<rc.getNumRCNodes_Pe(); i++) {
			RC2Node nd = (RC2Node)rc.getRCNode_Pe(i);
			if(!nd.isLeaf()) {
				RC2NodeInternal ndi = (RC2NodeInternal)nd;
				out.write( "edge: {" + "\n");
				out.write( "sourcename: \"" + ndi.nodeID + "\"" + "\n");
				out.write( "targetname: \"" + ndi.left.nodeID + "\"" + "\n");
				out.write( "}" + "\n");

				out.write( "edge: {" + "\n");
				out.write( "sourcename: \"" + ndi.nodeID + "\"" + "\n");
				out.write( "targetname: \"" + ndi.right.nodeID + "\"" + "\n");
				out.write( "}" + "\n");
			}
		}

		out.write("}\n");
		out.flush();
		out.close();
	}


	/** If there is anything that is wrong, this will throw an IllegalStateException.
	 */
	public static void confirmUnitResolutionRSAT(RC2 rc, BeliefNetwork bn) {
		//search for unit resolution learning
		List vars = rc.vars();
		KBMap.Mapping mapping = rc.map_kb;

		for(ListIterator itr = vars.listIterator(); itr.hasNext();) {
			FiniteVariable var = (FiniteVariable)itr.next();
			int indx = vars.indexOf(var);

			if(mapping.map[indx]!=null) { //if it has determinism

				if(var.size()==1) { throw new IllegalStateException("UnitResolutionError: " + var + " has determinism and only one state");}

				int numValidStates = 0;
				int validSt = -1;
				int possSt = -1;

				for(int s=0; s<var.size(); s++) {

					int vst = rc.kb_sat.varStatus(mapping.map[indx][s]);
					if(vst==1) {//known true
						if(validSt != -1 || numValidStates>0) {throw new IllegalStateException("UnitResolutionError: found a second true state (" + numValidStates + ") " + s + " and " + validSt + " " + possSt + " for " + var);}
						validSt = s;
						possSt = s;
						numValidStates++;
					}
					else if(vst==0) { //unknown but possible
						if(validSt != -1) { throw new IllegalStateException("UnitResolutionError: Already know " + var + " = " + validSt + ", but " + s + " is still possible");}
						numValidStates++;
						possSt = s;
					}
				}//for each state


				if(numValidStates==1) { //have a known variable
					if(validSt==-1) { throw new IllegalStateException("UnitResolutionError: known state was not known " + var);}
					int in = bn.inDegree(var);
					int out = bn.outDegree(var);
					if(out!=0) { throw new IllegalStateException("UnitResolutionError: known var was not leaf " + var);}
					if(in==0) { throw new IllegalStateException("UnitResolutionError: known var has no parents (independent) " + var);}
				}
				else if(numValidStates<=0) {
					throw new IllegalStateException("UnitResolutionError: no valid states for " + var);
				}
				else { //has more than one valid state (i.e. is unknown)
					if(numValidStates != var.size()) { throw new IllegalStateException("UnitResolutionError: valid states: " + numValidStates + " != " + var.size());}
				}
			}//if has determinism
		}//for each var
	}


	/** If there is anything that is wrong, this will throw an IllegalStateException.
	 */
	public static void confirmUnitResolutionKB(RC2 rc, BeliefNetwork bn) {
		//search for unit resolution learning
		rc.prePrepareToStartComputation(); //force it to create KB if it has not already done so.

		List vars = rc.vars();

		for(ListIterator itr = vars.listIterator(); itr.hasNext();) {
			FiniteVariable var = (FiniteVariable)itr.next();

			int vst = rc.rcKB.numPossibleStates(var); //returns number of possible states
			if(vst==1) { //have a known variable
				if(var.size()==1) { throw new IllegalStateException("UnitResolutionError: " + var + " has determinism and only one state");}
				int in = bn.inDegree(var);
				int out = bn.outDegree(var);
				if(out!=0) { throw new IllegalStateException("UnitResolutionError: known var was not leaf " + var);}
				if(in==0) { throw new IllegalStateException("UnitResolutionError: known var has no parents (independent) " + var);}
			}
			else if(vst==-1) { //not in KB
			}
			else { //unknown
				if(var.size()==1) { throw new IllegalStateException("UnitResolutionError: " + var + " has determinism and only one state");}
				if(vst != var.size()) {
					throw new IllegalStateException("UnitResolutionError: valid states: " + vst + " != " + var.size());
				}
			}
		}//for each var
	}




	public static class MiscStats {
		static final double log_e_2 = Math.log(2);


		int maxContextVars = 0;
		BigInteger maxContextTSS = BigInteger.ZERO;

		int maxCutsetVars = 0;
		long maxCutsetTSS = 0;

		int maxClusterVars = 0;
		BigInteger maxClusterTSS = BigInteger.ZERO;
		BigInteger sumClusterTSS = BigInteger.ZERO;

		BigInteger rcCallsFullCaching = BigInteger.ONE; //call to root

		int maxDepth = 0;

		public String toString() {
			StringBuffer ret = new StringBuffer();
			ret.append("Misc Stats on RC object: \n");
			ret.append(" maxContextVars: " + maxContextVars + "\n");
			ret.append(" maxContextTSS: " + maxContextTSS + "  log_2(maxContextTSS): " + (Math.log(maxContextTSS.doubleValue()) / log_e_2) + "\n");

			ret.append(" maxCutsetVars: " + maxCutsetVars + "\n");
			ret.append(" maxCutsetTSS: " + maxCutsetTSS + "  log_2(maxCutsetTSS): " + (Math.log(maxCutsetTSS) / log_e_2) + "\n");

			ret.append(" maxClusterVars: " + maxClusterVars + "\n");
			ret.append(" maxClusterTSS: " + maxClusterTSS + "  log_2(maxClusterTSS): " + (Math.log(maxClusterTSS.doubleValue()) / log_e_2) + "\n");
			ret.append(" sumClusterTSS: " + sumClusterTSS + "  log_2(sumClusterTSS): " + (Math.log(sumClusterTSS.doubleValue()) / log_e_2) + "\n");

			ret.append(" rcCallsFullCaching: " + rcCallsFullCaching + "\n");

			ret.append(" maxDepth: " + maxDepth + " (root is depth=0)");
			return ret.toString();
		}

	}

	public static MiscStats computeStatsDtree(RC2 rc) {
		MiscStats ret = new MiscStats();
		computeStats(rc.getPeRootNode(), ret, 0);
		return ret;
	}

	private static void computeStats(RC2Node rcN, MiscStats stat, int depth) {
		if(depth > stat.maxDepth) stat.maxDepth = depth;

		if(rcN.isLeaf()) {
		}
		else {
			RC2NodeInternal rcNi = (RC2NodeInternal)rcN;

			//Context
				RC2Index cntx = rcNi.context();
				int cntxNumVars = cntx.vars.size();
				BigInteger cntxTSS = cntx.totalStateSpace();

				if(cntxNumVars > stat.maxContextVars) stat.maxContextVars = cntxNumVars;
				stat.maxContextTSS = stat.maxContextTSS.max(cntxTSS);

			//Cutset
				int cutNumVars = rcNi.getCutsetVars(null).size();
				long cutTSS = rcNi.numCutsetInstantiations();

				if(cutNumVars > stat.maxCutsetVars) stat.maxCutsetVars = cutNumVars;
				if(cutTSS > stat.maxCutsetTSS) stat.maxCutsetTSS = cutTSS;

			//Cluster
				int clusNumVars = cntxNumVars + cutNumVars;
				BigInteger clusTSS = cntxTSS.multiply(BigInteger.valueOf(cutTSS));

				if(clusNumVars > stat.maxClusterVars) stat.maxClusterVars = clusNumVars;
				stat.maxClusterTSS = stat.maxClusterTSS.max(clusTSS);
				stat.sumClusterTSS = stat.sumClusterTSS.add(clusTSS);

			//RC Calls
				stat.rcCallsFullCaching = stat.rcCallsFullCaching.add(clusTSS.multiply(BigInteger.valueOf(2)));


			computeStats(rcNi.left, stat, depth+1);
			computeStats(rcNi.right, stat, depth+1);
		}
	}



	public final static double expectedRCCalls_Pe_inclEvid(RC2 rc, Collection nodesCached, Collection evidNodes) {
		double ret = 0;
		int numNodes = rc.getNumRCNodes_Pe();
		Map callsToNode = new HashMap(numNodes);

		for(int indx = numNodes-1; indx>=0; indx--) { //ordering must be parent-child
			RC2Node nd = rc.getRCNode_Pe(indx);
			ret += expectedRCCalls_inclEvid(nd,callsToNode,nodesCached,evidNodes);
		}
		return ret;
	}
	private final static double expectedRCCalls_inclEvid(RC2Node nd, Map callsToNode, Collection nodesCached, Collection evidNodes) {
		double ret = 0;

		if(nd.isRoot()) { //one call from caller
			callsToNode.put(nd, new Double(1));
		}
		if(!nd.isLeaf()) { //internal node (ancestors already done)
			RC2NodeInternal ndi = (RC2NodeInternal)nd;
			Double d = (Double)callsToNode.get(nd);
			if(d!=null) { //if making any calls to this node (should be)
				double callsHere = d.doubleValue();
				ret += callsHere;//add calls made to this node

				Double callsToChild;
				boolean isCached;
				if(nodesCached!=null) {
					isCached = nodesCached.contains(ndi);
				}
				else {
					isCached = (ndi.actualMemoryAllocated()!=0);
				}

				double cntxRed = 1;
				double cutRed = 1;

				for(Iterator itrN=ndi.context().vars.iterator(); itrN.hasNext();) {
					FiniteVariable fv = (FiniteVariable)itrN.next();
					if(!evidNodes.contains(fv)) cntxRed *= fv.size();
				}
				for(Iterator itrN=ndi.getCutsetVars(null).iterator(); itrN.hasNext();) {
					FiniteVariable fv = (FiniteVariable)itrN.next();
					if(!evidNodes.contains(fv)) cutRed *= fv.size();
				}

				if(isCached) { //cached here
					callsToChild = new Double(cntxRed * cutRed);
				}
				else {
					callsToChild = new Double(callsHere * cutRed);
				}

				//update callsToNode for children
				Double l = (Double)callsToNode.get(ndi.left);
				Double r = (Double)callsToNode.get(ndi.right);

				if(l==null) { callsToNode.put(ndi.left, callsToChild);}
				else { callsToNode.put(ndi.left, new Double(callsToChild.doubleValue() + l.doubleValue()));}

				if(r==null) { callsToNode.put(ndi.right, callsToChild);}
				else { callsToNode.put(ndi.right, new Double(callsToChild.doubleValue() + r.doubleValue()));}
			}
		}
		else {
			Double d = (Double)callsToNode.get(nd);
			ret += d.doubleValue();
		}
		return ret;
	}



}//end class RCUtilities
