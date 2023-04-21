package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.io.*;
import edu.ucla.util.*;
import edu.ucla.belief.*;

/** This class represents Internal RCNode objects which always have two children and a cache.
 *
 * @author David Allen
 */
final public class RCNodeInternalBinaryCache extends RCNode {
//    static final private boolean DEBUG_RCNodeIBC = false;

    protected RCNode left;
    protected RCNode right;
    Collection cntx_tmp = null;
    double cf_tmp = -1;

	protected RCInstanceIterator itr;
	protected Table cacheCondCutsetJoint = null;


	final boolean TODO_REMOVE_DO_INDEX_CACHING_DURING_CREATION = RC.TODO_REMOVE_DO_INDEX_CACHING_DURING_CREATION;
	int leftNIV[] = null;  //mapping from number of instantiated variables at this level to the same at the child level
	int rightNIV[] = null;
	int leftIndx[][] = null; //mapping from the context index at this level to the context index at the child level
	int rightIndx[][] = null;

	RCNodeLeaf descendantLeafNodes[];


	/** Warning: by using this constructor you must later call init AND
	 *  must add this node to the children manually.
	 *
	 *  <p>In general don't use this except for very special cases!
	 */
    RCNodeInternalBinaryCache( RC rc, RCNode left, RCNode right, boolean b1, boolean b2) {
		super( rc); //requires super.init to be called later by this classes init
		this.left = left;
		this.right = right;
        cache = null; //does not create an RCNodeCache
        itr = null; //does not create an RCInstanceIterator

		{
			HashSet leafs = new HashSet();
			RCUtilities.computeLeafs( this, leafs);
			descendantLeafNodes = (RCNodeLeaf[])leafs.toArray( new RCNodeLeaf[leafs.size()]);
		}
    }

	/** Warning: by using this constructor you must later call init.*/
    public RCNodeInternalBinaryCache( RC rc, RCNode left, RCNode right) {
		super( rc); //requires super.init to be called later by this classes init
        setChildren( left, right);
        cache = null; //does not create an RCNodeCache
        itr = null; //does not create an RCInstanceIterator
    }


    /** Warning: by using this constructor you must later call initCacheOrder.
     *
     * @params mpeVars Can be null if no mpe info is desired.
     */
    public RCNodeInternalBinaryCache( RC rc, Collection cutset, Collection context,
                                	  RCNode left, RCNode right, double cf, Collection mpeVars) {
        super( rc, RC.collOfFinVarsToStateSpace( cutset),
                   RC.collOfFinVarsToStateSpace( context));

        if( rc.allowKB) {
	        itr = new RCInstanceIteratorKB( rc, cutset);
		}
		else {
	        itr = new RCInstanceIterator( rc, cutset);
		}

        if( mpeVars != null) {
            vars_to_save_mpe = new int[mpeVars.size()];
            int i=0;
            for( Iterator itr = mpeVars.iterator(); itr.hasNext();) {
                vars_to_save_mpe[i] = rc.vars.indexOf( itr.next());
                i++;
            }
        }

        setChildren( left, right);
        cntx_tmp = new HashSet( context);
        cf_tmp = cf;
        cache = null;
    }

    /** Creates a new RCNodeInternalCache.
     *
     * @params mpeVars Can be null if no mpe info is desired.
     */
    public RCNodeInternalBinaryCache( RC rc, Collection cutset, List context,
                                	  RCNode left, RCNode right, double cf, Collection mpeVars,
                                	  ArrayList eo) {
        super( rc, RC.collOfFinVarsToStateSpace( cutset),
                   RC.collOfFinVarsToStateSpace( context));

        if( rc.allowKB) {
	        itr = new RCInstanceIteratorKB( rc, cutset);
		}
		else {
	        itr = new RCInstanceIterator( rc, cutset);
		}

        if( mpeVars != null) {
            vars_to_save_mpe = new int[mpeVars.size()];
            int i=0;
            for( Iterator itr = mpeVars.iterator(); itr.hasNext();) {
                vars_to_save_mpe[i] = rc.vars.indexOf( itr.next());
                i++;
            }
        }

        setChildren( left, right);

		//create cache
		{
			ArrayList ord_cntx = new ArrayList( context.size());
			for( int i=eo.size()-1; i>=0; i--) {
				Object var = eo.get(i);
				if( context.contains( var)) {
					ord_cntx.add( var);
				}
			}

			cache = new RCNodeCache( this, ord_cntx, cf);
			initChildIndexing();
		}
    }


	static public ArrayList orderVars( List globalOrd, Collection localVars) {
		ArrayList ret = new ArrayList( localVars.size());
		for( int i=globalOrd.size()-1; i>=0; i--) {
			Object var = globalOrd.get(i);
			if( localVars.contains( var)) {
				ret.add( var);
			}
		}
		return ret;
	}



