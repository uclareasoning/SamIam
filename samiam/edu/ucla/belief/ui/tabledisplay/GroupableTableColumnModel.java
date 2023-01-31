package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.util.Interruptable;

//import edu.ucla.belief.ui.util.Interruptable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Arrays;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Class which extends the functionality of DefaultColumnTableModel to
 * also provide capabilities to group columns. This can be used for
 * instance to aid in the layout of groupable table headers.
 * @author Steve Webb 16/09/04 swebb99_uk@hotmail.com
 */
public class GroupableTableColumnModel extends DefaultTableColumnModel
	implements PortedTableModel.Listener
	//, ChangeListener
	//, ListSelectionListener, TableModelListener
{
    /**
     * Hold the list of ColumnGroups which define what group each normal
     * column is within, if any.
     */
    protected ArrayList columnGroups = new ArrayList();


    /**
     * Add a new columngroup.
     * @param columnGroup new ColumnGroup
     */
    public void addColumnGroup(ColumnGroup columnGroup) {
        columnGroups.add(columnGroup);
    }

    /**
     * Provides an Iterator to iterate over the
     * ColumnGroup list.
     * @return Iterator over ColumnGroups
     */
    public Iterator columnGroupIterator() {
        return columnGroups.iterator();
    }

    /**
     * Returns a ColumnGroup specified by an index.
     * @param index index of ColumnGroup
     * @return ColumnGroup
     */
    public ColumnGroup getColumnGroup(int index) {
        if(index >= 0 && index < columnGroups.size()) {
            return (ColumnGroup)columnGroups.get(index);
        }
        return null;
    }

    /**
     * Provides and iterator for accessing the ColumnGroups
     * associated with a column.
     * @param col Column
     * @return ColumnGroup iterator
     */
    public Iterator getColumnGroups(TableColumn col) {
		//System.out.println( "GTCM.getColumnGroups( "+col.getIdentifier()+" )" );
		if( myPortedTableModelHGS != null ) return getColumnGroups( col.getModelIndex() );
        if (columnGroups.isEmpty()) return null;
        Iterator iter = columnGroups.iterator();
        while (iter.hasNext()) {
            ColumnGroup cGroup = (ColumnGroup)iter.next();
            Vector v_ret = (Vector)cGroup.getColumnGroups(col,new Vector());
            if (v_ret != null) {
                return v_ret.iterator();
            }
        }
        return null;
    }

	/** @since 030105 */
	public PortedTableModelHGS getPortedTableModelHGS(){
		return this.myPortedTableModelHGS;
	}

    /** @author Keith Cascio @since 102604 */
    public void setPortedTableModelHGS( PortedTableModelHGS portedtablemodelhgs ){
    	synchronized( mySynchRecord )
		{
		//System.out.println( "GTCM.setPortedTableModelHGS()" );
		this.myPortedTableModelHGS = portedtablemodelhgs;

		TableModel underlying = myPortedTableModelHGS.getUnderlying();
		myArrayColumnSelection = new boolean[ underlying.getColumnCount() ];
		//getSelectionModel().addListSelectionListener( (ListSelectionListener)this );
		//myPortedTableModelHGS.addTableModelListener( (TableModelListener)this );
		myPortedTableModelHGS.addPortedModelListener( (PortedTableModel.Listener)this );
		//myPortedTableModelHGS.addChangeListener( (ChangeListener)this );
		//if( myRunRecall == null ) myRunRecall = new RunRecall();
		}
	}

	/** @author Keith Cascio @since 102604 */
	public Iterator getColumnGroups( int modelIndex ){
		ColumnGroup parent = myPortedTableModelHGS.getColumnGroup( modelIndex );
		if( parent == null ) return Collections.EMPTY_LIST.iterator();

		LinkedList ret = new LinkedList();
		for( ColumnGroup current = parent; current != null; current = current.getParent() ){
			ret.addFirst( current );
		}

		return ret.iterator();
	}

	/** @since 030105 */
	public void selectAll(){
		synchronized( mySynchRecord )
		{
		if( myArrayColumnSelection == null ) return;
		for( int i=0; i<myArrayColumnSelection.length; i++ ){
			myArrayColumnSelection[i] = true;
		}
		}
	}

	/** @since 030105 */
	public int getUnderlyingColumnCount(){
		if( myPortedTableModelHGS != null ) return myPortedTableModelHGS.getUnderlying().getColumnCount();
		else return this.getColumnCount();
	}

	/** @since 011705 */
	public boolean isSelectedColumn( int columnIndex ){
		if( myArrayColumnSelection == null ) return false;
		else return myArrayColumnSelection[columnIndex];
	}

	/** interface PortedTableModel.Listener
		@since 011705 */
	public void portedModelEvent( PortedTableModel.PortedModelEvent e ){
		if( e.warning ) recordSelectedState( e.offset, e.breadth );
		//else myRunRecall.start( e );
		else portedModelEventImpl( e );
	}

	/** @since 011705 */
	private void portedModelEventImpl( PortedTableModel.PortedModelEvent e ){
		//Interruptable.checkInterrupted();
		if( e.model == myPortedTableModelHGS ) recallSelectedState( e.offset, e.breadth );
	}

	/** @since 030105 */
	public void recordSelectedState(){
		if( myPortedTableModelHGS != null ) recordSelectedState( myPortedTableModelHGS.getOffset(), myPortedTableModelHGS.getBreadth() );
	}

	/** @since 032205 */
	public void clearSelectedState(){
		if( myArrayColumnSelection == null ) return;
		synchronized( mySynchRecord ){
			if( myFlagCleared ) return;
			Arrays.fill( myArrayColumnSelection, false );
			myFlagCleared = true;
		}
	}

	/** @since 011705 */
	private void recordSelectedState( int offset, int columncount )
	{
		synchronized( mySynchRecord )
		{
		//System.out.println( "GTCM saving ss" );
		ListSelectionModel selmodel = getSelectionModel();
		//System.out.print( "    " );
		for( int i=0; i<columncount; i++ ){
			//System.out.print( selmodel.isSelectedIndex(i) + " " );
			myArrayColumnSelection[ offset+i ] = selmodel.isSelectedIndex( i );
		}
		//System.out.println();
		myFlagCleared = false;
		}
	}

	/** @since 011705 */
	private void recallSelectedState( int offset, int columncount )
	{
		//Interruptable.checkInterrupted();
		synchronized( mySynchRecall )
		{
		//System.out.println( "GTCM loading ss" );
		ListSelectionModel selmodel = getSelectionModel();
		//Interruptable.checkInterrupted();
		for( int i=0; i<columncount; i++ ){
			if( myArrayColumnSelection[ offset+i ] ) selmodel.addSelectionInterval(i,i);
		}
		}
	}

	/** @since 011705 *//*
	private class RunRecall extends Interruptable{
		public void runImpl( Object arg1 ){
			//GroupableTableColumnModel.this.stateChangedImpl( (ChangeEvent)arg1 );
			GroupableTableColumnModel.this.portedModelEventImpl( (PortedTableModel.PortedModelEvent)arg1 );
		}
		public boolean debug(){ return true; }
		//public boolean verbose(){ return true; }
		//public String getNameMethodOfInterest(){ return "stateChangedImpl"; }
	}*/

	/** interface ChangeListener
		@since 011705 *//*
	public void stateChanged( ChangeEvent e ){
		//super.stateChanged( e );
		//myRunRecall.start( (long)250, e );
		myRunRecall.start( e );
		//stateChangedImpl( e );
	}*/

	/** @since 011705 *//*
	private void stateChangedImpl( ChangeEvent e ){
		Interruptable.checkInterrupted();
		if( e.getSource() == myPortedTableModelHGS ) recallSelectedState( myPortedTableModelHGS.getOffset(), myPortedTableModelHGS.getColumnCount() );
	}*/

	/** interface ListSelectionListener *//*
	public void valueChanged( ListSelectionEvent e ){
		super.valueChanged( e );
		if( e.getValueIsAdjusting() ) return;

		int index0 = e.getFirstIndex();
		int index1 = e.getLastIndex();

		//System.out.println( "GTCM recording selection" );
		ListSelectionModel selmodel = getSelectionModel();
		int offset = myPortedTableModelHGS.getOffset();
		for( int i = index0; i<=index1; i++ ){
			//System.out.println( "["+(offset+i)+"] <- " + selmodel.isSelectedIndex(i) );
			myArrayColumnSelection[ offset+i ] = selmodel.isSelectedIndex(i);
		}
	}*/

	/** interface TableModelListener *//*
	public void tableChanged( TableModelEvent e ){
		//super.tableChanged( e );
		if( (e.getSource() == myUnderlying) && (e.getFirstRow() == -1) && (e.getLastRow() == -1) ){
			recordSelectedState( myPortedTableModelHGS );
		}
	}*/

	private PortedTableModelHGS myPortedTableModelHGS;
	private boolean[] myArrayColumnSelection;
	private boolean myFlagCleared = true;
	private Object mySynchRecord = new Object();
	private Object mySynchRecall = mySynchRecord;//new Object();
	//private RunRecall myRunRecall;
	//private TableModel myUnderlying;
}
