package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.io.*;
import edu.ucla.util.*;
import edu.ucla.belief.*;
import edu.ucla.structure.*;



/* Leaf MPE4 code actually calls MPE3 cache functions since the leaf nodes
 * still use the hierarchical caching.
 */


/** This class represents Leaf RCNode objects.
 *
 * @author David Allen
 */
final public class RCNodeLeaf extends RCNode {
    static final private boolean DEBUG_RCNodeLeaf = false;

    RCLookup lkup;
    Collection context;


	/** Warning: by using this constructor you must later call init.*/
    public RCNodeLeaf( RC rc, FiniteVariable fv) {
		super(rc); //requires super.init to be called later by this classes init
        lkup = new RCLookup( fv);
    }

    /** Creates a new RCNodeLeaf.*/
    public RCNodeLeaf( RC rc, FiniteVariable fv, Collection context) {
        super( rc, 1, RC.collOfFinVarsToStateSpace( context));
        this.context = Collections.unmodifiableSet( new HashSet(context));

        lkup = new RCLookup( fv);
    }

	/** Function which should be called if using non-complete constructor.*/
	public void init( Collection acutset, Collection context, boolean includeMPE, ArrayList eo) {
		super.init( 1, RC.collOfFinVarsToStateSpace( context));
        this.context = Collections.unmodifiableSet( new HashSet(context));
        if( rc.TODO_REMOVE_DO_LEAF_CACHING_DURING_CREATION) {
			ArrayList c = RCNodeInternalBinaryCache.orderVars( eo, lkup.getLeafVars( null));
			cache = new RCNodeCache( this, c, 1.0);
			initCache();
		}
	}

	public void initCacheOrder( ArrayList eo) {
        if( rc.TODO_REMOVE_DO_LEAF_CACHING_DURING_CREATION) {
			ArrayList c = RCNodeInternalBinaryCache.orderVars( eo, lkup.getLeafVars( null));
			cache = new RCNodeCache( this, c, 1.0);
			initCache();
		}
	}

	private void initCache() {
		boolean save = RC.TODO_REMOVE_DO_PROP_UP;
		RC.TODO_REMOVE_DO_PROP_UP = true;

		RCInstanceIterator it = new RCInstanceIterator( rc, lkup.getLeafVars( null));
//		RCInstanceIterator it = new RCInstanceIterator( rc, context);
		if( !it.setInitialState()) { throw new IllegalStateException();}

		while( true) {
			if( cache.lookupMPE3_actual() != RCNodeCache.ShouldBeCached) { throw new IllegalStateException();}
			double cptval = lkup.lookup();
			cache.addToCacheMPE3(cptval);

			if( it.next() == false) { break;}
		}

		RC.TODO_REMOVE_DO_PROP_UP = save;
	}


    final public boolean isLeaf() { return true;}

    final public int getHeight() { return 1;}

    final public Collection vars( ) {
		return vars( null);
	}
    final public Collection vars( Collection ret) {
		return lkup.getLeafVars( ret);
    }

    /** Get the "child" variable of this leaf (family).
     */
    final public FiniteVariable getLeafVar() {
		return (FiniteVariable)lkup.cptshell.variables().get( lkup.localVarIndex);
    }


    final double recCond() {
        rc.counters.callsToRCLeaf++;//count calls
        double ret = lkup.lookup();
        return ret;
    }


    final double recCondMPE1( int[] tmp_inst) {
        rc.counters.callsToRCLeaf++;//count calls
        double ret = lkup.lookupMax( tmp_inst);
        return ret;
    }

    final double recCondMPE3( double cutoff) {
        rc.counters.callsToRCLeaf++;//count calls
        if( cache != null) {
			return cache.lookupMPE3_actual();
		}
		else {
	        return lkup.lookupMax( null);
		}
    }
    final double lookAheadMPE3() {
        rc.counters.callsToLookAhd++;//count calls
		if( cache != null) {
			return cache.lookupMPE3_actual();
		}
		else {
	        return lkup.lookupAheadMPEMax();
		}
	}
    final double lookAheadMPE3_setBaseLineIndexing() {
        rc.counters.callsToLookAhd++;//count calls
		if( cache != null) {
			return cache.lookupMPE3_actual();
		}
		else {
	        return lkup.lookupAheadMPEMax();
		}
	}

