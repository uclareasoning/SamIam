package edu.ucla.util;

import edu.ucla.belief.Variable;

/** Built in property to designate variables of interest before pruning.
	@author keith cascio
	@since  20051006 */
public class QueryParticipantProperty extends FlagProperty
{
	public static final QueryParticipantProperty PROPERTY = new QueryParticipantProperty();

	private QueryParticipantProperty() {}

	public String getName(){
		return "query participant";
	}

	public EnumValue getDefault(){
		return TRUE;
	}

	public String getID(){
		return "isqueryparticipant";
	}

	public static boolean isQueryParticipant( Variable var ){
		if( var == null ) return false;
		else return var.getProperty( PROPERTY ) == PROPERTY.TRUE;
	}
}
