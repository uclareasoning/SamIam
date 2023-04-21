package edu.ucla.belief;

import java.util.*;
import java.io.PrintWriter;
import java.io.PrintStream;

//WARNING: When changing functions in this class, be aware that TableScaled
//  also uses them, or may need to override them.

public class Table implements Potential, edu.ucla.belief.io.CPTInfo.ReadableWritableTable
{
	public static double ZERO = (double)0;
	public static double ONE = (double)1;

	/**
		@author Keith Cascio
		@since 041403
	*/
	public double scalar() { return ONE; }
	public double getCPScaled( final int ind ) { return getCP( ind ); }
	public double getCPScaled( final int[] indices ) { return getCP( index.index(indices) ); }

	/**
		@author Keith Cascio
		@since 060602
	*/
	public boolean isValidProbability()
	{
		for( int i=0; i<data.length; i++ )
		{
			//if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.print( data[i] + " " );
			//if( data[i] < ZERO || data[i] > ONE ) return false;
			//if( !(data[i] >= ZERO && data[i] <= ONE ) )
			if( !(data[i] >= ZERO && data[i] <= Double.MAX_VALUE ) )
			{
				System.err.println( "Warning: invalid probability\n" + this );
				return false;
			}
		}

		return true;
	}

	/**
		@author Keith Cascio
		@since 110102
	*/
	public void normalize() throws Exception
	{
		if( !isSingular() )
		{
			int numconditions = data.length / index.getJoint().size();
			for( int i=0; i<numconditions; i++ )
			{
				normalize( i );
			}
		}
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
		double sum = sum( data, startindex, endindex );
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
	public static double sum( double[] values, int startindex, int endindex ) throws Exception
	{
		double ret = ZERO;
		while( startindex < endindex )
		{
			if( values[startindex] < ZERO ) throw new Exception( "Probability out of range: negative." );
			ret += values[ startindex ];
			++startindex;
		}

		return ret;
	}

	/**
		@author Keith Cascio
		@since 110102
	*/
	public void ensureNonsingular()
	{
		if( isSingular() ) makeUniform();
	}

	/**
		@author Keith Cascio
		@since 110102
	*/
	public boolean isSingular()
	{
		for( int i=0; i<data.length; i++ )
		{
			if( data[i] != (int)0 ) return false;
		}

		return true;
	}

	/**
		@author Keith Cascio
		@since 110102
	*/
	public void makeUniform()
	{
		double val = ((double)1) / ((double) index.getJoint().size());
		Arrays.fill( data, val );
	}


	/**
	 * The cpt entry values.
	*/
	protected double[] data;
	/**
	 * The index mapping instances to array indices.
	*/
	protected TableIndex index;
	/**
	 * Creates a copy of the table.  The values stored are replicated, not shared.
	*/
	public Table(Table t)
	{
		index = t.index;
		data = (double[]) t.data.clone();

		if( t.scalar() != 1.0) { //this class requires the scalar to be 1.0
			TableScaled.toRealPr( data, t.scalar());
		}
	}

	/**
	 * Creates a table with the supplied index.  All of the values are initialized to 0.
	*/
	public Table(TableIndex index)
	{
		this.index = index;
		this.data = new double[index.size()];
	}

	public Object clone()
	{
		Table ret = new Table(index, (double[]) data.clone());
		return ret;
	}

	public static boolean FLAG_DEBUG = Definitions.DEBUG;//false;

	/** @since 20020604 */
	public void replaceVariables( Map old2new )
	{
		this.replaceVariables( old2new, false );
	}

	/** @since 20080220 */
	public void replaceVariables( Map old2new, boolean partial )
	{
		if( FLAG_DEBUG ){ Definitions.STREAM_VERBOSE.print( super.toString().substring(16) + ".replaceVariables()\n\t oldvars: " + variables() ); }

		List   newVars   = new ArrayList( index.size() );
		Object newVar    = null, oldVar = null;
		for( Iterator it = variables().iterator(); it.hasNext(); ){
			oldVar       = it.next();
			newVar       = old2new.get( oldVar );
			if( newVar  == null ){
				if( ! partial ){ System.err.println( "warning: attempt to Table.replaceVariables() with incomplete Map" ); }
				newVar   = oldVar;
			}
			newVars.add( newVar );
		}

		if( FLAG_DEBUG ){ Definitions.STREAM_VERBOSE.print( "\t newvars: " + variables() ); }

		this.index = new TableIndex( newVars );
	}

	/**
		@author Keith Cascio
		@since 041403
	*/
	public static void printArr( double[] array, PrintWriter stream )
	{
		int i = (int)0;
		int limit = array.length-1;
		stream.print( "{ " );
		for( i=0; i < limit; i++ ) stream.print( array[i] + ", " );
		stream.print( array[i] + " }" );
	}

	public static void printArr( double[] array, PrintStream stream )
	{
		int i = (int)0;
		int limit = array.length-1;
		stream.print( "{ " );
		for( i=0; i < limit; i++ ) stream.print( array[i] + ", " );
		stream.print( array[i] + " }" );
	}

	/**
		@author Keith Cascio
		@since 110603
	*/
	public static void printArr( Table t, PrintWriter stream )
	{
		printArr( t.data, stream );
	}

	public static void printArr( Table t, PrintStream stream )
	{
		printArr( t.data, stream );
	}

	/**Calls TableScaled.toScaled on each value.
	*/
	public static void printArrScaled( double[] array, double scalar )
	{
		PrintStream stream = Definitions.STREAM_VERBOSE;
		int i = (int)0;
		int limit = array.length-1;
		stream.print( "{ " );
		for( i=0; i < limit; i++ ) stream.print( TableScaled.toRealPr(array[i], scalar) + ", " );
		stream.println( TableScaled.toScaled(array[i], scalar) + " }" );
	}
	/**Calls TableScaled.toRealPr on each value.
	*/
	public static void printArrUnScaled( double[] array, double scalar )
	{
		PrintStream stream = Definitions.STREAM_VERBOSE;
		int i = (int)0;
		int limit = array.length-1;
		stream.print( "{ " );
		for( i=0; i < limit; i++ ) stream.print( TableScaled.toRealPr(array[i], scalar) + ", " );
		stream.println( TableScaled.toRealPr(array[i], scalar) + " }" );
	}


	/**
	 * Creates a new Table.
	 * @param index  This is used directly (not copied).
	 * @param values These are used directly (not copied).
	*/
	public Table( TableIndex index, double[] values ){
		this( index, values, true );
	}

	/** @since 20060804 */
	public Table( TableIndex index, double[] values, boolean strict )
	{
		if( strict && (index.size() != values.length) ){
			String msg = "illegal attempt to create table for variable \""+index.getJoint()+"\", number of probability values supplied, "+values.length+", does not match size of cpt implied by index: " + index.size();
			throw new IllegalArgumentException( msg );
		}

		this.index = index;
		this.data = values;
	}

	/**
	 * Creates a new table that has a TableIndex generated from the variables
	 * supplied.
	 * @param values These are used directly (not copied).
	*/
	public Table(List variables, double[] values)
	{
		this(new TableIndex(variables), values);
	}

	/**
	 * Creates a new table that has a TableIndex generated from the variables
	 * supplied.
	*/
	public Table(List variables)
	{
		this.index = new TableIndex(variables);
		this.data = new double[index.size()];
	}

	/**
	 * Creates a new table that has a TableIndex generated from the variables
	 * supplied.
	*/
	public Table(FiniteVariable[] vars)
	{
		this(java.util.Arrays.asList(vars));
	}

	/**
	 * Creates a new table that has a TableIndex generated from the variables
	 * supplied.
	 * @param values These are used directly (not copied).
	*/
	public Table(Object[] vars, double[] values)
	{
		this(new TableIndex(vars), values);
	}

	/** @since 050904 */
	public void setValues( double[] valuesToSet ){
		if( data.length != valuesToSet.length ) throw new IllegalArgumentException( "data.length != valuesToSet.length" );
		System.arraycopy( valuesToSet, 0, data, 0, data.length );
	}

	public double[] dataclone() {
		return (double[])data.clone();
	}

	public int getCPLength() {
		return data.length;
	}

	public double getCP( int ind) {
		return data[ind];
	}

	public void setCP( int indx, double val) {
		data[indx] = val;
	}

	/**
		@ret The maximum single conditional probability
		value <= 1 in data.
		@author Keith Cascio
		@since 071003
	*/
	public double max()
	{
		double max = Double.NEGATIVE_INFINITY;
		for( int i=0; i<data.length; i++ )
			if( (max < data[i]) && (data[i] <= (double)1) ) max = data[i];
		return max;
	}

	/**
		@ret The minimum single conditional probability
		value >= 0 in data.
		@author Keith Cascio
		@since 010905
	*/
	public double min()
	{
		double min = Double.POSITIVE_INFINITY;
		for( int i=0; i<data.length; i++ )
			if( (0 <= data[i]) && (data[i] < min) ) min = data[i];
		return min;
	}

	/** @since 021704 */
	public int random( Map mapInstantions )
	{
		FiniteVariable joint = index.getJoint();
		if( mapInstantions.containsKey( joint ) ) return joint.index( mapInstantions.get( joint ) );

		int linear = index.index( mapInstantions );
		int blockSize = index.blockSize( joint );

		double random = RANDOM.nextDouble();
		//Definitions.STREAM_VERBOSE.println( cond );
		int len = joint.size() - 1;
		double upperbound = (double)0;
		int i;
		for( i=0; i<len; i++ )
		{
			upperbound += this.getCP( linear );
			if( random <= upperbound ) return i;
			linear += blockSize;
		}
		return i;
	}

	private static Random RANDOM = new Random();

	/**
	 * Returns the value of the table entry referenced by ind.
	 * @param ind The the instance of each variable in the table.
	*/
	public double value(int[] ind)
	{
		return getCP(index.index(ind));
	}

	/**
		@author Keith Cascio
		@since 100702
	*/
	public double value( Object[] instantiations )
	{
		return getCP(index.index(instantiations));
	}

	/**
	 * Sets the value of the table entry referenced by ind.
	 * @param ind The the instance of each variable in the table.
	 * @param val The value to set the entry to.
	*/
	public void setValue(int[] ind, double val)
	{
		setCP( index.index(ind), val);
	}

	/**
	 * Returns the TableIndex of this Table.
	*/
	public TableIndex index()
	{
		return index;
	}

	/**
	 * Returns the variables that this Table contains.
	*/
	public List variables()
	{
		return index.variables();
	}

	public void fill(double value)
	{
		java.util.Arrays.fill(data, value);
	}
//	public void fill(Object value)
//	{
//		double val = ((Number) value).doubleValue();
//		fill( val);
//	}

	/**
	 * Copies the values from another compatible array.  The orderings
	 * of the two tables must be the same for it to work meaningfully.
	*/
	public void set(Table t)
	{
		System.arraycopy(t.data, 0, data, 0, data.length);
	}

	private static String stringHeader( List vars )
	{
		return stringHeader( new StringBuffer( 0x40 ), vars, null ).toString();
	}

	/** @since 20080116 */
	private static StringBuffer stringHeader( StringBuffer buff, List vars, int[] widths ){
		int    i = 0;
		String id;
		for( Iterator it = vars.iterator(); it.hasNext(); ++i ){
			buff.append( id = ((FiniteVariable)it.next()).getID() );
			if( widths == null ){ buff.append( '\t' ); }
			else{
				for( int j = widths[i] - id.length(); j>=0; --j ){ buff.append( ' ' ); }
			}
		}
		return buff.append("Value");
	}

	private static String stringInstance( List vars, int[] inds, double value )
	{
		return stringInstance( new StringBuffer( 50 ), vars, null, inds, value ).toString();
	}

	/** @since 20080116 */
	private static StringBuffer stringInstance( StringBuffer buff, List vars, int[] widths, int[] inds, double value ){
		int    counter = 0;
		String instance;
		for( Iterator it = vars.iterator(); it.hasNext(); ++counter ){
			buff.append( instance = ((FiniteVariable)it.next()).instance( inds[ counter ] ).toString() );
			if( widths == null ){ buff.append( '\t' ); }
			else{
				for( int j = widths[counter] - instance.length(); j>=0; --j ){ buff.append( ' ' ); }
			}
		}
		return buff.append( value );
	}

	public String toString()
	{
		return super.toString().substring(16) + "\n" + tableString();
	}

	/** @since 20040506 */
	public String tableString( String tab )
	{
		List              vars    = variables();
		int[]             widths  = new int[ vars.size() ];
		int               index   = 0;
		FiniteVariable    var;
		for(     Iterator vit     = vars.iterator(); vit.hasNext(); ++index ){
			for( Iterator iit     = (var = (FiniteVariable) vit.next()).instances().iterator(); iit.hasNext(); ){
				widths[   index ] = Math.max( iit.next().toString().length(), widths[ index ] );
			}
			widths[       index ] = Math.max( var.getID().length(), widths[ index ] );
		}

		StringBuffer      buff    = new StringBuffer( 0x80 );
		String            nltab   = "\n";
		if( tab != null ){
			buff.append( tab );
			nltab     += tab;
		}
		stringHeader( buff, vars, widths );

		int[] inds;
		for( TableIndex.Iterator iter = this.index.iterator(); iter.hasNext(); ){
			index = iter.next();
			inds  = iter.current();
			buff.append( nltab );
			stringInstance( buff, vars, widths, inds, getCP( index ) );
		}

		return buff.toString();
	}

	/** @since 20040521 */
	public String tableString(){
		return tableString( (String)null );
	}

	public void multiplyInto( Table t2) {
		if( !index().variables().containsAll( t2.index().variables())) {
			throw new IllegalArgumentException("Cannot multiplyInto.");
		}
		int[] intoMapping = t2.index().intoMapping( index());
		for( int i=0; i<intoMapping.length; i++) {
			data[i] *= t2.getCP( intoMapping[i]);
		}
	}

	public static Table multiply( Table t1, Table t2) {
		return t1.multiply(t2);
	}
	public Table multiply(Table t2)
	{
		TableIndex ind = index().multiply(t2.index());
		double[] vals = new double[ind.size()];
		int[] intoMapping = index().intoMapping(ind);
		for (int i = 0; i < intoMapping.length; i++)
		{
			vals[i] = getCP(intoMapping[i]);
		}

		intoMapping = t2.index().intoMapping(ind);
		for (int i = 0; i < intoMapping.length; i++)
		{
			vals[i] *= t2.getCP(intoMapping[i]);
		}

		return new Table(ind, vals);
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
		for (Iterator iter = tables.iterator(); iter.hasNext();)
		{
			s.addAll(((Table) iter.next()).variables());
		}

		TableIndex ind = new TableIndex(new ArrayList(s));
		double[] vals = new double[ind.size()];
		java.util.Arrays.fill(vals, 1);
		for (Iterator iter = tables.iterator(); iter.hasNext();)
		{
			Table temp = (Table) iter.next();
			int[] intoMapping = temp.index().intoMapping(ind);
			for (int i = 0; i < intoMapping.length; i++)
			{
				vals[i] *= temp.getCP(intoMapping[i]);
			}
		}

		return new Table(ind, vals);
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
					result[i*newParentSize*childSize+j*childSize + k] = getCP(i*childSize+k);
				}
			}
		}
		return new Table( allVars, result );
	}


	public Table forget( Variable var )
	{
		Table ret = forget(Collections.singleton(var));

		if( var instanceof FiniteVariable )
		{
			int vsize = ((FiniteVariable) var).size();
			for( int i = 0; i < ret.getCPLength(); i++ ) ret.setCP( i, ret.getCP(i) / vsize);
		}

		return ret;
	}

	public Table forget(Set vars)
	{
		TableIndex ind = index.forget(vars);
		if(index.equals(ind))
		{
			return (Table)this.clone();
		}

		int[] intoMapping = ind.intoMapping(index);
		double[] vals = new double[ind.size()];
		for (int i = 0; i < intoMapping.length; i++)
		{
			vals[intoMapping[i]] += getCP(i);
		}

		return new Table(ind, vals);
	}

	public Table project(Set vars)
	{
		Set forgotten=new HashSet(index.variables());
		forgotten.removeAll(vars);
		return forget(forgotten);
	}

	public void addInto(Table t)
	{
		int[] intoMapping = index.intoMapping(t.index);
		for(int i=0;i<intoMapping.length;i++)
		{
			setCP( intoMapping[i], getCP(intoMapping[i]) + t.getCP(i));
		}
	}

	/**
		@author Keith Cascio
		@since 110503
	*/
	public void addIntoTrivial( Table that )
	{
		if( that.data.length != this.data.length ) throw new IllegalArgumentException( "Table.addIntoTrivial() called with wrong size argument." );

		for( int i=0; i<this.data.length; i++ ) this.data[i] += that.data[i];
	}

	/**
		@author Keith Cascio
		@since 110603
	*/
	public void addIntoTrivialScale( Table that, double scalar )
	{
		if( that.data.length != this.data.length ) throw new IllegalArgumentException( "Table.addIntoTrivialScale() called with wrong size argument." );

		for( int i=0; i<this.data.length; i++ ) this.data[i] += (that.data[i] * scalar);
	}

	/**
		@author Keith Cascio
		@since 110603
	*/
	public void scale( double scalar )
	{
		if( scalar == ZERO ) fill( ZERO );
		else if( scalar == ONE ) return;
		else for( int i=0; i<this.data.length; i++ ) this.data[i] *= scalar;
	}

	/** Could return a Table or TableScaled.*/
	public Table add(Table t)
	{
		Table result= (Table)this.clone();
		result.addInto(t);
		return result;
	}

	public void divideInto(Table t)
	{
		int[] intoMapping = t.index().intoMapping(index);
		for(int i=0;i<intoMapping.length;i++)
		{
			setCP(i, getCP(i) / t.getCP(intoMapping[i]));
		}
	}

	public Table divide(Table t)
	{
		Table result= (Table)this.clone();
		result.divideInto(t);
		return result;
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
			vals[i] = getCP(shrinkMapping[i]);
		}

		return new Table(ind, vals);
	}

	/**
		102502 Modified to accomodate CPTShell

		@author Keith Cascio
		@since 102502
	*/
	public static Set shrinkAll( Collection tables, Map evidence) {
		Set result = new HashSet();
		CPTShell shell = null;
		for( Iterator iter = tables.iterator(); iter.hasNext(); )
		{
			shell = (CPTShell) iter.next();
			result.add( shell.getCPT().shrink( evidence));
		}

		return result;
	}



	public Table permute(FiniteVariable[] order)
	{
		Table result=new Table(order);
		int[] intoMapping = result.index().intoMapping(index);
		for(int i=0;i<intoMapping.length;i++)
		{
			result.setCP( intoMapping[i], getCP(i));
		}

		return result;
	}

	public static Table normalize(Table t)
	{
		if( t == null ) return null;

		double total = 0;
		for (int i = 0; i < t.getCPLength(); i++)
		{
			total += t.getCP(i);
		}

		for (int i = 0; i < t.getCPLength(); i++)
		{
			t.setCP( i, t.getCP(i) / total);
		}

		return t;
	}

	public static Table innerProduct(Table t1, Table t2)
	{
		for (int i = 0; i < t1.getCPLength(); i++)
		{
			t1.setCP( i, t1.getCP(i) * t2.getCP(i));
		}

		return t1;
	}

	public boolean satisfiesCPTProperty( FiniteVariable var, double epsilon) {
		int blockSize = index.blockSize( var);
		int bigBlockSize = blockSize * var.size();
		for( int i=0; i<data.length; i+=bigBlockSize) {
			double sum = 0;
			for( int j=i; j<i+bigBlockSize; j+= blockSize) {
				sum += getCP( j);
			}

			if( Math.abs( 1-sum) > epsilon) { return false;}
		}
		return true;
	}

	public boolean ensureCPTProperty( FiniteVariable var) {
		int blockSize = index.blockSize(var);
		int bigBlockSize = blockSize * var.size();
		boolean ok = true;
		for (int i = 0; i < data.length; i += bigBlockSize)
		{
			double sum = 0;
			for (int j = i; j < i + bigBlockSize; j += blockSize)
			{
				sum += getCP(j);
			}

			if (sum == 0)
			{
				ok = false;
			}

			for (int j = i; j < i + bigBlockSize; j += blockSize)
			{
				setCP( j, data[j]/sum);
			}
		}

		return ok;
	}

	public int maxInd()
	{
		int best=0;
		for(int i=0;i<data.length;i++) if(getCP(i)>getCP(best)) best=i;

		return best;
	}

	public static boolean FLAG_DEBUG_STATE_MODIFICATION = false;

	/**
		@author Keith Cascio
		@since 101502
	*/
	public void insertState( int ind) {
		if( FLAG_DEBUG_STATE_MODIFICATION ) Definitions.STREAM_VERBOSE.println( "\nTable.insertState("+ind+")" );

		int newCardinality = index.getJoint().size();
		int oldCardinality = newCardinality - 1;
		int lenDataOld = getCPLength();
		int lenDataPerState = lenDataOld / oldCardinality;
		int lenDataNew = lenDataOld + lenDataPerState;
		double[] newCPT = new double[ lenDataNew ];

		int indexNew = (int)0;
		int indexOld = (int)0;
		int counter  = (int)-1;
		while( indexNew < lenDataNew )
		{
			for( counter=0; counter<ind; counter++ )
			{
				if( indexNew < lenDataNew && indexOld < lenDataOld )
				newCPT[indexNew++] = data[indexOld++];
			}
			if( indexNew < lenDataNew ) newCPT[indexNew] = (double)0;
			++indexNew;
			for( ; counter<oldCardinality; counter++ )
			{
				if( indexNew < lenDataNew && indexOld < lenDataOld )
				newCPT[indexNew++] = data[indexOld++];
			}
		}

		if( FLAG_DEBUG_STATE_MODIFICATION )
		{
			Definitions.STREAM_VERBOSE.print( "newCPT: ");
			printArr( newCPT, Definitions.STREAM_VERBOSE );
		}

		index = new TableIndex( variables());
		data = newCPT;
	}

	/**
		@author Keith Cascio
		@since 101502
	*/
	public void removeState( int ind) {
		if( FLAG_DEBUG_STATE_MODIFICATION ) Definitions.STREAM_VERBOSE.println( "\nTable.removeState("+ind+")" );

		int newCardinality = index.getJoint().size();
		int oldCardinality = newCardinality + 1;
		int lenDataOld = getCPLength();
		int lenDataPerState = lenDataOld / oldCardinality;
		int lenDataNew = lenDataOld - lenDataPerState;
		double[] newCPT = new double[ lenDataNew ];

		int indexNew = (int)0;
		int indexOld = (int)0;
		int counter  = (int)-1;
		while( indexNew < lenDataNew )
		{
			for( counter=0; counter<ind; counter++ )
			{
				if( indexNew < lenDataNew && indexOld < lenDataOld )
				newCPT[indexNew++] = data[indexOld++];
			}
			++indexOld;
			for( ; counter<newCardinality; counter++ )
			{
				if( indexNew < lenDataNew && indexOld < lenDataOld )
				newCPT[indexNew++] = data[indexOld++];
			}
		}

		if( FLAG_DEBUG_STATE_MODIFICATION )
		{
			Definitions.STREAM_VERBOSE.print( "newCPT: ");
			printArr( newCPT, Definitions.STREAM_VERBOSE );
		}

		index = new TableIndex( variables());
		data = newCPT;
	}

	/**
		@author Keith Cascio
		@since 101502
	*/
	public void parentStateInserted( FiniteVariable parent, int indexNewInstance) {
		if( FLAG_DEBUG_STATE_MODIFICATION ) Definitions.STREAM_VERBOSE.println( "\nTable.parentStateInserted( "+parent+", "+indexNewInstance+" )" );

		TableIndex TI = index();
		int indexParent = TI.variableIndex( parent );
		if( indexParent != (int)-1 )
		{
			int numVariables = TI.getNumVariables();
			List variables = variables();

			int fillLength = 1;
			for( int k=indexParent+1; k<numVariables; k++ )
			{
				fillLength *= ((FiniteVariable)variables.get( k )).size();
			}

			int preoffset = fillLength * indexNewInstance;
			int oldCardinality = TI.cardinality( indexParent );
			int newCardinality = parent.size();
			int postoffset = fillLength * (newCardinality-indexNewInstance-1);
			int inoffset = preoffset + postoffset;

			int lenDataOld = getCPLength();
			int lenDataInserted = lenDataOld / oldCardinality;
			int lenDataNew = lenDataOld + lenDataInserted;
			double[] newCPT = new double[ lenDataNew ];

			double fillValue = (double)1/(double)TI.getJoint().size();

			if( FLAG_DEBUG_STATE_MODIFICATION )
			{
				Definitions.STREAM_VERBOSE.println( "oldCardinality:\t" + oldCardinality );
				Definitions.STREAM_VERBOSE.println( "newCardinality:\t" + newCardinality );
				Definitions.STREAM_VERBOSE.println( "fillLength:\t" + fillLength );
				Definitions.STREAM_VERBOSE.println( "preoffset:\t" + preoffset );
				Definitions.STREAM_VERBOSE.println( "inoffset:\t" + inoffset );
				Definitions.STREAM_VERBOSE.println( "postoffset:\t" + postoffset );
				Definitions.STREAM_VERBOSE.println( "lenDataNew:\t" + lenDataNew );
				Definitions.STREAM_VERBOSE.println( "fill value:\t" + fillValue );
			}

			if( preoffset > 0 )
			{
				if( FLAG_DEBUG_STATE_MODIFICATION ) Definitions.STREAM_VERBOSE.println( "arraycopy( originalCPT[0], newCPT[0], "+preoffset+" ) (pre)" );
				System.arraycopy( data, 0, newCPT, 0, preoffset);
			}

			int indexOld = preoffset;
			int indexNew = preoffset;
			int fillToIndex = (int)-1;
			int copyLength = (int)-1;
			int bound = lenDataNew - postoffset;
			boolean debugFlagIsPost = false;
			while( indexNew < bound )
			{
				if( FLAG_DEBUG_STATE_MODIFICATION ) Definitions.STREAM_VERBOSE.println( "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" );
				fillToIndex = indexNew+fillLength;
				if( FLAG_DEBUG_STATE_MODIFICATION ) Definitions.STREAM_VERBOSE.println( "fill( newCPT, "+indexNew+", "+fillToIndex+" )" );
				Arrays.fill( newCPT, indexNew, fillToIndex, fillValue );

				if( fillToIndex+inoffset <= lenDataNew )
				{
					copyLength = inoffset;
					debugFlagIsPost = false;
				}
				else
				{
					copyLength = postoffset;
					debugFlagIsPost = true;
				}

				if( copyLength > (int)0 )
				{
					if( FLAG_DEBUG_STATE_MODIFICATION )
					{
						Definitions.STREAM_VERBOSE.print( "arraycopy( originalCPT["+indexOld+"], newCPT["+fillToIndex+"], "+copyLength+" ) " );
						Definitions.STREAM_VERBOSE.println( debugFlagIsPost ? "(post)" : "(in)" );
					}
					System.arraycopy( data, indexOld, newCPT, fillToIndex, copyLength);
				}

				indexOld += copyLength;
				indexNew += fillLength + copyLength;
			}

			index = new TableIndex( variables());
			data = newCPT;
		}
	}


	/**
		@author Keith Cascio
		@since 101502
	*/
	public void parentStateRemoved( FiniteVariable parent, int indexRemovedInstance) {
		if( FLAG_DEBUG_STATE_MODIFICATION ) Definitions.STREAM_VERBOSE.println( "\nTable.parentStateRemoved( "+parent+", "+indexRemovedInstance+" )" );

		TableIndex TI = index();
		int indexParent = TI.variableIndex( parent );
		if( indexParent != (int)-1 )
		{
			int numVariables = TI.getNumVariables();
			List variables = variables();


//			for( int i=0; i<numVariables; i++) {
//				FiniteVariable fv = (FiniteVariable)variables.get(i);
//				//Definitions.STREAM_VERBOSE.println("" + i + ": " + fv + " : " + fv.instances());
//			}

			int deleteLength = 1;
			for( int k=indexParent+1; k<numVariables; k++ )
			{
				deleteLength *= ((FiniteVariable)variables.get( k )).size();
			}

			int preoffset = deleteLength * indexRemovedInstance;
			int oldCardinality = TI.cardinality( indexParent );
			int newCardinality = parent.size();
			int postoffset = deleteLength * (newCardinality-indexRemovedInstance);
			int inoffset = preoffset + postoffset;

			int lenDataOld = getCPLength();
			int lenDeletedData = lenDataOld / oldCardinality;
			int lenDataNew = lenDataOld - lenDeletedData;
			double[] newCPT = new double[ lenDataNew ];

			if( FLAG_DEBUG_STATE_MODIFICATION )
			{
				Definitions.STREAM_VERBOSE.println( "oldCardinality:\t" + oldCardinality );
				Definitions.STREAM_VERBOSE.println( "newCardinality:\t" + newCardinality );
				Definitions.STREAM_VERBOSE.println( "deleteLength:\t" + deleteLength );
				Definitions.STREAM_VERBOSE.println( "preoffset:\t" + preoffset );
				Definitions.STREAM_VERBOSE.println( "inoffset:\t" + inoffset );
				Definitions.STREAM_VERBOSE.println( "postoffset:\t" + postoffset );
				Definitions.STREAM_VERBOSE.println( "lenDataNew:\t" + lenDataNew );
				Definitions.STREAM_VERBOSE.println( "oldDataLength:\t" + data.length );
				Definitions.STREAM_VERBOSE.println( "newDataLength:\t" + newCPT.length);
			}

			if( preoffset > 0 )
			{
				if( FLAG_DEBUG_STATE_MODIFICATION ) Definitions.STREAM_VERBOSE.println( "arraycopy( originalCPT[0], newCPT[0], "+preoffset+" ) (pre)" );
				System.arraycopy( data, 0, newCPT, 0, preoffset);
			}

			int indexNew = preoffset;
			int indexOld = preoffset;
			int copyLength = (int)-1;
			int bound = lenDataNew - postoffset;
			boolean debugFlagIsPost = false;
			while( indexNew <= bound )
			{
				if( FLAG_DEBUG_STATE_MODIFICATION ) Definitions.STREAM_VERBOSE.println( "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%" );

				indexOld += deleteLength;
				if( indexNew+inoffset <= lenDataNew )
				{
					copyLength = inoffset;
					debugFlagIsPost = false;
				}
				else if( postoffset < 1 ) break;
				else
				{
					copyLength = postoffset;
					debugFlagIsPost = true;
				}

				if( copyLength > (int)0 )
				{
					if( FLAG_DEBUG_STATE_MODIFICATION )
					{
						Definitions.STREAM_VERBOSE.print( "arraycopy( originalCPT["+indexOld+"], newCPT["+indexNew+"], "+copyLength+" ) " );
						Definitions.STREAM_VERBOSE.println( debugFlagIsPost ? "(post)" : "(in)" );
					}
					System.arraycopy( data, indexOld, newCPT, indexNew, copyLength);

					indexNew += copyLength;
					indexOld += copyLength;
				}
			}

			index = new TableIndex( variables());
			data = newCPT;
		}
	}



	/** For any variable=state, if all possible combinations of other variables have prob=0, then this state can be removed.
		@returns An ArrayList (or null) where even indices will be FiniteVariables and odd indices will be states which can be removed. */
	public ArrayList valueElimination(){
		ArrayList ret = null;

		boolean statesToRemove[][];
		statesToRemove = new boolean[index.getNumVariables()][];

		for( int i=0; i<statesToRemove.length; i++) {
			FiniteVariable fv = index.variable(i);
			statesToRemove[i] = new boolean[fv.size()];
			Arrays.fill( statesToRemove[i], true); //assume can remove all and then will change it as necessary
		}

		int mindex[] = null;
		for( int i=0; i<data.length; i++) {
			if( data[i] != 0) {
				//for every var=state combo, this state is necessary
				mindex = index.mindex( i, mindex);
				for( int j=0; j<mindex.length; j++) {
					statesToRemove[j][mindex[j]] = false;
				}
			}
		}

		//remove unnecessary states
		for( int i=0; i<statesToRemove.length; i++) {
			FiniteVariable fv = index.variable(i);
			for( int j=0; j<statesToRemove[i].length; j++) {
				if( statesToRemove[i][j]) {
					if(ret==null) {ret = new ArrayList();}
					ret.add(fv);
					ret.add(fv.instance(j));
				}
			}
		}
		return ret;
	}

	/** @author Hei Chan */
	public double distanceMeasure( Table table2 ){
		if( table2 == null ) return Double.NaN;
		return distanceMeasure( this.data, table2.data );
	}

	/** @author Hei Chan
		@author Keith Cascio 022805
	*/
	public static double distanceMeasure( double[] data1, double[] data2 ){
		if( data1.length != data2.length ) return Double.NaN;
		double max = 1.0, min = 1.0;
		for (int i = 0; i < data1.length; i++) {
			if( data1[i] == 0.0 && data2[i] == 0.0 ) continue;
			double ratio = data1[i] / data2[i];
			if( ratio > max ) max = ratio;
			else if( ratio < min ) min = ratio;
		}
		return Math.log( max / min );
	}

	/**
		@author Keith Cascio
		@since 080504
	*/
	public double entropy()
	{
		double sum = (double)0;
		int len = data.length;
		for( int i=0; i<len; i++ )
		{
			sum += (data[i] * myLog(data[i]));
		}
		return -sum;
	}

	/**
		@author Keith Cascio
		@since 080504
	*/
	public static final double myLog( double arg )
	{
		if( arg == (double)0 ) return (double)0;
		else return Math.log( arg ) * INVERSE_LN_2;
	}

	private static final double INVERSE_LN_2 = ((double)1) / Math.log( (double)2 );

	/**
		@author Keith Cascio
		@since 081903
	*/
	public boolean epsilonEquals( Table table, double epsilon )
	{
		if( this.data.length != table.data.length ) return false;

		for( int i=0; i<this.data.length; i++ )
		{
			if( Math.abs( this.data[i] - table.data[i] ) > epsilon )
			{
				//Definitions.STREAM_VERBOSE.println( "Table.epsilonEquals("+epsilon+") failed: " + this.data[i] + " != " + table.data[i] );
				return false;
			}
		}

		return true;
	}
}
