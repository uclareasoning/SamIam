package edu.ucla.belief.io.geneticlinkage;

import edu.ucla.belief.*;
import edu.ucla.util.Maps;

import java.io.*;
import java.util.*;



/** This class reads in and stores pedigrees.*/
public class Pedigree
{
	private static boolean DEBUG_Test2 = false;
	private static boolean DEBUG_Pedigree = false;
	private static boolean DEBUG_PreprocessRules = false;
	private static boolean DEBUG_PedigreeVerbose = false;
	private static boolean DEBUG_PedigreeVerbose2 = false;



	//The PREPROCESS variable determines how the preporcesing rules are handled.  It can be set to one of the following variables
	//  they mean the following:
	//  NONE: No preprocessing what-so-ever
	//  LEAF_PRUNE: Prune leaf nodes with no evidence & independent variables, but nothing else
	//  VALID_NET: Make sure this network satisfies the valid cpt properties, although prob may not sum to 1 & should not be normalized
	//  ALL: Full preprocessing, all rules implemented
	public final static int NONE = 0;			//no preprocessing what-so-ever
	public final static int LEAF_PRUNE = 1;	//prune leafs with no evidence, but no learning (also prune independent variables if any) & out edges from evidence (if any on non-leafs)
	public final static int LEAF_ADDITIONAL_EVID = 2; //learn additional info and do classical pruning, but no allele recoding or variable equivalence or founder removal
	public final static int VALID_NET = 3;		//learn, but want valid network
	public final static int ALL = 4;			//full preprocessing

	public final static int PREPROCESS = ALL;


	protected ArrayList pedigreeData = new ArrayList(); //Array of Person objects
	protected HashSet nuclearFamilies = new HashSet(); //Array of Nuclear Families
	protected final PseudoLoci psLoci;

	protected Person parChiOrder[] = null;


	static final private String states12[] = {"1","2"};
	static final private String states21[] = {"2","1"};
	static final private String other = "other";

	static final double epsilon = .000000001;


	private Pedigree(PseudoLoci ps) { this.psLoci = ps; }

	//evid must map from variableID -> string, but the string does not actually have to be
	//  an instance in the FiniteVariable object (but it needs to conform to the naming
	//  convention used with the rest of RC_Link)
	static public Pedigree createPedigree(BeliefNetwork bn, HashMap evid)
	{
		if (DEBUG_Pedigree) { System.out.println("Begin Read Pedigree (from Network)"); }
		Pedigree ret = read(bn, evid);

		finishCreation(ret);
		return ret;
	}

	static public Pedigree createPedigree(File file, Loci loci)
	throws IOException
	{
		return createPedigree(file.getCanonicalPath(), loci);
	}

	static public Pedigree createPedigree(String file, Loci loci)
	throws IOException {
		if(DEBUG_Pedigree) { System.out.println("Begin Read Pedigree");}
		Pedigree ret = read(new BufferedReader(new FileReader(file)),loci);

		finishCreation(ret);
		return ret;
	}

	static private void finishCreation(Pedigree ret)
	{
		if(DEBUG_PedigreeVerbose) { System.out.println("Pedigree.init");}
		ret.init();

		if(DEBUG_PedigreeVerbose) { System.out.println("Pedigree.simplify");}
		if(PREPROCESS!=NONE) ret.simplify();

		if(DEBUG_PedigreeVerbose) { System.out.println("Pedigree.alleleRecode");}
		ret.alleleRecode();

		if(DEBUG_Pedigree) { System.out.println("Finished creating the pedigree");}
	}


    /** Will return a Pedigree from the pedigree file.*/
    static protected Pedigree read(BufferedReader in, Loci loci)
    throws IOException {

		PseudoLoci_Loci psLociL = new PseudoLoci_Loci(loci);
		Pedigree ret = new Pedigree(psLociL);
        ret.pedigreeData.add(null); //individual index values start at 1 (code below relies on this)
		Person per = readLine(in, psLociL);

		if(per==null) { throw new IllegalStateException("ERROR (pedigree file): Contained no people.");}
		int pedID = per.pedigreeID;

		while(per != null) {
			if(ret.pedigreeData.size() != per.personID) {
				throw new IllegalArgumentException("ERROR (pedigree file): Person out of order: should have been " + ret.pedigreeData.size() + ", but instead found " + per.personID);
            }
            if(pedID!=per.pedigreeID) {
				throw new IllegalArgumentException("ERROR (pedigree file): Found multiple pedigrees : " + pedID + " and " + per.pedigreeID);
			}
            ret.pedigreeData.add(per);
            if(DEBUG_PedigreeVerbose) { System.out.println("Read Person "+per.personID);}
			per = readLine(in, psLociL);
        }
        if(DEBUG_Pedigree) { System.out.println("Read in "+(ret.pedigreeData.size()-1)+" people");}
        return ret;
    }

	/** Will return a Pedigree from the BN file.*/
	static protected Pedigree read(BeliefNetwork bn, HashMap evid)
	{
		PseudoLoci_BN psLociL = new PseudoLoci_BN(bn);
		Pedigree ret = new Pedigree(psLociL);
		ret.pedigreeData.add(null); //individual index values start at 1 (code below relies on this)
		int indxP = 1;
		for(;;) {
			Person per = readPer(indxP, psLociL, bn, evid);
			if(per==null) break;
			ret.pedigreeData.add(per);
			if (DEBUG_PedigreeVerbose) { System.out.println("Read Person " + per.personID); }
			indxP++;
		}
		if (indxP == 0) throw new IllegalStateException("ERROR (BN): Contained no people.");
		if (DEBUG_Pedigree) { System.out.println("Read in " + (ret.pedigreeData.size() - 1) + " people"); }
		return ret;
	}

    public String toString() {
        StringBuffer ret = new StringBuffer();

        ret.append("Pedigree {\n");
        for( int i=0; i<pedigreeData.size(); i++) {
			Object per = pedigreeData.get(i);
			if(per==null) {continue;}
			ret.append(" " + per.toString());
        }
        ret.append("}\n");
        return ret.toString();
    }


	/**Read in a line containing a Person object.*/
	static protected Person readLine(BufferedReader in, PseudoLoci_Loci psLoci)
	throws IOException {

		String ln = in.readLine();
		if(ln!=null) {ln=ln.trim(); if(ln.length()==0) {ln=null;}}
		if( ln == null) { return null;}
		StringBuffer line = new StringBuffer(ln);

		final int numLoci = psLoci.getNumLoci();

		Person ret = new Person(numLoci,
			/*pedigreeID =*/ UtilGeneReader.readIntExpected(line),
			/*personID   =*/ UtilGeneReader.readIntExpected(line),
			/*fatherID   =*/ UtilGeneReader.readIntExpected(line),
			/*motherID   =*/ UtilGeneReader.readIntExpected(line),
			/*firstSib   =*/ UtilGeneReader.readIntExpected(line),
			/*nextPaternalSib =*/ UtilGeneReader.readIntExpected(line),
			/*nextMaternalSib =*/ UtilGeneReader.readIntExpected(line),
			/*male       =*/ (UtilGeneReader.readIntExpected(line)==1),
			psLoci);

		/*ignore this =*/ UtilGeneReader.readIntExpected(line);

		for(int i=0; i<numLoci; i++) {

			//NOTE these convert phenotype information to 0 based, for array indexing
			int one;
			int two;

			switch(psLoci.getLociType(i)) {

				case 1:
					one = UtilGeneReader.readIntExpected(line);
					if(((Loci.LocusDataAffectionStatus)psLoci.getLoci().loci(i).data).numPenetrances>1) {
						two = UtilGeneReader.readIntExpected(line);//penetrance class
					}
					else {
						two = 1;//penetrance class
					}
				break;

				case 3:
					one = UtilGeneReader.readIntExpected(line);
					two = UtilGeneReader.readIntExpected(line);
				break;

				case 4:
					one=0; //only read in for 2nd complex disease locus
					two=0;


					if(psLoci.getLoci().loci(i).data instanceof Loci.LocusDataComplexAffection2) {
						one = UtilGeneReader.readIntExpected(line);

						if(((Loci.LocusDataComplexAffection2)psLoci.getLoci().loci(i).data).numPenetrances>1) {
							two = UtilGeneReader.readIntExpected(line);//penetrance class
						}
						else {
							two = 1;//penetrance class
						}
					}
				break;

				default:
					throw new IllegalStateException();
			}
			ret.phenotype[i].setFromInput(one, two);
		}

		{
			int tmp;
			if((tmp=UtilGeneReader.readInt(line)) != Integer.MIN_VALUE) {
				throw new IllegalStateException("ERROR (pedigree file): Line to long for person " + ret.personID + ", found a " + tmp);
			}
		}
		return ret;
	}//end readLine

	/**Read in a person from the BN object.*/
	static protected Person readPer(int perID, PseudoLoci_BN psLoci, BeliefNetwork bn, HashMap evid)
	{
		final int numLoci = psLoci.getNumLoci();

		int faID = 0;
		{
			FiniteVariable fv = (FiniteVariable)bn.forID("Gp" + perID + "_0");

			if (fv == null) return null;
			if (bn.inDegree(fv) > 0)
			{
				Iterator parI = bn.inComing(fv).iterator();
				FiniteVariable fvPar = (FiniteVariable)parI.next();
				if (fvPar.getID().charAt(0) == 'S') fvPar = (FiniteVariable)parI.next();
				faID = convertNodeNameToPerID(fvPar.getID());
			}
		}

		int moID = 0;
		{
			FiniteVariable fv = (FiniteVariable)bn.forID("Gm" + perID + "_0");
			if (fv == null) throw new IllegalStateException(); //couldn't find person
			if (bn.inDegree(fv) > 0)
			{
				Iterator parI = bn.inComing(fv).iterator();
				FiniteVariable fvPar = (FiniteVariable)parI.next();
				if (fvPar.getID().charAt(0) == 'S') fvPar = (FiniteVariable)parI.next();
				moID = convertNodeNameToPerID(fvPar.getID());
			}
		}

		int firSib = nextSib(bn, (FiniteVariable)bn.forID("Gp" + perID + "_0"), 0);

		int nextPatSib = 0;
		if (faID != 0) nextPatSib = nextSib(bn, (FiniteVariable)bn.forID("Gp"+faID+"_0"), perID);

		int nextMatSib = 0;
		if (moID != 0) nextMatSib = nextSib(bn, (FiniteVariable)bn.forID("Gp" + moID + "_0"), perID);

		boolean male = isMale(bn, (FiniteVariable)bn.forID("Gp" + perID + "_0"));

		Person ret = new Person(numLoci, 0, perID, faID, moID, firSib, nextPatSib, nextMatSib, male, psLoci);
		for (int i = 0; i < numLoci; i++)
		{
			int one = -1;//no evid
			int two = -1;//no evid
			String evidState = (String)evid.get("P"+perID+"_"+i);
			if (evidState != null)
			{
				switch (psLoci.getLociType(i))
				{
					case 1:
					case 4:
						if(evidState.equals("0")) one = 0;
						else if(evidState.equals("1")) one = 1;
						else throw new IllegalStateException();
						break;
					case 3:
						int l = evidState.indexOf('_');
						String t1 = evidState.substring(0,l);
						String t2 = evidState.substring(l+1);
						one = Integer.parseInt(t1);
						two = Integer.parseInt(t2);
						break;
				}
			}
			ret.phenotype[i].setFromInput(one+1, two+1);
		}
		return ret;
	}

	static protected boolean isMale(BeliefNetwork bn, FiniteVariable fv)
	{
		if (bn.outDegree(fv) == 0) return true; //no way of knowing, don't care
		Set children = bn.outGoing(fv);
		for (Iterator itr = children.iterator(); itr.hasNext(); )
		{
			FiniteVariable fvChi = (FiniteVariable)itr.next();
			if (fvChi.getID().charAt(0) != 'G') continue;
			if (fvChi.getID().charAt(1) == 'p') return true;
			else if (fvChi.getID().charAt(1) == 'm') return false;
			else throw new IllegalStateException();
		}
		return true; //no way of knowing, don't care
	}

	static protected int nextSib(BeliefNetwork bn, FiniteVariable parFV, int currSib)
	{
		if (bn.outDegree(parFV) == 0) return 0;
		Set children = bn.outGoing(parFV);
		int ret = 0;
		for (Iterator itr = children.iterator(); itr.hasNext(); )
		{
			FiniteVariable fvChi = (FiniteVariable)itr.next();
			if (fvChi.getID().charAt(0) != 'G') continue;
			int chiID = convertNodeNameToPerID(fvChi.getID());
			if (chiID > currSib)
			{
				if (ret == 0) ret = chiID;
				else if (chiID < ret) ret = chiID;
			}
		}
		return ret;
	}

	static protected int convertNodeNameToPerID(String name)
	{

		if(name.charAt(0)=='P')
		{
			String tmp = name.substring(1,name.indexOf('_')); //between P and _
			return Integer.parseInt(tmp);
		}
		else if(name.charAt(0)=='G')
		{
			String tmp = name.substring(2,name.indexOf('_')); //between Gp/Gm and _
			return Integer.parseInt(tmp);
		}
		else throw new IllegalStateException();
	}


	protected void init() {
		int founders = 0;
		int nonfounders = 0;

		//calc family
		for(int i=1; i<pedigreeData.size(); i++) {
			Person per = initFamily(i);
			if (per.father == null && per.mother == null) founders++;
			else nonfounders++;
		}

		//calc depth
		for(int i=1; i<pedigreeData.size(); i++) {
			Person per = (Person)pedigreeData.get(i);
			calcDepth(per);
		}

		//generate nuclear families
		generateNFamilies();

		if (PREPROCESS == NONE) //TODO should only be used during debug
		{
			System.out.println("\n\n\n----------------------");
			System.out.println("Founders:    " + founders);
			System.out.println("nonFounders: " + nonfounders);
			System.out.println("Loci:        " + psLoci.getNumLoci());
			System.out.println("Total Nodes: " + ((psLoci.getNumLoci() * (2 * founders + 4 * nonfounders))	//gp,gm,sp,sm
												+ (psLoci.getNumPheno() * (founders + nonfounders))));		//p
			System.out.println("----------------------\n\n\n");
		}
	}


	protected Person initFamily(int i) {
		Person per = (Person)pedigreeData.get(i);

		if(per.isFounder()) {
			per.depth = 0;
			per.father = null;
			per.mother = null;
		}
		else {
			per.father = (Person)pedigreeData.get(per.fatherID);
			per.mother = (Person)pedigreeData.get(per.motherID);
			if(per.father == null) {
				throw new IllegalStateException("ERROR (pedigree file): Could not find person " + per.fatherID);
			}
			if(per.mother==null) {
				throw new IllegalStateException("ERROR (pedigree file): Could not find person " + per.motherID);
			}
		}


		for(int locus=0; locus<per.paternalHaplotype.length; locus++) {
			per.paternalHaplotype[locus].setLength(psLoci.getNumAlleles(locus));
			per.maternalHaplotype[locus].setLength(psLoci.getNumAlleles(locus));
		}
		return per;
	}


	protected int calcDepth(Person per) {
		if(per.depth>=0) { return per.depth;}
		else {
			int dp = calcDepth(per.father);
			int dm = calcDepth(per.mother);
			if(dp>dm) {per.depth=dp+1;}
			else {per.depth=dm+1;}
			return per.depth;
		}
	}


	protected void generateNFamilies() {

		LinkedList allChildren = new LinkedList();
		LinkedList famChildren = new LinkedList();

		for(int p=1; p<pedigreeData.size(); p++) {
			Person father = (Person)pedigreeData.get(p);

			//generate NuclearFamily object(s) from fathers, as each family has exactly one
			if(father.male && father.firstSib>0) {

				allChildren.clear();
				getPaChildren(allChildren, father.firstSib);

				while(!allChildren.isEmpty()) {
					Person mother = null;
					famChildren.clear();

					for(Iterator itr_c = allChildren.iterator(); itr_c.hasNext();) {
						Person ch = (Person)itr_c.next();

						if(mother==null || mother==ch.mother) {//if haven't started family yet, or this child matches
							mother = ch.mother;//set family being looked for
							itr_c.remove();
							famChildren.add(ch);
						}
					}
					//have found all children of this family, create family
					NuclearFamily nf = new NuclearFamily(father, mother, (Person[])famChildren.toArray(new Person[famChildren.size()]));
					//add to person objects
					father.parInFam.add(nf);
					mother.parInFam.add(nf);
					for(int c=0; c<nf.child.length; c++) {
						if(nf.child[c].childInFam != null) { throw new IllegalStateException("ERROR (pedigree file): " + nf.child[c].childInFam + " and " + nf);}
						nf.child[c].childInFam = nf;
					}

					nuclearFamilies.add(nf);

				}//end while still have more children (multiple marriages)
			}//end find father
		}//end for each person
	}//end generateNFamilies



	protected void getPaChildren(LinkedList children, int firstID) {
		int nextID = firstID;
		while(nextID!=0) {
			Person per = (Person)pedigreeData.get(nextID);
			if(per==null) {
				throw new IllegalStateException("ERROR (pedigree file): Could not find person " + nextID);
			}
			children.add(per);
			nextID = per.nextPaternalSib;
		}
	}










	protected void simplify() {
		if(parChiOrder==null) { parChiOrder=createParentChildOrder();}

		if(DEBUG_PedigreeVerbose) { System.out.println("Simplifying Preprocess");}

		//for each numbered locus, and each person, simplify genotype based on phenotype
		for(int locus=0; locus<psLoci.getNumLoci(); locus++) {
			if(psLoci.getLociType(locus)==3) {//numbered allele
				for(int p=0; p<parChiOrder.length; p++) {
					Person per = parChiOrder[p];
					per.paternalHaplotype[locus].reduceNumberedFromPhenotype(per.phenotype[locus]);
					per.maternalHaplotype[locus].reduceNumberedFromPhenotype(per.phenotype[locus]);
					per.learnPhenoLocally(locus);
				}
			}
		}

		//PreProcess: for each person, reduce them based on each parent
		for(int locus=0; locus<psLoci.getNumLoci(); locus++) {
			for(int p=0; p<parChiOrder.length; p++) {
				Person per = parChiOrder[p];
				if(!per.isFounder()) {
					per.paternalHaplotype[locus].reduceHaplotypeFromParents(per.father.paternalHaplotype[locus],
																		    per.father.maternalHaplotype[locus]);
					per.maternalHaplotype[locus].reduceHaplotypeFromParents(per.mother.paternalHaplotype[locus],
																		    per.mother.maternalHaplotype[locus]);
					per.learnPhenoLocally(locus);
				}
			}
		}


		boolean saveAllele[][][];//[0:father,1:mother,2:children][0:paternal,1:maternal][allele]
		boolean forcedAllele[][];//[0:father,1:mother,2:children][allele]
		{//create all dimensions except last one, do that below since it depends on allele length
			saveAllele = new boolean[3][][];
			saveAllele[0] = new boolean[2][];
			saveAllele[1] = new boolean[2][];
			saveAllele[2] = new boolean[2][];
			forcedAllele = new boolean[3][];
		}


		for(int locus=0; locus<psLoci.getNumLoci(); locus++) {
			if(DEBUG_PedigreeVerbose) { System.out.println("Simplifying Locus: " + locus);}

			{
				int numAlleles = psLoci.getNumAlleles(locus);
				if(saveAllele[0][0]==null || saveAllele[0][0].length != numAlleles) {
					for(int i=0; i<saveAllele.length; i++) {
						for(int j=0; j<saveAllele[i].length; j++) {
							saveAllele[i][j] = new boolean[numAlleles];
						}
					}
					for(int i=0; i<forcedAllele.length; i++) {
						forcedAllele[i] = new boolean[numAlleles];
					}
				}
			}

			HashSet needsUpdate = new HashSet(nuclearFamilies);
			while(needsUpdate.size()>0) {
				NuclearFamily nf = (NuclearFamily)needsUpdate.iterator().next();
				nf.simplifyUpdate_Full(locus, saveAllele, forcedAllele, needsUpdate);
			}


			//TODO REMOVE?
			if(DEBUG_Test2) {//verify all hasChanged are fixed
				for(Iterator itr_nf = nuclearFamilies.iterator(); itr_nf.hasNext();) {
					NuclearFamily nf = (NuclearFamily)itr_nf.next();
					if(nf.hasChanged[0] || nf.hasChanged[1] || nf.hasChanged[2]) {
						System.err.println(nf + " had a person with hasChanged after simplify.");
						throw new IllegalStateException(nf + " had a person with hasChanged after simplify.");
					}
				}
			}
		}//simplify each locus
	}//end simplify


	/**The returned array contains all parents earlier than their children.*/
	protected Person[] createParentChildOrder() {
		Person ret[] = new Person[pedigreeData.size()-1];

		int nextIndx=0;
		int currDepth=0;

		while(nextIndx<ret.length) {
			for(int i=1; i<pedigreeData.size(); i++) {
				Person p = (Person)pedigreeData.get(i); //should always find one since skipping 0
				if(p.depth == currDepth) {ret[nextIndx++]=p;}
			}
			currDepth++;
			if(currDepth > pedigreeData.size()) {throw new IllegalStateException("INTERNAL PEDIGREE ERROR");} //just to be careful
		}
		return ret;
	}



	protected void alleleRecode() {
		if(parChiOrder==null) { parChiOrder=createParentChildOrder();}

		int numLoci = psLoci.getNumLoci();

		//Initialize
		for(int pi=0; pi<parChiOrder.length; pi++) {
			Person per = parChiOrder[pi];

			for(int locus=0; locus<numLoci; locus++) {
				Person.TransmittedAllele ta = new Person.TransmittedAllele(per.paternalHaplotype[locus].allele.length);
				per.transmittedAllele[locus] = ta;

				if(per.phenotype[locus].fullyKnown()) {//person is (pheno)typed, mark any allele as transmitted
					for(int a=0; a<ta.transmitted.length; a++) {
						if(per.paternalHaplotype[locus].allele[a] || per.maternalHaplotype[locus].allele[a]) {
							ta.transmitted[a] = true;
						}
						else {
							ta.transmitted[a] = false;
						}
					}

					//if this phenotype is for a complex disease (2-loci disease) then also set the transmitted for the first genotype location
					if(psLoci.isComplex2(locus)) {
						int complexLoci1 = psLoci.getComplex1InputOrd(locus);
						Person.TransmittedAllele cta = per.transmittedAllele[complexLoci1];

						for(int a=0; a<cta.transmitted.length; a++) {
							if(per.paternalHaplotype[complexLoci1].allele[a] || per.maternalHaplotype[complexLoci1].allele[a]) {
								cta.transmitted[a] = true;
							}
							else {
								cta.transmitted[a] = false;
							}
						}
					}
				}
				else {
					Arrays.fill(per.transmittedAllele[locus].transmitted, false);
				}
			}
		}

		//Mark
		for(int pi=parChiOrder.length-1; pi>=0; pi--) { //for each person (child before parent)
			Person per = parChiOrder[pi];


			for(int locus=0; locus<numLoci; locus++) { //for each locus
				for(int a=0; a<per.paternalHaplotype[locus].allele.length; a++) {
					if(per.father!=null) {
						if(per.paternalHaplotype[locus].allele[a] && per.transmittedAllele[locus].transmitted[a]) {
							if(per.father.paternalHaplotype[locus].allele[a] || per.father.maternalHaplotype[locus].allele[a]) {
								per.father.transmittedAllele[locus].transmitted[a] = true;
							}
						}
					}
					if(per.mother!=null) {
						if(per.maternalHaplotype[locus].allele[a] && per.transmittedAllele[locus].transmitted[a]) {
							if(per.mother.paternalHaplotype[locus].allele[a] || per.mother.maternalHaplotype[locus].allele[a]) {
								per.mother.transmittedAllele[locus].transmitted[a] = true;
							}
						}
					}
				}//each allele
			}//each locus
		}//each person
	}//end alleleRecode




	//allows for maping one variable to another, including remapping the instances of them
	static private class MyFV {
		final String id; //may not match fv.getID if this is a mapping
		final FiniteVariable fv; //This contains the cpt...
		final String states[]; //null if it matches fv, otherwise the mapped instances

