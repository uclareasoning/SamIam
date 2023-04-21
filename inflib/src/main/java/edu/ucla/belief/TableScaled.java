package edu.ucla.belief;

import java.util.*;
import edu.ucla.belief.recursiveconditioning.RCUtilities;

public class TableScaled extends Table
{
//	public static final double DEFAULT_SCALAR = 1.0;


	final double scalar; //representation: pr = data ^ scalar
	public double scalar() { return scalar;}


	public double toScaled( double in) { return toScaled( in, scalar);}
	public double[] toScaled( double in[]) { return toScaled( in, scalar);}
	public static double toScaled( double in, double scalar) { return Math.pow( in, 1.0/scalar);}
	public static double[] toScaled( double in[], double scalar) {
		if( in == null) { return null;}
		for( int i=0; i<in.length; i++) { in[i] = toScaled(in[i], scalar);}
		return in;
	}
	public double toRealPr( double in) { return toRealPr( in, scalar);}
	public double[] toRealPr( double in[]) { return toRealPr( in, scalar);}
	public static double toRealPr( double in, double scalar) { return Math.pow( in, scalar);}
	public static double[] toRealPr( double in[], double scalar) {
		if( in == null) { return null;}
		for( int i=0; i<in.length; i++) { in[i] = toRealPr( in[i], scalar);}
		return in;
	}


	/**
		@author Keith Cascio
		@since 110102
	*/
	public void normalize( int conditionIndex ) throws Exception
	{
		if( conditionIndex < 0 ) throw new IllegalArgumentException( "conditionIndex < 0" );

		int size = index.getJoint().size();
		int startindex = size * conditionIndex;
		int endindex = startindex + size;
		double sum = sum( data, scalar, startindex, endindex );
		if( sum <= ZERO ) throw new IllegalArgumentException( "Probabilities out of range: SUM <= 0" );

		while( startindex < endindex )
		{
			setCP( startindex, getCP(startindex)/sum);
			++startindex;
		}
	}

	/**
		@author Keith Cascio
		@since 110102
	*/
	public static double sum( double[] values, double scalar, int startindex, int endindex ) throws Exception
	{
		double ret = ZERO;
		while( startindex < endindex )
		{
			if( values[startindex] < ZERO ) throw new Exception( "Probability out of range: negative." );
			ret += toRealPr( values[ startindex ], scalar);
			++startindex;
		}

		return ret;
	}


	/**
		@author Keith Cascio
		@since 110102
	*/
	public void makeUniform()
	{
		double val = ((double)1) / ((double) index.getJoint().size());
		Arrays.fill( data, toScaled(val) );
	}


	public Object clone()
	{
		TableScaled ret = new TableScaled(index, (double[]) data.clone(), scalar);
		return ret;
	}


	/** Will return a copy of the data array which has already been scaled to real probability values.*/
	public double[] dataclone() {
		double ret[] = (double[])data.clone();
		toRealPr( ret, scalar);
		return ret;
	}

	public double getCP( int ind) {
		return toRealPr( data[ind]);
	}

	public double getCPScaled( final int ind) { return data[ind];}
	public double getCPScaled( final int[] indices) { return data[index.index(indices)];}

	public void setCP( int indx, double val) {
		data[indx] = toScaled( val);
	}



	public void fill(double value)
	{
		java.util.Arrays.fill(data, toScaled(value));
	}

	/**
	 * Copies the values from another compatible array.  The orderings
	 * of the two tables must be the same for it to work meaningfully.
	*/
	public void set(Table t)
	{
		if( (t instanceof TableScaled) && ((TableScaled)t).scalar == scalar) {
			System.arraycopy(t.data, 0, data, 0, data.length);
		}
		else {
			for( int i=0; i<data.length; i++) {
				setCP( i, t.getCP(i));
			}
		}
	}

	public void multiplyInto( Table t2) {
		if( !index().variables().containsAll( t2.index().variables())) {
			throw new IllegalArgumentException("Cannot multiplyInto.");
		}


		if( t2 instanceof TableScaled && ((TableScaled)t2).scalar == scalar) {

			int[] intoMapping = t2.index().intoMapping( index());
			for( int i=0; i<intoMapping.length; i++) {
				data[i] *= t2.data[ intoMapping[i]];
			}
		}
		else {
			int[] intoMapping = t2.index().intoMapping( index());
			for( int i=0; i<intoMapping.length; i++) {
				setCP( i, getCP(i) * t2.getCP( intoMapping[i]));
			}
		}
	}

