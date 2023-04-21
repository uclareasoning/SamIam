package edu.ucla.belief.io.dsl;

import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.AbstractEnumProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
//{superfluous} import java.util.Collection;
//{superfluous} import java.util.LinkedList;

/** A class that represents an enumerated type
	over the kinds of Genie nodes that
	inflib/samiam will currently support.

	@author keith cascio
	@since  20020304 */
public class DSLNodeType implements EnumValue
{
	private static List myValues = null;

	public static final DSLNodeType CPT =
		new DSLNodeType( DSLConstants.valueCPT,"CPT description", true );
	public static final DSLNodeType NOISY_OR =
		new DSLNodeType( DSLConstants.valueNOISY_OR,"NOISY_OR description", false );
	public static final DSLNodeType TRUTHTABLE =
		new DSLNodeType( DSLConstants.valueTRUTHTABLE,"TRUTHTABLE description", true );
	/** @since 010905 */
	public static final DSLNodeType DECISIONTREE =
		new DSLNodeType( "DECISION TREE","DECISION TREE description", false );

	/** @since 010905 */
	private static DSLNodeType[] ARRAY_INTERCONVERTIBLE, ARRAY_CPT, ARRAY_DECISIONTREE, ARRAY_NOISY_OR, ARRAY_TRUTHTABLE;

	/** @since 041305 */
	public static boolean FLAG_ENABLE_DECISIONTREE = false;

	/** @since 010905 */
	public static DSLNodeType[] getArrayInterconvertibleTypes( DSLNodeType type ){
		if( type == CPT ){
			if( FLAG_ENABLE_DECISIONTREE ) return getArrayInterconvertible();
			else{
				if( ARRAY_CPT == null ) ARRAY_CPT = new DSLNodeType[] { CPT };
				return ARRAY_CPT;
			}
		}
		else if( type == DECISIONTREE ){
			if( FLAG_ENABLE_DECISIONTREE ) return getArrayInterconvertible();
			else{
				if( ARRAY_DECISIONTREE == null ) ARRAY_DECISIONTREE = new DSLNodeType[] { DECISIONTREE };
				return ARRAY_DECISIONTREE;
			}
		}
		else if( type == NOISY_OR ){
			if( ARRAY_NOISY_OR == null ) ARRAY_NOISY_OR = new DSLNodeType[] { NOISY_OR };
			return ARRAY_NOISY_OR;
		}
		else if( type == TRUTHTABLE ){
			if( ARRAY_TRUTHTABLE == null ) ARRAY_TRUTHTABLE = new DSLNodeType[] { TRUTHTABLE };
			return ARRAY_TRUTHTABLE;
		}
		else return valuesAsArray();
	}

	/** @since 041305 */
	private static DSLNodeType[] getArrayInterconvertible(){
		if( ARRAY_INTERCONVERTIBLE == null ){
			ARRAY_INTERCONVERTIBLE = new DSLNodeType[] { CPT, DECISIONTREE };
		}
		return ARRAY_INTERCONVERTIBLE;
	}

	/** Call this method to get the standardized string representation of a DSLNodeType. */
	public String toString(){
		return myCode;
	}

	/** @since 081903 */
	public static EnumProperty PROPERTY = new AbstractEnumProperty()
	{
		public String getName()
		{
			return "representation";
		}
		public boolean contains( EnumValue val )
		{
			return DSLNodeType.myValues.contains( val );
		}
		public EnumValue forString( String str )
		{
			return DSLNodeType.forString( str );
		}
		public Iterator iterator()
		{
			return DSLNodeType.iterator();
		}
		public EnumValue[] valuesAsArray()
		{
			return DSLNodeType.valuesAsArray();
		}
		public int size()
		{
			return DSLNodeType.myValues.size();
		}
		public EnumValue getDefault()
		{
			return DSLNodeType.CPT;
		}
		public EnumValue valueOf( boolean flag )
		{
			return flag ? DSLNodeType.NOISY_OR : DSLNodeType.CPT;
		}
		public String getID()
		{
			return "dslnodetype";
		}
		public boolean isUserEditable()
		{
			return false;
		}
		public boolean isTransient()
		{
			return true;
		}

		/** @since 20050823 */
		public int indexOf( EnumValue value ){
			return DSLNodeType.indexOf( value );
		}

		/** @since 20050823 */
		public EnumValue forIndex( int index ){
			return DSLNodeType.forIndex( index );
		}
	};

	/** @since 20070420 */
	public EnumProperty property(){
		return PROPERTY;
	}

	private DSLNodeType(){}

	private DSLNodeType( String code, String description, boolean flagTableType )
	{
		this.myCode = code;
		this.myDescription = description;
		this.myFlagTableType = flagTableType;

		if( myValues == null ) myValues = new ArrayList();
		myValues.add( this );
	}

	/** @since 20060215 */
	public boolean isTableType(){
		return this.myFlagTableType;
	}

	public static DSLNodeType[] valuesAsArray()
	{
		return _array_type = (DSLNodeType[])(myValues.toArray( _array_type ));
	}

	public static Iterator iterator()
	{
		return myValues.iterator();
	}

	public static DSLNodeType forString( String code )
	{
		Iterator it = myValues.iterator();
		DSLNodeType temp = null;
		while( it.hasNext() )
		{
			temp = (DSLNodeType)(it.next());
			if( code.equals( temp.myCode ) )
			{
				return temp;
			}
		}
		return null;
	}

	/** @since 20050823 */
	public static int indexOf( EnumValue value ){
		if( _array_type.length < 2 ) valuesAsArray();
		for( int i=0; i<_array_type.length; i++ ){
			if( _array_type[i] == value ) return i;
		}
		return -1;
	}

	/** @since 20050823 */
	public static EnumValue forIndex( int index ){
		if( (-1 < index) && (index < _array_type.length) ) return _array_type[index];
		else return (EnumValue) null;
	}

	private static DSLNodeType[] _array_type = { new DSLNodeType() };
	private String myCode = null;
	private String myDescription = null;
	private boolean myFlagTableType = false;
}
