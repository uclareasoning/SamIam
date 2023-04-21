package edu.ucla.belief.rc2.tools;

import java.util.*;
import java.io.*;

import edu.ucla.belief.*;
import edu.ucla.belief.io.geneticlinkage.*;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.io.xmlbif.*;
import edu.ucla.belief.uai2006.*;

import edu.ucla.belief.rc2.structure.*;
import edu.ucla.belief.rc2.caching.*;
import edu.ucla.belief.rc2.creation.*;


//Many of these functions were coded very quickly and so are not
//  very robust and do not have much error checking.  They are not
//  recommended for general purpose use, but specifically for
//  the UAI compeition.
public class RC_Link_UAI2006 implements UaiPeEngine {

	static boolean EXTRA_DEBUG = false; //TODO

	public RC_Link_UAI2006() { }

	//for Testing
	public static void main(String args[])
	{
		try
		{
			BeliefNetwork bn;
			Map<FiniteVariable, Object> evid;
			if (args.length == 2)
			{
				bn = NetworkIO.read(args[0]);
				evid = readEvid(bn, new File(args[1]));
			}
			else if (args.length == 1)
			{
				RunReadBIF b = new RunReadBIF(new File(args[0]), null);
				bn = b.beliefNetwork();
				evid = new HashMap<FiniteVariable, Object>(bn.getEvidenceController().evidence());
			}
			else throw new IllegalStateException();

			if (isGeneticNetwork(bn, evid))
			{
				System.out.println("Genetic Network: " + args[0]);
				System.out.println("Computed value = " + new RC_Link_UAI2006().computePe(new Random(), bn, evid, null).probability+"\n\n");
			}
			else
			{
				System.out.println("Not a genetic network.");
				//Call my old KnowledgeBase code so Mark can attempt to run it
				System.out.println("Computed value = " + new RC_Link_UAI2006().computePeNonGenetic(bn, evid, 1).probability + "\n\n");
				//System.out.println("Computed value = " + new RC_Link_UAI2006().computePeKB(bn, evid).probability + "\n\n");
			}
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.toString());
			e.printStackTrace(System.err);
		}
	}

	//Main Interface
	//This function expects only genetic networks to be passed into it.
	public UaiMapSolution computePe
		(Random r, BeliefNetwork bn, Map<FiniteVariable, Object> e,
		 UaiPreprocessResult pr) throws Exception
	{
		HashMap evid = new HashMap();
//		if (EXTRA_DEBUG) System.out.println("evid: ");
		for (Iterator itr = e.keySet().iterator(); itr.hasNext(); )
		{
			FiniteVariable o = (FiniteVariable)itr.next();
			Object v = e.get(o);
			evid.put(o.getID(), v);
//			if (EXTRA_DEBUG) System.out.println(o + " = " + v);
		}

		int scalar = 1;
		Pedigree ped = Pedigree.createPedigree(bn, evid);
		Pedigree.GeneticNetwork gn = ped.createBeliefNetwork(scalar);

		RC2 rcBest = null;
		RC2.RCCreationParams param = new RC2.RCCreationParams(gn.bn);
		{
			param.allowPartialDerivatives = true; //all evidence nodes become leafs, therefore, iterateAll is better than iterateSkp.
			param.allowKBinst = false;
			param.useKBinst = param.allowKBinst;
			param.outputConsole = null;
			param.scalar = scalar;
			param.allowKBsat = 0;
		}

		Collection cachedNodesSearch = new HashSet();
		//for constrained ordering, weight search slightly toward non-constrained by adding multiple nulls
		RCGenerator.RC rcTmp = RCGenerator.generateRC(gn.bn, "network", null, false/*extendedSearch*/, gn.equivPreProcessVars(), new Set[][] { null, gn.locusOrdering(), gn.familyOrdering(), gn.reverseLocusOrdering(), gn.dynamicBNOrderingVEPD() });
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

		{//allocate memory

			long mm = Runtime.getRuntime().maxMemory();
			if (EXTRA_DEBUG) System.out.println("Max Memory: " + mm + " MB: " + (mm / 1048576.0));

			double factor = .9;
			long ce = (long)Math.floor(mm / 8 * factor); //(max memory / 8)=max number of caches, but leave some room for overhead
			if (EXTRA_DEBUG) System.out.println("Max Cache Entries: " + ce + " (MB: " + ce * 8 / 1048576.0 + ")");

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

			rcBest.setCachingScheme(new RC2CachingScheme_Collection(cachedName, cachedNodes));
			if (EXTRA_DEBUG) System.out.println(rcBest.compStats().currentCaching().toString());
		}

		if (rcBest != null) rcBest.synchEvidWithBN();

		double finalRes = rcBest.compute_Pe() * gn.constantScaled();

		if (rcBest != null) rcBest.close();

		UaiMapSolution ret = new UaiMapSolution();
		ret.instantiation = Collections.EMPTY_MAP;
		ret.probability = UaiMapSolution.toBigDecimal(finalRes);
		return ret;
	}//end computePe



	//This function expects only NON-genetic networks to be passed into it.
	public UaiMapSolution computePeKB(BeliefNetwork bn, Map<FiniteVariable, Object> e) throws Exception
	{
		HashMap evid = new HashMap();
//		if (EXTRA_DEBUG) System.out.println("evid: ");
		for (Iterator itr = e.keySet().iterator(); itr.hasNext(); )
		{
			FiniteVariable o = (FiniteVariable)itr.next();
			Object v = e.get(o);
			evid.put(o.getID(), v);
//			if (EXTRA_DEBUG) System.out.println(o + " = " + v);
		}

		RC2 rcBest = null;
		RC2.RCCreationParams param = new RC2.RCCreationParams(bn);
		{
			param.allowPartialDerivatives = false; //all evidence nodes become leafs, therefore, iterateAll is better than iterateSkp.
			param.allowKBinst = true;
			param.useKBinst = param.allowKBinst;
//			param.outputConsole = new PrintWriter(System.out, true); //TODO use this for debug, next line to remove debug statements
			param.outputConsole = null;
			param.scalar = 1.0;
			param.allowKBsat = 0;
		}

		Collection cachedNodesSearch = new HashSet();
		RCGenerator.RC rcTmp = RCGenerator.generateRC(bn, "network", null, false/*extendedSearch*/, null, new Set[][] { null});
		{
			File tmprc = File.createTempFile("rcFile", null, new File("."));
			Writer rcOut = new BufferedWriter(new FileWriter(tmprc));
			rcTmp.writeFile(rcOut);
			rcOut.flush();
			rcOut.close();
			rcOut = null;

			//could cause memory trouble
			rcBest = RC2CreatorFile.readRC(param, new RC2CreatorFile.Params(bn, tmprc.getPath()), cachedNodesSearch);

			tmprc.delete();
			tmprc = null;
		}
		{//allocate memory

			long mm = Runtime.getRuntime().maxMemory();
			if (EXTRA_DEBUG) System.out.println("Max Memory: " + mm + " MB: " + (mm / 1048576.0));

			double factor = .9;
			long ce = (long)Math.floor(mm / 8 * factor); //(max memory / 8)=max number of caches, but leave some room for overhead
			if (EXTRA_DEBUG) System.out.println("Max Cache Entries: " + ce + " (MB: " + ce * 8 / 1048576.0 + ")");

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

			rcBest.setCachingScheme(new RC2CachingScheme_Collection(cachedName, cachedNodes));
			if (EXTRA_DEBUG) System.out.println(rcBest.compStats().currentCaching().toString());
		}

		if (rcBest != null) rcBest.synchEvidWithBN();

		double finalRes = rcBest.compute_Pe();

		if (rcBest != null) rcBest.close();

		UaiMapSolution ret = new UaiMapSolution();
		ret.instantiation = Collections.EMPTY_MAP;
		ret.probability = UaiMapSolution.toBigDecimal(finalRes);
		return ret;
	}//end computePeKB


	//This function expects only NON-genetic networks to be passed into it.
	//They should also be preprocessed/pruned if possible
	public UaiMapSolution computePeNonGenetic(BeliefNetwork bn, Map<FiniteVariable, Object> e, int scalar) throws Exception
	{
		HashMap evid = new HashMap();
//		if (EXTRA_DEBUG) System.out.println("evid: ");
		for (Iterator itr = e.keySet().iterator(); itr.hasNext(); )
		{
			FiniteVariable o = (FiniteVariable)itr.next();
			Object v = e.get(o);
			evid.put(o.getID(), v);
//			if (EXTRA_DEBUG) System.out.println(o + " = " + v);
		}

		RC2 rcBest = null;
		RC2.RCCreationParams param = new RC2.RCCreationParams(bn);
		{
			param.allowPartialDerivatives = true; //all evidence nodes become leafs, therefore, iterateAll is better than iterateSkp.
			param.allowKBinst = false;
			param.useKBinst = param.allowKBinst;
//			param.outputConsole = new PrintWriter(System.out, true); //TODO use this for debug, next line to remove debug statements
			param.outputConsole = null;
			param.scalar = scalar;
			param.allowKBsat = 0;
		}

		Collection cachedNodesSearch = new HashSet();
		RCGenerator.RC rcTmp = RCGenerator.generateRC(bn, "network", null, false/*extendedSearch*/, null, new Set[][] { null });
		{
			File tmprc = File.createTempFile("rcFile", null, new File("."));
			Writer rcOut = new BufferedWriter(new FileWriter(tmprc));
			rcTmp.writeFile(rcOut);
			rcOut.flush();
			rcOut.close();
			rcOut = null;

			//could cause memory trouble
			rcBest = RC2CreatorFile.readRC(param, new RC2CreatorFile.Params(bn, tmprc.getPath()), cachedNodesSearch);

			tmprc.delete();
			tmprc = null;
		}
		{//allocate memory

			long mm = Runtime.getRuntime().maxMemory();
			if (EXTRA_DEBUG) System.out.println("Max Memory: " + mm + " MB: " + (mm / 1048576.0));

			double factor = .9;
			long ce = (long)Math.floor(mm / 8 * factor); //(max memory / 8)=max number of caches, but leave some room for overhead
			if (EXTRA_DEBUG) System.out.println("Max Cache Entries: " + ce + " (MB: " + ce * 8 / 1048576.0 + ")");

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

			rcBest.setCachingScheme(new RC2CachingScheme_Collection(cachedName, cachedNodes));
			if (EXTRA_DEBUG) System.out.println(rcBest.compStats().currentCaching().toString());
		}

		if (rcBest != null) rcBest.synchEvidWithBN();

		double finalRes = rcBest.compute_Pe();

		if (rcBest != null) rcBest.close();

		UaiMapSolution ret = new UaiMapSolution();
		ret.instantiation = Collections.EMPTY_MAP;
		ret.probability = UaiMapSolution.toBigDecimal(finalRes);
		return ret;
	}//end computePeNonGenetic





	//Tests if the network is a genetic network which RC_Link can use
	//If it is, it will rename the variable Ids
	//Is not a complete test of all attributes, but should work well enough for challenge
	public static boolean isGeneticNetwork(BeliefNetwork bn, Map<FiniteVariable, Object> evid)
	{
		if (EXTRA_DEBUG) System.out.println("Begin isGeneticNetwork");

		//Leafs are all phenotype variables
		HashSet phenotypeVars = new HashSet();
		for (Iterator itr = bn.iterator(); itr.hasNext(); )
		{
			Object var = itr.next();
			if (bn.outDegree(var) == 0)
			{
				phenotypeVars.add(var);
				int indeg = bn.inDegree(var); //phenotypes must have 2 or 4
				if (indeg != 2 && indeg != 4) return false;
			}
		}

		if (EXTRA_DEBUG) System.out.println("Leafs all have 2 or 4 parents");
		//if (EXTRA_DEBUG) System.out.println("" + phenotypeVars);

		//Parents of phenotype variables are genotype variables
		HashSet genotypeVars = new HashSet();
		for (Iterator itr = phenotypeVars.iterator(); itr.hasNext(); )
		{
			Object var = itr.next();
			genotypeVars.addAll(bn.inComing(var));
		}
		//if (EXTRA_DEBUG) System.out.println("Genotypes: " + genotypeVars);

		//Selectors
		HashSet selectorVars = new HashSet(bn);
		selectorVars.removeAll(phenotypeVars);
		selectorVars.removeAll(genotypeVars);
		if (selectorVars.size() % 2 == 1) return false;
		if (EXTRA_DEBUG) System.out.println("Even number of potential selectors");
		for (Iterator itr = selectorVars.iterator(); itr.hasNext(); )
		{
			FiniteVariable var = (FiniteVariable)itr.next();
			//if (EXTRA_DEBUG) System.out.println("" + var);
			if (var.size() != 2) return false; //selectors must have 2 states
			int indeg = bn.inDegree(var);
			if (indeg > 1) return false; //selectors can have at most 1 parent
			if (bn.outDegree(var) > 2) return false; //selectors can have 1 genotype child & 1 selector child
			if (indeg == 0)
			{
				CPTShell cpt = var.getCPTShell();
				if (cpt.getCP(0) != 0.5 || cpt.getCP(1) != 0.5)
				{
					if (EXTRA_DEBUG) System.out.println(cpt.getCPT().toString());
					return false;
				}
			}
			else if (indeg == 1)
			{
				CPTShell cpt = var.getCPTShell();
				if (cpt.getCP(0) != cpt.getCP(3) || cpt.getCP(1) != cpt.getCP(2))
				{
					if (EXTRA_DEBUG) System.out.println(cpt.getCPT().toString());
					return false;
				}
			}
		}
		if (EXTRA_DEBUG) System.out.println("Selectors okay");

		//Genotype structure
		for (Iterator itr1 = genotypeVars.iterator(); itr1.hasNext(); )
		{
			FiniteVariable var = (FiniteVariable)itr1.next();
			int indeg = bn.inDegree(var);
			if (indeg == 0) //founder
			{
			}
			else if (indeg == 3) //non-founder
			{
				Set parents = bn.inComing(var);
				for (Iterator itr2 = parents.iterator(); itr2.hasNext(); )
				{
					//parent->children genotype variables must have same cardinality
					FiniteVariable par = (FiniteVariable)itr2.next();
					if (genotypeVars.contains(par))
					{
						if (var.size() != par.size()) return false;
					}
				}
				//non-founder genotype variables must be deterministic
				CPTShell cpt = var.getCPTShell();
				for (int i = 0; i < cpt.index().size(); i++)
				{
					double val = cpt.getCP(i);
					if (val != 0.0 && val != 1.0) return false;
				}
			}
			else return false;
		}
		if (EXTRA_DEBUG) System.out.println("Genotypes okay");

		HashMap selectorChains = new HashMap();
		for(Iterator itr1 = selectorVars.iterator(); itr1.hasNext();)
		{
			FiniteVariable sel = (FiniteVariable)itr1.next();
			if (bn.inDegree(sel) != 0) continue; //look for first loci
			ArrayList selectors = new ArrayList();

			while (true)
			{
				selectors.add(sel);
				Set children = bn.outGoing(sel);
				if (children.size() == 1) { break; } //it is a genotype variable (end of selector chain)
				else if (children.size() == 2)
				{
					Iterator itr2 = children.iterator();
					Object ch = itr2.next();
					if (!selectorVars.contains(ch)) ch = itr2.next();
					if (!selectorVars.contains(ch)) throw new IllegalStateException();
					sel = (FiniteVariable)ch;
				}
				else throw new IllegalStateException();
			}
			selectorChains.put(selectors.get(0), selectors);
		}

		if (EXTRA_DEBUG) System.out.println("Network determined to be genetic, renaming variables");
		renameVariables(bn, phenotypeVars, genotypeVars, selectorVars, selectorChains, evid);

		//if (EXTRA_DEBUG)
		//{
		//    for (Iterator itr = bn.iterator(); itr.hasNext(); )
		//    {
		//        System.out.println("" + itr.next());
		//    }
		//}

		return true;
	}

	//Very rough code, but should work in the limited set of networks for UAI2006
	//Very little error checking
	private static void renameVariables(BeliefNetwork bn, HashSet phenotypeVars,
		HashSet genotypeVars, HashSet selectorVars, HashMap selectorChains,
		Map<FiniteVariable, Object> evidMap)
	{
		int nextPersonID = 1;
		//handle founders
		{
			for (Iterator itr1 = selectorChains.keySet().iterator(); itr1.hasNext(); )
			{
				boolean foundFounder = false;
				ArrayList selectors = (ArrayList)selectorChains.get(itr1.next());
				for(int i=0; i<selectors.size(); i++) {
					FiniteVariable sel = (FiniteVariable)selectors.get(i); //selector in locus i
					Set children = bn.outGoing(sel);
					Iterator itr2 = children.iterator();
					Object chi = itr2.next();
					if (selectorVars.contains(chi)) chi = itr2.next(); //chi is genotype variable

					Set parents = bn.inComing(chi);
					Iterator itr3 = parents.iterator();
					FiniteVariable par1 = (FiniteVariable)itr3.next();
					if(par1==sel) par1 = (FiniteVariable)itr3.next(); //par is one of the genotype vars of parent
					FiniteVariable par2 = (FiniteVariable)itr3.next();
					if(par2==sel) par2 = (FiniteVariable)itr3.next(); //par is one of the genotype vars of parent

					if (par1.getID().charAt(0) == 'G') break; //already changed

					foundFounder = (bn.inDegree(par1)==0);
					if(foundFounder) {
						renameID(par1, "Gp" + nextPersonID + "_" + i, bn);
						renameID(par2, "Gm" + nextPersonID + "_" + i, bn);
						//find phenotype
						Iterator itr4 = bn.outGoing(par1).iterator();
						FiniteVariable ph;
						do {
							ph = (FiniteVariable)itr4.next();
						} while (!phenotypeVars.contains(ph));
						renameID(ph, "P" + nextPersonID + "_" + i, bn);

						//clean sets
//						phenotypeVars.remove(ph);
						genotypeVars.remove(par1);
						genotypeVars.remove(par2);
					}
				}
				if(foundFounder) nextPersonID++; //next person
			}
		}//done with founders

		if (EXTRA_DEBUG) System.out.println("NumFounders = " + (nextPersonID-1));

		int numPeople = nextPersonID + (selectorChains.keySet().size() / 2);
		int parGender[] = new int[numPeople];
		Arrays.fill(parGender, -1); //-1=unknown, 1=male, 2=female

		if (EXTRA_DEBUG) System.out.println("NumPeople = " + numPeople);


		while (!selectorChains.isEmpty())
		{
			for (Iterator itr1 = selectorChains.keySet().iterator(); itr1.hasNext(); )
			{
				boolean remove = false;
				boolean canRename = true; //assume I can until I prove otherwise

				int par1ID = -1;
				int par2ID = -1;

				ArrayList selectors1 = (ArrayList)selectorChains.get(itr1.next());
				ArrayList selectors2 = null;
				for (int i = 0; i < selectors1.size() && canRename == true; i++)
				{
					FiniteVariable sel1 = (FiniteVariable)selectors1.get(i);
					if (sel1.getID().charAt(0) == 'S') { canRename = false; remove = true; break; } //This was the 2nd side of another one already done

					Iterator itr2 = bn.outGoing(sel1).iterator();
					FiniteVariable gen1 = (FiniteVariable)itr2.next();
					if (selectorVars.contains(gen1)) gen1 = (FiniteVariable)itr2.next();

					//if (gen1.getID().charAt(0) == 'G') { remove = true; break; } //This was the 2nd side of another one already done
					//Test if its parent is done
					{
						Iterator itr3 = bn.inComing(gen1).iterator();
						FiniteVariable par = (FiniteVariable)itr3.next();
						if (par == sel1) par = (FiniteVariable)itr3.next(); //par is now one of the genotype vars of parent
						String nm1 = par.getID();
						if (nm1.charAt(0) != 'G') { canRename = false; break; } //This parent is not ready
						int idx = nm1.indexOf('_');
						par1ID = Integer.parseInt(nm1.substring(2, idx));
					}

					//find phenotype variable
					FiniteVariable ph;
					{
						Iterator itr_ph = bn.outGoing(gen1).iterator();
						do
						{
							ph = (FiniteVariable)itr_ph.next();
						} while (!phenotypeVars.contains(ph));
					}

					//find 2nd genotype variable
					FiniteVariable sel2 = null;
					if (selectors2 == null)
					{
						boolean done = false;
						for (Iterator itr_g1 = bn.inComing(ph).iterator(); itr_g1.hasNext() && !done; )
						{
							Object o1 = itr_g1.next(); //A genotype parent of the phenotype
							if (o1 == gen1) continue;
							for (Iterator itr_g2 = bn.inComing(o1).iterator(); itr_g2.hasNext() && !done; )
							{
								Object o2 = itr_g2.next(); //Selector parent of genotype
								if (selectorChains.keySet().contains(o2))
								{
									sel2 = (FiniteVariable)o2;
									selectors2 = (ArrayList)selectorChains.get(o2);
									done = true;
								}
							}
						}
					}
					else
					{
						sel2 = (FiniteVariable)selectors2.get(i);
					}

					if (sel2 == null) throw new IllegalStateException("Did not find sel2");
					Iterator itr4 = bn.outGoing(sel2).iterator();
					FiniteVariable gen2 = (FiniteVariable)itr4.next();
					if (selectorVars.contains(gen2)) gen2 = (FiniteVariable)itr4.next();

					//Test if its parent is done
					{
						Iterator itr5 = bn.inComing(gen2).iterator();
						FiniteVariable par = (FiniteVariable)itr5.next();
						if (par == sel2) par = (FiniteVariable)itr5.next(); //par is one of the genotype vars of parent
						String nm2 = par.getID();
						if (nm2.charAt(0) != 'G') { canRename = false; break; } //This parent is not ready
						int idx = nm2.indexOf('_');
						par2ID = Integer.parseInt(nm2.substring(2, idx));
					}

					//based on parGender & par1ID & par2ID adjust the naming convention
					//This is a hack an may not work in all cases
					boolean par1male;
					if (parGender[par1ID] == -1)
					{
						if(parGender[par2ID]==-1 || parGender[par2ID]==2) par1male = true;
						else par1male = false;
					}
					else if (parGender[par1ID] == 1) //male
					{
						if (parGender[par2ID] == -1 || parGender[par2ID] == 2) par1male = true;
						else throw new IllegalStateException();
					}
					else if (parGender[par1ID] == 2) //female
					{
						if (parGender[par2ID] == -1 || parGender[par2ID] == 1) par1male = false;
						else throw new IllegalStateException();
					}
					else throw new IllegalStateException();

					String s1;
					String s2;
					if(par1male)
					{
						parGender[par1ID] = 1;
						parGender[par2ID] = 2;
						s1 = "p";
						s2 = "m";
					}
					else
					{
						parGender[par1ID] = 2;
						parGender[par2ID] = 1;
						s1 = "m";
						s2 = "p";
					}


					//rename this person (for all loci)
					renameID(gen1, "G" + s1 + nextPersonID + "_" + i, bn);
					renameID(gen2, "G" + s2 + nextPersonID + "_" + i, bn);
					renameID(sel1, "S" + s1 + nextPersonID + "_" + i, bn);
					renameID(sel2, "S" + s2 + nextPersonID + "_" + i, bn);
					renameID(ph, "P" + nextPersonID + "_" + i, bn);

//I don't remove phenotype variables because when you have a
//  complex locus with 4 parents I normally named it after the
//  last one, so if I don't remove it, the last one will rename it.
//					phenotypeVars.remove(ph);
					genotypeVars.remove(gen1);
					genotypeVars.remove(gen2);
					selectorVars.remove(sel1);
					selectorVars.remove(sel2);
				}//for each locus

				if (canRename || remove) itr1.remove(); //either just finished or had already done it
				if (canRename) nextPersonID++;
			}
		}//while selectorChains is not empty
//		if (!phenotypeVars.isEmpty()) throw new IllegalStateException();
		if (!genotypeVars.isEmpty()) throw new IllegalStateException();
		if (!selectorVars.isEmpty()) throw new IllegalStateException();
		if (!selectorChains.isEmpty()) throw new IllegalStateException();

		if (nextPersonID - 1 > numPeople) throw new IllegalStateException();

		//rename states only inside of evid
		for (Iterator itr_st = evidMap.keySet().iterator(); itr_st.hasNext(); )
		{
			FiniteVariable v = (FiniteVariable)itr_st.next();

			String nm = v.getID();
			if (nm.charAt(0) == 'G')
			{
				evidMap.put(v, "" + v.index(evidMap.get(v))); //change evidMap from state to a zero based index (as a String)
			}
			else if (nm.charAt(0) == 'S')
			{
				evidMap.put(v, "" + (1 + v.index(evidMap.get(v)))); //change evidMap from state to one based index (as a String)
			}
			else if (nm.charAt(0) == 'P')
			{
				if (v.size() == 2)
				{
					evidMap.put(v, "" + v.index(evidMap.get(v))); //change evidMap from state to a zero based index (as a String)
				}
				else
				{
					int oldIndx = v.index(evidMap.get(v));

					boolean done = false;

					//Should fix this, very inefficient
					int numAllele;
					{
						int sum = 0;
						for (int i = 1; ; i++)
						{
							sum += i;
							if (sum == v.size()) { numAllele = i; break; }
							else if (sum > v.size()) throw new IllegalStateException();
						}
					}

					int cnt = 0;
					for (int i = 0; i < numAllele && !done; i++)
					{
						for (int j = i; j < numAllele && !done; j++)
						{
							//v.set(cnt, i + "_" + j);
							if (cnt == oldIndx) { done = true; evidMap.put(v, i + "_" + j); }
							cnt++;
						}
					}
				}
			}
			else throw new IllegalStateException();
		}

		//try
		//{
		//    NetworkIO.writeNetwork(bn, new File("tempNet.net"));
		//}
		//catch (Exception e) {
		//    System.err.println("Threw exception: " + e);
		//    e.printStackTrace(System.err);
		//    throw new IllegalStateException();
		//}

	}//end renameVariables


	//Read in evidence from a file
	private static Map<FiniteVariable, Object> readEvid(BeliefNetwork bn, File inf)
	{
		Map<FiniteVariable, Object> ret = new HashMap<FiniteVariable, Object>();
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
						ret.put((FiniteVariable)bn.forID(s1), s2);
					}
					else throw new IllegalStateException();
				}
				ln = in.readLine();
			}
		}
		catch (Exception e)
		{
			System.err.println("ERROR: " + e.toString());
			ret = null;
		}
		return ret;
	}

	static private void renameID(FiniteVariable fv, String newID, BeliefNetwork bn)
	{
		String oldID = fv.getID();
		fv.setID(newID);
		bn.identifierChanged(oldID, fv);
	}

}//end RC_Link_UAI2006



