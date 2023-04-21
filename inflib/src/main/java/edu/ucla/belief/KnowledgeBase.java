package edu.ucla.belief;


import edu.ucla.structure.*;

import java.util.*;
import java.io.*;

/**
	Define KnowledgeBase.

	@author David Allen
	@since 033603
*/
public class KnowledgeBase
{
	static final PrintStream STREAM_VERBOSE = Definitions.STREAM_VERBOSE;
	static final boolean DEBUG_VERBOSE = Definitions.DEBUG;
	static final boolean DEBUG_VERBOSE2 = false;
	private static final boolean DEBUG_STACK_EXPANSION = false;
	private static final boolean DEBUG_STACK_CHANGES = false;
	private static final boolean DEBUG_KB_ASSERT = false || DEBUG_VERBOSE2;

//	private static final boolean USE_numUnSubsumedClauses = false;

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
//	protected int numUnSubsumedClauses[];

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


	//The stack always contains negative var,state pairs
	protected int stackVar[] = new int[INITIAL_STACK_SIZE];
	protected int stackState[] = new int[INITIAL_STACK_SIZE];
	protected boolean stackPos[] = new boolean[INITIAL_STACK_SIZE];
	protected int stackNext = 0;


	public int currentState() { return stackNext;}
	public int numClauses() { return clausesIndex.length;}
	public int numLiterals() { return literalsVars.length;}

	protected KnowledgeBaseListener listner;


	static public KnowledgeBase createFromBN( BeliefNetwork bn, MappedList vars,
												KnowledgeBaseListener lstn)
	throws KBUnsatisfiableStateException {
		return new KnowledgeBaseGeneratorFromBN().createFromBN( bn, vars, lstn);
	}

	public KnowledgeBase( MappedList vars, int clausesIndex[], int literalsVars[],
							int literalsStates[], boolean literalsPositive[],
							KnowledgeBaseListener lstn)
	throws KBUnsatisfiableStateException {
		if( DEBUG_VERBOSE) {STREAM_VERBOSE.println("\n\ncreate KB\n\n");}
		initialize( vars, clausesIndex, literalsVars, literalsStates, literalsPositive, lstn);
	}

	protected void initialize( MappedList vars, int clausesIndex[], int literalsVars[],
								int literalsStates[], boolean literalsPositive[],
								KnowledgeBaseListener lstn)
	throws KBUnsatisfiableStateException {
		if( DEBUG_VERBOSE) { STREAM_VERBOSE.println("called kb.initialize");}
		stackNext = 0;
		this.vars = (MappedList)vars.clone();

		this.clausesIndex = (int[])clausesIndex.clone();
		clausesNumLeft = new int[clausesIndex.length];

		this.literalsVars = (int[])literalsVars.clone();
		this.literalsStates = (int[])literalsStates.clone();
		this.literalsPositive = (boolean[])literalsPositive.clone();
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
				possibleStates[i] = new boolean[fv.size()];
				Arrays.fill( possibleStates[i], true); //initially all are possible
			}
		}


		//create inClauses arrays (initialize later)
		{
			inClauses = new int[vars.size()][];
			inClausLit = new int[inClauses.length][];

//			if( USE_numUnSubsumedClauses) {
//				numUnSubsumedClauses = new int[vars.size()];
//				Arrays.fill( numUnSubsumedClauses, 0);
//			}

			int numclau[] = new int[inClauses.length]; //count how many times a variable appears
			Arrays.fill( numclau, 0);
			for( int i=0; i<literalsVars.length; i++) {
				numclau[literalsVars[i]]++;
			}

			for( int i=0; i<inClauses.length; i++) {
				inClauses[i] = new int[numclau[i]];
				Arrays.fill( inClauses[i], -1); //initialize with no clause
				inClausLit[i] = new int[numclau[i]];
				Arrays.fill( inClausLit[i], -1); //initialize with no clause
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
//				if( USE_numUnSubsumedClauses) { numUnSubsumedClauses[var]++;}
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
					throw new KBUnsatisfiableStateException( clStr(cl));
				}
			}
		}


		{ //don't let KB deal with any variable it doesn't have in a clause
			HashSet varsToRemove = new HashSet( vars);

			for( int i=0; i<literalsVars.length; i++) {
				Object fv = vars.get( literalsVars[i]);
				varsToRemove.remove( fv);
			}
			if( DEBUG_VERBOSE) { STREAM_VERBOSE.println("Removing " + varsToRemove.size() + " vars from KB info.");}
			for( Iterator itr = varsToRemove.iterator(); itr.hasNext();) {
				Object fv = itr.next();
				int indx = vars.indexOf( fv);
				listner.kbDontCallAssertOnVar(indx);
				inClauses[indx] = null;
				inClausLit[indx] = null;
				possibleStates[indx] = null;
			}
		}
	}


	/** Asserts var=state as true as learned from another assert call.
	  * If KB is unsatisfiable it will return KB_UNSATISFIABLE.
	  * @returns Will return stackNext before this assertion takes place.
	  */
	final private int assertLearnedPositive( int var, int state) {
		if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("\nassertLearned " + vars.get(var) + " = " +
											((FiniteVariable)vars.get(var)).instance(state));}
		int ret = assertPositive( var, state);
		if( ret != KB_UNSATISFIABLE) {
			listner.assertLearnedPositive( var, state);
		}
		if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("end assertLearned " + vars.get(var) + " = " +
											((FiniteVariable)vars.get(var)).instance(state));}
		return ret;
	}

	/** Asserts var=state as true.
	  * If KB is unsatisfiable it will return KB_UNSATISFIABLE.
	  * @returns Will return stackNext before this assertion takes place.
	  */
	public int assertPositive( int var, int state) {
		if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("\nassert (" + stackNext + ") " +
								vars.get(var) + " = " +
								((FiniteVariable)vars.get(var)).instance(state));}
		int ret = stackNext;

		//allows single indexing instead of double as a speedup in the code
		boolean possState[] = possibleStates[var];


