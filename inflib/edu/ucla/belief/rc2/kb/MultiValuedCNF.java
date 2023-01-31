package edu.ucla.belief.rc2.kb;


import edu.ucla.structure.MappedList;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.FiniteVariableImpl;
import edu.ucla.belief.CPTShell;
import edu.ucla.belief.Table;
import edu.ucla.belief.TableIndex;
import edu.ucla.belief.io.geneticlinkage.Pedigree;


import java.util.*;
import java.io.*;

/**
	Class representing Multi-Valued CNFs.

	@author David Allen
*/
final public class MultiValuedCNF {

	static final boolean DEBUG_VERBOSE = false;
	static final boolean DEBUG_VERBOSE2 = false;
	static final boolean DEBUG_ARRAY_GROWTH = false;
	static final int LITERALS_LENGTH_INITIAL = 5000;
	static final int CLAUSES_LENGTH_INITIAL = 500;

	private boolean unsatisfiable = false; //if true, arrays will be cleared
	private boolean augmentedForCompilation = false; //if true, has forced all probabilities to the roots

	public int clausesIndex[];
	public int literalsVars[];
	public int literalsStates[];
	public boolean literalsPositive[];

	private int nextClause;
	private int nextLiteral;

	private HashSet varsInLiterals;
	private List allVarsIndexing; //NEVER MODIFY, this is from the RC object.  (Actually, augmenting for compilation does change this, be WARNED).

	private List eclauses = null; //only used if augmentedForCompilation
	private List eclausesFV = null; //only used if augmentedForCompilation.  It corresponds to eclauses, and represents the Var which caused this eclause to appear

	private HashMap fvToColOfClauses; //will be filled by a map of finite variable objects to a collection of Integer values, which
										// represent the clause numbers that came from that table.
										// this does not include any clauses added to eclauses


	public boolean augmentedForCompilation() { return augmentedForCompilation;}
	public boolean usingEClauses() { return eclauses!=null;}
	public List eclauses() {return Collections.unmodifiableList(eclauses);}
	public List eclausesFV() {return Collections.unmodifiableList(eclausesFV);}
	public Map fvToColOfClauses() {return Collections.unmodifiableMap(fvToColOfClauses);}


	public boolean unsatisfiable() {return unsatisfiable;}
	public boolean contains(Object var) {return varsInLiterals.contains(var);}

	/**This returns a list of all vars in the BN, not only those in the CNF.*/
	public List indexingList() {return allVarsIndexing;}

	private MultiValuedCNF(){
		clausesIndex = new int[CLAUSES_LENGTH_INITIAL];
		literalsVars = new int[LITERALS_LENGTH_INITIAL];
		literalsStates = new int[LITERALS_LENGTH_INITIAL];
		literalsPositive = new boolean[LITERALS_LENGTH_INITIAL];

		fvToColOfClauses = new HashMap();

		nextClause = 0;
		nextLiteral = 0;
	}



	/** After calling this, the user should test unsatisfiable, before attempting to
	 *  access the clause and literals arrays.  This function assumes that the CPT tables
	 *  have probabilities in the range of [0..1] (i.e. they need to be normalized prior to
	 *  calling this).  The parameter vars should be the mapped list from the RC object,
	 *  and will not be modified by this class (it however will be stored for later indexing
	 *  purposes.)
	 */
	static public MultiValuedCNF createFromBN(BeliefNetwork bn, MappedList vars) {

		if(DEBUG_VERBOSE) {System.out.println("\n\nCreate a KB from a BN");}

		MultiValuedCNF ret = new MultiValuedCNF();

		try {

//			ret.allVarsIndexing = Collections.unmodifiableList(vars);
			ret.allVarsIndexing = vars;

			//for each table start adding variables, clauses, & literals
			for(Iterator itr = bn.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				CPTShell cs = fv.getCPTShell();
				ret.handleTable(vars, cs.getCPT());
			}

			//make arrays exact size
			ret.makeExactLength();

			ret.varsInLiterals = new HashSet(vars.size());
			for(int i=0; i<ret.literalsVars.length; i++) {
				ret.varsInLiterals.add(vars.get(ret.literalsVars[i]));
			}


		}
		catch(ExceptionUnSAT e) {
			ret.makeUnsatisfiable();
		}

		return ret;
	}



