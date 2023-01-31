package edu.ucla.belief.inference;

import il2.inf.structure.EliminationOrders;
import il2.inf.*;
import il2.inf.jointree.*;
import il2.bridge.Converter;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.QuantitativeDependencyHandler;
import il2.model.BayesianNetwork;
import il2.util.*;
import java.util.*;

public class HuginWrapper extends JointWrapper
{
	private JointEngine engine;
	private Converter c;

	public HuginWrapper(BeliefNetwork bn)
	{
		c=new Converter();
		BayesianNetwork bn2=c.convert(bn);
		engine=UnindexedHuginAlgorithm.create( c, bn2 );
	}

    public HuginWrapper(BeliefNetwork bn, Collection queryVariables, Map evidence){
	c=new Converter();
	BayesianNetwork bn2 = c.convert(bn);
	IntSet vars=c.convert(new HashSet(queryVariables));
	IntMap e=c.convert(evidence);
	engine=UnindexedHuginAlgorithm.create(bn2,vars,e);
    }

	public HuginWrapper( BeliefNetwork bn, List eliminationOrder )
	{
		c=new Converter();
		BayesianNetwork bn2=c.convert(bn);
		engine = UnindexedHuginAlgorithm.create( c, bn2, c.convert( eliminationOrder ) );
	}

	/** @since 012904 */
	public HuginWrapper( BeliefNetwork bn, EliminationOrders.JT jt ){
		this( bn, jt, (QuantitativeDependencyHandler)null );
	}

	/** @since 061404 */
	public HuginWrapper handledClone( QuantitativeDependencyHandler handler ){
		return new HuginWrapper( this.c.getBeliefNetwork(), ((JoinTreeAlgorithm)this.engine()).getJoinTree(), handler );
	}

	/** @since 061404 */
	public HuginWrapper( BeliefNetwork bn, EliminationOrders.JT jt, QuantitativeDependencyHandler handler )
	{
		c = jt.converter;
		BayesianNetwork bn2 = jt.network;
		il2.model.Table[] tables = bn2.cpts();
		if( handler != null ) tables = c.convertTables( bn, handler );
		engine = UnindexedHuginAlgorithm.create( tables, jt );
	}

	protected JointEngine engine()
	{
		return engine;
	}

	protected Converter converter()
	{
		return c;
	}
}