//		if( possibleStates[var] == null) {  //KB doesn't know anything about this variable
//			return ret;
//		}


		if( !possState[state]) {
			if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("KB_UNSATISFIABLE tried to set " + vars.get(var) + "=" + ((FiniteVariable)vars.get(var)).instance(state));}
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
			if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println("made at least one addition to the stack"); }
			addToStack( var, state, POSITIVE);
			int i_len = inClauses[var].length;
			int[] in_cl = inClauses[var];
			for( int i=0; i<i_len; i++) {
				int cl = in_cl[i];
//				if( cl < 0) { continue;} //not a valid clause (has wasted space if this is true);

				if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( clStr(cl) + " numLeft: " + clausesNumLeft[cl]); }

				if( clausesNumLeft[cl] > 0) { //ignore if some literal is already sat
					int lit = inClausLit[var][i];
					if( literalsSatState[lit] == LIT_UNKNOWN) {

						if( literalsStates[lit] == state) { //var & state both match

							if( literalsPositive[lit]) {
								clausesNumLeft[cl] = 0; //this clause is sat
								literalsSatState[lit] = LIT_SATISFIED;
								if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( clStr(cl) + " is sat by " + litStr(lit)); }

//								if( USE_numUnSubsumedClauses) {
//									int beg = clauseFirstLiteral(cl);
//									int end = clauseLastLiteral(cl);
//									for( int ul=beg; ul<=end; ul++) {
//										numUnSubsumedClauses[literalsVars[ul]]--;
//										if( numUnSubsumedClauses[literalsVars[ul]] == 0) {
//											listner.kbDontCallAssertOnVar(literalsVars[ul]);
//										}
//									}
//								}
							}
							else { //this variable is unsat in this clause
								literalsSatState[lit] = LIT_UNSATISFIABLE;
								clausesNumLeft[cl]--;
								if( clausesNumLeft[cl] ==0) {
									//undo everything this function has done so far
									literalsSatState[lit] = LIT_UNKNOWN;
									clausesNumLeft[cl]++;
									retract(ret);
									if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("KB_UNSATISFIABLE cannot satisfy " + clStr(cl));}
									return KB_UNSATISFIABLE;
								}
								if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( clStr(cl) + " is ? but " + litStr(lit) + " is unsat"); }
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
									if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("KB_UNSATISFIABLE cannot satisfy " + clStr(cl));}
									return KB_UNSATISFIABLE;
								}
								if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( clStr(cl) + " is ? but " + litStr(lit) + " is unsat"); }
							}
							else {
								clausesNumLeft[cl] = 0; //this clause is sat
								literalsSatState[lit] = LIT_SATISFIED;
								if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( clStr(cl) + " is sat by " + litStr(lit)); }

