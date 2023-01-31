package edu.ucla.belief.inference.map;

import edu.ucla.belief.*;
import java.util.*;

class Tables
{

	public static MaximizationTable maximize(Table t, Set vars)
	{
		TableIndex ind = t.index().forget(vars);
		int[] intoMapping = ind.intoMapping(t.index());
		double[] vals = new double[ind.size()];
		int[] locations = new int[ind.size()];
		for (int i = 0; i < intoMapping.length; i++)
		{
			if (t.getCP(i) >= vals[intoMapping[i]])
			{
				//the equality is to ensure 0s are handled correctly
				vals[intoMapping[i]] = t.getCP(i);
				locations[intoMapping[i]] = i;
			}
		}

		return new MaximizationTable(new Table(ind, vals), locations);
	}

	public static class MaximizationTable
	{
		public MaximizationTable(Table table, int[] location)
		{
			this.table = table;
			this.location = location;
		}

		public final Table table;
		public final int[] location;
	}
}
