package il2.inf.structure;

import java.util.List;
//import il2.util.*;
//import il2.model.*;

/**
	il2.inf.structure.JTUnifier
	@author Keith Cascio
	@since 012904
*/
public interface JTUnifier extends JoinTreeStats.StatsSource
{
	public edu.ucla.belief.tree.JoinTree asJoinTreeIL1();
	public il2.inf.structure.EliminationOrders.JT asJTIL2();
	public List eliminationOrder();
}
