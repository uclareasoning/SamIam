package edu.ucla.belief.inference;

import il2.inf.structure.EliminationOrders;
//{superfluous} import il2.inf.jointree.JoinTreeAlgorithm;
import edu.ucla.belief.*;
import java.util.*;

public class SSEngine extends PartialDerivativeWrapperEngine
{
	public SSEngine(BeliefNetwork bn,Dynamator dyn)
	{
		super(new SSWrapper(bn),bn,dyn);
	}

	public SSEngine( BeliefNetwork bn, Dynamator dyn, java.util.List eliminationOrder )
	{
		super(new SSWrapper( bn, eliminationOrder ),bn,dyn);
	}

	/** @since 012904 */
	public SSEngine( BeliefNetwork bn, Dynamator dyn, EliminationOrders.JT jt ){
		super( new SSWrapper( bn, jt ), bn, dyn );
	}

	public SSEngine( BeliefNetwork bn, Collection queryVariables, Map evidence, Dynamator dyn){
		super( new SSWrapper(bn,queryVariables,evidence),bn,dyn);
	}

	/** @since 061404 */
	private SSEngine( SSWrapper rappa, BeliefNetwork bn, Dynamator dyn ){
		super( rappa, bn, dyn );
	}

	/** @since 061404 */
	public InferenceEngine handledClone( QuantitativeDependencyHandler handler )
	{
		SSEngine ret = new SSEngine( ((SSWrapper)this.pdw).handledClone( handler ), this.getBeliefNetwork(), this.getDynamator() );
		ret.setQuantitativeDependencyHandler( handler );
		return ret;
	}
}
