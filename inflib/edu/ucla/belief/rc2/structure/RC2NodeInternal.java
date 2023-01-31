package edu.ucla.belief.rc2.structure;

import java.util.*;
import java.math.BigInteger;

import edu.ucla.belief.TableIndex;


/** This class represents Internal RCNode objects.
 *
 * @author David Allen
 */
public class RC2NodeInternal extends RC2Node {

	static final private double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;

	final public RC2Node left;
	final public RC2Node right;

	private RC2InstItr cutsetItr1 = null;
	private RC2ItrSat cutsetItrSat = null;

	private double cache[] = null; //currently caches both log and non-log values, could possibly cause problems with dgraphs

	private ChildCntxUpdater leftCntxOffset = null;
	private ChildCntxUpdater rightCntxOffset = null;

	private long[] leftFC;
	private long[] rightFC;

	private long evidBaselineL=0; //only used by RC2InstItrSkp
	private long evidBaselineR=0; //only used by RC2InstItrSkp

	private TableIndex tblIndx = null;

	final private double scalar;
	final protected boolean flagCallLog;  //(should never have a non-LOG function called on it)


	public RC2NodeInternal(int id, RC2 rc, RC2Node c1, RC2Node c2, Map minVal) {
		super(id, rc, (Math.max(c1.height,c2.height)+1), union(c1.vars, c2.vars));

		if(c1.height > c2.height) { //do this so that zeros are more likely on left child
			left = c2;
			right = c1;
		}
		else {
			left = c1;
			right = c2;
		}


		if(left==null || right==null) { throw new IllegalArgumentException("Illegal Child Node.");}
		if(id<=left.nodeID || id<=right.nodeID) { throw new IllegalArgumentException("Node ID of parent not larger than that of children: " + id + " " + left.nodeID + " " + right.nodeID);}
		if(left.rc != right.rc) { throw new IllegalArgumentException("All nodes must use the same RC2 object.");}
		if(left == right) { throw new IllegalArgumentException("Children must be distint.");}
		if(left.rc != rc) { throw new IllegalArgumentException("All nodes must use the same RC2 object.");}
		left.parentNodes.add(this);
		right.parentNodes.add(this);
		scalar = rc.scalar;
		{
			if(left instanceof RC2NodeInternal && ((RC2NodeInternal)left).flagCallLog) { flagCallLog = true;}
			else if(right instanceof RC2NodeInternal && ((RC2NodeInternal)right).flagCallLog) { flagCallLog = true;}
			else {
				Double lfMin = (Double)minVal.get(left);
				Double rtMin = (Double)minVal.get(right);
				double lfm = lfMin.doubleValue();
				double rtm = rtMin.doubleValue();
				double thism = lfm * rtm;
				if(thism == 0.0) { flagCallLog = true; if(scalar==1.0) {System.out.println("\n\n\nWARNING: This dtree could possibly underflow.\n\n\n");}}
				else {
					flagCallLog = false;
					minVal.put( this, new Double(thism));
				}
			}
		}
	}//end constructor

	public void initialize(Collection acutset) {
		final boolean firstCall = (context==null);
		if(firstCall) {

			//create context
			Collection cntx = new HashSet(vars);
			cntx.retainAll(acutset);
			super.initializeNode(new RC2Index(rc,cntx));

			//create cutset
			ArrayList cutset = new ArrayList(RC2Utils.calculateCutsetFromChildren(this, acutset));

			//initialize children
			{
				Set newAcutset = new HashSet(acutset.size()+cutset.size());
				newAcutset.addAll(acutset);
				newAcutset.addAll(cutset);
				newAcutset = Collections.unmodifiableSet(newAcutset);
				left.initialize(newAcutset);
				right.initialize(newAcutset);
			}

			//create offsets for iteration speedup
			//this needs children initialized
			leftCntxOffset = new ChildCntxUpdater(left, cutset);
			rightCntxOffset = new ChildCntxUpdater(right, cutset);

			//create cutset iterator
			if(rc.rcFlags.allowParDeriv) {
				cutsetItr1 = new RC2InstItrAll(rc,cutset,leftCntxOffset.cutBlockSize,rightCntxOffset.cutBlockSize);
				leftFC = ((RC2InstItrAll)cutsetItr1).leftFlipChange();
				rightFC = ((RC2InstItrAll)cutsetItr1).rightFlipChange();
			}
			else if(rc.rcFlags.allowKBinst) {
				cutsetItr1 = new RC2InstItrSkpKB(rc,cutset,leftCntxOffset.cutBlockSize,rightCntxOffset.cutBlockSize);
				leftFC = null;
				rightFC = null;
			}
			else {
				cutsetItr1 = new RC2InstItrSkp(rc,cutset,leftCntxOffset.cutBlockSize,rightCntxOffset.cutBlockSize, this);
				leftFC = ((RC2InstItrSkp)cutsetItr1).leftFlipChange();
				rightFC = ((RC2InstItrSkp)cutsetItr1).rightFlipChange();
			}

			if(rc.rcFlags.allowKBsat) {
				cutsetItrSat = new RC2ItrSat(rc, cutset, leftCntxOffset.cutBlockSize, rightCntxOffset.cutBlockSize, left, right);
			}
			else {
				cutsetItrSat = null;
			}
		}
	}

