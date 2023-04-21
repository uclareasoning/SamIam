package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 * An internal node in an RCFormDtree.
 *
 * @author Taylor Curtis
 */
public class RCFormDtreeNodeInternal extends RCFormDtreeNode
{
	/*** METHODS REQUIRED FOR PRE-PROCESSING AND INFERENCE ***/
	
	/**
	 * Constructs new internal node with given id
	 *
	 * @param new_id the id number for this new node
	 */
	public RCFormDtreeNodeInternal(int new_id)
	{
		super(new_id);	
		
		alpha_cache = new HashMap<Term,Map<Formula,Double>>();
		beta_cache = new HashMap<Term,Map<Formula,Double>>();
	}
	
	/**
	 * Sets the parent references for all nodes beneath and including this one.
	 *
	 * @param p the parent node
	 */
	public void setParents(RCFormDtreeNode p)
	{
		super.setParents(p);
		
		left.setParents(this);
		right.setParents(this);	
	}
	
	/**
	 * Sets this node's left child to the given node
	 *
	 * @param lchild the node that will be the left child
	 */
	public void setLeftChild(RCFormDtreeNode lchild)
	{
		left = lchild;
	}
	
	/**
	 * Sets this node's right child to the given node
	 *
	 * @param rchild the node that will be the right child
	 */
	public void setRightChild(RCFormDtreeNode rchild)
	{
		right = rchild;
	}
	
	/**
	 * Gets the left child of this node
	 *
	 * @return the left child of this node
	 */
	public RCFormDtreeNode getLeft()
	{
		return left;	
	}
	
	/**
	 * Gets the right child of this node
	 *
	 * @return the right child of this node
	 */
	public RCFormDtreeNode getRight()
	{
		return right;	
	}
	
	/**
	 * Builds the two-level partition and exemplar map for this node
	 * and all nodes beneath it.
	 */
	public void buildPartitions()
	{
		super.buildPartitions();
		
		left.buildPartitions();
		right.buildPartitions();	
	}
	
	
	/**
	 * Returns whether this node is a leaf node
	 *
	 * @return true iff this node is a leaf node
	 */
	public boolean isLeaf()
	{
		return false;	
	}
	
	/**
	 * Returns the variable associated with this node, if it's a leaf node.
	 * Unsupported if it's an internal node.
	 *
	 * @return a reference to the finite variable corresponding to this leaf node
	 */
	public FiniteVariable getLeafVar()
	{
		throw new UnsupportedOperationException();	
	}
	
	/**
	 * Returns the number of nodes beneath (and including) this one in the dtree.
	 *
	 * @return the number of nodes beneath and including this one
	 */
	public int numNodes()
	{
		return 1 + left.numNodes() + right.numNodes();	
	}
	