		MyFV(FiniteVariable fv) {
			if(fv==null) { throw new IllegalArgumentException();}
			this.id = fv.getID();
			this.fv = fv;
			this.states = null;
		}

		MyFV(String id, FiniteVariable fv, String states[]) {
			if(id==null || fv==null) { throw new IllegalArgumentException("Error: id="+id+ " fv="+fv);}
			if(states!=null && states.length != fv.size()) { throw new IllegalArgumentException("States: " + Arrays.asList(states) + " != Variable: " + fv.instances());}
			this.id = id;
			this.fv = fv;
			this.states = states;
		}

		String instance(int indx) {
			if(states!=null) {
				return states[indx];
			}
			else {
				return (String)fv.instance(indx);
			}
		}

		int size() {return fv.size();}

		int index(String state) {
			if(states==null) { return fv.index(state);}
			else {
				for(int i=0; i<states.length; i++) {
					if(state.equals(states[i])) { return i;}
				}
				return -1;
			}
		}

		public String toString() {return "MyFV: " + id + "->" + fv + "   " + (states==null ? fv.instances().toString() : Arrays.asList(states).toString());}
	}


	static private class VarMap {
		final private HashMap idToMyVar = new HashMap();
		final private HashMap fvToColOfMyVar = new HashMap();

		public void add(MyFV v) {
			if(idToMyVar.put(v.id, v) != null) throw new IllegalStateException(v.id + " was already mapped to another variable.");
			Collection col = (Collection)fvToColOfMyVar.get(v.fv);
			if(col==null) {
				col = new HashSet();
				fvToColOfMyVar.put(v.fv, col);
			}
			col.add(v);
		}
		public FiniteVariable idToFV(String id) {
			MyFV v = (MyFV)idToMyVar.get(id);
			if(v!=null) return v.fv;
			else return null;
		}
		public MyFV idToMyFV(String id) {
			MyFV v = (MyFV)idToMyVar.get(id);
			return v;
		}
		public void removeFVAndMappings(FiniteVariable fv) {
			Collection col = (Collection)fvToColOfMyVar.remove(fv);
			if(col!=null) {
				for(Iterator itr = col.iterator(); itr.hasNext();) {
					MyFV mfv = (MyFV)itr.next();
					idToMyVar.remove(mfv.id);
				}
			}
		}
	}


	static public class GeneticNetwork {
		final public BeliefNetwork bn = new BeliefNetworkImpl();
		final private HashSet varsToRemove = new HashSet();
		final public int scalar; //only thing currently scaled is constantVal
		final public Pedigree ped;

		private double constantValReg=1; //for leaf nodes which become "independent", merge them, but need to scale it as well (store A if val is A^scalar)
		final private NumberedConstant constantValS1m;
		final private NumberedConstant constantValS1f;
		final private NumberedConstant constantValS2m;
		final private NumberedConstant constantValS2f;

		final HashSet changingRecomS1m = new HashSet(); //stores the actual finite variable objects
		final HashSet changingRecomS1f = new HashSet();
		final HashSet changingRecomS2m = new HashSet();
		final HashSet changingRecomS2f = new HashSet();
		final HashMap changingRecomCPTs = new HashMap(); //map from fv to ArrayList of NumberedConstants


		final private VarMap varMap = new VarMap();
		final private HashMap evid = (PREPROCESS==ALL ? null : new HashMap()); //Map from varID to Evidence.  When preprocessing all, don't need to store this in a file

		final HashSet equivPreProcessVars = new HashSet();
		public Set equivPreProcessVars() { return Collections.unmodifiableSet(equivPreProcessVars);}

		public GeneticNetwork(int scalar, Pedigree ped) {
			if(scalar <= 0) { throw new IllegalStateException("ERROR: Scalar = " + scalar);}
			this.scalar = scalar;
			this.ped = ped;

			int s1 = ped.psLoci.getChangingS1(); //returns locus where s value is changing, one higher than indexing in recom array
			s1--;
			if(s1>=0) {
				constantValS1m = new NumberedConstant(ped.psLoci.getRecombM(null, s1));
				if(DEBUG_PreprocessRules) {System.out.println("RULE: Constant for S1m has recom value of " + constantValS1m.recomValue + " for locus " + (s1+1));}
				if(ped.psLoci.getRecombM(null, s1)!=ped.psLoci.getRecombF(null, s1)) {
					constantValS1f = new NumberedConstant(ped.psLoci.getRecombF(null, s1));
					if(DEBUG_PreprocessRules) {System.out.println("RULE: Constant for S1f has recom value of " + constantValS1f.recomValue + " for locus " + (s1+1));}
				}
				else constantValS1f = null;
			}
			else {
				constantValS1m = null;
				constantValS1f = null;
			}

			int s2 = ped.psLoci.getChangingS2();
			s2--;
			if(s2>=0) {
				constantValS2m = new NumberedConstant(ped.psLoci.getRecombM(null, s2));
				if(DEBUG_PreprocessRules) {System.out.println("RULE: Constant for S2m has recom value of " + constantValS2m.recomValue + " for locus " + (s2+1));}
				if (ped.psLoci.getRecombM(null, s2) != ped.psLoci.getRecombF(null, s2)) {
					constantValS2f = new NumberedConstant(ped.psLoci.getRecombF(null, s2));
					if(DEBUG_PreprocessRules) {System.out.println("RULE: Constant for S2f has recom value of " + constantValS2f.recomValue + " for locus " + (s2+1));}
				}
				else constantValS2f = null;
			}
			else {
				constantValS2m = null;
				constantValS2f = null;
			}
		}



		public Set[] familyOrdering() {

			HashSet notDone = new HashSet(ped.nuclearFamilies);

			ArrayList peopleHere = new ArrayList(); //array of sets

			while(!notDone.isEmpty()) {

				HashSet famWithFinalChildren = new HashSet();
				{
					for(Iterator itr1 = notDone.iterator(); itr1.hasNext();) {
						NuclearFamily nf = (NuclearFamily)itr1.next();
						boolean childrenAllFinal = true;

						//see if any children are part of families not already finished
						for(int i=0; childrenAllFinal && i<nf.child.length; i++) {
							for(Iterator itr2 = nf.child[i].parInFam.iterator(); itr2.hasNext();) {
								NuclearFamily nfch = (NuclearFamily)itr2.next();
								if(notDone.contains(nfch)) {
									childrenAllFinal = false;
									break;
								}
							}
						}
						if(childrenAllFinal) famWithFinalChildren.add(nf);
					}
				}

				//in general will eliminate all of these, however a parent could have multiple marriages to another family either not finished or with non-final children
				boolean madeChange = true;
				while(madeChange) {
					madeChange = false;
					for(Iterator itr1 = famWithFinalChildren.iterator(); itr1.hasNext();) {
						NuclearFamily nf1 = (NuclearFamily)itr1.next();
						boolean skip = false;

						for(Iterator itr2 = nf1.father.parInFam.iterator(); !skip && itr2.hasNext();) {
							NuclearFamily nf2 = (NuclearFamily)itr2.next();
							if(!notDone.contains(nf2)) {}//ok
							else if(famWithFinalChildren.contains(nf2)){}//ok
							else {//bad
								skip = true;
								itr1.remove();
								madeChange = true;
								break;
							}
						}

						for(Iterator itr2 = nf1.mother.parInFam.iterator(); !skip && itr2.hasNext();) {
							NuclearFamily nf2 = (NuclearFamily)itr2.next();
							if(!notDone.contains(nf2)) {}//ok
							else if(famWithFinalChildren.contains(nf2)){}//ok
							else {//bad
								skip = true;
								itr1.remove();
								madeChange = true;
								break;
							}
						}
					}
				}

				HashSet here = new HashSet();

				//now eliminate all families with final children (eliminate parents if they don't have ancestors)
				for(Iterator itr1 = famWithFinalChildren.iterator(); itr1.hasNext();) {
					NuclearFamily nf1 = (NuclearFamily)itr1.next();
					for(int i=0; i<nf1.child.length; i++) here.add(new Integer(nf1.child[i].personID));
					if(nf1.father.fatherID==0 && nf1.father.motherID==0) here.add(new Integer(nf1.father.personID));
					if(nf1.mother.fatherID==0 && nf1.mother.motherID==0) here.add(new Integer(nf1.mother.personID));
					notDone.remove(nf1);
				}

				peopleHere.add(here);

			}//while !done

			Set ret[] = new Set[peopleHere.size()];
			for(int i=0; i<ret.length; i++) {
				ret[i] = new HashSet();
			}

			for(Iterator itr = bn.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				String id = fv.getID();

				if(id.startsWith("duplicate_")) id = id.substring(10);
				else if(id.startsWith("equiv_")) id = id.substring(6);


				String per = null;
				String loc = null;

				if(id.startsWith("Gp") || id.startsWith("Gm") || id.startsWith("Sp") || id.startsWith("Sm")) id = id.substring(2);
				else if(id.startsWith("merged_G")) id = id.substring(8);
				else if(id.startsWith("P")) id = id.substring(1);
				else throw new IllegalStateException("Could not parse: " + fv.getID());

				int under = id.indexOf('_');
				if(under < 0) throw new IllegalStateException("Could not parse: " + fv.getID());

				per = id.substring(0, under);
				loc = id.substring(under+1);

				Integer perInt = new Integer(per);

				for(int i=0; i<peopleHere.size(); i++) {
					if(((Set)peopleHere.get(i)).contains(perInt)) {
						ret[i].add(fv);
					}
				}
			}

			if(DEBUG_Pedigree) System.out.println("\n\nFamily Ordering: " + peopleHere + "\n\n");
			return ret;
		}

		public Set[] locusOrdering() {
			Set ret[] = new Set[ped.psLoci.getNumLoci()];
			for(int i=0; i<ret.length; i++) ret[i] = new HashSet();
			for(Iterator itr = bn.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				ret[getLocus(fv.getID())].add(fv);
			}

			return ret;
		}

		public Set[] reverseLocusOrdering() {
			Set[] tmp = locusOrdering();
			Set[] ret = new Set[tmp.length];
			for(int i=0; i<ret.length; i++) {
				ret[i] = tmp[tmp.length-i-1];
			}
			return ret;
		}

		public Set[] dynamicBNOrderingVEPD() {
			Set ret[] = new Set[ped.psLoci.getNumLoci()];
			for(int i=0; i<ret.length; i++) ret[i] = new HashSet();
			for(Iterator itr1 = bn.iterator(); itr1.hasNext();) {
				FiniteVariable par = (FiniteVariable)itr1.next();
				int maxLocus = getLocus(par.getID());

				for(Iterator itr2 = bn.outGoing(par).iterator(); itr2.hasNext();) {
					FiniteVariable chi = (FiniteVariable)itr2.next();
					int chiLocus = getLocus(chi.getID());

					if(chiLocus > maxLocus) maxLocus = chiLocus;
				}

				ret[maxLocus].add(par);
			}

			return ret;
		}

		private int getLocus(String varNameIn) {
			String varName = varNameIn;

			if(varName.startsWith("duplicate_")) varName = varName.substring(10);
			else if(varName.startsWith("equiv_")) varName = varName.substring(6);

			if(varName.startsWith("Gp") || varName.startsWith("Gm") || varName.startsWith("Sp") || varName.startsWith("Sm")) varName = varName.substring(2);
			else if(varName.startsWith("merged_G")) varName = varName.substring(8);
			else if(varName.startsWith("P")) varName = varName.substring(1);
			else throw new IllegalStateException("Could not parse: " + varNameIn);

			int under = varName.indexOf('_');
			if(under < 0) throw new IllegalStateException("Could not parse: " + varNameIn);

			String loc = varName.substring(under+1);
			return Integer.parseInt(loc);
		}



		void addEvidencePositive(String fvID, Object state) {
			if(PREPROCESS==ALL) { throw new IllegalStateException();}
			Evidence e = (Evidence)evid.get(fvID);
			if(e==null) {
				e=new Evidence(fvID);
				evid.put(fvID, e);
			}
			e.addPositive(state);
		}
		void addEvidenceNegative(String fvID, Object state) {
			if(PREPROCESS==ALL) { throw new IllegalStateException();}
			Evidence e = (Evidence)evid.get(fvID);
			if(e==null) {
				e=new Evidence(fvID);
				evid.put(fvID, e);
			}
			e.addNegative(state);
		}

		boolean hasPosEvidence(FiniteVariable fv) {
			if(PREPROCESS==ALL) {
				return fv.size()==1;
			}
			else {
				Evidence e = (Evidence)evid.get(fv.getID());
				if(e==null) return false;
				else return e.hasPositive();
			}
		}


		String getPosEvidence(String fvID) {
			if(PREPROCESS==ALL) {
				MyFV v = varMap.idToMyFV(fvID);
				if(v==null) return null;
				else if(v.size()==1) return v.instance(0);
				else return null;
			}
			else {
				MyFV v = varMap.idToMyFV(fvID);
				if(v==null) return null;

				Evidence ev = (Evidence)evid.get(v.fv.getID());
				if(ev==null) { return null;}
				else if(v.states==null) { return (String)ev.posState;}
				else { return v.states[v.fv.index(ev.posState)];}
			}
		}

		/**Currently only supports writing positive evidence.*/
		public void writeEvidToFile(PrintStream outEvid) {
			if(PREPROCESS==ALL) { throw new IllegalStateException("Cannot call writeEvidToFile with all preprocessing rules turned on.");}

			outEvid.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			outEvid.println("<instantiation date=\""+new Date()+"\">");
			for(Iterator itr=evid.values().iterator(); itr.hasNext();) {
				Evidence e = (Evidence)itr.next();
				if(e.hasPositive()) outEvid.println(e.evidFileString_Pos());
			}
			outEvid.println("</instantiation>");
			outEvid.flush();
		}
		public void writeEvidToFile_All(PrintStream outEvid) {
			if(PREPROCESS==ALL) { throw new IllegalStateException("Cannot call writeEvidToFile_All with all preprocessing rules turned on.");}

			outEvid.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			outEvid.println("<instantiation date=\""+new Date()+"\">");
			for(Iterator itr=evid.values().iterator(); itr.hasNext();) {
				Evidence e = (Evidence)itr.next();
				outEvid.println(e.evidFileString_All());
			}
			outEvid.println("</instantiation>");
			outEvid.flush();
		}

		public void outputChangingVariables(PrintStream out) {
			for(Iterator itr = changingRecomS1m.iterator(); itr.hasNext();)  {
				out.println(itr.next().toString());
			}
			for(Iterator itr = changingRecomS1f.iterator(); itr.hasNext();)  {
				out.println(itr.next().toString());
			}
			for(Iterator itr = changingRecomS2m.iterator(); itr.hasNext();)  {
				out.println(itr.next().toString());
			}
			for(Iterator itr = changingRecomS2f.iterator(); itr.hasNext();)  {
				out.println(itr.next().toString());
			}
		}

		void addVariable(MyFV fv) {
			if(!bn.addVariable(fv.fv, false)) { throw new IllegalStateException("!addVariable " + fv);}
			varMap.add(fv);
		}
		//This will remove the finite variable backed by myfv, including all mappings to it (be careful with this one)
		void removeVariableAndMappings(FiniteVariable fv) {
			varMap.removeFVAndMappings(fv);
			if(!bn.removeVariable(fv)) { throw new IllegalStateException("!removeVariable " + fv);}
			varsToRemove.remove(fv);
			if(evid!=null) evid.remove(fv.getID());

			changingRecomS1m.remove(fv);
			changingRecomS1f.remove(fv);
			changingRecomS2m.remove(fv);
			changingRecomS2f.remove(fv);
			changingRecomCPTs.remove(fv);
		}
		void createMapping(MyFV fv) {
			if(PREPROCESS<VALID_NET) { throw new IllegalStateException();}
			if(DEBUG_PedigreeVerbose) { System.out.println("Create Mapping: " + fv);}
			varMap.add(fv);

			if(evid!=null) {
				Evidence e_fake = (Evidence)evid.remove(fv.id);
				if(e_fake!=null) {
					String fvfvid = fv.fv.getID();
					if(e_fake.hasPositive()) addEvidencePositive(fvfvid, fv.fv.instance(fv.index((String)e_fake.posState)));
					else if(e_fake.negState!=null) { //technically don't need negative evidence if have positive evidence
						for(Iterator itre=e_fake.negState.iterator(); itre.hasNext();) {
							int ind = fv.index((String)itre.next());
							if(ind>=0) {
								addEvidenceNegative(fvfvid, fv.fv.instance(ind));
							}
						}
					}
				}
			}
		}
		MyFV getVar(String id) {
			return varMap.idToMyFV(id);
		}

		public boolean isChangingVar(FiniteVariable fv) {
			return changingRecomS1m.contains(fv) || changingRecomS1f.contains(fv) || changingRecomS2m.contains(fv) || changingRecomS2f.contains(fv);
		}
		private boolean areInSameChangingS(FiniteVariable f1, FiniteVariable f2) {
			if(changingRecomS1m.contains(f1) && changingRecomS1m.contains(f2)) return true;
			else if(changingRecomS1f.contains(f1) && changingRecomS1f.contains(f2)) return true;
			else if(changingRecomS2m.contains(f1) && changingRecomS2m.contains(f2)) return true;
			else if(changingRecomS2f.contains(f1) && changingRecomS2f.contains(f2)) return true;
			return false;
		}
		private void noLongerChanging(FiniteVariable fv) {
			changingRecomS1m.remove(fv);
			changingRecomS1f.remove(fv);
			changingRecomS2m.remove(fv);
			changingRecomS2f.remove(fv);
		}
		public void mergingTwoVars_OneIsChanging(FiniteVariable saving, FiniteVariable removing) {
			if(changingRecomS1m.contains(removing)) {
				changingRecomS1m.add(saving);
				changingRecomS1m.remove(removing);
			}
			else if(changingRecomS1f.contains(removing)) {
				changingRecomS1f.add(saving);
				changingRecomS1f.remove(removing);
			}
			else if(changingRecomS2m.contains(removing)) {
				changingRecomS2m.add(saving);
				changingRecomS2m.remove(removing);
			}
			else if(changingRecomS2f.contains(removing)) {
				changingRecomS2f.add(saving);
				changingRecomS2f.remove(removing);
			}
			else throw new IllegalStateException(removing + " was not a changing variable.");
		}
		public ArrayList getChangingVarAL(FiniteVariable fv) {
			return (ArrayList)changingRecomCPTs.get(fv);
		}
		public void addConstant(double val, FiniteVariable fvar) {
			if(val==0) { throw new IllegalArgumentException("Added a zero constant.");}

			if(Math.abs(1.0-val)>epsilon) {
				if(DEBUG_PedigreeVerbose) {
					System.out.println("Begin AddConstant " + val + " " + fvar + "\n" + constantValReg + "\n" + constantValS1m + "\n" + constantValS1f + "\n" + constantValS2m + "\n" + constantValS2f);
				}

				if(fvar==null) addConstantReg(val);
				else if(changingRecomS1m.contains(fvar)) constantValS1m.increment(val);
				else if(changingRecomS1f.contains(fvar)) constantValS1f.increment(val);
				else if(changingRecomS2m.contains(fvar)) constantValS2m.increment(val);
				else if(changingRecomS2f.contains(fvar)) constantValS2f.increment(val);
				else addConstantReg(val);

				if(DEBUG_PedigreeVerbose) {
					System.out.println("End AddConstant " + val + " " + fvar + "\n" + constantValReg + "\n" + constantValS1m + "\n" + constantValS1f + "\n" + constantValS2m + "\n" + constantValS2f);
				}
			}
		}
		public void addConstant(NumberedConstant nc, FiniteVariable fvar) {
			if(DEBUG_PedigreeVerbose) {
				System.out.println("Begin AddConstant (" + nc + ") " + fvar + "\n" + constantValReg + "\n" + constantValS1m + "\n" + constantValS1f + "\n" + constantValS2m + "\n" + constantValS2f);
			}

			if(changingRecomS1m.contains(fvar)) constantValS1m.increment(nc);
			else if(changingRecomS1f.contains(fvar)) constantValS1f.increment(nc);
			else if(changingRecomS2m.contains(fvar)) constantValS2m.increment(nc);
			else if(changingRecomS2f.contains(fvar)) constantValS2f.increment(nc);
			else throw new IllegalStateException(); //should not have used this function

			if(DEBUG_PedigreeVerbose) {
				System.out.println("End AddConstant (" + nc + ") " + fvar + "\n" + constantValReg + "\n" + constantValS1m + "\n" + constantValS1f + "\n" + constantValS2m + "\n" + constantValS2f);
			}
		}

		private void addConstantReg(double val) {
			if(val==0) { throw new IllegalArgumentException("Added a zero constant.");}
			double sval = (scalar!=1 ? Math.pow(val, 1.0/scalar) : val); //possibly scale the value
			if(DEBUG_PedigreeVerbose) { System.out.println("Constant Value * new Constant: " + constantValReg + " * " + val + " = " + (constantValReg*val));} //watch for underflow
			constantValReg *= sval;
			if(constantValReg==0) { throw new IllegalStateException("Underflow Occurred: Please increase the value of scalar.");}
		}

		public double constantScaled() {
			return constantValReg *
				(constantValS1m==null ? 1 : constantValS1m.getValScaled(scalar)) *
				(constantValS1f==null ? 1 : constantValS1f.getValScaled(scalar)) *
				(constantValS2m==null ? 1 : constantValS2m.getValScaled(scalar)) *
				(constantValS2f==null ? 1 : constantValS2f.getValScaled(scalar));
		}

		/**Returns the set of variables which had their cpts updated.*/
		public Collection changeRecomS1(double newSValMale, double newSValFemale) {
			for(Iterator itr = changingRecomS1m.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				if(!bn.contains(fv)) continue; //may be in constant

				Table cpt = fv.getCPTShell().getCPT();
				ArrayList al = getChangingVarAL(fv);
				if(al.size()==0) {
					System.out.println("cpt: " + cpt);
					throw new IllegalStateException("Didn't find arraylist for " + fv);
				}
				else {
					if(cpt.getCPLength()!=al.size()) throw new IllegalStateException("Sizes don't match " + cpt.getCPLength() + " != " + al.size() + " " + fv);
					for(int i=0; i<cpt.getCPLength(); i++) {
						NumberedConstant nc = (NumberedConstant)al.get(i);
						nc.recomValue = newSValMale;
						cpt.setCP(i,nc.getValReal());
					}
				}
			}
			for(Iterator itr = changingRecomS1f.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				if(!bn.contains(fv)) continue; //may be in constant

				Table cpt = fv.getCPTShell().getCPT();
				ArrayList al = getChangingVarAL(fv);
				if(al.size()==0) {
					System.out.println("cpt: " + cpt);
					throw new IllegalStateException("Didn't find arraylist for " + fv);
				}
				else {
					if(cpt.getCPLength()!=al.size()) throw new IllegalStateException("Sizes don't match " + cpt.getCPLength() + " != " + al.size() + " " + fv);
					for(int i=0; i<cpt.getCPLength(); i++) {
						NumberedConstant nc = (NumberedConstant)al.get(i);
						nc.recomValue = newSValFemale;
						cpt.setCP(i,nc.getValReal());
					}
				}
			}
			if(constantValS1m!=null) constantValS1m.recomValue = newSValMale;
			if(constantValS1f!=null) constantValS1f.recomValue = newSValFemale;
			HashSet ret = new HashSet(changingRecomS1m);
			ret.addAll(changingRecomS1f);
			return ret;
		}

