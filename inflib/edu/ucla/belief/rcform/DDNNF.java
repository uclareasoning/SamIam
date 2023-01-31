package edu.ucla.belief.rcform;

import edu.ucla.belief.*;
import edu.ucla.structure.*;

import java.io.*;
import java.util.*;

public class DDNNF
{
	public DDNNF()
	{
	}
	
	public DDNNF(Literal l)
	{
		node_type = null;
		literal = l;
		children = null;	
	}
	
	public DDNNF(String nodetype, ArrayList<DDNNF> c_nodes)
	{
		node_type = nodetype;
		literal = null;
		children = new ArrayList<DDNNF>(c_nodes);	
	}
	
	//Structural equality, not logical equality
	public boolean equals(Object obj)
	{
		if (obj == null || !(this.getClass().equals(obj.getClass())))
		{
			return false;
		}
		
		DDNNF other = (DDNNF)obj;
		if (this.isLeaf() && other.isLeaf())
		{
			return this.literal.equals(other.literal);	
		}
		
		return (this.node_type.equals(other.node_type) && this.children.equals(other.children));
	}
	
	public int hashCode()
	{
		int hash = 1;
		hash = hash * 31 + node_type.hashCode();
		hash = hash * 31 + literal.hashCode();
		hash = hash * 31 + children.hashCode();
		
		return hash;	
	}
	
	//Assumes CNF has more than one clause
	public static DDNNF convertCNF(CNF f) throws Exception
	{	
		assert (f.getClauses().size() > 1);
		
		//System.out.println("f = " + f);
		
		MappedList index_map = f.buildIndexMap();
		
		//Write the CNF to a temporary .cnf file
		File temp_file = File.createTempFile("tempfile", ".cnf", new File("."));
		temp_file.deleteOnExit();
		Writer writer = new BufferedWriter(new FileWriter(temp_file));
		f.writeToFile(writer, index_map);
		writer.close();
		
		//Convert the CNF to a dDNNF using c2d
		Runtime rt = Runtime.getRuntime();
		String[] cmds = new String[4];
		cmds[0] = RCFormUtils.C2DEXECUTABLE;
		cmds[1] = "-reduce";
		cmds[2] = "-in";
		cmds[3] = temp_file.getName(); 
		Process c2d_call = rt.exec(cmds);	
		
		//Exhaust the input and error streams so waitFor() won't hang
		BufferedReader br = new BufferedReader(new InputStreamReader(c2d_call.getInputStream()));
		while (br.readLine() != null);
		br = new BufferedReader(new InputStreamReader(c2d_call.getErrorStream()));
		while (br.readLine() != null);
		
		c2d_call.waitFor(); 
		
		//Build this from the dDNNF file
		DDNNF result = buildDDNNFFromFile(temp_file.getName() + ".nnf", index_map);
		temp_file.delete();
		return result;
		
	}
	
	public static DDNNF buildDDNNFFromFile(String filename, MappedList index_map) throws Exception
	{
		//System.out.println("Ready to build from file " + filename);
		
		//Maintain an arraylist of the nodes, so they can be accessed by their index
		
		ArrayList<DDNNF> nodes = new ArrayList<DDNNF>();
		ArrayList<DDNNF> child_array = null;
		
		File nnf_file = new File(filename);
		nnf_file.deleteOnExit();
		BufferedReader br = new BufferedReader(new FileReader(nnf_file));
		
		
		String line = br.readLine(); //Skip the initial line
		//System.out.println("First line: " + line);
		line = br.readLine();
		String[] split_line = null;
		FiniteVariable var = null;
		int val_index = 0;
		int leaf_entry = 0;
		//For each node
		while (line != null)
		{
			split_line = line.split(" ");
			
			//If it's a leaf, create it
			if (split_line[0].equals("L"))
			{
				//Recover the variable and value
				leaf_entry = new Integer(split_line[1]);
				var = (FiniteVariable)index_map.get(Math.abs(leaf_entry)-1);
				val_index = leaf_entry > 0 ? 0 : 1;
				nodes.add(new DDNNF(new Literal(var, val_index)));
			}
						
			//If it's internal, create it and link to all its children
			else
			{
				child_array = new ArrayList<DDNNF>();
				
				if (split_line[0].equals("O"))
				{
					for (int i=3; i<split_line.length; i++)
					{
						child_array.add(nodes.get(new Integer(split_line[i])));
					}	 	
					nodes.add(new DDNNF("or", child_array));
				}
				else
				{
					assert (split_line[0].equals("A"));
					for (int i=2; i<split_line.length; i++)
					{
						child_array.add(nodes.get(new Integer(split_line[i])));
					}	 	
					nodes.add(new DDNNF("and", child_array));	
				}
			}
					
			line = br.readLine();
		}
		
		br.close();
		
		//Return the last node (this is the root)
		assert (nodes.size() > 0);
		return nodes.get(nodes.size()-1);
		
	}
	
	public boolean isLeaf()
	{
		return (literal != null);
	}
	
	public boolean isOr()
	{
		return (node_type.equals("or"));
	}
	
	public boolean isAnd()
	{
		return (node_type.equals("and"));
	}
	
	public Literal getLiteral()
	{
		return literal;
	}
	
	public ArrayList<DDNNF> getChildren()
	{
		return children;
	}
	
	public String toString()
	{
		String result = "";
		
		result += "(";
		if (isLeaf())
		{
			result += literal;	
		}	
		else if (isAnd())
		{
			result += "AND";
			for (DDNNF child: children)
			{
				result += " " + child;	
			}	
		}
		else if (isOr())
		{
			result += "OR";
			for (DDNNF child: children)
			{
				result += " " + child;	
			}	
		}
		result += ")";
		
		return result;
	}	
	
	private ArrayList<DDNNF> children;
	
	private Literal literal;
	private String node_type;
	
}