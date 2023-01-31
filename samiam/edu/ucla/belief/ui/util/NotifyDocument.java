package edu.ucla.belief.ui.util;

import javax.swing.*;
import javax.swing.text.*;
import java.text.*;

public abstract class NotifyDocument extends PlainDocument
{
	public static final String
	  STR_SUPPRESS_ERRORS           = "suppress errors",
	  STR_EMPTY                     = "",
	  STR_ERROR_MESSAGE1            = "Invalid value: \"",
	  STR_ERROR_MESSAGE2            = "\".  ";

	abstract public void   fireActionPerformed();
	abstract public String    getLastValidData();

	public NotifyDocument(){}

	public String validateString( String in ){
		return in;
	}

	protected String getErrorMessage(){
		return STR_EMPTY;
	}

	/** @since 20090304 */
	public static int search( Object[] arr, Object key ){
		if( arr == null ){ return -1; }
		for( int i=0; i<arr.length; i++ ){ if( arr[i] == key ){ return i; } }
		return -1;
	}

	/** @since 20040805 */
	private void panic() throws BadLocationException{
		remove( 0, getLength() );
		insertString( 0, getLastValidData(), new Object[]{ STR_SUPPRESS_ERRORS } );
	}

	public final void insertString( int offs, String str, AttributeSet a ) throws BadLocationException{
		insertString( offs, str, (Object[]) null );
	}

	public final void insertString( int offs, String str, Object[] flags ) throws BadLocationException{
	  //System.out.println( "NotifyDocument.insertString('"+str+"')" );
		String currentText    = super.getText( 0, getLength() );
		String beforeOffset   = currentText.substring( 0, offs );
		String afterOffset    = currentText.substring( offs, currentText.length() );
		String proposedResult = beforeOffset + str + afterOffset;
		String normal         = null;

		if( (normal = validateString( proposedResult )) != null ){
			int delta = str.length();
			if( offs > 0 ){ delta += adjustment( proposedResult, normal ); }
			setTextPersistCaret( normal, delta );
		}
		else if( search( flags, STR_SUPPRESS_ERRORS ) >= 0 ){
			remove( 0, getLength() );
		}else{
			showErrorDialog( proposedResult );
			panic();
		}
	}

	public final void remove( int offs, int len ) throws BadLocationException
	{
		if( myFlagReplacing ){ super .remove( offs, len ); return; }

		String currentText    = super.getText( 0, getLength() );
	  //System.out.println( "NotifyDocument.remove('"+currentText.substring(offs,len)+"')" );
		String beforeOffset   = currentText.substring( 0, offs );
		String afterOffset    = currentText.substring( len + offs,currentText.length() );
		String proposedResult = beforeOffset + afterOffset;
		String normal         = null;

		if( ( ( normal = validateString( proposedResult )) != null) || (normal = proposedResult).equals( currentText ) ){
			if( normal ==                proposedResult ){ super.remove( offs, len ); fireActionPerformed(); return; }
			else{ setTextPersistCaret( normal, - len + adjustment( proposedResult, normal ) ); }
		}
		else{
			showErrorDialog( proposedResult );
			panic();
		}
	}

	/** @since 20080328 */
	public int adjustment( String proposedResult, String normal ){
		int nl = normal.length(), prl = proposedResult.length();
		if(      nl  > prl ){ return 1; }
	  //else if( nl == prl ){ return 0; }
	  //else{                 return nl - prl; }
		else{ return 0; }
	}

	public final void showErrorDialog( String proposedResult ){
		this.showErrorDialog( proposedResult, getErrorMessage() );
	}

	/** @since 20050608 */
	public final void showErrorDialog( String proposedResult, String errorMessage )
	{
		JOptionPane.showMessageDialog(	null,
						STR_ERROR_MESSAGE1 + proposedResult + STR_ERROR_MESSAGE2 + errorMessage,
						"Input error",
						JOptionPane.ERROR_MESSAGE);
	}

	/** @since 20080328 */
	private void setTextPersistCaret( String normal, int delta ){
		int caret = -1;
		try{
			if( myJTextComponent != null ){ caret = myJTextComponent.getCaretPosition() + delta; }
			super      .remove( 0, getLength() );
			super.insertString( 0,     normal, (AttributeSet) null );
			fireActionPerformed();
		}catch( Exception thrown ){
			System.err.println( "warning: NotifyDocument.setTextPersistCaret() caught " + thrown );
		}
		try{
			if( 0 <= caret && caret <= getLength() ){ myJTextComponent.setCaretPosition( caret ); }
		}catch( Exception thrown ){
			if( Util.DEBUG_VERBOSE ){ System.err.println( "warning: NotifyDocument.setTextPersistCaret() caught " + thrown ); }
		}
	}

	/** @since 20080328 */
	public NotifyDocument setJTextComponent( JTextComponent jtc ){
		this.myJTextComponent = jtc;
		return this;
	}

	/** @since 20080328 */
	public NotifyDocument setReplacing( boolean flag ){
		this.myFlagReplacing = flag;
		return this;
	}

	private JTextComponent myJTextComponent;
	private boolean        myFlagReplacing = false;
}
