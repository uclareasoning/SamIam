package edu.ucla.belief.ui.util;

import edu.ucla.belief.ui.UI;

import javax.swing.*;
import javax.swing.text.*;
import java.text.*;

/*
	We loosely based this file on one of the Sun Java Tutorials.
	@author David Allen
	@author Keith Cascio
	@since The Beginning of Known Time
*/
public class WholeNumberField extends JTextField
{
	public static final String STR_MESSAGE_RANGE = "Number must be in range ";
	public static final String STR_ERROR_MESSAGE = "Please use only digits 0-9 and the minus sign.";

	protected NumberFormat myFormat;
	protected int myMaxValue = Integer.MAX_VALUE;
	protected int myMinValue = (int)0;
	private String myErrorMessageRange;
	private InputVerifier myInputVerifier;
	private WholeNumberDocument myWholeNumberDocument;

	public WholeNumberField( int value, int columns ){
		super( columns );
		myFormat = NumberFormat.getNumberInstance();
		myFormat.setParseIntegerOnly( true );
		myFormat.setGroupingUsed( false );

		createRangeMessage();

		setValue(value);
	}

	/** @since 20040810 */
	public WholeNumberField( int value, int columns, int newFloor, int newCeiling )
	{
		this( value, columns );
		this.setBoundsInclusive( newFloor, newCeiling );
	}

	/** @since 20091208 */
	public int getMaxValue(){ return myMaxValue; }
	/** @since 20091208 */
	public int getMinValue(){ return myMinValue; }

	/** @since 20020806 */
	public void setMaxValue( int newMax ){
		myMaxValue = newMax;
		createRangeMessage();
	}

	/** @since 20091208 */
	public void setMinValue( int newMin ){
		myMinValue = newMin;
		initVerifier();
		createRangeMessage();
	}

	/** @since 20040805 */
	public void setBoundsInclusive( int newMin, int newMax )
	{
		myMinValue = newMin;
		myMaxValue = newMax;
		initVerifier();
		createRangeMessage();
	}

	/** @since 060805 */
	private void initVerifier(){
		InputVerifier verifier;
		if( myMinValue < 2 ) verifier = (InputVerifier)null;
		else verifier = getMyInputVerifier();
		this.setInputVerifier( verifier );
	}

	/** @since 060805 */
	private InputVerifier getMyInputVerifier(){
		//System.out.println( "Creating InputVerifier" );
		if( myInputVerifier == null ){
			myInputVerifier = new InputVerifier(){
				public boolean verify( JComponent input ){
					boolean ret = WholeNumberField.this.validateString( WholeNumberField.this.getText() );
					//System.out.println( "verify? " + ret );
					if( !ret ) WholeNumberField.this.requestFocusInWindow();
					return ret;
				}
			};
		}
		return myInputVerifier;
	}

	/** @since 060805 */
	public boolean validateString( String in )
	{
		String errmsg = STR_ERROR_MESSAGE;

		boolean ret = false;
		try{
			if( in == null ) return ret = false;
			else if( in.length() < 1 ) return ret = true;
			else{
				int len = in.length();
				for( int i=0; i<len; i++ ){
					if( !Character.isDigit( in.charAt(i) ) ) return ret = false;
				}
			}

			int proposed = this.myFormat.parse( in ).intValue();

			if( proposed < myMinValue ) errmsg = myErrorMessageRange;
			else if( myMaxValue < proposed ) errmsg = myErrorMessageRange;
			else return ret = true;
		}catch( ParseException e ){
			ret = false;
			errmsg = "Unrecognized number format.";
		}finally{
			if( !ret ) myWholeNumberDocument.showErrorDialog( in, errmsg );
		}
		return ret;
	}

	/** @since 080504 */
	private void createRangeMessage()
	{
		myErrorMessageRange = STR_MESSAGE_RANGE + "[ " + Integer.toString(myMinValue) + ", " + Integer.toString(myMaxValue) + " ].";
	}

	public int getValue(){
		int retVal = 0;
		String text = getText();
		try{
			if( text.length() > 0 ){
				retVal = myFormat.parse( text ).intValue();
			}
		}
		catch( ParseException e ){
			// This should never happen because insertString allows
			// only properly formatted data to get in the field.
			System.err.println("ERROR: Parse error in WholeNumberField");
			System.err.println( e.getMessage() );
		}

		return retVal;
	}

	public void setValue( int value ){
		setText( myFormat.format( value ) );
	}

	protected Document createDefaultModel(){
		if( myWholeNumberDocument == null ){ myWholeNumberDocument = new WholeNumberDocument();
		    myWholeNumberDocument.setJTextComponent( this );//since 20080626
		}
		return myWholeNumberDocument;
	}

	protected class WholeNumberDocument extends NotifyDocument
	{
		public WholeNumberDocument() {}

		public void fireActionPerformed(){
			try{
				WholeNumberField.this.fireActionPerformed();
			}catch( Exception e ){
				if( Util.DEBUG_VERBOSE )
				{
					Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
					e.printStackTrace();
				}
			}
		}

		/** Will accept empty strings (not null), and valid variable names.*/
		public String validateString( String in )
		{
			//System.out.println( "WholeNumberDocument.validateString("+in+")" );
			errmsg = STR_ERROR_MESSAGE;

			if(      in      == null ){ return in; }
			else if( in.length() < 1 ){ return in; }
			else{
				int len = in.length();
				for( int i=0; i<len; i++){
					if( (! Character.isDigit( in.charAt(i) )) && (in.charAt(i) != '-') ){ return null; }
				}
			}

			try
			{
				int proposed = WholeNumberField.this.myFormat.parse( in ).intValue();

				if( (WholeNumberField.this.myMinValue < 2) && (proposed < WholeNumberField.this.myMinValue) ){ errmsg = WholeNumberField.this.myErrorMessageRange; }
				else if( WholeNumberField.this.myMaxValue < proposed ){ errmsg = WholeNumberField.this.myErrorMessageRange; }
				else{ return myLastValidData = in; }
			}
			catch( ParseException e ){
				errmsg = "Unrecognized number format.";
			}

			return null;
		}

		/** @since 080504 */
		public String getLastValidData(){
			return myLastValidData;
		}

		public String getErrorMessage(){
			return errmsg;
		}

		private   String myLastValidData = STR_EMPTY;
		protected String errmsg          = STR_ERROR_MESSAGE;
	}
}
