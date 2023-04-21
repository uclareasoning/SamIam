package edu.ucla.belief.ui.dialogs;

import java.util.regex.Pattern;
import java.util.Set;
import javax.swing.*;
import java.awt.*;

/** Partial implementation of IdentificationModel

	@author Keith Cascio
	@since 020405 */
public abstract class AbstractIdentificationModel implements IdentificationModel
{
	public static final Color COLOR_DARK_BLUE = new Color( 0x00, 0x00, 0x99 );
	public static final Color COLOR_DARK_GREEN = new Color( 0x00, 0x99, 0x00 );

	public AbstractIdentificationModel( Stage stage ){
		this.myStage = stage;
	}

	final public Object[] getRange(){
		if( myRange == null ) myRange = makeRange();
		return myRange;
	}

	final public Object getElementUnidentified(){
		return CPTImportWizard.STR_UNIDENTIFIED;
	}

	final public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus ){
		Component ret = myRenderer.getListCellRendererComponent( list, this.valueToString( value ), index, isSelected, cellHasFocus );
		if( value == getElementUnidentified() ) ret.setForeground( Color.red );
		else{
			Color custom = getColor( value );
			if( custom != null ) ret.setForeground( custom );
		}
		return ret;
	}

	public boolean isOneToOne(){
		return true;
	}

	public Stage getStage(){
		return this.myStage;
	}

	public String valueToString( Object value ){
		return value.toString();
	}

	public static boolean guess( String str1, String str2 ){
		if( (str1 == null) || (str2 == null) ) return false;
		String regex = "(^|[^\\p{Alnum}])\\Q" + str2 + "\\E([^\\p{Alnum}]|$)";
		//return (str1 != null) && (str1.indexOf(str2) >= 0);
		boolean ret = Pattern.compile(regex).matcher(str1).find();//Pattern.matches( regex, str1 );
		//System.out.println( "guess( \""+str1+"\", \""+str2+"\" )? " + ret );
		return ret;
	}

	abstract public Color getColor( Object value );
	abstract public Object[] makeRange();

	private ListCellRenderer myRenderer = new DefaultListCellRenderer();
	private Object[] myRange;
	private Stage myStage;
}
