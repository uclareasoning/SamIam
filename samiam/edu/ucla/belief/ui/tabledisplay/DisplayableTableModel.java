package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.*;

import edu.ucla.belief.*;
import javax.swing.table.*;
import java.util.List;

public class DisplayableTableModel extends DefaultTableModel {
	private NetworkInternalFrame hnInternalFrame;
	private boolean editable;
	private int varsLength;

	/** Constructor */
	public DisplayableTableModel(	NetworkInternalFrame hnInternalFrame,
					TableIndex index, String[] tableNames,
					double[][] values, boolean editable )
	{
		this.hnInternalFrame = hnInternalFrame;
		this.editable = editable;

		//FiniteVariable[] vars = index.getVariableArray();
		List vars = index.variables();
		varsLength = vars.size();

		Object[][] rowData = new Object[index.size()][varsLength + tableNames.length];
		for (int i = 0; i < rowData.length; i++)
		{
			int[] mind = index.mindex(i, null);
			for (int j = 0; j < varsLength; j++)
				rowData[i][j] = ((FiniteVariable) vars.get(j)).instance(mind[j]);
			for (int j = 0; j < tableNames.length; j++)
				rowData[i][varsLength + j] = new
					Double(values[j][i]);
		}

		Object[] columnNames = new Object[varsLength + tableNames.length];
		for(int i = 0; i < varsLength; i++)
			columnNames[i] = vars.get(i);
		for(int i = 0; i < tableNames.length; i++)
			columnNames[varsLength + i] = tableNames[i];

		setDataVector(rowData, columnNames);
	}

	public boolean isEditable() {
		return editable;
	}

	public boolean isCellEditable(int row, int column) {
		if (column < varsLength)
			return false;
		return editable;
	}

	public Class getColumnClass(int columnIndex) {
		if (columnIndex < varsLength)
			return new Object().getClass();
		return new Double(0).getClass();
	}
}
