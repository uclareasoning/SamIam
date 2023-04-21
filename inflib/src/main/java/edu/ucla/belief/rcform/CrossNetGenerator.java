package edu.ucla.belief.rcform;

import edu.ucla.belief.*;

import java.io.*;
import java.util.*;

public class CrossNetGenerator
{
	public static void main(String[] args) throws IOException
	{
		boolean expand = false;
		boolean scramble = true;
		
		int k = -1;
		int m = new Integer(args[0]);
		//int k = new Integer(args[0]);
		int h = new Integer(args[1]);
		
		String appendix = "";
		if (args.length > 2)
		{
			appendix = args[2];
		}
			
		
		
		CrossNetwork cnet = generate(k,m,h,false,(int)Math.pow(2,h+1)-2, expand);
		
		String filename = null;
		String dtreefilename = null;
		
		if (m < h) {
			filename = "mcross-"+m+"-"+h+appendix+".net";
			dtreefilename = "mcross-"+m+"-"+h+appendix+".dt";
		}
		else {
			filename = "cross-"+h+appendix+".net";
			dtreefilename = "cross-"+h+appendix+".dt";
		}
			
		cnet.writeToFile(new BufferedWriter(new FileWriter(filename)));	
		cnet.writeDtreeToFile(new BufferedWriter(new FileWriter(dtreefilename)));
		
		if (expand)
		{
			String efilename = "ecross-"+m+"-"+h+appendix+".net";
			cnet.writeExpandedToFile(new BufferedWriter(new FileWriter(efilename)));	
		}
		
		
		/*
		if (expand)
		{
			ExpandedCrossNetwork ecn = new ExpandedCrossNetwork(cnet);
			filename = "ecross-"+m+"-"+h+appendix+".net";
			
			ecn.writeToFile(new BufferedWriter(new FileWriter(filename)));	
		}
		*/
		
		if (scramble)
		{
			ScrambledCrossNet scnet = new ScrambledCrossNet(cnet);
			scnet.writeToFile(new BufferedWriter(new FileWriter("s"+filename)));
		}
		
	}
	
	//Recursive function that generates a cross-network
	public static CrossNetwork generate(int k, int m, int height, boolean full, int root_index, boolean expand)
	{
		CrossNetwork new_network = null;
		
		if (height == 0)
		{
			new_network = new CrossNetwork(root_index);	
		}
		else
		{
			CrossNetwork crossnet1 = generate(k, m, height-1, full, root_index-1, expand);
			
			/* This makes networks that are too big...
			if (rng.nextBoolean())
			{
				new_network = crossnet1;
			}
			else
			{
				CrossNetwork crossnet2 = generate(k, m, height-1, full, root_index-(int)Math.pow(2,height));
			
				new_network = new CrossNetwork(crossnet1, crossnet2, k, m, full, root_index);
			}
			*/
			
			//CrossNetwork crossnet2 = generate(k, m, Math.min(rng.nextInt(m),height-1), full, root_index-(int)Math.pow(2,height));	
			
			CrossNetwork crossnet2 = generate(k, m, rng.nextInt(height), full, root_index-(int)Math.pow(2,height), expand);	
			
			new_network = new CrossNetwork(crossnet1, crossnet2, k, m, full, root_index, expand);
		}
		
		return new_network;
	}	
	