	public void initCacheOrder( ArrayList eo) {
		if( cache == null) {
			ArrayList ord_cntx = orderVars(eo, cntx_tmp);
			cache = new RCNodeCache( this, ord_cntx, cf_tmp);
			cntx_tmp = null;
			cf_tmp = -1;
		}
		left.initCacheOrder( eo);
		right.initCacheOrder( eo);
		initChildIndexing();
	}


	/** Function which should be called if using non-complete constructor.*/
	void init( Collection acutset, Collection context, boolean includeMPE, ArrayList eo) {

		{
			cacheCondCutsetJoint = null;
			//determine cutset and context

			Assert.notNull( context, "Context cannot be null");
	//		if( context == null) {
	//			Collection vars = vars();
	//			vars.retainAll( acutset);
	//			context = vars;
	//		}

			Collection cutset = RCUtilities.calculateCutsetFromChildren( this, acutset);

			super.init( RC.collOfFinVarsToStateSpace( cutset),
						RC.collOfFinVarsToStateSpace( context));

			if( itr == null) {
				if( rc.allowKB) {
					itr = new RCInstanceIteratorKB( rc, cutset);
				}
				else {
					itr = new RCInstanceIterator( rc, cutset);
				}
			}
			else {
				return;
			}


			//determine mpeVars
			//mpeVars: L union R - context (for binary nodes).
			if( vars_to_save_mpe == null && includeMPE == true) {

				Collection mpeVars = new HashSet();
				for( RCIterator chiitr = childIterator(); chiitr.hasNext();) {
					RCNode chi = chiitr.nextNode();
					mpeVars.addAll( chi.vars());
				}
				mpeVars.removeAll( context);

				if( mpeVars.size() > 0) {
					vars_to_save_mpe = new int[mpeVars.size()];
					int i=0;
					for( Iterator itr = mpeVars.iterator(); itr.hasNext();) {
						vars_to_save_mpe[i] = rc.vars.indexOf( itr.next());
						i++;
					}
				}
			}

			Collection newacutset = new HashSet( acutset);
			newacutset.addAll( cutset);

			for( RCIterator chiitr = childIterator(); chiitr.hasNext();) {
				RCNode chi = chiitr.nextNode();
				Collection newcntx = chi.vars();
				newcntx.retainAll( newacutset);

				chi.init( newacutset, newcntx, includeMPE, eo);
			}
		}


		//create cache
		{
			ArrayList ord_cntx = new ArrayList( context.size());
			for( int i=eo.size()-1; i>=0; i--) {
				Object var = eo.get(i);
				if( context.contains( var)) {
					ord_cntx.add( var);
				}
			}
			cache = new RCNodeCache( this, ord_cntx, 1.0);
			initChildIndexing();
		}
	}


