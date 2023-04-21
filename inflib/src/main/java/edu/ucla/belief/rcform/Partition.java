package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 * Class that represents a partition of a set of variables with respect to a
 * set of CPTs. The partition consists of a set of mutually exclusive and
 * exhaustive formulas over the variables being partitioned.
 */
public class Partition
{
	/**
	 * Default constructor. Unsupported.
	 */
	public Partition()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Constructor with formula list argument
	 *
	 * @param Formulas a list of formulas representing the equivalence classes
	 */
	public Partition(Collection<Formula> Formulas)
	{
		parts = new ArrayList<Formula>(Formulas);
	}		
	
	/**
	 * Constructor with parents and children arguments.
	 * Builds the partition of the parents with respect to the CPTs of the
	 * children.
	 *
	 * @param parents a list of variables
	 * @param children a list of variables
	 */
	public Partition(List<FiniteVariable> parents, List<FiniteVariable> children)
	{
		//Sort the parents (so allModelsList() will work)
		Collections.sort(parents, new RCFormUtils.VariableNameComparator());
		
		//Initialize parts_map, which will map reduced-CPT-keys to lists of Terms
		Map<List<List<Double>>, List<Term>> parts_map = new HashMap<List<List<Double>>,List<Term>>();
		
		//Examine each instantiation of the parents
		for (Term t: RCFormUtils.allModelsList(parents))
		{
			//Compute the key for the reduced CPT set that this instantiation leads to
			List<List<Double>> key = RCFormUtils.multiReducedCPTKey(children, t);
			
			//Add this term to the list indexed by this key
			List<Term> cur_list = null;	
			if (parts_map.containsKey(key))
			{
				cur_list = parts_map.get(key);	
			}
			else
			{
				cur_list = new ArrayList<Term>();	
			}
			cur_list.add(t);
			
			parts_map.put(key,cur_list);
		}	
		
		//Build the list of formulas from the now-constructed map
		parts = new ArrayList<Formula>();
		for (List<Term> lt: parts_map.values())
		{
			parts.add(new Formula(lt));	
		}
	}
	
	/**
	 * Constructor with parents and children arguments, as well as a parameter for a partial instantiation of some other variables
	 * Builds the partition of the parents with respect to the CPTs of the
	 * children once they are reduced according to the given instantiation. This
	 * instantiation must be over a subset of <code>children</code>'s parents that is disjoint
	 * from <code>parents</code>.
	 * Basically this constructor is just like the previous one, except that the
	 * children CPTs are first reduced according to the <code>inst</code> argument.
	 *
	 * @param parents a list of variables
	 * @param children a list of variables
	 * @param inst an instantiation of a subset of <code>children</code>'s parents that is disjoint from <code>parents</code>
	 */
	public Partition(List<FiniteVariable> parents, List<FiniteVariable> children, Term inst)
	{
		//Sort the parents (so allModelsList() will work)
		Collections.sort(parents, new RCFormUtils.VariableNameComparator());
		
		//Initialize parts_map, which will map reduced-CPT-keys to lists of Terms
		Map<List<List<Double>>, List<Term>> parts_map = new HashMap<List<List<Double>>,List<Term>>();
		
		//Examine each instantiation of the parents
		for (Term t: RCFormUtils.allModelsList(parents))
		{
			//Create the full instantiation by combining this instantiation with inst
			Term full_t = Term.conjoin(t,inst);
			
			//Compute the key for the reduced CPT set that this full instantiation leads to
			List<List<Double>> key = RCFormUtils.multiReducedCPTKey(children, full_t);
			
			//Add this term to the list indexed by this key
			List<Term> cur_list = null;
			if (parts_map.containsKey(key))
			{
				cur_list = parts_map.get(key);	
			}
			else
			{
				cur_list = new ArrayList<Term>();	
			}
			cur_list.add(t);
			
			parts_map.put(key,cur_list);
			
		}	
		
		//Build the list of formulas from the now-constructed map
		parts = new ArrayList<Formula>();
		for (List<Term> lt: parts_map.values())
		{
			parts.add(new Formula(lt));	
		}
	}
	
	/**
	 * Gets the list of formulas corresponding to the equivalence classes
	 *
	 * @return this partition's list of formulas
	 */
	public List<Formula> getParts()
	{
		return parts;	
	}
	/**
	 * Gets the number of equivalence classes in this partition
	 *
	 * @return the number of equivalence classes in this partition
	 */
	public int numParts()
	{
		return parts.size();	
	}
	
	/**
	 * Overridden toString method.
	 * Each formula gets its own line.
	 *
	 * @return a string representation of the Partition.
	 * 
	 */
	public String toString()
	{
		String result = "";
		for (Formula m: parts)
		{
			result += "\n| " + m;	
		}
		return result;
	}
	
	/**
	 * A list of formulas corresponding to the partition's equivalence classes
	 */
	private List<Formula> parts;
}