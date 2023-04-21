package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

public class DoubleTableCellRenderer extends DefaultTableCellRenderer
{
	private DoubleFormat format;

	public DoubleTableCellRenderer(DoubleFormat format) {
		setHorizontalAlignment(JLabel.TRAILING);
		this.format = format;
		Font font = getFont();
		setFont(new Font(font.getName(), Font.PLAIN,
			font.getSize()));
	}

	public Component getTableCellRendererComponent(JTable table,
		Object value, boolean isSelected, boolean hasFocus, int
		row, int column)
	{
		JLabel ret = (JLabel) super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );

		ret.setText(format.doubleFormat(((Double)value).doubleValue()));

		return ret;
	}
}
