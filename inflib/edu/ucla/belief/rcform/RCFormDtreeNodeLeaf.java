package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 * A leaf node in an RCFormDtree.
 *
 * @author Taylor Curtis
 */
public class RCFormDtreeNodeLeaf extends RCFormDtreeNode
{
	/*** METHODS REQUIRED FOR PRE-PROCESSING AND INFERENCE ***/
	
	/**
	 * Constructs a new leaf node with the given id and corresponding to the given
	 * variable.
	 *
	 * @param new_id the id number for this new node
	 * @param new_var the network variable that this new node corresponds to
	 */ 
	public RCFormDtreeNodeLeaf(int new_id, FiniteVariable new_var)
	{
		super(new_id);
		var = new_var;
	}
	
	/**
	 * Gets the left child of this node
	 * Unsupported for leaf nodes
	 *
	 * @return the left child of this node
	 */
	public RCFormDtreeNode getLeft()
	{
		throw new UnsupportedOperationException();	
	}
	
	/**
	 * Gets the right child of this node
	 * Unsupported for leaf nodes
	 *
	 * @return the right child of this node
	 */
	public RCFormDtreeNode getRight()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Returns whether this node is a leaf node
	 *
	 * @return true iff this node is a leaf node
	 */
	public boolean isLeaf()
	{
		return true;	
	}
	
	/**
	 * Returns the variable associated with this node, if it's a leaf node.
	 *
	 * @return a reference to the finite variable corresponding to this leaf node
	 */
	public FiniteVariable getLeafVar()
	{
		return var;	
	}
	
	/**
	 * Returns the number of nodes beneath (and including) this one in the dtree.
	 *
	 * @return the number of nodes beneath and including this one
	 */
	public int numNodes()
	{
		return 1;
	}
	
	/**
	 * Recursively builds exogenous-vars, endogenous-vars, vars, left-cutset, 
	 * right-cutset, left-cutset-children, right-cutset-children, 
	 * outer-context-children, and parent-context. Sets are built 
	 * from the leaves up to the root.
	 */
	public void buildVarSets()
	{
		//Just the one endogenous variable
		endogenous_vars = new ArrayList<FiniteVariable>(1);
		endogenous_vars.add(var);	
		
		//The exogenous variables are just the leaf var's parents
		exogenous_vars = RCFormUtils.getParents(var);
		Collections.sort(exogenous_vars, new RCFormUtils.VariableNameComparator());
			
		//Vars = exogenous plus endogenous	
		vars = new ArrayList<FiniteVariable>(exogenous_vars);
		vars.add(var);
		Collections.sort(vars, new RCFormUtils.VariableNameComparator());
		
		//Update largest outer-context and sibling-outer-context sizes
		RCFormStats.max_outer_size = Math.max(RCFormStats.max_outer_size, exogenous_vars.size());
		
		/*
		System.out.println("endogenous(" + id + "): " + endogenous_vars);
		System.out.println("exogenous(" + id + "): " + exogenous_vars);
		*/
		
	}
	
	/*** METHODS FOR PROVIDING OUTPUT AND DEBUGGING INFO ***/
	
	/** 
	 * Recursively computes the dtree's treewidth
	 *
	 * @return the maximum treewidth of any node below and including this one
	 */
	public int computeTreewidth()
	{
		//Treewidth is outer-context plus inner-context (leaf nodes have no cutset)
		int tw = exogenous_vars.size() + inner_context.size();
		
		return tw;
	}
	
	/**
	 * Checks the theoretical cache sizes of this node against the actual
	 * sizes.
	 */
	public void checkCacheSizes(boolean extra_caching)
	{
		super.checkCacheSizes(extra_caching);
		
		if (extra_caching)
		{
			int predicted_alpha_size = 0;
			int predicted_beta_size = 0;	
			int actual_alpha_size = node_stats.num_alpha_cache_entries;
			int actual_beta_size = node_stats.num_beta_cache_entries;
			
			if (predicted_alpha_size != actual_alpha_size)
			{
				System.out.println("Node " + getID() + " predicted alpha-cache size: " + predicted_alpha_size);
				System.out.println("Node " + getID() + " actual alpha-cache size: " + actual_alpha_size);
			}	
			
			if (predicted_beta_size != actual_beta_size)
			{
				System.out.println("Node " + getID() + " predicted beta-cache size: " + predicted_beta_size);
				System.out.println("Node " + getID() + " actual beta-cache size: " + actual_beta_size);
			}	
		}	
		
	}
	
	
	/**
	 * Returns a string representation of the dtree below this node.
	 * Because this is a leaf node, this is just the variable
	 * name associated with this node.
	 *
	 * @param hyphens A string of hyphens to proceed all lines of the string
	 * (this represents the depth of the node, which is sent down from higher in
	 * the dtree).
	 * @return a string representation of the dtree below this node
	 */
	public String treeString(String hyphens)
	{
		return id + " " + var.getID();
	}
	
	/*** DATA MEMBERS ***/
	
	/**
	 * The variable whose CPT corresponds to this leaf node.
	 */
	private FiniteVariable var;
}