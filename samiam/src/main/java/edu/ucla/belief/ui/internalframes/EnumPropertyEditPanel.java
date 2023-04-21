package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.*;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.Stringifier;
import edu.ucla.util.VariableStringifier;
import edu.ucla.util.AbstractStringifier;

import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.tabledisplay.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.tree.Hierarchy;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/** @author keith cascio
	@since  20030820 */
public class EnumPropertyEditPanel extends JPanel implements NodePropertyChangeListener, ActionListener, Stringifier.Selector
{
	public static final int INT_WIDTH_COLUMN_VARIABLE = 170,
	                        INT_WIDTH_COLUMN_VALUE    = 128;

	public static final String STR_ACHTUNG = "click here";

	public EnumPropertyEditPanel( BeliefNetwork bn )
	{
		super( new GridBagLayout() );

		this.myBeliefNetwork = bn;

		init();
	}

	public void reInitialize()
	{
		myFlagListenToComboProperty = false;
		EnumProperty[] array = myBeliefNetwork.propertiesAsArray();
		DefaultComboBoxModel model = new DefaultComboBoxModel( array );
		myComboProperty.setModel( model );
		myFlagListenToComboProperty = true;
		myTableModel.reInitialize();
		setVariables( myBeliefNetwork );

		if( myFlagNotVirgin )
		{
			EnumProperty property = myProperty;
			int index = model.getIndexOf( property );
			if( index == (int)-1 ) property = array[ (index = (int)0) ];
			myProperty = null;
			setProperty( property, index );
		}
	}

	/** interface Stringifier.Selector
		@since 20070310 */
	public Stringifier selectStringifier(){
		return selectVariableStringifier();
	}

	public VariableStringifier selectVariableStringifier(){
		VariableStringifier ret  = AbstractStringifier.VARIABLE_ID;
		if( !(this.myBeliefNetwork instanceof DisplayableBeliefNetwork) ) return ret;

		try{
			boolean flagLabels   = ((Boolean)((DisplayableBeliefNetwork) this.myBeliefNetwork).getNetworkInternalFrame().getPackageOptions().getMappedPreference( SamiamPreferences.displayNodeLabelIfAvail ).getValue()).booleanValue();
			if( flagLabels ) ret = AbstractStringifier.VARIABLE_LABEL;
		}catch( Exception exception ){
			System.err.println( "warning: EPEP.getPreferredStringifier() caught " + exception );
		}
		return ret;
	}

	public void setVariables( Collection variables )
	{
		myVariableLabelRenderer.recalculateDisplayValues( variables );
		myTableModel.setVariables( variables );
		resetButtonState();
	}

	public void commitValues()
	{
		myTableModel.commitValues();
	}

	/** @return true if the table changed as a result of this call */
	public boolean setDisplayedSelectedRows( boolean display ){
		boolean ret = false;
		try{
			if( myTableModel == null || myJTable == null ) return ret;

			int[] rows = myJTable.getSelectedRows();
			if( rows.length > 0 ) ret = myTableModel.setRowsDisplayed( rows, display );
		}catch( Exception exception ){
			System.err.println( "warning: EPEP.setDisplayedSelectedRows() caught " + exception );
		}
		return ret;
	}

	/** @since 071304 */
	public void setProperty( EnumProperty property )
	{
		DefaultComboBoxModel model = (DefaultComboBoxModel) myComboProperty.getModel();
		int index = model.getIndexOf( property );
		if( index == (int)-1 ){
			EnumProperty[] array = myBeliefNetwork.propertiesAsArray();
			property = array[ (index = (int)0) ];
		}
		myProperty = null;
		setProperty( property, index );
	}

	public void setProperty( EnumProperty property, int index )
	{
		if( myProperty != property )
		{
			myProperty = property;
			myFlagListenToComboValue = false;
			myValueModel.removeAllElements();
			for( Iterator it = property.iterator(); it.hasNext(); )
			{
				myValueModel.addElement( it.next() );
			}
			myFlagListenToComboValue = true;
			if( property.isFlag() ) myColumnValue.setCellEditor( myEditorCheck );
			else myColumnValue.setCellEditor( myEditorCombo );
			if( myColumnValue.getHeaderValue() != property ) myColumnValue.setHeaderValue( property );
			myTableModel.setProperty( property, index );
			resetButtonState();
		}
		myFlagNotVirgin = true;
	}

