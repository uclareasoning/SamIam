package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;



/*This class uses the RCCreateListener interface during its search.*/
public class CachingDFBnB extends CachingScheme {

    static final private boolean
      DEBUG_dfbnb                      = Definitions.DEBUG,
      DEBUG_dfbnb_verbose              = Definitions.DEBUG,
      DEBUG_dfbnb_verbose2             = false;

    static private boolean UselessCacheTest_Static = true;  //should find all

	public boolean USE_CachingNonOptimalSize_As_Seed = true;
	public boolean USE_CachingGreedy_As_Seed = true;

	static public boolean USE_INCORRECT_worthless_test = false;


    //USER SETABLE PARAMS
    public OrderingAlgo orderAlgo;
    public ChildExpansion childExpansion;
    public HeurFunct heur;
//     public boolean useLookAhead = false; //looks ahead along the ? branch
    {
        orderAlgo = OrderingAlgo.defaultVal();
        childExpansion = ChildExpansion.defaultVal();
        heur = HeurFunct.defaultVal();
    }


    //These are the temporary variables which were used during the last dfbnb search
    //  if multiple threads are using dfbnb, it is from the last to finish
    //  (undeterministic which thread that is)
    DFBnB_TmpVars tmpVars_last;


    public CachingDFBnB( ) {
        this( 1.0); //full caching is default
    }

    public CachingDFBnB( double cacheFactor) {
        super( cacheFactor);
    }

    public String toString() {
        StringBuffer ret = new StringBuffer("DFBnB");

        ret.append( "_" + childExpansion.toString());
        ret.append( "_" + orderAlgo.toString());
        ret.append( "_" + heur.toString());
//         if( useLookAhead) {
//             ret.append( "_lkahd");
//         }

        return ret.toString();
    }


    public DFBnB_TmpVars tmpVars_last() { return tmpVars_last;}



    public void allocateMemory( RCDtree tree, RCCreateListener listnr) {
        allocateMemoryDFBnB( tree, listnr, 1.0, -1.0, null);
    }
    /*Together seed_bestCost and seed_cf produce a seed to the dfbnb search*/
    public void allocateMemory( RCDtree tree, RCCreateListener listnr, double seed_bestCost, Map seed_cf) {
        allocateMemoryDFBnB( tree, listnr, 1.0, seed_bestCost, seed_cf);
    }
    public void allocateMemory( RCDgraph graph, RCCreateListener listnr) {
        allocateMemoryDFBnB( graph, listnr, graph.numRoots(), -1.0, null);
    }
    /*Together seed_bestCost and seed_cf produce a seed to the dfbnb search*/
    public void allocateMemory( RCDgraph graph, RCCreateListener listnr, double seed_bestCost, Map seed_cf) {
        allocateMemoryDFBnB( graph, listnr, graph.numRoots(), seed_bestCost, seed_cf);
    }
    public void allocateMemory( RC rc, RCCreateListener listnr) {
        allocateMemoryDFBnB( rc, listnr, rc.numRoots(), -1.0, null);
    }
    /*Together seed_bestCost and seed_cf produce a seed to the dfbnb search*/
    public void allocateMemory( RC rc, RCCreateListener listnr, double seed_bestCost, Map seed_cf) {
        allocateMemoryDFBnB( rc, listnr, rc.numRoots(), seed_bestCost, seed_cf);
    }

    private void allocateMemoryDFBnB( RC rc, RCCreateListener listnr, double initialPartialCost, double seed_bestCost, Map seed_cf) {

        if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println("\n\nBegin allocateMemoryDFBnB" + cacheFactor);}

		if( rc.outputConsole != null) {
			rc.outputConsole.println("\nStarting Cache Factor Search");
		}


		if( cacheFactor != 1.0) { //don't seed CF=1.0

			if( seed_cf == null) { seed_bestCost = Double.MAX_VALUE;}
			double tmpSeedCost;

			if(USE_CachingGreedy_As_Seed) {
				CachingGreedy mySeedCS = new CachingGreedy();
				mySeedCS.setCacheFactor( cacheFactor);

				mySeedCS.allocateMemory( rc, null);
				tmpSeedCost = rc.statsAll().expectedNumberOfRCCalls();
				if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println("Seed with CachingGreedy = " + tmpSeedCost);}
				if( tmpSeedCost < seed_bestCost) {
					if( DEBUG_dfbnb) { Definitions.STREAM_VERBOSE.println("Seed with CachingGreedy was better than user passed " + tmpSeedCost);}
					seed_cf = rc.getCFMap();
					seed_bestCost = tmpSeedCost;
				}
			}
			if( USE_CachingNonOptimalSize_As_Seed) {
				CachingNonOptimalSize mySeedCS = new CachingNonOptimalSize();
				mySeedCS.setCacheFactor( cacheFactor);

				mySeedCS.CF_LOW_TO_HIGH = true;
				mySeedCS.allocateMemory( rc, null);
				tmpSeedCost = rc.statsAll().expectedNumberOfRCCalls();
				if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println("Seed with CF_LOW_TO_HIGH = " + tmpSeedCost);}
				if( tmpSeedCost < seed_bestCost) {
					if( DEBUG_dfbnb) { Definitions.STREAM_VERBOSE.println("Seed with CF_LOW_TO_HIGH was better than previous " + tmpSeedCost);}
					seed_cf = rc.getCFMap();
					seed_bestCost = tmpSeedCost;
				}

