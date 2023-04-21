package edu.ucla.belief.io.geneticlinkage;

import edu.ucla.util.*;

import java.io.*;
import java.util.*;

public class NuclearFamily
{

	static final boolean DEBUG_TEST = false;

	final Person father;
	final Person mother;
	final Person child[];

	final boolean hasChanged[] = new boolean[3];//0=father, 1=mother, 3=any child




	public NuclearFamily(Person fa, Person mo, Person ch[]) {
		father = fa;
		mother = mo;
		child = ch;
		if(fa==null || mo==null || ch==null || ch.length<1) {
			throw new IllegalStateException("ERROR: Could not generate NuclearFamily: " + fa + ", " + mo);
		}
		Arrays.fill(hasChanged,true);//default to has changed
		if(DEBUG_TEST) {System.out.println("Generated: " + toString());}
	}


	void personChanged(Person p) {
		if(p==father) {hasChanged[0]=true;}
		else if(p==mother) {hasChanged[1]=true;}
		else {
			hasChanged[2]=true;
		}
	}

	public String toString() {
		//can identify family by first child (as they can only be the child in one family)
		StringBuffer ret = new StringBuffer("{Nuclear Family " + child[0].personID + ":");
		ret.append(father.personID + ", ");
		ret.append(mother.personID + "");
		for(int i=0; i<child.length; i++) {
			ret.append(", " + child[i].personID);
		}
		ret.append("}");
		return ret.toString();
	}