	public String toString() { return "[node:" + nodeID + "->" + left.nodeID + "," + right.nodeID + "]";}
    public boolean isLeaf() { return false;}

	void clearLocalCacheAndReset(){
		if(cache != null) {
			Arrays.fill(cache, -1);
		}
	}

	void setCaching(boolean cached) {
		if(cached) {
			if(cache==null) {
				BigInteger size = context.totalStateSpace();
				if(size.compareTo(maxInt)>0) { throw new IllegalStateException();}
				cache = new double[size.intValue()];
				Arrays.fill(cache, -1);
			}
		}
		else {
			cache = null;
		}
	}
	boolean getCaching() {return cache!=null;}

	void setEvidBaseline( long l, long r) { evidBaselineL = l; evidBaselineR = r;}

	double recCondAll(long cntxIndxLg) {
		rc.rcCallsCounter++;
		int cntxIndx = (int)cntxIndxLg;
		if(cache!=null && cache[cntxIndx]>=0) {
			return cache[cntxIndx];
		}
		else {
			long leftIndx = leftCntxOffset.computeBaseline();
			long rightIndx = rightCntxOffset.computeBaseline();

			int v;
			double ret = left.recCondAll(leftIndx);
			if(ret!=0.0) ret *= right.recCondAll(rightIndx);

			while((v=cutsetItr1.next())!=-1) {
				leftIndx += leftFC[v];
				rightIndx += rightFC[v];
				double l = left.recCondAll(leftIndx);
				if(l!=0.0) {
					l *= right.recCondAll(rightIndx);
					ret += l;
				}
			}

			if(cache!=null) {cache[cntxIndx]=ret;}
			return ret;
		}
	}

	double recCondAllLog(long cntxIndxLg) {
		rc.rcCallsCounter++;
		int cntxIndx = (int)cntxIndxLg;
		if(cache!=null && cache[cntxIndx]>=0) {
			return cache[cntxIndx];
		}
		else {
			long leftIndx = leftCntxOffset.computeBaseline();
			long rightIndx = rightCntxOffset.computeBaseline();

			double ret;
			int v;
			if(flagCallLog) {
				double s_lna;
				double l = left.recCondAllLog(leftIndx);
				if(l!=0.0) {
					l *= right.recCondAllLog(rightIndx);
					s_lna = scalar*Math.log(l);//s*ln(ret)
				}
				else s_lna=NEGATIVE_INFINITY;

				while((v=cutsetItr1.next())!=-1) {
					leftIndx += leftFC[v];
					rightIndx += rightFC[v];

					double tmp = left.recCondAllLog(leftIndx);
					if(tmp!=0.0) tmp *= right.recCondAllLog(rightIndx);

					if(tmp!=0.0) {//logsum: (ret+=tmp)
						double s_lnb=scalar*Math.log(tmp);
						if(s_lna==NEGATIVE_INFINITY) { s_lna=s_lnb;} //ret was 0
						else {
							//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is MAX(lnb, lna)//TODO not done for speed
							s_lna += Math.log(1.0 + Math.exp(s_lnb - s_lna));
						}
					}
				}
				ret = Math.exp(s_lna/scalar);
			}
			else {
				ret = left.recCondAll(leftIndx);
				if(ret!=0.0) ret *= right.recCondAll(rightIndx);
				while((v=cutsetItr1.next())!=-1) {
					leftIndx += leftFC[v];
					rightIndx += rightFC[v];
					double tmp = left.recCondAll(leftIndx);
					if(tmp!=0.0) {
						tmp *= right.recCondAll(rightIndx);
						ret += tmp;
					}
				}
				ret = Math.pow(ret, 1/scalar);
			}

			if(cache!=null) {cache[cntxIndx]=ret;}
			return ret;
		}
	}

