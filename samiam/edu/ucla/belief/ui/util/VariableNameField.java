package edu.ucla.belief.ui.util;


import javax.swing.*;
import javax.swing.text.*;
import java.text.*;

import edu.ucla.belief.*;


public class VariableNameField extends JTextField {
    BeliefNetwork beliefNet;

    public VariableNameField(String value, int columns, BeliefNetwork bn) {
        super(columns);

	String name = value;

	if( bn == null) {
	    throw new IllegalArgumentException( "VariableNameField requires a valid BeliefNetwork.");
	}
	if( value == null) {
	    throw new IllegalArgumentException( "VariableNameField requires a valid initial name.");
	}

	if( !validateString(name, bn)) {
	    for( int i=1;;i++) {
		if( validateString("Variable"+i, bn)) {
		    name = "Variable"+i;
		    break;
		}
	    }
	}

	beliefNet = bn;
        setDocument(new RestrictedDocument( beliefNet));
        super.setText(name);
    }

    public String getText() {
	String ret = super.getText();
	if( ret.length() <= 0) {
	    for( int i=1; ;i++) {
		ret = "v" + i;
		if( validateString( ret, beliefNet)) {
		    return ret;
		}
	    }
	}
	return ret;
    }

    public void setText( String in) {
	if( validateString( in, beliefNet)) {
	    super.setText( in);
	}
    }

    /** Will accept empty strings (not null), and valid variable names.*/
    static public boolean validateString( String in, BeliefNetwork bn) {
	if( in == null) {
	    return false;
	}
	else if( in.length() <= 0) {
	    return false;
	}
	else if( !Character.isLetter( in.charAt(0))) {
	    return false;
	}
	else {
	    for( int i=1; i<in.length(); i++) {
		if( !Character.isLetterOrDigit( in.charAt(i)) && !(in.charAt(i) == '_')) {
		    return false;
		}
	    }
	}
	
	//if( bn.getVariable( in) != null) return false;
	
	return true;
    }


    protected class RestrictedDocument extends PlainDocument {
	BeliefNetwork beliefNet;

        public RestrictedDocument( BeliefNetwork bn) {
	    beliefNet = bn;
        }

        public void insertString(int offs, String str, AttributeSet a)
            throws BadLocationException {

            String currentText = super.getText(0, getLength());
            String beforeOffset = currentText.substring(0, offs);
            String afterOffset = currentText.substring(offs, currentText.length());
            String proposedResult = beforeOffset + str + afterOffset;

	    if( validateString( proposedResult, beliefNet)) {
		super.insertString( offs, str, a);
	    }
	    else {
                //error dialog
                JOptionPane.showMessageDialog( null,
		  "This must be a unique value beginning with a letter, " +
		  "and only containing letters, numbers, and underscores.",
		  "Input error",
		  JOptionPane.ERROR_MESSAGE);
	    }
        }

        public void remove(int offs, int len) throws BadLocationException {
            String currentText = super.getText(0, getLength());
            String beforeOffset = currentText.substring(0, offs);
            String afterOffset = currentText.substring(len + offs,
                                                       currentText.length());
            String proposedResult = beforeOffset + afterOffset;

	    if( validateString( proposedResult, beliefNet)) {
		super.remove( offs, len);
	    }
	    else {
                //error dialog
                JOptionPane.showMessageDialog( null,
		  "This must be a unique value beginning with a letter, " +
		  "and only containing letters, numbers, and underscores.",
		  "Input error",
		  JOptionPane.ERROR_MESSAGE);
	    }
        }
    }
}
