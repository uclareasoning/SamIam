package edu.ucla.belief.rc2.structure;

import java.util.*;

import edu.ucla.belief.FiniteVariable;

import edu.ucla.belief.rc2.kb.sat.KB_SAT;

/**
 * @author David Allen
 */
class RC2ItrSat {

	static final private double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;

	private final RC2 rc;
	private final int[] instantiation; //rc2.instantiation
	private final int map[][];
	private final double scalar;
	protected final KB_SAT kb_sat;

	private final int cutsetLength;
	private final int[] varsIndices; //index into instantiation, always positive
	private final int[] maxState; //stores highest state value, variables indexed same as varsIndices array.

	private final long[] leftBlockSize;
	private final long[] rightBlockSize;

	public long blL = 0; //baseline left (vars in child cntx not in parent cutset)
	public long blR = 0; //baseline right (vars in child cntx not in parent cutset)

	final private RC2Node left;
	final private RC2Node right;


	public RC2ItrSat(RC2 rc, List vars, long lfBlk[], long rtBlk[], RC2Node lf, RC2Node rt) {
		this.rc = rc;
		this.instantiation = rc.instantiation;
		this.scalar = rc.scalar;
		this.map = rc.map_kb.map;
		this.kb_sat = rc.kb_sat;
		this.left = lf;
		this.right = rt;
		leftBlockSize = lfBlk;
		rightBlockSize = rtBlk;

		cutsetLength = vars.size();
		varsIndices = new int[cutsetLength];
		maxState = new int[cutsetLength];

		for(int i=0; i<varsIndices.length; i++) {
			FiniteVariable fv = (FiniteVariable)vars.get(i);
			varsIndices[i] = rc.vars.indexOf(fv);
			maxState[i] = fv.size()-1; //indexed 0..size-1
		}
	}


	double cutsetIterate(int cutIndx) {
		if(cutIndx >= cutsetLength) {//all cutset vars are instantiated CALL RC

			long cntx_l = blL;
			long cntx_r = blR;
			for(int i=varsIndices.length-1; i>=0; i--) {
				int val = instantiation[varsIndices[i]];
				cntx_l += (val * leftBlockSize[i]);
				cntx_r += (val * rightBlockSize[i]);
			}

			double rc_l = left.recCondSAT(cntx_l);
			if(rc_l > 0) {
				double rc_r = right.recCondSAT(cntx_r);
				if(rc_r > 0) { return rc_l * rc_r;} //NORMAL RC
				else { return rc_r;} //backtracking or pr=0
			}
			else { return rc_l;} //backtracking or pr=0 (skip right side)
		}

		int rcIndx = varsIndices[cutIndx];
		if(instantiation[rcIndx]>=0) { //already set (user evid), skip it
			return cutsetIterate(cutIndx+1);
		}


		double sum = 0;
		for(int val=maxState[cutIndx]; val>=0; val--) {

			if(map[rcIndx]==null) { //this var does not appear in the CNF (has no determinism)
				instantiation[rcIndx] = val;
				double ret = cutsetIterate(cutIndx+1);
				if(ret>=0) {sum+=ret;}
				else { instantiation[rcIndx] = -1; return ret;} //backtracking: since this var is not in CNF, it doesn't count as a backtrack level
			}
			else {//var appears in CNF
				int r1 = kb_sat.my_decide(map[rcIndx][val]);

//TODO REMOVE
//System.out.println("my_decide(" + (map[rcIndx][val]) + ")=" + r1);
//END TODO REMOVE

				if(r1 == 2) {//kb already knew about this, so just "skip" over it (no other value would be valid)
					instantiation[rcIndx] = val;
					double ret = cutsetIterate(cutIndx+1);
					instantiation[rcIndx] = -1;
					return ret;
				}
				else if(r1 == 1) {//value set in KB with decide
					instantiation[rcIndx] = val;
					double rcval = cutsetIterate(cutIndx+1);

					if(rcval >= 0) {
						sum+=rcval; //NORMAL RC
						kb_sat.undo_decide();
					}
					else if(rcval < -1) { instantiation[rcIndx]=-1; return rcval+1;}
					else {//rcval == -1 backtrack to this level, undo_decide already called, try again
						val++; //try this level again
					}
				}
				else if(r1 < -1) {//kb just failed on decide with assertion level on previous call, need to backtrack
					instantiation[rcIndx] = -1;
					return r1+1;
				}
				else if(r1 == -1) {//this is the assert_level (undo_decide and assert_cd_literal already called), try again
					val++; //try this level again
				}
				//else if(r1 == 0) {}//no assert_level, but val is invalid (already know -val), continue next value
			}//end if in cnf
		}//end for each value

		instantiation[rcIndx]=-1;
		return sum;
	}

