package edu.ucla.belief.dtree;

import java.util.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.*;



/**This class represents family dtree leaf nodes.*/
public class DtreeNodeLeaf extends DtreeNode {
    static final private boolean DEBUG_DtreeNodeLeaf = false;

    FiniteVariable child; //this is also included in vars


    /** Creates a new DtreeNodeLeaf using the Collection vars.
     *
     * @param vars A Collection of FiniteVariables.
     *   (This Collection will be used and modified, so pass copies of anything
     *    that cannot be changed.)
     */
    public DtreeNodeLeaf( Collection vars, FiniteVariable child) {
		super( vars);
        this.child = child;
    }


    public String toString() {
        return vars.toString();
    }

    public boolean isLeaf() { return true;}

    public FiniteVariable child() { return child;}


	public void populate( Collection acutset) {
		cutset = Collections.EMPTY_SET;
		context = new HashSet( vars);
		context.retainAll( acutset);
	}



    public int getNumberInternalNodes() {
        return 0;
    }
    public int getNumberLeafNodes() {
        return 1;
    }


	public Collection getLargestCluster( int largest) {
		if( getLocalClusterSize() == largest) {
			return getCluster();
		}
		else {
			return null;
		}
	}


    int getClusterSize() {
        return getLocalClusterSize();
    }

    int getContextSize() {
        return getLocalContextSize();
    }

    int getCutsetSize() {
        return getLocalCutsetSize();
    }



    public int getHeight() {
        return 1;
    }

    void getElimOrder( ArrayList ord) {
        if( ord == null) { ord = new ArrayList();}
        Collection elim = getCluster();
        elim.removeAll( getContext());
        ord.addAll( elim);
    }


    //out: Write out the dtree here
    //outtmp: Write out a mapping of numbers to families here
    int writeToParenFile( Writer out, Writer outtmp, int next)
        throws IOException{

        out.write( "" + next);
        outtmp.write( next + ":" + vars + "\n");
        return next+1;
    }

    /**
        @author Keith Cascio
        @since 121302
    */
    public void writeToParenFile( Writer out ) throws IOException
    {
        out.write( child.getID() );
    }


    void writeToVCGFileNodes( Writer out, Map nodeToInt, int[] next)
        throws IOException {

        Assert.notNull( nodeToInt, "DtreeNode: nodeToInt cannot be null");
        Assert.notNull( next, "DtreeNode: next cannot be null");
        Assert.condition( next.length==1, "DtreeNode: next.length must be 1");


        Integer i = new Integer( next[0]);
        nodeToInt.put( this, i);
        next[0] = next[0] + 1;

        out.write( "node: { title: \"" + i + "\"" + "\n");
        out.write( "label: \"" + vars + "\"" + "\n");
        out.write( "shape: box}" + "\n");
    }

    void writeToVCGFileEdges( Writer out, Map nodeToInt)
        throws IOException {
        //don't do anything
    }




    void writeToDOTFileNodes( Writer out, Map nodeToInt, int[] next)
        throws IOException {

        Assert.notNull( nodeToInt, "DtreeNode: nodeToInt cannot be null");
        Assert.notNull( next, "DtreeNode: next cannot be null");
        Assert.condition( next.length==1, "DtreeNode: next.length must be 1");

        Integer i = new Integer( next[0]);
        nodeToInt.put( this, i);
        next[0] = next[0] + 1;

        out.write( "node" + i +
                   "[label = \"<f0> |<f1> " + vars +
                   "|<f2> \"];" + "\n");
    }

    void writeToDOTFileEdges( Writer out, Map nodeToInt)
        throws IOException {
        //don't do anything
    }

}//end class DtreeNodeLeaf
