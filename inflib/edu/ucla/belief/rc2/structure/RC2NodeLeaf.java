package edu.ucla.belief.rc2.structure;

import java.util.*;
import java.math.BigInteger;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.CPTShell;

/** This class represents Leaf RCNode objects.
 *
 * @author David Allen
 */
public class RC2NodeLeaf extends RC2Node implements RC2Node.RC2LeafEventHandler {

	static final private double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;

	final private FiniteVariable fvLocal;
	final private int fvLocalIndx; //index of fvLocal in rc.vars
	final private int fvNumStates;
	private int fvBlkSz; //used to iterate over variables states if necessary (else it is -1)
	private CPTShell cptshell;
	final private double data[]; //don't lookup in cptshell (however expands noisy or nodes)

	//if fvLocal is not in the context, then precompute the sum of all states under each context setting
	//  so that don't have to repeat the (log) addition multiple times.
	private double precomputeSum[] = null;

	final private int inst[];
	final private int userEvid[];
	private boolean flagPreScaleData = false; //don't change except in initialize call


	public RC2NodeLeaf(int id, RC2 rc, FiniteVariable fv, Map minVal) {
		super(id, rc, 1, new HashSet(fv.getCPTShell().variables()));
		this.fvLocal = fv;
		this.fvLocalIndx = rc.vars.indexOf(fv);
		this.fvNumStates = fvLocal.size();
		this.cptshell = fvLocal.getCPTShell();

		inst = rc.instantiation;
		userEvid = rc.userEvid;

		data = new double[cptshell.index().size()];
		setData();

		{
			double min=Double.MAX_VALUE;
			for(int i=0; i<data.length; i++) {
				if(data[i]>0 && data[i] < min) {
					min = data[i];
				}
			}
			minVal.put( this, new Double(min));
		}

		if(fv==null||fvLocalIndx<0) { throw new IllegalArgumentException("Illegal FiniteVariable: " + fv);}
	}//end constructor


	public void initialize(Collection acutset) {
		final boolean firstCall = (context==null);
		if(firstCall) {

			flagPreScaleData = true;
			for(Iterator itr_par = parentNodes.iterator(); itr_par.hasNext();) {
				RC2NodeInternal par = (RC2NodeInternal)itr_par.next();
				if(par.flagCallLog==false) { flagPreScaleData=false; break;}
			}
			if(flagPreScaleData) { setData();}


			boolean fvInCntx = (acutset.contains(fvLocal));

			ArrayList cntx = new ArrayList(cptshell.index().variables());
			if(!fvInCntx) { cntx.remove(fvLocal);}

			{
				long blkSz[] = new long[cntx.size()];
				for(int i=0; i<blkSz.length; i++) {
					blkSz[i] = cptshell.index().blockSize((FiniteVariable)cntx.get(i));
				}
				super.initializeNode(new RC2Index(rc,cntx,blkSz));
			}


			//initialize fvItr
			if( fvInCntx) {
				fvBlkSz = -1;
			}
			else {
				//fvBlkSz & fvNumStates will be used to iterate over variable states
				fvBlkSz = cptshell.index().blockSize(fvLocal);

				//assumes fvBlkSz==1 (i.e. it is the last variable) It does this to make computing the
				//  context index easier
				if(fvBlkSz != 1) { throw new IllegalStateException("");}

				precomputeSum();
			}
		}
	}

	private void setData() {
		for(int i=0; i<data.length; i++) {
			//will work for scaled or not
			if(rc.scalar == 1.0 || !flagPreScaleData) {
				data[i] = cptshell.getCP(i);
			}
			else {
				data[i] = Math.pow(cptshell.getCP(i), 1.0/rc.scalar); //scale result
			}
		}
	}

