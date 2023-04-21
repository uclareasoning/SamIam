package edu.ucla.belief.inference;

import il2.inf.structure.EliminationOrders;
import il2.inf.*;
import il2.inf.jointree.*;
import il2.bridge.Converter;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.QuantitativeDependencyHandler;
import il2.model.BayesianNetwork;

import java.util.*;
import il2.util.*;

public class ZCWrapper extends PartialDerivativeWrapper
{
	private PartialDerivativeEngine pde;
	private Converter c;

	public ZCWrapper(BeliefNetwork bn)
	{
		c=new Converter();
		BayesianNetwork bn2=c.convert(bn);
		pde=UnindexedZCAlgorithm.create(c,bn2);
	}

	public ZCWrapper(BeliefNetwork bn, Collection queryVariables, Map evidence)
	{
		c=new Converter();
		BayesianNetwork bn2 = c.convert(bn);
		IntSet vars=c.convert(new HashSet(queryVariables));
		IntMap e=c.convert(evidence);
		pde=UnindexedZCAlgorithm.create(bn2,vars,e);
	}

	public ZCWrapper( BeliefNetwork bn, List eliminationOrder )
	{
		c=new Converter();
		BayesianNetwork bn2=c.convert(bn);
		pde = UnindexedZCAlgorithm.create( c, bn2, c.convert( eliminationOrder ) );
	}

	/** @since 012904 */
	public ZCWrapper( BeliefNetwork bn, EliminationOrders.JT jt ){
		this( bn, jt, (QuantitativeDependencyHandler)null );
	}

	/** @since 061404 */
	public ZCWrapper handledClone( QuantitativeDependencyHandler handler ){
		return new ZCWrapper( this.c.getBeliefNetwork(), ((JoinTreeAlgorithm)this.engine()).getJoinTree(), handler );
	}

	/** @since 061404 */
	public ZCWrapper( BeliefNetwork bn, EliminationOrders.JT jt, QuantitativeDependencyHandler handler )
	{
		c = jt.converter;
		BayesianNetwork bn2 = jt.network;
		il2.model.Table[] tables = bn2.cpts();
		if( handler != null ) tables = c.convertTables( bn, handler );
		pde = UnindexedZCAlgorithm.create( tables, jt );
	}

	protected PartialDerivativeEngine pdengine()
	{
		return pde;
	}

	protected JointEngine engine()
	{
		return pde;
	}

	protected Converter converter()
	{
		return c;
	}
}
