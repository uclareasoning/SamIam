package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.tabledisplay.*;

import edu.ucla.belief.sensitivity.*;
import edu.ucla.util.*;

import java.awt.event.*;
import java.util.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.TableModelEvent;

public class SensitivitySuggestionTable extends JTable// implements MouseListener
{
	public static final boolean DEBUG_VERBOSE = Util.DEBUG_VERBOSE;

	public boolean flagValid = true;

	private NetworkInternalFrame hnInternalFrame;
	protected PreferenceGroup myGlobalPrefs = null;
	private java.util.List suggestions;
	private boolean sortable;
	private TableColumn myLogOddsChangeColumn, myAbsolutChangeColumn;

	public SensitivitySuggestionTable(	NetworkInternalFrame hnInternalFrame,
						PreferenceGroup globalPrefs,
						java.util.List suggestions,
						boolean sortable )
	{
		this.hnInternalFrame = hnInternalFrame;
		this.myGlobalPrefs = globalPrefs;
		this.suggestions = suggestions;
		this.sortable = sortable;

		createModel();
		init();
	}

	public SensitivitySuggestionTable(	NetworkInternalFrame hnInternalFrame,
						PreferenceGroup globalPrefs,
						SensitivityReport sr,
						boolean sortable )
	{
		this.hnInternalFrame = hnInternalFrame;
		this.myGlobalPrefs = globalPrefs;
		this.sortable = sortable;

		suggestions = sr.generateSingleParamSuggestions();
		Collections.sort(suggestions, new
			SensitivitySuggestionComparator(
			SensitivitySuggestionComparator.LOG_ODDS_CHANGE));
		createModel();
		init();
	}

	/** @since 20020603 */
	public SensitivitySuggestion getCurrentlySelectedSuggestion()
	{
		int indexSelectedRow = getSelectedRow();
		if( indexSelectedRow >= (int)0 && indexSelectedRow < getRowCount() )
		{
			return ((SensitivitySuggestionTableModel) getModel()).getSuggestion( indexSelectedRow );
		}
		else return null;
	}

	/** @since 20051129 */
	public SensitivitySuggestionTableModel getSensitivitySuggestionTableModel(){
		return (SensitivitySuggestionTableModel) getModel();
	}

	/** @since 20051129 */
	public void tableChanged( TableModelEvent e ){
		super.tableChanged( e );

		if( e.getColumn() == TableModelEvent.ALL_COLUMNS ) SensitivitySuggestionTable.this.refreshColumnHeaderValues();
	}

	/** @since 20051129 */
	public void refreshColumnHeaderValues(){
		TableModel tm = SensitivitySuggestionTable.this.getModel();
		TableColumnModel tcm = SensitivitySuggestionTable.this.getColumnModel();

		TableColumn tc;
		for( Enumeration cols = tcm.getColumns(); cols.hasMoreElements(); ){
			tc = (TableColumn) cols.nextElement();
			tc.setHeaderValue( tm.getColumnName( tc.getModelIndex() ) );
		}
	}

	/** @since 20051129 */
	public void setTableDetailsVisible( boolean stateRequested ){
		try{
			TableColumnModel tcm = SensitivitySuggestionTable.this.getColumnModel();

			boolean stateExisting = tcm.getColumnCount() > 3;

			if( stateExisting == stateRequested ) return;

			if( stateRequested ){
				tcm.addColumn( myAbsolutChangeColumn );
				tcm.addColumn( myLogOddsChangeColumn );
			}
			else{
				tcm.removeColumn( myAbsolutChangeColumn );
				tcm.removeColumn( myLogOddsChangeColumn );
			}

			SensitivitySuggestionTable.this.prettySize();
		}catch( Exception exception ){
			System.err.println( "Warning: SensitivitySuggestionTable.setTableDetailsVisible() caught " + exception );
		}
	}

	/** @since 20020603 */
	public interface TableModelListener
	{
		public void tableModelChanged( SensitivitySuggestionTable SST );
	}

	private WeakLinkedList myListenerList;

	/** @since 20020603 */
	public void addTableModelListener( TableModelListener TML )
	{
		if( myListenerList == null ) myListenerList = new WeakLinkedList();
		myListenerList.add( TML );
	}

