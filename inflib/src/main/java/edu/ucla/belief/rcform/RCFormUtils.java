package edu.ucla.belief.rcform;

import edu.ucla.belief.*;
import edu.ucla.util.*;

import java.util.*;

/**
 * Class with various variables and methods used by other classes in the RCForm
 * package.
 * 
 * @author Taylor Curtis
 */
public class RCFormUtils
{
	
	/**
	 * Sets up the list of all variables in the network and accompanying 
	 * variables. net_vars is assumed to be already sorted
	 */
	public static void setNetworkVariables(List<FiniteVariable> net_vars)
	{
		all_network_vars = new ArrayList<FiniteVariable>(net_vars);
		
		net_var_array_size = (net_vars.size()+30) / 31;
		
		//Initialize the list of lists of variables
		network_variables = new ArrayList<List<FiniteVariable>>();
		for (int i=0; i<net_var_array_size; i++)
		{
			network_variables.add(new ArrayList<FiniteVariable>());	
		}
		
		//Add each variable to the list of lists of variables (each
		//list can fit 31 variables, since we're using integers to
		//encode instantiations).
		for (int i=0; i<net_vars.size(); i++)
		{
			network_variables.get(i/31).add(net_vars.get(i));	
		}			
	}
	
	/**
	 * Returns a list containing the given variable's parents
	 * 
	 * @param var a variable
	 *
	 * @return a set containing <code>var</code>'s parents
	 */
	public static List<FiniteVariable> getParents(FiniteVariable var)
	{
		//Get the set of variables in var's CPT
		List<FiniteVariable> parent_list = new ArrayList<FiniteVariable>(var.getCPTShell().variables());
		
		//Remove var itself, and what's left is var's parents
		parent_list.remove(var);
		
		//Collections.sort(parent_list, new RCFormUtils.VariableNameComparator());
		
		return parent_list;		
	}
	
	/**
	 * Returns a list of all models of a given list of variables.
	 * Assumes that the given list is sorted.
	 *
	 * @param vars a sorted list of variables
	 *
	 * @return a list of all models of <code>vars</code> 
	 */
	public static List<Term> allModelsList(List<FiniteVariable> vars)
	{
		List<Term> result = new ArrayList<Term>();
			
		//Get the indices of these variables in the list of all variables
		List<Integer> indices = getVariableIndices(vars);
		
		//Build the list of integers that encodes the list of variables
		int[] var_bits = computeVarBits(vars);
		
		//Build an empty term to prime the recursive buildAllModels function
		int[] partial_term = new int[RCFormUtils.net_var_array_size];
		for (int i=0; i<partial_term.length; i++)
		{
			partial_term[i] = 0;	
		}
		
		//Find all models
		buildAllModels(result, 0, indices, var_bits, partial_term); 
		
		return result;
	}
	
	/**
	 * Recursive function used to build all models for a list of variables
	 *
	 * @param model_list The list of models being constructed
	 * @param index An iterator used to traverse a list of indices of the variables in the list
	 * @param indices The list of indices corresponding to the variables in the list
	 * @param var_bits A bitmap encoding the list of variables
	 * @param partial_term The term under construction
	 */
	public static void buildAllModels(List<Term> model_list, int index, List<Integer> indices, int[] var_bits, int[] partial_term)
	{
		//Base case. If we have visited all the variables in the list, then
		//we're done building the term, and we can add it to the model list.
		if (index == indices.size())
		{
			model_list.add(new Term(var_bits, partial_term));	
		}
		
		//Recursive case. Find the next variable, and make two recursive calls:
		//one with that variable true, and one with it false.
		else
		{
			//Get the index of the next variable to be examined.
			int next_index = indices.get(index);
			
			//Compute which list this variable will be found in, and where in the list
			//it will be found.
			int slice = next_index / 31;
			int offset = next_index % 31;	
			
			//Create two copies of the partial term so far
			int[] new_partial_term_neg = new int[partial_term.length];
			int[] new_partial_term_pos = new int[partial_term.length];
			for (int i=0; i<partial_term.length; i++)
			{
				new_partial_term_neg[i] = partial_term[i];
				new_partial_term_pos[i] = partial_term[i];
			}
			
			//Set variable true and recurse (to set the variable true, add the power of
			//two corresponding to the position of the variable)
			new_partial_term_pos[slice] += RCFormUtils.powers_of_two[RCFormUtils.network_variables.get(slice).size() - 1 - offset];	
			buildAllModels(model_list, index+1, indices, var_bits, new_partial_term_pos);
			
			//Set variable false and recurse (nothing needs to be done to
			//new_partial_term_neg, because setting a variable false adds 0 to the int)
			buildAllModels(model_list, index+1, indices, var_bits, new_partial_term_neg);
		}
	}
	
