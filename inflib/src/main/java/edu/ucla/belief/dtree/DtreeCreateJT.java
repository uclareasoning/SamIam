package edu.ucla.belief.dtree;

import java.util.*;

import edu.ucla.util.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.ParseException;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.belief.recursiveconditioning.RC;

/** This class can create a Dtree, using the supplied JoinTree.
 *
 * @author David Allen
 */

public class DtreeCreateJT extends Dtree.Create {

    static final private boolean DEBUG_CreateJT = false;
    static final private boolean DEBUG_createDtree1 = false;
    static final private boolean DEBUG_createDtree2 = false;

	private HuginLogReader hlr;

    public String abrev() { return "JT";}
    public String name() { return "Create-JT";}


    /** Create a DtreeCreateJT which will use the HuginLogReader to
     *    generate dtrees.
     *
     * @param hlr Will call parse() and then use the results.
     */
    public DtreeCreateJT( HuginLogReader hlr) {
        Assert.notNull( hlr, "DtreeCreateJT: jt cannot be null");
        this.hlr = hlr;
        try {
	        hlr.parse();
		}
		catch (ParseException e) {
			this.hlr = null;
            throw new Dtree.DtreeCreationException("Could not parse the file. " + e.toString());
		}
    }


    /** Create a Dtree from the data.
     *
     * @param data A Collection of DtreeNodeLeaf objects.
     * @return The root of the created dtree.
     */
    public DtreeNode create( Collection data) {


		Map strToLeafNode = new HashMap();//from data
		{
			for( Iterator itr = data.iterator(); itr.hasNext();) {
				DtreeNodeLeaf dtn = (DtreeNodeLeaf)itr.next();
				FiniteVariable fv = dtn.child();
				strToLeafNode.put( fv.getID(), dtn);
			}
		}

		boolean[][] joinForest = hlr.getJoinForest();
		List roots = hlr.getRoots();
		List[] assignments = hlr.getAssignments();

		DtreeNode root = createDtree( joinForest, roots, assignments, strToLeafNode);
		return root;
    }


    static private DtreeNode createDtree( boolean[][] joinForest, List roots,
                                          List[] assignments,
                                          Map strToLeafNode) {
        DtreeNode ret = null;
        int intRoot;

        if( DEBUG_createDtree1 ) { Definitions.STREAM_VERBOSE.println("createDtree( " + roots.size() + " roots)");}

        Collection allLeafNodes = new HashSet( strToLeafNode.values());
        Collection leafNodesBelow = new HashSet();

        Iterator it = roots.iterator();
        if( it.hasNext()) { // >= 1 root
            intRoot = ((Integer)it.next()).intValue();
            ret = createDtreeRec( intRoot, joinForest, assignments, strToLeafNode,
                                  leafNodesBelow, allLeafNodes);
        }

        while( it.hasNext()) { // >= 2 roots (joinforest instead of jointree
//            System.err.println("join forest not join tree."); //TODO
            intRoot = ((Integer)it.next()).intValue();

            leafNodesBelow.clear();

            DtreeNode right = createDtreeRec( intRoot, joinForest, assignments, strToLeafNode,
                                              leafNodesBelow, allLeafNodes);
            if( right != null) {
                ret = new DtreeNodeInternal( ret, right);
            }
        }

        if( !strToLeafNode.isEmpty()) {
            ret = null;
            throw new Dtree.DtreeCreationException("Some families not used: " + strToLeafNode);
        }

        return ret;
    }

