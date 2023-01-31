package edu.ucla.belief.ui.tabledisplay;

//import edu.ucla.belief.*;
import edu.ucla.util.WeakLinkedList;

//import java.util.List;
import java.util.Iterator;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;
//import java.awt.*;
//import java.awt.event.*;

/**
	@author Keith Cascio
	@since 100604
*/
public class PortedTableModel implements TableModel, javax.swing.BoundedRangeModel
{
	public static final int INT_MAX_COLUMNS = (int)5;

	public PortedTableModel( TableModel toWrap ){
		this.myWrapped = toWrap;
		if( myWrapped instanceof AbstractTableModel ) myAbstractTableModel = (AbstractTableModel) myWrapped;
		init();
	}

	/** @since 102804 */
	public boolean isOccluded(){
		return myWrapped.getColumnCount() != getColumnCount();
	}

	/** @since 102804 */
	public boolean isOffsetAdjustable(){
		return (getOffsetFloor() < getOffset()) || (getOffset() < myWrapped.getColumnCount()-getBreadth());
	}

	public void reset(){
		setBreadth( Math.min( myWrapped.getColumnCount()-getOffset(), INT_MAX_COLUMNS ) );
	}

	public TableModel getUnderlying(){
		return this.myWrapped;
	}

	public String getUnderlyingClassName(){
		String full = getUnderlying().getClass().getName();
		return full.substring( full.lastIndexOf('.')+1 );
	}

	public int getBreadth() { return myBreadth; }
	public void setBreadth( int val )
	{
		synchronized( mySynchNotification ){
			synchronized( mySynchConsistency ){
				if( myBreadth == val ) return;
			}

			fireWarning();

			synchronized( mySynchConsistency ){
				while( val > getBreadthCeiling() ){
					if( myOffset > getOffsetFloor() ) --myOffset;
					else throw new IllegalArgumentException();
				}
				myBreadth = val;
			}

			if( myAbstractTableModel != null ) myAbstractTableModel.fireTableStructureChanged();
			fireBoundedRangeModelStateChanged();
			firePortChanged();
		}
	}

	/** @since 102904 */
	public int getBreadthCeiling(){
		return myWrapped.getColumnCount()-myOffset;
	}

	public int getOffset() { return myOffset; }
	public void setOffset( int val )
	{
		synchronized( mySynchNotification ){
			synchronized( mySynchConsistency ){
				if( (myOffset == val) || (val<myOffsetFloorInclusive) || (myOffsetCeilingInclusive<val) ) return;
			}

			fireWarning();

			synchronized( mySynchConsistency ){
				while( val > (myWrapped.getColumnCount()-myBreadth) ){
					if( myBreadth > 1 ) --myBreadth;
					else throw new IllegalArgumentException();
				}
				myOffset = val;
			}

			if( myAbstractTableModel != null ) myAbstractTableModel.fireTableStructureChanged();
			fireBoundedRangeModelStateChanged();
			firePortChanged();
		}
	}

	public void setOffsetCeilingInclusive( int val ){
		//System.out.println( "PTM.setOffetCeilingInclusive( "+val+" )" );
		synchronized( mySynchConsistency )
		{
			if( (myOffsetCeilingInclusive != val) && (val>=(int)0) && (val<=(myWrapped.getColumnCount()-1)) ){
				myOffsetCeilingInclusive = val;
				if( getOffset() > myOffsetCeilingInclusive ) setOffset( myOffsetCeilingInclusive );
			}
		}
	}
	public int getOffsetCeiling(){
		return myOffsetCeilingInclusive;
	}

	public void setOffsetFloorInclusive( int val ){
		//System.out.println( "PTM.setOffetFloorInclusive( "+val+" )" );
		//System.out.println( "    (myOffsetFloorInclusive != "+val+")? " + (myOffsetFloorInclusive != val) );
		//System.out.println( "    ("+val+">=(int)0)? " + (val>=(int)0) );
		//System.out.println( "    ("+val+"<=(myWrapped.getColumnCount()-1))? " + (val<=(myWrapped.getColumnCount()-1)) );
		//System.out.println( "    myWrapped.getColumnCount()? " + myWrapped.getColumnCount() );
		//System.out.println( "    myBreadth? " + myBreadth );
		//System.out.println( "    myOffset? " + myOffset );
		synchronized( mySynchConsistency )
		{
			if( (myOffsetFloorInclusive != val) && (val>=(int)0) && (val<=(myWrapped.getColumnCount()-1)) ){
				if( getOffset() < val ) setOffset( val );
				myOffsetFloorInclusive = val;
			}
		}
	}
	public int getOffsetFloor(){
		return myOffsetFloorInclusive;
	}

