package edu.ucla.belief.ui.internalframes;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
From: Graphic Java 2 3rd Edition by David M. Geary
1999 Sun Microsystems, Inc.
ISBN 0-13-079667-0

@author David M. Geary
*/
abstract public class AbstractCellEditor
				implements TableCellEditor {
	protected EventListenerList listenerList =
					new EventListenerList();
	protected Object value;
	protected ChangeEvent changeEvent = null;
	protected int clickCountToStart = 1;

	public Object getCellEditorValue() {
		return value;
	}
	public void setCellEditorValue(Object value) {
		this.value = value;
	}
	public void setClickCountToStart(int count) {
		clickCountToStart = count;
	}
	public int getClickCountToStart() {
		return clickCountToStart;
	}
	public boolean isCellEditable(EventObject anEvent) {
		if (anEvent instanceof MouseEvent) {
			if (((MouseEvent)anEvent).getClickCount() <
						clickCountToStart)
				return false;
		}
		return true;
	}
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}
	public boolean stopCellEditing() {
		fireEditingStopped();
		return true;
	}
	public void cancelCellEditing() {
		fireEditingCanceled();
	}
	public void addCellEditorListener(CellEditorListener l) {
		listenerList.add(CellEditorListener.class, l);
	}
	public void removeCellEditorListener(CellEditorListener l) {
		listenerList.remove(CellEditorListener.class, l);
	}
	protected void fireEditingStopped() {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i] == CellEditorListener.class) {
				if (changeEvent == null)
					changeEvent = new ChangeEvent(this);
				((CellEditorListener)
				listeners[i+1]).editingStopped(changeEvent);
			}
		}
	}
	protected void fireEditingCanceled() {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==CellEditorListener.class) {
				if (changeEvent == null)
					changeEvent = new ChangeEvent(this);
				((CellEditorListener)
				listeners[i+1]).editingCanceled(changeEvent);
			}
		}
	}
}
