package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.FlagProperty;
import edu.ucla.util.AbstractEnumProperty;

import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;

import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/** @author keith cascio
	@since  20030820 */
public class EnumTableModel extends AbstractTableModel implements TableCellRenderer, Comparator
{
	public EnumTableModel( BeliefNetwork bn )
	{
		this.myBeliefNetwork = bn;
		this.myArray = myBeliefNetwork.propertiesAsArray();
		init();
	}

	public void reInitialize()
	{
		//System.out.println( "EnumTableModel.reInitialize()" );
		this.myArray = myBeliefNetwork.propertiesAsArray();

		myProperty = null;
		myCurrentPropertyIndex = (int)-1;
		myValueModel.removeAllElements();

		myFlagStable = false;
	}

	private void init()
	{
		myData = new ArrayList( (int)0 );

		myValueModel = new DefaultComboBoxModel();
		myJCheckBox = new JCheckBox();

		myPanelBlank = new JPanel();

		myPanelError = new JPanel();
		myPanelError.add( new JLabel( "error" ) );
	}

	public void setDefaultTableCellRenderer( TableCellRenderer renderer )
	{
		myDefaultTableCellRenderer = renderer;
	}

	public DefaultComboBoxModel getValueModel()
	{
		return myValueModel;
	}

	public void setVariables( Collection variables )
	{
		//System.out.println( "EnumTableModel.setVariables()" );
		//new Throwable().printStackTrace();

		myFlagEditable = false;

		int size = variables.size();
		Collection drawFrom = variables;

		Struct[] toAdd = new Struct[ size ];
		int index = (int)0;
		Struct next;

		if( myFlagStable )
		{
			HashSet lookup = new HashSet( variables );
			drawFrom = lookup;
			for( Iterator it = myData.iterator(); it.hasNext(); )
			{
				next = (Struct) it.next();
				if( lookup.contains( next.variable ) )
				{
					toAdd[ index++ ] = next;
					lookup.remove( next.variable );
				}
			}
		}

		for( Iterator it = drawFrom.iterator(); it.hasNext(); )
		{
			next = new Struct( (FiniteVariable)it.next() );
			next.refreshValues();
			toAdd[ index++ ] = next;
		}

		Arrays.sort( toAdd, this );
		myData.clear();
		for( int i=0; i<size; i++ ) myData.add( toAdd[i] );

		myFlagStable = true;
		fireTableDataChanged();
		resetEditableState();
	}

	public void removeVariables( Collection toRemove )
	{
		if( myData != null )
		{
			Collection structs = new LinkedList();
			Struct next;
			for( Iterator it = myData.iterator(); it.hasNext(); )
			{
				next = (Struct) it.next();
				if( toRemove.contains( next.variable ) ) structs.add( next );
			}

			if( !structs.isEmpty() )
			{
				myData.removeAll( structs );
				fireTableDataChanged();
			}
		}
	}

	/** @return true if the model changed as a result of this call */
	public boolean setRowsDisplayed( int[] rows, boolean display ){
		boolean ret = false;
		if( myData == null ) return ret;
		try{
			if( ((rows == null) || (rows.length < 1)) && display ){
				ret = ! myData.isEmpty();
				myData.clear();
			}else{
				Collection structs = new LinkedList();
				for( int i=0; i<rows.length; i++ ) structs.add( myData.get( rows[i] ) );

				if( structs.isEmpty() ) return ret;

				ret = display ? myData.retainAll( structs ) : myData.removeAll( structs );
			}
		}catch( Exception exception ){
			System.err.println( "warning: EnumTableModel.setRowsDisplayed() caught " + exception );
		}finally{
			if( ret ) fireTableDataChanged();
		}
		return ret;
	}

