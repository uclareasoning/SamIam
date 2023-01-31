package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.io.*;
import java.util.*;

/**
 * A dtree usable by the RCForm algorithm.
 *
 * @author Taylor Curtis
 */
public class RCFormDtree
{
	/**
	 * Constructor that builds an <code>RCFormDtree</code> given a belief network
	 * and RC object containing a dtree skeleton.
	 *
	 * @param bn a belief network
	 * @param skeleton a basic dtree structure
	 */
	public RCFormDtree(BeliefNetwork bn, RCGenerator.RC skeleton) throws Exception
	{
		belief_net = bn;
			
		//Convert to RCFormDtree
		buildDtreeFromRC(skeleton);
	}

	/**
	 * Constructor that builds an <code>RCFormDtree</code> given a belief network
	 * and a file containing a dtree. The dtree file must be in the format
	 * that <code>RCGenerator.RC.writeFile</code> generates.
	 *
	 * @param bn a belief network
	 * @param filename the name of a file containing a dtree
	 */
	public RCFormDtree(BeliefNetwork bn, String filename) throws Exception
	{
		belief_net = bn;
		
		getDtreeFromFile(new BufferedReader(new FileReader(filename)));	
	}
	
	/**
	 * Gets the dtree structure from an RC object
	 * 
	 * @param skeleton an RC object with information about a dtree structure
	 */
	private void buildDtreeFromRC(RCGenerator.RC skeleton) throws IOException
	{
		//System.err.println("\nConverting RC to RCFormDtree");
		
		StringWriter sw = new StringWriter();
		skeleton.writeFile(sw);
		BufferedReader br = new BufferedReader(new StringReader(sw.toString()));
		
		sw.close();
		
		getDtreeFromFile(br);
	}
	
	/**
	 * Builds the dtree from a file. The file must be in the format that
	 * <code>RCGenerator.RC.writeFile</code> generates.
	 *
	 * @param br a <code>BufferedReader</code> object for the dtree file
	 */
	private void getDtreeFromFile(BufferedReader br) throws IOException
	{
		//Each line in the file represents a node in the dtree. Put these lines
		//in an array.
		ArrayList lines = new ArrayList();
		String line = br.readLine();
		while (line != null)
		{
			if (line.charAt(0) != '#')
			{
				lines.add(line);
			}
			line = br.readLine();
		}
		br.close();
			
		//Find the root node id, which will be the largest id
		String[] split_line = ((String)lines.get(0)).split(" ");
		assert (split_line[0].equals("ROOT")) : "First RCLine not ROOT";
		int root_id = Integer.parseInt(split_line[1]);
		
		//We'll maintain a map from integer IDs to RCFormDtreeNodes
		Map<Integer, RCFormDtreeNode> nodes = new HashMap<Integer, RCFormDtreeNode>();
		
		//Go through the lines in reverse order, creating nodes from the leaves up to
		//the root
		for (int i = lines.size()-1; i >= 0; i--)
		{
			split_line = ((String)lines.get(i)).split(" ");
			
			//If root or internal node...
			if (split_line[0].equals("ROOT") || split_line[0].equals("I"))
			{	
				int index = Integer.parseInt(split_line[1]);
				int left_index = Integer.parseInt(split_line[3]);
				int right_index = Integer.parseInt(split_line[4]);
				
				//Create the internal node
				RCFormDtreeNodeInternal temp_internal = new RCFormDtreeNodeInternal(index);
				
				//Set left and right children
				temp_internal.setLeftChild(nodes.get(left_index));
				temp_internal.setRightChild(nodes.get(right_index));
				
				//Add to the nodes map
				nodes.put(index,temp_internal);
				
				if (split_line[0].equals("ROOT"))
				{
					//Set root
					root = nodes.get(index);	
				}
				
			}
			
			//If leaf node...
			else if (split_line[0].equals("L"))
			{
				int index = Integer.parseInt(split_line[1]);
				String name = new String(split_line[2]);
				
				//Find the variable in the belief network with this name
				FiniteVariable leaf_var = (FiniteVariable)(belief_net.forID(name));
				
				//Create the leaf node
				RCFormDtreeNodeLeaf temp_leaf = new RCFormDtreeNodeLeaf(index, leaf_var);
				
				//Add to the nodes map
				nodes.put(index, temp_leaf);
			}
			else
			{
				assert (false) : ("Unexpected start to RCLine");
			}				
		} 
		
		//Recursively set the parent pointers for all nodes in the dtree
		root.setParents(null);
	}	
	
	/**
	 * Returns the root node of the dtree
	 *
	 * @return the root node of the dtree
	 */
	public RCFormDtreeNode getRoot()
	{
		return root;	
	}
	
	/**
	 * Does the pre-processing necessary to run RCForm on this dtree. This includes building 
	 * the exemplars map as well as the two-level outer context partition for each node.
	 */
	public void preprocess() throws Exception
	{
		//Build the variable sets for each node
		//System.err.println("\nBuilding variable sets");
		root.buildVarSets();
		root.computeInnerContexts();
		
		//Build a list of the variables in the network, for later use
		RCFormUtils.setNetworkVariables(new ArrayList<FiniteVariable>(root.getEndogenousVars()));
		
		//Print network/dtree stats
		System.out.println("num variables = " + root.getEndogenousVars().size());
		System.out.println("dtree size = " + this.numNodes());
		
		// Build partitions for all nodes in the dtree, recursively
		//System.err.println("Building partitions");
		root.buildPartitions();		
		
		RCFormStats.printDtreeStats();
		
		//Print treewidth and parametric treewidth
		System.out.println("Treewidth = " + root.computeTreewidth());
		root.computeParametricTreewidth();
		System.out.println("Parametric Treewidth = " + RCFormStats.max_ptw);
		System.out.println("Num Ev Vars of node with max ptw = " + RCFormStats.max_ptw_num_evidence_vars);
		root.computeNewParametricTreewidth();
		System.out.println("New Parametric Treewidth = " + RCFormStats.max_newptw);
		System.out.println("Num Ev Vars of node with max new ptw = " + RCFormStats.max_newptw_num_evidence_vars);
		
	}	
	
	/**
	 * Returns a string representation of the dtree.
	 *
	 * @return a string representation of the dtree 
	 */
	public String toString()
	{
		return root.treeString("");	
	}
	
	/**
	 * Returns the number of nodes in the dtree
	 *
	 * @return the number of nodes in the dtree
	 */
	public int numNodes()
	{
		return root.numNodes();	
	}
	
	/*** DATA MEMBERS ***/
	
	/**
	 * The belief network that this dtree applies to
	 */
	private BeliefNetwork belief_net;
	
	/**
	 * The root node of this dtree
	 */
	private RCFormDtreeNode root;
	
}