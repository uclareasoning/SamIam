package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.belief.sensitivity.*;
import edu.ucla.util.*;

import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;

/**
	@author Keith Cascio
	@since 021902
*/
public class SensitivitySuggestionTableModel extends AbstractTableModel
{
	private static final boolean BUTTON_COLUMNS = false;

	public static final int CPT_PARAMETER_COLUMN_INDEX	= 0;
	public static final int EDIT_CPT_BUTTON_COLUMN_INDEX	= ( BUTTON_COLUMNS ) ? CPT_PARAMETER_COLUMN_INDEX + 1 : -1;
	public static final int OLD_VALUE_COLUMN_INDEX		= ( BUTTON_COLUMNS ) ? EDIT_CPT_BUTTON_COLUMN_INDEX + 1 : CPT_PARAMETER_COLUMN_INDEX + 1;
	public static final int NEW_VALUE_COLUMN_INDEX		= OLD_VALUE_COLUMN_INDEX + 1;
	public static final int ADOPT_CHANGE_COLUMN_INDEX	= ( BUTTON_COLUMNS ) ? NEW_VALUE_COLUMN_INDEX + 1 : -2;
	public static final int ABSOLUTE_CHANGE_COLUMN_INDEX	= ( BUTTON_COLUMNS ) ? ADOPT_CHANGE_COLUMN_INDEX + 1 : NEW_VALUE_COLUMN_INDEX + 1;
	public static final int LOG_ODDS_CHANGE_COLUMN_INDEX	= ABSOLUTE_CHANGE_COLUMN_INDEX + 1;

	public static final int NUM_COLUMNS = LOG_ODDS_CHANGE_COLUMN_INDEX + 1;

	private Object[][] rowData = null;
	private String[] columnNames = null;
	private java.util.List suggestions;

	protected int myIndices[] = null;

	private NetworkInternalFrame hnInternalFrame;

	/** @since 20020607 */
	public SensitivitySuggestion getSuggestion( int row )
	{
		int realrowindex = myIndices[row];
		if( realrowindex >= 0 && realrowindex < suggestions.size())
		{
			return (SensitivitySuggestion) suggestions.get(
				realrowindex );
		}
		else return null;
	}

	public SensitivitySuggestionTableModel(NetworkInternalFrame hnInternalFrame,
		java.util.List suggestions)
	{
		this.hnInternalFrame = hnInternalFrame;
		this.suggestions = suggestions;
		this.rowData = new Object[suggestions.size()][NUM_COLUMNS];

		allocate();

		JButton tempButton = null;
		CPTParameter tempCPTParam = null;
		SingleParamSuggestion suggestion = null;

		for (int i = 0; i < suggestions.size(); i++)
		{
			suggestion = (SingleParamSuggestion)suggestions.
				get(i);

			rowData[i][CPT_PARAMETER_COLUMN_INDEX] = tempCPTParam = suggestion.getCPTParameter();
			rowData[i][OLD_VALUE_COLUMN_INDEX] = new Double(suggestion.getTheta());
			rowData[i][NEW_VALUE_COLUMN_INDEX] = suggestion.getInterval();
			rowData[i][ABSOLUTE_CHANGE_COLUMN_INDEX] = new Interval(suggestion.getAbsoluteChange(), Double.POSITIVE_INFINITY);
			rowData[i][LOG_ODDS_CHANGE_COLUMN_INDEX] = new Interval(suggestion.getLogOddsChange(), Double.POSITIVE_INFINITY);

			if( BUTTON_COLUMNS )
			{
				tempButton = new JButton( abbreviatedCPTType( suggestion ) );//KGC 030702
				tempButton.setFont( tempButton.getFont().deriveFont( (float)8.75 ) );
				tempButton.addActionListener( new EditButtonListener( (DisplayableFiniteVariable)tempCPTParam.getVariable() ) );
				tempButton.setEnabled( true );
				rowData[i][EDIT_CPT_BUTTON_COLUMN_INDEX] = tempButton;

				tempButton = new JButton( abbreviatedCPTType( suggestion ) );
				tempButton.setFont( tempButton.getFont().deriveFont( (float)8.75 ) );
				tempButton.addActionListener( new AdoptChangeButtonListener(suggestion) );
				tempButton.setEnabled( true );
				rowData[i][ADOPT_CHANGE_COLUMN_INDEX] = tempButton;
			}
		}

		this.columnNames = new String[ NUM_COLUMNS ];
		columnNames[    CPT_PARAMETER_COLUMN_INDEX ] = "Parameter";
		columnNames[        OLD_VALUE_COLUMN_INDEX ] = SensitivityInternalFrame.FLAG_CRAM ?   "Current" :   "Current value";
		columnNames[        NEW_VALUE_COLUMN_INDEX ] = SensitivityInternalFrame.FLAG_CRAM ? "Suggested" : "Suggested value";
		columnNames[  ABSOLUTE_CHANGE_COLUMN_INDEX ] = "Absolute change";
		columnNames[  LOG_ODDS_CHANGE_COLUMN_INDEX ] = "Log-odds change";

		if( BUTTON_COLUMNS )
		{
			columnNames[EDIT_CPT_BUTTON_COLUMN_INDEX] = "Edit";
			columnNames[ADOPT_CHANGE_COLUMN_INDEX] = "Adopt Change";
		}
	}