	private void createModel()
	{
		setModel( new SensitivitySuggestionTableModel( hnInternalFrame, suggestions ) );
		createDefaultColumnsFromModel();

		TableColumnModel tcm = this.getColumnModel();
		TableColumn column = tcm.getColumn( SensitivitySuggestionTableModel.CPT_PARAMETER_COLUMN_INDEX );
		column.setPreferredWidth( column.getPreferredWidth() * 3 );

		myAbsolutChangeColumn = tcm.getColumn( SensitivitySuggestionTableModel.ABSOLUTE_CHANGE_COLUMN_INDEX );
		myLogOddsChangeColumn = tcm.getColumn( SensitivitySuggestionTableModel.LOG_ODDS_CHANGE_COLUMN_INDEX );

		//keith
		initializeEditButtonsColumn();
		initHeaderSorting();

		if( myListenerList != null ){
			TableModelListener next;
			for( ListIterator it = myListenerList.listIterator(); it.hasNext(); ){
				next = (TableModelListener) it.next();
				if( next == null ) it.remove();
				else next.tableModelChanged( this );
			}
		}
	}

	/** @since 20051129 */
	private void initHeaderSorting(){
		JTableHeader header = SensitivitySuggestionTable.this.getTableHeader();
		if( header != null ){
			header.addMouseListener( new MouseAdapter(){
				public void mouseClicked( MouseEvent event ){
					SensitivitySuggestionTable.this.sort( event );
				}
			} );
		}
		else System.err.println( "Warning: SensitivitySuggestionTable.initHeaderSorting() failed" );
	}