		/**Returns the set of variables which had their cpts updated.*/
		public Collection changeRecomS2(double newSValMale, double newSValFemale) {
			for(Iterator itr = changingRecomS2m.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				if(!bn.contains(fv)) continue; //may be in constant

				Table cpt = fv.getCPTShell().getCPT();
				ArrayList al = getChangingVarAL(fv);
				if(al.size()==0) {
					System.out.println("cpt: " + cpt);
					throw new IllegalStateException("Didn't find arraylist for " + fv);
				}
				else {
					if(cpt.getCPLength()!=al.size()) throw new IllegalStateException("Sizes don't match " + cpt.getCPLength() + " != " + al.size() + " " + fv);
					for(int i=0; i<cpt.getCPLength(); i++) {
						NumberedConstant nc = (NumberedConstant)al.get(i);
						nc.recomValue = newSValMale;
						cpt.setCP(i,nc.getValReal());
					}
				}
			}
			for(Iterator itr = changingRecomS2f.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				if(!bn.contains(fv)) continue; //may be in constant

				Table cpt = fv.getCPTShell().getCPT();
				ArrayList al = getChangingVarAL(fv);
				if(al.size()==0) {
					System.out.println("cpt: " + cpt);
					throw new IllegalStateException("Didn't find arraylist for " + fv);
				}
				else {
					if(cpt.getCPLength()!=al.size()) throw new IllegalStateException("Sizes don't match " + cpt.getCPLength() + " != " + al.size() + " " + fv);
					for(int i=0; i<cpt.getCPLength(); i++) {
						NumberedConstant nc = (NumberedConstant)al.get(i);
						nc.recomValue = newSValFemale;
						cpt.setCP(i,nc.getValReal());
					}
				}
			}
			if(constantValS2m!=null) constantValS2m.recomValue = newSValMale;
			if(constantValS2f!=null) constantValS2f.recomValue = newSValFemale;
			HashSet ret = new HashSet(changingRecomS2m);
			ret.addAll(changingRecomS2f);
			return ret;
		}
	}


	public GeneticNetwork createBeliefNetwork(int scalar) {

		HashMap peopleNeedingPhaseRemoval_MG = new HashMap();

		GeneticNetwork ret = new GeneticNetwork(scalar, this);
		{
			if(parChiOrder==null) { parChiOrder=createParentChildOrder();}

			if(DEBUG_Pedigree) { System.out.println("Creating Variables");}

			{
				ArrayList statesP = new ArrayList();
				ArrayList statesM = new ArrayList();

				for(int locusChromoOrd=0; locusChromoOrd<psLoci.getNumLoci(); locusChromoOrd++) {
					int locusInputOrd = psLoci.inputOrd(locusChromoOrd)-1; //zero based for array indexing

					for(int pi=0; pi<parChiOrder.length; pi++) { //must have parents before children

						createVars(ret, parChiOrder[pi], locusChromoOrd, locusInputOrd, statesP, statesM, peopleNeedingPhaseRemoval_MG);
					}
				}
			}

			{
				if(DEBUG_Pedigree) { System.out.println("Cleaning up Variables");}

				//IMPORTANT: once cleanup starts, don't use MyFV objects anymore, as they are not
				//  updated during the simplifications/cleanup...  (e.g. when states are removed
				//  they are not removed from mapped variables...)

				if (PREPROCESS == NONE && ret.varsToRemove.size() > 0) throw new IllegalStateException();

				{//clean up variables with no cpts, or removed for some other reason
					for(Iterator itrv = ret.varsToRemove.iterator(); itrv.hasNext();) {
						Object var = itrv.next();
						itrv.remove();
						ret.removeVariableAndMappings((FiniteVariable)var);
						if(DEBUG_PreprocessRules) {System.out.println("RULE: Removing Variable " + var);}
					}
				}

				HashSet varsI = new HashSet(ret.bn);
				boolean doneCleaning = false;
				boolean doneWithPhaseRemoval = false;

				while(!doneCleaning) {
					doneCleaning = true;

					for(Iterator itrfv = varsI.iterator(); itrfv.hasNext();) {
						FiniteVariable fvar = (FiniteVariable)itrfv.next();

						if(!ret.bn.contains(fvar)) { itrfv.remove(); continue;} //possibly already removed (e.g. it became a leaf after its child was removed)

						int inDeg = ret.bn.inDegree(fvar);
						int outDeg = ret.bn.outDegree(fvar);

						//look for independent variables
						if(inDeg==0 && outDeg==0 && PREPROCESS>=LEAF_PRUNE) {

							if(ret.getChangingVarAL(fvar)==null) {

								if(DEBUG_PreprocessRules) {System.out.println("RULE: Independent Variable: " + fvar);} //usually merged genotypes or S node

								double con = 0;
								Table cpt = fvar.getCPTShell().getCPT();

								if(DEBUG_PedigreeVerbose2) {System.out.println("Independent Variable "+fvar+" CPT: \n" + cpt);}
								if(cpt.variables().size()!=1) throw new IllegalStateException("The CPT of " + fvar + " was not really independent.\n" + cpt);

								Evidence ev = (Evidence)(ret.evid==null ? null : ret.evid.get(fvar.getID()));

								if(ev==null) {
									for(int i=cpt.getCPLength()-1; i>=0; i--) {
										con += cpt.getCP(i);
									}
								}
								else if(ev.hasPositive()) {
									int stindx = fvar.index(ev.posState);
									con += cpt.getCP(stindx);
								}
								else if(ev.hasNegative()) {
									TableIndex tblIndx = cpt.index();
									int mindx[] = null;
									int loc_f = tblIndx.variableIndex(fvar);

									for(int i=cpt.getCPLength()-1; i>=0; i--) {
										mindx = tblIndx.mindex(i, mindx);
										Object state = fvar.instance(mindx[loc_f]);

										if(!ev.negState.contains(state)) {
											con += cpt.getCP(i);
										}
									}
								}

								if(Math.abs(1.0-con)>epsilon || ret.isChangingVar(fvar)) { //not equal to 1
									ret.addConstant(con, fvar);
								}
								ret.removeVariableAndMappings(fvar);

								itrfv.remove();
								//this won't lead to other reductions, therefore don't update doneCleaning
								continue;
							}
						}

						//look for leaf nodes with no evidence or if all cpt entries are 1.0
						if(outDeg==0 && PREPROCESS>=LEAF_PRUNE) { //leaf node
							if(tryLeafRemoval(fvar, ret)) {
								itrfv.remove();
								//this won't lead to other reductions, therefore don't update doneCleaning
								//actually this could lead to reductions in CPT independence or ValueElim based on
								//merging all values into 1, however its not worth the overhead of another pass
								continue;
							}
						}

						//Value Elimination
						if(PREPROCESS==ALL) {
							TableShell ts = (TableShell)fvar.getCPTShell();
							ArrayList change = ts.valueElimination();
							if(change!=null) {
								if(DEBUG_PreprocessRules) {System.out.println("RULE: Value Elimination: table of " + fvar + " remove " + change);}

								//make variable changes
								for(int i=0; i<change.size(); i+=2) {
									FiniteVariable frem = (FiniteVariable)change.get(i);
									Object sta = change.get(i+1);
									ret.bn.removeState(frem, frem.index(sta));

									updateChangingAL_reduce(frem.getCPTShell().getCPT(), ret.getChangingVarAL(frem));

									for(Iterator itrchi = ret.bn.outGoing(frem).iterator(); itrchi.hasNext();) {
										FiniteVariable chi = (FiniteVariable)itrchi.next();

										updateChangingAL_reduce(chi.getCPTShell().getCPT(), ret.getChangingVarAL(chi));
									}
								}

								//make structure changes
								TableIndex tblIndx = ts.index();
								for(int v=tblIndx.getNumVariables()-1; v>=0; v--) {//for each variable (parent or child)
									FiniteVariable var = tblIndx.variable(v);

									if(var.size()==1) { //variable was reduced to only a single state, remove from table (if parent)

										if(ret.bn.inDegree(var)==0) { //par will be disconnected, make constant
											ArrayList al = ret.getChangingVarAL(var);
											if(al==null) ret.addConstant(var.getCPTShell().getCP(0), var);
											else {
												ret.addConstant((NumberedConstant)al.get(0), var);
												if(al.size()!=1) throw new IllegalStateException();
											}
											ret.removeVariableAndMappings(var);
											if(DEBUG_PreprocessRules) {System.out.println("RULE: Value Elimination Disconnect: " + var);}
										}

										if(ret.bn.outDegree(var)!=0) {
											HashSet children = new HashSet(ret.bn.outGoing(var));
											for(Iterator itr_c = children.iterator(); itr_c.hasNext();) {
												FiniteVariable chi = (FiniteVariable)itr_c.next();
												if(!ret.bn.removeEdge(var, chi)) { throw new IllegalStateException("!removeEdge: " + var + " -> " + chi);}
											}
										}

										if(DEBUG_PreprocessRules) {System.out.println("RULE: Value Elimination Single State: " + var);}
									}
								}//for each table variable

								doneCleaning = false;
								if(!ret.bn.contains(fvar)) continue;
							}//end changed
						}//end value elimination

						//CPT independence
						//  This rule is not guaranteed to be safe when doing multiple computations after changing the
						//    recombination values.  The independence could be a combination of multiple recombination
						//    values and changing them could "break" the independence.
						if(PREPROCESS>=VALID_NET && !ret.isChangingVar(fvar)) {
							Table tbl = fvar.getCPTShell().getCPT();
							TableIndex tblIndx = tbl.index();
							int tblSize = tblIndx.size();

							FiniteVariable parents[] = tblIndx.getParents(); //if parent is necessary, then remove it.  If it is not removed, it is uncessary.
							int numLeft = parents.length;
							int blkSz[] = new int[parents.length];
							for(int i=0; i<blkSz.length; i++) {blkSz[i]=tblIndx.blockSize(i);}
							boolean done=false;

							for(int cptI=0; cptI<tblSize && (!done); cptI++) {
								double cpAtI = tbl.getCP(cptI);

								for(int parI=0; parI<parents.length&&(!done); parI++) {
									if(parents[parI]==null) { continue;}

									if((cptI+blkSz[parI] < tblSize) && (cpAtI != tbl.getCP(cptI+blkSz[parI]))) {
										parents[parI]=null;
										numLeft--;
										if(numLeft==0) {done=true;} //all parents are necessary
									}
								}
							}

							ArrayList al = ret.getChangingVarAL(fvar);
							for(int parI=0; parI<parents.length; parI++) {
								if(parents[parI]!=null) {
									if(DEBUG_PreprocessRules) {System.out.println("RULE: CPT Independence: removed " + parents[parI] + " from " + fvar);}

									if(!ret.bn.removeEdge(parents[parI], fvar)) { throw new IllegalStateException("!removeEdge: " + parents[parI] + " -> " + fvar);}
									updateChangingAL_reduce(fvar.getCPTShell().getCPT(), al);

									doneCleaning = false;
								}
							}
						}//end CPT Independence


						//look for variables with only a single child
						//This may or may not be helpful.
						//it could hurt by making the children contain more parents than previously, but it may help since it
						//  reduces the number of variables and if we have a "chain" of selectors or something like that they will be
						//  compressed.
						//If need probabilites in the roots, need to be careful with this, as this could merge those into children,
						//  which may may not be roots.
						if(doneWithPhaseRemoval) {
							if(PREPROCESS>=VALID_NET) {
								if(outDeg==1 && inDeg<=1) {
									if(doSingleChildRemoval(fvar, ret)) {
										itrfv.remove();
										doneCleaning = false; //could lead to more
										continue;
									}
								}
							}
						}
					}//end for each variable

					if(doneCleaning==false) continue; //make sure have done all preprocessing before doing phase removal

					if(!doneWithPhaseRemoval) {
						doneWithPhaseRemoval = true;
						doneCleaning=false; //at the very least, go back and try single child removal
					}

					if(PREPROCESS>=VALID_NET) {
						if(DEBUG_PreprocessRules && !peopleNeedingPhaseRemoval_MG.isEmpty()) {System.out.println("RULE: Doing Phase Removal");}
						for(Iterator itrPar = peopleNeedingPhaseRemoval_MG.keySet().iterator(); itrPar.hasNext();) {

							//if got in here, not done yet
							doneCleaning = false;

							Integer parIDint = (Integer)itrPar.next();
							Collection possiblePlaces = (Collection)peopleNeedingPhaseRemoval_MG.get(parIDint);

							if(DEBUG_PedigreeVerbose) System.out.println("\n\n\nPHASE REMOVAL for " + parIDint + " is possible at: " + possiblePlaces);

							//find variables still in network
							for(Iterator itrPos = possiblePlaces.iterator(); itrPos.hasNext();) {
								String name = (String)itrPos.next();

								MyFV myv = ret.getVar(name);
								FiniteVariable v = null;
								if(myv != null) v = (FiniteVariable)ret.bn.forID(myv.fv.getID());
								if(myv==null || v==null) itrPos.remove();
							}

							if(possiblePlaces.isEmpty()) {
								if(DEBUG_PreprocessRules) System.out.println("RULE: Phase Removal didn't find any children of founder " + parIDint + " (possible founder removal)");
								continue;
							}


							FiniteVariable mgPhaseRemoval = null;
							double score = -1;
							double score2 = -1;
							for(Iterator itrPos = possiblePlaces.iterator(); itrPos.hasNext();) {
								String name = (String)itrPos.next();

								MyFV myv = ret.getVar(name);
								FiniteVariable v = (FiniteVariable)ret.bn.forID(myv.fv.getID());

								//find best var to remove phase at
								{
									Collection children = ret.bn.outGoing(v);
									int numChi = children.size();
									int numChiWEvid = 0;
									for(Iterator itrc=children.iterator(); itrc.hasNext();) {
										if(ret.hasPosEvidence((FiniteVariable)itrc.next())) numChiWEvid++;
									}

									if(mgPhaseRemoval==null) {
										mgPhaseRemoval=v;
										score=numChiWEvid;
										score2=numChi;
									}
									else if(v.size()==2) {
										if(mgPhaseRemoval.size()!=2) {
											mgPhaseRemoval=v;
											score=numChiWEvid;
											score2=numChi;
										}
										else {
											if(numChiWEvid > score || (numChiWEvid==score && numChi > score2)) {
												mgPhaseRemoval=v;
												score=numChiWEvid;
												score2=numChi;
											}
										}
									}
									else if(mgPhaseRemoval.size()!=2) {
										if(numChiWEvid > score || (numChiWEvid==score && numChi > score2)) {
											mgPhaseRemoval=v;
											score=numChiWEvid;
											score2=numChi;
										}
									}
								}
							}//possibilities

							if(mgPhaseRemoval==null) throw new IllegalStateException(); //did not find a variable to remove

							//do phase removal
							if(DEBUG_PreprocessRules) {System.out.println("RULE: Phase Removal for parent " + parIDint + " at " + mgPhaseRemoval);}


							{
								//merged var only has 2 states, so learn evidence on it
								if(mgPhaseRemoval.size()==2) {
									ret.bn.removeState(mgPhaseRemoval, 1);
									updateChangingAL_reduce(mgPhaseRemoval.getCPTShell().getCPT(), ret.getChangingVarAL(mgPhaseRemoval));
									{
										//this resutls in learning evid on it, so remove outgoing edges
										Collection children = new HashSet(ret.bn.outGoing(mgPhaseRemoval));
										for(Iterator itrc = children.iterator(); itrc.hasNext();) {
											FiniteVariable chi = (FiniteVariable)itrc.next();
											if(!ret.bn.removeEdge(mgPhaseRemoval, chi)) { throw new IllegalStateException("!removeEdge: " + mgPhaseRemoval + " -> " + chi);}

											//possibly update getChangingVarAL
											updateChangingAL_reduce(chi.getCPTShell().getCPT(), ret.getChangingVarAL(chi));
										}
									}
									ret.addConstant(2, null); //double the probability for phase removal
								}
								else {

									FiniteVariable chiWithEvid = null;
									//if any of mgPhaseRemoval's children have evidence, just learn evidence on selector (same as removing founder's phase, only easier)
									for(Iterator itrc = ret.bn.outGoing(mgPhaseRemoval).iterator(); itrc.hasNext();) {
										FiniteVariable ch = (FiniteVariable)itrc.next();
										if(ret.hasPosEvidence(ch) && ch.getID().startsWith("G")) {
											chiWithEvid = ch;
											break;
										}
									}

									//found child with evidence, simply learn evidence on this child's Selector variable
									if(chiWithEvid!=null) {
										FiniteVariable sPhaseRemoval = null;
										for(Iterator itrp = ret.bn.inComing(chiWithEvid).iterator(); itrp.hasNext();) {
											FiniteVariable pa = (FiniteVariable)itrp.next();
											if(pa.getID().startsWith("S")) {
												sPhaseRemoval = pa;
												break;
											}
										}

										if(sPhaseRemoval==null) throw new IllegalStateException("No selector in " + ret.bn.inComing(chiWithEvid)); //didn't find Selector
										if(DEBUG_PreprocessRules) {System.out.println("RULE: Phase Removal for parent " + parIDint + " moved to " + sPhaseRemoval);}

										ret.bn.removeState(sPhaseRemoval, 1);
										updateChangingAL_reduce(sPhaseRemoval.getCPTShell().getCPT(), ret.getChangingVarAL(sPhaseRemoval));
										if(sPhaseRemoval.size()!=1) throw new IllegalStateException("" + sPhaseRemoval + " " + sPhaseRemoval.instances());
										{
											//this results in learning evid on it, so remove outgoing edges
											Collection children = new HashSet(ret.bn.outGoing(sPhaseRemoval));
											for(Iterator itrc = children.iterator(); itrc.hasNext();) {
												FiniteVariable chi = (FiniteVariable)itrc.next();
												if(!ret.bn.removeEdge(sPhaseRemoval, chi)) { throw new IllegalStateException("!removeEdge: " + sPhaseRemoval + " -> " + chi);}

												//possibly update getChangingVarAL
												updateChangingAL_reduce(chi.getCPTShell().getCPT(), ret.getChangingVarAL(chi));
											}
										}
										ret.addConstant(2, null); //double the probability for phase removal
									}
									else {
										//otherwise remove phase in founder and then adjust one child's cpt

										HashSet states = new HashSet(mgPhaseRemoval.instances());
										ArrayList al = ret.getChangingVarAL(mgPhaseRemoval);
										for(Iterator itrs = states.iterator(); itrs.hasNext();) {
											String st = (String)itrs.next();
											stateParser.parse(st);

											if(stateParser.indx1 > stateParser.indx2) { //remove it
												int idx = mgPhaseRemoval.index(st);
												ret.bn.removeState(mgPhaseRemoval, idx);
												if(al!=null) al.remove(idx);
											}
											else if(stateParser.indx1 < stateParser.indx2) { //double it
												int idx = mgPhaseRemoval.index(st);
												mgPhaseRemoval.getCPTShell().getCPT().setCP(idx, mgPhaseRemoval.getCPTShell().getCPT().getCP(idx)*2);
												if(al!=null) {
													NumberedConstant nc = (NumberedConstant)al.get(idx);
													nc.setAdditionalConst(2.0*nc.getAdditionalConst());
												}
											}
										}

										if(PREPROCESS>=ALL && ret.bn.outDegree(mgPhaseRemoval)>0) { //if doing all, choose a child and for homozygous founder, force S to be L or R

											//find any "normal G" or "merged_G" child
											FiniteVariable chi = null;
											for(Iterator itrc = ret.bn.outGoing(mgPhaseRemoval).iterator(); itrc.hasNext();) {
												FiniteVariable c = (FiniteVariable)itrc.next();
												if(c.getID().startsWith("G") || c.getID().startsWith("m")) {
													chi = c;
													break;
												}
											}
											if(chi==null) {
												System.out.println("Removed phase at: " + mgPhaseRemoval);
												System.out.println("However couldn't find child for homozygous removal: " + ret.bn.outGoing(mgPhaseRemoval));
//TODO
												//throw new IllegalStateException(); //did not find "normal" child
											}
											else {
												HashSet homozygousStatesInFounder = new HashSet();
												for(int i=0; i<mgPhaseRemoval.size(); i++) {
													stateParser.parse(mgPhaseRemoval.instance(i));
													if(stateParser.indx1==stateParser.indx2) {
														homozygousStatesInFounder.add(new Integer(i));
													}
												}

												Table cpt = chi.getCPTShell().getCPT();
												TableIndex ind = cpt.index();
												int parIndx = ind.variableIndex(mgPhaseRemoval);
												int selIndx = -1;

												{
													for(int i=0; i<ind.getNumVariables(); i++) {
														FiniteVariable s = ind.variable(i);
														if(s.getID().startsWith("S")) {
															selIndx = i;
															break;
														}
													}
													if(selIndx<0) throw new IllegalStateException();
												}

												for(TableIndex.Iterator itrT = ind.iterator(); itrT.hasNext();) {
													int ci = itrT.next();
													Integer parVal = new Integer(itrT.current()[parIndx]);

													//Force selector into state 0
													if(homozygousStatesInFounder.contains(parVal)) {
														if(itrT.current()[selIndx]!=0) {
															cpt.setCP(ci, 0.0);
														}
														else {
															cpt.setCP(ci, cpt.getCP(ci)*2);
														}
													}
												}
											}
										}
									}
								}//end >2 states
							}//end phase removal
						}
						peopleNeedingPhaseRemoval_MG.clear();
					}
					else {
						if(DEBUG_PreprocessRules) {System.out.println("RULE: Not Doing Phase Removal");}
						peopleNeedingPhaseRemoval_MG.clear();
					}

				}//end doneCleaning

				if(!peopleNeedingPhaseRemoval_MG.isEmpty()) {
					System.err.println("Did not remove phase of all founders: " + peopleNeedingPhaseRemoval_MG);
					throw new IllegalStateException("Did not remove phase of all founders: " + peopleNeedingPhaseRemoval_MG);
				}
//TODO Uncomment code
//				if(varsI.size()<ret.bn.size()) {
//					HashSet tmp = new HashSet(varsI.size()>ret.bn.size() ? (Collection)varsI : (Collection)ret.bn);
//					tmp.removeAll(varsI.size()>ret.bn.size() ? (Collection)ret.bn : (Collection)varsI);
//					if(varsI.size()>ret.bn.size()) System.out.println("varsI had more:");
//					else System.out.println("ret.bn had more:");
//					System.out.println("extra vars:" + tmp);
//					throw new IllegalStateException();
//				}
			}
		}

		ret.equivPreProcessVars.retainAll(ret.bn);

		//TEST that everything was done correctly
		if(DEBUG_Test2 && PREPROCESS>=VALID_NET) {
			if(DEBUG_PedigreeVerbose) System.out.println("Testing");
			for(Iterator itr = ret.bn.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();

				Set inVars = ret.bn.inComing(fv);
				int in = inVars.size();
				int out = ret.bn.outDegree(fv);


				//test for evid, which is not on leaf (should be made leafs)
				if(ret.hasPosEvidence(fv) && out>0) { throw new IllegalStateException(fv + " has evidence, but outdegree " + out);}

//				//test for leaf's without evid (should be removed)
//				if(out==0 && !ret.hasPosEvidence(fv) && !ret.cannotLeafPrune.contains(fv)) { throw new IllegalStateException(fv + " is a leaf, but has  " + fv.size() + " states.");}

				//test for no-CPTShell (should have been removed)
				if(fv.getCPTShell()==null) {throw new IllegalStateException("No cpt shell: " + fv);}

				//look for disconnected variables (should be constants)
				if(in==0 && out==0 && !ret.isChangingVar(fv)) {throw new IllegalStateException("Disconnected variable: " + fv);}

				//look for leafs with only 1's in cpt
				if(out==0) {
					boolean onlyOnes=true;
					Table cpt = fv.getCPTShell().getCPT();
					for(int i=0; i<cpt.getCPLength(); i++) {
						if(cpt.getCP(i)!=1.0) {onlyOnes=false;}
					}
					if(onlyOnes) { throw new IllegalStateException("Leaf with only 1.0. " + fv);}
				}

				//verify CPTs match up with BN
				{
					TableIndex tblIndx = fv.getCPTShell().index();
					HashSet parents = tblIndx.getParentsSet();

					if(!parents.equals(inVars)) { throw new IllegalStateException("CPT parents for " + fv + " do not equal Network parents. CPT:" + parents + " BN:" + inVars);}
				}

				//Look for CPT independence
				if(!ret.isChangingVar(fv)) {
					TableIndex tblIndx = fv.getCPTShell().index();
					Table tbl = fv.getCPTShell().getCPT();
					int tblSize = tblIndx.size();
					FiniteVariable parents[] = tblIndx.getParents(); //if parent is necessary, then remove it.  If it is not removed, it is uncessary.
					int numLeft = parents.length;
					int blkSz[] = new int[parents.length];
					for(int i=0; i<blkSz.length; i++) { blkSz[i] = tblIndx.blockSize(i);}
					boolean done=false;

					for(int cptI=0; cptI<tblSize&&(!done); cptI++) {
						double cpAtI = tbl.getCP(cptI);

						for(int parI=0; parI<parents.length&&(!done); parI++) {
							if(parents[parI]==null) { continue;}

							if((cptI+blkSz[parI] < tblSize) && (cpAtI != tbl.getCP(cptI+blkSz[parI]))) {
								parents[parI]=null;
								numLeft--;
								if(numLeft==0) {done = true;} //all parents are necessary
							}
						}
					}

					for(int parI=0; parI<parents.length; parI++) {
						if(parents[parI]!=null) {
							System.out.println("CPT Independence\n" + tbl);
							throw new IllegalStateException("Variable " + fv + " is actually independent of " + parents[parI] + " but it is still a parent in the CPT.");
						}
					}
				}

			}
			if(DEBUG_PedigreeVerbose) System.out.println("End Testing");
		}


		if(ret.evid != null) {
			Map ev = new HashMap();
			for(Iterator itrE = ret.evid.values().iterator(); itrE.hasNext();) {
				Evidence e = (Evidence)itrE.next();
				FiniteVariable fv = (FiniteVariable)ret.bn.forID(e.fvID);
				if(fv==null) throw new IllegalStateException("Evidence File Error: Var: " + e.fvID + " does not exist");

				if(e.hasPositive()) {
					ev.put(ret.bn.forID(e.fvID), e.posState);
				}
			}
			try{
				ret.bn.getEvidenceController().observe(ev);
			}
			catch(StateNotFoundException e) {
				throw new IllegalStateException(e.toString());
			}
		}

		return ret;
	}