//								if( USE_numUnSubsumedClauses) {
//									int beg = clauseFirstLiteral(cl);
//									int end = clauseLastLiteral(cl);
//									for( int ul=beg; ul<=end; ul++) {
//										numUnSubsumedClauses[literalsVars[ul]]--;
//										if( numUnSubsumedClauses[literalsVars[ul]] == 0) {
//											listner.kbDontCallAssertOnVar(literalsVars[ul]);
//										}
//									}
//								}
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
		if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("end assert (" + stackNext + ") " + vars.get(var) + " = " + ((FiniteVariable)vars.get(var)).instance(state)); }
		return ret;
	} //end assertPositive


	final private int assertLearnedNegative( int var, int state) {
		if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("\nassertLearned " + vars.get(var) + " != " +
											((FiniteVariable)vars.get(var)).instance(state));}
		int ret = assertNegative( var, state);
//		if( listner != null && ret != KB_UNSATISFIABLE) {
//			listner.assertLearnedNegative( var, state);
//		}
		if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("end assertLearned " + vars.get(var) + " != " +
											((FiniteVariable)vars.get(var)).instance(state));}
		return ret;
	}




	/** Asserts var!=state as true.
	  * If KB is unsatisfiable it will return KB_UNSATISFIABLE.
	  * @returns Will return stackNext before this assertion takes place.
	  */
	public int assertNegative( int var, int state) {
		if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("\nassert (" + stackNext + ") " + vars.get(var) + " != " +
									((FiniteVariable)vars.get(var)).instance(state));
		}
		int ret = stackNext;

 		//allows single indexing instead of double as a speedup in the code
		boolean possState[] = possibleStates[var];

//		if( possibleStates[var] == null) {  //KB doesn't know anything about this variable
//			return ret;
//		}

		if( !possState[state]) { //already known
			if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("\nend assert (" + stackNext + ") " + vars.get(var) + " != " +
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
				if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("\nend assertNeg->assertPos");}
				return assertLearnedPositive( var, otherValidState);
			}
			else if( numOtherValidStates == 0) {
				if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("KB_UNSATISFIABLE tried to set " + vars.get(var) + " != " + ((FiniteVariable)vars.get(var)).instance(state));}
				return KB_UNSATISFIABLE;
			}
		}

		possState[state] = false;
		addToStack( var, state, NEGATIVE);

		int in_cl[] = inClauses[var];

		for( int i=0; i<in_cl.length; i++) {
			int cl = in_cl[i];
//			if( cl < 0) { continue;} //not a valid clause (has wasted space if this is true);

			if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println("clause " + cl + " numLeft: " + clausesNumLeft[cl]); }

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
								if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("KB_UNSATISFIABLE cannot satisfy " + clStr(cl));}
								return KB_UNSATISFIABLE;//undo everything this function has done so far
							}
							if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( clStr(cl) + " is ? but " + litStr(lit) + " is unsat"); }
						}
						else {
							literalsSatState[lit] = LIT_SATISFIED;
							clausesNumLeft[cl] = 0; //this clause is sat
							if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( clStr(cl) + " is sat by " + litStr(lit)); }

//							if( USE_numUnSubsumedClauses) {
//								int beg = clauseFirstLiteral(cl);
//								int end = clauseLastLiteral(cl);
//								for( int ul=beg; ul<=end; ul++) {
//									numUnSubsumedClauses[literalsVars[ul]]--;
//									if( numUnSubsumedClauses[literalsVars[ul]] == 0) {
//										listner.kbDontCallAssertOnVar(literalsVars[ul]);
//									}
//								}
//							}
						}
					}
					else {
						//do nothing if var=state' in clause and
						//  learn var!=state (unless it is last which was done above)
					}
				}//end if already unsat
			}//end if have numLeft

