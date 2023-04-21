package edu.ucla.belief.decision;

import edu.ucla.belief.*;

import java.util.Set;
import java.util.HashSet;
//import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Collection;
import java.text.NumberFormat;
import java.text.DecimalFormat;

/** @author Keith Cascio
	@since 010905 */
public class Stats
{
	public Stats(){
		init();
		reset();
	}

	public void reset(){
		for( int i=0; i<stats.length; i++ ) stats[i].reset();
	}

	public void analyzeDescendants( DecisionNode node ){
		reset();
		node.getDescendants( internals.distinct, Classifier.INTERNALS );
		node.getDescendants( leaves.distinct, Classifier.LEAVES );
		privateAnalysis();
	}

	public void analyzeAncestors( DecisionNode node ){
		reset();
		node.getAncestors( internals.distinct, Classifier.INTERNALS );
		node.getAncestors( leaves.distinct, Classifier.LEAVES );
		privateAnalysis();
	}

	public String toString(){
		StringBuffer buff = new StringBuffer( 256 );
		for( int i=0; i<stats.length; i++ ) stats[i].append( buff );
		return buff.toString();
	}

	/** @since 012405 */
	public void total( Collection collectionofstats ){
		this.reset();

		Stats current;
		for( Iterator it = collectionofstats.iterator(); it.hasNext(); ){
			current = (Stats) it.next();
			for( int i=0; i<stats.length; i++ ) stats[i].accumulate( current.stats[i] );
		}

		for( int i=0; i<stats.length; i++ ) stats[i].calcFraction();
	}

	/** @since 012405 */
	public String toStringFull()
	{
		StringBuffer buff = new StringBuffer( 512 );
		if( this.myJoint != null ){
			buff.append( this.myJoint.getID() );
			buff.append( ": " );
		}
		for( int i=0; i<stats.length; i++ ) stats[i].appendFull( buff );
		return buff.toString();
	}

	/** @since 012405 */
	public void setJoint( FiniteVariable joint ){
		this.myJoint = joint;
		if( myJoint != null ){
			CPTShell shell = myJoint.getCPTShell( myJoint.getDSLNodeType() );
			if( shell != null ){
				TableIndex index = shell.index();
				parameters.sizeExponential = index.size();
				leaves.sizeExponential = parameters.sizeExponential / myJoint.size();
				internals.sizeExponential = numInternalNodes( index );
			}
		}
	}

	/** @since 012405 */
	public static int numInternalNodes( TableIndex index ){
		int numVars = index.getNumVariables();
		if( numVars < 2 ) return (int)0;
		int total = (int)1;
		int limit = numVars - 2;
		for( int i=0; i<limit; i++ ){
			total += (total*(index.variable(i).size()));
		}
		return total;
	}

	final private void init(){
		internals = makeStat( "decision nodes" );
		leaves = makeStat( "leaves" );
		parameters = makeStat( "parameters" );
		stats = new Stat[] { internals, leaves, parameters };
		initHook();
	}

	protected Stat makeStat( String caption ){
		return new Stat( caption );
	}

	protected void initHook() {}

	private void privateAnalysis()
	{
		int numParents;
		int occurences;
		for( Iterator it = internals.distinct.iterator(); it.hasNext(); )
		{
			numParents = ((DecisionNode)it.next()).getParents().size();
			occurences = ( numParents == 0 ) ? 1 : numParents;
			internals.total += occurences;
		}

		if( leaves.distinct.isEmpty() ) return;

		int sizeJoint = ((DecisionNode)leaves.distinct.iterator().next()).getVariable().size();
		DecisionLeaf leaf;
		for( Iterator it = leaves.distinct.iterator(); it.hasNext(); ){
			leaf = (DecisionLeaf)it.next();
			numParents = leaf.getParents().size();
			//System.out.println( "  " + leaf + " parents: " + leaf.getParents() );
			occurences = ( numParents == 0 ) ? 1 : numParents;
			leaves.total += occurences;
			parameters.total += (sizeJoint * occurences);
			leaf.getOutcomes( parameters.distinct );
		}

		for( int i=0; i<stats.length; i++ ) stats[i].calcFraction();
	}

	public static float calcFraction( int numerator, int denominator )
	{
		float fraction = ((float)numerator)/((float)denominator);
		return fraction;
	}

	public static class Stat
	{
		public Stat( String caption ){
			this.caption = caption;
		}

		public void reset(){
			this.total = 0;
			if( this.distinct == null ) this.distinct = new HashSet();
			else this.distinct.clear();
			this.fraction = (float)0;
		}

		public void append( StringBuffer buff ){
			this.appendPrivate( buff );
			buff.append( ", " );
		}

		private void appendPrivate( StringBuffer buff ){
			buff.append( Integer.toString( total ) );
			buff.append( " " );
			buff.append( caption );
			if( (total < 1) || (distinct.size() < 1) ) return;
			buff.append( " ( " );
			buff.append( Integer.toString( distinct.size() ) );
			buff.append( " distinct " );
			buff.append( FORMAT_PERCENT.format( fraction ) );
			buff.append( " )" );
		}

		/** @since 012405 */
		public void appendFull( StringBuffer buff ){
			this.appendPrivate( buff );
			if( (total > 0) && (sizeExponential > 0) ){
				buff.append( " ( " );
				buff.append( FORMAT_PERCENT.format( Stats.calcFraction( total, sizeExponential ) ) );
				buff.append( " of " );
				buff.append( sizeExponential );
				buff.append( " complete )" );
			}
			buff.append( ", " );
		}

		/** @since 012405 */
		public void accumulate( Stat other ){
			this.total += other.total;
			this.distinct.addAll( other.distinct );
			this.sizeExponential += other.sizeExponential;
		}

		public void calcFraction(){
			fraction = Stats.calcFraction( distinct.size(), total );
		}

		public int total;
		public Set distinct;
		public float fraction;
		public String caption;
		public int sizeExponential = (int)0;
	}

	public static final NumberFormat FORMAT_PERCENT = new DecimalFormat( "0.##%" );

	public Stat internals, leaves, parameters;
	public Stat[] stats;
	private edu.ucla.belief.FiniteVariable myJoint;
}
