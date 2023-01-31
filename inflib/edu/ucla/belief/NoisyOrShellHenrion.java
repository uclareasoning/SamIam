package edu.ucla.belief;

import java.util.*;

/**
	@author Keith Cascio
	@since 100702
	@changed by Hei Chan
	For details on exceptions, see method resetParamsAndLeaks()
*/
public class NoisyOrShellHenrion implements NoisyOrShell
{
	public NoisyOrShellHenrion( List variables, double[] weights )
		throws Exception
	{
		//System.out.print( "NoisyOrShell( "+variables+", " );
		//Table.print( weights );
		//System.out.println( " )" );
		myWeights = weights;
		myTableIndex = new TableIndex( variables );
		resetParamsAndLeaks();
	}

	protected NoisyOrShellHenrion( NoisyOrShellHenrion toCopy )
	{
		myWeights = (double[]) toCopy.myWeights.clone();
		myTableIndex = (TableIndex) toCopy.myTableIndex.clone();
		cParams = (double[][][]) toCopy.cParams.clone();
		cLeaks = (double[]) toCopy.cLeaks.clone();
	}

	public Object clone()
	{
		return new NoisyOrShellHenrion( this );
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
	public double[][][] getParams() {
		return (double[][][])cParams.clone();
	}

	public double[] getLeaks() {
		return (double[])cLeaks.clone();
	}

	/**	@author Hei Chan
		@since 050703
		Expand CPT to check if valid, without writing to memory
		Throws an exception if invalid
	*/
	public void resetParamsAndLeaks() throws Exception {
		List vars = variables();
		int uCount = vars.size()-1;
		int gx = ((FiniteVariable)vars.get(uCount)).size()-1;
		cParams = new double[gx][uCount][];
		cLeaks = new double[gx];
		int pos = 0;
		for (int i = 0; i < uCount; i++) {
			int gu = ((FiniteVariable)vars.get(i)).size()-1;
			for (int j = 0; j < gx; j++)
				cParams[j][i] = new double[gu];
			for (int j = 0; j < gu; j++) {
				cParams[0][i][j] = 1.0 - myWeights[pos++];
				for (int k = 1; k < gx; k++)
					cParams[k][i][j] = cParams[k-1][i][j]
						- myWeights[pos++];
				pos++;
			}
		}
		cLeaks[0] = 1.0 - myWeights[pos++];
		for (int i = 1; i < gx; i++)
			cLeaks[i] = cLeaks[i-1] - myWeights[pos++];

		double[] cpt = expandNoisyOr();
		for (int i = 0; i < cpt.length; i++)
			if (cpt[i] < 0.0 || cpt[i] > 1.0)
				throw new Exception("Invalid noisy-or weights");
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
			cParams = oldParams;
			cLeaks = oldLeaks;
			myTable = oldTable;
			throw e;
		}
	}

	public void setWeights( double[] weights ) throws Exception
	{
		double[] oldWeights = (double[])myWeights.clone();
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
		int wp = 0;
		for (int i = 0; i < cParams[0].length; i++) {
			FiniteVariable var =
				(FiniteVariable)variables.get(i);
			for (int j = 0; j < cParams[0][i].length; j++) {
				for (int k = 0; k < cLeaks.length+1; k++) {
					parameters[wp] = new
						CPTParameter(this, wp, k,
						new VariableInstance(var,
						j));
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
			int block = cParams[0][i].length *
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
			get(varPos), index / (cLeaks.length+1)));
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
			for (int i = 0; i < indices.length-1; i++)
				if (indices[i] != cParams[0][i].length)
					cp0 *= cParams[x-1][i][indices[i]]
						/ cLeaks[x-1];
		}
		if (x != cLeaks.length) {
			cp1 = cLeaks[x];
			for (int i = 0; i < indices.length-1; i++)
				if (indices[i] != cParams[0][i].length)
					cp1 *= cParams[x][i][indices[i]]
						/ cLeaks[x];
		}
		return cp0 - cp1;
	}