	/** @since 20051129 */
	public void setColumnName( int index, String name ){
		if( SensitivitySuggestionTableModel.this.columnNames == null ) return;
		if( (index >= 0) && (index < columnNames.length) ){
			//System.out.println( "columnNames["+index+"] = " + name );
			SensitivitySuggestionTableModel.this.columnNames[index] = name;
			SensitivitySuggestionTableModel.this.fireTableDataChanged();
			//SensitivitySuggestionTableModel.this.fireTableRowsUpdated();
			//SensitivitySuggestionTableModel.this.fireTableStructureChanged();
		}
	}

	/** @since 20020307 */
	private String abbreviatedCPTType( SensitivitySuggestion ss )
	{
		//CPTShell shell = ((CPTParameter)ss.getObject()).getCPTShell();
		CPTShell shell = ss.getVariable().getCPTShell();
		if( shell instanceof NoisyOrShellPearl ) return "NoisyOr";
		else return "CPT";
	}

	/**
		@author Keith Cascio
		@since 021902
	*/
	private class EditButtonListener implements ActionListener
	{
		private DisplayableFiniteVariable myVar = null;

		public EditButtonListener( DisplayableFiniteVariable var )
		{
			myVar = var;
		}

		public void actionPerformed( ActionEvent evt )
		{
			myVar.showNodePropertiesDialog( hnInternalFrame, true );
		}
	}

	/**
		@author Keith Cascio
		@since 051602
	*/
	private class AdoptChangeButtonListener implements ActionListener
	{
		private SensitivitySuggestion mySuggestion = null;

		public AdoptChangeButtonListener( SensitivitySuggestion ss )
		{
			mySuggestion = ss;
		}

		public void actionPerformed( ActionEvent evt )
		{
			try {
				mySuggestion.adoptChange();
			} catch (Exception e) {
				hnInternalFrame.getParentFrame().showErrorDialog(e.getMessage());
			}
		}
	}

	/**
		Based on David M. Geary's SortDecorator.java
		@author Keith Cascio
		@since 082102
	*/
	public void sort(int column)
	{
		if( columnSortable( column ) )
		{
			int rowCount = getRowCount();
			Comparator comp = forColumn( column );

			for(int i=0; i < rowCount; i++) {
				for(int j = i+1; j < rowCount; j++) {
					if( comp.compare( getValueAt(j,column), getValueAt(i,column) ) < 0 ) {
						swap(i,j);
					}
				}
			}
		}
	}

	/**
		Based on David M. Geary's SortDecorator.java
		@author Keith Cascio
		@since 082102
	*/
	public void swap(int i, int j)
	{
		int tmp = myIndices[i];
		myIndices[i] = myIndices[j];
		myIndices[j] = tmp;
	}

