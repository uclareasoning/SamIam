package edu.ucla.belief.rc2.structure;

import java.util.*;

/** This is an interface for Instance Iterator objects.
 *
 * @author David Allen
 */
interface RC2InstItr {



	/**Find the next valid state of the iterator.  A return of false
	 * indicates that there are no more valid states and that it has
	 * cleared any evidence it set.
	 */
	abstract int next();


	/**This is safe to typecast to an integer, however it could overflow if doing computations, therefore
	 *  this returns a long value as a "warning" to the user that it can be very large.
	 */
	abstract long numInstantiations();

	abstract Collection getVars(Collection ret);

	abstract int getIndx(); //used by roots to return cutset marginals

}//end interface RC2InstItr