	public void setProperty( EnumProperty property, int index )
	{
		//System.out.println( "EnumTableModel.setProperty( "+property+", "+index+" )" );
		if( (property == null) || (myProperty != property) )
		{
			myProperty = property;
			myCurrentPropertyIndex = index;

			if( myProperty != null )
			{
				myIsFlagProperty = myProperty.isFlag();

				myValueModel.removeAllElements();
				for( Iterator it = myProperty.iterator(); it.hasNext(); )
				{
					myValueModel.addElement( it.next() );
				}

				fireTableDataChanged();
			}

			resetEditableState();
		}
	}

	/**
		@author Keith Cascio
		@since 100903
	*/
	private void resetEditableState()
	{
		myFlagEditable = (myProperty != null ) && (myProperty.isUserEditable());
	}

	public void setValues( EnumProperty property, EnumValue value )
	{
		//System.out.println( "EnumTableModel.setValues( "+property+", "+value+" )" );
		if( myProperty != property )
		{
			setProperty( property, VariableImpl.index( property ) );
		}
		if( myProperty != null && myProperty.contains( value ) )
		{
			Struct next;
			for( Iterator it = myData.iterator(); it.hasNext(); )
			{
				next = (Struct) it.next();
				next.setValue( value );
			}
		}
		fireTableDataChanged();
	}

	public void refreshValues()
	{
		//new Throwable().printStackTrace();
		if( myData != null )
		{
			for( Iterator it = myData.iterator(); it.hasNext(); )
				((Struct) it.next()).refreshValues();
		}
		fireTableDataChanged();
		resetEditableState();
	}

	public void refreshValues( EnumProperty property )
	{
		if( myData != null )
		{
			for( Iterator it = myData.iterator(); it.hasNext(); )
				((Struct) it.next()).refreshValue( property );
		}
		fireTableDataChanged();
		resetEditableState();
	}

	public void refreshValue( FiniteVariable var, EnumProperty property )
	{
		if( myData != null )
		{
			Struct next;
			for( Iterator it = myData.iterator(); it.hasNext(); )
			{
				next = (Struct) it.next();
				if( next.variable == var ) next.refreshValue( property );
			}
		}
		fireTableDataChanged();
		resetEditableState();
	}

	public void commitValues()
	{
		if( myData != null )
		{
			for( Iterator it = myData.iterator(); it.hasNext(); )
				((Struct) it.next()).commitValues();
		}
	}

	public String getColumnName( int column )
	{
		if( column == 0 ) return "variable";
		else if( column == 1 ) return "property value";
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

	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return myFlagEditable && (columnIndex == 1);
	}

	/** @since 20070326 */
	public Struct getRow( int rowIndex ) throws Exception{
		return (Struct) myData.get( rowIndex );
	}

