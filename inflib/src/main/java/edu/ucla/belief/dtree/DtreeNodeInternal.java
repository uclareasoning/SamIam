package edu.ucla.belief.dtree;

import java.util.*;
import java.io.*;

import edu.ucla.util.*;


/** This class represents Internal DtreeNode objects.
 *
 * @author David Allen
 */
public class DtreeNodeInternal extends DtreeNode {
    static final private boolean DEBUG_DtreeNodeInternal = false;

    DtreeNode left;
    DtreeNode right;




    /** Creates a DtreeNodeInternal using the two parameters as children.
     *
     * @param left A non-Null DtreeNode.
     * @param right A non-Null DtreeNode.
     * @throws IllegalArgumentException if left.equals( right)
     */
    public DtreeNodeInternal( DtreeNode left, DtreeNode right) {
		super( vars( left, right));
        setChildren( left, right);
    }



    public String toString() {
        return "( " + left.toString() + " " + right.toString() + " )";
    }

    public boolean isLeaf() { return false;}

    public DtreeNode left() { return left;}
    public DtreeNode right() { return right;}


	void populate( Collection acutset) {

		//cutset
		{
			cutset = new HashSet( left.vars);
			cutset.retainAll( right.vars);
			cutset.removeAll( acutset);
		}
		//context
		{
			context = new HashSet( vars);
			context.retainAll( acutset);
		}
		//children
        Collection newacut = new HashSet( acutset);
        newacut.addAll( cutset);

		left.populate( newacut);
		right.populate( newacut);
	}

	void unpopulate() {
		super.unpopulate();
		left.unpopulate();
		right.unpopulate();
	}



    /** Sets the left and right child for an internal node.
	 *
	 * <p>Currently this unpopulates the node and its children, but can't notify dtree of this.
     *
     * @param lft A non-Null DtreeNode.
     * @param rt A non-Null DtreeNode.
     * @throws IllegalArgumentException if lft.equals( rt)
     */
    private void setChildren( DtreeNode lft, DtreeNode rt) {

        Assert.notNull( lft, "DtreeNodeInternal: cannot have null children (lft)");
        Assert.notNull( rt, "DtreeNodeInternal: cannot have null children (rt)");

        if( lft.equals( rt)) {
            throw new IllegalArgumentException(
                            "DtreeNodeInternal: children cannot be equal");
        }

        left = lft;
        right = rt;

        this.vars = vars( left, right);
        if( cutset != null) { throw new IllegalStateException("changed the children of a node which was populated");}
        unpopulate();  //TODO: no way of notifying the DTREE of this!
    }




    public int getNumberNodes() {
        return 1 + left.getNumberNodes() + right.getNumberNodes();
    }
    public int getNumberInternalNodes() {
        return 1 + left.getNumberInternalNodes() + right.getNumberInternalNodes();
    }
    public int getNumberLeafNodes() {
        return left.getNumberLeafNodes() + right.getNumberLeafNodes();
    }


	public Collection getLargestCluster( int largest) {
		if( getLocalClusterSize() == largest) {
			return getCluster();
		}
		Collection ret = left.getLargestCluster( largest);
		if( ret != null) {
			return ret;
		}
		ret = right.getLargestCluster( largest);
		return ret;
	}




    int getClusterSize() {
        int width = getLocalClusterSize();
        width = Math.max( width, left.getClusterSize());
        width = Math.max( width, right.getClusterSize());
        return width;
    }

    int getContextSize() {
        int width = getLocalContextSize();
        width = Math.max( width, left.getContextSize());
        width = Math.max( width, right.getContextSize());
        return width;
    }

    int getCutsetSize() {
        int width = getLocalCutsetSize();
        width = Math.max( width, left.getCutsetSize());
        width = Math.max( width, right.getCutsetSize());
        return width;
    }

    public int getHeight() {
        return Math.max(left.getHeight(), right.getHeight()) + 1;
    }


    void getElimOrder( ArrayList ord) {
        if( ord == null) { ord = new ArrayList();}

        //add children's
        left.getElimOrder( ord);
        right.getElimOrder( ord);

        //add own
        ord.addAll( cutset);
    }


    //out: Write out the dtree here
    //outtmp: Write out a mapping of numbers to families here
    int writeToParenFile( Writer out, Writer outtmp, int next)
        throws IOException{

        int local = next;
        out.write( "( ");
        local = left.writeToParenFile(out, outtmp, local);
        out.write( " ");
        local = right.writeToParenFile(out, outtmp, local);
        out.write( " )");
        return local;
    }

	/**
		@author Keith Cascio
		@since 121302
	*/
	public void writeToParenFile( Writer out ) throws IOException
	{
		out.write( "( ");
		left.writeToParenFile(out);
		out.write( " ");
		right.writeToParenFile(out);
		out.write( " )");
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
        out.write( " label: \"" +
                   "Cut: " + getCutset( ) + "\n" +
                   "Cxt: " + getContext( ) + "\"" + "\n");
        out.write( "shape: ellipse}" + "\n");

        left.writeToVCGFileNodes( out, nodeToInt, next);
        right.writeToVCGFileNodes( out, nodeToInt, next);

    }

    void writeToVCGFileEdges( Writer out, Map nodeToInt)
        throws IOException {

        Assert.notNull( nodeToInt, "DtreeNode: nodeToInt cannot be null");

        out.write( "edge: {" + "\n");
        out.write( "sourcename: \"" + nodeToInt.get(this) + "\"" + "\n");
        out.write( "targetname: \"" + nodeToInt.get(left) + "\"" + "\n");
        out.write( "}" + "\n");

        out.write( "edge: {" + "\n");
        out.write( "sourcename: \"" + nodeToInt.get(this) + "\"" + "\n");
        out.write( "targetname: \"" + nodeToInt.get(right) + "\"" + "\n");
        out.write( "}" + "\n");

        left.writeToVCGFileEdges( out, nodeToInt);
        right.writeToVCGFileEdges( out, nodeToInt);
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
                   "[label = \"<f0> " + "Cut:" + getCutset() +
                   "|<f1> |<f2> " + "Cxt:" + getContext() +
                   "\"];" + "\n");
        left.writeToDOTFileNodes( out, nodeToInt, next);
        right.writeToDOTFileNodes( out, nodeToInt, next);
    }

    void writeToDOTFileEdges( Writer out, Map nodeToInt)
        throws IOException {

        Assert.notNull( nodeToInt, "DtreeNode: nodeToInt cannot be null");

        out.write( "\"node" + nodeToInt.get(this) + "\":f0 -> \"node" +
                   nodeToInt.get(left) + "\":f1;" + "\n");
        out.write( "\"node" + nodeToInt.get(this) + "\":f2 -> \"node" +
                   nodeToInt.get(right) + "\":f1;" + "\n");

        left.writeToDOTFileEdges( out, nodeToInt);
        right.writeToDOTFileEdges( out, nodeToInt);
    }


	private static Collection vars( DtreeNode left, DtreeNode right) {
		Collection ret = new HashSet();
		ret.addAll( left.vars);
		ret.addAll( right.vars);
		return ret;
	}

}//end class DtreeNodeInternal





