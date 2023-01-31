package edu.ucla.belief;

import edu.ucla.belief.decision.*;
import edu.ucla.util.*;

import java.util.*;

/** @author keith cascio
	@since  20050109 */
public class DecisionShell implements CPTShell, DecisionListener
{
	protected TableIndex myTableIndex;
	protected Table myTable;
	protected DecisionTree myDecisionTree;
	protected int[] myUtilIndices;

	public DecisionShell( DecisionTree tree )
	{
		this.myDecisionTree = tree;
		this.myTableIndex = tree.getIndex();
		init();
	}

	protected DecisionShell( DecisionShell toCopy )
	{
		this.myTableIndex = toCopy.myTableIndex;
		this.myTable = toCopy.myTable;
		this.myDecisionTree = toCopy.myDecisionTree;
		init();
	}

	/** interface DecisionListener */
	public void decisionEvent( DecisionEvent e ){
		if( e.type == DecisionEvent.RENAME ){ return; }
		this.myTable = null;
	}

	public DecisionTree getDecisionTree(){
		return this.myDecisionTree;
	}

	private void init(){
		myDecisionTree.addListener( (DecisionListener)this );
		this.myUtilIndices = new int[ myTableIndex.getNumVariables() ];
	}

	public Object clone(){
		throw new UnsupportedOperationException();
		//return new DecisionShell( this );
	}

	public int randomJointValueIndex( Map mapInstantions ){
		throw new UnsupportedOperationException();
	}

	public FiniteVariable getVariable(){
		List vars = variables();
		return (FiniteVariable)vars.get(vars.size()-1);
	}

	public List variables(){
		return myTableIndex.variables();
	}

	public CPTParameter[] getCPTParameters(){
		CPTParameter[] parameters = new CPTParameter[myTable.getCPLength()];
		for( int i = 0; i < parameters.length; i++ )
			parameters[i] = new CPTParameter( this, myTableIndex.mindex( i, myUtilIndices ) );
		return parameters;
	}

	public CPTParameter getCPTParameter( int index ){
		return new CPTParameter( this, myTableIndex.mindex( index, myUtilIndices ) );
	}

	public CPTParameter getCPTParameter( int[] mindex ){
		return new CPTParameter( this, mindex );
	}

	public void ensureNonsingular() {}

	public void normalize() throws Exception {}

	public void replaceVariables( Map old2new, boolean partial )
	{
		throw new UnsupportedOperationException();
		/*
		if( myTable != null )
		{
			myTable.replaceVariables( oldToNew );
			myTableIndex = myTable.index();
		}
		else
		{
			List newVars = new ArrayList( myTableIndex.size() );

			Object newVar = null;
			Object oldVar = null;
			for( Iterator it = variables().iterator(); it.hasNext(); )
			{
				oldVar = it.next();
				newVar = oldToNew.get( oldVar );
				if( newVar == null )
				{
					if( ! partial ){ System.err.println( "Warning attempt to DecisionShell.replaceVariables() with incomplete Map" ); }
					newVar = oldVar;
				}

				newVars.add( newVar );
			}

			myTableIndex = new TableIndex( newVars );
		}*/
	}

	public void multiplyInto( Table t2 ) { throw new UnsupportedOperationException(); }

	public void forget( Variable from ){ throw new UnsupportedOperationException(); }

	public void expand( FiniteVariable var ){ throw new UnsupportedOperationException(); }

	public void insertState( int index ){ throw new UnsupportedOperationException(); }

	public void removeState( int index ){ throw new UnsupportedOperationException(); }

	public void parentStateInserted( FiniteVariable parent, int indexNewInstance ){ throw new UnsupportedOperationException(); }

	public void parentStateRemoved( FiniteVariable parent, int indexRemovedInstance ){ throw new UnsupportedOperationException(); }

	public TableIndex index(){
		return myTableIndex;
	}

	public Table getCPT(){
		if( myTable == null ) myTable = myDecisionTree.expand();
		return myTable;
	}

	public double scalar() { return Table.ONE; }
	public double getCPScaled( final int ind ) { return getCP(ind); }
	public double getCPScaled( final int[] indices ) { return getCP(indices); }

	public double getCP( int index ){
		return getCP( myTableIndex.mindex( index, myUtilIndices ) );
	}

	public double getCP( final int[] indices ){
		return myDecisionTree.getParameter( indices );
	}

	public double getCP( final Object[] instantiations )
	{
		if( myTable != null ) return myTable.value( instantiations );
		else return Table.ZERO;
	}

	public double getCP( final Map instantiations )
	{
		if( myTable != null )
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
		else return Table.ZERO;
	}

	public double getCP( final Object[] parentInstantiations, final Object childInstatiation )
	{
		if( myTable != null )
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
		else return Table.ZERO;
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
}
