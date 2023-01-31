//TODO Code for changing variables is not finished yet

package edu.ucla.belief.rc2.tools;

import java.util.*;
import java.io.*;
import java.text.*;
//{superfluous} import java.math.BigInteger;

import edu.ucla.util.JVMProfiler;
import edu.ucla.util.DblArrays;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.io.geneticlinkage.*;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.RCGenerator;
import edu.ucla.belief.Table;

import edu.ucla.belief.rc2.structure.RC2;
import edu.ucla.belief.rc2.structure.RC2Utils;
import edu.ucla.belief.rc2.caching.RC2CachingScheme_Full;
import edu.ucla.belief.rc2.caching.RC2CachingScheme_Greedy;
import edu.ucla.belief.rc2.caching.RC2CachingSchemeUtils;
import edu.ucla.belief.rc2.caching.RC2CachingScheme_Collection;
import edu.ucla.belief.rc2.creation.RC2CreatorFile;
import edu.ucla.belief.rc2.io.RC2WriteToFile;


public class RC_Link {

	static final String programTitle = "RC_Link";
	static final String programVersion = "2.0.1";

//TODO SET FLAGS (move some to command line)
	boolean runFast = true;
//TODO SET FLAGS

	CmdLn cmdln = null;

	MultipleOutputs stream_All = new MultipleOutputs();
	MultipleOutputs stream_Time = new MultipleOutputs();

	long time_cpu[] = new long[20];
	long time_sys[] = new long[20];

    static NumberFormat nfDbl;
    {
		nfDbl = NumberFormat.getInstance();
		nfDbl.setMaximumFractionDigits(7);
	}
    static NumberFormat nfLng;
    {
		nfLng = NumberFormat.getIntegerInstance();
	}



	public RC_Link(CmdLn cmdln) {
		this.cmdln = cmdln;
	}

