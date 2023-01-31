package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;

/** This class represents "compiled" dtree objects used in recursive conditioning.
 *
 * @author David Allen
 */
final public class RCDtree extends RC {

    static final private boolean DEBUG_RCDtree = false;

	{
		roots = new RCNode[1];
	}


	/** Warning: Using this constructor will not create the complete structure.  You must
	  *  eventually call setRoot to finish creating and initializing it.
	  */
	public RCDtree( RCCreationParams params) {
		super( params);
	}

    /** Creates a new RCDtree based on the parameters.
     *
     * @param bn A valid BeliefNetwork to use.
     * @param cs A valid CachingScheme to use.
     * @param listnr A Listener or else null.
     * @param scalar Usually 1.0, but for genetics can scale it (slowing down computations).
     * @param useKB Uses UnitResolution in a KnowledgeBase during computations.
     * @param includeMPE If true, allows MPE calculations to be run on this dtree, otherwise they cannot.
     *
     * @throws RCCreationException if could not create a RCDtree.
     */
    public RCDtree( RCCreationParams params, CachingScheme cs, CachingScheme.RCCreateListener listnr,
					DecompositionStructureUtils.ParamsTree crm_par) {

        super( params);
        Assert.notNull( cs, "RCDtree: cs cannot be null");

		crm_par.rcDt = this;
		DecompositionStructureUtils.createStructure( crm_par);

        if( roots == null || roots.length != 1 || roots[0] == null) throw new RCCreationException("RCDtree creation exception.");
        roots[0].isRoot = true;


		if( listnr != null ) listnr.rcConstructionDone();
        cs.allocateMemory( this, listnr);

        if( DEBUG_RCDtree) {
//             long m = numCacheEntries_All();
//             System.out.println("cache: " + m + " * 8 = " +  NumberFormat.getInstance().format(m*8));
//             System.out.println("global cf = " + cs.getCacheFactor());
        }
    }


	public void setRoot( RCNode rt, boolean callInit) {
		roots[0] = rt;
		ArrayList eo = new ArrayList();
		RCUtilities.getElimOrderUnInitialized( roots[0], Collections.EMPTY_SET, eo);
        roots[0].isRoot = true;

		if( callInit) {
			roots[0].init( Collections.EMPTY_SET, Collections.EMPTY_SET, true, eo);
		}
		else {
			roots[0].initCacheOrder( eo);
		}
	}