	double recCondSkp(long cntxIndxLg) {
		rc.rcCallsCounter++;
		int cntxIndx = (int)cntxIndxLg;
		if(cache!=null && cache[cntxIndx]>=0) {
			return cache[cntxIndx];
		}
		else {
			long leftIndx = leftCntxOffset.computeBaseline() + evidBaselineL;
			long rightIndx = rightCntxOffset.computeBaseline() + evidBaselineR;

			int v;
			double ret = left.recCondSkp(leftIndx);
			if(ret!=0.0) ret *= right.recCondSkp(rightIndx);
			while((v=cutsetItr1.next())!=-1) {
				leftIndx += leftFC[v];
				rightIndx += rightFC[v];
				double tmp = left.recCondSkp(leftIndx);
				if(tmp!=0.0) {
					tmp *= right.recCondSkp(rightIndx);
					ret += tmp;
				}
			}

			if(cache!=null) {cache[cntxIndx]=ret;}
			return ret;
		}
	}

	double recCondSkpLog(long cntxIndxLg) {
		rc.rcCallsCounter++;
		int cntxIndx = (int)cntxIndxLg;
		if(cache!=null && cache[cntxIndx]>=0) {
			return cache[cntxIndx];
		}
		else {
			long leftIndx = leftCntxOffset.computeBaseline() + evidBaselineL;
			long rightIndx = rightCntxOffset.computeBaseline() + evidBaselineR;

			double ret;
			int v;
			if(flagCallLog) {
				double s_lna;
				double l = left.recCondSkpLog(leftIndx);
				if(l!=0.0) {
					l *= right.recCondSkpLog(rightIndx);
					s_lna = scalar*Math.log(l);
				}
				else s_lna = NEGATIVE_INFINITY;

				while((v=cutsetItr1.next())!=-1) {
					leftIndx += leftFC[v];
					rightIndx += rightFC[v];

					double tmp = left.recCondSkpLog(leftIndx);
					if(tmp!=0.0) tmp *= right.recCondSkpLog(rightIndx);

					if(tmp!=0) {//logsum: (ret+=tmp)
						double s_lnb=scalar*Math.log(tmp);
						if(s_lna==NEGATIVE_INFINITY) { s_lna=s_lnb;} //ret was 0
						else {
							//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is MAX(lnb, lna)//TODO not done for speed
							s_lna += Math.log(1.0 + Math.exp(s_lnb - s_lna));
						}
					}
				}
				ret = Math.exp(s_lna/scalar);
			}
			else {
				ret = left.recCondSkp(leftIndx);
				if(ret!=0.0) ret *= right.recCondSkp(rightIndx);
				while((v=cutsetItr1.next())!=-1) {
					leftIndx += leftFC[v];
					rightIndx += rightFC[v];
					double tmp = left.recCondSkp(leftIndx);
					if(tmp!=0.0) {
						tmp *= right.recCondSkp(rightIndx);
						ret += tmp;
					}
				}
				ret = Math.pow(ret, 1/scalar);
			}

			if(cache!=null) {cache[cntxIndx]=ret;}
			return ret;
		}
	}

	double recCondKB(long cntxIndxLg) {
		rc.rcCallsCounter++;
		int cntxIndx = (int)cntxIndxLg;
		if(cache!=null && cache[cntxIndx]>=0) {
			return cache[cntxIndx];
		}
		else {
			double ret = 0.0;

			if(((RC2InstItrSkpKB)cutsetItr1).setInitialState()) {

				long blL = leftCntxOffset.computeBaseline();
				long blR = rightCntxOffset.computeBaseline();

				while(true) {
					double tmp = left.recCondKB(blL+((RC2InstItrSkpKB)cutsetItr1).cutIndxL);
					if(tmp!=0.0) {
						tmp *= right.recCondKB(blR+((RC2InstItrSkpKB)cutsetItr1).cutIndxR);
						ret += tmp;
					}
					int v = cutsetItr1.next();
					if(v==-1){break;}
				}
			}

			if(cache!=null) {cache[cntxIndx]=ret;}
			return ret;
		}
	}

