package edu.ucla.belief;

import edu.ucla.util.*;
import java.util.*;

/**
	@author Hei Chan
	@since 060803
	@from code of NoisyOrShell
	For details on exceptions, see method resetParamsAndLeaks()
*/
public class NoisyOrShellPearl implements NoisyOrShell
{
	protected TableIndex myTableIndex;
	protected double[] myWeights;
	protected int[] myStrengths;

	/** Added by Hei Chan; for computational purposes
	Whenever weights and strengths are changed, must call
		resetParamsAndLeaks()
	*/
	protected int[][] cIndexes;
	protected double[][][] cParams;
	protected double[] cLeaks;

	protected Table myTable;

	public NoisyOrShellPearl( List parents, FiniteVariable child,
		double[] weights, int[] strengths ) throws Exception
	{
		myWeights = (double[])weights.clone();
		myStrengths = (int[])strengths.clone();
		Vector variables = new Vector(parents);
		variables.add(child);
		myTableIndex = new TableIndex( variables );
		resetParamsAndLeaks();
	}

	protected NoisyOrShellPearl( NoisyOrShellPearl toCopy )
	{
		myWeights = (double[]) toCopy.myWeights.clone();
		myStrengths = (int[]) toCopy.myStrengths.clone();
		myTableIndex = (TableIndex) toCopy.myTableIndex.clone();
		cIndexes = (int[][]) toCopy.cIndexes.clone();
		cParams = (double[][][]) toCopy.cParams.clone();
		cLeaks = (double[]) toCopy.cLeaks.clone();
	}

	public Object clone()
	{
		return new NoisyOrShellPearl( this );
	}

	/** @since 021704 */
	public int randomJointValueIndex( Map mapInstantions ){
		throw new UnsupportedOperationException();
	}

	public FiniteVariable getVariable()
	{
		List vars = variables();
		return (FiniteVariable)vars.get(vars.size()-1);
	}

	public List variables()
	{
		return myTableIndex.variables();
	}

	/**	@author Hei Chan
		@since 050903
	*/
	public int[][] getIndexes() {
		return (int[][])cIndexes.clone();
	}

	public double[][][] getParams() {
		return (double[][][])cParams.clone();
	}

	public double[] getLeaks() {
		return (double[])cLeaks.clone();
	}

	public CPTParameter[] getCPTParameters() {
		CPTParameter[] parameters =
			new CPTParameter[myTable.getCPLength()];
		for (int i = 0; i < parameters.length; i++)
			parameters[i] = new CPTParameter(this,
				myTable.index.mindex(i, null));
		return parameters;
	}

	public CPTParameter getCPTParameter(int index) {
		return new CPTParameter(this, myTable.index.mindex(index,
			null));
	}

	public CPTParameter getCPTParameter(int[] mindex) {
		return new CPTParameter(this, mindex);
	}

	public CPTParameter[] getWeightParameters() {
		CPTParameter[] parameters = new
			CPTParameter[myWeights.length];
		List variables = variables();
		int sp = 0, wp = 0;
		for (int i = 0; i < cParams[0].length; i++) {
			FiniteVariable var =
				(FiniteVariable)variables.get(i);
			for (int j = 0; j < cParams[0][i].length+1; j++) {
				int index = myStrengths[sp++];
				for (int k = 0; k < cLeaks.length+1; k++) {
					parameters[wp] = new
						CPTParameter(this, wp, k,
						new VariableInstance(var,
						index));
					wp++;
				}
			}
		}
		for (int i = 0; i < cLeaks.length+1; i++) {
			parameters[wp] = new CPTParameter(this, wp, i,
				null);
			wp++;
		}
		return parameters;
	}

	public CPTParameter getWeightParameter(int weightIndex) {
		boolean leak = true;
		int index = weightIndex, varPos = 0;
		for (int i = 0; i < cParams[0].length; i++) {
			int block = (cParams[0][i].length+1) *
				(cLeaks.length+1);
			if (index < block) {
				varPos = i;
				leak = false;
				break;
			}
			index -= block;
		}
		if (leak)
			return new CPTParameter(this, weightIndex, index,
				 null);
		return new CPTParameter(this, weightIndex, index %
			(cLeaks.length+1), new
			VariableInstance((FiniteVariable)variables().
			get(varPos), myStrengths[weightIndex /
			(cLeaks.length+1)]));
	}

