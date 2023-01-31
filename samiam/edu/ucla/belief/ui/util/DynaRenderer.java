package edu.ucla.belief.ui.util;

import edu.ucla.belief.Dynamator;

import java.awt.*;
import javax.swing.*;

/**
	@author Keith Cascio
	@since 011703
*/
public class DynaRenderer implements ListCellRenderer
{
	public DynaRenderer( ListCellRenderer r )
	{
		myListCellRenderer = r;
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
				((Dynamator)value).getDisplayName(),
				index,
				isSelected,
				cellHasFocus );
	}
	
	protected ListCellRenderer myListCellRenderer;
}
