package edu.ucla.belief.rc2.caching;

import java.util.*;

import edu.ucla.belief.rc2.structure.*;



/** This class generates DFBnB caching schemes for RC2 Objects.
 */
final public class RC2CachingScheme_DFBnB implements RC2.CachingScheme {

	public RC2CachingScheme_DFBnB(double cf, double numCacheEntries){}

	public String toString() {return "DFBnB Caching";}

	public Collection getCachingScheme(RC2 rc) {
		throw new UnsupportedOperationException(""); //TODO
	}

} //end class RC2CachingScheme_DFBnB


