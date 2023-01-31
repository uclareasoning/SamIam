package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.*;

import java.util.List;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;

/** @author keith cascio
	@since  20030721 */
public class ProbabilityDataHandler implements DataHandler
{
	public ProbabilityDataHandler( double[] data, TableCellRenderer defaultRenderer, TableCellRenderer headerRenderer, TableCellEditor tce )
	{
	  //System.out.println( "ProbabilityDataHandler( "+HuginGenieStyleTableFactory.str(defaultRenderer)+", "+HuginGenieStyleTableFactory.str(headerRenderer)+" )" );
		myData            = data;
		myDefaultRenderer = defaultRenderer;
		myHeaderRenderer  = headerRenderer;
		myTableCellEditor = tce;
		myNumberFormat    = new DecimalFormat( "0.0###############################" );
	}

	public void setStainWright( StainWright wright ){
		myStainWright = wright;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		if( value instanceof Double )
		{
			Component ret = myDefaultRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			if( (myStainWright != null) && (!hasFocus) && (!isSelected) ){
				myStainWright.stain( row, column, ret );
			}
			return ret;
		}
		else return myHeaderRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
	}

	public Object getEventSource()
	{
		return myTableCellEditor;
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		JTextField ret = (JTextField) myTableCellEditor.getTableCellEditorComponent( table, value, isSelected, row, column );

		if( isSelected )
		{
			//System.out.println( "ProbabilityJTableEditor setting colors for selected cell editor component." );//debug
			if( ProbabilityJTableEditor.safe() ){
				ret.setBackground( ProbabilityJTableEditor.mySelectBackgroundColor );
				ret.setForeground( ProbabilityJTableEditor.mySelectForegroundColor );
			}else{
				ret.setForeground( (Color) UIManager.getDefaults().get( "TextField.foreground" ) );
			}
		}
		else
		{
			//System.out.println( "ProbabilityJTableEditor setting colors for non-selected cell editor component." );//debug
			ret.setBackground( ProbabilityJTableEditor.myNormalBackgroundColor );
			ret.setForeground( ProbabilityJTableEditor.myNormalForegroundColor );
		}

		//setText( myNumberFormat.format( ((Double)value).doubleValue() ) );
		//((javax.swing.JTable$GenericEditor)ret).setText( myNumberFormat.format( value ) );
		ret.setText( myNumberFormat.format( value ) );

		ret.setSelectedTextColor( ProbabilityJTableEditor.mySelectedTextColor );
		ret.setSelectionColor( ProbabilityJTableEditor.mySelectionColor );

		return ret;
	}

	public Object getCellEditorValue() { return myTableCellEditor.getCellEditorValue(); }
	public boolean isCellEditable(EventObject anEvent) { return myTableCellEditor.isCellEditable(anEvent); }
	public boolean shouldSelectCell(EventObject anEvent) { return myTableCellEditor.shouldSelectCell(anEvent); }
	public boolean stopCellEditing() { return myTableCellEditor.stopCellEditing(); }
	public void cancelCellEditing() { myTableCellEditor.cancelCellEditing(); }
	public void addCellEditorListener(CellEditorListener l) { myTableCellEditor.addCellEditorListener(l); }
	public void removeCellEditorListener(CellEditorListener l) { myTableCellEditor.removeCellEditorListener(l); }

	private TableCellRenderer myDefaultRenderer;
	private TableCellRenderer myHeaderRenderer;
	private NumberFormat myNumberFormat;
	private TableCellEditor myTableCellEditor;
	private double[] myData;
	private boolean myFlagEdited = false;
	private boolean myFlagMinimize = false;
	private StainWright myStainWright;

	/** @since 20080229 */
	public TableCellRenderer getDefaultRenderer(){
		return myDefaultRenderer;
	}

	/** @since 20080229 */
	public ProbabilityDataHandler setMinimize( boolean flag ){
		this.myFlagMinimize = flag;
		return this;
	}

	/** @since 20080229 */
	public boolean             minimize(){
		return this.myFlagMinimize;
	}

	public boolean isEdited(){
		return myFlagEdited;
	}

	/** @since 20050228 */
	public void setEdited( boolean flag ){
		myFlagEdited = flag;
	}

	public boolean handlesProbabilities()
	{
		return true;
	}

	public String getDisplayName()
	{
		return "Conditional Probability Table";
	}

	public String toString()
	{
		return getDisplayName();
	}

	public Object getValueAt( int possibleIndex )
	{
		if( 0 <= possibleIndex && possibleIndex < myData.length )
		{
			return new Double( myData[ possibleIndex ] );
		}
		else
		{
			System.err.println( "ProbabilityDataHandler bad data index: " + possibleIndex );
			return "!index";
		}
	}

	public void setValueAt( Object aValue, int linearIndex )
	{
		double newValue = (double)0;
		if( aValue instanceof Double ) newValue = ((Double)aValue).doubleValue();
		else
		{
			try{
				newValue = Double.parseDouble( aValue.toString() );
			}catch( NumberFormatException e ){
				return;
			}
		}

		myData[ linearIndex ] = newValue;

		myFlagEdited = true;
	}
}