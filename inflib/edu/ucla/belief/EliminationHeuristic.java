package edu.ucla.belief;

import edu.ucla.util.NamedObject;
import il2.model.Index;

import java.util.List;
import java.util.Arrays;
import java.util.Random;

/**
	Was edu.ucla.belief.dtree.MethodEliminationOrder.Algorithm

	@author Keith Cascio
	@since 092303
*/
public abstract class EliminationHeuristic extends NamedObject
{
	public static final int INT_MINFILL_REPS_DEFAULT = 7;

	protected EliminationHeuristic( String name )
	{
		super( name );
	}

	abstract public List getEliminationOrder( BeliefNetwork bn );
	abstract public il2.inf.structure.EliminationOrders.Record getEliminationOrder( Index[] leaves );
	abstract public String getJavaCodeName();

	/** @author Keith Cascio
		@since 081105 */
	public static abstract class RandomizedBestOfNStrategy extends EliminationHeuristic
	{
		public RandomizedBestOfNStrategy( String name ){
			super( name );
		}

		public int getRepetitions(){
			return this.myRepetitions;
		}

		public void setRepetitions( int reps ){
			this.myRepetitions = reps;
		}

		public void setSeed( Random seed ){
			this.mySeed = seed;
		}

		public List getEliminationOrder( BeliefNetwork bn ){
			return getEliminationOrder( bn, myRepetitions, mySeed );
		}

		public il2.inf.structure.EliminationOrders.Record getEliminationOrder( Index[] leaves ){
			return getEliminationOrder( leaves, myRepetitions, mySeed );
		}

		abstract public List getEliminationOrder( BeliefNetwork bn, int reps, Random seed );
		abstract public il2.inf.structure.EliminationOrders.Record getEliminationOrder( Index[] leaves, int reps, Random seed );

		private int myRepetitions = INT_MINFILL_REPS_DEFAULT;
		private Random mySeed = new Random();
	}

	public static final RandomizedBestOfNStrategy MIN_FILL = new RandomizedBestOfNStrategy( "Min Fill" )
	{
		public List getEliminationOrder( BeliefNetwork bn, int reps, Random seed )
		{
			return edu.ucla.belief.EliminationOrders.minFill( bn, reps, seed );
		}

		public il2.inf.structure.EliminationOrders.Record getEliminationOrder( Index[] leaves, int reps, Random seed )
		{
			return il2.inf.structure.EliminationOrders.minFill( Arrays.asList(leaves), reps, seed );
		}

		public String getJavaCodeName(){
			return "MIN_FILL";
		}
	};
	public static final EliminationHeuristic MIN_DEG = new EliminationHeuristic( "Min Deg" )
	{
		public List getEliminationOrder( BeliefNetwork bn )
		{
			return edu.ucla.belief.EliminationOrders.minDegree( bn );
		}

		public il2.inf.structure.EliminationOrders.Record getEliminationOrder( Index[] leaves )
		{
			return il2.inf.structure.EliminationOrders.minFill( Arrays.asList(leaves), 1, (Random)null );
		}

		public String getJavaCodeName(){
			return "MIN_DEG";
		}
	};
	public static final EliminationHeuristic MIN_SIZE = new EliminationHeuristic( "Min Size" )
	{
		public List getEliminationOrder( BeliefNetwork bn )
		{
			return edu.ucla.belief.EliminationOrders.minSize( bn );
		}

		public il2.inf.structure.EliminationOrders.Record getEliminationOrder( Index[] leaves )
		{
			return il2.inf.structure.EliminationOrders.minSize( Arrays.asList(leaves) );
		}

		public String getJavaCodeName(){
			return "MIN_SIZE";
		}
	};

	public static final EliminationHeuristic[] ARRAY = { MIN_FILL, MIN_SIZE };//{ MIN_FILL, MIN_DEG, MIN_SIZE };

	public static final EliminationHeuristic getDefault()
	{
		return MIN_FILL;
	}
}
