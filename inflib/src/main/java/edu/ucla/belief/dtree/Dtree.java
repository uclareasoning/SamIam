package edu.ucla.belief.dtree;

import edu.ucla.belief.*;

import java.util.*;
import java.io.*;

import edu.ucla.structure.*;
import edu.ucla.util.*;



/** This class represents dtree objects.
 *
 * @author David Allen
 */

public class Dtree {

    static final private boolean DEBUG_Dtree = false;
	static final private boolean DEBUG_DtreeVerbose = false;

	/** @author Keith Cascio @since 031103 */
	//public double userDouble = (double)-1;
	/** @author Keith Cascio @since 050103 */
	public CreationMethod myCreationMethod;

    private DtreeNode root;



    /** Create a Dtree with inRoot as the root of the tree.
     *  (i.e. no copying is done)
     *
     * @throws DtreeCreationException if inRoot is null.
     */
    public Dtree( DtreeNode inRoot) {
        if( inRoot == null) throw new DtreeCreationException("Dtrees cannot have a null root node.");
        root = inRoot;
    }



    /** Create a Dtree using the createAlgo algorithm and the DirectedGraph dag.
     *
     * @param dag A DirectedGraph to create the Dtree from.
     * @param createAlgo The algorithm to use.
     * @throws DtreeCreationException if could not create a Dtree.
     */
    public Dtree( DirectedGraph dag, Create createAlgo) {
        this( convertDAGtoMap( dag, new HashMap()), createAlgo);
    }



    /** Create a Dtree using the createAlgo algorithm.
     *
     * @param varToFamily A Map of a FiniteVariable to a Collection of FiniteVariables (Its Family, including itself).
     *   (These Collections will be used and modified, so pass copies of anything that cannot
     *   be changed.)
     * @param createAlgo The algorithm to use.
     * @throws DtreeCreationException if could not create a Dtree.
     */
    public Dtree( Map varToFamily, Create createAlgo) {
        Assert.notNull( varToFamily, "Dtree: varToFamily cannot be null");
        Assert.notNull( createAlgo, "Dtree: createAlgo cannot be null");

        Collection nodes = colOfColOfFVarsToColDtreeNodes( varToFamily, null);
        root = createAlgo.create( nodes);

        //could have been an empty network
        if( root == null) throw new DtreeCreationException("Dtrees cannot have a null root node.");
    }









	/**
		@author Keith Cascio
		@since 022703
	*/
	public String toString()
	{
		//return root.toString();
		StringWriter writer = new StringWriter( (int)1024 );
		try{
			root.writeToParenFile( writer );
			writer.close();
		}catch( Exception e){
			return null;
		}
		return writer.toString();
	}

    public DtreeNode root() { return root;}
	public int getCutsetSize() { populate(); return root.getCutsetSize();}
	public int getContextSize() { populate(); return root.getContextSize();}
	public int getClusterSize() { populate(); return root.getClusterSize();}
	/** Same as getClusterSize - 1.*/
    public int getWidth() { populate(); return root.getClusterSize()-1;}
    public int getHeight() { return root.getHeight();}
    public void getElimOrder( ArrayList ord) { populate(); root.getElimOrder( ord);}



	private boolean populated = false;
	public void populate() {
		if( populated) { return;}
		populated = true;
		root.populate( Collections.EMPTY_SET);
	}
	public void unpopulate() {
		if( !populated) { return;}
		populated = false;
		root.unpopulate();
	}



