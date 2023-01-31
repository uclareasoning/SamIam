package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.clipboard.InstantiationClipBoard;
import edu.ucla.belief.ui.actionsandmodes.*;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Filter;
import edu.ucla.belief.ui.internalframes.OutputPanel5.GrepField;

import edu.ucla.belief.Variable;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.VariableInstance;
import edu.ucla.util.Stringifier;

import java.awt.Component;
import java.awt.Color;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;

/** An enhanced version of OutputPanel that uses features of java 5, java 6 and beyond.
	@author keith cascio
	@since  20070321 */
public class OutputPanel5 extends OutputPanel implements Grepable<VariableInstance,GrepField,Object>
{
	public OutputPanel5( Map<? extends FiniteVariable,?> data, Collection<? extends FiniteVariable> variables ){
		super( data, variables, false );
	}

	public OutputPanel5( Map<? extends FiniteVariable,?> data, Collection<? extends FiniteVariable> variables, boolean useIDRenderer ){
		super( data, variables, useIDRenderer );
	}

	/** Define the grepable fields, i.e. the table columns.
		@author keith cascio
		@since  20070321 */
	public enum GrepField { varbls, values }

	/** @since 20070321 */
	public boolean isTiger(){ return true; }

	/** @since 20070312 */
	public void setClipboard( InstantiationClipBoard clipboard ){
		try{
			if( myInstantiationClipBoard != clipboard ){
				myInstantiationClipBoard = clipboard;
				if( myJTable != null ) myJTable.repaint();
			}
		}catch( Exception exception ){
			System.err.println( "warning: OutputPanel5.setClipBoard() caught " + exception );
		}
	}

	/** @since 20070312 */
	public void setDiff( boolean flag ){
		try{
			boolean not = ! flag;
			if( myFlagNoDiff != not ){
				myFlagNoDiff = not;
				if( myJTable != null ) myJTable.repaint();
			}
		}catch( Exception exception ){
			System.err.println( "warning: OutputPanel5.setDiff() caught " + exception );
		}
	}

	/** @since 20070326 */
	static public Method getMethodConvertRowIndexToModel(){
		if( FLAG_METHODS_CRI ) return METHOD_CRITM;

		try{
			METHOD_CRITM = JTable.class.getMethod( "convertRowIndexToModel", Integer.class );
			METHOD_CRITV = JTable.class.getMethod( "convertRowIndexToView",  Integer.class );
		}catch( Exception exception ){
			if( Util.DEBUG_VERBOSE ) System.err.println( "warning: OutputPanel5.getMethodConvertRowIndexToModel() caught " + exception );
		}
		try{
			if( METHOD_CRITM == null || METHOD_CRITV == null ){
				String name;
				for( Method method : JTable.class.getMethods() )
					if(     (name = method.getName()).equals( "convertRowIndexToModel" ) )
						METHOD_CRITM = method;
					else if( name                    .equals( "convertRowIndexToView"  ) )
						METHOD_CRITV = method;
			}
		}catch( Exception exception ){
			if( Util.DEBUG_VERBOSE ) System.err.println( "warning: OutputPanel5.getMethodConvertRowIndexToModel() caught " + exception );
		}

		FLAG_METHODS_CRI = true;
		return METHOD_CRITM;
	}

	/** @since 20070421 */
	static public Method getMethodConvertRowIndexToView(){
		if( FLAG_METHODS_CRI ) return METHOD_CRITV;
		getMethodConvertRowIndexToModel();
		return METHOD_CRITV;
	}
	private static Method  METHOD_CRITM, METHOD_CRITV;
	private static boolean FLAG_METHODS_CRI = false;

	/** @since 20070312 */
	public class DiffCellRenderer implements TableCellRenderer{
		private TableCellRenderer        myDefaultRenderer;
		private java.lang.reflect.Method myMethod;

		public DiffCellRenderer( TableCellRenderer defaultRenderer ){
			myDefaultRenderer = defaultRenderer;
			myMethod          = getMethodConvertRowIndexToModel();
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			//System.out.println( "DiffCellRenderer.gTCRC( "+value+", "+row+", "+column+" ) " + (!myFlagNoDiff) + " && " + (myInstantiationClipBoard != null) );
			JLabel ret = (JLabel) myDefaultRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			try{
				if( isSelected ) return ret;
				if( myFlagNoDiff || (myInstantiationClipBoard == null) ){
					ret.setBackground( SloppyPanel.COLOR_DEFAULT );
					return ret;
				}
				//row = table.convertRowIndexToModel( row );
				if( myMethod != null ) row = ((Integer) myMethod.invoke( table, row )).intValue();

				Color   background = SloppyPanel.COLOR_DIFF_EQUALS;
				Object  key        = myTableModel.getValueAt( row, 0 ), valueOnClipboard = null;
				boolean contains   = myInstantiationClipBoard.containsKey( key );
				if( ! contains ){
					key            = ((Variable) key).getID();
					contains       = myInstantiationClipBoard.containsKey( key );
				}
				//if( contains ) System.out.println( "@clipboard=" + myInstantiationClipBoard.get( key ) + ", @table=" + myTableModel.getValueAt( row, 1 ) );
				if( contains && (!myTableModel.getValueAt( row, 1 ).equals( myInstantiationClipBoard.get(key) )) ) background = SloppyPanel.COLOR_DIFF_NOTEQUALS;
				ret.setBackground( background );
			}catch( Exception exception ){
				System.err.println( "warning: OutputPanel5.getTableCellRendererComponent() caught " + exception );
			}
			return ret;
		}
	}

	protected void setupLeftmostColumn( Collection variables, TableCellRenderer defaultrenderer ){
		TableCellRenderer renderer = (defaultrenderer == null) ? getDiffCellRenderer() : defaultrenderer;
		super.setupLeftmostColumn( variables, renderer );
		myJTable.getColumnModel().getColumn(1).setCellRenderer( renderer );
	}

