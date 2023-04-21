package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 * A term (conjunction of literals).
 * Terms are mainly used by this program for storing an instantiation of a set
 * of variables. Terms are internally represented as two bitmaps, one denoting
 * which of the network's variables are in the term and the other denoting 
 * which variables are true. Each bitmap is stored as an array of integers,
 * which when laid out side by side in binary would form the bitmap.
 *
 * This is an immutable class.
 * 
 * @author Taylor Curtis
 */
public final class Term
{
	/**
	 * Default constructor.
	 * Creates an empty term.
	 */
	public Term()
	{
		var_bits = new int[RCFormUtils.net_var_array_size];
		term_bits = new int[RCFormUtils.net_var_array_size];
	
		//Create an empty term	
		for (int i=0; i<var_bits.length; i++)
		{
			var_bits[i] = 0;
			term_bits[i] = 0;	
		}
		
		value_map = new HashMap<FiniteVariable,Object>();
	}
	
	/**
	 * Constructor with varbits and termbits arguments.
	 */
	public Term(int[] varbits, int[] termbits)
	{
		var_bits = varbits;
		term_bits = termbits;	
				
		/*** Build the value map ***/
		
		value_map = new HashMap<FiniteVariable,Object>();
		
		//Go through the varbits bitmap, selecting the variables that have 1s
		for (int n=0; n<RCFormUtils.net_var_array_size; n++)
		{
			List<FiniteVariable> cur_slice = RCFormUtils.network_variables.get(n);
			int num_vars = cur_slice.size();
			
			//The mask variable is used to examine each bit of the varbits bitmap.
			//It starts with the leftmost (highest-order) bit and moves right.
			int mask = RCFormUtils.powers_of_two[num_vars-1]; 
			for (int i=0; i<num_vars; i++)
			{
				if ((var_bits[n] & mask) > 0)
				{
					//This variable is in the term
					
					FiniteVariable temp_var = cur_slice.get(i);
					
					if ((termbits[n] & mask) > 0)
					{
						//Positive literal
						value_map.put(temp_var, temp_var.instance(1));	
					}
					else
					{
						//Negative literal
						value_map.put(temp_var, temp_var.instance(0));	
					}			
				}	
			
				//Move right in the bitmap
				mask /= 2;
			}
		}		
	}
	
	/**
	 * Constructor with all three arguments
	 */
	public Term(int[] varbits, int[] termbits, Map<FiniteVariable,Object> vmap)
	{	
		var_bits = varbits;
		term_bits = termbits;	
		value_map = vmap;
	}
		
	/**
	 * Copy constructor
	 */
	public Term(Term other)
	{
		var_bits = other.var_bits;
		term_bits = other.term_bits;	
		value_map = new HashMap<FiniteVariable,Object>(other.value_map);
	}
	
	/**
	 * Returns whether this term is empty
	 *
	 * @return true iff this term is empty
	 */
	public final boolean isEmpty()
	{
		//Check if any variables are in this term
		for (int i=0; i<RCFormUtils.net_var_array_size; i++)
		{
			if (var_bits[i] > 0)
			{
				return false;	
			}	
		}
		return true;
	}	
	
	/**
	 * Overridden equals method
	 *
	 * @param obj Another Term
	 *
	 * @return true iff this object is "equal" to <code>obj</code>
	 */
	public boolean equals(Object obj)
	{	
		//Make sure obj is a Term
		if (obj == null || !(this.getClass().equals(obj.getClass())))
		{
			return false;
		}
	
		//The two Terms are equal iff their varbits and termbits are the same		
		boolean result = (Arrays.equals(this.term_bits,((Term)obj).term_bits) && Arrays.equals(this.var_bits,((Term)obj).var_bits)); 
		
		return result;		
	}
	
