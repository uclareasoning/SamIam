package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 * A clause (disjunction of literals).
 * This is an immutable class.
 *
 * @author Taylor Curtis
 */
public class Clause extends Formula
{
	/**
	 * Default constructor
	 */
	public Clause()
	{
		literals = new HashSet<Literal>();
	}
	
	
	/**
	 * Constructor with literal argument.
	 * Creates a new clause with a single literal.
	 *
	 * @param l a literal
	 */
	public Clause(Literal l)
	{
		literals = new HashSet<Literal>();
		literals.add(l);	
	}
	
	/**
	 * Constructor with list of Literals parameter
	 *
	 * @param literal_list a list of literals
	 *
	 */
	public Clause(Set<Literal> literal_list)
	{
		literals = new HashSet<Literal>(literal_list);	
	}
	
	/**
	 * Creates a new clause whose literals result
	 * from setting all of a given set of variables to
	 * the same given value.
	 *
	 * @param vars a set of variables
	 * @param val_name the name of the value to set all variables to
	 */
	public Clause(Collection<FiniteVariable> vars, String val_name)
	{
		literals = new HashSet<Literal>(); 
		for (FiniteVariable var: vars)
		{
			literals.add(new Literal(var, val_name));	
		}
	}
	
	
	public boolean equals(Object obj)
	{
		if (obj == null || !(this.getClass().equals(obj.getClass())))
		{
			return false;
		}
		
		return (this.literals.equals(((Clause)obj).literals));
			
	}
	
	public int hashCode()
	{
		int hash = 1;
		hash = hash * 31 + literals.hashCode();
		
		return hash;	
	}
	
	/**
	 * Returns whether this clause subsumes the given clause.
	 * A clause subsumes another iff its literals are a subset of the other one's
	 * literals (in other words, if its set of worlds is a subset of the other's)
	 *
	 * @param c the clause to check for subsumption
	 * @return true iff this clause subsumes <code>c</code>
	 */
	public boolean subsumes(Clause c)
	{
		if (this.literals.size() > c.literals.size())
		{
			return false;	
		}
		
		boolean found = false;
		//For each literal in this clause...
		for (Literal l: this.literals)
		{
			//See if it appears in the other clause
			found = false;
			for (Literal cl: c.literals)
			{
				if (l.equals(cl))
				{
					found = true;
					break;	
				}
			}	
			if (!found)
			{
				//This clause contains a literal that does not appear in
				//the other clause, so it does not subsume it.
				return false;	
			}
		}
		
		//If we make it here, then every literal in this clause appears in
		//the other clause. So this one subsumes the other one.
		return true;	
	}
	
	/**
	 * Returns whether the clause is empty
	 *
	 * @return true iff the clause contains no literals
	 */
	public boolean isEmpty()
	{
		return literals.isEmpty();
	}	
	
	/**
	 * Returns the clause's list of literals
	 *
	 * @return the literals that comprise the clause
	 */
	public final Set<Literal> getLiterals()
	{
		return literals;	
	}
	
	public final Literal[] getLiteralArray()
	{
		Literal[] specifier = new Literal[0];
		return literals.toArray(specifier);	
	}
	
	public final Set<FiniteVariable> getVariables()
	{
		Set<FiniteVariable> varset = new HashSet<FiniteVariable>();
		for (Literal l: literals)
		{
			varset.add(l.getVariable());	
		}	
		return varset;
	}
	
	/**
	 * Checks whether this clause is satisfiable
	 *
	 * @return true iff the clause is satisfiable
	 */
	public boolean isSatisfiable()
	{
		//The empty clause is false, hence unsatisfiable
		if (isEmpty())
		{
			return false;
		}
		
		//All non-empty clauses are satisfiable
		return true;
	}
	
	/**
	 * Conjoins a formula to this one
	 *
	 * @param f another logical formula
	 */	
	public void conjoin(Formula f)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Disjoins a clause and a literal
	 *
	 * @param c a clause
	 * @param l a literal
	 * @return the disjunction of <code>c</code> and <code>l</code>
	 */
	public static Clause disjoin(Clause c, Literal l)
	{
		Set<Literal> literal_set = new HashSet(c.literals);
	
		for (Literal l2: literal_set)
		{
			//If the literal already appears in the clause, don't add it.
			if (l.equals(l2))
			{
				return new Clause(literal_set);	
			}	
		}
		
		literal_set.add(l);
		return new Clause(literal_set);
	}
	
	/**
	 * Conditions this formula on the given literal
	 *
	 * @param value a literal
	 */
	public void condition(Literal value)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Sums out a collection of variables from the formula
	 *
	 * @param vars the variables to be summed out
	 */
	public void forget(Collection vars)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Converts this clause to a string representation
	 * Looks like "(<literal 1> | <literal 2> | <literal 3>)"
	 *
	 * @return a string representation of this clause
	 */
	public String toString()
	{
		String s = new String("");
		s += "(";
		
		//Append first literal
		Iterator<Literal> lit = literals.iterator();
		if (lit.hasNext())
		{
			s += lit.next();	
		}
		//Append remaining literals
		while (lit.hasNext())
		{
			s += " | " + lit.next();	
		}	
		s += ")";
		
		return s;
	}
	
	/**
	 * The literals comprising the clause.
	 * The formula is the disjunction of these literals.
	 */
	private final Set<Literal> literals;

}