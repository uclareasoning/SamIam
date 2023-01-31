package edu.ucla.belief.io.dsl;

//{superfluous} import java.awt.Point;
import java.awt.Dimension;
import java.util.*;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;

/**
	Encapsulates a SMILE node description.
*/
public class DSLNodeImpl extends StandardNodeImpl implements DSLNode
{
	/** @since 060402 */
	protected DSLNodeImpl( FiniteVariable toCopy )//DSLNodeImpl toCopy )
	{
		super( toCopy );

		if( toCopy instanceof PropertySuperintendent ){
			this.myMapProperties = deepCopyMap( ((PropertySuperintendent)toCopy).getProperties() );
		}
		else{
			this.myMapProperties = new HashMap();
		}

		if( toCopy instanceof DSLNode ){
			this.myDSLSubmodel = ((DSLNode)toCopy).getDSLSubmodel();
		}
		else{
			this.myDSLSubmodel = null;
		}
	}

	/** @since 20020429 */
	public DSLNodeImpl( String id, Map props )
	{
		super( id, (List) props.get( KEY_HUGIN_STATES ), props );
		this.myMapProperties = props;

		//setDiagnosisType( getDiagnosisType() );
		setDSLNodeType( getDSLNodeType() );
		//makeExcludePolicy();

		if( props.containsKey( DSLConstants .KEY_SUBMODEL ) ){ setDSLSubmodel( (DSLSubmodel) props.get( DSLConstants .KEY_SUBMODEL ) ); }
	}

	public Object clone()
	{
		return new DSLNodeImpl( this );
	}

	/** @since 041902 */
	public DSLSubmodel getDSLSubmodel()
	{
		return myDSLSubmodel;
	}

	/** @since 041902 */
	public void setDSLSubmodel( DSLSubmodel model )
	{
		myDSLSubmodel = model;
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 031302
	*/
	public Dimension makeDimension()
	{
		Dimension d = new Dimension();

		if( myMapProperties.containsKey( DSLConstants.KEY_POSITION_WIDTH ) )
		{
			int width = ((Integer)(myMapProperties.get( DSLConstants.KEY_POSITION_WIDTH ))).intValue();
			int height = ((Integer)(myMapProperties.get( DSLConstants.KEY_POSITION_HEIGHT ))).intValue();
			d.width = width;
			d.height = height;
		}
		else
		{
			d.width = (int)80;
			d.height = (int)40;
		}

		return d;
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public String getID()
	{
		if( myMapProperties.containsKey( KEY_HUGIN_ID ) ){
			return (String)(myMapProperties.get(KEY_HUGIN_ID));
		}
		else return null;
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public void setID( String newVal )
	{
		super.setID( newVal );
		if( myMapProperties.containsKey( KEY_HUGIN_ID ) ){
			myMapProperties.put( KEY_HUGIN_ID, newVal );
		}
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public String getLabel()
	{
		if( myMapProperties.containsKey( KEY_HUGIN_LABEL ) ){
			return (String)(myMapProperties.get(KEY_HUGIN_LABEL));
		}
		else return null;
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public void setLabel( String newVal )
	{
		if( myMapProperties.containsKey( KEY_HUGIN_LABEL ) ){
			myMapProperties.put( KEY_HUGIN_LABEL, newVal );
		}
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public DSLNodeType getDSLNodeType()
	{
		if( myMapProperties.containsKey( DSLConstants.KEY_TYPE ) ){
			DSLNodeType mappedval = (DSLNodeType) myMapProperties.get( DSLConstants.KEY_TYPE );
			if( super.getDSLNodeType() != mappedval ) super.setDSLNodeType( mappedval );
		}
		//else return DSLNodeType.CPT;
		return super.getDSLNodeType();
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public void setDSLNodeType( DSLNodeType newVal )
	{
		if( myMapProperties.containsKey( DSLConstants.KEY_TYPE ) ){
			myMapProperties.put( DSLConstants.KEY_TYPE, newVal );
		}

		/*(StandardNode)*/super.setProperty( DSLNodeType.PROPERTY, newVal );

		/*(FiniteVariable)*/super.setDSLNodeType( newVal );
	}

	/** @since 082703 */
	public void setProperty( EnumProperty property, EnumValue value )
	{
		super.setProperty( property, value );
		if( myMapProperties != null )
		{
			if( property == DSLNodeType.PROPERTY ) myMapProperties.put( DSLConstants.KEY_TYPE, value );
		}
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public Boolean getMandatory()
	{
		if( myMapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_MANDATORY ) ){
			return (Boolean)(myMapProperties.get(DSLConstants.KEY_EXTRADEFINITION_MANDATORY));
		}
		else return Boolean.FALSE;
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public void setMandatory( Boolean newVal )
	{
		if( myMapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_MANDATORY ) ){
			myMapProperties.put( DSLConstants.KEY_EXTRADEFINITION_MANDATORY, newVal );
		}
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public Boolean getRanked()
	{
		if( myMapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_RANKED ) ){
			return (Boolean)(myMapProperties.get(DSLConstants.KEY_EXTRADEFINITION_RANKED));
		}
		else return Boolean.FALSE;
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public void setRanked( Boolean newVal )
	{
		if( myMapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_RANKED ) )
		{
			myMapProperties.put( DSLConstants.KEY_EXTRADEFINITION_RANKED, newVal );
		}
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public List getTargetList()
	{
		if( myMapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_FAULT_STATES ) )
		{
			return (List)(myMapProperties.get(DSLConstants.KEY_EXTRADEFINITION_FAULT_STATES));
		}

		else return null;
	}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public void setTargetList( List newVal )
	{
		if( myMapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_FAULT_STATES ) )
		{
			myMapProperties.put( DSLConstants.KEY_EXTRADEFINITION_FAULT_STATES, newVal );
		}
	}

	/**
		Moved to StandardNodeImpl 041304
		@since 030402
	*/
	//public Integer getDefaultStateIndex(){
	//	if( myMapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_SETASDEFAULT ) ){
	//		if( ((Boolean)(myMapProperties.get( DSLConstants.KEY_EXTRADEFINITION_SETASDEFAULT ))).booleanValue() ){
	//			if( myMapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_DEFAULT_STATE ) ){
	//				return (Integer)(myMapProperties.get(DSLConstants.KEY_EXTRADEFINITION_DEFAULT_STATE));
	//			}
	//		}
	//	}
	//	return null;
	//}

	/**
		Convenience method.
		@author Keith Cascio
		@since 030402
	*/
	public void setDefaultStateIndex( Integer newVal )
	{
		if( myMapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_SETASDEFAULT ) )
		{
			if( myMapProperties.containsKey( DSLConstants.KEY_EXTRADEFINITION_DEFAULT_STATE ) )
			{
				myMapProperties.put( DSLConstants.KEY_EXTRADEFINITION_SETASDEFAULT, Boolean.TRUE );
				myMapProperties.put( DSLConstants.KEY_EXTRADEFINITION_DEFAULT_STATE, newVal );
			}
		}
	}

	public java.util.Map getProperties()
	{
		return myMapProperties;
	}

	public String toString()
	{
		if( isStringifier() ) return getStringifier().variableToString( this );
		else return "DSLNode "+id;
	}

	private final Map myMapProperties;
	private DSLSubmodel myDSLSubmodel;
}
