package edu.ucla.belief.io;

import edu.ucla.belief.sensitivity.ExcludePolicy;
//{superfluous} import edu.ucla.belief.sensitivity.SensitivityEngine;
import edu.ucla.belief.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.SdpProperty;
import edu.ucla.belief.inference.map.MapProperty;

import java.util.*;
import java.awt.Point;
import java.awt.Dimension;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** @author keith cascio
	@since  20021003 */
public abstract class StandardNodeImpl extends FiniteVariableImpl implements StandardNode
{
	protected abstract Dimension makeDimension();

	public StandardNodeImpl( String id, List values, Map properties )
	{
		this( id, values );
		makeDiagnosisType( properties );
		makeExcludePolicy( properties );
		makeMAPProperty( properties );
        makeSDPProperty( properties );
	}

	/** @since 100102 */
	protected StandardNodeImpl( FiniteVariable toCopy )
	{
		super( toCopy );

		if( toCopy instanceof StandardNodeImpl )
		{
			StandardNodeImpl SNItoCopy = (StandardNodeImpl)toCopy;

			if( SNItoCopy.myExcludeArray != null )
			{
				this.myExcludeArray = new boolean[SNItoCopy.myExcludeArray.length];
				System.arraycopy( SNItoCopy.myExcludeArray, 0, myExcludeArray, 0, myExcludeArray.length );
			}
			if( SNItoCopy.myLocation != null )
			{
				this.myLocation = new Point( SNItoCopy.myLocation );
			}
			if( SNItoCopy.myDimension != null )
			{
				this.myDimension = new Dimension( SNItoCopy.myDimension );
			}

			myExcludePolicy = SNItoCopy.myExcludePolicy;
		}
		else if( toCopy instanceof StandardNode )
		{
			StandardNode SNtoCopy = (StandardNode)toCopy;

			boolean[] hisExcludeArray = SNtoCopy.getExcludeArray();
			if( hisExcludeArray != null )
			{
				this.myExcludeArray = new boolean[ hisExcludeArray.length ];
				System.arraycopy( hisExcludeArray, 0, myExcludeArray, 0, myExcludeArray.length );
			}

			this.myLocation = SNtoCopy.getLocation( new Point() );
			this.myDimension = SNtoCopy.getDimension( new Dimension() );

			myExcludePolicy = SNtoCopy.getExcludePolicy();
		}
	}

	/** @since 20060523 */
	public StandardNodeImpl( String id, List values ){
		super( id, values );
	}

	/** @since 051002 */
	public boolean[] getExcludeArray()
	{
		return myExcludeArray;
	}

	/** @since 20020510 */
	public void setExcludeArray( boolean[] xa ){
		//System.out.println( "StandardNodeImpl.setExcludeArray( xa["+xa.length+"] )" );
		myExcludeArray = xa;
		getProperties().put( KEY_EXCLUDEARRAY, ExcludePolicy.makeExcludeString( this, myExcludeArray ) );
	}

	/** Moved from HuginNodeImpl 062204 @since 071603 */
	public boolean isMAPVariable()
	{
		Object val = getProperties().get( KEY_ISMAPVARIABLE );
        Map properties = getProperties();
		if( val == null ) return false;
		else return Boolean.TRUE.equals( val ) || VALUE_TRUE.equals( val.toString() );
	}

    public boolean isSDPVariable()
	{
        Object val = getProperties().get( KEY_ISSDPVARIABLE );
        Map properties = getProperties();
		if( val == null ) return false;
		else return Boolean.TRUE.equals( val ) || VALUE_TRUE.equals( val.toString() );
	}
    
	/** Moved from HuginNodeImpl 062204 @since 071603 */
	public void setMAPVariable( boolean flag )
	{
		//System.out.println( "StandardNodeImpl.setMAPVariable( "+flag+" )" );
		Map properties = getProperties();
		if( properties != null ) properties.put( KEY_ISMAPVARIABLE, flag ? Boolean.TRUE : Boolean.FALSE );
		setProperty( MapProperty.PROPERTY, MapProperty.PROPERTY.getValue( flag ) );
	}