	/**
	 * Overridden hashCode method
	 *
	 * @return a hashcode for this Term that is guaranteed to be the same for all equal Terms
	 */
	public int hashCode()
	{		
		int hash = 1;
		hash = hash * 31 + Arrays.hashCode(var_bits);
		hash = hash * 31 + Arrays.hashCode(term_bits);
		
		return hash;	
	}
	
	/**
	 * Checks whether this term contradicts the given term
	 *
	 * @param t a Term
	 *
	 * @return true iff this term contradicts <code>t</code>
	 */
	public boolean contradicts(Term t)
	{
		//Check each segment of the bitmaps
		for (int i=0; i<RCFormUtils.net_var_array_size; i++)
		{
			//Build a bitmap of shared variables
			int shared_vars = this.var_bits[i] & t.var_bits[i];
			
			//Now see if the two terms disagree on the values of the shared variables
			if ((shared_vars & this.term_bits[i]) != (shared_vars & t.term_bits[i]))
			{
				return true;	
			}
		}
		return false;
	}
	
	/**
	 * Getter for variable bitmap
	 *
	 * @return the bitmap that denotes the set of variables in the term
	 */	
	public final int[] getVarBits()
	{
		return var_bits;	
	}
	
	/**
	 * Static method that conjoins two Terms.
	 * Returns <code>null</code> if the two terms contradict each other.
	 *
	 * @param t1 a term
	 * @param t2 a term
	 *
	 * @return the term that is the conjunction of <code>t1</code> and <code>t2</code>
	 */
	public static Term conjoin(Term t1, Term t2)
	{	
		if (t1 == null)
		{
			return new Term(t2);	
		}
		if (t2 == null)
		{
			return new Term(t1);	
		}
		
		//Return null if the two terms contradict
		if (t1.contradicts(t2))
		{
			return null;	
		}
		
		int new_varbits[] = new int[RCFormUtils.net_var_array_size];
		int new_termbits[] = new int[RCFormUtils.net_var_array_size];
				
		for (int i=0; i<RCFormUtils.net_var_array_size; i++)
		{
			//The new variable set is the union of the two variable sets
			new_varbits[i] = t1.var_bits[i] | t2.var_bits[i];
			
			//The new set of positive literals is the union of the two sets
			new_termbits[i] = t1.term_bits[i] | t2.term_bits[i];	
		}

		//Add the two mappings
		Map<FiniteVariable,Object> new_value_map = new HashMap<FiniteVariable,Object>(t1.value_map);
		new_value_map.putAll(t2.value_map);
		
		Term result = new Term(new_varbits, new_termbits, new_value_map);
		
		return result;
	}
	
	/**
	 * Static method that projects a Term onto a set of variables
	 *
	 * @param t a term
	 * @param vars a list of variables
	 *
	 * @return the term that results from projecting <code>t</code> onto <code>vars</code>
	 */
	public static Term project(Term t, List<FiniteVariable> vars)
	{
		//Find the shared variables between t and vars (we're not assuming that vars
		//is a subset of t's variables)
		List<FiniteVariable> shared_vars = new ArrayList<FiniteVariable>(t.getActualVariables());
		shared_vars.retainAll(vars);
		
		//Sort the shared vars
		Collections.sort(shared_vars, new RCFormUtils.VariableNameComparator());
		
		//Project t onto the shared vars
		Term result = project(t,RCFormUtils.computeVarBits(shared_vars));
		
		return result;	
	}
	
	/**
	 * Static method that projects a Term onto a set of variables
	 *
	 * @param t a term
	 * @param proj_var_bits a bitmap representing a set of variables
	 *
	 * @return the term that results from projecting <code>t</code> onto the variables represented by <code>proj_var_bits</code>
	 */
	public static Term project(Term t, int[] proj_var_bits)
	{		
		int[] new_term_bits = new int[RCFormUtils.net_var_array_size];
		
		//Go through the bitmaps
		for (int i=0; i<RCFormUtils.net_var_array_size; i++)
		{
			//Make sure the variable is not in proj_var_bits and not t's var_bits
			if ((proj_var_bits[i] & (t.var_bits[i] ^ proj_var_bits[i])) > 0)
			{
				assert false;	
			}
			
			//Copy the term_bit for this variable to new_term_bits iff
			//the variable is in proj_var_bits
			new_term_bits[i] = t.term_bits[i] & proj_var_bits[i];
		}
	
		Term result = new Term(proj_var_bits, new_term_bits);
		
		return result;
	}
	
