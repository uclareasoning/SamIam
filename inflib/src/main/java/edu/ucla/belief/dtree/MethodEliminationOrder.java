package edu.ucla.belief.dtree;

import edu.ucla.belief.BeliefNetwork;
//{superfluous} import edu.ucla.belief.EliminationOrders;
import edu.ucla.belief.EliminationHeuristic;

/**
	@author Keith Cascio
	@since 050103
*/
public class MethodEliminationOrder extends CreationMethod
{
	public MethodEliminationOrder()
	{
		super( "Elimination order", "elim order" );
	}

	public Dtree getInstance( BeliefNetwork bn, Settings settings ) throws Exception
	{
		EliminationHeuristic algorithmItem = settings.getElimAlgo();
		java.util.List eo = algorithmItem.getEliminationOrder( bn );

		DtreeCreateEO dtc = new DtreeCreateEO( eo );
		Dtree ret = new Dtree( bn, dtc );
		ret.myCreationMethod = this;
		return ret;
	}
}
