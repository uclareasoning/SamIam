package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;


/** This class represents "compiled" dgraph objects used in recursive conditioning
 *    (it is a set of dtree objects which can share structure).
 *
 * @author David Allen
 */
final public class RCDgraph extends RC {

    static final private boolean DEBUG_RCDgraph = false;

    private Map varToRoot; //this could possibly return a null value if it doesn't support family marginals & variable not in any cutsets
                           //not all variables have to be in this mapping (e.g. if reduce graph with subset of vars)
    private boolean canComputeFamilyMarginals = true;


	/** Warning: Using this constructor will not create the complete structure.  You must
	  *  eventually call setRoots to finish creating and initializing it.
	  */
	public RCDgraph( RCCreationParams params) {
		super( params);
	}


	/** Creates a new RCDgraph based on the parameters.
	*
	* @param bn A valid BeliefNetwork to use.
	* @param cs A valid CachingScheme to use.
	* @param listnr A Listener or else null.
	* @param scalar Usually 1.0, but for genetics can scale it (slowing down computations).
	* @param useKB Uses UnitResolution in a KnowledgeBase during computations.
	*
	* @throws RCCreationException if could not create a RCDgraph.
	*/
	public RCDgraph( RCCreationParams params, CachingScheme cs, CachingScheme.RCCreateListener listnr,
			DecompositionStructureUtils.ParamsGraph crm_par)
	{
		super( params);

		Assert.notNull( cs, "RCDgraph: cs cannot be null");

		crm_par.rcDg = this;
		DecompositionStructureUtils.createStructure( crm_par);

		if( roots == null) throw new RCCreationException("RCDgraph creation exception.");

		for( int i=0; i<roots.length; i++) {
			roots[i].isRoot = true;
		}


		if( listnr != null ) listnr.rcConstructionDone();
		cs.allocateMemory( this, listnr);

		if( DEBUG_RCDgraph) {
		//             long m = numCacheEntries_All();
		//             System.out.println("cache: " + m + " * 8 = " +  NumberFormat.getInstance().format(m*8));
		//             System.out.println("global cf = " + cs.getCacheFactor());
		}
	}


	public void setRoots( Collection rts, boolean callInit, Map varToRoot, boolean canComputeFamilyMarginals) {
		roots = new RCNode[rts.size()];
		roots = (RCNode[])rts.toArray( roots);
		{
			ArrayList eo = new ArrayList();
			RCUtilities.getElimOrderUnInitialized( roots[0], Collections.EMPTY_SET, eo); //order all contexts using the MPE ordering (root[0])
			for( int i=0; i<roots.length; i++) {
				roots[i].isRoot = true;
				if( i==0) {
					if( callInit) {
						roots[i].init( Collections.EMPTY_SET, Collections.EMPTY_SET, true, eo);
					}
					else {
						roots[i].initCacheOrder( eo);
					}
				}
				else {
					if( callInit) {
						roots[i].init( Collections.EMPTY_SET, Collections.EMPTY_SET, false, eo);
					}
					else {
						roots[i].initCacheOrder( eo);
					}
				}
			}
		}
		this.varToRoot = varToRoot;
		this.canComputeFamilyMarginals = canComputeFamilyMarginals;
		changeRoots();
	}

	static public Thread allocateRCDgraphInThread( final RCDgraph dg,
							final CachingScheme cs,
							final CachingScheme.RCCreateListener listnr,
							final double seed_bestCost, final Map seed_cf)
	{
		Thread ret = new Thread()
		{
			public void run()
			{
				if( listnr != null) { //if null, don't do anything
					try {
						listnr.rcConstructionDone();
						if( seed_cf == null) {
							cs.allocateMemory( dg, listnr);
						}
						else {
							((CachingDFBnB)cs).allocateMemory( dg, listnr, seed_bestCost, seed_cf);
						}
						listnr.rcCreateDone( dg);
					}
					catch( RCCreationException e) {
						//listnr.rcCreateDone( null);
						listnr.rcCreateError( e.getMessage() );
						System.err.println( e );
					}
					catch( Exception e) {
						System.err.println("An exception was thrown during dgraph creation: " + e);
						if( Definitions.DEBUG ) {
							e.printStackTrace( System.err);
						}
						//listnr.rcCreateDone( null);
						listnr.rcCreateError( e.getMessage() );
						System.err.println( e );
					}
					catch( StackOverflowError error ){
						listnr.rcCreateError( STR_STACKOVERFLOW_MSG );
						System.err.println( error );
					}
					catch( OutOfMemoryError error ){
						listnr.rcCreateError( STR_OUTOFMEMORY_MSG );
						System.err.println( error );
					}
				}
			}
		};
		ret.start();
		return ret;
	}



