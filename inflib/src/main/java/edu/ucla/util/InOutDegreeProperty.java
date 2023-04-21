package edu.ucla.util;

import edu.ucla.belief.Variable;
import edu.ucla.belief.BeliefNetwork;

import java.util.*;

/** @author keith cascio
	@since  20031009 */
public class InOutDegreeProperty extends AbstractEnumProperty
{
	public static final InOutDegreeProperty PROPERTY = new InOutDegreeProperty();

	private InOutDegreeProperty() {}

	public String getName()
	{
		return "in-out degree";
	}

	public EnumValue getDefault()
	{
		return ROOT;
	}

	public String getID()
	{
		return "in-outdegree";
	}

	public boolean isUserEditable()
	{
		return false;
	}
	public boolean isTransient()
	{
		return true;
	}
	public boolean contains( EnumValue val )
	{
		return myList.contains( val );
	}
	public EnumValue forString( String str )
	{
		for( int i=0; i<myArray.length; i++ )
		{
			if( myArray[i].toString().equals( str ) ) return myArray[i];
		}
		return null;
	}
	public Iterator iterator()
	{
		return myList.iterator();
	}
	public EnumValue[] valuesAsArray()
	{
		return myArray;
	}
	public int size()
	{
		return myArray.length;
	}
	public boolean toBoolean( EnumValue value )
	{
		return value == INTERNAL;
	}
	public EnumValue valueOf( boolean flag )
	{
		return flag ? INTERNAL : ROOT;
	}

	public static class InOutDegree extends UserEnumValue
	{
		private InOutDegree( Object name )
		{
			super( name, InOutDegreeProperty.PROPERTY );
		}
	}

	public static InOutDegree valueFor( Variable var, BeliefNetwork bn )
	{
		if( bn.inDegree( var ) < (int)1 ) return ROOT;
		else if( bn.outDegree( var ) < (int)1 ) return LEAF;
		else return INTERNAL;
	}

	public static void setValue( Variable var, BeliefNetwork bn )
	{
		var.setProperty( PROPERTY, valueFor( var, bn ) );
	}

	public static void setAllValues( Collection vars, BeliefNetwork bn )
	{
		for( Iterator it = vars.iterator(); it.hasNext(); )
			setValue( (Variable)it.next(), bn );
	}

	/** @since 20050823 */
	public int indexOf( EnumValue value ){
		for( int i=0; i<myArray.length; i++ ){
			if( myArray[i] == value ) return i;
		}
		return -1;
	}

	/** @since 20050823 */
	public EnumValue forIndex( int index ){
		if( (-1 < index) && (index < myArray.length) ) return myArray[index];
		else return (EnumValue) null;
	}

	public static final InOutDegree ROOT      = new InOutDegree( "root"      ),
	                                INTERNAL  = new InOutDegree( "internal"  ),
	                                LEAF      = new InOutDegree( "leaf"      ),
	                                UNDEFINED = new InOutDegree( "undefined" );
	public static final EnumValue[] myArray   = new EnumValue[] { ROOT, INTERNAL, LEAF };
	public static final List        myList    = Arrays.asList( myArray );
}
