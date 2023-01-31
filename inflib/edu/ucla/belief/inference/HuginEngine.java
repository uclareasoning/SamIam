package edu.ucla.belief.inference;

import il2.inf.structure.EliminationOrders;
//{superfluous} import il2.inf.jointree.JoinTreeAlgorithm;
import edu.ucla.belief.*;
import java.util.*;

public class HuginEngine extends WrapperInferenceEngine
{
	public HuginEngine(BeliefNetwork bn,Dynamator dyn)
	{
		super(new HuginWrapper(bn),bn,dyn);
	}

	public HuginEngine( BeliefNetwork bn, Dynamator dyn, java.util.List eliminationOrder )
	{
		super( new HuginWrapper( bn, eliminationOrder ), bn, dyn );
	}

	/** @since 012904 */
	public HuginEngine( BeliefNetwork bn, Dynamator dyn, EliminationOrders.JT jt ){
		super( new HuginWrapper( bn, jt ), bn, dyn );
	}

	public HuginEngine(BeliefNetwork bn, Collection queryVariables, Map evidence, Dynamator dyn){
		super(new HuginWrapper(bn,queryVariables,evidence),bn,dyn);
	}

	/** @since 061404 */
	private HuginEngine( HuginWrapper rappa, BeliefNetwork bn, Dynamator dyn ){
		super( rappa, bn, dyn );
	}

	/** @since 061404 */
	public InferenceEngine handledClone( QuantitativeDependencyHandler handler )
	{
		HuginEngine ret = new HuginEngine( ((HuginWrapper)this.comp).handledClone( handler ), this.getBeliefNetwork(), this.getDynamator() );
		ret.setQuantitativeDependencyHandler( handler );
		return ret;
	}
}
