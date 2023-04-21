package edu.ucla.belief.ui.preference;

import edu.ucla.belief.ui.util.WholeNumberField;

import javax.swing.*;
import java.awt.*;
import java.util.StringTokenizer;

/**
	@author Keith Cascio
	@since 071202
*/
public class DimensionPreference extends AbstractPreference
{
	public static int INT_MAX_ALLOWED_VALUE = (int)10000;
	public static int INT_WIDTH_TEXTFIELD = (int)32;

	public DimensionPreference( String key, String name, Dimension defaultValue )
	{
		super( key, name, defaultValue );
	}

	public DimensionPreference( String key, String name, String valueToParse ) throws Exception
	{
		super( key, name, null );
		parseValue( valueToParse );
	}

	protected JComponent getEditComponentHook()
	{
		if( myGUI == null )
		{
			myGUI = makeGUI();
		}
		return myGUI;
	}

	/** @since 021605 */
	public WholeNumberField getWidthField(){
		return myWidthField;
	}

	/** @since 021605 */
	public WholeNumberField getHeightField(){
		return myHeightField;
	}

	protected JComponent myGUI = null;
	protected WholeNumberField myWidthField = null;
	protected WholeNumberField myHeightField = null;
	//protected JLabel myLabelWidth = null;
	protected JLabel myLabelHeight = null;

	protected JComponent makeGUI()
	{
		//myLabelWidth = new JLabel( "(width) " );
		//myLabelHeight = new JLabel( "(height) " );
		myLabelHeight = new JLabel( " x " );

		Dimension dim = (Dimension) myValue;
		myWidthField = new WholeNumberField( dim.width, 0 );
		myHeightField = new WholeNumberField( dim.height, 0 );

		Dimension guiTextfieldDimension = myWidthField.getPreferredSize();
		guiTextfieldDimension.width = INT_WIDTH_TEXTFIELD;
		myWidthField.setPreferredSize( guiTextfieldDimension );
		myHeightField.setPreferredSize( guiTextfieldDimension );

		myWidthField.setMaxValue( INT_MAX_ALLOWED_VALUE );
		myHeightField.setMaxValue( INT_MAX_ALLOWED_VALUE );

		myWidthField.addActionListener( this );
		myHeightField.addActionListener( this );

		JPanel pnlMain = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		pnlMain.setLayout( gridbag );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;

		c.weightx = (double)0;
		c.gridwidth = 1;
		//pnlMain.add( myLabelWidth, c );

		c.weightx = (double)1;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMain.add( myWidthField, c );

		c.weightx = (double)0;
		c.gridwidth = 1;
		pnlMain.add( myLabelHeight, c );

		c.weightx = (double)1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlMain.add( myHeightField, c );

		return pnlMain;
	}

	public Object getCurrentEditedValue()
	{
		return new Dimension( myWidthField.getValue(), myHeightField.getValue() );
	}

	public void hookSetEditComponentValue( Object newVal )
	{
		//System.out.println( "DimensionPreference.hookSetEditComponentValue("+newVal+")" );
		if( (newVal instanceof Dimension) && (myWidthField != null) )
		{
			setListeningEnabled( false );
			myWidthField.setValue( ((Dimension) newVal).width );
			myHeightField.setValue( ((Dimension) newVal).height );
			setListeningEnabled( true );
		}
	}

	public Object hookValueClone()
	{
		return new Dimension( ((Dimension)myValue) );
	}

	protected String valueToString()
	{
		Dimension dim = (Dimension) myValue;
		return String.valueOf( dim.width ) + "," + String.valueOf( dim.height );
	}

	public static final String STR_DELIMITERS = ", ";

	public Object parseValue( String strVal ) throws Exception
	{
		StringTokenizer toker = new StringTokenizer( strVal, STR_DELIMITERS );
		if( toker.countTokens() == (int)2 )
		{
			int[] vals = new int[ (int)2 ];
			int index = (int)0;
			while( toker.hasMoreTokens() )
			{
				vals[index++] = Integer.parseInt( toker.nextToken() );
			}
			Dimension ret = new Dimension( vals[0], vals[1] );
			myValue = ret;
			return ret;
		}
		else throw new Exception( "DimensionPreference.parseValue(): wrong number of tokens." );
	}
}
