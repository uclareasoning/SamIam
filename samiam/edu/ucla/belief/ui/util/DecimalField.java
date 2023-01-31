package edu.ucla.belief.ui.util;

import javax.swing.*;
import javax.swing.text.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

/** Originally from Sun Java Tutorial.<br />
    see: http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#46763 */
public class DecimalField extends JTextField
{
	private DecimalDocument myDD;
	private NumberFormat    format;
	private double
	  myFloor                  = -Double.MAX_VALUE,
	  myCeiling                =  Double.MAX_VALUE;
	private String
	  myFloorString            = "0.00000000000000000000",
	  myErrorMessageRange;

	public static final String
	  STR_MESSAGE_RANGE        = "Number must be in range ",
	  STR_ERROR_MESSAGE        = "Please enter a valid decimal number.",
	  STR_POINT                =   ".",
	  STR_EMPTY                = "",
	  STR_MINUS                = "-",
	  STR_MINUS_POINT          =  "-.",
	  STR_MINUS_ZERO           = "-0",
	  STR_MINUS_ZERO_POINT     = "-0.",
	  STR_ZERO_POINT           =  "0.";
	public static final char
	  CHAR_EXPONENT_LOWER      = 'e',
	  CHAR_EXPONENT_UPPER      = 'E',
	  CHAR_POINT               = '.',
	  CHAR_PLUS                = '+',
	  CHAR_MINUS               = '-';
	public static final double
	  DOUBLE_TEN               = 10.0,
	  DOUBLE_ZERO              =  0.0,
	  DOUBLE_ONE               =  1.0,
	  DOUBLE_ONE_TENTH         =  0.1;
	public static final Matcher
	  MATCHER_EXP_TRUNC        = Pattern.compile( "e-?$", Pattern.CASE_INSENSITIVE ).matcher( "" ),
	  MATCHER_EXPONENTIAL      = Pattern.compile( "^(-?([0-9]+|[.])[.]?((?<=[.])[0-9]*)?)(e([-+]?([0-9]+)))?$", Pattern.CASE_INSENSITIVE ).matcher( "" );
	// groups:
	// 0. entire value
	// 1. decimal part without exponent
	// 2. integer part with possible decimal point
	// 3. fractional part
	// 4. 'e' and exponent
	// 5. exponent
	// 6. exponent absolute value

	public DecimalField( double value, int columns ){
		super( columns );

		format = NumberFormat.getNumberInstance();
		format      .setParseIntegerOnly( false );
		format .setMaximumFractionDigits( 0x100 );

		DecimalDocument doc = myDD = new DecimalDocument( format );
		doc.addFormatForceValid( STR_EMPTY            );
		doc.addFormatForceValid( STR_POINT            );
		doc.addFormatForceValid( STR_MINUS            );
		doc.addFormatForceValid( STR_MINUS_POINT      );
		doc.addFormatForceValid( STR_MINUS_ZERO       );
		doc.addFormatForceValid( STR_MINUS_ZERO_POINT );
		doc.addFormatForceValid( STR_ZERO_POINT       );
		setDocument( doc.setJTextComponent( DecimalField.this ) );

		createRangeMessage();

		setValue( value );
	}

	/** @since 20040809 */
	public DecimalField( double value, int columns, double newFloor, double newCeiling )
	{
		this( value, columns );
		setBoundsInclusive( newFloor, newCeiling );
	}

	/** @since 20040805 */
	public void setBoundsInclusive( double newFloor, double newCeiling )
	{
		myFloor       = newFloor;
		myFloorString = format.format( myFloor );
		myCeiling     = newCeiling;
		createRangeMessage();
	}

	/** @since 20091208 */
	public double   getFloor(){ return myFloor;   }
	/** @since 20091208 */
	public double getCeiling(){ return myCeiling; }

	/** @since 20040805 */
	private void createRangeMessage(){
		myErrorMessageRange = STR_MESSAGE_RANGE + "[ " + Double.toString(myFloor) + ", " + Double.toString(myCeiling) + " ].";
	}

	public double getValue()
	{
		double retVal  = 0.0;
		String theText = getText();
		try{
			if( (! theText.equals( STR_EMPTY )) && (! theText.equals( STR_POINT )) ){
				retVal = format.parse( theText ).doubleValue();
			}
		}catch( ParseException thrown ){
			// This should never happen because insertString allows
			// only properly formatted data to get in the field.
			System.err.println( "ERROR: Parse error in DecimalField \n" + thrown.getMessage() );
		}
		return retVal;
	}