	/** Will return an array indexed by the vars list (from createFromBN),
	 *  with true if it appears in any CNF clause and false if it doesn't
	 *  appear anywhere.
	 */
	public boolean[] theseVarsAppearInCNF() {
		boolean ret[] = new boolean[allVarsIndexing.size()];

		Arrays.fill(ret, false);
		for(int i=0; i<literalsVars.length; i++) {
			ret[literalsVars[i]] = true;
		}

		return ret;
	}

	public int numVarsAppearingInCNF() {
		return varsInLiterals.size();
	}
	public int numClauses() { return clausesIndex.length;}
	public int numLiterals() { return literalsVars.length;}



	private void handleTable(MappedList vars, Table tbl)
	throws ExceptionUnSAT {

		FiniteVariable parents[] = tbl.index().getParents();
		FiniteVariable localV = tbl.index().variable(parents.length);
		int inst[] = new int[ parents.length + 1]; //include parents & child (possibly no parents)

		HashSet colOfClauses = (HashSet)fvToColOfClauses.get(localV);
		if(colOfClauses==null) {
			colOfClauses = new HashSet();
			fvToColOfClauses.put(localV, colOfClauses);
		}

		if(DEBUG_VERBOSE) {System.out.println("Handle table for " + localV + ", " + Arrays.asList(parents));}

		for(int i=0; i<parents.length; i++) {inst[i] = parents[i].size()-1;} //all parents to maxState
		int parIndx = parents.length-1; //set it to the right

		while(true) { //for all possible states of parents (always at least one)

			boolean foundOne = false;

			for(int i=0; i<localV.size(); i++) { //for each state of variable look for P(i)=1.0
				inst[inst.length-1] = i;
				if(tbl.getCP(tbl.index().index(inst)) == 1.0) {
					foundOne = true;
					break;
				}
			}

			if(foundOne) { //have a 1.0 at inst[]
				int oldc = nextClause;
				int oldl = nextLiteral;

				addClause();
				if(DEBUG_VERBOSE2) {System.out.print("Add Clause " +nextClause+ " (1.0): ");}
				for(int l=0; l<inst.length-1; l++) {
					if(parents[l].size() != 1) { //if parent only has one state, the negation of that is unsatisfiable (so don't add)
						addLiteral(vars.indexOf(parents[l]), inst[l], false); //add negative of each parent
						if(DEBUG_VERBOSE2) {System.out.print(parents[l] + "!=" + parents[l].instance(inst[l]) + " ");}
					}
				}
				if(localV.size() != 1) {
					addLiteral(vars.indexOf(localV), inst[inst.length-1], true); //add positive of localV
					if(DEBUG_VERBOSE2) {System.out.println(localV + "=" + localV.instance(inst[inst.length-1]));}
					colOfClauses.add(new Integer(oldc));
				}
				else { //if localV only has one state, then this clause is already satisfied, remove it
					nextClause = oldc;
					nextLiteral = oldl;
					if(DEBUG_VERBOSE2) {System.out.println("\tRetract clause, back to : " + nextClause + ", " + nextLiteral);}
				}
			}
			else { //no 1.0, so add clauses for any 0s

				for(int i=0; i<localV.size(); i++) { //for each state of variable if P=0, add clause
					inst[inst.length-1] = i;
					if(tbl.getCP(tbl.index().index(inst)) == 0.0) {

						int oldc = nextClause;
						int oldl = nextLiteral;
						int numLitAdded = 0;

						addClause();
						colOfClauses.add(new Integer(oldc));
						if(DEBUG_VERBOSE2) {System.out.print("Add Clause "+nextClause+" (0.0): ");}
						for(int l=0; l<inst.length-1; l++) {
							if(parents[l].size() != 1) {//if parent only has one state, the negation of that is unsatisfiable (so don't add)
								addLiteral(vars.indexOf(parents[l]), inst[l], false); //add negative of each parent
								if(DEBUG_VERBOSE2) {System.out.print(parents[l] + "!=" + parents[l].instance(inst[l]) + " ");}
								numLitAdded++;
							}
						}
						if(localV.size() != 1) { //if localV only has one state, the negation of that is unsatisfiable (so don't add)
							addLiteral(vars.indexOf(localV), inst[inst.length-1], false); //add negative of localV
							if(DEBUG_VERBOSE2) {System.out.println(localV + "!=" + localV.instance(inst[inst.length-1]));}
							numLitAdded++;
						}
						else {
							if(DEBUG_VERBOSE2) {System.out.println("");}
						}
						if(numLitAdded == 0) { //its possible all parents & child only have one state & it is 0 (KB is inconsistent)
							throw new ExceptionUnSAT("Initial KB is inconsistent!");
						}
					}
				}
			}


			//find new state of parents, if none, stop
			while(parIndx > -1 && inst[parIndx] == 0) {//if last state on parIndx, set it to Max and move left one (or more)
				inst[parIndx] = parents[parIndx].size()-1;
				parIndx--;
			}

			if(parIndx < 0) { break;} //no more available so stop

			inst[parIndx]--; //reduce this one
			parIndx = parents.length-1; //set it back to the right

		} //end while(true) for each parent state
	} //end handleTable


