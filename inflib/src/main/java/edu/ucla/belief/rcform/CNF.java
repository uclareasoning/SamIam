package edu.ucla.belief.rcform;

import edu.ucla.belief.*;
import edu.ucla.structure.*;

import java.io.*;
import java.util.*;

/**
 * A formula in Conjunctive Normal Form
 *
 * @author Taylor Curtis
 */
public class CNF extends Formula
{
	/**
	 * Default constructor. Creates an empty CNF
	 */
	public CNF()
	{
		clauses = new HashSet<Clause>();
	}
	
	/**
	 * Constructor with Term argument.
	 * Note that a term is already in CNF (each literal is a clause)
	 *
	 * @param t the term to build the CNF from
	 */
	public CNF(Term t)
	{
		clauses = new HashSet<Clause>();
		for (Literal literal: t.getLiterals())
		{
			clauses.add(new Clause(literal));	
		}			
	}
	
	/**
	 * Constructor with Clause argument.
	 * Note that a clause is already in CNF (it just contains one clause)
	 *
	 * @param c the clause to build the CNF from
	 */
	public CNF(Clause c)
	{
		clauses = new HashSet<Clause>();
		clauses.add(c);			
	}
	
	/**
	 * Copy constructor
	 *
	 * @param other the CNF to copy
	 */
	public CNF(CNF other)
	{
		clauses = new HashSet(other.clauses);	
	}
	
	public boolean equals(Object obj)
	{
		if (obj == null || !(this.getClass().equals(obj.getClass())))
		{
			return false;
		}
		
		return (this.clauses.equals(((CNF)obj).clauses));
			
	}
	
	public int hashCode()
	{
		int hash = 1;
		hash = hash * 31 + clauses.hashCode();
		
		return hash;	
	}
	
	/**
	 * Returns whether this CNF is empty
	 * 
	 * @return true iff this CNF is empty
	 */
	public boolean isEmpty()
	{
		return (clauses.size() == 0);	
	}
	
	/**
	 * Checks whether this CNF is satisfiable
	 * This function is not portable, because it makes an external call
	 * to the MiniSAT program. Assumes that the MiniSAT executable is
	 * in the path.
	 *
	 * @return true iff the formula is satisfiable
	 */
	public boolean isSatisfiable() throws Exception
	{
		if (isEmpty())
		{
			//The empty conjunction is true, and therefore satisfiable
			return true;	
		}
		
		//Write the CNF to a temporary .cnf file
		
		File temp_file = File.createTempFile("tempfile", ".cnf", new File("."));
		temp_file.deleteOnExit();
		Writer writer = new BufferedWriter(new FileWriter(temp_file));
		this.writeToFile(writer, buildIndexMap());
		writer.close();
		
		//Call MiniSAT on the .cnf file
		Runtime rt = Runtime.getRuntime();
		String[] cmds = new String[2];
		cmds[0] = RCFormUtils.MINISATEXECUTABLE;
		cmds[1] = temp_file.getName(); 
		Process minisat_call = rt.exec(cmds);
		
		//Check MiniSAT output
		String line = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(minisat_call.getInputStream()));
		//Look for the word 'UNSATISFIABLE'
		while ((line = br.readLine()) != null)
		{
			if (line.indexOf("UNSATISFIABLE") != -1)
			{
				//System.out.println(this.toString() + " is unsatisfiable");
				temp_file.delete();
				return false;	
			}	
		}
		