	final void initChildIndexing() {
		if( !TODO_REMOVE_DO_INDEX_CACHING_DURING_CREATION) { return;}

		ArrayList cntx = (ArrayList)cache.getContext( new ArrayList());

		boolean doLeft = (left.cache != null);
		boolean doRight = (right.cache != null);
		ArrayList leftCntx = null;
		ArrayList rightCntx = null;
		if( doLeft) { leftCntx = (ArrayList)left.cache.getContext( new ArrayList());}
		if( doRight) { rightCntx = (ArrayList)right.cache.getContext( new ArrayList());}

		{//initialize the number of children variables instantiated based on how many this node has
			if( doLeft) {leftNIV = new int[cntx.size()+1];}
			if( doRight) {rightNIV = new int[cntx.size()+1];}

			int tmpl = 0;
			int tmpr = 0;
			int tmpi = 0;
			for( ListIterator itr = cntx.listIterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				tmpi++;
				if( doLeft) {
					if( leftCntx.contains( fv)) { tmpl++;}
					leftNIV[tmpi] = tmpl;
				}
				if( doRight) {
					if( rightCntx.contains( fv)) { tmpr++;}
					rightNIV[tmpi] = tmpr;
				}
			}
		}
		{//initialize the indexing of the children based on the indexing of this node

			int iTOj[] = new int[cntx.size()];

			{
				long tmpSize = 1;
				if( doLeft) leftIndx = new int[cntx.size()+1][];
				if( doRight) rightIndx = new int[cntx.size()+1][];
				for( int tmpNIV = 0; tmpNIV <= cntx.size(); tmpNIV++) {
					if(doLeft) leftIndx[tmpNIV] = new int[(int)tmpSize];
					if(doRight) rightIndx[tmpNIV] = new int[(int)tmpSize];
					if( tmpNIV < cntx.size()) {
						tmpSize *= ((FiniteVariable)(rc.vars.get(cache.contextIndices[tmpNIV]))).size();
					}
					if( tmpSize >= Integer.MAX_VALUE) { throw new IllegalStateException("");}
				}
			}

			if(doLeft){
				//Calculate leftIndx
				{
					for( int i=0; i<iTOj.length; i++) {
						iTOj[i] = leftCntx.indexOf( rc.vars.get(cache.contextIndices[i]));
					}
				}//iTOj will now have a mapping from contextSizes here to contextSizes of child


				for( int tmpNIV = 0; tmpNIV < leftIndx.length; tmpNIV++) {
					for( int tmpIndx = 0; tmpIndx < leftIndx[tmpNIV].length; tmpIndx++) {
						int indx = tmpIndx;
						int newIndx = 0;

						//convert indx -> newIndx
						for( int tniv = cntx.size()-1; tniv >= 0; tniv--) {
							int instVarI = (int)Math.floor( indx / (double)cache.contextSizes[tniv]);
							indx = indx % cache.contextSizes[tniv];
							if( left.cache != null && iTOj[tniv] != -1) {
								newIndx = newIndx + (instVarI * left.cache.contextSizes[iTOj[tniv]]);
							}
						}

						leftIndx[tmpNIV][tmpIndx] = newIndx;
					}
				}
			}

			if(doRight){
				//Calculate rightIndx
				{
					for( int i=0; i<iTOj.length; i++) {
						iTOj[i] = rightCntx.indexOf( rc.vars.get(cache.contextIndices[i]));
					}
				}//iTOj will now have a mapping from contextSizes here to contextSizes of child


				for( int tmpNIV = 0; tmpNIV < rightIndx.length; tmpNIV++) {
					for( int tmpIndx = 0; tmpIndx < rightIndx[tmpNIV].length; tmpIndx++) {
						int indx = tmpIndx;
						int newIndx = 0;

						//convert indx -> newIndx
						for( int tniv = cntx.size()-1; tniv >= 0; tniv--) {
							int instVarI = (int)Math.floor( indx / (double)cache.contextSizes[tniv]);
							indx = indx % cache.contextSizes[tniv];
							if( right.cache != null && iTOj[tniv] != -1) {
								newIndx = newIndx + (instVarI * right.cache.contextSizes[iTOj[tniv]]);
							}
						}

						rightIndx[tmpNIV][tmpIndx] = newIndx;

					}
				}
			}
		}//end initialize the indexing of the children based on the indexing of this node
	}//end initChildIndexing



	/////////////////
	//  Functions dealing with children
	/////////////////

    public RCNode left() { return left;}
    public RCNode right() { return right;}

    /** Sets the left and right child for an internal node.
     *
     *  <p>Does NOT reset the cutset of this node.
     *
     * @throws IllegalArgumentException if an error is encountered.
     */
    private void setChildren( RCNode lt, RCNode rt) {
		if( left != null) { left.parentNodes.remove( this);}
		if( right != null) { right.parentNodes.remove( this);}

        if( lt==null || rt==null) {
            throw new IllegalArgumentException("RCNodeInternalBinaryCache: cannot have null children");
        }
        if( lt.equals( rt)) {
            throw new IllegalArgumentException("RCNodeInternalBinaryCache: children cannot be equal");
        }
        left = lt;
        right = rt;

		left.parentNodes.add( this);
		right.parentNodes.add( this);


		{
			HashSet leafs = new HashSet();
			RCUtilities.computeLeafs( this, leafs);
			descendantLeafNodes = (RCNodeLeaf[])leafs.toArray( new RCNodeLeaf[leafs.size()]);
		}
    }

	public HashSet children() {
		HashSet ret = new HashSet();
		ret.add( left);
		ret.add( right);
		return ret;
	}

	public RCNode[] childrenArr() {
		RCNode[] ret = new RCNode[]{left, right};
		return ret;
	}

	public RCIterator childIterator() {
		return new ChildIterator();
	}
	public int numChildren() { return 2;}




	/////////////////
	//  Functions dealing with cache
	/////////////////

    public double getCacheFactor() { return cache.cf();}
    public void changeCacheFactor( double cf) {
        if( cache != null) {cache.changeCacheFactor( cf);}
    }

	public void allocRCCaches() {cache.allocRCCache();}
	public void allocRCMPECaches() {cache.allocRCMPECache();}
	public void allocRCMPE3Caches() {cache.allocRCMPE3Cache();}
	public void allocRCMPE4Caches() {cache.allocRCMPECache();}

	public HashSet getCutset( HashSet ret) {
		if( itr != null) { return (HashSet)itr.getVars( ret);}
		else { return null;}
	}

    public void resetLocal() {
		super.resetLocal();
        itr.clearAllSetByItr();
		cacheCondCutsetJoint = null;
        if( cache != null) {cache.clearCache();}
    }

