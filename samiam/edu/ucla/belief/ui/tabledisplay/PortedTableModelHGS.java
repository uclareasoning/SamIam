package edu.ucla.belief.ui.tabledisplay;

//import edu.ucla.belief.io.StandardNode;
import edu.ucla.belief.ui.util.Util;
//import edu.ucla.belief.*;

//import java.util.List;
import java.util.EventObject;
import java.util.Enumeration;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
	@author Keith Cascio
	@since 100604
*/
public class PortedTableModelHGS extends PortedTableModel implements HuginGenieStyleTableFactory.TableModelHGS, TableModel, TableCellEditor, TableCellRenderer
{
	public PortedTableModelHGS( HuginGenieStyleTableFactory.TableModelHGS tablemodelhgs ){
		super( tablemodelhgs );
		this.myTableModelHGS = tablemodelhgs;
		this.myNumColumnsWrapped = myTableModelHGS.getColumnCount();
	}

	/** @since 030905 */
	public HuginGenieStyleTableFactory.TableModelHGS getUnderlyingHGS(){
		return myTableModelHGS;
	}

	/** interface HuginGenieStyleTableFactory.TableModelHGS
		@since 030705 */
	public int calculateDataIndex( int row, int column ){
		return myTableModelHGS.calculateDataIndex( row, this.convertColumnIndex( column ) );
	}

	public void setColumnGroup( int columnIndex, ColumnGroup cg ){
		if( myColumnGroups == null ) myColumnGroups = new ColumnGroup[ myNumColumnsWrapped ];
		myColumnGroups[columnIndex] = cg;
	}

	public ColumnGroup getColumnGroup( int columnIndex ){
		if( myColumnGroups == null ) return (ColumnGroup) null;
		else return myColumnGroups[ convertColumnIndex( columnIndex ) ];
	}

	public void setEditable( boolean editable ){
		myTableModelHGS.setEditable( editable );
	}

	public boolean hasExclude(){
		return myTableModelHGS.hasExclude();
	}

	public void configure( JTable jtable ){
		myTableModelHGS.configure( jtable );
	}

	public void configure( JTable jtable, DataHandler userHandler, boolean isHeader ){
		myTableModelHGS.configure( jtable, userHandler, isHeader );
	}

	/** @author Keith Cascio @since 101804 */
	/*
	public void snapshotGroupableState( JTable jtable )
	{
		//System.out.println( "PortedTableModelHGS["+getUnderlyingClassName()+"].snapshotGroupableState()" );
		//Util.printStackTrace( 4, System.out );

		JTableHeader header = jtable.getTableHeader();
		if( !(header instanceof GroupableTableHeader) ) return;

		TableColumnModel tcm = jtable.getColumnModel();
		int numCols = tcm.getColumnCount();

		if( (myGroupableState == null) || (myGroupableState.length != numCols) ) myGroupableState = new ColumnGroup.ColumnID[numCols];
		else return;

		int i=0;
		TableColumn tc;
		Enumeration enumeration = tcm.getColumns();
		if( enumeration.hasMoreElements() ) enumeration.nextElement();
		while( enumeration.hasMoreElements() ){
			tc = (TableColumn) enumeration.nextElement();
			if( tc.getIdentifier() instanceof ColumnGroup.ColumnID ){
				myGroupableState[i] = (ColumnGroup.ColumnID)tc.getIdentifier();
			}
			++i;
		}
	}*/

	//private ColumnGroup.ColumnID[] myGroupableState;

	/** @author Keith Cascio @since 101804 */
	/*
	public void resetGroupableState( JTable jtable )
	{
		//System.out.println( "PortedTableModelHGS["+getUnderlyingClassName()+"].resetGroupableState()" );

		JTableHeader header = jtable.getTableHeader();
		if( !(header instanceof GroupableTableHeader) ) return;

		TableColumnModel tcm = jtable.getColumnModel();
		int numCols = tcm.getColumnCount();

		if( myGroupableState == null ) throw new IllegalStateException();

		int i=getOffset();
		TableColumn tc;
		Enumeration enumeration = tcm.getColumns();
		if( enumeration.hasMoreElements() ) enumeration.nextElement();
		while( enumeration.hasMoreElements() ){
			tc = (TableColumn) enumeration.nextElement();
			if( myGroupableState[i] != null ){
				//System.out.println( "\t  old column id ("+tc.getIdentifier().getClass().getName()+") " + tc.getIdentifier() );
				//System.out.println( "\t  new column id ("+myGroupableState[i].getClass().getName()+") " + myGroupableState[i] );
				tc.setIdentifier( myGroupableState[i] );
			}
			++i;
		}

		//System.out.println( "\t DONE resetGroupableState()" );
		return;
	}*/

	/** @since 030105 */
	public int convertColumnIndexToConditionIndex( int column ){
		return myTableModelHGS.convertColumnIndexToConditionIndex( column );
	}

	/** @since 022805 */
	public void fireTableDataChanged(){
		myTableModelHGS.fireTableDataChanged();
	}

	public boolean isProbabilityEdited(){
		return myTableModelHGS.isProbabilityEdited();
	}

	/** @since 022805 */
	public void setProbabilityEdited(){
		myTableModelHGS.setProbabilityEdited();
	}

	public boolean isExcludeDataEdited(){
		return myTableModelHGS.isExcludeDataEdited();
	}

	public DataHandler[] getDataHandlers(){
		return myTableModelHGS.getDataHandlers();
	}

	public void setDataHandler( DataHandler handler ){
		myTableModelHGS.setDataHandler( handler );
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
		return myTableModelHGS.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column){
		return myTableModelHGS.getTableCellEditorComponent( table, value, isSelected, row, column );
	}

	public Object getCellEditorValue() { return myTableModelHGS.getCellEditorValue(); }
	public boolean isCellEditable(EventObject anEvent) { return myTableModelHGS.isCellEditable(anEvent); }
	public boolean shouldSelectCell(EventObject anEvent) { return myTableModelHGS.shouldSelectCell(anEvent); }
	public boolean stopCellEditing() { return myTableModelHGS.stopCellEditing(); }
	public void cancelCellEditing() { myTableModelHGS.cancelCellEditing(); }
	public void addCellEditorListener(CellEditorListener l){
		myTableModelHGS.addCellEditorListener( l );
	}
	public void removeCellEditorListener(CellEditorListener l){
		myTableModelHGS.removeCellEditorListener( l );
	}

	private HuginGenieStyleTableFactory.TableModelHGS myTableModelHGS;
	private int myNumColumnsWrapped;
	private ColumnGroup[] myColumnGroups;
}
