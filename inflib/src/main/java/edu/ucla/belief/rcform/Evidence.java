package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 * Class for the evidence used by the rc algorithm. This class
 * basically is a different way to represent a Term (it's less efficient
 * but much more flexible than the Term implementation).
 */
public class Evidence
{
	public Evidence()
	{
		literals = new HashSet<Literal>();
	}
	
	public Evidence(Collection<Literal> literal_list)
	{
		literals = new HashSet<Literal>(literal_list);
	}
	
	public Evidence(Map<FiniteVariable, Object> evidence_map)
	{
		literals = new HashSet<Literal>();
		
		for (FiniteVariable v: evidence_map.keySet())
		{
			literals.add(new Literal(v, v.index(evidence_map.get(v))));	
		}
	}
	
	public static Evidence project(Evidence e, List<FiniteVariable> proj_vars)
	{
		List<Literal> kept_literals = new ArrayList<Literal>();
		
		for (Literal l: e.literals)
		{
			if (proj_vars.contains(l.getVariable()))
			{
				kept_literals.add(l);	
			}	
		}
		
		Evidence result = new Evidence(kept_literals);
		
		return result;
	}
	
	//Returns null if e and t are incompatible
	public static Evidence conjoin(Evidence e, Term t)
	{
		Set<Literal> new_literals = new HashSet<Literal>();
		
		Map<FiniteVariable, Object> new_map = e.generateMap();
		Map<FiniteVariable, Object> t_map = t.getMap();
		
		for (FiniteVariable v: t_map.keySet())
		{
			if (new_map.containsKey(v))
			{
				if (!new_map.get(v).equals(t_map.get(v)))
				{
					return null;	
				}	
			}	
			else
			{
				new_map.put(v, t_map.get(v));	
			}
		}
		
		Evidence result = new Evidence(new_map);
		
		return result;
	}
	
	public static Evidence conjoin(Evidence e1, Evidence e2)
	{
		Set<Literal> new_literals = new HashSet<Literal>();
		
		Map<FiniteVariable, Object> new_map = e1.generateMap();
		Map<FiniteVariable, Object> e2_map = e2.generateMap();
		
		for (FiniteVariable v: e2_map.keySet())
		{
			if (new_map.containsKey(v))
			{
				if (!new_map.get(v).equals(e2_map.get(v)))
				{
					return null;	
				}	
			}	
			else
			{
				new_map.put(v, e2_map.get(v));	
			}
		}
		
		Evidence result = new Evidence(new_map);
		
		return result;
	}
	
	public boolean isEmpty()
	{
		return literals.isEmpty();	
	}
	
	public Literal getOnlyLiteral()
	{
		assert (literals.size() == 1);
		
		List<Literal> lit_list = new ArrayList<Literal>(literals);
		
		return lit_list.get(0);	
	}

	public Map<FiniteVariable, Object> generateMap()
	{
		Map<FiniteVariable, Object> result = new HashMap<FiniteVariable, Object>();
		
		for (Literal l: literals)
		{
			result.put(l.getVariable(), l.getValue());	
		}
		
		return result;
	}
	
	public String toString()
	{
		if (literals.size() == 0)
		{
			return "()";		
		}	
		
		String s = new String("");
		
		s += "(";
		
		for (Literal l: literals)
		{
			s += l + " & ";	
		}
		
		s = s.substring(0, s.length()-3) + ")";

		return s;
	}

	
	private final Set<Literal> literals;

}