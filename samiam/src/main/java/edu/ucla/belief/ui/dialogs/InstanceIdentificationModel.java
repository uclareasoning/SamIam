package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.util.Util;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;

import java.util.List;
import java.util.*;
import javax.swing.*;
import java.awt.*;

/** Model supports identifying the tokens
	that occur within one column
	of a tab-delimited data file containing CPT
	data.  The tokens correspond to instances
	of a single variable.

	@author Keith Cascio
	@since 020405 */
public class InstanceIdentificationModel extends AbstractIdentificationModel
{
	public static final boolean FLAG_VALUE_INDICES_BEGIN_AT_ONE_DEFAULT = true;

	public InstanceIdentificationModel( FiniteVariable fv, Stage stage ){
		super( stage );
		//System.out.println( "InstanceIdentificationModel( "+fv+" )" );
		this.myVariable = fv;
		this.myStringSize = "/" + Integer.toString( myVariable.size() ) + ")</font> ";
		this.myBuffer = new StringBuffer( 64 );
		this.myBuffer.append( STR_RENDERED_PREFIX );
	}

	public static final String STR_RENDERED_PREFIX = "<html><nobr><font color=\"#999999\">(";

	public boolean isOneToOne(){
		return false;
	}

	public void setIndicesBeginAtOne( boolean flag ){
		this.myFlagIndicesBeginAtOne = flag;
	}

	public Color getColor( Object value ){
		return (Color)null;//Color.black;
	}

	public String valueToString( Object value ){
		if( value == getElementUnidentified() ) return getElementUnidentified().toString();
		myBuffer.setLength( STR_RENDERED_PREFIX.length() );
		myBuffer.append( Integer.toString( myVariable.index(value)+1 ) );
		myBuffer.append( myStringSize );
		myBuffer.append( Util.htmlEncode( value.toString() ) );
		return myBuffer.toString();
	}

	public Object[] makeRange(){
		//System.out.println( "InstanceIdentificationModel.makeRange()" );
		myInstances = myVariable.instances();

		Object[] ret = new Object[ myInstances.size() + 1 ];
		int index = 0;
		ret[index++] = getElementUnidentified();
		for( Iterator it = myInstances.iterator(); it.hasNext(); ){
			ret[index++] = it.next();
		}
		return ret;
	}

	public Object guess( String token ){
		String tokenlower = token.trim().toLowerCase();

		int parsed = (int)-1;
		try{
			parsed = Integer.parseInt( tokenlower );
			if( myFlagIndicesBeginAtOne ) --parsed;
		}catch( Exception exception ){
			parsed = (int)-1;
		}

		if( (parsed >= 0) && (parsed < myVariable.size()) ){
			return myVariable.instance( parsed );
		}

		Object instance;
		String instancelower;
		for( Iterator it = myInstances.iterator(); it.hasNext(); ){
			instance = it.next();
			instancelower = instance.toString().trim().toLowerCase();
			if( guess( instancelower, tokenlower ) ) return instance;
			if( guess( tokenlower, instancelower ) ) return instance;
		}

		return getElementUnidentified();
	}

	public boolean isIdentified( Set selected ){
		//selected.remove( getElementUnidentified() );
		//return !selected.isEmpty();
		return !selected.contains( getElementUnidentified() );
	}

	private FiniteVariable myVariable;
	private String myStringSize;
	private StringBuffer myBuffer;
	private List myInstances;
	private boolean myFlagIndicesBeginAtOne = FLAG_VALUE_INDICES_BEGIN_AT_ONE_DEFAULT;
}