	private void resetButtonState()
	{
		boolean enabled = (myProperty != null) && (myProperty.isUserEditable());
		//myButtonSetValue.setEnabled( enabled );
		SamiamAction[] muta = getMutators();
		for( int i=0; i<muta.length; i++ ) muta[i].setEnabled( enabled );
	}

	/** @since 20070324 */
	public SamiamAction[] getMutators(){
		if( myMutators == null ) myMutators = new SamiamAction[] { action_TARGETALL };
		return myMutators;
	}

	private void init()
	{
		myComboProperty = new JComboBox( myBeliefNetwork.propertiesAsArray() );
		myComboProperty.addActionListener( this );
		myValueModel = new DefaultComboBoxModel();
		myValueModel.addElement( "                 " );
		myComboValue = new JComboBox( myValueModel );
		//myComboValue.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
		//myComboValue.addActionListener( this );
		//myButtonSetValue = new JButton( "Reset values to:" );
		//myButtonSetValue.addActionListener( this );

		myTableModel = Bridge2Tiger.Troll.solicit().newEnumTableModel( myBeliefNetwork );
		myJTable = new JTable( myTableModel );
		myJTable.setDragEnabled( false );
		TableCellRenderer rendererDefault = myJTable.getDefaultRenderer( Object.class );
		myTableModel.setDefaultTableCellRenderer( rendererDefault );
		myJTable.setDefaultRenderer( Object.class, myTableModel );
		//myJTable.setDefaultEditor( Object.class, myTableModel.getEditor() );

		TableColumnModel columnModel = myJTable.getColumnModel();
		EditableHeader editableHeader = new EditableHeader(columnModel);
		myJTable.setTableHeader( editableHeader );

		myVariableLabelRenderer = new VariableLabelRenderer( rendererDefault, Collections.EMPTY_SET );
		myJTable.getColumnModel().getColumn(0).setCellRenderer( myVariableLabelRenderer );

		EditableHeaderTableColumn colVariable = (EditableHeaderTableColumn)myJTable.getColumnModel().getColumn(0);
		colVariable.setHeaderEditable( false );

		myColumnValue = (EditableHeaderTableColumn)myJTable.getColumnModel().getColumn(1);
		//myColumnValue.setHeaderValue( myComboProperty.getItemAt(0) );
		myColumnValue.setHeaderValue( STR_ACHTUNG );
		DefaultCellEditor editorValueHeader = new DefaultCellEditor( myComboProperty );
		myColumnValue.setHeaderEditor( editorValueHeader );
		new Stopper( myComboProperty, editorValueHeader );
		TableCellRenderer headerRenderer = myColumnValue.getHeaderRenderer();
		if( headerRenderer == null ) headerRenderer = editableHeader.getDefaultRenderer();
		myColumnValue.setHeaderRenderer( new AchtungRenderer( headerRenderer ) );
		////TableCellRenderer rendererValueHeader = myColumnValue.getHeaderRenderer();//null
		//TableCellRenderer rendererValueHeader = editableHeader.getDefaultRenderer();
		////Component comp = rendererValueHeader.getTableCellRendererComponent(myJTable,"test",false,false,1,1);
		////comp.setBackground( Color.red );
		////comp.setForeground( Color.green );
		//rendererValueHeader = new HuginGenieStyleTableFactory.ForceBackgroundColorCellRenderer( rendererValueHeader, Color.red );
		//myColumnValue.setHeaderRenderer( rendererValueHeader );
		//myColumnValue.setCellRenderer( new HuginGenieStyleTableFactory.ForceBackgroundColorCellRenderer( rendererDefault, Color.red ) );

		myComboEditor = new JComboBox( myTableModel.getValueModel() );
		myEditorCombo = new DefaultCellEditor( myComboEditor );
		new Stopper( myComboEditor, myEditorCombo );
		myEditorCheck = new DefaultCellEditor( new JCheckBox() );

		myPain = new JScrollPane( myJTable );

		TableColumn column = myJTable.getColumnModel().getColumn( 0 );
		column.setPreferredWidth( INT_WIDTH_COLUMN_VARIABLE );
		//column.setMaxWidth( INT_WIDTH_COLUMN_VARIABLE );
		//column.setMinWidth( INT_WIDTH_COLUMN_VARIABLE );
		column.setWidth( INT_WIDTH_COLUMN_VARIABLE );

		column = myJTable.getColumnModel().getColumn( 1 );
		column.setPreferredWidth( INT_WIDTH_COLUMN_VALUE );
		//column.setMaxWidth( INT_WIDTH_COLUMN_VALUE );
		//column.setMinWidth( INT_WIDTH_COLUMN_VALUE );
		column.setWidth( INT_WIDTH_COLUMN_VALUE );

		Dimension dim = myPain.getPreferredSize();
		dim.width = INT_WIDTH_COLUMN_VARIABLE + INT_WIDTH_COLUMN_VALUE;
		dim.height = 150;
		myPain.setPreferredSize( dim );

		GridBagConstraints c = new GridBagConstraints();

		c.weightx   = c.weighty = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor    = GridBagConstraints.CENTER;
		c.fill      = GridBagConstraints.BOTH;
		add( myPain,                         c );
		////c.weightx   =  c.weighty = 0;
		////add( Box.createHorizontalStrut( 0x80 ),   c );

		/*c.weighty   = 0;
		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		add( Box.createVerticalStrut( 4 ),   c );

		c.anchor    = GridBagConstraints.EAST;
		c.gridwidth = 1;
		c.weightx   = 1;
		add( myButtonSetValue,               c );

		c.weightx   = 0;
		add( Box.createHorizontalStrut( 4 ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill      = GridBagConstraints.HORIZONTAL;
		c.weightx   = 1;
		add( myComboValue,                   c );*/

		EnumPropertyEditPanel.this.setMinimumSize( new Dimension( 0x80, 0x20 ) );
	}