	/*For speed purposes, this has an inline log sum function, see RC2Utils
	 * for a more versatile log sum function.
	 */
	double recCondKBLog(long cntxIndxLg) {
		rc.rcCallsCounter++;
		int cntxIndx = (int)cntxIndxLg;
		if(cache!=null && cache[cntxIndx]>=0) {
			return cache[cntxIndx];
		}
		else {
			double ret = 0.0;
			int v;

			if(flagCallLog) {
				if(((RC2InstItrSkpKB)cutsetItr1).setInitialState()) {

					long blL = leftCntxOffset.computeBaseline();
					long blR = rightCntxOffset.computeBaseline();

					double s_lna;
					double l = left.recCondKBLog(blL+((RC2InstItrSkpKB)cutsetItr1).cutIndxL);
					if(l!=0.0) {
						l *= right.recCondKBLog(blR+((RC2InstItrSkpKB)cutsetItr1).cutIndxR);
						s_lna = scalar*Math.log(l);
					}
					else s_lna = NEGATIVE_INFINITY;

					while((v=cutsetItr1.next())!=-1) {
						double tmp = left.recCondKBLog(blL+((RC2InstItrSkpKB)cutsetItr1).cutIndxL);
						if(tmp!=0.0) tmp *= right.recCondKBLog(blR+((RC2InstItrSkpKB)cutsetItr1).cutIndxR);

						if(tmp!=0) {//logsum: (ret+=tmp)
							double s_lnb=scalar*Math.log(tmp);
							if(s_lna==NEGATIVE_INFINITY) { s_lna=s_lnb;} //ret was 0
							else {
								//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is MAX(lnb, lna)//TODO not done for speed
								s_lna += Math.log(1.0 + Math.exp(s_lnb - s_lna));
							}
						}
					}
					ret = Math.exp(s_lna/scalar);
				}
			}
			else {
				if(((RC2InstItrSkpKB)cutsetItr1).setInitialState()) {

					long blL = leftCntxOffset.computeBaseline();
					long blR = rightCntxOffset.computeBaseline();

					while(true) {
						double tmp = left.recCondKB(blL+((RC2InstItrSkpKB)cutsetItr1).cutIndxL);
						if(tmp!=0.0) {
							tmp *= right.recCondKB(blR+((RC2InstItrSkpKB)cutsetItr1).cutIndxR);
							ret += tmp;
						}
						v = cutsetItr1.next();
						if(v==-1){break;}
					}
					ret = Math.pow(ret, 1/scalar);
				}
			}

			if(cache!=null) {cache[cntxIndx]=ret;}
			return ret;
		}
	}

	double recCondSAT(long cntxIndxLg) {
		rc.rcCallsCounter++;
		int cntxIndx = (int)cntxIndxLg;
		if(cache!=null && cache[cntxIndx]>=0) {
			return cache[cntxIndx];
		}
		else {
			cutsetItrSat.blL = leftCntxOffset.computeBaseline();
			cutsetItrSat.blR = rightCntxOffset.computeBaseline();
			double ret = cutsetItrSat.cutsetIterate(0);
			if(cache!=null && ret>=0) {cache[cntxIndx]=ret;} //don't store backtrack mode
			return ret;
		}
	}

	double recCondSATLog(long cntxIndxLg) {
		rc.rcCallsCounter++;
		int cntxIndx = (int)cntxIndxLg;
		if(cache!=null && cache[cntxIndx]>=0) {
			return cache[cntxIndx];
		}
		else {
			cutsetItrSat.blL = leftCntxOffset.computeBaseline();
			cutsetItrSat.blR = rightCntxOffset.computeBaseline();
			double ret;
			if(flagCallLog) {
				ret = cutsetItrSat.cutsetIterateLog(0);
			}
			else {
				ret = cutsetItrSat.cutsetIterate(0);
				if(ret>0) {ret = Math.pow(ret, 1/scalar);}
			}

			if(cache!=null && ret>=0) {cache[cntxIndx]=ret;} //don't store backtrack mode
			return ret;
		}
	}

