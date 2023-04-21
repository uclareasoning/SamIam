package edu.ucla.belief.rc2.structure;

import java.util.*;

import edu.ucla.belief.FiniteVariable;

/** This class represents Leaf RCNode objects which are evidence indicators..
 *
 * @author David Allen
 */
public class RC2NodeLeafEvidInd extends RC2Node implements RC2Node.RC2LeafEventHandler {

	final private FiniteVariable fvLocal;
	final private int fvLocalIndx; //index of fvLocal in rc.vars
	final private double evidInd[]; //probability of being in a particular state (hard or soft evidence)


	public RC2NodeLeafEvidInd(int id, RC2 rc, FiniteVariable fv) {
		super(id, rc, 1, Collections.singleton(fv));
		this.fvLocal = fv;
		this.fvLocalIndx = rc.vars.indexOf(fv);

		evidInd = new double[fvLocal.size()];
		Arrays.fill(evidInd,1);

		if(fv==null||fvLocalIndx<0) { throw new IllegalArgumentException("Illegal FiniteVariable: " + fv);}
	}//end constructor


	public void initialize(Collection acutset) {
		final boolean firstCall = (context==null);
		if(firstCall) {

			if(!acutset.contains(fvLocal)) {
				throw new IllegalStateException("Illegal Acutset in RC2NodeLeafEvidInd("+fvLocal.getID()+"): " + acutset);
			}

			super.initializeNode(new RC2Index(rc,Collections.singleton(fvLocal)));
		}
	}

	public String toString() { return "[leaf:" + nodeID + " evid " + fvLocal.getID() + "]";}
    public boolean isLeaf() { return true;}

	void clearLocalCacheAndReset(){}
	void setCaching(boolean cached) {
		if(cached) { throw new IllegalStateException("Attemped to cache leaf node");}
	}
	boolean getCaching() {return false;}

	//all vars are instantiated by defn.
	double recCondAll(long cntxIndx){
		return evidInd[(int)cntxIndx];
	}

	//all vars are instantiated by defn.
	double recCondAllLog(long cntxIndx){
		System.out.println("SPEEDUP: Using evidence indicators with RC-Log functions");
		return evidInd[(int)cntxIndx];
	}

	//all vars are instantiated by defn.
	double recCondSkp(long cntxIndx){
		rc.rcCallsCounter++;
		return evidInd[(int)cntxIndx];
	}

	//all vars are instantiated by defn.
	double recCondSkpLog(long cntxIndx){
		System.out.println("SPEEDUP: Using evidence indicators with RC-Log functions");
		rc.rcCallsCounter++;
		return evidInd[(int)cntxIndx];
	}

	//all vars are instantiated by defn.
	double recCondKB(long cntxIndx) {
		rc.rcCallsCounter++;
		return evidInd[(int)cntxIndx];
	}

	//all vars are instantiated by defn.
	//scaling 0 or 1 results in 0 or 1.
	double recCondKBLog(long cntxIndx){
		System.out.println("SPEEDUP: Using evidence indicators with RC-Log functions");
		rc.rcCallsCounter++;
		return evidInd[(int)cntxIndx];
	}

	double recCondSAT(long cntxIndx) {
		System.out.println("SPEEDUP: Using evidence indicators with RC-SAT functions");
		rc.rcCallsCounter++;
		return evidInd[(int)cntxIndx];
	}

	//all vars are instantiated by defn.
	//scaling 0 or 1 results in 0 or 1.
	double recCondSATLog(long cntxIndx){
		System.out.println("SPEEDUP: Using evidence indicators with RC-SAT-Log functions");
		rc.rcCallsCounter++;
		return evidInd[(int)cntxIndx];
	}

	//all vars are instantiated by defn.
	double recCondMPE(long cntxIndx){
		return evidInd[(int)cntxIndx];
	}

	public void observe(int varIndx, int value) {
		if(varIndx == fvLocalIndx) {
			if(value < 0) { unobserve(varIndx);}
			else {
				Arrays.fill(evidInd,0);
				evidInd[value]=1.0;
				clearAncestorCaches();
			}
		}
	}
	public void unobserve(int varIndx) {
		if(varIndx == fvLocalIndx) {
			Arrays.fill(evidInd,1);
			clearAncestorCaches();
		}
	}
	public void unobserveAll() {
		Arrays.fill(evidInd,1);
		//clearAncestorCaches();done in RC
	}
	public void setCPT(int varIndx) {
		if(varIndx == fvLocalIndx) {clearAncestorCaches();}
	}

	public int getFVIndx() {return fvLocalIndx;}
	public FiniteVariable getLeafVar() {return fvLocal;}

	void getElimOrder(ArrayList eo) {}

}//end class RC2NodeLeafEvidInd
