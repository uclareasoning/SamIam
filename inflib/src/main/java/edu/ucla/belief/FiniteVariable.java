package edu.ucla.belief;

import edu.ucla.structure.Filter;
import edu.ucla.belief.io.dsl.DSLNodeType;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collection;

/**
	A class for representing finite variables.
	It consists of a name as well as a list
	of possible values.
*/
public interface FiniteVariable extends Variable
{
	/** Returns the number of possible values. */
	public int size();

	/** Returns the instance associated with the index. */
	public Object instance( int index );

	/**
		Returns the index associated with the instance. Returns -1 if
		instance is not one of the values this variable can take on.
	*/
	public int index( Object instance );

	/** @author Keith Cascio
		@since 121802 */
	public boolean contains( Object instance );

	/** @author Keith Cascio
		@since 100803 */
	public Object instance(String name);

	/** @since  20070419
		@return count matched
		@param  results accumulate matches */
	public int grep( Filter filter, Collection results );

	/** @since  20070329
		@return count matched
		@param  invert  invert the sense of the grep, i.e. accumulate non-matches
		@param  results accumulate matches */
	public int grep( Pattern pattern, boolean invert, Collection results );

	/** @since  20070329
		@return count matched
		@param  invert  invert the sense of the grep, i.e. accumulate non-matches
		@param  results accumulate matches */
	public int grep( Matcher matcher, boolean invert, Collection results );

	public java.util.List instances();

	/**
		@author Keith Cascio
		@since 101102
	*/
	public Object set( int index, Object objNew );

	public boolean insert( int index, Object instance );
	public Object remove( int index );

	/** moved from DSLNode 010905
		@since 010905 */
	public DSLNodeType getDSLNodeType();

	/** moved from DSLNode 010905
		@since 010905 */
	public void setDSLNodeType( DSLNodeType newVal );

	public CPTShell getCPTShell();
	/** @deprecated */
	public void setCPTShell( CPTShell shell );
	//public DecisionShell getDecisionShell();
	//public void setDecisionShell( DecisionShell shell );
	//public CPTShell getOtherShell();
	//public void setOtherShell( CPTShell shell );
	public CPTShell getCPTShell( DSLNodeType type );
	public void setCPTShell( DSLNodeType type, CPTShell shell );
}