	public Table multiply(Table t2)
	{
		if( t2 instanceof TableScaled && ((TableScaled)t2).scalar == scalar) {
			TableIndex ind = index().multiply(t2.index());
			double[] vals = new double[ind.size()];
			int[] intoMapping = index().intoMapping(ind);
			for (int i = 0; i < intoMapping.length; i++)
			{
				vals[i] = data[intoMapping[i]];
			}

			intoMapping = t2.index().intoMapping(ind);
			for (int i = 0; i < intoMapping.length; i++)
			{
				vals[i] *= t2.data[intoMapping[i]];
			}

			return new TableScaled(ind, vals, scalar);
		}
		else {
			return super.multiply( t2);
		}
	}

	public static Table multiplyAll(Set tables)
	{
		if (tables.size() == 0)
		{
			return new Table(new TableIndex(Collections.EMPTY_LIST),
				new double[]
			{
				1
			});
		}

		Set s = new HashSet();
		double scalar = -1;
		for (Iterator iter = tables.iterator(); iter.hasNext();)
		{
			Table tbl = (Table)iter.next();
			s.addAll((tbl).variables());
			if( scalar == -1 && tbl instanceof TableScaled) {
				scalar = ((TableScaled)tbl).scalar;
			}
			if( !(tbl instanceof TableScaled) || ((TableScaled)tbl).scalar != scalar) {
				return Table.multiplyAll( tables);
			}
		}

		TableIndex ind = new TableIndex(new ArrayList(s));
		double[] vals = new double[ind.size()];
		java.util.Arrays.fill(vals, 1);
		for (Iterator iter = tables.iterator(); iter.hasNext();)
		{
			TableScaled temp = (TableScaled) iter.next();
			int[] intoMapping = temp.index().intoMapping(ind);
			for (int i = 0; i < intoMapping.length; i++)
			{
				vals[i] *= temp.data[intoMapping[i]];
			}
		}

		return new TableScaled(ind, vals, scalar);
	}


	public Table expand( FiniteVariable var )
	{
		List allVars = new ArrayList( variables() );

		int childSize = ((FiniteVariable)allVars.get(allVars.size()-1)).size();
		int newParentSize = var.size();

		allVars.add(allVars.size() - 1, var);

		int blockSize=data.length/childSize;
		double[] result = new double[data.length * newParentSize];
		for (int i = 0; i < blockSize; i++) {
			for (int j = 0; j < newParentSize; j++) {
				for (int k=0;k<childSize;k++){
					result[i*newParentSize*childSize+j*childSize + k] = data[i*childSize+k];
				}
			}
		}
		return new TableScaled( allVars, result, scalar );
	}


	public Table forget(Set vars)
	{
		TableIndex ind = index.forget(vars);
		if(index.equals(ind))
		{
			return (Table)this.clone();
		}

		int[]    intoMapping = ind.intoMapping(index);
		double[] vals        = new double[ ind.size() ];
		for( int i = 0; i < intoMapping.length; i++ ){
			double s_mult_lna = scalar * Math.log( vals[intoMapping[i]]);
			double s_mult_lnb = scalar * Math.log( data[i]);
			vals[intoMapping[i]] = Math.exp( logsum( s_mult_lna, s_mult_lnb ) / scalar);
		}

		return new TableScaled(ind, vals, scalar);
	}

	/** Copied from edu.ucla.belief.recursiveconditioning.RCUtilities

		@since 20081110

		Compute ln(a+b) from ln(a) and ln(b) using the log sum equation.
		Its possible that lna and/or lnb might be NaN, or pos/neg infin.
		If lna or lnb are NaN, the result is NaN.
		If lna=lnb=negInf, then (a=b=0 and ln(0)=negInf) return negInf.
		If lna=negInf or lnb=negInf return other one.
		If lna or lnb are posInf, then (a or b =posInf and ln(posInf=posInf) return posInf. */
	final static public double logsum( double lna, double lnb) {
		double ret;

		//NaN will propagate by itself

		//Handle infinite values
		if( lna == Double.NEGATIVE_INFINITY && lnb == Double.NEGATIVE_INFINITY) { return Double.NEGATIVE_INFINITY;} //A=0 && B=0
		else if( lna == Double.NEGATIVE_INFINITY) { return lnb;} //A=0
		else if( lnb == Double.NEGATIVE_INFINITY) { return lna;} //B=0
		else if( lna == Double.POSITIVE_INFINITY || lnb == Double.POSITIVE_INFINITY) { return Double.POSITIVE_INFINITY;}

		//use log sum equation
		// ln(a+b) = ln(a) + ln(1.0 + e ^ (ln(b)-ln(a)))

		ret = lna + Math.log( 1.0 + Math.exp( (lnb-lna)));

		//if lnb-lna is (really) large, then e^(lnb-lna) might=posInf but a better value is MAX(lnb, lna)
		if( ret == Double.POSITIVE_INFINITY) { ret = Math.max( lna, lnb);}

		//return result
		return ret;
	}

