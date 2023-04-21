package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.NoisyOrShell;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariableImpl;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariable;
import edu.ucla.belief.io.dsl.DSLNodeType;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.util.List;

/**
	Code removed from DisplayableFiniteVariableImpl

	This new class is born obsolete.

	@author Keith Cascio
	@since 061303
*/
public class NoisyOrListEditor extends TableStyleEditor
{
	public NoisyOrListEditor( DisplayableFiniteVariableImpl var, NetworkInternalFrame hnInternalFrame )
	{
		myDisplayableFiniteVariable = var;
		this.hnInternalFrame = hnInternalFrame;
		if( myDisplayableFiniteVariable.getDSLNodeType() != DSLNodeType.NOISY_OR ) throw new IllegalArgumentException( "NoisyOrListEditor requires NOISY_OR" );
		//this.myHuginGenieStyleTableFactory = new HuginGenieStyleTableFactory( hnInternalFrame.getParentFrame() );
	}

	public String commitExcludeDataChanges()
	{
		return null;
	}

	public String commitProbabilityChanges()
	{
		String errmsg = null;
		if( myTempNoisyOrWeights != null )//NOISY_OR
		{
			errmsg = HuginGenieStyleTableFactory.validate( myDisplayableFiniteVariable, myTempNoisyOrWeights );
			if( errmsg == null )
			{
				NoisyOrShell shell = (NoisyOrShell) myDisplayableFiniteVariable.getCPTShell( DSLNodeType.NOISY_OR );

				try{
					shell.setWeights( myTempNoisyOrWeights );
					hnInternalFrame.setCPT( myDisplayableFiniteVariable );
				}catch( Exception e ){
					errmsg = e.getMessage();
				}
			}
		}
		return errmsg;
	}

	public String normalize()
	{
		if( myTempJTable != null )
		{
			if( isProbabilityEditInProgress() ) stopProbabilityEditing();
			if( myTempNoisyOrWeights != null )
			{
				int[] selectedColumns = myTempJTable.getSelectedColumns();
				for( int i=0; i< selectedColumns.length; i++ )
				{
					String errmsg = HuginGenieStyleTableFactory.normalize( myDisplayableFiniteVariable, myTempNoisyOrWeights,  selectedColumns[i] );
					if( errmsg == null ) myTempJTable.clearSelection();
					else return errmsg;
				}
			}
		}

		return null;
	}

	public String complement()
	{
		if( myTempJTable != null )
		{
			if( isProbabilityEditInProgress() ) stopProbabilityEditing();
			if( myTempJTable.getSelectedColumnCount() != 1 ) return "Please select one probability value.";

			if( myTempNoisyOrWeights != null )
			{
				String errmsg = HuginGenieStyleTableFactory.complement( myDisplayableFiniteVariable, myTempNoisyOrWeights,  myTempJTable.getSelectedColumn(), myTempJTable.getSelectedRow(), DisplayableFiniteVariable.FLAG_ROUND_COMPLEMENT );
				if( errmsg == null ) myTempJTable.clearSelection();
				else return errmsg;
			}
		}

		return null;
	}

	public boolean discardChanges()
	{
		boolean flagSuper = super.discardChanges();
		myTempNoisyOrWeights = null;
		myTempWrapper = null;
		myTempCellEditorProbabilityTable = null;
		return flagSuper;
	}

	public JComponent makeProbabilityEditComponent()
	{
		myTempNoisyOrWeights = ((NoisyOrShell) myDisplayableFiniteVariable.getCPTShell( DSLNodeType.NOISY_OR )).weightsAsList();
		JComponent ret = makeNoisyOrEditComponent( myTempNoisyOrWeights );
		return ret;
	}

	/**
		@author Keith Cascio
		@since 052202
	*/
	private JComponent makeNoisyOrEditComponent( List tempNoisyOrWeights )
	{
		myTempNoisyOrWeights = tempNoisyOrWeights;

		HuginGenieStyleTableFactory.HuginGenieStyleJComponentWrapper rappa =
			 myHuginGenieStyleTableFactory.makeNoisyOrJComponent(	myDisplayableFiniteVariable.getCPTShell( DSLNodeType.NOISY_OR ).index().getParents(),
										myDisplayableFiniteVariable.instances(),
										myTempNoisyOrWeights,
										!hnInternalFrame.getSamiamUserMode().contains( SamiamUserMode.READONLY ) );//hnInternalFrame.getSamiamUserMode().contains( SamiamUserMode.EDIT ) );
		return makeNoisyOrEditComponent( rappa );
	}

	/**
		@author Keith Cascio
		@since 052202
	*/
	private JComponent makeNoisyOrEditComponent( HuginGenieStyleTableFactory.HuginGenieStyleJComponentWrapper rappa )
	{
		myTempWrapper = rappa;

		myTempJTable = myTempWrapper.table;
		myTempCellEditorProbabilityTable = myTempJTable.getDefaultEditor( Object.class );
		//myTempCellEditorProbabilityTable.addCellEditorListener( myDisplayableFiniteVariable );
		myTempCellEditorProbabilityTable = ((ProbabilityJTableEditor)myTempCellEditorProbabilityTable).getEventSource();

		JPanel ret = new JPanel();
		ret.setLayout( new BorderLayout() );

		JPanel pnlButtons = makeButtonPanel( "Noisy Or Weights" );

		ret.add( pnlButtons, BorderLayout.NORTH );
		ret.add( myTempWrapper.component, BorderLayout.CENTER );
		if( myTempJTable.getModel() instanceof PortedTableModel ) ret.add( makeScrollComponent( (PortedTableModel) myTempJTable.getModel() ), BorderLayout.SOUTH );

		return ret;
	}

	public CellEditor getCellEditor()
	{
		return myTempCellEditorProbabilityTable;
	}

	protected List myTempNoisyOrWeights;
	protected DisplayableFiniteVariableImpl myDisplayableFiniteVariable;
	private HuginGenieStyleTableFactory myHuginGenieStyleTableFactory = new HuginGenieStyleTableFactory();
	transient protected HuginGenieStyleTableFactory.HuginGenieStyleJComponentWrapper myTempWrapper;
	transient protected NetworkInternalFrame hnInternalFrame;
	transient protected CellEditor myTempCellEditorProbabilityTable;
}
