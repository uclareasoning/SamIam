package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.tabledisplay.HuginGenieStyleTableFactory.TableModelHGS;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

/**
	Code removed from DisplayableFiniteVariableImpl

	@author Keith Cascio
	@since 061303
*/
public abstract class TableStyleEditor implements ProbabilityEditor, ChangeListener, JOptionResizeHelper.JOptionResizeHelperListener//, CellEditorListener
{
	public static final String STR_TITLE_EXCLUDE = "Sensitivity Exclude Settings";

	//abstract public CellEditor getCellEditor();

	/**
		interface CellEditorListener
		@author Keith Cascio
		@since 071803
	*/
	/*
	public void editingStopped(ChangeEvent e)
	{
		Object source = e.getSource();
		if( source == myTempCellEditorProbabilityTable ) myFlagProbabilityEdited = true;
		else if( source == myExcludeEditor ) myFlagExcludeEdited = true;
		else System.err.println( "Warning: TableStyleEditor.editingStopped() - unrecognized source" );
	}*/

	/** @since 022505 */
	protected Action[] getExtraActions(){
		return (Action[])null;
	}

	/** @since 011305 */
	public void setEditable( boolean editable ){
		this.myFlagEditable = editable;
	}

	/** @since 011305 */
	public boolean isEditable(){
		return this.myFlagEditable;
	}

	public boolean isProbabilityEdited()
	{
		//return myFlagProbabilityEdited;
		if( myTempWrapper == null ) return false;
		return myTempWrapper.model.isProbabilityEdited();
	}

	public boolean isExcludeDataEdited()
	{
		//return myFlagExcludeEdited;
		if( myTempWrapper == null ) return false;
		return myTempWrapper.model.isExcludeDataEdited();
	}

	/**
		interface CellEditorListener
		@author Keith Cascio
		@since 071803
	*/
	public void editingCanceled(ChangeEvent e)
	{
		//System.out.println( "TableStyleEditor.editingCanceled()" );
	}

	public boolean isProbabilityEditInProgress()
	{
		//System.out.println( "TableStyleEditor.isProbabilityEditInProgress() == " + (myTempJTable.getEditingColumn() != -1) );
		if( myTempJTable == null ) return false;
		return ( myTempJTable.getEditingColumn() != -1 );
	}

	public void stopProbabilityEditing()
	{
		//System.out.println( "TableStyleEditor.stopProbabilityEditing()" );
		if( myTempJTable == null ) return;

		int row = myTempJTable.getEditingRow();
		int col = myTempJTable.getEditingColumn();
		if( row >= (int)0 && col >= (int)0 )
		{
			TableCellEditor editor = myTempJTable.getCellEditor( row, col );
			if( editor != null )
			{
				editor.stopCellEditing();
				//System.out.println( "\tstopped @ row " + row + ", column " + col );
			}
		}
	}

