package edu.ucla.belief.rcform;

import edu.ucla.belief.*;
import edu.ucla.util.*;

import java.util.*;

/**
 * Class that contains the RCForm (Formula Conditioning) inference algorithm
 *
 * @author Taylor Curtis
 */
public class RCForm
{	
	/**
	 * Wrapper function for RCForm algorithm
	 *
	 * @param dt a dtree that has been prepared for inference
	 * @param evidence evidence on some of the network's variables
	 * @param extra_caching a flag that directs whether to employ extra caching
	 *
	 * @return the probability of the given evidence on the given dtree
	 */
	public static double rcform(RCFormDtree dt, Term evidence, boolean extra_caching)
	{
		RCFormDtreeNode root_node = dt.getRoot();
		
		root_node.initializeNodes();
		RCFormStats.resetRunTimeStats();
		
		//Call the recursive RCForm method
		return rcformRecursive(root_node, new Term(), evidence, extra_caching);	
	}
	
	/**
	 * Recursive version of RCForm algorithm
	 *
	 * @param dt a dtree node
	 * @param context an instantiation of <code>dt</code>'s exogenous variables
	 * @param e a partial instantiation of <code>dt</code>'s endogenous variables
	 * @param extra_caching a flag that directs whether to employ extra caching
	 */ 
	public static double rcformRecursive(RCFormDtreeNode dt, Term context, Term e, boolean extra_caching)
	{	
		//Declare extra caches.
		Map<Term,Map<Formula,Double>> alpha_cache = null;
		Map<Term,Map<Formula,Double>> beta_cache = null;  
		
		Term term_key = null;
		boolean cached = false;
		
		dt.node_stats.num_calls++;
		RCFormStats.sum_num_calls++;
			
		//System.out.println("Calling rcformRecursive on node " + dt.getID() + " with context " + context + " and evidence " + e);
		
		//Get the exemplar for the given instantiation of the outer-context
		Term exemplar = dt.getExemplar(context);
		
		//Check the two-level cache
		if (dt.cache.containsKey(exemplar))
		{
			if (dt.cache.get(exemplar).containsKey(e))
			{
				return dt.cache.get(exemplar).get(e);	
			}
		}
		
		double p = 0;
		
		if (dt.isLeaf())
		{
			//Lookup the probability in the leaf variable's CPT
			FiniteVariable leaf_var = ((RCFormDtreeNodeLeaf)dt).getLeafVar();
			p = lookup(leaf_var, exemplar, e);
		}
		else //Internal node
		{
			//Divide the evidence into the part that applies to the left child and the
			//part that applies to the right child
			Term e_l = Term.project(e, dt.getLeft().getEndogenousVars());
			Term e_r = Term.project(e, dt.getRight().getEndogenousVars());
	
			//Divide the exemplar into the part that applies to the left child and the
			//part that applies to the right child
			Term exemplar_l = Term.project(exemplar, dt.getLeft().getExogenousVars()); 
			Term exemplar_r = Term.project(exemplar, dt.getRight().getExogenousVars());
		
			//Get the second-level partitions for the left and right children (these
			//will supply the formulas for case analysis)
			Partition left_partition = dt.getLeft().getPartition(exemplar_l);
			Partition right_partition = dt.getRight().getPartition(exemplar_r);
			
			p = 0;
			
			//Get the two lists of formulas to condition on
			List<Formula> left_parts = new ArrayList<Formula>(left_partition.getParts());
			List<Formula> right_parts = new ArrayList<Formula>(right_partition.getParts());
			
			//Remove formulas that conflict with the evidence
			Iterator<Formula> lit = left_parts.iterator();
			while (lit.hasNext())
			{
				Formula f = lit.next();	
				if (!f.isConsistentWith(e_r))
				{
					lit.remove();	
				}	
			}
			Iterator<Formula> rit = right_parts.iterator();
			while (rit.hasNext())
			{
				Formula f = rit.next();	
				if (!f.isConsistentWith(e_l))
				{
					rit.remove();	
				}	
			}
			
			//Consider each pair of formulas from left_parts and right_parts
			for (Formula alpha: right_parts)
			{	
				//Compute the outer-context instantiation of the right child	
				Term right_instance = Term.conjoin(alpha.getFirstTerm(), exemplar_r);
			
				for (Formula beta: left_parts)
				{
					//Compute the outer-context instantiation of the left child
					Term left_instance = Term.conjoin(beta.getFirstTerm(), exemplar_l);
				
					/*** COMPUTE THE PROBABILITY ON THE LEFT CHILD ***/
					
					double left_result = 0;
					
					if (extra_caching)
					{
						//Check the alpha cache
						cached = false;
						alpha_cache = dt.alpha_cache;
						term_key = Term.conjoin(left_instance,e_l);
						if (alpha_cache.containsKey(term_key))
						{
							if (alpha_cache.get(term_key).containsKey(alpha))
							{
								left_result = alpha_cache.get(term_key).get(alpha);	
								cached = true;
							}
						}
					}
					
					//If it's not in the alpha cache, compute it
					if (!extra_caching || !cached)
					{
						//To compute the probability of evidence alpha, sum the probabilities
						//for each model of alpha
						for (Term m: alpha.getModels())
						{
							//Build new evidence for left child. If m contradicts e_l, Term.conjoin
							//returns null
							Term new_evidence = Term.conjoin(e_l, m);
							
							if (new_evidence != null)
							{	
								left_result += rcformRecursive(dt.getLeft(), left_instance, new_evidence, extra_caching); 
							}	
						}		
					
						
						if (extra_caching)
						{
							//Cache left result
							Map<Formula,Double> Formula_cache = null;
							if (alpha_cache.containsKey(term_key))
							{
								Formula_cache = alpha_cache.get(term_key);	
							}
							else
							{
								Formula_cache = new HashMap<Formula, Double>();	
							}
							Formula_cache.put(alpha, left_result);
							alpha_cache.put(term_key, Formula_cache);
							dt.node_stats.num_alpha_cache_entries++;
						}
					}
							
					/*** COMPUTE THE PROBABILITY ON THE RIGHT CHILD ***/
					
					double right_result = 0;
					
					
					if (extra_caching)
					{
						//Check the beta cache
						cached = false;
						beta_cache = dt.beta_cache;
						term_key = Term.conjoin(right_instance,e_r);
						if (beta_cache.containsKey(term_key))
						{
							if (beta_cache.get(term_key).containsKey(beta))
							{
								right_result = beta_cache.get(term_key).get(beta);	
								cached = true;
							}
						}
					}
					
					//If it's not in the cache, compute it
					if (!extra_caching || !cached)
					{
						//To compute the probability of evidence beta, sum the probabilities
						//for each model of beta
						for (Term m: beta.getModels())
						{
							//Build new evidence for right child. If m contradicts e_r, Term.conjoin
							//returns null
							Term new_evidence = Term.conjoin(e_r, m);
							if (new_evidence != null)
							{	
								right_result += rcformRecursive(dt.getRight(), right_instance, new_evidence, extra_caching);	
								
							}
						}
					
						if (extra_caching)
						{
							//Cache right result
							Map<Formula,Double> Formula_cache = null;
							if (beta_cache.containsKey(term_key))
							{
								Formula_cache = beta_cache.get(term_key);	
							}
							else
							{
								Formula_cache = new HashMap<Formula, Double>();	
							}
							Formula_cache.put(beta, right_result);
							beta_cache.put(term_key, Formula_cache);
							dt.node_stats.num_beta_cache_entries++;
						}
					}
						
					//Finally multiply left_result and right_result to get the probability of
					//this case (that is, this pair of alpha and beta values). Add the product
					//to the total sum.
					p += (left_result * right_result);
				}
			}
		}
		
		//Cache the final result
		Map<Term, Double> second_cache = null;
		if (dt.cache.containsKey(exemplar))
		{
			second_cache = dt.cache.get(exemplar);	
		}
		else
		{
			second_cache = new HashMap<Term, Double>();	
		}
		second_cache.put(e, p);
		dt.cache.put(exemplar, second_cache);
		dt.node_stats.num_cache_entries++;
		
		return p;	
	}
	