	private void addClause() {
		if(nextClause >= clausesIndex.length) {
			int old[] = clausesIndex;
			clausesIndex = new int[old.length + CLAUSES_LENGTH_INITIAL];
			System.arraycopy(old, 0, clausesIndex, 0, old.length);
			if(DEBUG_ARRAY_GROWTH) {System.out.println("clausesIndex grew to " + clausesIndex.length);}
		}
		clausesIndex[nextClause] = nextLiteral;
		nextClause++;
	}

	private void addLiteral(int var, int state, boolean pos) {
		if(nextLiteral >= literalsVars.length) {
			int old[] = literalsVars;
			literalsVars = new int[old.length + LITERALS_LENGTH_INITIAL];
			System.arraycopy(old, 0, literalsVars, 0, old.length);

			old = literalsStates;
			literalsStates = new int[old.length + LITERALS_LENGTH_INITIAL];
			System.arraycopy(old, 0, literalsStates, 0, old.length);

			boolean oldb[] = literalsPositive;
			literalsPositive = new boolean[oldb.length + LITERALS_LENGTH_INITIAL];
			System.arraycopy(oldb, 0, literalsPositive, 0, oldb.length);

			if(DEBUG_ARRAY_GROWTH) {System.out.println("literals grew to " + literalsVars.length);}
		}
		literalsVars[nextLiteral] = var;
		literalsStates[nextLiteral] = state;
		literalsPositive[nextLiteral] = pos;
		nextLiteral++;
	}



	private class ExceptionUnSAT extends Exception {
		public ExceptionUnSAT(String reason) {super(reason);}
	}


	private void makeUnsatisfiable() {
		unsatisfiable = true;
		clausesIndex = null;
		literalsVars = null;
		literalsStates = null;
		literalsPositive = null;
		eclauses = null;
		eclausesFV = null;
		nextClause = -1;
		nextLiteral = -1;
	}


	private void makeExactLength() {
		//make arrays exact size
		{
			int old[];
			boolean oldb[];

			old = clausesIndex;
			clausesIndex = new int[nextClause];
			System.arraycopy(old, 0, clausesIndex, 0, nextClause);

			old = literalsVars;
			literalsVars = new int[nextLiteral];
			System.arraycopy(old, 0, literalsVars, 0, nextLiteral);

			old = literalsStates;
			literalsStates = new int[nextLiteral];
			System.arraycopy(old, 0, literalsStates, 0, nextLiteral);

			oldb = literalsPositive;
			literalsPositive = new boolean[nextLiteral];
			System.arraycopy(oldb, 0, literalsPositive, 0, nextLiteral);
		}
	}