	/**	@author Hei Chan
	*/
	public void resetParamsAndLeaks() throws Exception {
		List vars = variables();
		int uCount = vars.size()-1;
		int gx = ((FiniteVariable)vars.get(uCount)).size()-1;
		cIndexes = new int[uCount][];
		cParams = new double[gx][uCount][];
		cLeaks = new double[gx];
		int sp = 0, wp = 0;
		boolean valid = true;
		for (int i = 0; i < uCount; i++) {
			int gu = ((FiniteVariable)vars.get(i)).size()-1;
			cIndexes[i] = new int[gu+1];
			for (int j = 0; j < gx; j++)
				cParams[j][i] = new double[gu];
			for (int j = 0; j < gu; j++) {
				cIndexes[i][myStrengths[sp++]] = j;
				cParams[0][i][j] = 1.0 - myWeights[wp++];
				for (int k = 1; k < gx; k++)
					cParams[k][i][j] = cParams[k-1][i][j]
						- myWeights[wp++];
				wp++;
			}
			cIndexes[i][myStrengths[sp++]] = gu;
			for (int j = 0; j < gx+1; j++) {
				if (j < gx && myWeights[wp] != 0.0)
					valid = false;
//					myWeights[wp] = 0.0;
				if (j == gx && myWeights[wp] != 1.0)
					valid = false;
//					myWeights[wp] = 1.0;
				wp++;
			}
		}
		cLeaks[0] = 1.0 - myWeights[wp++];
		for (int i = 1; i < gx; i++)
			cLeaks[i] = cLeaks[i-1] - myWeights[wp++];

		if (!valid)
			throw new Exception("Invalid noisy-or weights");

/*		double[] cpt = expandNoisyOr();
		for (int i = 0; i < cpt.length; i++)
			if (cpt[i] < 0.0 || cpt[i] > 1.0)
				throw new Exception("Invalid noisy-or weights");
*/
	}

	public static final String STR_ERROR_WEIGHTS_LEN =
		"NoisyOrShell.setWeights() called with incorrect size list.";

	/**
		@author Keith Cascio
		@since 041403
	*/
	public void setWeights( List listWeights ) throws Exception
	{
		double[] oldWeights = (double[])myWeights.clone();
		int[][] oldIndexes = (int[][])cIndexes.clone();
		double[][][] oldParams = (double[][][])cParams.clone();
		double[] oldLeaks = (double[])cLeaks.clone();
		Table oldTable = (Table)myTable.clone();

		if( listWeights.size() != myWeights.length ) throw new
			IllegalArgumentException( STR_ERROR_WEIGHTS_LEN );

		for( int i=0; i<myWeights.length; i++ ) myWeights[i] = ((Number)listWeights.get(i)).doubleValue();

		try {
			resetParamsAndLeaks();
			myTable = null;
		} catch (Exception e) {
			myWeights = oldWeights;
			cIndexes = oldIndexes;
			cParams = oldParams;
			cLeaks = oldLeaks;
			myTable = oldTable;
			throw e;
		}
	}

	/**
		@author Keith Cascio
		@since 061103
	*/
	public void setWeights( double[] weights ) throws Exception
	{
		double[] oldWeights = (double[])myWeights.clone();
		int[][] oldIndexes = (int[][])cIndexes.clone();
		double[][][] oldParams = (double[][][])cParams.clone();
		double[] oldLeaks = (double[])cLeaks.clone();
		Table oldTable = (Table)myTable.clone();

		if( weights.length != myWeights.length ) throw new
			IllegalArgumentException( STR_ERROR_WEIGHTS_LEN );

		for( int i=0; i<myWeights.length; i++ )
			myWeights[i] = weights[i];

		try {
			resetParamsAndLeaks();
			myTable = null;
		} catch (Exception e) {
			myWeights = oldWeights;
			cIndexes = oldIndexes;
			cParams = oldParams;
			cLeaks = oldLeaks;
			myTable = oldTable;
			throw e;
		}
	}

	/**
		@author Keith Cascio
		@since 041403
	*/
	public double[] weightsClone()
	{
		return (double[]) myWeights.clone();
	}

	public int[] strengthsClone()
	{
		return (int[]) myStrengths.clone();
	}

	/**
		@author Keith Cascio
		@since 041403
	*/
	public List weightsAsList()
	{
		List ret = new ArrayList( myWeights.length );
		for( int i=0; i<myWeights.length; i++ ) ret.add( new Double( myWeights[i] ) );
		return ret;
	}

	public void ensureNonsingular() {}

	public void normalize() throws Exception {}

	/** @since 20080220 */
	public void replaceVariables( Map old2new, boolean partial )
	{
		if( myTable != null )
		{
			myTable.replaceVariables( old2new );
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
				newVar = old2new.get( oldVar );
				if( newVar == null )
				{
					if( ! partial ){ System.err.println( "Warning attempt to NoisyOrShell.replaceVariables() with incomplete Map" ); }
					newVar = oldVar;
				}

				newVars.add( newVar );
			}

			myTableIndex = new TableIndex( newVars );
		}
	}

	public void multiplyInto( Table t2) {
		throw new UnsupportedOperationException();
	}

