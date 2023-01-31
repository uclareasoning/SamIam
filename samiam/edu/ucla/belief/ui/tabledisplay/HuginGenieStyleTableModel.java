package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.io.StandardNode;
import edu.ucla.belief.sensitivity.ExcludePolicy;
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

Genie/Hugin style TableModel of a CPT representation.

@author Keith Cascio
@since 043002

*/
public class HuginGenieStyleTableModel extends AbstractTableModel implements HuginGenieStyleTableFactory.TableModelHGS
{
	/** Constructor */
	public HuginGenieStyleTableModel(	FiniteVariable child,
						FiniteVariable[] parentsVars,
						double[] copyOfData,
						boolean editable )
	{
		this( child, parentsVars, copyOfData, null, editable );
	}

	public HuginGenieStyleTableModel(	FiniteVariable child,
						FiniteVariable[] parentsVars,
						double[] copyOfData,
						boolean[] copyOfExcludeArray,
						boolean editable )
	{
		this.myFiniteVariable = child;
		this.myParentVars = parentsVars;//makeParentVars( cpt );
		this.myNumParents = myParentVars.length;
		this.myEditable = editable;
		//this.myStates = myFiniteVariable.instanceNames();
		this.myStates = myFiniteVariable.instances();
		this.myNumStates = myStates.size();

		initParentDegrees();
		initTableDimensions( myParentVars.length, myNumStates );
		initColumnNames();

		//this.myTable = cpt;
		//this.myData = myTable.data();
		this.myData = copyOfData;
		this.myExclude = copyOfExcludeArray;

		//showProbabilities();
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
		myHeaderRenderer = header.getDefaultRenderer();

		if( isHeader )
		{
		  //jtable.setDefaultRenderer( Object.class, myHeaderRenderer );
			jtable.setDefaultRenderer( Object.class, new HuginGenieStyleTableFactory.ForceBackgroundColorCellRenderer( new DefaultTableCellRenderer() ) );
			return;
		}

		if( userHandler == null )
		{
			if( myProbabilityDataHandler == null && myData != null ) myProbabilityDataHandler = new ProbabilityDataHandler( myData, new ProbabilityJTableRenderer(), myHeaderRenderer, new ProbabilityJTableEditor( jtable.getDefaultEditor( Object.class ) ) );
			if( myExcludeDataHandler == null ) myExcludeDataHandler = new ExcludeDataHandler( myExclude, myHeaderRenderer );
			myDataHandler = myProbabilityDataHandler;
		}
		else myDataHandler = myUserHandler = userHandler;

		//jtable.setDefaultRenderer( Object.class, new HuginGenieStyleTableFactory.HuginGenieStyleCellRenderer( new ProbabilityJTableRenderer(), header.getDefaultRenderer() ) );
		//jtable.setDefaultEditor( Object.class, new ProbabilityJTableEditor( jtable.getDefaultEditor( Object.class ) ) );
		jtable.setDefaultRenderer( Object.class, this );
		jtable.setDefaultEditor( Object.class, this );
	}

	public DataHandler[] getDataHandlers()
	{
		if( myDataHandlers == null )
		{
			if( myProbabilityDataHandler != null && myExcludeDataHandler != null ) myDataHandlers = new DataHandler[] { myProbabilityDataHandler, myExcludeDataHandler };
			else if( myUserHandler != null ) myDataHandlers = new DataHandler[] { myUserHandler };
		}

		return myDataHandlers;
	}

	public void setDataHandler( DataHandler handler )
	{
		if( myDataHandler != handler )
		{
			if( validate( handler ) )
			{
				if( handler == myExcludeDataHandler && myExclude == null )
				{
					myExclude = ExcludePolicy.makeExcludeArray( (StandardNode) myFiniteVariable );
					myExcludeDataHandler.setData( myExclude );
				}
				myDataHandler = handler;
				fireTableDataChanged();
			}
		}
	}

	public boolean validate( DataHandler handler )
	{
		if( myDataHandlers == null ) return false;
		else
		{
			for( int i=0; i<myDataHandlers.length; i++ )
			{
				if( myDataHandlers[i] == handler ) return true;
			}
			return false;
		}
	}

	public Object getProbabilityEventSource()
	{
		return ( myProbabilityDataHandler == null ) ? null : myProbabilityDataHandler.getEventSource();
	}
	public Object getExcludeEventSource()
	{
		return ( myExcludeDataHandler == null ) ? null : myExcludeDataHandler.getEventSource();
	}

	/** @since 022805 */
	public void setProbabilityEdited(){
		if( myProbabilityDataHandler != null ) myProbabilityDataHandler.setEdited( true );
	}

