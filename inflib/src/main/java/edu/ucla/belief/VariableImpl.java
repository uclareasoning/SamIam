package edu.ucla.belief;

import edu.ucla.belief.io.dsl.DiagnosisType;
import edu.ucla.belief.io.dsl.DSLNodeType;
import edu.ucla.belief.sensitivity.ExcludePolicy;
import edu.ucla.belief.inference.map.MapProperty;
import edu.ucla.util.HiddenProperty;
import edu.ucla.util.ImpactProperty;
import edu.ucla.util.InOutDegreeProperty;
import edu.ucla.util.EvidenceAssertedProperty;
import edu.ucla.util.InferenceValidProperty;
import edu.ucla.util.QueryParticipantProperty;
import edu.ucla.util.CSITypeProperty;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.VariableStringifier;
import edu.ucla.util.SdpProperty;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
	@author Keith Cascio
	@since 093002
*/
public abstract class VariableImpl implements Variable
{
	//{ cpt valid, diagnosis type, evidence, hidden, impact, in-out degree, map, query participant, sensitivity policy }
	//public static final EnumProperty[] PROPERTIES = new EnumProperty[] { InferenceValidProperty.PROPERTY, DiagnosisType.PROPERTY, EvidenceAssertedProperty.PROPERTY, HiddenProperty.PROPERTY, ImpactProperty.PROPERTY, InOutDegreeProperty.PROPERTY, MapProperty.PROPERTY, QueryParticipantProperty.PROPERTY, /* DSLNodeType.PROPERTY,*/ ExcludePolicy.PROPERTY };
	private static EnumProperty[] PROPERTIES;
	public static final int INT_NUM_PROPERTIES = (int)11;

	/** @since 091504 */
	public static final void initProperties(){
		if( PROPERTIES != null ) return;

		EnumProperty[] tempProperties = new EnumProperty[INT_NUM_PROPERTIES];
		int i=0;
		tempProperties[i++] = InferenceValidProperty.PROPERTY;
		tempProperties[i++] = CSITypeProperty.PROPERTY;
		tempProperties[i++] = DiagnosisType.PROPERTY;
		tempProperties[i++] = EvidenceAssertedProperty.PROPERTY;
		tempProperties[i++] = HiddenProperty.PROPERTY;
		tempProperties[i++] = ImpactProperty.PROPERTY;
		tempProperties[i++] = InOutDegreeProperty.PROPERTY;
		tempProperties[i++] = MapProperty.PROPERTY;
		tempProperties[i++] = QueryParticipantProperty.PROPERTY;
		tempProperties[i++] = ExcludePolicy.PROPERTY;
        tempProperties[i++] = SdpProperty.PROPERTY;
		PROPERTIES = tempProperties;

		//System.out.println( "PROPERTIES: " );
		//for( int j=0; j<PROPERTIES.length; j++ ){
		//	System.out.println( j + ": " + PROPERTIES[j] );
		//}
	}

	/** @since 091404 */
	public static int getNumProperties(){
		return INT_NUM_PROPERTIES;
		//return PROPERTIES.length;
	}

	/** @since 091404 */
	public static void propertiesArrayCopy( EnumProperty[] into ){
		if( into != null && into.length >= INT_NUM_PROPERTIES ){
			if( PROPERTIES == null ) initProperties();
			System.arraycopy( PROPERTIES, 0, into, 0, PROPERTIES.length );
		}
	}

	public abstract Object clone();

	public VariableImpl(String id)
	{
		this.id = id;
		this.myEnumProperties = new HashMap( INT_NUM_PROPERTIES );
	}

	public VariableImpl( Variable toCopy )
	{
		//System.out.println( "(VariableImpl)("+getClass().getName()+")"+toCopy.getID()+"(copy)" );
		this.id = toCopy.getID();
		this.userobject = toCopy.getUserObject();
		this.myEnumProperties = new HashMap( toCopy.getEnumProperties() );
	}

	/** @since 082003 */
	public EnumValue getProperty( EnumProperty property )
	{
		return (EnumValue) myEnumProperties.get( property );
	}
	public Map getEnumProperties()
	{
		return Collections.unmodifiableMap( myEnumProperties );
	}
	public void setProperty( EnumProperty property, EnumValue value )
	{
		//System.out.println( "(" +getClass().getName().substring(16)+ ")"+this+".setProperty( "+property+", "+value+" )" );
		myEnumProperties.put( property, value );
	}
	public void delete( EnumProperty property )
	{
		myEnumProperties.remove( property );
	}

	public String getID()
	{
		return id;
	}

	public void setID( String id )
	{
		this.id = id;
	}

	public Object getUserObject()
	{
		return userobject;
	}

	public void setUserObject( Object obj )
	{
		userobject = obj;
	}

	public int compareTo( Object obj )
	{
		//(new Throwable()).printStackTrace();
		//int ret = theCollator.compare( this.id, ((Variable)obj).getID() );
		//System.out.println( "(" + getClass().getName() + ")" + this + ".compareTo(("+obj.getClass().getName()+")"+obj+") = " + ret );
		//return ret;
		return theCollator.compare( this.id, ((Variable)obj).getID() );
	}

	public static final int index( EnumProperty property )
	{
		initProperties();
		for( int i=0; i<PROPERTIES.length; i++ )
			if( PROPERTIES[i] == property ) return i;
		return (int)-1;
	}
	public static final boolean validatePropertyNameAndID( Object name, Object id )
	{
		initProperties();
		String strName = name.toString();
		String strID = id.toString();
		for( int i=0; i<PROPERTIES.length; i++ )
			if( PROPERTIES[i].getName().equals( strName ) || PROPERTIES[i].getID().equals( strID ) ) return false;
		return true;
	}
	public static final EnumProperty forID( String propID )
	{
		initProperties();
		for( int i=0; i<PROPERTIES.length; i++ )
			if( PROPERTIES[i].getID().equals( propID ) ) return PROPERTIES[i];
		return null;
	}

	/** @since 052104 */
	public static final void setStringifier( VariableStringifier vs ){
		STRINGIFIER = vs;
	}
	/** @since 052104 */
	public static final VariableStringifier getStringifier(){
		return STRINGIFIER;
	}
	/** @since 052104 */
	public static final boolean isStringifier(){
		return STRINGIFIER != null;
	}
	private static VariableStringifier STRINGIFIER;

	public static final java.util.Comparator theCollator = java.text.Collator.getInstance();

	public String id;
	public Object userobject;
	private Map myEnumProperties;
}