	/** interface JOptionResizeHelperListener
		@since 110104 */
	public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
		if( myTempJTable != null ) resize( myTempJTable );
		//if( myTempJTable != null ) System.out.println( "table  size: " + myTempJTable.getSize() );
		//if( myTempJTable != null ) System.out.println( "header size: " + myTempJTable.getTableHeader().getSize() );
		//if( myTempWrapper != null ) System.out.println("compon size: " + myTempWrapper.component.getSize() );
	}

	public String resize(){
		resize( myTempJTable );
		return null;
	}

	/** @since 20031002 */
	static public void resize( JTable table ){
		resize( table, -1 );
	}

	/** @since 20080228 */
	static public void resize( JTable table, int width_bound ){
		if( table == null ){ return; }

		if( table.getAutoResizeMode() == JTable.AUTO_RESIZE_OFF ){ resizeNonAutoResizeTable( table ); }
		else{ resizePortedTable( table, width_bound ); }
	}

	/** @since 20041029 */
	static public void resizeNonAutoResizeTable( JTable table )
	{
		TableColumnModel        tcm = table.getColumnModel();
		TableColumn      tempColumn;
		for( Enumeration enumeration = tcm.getColumns(); enumeration.hasMoreElements(); )
		{
			tempColumn = (TableColumn) enumeration.nextElement();
			tempColumn.setMinWidth( HuginGenieStyleTableFactory.getPreferredWidthForColumn( tempColumn, table ) + 4 );
		}
	  //table.revalidate();
	}

	/** @since 20041029 */
	static public void resizePortedTable( JTable table, int width_bound ){
		resizePortedTableSafe( table, 0, width_bound );
	}

	/** @since 20041029 */
	static private void resizePortedTableSafe( JTable table, int recursions, int width_bound )
	{
		//System.out.println( "TableStyleEditor.resizePortedTableSafe( "+recursions+" )" );

		PortedTableModel ptm = null;
		TableModel       tm  = table.getModel();
		if( tm instanceof PortedTableModel ){ ptm = (PortedTableModel) tm; }
		else{ resizeNonAutoResizeTable( table ); }

		int widthTable = table.getSize().width;
		if( widthTable  < 2 ){ widthTable = table.getPreferredSize().width; }
		if( width_bound > 1 ){ widthTable = Math.min( widthTable, width_bound ); }
		//System.out.println( "    widthTable " + widthTable );

		TableColumnModel tcm    = table.getColumnModel();
		int              margin =   tcm.getColumnMargin();

		int numColumns    = 0;
		int currentWidth  = 0;
		int combinedWidth = 0;
		TableColumn tempColumn = null;
		for( Enumeration enumeration = tcm.getColumns(); enumeration.hasMoreElements(); )
		{
			tempColumn = (TableColumn) enumeration.nextElement();
			++numColumns;
			currentWidth = (HuginGenieStyleTableFactory.getPreferredWidthForColumn( tempColumn, table ) + margin);
			combinedWidth = currentWidth * numColumns;
			//System.out.println( "    column " + tempColumn.getIdentifier() + ", curr " + currentWidth + ", comb " + combinedWidth );
			if( (numColumns>1) && (combinedWidth > widthTable) && (ptm.getBreadth() >= numColumns) ){
				//System.out.println( "    ptm.setBreadth( "+(numColumns-1)+" )" );
				ptm.setBreadth( numColumns - 1 );
				return;
			}
		}

		int remainder = widthTable - combinedWidth;
		if( remainder < (int)1 ) return;

		int average = (int)Math.ceil( ((double)combinedWidth)/((double)numColumns) );
		int columnsToAdd = (int)Math.floor( ((double)remainder)/((double)average) );
		int newBreadth = Math.min( ptm.getBreadth() + columnsToAdd, ptm.getBreadthCeiling() );

		//System.out.println( "    rem "+remainder+", ave " +average + ", toadd " + columnsToAdd + ", new " + newBreadth );

		if( newBreadth != ptm.getBreadth() ){
			//System.out.println( "    ptm.setBreadth( "+newBreadth+" )" );
			ptm.setBreadth( newBreadth );
			if( recursions < 3 ){ resizePortedTableSafe( table, recursions + 1, width_bound ); }
		}
	}

	public boolean discardChanges()
	{
		myTempJTable = null;
		myTempWrapper = null;
		//myFlagProbabilityEdited = false;
		//myFlagExcludeEdited = false;
		return true;
	}

	/*
	public void toggleView()
	{
		stopProbabilityEditing();
		if( myFlagShowingProbabilities )
		{
			if( myTempWrapper != null  )
			{
				myFlagShowingProbabilities = !myTempWrapper.model.showExclude();
				if( !myFlagShowingProbabilities )
				{
					myLabelTitle.setText( STR_TITLE_EXCLUDE );
					//myTempJTable.setDefaultEditor( Object.class, getExcludeEditor() );
				}
			}
		}
		else
		{
			if( myTempWrapper != null  )
			{
				myFlagShowingProbabilities = myTempWrapper.model.showProbabilities();
				if( myFlagShowingProbabilities )
				{
					myLabelTitle.setText( myTitle );
					//myTempJTable.setDefaultEditor( Object.class, myTempCellEditorProbabilityTable );
				}
			}
		}
	}*/

	/*
	private TableCellEditor getExcludeEditor()
	{
		if( myExcludeEditor == null )
		{
			myExcludeEditor = new BooleanCellEditor();
			if( myDisplayableFiniteVariable != null ) myExcludeEditor.addCellEditorListener( this );
		}
		return myExcludeEditor;
	}*/

	/** @since 050802 */
	protected JPanel makeButtonPanel( String title )
	{
		myTitle = title;

		validateActions();

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel pnlButtons = new JPanel( gridbag );

		c.gridwidth = 1;

		DataHandler[] handlers = myTempWrapper.model.getDataHandlers();

		int sizeStrut = 0;
		if( handlers == null )
		{
			myLabelTitle = new JLabel( title );
			Font fntLabel = myLabelTitle.getFont();
			myLabelTitle.setFont( fntLabel.deriveFont( Font.BOLD ) );

			pnlButtons.add( myLabelTitle, c );
			sizeStrut = 50;
		}
		else
		{
			myCbDataHandlers = new JComboBox( handlers );
			myCbDataHandlers.addActionListener( new DataHandlerHandler() );
			pnlButtons.add( myCbDataHandlers, c );
			sizeStrut = 22;
		}

		Component strut = Box.createHorizontalStrut( sizeStrut );
		pnlButtons.add( strut, c );

		c.insets = new Insets( 4,0,4,0 );

		Action[] extra = this.getExtraActions();
		int sizeExtra = (extra == null ) ? 0 : extra.length;
		int sizeAll = 4 + sizeExtra;
		if( myActionToggleView != null ) ++sizeAll;

		Action[] all = new Action[ sizeAll ];
		int counter = 0;

		if( myActionToggleView != null ) all[ counter++ ] = myActionToggleView;
		all[ counter++ ] = myActionResize;
		all[ counter++ ] = myActionComplement;
		all[ counter++ ] = myActionNormalize;
		all[ counter++ ] = myActionSelectAll;

		if( extra != null ){
			for( int i=0; i<sizeExtra; i++ ){
				all[ counter++ ] = extra[i];
			}
		}

		int last = sizeAll - 1;
		for( int i=0; i<sizeAll; i++ ){
			c.gridwidth = ( i == last ) ? GridBagConstraints.REMAINDER : 1;
			pnlButtons.add( setupButton( new JButton( all[i] ) ), c );
		}

		return pnlButtons;
	}

	/** @since 102704 */
	public JComponent makeScrollComponent( PortedTableModel ptm )
	{
		int orientation = java.awt.Adjustable.HORIZONTAL;
		final JScrollBar slider = new JScrollBar( orientation );
		slider.setModel( (javax.swing.BoundedRangeModel) ptm );
		ptm.addChangeListenerNonAdjusting( (ChangeListener)this );
		slider.setVisible( ptm.isOffsetAdjustable() );
		myJScrollBar = slider;
		return slider;
	}

	/** @since 20071210 */
	public TableStyleEditor linkScrollBehavior( final TableStyleEditor other ){
		other.myJScrollBar.addAdjustmentListener( new AdjustmentListener(){
			public void adjustmentValueChanged( AdjustmentEvent e ){
				TableStyleEditor.this.myJScrollBar.setValue( other.myJScrollBar.getValue() );
			}
		} );
		return this;
	}

	/** @since 20071210 */
	public JScrollBar getScrollBar(){
		return myJScrollBar;
	}

	/** interface ChangeListener @since 102804 */
	public void stateChanged( ChangeEvent e ){
		//System.out.println( "* stateChanged() isOffsetAdjustable()? " + ((PortedTableModel) e.getSource()).isOffsetAdjustable() );
		if( myJScrollBar != null ) myJScrollBar.setVisible( ((PortedTableModel) e.getSource()).isOffsetAdjustable() );
	}

	/** @since 051002 */
	public static JButton setupButton( JButton btn )
	{
		btn.setMargin( new Insets( 0,8,0,8 ) );
		Font fntButton = btn.getFont();
		btn.setFont( fntButton.deriveFont( (float)10 ) );
		return btn;
	}

	protected void validateActions()
	{
		myActionNormalize = new SamiamAction( "Normalize", "Ensure the values in each selected column sum to 1", 'n', (Icon)null ){
			public void actionPerformed( ActionEvent evt ){
				handleError( normalize(), (Action)this );
			}
		};
		myActionResize = new SamiamAction( "Resize", "Resize cells if values truncated", 'r', (Icon)null ){
			public void actionPerformed( ActionEvent evt ){
				handleError( resize(), (Action)this );
			}
		};
		myActionComplement = new SamiamAction( "Complement", "Set selected value to 1 - sum", 'c', (Icon)null ){
			public void actionPerformed( ActionEvent evt ){
				handleError( complement(), (Action)this );
			}
		};
		myActionSelectAll = new SamiamAction( "Select All", "Select all parameters", 'a', (Icon)null ){
			public void actionPerformed( ActionEvent evt ){
				handleError( selectAll(), (Action)this );
			}
		};
		//if( myTempWrapper != null && myTempWrapper.model.hasExclude() ) myActionToggleView = new SamiamAction( "toggle", "Toggle between probability/exclude view", 't', (Icon)null ){
		//	public void actionPerformed( ActionEvent evt ){
		//		toggleView();
		//	}
		//};
	}

	/** @since 030105 */
	public String selectAll(){
		try{
			if( myTempJTable == null ) return "Warning: no table";
			TableColumnModel model = myTempJTable.getColumnModel();
			if( model instanceof GroupableTableColumnModel ){
				((GroupableTableColumnModel)model).selectAll();
			}
			myTempJTable.selectAll();
		}catch( Throwable throwable ){
			return throwable.toString();
		}
		return (String) null;
	}

	/** @since 030105 */
	private String handleError( String error, Action action ){
		if( error == null ) return error;
		showError( error, "Error: " + action.getValue( Action.NAME ) );
		return error;
	}

	/** @since 030105 */
	private void showError( String error, String title ){
		JOptionPane.showMessageDialog( myTempJTable, error, title, JOptionPane.ERROR_MESSAGE );
	}

	private void setActionsEnabled( boolean flag )
	{
		myActionNormalize.setEnabled( flag );
		myActionComplement.setEnabled( flag );
	}

	public class DataHandlerHandler implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			stopProbabilityEditing();
			DataHandler newHandler = (DataHandler) myCbDataHandlers.getSelectedItem();
			myTempWrapper.model.setDataHandler( newHandler );
			setActionsEnabled( newHandler.handlesProbabilities() );
		}
	}

	/** @since 20071210 */
	public TableModelHGS getTableModelHGS(){
		return myTempWrapper.model;
	}

	/** @since 20071210 */
	public JTable getTempJTable(){
		return myTempJTable;
	}

	/** @since 20080228 */
	public HuginGenieStyleTableFactory.HuginGenieStyleJComponentWrapper getWrapper(){
		return myTempWrapper;
	}

	//protected boolean myFlagShowingProbabilities = true;
	//protected boolean myFlagProbabilityEdited = false;
	//protected boolean myFlagExcludeEdited = false;
	transient protected JTable myTempJTable;
	transient protected HuginGenieStyleTableFactory.HuginGenieStyleJComponentWrapper myTempWrapper;
	//transient protected TableCellEditor myExcludeEditor;
	//transient protected TableCellEditor myTempCellEditorProbabilityTable;
	transient protected JLabel myLabelTitle;
	transient protected JComboBox myCbDataHandlers;
	protected String myTitle;
	protected DisplayableFiniteVariableImpl myDisplayableFiniteVariable;
	transient protected SamiamAction myActionNormalize, myActionResize, myActionComplement, myActionSelectAll;
	transient protected SamiamAction myActionToggleView;
	protected boolean myFlagEditable = true;

	transient private JScrollBar myJScrollBar;
}