	double cutsetIterateLog(int cutIndx) {
		if(cutIndx >= cutsetLength) {//all cutset vars are instantiated CALL RC

			long cntx_l = blL;
			long cntx_r = blR;
			for(int i=varsIndices.length-1; i>=0; i--) {
				int val = instantiation[varsIndices[i]];
				cntx_l += (val * leftBlockSize[i]);
				cntx_r += (val * rightBlockSize[i]);
			}

			double rc_l = left.recCondSATLog(cntx_l);
			if(rc_l > 0) {
				double rc_r = right.recCondSATLog(cntx_r);
				if(rc_r > 0) { return rc_l * rc_r;} //NORMAL RC
				else { return rc_r;} //backtracking or pr=0
			}
			else { return rc_l;} //backtracking or pr=0 (skip right side)
		}
		else {

			int rcIndx = varsIndices[cutIndx];
			if(instantiation[rcIndx]>=0) { //already set (user evid), skip it
				return cutsetIterateLog(cutIndx+1);
			}


			double s_lna = Double.NEGATIVE_INFINITY;
			for(int val=maxState[cutIndx]; val>=0; val--) {

				if(map[rcIndx]==null) { //this var does not appear in the CNF (has no determinism)
					instantiation[rcIndx] = val;
					double tmp = cutsetIterateLog(cutIndx+1);
					if(tmp>0) {

						{//logsum: (ret+=tmp)
							double s_lnb=rc.scalar*Math.log(tmp);
							if(s_lna==NEGATIVE_INFINITY) { s_lna=s_lnb;} //ret was 0
							else {
								//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is MAX(lnb, lna)//TODO not done for speed
								s_lna += Math.log(1.0 + Math.exp(s_lnb - s_lna));
							}
						}
					}
					else if(tmp<0){ instantiation[rcIndx] = -1; return tmp;} //backtracking: since this var is not in CNF, it doesn't count as a backtrack level
				}
				else {//var appears in CNF
					int r1 = kb_sat.my_decide(map[rcIndx][val]);

	//TODO REMOVE
	//System.out.println("my_decide(" + (map[rcIndx][val]) + ")=" + r1);
	//END TODO REMOVE

					if(r1 == 2) {//kb already knew about this, so just "skip" over it (no other value would be valid)
						instantiation[rcIndx] = val;
						double ret = cutsetIterateLog(cutIndx+1);
						instantiation[rcIndx] = -1;
						return ret;
					}
					else if(r1 == 1) {//value set in KB with decide
						instantiation[rcIndx] = val;
						double tmp = cutsetIterateLog(cutIndx+1);

						if(tmp==0.0) {
							kb_sat.undo_decide();
						}
						else if(tmp>0) {
							kb_sat.undo_decide();

							{//logsum: (ret+=tmp)
								double s_lnb=rc.scalar*Math.log(tmp);
								if(s_lna==NEGATIVE_INFINITY) { s_lna=s_lnb;} //ret was 0
								else {
									//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is MAX(lnb, lna)//TODO not done for speed
									s_lna += Math.log(1.0 + Math.exp(s_lnb - s_lna));
								}
							}
						}
						else if(tmp < -1) { instantiation[rcIndx]=-1; return tmp+1;}
						else {//tmp == -1 backtrack to this level, undo_decide already called, try again
							val++; //try this level again
						}
					}
					else if(r1 < -1) {//kb just failed on decide with assertion level on previous call, need to backtrack
						instantiation[rcIndx] = -1;
						return r1+1;
					}
					else if(r1 == -1) {//this is the assert_level (undo_decide and assert_cd_literal already called), try again
						val++; //try this level again
					}
					//else if(r1 == 0) {}//no assert_level, but val is invalid (already know -val), continue next value
				}//end if in cnf
			}//end for each value

			instantiation[rcIndx]=-1;
			return Math.exp(s_lna/rc.scalar);
		}
	}

}//end class RC2ItrSat
