package edu.ucla.belief.rc2.structure;

import java.util.*;

import edu.ucla.belief.FiniteVariable;

/** This class represents an Instance Iterator and will work with or
 *  without a KB and if the user is not interested in partial
 *  derivatives (as this iterator skips states inconsistent with
 *  the evidence on instantiation).  TODO: With KB turned off it will
 *  still probably run slower than RC2InstItrSkp but time it.
 *
 * @author David Allen
 */
class RC2InstItrSkpKB implements RC2InstItr {

	/*  This Iterator starts will all variables in their -1 state
	 *  and counts down from left to right.
	 *  -1-1-1->222->221->220->212->211->210->...
	 */


	private final RC2 rc;
	private final int[] instantiation; //rc2.instantiation


	private final int[] varsIndices; //index into instantiation, always positive
	private final boolean[] setByThis;
	private final int[] maxState; //stores highest state value, variables indexed same as varsIndices array.

	private final long[] leftBlockSize; //save to update FlipChange
	private final long[] rightBlockSize;//save to update FlipChange

	private final int numInstantiations;

	private final RC2.RC2KB rc2KB;

	public long cutIndxL;
	public long cutIndxR;

	/** Create an RC2InstItrSkpKB object.
	 *  @param rc An RC2 object.
	 *  @param vars A list of vars ordered the same as leftBlk and rightBlk.
	 *  @param leftBlk The block sizes of vars in the left child's context (used directly, not copied).
	 *  @param rightBlk The block sizes of vars in the right child's context (used directly, not copied).
	 */
	public RC2InstItrSkpKB(RC2 rc, List vars, long leftBlk[], long rightBlk[]) {
		this.rc = rc;
		this.instantiation = rc.instantiation;
		rc2KB = rc.rcKB;

		varsIndices = new int[vars.size()];
		setByThis = new boolean[vars.size()];
		Arrays.fill(setByThis, false);
		maxState = new int[vars.size()];
		leftBlockSize = leftBlk;
		rightBlockSize = rightBlk;

		long numInstLoc = 1;

		for(int i=0; i<varsIndices.length; i++) {
			FiniteVariable fv = (FiniteVariable)vars.get(i);
			varsIndices[i] = rc.vars.indexOf(fv);
			maxState[i] = fv.size()-1; //indexed 0..size-1

			numInstLoc *= (maxState[i]+1);
			if(numInstLoc<=0 || numInstLoc>Integer.MAX_VALUE) {
				throw new IllegalStateException("ERROR: Number of cutset instantiations > Integer.MAX_VALUE.");
			}
		}

		numInstantiations = (int)numInstLoc;
	}


	/*It would be nice to not recalculate this for user evidence variables, however they
	 * cannot be distinguished from KB evidence currently.
	 */
	final private void calcCutset() {
		cutIndxL = 0;
		cutIndxR = 0;
		for(int i=varsIndices.length-1; i>=0; i--) {
			int val = instantiation[varsIndices[i]];
			cutIndxL += val*leftBlockSize[i];
			cutIndxR += val*rightBlockSize[i];
		}
	}


	boolean setInitialState() {
		for(int i=0; i<varsIndices.length; i++) {
			if(!findValidStateAllowChangeLeft(i)) {
				return false;
			}
		}
		calcCutset();
		return true;
	}


	/** Return true if found a state for this variable that worked.*/
	private boolean findValidState(int cutsetIndx, int thisOrLower) {
		boolean done = false;
		while(!done && thisOrLower>=0) {
			done = rc2KB.setInst(varsIndices[cutsetIndx], thisOrLower);
			thisOrLower--;
		}
		if(done) {setByThis[cutsetIndx] = true;}
		return done;
	}

	//Assumes all to the left have had some evidence set on them (either here or elsewhere)...
	private boolean findValidStateAllowChangeLeft(int cutsetIndx) {
		int cutIndx = cutsetIndx;
		int valueToTry = maxState[cutIndx];

		while(true) {
			if(instantiation[varsIndices[cutIndx]]<0) { //this should update since no user evid or kb evid
				boolean ok = findValidState(cutIndx, valueToTry);

				if(ok) {
					cutIndx++;
					if(cutIndx>cutsetIndx) { break;}
					valueToTry = maxState[cutIndx];
				}
				else {//need to undo last set value & try another
					boolean foundAnother = false;
					while(!foundAnother) {
						cutIndx--;
						if(cutIndx < 0) { return false;}

						if(setByThis[cutIndx]) {
							int rcIndx = varsIndices[cutIndx];
							valueToTry = instantiation[rcIndx]-1;
							rc2KB.setInst(rcIndx, -1); //retract from KB
							setByThis[cutIndx] = false;
							if(valueToTry>=0 && findValidState(cutIndx, valueToTry)) {
								foundAnother = true;
								cutIndx++;
								valueToTry = maxState[cutIndx];
							}
						}//end if set by this
					}//end need to find another
				}
			}
			else { //user evid or kb evid, skip this one
				cutIndx++;
				if(cutIndx>cutsetIndx) { break;}
				valueToTry = maxState[cutIndx];
			}
		}
		return true;
	}


	public int next() {
		int cutIndx = varsIndices.length-1;
		int val;

		if(cutIndx < 0) { return -1;} //no more states

		while(true) { //seek left until find variable to change

			if(setByThis[cutIndx]) {
				int rcIndx = varsIndices[cutIndx];
				val = instantiation[rcIndx];

				//clear this evid
				rc2KB.setInst(rcIndx, -1);
				setByThis[cutIndx] = false;

				if(val>0 && findValidState(cutIndx, val-1)) {
					//reduced this var by at least 1
					break;
				}
				else { //couldn't find new state
					cutIndx--;
				}
			}
			else {
				cutIndx--;
			}
			if(cutIndx < 0) { return -1;} //no more states
		}//end whild find variable to reduce

		{//have reduced a variable by one, set all other cutIndx > current
			for(int i=cutIndx+1; i<varsIndices.length; i++) {
				int rcIndx = varsIndices[i];
				if(instantiation[rcIndx] < 0) { //uninstantiated vars
					if(!findValidStateAllowChangeLeft(i)) {
						return -1;
					}
				}
			}
		}
		calcCutset();
		return 1;
	}


	public long numInstantiations() {return numInstantiations;}

	/**If ret is an ordered collection, then the order the nodes
	 *  are added corresponds with passing this result into a
	 *  table index and calling getIndx.
	 */
	public Collection getVars(Collection ret) {
		if(ret == null) { ret = new HashSet(varsIndices.length);}

		//order them so that they match TableIndex
		for(int i=varsIndices.length-1; i>=0; i--) {
			ret.add(rc.vars.get(varsIndices[i]));
		}
		return ret;
	}

	/**The variables are indexed to match up with getVars.
	 */
	public int getIndx() {
		int ret = 0;
		int stSp = 1;
		for(int i=0; i<varsIndices.length; i++) {
			int rcIndx = varsIndices[i];
			ret += instantiation[rcIndx] * stSp;
			stSp *= (maxState[i]+1);
		}
		return ret;
	}

}//end class RC2InstItrSkpKB
