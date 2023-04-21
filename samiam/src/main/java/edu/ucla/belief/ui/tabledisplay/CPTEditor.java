package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariableImpl;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.Table;
import edu.ucla.belief.io.dsl.DSLNodeType;
import edu.ucla.belief.ui.dialogs.CPTImportWizard;
import edu.ucla.belief.io.CPTInfo;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

/** code removed from DisplayableFiniteVariableImpl

	@author keith cascio
	@since  20030613 */
public class CPTEditor extends ArrayStyleEditor
{
	public static boolean FLAG_ENABLE_CPT_IMPORT = false;

	public CPTEditor( DisplayableFiniteVariableImpl var, NetworkInternalFrame hnInternalFrame )
	{
		super( var );
		this.hnInternalFrame = hnInternalFrame;
		DSLNodeType type = myDisplayableFiniteVariable.getDSLNodeType();
		if( !(type == DSLNodeType.CPT || type == DSLNodeType.TRUTHTABLE) ) throw new IllegalArgumentException( "CPTEditor requires CPT or TRUTHTABLE" );
		//this.myHuginGenieStyleTableFactory = new HuginGenieStyleTableFactory( hnInternalFrame.getParentFrame() );
	}

	/** @since 20050225 */
	protected Action[] getExtraActions(){
		if( !FLAG_ENABLE_CPT_IMPORT ) return (Action[])null;

		if( myExtraActions == null ){
			myExtraActions = new Action[1];
			myExtraActions[0] = getActionImport();
		}
		return myExtraActions;
	}

	/** @since 20050225 */
	public Action getActionImport(){
		if( myActionImport == null ){
			myActionImport = new SamiamAction( "Import", "Import cpt data from tab-delimited file", 'i', (Icon)null ){
				public void actionPerformed( ActionEvent event ){
					CPTEditor.this.doImport();
				}
			};
		}
		return myActionImport;
	}

	/** @since 20050310 */
	public void doImport(){
		try{
			CPTInfo info = CPTEditor.this.getCPTImportWizard().showDialog( (Component)myTempJTable, myDisplayableFiniteVariable, (CPTInfo.ReadableWritableTable)CPTEditor.this );
			//if( myTempJTable != null ) myTempJTable.
			if( (info == null) || (!info.isCommitted()) ) return;
			if( myTempWrapper == null ) return;

			if( myImportStainer == null ){
				myImportStainer = new ImportedParameterStainer( info, myTempWrapper.model );
				if( myActionListener == null ){
					myActionListener = new ActionListener(){
						public void actionPerformed( ActionEvent event ){
							CPTEditor.this.myTempWrapper.table.repaint();
						}
					};
				}
				myImportStainer.addListener( myActionListener );
			}
			else myImportStainer.setCPTInfo( info );

			DataHandler[] handlers = myTempWrapper.model.getDataHandlers();
			if( handlers != null ){
				for( int i=0; i<handlers.length; i++ ) handlers[i].setStainWright( myImportStainer );
			}

			JComponent newSwitches = myImportStainer.getSwitches();
			if( mySwitches != newSwitches ){
				if( mySwitches != null ) myPanelBorderLayout.remove( mySwitches );
				else{
					Window window = SwingUtilities.getWindowAncestor( myPanelBorderLayout );
					if( window != null ){
						Dimension sizeWindow = window.getSize();
						sizeWindow.height += newSwitches.getPreferredSize().height;
						window.setSize( sizeWindow );
					}
				}
				myPanelBorderLayout.add( newSwitches, BorderLayout.SOUTH );
			}
			mySwitches = newSwitches;

			myTempWrapper.model.fireTableDataChanged();
		}catch( Throwable throwable ){
			System.err.println( throwable );
		}
	}

	/** @since 20050225 */
	public CPTImportWizard getCPTImportWizard(){
		if( myCPTImportWizard == null ){
			myCPTImportWizard = new CPTImportWizard();
		}
		return myCPTImportWizard;
	}

	public String commitProbabilityChanges()
	{
		String errmsg = null;
		if( myWeights != null )//CPT or TRUTHTABLE - UI.GENIE_STYLE_CPT_EDIT
		{
			errmsg = HuginGenieStyleTableFactory.validate( myDisplayableFiniteVariable, myWeights );
			if( errmsg == null )
			{
				Table tempTable = myDisplayableFiniteVariable.getCPTShell( DSLNodeType.CPT ).getCPT();
				if( tempTable.getCPLength() != myWeights.length ) return "Java warning data length != new values length.";
				else
				{
					for( int i=0; i<tempTable.getCPLength(); i++ )
					{
						tempTable.setCP( i, myWeights[i] );
					}
					if( hnInternalFrame == null ) System.err.println( "Warning: CPTEditor.commitProbabilityChanges() called but hnInternalFrame == null" );
					else hnInternalFrame.setCPT( myDisplayableFiniteVariable );
					return null;
				}
			}
		}

		return errmsg;
	}
	public boolean discardChanges()
	{
		boolean flagSuper = super.discardChanges();
		myTempWrapper = null;
	  //myTempCellEditorProbabilityTable = null;
		return flagSuper;
	}