//			if( clausesNumLeft[cl] == 1) {
//				if( onlyOneLiteralLeft( cl) == KB_UNSATISFIABLE) {
//					retract( ret); return KB_UNSATISFIABLE;
//				}//undo everything this function has done so far
//			}
		} //end for each clause var is in

		for( int i=0; i<in_cl.length; i++) {
			int cl = in_cl[i];
			if( clausesNumLeft[cl] == 1) {
				if( onlyOneLiteralLeft( cl) == KB_UNSATISFIABLE) {
					retract( ret); return KB_UNSATISFIABLE;
				}//undo everything this function has done so far
			}
		}

		if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("\nend assert (" + stackNext + ") " + vars.get(var) + " != " +
							((FiniteVariable)vars.get(var)).instance(state));
		}
		return ret;
	}


	public void retract( int size) {
		if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("\nretract back to " + size);}
		if( size < 0) { size = 0;}
		if( size > stackNext) {
			//something is amiss (possibly just reset stack to 0 & clearing out other things
//			if( stackNext != 0) {
//				System.err.println("called KB.retract, but " + size + "
//									is greater than stackNext " + stackNext);
//			}
			if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("end retract back to " + size);}
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
					if( !pos) { STREAM_VERBOSE.println("pop (" + s_i + ") " + fv + " != " + fv.instance(state));}
					else { STREAM_VERBOSE.println("pop (" + s_i + ") " + fv + " = " + fv.instance(state));}
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
//					STREAM_VERBOSE.println("update kb & then possibly continue to pop more");
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
//				if( cl < 0) { continue;} //not a valid clause (has wasted space if this is true);

				int lit = inClausLit[var][j];


				if( DEBUG_VERBOSE2) {STREAM_VERBOSE.println( "test: " + clStr(cl) + " and " + litStr(lit));}
				if( DEBUG_VERBOSE2) {STREAM_VERBOSE.println( "test: " + literalsSatState[lit]);}

				if( literalsSatState[lit] == LIT_UNSATISFIABLE) { //possibly not unsat anymore

					if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( litStr(lit) + " was unsatisfiable, test to see if changed."); }

					if(literalsPositive[lit]) {
						//if var=state is possible now, it is not unsat anymore
						if( possibleStates[var][literalsStates[lit]] == true) {
							literalsSatState[lit] = LIT_UNKNOWN;
							clausesNumLeft[cl]++;
							if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( litStr(lit) + " was unsat, but is now possible in " + clStr(cl));}
							if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println("updated numLeft to " + clausesNumLeft[cl]);}
							if( clausesNumLeft[cl] == 1) { throw new IllegalStateException("");}
						}
					}
					else {
						if( posState == literalsStates[lit]) { //stack had neg of all states except one, but that was not valid in this literal
							literalsSatState[lit] = LIT_UNKNOWN;
							clausesNumLeft[cl]++;
							if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( litStr(lit) + " was unsat, but is now possible in " + clStr(cl));}
							if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println("updated numLeft to " + clausesNumLeft[cl]);}
							if( clausesNumLeft[cl] == 1) { throw new IllegalStateException("");}
						}
					}
				}

				else if( literalsSatState[lit] == LIT_SATISFIED) {

					if( DEBUG_VERBOSE2) {STREAM_VERBOSE.println( litStr(lit) +
												" was satisfied, test to see if changed.");}

					if( literalsPositive[lit]) {
						literalsSatState[lit] = LIT_UNKNOWN;
						if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( litStr(lit) + " is not satisfied."); }
					}
					//was satisfied because this was not possible
					else if( possibleStates[var][literalsStates[lit]] == true) {
						literalsSatState[lit] = LIT_UNKNOWN;
						if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( litStr(lit) + " is not satisfied."); }
					}
					else {
						if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( litStr(lit) + " is still satisfied."); }
					}


					if( literalsSatState[lit] == LIT_UNKNOWN) {

						int numle = 0;
						int beg = clauseFirstLiteral(cl);
						int end = clauseLastLiteral(cl);
						if( DEBUG_VERBOSE2) {STREAM_VERBOSE.println( clStr(cl));}
						for( int l=beg; l<=end; l++) {


							if( literalsSatState[l] == LIT_UNSATISFIABLE) {
								if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( litStr(l) + " is unsatisfiable"); }
							}
							else if( literalsSatState[l] == LIT_UNKNOWN) {
								if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( litStr(l) + " is satisfiable"); }
								numle++;
							}
							else { //something else still satisfied, but this literal said
								   //     it was (cannot have two satisfied literals)
								if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( litStr(l) + " is satisfied"); }
								throw new IllegalStateException("");
							}
						}

