package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.decision.*;
//import edu.ucla.belief.FiniteVariable;

//import edu.ucla.belief.ui.event.*;
//import edu.ucla.belief.ui.util.*;
//import edu.ucla.belief.ui.*;

import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
//import javax.swing.event.*;
import java.util.*;
import java.awt.Color;
//import java.awt.event.*;

/**
	@author Keith Cascio
	@since 121204
*/
public class GroupingColoringJTable extends JTable
{
	public GroupingColoringJTable()
	{
		super();
		init();
	}

	public void setGroupingColoringTableModel( GroupingColoringTableModel model )
	{
		if( super.getModel() != model ){
			if( myGroupingColoringTableModel != null ){
				this.removeMouseListener( myGroupingColoringTableModel.getPopupHandler() );
			}

			this.myGroupingColoringTableModel = model;
			if( model.getDefaultTableCellRenderer() == null ) model.setDefaultTableCellRenderer( myDefaultTableCellRenderer );
			model.setGridColorDefault( this.getGridColor() );
			super.setModel( model );
			model.configureJTable( this );
			this.addMouseListener( myGroupingColoringTableModel.getPopupHandler() );
			repaint();
		}
	}

	public GroupingColoringTableModel getGroupingColoringTableModel(){
		return this.myGroupingColoringTableModel;
	}

	public TableCellEditor getDefaultTableCellEditor(){
		return this.myDefaultTableCellEditor;
	}

	public boolean isSelectionEmpty(){
		return getSelectionModel().isSelectionEmpty();
	}

	public Collection getSelectedOutcomes( Collection coll ){
		if( coll == null ) coll = new HashSet();
		ListSelectionModel smodel = getSelectionModel();
		int rowcount = getRowCount();
		Object outcome;
		for( int i=0; i<rowcount; i++ ){
			if( smodel.isSelectedIndex( i ) ){
				outcome = myGroupingColoringTableModel.getOutcomeAt( i );
				if( !coll.contains( outcome ) ) coll.add( outcome );
			}
		}
		return coll;
	}

	public Collection getSelectedInstances( Collection coll ){
		if( coll == null ) coll = new HashSet();
		ListSelectionModel smodel = getSelectionModel();
		int rowcount = getRowCount();
		Object instance;
		for( int i=0; i<rowcount; i++ ){
			if( smodel.isSelectedIndex( i ) ){
				instance = myGroupingColoringTableModel.getInstanceAt( i );
				if( !coll.contains( instance ) ) coll.add( instance );
			}
		}
		return coll;
	}

	public int getNumSelectedRows(){
		ListSelectionModel smodel = getSelectionModel();
		int rowcount = getRowCount();
		int ret = 0;
		for( int i=0; i<rowcount; i++ )
			if( smodel.isSelectedIndex( i ) ) ++ret;
		return ret;
	}

	public Object[] getSelectedInstancesArray(){
		ListSelectionModel smodel = getSelectionModel();
		Object[] ret = new Object[ getNumSelectedRows() ];
		int rowcount = getRowCount();
		int index = 0;
		for( int i=0; i<rowcount; i++ )
			if( smodel.isSelectedIndex( i ) )
				ret[index++] = myGroupingColoringTableModel.getInstanceAt( i );
		return ret;
	}

	public void init()
	{
		this.myDefaultTableCellRenderer = this.getDefaultRenderer( Object.class );
		this.myDefaultTableCellEditor = this.getDefaultEditor( Object.class );
		//this.myGridColorDefault = this.getGridColor();
		//System.out.println( "GroupingColoringJTable UI class: " + getUI().getClass().getName() );
		//this.updateUI();
	}

	public void updateUI()
	{
		GroupingColoringTableUI groupingcoloringtableui = new GroupingColoringTableUI( (GroupingColoringJTable)this );
		this.setUI( groupingcoloringtableui );
		this.setGridColor( UIManager.getColor("Table.gridColor") );
		if( myGroupingColoringTableModel != null ) myGroupingColoringTableModel.setGridColorDefault( this.getGridColor() );
	}

	private GroupingColoringTableModel myGroupingColoringTableModel;
	private TableCellRenderer myDefaultTableCellRenderer;
	private TableCellEditor myDefaultTableCellEditor;
	//private Color myGridColorDefault;
}