    /**Could possibly return null if nothing assigned to it (no families or neighbors with families).*/
    static private DtreeNode createDtreeRec( int intRoot,
                                             boolean[][] joinForest, List[] assignments,
                                             Map strToLeafNode,
                                             Collection leafNodesBelow, Collection allLeafNodes) {

        if( DEBUG_createDtree1 ) { Definitions.STREAM_VERBOSE.println("createDtreeRec(#"+intRoot+")");}

        Collection children = new HashSet(); //for all "children" of this dtree node

        //find families for node (add to children)
        List families = assignments[intRoot]; //families needing joined/created
        for( Iterator itr = families.iterator(); itr.hasNext();) {
            String id = (String)itr.next();

            DtreeNode leaf = (DtreeNode)strToLeafNode.remove( id); //remove it to ensure all appear only once
            if( leaf == null) { throw new Dtree.DtreeCreationException("Family of " + id + " not found");}

            children.add( leaf);
            leafNodesBelow.add( leaf);
        }

        //find neighbors for node (add to children)
        {
            boolean[] row = joinForest[intRoot];
            Collection below = new HashSet();
            for( int index1 = 0; index1 < row.length; index1++) {
                if( row[index1]) {
                    if( DEBUG_createDtree1 ) { Definitions.STREAM_VERBOSE.println("\t#" + intRoot + " -> #" + index1);}
                    DtreeNode chi = createDtreeRec( index1, joinForest, assignments,
                                                    strToLeafNode,
                                                    below, allLeafNodes);
                    if( chi != null) {
                        children.add( chi);
                    }
                    leafNodesBelow.addAll( below);
                    below.clear();
                }
            }
        }

        //connect up all nodes in children & return result
        if( DEBUG_createDtree1 ) { Definitions.STREAM_VERBOSE.println("Connect up " + children.size() + " nodes");}
        else if( DEBUG_createDtree2 && children.size() >= 7) {
            Definitions.STREAM_VERBOSE.println("Connect up " + children.size() + " nodes");
        }

        Collection allPossibleRoots;

        if( children.size() == 1) {
			allPossibleRoots = Collections.singleton( children.iterator().next());
		}
		else if( children.size() < 7) {
	        allPossibleRoots = connectNodes_All( (DtreeNode[])children.toArray( new DtreeNode[children.size()]),
                                                        children.size(),
                                                        new HashSet());
		}
		else {//could eventually raise this cutoff with a better algorithm
			System.err.println("This node is too large to connect using the minContext heuristic (using another). " + children.size());
			allPossibleRoots = Collections.singleton( new Dtree.Create().create(children));
		}



        if( DEBUG_createDtree1 ) { Definitions.STREAM_VERBOSE.println("Number possible roots:  " + allPossibleRoots.size());}
        else if( DEBUG_createDtree2 && children.size() >= 7) {
            Definitions.STREAM_VERBOSE.println("Number possible roots:  " + allPossibleRoots.size());
        }



        //calc context of this "local" root
        Collection cntxCol;
        {
            Collection restOfTreeLeafs = new HashSet( allLeafNodes);
            restOfTreeLeafs.removeAll( leafNodesBelow);

            Collection varsRest = Dtree.getVars( restOfTreeLeafs);
            Collection varsBelow = Dtree.getVars( leafNodesBelow);

            if( DEBUG_createDtree1) {
                Definitions.STREAM_VERBOSE.println("varsRest:" + varsRest);
                Definitions.STREAM_VERBOSE.println("varsBelow:" + varsBelow);
            }

            cntxCol = varsRest;
            cntxCol.retainAll( varsBelow);

            if( DEBUG_createDtree1) {
                Definitions.STREAM_VERBOSE.println("cntxCol:" + cntxCol);
            }
        }

        //find best possible one & return it
        long min = Long.MAX_VALUE;
        DtreeNode ret = null;
        for( Iterator itr = allPossibleRoots.iterator(); itr.hasNext();) {
            DtreeNode nd = (DtreeNode)itr.next();
            long cntx = getContextSize( nd, cntxCol, false, children);
            if( cntx < min) {
                if( DEBUG_createDtree1) {Definitions.STREAM_VERBOSE.println("new best " + min + " -> " + cntx);}
                min = cntx; ret = nd;
            }
            else {
                if( DEBUG_createDtree1) {Definitions.STREAM_VERBOSE.println("worse case " + min + " -> " + cntx);}
            }
        }
        if( DEBUG_createDtree1) {Definitions.STREAM_VERBOSE.println("final cntx "+ ret +" \t" + min);}

        return ret;
    }


/*TODO: could eventually use code above & code below to connect up MF dtrees optimally!*/



    /**Does some extra work, by generating isomorphic graphs multiple times.  TODO.
     *
     * <p>Does not handle case of nodes only having one node
     *
     * @param num Is the number of valid nodes in nodes[].
     */
    static Collection connectNodes_All( DtreeNode nodes[], int num, Collection ret) {

        if( num <= 1) { return ret;}

        DtreeNode nodes_loc[] = new DtreeNode[nodes.length-1];

        for( int i=0; i<nodes.length; i++) {
            if( nodes[i] == null) {continue;}

            for( int j=i+1; j<nodes.length; j++) {
                if( nodes[j] == null) {continue;}

                System.arraycopy( nodes, 0, nodes_loc, 0, j); //copy nodes left of j
                System.arraycopy( nodes, j+1, nodes_loc, j, nodes_loc.length-j); //copy nodes right of j

                nodes_loc[i] = new DtreeNodeInternal( nodes[i], nodes[j]);

                if( num == 2) {
                    ret.add( nodes_loc[i]);
                } //Add root

                ret = connectNodes_All( nodes_loc, num-1, ret);
            }
        }
        return ret;
    }


