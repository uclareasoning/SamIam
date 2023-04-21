/*
	(swing1.1.1)
	http://www2.gol.com/users/tame/swing/examples/JTableExamples9.html
*/

package edu.ucla.belief.ui.tabledisplay;
//package jp.gr.java_conf.tame.swing.table;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;

/**
 * @version 1.0 08/21/99
*/
public class EditableHeaderUI extends BasicTableHeaderUI
{
	protected MouseInputListener createMouseInputListener()
	{
		return new MouseInputHandler((EditableHeader)header);
	}

	public class MouseInputHandler extends BasicTableHeaderUI.MouseInputHandler
	{
		private Component dispatchComponent;
		protected EditableHeader header;

		public MouseInputHandler(EditableHeader header)
		{
			this.header = header;
		}

		private void setDispatchComponent(MouseEvent e)
		{
			Component editorComponent = header.getEditorComponent();
			Point p = e.getPoint();
			Point p2 = SwingUtilities.convertPoint(header, p, editorComponent);
			dispatchComponent = SwingUtilities.getDeepestComponentAt(editorComponent,
				p2.x, p2.y);
		}

		private boolean repostEvent(MouseEvent e)
		{
			if (dispatchComponent == null)
			{
				return false;
			}

			MouseEvent e2 = SwingUtilities.convertMouseEvent(header, e, dispatchComponent);
			dispatchComponent.dispatchEvent(e2);
			return true;
		}

		public void mousePressed(MouseEvent e)
		{
			if (!SwingUtilities.isLeftMouseButton(e))
			{
				return;
			}

			super.mousePressed(e);

			if (header.getResizingColumn() == null)
			{
				Point p = e.getPoint();
				TableColumnModel columnModel = header.getColumnModel();
				int index = columnModel.getColumnIndexAtX(p.x);
				if (index != -1)
				{
					if (header.editCellAt(index, e))
					{
						setDispatchComponent(e);
						repostEvent(e);
					}
				}
			}
		}

		public void mouseReleased(MouseEvent e)
		{
			super.mouseReleased(e);
			if (!SwingUtilities.isLeftMouseButton(e))
			{
				return;
			}

			repostEvent(e);
			dispatchComponent = null;
		}
	}
}