    public long numCacheEntries_local_total() { return cache.cacheEntries_total();}
    public long numCacheEntriesMpe_local_total() { return cache.cacheEntriesMpe_total();}
    public long numCacheEntries_local_used() { return cache.cacheEntries_used();}
    public long numCacheEntriesMpe_local_used() { return cache.cacheEntriesMpe_used();}


    public boolean isLeaf() { return false;}


    public int getHeight() {
		int tmp=0;
		for( RCIterator chiitr = childIterator(); chiitr.hasNext();) {
			RCNode chi = chiitr.nextNode();
			int t = chi.getHeight();
			if( t > tmp) { tmp = t;}
		}
		return tmp+1;
	}


    public Collection vars( ) {
		return vars( null);
	}

    public Collection vars( Collection ret) {
		if( ret == null) { ret = new HashSet();}
		for( RCIterator chiitr = childIterator(); chiitr.hasNext();) {
			RCNode chi = chiitr.nextNode();
			chi.vars( ret);
		}
		return ret;
	}

	/////////////////
	//  Functions dealing with computations
	/////////////////

    Table recCondCutsetJoint() {
        Table ret;
        TableIndex retindx;
        int mindex[] = null;
        int flatindex;

		if( cacheCondCutsetJoint != null) {
			return cacheCondCutsetJoint;
		}

		double newCPT[];

        { //table index is based on ordering of variables in cutset so that mindex is correct
            FiniteVariable v[] = new FiniteVariable[itr.getNumVars()];
            for( int i=0; i<v.length; i++) {
                v[i] = (FiniteVariable)rc.vars.get( itr.getInstantiationIndex(i));
            }
            retindx = new TableIndex(v);
            newCPT = new double[retindx.size()];
            if( rc.scalar != 1.0) {
	            ret = new TableScaled( retindx, newCPT, rc.scalar);
			}
			else {
	            ret = new Table( retindx, newCPT);
			}
			Arrays.fill( newCPT, 0);
        }

        rc.counters.callsToRCInternalCompute++;

        if( !itr.setInitialState()) { return ret;}
        mindex = itr.getCurrentState( mindex);
        flatindex = retindx.index(mindex);
        newCPT[flatindex] = left.recCond() * right.recCond();

        while( itr.next()) {
			if( rc.computationUserRequest != RC.COMPUTATION_RUN) {
				if( rc.computationUserRequest == RC.COMPUTATION_STOP) { //early termination for threaded version
					throw new RC.RCUserRequestedStop();
				}
				else if( rc.computationUserRequest == RC.COMPUTATION_PAUSE )
				{
					try{
						rc.waitForResume();
					}
					catch( InterruptedException e ){
						System.err.println( "Thread interrupted during RCNodeInternalBinaryCache.recCond()" );
					}
				}
			}
            mindex = itr.getCurrentState( mindex);
            flatindex = retindx.index(mindex);
	        newCPT[flatindex] = left.recCond() * right.recCond();
        }

        cacheCondCutsetJoint = ret;
        return ret;
    }

    final double recCondMPE1( int[] tmp_inst) {
		double ret;
		if( !cache.cached) {
			ret = recCondMPE1_NoCaching( tmp_inst, null);
		}
		else {

			ret = cache.lookupMPE1();
			if( ret >= 0.0) {
				rc.counters.callsToRCInternalCacheHit++;
				int[] inst_best = cache.lookupMPE1Arr();

				for( int i=0; i<inst_best.length; i++) {
					tmp_inst[vars_to_save_mpe[i]] = inst_best[i];  //return result in tmp_inst
				}
			}
			else if( ret == RCNodeCache.ShouldBeCached) {
				rc.counters.callsToRCInternalComputeAndSave++;
				int[] inst_cache = cache.lookupMPE1Arr();

				ret = recCondMPE1_NoCaching( tmp_inst, inst_cache);
				cache.addToCacheMPE1( ret, inst_cache); //save the value for next time
			}
			else {
				ret = recCondMPE1_NoCaching( tmp_inst, null); //call the non-caching version
			}
		}
        return ret;
    }



