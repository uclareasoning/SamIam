package edu.ucla.belief.rc2.caching;

import java.util.*;

import edu.ucla.belief.rc2.structure.*;



/** This class generates caching schemes from Collections for RC2 Objects.
 */
final public class RC2CachingScheme_Collection implements RC2.CachingScheme {

	final public String name;
	final protected Collection nodesToCache;

	/**This constructor saves the collection nodesToCache and uses it directly,
	 * it does not make a copy of it or ever modify it.
	 */
	public RC2CachingScheme_Collection(String name, Collection nodesToCache){
		this.name = name;
		this.nodesToCache = nodesToCache;
	}

	public String toString() {return name + " Caching";}

	public Collection getCachingScheme(RC2 rc) {
		return nodesToCache;
	}

} //end class RC2CachingScheme_Collection


