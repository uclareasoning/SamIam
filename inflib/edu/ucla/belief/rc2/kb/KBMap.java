package edu.ucla.belief.rc2.kb;

import java.util.*;
import java.io.*;

/**
 *  This interface defines functions required for
 *  mapping for multi-valued variables to binary variables.
 *
 *  @author David Allen
 */
abstract public class KBMap {

	/**This creates a cnf file and a Mapping which allows Variables and states from a
	 *  Bayesian network to be set on the cnf.
	 * <p>fvToColOfClauses can be an empty map or null.
	 */
	public static Mapping createLogicEncoding(MultiValuedCNF mvcnf, File cnfFile, String title, Map fvToColOfClauses, boolean includeComments)  {throw new UnsupportedOperationException("");}


	static public class Mapping {

		public Mapping(String file, int map[][], int numv, int numc) {
			this.file = file;
			this.map = map;
			numVars = numv;
			numClauses = numc;
		}

		final public String file;
		final public int map[][]; //map[var][state] is the value which should be asserted in the cnf to set this
		final public int numVars;
		final public int numClauses; //this includes things like eclauses etc.
	}



	static private final double LN2 = Math.log(2);
	static protected double log2(double in) { return Math.log(in)/LN2;}


} //end class KBMap