				mySeedCS.CF_LOW_TO_HIGH = false;
				mySeedCS.allocateMemory( rc, null);
				tmpSeedCost = rc.statsAll().expectedNumberOfRCCalls();
				if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println("Seed with !CF_LOW_TO_HIGH = " + tmpSeedCost);}
				if( tmpSeedCost < seed_bestCost) {
					if( DEBUG_dfbnb) { Definitions.STREAM_VERBOSE.println("Seed with !CF_LOW_TO_HIGH was better than previous " + tmpSeedCost);}
					seed_cf = rc.getCFMap();
					seed_bestCost = tmpSeedCost;
				}
			}
		}



        DFBnB_TmpVars tmpVars = new DFBnB_TmpVars( rc, orderAlgo);



        long maxAcceptableCache = (long)Math.floor( cacheFactor * tmpVars.sg.numCachingFullMinusWorthless);

		if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println("DFBnB:maxAcceptableCache " + maxAcceptableCache);}


        if( DEBUG_dfbnb && UselessCacheTest_Static) { Definitions.STREAM_VERBOSE.println("There were " + tmpVars.sg.worthlessCaches + " unused caches out of a total of " +
                               tmpVars.sg.numInternalNodes + " internal nodes (determined statically)");
        }


        //search for best caching scheme

        if( seed_cf != null) {
            try{
                BigInteger bi = BigInteger.ZERO;

                for( int i=0; i<tmpVars.sg.order.length; i++) {
                    RCNode nd = (RCNode)tmpVars.sg.nodes.get( tmpVars.sg.order[i]);
                    Number cf_N = (Number)seed_cf.get(nd);
                    double cf_l = cf_N.doubleValue();
                    if( cf_l == 0.0) {
                        bi = bi.clearBit( i);
					}
					else if( cf_l == 1.0) {
                        bi = bi.setBit( i);
					}
					else {
						throw new IllegalArgumentException("Seed must be discrete, cf of either 1.0 or 0.0 for each node: " + cf_l);
					}
                }

                tmpVars.cfi = new CacheFactorInstance( seed_bestCost, bi);

            }
            catch( Exception e) {
                System.err.println("invalid seed, ignoring");
                tmpVars.cfi = null;
            }

            //TODO should check that it satisfies memory requirements
        }
        if( tmpVars.cfi == null) {
            tmpVars.cfi = new CacheFactorInstance( HeurFunct.assumeZero.heur().h( -1, tmpVars), BigInteger.ZERO);
        }

        tmpVars.cfi.guaranteedOptimal = true; //assume optimal until dfbnb says otherwise
        boolean userstopped = false; //can be stopped by either finished search or user request

        if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println("Initialize dfbnb: " + tmpVars.cfi);
        }

        {
            if( listnr != null) {
                listnr.rcCreateUpdate( tmpVars.cfi.bestCost); //give user the initial search cost
            }

            if( DEBUG_dfbnb){ Definitions.STREAM_VERBOSE.println("start_dfbnb"); }
            long st = System.currentTimeMillis();


			tmpVars.max_mem = maxAcceptableCache;
			tmpVars.listnr = listnr;

            if( !dfbnb( 0, 0, BigInteger.ZERO, initialPartialCost, null, null, tmpVars)) {
                if( !tmpVars.cfi.guaranteedOptimal) {
                    userstopped = true;
                }
            }
            long en = System.currentTimeMillis();
            tmpVars.dfbnbTime_ms = (en-st);
            if( DEBUG_dfbnb){ Definitions.STREAM_VERBOSE.println( toString() + "_" + cacheFactor +
                                                 " took: \t" + (en - st) + "\t ms"); }

            if( listnr != null) {
                listnr.rcCreateUpdate( tmpVars.cfi.bestCost); //give user the last search cost
                tmpVars.changeInBestSinceLastUpdateCall = 0;
            }
        }

		if( rc.outputConsole != null) {
			rc.outputConsole.println("Finished Cache Factor Search");
			rc.outputConsole.println(" Maximum number of cache entries = " + maxAcceptableCache +
										" (" + maxAcceptableCache*8.0/1024.0/1024.0 + " Mb)");
			rc.outputConsole.println(" Best Cost = " + tmpVars.cfi.bestCost + " RC calls");

		}


        if( DEBUG_dfbnb_verbose) {
            Definitions.STREAM_VERBOSE.println("\n\ndfbnb stats");
            Definitions.STREAM_VERBOSE.println("  Found bestCost: " + tmpVars.cfi.bestCost);
//            Definitions.STREAM_VERBOSE.println("  Found bestCost: " + tmpVars.cfi.bestCost + ", " + tmpVars.cfi.bestCFSetting.toString(2) + ", " +
//						            tmpVars.cfi.bestCFSetting);
            Definitions.STREAM_VERBOSE.println("  maxAcceptableCache: " + maxAcceptableCache +
                               " out of " + tmpVars.sg.numCachingFullMinusWorthless);
            Definitions.STREAM_VERBOSE.println("  Searched: " + tmpVars.numSearchCalls + " out of " +
                               ((2L << tmpVars.sg.numInternalNodes) - 1));
//                 Doesn't make much sense to display this, since we still count a dfbnb for the worthless nodes
//                 Definitions.STREAM_VERBOSE.println("Searched: " + tmpVars.numSearchCalls + " out of " +
//                                    ((2L << (tmpVars.numInternalDtreeNodes-tmpVars.stree.worthlessCaches())) - 1) +
//                                    " (w/o worthless)");
        }




        //Set CF on each node
        for( int i=0; i<tmpVars.sg.order.length; i++) {
            RCNode nd = (RCNode)tmpVars.sg.nodes.get( tmpVars.sg.order[i]);
            int cf;
            if( tmpVars.cfi.bestCFSetting.testBit( i)) { cf = 1;}
            else { cf = 0;}

			if( cf != nd.getCacheFactor()) {
	            nd.changeCacheFactor( cf);
			}
        }

        if( DEBUG_dfbnb_verbose) {
			Definitions.STREAM_VERBOSE.println("Expected Calls_all = " + rc.statsAll().expectedNumberOfRCCalls());
			double tmem = rc.statsAll().numCacheEntries();
			Definitions.STREAM_VERBOSE.println("Expected Cache Entries = " + tmem);
			Definitions.STREAM_VERBOSE.println("Expected CacheMB = " + (tmem * 8.0 / 1048576.0));
		}

        if( DEBUG_dfbnb_verbose2) {
			Map tmprc = rc.getCFMap();
			if( tmprc.equals( seed_cf)) {
				Definitions.STREAM_VERBOSE.println("cf map same");
			}
			else {
				Definitions.STREAM_VERBOSE.println("cf maps different");
				Definitions.STREAM_VERBOSE.println("\n\nseed: " + seed_cf);
				Definitions.STREAM_VERBOSE.println("\n\nactual: " + tmprc);
			}
		}

        if( listnr != null) {
            listnr.rcCreateDone( tmpVars.cfi.bestCost, !userstopped);
        }

        tmpVars_last = tmpVars;

        if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println("End allocateMemoryDFBnB");}

    }//end allocateMemoryDFBnB










    /** Implements a depth-first branch-and-bound search and will set many values in the tmpVars parameter
     *
     * @param tmpVars.listnr Can be null, otherwise it implements the RCCreateListener interface.
     //Be careful with the next 2 items as you could break optimality with them.
     * @param preComputedH0 If non-null, will be used as the heur down the 0 side.
     * @param preComputedH1 If non-null, will be used as the heur down the 1 side. (The mem field will not
     *  be correct, but if passed down that means it doesn't satisfy the memory cutoff.  It will also be larger
     *  than the correct value).
     * @return True if normal, false if search should stop (StopRequested).
     */
    private boolean dfbnb( int indx, long curr_mem,
                           BigInteger partialCF, double partialCost,
                           HeurResult preComputedH0, HeurResult preComputedH1,
                           DFBnB_TmpVars tmpVars) {

        if( DEBUG_dfbnb_verbose2) { Definitions.STREAM_VERBOSE.println( "dfbnb(" + indx + "," + curr_mem + "," + tmpVars.max_mem + "," +
                                partialCF + "," + partialCost + "," +
                                ((preComputedH0==null) ? ""+null : ""+preComputedH0.h) + "," +
                                ((preComputedH1==null) ? ""+null : ""+preComputedH1.h) + ")");
        }

        if( curr_mem > tmpVars.max_mem) { //probable overflow?
            if( DEBUG_dfbnb || DEBUG_dfbnb_verbose) {
                throw new IllegalStateException( "mem:" + curr_mem + " > " + tmpVars.max_mem );
            }
            System.err.println("dfbnb error: mem:" + curr_mem + " > " + tmpVars.max_mem);
            return true;
        }
        if( partialCost >= tmpVars.cfi.bestCost) { //this branch is bad
			if( indx != 0) { //if indx==0, then first call, and possible partialCost==BestCost (not an error)
				if( DEBUG_dfbnb || DEBUG_dfbnb_verbose) {
					throw new IllegalStateException( "cost:" + partialCost + " >= " + tmpVars.cfi.bestCost);
				}
				System.err.println("dfbnb error: cost:" + partialCost + " >= " + tmpVars.cfi.bestCost);
			}
            return true;
        }



        //FULL CACHE TEST
        if( indx==0) { //check for full cache immediately
            HeurResult fchr = HeurFunct.assumeOne.heur().h( indx, tmpVars, null);
            if( curr_mem + fchr.mem <= tmpVars.max_mem) { //if valid, stop
                if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println( "FullCacheTest success " + (partialCost+fchr.h) + " = " +
                                        partialCost +"+"+ fchr.h);
                }
                leafCompare( tmpVars, partialCost+fchr.h, partialCF.or(fchr.cf));
                return true;
            }
            else if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println("FullCacheTest failed: req:" + (curr_mem + fchr.mem) +
                                   "  max_mem:" + tmpVars.max_mem);
            }
        }

        if( tmpVars.listnr != null) {
            if( tmpVars.listnr.rcCreateStopRequested()) {
                tmpVars.cfi.guaranteedOptimal = false; //no guarantees if stopped!
                if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println("User Requested Stop");}
                return false;
            }
        }
        tmpVars.numSearchCalls++;


        if( indx >= tmpVars.sg.numInternalNodes) { //leaf node of search tree
            leafCompare( tmpVars, partialCost, partialCF);
        }
        else {

            //could do dynamic node ordering here...
            int nodeIndex = tmpVars.sg.order[indx];
            RCNodeInternalBinaryCache n = (RCNodeInternalBinaryCache)tmpVars.sg.nodes.get(nodeIndex);

            if( tmpVars.sg.numRC[nodeIndex] < 0) {
                //                     System.out.println("Order (" + order.size() + "):" + order);
                //                     System.out.println("indx: " + indx);
                //                     System.out.println("curr_mem: " + curr_mem);
                //                     System.out.println("max_mem: " + tmpVars.max_mem);
                //                     System.out.println("partialCF: " + partialCF.toString(2));
                //                     System.out.println("partialCost: " + partialCost);
                throw new IllegalStateException("numRC not set: " + n.toString() + " order: " + indx);
            }


			int flag;	//5= expand0
						//6= expand1
						//7= expand0, expand1
						//8= expand1, expand0
			boolean resetCurrCF = true;  //for preassigned values don't do this
			HeurResult preComputedH0_passAhead = null;
			HeurResult preComputedH1_passAhead = null;


            //PREASSIGNED CF=0
            if( tmpVars.sg.currCF[nodeIndex] == 0) {//already assigned a cf=0, skip this branch (worthless cache)
                if( DEBUG_dfbnb_verbose2) { Definitions.STREAM_VERBOSE.println("preassigned cache=0");}

				flag = 5;
				resetCurrCF = false;
                //preComputedH0 still valid since going down 0 path
                //preComputedH1 still valid since 1 is worthless
				preComputedH0_passAhead = preComputedH0;
				preComputedH1_passAhead = preComputedH1;
            }
            else {

				//Compute Heur if needed

				if( preComputedH1 != null) {
					if( preComputedH1.h >= tmpVars.cfi.bestCost) {
						System.err.println("ERROR: called dfbnb with node which could have been pruned");
						return true;
					}
				}


				//Attempt to use a precomputed H value instead of computing it

				HeurResult hr = null;
				if( heur.equals( HeurFunct.assumeOne)) {
					hr = preComputedH1; //possibly null
				}
				else if( heur.equals( HeurFunct.assumeZero)) {
					hr = preComputedH0; //possibly null
				}

				if( hr == null) { //need to compute heuristic
					hr = heur.heur().h( indx, tmpVars, null);
					if( heurPrune( curr_mem, partialCF, partialCost,
								   hr, tmpVars)) { return true;} //COST PRUNE
				}


				//purely a speedup (don't have to set either passAhead heur)
				if( heur.equals( HeurFunct.assumeOne)) {
					preComputedH1_passAhead = hr;
				}
				else if( heur.equals( HeurFunct.assumeZero)) {
					preComputedH0_passAhead = hr;
				}



				boolean tryOne = true;

				//MEMORY BASED PRUNING
				if( curr_mem + n.contextInstantiations() > tmpVars.max_mem) { tryOne = false;}


				//Expand children nodes

				if( !tryOne) {
					flag = 5;
//					preComputedH0_passAhead; //okay
					preComputedH1_passAhead = null; //remove
				}
				else {
					if( childExpansion.equals(ChildExpansion.c10)) { //R->L (expand 1 then 0)
						flag = 8;
						//allow passAhead H0 on 0 & H1 on 1
					}
					else if( childExpansion.equals(ChildExpansion.c01)) { //L->R (expand 0 then 1)
						flag = 7;
						//allow passAhead H0 on 0 & H1 on 1
					}
					else {
						throw new IllegalStateException("Unknown child expansion order");
					}
				}
			}//end if preassigned



			//Used to have the expand0 & expand1 below in functions, but the amount of recursion was too much
			// for the java stack with all the parameters & recursive calls.  So I "inlined them" here.


			//recurse down dfbnb by either expanding 0 or expanding 1
			for( int iter=1; iter<=2; iter++) {
				if( (iter==1 && flag==5) || (iter==1 && flag==7) || (iter==2 && flag==8)) {

					//EXPAND0

					HeurResult passAheadH0 = preComputedH0_passAhead;
					HeurResult passAheadH1 = preComputedH1_passAhead;
					//for 7 & 8 possibly remove the passAheads
					if( flag==7 || flag==8) { passAheadH1 = null;}

					//number of calls to each child
					double cost = RCNode.expectedNumberOfRCCalls_local( n.cutsetInstantiations(), 0.0,
																		n.contextInstantiations(),
																		tmpVars.sg.numRC[n.userDefinedInt]);
					if( cost < 0) { throw new IllegalStateException("cost = " + cost + " = " +
																	n.cutsetInstantiations() + " * " +
																	tmpVars.sg.numRC[n.userDefinedInt]);}

					double childVal[] = new double[n.numChildren()];
					int i=0;
					RCIterator itr = n.childIterator();
					for( ; itr.hasNext();) {
						RCNode chi = itr.nextNode();
						childVal[i] = tmpVars.sg.numRC[chi.userDefinedInt];
						if( tmpVars.sg.numRC[chi.userDefinedInt] < 0) { tmpVars.sg.numRC[chi.userDefinedInt] = 0;}
						tmpVars.sg.numRC[chi.userDefinedInt] += cost;
						i++;
					}

					cost *= n.numChildren(); //choosing a 0 here adds this many calls to RC (for each child)

					tmpVars.sg.currCF[n.userDefinedInt] = 0;

					if( partialCost+cost >= tmpVars.cfi.bestCost) { } //don't need to try child
					else {
						if( DEBUG_dfbnb_verbose2) { Definitions.STREAM_VERBOSE.println("cache=0 -- " + n);}
						BigInteger newCF = partialCF.clearBit( indx);
						if( !dfbnb( indx+1, curr_mem, newCF, partialCost+cost,
									passAheadH0, passAheadH1, tmpVars)) { return false;}
					}

					if( resetCurrCF == true) {
						tmpVars.sg.currCF[n.userDefinedInt] = -1;
					}
					else {
		                tmpVars.sg.currCF[n.userDefinedInt] = 0;//don't want it reset to -1
					}

					i=0;
					for( itr.restart(); itr.hasNext();) {
						RCNode chi = itr.nextNode();
						tmpVars.sg.numRC[chi.userDefinedInt] = childVal[i];
						i++;
					}
				}
				else if( (iter==1 && flag==6) || (iter==1 && flag==8) || (iter==2 && flag==7)) {

					//EXPAND1

					HeurResult passAheadH0 = preComputedH0_passAhead;
					HeurResult passAheadH1 = preComputedH1_passAhead;
					//for 7 & 8 possibly remove the passAheads
					if( flag==7 || flag==8) { passAheadH0 = null;}

					//number of calls to each child
					double cost = RCNode.expectedNumberOfRCCalls_local( n.cutsetInstantiations(), 1.0,
																		n.contextInstantiations(),
																		tmpVars.sg.numRC[n.userDefinedInt]);
					if( cost < 0) { throw new IllegalStateException("cost = " + cost + " = " +
																	n.cutsetInstantiations() + " * " +
																	n.contextInstantiations());}


					double childVal[] = new double[n.numChildren()];
					int i=0;
					RCIterator itr = n.childIterator();
					for( ; itr.hasNext();) {
						RCNode chi = itr.nextNode();
						childVal[i] = tmpVars.sg.numRC[chi.userDefinedInt];
						if( tmpVars.sg.numRC[chi.userDefinedInt] < 0) { tmpVars.sg.numRC[chi.userDefinedInt] = 0;}
						tmpVars.sg.numRC[chi.userDefinedInt] += cost;
						i++;
					}

					cost *= n.numChildren(); //choosing a 1 here adds this many calls to RC (for each child)

					tmpVars.sg.currCF[n.userDefinedInt] = 1;

					if( partialCost+cost >= tmpVars.cfi.bestCost) { } //don't need to try child
					else {
						if( DEBUG_dfbnb_verbose2) { Definitions.STREAM_VERBOSE.println("cache=1 -- " + n);}
						BigInteger newCF = partialCF.setBit( indx);
						if( !dfbnb( indx+1, curr_mem + n.contextInstantiations(),
									newCF, partialCost+cost, passAheadH0, passAheadH1, tmpVars)) {
							return false;
						}
					}

					tmpVars.sg.currCF[n.userDefinedInt] = -1;
					i=0;
					for( itr.restart(); itr.hasNext();) {
						RCNode chi = itr.nextNode();
						tmpVars.sg.numRC[chi.userDefinedInt] = childVal[i];
						i++;
					}
				}
			}//end for loop
        }//end if leaf
        return true;
    }


    private final boolean heurPrune( long curr_mem,
									   BigInteger partialCF, double partialCost,
									   HeurResult hr, DFBnB_TmpVars tmpVars) {
        if( partialCost+hr.h >= tmpVars.cfi.bestCost) {
            if( DEBUG_dfbnb_verbose2) { Definitions.STREAM_VERBOSE.println("heur: " + partialCF.or(hr.cf) +
                                                          " - COST PRUNE " +
                                                          partialCost + "+" + hr.h + "=" +
                                                          (partialCost + hr.h) + ">=" +
                                                          tmpVars.cfi.bestCost);}
            return true; //COST PRUNE
        }
        else if( curr_mem + hr.mem <= tmpVars.max_mem) { //if valid, prune children
            leafCompare( tmpVars, partialCost+hr.h, partialCF.or(hr.cf));
            if( DEBUG_dfbnb_verbose2) { Definitions.STREAM_VERBOSE.println("heur: " + partialCF.or(hr.cf) +
                                                          " - COST (Child) PRUNE  " +
                                                          "pc:" + partialCost + "+" +
                                                          "h:" + hr.h);}
            return true; //OPTIMAL CHILD is valid, test it and then no others
        }
        else {
            if( DEBUG_dfbnb_verbose2) { Definitions.STREAM_VERBOSE.println("heur: " + partialCF.or(hr.cf));
            }
            return false;
        }
    }


    private final void leafCompare( DFBnB_TmpVars tmpVars, double partialCost, BigInteger partialCF) {
        if( DEBUG_dfbnb_verbose2) { Definitions.STREAM_VERBOSE.println("dfbnb: expanded leaf: " + partialCF);
        }

        if( partialCost < tmpVars.cfi.bestCost) {

            tmpVars.changeInBestSinceLastUpdateCall += (tmpVars.cfi.bestCost - partialCost);
            tmpVars.cfi.bestCost = partialCost;
            tmpVars.cfi.bestCFSetting = partialCF;

            if( DEBUG_dfbnb_verbose) { Definitions.STREAM_VERBOSE.println("dfbnb: Found new best (" + tmpVars.cfi.bestCost + ")");
            }
            if( tmpVars.listnr != null) {
                if( tmpVars.changeInBestSinceLastUpdateCall >= tmpVars.listnr.rcCreateUpdateThreshold()) {
                    tmpVars.listnr.rcCreateUpdate( tmpVars.cfi.bestCost);
                    tmpVars.changeInBestSinceLastUpdateCall = 0;
                }
            }
        }
    }













    static public class HeurResult {
        double h;
        long mem;
        BigInteger cf;

        public HeurResult() { init();}

        public void init() {
            h = 0;
            mem = 0;
            cf = BigInteger.ZERO;
        }

        public String toString() { return "h: " + h + ", mem: " + mem + ", cf: " + cf;}
    }

    static abstract private class HeuristicSearchFunction {
        abstract public double h( int indx, DFBnB_TmpVars tmpV);
        abstract public HeurResult h( int indx, DFBnB_TmpVars tmpV, HeurResult res);
        abstract public String toString();
    }


    /** Always returns 0.*/
    static private class NoHeuristic extends HeuristicSearchFunction {
        public double h( int indx, DFBnB_TmpVars tmpV) { return 0.0;}
        public HeurResult h( int indx, DFBnB_TmpVars tmpV, HeurResult res) {
            if( res == null) { res = new HeurResult();}
            else { res.init();}
            return res;
        }
        public String toString() { return "hNone";}
    }



    //TODO Add another AssumeOneHeur which looks as how much memory is left!



    /**
     * Assumes every node from indx to the end can be set to cf = 1 (ignores memory pruning,
     * although it does handle preset CF=0 values)
     * and returns the cost of doing this for each of those nodes.
     */
    static private class AssumeOneHeuristic extends HeuristicSearchFunction {
        HeurResult hr = new HeurResult(); //less memory allocation during fast runs
        public double h( int indx, DFBnB_TmpVars tmpV) {
            HeurResult ret = h( indx, tmpV, hr);
            return ret.h;
        }
        public HeurResult h( int indx, DFBnB_TmpVars tmpV, HeurResult res) {
            if( res == null) { res = new HeurResult();}
            else { res.init();}

            final boolean DEBUG_VERBOSE = false;
            if( DEBUG_VERBOSE) { Definitions.STREAM_VERBOSE.println("begin AssumeOneHeuristic");}

            MappedList nodes = tmpV.sg.nodes;
            int order[] = tmpV.sg.order;
            int currCF[] = tmpV.sg.currCF;

            //need to update numRC array... (because of possible worthless nodes)
            double tmpRC[] = (double[])tmpV.sg.numRC.clone();

            if( DEBUG_VERBOSE) {
				Definitions.STREAM_VERBOSE.print("numRC:");
				DblArrays.print( tmpRC, System.out );
				Definitions.STREAM_VERBOSE.println("");
            }

            while( indx < order.length) {
                RCNodeInternalBinaryCache n = (RCNodeInternalBinaryCache)nodes.get( order[indx]);

                double cost;

                if( currCF[order[indx]] != 0) { //if its not preassigned a zero value
                    //Node n adds this amount to cost when cf set to 1
                    cost =  n.cutsetInstantiations() * n.contextInstantiations();
                    res.h = res.h + (n.numChildren() * cost);
                    res.mem += n.contextInstantiations();
                    res.cf = res.cf.setBit( indx);
                }
                else {//it was probably a worthless cache (preassigned to 0)(force down 0 path since 1 is invalid)

                    if( tmpRC[order[indx]] >= 0) { //if parent was already set (it could have been set to 0, so use
                        // numRCCalls and not cntxsp)
                        cost = n.cutsetInstantiations() * tmpRC[order[indx]];
                        res.h = res.h + (n.numChildren() * cost);
                    }
                    else {
                        //For worthless caches, its same as above going down 1 branch
                        cost =  n.cutsetInstantiations() * n.contextInstantiations();
                        res.h = res.h + (n.numChildren() * cost);
                    }
                    //don't increase memory
                    //res.mem += 0
                    res.cf = res.cf.clearBit( indx);
                }

                //update children in tmpRC

                for( RCIterator itr = n.childIterator(); itr.hasNext();) {
					RCNode chi = itr.nextNode();
					if( tmpRC[chi.userDefinedInt] < 0) { tmpRC[chi.userDefinedInt] = 0;}
					tmpRC[chi.userDefinedInt] += cost;
				}
                indx++;
            }

            if( DEBUG_VERBOSE) {
                double sum=tmpRC[0];
                Definitions.STREAM_VERBOSE.print("numRC at end:[" + tmpRC[0]);
                for( int i=1; i<tmpRC.length; i++) {
                    Definitions.STREAM_VERBOSE.print("," + tmpRC[i]);
                    sum+= tmpRC[i];
                }
                Definitions.STREAM_VERBOSE.println("]=" + sum);
            }

            if( DEBUG_VERBOSE) { Definitions.STREAM_VERBOSE.println("end AssumeOneHeuristic (" + res + ")");}
            return res;
        }
        public String toString() { return "hAssumeOne";}
    }


    /**
     * Assumes every node from indx to the end is set to cf = 0 (not admissible)
     * and returns the cost of doing this for each of those nodes. (does not include
     * the cost of number of calls to the node at indx)
     *
     * <p>Can pass indx=-1 to get the global number of calls without caching
     *  (numRC should only have the number of calls to each root in it for
     *  this to be accurate!)
     */
    static private class AssumeZeroHeuristic extends HeuristicSearchFunction {
        public double h( int indx, DFBnB_TmpVars tmpV) {

            final boolean DEBUG_VERBOSE = false;
            if( DEBUG_VERBOSE) { Definitions.STREAM_VERBOSE.println("begin AssumeZeroHeuristic");}

            MappedList nodes = tmpV.sg.nodes;
            int order[] = tmpV.sg.order;
            double ret = 0.0;

            double tmpRC[] = (double[])tmpV.sg.numRC.clone();

            if( DEBUG_VERBOSE) {
                Definitions.STREAM_VERBOSE.print("numRC:[" + tmpRC[0]);
                for( int i=1; i<tmpRC.length; i++) {
                    Definitions.STREAM_VERBOSE.print("," + tmpRC[i]);
                }
                Definitions.STREAM_VERBOSE.println("]");
            }

            if( indx == -1) { //include cost to call the roots
                for( int i=0; i<tmpRC.length; i++) {
                    if( tmpRC[i] > 0) { ret += tmpRC[i];}
                }
                indx = 0;
                if( DEBUG_VERBOSE) { Definitions.STREAM_VERBOSE.println("num root calls: " + ret);}
            }

            while( indx < order.length) {
                RCNodeInternalBinaryCache n = (RCNodeInternalBinaryCache)nodes.get( order[indx]);

                double cost = n.cutsetInstantiations() * tmpRC[order[indx]]; //number of calls to each child
                if( cost <= 0) { throw new IllegalStateException( "cost = " + cost + " = " + n.cutsetInstantiations()
                                                                  + " * " + tmpRC[order[indx]]);}


				for( RCIterator itr = n.childIterator(); itr.hasNext();) {
					RCNode chi = itr.nextNode();
					if( tmpRC[chi.userDefinedInt] < 0) { tmpRC[chi.userDefinedInt] = 0;}
					tmpRC[chi.userDefinedInt] += cost;
				}

                cost *= n.numChildren(); //choosing a 0 here adds this many calls to RC (for each child)

                ret += cost; //Node n adds this amuont to cost when cf set to 0
                indx++;
            }
            if( DEBUG_VERBOSE) { Definitions.STREAM_VERBOSE.println("end AssumeZeroHeuristic (" + ret + ")");}
            return ret;
        }
        public HeurResult h( int indx, DFBnB_TmpVars tmpV, HeurResult res) {
            if( res == null) { res = new HeurResult();}
            else { res.init();}

            res.h += h( indx, tmpV);

            return res;
        }
        public String toString() { return "hAssumeZero";}
    }
















    static public final class HeurFunct {

        private static int _nextIndex = 0;

        private final int _intVal;
        private final HeuristicSearchFunction _heurFunct;

        //private so no new ones can be created
        private HeurFunct( HeuristicSearchFunction hf) {
            if( hf == null) { throw new IllegalArgumentException("hf = null");}
            _intVal = _nextIndex++;
            _heurFunct = hf;
        }

        public final int toInt() { return _intVal;}
        public final String toString() { return _heurFunct.toString();}
        public final HeuristicSearchFunction heur() { return _heurFunct;}

        public static final int getNumTypes() { return _nextIndex;}
        public static final HeurFunct defaultVal() { return assumeOne;}


        public static final HeurFunct assumeOne = new HeurFunct( new AssumeOneHeuristic());
        public static final HeurFunct none = new HeurFunct( new NoHeuristic());
        /*package*/ static final HeurFunct assumeZero = new HeurFunct( new AssumeZeroHeuristic()); //not admissible
    }


    static public final class OrderingAlgo {

        private static int _nextIndex = 0;

        private final int _intVal;
        private final String _strVal;

        //private so no new ones can be created
        private OrderingAlgo(String label) {
            _strVal = label;
            _intVal = _nextIndex++;
        }

        public final int toInt() { return _intVal;}
        public final String toString() { return _strVal;}

        public static final int getNumAlgos() { return _nextIndex;}
        public static final OrderingAlgo defaultVal() { return maxCache;}

        public static final OrderingAlgo df = new OrderingAlgo("df");
        public static final OrderingAlgo minCache = new OrderingAlgo("minCache");
        public static final OrderingAlgo maxCache = new OrderingAlgo("maxCache");
        public static final OrderingAlgo minDeriv = new OrderingAlgo("minDeriv");
        public static final OrderingAlgo maxDeriv = new OrderingAlgo("maxDeriv");
    }


    static public final class ChildExpansion {

        private static int _nextIndex = 0;

        private final int _intVal;
        private final String _strVal;

        //private so no new ones can be created
        private ChildExpansion(String label) {
            _strVal = label;
            _intVal = _nextIndex++;
        }

        public final int toInt() { return _intVal;}
        public final String toString() { return _strVal;}

        public static final int getNumExpansions() { return _nextIndex;}
        public static final ChildExpansion defaultVal() { return c10;}

        public static final ChildExpansion c10 = new ChildExpansion("10");
        public static final ChildExpansion c01 = new ChildExpansion("01");
    }






    static public class DFBnB_TmpVars {

        CacheFactorInstance cfi;
        double changeInBestSinceLastUpdateCall;
        long numSearchCalls;
        long dfbnbTime_ms;
        SearchGraph sg;
        long max_mem;
        RCCreateListener listnr;

        public DFBnB_TmpVars( RC rc, OrderingAlgo orderAlgo) {
            cfi = null;

            changeInBestSinceLastUpdateCall = 0;
            numSearchCalls = 0;
            dfbnbTime_ms = -1;

            sg = new SearchGraph( rc, orderAlgo);
        }

        public CacheFactorInstance cfi() { return cfi;}
        public long numSearchCalls() { return numSearchCalls;}
        public long dfbnbTime_ms() { return dfbnbTime_ms;}
        public SearchGraph sg() { return sg;}
    }


    static public class CacheFactorInstance {
        double bestCost;
        BigInteger bestCFSetting;
        boolean guaranteedOptimal;

        public CacheFactorInstance( double bestCost, BigInteger bestCFSetting) {
            this.bestCost = bestCost;
            this.bestCFSetting = bestCFSetting;
            guaranteedOptimal = false;
        }

        public CacheFactorInstance( ) {
            this( -1, new BigInteger("-1"));
        }

        public double bestCost() { return bestCost;}
        public BigInteger bestCFSetting() { return bestCFSetting;}
        public boolean guaranteedOptimal() { return guaranteedOptimal;}

        public String toString() { return "bestCost: " + bestCost + ", Opt: " + guaranteedOptimal + ", cf: " + bestCFSetting;}
    }









    static public class SearchGraph {
        RC rc;
        MappedList nodes;   //all nodes including leafs
        double numRC[];
        int currCF[];

        int order[]; //possibly not full size (if dynamic ordering) (use numInternalNodes instead for size)
                     //these are the indexes into nodes

        long numCachingFull;
        int numInternalNodes;

        //if UselessCacheTest_Static
        long numCachingFullMinusWorthless;  //otherwise set to numCachingFull
        int worthlessCaches; //otherwise will be -1


        public SearchGraph( RC rc, OrderingAlgo orderAlgo) {
            this.rc = rc;
            init( orderAlgo);
        }


        public long numCachingFullMinusWorthless() { return numCachingFullMinusWorthless;}
        public long numCachingFull() { return numCachingFull;}
        public int worthlessCaches() { return worthlessCaches;}
        public int[] order() { return order;}
        public MappedList nodes() { return nodes;}


        public void init( OrderingAlgo orderAlgo) {
            RCIterator itr = rc.getIterator();
            numCachingFull = 0;
            numInternalNodes = 0;
            nodes = new MappedList();
            int cntr=0;
            while( itr.hasNext()) {
                RCNode nd = itr.nextNode();
                nodes.add( nd);
                nd.userDefinedInt = cntr;
                cntr++;
                if( !nd.isLeaf()) {
                    numCachingFull += nd.contextInstantiations();
                    numInternalNodes++;
                }
            }

            numRC = new double[nodes.size()];
            Arrays.fill( numRC, -1);
            currCF = new int[nodes.size()];
            Arrays.fill( currCF, -1);

            numCachingFullMinusWorthless = numCachingFull;
            worthlessCaches = -1;


            itr = rc.getIteratorRoots();
            while( itr.hasNext()) {
                RCNode rt = itr.nextNode();
                numRC[rt.userDefinedInt] = 1.0;
            }


            //Generate ordering for DFBnB
            if( orderAlgo == OrderingAlgo.minCache) {
                order = generateOrderContextRestricted( false);
            }
            else if( orderAlgo == OrderingAlgo.maxCache) {
                order = generateOrderContextRestricted( true);
            }
            else {
                throw new UnsupportedOperationException( orderAlgo.toString() + " not valid");
            }

            if( UselessCacheTest_Static) {
                calcWorthlessCaches( order); //assumes order is a restricted ordering (parent<child)
            }

			//its possible a CF=0 has already been set on the node during creation (e.g. context is too large to cache)
			{
				RCIterator nditr = rc.getIterator();
				while( nditr.hasNext()) {
					RCNode nd = nditr.nextNode();

					RCNodeCache cache = nd.cache();

					if( cache != null) {
						if( cache.thisCacheOverFlowed) {
							if( currCF[nd.userDefinedInt] != 0) {
								//this could happen with nodes with very large contexts (which overflow)
								if( DEBUG_dfbnb) {System.err.println("Node " + nd + " was assigned a cf=0 before search and it was NOT worthless.");}
								currCF[nd.userDefinedInt] = 0;
							}
							else {
								if( false /*DEBUG_dfbnb*/) {System.err.println("Node " + nd + " was assigned a cf=0 before search and it WAS worthless.");}
							}
						}
					}
				}
			}
        }



        /*Generate an ordering based on the context.
         * <p>Do NOT include leafs in ordering.
         * <p>Enforce Restriction on parents before children.
         */
        private int[] generateOrderContextRestricted( boolean MAX) {
            int[] order = new int[numInternalNodes];
            Arrays.fill( order, -1);
            int nextIndx = 0;

            final Comparator cmp = rcNodeComparator_Size;

            HashSet openNodes = new HashSet();
            int numParentsLeft[] = rc.generateNumParents( nodes);

            //initialize openNodes
            for( int i=0; i<numParentsLeft.length; i++) {
                if( numParentsLeft[i] == 0) { //all parents seen (initially roots)
                    RCNode n = (RCNode)nodes.get(i);
                    if( !n.isLeaf()) {
                        openNodes.add( n);
                    }
                }
            }

            //remove node from openNodes & reduce the parent count on its children
            while( !openNodes.isEmpty()) {
                RCNodeInternalBinaryCache n;
                if( MAX) {
                    n = (RCNodeInternalBinaryCache)Collections.max( openNodes, cmp);
                }
                else {
                    n = (RCNodeInternalBinaryCache)Collections.min( openNodes, cmp);
                }

                if( DEBUG_dfbnb_verbose2) { Definitions.STREAM_VERBOSE.println("picked: " + n + " out of " + openNodes);}
                order[nextIndx] = n.userDefinedInt; nextIndx++;
                openNodes.remove( n);


				for( RCIterator itr = n.childIterator(); itr.hasNext();) {
					RCNode chi = itr.nextNode();
					numParentsLeft[chi.userDefinedInt] -= 1;
					if( numParentsLeft[chi.userDefinedInt] == 0 && !chi.isLeaf()) {
						openNodes.add( chi);
					}
				}
            }

            if( nextIndx != order.length) {
                System.err.println( "Ordering Error: order had " + nextIndx +
                                    " but should have had a size of " + order.length);
            }
            return order;
        }




        /* orderRestricted: parent[s]-before-child!!!
         */
        private void calcWorthlessCaches( int orderRestricted[]) {
            final boolean DEBUG_verbose = false;

            worthlessCaches = 0;
            numCachingFullMinusWorthless = numCachingFull;

            HashSet parCntxs[] = new HashSet[nodes.size()]; //union of all parent hashsets for a node
            int numPar[] = new int[nodes.size()]; //if has >=2 parents, not worthless anymore...
			Arrays.fill( numPar, 0);

            for( int i=0; i<orderRestricted.length; i++) {
                RCNodeInternalBinaryCache nd = (RCNodeInternalBinaryCache)nodes.get(orderRestricted[i]);
                List cntx = nd.cache().getContext( new ArrayList());

                //Add context here to parent contexts of each child
                for( RCIterator itr = nd.childIterator(); itr.hasNext();) {
					RCNode chi = itr.nextNode();
					numPar[chi.userDefinedInt]++;
	                if( parCntxs[chi.userDefinedInt] == null) { parCntxs[chi.userDefinedInt] = new HashSet();}
	                parCntxs[chi.userDefinedInt].addAll( cntx);
				}


                //For this node (all parents done already) check if worthless
                if( parCntxs[nd.userDefinedInt] == null) { //then it was a root node (so worthless)
                    currCF[nd.userDefinedInt] = 0;  //preAssign it to 0
                    worthlessCaches++;
                    numCachingFullMinusWorthless -= nd.contextInstantiations();
                    if( DEBUG_verbose) { Definitions.STREAM_VERBOSE.println("worthless: root node");}
                }
				else if( USE_INCORRECT_worthless_test || numPar[nd.userDefinedInt] < 2) {
//                else {  //this is old and incorrect should be else if( numPar[indxH] < 2) {...
//                else if( numPar[indxH] < 2) {  //two parents makes a worthless cache non-worthless
					if( DEBUG_verbose) {
						Definitions.STREAM_VERBOSE.println("worthless_test: parents cntx: " + parCntxs[nd.userDefinedInt]);
						Definitions.STREAM_VERBOSE.println("worthless_test: local cntx  : " + cntx);
					}

					parCntxs[nd.userDefinedInt].removeAll( cntx); //remove local context from parent contexts
					if( parCntxs[nd.userDefinedInt].isEmpty()) {
						if( DEBUG_dfbnb_verbose2) { Definitions.STREAM_VERBOSE.println("worthless: ordered node: " + i);}
						currCF[nd.userDefinedInt] = 0;  //preAssign it to 0
						worthlessCaches++;
						numCachingFullMinusWorthless -= nd.contextInstantiations();

						if( DEBUG_verbose) { Definitions.STREAM_VERBOSE.println("worthless: worthless cache");
						}
					}
					else if( DEBUG_verbose) { Definitions.STREAM_VERBOSE.println("worthless: useful cache");
					}
				}
                parCntxs[nd.userDefinedInt] = null;//all done with parents of this node
            }
        }//end calcWorthless





    }//end class SearchGraph









