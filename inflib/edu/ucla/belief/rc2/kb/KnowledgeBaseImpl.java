package edu.ucla.belief.rc2.kb;


import edu.ucla.structure.MappedList;
import edu.ucla.belief.FiniteVariable;


import java.util.*;
import java.io.*;

/**
	Implement KnowledgeBase.

	@author David Allen
*/
public class KnowledgeBaseImpl implements KnowledgeBase
{

	static final boolean DEBUG_VERBOSE = false;
	static final boolean DEBUG_VERBOSE2 = false;
	private static final boolean DEBUG_STACK_EXPANSION = false;
	private static final boolean DEBUG_STACK_CHANGES = false;
	private static final boolean DEBUG_KB_ASSERT = false || DEBUG_VERBOSE2;

	/** Must be a positive value.*/
	public int INITIAL_STACK_SIZE = 100;
	/** Must be a positive value.*/
	public int INCREMENTAL_STACK_GROWTH = 100;


	protected MappedList vars; //MappedList of FiniteVariable objects

	//These arrays all have a length in the number of variables
	protected boolean possibleStates[][];
	protected int inClauses[][];
	protected int inClausLit[][];

	//These arrays all have a length in the number of clauses
	protected int clausesIndex[];
	protected int clausesNumLeft[];

	//These arrays all have a length in the number of literals in all the clauses...
	protected int literalsVars[];
	protected int literalsStates[];
	protected boolean literalsPositive[];
	protected int literalsSatState[];


	private static final boolean POSITIVE = true;
	private static final boolean NEGATIVE = false;

	//These are used by literalsSatState
	private static final int LIT_UNSATISFIABLE = -1;
	private static final int LIT_UNKNOWN = 0;
	private static final int LIT_SATISFIED = 1;


	/* Note: A variable may only appear in a clause once, otherwise this code will not work!*/


	protected int stackVar[] = new int[INITIAL_STACK_SIZE];
	protected int stackState[] = new int[INITIAL_STACK_SIZE];
	protected boolean stackPos[] = new boolean[INITIAL_STACK_SIZE];
	protected int stackNext = 0;


	public int currentState() { return stackNext;}
	public int numClauses() { return clausesIndex.length;}
	public int numLiterals() { return literalsVars.length;}

	protected KnowledgeBaseListener listner;


	private KnowledgeBaseImpl() {}

	/** Will return the created KB, or null if it was unsatisfiable.
	 */
	static public KnowledgeBaseImpl createKB(MappedList vars, MultiValuedCNF cnf, KnowledgeBaseListener lstn) {
		if(lstn == null) { throw new IllegalArgumentException("Need valid listener");}
		if(DEBUG_VERBOSE) {System.out.println("\n\ncreate KB\n\n");}
		KnowledgeBaseImpl ret = new KnowledgeBaseImpl();
		if(ret.initialize(vars, cnf, lstn)) {
			return ret;
		}
		else {
			return null;
		}
	}

