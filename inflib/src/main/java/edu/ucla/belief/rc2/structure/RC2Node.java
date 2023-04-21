package edu.ucla.belief.rc2.structure;

import java.util.*;
import java.math.BigInteger;

//{superfluous} import edu.ucla.belief.TableIndex;

/** This class represents RCNode objects.
 *
 * @author David Allen
 */
abstract public class RC2Node implements Comparable {
	static final protected BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);

	final public int nodeID; //must be larger than its children
	final public RC2 rc;

	final protected Set parentNodes = new HashSet();
	final public Collection vars;

	//context should never be changed, but can't be final since it is
	// created during initialize.
	protected RC2Index context = null;

	/** This height is calculated by how many levels of nodes
	 *   there are, not by how many edges there are between them.
	 *   (e.g. leaf nodes have height of 1, their parents have
	 *    a height of 2...)
	 */
	final public int height;


	public RC2Node(int id, RC2 rc, int height, Collection vars) {
		this.nodeID = id;
		this.rc = rc;
		this.height = height;
		this.vars = Collections.unmodifiableCollection(vars);

		if(rc==null) { throw new IllegalArgumentException("Illegal RC2 object.");}
	}//end constructor


	final public int hashCode() { return nodeID;}

	abstract void getElimOrder(ArrayList eo);


	/*
	 *  Misc Helper Functions.
	 */
	/**Called to initialize the nodes.  Implementations of this funciton
	 * should make a call to initializeNode.
	 */
	abstract public void initialize(Collection acutset);
	final protected void initializeNode(RC2Index cntx) {
		if(context==null) { //never been initialized
			context = cntx;
		}
		else {
			throw new IllegalStateException("Node initialized twice");
		}
	}
    public String toString() { return "[node:" + nodeID + "]";}
	final public boolean isRoot() { return parentNodes.isEmpty();}
    abstract public boolean isLeaf();
	final public RC2Index context() { return context;}

	/*
	 *  Caching Functions.
	 */
	/**This function will clear the local cache and reset any iterators
	 * or other variables so that a new computation can be run.
	 * <p>Call this for example if a computation is stopped.
	 */
	abstract void clearLocalCacheAndReset();
	abstract void setCaching(boolean cached);
	abstract boolean getCaching();


	/*
	 *  Parent Functions.
	 */
	final public Set parentNodes() { return Collections.unmodifiableSet(parentNodes);}
	final public int numParentNodes() { return parentNodes.size();}

	final void clearAncestorCaches() {
		clearLocalCacheAndReset();
		for(Iterator itr = parentNodes.iterator(); itr.hasNext();) {
			RC2Node nd = (RC2Node)itr.next();
			nd.clearAncestorCaches();
		}
	}




	/*
	 *  Computation Functions.
	 */
    abstract double recCondAll(long cntxIndx);  //used for normal inference on BN (iterates over all states)
    abstract double recCondAllLog(long cntxIndx);  //used for normal inference on BN (iterates over all states)

    abstract double recCondSkp(long cntxIndx);  //used for normal inference on BN (skips states with pr()=0)
    abstract double recCondSkpLog(long cntxIndx);  //used for normal inference on BN (skips states with pr()=0)

    abstract double recCondKB(long cntxIndx);    //when doing KB on BN
    abstract double recCondKBLog(long cntxIndx); //when doing genetic networks & need scaling

    abstract double recCondSAT(long cntxIndx);	//using Darwiche sat engine w/ recursive iterator
    abstract double recCondSATLog(long cntxIndx);	//using Darwiche sat engine w/ recursive iterator

    abstract double recCondMPE(long cntxIndx);





	/*
	 *  Static Helper Functions.
	 */
	final public int compareTo(Object o) {
		if(o == null) { throw new ClassCastException("RC2Node compared with null");}
		if(!(o instanceof RC2Node)) { throw new ClassCastException("RC2Node compared with " + o.getClass().getName());}
		RC2Node on = (RC2Node)o;

		if(nodeID > on.nodeID) { return 1;}
		else if(nodeID < on.nodeID) { return -1;}
		else {
			if(equals(o)) { return 0;}
			else { throw new IllegalStateException("Two nodes have same nodeID but aren't equal: " + nodeID);}
		}
	}



	/*
	 *  Interfaces
	 */
	interface RC2LeafEventHandler {

		void observe(int varIndx, int value);
		void unobserve(int varIndx);
		void unobserveAll();
		void setCPT(int varIndx);
		int getFVIndx();

	}//end interface RC2LeafHndlr

}
