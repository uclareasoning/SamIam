package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.decision.*;
import edu.ucla.util.Interruptable;

//import edu.ucla.belief.ui.util.Interruptable;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.EventObject;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/**
	@author Keith Cascio
	@since 011105
*/
public abstract class MultiViewTableModel extends GroupingColoringTableModel implements
	TableModel, TableCellRenderer, ListSelectionListener,
	ActionListener, MenuListener, TableCellEditor, PopupMenuListener
{
	public static final boolean FLAG_ACTIONPERFORMED_THREADED = true;

	public MultiViewTableModel( GroupingColoringJTable table )
	{
		super( table );
		initMultiViewTableModel();
	}

	abstract public String getDisplayOutcomeTypeName();
	abstract public void makeOutcomeDistinctForInstance( Object instance );
	abstract public void makeOutcomeDistinct( Object outcome );
	abstract public View getDefaultView();
	abstract public View[] getArrayViews();

	/** override this method */
	public boolean supportsDeepCloning(){
		return false;
	}

	/** override this method */
	public void makeOutcomeDistinctForInstanceDeep( Object instance ){
		makeOutcomeDistinctForInstance( instance );
	}

	/** override this method */
	public void makeOutcomeDistinctDeep( Object outcome ){
		makeOutcomeDistinct( outcome );
	}

	public void configureJTable( GroupingColoringJTable jtable ){
		super.configureJTable( jtable );
		myDefaultTableCellEditor = jtable.getDefaultTableCellEditor();
		jtable.setDefaultEditor( Object.class, (TableCellEditor)this );
	}

	/** interface TableCellEditor */
	public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected, int row, int column ){
		value = myView.getEditValue( value, row );
		return myDefaultTableCellEditor.getTableCellEditorComponent( table, value, isSelected, row, column );
	}
	/** interface TableCellEditor */
	public Object getCellEditorValue(){
		return myDefaultTableCellEditor.getCellEditorValue();
	}
	/** interface TableCellEditor */
	public boolean isCellEditable(EventObject anEvent){
		return myDefaultTableCellEditor.isCellEditable( anEvent );
	}
	/** interface TableCellEditor */
	public boolean shouldSelectCell(EventObject anEvent){
		return myDefaultTableCellEditor.shouldSelectCell( anEvent );
	}
	/** interface TableCellEditor */
	public boolean stopCellEditing(){
		return myDefaultTableCellEditor.stopCellEditing();
	}
	/** interface TableCellEditor */
	public void cancelCellEditing(){
		myDefaultTableCellEditor.cancelCellEditing();
	}
	/** interface TableCellEditor */
	public void addCellEditorListener(CellEditorListener l){
		myDefaultTableCellEditor.addCellEditorListener( l );
	}
	/** interface TableCellEditor */
	public void removeCellEditorListener(CellEditorListener l){
		myDefaultTableCellEditor.removeCellEditorListener( l );
	}

	private void initMultiViewTableModel(){
		GroupingColoringPopupHandler handler = getPopupHandler();
		handler.addPopupMenuListener( (PopupMenuListener)this );

		myMenuDistinct = new JMenu( "make "+getDisplayOutcomeTypeName()+"s distinct" );
		myMenuDistinct.addMenuListener( (MenuListener)this );

		myItemDistinctSelected = new JMenuItem( "selected instances" );//"make selected "+getDisplayOutcomeTypeName()+"s distinct" );
		myItemDistinctSelected.addActionListener( (ActionListener)this );
		//handler.insertItem( myItemDistinctSelected, 0 );
		myMenuDistinct.add( myItemDistinctSelected );

		if( supportsDeepCloning() ){
			myItemDistinctSelectedDeep = new JMenuItem( "selected instances (deep)" );
			myItemDistinctSelectedDeep.addActionListener( (ActionListener)this );
			myMenuDistinct.add( myItemDistinctSelectedDeep );
		}

		myItemDistinctAll = new JMenuItem( "all outcomes" );//"make all "+getDisplayOutcomeTypeName()+"s distinct" );
		myItemDistinctAll.addActionListener( (ActionListener)this );
		//handler.insertItem( myItemDistinctAll, 1 );
		myMenuDistinct.add( myItemDistinctAll );

		if( supportsDeepCloning() ){
			myItemDistinctAllDeep = new JMenuItem( "all outcomes (deep)" );
			myItemDistinctAllDeep.addActionListener( (ActionListener)this );
			myMenuDistinct.add( myItemDistinctAllDeep );
		}

		handler.insertItem( myMenuDistinct, 0 );

		myMenuView = new JMenu( getDisplayOutcomeTypeName() + " view/edit options" );
		myMenuView.addMenuListener( (MenuListener)this );
		myMenuView.add( myMenuListView = new MenuList( new DefaultComboBoxModel( getArrayViews() ) ) );
		myMenuListView.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		myMenuListView.addListSelectionListener( (ListSelectionListener)this );

		myMenuView.add( myItemOK = new JMenuItem( "OK" ) );
		myItemOK.addActionListener( (ActionListener)this );

		handler.addItemViewOptions( myMenuView );

		myView = getDefaultView();
	}

	/** interface ListSelectionListener */
	public void valueChanged(ListSelectionEvent e){
		myItemOK.setEnabled( !myMenuListView.isSelectionEmpty() );
	}

	/** interface ActionListener */
	public void actionPerformed( ActionEvent e ){
		Object src = e.getSource();
		if( src == myItemOK ) setView( (View) myMenuListView.getSelectedValue() );
		else if( src == myItemDistinctSelected ) runMakeDistinct( myGroupingColoringJTable.getSelectedInstances( new HashSet(getRowCount()) ), false );
		else if( src == myItemDistinctSelectedDeep ) runMakeDistinct( myGroupingColoringJTable.getSelectedInstances( new HashSet(getRowCount()) ), true );
		else if( src == myItemDistinctAll ) runMakeDistinct( (Collection)null, false );
		else if( src == myItemDistinctAllDeep ) runMakeDistinct( (Collection)null, true );
	}

	public void runMakeDistinct( Collection selected, boolean deep ){
		myRunMakeDistinct.run( FLAG_ACTIONPERFORMED_THREADED, new RunMakeDistinctArgs( selected, deep ) );
	}

	public class RunMakeDistinct extends Interruptable{
		public void runImpl( Object arg1 ) throws InterruptedException{
			RunMakeDistinctArgs args = (RunMakeDistinctArgs) arg1;
			if( args.selected == null ) MultiViewTableModel.this.doMakeOutcomesDistinct( (Collection)null, args.deep );
			else MultiViewTableModel.this.doMakeDistinctForInstances( args.selected, args.deep );
		}
	}

	public class RunMakeDistinctArgs{
		public RunMakeDistinctArgs( Collection selected, boolean deep ){
			this.selected = selected;
			this.deep = deep;
		}
		public Collection selected;
		public boolean deep;
	}

	public void doMakeDistinctForInstances( Collection selected, boolean deep ) throws InterruptedException
	{
		fireEditWarning();
		Thread.sleep(4);//Interruptable.checkInterrupted();
		if( selected == null ) selected = new HashSet( getDecisionNode().getVariable().instances() );

		//System.out.println( "MultiViewTableModel.doMakeDistinctForInstances() started at: " + System.currentTimeMillis() );

		for( Iterator it = selected.iterator(); it.hasNext(); ){
			Thread.sleep(4);//Interruptable.checkInterrupted();
			if( deep ) makeOutcomeDistinctForInstanceDeep( it.next() );
			else makeOutcomeDistinctForInstance( it.next() );
		}
	}

	public void doMakeOutcomesDistinct( Collection outcomes, boolean deep ) throws InterruptedException
	{
		fireEditWarning();
		Thread.sleep(4);//Interruptable.checkInterrupted();
		if( outcomes == null ) outcomes = getDecisionNode().getOutcomes( new HashSet() );

		//System.out.println( "MultiViewTableModel.doMakeOutcomesDistinct() started at: " + System.currentTimeMillis() );

		for( Iterator it = outcomes.iterator(); it.hasNext(); ){
			Thread.sleep(4);//Interruptable.checkInterrupted();
			if( deep ) makeOutcomeDistinctDeep( it.next() );
			else makeOutcomeDistinct( it.next() );
		}
	}

	/** interface PopupMenuListener */
	public void popupMenuWillBecomeVisible(PopupMenuEvent e){
	}

	/** interface MenuListener */
	public void menuSelected(MenuEvent e){
		Object src = e.getSource();
		if( src == myMenuView ){
			myMenuListView.setSelectedValue( myView );
			//myMenuListView.printDebugInfo( System.out );
			//myMenuView.validate();
			//myMenuView.revalidate();
			//myMenuView.repaint();
		}
		else if( src == myMenuDistinct ){
			boolean editable = this.isEditable() && getDecisionNode().isEditable();
			boolean enableselected = editable && (!myGroupingColoringJTable.isSelectionEmpty());
			myItemDistinctSelected.setEnabled( enableselected );
			if( myItemDistinctSelectedDeep != null ) myItemDistinctSelectedDeep.setEnabled( enableselected );
			myItemDistinctAll.setEnabled( editable );
			if( myItemDistinctAllDeep != null ) myItemDistinctAllDeep.setEnabled( editable );
		}
	}
	/** interface MenuListener */
	public void menuDeselected(MenuEvent e){}
	/** interface MenuListener */
	public void menuCanceled(MenuEvent e){}

	public void setView( View view ){
		if( view != myView ){
			this.myView = view;
			fireTableDataChanged();
		}
	}

	public abstract class View{
		public View( String name ){
			this.myName = name;
		}
		public String toString(){
			return this.myName;
		}
		public String emptyValueToString(){
			return "";
		}
		abstract public String outcomeToString( Object outcome );
		abstract public Object getEditValue( Object value, int row );
		abstract public boolean setValue( Object outcome, Object editedvalue, Object instance );
		private String myName;
	}

	public String outcomeToString( Object outcome ){
		if( outcome == null ) return myView.emptyValueToString();
		else return myView.outcomeToString( outcome );
	}

	public String instanceToString( Object instance ){
		return instance.toString();
	}

	public void setOutcome( Object instance, Object outcome, Object newValue, int rowIndex ){
		myView.setValue( outcome, newValue, instance );
	}

	private View myView;
	private JMenu myMenuView, myMenuDistinct;
	private MenuList myMenuListView;
	private JMenuItem myItemOK;
	private JMenuItem myItemDistinctAll, myItemDistinctAllDeep;
	private JMenuItem myItemDistinctSelected, myItemDistinctSelectedDeep;
	private TableCellEditor myDefaultTableCellEditor;
	private RunMakeDistinct myRunMakeDistinct = new RunMakeDistinct();
}
