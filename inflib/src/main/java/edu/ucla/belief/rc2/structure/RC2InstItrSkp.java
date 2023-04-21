package edu.ucla.belief.rc2.structure;

import java.util.*;

import edu.ucla.belief.FiniteVariable;

/** This class represents an Instance Iterator and will only work
 *  if RC does not use a KB and if the user is not interested in
 *  partial derivatives (as this iterator skips states inconsistent
 *  with the evidence on instantiation).
 *
 * @author David Allen
 */
class RC2InstItrSkp implements RC2InstItr {

	/*  This Iterator starts with all variables in their 0 state
	 *  and counts up from right to left.
	 *  000->001->002->003->010->011->012->013->...
	 */


	private final RC2 rc;
	private final int[] instantiation; //rc2.instantiation


	private final int[] varsIndices; //index into instantiation, positive if "this" has set evidence (including initial state),
										//otherwise -index-1 if someone else set value (i.e. user evidence)(need -1 to handle -0).
	private final int[] maxState; //stores highest state value, variables indexed same as varsIndices array.

	private final long[] leftBlockSize; //save to update FlipChange
	private final long[] rightBlockSize;//save to update FlipChange

	private final long[] leftFlipChange; //when flip var at i, all vars j>0 are 0 (adjusted for evidence)
	private final long[] rightFlipChange;//when flip var at i, all vars j>0 are 0 (adjusted for evidence)

	private final int numInstantiations;

	final private RC2NodeInternal nd;


	/** Create an RC2InstItrSkp object.
	 *  @param rc An RC2 object.
	 *  @param vars A list of vars ordered the same as leftBlk and rightBlk.
	 *  @param leftBlk The block sizes of vars in the left child's context (used directly, not copied).
	 *  @param rightBlk The block sizes of vars in the right child's context (used directly, not copied).
	 */
	public RC2InstItrSkp(RC2 rc, List vars, long leftBlk[], long rightBlk[], RC2NodeInternal nd) {
		this.rc = rc;
		this.instantiation = rc.instantiation;
		this.nd = nd;

		varsIndices = new int[vars.size()];
		maxState = new int[vars.size()];
		leftBlockSize = leftBlk;
		rightBlockSize = rightBlk;
		leftFlipChange = new long[varsIndices.length];
		rightFlipChange = new long[varsIndices.length];

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

		recalculateFlipChanges();
	}

	/**User is responsible for calling this at some point after new
	 * evidence has been set on rc.inst and before any computations
	 * are run.
	 */
	public void evidenceChanged() { recalculateFlipChanges();}
	long[] leftFlipChange() { return leftFlipChange;}
	long[] rightFlipChange() { return rightFlipChange;}

	/*This needs to be called anytime the evidence changes and before
	 * a computation is started.*/
	private void recalculateFlipChanges() {
		long resetL = 0;
		long resetR = 0;
		long evidBaselineL = 0;
		long evidBaselineR = 0;
		for(int i=varsIndices.length-1; i>=0; i--) {

			int rcIndx = varsIndices[i]; //positive if "this" set any evidence (including initial state)
			int rcIndxPos = (rcIndx>=0?rcIndx:-rcIndx-1); //could possibly have evidence or not

			//Update varIndices as positive (no user evid) or negative (has user evid)
			if(rc.userEvid[rcIndxPos]!=-1) { //has user evid, "this" will skip it
				varsIndices[i] = -rcIndxPos-1;

				int val = instantiation[rcIndxPos];
				evidBaselineL += val * leftBlockSize[i];
				evidBaselineR += val * rightBlockSize[i];
				leftFlipChange[i] = 0;
				rightFlipChange[i] = 0;
			}
			else { //no user evid, "this" will iterate over it
				varsIndices[i] = rcIndxPos;

				//when increase a variable by one, increase index by block size
				//  and reduce it since all vars to the right are now 0
				leftFlipChange[i] = leftBlockSize[i] - resetL;
				resetL += leftBlockSize[i] * maxState[i];

				rightFlipChange[i] = rightBlockSize[i] - resetR;
				resetR += rightBlockSize[i] * maxState[i];
			}
		}
		nd.setEvidBaseline(evidBaselineL, evidBaselineR);
	}



	/*
	 *  <p>All variables should already be set to something (either by this
	 *     iterator or initial state or by user evid else).
	 */
	public int next() {
		for(int i=varsIndices.length-1; i>=0; i--) {
			int rcIndx = varsIndices[i];

			if(rcIndx >= 0) { //if has evid set by "this"
				if(instantiation[rcIndx] == maxState[i]) {
					instantiation[rcIndx] = 0;
				}
				else {
					instantiation[rcIndx]++;
					return i;
				}
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
			int rcIndx = varsIndices[i];
			int rcIndxPos = (rcIndx>=0 ? rcIndx : (-rcIndx-1));
			ret.add(rc.vars.get(rcIndxPos));
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
			int rcIndxPos = (rcIndx>=0 ? rcIndx : (-rcIndx-1));
			ret += instantiation[rcIndxPos] * stSp;
			stSp *= (maxState[i]+1);
		}
		return ret;
	}

}//end class RC2InstItrSkp
