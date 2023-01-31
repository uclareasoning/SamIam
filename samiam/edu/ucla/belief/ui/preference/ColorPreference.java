package edu.ucla.belief.ui.preference;

import javax.swing.JComponent;
import java.awt.Color;
import java.util.StringTokenizer;

/**
	@author Keith Cascio
	@since 071202
*/
public class ColorPreference extends AbstractPreference
{
	public ColorPreference( String key, String name, Color defaultValue )
	{
		super( key, name, defaultValue );
	}

	public ColorPreference( String key, String name, String valueToParse ) throws Exception
	{
		super( key, name, null );
		parseValue( valueToParse );
	}

	protected JComponent getEditComponentHook()
	{
		if( myColorSwatchLabel == null )
		{
			myColorSwatchLabel = new ColorSwatchLabel( (Color)getValue() );
			myColorSwatchLabel.addActionListener( this );
		}
		return myColorSwatchLabel;
	}

	public Object hookValueClone()
	{
		return new Color( ((Color)myValue).getRGB() );
	}

	public void hookSetEditComponentValue( Object newVal )
	{
		if( (newVal instanceof Color) && (myColorSwatchLabel != null) )
		{
			setListeningEnabled( false );
			myColorSwatchLabel.setValue( (Color) newVal );
			setListeningEnabled( true );
		}
	}

	protected ColorSwatchLabel myColorSwatchLabel = null;

	public Object getCurrentEditedValue()
	{
		return myColorSwatchLabel.clr;
	}

	protected String valueToString()
	{
		Color clr = (Color) myValue;
		return String.valueOf( clr.getRed() ) + "," + String.valueOf( clr.getGreen() ) + "," + String.valueOf( clr.getBlue() );
	}

	public static final String STR_DELIMITERS = ", ";

	public Object parseValue( String strVal ) throws Exception
	{
		StringTokenizer toker = new StringTokenizer( strVal, STR_DELIMITERS );
		if( toker.countTokens() == (int)3 )
		{
			int[] colorvals = new int[ (int)3 ];
			int index = (int)0;
			while( toker.hasMoreTokens() )
			{
				colorvals[index++] = Integer.parseInt( toker.nextToken() );
			}
			Color ret = new Color( colorvals[0], colorvals[1], colorvals[2] );
			myValue = ret;
			return ret;
		}
		else throw new Exception( "ColorPreference.parseValue(): wrong number of tokens." );
	}
}