	public Object getValueAt( int rowIndex, int columnIndex )
	{
		Struct ret = (Struct) myData.get( rowIndex );
		if( columnIndex == 0 ) return ret.variable;
		//else if( columnIndex == 1 ) return ret.getRawValue();
		else if( columnIndex == 1 )
		{
			if( myProperty == null ) return null;
			else return ret.getRawValue();
		}
		else return null;
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if( columnIndex == 1 )
		{
			Struct struct = (Struct) myData.get( rowIndex );
			if( struct != null ) struct.setRawValue( aValue );
		}
	}

	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
	{
		column = table.convertColumnIndexToModel( column );
		if( column == 1 )
		{
			if( value == null || myProperty == null ) return myPanelBlank;
			else if( myProperty.isFlag() )
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
		public Struct( FiniteVariable variable )
		{
			this.variable = variable;
			this.values = new Object[ myArray.length ];
			this.flags = new boolean[ this.values.length ];
			Arrays.fill( this.flags, false );
		}

		/** @since 20070324 */
		public EnumValue rotate(){
			EnumValue ret = null;
			try{
				setValue( ret = AbstractEnumProperty.rotate( myProperty, getValue() ) );
			}catch( Exception exception ){
				System.err.println( "warning: EnumTableModel.rotate() caught " + exception );
			}
			return ret;
		}

		public void setValue( EnumValue value )
		{
			//if( myIsFlagProperty ) this.value = myProperty.toBoolean( value ) ? Boolean.TRUE : Boolean.FALSE;
			//else this.value = value;
			setValue( value, myCurrentPropertyIndex );
			this.flags[myCurrentPropertyIndex] = true;
		}

		private void setValue( EnumValue value, int index )
		{
			//System.out.println( "Struct("+variable+").setValue( "+value+", "+index+" )" );
			if( myArray[index].isFlag() ) this.values[index] = myArray[index].toBoolean( value ) ? Boolean.TRUE : Boolean.FALSE;
			else this.values[index] = value;
		}

		public void setRawValue( Object value )
		{
			//this.value = value;
			this.values[myCurrentPropertyIndex] = value;
			this.flags[myCurrentPropertyIndex] = true;
		}

		public EnumValue getValue()
		{
			//if( myIsFlagProperty ) return myProperty.valueOf( ((Boolean)this.value).booleanValue() );
			//else return (EnumValue)this.value;
			return getValue( myCurrentPropertyIndex );
		}

		/** @since 20070425 */
		public String toString(){
			return EnumTableModel.this.myProperty == null ?
																  this.variable.toString() :
				EnumTableModel.this.myProperty.getName() + "( " + this.variable.toString() + " ) = " + getValue().toString();
		}

		private EnumValue getValue( int index )
		{
			if( myArray[index].isFlag() ) return myArray[index].valueOf( ((Boolean)this.values[index]).booleanValue() );
			else return (EnumValue)this.values[index];
		}

		public Object getRawValue()
		{
			//System.out.println( "Struct("+variable+").getRawValue() <- " + this.values[myCurrentPropertyIndex] );
			//return this.value;
			return this.values[myCurrentPropertyIndex];
		}

		public void refreshValues()
		{
			Arrays.fill( this.flags, false );
			EnumValue val;
			for( int i=0; i<this.values.length; i++ )
			{
				val = variable.getProperty( myArray[i] );
				if( val == null ) val = myArray[i].getDefault();
				setValue( val, i );
			}
		}

		public void refreshValue( EnumProperty property )
		{
			EnumValue val;
			for( int i=0; i<this.values.length; i++ )
			{
				if( myArray[i] == property )
				{
					val = variable.getProperty( myArray[i] );
					if( val == null ) val = myArray[i].getDefault();
					setValue( val, i );
					flags[i] = false;
				}
			}
		}

		public void commitValues()
		{
			//System.out.println( "Struct("+variable+").commitValues()" );
			EnumProperty property;
			EnumValue value;
			for( int i=0; i<this.values.length; i++ )
			{
				if( flags[i] ){
					property = myArray[i];
					value = getValue(i);
					if( !property.contains( value ) ) throw new IllegalStateException( "EnumProperty " + property + " does not contain value \"" + value + "\"" );
					//System.out.println( "\t "+variable.getID()+".setProperty( "+property+", "+value+" )" );
					variable.setProperty( property, value );
				}
			}
		}

		public FiniteVariable variable;
		public Object[] values;
		public boolean[] flags;
		//public Object value;
		//public boolean flagValid = false;
	}

	public int compare( Object o1, Object o2 )
	{
		if( o1 instanceof Struct && o2 instanceof Struct )
		{
			Struct struct1 = (Struct)o1;
			Struct struct2 = (Struct)o2;

			return myComparator.compare( struct1.variable, struct2.variable );
		}
		else return (int)0;
	}

	private int                  myCurrentPropertyIndex = (int)0;
	protected BeliefNetwork        myBeliefNetwork;
	private EnumProperty[]       myArray;
	private Comparator           myComparator = VariableComparator.getInstance();
	private TableCellRenderer    myDefaultTableCellRenderer;
	private JCheckBox            myJCheckBox;
	private DefaultComboBoxModel myValueModel;
	private JPanel               myPanelBlank, myPanelError;
	protected List                 myData;
	protected EnumProperty         myProperty;
	private boolean              myIsFlagProperty = false;
	private boolean              myFlagStable     = false;
	private boolean              myFlagEditable   = false;
}
