package edu.ucla.belief.io.geneticlinkage;

import edu.ucla.util.*;

import java.io.*;
import java.util.*;

public class Person
{
	final int pedigreeID;
	final int personID;//indexed 1,2,...
	final int fatherID;//0=founder, implies motherID=0;
	final int motherID;//0=founder, implies fatherID=0;
	final boolean male;

	final int firstSib;
	final int nextPaternalSib;
	final int nextMaternalSib;

	final PossibleAllele paternalHaplotype[];//indexed by locus
	final PossibleAllele maternalHaplotype[];//indexed by locus
	final Phenotype phenotype[];//indexed by locus (phenotypes are renumbered 0..num-1 instead of 1..num

	final TransmittedAllele transmittedAllele[]; //these are only initialized (to non-null) if Pedigree.alleleRecode is called


	public Person(int numLoci, int pedigreeID, int personID,
				  int fatherID, int motherID,
				  int firstSib, int nextPaternalSib,
				  int nextMaternalSib, boolean male, Pedigree.PseudoLoci loci) {
		paternalHaplotype = new PossibleAllele[numLoci];
		maternalHaplotype = new PossibleAllele[numLoci];
		phenotype = new Phenotype[numLoci];
		transmittedAllele = new TransmittedAllele[numLoci];
		this.pedigreeID = pedigreeID;
		this.personID = personID;
		this.fatherID = fatherID;
		this.motherID = motherID;
		this.male = male;
		this.firstSib = firstSib;
		this.nextPaternalSib = nextPaternalSib;
		this.nextMaternalSib = nextMaternalSib;

		for(int i=0; i<numLoci; i++) {
			paternalHaplotype[i]=new PossibleAllele();
			maternalHaplotype[i]=new PossibleAllele();
			phenotype[i]=new Phenotype(loci.getLociType(i));
		}
	}


	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("[");
		ret.append(pedigreeID);
		ret.append(",");
		ret.append(personID);
		ret.append(",");
		ret.append(fatherID);
		ret.append(",");
		ret.append(motherID);
		ret.append(",");
		ret.append(male);
		ret.append(",");
		ret.append(firstSib);
		ret.append(",");
		ret.append(nextPaternalSib);
		ret.append(",");
		ret.append(nextMaternalSib);
		ret.append("]");
		return ret.toString();//include phenotype data?
	}



