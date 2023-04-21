/*
* RCGenerator.java
*
* @author David
* @version
*/
package edu.ucla.belief;

import java.util.*;
import java.io.*;
import java.text.NumberFormat;


import edu.ucla.structure.MappedList;
import edu.ucla.structure.Graph;
import edu.ucla.structure.Graphs;

public class RCGenerator {
	static final boolean DEBUG_VERBOSE = false; //TODO
	static final boolean DEBUG_VERBOSE1 = false; //TODO
	static final boolean DEBUG_VERBOSE_PREPROCESS = false; //TODO
	static final boolean DEBUG_EXTRA_TESTING = false; //TODO

//	static final public int largestAcceptableRCCache = 33000000; //33MillTSS ~= 250MB
//	static final public int largestAcceptableRCCache = 98000000; //98MillTSS ~= 750MB
//	static final public int largestAcceptableRCCache = 130000000; //130MillTSS ~= 1GB
	static final public int largestAcceptableRCCache = 260000000; //260MillTSS ~= 2GB

	static final public int largestAcceptableRCCost = Integer.MAX_VALUE;

	static public int largestAcceptableEOTSS = Integer.MAX_VALUE;

	static final private double callsPerMilliSec = 9000.0; //currently ranges from 5 to 12 million per SEC (so use 9 million, the average) then convert to MilliSec



	private RCGenerator(){}

	static private void print(Collection col) {
		for(Iterator itr=col.iterator(); itr.hasNext();) {
			System.out.println(itr.next());
		}
	}

	static public RC generateRC(BeliefNetwork bn, String netName, String outDetailsFile, boolean forceExtendedSearch, Set varsToPreprocess) {
		return generateRC(bn, netName, outDetailsFile, forceExtendedSearch, varsToPreprocess, null);
	}