	/**This will create a thread in which an RCDgraph will be created (by using the listnr
	 *  parameter, the search for CacheSchemes can be monitored and stopped which will return
	 *  the best result found so far (if the cachingScheme supports it).
	 *
	 *  <p>It catches RCCreationException and will call rcCreateError() ------rcCreateDone(null) if that happens.
	 */
	static public Thread createRCDgraphInThread( final RCCreationParams params,
												 final CachingScheme cs,
												 final CachingScheme.RCCreateListener listnr,
												 final DecompositionStructureUtils.ParamsGraph crm_par)
	{
		Thread ret = new Thread()
		{
			public void run()
			{
				if( listnr != null) { //if null, will never get results back so don't do anything
					try {
						RCDgraph rcDg = new RCDgraph( params, cs, listnr, crm_par);
						listnr.rcCreateDone( rcDg);
					}
					catch( RCCreationException e) {
						//listnr.rcCreateDone( null);
						listnr.rcCreateError( e.getMessage() );
						System.err.println( e );
					}
					catch( Exception e) {
						System.err.println("An exception was thrown during dgraph creation: " + e);
						if( Definitions.DEBUG ) {
							e.printStackTrace( System.err);
						}
						//listnr.rcCreateDone( null);
						listnr.rcCreateError( e.getMessage() );
						System.err.println( e );
					}
					catch( StackOverflowError error ){
						listnr.rcCreateError( STR_STACKOVERFLOW_MSG );
						System.err.println( error );
					}
					catch( OutOfMemoryError error ){
						listnr.rcCreateError( STR_OUTOFMEMORY_MSG );
						System.err.println( error );
					}
				}
			}
		};
		ret.start();
		return ret;
	}


	public boolean canComputeFamilyMarginals() { return canComputeFamilyMarginals;}


    public RCIterator getIteratorRoots() { return new RCIteratorArray( roots, roots);}
    /**Iterator only over the first root*/
    public RCIterator getIteratorTree() { return new RCIteratorTraversal( roots[0]);}
    public RCIteratorTraversal getIteratorTraversal() { return new RCIteratorTraversal( roots);}
    public RCNode getAnyRoot() { return roots[0];} //return 0, as that is what is used for MPE


    public Map varToRoot() { return Collections.unmodifiableMap(varToRoot);}


