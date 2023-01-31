package edu.ucla.belief;

import java.util.*;

/**
	@author Keith Cascio
	@since 100702
*/
public class TableShell implements CPTShell
{
	public TableShell( Table t )
	{
		myTable = t;
		cptParameters = new CPTParameter[t.dataclone().length];
	}

	/** @since 050904 */
	public void setValues( double[] valuesToSet ){
		myTable.setValues( valuesToSet );
	}

	public Object clone()
	{
		//System.out.println( "(TableShell)"+myTable.index().getJoint().getID()+".clone()" );
		return new TableShell( (Table) myTable.clone() );
	}

	/** @since 021704 */
	public int randomJointValueIndex( Map mapInstantions ){
		return myTable.random( mapInstantions );
	}

	public FiniteVariable getVariable()
	{
		List vars = variables();
		return (FiniteVariable)vars.get(vars.size()-1);
	}

	public List variables()
	{
		return myTable.variables();
	}

	/** @since 20080220 */
	public void replaceVariables( Map old2new, boolean partial )
	{
		myTable.replaceVariables( old2new, partial );
		cptParameters = new CPTParameter[myTable.dataclone().length];
	}

//	public void forgetNoScale( Variable from )
//	{
//		myTable = myTable.forget( Collections.singleton(from) );
////		myTable.normalizeSpecial();
//	}

	public void forget( Variable from )
	{
		myTable = myTable.forget( from );
		cptParameters = new CPTParameter[myTable.dataclone().length];
	}

	public void expand( FiniteVariable var )
	{
		myTable = myTable.expand( var );
		cptParameters = new CPTParameter[myTable.dataclone().length];
	}

	public void multiplyInto( Table t2) {
		myTable.multiplyInto( t2);
		cptParameters = new CPTParameter[myTable.dataclone().length];
	}

	public void ensureNonsingular()
	{
		myTable.ensureNonsingular();
	}

	public void normalize() throws Exception
	{
		myTable.normalize();
	}

	public static boolean FLAG_DEBUG_INSERT_STATE = false;

	/**
		@author Keith Cascio
		@since 101502
	*/
	public void insertState( int index )
	{
		myTable.insertState( index);
		cptParameters = new CPTParameter[myTable.dataclone().length];
	}

	/**
		@author Keith Cascio
		@since 101502
	*/
	public void removeState( int index )
	{
		myTable.removeState( index);
		cptParameters = new CPTParameter[myTable.dataclone().length];
	}

	/**
		@author Keith Cascio
		@since 101502
	*/
	public void parentStateInserted( FiniteVariable parent, int indexNewInstance )
	{
		myTable.parentStateInserted( parent, indexNewInstance);
		cptParameters = new CPTParameter[myTable.dataclone().length];
	}

	/**
		@author Keith Cascio
		@since 101502
	*/
	public void parentStateRemoved( FiniteVariable parent, int indexRemovedInstance )
	{
		myTable.parentStateRemoved( parent, indexRemovedInstance);
		cptParameters = new CPTParameter[myTable.dataclone().length];
	}

	public TableIndex index()
	{
		return myTable.index();
	}

	public Table getCPT()
	{
		return myTable;
	}

	/**
		@author Keith Cascio
		@since 041403
	*/
	public double scalar() { return myTable.scalar(); }
	public double getCPScaled( final int ind ) { return myTable.getCPScaled(ind); }
	public double getCPScaled( final int[] indices) { return myTable.getCPScaled(indices); }

	public double getCP( int index )
	{
		return myTable.getCP( index);
	}

	public double getCP( final int[] indices )
	{
		return myTable.value( indices );
	}

	public double getCP( final Object[] instantiations )
	{
		return myTable.value( instantiations );
	}

	public double getCP( final Map instantiations )
	{
		TableIndex ind = myTable.index();
		int size = ind.getNumVariables();
		Object[] instArray = new Object[ size ];
		FiniteVariable fVar = null;
		for( int i=0; i<size; i++ )
		{
			fVar = ind.variable(i);
			instArray[i] = instantiations.get( fVar );
		}
		return myTable.value( instArray );
	}

	public double getCP( final Object[] parentInstantiations, final Object childInstatiation )
	{
		TableIndex ind = myTable.index();
		Object[] instArray = new Object[ ind.getNumVariables() ];
		instArray[ instArray.length - 1 ] = childInstatiation;
		int counter = 0;
		FiniteVariable fVar = null;
		int variableIndex = -1;
		while( counter < parentInstantiations.length )
		{
			fVar = (FiniteVariable) parentInstantiations[counter++];
			variableIndex = ind.variableIndex( fVar );
			if( variableIndex >= 0 )
			{
				instArray[variableIndex] = parentInstantiations[counter];
			}
			++counter;
		}
		return myTable.value( instArray );
	}

	public java.util.Set getRelevant( final int[] indices )
	{
		return Collections.EMPTY_SET;
	}

	public java.util.Set getRelevant( final Object[] instantiations )
	{
		return Collections.EMPTY_SET;
	}

	public java.util.Set getRelevant( final Map instantiations )
	{
		return Collections.EMPTY_SET;
	}

	public java.util.Set getRelevant( final Object[] parentInstantiations, final Object childInstatiation )
	{
		return Collections.EMPTY_SET;
	}

	protected Table myTable;
	protected CPTParameter[] cptParameters;

	public ArrayList   valueElimination(){
		return myTable.valueElimination();
	}

	public CPTParameter[] getCPTParameters() {
		for (int i = 0; i < cptParameters.length; i++)
			cptParameters[i] = getCPTParameter(i);
		return cptParameters;
	}

	public CPTParameter getCPTParameter(int index) {
		if (cptParameters[index] == null)
			cptParameters[index] = new CPTParameter(this,
				myTable.index.mindex(index, null));
		return cptParameters[index];
	}

	public CPTParameter getCPTParameter(int[] mindex) {
		int index = myTable.index().index(mindex);
		if (cptParameters[index] == null)
			cptParameters[index] = new CPTParameter(this,
				mindex);
		return cptParameters[index];
	}
}