	//both locusChromoOrd and locusInputOrd are 0 based.
	private void createVars(GeneticNetwork gn, Person per, int locusChromoOrd, int locusInputOrd,
			ArrayList statesP, ArrayList statesM, Map peopleNeedingPhaseRemoval_MG) {

		Person.PossibleAllele patH = per.paternalHaplotype[locusInputOrd];
		Person.PossibleAllele matH = per.maternalHaplotype[locusInputOrd];
		Person.Phenotype pheno = per.phenotype[locusInputOrd];
		Person.TransmittedAllele ta = per.transmittedAllele[locusInputOrd];

		boolean founder = per.isFounder();

		statesP.clear();
		statesM.clear();
		double otherPrP = 0;
		double otherPrM = 0;
		ArrayList chiPatOtherStates = new ArrayList();
		ArrayList chiMatOtherStates = new ArrayList();

		String gpName = "Gp"+per.personID+"_"+locusChromoOrd;
		String gmName = "Gm"+per.personID+"_"+locusChromoOrd;


		{//First, determine which states are possible

			int numStP = 0; String stP=null; //use stP if numStP==1, then it contains the state
			int numStM = 0; String stM=null;

			for(int a=0; a<patH.allele.length; a++) {
				if(patH.allele[a]) {
					if(ta!=null && !ta.transmitted[a] && PREPROCESS>=VALID_NET && !(patH.numValidStates==1)) {
						chiPatOtherStates.add(String.valueOf(a));
						if(founder) {otherPrP += psLoci.geneFreq(locusInputOrd, a);}
					}
					else { stP = String.valueOf(a); statesP.add(stP); numStP++;}
				}
				else { //allele not possible
					if(PREPROCESS == ALL) {
					}
					else if(PREPROCESS == VALID_NET) { //can remove state for founders, but not others
						if(!founder) {
							statesP.add(String.valueOf(a));
							gn.addEvidenceNegative(gpName,String.valueOf(a));
						}
					}
					else if(PREPROCESS == LEAF_ADDITIONAL_EVID || PREPROCESS == LEAF_PRUNE) {
						statesP.add(String.valueOf(a));
						gn.addEvidenceNegative(gpName,String.valueOf(a));
					}
					else if(PREPROCESS == NONE) {
						statesP.add(String.valueOf(a));
					}
					else throw new IllegalStateException();
				}

				if(matH.allele[a]) {
					if(ta!=null && !ta.transmitted[a] && PREPROCESS>=VALID_NET && !(matH.numValidStates==1)) {
						chiMatOtherStates.add(String.valueOf(a));
						if(founder) {otherPrM += psLoci.geneFreq(locusInputOrd, a);}
					}
					else { stM = String.valueOf(a); statesM.add(stM); numStM++;}
				}
				else { //allele not possible
					if(PREPROCESS == ALL) {
					}
					else if(PREPROCESS == VALID_NET) { //can remove state for founders, but not others
						if(!founder) {
							statesM.add(String.valueOf(a));
							gn.addEvidenceNegative(gmName,String.valueOf(a));
						}
					}
					else if(PREPROCESS == LEAF_ADDITIONAL_EVID || PREPROCESS == LEAF_PRUNE) {
						statesM.add(String.valueOf(a));
						gn.addEvidenceNegative(gmName,String.valueOf(a));
					}
					else if(PREPROCESS == NONE) {
						statesM.add(String.valueOf(a));
					}
					else throw new IllegalStateException();
				}
			}//for each allele

			if(chiPatOtherStates.size()>0) {statesP.add(other); numStP++; stP = other;}
			if(chiMatOtherStates.size()>0) {statesM.add(other); numStM++; stM = other;}

			if(PREPROCESS>LEAF_PRUNE && PREPROCESS<ALL) {
				if(statesP.size()==1) {gn.addEvidencePositive(gpName, statesP.get(0));} //could be other, but won't catch things when neg states are included
				else if(patH.numValidStates==1) {gn.addEvidencePositive(gpName, String.valueOf(patH.validState));}
				if(statesM.size()==1) {gn.addEvidencePositive(gmName, statesM.get(0));} //could be other, but won't catch things when neg states are included
				else if(matH.numValidStates==1) {gn.addEvidencePositive(gmName, String.valueOf(matH.validState));}
			}
		}//end determine which states are possible

		if(DEBUG_PedigreeVerbose) {System.out.println("Possible States for " + gpName + ": " + statesP + " " + gmName + ": " + statesM);}

		//FATHER
		double fatherRemovedPr[] = null;
		if(per.father!=null) {
			//do founder removal
			//  if parent is a founder (which implies its variables were merged) and only has 1 child,
			//  it may be able to be removed
			MyFV fatherMerged = gn.getVar("merged_G"+per.father.personID+"_"+locusChromoOrd);
			if(PREPROCESS>=VALID_NET && pheno.type!=4 && per.father.numChildren()<=1 &&
				fatherMerged!=null && gn.bn.inDegree(fatherMerged.fv)==0 && gn.bn.outDegree(fatherMerged.fv)==0 && //(outDeg=0 because of var mapping; it could have an = child with the other variable having incomming edges)
				satisfiesFounderRemoval(fatherMerged, gn)) {
				//remove parent's merged genotypes and this person will not need selectors (and will possibly be a founder themselves)

				if(DEBUG_PreprocessRules) {System.out.println("RULE: Founder Removal: " + fatherMerged);}

				boolean otherOther = canOtherInheritOther(patH, ta, per.father.paternalHaplotype[locusInputOrd], per.father.transmittedAllele[locusInputOrd]);
				fatherRemovedPr = new double[statesP.size()];
				gn.removeVariableAndMappings(fatherMerged.fv);

				handleFounderRemoval(fatherMerged, statesP, fatherRemovedPr, otherOther, chiPatOtherStates, gn);
				fatherMerged = null;
			}
		}

		//MOTHER
		double motherRemovedPr[] = null;
		if(per.mother!=null) {
			//do founder removal
			//  if parent is a founder (which implies its variables were merged) and only has 1 child,
			//  it may be able to be removed
			MyFV motherMerged = gn.getVar("merged_G"+per.mother.personID+"_"+locusChromoOrd);
			if(PREPROCESS>=VALID_NET && pheno.type!=4 && per.mother.numChildren()<=1 &&
				motherMerged!=null && gn.bn.inDegree(motherMerged.fv)==0 && gn.bn.outDegree(motherMerged.fv)==0 && //(outDeg=0 because of var mapping)
				satisfiesFounderRemoval(motherMerged, gn)) {
				//remove parent's merged genotypes and this person will not need selectors (and will possibly be a founder themselves)

				if(DEBUG_PreprocessRules) {System.out.println("RULE: Founder Removal: " + motherMerged);}

				boolean otherOther = canOtherInheritOther(matH, ta, per.mother.paternalHaplotype[locusInputOrd], per.mother.transmittedAllele[locusInputOrd]);
				motherRemovedPr = new double[statesM.size()];
				gn.removeVariableAndMappings(motherMerged.fv);

				handleFounderRemoval(motherMerged, statesM, motherRemovedPr, otherOther, chiMatOtherStates, gn);
				motherMerged = null;
			}
		}

		//look for parents (possibly just removed)
		MyFV fatherGm = (per.father==null ? null : gn.getVar("Gm"+per.father.personID+"_"+locusChromoOrd));
		MyFV fatherGp = (per.father==null ? null : gn.getVar("Gp"+per.father.personID+"_"+locusChromoOrd));
		MyFV fatherMerged = (per.father==null ? null : gn.getVar("merged_G"+per.father.personID+"_"+locusChromoOrd));

		MyFV motherGm = (per.mother==null ? null : gn.getVar("Gm"+per.mother.personID+"_"+locusChromoOrd));
		MyFV motherGp = (per.mother==null ? null : gn.getVar("Gp"+per.mother.personID+"_"+locusChromoOrd));
		MyFV motherMerged = (per.mother==null ? null : gn.getVar("merged_G"+per.mother.personID+"_"+locusChromoOrd));

		if(fatherGm==fatherGp && fatherGm!=null && fatherMerged==null) throw new IllegalStateException("Merge didn't exist: "+fatherGm);
		if(motherGm==motherGp && motherGm!=null && motherMerged==null) throw new IllegalStateException("Merge didn't exist: "+motherGm);


		if(PREPROCESS >= VALID_NET &&
			fatherGm==null && fatherGp==null && fatherMerged==null &&
			motherGm==null && motherGp==null && motherMerged==null) {

			//Found a FOUNDER (either a true founder, or one created by founder removals)
			//  so created a merged_G variable, along with P (if necessary, possibly included in merged_G).

			if(!statesP.equals(statesM)) {
				System.err.println("ERROR: Issue with founder:");
				System.err.println("pat: " + statesP);
				System.err.println("mat: " + statesM);
				throw new IllegalStateException("ERROR: Issue with founder: " + per.personID);
			}

			//no evidence is allowed on founders, this is taken care of by variable merge
			if(gn.evid!=null) {
				Evidence ev_gp = (Evidence)gn.evid.remove(gpName);
				Evidence ev_gm = (Evidence)gn.evid.remove(gmName);

				if(ev_gp!=null) {
					if(ev_gp.hasPositive()) {statesP.clear(); statesP.add(ev_gp.posState);}
					else if(ev_gp.hasNegative()) {statesP.removeAll(ev_gp.negState);}
				}
				if(ev_gm!=null) {
					if(ev_gm.hasPositive()) {statesM.clear(); statesM.add(ev_gm.posState);}
					else if(ev_gm.hasNegative()) {statesM.removeAll(ev_gm.negState);}
				}
			}

			{ //Setup Possible Phase Removal
				Integer perIDint = new Integer(per.personID);
				Collection possiblePlaces = (Collection)peopleNeedingPhaseRemoval_MG.get(perIDint);
				if(possiblePlaces==null) {
					possiblePlaces = new ArrayList();
					peopleNeedingPhaseRemoval_MG.put(perIDint, possiblePlaces);
				}
				possiblePlaces.add("merged_G"+per.personID+"_"+locusChromoOrd);
			}

			FiniteVariable mg = new FiniteVariableImpl("merged_G"+per.personID+"_"+locusChromoOrd, combine(statesP, statesM, pheno));
			MyFV mymg = new MyFV(mg);
			gn.addVariable(mymg); //add variable to network

			if(DEBUG_PreprocessRules) {System.out.println("Variables " + gpName + " and " + gmName + " are founders and being merged into " + mymg);}

			double data[] = new double[mg.size()];

			//if only have 1 or 2 states, then create mappings for Gp and Gm, to make other code easier
			if(mg.size()<=2) {
				String statesp[] = new String[mg.size()];
				String statesm[] = new String[mg.size()];

				for(int i=0; i<mg.size(); i++) {
					stateParser.parse(mg.instance(i));
					statesp[i] = stateParser.s1;
					statesm[i] = stateParser.s2;
				}

				MyFV gpf = new MyFV(gpName, mg, statesp);
				MyFV gpm = new MyFV(gmName, mg, statesm);
				gn.createMapping(gpf);
				gn.createMapping(gpm);
			}

			for(int i=0; i<data.length; i++) {
				stateParser.parse(mg.instance(i));
				double v1;
				double v2;

				if(fatherRemovedPr!=null) {v1=fatherRemovedPr[statesP.indexOf(stateParser.s1)];}
				else if(stateParser.indx1==-1) {v1=otherPrP;}
				else {v1=psLoci.geneFreq(locusInputOrd, stateParser.indx1);}

				if(motherRemovedPr!=null) {v2=motherRemovedPr[statesM.indexOf(stateParser.s2)];}
				else if(stateParser.indx2==-1) {v2=otherPrM;}
				else {v2=psLoci.geneFreq(locusInputOrd, stateParser.indx2);}

				data[i] = v1 * v2;
			}

			//If it only has one state, don't add cpt, & will be removed later
			if(data.length > 1) {
				mg.setCPTShell(new TableShell( new Table(Collections.singletonList(mg), data)));

				if(DEBUG_PedigreeVerbose2) {System.out.println("Created "+mymg+" CPT: \n" + mymg.fv.getCPTShell().getCPT());}
			}
			else {//value known
				//this variable was added to the network w/o a CPT so that children can be created,
				// but it should be removed later, because this constant is all that is needed.
				gn.varsToRemove.add(mg);
				gn.addConstant(data[0], mymg.fv);

				if(DEBUG_PedigreeVerbose2) {System.out.println("Created "+mymg+" CPT: " + data[0]);}
			}//end m_G value known


			//Create PHENOTYPE variable
			if(pheno.type==4) {
				if(psLoci.isComplex2(locusInputOrd)) {
					int firstComplexLociInputOrd = psLoci.getComplex1InputOrd(locusInputOrd);
					MyFV mg2 = gn.getVar("merged_G"+per.personID+"_"+psLoci.chromoOrd(firstComplexLociInputOrd));
					if(mg2==null) { throw new IllegalStateException("Could not find: " + "merged_G"+per.personID+"_"+psLoci.chromoOrd(firstComplexLociInputOrd));}
					createFounderP(gn, mymg, mg2, pheno, locusInputOrd, ("P"+per.personID+"_"+locusChromoOrd));
				}
			}
			else {
				createFounderP(gn, mymg, null, pheno, locusInputOrd, ("P"+per.personID+"_"+locusChromoOrd));
			}
		}
		else { //either non-founder or rules disallow merging
			//create Gp & Gm (or possibly merged_G) & P & Sp & Sm

			boolean otherOtherPP = true; //true if parent "other" is valid parent of child "other" (parent must be subset of child)
			boolean otherOtherPM = true;
			boolean otherOtherMP = true;
			boolean otherOtherMM = true;
			if(per.father!=null) {
				otherOtherPP = canOtherInheritOther(patH, ta, per.father.paternalHaplotype[locusInputOrd], per.father.transmittedAllele[locusInputOrd]);
				otherOtherPM = canOtherInheritOther(patH, ta, per.father.maternalHaplotype[locusInputOrd], per.father.transmittedAllele[locusInputOrd]);
			}
			if(per.mother!=null) {
				otherOtherMP = canOtherInheritOther(matH, ta, per.mother.paternalHaplotype[locusInputOrd], per.mother.transmittedAllele[locusInputOrd]);
				otherOtherMM = canOtherInheritOther(matH, ta, per.mother.maternalHaplotype[locusInputOrd], per.mother.transmittedAllele[locusInputOrd]);
			}


			MyFV sp = null;
			MyFV sm = null;

			if(fatherGm!=null || fatherGp!=null || fatherMerged!=null) {
				sp = createS(statesP, (statesP.size()==1 ? (String)statesP.get(0) : gn.getPosEvidence(gpName)), fatherGp, fatherGm, fatherMerged,
					gn.getVar("Sp"+per.personID+"_"+(locusChromoOrd-1)), otherOtherPP, otherOtherPM, chiPatOtherStates, gn,
					"Sp"+per.personID+"_"+locusChromoOrd, locusChromoOrd, true);
			}
			if(motherGm!=null || motherGp!=null || motherMerged!=null) {
				sm = createS(statesM, (statesM.size()==1 ? (String)statesM.get(0) : gn.getPosEvidence(gmName)), motherGp, motherGm, motherMerged,
					gn.getVar("Sm"+per.personID+"_"+(locusChromoOrd-1)), otherOtherMP, otherOtherMM, chiMatOtherStates, gn,
					"Sm"+per.personID+"_"+locusChromoOrd, locusChromoOrd, false);
			}

			if(DEBUG_PedigreeVerbose) {System.out.println("Created S Variables " + sp + " and " + sm);}



			MyFV gp = null;
			MyFV gm = null;
			MyFV gpMappedTo = null;
			MyFV gmMappedTo = null;

			{
				boolean mappedGP = false;
				boolean mappedGM = false;

				if(sp!=null && gn.hasPosEvidence(sp.fv) && PREPROCESS>=VALID_NET) mappedGP = true;
				if(sm!=null && gn.hasPosEvidence(sm.fv) && PREPROCESS>=VALID_NET) mappedGM = true;

				if(mappedGP) {
					if(gn.getPosEvidence(sp.id).equals("1")) gpMappedTo = fatherGp;
					else gpMappedTo = fatherGm;

					if(gpMappedTo==null) throw new IllegalStateException("Could not find parent during mapping"); //possibly merged parent?

					gp = new MyFV(gpName, gpMappedTo.fv, gpMappedTo.states);
					if(DEBUG_PreprocessRules) {System.out.println("RULE: Variable Equivalence: " + gp);}
					gn.createMapping(gp);

					if(PREPROCESS==ALL) { //normal evidence is processed in createMapping, but state removal is checked here
						for(int i=0; i<gpMappedTo.size(); i++) {
							Object state = gpMappedTo.instance(i);
							if(!statesP.contains(state)) throw new IllegalStateException("Need to remove " + state + " from " + gpMappedTo);
						}
					}
				}

				if(mappedGM) {
					if(gn.getPosEvidence(sm.id).equals("1")) gmMappedTo = motherGp;
					else gmMappedTo = motherGm;

					if(gmMappedTo==null) throw new IllegalStateException("Could not find parent during mapping"); //possibly merged parent?

					gm = new MyFV(gmName, gmMappedTo.fv, gmMappedTo.states);
					if(DEBUG_PreprocessRules) {System.out.println("RULE: Variable Equivalence: " + gm);}
					gn.createMapping(gm);

					if(PREPROCESS==ALL) { //normal evidence is processed in createMapping, but state removal is checked here
						for(int i=0; i<gmMappedTo.size(); i++) {
							Object state = gmMappedTo.instance(i);
							if(!statesM.contains(state)) throw new IllegalStateException("Need to remove " + state + " from " + gmMappedTo);
						}
					}
				}
			}

			//quick debug test
			if(pheno.fullyKnown()) {
				//if pheno is known, either both P&M are known or both are unknown
				if((statesP.size()==1 && statesM.size()==1) ||
				   (statesP.size()>1  && statesM.size()>1)) {}
				else {
					throw new IllegalStateException("Phenotype was known, but states are : " + statesP + " " + statesM);
				}
			}

			//Horiz Merge for numbered locus with known phenotype and no evidence (only do with ALL, since it creates invalid CPTs)
			if(PREPROCESS>=ALL && pheno.type==3 && pheno.fullyKnown() && statesP.size()>1 && statesM.size()>1) {
				if(statesP.size()!=2) throw new IllegalStateException("Known phenotype, but statesP = " + statesP);
				if(statesM.size()!=2) throw new IllegalStateException("Known phenotype, but statesM = " + statesM);

				if(DEBUG_PreprocessRules) {System.out.println("RULE: Horizontal Mapping: " + gpName + " and " + gmName);}

				if(gpMappedTo!=null && gmMappedTo==null) {
					if(gp.size()!=2) throw new IllegalStateException();

					gn.createMapping(new MyFV("merged_G"+per.personID+"_"+locusChromoOrd, gp.fv, new String[]{gp.instance(0)+"_"+gp.instance(1), gp.instance(1)+"_"+gp.instance(0)}));
					moveInComing(gpMappedTo, gn);

					String states[] = {gp.instance(1), gp.instance(0)};
					gm = new MyFV(gmName, gp.fv, states);
					gn.createMapping(gm);

					//need to include parents of gm, but don't put them in gp, put them in "extra" variable (unless there are no parents)
					if(motherRemovedPr!=null) founderInclude(gm, motherRemovedPr, statesM);
					else if(sm!=null) {
						MyFV equivGm = createEquivVar(gm, gn);
						if(!addParents(equivGm, sm, motherGp, motherGm, motherMerged, otherOtherMP, otherOtherMM, chiMatOtherStates, gn)) {
							//if no parents were added, remove equivGm & its child
							if(gn.bn.outDegree(equivGm.fv)==1) {
								gn.removeVariableAndMappings((FiniteVariable)gn.bn.outGoing(equivGm.fv).iterator().next());
							}
							gn.removeVariableAndMappings(equivGm.fv);
						}
					}
				}
				else if(gpMappedTo==null && gmMappedTo!=null) {
					if(gm.size()!=2) throw new IllegalStateException();

					gn.createMapping(new MyFV("merged_G"+per.personID+"_"+locusChromoOrd, gm.fv, new String[]{gm.instance(1)+"_"+gm.instance(0), gm.instance(0)+"_"+gm.instance(1)}));
					moveInComing(gmMappedTo, gn);

					String states[] = {gm.instance(1), gm.instance(0)};
					gp = new MyFV(gpName, gm.fv, states);
					gn.createMapping(gp);

					//need to include parents of gp, but don't put them in gm, put them in "extra" variable (unless there are no parents)
					if(fatherRemovedPr!=null) founderInclude(gp, fatherRemovedPr, statesP);
					else if(sp!=null) {
						MyFV equivGp = createEquivVar(gp, gn);
						if(!addParents(equivGp, sp, fatherGp, fatherGm, fatherMerged, otherOtherPP, otherOtherPM, chiPatOtherStates, gn)) {
							//if no parents were added, remove equivGp & its child
							if(gn.bn.outDegree(equivGp.fv)==1) {
								gn.removeVariableAndMappings((FiniteVariable)gn.bn.outGoing(equivGp.fv).iterator().next());
							}
							gn.removeVariableAndMappings(equivGp.fv);
						}
					}
				}
				else if(gpMappedTo!=null && gmMappedTo!=null) {
					//TODO;
					throw new UnsupportedOperationException();
				}
				else if(gpMappedTo==null && gmMappedTo==null) {

					MyFV mymg = new MyFV(new FiniteVariableImpl("merged_G"+per.personID+"_"+locusChromoOrd, combine(statesP, statesM, pheno)));
					gn.addVariable(mymg);

					if(mymg.size()!=2) throw new IllegalStateException();
					{
						String statesp[] = new String[2];
						String statesm[] = new String[2];
						for(int i=0; i<2; i++) {
							stateParser.parse(mymg.instance(i));
							statesp[i] = stateParser.s1;
							statesm[i] = stateParser.s2;
						}
						gp = new MyFV(gpName, mymg.fv, statesp);
						gm = new MyFV(gmName, mymg.fv, statesm);
						gn.createMapping(gp);
						gn.createMapping(gm);
					}

					mymg.fv.setCPTShell(new TableShell(new Table(Collections.singletonList(mymg.fv), new double[]{1.0, 1.0})));
					{
						//need to include parents of gm, but don't put them in gp, put them in "extra" variable (unless there are no parents)
						if(motherRemovedPr!=null) founderInclude(gm, motherRemovedPr, statesM);
						else if(sm!=null) {
							MyFV equivGm = createEquivVar(gm, gn);
							if(!addParents(equivGm, sm, motherGp, motherGm, motherMerged, otherOtherMP, otherOtherMM, chiMatOtherStates, gn)) {
								//if no parents were added, remove equivGm & its child
								if(gn.bn.outDegree(equivGm.fv)==1) {
									gn.removeVariableAndMappings((FiniteVariable)gn.bn.outGoing(equivGm.fv).iterator().next());
								}
								gn.removeVariableAndMappings(equivGm.fv);
							}
						}

						//need to include parents of gp, but don't put them in gm, put them in "extra" variable (unless there are no parents)
						if(fatherRemovedPr!=null) founderInclude(gp, fatherRemovedPr, statesP);
						else if(sp!=null) {
							MyFV equivGp = createEquivVar(gp, gn);
							if(!addParents(equivGp, sp, fatherGp, fatherGm, fatherMerged, otherOtherPP, otherOtherPM, chiPatOtherStates, gn)) {
								//if no parents were added, remove equivGp & its child
								if(gn.bn.outDegree(equivGp.fv)==1) {
									gn.removeVariableAndMappings((FiniteVariable)gn.bn.outGoing(equivGp.fv).iterator().next());
								}
								gn.removeVariableAndMappings(equivGp.fv);
							}
						}
					}
				}
				else throw new IllegalStateException();
			}
			else {
				//not horizontally merged, so created regular Gp & Gm

				//Create GP
				if(gp==null) {
					gp = new MyFV(new FiniteVariableImpl(gpName, statesP));
					gn.addVariable(gp);
					if(DEBUG_PedigreeVerbose) {System.out.println("Created Variable " + gp);}

					//Create CPT
					if(sp==null) { //no parent
						double data[] = new double[gp.size()];
						for(int i=0; i<data.length; i++) {
							String state = gp.instance(i);

							if(fatherRemovedPr!=null) data[i] = fatherRemovedPr[statesP.indexOf(state)];
							else if(state.equals(other)) data[i] = otherPrP;
							else data[i] = psLoci.geneFreq(locusInputOrd, Integer.parseInt(state));
						}
						if(DEBUG_PedigreeVerbose && fatherRemovedPr!=null) {
							System.out.println("Created Variable " + gp + " with parent removed ");
							for(int i=0; i<fatherRemovedPr.length; i++) {
								System.out.print(fatherRemovedPr[i] + ", ");
							}
							System.out.println("");
						}
						gp.fv.setCPTShell(new TableShell(new Table(Collections.singletonList(gp.fv), data)));
					}
					else {
						double data[] = new double[gp.size()];
						Arrays.fill(data, 1.0);
						gp.fv.setCPTShell(new TableShell(new Table(Collections.singletonList(gp.fv), data)));

						addParents(gp, sp, fatherGp, fatherGm, fatherMerged, otherOtherPP, otherOtherPM, chiPatOtherStates, gn);
					}
				}

				//Create GM
				if(gm==null) {
					gm = new MyFV(new FiniteVariableImpl(gmName, statesM));
					gn.addVariable(gm);
					if(DEBUG_PedigreeVerbose) {System.out.println("Created Variable " + gm);}

					//Create CPT
					if(sm==null) { //no parent
						double data[] = new double[gm.size()];
						for(int i=0; i<data.length; i++) {
							String state = gm.instance(i);

							if(motherRemovedPr!=null) data[i] = motherRemovedPr[statesM.indexOf(state)];
							else if(state.equals(other)) data[i] = otherPrM;
							else data[i] = psLoci.geneFreq(locusInputOrd, Integer.parseInt(state));
						}
						gm.fv.setCPTShell(new TableShell(new Table(Collections.singletonList(gm.fv), data)));
					}
					else {
						double data[] = new double[gm.size()];
						Arrays.fill(data, 1.0);
						gm.fv.setCPTShell(new TableShell(new Table(Collections.singletonList(gm.fv), data)));

						addParents(gm, sm, motherGp, motherGm, motherMerged, otherOtherMP, otherOtherMM, chiMatOtherStates, gn);
					}
				}


				//create P
				if(!(psLoci.isComplex1(locusInputOrd))) { //don't add P for first complex loci
					MyFV gp2 = null;
					MyFV gm2 = null;
					if(psLoci.isComplex2(locusInputOrd)) {
						gp2 = gn.getVar("Gp" + per.personID + "_" + psLoci.getComplex1InputOrd(locusInputOrd));
						gm2 = gn.getVar("Gm" + per.personID + "_" + psLoci.getComplex1InputOrd(locusInputOrd));

						if (gp2 == null) { throw new IllegalStateException("Could not find: " + "Gp" + per.personID + "_" + psLoci.getComplex1InputOrd(locusInputOrd)); }
						if (gm2 == null) { throw new IllegalStateException("Could not find: " + "Gm" + per.personID + "_" + psLoci.getComplex1InputOrd(locusInputOrd)); }
					}
					createNonFounderP(gn, pheno, gp, gm, gp2, gm2, locusInputOrd, per.male, ("P"+per.personID+"_"+locusChromoOrd));
				}
			}
		}
	}//end createVars



