/*
* EOGenerator.java
*
*/
package edu.ucla.belief;
import java.util.*;
import edu.ucla.structure.*;

import java.text.NumberFormat;
import java.io.*;

/**
*
* @author David
* @version
*/


public class EOGenerator {
	static final boolean DEBUG_VERBOSE = false;
	static final boolean DEBUG_VERBOSE1 = false;
	public static final java.io.PrintStream STREAM_DEBUG = System.out;

//	static final public int largestAcceptableEOCluster = Integer.MAX_VALUE; //2,147,483,648
	static final public int largestAcceptableEOCluster = 130000000; //130MillTSS ~= 1GB
//	static final public int largestAcceptableEOCluster = 98000000; //98MillTSS ~= 750MB
//	static final public int largestAcceptableEOCluster = 65000000; //65MillTSS ~= 500MB
//	static final public int largestAcceptableEOCluster = 13000000; //13MillTSS ~= 100MB
//	static final public int largestAcceptableEOCluster = 33000000; //33MillTSS ~= 250MB
//	static final public int largestAcceptableEOCluster = 0;        //0MillTSS  ~= 0MB

//	static final public int largestAcceptableRCCache = 33000000; //33MillTSS ~= 250MB
//	static final public int largestAcceptableRCCache = 98000000; //98MillTSS ~= 750MB
//	static final public int largestAcceptableRCCache = 130000000; //130MillTSS ~= 1GB
	static final public int largestAcceptableRCCache = 260000000; //260MillTSS ~= 2GB

//	static final public int largestAcceptableRCCost = 98000000;
	static final public int largestAcceptableRCCost = Integer.MAX_VALUE;


	static final private int costFunctionTSS = 1;
	static final private int costFunctionMF = 2;
	static final private int costFunctionWMF = 3;

	static final private double callsPerMilliSec = 9000.0; //currently ranges from 5 to 12 million per SEC (so use 9 million, the average) then convert to MilliSec
	static private final double thresholdMin = 15;

	static private final boolean SpecialCondition = false;

	private EOGenerator(){}



	/**Temporary function to test new elimination functions.*/
	static private EO eliminateEO(UndirGraph udg, int costF1, boolean deterministic, double currentBestEO, int version) {
		if(version==1) {
			return eliminateEO1(udg, costF1, deterministic, currentBestEO);
		}
		else if(version==2) {
			return eliminateEO2(udg, costF1, deterministic, currentBestEO);
		}
		else if(version==3) {
			return eliminateEO3(udg, costF1, deterministic, currentBestEO, true, false);
		}
		else if(version==4) {
			return eliminateEO3(udg, costF1, deterministic, currentBestEO, false, false);
		}
		else if(version==5) {
			return eliminateEO3(udg, costF1, deterministic, currentBestEO, true, true);
		}
		else if(version==6) {
			return eliminateEO3(udg, costF1, deterministic, currentBestEO, false, true);
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	/**Temporary function to test new elimination functions.*/
	static private RC eliminateRC( UndirGraph udg, CollectionOfTables cot, int costF1, boolean deterministic, double currentBestEO, int version) {
		return eliminateRC1( udg, cot, costF1, deterministic, currentBestEO, version);
	}

	/*outDetailsFile can be null. netName can be null.*/
	static public EO generateEO(BeliefNetwork bn, String netName, String outDetailsFile, boolean forceExtendedSearch, int version) {
		if(DEBUG_VERBOSE) { STREAM_DEBUG.println("\n\n\nGenerateEO: " + netName + " version: " + version + (forceExtendedSearch ? " Extended Search" : " Regular Search") + "\n");}

		EO preprocessEO = new EO(bn.size());
		EO bestEO = null;
		int numTotalVars = bn.size();
		UndirGraph udg;

		{
			Graph udg_tmp = Graphs.moralGraph(bn);
			preprocess(udg_tmp, preprocessEO);
			udg = new UndirGraph(udg_tmp);
		}

		//Calculate difficulty of problem (do each algo once, no pruning based on best to setup timings)
		int minimumIterations;
		int maximumIterations;
		int numOrderingsPerAlgoTSS = 100; //be careful about changing (due to timings)
		int numOrderingsPerAlgoMF = 50;   //be careful about changing (due to timings)
		int numOrderingsPerAlgoWMF = 50;  //be careful about changing (due to timings)
		long timePerIter;  //approx milliSec per iteration

		{
			long tmpTime1 = System.currentTimeMillis();
			bestEO = eliminateEO((UndirGraph)udg.clone(), costFunctionTSS, true, Double.MAX_VALUE, version);
			long tmpTime2 = System.currentTimeMillis();
			bestEO = keepBest( bestEO, eliminateEO((UndirGraph)udg.clone(), costFunctionMF, true, Double.MAX_VALUE, version));
			long tmpTime3 = System.currentTimeMillis();
			bestEO = keepBest( bestEO, eliminateEO((UndirGraph)udg.clone(), costFunctionWMF, true, Double.MAX_VALUE, version));
			long tmpTime4 = System.currentTimeMillis();
			if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Original Score: " + bestEO.eoScore());}

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
				double et = bs/callsPerMilliSec; //expected computation time
//				double log_10_bs = Math.log(bs) / Math.log(10);   //complexity coefficient (i.e. 10^log_10_bs is complexity)

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
						if(DEBUG_VERBOSE) {STREAM_DEBUG.println("\n\n\nReduce numOrderingsPerAlgoTSS to 50\n\n\n");}
					}

					if(timePerIter > .20*et) {  //if the time for a single iteration is > 10% of expected time, do simpler iterations
						timePerIter=(long)(timePerIter*10.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=10;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=5;
						if(DEBUG_VERBOSE) {STREAM_DEBUG.println("\n\n\nReduce numOrderingsPerAlgoTSS to 10\n\n\n");}
					}
					else if(timePerIter > .10*et) {  //if the time for a single iteration is > 10% of expected time, do simpler iterations
						timePerIter=(long)(timePerIter*50.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=50;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=25;
						if(DEBUG_VERBOSE) {STREAM_DEBUG.println("\n\n\nReduce numOrderingsPerAlgoTSS to 50\n\n\n");}
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
					STREAM_DEBUG.println("\n\nIteration: " + countIterations);
					STREAM_DEBUG.println("Expected computation time is " + (et/1000.0) + " (sec) for expected score of " + bs);
					STREAM_DEBUG.println("Time so far:       " + (tmpTotalTimeSoFar));
					STREAM_DEBUG.println("MilliSec per Iter: " + timePerIter);
					STREAM_DEBUG.println("min: " + minimumIterations + " (" + (minimumIterations*timePerIter/1000.0) + " sec)");
					STREAM_DEBUG.println("max: " + maximumIterations + " (" + (maximumIterations*timePerIter/1000.0) + " sec)");
				}

				if(countIterations>maximumIterations) { //the above may have updated the max, if so possibly stop now (has not run the one listed as countIterations yet, so don't use >=)
					if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Searched the maximum number of iterations requested.\n");}
					break;
				}
			}


			//run TotalStateSpace
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoTSS; i++) {
				EO tbestEO = keepBest( bestEO, eliminateEO((UndirGraph)udg.clone(), costFunctionTSS, false, bestEO.eoScore(), version));
				if(tbestEO != bestEO) {
					bestEO = tbestEO;
					algoUsed = costFunctionTSS;
					if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Better score found by TSS: " + bestEO.eoScore() + "   iteration: " + countIterations);}
				}
			}
			timeTSS += (System.currentTimeMillis() - tmpTime);
			countTSS+=numOrderingsPerAlgoTSS;

			//run MF
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoMF; i++) {
				EO tbestEO = keepBest( bestEO, eliminateEO((UndirGraph)udg.clone(), costFunctionMF, false, bestEO.eoScore(), version));
				if(tbestEO != bestEO) {
					bestEO = tbestEO;
					algoUsed = costFunctionMF;
					if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Better score found by MF: " + bestEO.eoScore() + "   iteration: " + countIterations);}
				}
			}
			timeMF += (System.currentTimeMillis() - tmpTime);
			countMF+=numOrderingsPerAlgoMF;

			//run WMF
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoWMF; i++) {
				EO tbestEO = keepBest( bestEO, eliminateEO((UndirGraph)udg.clone(), costFunctionWMF, false, bestEO.eoScore(), version));
				if(tbestEO != bestEO) {
					bestEO = tbestEO;
					algoUsed = costFunctionWMF;
					if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Better score found by wMF: " + bestEO.eoScore() + "   iteration: " + countIterations);}
				}
			}
			timeWMF += (System.currentTimeMillis() - tmpTime);
			countWMF+=numOrderingsPerAlgoWMF;

			double improvementRate = (lastScore - bestEO.eoScore()) / lastScore;
			if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Improvement Rate for iteration " + countIterations + " was " + (improvementRate));}

			if(stop) { //already found good ordering (not during forceExtendedSearch)
				if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Current ordering is already fast, skip rest of search.\n");}
				break;
			}
			else if(countIterations>=maximumIterations) {
				if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Searched the maximum number of iterations requested.\n");}
				break;
			}
			else {
				int extraItr = (countIterations-minimumIterations);
				if(extraItr>=0) { //if using more than the minimum number of iterations, force it to have a better improvementRate
					if(improvementRate < (.03 + (extraItr*.01))) { //on first extra iteration will be .04, then .05, .06, ...
						if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Stopping search because improvement was too small.\n");}
						break;
					}
				}
			}

			if(improvementRate==0.0) {
				consecutiveItrsWithZeroIncrease++;
				if((!forceExtendedSearch && consecutiveItrsWithZeroIncrease>=3) ||
				    (forceExtendedSearch && consecutiveItrsWithZeroIncrease>=10)) {
					if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Stopping search because had " + consecutiveItrsWithZeroIncrease + " consecutive iterations with no improvement.\n");}
					break;
				}
			}
			else {
				consecutiveItrsWithZeroIncrease=0;
			}

			lastScore = bestEO.eoScore();
			countIterations++;

			timePerIter = System.currentTimeMillis()-timeItrSt;
		}


		String algoUsedStr = (algoUsed==costFunctionTSS ? "TSS" : (algoUsed==costFunctionMF ? "MF" : "wMF"));

