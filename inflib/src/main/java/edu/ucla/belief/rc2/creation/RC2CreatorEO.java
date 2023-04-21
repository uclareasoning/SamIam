package edu.ucla.belief.rc2.creation;

import java.util.*;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;

import edu.ucla.belief.rc2.structure.*;

import edu.ucla.structure.MappedList;


/** This class generates RC2 objects for a BeliefNetwork
 *  from an elimination order.
 */

final public class RC2CreatorEO extends RC2Creator {

	private RC2CreatorEO(){}


	final static public RC2 createDtree(RC2.RCCreationParams rcParam, Params eoParam) {
		RC2 rc = new RC2(rcParam);

		RC2Node rt = createDtreeStructure(rc, eoParam);

		if(rt == null) { rc.outputInfo("WARNING: Could not create dtree root."); return null;}

		rt.initialize(Collections.EMPTY_SET);
		rc.setRoots(rt,null, null,eoParam.cs);
		return rc;
	}


	public final static class Params extends RC2Creator.Params {
		static final public int EO_ConnectRandomly = 0;
		static final public int EO_ConnectBalanced = 1;
		static final public int EO_ConnectContextMin = 2;
		static final public int EO_ConnectContextMax = 3;
		static final private int LAST_ONE = 3;
		static final public int EO_DEFAULT = EO_ConnectContextMin; //appears to work the best
		int algorithm;
		List elimOrd;
		RC2.CachingScheme cs;
		boolean useEvidIndicators;

		public Params(BeliefNetwork bn, boolean evidInd, int algo, List eo, RC2.CachingScheme cs) {
			super(bn);

			if(algo >= 0 && algo <= LAST_ONE) {
				algorithm = algo;
			}
			else {
				throw new IllegalArgumentException("Illegal Elimination Order Algorithm: " + algo);
			}

			useEvidIndicators = evidInd;
			elimOrd = eo;
			if(!eo.containsAll(bn)) {
				throw new IllegalArgumentException("Elimination Order does not match BeliefNetwork");
			}
			this.cs = cs;
		}
	}


	///////////////////////////////
	//  Structure Creation Functions
	///////////////////////////////


	static final public RC2Node createDtreeStructure(RC2 rc, Params eoParam) {
		if(rc.vars().size()==0) { System.err.println("Attempted to create a dtree with no nodes."); return null;}

		Map minVals = new HashMap();
		MappedList eo = new MappedList(eoParam.elimOrd);
		LinkedList nodesWithVar[] = new LinkedList[eo.size()+1];

		placeInitialNodes(eo, nodesWithVar, createLeafNodes(rc, eoParam.useEvidIndicators, minVals));

		int nextNodeID = (eoParam.useEvidIndicators ? rc.vars().size()*2 : rc.vars().size());

		for(int i=0; i<eo.size(); i++) {
			if(nodesWithVar[i]==null) {continue;}

//if(i%1000==0) System.out.println("start elim var "+(i+1)+": " + new Date()); //TODO REMOVE

			RC2Node t;

			switch(eoParam.algorithm) {
				case Params.EO_ConnectRandomly:
					t = eliminateVariable_rndm(nodesWithVar[i], nextNodeID, minVals);
					break;

				case Params.EO_ConnectBalanced:
					t = eliminateVariable_bal(nodesWithVar[i], nextNodeID, minVals);
					break;

				case Params.EO_ConnectContextMin:
					t = eliminateVariable_cntx(nodesWithVar, i, nextNodeID, true, minVals);
					break;

				case Params.EO_ConnectContextMax:
					t = eliminateVariable_cntx(nodesWithVar, i, nextNodeID, false, minVals);
					break;

				default:
					throw new IllegalStateException("");
			}
			if(t!=null) {
				if(t.nodeID+1 > nextNodeID) { nextNodeID = t.nodeID+1;}
				placeNode(eo, nodesWithVar, t, i);
			}
		}

		//nodes which contain disjoint variables (ie not connected up together)
		RC2Node rt=null;
		int len = nodesWithVar.length-1;
		if(nodesWithVar[len]!=null) {
			rt = connectNodes(nodesWithVar[len], nextNodeID, minVals);
		}
		return rt;
	}



