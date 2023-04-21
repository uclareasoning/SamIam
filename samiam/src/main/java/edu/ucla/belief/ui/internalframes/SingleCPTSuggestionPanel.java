package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.tabledisplay.*;

import edu.ucla.belief.*;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.sensitivity.*;
import edu.ucla.belief.io.NetworkIO;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Comparator;
import java.util.ArrayList;

/**
	@author Keith Cascio
	@since 092903
*/
public class SingleCPTSuggestionPanel extends JPanel implements ListSelectionListener, MouseListener
{
	public boolean flagValid = true;

	public SingleCPTSuggestionPanel()
	{
		super( new GridLayout() );
		init();
	}

	public void setSuggestions( Map singleCPTSuggestions )
	{
		clearEditPanel();
		myListModel.setSuggestions( singleCPTSuggestions );
		TableColumnModel tcm = myListTable.getColumnModel();
		TableColumn col = tcm.getColumn( SuggestionListTableModel.INT_INDEX_LOG_ODDS );
		col.setMaxWidth( HuginGenieStyleTableFactory.getPreferredWidthForColumn( col, myListTable ) + 2 );
		flagValid = true;
	}

	private void init()
	{
		//add( makeListPanel() );
		//add( makeEditPanel() );
		JSplitPane splitsville = new JSplitPane();
		splitsville.setLeftComponent( makeListPanel() );
		splitsville.setRightComponent( makeEditPanel() );
		add( splitsville );
		splitsville.setDividerLocation( 170 );
	}

	public JComponent makeEditPanel()
	{
		myEditPanel = new JPanel( new GridBagLayout() );
		myConstraints = new GridBagConstraints();
		myConstraints.weightx = myConstraints.weighty = 1;
		myConstraints.fill = GridBagConstraints.BOTH;
		return myEditPanel;
	}

	public JComponent makeListPanel()
	{
		myListModel = new SuggestionListTableModel();
		myListTable = new JTable( myListModel );
		myListTable.setToolTipText( "Sort by clicking on column header" );
		myListModel.setDefaultTableCellRenderer( myListTable.getDefaultRenderer( Object.class ) );
		TableColumnModel tcm = myListTable.getColumnModel();
		TableColumn col = tcm.getColumn( SuggestionListTableModel.INT_INDEX_VARIABLE );
		col.setCellRenderer( myListModel );
		col.setMinWidth( HuginGenieStyleTableFactory.getPreferredWidthForColumn( col, myListTable ) + 4 );
		col = tcm.getColumn( SuggestionListTableModel.INT_INDEX_LOG_ODDS );
		col.setCellRenderer( myListModel );
		col.setMinWidth( HuginGenieStyleTableFactory.getPreferredWidthForColumn( col, myListTable ) + 2 );
		//myListTable.setShowHorizontalLines( false );
		//myListTable.setShowVerticalLines( false );
		ListSelectionModel lsm = myListTable.getSelectionModel();
		lsm.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		lsm.addListSelectionListener( this );
		JTableHeader header = myListTable.getTableHeader();
		header.addMouseListener( this );
		myPainList = new JScrollPane( myListTable );

		JPanel ret = new JPanel( new GridBagLayout() );

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.WEST;
		//ret.add( Box.createHorizontalStrut(8), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		//ret.add( new JLabel( "Variable suggestions" ), c );

		c.weightx = c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		ret.add( myPainList, c );

		Dimension dim = myListTable.getPreferredSize();
		dim.height = (int)200;
		dim.width = (int)256;
		//myListTable.setPreferredSize( dim );
		myPainList.setPreferredSize( dim );

		return ret;
	}

	public void valueChanged( ListSelectionEvent e )
	{
		setRowIndex( myListTable.getSelectedRow() );
	}

	public void setRowIndex( int row )
	{
		if( row < 0 ) return;
		else
		{
			myEditPanel.removeAll();
			myEditPanel.add( myListModel.getDetails( row ), myConstraints );
			myEditPanel.revalidate();
			myListModel.resizeDetails( row );
			myEditPanel.repaint();
		}
	}

	public DisplayableFiniteVariable getCurrentlySelectedVariable()
	{
		int index = myListTable.getSelectedRow();
		if( index < (int)0 ) return null;
		else return myListModel.getVariableAt( index );
	}

	/**
		@author Keith Cascio
		@since 102003
	*/
	public SingleCPTSuggestion getCurrentlySelectedSuggestion()
	{
		int index = myListTable.getSelectedRow();
		if( index < (int)0 ) return null;
		else return myListModel.getSuggestionAt( index );
	}

	public JTable getListTable()
	{
		return myListTable;
	}

	/**
		Test/debug
	*/
	public static void main( String[] args )
	{
		Util.setLookAndFeel();

		String path = "c:\\keithcascio\\networks\\cancer_test.net";
		if( args.length > 0 ) path = args[0];

		BeliefNetwork bn = null;

		try{
			bn = NetworkIO.read( path );
		}catch( Exception e ){
			System.err.println( "Error: Failed to read " + path );
			return;
		}

		bn = Bridge2Tiger.Troll.solicit().newDisplayableBeliefNetworkImpl( bn, null );

		SingleCPTSuggestionPanel scptsp = new SingleCPTSuggestionPanel();

		InferenceEngine ie = new JEngineGenerator().manufactureInferenceEngine( bn );
		SensitivityEngine se = new SensitivityEngine( bn, ie, (PartialDerivativeEngine) ie );

		FiniteVariable dvarY = (FiniteVariable) bn.forID( "D" );
		Object valueY = dvarY.instance( (int)0 );
		Object opComparison = SensitivityEngine.OPERATOR_GTE;
		double constant = (double)0.4;

		SensitivityReport sr = se.getResults( dvarY, valueY, null, null, null, opComparison, constant, false, true );

		scptsp.setSuggestions( sr.getSingleCPTMap() );

		JTabbedPane tabbed = new JTabbedPane();
		tabbed.addTab( "Single parameter suggestions (sort by clicking on a column header)", new JPanel() );
		tabbed.addTab( "Single CPT suggestions", scptsp );
		tabbed.setSelectedComponent( scptsp );

		JComponent contentPain = new JPanel( new BorderLayout() );
		contentPain.add( tabbed, BorderLayout.CENTER );
		contentPain.add( new JLabel( "network: " + path ), BorderLayout.SOUTH );
		//contentPain.add( pnlButtons, BorderLayout.EAST );

		JFrame frame = Util.getDebugFrame( "DEBUG SingleCPTSuggestionPanel", contentPain );
		frame.setVisible( true );
	}

	/** @since 20050824 */
	private void clearEditPanel(){
		myEditPanel.removeAll();
		myEditPanel.revalidate();
		myEditPanel.repaint();
	}

	public void mouseClicked( MouseEvent event )
	{
		int indexColumn = myListTable.getColumnModel().getColumnIndexAtX( event.getX() );
		int indexModel = myListTable.convertColumnIndexToModel( indexColumn );

		myListModel.sort( indexModel );
		clearEditPanel();
	}

	public void mouseEntered(MouseEvent event) { }

	public void mouseExited(MouseEvent event) { }

	public void mousePressed(MouseEvent event) { }

	public void mouseReleased(MouseEvent event) { }

	private JTable myListTable;
	private JScrollPane myPainList;
	private SuggestionListTableModel myListModel;
	private JComponent myEditPanel;
	private GridBagConstraints myConstraints;
}