		//If we get here, then the output from MiniSAT never said 'UNSATISFIABLE', so
		//the CNF is satisfiable.
		//System.out.println(this.toString() + " is satisfiable");
		temp_file.delete();
		return true;
	}
	
	/**
	 * Conjoins two CNFs.
	 * Removes subsumed clauses
	 *
	 * @param f1 a CNF formula
	 * @param f2 a CNF formula
	 *
	 * @return the conjunction of <code>f1</code> and <code>f2</code>
	 */	
	public static CNF conjoin(CNF f1, CNF f2)
	{
		CNF new_cnf = new CNF();
		new_cnf.clauses.addAll(f1.clauses);
		new_cnf.clauses.addAll(f2.clauses);
		new_cnf.clean();
		
		return new_cnf;
	}
	 
	/**
	 * Disjoins a CNF with a term
	 *
	 * @param f a CNF
	 * @param t a term
	 *
	 * @return the conjunction of <code>f</code> and <code>t</code>
	 */
	public static CNF disjoin(CNF f, Term t)
	{
		CNF new_cnf = new CNF();
		Clause new_clause = null;
		
		//Just make new clauses by disjoining each clause in the CNF with each
		//literal in the term (distributive property)
		for (Clause c: f.clauses)
		{
			for (Literal l: t.getLiterals())
			{
				new_clause = Clause.disjoin(c, l);
				
				new_cnf.clauses.add(new_clause);	
			}	
		}	
		new_cnf.clean();
		return new_cnf;	
	}
	
	/**
	 * Makes the CPT more presentable by removing redundant information
	 * like subsumed and trivial clauses
	 */
	public void clean()
	{
		removeSubsumedClauses();
		removeTrivialClauses();	
	}
	
	/**
	 * Removes from the CNF all clauses that are subsumed by another clause
	 * (they're redundant) 
	 */
	public void removeSubsumedClauses()
	{	
		Clause c1 = null;
		Clause c2 = null;
		
		Set<Clause> set_copy = new HashSet<Clause>(clauses);
		
		//Go through the clauses
		for (Iterator<Clause> cit1 = clauses.iterator(); cit1.hasNext(); )
		{
			c1 = cit1.next();
			//Compare this clause to each other clause
			for (Iterator<Clause> cit2 = set_copy.iterator(); cit2.hasNext(); )
			{
				c2 = cit2.next();
				
				if (c1 != c2)
				{
					//If this clause is subsumed by another clause, remove it
					if (c2.subsumes(c1))
					{
						cit1.remove();
						set_copy.remove(c1);
						break;
					}
				}	
			}				
		}
	}
	
	/**
	 * Removes from the CNF all clauses that are tautologous (always true)
	 */
	public void removeTrivialClauses()
	{
		Map<FiniteVariable, Integer> var_counts = null;
		Clause c = null;
		FiniteVariable var = null;
		int count = 0;
		
		//Examine each clause and remove it if it's trivial
		for (Iterator<Clause> cit = clauses.iterator(); cit.hasNext();)
		{
			c = cit.next();
			
			var_counts = new HashMap<FiniteVariable, Integer>();
			
			//Examine each literal in the clause, keeping track of how many times
			//each variable appears. If this number is equal to the variable's size
			//(number of possible values), then any instantiation will satisfy this
			//clause, so the clause is trivial.
			for (Literal l: c.getLiterals())
			{
				var = l.getVariable();
				
				if (!var_counts.containsKey(var))
				{
					var_counts.put(var, 1);
				}
				else
				{
					count = var_counts.get(var);
					if (count+1 >= var.size())
					{
						//All of this variable's values appear in this clause, so the clause is
						//trivial. Remove it.
						cit.remove();	
						
					}
					else
					{
						var_counts.put(var, var_counts.get(var)+1);	
					}
				}
			}				
		}	
	}
	
	/*
	//Assumes binary variables
	public static CNF forget(CNF f, Set<FiniteVariable> vars)
	{
		CNF old_f = null;
		Literal first_value = null;
		Literal second_value = null;
		for (FiniteVariable v: vars)
		{
			//Forget v
			first_value = new Literal(v, 0);
			second_value = new Literal(v, 1);
			old_f = f;
			f = CNF.disjoin(CNF.condition(f, first_value), CNF.condition(f, second_value));
		}
		
		return f;	
	}
	*/
	
	public static CNF condition(CNF f, Literal l)
	{
		//Go through each clause. If the literal appears, remove the clause.
		//Remove any conflicting literals that appear
		
		CNF result = new CNF(f);
		Clause c = null;
		Literal l2 = null;
		for (Iterator<Clause> cit = result.clauses.iterator(); cit.hasNext();)
		{
			c = cit.next();
			for (Iterator<Literal> lit = c.getLiterals().iterator(); lit.hasNext();)
			{
				l2 = lit.next();
				if (l.equals(l2))
				{
					cit.remove();	
				}
				else if (l.sameVariable(l2))
				{
					lit.remove();		
				}
			}	
		}
		
		result.clean();
		
		return result;	
	}
	
	/**
	 * Conditions this CNF on the given literal
	 *
	 * @param value a literal
	 */
	public void condition(Literal value)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Sums out a collection of variables from the CNF
	 *
	 * @param vars the variables to be summed out
	 */
	public void forget(Collection vars)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Converts this CNF to string representation
	 * Looks like "(<clause 1> & <clause 2> & <clause 3>)"
	 *
	 * @return a string representation of this CNF
	 */
	public String toString()
	{
		String s = new String("");
		s += "(";
		
		//Append first clause
		Iterator<Clause> cit = clauses.iterator();
		if (cit.hasNext())
		{
			s += cit.next();	
		}
		//Append remaining clauses
		while (cit.hasNext())
		{
			s += " & " + cit.next();	
		}	
		s += ")";
		
		return s;
	}
	
	public Set<Clause> getClauses()
	{
		return clauses;	
	}
	
	public Clause getOnlyClause()
	{
		assert (clauses.size() == 1);
		
		Iterator<Clause> cit = clauses.iterator();
		return cit.next();	
	}
	
	public Set<FiniteVariable> getVariables()
	{
		Set<FiniteVariable> varset = new HashSet<FiniteVariable>();
		for (Clause c: clauses)
		{
			for (FiniteVariable v: c.getVariables())
			{
				varset.add(v);	
			}	
		}	
		return varset;
	}
	
	/**
	 * Writes this CNF to a file in .cnf format, so that it can be read by MiniSAT
	 * Assumes that all variables are binary (since MiniSAT does)
	 *
	 * @param writer a Writer hooked to the file to be written to
	 */
	public void writeToFile(Writer writer, MappedList index_map) throws Exception
	{
		//Print initial comment line
		writer.write("c p cnf #vars #clauses\n");
		
		//Print comment lines with information about variables
		for (int i=0; i<index_map.size(); i++)
		{
			writer.write("c " + i + " " + ((FiniteVariable)(index_map.get(i))).getID() + "\n");	
		}
			
		//Print initial CNF spec line
		writer.write("p cnf " + index_map.size() + " " + clauses.size() + "\n");
					
		//Print clauses, one per line
		int val_index = 0;
		FiniteVariable var = null;
		for (Clause c: clauses)
		{
			//Write this clause
			for (Literal l: c.getLiterals())
			{
				var = l.getVariable();
				val_index = var.index(l.getValue());
				
				if (val_index == 0)
				{
					//For positive literals, just write the variable index
					writer.write((index_map.indexOf(var)+1) + " ");	
				}
				else if (val_index == 1)
				{
					//For negative literals, put a minus sign before the variable index
					writer.write("-" + (index_map.indexOf(var)+1) + " ");
				}
				else
				{
					assert (false) : ("Variable is not binary");	
				}
			}
			//End every line with a '0'		
			writer.write("0\n");
		}
		writer.write("\n");
	}
	
	//Build a map from variable names to integers
	public MappedList buildIndexMap()
	{
		MappedList index_map = new MappedList();
		//Just go through the clauses and find all the variables
		for (Clause c: clauses)
		{
			for (Literal l: c.getLiterals())
			{
				if (!index_map.contains(l.getVariable()))
				{
					index_map.add(l.getVariable());
				}
			}
		}
		
		return index_map;
	}
	
	/**
	 * The clauses that comprise the CNF.
	 * This formula is the conjunction of these clauses.
	 */
	private Set<Clause> clauses;
}