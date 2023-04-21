package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.Variable;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.belief.ui.UI;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.FlagProperty;
import edu.ucla.util.UserEnumProperty;
import edu.ucla.util.UserEnumValue;

import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
//import javax.swing.event.*;

/** @author keith cascio
	@since  20030828 */
public class EnumValuesTableModel extends AbstractTableModel implements ActionListener
{
	public static final int NUM_COLS	= 2;
	public static final int COL_NAME	= 0;
	public static final int COL_DEFAULT	= 1;
	public final String[] COLUMN_NAMES = { "Value","Default" };

	public EnumValuesTableModel( boolean editable )
	{
		myFlagEditable = editable;
		addRadioButtons( 3 );
		myMapPropToStruct.put( null, EMPTY );
	}

	public void setProperty( UserEnumProperty virtual, UserEnumProperty actual )
	{
		//System.out.println( "EnumValuesTableModel.setProperty( "+UserEnumProperty.getDebugLabel( virtual )+" )" );
		saveRadioState();

		if( myProperty != virtual )
		{
			myProperty = virtual;

			myCurrent = (Struct) myMapPropToStruct.get( myProperty );
			if( myCurrent == null ) myCurrent = init( myProperty, actual );

			((JRadioButton)myRadioButtons.get( myCurrent.getIndexDefault() )).setSelected( true );

			fireTableDataChanged();
		}
	}

	private void saveRadioState()
	{
		if( myProperty != null ){
			myCurrent.setIndexDefault( getIndexSelectedRadio() );
		}
	}

	private Struct init( UserEnumProperty virtual, UserEnumProperty actual )
	{
		//System.out.println( "EnumValuesTableModel.init( "+UserEnumProperty.getDebugLabel( virtual )+" )" );
		addRadioButtons( virtual.size() - myRadioButtons.size() );
		Struct ret = new Struct( virtual, actual );
		myMapPropToStruct.put( virtual, ret );
		return ret;
	}

	private void addRadioButtons( int num )
	{
		//System.out.println( "EnumValuesTableModel.addRadioButtons( "+num+" )" );

		if( myRadioGroup == null ) myRadioGroup = new ButtonGroup();
		if( myRadioButtons == null ) myRadioButtons = new LinkedList();

		JRadioButton temp = null;
		for( int i=0; i < num; i++ )
		{
			temp = new JRadioButton();
			temp.addActionListener( this );
			myRadioGroup.add( temp );
			myRadioButtons.add( temp );
		}
	}

	/**
		interface ActionListener
		@since 112403
	*/
	public void actionPerformed( ActionEvent e )
	{
		fireTableDataChanged();
	}

	private int getIndexSelectedRadio()
	{
		int ret = (int)0;
		for( Iterator it = myRadioButtons.iterator(); it.hasNext(); )
		{
			if( ((JRadioButton)it.next()).isSelected() ) return ret;
			++ret;
		}
		return (int)-1;
	}

	public static final String STR_PREFIX_NEW_VALUE = "value";
	transient protected int INT_COUNTER_NEW_VALUES = (int)2;

	public boolean addValue( int index )
	{
		if( index < 0 ) index = 0;
		else ++index;

		if( index <= myCurrent.size() )
		{
			String strNew = null;
			do strNew = STR_PREFIX_NEW_VALUE + String.valueOf( INT_COUNTER_NEW_VALUES++ );
			while( myCurrent.contains( strNew ) );

			EnumValue objNew = myCurrent.newValue( strNew );

			//myCurrent.list.add( index, objNew );
			//myCurrent.set.add( strNew );
			myCurrent.add( objNew, strNew, index );

			addRadioButtons( myCurrent.size() - myRadioButtons.size() );
			int indexSelectedRadio = getIndexSelectedRadio();
			if( index <= indexSelectedRadio ) ++indexSelectedRadio;
			((JRadioButton)myRadioButtons.get( indexSelectedRadio )).setSelected( true );

			myCurrent.valuesEdited = true;
			fireTableRowsInserted(index, index);

			return true;
		}
		else return false;
	}

	public boolean removeValue( int index )
	{
		//System.out.println( "EnumValuesTableModel.removeValue( "+index+" )" );
		if( myCurrent.size() > 2 )
		{
			if( index < 0 ) index = 0;

			if( index < myCurrent.size() )
			{
				//Object objOld = myCurrent.list.remove( index );
				//myCurrent.set.remove( objOld.toString() );
				Object objOld = myCurrent.remove( index );

				int indexSelectedRadio = getIndexSelectedRadio();
				if( indexSelectedRadio == index ) indexSelectedRadio = (int)0;
				else if( indexSelectedRadio > index ) --indexSelectedRadio;
				((JRadioButton)myRadioButtons.get( indexSelectedRadio )).setSelected( true );

				myCurrent.valuesEdited = true;
				fireTableRowsDeleted(index, index);

				return true;
			}
			else return false;
		}
		else return false;
	}