	public static void main(String args[]) {
		if(CmdLn.containsHelp(args)) {
			System.out.println(CmdLn.cmdlnMsg);
		}
		else {
			try{
				RC_Link prog = new RC_Link(new CmdLn(args));
				prog.start();
			}
			catch(IllegalArgumentException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	void start() {
		long totalTime = 0;

		PrintStream outDetails;
		PrintStream outRCInfo;
		PrintStream outResults;


		try{
			outDetails = new PrintStream(new BufferedOutputStream(new FileOutputStream(cmdln.outDir+File.separator+"details.txt")),true);

			stream_All.add(System.out);
			stream_All.add(outDetails);

			stream_Time.add(outDetails);
//			stream_Time.add(new PrintStream(new BufferedOutputStream(new FileOutputStream(cmdln.outDir+File.separator+"times-"+cmdln.method+"_"+cmdln.sat_encoding+".txt")),true));

			{
				File fileTmp = new File("rcInfo.tsv");
				boolean exists = fileTmp.exists();
				outRCInfo = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileTmp, /*append*/true)),/*autoFlush*/true);
				if(!exists) {
					outRCInfo.println("\t"       +"\t"      +"Full Caching\t\t"+"Greedy Caching\t\t"+"Max MB\t" +"Nodes\t\t\t\t"            );
					outRCInfo.println("Network\t"+"Method\t"+"MB\t"+"Calls\t"  +"MB\t"+"Calls\t"    +"Allowed\t"+"Too Large\t"+"Worthless\t"+"Full\t"+"Greedy\t"+"Timings:");
				}
				if (cmdln.locus != null) { outRCInfo.print(cmdln.locus.getName() + "\t"); }
				else if (cmdln.fBN != null) { outRCInfo.print(cmdln.fBN + "\t"); }
				else { outRCInfo.print("\t"); }
				outRCInfo.print(cmdln.method);
			}

			{
				File fileTmp = new File("results.tsv");
				boolean exists = fileTmp.exists();
				outResults = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileTmp, /*append*/true)),/*autoFlush*/true);
				if(!exists) outResults.println("Network\tln(likelihood)\tln(likelihood)\tLOD");
				if (cmdln.locus != null) { outResults.print(cmdln.locus.getName()); }
				else if (cmdln.fBN != null) { outResults.print(cmdln.fBN.toString()); }
				else { outResults.print("no file name"); }
			}
		}
		catch(IOException e) {
			System.err.println("Unrecoverable Error creating output streams\n" + e.getMessage());
			return;
		}




        println(stream_All, "");
        println(stream_All, "----------");
        println(stream_All, "running " + programTitle + " " + programVersion);
        println(stream_All, "" + new Date());
        println(stream_All, "Command-Line: " + cmdln);
        println(stream_All, "----------");


		try
		{
			Pedigree.GeneticNetwork gn;
			Loci loci = null;
			HashSet changingSValues = null;

			String locusName = null;


			if (cmdln.fBN != null)
			{
				markTime(0);
				BeliefNetwork bn = NetworkIO.read(cmdln.fBN);
				HashMap evid = readEvid(cmdln.fEvid);
				if (cmdln.fChanging != null) changingSValues = readChanging(cmdln.fChanging);

				locusName = cmdln.fBN.getName();

				markTime(1);
				displayTime(0, 1, "Read in Pedigree");

				Pedigree ped = Pedigree.createPedigree(bn, evid);
				gn = ped.createBeliefNetwork(cmdln.scalar);

				markTime(2);
				displayTime(1, 2, "Pedigree preprocess & Convert to Belief Network");
			}
			else
			{
				markTime(0);
				{
					loci = Loci.readLoci(cmdln.locus.getPath());

					locusName = cmdln.locus.getName();

					Pedigree ped = Pedigree.createPedigree(cmdln.pedigree.getPath(), loci);

					markTime(1);
					displayTime(0, 1, "Pedigree preprocess");

					gn = ped.createBeliefNetwork(cmdln.scalar);

					markTime(2);
					displayTime(1, 2, "Convert to Belief Network");
				}
			}


					if (cmdln.method.equalsIgnoreCase("Generate-Net") || cmdln.generateTestFiles)
					{
						File tmpnetfile = new File(cmdln.outDir + File.separator + "network_" + locusName + ".net");
						NetworkIO.writeNetwork(gn.bn, tmpnetfile);
						println(stream_All, "Generated file: " + tmpnetfile);
						{
							PrintStream outConst = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(cmdln.outDir + File.separator + "constant_" + locusName + ".txt"))), true);
							outConst.println("Constant (scaled): \t" + gn.constantScaled());
							outConst.println("Scalar: \t" + gn.scalar);
							outConst.flush();
							outConst.close();
						}

					}

					if (cmdln.generateTestFiles)
					{

						if (Pedigree.PREPROCESS != Pedigree.ALL)
						{
							PrintStream outEvid = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(cmdln.outDir + File.separator + "X.inst"))), true);
							gn.writeEvidToFile(outEvid);
							outEvid.flush();
							outEvid.close();
							outEvid = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(cmdln.outDir + File.separator + "X_All.inst"))), true);
							gn.writeEvidToFile_All(outEvid);
							outEvid.flush();
							outEvid.close();
						}

						gn.outputChangingVariables(new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(cmdln.outDir + File.separator + "thetas.list"))), true));
						{
							Set pre = gn.equivPreProcessVars();
							if (pre.size() > 0)
							{
								PrintStream outPre = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(cmdln.outDir + File.separator + "preprocess.txt"))), true);
								for (Iterator itr = pre.iterator(); itr.hasNext(); )
								{
									outPre.println("" + itr.next());
								}
								outPre.flush();
								outPre.close();
							}
						}
					}