    /**Get all the vars contained within a collection of DtreeNodes.*/
    static public Collection getVars( Collection A) {
        return getVars( A, null);
    }
    /**Get all the vars contained within a collection of DtreeNodes.*/
    static public Collection getVars( Collection A, Collection ret) {
        if( ret == null) { ret = new HashSet();}
        for( Iterator itr = A.iterator(); itr.hasNext();) {
            DtreeNode dn = (DtreeNode)itr.next();
            ret.addAll( dn.getVars());
        }
        return ret;
    }


//     /**When finished, ret will contain a map of the variables to
//      * HashSets of DtreeNodes containing those variables.
//      *
//      * @param nodes A collection of DtreeNodes.
//      * @param ret A Map (not necessarily empty) of variables (Objects)
//      *  to Collections of DtreeNodes.
//      */
//     static public
//         Map createVarsToLeafNodes( Collection nodes, Map ret) {

//         Assert.notNull( ret, "Dtree: ret cannot be null");
//         Assert.notNull( nodes, "Dtree: nodes cannot be null");

//         for( Iterator itr1 = nodes.iterator(); itr1.hasNext();) {
//             DtreeNode dn = (DtreeNode)itr1.next();

//             for( Iterator itr2 = dn.getVars().iterator(); itr2.hasNext();) {
//                 Object var = itr2.next();
//                 Collection col = (Collection)ret.get( var);
//                 if( col == null) {
//                     col = new HashSet();
//                     ret.put( var, col);
//                 }
//                 col.add( dn);
//             }
//         }
//         return ret;
//     }



//     /**Create (append to) a map of variables to a collection of
//      *  neighboring variables.
//      *
//      * @param nodes A collection of DtreeNodes.
//      * @param ret A Map (not necessarily empty) of variables (Objects)
//      *  to Collections of variables (Objects).
//      */
//     static public
//         Map createVarsToNeighbors( Collection nodes, Map varsToNeighs) {

//         Assert.notNull( nodes, "Dtree: nodes cannot be null");
//         Assert.notNull( varsToNeighs, "Dtree: varsToNeighs cannot be null");

//         Collection vars = new HashSet();
//         for( Iterator itr1 = nodes.iterator(); itr1.hasNext();) {
//             DtreeNode n = (DtreeNode)itr1.next();
//             n.getVars( vars);

//             for( Iterator itr2 = vars.iterator(); itr2.hasNext();) {
//                 Object v = itr2.next();
//                 Collection nei = (Collection)varsToNeighs.get( v);
//                 if( nei == null) {
//                     nei = new HashSet();
//                     varsToNeighs.put( v, nei);
//                 }
//                 nei.addAll( vars);
//                 nei.remove( v);
//             }
//             vars.clear();
//         }
//         return varsToNeighs;
//     }

	public static String getTagName() { return "dtree"; }

	/**
		@author Keith Cascio
		@since 121302
	*/
	public void write( File ofile ) throws IOException
	{
		Writer out = new FileWriter( ofile );
		root.writeToParenFile( out );
		out.flush();
		out.close();
	}

	/**
		@author Keith Cascio
		@since 022703
	*/
	public void write( Writer out ) throws IOException
	{
		write( out, new CharArrayWriter() );
	}

	/**
		@author Keith Cascio
		@since 022703
	*/
	public void write( Writer out, CharArrayWriter outtmp ) throws IOException
	{
		root.writeToParenFile( out, outtmp, 1 );
		outtmp.flush();
		outtmp.close();
		out.write( "\n" + outtmp.toString() );
	}

    /** Writes this Dtree to a VCG file.
     *
     * @param out The file to write the object to.
     * @throws IOException if there is a file error.
     */
    public void writeToVCGFile( Writer out)
    throws IOException {

        Assert.notNull( out, "Dtree: out cannot be null");

        out.write( "" + "\n");
        out.write( "graph: { title: \"dtree\"" + "\n");
        out.write( "layoutalgorithm: tree" + "\n");
        out.write( "treefactor: 0.9" + "\n");

		populate();

        Map nodeToInt = new HashMap();
        int[] t = {1};
        root.writeToVCGFileNodes( out, nodeToInt, t);
        root.writeToVCGFileEdges( out, nodeToInt);

        out.write( "}" + "\n");
    }