	public int getRowCount()
	{
		return myCurrent.size();
	}

	public int getColumnCount()
	{
		return NUM_COLS;
	}

	public Object getValueAt(int row, int column)
	{
		switch( column )
		{
			case COL_NAME:
			return   myCurrent.get( row );
			case COL_DEFAULT:
			return myRadioButtons.get( row );
			default:
			return "ERROR";
		}
	}

	protected void showErrorMessage( String msg )
	{
		JOptionPane.showMessageDialog( null, msg, "Invalid value name", JOptionPane.ERROR_MESSAGE );
	}

	public void setValueAt( Object value, int row, int column )
	{
		switch( column )
		{
			case COL_NAME:
				String strValue = null;
				if( value instanceof String ) value = strValue = ((String)value).trim();
				else strValue = value.toString();
				if( myCurrent.contains( strValue ) )
				{
					if( !myCurrent.get( row ).equals( value ) )
					{
						showErrorMessage( myProperty.toString() + " already contains value \""+value+"\"." );
					}
				}
				else if( value.toString().length() <= 0 )
				{
					showErrorMessage( UI.STR_SAMIAM_ACRONYM + " does not allow blank value names." );
				}
				else
				{
					Object objOld = myCurrent.get( row );
					//System.out.println( "objOld == " + objOld + ", value == " + value );
					if( !objOld.equals( value ) )
					{
						//myCurrent.set.remove( objOld.toString() );
						//myCurrent.set.add( strValue );
						myCurrent.exchange( objOld.toString(), strValue );
						if( objOld instanceof UserEnumValue )
						{
							//System.out.println( "Renaming UserEnumValue" );
							((UserEnumValue)objOld).setName( value );
						}
						else
						{
							//System.out.println( "Replacing " + objOld.getClass().getName() );
							myCurrent.set( row, myCurrent.newValue( value ) );
							myCurrent.valuesEdited = true;
						}
					}
				}
				break;
			case COL_DEFAULT:
				break;
			default:
				break;
		}
	}

	public String getColumnName( int column )
	{
		return COLUMN_NAMES[column];
	}

	public Class getColumnClass( int column )
	{
		switch( column )
		{
			case COL_NAME:
			return String.class;
			case COL_DEFAULT:
			return JRadioButton.class;
			default:
			return Object.class;
		}
	}

	public boolean isCellEditable( int row, int column )
	{
		return myFlagEditable;
	}

	public static class Struct
	{
		public Struct( UserEnumProperty virtual, UserEnumProperty actual ){
			LinkedList list = new LinkedList();
			int capacity = virtual.size() + 3;
			HashSet set = new HashSet( capacity );
			Map mapVirtualToActual = null, mapActualToVirtual = null;
			EnumValue[] array = virtual.valuesAsArray(), arrayActual = null;

			boolean flagRememberActual = actual != null;
			if( flagRememberActual ){
				mapVirtualToActual = new HashMap( capacity );
				mapActualToVirtual = new HashMap( capacity );
				arrayActual = actual.valuesAsArray();
			}

			EnumValue def = virtual.getDefault();
			//System.out.println( "\t default == " + def );
			int indexDefault = (int)0;
			for( int i=0; i<array.length; i++ )
			{
				list.add( array[i] );
				set.add( array[i].toString() );
				if( array[i] == def ) indexDefault = i;

				if( flagRememberActual ){
					mapVirtualToActual.put( array[i], arrayActual[i] );
					mapActualToVirtual.put( arrayActual[i], array[i] );
				}
			}
			//System.out.println( "\t index default == " + indexDefault );
			this.myList = list;
			this.mySet = set;
			this.myMapVirtualToActual = mapVirtualToActual;
			this.myMapActualToVirtual = mapActualToVirtual;
			this.myIndexDefault = indexDefault;
			this.myActual = actual;
			this.myVirtual = virtual;
		}

		public Struct( List list, Set set, int indexDefault ){
			this.myList = list;
			this.mySet = set;
			this.myIndexDefault = indexDefault;
		}

		/** @since 20070420 */
		public UserEnumValue newValue( Object name ){
			return new UserEnumValue( name, this.myActual == null ? this.myVirtual : this.myActual );
		}

		public boolean validate()
		{
			boolean ret = (this.myList.size() == this.mySet.size()) && (this.myIndexDefault < this.myList.size());
			if( !ret )
			{
				System.err.println( "Warning: Struct.validate() failed" );
				System.err.println( "list(" + this.myList.size() + ") " + this.myList );
				System.err.println( "set(" + this.mySet.size() + ") " + this.mySet);
				System.err.println( "indexDefault " + this.myIndexDefault );
			}
			return ret;
		}

		public List getList(){
			return this.myList;
		}

		public Object get( int row ){
			return this.myList.get( row );
		}

		public void set( int row, EnumValue value ){
			this.myList.set( row, value );
		}

		public int size(){
			return this.myList.size();
		}

