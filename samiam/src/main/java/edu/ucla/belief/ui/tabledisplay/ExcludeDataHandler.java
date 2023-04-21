package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.*;

import java.util.List;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/** @author keith cascio
	@since  20030721 */
public class ExcludeDataHandler extends edu.ucla.belief.ui.internalframes.AbstractCellEditor implements DataHandler, ActionListener
{
	public ExcludeDataHandler( boolean[] data, TableCellRenderer headerRenderer )
	{
		myHeaderRenderer = headerRenderer;
		myExclude = data;

		FlowLayout layout = new FlowLayout();
		layout.setHgap( 0 );
		layout.setVgap( 0 );
		layout.setAlignment( FlowLayout.CENTER );
		myJPanel = new JPanel( layout );
		myJCheckBox = new JCheckBox();
		myJCheckBox.addActionListener( this );
		myJPanel.add( myJCheckBox );
	}

	/** @since 20080229 */
	public boolean             minimize(){
		return false;
	}

	public void setStainWright( StainWright wright ){
		myStainWright = wright;
	}

	public void setData( boolean[] data )
	{
		myExclude = data;
	}

	public Object getEventSource()
	{
		return this;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		if( value instanceof Boolean )
		{
			myJCheckBox.setSelected( ((Boolean)value).booleanValue() );
			return myJPanel;
		}
		else return myHeaderRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
	}

	public void setCellEditorValue(Object value) {
		super.setCellEditorValue( value );
		myJCheckBox.setSelected( ((Boolean)value).booleanValue() );
	}

	public Object getCellEditorValue() {
		return myJCheckBox.isSelected() ? Boolean.TRUE : Boolean.FALSE;
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		myJCheckBox.setSelected( ((Boolean)value).booleanValue() );
		return myJPanel;
	}

	public boolean stopCellEditing() {
		//System.out.println( "ExcludeDataHandler.stopCellEditing()" );
		return super.stopCellEditing();
	}

	public void cancelCellEditing() {
		//new Throwable().printStackTrace();
		//System.out.println( "ExcludeDataHandler.cancelCellEditing()" );
		super.cancelCellEditing();
	}

	public boolean shouldSelectCell(EventObject anEvent) {
		return false;
	}

	public void actionPerformed( ActionEvent e ){
		stopCellEditing();
	}

	private JCheckBox myJCheckBox;
	private JPanel myJPanel;
	private TableCellRenderer myHeaderRenderer;
	private boolean[] myExclude;
	private boolean myFlagEdited = false;
	private StainWright myStainWright;

	public boolean isEdited()
	{
		return myFlagEdited;
	}

	/** @since 022805 */
	public void setEdited( boolean flag ){
		myFlagEdited = flag;
	}

	public boolean handlesProbabilities()
	{
		return false;
	}

	public String getDisplayName()
	{
		return "Sensitivity: lock by parameters settings";
	}

	public String toString()
	{
		return getDisplayName();
	}

	public Object getValueAt( int possibleIndex )
	{
		if( 0 <= possibleIndex && possibleIndex < myExclude.length )
		{
			return new Boolean( myExclude[ possibleIndex ] );
		}
		else
		{
			System.err.println( "ExcludeDataHandler bad data index: " + possibleIndex );
			return "!index";
		}
	}

	public void setValueAt( Object aValue, int linearIndex )
	{
		boolean newValue = false;
		if( aValue instanceof Boolean ) newValue = ((Boolean)aValue).booleanValue();
		else
		{
			newValue = aValue.equals( "true" );
		}

		myExclude[ linearIndex ] = newValue;

		myFlagEdited = true;
	}
}