	/**
		@author Keith Cascio
		@since 082102
	*/
	public boolean columnSortable( int column )
	{
		//return column == CPT_PARAMETER_COLUMN_INDEX || column == ABSOLUTE_CHANGE_COLUMN_INDEX || column == LOG_ODDS_CHANGE_COLUMN_INDEX;
		return true;
	}

	/**
		@author Keith Cascio
		@since 082102
	*/
	protected Comparator forColumn( int column )
	{
		switch( column )
		{
			case CPT_PARAMETER_COLUMN_INDEX:
				return theCPTParameterComparator;
			case OLD_VALUE_COLUMN_INDEX:
				return theCurrentValueComparator;
			case NEW_VALUE_COLUMN_INDEX:
				return theSuggestedValueComparator;
			case ABSOLUTE_CHANGE_COLUMN_INDEX:
				return theAbsoluteChangeComparator;
			case LOG_ODDS_CHANGE_COLUMN_INDEX:
				return theLogOddsChangeComparator;
			default:
				return null;
		}
	}

	protected static Comparator theCPTParameterComparator = new Comparator()
	{
		public int compare(Object o1, Object o2)
		{
			return ((CPTParameter)o1).compareTo( o2 );
		}
	};
	public static Comparator theDoubleComparator = new Comparator()
	{
		public int compare(Object o1, Object o2)
		{
			return ((Double)o1).compareTo( (Double)o2 );
		}
	};
	public static Comparator theIntervalComparator = new Comparator()
	{
		public int compare(Object o1, Object o2)
		{
			return ((Interval)o1).compareLowerBound( o2 );
		}
	};
	protected static Comparator theCurrentValueComparator = theDoubleComparator;
	protected static Comparator theAbsoluteChangeComparator = theIntervalComparator;
	protected static Comparator theLogOddsChangeComparator = theIntervalComparator;
	protected static Comparator theSuggestedValueComparator = theIntervalComparator;

	/**
		Based on David M. Geary's SortDecorator.java
		@author Keith Cascio
		@since 082102
	*/
	private void allocate() {
		myIndices = new int[getRowCount()];

		for(int i=0; i < myIndices.length; ++i) {
			myIndices[i] = i;
		}
	}

	/**
		Based on David M. Geary's SortDecorator.java
		@author Keith Cascio
		@since 082102
	*/
	public Object getValueAt(int row, int column) {
		return myGetValueAt(myIndices[row], column);
	}

	/**
		Based on David M. Geary's SortDecorator.java
		@author Keith Cascio
		@since 082102
	*/
	public void setValueAt(Object aValue, int row, int column) {
		mySetValueAt(aValue, myIndices[row], column);
	}

	protected void mySetValueAt( Object value, int row, int column )
	{
		rowData[ row ][ column ] = value;
		fireTableCellUpdated( row, column );
	}

	protected Object myGetValueAt( int row, int column )
	{
		return rowData[row][column];
	}

	public int getRowCount()
	{
		return rowData.length;
	}

	public int getColumnCount()
	{
		return NUM_COLUMNS;
	}

	public String getColumnName( int column )
	{
		return columnNames[ column ];
	}

	public boolean isCellEditable(int row, int column)
	{
		switch( column )
		{
			case EDIT_CPT_BUTTON_COLUMN_INDEX:
				return true;
			case ADOPT_CHANGE_COLUMN_INDEX:
				return true;
			default:
				return false;
		}
		//return ( column == EDIT_CPT_BUTTON_COLUMN_INDEX || column == ADOPT_CHANGE_COLUMN_INDEX );
	}

	public Class getColumnClass(int columnIndex)
	{
		switch( columnIndex )
		{
			case CPT_PARAMETER_COLUMN_INDEX:
				return CPTParameter.class;
			case OLD_VALUE_COLUMN_INDEX:
				return Double.class;
			case EDIT_CPT_BUTTON_COLUMN_INDEX:
				return JButton.class;
			case ADOPT_CHANGE_COLUMN_INDEX:
				return JButton.class;
			default:
				return Interval.class;
		}
	}
}
