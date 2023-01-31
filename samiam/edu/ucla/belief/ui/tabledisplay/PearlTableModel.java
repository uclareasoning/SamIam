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

/**

TableModel of a Pearl/Diez style NoisyOr representation a la Genie 2.0/SMILE 1.1, with strengths.

@author Keith Cascio
@since 061103
@see edu.ucla.belief.ui.util.NoisyOrTableModel

*/
public class PearlTableModel extends AbstractTableModel implements HuginGenieStyleTableFactory.TableModelHGS
{
	/** Constructor */
	public PearlTableModel(	FiniteVariable[] parentVars,
				List states,
				double[] weights,
				int[][] strengths,
				boolean editable )
	{
		this.myParentVars = parentVars;
		this.myNumParents = parentVars.length;
		this.myEditable = editable;
		this.myStates = states;
		this.myWeights = weights;
		this.myStrengths = strengths;
		init();
	}

	/**
		HuginGenieStyleTableFactory.TableModelHGS
		@author Keith Cascio
		@since 072103
	*/
	public void configure( JTable jtable )
	{
		configure( jtable, null, false );
	}

	/**
		HuginGenieStyleTableFactory.TableModelHGS
		@author Keith Cascio
		@since 093003
	*/
	public void configure( JTable jtable, DataHandler userHandler, boolean isHeader )
	{
		JTableHeader header = jtable.getTableHeader();
		jtable.setDefaultRenderer( Object.class, new HuginGenieStyleTableFactory.HuginGenieStyleCellRenderer( new ProbabilityJTableRenderer(), header.getDefaultRenderer() ) );

		if( isHeader ) return;

		TableCellEditor editor = new ProbabilityJTableEditor( jtable.getDefaultEditor( Object.class ) );
		jtable.setDefaultEditor( Object.class, editor );
		myProbabilityEventSource = editor;
	}

	public DataHandler[] getDataHandlers()
	{
		return null;
	}

	public void setDataHandler( DataHandler handler ){}

	private Object myProbabilityEventSource;

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		return null;
	}
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		return null;
	}
	public Object getCellEditorValue() { return null; }
	public boolean isCellEditable(EventObject anEvent) { return false; }
	public boolean shouldSelectCell(EventObject anEvent) { return false; }
	public boolean stopCellEditing() { return false; }
	public void cancelCellEditing() {}
	public void addCellEditorListener(CellEditorListener l) {}
	public void removeCellEditorListener(CellEditorListener l) {}

	public Object getProbabilityEventSource()
	{
		return myProbabilityEventSource;
	}

	public Object getExcludeEventSource()
	{
		return null;
	}

	/**
		For HuginGenieStyleTableFactory.TableModelHGS
		@author Keith Cascio
		@since 061902
	*/
	public void setEditable( boolean editable ) { this.myEditable = editable; }

	/*
	public boolean showProbabilities()
	{
		return true;
	}

	public boolean showExclude()
	{
		return false;
	}*/

	public boolean hasExclude()
	{
		return false;
	}

	/** TableModel */
	public int getRowCount()
	{
		return myTableHeight;
	}

	/** TableModel */
	public int getColumnCount()
	{
		return myTableWidth;
	}

	/** TableModel */
	public String getColumnName( int column )
	{
		Object obj = myFirstRow[column];
		if( obj == null ) return "!index";
		else return obj.toString();
	}

	/** TableModel */
	public Object getValueAt( int row, int column )
	{
		if( row < (int)0 || column < (int)0 || row > myTableHeight || column > myTableWidth )
		{
			return null;
		}
		if( column == (int)0 )
		{
			return myStates.get(row);
		}
		else return new Double( myWeights[ calculateDataIndex( row, column ) ] );
	}

	/** interface HuginGenieStyleTableFactory.TableModelHGS
		@since 030105 */
	public int convertColumnIndexToConditionIndex( int column ){
		if( (0 < column) && (column <= myTableWidth) ) return column - 1;
		else return (int)-1;
	}

	/** TableModel */
	public void setValueAt( Object aValue, int rowIndex, int columnIndex )
	{
		//System.out.println( "setValueAt( " + aValue + " )" );//debug
		if( rowIndex < 0 || columnIndex < 1 ) return;

		Double newValue = null;
		if( aValue instanceof Double ) newValue = (Double) aValue;
		else newValue = Double.valueOf( aValue.toString() );

		myWeights[ calculateDataIndex( rowIndex, columnIndex ) ] = newValue.doubleValue();

		myFlagEdited = true;
	}

	/** @since 022805 */
	public void setProbabilityEdited(){
		myFlagEdited = true;
	}

	public boolean isProbabilityEdited()
	{
		return myFlagEdited;
	}
	public boolean isExcludeDataEdited()
	{
		return false;
	}

	public boolean isEditable()
	{
		return myEditable;
	}

	/** TableModel */
	public boolean isCellEditable(int row, int column)
	{
		return( myEditable && column > 0 );
	}





	/** interface HuginGenieStyleTableFactory.TableModelHGS */
	public int calculateDataIndex( int row, int column ){
		return  ((myStates.size())*(column-(int)1)) + row;
	}

	//private
	/** @since 050503 */
	private void init()
	{
		myTableWidth = 2;
		for( int i=0; i<myNumParents; i++ )
		{
			myTableWidth += myParentVars[i].size();
		}
		myFirstRow = new Object[ myTableWidth ];
		int index = 0;
		myFirstRow[ index++ ] = "states";
		int sizeParent;
		int strength;
		for( int i=0; i<myNumParents; i++ )
		{
			sizeParent = myParentVars[i].size();
			for( int j=0; j<sizeParent; j++ )
			{
				strength = myStrengths[i][j];
				myFirstRow[ index++ ] = myParentVars[i].instance( strength );
			}
		}
		myFirstRow[ index++ ] = "LEAK";

		myTableHeight = myStates.size();
	}

	private boolean myFlagEdited = false;
	private boolean myEditable = false;
	private int myNumParents = (int)0;
	private int myTableWidth = (int)0;
	private int myTableHeight = (int)0;
	private List myStates = null;
	private double[] myWeights = null;
	private int[][] myStrengths = null;
	private Object[] myFirstRow;
	private FiniteVariable[] myParentVars = null;
}