	static public RC generateRC(BeliefNetwork bn, String netName, String outDetailsFile, boolean forceExtendedSearch, Set varsToPreprocess, Set constrainedOrdering[][]) {
		if(DEBUG_VERBOSE) { System.out.println("\n\n\nGenerateRC: " + netName + (forceExtendedSearch ? " Extended Search" : " Regular Search") + "preprocess: " + varsToPreprocess + "\n");}

		if(varsToPreprocess==null) varsToPreprocess=Collections.EMPTY_SET;

		ArrayList preprocessEO = new ArrayList(bn.size());
		int numTotalVars = bn.size();

		{
			Graph udg_tmp = Graphs.moralGraph(bn);
			preprocess(udg_tmp, preprocessEO, varsToPreprocess);


			if(DEBUG_VERBOSE1) {
				TreeSet ts1 = new TreeSet(bn); //all
				TreeSet ts2 = new TreeSet(preprocessEO); //removed

				System.out.println("\n\n\nAll Vars: " + ts1.size());
				print(ts1);

				System.out.println("\n\n\nPreprocessed Vars:" + ts2.size());
				print(ts2);

				ts1.removeAll(ts2);
				System.out.println("\n\n\nRemaining Vars: " + ts1.size());
				print(ts1);

				//print graph
				TreeSet verts = new TreeSet(udg_tmp.vertices());
				System.out.println("\n\n\nGraph: "+ verts.size());
				for(Iterator itrv = verts.iterator(); itrv.hasNext();) {
					Object ver = itrv.next();
					TreeSet nei = new TreeSet(udg_tmp.neighbors(ver));

					System.out.println(ver + " " + nei.size() + " " + nei);
				}
			}
		}

		RC bestRC = null;

		//Calculate difficulty of problem (do each algo once, no pruning based on best to setup timings)
		int minimumIterations;
		int maximumIterations;
		int numOrderingsPerAlgoTSS = 100; //be careful about changing (due to timings)
		int numOrderingsPerAlgoMF = 50;   //be careful about changing (due to timings)
		int numOrderingsPerAlgoWMF = 50;  //be careful about changing (due to timings)
		long timePerIter;  //approx milliSec per iteration

		EliminationStructure1 esTSS_orig = new EliminationStructure1(bn, preprocessEO, EliminationStructure1.scoreTSS, constrainedOrdering);
		EliminationStructure1 esMF_orig = new EliminationStructure1(bn, preprocessEO, EliminationStructure1.scoreMF, constrainedOrdering);
		EliminationStructure1 esWMF_orig = new EliminationStructure1(bn, preprocessEO, EliminationStructure1.scoreWMF, constrainedOrdering);
		{//difficulty of problem
			RC tmpRC;
			EliminationStructure1 esTSS = (EliminationStructure1)esTSS_orig.clone();
			EliminationStructure1 esMF = (EliminationStructure1)esMF_orig.clone();
			EliminationStructure1 esWMF = (EliminationStructure1)esWMF_orig.clone();

			long tmpTime1 = System.currentTimeMillis();
			tmpRC = esTSS.createRC(true/*deterministic*/, Double.MAX_VALUE, 4);
			bestRC = RC.keepBest(bestRC, tmpRC);
			long tmpTime2 = System.currentTimeMillis();
			tmpRC = esMF.createRC(true/*deterministic*/, Double.MAX_VALUE, 4);
			bestRC = RC.keepBest(bestRC, tmpRC);
			long tmpTime3 = System.currentTimeMillis();
			tmpRC = esWMF.createRC(true/*deterministic*/, Double.MAX_VALUE, 4);
			bestRC = RC.keepBest(bestRC, tmpRC);
			long tmpTime4 = System.currentTimeMillis();

			if(DEBUG_VERBOSE) { System.out.println("Original Score: " + bestRC.rcScore());}

			long timeTSS = (tmpTime2-tmpTime1);
			long timeMF = (tmpTime3-tmpTime2);
			long timeWMF = (tmpTime4-tmpTime3);

			timePerIter = (timeTSS*numOrderingsPerAlgoTSS)+(timeMF*numOrderingsPerAlgoMF)+(timeWMF*numOrderingsPerAlgoWMF);
		}
		double seedScore = bestRC.rcScore();

		double lastScore = bestRC.rcScore();

		int algoUsed = 1; //1=TSS, 2=MF, 3=WMF
		long timeTSS = 0; int countTSS=0;
		long timeMF = 0;  int countMF=0;
		long timeWMF = 0; int countWMF=0;
		long tmpTime;
		long timeItrSt;
		boolean stop = false;


		int countIterations=1;
		int consecutiveItrsWithZeroIncrease = 0;
		while(true) {

			timeItrSt = System.currentTimeMillis();


			{//set minimum & maximum number of iterations
				double bs = bestRC.rcScore();
				double et = bs/callsPerMilliSec; //expected computation time

				long tmpTotalTimeSoFar = timeTSS+timeMF+timeWMF;

				if(forceExtendedSearch) {
					minimumIterations = (int)Math.floor(.3 * et / timePerIter); //spend at least 30% of time searching
					maximumIterations = (int)Math.ceil( .75 * et / timePerIter); //spend at most  75% of time searching

					if(minimumIterations<10) {minimumIterations=10;}
					if(maximumIterations<10) {maximumIterations=10;}
				}
				else {

					if(countIterations==1 && (timePerIter>30000)) { //watch out for long searches when don't have reliable best cost figure
						timePerIter=(long)(timePerIter*50.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=50;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=25;
						if(DEBUG_VERBOSE) {System.out.println("\n\n\nReduce numOrderingsPerAlgoTSS to 50 (timePerIter: " +timePerIter+ ")\n\n\n");}
					}



					if(timePerIter > .50*et && timePerIter>30000) {  //if the time for a single iteration is > 50% of expected time, do simpler iterations (if <30 seconds, let next if handle it)
						timePerIter=(long)(timePerIter*10.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=10;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=5;
						if(DEBUG_VERBOSE) {System.out.println("\n\n\nReduce numOrderingsPerAlgoTSS to 10 (timePerIter: "+timePerIter+")\n\n\n");}
					}
					else if(timePerIter > et && et <= 4000) { //search > inference time && inference time < 4 seconds
						timePerIter=(long)(timePerIter*10.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=10;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=5;
						if(DEBUG_VERBOSE) {System.out.println("\n\n\nReduce numOrderingsPerAlgoTSS to 10 (timePerIter: "+timePerIter+")\n\n\n");}
					}
//					else if(timePerIter > .20*et) {  //if the time for a single iteration is > 20% of expected time, do simpler iterations (if <10 seconds, let next if handle it)
//						timePerIter=(long)(timePerIter*25.0/numOrderingsPerAlgoTSS);
//						numOrderingsPerAlgoTSS=25;
//						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=12;
//						if(DEBUG_VERBOSE) {System.out.println("\n\n\nReduce numOrderingsPerAlgoTSS to 25 (timePerIter: "+timePerIter+")\n\n\n");}
//					}
					else if(timePerIter > .10*et) {  //if the time for a single iteration is > 10% of expected time, do simpler iterations
						timePerIter=(long)(timePerIter*50.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=50;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=25;
						if(DEBUG_VERBOSE) {System.out.println("\n\n\nReduce numOrderingsPerAlgoTSS to 50 (timePerIter: "+timePerIter+")\n\n\n");}
					}

					//Use these formulas since the length of the timePerIter may change
					//min = ((total min time - time so far) / timePerIter) + (currentIterations-1)
					//max = ((total max time - time so far) / timePerIter) + (currentIterations-1)

					minimumIterations = (int)Math.floor((.03 * et - tmpTotalTimeSoFar)/ timePerIter +(countIterations-1)); //spend at least 3% of time searching
					maximumIterations = (int)Math.ceil(( .10 * et - tmpTotalTimeSoFar)/ timePerIter +(countIterations-1)); //spend at most 10% of time searching

					if(minimumIterations<0) {minimumIterations=0;}
					if(maximumIterations<0) {maximumIterations=0;}

//					if(et < 30000) { numOrderingsPerAlgoTSS=numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=5;stop=true;} //for easy networks, do fewer searches per iteration
				}

				if(DEBUG_VERBOSE) {
					System.out.println("\n\nIteration: " + countIterations);
					System.out.println("Expected computation time is " + (et/1000.0) + " (sec) for expected score of " + bs);
					System.out.println("Time so far:       " + (tmpTotalTimeSoFar));
					System.out.println("MilliSec per Iter: " + timePerIter);
					System.out.println("min: " + minimumIterations + " (" + (minimumIterations*timePerIter/1000.0) + " sec)");
					System.out.println("max: " + maximumIterations + " (" + (maximumIterations*timePerIter/1000.0) + " sec)");
				}

				if(countIterations>maximumIterations) { //the above may have updated the max, if so possibly stop now (has not run the one listed as countIterations yet, so don't use >=)
					if(DEBUG_VERBOSE) {System.out.println("Searched the maximum number of iterations requested.\n");}
					break;
				}
			}





			//run TotalStateSpace
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoTSS; i++) {
				EliminationStructure1 esTSS = (EliminationStructure1)esTSS_orig.clone();
				//the first constrained order is weighted stronger than the others
				int const_i=-1;
				{
					double rndm = Math.random();
					
					int numberOrderings = (constrainedOrdering==null ? 1 : constrainedOrdering.length);
					if(rndm < 0.68 || numberOrderings==1) const_i = 0;
					else {
						double percentEach = (1.0-0.68)/(numberOrderings-1);
						for(int indx=1; indx<constrainedOrdering.length; indx++) {
							if(rndm < (0.68+indx*percentEach)) {
								const_i = indx;
								break;
							}
						}
						if(const_i<0) throw new IllegalStateException();
					}
				}
				RC tbestRC = RC.keepBest( bestRC, esTSS.createRC(false/*deterministic*/, bestRC.rcScore(), (i % EliminationStructure1.MAX_VERSION)+1, const_i));
				if(tbestRC != bestRC) {
					bestRC = tbestRC;
					algoUsed = EliminationStructure1.scoreTSS;
					if(DEBUG_VERBOSE) { System.out.println("Better score found by TSS: " + bestRC.rcScore() + "   iteration: " + countIterations);}
				}
				System.gc();
			}
			timeTSS += (System.currentTimeMillis() - tmpTime);
			countTSS+=numOrderingsPerAlgoTSS;

			//run MF
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoMF; i++) {
				EliminationStructure1 esMF = (EliminationStructure1)esMF_orig.clone();
				int const_i=-1;
				{
					double rndm = Math.random();
					
					int numberOrderings = (constrainedOrdering==null ? 1 : constrainedOrdering.length);
					if(rndm < 0.68 || numberOrderings==1) const_i = 0;
					else {
						double percentEach = (1.0-0.68)/(numberOrderings-1);
						for(int indx=1; indx<constrainedOrdering.length; indx++) {
							if(rndm < (0.68+indx*percentEach)) {
								const_i = indx;
								break;
							}
						}
						if(const_i<0) throw new IllegalStateException();
					}
				}
				RC tbestRC = RC.keepBest( bestRC, esMF.createRC(false/*deterministic*/, bestRC.rcScore(), (i % EliminationStructure1.MAX_VERSION)+1, const_i));
				if(tbestRC != bestRC) {
					bestRC = tbestRC;
					algoUsed = EliminationStructure1.scoreMF;
					if(DEBUG_VERBOSE) { System.out.println("Better score found by MF: " + bestRC.rcScore() + "   iteration: " + countIterations);}
				}
				System.gc();
			}
			timeMF += (System.currentTimeMillis() - tmpTime);
			countMF+=numOrderingsPerAlgoMF;

			//run WMF
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoWMF; i++) {
				EliminationStructure1 esWMF = (EliminationStructure1)esWMF_orig.clone();
				int const_i=-1;
				{
					double rndm = Math.random();
					
					int numberOrderings = (constrainedOrdering==null ? 1 : constrainedOrdering.length);
					if(rndm < 0.68 || numberOrderings==1) const_i = 0;
					else {
						double percentEach = (1.0-0.68)/(numberOrderings-1);
						for(int indx=1; indx<constrainedOrdering.length; indx++) {
							if(rndm < (0.68+indx*percentEach)) {
								const_i = indx;
								break;
							}
						}
						if(const_i<0) throw new IllegalStateException();
					}
				}
				RC tbestRC = RC.keepBest( bestRC, esWMF.createRC(false/*deterministic*/, bestRC.rcScore(), (i % EliminationStructure1.MAX_VERSION)+1, const_i));
				if(tbestRC != bestRC) {
					bestRC = tbestRC;
					algoUsed = EliminationStructure1.scoreWMF;
					if(DEBUG_VERBOSE) { System.out.println("Better score found by wMF: " + bestRC.rcScore() + "   iteration: " + countIterations);}
				}
				System.gc();
			}
			timeWMF += (System.currentTimeMillis() - tmpTime);
			countWMF+=numOrderingsPerAlgoWMF;

			double improvementRate = (lastScore - bestRC.rcScore()) / lastScore;
			if(DEBUG_VERBOSE) {System.out.println("Improvement Rate for iteration " + countIterations + " was " + (improvementRate));}

			if(stop) { //already found good ordering (not during forceExtendedSearch)
				if(DEBUG_VERBOSE) {System.out.println("Current ordering is already fast, skip rest of search.\n");}
				break;
			}
			else if(countIterations>=maximumIterations) {
				if(DEBUG_VERBOSE) {System.out.println("Searched the maximum number of iterations requested.\n");}
				break;
			}
			else {
				int extraItr = (countIterations-minimumIterations);
				if(extraItr>=0) { //if using more than the minimum number of iterations, force it to have a better improvementRate
					if(improvementRate < (.03 + (extraItr*.01))) { //on first extra iteration will be .04, then .05, .06, ...
						if(DEBUG_VERBOSE) {System.out.println("Stopping search because improvement was too small.\n");}
						break;
					}
				}
			}

			if(improvementRate==0.0) {
				consecutiveItrsWithZeroIncrease++;
				if((!forceExtendedSearch && consecutiveItrsWithZeroIncrease>=3) ||
				    (forceExtendedSearch && consecutiveItrsWithZeroIncrease>=10)) {
					if(DEBUG_VERBOSE) {System.out.println("Stopping search because had " + consecutiveItrsWithZeroIncrease + " consecutive iterations with no improvement.\n");}
					break;
				}
			}
			else {
				consecutiveItrsWithZeroIncrease=0;
			}

			lastScore = bestRC.rcScore();
			countIterations++;

			timePerIter = System.currentTimeMillis()-timeItrSt;
			System.gc();
		}


		String algoUsedStr = (algoUsed==EliminationStructure1.scoreTSS ? "TSS" : (algoUsed==EliminationStructure1.scoreMF ? "MF" : "wMF"));

		if(DEBUG_VERBOSE) {
			NumberFormat nfInt = NumberFormat.getInstance();
			NumberFormat nfDbl = NumberFormat.getInstance();

			nfInt.setMaximumFractionDigits(0);
			nfDbl.setMaximumFractionDigits(2);

			System.out.println("GenerateRC made " + countIterations + " iterations.");
			System.out.println("The final algorithm used was: " + algoUsedStr);
			System.out.println("GenerateRC found the best score of " + bestRC.rcScore());
			System.out.println("   time in TSS: " + nfDbl.format(timeTSS/1000.0) + "   (" + nfInt.format(countTSS) + " orders)");
			System.out.println("   time in  MF: " + nfDbl.format(timeMF/1000.0) + "   (" + nfInt.format(countMF) + " orders)");
			System.out.println("   time in wMF: " + nfDbl.format(timeWMF/1000.0) + "   (" + nfInt.format(countWMF) + " orders)");
			System.out.println("   total time : " + nfDbl.format((timeTSS+timeMF+timeWMF)/1000.0) + "   (" + nfInt.format(countTSS+countMF+countWMF) + " orders)");
			System.out.println("   minimum number of iterations was set to : " + minimumIterations);
			System.out.println("   maximum number of iterations was set to : " + maximumIterations);
//			System.out.println("   width: " + bestRC.width());
//			System.out.println("   tsScore: " + bestRC.tsScore());
			System.out.println("   rcScore: " + bestRC.rcScore());
//			System.out.println("   numCond: " + bestRC.numCond());
		}


		if(outDetailsFile != null) {
			try {
				File oDF = new File(outDetailsFile);
				boolean appending = oDF.exists();
				PrintStream outTable = new PrintStream(new BufferedOutputStream(new FileOutputStream(oDF,true/*append*/)),true/*autoflush*/);
				if(!appending) {
					outTable.println("\t"       +"Time\t\t\t\t"         +"NumOrders\t\t\t\t"    +"Iterations\t\t\t"  +"Vars\tAfter\t" +"Initial\t"+"Final\t"  +"Final\t");
					outTable.println("Network\t"+"Total\tTSS\tMF\tWMF\t"+"Total\tTSS\tMF\tWMF\t"+"Min\tMax\tActual\t"+"Total\tRules\t"+"FCCalls\t"+"FCCalls\t"+"Algo\t" );
				}
				outTable.println(netName+"\t"+(timeTSS+timeMF+timeWMF)+"\t"+timeTSS+"\t"+timeMF+"\t"+timeWMF+"\t"+(countTSS+countMF+countWMF)+"\t"+countTSS+"\t"+countMF+"\t"+countWMF+"\t"+minimumIterations+"\t"+maximumIterations+"\t"+countIterations+"\t"+numTotalVars+"\t"+(numTotalVars-preprocessEO.size())+"\t"+seedScore+"\t"+bestRC.rcScore()+"\t"+algoUsedStr+"\t");
			}
			catch(FileNotFoundException e) {}
		}

		return bestRC;
	}

	static public EO generateEO(BeliefNetwork bn, String netName, String outDetailsFile, boolean forceExtendedSearch, Set varsToPreprocess) {
		if(DEBUG_VERBOSE) { System.out.println("\n\n\nGenerateEO: " + netName + (forceExtendedSearch ? " Extended Search" : " Regular Search") + "\n");}

		if(varsToPreprocess==null) varsToPreprocess=Collections.EMPTY_SET;

		ArrayList preprocessEO = new ArrayList(bn.size());
		int numTotalVars = bn.size();

		{
			Graph udg_tmp = Graphs.moralGraph(bn);
			preprocess(udg_tmp, preprocessEO, varsToPreprocess);
		}

		EO bestEO = null;

		//Calculate difficulty of problem (do each algo once, no pruning based on best to setup timings)
		int minimumIterations;
		int maximumIterations;
		int numOrderingsPerAlgoTSS = 100; //be careful about changing (due to timings)
		int numOrderingsPerAlgoMF = 50;   //be careful about changing (due to timings)
		int numOrderingsPerAlgoWMF = 50;  //be careful about changing (due to timings)
		long timePerIter;  //approx milliSec per iteration

		EliminationStructure2 esTSS_orig = new EliminationStructure2(bn, preprocessEO, EliminationStructure2.scoreTSS);
		EliminationStructure2 esMF_orig = new EliminationStructure2(bn, preprocessEO, EliminationStructure2.scoreMF);
		EliminationStructure2 esWMF_orig = new EliminationStructure2(bn, preprocessEO, EliminationStructure2.scoreWMF);
		{//difficulty of problem
			EO tmpEO;
			EliminationStructure2 esTSS = (EliminationStructure2)esTSS_orig.clone();
			EliminationStructure2 esMF = (EliminationStructure2)esMF_orig.clone();
			EliminationStructure2 esWMF = (EliminationStructure2)esWMF_orig.clone();

			long tmpTime1 = System.currentTimeMillis();
			tmpEO = esTSS.createEO(true/*deterministic*/, Double.MAX_VALUE);
			bestEO = EO.keepBest(bestEO, tmpEO);
			long tmpTime2 = System.currentTimeMillis();
			tmpEO = esMF.createEO(true/*deterministic*/, Double.MAX_VALUE);
			bestEO = EO.keepBest(bestEO, tmpEO);
			long tmpTime3 = System.currentTimeMillis();
			tmpEO = esWMF.createEO(true/*deterministic*/, Double.MAX_VALUE);
			bestEO = EO.keepBest(bestEO, tmpEO);
			long tmpTime4 = System.currentTimeMillis();

			if(DEBUG_VERBOSE) { System.out.println("Original Score: " + bestEO.eoScore());}

			long timeTSS = (tmpTime2-tmpTime1);
			long timeMF = (tmpTime3-tmpTime2);
			long timeWMF = (tmpTime4-tmpTime3);

			timePerIter = (timeTSS*numOrderingsPerAlgoTSS)+(timeMF*numOrderingsPerAlgoMF)+(timeWMF*numOrderingsPerAlgoWMF);
		}

		double lastScore = bestEO.eoScore();

		int algoUsed = 1; //1=TSS, 2=MF, 3=WMF
		long timeTSS = 0; int countTSS=0;
		long timeMF = 0;  int countMF=0;
		long timeWMF = 0; int countWMF=0;
		long tmpTime;
		long timeItrSt;
		boolean stop = false;


		int countIterations=1;
		int consecutiveItrsWithZeroIncrease = 0;
		while(true) {

			timeItrSt = System.currentTimeMillis();


			{//set minimum & maximum number of iterations
				double bs = bestEO.eoScore();
				double et = bs/callsPerMilliSec; //expected computation time (this is not accurate, as this doesn't represent RC calls)

				long tmpTotalTimeSoFar = timeTSS+timeMF+timeWMF;

				if(forceExtendedSearch) {
					minimumIterations = (int)Math.floor(.3 * et / timePerIter); //spend at least 30% of time searching
					maximumIterations = (int)Math.ceil( .75 * et / timePerIter); //spend at most  75% of time searching

					if(minimumIterations<10) {minimumIterations=10;}
					if(maximumIterations<10) {maximumIterations=10;}
				}
				else {

					if(countIterations==1 && (timePerIter>30000)) { //watch out for long searches when don't have reliable best cost figure
						timePerIter=(long)(timePerIter*50.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=50;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=25;
						if(DEBUG_VERBOSE) {System.out.println("\n\n\nReduce numOrderingsPerAlgoTSS to 50\n\n\n");}
					}

					if(timePerIter > .20*et) {  //if the time for a single iteration is > 20% of expected time, do simpler iterations
						timePerIter=(long)(timePerIter*10.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=10;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=5;
						if(DEBUG_VERBOSE) {System.out.println("\n\n\nReduce numOrderingsPerAlgoTSS to 10\n\n\n");}
					}
					else if(timePerIter > .10*et) {  //if the time for a single iteration is > 10% of expected time, do simpler iterations
						timePerIter=(long)(timePerIter*50.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=50;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=25;
						if(DEBUG_VERBOSE) {System.out.println("\n\n\nReduce numOrderingsPerAlgoTSS to 50\n\n\n");}
					}

					//Use these formulas since the length of the timePerIter may change
					//min = ((total min time - time so far) / timePerIter) + (currentIterations-1)
					//max = ((total max time - time so far) / timePerIter) + (currentIterations-1)

					minimumIterations = (int)Math.floor((.03 * et - tmpTotalTimeSoFar)/ timePerIter +(countIterations-1)); //spend at least 3% of time searching
					maximumIterations = (int)Math.ceil(( .10 * et - tmpTotalTimeSoFar)/ timePerIter +(countIterations-1)); //spend at most 10% of time searching

					if(minimumIterations<0) {minimumIterations=0;}
					if(maximumIterations<0) {maximumIterations=0;}

					if(et < 30000) { numOrderingsPerAlgoTSS=numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=5;stop=true;} //for easy networks, do fewer searches per iteration
				}

				if(DEBUG_VERBOSE) {
					System.out.println("\n\nIteration: " + countIterations);
					System.out.println("Expected computation time is " + (et/1000.0) + " (sec) for expected score of " + bs + " (not very accurate)");
					System.out.println("Time so far:       " + (tmpTotalTimeSoFar));
					System.out.println("MilliSec per Iter: " + timePerIter);
					System.out.println("min: " + minimumIterations + " (" + (minimumIterations*timePerIter/1000.0) + " sec)");
					System.out.println("max: " + maximumIterations + " (" + (maximumIterations*timePerIter/1000.0) + " sec)");
				}

				if(countIterations>maximumIterations) { //the above may have updated the max, if so possibly stop now (has not run the one listed as countIterations yet, so don't use >=)
					if(DEBUG_VERBOSE) {System.out.println("Searched the maximum number of iterations requested.\n");}
					break;
				}
			}


			//run TotalStateSpace
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoTSS; i++) {
				EliminationStructure2 esTSS = (EliminationStructure2)esTSS_orig.clone();
				EO tbestEO = EO.keepBest( bestEO, esTSS.createEO(false/*deterministic*/, bestEO.eoScore()));
				if(tbestEO != bestEO) {
					bestEO = tbestEO;
					algoUsed = EliminationStructure2.scoreTSS;
					if(DEBUG_VERBOSE) { System.out.println("Better score found by TSS: " + bestEO.eoScore() + "   iteration: " + countIterations);}
				}
				System.gc();
			}
			timeTSS += (System.currentTimeMillis() - tmpTime);
			countTSS+=numOrderingsPerAlgoTSS;

			//run MF
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoMF; i++) {
				EliminationStructure2 esMF = (EliminationStructure2)esMF_orig.clone();
				EO tbestEO = EO.keepBest( bestEO, esMF.createEO(false/*deterministic*/, bestEO.eoScore()));
				if(tbestEO != bestEO) {
					bestEO = tbestEO;
					algoUsed = EliminationStructure2.scoreMF;
					if(DEBUG_VERBOSE) { System.out.println("Better score found by MF: " + bestEO.eoScore() + "   iteration: " + countIterations);}
				}
				System.gc();
			}
			timeMF += (System.currentTimeMillis() - tmpTime);
			countMF+=numOrderingsPerAlgoMF;

			//run WMF
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoWMF; i++) {
				EliminationStructure2 esWMF = (EliminationStructure2)esWMF_orig.clone();
				EO tbestEO = EO.keepBest( bestEO, esWMF.createEO(false/*deterministic*/, bestEO.eoScore()));
				if(tbestEO != bestEO) {
					bestEO = tbestEO;
					algoUsed = EliminationStructure2.scoreWMF;
					if(DEBUG_VERBOSE) { System.out.println("Better score found by wMF: " + bestEO.eoScore() + "   iteration: " + countIterations);}
				}
				System.gc();
			}
			timeWMF += (System.currentTimeMillis() - tmpTime);
			countWMF+=numOrderingsPerAlgoWMF;

			double improvementRate = (lastScore - bestEO.eoScore()) / lastScore;
			if(DEBUG_VERBOSE) {System.out.println("Improvement Rate for iteration " + countIterations + " was " + (improvementRate));}

			if(stop) { //already found good ordering (not during forceExtendedSearch)
				if(DEBUG_VERBOSE) {System.out.println("Current ordering is already fast, skip rest of search.\n");}
				break;
			}
			else if(countIterations>=maximumIterations) {
				if(DEBUG_VERBOSE) {System.out.println("Searched the maximum number of iterations requested.\n");}
				break;
			}
			else {
				int extraItr = (countIterations-minimumIterations);
				if(extraItr>=0) { //if using more than the minimum number of iterations, force it to have a better improvementRate
					if(improvementRate < (.03 + (extraItr*.01))) { //on first extra iteration will be .04, then .05, .06, ...
						if(DEBUG_VERBOSE) {System.out.println("Stopping search because improvement was too small.\n");}
						break;
					}
				}
			}

			if(improvementRate==0.0) {
				consecutiveItrsWithZeroIncrease++;
				if((!forceExtendedSearch && consecutiveItrsWithZeroIncrease>=3) ||
				    (forceExtendedSearch && consecutiveItrsWithZeroIncrease>=10)) {
					if(DEBUG_VERBOSE) {System.out.println("Stopping search because had " + consecutiveItrsWithZeroIncrease + " consecutive iterations with no improvement.\n");}
					break;
				}
			}
			else {
				consecutiveItrsWithZeroIncrease=0;
			}

			lastScore = bestEO.eoScore();
			countIterations++;

			timePerIter = System.currentTimeMillis()-timeItrSt;
			System.gc();
		}


		String algoUsedStr = (algoUsed==EliminationStructure2.scoreTSS ? "TSS" : (algoUsed==EliminationStructure2.scoreMF ? "MF" : "wMF"));

		if(DEBUG_VERBOSE) {
			NumberFormat nfInt = NumberFormat.getInstance();
			NumberFormat nfDbl = NumberFormat.getInstance();

			nfInt.setMaximumFractionDigits(0);
			nfDbl.setMaximumFractionDigits(2);

			System.out.println("GenerateEO made " + countIterations + " iterations.");
			System.out.println("The final algorithm used was: " + algoUsedStr);
			System.out.println("GenerateEO found the best score of " + bestEO.eoScore());
			System.out.println("   time in TSS: " + nfDbl.format(timeTSS/1000.0) + "   (" + nfInt.format(countTSS) + " orders)");
			System.out.println("   time in  MF: " + nfDbl.format(timeMF/1000.0) + "   (" + nfInt.format(countMF) + " orders)");
			System.out.println("   time in wMF: " + nfDbl.format(timeWMF/1000.0) + "   (" + nfInt.format(countWMF) + " orders)");
			System.out.println("   total time : " + nfDbl.format((timeTSS+timeMF+timeWMF)/1000.0) + "   (" + nfInt.format(countTSS+countMF+countWMF) + " orders)");
			System.out.println("   minimum number of iterations was set to : " + minimumIterations);
			System.out.println("   maximum number of iterations was set to : " + maximumIterations);
//			System.out.println("   width: " + bestRC.width());
//			System.out.println("   tsScore: " + bestRC.tsScore());
			System.out.println("   eoScore: " + bestEO.eoScore());
//			System.out.println("   numCond: " + bestRC.numCond());
		}


		if(outDetailsFile != null) {
			try {
				File oDF = new File(outDetailsFile);
				boolean appending = oDF.exists();
				PrintStream outTable = new PrintStream(new BufferedOutputStream(new FileOutputStream(oDF,true/*append*/)),true/*autoflush*/);
				if(!appending) {
					outTable.println("\t"       +"Time\t\t\t\t"         +"NumOrders\t\t\t\t"    +"Iterations\t\t\t"  +"Vars\tAfter\t" +"Initial\t"+"Final\t"  +"Final\t");
					outTable.println("Network\t"+"Total\tTSS\tMF\tWMF\t"+"Total\tTSS\tMF\tWMF\t"+"Min\tMax\tActual\t"+"Total\tRules\t"+"FCCalls\t"+"FCCalls\t"+"Algo\t" );
				}
				outTable.println(netName+"\t"+timeTSS+"\t"+timeMF+"\t"+timeWMF+"\t"+(timeTSS+timeMF+timeWMF)+"\t"+countTSS+"\t"+countMF+"\t"+countWMF+"\t"+(countTSS+countMF+countWMF)+"\t"+minimumIterations+"\t"+maximumIterations+"\t"+countIterations+"\t"+numTotalVars+"\t"+(numTotalVars-preprocessEO.size())+"\t"+"-1\t-1\t"+algoUsedStr+"\t");
			}
			catch(FileNotFoundException e) {}
		}

		return bestEO;
	}//end generateEO



	static class EliminationStructure1 implements Cloneable {
		static final int scoreTSS = 1;
		static final int scoreMF = 2;
		static final int scoreWMF = 3;

		final MappedList fVars;
		final UndirectedGraphWithScores udg;
		final CollectionOfTables tables;

		final int scoreFunction;

		final private IntArr constrainedOrderingArr[][]; private int indxIntoConstrainedOrderingArr = 0;

		public EliminationStructure1(BeliefNetwork bn, ArrayList preprocessEO, int scoreFunction, Set constrainedOrdering[][]) {
			this.scoreFunction = scoreFunction;

			//create structures
			int numVars = bn.size();

			{
				ArrayList tmp = new ArrayList(bn);
				Collections.shuffle(tmp); //help randomize it a bit, also when cloning, randomize heap
				fVars = new MappedList(tmp);
			}

			if(constrainedOrdering==null) {
				constrainedOrderingArr = new IntArr[1][1];
				constrainedOrderingArr[0][0] = new IntArr(fVars.size());
				for(int i=0; i<fVars.size(); i++) constrainedOrderingArr[0][0].add(i);
			}
			else {
				boolean verify[] = new boolean[fVars.size()]; //confirm that all variables appear exactly once
				constrainedOrderingArr = new IntArr[constrainedOrdering.length][];
				for(int ord=0; ord<constrainedOrdering.length; ord++) {
					if(constrainedOrdering[ord] == null) {
						constrainedOrderingArr[ord] = new IntArr[1];
						constrainedOrderingArr[ord][0] = new IntArr(fVars.size());
						for(int i=0; i<fVars.size(); i++) constrainedOrderingArr[ord][0].add(i);
					}
					else {
						Arrays.fill(verify, false);
						constrainedOrderingArr[ord] = new IntArr[constrainedOrdering[ord].length];
						for(int i=0; i<constrainedOrdering[ord].length; i++) {
							constrainedOrderingArr[ord][i] = new IntArr(constrainedOrdering[ord][i].size());

							for(Iterator itr = constrainedOrdering[ord][i].iterator(); itr.hasNext();) {
								Object fv = itr.next();
								int nd = fVars.indexOf(fv);
								if(nd < 0) throw new IllegalStateException("Could not find: " + fv + " which was in constrained ordering " + ord);
								if(verify[nd]) throw new IllegalStateException(fv + " was in more than one constrained ordering set in ordering " + ord);
								verify[nd] = true;
								constrainedOrderingArr[ord][i].add(nd);
							}
						}

						for(int i=0; i<verify.length; i++) if(!verify[i]) throw new IllegalStateException(fVars.get(i) + " was not in any constrained ordering set in ordering " + ord);
					}
				}
			}



			tables = new CollectionOfTables(fVars);

			switch(scoreFunction) {
				case scoreTSS:
					udg = new UndirectedGraphTSS(tables);
					break;

				case scoreMF:
					udg = new UndirectedGraphMinFillTieTSS(tables, false);
					break;

				case scoreWMF:
					udg = new UndirectedGraphMinFillTieTSS(tables, true);
					break;

				default:
					throw new IllegalArgumentException("Illegal score function: " + scoreFunction);
			}

			//eliminate preprocessed variables
			for(int i=0; i<preprocessEO.size(); i++) {
				FiniteVariable fv = (FiniteVariable)preprocessEO.get(i);
				int varID = fVars.indexOf(fv);
				if(DEBUG_VERBOSE1) {System.out.println("\nPreprocess removing " + fv + " " + varID);}
				udg.eliminateUpdateScore(varID);
				tables.mergeAllTables(varID);
			}
			if(DEBUG_VERBOSE1) {System.out.println("Preprocess finished removing variables.");}
		}

		EliminationStructure1(EliminationStructure1 in) {
			fVars = (MappedList)in.fVars.clone();
			udg = (UndirectedGraphWithScores)in.udg.clone();
			tables = (CollectionOfTables)in.tables.clone();
			scoreFunction = in.scoreFunction;

			indxIntoConstrainedOrderingArr = in.indxIntoConstrainedOrderingArr;
			constrainedOrderingArr = new IntArr[in.constrainedOrderingArr.length][];
			for(int j=0; j<constrainedOrderingArr.length; j++) {
				constrainedOrderingArr[j] = new IntArr[in.constrainedOrderingArr[j].length];

				for(int i=0; i<constrainedOrderingArr[j].length; i++) {
					if(in.constrainedOrderingArr[j][i]!=null) constrainedOrderingArr[j][i] = (IntArr)in.constrainedOrderingArr[j][i].clone();
				}
			}
		}

		private void addNextWatchSetToGraph(int constrainedOrder) {
			//Tell graph to begin watching certain variables & their score
			for(; indxIntoConstrainedOrderingArr<constrainedOrderingArr[constrainedOrder].length; ) {
				udg.beginWatching(constrainedOrderingArr[constrainedOrder][indxIntoConstrainedOrderingArr]);
				indxIntoConstrainedOrderingArr++;
				if(udg.numWatching() > 0) break;
			}
		}


		public Object clone() { return new EliminationStructure1(this);}

		public final static int MAX_VERSION = 17;

		public RC createRC(boolean deterministic, double currentBest, int scoreVer) {
			return createRC(deterministic, currentBest, scoreVer, 0);
		}
		public RC createRC(boolean deterministic, double currentBest, int scoreVer, int constrainedOrder) {
			IntArr varsAtEnd = new IntArr();

			indxIntoConstrainedOrderingArr=0;
			addNextWatchSetToGraph(constrainedOrder);
			while(udg.numWatching()!=0) {

				double bestScr[] = udg.best3scores();
				int bestEl[] = udg.best3elements();
				int condElem = -1;
				int numBad = 0;

				CollectionOfTables.ScoreResults scr = null;

				switch(scoreVer) {
					case 1: //cost per table
					{
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.sumCounts / scr.numTables;
								if(scr.useImmediately) {
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
							}
							else numBad++;
						}
						break;
					}

					case 2: //cost
					{
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.sumCounts;
								if(scr.useImmediately) {
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
							}
							else numBad++;
						}
						break;
					}

					case 3: //root context
					{
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.rootContext;
								if(scr.useImmediately) {
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
							}
							else numBad++;
						}
						break;
					}

					case 4: //sum of context
					{
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.sumContext;
								if(scr.useImmediately) {
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
							}
							else numBad++;
						}
						break;
					}

					case 5: //max context
					{
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.maxContext;
								if(scr.useImmediately) {
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
							}
							else numBad++;
						}
						break;
					}

