package edu.ucla.belief.ui.util;

import edu.ucla.belief.Variable;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

/**
	@author Keith Cascio
	@since 042103
*/
public class VariableIDRenderer implements ListCellRenderer, TableCellRenderer
{
	public VariableIDRenderer( ListCellRenderer r )
	{
		myListCellRenderer = r;
	}

	public VariableIDRenderer( TableCellRenderer r )
	{
		myTableCellRenderer = r;
	}

	public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus)
	{
		return myListCellRenderer.getListCellRendererComponent(
				list,
				decorate( value ),
				index,
				isSelected,
				cellHasFocus );
	}

	public Component getTableCellRendererComponent(JTable table,
				Object value,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column)
	{
		return myTableCellRenderer.getTableCellRendererComponent(
				table,
				decorate( value ),
				isSelected,
				hasFocus,
				row,
				column);
	}

	protected Object decorate( Object value )
	{
		if( value instanceof Variable ) return ((Variable)value).getID();
		else return value;
	}

	protected ListCellRenderer myListCellRenderer;
	protected TableCellRenderer myTableCellRenderer;
}