	private DiffCellRenderer getDiffCellRenderer(){
		if( myDiffRenderer == null ) myDiffRenderer = new DiffCellRenderer( new DefaultTableCellRenderer() );
		return myDiffRenderer;
	}

	/** @since 20091110 */
	protected void init( Map data, Collection variables ){
		super.init( data, variables );
		try{
			Class       clazz  = Class.forName( "javax.swing.table.TableRowSorter" );
			Method      meth   = JTable.class.getMethod( "setAutoCreateRowSorter", Boolean.class );
			if( meth   != null ){ meth.invoke( myJTable, Boolean.TRUE ); }

			myMethodModelStructureChanged = clazz.getMethod( "modelStructureChanged" );
			myMethodGetRowSorter          = JTable.class.getMethod( "getRowSorter", (Class[]) null );
		}catch( Throwable throwable ){
			if( Util.DEBUG_VERBOSE ) System.err.println( "warning: OutputPanel5.init() caught " + throwable );
		}
	}

	/** @since 20070311 */
	private void modelStructureChanged() throws Exception{
		if( myMethodGetRowSorter == null ){ return; }
		myRowSorter = myMethodGetRowSorter.invoke( (Object[]) null );
		if( myRowSorter == null || myMethodModelStructureChanged == null ){ return; }
		myMethodModelStructureChanged.invoke( myRowSorter );
	}

	/** @since 20070311 */
	public Appendable append( Appendable buff, Stringifier ifier ) throws Exception{
		if( myJTable == null ) return buff;
		int rows = myJTable.getRowCount();
		int cols = myJTable.getColumnCount();
		Object value = null;
		String data  = null;
		for( int row = 0; row<rows; row++ ){
			value = myJTable.getValueAt(row,0);
			data  = (ifier == null) ? value.toString() : ifier.objectToString( value );
			buff.append( data ).append( '=' ).append( myJTable.getValueAt(row,1).toString() ).append( '\n' );
		}
		return buff;
	}

	/** interface Grepable
		@since 20070321 */
	public Class<GrepField> grepFields(){
		return GrepField.class;
	}

	/** interface Grepable
		@since 20070321 */
	public EnumSet<GrepField> grepFieldsDefault(){
		return EnumSet.of( GrepField.values );
	}

	/** interface Grepable
		@since 20070321 */
	public String grepInfo(){
		return "select rows that match the pattern";
	}

	/** interface Grepable
		@since 20070311
		@since 20070321 */
	public long grep( Filter filter, EnumSet<GrepField> field_selector, Stringifier ifier, Object state, Collection<VariableInstance> results ){
		long matches = 0;
		int  head    = 0;
		try{
			Thread                thread  = Thread.currentThread();
			ArrayList<Object[]>   in      = new ArrayList<Object[]>( myTableModel.data.length );
			ArrayList<Object[]>   out     = new ArrayList<Object[]>( myTableModel.data.length );
			Object                next    = null;
			Object[]              rowmodel= null;
			String                data    = null;
			StringBuilder         buff    = null;
			int                   width   = myTableModel.columnNames.length, rowcount = myJTable.getRowCount();
			int                   numfs   = field_selector == null ? 0 : field_selector.size();
			int                   column  = -1, rowindexview = 0;
			Method                critv   = getMethodConvertRowIndexToView();
			boolean               additive= filter.flags().contains( Flag.additive ), selected = false, match;
			if(      numfs >  1 ) buff    = new StringBuilder( 0x80 );
			else if( numfs == 1 ) column  = field_selector.iterator().next().ordinal();
			else if( numfs <  1 ) column  = 1;

			for( int rowindexmodel=0; rowindexmodel<rowcount; rowindexmodel++ ){
				rowmodel = myTableModel.data[ rowindexmodel ];

				if( additive ){
					if( critv != null ) rowindexview = ((Integer) critv.invoke( myJTable, rowindexmodel )).intValue();
					selected = myJTable.isRowSelected( rowindexview );
				}

				if( numfs <= 1 ){
					next = rowmodel[column];
					data = (ifier == null) ? next.toString() : ifier.objectToString( next );
				}
				else{
					buff.setLength(0);
					for( Object coldata : rowmodel ){
						buff.append( (ifier == null) ? coldata.toString() : ifier.objectToString( coldata ) );
						buff.append( ' ' );
					}
					data = buff.toString();
				}
				if( thread.isInterrupted() ) break;
				((match = filter.accept( data )) || selected ? in : out).add( rowmodel );
				if( match ) ++matches;
			}

			if( (head = in.size()) > 0 ){
				int index = 0;
				for( Object[] row : in  ) myTableModel.data[index++] = row;
				for( Object[] row : out ) myTableModel.data[index++] = row;

				if( results != null ){
					for( Object[] row : in ) results.add( new VariableInstance( (FiniteVariable) row[0], row[1] ) );
				}
			}
		}catch( Exception exception ){
			System.err.println( "warning: OutputPanel5.grep() caught " + exception );
		}

		try{
			myTableModel.fireTableDataChanged();
			this.modelStructureChanged();
			if( head > 0 ) myJTable.getSelectionModel().setSelectionInterval(0,head-1);
		}catch( Exception exception ){
			System.err.println( "warning: OutputPanel5.grep() caught " + exception );
		}

		return matches;
	}

	private DiffCellRenderer       myDiffRenderer;
	private InstantiationClipBoard myInstantiationClipBoard;
	private boolean                myFlagNoDiff = true;
	private Object                 myRowSorter;
	private Method                 myMethodModelStructureChanged, myMethodGetRowSorter;
}