		if(DEBUG_VERBOSE) {
			NumberFormat nfInt = NumberFormat.getInstance();
			NumberFormat nfDbl = NumberFormat.getInstance();

			nfInt.setMaximumFractionDigits(0);
			nfDbl.setMaximumFractionDigits(2);

			STREAM_DEBUG.println("GenerateEO made " + countIterations + " iterations.");
			STREAM_DEBUG.println("The final algorithm used was: " + algoUsedStr);
			STREAM_DEBUG.println("GenerateEO made found the best score of " + bestEO.eoScore());
			STREAM_DEBUG.println("   time in TSS: " + nfDbl.format(timeTSS/1000.0) + "   (" + nfInt.format(countTSS) + " orders)");
			STREAM_DEBUG.println("   time in  MF: " + nfDbl.format(timeMF/1000.0) + "   (" + nfInt.format(countMF) + " orders)");
			STREAM_DEBUG.println("   time in wMF: " + nfDbl.format(timeWMF/1000.0) + "   (" + nfInt.format(countWMF) + " orders)");
			STREAM_DEBUG.println("   total time : " + nfDbl.format((timeTSS+timeMF+timeWMF)/1000.0) + "   (" + nfInt.format(countTSS+countMF+countWMF) + " orders)");
			STREAM_DEBUG.println("   minimum number of iterations was set to : " + minimumIterations);
			STREAM_DEBUG.println("   maximum number of iterations was set to : " + maximumIterations);
			STREAM_DEBUG.println("   width: " + bestEO.width());
			STREAM_DEBUG.println("   tsScore: " + bestEO.tsScore());
			STREAM_DEBUG.println("   eoScore: " + bestEO.eoScore());
			STREAM_DEBUG.println("   numCond: " + bestEO.numCond());
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
				outTable.println(netName+"\t"+timeTSS+"\t"+timeMF+"\t"+timeWMF+"\t"+(timeTSS+timeMF+timeWMF)+"\t"+countTSS+"\t"+countMF+"\t"+countWMF+"\t"+(countTSS+countMF+countWMF)+"\t"+minimumIterations+"\t"+maximumIterations+"\t"+countIterations+"\t"+numTotalVars+"\t"+(numTotalVars-preprocessEO.length())+"\t"+"-1\t-1\t"+algoUsedStr+"\t");
			}
			catch(FileNotFoundException e) {}
		}


		preprocessEO.append(bestEO);
		return preprocessEO;
	}



	/*outDetailsFile can be null. netName can be null.*/
	static public RC generateRC(BeliefNetwork bn, String netName, String outDetailsFile, boolean forceExtendedSearch, int version) {
		if(DEBUG_VERBOSE) { STREAM_DEBUG.println("\n\n\nGenerateRC: " + netName + " version: " + version + (forceExtendedSearch ? " Extended Search" : " Regular Search") + "\n");}

		EO preprocessEO = new EO(bn.size());
		RC bestRC = null;
		int numTotalVars = bn.size();
		UndirGraph udg;

		{
			Graph udg_tmp = Graphs.moralGraph(bn);
			preprocess(udg_tmp, preprocessEO);
			udg = new UndirGraph(udg_tmp);
		}

		//Calculate difficulty of problem (do each algo once, no pruning based on best to setup timings)
		int minimumIterations;
		int maximumIterations;
		int numOrderingsPerAlgoTSS = 100; //be careful about changing (due to timings)
		int numOrderingsPerAlgoMF = 50;   //be careful about changing (due to timings)
		int numOrderingsPerAlgoWMF = 50;  //be careful about changing (due to timings)
		long timePerIter;  //approx milliSec per iteration

		{
			UndirGraph tmp_udg;
			CollectionOfTables tmp_cot;

			//TODO eventually can speedup code if can clone COT instead of simplfying the preprocessing multiple times


			long tmpTime1 = System.currentTimeMillis();
			tmp_udg = (UndirGraph)udg.clone();
			tmp_cot = new CollectionOfTables(bn);
			tmp_cot.simplifyFromElimOrd(preprocessEO.eo);
			bestRC = eliminateRC(tmp_udg, tmp_cot, costFunctionTSS, true, Double.MAX_VALUE, version);

			long tmpTime2 = System.currentTimeMillis();
			tmp_udg = (UndirGraph)udg.clone();
			tmp_cot = new CollectionOfTables(bn);
			tmp_cot.simplifyFromElimOrd(preprocessEO.eo);
			bestRC = keepBest( bestRC, eliminateRC(tmp_udg, tmp_cot, costFunctionMF, true, Double.MAX_VALUE, version));

			long tmpTime3 = System.currentTimeMillis();
			tmp_udg = (UndirGraph)udg.clone();
			tmp_cot = new CollectionOfTables(bn);
			tmp_cot.simplifyFromElimOrd(preprocessEO.eo);
			bestRC = keepBest( bestRC, eliminateRC(tmp_udg, tmp_cot, costFunctionWMF, true, Double.MAX_VALUE, version));

			long tmpTime4 = System.currentTimeMillis();
			if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Original Score: " + bestRC.rcScore());}

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
						if(DEBUG_VERBOSE) {STREAM_DEBUG.println("\n\n\nReduce numOrderingsPerAlgoTSS to 50\n\n\n");}
					}

					if(timePerIter > .20*et) {  //if the time for a single iteration is > 10% of expected time, do simpler iterations
						timePerIter=(long)(timePerIter*10.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=10;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=5;
						if(DEBUG_VERBOSE) {STREAM_DEBUG.println("\n\n\nReduce numOrderingsPerAlgoTSS to 10\n\n\n");}
					}
					else if(timePerIter > .10*et) {  //if the time for a single iteration is > 10% of expected time, do simpler iterations
						timePerIter=(long)(timePerIter*50.0/numOrderingsPerAlgoTSS);
						numOrderingsPerAlgoTSS=50;
						numOrderingsPerAlgoMF=numOrderingsPerAlgoWMF=25;
						if(DEBUG_VERBOSE) {STREAM_DEBUG.println("\n\n\nReduce numOrderingsPerAlgoTSS to 50\n\n\n");}
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
					STREAM_DEBUG.println("\n\nIteration: " + countIterations);
					STREAM_DEBUG.println("Expected computation time is " + (et/1000.0) + " (sec) for expected score of " + bs);
					STREAM_DEBUG.println("Time so far:       " + (tmpTotalTimeSoFar));
					STREAM_DEBUG.println("MilliSec per Iter: " + timePerIter);
					STREAM_DEBUG.println("min: " + minimumIterations + " (" + (minimumIterations*timePerIter/1000.0) + " sec)");
					STREAM_DEBUG.println("max: " + maximumIterations + " (" + (maximumIterations*timePerIter/1000.0) + " sec)");
				}

				if(countIterations>maximumIterations) { //the above may have updated the max, if so possibly stop now (has not run the one listed as countIterations yet, so don't use >=)
					if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Searched the maximum number of iterations requested.\n");}
					break;
				}
			}


			//run TotalStateSpace
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoTSS; i++) {
				UndirGraph tmp_udg = (UndirGraph)udg.clone();
				CollectionOfTables tmp_cot = new CollectionOfTables(bn);
				tmp_cot.simplifyFromElimOrd(preprocessEO.eo);
				RC tbestRC = keepBest( bestRC, eliminateRC(tmp_udg, tmp_cot, costFunctionTSS, false, bestRC.rcScore(), version));
				if(tbestRC != bestRC) {
					bestRC = tbestRC;
					algoUsed = costFunctionTSS;
					if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Better score found by TSS: " + bestRC.rcScore() + "   iteration: " + countIterations);}
				}
				System.gc();
			}
			timeTSS += (System.currentTimeMillis() - tmpTime);
			countTSS+=numOrderingsPerAlgoTSS;

			//run MF
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoMF; i++) {
				UndirGraph tmp_udg = (UndirGraph)udg.clone();
				CollectionOfTables tmp_cot = new CollectionOfTables(bn);
				tmp_cot.simplifyFromElimOrd(preprocessEO.eo);
				RC tbestRC = keepBest( bestRC, eliminateRC(tmp_udg, tmp_cot, costFunctionMF, false, bestRC.rcScore(), version));
				if(tbestRC != bestRC) {
					bestRC = tbestRC;
					algoUsed = costFunctionMF;
					if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Better score found by MF: " + bestRC.rcScore() + "   iteration: " + countIterations);}
				}
				System.gc();
			}
			timeMF += (System.currentTimeMillis() - tmpTime);
			countMF+=numOrderingsPerAlgoMF;

			//run WMF
			tmpTime = System.currentTimeMillis();
			for(int i=0; i<numOrderingsPerAlgoWMF; i++) {
				UndirGraph tmp_udg = (UndirGraph)udg.clone();
				CollectionOfTables tmp_cot = new CollectionOfTables(bn);
				tmp_cot.simplifyFromElimOrd(preprocessEO.eo);
				RC tbestRC = keepBest( bestRC, eliminateRC(tmp_udg, tmp_cot, costFunctionWMF, false, bestRC.rcScore(), version));
				if(tbestRC != bestRC) {
					bestRC = tbestRC;
					algoUsed = costFunctionWMF;
					if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Better score found by wMF: " + bestRC.rcScore() + "   iteration: " + countIterations);}
				}
				System.gc();
			}
			timeWMF += (System.currentTimeMillis() - tmpTime);
			countWMF+=numOrderingsPerAlgoWMF;

			double improvementRate = (lastScore - bestRC.rcScore()) / lastScore;
			if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Improvement Rate for iteration " + countIterations + " was " + (improvementRate));}

			if(stop) { //already found good ordering (not during forceExtendedSearch)
				if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Current ordering is already fast, skip rest of search.\n");}
				break;
			}
			else if(countIterations>=maximumIterations) {
				if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Searched the maximum number of iterations requested.\n");}
				break;
			}
			else {
				int extraItr = (countIterations-minimumIterations);
				if(extraItr>=0) { //if using more than the minimum number of iterations, force it to have a better improvementRate
					if(improvementRate < (.03 + (extraItr*.01))) { //on first extra iteration will be .04, then .05, .06, ...
						if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Stopping search because improvement was too small.\n");}
						break;
					}
				}
			}

			if(improvementRate==0.0) {
				consecutiveItrsWithZeroIncrease++;
				if((!forceExtendedSearch && consecutiveItrsWithZeroIncrease>=3) ||
				    (forceExtendedSearch && consecutiveItrsWithZeroIncrease>=10)) {
					if(DEBUG_VERBOSE) {STREAM_DEBUG.println("Stopping search because had " + consecutiveItrsWithZeroIncrease + " consecutive iterations with no improvement.\n");}
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


		String algoUsedStr = (algoUsed==costFunctionTSS ? "TSS" : (algoUsed==costFunctionMF ? "MF" : "wMF"));

		if(DEBUG_VERBOSE) {
			NumberFormat nfInt = NumberFormat.getInstance();
			NumberFormat nfDbl = NumberFormat.getInstance();

			nfInt.setMaximumFractionDigits(0);
			nfDbl.setMaximumFractionDigits(2);

			STREAM_DEBUG.println("GenerateRC made " + countIterations + " iterations.");
			STREAM_DEBUG.println("The final algorithm used was: " + algoUsedStr);
			STREAM_DEBUG.println("GenerateRC found the best score of " + bestRC.rcScore());
			STREAM_DEBUG.println("   time in TSS: " + nfDbl.format(timeTSS/1000.0) + "   (" + nfInt.format(countTSS) + " orders)");
			STREAM_DEBUG.println("   time in  MF: " + nfDbl.format(timeMF/1000.0) + "   (" + nfInt.format(countMF) + " orders)");
			STREAM_DEBUG.println("   time in wMF: " + nfDbl.format(timeWMF/1000.0) + "   (" + nfInt.format(countWMF) + " orders)");
			STREAM_DEBUG.println("   total time : " + nfDbl.format((timeTSS+timeMF+timeWMF)/1000.0) + "   (" + nfInt.format(countTSS+countMF+countWMF) + " orders)");
			STREAM_DEBUG.println("   minimum number of iterations was set to : " + minimumIterations);
			STREAM_DEBUG.println("   maximum number of iterations was set to : " + maximumIterations);
//			STREAM_DEBUG.println("   width: " + bestRC.width());
//			STREAM_DEBUG.println("   tsScore: " + bestRC.tsScore());
			STREAM_DEBUG.println("   rcScore: " + bestRC.rcScore());
//			STREAM_DEBUG.println("   numCond: " + bestRC.numCond());
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
				outTable.println(netName+"\t"+timeTSS+"\t"+timeMF+"\t"+timeWMF+"\t"+(timeTSS+timeMF+timeWMF)+"\t"+countTSS+"\t"+countMF+"\t"+countWMF+"\t"+(countTSS+countMF+countWMF)+"\t"+minimumIterations+"\t"+maximumIterations+"\t"+countIterations+"\t"+numTotalVars+"\t"+(numTotalVars-preprocessEO.length())+"\t"+seedScore+"\t"+bestRC.rcScore()+"\t"+algoUsedStr+"\t");
			}
			catch(FileNotFoundException e) {}
		}

		return bestRC;
	}

	static private EO keepBest(EO eo1, EO eo2) {
		if(eo1==null) {return eo2;}
		else if(eo2==null) {return eo1;}
		else if(eo1.eoScore() <= eo2.eoScore()) { return eo1;}
		else { return eo2;}
	}
	static private RC keepBest(RC rc1, RC rc2) {
		if(rc1==null) {return rc2;}
		else if(rc2==null) {return rc1;}
		else if(rc1.rcScore() <= rc2.rcScore()) { return rc1;}
		else { return rc2;}
	}


	static private RC eliminateRC1(UndirGraph udg, CollectionOfTables cot, int costF1, boolean deterministic, double currentBest, int scoreVer) {

		TreeSet allVerts = new TreeSet();
		ArrayList varsAtEnd = new ArrayList();
		{
			for(int i=0; i<udg.verts.length; i++) {
				if(udg.verts[i]!=null) {
					double score;
					if(costF1==costFunctionTSS) { score = udg.scoreMinWeight(udg.verts[i].id);}
					else if(costF1==costFunctionMF) { score = udg.scoreMinFill(udg.verts[i].id);}
					else if(costF1==costFunctionWMF) { score = udg.scoreWeightedMinFill(udg.verts[i].id);}
					else {throw new IllegalArgumentException();}
					udg.verts[i].eo_score = score;
					allVerts.add(udg.verts[i]);
				}
			}
		}

		while(!allVerts.isEmpty()) {
			Vertex v1=null; double s1=0;
			Vertex v2=null; double s2=0;
			Vertex v3=null; double s3=0;
			Vertex vCond=null;

			{
				Iterator itr_v = allVerts.iterator();
				v1=(Vertex)itr_v.next();
				if(itr_v.hasNext()) v2=(Vertex)itr_v.next();
				if(itr_v.hasNext()) v3=(Vertex)itr_v.next();
			}

			CollectionOfTables.ScoreResults scr=null;
			switch(scoreVer) {
				case 1: //cost per table
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.sumCounts / scr.numTables;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.sumCounts / scr.numTables;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.sumCounts / scr.numTables;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}
					break;
				}

				case 2: //cost
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.sumCounts;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.sumCounts;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.sumCounts;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}
					break;
				}

				case 3: //root context
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.rootContext;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.rootContext;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.rootContext;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}
					break;
				}

				case 4: //sum of context
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.sumContext;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.sumContext;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.sumContext;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}
					break;
				}

				case 5: //max context
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.maxContext;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}
					break;
				}

				case 6: //max context with Conditioning (find var in most tables)
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.maxContext;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}

					if(s1>largestAcceptableRCCache) {v1=null; s1=0;}
					if(s2>largestAcceptableRCCache) {v2=null; s2=0;}
					if(s3>largestAcceptableRCCache) {v3=null; s3=0;}

					if(v1==null && v2==null && v3==null) {
						FiniteVariable f = cot.varInMostTables(varsAtEnd);
						vCond = udg.findVert(f);
						if(vCond==null) { throw new IllegalStateException("Couldn't find " + f);}
					}
					break;
				}

				case 7: //max context with Conditioning (find one of 3 in most tables)
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.maxContext;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}

					if(s1>largestAcceptableRCCache && (s2>largestAcceptableRCCache || v2==null) && (s3>largestAcceptableRCCache || v3==null)) {

						int cot_i1 = (v1==null ? -1 : cot.vars.indexOf(v1.var));
						int cot_i2 = (v2==null ? -1 : cot.vars.indexOf(v2.var));
						int cot_i3 = (v3==null ? -1 : cot.vars.indexOf(v3.var));
						int cot_ic;

						//v1
						vCond=v1; cot_ic=cot_i1;

						//v2
						if(v2!=null) {
							if(cot.varCounts[cot_i2] > cot.varCounts[cot_ic]) {
								vCond=v2; cot_ic=cot_i2;
							}
						}

						//v3
						if(v3!=null) {
							if(cot.varCounts[cot_i3] > cot.varCounts[cot_ic]) {
								vCond=v3; cot_ic=cot_i3;
							}
						}

						v1=null; s1=0;
						v2=null; s2=0;
						v3=null; s3=0;
						if(vCond==null) { throw new IllegalStateException();}
					}
					if(s1>largestAcceptableRCCache) {v1=null; s1=0;}
					if(s2>largestAcceptableRCCache) {v2=null; s2=0;}
					if(s3>largestAcceptableRCCache) {v3=null; s3=0;}
					break;
				}

				case 8: //cost with Conditioning (find var in most tables)
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.sumCounts;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.sumCounts;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.sumCounts;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}

					if(s1>largestAcceptableRCCost) {v1=null; s1=0;}
					if(s2>largestAcceptableRCCost) {v2=null; s2=0;}
					if(s3>largestAcceptableRCCost) {v3=null; s3=0;}

					if(v1==null && v2==null && v3==null) {
						FiniteVariable f = cot.varInMostTables(varsAtEnd);
						vCond = udg.findVert(f);
						if(vCond==null) { throw new IllegalStateException("Couldn't find " + f);}
					}
					break;
				}

				case 9: //cost with Conditioning (find one of 3 in most tables)
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.sumCounts;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.sumCounts;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.sumCounts;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}

					if(s1>largestAcceptableRCCost && (s2>largestAcceptableRCCost||v2==null) && (s3>largestAcceptableRCCost||v3==null)) {
						int cot_i1 = (v1==null ? -1 : cot.vars.indexOf(v1.var));
						int cot_i2 = (v2==null ? -1 : cot.vars.indexOf(v2.var));
						int cot_i3 = (v3==null ? -1 : cot.vars.indexOf(v3.var));
						int cot_ic;

						//v1
						vCond=v1; cot_ic=cot_i1;

						//v2
						if(v2!=null) {
							if(cot.varCounts[cot_i2] > cot.varCounts[cot_ic]) {
								vCond=v2; cot_ic=cot_i2;
							}
						}

						//v3
						if(v3!=null) {
							if(cot.varCounts[cot_i3] > cot.varCounts[cot_ic]) {
								vCond=v3; cot_ic=cot_i3;
							}
						}

						v1=null; s1=0;
						v2=null; s2=0;
						v3=null; s3=0;
						if(vCond==null) { throw new IllegalStateException();}
					}

					if(s1>largestAcceptableRCCost) {v1=null; s1=0;}
					if(s2>largestAcceptableRCCost) {v2=null; s2=0;}
					if(s3>largestAcceptableRCCost) {v3=null; s3=0;}

					break;
				}




				case 10: //max context with Conditioning (find var in fewest tables)
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.maxContext;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}

					if(s1>largestAcceptableRCCache) {v1=null; s1=0;}
					if(s2>largestAcceptableRCCache) {v2=null; s2=0;}
					if(s3>largestAcceptableRCCache) {v3=null; s3=0;}

					if(v1==null && v2==null && v3==null) {
						FiniteVariable f = cot.varInFewestTables(varsAtEnd);
						vCond = udg.findVert(f);
						if(vCond==null) { throw new IllegalStateException("Couldn't find " + f);}
					}
					break;
				}

				case 11: //max context with Conditioning (find one of 3 in fewest tables)
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.maxContext;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}

					if(s1>largestAcceptableRCCache && (s2>largestAcceptableRCCache||v2==null) && (s3>largestAcceptableRCCache||v3==null)) {

						int cot_i1 = (v1==null ? Integer.MAX_VALUE : cot.vars.indexOf(v1.var));
						int cot_i2 = (v2==null ? Integer.MAX_VALUE : cot.vars.indexOf(v2.var));
						int cot_i3 = (v3==null ? Integer.MAX_VALUE : cot.vars.indexOf(v3.var));
						int cot_ic;

						//v1
						vCond=v1; cot_ic=cot_i1;

						//v2
						if(v2!=null) {
							if(cot.varCounts[cot_i2] < cot.varCounts[cot_ic]) {
								vCond=v2; cot_ic=cot_i2;
							}
						}

						//v3
						if(v3!=null) {
							if(cot.varCounts[cot_i3] < cot.varCounts[cot_ic]) {
								vCond=v3; cot_ic=cot_i3;
							}
						}

						v1=null; s1=0;
						v2=null; s2=0;
						v3=null; s3=0;
						if(vCond==null) { throw new IllegalStateException();}
					}
					if(s1>largestAcceptableRCCache) {v1=null; s1=0;}
					if(s2>largestAcceptableRCCache) {v2=null; s2=0;}
					if(s3>largestAcceptableRCCache) {v3=null; s3=0;}
					break;
				}

				case 12: //cost with Conditioning (find var in fewest tables)
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.sumCounts;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.sumCounts;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.sumCounts;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}

					if(s1>largestAcceptableRCCost) {v1=null; s1=0;}
					if(s2>largestAcceptableRCCost) {v2=null; s2=0;}
					if(s3>largestAcceptableRCCost) {v3=null; s3=0;}

					if(v1==null && v2==null && v3==null) {
						FiniteVariable f = cot.varInFewestTables(varsAtEnd);
						vCond = udg.findVert(f);
						if(vCond==null) { throw new IllegalStateException("Couldn't find " + f);}
					}
					break;
				}

				case 13: //cost with Conditioning (find one of 3 in fewest tables)
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.sumCounts;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.sumCounts;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.sumCounts;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}

					if(s1>largestAcceptableRCCost && (s2>largestAcceptableRCCost||v2==null) && (s3>largestAcceptableRCCost||v3==null)) {
						int cot_i1 = (v1==null ? Integer.MAX_VALUE : cot.vars.indexOf(v1.var));
						int cot_i2 = (v2==null ? Integer.MAX_VALUE : cot.vars.indexOf(v2.var));
						int cot_i3 = (v3==null ? Integer.MAX_VALUE : cot.vars.indexOf(v3.var));
						int cot_ic;

						//v1
						vCond=v1; cot_ic=cot_i1;

						//v2
						if(v2!=null) {
							if(cot.varCounts[cot_i2] < cot.varCounts[cot_ic]) {
								vCond=v2; cot_ic=cot_i2;
							}
						}

						//v3
						if(v3!=null) {
							if(cot.varCounts[cot_i3] < cot.varCounts[cot_ic]) {
								vCond=v3; cot_ic=cot_i3;
							}
						}

						v1=null; s1=0;
						v2=null; s2=0;
						v3=null; s3=0;
						if(vCond==null) { throw new IllegalStateException();}
					}

					if(s1>largestAcceptableRCCost) {v1=null; s1=0;}
					if(s2>largestAcceptableRCCost) {v2=null; s2=0;}
					if(s3>largestAcceptableRCCost) {v3=null; s3=0;}

					break;
				}

				case 14: //max context with special elimination
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.maxContext;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}

					if(s1>largestAcceptableRCCache) {v1=null; s1=0;}
					if(s2>largestAcceptableRCCache) {v2=null; s2=0;}
					if(s3>largestAcceptableRCCache) {v3=null; s3=0;}

					if(v1==null && v2==null && v3==null) {
						FiniteVariable f = cot.varInMostTables(varsAtEnd);
						v1 = udg.findVert(f); s1=1;
						if(v1==null) { throw new IllegalStateException("Couldn't find " + f);}
					}
					break;
				}

				case 15: //max context with special elimination
				{
					if(v1!=null) {
						scr = cot.calcScore1(v1.var);
						s1 = scr.maxContext;
						if(scr.useImmediately) {s1=1;s2=0;s3=0;v2=null;v3=null; break;}
					}

					if(v2!=null) {
						scr = cot.calcScore1(v2.var);
						s2 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=1;s3=0;v1=null;v3=null; break;}
					}

					if(v3!=null) {
						scr = cot.calcScore1(v3.var);
						s3 = scr.maxContext;
						if(scr.useImmediately) {s1=0;s2=0;s3=1;v1=null;v2=null; break;}
					}

					if(s1>largestAcceptableRCCache) {v1=null; s1=0;}
					if(s2>largestAcceptableRCCache) {v2=null; s2=0;}
					if(s3>largestAcceptableRCCache) {v3=null; s3=0;}

					if(v1==null && v2==null && v3==null) {
						FiniteVariable f = cot.varInFewestTables(varsAtEnd);
						v1 = udg.findVert(f); s1=1;
						if(v1==null) { throw new IllegalStateException("Couldn't find " + f);}
					}
					break;
				}
				default: throw new IllegalStateException();
			}//end switch score


