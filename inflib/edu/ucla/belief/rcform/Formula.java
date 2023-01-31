package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 * Class that represents a formula over a particular set of variables
 * Internal representation is just a list of terms
 *
 * @author Taylor Curtis
 */
public class Formula
{
	/**
	 * Constructor with varbits argument
	 */
	public Formula(int[] varbits)
	{
		var_bits = varbits;
		models = new ArrayList<Term>();	
	}
	
	/**
	 * Constructor with list of terms argument.
	 */
	public Formula(List<Term> terms)
	{
		models = new ArrayList<Term>();
		
		if (terms.size() > 0)
		{
			var_bits = terms.get(0).getVarBits();	
		
			for (Term t: terms)
			{
				//Make sure all terms have the same var bits
				assert (Arrays.equals(t.getVarBits(), var_bits)) : "Can't construct Formula: Not all terms over same variables"; 
				
				//Add term to formula
				models.add(t);
			}
			
			//Sort the list
			Collections.sort(terms, new Term.TermComparator());
		}	
	}
	
	/**
	 * Gets the list of models that satisfy this formula
	 *
	 * @return the list of models that satisfy this formula
	 */
	public List<Term> getModels()
	{
		return models;
	}

	/**
	 * Gets the number of models that satisfy this formula
	 *
	 * @return the number of models that satisfy this formula
	 */
	public int getSize()
	{
		return models.size();	
	}
	
	/**
	 * Returns whether this is an empty formula
	 *
	 * @return true iff this formula includes no terms
	 */
	public boolean isEmpty()
	{
		return (models.size() == 0);
	}	
	
	/**
	 * Adds a term to the formula
	 *
	 * @param t a term
	 */
	public void addTerm(Term t)
	{
		models.add(t);	
		Collections.sort(models, new Term.TermComparator());
	}
	
	/**
	 * Static method that projects a formula onto a set of variables
	 *
	 * @param f a formula
	 * @param proj_var_bits a bitmap representing a set of variables
	 *
	 * @return the formula that results from project <code>f</code> onto the variable set represented by <code>proj_var_bits</code>
	 */
	public static Formula project(Formula f, int[] proj_var_bits)
	{	
		Set<Term> model_set = new HashSet<Term>();
		
		//Project each model onto proj_var_bits, and add to the new set of models
		for (Term model: f.models)
		{
			model_set.add(Term.project(model, proj_var_bits));	
		}
		
		//Build the new formula from the set of projected terms
		Formula result = new Formula(new ArrayList<Term>(model_set));
		
		return result;
	}
	
	public boolean isConsistentWith(Term t)
	{
		for (Term m: models)
		{
			if (!m.contradicts(t))
			{
				return true;	
			}	
		}	
		return false;
	}
	
	/**
	 * Gets the variable bitmap for this formula
	 *
	 * @return this formula's variable bitmap
	 */
	public int[] getVarBits()
	{
		return var_bits;
	}
	
	/**
	 * Gets the list of variables that this formula is over
	 *
	 * @return a list of this formula's variables
	 */
	public List<FiniteVariable> getActualVariables()
	{
		return getFirstTerm().getActualVariables();
	}
	
	/**
	 * Gets the first term in the list of terms that satisfies this formula
	 *
	 * @return the first term in the list of terms that satisfies this formula
	 */
	public Term getFirstTerm()
	{
		return models.get(0);	
	}
	
	/**
	 * Overridden equals method
	 *
	 * @param obj a formula
	 *
	 * @return true iff <code>obj</code> "equals" this formula
	 */
	public boolean equals(Object obj)
	{
		//Make sure obj is the right kind of object
		if (obj == null || !(this.getClass().equals(obj.getClass())))
		{
			return false;
		}	
		
		//The two formulas are equal if they have the same list of models
		boolean result = models.equals(((Formula)obj).models);
		
		return result;
	}
	
	/**
	 * Overridden hashCode method
	 *
	 * @return an integer guaranteed to be the same for all "equal" formulas
	 */
	public int hashCode()
	{
		int hash = 1;
		hash = hash * 31 + models.hashCode();
		
		return hash;		
	}

	/**
	 * Overridden toString method
	 *
	 * @return a string representation of the formula
	 */
	public String toString()
	{
		return models.toString();	
	}	

	/**
	 * Bitmap representing the variables in this formula
	 */
	private int[] var_bits;
	
	/**
	 * The models that satisfy the formula.
	 * This list is kept sorted.
	 */
	private List<Term> models; 
}