	/**
	 * Generates a random instantiation of a given list of variables.
	 * The variables are assumed to be sorted, and all binary.
	 *
	 * @param vars a sorted list of binary variables
	 * @param rng a random number generator
	 *
	 * @return a random instantiation of <code>vars</code>
	 */
	public static Term randomTerm(List<FiniteVariable> vars, Random rng)
	{
		//Initialize varbits and termbits
		int[] varbits = new int[net_var_array_size];
		int[] termbits = new int[net_var_array_size];
		for (int i=0; i<varbits.length; i++)
		{
			varbits[i] = 0;
			termbits[i] = 0;	
		}
		
		//Get the indices of the variables in the list
		List<Integer> var_indices = getVariableIndices(vars);
		
		//Go through the variables, choosing some of them to instantiate
		for (int i=0; i<vars.size(); i++)
		{
			//Maybe choose this variable for instantiation
			if (rng.nextDouble() < 4.0/vars.size())
			{
				int index = var_indices.get(i);
				int slice = index / 31;
				int offset = index % 31;
				
				//Update varbits to include this variable
				varbits[slice] += powers_of_two[network_variables.get(slice).size()-1-offset];
				
				if (rng.nextBoolean())
				{
					//Assign true to this variable. This is done by adding the appropriate
					//power of two to the appropriate element of termbits.
					termbits[slice] += powers_of_two[network_variables.get(slice).size()-1-offset];	
				}	
				else
				{
					//Nothing need be done to assign false to the variable (because you're
					//just adding 0 to termbits).	
				}
			}	
		}
		
		//Create the actual term object
		Term result = new Term(varbits, termbits);
		
		return result;
	}
	
	
	/**
	 * Given a list of variables, returns a list of the indices of those variables
	 * in the <code>all_network_vars</code> list. The input list must be sorted.
	 *
	 * @param variables a sorted list of variables
	 *
	 * @return a list of integers, where each integer denotes the index of the variable at that position in <code>variables</code>
	 */
	private static List<Integer> getVariableIndices(List<FiniteVariable> variables)
	{
		List<Integer> result = new ArrayList<Integer>();
		
		//Go through the variable list, finding each one in the all_network_vars list
		//(use j to keep track of where you are in all_network_vars).
		int j = 0;
		for (int i=0; i<variables.size(); i++)
		{
			//Search for the variable
			while (!all_network_vars.get(j).equals(variables.get(i)))	
			{
				//Keep searching...
				j++;	
			}
			
			//Found it! Add the index to the list of indices
			result.add(j);
			
			j++;
		}
		
		return result;
	}
	
