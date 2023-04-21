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

public class SSWrapper extends PartialDerivativeWrapper
{
	private PartialDerivativeEngine pde;
	private Converter c;

	public SSWrapper(BeliefNetwork bn)
	{
		c=new Converter();
		BayesianNetwork bn2=c.convert(bn);
		pde=UnindexedSSAlgorithm.create(c,bn2);
	}

    public SSWrapper(BeliefNetwork bn, Collection queryVariables, Map evidence){
	c=new Converter();
	BayesianNetwork bn2 = c.convert(bn);
	IntSet vars=c.convert(new HashSet(queryVariables));
	IntMap e=c.convert(evidence);
	pde=UnindexedSSAlgorithm.create(bn2,vars,e);
    }

    public SSWrapper( BeliefNetwork bn, List eliminationOrder )
	{
		c=new Converter();
		BayesianNetwork bn2=c.convert(bn);
		pde = UnindexedSSAlgorithm.create( c, bn2, c.convert( eliminationOrder ) );
	}

	/** @since 012904 */
	public SSWrapper( BeliefNetwork bn, EliminationOrders.JT jt ){
		this( bn, jt, (QuantitativeDependencyHandler)null );
	}

	/** @since 061404 */
	public SSWrapper handledClone( QuantitativeDependencyHandler handler ){
		return new SSWrapper( this.c.getBeliefNetwork(), ((JoinTreeAlgorithm)this.engine()).getJoinTree(), handler );
	}

	/** @since 061404 */
	public SSWrapper( BeliefNetwork bn, EliminationOrders.JT jt, QuantitativeDependencyHandler handler )
	{
		c = jt.converter;
		BayesianNetwork bn2 = jt.network;
		il2.model.Table[] tables = bn2.cpts();
		if( handler != null ) tables = c.convertTables( bn, handler );
		pde = UnindexedSSAlgorithm.create( tables, jt );
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
