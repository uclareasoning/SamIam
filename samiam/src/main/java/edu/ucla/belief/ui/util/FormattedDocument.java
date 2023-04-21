package edu.ucla.belief.ui.util;

import javax.swing.*;
import javax.swing.text.*;
import java.text.*;
import java.util.*;

public abstract class FormattedDocument extends NotifyDocument
{
	private Format format;
	private Set    setFormatForceValids;
	private String myLastValidData       = STR_EMPTY;

	public FormattedDocument(Format f) {
		format = f;
	}

	/** @since 20040805 */
	public String getLastValidData()
	{
		return myLastValidData;
	}

	/** @since 20020523 */
	public void addFormatForceValid( String newFormatForceValid )
	{
		if( setFormatForceValids == null ){ setFormatForceValids = new HashSet( 0x10 ); }
		setFormatForceValids.add( newFormatForceValid );
	}

	public Format getFormat() {
		return format;
	}

	public String secondaryValidation( String in ){
		return in;
	}

	public String validateString( String in )
	{
		//System.out.println( "FormattedDocument.validateString("+in+")" );

		if(      in       == null ){ return in; }
		else if( in.length() <= 0 ){ return in; }
		else{
			if( (setFormatForceValids != null) && setFormatForceValids.contains( in ) ){ return in; }

			if( (in = secondaryValidation( in )) == null ){ return in; }

			try{
				format.parseObject( in );
			}catch( ParseException e ){
				return null;
			}

			return myLastValidData = in;
		}
	}
}