	public JComponent makeProbabilityEditComponent()
	{
		if( edu.ucla.belief.ui.UI.FLAG_GENIE_STYLE_CPT_EDIT )
		{
			myWeights = (double[]) myDisplayableFiniteVariable.getCPTShell( DSLNodeType.CPT ).getCPT().dataclone();
			boolean[] excludeArray = myDisplayableFiniteVariable.getExcludeArray();
			if( excludeArray != null ) myExclude = (boolean[]) excludeArray.clone();
			JComponent ret = makeCPTEditComponent( (DataHandler)null, /*flagAddButtons*/true );
			return ret;
		}
		else
		{
			//myCPTJTable = new DisplayableTable(hnInternalFrame, getCPTShell( DSLNodeType.CPT ).getCPT(),"Probability", true);
			//JScrollPane ret = new JScrollPane( myCPTJTable );
			//myTempJTable = myCPTJTable;
			//return ret;
			return null;
		}
	}

	/** @since 20050113 */
	public JComponent makeCPTDisplayComponent( DataHandler handler ){
		return makeCPTEditComponent( handler, /*flagAddButtons*/false );
	}

	/** @since 20050113 */
	public void resizeJTable(){
		if( myTempJTable != null ) TableStyleEditor.resize( myTempJTable );
	}

	/** @since 20050113 */
	private HuginGenieStyleTableFactory.HuginGenieStyleJComponentWrapper makeWrapper( DataHandler handler )
	{
		boolean flagEditable = this.isEditable();
		if( hnInternalFrame != null ) flagEditable &= !hnInternalFrame.getSamiamUserMode().contains( SamiamUserMode.READONLY );
		myTempWrapper =
			myHuginGenieStyleTableFactory.makeCPTJComponent( myDisplayableFiniteVariable,
									myDisplayableFiniteVariable.getCPTShell( DSLNodeType.CPT ).index().getParents(),
									myWeights,
									myExclude,
									flagEditable,
									handler );//hnInternalFrame.getSamiamUserMode().contains( SamiamUserMode.EDIT ) );
		myTempJTable = myTempWrapper.table;
	  //myTempCellEditorProbabilityTable = myTempJTable.getDefaultEditor( Object.class );
	  //myTempCellEditorProbabilityTable.addCellEditorListener( myDisplayableFiniteVariable );
	  //myTempCellEditorProbabilityTable.addCellEditorListener( this );
	  //myTempCellEditorProbabilityTable = ((ProbabilityJTableEditor)myTempCellEditorProbabilityTable).getEventSource();

	  //myTempWrapper.model.addCellEditorListener( this );
		return myTempWrapper;
	}

	/** @since 20020522 */
	private JComponent makeCPTEditComponent( DataHandler handler, boolean flagAddButtons )
	{
		makeWrapper( handler );
		myPanelBorderLayout = new JPanel( new BorderLayout() );

		if( flagAddButtons ){
			JPanel pnlButtons = makeButtonPanel( "Conditional Probability Table" );
			myPanelBorderLayout.add( pnlButtons, BorderLayout.NORTH );
		}
		myPanelTable = new JPanel( new BorderLayout() );
		myPanelTable.add( myTempWrapper.component, BorderLayout.CENTER );
		if( myTempJTable.getModel() instanceof PortedTableModel ) myPanelTable.add( makeScrollComponent( (PortedTableModel) myTempJTable.getModel() ), BorderLayout.SOUTH );

		myPanelBorderLayout.add( myPanelTable, BorderLayout.CENTER );

		/** this won't work here */
	  //TableStyleEditor.resize( myTempJTable );

		return myPanelBorderLayout;
	}

  //public CellEditor getCellEditor()
  //{
  //	return myTempCellEditorProbabilityTable;
  //}

	transient private   HuginGenieStyleTableFactory myHuginGenieStyleTableFactory = new HuginGenieStyleTableFactory();
	transient protected NetworkInternalFrame        hnInternalFrame;
	transient private   Action[]                    myExtraActions;
	transient private   Action                      myActionImport;
	transient private   CPTImportWizard             myCPTImportWizard;
	transient private   JComponent                  myPanelBorderLayout, myPanelTable, mySwitches;
	transient private   ImportedParameterStainer    myImportStainer;
	transient private   ActionListener              myActionListener;
}
