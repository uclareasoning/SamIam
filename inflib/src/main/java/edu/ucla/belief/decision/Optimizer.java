package edu.ucla.belief.decision;

import edu.ucla.belief.CPTShell;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.Table;
import edu.ucla.belief.TableIndex;
import edu.ucla.belief.StateNotFoundException;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.io.dsl.DSLNodeType;

import java.util.*;
import java.text.NumberFormat;

/** @author Keith Cascio
	@since 010805 */
public class Optimizer
{
	public Optimizer( double epsilon )
	{
		this.myEpsilon = epsilon;
	}

	public double getEpsilon(){
		return this.myEpsilon;
	}

	public void setEpsilon( double eps ){
		this.myEpsilon = eps;
	}

	public ParameterOptimizationLevel getParameterOptimizationLevel(){
		return myParameterOptimizationLevel;
	}

	public void setParameterOptimizationLevel( ParameterOptimizationLevel pol ){
		this.myParameterOptimizationLevel = pol;
	}

	public static final double DOUBLE_SUGGESTED_REDUCTION_FACTOR = (double)0.000000001;
	public static final double DOUBLE_SUGGESTED_FLOOR = (double)0.0000000000000001;
	public static final boolean FLAG_MASSAGE_SUGGESTED = true;
	private static NumberFormat FORMAT_SUGGESTED;

	/** @since 011005 */
	public static double suggestEpsilon( Table table ){
		double min = table.min();
		double min_reduced = min*DOUBLE_SUGGESTED_REDUCTION_FACTOR;
		if( min_reduced > DOUBLE_SUGGESTED_FLOOR ) return massageEpsilon( min_reduced );
		else return DOUBLE_SUGGESTED_FLOOR;
	}

	/** @since 011005 */
	public static double massageEpsilon( double min_reduced )
	{
		if( !FLAG_MASSAGE_SUGGESTED ) return min_reduced;
		if( min_reduced >= (double)1 ) return min_reduced;
		if( FORMAT_SUGGESTED == null ){
			FORMAT_SUGGESTED = NumberFormat.getNumberInstance();
			FORMAT_SUGGESTED.setParseIntegerOnly(false);
			FORMAT_SUGGESTED.setMaximumFractionDigits( 200 );
		}
		String str = FORMAT_SUGGESTED.format( min_reduced );
		int len = str.length();
		char charat;
		for( int i=0; i<len; i++ ){
			charat = str.charAt( i );
			if( Character.isDigit( charat ) && (charat != '0') ) return Double.parseDouble( str.substring( 0, i+1 ) );
		}
		return min_reduced;
	}

	/** @since 012405 */
	public Collection collectOptimizedStats( BeliefNetwork bn )
	{
		Collection ret = new LinkedList();
		FiniteVariable var;
		CPTShell shell;
		DecisionTree tree;
		Stats stats;
		for( Iterator it = bn.iterator(); it.hasNext(); ){
			var = (FiniteVariable) it.next();
			shell = var.getCPTShell( DSLNodeType.CPT );
			if( shell == null ) throw new IllegalArgumentException( "variable " + var.getID() + " lacks cpt" );
			this.setEpsilon( suggestEpsilon( shell.getCPT() ) );
			tree = optimize( shell );
			stats = new Stats();
			stats.setJoint( var );
			stats.analyzeDescendants( tree.getRoot() );
			ret.add( stats );
		}
		return ret;
	}

	/** @since 012405 */
	public void printOptimizedStats( BeliefNetwork bn, java.io.PrintWriter out )
	{
		Stats total = new Stats();
		Collection all = collectOptimizedStats(bn);
		total.total( all );
		Stats stats;
		for( Iterator it = all.iterator(); it.hasNext(); ){
			stats = (Stats) it.next();
			out.println( stats.toStringFull() );
		}
		out.println( "totals: " + total.toStringFull() );
	}

	public DecisionTree optimize( CPTShell shell )
	{
		System.out.println( "Optimizer.optimize(), pol " + myParameterOptimizationLevel + ", e " + Double.toString( myEpsilon ) );

		TableIndex tindex = shell.index();
		DecisionTreeImpl ret = new DecisionTreeImpl( tindex );

		List variables = tindex.variables();
		FiniteVariable joint = tindex.getJoint();
		int sizeJoint = joint.size();
		Parameter[] params = optimize( shell.getCPT().dataclone(), joint, (Factory)ret );
		DecisionLeaf[] leaves = optimize( params, joint, (Factory)ret );

		DecisionNode[] currentLevel = leaves;
		for( ListIterator it = variables.listIterator( variables.size()-1 ); it.hasPrevious(); ){
			currentLevel = optimize( currentLevel, (FiniteVariable)it.previous(), (Factory)ret );
		}

		if( currentLevel.length != 1 ) throw new IllegalStateException();

		ret.initRoot( currentLevel[0] );
		ret.noteOptimizationEpsilon( myEpsilon );

		return ret;
	}

