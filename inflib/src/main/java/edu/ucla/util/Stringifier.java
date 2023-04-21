package edu.ucla.util;

/** @author keith cascio
	@since 20041102 election day */
public interface Stringifier
{
	public String objectToString(     Object               o    );
	public String collectionToString( java.util.Collection list );
	public String mapToString(        java.util.Map        map  );

	/** @since 20070321 */
	public interface Selector{
		public Stringifier selectStringifier();
	}
}