	/**
	 * Returns a bitmap representing the given list of variables.
	 * The bitmap takes the form of a list of integers: Think about them
	 * laid out side-by-side in binary. It can't be one integer because ints
	 * are only 32 bits. If a variable is present in the list, the bitmap has
	 * a 1 in that variable's position, otherwise it has a 0. The positions of
	 * variables are determnied by the already-computed network_variables
	 * data structure.
	 *
	 * varlist must be sorted.
	 *
	 * @param varlist a sorted list of variables
	 *
	 * @return a bitmap (in the form of a list of integers) representing <code>varlist</code>
	 */
	public static int[] computeVarBits(List<FiniteVariable> varlist)
	{
		//Initialize the bitmap
		int[] result = new int[RCFormUtils.net_var_array_size];
		for (int i=0; i<result.length; i++)
		{
			result[i] = 0;	
		}
		
		//Initialize iterators for varlist and the network_variables list
		int n=0; //index that keeps track of which list in network_variables we're on
		Iterator<FiniteVariable> vit1 = RCFormUtils.network_variables.get(n).iterator();
		Iterator<FiniteVariable> vit2 = varlist.iterator();
		
		//addend is the number to add if a variable is present. It descends down the
		//powers of two, so that when we add a variable it effectively adds a 1 to
		//the right part of the bitmap
		int addend = RCFormUtils.powers_of_two[RCFormUtils.network_variables.get(0).size()-1];
		
		//Go through the variables in varlist
		while (vit2.hasNext())
		{
			FiniteVariable v = vit2.next();
			
			//Go through the network variables looking for v. Note that we 
			//start where we found the last variable; that's okay because varlist is
			//sorted
			while (vit1.hasNext() || n < RCFormUtils.net_var_array_size-1)
			{
				if (vit1.hasNext())
				{
					//See if this is the variable we're looking for	
					if (v.equals(vit1.next()))
					{
						//Add a 1 to the bitmap at this location	
						result[n] += addend;
						//Move to the right
						addend /= 2;
						//Break out of the inner while loop so we can move on to the next variable
						break;
					}	
					
					//Move to the right (keep looking)
					addend /= 2;
				}
				
				
				else
				{
					//We've reached the end of this variable list. Go to next list in 
					//the network_variables array.
					n++;
					vit1 = RCFormUtils.network_variables.get(n).iterator();
					addend = RCFormUtils.powers_of_two[RCFormUtils.network_variables.get(n).size()-1];	
				}
			}		
		}
		
		return result;
	}
	
	/**
	 * Returns a key that uniquely identifies the reduced form of this CPT 
	 * when it has been reduced according to the given parent instantiation.
	 * The form of this key is just an ordered list of all the numbers in the CPT.
	 * 
	 * @param v A variable
	 * @param parent_inst A partial instantiation of <code>v</code>'s parents
	 *
	 * @return a list of doubles representing what <code>v</code>'s CPT looks like under <code>parent_inst</code>
	 */
	public static List<Double> reducedCPTKey(FiniteVariable v, Term parent_inst)
	{
		List<Double> result = new ArrayList<Double>();	
		List<FiniteVariable> all_parents = getParents(v);
		Map<FiniteVariable,Object> parent_map = parent_inst.getMap();
		Object csi_type = v.getProperty(CSITypeProperty.PROPERTY);
		
		//If this variable is an OR or AND node, we can shortcut the process
		//so that we don't have to look at the CPT at all
		
		if (csi_type == CSITypeProperty.OR)
		{
			//In the case of an OR node, there are just two possibilities:
			//Either we care about the rest of the variables (key=1) or
			//we don't (key=0).
			for (FiniteVariable par: all_parents)
			{
				String mapped_value = (String)(parent_map.get(par));
				if (mapped_value != null && mapped_value.equals("true"))
				{
					//One of the parents is true, so the reduced CPT is just TRUE. Return key 0
					result.add(0.0);
					return result;		
				}	
			}
			//None of the parents is true, so the reduced CPT is an OR of the remaining parents.
			//Return key 1
			result.add(1.0);
			return result;
		}
		else if (csi_type == CSITypeProperty.AND)
		{
			//In the case of an AND node, there are just two possibilities:
			//Either we care about the rest of the variables (key=1) or
			//we don't (key=0).
			for (FiniteVariable par: all_parents)
			{
				String mapped_value = (String)(parent_map.get(par));
				if (mapped_value != null && mapped_value.equals("false"))
				{
					//One of the parents is false, so the reduced CPT is just FALSE. Return key 0
					result.add(0.0);
					return result;		
				}	
			}
			//None of the parents is false, so the reduced CPT is an AND of the remaining parents.
			//Return key 1
			result.add(1.0);
			return result;
		}
		else if (csi_type == CSITypeProperty.EVEN || csi_type == CSITypeProperty.ODD)
		{
			//In the case of an EVEN or ODD parity node, there are just two possibilities:
			//Either it stays the same parity (key=1) or it changes parity (key=0).
			int true_count = 0;
			for (FiniteVariable par: all_parents)
			{
				String mapped_value = (String)(parent_map.get(par));
				if (mapped_value != null)
				{
					if (mapped_value.equals("true"))
					{
						true_count++;
					}
				}	
			}
			if (true_count%2 == 0)
			{
				result.add(1.0);	
			}
			else
			{
				result.add(0.0);	
			}
			
			return result;
			
		}
		
		else
		{
			//In the general case the key is the reduced CPT itself, which we build
			//from scratch. This is very slow.
			
			CPTShell sh = v.getCPTShell();
		
			//Find the CPT variables that aren't set by parent_inst
			List<FiniteVariable> free_variables = new ArrayList<FiniteVariable>(sh.variables());
			free_variables.removeAll(parent_inst.getActualVariables());
			Collections.sort(free_variables, new RCFormUtils.VariableNameComparator());
			
			//For each instantiation of these free variables, add the resulting probability
			//to the key.
			for (Term t: RCFormUtils.allModelsList(free_variables))
			{
				Map<FiniteVariable, Object> inst_map = new HashMap<FiniteVariable, Object>(parent_map);
				inst_map.putAll(t.getMap());		
			
				result.add(sh.getCP(inst_map));
			}		
		}
		
		return result;
	}
	