    double recCondMPE1_NoCaching( int[] tmp_inst, int[] best_i_in) {
        double max = 0.0;
        int[] best_i;

        if( best_i_in != null) {
            Assert.condition( best_i_in.length == vars_to_save_mpe.length);
            best_i = best_i_in;
        }
        else {
            best_i = new int[vars_to_save_mpe.length];
        }
        Arrays.fill( best_i, -1);

        rc.counters.callsToRCInternalCompute++;


		if( rc.computationUserRequest != RC.COMPUTATION_RUN) {
			if( rc.computationUserRequest == RC.COMPUTATION_STOP) { //early termination for threaded version
				throw new RC.RCUserRequestedStop();
			}
			else if( rc.computationUserRequest == RC.COMPUTATION_PAUSE )
			{
				try{
					rc.waitForResume();
				}
				catch( InterruptedException e ){
					System.err.println( "Thread interrupted during RCNodeInternalBinaryCache.recCondMPE1_NoCaching()" );
				}
			}
		}


        //child returns results in tmp_inst and if they are better than max then they
        //  are copied into best_i.  When done this copies the best it found into
        //  tmp_inst and returns that to its parent.

        if( !itr.setInitialState()) {
			itr.copyVarValuesToInst( tmp_inst);//set cutset vars to correct inst (based on itr)
			return 0.0;
		}

		max = left.recCondMPE1( tmp_inst) * right.recCondMPE1( tmp_inst);
        itr.copyVarValuesToInst( tmp_inst);//set cutset vars to correct inst (based on itr)
        for( int i=0; i<best_i.length; i++) {
            best_i[i] = tmp_inst[vars_to_save_mpe[i]];
        }

        while( itr.next() )
        {
			double max_tmp = left.recCondMPE1( tmp_inst) * right.recCondMPE1( tmp_inst);;
            if( max_tmp > max) {
                max = max_tmp;
                itr.copyVarValuesToInst( tmp_inst);//set cutset vars to correct inst (based on itr)
                for( int i=0; i<best_i.length; i++) {
                    best_i[i] = tmp_inst[vars_to_save_mpe[i]];
                }
            }
        }

        for( int i=0; i<best_i.length; i++) {
            tmp_inst[vars_to_save_mpe[i]] = best_i[i];
        }
        return max;
    }




	final double lookAheadMPE4() {
        rc.counters.callsToLookAhd++;//count calls
		double ret = 1;
		for( int i=0; i<descendantLeafNodes.length; i++) {
			ret *= descendantLeafNodes[i].lookAheadMPE4();
		}
		return ret;
	}


	/*  Only called on children from recCondMPE3, so can clear the cache indecies at the end.*/
    final double lookAheadMPE3() {
		double ret = -1;
        rc.counters.callsToLookAhd++;//count calls

		if( cache.cached) { ret = cache.lookupMPE3_bound();}

		if( ret < 0.0) { //need to compute it
			if( TODO_REMOVE_DO_INDEX_CACHING_DURING_CREATION) {
				RCNodeCache lfch = left.cache;
				RCNodeCache rtch = right.cache;
				if( lfch != null) {
					lfch.numInstVars = leftNIV[cache.numInstVars];
					lfch.lastIndex = leftIndx[cache.numInstVars][cache.lastIndex];
				}
				if( rtch != null) {
					rtch.numInstVars = rightNIV[cache.numInstVars];
					rtch.lastIndex = rightIndx[cache.numInstVars][cache.lastIndex];
				}
			}
			ret = left.lookAheadMPE3() * right.lookAheadMPE3();

			if( cache.cached) { cache.addToCacheMPE3Bound( ret);}
		}

		cache.lastIndex = -1;
		cache.numInstVars = -1;
		return ret;
	}


	/*  This function will leave the cache lookup indecies set after its call
	 *  (must be cleared by caller).
	 */
    final double lookAheadMPE3_setBaseLineIndexing() {
		double ret = -1;
        rc.counters.callsToLookAhd++;//count calls

		if( cache.cached) { ret = cache.lookupMPE3_bound();}

		if( ret < 0.0) { //need to compute it
			if( TODO_REMOVE_DO_INDEX_CACHING_DURING_CREATION) {
				RCNodeCache lfch = left.cache;
				RCNodeCache rtch = right.cache;
				if( lfch != null) {
					lfch.numInstVars = leftNIV[cache.numInstVars];
					lfch.lastIndex = leftIndx[cache.numInstVars][cache.lastIndex];
				}
				if( rtch != null) {
					rtch.numInstVars = rightNIV[cache.numInstVars];
					rtch.lastIndex = rightIndx[cache.numInstVars][cache.lastIndex];
				}
			}
			ret = left.lookAheadMPE3() * right.lookAheadMPE3();

			if( cache.cached) { //need to save indexing during this call
				int tmpLI = cache.lastIndex;
				int tmpNIV = cache.numInstVars;
				cache.addToCacheMPE3Bound( ret);
				cache.lastIndex = tmpLI;
				cache.numInstVars = tmpNIV;
			}
		}

		//don't clear indexing with baseline version
//		cache.lastIndex = -1;
//		cache.numInstVars = -1;
		return ret;
	}



	final double lookAheadMPE_lowerBound() {
		double ret = 1.0;

		System.arraycopy( rc.instantiation, 0, rc.instantiation_tmp, 0, rc.instantiation.length);

		for(int i=0; i < descendantLeafNodes.length; i++) {
			ret *= descendantLeafNodes[i].lookAheadMPE_lowerBound();
		}

		return ret;
	}