	public DecisionInternal[] optimize( DecisionNode[] sublevel, FiniteVariable var, Factory factory )
	{
		int sizeVar = var.size();
		int numInternals = sublevel.length / sizeVar;
		DecisionInternal[] ret = new DecisionInternal[ numInternals ];

		HashMap mapInternals = new HashMap( ret.length/2 );
		DecisionInternal internal;
		EquivalenceKey wrapper = new EquivalenceKey( sublevel, var );
		int counter = 0;
		for( int segment=0; segment<sublevel.length; segment += sizeVar ){
			wrapper.setSegment( segment );
			if( mapInternals.containsKey( wrapper ) ) internal = (DecisionInternal) mapInternals.get( wrapper );
			else{
				mapInternals.put( wrapper, internal = wrapper.newInternal( factory, var ) );
				//factory.adopt( internal );
				wrapper = new EquivalenceKey( sublevel, var );
			}
			ret[ counter++ ] = internal;
		}

		return ret;
	}

	public DecisionLeaf[] optimize( Parameter[] params, FiniteVariable joint, Factory factory )
	{
		int sizeJoint = joint.size();
		int numLeaves = params.length / sizeJoint;
		DecisionLeaf[] ret = new DecisionLeaf[ numLeaves ];

		HashMap mapLeaves = new HashMap( ret.length/2 );
		Parameter[] leafParams;
		DecisionLeaf leaf;
		EquivalenceKey wrapper = new EquivalenceKey( params, joint );
		int counter = 0;
		for( int segment=0; segment<params.length; segment += sizeJoint ){
			wrapper.setSegment( segment );
			if( mapLeaves.containsKey( wrapper ) ) leaf = (DecisionLeaf) mapLeaves.get( wrapper );
			else{
				mapLeaves.put( wrapper, leaf = wrapper.newLeaf( factory, joint ) );
				//factory.adopt( leaf );
				wrapper = new EquivalenceKey( params, joint );
			}
			ret[ counter++ ] = leaf;
		}

		return ret;
	}

	public Parameter[] optimize( double[] data, FiniteVariable joint, Factory factory )
	{
		if( data.length<1 ) return new Parameter[0];
		return myParameterOptimizationLevel.optimize( data, joint, factory );
	}

	public abstract class ParameterOptimizationLevel
	{
		public ParameterOptimizationLevel( String name ){
			this.myName = name;
		}

		public String toString(){
			return myName;
		}

		abstract public Parameter[] optimize( double[] data, FiniteVariable joint, Factory factory );

		private String myName;
	}

	public abstract class ParameterOptimizationOn extends ParameterOptimizationLevel
	{
		public ParameterOptimizationOn( String name ){
			super( name );
		}

		public Parameter[] optimize( double[] data, FiniteVariable joint, Factory factory )
		{
			//System.out.println( "ParameterOptimizationOn.optimize()" );
			Unsortable[] sorted = new Unsortable[data.length];
			for( int i=0; i<data.length; i++ ) sorted[i] = new Unsortable( data[i], i );
			//Arrays.sort( sorted );
			this.sort( sorted, joint );
			Parameter[] ret = new Parameter[ sorted.length ];

			int sizeJoint = joint.size();
			Parameter bogus = factory.newParameter( (double)-99 );
			Parameter current = bogus;
			for( int i=0; i<sorted.length; i++ ){
				if( sorted[i].flagIsBoundary ){
					//System.out.println( "    boundary: " + i );
					current = bogus;
				}
				if( !epsilonEquals( sorted[i].value, current.getValue() ) ) current = factory.newParameter( sorted[i].value );
				ret[ sorted[i].index ] = current;
			}

			return ret;
		}

		abstract public void sort( Unsortable[] tosort, FiniteVariable joint );
	}

	public final ParameterOptimizationLevel GLOBAL = new ParameterOptimizationOn( "global" )
	{
		public void sort( Unsortable[] tosort, FiniteVariable joint ){
			Arrays.sort( tosort );
		}
	};
	public final ParameterOptimizationLevel LOCAL = new ParameterOptimizationOn( "local" )
	{
		public void sort( Unsortable[] tosort, FiniteVariable joint ){
			int sizeJoint = joint.size();
			for( int segment = 0; segment < tosort.length; segment += sizeJoint ){
				Arrays.sort( tosort, segment, segment+sizeJoint );
				tosort[segment].flagIsBoundary = true;
			}
		}
	};
	public final ParameterOptimizationLevel NONE = new ParameterOptimizationLevel( "none" )
	{
		public Parameter[] optimize( double[] data, FiniteVariable joint, Factory factory ){
			//System.out.println( "NONE.optimize()" );
			Parameter[] ret = new Parameter[ data.length ];
			for( int i=0; i<data.length; i++ ) ret[i] = factory.newParameter( data[i] );
			return ret;
		}
	};
	public final ParameterOptimizationLevel DEFAULT = GLOBAL;
	public final ParameterOptimizationLevel[] ARRAY_POLS = new ParameterOptimizationLevel[] { GLOBAL, LOCAL, NONE };

