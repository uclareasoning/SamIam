package il2.util;

import edu.ucla.belief.FiniteVariable;
import il2.bridge.Converter;
import java.util.*;

public class Instantiation
{
	protected int[] vars;
	protected int[] vals;

	public Instantiation()
	{
		vars = new int[0];
		vals = new int[0];
	}

	public Instantiation(int var, int val)
	{
		vars = new int[]{var};
		vals = new int[]{val};
	}

	public Instantiation(IntSet varSet, int[] valArray)
	{
		vars = varSet.toArray();
		vals = valArray;
	}

	private Instantiation(int[] varArray, int[] valArray)
	{
		vars = varArray;
		vals = valArray;
	}

	public String toString()
	{
		String s = "";
		for (int i = 0; i < vars.length; i++)
		{
			s += vars[i] + "<-" + vals[i];
			if (i != vars.length-1)
				s += ", ";
		}
		return s;
	}

	public boolean consistent(Instantiation inst)
	{
		int l1 = vars.length, l2 = inst.vars.length;
		int p = 0, p1 = 0, p2 = 0;
		while (!(p1 == l1 || p2 == l2))
		{
			if (vars[p1] < inst.vars[p2])
			{
				p++;
				p1++;
			}
			else if (vars[p1] > inst.vars[p2])
			{
				p++;
				p2++;
			}
			else if (vals[p1] == inst.vals[p2])
			{
				p++;
				p1++;
				p2++;
			}
			else
				return false;
		}
		return true;
	}

	public Instantiation merge(Instantiation inst)
	{
		int l1 = vars.length, l2 = inst.vars.length;
		int[] newVars = new int[l1 + l2];
		int[] newVals = new int[l1 + l2];
		int p = 0, p1 = 0, p2 = 0;
		while (!(p1 == l1 && p2 == l2))
		{
			if (p2 == l2)
			{
				for (int i = p1; i < l1; i++)
				{
					newVars[p] = vars[i];
					newVals[p] = vals[i];
					p++;
				}
				p1 = l1;
				break;
			}
			if (p1 == l1)
			{
				for (int i = p2; i < l2; i++)
				{
					newVars[p] = inst.vars[i];
					newVals[p] = inst.vals[i];
					p++;
				}
				p2 = l2;
				break;
			}
			if (vars[p1] < inst.vars[p2])
			{
				newVars[p] = vars[p1];
				newVals[p] = vals[p1];
				p++;
				p1++;
			}
			else if (vars[p1] > inst.vars[p2])
			{
				newVars[p] = inst.vars[p2];
				newVals[p] = inst.vals[p2];
				p++;
				p2++;
			}
			else if (vals[p1] == inst.vals[p2])
			{
				newVars[p] = vars[p1];
				newVals[p] = vals[p1];
				p++;
				p1++;
				p2++;
			}
			else
				return new Instantiation();
		}
		int[] varArray = new int[p];
		int[] valArray = new int[p];
		System.arraycopy(newVars, 0, varArray, 0, p);
		System.arraycopy(newVals, 0, valArray, 0, p);
		return new Instantiation(varArray, valArray);
	}

	public Map convertToIL1(Converter converter)
	{
		Map mapIL1 = new Hashtable(vars.length);
		for (int i = 0; i < vars.length; i++)
		{
			FiniteVariable il1Var = converter.convert(vars[i]);
			mapIL1.put(il1Var, il1Var.instance(vals[i]));
		}
		return mapIL1;
	}
}