	public void setValue( double value ){
		setText( format.format( value ) );
	}

	/** @since 20080328 */
	public void replaceSelection( String content ){
		myDD.setReplacing( true );
		try{
			super.replaceSelection( content );
		}finally{
			myDD.setReplacing( false );
		}
	}

	//protected Document createDefaultModel()
	//{
	//	return new DecimalDocument();
	//}

	protected class DecimalDocument extends FormattedDocument
	{
		public DecimalDocument( Format format ){
			super( format );
		}

		public void fireActionPerformed(){
			DecimalField.this.fireActionPerformed();
		}

		public String secondaryValidation( String in )
		{
			//System.out.println( "DecimalDocument.secondaryValidation("+in+")" );
			errmsg = STR_ERROR_MESSAGE;

			char tempChar;
			for( int i=0; i<in.length(); i++ ){
				tempChar = in.charAt(i);
				if( !(Character.isDigit( tempChar )                       ||
				                        (tempChar == CHAR_POINT)          ||
				                        (tempChar == CHAR_MINUS)          ||
				                        (tempChar == CHAR_EXPONENT_LOWER) ||
				                        (tempChar == CHAR_EXPONENT_UPPER) ||
				                        (tempChar == CHAR_PLUS)) ){
					return null;
				}
			}

			double proposed;
			try{
				proposed = Double.parseDouble( in );

				if( proposed < DecimalField.this.myFloor ){
					if(       myFloorString.startsWith( in ) ){ return in; }
					else if( (proposed==DOUBLE_ZERO) && (DOUBLE_ZERO <= DecimalField.this.myFloor) && (DecimalField.this.myFloor < DOUBLE_ONE_TENTH) ){ return in; }
					else{ errmsg = DecimalField.this.myErrorMessageRange; }
				}
				else if( proposed > DecimalField.this.myCeiling ){ errmsg = DecimalField.this.myErrorMessageRange; }
				else{ return in; }
			}
			catch( Exception thrown ){
				errmsg = "Unrecognized number format.";
			}

			try{
				if( MATCHER_EXP_TRUNC.reset( in ).find() ){ in += "0"; }
				Matcher m = MATCHER_EXPONENTIAL.reset( in );
			  //System.out.println( in + "? " + m.matches() );
				if( m.matches() ){
					double base     = Double.parseDouble( m.group(1) );
					if( DecimalField.this.myFloor >= DOUBLE_ZERO ){ base = Math.abs( base ); }
					String sexp     = m.group(5);
				  //System.out.println( "    " + m.group(1) + " ... " + m.group(5) );
					double exponent = ((sexp == null) || (sexp.length() < 1)) ? 0.0 : Double.parseDouble( sexp );
					int    spins    = 0;

					while( (spins++ < 0x1000) && ( (proposed = propose( base, exponent )) < DecimalField.this.myFloor   ) ){
						++exponent;
					}
					while( (spins++ < 0x2000) && ( (proposed = propose( base, exponent )) > DecimalField.this.myCeiling ) ){
						--exponent;
					}
					return in = toString( base, exponent, sexp );
				}
			}catch( Exception thrown ){
				errmsg = thrown.toString();
			}

			return null;
		}

		/** @since 20080328 */
		public double propose( double base, double exponent ){
			return base * Math.pow( DOUBLE_TEN, exponent );
		}

		/** @since 20080328 */
		public String toString( double base, double exponent, String sexpin ){
		 //System.out.println( "toString( "+base+", "+exponent+" )" );
			while( (exponent < DOUBLE_ZERO) && (base > DOUBLE_TEN) ){
				++exponent;
				base = base / DOUBLE_TEN;
			}
			while( (exponent > DOUBLE_ZERO) && (base < DOUBLE_ONE) ){
				--exponent;
				base *= DOUBLE_TEN;
			}
			String sbase = toString( base );
			String sexp  = ((exponent == DOUBLE_ZERO) && (sexpin == null)) ? "" : ("e" + toString( exponent ));
			return sbase + sexp;
		}

		/** @since 20080328 */
		public String toString(       double dub ){
			String    ret = Double.toString( dub ).toLowerCase();
			if(       ret  .endsWith(   ".0" ) ){ ret = ret.substring( 0, ret.length() - 2 ); }
			return    ret.replaceAll( "[.]0[eE]", "e" );
		}

		public String getErrorMessage(){
			return errmsg;
		}

		protected  String errmsg = STR_ERROR_MESSAGE;
	}
}