	public static class CrossNetwork
	{
		public CrossNetwork(CrossNetwork net1, CrossNetwork net2, int k, int m, boolean full, int root_index, boolean expand)
		{	
			//Carry over aux variables
			aux = new ArrayList<SimpleNode>(net1.aux);
			aux.addAll(net2.aux);
			
			//Carry over aux_child map
			aux_children = new HashMap<SimpleNode,SimpleNode>(net1.aux_children);
			aux_children.putAll(net2.aux_children);
			
			//Build U^L
			u_left = new ArrayList<SimpleNode>(net1.u_left);
			u_left.addAll(net1.u_right);
			
			//Build U^R
			u_right = new ArrayList<SimpleNode>(net2.u_left);
			u_right.addAll(net2.u_right);
			
			//Build V^L
			v_left = new ArrayList<SimpleNode>(net2.v_left);
			v_left.addAll(net2.v_right);
			
			//Build V^R
			v_right = new ArrayList<SimpleNode>(net1.v_left);
			v_right.addAll(net1.v_right);
			
			//Add edges from U^L to V^L
			
			List<SimpleNode> left_parent_list = new ArrayList<SimpleNode>();
			for (SimpleNode p: u_left)
			{
				if (m == -1 || p.age < m)
				{
					if (full || rng.nextBoolean())
					{
						for (SimpleNode c: v_left)
						{
							c.parents.add(p);	
						}
						
						if (expand)
						{
							left_parent_list.add(p);
						}	
					}
					p.age++;
				}
					
			}
		
			
			//Add edges from U^R to V^R
			
			List<SimpleNode> right_parent_list = new ArrayList<SimpleNode>();
			for (SimpleNode p: u_right)
			{
				if (m == -1 || p.age < m)
				{
					if (full || rng.nextBoolean())
					{
						for (SimpleNode c: v_right)
						{
							c.parents.add(p);	
						}
						if (expand)
						{	
							right_parent_list.add(p);
						}
					}	
					p.age++;
				}								
			}
		
			
				
			
			
			if (expand)
			{
				SimpleNode new_aux_node = null;
					
				if (v_left.size() > 0)
				{
				if (left_parent_list.size() > 0)
				{
					List<SimpleNode> aux_parents = new ArrayList<SimpleNode>();
						
					//Create the aux chain
					for (int i=0; i<left_parent_list.size(); i++)
					{
						//Create new auxiliary node
						aux_parents.add(left_parent_list.get(i));
						new_aux_node = new SimpleNode(root_index, i+1, "l", new ArrayList<SimpleNode>(aux_parents));
						aux.add(new_aux_node);
						aux_parents.clear();
						aux_parents.add(new_aux_node);			
					}
					
				
					
					//Connect the aux chain to each child (the aux copy of the child)
					for (SimpleNode c: v_left)
					{
						SimpleNode aux_child = aux_children.get(c);
						if (aux_child.parents.size() > 0)
						{
							assert (aux_child.parents.size() == 1);
							aux_parents.add(aux_child.parents.get(0));
							new_aux_node = new SimpleNode(root_index, aux_child.number, new ArrayList<SimpleNode>(aux_parents));
							aux.add(new_aux_node);
							aux_child.parents.clear();
							aux_child.parents.add(new_aux_node);	
							aux_parents.remove(aux_parents.size()-1);
						}
						else
						{
							aux_child.parents.add(aux_parents.get(0));	
						}
				
					}
					
					
				}
			}
				
				//Do the same thing for the right nodes
				
				if (v_right.size() > 0)
				{
				if (right_parent_list.size() > 0)
				{
					List<SimpleNode> aux_parents = new ArrayList<SimpleNode>();
						
					//Create the aux chain
					for (int i=0; i<right_parent_list.size(); i++)
					{
						//Create new auxiliary node
						aux_parents.add(right_parent_list.get(i));
						new_aux_node = new SimpleNode(root_index, i+1, "r", new ArrayList<SimpleNode>(aux_parents));
						aux.add(new_aux_node);
						aux_parents.clear();
						aux_parents.add(new_aux_node);			
					}
					
					//Connect the aux chain to each child (the aux copy of the child)
					for (SimpleNode c: v_right)
					{
						SimpleNode aux_child = aux_children.get(c);
						if (aux_child.parents.size() > 0)
						{
							assert (aux_child.parents.size() == 1);
							aux_parents.add(aux_child.parents.get(0));
							new_aux_node = new SimpleNode(root_index, aux_child.number, new ArrayList<SimpleNode>(aux_parents));
							aux.add(new_aux_node);
							aux_child.parents.clear();
							aux_child.parents.add(new_aux_node);	
							aux_parents.remove(aux_parents.size()-1);
						}
						else
						{
							aux_child.parents.add(aux_parents.get(0));	
						}	
					}
				}
			}
				
				
			}
		
			
			
			dtree = new DtreeNode(root_index, net1.dtree, net2.dtree, null);
		}
			
		public CrossNetwork(int root_index)
		{	
			u_left = new ArrayList<SimpleNode>();
			u_right = new ArrayList<SimpleNode>();
			v_left = new ArrayList<SimpleNode>();
			v_right = new ArrayList<SimpleNode>();
			aux = new ArrayList<SimpleNode>();
			aux_children = new HashMap<SimpleNode,SimpleNode>();
			
			if (rng.nextBoolean())
			{
				//New cross-network with a single parent
				u_left.add(new SimpleNode(name_index));
			}
			else
			{
				//New cross-network with a single child
				List<SimpleNode> parent_list = new ArrayList<SimpleNode>();
				SimpleNode new_child = new SimpleNode(name_index, parent_list);
				v_left.add(new_child);
				SimpleNode new_aux_child = new SimpleNode(name_index, new ArrayList<SimpleNode>());
				new_aux_child.parents.addAll(parent_list);
				aux_children.put(new_child, new_aux_child);
					
			}
			
			ArrayList vals = new ArrayList();
			vals.add("true");
			vals.add("false");
			FiniteVariable leaf_var = new FiniteVariableImpl("N"+(name_index),vals);
			
			dtree = new DtreeNode(root_index, null, null,leaf_var);
			
			name_index++;	
		}
		