//						if( USE_numUnSubsumedClauses && clausesNumLeft[cl] == 0 && numle != 0) {
//							for( int ul=beg; ul<=end; ul++) {
//								numUnSubsumedClauses[literalsVars[ul]]++;
//								if( numUnSubsumedClauses[literalsVars[ul]] == 1) {
//									listner.kbCallAssertOnVar(literalsVars[ul]);
//								}
//							}
//						}

						clausesNumLeft[cl] = numle;
						if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( clStr(cl) + " has numLeft = " + numle); }
//						if( numle > 0) {
//							listnerUnLearnedPositive = true;
//							if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println("assertUnLearnedPositive triggered by " + litStr(lit)); }
//						}
					}
				}

			} //end for all clauses var is in
		}//end for each stack value
		stackNext = size;
		if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println("stackNext = " + stackNext);}
		if( DEBUG_KB_ASSERT) { STREAM_VERBOSE.println("end retract back to " + size);}


	}




	final private void addToStack( int var, int val, boolean pos) {
		if( DEBUG_STACK_CHANGES) { STREAM_VERBOSE.println("addToStack(" + var + "," + val + "," + pos + ")"); }


		//grow stack if necessary
		if( stackNext >= stackVar.length) {
			if( DEBUG_STACK_EXPANSION) { STREAM_VERBOSE.println("Expand stack from " + stackVar.length + " to " +
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

//		int beg = -1;
//		int end = -1;
//		if( USE_numUnSubsumedClauses) {
//			beg = clauseFirstLiteral(cl);
//			end = clauseLastLiteral(cl);
//			for( int ul=beg; ul<=end; ul++) {
//				numUnSubsumedClauses[literalsVars[ul]]--;
//				if( numUnSubsumedClauses[literalsVars[ul]] == 0) {
//					listner.kbDontCallAssertOnVar(literalsVars[ul]);
//				}
//			}
//		}


		if( literalsPositive[satlit]) {
			ret = assertLearnedPositive( literalsVars[satlit], literalsStates[satlit]);
		}
		else {
			ret = assertLearnedNegative( literalsVars[satlit], literalsStates[satlit]);
		}

		if( ret == KB_UNSATISFIABLE) {
			clausesNumLeft[cl] = tmp;
			literalsSatState[satlit] = tmp2;

//			if( USE_numUnSubsumedClauses) {
//				for( int ul=beg; ul<=end; ul++) {
//					numUnSubsumedClauses[literalsVars[ul]]++;
//					if( numUnSubsumedClauses[literalsVars[ul]] == 1) {
//						listner.kbCallAssertOnVar(literalsVars[ul]);
//					}
//				}
//			}

		} //undo everything this function has done so far
		return ret;
	}



	public void write( PrintStream stream) {
		stream.println("KB:");
		for( int cl=0; cl<clausesIndex.length; cl++) {
			int beg = clauseFirstLiteral(cl);
			int end = clauseLastLiteral(cl);

			for( int l=beg; l<=end; l++) {
				FiniteVariable fv = (FiniteVariable)vars.get( literalsVars[l]);
				if( literalsPositive[l]) {
					stream.print( fv + "=" + fv.instance(literalsStates[l]) + " ");
				}
				else {
					stream.print( fv + "!=" + fv.instance(literalsStates[l]) + " ");
				}
			}
			stream.println("");
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





	public final static int KB_UNSATISFIABLE = Integer.MIN_VALUE;

	/** This Exception is thrown when the KnowledgeBase becomes inconsistent,
	  *  or unsatisfiable.
	  */
	static public class KBUnsatisfiableStateException extends Exception {
		public KBUnsatisfiableStateException(String message) {
			super( message);
		}
	}




	/** Assumes Tables in BN have probabilities in the range of 0..1.*/
	static class KnowledgeBaseGeneratorFromBN {

		static final boolean DEBUG_ARRAY_GROWTH = false;
		static final int LITERALS_LENGTH_INITIAL = 5000;
		static final int CLAUSES_LENGTH_INITIAL = 500;

		MappedList vars;
		int clausesIndex[];
		int literalsVars[];
		int literalsStates[];
		boolean literalsPositive[];

		int nextClause;
		int nextLiteral;

		boolean inUse = false;


		public KnowledgeBaseGeneratorFromBN(){}


		public KnowledgeBase createFromBN( BeliefNetwork bn, MappedList vs,
											KnowledgeBaseListener lstn)
		throws KBUnsatisfiableStateException {

			if( inUse) { throw new IllegalStateException("This object is already being used, create a new one");}
			inUse = true;

			if( DEBUG_VERBOSE) { STREAM_VERBOSE.println("\n\nCreate a KB from a BN");}

			vars = vs;

			clausesIndex = new int[CLAUSES_LENGTH_INITIAL];
			literalsVars = new int[LITERALS_LENGTH_INITIAL];
			literalsStates = new int[LITERALS_LENGTH_INITIAL];
			literalsPositive = new boolean[LITERALS_LENGTH_INITIAL];

			nextClause = 0;
			nextLiteral = 0;

			//for each table start adding variables, clauses, & literals
			for( Iterator itr = bn.iterator(); itr.hasNext();) {
				FiniteVariable fv = (FiniteVariable)itr.next();
				CPTShell cs = fv.getCPTShell( fv.getDSLNodeType() );
				handleTable( cs.getCPT());
			}

			//make arrays exact size
			{
				int old[];
				boolean oldb[];

				old = clausesIndex;
				clausesIndex = new int[nextClause];
				System.arraycopy( old, 0, clausesIndex, 0, nextClause);

				old = literalsVars;
				literalsVars = new int[nextLiteral];
				System.arraycopy( old, 0, literalsVars, 0, nextLiteral);

				old = literalsStates;
				literalsStates = new int[nextLiteral];
				System.arraycopy( old, 0, literalsStates, 0, nextLiteral);

				oldb = literalsPositive;
				literalsPositive = new boolean[nextLiteral];
				System.arraycopy( oldb, 0, literalsPositive, 0, nextLiteral);
			}


			KnowledgeBase ret = new KnowledgeBase( vars, clausesIndex,  literalsVars, literalsStates,
													literalsPositive, lstn);
			inUse = false;
			return ret;
		}


		private void handleTable( Table tbl)
		throws KBUnsatisfiableStateException {

			FiniteVariable parents[] = tbl.index().getParents();
			FiniteVariable localV = tbl.index().variable( parents.length);
			int inst[] = new int[ parents.length + 1]; //include parents & child (possibly no parents)

			if( DEBUG_VERBOSE) { STREAM_VERBOSE.println("Handle table for " + localV + ", " + Arrays.asList( parents));}

			for( int i=0; i<parents.length; i++) { inst[i] = parents[i].size()-1;} //all parents to maxState
			int parIndx = parents.length-1; //set it to the right

			while( true) { //for all possible states of parents (always at least one)

				boolean foundOne = false;

				for( int i=0; i<localV.size(); i++) { //for each state of variable look for P(i)=1.0
					inst[inst.length-1] = i;
					if( tbl.getCP( tbl.index().index(inst)) == 1.0) {
						foundOne = true;
						break;
					}
				}

				if( foundOne) { //have a 1.0 at inst[]
					addVars( parents, localV);

					int oldc = nextClause;
					int oldl = nextLiteral;

					addClause();
					if( DEBUG_VERBOSE2) { STREAM_VERBOSE.print("Add Clause " +nextClause+ " (1.0): ");}
					for( int l=0; l<inst.length-1; l++) {
						if( parents[l].size() != 1) { //if parent only has one state, the negation of that is unsatisfiable (so don't add)
							addLiteral( parents[l], inst[l], false); //add negative of each parent
							if( DEBUG_VERBOSE2) { STREAM_VERBOSE.print( parents[l] + "!=" + parents[l].instance(inst[l]) + " ");}
						}
					}
					if( localV.size() != 1) {
						addLiteral( localV, inst[inst.length-1], true); //add positive of localV
						if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( localV + "=" + localV.instance(inst[inst.length-1]));}
					}
					else { //if localV only has one state, then this clause is already satisfied, remove it
						nextClause = oldc;
						nextLiteral = oldl;
						if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( "\tRetract clause, back to : " + nextClause + ", " + nextLiteral);}
					}
				}
				else { //no 1.0, so add clauses for any 0s
					addVars( parents, localV);

					for( int i=0; i<localV.size(); i++) { //for each state of variable if P=0, add clause
						inst[inst.length-1] = i;
						if( tbl.getCP( tbl.index().index(inst)) == 0.0) {

							int oldc = nextClause;
							int oldl = nextLiteral;
							int numLitAdded = 0;

							addClause();
							if( DEBUG_VERBOSE2) { STREAM_VERBOSE.print("Add Clause "+nextClause+" (0.0): ");}
							for( int l=0; l<inst.length-1; l++) {
								if( parents[l].size() != 1) {
									addLiteral( parents[l], inst[l], false); //add negative of each parent
									if( DEBUG_VERBOSE2) { STREAM_VERBOSE.print( parents[l] + "!=" + parents[l].instance(inst[l]) + " ");}
									numLitAdded++;
								}
							}
							if( localV.size() != 1) { //if parent only has one state, the negation of that is unsatisfiable (so don't add)
								addLiteral( localV, inst[inst.length-1], false); //add negative of localV
								if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( localV + "!=" + localV.instance(inst[inst.length-1]));}
								numLitAdded++;
							}
							else {
								if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( "");}
							}
							if( numLitAdded == 0) { //its possible all parents & child only have one state & it is 0 (KB is inconsistent)
								nextClause = oldc;
								nextLiteral = oldl;
								if( DEBUG_VERBOSE2) { STREAM_VERBOSE.println( "\tRetract clause, back to : " + nextClause + ", " + nextLiteral);}
								throw new KBUnsatisfiableStateException("Initial KB is inconsistent!");
							}
						}
					}
				}


				//find new state of parents, if none, stop
				while( parIndx > -1 && inst[parIndx] == 0) {//if last state on parIndx, set it to Max and move left one (or more)
					inst[parIndx] = parents[parIndx].size()-1;
					parIndx--;
				}

				if( parIndx < 0) { break;} //no more available so stop

				inst[parIndx]--; //reduce this one
				parIndx = parents.length-1; //set it back to the right

			} //end while( true) for each parent state



		}


		private void addVars( FiniteVariable pars[], FiniteVariable loc) {
			if( !vars.contains( loc)) { vars.add( loc);}
			for( int i=0; i<pars.length; i++) {
				if( !vars.contains( pars[i])) { vars.add( pars[i]);}
			}
		}

		private void addClause() {
			if( nextClause >= clausesIndex.length) {
				int old[] = clausesIndex;
				clausesIndex = new int[old.length + CLAUSES_LENGTH_INITIAL];
				System.arraycopy( old, 0, clausesIndex, 0, old.length);
				if( DEBUG_ARRAY_GROWTH) { STREAM_VERBOSE.println("clausesIndex grew to " + clausesIndex.length);}
			}
			clausesIndex[ nextClause] = nextLiteral;
			nextClause++;
		}

		private void addLiteral( FiniteVariable var, int state, boolean pos) {
			if( nextLiteral >= literalsVars.length) {
				int old[] = literalsVars;
				literalsVars = new int[old.length + LITERALS_LENGTH_INITIAL];
				System.arraycopy( old, 0, literalsVars, 0, old.length);

				old = literalsStates;
				literalsStates = new int[old.length + LITERALS_LENGTH_INITIAL];
				System.arraycopy( old, 0, literalsStates, 0, old.length);

				boolean oldb[] = literalsPositive;
				literalsPositive = new boolean[oldb.length + LITERALS_LENGTH_INITIAL];
				System.arraycopy( oldb, 0, literalsPositive, 0, oldb.length);

				if( DEBUG_ARRAY_GROWTH) { STREAM_VERBOSE.println("literals grew to " + literalsVars.length);}
			}
			literalsVars[nextLiteral] = vars.indexOf( var);
			literalsStates[nextLiteral] = state;
			literalsPositive[nextLiteral] = pos;
			nextLiteral++;
		}


	} //end class KnowledgeBaseGeneratorFromBN


	public interface KnowledgeBaseListener {
		/** This is called if the KB learns about a variable from unit propagation, and was not
		  *  told about this from the user.
		  *
		  *  <p>If this one is called, may not call AssertLearnedNegative for all the states.
		  */
		public void assertLearnedPositive( int fv, int state);
		/** This is called if the KB learns about a variable from unit propagation, and was not
		  *  told about this from the user.
		  */
//		public void assertLearnedNegative( int fv, int state);
//		/** Will be called for variables from assert and assertLearned (called whenever removed from stack).*/
//		public void assertUnLearnedNegative( int fv, int state);
		/** Called when transitioning from 1 possible state to two possibe states
		  *  (for any variable, whether the KB learned it or was told it).
		  */
		public void assertUnLearnedPositive( int fv);
		public void kbDontCallAssertOnVar( int fv);
		public void kbCallAssertOnVar( int fv);
	}


} //end class KnowledgeBase
