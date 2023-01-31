package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;

import edu.ucla.belief.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class RetractTable extends JTable
{
	//private NetworkInternalFrame hnInternalFrame;
	private boolean validMove;

	public RetractTable( NetworkInternalFrame hnInternalFrame )
	{
		super( new RetractTableModel( hnInternalFrame ) );
		//this.hnInternalFrame = hnInternalFrame;
		init();
	}

	public RetractTable( NetworkInternalFrame hnInternalFrame, VariableInstance[] varInstances )
	{
		super( new RetractTableModel(hnInternalFrame,varInstances) );
		//this.hnInternalFrame = hnInternalFrame;
		init();
	}
	
	/**
		@author Keith Cascio
		@since 110402
	*/
	protected void init()
	{
		validMove = false;
		setDefaultRenderer( Double.class, new BarTableCellRenderer( Color.green ) );
		setRowHeight(150);
	}

	public void columnMoved( TableColumnModelEvent event )
	{
		if(validMove) {
			validMove = false;
			return;
		}
		int fromIndex = event.getFromIndex();
		int toIndex = event.getToIndex();
		if (fromIndex == 0 || toIndex == 0) {
			validMove = true;
			getColumnModel().moveColumn(toIndex, fromIndex);
		}
		//repaint( new Rectangle(getX(), getY(), getWidth(), getHeight()) );
		repaint( getBounds() );
	}
}
