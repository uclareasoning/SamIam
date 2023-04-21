package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.*;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.UserEnumValue;
import edu.ucla.util.FlagProperty;
import edu.ucla.util.UserEnumProperty;
import edu.ucla.belief.io.dsl.DSLNodeType;

import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.tabledisplay.*;

import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/**
	@author Keith Cascio
	@since 082703
*/
public class UserPropertyEditPanel extends JPanel implements ActionListener, ListSelectionListener, CellEditorListener
{
	public UserPropertyEditPanel()
	{
		super( new GridLayout() );

		init();
	}

	public void init()
	{
		add( makeListPanel() );
		add( makeEditPanel() );

		resetEnabledState();
	}

	public static final int INT_PREF_HEIGHT = (int)300;

	public JComponent makeListPanel()
	{
		//myListModel = new DefaultListModel();
		//myJList = new JList( myListModel );
		//myJList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		//myJList.addListSelectionListener( this );
		myListModel = new UserPropertyTableModel();
		myListTable = new JTable( myListModel );
		myListModel.setDefaultTableCellRenderer( myListTable.getDefaultRenderer( Object.class ) );
		TableColumnModel tcm = myListTable.getColumnModel();
		TableColumn col = tcm.getColumn(0);
		col.setCellRenderer( myListModel );
		col.setMinWidth( 16 );
		TableCellEditor editor = col.getCellEditor();
		if( editor == null ) editor = myListTable.getDefaultEditor( Object.class );
		editor.addCellEditorListener( this );
		myListTable.setShowHorizontalLines( false );
		myListTable.setShowVerticalLines( false );
		ListSelectionModel lsm = myListTable.getSelectionModel();
		lsm.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		//myListTable.setColumnSelectionAllowed( false );
		//myListTable.setRowSelectionAllowed( false );
		//myListTable.setCellSelectionEnabled( false );
		lsm.addListSelectionListener( this );
		myPainList = new JScrollPane( myListTable );

		JPanel ret = new JPanel( new GridBagLayout() );

		GridBagConstraints c = new GridBagConstraints();

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		ret.add( new JButton( action_NEW ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( new JButton( action_DELETE ), c );

		ret.add( Box.createVerticalStrut(4), c );

		c.weightx = c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTH;
		ret.add( myPainList, c );

		Dimension dim = myListTable.getPreferredSize();
		dim.height = INT_PREF_HEIGHT;
		dim.width = (int)160;
		//myListTable.setPreferredSize( dim );
		myPainList.setPreferredSize( dim );

		myListTable.setBorder( BorderFactory.createMatteBorder( 0,0,1,1,Color.black ) );
		ret.setBorder( BorderFactory.createEmptyBorder( 8,8,1,1 ) );

		//JPanel cont = new JPanel( new GridLayout() );
		//GridBagConstraints gbc = new GridBagConstraints();
		//gbc.fill = GridBagConstraints.BOTH;
		//cont.add( ret );//, gbc );
		//cont.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );

		return ret;
	}

	public JComponent makeEditPanel()
	{
		myTableModel = new EnumValuesTableModel( true );
		myJTable = new JTable( myTableModel );

		TableColumnModel tcm = myJTable.getColumnModel();

		TableColumn colValue = tcm.getColumn( EnumValuesTableModel.COL_NAME );
		if( myListModel != null ) colValue.setCellRenderer( myListModel );
		TableCellEditor editor = colValue.getCellEditor();
		if( editor == null ) editor = myJTable.getDefaultEditor( Object.class );
		editor.addCellEditorListener( this );

		TableColumn colDefault = tcm.getColumn( EnumValuesTableModel.COL_DEFAULT );

		colDefault.setCellRenderer( new NodePropertiesPanel.CompRenderer() );
		colDefault.setCellEditor( new NodePropertiesPanel.CompEditor() );

		int dw = HuginGenieStyleTableFactory.getPreferredWidthForColumn( colDefault, myJTable ) + INT_HORIZONTAL_CELLPADDING;
		colDefault.setMinWidth( dw );
		colDefault.setMaxWidth( dw );

		myPain = new JScrollPane( myJTable );

		myCheckDisplayAsFlag = new JCheckBox( "display as flag" );
		myCheckDisplayAsFlag.addActionListener( this );

		JPanel ret = new JPanel( new GridBagLayout() );

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.WEST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myCheckDisplayAsFlag, c );

		ret.add( Box.createVerticalStrut(8), c );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		ret.add( new JButton( action_INSERT ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( new JButton( action_REMOVE ), c );

		ret.add( Box.createVerticalStrut(4), c );

		c.weightx = c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		ret.add( myPain, c );

		Dimension dim = ret.getPreferredSize();
		dim.height = INT_PREF_HEIGHT;
		dim.width = (int)170;
		ret.setPreferredSize( dim );

		Border etched = BorderFactory.createEtchedBorder();
		etched = BorderFactory.createTitledBorder( etched, "definition" );
		Border empty = BorderFactory.createEmptyBorder( 4,4,4,4 );
		Border compound = BorderFactory.createCompoundBorder( etched, empty );
		empty = BorderFactory.createEmptyBorder( 4,8,1,1 );
		ret.setBorder( BorderFactory.createCompoundBorder( empty, compound ) );

		//JPanel cont = new JPanel( new GridLayout() );
		//GridBagConstraints gbc = new GridBagConstraints();
		//gbc.fill = GridBagConstraints.BOTH;
		//cont.add( ret );//, gbc );
		//cont.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );

		return ret;
	}

	private void resetEnabledState()
	{
		action_DELETE.setEnabled( myListModel != null && myListModel.getRowCount() > 0 );
		myCheckDisplayAsFlag.setEnabled( myCurrentProperty != null && myTableModel.getRowCount() == (int)2 );
		action_INSERT.setEnabled( myCurrentProperty != null );
		action_REMOVE.setEnabled( myCurrentProperty != null && myTableModel != null && myTableModel.getRowCount() > 2 );
	}

	public final Action action_NEW = new SamiamAction( "New Property", "Create new user property", 'n', null )
	{
		public void actionPerformed( ActionEvent e )
		{
			if( myListModel != null && myListTable != null )
			{
				myListModel.addVirtual( new UserEnumProperty() );
				firePanelEdited();
			}
		}
	};
	public final Action action_DELETE = new SamiamAction( "Delete Property", "Delete selected user property", 'd', null )
	{
		public void actionPerformed( ActionEvent e )
		{
			if( myListModel != null && myListTable != null )
			{
				//myListModel.removeElement( myJList.getSelectedValue() );
				myListModel.removeProperty( myListTable.getSelectedRow() );
				firePanelEdited();
			}
		}
	};
	public final Action action_INSERT = new SamiamAction( "Insert Value", "Insert value", 'i', null )
	{
		public void actionPerformed( ActionEvent e )
		{
			if( myTableModel != null && myJTable != null )
			{
				if( myTableModel.getRowCount() < 3 )
				{
					myCheckDisplayAsFlag.setSelected( false );
					saveCheckBoxState();
				}
				myTableModel.addValue( myJTable.getSelectedRow() );
				firePanelEdited();
			}
		}
	};
	public final Action action_REMOVE = new SamiamAction( "Delete Value", "Remove selected value", 'r', null )
	{
		public void actionPerformed( ActionEvent e )
		{
			if( myTableModel != null && myJTable != null )
			{
				int row = myJTable.getSelectedRow();
				if( row >= 0 )
				{
					myTableModel.removeValue( row );
					firePanelEdited();
				}
			}
		}
	};

	/**
		@since 110403
	*/
	private void firePanelEdited()
	{
		//System.out.println( "UserPropertyEditPanel.firePanelEdited()" );
		resetEnabledState();
		if( myEditListener != null ) myEditListener.actionPerformed( EVENT_PANEL_EDITED );
	}
	public final ActionEvent EVENT_PANEL_EDITED = new ActionEvent( this, (int)0, "UserPropertyEditPanel edited" );
	private ActionListener myEditListener;

	/**
		@since 110403
	*/
	public void setEditListener( ActionListener listener )
	{
		myEditListener = listener;
	}

	/**
		interface CellEditorListener
		@since 110403
	*/
	public void editingStopped(ChangeEvent e)
	{
		firePanelEdited();
	}
	public void editingCanceled(ChangeEvent e){}

	public void actionPerformed( ActionEvent e )
	{
		Object src = e.getSource();
		if( src == myCheckDisplayAsFlag && myFlagListenCBDAF )
		{
			saveCheckBoxState();
			firePanelEdited();
		}
	}

	public void saveCheckBoxState()
	{
		if( myCurrentProperty != null )
		{
			//System.out.println( myCurrentProperty + " disp as flag " + myCheckDisplayAsFlag.isSelected() );
			myCurrentProperty.setIsFlag( myCheckDisplayAsFlag.isSelected() );
		}
	}

	public void valueChanged( ListSelectionEvent e )
	{
		//int index = e.getFirstIndex();
		//UserEnumProperty virtual = (UserEnumProperty) myJList.getSelectedValue();
		UserEnumProperty virtual = null, actual = null;
		int index = myListTable.getSelectedRow();
		if( index >= 0 ){
			virtual = (UserEnumProperty) myListModel.getValueAt( index, 0 );
			actual = myListModel.getActualValueAt( index );
		}
		setProperty( virtual, actual );
	}

	public void setProperty( UserEnumProperty virtual, UserEnumProperty actual )
	{
		if( myCurrentProperty != virtual )
		{
			myCurrentProperty = virtual;
			myTableModel.setProperty( virtual, actual );

			boolean cbState = false;
			if( myCurrentProperty != null )
			{
				cbState = myCurrentProperty.isFlag();
			}

			myFlagListenCBDAF = false;
			myCheckDisplayAsFlag.setSelected( cbState );
			myFlagListenCBDAF = true;

			resetEnabledState();
		}
	}

	public void commit( BeliefNetwork bn, Collection oldProperties )
	{
		myTableModel.commit( bn );
		myListModel.commit( bn, oldProperties );
	}

	/** @since 010904 */
	public boolean wasPropertyDeleted()
	{
		return (myListModel == null ) ? false : myListModel.wasPropertyDeleted();
	}

	public void editProperties( Collection properties )
	{
		clear();
		myListModel.editProperties( properties );
	}

	public void clear()
	{
		myTableModel.clear();
		myListModel.clear();
	}

	/**
		Test/debug
	*/
	public static void main( String[] args )
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		String path = "c:\\keithcascio\\networks\\cancer_test.net";
		if( args.length > 0 ) path = args[0];

		//final BeliefNetwork bn = new BeliefNetworkImpl();
		BeliefNetwork bn = null;

		try{
			bn = NetworkIO.read( path );
		}catch( Exception e ){
			System.err.println( "Error: Failed to read " + path );
			return;
		}

		bn.setUserEnumProperties( Arrays.asList( new EnumProperty[] { new UserEnumProperty(), new UserEnumProperty() } ) );

		final BeliefNetwork bnFinal = bn;
		final UserPropertyEditPanel upep = new UserPropertyEditPanel();
		final UserEnumProperty prop49 = new UserEnumProperty();
		prop49.setValues( new EnumValue[] { new UserEnumValue( "red", prop49 ), new UserEnumValue( "blue", prop49 ), new UserEnumValue( "green", prop49 ) } );
		//upep.setProperty( prop49 );

		final Collection oldProperties = bn.getUserEnumProperties();

		upep.editProperties( oldProperties );

		Border etched = BorderFactory.createEtchedBorder();
		Border empty1 = BorderFactory.createEmptyBorder( 8,8,8,8 );
		Border empty2 = BorderFactory.createEmptyBorder( 16,16,16,16 );
		Border compound1 = BorderFactory.createCompoundBorder( etched, empty1 );
		Border compound2 = BorderFactory.createCompoundBorder( empty2, compound1 );
		upep.setBorder( compound2 );

		JButton btnCommit = new JButton( "commit" );
		btnCommit.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				upep.commit( bnFinal, oldProperties );
			}
		} );
		JButton btnRemove = new JButton( "old" );
		btnRemove.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				upep.setProperty( prop49, prop49 );
			}
		} );
		JPanel pnlButtons = new JPanel();
		pnlButtons.setLayout( new BoxLayout( pnlButtons, BoxLayout.Y_AXIS ) );
		pnlButtons.add( btnCommit );
		pnlButtons.add( btnRemove );

		JFrame frame = new JFrame( "DEBUG UserPropertyEditPanel" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		Container contentPain = frame.getContentPane();
		contentPain.setLayout( new BorderLayout() );
		contentPain.add( upep, BorderLayout.CENTER );
		contentPain.add( new JLabel( "network: " + path ), BorderLayout.SOUTH );
		contentPain.add( pnlButtons, BorderLayout.EAST );
		frame.pack();
		Util.centerWindow( frame );
		frame.setVisible( true );
	}

	//private DefaultListModel myListModel;
	//private JList myJList;
	private UserPropertyTableModel myListModel;
	private JTable myListTable;
	private JScrollPane myPainList;
	private EnumValuesTableModel myTableModel;
	private JTable myJTable;
	private JScrollPane myPain;
	private JCheckBox myCheckDisplayAsFlag;
	private boolean myFlagListenCBDAF = true;
	private UserEnumProperty myCurrentProperty;

	static public final int INT_HORIZONTAL_CELLPADDING = 4;
}