	private void init(){
		synchronized( mySynchConsistency ){
			//myBreadth = Math.min( myWrapped.getColumnCount(), INT_MAX_COLUMNS );
			myBreadth = myWrapped.getColumnCount();
		}
	}

	public int convertColumnIndex( int columnIndex ){
		synchronized( mySynchConsistency )
		{
		return columnIndex + myOffset;
		}
	}

	public int invertColumnIndex( int columnIndex ){
		synchronized( mySynchConsistency )
		{
		return columnIndex - myOffset;
		}
	}

	public int getRowCount(){
		return myWrapped.getRowCount();
	}

	public int getColumnCount(){
		return myBreadth;
	}

	public String getColumnName(int columnIndex){
		return myWrapped.getColumnName( convertColumnIndex( columnIndex ) );
	}

	public Class getColumnClass(int columnIndex){
		return myWrapped.getColumnClass( convertColumnIndex( columnIndex ) );
	}

	public boolean isCellEditable(int rowIndex,int columnIndex){
		return myWrapped.isCellEditable( rowIndex, convertColumnIndex( columnIndex ) );
	}

	public Object getValueAt(int rowIndex,int columnIndex){
		return myWrapped.getValueAt( rowIndex, convertColumnIndex( columnIndex ) );
	}

	public void setValueAt(Object aValue,int rowIndex,int columnIndex){
		myWrapped.setValueAt( aValue, rowIndex, convertColumnIndex( columnIndex ) );
	}

	public void addTableModelListener(TableModelListener l){
		myWrapped.addTableModelListener( l );
	}

	public void removeTableModelListener(TableModelListener l){
		myWrapped.removeTableModelListener( l );
	}

	/** interface BoundedRangeModel */
	public int getMinimum(){
		//System.out.println( "PTM.getMinimum() <- 0" );
		return getOffsetFloor();
	}
	/** interface BoundedRangeModel */
	public void setMinimum(int newMinimum){
		//System.out.println( "PTM.setMinimum( "+newMinimum+" )" );
	}
	/** interface BoundedRangeModel */
	public int getMaximum(){
		//System.out.println( "PTM.getMaximum() <- " + myWrapped.getColumnCount() );
		return Math.min( myWrapped.getColumnCount(), getOffsetCeiling() );
	}
	/** interface BoundedRangeModel */
	public void setMaximum(int newMaximum){
		//System.out.println( "PTM.setMaximum( "+newMaximum+" )" );
	}
	/** interface BoundedRangeModel */
	public int getValue(){
		//System.out.println( "PTM.getValue() <- " + getOffset() );
		return getOffset();
	}
	/** interface BoundedRangeModel */
	public void setValue(int newValue){
		//System.out.println( "PTM.setValue( "+newValue+" )" );
		if( (getOffsetFloor() <= newValue) && (newValue <= (myWrapped.getColumnCount()-myBreadth)) )
			setOffset( newValue );
	}
	/** interface BoundedRangeModel */
	public void setValueIsAdjusting(boolean b){
		myFlagValueIsAdjusting = b;
	}
	/** interface BoundedRangeModel */
	public boolean getValueIsAdjusting(){
		return myFlagValueIsAdjusting;
	}
	/** interface BoundedRangeModel */
	public int getExtent(){
		//System.out.println( "PTM.getExtent() <- " + getBreadth() );
		return getBreadth();
	}
	/** interface BoundedRangeModel */
	public void setExtent(int newExtent){
		//System.out.println( "PTM.setExtent( "+newExtent+" )" );
		setBreadth( newExtent );
	}
	/** interface BoundedRangeModel */
	public void setRangeProperties(int value,int extent,int min,int max,boolean adjusting){
		setValueIsAdjusting( true );
		setValue( value );
		setExtent( extent );
		setMinimum( min );
		setMaximum( max );
		setValueIsAdjusting( adjusting );
		fireBoundedRangeModelStateChanged();
		firePortChanged();
	}

