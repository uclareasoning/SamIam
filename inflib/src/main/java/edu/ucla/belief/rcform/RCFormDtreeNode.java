package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.util.*;

/**
 * A node in an RCFormDtree.
 *
 * @author Taylor Curtis
 */
public abstract class RCFormDtreeNode
{
	/*** METHODS REQUIRED FOR PRE-PROCESSING AND INFERENCE ***/
	
	/**
	 * Constructs new node with given id
	 *
	 * @param new_id the id number for this new node
	 */
	public RCFormDtreeNode(int new_id)
	{
		id = new_id;
	}
	
	/**
	 * Returns this node's ID
	 *
	 * @return the ID for this node
	 */
	public int getID()
	{
		return id;	
	}
	
	/**
	 * Returns a reference to this node's parent
	 *
	 * @return this node's parent
	 */
	public RCFormDtreeNode getParent()
	{
		return parent;	
	}
		 
	/**
	 * Sets the parent references for all nodes beneath and including this one.
	 * RCFormDtreeNodeInternal overrides this function to make it recursive.
	 *
	 * @param p the parent node
	 */
	public void setParents(RCFormDtreeNode p)
	{
		assert (p == null || !p.isLeaf());
		
		parent = (RCFormDtreeNodeInternal)p;	
	}
	
	/**
	 * Returns whether this node is the root node of the dtree.
	 *
	 * @return true iff this node is the root of the dtree
	 */
	public boolean isRoot()
	{
		return (parent == null);	
	}
	
	/**
	 * Returns a reference to this node's left child.
	 * Only supported for internal nodes
	 *
	 * @return a reference to the left child
	 */
	public abstract RCFormDtreeNode getLeft();
	
	
	/**
	 * Returns a reference to this node's left child.
	 * Only supported for internal nodes
	 *
	 * @return a reference to the right child
	 */
	public abstract RCFormDtreeNode getRight();
	
	/**
	 * Returns whether this node is a leaf node
	 *
	 * @return true iff this node is a leaf node
	 */
	public abstract boolean isLeaf();
	
	/**
	 * Returns the variable associated with this node, if it's a leaf node.
	 * Unsupported if it's an internal node.
	 *
	 * @return a reference to the finite variable corresponding to this leaf node
	 */
	public abstract FiniteVariable getLeafVar();
	
	/**
	 * Returns the number of nodes beneath (and including) this one in the dtree.
	 *
	 * @return the number of nodes beneath and including this one
	 */
	public abstract int numNodes();
	
	/**
	 * Recursively builds exogenous-vars, endogenous-vars, vars, left-cutset, 
	 * right-cutset, left-cutset-children, right-cutset-children, 
	 * outer-context-children, and parent-context. Sets are built 
	 * from the leaves up to the root.
	 */
	public abstract void buildVarSets();
	
	/**
	 * Recursively computes all nodes' inner contexts. This can't be done 
	 * in the buildVarSets function because it requires information about the parent's
	 * sets, not the children's. 
	 * RCFormDtreeNodeInternal overrides this function to make it recursive.
	 */
	public void computeInnerContexts()
	{
		//System.out.println("Computing inner context for node " + id);
		
		if (isRoot())
		{
			inner_context = new ArrayList<FiniteVariable>();	
		}
		else
		{
			//Inner context is the intersection of the parent's inner context with this
			//node's endogenous variables, plus the variables that point to the sibling's
			//endogenous variables (AKA the sibling-inner-context).
			Set<FiniteVariable> ic_set = new HashSet<FiniteVariable>(parent.inner_context);
			ic_set.retainAll(endogenous_vars);
			ic_set.addAll(sibling_inner_context);
			
			inner_context = new ArrayList<FiniteVariable>(ic_set);
		}	
		
		//Update largest inner-context size
		RCFormStats.max_inner_size = Math.max(RCFormStats.max_inner_size, inner_context.size());
	}
	
