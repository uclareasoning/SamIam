package edu.ucla.belief;
import java.util.*;
import edu.ucla.structure.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.tree.*;
import edu.ucla.belief.inference.*;
//{superfluous} import java.io.IOException;
/**
* Contains a collection of static methods relating to generating random networks
*/
public class RandomNetworks {
    private static final Random rand=new Random();
    private RandomNetworks() {
    }
    /**
     * Generates a random directed graph using nodes as the vertices.
     * Creates a random graph whose width is usually relatively close
     * to the value in connectivity.
     */
    public static DirectedGraph randomGraph(Object[] nodes,
            int connectivity) {
        int nodeCount = nodes.length;
        int[] numparents = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            double r = Math.random();
            if (r <= .2) {
                numparents[i] = 0;
            } else if (r <= .3) {
                numparents[i] = 1;
            } else if (r <= .5) {
                numparents[i] = 2;
            } else if (r <= .75) {
                numparents[i] = 3;
            } else if (r <= .95) {
                numparents[i] = 4;
            } else {
                numparents[i] = 5;
            }
            numparents[i] = Math.min(numparents[i], i);
            numparents[i] = Math.min(numparents[i], connectivity);
        }
        DirectedGraph graph = new HashDirectedGraph();
        for (int i = 0; i < nodeCount; i++) {
            Object node = nodes[i];
            graph.add(node);
            for (int j = 0; j < numparents[i];) {
                int parent;
                if (i > connectivity) {
                    parent = (int)(i - 1 -
                            Math.floor(connectivity * Math.random()));
                } else {
                    parent = (int) Math.floor(i * Math.random());
                }
                Object p = nodes[parent];
                if (graph.addEdge(p, node)) {
                    j++;
                }
            }
        }
        return graph;
    }
    /**
     * Returns a deterministic CPT(For each instance of the parents,
     *  one instance of the variable has value 1, the rest 0.
     */
    public static Table deterministicCPT(Collection condvars,
            FiniteVariable var) {
        int vsize = var.size();
        int size = vsize;
        for (Iterator iter = condvars.iterator(); iter.hasNext();) {
            size *= ((FiniteVariable) iter.next()).size();
        }
        double[] prob = new double[size];
        for (int i = 0; i < size; i += vsize) {
            double total = 0;
            int ind = (int)(Math.random() * vsize);
            prob[i + ind] = 1;
        }
        ArrayList varlist = new ArrayList(condvars);
        varlist.add(var);
        FiniteVariable[] vararray = new FiniteVariable[varlist.size()];
        varlist.toArray(vararray);
        return new Table(vararray, prob);
    }
    /**
     * Returns a cpt where the values in the table are random numbers
     * consistent with it being a CPT.
     */
    public static Table randomCPT(Collection condvars, FiniteVariable var) {
        int vsize = var.size();
        int size = vsize;
        for (Iterator iter = condvars.iterator(); iter.hasNext();) {
            size *= ((FiniteVariable) iter.next()).size();
        }
        double[] prob = new double[size];
        for (int i = 0; i < size; i += vsize) {
            double total = 0;
            for (int j = 0; j < vsize; j++) {
                prob[i + j] = Math.random();
                total += prob[i + j];
            }
            for (int j = 0; j < vsize; j++) {
                prob[i + j] /= total;
            }
        }
        ArrayList varlist = new ArrayList(condvars);
        varlist.add(var);
        FiniteVariable[] vararray = new FiniteVariable[varlist.size()];
        varlist.toArray(vararray);
        return new Table(vararray, prob);
    }
    public static Table randomCPT(Collection condvars, FiniteVariable var,double bias) {
        int vsize = var.size();
        if(vsize!=2){
            throw new IllegalArgumentException("Binary variable required");
        }
        int size = vsize;
        for (Iterator iter = condvars.iterator(); iter.hasNext();) {
            size *= ((FiniteVariable) iter.next()).size();
        }
        double[] prob=new double[size];
        for(int i=0;i<prob.length;i+=2){
            double val=bias*Math.random();
            if(rand.nextBoolean()){
                prob[i]=val;
                prob[i+1]=1-val;
            }else{
                prob[i]=1-val;
                prob[i+1]=val;                 
            }
        }
        ArrayList varlist = new ArrayList(condvars);
        varlist.add(var);
        FiniteVariable[] vararray = new FiniteVariable[varlist.size()];
        varlist.toArray(vararray);
        return new Table(vararray, prob);
    }
    /**
     * Returns a belief network with biased CPTs.  The FiniteVariables must
     */
    public static BeliefNetwork randomNetwork(DirectedGraph g,double bias){
        Map tables=new HashMap(g.size());
        Iterator nodeIter=g.iterator();
        while (nodeIter.hasNext()) {
            FiniteVariable node = (FiniteVariable) nodeIter.next();
            if(bias==0 && g.inDegree(node)==0){
                tables.put(node,randomCPT(g.inComing(node),node));
            }else{
                tables.put(node, randomCPT(g.inComing(node), node,bias));
            }
        }
        return new BeliefNetworkImpl(g, tables);
    }
    /**
     * returns a belief network with uniformly random CPTs.
     * The nodes of the the graph must be finite variables.
     */
    public static BeliefNetwork randomNetwork(DirectedGraph g) {
        Map tables = new HashMap(g.size());
        Iterator nodeIter = g.iterator();
        while (nodeIter.hasNext()) {
            FiniteVariable node = (FiniteVariable) nodeIter.next();
            tables.put(node, randomCPT(g.inComing(node), node));
        }
        return new BeliefNetworkImpl(g, tables);
    }
    /**
     * Creates a deterministic network from the graph of variables supplied.
     */
    public static BeliefNetwork deterministicNetwork(DirectedGraph g) {
        Map tables = new HashMap(g.size());
        Iterator nodeIter = g.iterator();
        while (nodeIter.hasNext()) {
            FiniteVariable node = (FiniteVariable) nodeIter.next();
            if (g.inComing(node).size() == 0) {
                tables.put(node, randomCPT(g.inComing(node), node));
            } else {
                tables.put(node, deterministicCPT(g.inComing(node), node));
            }
        }
        return new BeliefNetworkImpl(g, tables);
    }
    /**
     * Creates a random network where each edge has probability edgeProbability
     * of being included. All variables are binary.
     */
    public static BeliefNetwork randomNetwork(int nodeCount,
            double edgeProbability) {
        FiniteVariable[] vars = createBooleanVars(nodeCount);
        DirectedGraph g = Graphs.randomDAG(vars, edgeProbability);
        return randomNetwork(g);
    }
    public static BeliefNetwork randomNetwork(int nodeCount,
            double edgeProbability,double bias) {
        FiniteVariable[] vars = createBooleanVars(nodeCount);
        DirectedGraph g = Graphs.randomDAG(vars, edgeProbability);
        return randomNetwork(g,bias);
    }
    /**
     * Creates a deterministic network from the random graph generated using
     * the supplied parameters.
     */
    public static BeliefNetwork deterministicNetwork(int nodeCount,
            double edgeProbability) {
        FiniteVariable[] vars = createBooleanVars(nodeCount);
        DirectedGraph g = Graphs.randomDAG(vars, edgeProbability);
        return deterministicNetwork(g);
    }
    /**
     * Generates a random network consisting of boolean variables.
     * @param nodeCount the number of variables.
     * @param connectivity the connectity of the network as described in
     * randomGraph.
     */
    public static BeliefNetwork randomNetwork(int nodeCount,
            int connectivity) {
        FiniteVariable[] vars = createBooleanVars(nodeCount);
        DirectedGraph g = randomGraph(vars, connectivity);
        return randomNetwork(g);
    }
    public static BeliefNetwork randomNetwork(int nodeCount,
            int connectivity,double bias) {
        FiniteVariable[] vars = createBooleanVars(nodeCount);
        DirectedGraph g = randomGraph(vars, connectivity);
        return randomNetwork(g,bias);
    }
    /**
     * Generates a deterministic network from the random graph generated
     * using the supplied parameters.
     */
    public static BeliefNetwork deterministicNetwork(int nodeCount,
            int connectivity) {
        FiniteVariable[] vars = createBooleanVars(nodeCount);
        DirectedGraph g = randomGraph(vars, connectivity);
        return deterministicNetwork(g);
    }
    private static FiniteVariable[] createBooleanVars(int nodeCount) {
        String[] vals = { "T", "F" };
        FiniteVariable[] vars = new FiniteVariable[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            vars[i] = new FiniteVariableImpl("v" + Integer.toString(i), vals);
        }
        return vars;
    }
}
