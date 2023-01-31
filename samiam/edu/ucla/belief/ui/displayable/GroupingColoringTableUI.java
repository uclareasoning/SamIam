package edu.ucla.belief.ui.displayable;

//import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.*;
//import javax.swing.event.*;
//import java.util.Enumeration;
//import java.util.Hashtable;
//import java.util.TooManyListenersException;
//import java.awt.event.*;
import java.awt.*;
//import java.awt.datatransfer.*;
//import java.awt.dnd.*;
//import javax.swing.plaf.*;
//import java.util.EventObject;

//import javax.swing.text.*;

//import java.beans.PropertyChangeEvent;
//import java.beans.PropertyChangeListener;

/*
 * @(#)BasicTableUI.java	1.123 03/01/23
 *
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
 * Based on Sun's BasicTableUI 1.123 from 1.4.2_04,
 * modified by Keith Cascio 121304 to
 * allow fine-tuned painting of horizontal grid-lines.
 *
 * @author Philip Milne
 * @author Keith Cascio
 * @since 121304
 */
public class GroupingColoringTableUI extends javax.swing.plaf.basic.BasicTableUI
{
	public GroupingColoringTableUI( GroupingColoringJTable groupingcoloringjtable ){
		super();
		this.myGroupingColoringJTable = groupingcoloringjtable;
	}

	public GroupingColoringTableModel getModel(){
		return this.myGroupingColoringJTable.getGroupingColoringTableModel();
	}

	private GroupingColoringJTable myGroupingColoringJTable;
//
//  Paint methods and support
//

    /** Paint a representation of the <code>table</code> instance
     * that was set in installUI().
     */
    public void paint(Graphics g, JComponent c) {
	if (table.getRowCount() <= 0 || table.getColumnCount() <= 0) {
	    return;
	}
	Rectangle clip = g.getClipBounds();
	Point upperLeft = clip.getLocation();
	Point lowerRight = new Point(clip.x + clip.width - 1, clip.y + clip.height - 1);
        int rMin = table.rowAtPoint(upperLeft);
        int rMax = table.rowAtPoint(lowerRight);
        // This should never happen.
        if (rMin == -1) {
	    rMin = 0;
        }
        // If the table does not have enough rows to fill the view we'll get -1.
        // Replace this with the index of the last row.
        if (rMax == -1) {
	    rMax = table.getRowCount()-1;
        }

        boolean ltr = table.getComponentOrientation().isLeftToRight();
        int cMin = table.columnAtPoint(ltr ? upperLeft : lowerRight);
        int cMax = table.columnAtPoint(ltr ? lowerRight : upperLeft);
        // This should never happen.
        if (cMin == -1) {
	    cMin = 0;
        }
	// If the table does not have enough columns to fill the view we'll get -1.
        // Replace this with the index of the last column.
        if (cMax == -1) {
	    cMax = table.getColumnCount()-1;
        }

        // Paint the grid.
        paintGrid(g, rMin, rMax, cMin, cMax);

        // Paint the cells.
	paintCells(g, rMin, rMax, cMin, cMax);
    }

    /*
     * Paints the grid lines within <I>aRect</I>, using the grid
     * color set with <I>setGridColor</I>. Paints vertical lines
     * if <code>getShowVerticalLines()</code> returns true and paints
     * horizontal lines if <code>getShowHorizontalLines()</code>
     * returns true.
     */
    protected void paintGrid(Graphics g, int rMin, int rMax, int cMin, int cMax)
    {
    	//System.out.println( "GroupingColoringTableUI.paintGrid( "+rMin+", "+rMax+" )" );
        //g.setColor(table.getGridColor());
        GroupingColoringTableModel gctm = getModel();

	Rectangle minCell = table.getCellRect(rMin, cMin, true);
	Rectangle maxCell = table.getCellRect(rMax, cMax, true);
        Rectangle damagedArea = minCell.union( maxCell );

        if (table.getShowHorizontalLines()) {
	    int tableWidth = damagedArea.x + damagedArea.width;
	    int y = damagedArea.y;
	    for (int row = rMin; row <= rMax; row++) {
		y += table.getRowHeight(row);
		//System.out.println( "    color row "+row+": " + gctm.getGridColorSouth( row ) );
		g.setColor( gctm.getGridColorSouth( row ) );
		g.drawLine(damagedArea.x, y - 1, tableWidth - 1, y - 1);
	    }
	}

	g.setColor(table.getGridColor());

        if (table.getShowVerticalLines()) {
	    TableColumnModel cm = table.getColumnModel();
	    int tableHeight = damagedArea.y + damagedArea.height;
	    int x;
	    if (table.getComponentOrientation().isLeftToRight()) {
		x = damagedArea.x;
		for (int column = cMin; column <= cMax; column++) {
		    int w = cm.getColumn(column).getWidth();
		    x += w;
		    g.drawLine(x - 1, 0, x - 1, tableHeight - 1);
		}
	    } else {
		x = damagedArea.x + damagedArea.width;
		for (int column = cMin; column < cMax; column++) {
		    int w = cm.getColumn(column).getWidth();
		    x -= w;
		    g.drawLine(x - 1, 0, x - 1, tableHeight - 1);
		}
		x -= cm.getColumn(cMax).getWidth();
		g.drawLine(x, 0, x, tableHeight - 1);
	    }
	}
    }

