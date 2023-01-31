package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.tabledisplay.*;
import edu.ucla.belief.ui.clipboard.InstantiationClipBoard;

import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import edu.ucla.belief.*;
import edu.ucla.belief.inference.map.*;
import edu.ucla.belief.inference.*;
import java.net.*;

/**
	@author  Paul Medvedev
	@version
	output panel that shows the results on the right side of the frame.
*/
public class OutputPanel extends javax.swing.JPanel
{
	public OutputPanel( Map data, Collection variables )
	{
		this( data, variables, false );
	}

	public OutputPanel( Map data, Collection variables, boolean useIDRenderer )
	{
		super();
		myFlagUseIDRenderer = useIDRenderer;
		if( myFlagUseIDRenderer ){
			myVariableComparator = IDComparator.getInstanceID();
			myNameVariableColumn = STR_NAME_VARIABLE_ID;
		}
		else{
			myVariableComparator = VariableComparator.getInstance();
			myNameVariableColumn = STR_NAME_VARIABLE;
		}
		//myVariableComparator = myFlagUseIDRenderer ? VariableComparator.getInstanceID() : VariableComparator.getInstance();
		//myNameVariableColumn = myFlagUseIDRenderer ? STR_NAME_VARIABLE_ID : STR_NAME_VARIABLE;
		//super( new GridBagLayout() );
		init( data, variables );
	}

	/** @since 20070321 */
	public boolean isTiger(){ return false; }
	/** @since 20070312 */
	public void setClipboard( InstantiationClipBoard cb ){}
	/** @since 20070312 */
	public void setDiff(      boolean                fl ){}

	/** @author keith cascio
		@since 20030519 */
	protected void init( Map data, Collection variables )
	{
		myVariables = variables;

		Object [][] ar3 = ARRAY_EMPTY;

		myJTable = new JTable();
		myJTable.setAutoResizeMode( JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS );
		panMain = new JScrollPane( myJTable );

		newData( data, variables );

		setLayout (new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		//gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 1;
		add(panMain, gbc);
	}

	/** @author keith cascio
		@since 20030523 */
	private final void setupLeftmostColumn( Collection variables ){
		setupLeftmostColumn( variables, null );
	}

	protected void setupLeftmostColumn( Collection variables, TableCellRenderer defaultrenderer )
	{
		if( myRenderer == null )
		{
			TableCellRenderer renderer = (defaultrenderer == null ) ? new DefaultTableCellRenderer() : defaultrenderer;
			if( myFlagUseIDRenderer ) myRenderer = new VariableIDRenderer(    renderer );
			else                      myRenderer = new VariableLabelRenderer( renderer, variables );
		}
		else if( myRenderer instanceof VariableLabelRenderer ) ((VariableLabelRenderer)myRenderer).recalculateDisplayValues( variables );

		TableColumnModel model = myJTable.getColumnModel();
		TableColumn leftmost = model.getColumn( 0 );
		leftmost.setCellRenderer( myRenderer );
		leftmost.setMinWidth( HuginGenieStyleTableFactory.getPreferredWidthForColumn( leftmost, myJTable ) );
	}

	/** @since 20030519 */
	protected void calculateSize()
	{
		Dimension dim = myJTable.getPreferredSize();
		dim.height = Math.max( dim.height + 64, 200 );
		dim.height = Math.min( dim.height, 300 );
		dim.width =  Math.max( dim.width + 16, 300 );
		dim.width =  Math.min( dim.width, 400 );
		panMain.setPreferredSize( dim );
	}

	public static final String STR_EMPTY = "empty";
	public static final Object[][] ARRAY_EMPTY = { { STR_EMPTY, STR_EMPTY } };

	/** @since 20030519 */
	public void newData( Map data, Collection newVariables )
	{
		Object [][] ar3;

		Set keySet = data.keySet();
		if( data.size() == (int)0 ) ar3 = ARRAY_EMPTY;
		else
		{
			ar3 = new Object[data.size()][2]; //table contents

			int i = 0;
			Object dVar = null;

			java.util.List variables = new ArrayList( keySet );
			Collections.sort( variables, myVariableComparator );

			for( Iterator it = variables.iterator(); it.hasNext(); i++ )
			{
				dVar = it.next();
				ar3[i][0] = dVar;//.toString();
				ar3[i][1] = data.get( dVar );//.toString();
			}
		}

		myJTable.setModel( myTableModel = new MyTableModel( ar3 ) );

		if( (keySet != null) && (!keySet.isEmpty()) && (newVariables != null) )
		{
			if( !newVariables.containsAll( keySet ) && keySet.iterator().next() instanceof StandardNode ) newVariables = keySet;
			setupLeftmostColumn( newVariables );
		}

		calculateSize();
	}

	/** @since 20030519 */
	public void addMouseListener( MouseListener listener )
	{
		//System.out.println( "OutputPanel.addMouseListener()" );
		myJTable.addMouseListener( listener );
	}

	/** @since 20030519 */
	public JTable getJTable(){ return myJTable; }

	protected Comparator myVariableComparator = VariableComparator.getInstance();

	protected MyTableModel myTableModel;
	protected javax.swing.JTable myJTable;
	private javax.swing.JScrollPane panMain;
	private Collection myVariables;
	private TableCellRenderer myRenderer;
	private boolean myFlagUseIDRenderer = false;
	private String myNameVariableColumn = STR_NAME_VARIABLE;
	public static final String STR_NAME_VARIABLE = "Variable";
	public static final String STR_NAME_VARIABLE_ID = "Variable ID";

	//the only purpose of this model is so that the contents of the table are not editable.
	public class MyTableModel extends AbstractTableModel
	{
		public final String[] columnNames = new String[]{ myNameVariableColumn, "Value" };

		Object[][] data;

		public MyTableModel (Object[][] data1)
		{
			data = data1;

		}

		public int getColumnCount()
		{
			return columnNames.length;
		}

		public int getRowCount()
		{
			return data.length;
		}

		public String getColumnName(int col)
		{
			return columnNames[col];
		}

		public Object getValueAt(int row, int col)
		{
			return data[row][col];
		}
	}
}
