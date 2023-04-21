package edu.ucla.belief.ui.tabledisplay;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.text.*;

/** @author Keith Cascio
	@since  20020517 */
public class ProbabilityJTableRenderer extends DefaultTableCellRenderer implements TableCellRenderer
{
	public static final String STR_NAN = "NaN";

	private Color
	  myNormalBackgroundColor =     Color .white,
	  myNormalForegroundColor =     Color .black,
	  myHFocusBackgroundColor = new Color( 196,196,220 ),
	  myHFocusForegroundColor =     Color .black,
	  mySelectBackgroundColor = new Color( 240,240,255 ),
	  mySelectForegroundColor =     Color .black;
	private NumberFormat myNumberFormat = null;

	public ProbabilityJTableRenderer(){
		myNumberFormat = new DecimalFormat( "0.0###############################" );
	}

	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column ){
		Color                  bg = myNormalBackgroundColor;
		Color                  fg = myNormalForegroundColor;
		if(        hasFocus ){ bg = myHFocusBackgroundColor;
		                       fg = myHFocusForegroundColor; }
		else if( isSelected ){ bg = mySelectBackgroundColor;
		                       fg = mySelectForegroundColor; }
		setBackground(         bg );
		setForeground(         fg );

		Double pr = (Double) value;
		setText( pr.isNaN() ? STR_NAN : myNumberFormat.format( pr ) );
		return this;
	}

	/*
	/// @since 20080228 //
	public Dimension getPreferredSize(){
		Dimension dim  = super.getPreferredSize();
		Insets insets  = getInsets( new Insets(0,0,0,0) );
        dim    .width -= (insets.left +  insets.right);
        dim   .height -= (insets .top + insets.bottom);
        return dim;
	}

	/// Overridden for performance reasons. //
	public void    firePropertyChange( String propertyName, boolean oldValue, boolean newValue ){}
	/// Overridden for performance reasons. //
	protected void firePropertyChange( String propertyName,  Object oldValue,  Object newValue ){}
	/// Overridden for performance reasons. //
	public boolean isOpaque(){ return true; }
	/// Overridden for performance reasons. //
	public void     repaint( long tm, int x, int y, int width, int height ){}
	/// Overridden for performance reasons. //
	public void     repaint( Rectangle r ){}
	/// Overridden for performance reasons. //
	public void  revalidate(){}
	/// Overridden for performance reasons. //
	public void    validate(){}*/
}