	/**
	 * Builds the two-level partition and exemplar map for this node
	 * and all nodes beneath it.
	 * RCFormDtreeNodeInternal overrides this to make it recursive.
	 */
	public void buildPartitions()
	{
		if (this.isRoot())
		{
			//Root node has one empty exemplar
			exemplars = new HashMap<List<List<Double>>,Term>();
			exemplars.put(new ArrayList<List<Double>>(),new Term());
			
			outer_model_map = new HashMap<Term,List<List<Double>>>();
			outer_model_map.put(new Term(), new ArrayList<List<Double>>());
			
			//Root node has no outer-context, so partition is basically empty
			two_level_partition = new HashMap<Term,Partition>();
			two_level_partition.put(new Term(), null);
		}
		else //Not a root node
		{
			//Initialize exemplars and two-level-partition
			exemplars = new HashMap<List<List<Double>>,Term>();
			outer_model_map = new HashMap<Term,List<List<Double>>>();
			two_level_partition = new HashMap<Term,Partition>();
		
			//Find the set of endogenous variables that have parents in the 
			//parent-outer-context but not in the sibling-outer-context.
			List<FiniteVariable> remaining_context_children = new ArrayList<FiniteVariable>(outer_context_children);
			remaining_context_children.removeAll(sibling_context_children);		
			
			//Consider each of the parent node's exemplars. Projecting these exemplars
			//onto the parent-outer-context will give the first level of the two-level
			//partition. Then we just have to calculate the second-level partition for
			//each bucket of the first level, and identify this node's exemplars as part
			//of that process.
			for (Term t: parent.exemplars.values())
			{	
				//Project the exemplar onto the parent-outer-context
				Term projected_t = Term.project(t, RCFormUtils.computeVarBits(parent_context));
				
				//If we have already seen this instance, move on to the next one.
				if (two_level_partition.containsKey(projected_t))
				{
					continue;	
				}
				else //New element of the first-level partition
				{
					//Initialize second-level partition
					Map<List<List<Double>>, Formula> second_part = new HashMap<List<List<Double>>, Formula>();
					
					//To build the second-level partition, examine each instantiation of
					//the sibling-outer-context. Assign it to the bucket corresponding
					//to the reduced set of CPTs it yields.
					for (Term s: RCFormUtils.allModelsList(sibling_context))
					{
						//Get the entire outer context instantiation by conjoining the
						//parent-outer-context instantiation with the sibling-outer-context
						//instantiation.
						Term outer_instance = Term.conjoin(projected_t, s);
						
						//Compute reduced CPT keys. key_sibling represents the set of reduced
						//CPTs for the sibling-outer-context-children, while key_outer represents
						//the set of reduced CPTS for the outer-context-children. We use key_outer
						//for building the exemplar map, and key_sibling for build the second-level
						//partition.
						List<List<Double>> key_sibling = RCFormUtils.multiReducedCPTKey(sibling_context_children, outer_instance);
						
						//We can build key_outer by just adding to key_sibling the information 
						//about the reduced CPTS that are in outer-context-children by not in
						//sibling-outer-context-children.
						List<List<Double>> key_outer = new ArrayList<List<Double>>(key_sibling);
						key_outer.addAll(RCFormUtils.multiReducedCPTKey(remaining_context_children, outer_instance));
						
						//Add this sibling-outer-context instantiation to the appropriate bucket 
						//in the second-level partition (the key determines the bucket).
						Formula cur_formula = second_part.get(key_sibling);
						if (cur_formula == null)
						{
							cur_formula = new Formula(s.getVarBits());	
						}
						cur_formula.addTerm(s);
						second_part.put(key_sibling, cur_formula); 
						
						//If this key has no exemplar, make this instance the exemplar.
						if (!exemplars.containsKey(key_outer))
						{
							exemplars.put(key_outer, outer_instance);	
						}
						//Add this <instance,key> pair to the outer model map
						outer_model_map.put(outer_instance,key_outer);
					}		
					
					//Finally, add the second-level partition to appropriate bucket of the 
					//two-level partition.
					two_level_partition.put(projected_t,new Partition(second_part.values()));
				}
			}	
			
			
			
			
			//Print exemplars
			/*
			System.out.println("Exemplars(" + id + "):");
			for (Term e: exemplars.values())
			{
				System.out.println("\t" + e);	
			}
			*/
						
			//Print two level partition
			/*
			System.out.println("Partition(" + id + "):");
			for (Term t: two_level_partition.keySet())
			{
				System.out.println("\t" + t);
				for (Formula m: two_level_partition.get(t).getParts())
				{
					System.out.println("\t\t" + m);	
				}	
			}
			*/
			
		}
		
		
		int partsize = exemplars.keySet().size();
		
		if (partsize > RCFormStats.max_partition_size) {
			RCFormStats.max_partition_size = partsize;
		}	
			
	}	
	
