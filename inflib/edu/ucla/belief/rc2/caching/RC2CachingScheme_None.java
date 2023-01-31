package edu.ucla.belief.rc2.caching;

import java.util.*;

import edu.ucla.belief.rc2.structure.*;



/** This class generates empty caching schemes for RC2 Objects.
 */
final public class RC2CachingScheme_None implements RC2.CachingScheme {

	public RC2CachingScheme_None(){}

	public String toString() {return "No Caching";}

	public Collection getCachingScheme(RC2 rc) {
		return Collections.EMPTY_SET;
	}

} //end class RC2CachingScheme_None


