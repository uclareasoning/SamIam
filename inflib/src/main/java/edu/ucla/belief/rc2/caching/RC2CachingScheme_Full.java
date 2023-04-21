package edu.ucla.belief.rc2.caching;

import java.util.*;

import edu.ucla.belief.rc2.structure.*;



/** This class generates full caching schemes for RC2 Objects.
 */
final public class RC2CachingScheme_Full implements RC2.CachingScheme {

	public RC2CachingScheme_Full(){}

	public String toString() {return "Full Caching";}

	public Collection getCachingScheme(RC2 rc) {
		Collection cachedNodes = RC2Utils.getSetOfAllNonLeafNodes(rc);
		RC2CachingSchemeUtils.removeWorthlessCaches(rc, cachedNodes);
		return cachedNodes;
	}

} //end class RC2CachingScheme_Full