//				}
				markTime(3);
				displayTime(2, 3, "Output Test Files");

				RC2 rcBest = null;

				if (cmdln.method.equalsIgnoreCase("Generate-Net")) { }
				else
				{//create RC object

					RC2.RCCreationParams param = new RC2.RCCreationParams(gn.bn);
					{
						if (cmdln.method.equalsIgnoreCase("RC-SAT") ||
							cmdln.method.equalsIgnoreCase("RC-KB"))
						{
							param.allowPartialDerivatives = false;
						}
						else
						{
							param.allowPartialDerivatives = true; //all evidence nodes become leafs, therefore, iterateAll is better than iterateSkp.
						}

						param.allowKBinst = cmdln.method.equalsIgnoreCase("RC-KB");
						param.useKBinst = param.allowKBinst;
						param.outputConsole = new PrintWriter(outDetails, true);
						param.scalar = gn.scalar;

						if (cmdln.method.equalsIgnoreCase("RC-SAT"))
						{

							//currently only have "L" implemented
							param.allowKBsat = 1;

							//						if(cmdln.sat_encoding.equalsIgnoreCase("L")) {
							//							param.allowKBsat = 1;
							//						}
							//						else {
							//							println(stream_All, "Unknown encoding: " + cmdln.sat_encoding);
							//						}
						}
						else
						{
							param.allowKBsat = 0;
						}
					}


					try
					{
						//					if(cmdln.loadRC!=null) {//load RC
						//						rcBest = RC2CreatorFile.readRC(param, new RC2CreatorFile.Params(gn.bn, cmdln.loadRC.getPath()));
						//						markTime(5);
						//						displayTime(3,5,"Load RC from file");
						//						println(stream_All, rcBest.compStats().toString());
						//					}
						//					else {//create & save RC using RCGenerator}
						{

							Collection cachedNodesSearch = new HashSet();
							//for constrained ordering, weight search slightly toward non-constrained by adding multiple nulls
							RCGenerator.RC rcTmp = RCGenerator.generateRC(gn.bn, locusName, "rcSearch_detailed.tsv", cmdln.extendedSearch, gn.equivPreProcessVars(), new Set[][] { null, gn.locusOrdering(), gn.familyOrdering(), gn.reverseLocusOrdering(), gn.dynamicBNOrderingVEPD() });
							{
								File tmprc = File.createTempFile("rcFile", null, new File("."));
								Writer rcOut = new BufferedWriter(new FileWriter(tmprc));
								rcTmp.writeFile(rcOut);
								rcOut.flush();
								rcOut.close();
								rcOut = null;

								//could cause memory trouble
								rcBest = RC2CreatorFile.readRC(param, new RC2CreatorFile.Params(gn.bn, tmprc.getPath()), cachedNodesSearch);

								tmprc.delete();
								tmprc = null;
							}

							if (!runFast && rcBest != null)
							{ //output a few extra stats

								println(stream_All, "\nRC from the search (orig): \n  " +
											cachedNodesSearch.size() + " nodes were cached.\n  " +
											RC2Utils.expectedRCCalls_Pe(rcBest, cachedNodesSearch) + " rcCalls\n  " +
											(RC2CachingSchemeUtils.expectedMemoryUsage(cachedNodesSearch) * 8 / 1048576) + "MB");


								RC2CachingSchemeUtils.removeLargeCaches(rcBest, cachedNodesSearch);
								RC2CachingSchemeUtils.removeWorthlessCaches(rcBest, cachedNodesSearch);

								println(stream_All, "\nRC from the search (cleaned-up): \n  " +
											cachedNodesSearch.size() + " nodes were cached.\n  " +
											RC2Utils.expectedRCCalls_Pe(rcBest, cachedNodesSearch) + " rcCalls\n  " +
											(RC2CachingSchemeUtils.expectedMemoryUsage(cachedNodesSearch) * 8 / 1048576) + "MB\n");
							}

							markTime(4);
							displayTime(3, 4, "Search for and Create RC");

							if (rcBest != null)
							{//allocate memory

								long mm = Runtime.getRuntime().maxMemory();

								println(stream_All, "\nBefore Caching: max Mem:" + mm + " MB: " + mm / 1048576.0);

								double factor = .9;
								long ce = (long)Math.floor(mm / 8 * factor); //(max memory / 8)=max number of caches, but leave some room for overhead

								println(stream_All, "Allow Caching of up to " + ce + " entries (MB: " + ce * 8 / 1048576.0 + ")");

								Collection cachedNodes; String cachedName;


								if (rcBest.compStats().fullCaching().memUsed() <= ce)
								{
									cachedName = "Full Caching";
									cachedNodes = new RC2CachingScheme_Full().getCachingScheme(rcBest);
								}
								else
								{
									cachedName = "Greedy Caching";
									cachedNodes = new RC2CachingScheme_Greedy(ce).getCachingScheme(rcBest);
								}

								if (!cmdln.method.equalsIgnoreCase("Generate-RC"))
								{
									rcBest.setCachingScheme(new RC2CachingScheme_Collection(cachedName, cachedNodes));
								}

								println(stream_All, "\n" + rcBest.compStats().toString());

								if (cmdln.generateTestFiles)
								{
									PrintWriter rcOut = new PrintWriter(new BufferedWriter(new FileWriter(cmdln.outDir + File.separator + "net_rc.rc")));
									RC2WriteToFile.writeRC(rcBest, rcOut);
									rcOut.flush();
									rcOut.close();
								}

								int numTooLarge;
								int numWorthless;
								{
									Collection all = RC2Utils.getSetOfAllNonLeafNodes(rcBest);
									int numAll = all.size();
									RC2CachingSchemeUtils.removeLargeCaches(rcBest, all);
									numTooLarge = numAll - all.size();
									numWorthless = numAll - rcBest.compStats().fullCaching().numNodesCached();
								}

								outRCInfo.print("\t" + rcBest.compStats().fullCaching().memUsedMB() + "\t" +
													rcBest.compStats().fullCaching().rcCalls_pe() + "\t" +
													(RC2CachingSchemeUtils.expectedMemoryUsage(cachedNodes) * 8.0 / 1048576.0) + "\t" +
													RC2Utils.expectedRCCalls_Pe(rcBest, cachedNodes) + "\t" +
													(mm / 1048576.0) + "\t" +
													numTooLarge + "\t" +
													numWorthless + "\t" +
													rcBest.compStats().fullCaching().numNodesCached() + "\t" +
													cachedNodes.size());
							}

							markTime(5);
							displayTime(4, 5, "Allocate caches");
						}
					}
					catch (Exception e)
					{
						rcBest = null;
						println(stream_All, "Error Creating Dtree: " + e.toString());
						e.printStackTrace();
						return;
					}

					if (rcBest != null) rcBest.synchEvidWithBN();

					markTime(6);
					displayTime(5, 6, "Synch With BN Evid");
				}//end create RC

				//display some stats
				if (rcBest != null && runFast == false)
				{//this is only for debug and informational purposes
					RC2Utils.MiscStats miscStats = RC2Utils.computeStatsDtree(rcBest);
					println(stream_All, "\n\n\n" + miscStats + "\n");
				}

				markTime(7);

				if (cmdln.method.equalsIgnoreCase("Generate-Net")) { }
				else
				{

					RC_Results resHalf = null;
					//iteration 0 will be for recom = 0.5
					//iteration 1 will be for what is in the file
					//later ones will be increments of this

					double recom1m = -1;
					double recom1f = -1;
					double recom2m = -1;
					double recom2f = -1;
					double inc = -1;
					double max = -1;
					double thetaAC = 0.5;

					long timeComp1 = 0;
					{
						if (loci == null)
						{
							inc = Integer.MAX_VALUE; //set later
							max = 0;
						}
						else if (loci.header().program == 5)
						{
							inc = ((Loci.Footer.Mlink)loci.footer().progData).inc;
							max = ((Loci.Footer.Mlink)loci.footer().progData).finishedValue;
						}
						else if (loci.header().program == 4)
						{
							inc = 0; //set later
							max = ((Loci.Footer.Linkmap)loci.footer().progData).finishedValue;
						}
						else throw new IllegalStateException("\nUnknown program type " + loci.header().program + " in loci file.");
					}

					for (int itr = 0; ; itr++)
					{
						markTime(8);
						String desc = null;
						if(loci==null)//no updating of any S values
						{
							if (changingSValues == null && itr > 0) break;
							if (changingSValues != null && itr > 1) break;

							if (changingSValues != null && itr == 1)
							{
								for (Iterator itrc = changingSValues.iterator(); itrc.hasNext(); )
								{
									String varName = (String)itrc.next();
									FiniteVariable fv = (FiniteVariable)gn.bn.forID(varName);
									if (fv == null) throw new IllegalStateException("Could not find: " + varName);
									Table t = fv.getCPTShell().getCPT();
									t.setCP(0, 0.5);
									t.setCP(1, 0.5);
									t.setCP(2, 0.5);
									t.setCP(3, 0.5);
									rcBest.setCPT(fv, false);
								}
							}
						}
						else if (loci.header().program == 5 ||
							(loci.header().program == 4 && loci.getChromoOrd_ChangingS2() < 0))
						{ //External interval (left or right end)

							//TODO Superlink seems to only ever do one iteration?, so I will do that also
							if (itr == 0 && loci.header().program == 4) inc = max + 1;


							boolean changedDeterminism = (recom1m == 0.0 || recom1m == 1.0 || recom1f == 0.0 || recom1f == 1.0); //changed from deterministic

							if (itr == 0) { recom1m = 0.5; recom1f = 0.5; }
							else if (itr == 1)
							{
								recom1m = loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS1() - 1)]; //initial from file
								recom1f = loci.footer().recombinationValues_female[(loci.getChromoOrd_ChangingS1() - 1)]; //initial from file
							}
							else
							{
								recom1m += inc; //increment
								recom1f = 0.5 * (1 - (Math.pow((1 - 2 * recom1m), loci.footer().sexDifference)));
							}

							if (itr > 1 && recom1m > max) break;
							{
								double t1 = loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS1() - 1)];
								double t2 = loci.footer().recombinationValues_female[(loci.getChromoOrd_ChangingS1() - 1)];
								loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS1() - 1)] = recom1m;
								loci.footer().recombinationValues_female[(loci.getChromoOrd_ChangingS1() - 1)] = recom1f;

								desc = "" + recom1m;
								if (loci.footer().recombinationValues_male == loci.footer().recombinationValues_female)
								{
									println(stream_All, "\nRecomValues: " + DblArrays.convertToString(loci.footer().recombinationValues_male));
								}
								else
								{
									println(stream_All, "\nRecomValues (male): " + DblArrays.convertToString(loci.footer().recombinationValues_male));
									println(stream_All, "RecomValues (female): " + DblArrays.convertToString(loci.footer().recombinationValues_female));
								}
								if (itr == 0)
								{
									loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS1() - 1)] = t1;
									loci.footer().recombinationValues_female[(loci.getChromoOrd_ChangingS1() - 1)] = t2;
								}
							}

							changedDeterminism = changedDeterminism || (recom1m == 0.0 || recom1m == 1.0 || recom1f == 0.0 || recom1f == 1.0); //changed to deterministic

							//update cpt tables for s variables
							Collection updatedVars = gn.changeRecomS1(recom1m, recom1f);
							for (Iterator itrv = updatedVars.iterator(); itrv.hasNext(); )
							{
								rcBest.setCPT((FiniteVariable)itrv.next(), changedDeterminism);
							}
						}
						else if (loci.header().program == 4 && loci.getChromoOrd_ChangingS2() >= 0)
						{ //Internal interval
							{
								double thetaAB = loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS1() - 1)];
								double thetaBC = loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS2() - 1)];
								thetaAC = thetaAB + thetaBC - (2 * thetaAB * thetaAC);
								inc = thetaAC / ((Loci.Footer.Linkmap)loci.footer().progData).numEvals;
								if (max > thetaAC) max = thetaAC;
							}

							boolean changedDeterminism1 = (recom1m == 0.0 || recom1m == 1.0 || recom1f == 0.0 || recom1f == 1.0); //changed from deterministic
							boolean changedDeterminism2 = (recom2m == 0.0 || recom2m == 1.0 || recom2f == 0.0 || recom2f == 1.0);

							if (itr == 0) { recom1m = 0.5; recom2m = thetaAC; }
							else if (itr == 1)
							{
								recom1m = loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS1() - 1)]; //initial from file
								recom2m = (thetaAC - recom1m) / (1 - 2 * recom1m); //keep thetaAC constant & vary recom2
							}
							else
							{
								recom1m += inc; //increment
								recom2m = (thetaAC - recom1m) / (1 - 2 * recom1m); //keep thetaAC constant & vary recom2
							}

							if (itr > 1 && recom1m > max) break;

							recom1f = 0.5 * (1 - (Math.pow((1 - 2 * recom1m), loci.footer().sexDifference)));
							recom2f = 0.5 * (1 - (Math.pow((1 - 2 * recom2m), loci.footer().sexDifference)));
							{
								double t1 = loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS1() - 1)];
								double t2 = loci.footer().recombinationValues_female[(loci.getChromoOrd_ChangingS1() - 1)];
								double t3 = loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS2() - 1)];
								double t4 = loci.footer().recombinationValues_female[(loci.getChromoOrd_ChangingS2() - 1)];

								loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS1() - 1)] = recom1m;
								loci.footer().recombinationValues_female[(loci.getChromoOrd_ChangingS1() - 1)] = recom1f;
								loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS2() - 1)] = recom2m;
								loci.footer().recombinationValues_female[(loci.getChromoOrd_ChangingS2() - 1)] = recom2f;


								desc = recom1m + " and " + recom2m;
								if (loci.footer().recombinationValues_male == loci.footer().recombinationValues_female)
								{
									println(stream_All, "\nRecomValues: " + DblArrays.convertToString(loci.footer().recombinationValues_male));
								}
								else
								{
									println(stream_All, "\nRecomValues (male): " + DblArrays.convertToString(loci.footer().recombinationValues_male));
									println(stream_All, "RecomValues (female): " + DblArrays.convertToString(loci.footer().recombinationValues_female));
								}

								if (itr == 0)
								{
									loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS1() - 1)] = t1;
									loci.footer().recombinationValues_female[(loci.getChromoOrd_ChangingS1() - 1)] = t2;
									loci.footer().recombinationValues_male[(loci.getChromoOrd_ChangingS2() - 1)] = t3;
									loci.footer().recombinationValues_female[(loci.getChromoOrd_ChangingS2() - 1)] = t4;
								}
							}

							changedDeterminism1 = changedDeterminism1 || (recom1m == 0.0 || recom1m == 1.0 || recom1f == 0.0 || recom1f == 1.0); //changed to deterministic
							changedDeterminism2 = changedDeterminism2 || (recom2m == 0.0 || recom2m == 1.0 || recom2f == 0.0 || recom2f == 1.0); //changed to deterministic

							//update cpt tables for s variables
							Collection updatedVars = gn.changeRecomS1(recom1m, recom1f);
							for (Iterator itrv = updatedVars.iterator(); itrv.hasNext(); )
							{
								rcBest.setCPT((FiniteVariable)itrv.next(), changedDeterminism1);
							}
							updatedVars = gn.changeRecomS2(recom2m, recom2f);
							for (Iterator itrv = updatedVars.iterator(); itrv.hasNext(); )
							{
								rcBest.setCPT((FiniteVariable)itrv.next(), changedDeterminism2);
							}


						}

						markTime(9);
						timeComp1 += displayTime(8, 9, "Update S values");

						RC_Results resCurrent;
						double res = compute(rcBest);
						resCurrent = new RC_Results(res, rcBest, gn.constantScaled());
						println(System.out, "\nFor recombination factor of " + desc + " the results are:\n" + resCurrent.toString());
						println(outDetails, "\nFor recombination factor of " + desc + " the results are:\n" + resCurrent.toStringLong());
						if (itr == 0)
						{
							resHalf = resCurrent;
							outResults.print("\t" + nfDbl.format(resCurrent.ln_pe));
						}
						else
						{
							String lod = nfDbl.format(RC_Results.LOD(resCurrent, resHalf));
							println(stream_All, "\nLOD Score = " + lod);
							outResults.print("\t" + nfDbl.format(resCurrent.ln_pe) + "\t" + lod);
						}

						markTime(10);

						long timeThis = displayTime(9, 10, "Compute score for recombination of " + desc);
						timeComp1 += timeThis;

						outRCInfo.print("\tRun " + itr + ":\t" + timeThis + "\t" + resCurrent.calls);
					}
					outRCInfo.print("\tComputation Time:\t" + timeComp1);
				}

				//final output
				markTime(11);
				totalTime = displayTime(0, 11, "Total Time");
				outRCInfo.print("\tTotal Time:\t" + totalTime);

				if (rcBest != null) rcBest.close();
