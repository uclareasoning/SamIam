package edu.ucla.belief.dtree;

import java.util.*;
import java.io.*;

import edu.ucla.util.*;


abstract public class DtreeNode {

	static int nextNodeID = 0;

	Collection vars;
	private int myNodeID;

	/*These collections are populated by the function populate, and are removed by unpopulate.*/
	Collection cutset = null;
	Collection context = null;

	public DtreeNode( Collection vars) {
        Assert.notNull( vars, "DtreeNode: vars cannot be null");
		this.vars = vars;
		myNodeID = nextNodeID;
		nextNodeID++;
	}

	/** Should only be called through dtree. Sets cutset and context.*/
	abstract void populate( Collection acutset);

	/** Should only be called through dtree.*/
	void unpopulate() {
		cutset = null;
		context = null;
	}



    abstract public boolean isLeaf();

    // Can be overridden for efficiency.
    public boolean containsVar( Object var) { return vars.contains( var);}

	/** Returns a nonModifiableCollection containing the variables below this node.*/
    public Collection getVars() { return Collections.unmodifiableCollection(vars);}


	public int hashCodeSpecial() { return myNodeID;}


    /** The cutset is defined as:
     *    (1) varsL (intersect) varsR - acutset if it is a leaf node, or
     *    (2) empty Collection otherwise.
     *  <p>Returns a nonModifiableCollection.
     *  <p>Requires the dtree to have been populated.
     */
	public Collection getCutset() { return Collections.unmodifiableCollection(cutset);}


    /** The context is defined as vars (intersect) acutset.
     *  <p>Returns a nonModifiableCollection.
     *  <p>Requires the dtree to have been populated.
     */
    public Collection getContext() { return Collections.unmodifiableCollection(context);}


    /** The Cluster is defined as:
     *    (1) vars(t) if it is a leaf node, or
     *    (2) cutset (union) context otherwise.
     *  <p>Returns a copy of the cluster, user can modify as they like.
     *  <p>Requires the dtree to have been populated.
     */
    public Collection getCluster( ) {
        if( isLeaf()) {
			return new HashSet( vars);
        }
        else {
	        Collection cluster = new HashSet();
            cluster.addAll( getCutset());
            cluster.addAll( getContext());
	        return cluster;
        }
    }



	/**
     *  <p>Requires the dtree to have been populated.
     */
    int getLocalClusterSize() {
        return getCluster().size();
    }

	/**
     *  <p>Requires the dtree to have been populated.
     */
    int getLocalContextSize() {
        return getContext().size();
    }

	/**
     *  <p>Requires the dtree to have been populated.
     */
    int getLocalCutsetSize() {
        return getCutset().size();
    }


    /**Returns the number of DtreeNodes including this one and all
     * below it.
     */
    public int getNumberNodes() { return 1;}
    /**Returns the number of internal nodes including (possibly) this one and all
     * below it.
     */
    abstract public int getNumberInternalNodes();
    /**Returns the number of leaf nodes including (possibly) this one and all
     * below it.
     */
    abstract public int getNumberLeafNodes();



	/** Returns the largest cluster (actually the first it finds which is at least as
	 *  big as the parameter largest).
	 */
	abstract public Collection getLargestCluster( int largest);



    /** The largest CutsetWidth at this node or below.
     *  <p>Requires the dtree to have been populated.
     */
    abstract int getCutsetSize();
    /** The largest ContextWidth at this node or below.
     *  <p>Requires the dtree to have been populated.
     */
    abstract int getContextSize();
    /** The largest ClusterWidth at this node or below.
     *  <p>Requires the dtree to have been populated.
     */
    abstract int getClusterSize();


    /** This height is calculated by how many levels of nodes
     *   there are, not by how many edges there are between them.
     *   (e.g. leaf nodes have height of 1, their parents have
     *    a height of 2...)
     */
    abstract public int getHeight();


    /** The ArrayList ord will contain the vars in an
     *   elimination order determined by the Dtree (it is not unique).
     */
    abstract void getElimOrder( ArrayList ord);

    abstract int writeToParenFile( Writer out, Writer outtmp, int next)
        throws IOException;

	/**
		@author Keith Cascio
		@since 121302
	*/
	abstract void writeToParenFile( Writer out ) throws IOException;

	/**
     *  <p>Requires the dtree to have been populated.
     */
    abstract void writeToVCGFileNodes( Writer out, Map nodeToInt, int[] next)
        throws IOException;
    abstract void writeToVCGFileEdges( Writer out, Map nodeToInt)
        throws IOException;
	/**
     *  <p>Requires the dtree to have been populated.
     */
    abstract void writeToDOTFileNodes( Writer out, Map nodeToInt, int[] next)
        throws IOException;
    abstract void writeToDOTFileEdges( Writer out, Map nodeToInt)
        throws IOException;

}