//System.out.println("Scores s: \t" + s1 + " \t " + s2 + " \t " + s3 );

if(s1==0 && v1!=null) {throw new IllegalStateException();}
if(s2==0 && v2!=null) {throw new IllegalStateException();}
if(s3==0 && v3!=null) {throw new IllegalStateException();}


			if(vCond==null) { //eliminate a variable

				//pick var to eliminate
				Vertex remV = (v1!=null ? v1 : (v2!=null ? v2 : v3));//if deterministic take the best, otherwise pick from best 3
				if(!deterministic) {
					double power = -0.5; //use negative .5 so that probability distribution helps smaller numbers
					double b1 = (v1==null||s1==0 ? 0 : Math.pow(s1,power));
					double b2 = (v2==null||s2==0 ? 0 : Math.pow(s2,power));
					double b3 = (v3==null||s3==0 ? 0 : Math.pow(s3,power));
					double bAll = b1+b2+b3;

					if(Double.isNaN(bAll) || Double.isInfinite(bAll)) {
						System.err.println("Scores s: \t" + s1 + " \t " + s2 + " \t " + s3 );
						System.err.println("b1: " + b1);
						System.err.println("b2: " + b2);
						System.err.println("b3: " + b3);
						System.err.println("bAll: " + bAll);
						throw new IllegalStateException();
					}

//System.err.println("Scores b: \t" + b1 + " \t " + b2 + " \t " + b3 );

					double r = Math.random();

					if(r<(b1/bAll)) {
						remV = v1;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v1");}
					}
					else if(r<((b1+b2)/bAll)) {
						remV = v2;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v2");}
					}
					else if(r<((b1+b2+b3)/bAll)) {
						remV = v3;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v3");}
					}
					else {
						System.err.println("Scores s: \t" + s1 + " \t " + s2 + " \t " + s3 );
						System.err.println("b1: " + b1);
						System.err.println("b2: " + b2);
						System.err.println("b3: " + b3);
						System.err.println("bAll: " + bAll);
						System.err.println("r: " + r);
						throw new IllegalStateException();
					}

					if(remV==null) {
						System.err.println("Scores s: \t" + s1 + " \t " + s2 + " \t " + s3 );
						System.err.println("b1: " + b1);
						System.err.println("b2: " + b2);
						System.err.println("b3: " + b3);
						System.err.println("bAll: " + bAll);
						System.err.println("r: " + r);
						System.err.println("" + v1 + v2 + v3);
						throw new IllegalStateException();
					}
				}

				if(remV==null) {
					System.err.println("Scores s: \t" + s1 + " \t " + s2 + " \t " + s3 );
					System.err.println("" + v1 + v2 + v3);
					throw new IllegalStateException();
				}


				Set recompute = udg.vertsToRecompute(remV.id, costF1);
				recompute.remove(remV); //it is possibly there if using nei of nei
				allVerts.remove(remV);
				allVerts.removeAll(recompute);

				udg.connectAllNeighbors(remV.id);
				if( cot.mergeAllTables(remV.var) >= currentBest) {
					if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("pruning the search because exceeded cutoff");}
					return null;
				}
				udg.removeVariableAndEdges(remV.id);

				for(Iterator itr_r = recompute.iterator(); itr_r.hasNext();) {
					Vertex vr = (Vertex)itr_r.next();
					double score;
					if(costF1==costFunctionTSS) { score = udg.scoreMinWeight(vr.id);}
					else if(costF1==costFunctionMF) { score = udg.scoreMinFill(vr.id);}
					else if(costF1==costFunctionWMF) { score = udg.scoreWeightedMinFill(vr.id);}
					else {throw new IllegalArgumentException();}
					vr.eo_score = score;
					allVerts.add(vr);
				}
			}
			else { //condition a variable
//				System.out.println("\n\n\nUSING CONDITIONING: on " + vCond.var + "\n\n\n");

				if(SpecialCondition) {cot.conditionOnVariable(vCond.var);}

				Set recompute = udg.vertsToRecompute(vCond.id, costFunctionTSS); //force TSS because don't want neigh of neigh
				recompute.remove(vCond); //it is possibly there if using nei of nei
				allVerts.remove(vCond);
				allVerts.removeAll(recompute);

				varsAtEnd.add(vCond.var);
				udg.removeVariableAndEdges(vCond.id);

				for(Iterator itr_r = recompute.iterator(); itr_r.hasNext();) {
					Vertex vr = (Vertex)itr_r.next();
					double score;
					if(costF1==costFunctionTSS) { score = udg.scoreMinWeight(vr.id);}
					else if(costF1==costFunctionMF) { score = udg.scoreMinFill(vr.id);}
					else if(costF1==costFunctionWMF) { score = udg.scoreWeightedMinFill(vr.id);}
					else {throw new IllegalArgumentException();}
					vr.eo_score = score;
					allVerts.add(vr);
				}
			}

		} //while haven't eliminated all

		int numCondUsed = varsAtEnd.size();
		cot.rc.numCond = numCondUsed;