	/** Reduce this graph to one which (possibly) has fewer roots, but can only compute variable marginals instead
	 *  of being able to compute family marginals.  Cannot eliminate root[0] if it contains the MPE information.*/
	public void reduceToOnlyMarginals( Collection varsOfInterest) {

		if( canComputeFamilyMarginals == false) {
			throw new IllegalStateException("Cannot reduce a graph which is already reduced.");
			//otherwise when you reduce it the second time, a variable may not be in a cutset,
			//  but that doesn't mean it can be computed from only a cpt...
		}

		canComputeFamilyMarginals = false;
		HashSet nodesCovered[] = new HashSet[roots.length];
		HashMap locVarToRoot = new HashMap();

		for( int i=0; i<roots.length; i++) {
			if( !roots[i].isLeaf()) {
				nodesCovered[i] = ((RCNodeInternalBinaryCache)roots[i]).getCutset( null);
				nodesCovered[i].retainAll( varsOfInterest);
			}
			else {
				//root should only be a leaf if it is the only node in the network
				if( roots.length != 1) { throw new IllegalStateException("bad root");}
				return;
			}
		}

		ArrayList retRoots = new ArrayList();  //Need this to be ordered so that when forceRootZero, it ends up RootZero
		HashSet myVarsOfInterest = new HashSet( varsOfInterest);

		boolean forceRootZero = false;
		//root[0] is an internal node, since it checked for leaf roots above
		if( ((RCNodeInternalBinaryCache)roots[0]).vars_to_save_mpe != null) {
			if( Definitions.DEBUG ) { Definitions.STREAM_VERBOSE.println("forceRootZero during reduction");}
			forceRootZero = true;
		}

		while( myVarsOfInterest.size() > 0) { //while desire marginals over variables

			int bestCutsetIndx = -1;
			int bestCutsetSize = -1;

			//find root which has a maximal covering over varsOfInterest
			if( forceRootZero) {
				forceRootZero = false;
				bestCutsetIndx = 0;
				bestCutsetSize = Math.max( nodesCovered[0].size(), 1); //need it at least 1 so it is included.
			}
			else {
				for( int i=0; i<nodesCovered.length; i++) {
					if( nodesCovered[i] == null) { continue;}

					if( nodesCovered[i].size() >= bestCutsetSize) {
						bestCutsetIndx = i;
						bestCutsetSize = nodesCovered[i].size();
					}
				}
			}

			if( bestCutsetSize > 0) {
				//add root to return
				retRoots.add( roots[bestCutsetIndx]);

				//remove all vars in its cutset from myVarsOfInterest
				myVarsOfInterest.removeAll( nodesCovered[bestCutsetIndx]);

				for( Iterator itr = nodesCovered[bestCutsetIndx].iterator(); itr.hasNext();) {
					locVarToRoot.put( itr.next(), roots[bestCutsetIndx]);
				}

				//reduce the nodesCovered
				for( int i=0; i<nodesCovered.length; i++) {
					if( nodesCovered[i] == null) { continue;}

					nodesCovered[i].removeAll( nodesCovered[bestCutsetIndx]);
				}

				//remove nodes covered for best (size is 0)
				nodesCovered[bestCutsetIndx] = null;
			}
			else { //some vars may not be in a cutset, must map them to a root which contains their entire family
				for( Iterator itr = myVarsOfInterest.iterator(); itr.hasNext();) {
					FiniteVariable fv = (FiniteVariable)itr.next(); //var of interest (not in any cutsets)
					HashSet family = new HashSet( fv.getCPTShell( fv.getDSLNodeType() ).variables());
					family.remove( fv);

					int rootIndx = -1;
					int cutsetVars = Integer.MAX_VALUE;

					for( int i=0; i<roots.length; i++) {
						HashSet root_cutset = ((RCNodeInternalBinaryCache)roots[i]).getCutset( null);

						if( root_cutset.containsAll( family)) {
							if( retRoots.contains( roots[i])) { //if already selected use it & return
								rootIndx = -1;
								cutsetVars = -1;
								locVarToRoot.put( fv, roots[i]);
								itr.remove();
								break;
							}
							else { //remember it (if better than any other remembered one) and possibly use it later
								if( root_cutset.size() < cutsetVars) {
									rootIndx = i;
									cutsetVars = root_cutset.size();
								}
							}
						}
					}//end iterate through roots

					if( rootIndx >= 0) { //never found one already selected, therefore add the one that was remembered
						retRoots.add( roots[rootIndx]);
						locVarToRoot.put( fv, roots[rootIndx]);
						itr.remove();
					}
				} //end vars of interest
				if( !myVarsOfInterest.isEmpty()) { throw new IllegalStateException("myVarsOfInterest !empty");}
			}
		}

		if( Definitions.DEBUG ) { Definitions.STREAM_VERBOSE.println("Reduced the number of roots from " + roots.length + " to " + retRoots.size());}

		if( roots.length > 0 && retRoots.size() == 0) {
			retRoots.add( roots[0]);  //RCDgraph expects at least one root, so can't reduce all of them (e.g. need P(e) computation)
		}

		for( int i=0; i<roots.length; i++) {
			if( !retRoots.contains( roots[i])) {
				roots[i].removeParentAndSelfRecursive( null);
			}
		}

		//reset roots and varToRoot
		setRoots( retRoots, false, locVarToRoot, /*canComputeFamilyMarginals*/false);
	}



    public void recCond_PreCompute() {
		if( isRunning() ) {
			stopAndWaitRecCondAsThread();
			if( isRunning() ) throw new IllegalStateException( STR_EXCEPTION_EVIDENCE );
		}

		startComputation();

		NumberFormat nf = NumberFormat.getNumberInstance();

		if( outputConsole != null) {
			outputConsole.println("Precomputing Marginals");
			outputConsole.println("Number of Root Nodes: " + roots.length);
			double cacheen = statsAll().numCacheEntries();
			outputConsole.println("Number of Cache Entries: " + nf.format(cacheen) + " (" + cacheen*8.0/1024.0/1024.0 + " Mb)");
			if( kb != null) {
				outputConsole.println("KB Size: " + kb.numClauses() + " : " + kb.numLiterals() + "   (totalClauses:totalLiterals)");
			}
		}

		try{

			long sysT_beg, sysT_end;
			long cpuT_beg, cpuT_end;

			sysT_beg = System.currentTimeMillis();
			cpuT_beg = JVMProfiler.getCurrentThreadCpuTimeMS();

			for( int i=0; i<roots.length; i++) {
				roots[i].recCondCutsetJoint();
			}

			sysT_end = System.currentTimeMillis();
			cpuT_end = JVMProfiler.getCurrentThreadCpuTimeMS();


			if( outputConsole != null) {
				double secSys = (sysT_end - sysT_beg)/1000.0;
				double secCPU = (cpuT_end - cpuT_beg)/1000.0;

				outputConsole.println("Calls RC Actual: \t" + nf.format(counters.totalCalls()));
				outputConsole.println("(Ellapsed) Sec:  \t" + secSys + "\t   calls/sec: \t" + (secSys!=0?(counters.totalCalls()/secSys):0));
				outputConsole.println("(CPU) Sec:       \t" + secCPU + "\t   calls/sec: \t" + (secCPU!=0?(counters.totalCalls()/secCPU):0));
				outputConsole.println("");
			}

		}catch( RuntimeException e ) {
			endComputation();
			resetRC(); //need to keep RC object in a valid state
			throw e;
		}

		endComputation();
	}



