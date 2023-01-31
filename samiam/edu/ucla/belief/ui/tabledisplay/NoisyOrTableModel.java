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

TableModel of a NoisyOr representation.

@author Keith Cascio
@since 021402
@see edu.ucla.belief.ui.dialogs.EditNoisyOrDialog

*/
public class NoisyOrTableModel extends AbstractTableModel implements HuginGenieStyleTableFactory.TableModelHGS
{
	/** Constructor */
	public NoisyOrTableModel(	FiniteVariable[] parentVars,
						List states,
						List weights,
						boolean editable )
	{
		this.myParentVars = parentVars;
		this.myNumParents = parentVars.length;
		this.myEditable = editable;
		this.myStates = states;
		this.myWeights = weights;
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

	public Object getProbabilityEventSource()
	{
		return myProbabilityEventSource;
	}

	public Object getExcludeEventSource()
	{
		return null;
	}

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
			//return myStates.get(row-(int)1);
			return myStates.get(row);
		}
		else return myWeights.get( calculateDataIndex( row, column ) );
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
		//if( rowIndex < 1 || columnIndex < 1 ) return;
		if( rowIndex < 0 || columnIndex < 1 ) return;

		Object newValue = null;
		if( aValue instanceof Double ) newValue = aValue;
		else newValue = Double.valueOf( aValue.toString() );

		myWeights.set( calculateDataIndex( rowIndex, columnIndex ), newValue );

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
		//return( myEditable && row > 0 && column > 0 );
		return( myEditable && column > 0 );
	}




	/** interface HuginGenieStyleTableFactory.TableModelHGS */
	public int calculateDataIndex( int row, int column ){
		//return  myNumParents*(row-(int)1) + (column-(int)1);//not correct
		//return  ((myStates.size())*(column-(int)1)) + (row-(int)1);//no more mock header row no no longer valid
		return  ((myStates.size())*(column-(int)1)) + row;
	}

	//private
	/** @since 050503 */
	private void init()
	{
		myTableWidth = 2;
		for( int i=0; i<myNumParents; i++ )
		{
			myTableWidth += myParentVars[i].size() - 1;
		}
		myFirstRow = new Object[ myTableWidth ];
		//myColumnNames = new Object[myTableWidth];
		int index = 0;
		//myColumnNames[index] = "parent nodes";
		myFirstRow[ index++ ] = "states";
		int sizeMinusOne;
		for( int i=0; i<myNumParents; i++ )
		{
			sizeMinusOne = myParentVars[i].size() - 1;
			for( int j=0; j<sizeMinusOne; j++ )
			{
				//myColumnNames[ index ] = myParentVars[i];
				myFirstRow[ index++ ] = myParentVars[i].instance(j);
			}
		}
		//myColumnNames[ index ] = "";
		myFirstRow[ index++ ] = "LEAK";

		myTableHeight = myStates.size();// + 1;
	}

	/*
	private Object[] makeColumnNames()
	{
		//System.out.println( "Java NoisyOrTableModel.makeColumnNames() myNumParents = " + myNumParents );//debug
		Object[] columnNames = new Object[myTableWidth];

		int index=(int)0;
		//System.out.println( "\tcolumnNames[" + (index) + "] = parent nodes" );//debug
		columnNames[index] = "parent nodes";
		while ( index < myNumParents )
		{
			columnNames[index+1] = myParentVars[index];
			//System.out.println( "\tcolumnNames[" + (index+1) + "] = myParentVars[" + (index) + "] = " + myParentVars[index] );//debug
			index++;
		}
		//System.out.println( "\tcolumnNames[" + (index) + "] = \"\"" );//debug
		columnNames[++index] = "";

		return columnNames;
	}*/

	private boolean myFlagEdited = false;
	private boolean myEditable = false;
	private int myNumParents = (int)0;
	private int myTableWidth = (int)0;
	private int myTableHeight = (int)0;
	private List myStates = null;
	private List myWeights = null;
	//private Object[] myColumnNames = null;
	private Object[] myFirstRow;
	private FiniteVariable[] myParentVars = null;
}