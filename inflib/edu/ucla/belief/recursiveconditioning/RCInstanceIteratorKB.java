package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.io.*;
import edu.ucla.util.*;
import edu.ucla.belief.*;

/** This class represents an Instance Iterator and will work with or without a KB.
 *
 * @author David Allen
 */
public class RCInstanceIteratorKB extends RCInstanceIterator {

	protected boolean useKB;

	public RCInstanceIteratorKB( RC rc, Collection vars) {
		super( rc, vars);
		useKB = (rc.kb != null);
	}


	/* Cannot override this constructor, because using a different instantiation array will cause problems with KB.*/
//	public RCInstanceIterator( RC rc, Collection vars, int instantiation[])


	/** Will set (or clear) a variable's instantiation.  To clear it, set val to -1.
	 *
	 *  <p>It is possible for this to fail if using the KB.
	 */
	final protected boolean setVarInst( int vIndx, int val) {

		int rcIndx = varsIndices[vIndx]; //could be + ("this" hasn't set) or - ("this" has set)

		if( val < 0 && rcIndx < 0) { //want to clear evid that "this" set
			rcIndx = -rcIndx-1;
			varsIndices[vIndx] = rcIndx;
			rc.setInst( rcIndx, -1);
		}
		else if( val > -1 && rcIndx > -1) { //want to set evid on a var already "clear"
			if( !rc.setInst( rcIndx, val)) {
				return false;
			}
			varsIndices[vIndx] = -rcIndx-1;
		}
		else {
			throw new IllegalStateException("Tried to set " + vIndx + " to " + val + ", oldIndx = " + varsIndices[vIndx] +
											" " + rc.vars.get(getInstantiationIndex(vIndx)));
		}
		return true;
	}


	protected final void clearAllSetByItr() {
		for( int i=varsIndices.length-1; i>=0; i--) {
			if( varsIndices[i] < 0) { //since this isn't necessarily called in order, someone else may have reset it
				varsIndices[i] = -varsIndices[i]-1;
				rc.setInst(varsIndices[i], -1);
			}
		}
	}


	/** It is possible for this to fail if using the KB.*/
	protected final boolean setInitialState() {
		useKB = (rc.kb != null); //use this for speedups later in the iteration process

		//set varIndx & states of variables
		if( !useKB) { return super.setInitialState();}

		for( int i=0; i<varsIndices.length; i++) {
			//for each variable try to set it to its max state (if invalid set it to anything by changing other vars left of it)
			if( instantiation[getInstantiationIndex(i)] < 0) {
				if( !findValidStateAllowChangeLeft( i, -1, 0)) {
					varIndx = -1; //there is no valid initial state!
					return false;
				}
			}
		}

		varIndx = varsIndices.length-1; //set this to the far right & let it find the next iterator state
		return true;
	}


	/** Return true if found a state for this variable that worked.*/
	final private boolean findValidState( int cutsetIndx, int thisOrLower) {
		boolean done = false;
		while( !done && thisOrLower>=0) {
			done = setVarInst( cutsetIndx, thisOrLower);
			thisOrLower--;
		}
		return done;
	}

	//Assumes all to the left have had some evidence set on them (either here or elsewhere)...
	final private boolean findValidStateAllowChangeLeft( int cutsetIndx, int thisOrLower, int leftmostChangeAble) {
		int cutIndx = cutsetIndx;
		int valueToTry = thisOrLower;

		while( cutIndx <= cutsetIndx) {
			int rcIndx = getInstantiationIndex(cutIndx);
			if( valueToTry < 0) { valueToTry = maxState[cutIndx];} //normally want max state, unless that failed

			//if doesn't already have someone setting it to something
			if( instantiation[ rcIndx] < 0) {
				boolean ok = findValidState( cutIndx, valueToTry);

				if( ok) { //found acceptable state, move to right and try to find one there
					cutIndx++;
					valueToTry = -1;
				}
				else { //if !ok, then need to undo last set value & try another value
					boolean foundAnother = false;
					while( !foundAnother) {
						cutIndx--;

						if( cutIndx < leftmostChangeAble) { return false;} //could not find any valid state for cutsetIndx (even changing all values to the left)

						if( varsIndices[cutIndx] < 0) { //set by "this"

							rcIndx = getInstantiationIndex(cutIndx);
							valueToTry = instantiation[rcIndx]-1;

							setVarInst(cutIndx, -1);
							if( findValidState( cutIndx, valueToTry)) { //could reduce this variable
								foundAnother = true;
								cutIndx++;
								valueToTry = -1;
							}
//							else { //could not reduce this variable, keep going left
//							}
						}
					} //end while !foundAnother
				} //end if ok
			} //end if something set on it
			else {
				cutIndx++;
				valueToTry = -1;
			}
		} //end while
		return true;
	}


	/** Will set the next state and return true if one is found.  If no next state exists,
	 *   it will return false.
	 */
	protected boolean next() {

		if( !useKB) { return super.next();}

		if( varIndx < 0) {
			return false;
		}
		int val;

		boolean done = false;

		while( !done) { //until found a variable which can be reduced

			//move left until can reduce a state by one
			while( true) {
				int rcIndx = varsIndices[varIndx]; //could be positive or negative
				int rcIndxPos = getInstantiationIndex( varIndx);
				val = instantiation[rcIndxPos];

				//everything to the "left" must have evid, either from me or from somewhere else
				if( val < 0) { //not set to anything, therefore couldn't reduce it, & we are in an illegal state
					throw new IllegalStateException("var " + rc.vars.get(rcIndxPos) + " was not set.");
				}

				if( rcIndx > -1) { //if has evid & not by "this", then skip it
					varIndx--;
				}
				else { //has evid set by "this"
					if( val == 0) { //then at its lowest state, clear it and try next variable
						varsIndices[varIndx] = -rcIndx-1;
						rc.setInst( rcIndxPos, -1);
						varIndx--;
					}
					else {
						varsIndices[varIndx] = -rcIndx-1;
						rc.setInst( rcIndxPos, -1);
						break; //can reduce this variable by one
					}
				}

				if( varIndx < 0) { return false;}  //no more states, all to "right" have been "cleared"
			} //end while find variable to reduce


			{ //with KB setting states can fail

				//reduce it by one if possible (if not, continue searching for another variable to reduce)
				if( findValidState( varIndx, val-1)) {

					done = true; //assume can find setting of variables to the right

					//find a setting of all variables to the right of this one
					for( int i=varIndx+1; i<varsIndices.length; i++) {
						if( instantiation[getInstantiationIndex(i)] < 0) { //if not set yet
							if( !findValidStateAllowChangeLeft( i, maxState[i], varIndx+1)) {  //could not find another valid state
								done = false;
								break;
							}
						}
					}
					if( !done) { //couldn't find setting to the right of varIndx
						setVarInst( varIndx, -1);
						varIndx--;
						if( varIndx < 0) {
							return false;
						}
					}
				}
				else { //couldn't reduce this one
					varIndx--;
					if( varIndx < 0) {
						return false;
					}
				} //if return, no more states. all to "right" have been "cleared"
			}

		}//until found a variable which can be reduced


		//set index to right end
		varIndx = varsIndices.length-1;
		return true;
	}

}//end class RCInstanceIteratorKB