//         /**Partial Derivs w.r.t current cache factor settings.*/
//         public double[] partialDeriv_Algo2( ) {
//             double[] par_deriv = new double[nextNodeNum];
//             double[] var_a = new double[nextNodeNum];
//             algo2( root(), -1, -1, -1, par_deriv, var_a);
//             return par_deriv;
//         }

//         static private void algo2( IntTreeNode n1, int ancestor, long cntx, long union_cuts,
//                                    double par_deriv[], double var_a[]) {
//             //compute par_deriv w.r.t. ancestor
//             if( ancestor >= 0) {
//                 par_deriv[ancestor] += (union_cuts * var_a[ancestor]);
//             }

//             //start its own partial derivative & the cntx for next one below it
//             if( n1.currCF == 1) {
//                 ancestor = n1.nodeNum;
//                 par_deriv[ancestor] = 0;
//                 var_a[ancestor] = (cntx * union_cuts) - n1.cntxsp;

//                 cntx = n1.cntxsp;
//                 union_cuts = n1.cutsp;
//             }
//             else if( n1.currCF == 0) {
//                 union_cuts *= n1.cutsp;
//             }
//             else {
//                 throw new IllegalStateException("n1.currCF = " + n1.currCF);
//             }

//             //recurse
//             if( n1.left != null) {
//                 algo2( n1.left, ancestor, cntx, union_cuts, par_deriv, var_a);
//                 algo2( n1.right, ancestor, cntx, union_cuts, par_deriv, var_a);
//             }
//         }