	//combine c1 & c2 states into "joint state" (merged genotype or a phenotype variable)
	//the result is a list of states which are not illegal based on the phenotype passed in (possibly null)
	//if allStates is true then 1_2 != 2_1, otherwise they are considered the same (phenotype)
	static private List combine(Collection c1, Collection c2, Person.Phenotype pheno) {
		if(pheno!=null && pheno.type!=3) {
			pheno=null;
		} //can only reduce with numbered phenotypes

		List ret = new LinkedList();
		for(Iterator i_c1=c1.iterator(); i_c1.hasNext();) {
			String s1 = i_c1.next().toString();
			int v1 = ((pheno==null || s1.equals(other)) ? -1 : Integer.parseInt(s1));

			for(Iterator i_c2=c2.iterator(); i_c2.hasNext();) {
				String s2 = i_c2.next().toString();
				int v2 = ((pheno==null || s2.equals(other)) ? -1 : Integer.parseInt(s2));

				if(pheno!=null) {
					int p1 = pheno.one();
					int p2 = pheno.two();

					if(p2 >= 0) { //know both
						if(!((v1==p1 && v2==p2) || (v1==p2 && v2==p1))) { continue;} //doesn't match
					}
					else if(p1 >= 0) { //know one
						if(!(p1==v1 || p1==v2)) { continue;} //doesn't match
					}
				}

				String ij = s1+"_"+s2;
				ret.add(ij);
			}
		}
		return ret;
	}


	private MyFV createS(ArrayList statesChi, String evidChi, MyFV parP, MyFV parM, MyFV parMer, MyFV prevS,
							boolean gpOtherOther, boolean gmOtherOther, Collection chiOther,
							GeneticNetwork gn, String sName,
							int locusChromoOrd, boolean paternal) {

		if(DEBUG_PedigreeVerbose2) {System.out.println("createS: " + sName);}

		double recomValue;
		{
			if(paternal) {
				recomValue = (locusChromoOrd<=0 ? -1 : psLoci.getRecombM(sName, locusChromoOrd-1));
			}
			else {
				recomValue = (locusChromoOrd<=0 ? -1 : psLoci.getRecombF(sName, locusChromoOrd-1));
			}
		}

		MyFV s = null;

		boolean include1 = false;
		boolean include2 = false;

		if(parP!=null && parM!=null && parP!=parM) {

			{
				MyFV par = parP;

				String evidPar = gn.getPosEvidence(par.id); boolean evidParB = (evidPar!=null);
				int size = (evidParB ? 1 : par.size());
				for(int i=0; i<size; i++) {
					if(!evidParB) evidPar = par.instance(i);

					if(evidPar.equals(other)) {//parent is other
						if(gpOtherOther && statesChi.contains(other) && (evidChi==null || evidChi.equals(other))) include1 = true;
					}
					else {
						if(statesChi.contains(evidPar) && (evidChi==null || evidChi.equals(evidPar))) include1 = true;
						else if(chiOther.contains(evidPar) && statesChi.contains(other) && (evidChi==null || evidChi.equals(other))) include1 = true;
					}

					if(include1) break;
				}
			}

			{
				MyFV par = parM;

				String evidPar = gn.getPosEvidence(par.id); boolean evidParB = (evidPar!=null);
				int size = (evidParB ? 1 : par.size());
				for(int i=0; i<size; i++) {
					if(!evidParB) evidPar = par.instance(i);

					if(evidPar.equals(other)) {//parent is other
						if(gmOtherOther && statesChi.contains(other) && (evidChi==null || evidChi.equals(other))) include2 = true;
					}
					else {
						if(statesChi.contains(evidPar) && (evidChi==null || evidChi.equals(evidPar))) include2 = true;
						else if(chiOther.contains(evidPar) && statesChi.contains(other) && (evidChi==null || evidChi.equals(other))) include2 = true;
					}

					if(include2) break;
				}
			}
		}
		else if(parMer!=null) {
			String evidPar = gn.getPosEvidence(parMer.id); boolean evidParB = (evidPar!=null);
			int size = (evidParB ? 1 : parMer.size());
			for(int i=0; i<size; i++) {
				if(!evidParB) evidPar = parMer.instance(i);

				stateParser.parse(evidPar);
				String evidGP = stateParser.s1;
				String evidGM = stateParser.s2;

				if(evidGP.equals(other)) {//parent is other
					if(gpOtherOther && statesChi.contains(other) && (evidChi==null || evidChi.equals(other))) include1 = true;
				}
				else {
					if(statesChi.contains(evidGP) && (evidChi==null || evidChi.equals(evidGP))) include1 = true;
					else if(chiOther.contains(evidGP) && statesChi.contains(other) && (evidChi==null || evidChi.equals(other))) include1 = true;
				}

				if(evidGM.equals(other)) {//parent is other
					if(gmOtherOther && statesChi.contains(other) && (evidChi==null || evidChi.equals(other))) include2 = true;
				}
				else {
					if(statesChi.contains(evidGM) && (evidChi==null || evidChi.equals(evidGP))) include2 = true;
					else if(chiOther.contains(evidGM) && statesChi.contains(other) && (evidChi==null || evidChi.equals(other))) include2 = true;
				}

				if(include1 && include2) break;
			}
		}
		else throw new IllegalStateException("Illegal configuration of variables: " + sName + " " + parP + " " + parM + " " + parMer);

		if(psLoci.sexLinked()) throw new UnsupportedOperationException();
//		if(loci.header().sexLinked==1 && (pheno.type==1 || pheno.type==4)) {
//			if(per.male && paternal) include2 = false; //Y gene must have ben from grandfather
//			else if((!per.male) && paternal) include1 = false; //paternal X must be from grandmother
//		}

		boolean hasEvidOnS = (include1==false || include2==false);

		if(DEBUG_PedigreeVerbose2) {System.out.println("createS: " + sName + " has Evid: " + hasEvidOnS);}

		if(PREPROCESS!=ALL) {
			if(!include1 && PREPROCESS>=LEAF_ADDITIONAL_EVID) {
				gn.addEvidencePositive(sName, states12[1]);
			}
			else if(!include2 && PREPROCESS>=LEAF_ADDITIONAL_EVID) {
				gn.addEvidencePositive(sName, states12[0]);
			}

			include1 = true;
			include2 = true;
		}


		if(PREPROCESS>=VALID_NET && include1 && include2 && evidChi!=null && (!evidChi.equals(other)) && //if evidChi==other, then it matches both parents & no link is necessary between them
			(parMer!=null ? isHeterozygous(parMer, gn.getPosEvidence(parMer.id)) : isHeterozygous(parP, parM, gn.getPosEvidence(parP.id), gn.getPosEvidence(parM.id)))) {
			//if don't know S value & chi is known & parent is heterozygous then merge S variables

			//determine inverted versus regular by the states of the merged parent
			boolean invertedS;
			FiniteVariable parFV;
			{
				if(parMer!=null) {
					stateParser.parse(parMer.instance(0));
					invertedS = evidChi.equals(stateParser.s2);
					parFV = parMer.fv;
				}
				else throw new IllegalStateException("There should always be a merged variable now");
//				else { //if parent's were horizontally merged (& one was mapped), may not have created a merged_G variable
//					invertedS = evidChi.equals(parM.instance(0));
//					parFV = parM.fv;
//				}
			}

			{
				s = new MyFV(sName, parFV, (invertedS ? states21 : states12));
				gn.createMapping(s);

				if(DEBUG_PreprocessRules) {System.out.println("RULE: S Equivalence: " + s);}

				if(prevS==null) {//no parent
					Table cpt = s.fv.getCPTShell().getCPT();
					for(int i=0; i<cpt.getCPLength(); i++) {
						cpt.setCP(i, cpt.getCP(i) * 0.5);
					}
				}
				else {
					MyFV equivS = createEquivVar(s, gn);

					if(locusChromoOrd==psLoci.getChangingS1()) {
						if(paternal) gn.changingRecomS1m.add(equivS.fv);
						else  gn.changingRecomS1f.add(equivS.fv);
						gn.changingRecomCPTs.put(equivS.fv, new ArrayList());
					}
					else if(locusChromoOrd==psLoci.getChangingS2()) {
						if(paternal) gn.changingRecomS2m.add(equivS.fv);
						else gn.changingRecomS2f.add(equivS.fv);
						gn.changingRecomCPTs.put(equivS.fv, new ArrayList());
					}


					ArrayList varIndx = new ArrayList(2);
					if(!gn.hasPosEvidence(prevS.fv)) varIndx.add(prevS.fv);
					varIndx.add(equivS.fv);

					TableIndex ti = new TableIndex(varIndx);
					double cpt[] = new double[ti.size()];
					ArrayList changingAL = gn.getChangingVarAL(equivS.fv); //possibly null

					int size1 = (gn.hasPosEvidence(prevS.fv) ? 1 : prevS.size());
					int indx=0;
					for(int vPrev=0; vPrev < size1; vPrev++) {
						String sPrev = (gn.hasPosEvidence(prevS.fv) ? gn.getPosEvidence(prevS.id) : prevS.instance(vPrev));

						for(int vS=0; vS < equivS.size(); vS++) {
							String sS = equivS.instance(vS);

							if(sPrev.equals(sS)) cpt[indx] = 1-recomValue;
							else cpt[indx] = recomValue;

							if(changingAL!=null) {
								NumberedConstant nc = new NumberedConstant(recomValue);
								nc.increment(cpt[indx]);
								changingAL.add(nc);
							}
							indx++;
						}
					}
					if(indx!=cpt.length) throw new IllegalStateException("didn't fill entire array: " + indx + " " + cpt.length);

					Table t = new Table(ti, cpt);
					addEdges(gn, t);
					equivS.fv.setCPTShell(new TableShell(t));

					if(DEBUG_PedigreeVerbose2) {System.out.println("createS: " + sName + " \n" + equivS.fv.getCPTShell().getCPT());}
				}
			}
		}
		else {
			if(include1 && include2) {
				s = new MyFV(new FiniteVariableImpl(sName, states12));
			}
			else if(include1) {
				s = new MyFV(new FiniteVariableImpl(sName, Collections.singletonList("1")));
			}
			else if(include2) {
				s = new MyFV(new FiniteVariableImpl(sName, Collections.singletonList("2")));
			}
			else {
				throw new IllegalStateException("No valid states for: "+sName);
			}

			if(locusChromoOrd==psLoci.getChangingS1() && prevS!=null) {
				if(paternal) gn.changingRecomS1m.add(s.fv);
				else gn.changingRecomS1f.add(s.fv);
				gn.changingRecomCPTs.put(s.fv, new ArrayList());
			}
			else if(locusChromoOrd==psLoci.getChangingS2() && prevS!=null) {
				if(paternal) gn.changingRecomS2m.add(s.fv);
				else gn.changingRecomS2f.add(s.fv);
				gn.changingRecomCPTs.put(s.fv, new ArrayList());
			}

			gn.addVariable(s);

			createS_CPT(gn, s, prevS, recomValue);

			if(DEBUG_PedigreeVerbose2) {
				System.out.println("createS: " + sName + " " + s);
				CPTShell sh = s.fv.getCPTShell();
				if(sh!=null) System.out.println(sh.getCPT());
			}
		}
		return s;
	}


	static private void createS_CPT(GeneticNetwork gn, MyFV s, MyFV prevS, double recom) {
		double recomFromFile = recom;

		ArrayList changingAL = gn.getChangingVarAL(s.fv); //possibly null

		if(prevS==null) {
			if(!gn.hasPosEvidence(s.fv)) {
				s.fv.setCPTShell(new TableShell(new Table(Collections.singletonList(s.fv), new double[]{.5, .5})));
			}
			else {
				//if first one is known, it becomes separated from the graph (leave it in network w/o a cpt until child is created)
				gn.addConstant(.5, null); //don't include variable in this, because it might be a changing variable, however don't care about recombination
				gn.varsToRemove.add(s.fv); //remove it later
			}
		}
		else {//has a parent selector

			if(gn.hasPosEvidence(prevS.fv)) { //parent selector is known
				Object parState = gn.getPosEvidence(prevS.id);

				if(gn.hasPosEvidence(s.fv)) { //this one is known, make constant, leave in network unless last one (child will remove)
					Object chiState = gn.getPosEvidence(s.id);
					gn.addConstant((parState.equals(chiState) ? 1-recom : recom), s.fv);
					gn.varsToRemove.add(s.fv);
				}
				else {
					TableIndex tblIndx = new TableIndex(Collections.singletonList(s.fv));
					double cpt[] = new double[tblIndx.size()];
					for(int i=0; i<cpt.length; i++) {
						if(parState.equals(s.instance(i))) {
							cpt[i] = 1-recom;
						}
						else {
							cpt[i] = recom;
						}
						if(changingAL!=null) {
							NumberedConstant nc = new NumberedConstant(recom);
							nc.increment(cpt[i]);
							changingAL.add(nc);
						}
					}
					s.fv.setCPTShell(new TableShell(new Table(tblIndx, cpt)));
				}
			}
			else { //parent selector is unknown

				if((!gn.isChangingVar(s.fv)) && recom==0.5 && PREPROCESS>=VALID_NET) { //if by chance recom=.5, separate network (not if doing multiple recombination factors)
					if(gn.hasPosEvidence(s.fv)) {
						gn.addConstant(.5, s.fv);
						gn.varsToRemove.add(s.fv);
					}
					else {
						s.fv.setCPTShell(new TableShell(new Table(Collections.singletonList(s.fv), new double[]{.5, .5})));
					}
					return;
				}
				else {

					if(!gn.bn.addEdge(prevS.fv,s.fv, false)) { throw new IllegalArgumentException("!addEdge " + prevS + "->" + s);}

					FiniteVariable varIndx[] = {prevS.fv, s.fv};
					TableIndex tblIndx = new TableIndex(varIndx);
					if(s.size()==2) {
						if(!prevS.instance(0).equals(s.instance(0))) { recom = 1-recom;}
						double cpt[] = {1-recom,recom,recom,1-recom};
						s.fv.setCPTShell(new TableShell(new Table(tblIndx, cpt)));
						if(changingAL!=null) {
							for(int i=0; i<cpt.length; i++) {
								NumberedConstant nc = new NumberedConstant(recomFromFile);
								nc.increment(cpt[i]);
								changingAL.add(nc);
							}
						}
					}
					else if(s.instance(0).equals("1")) {
						if(!prevS.instance(0).equals("1")) { recom = 1-recom;}
						double cpt[] = {1-recom,recom};
						s.fv.setCPTShell(new TableShell(new Table(tblIndx, cpt)));
						if(changingAL!=null) {
							for(int i=0; i<cpt.length; i++) {
								NumberedConstant nc = new NumberedConstant(recomFromFile);
								nc.increment(cpt[i]);
								changingAL.add(nc);
							}
						}
					}
					else {
						if(!prevS.instance(0).equals("1")) { recom = 1-recom;}
						double cpt[] = {recom,1-recom};
						s.fv.setCPTShell(new TableShell(new Table(tblIndx, cpt)));
						if(changingAL!=null) {
							for(int i=0; i<cpt.length; i++) {
								NumberedConstant nc = new NumberedConstant(recomFromFile);
								nc.increment(cpt[i]);
								changingAL.add(nc);
							}
						}
					}
				}
			}//end parent known/unknown
		}//end which locus
	}//end createS_CPT


	//mg2 is only for complex disease
	//never called when PREPROCESS!=NONE
	private void createFounderP(GeneticNetwork gn, MyFV mg1, MyFV mg_first_complex, Person.Phenotype ph,
								int locusInputOrd, String pName) {

		if(PREPROCESS==NONE) throw new IllegalStateException(); //TODO this doesn't create necessary variables
		if(psLoci.sexLinked()) { throw new IllegalStateException("");}//TODO: this doesn't support sexLinked

		if(!ph.fullyKnown()) { return;} //unknown, don't create phenotype variable (if it is partially known, handled by genotype merge)

		//since phenotype is known, don't need to worry about finding "other" in the states, that is an error

		if(ph.type==3) { //numbered
			//for founder, never need to include numbered phenotype, because the variable merge handled it already
			return;
		}
		else if(ph.type==1) { //disease loci, know phenotype

			if(gn.hasPosEvidence(mg1.fv)) { //know pheno & know geno, pheno becomes a constant
				stateParser.parse(gn.getPosEvidence(mg1.id));
				if(stateParser.indx1==-1 || stateParser.indx2==-1) { throw new IllegalStateException("ERROR");}
				double constV = psLoci.getPenetrance(locusInputOrd, pName, ph.two(), stateParser.indicesToIndx());
				if(ph.one()==0) {constV = 1-constV;}//convert to unaffected
				gn.addConstant(constV, null);
			}
			else { //need to create pheno variable & edge from mg1->pheno  (actually, just multiply the probability into the parent)

				Table tblCpt = mg1.fv.getCPTShell().getCPT();
				int parSize = mg1.size();
				for(int i=0; i<parSize; i++) {
					stateParser.parse(mg1.instance(i));
					if(stateParser.indx1==-1 || stateParser.indx2==-1) { throw new IllegalStateException("ERROR");}
					double pVal = psLoci.getPenetrance(locusInputOrd, pName, ph.two(), stateParser.indicesToIndx());
					if(ph.one()==0) {pVal = 1-pVal;}//convert to unaffected
					tblCpt.setCP(i, tblCpt.getCP(i)*pVal);
				}
			}
		}
		else if(ph.type==4) { //complex disease loci, know phenotype

			int sz = (PREPROCESS==ALL?1:2);
			{
				if(!gn.hasPosEvidence(mg1.fv)) sz *= mg1.size();
				if(!gn.hasPosEvidence(mg_first_complex.fv)) sz *= mg_first_complex.size();
			}
			double cpt[] = new double[sz];
			int indx=0;

			int size1 = (gn.hasPosEvidence(mg1.fv) ? 1 : mg1.size());
			int size2 = (gn.hasPosEvidence(mg_first_complex.fv) ? 1 : mg_first_complex.size());

			for(int v1=0; v1<size1; v1++) {
				stateParser.parse( (gn.hasPosEvidence(mg1.fv) ? gn.getPosEvidence(mg1.id) : mg1.instance(v1)));
				int i2 = stateParser.indicesToIndx();

				for(int v2=0; v2<size2; v2++) {
					stateParser.parse( (gn.hasPosEvidence(mg_first_complex.fv) ? gn.getPosEvidence(mg_first_complex.id) : mg_first_complex.instance(v2)));
					int i1 = stateParser.indicesToIndx();

					cpt[indx] = psLoci.getComplexPenetrance(locusInputOrd, pName, ph.two(), i1, i2);
					if(ph.one()==0) cpt[indx] = 1-cpt[indx]; //convert to unaffected
					indx++;

					if(PREPROCESS!=ALL) {
						cpt[indx] = 1-(cpt[indx-1]);
						indx++;
					}
				}
			}
			if(indx!=cpt.length) {throw new IllegalStateException("didn't fill entire array: " + indx + " " + cpt.length);}

			//test for all same
			boolean allSame = true;
			for(int i=1; i<cpt.length; i++) {
				if(cpt[0] != cpt[i]) {allSame = false; break;}
			}

			if(allSame && PREPROCESS>=VALID_NET) {
				gn.addConstant(cpt[0], null);
			}
			else {
				FiniteVariable phFV;
				if(PREPROCESS==ALL) {
					phFV = new FiniteVariableImpl(pName, Collections.singletonList(String.valueOf(ph.one())));
				}
				else {
					int o = ph.one(); //{0.1}
					int t = (o==0 ? 1 : 0);
					String val[] = {String.valueOf(o), String.valueOf(t)};
					phFV = new FiniteVariableImpl(pName, val);
					gn.addEvidencePositive(phFV.getID(), val[0]);
				}

				ArrayList varIndx = new ArrayList(3);
				if(!gn.hasPosEvidence(mg1.fv)) varIndx.add(mg1.fv);
				if(!gn.hasPosEvidence(mg_first_complex.fv)) varIndx.add(mg_first_complex.fv);
				varIndx.add(phFV);

				gn.addVariable(new MyFV(phFV));
				Table tbl = new Table(varIndx, cpt);
				addEdges(gn, tbl);
				phFV.setCPTShell(new TableShell(tbl));
			}
		}
		else {
			throw new IllegalStateException();
		}
	}