	/** Return true if successful, or false if KB is unsatisfiable already.
	 */
	private boolean initialize(MappedList vars, MultiValuedCNF cnf, KnowledgeBaseListener lstn) {
		stackNext = 0;
		this.vars = vars;

		this.clausesIndex = cnf.clausesIndex;
		clausesNumLeft = new int[clausesIndex.length];

		this.literalsVars = cnf.literalsVars;
		this.literalsStates = cnf.literalsStates;
		this.literalsPositive = cnf.literalsPositive;
		literalsSatState = new int[literalsPositive.length];

		listner = lstn;
		if( listner == null) { throw new IllegalArgumentException("Need valid listener");}

		//initialize literalsSatState
		Arrays.fill( literalsSatState, LIT_UNKNOWN);


		//create & initialize PossibleStates
		{
			possibleStates = new boolean[vars.size()][];
			for( int i=0; i<possibleStates.length; i++) {
				FiniteVariable fv = (FiniteVariable)vars.get(i);
				if(cnf.contains(fv)) {
					possibleStates[i] = new boolean[fv.size()];
					Arrays.fill( possibleStates[i], true); //initially all are possible
				}
				else {
					possibleStates[i] = null;
				}
			}
		}


		//create inClauses arrays (initialize later)
		{
			inClauses = new int[vars.size()][];
			inClausLit = new int[inClauses.length][];

			int numclau[] = new int[inClauses.length]; //count how many times a variable appears
			Arrays.fill( numclau, 0);
			for( int i=0; i<literalsVars.length; i++) {
				numclau[literalsVars[i]]++;
			}

			for( int i=0; i<inClauses.length; i++) {
				if(possibleStates[i]!=null) {//only deal with variables which appear in the CNF
					inClauses[i] = new int[numclau[i]];
					Arrays.fill( inClauses[i], -1); //initialize with no clause
					inClausLit[i] = new int[numclau[i]];
					Arrays.fill( inClausLit[i], -1); //initialize with no clause
				}
			}
		}

		//initialize clausesNumLeft, inClauses
		for( int cl=0; cl<clausesIndex.length; cl++) {
			//calculate 1st and last literal in this clause
			int beg = clauseFirstLiteral(cl);
			int end = clauseLastLiteral(cl);

			//set clausesNumLeft to size of clause
			clausesNumLeft[cl] = end-beg+1;

			for( int lit=beg; lit<=end; lit++) { //for each literal in clause
				//add cl to inClauses & inClausLit for literalsVars[lit]
				int var = literalsVars[lit];
				for( int j=0; j<inClauses[var].length; j++) {
					if( inClauses[var][j] == -1) {
						inClauses[var][j] = cl;
						inClausLit[var][j] = lit;
						break; //next literal
					}
				}
			}
		}

		//for any clause that only has numLeft=1, learn from it.
		for( int cl=0; cl<clausesIndex.length; cl++) {
			if( clausesNumLeft[cl] == 1) {
				if( onlyOneLiteralLeft( cl) == KB_UNSATISFIABLE) {
					return false;//initial KB is Unsatisfiable,
				}
			}
		}


		return true;
	}


	/* Returns:
	 *   Number of possible states, -1 if it is not in the knowledgebase.
	 */
	public int numPossibleStates(FiniteVariable fv) {
		int indx = vars.indexOf(fv);
		if(indx<0 || possibleStates[indx]==null) { return -1;}

		int poss=0;
		for(int i=0; i<possibleStates[indx].length; i++) {
			if(possibleStates[indx][i]) {poss++;}
		}
		return poss;
	}


	/** Asserts var=state as true as learned from another assert call.
	  * If KB is unsatisfiable it will return KB_UNSATISFIABLE.
	  * @returns Will return stackNext before this assertion takes place.
	  */
	final private int assertLearnedPositive( int var, int state) {
		if( DEBUG_KB_ASSERT) { System.out.println("\nassertLearned " + vars.get(var) + " = " +
											((FiniteVariable)vars.get(var)).instance(state));}
		int ret = assertPositive( var, state);
		if( ret != KB_UNSATISFIABLE) {
			listner.assertLearnedPositive( var, state);
		}
		if( DEBUG_KB_ASSERT) { System.out.println("end assertLearned " + vars.get(var) + " = " +
											((FiniteVariable)vars.get(var)).instance(state));}
		return ret;
	}