//         static public boolean partialDeriv_Test( IntTreeNode n1, RCDtreeNode n2, RCDtree rcDt, double par_deriv[]) {
//             boolean ret = true;

//             if( n1.currCF == 1) {
//                 ((RCDtreeNodeInternalCache)n2).changeCacheFactor(0);
//                 double newnumRCCalls = rcDt.expectedNumberOfRCCalls();
//                 ((RCDtreeNodeInternalCache)n2).changeCacheFactor(1);

//                 double numRCCalls = rcDt.expectedNumberOfRCCalls();
//                 double diff = newnumRCCalls - numRCCalls;
//                 if( diff != par_deriv[n1.nodeNum]) {
//                     ret = false;
//                 }
//             }
//             else {
//                 if( par_deriv[n1.nodeNum] != 0.0) {
//                     ret = false;
//                 }
//             }

//             if( !n2.isLeaf()) {
//                 ret = ret && partialDeriv_Test( n1.left, ((RCDtreeNodeInternal)n2).left(), rcDt, par_deriv);
//                 ret = ret && partialDeriv_Test( n1.right, ((RCDtreeNodeInternal)n2).right(), rcDt, par_deriv);
//             }
//             return ret;
//         }






    public static final RCNodeComparator_Size rcNodeComparator_Size
        = new RCNodeComparator_Size();

    /**Not consistent with equals!*/
    static public class RCNodeComparator_Size
        implements Comparator {

        public int compare(Object o1, Object o2) {
            RCNode n1 = (RCNode)o1;
            RCNode n2 = (RCNode)o2;

            long num1 = n1.contextInstantiations();
            long num2 = n2.contextInstantiations();
            if( num1 < num2) { return -1;}
            else if( num1 > num2) { return 1;}

            num1 = n1.cutsetInstantiations();
            num2 = n2.cutsetInstantiations();
            if( num1 < num2) { return -1;}
            else if( num1 > num2) { return 1;}
            else { return 0;} //possibly inconsistent with equals...
        }
    }

//     /**Not consistent with equals!*/
//     static public class IntSearchTreeNodeComparator_Deriv
//         implements Comparator {

//         double deriv[];
//         public IntSearchTreeNodeComparator_Deriv( double deriv[]) {
//             this.deriv = deriv;
//         }

//         public int compare(Object o1, Object o2) {
//             IntTreeNode n1 = (IntTreeNode)o1;
//             IntTreeNode n2 = (IntTreeNode)o2;

//             double num1 = deriv[n1.nodeNum];
//             double num2 = deriv[n2.nodeNum];

//             if( num1 < num2) { return -1;}
//             else if( num1 > num2) { return 1;}

//             num1 = n1.nodeNum;
//             num2 = n2.nodeNum;
//             if( num1 < num2) { return -1;}
//             else if( num1 > num2) { return 1;}
//             else { return 0;} //possibly inconsistent with equals...
//         }
//     }

}