	public class AchtungRenderer implements TableCellRenderer
	{
		public AchtungRenderer( TableCellRenderer renderer )
		{
			myTableCellRenderer = renderer;

			Component ret = myTableCellRenderer.getTableCellRendererComponent( myJTable, "test", false, false, 0, 0 );
			myDefault = ret.getForeground();
		}

		public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column)
		{
			Component ret = myTableCellRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			if( value == STR_ACHTUNG ) ret.setForeground( myAchtung );
			return ret;
		}

		public void setAchtung( Color color )
		{
			myAchtung = color;
		}

		private Color myDefault;
		private Color myAchtung = Color.red;
		//private Color myColor = myAchtung;
		private TableCellRenderer myTableCellRenderer;
	}

	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		//System.out.println( "EnumPropertyEditPanel.nodePropertyChanged( "+e+" )" );
		if( e.isEnumPropertyChange() ) myTableModel.refreshValue( e.variable, e.property );
	}

	private boolean myFlagListenToComboValue = true;
	private boolean myFlagListenToComboProperty = true;

	public void actionPerformed( ActionEvent e )
	{
		Object src = e.getSource();
		if( src == myComboProperty && myFlagListenToComboProperty )
		{
			EnumProperty property = (EnumProperty) myComboProperty.getSelectedItem();
			int index = myComboProperty.getSelectedIndex();
			if( property != null ) setProperty( property, index );
		}
		/*else if( src == myButtonSetValue && myFlagListenToComboValue )
		{
			EnumProperty property = (EnumProperty) myColumnValue.getHeaderValue();//NOT! myComboProperty.getSelectedItem();
			myTableModel.setValues( property, getTargetValue() );//EnumTableModel
		}*/
	}

	/** @since 20070324 */
	public EnumValue getTargetValue(){
		try{
			return (EnumValue) myComboValue.getSelectedItem();
		}catch( Exception exception ){
			System.err.println( "warning: EPEP.getTargetValue() caught " + exception );
		}
		return null;
	}

	/** @since 20070324 */
	public final SamiamAction action_TARGETALL = new SamiamAction( "all", "set target value on all rows", 'a', null ){
		public void actionPerformed( ActionEvent e ){
			try{
				EnumProperty property = (EnumProperty) myColumnValue.getHeaderValue();//NOT! myComboProperty.getSelectedItem();
				myTableModel.setValues( property, getTargetValue() );//EnumTableModel
			}catch( Exception exception ){
				System.err.println( "warning: EPEP.action_TARGETALL.aP() caught " + exception );
			}
		}
	};

	/** @since 20070326 */
	public final SamiamAction action_RETAIN = new SamiamAction( "retain", "hide all unselected rows", 'r', null ){
		public void actionPerformed( ActionEvent e ){
			EnumPropertyEditPanel.this.setDisplayedSelectedRows( true );
		}
	};

	public final SamiamAction action_REMOVE = new SamiamAction( "hide", "hide selected variables from edit list", 'h', null ){
		public void actionPerformed( ActionEvent e ){
			EnumPropertyEditPanel.this.setDisplayedSelectedRows( false );
		}
	};

	public final SamiamAction action_EDITALL = new SamiamAction( "show all", "add all network variables to the edit list", 'a', null ){
		public void actionPerformed( ActionEvent e ){
			if( myBeliefNetwork != null ) EnumPropertyEditPanel.this.setVariables( myBeliefNetwork );
		}
	};

	/** @since 20070324 */
	public final SamiamAction action_SELECTALL = new SamiamAction( "all", "select all", 'a', null ){
		public void actionPerformed( ActionEvent e ){
			try{
				myJTable.setRowSelectionInterval( 0, myJTable.getRowCount()-1 );
			}catch( Exception exception ){
				System.err.println( "warning: EPEP.action_SELECTALL.aP() caught " + exception );
			}
		}
	};

	/** @since 20070324 */
	public final SamiamAction action_SELECTNONE = new SamiamAction( "none", "deselect all rows", 'n', null ){
		public void actionPerformed( ActionEvent e ){
			try{
				myJTable.clearSelection();
			}catch( Exception exception ){
				System.err.println( "warning: EPEP.action_SELECTNONE.aP() caught " + exception );
			}
		}
	};

	/** @since 20070324 */
	public final SamiamAction action_INVERTSELECTION = new SamiamAction( "invert", "deselect all selected rows and select all unselected rows", 'i', null ){
		public void actionPerformed( ActionEvent e ){
			try{
				int                rows = myJTable.getRowCount();
				ListSelectionModel lsm  = myJTable.getSelectionModel();
				if( lsm.isSelectionEmpty() ){
					lsm.setSelectionInterval( 0, rows );
					return;
				}

				for( int i=0; i<rows; i++ ){
					if( lsm.isSelectedIndex(i) ) lsm.removeSelectionInterval( i, i );
					else                         lsm.   addSelectionInterval( i, i );
				}
			}catch( Exception exception ){
				System.err.println( "warning: EPEP.action_INVERTSELECTION.aP() caught " + exception );
			}
		}
	};

	/** @since 20070324 */
	public Hierarchy getHierarchySet(){
		if( myHierarchySet != null ) return myHierarchySet;

		try{
			myHierarchySet = new Hierarchy( "set", "apply the target value to a subset of variables" );
			myHierarchySet.add( action_TARGETALL );
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel.getHierarchySet() caught " + exception );
		}

		return myHierarchySet;
	}

	/** @since 20070324 */
	public Hierarchy getHierarchySelected(){
		if( myHierarchySelected != null ) return myHierarchySelected;

		try{
			myHierarchySelected = new Hierarchy( "selected", "perform these operations on all selected variables" );
			myHierarchySelected.add( action_REMOVE );
			myHierarchySelected.add( action_RETAIN );
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel.getHierarchySelected() caught " + exception );
		}

		return myHierarchySelected;
	}

	/** @since 20070324 */
	public Hierarchy getHierarchySelect(){
		if( myHierarchySelect != null ) return myHierarchySelect;

		try{
			myHierarchySelect = new Hierarchy( "select", "change which variables are selected in the table" );
			myHierarchySelect.add( action_SELECTALL       );
			myHierarchySelect.add( action_SELECTNONE      );
			myHierarchySelect.add( action_INVERTSELECTION );
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel.getHierarchySelect() caught " + exception );
		}

		return myHierarchySelect;
	}

	/** @since 20070324 */
	public Hierarchy getHierarchyView(){
		if( myHierarchyView != null ) return myHierarchyView;

		try{
			myHierarchyView = new Hierarchy( "view", "view settings" );
			myHierarchyView.add( action_EDITALL );
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel.getHierarchyView() caught " + exception );
		}

		return myHierarchyView;
	}

	/** @since 20070324 */
	public Hierarchy getTargetHierarchy(){
		if( myHierarchyTarget != null ) return myHierarchyTarget;

		try{
			JPanel             pnlTarget = new JPanel( new GridBagLayout() );
			GridBagConstraints c         = new GridBagConstraints();
			JLabel             lblTarget = new JLabel( "target:" );
			pnlTarget.add( lblTarget,                    c );
			pnlTarget.add( Box.createHorizontalStrut(4), c );
			c.fill    = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			pnlTarget.add( myComboValue,                 c );

			lblTarget.setToolTipText( "choose a target value then apply these operations" );
			myHierarchyTarget = new Hierarchy( pnlTarget, lblTarget );

			myHierarchyTarget.add( getHierarchySet() );
		}catch( Exception exception ){
			System.err.println( "warning: EnumPropertyEditPanel.getTargetHierarchy() caught " + exception );
			exception.printStackTrace();
		}

		return myHierarchyTarget;
	}

	/**
		@author Keith Cascio
		@since 110403
	*/
	public static class Stopper implements Runnable, PopupMenuListener
	{
		public Stopper( JComboBox combo, TableCellEditor editor )
		{
			myJComboBox = combo;
			myEditor = editor;
			myJComboBox.addPopupMenuListener( this );
		}

		public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
		{
			invokeLater();
		}
		public void popupMenuCanceled(PopupMenuEvent e)
		{
			invokeLater();
		}
		public void popupMenuWillBecomeVisible(PopupMenuEvent e){}

		public void invokeLater()
		{
			SwingUtilities.invokeLater( this );
		}

		public void run()
		{
			myEditor.cancelCellEditing();
		}

		private TableCellEditor myEditor;
		private JComboBox myJComboBox;
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

		final EnumPropertyEditPanel epep = Bridge2Tiger.Troll.solicit().newEnumPropertyEditPanel( bn );
		epep.reInitialize();

		Border etched = BorderFactory.createEtchedBorder();
		Border empty1 = BorderFactory.createEmptyBorder( 8,8,8,8 );
		Border empty2 = BorderFactory.createEmptyBorder( 16,16,16,16 );
		Border compound1 = BorderFactory.createCompoundBorder( etched, empty1 );
		Border compound2 = BorderFactory.createCompoundBorder( empty2, compound1 );
		epep.setBorder( compound2 );

		JButton btnCommit = new JButton( "commit" );
		btnCommit.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				epep.commitValues();
			}
		} );
		JButton btnRemove = new JButton( "remove" );
		btnRemove.addActionListener( new ActionListener(){
			public void actionPerformed( ActionEvent e ){
				epep.setDisplayedSelectedRows( false );
			}
		} );
		JPanel pnlButtons = new JPanel();
		pnlButtons.setLayout( new BoxLayout( pnlButtons, BoxLayout.Y_AXIS ) );
		pnlButtons.add( btnCommit );
		pnlButtons.add( btnRemove );

		JFrame frame = new JFrame( "DEBUG EnumPropertyEditPanel" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		Container contentPain = frame.getContentPane();
		contentPain.setLayout( new BorderLayout() );
		contentPain.add( epep, BorderLayout.CENTER );
		contentPain.add( new JLabel( "network: " + path ), BorderLayout.SOUTH );
		contentPain.add( pnlButtons, BorderLayout.EAST );
		frame.pack();
		Util.centerWindow( frame );
		frame.setVisible( true );
	}

	private boolean                   myFlagNotVirgin = false;
	private VariableLabelRenderer     myVariableLabelRenderer;
	private EditableHeaderTableColumn myColumnValue;
	private JComboBox                 myComboEditor;
	private DefaultCellEditor         myEditorCombo, myEditorCheck;
	protected JComboBox               myComboProperty, myComboValue;
	//private JButton                   myButtonSetValue;
	private DefaultComboBoxModel      myValueModel;
	protected EnumTableModel          myTableModel;
	private EnumProperty              myProperty;
	protected JTable                  myJTable;
	private JScrollPane               myPain;
	protected BeliefNetwork           myBeliefNetwork;
	protected SamiamAction[]          myMutators;
	protected Hierarchy               myHierarchyTarget, myHierarchySet, myHierarchySelected, myHierarchyView, myHierarchySelect;
}
