package edu.ucla.belief.rc2.structure;

import java.util.*;

import edu.ucla.belief.FiniteVariable;

/** This class represents an Instance Iterator and will only work
 *  if RC does not use a KB and if evidence is not being placed
 *  on rc.instantiation, other than by cutsets (currently only if
 *  rc has partial derivatives turned on).
 *
 * @author David Allen
 */
class RC2InstItrAll implements RC2InstItr {

	/*  This Iterator starts with all variables in state 0
	 *  and counts up from left to right.
	 *  000->100->200->300->010->110->210->310->...
	 */


	private final RC2 rc;
	private final int[] instantiation; //rc2.instantiation


	private final int[] varsIndices; //index into instantiation, always positive
	private final int[] maxState; //stores highest state value, variables indexed same as varsIndices array.

	private final long[] leftFlipChange; //when flip var at i, all vars j<i are 0
	private final long[] rightFlipChange;//when flip var at i, all vars j<i are 0

	private final int numInstantiations;

	/** Create an RC2InstItrAll object.
	 *  @param rc An RC2 object.
	 *  @param vars A list of vars ordered the same as leftBlk and rightBlk.
	 *  @param leftBlk The block sizes of vars in the left child's context (used directly, not copied).
	 *  @param rightBlk The block sizes of vars in the right child's context (used directly, not copied).
	 */
	public RC2InstItrAll(RC2 rc, List vars, long leftBlk[], long rightBlk[]) {
		this.rc = rc;
		this.instantiation = rc.instantiation;

		varsIndices = new int[vars.size()];
		maxState = new int[varsIndices.length];
		leftFlipChange = new long[varsIndices.length];
		rightFlipChange = new long[varsIndices.length];

		long resetL = 0;
		long resetR = 0;

		long numInstLoc = 1;

		for(int i=0; i<varsIndices.length; i++) {
			FiniteVariable fv = (FiniteVariable)vars.get(i);
			varsIndices[i] = rc.vars.indexOf(fv);
			maxState[i] = fv.size()-1; //indexed 0..size-1

			numInstLoc *= (maxState[i]+1);
			if(numInstLoc<=0 || numInstLoc>Integer.MAX_VALUE) {
				throw new IllegalStateException("ERROR: Number of cutset instantiations > Integer.MAX_VALUE.");
			}


			//when increase a variable by one, increase index by block size
			//  and reduce it since all vars to the left are now 0
			leftFlipChange[i] = leftBlk[i]-resetL;
			resetL += leftBlk[i] * maxState[i];

			rightFlipChange[i] = rightBlk[i]-resetR;
			resetR += rightBlk[i] * maxState[i];
		}
		numInstantiations = (int)numInstLoc;
	}

	long[] leftFlipChange() { return leftFlipChange;}
	long[] rightFlipChange() { return rightFlipChange;}


	/*
	 *  <p>All variables should already be set by this iterator (at least be in base case of [000...].
	 */
	public int next() {
		for(int i=0; i<varsIndices.length; i++) {
			int rcIndx = varsIndices[i];

			if(instantiation[rcIndx]==maxState[i]) {
				instantiation[rcIndx]=0;
			}
			else {
				instantiation[rcIndx]++;
				return i;
			}
		}

		return -1;
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

}//end class RC2InstItrAll