    /**
     * For node par & its context, determine the size of the useful contexts/caches
     *  for nodes as low as localLeafs.  (For initial calls in createDtreeRec, let useful be
     *  false, although all are same so doesn't matter).
     *
     * @param localLeafs Are the leafs of the box (children in createDtreeRec).
     */
    static long getContextSize( DtreeNode par,
                                Collection context, boolean useful,
                                Collection localLeafs) {

        if( DEBUG_createDtree1 ) { Definitions.STREAM_VERBOSE.print("\ngetContextSize: " +par+ "(useful " + useful + ") " );}
        if( DEBUG_createDtree1 ) { Definitions.STREAM_VERBOSE.print("context: " + context);}

        if( par.isLeaf()) {
            if( DEBUG_createDtree1 ) { Definitions.STREAM_VERBOSE.println("0 (leaf)");}
            return 0;
        }

        DtreeNodeInternal parInt = (DtreeNodeInternal)par;

		Collection varsL = parInt.left().getVars();
		Collection varsR = parInt.right().getVars();


        Collection locCutset = new HashSet( varsL);  //this is local cutset, possibly has variables in acutset
        {
            locCutset.retainAll( varsR);
        }


        //contextLeft = (cntx_parent intersect vars_left) + localCutsetVars
        Collection contextLeft;
        boolean usefulL;
        {
            contextLeft = new HashSet( context);
            contextLeft.retainAll( varsL);

            if( contextLeft.size() == context.size()) { usefulL = false;}  //if retainAll didn't remove any, then
            else { usefulL = true;}                                        // all in cntx_par in cntx_left

            contextLeft.addAll( locCutset);
            if( parInt.left().isLeaf()) { usefulL = false;}
        }

        //contextRight = (cntx_parent intersect vars_right) + localCutsetVars
        Collection contextRight;
        boolean usefulR;
        {
            contextRight = new HashSet( context);
            contextRight.retainAll( varsR);

            if( contextRight.size() == context.size()) { usefulR = false;}  //if retainAll didn't remove any, then
            else { usefulR = true;}                                         // all in cntx_par in cntx_right

            contextRight.addAll( locCutset);
            if( parInt.right().isLeaf()) { usefulR = false;}
        }



        StringBuffer debug;
        if( DEBUG_createDtree1) { debug = new StringBuffer("getContextSize: " +par+":");}

        long ret = 0;


        if( useful) {
            long tmp = RC.collOfFinVarsToStateSpace( context);
            ret += tmp;

            if( DEBUG_createDtree1) { debug.append(" local " + tmp);}
        }
        else if( DEBUG_createDtree1){
            long tmp = RC.collOfFinVarsToStateSpace( context);
            debug.append(" local (unused) " + tmp);
        }



        //if left/right are not the last ones recurse,
        //if they are last ones, then if not useless add to total
        if( !localLeafs.contains( parInt.left())) {
            long tmp = getContextSize( parInt.left(), contextLeft, usefulL,
                                       localLeafs);

            if( DEBUG_createDtree1) { debug.append(", Lf " + tmp);}
            ret += tmp;
        }
        else {
            if( usefulL) {
                long tmp = RC.collOfFinVarsToStateSpace( contextLeft);
                if( DEBUG_createDtree1) { debug.append(", Lf (leaf: "+parInt.left()+") " + tmp);}
                ret += tmp;
            }
            if( DEBUG_createDtree1) { debug.append(", Lf (unused leaf: "+parInt.left()+") 0");}
        }

        if( !localLeafs.contains( parInt.right())) {
            long tmp = getContextSize( parInt.right(), contextRight, usefulR,
                                   localLeafs);
            if( DEBUG_createDtree1) { debug.append(", Rt " + tmp);}
            ret += tmp;
        }
        else {
            if( usefulR) {
                long tmp = RC.collOfFinVarsToStateSpace( contextRight);
                if( DEBUG_createDtree1) { debug.append(", Rt (leaf: "+parInt.right()+") " + tmp);}
                ret += tmp;
            }
            if( DEBUG_createDtree1) { debug.append(", Rt (unused leaf: "+parInt.right()+") 0");}
        }

        if( DEBUG_createDtree1 ) { Definitions.STREAM_VERBOSE.println("ret: " + ret + ":" + debug);}
        if( DEBUG_createDtree1 ) { Definitions.STREAM_VERBOSE.println("end getContextSize: " +par+ "(useful " + useful + ") \t" + ret);}
        return ret;
    }



}//end class DtreeCreateJT