	private void precomputeSum() {
		//only precompute this if the finite variable is not in the context
		// and this node has been initialized
		if(fvBlkSz != -1 && (context!=null)) {
			if(precomputeSum == null) { //possibly already allocated from a previous call
				BigInteger size = context.totalStateSpace();
				if(size.compareTo(maxInt)>0) { throw new IllegalStateException();}
				precomputeSum = new double[size.intValue()];
			}

			for(int n_cntxIndx = 0; n_cntxIndx < precomputeSum.length; n_cntxIndx++) {
				int cntxIndx = n_cntxIndx * fvNumStates;

				if(rc.scalar==1.0 || !flagPreScaleData) {
					double ret = data[cntxIndx]; //state 0
					int tmpCntxIndx = cntxIndx;
					for(int i=1; i<fvNumStates; i++) {//states 1,2,3...
						tmpCntxIndx += fvBlkSz;
						ret += data[tmpCntxIndx];
					}
					precomputeSum[n_cntxIndx] = ret;
				}
				else {
					double s_lna = rc.scalar*Math.log(data[cntxIndx]);
					int tmpCntxIndx = cntxIndx;
					for(int i=1; i<fvNumStates; i++) {
						tmpCntxIndx += fvBlkSz;
						double tmp = data[tmpCntxIndx];

						if(tmp!=0) {//logsum: (ret+=tmp)
							double s_lnb=rc.scalar*Math.log(tmp);
							if(s_lna==NEGATIVE_INFINITY) { s_lna=s_lnb;} //ret was 0
							else {
								//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is MAX(lnb, lna)//TODO not done for speed
								s_lna += Math.log(1.0 + Math.exp(s_lnb - s_lna));
							}
						}
					}
					precomputeSum[n_cntxIndx] = Math.exp(s_lna/rc.scalar);;
				}
			}//end for cntxIndx
		}
	}

	public String toString() { return "[leaf:" + nodeID + " cpt " + fvLocal.getID() + "]";}
    public boolean isLeaf() { return true;}

	void clearLocalCacheAndReset(){}
	void setCaching(boolean cached) {
		if(cached) { throw new IllegalStateException("Attemped to cache leaf node");}
	}
	boolean getCaching() {return false;}

	double recCondAll(long cntxIndxLg){
		int cntxIndx = (int)cntxIndxLg;
		rc.rcCallsCounter++;
		if(fvBlkSz == -1) { //fv in cntx
			return data[cntxIndx];
		}
		else { //fv not in cntx (may or may not be instantiated by user, check rc.userEvid)
			int val = userEvid[fvLocalIndx];
			if(val != -1) { //is instantiated
				return data[cntxIndx + (val * fvBlkSz)];
			}
			else { //is not instantiated (actually should normally return 1.0 if everything is normalized)
				return precomputeSum[cntxIndx/fvNumStates];
			}
		}
	}

	double recCondAllLog(long cntxIndxLg){
		int cntxIndx = (int)cntxIndxLg;
		rc.rcCallsCounter++;
		double ret;
		if(fvBlkSz == -1) { //fv in cntx
			ret = data[cntxIndx];
		}
		else { //fv not in cntx (may or may not be instantiated by user, check rc.userEvid)
			int val = userEvid[fvLocalIndx];
			if(val != -1) { //is instantiated
				ret = data[cntxIndx + (val * fvBlkSz)];
			}
			else { //is not instantiated (actually should normally return 1.0 if everything is normalized)
				ret = precomputeSum[cntxIndx/fvNumStates];
			}
		}
		if(!flagPreScaleData) {
			return Math.pow(ret, 1/rc.scalar);
		}
		else {return ret;}
	}

	double recCondSkp(long cntxIndxLg){
		int cntxIndx = (int)cntxIndxLg;
		rc.rcCallsCounter++;
		if(fvBlkSz == -1) { //fv in cntx
			return data[cntxIndx];
		}
		else { //fv not in cntx (may or may not be instantiated by user)
			int val = userEvid[fvLocalIndx];
			if(val != -1) { //is instantiated
				return data[cntxIndx + (val * fvBlkSz)];
			}
			else { //is not instantiated (actually should normally return 1.0 if everything is normalized)
				return precomputeSum[cntxIndx/fvNumStates];
			}
		}
	}

	double recCondSkpLog(long cntxIndxLg){
		int cntxIndx = (int)cntxIndxLg;
		rc.rcCallsCounter++;
		double ret;
		if(fvBlkSz == -1) { //fv in cntx
			ret = data[cntxIndx];
		}
		else { //fv not in cntx (may or may not be instantiated by user)
			int val = userEvid[fvLocalIndx];
			if(val != -1) { //is instantiated
				ret = data[cntxIndx + (val * fvBlkSz)];
			}
			else { //is not instantiated (actually should normally return 1.0 if everything is normalized)
				ret = precomputeSum[cntxIndx/fvNumStates];
			}
		}
		if(!flagPreScaleData) {
			return Math.pow(ret, 1/rc.scalar);
		}
		else {return ret;}
	}

