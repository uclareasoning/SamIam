package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 * A literal (variable with assigned value).
 * This class basically just wraps around a VariableInstance.
 * This class is immutable.
 * 
 * @author Taylor Curtis
 */
public class Literal
{
	/**
	 * Constructor with VariableInstance parameter
	 *
	 * @param v a VariableInstance
	 */
	/*
	public Literal(VariableInstance v)
	{
		//Should I defensive copy? The instance might not be immutable...
		variable = v.getVariable();
		value = v.getInstance();
	}
	*/
	
	/**
	 * Constructor with variable and value parameters
	 * 
	 * @param var a variable
	 * @param val_name the name of a value for <code>var</code>
	 */
	public Literal(FiniteVariable var, String val_name)
	{
		variable = var; //Should I defensive copy?
		value_index = var.index(var.instance(val_name));
	}
	
	public Literal(FiniteVariable var, int val_index)
	{
		variable = var;
		value_index = val_index;
	}
	
	/**
	 * Returns whether the given literal is equal to this literal.
	 * Equality means they have the same variable and value.
	 *
	 * @param l the literal to compare with
	 * @return true iff the two literals are equal
	 */
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj.getClass().equals(this.getClass())))
		{
			return false;
		}
		return (this.sameVariable((Literal)obj) && this.sameValue((Literal)obj));
		
	}
	
	public int hashCode()
	{
		int hash = 1;
		hash = hash * 31 + variable.getID().hashCode();	
		hash = hash * 31 + value_index; //Maybe instead do index of value?
		
		return hash;
	}
	
	public boolean sameVariable(Literal l)
	{
		return variable.getID().equals(l.variable.getID());
	}
	
	public boolean sameValue(Literal l)
	{
		//System.out.println("Comparing " + value + " with " + l.value);
		boolean result = (value_index == l.value_index);
		//System.out.println("Result = " + result);
		return result;
	}
	
	//Assumes binary variable
	public Literal negate()
	{
		assert (variable.size() == 2);
		
		if (value_index == 0)
		{
			return new Literal(variable, 1);	
		}
		return new Literal(variable, 0);
	}
	
	/**
	 * Returns whether the given literal is consistent with this literal.
	 * Two literals are consistent unless they assign different values to the same
	 * variable.
	 *
	 * @param l the literal to compare with
	 * @return true iff the two literals are consistent
	 */
	 
	public boolean isConsistentWith(Literal l)
	{
		return (!this.sameVariable(l)) || (this.sameValue(l));	
	}
	
	
	/**
	 * Returns this literal's variable
	 *
	 * @return this literal's variable
	 */
	public FiniteVariable getVariable()
	{
		return variable;	
	}
	
	/**
	 * Returns the name of this literal's variable
	 *
	 * @return the name of this literal's variable
	 */
	public String getVariableName()
	{
		return variable.getID();	
	}
	
	/**
	 * Returns this literal's value
	 *
	 * @return this literal's value
	 */
	public Object getValue()
	{
		return variable.instance(value_index);	
	}
	
	public int getValueIndex()
	{
		return value_index;	
	}
	
	/**
	 * Converts this literal to a string representation.
	 * Looks like "<name> = <value>"\
	 *
	 * @return a string representation of this literal.
	 */
	public String toString()
	{
		return variable.getID() + " = " + variable.instance(value_index);	
	}
	
	public static class NameComparator implements Comparator<Literal>
	{
		public int compare(Literal l1, Literal l2)
		{
			String s1 = l1.getVariableName();
			String s2 = l2.getVariableName();
			return s1.compareTo(s2);
		}
	}
	
	/**
	 * The variable
	 */
	private final FiniteVariable variable;
	
	/**
	 * The value
	 */
	private final int value_index; 
}