    /** Writes this Dtree to a DOT file.
     *
     * @param out The file to write the object to.
     * @throws IOException if there is a file error.
     */
    public void writeToDOTFile( Writer out)
    throws IOException {

        Assert.notNull( out, "Dtree: out cannot be null");

        out.write( "digraph g {" + "\n");
        out.write( "node [shape = record, height=.1];" + "\n");
        out.write( "size = \"10, 7.5\";" + "\n");
        out.write( "rotate = \"90\";" + "\n");
        out.write( "ratio = \"fill\";" + "\n");

		populate();

        Map nodeToInt = new HashMap();
        int[] t = {1};
        root.writeToDOTFileNodes( out, nodeToInt, t);
        root.writeToDOTFileEdges( out, nodeToInt);

        out.write( "}" + "\n");
    }



    /** Will determine which dtree is better and return it.*/
    public static Dtree selectBestDtree( Dtree n1, Dtree n2) {
		n1.populate();
		n2.populate();

        int v1 = n1.getClusterSize();
        int v2 = n2.getClusterSize();

        if( v1 < v2) { return n1;}
        else if( v1 > v2) { return n2;}

        v1 = n1.getContextSize();
        v2 = n2.getContextSize();

        if( v1 < v2) { return n1;}
        else if( v1 > v2) { return n2;}

        v1 = n1.getHeight();
        v2 = n2.getHeight();

        if( v1 < v2) { return n1;}
        else if( v1 > v2) { return n2;}

        return n1;
    }






    /** Each instantiation of this class represents a way of generating
     *  dtrees.  They do this by overriding the create function.
     */
    public static class Create {

        public String abrev() { return "Rndm";}
        public String name() { return "Create-Random";}



        /** Create a dtree from the data.
         *
         *  <p>This version will create an ordering by iteratively connecting the nodes
         *     with the smallest height values.
         *
         *  @param data A Collection of DtreeNodes.
         */
        public DtreeNode create( Collection data) {
            final boolean DEBUG_VERBOSE = false && DEBUG_Dtree;
            final boolean DEBUG_VERBOSE2 = false && DEBUG_Dtree;

            Assert.notNull( data, "Create: data cannot be null");

            if( DEBUG_VERBOSE ) Definitions.STREAM_VERBOSE.println("Creating Dtree with: " + name());

            DtreeNode ret;

            SortedSet openNodes = new TreeSet( new DtreeComparatorHeightLowToHigh());
            openNodes.addAll( data);

             if( DEBUG_VERBOSE ) Definitions.STREAM_VERBOSE.println("There are " + openNodes.size() + " leaf nodes");
             if( DEBUG_VERBOSE ) Definitions.STREAM_VERBOSE.println("Total Data:" + data);

            //link leaf nodes up with internal nodes
            while( openNodes.size() > 1) {
                DtreeNode n1 = (DtreeNode)openNodes.first();
                boolean b1 = openNodes.remove( n1);
                DtreeNode n2 = (DtreeNode)openNodes.first();
                boolean b2 = openNodes.remove( n2);

				boolean fail = false;

                if( !b1 || !b2) {
					Definitions.STREAM_VERBOSE.println("a remove didn't work: " + b1 + " " + b2 + " " + n1 + " " + n2 + "\n\n" + openNodes + "\n\n" + data);
					fail = true;
				}
				else if( n1 == n2) {
					Definitions.STREAM_VERBOSE.println("error (==): " + b1 + " " + b2 + " " + n1 + " " + n2 + "\n\n" + openNodes + "\n\n" + data);
					fail = true;
				}
				else if( n1.equals( n2)) {
					Definitions.STREAM_VERBOSE.println("error (equals): " + b1 + " " + b2 + " " + n1 + " " + n2 + "\n\n" + openNodes + "\n\n" + data);
					fail = true;
				}

				if( fail) {
					Definitions.STREAM_VERBOSE.println("failed!");
					for( Iterator itr = openNodes.iterator(); itr.hasNext();) {
						Object o = itr.next();
						Definitions.STREAM_VERBOSE.print(", " + ((DtreeNode)o).getHeight());
					}
					Definitions.STREAM_VERBOSE.println("\n");
					for( Iterator itr = openNodes.iterator(); itr.hasNext();) {
						Object o = itr.next();
						Definitions.STREAM_VERBOSE.print(", " + ((DtreeNode)o).hashCode());
					}
					Definitions.STREAM_VERBOSE.println("\n");
					for( Iterator itr = openNodes.iterator(); itr.hasNext();) {
						Object o = itr.next();
						Definitions.STREAM_VERBOSE.print(", " + ((DtreeNode)o).hashCodeSpecial());
					}
					Definitions.STREAM_VERBOSE.println("\n");
				}

                openNodes.add( new DtreeNodeInternal( n1, n2));
            }

            Assert.condition( openNodes.size() <= 1, "Create: Did not create valid Dtree");

            //if data was not empty, set the root
            if( openNodes.size() > 0) {
                ret = (DtreeNode)openNodes.first();
                openNodes.remove( ret);
            }
            else {
                ret = null;
            }
            return ret;
        }
    }