	/** @since 011705 */
	public void addChangeListenerNonAdjusting( ChangeListener x ){
		//System.out.println( "PTM.addChangeListenerNonAdjusting( "+x.getClass()+" )" );
		if( myChangeListenersNonAdjusting == null ){
			myChangeListenersNonAdjusting = new WeakLinkedList();
			if( myChangeEvent == null ) myChangeEvent = new ChangeEvent( (Object)this );
		}
		myChangeListenersNonAdjusting.addFirst( x );
	}

	/** interface BoundedRangeModel */
	public void addChangeListener(ChangeListener x){
		//System.out.println( "PTM.addChangeListener( "+x.getClass()+" )" );
		if( myChangeListeners == null ){
			myChangeListeners = new WeakLinkedList();
			if( myChangeEvent == null ) myChangeEvent = new ChangeEvent( (Object)this );
		}
		myChangeListeners.addFirst( x );
	}
	/** interface BoundedRangeModel */
	public void removeChangeListener(ChangeListener x){
		//System.out.println( "PTM.removeChangeListener( "+x.getClass()+" )" );
		if( myChangeListeners != null ) myChangeListeners.remove( x );
	}
	/** interface BoundedRangeModel */
	protected void fireBoundedRangeModelStateChanged(){
		//System.out.println( "PTM.fireBoundedRangeModelStateChanged()" );

		ChangeListener next;
		if( myChangeListeners != null ){
			for( Iterator it = myChangeListeners.iterator(); it.hasNext(); ){
				next = (ChangeListener) it.next();
				if( next == null ) it.remove();
				else next.stateChanged( myChangeEvent );
			}
		}

		if( myFlagValueIsAdjusting || (myChangeListenersNonAdjusting == null) ) return;

		for( Iterator it = myChangeListenersNonAdjusting.iterator(); it.hasNext(); ){
			next = (ChangeListener) it.next();
			if( next == null ) it.remove();
			else next.stateChanged( myChangeEvent );
		}
	}

	/** @since 011705 */
	public interface Listener{
		public void portedModelEvent( PortedModelEvent event );
	}

	/** @since 011705 */
	public class PortedModelEvent{
		public PortedModelEvent( boolean warning ){
			this.model = PortedTableModel.this;
			this.offset = model.getOffset();
			this.breadth = model.getBreadth();
			this.warning = warning;
		}
		public PortedTableModel model;
		public int offset;
		public int breadth;
		public boolean warning;
	}

	/** @since 011705 */
	public void addPortedModelListener(Listener x){
		//System.out.println( "PTM.addPortedModelListener( "+x.getClass()+" )" );
		if( myPortedModelListeners == null ){
			myPortedModelListeners = new WeakLinkedList();
		}
		myPortedModelListeners.addFirst( x );
	}
	/** @since 011705 */
	public boolean removePortedModelListener(Listener x){
		//System.out.println( "PTM.removePortedModelListener( "+x.getClass()+" )" );
		if( myPortedModelListeners != null ) return myPortedModelListeners.remove( x );
		else return false;
	}
	/** @since 011705 */
	protected void fireWarning(){
		if( myPortedModelListeners == null ) return;
		firePortedModelEvent( new PortedModelEvent( true ) );
	}
	/** @since 011705 */
	protected void firePortChanged(){
		if( myPortedModelListeners == null ) return;
		firePortedModelEvent( new PortedModelEvent( false ) );
	}
	/** @since 011705 */
	private void firePortedModelEvent( PortedModelEvent event ){
		//System.out.println( "PTM.firePortedModelEvent()" );
		if( myPortedModelListeners == null ) return;

		//System.out.println( "   for real" );
		Listener next;
		for( Iterator it = myPortedModelListeners.iterator(); it.hasNext(); ){
			next = (Listener) it.next();
			if( next == null ) it.remove();
			else next.portedModelEvent( event );
		}
	}

	private boolean myFlagValueIsAdjusting = false;
	private WeakLinkedList myChangeListeners, myChangeListenersNonAdjusting, myPortedModelListeners;
	private ChangeEvent myChangeEvent;

	private TableModel myWrapped;
	private AbstractTableModel myAbstractTableModel;
	private int myBreadth = (int)0;
	private int myOffset = (int)0;
	private int myOffsetCeilingInclusive = Integer.MAX_VALUE;
	private int myOffsetFloorInclusive = (int)0;
	private Object mySynchConsistency = new Object();
	private Object mySynchNotification = new Object();
}
