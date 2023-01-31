package edu.ucla.belief.rc2.creation;

import java.util.*;

import edu.ucla.belief.rc2.structure.*;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.BeliefNetwork;



/** This class orients Dtrees into Dgraphs.
 */

public class RC2CreatorDtToDg {

	private RC2CreatorDtToDg(){}

	/**Cannot be called if rt has already been added to the RC object in rt.rc.
	 * TODO document more
	 */
	static public RC2Node[] orient(RC2Node rt, Map minVals) {
		HashMap orientCache = new HashMap(); //map of OrientedPair to RC2Node (leafs not included)
		Map parentsInDt = new HashMap(); //map of RC2Node to RC2Node
		Collection leafNodes = new HashSet(); //collection of RC2Node objects

		RC2Utils.computeLeafs(rt, leafNodes);
		RC2Utils.computeParentsInTree(rt, parentsInDt);

		//find largest leaf nodeID (can ignore others, as will recreate new nodes)
		int nextNodeID = 0;
		{
			for(Iterator itr_nd = leafNodes.iterator(); itr_nd.hasNext();) {
				RC2Node lf = (RC2Node)itr_nd.next();
				if( lf.nodeID >= nextNodeID) {
					nextNodeID = lf.nodeID+1;
				}
			}
		}

		RC2Node roots[] = new RC2Node[leafNodes.size()];
		if(roots.length == 0) { return roots;}
		else if(roots.length == 1) { roots[0] = (RC2Node)leafNodes.iterator().next(); return roots;}

		{//in the parents map, "remove" the root & for its children add each
		 //other as parents of their siblings
			RC2Node left = ((RC2NodeInternal)rt).left;
			RC2Node right = ((RC2NodeInternal)rt).right;
			parentsInDt.put(left,right);
			parentsInDt.put(right,left);
		}


		//there are at least 2 leaf nodes
		int nextRt = 0;
		for(Iterator itr_lf = leafNodes.iterator(); itr_lf.hasNext();) {
			//for each leaf create a new root
			RC2Node leaf = (RC2Node)itr_lf.next();
			RC2Node parent = (RC2Node)parentsInDt.get(leaf);
			RC2Node n2 = orientNode(parent, leaf, orientCache, parentsInDt, nextNodeID, minVals);
			if(n2.nodeID >= nextNodeID) { nextNodeID = n2.nodeID+1;}

			roots[nextRt++] = new RC2NodeInternal(nextNodeID++, leaf.rc, leaf, n2, minVals);
		}
		return roots;
	}

	/**Orient node "to" with respect to having a parent "from".*/
	static private RC2Node orientNode(RC2Node to, RC2Node from, Map orientCache, Map parentsInDt, int nextNodeID, Map minVals) {
		if(to.isLeaf()) { return to;}
		else {
			OrientedPair op = new OrientedPair(to, from);
			RC2Node m = (RC2Node)orientCache.get(op);
			if(m==null) { //have not done it yet
				RC2Node n1 = (RC2Node)parentsInDt.get(to);
				RC2Node n2 = ((RC2NodeInternal)to).left;
				{
					RC2Node n3 = ((RC2NodeInternal)to).right;
					if(n1==from) {n1 = n3;}
					else if(n2==from) {n2 = n3;}
					else if(n3==from) {}//n1 and n2 are ok
					else { throw new IllegalStateException("");}
				}

				RC2Node ln1 = orientNode(n1, to, orientCache, parentsInDt, nextNodeID, minVals);
				if(ln1.nodeID >= nextNodeID) { nextNodeID = ln1.nodeID+1;}
				RC2Node ln2 = orientNode(n2, to, orientCache, parentsInDt, nextNodeID, minVals);
				if(ln2.nodeID >= nextNodeID) { nextNodeID = ln2.nodeID+1;}
				m = new RC2NodeInternal(nextNodeID++, ln1.rc, ln1, ln2, minVals);
				orientCache.put(op,m);
				return m;
			}
			else { //have already oriented this node
				return m;
			}
		}
	}


