package edu.ucla.belief;

import edu.ucla.structure.Filter;
import edu.ucla.structure.MappedList;
import edu.ucla.structure.IdentityArrayMap;
import edu.ucla.belief.io.dsl.DSLNodeType;

import java.util.*;
import java.util.regex.*;

/**
	A class for representing finite variables.
	It consists of a name as well as a list
	of possible values.
*/
public class FiniteVariableImpl extends VariableImpl implements FiniteVariable
{
	/**
	* Creates a new Finite variable.
	* @param name the name the Variable will be known by.
	* @param instances the list of instances this variable can take on. These may be of
	* any type.
	*/
	public FiniteVariableImpl( String id, Object[] instances )
	{
		this( id, java.util.Arrays.asList(instances) );
	}

	public FiniteVariableImpl( String id, List instances )
	{
		super( id );
		this.myMappedListInstances = new MappedList( instances );
		if( instances.size() != myMappedListInstances.size() ) throw new IllegalStateException( "Illegal variable definition: \"" +id+ "\" contains duplicate state names.\n" );
	}

	/** @author keith cascio
		@since  20020603 */
	public FiniteVariableImpl( FiniteVariable toCopy )
	{
		super( toCopy );
		this.myMappedListInstances = (MappedList) ((MappedList)toCopy.instances()).clone();
		//this.myCPTShell = (CPTShell) toCopy.getCPTShell().clone();
		this.myMapTypesToShells = deepCloneMapTypesToShells( toCopy );
	}

	/** @author keith cascio
		@since  20071225 xmas */
	public FiniteVariableImpl( FiniteVariable toCopy, DSLNodeType type, CPTShell shell ){
		super( toCopy );
		this.myMappedListInstances = (MappedList) ((MappedList) toCopy.instances()).clone();
		this.myMapTypesToShells    = new HashMap(1);
		this.myMapTypesToShells.put( type, shell );
	}

	/** @since  20070419
		@return count matched
		@param  results accumulate matches */
	public int grep( Filter filter, Collection results ){
		return myMappedListInstances.grep( filter, results );
	}

	/** @since  20070329
		@return count matched
		@param  invert  invert the sense of the grep, i.e. add non-matches
		@param  results accumulate matches */
	public int grep( Pattern pattern, boolean invert, Collection results ){
		return myMappedListInstances.grep( pattern, invert, results );
	}

	/** @since  20070329
		@return count matched
		@param  invert  invert the sense of the grep, i.e. add non-matches
		@param  results accumulate matches */
	public int grep( Matcher matcher, boolean invert, Collection results ){
		return myMappedListInstances.grep( matcher, invert, results );
	}

	/** @since 010905 */
	public static Map deepCloneMapTypesToShells( FiniteVariable toCopy )
	{
		Map ret = new IdentityArrayMap();
		DSLNodeType[] types = DSLNodeType.valuesAsArray();

		DSLNodeType type;
		CPTShell shell;
		for( int i=0; i<types.length; i++ ){
			type = types[i];
			shell = toCopy.getCPTShell( type );
			if( shell != null ) ret.put( type, shell.clone() );
		}

		return ret;
	}

	/**
		@author Keith Cascio
		@since 060302
	*/
	public Object clone()
	{
		FiniteVariableImpl ret = new FiniteVariableImpl( this );
		return ret;
	}

	public List instances()
	{
		return myMappedListInstances;
	}

	public Object set( int index, Object objNew )
	{
		return myMappedListInstances.set( index, objNew );
	}

	public boolean insert( int index, Object instance )
	{
		LinkedList temp = new LinkedList( myMappedListInstances );
		temp.add( index, instance );
		myMappedListInstances = new MappedList( temp );
		//if( myCPTShell != null ) myCPTShell.insertState( index );
		if( myMapTypesToShells != null ){
			DSLNodeType type;
			CPTShell shell;
			for( Iterator it = myMapTypesToShells.keySet().iterator(); it.hasNext(); ){
				type = (DSLNodeType) it.next();
				shell = (CPTShell) myMapTypesToShells.get( type );
				shell.insertState( index );
			}
		}
		return true;
	}

	public Object remove( int index )
	{
		Object ret = myMappedListInstances.remove( index );
		//if( myCPTShell != null ) myCPTShell.removeState( index );
		if( myMapTypesToShells != null ){
			DSLNodeType type;
			CPTShell shell;
			for( Iterator it = myMapTypesToShells.keySet().iterator(); it.hasNext(); ){
				type = (DSLNodeType) it.next();
				shell = (CPTShell) myMapTypesToShells.get( type );
				shell.removeState( index );
			}
		}
		return ret;
	}