    public double[] recCond_All() {
		return recCond_All( true, true, true);
	}


    /** Run Recursive conditioning P(e) for each root node.
     *
     *  <p>Note: It is possible for the returned value to be NaN if the network contains that in a cpt.
     */
    public double[] recCond_All( boolean runAllRoots, boolean runWithKB, boolean runNoKB) {

		final boolean DEBUG_OUT = true;
		double ret[] = null;

		//For testing, clear all caches, then run rc on all roots (with and without KB), timing total time

		NumberFormat nf = NumberFormat.getNumberInstance();

		if( useKBflag() && kb == null) { createKB();} //pre-create this so its not in the timings

		if( DEBUG_OUT) {
			outputInfo("\n\nDtree: " + statsPe().numNodes() + "  Dgraph: " + statsAll().numNodes() + "  roots: " + roots.length);

			outputInfo("Calls RC Expected (all roots): " + nf.format((long)statsAll().expectedNumberOfRCCalls()));
			outputInfo("Calls RC Expected (single root): " + nf.format((long)statsPe().expectedNumberOfRCCalls()));

			if( kb != null) {
				outputInfo("KB Size: " + kb.numClauses() + " : " + kb.numLiterals() + "   (totalClauses:totalLiterals)");
			}
		}

		if( runWithKB && kb != null) {
			startComputation();
			ret = new double[roots.length];

			long sysT[] = new long[3];
			long cpuT[] = new long[3];

			sysT[0] = System.currentTimeMillis();
			cpuT[0] = JVMProfiler.getCurrentThreadCpuTimeMS();

			ret[0] = roots[0].recCond();

			sysT[1] = System.currentTimeMillis();
			cpuT[1] = JVMProfiler.getCurrentThreadCpuTimeMS();

//			suspendKB();

			long count0 = counters.totalCalls();
			if( runAllRoots) {
				for( int i=1; i<roots.length; i++) {
					ret[i] = roots[i].recCond();
				}
			}

			sysT[2] = System.currentTimeMillis();
			cpuT[2] = JVMProfiler.getCurrentThreadCpuTimeMS();

			endComputation();

//			resumeKB();

			if( DEBUG_OUT) {
				double secSys = (sysT[2] - sysT[0]) / 1000.0;
				double sec1Sys = (sysT[1] - sysT[0]) / 1000.0;

				double secCPU = (cpuT[2] - cpuT[0]) / 1000.0;
				double sec1CPU = (cpuT[1] - cpuT[0]) / 1000.0;

				outputInfo("\nKB:");
				if( runAllRoots) {
					outputInfo("Calls RC Actual: \t" + nf.format(counters.totalCalls()));
					outputInfo("(Ellapsed) Sec:  \t" + secSys + "\t   calls/sec: \t" + (counters.totalCalls()/secSys));
					outputInfo("(CPU) Sec:       \t" + secCPU + "\t   calls/sec: \t" + (counters.totalCalls()/secCPU));
				}
				outputInfo("Time for RC on root[0] (Ellapsed sec): \t" + sec1Sys + "\tcalls: \t" + count0);
				outputInfo("Time for RC on root[0] (CPU sec): \t" + sec1CPU + "\tcalls: \t" + count0);
//				outputInfo("" + counters);
			}
		}


		//"remove" KB, call recCond on roots and then put it back

		if( runNoKB) {

			suspendKB();
			reSynchEvidWithBN(true);
			resetRC();

			startComputation();
			ret = new double[roots.length];

			long sysT[] = new long[3];
			long cpuT[] = new long[3];

			sysT[0] = System.currentTimeMillis();
			cpuT[0] = JVMProfiler.getCurrentThreadCpuTimeMS();

			ret[0] = roots[0].recCond();

			sysT[1] = System.currentTimeMillis();
			cpuT[1] = JVMProfiler.getCurrentThreadCpuTimeMS();

			long count0 = counters.totalCalls();
			if( runAllRoots) {
				for( int i=1; i<roots.length; i++) {
					ret[i] = roots[i].recCond();
				}
			}

			sysT[2] = System.currentTimeMillis();
			cpuT[2] = JVMProfiler.getCurrentThreadCpuTimeMS();

			endComputation();

			if( DEBUG_OUT) {
				double secSys = (sysT[2] - sysT[0]) / 1000.0;
				double sec1Sys = (sysT[1] - sysT[0]) / 1000.0;

				double secCPU = (cpuT[2] - cpuT[0]) / 1000.0;
				double sec1CPU = (cpuT[1] - cpuT[0]) / 1000.0;

				outputInfo("\nnoKB:");
				if( runAllRoots) {
					outputInfo("Calls RC Actual: \t" + nf.format(counters.totalCalls()));
					outputInfo("(Ellapsed) Sec:  \t" + secSys + "\t   calls/sec: \t" + (counters.totalCalls()/secSys));
					outputInfo("(CPU) Sec:       \t" + secCPU + "\t   calls/sec: \t" + (counters.totalCalls()/secCPU));
				}
				outputInfo("Time for RC on root[0] (Ellapsed sec): \t" + sec1Sys + "\tcalls: \t" + count0);
				outputInfo("Time for RC on root[0] (CPU sec): \t" + sec1CPU + "\tcalls: \t" + count0);
	//			outputInfo("" + counters);
			}

			resumeKB();
			resetRC();
		}

        return toRealPr( ret);
    }