    final double recCondMPE3( double cutoff) {
		double ret;

		if( !cache.cached) {
			ret = recCondMPE3_NoCache( cutoff);
		}
		else {

			ret = cache.lookupMPE3_actual();

			if( ret >= 0.0) {
				rc.counters.callsToRCInternalCacheHit++;
			}
			else if( ret == RCNodeCache.ShouldBeCached) {
				rc.counters.callsToRCInternalComputeAndSave++;

				ret = cache.lookupMPE3_bound(); //possibly have a precomputed bound to prune with

				if( ret <= cutoff && ret >= 0) { //have bound, but know it is less than cutoff, so can prune
					ret = -ret;
					cache.lastIndex = -1;
					cache.numInstVars = -1;
				}
				else {
					ret = recCondMPE3_NoCache( cutoff);
					if( ret >= 0.0) {
						cache.addToCacheMPE3( ret); //save the value for next time
					}
					else{ //negative value means that actual is <= -ret
						cache.addToCacheMPE3Bound( -ret); //save the value for next time
					}
				}
			}
			else {
				ret = recCondMPE3_NoCache( cutoff); //call the non-caching version
				cache.lastIndex = -1;
				cache.numInstVars = -1;
			}
		}
        return ret;
    }


	final private double recCondMPE3_NoCache( double cutoff) {

		if( TODO_REMOVE_DO_INDEX_CACHING_DURING_CREATION) {
			RCNodeCache lfch = left.cache;
			RCNodeCache rtch = right.cache;

			if( lfch != null) {
				lfch.numInstVars = leftNIV[cache.numInstVars];
				lfch.lastIndex = leftIndx[cache.numInstVars][cache.lastIndex];
			}
			if( rtch != null) {
				rtch.numInstVars = rightNIV[cache.numInstVars];
				rtch.lastIndex = rightIndx[cache.numInstVars][cache.lastIndex];
			}
		}


		double leftLA = left.lookAheadMPE3_setBaseLineIndexing();
		double rightLA = right.lookAheadMPE3_setBaseLineIndexing();
		if( leftLA * rightLA <= cutoff) {
			left.clearContextIndexing(); right.clearContextIndexing(); //clear "baseline indexing"
			return -(leftLA * rightLA);
		}

        rc.counters.callsToRCInternalCompute++;


		boolean leftFirst;
		{
			if( RC.TODO_REMOVE_REVERSE_ORDER == false) {
				//Go down the largest first
				if( leftLA >= rightLA) { leftFirst = true;}  //TODO: Heuristic ordering (other may be better?)
				else { leftFirst = false;}
			}
			else {
				//Go down the smallest first
				if( leftLA < rightLA) { leftFirst = true;}  //TODO: Heuristic ordering (other may be better?)
				else { leftFirst = false;}
			}
		}

		RCNode n1, n2;
		double la1, la2;
		{
			if( leftFirst) {
				n1 = left;  la1 = leftLA;
				n2 = right; la2 = rightLA;
			}
			else {
				n1 = right; la1 = rightLA;
				n2 = left;  la2 = leftLA;
			}
		}


		boolean n1CacheBaseline = ( n1.cache != null && n1.cache.cached);
		boolean n2CacheBaseline = ( n2.cache != null && n2.cache.cached);
		int baselineN1LI;
		int baselineN1NIV;
		int baselineN2LI;
		int baselineN2NIV;

		{
			if( n1CacheBaseline) {
				baselineN1LI  = n1.cache.lastIndex;
				baselineN1NIV = n1.cache.numInstVars;
			}
			else {
				baselineN1LI  = -1;
				baselineN1NIV = -1;
			}

			if( n2CacheBaseline) {
				baselineN2LI  = n2.cache.lastIndex;
				baselineN2NIV = n2.cache.numInstVars;
			}
			else {
				baselineN2LI  = -1;
				baselineN2NIV = -1;
			}
		}



        double max = -1;
        double max_bound = -1;


		if(RC.TODO_REMOVE_DO_LOOKAHEAD_LOWERBOUND) {
			double low = lookAheadMPE_lowerBound();
			if( low >= cutoff) {
				max = low;
				cutoff = low;
			}
			if( (la1*la2)== low) {
				left.clearContextIndexing(); right.clearContextIndexing(); //clear "baseline indexing"
				return low;
			}
		}



        if( !itr.setInitialState()) {
			left.clearContextIndexing(); right.clearContextIndexing(); //clear "baseline indexing"
			return 0.0;
		}

        while( true )
        {

			double tempLA2;
			if( RC.TODO_REMOVE_DO_EXTRA_LOOKAHEAD) {
				if( n2CacheBaseline) { n2.cache.lastIndex = baselineN2LI; n2.cache.numInstVars = baselineN2NIV;}
				tempLA2 = n2.lookAheadMPE3(); //slightly more restricted than la2 (since have instantiated cutset now)
			}
			else {
				tempLA2 = la2;
			}

			double max_tmp = tempLA2 * la1;
			if( max_tmp > cutoff) {

				if( n1CacheBaseline) { n1.cache.lastIndex = baselineN1LI; n1.cache.numInstVars = baselineN1NIV;}
				double actualL1 = n1.recCondMPE3( cutoff / tempLA2);
				max_tmp = actualL1 * tempLA2;

				if( max_tmp < 0) {
					max_tmp = -max_tmp; //if pruned here because of cutoff (make it positive)
					if( (max_tmp > cutoff) && (max_tmp - cutoff < .00001)) {max_tmp = cutoff;}//adjust roundoff error
				}
				else if( max_tmp > cutoff) {

					if( n2CacheBaseline) { n2.cache.lastIndex = baselineN2LI; n2.cache.numInstVars = baselineN2NIV;}
					double actualL2 = n2.recCondMPE3( cutoff / actualL1);
					max_tmp = actualL1 * actualL2;
					if( max_tmp < 0) { //if pruned here because of cutoff
						max_tmp = -max_tmp;
						if( (max_tmp > cutoff) && (max_tmp - cutoff < .00001)) {max_tmp = cutoff;}//adjust roundoff error
					}
					else if( max_tmp > max && max_tmp >= cutoff) {
						max = max_tmp;
						cutoff = max; //now can use this for pruning future iterations
					}
				}
			}
			if( max_tmp > max_bound) {  //if it is > cutoff, would have computed max, so it is a better bound than cutoff
				max_bound = max_tmp;
			}

            if( !itr.next()) { break;}
        }


		if( max < 0) { //if didn't find any, know that actualMPE value below this node is <= cutoff
			max = -max_bound;
//			if( max_bound > cutoff) { throw new IllegalStateException( "max_bound: " + max_bound + "  cutoff:" + cutoff);}
		}

		left.clearContextIndexing(); right.clearContextIndexing(); //clear "baseline indexing"
        return max;
	}




