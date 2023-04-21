package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.tabledisplay.*;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;

import java.util.Collections;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import edu.ucla.belief.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.sensitivity.ExcludePolicy;
import edu.ucla.belief.decision.*;

/** Code lifted from DisplayableFiniteVariableImpl
	@author Keith Cascio
	@since 021405 Valentine's Day! */
public class NodePropertiesPanel extends JPanel implements CellEditorListener, ActionListener
{
	public NodePropertiesPanel( DisplayableFiniteVariableImpl dvar ){
		super( new GridBagLayout() );
		this.myDFV = dvar;
		init();
	}

	/** @since 021405 Valentine's Day! */
	public static boolean isEditable( DisplayableFiniteVariable dVar ){
		return isEditable( dVar.getNetworkInternalFrame().getSamiamUserMode() );
	}

	/** @since 021405 Valentine's Day! */
	public static boolean isEditable( SamiamUserMode SUM ){
		return
			SUM.contains( SamiamUserMode.EDIT ) &&
			(!SUM.contains( SamiamUserMode.SMILEFILE )) &&
			(!SUM.contains( SamiamUserMode.READONLY ));
	}

	/** @since 030102 */
	private void init()
	{
		boolean flagEditable = isEditable( myDFV );

		JLabel lblIdentifier = new JLabel( "Identifier: " );
		JLabel lblName = new JLabel( "Name: " );
		JLabel lblRepresentation = new JLabel( "Representation: " );
		//JLabel lblType = new JLabel( "Type: " );
		//JLabel lblPolicy = new JLabel( "Sensitivity policy: " );
		myButtonInsertState = new JButton( "Insert" );
		setupButton( myButtonInsertState );
		myButtonDeleteState = new JButton( "Remove" );
		setupButton( myButtonDeleteState );
		int COLUMNS = 10;
		myTfIdentifier = new IdentifierField( myDFV.getID(), COLUMNS );
		myTfIdentifier.addActionListener( (ActionListener)this );
		myTfName = new NotifyField( myDFV.getLabel(), COLUMNS );
		myTfName.addActionListener( (ActionListener)this );
		int WIDTH = 90;
		int HEIGHT = myTfIdentifier.getPreferredSize().height;
		Dimension dim = new Dimension( WIDTH, HEIGHT );
		myCboRepresentation = new JComboBox( edu.ucla.belief.io.dsl.DSLNodeType.getArrayInterconvertibleTypes(  myDFV.getDSLNodeType() ) );
		myCboRepresentation.setSelectedItem( myUncommitedRepresentation = myDFV.getDSLNodeType() );
		myCboRepresentation.setPreferredSize( dim );
		myCboRepresentation.addActionListener( (ActionListener)this );
		//JComboBox cboType = new JComboBox( edu.ucla.belief.io.dsl.DiagnosisType.valuesAsArray() );
		//DiagnosisType type = getDiagnosisType();
		//cboType.setSelectedItem( type );
		//cboType.setPreferredSize( dim );
		//myComboPolicy = new JComboBox( ExcludePolicy.ARRAY );
		//myComboPolicy.setSelectedItem( getExcludePolicy() );
		//myComboPolicy.setPreferredSize( dim );
		//myComboPolicy.addActionListener( this );

		JTable myJTableStates = makeStatesTable( flagEditable );
		JScrollPane pain = new JScrollPane( myJTableStates );

		/*
		JCheckBox cbxMandatory = new JCheckBox( "Mandatory" );
		cbxMandatory.setEnabled( type == DiagnosisType.OBSERVATION );
		cbxMandatory.setSelected( getMandatory().booleanValue() );
		JCheckBox cbxRanked = new JCheckBox( "Ranked" );
		cbxRanked.setEnabled( type == DiagnosisType.TARGET );
		cbxRanked.setSelected( getRanked().booleanValue() );
		myCbMAP = new JCheckBox( "MAP" );
		myCbMAP.addActionListener( this );
		myCbMAP.setEnabled( myFlagIsHuginNode );
		myCbMAP.setSelected( isMAPVariable() );

		pnlFlags = new JPanel();
		pnlFlags.add( cbxMandatory );
		pnlFlags.add( cbxRanked );
		pnlFlags.add( myCbMAP );
		pnlFlags.setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "Flags" ) );
		*/
		Component pnlSpacer1 = Box.createHorizontalStrut( 8 );
		Component pnlSpacer2 = Box.createHorizontalStrut( 8 );
		Component vstrut1 = Box.createVerticalStrut( 8 );
		Component vstrut2 = Box.createVerticalStrut( 8 );

		pnlButtons = new JPanel();
		pnlButtons.add( myButtonInsertState );
		pnlButtons.add( myButtonDeleteState );
		pnlButtons.setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), "State Actions" ) );

		/*
		//Keith Cascio 052002
		//This code is not necessary because GridBagLayout can be
		//set to fill the components vertically.

		Dimension szFlags = pnlFlags.getPreferredSize();
		Dimension szButtons = pnlButtons.getPreferredSize();
		int newHeight = Math.max( szFlags.height, szButtons.height );
		szFlags.height = newHeight;
		szButtons.height = newHeight;
		pnlFlags.setPreferredSize( szFlags );
		pnlButtons.setPreferredSize( szButtons );
		*/

		//JPanel pnlGridbag = new JPanel();
		//GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		//pnlGridbag.setLayout( gridbag );

		c.weighty = 0;
		c.ipadx = 4;
		c.ipady = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add(vstrut1, c);
		c.gridy++;
		c.gridwidth = 1;
		this.add(lblIdentifier, c);
		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = (double)1;
		this.add(myTfIdentifier, c);
		c.fill = GridBagConstraints.NONE;
		c.gridx++;
		c.weightx = (double)0;
		this.add(pnlSpacer1, c);
		c.gridx++;
		this.add(lblRepresentation, c);
		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add(myCboRepresentation, c);
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy++;
		this.add(lblName, c);
		c.gridx++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = (double)1;
		this.add(myTfName, c);
		c.fill = GridBagConstraints.NONE;
		c.gridx++;
		c.weightx = (double)0;
		this.add(pnlSpacer2, c);

		boolean legacy = false;
		if(     legacy ){
			c.gridx++;
			//this.add(lblType, c);
			this.add(Box.createHorizontalStrut(2), c);
			c.gridx++;
			c.fill = GridBagConstraints.HORIZONTAL;
			//this.add(cboType, c);
			this.add(Box.createHorizontalStrut(2), c);

			//c.gridy++;
			//c.gridx = 3;
			//this.add(lblPolicy, c);
			//c.gridx++;
			//c.fill = GridBagConstraints.HORIZONTAL;
			//this.add(myComboPolicy, c);

			//c.gridx = 0;
			c.gridx = 3;
			c.gridy++;
			c.gridwidth = 2;
		}else{
			c.gridheight = 2;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.fill = GridBagConstraints.VERTICAL;
			c.anchor = GridBagConstraints.NORTHEAST;
		}
		this.add(pnlButtons, c);
		//c.gridx = 3;
		//c.anchor = GridBagConstraints.EAST;
		//this.add(pnlFlags, c);

		if( !   legacy ){
			c.gridheight = 1;
			c.gridx = 0;
			c.gridy++;
			c.gridwidth = GridBagConstraints.REMAINDER;
			this.add(Box.createHorizontalStrut(2), c);

		}
		c.weighty = 1;
		c.gridx = 0;
		c.gridy++;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add(pain, c);

		c.weighty = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
		this.add(vstrut2, c);

		//this.add(vstrut1);
		//this.add(lblIdentifier);
		//this.add(myTfIdentifier);
		//this.add(lblRepresentation);
		//this.add(pnlSpacer1);
		//this.add(myCboRepresentation);
		//this.add(lblName);
		//this.add(myTfName);
		////this.add(lblType);
		//this.add(pnlSpacer2);
		////this.add(cboType);
		////this.add(lblPolicy);
		////this.add(myComboPolicy);
		//this.add(pnlButtons);
		////this.add(pnlFlags);
		//this.add(pain);
		//this.add(vstrut2);

		myTfIdentifier.setEnabled( flagEditable );
		myTfName.setEnabled( flagEditable );
		myButtonInsertState.setEnabled( flagEditable );

		if( flagEditable ) checkDeleteEnabled();
		else myButtonDeleteState.setEnabled( false );

		//myCboRepresentation.setEnabled( false );//enabled as of 010905
		myCboRepresentation.setEnabled( flagEditable );
		//cboType.setEnabled( false );
		//cbxMandatory.setEnabled( false );
		//cbxRanked.setEnabled( false );

		if( DisplayableFiniteVariable.FLAG_IMMUTABLE )
		{
			myTfIdentifier.setEnabled( false );
			myTfName.setEnabled( false );
			//myComboPolicy.setEnabled( false );
			myCboRepresentation.setEnabled( false );
			//cboType.setEnabled( false );
			//cbxMandatory.setEnabled( false );
			//cbxRanked.setEnabled( false );
			myButtonInsertState.setEnabled( false );
			myButtonDeleteState.setEnabled( false );
		}

		Dimension dimPanel = this.getPreferredSize();
		dimPanel.height = 225;
		this.setPreferredSize( dimPanel );

		this.setBorder( BorderFactory.createEmptyBorder( 0,16,0,16 ) );

		//return this;
	}

	/** @since 101002 */
	public String commitPropertyChanges()
	{
		boolean displayTextMightHaveChanged = false;
		boolean somethingChanged = false;

		NetworkInternalFrame hnInternalFrame = myDFV.getNetworkInternalFrame();
		BeliefNetwork bn = hnInternalFrame.getBeliefNetwork();

		if( myFlagIdentifier )
		{
			String newID = myTfIdentifier.getText();
			if( newID.length() <= 0 ) return "Please enter a valid variable identifier.";
			String oldID = myDFV.getID();
			if( !newID.equals( oldID ) )
			{
				if( bn.forID( newID ) != null ) return "Variable identifier \"" + newID + "\" is not unique.";
				myDFV.setID( newID );
				bn.identifierChanged( oldID, myDFV );
				somethingChanged = displayTextMightHaveChanged = true;
			}
		}

		if( myFlagName )
		{
			myDFV.setLabel( myTfName.getText() );
			somethingChanged = displayTextMightHaveChanged = true;
		}

		if( myFlagPolicy )
		{
			myDFV.setExcludePolicy( (ExcludePolicy) myComboPolicy.getSelectedItem() );
			somethingChanged = true;
		}

		//if( myFlagMAPFlagEdited )
		//{
		//	myDFV.setMAPVariable( myCbMAP.isSelected() );
		//	somethingChanged = true;
		//}

		String stateserrmsg = null;
		if( myFlagStateNames || myFlagStateList )
		{
			bn.getEvidenceController().unobserve( myDFV );
			stateserrmsg = myStatesTableModel.commitNewStateObjects();
			if( stateserrmsg == null )
			{
				somethingChanged = true;
				myDFV.getCPTShell( DSLNodeType.CPT ).ensureNonsingular();
				myDFV.getNodeLabel().killEvidenceDialog();
			}
		}

		if( displayTextMightHaveChanged )
		{
			myDFV.changeDisplayText();
			myDFV.getNodeLabel().setText( toString() );
			somethingChanged = true;
		}

		if( myFlagUncommitedRepresentation ){
			myDFV.setDSLNodeType( myUncommitedRepresentation );
			somethingChanged = true;
		}

		if( somethingChanged ){
			hnInternalFrame.fireNodePropertyChangeEvent( myDFV );
		}

		if( stateserrmsg != null ) return stateserrmsg;
		else if( myFlagStateList ){
			//hnInternalFrame.fireCPTChanged( new CPTChangeEvent( this ) );
			hnInternalFrame.netStructureChanged( new NetStructureEvent( NetStructureEvent.GENERIC, Collections.singleton( myDFV ) ) );
		}

		return null;
	}

	/** @since 101002 */
	protected void setEditFlags( boolean setting )
	{
		myFlagIdentifier = setting;
		myFlagName = setting;
		myFlagPolicy = setting;
		//myFlagProbability = setting;
		myFlagStateNames = setting;
		myFlagStateList = setting;
		myFlagUncommitedRepresentation = setting;
	}

	/** @since 011205 */
	public boolean setRepresentationProperty( DSLNodeType representation ){
		if( (myCboRepresentation == null) || (!myCboRepresentation.isEnabled())) return false;
		myCboRepresentation.setSelectedItem( representation );
		return true;
	}

	/** @since 021405 Valentine's Day! */
	public int doInsertState(){
		if( !isEditable( myDFV ) ) throw new IllegalStateException( "illegal to edit states now" );

		int indexSelectedRow = myJTableStates.getSelectedRow();

		int indexInserted = -1;
		if( indexSelectedRow < 0 ) indexInserted = 0;
		else indexInserted = indexSelectedRow + 1;

		if( myStatesTableModel.addState( indexSelectedRow ) ){
			checkDeleteEnabled();
			myDFV.setProbabilityTabEnabled( false );
			myFlagStateList = true;
			return indexInserted;
		}

		return -99;
	}

	/** @since 021405 Valentine's Day! */
	public void doDeleteState(){
		if( !isEditable( myDFV ) ) throw new IllegalStateException( "illegal to edit states now" );
		if( myStatesTableModel.removeState( myJTableStates.getSelectedRow() ) )
		{
			checkDeleteEnabled();
			myDFV.setProbabilityTabEnabled( false );
			myFlagStateList = true;
		}
	}

	/** @since 021405 Valentine's Day! */
	public JTable getTableStates(){
		return this.myJTableStates;
	}

	/** interface ActionListener
		@since 050802 */
	public void actionPerformed( ActionEvent e ){
		Object source = e.getSource();
		if( source == myTfIdentifier ) myFlagIdentifier = true;
		else if ( source == myTfName ) myFlagName = true;
		else if ( source == myComboPolicy ) myFlagPolicy = true;
		else if( source == myButtonInsertState ) doInsertState();
		else if( source == myButtonDeleteState  ) doDeleteState();
		//else if( source == myCbMAP ) myFlagMAPFlagEdited = true;
		else if( source == myCboRepresentation ) doRepresentation();
	}

	/** @since 010905 */
	private void doRepresentation()
	{
		DSLNodeType selected = (DSLNodeType) myCboRepresentation.getSelectedItem();
		if( myUncommitedRepresentation != selected ){
			CPTShell shell = myDFV.getCPTShell( selected );
			if( shell == null ){
				if( selected == DSLNodeType.DECISIONTREE ){
					DecisionTree dt = DecisionTreeEditPanel.showCreationDialog( myDFV.getNetworkInternalFrame(), myDFV.getCPTShell( myDFV.getDSLNodeType() ) );
					if( dt != null ){
						myDFV.addDecisionTreeTab( new DecisionShell( dt ), true );
						myUncommitedRepresentation = selected;
						myFlagUncommitedRepresentation = true;
					}
					else myCboRepresentation.setSelectedItem( myUncommitedRepresentation );
					return;
				}
				else{
					JOptionPane.showMessageDialog( myDFV.getNetworkInternalFrame(), "Error: Don't know what to do for type "+selected+".", "Don't know how", JOptionPane.ERROR_MESSAGE );
					myCboRepresentation.setSelectedItem( myUncommitedRepresentation );
					return;
				}
			}
			else myFlagUncommitedRepresentation = true;
		}
	}

	/** @since 051602 */
	public void stopStatesEditing(){
		if( myJTableStates.getEditingColumn() != -1 )
		{
			TableCellEditor editor = myJTableStates.getCellEditor( myJTableStates.getEditingRow(), myJTableStates.getEditingColumn() );
			if( editor != null ) editor.stopCellEditing();
		}
	}

	/** @since 051002 */
	private void setupButton( JButton btn ){
		btn.addActionListener( (ActionListener)this );
		btn.setMargin( new Insets( 0,8,0,8 ) );
		Font fntButton = btn.getFont();
		btn.setFont( fntButton.deriveFont( (float)10 ) );
	}

	/** @since 101502 */
	public void checkDeleteEnabled(){
		myButtonDeleteState.setEnabled( myStatesTableModel.getRowCount() > 2 );
	}

	/** interface CellEditorListener
		@since 101002 */
	public void editingStopped( ChangeEvent e ){
		Object source = e.getSource();
		//if( source == myTempCellEditorProbabilityTable ) myFlagProbability = true;
		//else
		if( source == myTempCellEditorStatesTable )
		{
			myDFV.setProbabilityTabEnabled( false );
			myFlagStateNames = true;
		}
		else System.err.println( "Warning: DisplayableFiniteVariableImpl.editingStopped() - unrecognized source" );
	}

	/** interface CellEditorListener
		@since 101002 */
	public void editingCanceled( ChangeEvent e ){
		//System.out.println( "NodePropertiesPanel.editingCanceled()" );
	}

	/** @since 030102 */
	private JTable makeStatesTable( boolean editable )
	{
		myStatesTableModel = new StatesTableModel( myDFV, editable );
		myJTableStates = new JTable( myStatesTableModel );
		try{
			Insets editInsets = UIManager.getInsets( "FormattedTextField.contentMargins" );
			if(    editInsets != null ){ myJTableStates.setRowHeight( myJTableStates.getRowHeight() + editInsets.top + editInsets.bottom ); }
		}catch( Throwable thrown ){
			System.err.println( "warning: NodePropertiesPanel.makeStatesTable() caught " + thrown );
		}

		TableColumnModel TCM = myJTableStates.getColumnModel();
		TableColumn colName = TCM.getColumn( StatesTableModel.COL_NAME );
		TableColumn colTarget = TCM.getColumn( StatesTableModel.COL_TARGET );
		TableColumn colDefault = TCM.getColumn( StatesTableModel.COL_DEFAULT );

		TableCellEditor statenameeditor = new DefaultCellEditor( new IdentifierField( /*notify*/false ) );
		myJTableStates.setDefaultEditor( Object.class, statenameeditor );
		//myTempCellEditorStatesTable = colName.getCellEditor();
		//myTempCellEditorStatesTable = myJTableStates.getDefaultEditor( Object.class );
		myTempCellEditorStatesTable = statenameeditor;
		myTempCellEditorStatesTable.addCellEditorListener( this );

		TableCellRenderer defaultRenderer = myJTableStates.getDefaultRenderer( Object.class );
		colName.setCellRenderer( new FocusOverrideDecorator( defaultRenderer, false ) );
		if( true )//DisplayableFiniteVariable.FLAG_IMMUTABLE )
		{
			//colTarget.setCellRenderer( new DisableDecorator( colTarget.getCellRenderer(), false ) );
			colTarget.setCellRenderer( new DisableDecorator( new BooleanRenderer(), false ) );
		}
		//colTarget.setCellRenderer( new BooleanRenderer() );
		//colDefault.setCellRenderer( new RadioRenderer() );
		colDefault.setCellRenderer( new CompRenderer() );
		colDefault.setCellEditor( new CompEditor() );

		int nw = HuginGenieStyleTableFactory.getPreferredWidthForColumn( colName, myJTableStates ) + INT_HORIZONTAL_CELLPADDING;
		colName.setMinWidth( nw );
		int tw = HuginGenieStyleTableFactory.getPreferredWidthForColumn( colTarget, myJTableStates ) + INT_HORIZONTAL_CELLPADDING;
		colTarget.setMinWidth( tw );
		colTarget.setMaxWidth( tw );
		int dw = HuginGenieStyleTableFactory.getPreferredWidthForColumn( colDefault, myJTableStates ) + INT_HORIZONTAL_CELLPADDING;
		colDefault.setMinWidth( dw );
		colDefault.setMaxWidth( dw );

		//myJTableStates.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		try{
			myJTableStates.getTableHeader().setResizingAllowed( false );
		}catch( Throwable thrown ){
			System.err.println( "warning: NodePropertiesPanel.makeStatesTable() caught " + thrown );
		}

		return myJTableStates;
	}

	/** @since 022504 */
	public static class FocusOverrideDecorator implements TableCellRenderer{
		private TableCellRenderer myTableCellRenderer;
		private boolean myFlagHasFocus = false;

		public FocusOverrideDecorator( TableCellRenderer sub, boolean hasFocus ){
			myTableCellRenderer = sub;
			myFlagHasFocus = hasFocus;
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Component ret = myTableCellRenderer.getTableCellRendererComponent(table,value,isSelected,myFlagHasFocus,row,column);
			return ret;
		}
	}

	/** @since 050602 */
	public static class DisableDecorator implements TableCellRenderer{
		private TableCellRenderer myTableCellRenderer = null;
		private boolean myEnabled = false;

		public DisableDecorator( TableCellRenderer sub, boolean enabled ){
			myTableCellRenderer = sub;
			myEnabled = enabled;
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Component ret = myTableCellRenderer.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
			ret.setEnabled( myEnabled );
			return ret;
		}
	}

	/** @since 030102 */
	public static class BooleanRenderer implements TableCellRenderer{
		public BooleanRenderer(){}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			JCheckBox cbxValue = new JCheckBox();
			cbxValue.setSelected( ((Boolean)value).booleanValue() );
			//JPanel pnl = new JPanel();
			//FlowLayout FL = new FlowLayout();
			//FL.setHgap( 0 );
			//FL.setVgap( 0 );
			//pnl.setLayout( FL );
			//pnl.getInsets( new Insets( 0,0,0,0 ) );
			//pnl.add( cbxValue );
			//return pnl;
			//cbxValue.setAlignmentX( Component.CENTER_ALIGNMENT );

			return cbxValue;
		}
	}

	/** @since 030402 */
	public static class CompRenderer implements TableCellRenderer{
		public CompRenderer(){}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
			return (Component)value;
		}
	}

	/** @since 030402 */
	public static class CompEditor extends edu.ucla.belief.ui.internalframes.AbstractCellEditor{
		public CompEditor(){}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column){
			return (Component)value;
		}
	}

	private DisplayableFiniteVariableImpl myDFV;

	public static final int INT_HORIZONTAL_CELLPADDING = 4;

	transient protected JButton myButtonInsertState;
	transient protected JButton myButtonDeleteState;

	transient protected JTextField myTfIdentifier;
	transient protected JTextField myTfName;
	transient protected JComboBox myComboPolicy;
	//transient protected CellEditor myTempCellEditorProbabilityTable = null;
	transient protected StatesTableModel myStatesTableModel;
	transient protected JTable myJTableStates;
	transient protected CellEditor myTempCellEditorStatesTable;
	transient protected JCheckBox myCbMAP;

	transient private JComboBox myCboRepresentation;
	transient private DSLNodeType myUncommitedRepresentation;
	transient boolean myFlagUncommitedRepresentation = false;

	transient protected JPanel pnlButtons;

	transient protected boolean myFlagStateNames = false;
	transient protected boolean myFlagStateList = false;
	transient protected boolean myFlagIdentifier = false;
	transient protected boolean myFlagName = false;
	transient protected boolean myFlagPolicy = false;
}