		public void writeToFile(Writer wr) throws IOException
		{	
			wr.write("net\n{\n\thuginity = \"permissive\";\n}\n");
			
			//Declare variables
			for (SimpleNode p: u_left)
			{
				wr.write("node " + p.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n}\n");
			}	
			for (SimpleNode p: u_right)
			{
				wr.write("node " + p.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n}\n");
			}	
			for (SimpleNode c: v_left)
			{
				wr.write("node " + c.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n");
				wr.write("\tcsitype = \"or\";\n}\n");
			}	
			for (SimpleNode c: v_right)
			{
				wr.write("node " + c.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n");
				wr.write("\tcsitype = \"or\";\n}\n");
			}	
			
			//Define CPTs (all ORs right now, parents have 50/50 priors))
			for (SimpleNode p: u_left)
			{
				wr.write("potential ( " + p.name + " | )\n{\n");	
				wr.write("\tdata = ( 0.5 0.5 );\n}\n");								
				
			}	
			for (SimpleNode p: u_right)
			{
				wr.write("potential ( " + p.name + " | )\n{\n");		
				wr.write("\tdata = ( 0.5 0.5 );\n}\n");
			}	
			for (SimpleNode c: v_left)
			{
				wr.write("potential ( " + c.name + " | ");
				for (SimpleNode parent: c.parents)
				{
					wr.write(parent.name + " " );	
				}
				wr.write(")\n{}\n");	
				
				//Don't write CPT (it could be big)
				/*
				wr.write("\tdata = ( ");
				
				for (int i=0; i<Math.pow(2,c.parents.size())-1; i++)
				{
					wr.write("\t\t( 1.0 0.0 )\n");	
				}
				wr.write("\t\t( 0.0 1.0 ));\n\n}\n");
				*/
			}	
			for (SimpleNode c: v_right)
			{
				wr.write("potential ( " + c.name + " | ");
				for (SimpleNode parent: c.parents)
				{
					wr.write(parent.name + " " );	
				}
				wr.write(")\n{}\n");
				
				//Don't write CPT (it could be big)
				/*
				wr.write("\tdata = ( ");
				for (int i=0; i<Math.pow(2,c.parents.size())-1; i++)
				{
					wr.write("\t\t( 1.0 0.0 )\n");	
				}
				wr.write("\t\t( 0.0 1.0 ));\n\n}\n");
				*/
			}	
			
			wr.close();
			
		}
		
		public void writeExpandedToFile(Writer wr) throws IOException
		{
			wr.write("net\n{\n\thuginity = \"permissive\";\n}\n");
			
			//Declare variables
			for (SimpleNode p: u_left)
			{
				wr.write("node " + p.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n}\n");
			}	
			for (SimpleNode p: u_right)
			{
				wr.write("node " + p.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n}\n");
			}	
			for (SimpleNode c: aux_children.keySet())
			{
				wr.write("node " + c.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n");
				wr.write("\tcsitype = \"or\";\n}\n");
			}	
			for (SimpleNode a: aux)
			{
				wr.write("node " + a.name + "\n{\n");
				wr.write("\tstates = (\"true\" \"false\");\n");
				wr.write("\tcsitype = \"or\";\n}\n");	
			}
			
			//Define CPTs (all ORs right now, parents have 50/50 priors))
			for (SimpleNode p: u_left)
			{
				wr.write("potential ( " + p.name + " | )\n{\n");	
				wr.write("\tdata = ( 0.5 0.5 );\n}\n");								
				
			}	
			for (SimpleNode p: u_right)
			{
				wr.write("potential ( " + p.name + " | )\n{\n");		
				wr.write("\tdata = ( 0.5 0.5 );\n}\n");
			}	
			for (SimpleNode c: aux_children.keySet())
			{
				wr.write("potential ( " + c.name + " | ");
				for (SimpleNode parent: c.parents)
				{
					wr.write(parent.name + " " );	
				}
				wr.write(")\n{\n");
				
				//Write the OR CPT because it's little
				
				wr.write("\tdata = ( ");
				for (int i=0; i<Math.pow(2,c.parents.size())-1; i++)
				{
					wr.write("\t\t( 1.0 0.0 )\n");	
				}
				wr.write("\t\t( 0.0 1.0 ));\n\n}\n");
				
			}	
			for (SimpleNode a: aux)
			{
				wr.write("potential ( " + a.name + " | ");
				for (SimpleNode parent: a.parents)
				{
					wr.write(parent.name + " " );	
				}
				wr.write(")\n{\n");	
				
				//Write the OR CPT because it's little
				/*
				wr.write("\tdata = ( ");
				for (int i=0; i<Math.pow(2,a.parents.size())-1; i++)
				{
					wr.write("\t\t( 1.0 0.0 )\n");	
				}
				wr.write("\t\t( 0.0 1.0 ));\n\n}\n");
				*/
			}
			
			wr.close();
			
		}
		
		public void writeDtreeToFile(Writer wr) throws IOException
		{
			dtree.writeDtree(wr);
		}
				
		public List<SimpleNode> u_left;
		public List<SimpleNode> u_right;
		public List<SimpleNode> v_left;
		public List<SimpleNode> v_right;
		
		public List<SimpleNode> aux;
		public Map<SimpleNode,SimpleNode> aux_children;
		
		private DtreeNode dtree;
	}
	
	public static class SimpleNode
	{
		public SimpleNode(int n)
		{
			System.out.println("Creating parent node N " + n);
			name = "N" + n;	
			parents = new ArrayList<SimpleNode>();
			age = 0;
			number = n;
		}
		
		public SimpleNode(int n, List<SimpleNode> par)
		{
			System.out.println("Creating child node N " + n);
			name = "N" + n;
			parents = par;	
			age = 0;
			number = n;
		}
		
		public SimpleNode(int n, int m, List<SimpleNode> par)
		{
			System.out.println("Creating auxiliary node N " + n + "_" + m);
			name = "N" + n + "_" + m;
			parents = par;	
			age = 0;	
			number = n;
		}
		
		public SimpleNode(int n, int m, String s, List<SimpleNode> par)
		{
			System.out.println("Creating auxiliary node N " + n + "_" + m + "_" + s);
			name = "N" + n + "_" + m + "_" + s;
			parents = par;	
			age = 0;	
			number = n;
		}
		
		public String toString()
		{
			return name;	
		}
		
		public String name;
		public List<SimpleNode> parents;
		public int age;	
		public int number;
	}
	
	public static class DtreeNode
	{
		public DtreeNode(int node_id, DtreeNode left_tree, DtreeNode right_tree, FiniteVariable leafvar)
		{
			left = left_tree;
			right = right_tree;	
				
			id = node_id;
			
			leaf_var = leafvar;
		}
		
		public void writeDtree(Writer wr) throws IOException
		{
			//System.out.println("writing dtree " + id);
			
			if (left != null)
			{
				assert (right != null);
				//Add the line for the root
				wr.write("ROOT " + id + " cachefalse " + left.id + " " + right.id + "\n");
				left.write(wr);
				right.write(wr);	
			}
			else
			{
				assert (right == null);
				this.write(wr);
			}
			
			wr.close();
		}
		
		public void write(Writer wr) throws IOException
		{
			//System.out.println("Writing dtree node " + id);
			
			if (left == null)
			{
				assert (right == null);
				assert (leaf_var != null);
				//Add line for leaf
				wr.write("L " + id + " " + leaf_var.getID() + " cpt\n");
			}
			else
			{
				assert (right != null);
				
				wr.write("I " + id + " cachefalse " + left.id + " " + right.id + "\n");
				left.write(wr);
				right.write(wr);	
			}
		}
		
		public int id;		
		public FiniteVariable leaf_var;
		public DtreeNode left;
		public DtreeNode right;	
	}
	
	/**
	 * Class that adds auxiliary variables to a cross-network so that every variable has at most two parents
	 */
	public static class ExpandedCrossNetwork
	{
		public ExpandedCrossNetwork(CrossNetwork cnet)
		{
			//Add parents as is (as are?)
			parents = new ArrayList<SimpleNode>(cnet.u_left);
			parents.addAll(cnet.u_right);
			
			//Go through the children, and auxiliarize them
			children = new ArrayList<SimpleNode>();
			
			List<SimpleNode> old_children = new ArrayList<SimpleNode>(cnet.v_left);
			old_children.addAll(cnet.v_right);
			
			for (SimpleNode child: old_children)
			{
				List<SimpleNode> parent_list = new ArrayList<SimpleNode>(child.parents);
				if (parent_list.size() <= 2)
				{
					children.add(child);
				}
				else
				{
					List<SimpleNode> aux_parents = new ArrayList<SimpleNode>();
					aux_parents.add(parent_list.get(0));
					
					for (int i=1; i<parent_list.size(); i++)
					{
						//Create new auxiliary node
						aux_parents.add(parent_list.get(i));
						SimpleNode new_aux_node = new SimpleNode(child.number, i, new ArrayList<SimpleNode>(aux_parents));
						children.add(new_aux_node);
						aux_parents.clear();
						aux_parents.add(new_aux_node);			
					}
				}	
			}	
		}
		
		public void writeToFile(Writer wr) throws IOException
		{
			wr.write("net\n{\n\thuginity = \"permissive\";\n}\n");
			
			//Declare variables
			for (SimpleNode p: parents)
			{
				wr.write("node " + p.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n}\n");
			}	
			for (SimpleNode c: children)
			{
				wr.write("node " + c.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n");
				wr.write("\tcsitype = \"or\";\n}\n");
			}	
			
			//Define CPTs (all ORs right now, parents have 50/50 priors))
			for (SimpleNode p: parents)
			{
				wr.write("potential ( " + p.name + " | )\n{\n");	
				wr.write("\tdata = ( 0.5 0.5 );\n}\n");								
				
			}	
			for (SimpleNode c: children)
			{
				wr.write("potential ( " + c.name + " | ");
				for (SimpleNode parent: c.parents)
				{
					wr.write(parent.name + " " );	
				}
				wr.write(")\n{}\n");
				
				//Don't write CPT (it could be big)
				/*
				wr.write("\tdata = ( ");
				for (int i=0; i<Math.pow(2,c.parents.size())-1; i++)
				{
					wr.write("\t\t( 1.0 0.0 )\n");	
				}
				wr.write("\t\t( 0.0 1.0 ));\n\n}\n");
				*/
			}	
			
			wr.close();
			
		}
		
		List<SimpleNode> parents;
		List<SimpleNode> children;
	}
	
	public static class ScrambledCrossNet
	{
		public ScrambledCrossNet(CrossNetwork cnet)	
		{
			int num_edges = 0;
			parents = new ArrayList<SimpleNode>();
			children = new ArrayList<SimpleNode>();
			
			for (SimpleNode p: cnet.u_left) {
				parents.add(new SimpleNode(p.number));	
			}
			for (SimpleNode p: cnet.u_right) {
				parents.add(new SimpleNode(p.number));	
			}
			for (SimpleNode c: cnet.v_left) {
				num_edges += c.parents.size();
				children.add(new SimpleNode(c.number, new ArrayList<SimpleNode>()));	
			}
			for (SimpleNode c: cnet.v_right) {
				num_edges += c.parents.size();
				children.add(new SimpleNode(c.number, new ArrayList<SimpleNode>()));	
			}
			
			//Add edges at random (same number as in original crossnet)
			int num_parents = parents.size();
			int num_children = children.size();
			Random rng = new Random();
			for (int i=0; i<num_edges; i++) {
				SimpleNode random_parent = parents.get(rng.nextInt(num_parents));
				SimpleNode random_child = children.get(rng.nextInt(num_children));
				while (random_child.parents.contains(random_parent)) {
					random_parent = parents.get(rng.nextInt(num_parents));
					random_child = children.get(rng.nextInt(num_children));
				}
				
				random_child.parents.add(random_parent);
				
			}
			
		}
		
		public void writeToFile(Writer wr) throws IOException
		{
			wr.write("net\n{\n\thuginity = \"permissive\";\n}\n");
			
			//Declare variables
			for (SimpleNode p: parents)
			{
				wr.write("node " + p.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n}\n");
			}	
			for (SimpleNode c: children)
			{
				wr.write("node " + c.name + "\n{\n");	
				wr.write("\tstates = (\"true\" \"false\");\n");
				wr.write("\tcsitype = \"or\";\n}\n");
			}	
			
			//Define CPTs (all ORs right now, parents have 50/50 priors))
			for (SimpleNode p: parents)
			{
				wr.write("potential ( " + p.name + " | )\n{\n");	
				wr.write("\tdata = ( 0.5 0.5 );\n}\n");								
				
			}	
			for (SimpleNode c: children)
			{
				wr.write("potential ( " + c.name + " | ");
				for (SimpleNode parent: c.parents)
				{
					wr.write(parent.name + " " );	
				}
				wr.write(")\n{}\n");
			}	
			
			wr.close();
			
		}
		
		
		private List<SimpleNode> parents;
		private List<SimpleNode> children;
	}
	
	private static int name_index = 0;	
	private static Random rng = new Random();
}
	
	
	
	
	
	
	
	