	/**
	 * Looks up and returns a probability in a CPT
	 *
	 * @param v a variable
	 * @param t an instantiation of v's parents
	 * @param e a possibly empty instantiation of v
	 *
	 * @return P(e|t), or 1 if e is empty
	 */
	public static double lookup(FiniteVariable v, Term t, Term e)
	{
		if (e.isEmpty())
		{
			return 1;	
		}
		
		double result = 0;
		
		Object csi_type = v.getProperty(CSITypeProperty.PROPERTY);
		
		
		if (csi_type == CSITypeProperty.OR)
		{
			//If v is an OR, there's no need to reference the CPT. Just check if any of
			//the parents are true.
			if (t.allFalse() == e.allFalse())
			{
				result = 1.0;
			}
			else
			{
				result = 0.0;	
			}
		}
		else if (csi_type == CSITypeProperty.AND)
		{
			//If v is an AND, there's no need to reference the CPT. Just check if any of
			//the parents are false.
			if (t.allTrue() == e.allTrue())
			{
				result = 1.0;
			}
			else
			{
				result = 0.0;	
			}
		}
		else if (csi_type == CSITypeProperty.EVEN)
		{
			//Check if an even number of parents is true
			if (t.evenTrue() == e.allTrue())
			{
				result = 1.0;	
			}
			else
			{
				result = 0.0;	
			}
		}
		else if (csi_type == CSITypeProperty.ODD)
		{
			//Check if an even number of parents is true
			if (t.evenTrue() == e.allFalse())
			{
				result = 1.0;	
			}
			else
			{
				result = 0.0;	
			}
		}
		else
		{
			//Get the CPT
			CPTShell cpt = v.getCPTShell();
			
			//Build an instantiation map for getCP()
			Map<FiniteVariable, Object> instantiations = t.getMap();
			instantiations.putAll(e.getMap());
			
			//Look it up
			result = cpt.getCP(instantiations);
		}		
		
		return result;	
	}
}