//			}
		}
		catch (Exception e)
		{
			System.gc(); //possible memory error
			println(stream_All, "\n\n" + programTitle + " caught the exception:\n" + e.getMessage());
			e.printStackTrace(outDetails);
		}
		outResults.println("");
		outRCInfo.println("");
		println(stream_All, "\nTotal Time used: " + (totalTime/1000.0) + " (sec)");
		println(stream_All, "\n\n" + programTitle + " " + programVersion + " by Allen and Darwiche (2004)\n\n");
	}


	private double compute(RC2 rc) {
		if(cmdln.method.equalsIgnoreCase("RC-SAT")) {
			return rc.compute_Pe_sat();
		}
		else {
			return rc.compute_Pe();
		}
	}


	private void markTime(int indx) {
		time_cpu[indx] = JVMProfiler.getCurrentThreadCpuTimeMS();
		time_sys[indx] = System.currentTimeMillis();
	}
	private long displayTime(int st, int en, String desc) {
		long cpu = time_cpu[en] - time_cpu[st];
		long sys = time_sys[en] - time_sys[st];
		long ret = sys;
		println(stream_Time, "Timing for: " + desc);
		println(stream_Time, "\t" + nfLng.format(sys) + "\t (ms) System Elapsed Time.");
		if(cpu > 0) {
			println(stream_Time, "\t" + nfLng.format(cpu) + "\t (ms) CPU Time (if using profiler).");
			ret = cpu;
		}
		return ret;
	}





	static public class RC_Results {
		public static final double MB = 8.0 / 1048576.0; //8.0 for double, 1048576 for Byte->MB (2^20)

		double pe_unscaled;
		double pe;
		double ln_pe;
		double log_pe;
		double calls; //calls to RC       (USED during computation)
		double constant;


		public RC_Results( double pe_unscaled, RC2 rc, double constant) {
			setPe_unscaled( pe_unscaled, rc, constant);
			if( rc != null) {
				calls = rc.rcCallsCounter;
			}
		}

		private void setPe_unscaled( double in, RC2 rc, double constant) {
			pe_unscaled = in;
			this.constant = constant;
			if( in == -1 || rc == null) {
				pe = -1;
				ln_pe = -1;
				log_pe = -1;
			}
			else {
				pe = Math.pow(in*constant, rc.scalar);

				ln_pe = rc.scalar * Math.log(in*constant);
				log_pe = ln_pe / Math.log(10);
			}
		}

		public String toString() {
			StringBuffer ret = new StringBuffer();
			ret.append("ln(likelihood)       : " + nfDbl.format(ln_pe) + "\n");
			ret.append("log(likelihood)      : " + nfDbl.format(log_pe) + "\n");
			ret.append("-2*ln(likelihood)    : " + nfDbl.format(-2.0*ln_pe) + "\n");
			ret.append("RC Calls             : " + calls);
			return ret.toString();
		}

		public String toStringLong() {
			StringBuffer ret = new StringBuffer();
			ret.append("ln(likelihood)       : " + nfDbl.format(ln_pe) + "\n");
			ret.append("log(likelihood)      : " + nfDbl.format(log_pe) + "\n");
			ret.append("-2*ln(likelihood)    : " + nfDbl.format(-2.0*ln_pe) + "\n");
			ret.append("pe (unscaled): " + nfDbl.format(pe_unscaled) + "   " + pe_unscaled + "\n");
			ret.append("constant: " + constant + "\n");
			ret.append("likelihood           : " + nfDbl.format(pe) + "   " + pe + "\n");
			ret.append("RC Calls             : " + calls);
			return ret.toString();
		}

		static public double LOD( RC_Results res, RC_Results resHalf) {
			return res.log_pe - resHalf.log_pe;
		}
	}//end class RC_Results






	/**  This class parses the command line.
	 *
	 *  <p>Possible Options are:
	 *  <ul><b>Required</b>
	 *   <li> method: "RC" (default), "RC-KB", "RC-RSAT", "Generate-Net"
	 *   <li> locus: [file]
	 *   <li> pedigree: [file]
	 *  </ul>
	 *  <ul><b>Optional</b>
	 *   <li> outDir: [dir] //default to current
	 *   <li> scalar: [integer] (if not present will use 6)
	 *   <li> help
	 *   <li> extendedSearch
	 *   <li> generateTestFiles
	 *  </ul>
	 */
	static private class CmdLn {

		public final static String cmdlnMsg =
								"The command-line format for " + programTitle + " " + programVersion + " is:\n" +
								"java -cp rc_link.jar edu.ucla.belief.rc2.tools.RC_Link [options] Locus [file] Pedigree [file]\n" +
								"  Where [options] can contain:\n"+
								"    Help\n" +
								"    Method Generate-Net\n" +
								"    Scalar [integer greater than 6]\n" +
								"    ExtendedSearch\n" +
								"  Please see the manual for more details\n";

		String local_args[];

		String method = "RC";
		File locus;
		File pedigree;
		File fBN;
		File fEvid;
		File fChanging;

		File outDir = new File(".");
		int scalar = 6;

		boolean extendedSearch = false;
		boolean generateTestFiles = false;

		static public boolean containsHelp(String[] args) {
			for(int i=0; i<args.length; i++) {
				if(args[i].equalsIgnoreCase("help")) return true;
			}
			return false;
		}

		public CmdLn( String[] args) {
			if( !parse( args)) { throw new IllegalArgumentException("ERROR: Could not parse command line\n\n"+cmdlnMsg);}
		}

		public String toString() {
			if( local_args != null) { return Arrays.asList( local_args).toString();}
			else { return "[null]";}
		}

		/** Will parse the command line and return true if it was valid and false if there were any errors.*/
		public boolean parse( String[] args) {

			local_args = args;
			int i=0;

			try {

			for(i=0; i<args.length; i++) {
				if(args[i].equalsIgnoreCase("method:") || args[i].equalsIgnoreCase("method")) {
					i++;
					method = args[i];
				}
				else if(args[i].equalsIgnoreCase("locus:") || args[i].equalsIgnoreCase("locus")) {
					i++;
					locus = new File(args[i]);
				}
				else if(args[i].equalsIgnoreCase("pedigree:") || args[i].equalsIgnoreCase("pedigree")) {
					i++;
					pedigree = new File(args[i]);
				}
				else if (args[i].equalsIgnoreCase("BN:") || args[i].equalsIgnoreCase("BN"))
				{
					i++;
					fBN = new File(args[i]);
				}
				else if (args[i].equalsIgnoreCase("Evid:") || args[i].equalsIgnoreCase("Evid"))
				{
					i++;
					fEvid = new File(args[i]);
				}
				else if (args[i].equalsIgnoreCase("Changing:") || args[i].equalsIgnoreCase("Changing"))
				{
					i++;
					fChanging = new File(args[i]);
				}
				else if (args[i].equalsIgnoreCase("outDir:") || args[i].equalsIgnoreCase("outDir"))
				{
					i++;
					outDir = new File(args[i]);
				}
				else if(args[i].equalsIgnoreCase("scalar:") || args[i].equalsIgnoreCase("scalar")) {
					i++;
					scalar = Integer.parseInt(args[i]);
				}
				else if(args[i].equalsIgnoreCase("ExtendedSearch")) {
					extendedSearch=true;
				}
				else if(args[i].equalsIgnoreCase("GenerateTestFiles")) {
					generateTestFiles=true;
				}
				else
				{
					System.err.println("\nERROR: Unknown command line argument: " + args[i]);
					return false;
				}
			}

			}
			catch(Exception e) {
				System.err.println("\nERROR: Could not parse command line: " + args[i]);
				return false;
			}


			return verifyValid();
		}//end parse


		private boolean verifyValid() {
			if(method==null || !(method.equalsIgnoreCase("RC") ||
								 method.equalsIgnoreCase("RC-KB") ||
								 method.equalsIgnoreCase("RC-SAT") ||
								 method.equalsIgnoreCase("Generate-Net"))) {
				System.out.println("\nERROR (Command Line): Invalid method: " + method);
				return false;
			}
			if (locus == null && pedigree == null && fBN != null && fEvid != null)
			{
				if (!fBN.exists())
				{
					System.out.println("\nERROR (Command Line): Invalid BN file: " + fBN);
					return false;
				}
				if (!fEvid.exists())
				{
					System.out.println("\nERROR (Command Line): Invalid Evid file: " + fEvid);
					return false;
				}
			}
			else
			{
				if (locus == null || !locus.exists())
				{
					System.out.println("\nERROR (Command Line): Invalid locus file: " + locus);
					return false;
				}
				if (pedigree == null || !pedigree.exists())
				{
					System.out.println("\nERROR (Command Line): Invalid pedigree file: " + pedigree);
					return false;
				}
			}
			if(scalar<=0) {
				System.out.println("\nERROR (Command Line): Invalid scalar: " + scalar);
				return false;
			}
			return true;
		}//end verifyValid
	}//end class CmdLn




	private void println(MultipleOutputs out, String str) {
		out.println(str);
	}
	private void println(PrintWriter out, String str) {
		out.println(str);
	}
	private void println(PrintStream out, String str) {
		out.println(str);
	}

	private class MultipleOutputs {
		ArrayList outStreams = new ArrayList();
		ArrayList outWriters = new ArrayList();

		public MultipleOutputs() {}

		public void add(PrintStream out) {
			if(out!=null) { outStreams.add(out);}
		}
		public void add(PrintWriter out) {
			if(out!=null) { outWriters.add(out);}
		}

		public void println(String str) {
			for(int i=0; i<outStreams.size(); i++) {
				((PrintStream)outStreams.get(i)).println(str);
			}
			for(int i=0; i<outWriters.size(); i++) {
				((PrintWriter)outWriters.get(i)).println(str);
			}
		}
	}

	HashMap readEvid(File inf)
	{
		HashMap ret = new HashMap();
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(inf));
			String ln = in.readLine();
			while (ln != null)
			{
				if (ln.length() > 0)
				{
					//parse
					if (ln.substring(0, 5).equals("<?xml")) { }
					else if (ln.substring(0, 14).equals("<instantiation")) { }
					else if (ln.substring(0, 15).equals("</instantiation")) { }
					else if (ln.substring(0, 9).equals("<inst id="))
					{
						ln = ln.substring(10);//remove first part
						int i = ln.indexOf('"');//end of var name
						String s1 = ln.substring(0, i);
						ln = ln.substring(i + 1);
						i = ln.indexOf('"');//first part of value
						ln = ln.substring(i + 1);
						i = ln.indexOf('"');//end of value
						String s2 = ln.substring(0, i);
						ret.put(s1, s2);
					}
					else throw new IllegalStateException();
				}
				ln = in.readLine();
			}
		}
		catch (Exception e)
		{
			println(stream_All, "ERROR: " + e.toString());
			ret = null;
		}
		return ret;
	}

	HashSet readChanging(File inf)
	{
		HashSet ret = new HashSet();
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(inf));
			String ln = in.readLine();
			while (ln != null)
			{
				if (ln.length() > 0)
				{
					ret.add(ln);
				}
				ln = in.readLine();
			}
		}
		catch (Exception e)
		{
			println(stream_All, "ERROR: " + e.toString());
			ret = null;
		}
		return ret;
	}
}