    private int viewIndexForColumn(TableColumn aColumn) {
        TableColumnModel cm = table.getColumnModel();
        for (int column = 0; column < cm.getColumnCount(); column++) {
            if (cm.getColumn(column) == aColumn) {
                return column;
            }
        }
        return -1;
    }

    private void paintCells(Graphics g, int rMin, int rMax, int cMin, int cMax) {
	JTableHeader header = table.getTableHeader();
	TableColumn draggedColumn = (header == null) ? null : header.getDraggedColumn();

	TableColumnModel cm = table.getColumnModel();
	int columnMargin = cm.getColumnMargin();

        Rectangle cellRect;
	TableColumn aColumn;
	int columnWidth;
	if (table.getComponentOrientation().isLeftToRight()) {
	    for(int row = rMin; row <= rMax; row++) {
		cellRect = table.getCellRect(row, cMin, false);
                for(int column = cMin; column <= cMax; column++) {
                    aColumn = cm.getColumn(column);
                    columnWidth = aColumn.getWidth();
                    cellRect.width = columnWidth - columnMargin;
                    if (aColumn != draggedColumn) {
                        paintCell(g, cellRect, row, column);
                    }
                    cellRect.x += columnWidth;
        	}
	    }
	} else {
	    for(int row = rMin; row <= rMax; row++) {
                cellRect = table.getCellRect(row, cMin, false);
                aColumn = cm.getColumn(cMin);
                if (aColumn != draggedColumn) {
                    columnWidth = aColumn.getWidth();
                    cellRect.width = columnWidth - columnMargin;
                    paintCell(g, cellRect, row, cMin);
                }
                for(int column = cMin+1; column <= cMax; column++) {
                    aColumn = cm.getColumn(column);
                    columnWidth = aColumn.getWidth();
                    cellRect.width = columnWidth - columnMargin;
                    cellRect.x -= columnWidth;
                    if (aColumn != draggedColumn) {
                        paintCell(g, cellRect, row, column);
                    }
        	}
	    }
	}

        // Paint the dragged column if we are dragging.
        if (draggedColumn != null) {
	    paintDraggedArea(g, rMin, rMax, draggedColumn, header.getDraggedDistance());
	}

	// Remove any renderers that may be left in the rendererPane.
	rendererPane.removeAll();
    }

    private void paintDraggedArea(Graphics g, int rMin, int rMax, TableColumn draggedColumn, int distance) {
        int draggedColumnIndex = viewIndexForColumn(draggedColumn);

        Rectangle minCell = table.getCellRect(rMin, draggedColumnIndex, true);
	Rectangle maxCell = table.getCellRect(rMax, draggedColumnIndex, true);

	Rectangle vacatedColumnRect = minCell.union(maxCell);

	// Paint a gray well in place of the moving column.
	g.setColor(table.getParent().getBackground());
	g.fillRect(vacatedColumnRect.x, vacatedColumnRect.y,
		   vacatedColumnRect.width, vacatedColumnRect.height);

	// Move to the where the cell has been dragged.
	vacatedColumnRect.x += distance;

	// Fill the background.
	g.setColor(table.getBackground());
	g.fillRect(vacatedColumnRect.x, vacatedColumnRect.y,
		   vacatedColumnRect.width, vacatedColumnRect.height);

	// Paint the vertical grid lines if necessary.
	if (table.getShowVerticalLines()) {
	    g.setColor(table.getGridColor());
	    int x1 = vacatedColumnRect.x;
	    int y1 = vacatedColumnRect.y;
	    int x2 = x1 + vacatedColumnRect.width - 1;
	    int y2 = y1 + vacatedColumnRect.height - 1;
	    // Left
	    g.drawLine(x1-1, y1, x1-1, y2);
	    // Right
	    g.drawLine(x2, y1, x2, y2);
	}

	for(int row = rMin; row <= rMax; row++) {
	    // Render the cell value
	    Rectangle r = table.getCellRect(row, draggedColumnIndex, false);
	    r.x += distance;
	    paintCell(g, r, row, draggedColumnIndex);

	    // Paint the (lower) horizontal grid line if necessary.
	    if (table.getShowHorizontalLines()) {
		g.setColor(table.getGridColor());
		Rectangle rcr = table.getCellRect(row, draggedColumnIndex, true);
		rcr.x += distance;
		int x1 = rcr.x;
		int y1 = rcr.y;
		int x2 = x1 + rcr.width - 1;
		int y2 = y1 + rcr.height - 1;
		g.drawLine(x1, y2, x2, y2);
	    }
	}
    }

    private void paintCell(Graphics g, Rectangle cellRect, int row, int column) {
        if (table.isEditing() && table.getEditingRow()==row &&
                                 table.getEditingColumn()==column) {
            Component component = table.getEditorComponent();
	    component.setBounds(cellRect);
            component.validate();
        }
        else {
            TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component component = table.prepareRenderer(renderer, row, column);
            rendererPane.paintComponent(g, component, table, cellRect.x, cellRect.y,
                                        cellRect.width, cellRect.height, true);
        }
    }
}  // End of Class BasicTableUI