    final double recCondMPE4( double cutoff) {
        rc.counters.callsToRCLeaf++;//count calls
        if( cache != null) {
			return cache.lookupMPE3_actual();
		}
		else {
	        return lkup.lookupMax( null);
		}
    }
    final double lookAheadMPE4() {
        rc.counters.callsToLookAhd++;//count calls
		if( cache != null) {
			return cache.lookupMPE3_actual();
		}
		else {
	        return lkup.lookupAheadMPEMax();
		}
	}


	final double lookAheadMPE_lowerBound() {
		//this can't use cache because variables are not going to be instantiated in "order" and
		//  therefore we couldn't easily get max values.
		return lkup.lookAheadMPE_lowerBound();
	}




    /**This should only ever be called on a single node dtree (as it is only
     * to be called on root nodes).*/
    Table recCondCutsetJoint() {
		return lkup.cptshell.getCPT();
    }


    void observe( int varIndx, int value) {
		if( varIndx == lkup.varIndices[lkup.localVarIndex]) { resetAncestorNodes();}
	}
	/** This does not notify the ancestors, only locally!*/
    void unobserve( int varIndx) {
		if( varIndx == lkup.varIndices[lkup.localVarIndex]) { resetAncestorNodes();}
	}

	void setCPT( int varIndx) {
		if( varIndx == lkup.varIndices[lkup.localVarIndex]) {
			resetAncestorNodes();
			lkup = new RCLookup( (FiniteVariable)rc.vars.get( varIndx));
		}
	}


	/* This class's code makes some ASSUMPTIONS.
	 *  1) The CPTShell object of a variable won't change after creating the RC Object (keeps a pointer directly to it).
	 *  2) All CPTShells will have the same scalar value (checks in constructor but not again).
	 *  3) The scalar for a CPTShell will not change after creating the RC Object (checks in constructor but not again).
	 *  4) Doesn't currently work with RC.USE_FVState_Randomization.
	 */
    final class RCLookup {

        final int localVarIndex;   //index in varIndices of local variable
        final int rcLocalVarIndex;   //index in rc.instantiation of local variable
        final int numStatesLocalVar;

        final CPTShell cptshell;

        final int[] varIndices;    //mapping from local to instantiation of indexes
        /*final*/ int[] famWithVar;   //store the instantiation of the variable and its family, for querying CPTshell
        int[] famWithVar_tmpMax;   //store the instantiation of the variable and its family, for querying CPTshell
        final int[] instantiation;

	    RCInstanceIterator mpeItr = null;
	    RCInstanceIterator mpeItr2 = null;

        public RCLookup( FiniteVariable fv_in) {

			instantiation = rc.instantiation;

			cptshell = fv_in.getCPTShell( fv_in.getDSLNodeType() );
			numStatesLocalVar = fv_in.size();

            List listVariables = cptshell.variables();
            varIndices = new int[listVariables.size()];
            famWithVar = new int[listVariables.size()];
            famWithVar_tmpMax = new int[listVariables.size()];

			int i=0;
			int findLocalVarIndex = -1;
			for( ListIterator itr = listVariables.listIterator(); itr.hasNext(); ) {
				FiniteVariable fv = (FiniteVariable)itr.next();
                varIndices[i] = rc.vars.indexOf( fv);
                if( fv.equals( fv_in)) { findLocalVarIndex = i;}
                i++;
			}
			localVarIndex = findLocalVarIndex;
			rcLocalVarIndex = varIndices[localVarIndex];

			if( cptshell.scalar() != rc.scalar) {
            	throw new UnsupportedOperationException( "RC.scalar = " + rc.scalar + ", but cptshell.scalar = " + cptshell.scalar() + " " + cptshell.getCPT() + " " + fv_in.getID());
			}

			if( rc.allowKB) {
				mpeItr = new RCInstanceIteratorKB( rc, listVariables);
			}
			else {
				mpeItr = new RCInstanceIterator( rc, listVariables);
			}

			if( RC.TODO_REMOVE_DO_LOOKAHEAD_LOWERBOUND && !rc.allowKB) {
				mpeItr2 = new RCInstanceIterator( rc, listVariables, rc.instantiation_tmp);
			}
        }

