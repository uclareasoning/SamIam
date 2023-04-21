package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.*;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;

import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
	@author Keith Cascio
	@since 090803
*/
public class SingleVarEnumEditPanel extends JPanel
{
	public static final int INT_WIDTH_COLUMN_PROPERTY = 170;
	public static final int INT_WIDTH_COLUMN_VALUE = 128;

	public SingleVarEnumEditPanel( BeliefNetwork bn, Variable var )
	{
		super( new GridBagLayout() );

		this.myBeliefNetwork = bn;
		this.myVariable = var;

		init();
	}

	public void reInitialize( SamiamUserMode mode )
	{
		myTableModel.reInitialize( mode );
	}

	public void commit()
	{
		myTableModel.EDITOR_DEFAULT.stopCellEditing();
		myTableModel.commitValues();
	}

	private void init()
	{
		myTableModel = new SingleVarEnumTableModel( myBeliefNetwork, myVariable );
		myJTable = new JTable( myTableModel );
		myJTable.setDragEnabled( false );
		TableCellRenderer rendererDefault = myJTable.getDefaultRenderer( Object.class );
		myTableModel.setDefaultTableCellRenderer( rendererDefault );
		myJTable.setDefaultRenderer( Object.class, myTableModel );
		myJTable.setDefaultEditor( Object.class, null );

		myPain = new JScrollPane( myJTable );

		TableColumnModel columnModel = myJTable.getColumnModel();

		TableColumn column = columnModel.getColumn( SingleVarEnumTableModel.INT_COLUMN_PROPERTY_NAME );
		column.setPreferredWidth( INT_WIDTH_COLUMN_PROPERTY );
		column.setWidth( INT_WIDTH_COLUMN_PROPERTY );

		column = columnModel.getColumn( SingleVarEnumTableModel.INT_COLUMN_VALUE );
		column.setPreferredWidth( INT_WIDTH_COLUMN_VALUE );
		column.setWidth( INT_WIDTH_COLUMN_VALUE );
		column.setCellEditor( myTableModel.EDITOR_DEFAULT );

		Dimension dim = myPain.getPreferredSize();
		dim.width = INT_WIDTH_COLUMN_PROPERTY + INT_WIDTH_COLUMN_VALUE;
		dim.height = 180;
		myPain.setPreferredSize( dim );

		GridBagConstraints c = new GridBagConstraints();

		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		add( myPain, c );
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

		BeliefNetwork bn = null;

		try{
			bn = NetworkIO.read( path );
		}catch( Exception e ){
			System.err.println( "Error: Failed to read " + path );
			return;
		}

		final Iterator it = bn.iterator();

		final SingleVarEnumEditPanel sveep = new SingleVarEnumEditPanel( bn, (Variable) it.next() );
		sveep.reInitialize( (SamiamUserMode)null );

		Border etched = BorderFactory.createEtchedBorder();
		Border empty1 = BorderFactory.createEmptyBorder( 8,8,8,8 );
		Border empty2 = BorderFactory.createEmptyBorder( 16,16,16,16 );
		Border compound1 = BorderFactory.createCompoundBorder( etched, empty1 );
		Border compound2 = BorderFactory.createCompoundBorder( empty2, compound1 );
		sveep.setBorder( compound2 );

		JButton btnCommit = new JButton( "new variable" );
		btnCommit.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				if( it.hasNext() )
				{
					sveep.myTableModel.setVariable( (Variable) it.next() );
					sveep.myTableModel.refreshValues();
				}
			}
		} );
		JButton btnRemove = new JButton( "btn2" );
		btnRemove.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
			}
		} );
		JPanel pnlButtons = new JPanel();
		pnlButtons.setLayout( new BoxLayout( pnlButtons, BoxLayout.Y_AXIS ) );
		pnlButtons.add( btnCommit );
		pnlButtons.add( btnRemove );

		JFrame frame = new JFrame( "DEBUG SingleVarEnumEditPanel" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		Container contentPain = frame.getContentPane();
		contentPain.setLayout( new BorderLayout() );
		contentPain.add( sveep, BorderLayout.CENTER );
		contentPain.add( new JLabel( "network: " + path ), BorderLayout.SOUTH );
		contentPain.add( pnlButtons, BorderLayout.EAST );
		frame.pack();
		Util.centerWindow( frame );
		frame.setVisible( true );
	}

	private BeliefNetwork myBeliefNetwork;
	private Variable myVariable;
	private SingleVarEnumTableModel myTableModel;
	private JTable myJTable;
	private JScrollPane myPain;
}
