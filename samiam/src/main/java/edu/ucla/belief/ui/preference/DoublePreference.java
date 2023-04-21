package edu.ucla.belief.ui.preference;

import edu.ucla.belief.ui.util.DecimalField;

import javax.swing.*;

/**
	@author Keith Cascio
	@since 071202
*/
public class DoublePreference extends AbstractPreference
{
	public DoublePreference( String key, String name, Double defaultValue )
	{
		super( key, name, defaultValue );
	}

	/** @since 080404 */
	public DoublePreference( String key, String name, Double defaultValue, double floor, double ceiling )
	{
		super( key, name, defaultValue );
		setBounds( floor, ceiling );
	}

	public DoublePreference( String key, String name, String valueToParse ) throws Exception
	{
		super( key, name, null );
		parseValue( valueToParse );
	}

	/** @since 080404 */
	public void setBounds( double floor, double ceiling )
	{
		myFlagBounded = true;
		myFloor = floor;
		myCeiling = ceiling;
		if( myDecimalField != null ) myDecimalField.setBoundsInclusive( myFloor, myCeiling );
	}

	protected JComponent getEditComponentHook()
	{
		if( myDecimalField == null )
		{
			myDecimalField = new DecimalField( ((Double)getValue()).doubleValue(),0 );
			if( myFlagBounded ) myDecimalField.setBoundsInclusive( myFloor, myCeiling );
			myDecimalField.addActionListener( this );
		}
		return myDecimalField;
	}

	public void hookSetEditComponentValue( Object newVal )
	{
		if( (newVal instanceof Number) && (myDecimalField != null) )
		{
			setListeningEnabled( false );
			myDecimalField.setValue( ((Number) newVal).doubleValue() );
			setListeningEnabled( true );
		}
	}

	public Object hookValueClone()
	{
		return new Double( ((Number)myValue).doubleValue() );
	}

	protected DecimalField myDecimalField = null;

	public Object getCurrentEditedValue()
	{
		double edited = myDecimalField.getValue();
		if( myFlagBounded ){
			if( edited < myFloor || myCeiling < edited ) return this.getValue();
		}
		return new Double( edited );
	}

	protected String valueToString()
	{
		return myValue.toString();
	}

	public Object parseValue( String strVal ) throws Exception
	{
		Double ret = Double.valueOf( strVal );
		double val = ret.doubleValue();
		if( myFlagBounded ){
			if( val < myFloor || myCeiling < val ) throw new Exception( "Please enter a value in the range [ "+Double.toString(myFloor)+", "+Double.toString(myCeiling)+" ]" );
		}
		myValue = ret;
		return ret;
	}

	private boolean myFlagBounded = false;
	private double myFloor;
	private double myCeiling;
}
