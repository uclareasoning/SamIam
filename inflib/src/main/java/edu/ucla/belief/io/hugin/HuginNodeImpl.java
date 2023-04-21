package edu.ucla.belief.io.hugin;

//{superfluous} import java.awt.Point;
import java.awt.Dimension;
import java.util.*;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;

/**
	Encapsulates a hugin node description.
*/
public class HuginNodeImpl extends StandardNodeImpl implements HuginNode
{
	public HuginNodeImpl( String id, List instances, Map values, int argNodeType, int vartype )
	{
		super( id, instances, values );

		this.myMapProperties = values;

		if (argNodeType != NODE || vartype != DISCRETE) throw new IllegalArgumentException("We support only discrete nodes. "+
			id + " is an unsupported node type.");

		//myMapProperties.put( DSLConstants.HUGIN_node_ID, id );//Keith Cascio 052402

		if( vartype == DISCRETE ) myRepresentationType = DISCRETE;
		else myRepresentationType = CONTINUOUS;

		if( argNodeType == NODE ) this.myHuginType = NODE;
		else if( argNodeType == DECISION ) this.myHuginType = DECISION;
		else this.myHuginType = UTILITY;

		//makeExcludePolicy();
		//makeMAPProperty();
	}

	/**
		Constructs with default values: type NODE and DISCRETE.
		@author Keith Cascio
		@since 042902
	*/
	public HuginNodeImpl( String id, Map values )
	{
		this( id, values, NODE, DISCRETE );
	}

	public HuginNodeImpl( String id, Map values, int myHuginType, int vartype )
	{
		this( id, (List) values.get( KEY_HUGIN_STATES ), values, myHuginType, vartype );
	}

	public HuginNodeImpl( String id, List instances, Map values )
	{
		this( id, instances, values, NODE, DISCRETE );
	}

	/* @since 060402
	private HuginNodeImpl( HuginNodeImpl toCopy )
	{
		super( toCopy );
		this.myMapProperties = deepCopyMap( toCopy.myMapProperties );
		this.myRepresentationType = toCopy.myRepresentationType;
		this.myHuginType = toCopy.myHuginType;

		//System.out.println( "HuginNodeImpl( HuginNodeImpl toCopy )" );
	}*/

	/** @since 020603 */
	public HuginNodeImpl( FiniteVariable toUpgrade )
	{
		super( toUpgrade );

		if( toUpgrade instanceof PropertySuperintendent ){
			this.myMapProperties = deepCopyMap( ((PropertySuperintendent)toUpgrade).getProperties() );
		}
		else{
			this.myMapProperties = new HashMap();
		}

		if( toUpgrade instanceof HuginNode ){
			HuginNode HNToUpgrade = (HuginNode)toUpgrade;
			this.myRepresentationType = HNToUpgrade.getValueType();
			this.myHuginType = HNToUpgrade.getNodeType();
		}
		else{
			this.myRepresentationType = DISCRETE;
			this.myHuginType = NODE;
		}

		//System.out.println( "HuginNodeImpl( FiniteVariable toUpgrade )" );
	}

	public Object clone()
	{
		return new HuginNodeImpl( this );
	}

	protected Dimension makeDimension()
	{
		Object value = this.myMapProperties.get( KEY_HUGIN_NODE_SIZE );

		try{
			if( value instanceof Dimension ){ return (Dimension) value; }

			Dimension ret = new Dimension();

			List valList = (value instanceof List) ? ((List) value) : null;

			if( valList == null || valList.size() != 2)
			{
				ret.width = (int)80;
				ret.height = (int)40;
			}
			else
			{
				myFlagSpecifiedDimension = true;
				Number t = (Number)valList.get(0);
				ret.width = t.intValue();
				t = (Number)valList.get(1);
				ret.height = t.intValue();
			}

			return ret;
		}finally{
			this.myMapProperties.remove( KEY_HUGIN_NODE_SIZE );
		}
	}

	public boolean isSpecifiedDimension()
	{
		return myFlagSpecifiedDimension;
	}

	public void resetSpecifiedDimension()
	{
		setDimension( makeDimension() );
	}

	/**
	* Will get the label from a HuginNodeImpl.	Can possibly return null if one is not
	*	present.	Will not return empty string, will return null in its place.
	*/
	public String getLabel()
	{
		//It is possible for this to be null.
		Object val = this.myMapProperties.get( KEY_HUGIN_LABEL );
		if( val != null)
		{
			if( val.equals(""))
			{
				val = null;
			}
		}

		return (String)val;
	}

	public void setLabel( String newVal )
	{
		this.myMapProperties.put( KEY_HUGIN_LABEL, newVal );
	}

	/* @since 082703 */
	//public void setProperty( EnumProperty property, EnumValue value ){
	//	super.setProperty( property, value );
	//}

	public java.util.Map getProperties()
	{
		return myMapProperties;
	}

	public int getValueType()
	{
		return myRepresentationType;
	}

	public int getNodeType()
	{
		return myHuginType;
	}

	public String toString()
	{
		if( isStringifier() ) return getStringifier().variableToString( this );
		else return "HuginNode " + id;
	}

	//public FiniteVariable getFiniteVariable(){
	//	return this;
	//}

	private boolean myFlagSpecifiedDimension = false;
	private final Map myMapProperties;
	/**
	* The type of values it can take on. One of DISCRETE or CONTINUOUS.
	*/
	private final int myRepresentationType;
	/**
	* The type of node it is. One of NODE, DECISION or UTILITY.
	*/
	private final int myHuginType;
}