	/**
	 * Returns the exemplar for the given instantiation of the outer-context
	 *
	 * @param t an outer-context instantiation
	 * @return the exemplar for the given instantiation
	 */
	public Term getExemplar(Term t)
	{
		return exemplars.get(outer_model_map.get(t));
	}
		
	/**
	 * Returns the second-level partition that is paired with the given instantiation
	 *
	 * @param t an instantiation of the parent-outer-context that identifies a bucket of the first-level partition
	 * @return the second-level partition that is paired with <code>t</code>
	 */
	public Partition getPartition(Term t)
	{			
		if (!two_level_partition.containsKey(t))
		{
			System.err.println("Part map does not contain term:");
			RCFormUtils.printMap(two_level_partition);
			System.err.println("term = " + t);
			
			assert(false);	
		}
		
		return two_level_partition.get(t);
	}
	
	/**
	 * Gets the node's endogenous variables 
	 *
	 * @return a list of the node's endogenous variables
	 */
	public List<FiniteVariable> getEndogenousVars()
	{
		return endogenous_vars;	
	}
	
	/**
	 * Gets the node's endogenous variables 
	 *
	 * @return a list of the node's endogenous variables
	 */
	public List<FiniteVariable> getExogenousVars()
	{
		return exogenous_vars;	
	}

	/**
	 * Initializes this node and all nodes beneath it.
	 * RCFormTreeNodeInternal overrides this function to make it recursive.
	 */
	public void initializeNodes()
	{
		cache = new HashMap<Term, Map<Term, Double>>();	
		node_stats = new RCFormStats.NodeStats();
	}
	
	/*** METHODS FOR PROVIDING OUTPUT AND DEBUGGING INFO ***/
	
	/** 
	 * Recursively computes the dtree's treewidth
	 *
	 * @return the maximum treewidth of any node below and including this one
	 */
	public abstract int computeTreewidth();
	
	/** 
	 * Recursively computes the dtree's parametric treewidth
	 *
	 * @return the maximum parametric treewidth of any node below and including this one
	 */
	public void computeParametricTreewidth()
	{
		double ptw = 0;
		
		Set<FiniteVariable> evidence_vars = new HashSet<FiniteVariable>();
		
		
		if (isRoot())
		{
			ptw = 1;
		}		
		else
		{
			/*** Compute old ptw ***/
			
			
		//The first term is the size of the union of the parent's inner-context and this
		//node's sibling-inner-context, minus the sibling-outer-context.
		evidence_vars.addAll(parent.inner_context);
		evidence_vars.removeAll(sibling_context);
		evidence_vars.addAll(sibling_inner_context);
		ptw = evidence_vars.size();
		
		//The second term is the log of the sum, over all of the parent node's 
		//exemplars, of the size of the second-level partition that corresponds 
		//to that exemplar.
		
		List<FiniteVariable> instance_vars = new ArrayList<FiniteVariable>(sibling_context);
		instance_vars.retainAll(parent.inner_context);
		
		int sum = 0;
		for (Term instance: RCFormUtils.allModelsList(instance_vars))
		{
			for (Term t: parent.exemplars.values())
			{
				Partition p = two_level_partition.get(Term.project(t,parent_context));
				
				for (Formula part: p.getParts())
				{
					if (part.isConsistentWith(instance))
					{
						sum++;
					}
				}
					
			}
		}
		
		/*
		System.out.println("parent's inner-context size = " + parent.inner_context.size());
		System.out.println("sibling-inner-context size = " + sibling_inner_context.size());
		System.out.println("sum = " + sum);
		*/
		
		
		//Add the log of the sum
		ptw += (Math.log(sum) / Math.log(2));
		}
	
		//Update best ptw
		if (ptw > RCFormStats.max_ptw)
		{
			RCFormStats.max_ptw = ptw;
			RCFormStats.max_ptw_num_evidence_vars = evidence_vars.size();
		}
}	

