package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 * a formula in deterministic Disjunctive Normal Form.
 * DNF means a disjunction of terms. 'Deterministic' means each
 * pair of disjuncts is incompatible: Only one disjunct can be true at once
 *
 * @author Taylor Curtis
 */
public class DeterministicDNF extends Formula
{
	/**
	 * Default constructor
	 */
	public DeterministicDNF()
	{
		terms = new HashSet<Term>();
	}
	
	public DeterministicDNF(Literal l)
	{
		terms = new HashSet<Term>();
		terms.add(new Term(l));	
	}
	
	public DeterministicDNF(Term t)
	{
		terms = new HashSet<Term>();
		terms.add(new Term(t));	
	}
	
	//Assumes binary variables	
	public DeterministicDNF(Clause c)
	{
		terms = new HashSet<Term>();
			
		Literal l1 = null;
		Literal l2 = null;
		Literal[] c_literals = c.getLiteralArray();
		Set<Literal> new_literal_set = null;
		for (int i=0; i<c_literals.length; i++)
		{
			l1 = c_literals[i];
			new_literal_set = new HashSet<Literal>();
			for (int j=0; j<i; j++)
			{
				l2 = c_literals[j];
				new_literal_set.add(l2.negate());				
			}
			new_literal_set.add(l1);
			
			terms.add(new Term(new_literal_set));
		}
	}
	
	public DeterministicDNF(DeterministicDNF other)
	{
		assert (other != null);
		terms = new HashSet<Term>();
		for (Term t: other.terms)
		{
			terms.add(new Term(t));	
		}	
	}
	
	public boolean equals(Object obj)
	{
		if (obj == null || !(this.getClass().equals(obj.getClass())))
		{
			return false;
		}
		
		return (this.terms.equals(((DeterministicDNF)obj).terms));
			
	}
	
	public int hashCode()
	{
		int hash = 1;
		hash = hash * 31 + terms.hashCode();
		
		return hash;	
	}
	
	public static DeterministicDNF convertCNF(CNF f) throws Exception
	{
		//c2d will crash if zero or one clauses, so do these cases manually
		if (f.getClauses().size() == 0)
		{
			return new DeterministicDNF();
		}
		else if (f.getClauses().size() == 1)
		{
			return new DeterministicDNF(f.getOnlyClause());
		}
		
		DDNNF intermediate = DDNNF.convertCNF(f);
		
		//System.out.println("intermediate = " + intermediate);
		
		return DeterministicDNF.convertDDNNF(intermediate);	
	}
	
	private static DeterministicDNF convertDDNNF(DDNNF ddnnf)
	{
		DeterministicDNF result = new DeterministicDNF();
		
		if (ddnnf.isLeaf())
		{
			result = new DeterministicDNF(ddnnf.getLiteral());	
		}	
		else if (ddnnf.isOr())
		{
			ArrayList<DDNNF> or_children = ddnnf.getChildren();
			for (DDNNF child: or_children)
			{
				result = DeterministicDNF.disjoin(result, convertDDNNF(child));	
			}
		}
		else
		{
			assert (ddnnf.isAnd());
			
			ArrayList<DDNNF> and_children = ddnnf.getChildren();
			for (DDNNF child: and_children)
			{
				result = DeterministicDNF.conjoin(result, convertDDNNF(child));	
			}
		}
		
		return result;
	}
	
	/**
	 * Checks whether this formula is satisfiable
	 *
	 * @return true iff the formula is satisfiable
	 */
	public boolean isSatisfiable()
	{
		if (terms.isEmpty()) 
		{
			return false;
		}
		return true;
	}
	
	public static DeterministicDNF conditionOnCNF(DeterministicDNF dnf, CNF cnf)
	{
		DeterministicDNF result = new DeterministicDNF();
		Term cur_term = null;
		
		for (Iterator<Term> termit = dnf.terms.iterator(); termit.hasNext();)
		{	
			cur_term = termit.next();
			
			if (cur_term.entails(cnf))
			{
				result.terms.add(cur_term);	
			}
		}
		
		return result;	
	}
	
	//Assumes binary variables
	public static DeterministicDNF forget(DeterministicDNF f, Set<FiniteVariable> vars)
	{
		DeterministicDNF old_f = null;
		DeterministicDNF new_f = new DeterministicDNF(f);
		Literal first_value = null;
		Literal second_value = null;
		for (FiniteVariable v: vars)
		{
			//Forget v
			first_value = new Literal(v, 0);
			second_value = new Literal(v, 1);
			old_f = new_f;
			new_f = DeterministicDNF.disjoin(DeterministicDNF.condition(old_f, first_value), DeterministicDNF.condition(old_f, second_value));
		}
		
		return new_f;	
	}
	