	/** Asserts var=state as true.
	  * If KB is unsatisfiable it will return KB_UNSATISFIABLE.
	  * @returns Will return stackNext before this assertion takes place.
	  */
	public int assertPositive( int var, int state) {
		if( DEBUG_KB_ASSERT) { System.out.println("\nassert (" + stackNext + ") " +
								vars.get(var) + " = " +
								((FiniteVariable)vars.get(var)).instance(state));}
		int ret = stackNext;

		//allows single indexing instead of double as a speedup in the code
		boolean possState[] = possibleStates[var];


//		if( possibleStates[var] == null) {  //KB doesn't know anything about this variable
//			return ret;
//		}


		if( !possState[state]) {
			if( DEBUG_KB_ASSERT) { System.out.println("KB_UNSATISFIABLE tried to set " + vars.get(var) + "=" + ((FiniteVariable)vars.get(var)).instance(state));}
			return KB_UNSATISFIABLE;
		}

		boolean changed = false;
		//try to remove all other states
		{
			int st_len = possState.length;
			for( int st=0; st<st_len; st++) {
				if( st != state && possState[st]) { //if st != state & is thought to be valid, make it invalid
					possState[st] = false;
					addToStack( var, st, NEGATIVE);
					changed = true;
				}
			}
		}

		if( changed) { //removed at least one state, try to do unit prop
			if( DEBUG_VERBOSE2) {
				System.out.println("made at least one addition to the stack");
			}
			addToStack( var, state, POSITIVE);
			int[] in_cl = inClauses[var];
			int i_len = in_cl.length;
			for( int i=0; i<i_len; i++) {
				int cl = in_cl[i];

				if( DEBUG_VERBOSE2) {
					System.out.println( clStr(cl) + " numLeft: " + clausesNumLeft[cl]);
				}

				if( clausesNumLeft[cl] > 0) { //ignore if some literal is already sat
					int lit = inClausLit[var][i];
					if( literalsSatState[lit] == LIT_UNKNOWN) {

						if( literalsStates[lit] == state) { //var & state both match

							if( literalsPositive[lit]) {
								clausesNumLeft[cl] = 0; //this clause is sat
								literalsSatState[lit] = LIT_SATISFIED;
								if( DEBUG_VERBOSE2) {
									System.out.println( clStr(cl) + " is sat by " + litStr(lit));
								}
							}
							else { //this variable is unsat in this clause
								literalsSatState[lit] = LIT_UNSATISFIABLE;
								clausesNumLeft[cl]--;
								if( clausesNumLeft[cl] ==0) {
									//undo everything this function has done so far
									literalsSatState[lit] = LIT_UNKNOWN;
									clausesNumLeft[cl]++;
									retract(ret);
									if( DEBUG_KB_ASSERT) { System.out.println("KB_UNSATISFIABLE cannot satisfy " + clStr(cl));}
									return KB_UNSATISFIABLE;
								}
								if( DEBUG_VERBOSE2) {
									System.out.println( clStr(cl) + " is ? but " + litStr(lit) + " is unsat");
								}
							}
						}
						else { //var matches, but state does not
							if( literalsPositive[lit]) {//this variable is unsat in this clause
								literalsSatState[lit] = LIT_UNSATISFIABLE;
								clausesNumLeft[cl]--;
								if( clausesNumLeft[cl] ==0) {
									//undo everything this function has done so far
									literalsSatState[lit] = LIT_UNKNOWN;
									clausesNumLeft[cl]++;
									retract(ret);
									if( DEBUG_KB_ASSERT) { System.out.println("KB_UNSATISFIABLE cannot satisfy " + clStr(cl));}
									return KB_UNSATISFIABLE;
								}
								if( DEBUG_VERBOSE2) {
									System.out.println( clStr(cl) + " is ? but " + litStr(lit) + " is unsat");
								}
							}
							else {
								clausesNumLeft[cl] = 0; //this clause is sat
								literalsSatState[lit] = LIT_SATISFIED;
								if( DEBUG_VERBOSE2) {
									System.out.println( clStr(cl) + " is sat by " + litStr(lit));
								}
							}
						}
					}//end if already unsat
				}//end if have numLeft

			}//end for each clause which contains var
			for( int i=0; i<i_len; i++) {
				int cl = in_cl[i];
				if( clausesNumLeft[cl] == 1) {
					if( onlyOneLiteralLeft( cl) == KB_UNSATISFIABLE) {
						retract(ret); return KB_UNSATISFIABLE;
					}//undo everything this function has done so far
				}
			}
		} //end if changed the valid states of var at all
		if( DEBUG_KB_ASSERT) {
			System.out.println("end assert (" + stackNext + ") " + vars.get(var) + " = " +
									((FiniteVariable)vars.get(var)).instance(state));
		}
		return ret;
	} //end assertPositive


