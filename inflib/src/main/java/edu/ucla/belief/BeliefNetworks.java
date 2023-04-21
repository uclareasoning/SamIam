package edu.ucla.belief;

import java.util.*;
import edu.ucla.structure.*;
import edu.ucla.belief.io.*;
//{superfluous} import edu.ucla.belief.io.hugin.HuginNet;
import edu.ucla.belief.tree.*;
import edu.ucla.belief.inference.*;
//{superfluous} import java.io.IOException;
//{superfluous} import javax.swing.ProgressMonitor;

/** Contains a collection of static methods relating to belief networks
	that are sometimes useful. This class can not be instantiated. */
public class BeliefNetworks{
	private  BeliefNetworks(){}

	/**
	* Generates the graph induced by the potential map. The nodes in the graph
	* are the keys in the mapping. The values in the mapping are Potentials.
	*/
	public static DirectedGraph inducedGraph(Map potentials)
	{
		DirectedGraph dg = new HashDirectedGraph(potentials.size());
		for (Iterator iter = potentials.keySet().iterator();
		iter.hasNext();)
		{
			Object node = iter.next();
			Potential p = (Potential) potentials.get(node);
			dg.add(node);
			for (Iterator viter = p.variables().iterator();
			viter.hasNext();)
			{
				Object n2 = viter.next();
				if( !node.equals(n2) ) dg.addEdge(n2, node);
			}
		}

		return dg;
	}

	/**
	* Generates a random directed graph using nodes as the vertices.
	* Creates a random graph whose width is usually relatively close
	* to the value in connectivity.
	*/
	public static DirectedGraph randomGraph( Object[] nodes, int connectivity )
	{
		int nodeCount = nodes.length;
		int[] numparents = new int[nodeCount];
		for (int i = 0; i < nodeCount; i++)
		{
			double r = Math.random();
			if (r <= .2) numparents[i] = 0;
			else if (r <= .3) numparents[i] = 1;
			else if (r <= .5) numparents[i] = 2;
			else if (r <= .75) numparents[i] = 3;
			else if (r <= .95) numparents[i] = 4;
			else numparents[i] = 5;

			numparents[i] = Math.min(numparents[i], i);
			numparents[i] = Math.min(numparents[i], connectivity);
		}

		DirectedGraph graph = new HashDirectedGraph();
		for (int i = 0; i < nodeCount; i++)
		{
			Object node = nodes[i];
			graph.add(node);
			for (int j = 0; j < numparents[i];)
			{
				int parent;
				if (i > connectivity) parent = (int)(i - 1 - Math.floor(connectivity * Math.random()));
				else parent = (int) Math.floor(i * Math.random());
				Object p = nodes[parent];
				if (graph.addEdge(p, node)) j++;
			}
		}

		return graph;
	}

	/**
	* Returns a cpt where the values in the table are random numbers
	* consistent with it being a CPT.
	*/
	public static Table randomCPT(Collection condvars, FiniteVariable var)
	{
		int vsize = var.size();
		int size = vsize;
		for (Iterator iter = condvars.iterator(); iter.hasNext();)
		{
			size *= ((FiniteVariable) iter.next()).size();
		}

		double[] prob = new double[size];
		for (int i = 0; i < size; i += vsize)
		{
			double total = 0;
			for (int j = 0; j < vsize; j++)
			{
				prob[i + j] = Math.random();
				total += prob[i + j];
			}

			for (int j = 0; j < vsize; j++)
			{
				prob[i + j] /= total;
			}
		}

		ArrayList varlist = new ArrayList(condvars);
		varlist.add(var);
		FiniteVariable[] vararray = new FiniteVariable[varlist.size()];
		varlist.toArray(vararray);
		return new Table(vararray, prob);
	}

	/**
	* returns a belief network with random CPTs.
	* The nodes of the the graph must be finite variables.
	*/
	public static BeliefNetwork randomNetwork(DirectedGraph g)
	{
		Map tables = new HashMap(g.size());
		Iterator nodeIter = g.iterator();
		while (nodeIter.hasNext())
		{
			FiniteVariable node = (FiniteVariable) nodeIter.next();
			tables.put(node, randomCPT(g.inComing(node), node));
		}

		return new BeliefNetworkImpl(g, tables);
	}

	/**
	* Creates a random network where each edge has probability edgeProbability
	* of being included. All variables are binary.
	*/
	public static BeliefNetwork randomNetwork(int nodeCount,
		double edgeProbability)
	{
		FiniteVariable[] vars = createBooleanVars(nodeCount);
		DirectedGraph g = Graphs.randomDAG(vars, edgeProbability);
		return randomNetwork(g);
	}

	private static FiniteVariable[] createBooleanVars(int nodeCount)
	{
		String[] vals =
		{
			"T", "F"
		};

		FiniteVariable[] vars = new FiniteVariable[nodeCount];
		for (int i = 0; i < nodeCount; i++)
		{
			vars[i] = new FiniteVariableImpl("v" + Integer.toString(i), vals);
		}

		return vars;
	}

	/**
	* Generates a random network consisting of boolean variables.
	* @param nodeCount the number of variables.
	* @param connectivity the connectity of the network as described in
	* randomGraph.
	*/
	public static BeliefNetwork randomNetwork(int nodeCount, int connectivity)
	{
		FiniteVariable[] vars = createBooleanVars(nodeCount);
		DirectedGraph g = randomGraph(vars, connectivity);
		return randomNetwork(g);
	}

	public static boolean satisfiesCPTProperty(Table t,
		FiniteVariable var, double epsilon)
	{
		return t.satisfiesCPTProperty( var, epsilon);
	}

	public static boolean ensureCPTProperty(BeliefNetwork bn)
	{
		//System.out.println( "BeliefNetworks.ensureCPTProperty(bn)" );
		boolean ok = true;
		for (Iterator iter = bn.iterator(); iter.hasNext();)
		{
			FiniteVariable fv = (FiniteVariable) iter.next();
			//ok &= ensureCPTProperty((Table) bn.getCPT(fv), fv);
			ok &= ensureCPTProperty( fv );
		}

		return ok;
	}

	public static boolean ensureCPTProperty( FiniteVariable var )
	{
		//System.out.println( "BeliefNetworks.ensureCPTProperty( " + var + " )" );
		CPTShell shell = var.getCPTShell( edu.ucla.belief.io.dsl.DSLNodeType.CPT );
		if( shell instanceof TableShell ) return ensureCPTProperty( shell.getCPT(), var );
		else return true;
	}

	public static boolean ensureCPTProperty(Table t, FiniteVariable var)
	{
		boolean ret = t.ensureCPTProperty( var);
		//if( !ret ) System.err.println( "failed cpt property: " + var );
		return ret;
	}

	/**
	*
	* Assumes that the CPT variable is indexed as the least significant entry.
	*/
	public static boolean ensureCPTProperty(FiniteVariable var,
		double[] oldValues, double[] newValues)
	{
		boolean ok = true;
		for (int i = 0; i < oldValues.length; i += var.size())
		{
			double sum = 0;
			for (int j = i; j < i + var.size(); j++)
			{
				sum += oldValues[j];
			}

			if (sum == 0)
			{
				ok = false;
			}

			for (int j = i; j < i + var.size(); j++)
			{
				newValues[i] = oldValues[i] / sum;
			}
		}

		return ok;
	}
}