	double recCondMPE(long cntxIndxLg) {
		int cntxIndx = (int)cntxIndxLg;
		if(cache!=null && cache[cntxIndx]>=0) {
			return cache[cntxIndx];
		}
		else {
			long leftIndx = leftCntxOffset.computeBaseline() + evidBaselineL; //works with itrSkp or itrAll
			long rightIndx = rightCntxOffset.computeBaseline() + evidBaselineR;
			double ret = 0.0;

			while(true) {
				ret += left.recCondMPE(leftIndx) * right.recCondMPE(rightIndx);
				int v = cutsetItr1.next();
				if(v==-1){break;}
				leftIndx += leftFC[v];
				rightIndx += rightFC[v];
			}

			if(cache!=null) {cache[cntxIndx]=ret;}
			return ret;
		}
	}

    double[] recCondCutMar() {
		double ret[] = new double[(int)cutsetItr1.numInstantiations()];

		long leftIndx = leftCntxOffset.computeBaseline() + evidBaselineL; //works with itrSkp or itrAll
		long rightIndx = rightCntxOffset.computeBaseline() + evidBaselineR;

		while(true) {
			if(rc.rcFlags.allowParDeriv) {
				ret[cutsetItr1.getIndx()] = left.recCondAll(leftIndx) * right.recCondAll(rightIndx);
			}
			else {
				ret[cutsetItr1.getIndx()] = left.recCondSkp(leftIndx) * right.recCondSkp(rightIndx);
			}
			int v = cutsetItr1.next();
			if(v==-1){break;}
			leftIndx += leftFC[v];
			rightIndx += rightFC[v];
		}

		return ret;
	}

	TableIndex getCutsetIndx() {
		if(tblIndx == null) {
			tblIndx = new TableIndex((ArrayList)cutsetItr1.getVars(new ArrayList()));
		}
		return tblIndx;
	}
	public long numCutsetInstantiations() {
		return cutsetItr1.numInstantiations();
	}
	public Collection getCutsetVars(Collection ret) {return cutsetItr1.getVars(ret);}
	RC2InstItr getCutsetIterator() {
		return cutsetItr1;
	}


	public int actualMemoryAllocated() {
		if(cache == null) { return 0;}
		else { return cache.length;}
	}

	static private final HashSet union(Collection l, Collection r) {
		HashSet ret = new HashSet(Math.max(l.size(), r.size()));
		ret.addAll(l);
		ret.addAll(r);
		return ret;
	}

	void getElimOrder(ArrayList eo) {
		left.getElimOrder(eo);
		right.getElimOrder(eo);
		eo.addAll(getCutsetVars(null));
	}



	static private class ChildCntxUpdater {

		final long cutBlockSize[];
		final private long fixedBlockSize[];
		final private int fixedVarIndx[];
		final private int instantiation[];

		public ChildCntxUpdater(RC2Node child, List cutset) {
			RC2 rc = child.rc;
			instantiation = rc.instantiation;

			//calculate vars in child.context - cutset (those are fixed)
			HashSet fixedVars = new HashSet( child.context.vars);
			fixedVars.removeAll(cutset);
			//create fixedVarIndx and fixedBlockSize
			fixedBlockSize = new long[fixedVars.size()];
			fixedVarIndx = new int[fixedVars.size()];
			int nxt=0;
			for(Iterator itr=fixedVars.iterator(); itr.hasNext();) {
				Object fv = itr.next();
				fixedVarIndx[nxt] = rc.vars.indexOf(fv);
				fixedBlockSize[nxt] = child.context.blockSizeOfVar(fixedVarIndx[nxt]);
				nxt++;
			}
			//create cutBlockSize
			cutBlockSize = new long[cutset.size()];
			for(int i=0; i<cutBlockSize.length; i++) {
				cutBlockSize[i] = child.context.blockSizeOfVar(rc.vars.indexOf(cutset.get(i)));
			}
		}

		final private long computeBaseline() {
			long ret = 0;
			for(int i=0; i<fixedVarIndx.length; i++) {
				ret += (instantiation[fixedVarIndx[i]]*fixedBlockSize[i]);
			}
			return ret;
		}
	}//end class ChildCntxUpdater

}//end class RC2NodeInternal