	/**
	 * Checks if this term sets all its variables to true
	 *
	 * @return true iff all literals in the term are positive
	 */
	public boolean allTrue()
	{
		//Just check the values in the value map to see if any are false
		for (Object value: value_map.values())
		{
			String s = (String)value;
			if (s.equals("false"))
			{
				return false;
			}		
		}
		return true;
	}
	
	/**
	 * Checks if this term sets all its variables to false
	 *
	 * @return true iff all literals in the term are negative
	 */
	public boolean allFalse()
	{
		//Just check the values in the value map to see if any are true
		for (Object value: value_map.values())
		{
			String s = (String)value;
			if (s.equals("true"))
			{
				return false;
			}		
		}
		return true;
	}
	
	/**
	 * Checks if this term sets an even number of variables to true
	 *
	 * @return true iff an even number of literals are true
	 */
	public boolean evenTrue()
	{
		int true_count = 0;
		for (Object value: value_map.values())
		{
			String s = (String)value;
			if (s.equals("true"))
			{
				true_count++;	
			}	
		}	
		return (true_count % 2 == 0);
	}
	
	/**
	 * Gets a list of the variables in this term
	 *
	 * @return a list of the variables in this term
	 */
	public List<FiniteVariable> getActualVariables()
	{
		return new ArrayList<FiniteVariable>(value_map.keySet());
	}
	
	/**
	 * Gets a map version of the term.
	 * Variables are mapped to their values.
	 *
	 * @return a map that maps variables to the values they are assigned by this term
	 */
	public Map<FiniteVariable,Object> getMap()
	{
		return value_map;	
	}
	
	/**
	 * Converts this term to a string representation
	 * Looks like "(literal1 & literal2 & literal3)"
	 *
	 * @return a string representation of this term
	 */
	public String toString()
	{		
		if (value_map.size() == 0)
		{
			return "()";		
		}	
		
		String s = new String("");
		
		s += "(";
		
		for (FiniteVariable v: value_map.keySet())
		{
			s += v + " = "  + value_map.get(v) + " & ";	
		}
		
		//Remove the last " & "
		s = s.substring(0, s.length()-3) + ")";

		return s;
	}
	
	/**
	 * Static class for comparing two terms.
	 * This is only meant for comparing terms with the same varbits.
	 *
	 * @author Taylor Curtis
	 */
	public static class TermComparator implements Comparator<Term>
	{
		/**
		 * Comparison method for terms with the same var_bits
		 */
		public int compare(Term t1, Term t2)
		{	
			//Make sure var_bits are the same
			assert (Arrays.equals(t1.var_bits, t2.var_bits)): "Comparing terms with different var_bits";
			
			for (int i=0; i<RCFormUtils.net_var_array_size; i++)
			{	
				if (t1.term_bits[i] < t2.term_bits[i]) return -1;
				if (t2.term_bits[i] > t2.term_bits[i]) return 1;
			}	
			return 0;
		}
	}
	

	/**
	 * A number encoding the set of variables that this term includes. Each bit is
	 * 1 if that variable appears in the term, 0 otherwise. Broken into groups
	 * of 31 bits
	 */
	private final int[] var_bits;
	
	/**
	 * A number encoding the term. Each bit is a 1 if the variable is a positive
	 * literal, 0 otherwise. Broken into groups of 32 bits
	 */
	private final int[] term_bits;
	
	/**
	 * A map from the variables in the term to their values. Basically an
	 * alternate representation of the Term that is sometimes useful
	 */
	private final Map<FiniteVariable,Object> value_map;
}