//	public void forgetNoScale( Variable from )
//	{
//	}

	public void forget( Variable from )
	{
	}

	public void expand( FiniteVariable var )
	{
	}

	public void insertState( int index )
	{
	}

	public void removeState( int index )
	{
	}

	public void parentStateInserted( FiniteVariable parent, int indexNewInstance )
	{
	}

	public void parentStateRemoved( FiniteVariable parent, int indexRemovedInstance )
	{
	}

	public TableIndex index()
	{
		return myTableIndex;
	}

	public Table getCPT()
	{
		if( myTable == null ) myTable = new Table( myTableIndex, expandNoisyOr() );
		return myTable;
	}

	/**
		@author Keith Cascio
		@since 041403
	*/
	public double scalar() { return Table.ONE; }
	public double getCPScaled( final int ind ) { return getCP(ind); }
	public double getCPScaled( final int[] indices ) { return getCP(indices); }

	public double getCP( int index )
	{
		if( myTable != null ) return myTable.getCP( index);
		else return Table.ZERO;
	}

	/**	@author Hei Chan
		@since 050703
	*/
	public double getCP( final int[] indices )
	{
		int x = indices[indices.length-1];
		double cp0 = 1.0, cp1 = 0.0;
		if (x != 0) {
			cp0 = cLeaks[x-1];
			for (int i = 0; i < indices.length-1; i++) {
				int j = cIndexes[i][indices[i]];
				if (j != cParams[0][i].length)
					cp0 *= cParams[x-1][i][j];
			}
		}
		if (x != cLeaks.length) {
			cp1 = cLeaks[x];
			for (int i = 0; i < indices.length-1; i++) {
				int j = cIndexes[i][indices[i]];
				if (j != cParams[0][i].length)
					cp1 *= cParams[x][i][j];
			}
		}
		return cp0 - cp1;
	}

	/**
		@author Hei Chan
		For sensitivity analysis use
	*/
	public double getCPD( int rIndex, final int[] mindex ) {
		double cpd = 1.0;
		if (rIndex != -1)
			cpd = cLeaks[0];
		for (int i = 0; i < cIndexes.length-1; i++) {
			int j = cIndexes[i][mindex[i]];
			if (i != rIndex && j != cParams[0][i].length)
				cpd *= cParams[0][i][j];
		}
		return cpd;
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

	public int[][] strengths() {
		int sp = 0;
		int[][] strengths = new int[cParams[0].length][];
		for (int i = 0; i < strengths.length; i++) {
			strengths[i] = new int[cParams[0][i].length+1];
			for (int j = 0; j < strengths[i].length; j++)
				strengths[i][j] = myStrengths[sp++];
		}
		return strengths;
	}

	/**
		<p>
		Expand noisy-Or weights into CPT.
		<p>
		@author Hei Chan
		@since 050703
	*/

	public double[] expandNoisyOr() {
		int size = 1, sp = 0, lp = 0, wp;
		List vars = variables();
		int[] blocks = new int[cParams[0].length];
		int[][] strengths = new int[blocks.length][];
		for (int i = blocks.length-1; i >= 0; i--) {
			blocks[i] = size;
			size *= (cParams[0][i].length+1);
		}
		for (int i = 0; i < blocks.length; i++) {
			int gu = cParams[0][i].length;
			strengths[i] = new int[gu+1];
			for (int j = 0; j <= gu; j++)
				strengths[i][j] = myStrengths[sp++];
			lp += strengths[i][gu] * blocks[i];
		}
		double[] cpt = new double[size * (cLeaks.length+1)];
		double[] ops = new double[size];
		Arrays.fill(ops, 0.0);
		for (int i = cLeaks.length; i > 0; i--) {
			double[] cps = computeNoisyOr(i-1, lp, size,
				blocks, strengths);
			wp = i;
			for (int j = 0; j < size; j++) {
				cpt[wp] = cps[j] - ops[j];
				ops[j] = cps[j];
				wp += (cLeaks.length+1);
			}
		}
		wp = 0;
		for (int i = 0; i < size; i++) {
			cpt[wp] = 1.0 - ops[i];
			wp += (cLeaks.length+1);
		}
		return cpt;
	}

	public double[] computeNoisyOr(int x, int lp, int size, int[]
		blocks, int[][] strengths) {
		double[] cps = new double[size];
		cps[lp] = cLeaks[x];
		for (int i = cParams[0].length-1; i >= 0; i--) {
			int l = strengths[i][strengths[i].length-1];
			lp -= l * blocks[i];
			for (int j = 0; j < strengths[i].length-1; j++) {
				int a = strengths[i][j];
				int d = (l-a) * blocks[i];
				int wp = lp + a * blocks[i];
				for (int k = 0; k < blocks[i]; k++) {
					cps[wp] = cps[wp+d] *
						cParams[x][i][j];
					wp++;
				}
			}
		}
		return cps;
	}
}