	/**
	 * Returns a key that uniquely identifies a reduced set of CPTs.
	 * The key takes the form of a list of single-CPT keys, one for each child
	 * variable. Each single-CPT key is just a list of the probabilities in the
	 * reduced CPT.
	 *
	 * @param children a list of variables
	 * @param parent_inst an instantiation of some of the parents of <code>children</code>
	 *
	 * @return A list of single-CPT keys, each of which is a list of doubles representing the CPT of one of the child variables reduced according to <code>parent_inst</code>
	 */
	public static List<List<Double>> multiReducedCPTKey(List<FiniteVariable> children, Term parent_inst)
	{
		//Build a list of the CPT keys for each of the child variables.
		List<List<Double>> result = new ArrayList<List<Double>>();
		for (FiniteVariable v: children)
		{		
			result.add(reducedCPTKey(v, parent_inst));
		}
		
		return result;	
	}
	
	/**
	 * Prints a map in a nice, readable form.
	 * Assumes that the keys and values can be printed.
	 *
	 * @param m a map
	 */
	public static void printMap(Map m)
	{
		for (Object key: m.keySet())
		{
			System.out.println(key + ": " + m.get(key));	
		}	
	}
		
	/**
	 * Returns a string representing an integer array
	 *
	 * @param arr an array of ints
	 *
	 * @return a string showing the values in the array
	 */
	public static String arrayString(int[] arr)
	{
		String s = "[";
		for (int i=0; i<arr.length; i++)
		{
			s += " " + arr[i];
		}	
		s += " ]";
		
		return s;
	}
	
	/**
	 * An array with the powers of two from 0 to 30
	 */
	public static final int[] powers_of_two = {1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608,16777216,33554432,67108864,134217728,268435456,536870912,1073741824};
	
	/**
	 * The number of array elements it takes to store bitmaps of the variables in the network.
	 * This is n / 31, where n is the number of variables in the network.
	 */
	public static int net_var_array_size;
	
	/**
	 * A flat list of the variables in the network
	 */
	public static List<FiniteVariable> all_network_vars;
	
	/**
	 * A list of the network variables, broken into 31-variable sections.
	 * This format is used to compactly represent variable sets and instantiations
	 * (as short lists of integers that represent bitmaps).
	 */
	public static List<List<FiniteVariable>> network_variables;
	
	/**
	 * Class for comparing variables (needed to sort lists of variables)
	 *
	 * @author Taylor Curtis
	 */
	public static class VariableNameComparator implements Comparator<FiniteVariable>
	{
		public int compare(FiniteVariable v1, FiniteVariable v2)
		{
			String s1 = v1.getID();
			String s2 = v2.getID();
			return s1.compareTo(s2);	
		}
	}
	
}