		public void add( EnumValue objNew, String strNew, int index ){
			this.myList.add( index, objNew );
			this.mySet.add( strNew );
			//this.myMapVirtualToActual.put( objNew, null );
		}

		public Object remove( int index ){
			Object objOld = this.myList.remove( index );
			this.mySet.remove( objOld.toString() );
			if( (myMapVirtualToActual != null) && myMapVirtualToActual.containsKey( objOld ) ){
				Object actual = myMapVirtualToActual.get( objOld );
				if( myMapActualToVirtual != null ) this.myMapActualToVirtual.put( actual, null );
			}
			return objOld;
		}

		public Object getDefault(){
			return this.myList.get( this.myIndexDefault );
		}

		public boolean contains( String strValue ){
			return this.mySet.contains( strValue );
		}

		public void exchange( String strOld, String strValue ){
			this.mySet.remove( strOld );
			this.mySet.add( strValue );
		}

		public int getIndexDefault(){
			return myIndexDefault;
		}

		public void setIndexDefault( int newIndex ){
			if( this.valuesEdited || this.myIndexDefault != newIndex )
			{
				this.myIndexDefault = newIndex;
				this.defaultEdited = true;
			}
		}

		public EnumValue translate( EnumValue actual ){
			if( (actual == null) || (myMapActualToVirtual == null) ) return null;
			else return (EnumValue) myMapActualToVirtual.get( actual );
		}

		public UserEnumProperty getPropertyEffective(){
			return ( myActual == null ) ? myVirtual : myActual;
		}

		public List myList;
		public Set mySet;
		public Map myMapVirtualToActual;
		public Map myMapActualToVirtual;
		public int myIndexDefault;
		public UserEnumProperty myActual, myVirtual;
		public boolean valuesEdited = false;
		public boolean defaultEdited = false;
	}

	public void commit( BeliefNetwork bn )
	{
	  //Util.STREAM_TEST.println( "\nEVTM.commit(){" );
		saveRadioState();

		UserEnumProperty propActual,  propVirtual;
		EnumValue        valueActual, translation;
		Struct           struct;
		Variable         var;
		for( Iterator it = myMapPropToStruct.keySet().iterator(); it.hasNext(); )
		{
			if( (propVirtual = (UserEnumProperty) it.next()) == null ) continue;
		  //Util.STREAM_TEST.println( "    committing " + propVirtual + "{" );
			if( ! (struct = (Struct) myMapPropToStruct.get( propVirtual )).validate() ){
				System.err.println( "warning: user property " + propVirtual + " failed validation" );
				continue;
			}

			if( struct.valuesEdited ){
				propVirtual.setValues( struct.getList() );
				struct.valuesEdited = false;
			}
			if( struct.defaultEdited ){
				propVirtual.setDefault( (EnumValue) struct.getDefault() );
				struct.defaultEdited = false;
			}

		  /*Util.STREAM_TEST.println( "      {" );
			EnumValue[] values = propVirtual.valuesAsArray();
			for( int i=0; i<values.length; i++ ){
				String hash = Integer.toString( System.identityHashCode( values[i] ) );
				Util.STREAM_TEST.println( "        " + i + ": " + values[i] + "          ".substring( values[i].toString().length() ) + "        ".substring( hash.length() ) + hash );
			}
			Util.STREAM_TEST.println( "      }" );*/

			propActual = struct.getPropertyEffective();
			for( Iterator varIt = bn.iterator(); varIt.hasNext(); ){
				valueActual = (var = (Variable) varIt.next()).getProperty( propActual );
			  //String hash = Integer.toString( System.identityHashCode( valueActual ) );
			  //Util.STREAM_TEST.println( "      inspecting " + var + "                      ".substring( var.toString().length() ) + " = " + valueActual + "          ".substring( valueActual.toString().length() ) + "        ".substring( hash.length() ) + hash );
				if( (valueActual != null) && (!propVirtual.contains( valueActual )) ){
					var.setProperty( propActual, translation = struct.translate( valueActual ) );
				  //Util.STREAM_TEST.println( "        translated " + valueActual + " " + System.identityHashCode( valueActual ) + " -> " + translation + " " + System.identityHashCode( translation ) );
				}
			}
		  //Util.STREAM_TEST.println( "    }" );
		}
	  //Util.STREAM_TEST.println( "}" );
	}

	public void clear()
	{
		setProperty( null, null );
		myMapPropToStruct.clear();
		myMapPropToStruct.put( null, EMPTY );
	}

	public static final Struct EMPTY = new Struct( Collections.EMPTY_LIST, Collections.EMPTY_SET, 0 );

	protected UserEnumProperty myProperty;
	protected ButtonGroup myRadioGroup;
	protected LinkedList myRadioButtons;
	protected Map myMapPropToStruct = new HashMap();
	protected Struct myCurrent = EMPTY;
	protected boolean myFlagEditable = false;
}
