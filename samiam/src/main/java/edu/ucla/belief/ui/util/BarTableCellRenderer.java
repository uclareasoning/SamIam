package edu.ucla.belief.ui.util;

import edu.ucla.belief.*;
import java.awt.*;
import java.text.*;
import javax.swing.*;
import javax.swing.table.*;

public class BarTableCellRenderer extends DefaultTableCellRenderer {
	private Color color;

	public BarTableCellRenderer(Color color) {
		this.color = color;
	}

	public Component getTableCellRendererComponent(JTable table,
		Object value, boolean isSelected, boolean hasFocus, int
		row, int column) {
		double doubleValue = ((Double)value).doubleValue();

		NumberFormat format = NumberFormat.getInstance();
		format.setMinimumFractionDigits(4);
		format.setMaximumFractionDigits(4);
		setText(format.format(doubleValue));

		Bar icon = new Bar(table.getColumnModel().
			getColumn(column).getWidth(),
			table.getRowHeight(row) - 30, color,
			Bar.VERTICAL);
		icon.setValue(doubleValue);
		setIcon(icon);

		setHorizontalAlignment(CENTER);
		setVerticalAlignment(TOP);
		setHorizontalTextPosition(CENTER);
		setVerticalTextPosition(BOTTOM);
		return this;
	}
}