	///////////////////////////////
	//  EliminateVariable Functions
	///////////////////////////////


	/**Will "randomly" connect up all nodes in allNodesToConnect
	 * into a subtree and return the root of it.  Can possibly return null if no
	 * nodes are in nodesToConnect.
	 * @param allNodesToConnect This should never be null, and at the end this will be empty.
	 */
	final static protected RC2Node eliminateVariable_rndm(LinkedList allNodesToConnect, int nextNodeID, Map minVals) {
		RC2Node ret = connectNodes(allNodesToConnect, nextNodeID, minVals);
		return ret;
	}


	/**Will "randomly" connect up all nodes in allNodesToConnect
	 * into a subtree attempting to balance it and return the root of it.  Can possibly
	 * return null if no nodes are in allNodesToConnect
	 * @param allNodesToConnect This should never be null, and at the end this will be empty.
	 */
	final static protected RC2Node eliminateVariable_bal(LinkedList allNodesToConnect, int nextNodeID, Map minVals) {

		while(allNodesToConnect.size()>1) { //have at least two nodes
			RC2Node n1 = (RC2Node)allNodesToConnect.removeFirst();//find one with smallest height
			RC2Node n2 = (RC2Node)allNodesToConnect.removeFirst();//find one with next smallest height

			{//find two with smallest height & remove from list
				if(n2.height<n1.height) {
					RC2Node tmp = n1;
					n1 = n2;
					n2 = tmp;
				}

				ListIterator itr = allNodesToConnect.listIterator();
				while(itr.hasNext()) {
					RC2Node n3 = (RC2Node)itr.next();
					if(n3.height < n1.height) {
						itr.remove();
						itr.add(n2); //add it back into list, prior to iterator
						n2=n1;
						n1=n3;
					}
					else if(n3.height < n2.height) {
						itr.remove();
						itr.add(n2); //add it back into list, prior to iterator
						n2=n3;
					}
				}
			}
			RC2Node newNd = new RC2NodeInternal(nextNodeID, n1.rc, n1, n2, minVals);
			allNodesToConnect.add(newNd);
			nextNodeID++;
		}

		if(allNodesToConnect.size()==1) {
			RC2Node ret = (RC2Node)allNodesToConnect.removeFirst();
			return ret;
		}
		else {
			return null;
		}
	}


	/**Will connect up all nodes in allNodesToConnect[indx]
	 * by determining the context of this subtree's root and then connecting
	 * all nodes containing those variables in a specified order.
	 * Can possibly return null if no nodes are in allNodesToConnect[indx].
	 * @param allNodesToConnect allNodesToConnect[indx] should not be null, and at the end this will be empty.
	 */
	final static protected RC2Node eliminateVariable_cntx(LinkedList allNodesToConnect[], int indx, int nextNodeID, boolean cntxVarsMinNodes, Map minVals) {

		if(allNodesToConnect[indx].size()==0) { return null;}
		else if(allNodesToConnect[indx].size()==1) { return (RC2Node)allNodesToConnect[indx].removeFirst();}


		Collection cntxVars;
		{
			Collection connectVars = varsInSet(allNodesToConnect[indx]);
			Collection otherVars = new HashSet();

			for(int i=indx+1; i<allNodesToConnect.length; i++) {
				if(allNodesToConnect[i]!=null) varsInSet(allNodesToConnect[i], otherVars);
			}

			otherVars.retainAll(connectVars); //context is connectVars union otherVars
			cntxVars = otherVars;
		}

		ArrayList nodesCreated = new ArrayList(cntxVars.size()); //will generate nodes, but not connect them until the end

		while(allNodesToConnect[indx].size() > 0) {

			//find var to eliminate which is in the context (if no more, connect up all nodes which have no context variables)
			Object varToRemove;
			{
				if(cntxVarsMinNodes) {
					varToRemove = varInFewestNodes(cntxVars,allNodesToConnect[indx]);
				}
				else {
					varToRemove = varInMostNodes(cntxVars,allNodesToConnect[indx]);
				}
			}


			RC2Node nodeConnected;
			if(varToRemove == null) { //no more context variables appear in nodes, connect the rest up
				nodeConnected = connectNodes(allNodesToConnect[indx], nextNodeID, minVals);
				if(nodeConnected.nodeID+1 > nextNodeID) { nextNodeID = nodeConnected.nodeID+1;}
			}
			else { //connect up all nodes containing that variable (possibly only 1)
				nodeConnected = connectNodesContaining((FiniteVariable)varToRemove, allNodesToConnect[indx], nextNodeID, minVals);
				if(nodeConnected.nodeID+1 > nextNodeID) { nextNodeID = nodeConnected.nodeID+1;}
			}

			/*nodeConnected will not be added to nodesToConnect_eo, as it will instead be the sibling
			 * of the connection of all other nodes in nodesToConnect_eo.  This attempts to put this node
			 * higher in the subtree in order to adjust how the contexts use memory.
			 */

			//store to connect all at the end (in reverse order)
			nodesCreated.add(nodeConnected);
		}

		//connect up all nodes in nodesCreated
		RC2Node ret = null;
		if(nodesCreated.size()>0) { ret = (RC2Node)nodesCreated.remove(nodesCreated.size()-1);}
		while(nodesCreated.size()>0) {
			ret = new RC2NodeInternal(nextNodeID, ret.rc, ret, (RC2Node)nodesCreated.remove(nodesCreated.size()-1), minVals);
			nextNodeID++;
		}
		return ret;
	}