        final double lookup() {
            if( instantiation[rcLocalVarIndex] == -1) { return 1.0;}
            else {
				for( int i=0; i<varIndices.length; i++) {
					famWithVar[i] = instantiation[varIndices[i]];
				}
				return cptshell.getCPScaled( famWithVar);
            }
        }



        final double lookupMax( int[] inst_in) {
            double max = -1.0;
            int value = instantiation[rcLocalVarIndex];

            if( value == -1) {

				//set famWithVar (localVarIndex will be wrong, as it isn't set yet)
				for( int i=0; i<varIndices.length; i++) {
					famWithVar[i] = instantiation[varIndices[i]];
				}

                //find max state of var
				for( int v=0; v < numStatesLocalVar; v++) {
					famWithVar[localVarIndex] = v;
					double tmpVal = cptshell.getCPScaled( famWithVar);
					if( tmpVal > max) {
						max = tmpVal;
						value = v;
					}
				}
            }
            else {
				//set famWithVar (localVarIndex will be wrong, as it isn't set yet)
				for( int i=0; i<varIndices.length; i++) {
					famWithVar[i] = instantiation[varIndices[i]];
				}
				max = cptshell.getCPScaled( famWithVar);
            }


            //set values in inst_in
            if( inst_in != null) {
				for( int i=0; i<varIndices.length; i++) {
					if( i == localVarIndex) {
						inst_in[varIndices[i]] = value;
					}
					else {
						inst_in[varIndices[i]] = instantiation[varIndices[i]];
					}
				}
			}

            if( max == -1) { //had an error, most likely due to NotANumber (NaN)
                if( Definitions.DEBUG ) { Definitions.STREAM_VERBOSE.println("RCNodeLeaf:lookupMax:Error");}
                max = 0;
            }

            return max;
        }


		/**Not all variables may be instantiated, so use an iterator*/
        final double lookAheadMPE_lowerBound( ) {
            double max = -1.0;

			boolean needToSetVars = false;
            boolean notDone;

            notDone = mpeItr2.setInitialState();
            while( notDone) {
				mpeItr2.getCurrentState( famWithVar);
				double tmpVal = cptshell.getCPScaled( famWithVar);
				if( tmpVal > max) {
					max = tmpVal;

					//swap famWithVar and famWithVar_tmpMax so that max is always in famWithVar_tmpMax
					int[] tmp = famWithVar;
					famWithVar = famWithVar_tmpMax;
					famWithVar_tmpMax = tmp;
					needToSetVars = true;
				}
				notDone = mpeItr2.next();
			}


			//set instantiation on variables used to get max value
			if( needToSetVars) {
				for( int i=0; i<famWithVar_tmpMax.length; i++) {
					rc.instantiation_tmp[varIndices[i]] = famWithVar_tmpMax[i];
				}
			}

            return max;
        }



		/**Not all variables may be instantiated, so use an iterator*/
        final double lookupAheadMPEMax( ) {
            double max = -1.0;

            boolean notDone;
            notDone = mpeItr.setInitialState();
            while( notDone) {
				mpeItr.getCurrentState( famWithVar);
				double tmpVal = cptshell.getCPScaled( famWithVar);
				if( tmpVal > max) {
					max = tmpVal;
				}
				notDone = mpeItr.next();
			}

            return max;
        }


        Collection getLeafVars( Collection ret) {
            if( ret == null) { ret = new HashSet();}

			ret.addAll( cptshell.variables());
            return ret;
        }
    }
}//end class RCNodeLeaf