	/** @since 20020219 */
	private class JButtonRenderer implements TableCellRenderer{
		public JButtonRenderer(){}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
			//JPanel pnlButton = new JPanel();
			//pnlButton.add( (JButton)value );
			//return pnlButton;
			JButton ret = (JButton)value;
			ret.setEnabled( true );
			return ret;
		}
	}

	/** @since 20020219 */
	private class JButtonEditor extends AbstractCellEditor
	{
		private JButtonRenderer renderer = new JButtonRenderer();
		private JButton btn = null;

		public JButtonEditor()
		{}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
		{
			btn = (JButton)(renderer.getTableCellRendererComponent( table, value, isSelected, true, row, column ));
			return btn;
		}

		public boolean stopCellEditing()
		{
			setCellEditorValue( btn );
			return super.stopCellEditing();
		}
	}

	/** @since 20020219 */
	private void initializeEditButtonsColumn()
	{
		if( (int)0 <= SensitivitySuggestionTableModel.EDIT_CPT_BUTTON_COLUMN_INDEX && SensitivitySuggestionTableModel.EDIT_CPT_BUTTON_COLUMN_INDEX < getColumnModel().getColumnCount() )
		{
			//setDefaultRenderer( JButton.class, new JButtonRenderer() );
			//setDefaultEditor( JButton.class, new JButtonEditor() );
			TableColumn col = getColumnModel().getColumn( SensitivitySuggestionTableModel.EDIT_CPT_BUTTON_COLUMN_INDEX );
			col.setCellRenderer( new JButtonRenderer() );
			col.setCellEditor( new JButtonEditor() );
			col.setMinWidth( 80 );

			col = getColumnModel().getColumn( SensitivitySuggestionTableModel.ADOPT_CHANGE_COLUMN_INDEX );
			col.setCellRenderer( new JButtonRenderer() );
			col.setCellEditor( new JButtonEditor() );
			col.setMinWidth( 80 );
		}
		else{
			//if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Debug: ArrayIndexOutOfBoundsException caught in SensitivitySuggestionTable.initializeEditButtonsColumn()" );
		}
	}

	private void init()
	{
		DoubleFormat df = new DoubleFormat(6);
		setDefaultRenderer( Double.class, new DoubleTableCellRenderer( df ) );
		IntervalTableCellRenderer itcr = new IntervalTableCellRenderer( df );
		setDefaultRenderer( Interval.class, itcr );
		TableColumn colSuggested = getColumnModel().getColumn( SensitivitySuggestionTableModel.NEW_VALUE_COLUMN_INDEX );
		colSuggested.setCellRenderer( new ChromaticIntervalTableCellRenderer( df, SuggestionDataHandler.COLOR_BACKGROUND_PANEL, SuggestionDataHandler.COLOR_BACKGROUND_SUGGESTED ) );

		//keith
		initializeEditButtonsColumn();

		//getTableHeader().addMouseListener( this );
		setRowSelectionAllowed( true );
		setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS );
		//setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

		setMaxWidthForColumn( SensitivitySuggestionTableModel.OLD_VALUE_COLUMN_INDEX );
		setMaxWidthForColumn( SensitivitySuggestionTableModel.NEW_VALUE_COLUMN_INDEX );
		setMaxWidthForColumn( SensitivitySuggestionTableModel.ABSOLUTE_CHANGE_COLUMN_INDEX );
		setMaxWidthForColumn( SensitivitySuggestionTableModel.LOG_ODDS_CHANGE_COLUMN_INDEX );

		setToolTipText( "Sort by clicking on column header" );
	}

	/** @since 20020607 */
	private void setMaxWidthForColumn( int index )
	{
		TableColumn col = getColumnModel().getColumn( index );
		if( col == null ) System.err.println( "Warning: SensitivitySuggestionTable.setMaxWidthForColumn( bad column index )" );
		else col.setMaxWidth( HuginGenieStyleTableFactory.getPreferredWidthForColumn( col , this ) + 2 );
	}

	private JScrollPane myJScrollPane = null;

	/** @since 20020607 */
	public void setJScrollPane( JScrollPane jsp )
	{
		myJScrollPane = jsp;
	}

	/** @since 20020607 */
	public void prettySize()
	{
		//System.out.println( "Parent: " + getParent() );//debug
		//System.out.println( "JScrollPane anscestor: " + SwingUtilities.getAncestorOfClass( JScrollPane.class, this ) );//debug
		//System.out.println( "topLevelAncestor: " + getTopLevelAncestor() );//debug
		//getScrollableTracksViewportWidth();
		//setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS );
		//Dimension prefSize = getPreferredSize();
		//resizeAndRepaint();
		//setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		//setSize( prefSize );

		//if( myJScrollPane != null ) setPreferredSize( new Dimension( myJScrollPane.getSize().width - 4, getSize().height ) );

		//Rectangle rectVis = getVisibleRect();
		//Rectangle rectBound = getBounds();
		//if( rectVis.equals( rectBound ) || rectVis.contains( rectBound ) )//always true
		/*
		if( myJScrollPane != null )
		{
			//System.out.println( "table width: " + getWidth() + ", scrollpane width: " + myJScrollPane.getWidth() );//debug
			if( getWidth() > myJScrollPane.getWidth() )
			{
				if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Debug: sensitivity table is partially obscured" );
				setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
			}
			else
			{
				if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Debug: sensitivity table is completely visible" );
				setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS );
			}
		}*/

		TableColumn colOldVal = getColumnModel().getColumn( SensitivitySuggestionTableModel.OLD_VALUE_COLUMN_INDEX );
		TableColumn colNewVal = getColumnModel().getColumn( SensitivitySuggestionTableModel.NEW_VALUE_COLUMN_INDEX );

		if( ! SensitivityInternalFrame.FLAG_CRAM ){
			colOldVal.setMinWidth( colOldVal.getMaxWidth() );
			colNewVal.setMinWidth( colNewVal.getMaxWidth() );
		}

		int remainingSpace = getWidth() - colOldVal.getWidth() - colNewVal.getWidth();

		TableColumn colParam = getColumnModel().getColumn( SensitivitySuggestionTableModel.CPT_PARAMETER_COLUMN_INDEX );
		int colParamPrefWidth = HuginGenieStyleTableFactory.getPreferredWidthForColumn( colParam , this );

		if( SensitivitySuggestionTableModel.LOG_ODDS_CHANGE_COLUMN_INDEX < getColumnModel().getColumnCount() )
		{
			TableColumn colAbs = getColumnModel().getColumn( SensitivitySuggestionTableModel.ABSOLUTE_CHANGE_COLUMN_INDEX );
			TableColumn colLog = getColumnModel().getColumn( SensitivitySuggestionTableModel.LOG_ODDS_CHANGE_COLUMN_INDEX );
			if( colParamPrefWidth + colAbs.getMaxWidth() + colLog.getMaxWidth() < remainingSpace )
			{
				if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Debug: comfortable width in sensitivity table for all columns" );
				colParam.setMinWidth( colParamPrefWidth );
				colAbs.setMinWidth( colAbs.getMaxWidth() );
				colLog.setMinWidth( colLog.getMaxWidth() );
			}
			else
			{
				if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Debug: uncomfortable width in sensitivity table" );
				colParam.setMinWidth( ULTIMATE_MIN_COLUMN_WIDTH );
				colOldVal.setMinWidth( ULTIMATE_MIN_COLUMN_WIDTH );
				colNewVal.setMinWidth( ULTIMATE_MIN_COLUMN_WIDTH );
				colAbs.setMinWidth( ULTIMATE_MIN_COLUMN_WIDTH );
				colLog.setMinWidth( ULTIMATE_MIN_COLUMN_WIDTH );
				colParam.setWidth( (int)(remainingSpace * 0.6) );
			}
		}
		else
		{
			if( colParamPrefWidth <= remainingSpace )
			{
				if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Debug: comfortable width in sensitivity table for all columns" );
				colParam.setMinWidth( colParamPrefWidth );
			}
			else
			{
				if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Debug: uncomfortable width in sensitivity table" );
				colParam.setMinWidth( ULTIMATE_MIN_COLUMN_WIDTH );
				if( remainingSpace < 51 )
				{
					colOldVal.setMinWidth( ULTIMATE_MIN_COLUMN_WIDTH );
					colNewVal.setMinWidth( ULTIMATE_MIN_COLUMN_WIDTH );
					colParam.setWidth( (int)(remainingSpace * 0.6) );
				}
			}
		}
	}

	private static final int ULTIMATE_MIN_COLUMN_WIDTH = (int)16;

	public SensitivitySuggestion getSuggestion(int row) {
		return (SensitivitySuggestion)suggestions.get(row);
	}

	/*
	public void sort(int columnIndex)
	{
		if (!sortable) return;

		SensitivitySuggestionComparator comparator = null;

		if( columnIndex == SensitivitySuggestionTableModel.CPT_PARAMETER_COLUMN_INDEX )
		{
			comparator = new SensitivitySuggestionComparator( SensitivitySuggestionComparator.CPT_PARAMETER );
		}
		else if(columnIndex == SensitivitySuggestionTableModel.ABSOLUTE_CHANGE_COLUMN_INDEX)
		{
			comparator = new SensitivitySuggestionComparator( SensitivitySuggestionComparator.ABSOLUTE_CHANGE);
		}
		else if (columnIndex == SensitivitySuggestionTableModel.LOG_ODDS_CHANGE_COLUMN_INDEX)
		{
			comparator = new SensitivitySuggestionComparator( SensitivitySuggestionComparator.LOG_ODDS_CHANGE );
		}
		else return;

		Collections.sort(suggestions, comparator);
		createModel();
	}*/

	/** @since 20051129 */
	public void sort( MouseEvent event ){
		int indexColumn = SensitivitySuggestionTable.this.getColumnModel().getColumnIndexAtX( event.getX() );
		int indexModel = SensitivitySuggestionTable.this.convertColumnIndexToModel( indexColumn );

		SensitivitySuggestionTable.this.sort( indexModel );
	}

	/** @since 20020821 */
	public void sort( int columnIndex ){
		if( sortable ) ((SensitivitySuggestionTableModel) getModel()).sort( columnIndex );
	}

	/*
	public void mouseClicked(MouseEvent event){
		sort(convertColumnIndexToModel(getColumnModel().getColumnIndexAtX(event.getX())));
	}

	public void mouseEntered(MouseEvent event) { }

	public void mouseExited(MouseEvent event) { }

	public void mousePressed(MouseEvent event) { }

	public void mouseReleased(MouseEvent event) { }
	*/
}