	public void computeNewParametricTreewidth()
	{
		double ptw = 0;
		
		int num_evidence_vars = 0;
		
		if (isRoot()) {
			ptw = 1;	
		}
		else {
		
			/*** Compute new ptw ***/
		
			Set<FiniteVariable> inner_cutset = new HashSet<FiniteVariable>(sibling_inner_context);
			inner_cutset.removeAll(parent.inner_context);
		
			num_evidence_vars = parent.inner_context.size() + inner_cutset.size();
			
			ptw = num_evidence_vars;
			ptw += Math.log(parent.exemplars.size())/Math.log(2);
		
			//Build outer cutset partition
			List<FiniteVariable> outer_cutset = new ArrayList<FiniteVariable>(sibling_context);
			outer_cutset.removeAll(parent.inner_context);
			
			Partition outer_cutset_partition = new Partition(outer_cutset, sibling_context_children);
			
			ptw += (Math.log(outer_cutset_partition.numParts())/Math.log(2));
		}
		
		if (ptw > RCFormStats.max_newptw)
		{
			RCFormStats.max_newptw = ptw;
			RCFormStats.max_newptw_num_evidence_vars = num_evidence_vars;
		}
	}
	
	/**
	 * Test function that computes the theoretical number of times that this node
	 * will be called by rcform (so that it can be compared with the actual 
	 * number, as a sanity check)
	 */
	public int predictedNumCalls(boolean extra_caching)
	{
		System.out.println("Predicting number of calls to " + id);
		
		if (isRoot())
		{
			return 1;	
		}
		
		int result;
		
		if (extra_caching)
		{
			
			Set<FiniteVariable> evidence_vars = new HashSet<FiniteVariable>(inner_context);
			evidence_vars.removeAll(sibling_inner_context);
		
			//System.out.println("node " + id + " inner context = " + inner_context);
			//System.out.println("node " + id + " sibling inner context = " + sibling_inner_context);
			
			result = RCFormUtils.powers_of_two[evidence_vars.size()];
			
			//Find the sibling
			RCFormDtreeNode sibling = null;
			if (parent.getRight() == this)
			{
				sibling = parent.getLeft();	
			}
			else
			{
				sibling = parent.getRight();	
			}
			
			int sum = 0;

			//Make a list of all the unique <alpha,beta> pairs that can occur
			Map<Term,Set<List<Formula>>> big_map = new HashMap<Term,Set<List<Formula>>>();
			Set<List<Formula>> unique_pairs = null;
			for (Term t: parent.exemplars.values())
			{
				Term t_sibling = Term.project(t, sibling.exogenous_vars);
				Term t_this = Term.project(t, this.exogenous_vars);
				
				if (big_map.containsKey(t_this))
				{
					unique_pairs = big_map.get(t_this);	
				}
				else
				{
					unique_pairs = new HashSet<List<Formula>>();	
				}
								
				for (Formula n: this.two_level_partition.get(t_this).getParts())
				{			
					for (Formula m: sibling.two_level_partition.get(t_sibling).getParts())
					{
						List<Formula> next_list = new ArrayList<Formula>();
						next_list.add(n);
						next_list.add(m);
				
						if (!unique_pairs.contains(next_list))
						{
							sum += m.getSize();	
							unique_pairs.add(next_list);
						}
					}
				}	
				
				big_map.put(t_this,unique_pairs);
			
				//System.out.println("node " + id + ": t_this = " + t_this + ", unique pairs: " + unique_pairs);
			}
			
			
			
			
			result *= sum;
			
			
		}
		
		else //no extra caching
		{
			
			Set<FiniteVariable> evidence_vars = new HashSet<FiniteVariable>(parent.inner_context);
			evidence_vars.removeAll(sibling_context);
			evidence_vars.addAll(sibling_inner_context);
		
			result = RCFormUtils.powers_of_two[evidence_vars.size()];
		
			//System.out.println("Evidence vars = " + evidence_vars);
			//System.out.println("Num evidence vars (" + id + ") = " + evidence_vars.size());
			
			//See if we can get this part just from formula partitions
			
			//Build the parent's outer context partition
			Partition p1 = new Partition(parent.exogenous_vars, parent.outer_context_children);
			
			//Build this node's three-level outer context partition
			
			//System.out.println("Partitions for " + id);
			
			List<FiniteVariable> instantiated_context = new ArrayList<FiniteVariable>(sibling_context);
			List<FiniteVariable> uninstantiated_context = new ArrayList<FiniteVariable>(sibling_context);
			instantiated_context.retainAll(parent.inner_context);
			uninstantiated_context.removeAll(parent.inner_context);
			
			int sum = 0;
			for (Term instance: RCFormUtils.allModelsList(instantiated_context))
			{
				for (Formula m: p1.getParts())
				{
					Term exemplar = m.getFirstTerm();
					exemplar = Term.project(exemplar, parent_context);
				
					Partition p2 = new Partition(uninstantiated_context, outer_context_children, Term.conjoin(exemplar,instance));
				
					//System.out.println("Partition(" + instance + ", " + m + ") = " + p2);
				
					sum += p2.numParts();
				}
			
				
					
			}
			//System.out.println("result = " + result);
			result *= sum;
		}
	
		return result;
	}
	