    public void setSDPVariable( boolean flag )
	{
		Map properties = getProperties();
		if( properties != null ) properties.put( KEY_ISSDPVARIABLE, flag ? Boolean.TRUE : Boolean.FALSE );
		setProperty( SdpProperty.PROPERTY, SdpProperty.PROPERTY.getValue( flag ) );
	}

	/** Moved from HuginNodeImpl 062204 @since 082003 */
	protected void makeMAPProperty( Map properties )
	{
		Object val = properties.get( KEY_ISMAPVARIABLE );
		boolean flag = ( val != null && (Boolean.TRUE.equals( val ) || VALUE_TRUE.equals( val.toString() )) );
		setMAPVariable( flag );
	}

    protected void makeSDPProperty( Map properties )
	{
		Object val = properties.get( KEY_ISSDPVARIABLE );
		boolean flag = ( val != null && (Boolean.TRUE.equals( val ) || VALUE_TRUE.equals( val.toString() )) );
		setSDPVariable( flag );
	}
    
	/** @since 072203 */
	public ExcludePolicy getExcludePolicy()
	{
		return myExcludePolicy;
	}
	public void setExcludePolicy( ExcludePolicy ep )
	{
		//System.out.println( "StandardNodeImpl.setExcludePolicy( "+ep+" )" );
		setProperty( ExcludePolicy.PROPERTY, ep );
	}

	/** @since 072403 */
	protected void makeExcludePolicy( Map vals )
	{
		if( vals != null )
		{
			Object ep = vals.get( KEY_EXCLUDEPOLICY );
			if( ep instanceof String )
			{
				myExcludePolicy = ExcludePolicy.forString( ep.toString() );
				if( myExcludePolicy != null ) vals.put( KEY_EXCLUDEPOLICY, myExcludePolicy );
			}
			else if( ep instanceof ExcludePolicy ) myExcludePolicy = (ExcludePolicy)ep;
		}
		setExcludePolicy( myExcludePolicy );
	}

	/** @since 072403 */
	protected void makeDiagnosisType( Map vals )
	{
		//System.out.println( "StandardNodeImpl.makeDiagnosisType( "+((vals==null)?"null":"non-null")+" )" );
		if( vals != null )
		{
			Object dt = vals.get( DSLConstants.KEY_EXTRADEFINITION_DIAGNOSIS_TYPE );
			if( dt instanceof String )
			{
				myDiagnosisType = DiagnosisType.forString( dt.toString() );
				if( myDiagnosisType != null ) vals.put( DSLConstants.KEY_EXTRADEFINITION_DIAGNOSIS_TYPE, myDiagnosisType );
			}
			else if( dt instanceof DiagnosisType ) myDiagnosisType = (DiagnosisType)dt;
		}
		setDiagnosisType( myDiagnosisType );
	}

	/** @since 082703 */
	public void setProperty( EnumProperty property, EnumValue value )
	{
		//System.out.println( "StandardNodeImpl.setProperty( "+property+", "+value+" )" );
		super.setProperty( property, value );
		Map mapProperties = getProperties();
		if( property == ExcludePolicy.PROPERTY )
		{
			myExcludePolicy = (ExcludePolicy) value;
			if( mapProperties != null ) mapProperties.put( KEY_EXCLUDEPOLICY, value );
		}
		else if( property == DiagnosisType.PROPERTY )
		{
			myDiagnosisType = (DiagnosisType) value;
			if( mapProperties != null ) mapProperties.put( DSLConstants.KEY_EXTRADEFINITION_DIAGNOSIS_TYPE, value );
		}
		else if( property == MapProperty.PROPERTY )
		{
			if( mapProperties != null ) mapProperties.put( KEY_ISMAPVARIABLE, property.toBoolean( value ) ? Boolean.TRUE : Boolean.FALSE );
		}
        else if( property == SdpProperty.PROPERTY )
		{
			if( mapProperties != null ) mapProperties.put( KEY_ISSDPVARIABLE, property.toBoolean( value ) ? Boolean.TRUE : Boolean.FALSE );
		}
	}