/*
		while(!varsAtEnd.isEmpty()) {
			int indx=-1;
			double best=Double.MAX_VALUE;

			for(int i=0; i<varsAtEnd.size(); i++) {
				CollectionOfTables.ScoreResults scr = cot.calcScore1((FiniteVariable)varsAtEnd.get(i));
				if(scr.useImmediately) {indx=i; break;}
				else if(scr.sumCounts < best) {indx=i; best=scr.sumCounts;}
			}
			if(cot.mergeAllTables((FiniteVariable)varsAtEnd.get(indx)) >= currentBest) {
					if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("pruning the search because exceeded cutoff");}
					return null;
			}
			varsAtEnd.remove(indx);
		}
*/
		while(!varsAtEnd.isEmpty()) {

			if(cot.mergeAllTables((FiniteVariable)varsAtEnd.remove(varsAtEnd.size()-1)) >= currentBest) {
					if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("pruning the search because exceeded cutoff");}
					return null;
			}
		}

		if(DEBUG_VERBOSE) STREAM_DEBUG.println("Used Cond: " + numCondUsed + " times.");

		return cot.finishCreatingRC();
	}//end eliminateRC1



	static private RC eliminateRC2(CollectionOfTables cot, boolean deterministic, double currentBest, int scoreVer) {

		HashSet allVars= new HashSet();
		{
			for(int i=0; i<cot.varCounts.length; i++) {
				if(cot.varCounts[i] > 0) { allVars.add(cot.vars.get(i));}
			}
		}

		int calcScoreVer = scoreVer;

		while(!allVars.isEmpty()) {
			FiniteVariable v1=null;
			FiniteVariable v2=null;
			FiniteVariable v3=null;

			double s1 = Double.MAX_VALUE;
			double s2 = Double.MAX_VALUE;
			double s3 = Double.MAX_VALUE;


			for(Iterator itr_v = allVars.iterator(); itr_v.hasNext();) {
				FiniteVariable tmp_v = (FiniteVariable)itr_v.next();
//TODO
				double tmp_s = 0;//cot.calcScore1(tmp_v, calcScoreVer);

				if(tmp_s < s1) {
					v3=v2; s3=s2;
					v2=v1; s2=s1;
					v1=tmp_v; s1=tmp_s;
					if(tmp_s < 0) {v2=null; v3=null; s1=1; break;}
				}
				else if(tmp_s < s2) {
					v3=v2; s3=s2;
					v2=tmp_v; s2=tmp_s;
				}
				else if(tmp_s < s3) {
					v3=tmp_v; s3=tmp_s;
				}
			}

			//pick var to eliminate
			FiniteVariable remV = (v1!=null ? v1 : (v2!=null ? v2 : v3));//if deterministic take the best, otherwise pick from best 3
			if(!deterministic) {
				double power = -0.5; //use negative .5 so that probability distribution helps smaller numbers
				double b1 = (v1==null ? 0 : Math.pow(s1,power));
				double b2 = (v2==null ? 0 : Math.pow(s2,power));
				double b3 = (v3==null ? 0 : Math.pow(s3,power));
				double bAll = b1+b2+b3;

				double r = Math.random();

				if(r<(b1/bAll)) {
					remV = v1;
					if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v1");}
				}
				else if(r<((b1+b2)/bAll)) {
					remV = v2;
					if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v2");}
				}
				else if(r<((b1+b2+b3)/bAll)) {
					remV = v3;
					if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v3");}
				}
				else {
					System.err.println("b1: " + b1);
					System.err.println("b2: " + b2);
					System.err.println("b3: " + b3);
					System.err.println("bAll: " + bAll);
					System.err.println("r: " + r);
					throw new IllegalStateException();
				}
			}

			//connect neighbors
			{
				allVars.remove(remV);

				if( cot.mergeAllTables(remV) >= currentBest) {
					if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("pruning the search because exceeded cutoff");}
					return null;
				}
			}
		} //while haven't eliminated all

		return cot.finishCreatingRC();
	}//end eliminateRC2

	static private EO eliminateEO3(UndirGraph udg, int costF1, boolean deterministic, double currentBestEO, boolean maximize3, boolean connectNeiCond) {

		TreeSet allVerts = new TreeSet();
		{
			for(int i=0; i<udg.verts.length; i++) {
				if(udg.verts[i]!=null) {
					double score;
					if(costF1==costFunctionTSS) { score = udg.scoreMinWeight(udg.verts[i].id);}
					else if(costF1==costFunctionMF) { score = udg.scoreMinFill(udg.verts[i].id);}
					else if(costF1==costFunctionWMF) { score = udg.scoreWeightedMinFill(udg.verts[i].id);}
					else {throw new IllegalArgumentException();}
					udg.verts[i].eo_score = score;
					allVerts.add(udg.verts[i]);
				}
			}
		}
		EO eo = new EO(allVerts.size());
		while(!allVerts.isEmpty()) {
			Vertex v1=null;
			Vertex v2=null;
			Vertex v3=null;
			boolean cond=true;

			{
				Iterator itr_v = allVerts.iterator();
				v1=(Vertex)itr_v.next(); if(v1.neiTSS <= largestAcceptableEOCluster) {cond=false;}
				if(itr_v.hasNext()) { v2=(Vertex)itr_v.next(); if(v2.neiTSS <= largestAcceptableEOCluster) {cond=false;}}
				if(itr_v.hasNext()) { v3=(Vertex)itr_v.next(); if(v3.neiTSS <= largestAcceptableEOCluster) {cond=false;}}
			}


			if(!cond) { //eliminate one of v1,v2,v3

				//if can't eliminate this variable, remove it from consideration
				if(v1!=null && v1.neiTSS > largestAcceptableEOCluster) { v1 = null;}
				if(v2!=null && v2.neiTSS > largestAcceptableEOCluster) { v2 = null;}
				if(v3!=null && v3.neiTSS > largestAcceptableEOCluster) { v3 = null;}

				//pick var to eliminate
				Vertex remV = (v1!=null ? v1 : (v2!=null ? v2 : v3));//if deterministic take the best, otherwise pick from best 3
				if(!deterministic) {
					double power = -0.5; //use negative .5 so that probability distribution helps smaller numbers
					double b1 = (v1==null ? 0 : Math.pow(v1.neiTSS,power));
					double b2 = (v2==null ? 0 : Math.pow(v2.neiTSS,power));
					double b3 = (v3==null ? 0 : Math.pow(v3.neiTSS,power));
					double bAll = b1+b2+b3;

					double r = Math.random();

					if(r<(b1/bAll)) {
						remV = v1;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v1");}
					}
					else if(r<((b1+b2)/bAll)) {
						remV = v2;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v2");}
					}
					else if(r<((b1+b2+b3)/bAll)) {
						remV = v3;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v3");}
					}
					else {
						throw new IllegalStateException();
					}
				}

				//connect neighbors
				{
					Set recompute = udg.vertsToRecompute(remV.id, costF1);
					recompute.remove(remV); //it is possibly there if using nei of nei
					allVerts.remove(remV);
					allVerts.removeAll(recompute);

					udg.connectAllNeighbors(remV.id);
					eo.addElim(remV);
					udg.removeVariableAndEdges(remV.id);

					for(Iterator itr_r = recompute.iterator(); itr_r.hasNext();) {
						Vertex vr = (Vertex)itr_r.next();
						double score;
						if(costF1==costFunctionTSS) { score = udg.scoreMinWeight(vr.id);}
						else if(costF1==costFunctionMF) { score = udg.scoreMinFill(vr.id);}
						else if(costF1==costFunctionWMF) { score = udg.scoreWeightedMinFill(vr.id);}
						else {throw new IllegalArgumentException();}
						vr.eo_score = score;
						allVerts.add(vr);
					}
				}
			}
			else { //find vertex to condition on
				if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using conditioning");}

				double c1 = (v1==null ? (maximize3?-1:Integer.MAX_VALUE) : Math.sqrt(v1.numNei)*v1.eo_score);
				double c2 = (v2==null ? (maximize3?-1:Integer.MAX_VALUE) : Math.sqrt(v2.numNei)*v2.eo_score);
				double c3 = (v3==null ? (maximize3?-1:Integer.MAX_VALUE) : Math.sqrt(v3.numNei)*v3.eo_score);

//TODO:try min and max
				Vertex vc1;

				if(maximize3) {
					//find max
					if(c1>=c2 && c1>=c3) { vc1=v1;}
					else if(c2>=c1 && c2>=c3) { vc1=v2;}
					else if(c3>=c1 && c3>=c2) { vc1=v3;}
					else {throw new IllegalStateException();}
				}
				else {
					//find min
					if(c1<=c2 && c1<=c3) { vc1=v1;}
					else if(c2<=c1 && c2<=c3) { vc1=v2;}
					else if(c3<=c1 && c3<=c2) { vc1=v3;}
					else {throw new IllegalStateException();}
				}

				Set recompute = udg.vertsToRecompute(vc1.id, (connectNeiCond ? costF1 : costFunctionTSS)); //force TSS because don't want neigh of neigh
				recompute.remove(vc1); //it is possibly there if using nei of nei
				allVerts.remove(vc1);
				allVerts.removeAll(recompute);

				eo.addCond(vc1);
				if(connectNeiCond) udg.connectAllNeighbors(vc1.id);
				udg.removeVariableAndEdges(vc1.id);

				for(Iterator itr_r = recompute.iterator(); itr_r.hasNext();) {
					Vertex vr = (Vertex)itr_r.next();
					double score;
					if(costF1==costFunctionTSS) { score = udg.scoreMinWeight(vr.id);}
					else if(costF1==costFunctionMF) { score = udg.scoreMinFill(vr.id);}
					else if(costF1==costFunctionWMF) { score = udg.scoreWeightedMinFill(vr.id);}
					else {throw new IllegalArgumentException();}
					vr.eo_score = score;
					allVerts.add(vr);
				}
			}

			//if parial elim ord cost > current best stop this iteration & begin again
			if(eo.eoScore() >= currentBestEO) {
				if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("pruning the search because exceeded cutoff");}
				return null;
			}

		}//end while allVerts not empty

		if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("eliminate found an ordering with cost " + eo.eoScore);}

		if(DEBUG_VERBOSE) {
			if(eo.numCond()>0) { STREAM_DEBUG.println("This order USED conditioning " + eo.numCond() + " times");}
			else { STREAM_DEBUG.println("This order did NOT use conditioning");}
		}

		return eo;

	}//end eliminate


	static private EO eliminateEO2(UndirGraph udg, int costF1, boolean deterministic, double currentBestEO) {

		TreeSet allVerts = new TreeSet();
		{
			for(int i=0; i<udg.verts.length; i++) {
				if(udg.verts[i]!=null) {
					double score;
					if(costF1==costFunctionTSS) { score = udg.scoreMinWeight(udg.verts[i].id);}
					else if(costF1==costFunctionMF) { score = udg.scoreMinFill(udg.verts[i].id);}
					else if(costF1==costFunctionWMF) { score = udg.scoreWeightedMinFill(udg.verts[i].id);}
					else {throw new IllegalArgumentException();}
					udg.verts[i].eo_score = score;
					allVerts.add(udg.verts[i]);
				}
			}
		}
		EO eo = new EO(allVerts.size());
		while(!allVerts.isEmpty()) {
			Vertex v1=null;
			Vertex v2=null;
			Vertex v3=null;

			{
				Iterator itr_v = allVerts.iterator();
				boolean done = false;
				{
					while(itr_v.hasNext()) {
						Vertex t=(Vertex)itr_v.next();
						if(t.neiTSS < thresholdMin) {done=true; if(DEBUG_VERBOSE) {STREAM_DEBUG.println("used thresholdMin");} v1=t; break;}
					}
				}

				if(!done) {
					itr_v = allVerts.iterator();

					//look for v1 which doesn't require conditioning
					while(itr_v.hasNext()) {
						v1=(Vertex)itr_v.next();
						if(v1.neiTSS <= largestAcceptableEOCluster) { break;} //doesn't require conditioning
					}
					//look for v2 which doesn't require conditioning
					while(!done && itr_v.hasNext()) {
						v2=(Vertex)itr_v.next();
						if(v2.neiTSS <= largestAcceptableEOCluster) { break;} //doesn't require conditioning
					}
					//look for v3 which doesn't require conditioning
					while(!done && itr_v.hasNext()) {
						v3=(Vertex)itr_v.next();
						if(v3.neiTSS <= largestAcceptableEOCluster) { break;} //doesn't require conditioning
					}
				}
			}

			if(v1!=null) { //eliminate one of v1,v2,v3
				//pick var to eliminate
				Vertex remV = v1;//if deterministic take the best, otherwise pick from best 3
				if(!deterministic) {
					double power = -0.5; //use negative .5 so that probability distribution helps smaller numbers
					double b1 = (v1==null ? 0 : Math.pow(v1.neiTSS,power));
					double b2 = (v2==null ? 0 : Math.pow(v2.neiTSS,power));
					double b3 = (v3==null ? 0 : Math.pow(v3.neiTSS,power));
					double bAll = b1+b2+b3;

					double r = Math.random();

					if(r<(b1/bAll)) {
						remV = v1;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v1");}
					}
					else if(r<((b1+b2)/bAll)) {
						remV = v2;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v2");}
					}
					else if(r<((b1+b2+b3)/bAll)) {
						remV = v3;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v3");}
					}
					else {
						throw new IllegalStateException();
					}
				}

				//connect neighbors
				{
					Set recompute = udg.vertsToRecompute(remV.id, costF1);
					recompute.remove(remV); //it is possibly there if using nei of nei
					allVerts.remove(remV);
					allVerts.removeAll(recompute);

					udg.connectAllNeighbors(remV.id);
					eo.addElim(remV);
					udg.removeVariableAndEdges(remV.id);

					for(Iterator itr_r = recompute.iterator(); itr_r.hasNext();) {
						Vertex vr = (Vertex)itr_r.next();
						double score;
						if(costF1==costFunctionTSS) { score = udg.scoreMinWeight(vr.id);}
						else if(costF1==costFunctionMF) { score = udg.scoreMinFill(vr.id);}
						else if(costF1==costFunctionWMF) { score = udg.scoreWeightedMinFill(vr.id);}
						else {throw new IllegalArgumentException();}
						vr.eo_score = score;
						allVerts.add(vr);
					}
				}
			}
			else { //find vertex to condition on
				if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using conditioning");}

				Iterator itr_v = allVerts.iterator();

				Vertex vc1 = (Vertex)itr_v.next();
				double costvc1 = Math.sqrt(vc1.numNei)*vc1.eo_score;

				while(itr_v.hasNext()) {
					Vertex tmp = (Vertex)itr_v.next();
					double costtmp = Math.sqrt(tmp.numNei)*tmp.eo_score;
					if(costtmp<costvc1) {
						vc1 = tmp;
						costvc1 = costtmp;
					}
				}

				Set recompute = udg.vertsToRecompute(vc1.id, costFunctionTSS); //force TSS because don't want neigh of neigh
				recompute.remove(vc1); //it is possibly there if using nei of nei
				allVerts.remove(vc1);
				allVerts.removeAll(recompute);

				eo.addCond(vc1);
				udg.removeVariableAndEdges(vc1.id);

				for(Iterator itr_r = recompute.iterator(); itr_r.hasNext();) {
					Vertex vr = (Vertex)itr_r.next();
					double score;
					if(costF1==costFunctionTSS) { score = udg.scoreMinWeight(vr.id);}
					else if(costF1==costFunctionMF) { score = udg.scoreMinFill(vr.id);}
					else if(costF1==costFunctionWMF) { score = udg.scoreWeightedMinFill(vr.id);}
					else {throw new IllegalArgumentException();}
					vr.eo_score = score;
					allVerts.add(vr);
				}
			}

			//if parial elim ord cost > current best stop this iteration & begin again
			if(eo.eoScore() >= currentBestEO) {
				if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("pruning the search because exceeded cutoff");}
				return null;
			}

		}//end while allVerts not empty

		if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("eliminate found an ordering with cost " + eo.eoScore);}

		if(DEBUG_VERBOSE) {
			if(eo.numCond()>0) { STREAM_DEBUG.println("This order USED conditioning " + eo.numCond() + " times");}
			else { STREAM_DEBUG.println("This order did NOT use conditioning");}
		}

		return eo;

	}//end eliminate




	/**
	 *  costF1 determines the algorithm to use (1=totalStateSpace, 2=minFill, 3=weighted-MinFill).
	 *  deterministic if true, will always pick the best (although still uses thresholdMin), otherwise will pick from best 3.
	 *  currentBestEO if the eo this finds has a larger cost than this number, it will stop and not return the new eo (upper bound).
	 *  Will return an elimination order if it finds one with a cost < currentBestEO.
	 */
	static private EO eliminateEO1(UndirGraph udg, int costF1, boolean deterministic, double currentBestEO) {

		HashSet allVerts = new HashSet(udg.verts.length);
		{
			for(int i=0; i<udg.verts.length; i++) {
				if(udg.verts[i]!=null) allVerts.add(udg.verts[i]);
			}
		}
		EO eo = new EO(allVerts.size());
		while(!allVerts.isEmpty()) {
			Vertex v1=null; double c1=Double.MAX_VALUE; //pick one of these three to eliminate
			Vertex v2=null; double c2=Double.MAX_VALUE;
			Vertex v3=null; double c3=Double.MAX_VALUE;
			Vertex vc1=null; double cc1=Double.MAX_VALUE; //condition on this variable

			for(Iterator iter=allVerts.iterator(); iter.hasNext();) {
				Vertex tmpV = (Vertex)iter.next();

				double tmpTSS = udg.scoreMinWeight(tmpV.id);

				if(tmpTSS < thresholdMin) { //if this variable is really easy, skip computing the rest of the scores
					v1=tmpV;
					v2=null;
					v3=null;
					if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("used thresholdMin");}
					break;
				}

				if(tmpTSS > largestAcceptableEOCluster && v1 != null) { continue;} //if this would require conditioning but have seen something else to eliminate skip

				double tmpC1;
				{
					if(costF1==costFunctionTSS) {
						tmpC1 = tmpTSS;
					}
					else if(costF1==costFunctionMF) {
						tmpC1 = udg.scoreMinFill(tmpV.id);
					}
					else if(costF1==costFunctionWMF) {
						tmpC1 = udg.scoreWeightedMinFill(tmpV.id);
					}
					else {throw new IllegalArgumentException();}
				}

				if(tmpTSS <= largestAcceptableEOCluster) {
					vc1=null; cc1=0; //don't need to condition if found something to eliminate

					//place tmpV in v1..v3 if it is one of the best
					if(v1==null || tmpC1<c1) {
						v3=v2; c3=c2;
						v2=v1; c2=c1;
						v1=tmpV; c1=tmpC1;
					}
					else if(v2==null || tmpC1<c2) {
						v3=v2; c3=c2;
						v2=tmpV; c2=tmpC1;
					}
					else if(v3==null || tmpC1<c3) {
						v3=tmpV; c3=tmpC1;
					}
				}
				else if(v1==null) { //haven't found anything to eliminate yet, test for minimum conditioning variable
					double tmpC2 = Math.sqrt(tmpV.numNei)*tmpC1; //should be used for min-Fill & weighted min-Fill
					//TODO eventually implement the other C2 for the min-Weight algorithm

					if(tmpC2<cc1) {
						vc1=tmpV;
						cc1=tmpC2;
					}
				}
			}//for each vertex calculate score

			if(v1==null) { //need to condition
				if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using conditioning");}
				allVerts.remove(vc1);
				eo.addCond(vc1);
				udg.removeVariableAndEdges(vc1.id);
			}
			else { //have at least one variabe which can still be eliminated
				//pick var to eliminate
				Vertex remV = v1;//if deterministic take the best, otherwise pick from best 3
				if(!deterministic) {
					double power = -0.5; //use negative .5 so that probability distribution helps smaller numbers
					double b1 = (v1==null ? 0 : Math.pow(v1.neiTSS,power));
					double b2 = (v2==null ? 0 : Math.pow(v2.neiTSS,power));
					double b3 = (v3==null ? 0 : Math.pow(v3.neiTSS,power));
					double bAll = b1+b2+b3;

					double r = Math.random();

					if(r<(b1/bAll)) {
						remV = v1;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v1");}
					}
					else if(r<((b1+b2)/bAll)) {
						remV = v2;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v2");}
					}
					else if(r<((b1+b2+b3)/bAll)) {
						remV = v3;
						if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Using elimination on v3");}
					}
					else {
						throw new IllegalStateException();
					}
				}

				//connect neighbors
				udg.connectAllNeighbors(remV.id);
				allVerts.remove(remV);
				eo.addElim(remV);
				udg.removeVariableAndEdges(remV.id);
			}

			//if parial elim ord cost > current best stop this iteration & begin again
			if(eo.eoScore() >= currentBestEO) {
				if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("pruning the search because exceeded cutoff");}
				return null;
			}

		}//while !allVerts.isEmpty
		if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("eliminate found an ordering with cost " + eo.eoScore);}

		if(DEBUG_VERBOSE) {
			if(eo.numCond()>0) { STREAM_DEBUG.println("This order USED conditioning " + eo.numCond() + " times");}
			else { STREAM_DEBUG.println("This order did NOT use conditioning");}
		}

		return eo;
	}//end eliminate


	static private double totalStateSpace(FiniteVariable v, Collection nei) {
		double ret=v.size();
		for(Iterator itr = nei.iterator(); itr.hasNext();) {
			ret *= ((FiniteVariable)itr.next()).size();
		}
		return ret;
	}


	static private void preprocess(Graph gr, EO eo) {
		double low = 0.0;

		if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Preprocess began with " + gr.size() + " nodes");}

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

					nodesToCheck.addAll(nei); //these could now possibly be simplicial

					gr.remove(vertex);
					//don't need to add any edges
					eo.addElim(vertex, tmpW, tmpTS);

					//don't need to change done1, as this will already remove all simplicial vertices

					if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Found simplicial vertex: " + vertex);}
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

				double tmpTS = totalStateSpace(vertex, nei);
				int tmpW = nei.size();
				if(tmpTS > low) { aSimplicial=false;}

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

									if(nei_u2.size() > vertex.size()) { //invalid u, (almost simplicial)
										nei_u2 = null;
									}
									if(nei_u.size() > vertex.size()) { //invalid u, (almost simplicial)
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

										if(nei_u==null) { aSimplicial=false;} //no edge in common
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
					eo.addElim(vertex, tmpW, tmpTS);

					if(DEBUG_VERBOSE1) { STREAM_DEBUG.println("Found almost simplicial vertex: " + vertex);}
				}
			}//while have nodes to check
		}//end done1

		if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Preprocess finished with " + gr.size() + " nodes.");}
		if(DEBUG_VERBOSE) { STREAM_DEBUG.println("Preprocess eliminated " + eo.length() + " nodes.");}

	}//end preprocess


	static public class EO implements Cloneable {
		protected ArrayList eo;
		protected int width=-1;
		protected double tsScore=-1; //largest neiTSS from any single condition or elimination
		protected double eoScore=0;
		protected int numCond=0;
		private double cutTSS = 1.0;

		private EO() {}

		public EO(int size) {
			eo = new ArrayList(size);
		}

		public List eo() { return Collections.unmodifiableList(eo);}
		public int width() { return width;}
		public double tsScore() { return tsScore;}
		public double eoScore() { return eoScore;}
		public int length() { return eo.size();}
		public int numCond() { return numCond;}

		public Object clone() {
			EO ret = new EO();
			ret.eo = (ArrayList)eo.clone();
			ret.width = width;
			ret.tsScore = tsScore;
			ret.eoScore = eoScore;
			ret.numCond = numCond;
			ret.cutTSS = cutTSS;
			return ret;
		}

		public void addCond(Vertex vertex) {
			eo.add(vertex.var);
			numCond++;
			if(vertex.numNei>width) { width = vertex.numNei;}
			if(vertex.neiTSS>tsScore) { tsScore = vertex.neiTSS;}
			cutTSS *= vertex.varSize;
		}
		public void addElim(Vertex vertex) {
			eo.add(vertex.var);
			if(vertex.numNei>width) { width = vertex.numNei;}
			if(vertex.neiTSS>tsScore) { tsScore = vertex.neiTSS;}
			eoScore += (cutTSS * vertex.neiTSS);
		}
		public void addElim(Object vertex, int w, double ts) {
			eo.add(vertex);
			if(w>width) { width = w;}
			if(ts>tsScore) { tsScore = ts;}
			eoScore += (cutTSS * ts);
		}

		public void append(EO eoEnd) {
			eo.addAll(eoEnd.eo);
			if(eoEnd.width > width) { width = eoEnd.width;}
			if(eoEnd.tsScore > tsScore) { tsScore = eoEnd.tsScore;}
			eoScore += eoEnd.eoScore;
			numCond += eoEnd.numCond;
			cutTSS *= eoEnd.cutTSS;
		}
	}

	static public class RC {
		protected ArrayList rcObj;
		final int size;
		protected double rcScore = -1;
		double currentCount = 0;
		int nextID = 0;

		public int maxCluster=-1;
		public int numCond=0;

		public RC(int size) {
			this.size = size;
			rcObj = new ArrayList((2*size) + 15);
		}

		public double rcScore() { return rcScore;}

		public void add(String line) {
			rcObj.add(line);
		}

		public void writeFile(Writer wtr) throws IOException {
			wtr.write("# " + new Date() + "\n");
			wtr.write("# RC Search Score: " + rcScore + "\n");
			for(int i=rcObj.size()-1; i>=0; i--) {
				wtr.write((String)rcObj.get(i) + "\n");
			}
		}
	}

	static private class UndirGraph implements Cloneable {
		final Vertex verts[];

		private UndirGraph(Vertex v[]) {
			verts = v;
		}

		public UndirGraph(Graph gr) {
			int size = gr.size();
			verts = new Vertex[size];
			MappedList mlvert = new MappedList(size);

			//generate Vertex objects
			int i=0;
			for(Iterator itr = gr.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				verts[i] = new Vertex(this, i, fv, size);
				i++;
				mlvert.add(fv);
			}

			//generate edges (already moralized)
			for(i=0; i<verts.length; i++) {
				FiniteVariable fv = verts[i].var;
				Set neighbors = gr.neighbors(fv);

				for(Iterator iter1 = neighbors.iterator(); iter1.hasNext();) {
					int n1 = mlvert.indexOf(iter1.next());
					addEdge(i, n1); //add edges
				}
			}
		}

		public UndirGraph(BeliefNetwork bn) {
			int size = bn.size();
			verts = new Vertex[size];
			MappedList mlvert = new MappedList(size);

			//generate Vertex objects
			int i=0;
			for(Iterator itr = bn.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				verts[i] = new Vertex(this, i, fv, size);
				i++;
				mlvert.add(fv);
			}

			//generate edges & moralize
			for(i=0; i<verts.length; i++) {
				FiniteVariable fv = verts[i].var;
				Set parents = bn.inComing(fv);
				for(Iterator piter1 = parents.iterator(); piter1.hasNext();) {
					int p1 = mlvert.indexOf(piter1.next());
					addEdge(i, p1); //add directed edges (as undirected)

					for(Iterator piter2 = parents.iterator(); piter2.hasNext();) {
						int p2 = mlvert.indexOf(piter2.next());

						if(p1!=p2) { addEdge(p1, p2);} //moralize parents
					}
				}
			}
		}

		public Object clone() {
			Vertex v[] = new Vertex[verts.length];
			UndirGraph ret = new UndirGraph(v);

			for(int i=0; i<verts.length; i++) {
				v[i] = (verts[i]==null ? null : new Vertex(ret, verts[i]));
			}
			return ret;
		}

		public Vertex findVert(FiniteVariable fv) {
			for(int i=0; i<verts.length; i++) {
				if(verts[i]!=null && verts[i].var==fv) {return verts[i];}
			}
			return null;
		}

		private void addEdge(int n1, int n2) {
			if(n1<0 || n2<0 || n1>=verts.length || n2>=verts.length || n1==n2) { throw new IllegalArgumentException("Error in addEdge(" + n1 + "," + n2 + ")");}

			verts[n1].addNeighbor(n2);
			verts[n2].addNeighbor(n1);
		}//end addEdge

		private void removeEdge(int n1, int n2) {
			if(n1<0 || n2<0 || n1>=verts.length || n2>=verts.length || n1==n2) { throw new IllegalArgumentException("Error in removeEdge(" + n1 + "," + n2 + ")");}

			verts[n1].removeNeighbor(n2);
			verts[n2].removeNeighbor(n1);
		}
		private int removeAllEdges(int n1) {
			if(n1<0 || n1>=verts.length) { throw new IllegalArgumentException("Error in removeAllEdges(" + n1 + ")");}
			int ret=0;

			Vertex v1 = verts[n1];
			int neiI=v1.firstNei;
			while(neiI!=-1) {
				verts[neiI].removeNeighbor(n1);
				neiI=v1.nextNei[neiI];
				ret++;
			}
			v1.removeAllNeighbors();
			return ret;
		}
		private int removeVariableAndEdges(int n1) {
			if(n1<0 || n1>=verts.length) { throw new IllegalArgumentException("Error in removeAllEdges(" + n1 + ")");}
			int ret=removeAllEdges(n1);
			verts[n1]=null;
			return ret;
		}
		private void connectAllNeighbors(int n1) {
			if(n1<0 || n1>=verts.length) { throw new IllegalArgumentException("Error in removeAllEdges(" + n1 + ")");}
			Vertex v = verts[n1];
			if(v.numNei==0) { return;}
			else {
				int nextVert1 = v.firstNei;
				while(nextVert1 !=-1) {
					int nextVert2 = v.nextNei[nextVert1];
					while(nextVert2!=-1) {
						if(!verts[nextVert1].isNeighbor(nextVert2)) {
							verts[nextVert1].addNeighbor(nextVert2);
							verts[nextVert2].addNeighbor(nextVert1);
						}
						nextVert2 = v.nextNei[nextVert2];
					}
					nextVert1 = v.nextNei[nextVert1];
				}
			}
		}

		Set vertsToRecompute(int n1, int costF1) {
			if(n1<0 || n1>=verts.length) { throw new IllegalArgumentException("Error in vertsToRecompute(" + n1 + ")");}
			Vertex v = verts[n1];

			if(v.numNei==0) { return Collections.EMPTY_SET;}
			else {
				HashSet ret = new HashSet(v.numNei); //could possibly be larger if including neigh of neigh
				addNeighborsToSet(n1, ret, (costF1==costFunctionMF || costF1==costFunctionWMF));
				return ret;
			}
		}

		private void addNeighborsToSet(int n1, Set nei, boolean includeNeiOfNei) {
			Vertex v = verts[n1];
			if(v.numNei==0) { return;}
			else {
				int nextVert1 = v.firstNei;
				while(nextVert1 !=-1) {
					nei.add(verts[nextVert1]);
					if(includeNeiOfNei) { addNeighborsToSet(verts[nextVert1].id, nei, false);}
					nextVert1 = v.nextNei[nextVert1];
				}
			}
		}

		public double scoreMinWeight(int i) {
			Vertex v = verts[i];
			if(v.numNei==0) { return 0;}
			else { return v.neiTSS;}
		}
		public double scoreMinFill(int i) {
			Vertex v = verts[i];
			if(v.numNei==0) { return 0;}
			else {
				double ret = 0;

				int nextVert1 = v.firstNei;
				while(nextVert1!=-1) {
					int nextVert2 = v.nextNei[nextVert1];
					while(nextVert2!=-1) {
						if(!verts[nextVert1].isNeighbor(nextVert2)) {ret++;}
						nextVert2 = v.nextNei[nextVert2];
					}
					nextVert1=v.nextNei[nextVert1];
				}
				return ret;
			}
		}
		public double scoreWeightedMinFill(int i) {
			Vertex v = verts[i];
			if(v.numNei==0) { return 0;}
			else {
				double ret = 0;

				int nextVert1 = v.firstNei;
				while(nextVert1!=-1) {
					int nextVert2 = v.nextNei[nextVert1];
					while(nextVert2!=-1) {
						if(!verts[nextVert1].isNeighbor(nextVert2)) {
							ret+=((double)verts[nextVert1].varSize * (double)verts[nextVert2].varSize);
						}
						nextVert2 = v.nextNei[nextVert2];
					}
					nextVert1=v.nextNei[nextVert1];
				}
				return ret;
			}
		}
	}//end class UndirGraph

	static private class Vertex implements Comparable {
		final UndirGraph udg;
		final int id;
		final FiniteVariable var;
		final int varSize; //used as node weight

		int numNei;
		double neiTSS; //total state space of var & its neighbors

		int firstNei=-1;
		final int nextNei[]; //-1 if this is the  last neighbor (or not in list), else 0..numNei-1
		final int prevNei[]; //-1 if this is the first neighbor (or not in list), else 0..numNei-1

		double eo_score;//used for elimination order heap
		final private double eo_score2Random = Math.random();

		public Vertex(UndirGraph udg, Vertex v) {
			this.udg = udg;
			id = v.id;
			var = v.var;
			varSize = v.varSize;

			numNei = v.numNei;
			neiTSS = v.neiTSS;

			firstNei = v.firstNei;
			nextNei = (int[])v.nextNei.clone();
			prevNei = (int[])v.prevNei.clone();
		}

		public Vertex(UndirGraph udg, int id, FiniteVariable var, int numVertices) {
			this.udg = udg;
			this.id = id;
			this.var = var;
			this.varSize = var.size();

			numNei=0;
			neiTSS=varSize;

			nextNei = new int[numVertices];
			prevNei = new int[numVertices];

			Arrays.fill(nextNei, -1);
			Arrays.fill(prevNei, -1);
		}

		boolean isNeighbor(int n) {
			if(firstNei==n || prevNei[n]!=-1 || nextNei[n]!=-1) { return true;}  //if it is the only one, or has a preceeding or following neighbor it is in the list
			else return false;
		}

		void addNeighbor(int n) {
			if(n<0 || n>prevNei.length || n==id) { throw new IllegalArgumentException();}
			if(isNeighbor(n)) return;

			numNei++;
			neiTSS *= udg.verts[n].varSize;

			nextNei[n]=firstNei;//add to start of list
			prevNei[n]=-1;
			if(firstNei!=-1) prevNei[firstNei]=n;
			firstNei=n;
		}

		void removeNeighbor(int n) {
			if(n<0 || n>prevNei.length || n==id) { throw new IllegalArgumentException();}
			if(!isNeighbor(n)) return;

			numNei--;
			neiTSS /= udg.verts[n].varSize;

			int pn = prevNei[n];
			int nn = nextNei[n];

			if(pn==-1) {firstNei=nn;}//first in list (possibly a second in nn, but not necessarily)

			if(pn!=-1 && nn!=-1) { //middle
				nextNei[pn]=nn;
				prevNei[nn]=pn;
			}
			else if(pn!=-1) { //last
				nextNei[pn]=-1;
			}
			else if(nn!=-1) { //first, but there is a second
				prevNei[nn]=-1;
			}

			prevNei[n]=-1;
			nextNei[n]=-1;
		}

		void removeAllNeighbors() {
			numNei=0;
			neiTSS=varSize;

			firstNei = -1;
			Arrays.fill(nextNei, -1);
			Arrays.fill(prevNei, -1);
		}

		public boolean equals(Object o) {
			return var.equals(((Vertex)o).var);
		}

		public int hashCode() {
			return var.hashCode();
		}

		public int compareTo(Object o) {
			Vertex in = (Vertex)o;
			if(eo_score < in.eo_score) { return -1;}
			else if(eo_score > in.eo_score) { return 1;}
			else if(var.equals(in.var)) { return 0;}
			else {
				//Using a ramdom tie breaker instead of the TSS seems to work better in allowing more searching
//				if(neiTSS < in.neiTSS) { return 1;}
//				else if(neiTSS > in.neiTSS) { return -1;}
//				else
				if(eo_score2Random < in.eo_score2Random) { return -1;}
				else if(eo_score2Random > in.eo_score2Random) { return 1;}
				else { throw new IllegalStateException();} //how to distinguish
			}
		}

	}//end class vertex




	static private class CollectionOfTables {

		final MappedList vars;

		final int tmp_varCounts[]; //used for scoring functions, fill as necessary, not maintained
		final int varCounts[]; //variables are ordered in "vars"
				 // once a variable is only in one table, remove it immediately, so having a 1 in this array is invalid
		final int varSize[];
		final VarTable varLists[][]; //[variable][table] (can have nulls in the middle of the lists)

		//when all variables are removed from a table, add the Id to this list to connect up to the root at the end (at a cost of 2)
		//  this could happend with disjointed sections or tables with leaf variables which don't appear elsewhere.
		ArrayList emptyTablesToConnect = new ArrayList(); //list of Integers (Ids of RCNode objects)
		final private RC rc;

		public CollectionOfTables(BeliefNetwork bn) {
			int sizebn = bn.size();
			varCounts = new int[sizebn];
			Arrays.fill(varCounts, 0);
			tmp_varCounts = new int[sizebn];
			varSize = new int[sizebn];
			varLists = new VarTable[sizebn][];
			rc = new RC(sizebn);

			//create a variable ordering
			{
				ArrayList sh = new ArrayList(bn);
				Collections.shuffle(sh);
				vars = new MappedList(sh);
			}

			//Count how many tables each var is in
			for(int i=0; i<sizebn; i++) {
				FiniteVariable fv = (FiniteVariable)vars.get(i);
				varSize[i] = fv.size();

				TableIndex tbi = fv.getCPTShell().index();
				for(int v=0; v<tbi.getNumVariables(); v++) {
					FiniteVariable tmpVar = tbi.variable(v);
					varCounts[vars.indexOf(tmpVar)]++;
				}
			}

			//Allocate varLists
			for(int i=0; i<sizebn; i++) {
				if(varCounts[i]==1) { //1 is not a valid number of tables to be in, remove it immediately
					varCounts[i]=0;
				}
				varLists[i] = new VarTable[varCounts[i]];
			}

			//Create VarTable objects and index them in varLists
			for(int i=0; i<sizebn; i++) {
				FiniteVariable fv = (FiniteVariable)vars.get(i);
				TableIndex tbi = fv.getCPTShell().index();

				VarTable vt = new VarTable(1, new HashMap(tbi.getNumVariables()), rc.nextID);
				boolean emptyTable = true;

				rc.add("L " + rc.nextID + " " + fv.getID() + " cpt");
				rc.nextID++;

				for(int v=0; v<tbi.getNumVariables(); v++) {
					FiniteVariable tmpVar = tbi.variable(v);
					int varID = vars.indexOf(tmpVar);

					if(varCounts[varID]==0) continue;//may only appear here

					int vtindx = 0;
					while(varLists[varID][vtindx]!=null) vtindx++;
					varLists[varID][vtindx] = vt;
					vt.add(new Integer(varID), new Integer(vtindx));
					emptyTable = false;
				}
				if(emptyTable) {
					rc.currentCount+=2; //since this table is already empty include 2 to connect it up to dtree root
					emptyTablesToConnect.add(new Integer(vt.rcID));
				}
			}
		}//end constructor

		static class VarTable {
			HashMap varToIndex; //map of varID(Integer)(1st index in varLists) to Index(Integer)(2nd index in varLists)
			double cpc = -1;
			final int rcID;

			public VarTable(double cpc, HashMap varToIndex, int rcID) {
				this.cpc = cpc;
				this.varToIndex = varToIndex;
				this.rcID = rcID;
			}

			void add(Integer varID, Integer varListsIndx) {
				Object old = varToIndex.put(varID, varListsIndx);
				if(old!=null) {throw new IllegalStateException();}
			}
		}

		private FiniteVariable varInMostTables(Collection exclude) {
			FiniteVariable retFV = null;
			int retVal = 0;
			for(int i=0; i<varCounts.length; i++) {
				if(varCounts[i] > retVal) {
					FiniteVariable obj = (FiniteVariable)vars.get(i);
					if(!exclude.contains(obj)) {
						retFV=obj;
					}
				}
			}
			return retFV;
		}
		private FiniteVariable varInFewestTables(Collection exclude) {
			FiniteVariable retFV = null;
			int retVal = Integer.MAX_VALUE;
			for(int i=0; i<varCounts.length; i++) {
				if(varCounts[i] > 0 && varCounts[i] < retVal) {
					FiniteVariable obj = (FiniteVariable)vars.get(i);
					if(!exclude.contains(obj)) {
						retFV=obj;
					}
				}
			}
			return retFV;
		}

		public double mergeAllTables(FiniteVariable fv) {
			double ret = 0;
			int varID = vars.indexOf(fv);
			if(varCounts[varID]<2) return rc.currentCount;

			int vtIndx=0;
			while(varCounts[varID]>=2) {

				int ti1; VarTable t1;
				int ti2; VarTable t2;

				{
					if(vtIndx==varLists[varID].length) vtIndx=0;
					while(varLists[varID][vtIndx]==null) {
						vtIndx++;
						if(vtIndx==varLists[varID].length) vtIndx=0;
					}
					ti1 = vtIndx;  vtIndx++;
					t1 = varLists[varID][ti1];

					if(vtIndx==varLists[varID].length) vtIndx=0;
					while(varLists[varID][vtIndx]==null) {
						vtIndx++;
						if(vtIndx==varLists[varID].length) vtIndx=0;
					}
					ti2 = vtIndx; vtIndx++;
					t2 = varLists[varID][ti2];
				}


				//calculate cutset & context
				double cutset_use = 1; double cutset_includeHidden = 1;
				double context_use = 1; double context_includeHidden = 1;
				int clusterSize = 0;

				VarTable newVTable = new VarTable( -1, new HashMap((t1.varToIndex.size() + t2.varToIndex.size())), rc.nextID);
				rc.nextID++;

				{
					for(Iterator itr1 = t1.varToIndex.keySet().iterator(); itr1.hasNext();) {
						Integer tmp_v = (Integer)itr1.next();
						int tmp_v_int = tmp_v.intValue();
						boolean hidden = (tmp_v_int<0);
						if(hidden) tmp_v_int = -tmp_v_int-1;

						Integer tmp_vtind = (Integer)t1.varToIndex.get(tmp_v);
						boolean inBoth = t2.varToIndex.containsKey(tmp_v);
						if(inBoth) {
							varCounts[tmp_v_int]--; //if in both, now it will be in 1 less
							if(varCounts[tmp_v_int]==1) {
								varCounts[tmp_v_int]=0;
								varLists[tmp_v_int][tmp_vtind.intValue()] = null;
								varLists[tmp_v_int][((Integer)t2.varToIndex.get(tmp_v)).intValue()] = null;
								cutset_includeHidden *= varSize[tmp_v_int];
								if(!hidden) cutset_use *= varSize[tmp_v_int];
								clusterSize++;
							}
							else {
								varLists[tmp_v_int][((Integer)t2.varToIndex.get(tmp_v)).intValue()] = null; //clear out 2nd one, use 1st for map
							}
						}
						if(varCounts[tmp_v_int]>=2) {
							newVTable.add(tmp_v, tmp_vtind);
							varLists[tmp_v_int][tmp_vtind.intValue()] = newVTable;
							context_includeHidden *= varSize[tmp_v_int];
							if(!hidden) context_use *= varSize[tmp_v_int];
							clusterSize++;
						}
					}
					for(Iterator itr2 = t2.varToIndex.keySet().iterator(); itr2.hasNext();) {
						Integer tmp_v = (Integer)itr2.next();
						if(t1.varToIndex.containsKey(tmp_v)) continue; //already handled

						int tmp_v_int = tmp_v.intValue();
						Integer tmp_vtind = (Integer)t2.varToIndex.get(tmp_v);
						boolean hidden = (tmp_v_int<0);
						if(hidden) tmp_v_int = -tmp_v_int-1;
						if(varCounts[tmp_v_int]>=2) {
							newVTable.add(tmp_v, tmp_vtind);
							varLists[tmp_v_int][tmp_vtind.intValue()] = newVTable;
							context_includeHidden *= varSize[tmp_v_int];
							if(!hidden) context_use *= varSize[tmp_v_int];
							clusterSize++;
						}
					}
				}
				t1.varToIndex.clear();
				t2.varToIndex.clear();

				if(clusterSize > rc.maxCluster) {rc.maxCluster = clusterSize;}

				//determine if caching
				boolean cacheHere = (context_use < largestAcceptableRCCache);

				//setup new varTable
				if(cacheHere) {
					newVTable.cpc = 1; //this makes this inclusive of calls to this node
					rc.currentCount += ((cutset_includeHidden*context_includeHidden)*(t1.cpc + t2.cpc));
					rc.add("I " + newVTable.rcID + " cachetrue " + t1.rcID + " " + t2.rcID);
				}
				else {
					newVTable.cpc = cutset_includeHidden*(t1.cpc + t2.cpc) + 1; //this is calls per call, inclusive of call to it
					rc.add("I " + newVTable.rcID + " cachefalse " + t1.rcID + " " + t2.rcID);
				}

				if(newVTable.varToIndex.size()==0) {
					rc.currentCount += (1+newVTable.cpc); //this table is done, add 1 to connect up this to another part of dtree & make 1 call to it
					emptyTablesToConnect.add(new Integer(newVTable.rcID));
					ret = rc.currentCount;
				}
				else { //if this one isn't empty yet and is the root, can add its costs to those it would be connected to
					ret = rc.currentCount + newVTable.cpc;
				}

			}//while still have 2 tables to connect up
			return ret;
		}//end mergeAllTables

		public void conditionOnVariable(FiniteVariable fv) {
			int varID = vars.indexOf(fv);
			Integer varID_Int = new Integer(varID);
			Integer varID_IntNeg = new Integer(-varID-1);
			if(varCounts[varID]<2) return;

			for(int i=0; i<varLists[varID].length; i++) {
				if(varLists[varID][i]==null) continue;
				VarTable t1 = varLists[varID][i];

				Object idx = t1.varToIndex.remove(varID_Int);
				if(idx==null) throw new IllegalStateException();
				t1.varToIndex.put(varID_IntNeg, idx);
			}
		}


		private class TblLst {
			Collection col;
			double cpc;
			public TblLst(Collection c, double d) {
				col = c;
				cpc = d;
			}
		}

		static class ScoreResults {
			double maxContext;
			double sumContext;
			double rootContext;
			int numTables;
			double sumCounts;
			boolean useImmediately;

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
				return "maxContext: " + maxContext + " sumContext: " + sumContext + " rootContext: " + rootContext + " numTables: " + numTables + " sumCounts: " + sumCounts + " useImmediately: " + useImmediately;
			}
		}
		private ScoreResults scoreResults = new ScoreResults();

		public ScoreResults calcScore1(FiniteVariable fv) {

			scoreResults.clear();

			int varID = vars.indexOf(fv);
			if(varCounts[varID]<2) { scoreResults.useImmediately=true; return scoreResults;} //special case, remove this variable, as it doesn't cost anything

			System.arraycopy(varCounts, 0, tmp_varCounts, 0, varCounts.length);

			LinkedList tablesToConnect = new LinkedList(); //Lists of TblLst objects (Collections of Integer objects (variable ids) & cpc values)

			for(int i=0; i<varLists[varID].length; i++) {
				if(varLists[varID][i]==null) continue;
				tablesToConnect.add( new TblLst(varLists[varID][i].varToIndex.keySet(), varLists[varID][i].cpc));
			}

			while(tablesToConnect.size()>=2) {
				double cutset_use = 1; double cutset_includeHidden = 1;
				double context_use = 1; double context_includeHidden = 1;
				TblLst newTable = new TblLst(new HashSet(), -1);

				TblLst t1 = (TblLst)tablesToConnect.removeFirst();
				TblLst t2 = (TblLst)tablesToConnect.removeFirst();

				for(Iterator itr1 = t1.col.iterator(); itr1.hasNext();) {
					Integer tmp_v = (Integer)itr1.next();
					int tmp_v_int = tmp_v.intValue();
					boolean hidden = (tmp_v_int<0);
					if(hidden) tmp_v_int = -tmp_v_int-1;

					boolean inBoth = t2.col.contains(tmp_v);
					if(inBoth) {
						tmp_varCounts[tmp_v_int]--;
						if(tmp_varCounts[tmp_v_int]==1) {
							tmp_varCounts[tmp_v_int]=0;
							cutset_includeHidden *= varSize[tmp_v_int];
							if(!hidden) cutset_use *= varSize[tmp_v_int];
						}
					}
					if(tmp_varCounts[tmp_v_int]>=2) {
						newTable.col.add(tmp_v);
						context_includeHidden *= varSize[tmp_v_int];
						if(!hidden) context_use *= varSize[tmp_v_int];
					}
				}
				for(Iterator itr2 = t2.col.iterator(); itr2.hasNext();) {
					Integer tmp_v = (Integer)itr2.next();
					if(t1.col.contains(tmp_v)) continue; //already done

					int tmp_v_int = tmp_v.intValue();
					boolean hidden = (tmp_v_int<0);
					if(hidden) tmp_v_int = -tmp_v_int-1;
					if(tmp_varCounts[tmp_v_int]>=2) {
						newTable.col.add(tmp_v);
						context_includeHidden *= varSize[tmp_v_int];
						if(!hidden) context_use *= varSize[tmp_v_int];
					}
				}

				tablesToConnect.add(newTable);

				//determine if caching
				boolean cacheHere = (context_use < largestAcceptableRCCache);


				if(context_use > scoreResults.maxContext) {scoreResults.maxContext=context_use;}
				scoreResults.sumContext+=context_use;
				if(tablesToConnect.size()==1) { scoreResults.rootContext = context_use;}


				if(cacheHere) {
					newTable.cpc=1;
					scoreResults.sumCounts += ((cutset_includeHidden * context_includeHidden) * (t1.cpc + t2.cpc));
				}
				else if(tablesToConnect.size()==1) { //this will be the root, but since it isn't cached penalize it
					//assume parent has same cutset & context & is cached
					newTable.cpc = cutset_includeHidden * (t1.cpc + t2.cpc)+1;
					scoreResults.sumCounts += (cutset_includeHidden * context_includeHidden * (newTable.cpc));
				}
				else { //not cached, but also not the root
					newTable.cpc = cutset_includeHidden * (t1.cpc + t2.cpc)+1;
				}
			}//end while have 2 to connect

			scoreResults.numTables = varCounts[varID];

if(scoreResults.maxContext==0) {scoreResults.maxContext=1;}
if(scoreResults.sumContext==0) {scoreResults.sumContext=1;}
if(scoreResults.rootContext==0) {scoreResults.rootContext=1;}
if(scoreResults.invalid()) { System.err.println(scoreResults.toString()); throw new IllegalStateException(scoreResults.toString());}

			return scoreResults;
		}//end calcScore1

		public RC finishCreatingRC() {

			if(rc.rcScore>=0) throw new IllegalStateException("Already called this");
			if(DEBUG_VERBOSE) testDone();

//TODO
//System.out.println("FinishCreatingRC had " + emptyTablesToConnect.size() + " empty tables: " + emptyTablesToConnect);

			if(emptyTablesToConnect.isEmpty()) { throw new IllegalStateException();} //at the very least, the root becomes empty and is added
			else if(emptyTablesToConnect.size()==1) { //last one added was actually root
				//need to make last internal node the root
				String last = (String)rc.rcObj.remove(rc.rcObj.size()-1);
				rc.add("ROOT" + last.substring(1));
			}
			else {
				int currentRoot = rc.nextID-1; //last one added to RC (should be in emptyTables)
				emptyTablesToConnect.remove(new Integer(currentRoot));

				while(emptyTablesToConnect.size()>1) {
					Integer i_rem = (Integer)emptyTablesToConnect.remove(emptyTablesToConnect.size()-1);
					rc.add("I " + rc.nextID + " cachefalse " + currentRoot + " " + i_rem.intValue());
					currentRoot = rc.nextID;
					rc.nextID++;
				}
				Integer i_rem = (Integer)emptyTablesToConnect.remove(emptyTablesToConnect.size()-1);
				rc.add("ROOT " + rc.nextID + " cachefalse " + currentRoot + " " + i_rem.intValue());
				currentRoot = rc.nextID;
				rc.nextID++;
			}

			rc.currentCount-=1; //two empty tables are added plus 1 each, but those 2 are incorrect, however there is 1 call to the root
			rc.rcScore = rc.currentCount;

			//cleanup
			vars.clear();
			return rc;
		}


		protected void testDone() {
			for(int i=0; i<varLists.length; i++) {
				for(int j=0; j<varLists[i].length; j++) {
					if(varLists[i][j] != null) throw new IllegalStateException("Not all tables have been removed");
				}
				if(varCounts[i]!=0) throw new IllegalStateException("Not all variables are listed as done");
			}
		}

		public void simplifyFromElimOrd(ArrayList eo) {
			for(int i=0; i<eo.size(); i++) {
				FiniteVariable fv = (FiniteVariable)eo.get(i);
				mergeAllTables(fv);
			}
		}


	}//end class CollectionOfTables


}//end class EOGenerator
