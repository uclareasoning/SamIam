package edu.ucla.belief;

import java.util.*;

/** @author keith cascio
	@since  20021007 */
public interface CPTShell extends Potential
{
	public FiniteVariable getVariable();

	/** @since 021704 */
	public int randomJointValueIndex( Map mapInstantions );

	public Table getCPT();

	public List variables();

	/** @param partial If true, then skip warnings about incomplete mapping. */
	public void replaceVariables( Map old2new, boolean partial );

//	public void forgetNoScale( Variable from );

	public void forget( Variable from );

	public void expand( FiniteVariable var );

	public void insertState( int index );

	public void removeState( int index );

	public void parentStateInserted( FiniteVariable parent, int indexNewInstance );

	public void parentStateRemoved( FiniteVariable parent, int indexRemovedInstance );

	public void ensureNonsingular();

	public void normalize() throws Exception;

	public TableIndex index();

	public void multiplyInto( Table t2);

	/**
		@author Keith Cascio
		@since 041403
	*/
	public double scalar();
	public double getCPScaled( final int ind );
	public double getCPScaled( final int[] indices );

	public double getCP( int index );

	public double getCP( final int[] indices );

	public double getCP( final Object[] instantiations );

	public double getCP( final Map instantiations );

	public double getCP( final Object[] parentInstantiations, final Object childInstatiation );

	public java.util.Set getRelevant( final int[] indices );

	public java.util.Set getRelevant( final Object[] instantiations );

	public java.util.Set getRelevant( final Map instantiations );

	public java.util.Set getRelevant( final Object[] parentInstantiations, final Object childInstatiation );

	/**
		@author Hei Chan
		@since 062503
	*/
	public CPTParameter[] getCPTParameters();
	public CPTParameter getCPTParameter(int index);
	public CPTParameter getCPTParameter(int[] mindex);
}