	final private int assertLearnedNegative( int var, int state) {
		if( DEBUG_KB_ASSERT) { System.out.println("\nassertLearned " + vars.get(var) + " != " +
											((FiniteVariable)vars.get(var)).instance(state));}
		int ret = assertNegative( var, state);
//		if( listner != null && ret != KB_UNSATISFIABLE) {
//			listner.assertLearnedNegative( var, state);
//		}
		if( DEBUG_KB_ASSERT) { System.out.println("end assertLearned " + vars.get(var) + " != " +
											((FiniteVariable)vars.get(var)).instance(state));}
		return ret;
	}




	/** Asserts var!=state as true.
	  * If KB is unsatisfiable it will return KB_UNSATISFIABLE.
	  * @returns Will return stackNext before this assertion takes place.
	  */
	public int assertNegative( int var, int state) {
		if( DEBUG_KB_ASSERT) {
			System.out.println("\nassert (" + stackNext + ") " + vars.get(var) + " != " +
									((FiniteVariable)vars.get(var)).instance(state));
		}
		int ret = stackNext;

 		//allows single indexing instead of double as a speedup in the code
		boolean possState[] = possibleStates[var];

		if( !possState[state]) { //already known
			if( DEBUG_KB_ASSERT) {
				System.out.println("\nend assert (" + stackNext + ") " + vars.get(var) + " != " +
											((FiniteVariable)vars.get(var)).instance(state));
			}
			return ret;
		}

		{ //test for only one other state & if found call assertPositive on it
			int numOtherValidStates = 0;
			int otherValidState = -1;
			int st_len = possState.length;
			for( int st=0; st<st_len; st++) {
				if( st != state && possState[st]) {
					numOtherValidStates++;
					otherValidState = st;
					if( numOtherValidStates > 1) break;
				}
			}
			if( numOtherValidStates == 1) {
				if( DEBUG_KB_ASSERT) { System.out.println("\nend assertNeg->assertPos");}
				return assertLearnedPositive( var, otherValidState);
			}
			else if( numOtherValidStates == 0) {
				if( DEBUG_KB_ASSERT) { System.out.println("KB_UNSATISFIABLE tried to set " + vars.get(var) + " != " + ((FiniteVariable)vars.get(var)).instance(state));}
				return KB_UNSATISFIABLE;
			}
		}

		possState[state] = false;
		addToStack( var, state, NEGATIVE);

		int in_cl[] = inClauses[var];

		for( int i=0; i<in_cl.length; i++) {
			int cl = in_cl[i];

			if( DEBUG_VERBOSE2) {
				System.out.println("clause " + cl + " numLeft: " + clausesNumLeft[cl]);
			}

			if( clausesNumLeft[cl] > 0) { //ignore if some literal is already sat
				int lit = inClausLit[var][i];
				if( literalsSatState[lit] == LIT_UNKNOWN) {

					if( literalsStates[lit] == state) { //var & state both match

						if( literalsPositive[lit]) { //this variable is unsat in this clause
							literalsSatState[lit] = LIT_UNSATISFIABLE;
							clausesNumLeft[cl]--;
							if( clausesNumLeft[cl] ==0) {
								literalsSatState[lit] = LIT_UNKNOWN;
								clausesNumLeft[cl]++;
								retract(ret);
								if( DEBUG_KB_ASSERT) { System.out.println("KB_UNSATISFIABLE cannot satisfy " + clStr(cl));}
								return KB_UNSATISFIABLE;//undo everything this function has done so far
							}
							if( DEBUG_VERBOSE2) {
								System.out.println( clStr(cl) + " is ? but " + litStr(lit) + " is unsat");
							}
						}
						else {
							literalsSatState[lit] = LIT_SATISFIED;
							clausesNumLeft[cl] = 0; //this clause is sat
							if( DEBUG_VERBOSE2) {
								System.out.println( clStr(cl) + " is sat by " + litStr(lit));
							}
						}
					}
					else {
						//do nothing if var=state' in clause and
						//  learn var!=state (unless it is last which was done above)
					}
				}//end if already unsat
			}//end if have numLeft
		} //end for each clause var is in

		for( int i=0; i<in_cl.length; i++) {
			int cl = in_cl[i];
			if( clausesNumLeft[cl] == 1) {
				if( onlyOneLiteralLeft( cl) == KB_UNSATISFIABLE) {
					retract( ret); return KB_UNSATISFIABLE;
				}//undo everything this function has done so far
			}
		}

		if( DEBUG_KB_ASSERT) {
			System.out.println("\nend assert (" + stackNext + ") " + vars.get(var) + " != " +
							((FiniteVariable)vars.get(var)).instance(state));
		}
		return ret;
	}


