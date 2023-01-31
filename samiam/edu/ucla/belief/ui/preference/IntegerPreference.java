package edu.ucla.belief.ui.preference;

import edu.ucla.belief.ui.util.WholeNumberField;

import javax.swing.*;

/**
	@author Keith Cascio
	@since 071202
*/
public class IntegerPreference extends AbstractPreference
{
	public IntegerPreference( String key, String name, Integer defaultValue )
	{
		super( key, name, defaultValue );
	}

	/** @since 080404 */
	public IntegerPreference( String key, String name, Integer defaultValue, int floor, int ceiling )
	{
		super( key, name, defaultValue );
		setBounds( floor, ceiling );
	}

	public IntegerPreference( String key, String name, String valueToParse ) throws Exception
	{
		super( key, name, null );
		parseValue( valueToParse );
	}

	/** @since 080404 */
	public void setBounds( int floor, int ceiling )
	{
		myFlagBounded = true;
		myFloor = floor;
		myCeiling = ceiling;
		if( myWholeNumberField != null ) myWholeNumberField.setBoundsInclusive( myFloor, myCeiling );
	}

	protected JComponent getEditComponentHook()
	{
		if( myWholeNumberField == null )
		{
			myWholeNumberField = new WholeNumberField( ((Integer)getValue()).intValue(),0 );
			if( myFlagBounded ) myWholeNumberField.setBoundsInclusive( myFloor, myCeiling );
			myWholeNumberField.addActionListener( this );
		}
		return myWholeNumberField;
	}

	public void hookSetEditComponentValue( Object newVal )
	{
		if( (newVal instanceof Number) && (myWholeNumberField != null) )
		{
			setListeningEnabled( false );
			myWholeNumberField.setValue( ((Number)newVal).intValue() );
			setListeningEnabled( true );
		}
	}

	public Object hookValueClone()
	{
		return new Integer( ((Number)myValue).intValue() );
	}

	protected WholeNumberField myWholeNumberField = null;

	public Object getCurrentEditedValue()
	{
		int edited = myWholeNumberField.getValue();
		if( myFlagBounded ){
			if( edited < myFloor || myCeiling < edited ) return this.getValue();
		}
		return new Integer( edited );
	}

	protected String valueToString()
	{
		return myValue.toString();
	}

	public Object parseValue( String strVal ) throws Exception
	{
		Integer ret = Integer.valueOf( strVal );
		int val = ret.intValue();
		if( myFlagBounded ){
			if( val < myFloor || myCeiling < val ) throw new Exception( "Please enter a value in the range [ "+Integer.toString(myFloor)+", "+Integer.toString(myCeiling)+" ]" );
		}
		myValue = ret;
		return ret;
	}

	private boolean myFlagBounded = false;
	private int myFloor;
	private int myCeiling;
}
