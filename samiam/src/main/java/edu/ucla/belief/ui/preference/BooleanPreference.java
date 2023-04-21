package edu.ucla.belief.ui.preference;

import javax.swing.*;

/**
	@author Keith Cascio
	@since 071202
*/
public class BooleanPreference extends AbstractPreference
{
	public BooleanPreference( String key, String name, Boolean defaultValue )
	{
		super( key, name, defaultValue );
	}

	public BooleanPreference( String key, String name, String valueToParse ) throws Exception
	{
		super( key, name, null );
		parseValue( valueToParse );
	}

	protected JComponent getEditComponentHook()
	{
		if( myJCheckBox == null )
		{
			myJCheckBox = new JCheckBox( "", ((Boolean)getValue()).booleanValue() );
			myJCheckBox.addActionListener( this );
		}
		return myJCheckBox;
	}

	public Object hookValueClone()
	{
		return new Boolean( ((Boolean)myValue).booleanValue() );
	}

	public void hookSetEditComponentValue( Object newVal )
	{
		if( (newVal instanceof Boolean) && (myJCheckBox != null) )
		{
			setListeningEnabled( false );
			myJCheckBox.setSelected( ((Boolean) newVal).booleanValue() );
			setListeningEnabled( true );
		}
	}

	protected JCheckBox myJCheckBox = null;

	public Object getCurrentEditedValue()
	{
		return new Boolean( myJCheckBox.isSelected() );
	}

	protected String valueToString()
	{
		return myValue.toString();
	}

	public Object parseValue( String strVal ) throws Exception
	{
		Boolean ret = Boolean.valueOf( strVal );
		myValue = ret;
		return ret;
	}
}