	public boolean isProbabilityEdited()
	{
		return ( myProbabilityDataHandler == null ) ? false : myProbabilityDataHandler.isEdited();
	}
	public boolean isExcludeDataEdited()
	{
		return ( myExcludeDataHandler == null ) ? false : myExcludeDataHandler.isEdited();
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		return myDataHandler.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
	}
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		return myDataHandler.getTableCellEditorComponent( table, value, isSelected, row, column );
	}
	public Object getCellEditorValue() { return myDataHandler.getCellEditorValue(); }
	public boolean isCellEditable(EventObject anEvent) { return myDataHandler.isCellEditable(anEvent); }
	public boolean shouldSelectCell(EventObject anEvent) { return myDataHandler.shouldSelectCell(anEvent); }
	public boolean stopCellEditing() { return myDataHandler.stopCellEditing(); }
	public void cancelCellEditing() { myDataHandler.cancelCellEditing(); }
	public void addCellEditorListener(CellEditorListener l)
	{
		if( myProbabilityDataHandler != null ) myProbabilityDataHandler.addCellEditorListener(l);
		if( myExcludeDataHandler != null ) myExcludeDataHandler.addCellEditorListener(l);
	}
	public void removeCellEditorListener(CellEditorListener l)
	{
		if( myProbabilityDataHandler != null ) myProbabilityDataHandler.removeCellEditorListener(l);
		if( myExcludeDataHandler != null ) myExcludeDataHandler.removeCellEditorListener(l);
	}

	/**
		For HuginGenieStyleTableFactory.TableModelHGS
		@author Keith Cascio
		@since 061902
	*/
	public void setEditable( boolean editable ) { this.myEditable = editable; }

	int[] myParentDegrees = null;
	Object[] myColumnNames = null;

	private void initParentDegrees()
	{
		myParentDegrees = new int[ myNumParents ];
		int currentDegree = (int)0;
		for( int i=myParentVars.length-1; i >= 0; i-- )
		{
			currentDegree = myParentVars[i].size();
			myParentDegrees[i] = currentDegree;
		}
	}

	private void initColumnNames()
	{
		if( myNumParents == 0 ) return;
		else
		{
			myColumnNames = new Object[ myTableWidth ];
			FiniteVariable lastParent = myParentVars[ myParentVars.length-1 ];
			myColumnNames[0] = lastParent;
			int lastParentSize = lastParent.size();
			int counter = 1;
			while( counter < myTableWidth )
			{
				for( int i=0; i<lastParentSize; i++ )
				{
					myColumnNames[ counter++ ] = lastParent.instance( i );
				}
			}
		}
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
		if( myColumnNames == null ) return null;
		else return myColumnNames[column].toString();
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
			//int possibleIndex = row-myNumParents;
			int possibleIndex = row;
			if( 0 <= possibleIndex && possibleIndex < myNumStates ) return myStates.get(possibleIndex);
			else return "!index";
		}
		else
		{
			int possibleIndex = calculateDataIndex( row, column );
			return myDataHandler.getValueAt( possibleIndex );
		}
	}

	/** interface HuginGenieStyleTableFactory.TableModelHGS
		@since 030105 */
	public int convertColumnIndexToConditionIndex( int column ){
		if( (0 < column) && (column <= myTableWidth) ) return column - 1;
		else return (int)-1;
	}

	/*
	public boolean showProbabilities()
	{
		if( myData == null ) return false;
		//else if( myProbabilityDataHandler == null ) myProbabilityDataHandler = new ProbabilityDataHandler();

		myDataHandler = myProbabilityDataHandler;
		fireTableDataChanged();
		return true;
	}

	public boolean showExclude()
	{
		//if( myExclude == null ) myExclude = SensitivityEngine.getExcludeArray( myFiniteVariable );
		if( myExclude == null ) return false;
		//else if( myExcludeDataHandler == null ) myExcludeDataHandler = new ExcludeDataHandler( myExclude, myHeaderRenderer );

		myDataHandler = myExcludeDataHandler;
		fireTableDataChanged();
		return true;
	}*/

	public boolean hasExclude()
	{
		return myExclude != null;
	}

	public boolean[] getExcludeData()
	{
		return myExclude;
	}

	private DataHandler[] myDataHandlers;
	private DataHandler myDataHandler;
	private ProbabilityDataHandler myProbabilityDataHandler;
	private ExcludeDataHandler myExcludeDataHandler;
	private DataHandler myUserHandler;
	private TableCellRenderer myHeaderRenderer;

	/** interface HuginGenieStyleTableFactory.TableModelHGS */
	public int calculateDataIndex( int row, int column )
	{
		return ((column-INDEX_FIRST_DATA_COLUMN)*myNumStates) + (row/*-0*/);//(row-myNumParents);
	}

	/** TableModel */
	public void setValueAt( Object aValue, int rowIndex, int columnIndex )
	{
		//System.out.println( "setValueAt( " + aValue + " )" );//debug
		if( rowIndex < 0 || columnIndex < INDEX_FIRST_DATA_COLUMN ) return;
		myDataHandler.setValueAt( aValue, calculateDataIndex( rowIndex, columnIndex ) );
	}

	public boolean isEditable()
	{
		return myEditable;
	}

	/** TableModel */
	public boolean isCellEditable(int row, int column)
	{
		return( myEditable && row >= 0 && column >= INDEX_FIRST_DATA_COLUMN );
	}

	private void initTableDimensions( int parentVarsLength, int statesLength )
	{
		//myTableHeight = parentVarsLength + statesLength;
		myTableHeight = 0 + statesLength;
		myTableWidth = (int)1;
		for( int i=0; i < parentVarsLength; i++ ) myTableWidth *= myParentDegrees[i];
		myTableWidth += INDEX_FIRST_DATA_COLUMN;
	}

	private int INDEX_FIRST_DATA_COLUMN = (int)1;
	private boolean myEditable = false;
	private int myNumParents = (int)0;
	private int myNumStates = (int)0;
	private int myTableWidth = (int)0;
	private int myTableHeight = (int)0;
	private List myStates = null;
	private Table myTable = null;
	private double[] myData = null;
	private boolean[] myExclude = null;
	private FiniteVariable[] myParentVars = null;
	private FiniteVariable myFiniteVariable = null;
}
