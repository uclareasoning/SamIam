package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariableImpl;
import javax.swing.*;

/**
	Code removed from DisplayableFiniteVariableImpl

	@author Keith Cascio
	@since 061303
*/
public abstract class ArrayStyleEditor extends TableStyleEditor implements edu.ucla.belief.io.CPTInfo.ReadableWritableTable
{
	public ArrayStyleEditor( DisplayableFiniteVariableImpl var )
	{
		myDisplayableFiniteVariable = var;
	}

	/** interface ReadableWritableTable
		@since 022805 */
	public int getCPLength(){
		if( myWeights == null ) return (int)-1;
		return myWeights.length;
	}

	/** interface ReadableWritableTable
		@since 022805 */
	public double getCP( int ind ){
		if( myWeights == null ) return (double)-1;
		return myWeights[ind];
	}

	/** interface ReadableWritableTable
		@since 022805 */
	public void setCP( int indx, double val ){
		if( myWeights == null ) return;
		myWeights[indx] = val;
		if( myTempWrapper != null ) myTempWrapper.model.setProbabilityEdited();
	}

	/** interface ProbabilityEditor */
	public String commitExcludeDataChanges()
	{
		String errmsg = null;
		HuginGenieStyleTableFactory.TableModelHGS modelhgs = myTempWrapper.model;
		if( modelhgs instanceof PortedTableModelHGS ){
			modelhgs = ((PortedTableModelHGS)modelhgs).getUnderlyingHGS();
		}
		if( (myExclude == null) && (modelhgs instanceof HuginGenieStyleTableModel) )
		{
			myExclude = ((HuginGenieStyleTableModel) modelhgs).getExcludeData();
		}

		if( myExclude == null ) errmsg = "commitExcludeDataChanges() failed: null exclude array";
		else myDisplayableFiniteVariable.setExcludeArray( myExclude );

		return errmsg;
	}

	/** interface ProbabilityEditor */
	public String normalize(){
		if( myTempWrapper == null ) return "Warning: no wrapper";
		if( myTempJTable == null ) return "Warning: no table";
		if( isProbabilityEditInProgress() ) stopProbabilityEditing();
		if( myWeights == null ) return "Warning: no weights";
		javax.swing.table.TableColumnModel model = myTempJTable.getColumnModel();
		String errmsg = null;
		if( model instanceof GroupableTableColumnModel ){
			errmsg = normalizePorted( (GroupableTableColumnModel)model );
		}
		else errmsg = normalizeUnported();
		myTempWrapper.model.fireTableDataChanged();
		return errmsg;
	}

	/** @since 030105 */
	public String normalizeUnported(){
		//System.out.println( "ArrayStyleEditor.normalizeUnported()" );
		if( myTempJTable == null ) return "Warning: no table";
		if( myWeights == null ) return "Warning: no weights";

		int[] selectedColumns = myTempJTable.getSelectedColumns();
		String errmsg = null;
		int condition = (int)-1;
		for( int i=0; i< selectedColumns.length; i++ )
		{
			//condition = myTempWrapper.model.convertColumnIndexToConditionIndex( selectedColumns[i] );
			condition = selectedColumns[i];
			errmsg = HuginGenieStyleTableFactory.normalize( myDisplayableFiniteVariable, myWeights, condition );
			//if( errmsg == null ) myTempJTable.clearSelection();//obviated by fireTableDataChanged()
			if( errmsg != null ) return errmsg;
		}
		return (String) null;
	}

	/** @since 030105 */
	public String normalizePorted( GroupableTableColumnModel model ){
		//System.out.println( "ArrayStyleEditor.normalizePorted()" );
		if( myTempWrapper == null ) return "Warning: no wrapper";
		if( myWeights == null ) return "Warning: no weights";

		model.recordSelectedState();
		int columnCount = model.getUnderlyingColumnCount();
		String errmsg = null;
		int condition = (int)-1;
		for( int i=0; i<columnCount; i++ ){
			if( model.isSelectedColumn( i ) ){
				condition = myTempWrapper.model.convertColumnIndexToConditionIndex( i );
				errmsg = HuginGenieStyleTableFactory.normalize( myDisplayableFiniteVariable, myWeights, condition );
				if( errmsg != null ) return errmsg;
			}
		}
		return (String) null;
	}

	/** interface ProbabilityEditor */
	public String complement(){
		if( myTempWrapper == null ) return "Warning: no wrapper";
		if( myTempJTable == null ) return "Warning: no table";
		if( isProbabilityEditInProgress() ) stopProbabilityEditing();
		if( myTempJTable.getSelectedColumnCount() != 1 ) return "Please select one probability value.";

		if( myWeights == null ) return "Warning: no weights";

		//System.out.println( "selected column: " + myTempJTable.getSelectedColumn() );
		//int condition = myTempWrapper.model.convertColumnIndexToConditionIndex( myTempJTable.getSelectedColumn() );
		int condition = myTempJTable.getSelectedColumn();
		String errmsg = HuginGenieStyleTableFactory.complement( myDisplayableFiniteVariable, myWeights, condition, myTempJTable.getSelectedRow(), DisplayableFiniteVariableImpl.FLAG_ROUND_COMPLEMENT );
		if( errmsg == null ) myTempWrapper.model.fireTableDataChanged();

		return errmsg;
	}

	/** interface ProbabilityEditor */
	public boolean discardChanges()
	{
		boolean flagSuper = super.discardChanges();
		myWeights = null;
		myExclude = null;
		return flagSuper;
	}

	protected double[] myWeights;
	protected boolean[] myExclude;
}