	/** @since 011105 */
	public static class EquivalenceKey
	{
		public EquivalenceKey( Object[] sublevel, FiniteVariable var ){
			this.sublevel = sublevel;
			this.length = var.size();
		}

		public boolean equals( Object o ){
			EquivalenceKey other = (EquivalenceKey)o;
			for( int i=0; i<length; i++ ){
				if( get(i) != other.get(i) ) return false;
			}
			return true;
		}

		public int hashCode(){
			return hashcode;
		}

		public void setSegment( int segment ){
			this.segment = segment;
			this.hashcode = computeHashCode();
		}

		public int computeHashCode(){
			int h=0;
			int indexhigh = segment + length;
			for( int i=segment; i<indexhigh; i++ ) h += sublevel[i].hashCode();
			return h;
		}

		public Object get( int localindex ){
			return sublevel[ segment+localindex ];
		}

		public DecisionInternal newInternal( Factory factory, FiniteVariable var )
		{
			DecisionInternal internal = factory.newInternal( var );
			try{
				for( int i=0; i<length; i++ ){
					internal.setNext( i, (DecisionNode) get(i) );
				}
			}catch( StateNotFoundException statenotfoundexception ){
				System.err.println( "Warning: " + statenotfoundexception );
			}
			return internal;
		}

		public DecisionLeaf newLeaf( Factory factory, FiniteVariable joint )
		{
			Parameter[] leafParams = new Parameter[length];
			System.arraycopy( sublevel, segment, leafParams, 0, length );
			return factory.newLeaf( joint, leafParams );
		}

		public Object[] sublevel;
		public int segment;
		public int length;
		public int hashcode;
	}

	public static class Unsortable implements Comparable
	{
		public Unsortable( double value, int index ){
			this.value = value;
			this.index = index;
		}

		public int compareTo( Object o ){
			return Double.compare( this.value, ((Unsortable)o).value );
		}

		public double value;
		public int index;
		public boolean flagIsBoundary = false;
	}

	public boolean epsilonEquals( double v1, double v2 ){
		return Math.abs( v1 - v2 ) < myEpsilon;
	}

	/** @since 011505 */
	public static boolean epsilonEquals( double v1, double v2, double epsilon ){
		return Math.abs( v1 - v2 ) < epsilon;
	}

	/** based on jdk1.5.0 Arrays.hashCode() */
	public static int hashCode( Object a[] )
	{
		if( a == null ) return 0;

		int result = 1;

		//for( Object element : a )
		//	result = 31 * result + (element == null ? 0 : element.hashCode());
		for( int i=0; i<a.length; i++ )
			result = 31 * result + (a[i] == null ? 0 : a[i].hashCode());

		return result;
	}

	private double myEpsilon = Double.MIN_VALUE;
	private ParameterOptimizationLevel myParameterOptimizationLevel = DEFAULT;
}

/*
	public static class EquivalenceWrapper
	{
		public EquivalenceWrapper(){}

		public EquivalenceWrapper( DecisionNode node ){
			this.node = node;
		}

		public boolean equals( Object o ){
			return node.equivales( ((EquivalenceWrapper)o).node );
		}

		public int hashCode(){
			return node.equivalenceHashCode();
		}

		public DecisionNode node;
	}
*/

/*
	public ParameterOptimizationLevel LOCAL = new ParameterOptimizationLevel( "local" )
	{
		public Parameter[] optimize( double[] data, FiniteVariable joint, Factory factory )
		{
			Parameter[] ret = new Parameter[ data.length ];
			int sizeJoint = joint.size();
			//for( int i=0; i<data.length; i++ ) ret[i] = factory.newParameter( data[i] );

			Parameter bogus = factory.newParameter( (double)-99 );
			Parameter current = bogus;
			int index;
			for( int segment = 0; segment < data.length; segment += sizeJoint ){
				current = bogus;
				for( int offset = 0; offset < sizeJoint; offset++ ){
					index = segment + offset;
					if( !epsilonEquals( data[index], current.getValue() ) ) current = factory.newParameter( data[index] );
					ret[index] = current;
				}
			}
			return ret;
		}
	};
*/

/*
public class ParameterFactory
{
	public ParameterFactory( double epsilon ){
		this.myEpsilon = epsilon;
		this.myList = new LinkedList();
	}

	public Parameter getParameter( double value )
	{
		double condition = value - myEpsilon;
		Parameter next;
		double nextValue;
		for( ListIterator it = myList.iterator(); it.hasNext(); ){
			next = (Parameter) it.next();
			nextValue = next.getValue();
			if( nextValue < condition

			if( epsilonEquals( nextValue, value ) ) return next;
			else if( nextValue > value ){
				it.previous();
				it.add(
			}
		}
	}

	private double myEpsilon;
	private LinkedList myList;
}
*/