    /** Convert a DirectedGraph to a Map from a FiniteVariable to
     *   a collection of FiniteVariables (its family, & itself).
     *
     * @param varToFamily The Map which will be returned (if null, a new HashMap
     *   will be created and returned with the result).
     */
    static public
        Map convertDAGtoMap( DirectedGraph dag, Map varToFamily) {

        Assert.notNull( dag, "Dtree: dag cannot be null");
        if( varToFamily == null) { varToFamily = new HashMap();}

        for( Iterator itrv = dag.vertices().iterator(); itrv.hasNext();) {
            Object vertex = itrv.next();
            HashSet family = new HashSet( dag.inComing( vertex));
            family.add( vertex);
            varToFamily.put( vertex, family);
        }
        return varToFamily;
    }


    /** Convert a Collection of Collections of FiniteVariables
     *  (Collection of Families or UndirectedEdges) to a Collection of DtreeNodes.
     *
     * @param varToFamily A Map of a FiniteVariable to a Collection of FiniteVariables (Its Family, including itself).
     *   (These Collections will be used and modified, so pass copies of anything that cannot
     *   be changed.)
     * @param ret The Collection to add the DtreeNodes to and return (can be null
     *    in which case a new Collection will be created).
     */
    static public
        Collection colOfColOfFVarsToColDtreeNodes( Map varToFamily, Collection ret) {

        Assert.notNull( varToFamily, "Dtree: varToFamily cannot be null");
        if( ret == null) { ret = new HashSet();}

        for( Iterator itr1 = varToFamily.keySet().iterator(); itr1.hasNext(); ) {
            FiniteVariable v = (FiniteVariable)itr1.next();
            ret.add( new DtreeNodeLeaf( (Collection)varToFamily.get(v), v));
        }
        return ret;
    }



    /**This comparator will first sort based on height, then on hashCode, and then try
     * a few different things before returning 0.
     */
    public static class DtreeComparatorHeightLowToHigh implements Comparator {

        public DtreeComparatorHeightLowToHigh(){}

        public int compare( Object o1, Object o2) {

            if( o1.equals( o2)) { return 0;}
            else if( o1 == o2) { System.err.println("else if o1 == o2"); return 0;}

            DtreeNode n1 = (DtreeNode)o1;
            DtreeNode n2 = (DtreeNode)o2;

            int h1 = n1.getHeight();
            int h2 = n2.getHeight();

            if( h1 < h2) { return -1;}
            else if( h1 > h2) { return 1;}
            else {
                h1 = n1.hashCode();
                h2 = n2.hashCode();

                if( h1 < h2) { return -1;}
                else if( h1 > h2) { return 1;}
                else {
                    h1 = n1.hashCodeSpecial();
                    h2 = n2.hashCodeSpecial();

                    if( h1 < h2) { return -1;}
                    else if( h1 > h2) { return 1;}
                    else {
                        System.err.println("DtreeComparatorHeightLowToHigh: Could not distinguish two" +
                                           " non-equal DtreeNodes");
                        return 0;
                    }
                }
            }
        }
    }


    public static class DtreeCreationException extends RuntimeException {
        public DtreeCreationException( String msg) {
            super( msg);
        }
    }

}//end class Dtree

