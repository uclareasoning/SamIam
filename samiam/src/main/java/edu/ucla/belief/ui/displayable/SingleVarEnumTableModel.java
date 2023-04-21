package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.FlagProperty;
import edu.ucla.util.UserEnumProperty;

import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;

import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/**
	@author Keith Cascio
	@since 090803
*/
public class SingleVarEnumTableModel extends AbstractTableModel implements TableCellRenderer
{
	public SingleVarEnumTableModel( BeliefNetwork bn, Variable variable )
	{
		myBeliefNetwork = bn;
		myVariable = variable;
		init();
	}

	public void init()
	{
		myData = Collections.EMPTY_LIST;

		myJCheckBox = new JCheckBox();

		myPanelBlank = new JPanel();

		myPanelError = new JPanel();
		myPanelError.add( new JLabel( "error" ) );
	}

	public void refreshValues()
	{
		for( Iterator it = myData.iterator(); it.hasNext(); )
		{
			((Struct) it.next()).refreshValue();
		}

		fireTableDataChanged();
	}

	public void commitValues()
	{
		//System.out.println( "SingleVarEnumTableModel.commitValues()" );
		for( Iterator it = myData.iterator(); it.hasNext(); )
		{
			((Struct) it.next()).commitValue();
		}
	}

	public void setVariable( Variable variable )
	{
		myVariable = variable;
	}

	public void reInitialize( SamiamUserMode mode )
	{
		//if( mode != null ) setEditable( !(mode.contains( SamiamUserMode.READONLY )) );

		EnumProperty[] array = myBeliefNetwork.propertiesAsArray();
		myData = new ArrayList( array.length );
		for( int i=0; i<array.length; i++ )
		{
			myData.add( new Struct( array[i] ) );
		}

		refreshValues();
	}

	public void setDefaultTableCellRenderer( TableCellRenderer renderer )
	{
		myDefaultTableCellRenderer = renderer;
	}

	public String getColumnName( int column )
	{
		if( column == INT_COLUMN_PROPERTY_NAME ) return "property";
		else if( column == INT_COLUMN_VALUE ) return "value";
		else return "getColumnName() Error";
	}

	public int getRowCount()
	{
		return myData.size();
	}

	public int getColumnCount()
	{
		return 2;
	}

	/** @since 032904 */
	public void setEditable( boolean flag ){
		myFlagEditable = flag;
	}

	private boolean myFlagEditable = true;
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		if( myFlagEditable && (columnIndex == INT_COLUMN_VALUE) ) return ((Struct) myData.get(rowIndex)).property.isUserEditable();
		else return false;
	}

	public Object getValueAt( int rowIndex, int column )
	{
		Struct struct = (Struct) myData.get( rowIndex );

		if( column == INT_COLUMN_PROPERTY_NAME ) return struct.property;
		else if( column == INT_COLUMN_VALUE ) return struct.getRawValue();

		return null;
	}

	public void setValueAt(Object aValue, int rowIndex, int column )
	{
		//System.out.println( "SingleVarEnumTableModel.setValueAt( "+aValue.getClass().getName()+" )" );
		Struct struct = (Struct) myData.get( rowIndex );

		if( column == INT_COLUMN_VALUE ) struct.setRawValue( aValue );
	}

	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
	{
		column = table.convertColumnIndexToModel( column );
		if( column == INT_COLUMN_VALUE )
		{
			EnumProperty property = ((Struct) myData.get( row )).property;
			if( value == null || property == null ) return myPanelBlank;
			else if( property.isFlag() )
			{
				myJCheckBox.setSelected( ((Boolean)value).booleanValue() );
				return myJCheckBox;
			}
		}

		if( myDefaultTableCellRenderer == null ) return myPanelError;
		else return myDefaultTableCellRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
	}

	public class Struct
	{
		public Struct( EnumProperty property )
		{
			this.property = property;
			this.flag = false;
		}

		private void setValue( EnumValue value )
		{
			//System.out.println( "Struct("+property+").setValue( "+value+" )" );
			if( property.isFlag() ) this.value = property.toBoolean( value ) ? Boolean.TRUE : Boolean.FALSE;
			else this.value = value;
		}

		public void setRawValue( Object value )
		{
			this.value = value;
			this.flag = true;
		}

		private EnumValue getValue()
		{
			if( property.isFlag() ) return property.valueOf( ((Boolean)this.value).booleanValue() );
			else return (EnumValue)this.value;
		}

		public Object getRawValue()
		{
			//System.out.println( "Struct("+property+").getRawValue() <- " + this.value );
			return this.value;
		}

		public void refreshValue()
		{
			this.flag = false;
			EnumValue val  = myVariable.getProperty( property );
			if( val == null ) val = property.getDefault();
			setValue( val );
		}

		public void commitValue()
		{
			if( this.flag ) myVariable.setProperty( property, getValue() );
		}

		public EnumProperty property;
		public boolean flag;
		public Object value;
	}

	private EnumValue[] myDefaultArray = new EnumValue[0];
	private ComboBoxModel myComboBoxModel = new DefaultComboBoxModel()
	{
		public int getSize()
		{
			return myDefaultArray.length;
		}

		public Object getElementAt(int index)
		{
			return myDefaultArray[index];
		}
	};
	private JComboBox myJComboBox = new JComboBox( myComboBoxModel );
	public TableCellEditor EDITOR_DEFAULT = new TableCellEditor()
	{
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
		{
			Struct struct = (Struct) myData.get( row );

			if( struct.property.isFlag() ) editor = editorCheck;
			else
			{
				myDefaultArray = struct.property.valuesAsArray();
				editor = editorCombo;
			}

			return editor.getTableCellEditorComponent( table, value, isSelected, row, column );
		}

		public Object getCellEditorValue()
		{
			return editor.getCellEditorValue();
		}

		public boolean isCellEditable(EventObject anEvent)
		{
			return editor.isCellEditable( anEvent );
		}

		public boolean shouldSelectCell(EventObject anEvent)
		{
			return editor.shouldSelectCell( anEvent );
		}

		public boolean stopCellEditing()
		{
			return editor.stopCellEditing();
		}

		public void cancelCellEditing()
		{
			editor.cancelCellEditing();
		}

		public void addCellEditorListener(CellEditorListener l)
		{
			editorCombo.addCellEditorListener( l );
			editorCheck.addCellEditorListener( l );
		}

		public void removeCellEditorListener(CellEditorListener l)
		{
			editorCombo.removeCellEditorListener( l );
			editorCheck.removeCellEditorListener( l );
		}

		private TableCellEditor editorCombo = new DefaultCellEditor( myJComboBox );
		private TableCellEditor editorCheck = new DefaultCellEditor( new JCheckBox() );
		private TableCellEditor editor = editorCombo;
	};

	private BeliefNetwork myBeliefNetwork;
	private Variable myVariable;
	private JList myJList;
	private JCheckBox myJCheckBox;
	private JPanel myPanelBlank;
	private JPanel myPanelError;
	private List myData;
	private TableCellRenderer myDefaultTableCellRenderer;

	public static final int INT_COLUMN_PROPERTY_NAME = 0;
	public static final int INT_COLUMN_VALUE = 1;
}
