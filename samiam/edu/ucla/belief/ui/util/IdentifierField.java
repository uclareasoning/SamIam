package edu.ucla.belief.ui.util;

import javax.swing.*;
import javax.swing.text.*;
import java.text.*;

/** @author Keith Cascio */
public class IdentifierField extends JTextField
{
	public IdentifierField( String value, int columns )
	{
		super( value, columns );

		/*
		RestrictedDocument RD = new RestrictedDocument();

		String name = value;
		if( !RD.validateString(name) )
		{
			name = edu.ucla.belief.ui.networkdisplay.NetworkDisplay.STR_NEW_VARIABLE_ID +
				String.valueOf( edu.ucla.belief.ui.NetworkInternalFrame.INT_COUNTER_NEW_VARIABLES++ );
		}

		setDocument( RD );

		super.setText(name);
		*/
	}

	/** @since 020205 */
	public IdentifierField( boolean notify ){
		super( "", (int)5 );
		this.myFlagNotify = notify;
	}

	/*
	public void setText( String in)
	{
		if( validateString( in ) )
		{
			super.setText( in);
		}
	}*/

	protected Document createDefaultModel(){
		return new RestrictedDocument().setJTextComponent( this );//since 20080626
	}

	/** @since 020205 */
	public static boolean isValidID( String in )
	{
		if( in == null) return false;

		int length = in.length();
		if( length == 0 ) return true;

		if( length <= 0 ) return false;
		else if( !Character.isLetter( in.charAt(0) ) ) return false;
		else{
			char charat;
			for( int i=1; i<length; i++){
				charat = in.charAt(i);
				if( (!Character.isLetterOrDigit( charat )) && (charat != '_') ) return false;
			}
		}

		return true;
	}

	protected class RestrictedDocument extends NotifyDocument
	{
		public RestrictedDocument() {}

		public void fireActionPerformed(){
			if( IdentifierField.this.myFlagNotify ) IdentifierField.this.fireActionPerformed();
		}

		/** Will accept empty strings (not null), and valid variable names.*/
		public String validateString( String in )
		{
			//System.out.println( "RestrictedDocument.validateString("+in+")" );
			if(      in        == null ){ return   in; }
			else if( in.length()   < 1 ){ return   in; }
			else if( ! isValidID( in ) ){ return null; }
			else{                         return myLastValidData = in; }
		}

		/** @since 080504 */
		public String getLastValidData(){
			return myLastValidData;
		}

		public String getErrorMessage(){
			return STR_ERROR_MESSAGE;
		}

		private String myLastValidData = STR_EMPTY;
	}

	public static final String STR_ERROR_MESSAGE = "Must begin with a letter, and only contain letters, numbers, and underscores.";

	private boolean myFlagNotify = true;
}
