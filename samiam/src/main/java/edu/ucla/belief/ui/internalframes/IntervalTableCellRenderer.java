package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;

import edu.ucla.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

public class IntervalTableCellRenderer extends DefaultTableCellRenderer
{
	private DoubleFormat format;

	public IntervalTableCellRenderer(DoubleFormat format) {
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
		Interval interval = (Interval)value;
		ret.setText( interval.toString( format ) );
		return ret;
	}
}