	final double recCondMPE4( double cutoff) {
		double ret;

		if( !cache.cached) {
			ret = recCondMPE4_NoCache( cutoff);
		}
		else {

			ret = cache.lookupMPE4_actual();

			if( ret >= 0.0) {
				rc.counters.callsToRCInternalCacheHit++;
			}
			else if( ret == RCNodeCache.ShouldBeCached) {
				rc.counters.callsToRCInternalComputeAndSave++;

				ret = cache.lookupMPE4_bound(); //possibly have a precomputed bound to prune with

				if( ret <= cutoff && ret >= 0) { //have bound & can prune
					ret = -ret;
					cache.lastIndex = -1;
					cache.numInstVars = -1;
				}
				else {
					ret = recCondMPE4_NoCache( cutoff);
					if( ret >= 0) {
						cache.addToCacheMPE4( ret);  //save the value for next time
					}
					else { //negative means actual is <= -ret
						cache.addToCacheMPE4Bound( -ret);
					}
				}
			}
			else {
				ret = recCondMPE4_NoCache( cutoff);
				cache.lastIndex = -1;
				cache.numInstVars = -1;
			}
		}
		return ret;
	}


	final double recCondMPE4_NoCache( double cutoff) {

		double leftLA = left.lookAheadMPE4();
		double rightLA = right.lookAheadMPE4();
		double la = leftLA * rightLA;
		if( la <= cutoff) {
			return -la;
		}

        rc.counters.callsToRCInternalCompute++;

		boolean leftFirst;
		if( RC.TODO_REMOVE_REVERSE_ORDER == false) {
			//Go down the largest first
			if( leftLA >= rightLA) { leftFirst = true;}  //TODO: Heuristic ordering (other may be better?)
			else { leftFirst = false;}
		}
		else {
			//Go down the smallest first
			if( leftLA < rightLA) { leftFirst = true;}  //TODO: Heuristic ordering (other may be better?)
			else { leftFirst = false;}
		}

		RCNode n1, n2;
		double la1, la2;
		if( leftFirst) {
			n1 = left;  la1 = leftLA;
			n2 = right; la2 = rightLA;
		}
		else {
			n1 = right; la1 = rightLA;
			n2 = left;  la2 = leftLA;
		}

		double max = -1;
		double max_bound = -1;

		if(RC.TODO_REMOVE_DO_LOOKAHEAD_LOWERBOUND) {
			double low = lookAheadMPE_lowerBound();
			if( low >= cutoff) {
				max = low;
				cutoff = low;
			}
			if( (la1*la2)== low) {
				left.clearContextIndexing(); right.clearContextIndexing(); //clear "baseline indexing"
				return low;
			}
		}


		if( !itr.setInitialState()) {
			return 0;
		}

		while( true) {

			double tempLA2;
			if( RC.TODO_REMOVE_DO_EXTRA_LOOKAHEAD) {
				tempLA2 = n2.lookAheadMPE4(); //slightly more restricted than la2 (since have instantiated cutset now)
			}
			else {
				tempLA2 = la2;
			}

			double max_tmp = tempLA2 * la1;
			if( max_tmp > cutoff) {

				double actualL1 = n1.recCondMPE4( cutoff / tempLA2);
				max_tmp = actualL1 * tempLA2;

				if( max_tmp < 0) { //was pruned
					max_tmp = -max_tmp;
					if( (max_tmp > cutoff) && (max_tmp - cutoff < .00001)) { max_tmp = cutoff;} //adjust roundoff error
				}
				else if( max_tmp > cutoff) {

					double actualL2 = n2.recCondMPE4( cutoff / actualL1);
					max_tmp = actualL1 * actualL2;

					if( max_tmp < 0) { //was pruned
						max_tmp = -max_tmp;
						if( (max_tmp > cutoff) && (max_tmp - cutoff < .00001)) { max_tmp = cutoff;} //adjust roundoff error
					}
					else if( max_tmp > max && max_tmp >= cutoff) {
						max = max_tmp;
						cutoff = max; //now can use this for pruning future iterations
					}
				}
			}
			if( max_tmp > max_bound) {
				max_bound = max_tmp;
			}

			if( !itr.next()) { break;}
		}//end while

		left.clearContextIndexing(); right.clearContextIndexing();

		if( max < 0) {
			max = -max_bound;
		}
		return max;

	}//end recCondMPE4_NoCache