	public Table shrink(Map evidence)
	{
		TableIndex ind = index.forget(evidence.keySet());
		if (ind.equals(index))
		{
			return (Table)this.clone();
		}

		int[] shrinkMapping = ind.shrinkMapping(index, evidence);
		double[] vals = new double[ind.size()];
		for (int i = 0; i < shrinkMapping.length; i++)
		{
			vals[i] = data[shrinkMapping[i]];
		}

		return new TableScaled(ind, vals, scalar);
	}

	public static Table innerProduct(Table t1, Table t2)
	{
		if( t1 instanceof TableScaled && t2 instanceof TableScaled && ((TableScaled)t1).scalar == ((TableScaled)t2).scalar) {
			for (int i = 0; i < t1.getCPLength(); i++)
			{
				t1.data[i] = t1.data[i] * t2.data[i];
			}

			return t1;
		}
		else {
			return Table.innerProduct( t1, t2);
		}
	}


	public static Table createTable( TableIndex indx, double data[], double scalar, boolean scaleData)
	{
		if( scalar == 1.0) {
			return new Table( indx, data);
		}
		else if( scalar > 0) {
			if( scaleData) {
				toScaled( data, scalar);
			}
			return new TableScaled( indx, data, scalar);
		}
		else {
			throw new IllegalStateException("Illegal scalar");
		}
	}

	public static Table createTable( TableIndex indx, double scalar)
	{
		if( scalar == 1.0) {
			return new Table( indx);
		}
		else if( scalar > 0) {
			return new TableScaled( indx, scalar);
		}
		else {
			throw new IllegalStateException("Illegal scalar");
		}
	}





//////////////
//Constructors
//////////////

	/**
	 * Creates a copy of the table.  The values stored are replicated, not shared.
 	 */
	public TableScaled(Table t)
	{
		super( t);
		if( t instanceof TableScaled) {
			this.scalar = ((TableScaled)t).scalar;
		}
		else {
			this.scalar = 1.0; //other table wasn't scaled
		}
	}

	/**
	 * Creates a copy of the table.  The values stored are replicated, not shared.
	 * @author David Allen Keith Cascio
	 * @since 072803
	 */
	public TableScaled( Table t, double scalar )
	{
		super( t.index, toScaled( (double[])t.data.clone(), scalar / t.scalar() ) );
		this.scalar = scalar;
	}


	/**
	 * Creates a table with the supplied index.
	 */
	public TableScaled(TableIndex index, double scalar)
	{
		super( index);
		this.scalar = scalar;
	}

	/**
	 * Creates a new Table.
	 * @param index  This is used directly (not copied).
	 * @param values These are used directly (not copied).
	 */
	public TableScaled(TableIndex index, double[] values, double scalar)
	{
		super( index, values);
		this.scalar = scalar;
	}

	/**
	 * Creates a new table that has a TableIndex generated from the variables
	 * supplied.
	 */
	public TableScaled(List variables, double scalar)
	{
		super( variables);
		this.scalar = scalar;
	}

	/**
	 * Creates a new table that has a TableIndex generated from the variables
	 * supplied.
	 * @param values These are used directly (not copied).
	 */
	public TableScaled(List variables, double[] values, double scalar)
	{
		this(new TableIndex(variables), values, scalar);
	}

	/**
	 * Creates a new table that has a TableIndex generated from the variables
	 * supplied.
	 */
	public TableScaled(FiniteVariable[] vars, double scalar)
	{
		this(java.util.Arrays.asList(vars), scalar);
	}

	/**
	 * Creates a new table that has a TableIndex generated from the variables
	 * supplied.
	 * @param values These are used directly (not copied).
	 */
	public TableScaled(Object[] vars, double[] values, double scalar)
	{
		this(new TableIndex(vars), values, scalar);
	}


//	public TableScaled(TableIndex index) {
//		this( index, DEFAULT_SCALAR);
//	}
//	public TableScaled(TableIndex index, double[] values) {
//		this( index, values, DEFAULT_SCALAR);
//	}
//	public TableScaled(List variables, double[] values) {
//		this( variables, values, DEFAULT_SCALAR);
//	}
//	public TableScaled(List variables) {
//		this( variables, DEFAULT_SCALAR);
//	}
//	public TableScaled(FiniteVariable[] vars) {
//		this( vars, DEFAULT_SCALAR);
//	}
//	public TableScaled(Object[] vars, double[] values) {
//		this( vars, values, DEFAULT_SCALAR);
//	}

}
