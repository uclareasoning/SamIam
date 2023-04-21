package edu.ucla.belief.inference.map;

import edu.ucla.belief.*;
import java.util.*;

public class MapEngine extends TableEliminationEngine
{
	protected Set mapvars;
	int[][] locations;
	double value;
	TableIndex[] from;
	TableIndex[] to;
	FiniteVariable[] vars;
	int currentInd;
	double probability;
	boolean eliminated = false;

	public MapEngine(BeliefNetwork bn, Set mapvars, Map evidence)
	{
		this(bn, null, mapvars, evidence);
	}

	private int countVariables(Collection tables)
	{
		Set vars = new HashSet();
		for (Iterator iter = tables.iterator(); iter.hasNext();)
		{
			vars.addAll(((Table) iter.next()).variables());
		}

		return vars.size();
	}

	public MapEngine(BeliefNetwork bn, List eliminationOrder,
		Set mapvars, Map evidence)
	{
		this.mapvars = mapvars;
		locations = new int[mapvars.size()][];
		from = new TableIndex[mapvars.size()];
		to = new TableIndex[mapvars.size()];
		vars = new FiniteVariable[mapvars.size()];
		Set tables = Table.shrinkAll( bn.tables(), evidence );
		if (eliminationOrder == null)
		{
			eliminationOrder = EliminationOrders.mapOrder(tables, mapvars);
			int sumvarcount = bn.size() - mapvars.size()-evidence.size();
			for (int i = 0; i < sumvarcount; i++)
			{
				if (mapvars.contains(eliminationOrder.get(i)))
				{
					//System.err.println(eliminationOrder);
					System.err.println("var count:"+bn.size());
					System.err.println("elimination count:"+
						eliminationOrder.size());
					System.err.println("table variable count:"+
						countVariables(tables));
					System.err.println("pre shrink vc:"+
						countVariables( bn.tables() ));
					System.err.println(mapvars);
					throw new IllegalArgumentException("Wrong order");
				}
			}

			for (int i = sumvarcount; i < sumvarcount+mapvars.size(); i++)
			{
				if (!mapvars.contains(eliminationOrder.get(i)))
				{
					System.err.println(eliminationOrder);
					System.err.println("var count:"+bn.size());
					System.err.println("elimination count:"+
						eliminationOrder.size());
					System.err.println("offending variable:"+
						eliminationOrder.get(i));
					System.err.println(mapvars);
					throw new IllegalStateException("Evil order");
				}
			}
		}

		initialize(eliminationOrder, tables, true);
	}

	protected Set combine(Set vals, Object var)
	{
		Table t;
		if (vals.size() == 0)
		{
			return Collections.EMPTY_SET;
		}
		else if (vals.size() == 1)
		{
			t = (Table) vals.iterator().next();
		}
		else
		{
			t = Table.multiplyAll(vals);
		}

		if (var == null)
		{
			probability = t.getCP(0);
			return Collections.EMPTY_SET;
		}

		if (mapvars.contains(var))
		{
			Tables.MaximizationTable max =
			Tables.maximize(t, Collections.singleton(var));
			locations[currentInd] = max.location;
			from[currentInd] = t.index();
			to[currentInd] = max.table.index();
			vars[currentInd] = (FiniteVariable) var;
			currentInd++;
			return Collections.singleton(max.table);
		}
		else
		{
			return Collections.singleton(
				t.forget( Collections.singleton(var)));
		}
	}

	public Map getInstance()
	{
		if (!eliminated)
		{
			eliminate();
			eliminated = true;
		}

		Map instance = new HashMap();
		for (int i = from.length - 1; i >= 0; i--)
		{
			//FiniteVariable[] tvars = to[i].getVariableArray();
			List tvars = to[i].variables();
			int[] tinst = instance(tvars, instance);
			int tloc = to[i].index(tinst);
			int[] finst = from[i].mindex(locations[i][tloc], null);
			instance.put(vars[i],
				new Integer(finst[from[i].variableIndex(vars[i])]));
		}

		return convert(instance);
	}

	private Map convert(Map numInst)
	{
		Map result = new HashMap(numInst.size());
		for (Iterator iter = numInst.entrySet().iterator();
		iter.hasNext();)
		{
			Map.Entry entry = (Map.Entry) iter.next();
			result.put(entry.getKey(),
				((FiniteVariable) entry.getKey()).instance(
				((Integer) entry.getValue()).intValue()));
		}

		return result;
	}

	private int[] instance(List varlist, Map instance)
	{
		//int[] result = new int[varlist.length];
		//for (int i = 0; i < result.length; i++)
		//{
		//	result[i] = ((Integer) instance.get(varlist[i])).intValue();
		//}
		int[] result = new int[varlist.size()];
		int counter = 0;
		for( Iterator it = varlist.iterator(); it.hasNext(); )
		{
			result[counter++] = ((Integer) instance.get( it.next() )).intValue();
		}

		return result;
	}

	public double probability()
	{
		if (!eliminated)
		{
			eliminate();
			eliminated = true;
		}

		return probability;
	}
}