	////////////////////
	//  Helper Functions
	////////////////////

	/**Count the number of nodes that a set of variables is in and return the
	 *  variable in the fewest number of nodes (>0).
	 */
	static private Object varInFewestNodes(Collection cntxVars, Collection nodesToConnect_eo) {
		Object ret = null;
		int count = Integer.MAX_VALUE;

		for(Iterator itr_v = cntxVars.iterator(); itr_v.hasNext();) {
			Object var = itr_v.next();
			int tmp_cnt = 0;

			for(Iterator itr = nodesToConnect_eo.iterator(); itr.hasNext();) {
				RC2Node nd = (RC2Node)itr.next();
				if(nd.vars.contains(var)) { tmp_cnt++;}
			}

			if(tmp_cnt > 0 && tmp_cnt < count) {
				ret = var;
				count = tmp_cnt;
			}
		}
		return ret;
	}

	/**Count the number of nodes that a set of variables is in and return the
	 *  variable in the most number of nodes (>0).
	 */
	static private Object varInMostNodes(Collection cntxVars, Collection nodesToConnect_eo) {
		Object ret = null;
		int count = 0;

		for(Iterator itr_v = cntxVars.iterator(); itr_v.hasNext();) {
			Object var = itr_v.next();
			int tmp_cnt = 0;

			for(Iterator itr = nodesToConnect_eo.iterator(); itr.hasNext();) {
				RC2Node nd = (RC2Node)itr.next();
				if(nd.vars.contains(var)) { tmp_cnt++;}
			}

			if(tmp_cnt > count) {
				ret = var;
				count = tmp_cnt;
			}
		}
		return ret;
	}

	static private void placeInitialNodes(List eo, LinkedList nodesWithVar[], Set nodes) {
		for(Iterator itr=nodes.iterator(); itr.hasNext();) {
			placeNode(eo, nodesWithVar, (RC2Node)itr.next(), -1);
		}
		nodes.clear();
	}

	static private void placeNode(List eo, LinkedList nodesWithVar[], RC2Node nd, int greaterThan) {
		int firstIndx = eo.size();//this is an actual index in nodesWithVar to catch all nodes with no more variables in common.

		for(Iterator itr_v = nd.vars.iterator(); itr_v.hasNext();) {
			FiniteVariable fv = (FiniteVariable)itr_v.next();
			int indx = eo.indexOf(fv);
			if(indx < firstIndx && indx > greaterThan) { firstIndx=indx;}
		}
		if(nodesWithVar[firstIndx]==null) {
			nodesWithVar[firstIndx]=new LinkedList();
		}
		nodesWithVar[firstIndx].add(nd);
	}



}//end class RC2CreatorEO


