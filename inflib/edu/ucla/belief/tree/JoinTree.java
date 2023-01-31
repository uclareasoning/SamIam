/*
* JoinTree.java
*
* Created on February 18, 2000, 3:57 PM
*/
package edu.ucla.belief.tree;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.structure.*;
import edu.ucla.belief.FiniteVariable;
import il2.inf.structure.JoinTreeStats;
import il2.inf.structure.JTUnifier;

import java.util.*;
import java.math.BigInteger;

/**
	@author JD Park
	@author Keith Cascio
	@since 110502
	@version
*/
public class JoinTree implements JTUnifier
{
	private IntGraph tree;
	private Set[] clusters;
	private Set[][] separators;
	private BeliefNetwork myBeliefNetwork;
	private List/*<FiniteVariable>*/ myOrder;

	/** @since 020904 */
	public JoinTree( List/*<FiniteVariable>*/ order, IntGraph t, Set[] clusters, BeliefNetwork bn )
	{
		JoinTree.this.myOrder = order;
		this.tree = t;
		this.clusters = clusters;
		this.myBeliefNetwork = bn;
		generateSeparators();
	}

	public JoinTree( JoinTree t )
	{
		JoinTree.this.myOrder = t.myOrder;
		this.tree = t.tree;
		this.clusters = t.clusters;
		this.separators = t.separators;
		this.myBeliefNetwork = t.myBeliefNetwork;
	}

	/** @since 061404 */
	public edu.ucla.belief.tree.JoinTree asJoinTreeIL1(){
		return this;
	}
	public il2.inf.structure.EliminationOrders.JT asJTIL2(){
		return null;
	}

	/** @since 20060224 */
	public List eliminationOrder(){
		return JoinTree.this.myOrder;
	}

	/** @since 020904 */
	public BeliefNetwork getBeliefNetwork(){
		return myBeliefNetwork;
	}

	/** @since 020904 */
	public void setBeliefNetwork( BeliefNetwork bn ){
		myBeliefNetwork = bn;
	}

	private void generateSeparators()
	{
		separators = new Set[tree.size()][];
		int[] neighbors;

		for (int i = 0; i < separators.length; i++)
		{
			neighbors = tree.neighbors(i);
			separators[i] = new Set[neighbors.length];
			for (int j = 0; j < neighbors.length; j++)
			{
				if (neighbors[j] < i)
				{
					separators[i][j] = separators[neighbors[j]]
					[tree.neighborIndex(neighbors[j], i)];
				}
				else
				{
					Set s = new HashSet(clusters[i]);
					s.retainAll(clusters[neighbors[j]]);
					separators[i][j] = s;
				}
			}
		}
	}

	/** @author Keith Cascio
		@since 110502 */
	public final JoinTreeStats.Stat getClusterStats(){
		return getStatsBig( clusters );
	}

	/** @author Keith Cascio
		@since 111302 */
	public final JoinTreeStats.Stat getSeparatorStats(){
		JoinTreeStats.StatImpl combined = new JoinTreeStats.StatImpl( BigInteger.ZERO, (double)0, BigInteger.ZERO );
		JoinTreeStats.StatImpl current;
		for( int i=0; i<separators.length; i++ ){
			current = getStatsBig( separators[i] );
			combined.combine( current );
		}
		JoinTreeStats.StatImpl ret =
			new JoinTreeStats.StatImpl(
					combined.getMax(),
					JoinTreeStats.logBaseTwo( combined.getMax().doubleValue() ),
					combined.getTotal() );
		return ret;
	}

    /** @since 020305 */
    public static JoinTreeStats.StatImpl getStatsDouble( Set[] arrayofset )
    {
		double largest = (double)0;
		BigInteger total = BigInteger.ZERO;
		for(int i=0;i<arrayofset.length;i++){
			double currentSize = calcProductDouble( arrayofset[i] );
			largest = Math.max( largest, currentSize );
			total = total.add( BigInteger.valueOf( (long)currentSize ) );//total+=currentSize;
		}
		return new JoinTreeStats.StatImpl( BigInteger.valueOf( (long)largest ), JoinTreeStats.logBaseTwo( largest ), total );
    }

    /** @since 020305 */
    public static JoinTreeStats.StatImpl getStatsBig( Set[] arrayofset )
    {
		BigInteger largest = BigInteger.ZERO;
		BigInteger total = BigInteger.ZERO;
		BigInteger currentSize = BigInteger.ZERO;
		for(int i=0;i<arrayofset.length;i++){
			currentSize = calcProductBig( arrayofset[i] );
			largest = largest.max( currentSize );
			total = total.add( currentSize );//total+=currentSize;
		}
		return new JoinTreeStats.StatImpl( largest, JoinTreeStats.logBaseTwo( largest.doubleValue() ), total );
    }

    /** @since 020305 */
    public static double calcProductDouble( Set setofvariables ){
    	double ret = (double)1;
		for( Iterator it = setofvariables.iterator(); it.hasNext(); ){
			ret *= (double)((FiniteVariable) it.next()).size();
		}
		return ret;
    }

    /** @since 020305 */
    public static BigInteger calcProductBig( Set setofvariables ){
    	BigInteger ret = BigInteger.ONE;
		for( Iterator it = setofvariables.iterator(); it.hasNext(); ){
			ret = ret.multiply( BigInteger.valueOf( (long)((FiniteVariable) it.next()).size() ) );
		}
		return ret;
    }

	public final IntGraph tree()
	{
		return tree;
	}

	public Set cluster(int i)
	{
		return clusters[i];
	}

	public Set separator(int vertex1, int vertex2)
	{
		return separators[vertex1][tree.neighborIndex(vertex1, vertex2)];
	}
}