	final double recCond() {
		double ret;
		boolean addToCache = false;

		if( cache.cached) {
			ret = cache.lookupRC();
			if( ret >= 0.0 || Double.isNaN(ret)) {
				rc.counters.callsToRCInternalCacheHit++;
				return ret;
			}
			else if( ret == RCNodeCache.ShouldBeCached) {
				addToCache = true;
			}
		}


		{//Test for stop or pause
			if( rc.computationUserRequest != RC.COMPUTATION_RUN) {
				if( rc.computationUserRequest == RC.COMPUTATION_STOP) { //early termination for threaded version
					throw new RC.RCUserRequestedStop();
				}
				else if( rc.computationUserRequest == RC.COMPUTATION_PAUSE )
				{
					try{
						rc.waitForResume();
					}
					catch( InterruptedException e ){
	                    System.err.println( "Thread interrupted during RCNodeInternalBinaryCache.recCond()" );
					}
				}
			}
		}


		{
			double s = rc.scalar;

			rc.counters.callsToRCInternalCompute++;

			if( !itr.setInitialState()) { ret = 0.0;}
			else {

				ret = left.recCond() * right.recCond(); //works for scaled or not

				while( itr.next()) {

					if( s != 1.0) { //USE_SCALING

						// ret^scalar = ret^scalar + recCond^scalar
						// C^S = A^S + B^S

						//use log sum equation to compute ln(A^S + B^S) from ln(A^S)=S*ln(A) and ln(B^S)=S*ln(B)
						double s_mult_lna = s * Math.log(ret);

						double c_c = left.recCond() * right.recCond(); //works for scaled or not
						double s_mult_lnb = s * Math.log( c_c);

						//if lna=-inf && lnb=-inf, want value to be 0, not -inf as logsum computes
						if( s_mult_lna == Double.NEGATIVE_INFINITY && s_mult_lnb == Double.NEGATIVE_INFINITY) {
							//ret += 0;
						}
						else {
							double logsum = TableScaled.logsum( s_mult_lna, s_mult_lnb);
							//then S ln(C) = ln(A^S+B^S)
							// C = exp(ln(A^S+B^S)/S)
							ret = Math.exp( logsum / s);
						}
					}
					else {
						ret += (left.recCond() * right.recCond()); //works for scaled or not
					}
				}
			}
		}


		if( addToCache) {
            rc.counters.callsToRCInternalComputeAndSave++;
			cache.addToCacheRC( ret);
		}
		return ret;
	}









	/////////////////
	//  Helper Classes
	/////////////////

	public class ChildIterator extends RCIterator {
		int i=0;

		public ChildIterator() {}

		public boolean hasNext() {
			if( i < 2) { return true;} //0 returns Left, 1 returns Right
			else { return false;}
		}

		public Object next() {
			return nextNode();
		}

		public RCNode nextNode() {
			if( i==0) { i++; return left;}
			else if( i==1) { i++; return right;}
			else {
				throw new NoSuchElementException();
			}
		}

		public void restart() {i=0;}
	}

}//end class RCNodeInternalBinaryCache