	/**
	 * Test function that predicts how many entries this node's cache will
	 * have after rcform is run. This can be compared with the actual cache
	 * size to confirm the theoretical measure.
	 */
	public int predictedCacheSize()
	{
		int ocp_size = exemplars.values().size();
		int exp_ic = RCFormUtils.powers_of_two[inner_context.size()];
		
		//System.out.println("ocp_size(" + id + ") = " + ocp_size);
		//System.out.println("exp_ic(" + id + ") = " + exp_ic);
		//System.out.println("inner context is " + inner_context);
		
		int result = ocp_size * exp_ic;
		
		return result;	
	}
	
	/**
	 * Returns what the size should be of this node's parent's extra cache that pertains 
	 * to this node (alpha-cache for a left child, beta-cache for a right child). For
	 * testing purposes.
	 */
	public int predictedExtraCacheSize()
	{
		Set<FiniteVariable> evidence_vars = new HashSet<FiniteVariable>(parent.inner_context);
		evidence_vars.retainAll(inner_context);
		
		//System.out.println("node " + id + " inner context = " + inner_context);
		//System.out.println("node " + id + " sibling inner context = " + sibling_inner_context);
			
		//System.out.println("evidence_vars = " + evidence_vars);
		int result = RCFormUtils.powers_of_two[evidence_vars.size()];
		
		//Find the sibling
		RCFormDtreeNode sibling = null;
		if (parent.getRight() == this)
		{
			sibling = parent.getLeft();	
		}
		else
		{
			sibling = parent.getRight();	
		}
		
		int count = 0;

		//Make a list of all the unique <alpha,beta> pairs that can occur
		Map<Term,Set<List<Formula>>> big_map = new HashMap<Term,Set<List<Formula>>>();
		Set<List<Formula>> unique_pairs = null;
		for (Term t: parent.exemplars.values())
		{
			Term t_sibling = Term.project(t, sibling.exogenous_vars);
			Term t_this = Term.project(t, this.exogenous_vars);
			
			if (big_map.containsKey(t_this))
			{
				unique_pairs = big_map.get(t_this);	
			}
			else
			{
				unique_pairs = new HashSet<List<Formula>>();	
			}
							
			for (Formula n: this.two_level_partition.get(t_this).getParts())
			{			
				for (Formula m: sibling.two_level_partition.get(t_sibling).getParts())
				{
					List<Formula> next_list = new ArrayList<Formula>();
					next_list.add(n);
					next_list.add(m);
			
					if (!unique_pairs.contains(next_list))
					{
						count++;	
						unique_pairs.add(next_list);
					}
				}
			}	
			
			big_map.put(t_this,unique_pairs);
			
		}
		
		/*
		for (Term t_this: big_map.keySet())
		{
			System.out.println("node " + parent.getID() + ": t_this = " + t_this + ", unique pairs: " + big_map.get(t_this));	
		}
		*/
		
		result *= count;
		
		return result;
				
	}
	
	/**
	 * Checks the theoretical number of calls to this node against the actual
	 * number.
	 * RCFormDtreeNodeInternal overrides this to make it recursive
	 */
	public void checkNodeCalls(boolean extra_caching)
	{
		int num_predicted = predictedNumCalls(extra_caching);
		int num_actual = node_stats.num_calls;
		
		if (num_predicted != num_actual)
		{
			System.out.println("Discrepancy!");
			System.out.println("Node " + getID() + " predicted num calls: " + num_predicted);
			System.out.println("Node " + getID() + " actual num calls: " + num_actual);
		}
	}
	
