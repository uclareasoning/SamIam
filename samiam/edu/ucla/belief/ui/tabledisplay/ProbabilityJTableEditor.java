package edu.ucla.belief.ui.tabledisplay;

import java.util.EventObject;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.text.*;

/** @author keith cascio
    @since  20020517 */
public class ProbabilityJTableEditor implements TableCellEditor
{
	public static Color
	  myNormalBackgroundColor = Color.green,
	  myNormalForegroundColor = Color.black,
	  mySelectBackgroundColor = new Color( 0x38, 0x60, 0x80 ),//new Color( 0xff, 0xcc, 0xff ),//
	  mySelectForegroundColor = Color.white,                  //new Color( 0x00, 0x33, 0x00 ),//
	  mySelectedTextColor     = Color.black,
	  mySelectionColor        = new Color( 0x00, 0xff, 0xff );
	private NumberFormat myNumberFormat = null;

	private TableCellEditor myTableCellEditor = null;

	public ProbabilityJTableEditor( TableCellEditor tce )
	{
		myTableCellEditor = tce;
		myNumberFormat = new DecimalFormat( "0.0###############################" );
	}

	/** @since 2002101602 */
	public TableCellEditor getEventSource(){
		return myTableCellEditor;
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		JTextField ret = (JTextField) myTableCellEditor.getTableCellEditorComponent( table, value, isSelected, row, column );

		if( isSelected ){
			//System.out.println( "ProbabilityJTableEditor setting colors for selected cell editor component." );//debug
			if( safe() ){
				ret.setBackground( mySelectBackgroundColor );
				ret.setForeground( mySelectForegroundColor );
			}else{
				ret.setForeground( (Color) UIManager.getDefaults().get( "TextField.foreground" ) );
			}
		}
		else
		{
			//System.out.println( "ProbabilityJTableEditor setting colors for non-selected cell editor component." );//debug
			ret.setBackground( myNormalBackgroundColor );
			ret.setForeground( myNormalForegroundColor );
		}

		//setText( myNumberFormat.format( ((Double)value).doubleValue() ) );
		//((javax.swing.JTable$GenericEditor)ret).setText( myNumberFormat.format( value ) );
		ret.setText( myNumberFormat.format( value ) );

		ret.setSelectedTextColor( mySelectedTextColor );
		ret.setSelectionColor( mySelectionColor );

		return ret;
	}

	public Object        getCellEditorValue(                      ){ return myTableCellEditor.      getCellEditorValue(    ); }
	public boolean           isCellEditable( EventObject       ev ){ return myTableCellEditor.          isCellEditable( ev ); }
	public boolean         shouldSelectCell( EventObject       ev ){ return myTableCellEditor.        shouldSelectCell( ev ); }
	public boolean          stopCellEditing(                      ){ return myTableCellEditor.         stopCellEditing(    ); }
	public void           cancelCellEditing(                      ){        myTableCellEditor.       cancelCellEditing(    ); }
	public void       addCellEditorListener( CellEditorListener l ){        myTableCellEditor.   addCellEditorListener(  l ); }
	public void    removeCellEditorListener( CellEditorListener l ){        myTableCellEditor.removeCellEditorListener(  l ); }

	/** @since 20091229 */
	static public boolean safe(){
		try{
			String  lower = UIManager.getLookAndFeel().getClass().getName().toLowerCase();
			return (lower.indexOf( "gtk" ) < 0) && (lower.indexOf( "nimbus" ) < 0);
		}catch( Throwable thrown ){}
		return false;
	}
}