//To Make things easier during simplification
	Person father;
	Person mother;
	public int depth=-1;

	NuclearFamily childInFam=null; //can only be a child in one family (possibly null if founder)
	final HashSet parInFam = new HashSet(); //can be married multiple times (or could be empty)


	public int numChildren() {
		int numChildren=0;
		for(Iterator itr=parInFam.iterator(); itr.hasNext();) {
			NuclearFamily nf = (NuclearFamily)itr.next();
			numChildren+=nf.child.length;
		}
		return numChildren;
	}


	public boolean isFounder() {
		if(fatherID==0 && motherID==0) { return true;}
		else if(fatherID==0 || motherID==0) { throw new IllegalStateException("Person " + personID + " has motherID " + motherID + " and fatherID " + fatherID);}
		else return false;
	}


	void hasChanged(HashSet needsUpdate, NuclearFamily updateFrom) {
		if(childInFam!=null && childInFam!=updateFrom) {
			needsUpdate.add(childInFam);
			childInFam.personChanged(this);
		}
		for(Iterator itr = parInFam.iterator(); itr.hasNext();) {
			NuclearFamily fam = (NuclearFamily)itr.next();
			if(fam!=updateFrom) {
				needsUpdate.add(fam);
				fam.personChanged(this);
			}
		}
	}


	//if both haplotypes are known, set phenotype
	//if one haplotype is known, set partial phenotype and attempt to learn other haplotype from phenotype
	//return true if changed anything
	//if know any two of (haplo1, haplo2, pheno) this will learn the 3rd.
	boolean learnPhenoLocally(int locus) {
		if(phenotype[locus].type==3) {

			boolean changed = false;

			PossibleAllele pat = paternalHaplotype[locus];
			PossibleAllele mat = maternalHaplotype[locus];
			Phenotype ph = phenotype[locus];

			if(pat.numValidStates==1 && mat.numValidStates==1) {//know both genotypes
				changed |= ph.setNumbered(pat.validState, mat.validState);
			}
			else if(pat.numValidStates==1) {
				changed |= ph.setNumbered(pat.validState);
				int other = ph.getOther(pat.validState);
				if(other>=0) {mat.setState(other); changed=true;}
			}
			else if(mat.numValidStates==1) {
				changed |= ph.setNumbered(mat.validState);
				int other = ph.getOther(mat.validState);
				if(other>=0) {pat.setState(other); changed=true;}
			}
			return changed;
		}
		return false;
	}


	static class TransmittedAllele {

		final boolean transmitted[];

		public TransmittedAllele(int len) {
			if(len<=0) {throw new IllegalStateException();}
			transmitted = new boolean[len];
		}
	}


	static class PossibleAllele {
		boolean allele[];//these are indexed 0..num-1 instead of 1..num
		int numValidStates=-1;
		int validState=-1; //only set when numValidStates==1

		void setLength(int len) {
			if(allele != null || len<=0) {throw new IllegalStateException();}
			allele = new boolean[len];
			Arrays.fill(allele, true);
			numValidStates = len;
			if(len==1) {validState=0;}
		}

		void setState(int state) {
			if(!allele[state]) {throw new IllegalStateException("Attemped to set a genotype to something not possible, inconsistent");}
			Arrays.fill(allele, false);
			allele[state]=true;
			numValidStates=1;
			validState=state;
		}

		private int findOnlyState() {
			for(int a=0; a<allele.length; a++) {
				if(allele[a]) {return a;}
			}
			System.err.println("ERROR: Called findOnlyState, but didn't find any.");
			return -1;
		}


		//Note: If this reduces the numValidStates to 1, should set partial phenotype (& possibly learn other haplotype)
		//return true if changed anything
		boolean reduceFromSavedArray(boolean savedA[]) {
			boolean changed = false;

			for(int a=0; a<allele.length; a++) {
				if(allele[a] && !savedA[a]) {
					allele[a] = false;
					numValidStates--;
					changed = true;
				}
			}
			if(changed && numValidStates==1) {validState = findOnlyState();}
			if(numValidStates<=0) {throw new IllegalStateException("No valid states left! (" + numValidStates + ")");}
			return changed;
		}



		//Note: If this reduces the numValidStates to 1, should set partial phenotype (& possibly learn other haplotype)
		//This doesn't work for sex-linked, as they only use one phenotype field
		//return true if changed anything
		boolean reduceNumberedFromPhenotype(Phenotype valid_ph) {
			if(valid_ph.type!=3) {throw new IllegalStateException("called reduceNumberedFromPhenotype on type " + valid_ph.type);}

			int p_one = valid_ph.one();
			int p_two = valid_ph.two();

			//if phenotype contains unknown, can't reduce based on this
			if(p_one==-1 || p_two==-1) { return false;}//(phenotypes are indexed 1,2,3..., while arrays are 0,1,2,3...)

			if(numValidStates==1) {
				if(validState!=p_one && validState!=p_two) {
					allele[validState]=false;
					validState=-1;
					numValidStates--;
					throw new IllegalStateException("");//no possible values then
				}
				return false;
			}
			else {
				boolean changed = false;
				for(int i=0; i<allele.length; i++) {
					if(allele[i] && i!=p_one && i!=p_two) {
						allele[i] = false;
						changed = true;
						numValidStates--;
					}
				}
				if(changed && numValidStates==1) {validState = findOnlyState();}
				if(numValidStates<=0) {throw new IllegalStateException("No valid states left! (" + numValidStates + ")");}
				return changed;
			}//end if already know value
		}

		//Note: If this reduces the numValidStates to 1, should set partial phenotype (& possibly learn other haplotype)
		//g1 & g2 are the parent alleles associated with this one
		//return true if changed anything
		boolean reduceHaplotypeFromParents(PossibleAllele g1, PossibleAllele g2) {
			if(numValidStates==1) {
				if(!g1.allele[validState] && !g2.allele[validState]) {
					allele[validState]=false;
					validState=-1;
					numValidStates--;
					throw new IllegalStateException("No valid states left!");//no possible values then
				}
				return false;
			}
			else {
				boolean changed = false;
				for(int i=0; i<allele.length; i++) {
					if(allele[i] && !g1.allele[i] && !g2.allele[i]) {
						allele[i] = false;
						changed = true;
						numValidStates--;
					}
				}
				if(changed && numValidStates==1) {validState = findOnlyState();}
				if(numValidStates<=0) {throw new IllegalStateException("No valid states left! (" + numValidStates + ")");}
				return changed;
			}//end if already know value
		}
	}//end class PossibleAllele


	/**Initial phenotype data will know either both or none, however
	 * this class allows for you to learn "half" the phenotype.
	 */
	static class Phenotype {
		final int type; //1=affection, 3=numbered, 4=complex

		/* For a numbered locus, one and two are the phenotypes, can possibly know "one" and not "two".
		 * For other types, "one" contains the phenotype and "two" contains the penetrance class
		 */
		private int one=-1;//-1=unknown (NOTE: This is different from pedigree file which bases things on 1,2,3...)
		private int two=-1;//-1=unknown

		public int one() {return one;}
		public int two() {return two;}

		public Phenotype(int type) {
			this.type = type;
			if(type!=1 && type!=3 && type!=4) {
				throw new IllegalStateException("Attempted to create a phenotype of type " + type);
			}
		}

		public String toString() {
			if(type==3) {
				return (one>=0 ? (""+one) : "?") + "_" + (two>=0 ? (""+two) : "?");
			}
			else {
				return one + "_.";
			}
		}


		//return true if it made any changes
		boolean setNumbered(int p) {
			if(type!=3) {throw new IllegalStateException("called setNumbered on type " + type);}

			if(p==one || p==two) {return false;}//already matches one of them
			else if(one==-1) {one=p; return true;}//learned first
			else if(two==-1) {two=p; return true;}//learned second
			else { throw new IllegalStateException("type: " + type + " old:" + one + "_" + two + " setting " + p);}//doesn't match
		}

		//return true if it made any changes
		boolean setNumbered(int o, int t) {
			if(type!=3) {throw new IllegalStateException("called setNumbered on type " + type);}

			if(o<0 || t<0) {throw new IllegalArgumentException("called setNumbered(" + o + "," + t + ")");}
			if(one>=0 && two>=0) {//already know both, verify correct
				if(!((one==o && two==t) || (one==t && two==o))) {
					throw new IllegalStateException("Attemped to set the phenotype to " + o + "_" + t + " but already had " + one + "_" + two);
				}
				return false;
			}
			else if(one>=0) {//already know one, verify its correct & set other
				if(one==o) {
					two = t;
				}
				else if(one==t) {
					two = o;
				}
				else {
					throw new IllegalStateException("Attemped to set the phenotype to " + o + "_" + t + " but already had " + one + "_?");
				}
				return true;
			}
			else {//don't know any, set them
				one=o;
				two=t;
				return true;
			}
		}

		void setFromInput(int o, int t) {
			one = o-1;
			two = t-1;//convert to 0 based instead of 1 based
		}

		int getOther(int in) {
			if(in==one) {return two;}
			else {return one;}
		}

		boolean fullyKnownHeterozygous() {
			if(type!=3) {return false;}
			else {
				if(one>=0 && two>=0 && one!=two) {return true;}
				return false;
			}
		}

		boolean fullyKnown() {
			if(type==3) {
				return one>=0 && two>=0;
			}
			else if(type==1 || type==4) {
				return one>=0;
			}
			else {
				throw new IllegalStateException();
			}
		}

		/*Return true for partially or fully known.*/
		boolean partiallyKnown() {
			return one>=0;
		}
	}//end class Phenotype




}//end class Person