	private void createNonFounderP(GeneticNetwork gn, Person.Phenotype ph, MyFV gp1, MyFV gm1,
					MyFV gp_first, MyFV gm_first, int locusInputOrd, boolean male, String pName) {

		if(!ph.fullyKnown() && PREPROCESS>NONE) return; //it is a leaf node with no evidence

		if(ph.type==4) { //has 4 parents
			if(psLoci.isComplex1(locusInputOrd)) {return;}


			if(psLoci.sexLinked() && male) {
				int sz = (PREPROCESS==ALL?1:2);
				{
					if(!gn.hasPosEvidence(gm1.fv)) sz *= gm1.size();
					if(!gn.hasPosEvidence(gm_first.fv)) sz *= gm_first.size();
				}
				double cpt[] = new double[sz];
				int indx=0;

				int size1 = (gn.hasPosEvidence(gm1.fv) ? 1 : gm1.size());
				int size2 = (gn.hasPosEvidence(gm_first.fv) ? 1 : gm_first.size());

				for(int v_m1=0; v_m1<size1; v_m1++) {
					int gmv1 = Integer.parseInt((String)(gn.hasPosEvidence(gm1.fv) ? gn.getPosEvidence(gm1.id) : gm1.instance(v_m1)));

					for(int v_m2=0; v_m2<size2; v_m2++) {
						int gmv2 = Integer.parseInt((String)(gn.hasPosEvidence(gm_first.fv) ? gn.getPosEvidence(gm_first.id) : gm_first.instance(v_m2)));

						cpt[indx] = psLoci.getComplexPenetrance(locusInputOrd, pName, ph.two(), gmv2, gmv1);
						if(PREPROCESS==ALL && ph.one()==0) cpt[indx] = 1-cpt[indx]; //convert to unaffected
						indx++;

						if(PREPROCESS!=ALL) {
							cpt[indx] = 1-(cpt[indx-1]);
							indx++;
						}
					}
				}
				if(indx!=cpt.length) throw new IllegalStateException("didn't fill entire array: " + indx + " " + cpt.length);

				//test for all same
				boolean allSame = true;
				for(int i=1; i<cpt.length; i++) {
					if(cpt[0] != cpt[i]) {allSame = false; break;}
				}

				if(allSame && PREPROCESS>=VALID_NET) {
					gn.addConstant(cpt[0], null);
				}
				else {
					FiniteVariable p;
					if(PREPROCESS==ALL) {
						p = new FiniteVariableImpl(pName, Collections.singletonList(String.valueOf(ph.one())));
					}
					else {
						int o = ph.one(); //{0,1}
						int t = (o==0 ? 1 : 0);
						String val[] = {String.valueOf(0), String.valueOf(1)};
						p = new FiniteVariableImpl(pName, val);
						if(ph.fullyKnown()) gn.addEvidencePositive(p.getID(), String.valueOf(ph.one()));
					}

					ArrayList varIndx = new ArrayList(3);
					if(!gn.hasPosEvidence(gm1.fv)) varIndx.add(gm1.fv);
					if(!gn.hasPosEvidence(gm_first.fv)) varIndx.add(gm_first.fv);
					varIndx.add(p);

					gn.addVariable(new MyFV(p));
					Table tbl = new Table(varIndx, cpt);
					addEdges(gn, tbl);
					p.setCPTShell(new TableShell(tbl));
				}
			}
			else { //either not sexlinked, or else female
				int sz = (PREPROCESS==ALL?1:2);
				{
					if(!gn.hasPosEvidence(gp1.fv)) sz *= gp1.size();
					if(!gn.hasPosEvidence(gm1.fv)) sz *= gm1.size();
					if(!gn.hasPosEvidence(gp_first.fv)) sz *= gp_first.size();
					if(!gn.hasPosEvidence(gm_first.fv)) sz *= gm_first.size();
				}
				double cpt[] = new double[sz];
				int indx=0;

				int size1 = (gn.hasPosEvidence(gp1.fv) ? 1 : gp1.size());
				int size2 = (gn.hasPosEvidence(gm1.fv) ? 1 : gm1.size());
				int size3 = (gn.hasPosEvidence(gp_first.fv) ? 1 : gp_first.size());
				int size4 = (gn.hasPosEvidence(gm_first.fv) ? 1 : gm_first.size());

				for(int vp1=0; vp1<size1; vp1++) {
					int gpv1 = Integer.parseInt((String)(gn.hasPosEvidence(gp1.fv) ? gn.getPosEvidence(gp1.id) : gp1.instance(vp1)));

					for(int vm1=0; vm1<size2; vm1++) {
						int gmv1 = Integer.parseInt((String)(gn.hasPosEvidence(gm1.fv) ? gn.getPosEvidence(gm1.id) : gm1.instance(vm1)));

						for(int vp2=0; vp2<size3; vp2++) {
							int gpv2 = Integer.parseInt((String)(gn.hasPosEvidence(gp_first.fv) ? gn.getPosEvidence(gp_first.id) : gp_first.instance(vp2)));

							for(int vm2=0; vm2<size4; vm2++) {
								int gmv2 = Integer.parseInt((String)(gn.hasPosEvidence(gm_first.fv) ? gn.getPosEvidence(gm_first.id) : gm_first.instance(vm2)));

								cpt[indx] = psLoci.getComplexPenetrance(locusInputOrd, pName, ph.two(), stateParser.indicesToIndx(gpv2, gmv2), stateParser.indicesToIndx(gpv1, gmv1));
								if(PREPROCESS==ALL && ph.one()==0) cpt[indx] = 1-cpt[indx]; //conver to unaffected
								indx++;

								if(PREPROCESS!=ALL) {
									cpt[indx]=1-(cpt[indx-1]);
									indx++;
								}
							}
						}
					}
				}
				if(indx!=cpt.length) throw new IllegalStateException("didn't fill entire array: " + indx + " " + cpt.length);

				//test for all same
				boolean allSame = true;
				for(int i=1; i<cpt.length; i++) {
					if(cpt[0] != cpt[i]) {allSame = false; break;}
				}

				if(allSame && PREPROCESS>=VALID_NET) {
					gn.addConstant(cpt[0], null);
				}
				else {
					FiniteVariable p;
					if(PREPROCESS==ALL) {
						p = new FiniteVariableImpl(pName, Collections.singletonList(String.valueOf(ph.one())));
					}
					else {
						int o = ph.one(); //{0,1}
						int t = (o==0 ? 1 : 0);
						String val[] = {String.valueOf(0), String.valueOf(1)};
						p = new FiniteVariableImpl(pName, val);
						if(ph.fullyKnown()) gn.addEvidencePositive(p.getID(), String.valueOf(ph.one()));
					}

					ArrayList varIndx = new ArrayList(5);
					if(!gn.hasPosEvidence(gp1.fv)) varIndx.add(gp1.fv);
					if(!gn.hasPosEvidence(gm1.fv)) varIndx.add(gm1.fv);
					if(!gn.hasPosEvidence(gp_first.fv)) varIndx.add(gp_first.fv);
					if(!gn.hasPosEvidence(gm_first.fv)) varIndx.add(gm_first.fv);
					varIndx.add(p);

					gn.addVariable(new MyFV(p));
					Table tbl = new Table(varIndx, cpt);
					addEdges(gn, tbl);
					p.setCPTShell(new TableShell(tbl));
				}
			}
		}
		else { //has 2 parents

			if(ph.type==1) { //disease locus

				if(psLoci.sexLinked() && male) {
					int sz = (PREPROCESS==ALL?1:2);
					{
						if(!gn.hasPosEvidence(gm1.fv)) sz *= gm1.size();
					}
					double cpt[] = new double[sz];
					int indx=0;

					int size1 = ((gn.hasPosEvidence(gm1.fv) ? 1 : gm1.size()));

					for(int v_m=0; v_m < size1; v_m++) {
						int gmv = Integer.parseInt((String)(gn.hasPosEvidence(gm1.fv) ? gn.getPosEvidence(gm1.id) : gm1.instance(v_m)));

						cpt[indx] = psLoci.getPenetrance(locusInputOrd, pName, ph.two(), 3 + gmv);
						if(PREPROCESS==ALL && ph.one()==0) {cpt[indx] = 1-cpt[indx];}//convert to unaffected
						indx++;

						if(PREPROCESS!=ALL) {
							cpt[indx]=1-(cpt[indx-1]);
							indx++;
						}
					}
					if(indx!=cpt.length) {throw new IllegalStateException("didn't fill entire array: " + indx + " " + cpt.length);}

					//test for all same
					boolean allSame = true;
					for(int i=1; i<cpt.length; i++) {
						if(cpt[0] != cpt[i]) {allSame = false; break;}
					}

					if(allSame && PREPROCESS>=VALID_NET) {
						gn.addConstant(cpt[0], null);
					}
					else {
						FiniteVariable p;
						if(PREPROCESS==ALL) {
							p = new FiniteVariableImpl(pName, Collections.singletonList(String.valueOf(ph.one())));
						}
						else {
							int o = ph.one(); //{0,1}
							int t = (o==0 ? 1 : 0);
							String val[] = {String.valueOf(0), String.valueOf(1)};
							p = new FiniteVariableImpl(pName, val);
							if(ph.fullyKnown()) gn.addEvidencePositive(p.getID(), String.valueOf(ph.one()));
						}

						ArrayList varIndx = new ArrayList(2);
						if(!gn.hasPosEvidence(gm1.fv)) varIndx.add(gm1.fv);
						varIndx.add(p);

						gn.addVariable(new MyFV(p));
						Table tbl = new Table(varIndx, cpt);
						addEdges(gn, tbl);
						p.setCPTShell(new TableShell(tbl));
					}
				}
				else { //not sexlinked or else female
					int sz = (PREPROCESS==ALL?1:2);
					{
						if(!gn.hasPosEvidence(gp1.fv)) sz *= gp1.size();
						if(!gn.hasPosEvidence(gm1.fv)) sz *= gm1.size();
					}
					double cpt[] = new double[sz];
					int indx=0;

					int size1 = (gn.hasPosEvidence(gp1.fv) ? 1 : gp1.size());
					int size2 = (gn.hasPosEvidence(gm1.fv) ? 1 : gm1.size());

					for(int v_p=0; v_p < size1; v_p++) {
						int gpv = Integer.parseInt((String)(gn.hasPosEvidence(gp1.fv) ? gn.getPosEvidence(gp1.id) : gp1.instance(v_p)));

						for(int v_m=0; v_m < size2; v_m++) {
							int gmv = Integer.parseInt((String)(gn.hasPosEvidence(gm1.fv) ? gn.getPosEvidence(gm1.id) : gm1.instance(v_m)));

							cpt[indx] = psLoci.getPenetrance(locusInputOrd, pName, ph.two(), StateParser.indicesToIndx(gpv, gmv));
							if(PREPROCESS==ALL && ph.one()==0) {cpt[indx] = 1-cpt[indx];}//convert to unaffected
							indx++;

							if(PREPROCESS!=ALL) {
								cpt[indx]=1-(cpt[indx-1]);
								indx++;
							}
						}
					}
					if(indx!=cpt.length) {throw new IllegalStateException("didn't fill entire array: " + indx + " " + cpt.length);}

					//test for all same
					boolean allSame = true;
					for(int i=1; i<cpt.length; i++) {
						if(cpt[0] != cpt[i]) {allSame = false; break;}
					}

					if(allSame && PREPROCESS>=VALID_NET) {
						gn.addConstant(cpt[0], null);
					}
					else {
						FiniteVariable p;
						if(PREPROCESS==ALL) {
							p = new FiniteVariableImpl(pName, Collections.singletonList(String.valueOf(ph.one())));
						}
						else {
							int o = ph.one(); //{0,1}
							int t = (o==0 ? 1 : 0);
							String val[] = {String.valueOf(0), String.valueOf(1)};
							p = new FiniteVariableImpl(pName, val);
							if(ph.fullyKnown()) gn.addEvidencePositive(p.getID(), String.valueOf(ph.one()));
						}

						ArrayList varIndx = new ArrayList(3);
						if(!gn.hasPosEvidence(gp1.fv)) varIndx.add(gp1.fv);
						if(!gn.hasPosEvidence(gm1.fv)) varIndx.add(gm1.fv);
						varIndx.add(p);

						gn.addVariable(new MyFV(p));
						Table tbl = new Table(varIndx, cpt);
						addEdges(gn, tbl);
						p.setCPTShell(new TableShell(tbl));
					}
				}
			}
			else if(ph.type==3) {
				if(gn.hasPosEvidence(gp1.fv) && gn.hasPosEvidence(gm1.fv) && PREPROCESS>NONE) return; //p is only deterministic & is independent

				if (PREPROCESS == NONE)
				{
					if(gp1.size()!=gm1.size()) throw new IllegalStateException();
					int sz = gp1.size();
					sz = sz * (sz+1) / 2; //number of states of p
					sz *= gp1.size();
					sz *= gm1.size();

					double cpt[] = new double[sz];
					String val[] = new String[gp1.size() * (gp1.size()+1) / 2];

					int indx = 0;
					for (int i = 0; i < gp1.size(); i++)
					{
						for (int j = i; j < gm1.size(); j++)
						{
							val[indx] = gp1.instance(i) + "_" + gm1.instance(j);
							indx++;
						}
					}
					if (indx != val.length) throw new IllegalStateException();
					indx = 0;

					for (int vp = 0; vp < gp1.size(); vp++)
					{
						String gpv = gp1.instance(vp);

						for (int vm = 0; vm < gm1.size(); vm++)
						{
							String gmv = gm1.instance(vm);

							for (int i = 0; i < val.length; i++)
							{
								String pv = val[i];
								if (pv.equals(gpv+"_"+gmv) || pv.equals(gmv+"_"+gpv)) { cpt[indx] = 1.0; }
								else { cpt[indx] = 0.0; }
								indx++;
							}
						}
					}
					if (indx != cpt.length) throw new IllegalStateException();
					FiniteVariable p;
					p = new FiniteVariableImpl(pName, val);

					ArrayList varIndx = new ArrayList(3);
					varIndx.add(gp1.fv);
					varIndx.add(gm1.fv);
					varIndx.add(p);

					gn.addVariable(new MyFV(p));
					Table tbl = new Table(varIndx, cpt);
					addEdges(gn, tbl);
					p.setCPTShell(new TableShell(tbl));

					if (ph.fullyKnown()) {
						String ev;
						if(ph.one() < ph.two()) {ev = ph.one() + "_" + ph.two();}
						else {ev = ph.two() + "_" + ph.one();}
						gn.addEvidencePositive(p.getID(), ev);
					}
				}
				else
				{
					int sz = (PREPROCESS == ALL ? 1 : 2);
					{
						if (!gn.hasPosEvidence(gp1.fv)) sz *= gp1.size();
						if (!gn.hasPosEvidence(gm1.fv)) sz *= gm1.size();
					}

					double cpt[] = new double[sz];
					int indx = 0;
					boolean onlyOnes = true;

					int o = ph.one();
					int t = ph.two();

					int size1 = (gn.hasPosEvidence(gp1.fv) ? 1 : gp1.size());
					int size2 = (gn.hasPosEvidence(gm1.fv) ? 1 : gm1.size());

					for (int v_p = 0; v_p < size1; v_p++)
					{
						int gpv = Integer.parseInt((String)(gn.hasPosEvidence(gp1.fv) ? gn.getPosEvidence(gp1.id) : gp1.instance(v_p)));

						for (int v_m = 0; v_m < size2; v_m++)
						{
							int gmv = Integer.parseInt((String)(gn.hasPosEvidence(gm1.fv) ? gn.getPosEvidence(gm1.id) : gm1.instance(v_m)));

							if (t >= 0 && ((gpv == o && gmv == t) || (gpv == t && gmv == o)))
							{ //fully known phenotype
								cpt[indx++] = 1.0;
							}
							else if (t < 0 && (gpv == o || gmv == o))
							{ //partially known phenotype
								cpt[indx++] = 1.0;
							}
							else
							{
								cpt[indx++] = 0.0;
								onlyOnes = false;
							}

							if (PREPROCESS != ALL)
							{
								cpt[indx] = 1.0 - cpt[indx - 1];
								indx++;
								onlyOnes = false;
							}
						}
					}
					if (indx != cpt.length) { throw new IllegalStateException("didn't fill entire array: " + indx + " " + cpt.length); }

					if ((!onlyOnes) || (PREPROCESS < LEAF_ADDITIONAL_EVID))
					{ //don't add variable if it would be a leaf, has only 1 state, & prob are all 1.0

						FiniteVariable p;
						if (PREPROCESS == ALL)
						{
							p = new FiniteVariableImpl(pName, Collections.singletonList(ph.toString()));
						}
						else
						{
							String val[] = { ph.toString(), "InValid" };
							p = new FiniteVariableImpl(pName, val);
							gn.addEvidencePositive(p.getID(), val[0]);
						}

						ArrayList varIndx = new ArrayList(3);
						if (!gn.hasPosEvidence(gp1.fv)) varIndx.add(gp1.fv);
						if (!gn.hasPosEvidence(gm1.fv)) varIndx.add(gm1.fv);
						varIndx.add(p);

						gn.addVariable(new MyFV(p));
						Table tbl = new Table(varIndx, cpt);
						addEdges(gn, tbl);
						p.setCPTShell(new TableShell(tbl));
					}
				}
			}
			else {
				throw new IllegalStateException();
			}
		}//end has 2 parents
	}//end createNonFounderP