	/** If the roots is not from a complete dgraph, it is possible for the preprocessing to produce incorrect
	 *   results.  As in this case, a variable (in varsOfInterest) may not appear in a cutset and could possibly be
	 *   mapped to another root thinking it is a leaf variable with no evidence indicators.
	 */
	static public ReducedRootSet reduceToOnlyMarginals(RC2Node roots[], Collection varsOfInterest, BeliefNetwork bn) {

		ArrayList rootsToReturn = new ArrayList(roots.length);
		HashMap varToRootIndx = new HashMap(varsOfInterest.size()); //map each variable in varsOfInterest to an Integer, which is the index of the root

		HashSet myVarsOfInterest = new HashSet(varsOfInterest);
		HashSet nodesCovered[] = new HashSet[roots.length];


		//iterate through roots & ask for all vars in cutset reduced to varsOfInterest
		for(int i=0; i<roots.length; i++) {
			if(!roots[i].isLeaf()) {
				RC2NodeInternal rni = (RC2NodeInternal)roots[i];

				nodesCovered[i] = (HashSet)rni.getCutsetVars(new HashSet());
				//nodesCovered[i].retainAll(myVarsOfInterest);//Will do this after preprocessing of root nodes
			}
			else { //leaf should only be root if network only contains single variable
				if(roots.length != 1 || varsOfInterest.size()>1) {
					throw new IllegalStateException("Root node is a leaf, which should only happen in single variable networks.");
				}
				else if(!roots[0].vars.contains(varsOfInterest.iterator().next())) {
					throw new IllegalStateException("Variable of interest was not found in these roots");
				}
				else {
					rootsToReturn.add(roots[0]);
					varToRootIndx.put(varsOfInterest.iterator().next(), new Integer(0));
				}
				return new ReducedRootSet((RC2Node[])rootsToReturn.toArray(new RC2Node[rootsToReturn.size()]), varToRootIndx);
			}
		}//end for each root

		//preprocess roots (look for roots which have to be included)(e.g. if that is the only cutset with that variable) //TODO

		//preprocess roots (look for any vars which are not in any root cutsets, & possibly add a cutset with their entire family).
		//  this should be done if it is a leaf node in the BN, and evidence indicators are not being used.
		{
			HashSet varsNotInAnyCutset = new HashSet(myVarsOfInterest);
			for(int i=0; i<nodesCovered.length; i++) {
				varsNotInAnyCutset.removeAll(nodesCovered[i]);
			}
			for(Iterator itr = varsNotInAnyCutset.iterator(); itr.hasNext();) {
				Object var = itr.next();
				if(bn.outDegree(var) == 0) { //variable is a leaf, can't easily check for evidence indicators so don't
					//map the var to a root which contains their entire family
					FiniteVariable fv = (FiniteVariable)var;
					HashSet family = new HashSet(fv.getCPTShell().variables());
					family.remove(fv);

					//find a root //TODO: could actually look for one with smallest/largest cutset or one already included
					for(int i=0; i<roots.length; i++) {
						if(nodesCovered[i].containsAll(family)) {
							//add it
							rootsToReturn.add(roots[0]);
							Integer rtIndx = new Integer(rootsToReturn.size()-1);
							for(Iterator itr_c = nodesCovered[i].iterator(); itr_c.hasNext();) {
								Object varIncl = itr_c.next();
								if(myVarsOfInterest.contains(varIncl)) {
									varToRootIndx.put(varIncl, rtIndx);
								}
							}
							break;
						}
					}
				}
				else {
					System.out.println("Variable " + var + " was in vars of interest, but not in any root cutsets.");
					myVarsOfInterest.remove(var);
					varToRootIndx.put(var, new Integer(-1));
				}
			}
		}

		//fix nodesCovered & myVarsOfInterest based on above preprocessing
		{
			myVarsOfInterest.removeAll(varToRootIndx.keySet());
			for(int i=0; i<nodesCovered.length; i++) {
				nodesCovered[i].retainAll(myVarsOfInterest);
			}
		}

		//iteratively pick root with largest number of vars in the cutset which are also in myVarsOfInterest
		while(myVarsOfInterest.size() > 0) {//there are still variables to find roots for
			int bestCutsetIndx = -1;
			int bestCutsetSize = -1;

			//find root which has maximal covering over myVarsOfInterest
			for(int i=0; i<nodesCovered.length; i++) {
				if(nodesCovered[i] == null) { continue;}

				if(nodesCovered[i].size() > bestCutsetSize) {
					bestCutsetIndx = i;
					bestCutsetSize = nodesCovered[i].size();
				}
			}

			if(bestCutsetSize > 0) {
				rootsToReturn.add(roots[bestCutsetIndx]); //add this one to return
				Integer rtIndx = new Integer(rootsToReturn.size()-1);

				for(Iterator itr = nodesCovered[bestCutsetIndx].iterator(); itr.hasNext();) {
					Object var = itr.next();
					if(myVarsOfInterest.contains(var)) {
						varToRootIndx.put(var, rtIndx); //map this variable as covered
						myVarsOfInterest.remove(var);   //remove it from those left
					}
				}

				//reduce all nodesCovered[]
				for(int i=0; i<nodesCovered.length; i++) {
					if(nodesCovered[i] == null) { continue;}

					nodesCovered[i].removeAll(nodesCovered[bestCutsetIndx]);
					if(nodesCovered[i].size() == 0) { nodesCovered[i] = null;}
				}
			}
			else {
				throw new IllegalStateException("Didn't find a best cutset size for reduction, but there were variables not covered yet.");
			}
		}

		return new ReducedRootSet((RC2Node[])rootsToReturn.toArray(new RC2Node[rootsToReturn.size()]), varToRootIndx);
	}

	public static class ReducedRootSet {
		public final RC2Node[] roots;
		public final HashMap varToRootIndx; //map each variable to an Integer, which is the index of the root

		public ReducedRootSet(RC2Node[] roots, HashMap varToRootIndx) {
			this.roots = roots;
			this.varToRootIndx = varToRootIndx;
		}
	}


	private static class OrientedPair {
		final public RC2Node to;
		final public RC2Node from;

		public OrientedPair(RC2Node to, RC2Node from) {
			this.to = to;
			this.from = from;
		}

		public boolean equals(Object o) {
			if(o instanceof OrientedPair) {
				OrientedPair op = (OrientedPair)o;
				if(to == op.to && from == op.from) {
					return true;
				}
			}
			return false;
		}
		public int hashCode() {
			return to.hashCode() + from.hashCode(); //TODO not a very good hashCode
		}

	}

} //end class RC2CreatorDtToDg


