package edu.ucla.belief.dtree;

import edu.ucla.util.NamedObject;
import edu.ucla.structure.DirectedGraph;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.Definitions;

/** @author keith cascio
	@since  20030501 */
public class MethodHmetis extends CreationMethod
{
	public MethodHmetis()
	{
		super( "hMeTiS", "hMeTiS" );
	}

	public Dtree getInstance( BeliefNetwork bn, Settings settings ) throws Exception
	{
		Object balanceFactorItem = settings.getBalanceFactor();
		int[] balanceFactorsToTry = null;
		if( balanceFactorItem == STR_BALANCE_FACTOR_ALL )
		{
			balanceFactorsToTry = ARRAY_BALANCE_FACTORS;
		}
		else balanceFactorsToTry = new int[]{ ((Integer)balanceFactorItem).intValue() };

		Dtree ret = generateBestDtree( balanceFactorsToTry, bn, settings );
		ret.myCreationMethod = this;
		return ret;
	}

	protected synchronized Dtree generateBestDtree( int[] balanceFactorsToTry, BeliefNetwork bn, Settings settings )
	{
		Dtree best = null;

		Algorithm algorithmItem = settings.getHMeTiSAlgo();
		int numDTrees = Math.max( settings.getNumDtrees(), (int)1 );//force >= 1
		int numPartitions = Math.max( settings.getNumPartitions(), (int)1 );//force >= 1

		DtreeCreateHMetis dtc = null;
		Dtree t = null;
		DirectedGraph DG = bn;
		for( int i=0; i<balanceFactorsToTry.length; i++ )
		{
			dtc = algorithmItem.getDtreeCreateHMetis( balanceFactorsToTry[i], numDTrees, numPartitions );
			try{
				t = new Dtree( DG, dtc );
			}catch( Exception e ){
				if( edu.ucla.belief.recursiveconditioning.Settings.FLAG_DEBUG_VERBOSE )
				{
					Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
					e.printStackTrace();
				}
				continue;
			}
			if( best == null || t.getWidth() < best.getWidth()) {
				best = t;
			}
			//time += dtc.getHmetisTime();
		}

		return best;
	}

	public static abstract class Algorithm extends NamedObject
	{
		protected Algorithm( String name )
		{
			super( name );
		}

		abstract public DtreeCreateHMetis getDtreeCreateHMetis( int balance, int numDtrees, int numPartitions );
	}

	public static final Algorithm ALGO_HMETIS_STANDARD = new Algorithm( "Standard hMeTiS" )
	{
		public DtreeCreateHMetis getDtreeCreateHMetis( int balance, int numDtrees, int numPartitions )
		{
			return new DtreeCreateHMetis1( balance, numDtrees, numPartitions );
		}
	};
	public static final Algorithm ALGO_HMETIS_WEIGHTED = new Algorithm( "Weighted hMeTiS" )
	{
		public DtreeCreateHMetis getDtreeCreateHMetis( int balance, int numDtrees, int numPartitions )
		{
			return new DtreeCreateHMetis1wn( balance, numDtrees, numPartitions );
		}
	};

	public static final Algorithm[] ARRAY_HMETIS_ALGOS = { ALGO_HMETIS_STANDARD, ALGO_HMETIS_WEIGHTED };

	public static final int[] ARRAY_BALANCE_FACTORS = { 1,10,20,30,40 };
	public static final String STR_BALANCE_FACTOR_ALL = "All";
	public static Object[] ARRAY_BALANCE_FACTOR_ITEMS = null;//{ STR_BALANCE_FACTOR_ALL,new Integer( 1 ),new Integer( 10 ),new Integer( 20 ),new Integer( 30 ),new Integer( 40 ) };

	static
	{
		ARRAY_BALANCE_FACTOR_ITEMS = new Object[ ARRAY_BALANCE_FACTORS.length + 1 ];
		ARRAY_BALANCE_FACTOR_ITEMS[0] = STR_BALANCE_FACTOR_ALL;
		for( int i=0; i<ARRAY_BALANCE_FACTORS.length; i++ ) ARRAY_BALANCE_FACTOR_ITEMS[i+1] = new Integer( ARRAY_BALANCE_FACTORS[i] );
	}
}