					case 6: //max context with Conditioning (find var in most tables)
					{
						boolean foundOne = false;
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.maxContext;
								if(scr.useImmediately) {
									foundOne=true;
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
								else if(bestScr[i]>largestAcceptableRCCache) {bestScr[i]=0; numBad++;}
								else {foundOne=true;}
							}
							else numBad++;
						}
						if(!foundOne) {
							condElem = tables.varInMostTables(varsAtEnd);
							if(condElem < 0) throw new IllegalStateException();
						}
						break;
					}

					case 7: //max context with Conditioning (find one of 3 in most tables)
					{
						boolean foundOne = false;
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.maxContext;
								if(scr.useImmediately) {
									foundOne=true;
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
								else if(bestScr[i]>largestAcceptableRCCache) {bestScr[i]=0; numBad++;}
								else {foundOne=true;}
							}
							else numBad++;
						}
						if(!foundOne) {
							for(int i=0; i<bestEl.length; i++) {
								if(bestEl[i]>=0) {
									if(condElem<0) { condElem=bestEl[i];}
									else if(tables.varCounts[bestEl[i]] > tables.varCounts[condElem]) { condElem=bestEl[i];}
								}
							}
							if(condElem < 0) throw new IllegalStateException();
						}
						break;
					}

					case 8: //cost with Conditioning (find var in most tables)
					{
						boolean foundOne = false;
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.sumCounts;
								if(scr.useImmediately) {
									foundOne=true;
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
								else if(bestScr[i]>largestAcceptableRCCost) {bestScr[i]=0; numBad++;}
								else {foundOne=true;}
							}
							else numBad++;
						}
						if(!foundOne) {
							condElem = tables.varInMostTables(varsAtEnd);
							if(condElem < 0) throw new IllegalStateException();
						}
						break;
					}

					case 9: //cost with Conditioning (find one of 3 in most tables)
					{
						boolean foundOne = false;
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.sumCounts;
								if(scr.useImmediately) {
									foundOne=true;
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
								else if(bestScr[i]>largestAcceptableRCCost) {bestScr[i]=0; numBad++;}
								else {foundOne=true;}
							}
							else numBad++;
						}
						if(!foundOne) {
							for(int i=0; i<bestEl.length; i++) {
								if(bestEl[i]>=0) {
									if(condElem<0) { condElem=bestEl[i];}
									else if(tables.varCounts[bestEl[i]] > tables.varCounts[condElem]) { condElem=bestEl[i];}
								}
							}
							if(condElem < 0) throw new IllegalStateException();
						}
						break;
					}




					case 10: //max context with Conditioning (find var in fewest tables)
					{
						boolean foundOne = false;
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.maxContext;
								if(scr.useImmediately) {
									foundOne=true;
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
								else if(bestScr[i]>largestAcceptableRCCache) {bestScr[i]=0; numBad++;}
								else {foundOne=true;}
							}
							else numBad++;
						}
						if(!foundOne) {
							condElem = tables.varInFewestTables(varsAtEnd);
							if(condElem < 0) throw new IllegalStateException();
						}
						break;
					}

					case 11: //max context with Conditioning (find one of 3 in fewest tables)
					{
						boolean foundOne = false;
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.maxContext;
								if(scr.useImmediately) {
									foundOne=true;
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
								else if(bestScr[i]>largestAcceptableRCCache) {bestScr[i]=0; numBad++;}
								else {foundOne=true;}
							}
							else numBad++;
						}
						if(!foundOne) {
							for(int i=0; i<bestEl.length; i++) {
								if(bestEl[i]>=0) {
									if(condElem<0) { condElem=bestEl[i];}
									else if(tables.varCounts[bestEl[i]] < tables.varCounts[condElem]) { condElem=bestEl[i];}
								}
							}
							if(condElem < 0) throw new IllegalStateException();
						}
						break;
					}

					case 12: //cost with Conditioning (find var in fewest tables)
					{
						boolean foundOne = false;
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.sumCounts;
								if(scr.useImmediately) {
									foundOne=true;
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
								else if(bestScr[i]>largestAcceptableRCCost) {bestScr[i]=0; numBad++;}
								else {foundOne=true;}
							}
							else numBad++;
						}
						if(!foundOne) {
							condElem = tables.varInFewestTables(varsAtEnd);
							if(condElem < 0) throw new IllegalStateException();
						}
						break;
					}

					case 13: //cost with Conditioning (find one of 3 in fewest tables)
					{
						boolean foundOne = false;
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.sumCounts;
								if(scr.useImmediately) {
									foundOne=true;
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
								else if(bestScr[i]>largestAcceptableRCCost) {bestScr[i]=0; numBad++;}
								else {foundOne=true;}
							}
							else numBad++;
						}
						if(!foundOne) {
							for(int i=0; i<bestEl.length; i++) {
								if(bestEl[i]>=0) {
									if(condElem<0) { condElem=bestEl[i];}
									else if(tables.varCounts[bestEl[i]] < tables.varCounts[condElem]) { condElem=bestEl[i];}
								}
							}
							if(condElem < 0) throw new IllegalStateException();
						}
						break;
					}

					case 14: //max context with special elimination
					{
						boolean foundOne = false;
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.maxContext;
								if(scr.useImmediately) {
									foundOne=true;
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
								else if(bestScr[i]>largestAcceptableRCCache) {bestScr[i]=0; numBad++;}
								else {foundOne=true;}
							}
							else numBad++;
						}
						if(!foundOne) {
							bestEl[0] = tables.varInMostTables(varsAtEnd);
							bestScr[0] = 1.0;
							numBad=0;
						}
						break;
					}

					case 15: //max context with special elimination
					{
						boolean foundOne = false;
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								scr = tables.calcScore(bestEl[i]);
								bestScr[i] = scr.maxContext;
								if(scr.useImmediately) {
									foundOne=true;
									Arrays.fill(bestScr,0);
									bestScr[i]=1.0;
									break;
								}
								if(scr.dontUse) {bestScr[i]=0.0; numBad++;}
								else if(bestScr[i]>largestAcceptableRCCache) {bestScr[i]=0; numBad++;}
								else {foundOne=true;}
							}
							else numBad++;
						}
						if(!foundOne) {
							bestEl[0] = tables.varInFewestTables(varsAtEnd);
							bestScr[0] = 1.0;
							numBad=0;
						}
						break;
					}

					case 16: //random
					{
						for(int i=0; i<bestEl.length; i++) {
							if(bestEl[i]>=0) {
								bestScr[i]=1.0;
							}
							else numBad++;
						}
						break;
					}

					case 17: //MF
					{
						//already have MF in bestScr
						//however inc all by .5 to handle case of 0
						for(int i=0; i<bestScr.length; i++) {
							if(bestEl[i]>=0) {
								bestScr[i]+=0.5;
							}
							else numBad++;
						}
						break;
					}
					default: throw new IllegalStateException();
				}//end switch score

				if(numBad==bestScr.length && condElem<0) { //if expecting to eliminate, but all have invalid scores, condition instead

					if(bestScr[0]>0) throw new IllegalStateException("Score was " + bestScr[0]);
					if(bestScr[1]>0) throw new IllegalStateException("Score was " + bestScr[1]);
					if(bestScr[2]>0) throw new IllegalStateException("Score was " + bestScr[2]);

					//condElem = tables.varInMostTables(varsAtEnd);
					condElem = bestEl[0];
					if(DEBUG_VERBOSE1) System.out.println("All scores were problematic, condition on variable " + condElem);
				}


				if(DEBUG_VERBOSE1) {
					System.out.println("Scores: " + bestScr[0] + " " + bestScr[1] + " " + bestScr[2]);
					System.out.println("Elements: " + bestEl[0] + " " + bestEl[1] + " " + bestEl[2]);
					System.out.println("CondElem: " + condElem);
				}


				if(condElem<0) { //eliminate a variable
					int elimElem = (bestEl[0]>=0 ? bestEl[0] : (bestEl[1]>=0 ? bestEl[1] : bestEl[2]));
					if(!deterministic) {
						double power = -0.5; //use negative so that it helps smaller numbers
						double b1 = (bestScr[0]<=0 ? 0 : Math.pow(bestScr[0], power));
						double b2 = (bestScr[1]<=0 ? 0 : Math.pow(bestScr[1], power));
						double b3 = (bestScr[2]<=0 ? 0 : Math.pow(bestScr[2], power));
						double bAll = b1+b2+b3;

						if(Double.isNaN(bAll) || Double.isInfinite(bAll)) {
							System.out.println("Scores: " + bestScr[0] + " " + bestScr[1] + " " + bestScr[2]);
							System.out.println("Bs: " + b1 + " " + b2 + " " + b3 + " " + bAll);
							throw new IllegalStateException();
						}

						double r = Math.random();

						if(r < (b1/bAll)) {
							elimElem = bestEl[0];
							if(DEBUG_VERBOSE1) { System.out.println("Using elimination on v1");}
						}
						else if(r < ((b1+b2)/bAll)) {
							elimElem = bestEl[1];
							if(DEBUG_VERBOSE1) { System.out.println("Using elimination on v2");}
						}
						else if(r < ((b1+b2+b3)/bAll)) {
							elimElem = bestEl[2];
							if(DEBUG_VERBOSE1) { System.out.println("Using elimination on v3");}
						}
						else {
							System.out.println("Scores: " + bestScr[0] + " " + bestScr[1] + " " + bestScr[2]);
							System.out.println("Bs: " + b1 + " " + b2 + " " + b3 + " " + bAll);
							System.out.println("r: " + r);
							throw new IllegalStateException();
						}
					}

					if(elimElem<0) throw new IllegalStateException();

					udg.eliminateUpdateScore(elimElem);
					if(tables.mergeAllTables(elimElem) >= currentBest && currentBest!=Double.MAX_VALUE) {
						return null;
					}
					if(udg.numWatching()==0) addNextWatchSetToGraph(constrainedOrder);
				}
				else {//condition a variable
					udg.conditionUpdateScore(condElem);
					varsAtEnd.add(condElem);
					if(udg.numWatching()==0) addNextWatchSetToGraph(constrainedOrder);
				}
			}//end while graph is not empty

			int numCondUsed = varsAtEnd.size();
			while(!varsAtEnd.isEmpty()) {
				if(tables.mergeAllTables(varsAtEnd.removeLast()) >= currentBest && currentBest!=Double.MAX_VALUE) {
					return null;
				}
			}

			if(DEBUG_VERBOSE) System.out.println("Used Cond: " + numCondUsed + " times.");

			return tables.finishCreatingRC();
		}//end createRC
	}//end class EliminationStructure1



	/*Does not do RC objects, only elimination orders.*/
	static class EliminationStructure2 implements Cloneable {
		static final int scoreTSS = 1;
		static final int scoreMF = 2;
		static final int scoreWMF = 3;

		final MappedList fVars;
		final UndirectedGraphWithScores udg;
		EO eo;

		final int scoreFunction;

		public EliminationStructure2(BeliefNetwork bn, ArrayList preprocessEO, int scoreFunction) {
			this.scoreFunction = scoreFunction;
			eo = new EO(bn.size());

			//create structures
			int numVars = bn.size();

			{
				ArrayList tmp = new ArrayList(bn);
				Collections.shuffle(tmp); //help randomize it a bit, also when cloning, randomize heap
				fVars = new MappedList(tmp);
			}

			switch(scoreFunction) {
				case scoreTSS:
					udg = new UndirectedGraphTSS(Graphs.moralGraph(bn), fVars);
					break;

				case scoreMF:
					udg = new UndirectedGraphMinFillTieTSS(Graphs.moralGraph(bn), fVars, false);
					break;

				case scoreWMF:
					udg = new UndirectedGraphMinFillTieTSS(Graphs.moralGraph(bn), fVars, true);
					break;

				default:
					throw new IllegalArgumentException("Illegal score function: " + scoreFunction);
			}

			//tell graph to watch all variables
			{
				IntArr all = new IntArr(fVars.size());
				for(int i=0; i<fVars.size(); i++) all.add(i);
				udg.beginWatching(all);
			}

			//eliminate preprocessed variables
			for(int i=0; i<preprocessEO.size(); i++) {
				FiniteVariable fv = (FiniteVariable)preprocessEO.get(i);
				int varID = fVars.indexOf(fv);
				if(DEBUG_VERBOSE1) {System.out.println("\nPreprocess removing " + fv + " " + varID);}
				eo.elimVar(fv, udg.numNei(varID), udg.neiTSS(varID));
				udg.eliminateUpdateScore(varID);
			}
			if(DEBUG_VERBOSE1) {System.out.println("Preprocess finished removing variables.");}
		}

		EliminationStructure2(EliminationStructure2 in) {
			fVars = (MappedList)in.fVars.clone();
			udg = (UndirectedGraphWithScores)in.udg.clone();
			eo = (EO)in.eo.clone();
			scoreFunction = in.scoreFunction;
		}

		public Object clone() { return new EliminationStructure2(this);}

		public EO createEO(boolean deterministic, double currentBest) {
			EO ret = (EO)eo.clone();

			while(udg.numWatching()!=0) {
				int bestEl[] = udg.best3elements();
				int elimElem = (bestEl[0]>=0 ? bestEl[0] : (bestEl[1]>=0 ? bestEl[1] : bestEl[2]));
				double bestTSS[] = udg.best3TSS();

				if(!deterministic) {
					double power = -0.5; //use negative so that it helps smaller numbers
					double b1 = (bestTSS[0]<=0 ? 0 : Math.pow(bestTSS[0], power));
					double b2 = (bestTSS[1]<=0 ? 0 : Math.pow(bestTSS[1], power));
					double b3 = (bestTSS[2]<=0 ? 0 : Math.pow(bestTSS[2], power));
					double bAll = b1+b2+b3;

					if(Double.isNaN(bAll) || Double.isInfinite(bAll)) {
						System.out.println("Scores: " + bestTSS[0] + " " + bestTSS[1] + " " + bestTSS[2]);
						System.out.println("Bs: " + b1 + " " + b2 + " " + b3 + " " + bAll);
						throw new IllegalStateException();
					}

					double r = Math.random();

					if(r < (b1/bAll)) {
						elimElem = bestEl[0];
						if(DEBUG_VERBOSE1) { System.out.println("Using elimination on v1");}
					}
					else if(r < ((b1+b2)/bAll)) {
						elimElem = bestEl[1];
						if(DEBUG_VERBOSE1) { System.out.println("Using elimination on v2");}
					}
					else if(r < ((b1+b2+b3)/bAll)) {
						elimElem = bestEl[2];
						if(DEBUG_VERBOSE1) { System.out.println("Using elimination on v3");}
					}
					else {
						System.out.println("Scores: " + bestTSS[0] + " " + bestTSS[1] + " " + bestTSS[2]);
						System.out.println("Bs: " + b1 + " " + b2 + " " + b3 + " " + bAll);
						System.out.println("r: " + r);
						throw new IllegalStateException();
					}
				}
				if(elimElem<0) throw new IllegalStateException();

				if(udg.neiTSS(elimElem) <= largestAcceptableEOTSS) { //eliminate
					ret.elimVar((FiniteVariable)fVars.get(elimElem), udg.numNei(elimElem), udg.neiTSS(elimElem));
					if(ret.eoScore() >= currentBest) return null;
					udg.eliminateUpdateScore(elimElem);
				}
				else { //condition
					double c1 = (bestTSS[0]<0 ? (-1) : Math.sqrt(udg.numNei(bestEl[0]))*bestTSS[0]);
					double c2 = (bestTSS[1]<0 ? (-1) : Math.sqrt(udg.numNei(bestEl[1]))*bestTSS[1]);
					double c3 = (bestTSS[2]<0 ? (-1) : Math.sqrt(udg.numNei(bestEl[2]))*bestTSS[2]);
					int condElem;

					if(c1>=c2 && c1>=c3) condElem = bestEl[0];
					else if(c2>=c1 && c2>=c3) condElem = bestEl[1];
					else if(c3>=c1 && c3>=c2) condElem = bestEl[2];
					else throw new IllegalStateException();

					ret.condVar((FiniteVariable)fVars.get(elimElem), udg.numNei(elimElem), udg.neiTSS(elimElem), udg.varSize(elimElem));
					if(ret.eoScore() >= currentBest) return null;
					udg.eliminateUpdateScore(elimElem); //this will connect neighbors up as well, which Geiger doesn't do
				}
			}

			return ret;
		}//end createEO
	}//end class EliminationStructure2




	private interface UndirectedGraphWithScores extends Cloneable {
		public void eliminateUpdateScore(int remove);
		public void conditionUpdateScore(int remove);

		public void beginWatching(IntArr watch);
		public int numWatching();

		public int numNei(int var);
		public double neiTSS(int var);
		public int varSize(int var);

		public double[] best3scores();
		public int[] best3elements();
		public double[] best3TSS();

		public double bestScore();
		public int bestElement();

		public Object clone();
	}


	/**An undirected graph maintaining min fill scores, which breaks ties using
	 *  the total state space of neighbors.
	 *  Scores are only kept on variables after a call to beginWatching.
	 */
	final static class UndirectedGraphMinFillTieTSS implements UndirectedGraphWithScores {
		static final private int NEI_CUTOFF = 10;
		static final private boolean DEBUG_UNDGR = false;

		final int varSizes[];				//number of states of a given node (once created, never changes)
		final OrderedIntArr neighbors[]; 	//ordered list of neighbors of a given node
		final double neighborsTSS[]; 		//the total state space of all neighbors of a given node, including itself

		final IntHeapMin scores;
		final private boolean useWeightedMinFill;

		final boolean tempBool[]; //watch out that it isn't being used by more than one function at a time

		public UndirectedGraphMinFillTieTSS(CollectionOfTables tables, boolean useWeightedMinFill) {
			neighbors = tables.fillNeighbors();
			varSizes = (int[])tables.varSizes.clone();
			tempBool = new boolean[neighbors.length];
			this.useWeightedMinFill = useWeightedMinFill;
			neighborsTSS = new double[neighbors.length];
			{
				Arrays.fill(neighborsTSS, 1.0);
				for(int i=0; i<neighbors.length; i++) { //for each variable
					neighborsTSS[i] *= varSizes[i];
					for(int j=0; j<neighbors[i].size(); j++) { //multiple sizes of its neighbors
						int nei = neighbors[i].get(j);
						neighborsTSS[i] *= varSizes[nei];
					}
					if(DEBUG_UNDGR) {System.out.println("Var " + i + " has " + neighbors[i].size() + " neighbors, with a TSS of " + neighborsTSS[i]);}
					if(DEBUG_VERBOSE1) {System.out.println("    Neighbors: " + i + ": " + neighbors[i]);}
				}
			}
			scores = new IntHeapMin(neighbors.length);
		}

		public UndirectedGraphMinFillTieTSS(Graph moralGr, MappedList fVars, boolean useWeightedMinFill) {
			{
				varSizes = new int[moralGr.size()];
				neighbors = new OrderedIntArr[moralGr.size()];

				for(int i=0; i<varSizes.length; i++) {
					FiniteVariable v = (FiniteVariable)fVars.get(i);
					varSizes[i] = v.size();
					Set nei = moralGr.neighbors(v);
					neighbors[i] = new OrderedIntArr(3*nei.size()/2); //make it bigger than current for expansion
					for(Iterator itr = nei.iterator(); itr.hasNext();) {
						neighbors[i].add(fVars.indexOf(itr.next()));
					}
				}
			}


			tempBool = new boolean[neighbors.length];
			this.useWeightedMinFill = useWeightedMinFill;
			neighborsTSS = new double[neighbors.length];
			{
				Arrays.fill(neighborsTSS, 1.0);
				for(int i=0; i<neighbors.length; i++) { //for each variable
					neighborsTSS[i] *= varSizes[i];
					for(int j=0; j<neighbors[i].size(); j++) { //multiple sizes of its neighbors
						int nei = neighbors[i].get(j);
						neighborsTSS[i] *= varSizes[nei];
					}
					if(DEBUG_UNDGR) {System.out.println("Var " + i + " has " + neighbors[i].size() + " neighbors, with a TSS of " + neighborsTSS[i]);}
					if(DEBUG_VERBOSE1) {System.out.println("    Neighbors: " + i + ": " + neighbors[i]);}
				}
			}
			scores = new IntHeapMin(neighbors.length);
		}

		/**Randomizes heap during creation.*/
		UndirectedGraphMinFillTieTSS(UndirectedGraphMinFillTieTSS in) {
			varSizes = (int[])in.varSizes.clone();
			neighbors = new OrderedIntArr[varSizes.length];
			for(int i=0; i<neighbors.length; i++) {
				if(in.neighbors[i]==null) neighbors[i] = null;
				else neighbors[i] = (OrderedIntArr)in.neighbors[i].clone();
			}
			neighborsTSS = (double[])in.neighborsTSS.clone();
			this.useWeightedMinFill = in.useWeightedMinFill;
			scores = (IntHeapMin)in.scores.clone();
			scores.randomize();
			tempBool = (boolean[])in.tempBool.clone();
		}

		/**Randomizes heap during creation.*/
		public Object clone() { return new UndirectedGraphMinFillTieTSS(this);}

		public double[] best3scores() {scores.fillBest3(); return scores.best3scores;}
		public int[] best3elements() {scores.fillBest3(); return scores.best3elements;}
		public double[] best3TSS() {scores.fillBest3(); return scores.best3TieBreaker;}
		public double bestScore() {scores.fillBest3(); return scores.best3scores[0];}
		public int bestElement() {scores.fillBest3(); return scores.best3elements[0];}


		public void beginWatching(IntArr watch) {
			for(int i=0; i<watch.size(); i++) {
				int node = watch.get(i);
				if(neighbors[node]!=null) scores.insert(node, bruteForceScore(node), neighborsTSS[node]);
			}
		}
		public int numWatching() {return scores.size();}


		private static void fillNeighbors(OrderedIntArr neiList, boolean temp[]) {
			for(int i=neiList.size()-1; i>=0; i--) {
				temp[neiList.get(i)] = true;
			}
		}

		public int numNei(int var) {return neighbors[var].size();}
		public double neiTSS(int var) {return neighborsTSS[var];}
		public int varSize(int var) {return varSizes[var];}

		final private double bruteForceScore(int node) {
			return (useWeightedMinFill ? bruteForceWeightedMinFill(node) : bruteForceMinFill(node));
		}

		private double bruteForceMinFill(int node) {
			double ret = 0;

			for(int indx1=0; indx1<neighbors[node].size(); indx1++) {
				int var1 = neighbors[node].get(indx1);

				if(neighbors[var1].size() < NEI_CUTOFF || ((neighbors[node].size()-indx1) < NEI_CUTOFF)) { //this node doesn't have many neighbors or will not check many, easy to search
					for(int indx2=indx1+1; indx2<neighbors[node].size(); indx2++) {
						int var2 = neighbors[node].get(indx2);

						if(!neighbors[var1].containsValue(var2)) { ret+=1.0;}
					}
				}
				else { //this node has many neighbors, make neighbor array instead of individually searching
					Arrays.fill(tempBool, false);
					fillNeighbors(neighbors[var1], tempBool);
					for(int indx2=indx1+1; indx2<neighbors[node].size(); indx2++) {
						int var2 = neighbors[node].get(indx2);

						if(!tempBool[var2]) { ret+=1.0;}
					}
				}
			}
			return ret;
		}

		private double bruteForceWeightedMinFill(int node) {
			double ret = 0;

			for(int indx1=0; indx1<neighbors[node].size(); indx1++) {
				int var1 = neighbors[node].get(indx1);

				if(neighbors[var1].size() < NEI_CUTOFF || ((neighbors[node].size()-indx1) < NEI_CUTOFF)) { //this node doesn't have many neighbors or will not check many, easy to search
					for(int indx2=indx1+1; indx2<neighbors[node].size(); indx2++) {
						int var2 = neighbors[node].get(indx2);

						if(!neighbors[var1].containsValue(var2)) { ret+=(varSizes[var1] * varSizes[var2]);}
					}
				}
				else { //this node has many neighbors, make neighbor array instead of individually searching
					Arrays.fill(tempBool, false);
					fillNeighbors(neighbors[var1], tempBool);
					for(int indx2=indx1+1; indx2<neighbors[node].size(); indx2++) {
						int var2 = neighbors[node].get(indx2);

						if(!tempBool[var2]) { ret+=(varSizes[var1] * varSizes[var2]);}
					}
				}
			}
			return ret;
		}

		private void removeVariableAndEdges(int rem) {
			if(DEBUG_UNDGR) {System.out.println("UndGr.removeVariableAndEdges(" + rem + ")");}

			for(int neiIndx=0; neiIndx<neighbors[rem].size(); neiIndx++) {
				int nei = neighbors[rem].get(neiIndx);
				neighbors[nei].removeFirstValue(rem);
				neighborsTSS[nei] /= varSizes[rem];
				scores.score2Changed(nei, neighborsTSS[nei]);
			}
			neighbors[rem].clear();
			neighbors[rem] = null;
			neighborsTSS[rem] = 0;
			scores.removeElement(rem);
		}


		public void conditionUpdateScore(int remove) {
			if(DEBUG_UNDGR) {System.out.println("UndGr.conditionUpdateScore(" + remove + "), score was " + scores.score1OfElement(remove));}
			if(DEBUG_EXTRA_TESTING) {
				double s1 = scores.score1OfElement(remove);
				double s2 = bruteForceScore(remove);
				if(( Math.abs(s1-s2) ) > .000001) throw new IllegalStateException("MinFill score error: " + s1 + " " + s2);
			}

			//For each neighbor of var1, reduce score(var1) by 1 for each neighbor not near "remove"
			{
				//neighbors of "remove" into tempBool
				Arrays.fill(tempBool, false);
				fillNeighbors(neighbors[remove], tempBool);
				tempBool[remove]=true; //make remove a neighbor of itself so as to not change the score

				for(int indx1=0; indx1<neighbors[remove].size(); indx1++) {
					int var1 = neighbors[remove].get(indx1);

					double change=0;
					for(int indx2=0; indx2<neighbors[var1].size(); indx2++) {
						int var2 = neighbors[var1].get(indx2);
						if(!tempBool[var2]) {  //if var2 is not neighbor of remove, decrement score
							if(useWeightedMinFill) change-=(varSizes[var2]*varSizes[remove]);
							else change-=1.0;
						}
					}
					if(change!=0) { scores.score1Changed_delta(var1, change);}
				}
			}
			removeVariableAndEdges(remove);
		}

		public void eliminateUpdateScore(int remove) {
			if(DEBUG_UNDGR) {System.out.println("UndGr.eliminateUpdateScore(" + remove + "), score was " + scores.score1OfElement(remove));}
			if(DEBUG_EXTRA_TESTING) {
				double s1 = scores.score1OfElement(remove);
				double s2 = bruteForceScore(remove);
				if(( Math.abs(s1-s2) ) > .000001) throw new IllegalStateException("MinFill score error: " + s1 + " " + s2);
			}

			//For each neighbor of var1, reduce score(var1) by 1 for each neighbor not near "remove"
			{
				//neighbors of "remove" into tempBool
				Arrays.fill(tempBool, false);
				fillNeighbors(neighbors[remove], tempBool);
				tempBool[remove]=true; //make remove a neighbor of itself so as to not change the score

				for(int indx1=0; indx1<neighbors[remove].size(); indx1++) {
					int var1 = neighbors[remove].get(indx1);

					double change=0;
					for(int indx2=0; indx2<neighbors[var1].size(); indx2++) {
						int var2 = neighbors[var1].get(indx2);
						if(!tempBool[var2]) {  //if var2 is not neighbor of remove, decrement score
							if(useWeightedMinFill) change-=(varSizes[var2]*varSizes[remove]);
							else change-=1.0;
						}
					}
					if(change!=0) { scores.score1Changed_delta(var1, change);}
				}
			}

			//Add edges and continue updating min fill scores when additions occur
			{
				for(int indx1=0; indx1<neighbors[remove].size(); indx1++) {
					int var1 = neighbors[remove].get(indx1);

					if(neighbors[var1].size() < NEI_CUTOFF || ((neighbors[remove].size()-indx1) < NEI_CUTOFF)) { //this node doesn't have many neighbors, easy to search
						for(int indx2=indx1+1; indx2<neighbors[remove].size(); indx2++) {
							int var2 = neighbors[remove].get(indx2);

							if(neighbors[var1].addIfNew(var2)) { //add edge var1-var2
								if(!neighbors[var2].addIfNew(var1)) throw new IllegalStateException();

								neighborsTSS[var1] *= varSizes[var2];
								scores.score2Changed(var1, neighborsTSS[var1]);
								neighborsTSS[var2] *= varSizes[var1];
								scores.score2Changed(var2, neighborsTSS[var2]);

								updateMinFillAddedEdge(var1, var2, scores);
							}
						}
					}
					else { //this node has many neighbors, make neighbor array instead of individually searching
						Arrays.fill(tempBool, false);
						fillNeighbors(neighbors[var1], tempBool); //tempBool has neighbors of var1
						for(int indx2=indx1+1; indx2<neighbors[remove].size(); indx2++) {
							int var2 = neighbors[remove].get(indx2);

							if(!tempBool[var2]) { //add edge var1-var2
								if(!neighbors[var1].addIfNew(var2)) throw new IllegalStateException();
								if(!neighbors[var2].addIfNew(var1)) throw new IllegalStateException();

								neighborsTSS[var1] *= varSizes[var2];
								scores.score2Changed(var1, neighborsTSS[var1]);
								neighborsTSS[var2] *= varSizes[var1];
								scores.score2Changed(var2, neighborsTSS[var2]);

								updateMinFillAddedEdge(var1, var2, scores);
							}
						}
					}
				}
			}
			//score should be 0
			{
				double sc = scores.score1OfElement(remove);
				if(sc<0) sc=0;
				if( Math.abs(sc) >.0000001) {throw new IllegalStateException("Score was: " + sc + ", was expecting a 0.");}
			}
			removeVariableAndEdges(remove);
		}//end eliminateUpdateScore

		private void updateMinFillAddedEdge(int v1, int v2, IntHeapMin scores) {
			int next1Indx = 0;
			int next2Indx = 0;

			//iterate through neighbors of v1 and v2 adjusting scores
			for(;;) {
				if(next1Indx>=neighbors[v1].size()) next1Indx=-1;
				if(next2Indx>=neighbors[v2].size()) next2Indx=-1;
				if(next1Indx<0 && next2Indx<0) break;

				int next1Var = (next1Indx<0 ? Integer.MAX_VALUE : neighbors[v1].get(next1Indx));
				int next2Var = (next2Indx<0 ? Integer.MAX_VALUE : neighbors[v2].get(next2Indx));

				//hava actually already added v1-v2, so they show up as neighbors, pretend they don't
				if(next1Var==v2) {next1Indx++; continue;}
				if(next2Var==v1) {next2Indx++; continue;}

				if(next1Var==next2Var) {
					scores.score1Changed_delta(next1Var, (useWeightedMinFill ? (-varSizes[v1]*varSizes[v2]) : -1.0));
					next1Indx++;
					next2Indx++;
				}
				else if(next1Var<next2Var) {
					scores.score1Changed_delta(v1, (useWeightedMinFill ? (varSizes[next1Var] * varSizes[v2]) : 1.0));
					next1Indx++;
				}
				else if(next1Var>next2Var) {
					scores.score1Changed_delta(v2, (useWeightedMinFill ? (varSizes[next2Var] * varSizes[v1]) : 1.0));
					next2Indx++;
				}
			}
		}//end addEdgeUpdateMinFill

	}//end class UndirectedGraphMinFillTieTSS


	/**An undirected graph maintaining TSS scores, which breaks ties randomly (i.e. no tie breaker).
	 *  Scores are only kept on variables after a call to beginWatching.
	 */
	final static class UndirectedGraphTSS implements UndirectedGraphWithScores {
		static final private int NEI_CUTOFF = 10;
		static final private boolean DEBUG_UNDGR = false;

		final int varSizes[];				//number of states of a given node (once created, never changes)
		final OrderedIntArr neighbors[]; 	//ordered list of neighbors of a given node
		final double neighborsTSS[]; 		//the total state space of all neighbors of a given node, including itself

		final IntHeapMin scores;

		final boolean tempBool[]; //watch out that it isn't being used by more than one function at a time

		public UndirectedGraphTSS(CollectionOfTables tables) {
			neighbors = tables.fillNeighbors();
			varSizes = (int[])tables.varSizes.clone();
			tempBool = new boolean[neighbors.length];
			scores = new IntHeapMin(neighbors.length);
			neighborsTSS = new double[neighbors.length];
			{
				Arrays.fill(neighborsTSS, 1.0);
				for(int i=0; i<neighbors.length; i++) { //for each variable
					neighborsTSS[i] *= varSizes[i];
					for(int j=0; j<neighbors[i].size(); j++) { //multiple sizes of its neighbors
						int nei = neighbors[i].get(j);
						neighborsTSS[i] *= varSizes[nei];
					}
					if(DEBUG_UNDGR) {System.out.println("Var " + i + " has " + neighbors[i].size() + " neighbors, with a TSS of " + neighborsTSS[i]);}
					if(DEBUG_VERBOSE1) {System.out.println("    Neighbors: " + i + ": " + neighbors[i]);}
				}
			}
		}

		public UndirectedGraphTSS(Graph moralGr, MappedList fVars) {
			{
				varSizes = new int[moralGr.size()];
				neighbors = new OrderedIntArr[moralGr.size()];

				for(int i=0; i<varSizes.length; i++) {
					FiniteVariable v = (FiniteVariable)fVars.get(i);
					varSizes[i] = v.size();
					Set nei = moralGr.neighbors(v);
					neighbors[i] = new OrderedIntArr(3*nei.size()/2); //make it bigger than current for expansion
					for(Iterator itr = nei.iterator(); itr.hasNext();) {
						neighbors[i].add(fVars.indexOf(itr.next()));
					}
				}
			}


			tempBool = new boolean[neighbors.length];
			scores = new IntHeapMin(neighbors.length);
			neighborsTSS = new double[neighbors.length];
			{
				Arrays.fill(neighborsTSS, 1.0);
				for(int i=0; i<neighbors.length; i++) { //for each variable
					neighborsTSS[i] *= varSizes[i];
					for(int j=0; j<neighbors[i].size(); j++) { //multiple sizes of its neighbors
						int nei = neighbors[i].get(j);
						neighborsTSS[i] *= varSizes[nei];
					}
					if(DEBUG_UNDGR) {System.out.println("Var " + i + " has " + neighbors[i].size() + " neighbors, with a TSS of " + neighborsTSS[i]);}
					if(DEBUG_VERBOSE1) {System.out.println("    Neighbors: " + i + ": " + neighbors[i]);}
				}
			}
		}

		/**Randomizes heap during creation.*/
		UndirectedGraphTSS(UndirectedGraphTSS in) {
			varSizes = (int[])in.varSizes.clone();
			neighbors = new OrderedIntArr[varSizes.length];
			for(int i=0; i<neighbors.length; i++) {
				if(in.neighbors[i]==null) neighbors[i] = null;
				else neighbors[i] = (OrderedIntArr)in.neighbors[i].clone();
			}
			neighborsTSS = (double[])in.neighborsTSS.clone();
			scores = (IntHeapMin)in.scores.clone();
			scores.randomize();
			tempBool = (boolean[])in.tempBool.clone();
		}

		/**Randomizes heap during creation.*/
		public Object clone() { return new UndirectedGraphTSS(this);}

		public double[] best3scores() {scores.fillBest3(); return scores.best3scores;}
		public int[] best3elements() {scores.fillBest3(); return scores.best3elements;}
		public double[] best3TSS() {scores.fillBest3(); return scores.best3scores;}
		public double bestScore() {scores.fillBest3(); return scores.best3scores[0];}
		public int bestElement() {scores.fillBest3(); return scores.best3elements[0];}


		public void beginWatching(IntArr watch) {
			for(int i=0; i<watch.size(); i++) {
				int node = watch.get(i);
				if(neighbors[node]!=null) scores.insert(node, neighborsTSS[node], 0);
			}
		}
		public int numWatching() {return scores.size();}


		private static void fillNeighbors(OrderedIntArr neiList, boolean temp[]) {
			for(int i=neiList.size()-1; i>=0; i--) {
				temp[neiList.get(i)] = true;
			}
		}

		public int numNei(int var) {return neighbors[var].size();}
		public double neiTSS(int var) {return neighborsTSS[var];}
		public int varSize(int var) {return varSizes[var];}

		final private double bruteForceScore(int node) {
			double ret = varSizes[node];
			for(int j=0; j<neighbors[node].size(); j++) { //multiple sizes of its neighbors
				int nei = neighbors[node].get(j);
				ret *= varSizes[nei];
			}
			return ret;
		}

		private void removeVariableAndEdges(int rem) {
			if(DEBUG_UNDGR) {System.out.println("UndGr.removeVariableAndEdges(" + rem + ")");}

			for(int neiIndx=0; neiIndx<neighbors[rem].size(); neiIndx++) {
				int nei = neighbors[rem].get(neiIndx);
				neighbors[nei].removeFirstValue(rem);
				neighborsTSS[nei] /= varSizes[rem];
				scores.score1Changed(nei, neighborsTSS[nei]);
			}
			neighbors[rem].clear();
			neighbors[rem] = null;
			neighborsTSS[rem] = 0;
			scores.removeElement(rem);
		}


		public void conditionUpdateScore(int remove) {
			if(DEBUG_UNDGR) {System.out.println("UndGr.conditionUpdateScore(" + remove + "), score was " + scores.score1OfElement(remove));}
			if(DEBUG_EXTRA_TESTING) {
				double s1 = scores.score1OfElement(remove);
				double s2 = bruteForceScore(remove);
				if(( Math.abs(s1-s2) ) > .000001) throw new IllegalStateException("MinFill score error: " + s1 + " " + s2);
			}

			removeVariableAndEdges(remove);
		}

		public void eliminateUpdateScore(int remove) {
			if(DEBUG_UNDGR) {System.out.println("UndGr.eliminateUpdateScore(" + remove + "), score was " + scores.score1OfElement(remove));}
			if(DEBUG_EXTRA_TESTING) {
				double s1 = scores.score1OfElement(remove);
				double s2 = bruteForceScore(remove);
				if(( Math.abs(s1-s2) ) > .000001) throw new IllegalStateException("MinFill score error: " + s1 + " " + s2);
			}

			//Add edges and continue updating scores when additions occur
			{
				for(int indx1=0; indx1<neighbors[remove].size(); indx1++) {
					int var1 = neighbors[remove].get(indx1);

					if(neighbors[var1].size() < NEI_CUTOFF || ((neighbors[remove].size()-indx1) < NEI_CUTOFF)) { //this node doesn't have many neighbors or will not check many, easy to search
						for(int indx2=indx1+1; indx2<neighbors[remove].size(); indx2++) {
							int var2 = neighbors[remove].get(indx2);

							if(neighbors[var1].addIfNew(var2)) { //add edge var1-var2
								if(!neighbors[var2].addIfNew(var1)) throw new IllegalStateException();

								neighborsTSS[var1] *= varSizes[var2];
								scores.score1Changed(var1, neighborsTSS[var1]);
								neighborsTSS[var2] *= varSizes[var1];
								scores.score1Changed(var2, neighborsTSS[var2]);
							}
						}
					}
					else { //this node has many neighbors, make neighbor array instead of individually searching
						Arrays.fill(tempBool, false);
						fillNeighbors(neighbors[var1], tempBool); //tempBool has neighbors of var1
						for(int indx2=indx1+1; indx2<neighbors[remove].size(); indx2++) {
							int var2 = neighbors[remove].get(indx2);

							if(!tempBool[var2]) { //add edge var1-var2
								if(!neighbors[var1].addIfNew(var2)) throw new IllegalStateException();
								if(!neighbors[var2].addIfNew(var1)) throw new IllegalStateException();

								neighborsTSS[var1] *= varSizes[var2];
								scores.score1Changed(var1, neighborsTSS[var1]);
								neighborsTSS[var2] *= varSizes[var1];
								scores.score1Changed(var2, neighborsTSS[var2]);
							}
						}
					}
				}
			}
			removeVariableAndEdges(remove);
		}//end eliminateUpdateScore

	}//end class UndirectedGraphTSS



	/**Implements a minimization heap of elements.  The elements are each identified by
	 *  an integer value (which the user picks when adding them).  The integer value can
	 *  be from 0..size-1, and any subset of elements can be in the heap, i.e. it doesn't
	 *  have to be from 0,1,2...
	 *
	 * <p>The heap will not allow for elements to be added multiple times, unless of course
	 *  they are removed first.  Therefore, there can never be more than "size" elements in
	 *  the heap at any one time, and it cannot grow any larger.
	 *
	 * <p>As well as removing the smallest, this heap allows a user to find the best 3
	 *  elements, as well as remove any element they wish from the heap.
	 */
	static class IntHeapMin implements Cloneable {
		static final private boolean DEBUG_HEAP = false;

		private int length = 0; //current number of elements

		final private int[] heapLocations;	//heapLocation[element]=index in heap of this element
		final private double[] scores;		//scores[index]=score of this heap location
		final private double[] scoresTieBreaker; //scores[index]=score to use for tie breaking
		final private int[] heap;			//heap[index]=element at this location of the heap

		//these are unordered and only filled after a call to fillBest3
		final public double best3scores[] = new double[3];
		final public double best3TieBreaker[] = new double[3];
		final public int best3elements[] = new int[3];

		//LEFT_CHI = CURR*2+1
		//RIGHT_CHI = CURRENT*2+2
		//PARENT = (CURRENT-1)/2  (truncated)


		private void printHeap() {
			System.out.println("length: " + length);
			System.out.println("heapLocations: ");  printArrWithIndex(heapLocations);
			System.out.println("scores: ");  printArrWithIndex(scores);
			System.out.println("scoresTieBreaker: ");  printArrWithIndex(scoresTieBreaker);
			System.out.println("heap: ");  printArrWithIndex(heap);
		}

		public IntHeapMin(int size) {
			scores  = new double[size];
			scoresTieBreaker = new double[size];
			heapLocations = new int[size];
			heap = new int[size];

			Arrays.fill(heapLocations, -1);
			if(DEBUG_HEAP) { System.out.println("Created IntHeapMin with size of " + size);}
		}

		IntHeapMin(IntHeapMin in) {
			length = in.length;

			heapLocations = (int[])in.heapLocations.clone();
			scores = (double[])in.scores.clone();
			scoresTieBreaker = (double[])in.scoresTieBreaker.clone();
			heap = (int[])in.heap.clone();

			System.arraycopy(in.best3scores,0,best3scores,0,best3scores.length);
			System.arraycopy(in.best3TieBreaker,0,best3TieBreaker,0,best3TieBreaker.length);
			System.arraycopy(in.best3elements,0,best3elements,0,best3elements.length);
			if(DEBUG_HEAP) { System.out.println("Copied an IntHeapMin with " + heapLocations.length + " elements, max size: "+ heapLocations.length + ".");}
		}

		public Object clone() { return new IntHeapMin(this);}

		final private int left(int ind) { return ind*2+1;}
		final private int right(int ind) { return ind*2+2;}
		final private int parent(int ind) { return (ind-1)/2;} //note the integer division

		final private boolean isBetter(int i1, int i2) {
			if(scores[i1] < scores[i2]) return true;
			else if(scores[i1] > scores[i2]) return false;
			//are equal
			else if(scoresTieBreaker[i1] < scoresTieBreaker[i2]) return true;
			else if(scoresTieBreaker[i1] > scoresTieBreaker[i2]) return false;
			else return false;
		}

		final public int size() { return length;}

		final double score1OfElement(int element) {
			if(element < 0 || element >= heapLocations.length) throw new IllegalArgumentException("Tried to get score of element " + element + " max = " + (heapLocations.length-1));
			int ind = heapLocations[element];
			if(ind>=0) return scores[ind];
			else return -1;
		}

		/**
		 *  Will shuffle the values in the heap and then reheapify it.
		 *  See Collections.shuffle for a description of the algorithm.
		 */
		final void randomize() {
			for(int i=length-1; i>0; i--) { //for last to second
				//pick another element from 0..i
				double rndm = Math.random(); //inclusive [0.0..1.0) exclusive
				int indx = (int)Math.floor(rndm * (i+1)); //number from 0..i (inclusive)

				//swap
				if(indx != i) swap(indx, i);
			}

			//start with the parent of the last node and begin heapifying
			for(int i=parent(length-1); i>=0; i--) {
				propagateDown(i);
			}
			if(DEBUG_EXTRA_TESTING) testHeapProperty();
		}

		final public void insert(int element, double score, double scoreTie) {
			if(DEBUG_HEAP) { System.out.println("Heap.insert(" + element + ", " + score + ", " + scoreTie + ")");}
			if(element < 0 || element >= heapLocations.length) throw new IllegalArgumentException("Tried to insert element " + element + " max = " + (heapLocations.length-1));
			int ind = heapLocations[element];
			if(ind >= 0) throw new IllegalArgumentException("This element is already in the heap");

			ind = length;
			heapLocations[element] = ind;
			heap[ind] = element;
			scores[ind] = score;
			scoresTieBreaker[ind] = scoreTie;
			length++;
			propagateUp(ind);
			if(DEBUG_EXTRA_TESTING) testHeapProperty();
		}

		final public int removeMin() {
			if(length==0) throw new NoSuchElementException("Heap is empty.");
			int element = heap[0]; //element at root
			if(DEBUG_HEAP) { System.out.println("Heap.removeMin(" + element + ", " + scores[0] + ", " + scoresTieBreaker[0] + ")");}

			heapLocations[element] = -1;
			length--;
			swap(0, length);
			propagateDown(0);
			if(DEBUG_EXTRA_TESTING) testHeapProperty();
			return element;
		}

		final public void removeElement(int element) {
			if(element < 0 || element >= heapLocations.length) throw new IllegalArgumentException("Tried to remove element " + element + " max = " + (heapLocations.length-1));
			int ind = heapLocations[element];
			if(ind < 0 || ind >= length) return;//throw new NoSuchElementException("Heap did not contain element " + element);
			if(DEBUG_HEAP) { System.out.println("Heap.removeElement(" + element + ", " + scores[ind] + ", " + scoresTieBreaker[ind] + ")");}

			length--;
			swap(ind, length);
			heapLocations[element] = -1;

			if(isBetter(length, ind)) propagateDown(ind); //if old was better, push this down the heap
			else propagateUp(ind); //if new is better, push this up the heap

			if(DEBUG_EXTRA_TESTING) testHeapProperty();
		}

		public final void scoresChanged(int element, double newScore, double newScoreTie) {
			if(element < 0 || element >= heapLocations.length) throw new IllegalArgumentException("Tried to change scores of element " + element + " max = " + (heapLocations.length-1));
			int ind = heapLocations[element];
			if(ind < 0 || ind >= length) return;//throw new NoSuchElementException("Heap did not contain element " + element);
			if(DEBUG_HEAP) { System.out.println("Heap.scoresChanged(" + element + ", " + scores[ind] + "->" + newScore + ", " + scoresTieBreaker[ind] + "->" + newScoreTie + ")");}

			if(newScore < scores[ind]) { //score decrease (up the heap)
				scores[ind] = newScore;
				scoresTieBreaker[ind] = newScoreTie;
				propagateUp(ind);
			}
			else if(newScore > scores[ind]) { //score increase (down the heap)
				scores[ind] = newScore;
				scoresTieBreaker[ind] = newScoreTie;
				propagateDown(ind);
			}
			//they are ==
			else if(newScoreTie < scoresTieBreaker[ind]) { //scores were equal, but tie breaker went down (up the heap)
				scores[ind] = newScore;
				scoresTieBreaker[ind] = newScoreTie;
				propagateUp(ind);
			}
			else if(newScoreTie > scoresTieBreaker[ind]) { //scores were equal, but tie breaker went up (down the heap)
				scores[ind] = newScore;
				scoresTieBreaker[ind] = newScoreTie;
				propagateDown(ind);
			}
			if(DEBUG_EXTRA_TESTING) testHeapProperty();
		}

		public final void score1Changed_delta(int element, double delta) {
			if(element < 0 || element >= heapLocations.length) throw new IllegalArgumentException("Tried to change scores of element " + element + " max = " + (heapLocations.length-1));
			int ind = heapLocations[element];
			if(ind < 0 || ind >= length) return;//throw new NoSuchElementException("Heap did not contain element " + element);
			double newScore = (scores[ind]+delta);
			if(DEBUG_HEAP) { System.out.println("Heap.score1Changed_delta(" + element + ", " + scores[ind] + "->" + newScore + ", " + scoresTieBreaker[ind] + ")");}


			if(newScore < scores[ind]) { //score decrease (up the heap)
				scores[ind] = newScore;
				propagateUp(ind);
			}
			else if(newScore > scores[ind]) { //score increase (down the heap)
				scores[ind] = newScore;
				propagateDown(ind);
			}
			if(DEBUG_EXTRA_TESTING) testHeapProperty();
		}

		public final void score1Changed(int element, double newScore) {
			if(element < 0 || element >= heapLocations.length) throw new IllegalArgumentException("Tried to change scores of element " + element + " max = " + (heapLocations.length-1));
			int ind = heapLocations[element];
			if(ind < 0 || ind >= length) return;//throw new NoSuchElementException("Heap did not contain element " + element);
			if(DEBUG_HEAP) { System.out.println("Heap.score1Changed(" + element + ", " + scores[ind] + "->" + newScore + ", " + scoresTieBreaker[ind] + ")");}

			if(newScore < scores[ind]) { //score decrease (up the heap)
				scores[ind] = newScore;
				propagateUp(ind);
			}
			else if(newScore > scores[ind]) { //score increase (down the heap)
				scores[ind] = newScore;
				propagateDown(ind);
			}
			if(DEBUG_EXTRA_TESTING) testHeapProperty();
		}

		public final void score2Changed(int element, double newScoreTie) {
			if(element < 0 || element >= heapLocations.length) throw new IllegalArgumentException("Tried to change scores of element " + element + " max = " + (heapLocations.length-1));
			int ind = heapLocations[element];
			if(ind < 0 || ind >= length) return;//throw new NoSuchElementException("Heap did not contain element " + element);
			if(DEBUG_HEAP) { System.out.println("Heap.score2Changed(" + element + ", " + scores[ind] + ", " + scoresTieBreaker[ind] + "->" + newScoreTie + ")");}

			//they are ==
			if(newScoreTie < scoresTieBreaker[ind]) { //scores were equal, but tie breaker went down (up the heap)
				scoresTieBreaker[ind] = newScoreTie;
				propagateUp(ind);
			}
			else if(newScoreTie > scoresTieBreaker[ind]) { //scores were equal, but tie breaker went up (down the heap)
				scoresTieBreaker[ind] = newScoreTie;
				propagateDown(ind);
			}
			if(DEBUG_EXTRA_TESTING) testHeapProperty();
		}

		//will put the best 3 elements in the public arrays (unordered)
		final public void fillBest3() {
			Arrays.fill(best3scores, -1);
			Arrays.fill(best3TieBreaker, -1);
			Arrays.fill(best3elements, -1);

			//put the top 3 heap elements in the arrays
			if(length<1) return;

			best3scores[0] = scores[0];
			best3TieBreaker[0] = scoresTieBreaker[0];
			best3elements[0] = heap[0];

			if(length<2) return;

			best3scores[1] = scores[1];
			best3TieBreaker[1] = scoresTieBreaker[1];
			best3elements[1] = heap[1];

			if(length<3) return;

			best3scores[2] = scores[2];
			best3TieBreaker[2] = scoresTieBreaker[2];
			best3elements[2] = heap[2];

			//heap indecies 3&4 could be better than 2
			int indx=2;

			if(length<4) return;
			if(isBetter(3,indx)) {
				indx=3;
				best3scores[2] = scores[indx];
				best3TieBreaker[2] = scoresTieBreaker[indx];
				best3elements[2] = heap[indx];
			}

			if(length<5) return;
			if(isBetter(4,indx)) {
				indx=4;
				best3scores[2] = scores[indx];
				best3TieBreaker[2] = scoresTieBreaker[indx];
				best3elements[2] = heap[indx];
			}

			//heap indecies 5&6 could be better than 1
			indx=1;

			if(length<6) return;
			if(isBetter(5,indx)) {
				indx=5;
				best3scores[1] = scores[indx];
				best3TieBreaker[1] = scoresTieBreaker[indx];
				best3elements[1] = heap[indx];
			}

			if(length<7) return;
			if(isBetter(6,indx)) {
				indx=6;
				best3scores[1] = scores[indx];
				best3TieBreaker[1] = scoresTieBreaker[indx];
				best3elements[1] = heap[indx];
			}
		}



		final private void propagateDown(int ind) {
			boolean done=false;
			while(!done) {
				int l = ind*2+1;
				int r = ind*2+2;
				int smallest = ind;

				if(l >= length) { break;} //hit bottom of heap
				if(isBetter(l,ind)) smallest = l;

				if(r < length && isBetter(r,smallest)) smallest = r;

				if(smallest == ind) {break;} //found right spot

				swap(ind, smallest);
				ind = smallest;
			}
		}

		final private void propagateUp(int ind) {
			int par = (ind-1)/2;

			while(ind>0 && isBetter(ind,par)) {
				swap(ind, par);
				ind = par;
				par = (ind-1)/2;
			}
		}

		final private void swap(int i, int j) {
			//swap heap objects
			int elemI = heap[i];
			int elemJ = heap[j];
			heap[i] = elemJ;
			heap[j] = elemI;

			//swap scores
			double tmp = scores[i];
			scores[i] = scores[j];
			scores[j] = tmp;

			tmp = scoresTieBreaker[i];
			scoresTieBreaker[i] = scoresTieBreaker[j];
			scoresTieBreaker[j] = tmp;

			//swap element pointers
			int tmpLoc = heapLocations[elemI];
			heapLocations[elemI] = heapLocations[elemJ];
			heapLocations[elemJ] = tmpLoc;
		}

		/*Only used for debugging.*/
		final public void testHeapProperty() {
			for(int i=1; i<length; i++) { //start at 1, since it has a parent
				int par = (i-1)/2;
				if(isBetter(i,par)) { printHeap(); throw new IllegalStateException("Error with heap value at index: " + i);}
			}
		}
	}//end class IntHeapMin


	static class IntArr implements Cloneable {
		private final static int DEFAULT_SIZE = 10;

		int values[];
		int length;

		public IntArr() {
			this(DEFAULT_SIZE);
		}

		public IntArr(int initialSize) {
			values = new int[initialSize];
			length = 0;
		}

		IntArr(IntArr in) {
			values = (int[])in.values.clone();
			length = in.length;
		}

		public Object clone() { return new IntArr(this);}

		public final int get(int ind) {
			if(ind<0 || ind>=length) throw new IndexOutOfBoundsException("index: " + ind + ", max " + length);
			return values[ind];
		}

		public final void set(int ind, int val) {
			if(ind<0 || ind>=length) throw new IndexOutOfBoundsException("index: " + ind + ", max " + length);
			values[ind]=val;
		}

		public final void add(int val) {
			if(values.length==length) lengthenAllocation(length+1);
			values[length]=val;
			length++;
		}

		public final void insert(int ind, int val) {
			if(ind<0 || ind>length) throw new IndexOutOfBoundsException("index: " + ind + ", max " + length);
			if(values.length==length) lengthenAllocation(length+1);
			System.arraycopy(values, ind, values, ind+1, length-ind);
			values[ind]=val;
			length++;
		}

		public final boolean containsValue(int val) {
			for(int i=0; i<length; i++) {
				if(values[i] == val) return true;
			}
			return false;
		}

		/**Removes this entry, however it maintains the ordering of the rest.*/
		public final void removeEntryAt(int ind) {
			if(ind>=length) throw new IndexOutOfBoundsException(ind + " >= " + length);
			System.arraycopy(values, ind+1, values, ind, length-ind-1);
			length--;
		}

		/**Removes this entry, however it does NOT maintain the ordering of the rest.*/
		public final void removeFirstValue(int value) {
			for(int i=0; i<length; i++) {
				if(values[i]==value) {
					swapWithLastAndRemove(i);
					return;
				}
			}
			throw new NoSuchElementException("List did not contain " + value);
		}

		public final int removeLast() {
			if(length==0) throw new NoSuchElementException("List is empty");
			length--;
			return values[length];
		}

		public final void swapWithLastAndRemove(int ind) {
			int tmp = removeLast();
			values[ind]=tmp; //works even if this was the only element
		}

		public final void clear() { length=0;}
		public final int size() {return length;}
		public final boolean isEmpty() {return length==0;}

		public final int last() {
			if(length==0) throw new NoSuchElementException("List is empty");
			return values[length-1];
		}

		final private void lengthenAllocation(int size) {
			int allocation=3*length/2;
			if(allocation<size) allocation=size;
			int newvals[] = new int[allocation];
			System.arraycopy(values, 0, newvals, 0, length);
			values=newvals;
		}

		public String toString() {
			StringBuffer buf=new StringBuffer(5*size());
			buf.append('[');
			for(int i=0;i<length;i++){
				buf.append(' ');
				buf.append(get(i));
				if(i+1<length){
					buf.append(',');
				}
			}
			buf.append(']');
			return buf.toString();
		}
	}

	static class OrderedIntArr implements Cloneable {
		IntArr data;
		final boolean allowDuplicates = false; //TODO

		public OrderedIntArr() {
			data = new IntArr();
		}

		public OrderedIntArr(int initialSize) {
			data = new IntArr(initialSize);
		}

		OrderedIntArr(OrderedIntArr in) { data = (IntArr)in.data.clone();}

		public Object clone() {
			return new OrderedIntArr(this);
		}

		public final int get(int ind) { return data.get(ind);}

		public final void add(int value) {
			if(data.size()==0) { data.add(value);}
			else if(value > data.last()) { data.add(value);} //if it goes at the end, add it quickly
			else {
				int ind=indexOf(value);
				if(ind<0) { //add it new
					ind=-ind-1;
					data.insert(ind, value);
				}
				else { //it is a duplicate, add it anyway
					if(!allowDuplicates) throw new IllegalStateException("OrderedIntArr already contains " + value);
					data.insert(ind, value);
				}
			}
			if(DEBUG_EXTRA_TESTING) testOrderedProperty();
		}

		/**Add the value to the list if it is not there and return true.  If the value is already in
		 *   the list, return false.
		 */
		public final boolean addIfNew(int value) {
			if(data.size()==0) { data.add(value); return true;}
			else if(value > data.last()) { data.add(value); return true;} //if it goes at the end, add it quickly
			else {
				int ind=indexOf(value);
				if(ind<0) { //add it new
					ind=-ind-1;
					data.insert(ind, value);
					if(DEBUG_EXTRA_TESTING) testOrderedProperty();
					return true;
				}
				else { //it is a duplicate, add it anyway
					return false;
				}
			}
		}

		public final boolean containsValue(int value) {
			return indexOf(value)>=0;
		}

		public final void removeEntryAt(int ind) {
			data.removeEntryAt(ind);
			if(DEBUG_EXTRA_TESTING) testOrderedProperty();
		}

		//Doesn't actually matter if it is first if it is duplicate, eliminate any
		public final void removeFirstValue(int value) {
			int ind=indexOf(value);
			if(ind<0) throw new NoSuchElementException("List did not contain " + value);
			data.removeEntryAt(ind);
			if(DEBUG_EXTRA_TESTING) testOrderedProperty();
		}

		public final int removeLargest() { return data.removeLast();}

		public final void clear() { data.clear();}
		public final int size() { return data.size();}
		public final int largest() { return data.last();}
		public final int smallest() { return data.get(0);}

		public final int indexOf(int value) {
			return binarySearch(value, 0, size());
		}

		private final int binarySearch(int value, int low, int high) {
			while(true) {
				if(low==high) { return -low-1;}
				int ind=(low+high)/2;
				int ival=get(ind);

				if(value<ival) {high=ind;}
				else if(value>ival) {low=ind+1;}
				else return ind;
			}
		}

		public String toString() { return data.toString();}

		/*Only used for debugging.*/
		final public void testOrderedProperty() {
			if(size()==0) return;

			int prev=get(0);

			for(int i=1; i<size(); i++) {
				int val=get(i);
				if(val < prev) throw new IllegalStateException("OrderedIntArr was out of order: " + prev + " - " + val);
				if(val == prev && !allowDuplicates) throw new IllegalStateException("OrderedIntArr contained multiple copies of " + val);
				prev = val;
			}
		}
	}//end class OrderedIntArr


	static class CollectionOfTables implements Cloneable {

		//variables
		final RC rc;
		final int tmp_varCounts[]; //used for scoring functions, (not maintained)
		final int varCounts[];
		final int varSizes[];		//number of states of a given node (once created, never changes)
		final VarTable varLists[][]; //[variable][table] (can have nulls in the middle of the lists)
		int nextTblNum;
		final ArrayList emptyTablesToConnect = new ArrayList();

		public CollectionOfTables(MappedList fVars) {
			int sizebn = fVars.size();

			rc = new RC(sizebn);

			varSizes = new int[sizebn];
			{
				for(int i=0; i<varSizes.length; i++) {
					varSizes[i] = ((FiniteVariable)fVars.get(i)).size();
				}
			}

			varCounts = new int[sizebn];
			Arrays.fill(varCounts, 0);
			tmp_varCounts = new int[sizebn];
			varLists = new VarTable[sizebn][];

			//count how many tables each var is in
			for(int i=0; i<sizebn; i++) {
				FiniteVariable varTbl = (FiniteVariable)fVars.get(i);
				TableIndex tbi = varTbl.getCPTShell().index();
				for(int v=0; v<tbi.getNumVariables(); v++) {
					FiniteVariable tmpVar = tbi.variable(v);
					varCounts[fVars.indexOf(tmpVar)]++;
				}
			}

			//allocate varLists
			for(int i=0; i<sizebn; i++) {
				if(varCounts[i]==1) {//1 is not a valid number of tables to be in, so remove it now
					varCounts[i] = 0;
				}
				varLists[i] = new VarTable[varCounts[i]];
			}

			//Create tables and fill varLists
			for(int i=0; i<sizebn; i++) {
				FiniteVariable varTbl = (FiniteVariable)fVars.get(i);
				TableIndex tbi = varTbl.getCPTShell().index();

				OrderedIntArr tblVars = new OrderedIntArr(tbi.getNumVariables());
				VarTable vt = new VarTable(1, tblVars, i);

				rc.add(new RC.RCLine('L', vt.rcID, false, -1, -1, varTbl, "cpt"));

				boolean emptyTable = true;

				for(int v=0; v<tbi.getNumVariables(); v++) {
					FiniteVariable tmpVar = tbi.variable(v);
					int varID = fVars.indexOf(tmpVar);

					if(varCounts[varID]==0) continue; //if only appears once, remove it

					int vtindx = 0;
					while(varLists[varID][vtindx]!=null) vtindx++;
					varLists[varID][vtindx] = vt;
					tblVars.add(varID);
					emptyTable = false;
				}
				if(emptyTable) {
					rc.rcCurrentCost+=2; //since this table is empty, connect it up to the root
					emptyTablesToConnect.add(new Integer(vt.rcID));
				}
			}

			nextTblNum = sizebn;
		}//end constructor

		CollectionOfTables(CollectionOfTables in) {
			rc = (RC)in.rc.clone();
			tmp_varCounts = (int[])in.tmp_varCounts.clone();
			varCounts = (int[])in.varCounts.clone();
			varSizes = (int[])in.varSizes.clone();
			varLists = new VarTable[varSizes.length][];
			for(int i=0; i<varLists.length; i++) { varLists[i] = (VarTable[])in.varLists[i].clone();}
			nextTblNum = in.nextTblNum;
			emptyTablesToConnect.addAll(in.emptyTablesToConnect);
		}

		public Object clone() { return new CollectionOfTables(this);}



		/** Return an array of OrderedIntArr's which are a list of the neighbors of
		 *   each variable (two variables are neighbors if they are in any table together).
		 */
		public OrderedIntArr[] fillNeighbors() {
			int size = varCounts.length;
			OrderedIntArr[] ret = new OrderedIntArr[size];
			boolean nei[] = new boolean[size];

			for(int varID=0; varID<size; varID++) { //for each variable
				Arrays.fill(nei, false);
				int count = 0;

				for(int vtindx=0; vtindx<varLists[varID].length; vtindx++) { //for each table it is in
					VarTable vt = varLists[varID][vtindx];
					if(vt==null) continue;

					for(int v=0; v<vt.vars.size(); v++) { //for each other variable in table
						int var = vt.vars.get(v);

						if(var!=varID && !nei[var]) {
							nei[var]=true;
							count++;
						}
					}
				}

				ret[varID] = new OrderedIntArr(3*count/2); //make it bigger than count for expansion, as connect other vars
				for(int i=0; i<nei.length; i++) {
					if(nei[i]) ret[varID].add(i);//add them in order, which wil be faster than if not in order
				}
			}
			return ret;
		}



		static class ScoreResults {
			double maxContext;
			double sumContext;
			double rootContext;
			int numTables;
			double sumCounts;
			boolean useImmediately;
			boolean dontUse;
			double pruningBound;

			public ScoreResults() {
				clear();
			}

			public void clear() {
				maxContext=0;
				sumContext=0;
				rootContext=0;
				numTables=0;
				sumCounts=0;
				useImmediately=false;
				dontUse=false;
				pruningBound=0;
			}

			public boolean invalid() {
				if(maxContext<0) { return true;}
				else if(sumContext<0) { return true;}
				else if(rootContext<0) { return true;}
				else if(numTables<0) { return true;}
				else if(sumCounts<0) { return true;}
				return false;
			}

			public String toString() {
				return "maxContext: " + maxContext + " sumContext: " + sumContext + " rootContext: " + rootContext +
					" numTables: " + numTables + " sumCounts: " + sumCounts + " useImmediately: " + useImmediately +
					" dontUse: " + dontUse;
			}
		}
		private ScoreResults scoreResults = new ScoreResults();


		public ScoreResults calcScore(int varID) { return mergeAllTables_priv(varID, false);}
		public double mergeAllTables(int varID) {
			ScoreResults scr = mergeAllTables_priv(varID, true);
			if(scr.dontUse) return (Double.MAX_VALUE); //allow it if that is the first
			else return scr.pruningBound;
		}

		private ScoreResults mergeAllTables_priv(int varID, boolean makePermanent) {

			scoreResults.clear();

			int countArr[] = (makePermanent ? varCounts : tmp_varCounts);
			if(!makePermanent) System.arraycopy(varCounts,0,tmp_varCounts,0,varCounts.length);

			VarTable varIDList[] = (makePermanent ? varLists[varID] : new VarTable[varLists[varID].length]);
			if(!makePermanent) System.arraycopy(varLists[varID],0,varIDList,0,varIDList.length);

			if(countArr[varID]<2) {scoreResults.useImmediately=true; scoreResults.pruningBound=rc.rcCurrentCost; return scoreResults;}  //special case, remove this variable, as it doesn't cost anything

			scoreResults.numTables = countArr[varID];

			int vtIndx=0;
			while(countArr[varID]>=2) {
				int ti1; VarTable t1;
				int ti2; VarTable t2;

				{//find 2 tables
					while(varIDList[vtIndx]==null) {
						vtIndx++;
						if(vtIndx==varIDList.length) vtIndx=0;
					}
					ti1 = vtIndx;
					t1 = varIDList[ti1];

					vtIndx++;
					if(vtIndx==varIDList.length) vtIndx=0;

					while(varIDList[vtIndx]==null) {
						vtIndx++;
						if(vtIndx==varIDList.length) vtIndx=0;
					}

					ti2 = vtIndx;
					t2 = varIDList[ti2];

					vtIndx++;
					if(vtIndx==varIDList.length) vtIndx=0;
				}

				//calculate cutset & context
				double cutset=1;
				double context=1;
				OrderedIntArr newContext = new OrderedIntArr(t1.vars.size()+t2.vars.size());
				{
					int next1Indx = 0;
					int next2Indx = 0;

					for(;;) {
						if(next1Indx>=t1.vars.size()) next1Indx=-1;
						if(next2Indx>=t2.vars.size()) next2Indx=-1;
						if(next1Indx<0 && next2Indx<0) break;

						int next1Var = (next1Indx<0 ? Integer.MAX_VALUE : t1.vars.get(next1Indx));
						int next2Var = (next2Indx<0 ? Integer.MAX_VALUE : t2.vars.get(next2Indx));

						if(next1Var==next2Var) { //in both
							countArr[next1Var]--;
							if(countArr[next1Var]==1) { //in cutset
								countArr[next1Var]=0;
								cutset*=varSizes[next1Var];
								if(makePermanent) changeTbls(varLists[next1Var], t1, t2, null);
							}
							else {
								newContext.add(next1Var);
								context*=varSizes[next1Var];
							}
							next1Indx++;
							next2Indx++;
						}
						else if(next1Var<next2Var) { //do one from t1
							newContext.add(next1Var);
							context*=varSizes[next1Var];
							next1Indx++;
						}
						else if(next1Var>next2Var) { //do one from t2
							newContext.add(next2Var);
							context*=varSizes[next2Var];
							next2Indx++;
						}
					}//end for vars in t1 & t2

					//update scoreResults
					if(context > scoreResults.maxContext) scoreResults.maxContext = context;
					scoreResults.sumContext += context;
					scoreResults.rootContext = context;
					if(cutset > Integer.MAX_VALUE) scoreResults.dontUse = true;
					if(context > Long.MAX_VALUE) scoreResults.dontUse = true;

					//determine if caching
					boolean cacheHere = (context < largestAcceptableRCCache);

					//setup new varTable
					double cpc;
					if(cacheHere) {
						cpc = 1.0; //this makes this inclusive of calls to this node
						scoreResults.sumCounts += ((cutset*context)*(t1.cpc + t2.cpc));
						if(makePermanent) {
							rc.rcCurrentCost += ((cutset*context)*(t1.cpc+t2.cpc));
						}
					}
					else {
						cpc = cutset*(t1.cpc + t2.cpc)+1; //this is calls per call, inclusive of call to it
						if(countArr[varID]<2) {//this is root of subtree, penalize for no caching
							//assume parent has same cutset & context & is cached
							scoreResults.sumCounts += (cutset*context*cpc);
						}
					}

					VarTable vt = new VarTable(cpc, newContext, nextTblNum++);
					if(makePermanent) {
						rc.add(new RC.RCLine('I', vt.rcID, cacheHere, t1.rcID, t2.rcID, null, null));
						for(int i=0; i<newContext.size(); i++) {
							int iv=newContext.get(i);
							changeTbls(varLists[iv], t1, t2, vt);
						}

						if(newContext.size()==0) {//table became empty
							rc.rcCurrentCost += (1+vt.cpc); //this table is done, add 1 to connect up this to another part of dtree & make 1 call to it
							emptyTablesToConnect.add(new Integer(vt.rcID));
						}
					}
					else {
						changeTbls(varIDList, t1, t2, vt);
					}

					//set a return value in case done
					//  if this is the root, just return currentCount
					//  if there is more to do, can also include the cpc for this table also
					scoreResults.pruningBound = rc.rcCurrentCost + (newContext.size()==0 ? 0 : vt.cpc);
				}
			}//while have >= 2 tables


			//make some updates to ease scoring later
			if(scoreResults.maxContext==0) {scoreResults.maxContext=1;}
			if(scoreResults.sumContext==0) {scoreResults.sumContext=1;}
			if(scoreResults.rootContext==0) {scoreResults.rootContext=1;}
			if(scoreResults.sumCounts==0) {throw new IllegalStateException(scoreResults.toString());}
			if(scoreResults.invalid()) { System.err.println(scoreResults.toString()); throw new IllegalStateException(scoreResults.toString());}

			return scoreResults;
		}

		//put new1 into the location of either old1 or old2 (any of these 3 parameters can be null)
		static private void changeTbls(VarTable varList[], VarTable old1, VarTable old2, VarTable new1) {
			for(int i=0; i<varList.length; i++) {
				if(varList[i]==null) continue;
				else if(varList[i]==old1) {
					varList[i]=new1;
					new1=null;
					old1=null;
					if(old2==null) break;
				}
				else if(varList[i]==old2) {
					varList[i]=new1;
					new1=null;
					old2=null;
					if(old1==null) break;
				}
			}
			if(new1!=null) throw new IllegalStateException("Didn't find a table: " + old1 + " " + old2 + " " + new1);
		}


		static class VarTable {
			final OrderedIntArr vars;
			final double cpc;
			final int rcID;

			public VarTable(double cpc, OrderedIntArr vars, int rcID) {
				this.cpc = cpc;
				this.vars = vars;
				this.rcID = rcID;
			}

			public String toString() {
				return "(" + rcID + ", " + cpc + " " + vars + ")";
			}
		}//end class VarTable

		public int varInMostTables(IntArr exclude) {
			int retIndx = -1;
			int numTbls = 0;
			for(int i=0; i<varCounts.length; i++) {
				if(varCounts[i] > numTbls && (!exclude.containsValue(i))) {
					retIndx = i;
					numTbls = varCounts[i];
				}
			}
			return retIndx;
		}

		public int varInFewestTables(IntArr exclude) {
			int retIndx = -1;
			int numTbls = Integer.MAX_VALUE;
			for(int i=0; i<varCounts.length; i++) {
				if(varCounts[i] > 0 && varCounts[i] < numTbls && (!exclude.containsValue(i))) {
					retIndx = i;
					numTbls = varCounts[i];
				}
			}
			return retIndx;
		}

		protected void testDone() {
			for(int i=0; i<varLists.length; i++) {
				for(int j=0; j<varLists[i].length; j++) {
					if(varLists[i][j] != null) throw new IllegalStateException();
				}
				if(varCounts[i]!=0) throw new IllegalStateException();
			}
			if(!emptyTablesToConnect.isEmpty()) throw new IllegalStateException();
		}

		public RC finishCreatingRC() {
			if(rc.finished) return rc;

			if(emptyTablesToConnect.isEmpty()) { //at the very least, the root becomes empty and is added
				throw new IllegalStateException("Either finishCreatingRC has already been called, or else an error happened.");
			}
			else if(emptyTablesToConnect.size()==1) { //last one added was actually root
				//need to make last internal node the root
				RC.RCLine last = (RC.RCLine)rc.rcObj.remove(rc.rcObj.size()-1);
				rc.add(new RC.RCLine('R', last.id, last.cache, last.left, last.right, last.leafVar, last.leafParam));
				emptyTablesToConnect.clear();
			}
			else {
				int currentRoot = nextTblNum-1;
				emptyTablesToConnect.remove(new Integer(currentRoot)); //last root in tree

				while(emptyTablesToConnect.size()>1) {
					Integer i_rem = (Integer)emptyTablesToConnect.remove(emptyTablesToConnect.size()-1);
					rc.add(new RC.RCLine('I', nextTblNum, false, currentRoot, i_rem.intValue(), null, null));
					currentRoot = nextTblNum;
					nextTblNum++;
				}
				Integer i_rem = (Integer)emptyTablesToConnect.remove(emptyTablesToConnect.size()-1);
				rc.add(new RC.RCLine('R', nextTblNum, false, currentRoot, i_rem.intValue(), null, null));
				currentRoot = nextTblNum;
				nextTblNum++;
			}
			rc.rcCurrentCost-=1; //two empty tables are added plus 1 each, but those 2 are incorrect, however there is 1 call to the root
			rc.finished = true;
			if(DEBUG_EXTRA_TESTING) testDone();
			return rc;
		}

	}//end class CollectionOfTables

	static public class RC implements Cloneable {
		protected ArrayList rcObj;
		double rcCurrentCost = 0;
		boolean finished = false; //once the root it set, don't make any more changes

		public RC(int sizeBN) {
			rcObj = new ArrayList((2*sizeBN) + 15);
		}

		RC(RC in) {
			rcObj = (ArrayList)in.rcObj.clone();
			rcCurrentCost = in.rcCurrentCost;
			finished = in.finished;
		}

		public Object clone() { return new RC(this);}

		public void add(RCLine line) {
			if(finished) throw new IllegalStateException();
			rcObj.add(line);
		}

		public void writeFile(Writer wtr) throws IOException {
			wtr.write("# " + new Date() + "\n");
			wtr.write("# RC Search Score: " + rcCurrentCost + "\n");
			for(int i=rcObj.size()-1; i>=0; i--) {
				wtr.write((RCLine)rcObj.get(i) + "\n");
			}
		}

		public double rcScore() {return rcCurrentCost;}

		static public RC keepBest(RC rc1, RC rc2) {
			if(rc1==null) {return rc2;}
			else if(rc2==null) {return rc1;}
			else if(rc1.rcCurrentCost <= rc2.rcCurrentCost) {return rc1;}
			else {return rc2;}
		}


		static class RCLine {
			final char type; //R,I,L  (root is a type of internal)
			final int id;
			final boolean cache; //only for internal
			final int left; //only for internal
			final int right; //only for internal
			final FiniteVariable leafVar; //only for leafs
			final String leafParam; //only for leafs (cpt or evid)

			public RCLine(char t, int i, boolean c, int l, int r, FiniteVariable v, String lp) {
				type = t;
				id = i;
				cache = c;
				left = l;
				right = r;
				leafVar = v;
				leafParam = lp;
			}

			public String toString() {
				if(type=='R') return "ROOT " + id + (cache ? " cachetrue " : " cachefalse ") + left + " " + right;
				else if(type=='I')  return "I " + id + (cache ? " cachetrue " : " cachefalse ") + left + " " + right;
				else if(type=='L')  return "L " + id + " " + leafVar.getID() + " " + leafParam;
				else throw new IllegalStateException();
			}
		}

	}

	static public class EO implements Cloneable {
		protected ArrayList eo;

		int width=-1;
		double cutTSS = 1.0;
		int numCond = 0;

		double currentCost = 0;

		public EO(int sizeBN) {
			eo = new ArrayList(sizeBN);
		}

		EO(EO in) {
			eo = (ArrayList)in.eo.clone();
			width = in.width;
			cutTSS = in.cutTSS;
			numCond = in.numCond;
			currentCost = in.currentCost;
		}

		public Object clone() { return new EO(this);}
		public List eo() { return Collections.unmodifiableList(eo);}
		public int width() { return width;}
		public int numCond() { return numCond;}

		public void condVar(FiniteVariable fv, int w, double tss, int varSize) {
			if(w<0) throw new IllegalArgumentException();
			if(tss<0) throw new IllegalArgumentException();
			if(varSize<=0) throw new IllegalArgumentException();

			eo.add(fv);
			numCond++;
			if(w>width) width=w;
			cutTSS *= varSize;
		}

		public void elimVar(FiniteVariable fv, int w, double tss) {
			if(w<0) throw new IllegalArgumentException();
			if(tss<0) throw new IllegalArgumentException();

			eo.add(fv);
			if(w>width) width=w;
			currentCost+=(cutTSS * tss);
		}

		public double eoScore() {return currentCost;}

		static public EO keepBest(EO eo1, EO eo2) {
			if(eo1==null) return eo2;
			else if(eo2==null) return eo1;
			else if(eo1.currentCost <= eo2.currentCost) return eo1;
			else return eo2;
		}

		public String toString() { return eo.toString();}
	}


	static private double totalStateSpace(FiniteVariable v, Collection nei) {
		double ret=v.size();
		for(Iterator itr = nei.iterator(); itr.hasNext();) {
			ret *= ((FiniteVariable)itr.next()).size();
		}
		return ret;
	}

	static private void preprocess(Graph gr, ArrayList eo, Set varsToPreprocess) {
		double low = 0.0;
		double loww = 0.0;

		if(DEBUG_VERBOSE_PREPROCESS) { System.out.println("Preprocess began with " + gr.size() + " nodes");}


		for(Iterator itrp = varsToPreprocess.iterator(); itrp.hasNext();) {
			FiniteVariable v = (FiniteVariable)itrp.next();
			Set nei = gr.neighbors(v);
			int tmpW = nei.size();
			double tmpTS = totalStateSpace(v, nei);

			if(tmpTS > low) {low = tmpTS;}
			if(tmpW > loww) loww = tmpW;

			//connect up neighbors
			for(Iterator itr1 = nei.iterator(); itr1.hasNext();) {
				Object neigh1 = itr1.next();

				for(Iterator itr2 = nei.iterator(); itr2.hasNext();) {
					Object neigh2 = itr2.next();

					if(neigh1!=neigh2 && !gr.containsEdge(neigh1, neigh2)) {
						gr.addEdge(neigh1, neigh2);
					}
				}
			}
			gr.remove(v);
			eo.add(v);
			if(DEBUG_VERBOSE_PREPROCESS) { System.out.println("Was told to preprocess: " + v);}
		}

		boolean done1=false;
		while(!done1) {
			done1=true;

			HashSet nodesToCheck = new HashSet(gr);

			//remove simplicial vertices (minFill score = 0)
			while(!nodesToCheck.isEmpty()) {
				Object vertex = nodesToCheck.iterator().next();
				nodesToCheck.remove(vertex);

				boolean simplicial=true;
				Set nei = gr.neighbors(vertex);

				for(Iterator itr1=nei.iterator(); simplicial && itr1.hasNext();) { //TODO could be simpler with using arrays
					Object n1 = itr1.next();
					for(Iterator itr2=nei.iterator(); simplicial && itr2.hasNext();) {
						Object n2 = itr2.next();
						if(n1!=n2 && !gr.containsEdge(n1, n2)) {
							simplicial=false; //not simplicial
						}
					}
				}

				if(simplicial) {
					int tmpW = nei.size();
					double tmpTS = totalStateSpace((FiniteVariable)vertex, nei);
					if(tmpTS > low) { low = tmpTS;}
					if(tmpW > loww) loww = tmpW;

					nodesToCheck.addAll(nei); //these could now possibly be simplicial

					gr.remove(vertex);
					//don't need to add any edges
					eo.add(vertex);

					//don't need to change done1, as this will already remove all simplicial vertices

					if(DEBUG_VERBOSE_PREPROCESS) { System.out.println("Found simplicial vertex: " + vertex);}
				}
			}


			//remove almost simplicial vertices (minFill score = n, but all n edges have a vertex in common)
			//this could possibly make something simplicial or almost simplicial, therefore reset done1 when necessary
			nodesToCheck.addAll(gr);
			while(!nodesToCheck.isEmpty()) {
				FiniteVariable vertex = (FiniteVariable)nodesToCheck.iterator().next();
				nodesToCheck.remove(vertex);

				boolean aSimplicial=true;
				Set nei = gr.neighbors(vertex);

				if(DEBUG_VERBOSE_PREPROCESS) { System.out.println("Testing " + vertex + " for almost simplicial");}

				double tmpTS = totalStateSpace(vertex, nei);
				int tmpW = nei.size();
				if(tmpTS > low) { aSimplicial=false;}

				if(DEBUG_VERBOSE_PREPROCESS && !aSimplicial) { System.out.println(vertex + " failed because " + tmpTS + " > " + low);}


				FiniteVariable nei_u = null;
				FiniteVariable nei_u2 = null;

				for(Iterator itr1=nei.iterator(); aSimplicial && itr1.hasNext();) { //TODO could be simpler with using arrays
					FiniteVariable n1 = (FiniteVariable)itr1.next();
					for(Iterator itr2=nei.iterator(); aSimplicial && itr2.hasNext();) {
						FiniteVariable n2 = (FiniteVariable)itr2.next();

						if(n1!=n2) {
							if(!gr.containsEdge(n1, n2)) {//edge not present
								if(nei_u==null && nei_u2==null) { //first missing edge
									nei_u=n1;
									nei_u2=n2;

									if(DEBUG_VERBOSE_PREPROCESS) { System.out.println("Missing Edge " + nei_u + " " + nei_u2);}

									if(nei_u2.size() > vertex.size()) { //invalid u, (almost simplicial)
										nei_u2 = null;
										if(DEBUG_VERBOSE_PREPROCESS) { System.out.println(nei_u2 + " invalid due to size");}
									}
									if(nei_u.size() > vertex.size()) { //invalid u, (almost simplicial)
										if(DEBUG_VERBOSE_PREPROCESS) { System.out.println(nei_u + " invalid due to size");}
										nei_u = nei_u2;
										nei_u2 = null;

										if(nei_u==null) { aSimplicial=false;} //both invalid u
									}
								}
								else {

									if(nei_u2 != n1 && nei_u2 != n2) {
										nei_u2 = null;
									}
									if(nei_u != n1 && nei_u != n2) {
										nei_u = nei_u2;
										nei_u2 = null;

										if(nei_u==null) {
											aSimplicial=false;
											if(DEBUG_VERBOSE_PREPROCESS) { System.out.println(vertex + " was not almost simplicial after reviewing " + n1 + " " + n2);}
										} //no edge in common
									}
								}
							}
						}//end if n1!=n2
					}
				}

				if(aSimplicial) { //it could possibly be simplicial based on another variables removal, in this case nei_u and nei_u2 are both null
					done1 = false;

					if(nei_u==null) { //this node is actually simplicial
						if(tmpTS > low) { low = tmpTS;}
						if(tmpW > loww) loww = tmpW;
					}
					else { //this node is almost simplicial
						//connect up neighbors
						for(Iterator itr = nei.iterator(); nei_u!=null && itr.hasNext();) {
							Object neigh = itr.next();
							if(neigh != nei_u && !gr.containsEdge(neigh, nei_u)) {
								gr.addEdge(neigh, nei_u);
							}
						}
					}

					gr.remove(vertex);
					//don't need to add any edges
					eo.add(vertex);

					if(DEBUG_VERBOSE_PREPROCESS && nei_u==null) { System.out.println("Found simplicial vertex: " + vertex);}
					if(DEBUG_VERBOSE_PREPROCESS && nei_u!=null) { System.out.println("Found almost simplicial vertex: " + vertex);}
				}
			}//while have nodes to check
		}//end done1

		if(DEBUG_VERBOSE) { System.out.println("Preprocess finished with " + gr.size() + " nodes.");}
		if(DEBUG_VERBOSE) { System.out.println("Preprocess eliminated " + eo.size() + " nodes.");}
		if(DEBUG_VERBOSE) { System.out.println("Preprocess found low: " + low + " (min tss)");}
		if(DEBUG_VERBOSE) { System.out.println("Preprocess found loww: " + loww + " (min neighbor)");}

	}//end preprocess


	static private void printArr(int in[]) {
		if(in==null) { System.out.println("null"); return;}
		else if(in.length==0) { System.out.println("[]"); return;}

		System.out.print("[ " + in[0]);
		for(int i=1; i<in.length; i++) {
			System.out.print(", " + in[i]);
		}
		System.out.println("]");
	}
	static private void printArr(boolean in[]) {
		if(in==null) { System.out.println("null"); return;}
		else if(in.length==0) { System.out.println("[]"); return;}

		System.out.print("[ " + in[0]);
		for(int i=1; i<in.length; i++) {
			System.out.print(", " + in[i]);
		}
		System.out.println("]");
	}
	static private void printArr(double in[]) {
		if(in==null) { System.out.println("null"); return;}
		else if(in.length==0) { System.out.println("[]"); return;}

		System.out.print("[ " + in[0]);
		for(int i=1; i<in.length; i++) {
			System.out.print(", " + in[i]);
		}
		System.out.println("]");
	}

	static private void printArrWithIndex(int in[]) {
		if(in==null) { System.out.println("null"); return;}
		else if(in.length==0) { System.out.println("[]"); return;}

		System.out.print("[ 0:" + in[0]);
		for(int i=1; i<in.length; i++) {
			System.out.print(", " + i + ":"+ in[i]);
		}
		System.out.println("]");
	}
	static private void printArrWithIndex(boolean in[]) {
		if(in==null) { System.out.println("null"); return;}
		else if(in.length==0) { System.out.println("[]"); return;}

		System.out.print("[ 0:" + in[0]);
		for(int i=1; i<in.length; i++) {
			System.out.print(", " + i + ":"+ in[i]);
		}
		System.out.println("]");
	}
	static private void printArrWithIndex(double in[]) {
		if(in==null) { System.out.println("null"); return;}
		else if(in.length==0) { System.out.println("[]"); return;}

		System.out.print("[ 0:" + in[0]);
		for(int i=1; i<in.length; i++) {
			System.out.print(", " + i + ":"+ in[i]);
		}
		System.out.println("]");
	}


	static public class Util {
		private Util(){}

		static public void convertRCtoCnfDtree(RC rc, PrintStream out, HashMap fvToColOfClauses) {
			if(!rc.finished) throw new IllegalStateException("RC not finished");
			Map bnDT = createMapForRC(rc);
			out.println("dtree " + countSize(rc, fvToColOfClauses));
			writeMap((RC.RCLine)rc.rcObj.get(rc.rcObj.size()-1), bnDT, out, new int[]{0}, fvToColOfClauses);
		}

		static private Map createMapForRC(RC rc) {
			HashMap intToNd = new HashMap();
			for(int i=0; i<rc.rcObj.size(); i++) {
				RC.RCLine ln = (RC.RCLine)rc.rcObj.get(i);
				intToNd.put(new Integer(ln.id), ln);
			}
			return intToNd;
		}

		static int countSize(RC rc, HashMap fvToColOfClauses) {
			int ret = 0;
			for(Iterator itr = rc.rcObj.iterator(); itr.hasNext();) {
				RC.RCLine curr = (RC.RCLine)itr.next();
				if(curr.type=='L') {
					Collection clausesAtLeaf = (Collection)fvToColOfClauses.get(curr.leafVar);
					if(clausesAtLeaf==null || clausesAtLeaf.size()==0) continue;
					ret += clausesAtLeaf.size();
				}
			}
			return (2*ret-1);
		}

		//child-first traversal of tree
		static private int writeMap(RC.RCLine current, Map bnDT, PrintStream out, int nextNodeId[], HashMap fvToColOfClauses) {
			if(current==null) throw new IllegalStateException();

			if(current.type=='R' || current.type=='I') {
				int nodebelowL = writeMap((RC.RCLine)bnDT.get(new Integer(current.left)), bnDT, out, nextNodeId, fvToColOfClauses);
				int nodebelowR = writeMap((RC.RCLine)bnDT.get(new Integer(current.right)), bnDT, out, nextNodeId, fvToColOfClauses);

				//add this node if both children exist
				if(nodebelowL>=0 && nodebelowR>=0) {
					int myNum = nextNodeId[0];
					out.println("I " + nodebelowL + " " + nodebelowR);

					nextNodeId[0]++;
					return myNum;
				}
				else if(nodebelowL<0) return nodebelowR;
				else return nodebelowL;
			}
			else if(current.type=='L') {

				Collection clausesAtLeaf = (Collection)fvToColOfClauses.get(current.leafVar);
				if(clausesAtLeaf==null || clausesAtLeaf.size()==0) return -1;

				//add a leaf node for each clause from this table
				LinkedList list = new LinkedList();
				for(Iterator itr = clausesAtLeaf.iterator(); itr.hasNext();) {
					Integer nodeID = new Integer(nextNodeId[0]);
					Integer clauseID = (Integer)itr.next();
					out.println("L " + clauseID);

					nextNodeId[0]++;
					list.add(nodeID);
				}

				//connect up nodes in list in balanced sub-dtree
				while(list.size()>=2) {
					Integer first = (Integer)list.removeFirst();
					Integer second = (Integer)list.removeFirst();
					Integer nodeID = new Integer(nextNodeId[0]);
					out.println("I " + first + " " + second);

					nextNodeId[0]++;
					list.add(nodeID);
				}
				return ((Integer)list.removeFirst()).intValue();
			}
			else throw new IllegalStateException();
		}

	}



}//end class RCGenerator