	/**
		@author Hei Chan
		@since 051203
		For sensitivity analysis use
	*/
	public double getCPD( int j, final int[] indices ) {
		double cpd = cLeaks[0];
		for (int i = 0; i < indices.length-1; i++)
			if (i != j && indices[i] != cParams[0][i].length)
				cpd *= cParams[0][i][indices[i]] / cLeaks[0];
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


	/**
		<p>
		Expand noisy-Or weights into CPT.
		<p>
		@author Hei Chan
		@since 050703
	*/

	public double[] expandNoisyOr() {
		int size = 1, wp;
		for (int i = 0; i < cParams[0].length; i++)
			size *= (cParams[0][i].length + 1);
		double[] cpt = new double[size * (cLeaks.length+1)];
		double[] cps = new double[size];
		double[] ops = new double[size];
		Arrays.fill(ops, 0.0);
		for (int i = cLeaks.length; i > 0; i--) {
			computeNoisyOr(cps, i-1);
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

	public void computeNoisyOr(double[] cps, int x) {
		cps[cps.length-1] = cLeaks[x];
		int a = 0, b = 0, c = 1, d = 1;
		int j = cParams[0].length - 1;
		int k = cParams[0][j].length;
		for (int i = cps.length-2; i >= 0; i--) {
			if (++b == c) {
				if (++a > k) {
					a = 1;
					c *= (k+1);
					k = cParams[0][--j].length;
				}
				d = a * c;
				b = 0;
			}
			cps[i] = cps[i+d] * cParams[x][j][k-a] / cLeaks[x];
		}
	}

/*	public static List expandNoisyOr(List noisyOrWeights)
	{
		int size = noisyOrWeights.size();
		int cptSize = 1;
		for (int i = 0; i < size / 2; i++)
		cptSize *= 2;
		double[] cpt = null;
		//**************************************************
		//NOTE:
		//If the assumption that the node is a binary variable
		//is violated, and the node has many parents, this
		//line will very likely fail because the JVM cannot
		//allocate enough memory.	new() will throw an
		//OutOfMemoryError.
		try
		{
			cpt = new double[cptSize];
		}
		catch( OutOfMemoryError err )
		{
			return null;
		}

		//**************************************************
		Arrays.fill(cpt, -1);

		double leak = ((Double)noisyOrWeights.get(size - 1)).doubleValue();
		double[] params = new double[size / 2 - 1];
		for (int i = 0; i < params.length; i++)
			params[i] = ((Double)noisyOrWeights.get(2*i + 1)).doubleValue();

		cpt[cptSize - 1] = leak;
		for (int i = cptSize - 1; i >= 0; i -= 2)
		{
			cpt[i - 1] = 1.0 - cpt[i];
			int offset = 2;
			for (int j = params.length - 1; j >= 0; j++)
			{
				if (i < offset) break;
				if (cpt[i - offset] >= 0) break;
				cpt[i - offset] = cpt[i] * params[j] / leak;
				offset *= 2;
			}
		}

		ArrayList cptList = new ArrayList(cptSize);
		for (int i = 0; i < cptSize; i++)
			cptList.add(new Double(cpt[i]));
		return cptList;
	}
*/

/*	public static double[] expandNoisyOr( double[] noisyOrWeights )
	{
		int size = noisyOrWeights.length;
		int cptSize = 1;
		for (int i = 0; i < size / 2; i++)
			cptSize *= 2;
		double[] cpt = null;
		//**************************************************
		//NOTE:
		//If the assumption that the node is a binary variable
		//is violated, and the node has many parents, this
		//line will very likely fail because the JVM cannot
		//allocate enough memory.	new() will throw an
		//OutOfMemoryError.
		try
		{
			cpt = new double[cptSize];
		}
		catch( OutOfMemoryError err )
		{
			return null;
		}

		//**************************************************
		Arrays.fill(cpt, -1);

		double leak = noisyOrWeights[size - 1];//((Double)noisyOrWeights.get(size - 1)).doubleValue();
		double[] params = new double[size / 2 - 1];
		for (int i = 0; i < size / 2 - 1; i++)
			params[i] = noisyOrWeights[2*i + 1];//((Double)noisyOrWeights.get(2*i + 1)).doubleValue();

		cpt[cptSize - 1] = leak;
		for (int i = cptSize - 1; i >= 0; i -= 2)
		{
			cpt[i - 1] = 1.0 - cpt[i];
			int offset = 2;
			for (int j = params.length - 1; j >= 0; j++)
			{
				if (i < offset) break;
				if (cpt[i - offset] >= 0) break;
				cpt[i - offset] = cpt[i] * params[j] / leak;
				offset *= 2;
			}
		}

		return cpt;
	}
*/
	protected TableIndex myTableIndex;
	protected double[] myWeights;

	/** Added by Hei Chan; for computational purposes
	Whenever weights are changed, must call setParamsAndLeaks()
	*/
	protected double[][][] cParams;
	protected double[] cLeaks;

	protected Table myTable;
}