	public void retract( int size) {
		if( DEBUG_KB_ASSERT) { System.out.println("\nretract back to " + size);}
		if( size < 0) { size = 0;}
		if( size > stackNext) {
			//something is amiss (possibly just reset stack to 0 & clearing out other things
//			if( stackNext != 0) {
//				System.err.println("called KB.retract, but " + size + "
//									is greater than stackNext " + stackNext);
//			}
			if( DEBUG_KB_ASSERT) { System.out.println("end retract back to " + size);}
			return;
		}

		for( int s_i=stackNext-1; s_i>=size; s_i--) {

			int var = stackVar[s_i];
			int posState = -1;

			//pop all consecutive items on stack related to same variable
			while( true) {

				int state = stackState[s_i];
				boolean pos = stackPos[s_i];

				if( DEBUG_VERBOSE2) {
					FiniteVariable fv = (FiniteVariable)vars.get( var);
					if( !pos) {System.out.println("pop (" + s_i + ") " + fv + " != " +
														fv.instance(state));}
					else {System.out.println("pop (" + s_i + ") " + fv + " = " +
														fv.instance(state));}
				}

				if( !pos) { //pop A!=a, so make it possible again
					possibleStates[var][state] = true;
//					if( listner != null) {
//						listner.assertUnLearnedNegative( var, state);
//					}
				}
				else {
					posState = state;
//					if( listner != null) {
						listner.assertUnLearnedPositive( var);
//					}
				}

				//continue poping items if more exist and they are the same variable
				if( s_i <= 0 || stackVar[s_i-1]!=var) {
//					System.out.println("update kb & then possibly continue to pop more");
					break;
				}
				else {
					s_i--;
				}
			}


			//do the following once for each variable poped off the stack

			int in_cl[] = inClauses[var];
			for( int j=0; j<in_cl.length; j++) {
				int cl = in_cl[j];

				int lit = inClausLit[var][j];

				if( literalsSatState[lit] == LIT_UNSATISFIABLE) { //possibly not unsat anymore

					if( DEBUG_VERBOSE2) {
						System.out.println( litStr(lit) + " was unsatisfiable, test to see if changed.");
					}

					if(literalsPositive[lit]) {
						//if var=state is possible now, it is not unsat anymore
						if( possibleStates[var][literalsStates[lit]] == true) {
							literalsSatState[lit] = LIT_UNKNOWN;
							clausesNumLeft[cl]++;
							if( DEBUG_VERBOSE2) { System.out.println( litStr(lit) + " was unsat, but is now possible in " + clStr(cl));}
							if( DEBUG_VERBOSE2) { System.out.println("updated numLeft to " + clausesNumLeft[cl]);}
							if( clausesNumLeft[cl] == 1) { throw new IllegalStateException("");}
						}
					}
					else {
						if( posState == literalsStates[lit]) { //stack had neg of all states except one, but that was not valid in this literal
							literalsSatState[lit] = LIT_UNKNOWN;
							clausesNumLeft[cl]++;
							if( DEBUG_VERBOSE2) { System.out.println( litStr(lit) + " was unsat, but is now possible in " + clStr(cl));}
							if( DEBUG_VERBOSE2) { System.out.println("updated numLeft to " + clausesNumLeft[cl]);}
							if( clausesNumLeft[cl] == 1) { throw new IllegalStateException("");}
						}
					}
				}

				else if( literalsSatState[lit] == LIT_SATISFIED) {

					if( DEBUG_VERBOSE2) {System.out.println( litStr(lit) +
												" was satisfied, test to see if changed.");}

					if( literalsPositive[lit]) {
						literalsSatState[lit] = LIT_UNKNOWN;
						if( DEBUG_VERBOSE2) {
							System.out.println( litStr(lit) + " is not satisfied.");
						}
					}
					//was satisfied because this was not possible
					else if( possibleStates[var][literalsStates[lit]] == true) {
						literalsSatState[lit] = LIT_UNKNOWN;
						if( DEBUG_VERBOSE2) {
							System.out.println( litStr(lit) + " is not satisfied.");
						}
					}
					else {
						if( DEBUG_VERBOSE2) {
							System.out.println( litStr(lit) + " is still satisfied.");
						}
					}


					if( literalsSatState[lit] == LIT_UNKNOWN) {

						int numle = 0;
						int beg = clauseFirstLiteral(cl);
						int end = clauseLastLiteral(cl);
						if( DEBUG_VERBOSE2) {System.out.println( clStr(cl));}
						for( int l=beg; l<=end; l++) {


							if( literalsSatState[l] == LIT_UNSATISFIABLE) {
								if( DEBUG_VERBOSE2) {
									System.out.println( litStr(l) + " is unsatisfiable");
								}
							}
							else if( literalsSatState[l] == LIT_UNKNOWN) {
								if( DEBUG_VERBOSE2) {
									System.out.println( litStr(l) + " is satisfiable");
								}
								numle++;
							}
							else { //something else still satisfied, but this literal said
								   //     it was (cannot have two satisfied literals)
								if( DEBUG_VERBOSE2) {
									System.out.println( litStr(l) + " is satisfied");
								}
								throw new IllegalStateException("");
							}
						}

						clausesNumLeft[cl] = numle;
						if( DEBUG_VERBOSE2) {
							System.out.println( clStr(cl) + " has numLeft = " + numle);
						}
//						if( numle > 0) {
//							listnerUnLearnedPositive = true;
//							if( DEBUG_VERBOSE2) {
//								System.out.println("assertUnLearnedPositive triggered by " + litStr(lit));
//							}
//						}
					}
				}

			} //end for all clauses var is in
		}//end for each stack value
		stackNext = size;
		if( DEBUG_VERBOSE2) { System.out.println("stackNext = " + stackNext);}
		if( DEBUG_KB_ASSERT) { System.out.println("end retract back to " + size);}


	}