	/**
	 * Recursively builds exogenous-vars, endogenous-vars, vars, left-cutset, 
	 * right-cutset, left-cutset-children, right-cutset-children, 
	 * outer-context-children, and parent-context. Sets are built 
	 * from the leaves up to the root
	 */
	public void buildVarSets()
	{
		assert (left != null) : "Left child null";
		assert (right != null) : "Right child null";
		
		//First build the var sets for all nodes beneath this one
		left.buildVarSets();
		right.buildVarSets();
		
		if (isRoot())
		{
			outer_context_children = new ArrayList<FiniteVariable>();
			sibling_context_children = new ArrayList<FiniteVariable>();	
			sibling_context = new ArrayList<FiniteVariable>();
		}
		
		//ENDOGENOUS VARS: Take the union of the left and right endogenous vars
		endogenous_vars = new ArrayList<FiniteVariable>(left.endogenous_vars);
		endogenous_vars.addAll(right.endogenous_vars);
		Collections.sort(endogenous_vars, new RCFormUtils.VariableNameComparator());	
		
		//EXOGENOUS VARS: Take the union of the left and right exogenous vars, minus the endogenous vars
		Set<FiniteVariable> exo_set = new HashSet<FiniteVariable>(left.exogenous_vars);
		exo_set.addAll(right.exogenous_vars);
		exo_set.removeAll(endogenous_vars);
		exogenous_vars = new ArrayList<FiniteVariable>(exo_set);
		Collections.sort(exogenous_vars, new RCFormUtils.VariableNameComparator());	
		
		//VARS: Exogenous plus endogenous
		vars = new ArrayList<FiniteVariable>(exogenous_vars);
		vars.addAll(endogenous_vars);
		Collections.sort(vars, new RCFormUtils.VariableNameComparator());
		
		//RIGHT CHILD'S SIBLING CONTEXT
		right.sibling_context = new ArrayList<FiniteVariable>(right.exogenous_vars);
		right.sibling_context.removeAll(exogenous_vars);
		Collections.sort(right.sibling_context, new RCFormUtils.VariableNameComparator());
		
		//LEFT CHILD'S SIBLING CONTEXT
		left.sibling_context = new ArrayList<FiniteVariable>(left.exogenous_vars);
		left.sibling_context.removeAll(exogenous_vars);
		Collections.sort(left.sibling_context, new RCFormUtils.VariableNameComparator());
		
		//RIGHT CHILD'S PARENT CONTEXT: Right exogenous vars intersect exogenous vars
		right.parent_context = new ArrayList<FiniteVariable>(right.exogenous_vars);
		right.parent_context.retainAll(exogenous_vars);
		Collections.sort(right.parent_context, new RCFormUtils.VariableNameComparator());
		
		//LEFT CHILD'S PARENT CONTEXT: Left exogenous vars intersect exogenous vars
		left.parent_context = new ArrayList<FiniteVariable>(left.exogenous_vars);
		left.parent_context.retainAll(exogenous_vars);
		Collections.sort(left.parent_context, new RCFormUtils.VariableNameComparator());
		
		//SIBLING-INNER-CONTEXTS: Just mirror the SIBLING-OUTER-CONTEXTS
		right.sibling_inner_context = new ArrayList<FiniteVariable>(left.sibling_context);
		left.sibling_inner_context = new ArrayList<FiniteVariable>(right.sibling_context);

		//Find the left and right nodes' sibling-context-children and outer-context-children
		left.sibling_context_children = new ArrayList<FiniteVariable>();
		right.sibling_context_children = new ArrayList<FiniteVariable>();
		left.outer_context_children = new ArrayList<FiniteVariable>();
		right.outer_context_children = new ArrayList<FiniteVariable>();
		
		//Examine each endogenous variable to see if it has outer-context and/or
		//sibling-outer-context parents
		for (FiniteVariable v: right.getEndogenousVars())
		{
			List<FiniteVariable> shared_vars = RCFormUtils.getParents(v);
			shared_vars.retainAll(right.getExogenousVars());
			if (!shared_vars.isEmpty())
			{
				right.outer_context_children.add(v);	
			
				shared_vars.retainAll(right.sibling_context);
				if (!shared_vars.isEmpty())
				{
					right.sibling_context_children.add(v);	
				}
			}
			
		}
		
		//Examine each endogenous variable to see if it has outer-context and/or
		//sibling-outer-context parents
		for (FiniteVariable v: left.getEndogenousVars())
		{
			List<FiniteVariable> shared_vars = RCFormUtils.getParents(v);
			shared_vars.retainAll(left.getExogenousVars());
			if (!shared_vars.isEmpty())
			{
				left.outer_context_children.add(v);	
			
				shared_vars.retainAll(left.sibling_context);
				if (!shared_vars.isEmpty())
				{
					left.sibling_context_children.add(v);	
				}
			}
		}
		
		//Update largest outer-context and sibling-outer-context sizes
		RCFormStats.max_outer_size = Math.max(RCFormStats.max_outer_size, exogenous_vars.size());
		RCFormStats.max_sibling_outer_size = Math.max(RCFormStats.max_sibling_outer_size, Math.max(left.sibling_context.size(),right.sibling_context.size()));	
		
		//System.out.println("endogenous(" + id + "): " + endogenous_vars);
		//System.out.println("exogenous(" + id + "): " + exogenous_vars);
		
	}
	
	/**
	 * Recursively computes all nodes' inner contexts. This can't be done 
	 * in the buildVarSets function because it requires information about the parent's
	 * sets, not the children's. 
	 */
	public void computeInnerContexts()
	{
		//System.out.println("Computing inner context for node " + id);
		
		super.computeInnerContexts();
		
		//Recurse
		left.computeInnerContexts();
		right.computeInnerContexts();
	}
	