	//do full test
	//pass in saveA and forcedA, so all families can use same arrays without reallocating them
	//saveA[0=father,1=mother,2=child][0=paternal,1=maternal][allele]
	//forcedA[0=father,1=mother,2=child][allele]
	//forcedA is only used for numbered alleles, as phenotypes of non-numbered are not forced in family relations
	void simplifyUpdate_Full(int locus, boolean saveA[][][], boolean forcedA[][], HashSet needsUpdate) {

		needsUpdate.remove(this);
		if(DEBUG_TEST) {System.out.println("simplifyUpdate_Full on family " + child[0].personID);}

		boolean numberedLocus = (father.phenotype[locus].type==3);
		int numAlleles = father.paternalHaplotype[locus].allele.length;

		boolean done = false;
		while(!done) {
			done = true;


			{//initialize arrays
				for(int i=0; i<saveA.length; i++) {
					for(int j=0; j<saveA[i].length; j++) {
						Arrays.fill(saveA[i][j],false);
					}
				}
				if(numberedLocus) {
					Arrays.fill(forcedA[0],true);//father
					Arrays.fill(forcedA[1],true);//father
					Arrays.fill(forcedA[2],true);//father
				}
			}

			Person.PossibleAllele ff = father.paternalHaplotype[locus];
			Person.PossibleAllele fm = father.maternalHaplotype[locus];
			int fp1 = (numberedLocus ? father.phenotype[locus].one() : -1); //only for numbered, convert to 0 base
			int fp2 = (numberedLocus ? father.phenotype[locus].two() : -1); //only for numbered, convert to 0 base
			Person.PossibleAllele mf = mother.paternalHaplotype[locus];
			Person.PossibleAllele mm = mother.maternalHaplotype[locus];
			int mp1 = (numberedLocus ? mother.phenotype[locus].one() : -1); //only for numbered, convert to 0 base
			int mp2 = (numberedLocus ? mother.phenotype[locus].two() : -1); //only for numbered, convert to 0 base

			//for each father-mother genotype test kids
			for(int f1=0; f1<numAlleles; f1++) {
				if(!ff.allele[f1]) {continue;} //is allele valid
				for(int f2=0; f2<numAlleles; f2++) {
					if(!fm.allele[f2]) {continue;} //is allele valid

					//test father genotype with phenotype
					if(fp2>=0) {//know both
						if(!((f1==fp1 && f2==fp2) || (f1==fp2 && f2==fp1))) {continue;}
					}
					else if(fp1>=0) {//know one
						if(fp1!=f1 && fp1!=f2) {continue;}
					}

					for(int m1=0; m1<numAlleles; m1++) {
						if(!mf.allele[m1]) {continue;} //is allele valid
						for(int m2=0; m2<numAlleles; m2++) {
							if(!mm.allele[m2]) {continue;} //is allele valid

							//test mother genotype with phenotype
							if(mp2>=0) {
								if(!((m1==mp1 && m2==mp2) || (m1==mp2 && m2==mp1))) {continue;}
							}
							else if(mp1>=0) {
								if(mp1!=m1 && mp1!=m2) {continue;}
							}

							//four possible zygotes
							//[f1][m1]
							//[f1][m2]
							//[f2][m1]
							//[f2][m2]

							boolean ok = true;
							for(int ch=0; ch<child.length; ch++) {
								Person.PossibleAllele cf = child[ch].paternalHaplotype[locus];
								Person.PossibleAllele cm = child[ch].maternalHaplotype[locus];

								if(cf.allele[f1] && cm.allele[m1]) {//ok
								}
								else if(cf.allele[f1] && cm.allele[m2]) {//ok
								}
								else if(cf.allele[f2] && cm.allele[m1]) {//ok
								}
								else if(cf.allele[f2] && cm.allele[m2]) {//ok
								}
								else { //child did not satisfy any of these possible alleles
									ok = false;
									break;
								}
							}//end for each child

							if(ok) { //these are valid for all, save things

//							System.out.println(f1 + "_" + f2 + " x " + m1 + "_" + m2); //display all valid genotype combinations of parents

								saveA[0][0][f1] = true;//father
								saveA[0][1][f2] = true;
								saveA[1][0][m1] = true;//mother
								saveA[1][1][m2] = true;
								saveA[2][0][f1] = true;//child.paternal
								saveA[2][0][f2] = true;
								saveA[2][1][m1] = true;//child.maternal
								saveA[2][1][m2] = true;
								if(numberedLocus) {//test if phenotypes are forced
									for(int a=0; a<numAlleles; a++) {
										if(a!=f1 && a!=f2) {forcedA[0][a] = false;}//have valid w/o 'a' in father
										if(a!=m1 && a!=m2) {forcedA[1][a] = false;}//have valid w/o 'a' in mother

										if(a!=f1 && a!=m1) {forcedA[2][a] = false;}//have valid w/o 'a' in children
										if(a!=f1 && a!=m2) {forcedA[2][a] = false;}//have valid w/o 'a' in children
										if(a!=f2 && a!=m1) {forcedA[2][a] = false;}//have valid w/o 'a' in children
										if(a!=f2 && a!=m2) {forcedA[2][a] = false;}//have valid w/o 'a' in children
									}
								}
							}//end have valid assignment
						}//end m2
					}//end m1
				}//end f2
			}//end f1


			//adjust people based on saveA and forcedA

			//father
			{
				boolean changed = false;
				changed |= ff.reduceFromSavedArray(saveA[0][0]);
				changed |= fm.reduceFromSavedArray(saveA[0][1]);
				if(numberedLocus) {
					for(int a=0; a<numAlleles; a++) {
						if(forcedA[0][a]) {
							changed |= father.phenotype[locus].setNumbered(a);
						}
					}
				}
				if(changed) {
					father.learnPhenoLocally(locus);
					father.hasChanged(needsUpdate, this);
					done=false;
				}
			}

			//mother
			{
				boolean changed = false;
				changed |= mf.reduceFromSavedArray(saveA[1][0]);
				changed |= mm.reduceFromSavedArray(saveA[1][1]);
				if(numberedLocus) {
					for(int a=0; a<numAlleles; a++) {
						if(forcedA[1][a]) {
							changed |= mother.phenotype[locus].setNumbered(a);
						}
					}
				}
				if(changed) {
					mother.learnPhenoLocally(locus);
					mother.hasChanged(needsUpdate, this);
					done=false;
				}
			}

			//children
			for(int ch=0; ch<child.length; ch++) {
				Person.PossibleAllele cf = child[ch].paternalHaplotype[locus];
				Person.PossibleAllele cm = child[ch].maternalHaplotype[locus];

				boolean changed = false;
				changed |= cf.reduceFromSavedArray(saveA[2][0]);
				changed |= cm.reduceFromSavedArray(saveA[2][1]);
				if(numberedLocus) {
					for(int a=0; a<numAlleles; a++) {
						if(forcedA[2][a]) {
							changed |= child[ch].phenotype[locus].setNumbered(a);
						}
					}
				}
				if(changed) {
					child[ch].learnPhenoLocally(locus);
					child[ch].hasChanged(needsUpdate, this);
					done=false;
				}
			}
		}//while !done
		hasChanged[0]=false;
		hasChanged[1]=false;
		hasChanged[2]=false;
	}//end simplifyUpdate_Full

	void printLocus(int locus) {
		int numA = father.paternalHaplotype[locus].allele.length;

		System.out.println();
		System.out.println("Father:" + father.personID + "\t" + "Mother: " + mother.personID);
		for(int a=0; a<numA; a++) {
			System.out.print(father.paternalHaplotype[locus].allele[a] + "\t");
			System.out.print(father.maternalHaplotype[locus].allele[a] + "\t");
			System.out.print(mother.paternalHaplotype[locus].allele[a] + "\t");
			System.out.println(mother.maternalHaplotype[locus].allele[a] + "\t");
		}
		System.out.println(father.phenotype[locus] + "\t\t" + mother.phenotype[locus]);

		for(int ch=0; ch<child.length; ch++) {
			System.out.print("Child:" + child[ch].personID + "\t");
		}
		System.out.println("");
		for(int a=0; a<numA; a++) {
			for(int ch=0; ch<child.length; ch++) {
				System.out.print(child[ch].paternalHaplotype[locus].allele[a] + "\t");
				System.out.print(child[ch].maternalHaplotype[locus].allele[a] + "\t");
			}
			System.out.println("");
		}
		for(int ch=0; ch<child.length; ch++) {
			System.out.print(child[ch].phenotype[locus] + "\t\t");
		}
		System.out.println("");
	}


}//end class NuclearFamily