	public static DeterministicDNF disjoin(DeterministicDNF f, Term t)
	{
		DeterministicDNF result = new DeterministicDNF(f);
		result.terms.add(t);
		return result;
	}
	
	public static DeterministicDNF disjoin(DeterministicDNF f1, DeterministicDNF f2)
	{
		
		
		DeterministicDNF new_f = new DeterministicDNF();
		new_f.terms.addAll(f1.terms);
		new_f.terms.addAll(f2.terms);
		new_f.clean();
		
		/*
		System.out.println("disjunct f1 = " + f1);
		System.out.println("disjunct f2 = " + f2);
		System.out.println("disjunction = " + new_f);
		*/
		
		return new_f;
	}
	
	//Assumes f1 and f2 talk about different variables
	public static DeterministicDNF conjoin(DeterministicDNF f1, DeterministicDNF f2)
	{
		if (f1.isEmpty())
		{
			return new DeterministicDNF(f2);		
		}
		if (f2.isEmpty())
		{
			return new DeterministicDNF(f1);	
		}
		
		DeterministicDNF new_f = new DeterministicDNF();
		for (Term t1: f1.terms)
		{
			for (Term t2: f2.terms)
			{
				new_f.terms.add(Term.conjoin(t1, t2));	
			}	
		}	
			
		/*
		System.out.println("conjunct f1 = " + f1);
		System.out.println("conjunct f2 = " + f2);
		System.out.println("conjunction = " + new_f);
		*/
			
		return new_f;
	}
	
	public Set<Term> getTerms()
	{
		return terms;	
	}
	
	public void clean()
	{
		removeSubsumedTerms();	
	}
	
	public void removeSubsumedTerms()
	{
		//throw new UnsupportedOperationException();
		
		
		Term t1 = null;
		Term t2 = null;
		
		//Set<Term> set_copy = new HashSet<Term>(terms);
		
		//Go through the terms
		for (Iterator<Term> termit = terms.iterator(); termit.hasNext(); )
		{
			t1 = termit.next();
			//Compare this term to each other term
			for (Iterator<Term> termit2 = terms.iterator(); termit2.hasNext();)
			{
				t2 = termit2.next();
				
				if (t1 != t2)
				{
					//System.out.println(t2 + " subsumes " + t1 + "?");
					//If this term is subsumed by another term, remove it
					if (t2.subsumes(t1))
					{
						//System.out.println("Yes");
						termit.remove();
						break;
					}
				}
			}				
		}	
		
	}
	
	public static DeterministicDNF condition(DeterministicDNF f, Term t)
	{
		DeterministicDNF result = new DeterministicDNF(f);
		
		//System.out.println("Conditioning " + f + " on " + t);
		
		for (Literal l: t.getLiterals())
		{
			result = DeterministicDNF.condition(result, l);	
		}	
		
		return result;
	}
	
	public static DeterministicDNF condition(DeterministicDNF f, Literal l)
	{
		DeterministicDNF result = new DeterministicDNF();
		Term new_t = null;
		Literal l2 = null;
		boolean bad = false;
		for (Term t: f.terms)
		{
			bad = false;
			new_t = new Term(t);
			//System.out.println("new_t = " + new_t);
			for (Iterator<Literal> lit = new_t.getLiterals().iterator(); lit.hasNext();)
			{
				l2 = lit.next();
				if (l.equals(l2))
				{
					lit.remove();	
				}
				else if (l.sameVariable(l2))
				{
					bad = true;
					break;		
				}
			}	
			//System.out.println("Now new_t = " + new_t);
			if (!bad)
			{
				result = DeterministicDNF.disjoin(result, new_t);	
			}
		}
		
		//result.clean();
		
		return result;	
	}
	
	public Term getOneTerm()
	{
		Iterator<Term> termit = terms.iterator();
		if (!termit.hasNext())
		{
			return null;
		}
		return termit.next();
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
	 * Disjoins a formula to this one
	 *
	 * @param f another logical formula
	 */
	public void disjoin(Formula f)
	{
		throw new UnsupportedOperationException();
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
	
	public boolean isEmpty()
	{
		return terms.isEmpty();	
	}
	
	public String toString()
	{
		String s = new String("");
		s += "(";
		
		//Append first term
		Iterator<Term> termit = terms.iterator();
		if (termit.hasNext())
		{
			s += termit.next();	
		}
		//Append remaining terms
		while (termit.hasNext())
		{
			s += " | " + termit.next();	
		}	
		s += ")";
		
		return s;
	}
	
	/**
	 * The terms that comprise the deterministic DNF.
	 * The formula is the disjunction of these terms.
	 *
	 */
	private Set<Term> terms;

}