	//add parents s and ((parP & parM) or parMer) (keeping in mind there could be evidence)
	//  then enforce the constraints of inheritance
	//gpOtherOther and gmOtherOther are true if parent "other" is a valid parent of the child's "other" state
	//Returns true if "g" was changed at all, otherwise returns false.
	static private boolean addParents(MyFV g, MyFV s, MyFV parP, MyFV parM, MyFV parMer,
							boolean gpOtherOther, boolean gmOtherOther, Collection chiOther,
							GeneticNetwork gn) {

		if(s==null) throw new IllegalArgumentException();

		if(parMer!=null && parMer.fv==s.fv) {
			//This would occur when have heterozygous parent and known child
			//  but once S is merged with parMer, the child becomes irrelevant and does not need to be created
			if(!gn.hasPosEvidence(g.fv)) throw new IllegalStateException("No Evidence on " + g + " " + s + " " + parP + " " + parM + " " + parMer);
			return false;
		}

		Table cpt = g.fv.getCPTShell().getCPT();
		ArrayList tmpChangingAL = null;//in case need to revert back
		{
			tmpChangingAL = gn.getChangingVarAL(g.fv);
			if(tmpChangingAL!=null) tmpChangingAL = (ArrayList)tmpChangingAL.clone();
		}
		boolean madeChanges = false; //only true if added constraints to the CPT

		if(DEBUG_PedigreeVerbose2) {System.out.println("Adding Parents to " + g + " " + s + " " + parP + " " + parM + " " + parMer + "\nOrigCPT:\n" + cpt);}

		if(parM!=null && parP!=null && parM!=parP) { //if parM==parP then parMer should exist

			{//quick test to make sure evidence makes sense (not complete) AND possibly RETURN
				boolean knowG = gn.hasPosEvidence(g.fv);
				boolean knowParP = gn.hasPosEvidence(parP.fv);
				boolean knowParM = gn.hasPosEvidence(parM.fv);
				boolean knowS = gn.hasPosEvidence(s.fv);

				if(knowS && knowParP && knowParM) { //know all parents
					if(!knowG) throw new IllegalStateException(); //S & Gp/Gm -> G
					else return false;
				}
				if(knowS && knowG && !(knowParP || knowParM)) throw new IllegalStateException(); //S & G -> (either Gp or Gm)
				if(knowParP && knowParM && knowG) {//both parents are known along with child
					String evidP = gn.getPosEvidence(parP.id);
					String evidM = gn.getPosEvidence(parM.id);
					String evidG = gn.getPosEvidence(g.id);
					//if child can inherit from either, don't need to include any parents (Gp, Gm, or S) and also don't need to place any restrictions in here.
					//an example of this is if the parent is homozygous
					if(evidG.equals(other)) {
						boolean fromFather = false;
						boolean fromMother = false;
						if((evidP.equals(other) && gpOtherOther) ||
							(chiOther.contains(evidP))) { //then child can inherit from father
							fromFather=true;
						}
						if((evidM.equals(other) && gmOtherOther) ||
							(chiOther.contains(evidM))) { //then child can inherit from mother
							fromMother=true;
						}
						if(fromFather && fromMother) { //don't need parents
							if(knowS) throw new IllegalStateException();
							else return false;
							//NOTE there is a return here!!!  If parent is homozygous then don't need to add these parent variables
						}
						else { //should know S
							if(!knowS) throw new IllegalStateException();
						}
					}
					else {
						if(evidG.equals(evidP) && evidG.equals(evidM)) { //homozygous parent
							if(knowS) throw new IllegalStateException();
							else return false;
							//NOTE there is a return here!!!  If parent is homozygous then don't need to add these parent variables
						}
						else { //should know S
							if(!knowS) throw new IllegalStateException();
						}
					}
				}
			}

			{
				String evidGP = gn.getPosEvidence(parP.id);
				String evidGM = gn.getPosEvidence(parM.id);
				String evidS = gn.getPosEvidence(s.id);

				if(evidS!=null) {
					if(evidS.equals("1")) { //don't need GM
						if(evidGM==null) evidGM = "place holder"; //don't include this as parent
					}
					else if(evidS.equals("2")) { //don't need GP
						if(evidGP==null) evidGP = "place holder"; //don't include this as parent
					}
					else throw new IllegalStateException();
				}

				ArrayList changingAL = gn.getChangingVarAL(g.fv); //possibly null

				//for each parent, if not already included and no evidence, then include it
				if(cpt.index().variableIndex(parP.fv)<0 && evidGP==null) {
					cpt = cpt.expand(parP.fv);
					updateChangingAL_expand(cpt, changingAL);
				}
				if(cpt.index().variableIndex(parM.fv)<0 && evidGM==null) {
					cpt = cpt.expand(parM.fv);
					updateChangingAL_expand(cpt, changingAL);
				}
				if(cpt.index().variableIndex(s.fv)<0 && evidS==null) {
					cpt = cpt.expand(s.fv);
					updateChangingAL_expand(cpt, changingAL);
				}

				if(DEBUG_PedigreeVerbose2) {System.out.println("addParents (midway): " + cpt);}

				TableIndex ti = cpt.index();

				int indxGP = ti.variableIndex(parP.fv);
				int indxGM = ti.variableIndex(parM.fv);
				int indxS = ti.variableIndex(s.fv);
				int indxG = ti.variableIndex(g.fv);

				//"^" means XOR (either want it added to table, or want evidence, but not both and not neither)
				if(!((evidGP!=null) ^ (indxGP!=-1))) throw new IllegalStateException("Included, but has evidence " +parP);
				if(!((evidGM!=null) ^ (indxGM!=-1))) throw new IllegalStateException("Included, but has evidence " +parM);
				if(!((evidS!=null) ^ (indxS!=-1))) throw new IllegalStateException("Included, but has evidence " +s);

				for(TableIndex.Iterator itrT = ti.iterator(); itrT.hasNext();) {
					int ci = itrT.next();

					String sst = (indxS!=-1 ? s.instance(itrT.current()[indxS]) : evidS);
					String gst = g.instance(itrT.current()[indxG]);

					if(sst.equals("1")) { //G=parGP (possibly "other" state)
						//set evidGP (either from evid or from iterator)
						if(indxGP!=-1) evidGP = parP.instance(itrT.current()[indxGP]);

						if(gst.equals(other)) { //child is in state "other"
							if(evidGP.equals(other)) { //other->other if parent version is subset of child
								if(!gpOtherOther) {cpt.setCP(ci, 0.0); madeChanges=true;}
							}
							else {//"i"->other if other includes "i"
								if(!chiOther.contains(evidGP)) {cpt.setCP(ci, 0.0); madeChanges=true;}
							}
						}
						else {
							if(!gst.equals(evidGP)) {cpt.setCP(ci, 0.0); madeChanges=true;}
						}
					}
					else if(sst.equals("2")) { //G=parGM (possibly "other" state)
						//set evidGM (either from evid or from iterator)
						if(indxGM!=-1) evidGM = parM.instance(itrT.current()[indxGM]);

						if(gst.equals(other)) { //child is in state "other"
							if(evidGM.equals(other)) { //other->other if parent version is subset of child
								if(!gmOtherOther) {cpt.setCP(ci, 0.0); madeChanges=true;}
							}
							else {//"i"->other if other includes "i"
								if(!chiOther.contains(evidGM)) {cpt.setCP(ci, 0.0); madeChanges=true;}
							}
						}
						else {
							if(!gst.equals(evidGM)) {cpt.setCP(ci, 0.0); madeChanges=true;}
						}
					}
					else throw new IllegalStateException();
				}
			}
		}
		else if(parMer!=null) {

			{//quick test to make sure evidence makes sense (not complete)
				boolean knowG = gn.hasPosEvidence(g.fv);
				boolean knowParMer = gn.hasPosEvidence(parMer.fv);
				boolean knowS = gn.hasPosEvidence(s.fv);

				if(knowS && knowParMer && !knowG) throw new IllegalStateException(); //S & Gp/Gm -> G
				if(knowS && knowG && !(knowParMer)) throw new IllegalStateException(); //S & G -> (either Gp or Gm)
				if(knowParMer && knowG) { //if par is homozygous S is unknown, if it is heterozygous it should be known
					stateParser.parse(gn.getPosEvidence(parMer.id));
					if(stateParser.s1.equals(stateParser.s2)) { //homozygous
						if(knowS) throw new IllegalStateException();
						else return false;
						//NOTE there is a return here!!!  If parent is homozygous then don't need to add these parent variables
					}
					else { //hererozygous
						if(!knowS) throw new IllegalStateException();
					}
				}
			}

			{
				String evidMer = gn.getPosEvidence(parMer.id);
				String evidS = gn.getPosEvidence(s.id);
				String evidGP = null;
				String evidGM = null;
				if(evidMer!=null) {
					stateParser.parse(evidMer);
					evidGP = stateParser.s1;
					evidGM = stateParser.s2;
				}
				if(evidS!=null) {
					if(evidS.equals("1")) { //don't need GM
						if(evidGM==null) evidGM = "place holder"; //don't include this as parent
					}
					else if(evidS.equals("2")) { //don't need GP
						if(evidGP==null) evidGP = "place holder"; //don't include this as parent
					}
					else throw new IllegalStateException();
				}

				//for each parent, if not already included and no evidence then include it
				ArrayList changingAL = gn.getChangingVarAL(g.fv); //possibly null

				if(cpt.index().variableIndex(parMer.fv)<0 && evidMer==null) {
					cpt = cpt.expand(parMer.fv);
					updateChangingAL_expand(cpt, changingAL);
				}
				if(cpt.index().variableIndex(s.fv)<0 && evidS==null) {
					cpt = cpt.expand(s.fv);
					updateChangingAL_expand(cpt, changingAL);
				}

				if(DEBUG_PedigreeVerbose2) {System.out.println("addParents (midway): " + cpt);}

				TableIndex ti = cpt.index();

				int indxMer = ti.variableIndex(parMer.fv);
				int indxS = ti.variableIndex(s.fv);
				int indxG = ti.variableIndex(g.fv);

				//"^" means XOR (either want it added to table, or want evidence, but not both and not neither)
				if(!((evidMer!=null) ^ (indxMer!=-1))) throw new IllegalStateException("Included, but has evidence " +parMer+ " " + evidMer + " " + indxMer + " during creation of " + g);
				if(!((evidS!=null) ^ (indxS!=-1))) throw new IllegalStateException("Included, but has evidence " +s+ " " + evidS + " " + indxS + " during creation of " + g);

				for(TableIndex.Iterator itrT = ti.iterator(); itrT.hasNext();) {
					int ci = itrT.next();

					String sst = (indxS!=-1 ? s.instance(itrT.current()[indxS]) : evidS);
					String gst = g.instance(itrT.current()[indxG]);
					//set evidGP/evidGM (either from evid or from iterator)
					if(indxMer!=-1) {
						stateParser.parse(parMer.instance(itrT.current()[indxMer]));
						evidGP = stateParser.s1;
						evidGM = stateParser.s2;
					}

					if(sst.equals("1")) { //G=parGP (possibly "other" state)
						if(gst.equals(other)) { //child is in state "other"
							if(evidGP.equals(other)) { //other->other if parent version is subset of child
								if(!gpOtherOther) {cpt.setCP(ci, 0.0); madeChanges=true;}
							}
							else {//"i"->other if other includes "i"
								if(!chiOther.contains(evidGP)) {cpt.setCP(ci, 0.0); madeChanges=true;}
							}
						}
						else {
							if(!gst.equals(evidGP)) {cpt.setCP(ci, 0.0); madeChanges=true;}
						}
					}
					else if(sst.equals("2")) { //G=parGM (possibly "other" state)
						if(gst.equals(other)) { //child is in state "other"
							if(evidGM.equals(other)) { //other->other if parent version is subset of child
								if(!gmOtherOther) {cpt.setCP(ci, 0.0); madeChanges=true;}
							}
							else {//"i"->other if other includes "i"
								if(!chiOther.contains(evidGM)) {cpt.setCP(ci, 0.0); madeChanges=true;}
							}
						}
						else {
							if(!gst.equals(evidGM)) {cpt.setCP(ci, 0.0); madeChanges=true;}
						}
					}
					else throw new IllegalStateException();
				}
			}
		}
		else throw new IllegalStateException("Illegal configuration of variables: " + g + " " + parP + " " + parM + " " + parMer);

		if(madeChanges) {
			if(DEBUG_PedigreeVerbose2) {System.out.println("Adding Parents to " + g + "\nfinalCPT:\n" + cpt);}
			g.fv.setCPTShell(new TableShell(cpt));
			addEdges(gn, cpt);
			return true;
		}
		else {
			ArrayList al = gn.getChangingVarAL(g.fv);
			if(al!=null) {
				al.clear();
				al.addAll(tmpChangingAL); //revert back to original
			}
			return false;
		}
	}


	static void addEdges(GeneticNetwork gn, Table cpt) {
		TableIndex tblIndx = cpt.index();
		FiniteVariable child = tblIndx.getJoint();
		for(int i=0; i<tblIndx.getNumVariables(); i++) {
			FiniteVariable par = tblIndx.variable(i);
			if(par != child && !gn.bn.containsEdge(par, child)) { if(!gn.bn.addEdge(par, child, false)) {throw new IllegalArgumentException("!addEdge " + par + "->" + child);}}
		}
	}


	final static private StateParser stateParser = new StateParser();
	private static class StateParser {
		String s1;
		String s2;
		int indx1;
		int indx2;

		void parse(Object ino) {
			String in = ino.toString();
			int split = in.indexOf('_');
			s1 = in.substring(0, split);
			s2 = in.substring(split+1);

			if(s1.equals(other)) {indx1=-1;}
			else {indx1=Integer.parseInt(s1);}

			if(s2.equals(other)) {indx2=-1;}
			else {indx2=Integer.parseInt(s2);}
		}

		int indicesToIndx() {
			if(indx1==0 && indx2==0) { return 0;}
			else if((indx1==1 && indx2==0) || (indx1==0 && indx2==1)) { return 1;}
			else if(indx1==1 && indx2==1) { return 2;}
			else throw new IllegalStateException("State: " + s1 + "_" + s2 + " : " + indx1 + "_" + indx2);
		}

		static int indicesToIndx(int o, int t) {
			if(o==0 && t==0) { return 0;}
			else if((o==0 && t==1) || (o==1 && t==0)) { return 1;}
			else if(o==1 && t==1) { return 2;}
			else throw new IllegalStateException("State: " + o + "_" + t);
		}
	}


	public void printFamily(int childID, int locus) {
		Person per = (Person)pedigreeData.get(childID);
		if(per==null) { System.out.println("Could not find person " + childID); return;}

		NuclearFamily nf = per.childInFam;
		if(nf!=null) {
			nf.printLocus(locus);
		}
		else {
			System.out.println("Person " + childID + " is a founder.");
		}
	}

	public void printPerson(int perID, int locus) {
		Person per = (Person)pedigreeData.get(perID);
		if(per==null) { System.out.println("Could not find person " + perID); return;}

		{
			NuclearFamily nf = per.childInFam;
			if(nf!=null) {
				nf.printLocus(locus);
			}
			else {
				System.out.println("Person " + perID + " is a founder.");
			}
		}

		for(Iterator itr = per.parInFam.iterator(); itr.hasNext();) {
			NuclearFamily nf = (NuclearFamily)itr.next();
			nf.printLocus(locus);
		}
	}

	//it is unlikely, but it is possible to get a merged parent with 2 states (from a horizontal merge)
	//  which does not have 1_2==2_1, and therefore "founder removal" simplification won't work
	static private boolean satisfiesFounderRemoval(MyFV myfv, GeneticNetwork gn) {
		if(myfv==null) throw new IllegalArgumentException("Error in satisfiesFounderRemoval(null)");
		CPTShell cptsh = myfv.fv.getCPTShell();
		if(cptsh==null) { //could be null if have evidence (should only have that if homozygous)
			String evid = gn.getPosEvidence(myfv.id);
			if(evid==null) throw new IllegalStateException("Null CPT without any evidence: " + myfv);
			stateParser.parse(evid);
			if(!stateParser.s1.equals(stateParser.s2)) throw new IllegalStateException("Evidence on founder which wasn't homozygous: " + evid + " " + myfv);
			return true;
		}

		for(int i=0; i<myfv.size(); i++) {
			stateParser.parse(myfv.instance(i));
			int j = myfv.index(stateParser.s2+"_"+stateParser.s1);
			if(Math.abs(cptsh.getCP(i)-cptsh.getCP(j)) > epsilon) return false;
		}
		return true;
	}

	//ONLY call this on a merged variable, unknown results otherwise
	static private boolean isHeterozygous(MyFV myfv, String evid) {
		if(myfv==null) return false;

		if(evid!=null) {
			stateParser.parse(evid);
			if(stateParser.s1.equals(stateParser.s2)) return false;
			else return true;
		}
		else {
			for(int i=0; i<myfv.size(); i++) {
				stateParser.parse(myfv.instance(i));
				if(stateParser.s1.equals(stateParser.s2)) return false;
			}
			return true;
		}
	}

	static private boolean isHeterozygous(MyFV parP, MyFV parM, String evidP, String evidM) {

		//if any states in P are equal to any states in M, then return false

		if(evidP!=null && evidM!=null) {
			if(evidP.equals(evidM)) return false;
			else return true;
		}
		else if(evidP!=null) {
			if(parM.index(evidP)>=0) return false;
			else return true;
		}
		else if(evidM!=null) {
			if(parP.index(evidM)>=0) return false;
			else return true;
		}
		else {
			for(int i=0; i<parP.size(); i++) {
				String p = parP.instance(i);
				if(parM.index(p)>=0) return false;
			}
			return true;
		}
	}


	static private boolean tryLeafRemoval(FiniteVariable fvL, GeneticNetwork gn) {
		if(PREPROCESS<LEAF_PRUNE) { return false;}
		if(PREPROCESS<ALL && gn.hasPosEvidence(fvL)) { return false;} //don't ask this if using all, because could still sum to one

		boolean ret = false;

		LinkedList varsToTry = new LinkedList();
		varsToTry.add(fvL);

		while(!varsToTry.isEmpty()) {
			FiniteVariable fv = (FiniteVariable)varsToTry.removeLast();
			if(PREPROCESS<ALL && gn.hasPosEvidence(fv)) continue; //if has evidence, cannot leaf prune (unless all those add to 1.0)
			int fvSize = fv.size();
			Table cpt = fv.getCPTShell().getCPT();

			//for each parent instantiation, if the child sums to 1.0, then they can be removed (assuming no evidence is also in place)

			boolean canRemove = true;
			{
				int cnt=1;
				double sum=0;
				for(int i=0; i<cpt.getCPLength(); i++) {
					sum+=cpt.getCP(i);
					if(cnt==fvSize) { //since child is last, each set of fvSize values represents a parent instantiation
						if(Math.abs(sum-1.0)>epsilon) { //did not sum to 1.0, therefore can't remove (probably had evidence included)
							canRemove = false;
							break;
						}
						cnt=1; sum=0;
					}
					else cnt++;
				}
			}


			if(!canRemove && fv.size()>1 && PREPROCESS>=ALL && !gn.isChangingVar(fv)) { //merge into 1 state

				if(DEBUG_PreprocessRules) {System.out.println("RULE: Merging states in leaf: " + fv);}
				if(DEBUG_PedigreeVerbose2) {System.out.println("Merging states in leaf: \noldCPT\n" + cpt);}

				double newcpt[] = new double[cpt.getCPLength() / fv.size()];
				int indx=0;
				for(int i=0; i<newcpt.length; i++) {
					for(int j=0; j<fv.size(); j++) {
						newcpt[i] += cpt.getCP(indx); indx++;
					}
				}

				while(fv.size()>1) {
					gn.bn.removeState(fv, 0);
				}
				cpt = fv.getCPTShell().getCPT();
				fv.set(0, "valid");
				for(int i=0; i<newcpt.length; i++) {
					cpt.setCP(i, newcpt[i]);
				}

				if(DEBUG_PedigreeVerbose2) {System.out.println("Merging states in leaf: \nnewCPT\n" + cpt);}
			}

			//leaf has 1 state
			if(!canRemove && PREPROCESS>=ALL && !gn.isChangingVar(fv)) { //if all states are identical can remove variable
				boolean allEqual = true;
				double val = cpt.getCP(0); //if all CPT values are equal, then can make it a constant
				for(int i=0; i<cpt.getCPLength(); i++) {
					if(Math.abs(val - cpt.getCP(i)) > epsilon) { //did not match
						allEqual = false;
						break;
					}
				}
				if(allEqual) {
					if(DEBUG_PreprocessRules) {System.out.println("RULE: Leaf becoming a constant: " + fv + " = " + val);}
					gn.addConstant(val, null);
					canRemove = true;
				}
			}


			if(canRemove) {
				ret=true;
				for(Iterator itr_par = gn.bn.inComing(fv).iterator(); itr_par.hasNext();) {
					FiniteVariable par = (FiniteVariable)itr_par.next();

					if(gn.bn.outDegree(par)==1) { //it will now become a leaf node
						varsToTry.add(par);
					}
				}
				gn.removeVariableAndMappings(fv);
				if(DEBUG_PreprocessRules) {System.out.println("RULE: Leaf Prune: " + fv);}
			}
			else if(fv.size()==1 && cpt.index().getNumVariables()==2) { //child w/ single parent & only 1 state can be merged into parent

				FiniteVariable par = cpt.index().variable(0);
				Table cptPar = par.getCPTShell().getCPT();

				ArrayList changingALPar = gn.getChangingVarAL(par);
				ArrayList changingALChi = gn.getChangingVarAL(fv);

				if(DEBUG_PedigreeVerbose2) {System.out.println("Attempting Leaf Merge\n" + "orig par\n" + cptPar + "\n"+changingALPar + "\norig child\n" + cpt + "\n" + changingALChi);}

				if(!gn.isChangingVar(fv)) {
					if(DEBUG_PreprocessRules) {System.out.println("RULE: Leaf Merge: " + fv);}
					if(gn.bn.outDegree(par)==1) varsToTry.add(par);
					ret=true;

					//child's table has x numbers, indexed by parent's state ordering
					//parent's cpt has those same states repeated once for each parent instantiation (but in same order)

					int indx1=0;
					while(indx1<cptPar.getCPLength()) {

						for(int i=0; i<cpt.getCPLength(); i++) {

							int parIndx = indx1+i;

							cptPar.setCP(parIndx, cptPar.getCP(parIndx) * cpt.getCP(i));

							if(changingALPar!=null) {
								NumberedConstant nc = (NumberedConstant)changingALPar.get(parIndx);
								nc.setAdditionalConst(nc.getAdditionalConst() * cpt.getCP(i));
							}
						}
						indx1 += cpt.getCPLength();
					}
					gn.removeVariableAndMappings(fv);
				}
				else if(!gn.isChangingVar(par)) { //parent is not changing, but child is (make parent changing & merge them)
					if(DEBUG_PreprocessRules) {System.out.println("RULE: Leaf Merge: " + fv);}
					if(gn.bn.outDegree(par)==1) varsToTry.add(par);
					ret=true;

					gn.mergingTwoVars_OneIsChanging(par, fv); //make parent a changing variable now
					{
						if(changingALPar!=null) throw new IllegalStateException(par + " already had a changingALPar " + changingALPar + "\n" + cptPar);
						changingALPar = new ArrayList();
						gn.changingRecomCPTs.put(par, changingALPar);
					}

					if(changingALChi==null) throw new IllegalStateException();

					//child's table has x numbers, indexed by parent's state ordering
					//parent's cpt has those same states repeated once for each parent instantiation (but in same order)

					int indx1=0;
					while(indx1<cptPar.getCPLength()) {

						for(int i=0; i<cpt.getCPLength(); i++) {

							int parIndx = indx1+i;

							NumberedConstant ncChi = (NumberedConstant)changingALChi.get(i);
							NumberedConstant ncPar = new NumberedConstant(ncChi);
							changingALPar.add(ncPar);

							ncPar.setAdditionalConst(ncPar.getAdditionalConst() * cptPar.getCP(parIndx));

							cptPar.setCP(parIndx, ncPar.getValReal());
						}
						indx1 += cpt.getCPLength();
					}
					gn.removeVariableAndMappings(fv);
				}
				else if(gn.areInSameChangingS(par, fv)) { //both are changing, if part of same S can handle that (only 1 changing value)
					if(DEBUG_PreprocessRules) {System.out.println("RULE: Leaf Merge: " + fv);}
					if(gn.bn.outDegree(par)==1) varsToTry.add(par);
					ret=true;

					if(changingALPar==null) throw new IllegalStateException();
					if(changingALChi==null) throw new IllegalStateException();

					//child's table has x numbers, indexed by parent's state ordering
					//parent's cpt has those same states repeated once for each parent instantiation (but in same order)

					int indx1=0;
					while(indx1<cptPar.getCPLength()) {

						for(int i=0; i<cpt.getCPLength(); i++) {

							int parIndx = indx1+i;

							NumberedConstant ncChi = (NumberedConstant)changingALChi.get(i);
							NumberedConstant ncPar = (NumberedConstant)changingALPar.get(parIndx);

							ncPar.increment(ncChi);
							cptPar.setCP(parIndx, ncPar.getValReal());
						}
						indx1 += cpt.getCPLength();
					}
					gn.removeVariableAndMappings(fv);
				}
				else {//TODO cannot currently support this if both parent & child are changing with different values
				}

				if(DEBUG_PedigreeVerbose2) {System.out.println("Attempting Leaf Merge\n" + "new par\n" + cptPar + "\n" + changingALPar);}
			}
		}
		return ret;
	}


	/**Return true if changed anything.*/
	static private boolean doSingleChildRemoval(FiniteVariable fvarTmp, GeneticNetwork gn) {
		if(PREPROCESS<VALID_NET) { return false;}
		boolean ret = false;

		LinkedList rootToCompress = new LinkedList();
		rootToCompress.add(fvarTmp);

		while(!rootToCompress.isEmpty()) {
			FiniteVariable fv = (FiniteVariable)rootToCompress.removeLast();
			FiniteVariable chi = (FiniteVariable)gn.bn.outGoing(fv).iterator().next();


			if(gn.isChangingVar(fv)) { continue;}
			else if(gn.isChangingVar(chi)) { //child is changing, but parent is not

				boolean handleHere = true;//to handle it, it must satisfy all the following criteria

				//this usually is satisfied when parent is .5/.5 & child is changing (in which case child
				//  becomes .5/.5 and is no longer changing)

				if(gn.bn.inDegree(fv)>0) {
					handleHere = false;
					continue;
				}

				//parent probabilities must all match
				Table tmpcpt = fv.getCPTShell().getCPT();
				double val = tmpcpt.getCP(0);
				for(int i=0; i<tmpcpt.getCPLength(); i++) {
					if(Math.abs(val - tmpcpt.getCP(i)) > epsilon) { //no match
						handleHere = false;
						break;
					}
				}
				if(!handleHere) continue;

				//child must sum to one for all parent instantiations
				tmpcpt = chi.getCPTShell().getCPT();
				int indx = 0;
				while(indx < tmpcpt.getCPLength()) {
					double sum = 0;
					for(int i=0; i<chi.size(); i++) {
						sum += tmpcpt.getCP(indx); indx++;
					}
					if(Math.abs(1.0 - sum) > epsilon) { //no sum to 1
						handleHere = false;
						break;
					}
				}
				if(!handleHere) continue;

				//handle it here
				{
					if(DEBUG_PreprocessRules) {System.out.println("RULE: Single Child Removal (special): " + fv + " -> " + chi);}
					if(!gn.bn.removeEdge(fv,chi)) throw new IllegalStateException("!removeEdge");
					tmpcpt = chi.getCPTShell().getCPT();
					for(int i=0; i<tmpcpt.getCPLength(); i++) {
						tmpcpt.setCP(i, val);
					}
					gn.removeVariableAndMappings(fv);
					gn.noLongerChanging(chi);
					ret = true;

					if(gn.bn.outDegree(chi) == 1) rootToCompress.add(chi);
					continue;
				}
			}

			ret = true;

			if(DEBUG_PreprocessRules) {System.out.println("RULE: Single Child Removal: " + fv + " -> " + chi);}

			Table parTbl = fv.getCPTShell().getCPT();
			Table chiTbl = chi.getCPTShell().getCPT();

			if(DEBUG_PedigreeVerbose2) {System.out.println(parTbl + "\n" + chiTbl);}

			Table newTbl = parTbl.multiply(chiTbl);
			newTbl = newTbl.forget(Collections.singleton(fv));

			if(DEBUG_PedigreeVerbose2) {System.out.println(newTbl);}

			for(Iterator parItr = gn.bn.inComing(fv).iterator(); parItr.hasNext();) {
				FiniteVariable parIncom = (FiniteVariable)parItr.next();

				if(!gn.bn.containsEdge(parIncom, chi)) {
					if(!gn.bn.addEdge(parIncom, chi, false)) { throw new IllegalStateException("!addEdge");}
				}
				else if(gn.bn.outDegree(parIncom)==2 && gn.bn.inDegree(parIncom)<=1) {
					rootToCompress.add(parIncom);
				}
			}

			if(!((BeliefNetworkImpl)gn.bn).removeEdgeNoCPTChanges(fv,chi)) { throw new IllegalStateException("!removeEdge");}
			gn.removeVariableAndMappings(fv);

			chi.setCPTShell(new TableShell(newTbl)); //This must be done after removeVariable, as that attempts to change the cpt
		}
		return ret;
	}//end remove single child vars


