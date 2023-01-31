package edu.ucla.belief.inference;

import il2.inf.structure.EliminationOrders;
//{superfluous} import il2.inf.jointree.JoinTreeAlgorithm;
import edu.ucla.belief.*;
import java.util.*;

public class ZCEngine extends PartialDerivativeWrapperEngine
{
	public ZCEngine(BeliefNetwork bn,Dynamator dyn)
	{
		super(new ZCWrapper(bn),bn,dyn);
	}

	public ZCEngine( BeliefNetwork bn, Dynamator dyn, java.util.List eliminationOrder )
	{
		super( new ZCWrapper( bn, eliminationOrder ), bn, dyn );
	}

	/** @since 012904 */
	public ZCEngine( BeliefNetwork bn, Dynamator dyn, EliminationOrders.JT jt ){
		super( new ZCWrapper( bn, jt ), bn, dyn );
	}

	public ZCEngine( BeliefNetwork bn, Collection queryVariables, Map evidence, Dynamator dyn){
		super( new ZCWrapper(bn,queryVariables,evidence),bn,dyn);
	}

	/** @since 061404 */
	private ZCEngine( ZCWrapper rappa, BeliefNetwork bn, Dynamator dyn ){
		super( rappa, bn, dyn );
	}

	/** @since 061404 */
	public InferenceEngine handledClone( QuantitativeDependencyHandler handler )
	{
		ZCEngine ret = new ZCEngine( ((ZCWrapper)this.pdw).handledClone( handler ), this.getBeliefNetwork(), this.getDynamator() );
		ret.setQuantitativeDependencyHandler( handler );
		return ret;
	}
}
