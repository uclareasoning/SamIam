package edu.ucla.belief.rc2.kb;

import edu.ucla.structure.MappedList;
import edu.ucla.belief.FiniteVariable;

import java.util.*;
import java.io.*;

/**
	Define KnowledgeBase.

	@author David Allen
*/

public interface KnowledgeBase
{

	public final static int KB_UNSATISFIABLE = Integer.MIN_VALUE;

	public int numClauses();
	public int numLiterals();


//	public KnowledgeBase createKB(MappedList vars, MultiValuedCNF cnf, KnowledgeBaseListener lstn);


	/** Returns the current state of the KB, if retract is later called
	 *  with this value, the KB will return to this state.
	 */
	public int currentState();

	/** Asserts var=state as true.
	  * If KB is unsatisfiable it will return KB_UNSATISFIABLE.
	  * @returns The state of the KB prior to this assertion.
	  */
	public int assertPositive( int var, int state);

	/** The parameter state should be the returned value from either an
	 *  assertPositive call or from a currentState call.
	 */
	public void retract(int state);



	public interface KnowledgeBaseListener {
		/*KB's do not need to subscribe to this, as some don't track and report
		 * when a variable is learned.  The KB will just return false when RC
		 * attempts to iterate over that variable.
		 */


		/** This is called if the KB learns about a variable from unit propagation, and was not
		  *  told about this from the user.
		  *
		  *  <p>If this one is called, may not call AssertLearnedNegative for all the states.
		  */
		public void assertLearnedPositive( int fv, int state);
		/** Called when transitioning from 1 possible state to two possibe states
		  *  (for any variable, whether the KB learned it or was told it).
		  */
		public void assertUnLearnedPositive( int fv);
	}


	/* Returns:
	 *   Number of possible states, -1 if it is not in the knowledgebase.
	 */
	public int numPossibleStates(FiniteVariable fv);


} //end interface KnowledgeBase
