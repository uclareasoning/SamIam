/*
	(swing1.1.1)
	http://www2.gol.com/users/tame/swing/examples/JTableExamples9.html
*/

package edu.ucla.belief.ui.tabledisplay;
//package jp.gr.java_conf.tame.swing.table;

import javax.swing.*;
import javax.swing.table.*;

/**
 * @version 1.0 08/21/99
*/
public class EditableHeaderTableColumn extends TableColumn
{
	protected TableCellEditor headerEditor;
	protected boolean isHeaderEditable;

	public EditableHeaderTableColumn()
	{
		setHeaderEditor(createDefaultHeaderEditor());
		isHeaderEditable = true;
	}

	public void setHeaderEditor(TableCellEditor headerEditor)
	{
		this.headerEditor = headerEditor;
	}

	public TableCellEditor getHeaderEditor()
	{
		return headerEditor;
	}

	public void setHeaderEditable(boolean isEditable)
	{
		isHeaderEditable = isEditable;
	}

	public boolean isHeaderEditable()
	{
		return isHeaderEditable;
	}

	public void copyValues(TableColumn base)
	{
		modelIndex     = base.getModelIndex();
		identifier     = base.getIdentifier();
		width	  = base.getWidth();
		minWidth       = base.getMinWidth();
		setPreferredWidth(base.getPreferredWidth());
		maxWidth       = base.getMaxWidth();
		headerRenderer = base.getHeaderRenderer();
		headerValue    = base.getHeaderValue();
		cellRenderer   = base.getCellRenderer();
		cellEditor     = base.getCellEditor();
		isResizable    = base.getResizable();
	}

	protected TableCellEditor createDefaultHeaderEditor()
	{
		return new DefaultCellEditor(new JTextField());
	}
}
