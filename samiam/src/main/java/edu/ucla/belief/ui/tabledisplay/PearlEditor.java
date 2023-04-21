package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariableImpl;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.NoisyOrShellPearl;
import edu.ucla.belief.Table;
import edu.ucla.belief.io.dsl.DSLNodeType;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.BorderLayout;

/**
	Code removed from DisplayableFiniteVariableImpl

	@author Keith Cascio
	@since 061303
*/
public class PearlEditor extends ArrayStyleEditor
{
	public PearlEditor( DisplayableFiniteVariableImpl var, NetworkInternalFrame hnInternalFrame )
	{
		super( var );
		this.hnInternalFrame = hnInternalFrame;
		if( myDisplayableFiniteVariable.getDSLNodeType() != DSLNodeType.NOISY_OR ) throw new IllegalArgumentException( "PearlEditor requires NOISY_OR" );
		//this.myHuginGenieStyleTableFactory = new HuginGenieStyleTableFactory( hnInternalFrame.getParentFrame() );
	}

	public String commitProbabilityChanges()
	{
		String errmsg = null;
		if( myWeights != null )//NOISY_MAX
		{
			errmsg = HuginGenieStyleTableFactory.validate( myDisplayableFiniteVariable, myWeights );
			if( errmsg == null )
			{
				NoisyOrShellPearl shell = (NoisyOrShellPearl) myDisplayableFiniteVariable.getCPTShell( DSLNodeType.NOISY_OR );

				try{
					shell.setWeights( myWeights );
					hnInternalFrame.setCPT( myDisplayableFiniteVariable );
				}catch( Exception e ){
					errmsg = e.getMessage();
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
		NoisyOrShellPearl shell = (NoisyOrShellPearl) myDisplayableFiniteVariable.getCPTShell( DSLNodeType.NOISY_OR );
		myWeights = shell.weightsClone();
		int[][] strengths = shell.strengths();
		JComponent ret = makePearlEditComponent( myWeights, strengths );
		return ret;
	}

	/**
		@author Keith Cascio
		@since 061103
	*/
	private JComponent makePearlEditComponent( double[] weights, int[][] strengths )
	{
		myWeights = weights;

		HuginGenieStyleTableFactory.HuginGenieStyleJComponentWrapper rappa =
			 myHuginGenieStyleTableFactory.makePearlJComponent(	myDisplayableFiniteVariable.getCPTShell( DSLNodeType.NOISY_OR ).index().getParents(),
										myDisplayableFiniteVariable.instances(),
										myWeights,
										strengths,
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
		//myTempCellEditorProbabilityTable = myTempJTable.getDefaultEditor( Object.class );
		//myTempCellEditorProbabilityTable.addCellEditorListener( myDisplayableFiniteVariable );
		//myTempCellEditorProbabilityTable = ((ProbabilityJTableEditor)myTempCellEditorProbabilityTable).getEventSource();

		//myTempWrapper.model.addCellEditorListener( this );

		JPanel ret = new JPanel();
		ret.setLayout( new BorderLayout() );

		JPanel pnlButtons = makeButtonPanel( "Noisy Or Weights" );

		ret.add( pnlButtons, BorderLayout.NORTH );
		ret.add( myTempWrapper.component, BorderLayout.CENTER );
		if( myTempJTable.getModel() instanceof PortedTableModel ) ret.add( makeScrollComponent( (PortedTableModel) myTempJTable.getModel() ), BorderLayout.SOUTH );

		return ret;
	}

	//public CellEditor getCellEditor()
	//{
	//	return myTempCellEditorProbabilityTable;
	//}

	private HuginGenieStyleTableFactory myHuginGenieStyleTableFactory = new HuginGenieStyleTableFactory();
	transient protected NetworkInternalFrame hnInternalFrame;
}
