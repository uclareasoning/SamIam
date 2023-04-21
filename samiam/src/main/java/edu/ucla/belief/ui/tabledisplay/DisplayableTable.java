package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;

import edu.ucla.belief.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.util.List;
import java.util.Iterator;

/** A JTable that displays a Table or an array of Table objects. */
public class DisplayableTable extends JTable {
	private NetworkInternalFrame hnInternalFrame;
	private Table[] tables;
	private String[] tableNames;
	private FiniteVariable[] vars;
	private double[][] values;
	private boolean editable;
	private boolean initializing;
	private boolean validMove;
	private boolean validValue;

	/** Constructor for one table */
	public DisplayableTable(NetworkInternalFrame hnInternalFrame,
		Table table, String tableName, boolean editable) {
		this.hnInternalFrame = hnInternalFrame;
		tables = new Table[1];
		tables[0] = table;
		tableNames = new String[1];
		tableNames[0] = tableName;
		this.editable = editable;
		createDisplayableTable();
	}

	public DisplayableTable(NetworkInternalFrame hnInternalFrame,
		Table[] tables, String[] tableNames, boolean editable) {
		this.hnInternalFrame = hnInternalFrame;
		this.tables = (Table[])tables.clone();
		this.tableNames = (String[])tableNames.clone();
		this.editable = editable;
		createDisplayableTable();
	}

	public void createDisplayableTable()
	{
		TableIndex index = tables[0].index();

		//vars = (FiniteVariable[])index.getVariableArray().clone();
		List listVariables = index.variables();
		vars = new FiniteVariable[ listVariables.size() ];
		int counter = 0;
		for( Iterator it = listVariables.iterator(); it.hasNext(); ) vars[counter++] = (FiniteVariable) it.next();

		values = new double[tables.length][];
		for (int i = 0; i < tables.length; i++)
			values[i] = (double[])tables[i].dataclone();

		initializing = true;
		validMove = false;
		validValue = false;
		setModel(new DisplayableTableModel(hnInternalFrame, index,
			tableNames, values, editable));
		createDefaultColumnsFromModel();

		TableColumnModel columnModel = getColumnModel();
		DefaultCellEditor editor = new DefaultCellEditor(new
			JTextField());
		editor.setClickCountToStart(1);
		Class doubleClass = new Double(0).getClass();
		setDefaultEditor(doubleClass, editor);
		setDefaultRenderer(doubleClass, new
			DoubleTableCellRenderer(new DoubleFormat(16)));
		initializing = false;
	}

	public boolean isEditable() {
		return editable;
	}

	public double getTableValueAt(int table, int row) {
		return values[table][row];
	}

	public void setTableValueAt(double value, int table, int row) {
		validValue = true;
		values[table][row] = value;
		setValueAt(new Double(value), row, vars.length + table);
	}

	private void resetTableValueAt(int table, int row) {
		validValue = true;
		setValueAt(String.valueOf(values[table][row]), row,
			vars.length + table);
	}

	public void stopCellEditing()
	{
		int editingColumn = getEditingColumn();
		if (editingColumn != -1)
		{
			//Keith Cascio
			//030402
			TableCellEditor TCE = getColumnModel().getColumn(editingColumn).getCellEditor();
			if( TCE != null ) TCE.stopCellEditing();
		}
	}

	public void saveToTables() {
		for (int i = 0; i < tables.length; i++)
			for (int j = 0; j < getRowCount(); j++)
				tables[i].setCP(j, values[i][j]);
	}

	public void tableChanged(TableModelEvent event) {
		if (hnInternalFrame == null)
			return;
		if (initializing)
			return;
		if (validValue) {
			validValue = false;
			return;
		}
		int row = event.getFirstRow();
		int column = event.getColumn();
		int table = column - vars.length;
		String string = getValueAt(row, column).toString();
		try {
			double value = Double.parseDouble(string);
			if (value < 0.0)
				resetTableValueAt(table, row);
			else
				setTableValueAt(value, table, row);
		}
		catch (NumberFormatException nfe) {
			resetTableValueAt(table, row);
		}
	}

	public void columnMoved(TableColumnModelEvent event) {
		if (validMove) {
			validMove = false;
			return;
		}
		validMove = true;
		getColumnModel().moveColumn(event.getToIndex(),
			event.getFromIndex());
	}
}
