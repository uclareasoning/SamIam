/*
 * (swing1.1beta3)
 * http://www2.gol.com/users/tame/swing/examples/JTableExamples1.html
 */

package edu.ucla.belief.ui.tabledisplay;
//package jp.gr.java_conf.tame.swing.table;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumnModel;


/**
  * ColumnGroup
  *
  * @version 1.0 10/20/98
  * @author Nobuo Tamemasa
  * @author Steve Webb 16/09/04 swebb99_uk@hotmail.com
  */

public class ColumnGroup {
    /**
     * Cell renderer for group header.
     */
    protected TableCellRenderer renderer;
    /**
     * Holds the TableColumn or ColumnGroup objects contained
     * within this ColumnGroup instance.
     */
    protected Vector v;
    /**
     * The ColumnGroup instance name.
     */
    protected String text;
    /**
     * The margin to use for renderering.
     */
    protected int margin=0;

    private ColumnGroup myParent;

    /** @author Keith Cascio @since 102604 */
    public void setParent( ColumnGroup columngroup ){
		this.myParent = columngroup;
	}

    /** @author Keith Cascio @since 102604 */
    public ColumnGroup getParent(){
		return this.myParent;
	}

    /**
     * Standard ColumnGroup constructor.
     * @param text Name of the ColumnGroup which will be displayed
     * when the ColumnGroup is renderered.
     */
    public ColumnGroup(String text) {
        this(null,text);
    }

    /**
     * Standard ColumnGroup constructor.
     * @param renderer a TableCellRenderer for the group.
     * @param text Name of the ColumnGroup which will be displayed
     * when the ColumnGroup is renderered.
     */
    public ColumnGroup(TableCellRenderer renderer,String text) {
        if (renderer == null) {
            this.renderer = new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
                    JTableHeader header = table.getTableHeader();
                    if (header != null) {
                        setForeground(header.getForeground());
                        setBackground(header.getBackground());
                        setFont(header.getFont());
                    }
                    setHorizontalAlignment(JLabel.CENTER);
                    setText((value == null) ? "" : value.toString());
                    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
                    return this;
                }
            };
        } else {
            this.renderer = renderer;
        }
        this.text = text;
        v = new Vector();
    }


    /**
     * Add a TableColumn or ColumnGroup object to the
     * ColumnGroup instance.
     * @param obj TableColumn or ColumnGroup
     */
    public void add(Object obj) {
        if (obj == null) { return; }
        v.addElement(obj);
        if( obj instanceof ColumnGroup ) ((ColumnGroup)obj).setParent( (ColumnGroup)this );
    }


    /**
     * Get the ColumnGroup list containing the required table
     * column.
     * @param g vector to populate with the ColumnGroup/s
     * @param c TableColumn
     * @return Vector containing the ColumnGroup/s
     */
    public Vector getColumnGroups(TableColumn c, Vector g) {
        g.addElement(this);
        if (v.contains(c)) return g;
        Iterator iter = v.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (obj instanceof ColumnGroup) {
                Vector groups =
                (Vector)((ColumnGroup)obj).getColumnGroups(c,(Vector)g.clone());
                if (groups != null) return groups;
            }
        }
        return null;
    }

    /**
     * Returns the TableCellRenderer for the ColumnGroup.
     * @return the TableCellRenderer
     */
    public TableCellRenderer getHeaderRenderer() {
        return renderer;
    }

    /**
     * Set the TableCellRenderer for this ColumnGroup.
     * @param renderer the renderer to use
     */
    public void setHeaderRenderer(TableCellRenderer renderer) {
        if (renderer != null) {
            this.renderer = renderer;
        }
    }

    /**
     * Get the ColumnGroup header value.
     * @return the value.
     */
    public Object getHeaderValue() {
        return text;
    }

    /**
     * Get the dimension of this ColumnGroup.
     * @param table the table the header is being rendered in
     * @return the dimension of the ColumnGroup
     */
    public Dimension getSize(JTable table) {
        Component comp = renderer.getTableCellRendererComponent(
        table, getHeaderValue(), false, false,-1, -1);
        int height = comp.getPreferredSize().height;
        int width  = 0;

        TableModel tm = table.getModel();
        PortedTableModelHGS portedtablemodelhgs = null;
        if( tm instanceof PortedTableModelHGS ) width = calcWidth( table, (PortedTableModelHGS)tm );
		else{
        Iterator iter = v.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (obj instanceof TableColumn) {
                TableColumn aColumn = (TableColumn)obj;
                width += aColumn.getWidth();
            } else {
                width += ((ColumnGroup)obj).getSize(table).width;
            }
        }
		}

        return new Dimension(width, height);
    }

    /** @author Keith Cascio @since 102604 */
    public int calcWidth( JTable table, PortedTableModelHGS portedtablemodelhgs )
    {
		//System.out.println( "(ColumnGroup)"+text+".calcWidth()" );

		if( (portedtablemodelhgs==null) || (table == null) ) return (int)0;

		TableColumnModel tcm = table.getColumnModel();
		TableColumn aColumn = null;
		int width=0, mappedIndex, viewIndex, columnCount = tcm.getColumnCount();

		for( Iterator iter = v.iterator(); iter.hasNext(); ){
            Object obj = iter.next();
            if (obj instanceof TableColumn) {
                aColumn = (TableColumn)obj;
                mappedIndex = portedtablemodelhgs.invertColumnIndex( aColumn.getModelIndex() );
                viewIndex = table.convertColumnIndexToView( mappedIndex );
                //System.out.println( "    "+aColumn.getIdentifier()+"(modeli "+aColumn.getModelIndex()+")" );
                //System.out.print( "    viewi "+viewIndex+"(mappedi "+mappedIndex+"): " );
                if( (viewIndex < tcm.getColumnCount()) && (viewIndex >= 0) ){
					width += tcm.getColumn( viewIndex ).getWidth();
					//System.out.println( tcm.getColumn( viewIndex ).getIdentifier() );
				}//else System.out.println();
            } else {
                width += ((ColumnGroup)obj).getSize(table).width;
            }
		}

		return width;
	}

    /**
     * Sets the margin that ColumnGroup instance will use and all
     * held TableColumns and/or ColumnGroups.
     * @param margin the margin
     */
    public void setColumnMargin(int margin) {
        this.margin = margin;
        Iterator iter = v.iterator();
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (obj instanceof ColumnGroup) {
                ((ColumnGroup)obj).setColumnMargin(margin);
            }
        }
    }
}