	/*NOT same as above, since must check instantiation array instead of userEvid array, due to
	 * possible evidence from KB.
	 */
	double recCondKB(long cntxIndxLg) {
		int cntxIndx = (int)cntxIndxLg;
		rc.rcCallsCounter++;
		if(fvBlkSz == -1) { //fv in cntx
			return data[cntxIndx];
		}
		else { //fv not in cntx (may or may not be instantiated by user or by the KB)
			int val = inst[fvLocalIndx];
			if(val != -1) { //is instantiated
				return data[cntxIndx + (val * fvBlkSz)];
			}
			else { //is not instantiated (actually should normally return 1.0 if everything is normalized)
				return precomputeSum[cntxIndx/fvNumStates];
			}
		}
	}

	/*NOT same as above, due to log addition (which is precomputed).*/
	double recCondKBLog(long cntxIndxLg){
		int cntxIndx = (int)cntxIndxLg;
		rc.rcCallsCounter++;
		double ret;
		if(fvBlkSz == -1) { //fv in cntx
			ret = data[cntxIndx];
		}
		else { //fv not in cntx (may or may not be instantiated by user)
			int val = inst[fvLocalIndx];
			if(val != -1) { //is instantiated
				ret = data[cntxIndx + (val * fvBlkSz)];
			}
			else { //is not instantiated (precompute so don't have to redo log addition)
				ret = precomputeSum[cntxIndx/fvNumStates];
			}
		}
		if(!flagPreScaleData) {
			return Math.pow(ret, 1/rc.scalar);
		}
		else {return ret;}
	}

	double recCondSAT(long cntxIndxLg){
		int cntxIndx = (int)cntxIndxLg;
		rc.rcCallsCounter++;
		if(fvBlkSz == -1) { //fv in cntx
			return data[cntxIndx];
		}
		else { //fv not in cntx (may or may not be instantiated by user)
			int val = userEvid[fvLocalIndx];
			if(val != -1) { //is instantiated
				return data[cntxIndx + (val * fvBlkSz)];
			}
			else { //is not instantiated (actually should normally return 1.0 if everything is normalized)
				return precomputeSum[cntxIndx/fvNumStates];
			}
		}
	}

	/*NOT same as above, due to log addition (which is precomputed).*/
	double recCondSATLog(long cntxIndxLg){
		int cntxIndx = (int)cntxIndxLg;
		rc.rcCallsCounter++;
		double ret;
		if(fvBlkSz == -1) { //fv in cntx
			ret = data[cntxIndx];
		}
		else { //fv not in cntx (may or may not be instantiated by user)
			int val = inst[fvLocalIndx];
			if(val != -1) { //is instantiated
				ret = data[cntxIndx + (val * fvBlkSz)];
			}
			else { //is not instantiated (precompute so don't have to redo log addition)
				ret = precomputeSum[cntxIndx/fvNumStates];
			}
		}
		if(!flagPreScaleData) {
			return Math.pow(ret, 1/rc.scalar);
		}
		else {return ret;}
	}

	double recCondMPE(long cntxIndxLg){
		int cntxIndx = (int)cntxIndxLg;
		if(fvBlkSz == -1) { //fv in cntx
			return data[cntxIndx];
		}
		else { //fv not in cntx (may or may not be instantiated by user)
			int val = userEvid[fvLocalIndx];
			if(val != -1) { //is instantiated
				return data[cntxIndx + (val * fvBlkSz)];
			}
			else { //is not instantiated, so iterate over it to find max
				double ret = 0;
				for(int i=0; i<fvNumStates; i++) {
					double tmp = data[cntxIndx];
					cntxIndx += fvBlkSz;
					if(tmp>ret) { ret = tmp;}
				}
				return ret;
			}
		}
	}


	public void observe(int varIndx, int value) {
		if(varIndx == fvLocalIndx) {clearAncestorCaches();}
	}
	public void unobserve(int varIndx) {
		if(varIndx == fvLocalIndx) {clearAncestorCaches();}
	}
	public void unobserveAll() {/*clearAncestorCaches();//done in RC*/}
	public void setCPT(int varIndx) {
		if(varIndx == fvLocalIndx) {
			CPTShell oldshell = cptshell;
			cptshell=fvLocal.getCPTShell();
			if(!oldshell.index().equals(cptshell.index())) {
				throw new IllegalStateException("RC2NodeLeaf does not support CPT reindexing.");
			}

			setData();
			precomputeSum();
			clearAncestorCaches();
		}
	}

	public int getFVIndx() {return fvLocalIndx;}
	public FiniteVariable getLeafVar() {return fvLocal;}

	void getElimOrder(ArrayList eo) {
		HashSet vars = new HashSet(cptshell.variables());
		vars.removeAll(context.vars);
		eo.addAll(vars);
	}


}//end class RC2NodeLeaf
