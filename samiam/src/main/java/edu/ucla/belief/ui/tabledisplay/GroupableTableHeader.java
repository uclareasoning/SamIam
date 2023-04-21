/*
 * (swing1.1beta3)
 * http://www2.gol.com/users/tame/swing/examples/JTableExamples1.html
 */

package edu.ucla.belief.ui.tabledisplay;
//package jp.gr.java_conf.tame.swing.table;

import java.util.Iterator;
import java.util.Vector;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;



/**
  * GroupableTableHeader
  *
  * @version 1.0 10/20/98
  * @author Nobuo Tamemasa
  * @author Steve Webb 16/09/04 swebb99_uk@hotmail.com
  */

public class GroupableTableHeader extends JTableHeader {

    /** Identifies the UI class which draws the header */
    private static final String uiClassID = "GroupableTableHeaderUI";

    /**
     * Constructs a GroupableTableHeader which is initialized with cm as the
     * column model. If cm is null this method will initialize the table header
     * with a default TableColumnModel.
     * @param model the column model for the table
     */
    public GroupableTableHeader(GroupableTableColumnModel model) {
        super(model);
        setUI(new GroupableTableHeaderUI());
        setReorderingAllowed(false);
    }


    /**
     * Sets the margins correctly for all groups within
     * the header.
     */
    public void setColumnMargin() {
        int columnMargin = getColumnModel().getColumnMargin();
        Iterator iter = ((GroupableTableColumnModel)columnModel).columnGroupIterator();
        while (iter.hasNext()) {
            ColumnGroup cGroup = (ColumnGroup)iter.next();
            cGroup.setColumnMargin(columnMargin);
        }
    }

}

