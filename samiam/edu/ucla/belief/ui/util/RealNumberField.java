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
public class RealNumberField extends JTextField
{
	public static final String STR_MESSAGE_RANGE = "Number must be in range ";
	public static final String STR_ERROR_MESSAGE = "Please use only the digits 0-9 and \".\" to create a real number";

	protected NumberFormat myFormat;
	protected double myMaxValue = 1.0; // just default value to prevent initial error
	protected double myMinValue = 0; //  just default value to prevent initial error
	private String myErrorMessageRange;
	private InputVerifier myInputVerifier;
	private RealNumberDocument myRealNumberDocument;

	public RealNumberField( double value, int columns ){
		super( columns );
		myFormat = NumberFormat.getNumberInstance();
		myFormat.setParseIntegerOnly( false );
		myFormat.setGroupingUsed( false );

		createRangeMessage();

		setValue(value);
	}

	/** @since 20040810 */
	public RealNumberField( double value, int columns, double newFloor, double newCeiling )
	{
		this( value, columns );
		this.setBoundsInclusive( newFloor, newCeiling );
	}

	/** @since 20091208 */
	public double getMaxValue(){ return myMaxValue; }
	/** @since 20091208 */
	public double getMinValue(){ return myMinValue; }

	/** @since 20020806 */
	public void setMaxValue( double newMax ){
		myMaxValue = newMax;
		createRangeMessage();
	}

	/** @since 20091208 */
	public void setMinValue( double newMin ){
		myMinValue = newMin;
		initVerifier();
		createRangeMessage();
	}

	/** @since 20040805 */
	public void setBoundsInclusive( double newMin, double newMax )
	{
		myMinValue = newMin;
		myMaxValue = newMax;
		initVerifier();
		createRangeMessage();
	}

	/** @since 060805 */
	private void initVerifier(){
		InputVerifier verifier = getMyInputVerifier();
		//if( myMinValue < 2 ) verifier = (InputVerifier)null;
		//else verifier = getMyInputVerifier();
		this.setInputVerifier( verifier );
	}

	/** @since 060805 */
	private InputVerifier getMyInputVerifier(){
		//System.out.println( "Creating InputVerifier" );
		if( myInputVerifier == null ){
			myInputVerifier = new InputVerifier(){
				public boolean verify( JComponent input ){
					boolean ret = RealNumberField.this.validateString( RealNumberField.this.getText() );
					//System.out.println( "verify? " + ret );
					if( !ret ) RealNumberField.this.requestFocusInWindow();
					return ret;
				}
			};
		}
		return myInputVerifier;
	}

	/** @since 060805 */
	public boolean validateString( String in ) //called as a final check
	{
		String errmsg = STR_ERROR_MESSAGE;

		boolean ret = false;
		try{
			if( in == null ) return ret = false;
			else if( in.length() < 1 ) return ret = true;
			else{
				int len = in.length();
				for( int i=0; i<len; i++ ){
					if( !Character.isDigit( in.charAt(i) ) && (in.charAt(i) !='.'))
                        return ret = false;
				}
                if (len == 1) { // for the case where we just have a .
                    if (in.charAt(0) == '.')
                        return ret = true;
                }
			}

			double proposed = this.myFormat.parse( in ).doubleValue();
            //System.out.println(proposed + " " + myMinValue + " " + myMaxValue);
			if( proposed < myMinValue ) errmsg = myErrorMessageRange;
			else if( myMaxValue < proposed ) errmsg = myErrorMessageRange;
			else {
                //System.out.println("threshold is " + proposed);
                return ret = true;
            }
		}catch( ParseException e ){
			ret = false;
			errmsg = "Unrecognized number format.";
		}finally{
			if( !ret ) {
                //System.out.println("failure!");
                myRealNumberDocument.showErrorDialog( in, errmsg );
                //System.out.println("real number document");
            }
		}
		return ret;
	}

	/** @since 080504 */
	private void createRangeMessage()
	{
		myErrorMessageRange = STR_MESSAGE_RANGE + "[ " + Double.toString(myMinValue) + ", " + Double.toString(myMaxValue) + " ].";
	}

	public double getValue(){
		double retVal = 0;
		String text = getText();
		try{
			if( text.length() > 0 ){
				retVal = myFormat.parse( text ).doubleValue();
			}
		}
		catch( ParseException e ){
			// This should never happen because insertString allows
			// only properly formatted data to get in the field.
			System.err.println("ERROR: Parse error in RealNumberField");
			System.err.println( e.getMessage() );
		}

		return retVal;
	}

	public void setValue( double value ){
		setText( myFormat.format( value ) );
	}

	protected Document createDefaultModel(){
		if( myRealNumberDocument == null ){ myRealNumberDocument = new RealNumberDocument();
		    myRealNumberDocument.setJTextComponent( this );//since 20080626
		}
		return myRealNumberDocument;
	}

	protected class RealNumberDocument extends NotifyDocument
	{
		public RealNumberDocument() {}

		public void fireActionPerformed(){
			try{
				RealNumberField.this.fireActionPerformed();
			}catch( Exception e ){
				if( Util.DEBUG_VERBOSE )
				{
					Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
					e.printStackTrace();
				}
			}
		}

		/** Will accept empty strings (not null), and valid variable names.*/
		public String validateString( String in ) //this is called for every char
		{
			errmsg = STR_ERROR_MESSAGE;

			if(      in      == null ){ return in; }
			else if( in.length() < 1 ){ return in; }
			else{
				int len = in.length();
				for( int i=0; i<len; i++){
					if( (! Character.isDigit( in.charAt(i) )) && (in.charAt(i) != '.') ){ return null; }
                    
				}
                 if (len == 1) { // for the case where we just have a .
                     //System.out.println(in.charAt(0));
                    if (in.charAt(0) == '.')
                        return in;
                }
			}

			try
			{
               
				double proposed = RealNumberField.this.myFormat.parse( in ).doubleValue();

				if( (proposed < RealNumberField.this.myMinValue) ){
                    errmsg = RealNumberField.this.myErrorMessageRange;
                    //System.out.println("thing is " + proposed);
                    //System.out.println(RealNumberField.this.myMinValue);
                }
				else if( RealNumberField.this.myMaxValue < proposed ){
                    errmsg = RealNumberField.this.myErrorMessageRange;
                    //System.out.println("thing is " + proposed);
                    //System.out.println(RealNumberField.this.myMaxValue);
                }
				else{
                    //System.out.println("proposed is " + in);
                    return myLastValidData = in;

                }
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