	public void displayCNF(PrintStream out) {
		int nextCl = 1;
		for(int lit=0; lit<literalsVars.length; lit++) {
			FiniteVariable fv = (FiniteVariable)allVarsIndexing.get(literalsVars[lit]);
			out.print("(" + fv);
			out.print(literalsPositive[lit]?"=>":"!=>");
			out.print(fv.instance(literalsStates[lit]) + ")");

			if(nextCl<clausesIndex.length && clausesIndex[nextCl]==lit+1) {
				out.println("");
				nextCl++;
			}
		}
		out.println("");
	}



	/**WARNING: This will add variables and adjust sizes, so it will no longer be "valid" with the BeliefNetwork passed in.
	 *
	 * <p>This function is specific to the current method of encoding genetic networks!
	 */
	static public void AugmentForCompilation(MultiValuedCNF mvcnf, BeliefNetwork bn, boolean useEClause) {

		if(mvcnf.augmentedForCompilation) throw new IllegalStateException("Already augmented for compilation.");
		mvcnf.augmentedForCompilation = true;

		if(useEClause) {
			mvcnf.eclauses = new ArrayList();
			mvcnf.eclausesFV = new ArrayList();
		}

		for(Iterator var_itr=bn.iterator(); var_itr.hasNext();) {

			FiniteVariable fv = (FiniteVariable)var_itr.next();
			String id = fv.getID();

			char c = id.charAt(0);
			if(c=='P') { //found a phenotype, see if it is probabilistic
				boolean deterministic = true;

				CPTShell shell = fv.getCPTShell();
				TableIndex indx = shell.index();
				for(int i=0; i<indx.size(); i++) {
					double pr = shell.getCP(i);
					if(!(pr==0.0 || pr==1.0)) {deterministic = false; break;}
				}

				if(!deterministic) { //find probabilistic phenotypes (ignore deterministic ones)
					if(Pedigree.PREPROCESS!=Pedigree.ALL) {
						System.err.println("Pedigree is NOT using PREPROCESS_RULES, which this requires.");
						throw new IllegalStateException("Pedigree is NOT using PREPROCESS_RULES, which this requires.");
//						continue;
					}
					if(fv.size()!=1) { throw new IllegalStateException("phenotype of size " + fv.size());} //phenotype if not known should be removed, otherwise reduced to size 1

					HashSet colOfClauses = (HashSet)mvcnf.fvToColOfClauses.get(fv);
					if(colOfClauses==null) {
						colOfClauses = new HashSet();
						mvcnf.fvToColOfClauses.put(fv, colOfClauses);
					}

					String values[] = {"valid", "invalid"};

					FiniteVariable parents[] = indx.getParents();
					int parIndx[] = new int[parents.length];

					{
						for(int i=0; i<parents.length; i++) {
							parIndx[i] = mvcnf.allVarsIndexing.indexOf(parents[i]);
						}
					}


					if(parIndx.length==0) { //no parents
						throw new IllegalStateException("Independent variable not removed: " + id);
					}
					else if(parIndx.length==1 && bn.inDegree(parents[0])==0) {
						//only one parent & it is root, so just "merge probabilities in" TODO
					}
					else {

						int next=0;
						int eclauseVars[] = null;
						if(useEClause) {
							eclauseVars = new int[indx.size()];
							mvcnf.eclauses.add(eclauseVars);
							mvcnf.eclausesFV.add(fv);
						}


						//create a new variable for each possible state (possibly up to 4 normally, 16 for two disease locus)
						for(TableIndex.Iterator val_itr=indx.iterator(); val_itr.hasNext();) {

							int tblindx = val_itr.next();

							{
								StringBuffer val = new StringBuffer();
								for(int i=0; i<parIndx.length+1; i++) {
									val.append(String.valueOf(val_itr.current()[i]));
								}
								values[0] = val.toString();
							}

							FiniteVariable newP = new FiniteVariableImpl(id+"_val_"+tblindx, values);
							mvcnf.allVarsIndexing.add(newP);
							mvcnf.varsInLiterals.add(newP);
							int varIndxP = mvcnf.allVarsIndexing.size()-1;
							mvcnf.fvToColOfClauses.put(newP, colOfClauses);

							//create clauses for it
							{
								//par1=val and par2=val => newP=val  (this is converted to the following)
								//!par1=val or !par2=val or newP=val
								colOfClauses.add(new Integer(mvcnf.nextClause));
								mvcnf.addClause();
								for(int i=0; i<parents.length; i++) {
									mvcnf.addLiteral(parIndx[i], val_itr.current()[i], false); //pari!=val
									mvcnf.varsInLiterals.add(parents[i]);
								}
								mvcnf.addLiteral(varIndxP,val_itr.current()[parents.length],true);//newP=val

								if(!useEClause) {
									//newP=val => par1=val  (newP!=val or par1=val)
									//newP=val => par2=val
									for(int i=0; i<parents.length; i++) {
										colOfClauses.add(new Integer(mvcnf.nextClause));
										mvcnf.addClause();
										mvcnf.addLiteral(varIndxP, val_itr.current()[parents.length], false);
										mvcnf.addLiteral(parIndx[i], val_itr.current()[i], true);
									}
								}
								else { //using eclause
									eclauseVars[next] = varIndxP;
									next++;
								}
							}
						}//for each value
					}
				}//if probabilistic
			}//end phenotype variable
			else if(c=='S') {//found a selector variable

				//look for non-roots (roots are either the first loci, or if a parent is known)
				int numParents = bn.inDegree(fv);

				if(numParents>=1) { //S*One may have >1 parent
					Table tbl = fv.getCPTShell().getCPT();
					List vars = tbl.index().variables();

					HashSet colOfClauses = (HashSet)mvcnf.fvToColOfClauses.get(fv);
					if(colOfClauses==null) {
						colOfClauses = new HashSet();
						mvcnf.fvToColOfClauses.put(fv, colOfClauses);
					}

					int varIndx[] = new int[vars.size()];
					for(int i=0; i<varIndx.length; i++) {
						FiniteVariable tblVar = (FiniteVariable)vars.get(i);
						if(tblVar.size()>1) {
							varIndx[i] = mvcnf.allVarsIndexing.indexOf(tblVar);
							mvcnf.varsInLiterals.add(tblVar);
						}
						else { //could only happen for the child variable (it could be known)
							varIndx[i] = Integer.MIN_VALUE;
						}
					}

					//new inverter variable
					MappedList values = new MappedList();
					{
						for(int i=0; i<tbl.index().size(); i++) {
							values.add(new Double(tbl.getCP(i)));
						}
					}
					FiniteVariable inverterSel = new FiniteVariableImpl(id+"_inverter", values);
					mvcnf.allVarsIndexing.add(inverterSel);
					mvcnf.varsInLiterals.add(inverterSel);
					int varIndxInv = mvcnf.allVarsIndexing.size()-1;
					mvcnf.fvToColOfClauses.put(inverterSel, colOfClauses);

					{
						for(TableIndex.Iterator itr=tbl.index().iterator(); itr.hasNext();) {
							int st = itr.next();
							int curr[] = itr.current();

							colOfClauses.add(new Integer(mvcnf.nextClause));
							mvcnf.addClause();
							for(int i=0; i<varIndx.length; i++) {
								if(varIndx[i] != Integer.MIN_VALUE) {
									mvcnf.addLiteral(varIndx[i],curr[i],false);
								}
							}
							mvcnf.addLiteral(varIndxInv,values.indexOf(new Double(tbl.getCP(st))),true);
						}
					}
				}
			}
			else if(c=='G' || c=='m') { //genotype or merged_genotype
				//ignore
			}
			else {
				throw new IllegalStateException("unknown variable name: " + id);
			}
		}
		mvcnf.makeExactLength();
	}
} //end class MultiValuedCNF