	/**
	 * Initializes this node and all nodes beneath it.
	 * RCFormTreeNodeInternal overrides this function to make it recursive.
	 */
	public void initializeNodes()
	{
		super.initializeNodes();
		
		left.initializeNodes();
		right.initializeNodes();
	}
	
	/*** METHODS FOR PROVIDING OUTPUT AND DEBUGGING INFO ***/
	
	/** 
	 * Recursively computes the dtree's treewidth
	 *
	 * @return the maximum treewidth of any node below and including this one
	 */
	public int computeTreewidth()
	{
		//Treewidth is outer-context plus inner-context plus right-cutset plus left-cutset.
		int tw = exogenous_vars.size() + inner_context.size() + left.sibling_context.size() + right.sibling_context.size();
		
		return Math.max(tw, Math.max(left.computeTreewidth(), right.computeTreewidth()));
	}
	
	/** 
	 * Recursively computes the dtree's parametric treewidth
	 *
	 * @return the maximum parametric treewidth of any node below and including this one
	 */
	public void computeParametricTreewidth()
	{
		super.computeParametricTreewidth();
		
		left.computeParametricTreewidth();
		right.computeParametricTreewidth();	
	}
	
	public void computeNewParametricTreewidth()
	{
		super.computeNewParametricTreewidth();
		
		left.computeNewParametricTreewidth();
		right.computeNewParametricTreewidth();	
	}
	
	/**
	 * Checks the theoretical number of calls to this node against the actual
	 * number.
	 */
	public void checkNodeCalls(boolean extra_caching)
	{
		super.checkNodeCalls(extra_caching);
		
		left.checkNodeCalls(extra_caching);
		right.checkNodeCalls(extra_caching);
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
			int predicted_alpha_size = left.predictedExtraCacheSize();
			int predicted_beta_size = right.predictedExtraCacheSize();	
			int actual_alpha_size = node_stats.num_alpha_cache_entries;
			int actual_beta_size = node_stats.num_beta_cache_entries;
			
			if (predicted_alpha_size != actual_alpha_size)
			{
				System.out.println("Discrepancy!");
				System.out.println("Node " + getID() + " predicted alpha-cache size: " + predicted_alpha_size);
				System.out.println("Node " + getID() + " actual alpha-cache size: " + actual_alpha_size);
				
				System.out.println("alpha_cache:");
				for (Term t: alpha_cache.keySet())
				{
					for (Formula m: alpha_cache.get(t).keySet())
					{
						System.out.println("(" + t + " | " + m + ")");	
					}		
				}	
			}	
			
			if (predicted_beta_size != actual_beta_size)
			{
				System.out.println("Discrepancy!");
				System.out.println("Node " + getID() + " predicted beta-cache size: " + predicted_beta_size);
				System.out.println("Node " + getID() + " actual beta-cache size: " + actual_beta_size);
				
				System.out.println("beta_cache:");
				for (Term t: beta_cache.keySet())
				{
					for (Formula m: beta_cache.get(t).keySet())
					{
						System.out.println("(" + t + " | " + m + ")");	
					}		
				}	
			}	
		}	
		
		left.checkCacheSizes(extra_caching);
		right.checkCacheSizes(extra_caching);
		
	}
	
	/**
	 * Returns a string representation of the dtree below this node.
	 * Because this is an internal node, this consists of this node's id
	 * followed by the string representations of the left and right subtrees.
	 *
	 * @param hyphens A string of hyphens to proceed all lines of the string
	 * (this represents the depth of the node, which is sent down from higher in
	 * the dtree).
	 * @return a string representation of the dtree below this node
	 */
	public String treeString(String hyphens)
	{
		String new_hyphens = hyphens + "-";
		return id + 
			   "\n" + new_hyphens + left.treeString(new_hyphens) +
			   "\n" + new_hyphens + right.treeString(new_hyphens);	
	}
	
	/*** DATA MEMBERS ***/
	
	/**
	 * The left child of this node.
	 */
	private RCFormDtreeNode left;
	
	/**
	 * The right child of this node.
	 */
	private RCFormDtreeNode right;
}