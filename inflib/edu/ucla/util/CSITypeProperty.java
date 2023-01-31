package edu.ucla.util;

import edu.ucla.belief.Variable;
import edu.ucla.belief.BeliefNetwork;

import java.util.*;

/** Represents the category of a variable's
	context-specific independence.

	@author keith cascio
	@since  20060127 */
public class CSITypeProperty extends AbstractEnumProperty
{
	public static final CSITypeProperty PROPERTY = new CSITypeProperty();

	private CSITypeProperty() {}

	public String getName(){
		return "csi type";
	}

	public EnumValue getDefault(){
		return UNKNOWN;
	}

	public String getID(){
		return "csitype";
	}

	public boolean isUserEditable(){
		return true;
	}
	public boolean isTransient(){
		return false;
	}
	public boolean contains( EnumValue val ){
		return myList.contains( val );
	}
	public EnumValue forString( String str )
	{
		for( int i=0; i<myArray.length; i++ ){
			if( myArray[i].toString().equals( str ) ) return myArray[i];
		}
		return null;
	}
	public Iterator iterator(){
		return myList.iterator();
	}
	public EnumValue[] valuesAsArray(){
		return myArray;
	}
	public int size(){
		return myArray.length;
	}
	public boolean toBoolean( EnumValue value ){
		return value == OR;
	}
	public EnumValue valueOf( boolean flag ){
		return flag ? OR : UNKNOWN;
	}

	public class CSIType extends UserEnumValue{
		private CSIType( Object name ){
			super( name, CSITypeProperty.this );
		}
	}

	public static CSIType valueFor( Variable var, BeliefNetwork bn ){
		return PROPERTY.UNKNOWN;
	}

	public static void setValue( Variable var, BeliefNetwork bn ){
		var.setProperty( PROPERTY, valueFor( var, bn ) );
	}

	public static void setAllValues( Collection vars, BeliefNetwork bn ){
		for( Iterator it = vars.iterator(); it.hasNext(); )
			setValue( (Variable)it.next(), bn );
	}

	public int indexOf( EnumValue value ){
		for( int i=0; i<myArray.length; i++ ){
			if( myArray[i] == value ) return i;
		}
		return -1;
	}

	public EnumValue forIndex( int index ){
		if( (-1 < index) && (index < myArray.length) ) return myArray[index];
		else return (EnumValue) null;
	}

	public final CSIType     UNKNOWN = new CSIType( "unknown" ),
	                         OR      = new CSIType( "or"      ),
	                         AND     = new CSIType( "and"     );

	public final EnumValue[] myArray = new EnumValue[]{ UNKNOWN, OR, AND };
	public final List        myList  = Arrays.asList( myArray );
}
