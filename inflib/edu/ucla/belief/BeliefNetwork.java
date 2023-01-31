package edu.ucla.belief;

import java.util.*;

import edu.ucla.structure.*;
import edu.ucla.util.UserObject;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.belief.io.NodeLinearTask;

/**
* Encapsulates information necessary to specify a belief network. A belief
* network consists of a Directed graph whose nodes are the Variables
* defining the structure of the network, and a mapping from Variable to
* Table representing the CPT of that variable.
*/
public interface BeliefNetwork extends DirectedGraph, Cloneable
{
	//public UserObject getUserObject();
	//public void setUserObject( UserObject obj );
	//public UserObject getUserObject2();
	//public void setUserObject2( UserObject obj );

	/** @since 20080219 */
	public FiniteVariable newFiniteVariable( Map properties );

	public EnumProperty[] propertiesAsArray();
	public int countUserEnumProperties();
	public Collection getUserEnumProperties();
	public void setUserEnumProperties( Collection userProperties );
	public void makeUserEnumProperties( Map params );
	public boolean forAll( EnumProperty property, EnumValue value );
	public boolean thereExists( EnumProperty property, EnumValue value );
	public Collection findVariables( EnumProperty property, EnumValue value );
	public void setAutoCPTInvalidation( boolean flag );
	public boolean getAutoCPTInvalidation();
	public boolean thereExistsModifiedUserEnumProperty();
	public void setUserEnumPropertiesModified( boolean flag );

	/** @since 072903 */
	public void setScalars( double scalar );

	/** @since 100202 */
	public boolean mayContain( Object obj );

	/** @since 100302 */
	public void induceGraph( Map mapVariablesToPotentials );

	/** @since 100402 */
	public void replaceAllPotentials( Map mapVariablesToPotentials );

	/** @since 022503 */
	public void cloneAllCPTShells();

	public EvidenceController getEvidenceController();
	public void setEvidenceController( EvidenceController EC );

	/** @since 021804 */
	public Copier getCopier();
	public Object clone();
	public BeliefNetwork deepClone();
	public BeliefNetwork seededClone( Map variablesOldToNew );
	public BeliefNetwork shallowClone();
	public void replaceVariables( Map variablesOldToNew, NodeLinearTask task );

	public void identifierChanged( String oldID, Variable var );

	/** @author keith cascio
		@since 20091124 */
	public interface Auditor{
		/** @author keith cascio
			@since 20091124 */
		public static class Intention{
			public String verb;
			private Intention( String verb ){
				this.verb = verb;
			}
			public static final Intention
			  beget = new Intention( "add" ),
			  kill  = new Intention( "delete" );
		};
		/** @author keith cascio
			@since 20091124 */
		public static class Unit{
			public String noun;
			private Unit( String noun ){
				this.noun = noun;
			}
			public static final Unit
			  edge   = new Unit( "edge"     ),
			  node   = new Unit( "variable" ),
			  belief = new Unit( "evidence" );
		};
		/** @author keith cascio
			@since 20091124 */
		public static class Deed{
			public Intention intention;
			public Unit      unit;
			private Deed( Intention intention, Unit unit ){
				this.intention = intention;
				this.unit      = unit;
			}
			public static final Deed
			  CREATE_EDGE = new Deed( Intention.beget, Unit.edge   ),
			  CREATE_NODE = new Deed( Intention.beget, Unit.node   ),
			  OBSERVE     = new Deed( Intention.beget, Unit.belief ),
			  DROP_EDGE   = new Deed( Intention.kill,  Unit.edge   ),
			  DROP_NODE   = new Deed( Intention.kill,  Unit.node   ),
			  RETRACT     = new Deed( Intention.kill,  Unit.belief );
		};
		/** @return an error message to veto creation/removal of the edge, or creation/removal of the variable */
		public String audit( BeliefNetwork bn, Variable from, Variable to, Collection targets, Deed deed );
	}
	/** @since 20091124 */
	public boolean    addAuditor( Auditor auditor );
	/** @since 20091124 */
	public boolean removeAuditor( Auditor auditor );
	/** @since 20091124 */
	public BeliefNetwork fireAudit( Variable from, Variable to, Collection targets, Auditor.Deed deed );

	/**
	* Adds an edge to the belief network. Both variables must already
	* be part of the network, and it must leave the graph acyclic.
	* The CPT will be expanded to include the new parent, and the values
	* set will be independant of it. The relations for the other parents
	* will remain as they were before.
	*/
	public boolean    addEdge( Variable from, Variable to, boolean expandCPT );

	/** Removes an edge from the network. If argument 'forget' is true,
	*   the resulting CPT for the variable to
	*   is formed from taking the original CPT
	*   and summing out the from variable. */
	public boolean removeEdge( Variable from, Variable to, boolean forget );
  //public boolean removeEdge( Variable from, Variable to );

	/**
	* Adds a new node to the graph, with no parents. The CPT
	* created will be uniform.
	*/
	public boolean addVariable( Variable newNode, boolean createCPT );
	/**
	* Removes a variable from the network. Any children of that variable
	* are first disconnected by calling removeEdge(var,child).
	*/
	public boolean removeVariable(Variable var);

	public int getMaxDomainCardinality();
	public int getMinDomainCardinality();

	public int getTheoreticalCPTSize( FiniteVariable fVar );
	public int getMaxTheoreticalCPTSize();
	public int getMinTheoreticalCPTSize();

	public boolean insertState( FiniteVariable var, int index, Object instance );
	public Object removeState( FiniteVariable var, int index );

	/** @since 060602 */
	public boolean checkValidProbabilities();

	/* @deprecated */
	//public Set variables();

	/** @since 042902 */
	public Collection tables();

	/**
	* Returns the variable with the name supplied.
	*/
	public Variable forID(String name);
}