	public CPTShell getCPTShell(){
		//return myCPTShell;
		return getCPTShell( getDSLNodeType() );
	}

	/** @deprecated */
	public void setCPTShell( CPTShell shell ){
		DSLNodeType type = null;
		if( shell instanceof TableShell ) type = DSLNodeType.CPT;
		else if( shell instanceof NoisyOrShell ) type = DSLNodeType.NOISY_OR;
		else if( shell instanceof DecisionShell ) type = DSLNodeType.DECISIONTREE;
		else throw new IllegalStateException();
		setCPTShell( type, shell );
	}

	/** @since 010905 */
	public CPTShell getCPTShell( DSLNodeType type ){
		if( myMapTypesToShells == null ) return (CPTShell) null;
		else return (CPTShell) myMapTypesToShells.get( type );
	}

	/** @since 010905 */
	public void setCPTShell( DSLNodeType type, CPTShell shell ){
		if( (shell == null) && (myMapTypesToShells != null) ){
			myMapTypesToShells.remove( type );
			return;
		}
		if( myMapTypesToShells == null ) myMapTypesToShells = new IdentityArrayMap();
		myMapTypesToShells.put( type, shell );
	}

	/** @since 010905 */
	public DSLNodeType getDSLNodeType(){
		return myDSLNodeType;
	}

	/** @since 010905 */
	public void setDSLNodeType( DSLNodeType newVal ){
		this.myDSLNodeType = newVal;
	}

	/** moved from DSLNodeType 20081110
		@since 20051107 */
	public static Collection findVariablesForType( Collection bn, DSLNodeType target, Collection ret ){
		if( ret == null ) ret = new LinkedList();
		if( bn == null ) return ret;
		FiniteVariable var;
		for( Iterator it = bn.iterator(); it.hasNext(); ){
			var = (FiniteVariable) it.next();
			if( var.getDSLNodeType() == target ) ret.add( var );
		}
		return ret;
	}

	/** moved from DSLNodeType 20081110
		@since 20051107 */
	public static FiniteVariable thereExists( Collection bn, DSLNodeType target ){
		if( bn == null ) return (FiniteVariable)null;
		FiniteVariable var;
		for( Iterator it = bn.iterator(); it.hasNext(); ){
			var = (FiniteVariable) it.next();
			if( var.getDSLNodeType() == target ) return var;
		}
		return (FiniteVariable)null;
	}

	/**
	* Returns the number of possible values.
	*/
	public int size()
	{
		return myMappedListInstances.size();
	}

	/**
	* Returns the instance associated with the index.
	*/
	public Object instance(int index)
	{
		return myMappedListInstances.get(index);
	}

	/**
	* Returns the instance represented by the string. Linearly searches the instances, so should
	* be used sparingly.
	*/
	public Object instance( String name )
	{
		Object ret;
		for( Iterator it = myMappedListInstances.iterator(); it.hasNext(); ){
			if( (ret = it.next()).toString().equals( name ) ) return ret;
		}

		return null;
	}

	/**
	* Returns the index associated with the instance. Returns -1 if
	* instance is not one of the values this variable can take on.
	*/
	public int index(Object instance)
	{
		return myMappedListInstances.indexOf(instance);
	}

	public boolean contains( Object instance )
	{
		return myMappedListInstances.contains(instance);
	}

	public String toString()
	{
		String ret = id;
		if( Definitions.DEBUG ){ ret += " " + super.toString().substring( 30 ); }
		return ret;
	}

	/**
	* Returns the number of instantiations of the collection of variables.
	* It returns a long only because reasonable sized sets sometimes overflow
	* ints.
	*/
	public static long size(Collection vars)
	{
		long result = 1;
		for (Iterator iter = vars.iterator(); iter.hasNext();)
		{
			FiniteVariable fv = (FiniteVariable) iter.next();
			result *= fv.size();
		}

		return result;
	}

	/**
		@author Keith Cascio
		@since 031003
	*/
	public static FiniteVariableImpl debugInstance()
	{
		FiniteVariableImpl fVar = new FiniteVariableImpl( "debugInstance", new String[]{ "state1", "state2", "state3" } );
		FiniteVariable[] vars = new FiniteVariable[]{ fVar };
		double datum1 = (double)1/(double)6;
		double datum2 = (double)2/(double)6;
		double datum3 = (double)3/(double)6;
		double[] data = new double[] { datum1, datum2, datum3 };
		Table table = new Table( vars, data );
		TableShell shell = new TableShell( table );
		fVar.setCPTShell( shell );
		return fVar;
	}

	private MappedList myMappedListInstances;
	//private CPTShell myCPTShell;
	private Map myMapTypesToShells;
	private DSLNodeType myDSLNodeType = DSLNodeType.CPT;
}