    static public Thread allocateRCDtreeInThread( final RCDtree dt,
                                                  final CachingScheme cs,
                                                  final CachingScheme.RCCreateListener listnr,
                                                  final double seed_bestCost, final Map seed_cf) {

        Thread ret = new Thread() {
                public void run() {
                    if( listnr != null) { //if null, don't do anything
                        try {
                        	listnr.rcConstructionDone();
                            if( seed_cf == null) {
                                cs.allocateMemory( dt, listnr);
                            }
                            else {
                                ((CachingDFBnB)cs).allocateMemory( dt, listnr, seed_bestCost, seed_cf);
                            }
                            listnr.rcCreateDone( dt);
                        }
                        catch( RCCreationException e) {
                            //listnr.rcCreateDone( null);
                            listnr.rcCreateError( e.getMessage() );
                            System.err.println( e );
                        }
                        catch( Exception e) {
                            System.err.println("An exception was thrown during dtree creation: " + e);
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
                    }
                }
            };
        ret.start();
        return ret;
    }


    /**This will create a thread in which an RCDtree will be created (by using the listnr
     *  parameter, the search for CacheSchemes can be monitored and stopped which will return
     *  the best result found so far (if the cachingScheme supports it).
     *
     *  <p>It catches RCCreationException and will call rcCreateError() ------rcCreateDone(null) if that happens.
     */
    static public Thread createRCDtreeInThread( final RCCreationParams params,
                                                final CachingScheme cs,
                                                final CachingScheme.RCCreateListener listnr,
												final DecompositionStructureUtils.ParamsTree crm_par) {

        Thread ret = new Thread() {
                public void run() {
                    if( listnr != null) { //if null, will never get results back so don't do anything
                        try {
                            RCDtree rcDt = new RCDtree( params, cs, listnr, crm_par);
                            listnr.rcCreateDone( rcDt);
                        }
                        catch( RCCreationException e) {
                            //listnr.rcCreateDone( null);
                            listnr.rcCreateError( e.getMessage() );
                            System.err.println( e );
                        }
                        catch( Exception e) {
                            System.err.println("An exception was thrown during dtree creation: " + e);
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
                    }
                }
            };
        ret.start();
        return ret;
    }




    public RCIterator getIteratorRoots() { return new RCIteratorArray( roots, roots);}
    public RCIterator getIteratorTree() { return getIterator();}
    public RCIteratorTraversal getIteratorTraversal() { return new RCIteratorTraversal( roots[0]);}
    public RCNode getAnyRoot() { return roots[0];}




    public void recCond_PreCompute() {
		if( isRunning() ) {
			stopAndWaitRecCondAsThread();
			if( isRunning() ) throw new IllegalStateException( STR_EXCEPTION_EVIDENCE );
		}

		startComputation();

		NumberFormat nf = NumberFormat.getNumberInstance();

		if( outputConsole != null) {
			outputConsole.println("Precomputing Values on dtree");
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

			roots[0].recCond();

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
		final boolean DEBUG_OUT = true;

		final boolean runWithKB = true;
		final boolean runNoKB = true;

		//For testing, clear all caches, then run rc on all roots (with and without KB), timing total time

		NumberFormat nf = NumberFormat.getNumberInstance();

		if( useKBflag() && kb == null) { createKB();} //pre-create this so its not in the timings

//		resetRC();
		if( DEBUG_OUT) {
			outputInfo( "\n\nDtree: " + statsPe().numNodes() + "  Dgraph: " + statsAll().numNodes() + "  roots: 1");
			outputInfo( "Calls RC Expected (single root): " + nf.format((long)statsPe().expectedNumberOfRCCalls()));

			if( kb != null) {
				outputInfo( "KB Size: " + kb.numClauses() + " : " + kb.numLiterals() + "   (totalClauses:totalLiterals)");
			}
		}

		double ret[] = new double[1];

		if( runWithKB && kb != null) {

			startComputation();

			long st = System.currentTimeMillis();
			ret[0] = roots[0].recCond();
			long en1 = System.currentTimeMillis();
			long count0 = counters.totalCalls();

			endComputation();

			if( DEBUG_OUT) {
				double sec1 = (en1 - st) / 1000.0;
				outputInfo("\nKB:");
				outputInfo("Time (Ellapsed) for RC on roots[0] (sec): \t" + sec1 + "\tcalls: \t" + count0);
	//			outputInfo("" + counters());
			}
		}

		if( runNoKB) {
			//"remove" KB, call recCond on roots and then put it back

			KnowledgeBase mykb = kb;
			kb = null;
			reSynchEvidWithBN(true);
			resetRC();

			startComputation();

			long st = System.currentTimeMillis();
			ret[0] = roots[0].recCond();
			long en1 = System.currentTimeMillis();
			long count0 = counters.totalCalls();

			endComputation();

			if( DEBUG_OUT) {
				double sec1 = (en1 - st) / 1000.0;
				outputInfo("\nnoKB:");
				outputInfo("Time (Ellapsed) for RC on roots[0] (sec): \t" + sec1 + "\tcalls: \t" + count0);
	//			outputInfo("" + counters());
			}

			kb = mykb;
			resetRC();
		}

        return toRealPr( ret);
	}







    //IO FUNCTIONS
	public String getTagName() { return "rcdtree"; }
	public static String getStaticTagName() { return "rcdtree"; }
}//end class RCDtree
