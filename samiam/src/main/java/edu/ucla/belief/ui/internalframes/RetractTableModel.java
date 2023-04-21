package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;

import edu.ucla.belief.*;
import javax.swing.table.*;
import java.util.*;

public class RetractTableModel extends DefaultTableModel
{
	private NetworkInternalFrame hnInternalFrame;

	public RetractTableModel(NetworkInternalFrame hnInternalFrame)
	{
		this.hnInternalFrame = hnInternalFrame;
		InferenceEngine ie = hnInternalFrame.getInferenceEngine();
		PartialDerivativeEngine pde = hnInternalFrame.getPartialDerivativeEngine();

		//Map evidence = ie == null ? new Hashtable() :
		//	ie.evidence();
		Map evidence = hnInternalFrame.getBeliefNetwork().getEvidenceController().evidence();

		Object[] keys = evidence.keySet().toArray();
		FiniteVariable[] evidenceVars = new FiniteVariable[keys.length];
		VariableInstance[] evidenceInstances = new
		VariableInstance[evidenceVars.length];
		for (int i = 0; i < evidenceVars.length; i++)
		{
			evidenceVars[i] = (DisplayableFiniteVariable)keys[i];
			evidenceInstances[i] = new VariableInstance( (DisplayableFiniteVariable)evidenceVars[i], evidence.get(evidenceVars[i]));
		}

		double[] values = new double[evidenceVars.length];
		for( int i = 0; i < evidenceVars.length; i++ )
		{
			/* Should use retractedConditional(var)
			Table retractedConditional =
			ie.retractedConditional(evidenceVars[i]);
			values[i] = retractedConditional.data()
			[evidenceInstances[i].getIndex()];
			*/
			//Table retractedJoint = ie.retractedJoint( evidenceVars[i] );
			Table retractedJoint = pde.partial( evidenceVars[i] );
			double sum = 0.0;
			for (int j = 0; j < evidenceVars[i].size(); j++)
			{
				sum += retractedJoint.getCP(j);
			}
			values[i] = retractedJoint.getCP(evidenceInstances[i].getIndex()) / sum;
		}

		Object[][] rowData = new Object[1][evidenceVars.length + 1];
		rowData[0][0] = "Pr(X = x | e - x)";
		for (int i = 0; i < evidenceVars.length; i++)
		rowData[0][i + 1] = new Double(values[i]);

		Object[] columnNames = new Object[evidenceVars.length + 1];
		columnNames[0] = "Retract evidence X = x";
		for (int i = 0; i < evidenceVars.length; i++)
		columnNames[i + 1] = evidenceInstances[i];

		setDataVector(rowData, columnNames);
	}

	public RetractTableModel( NetworkInternalFrame hnInternalFrame,VariableInstance[] varInstances )
	{
		this.hnInternalFrame = hnInternalFrame;
		InferenceEngine ie = hnInternalFrame.getInferenceEngine();

		//Map evidence = ie == null ? new Hashtable() :
		//	ie.evidence();
		Map evidence = hnInternalFrame.getBeliefNetwork().getEvidenceController().evidence();

		Object[] keys = evidence.keySet().toArray();
		FiniteVariable[] evidenceVars = new
		FiniteVariable[keys.length];
		VariableInstance[] evidenceInstances = new VariableInstance[keys.length];
		for (int i = 0; i < keys.length; i++)
		{
			evidenceVars[i] = (DisplayableFiniteVariable)keys[i];
			evidenceInstances[i] = new VariableInstance( (DisplayableFiniteVariable)evidenceVars[i], evidence.get(evidenceVars[i]));
		}

		double[][] values = new double[evidenceVars.length][varInstances.length];

		for (int i = 0; i < varInstances.length; i++)
			for (int j = 0; j < evidenceVars.length; j++)
				values[j][i] = retract(varInstances[i],evidenceVars[j]);

		double[] conditionals = new double[varInstances.length];
		for (int i = 0; i < varInstances.length; i++)
		conditionals[i] = ie.conditional(varInstances[i].getVariable()).getCP(varInstances[i].getIndex());

		Object[][] rowData = new Object[evidenceVars.length + 1]
		[varInstances.length + 1];

		rowData[0][0] = "No retraction";
		for (int i = 0; i < varInstances.length; i++)
		rowData[0][i + 1] = new Double(conditionals[i]);

		for (int i = 0; i < evidenceVars.length; i++)
		{
			rowData[i + 1][0] = evidenceInstances[i];
			for (int j = 0; j < varInstances.length; j++)
			rowData[i + 1][j + 1] = new
			Double(values[i][j]);
		}

		Object[] columnNames = new Object[varInstances.length +
			1];
		columnNames[0] = "Retract evidence X = x";
		for (int i = 0; i < varInstances.length; i++)
		columnNames[i + 1] = varInstances[i];

		setDataVector(rowData, columnNames);
	}

	public boolean isCellEditable(int row, int column)
	{
		return false;
	}

	public Class getColumnClass(int columnIndex)
	{
		if (columnIndex == 0)
		return new Object().getClass();
		return new Double(0).getClass();
	}

	public double retract( VariableInstance varInstance, FiniteVariable evidenceVar )
	{
		InferenceEngine ie = hnInternalFrame.getInferenceEngine();
		PartialDerivativeEngine pde = hnInternalFrame.getPartialDerivativeEngine();
		//java.util.Map evidence = ie.evidence();

		EvidenceController EC = hnInternalFrame.getBeliefNetwork().getEvidenceController();
		Map evidence = EC.evidence();

		//Turn off evidence change events
		//EC.setNotifyEnabled( false );

		FiniteVariable var = varInstance.getVariable();
		Object instance = varInstance.getInstance();
		int index = varInstance.getIndex();
		Object varEvidence = evidence.get(var);
		if (varEvidence != null)
		{
			// Should use retractedConditional(var)
			//Table retractedTable = ie.retractedJoint(var);
			Table retractedTable = pde.partial( var );

			double sum = 0.0;
			for (int i = 0; i < var.size(); i++)
				sum += retractedTable.getCP(i);

			if (var != evidenceVar)
				return instance == varEvidence ? 1.0 : 0.0;
			else
				return retractedTable.getCP(index) / sum;
		}

		//Table retractedJoint = ie.retractedJoint(evidenceVar);
		Table retractedJoint = pde.partial( evidenceVar );
		double denominator = 0.0;
		for (int i = 0; i < evidenceVar.size(); i++)
			denominator += retractedJoint.getCP(i);

		double numerator = 0.0;
		try{
			EC.observeNotifyOnlyPriorityListeners(var, instance);

			//retractedJoint = ie.retractedJoint(evidenceVar);
			retractedJoint = pde.partial( evidenceVar );
			for (int i = 0; i < evidenceVar.size(); i++)
				numerator += retractedJoint.getCP(i);

			//Reset the real evidence
			if (varEvidence == null) EC.unobserveNotifyOnlyPriorityListeners(var);
			else EC.observeNotifyOnlyPriorityListeners(var, varEvidence);
		}catch( StateNotFoundException e ){
			System.err.println( "RetractTableModel.retract() caught " + e );
			if( Util.DEBUG_VERBOSE )
			{
				System.err.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
		}

		//Turn evidence change events back on again
		//EC.setNotifyEnabled( true );

		return numerator / denominator;
	}
}
