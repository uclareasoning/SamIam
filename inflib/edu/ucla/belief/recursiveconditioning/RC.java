package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;







/* Info about KnowledgeBases in RC
 *
 * The function useKB() will set a flag that tells RC to create a KB before its next
 *   computation, however it doesn't do it immediately (see startComputation).
 * The function clearKB() will delete all references to the KB immediately & turn the useKBflag off.
 * The function createKB() will actually create a KB immediately & set the useKBflag.
 *
 * Can suspend and resume the use of the KB through suspendKB and resumeKB.  (do this with caution)
 *
 * The constructor will actually create the KB if its useKB parameter
 *  is set, rather than waiting.  In order to have it do it later, the user could pass
 *  the false flag in and then call useKB().
 *
 */






/** This abstract class helps with removing code from the RCDgraph and RCDtree classes.
 *
 * @author David Allen
 */
abstract public class RC
	implements KnowledgeBase.KnowledgeBaseListener {

    static final private boolean DEBUG_RC = false;
    static final private boolean DEBUG_RC_Threads = false;
    static final public boolean DEBUG_RC_CALC = false;
    static final public boolean DEBUG_RC_EVID = false;

	static public final String STR_STACKOVERFLOW_MSG = "Stack overflow. Try java command line parameter '-Xss2m'.";
	static public final String STR_OUTOFMEMORY_MSG = "Out of memory. Make sure to use samiam.bat.\nYou may need to increase '-Xmx'.";

static public boolean TODO_REMOVE_REVERSE_ORDER = false;
static public boolean TODO_REMOVE_DO_PROP_UP = true;
static public boolean TODO_REMOVE_DO_EXTRA_LOOKAHEAD = true;
static public boolean TODO_REMOVE_DO_LOOKAHEAD_LOWERBOUND = false;

/**This variable is only used during RC creation, not dynamically!*/
static public boolean TODO_REMOVE_DO_LEAF_CACHING_DURING_CREATION = false;

/**This variable needs to be set during RC creation and stay the same during computations!*/
static public boolean TODO_REMOVE_DO_INDEX_CACHING_DURING_CREATION = false;


	public PrintWriter outputConsole = null;
    final public MappedList vars; //ordering of variables in instantiation

    final public RCCounters counters = new RCCounters();
    final protected BeliefNetwork myBeliefNetwork;
	final public double scalar; //usually it should be 1.0, however it can be larger
								//when dealing with very small probabilities that you
								//don't want to underflow (see TableScaled)



	protected RCNode roots[]; //array can change, as graphs create new ones when new roots
							//are set, however trees always keep the same array (but this could
							//change in the future).

	/**
		@author Keith Cascio
		@since 060303
	*/
	public boolean canComputeFamilyMarginals() { return false; }
	public Map varToRoot() { return Collections.EMPTY_MAP; }



    //run recCond as thread
    protected Thread myThreadRunning;
    private boolean isRunning = false;

    /*package*/ static final int COMPUTATION_RUN   = 0;
    /*package*/ static final int COMPUTATION_STOP  = 1;
    /*package*/ static final int COMPUTATION_PAUSE = 2;
    /*package*/ int computationUserRequest = COMPUTATION_RUN;


	public Thread getThreadRunning() { return myThreadRunning;}



	static public class RCCreationParams {
		public double scalar = 1.0;
		public boolean useKB = false;
		public boolean allowKB = true; //if you pass this to constructor as false, you can never use the KB with this RC object
										//cannot have useKB = true & allowKB = false
		public BeliefNetwork bn = null;

		public RCCreationParams() {}
	}



    /** RC Object constructor.
     *
     *  <p>Initializes the variables
     */
    public RC( RCCreationParams params) {

        Assert.notNull( params.bn, "RC: bn cannot be null");


		this.scalar = params.scalar;
		this.allowKB = params.allowKB;

		if( params.useKB && !params.allowKB) { throw new IllegalStateException("Cannot have useKB=true and allowKB=false");}

		vars = new MappedList( params.bn );
		myBeliefNetwork = params.bn;

        instantiation = new int[vars.size()];
	   	Arrays.fill( instantiation, -1);  //set all variables to no evidence/state

	   	instantiation_tmp = new int[vars.size()]; //TODO eventually remove all references to this array.

        if( params.useKB) { initKB(/*resetEvid*/false);}
    }




    public boolean isRunning() { return isRunning;}

    public void resetStatistics() { counters.init();}
    public String getStatistics() { return counters.toString();}
    public String getStatistics( boolean shortStr) { return counters.toString( shortStr);}



    public RCIterator getIterator() { return getIteratorParentChild();}
    abstract public RCIteratorTraversal getIteratorTraversal();
    abstract public RCIterator getIteratorRoots();
    abstract public RCIterator getIteratorTree();
    abstract public RCNode getAnyRoot();


	void changeRoots() {
		parentChildOrdering = null;
	}

    private RCNode[] parentChildOrdering = null;
    final public RCIterator getIteratorParentChild() {
        if( parentChildOrdering == null) {
            parentChildOrdering = parentChildOrdering();
        }
        return new RCIteratorArray( parentChildOrdering, parentChildOrdering);
    }


	public void allocRCCaches() { allocCaches( 0, getIterator());}
	public void allocRCMPECaches() { allocCaches( 1, getIteratorTree());}
	public void allocRCMPE3Caches() { allocCaches( 3, getIteratorTree());}
	public void allocRCMPE4Caches() { allocCaches( 4, getIteratorTree());}
	private void allocCaches( int type, RCIterator itr) {
		while( itr.hasNext()) {
			if( type == 0) {
				itr.nextNode().allocRCCaches();
			}
			else if( type == 1) {
				itr.nextNode().allocRCMPECaches();
			}
			else if( type == 3) {
				itr.nextNode().allocRCMPE3Caches();
			}
			else if( type == 4) {
				itr.nextNode().allocRCMPE4Caches();
			}
			else {
				throw new IllegalArgumentException("Illegal Type " + type);
			}
		}
	}


    /** Run Recursive conditioning P(e) for each root node (special for testing & output of timing/memory...).
     *
     *  <p>Note: It is possible for the returned value to be NaN if the network contains that in a cpt.
     */
    abstract public double[] recCond_All();


    /** Run RC for each root node to "precompute" the marginals.
     *  <p>This is meant mostly for dgraphs, where the marginal is saved in the root.  For a dtree, it will
     *   just fill up the caches, but not at the root node.
     *
     *  <p>Note: It is possible for the returned value to be NaN if the network contains that in a cpt.
     */
    abstract public void recCond_PreCompute();


	//Nodes with empty contexts are never counted
	//if rtChildAsOne = true the root children are counted as one (assuming neither is a leaf node).
	//if rtChildAsOne = false the root children are NOT counted at all
	public double limitedContexts( boolean rtChildAsOne) {
		RCIterator itrrt = getIteratorRoots();
		Set rts = new HashSet();
		while( itrrt.hasNext()) {
			rts.add( itrrt.nextNode());
		}

		boolean seenOneRtChild = false;

		RCIterator itr = getIterator();
        double ret = 0;
        while( itr.hasNext()) {
			RCNode nd = itr.nextNode();

			if( !nd.isLeaf()) {

				if( nd.contextInstantiations() != 1) {

					//count root children as one (if both are non-leafs), otherwise they don't count
					if( nd.parentNodes().size() == 1 && rts.contains( nd.parentNodes().iterator().next())) {
						if( rtChildAsOne) {
							if( !seenOneRtChild) { seenOneRtChild = true;}
							else {ret += nd.contextInstantiations();}
						}
					}

					else {
			            ret += nd.contextInstantiations();
					}
				}
			}
        }
        return ret;
	}

	public double allContexts() {
		RCIterator itr = getIterator();
        double ret = 0;
        while( itr.hasNext()) {
            ret += itr.nextNode().contextInstantiations();
        }
        return ret;
	}

	public double allContextsMinusLeafs() {
		RCIterator itr = getIterator();
        double ret = 0;
        while( itr.hasNext()) {
			RCNode nd = itr.nextNode();
			if( !nd.isLeaf()) {
	            ret += nd.contextInstantiations();
			}
        }
        return ret;
	}


    /**Number of Caches being used by this RC object.*/
    public double numCacheEntries_MPE() {
        double ret = 0;
        RCIterator itr = getIteratorTree();
        while( itr.hasNext()) {
            ret += itr.nextNode().numCacheEntries_local_used();
        }
        return ret;
    }


    public void resetRC() {
		if( DEBUG_RC) { Definitions.STREAM_VERBOSE.println("RC.resetRC");}
        //notify nodes
        RCIterator itr = getIterator();
        while( itr.hasNext()) {
            itr.nextNode().resetLocal();
        }
        if( kb != null) {
			reSynchEvidWithBN(true); //revert back to only evid
		}
    }


	public Map getCFMap() {
		HashMap ret = new HashMap();
		for( RCIterator itr = getIterator(); itr.hasNext();) {
			RCNode n = itr.nextNode();
			ret.put( n, new Double(n.getCacheFactor()));
		}
		return ret;
	}




	final static boolean DEBUG_KB_LISTENER = false;

	/* Get information from the KB.*/
	final public void assertLearnedPositive( int fv, int state) {
		if( DEBUG_KB_LISTENER) { Definitions.STREAM_VERBOSE.println("RC.assertLearnedPositive: " + vars.get(fv) + " = " +
								((FiniteVariable)vars.get(fv)).instance(state));
		}
		//since from KB, don't call setInst because don't want to tell KB about it
		instantiation[fv] = state;
	}
//	public void assertLearnedNegative( int fv, int state) {} //for now do not do anything with this
//	public void assertUnLearnedNegative( int fv, int state) {} //for now do not do anything with this
	final public void assertUnLearnedPositive( int fv) {
		if( DEBUG_KB_LISTENER) { Definitions.STREAM_VERBOSE.println("RC.assertUnLearnedPositive: " + vars.get(fv));}
		//since from KB, don't call setInst because don't want to tell KB about it
		instantiation[fv] = -1;
	}
	final public void kbDontCallAssertOnVar( int fv) {
		kbCallAssert[fv] = false;
	}
	final public void kbCallAssertOnVar( int fv) {
		kbCallAssert[fv] = true;
	}



    /** Run Recursive conditioning P(e).
     *
     *  <p>Note: It is possible for the returned value to be NaN if the network contains that in a cpt.
     */
    public  double recCond_Pe_Scaled() {
		double ret;
        startComputation();
        try{
	        ret = getAnyRoot().recCond();
		}
		catch( RuntimeException e) {
			System.gc();
	        endComputation();
//			System.err.println("Exception thrown: " + e);
			resetRC(); //need to keep RC object in a valid state
			throw e;
		}
        endComputation();
//		ret = toRealPr(ret);
		if( DEBUG_RC_CALC) { Definitions.STREAM_VERBOSE.println("Pe (scaled): " + ret);}
		return ret;
    }


    /** Run Recursive conditioning P(e).
     *
     *  <p>Note: It is possible for the returned value to be NaN if the network contains that in a cpt.
     */
    public double recCond_Pe() {

		double ret = recCond_Pe_Scaled();
		ret = toRealPr(ret);
		return ret;
    }

    /** Run Recursive conditioning MPE.
     *
     *  <p>Note: It is possible for the returned value to be NaN if the network contains that in a cpt.
     */
    public double recCond_MPE( Map varToValue) {
		double ret;
        startComputation();

        int[] inst = new int[vars.size()];
        Arrays.fill( inst, -1);
        try {
	        ret = getAnyRoot().recCondMPE1( inst);
		}
		catch( RuntimeException e) {
			System.gc();
	        endComputation();
//			System.err.println("Exception thrown: " + e);
			resetRC(); //need to keep RC object in a valid state
			throw e;
		}

        //convert inst array back to a map
        for( int i=0; i<vars.size(); i++) {
            FiniteVariable fv = (FiniteVariable)vars.get(i);
            int state = inst[i];
            if( state >= 0) {
                varToValue.put( fv, fv.instance( state));
            }
            else {
                varToValue.put( fv, "Error Encountered in Network.");
            }
        }

        endComputation();
		ret = toRealPr(ret);
		if( DEBUG_RC_CALC) { Definitions.STREAM_VERBOSE.println("Pmpe: " + ret);}
		return ret;
    }

    /** Run Recursive conditioning MPE3.
     *
     *  <p>Note: It is possible for the returned value to be NaN if the network contains that in a cpt.
     */
    public double recCond_MPE3( double cutoff) {
		double ret;
        startComputation();

        try {
			RCNode rt = getAnyRoot();
			if( rt.cache() != null) {
				rt.cache().numInstVars = 0;
				rt.cache().lastIndex = 0;
			}
	        ret = rt.recCondMPE3( cutoff);
		}
		catch( RuntimeException e) {
			System.gc();
	        endComputation();
//			System.err.println("Exception thrown: " + e);
			resetRC(); //need to keep RC object in a valid state
			throw e;
		}

        endComputation();
		ret = toRealPr(ret);
		if( DEBUG_RC_CALC) { Definitions.STREAM_VERBOSE.println("Pmpe3: " + ret);}
		return ret;
    }

    /** Run Recursive conditioning MPE4.
     *
     *  <p>Note: It is possible for the returned value to be NaN if the network contains that in a cpt.
     */
    public double recCond_MPE4( double cutoff) {
		double ret;
        startComputation();

        try {
			RCNode rt = getAnyRoot();
			if( rt.cache() != null) {
				rt.cache().numInstVars = 0;
				rt.cache().lastIndex = 0;
			}
	        ret = rt.recCondMPE4( cutoff);
		}
		catch( RuntimeException e) {
			System.gc();
	        endComputation();
//			System.err.println("Exception thrown: " + e);
			resetRC(); //need to keep RC object in a valid state
			throw e;
		}

        endComputation();
		ret = toRealPr(ret);
		if( DEBUG_RC_CALC) { Definitions.STREAM_VERBOSE.println("Pmpe4: " + ret);}
		return ret;
    }


	/** Run recCond_Pe in a separate thread, and call finished.rcFinished( double) when done.
	 *  This function can be stopped by using stopAndWaitRecCondAsThread and passing in the
	 *  returned value from this function.  In this case, finished.rcFinished is NOT
	 *  called.
	 *
	 *  <p>Since the only way to determine a result is through finished, if it is null nothing
	 *  will be done.
	 */
	public void recCond_PeAsThread( final RecCondThreadListener finished) {
		recCond_PeAsThread( finished, true);
	}
	/** Run recCond_Pe_Scaled in a separate thread, and call finished.rcFinished( double) when done.
	 *  This function can be stopped by using stopAndWaitRecCondAsThread and passing in the
	 *  returned value from this function.  In this case, finished.rcFinished is NOT
	 *  called.
	 *
	 *  <p>Since the only way to determine a result is through finished, if it is null nothing
	 *  will be done.
	 */
	public void recCond_Pe_ScaledAsThread( final RecCondThreadListener finished) {
		recCond_PeAsThread( finished, false);
	}

	/** Run recCond_Pe in a separate thread, and call finished.rcFinished( double) when done.
	 *  This function can be stopped by using stopAndWaitRecCondAsThread and passing in the
	 *  returned value from this function.  In this case, finished.rcFinished is NOT
	 *  called.
	 *
	 *  <p>Since the only way to determine a result is through finished, if it is null nothing
	 *  will be done.
	 */
	protected void recCond_PeAsThread( final RecCondThreadListener finished, final boolean nonScaled )
	{
		if( finished != null )
		{
			//System.out.println( Thread.currentThread().getName() + " recCond_PeAsThread()" );
			//new Throwable().printStackTrace();

			stopAndWaitRecCondAsThread();

			if( myThreadRunning != null && myThreadRunning.isAlive() ) throw new IllegalStateException( "seems like join() didn't work." );

			myThreadRunning = new RecCondPeThread( finished, nonScaled );
			if( DEBUG_RC_Threads ) { Definitions.STREAM_VERBOSE.println("Starting RC Thread: " + myThreadRunning);}
			myThreadRunning.start();

			//return myThreadRunning;
		}
	}

	static protected int theCounterThreadCreation = (int)0;

	public static final double DOUBLE_OUTOFMEMORY = (double)-12345678;

	protected class RecCondPeThread extends Thread
	{
		protected RecCondThreadListener finished;
		protected boolean nonScaled;

		public RecCondPeThread( final RecCondThreadListener finished, final boolean nonScaled )
		{
			setName( "RecCondPeThread" + Integer.toString( theCounterThreadCreation++ ) );
			//System.out.println( getName() + "()" );
			this.finished = finished;
			this.nonScaled = nonScaled;
		}

		public void run()
		{
			//System.out.println( getName() + ".run()" );
			try{

				if( useKBflag && kb == null) { createKB();} //pre-create this so its not in the timings
				allocRCCaches();

				long start_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
				long start_ms = System.currentTimeMillis();

				double result = -1;

				try{
					if( nonScaled ) result = recCond_Pe();
					else result = recCond_Pe_Scaled();
				}
				catch( OutOfMemoryError e ){
					if( Definitions.DEBUG ) {
						Runtime rt = Runtime.getRuntime();
						System.err.println( "Free Mem: " + rt.freeMemory() +
											", Total Mem: " + rt.totalMemory());
					}
					System.gc();
					endComputation();
					resetRC(); //need to keep RC object in a valid state
					System.err.println( "Warning: recursive conditioning out of memory." );
					result = DOUBLE_OUTOFMEMORY;
				}
				//catch( RuntimeException e) {
				//endComputation();
				//System.err.println("Exception thrown: " + e);
				//resetRC(); //need to keep RC object in a valid state
				//throw e;
				//}

				long end_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
				long end_ms = System.currentTimeMillis();

				long threadCPUMS = end_cpu_ms - start_cpu_ms;
				if( DEBUG_RC_Threads) Definitions.STREAM_VERBOSE.println( "threadCPU MS: " + threadCPUMS );
				long elapsedMillis = end_ms - start_ms;
				if( DEBUG_RC_Threads) Definitions.STREAM_VERBOSE.println( "elapsedMillis: " + elapsedMillis );

				long answerMillis = 0;
				if( Definitions.DEBUG ) {  //Can disable this by setting it to true
					threadCPUMS = 0; //on linux the profiler appears to be incorrect, so "disable" the profiler
				}
				if( threadCPUMS == 0 ) answerMillis = elapsedMillis;
				else answerMillis = threadCPUMS;

				finished.rcFinished( result, answerMillis );
				//new Herald( finished, result, answerMillis ).start();
			}
			catch( RCUserRequestedStop e) {
				//ignore (Can exit by throwing a RCUserRequestedStop)
				endComputation();
				resetRC(); //need to keep RC object in a valid state
			}
			catch( OutOfMemoryError error ){
				if( Definitions.DEBUG ) {
					Runtime rt = Runtime.getRuntime();
					System.err.println( "Free Mem: " + rt.freeMemory() + ", Total Mem: " + rt.totalMemory());
				}
				System.gc();
				endComputation();
				resetRC(); //need to keep RC object in a valid state
				System.err.println( "Warning: RecCondPeThread out of memory." );
				finished.rcComputationError( STR_OUTOFMEMORY_MSG );
			}
			if( DEBUG_RC_Threads) { Definitions.STREAM_VERBOSE.println("Finished RC Thread: " + Thread.currentThread());}
			computationUserRequest = COMPUTATION_RUN;// may have been stopped or paused
			//System.out.println( Thread.currentThread().getName() + " isRunning = false1" );
			isRunning = false; //was probably set by recCond, but if the exception was thrown it still
							   // needs to be reset
			//myThreadRunning = null;
		}
	}

	/** Run rEccond_Mpe In A Separate Thread, And Call Finished.Rcfinishedmpe( Double, Map) When Done.
	*  This Function Can Be Stopped By Using Stopandwaitreccondasthread And Passing In The
	*  Returned Value From This Function.  In This Case, Finished.Rcfinishedmpe Is Not
	*  Called.
	*
	*  <P>Since The Only Way To Determine A Result Is Through Finished, If It Is Null Nothing
	*  Will Be Done.
	*/
	public Thread recCond_MPEAsThread( final RecCondThreadListener finished )
	{
		if( finished == null ) return null;
		else
		{
			stopAndWaitRecCondAsThread();

			if( myThreadRunning != null && myThreadRunning.isAlive() ) throw new IllegalStateException( "seems like join() didn't work." );

			myThreadRunning = new RecCondMPEThread( finished );
			if( DEBUG_RC_Threads) { Definitions.STREAM_VERBOSE.println("Starting MPE Thread: " + myThreadRunning); }
			myThreadRunning.start();

			return myThreadRunning;
		}
	}

	protected class RecCondMPEThread extends Thread
	{
		protected RecCondThreadListener finished;

		public RecCondMPEThread( final RecCondThreadListener finished )
		{
			setName( "RecCondMPEThread" + Integer.toString( theCounterThreadCreation++ ) );
			//System.out.println( getName() + "()" );
			this.finished = finished;
		}

		public void run()
		{
			try{

				if( useKBflag && kb == null) { createKB();} //pre-create this so its not in the timings
				allocRCMPECaches();

				long start1_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
				long start1_ms = System.currentTimeMillis();

				double result1 = -1;
				Map varToValue1 = new HashMap();

				try{
					//myThreadRunning = this;
					result1 = recCond_MPE( varToValue1);
				}
				catch( OutOfMemoryError e ){
					if( Definitions.DEBUG ) {
					Runtime rt = Runtime.getRuntime();
					System.err.println( "Free Mem: " + rt.freeMemory() +
					", Total Mem: " + rt.totalMemory());
				}
				System.gc();
				endComputation();
				resetRC(); //need to keep RC object in a valid state
				System.err.println( "Warning: recursive conditioning out of memory." );
				}
				//                            catch( RuntimeException e) {
				//						        endComputation();
				//								System.err.println("Exception thrown: " + e);
				//								resetRC(); //need to keep RC object in a valid state
				//								throw e;
				//							}

				long end1_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();
				long end1_ms = System.currentTimeMillis();

				long threadCPUMS = end1_cpu_ms - start1_cpu_ms;
				if( DEBUG_RC_Threads) Definitions.STREAM_VERBOSE.println( "mpe:threadCPU MS: " + threadCPUMS );
				long elapsedMillis = end1_ms - start1_ms;
				if( DEBUG_RC_Threads) Definitions.STREAM_VERBOSE.println( "mpe:elapsedMillis: " + elapsedMillis );

				long answerMillis = 0;
				if( threadCPUMS == 0 ) answerMillis = elapsedMillis;
				else answerMillis = threadCPUMS;

				finished.rcFinishedMPE( result1, varToValue1, answerMillis );
			}
			catch( RCUserRequestedStop e) {
				//ignore (Can exit by throwing a RCUserRequestedStop)
				endComputation();
				resetRC(); //need to keep RC object in a valid state
			}
			catch( OutOfMemoryError error ){
				if( Definitions.DEBUG ) {
					Runtime rt = Runtime.getRuntime();
					System.err.println( "Free Mem: " + rt.freeMemory() + ", Total Mem: " + rt.totalMemory());
				}
				System.gc();
				endComputation();
				resetRC(); //need to keep RC object in a valid state
				System.err.println( "Warning: RecCondMPEThread out of memory." );
				finished.rcComputationError( STR_OUTOFMEMORY_MSG );
			}

			if( DEBUG_RC_Threads) { Definitions.STREAM_VERBOSE.println("Finished MPE Thread: " + Thread.currentThread());}
			computationUserRequest = COMPUTATION_RUN;// may have been stopped or paused
			//System.out.println( Thread.currentThread().getName() + " isRunning = false2" );
			isRunning = false; //was probably set by recCond, but if the exception was thrown it still
			// needs to be reset
			//myThreadRunning = null;
		}
	}

	protected void startComputation()
	{
		if( isRunning) { throw new IllegalStateException("Cannot run two computations at once");}
		if( DEBUG_RC_CALC || DEBUG_RC) { Definitions.STREAM_VERBOSE.println("startComputation");}

		if( useKBflag && kb == null) { createKB();}

		isRunning = true;
		//System.out.println( Thread.currentThread().getName() + " isRunning = true" );
		resetStatistics();
	}

	protected void endComputation()
	{
		if( DEBUG_RC_CALC || DEBUG_RC) { Definitions.STREAM_VERBOSE.println("endComputation");}
		//System.out.println( Thread.currentThread().getName() + " isRunning = false3" );
		isRunning = false;
		//myThreadRunning = null;
	}

	/**
		@author Keith Cascio
		@since 072402
	*/
	public synchronized void pauseRecCondAsThread()
	{
		computationUserRequest = COMPUTATION_PAUSE;
	}

	/*package*/ synchronized void waitForResume() throws InterruptedException
	{
		wait();
	}

	/**
		@author Keith Cascio
		@since 072402
	*/
	public synchronized void resumeRecCondAsThread()
	{
		if( computationUserRequest == COMPUTATION_PAUSE )
		{
			computationUserRequest = COMPUTATION_RUN;
			notifyAll();
		}
		else if( Definitions.DEBUG ) {
			System.err.println( "RC.resumeRecCondAsThread() called but no calculation was paused." );
		}
	}

	/** Will stop any running functions and wait for them to finish.
	* @param waitFor A non-Null thread (running ...AsThread) to wait for it to finish
	*/
	public  void stopAndWaitRecCondAsThread()
	{
		if( myThreadRunning != null && myThreadRunning.isAlive() )
		{
			try{
				computationUserRequest = COMPUTATION_STOP;// may have been stopped or paused

				//System.out.println( Thread.currentThread().getName() + " joining (stopping) on " + myThreadRunning.getName() );
				myThreadRunning.join();

				//should not get here unless exeption was thrown or thread finished
				if( isRunning != false) { throw new IllegalStateException("isRunning");}
			}
			catch( InterruptedException e) {
				System.err.println( "Warning: RC.stopAndWaitRecCondAsThread() join() interrupted/" );
			}
		}
	}

	public static final String STR_EXCEPTION_EVIDENCE = "Cannot change evidence during a computation";

	/**Returns the evidence to the state of no observations.*/
	public void resetEvidence()
	{
		if( isRunning)
		{
			stopAndWaitRecCondAsThread();
			if( isRunning) throw new IllegalStateException( STR_EXCEPTION_EVIDENCE );
		}

		if( DEBUG_RC_EVID) { Definitions.STREAM_VERBOSE.println("resetEvidence");}

		Arrays.fill( instantiation, -1);  //set all variables to no evidence

		//notify nodes
		for( RCIterator itr = getIterator(); itr.hasNext(); ) {
			itr.nextNode().resetLocal();
		}

		if( kb != null) kb.retract( kbDefault);
	}

	/**Add var=value to the list of observations.*/
	public void observe(FiniteVariable var, Object value)
	{
		if( isRunning)
		{
			stopAndWaitRecCondAsThread();
			if( isRunning) throw new IllegalStateException( STR_EXCEPTION_EVIDENCE );
		}

		if( value == null) { unobserve( var); return;}

		if( DEBUG_RC_EVID) { Definitions.STREAM_VERBOSE.println("observe: " + var + " = " + value);}

		int indx = vars.indexOf( var);
		int val = var.index( value);

		if( val < 0) { throw new IllegalArgumentException("State not found: " + var + " - " + value);}
		int oldvalue = instantiation[indx];
		if( oldvalue == val) { return;} //already set (possibly by KB)

		instantiation[indx] = val;

		//notify nodes
		RCIterator itr = getIterator();
		while( itr.hasNext()) {
			RCNode nd = itr.nextNode();
			if( nd.isLeaf()) {
				((RCNodeLeaf)nd).observe( indx, val);
			}
		}
		if( kb != null && kbCallAssert[indx]) {
			if( oldvalue < 0) { //just learn new thing, as it was uninitialized
				int ret = kb.assertPositive( indx, val);
				if( ret == KnowledgeBase.KB_UNSATISFIABLE) {
					//if evidence sets this, not much I can do other than remove KB (as it won't match RC anymore)
					System.err.println("KnowledgeBase removed because Evidence was inconsistent (" + var + " = " + value + ")");
					clearKB();
				}
			}
			else { //have to "unlearn" old & then learn new
				reSynchEvidWithBN(true); //evid is not set&retracted in order, so clear KB & add all again
			}
		}
	}

	public void setCPT(FiniteVariable var) {
		if( isRunning)
		{
			stopAndWaitRecCondAsThread();
			if( isRunning) throw new IllegalStateException( STR_EXCEPTION_EVIDENCE );
		}

		if( DEBUG_RC_EVID) { Definitions.STREAM_VERBOSE.println("setCPT: " + var);}

		int indx = vars.indexOf( var);

		//notify nodes
		RCIterator itr = getIterator();
		while( itr.hasNext()) {
			RCNode nd = itr.nextNode();
			if( nd.isLeaf()) {
				((RCNodeLeaf)nd).setCPT( indx);
			}
		}
		if( kb != null) {
			initKB(/*resetEvid*/true); //create a new KB, as the cpt may have changed the KB
		}
	}


	/*Sets the state of the variable to unobserved.*/
	public void unobserve(FiniteVariable var)
	{
		if( isRunning)
		{
			stopAndWaitRecCondAsThread();
			if( isRunning) throw new IllegalStateException( STR_EXCEPTION_EVIDENCE );
		}

		if( DEBUG_RC_EVID) { Definitions.STREAM_VERBOSE.println("unobserve: " + var);}

		int indx = vars.indexOf( var);
		if( Definitions.DEBUG ) {
			if( indx < 0 ||
				indx >= vars.size()) {
				throw new IllegalArgumentException("RC: var out of bounds (not found).");
			}
		}
		instantiation[indx] = -1;
		//notify nodes
		RCIterator itr = getIterator();
		while( itr.hasNext()) {
			RCNode nd = itr.nextNode();
			if( nd.isLeaf()) {
				((RCNodeLeaf)nd).unobserve( indx);
			}
		}
		if( kb != null) {
			reSynchEvidWithBN(true); //evid is not set&retracted in order, so clear KB & add all again
		}
	}


	/** Should not normally be called from outside the package, usually
	 *  it should be handled by the inference engine.
	 *  Usually resetEvidence is true, but the constructor cannot call it.
	 */
	public void reSynchEvidWithBN( boolean resetEvid) { //evid is not set&retracted in order, so clear KB & add all again
		if( resetEvid) {
			resetEvidence(); //will reset evidence & retractKB
		}

		Map evid = myBeliefNetwork.getEvidenceController().evidence();
		for( Iterator itr = evid.keySet().iterator(); itr.hasNext();) {
			FiniteVariable fv = (FiniteVariable)itr.next();
			observe( fv, evid.get(fv));
		}
	}







    //MISC SUB-CLASSES

    public static class RCCreationException extends RuntimeException {
        public RCCreationException( String msg) {
            super( msg);
        }
    }



    final static public class RCCounters {
//         public long callsToRC; //Internal + leaf
//         public long callsToRCInternal; //= compute + cacheHit
        public long callsToRCInternalCompute;
        public long callsToRCInternalComputeAndSave;
        public long callsToRCInternalCacheHit;
        public long callsToRCLeaf;
        public long callsToLookAhd;

        public RCCounters() {
            init();
        }

		public long totalCalls() { return callsToRCInternalCompute+callsToRCInternalCacheHit+callsToRCLeaf;}
		public long totalCallsPlusLkAhd() { return callsToRCInternalCompute+callsToRCInternalCacheHit+callsToRCLeaf+callsToLookAhd;}

        public void init() {
            callsToRCInternalCompute = 0;
            callsToRCInternalComputeAndSave = 0;
            callsToRCInternalCacheHit = 0;
            callsToRCLeaf = 0;
            callsToLookAhd = 0;
        }

        public String toString() {
			NumberFormat fmt = NumberFormat.getIntegerInstance();

			StringBuffer ret = new StringBuffer();
            ret.append("calls to RC: " + fmt.format(totalCalls()) + "\n");
            ret.append("calls to RC Internal: " +
                       fmt.format(callsToRCInternalCompute+callsToRCInternalCacheHit) + "=" +
                       fmt.format(callsToRCInternalCompute) + "(compute) +" +
                       fmt.format(callsToRCInternalCacheHit) + "(CacheHit) ::" +
                       fmt.format(callsToRCInternalComputeAndSave) + "(computeAndSave)\n");
            ret.append("calls to RC Leaf: " + fmt.format(callsToRCLeaf));
            ret.append("calls to RC LookAhead: " + fmt.format(callsToLookAhd));

            return ret.toString();
        }

        public String toStringShort() {
			NumberFormat fmt = NumberFormat.getIntegerInstance();
			String ret = new String("calls to RC: " + fmt.format(totalCalls()) + "\t calls to RCLookAhead: " + fmt.format(callsToLookAhd) + "\n");
            return ret;
        }

        public String toString( boolean shortStr) {
			if( shortStr) {return toStringShort();}
			else { return toString();}
		}
    }


	final /*package*/ static class RCUserRequestedStop extends RuntimeException {
        public RCUserRequestedStop( ) {
            super( "Was asked to stop.");
        }
	}



	public RCStats statsPe() { return new RCStats_Pe();} //TODO: eventually want to store the object and only recalc when necessary
	public RCStats statsAll() { return new RCStats_All();} //TODO: eventually want to store the object and only recalc when necessary



	abstract public class RCStats {
		final static public int NOT_COMPUTED_int = -1;
		final static public double NOT_COMPUTED_dbl = -1;
		final static public double entriesToMB = 8.0/1048576.0;  //entries*8/1024/1024

		protected int numNodes;
		protected double numCacheEntries;
		protected double expectedNumberOfRCCalls;

		/**Create an RCStats object without doing any computations yet.*/
		public RCStats(){ reset();}

		/**Warning: If you don't know the value of one of the parameters, you must pass in
		 * NOT_COMPUTED_int or NOT_COMPUTED_dbl, otherwise these classes will not work.
		 */
		public RCStats( int numNodes, double numCacheEntries, double expectedNumberOfRCCalls) {
			this.numNodes = numNodes;
			this.numCacheEntries = numCacheEntries;
			this.expectedNumberOfRCCalls = expectedNumberOfRCCalls;
		}

		abstract protected RCIterator itrToUse();
		abstract protected RCIterator itrToUseRCCalls();



		public void reset() {
			numNodes = NOT_COMPUTED_int;
			numCacheEntries = NOT_COMPUTED_dbl;
			expectedNumberOfRCCalls = NOT_COMPUTED_dbl;
		}

		public void calculateStats() {
			numNodes();
			numCacheEntries();
			expectedNumberOfRCCalls();
		}

		final public int numInternalNodes() {
			RCIterator itr = itrToUse();
			int ret = 0;
			while( itr.hasNext()) {
				RCNode nd = itr.nextNode();
				if( !nd.isLeaf()) {
					ret++;
				}
			}
			return ret;
		}
		final public int numLeafNodes() {
			RCIterator itr = itrToUse();
			int ret = 0;
			while( itr.hasNext()) {
				RCNode nd = itr.nextNode();
				if( nd.isLeaf()) {
					ret++;
				}
			}
			return ret;
		}

		final public int numNodesCached() {
			RCIterator itr = itrToUse();
			int ret = 0;
			while( itr.hasNext()) {
				RCNode nd = itr.nextNode();
				if( nd.getCacheFactor() != 0) {
					ret++;
				}
			}
			return ret;
		}

		final public int numNodesNotCached() {
			RCIterator itr = itrToUse();
			int ret = 0;
			while( itr.hasNext()) {
				RCNode nd = itr.nextNode();
				if( nd.getCacheFactor() == 0) {
					ret++;
				}
			}
			return ret;
		}


		final public int numNodes() {
			if( numNodes != NOT_COMPUTED_int) { return numNodes;}
			else {
				RCIterator itr = itrToUse();
				int ret = 0;
				while( itr.hasNext()) {
					ret++;
					itr.nextNode();
				}
				numNodes = ret;
				return numNodes;
			}
		}


		final public double numCacheEntriesMB() {
			return numCacheEntries() * entriesToMB;
		}

		final public double numCacheEntries() {
			if( numCacheEntries != NOT_COMPUTED_dbl) { return numCacheEntries;}
			else {
				RCIterator itr = itrToUse();
				double ret = 0;
				while( itr.hasNext()) {
					ret += itr.nextNode().numCacheEntries_local_used();
				}
				numCacheEntries = ret;
				return numCacheEntries;
			}
		}

		final public double expectedNumberOfRCCalls() {
			if( expectedNumberOfRCCalls != NOT_COMPUTED_dbl) { return expectedNumberOfRCCalls;}
			else {
				RCIterator itr = itrToUseRCCalls();

				if( parentChildOrdering == null) { //need just for the length
					parentChildOrdering = parentChildOrdering();
				}

				double ret = 0.0;

				MappedList nodes = new MappedList(); //nodes added from itr or from seeing parents or being roots
				double numRC[] = new double[parentChildOrdering.length];
				Arrays.fill( numRC, 0);

				{//initialize numRC for roots
					RCIterator itrR = getIteratorRoots();
					while( itrR.hasNext()) {
						RCNode node = itrR.nextNode();
						nodes.add( node);
						int indx = nodes.indexOf( node);
						numRC[indx] = 1;  //roots are called once (at most)
					}
				}

				while( itr.hasNext()) {
					RCNode node = itr.nextNode();
					nodes.add( node); //possibly add (this should actually always fail if its parents have been visited)
					int indx = nodes.indexOf( node);
					if( numRC[indx] <= 0) { throw new IllegalStateException("numRC["+indx+"]=" + numRC[indx]);}
					ret += numRC[indx]; //this nodes impact to numberOfRCCalls

					if( !node.isLeaf()) {


						double childCost = RCNode.expectedNumberOfRCCalls_local( node.cutsetInstantiations(),
																				 node.getCacheFactor(),
																				 node.contextInstantiations(),
																				 numRC[indx]);

						RCIterator chi_itr = ((RCNodeInternalBinaryCache)node).childIterator();
						while( chi_itr.hasNext()) {
							RCNode chi = chi_itr.nextNode();
							nodes.add( chi); //possibly add
							int indxC = nodes.indexOf( chi);
							nodes.add( chi);
							numRC[indxC] += childCost;
						}
					}
				}
				expectedNumberOfRCCalls = ret;
				return expectedNumberOfRCCalls;
			}
		}
	}


	/**Handle storing and computing computational statistics based on doing Pe computations.*/
	final public class RCStats_Pe extends RCStats {

		/**Create an RCStats object without doing any computations yet.*/
		public RCStats_Pe() {super();}

		/**Warning: If you don't know the value of one of the parameters, you must pass in
		 * NOT_COMPUTED_int or NOT_COMPUTED_dbl, otherwise the classes will not work.
		 */
		public RCStats_Pe( int numNodes, double numCacheEntries, double expectedNumberOfRCCalls) {
			super( numNodes, numCacheEntries, expectedNumberOfRCCalls);
		}

		protected RCIterator itrToUse() { return getIteratorTree();}
		protected RCIterator itrToUseRCCalls() { return getIteratorTree();}
	}

	/**Handle storing and computing computational statistics based on doing All computations
	 * (on graphs it would be for all marginals, and for trees it would be the same as Pe).
	 */
	final public class RCStats_All extends RCStats {

		/**Create an RCStats object without doing any computations yet.*/
		public RCStats_All() {super();}

		/**Warning: If you don't know the value of one of the parameters, you must pass in
		 * NOT_COMPUTED_int or NOT_COMPUTED_dbl, otherwise the classes will not work.
		 */
		public RCStats_All( int numNodes, double numCacheEntries, double expectedNumberOfRCCalls) {
			super( numNodes, numCacheEntries, expectedNumberOfRCCalls);
		}

		protected RCIterator itrToUse() { return getIterator();}
		protected RCIterator itrToUseRCCalls() { return getIteratorParentChild();}
	}







    //INTERFACES

    public interface RecCondThreadListener {
        public void rcFinished( double result, long time_ms);
        public void rcFinishedMPE( double result, Map instantiation, long time_ms);
        public void rcComputationError( String msg );
    }







    //HELPER FUNCTIONS

    /** Will return the state space of all the variables in the collection, and if the collection
     *  is empty it will return 1.
     */
    static final public long collOfFinVarsToStateSpace( Collection col) {
        long ret = 1;
        for( Iterator itr = col.iterator(); itr.hasNext();) {
            FiniteVariable fv = (FiniteVariable)itr.next();
            ret *= fv.size();

            if( fv.size() == 0) { throw new IllegalStateException("fv.size() == 0 " + fv);}
        }
        return ret;
    }


	final void outputInfo( String str) {
		Definitions.STREAM_VERBOSE.println(str);
		if( outputConsole != null) { outputConsole.println(str);}
	}


//    /** Will perform a reverse lookup in the array, looking for the value val and
//     *  returning the index of it.  This is useful for reversing the changes made
//     *  by the varStatesFVToInst array.  Will return -1 if val does not appear in
//     *  the array.
//     */
//    static final public int reverseArrayLookup( int[] array, int val) {
//        int ret = -1;
//        for( int i=0; i<array.length; i++) {
//            if( array[i] == val) {
//                ret = i;
//                break;
//            }
//        }
//        return ret;
//    }


	public double toRealPr( double in) {
		if( scalar != 1.0) {
			return TableScaled.toRealPr( in, scalar);
		}
		else {
			return in;
		}
	}

	public double[] toRealPr( double in[]) {
		if( scalar != 1.0) {
			return TableScaled.toRealPr( in, scalar);
		}
		else {
			return in;
		}
	}


	final public RCNode[] getRoots() { return (RCNode[])roots.clone();}
	final public RCNode root( int i) { return roots[i];}
	final public int numRoots() {
		if( roots == null) { return 0;}
		else { return roots.length;}
	}






    /** Guarantees a parent before a child.*/
    public RCNode[] parentChildOrdering() {

        MappedList nodes = new MappedList();
        int[] numPars = generateNumParents( nodes);
        ArrayList openNodes = new ArrayList();
        RCNode ret[] = new RCNode[nodes.size()];

        //initialize openNodes
        for( int i=0; i<numPars.length; i++) {
            if( numPars[i] == 0) { //includes all the roots
                openNodes.add( nodes.get(i));
            }
        }

        int indx = 0;
        while( !openNodes.isEmpty()) {
            RCNode n = (RCNode)openNodes.remove( openNodes.size()-1);
            ret[indx] = n; indx++;

            if( !n.isLeaf()) {

				RCIterator itr = ((RCNodeInternalBinaryCache)n).childIterator();
				while( itr.hasNext()) {
					RCNode chi = itr.nextNode();
					int indxC = nodes.indexOf( chi);
					numPars[indxC] -= 1;
					if( numPars[indxC] == 0) { openNodes.add( chi);}
				}
            }
        }

        if( indx != ret.length) { throw new IllegalStateException("ERROR");}
        return ret;
    }



    /** Will not change nds if it is non-null & non-empty*/
    public int[] generateNumParents( MappedList nds) {

        MappedList nodes = nds;
        if( nodes == null) {
            nodes = new MappedList();
        }
        if( nodes.isEmpty()) {
            RCIterator itrN = getIteratorTraversal(); //can't use getIterator because some of them ask for a parent ordering
            while( itrN.hasNext()) {
                nodes.add( itrN.nextNode());
            }
        }

        int numParents[] = new int[nodes.size()];
        Arrays.fill( numParents, 0);

        HashSet seen = new HashSet();
        RCIterator itrR = getIteratorRoots();
        while( itrR.hasNext()) {
            RCNode rt = itrR.nextNode();
            generateNumParents( rt, seen, nodes, numParents);
        }
        return numParents;
    }


    final protected void generateNumParents( RCNode nd, HashSet seen, MappedList nodes, int[] numParents) {
        //only visit a given node once (as a parent)
        if( seen.contains( nd)) { return;}
        else { seen.add( nd);}

        if( !nd.isLeaf()) {
            RCNodeInternalBinaryCache n = (RCNodeInternalBinaryCache)nd;

			RCIterator itr = n.childIterator();
			while( itr.hasNext()) {
				RCNode chi = itr.nextNode();
				int indxC = nodes.indexOf(chi);
				numParents[indxC] += 1;
				generateNumParents( chi, seen, nodes, numParents);
			}
        }
    }

	/**
		@author Keith Cascio
		@since 022603
	*/
	//abstract public org.w3c.dom.Document xmlize();// throws javax.xml.parsers.ParserConfigurationException;
	abstract public String getTagName();



    /**Get all the vars contained within a collection of RCNodes.*/
    static public Collection getVars( Collection A) {
        return getVars( A, null);
    }
    /**Get all the vars contained within a collection of RCNodes.*/
    static public Collection getVars( Collection A, Collection ret) {
        if( ret == null) { ret = new HashSet();}
        for( Iterator itr = A.iterator(); itr.hasNext();) {
            RCNode rn = (RCNode)itr.next();
            rn.vars( ret);
        }
        return ret;
    }





//Code for dealing with the instantiation array (BEGIN)

		/** In general, instantiation should only be changed through the setInst() function,
		 *  however it is public so that others can reference it without a function call.
		 */
	    final int[] instantiation; //the instantiation of each variable (-1 for no state)  (ordered by RC.vars)
	    final int[] instantiation_tmp; //the instantiation of each variable (-1 for no state)  (ordered by RC.vars)

		/** When using KnowledgeBase, be careful about how algorithms will react to KB making changes.
		 *
		 *  <p>Can only fail if using the KB, otherwise it only sets the inst array.
		 *
		 *  @returns false if KB failed to set the value.
		 */
		final public boolean setInst( int indx, int newvalue) {
			int oldvalue = instantiation[indx];
			instantiation[indx] = newvalue; //set the instantiation array

			if( kb != null ) { //if using KB
				if( oldvalue != newvalue) { //if it has changed
					if( newvalue == -1) { //removing evidence
						int s = kbRetract[indx];
						kbRetract[indx] = -1;
						if( s >= 0) {
							kb.retract( s);
						}
					}
					else if( kbCallAssert[indx]) { //setting evidence, but can disable KB for certain variables
						int ret = kb.assertPositive( indx, newvalue); //TODO if USE_FVState_Randomization, mixing two states here!
						if( ret == KnowledgeBase.KB_UNSATISFIABLE) {
							instantiation[indx] = oldvalue;
							return false;
						}
						kbRetract[indx] = ret;
					}
				}
			}
			return true;
		}

//Code for dealing with the instantiation array (END)






//Code for dealing with the KB (BEGIN)  (SEE COMMENTS AT TOP OF FILE)

	final public boolean allowKB;
	private boolean useKBflag;
	KnowledgeBase kb = null;
	private KnowledgeBase suspendedKB = null; //use with caution
	private int kbDefault=0;
	private int kbRetract[] = null;
	private boolean kbCallAssert[] = null;


	public KnowledgeBase knowledgeBase() { return kb;}
	public boolean useKBflag() { return useKBflag;}


	public void useKB() {
		if( isRunning) { throw new IllegalStateException("Cannot call RC.useKB during a computation");}
		if( !allowKB) { throw new IllegalStateException("Cannot call RC.useKB if allowKB was set to false");}
		useKBflag = true; //set the flag (a KB may already exist for it), but if not startComputation will create it before running
		suspendedKB = null;
	}

	public void createKB() {
		if( isRunning) { throw new IllegalStateException("Cannot call RC.createKB during a computation");}
		if( !allowKB) { throw new IllegalStateException("Cannot call RC.createKB if allowKB was set to false");}
		suspendedKB = null;
		if( kb == null) {
			initKB( /*resetEvid*/true);
		}
	}

	public void clearKB() {
		if( isRunning) { throw new IllegalStateException("Cannot call RC.clearKB during a computation");}
		useKBflag = false;
		kb = null;
		suspendedKB = null;
		kbRetract = null;
		kbCallAssert = null;
		reSynchEvidWithBN(true);
	}

	//usually resetEvidence is true, but the constructor cannot call it.
    private void initKB( boolean resetEvid) {
		if( !allowKB) { throw new IllegalStateException("Cannot call RC.initKB if allowKB was set to false");}
		if( DEBUG_RC || Definitions.DEBUG ) {Definitions.STREAM_VERBOSE.println("Creating a KB to use");}
		try {
			useKBflag = true;
			suspendedKB = null;

			kbCallAssert = new boolean[vars.size()];
			Arrays.fill( kbCallAssert, true);

			kbRetract = new int[vars.size()];
			Arrays.fill( kbRetract, -1);

			kb = KnowledgeBase.createFromBN( myBeliefNetwork, vars, this);
			kbDefault = kb.currentState();
			reSynchEvidWithBN(resetEvid);
		}
		catch( KnowledgeBase.KBUnsatisfiableStateException e) {
			System.err.println("Cannot use KnowledgeBase, as the initial one is inconsistent.");
			clearKB();
		}
	}

	/**Warning: This function should not really be public and should not be called from outside
	 *  of the package unless you are sure it will not lead to computation errors!
	 */
	public void suspendKB() {
		suspendedKB = kb;
		kb = null; //remove current
		useKBflag = false; //don't create any new ones
	}
	/**Warning: This function should not really be public and should not be called from outside
	 *  of the package unless you are sure it will not lead to computation errors!
	 */
	public void resumeKB() {
		if( suspendedKB != null) {
			kb = suspendedKB;
			useKBflag = true;
		}
		else {
			if( Definitions.DEBUG ) {System.err.println("Called resumeKB, but nothing was suspended.");}
			clearKB();
		}
	}
//Code for dealing with the KB (END)










}//end class RC