	final private void addToStack( int var, int val, boolean pos) {
		if( DEBUG_STACK_CHANGES) {
			System.out.println("addToStack(" + var + "," + val + "," + pos + ")");
		}


		//grow stack if necessary
		if( stackNext >= stackVar.length) {
			if( DEBUG_STACK_EXPANSION) {
				System.out.println("Expand stack from " + stackVar.length + " to " +
									(stackVar.length+INCREMENTAL_STACK_GROWTH));
			}
			int oldvar[] = stackVar;
			int oldstate[] = stackState;
			boolean oldpos[] = stackPos;
			stackVar = new int[ oldvar.length + INCREMENTAL_STACK_GROWTH];
			stackState = new int[ oldstate.length + INCREMENTAL_STACK_GROWTH];
			stackPos = new boolean[ oldpos.length + INCREMENTAL_STACK_GROWTH];
			System.arraycopy( oldvar, 0, stackVar, 0, oldvar.length);
			System.arraycopy( oldstate, 0, stackState, 0, oldstate.length);
			System.arraycopy( oldpos, 0, stackPos, 0, oldpos.length);
		}


		//add var-val to stack
		stackVar[stackNext] = var;
		stackState[stackNext] = val;
		stackPos[stackNext] = pos;
		stackNext++;
	}


