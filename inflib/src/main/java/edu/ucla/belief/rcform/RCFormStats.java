package edu.ucla.belief.rcform;

/**
 * Class for keeping rcform-related statistics
 *
 * @author Taylor Curtis
 */
public class RCFormStats
{
	/**
	 * Re-initializes the runtime statistics (called by rcform before it starts)
	 */
	public static void resetRunTimeStats()
	{
		//sum_num_calls starts at 1 for the first call to the root
		sum_num_calls = 1;	
	}
	
	/**
	 * Prints the dtree statistics
	 */
	public static void printDtreeStats()
	{
		System.out.println("Max outer-context size = " + max_outer_size);
		System.out.println("Max inner-context size = " + max_inner_size);
		System.out.println("Max sibling-outer-context size = " + max_sibling_outer_size);	
		System.out.println("Max outer-context partition size = " + max_partition_size);
	}
	
	/*** Dtree Stats ***/
	
	public static int max_outer_size = 0;
	
	public static int max_inner_size = 0;
		
	public static int max_sibling_outer_size = 0;
	
	public static int max_partition_size = 0;
	
	public static double max_ptw = 0;
	
	public static int max_ptw_num_evidence_vars = 0;
	
	public static double max_newptw = 0;
	
	public static int max_newptw_num_evidence_vars = 0;
	
	/*** Runtime stats ***/
	
	public static int sum_num_calls = 1;
	
	
	/**
	 * Class for holding stats for a particular dtree node.
	 * This is just used by the sanity-checking code that
	 * makes sure each node gets called the right number of
	 * times and has the right size cache.
	 *
	 * @author Taylor Curtis
	 */
	public static class NodeStats
	{
		/**
		 * Default constructor
		 */
		public NodeStats()
		{
			num_calls = 0;	
			num_cache_entries = 0;
			num_alpha_cache_entries = 0;
			num_beta_cache_entries = 0;
		}		
	
		/**
		 * The number of times this node is called during a run of rcform
		 */
		public int num_calls;
		
		/**
		 * The number of entries in this node's cache
		 */
		public int num_cache_entries;
		
		/**
		 * The number of entries in this node's alpha cache
		 */
		public int num_alpha_cache_entries;
		
		/**
		 * The number of entries in this node's beta cache
		 */
		public int num_beta_cache_entries;
	}
}

