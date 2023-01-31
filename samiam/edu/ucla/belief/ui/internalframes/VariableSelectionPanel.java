package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.*;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;

import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;

import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
	@author Keith Cascio
	@since 082003
*/
public class VariableSelectionPanel extends JPanel implements NodePropertyChangeListener, ActionListener
{
	public static final int INT_LIST_WIDTH = 200;
	public static final String STR_ITEM_EMPTY = "                 ";

	public VariableSelectionPanel( BeliefNetwork variables )
	{
		super( new GridBagLayout() );

		this.myBeliefNetwork = variables;

		init();
	}

	public void reInitialize()
	{
		Object itemSelected = myComboProperty.getSelectedItem();
		clear();

		myFlagListenComboProperty = false;
		DefaultComboBoxModel model = new DefaultComboBoxModel( myBeliefNetwork.propertiesAsArray() );
		myComboProperty.setModel( model );
		if( model.getIndexOf( itemSelected ) >= 0 ) myComboProperty.setSelectedItem( itemSelected );
		myFlagListenComboProperty = true;

		doPropertySelected();
	}

	private void init()
	{
		myComboProperty = new JComboBox( myBeliefNetwork.propertiesAsArray() );
		myComboProperty.addActionListener( this );
		myValueModel = new DefaultComboBoxModel();
		myValueModel.addElement( STR_ITEM_EMPTY );
		myComboValue = new JComboBox( myValueModel );
		myComboValue.addActionListener( this );

		myJList = new JList();
		myJList.setSelectionModel( new DefaultListSelectionModel()
		{
			public boolean isSelectionEmpty()
			{
				return true;
			}
			public boolean isSelectedIndex(int index)
			{
				return false;
			}
		} );
		ListCellRenderer renderer = new VariableLabelRenderer( myJList.getCellRenderer(), myBeliefNetwork );
		myJList.setCellRenderer( renderer );
		myPain = new JScrollPane( myJList );

		GridBagConstraints c = new GridBagConstraints();

		c.gridwidth = 1;
		add( myComboProperty, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		add( myComboValue, c );

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		add( myPain, c );

		Dimension dim = getPreferredSize();
		dim.width = 256;
		dim.height = 128;
		//setPreferredSize( dim );

		//setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
	}

	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		//System.out.println( "VariableSelectionPanel.nodePropertyChanged( "+e+" )" );
		if( e.isEnumPropertyChange() && myComboProperty.getSelectedItem() == e.property )
		{
			doValueSelected();
		}
	}

	public void setEnabled( boolean flag )
	{
		//System.out.println( "VariableSelectionPanel.setEnabled( "+flag+" )" );
		super.setEnabled( flag );
		myComboProperty.setEnabled( flag );
		myComboValue.setEnabled( flag );
		myJList.setEnabled( flag );
	}

	public void clear()
	{
		myFlagListenComboProperty = false;
		myFlagListenComboValue = false;
		myComboProperty.setSelectedIndex( 0 );
		myValueModel.removeAllElements();
		myValueModel.addElement( STR_ITEM_EMPTY );
		myJList.setModel( new DefaultListModel() );
		myFlagListenComboValue = true;
		myFlagListenComboProperty = true;
	}

	public void actionPerformed( ActionEvent e )
	{
		Object src = e.getSource();
		if( src == myComboProperty && myFlagListenComboProperty ) doPropertySelected();
		else if( src == myComboValue && myFlagListenComboValue ) doValueSelected();
	}

	private void doValueSelected()
	{
		if( myComboValue.getSelectedItem() != STR_ITEM_EMPTY )
		{
			EnumProperty property = (EnumProperty) myComboProperty.getSelectedItem();
			EnumValue value = (EnumValue) myComboValue.getSelectedItem();
			if( value != null && property != null ) load( property, value );
		}
	}

	private void doPropertySelected()
	{
		//System.out.println( "VariableSelectionPanel.doPropertySelected(): " + myComboProperty.getSelectedItem() );
		EnumProperty property = (EnumProperty) myComboProperty.getSelectedItem();
		if( property != null )
		{
			myValueModel.removeAllElements();
			for( Iterator it = property.iterator(); it.hasNext(); )
			{
				myValueModel.addElement( it.next() );
			}
		}
	}

	public Iterator iterator()
	{
		return myData.iterator();
	}

	public List getVariables()
	{
		return Collections.unmodifiableList( myData );
	}

	public void load( EnumProperty property, EnumValue value )
	{
		//System.out.print( "VariableSelectionPanel.load( "+property+", "+value+" )" );
		myData.clear();

		boolean flagDefault = ( value == property.getDefault() );

		Variable next;
		Object nextValue;
		for( Iterator it = myBeliefNetwork.iterator(); it.hasNext(); )
		{
			next = (Variable) it.next();
			nextValue = next.getProperty( property );
			//System.out.print( " " + next + " -> " + nextValue );
			if( (nextValue == null && flagDefault) || nextValue == value ) myData.add( next );
		}

		myListModel = new SortedListModel( myData, VariableComparator.getInstance(), myBeliefNetwork.size() );

		myJList.setModel( myListModel );

		//System.out.println();
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

		VariableSelectionPanel vsp = new VariableSelectionPanel( bn );

		Border etched = BorderFactory.createEtchedBorder();
		Border empty1 = BorderFactory.createEmptyBorder( 8,8,8,8 );
		Border empty2 = BorderFactory.createEmptyBorder( 16,16,16,16 );
		Border compound1 = BorderFactory.createCompoundBorder( etched, empty1 );
		Border compound2 = BorderFactory.createCompoundBorder( empty2, compound1 );
		vsp.setBorder( compound2 );

		JFrame frame = new JFrame( "DEBUG VariableSelectionPanel" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		Container contentPain = frame.getContentPane();
		contentPain.setLayout( new BorderLayout() );
		contentPain.add( vsp, BorderLayout.CENTER );
		contentPain.add( new JLabel( "network: " + path ), BorderLayout.SOUTH );
		frame.pack();
		Util.centerWindow( frame );

		vsp.reInitialize();
		frame.setVisible( true );
	}

	private List myData = new LinkedList();
	private JComboBox myComboProperty;
	private boolean myFlagListenComboProperty = true;
	private JComboBox myComboValue;
	private boolean myFlagListenComboValue = true;
	private DefaultComboBoxModel myValueModel;
	private JList myJList;
	private SortedListModel myListModel;
	private JScrollPane myPain;
	private BeliefNetwork myBeliefNetwork;
}