	/**
	 * Checks the theoretical cache sizes of this node against the actual
	 * sizes.
	 */
	public void checkCacheSizes(boolean extra_caching)
	{
		int predicted_size = predictedCacheSize();
		int actual_size = node_stats.num_cache_entries;
		
		if (predicted_size != actual_size)
		{
			System.out.println("Discrepancy!");
			System.out.println("Node " + getID() + " predicted cache size: " + predicted_size);
			System.out.println("Node " + getID() + " actual cache size: " + actual_size);
		}		
	}
	
	/**
	 * Returns a string representation of the dtree below this node.
	 *
	 * @param hyphens A string of hyphens to proceed all lines of the string
	 * (this represents the depth of the node, which is sent down from higher in
	 * the dtree).
	 * @return a string representation of the dtree below this node.
	 */
	public abstract String treeString(String hyphens);
	
	/*** DATA MEMBERS ***/
	
	/**
	 * ID number for this node.
	 * Every node in an <code>RCFormDtree</code> has a unique ID number.
	 */
	protected int id;
	
	/**
	 * Reference to this node's parent node (null for the root)
	 */
	protected RCFormDtreeNodeInternal parent;
	
	/**
	 * The list of variables that appear in at least one CPT below this node
	 */
	protected List<FiniteVariable> vars;
	
	/**
	 * The list of variables whose conditional probability distributions are
	 * specified by the CPTs that appear below this node. This list is kept
	 * sorted.
	 */
	protected List<FiniteVariable> endogenous_vars;
	
	/**
	 * The list of variables that appear in at least one CPT below this node,
	 * but only as parents. This list is kept sorted
	 */
	protected List<FiniteVariable> exogenous_vars;
	
	/**
	 * The intersection of this node's outer context (exogenous vars) and that
	 * of its parent
	 */
	protected List<FiniteVariable> parent_context;
	
	/**
	 * The part of the outer context (exogenous vars) that comes from this
	 * node's sibling node
	 */
	protected List<FiniteVariable> sibling_context;
	
	/**
	 * The endogenous variables that point to at least one of the sibling's 
	 * endogenous variables
	 */
	protected List<FiniteVariable> sibling_inner_context;
	
	/**
	 * The set of endogenous variables that have non-endogenous children
	 */
	protected List<FiniteVariable> inner_context;
	
	/**
	 * The set of endogenous variables that have at least one outer context parent
	 */
	public List<FiniteVariable> outer_context_children;
	
	/**
	 * The set of endogenous variables that have at least one sibling context parent
	 */
	public List<FiniteVariable> sibling_context_children;
	
	/**
	 * Maps instantiations of the outer context to reduced CPT keys.
	 * Used with <code>exemplars</code> to map outer context instantiations
	 * to exemplars.
	 */ protected Map<Term, List<List<Double>>> outer_model_map;
	
	/**
	 * Maps reduced CPT keys to instantiations (exemplars).
	 * Used with <code>exemplars</code> to map outer context instantiations
	 * to exemplars.
	 */
	protected Map<List<List<Double>>,Term> exemplars;
	
	/**
	 * The two-level partition for this node.
	 * Partitions first the parent-context, then the sibling-context.
	 * This is the partition used to determine which formulas to
	 * use in case analysis
	 */
	protected Map<Term,Partition> two_level_partition;
	
	/**
	 * Two-level cache. First key is instantiation of outer context (exemplar), 
	 * second key is instantiation of endogenous vars (evidence)
	 */
	protected Map<Term, Map<Term, Double>> cache;
	
	/** 
	 * Extra cache. First key is a term that is the conjunction of the left half
	 * of the evidence, left half of the exemplar, and first term of a formula beta.
	 * Second key is the formula alpha.
	 */
	protected Map<Term, Map<Formula, Double>> alpha_cache;
	
	/** 
	 * Extra cache. First key is a term that is the conjunction of the right half
	 * of the evidence, right half of the exemplar, and first term of a formula alpha.
	 * Second key is the formula beta.
	 */
	protected Map<Term, Map<Formula, Double>> beta_cache;
	
	/** 
	 * Holds run-time stats for this node
	 */
	protected RCFormStats.NodeStats node_stats;
		
}