    public Table joint( FiniteVariable var) {
		return cutsetJoint( var).project( Collections.singleton( var));
    }

	public Table familyJoint( FiniteVariable var )
	{
		if( !canComputeFamilyMarginals) {
			throw new IllegalStateException("This graph cannot compute family Joints");
		}

		Table res = cutsetJoint( var);
		return res;
	}


	private Table cutsetJoint( FiniteVariable var) {
		boolean DEBUG_loc = false;

		if( isRunning() ) {
			stopAndWaitRecCondAsThread();
			if( isRunning() ) throw new IllegalStateException( STR_EXCEPTION_EVIDENCE );
		}

		startComputation();

		Table res = null;

		try{
			int indx = vars.indexOf( var);
			if( indx < 0 ) throw new IllegalArgumentException( var + " not found." );
			int value = instantiation[indx];

			if( !varToRoot.containsKey( var)) {
				throw new IllegalArgumentException("Graph does not contain information about variable " + var);
			}

			RCNode rt = (RCNode)varToRoot.get( var); //possibly var not in a cutset
//			if( rt != null) {
				res = rt.recCondCutsetJoint();  //pr( par, ?var?, evid)
				if( DEBUG_loc) { Definitions.STREAM_VERBOSE.println("cutset joint: " + res);}
//			}

			if( res.index().variableIndex(var) == -1) { //var not in cutset joint, so add

				if( value >= 0) { //has evidence set (only when var added)
					FiniteVariable v[] = {var};
					Table evid = new Table(v);

					if( evid.getCPLength() != var.size()) { throw new IllegalStateException();}
					evid.fill( 0);
					evid.setCP(value, 1);
					if( res != null) {
						res = res.multiply( evid);
					}
					else {
						res = evid;
					}
					if( DEBUG_loc) { Definitions.STREAM_VERBOSE.println("had evidence on variable, cutset joint: " + res);}
				}
				else {
					Table cpt = var.getCPTShell( var.getDSLNodeType() ).getCPT();
					if( DEBUG_loc) { Definitions.STREAM_VERBOSE.println("cpt(var): " + cpt);}
					res = res.multiply( cpt);
					if( DEBUG_loc) { Definitions.STREAM_VERBOSE.println("full cutset joint: " + res);}
				}
			}
		}catch( RuntimeException e ) {
			endComputation();
			resetRC(); //need to keep RC object in a valid state
			throw e;
		}

		endComputation();
		return res;
	}




	public String getTagName() { return "rcdgraph"; }
	public static String getStaticTagName() { return "rcdgraph"; }
}//end class RCDgraph

