package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.FlagProperty;
import edu.ucla.util.UserEnumProperty;

import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;

import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/**
	@author Keith Cascio
	@since 082703
*/
public class UserPropertyTableModel extends AbstractTableModel implements TableCellRenderer
{
	public UserPropertyTableModel()
	{
		init();
	}

	public void init()
	{
		myData = new ArrayList();

		myPanelBlank = new JPanel();

		myPanelError = new JPanel();
		myPanelError.add( new JLabel( "error" ) );
	}

	public void clear()
	{
		myData.clear();
		myDeleted.clear();
		fireTableDataChanged();
	}

	public void editProperties( Collection properties )
	{
		clear();
		for( Iterator it = properties.iterator(); it.hasNext(); ) addActual( (UserEnumProperty) it.next() );
	}

	public void addActual( UserEnumProperty property )
	{
		add( new Struct( property ) );
	}

	public void addVirtual( UserEnumProperty property )
	{
		add( new Struct( null, property ) );
	}

	private void add( Struct struct )
	{
		myData.add( struct );
		fireTableDataChanged();
	}

	public void removeProperty( int index )
	{
		if( 0 <= index && index < myData.size() )
		{
			Struct struct = (Struct) myData.remove( index );
			myDeleted.add( struct.commit( (BeliefNetwork) null ) );
			fireTableDataChanged();
		}
	}

	public void setDefaultTableCellRenderer( TableCellRenderer renderer )
	{
		myDefaultTableCellRenderer = renderer;
	}

	public String getColumnName( int column )
	{
		if( column == 0 ) return "Properties";
		else return "getColumnName() Error";
	}

	public int getRowCount()
	{
		return myData.size();
	}

	public int getColumnCount()
	{
		return 1;
	}

	private boolean myFlagEditable = true;
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return myFlagEditable;
	}

	public Object getValueAt( int rowIndex, int column )
	{
		Struct struct = (Struct) myData.get( rowIndex );
		UserEnumProperty property = struct.virtual;

		if( column == 0 ) return property;

		return null;
	}

	/** @since 20050823 */
	public UserEnumProperty getActualValueAt( int rowIndex ){
		return ((Struct) myData.get( rowIndex )).actual;
	}

	public void setValueAt(Object aValue, int rowIndex, int column )
	{
		Struct struct = (Struct) myData.get( rowIndex );
		UserEnumProperty property = struct.virtual;

		if( column == 0 )
		{
			String errmsg = null;
			StringTokenizer toker = null;
			String id = null;
			String name = aValue.toString();
			if( name.length() < 1 ) errmsg = "Blank property name invalid.";

			if( errmsg == null )
			{
				toker = new StringTokenizer( name, " \t\n\r\b" );
				if( !toker.hasMoreTokens() ) errmsg = "Invalid property name.";
			}

			if( errmsg == null )
			{
				id = toker.nextToken();
				while( toker.hasMoreTokens() ) id += toker.nextToken();
			}

			if( !property.getName().equals( name ) )
			{
				if( errmsg == null )
				{
					if( !VariableImpl.validatePropertyNameAndID( name, id ) ) errmsg = "System property \"";
					else if( !validatePropertyNameAndID( name, id ) ) errmsg = "Property \"";
				}

				if( errmsg == null )
				{
					property.setName( name );
					property.setID( id );
				}
				else showErrorMessage( errmsg + name + "\" already exists." );
			}
		}
	}

	private boolean validatePropertyNameAndID( String name, String id )
	{
		UserEnumProperty property;
		for( Iterator it = myData.iterator(); it.hasNext(); )
		{
			property = ((Struct)it.next()).virtual;
			if( property.getName().equals( name ) || property.getID().equals( id ) ) return false;
		}
		return true;
	}

	protected void showErrorMessage( String msg )
	{
		JOptionPane.showMessageDialog( null, msg, "Invalid property name", JOptionPane.ERROR_MESSAGE );
	}

	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
	{
		column = table.convertColumnIndexToModel( column );

		if( myDefaultTableCellRenderer == null ) return myPanelError;
		else return myDefaultTableCellRenderer.getTableCellRendererComponent( table, value, isSelected, false, row, column );
	}

	public void commit( BeliefNetwork network, Collection oldProperties )
	{
		//System.out.println( "UserPropertyTableModel.commit()" );

		Collection properties = new ArrayList( myData.size() );

		for( Iterator it = myData.iterator(); it.hasNext(); )
		{
			properties.add( ((Struct)it.next()).commit( network ) );
		}

		network.setUserEnumProperties( properties );

		myDeleted.retainAll( oldProperties );
		//System.out.println( "deleting " + myDeleted );

		if( !myDeleted.isEmpty() )
		{
			myFlagPropertyDeleted = true;

			Variable var;
			for( Iterator it = network.iterator(); it.hasNext(); )
			{
				var = (Variable)it.next();
				for( Iterator itit = myDeleted.iterator(); itit.hasNext(); )
				{
					var.delete( (EnumProperty)itit.next() );
				}
			}
		}
	}

	/** @since 010904 */
	public boolean wasPropertyDeleted()
	{
		return myFlagPropertyDeleted;
	}

	public class Struct
	{
		public Struct( UserEnumProperty actual )
		{
			this.actual = actual;
			this.virtual = new UserEnumProperty( actual );
			this.virtual.setDebugID( "_VIRTUAL" + Integer.toString( INT_DEBUG_COUNTER++ ) );
			//System.out.println( virtual.getDebugLabel() + ".getDefault() == " + virtual.getDefault() );
		}

		public Struct( UserEnumProperty actual, UserEnumProperty virtual )
		{
			this.actual = actual;
			this.virtual = virtual;
		}

		public UserEnumProperty commit( BeliefNetwork bn )
		{
			//System.out.println( "(UPTM.Struct)"+virtual+".commit()" );

			if( actual == null ) return virtual;
			else
			{
				if( bn != null ){
					Variable var;
					EnumValue valueActual, translation;
					for( Iterator varIt = bn.iterator(); varIt.hasNext(); ){
						var = (Variable) varIt.next();
						//System.out.println( "    inspecting " + var );
						valueActual = var.getProperty( actual );
						if( (valueActual != null) && (!virtual.contains( valueActual )) ){
							translation = this.actualToVirtual( valueActual );
							var.setProperty( actual, translation );
							//System.out.println( "      translated " + valueActual + " -> " + translation );
						}
					}
				}

				actual.assume( virtual );
				return actual;
			}
		}

		/** @since 20050823 */
		public EnumValue actualToVirtual( EnumValue valueActual ){
			return virtual.forIndex( actual.indexOf( valueActual ) );
		}

		public UserEnumProperty actual;
		public UserEnumProperty virtual;
	}

	private static int INT_DEBUG_COUNTER = (int)0;

	private JPanel myPanelBlank;
	private JPanel myPanelError;
	private List myData;
	private List myDeleted = new LinkedList();
	private TableCellRenderer myDefaultTableCellRenderer;
	private boolean myFlagPropertyDeleted = false;
}