	//This function is heavily dependent on how Table.expand works!
	static private void updateChangingAL_expand(Table cpt, ArrayList al) {
		if(cpt==null) throw new IllegalArgumentException();
		if(al==null) return;

		TableIndex ti = cpt.index();
		int newSize = ti.variable(ti.getNumVariables()-2).size(); //size of variable just added
		int oldSize = ti.variable(ti.getNumVariables()-1).size(); //size of variable whose cpt it is (the last one)

		ArrayList newAL = new ArrayList();
		int counterAdds=0;
		for(int counterLoops=0; counterAdds < cpt.getCPLength(); counterLoops++) {
			for(int iNew=0; iNew<newSize; iNew++) {
				for(int iOld=0; iOld<oldSize; iOld++) {
					newAL.add(new NumberedConstant((NumberedConstant)al.get(counterLoops*oldSize+iOld)));
					counterAdds++;
				}
			}
		}

		al.clear();
		al.addAll(newAL);

		if(DEBUG_Test2) {
			if(ti.size()!=al.size()) throw new IllegalStateException("ChangingAL doesn't match (length):\n" + cpt + "\n" + al);
			for(int i=0; i<ti.size(); i++) {
				if(Math.abs(cpt.getCP(i)-((NumberedConstant)al.get(i)).getValReal()) > epsilon) throw new IllegalStateException("ChangingAL doesn't match (pr["+i+"]):\n" + cpt + "\n" + al);
			}
		}
	}

	//TODO: this really isn't very accurate and could potentially lead to bugs
	static private void updateChangingAL_reduce(Table cpt, ArrayList al) {
		if(cpt==null) throw new IllegalArgumentException();
		if(al==null) return;

		int nextALIndx = 0;
		for(int i=0; i<cpt.getCPLength(); i++) {
			double cptval = cpt.getCP(i);

			while(Math.abs(cptval - ((NumberedConstant)al.get(nextALIndx)).getValReal())>epsilon) al.remove(nextALIndx);

			nextALIndx++;//found match, increment
		}
		while((al.size()-1) >= nextALIndx) al.remove(al.size()-1);
		if(cpt.getCPLength()!=al.size()) {
			System.out.println("ERROR:");
			System.out.println("CPT:" + cpt);
			System.out.println("AL:" + al);
			throw new IllegalStateException("cpt != al");
		}
	}

	static private void founderInclude(MyFV g, double[] removedPr, ArrayList statesChi) {

		if(g.size()!=removedPr.length) throw new IllegalStateException();

		Table tbl = g.fv.getCPTShell().getCPT();
		int indx = tbl.index().variableIndex(g.fv);

		if(DEBUG_PedigreeVerbose2) {System.out.println("pre-founderInclude "+g + "\n" + tbl);}

		for(TableIndex.Iterator itr=tbl.index().iterator(); itr.hasNext();) {
			int ci = itr.next();
			String stateG = g.instance(itr.current()[indx]);
			double newPr = removedPr[statesChi.indexOf(stateG)];
			tbl.setCP(ci, tbl.getCP(ci)*newPr);
		}

		if(DEBUG_PedigreeVerbose2) {System.out.println("post-founderInclude "+g + "\n" + tbl);}
	}



	static private void handleFounderRemoval(MyFV parRemoved, ArrayList chiStates, double[] chiCPT,
			boolean otherOther, ArrayList chiPatOtherStates, GeneticNetwork gn) {

		if(DEBUG_PedigreeVerbose2) {System.out.println("handleFounderRemoval "+parRemoved);}

		CPTShell parCPT = parRemoved.fv.getCPTShell();
		if(parCPT==null) { //parent was homozygous and had evidence
			if(!gn.hasPosEvidence(parRemoved.fv)) throw new IllegalStateException(parRemoved + " did not have a cpt, but also did not have evidence");
			Arrays.fill(chiCPT, 1.0);
			return; //evidence is already included as constant when evidence was detected
		}
		else {
			for(int i=0; i<parRemoved.size(); i++) {
				String parentState = parRemoved.instance(i);
				stateParser.parse(parentState);
				parentState = stateParser.s1; //Create child by assuming that S=L
				int mapIntoIndx;

				if(parentState.equals(other)) {//confirm IF i is in child's other
					mapIntoIndx = chiStates.indexOf(other);
					if(!otherOther) mapIntoIndx=-1;
				}
				else {//parent is not in other
					mapIntoIndx = chiStates.indexOf(parentState);
					if(mapIntoIndx < 0) { //G does not have this state, it is possibly in the "other" state

						//it is a valid parent of "other" if it is a subset of the child's "other", & since parent only has one element, see if it is in there
						if(chiPatOtherStates.contains(parentState)) {
							mapIntoIndx = chiStates.indexOf(other);
						}
					}
				}

				if(mapIntoIndx>=0) chiCPT[mapIntoIndx] += parCPT.getCP(i);
			}
		}

		if(DEBUG_PedigreeVerbose2) {
			System.out.println("handleFounderRemoval "+parRemoved);
			for(int i=0; i<chiCPT.length; i++) System.out.print(chiCPT[i] + ", ");
			System.out.println("");
		}

	}


	static private boolean canOtherInheritOther(Person.PossibleAllele childHap, Person.TransmittedAllele childTA,
												Person.PossibleAllele parHap, Person.TransmittedAllele parTA) {
		boolean ret = true;
		for(int a=0; a<childHap.allele.length; a++) {
			if(parHap.allele[a] && !parTA.transmitted[a] &&				//possible in parent & not transmitted (so in other)
				(!childHap.allele[a] || childTA.transmitted[a])) {		//either not possible in child or transmitted (not in other)
				ret = false;
				break;
			}
		}
		return ret;
	}


	//When A=B, let all children come from one (e.g. A), but allow A&B to have different parent sets
	//  and create an extra child which enforces the equivalence.
	//To use this function, first create the A variable and a mapping into to for the B variable
	//  (this will force children to all come from A).
	//Pass as a parameter the "mapped" B variable into this function and it will generate the
	//  actual B (for the different parents) and the extra child to enforce equivalence.
	//It will return the actual B, so that parents can be added to it.
	//The CPT for the returned value will be created and it will contain all 1.0,
	//  it is the callers responsibility to then adjust this to the correct values.
	static private MyFV createEquivVar(MyFV in, GeneticNetwork gn) {
		ArrayList states = new ArrayList(in.size());
		for(int i=0; i < in.size(); i++) {
			states.add(in.instance(i));
		}

		MyFV ret = new MyFV(new FiniteVariableImpl("duplicate_" + in.id, states));
		gn.addVariable(ret);

		{
			double cpt[] = new double[in.size()];
			Arrays.fill(cpt, 1.0);
			ret.fv.setCPTShell(new TableShell( new Table( Collections.singletonList(ret.fv), cpt)));
		}
		gn.equivPreProcessVars.add(ret.fv);

		if(in.size()>1) { //don't need child to enforce equivalence if only 1 state
			FiniteVariable equ = new FiniteVariableImpl("equiv_" + in.id, Collections.singletonList("valid"));
			gn.addVariable(new MyFV(equ));

			gn.equivPreProcessVars.add(equ);

			double cpt[] = new double[in.size()*ret.size()];
			int indx = 0;
			for(int i=0; i<in.size(); i++) {
				String istr = in.instance(i);

				for(int j=0; j<ret.size(); j++) {
					String jstr = ret.instance(j);

					if(istr.equals(jstr)) cpt[indx]=1.0;
					else cpt[indx]=0.0;

					indx++;
				}
			}
			Table t = new Table(new FiniteVariable[]{in.fv, ret.fv, equ}, cpt);
			addEdges(gn, t);
			equ.setCPTShell(new TableShell( t));
		}
		return ret;
	}

	static private void moveInComing(MyFV mappedTo, GeneticNetwork gn) {

		if(gn.bn.inDegree(mappedTo.fv)>0) {//create a duplicate variable to put the in-edges on

			if(DEBUG_PedigreeVerbose2) {System.out.println("Duplicate " + mappedTo + " and move in-edges there.");}

			MyFV dupG = createEquivVar(mappedTo, gn);
			//move edges by swapping CPTs & re-mapping variable
			{
				Table cpt1 = mappedTo.fv.getCPTShell().getCPT();
				Table cpt2 = dupG.fv.getCPTShell().getCPT();

				if(DEBUG_PedigreeVerbose2) {System.out.println("orig mapped \n" + cpt1);}
				if(DEBUG_PedigreeVerbose2) {System.out.println("orig duplicate \n" + cpt2);}

				HashMap oldToNew1 = Maps.identityMap(cpt1.variables());
				HashMap oldToNew2 = Maps.identityMap(cpt2.variables());
				oldToNew1.put(mappedTo.fv, dupG.fv);
				oldToNew2.put(dupG.fv, mappedTo.fv);
				cpt1.replaceVariables(oldToNew1);
				cpt2.replaceVariables(oldToNew2);

				mappedTo.fv.setCPTShell(new TableShell(cpt2));
				dupG.fv.setCPTShell(new TableShell(cpt1));

				if(DEBUG_PedigreeVerbose2) {System.out.println("new mapped \n" + cpt2);}
				if(DEBUG_PedigreeVerbose2) {System.out.println("old duplicate \n" + cpt1);}

				{
					for(Iterator itr=oldToNew1.keySet().iterator(); itr.hasNext();) {
						FiniteVariable par = (FiniteVariable)itr.next();
						if(par==mappedTo.fv) continue;
						if(!((BeliefNetworkImpl)gn.bn).removeEdgeNoCPTChanges(par,mappedTo.fv)) { throw new IllegalStateException("!removeEdge" + par + " " + mappedTo.fv);}
					}
					addEdges(gn, cpt1);
				}

				if(DEBUG_PedigreeVerbose2) {System.out.println("mapped    " + gn.bn.inComing(mappedTo.fv));}
				if(DEBUG_PedigreeVerbose2) {System.out.println("duplicate " + gn.bn.inComing(dupG.fv));}
			}
		}
	}


	/**Currently only supports positive evidence.*/
	static private class Evidence {
		String fvID;
		Object posState = null;
		HashSet negState = null;

		Evidence(String fvID) {
			this.fvID = fvID;
		}

		void addPositive(Object state) {
			if(state==null) throw new IllegalArgumentException();
			if(posState==null) posState = state;
			else if(!state.equals(posState)) throw new IllegalStateException("Added two different positive states: " + posState + " and " + state);

			negState=null; //don't track negative evidence if have positive evidence
		}

		void addNegative(Object state) {
			if(state==null) throw new IllegalArgumentException();
			if(state.equals(posState)) throw new IllegalArgumentException("Added negative evidence which was already positive: " + state);
			if(posState!=null) {return;} //don't track negative evidence if have positive evidence
			if(negState==null) {negState = new HashSet();}
			negState.add(state);
		}

		public boolean hasPositive() {return posState!=null;}
		public boolean hasNegative() {return negState!=null;}

		public String evidFileString_Pos() {
			if(hasPositive()) return "<inst id=\""+fvID+"\" value=\""+posState+"\"/>";
			else throw new IllegalStateException("Currently only supports positive evidence");
		}
		public String evidFileString_All() {
			if(hasPositive()) return "<inst id=\""+fvID+"\" value=\""+posState+"\"/>";
			else if(hasNegative()) {
				StringBuffer ret = new StringBuffer();
				for(Iterator itr = negState.iterator(); itr.hasNext();) {
					ret.append("<inst id=\""+fvID+"\" value=\""+itr.next()+"\" negative=\"true\" />");
					if(itr.hasNext()) ret.append("\n");
				}
				return ret.toString();
			}

			else throw new IllegalStateException("Currently only supports positive evidence");
		}

		public String toStringAll() {
			StringBuffer ret = new StringBuffer();
			if(hasPositive()) ret.append(fvID+"="+posState);
			if(hasNegative()) ret.append(fvID+"!="+negState);
			return ret.toString();
		}

		public String toString() { return evidFileString_All();}
	}


	static private class NumberedConstant {
		double additionalConst=1; //no scaling involved
		private double recomValue; //actual value which would be in cpt (no scaling)(should be same as from locus file so it can be changed)
		private int numberOfPosTimes=0; //this is number of recomValue entries
		private int numberOfNegTimes=0; //this is (1-recomValue)

		public NumberedConstant(double rValue) {
			this(rValue, 1.0);
		}
		public NumberedConstant(double rValue, double additionalC) {
			this.recomValue = rValue;
			this.additionalConst = additionalC;
		}
		public NumberedConstant(NumberedConstant nc) {
			this.additionalConst = nc.additionalConst;
			this.recomValue = nc.recomValue;
			this.numberOfPosTimes = nc.numberOfPosTimes;
			this.numberOfNegTimes = nc.numberOfNegTimes;
		}
		public void incrementPos() {numberOfPosTimes++;}
		public void incrementNeg() {numberOfNegTimes++;}
		public void increment(NumberedConstant nc) {
			if(Math.abs(nc.recomValue-recomValue)<epsilon) {numberOfPosTimes+=nc.numberOfPosTimes; numberOfNegTimes+=nc.numberOfNegTimes;additionalConst*=nc.additionalConst;}
			else if(Math.abs(nc.recomValue-(1-recomValue))<epsilon) {numberOfPosTimes+=nc.numberOfNegTimes; numberOfNegTimes+=nc.numberOfPosTimes;additionalConst*=nc.additionalConst;}
			else throw new IllegalStateException("Constant Error: " + nc + " " + recomValue);
		}
		public void increment(double val) {
			if(Math.abs(val-recomValue)<epsilon) {incrementPos();}
			else if(Math.abs(val-(1-recomValue))<epsilon) {incrementNeg();}
			else throw new IllegalStateException("Constant Error: " + val + " " + recomValue);
		}
		public boolean incrementTest(double val) {
			if(Math.abs(val-recomValue)<epsilon) {incrementPos();}
			else if(Math.abs(val-(1-recomValue))<epsilon) {incrementNeg();}
			else return false;
			return true;
		}
		public void setAdditionalConst(double in) {
			additionalConst = in;
		}
		public double getAdditionalConst() { return additionalConst;}
		/**This function could underflow and return 0.*/
		public double getValReal() {return Math.pow(recomValue, numberOfPosTimes) * Math.pow((1-recomValue), numberOfNegTimes) * additionalConst;}
		public double getValScaled(double scalar) {
			//TODO this presently doesn't work correctly, because it could underflow with scaling
			double ret = Math.pow(getValReal(), 1/scalar); //scale the value
			if(ret==0) throw new IllegalStateException("Underflow in constant.");
			return ret;
		}
		public String toString() { return recomValue + "^" + numberOfPosTimes + "+" + (1-recomValue) + "^" + numberOfNegTimes + "*" + additionalConst + "=" + getValReal();}
	}



	abstract static public class PseudoLoci
	{
		abstract public int getNumLoci();
		abstract public int getNumPheno(); //complexAffection has one fewer
		abstract public int getLociType(int lociIndx);
		abstract public boolean isComplex1(int lociIndx);
		abstract public boolean isComplex2(int lociIndx);
		abstract public int getComplex1InputOrd(int locusInputOrd);
		abstract public int getNumAlleles(int lociIndx);
		abstract public int getChangingS1();
		abstract public int getChangingS2();
		abstract public double getRecombM(String sName, int i);
		abstract public double getRecombF(String sName, int i);
		abstract public double getPenetrance(int locusInputOrd, String p, int i, int j);
		abstract public double getComplexPenetrance(int locusInputOrd, String p, int i, int j, int k);
		abstract public int inputOrd(int indx);
		abstract public int chromoOrd(int indx);
		abstract public double geneFreq(int locusInputOrd, int allele);
		abstract public boolean sexLinked();
	}

	static private class PseudoLoci_Loci extends PseudoLoci
	{
		private Loci loci;

		PseudoLoci_Loci(Loci loci)
		{
			this.loci = loci;
		}
		public Loci getLoci() { return loci; }
		public int getNumLoci() { return loci.header().numLoci; }
		public int getNumPheno() { if (loci.header().numComplexAffection == 2) return getNumLoci() - 1; else return getNumLoci(); }
		public int getLociType(int lociIndx) { return loci.loci(lociIndx).type;}
		public boolean isComplex1(int lociIndx) { return loci.loci(lociIndx).data instanceof Loci.LocusDataComplexAffection1; }
		public boolean isComplex2(int lociIndx) { return loci.loci(lociIndx).data instanceof Loci.LocusDataComplexAffection2; }
		public int getComplex1InputOrd(int locusInputOrd) { return ((Loci.LocusDataComplexAffection2)loci.loci[locusInputOrd].data).firstComplexLociInputOrd; }
		public int getNumAlleles(int lociIndx) { return loci.loci[lociIndx].numAlleles; }
		public int getChangingS1() { return loci.getChromoOrd_ChangingS1(); }
		public int getChangingS2() { return loci.getChromoOrd_ChangingS2(); }
		public double getRecombM(String sName, int i) { return loci.ftr.recombinationValues_male[i]; }
		public double getRecombF(String sName, int i) { return loci.ftr.recombinationValues_female[i]; }
		public double getPenetrance(int locusInputOrd, String p, int i, int j) { return ((Loci.LocusDataAffectionStatus)loci.loci[locusInputOrd].data).penetrances[i][j];}
		public double getComplexPenetrance(int locusInputOrd, String p, int i, int j, int k) { return ((Loci.LocusDataComplexAffection2)loci.loci[locusInputOrd].data).penetrances[i][j][k];}
		public int inputOrd(int indx) { return loci.header().chromosomeOrder[indx]; }
		public int chromoOrd(int indx) {return loci.header().inputToChromOrder[indx];}
		public double geneFreq(int locusInputOrd, int allele) { return loci.loci[locusInputOrd].data.geneFreq[allele]; }
		public boolean sexLinked() { return loci.header().sexLinked==1;}
	}

	//Only works with non-pruned networks (any pruning will most likely
	//  break this class.
	//Currently does not handle changing S values (possible implement in future)
	static private class PseudoLoci_BN extends PseudoLoci
	{
		private BeliefNetwork bn;
		PseudoLoci_BN(BeliefNetwork bn)
		{
			this.bn = bn;
		}
		public int getNumLoci() {
			int cnt = 0;
			while(bn.forID("Gp1_"+cnt)!=null) cnt++; //look for how many Gp variables person 1 has
			return cnt;
		}
		public int getNumPheno() { //complexAffection has one fewer than numLoci
			int numLoci = getNumLoci();
			int cnt = 0;
			for(int i=0; i<numLoci; i++) {
				if(bn.forID("P1_"+i)!=null) cnt++;
			}
			return cnt;
		}
		public int getLociType(int lociIndx) {
			FiniteVariable fv = (FiniteVariable)bn.forID("P1_"+lociIndx);
			if(fv==null) return 4; //only phenotypes of type 4a can have no phenotype
			else if(bn.inDegree(fv)==4) return 4; //only phenotypes of type 4b have 4 parents
			else if(fv.size()>2) return 3; //numbered phenotypes are guaranteed to have > 2 states
			else if(fv.size()==2) return 1;
			else throw new IllegalStateException();
		}
		public boolean isComplex1(int lociIndx) {
			FiniteVariable fv = (FiniteVariable)bn.forID("P1_"+lociIndx);
			if(fv==null) return true; //only phenotypes of type 4a can have no phenotype
			else return false;
		}
		public boolean isComplex2(int lociIndx) {
			FiniteVariable fv = (FiniteVariable)bn.forID("P1_"+lociIndx);
			if(fv!=null && bn.inDegree(fv)==4) return true; //only phenotypes of type 4b have 4 parents
			else return false;
		}
		public int getComplex1InputOrd(int locusInputOrd) {
			int numLoci = getNumLoci();
			for(int i=0; i<numLoci; i++) {
				if(isComplex1(i)) return i;
			}
			throw new IllegalStateException(); //not found
		}
		public int getNumAlleles(int lociIndx) {
			FiniteVariable fv = (FiniteVariable)bn.forID("Gp1_"+lociIndx);
			return fv.size();
		}
		public int getChangingS1() { return -1;} //TODO can't detect this (for testing can hardcode it)
		public int getChangingS2() { return -1;} //TODO can't detect this (for testing can hardcode it)
		public double getRecombM(String sName, int loc) {
			FiniteVariable fv = null;
			fv = (FiniteVariable)bn.forID(sName);
			if (fv == null) throw new IllegalStateException("Could not find: " + sName);
			return fv.getCPTShell().getCP(1); //0th element is 1-recom, 1st is recom
		}
		public double getRecombF(String sName, int loc) {
			FiniteVariable fv = null;
			fv = (FiniteVariable)bn.forID(sName);
			if (fv == null) throw new IllegalStateException("Could not find: " + sName);
			return fv.getCPTShell().getCP(1); //0th element is 1-recom, 1st is recom
		}
		public double getPenetrance(int locusInputOrd, String p, int i, int j) {
			int chrOrd = locusInputOrd; //don't convert
			FiniteVariable fv = (FiniteVariable)bn.forID(p);
			if (bn.inDegree(fv) != 2) throw new IllegalStateException(); //test for correctness
			if (getLociType(chrOrd) != 1) throw new IllegalStateException(); //test for correctness
			//i is handled by the fact that I have correct loci
			//j is either 0,1,2 meaning gm=gp=0, gm!=gp, gm=gp=1 respectively
			//   therefore index into cpt at 0,2,6
			if (j == 0) return fv.getCPTShell().getCP(0);
			else if (j == 1) return fv.getCPTShell().getCP(2);
			else if (j == 2) return fv.getCPTShell().getCP(6);
			throw new IllegalStateException();
		}
		public double getComplexPenetrance(int locusInputOrd, String p, int i, int j, int k)
		{
			int chrOrd = locusInputOrd; //don't convert
			FiniteVariable fv = (FiniteVariable)bn.forID(p);
			if (bn.inDegree(fv) != 4) throw new IllegalStateException(); //test for correctness
			if (getLociType(chrOrd) != 4) throw new IllegalStateException(); //test for correctness
			//i is handled by the fact that I have correct loci
			//j is either 0,1,2 meaning gm2=gp2=0, gm2!=gp2, gm2=gp2=1 respectively
			//k is either 0,1,2 meaning gm1=gp1=0, gm1!=gp1, gm1=gp1=1 respectively
			if(k<0 || k>2) throw new IllegalStateException();
			if (j == 0) return fv.getCPTShell().getCP((k==0?0:(k==1?8:(k==2?24:-1))));
			else if (j == 1) return fv.getCPTShell().getCP((k==0?2:(k==1?10:(k==2?26:-1))));
			else if (j == 2) return fv.getCPTShell().getCP((k==0?6:(k==1?14:(k==2?30:-1))));
			throw new IllegalStateException();
		}
		public int inputOrd(int indx) { return indx+1; } //only used once, need to compensate for -1 during call
		public int chromoOrd(int indx) { return indx; }
		public double geneFreq(int locusInputOrd, int allele) {
			int chrOrd = locusInputOrd; //don't convert
			FiniteVariable fv = (FiniteVariable)bn.forID("Gp1_" + chrOrd);
			if (fv == null) throw new IllegalStateException("Could not find: " + "Gp1_" + chrOrd);
			if (bn.inDegree(fv) != 0) throw new IllegalStateException(); //test for correctness
			return fv.getCPTShell().getCP(allele);
		}
		public boolean sexLinked() { return false; } //TODO don't currently support this
	}

}//end class Pedigree


