package edu.ucla.belief.rc2.creation;

import java.util.*;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;

import edu.ucla.belief.rc2.structure.*;



/** This class generates RC2 objects for a BeliefNetwork.
 */

abstract public class RC2Creator {


	/**Will connect up the RC2Nodes in nodes and return the root of the subtree created.
	 *  <p>Any new nodes will start with nextNodeID and if any are created then
	 *     the largest one NodeID will be that of the returned root.  However if
	 *     nodes_in only contains a single variable, root may not have the largest ID.
	 *  <p>Will return null only if the nodes collection is empty (will throw and
	 *     exception if nodes is null).
	 *  <p>nodes will be empty when it is finished.
	 */
	final static protected RC2Node connectNodes(Collection nodes, int nextNodeID, Map minVal) {
		RC2Node ret = connectNodes(new LinkedList(nodes), nextNodeID, minVal);
		nodes.clear();
		return ret;
	}

	final static protected RC2Node connectNodes(LinkedList nodes, int nextNodeID, Map minVal) {
		if(nodes.size()==0) {return null;}

		//create a (somewhat) balanced list, because it will reduce the height and also
		// sometimes the size of the "vars set", especially in the genetics networks

		while(nodes.size()>1) {
			RC2Node tmp1 = (RC2Node)nodes.removeFirst();
			RC2Node tmp2 = (RC2Node)nodes.removeFirst();
			nodes.addLast(new RC2NodeInternal(nextNodeID, tmp1.rc, tmp1, tmp2, minVal));
			nextNodeID++;
		}

		return (RC2Node)nodes.removeFirst();
	}

	final static protected RC2Node connectNodesContaining(FiniteVariable fv, Collection nodes_in, int nextNodeID, Map minVal) {
		if(nodes_in.size()==0) {return null;}

		LinkedList nodes = new LinkedList(nodes_in);

		LinkedList nodesToConnect = new LinkedList();
		for(Iterator itr = nodes.iterator(); itr.hasNext();) {
			RC2Node nd = (RC2Node)itr.next();
			if(nd.vars.contains(fv)) {
				nodesToConnect.add(nd);
				nodes_in.remove(nd);//remove it from nodes_in, don't care about nodes
			}
		}
		nodes.clear();

		if(nodesToConnect.size()==0) {return null;}

		RC2Node ret = connectNodes(nodesToConnect, nextNodeID, minVal);
		return ret;
	}


	final static protected RC2Node connectNodesContaining(FiniteVariable fv, LinkedList nodes, int nextNodeID, Map minVal) {
		if(nodes.size()==0) {return null;}

		LinkedList nodesToConnect = new LinkedList();
		for(Iterator itr = nodes.iterator(); itr.hasNext();) {
			RC2Node nd = (RC2Node)itr.next();
			if(nd.vars.contains(fv)) {
				nodesToConnect.add(nd);
				itr.remove();//remove it from nodes
			}
		}

		if(nodesToConnect.size()==0) {return null;}

		RC2Node ret = connectNodes(nodesToConnect, nextNodeID, minVal);
		return ret;
	}








	/**Returns a set of leaf nodes (RC2Node objects).
	 *  If useEvidenceIndicators then it is twice as big, with some being
	 *  the evidence indicators.
	 */
	 final static protected HashSet createLeafNodes(RC2 rc, boolean useEvidenceIndicators, Map minVals) {
		 int numVars = rc.vars().size();

		 HashSet ret = new HashSet((useEvidenceIndicators?2*numVars:numVars));

		 for(int i=0; i<numVars; i++) {
			 FiniteVariable fv = (FiniteVariable)rc.vars().get(i);
			 ret.add(new RC2NodeLeaf(i, rc, fv, minVals));

			 if(useEvidenceIndicators) { ret.add(new RC2NodeLeafEvidInd(i+numVars, rc, fv));}
		 }
		 return ret;
	 }


	/**For each entry in nodes, union all the variables together.
	 */
	final static protected Collection varsInSet(Collection nodes) {
		return varsInSet(nodes, null);
	}
	/**For each entry in nodes, union all the variables together.
	 */
	final static protected Collection varsInSet(Collection nodes, Collection ret) {
		if(ret==null) { ret = new HashSet();}

		for(Iterator itr_nd = nodes.iterator(); itr_nd.hasNext();) {
			RC2Node nd = (RC2Node)itr_nd.next();
			ret.addAll(nd.vars);
		}
		return ret;
	}



	static public class Params {
		BeliefNetwork bn;

		public Params(BeliefNetwork bn) {
			this.bn = bn;
		}
	}


} //end class RC2Creator


