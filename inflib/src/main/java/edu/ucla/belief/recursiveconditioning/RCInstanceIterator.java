package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.io.*;
import edu.ucla.util.*;
import edu.ucla.belief.*;

/** This class represents an Instance Iterator and will only work if RC does not use a KB.
 *
 * @author David Allen
 */
public class RCInstanceIterator {

	/** Real evidence should not be set on inst during an iteration (only between them).*/
	protected final RC rc; //be careful when using RC object, as can use different instantiation arrays.
	protected final int[] varsIndices; //index into instantiation, -index-1 if "this" set current value (need -1 to handle -0).
	protected final int[] maxState; //stores highest state value, variables indexed same as varsIndices array.
	protected final int[] instantiation;

	protected int varIndx = -1;


	public RCInstanceIterator( RC rc, Collection vars) {
		this( rc, vars, rc.instantiation);
	}

	/** Normally the constructor (RC, Collection)  is used, but sometimes a non-standard
	 *  instantiation array is desired.
	 */
	public RCInstanceIterator( RC rc, Collection vars, int instantiation[]) {
		this.rc = rc;
		this.instantiation = instantiation;
		varsIndices = new int[vars.size()];
		maxState = new int[vars.size()];

		int i=0;
		for( Iterator itr_v = vars.iterator(); itr_v.hasNext();) {
			FiniteVariable fv = (FiniteVariable)itr_v.next();
			varsIndices[i] = rc.vars.indexOf( fv);
			maxState[i] = fv.size()-1; //indexed 0..size-1
			i++;
		}
	}


	protected final int getInstantiationIndex( int vIndex) {
		if( varsIndices[vIndex] < 0) {
			return -varsIndices[vIndex]-1;
		}
		else {
			return varsIndices[vIndex];
		}
	}

	protected void clearAllSetByItr() {
		for( int i=varsIndices.length-1; i>=0; i--) {
			if( varsIndices[i] < 0) { //since this isn't necessarily called in order, someone else may have reset it
				varsIndices[i] = -varsIndices[i]-1;
				instantiation[ varsIndices[i]] = -1;
			}
		}
	}


	protected boolean setInitialState() {
		//set varIndx & states of variables
		for( int i=0; i<varsIndices.length; i++) {
			int indx = getInstantiationIndex(i);
			if( instantiation[indx] < 0) { //no evidence currently on variable so do it here
				instantiation[indx] = maxState[i];
				varsIndices[i] = -indx-1;
			}
		}
		varIndx = varsIndices.length-1; //set this to the far right & let it find the next iterator state
		return true;
	}


	/** Will set the next state and return true if one is found.  If no next state exists,
	 *   it will return false.
	 *  <p>All variables to the left of varIndx should already be set to something (either by this
	 *     iterator or by somewhere else).
	 */
	protected boolean next() {
		if( varIndx < 0) {
			return false;
		}
		int val;

		//move left until can reduce a state by one
		while( true) {
			int rcIndx = varsIndices[varIndx]; //could be positive or negative

			if( rcIndx > -1) { //if has evid & not by "this", then skip it
				varIndx--;
			}
			else { //has evid set by "this"
				int rcIndxPos = -varsIndices[varIndx]-1;
				val = instantiation[rcIndxPos];

				if( val == 0) { //then at its lowest state, clear it and try next variable
					varsIndices[varIndx] = -rcIndx-1;
					instantiation[rcIndxPos] = -1;
					varIndx--;
				}
				else {
					instantiation[rcIndxPos] = val-1;
					break; //can reduce this variable by one
				}
			}

			if( varIndx < 0) { return false;}  //no more states, all to "right" have been "cleared"
		} //end while find variable to reduce


		{//change state of variable
			//find a setting of all variables to the right of this one
			for( int i=varIndx+1; i<varsIndices.length; i++) {
				if( instantiation[getInstantiationIndex(i)] < 0) { //if not set yet
					instantiation[varsIndices[i]] = maxState[i];
					varsIndices[i] = -varsIndices[i]-1;
				}
			}
		}


		//set index to right end
		varIndx = varsIndices.length-1;
		return true;
	}

	/**Current states*/
	int[] getCurrentState( int[] ret) {
		if( ret == null) { ret = new int[varsIndices.length];}
		for( int i=0; i<ret.length; i++) {
			int indx = getInstantiationIndex(i);
			ret[i] = instantiation[indx];
		}
		return ret;
	}



	final public void copyVarValuesToInst( int[] inst) {
		for( int i=0; i<varsIndices.length; i++) {
			int indx = getInstantiationIndex(i);
			inst[indx] = instantiation[indx];
		}
	}


	public int getNumVars() { return varsIndices.length;}

	public Collection getVars( Collection ret) {
		if( ret == null) { ret = new HashSet();}

		for( int i=0; i<varsIndices.length; i++) {
			ret.add(rc.vars.get(varsIndices[i]));
		}
		return ret;
	}

}//end class RCInstanceIterator