	final private int clauseFirstLiteral( int clause) {
		return clausesIndex[clause];
	}
	final private int clauseLastLiteral( int clause) {
		if( clause != clausesIndex.length-1) {
			return clausesIndex[clause+1]-1;} //one less than start of next clause
		else {
			return literalsVars.length-1;} //end of array
	}
//	protected int clauseFindLit( int clause, int var) {
//		int beg = clauseFirstLiteral( clause);
//		int end = clauseLastLiteral( clause);
//		for( int i=beg; i<=end; i++) {
//			if( literalsVars[i] == var) { return i;}
//		}
//		return -1; //not found
//	}

	protected int clauseFindLitNeedSat( int clause) {
		int beg = clauseFirstLiteral( clause);
		int end = clauseLastLiteral( clause);
		for( int i=beg; i<=end; i++) {
			if( literalsSatState[i] != LIT_UNSATISFIABLE) { return i;}
		}
		return -1; //not found
	}

//	protected boolean isSatisfied( int var, int state, boolean positive) {
//		if( positive) { //clause has var=state
//			//return true if no other state is possible & this one is, else return false
//			for( int st=0; st<possibleStates[var].length; st++) {
//				if( st != state && possibleStates[var][st] == true) {
//					return false;
//				}
//				else if( st == state && possibleStates[var][st] == false) {
//					return false;
//				}
//			}
//			return true;
//		}
//		else { //clause has var!=state
//			if( possibleStates[var][state] == false) { return true;} //it is satisfied
//			else { return false;} //it is not sat, as var=state is still possible
//		}
//	}



	final private int onlyOneLiteralLeft( int cl) {
		int ret;

		//find lit which needs to be sat
		int satlit = clauseFindLitNeedSat(cl);

		int tmp = clausesNumLeft[cl];
		int tmp2 = literalsSatState[satlit];

		clausesNumLeft[cl] = 0;
		literalsSatState[satlit] = LIT_SATISFIED;

		if( literalsPositive[satlit]) {
			ret = assertLearnedPositive( literalsVars[satlit], literalsStates[satlit]);
		}
		else {
			ret = assertLearnedNegative( literalsVars[satlit], literalsStates[satlit]);
		}

		if( ret == KB_UNSATISFIABLE) {
			clausesNumLeft[cl] = tmp;
			literalsSatState[satlit] = tmp2;
		} //undo everything this function has done so far
		return ret;
	}



	public void write( PrintStream out) {
		out.println("KB:");
		for( int cl=0; cl<clausesIndex.length; cl++) {
			int beg = clauseFirstLiteral(cl);
			int end = clauseLastLiteral(cl);

			for( int l=beg; l<=end; l++) {
				FiniteVariable fv = (FiniteVariable)vars.get( literalsVars[l]);
				if( literalsPositive[l]) {
					out.print( fv + "=" + fv.instance(literalsStates[l]) + " ");
				}
				else {
					out.print( fv + "!=" + fv.instance(literalsStates[l]) + " ");
				}
			}
			out.println("");
		}
	}



	private String litStr( int lit) {
		StringBuffer ret = new StringBuffer();
		FiniteVariable fv = (FiniteVariable)vars.get(literalsVars[lit]);
		ret.append("[lit " + lit + "(" + fv);
		if( literalsPositive[lit]) { ret.append("=");}
		else { ret.append("!=");}
		ret.append( fv.instance(literalsStates[lit]) + ")]");
		return ret.toString();
	}
	private String clStr( int cl) {
		StringBuffer ret = new StringBuffer();
		int beg = clauseFirstLiteral(cl);
		int end = clauseLastLiteral(cl);
		ret.append("clause " + cl + ": ");
		for( int l=beg; l<=end; l++) {
			ret.append( litStr(l));
		}
		return ret.toString();
	}





} //end class KnowledgeBaseImpl