	/** @since 041304 */
	public Integer getDefaultStateIndex()
	{
		Map mapProperties = getProperties();
		if( mapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_SETASDEFAULT ) )
		{
			Object objValueSetAsDefault = mapProperties.get( DSLConstants.KEY_EXTRADEFINITION_SETASDEFAULT );
			if( !(objValueSetAsDefault instanceof Boolean) )
			{
				objValueSetAsDefault = Boolean.valueOf( objValueSetAsDefault.toString() );
				mapProperties.put( DSLConstants.KEY_EXTRADEFINITION_SETASDEFAULT, objValueSetAsDefault );
			}
			if( ((Boolean)objValueSetAsDefault).booleanValue() )
			{
				if( mapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_DEFAULT_STATE ) )
				{
					Object objValueDefaultState = mapProperties.get(DSLConstants.KEY_EXTRADEFINITION_DEFAULT_STATE);
					if( !(objValueSetAsDefault instanceof Integer) )
					{
						objValueDefaultState = Integer.valueOf( objValueDefaultState.toString() );
						mapProperties.put( DSLConstants.KEY_EXTRADEFINITION_DEFAULT_STATE, objValueDefaultState );
					}
					return (Integer)objValueDefaultState;
				}
			}
		}

		return null;
	}

	/** @since 091603 */
	public DiagnosisType getDiagnosisType()
	{
		return myDiagnosisType;
	}

	/** @since 091603 */
	public void setDiagnosisType( DiagnosisType newVal )
	{
		//System.out.println( "StandardNodeImpl.setDiagnosisType( "+newVal+" )" );
		setProperty( DiagnosisType.PROPERTY, newVal );
	}

	/** @since 101702 */
	public Point getLocation( Point ret )
	{
		if( myLocation == null ) makeLocation();
		if( ret == null ) ret = new Point();
		ret.setLocation( myLocation );
		return ret;
	}

	/** @since 101702 */
	public void setLocation( Point newLoc )
	{
		if( myLocation == null ) myLocation = new Point( newLoc );
		else myLocation.setLocation( newLoc );
	}

	public Dimension getDimension( Dimension d )
	{
		if( myDimension == null ) myDimension = makeDimension();
		if( d == null ) d = new Dimension();
		d.setSize( myDimension );
		return d;
	}

	public void setDimension( Dimension d )
	{
		if( myDimension == null ) myDimension = new Dimension( d );
		else myDimension.setSize( d );
	}

	/** @since 010904 */
	static public int countMaxPositionCoincidence( BeliefNetwork bn )
	{
		//System.out.println( "StandardNodeImpl.maxPositionCoincidence()" );

		List topo = bn.topologicalOrder();
		Map map = new HashMap( topo.size() );
		int max = (int)1;

		StandardNode node;
		Point position;
		Struct struct;
		for( Iterator it = topo.iterator(); it.hasNext(); )
		{
			node = (StandardNode) it.next();
			position = node.getLocation( new Point() );
			struct = (Struct) map.get( position );
			if( struct == null )
			{
				struct = new Struct( (int)1 );
				map.put( position, struct );
			}
			else max = Math.max( max, ++struct.weight );
		}

		//System.out.println( "\t\t " + max );
		return max;
	}
	/** @since 010904 */
	public static class Struct {
		public Struct( int weight ){
			this.weight = weight;
		}
		public int weight = (int)0;
	}

	/** @since 20021017 */
	protected void makeLocation()
	{
		Map vals = getProperties();
		try{
			Object value = vals.get( KEY_HUGIN_POSITION );

			if( value instanceof Point ){
				myLocation = (Point) value;
				return;
			}

			myLocation = new Point();

			//will return a list of 2 floats [x, y]
			List valList = (List) value;

			if( valList == null || valList.size() != 2 ) {
				//System.err.println( "WARNING: Invalid or missing position information found in network." );
			}
			else
			{
				Number n = (Number)valList.get(0);
				myLocation.x = n.intValue();
				n = (Number)valList.get(1);

				//Hugin's y-axis gets larger as it goes up, the screen's does going down, so negate it.
				//myLocation.y = 0 - n.intValue();
				myLocation.y = n.intValue();
			}
		}finally{
			vals.remove( KEY_HUGIN_POSITION );
		}
	}

	/** @return A Map containing all key/value pairs from toCopy for which the value could be cloned.
		@author keith cascio
		@since 20020604 */
	public static Map deepCopyMap( Map toCopy )
	{
		Map        ret = new HashMap();
		Object tempKey = null, tempValue = null, newValue = null;
		for( Iterator it = toCopy.keySet().iterator(); it.hasNext(); )
		{
			if( (tempValue = toCopy.get( tempKey = it.next() )) == null ){ continue; }
			newValue  = null;

			if(      tempValue instanceof String     ){ newValue = tempValue; }
			else if( tempValue instanceof List       ){ newValue = new ArrayList( (List) tempValue ); }
			else if( tempValue instanceof Collection ){ newValue = Collections.unmodifiableCollection( (Collection)tempValue ); }
			else if( tempValue instanceof EnumValue  ){ newValue = tempValue; }//( tempValue instanceof DiagnosisType || tempValue instanceof DSLNodeType )
			else if( tempValue instanceof Integer    ){ newValue = new Integer( ((Integer)tempValue).    intValue() ); }
			else if( tempValue instanceof Double     ){ newValue = new  Double( ( (Double)tempValue). doubleValue() ); }
			else if( tempValue instanceof Boolean    ){ newValue = new Boolean( ((Boolean)tempValue).booleanValue() ); }
			else if( tempValue instanceof Cloneable  ){
				try{
					Method method = tempValue.getClass().getMethod( "clone", (Class[]) null );
					if( (method.getModifiers() & Modifier.PUBLIC) > 0 ){ newValue = method.invoke( tempValue, (Object[]) null ); }
				}catch( Throwable thrown ){
					newValue = null;
					System.err.println( thrown );
				}
			}

			if( (newValue == null) && (tempKey != DSLConstants.KEY_SUBMODEL) ){
				System.err.println( "Java non-cloneable Map entry: { " + tempKey + ", ("+tempValue.getClass().getName()+")" + tempValue + " }" ); }
			else{ ret.put( tempKey, newValue ); }
		}

		return ret;
	}

	/** @since 20081110 */
	static public EvidenceController setDefaultEvidence( EvidenceController castro ) throws StateNotFoundException{
		Integer         tempIndex = null;
		Object          next      = null;
		StandardNode    sn        = null;
		for( Iterator   it        = castro.getBeliefNetwork().iterator(); it.hasNext(); ){
			if( (next = it.next()) instanceof StandardNode ){
				if( (tempIndex =   (sn =     (StandardNode) next).getDefaultStateIndex()) != null ){
					castro.observe( sn, sn.instance( tempIndex.intValue() ) );
				}
			}
		}
		return castro;
	}

	/** @since 20100113 */
	static public int countDefaultEvidence( BeliefNetwork bn ){
		int             count     = 0;
		Object          next      = null;
		for( Iterator   it        = bn.iterator(); it.hasNext(); ){
			if( (next = it.next()) instanceof StandardNode ){
				if( ((StandardNode) next).getDefaultStateIndex() != null ){ ++count; }
			}
		}
		return count;
	}

	/** @since 20100113 */
	static public boolean seenDefaultEvidence( PropertySuperintendent props, BeliefNetwork bn ){
		try{
			if( props == null || props.getProperties() == null || bn == null ){ return false; }
			if( props.getProperties().get( PropertySuperintendent.KEY_SEENDEFAULTEVIDENCE ) == Boolean.TRUE ){ return true; }
			if( countDefaultEvidence( bn ) > 0 ){
				props.getProperties().put( PropertySuperintendent.KEY_SEENDEFAULTEVIDENCE,     Boolean.TRUE );
				return true;
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: StandardNodeImpl.seenDefaultEvidence() caught " + thrown );
		}
		return false;
	}

	private Dimension     myDimension;
	private Point         myLocation;
	/** @since 20020510 */
	private boolean[]     myExcludeArray;
	private ExcludePolicy myExcludePolicy = ExcludePolicy.INCLUDE;
	private DiagnosisType myDiagnosisType = DiagnosisType.AUXILIARY;
}
