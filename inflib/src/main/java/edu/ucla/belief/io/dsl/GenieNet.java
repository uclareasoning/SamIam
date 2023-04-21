package edu.ucla.belief.io.dsl;

//{superfluous} import java.awt.Point;
//{superfluous} import java.awt.Dimension;
import java.util.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.PropertySuperintendent;

/**
	@author Keith Cascio
	@since 100102
*/
public interface GenieNet extends BeliefNetwork, PropertySuperintendent
{
	public Collection getVariables( DSLSubmodel forModel );
	public Set getDeepVariables( DSLSubmodel forModel );
	public boolean isAnscestor( DSLSubmodel forModel, Variable var );

	/**
		Recusively traverse submodel tree rooted at forModel and add all variables to ret.

		@param forModel The root of the submodel tree to traverse.
		@param ret The Collection to which all variables will be added.
	*/
	public void addDeepVariables( Collection ret, DSLSubmodel forModel );

	public DSLSubmodelFactory getDSLSubmodelFactory();

	public void setParams( Map params );
}
