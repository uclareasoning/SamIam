package edu.ucla.belief.io.dsl;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.AbstractEnumProperty;

/** A class that represents an enumerated type
	over the designations of nodes in
	Genie Diagnosis.

	@author keith cascio
	@since  20020304 */
public class DiagnosisType implements EnumValue
{
	private static List myValues = null;

	public static final DiagnosisType TARGET =
		new DiagnosisType( "TARGET","TARGET description", SMILEReader.TARGET );
	public static final DiagnosisType OBSERVATION =
		new DiagnosisType( "OBSERVATION","OBSERVATION description", SMILEReader.OBSERVATION );
	public static final DiagnosisType AUXILIARY =
		new DiagnosisType( "AUXILIARY","AUXILIARY description", SMILEReader.AUXILIARY );

	/** Call this method to get the standardized string representation of a DiagnosisType. */
	public String toString()
	{
		return myCode;
	}

	/** @since 20030916 */
	public int getSmileID()
	{
		return mySmileID;
	}

	/** @since 20030819 */
	public static EnumProperty PROPERTY = new AbstractEnumProperty()
	{
		public String getName()
		{
			return "diagnosis type";
		}
		public boolean contains( EnumValue val )
		{
			return DiagnosisType.myValues.contains( val );
		}
		public EnumValue forString( String str )
		{
			return DiagnosisType.forString( str );
		}
		public Iterator iterator()
		{
			return DiagnosisType.iterator();
		}
		public EnumValue[] valuesAsArray()
		{
			return DiagnosisType.valuesAsArray();
		}
		public int size()
		{
			return DiagnosisType.getNumTypes();
		}
		public EnumValue getDefault()
		{
			return DiagnosisType.AUXILIARY;
		}
		public EnumValue valueOf( boolean flag )
		{
			return flag ? DiagnosisType.TARGET : DiagnosisType.AUXILIARY;
		}
		public String getID()
		{
			return "diagnosistype";
		}
		/** @since 20050823 */
		public int indexOf( EnumValue value ){
			return DiagnosisType.indexOf( value );
		}

		/** @since 20050823 */
		public EnumValue forIndex( int index ){
			return DiagnosisType.forIndex( index );
		}
	};

	/** @since 20070420 */
	public EnumProperty property(){
		return PROPERTY;
	}

	/** @since 20020820 */
	public static int getNumTypes()
	{
		return myValues.size();
	}

	private DiagnosisType(){}

	private DiagnosisType( String code, String description, int smileID )
	{
		if ( myValues == null ) myValues = new ArrayList();

		this.myCode = code;

		this.myDescription = description;

		this.mySmileID = smileID;

		myValues.add( this );
	}

	public static DiagnosisType[] valuesAsArray()
	{
		return _array_type = (DiagnosisType[])(myValues.toArray( _array_type ));
	}

	public static Iterator iterator()
	{
		return myValues.iterator();
	}

	public static DiagnosisType forString( String code )
	{
		Iterator it = myValues.iterator();
		DiagnosisType temp = null;
		while( it.hasNext() )
		{
			temp = (DiagnosisType)(it.next());
			if( code.equals( temp.myCode ) )
			{
				return temp;
			}
		}
		return null;
	}

	/** @since 20030916 */
	public static DiagnosisType forSmileID( int id )
	{
		DiagnosisType temp = null;
		for( Iterator it = myValues.iterator(); it.hasNext(); )
		{
			temp = (DiagnosisType)(it.next());
			if( temp.mySmileID == id ) return temp;
		}
		return null;
	}

	/** @since 20050823 */
	public static int indexOf( EnumValue value ){
		//int ret = -99;
		//try{
		if( _array_type.length < 2 ) valuesAsArray();
		for( int i=0; i<_array_type.length; i++ ){
			if( _array_type[i] == value ) return i;
		}
		return -1;
		//}finally{
		//	System.out.println( "indexOf( "+value+" )? " + ret );
		//	System.out.println( "forString()? " + forString( value.toString() ) );
		//}
	}

	/** @since 20050823 */
	public static EnumValue forIndex( int index ){
		if( (-1 < index) && (index < _array_type.length) ) return _array_type[index];
		else return (EnumValue) null;
	}

	private static DiagnosisType[] _array_type = { new DiagnosisType() };
	private String myCode = null;
	private String myDescription = null;
	private int mySmileID = (int)0;
}
