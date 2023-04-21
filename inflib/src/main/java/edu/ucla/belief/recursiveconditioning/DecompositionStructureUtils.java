package edu.ucla.belief.recursiveconditioning;

import java.util.*;
import java.math.*;

import java.text.*;
import java.io.*;

import edu.ucla.util.*;
import edu.ucla.belief.dtree.*;
import edu.ucla.structure.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.belief.recursiveconditioning.*;
import edu.ucla.belief.tree.*;
import edu.ucla.belief.inference.*;


/** This is a utility class for creating RC objects (Decomposition Structures).
 *
 *  <p>User calls one of the createStructure methods to create them.
 *
 * @author David Allen
 */
final public class DecompositionStructureUtils {

    static final private boolean DEBUG_DS = false;

	static final private int WAS_FOUND_IN_CACHE = -245;


	private DecompositionStructureUtils() {} //don't allow anyone to create any



	//////////////////////////////
	//Abstract classes used to define createStructure parameters
	//////////////////////////////
	static public abstract class Params {
		BeliefNetwork bn;

		Params( BeliefNetwork bn) {
			this.bn = bn;
			Assert.notNull( bn, "BeliefNetwork cannot be null");
		}

	}
	static public abstract class ParamsTree extends Params{
		RCDtree rcDt;
		boolean includeMPE;

		/**@param rcDt Usually set in RC object constructor.*/
		ParamsTree( BeliefNetwork bn, RCDtree rcDt, boolean includeMPE) {
			super( bn);
			this.rcDt = rcDt;
			this.includeMPE = includeMPE;
		}
	}
	static public abstract class ParamsGraph extends Params{
		RCDgraph rcDg;

		/**@param rcDg Usually set in RC object constructor.*/
		ParamsGraph( BeliefNetwork bn, RCDgraph rcDg) {
			super( bn);
			this.rcDg = rcDg;
		}
	}


	/**Parameter to the createStructure series of functions.*/
	static public class ParamsTreeDT extends ParamsTree {
		Dtree dt;

		/**@param rcDt Usually set in RC object constructor.*/
		public ParamsTreeDT( BeliefNetwork bn, RCDtree rcDt, Dtree dt, boolean includeMPE) {
			super( bn, rcDt, includeMPE);
			this.dt = dt;
			Assert.notNull( dt, "Dtree cannot be null");
		}
	}
	/**Parameter to the createStructure series of functions.*/
	static public class ParamsTreeJT extends ParamsTree {
		HuginLogReader jt;
		CreateNodeMethod cnm;

		/**@param rcDt Usually set in RC object constructor.*/
		public ParamsTreeJT( BeliefNetwork bn, RCDtree rcDt, HuginLogReader jt, boolean includeMPE, CreateNodeMethod cnm) {
			super( bn, rcDt, includeMPE);
			this.jt = jt;
			this.cnm = cnm;
			Assert.notNull( jt, "HuginLogReader cannot be null");
		}
	}
	/**Parameter to the createStructure series of functions.*/
	static public class ParamsTreeJT2 extends ParamsTree {
		BeliefCompilation bc;
		CreateNodeMethod cnm;

		/**@param rcDt Usually set in RC object constructor.*/
		public ParamsTreeJT2( BeliefNetwork bn, RCDtree rcDt, BeliefCompilation bc, boolean includeMPE, CreateNodeMethod cnm) {
			super( bn, rcDt, includeMPE);
			this.bc = bc;
			this.cnm = cnm;
			Assert.notNull( bc, "Jointree cannot be null");
		}
	}
	/**Parameter to the createStructure series of functions.*/
	static public class ParamsGraphDT extends ParamsGraph {
		Dtree dt;
		public boolean desireAbilityToComputeFamilyMarginals;
		Collection reduceToVars;

		/**@param rcDg Usually set in RC object constructor.*/
		public ParamsGraphDT( BeliefNetwork bn, RCDgraph rcDg, Dtree dt) {
			this( bn, rcDg, dt, bn);
		}
		/**@param rcDg Usually set in RC object constructor.*/
		public ParamsGraphDT( BeliefNetwork bn, RCDgraph rcDg, Dtree dt, Collection reduceToVars) {
			super( bn, rcDg);
			this.dt = dt;
			Assert.notNull( dt, "Dtree cannot be null");
			desireAbilityToComputeFamilyMarginals = false;
			this.reduceToVars = reduceToVars;
		}
	}
	/**Parameter to the createStructure series of functions.*/
	static public class ParamsGraphJT extends ParamsGraph {
		HuginLogReader jt;
		CreateNodeMethod cnm;

		/**@param rcDg Usually set in RC object constructor.*/
		public ParamsGraphJT( BeliefNetwork bn, RCDgraph rcDg, HuginLogReader jt, CreateNodeMethod cnm) {
			super( bn, rcDg);
			this.jt = jt;
			this.cnm = cnm;
			Assert.notNull( jt, "HuginLogReader cannot be null");
		}
	}
	/**Parameter to the createStructure series of functions.*/
	static public class ParamsGraphJT2 extends ParamsGraph {
		BeliefCompilation bc;
		CreateNodeMethod cnm;

		/**@param rcDg Usually set in RC object constructor.*/
		public ParamsGraphJT2( BeliefNetwork bn, RCDgraph rcDg, BeliefCompilation bc, CreateNodeMethod cnm) {
			super( bn, rcDg);
			this.bc = bc;
			this.cnm = cnm;
			Assert.notNull( bc, "Jointree cannot be null");
		}
	}



	//Trees

	/**A generic function for any type of tree creation, will decide how to create it
	 *  based on the class of the parameter in.
	 */
	final static public void createStructure( ParamsTree in) {
		if( in instanceof ParamsTreeDT) {
			createStructure( (ParamsTreeDT)in);
		}
		else if( in instanceof ParamsTreeJT) {
			createStructure( (ParamsTreeJT)in);
		}
		else if( in instanceof ParamsTreeJT2) {
			createStructure( (ParamsTreeJT2)in);
		}
		else {
			throw new IllegalArgumentException("Unknown or Unsupported Creation Method");
		}
	}

	/**Creates the structure of an RC object, based on the parameters.*/
	static public void createStructure( ParamsTreeDT in) {
		RCNode root = createTreeDT( in.rcDt, in.dt, in.bn, in.includeMPE);
		in.rcDt.setRoot( root, false/*init already done*/);
	}
	/**Creates the structure of an RC object, based on the parameters.*/
	static public void createStructure( ParamsTreeJT in) {
		RCNode root = createTreeJT( in.rcDt, in.bn, in.jt, in.includeMPE, in.cnm);
		in.rcDt.setRoot( root, true/*call init*/);
	}
	/**Creates the structure of an RC object, based on the parameters.*/
	static public void createStructure( ParamsTreeJT2 in) {
		RCNode root = createTreeJT( in.rcDt, in.bn, in.bc, in.includeMPE, in.cnm);
		in.rcDt.setRoot( root, true/*call init*/);
	}


	//Graphs

	/**A generic function for any type of graph creation, will decide how to create it
	 *  based on the class of the parameter in.
	 */
	final static public void createStructure( ParamsGraph in) {
		if( in instanceof ParamsGraphDT) {
			createStructure( (ParamsGraphDT)in);
		}
		else if( in instanceof ParamsGraphJT) {
			createStructure( (ParamsGraphJT)in);
		}
		else if( in instanceof ParamsGraphJT2) {
			createStructure( (ParamsGraphJT2)in);
		}
		else {
			throw new IllegalArgumentException("Unknown or Unsupported Creation Method");
		}
	}


	/**Creates the structure of an RC object, based on the parameters.*/
	static public void createStructure( ParamsGraphDT in) {
		HashMap varToRoot = new HashMap();
		RCNode[] roots = createGraphDT( in.rcDg, in.dt, in.bn, varToRoot);
		in.rcDg.setRoots( Arrays.asList(roots), false/*init already done*/, varToRoot, true/*canComputeFamilyMarginals*/);
		if( !in.desireAbilityToComputeFamilyMarginals) { //if user doesn't want families (default), then reduce it
			//System.out.println( "rcdgraph before reduceToOnlyMarginals(): " + in.rcDg.numCacheEntries_All() );
			in.rcDg.reduceToOnlyMarginals( in.reduceToVars);
			//System.out.println( "rcdgraph after reduceToOnlyMarginals(): " + in.rcDg.numCacheEntries_All() );
		}
	}
	/**Creates the structure of an RC object, based on the parameters.*/
	static public void createStructure( ParamsGraphJT in) {
		HashMap varToRoot = new HashMap();
		RCNode[] roots = createGraphJT_Assignment( in.rcDg, in.bn, in.jt, in.cnm, varToRoot);
		in.rcDg.setRoots( Arrays.asList(roots), true/*call init*/, varToRoot, false/*canComputeFamilyMarginals*/);
	}
	/**Creates the structure of an RC object, based on the parameters.*/
	static public void createStructure( ParamsGraphJT2 in) {
		HashMap varToRoot = new HashMap();
		RCNode[] roots = createGraphJT_Assignment( in.rcDg, in.bn, in.bc, in.cnm, varToRoot);
		in.rcDg.setRoots( Arrays.asList(roots), true/*call init*/, varToRoot, false/*canComputeFamilyMarginals*/);
	}




















//////////////////////////////////////////////
//   Misc Helper Functions
//////////////////////////////////////////////


	static private HashSet createTwoElementHashSet( Object o1, Object o2) {
		HashSet ret = new HashSet(2);
		ret.add( o1);
		ret.add( o2);
		return ret;
	}


    /** Create a map of RCNode->(Collection)Clusters, from a map of DtreeNode->Clusters.
     */
    final static protected void convertDnClustersToRcClustersBinary( DtreeNode dtRt, RCNode rcRt, Map rcClusters) {
        rcClusters.put( rcRt, dtRt.getCluster());
        if( !dtRt.isLeaf() && !rcRt.isLeaf()) {
            convertDnClustersToRcClustersBinary( ((DtreeNodeInternal)dtRt).left(),
                                           ((RCNodeInternalBinaryCache)rcRt).left(),
                                           rcClusters);
            convertDnClustersToRcClustersBinary( ((DtreeNodeInternal)dtRt).right(),
                                           ((RCNodeInternalBinaryCache)rcRt).right(),
                                           rcClusters);
        }
        else if( !dtRt.isLeaf() || !rcRt.isLeaf()) {
            throw new IllegalStateException("CachingScheme.convertDnClustersToRcClustersBinary 2 trees did not match");
        }
    }


	/** Create a Map of strings (var.getID) to its leaf node.*/
	static private Map createLeafNodes( RC rc, BeliefNetwork bn) {
        Map strToLeafNode = new HashMap(); //map of variable.IDs to a DtreeLeafNode
        for( Iterator itr = bn.iterator(); itr.hasNext();) {
            FiniteVariable var = (FiniteVariable)itr.next();
            RCNodeLeaf leaf = new RCNodeLeaf( rc, var);
            strToLeafNode.put( var.getID(), leaf);
        }
        return strToLeafNode;
	}




    /**
     * For node par & its context, determine the size of the useful contexts/caches
     *  for nodes as low as localLeafs.  (For initial calls in createNode, let useful be
     *  false, although all are same so doesn't matter).
     *
     * @param localLeafs Are the leafs of the box (children in createNode).
     */
    static long getContextSize( RCNode par,
                                Collection context, boolean useful,
                                Collection localLeafs) {

        if( DEBUG_DS) { Definitions.STREAM_VERBOSE.print("\ngetContextSize: " +par+ "(useful " + useful + ") " );}
        if( DEBUG_DS) { Definitions.STREAM_VERBOSE.print("context: " + context);}

        if( par.isLeaf()) {
            if( DEBUG_DS) { Definitions.STREAM_VERBOSE.println("0 (leaf)");}
            return 0;
        }

        RCNodeInternalBinaryCache parInt = (RCNodeInternalBinaryCache)par;

		Collection locCutset = RCUtilities.calculateCutsetFromChildren( parInt, Collections.EMPTY_SET);

		long ret = 0;
		if( useful) {
            long tmp = RC.collOfFinVarsToStateSpace( context);
            ret += tmp;
		}

		for( RCIterator chiitr = parInt.childIterator(); chiitr.hasNext();) {
			RCNode chi = chiitr.nextNode();

			Collection varsChi = chi.vars();
			Collection contextChi = new HashSet( context);
			contextChi.retainAll( varsChi);

			boolean usefulChi;
			if( contextChi.size() == context.size()) { usefulChi = false;} //if retainAll didn't remove any, then
			else { usefulChi = true;}									   // all in cntx_par are in cntx_chi
			contextChi.addAll( locCutset);
			if( chi.isLeaf()) { usefulChi = false;}

			//if child is not in children recurse,
			if( !localLeafs.contains( chi)) {
				long tmp = getContextSize( chi, contextChi, usefulChi, localLeafs);
				ret += tmp;
			}
			//if they are, then if not useless add to total
			else {
				if( usefulChi) {
					long tmp = RC.collOfFinVarsToStateSpace( contextChi);
					ret += tmp;
				}
			}
		}

        if( DEBUG_DS) { Definitions.STREAM_VERBOSE.println("end getContextSize: " +par+ "(useful " + useful + ") \t" + ret);}
        return ret;
    }

    /**
     * For node par & its context, determine how many RC Calls to itself & its decesdants.
     *
     * @param localLeafs Are the leafs of the box (children in createNode).
     */
    static long getRCCalls( RCNode par,
                             Collection context, long myCalls,
                             Collection localLeafs) {

        if( DEBUG_DS) { Definitions.STREAM_VERBOSE.print("\ngetRCCalls: " +par+ "(myCalls " + myCalls + ") " );}
        if( DEBUG_DS) { Definitions.STREAM_VERBOSE.print("context: " + context);}

		if( par.userDefinedInt == WAS_FOUND_IN_CACHE) {return 0;} //don't count (or recurse) if cached
		long ret = myCalls; //own calls

        if( par.isLeaf()) {
            if( DEBUG_DS) { Definitions.STREAM_VERBOSE.println( myCalls + " (leaf)");}
            return ret;
        }

        RCNodeInternalBinaryCache parInt = (RCNodeInternalBinaryCache)par;
		Collection cutset = RCUtilities.calculateCutsetFromChildren( parInt, /*acutset in context*/context);
		long tmp_childCalls = RC.collOfFinVarsToStateSpace( context) * RC.collOfFinVarsToStateSpace( cutset); //under full caching

		for( RCIterator chiitr = parInt.childIterator(); chiitr.hasNext();) {
			RCNode chi = chiitr.nextNode();
			if( chi.userDefinedInt == WAS_FOUND_IN_CACHE) { continue;} //don't count if cached

			Collection varsChi = chi.vars();
			Collection contextChi = new HashSet( context);
			contextChi.retainAll( varsChi);
			contextChi.addAll( cutset);

			//if child is not in children recurse,
			if( !localLeafs.contains( chi)) {
				long tmp = getRCCalls( chi, contextChi, tmp_childCalls, localLeafs);
				ret += tmp;
			}
			//if they are, then add to total
			else {
				ret += tmp_childCalls;
			}
		}

        if( DEBUG_DS) { Definitions.STREAM_VERBOSE.println("end getRCCalls: " +par+ "(myCalls " + myCalls + ") \t" + ret);}
        return ret;
    }



//////////////////////////////////////////////
//   Classes for creating a Dtree or Dgraph directly
//    from a JoinTree
//////////////////////////////////////////////

	/** Create graph by picking roots iteratively, based on the number of un-rooted variables it contains.*/
	static private RCNode[] createGraphJT_LargestClique( RCDgraph rcDg, BeliefNetwork bn, HuginLogReader jt, CreateNodeMethod crn, Map varToRoot) {

		if( varToRoot == null) { throw new IllegalArgumentException("varToRoot must not be null");}

		Collection rootsRet = new HashSet();
		Map strToLeafNode = createLeafNodes( rcDg, bn);

        try{
            //parse file into jointree
            jt.parse();

            boolean[][] joinForest = jt.getJoinForest();
            List[] assignments = jt.getAssignments();

            Collection[] clusters;
			{
				List lst = jt.getCliques(); //list of HuginClique values
				clusters = new HashSet[assignments.length];

				for( ListIterator litr = lst.listIterator(); litr.hasNext();) {
					HuginClique clq = (HuginClique)litr.next();
//					if( assignments[clq.id].size() > 0) { //only allow roots which have variables assigned to them (sometimes is faster, but now have createGraphJT_Assignment).
						clusters[clq.id] = new HashSet( clq.members);
//					}
				}
			}

			//TODO
			if( jt.getRoots().size() > 1) { throw new IllegalStateException("Does not currently support Join Forests");}


			//determine roots
			RCNodeCache_JT rcNodeCacheJT = new RCNodeCache_JT();
			RCNodeCache_ChildrenBinary cacheChildren = new RCNodeCache_ChildrenBinary();
			Collection varsLeft = new HashSet();
			{
				for( Iterator itr = bn.iterator(); itr.hasNext();) {
					varsLeft.add( ((Variable)itr.next()).getID());
				}
			}

			Collection varNames = new HashSet();
			while( !varsLeft.isEmpty()) { //still need more roots

				varNames.clear();
				int nextRoot = pickRoot( varsLeft, clusters, varNames); //one with largest number in varsLeft

				if( DEBUG_DS) { Definitions.STREAM_VERBOSE.println("nextRoot: " + nextRoot);}

				//create dtree for this root
				RCNode tmp = createNode( rcDg, bn, nextRoot, -1/*fromNode*/, joinForest, assignments, strToLeafNode, rcNodeCacheJT, cacheChildren, crn, true/*forceCutset*/);
				if( tmp != null) { rootsRet.add( tmp);}

				varsLeft.removeAll( clusters[nextRoot]);

				for( Iterator itr = varNames.iterator(); itr.hasNext();) {
					String name = (String)itr.next();
					Variable fv = bn.forID( name);
					varToRoot.put( fv, tmp);
				}

			}//end while still need more roots
        }
        catch( Exception e) {
            StringWriter str = new StringWriter();
            e.printStackTrace( new PrintWriter( str));

            throw new IllegalStateException("createGraphJT_LargestClique: " + e.toString() + "\n" +
                                             str.toString());
        }

//        if( !strToLeafNode.isEmpty()) { throw new IllegalStateException("Did not connect up all leaf nodes: " + strToLeafNode);}

		return (RCNode[])rootsRet.toArray( new RCNode[rootsRet.size()]);
	}



	/** Create graph with one root for any clique which had a variable assigned to it in the jointree.*/
	static private RCNode[] createGraphJT_Assignment( RCDgraph rcDg, BeliefNetwork bn, HuginLogReader jt, CreateNodeMethod crn, Map varToRoot) {

		if( varToRoot == null) { throw new IllegalArgumentException("varToRoot must not be null");}

		Collection rootsRet = new HashSet();
		Map strToLeafNode = createLeafNodes( rcDg, bn);

        try{
            //parse file into jointree
            jt.parse();

            boolean[][] joinForest = jt.getJoinForest();
            List[] assignments = jt.getAssignments();

			//TODO
			if( jt.getRoots().size() > 1) { throw new IllegalStateException("Does not currently support Join Forests");}


			//determine roots
			RCNodeCache_JT rcNodeCacheJT = new RCNodeCache_JT();
			RCNodeCache_ChildrenBinary cacheChildren = new RCNodeCache_ChildrenBinary();
			List lst = jt.getCliques(); //list of HuginClique values

			for( ListIterator litr = lst.listIterator(); litr.hasNext();) {
				HuginClique clq = (HuginClique)litr.next();
				if( assignments[clq.id].size() > 0) {

					if( DEBUG_DS) { Definitions.STREAM_VERBOSE.println("nextRoot: " + clq.id);}

					RCNode tmp = createNode( rcDg, bn, clq.id, -1/*fromNode*/, joinForest, assignments, strToLeafNode, rcNodeCacheJT, cacheChildren, crn, true/*forceCutset*/);
					if( tmp != null) { rootsRet.add( tmp);}

					for( Iterator itr=assignments[clq.id].iterator(); itr.hasNext();) {
						String varID = (String)itr.next();
						varToRoot.put( bn.forID( varID), tmp);
					}
				}
			}
        }
        catch( Exception e) {
            StringWriter str = new StringWriter();
            e.printStackTrace( new PrintWriter( str));

            throw new IllegalStateException("createGraphJT_Assignment: " + e.toString() + "\n" +
                                             str.toString());
        }

//        if( !strToLeafNode.isEmpty()) { throw new IllegalStateException("Did not connect up all leaf nodes: " + strToLeafNode);}

		return (RCNode[])rootsRet.toArray( new RCNode[rootsRet.size()]);
	}




	/** Create graph with one root for any clique which had a variable assigned to it in the jointree.*/
	static private RCNode[] createGraphJT_Assignment( RCDgraph rcDg, BeliefNetwork bn, BeliefCompilation bc, CreateNodeMethod crn, Map varToRoot) {

		if( varToRoot == null) { throw new IllegalArgumentException("varToRoot must not be null");}

		Collection rootsRet = new HashSet();
		Map strToLeafNode = createLeafNodes( rcDg, bn);

        try{
            boolean[][] joinForest = joinForestFromJoinTree( bc.getJoinTree());
            List[] assignments = assignmentsFromJoinTree( bc);

			//TODO
			if( rootsFromJoinTree( bc.getJoinTree()).size() > 1) { throw new IllegalStateException("Does not currently support Join Forests");}


			//determine roots
			RCNodeCache_JT rcNodeCacheJT = new RCNodeCache_JT();
			RCNodeCache_ChildrenBinary cacheChildren = new RCNodeCache_ChildrenBinary();

			for( int i=0; i<joinForest.length; i++) {
				if( assignments[i].size() > 0) {

					if( DEBUG_DS) { Definitions.STREAM_VERBOSE.println("nextRoot: " + i);}

					RCNode tmp = createNode( rcDg, bn, i, -1/*fromNode*/, joinForest, assignments, strToLeafNode, rcNodeCacheJT, cacheChildren, crn, true/*forceCutset*/);
					if( tmp != null) { rootsRet.add( tmp);}

					for( Iterator itr=assignments[i].iterator(); itr.hasNext();) {
						String varID = (String)itr.next();
						varToRoot.put( bn.forID( varID), tmp);
					}
				}
			}
        }
        catch( Exception e) {
            StringWriter str = new StringWriter();
            e.printStackTrace( new PrintWriter( str));

            throw new IllegalStateException("createGraphJT_Assignment: " + e.toString() + "\n" +
                                             str.toString());
        }

//        if( !strToLeafNode.isEmpty()) { throw new IllegalStateException("Did not connect up all leaf nodes: " + strToLeafNode);}

		return (RCNode[])rootsRet.toArray( new RCNode[rootsRet.size()]);
	}




	/**Create a Dtree from a JT*/
	static private RCNode createTreeJT( RCDtree rcDt, BeliefNetwork bn, HuginLogReader jt,
									    boolean includeMPE, CreateNodeMethod crn) {

		RCNode ret;
		Map strToLeafNode = createLeafNodes( rcDt, bn);


        try{
            //parse file into jointree
            jt.parse();

            //create dtree

            boolean[][] joinForest = jt.getJoinForest();
            List roots = jt.getRoots();
            List[] assignments = jt.getAssignments();

			HashSet nodes = new HashSet();
			for( ListIterator itr = roots.listIterator(); itr.hasNext();) { //handle join forest here
				int rt = ((Integer)itr.next()).intValue();
	            RCNode tmp = createNode( rcDt, bn, rt, -1, joinForest, assignments, strToLeafNode, null/*rcNodeCacheJT*/, null/*cacheChildren*/, crn, false/*forceCutset*/);
	            if( tmp != null) { nodes.add( tmp);}
			}

			ret = connectChildren( rcDt, nodes, crn, strToLeafNode.values(), null/*cacheChildren*/);
        }
        catch( Exception e) {
            StringWriter str = new StringWriter();
            e.printStackTrace( new PrintWriter( str));

            throw new IllegalStateException("createTreeJT: " + e.toString() + "\n" +
                                             str.toString());
        }

//        if( !strToLeafNode.isEmpty()) { throw new IllegalStateException("Did not connect up all leaf nodes: " + strToLeafNode);}

		return ret;
	}



	static private boolean[][] joinForestFromJoinTree( JoinTree jt) { //jt uses 0 based numbering
		int size = jt.tree().size();
		boolean joinForest[][] = new boolean[size][];
		for( int i=0; i<joinForest.length; i++) { joinForest[i] = new boolean[size]; Arrays.fill( joinForest[i], false);}

		//initialize joinForest
		IntGraph tr = jt.tree();
		for( int i=0; i<tr.size(); i++) {
			int[] nei = tr.neighbors(i);
			for( int j=0; j<nei.length; j++) {
				joinForest[i][j] = true;
				joinForest[j][i] = true;
			}
		}
		return joinForest;
	}


	static private List rootsFromJoinTree( JoinTree jt) { //jt uses 0 based numbering
		List ret = new LinkedList();
		int size = jt.tree().size();
		Integer root = null;

		for( int i=0; i<size; i++) { //try to find a non-leaf node for the root
			if( !jt.tree().isLeaf(i)) { root = new Integer(i); break;}
		}
		if( root == null) { root = new Integer(0);}
		ret.add( root);
		return ret;
	}


	static private List[] assignmentsFromJoinTree( BeliefCompilation bc) { //bc uses 0 based numbering
		int size = bc.getJoinTree().tree().size();
		List ret[] = new List[size];
		for( int i=0; i<ret.length; i++) { ret[i] = new LinkedList();}

		Map famLocations = bc.thetaLocations(); //map from FinVar->Integer

		for( Iterator itr = famLocations.keySet().iterator(); itr.hasNext();) {
			FiniteVariable fv = (FiniteVariable)itr.next();
			Integer clq = (Integer)famLocations.get(fv);
			ret[clq.intValue()].add( fv.getID());
		}
		return ret;
	}



	/**Create a Dtree from a JT*/
	static private RCNode createTreeJT( RCDtree rcDt, BeliefNetwork bn, BeliefCompilation bc,
									    boolean includeMPE, CreateNodeMethod crn) {

		RCNode ret;
		Map strToLeafNode = createLeafNodes( rcDt, bn);


        try{
            //create dtree

            boolean[][] joinForest = joinForestFromJoinTree( bc.getJoinTree());
            List roots = rootsFromJoinTree( bc.getJoinTree());;
            List[] assignments = assignmentsFromJoinTree( bc);

			HashSet nodes = new HashSet();
			for( ListIterator itr = roots.listIterator(); itr.hasNext();) { //handle join forest here
				int rt = ((Integer)itr.next()).intValue();
	            RCNode tmp = createNode( rcDt, bn, rt, -1, joinForest, assignments, strToLeafNode, null/*rcNodeCacheJT*/, null/*cacheChildren*/, crn, false/*forceCutset*/);
	            if( tmp != null) { nodes.add( tmp);}
			}

			ret = connectChildren( rcDt, nodes, crn, strToLeafNode.values(), null/*cacheChildren*/);
        }
        catch( Exception e) {
            StringWriter str = new StringWriter();
            e.printStackTrace( new PrintWriter( str));

            throw new IllegalStateException("createTreeJT: " + e.toString() + "\n" +
                                             str.toString());
        }

//        if( !strToLeafNode.isEmpty()) { throw new IllegalStateException("Did not connect up all leaf nodes: " + strToLeafNode);}

		return ret;
	}




	/**Will change the Collections in clusters[].*/
	static private int pickRoot( Collection varsLeft, Collection clusters[], Collection varNames) {
		int maxNum = -1;
		int maxIndx = -1;

		for( int i=0; i<clusters.length; i++) {
			if( clusters[i] != null) {

				//System.out.println("before cluster[" + i + "] = " + clusters[i].size() + " " + clusters[i]);
				//System.out.println("varsLeft" + varsLeft);

				clusters[i].retainAll( varsLeft);
				int size = clusters[i].size();

				//System.out.println("after cluster[" + i + "] = " + clusters[i].size() + " " + clusters[i]);

				//pick the cluster which has the largest number of variables not covered by another root
				if( size == 0) { clusters[i] = null;}
				else if( size > maxNum) { maxNum = size; maxIndx = i;}
			}
		}
		if( varNames != null) { varNames.addAll( clusters[maxIndx]);}
		return maxIndx;
	}



	//need to go up and down the jointree, & cache nodes already created
	//rcNodeCacheJT can be null
	static private RCNode createNode( RC rc, BeliefNetwork bn, int jtnode, int fromnode,
									  boolean[][] joinForest, List[] assignments,
									  Map strToLeafNode, RCNodeCache_JT rcNodeCacheJT,
									  RCNodeCache_ChildrenBinary cacheChildren,
									  CreateNodeMethod cnMethod, boolean forceCutset) {

		RCNode ret;
		HashSet children = new HashSet();
		HashSet children_fam = new HashSet();

		//create children_fam from families assigned to this node
		List families = assignments[jtnode]; //variables assigned to this jt node
		for( Iterator itr = families.iterator(); itr.hasNext();) {
			String id = (String)itr.next();
			RCNode leaf = (RCNode)strToLeafNode.get( id);
			if( leaf == null) { throw new IllegalStateException("Leaf node for " + id + " not found");}

			children_fam.add( leaf);
		}

		//create children based on jointree
		{
			boolean[] row = joinForest[jtnode];
			for( int index1 = 0; index1 < row.length; index1++) {
				if( row[index1]) {
					if( index1 == fromnode) { continue;}
                    if( DEBUG_DS) { Definitions.STREAM_VERBOSE.println("\t#" + jtnode + " -> #" + index1);}
                    RCNode tmp = null;

					if( rcNodeCacheJT != null) { tmp = (RCNode)rcNodeCacheJT.getCache( jtnode + "---" + index1);}
                    if( tmp == null) {
                    	tmp = createNode( rc, bn, index1, jtnode, joinForest, assignments, strToLeafNode, rcNodeCacheJT, cacheChildren, cnMethod, false/*forceCutset*/);
                    	if( rcNodeCacheJT != null) {rcNodeCacheJT.putCache( jtnode + "---" + index1, tmp);}
					}
                    if( tmp != null) { children.add( tmp);}
				}
			}

			//find parent, as it might be child in this orientation
			for( int index1 = 0; index1 < joinForest.length; index1++) {
				if( joinForest[index1][jtnode]) {
					if( index1 == fromnode) { continue;}
                    if( DEBUG_DS) { Definitions.STREAM_VERBOSE.println("\t#" + jtnode + " -> #" + index1);}
                    RCNode tmp = null;

                    if( rcNodeCacheJT != null) { tmp = (RCNode)rcNodeCacheJT.getCache( jtnode + "---" + index1);}
                    if( tmp == null) {
                    	tmp = createNode( rc, bn, index1, jtnode, joinForest, assignments, strToLeafNode, rcNodeCacheJT, cacheChildren, cnMethod, false/*forceCutset*/);
                    	if( rcNodeCacheJT != null) { rcNodeCacheJT.putCache( jtnode + "---" + index1, tmp);}
					}
                    if( tmp != null) { children.add( tmp);}
				}
			}
		}

		if( forceCutset) { //force variables to be in the cutset of this node
			//create this node

			RCNode lf = connectChildren( rc, children_fam, cnMethod, strToLeafNode.values(), cacheChildren);
			RCNode rt = connectChildren( rc, children, cnMethod, strToLeafNode.values(), cacheChildren);

			ret = new RCNodeInternalBinaryCache( rc, lf, rt);
		}
		else {
			//create this node
			children.addAll( children_fam);
			ret = connectChildren( rc, children, cnMethod, strToLeafNode.values(), cacheChildren);
		}

		return ret;
	}



	/** Connect up all the nodes in children using the method specified by cnMethod.
	 *
	 *  @param cacheChildren can be null.
	 */
	static private RCNode connectChildren( RC rc, HashSet children, CreateNodeMethod cnMethod, Collection allLeafs,
											RCNodeCache_ChildrenBinary cacheChildren) {
		RCNode ret;

//System.out.println("begin connectChildren");

		if( cacheChildren != null) { cacheChildren.reduceCollection( children);}

		if( children.size() > 1) {
			ret = null;

			if( cnMethod == CreateNodeMethod.binaryNode_MinRCCalls) {

				if( children.size() >= 5) {
					System.err.println("This node is too large to connect using the minRCCalls heuristic.");
//					System.out.println("This node is too large to connect using the minRCCalls heuristic.");
					ret = connectNodes_RandomBinary( rc, children, cacheChildren);
				}
				else {
					Collection allPossibleRoots = connectNodes_All( rc, (RCNode[])children.toArray( new RCNode[children.size()]),
																	children.size(), new HashSet());

					ret = pickBestRoot_minRCCalls( allPossibleRoots, allLeafs, children, cacheChildren);
				}
			}
			else if( cnMethod == CreateNodeMethod.binaryNode_MinContext) {

				if( children.size() >= 5) {
					System.err.println("This node is too large to connect using the minContext heuristic.");
//					System.out.println("This node is too large to connect using the minContext heuristic.");
					ret = connectNodes_RandomBinary( rc, children, cacheChildren);
				}
				else {
					Collection allPossibleRoots = connectNodes_All( rc, (RCNode[])children.toArray( new RCNode[children.size()]),
																	children.size(), new HashSet());

					ret = pickBestRoot_minContext( allPossibleRoots, allLeafs, children, cacheChildren);
				}
			}
			else if( cnMethod == CreateNodeMethod.binaryNode_Random) {

				if( ret == null) {
					ret = connectNodes_RandomBinary( rc, children, cacheChildren);
				}
			}
			else {
				throw new IllegalArgumentException();
			}
		}
		else if( children.size() == 1) {
			ret = (RCNode)children.iterator().next();
		}
		else {
			ret = null;
		}
//System.out.println("end connectChildren");
		return ret;
	}



	/** Does not look into the cacheChildren for matches, but will add
	 *   new nodes to the cache for others.
	 */
	static private RCNode connectNodes_RandomBinary( RC rc, Collection nodes, RCNodeCache_ChildrenBinary cacheChildren) {
		LinkedList openNodes = new LinkedList( nodes);
		while( openNodes.size() > 1) {
			RCNode lf = (RCNode)openNodes.removeFirst();
			RCNode rt = (RCNode)openNodes.removeFirst();
			RCNodeInternalBinaryCache nd = null;

//			HashSet chi = null;
//			if( cacheChildren != null) {
//				chi = createTwoElementHashSet( lf, rt);
//				nd = (RCNodeInternalBinaryCache)cacheChildren.getCache( chi);
//			}

//			if( nd == null) {
				nd = new RCNodeInternalBinaryCache( rc, lf, rt);
//			}
			openNodes.add( nd);
			if( cacheChildren != null) { cacheChildren.putCache( lf, rt, nd);}
		}
		if( openNodes.size() == 1) { return (RCNode)openNodes.removeFirst();}
		else if( openNodes.size() == 0) { return null;}
		else { throw new IllegalStateException();}
	}

    /**Does some extra work, by generating isomorphic graphs multiple times.  TODO.
     *
     * <p>Does not handle case of nodes only having one node
     *
     * @param num Is the number of valid nodes in nodes[].
     */
    static Collection connectNodes_All( RC rc, RCNode nodes[], int num, Collection ret) {

        if( num <= 1) { return ret;}

        RCNode nodes_loc[] = new RCNode[nodes.length-1];

        for( int i=0; i<nodes.length; i++) {
            if( nodes[i] == null) {continue;}

            for( int j=i+1; j<nodes.length; j++) {
                if( nodes[j] == null) {continue;}

                System.arraycopy( nodes, 0, nodes_loc, 0, j); //copy nodes left of j
                System.arraycopy( nodes, j+1, nodes_loc, j, nodes_loc.length-j); //copy nodes right of j


				RCNode nd = null;
				if( nd == null) { nd = new RCNodeInternalBinaryCache( rc, nodes[i], nodes[j], false, false);}

                nodes_loc[i] = nd;

                if( num == 2) {
                    ret.add( nodes_loc[i]);
                } //Add root

                connectNodes_All( rc, nodes_loc, num-1, ret);
            }
        }
        return ret;
    }


	static private RCNode pickBestRoot_minContext( Collection allPossibleRoots, Collection allLeafs, Collection children,
													RCNodeCache_ChildrenBinary cacheChildren) {
		RCNode ret = null;
		//compute local context
		Collection cntxCol;
		{
			RCNode anyRoot = (RCNode)allPossibleRoots.iterator().next();

			Collection leafsBelow = new HashSet();
			RCUtilities.computeLeafs( anyRoot, leafsBelow);

			Collection leafsAbove = new HashSet( allLeafs);
			leafsAbove.removeAll( leafsBelow);

			Collection varsBelow = RC.getVars(leafsBelow);
			Collection varsAbove = RC.getVars(leafsAbove);

			cntxCol = varsAbove;
			cntxCol.retainAll( varsBelow);
		}


		//pick best root & store it in ret
		long min = Long.MAX_VALUE;
		for( Iterator itr = allPossibleRoots.iterator(); itr.hasNext();) {
			RCNode rn = (RCNode)itr.next();
			long cntx = getContextSize( rn, cntxCol, false, children);
			if( cntx < min) {
				if( DEBUG_DS) {Definitions.STREAM_VERBOSE.println("new best " + min + " -> " + cntx);}
				min = cntx; ret = rn;
			}
			else {
				if( DEBUG_DS) {Definitions.STREAM_VERBOSE.println("worse case " + min + " -> " + cntx);}
			}
		}

		addCache( ret, children, cacheChildren);
		addParentNodes( ret, children);
		return ret;
	}
	static private RCNode pickBestRoot_minRCCalls( Collection allPossibleRoots, Collection allLeafs, Collection children,
													RCNodeCache_ChildrenBinary cacheChildren) {
		RCNode ret = null;
		//compute local context
		Collection cntxCol;
		{
			RCNode anyRoot = (RCNode)allPossibleRoots.iterator().next();

			Collection leafsBelow = new HashSet();
			RCUtilities.computeLeafs( anyRoot, leafsBelow);

			Collection leafsAbove = new HashSet( allLeafs);
			leafsAbove.removeAll( leafsBelow);

			Collection varsBelow = RC.getVars(leafsBelow);
			Collection varsAbove = RC.getVars(leafsAbove);

			cntxCol = varsAbove;
			cntxCol.retainAll( varsBelow);
		}


		//pick best root & store it in ret
		long min = Long.MAX_VALUE;
		for( Iterator itr = allPossibleRoots.iterator(); itr.hasNext();) {
			RCNode rn = (RCNode)itr.next();
			long calls = getRCCalls( rn, cntxCol, 1, children);
			if( calls < min) {
				if( DEBUG_DS) {Definitions.STREAM_VERBOSE.println("new best " + min + " -> " + calls);}
				min = calls; ret = rn;
			}
			else {
				if( DEBUG_DS) {Definitions.STREAM_VERBOSE.println("worse case " + min + " -> " + calls);}
			}
		}

		addCache( ret, children, cacheChildren);
		addParentNodes( ret, children);
		return ret;
	}

	/**Add nd to cacheChildren (map from nd.children()->nd) recursively,
	 *  until (and including) nodes in children.  If children is null, go until
	 *  hit roots.
	 */
	static private void addCache( RCNode nd, Collection children, RCNodeCache_ChildrenBinary cacheChildren) {
		if( nd == null || cacheChildren == null) { return;}
		if( nd.isLeaf()) { return;}

		RCNodeInternalBinaryCache ndi = (RCNodeInternalBinaryCache)nd;

		int i=0;
		RCNode lf = null;
		RCNode rt = null;

		for( RCIterator chiitr = ndi.childIterator(); chiitr.hasNext();) {
			RCNode chi = chiitr.nextNode();
			if( i==0) { lf = chi; i++;}
			else if( i==1) { rt = chi; i++;}
			else { throw new IllegalStateException("non-binary node");}
		}

		cacheChildren.putCache( lf, rt, nd);

		if( children != null && children.contains( nd)) { return;}
		for( RCIterator chiitr = ndi.childIterator(); chiitr.hasNext();) {
			RCNode chi = chiitr.nextNode();
			addCache( chi, children, cacheChildren);
		}
	}
	/**Set parentNodes for the nodes actually used.*/
	static private void addParentNodes( RCNode nd, Collection children) {
		if( nd == null) { return;}
		if( nd.isLeaf()) { return;}

		RCNodeInternalBinaryCache ndi = (RCNodeInternalBinaryCache)nd;

		if( children != null && children.contains( nd)) { return;}
		for( RCIterator chiitr = ndi.childIterator(); chiitr.hasNext();) {
			RCNode chi = chiitr.nextNode();
			chi.parentNodes.add( nd);
			addParentNodes( chi, children);
		}
	}


	static final public class CreateNodeMethod {
		private CreateNodeMethod(){}

		public static final CreateNodeMethod binaryNode_MinRCCalls = new CreateNodeMethod();
		public static final CreateNodeMethod binaryNode_MinContext = new CreateNodeMethod();
		public static final CreateNodeMethod binaryNode_Random = new CreateNodeMethod();
	}






	/**For caching nodes based on the direction of traversal in the JT, use this caching scheme.*/
	final static private class RCNodeCache_JT {
		HashMap cache = new HashMap();

		public RCNode getCache( String key) {
			return (RCNode)cache.get( key);
		}

		public void putCache( String key, RCNode nd) {
			Object ret = cache.put( key, nd);
            if( ret != null && ret != nd) { throw new IllegalStateException("Tried to add a cache which already existed.");}
		}
	}


	/**When would know children, use this caching scheme.*/
	final static private class RCNodeCache_ChildrenBinary {
		HashMap cache = new HashMap();

		/** Will try to match nodes in chi by using cached nodes.*/
		public void reduceCollection( Collection chi) {

//System.out.println("begin reduceCollection " + chi.size());

			boolean done = false;
			while( !done) {

				Object chi1 = null;
				Object chi2 = null;
				Object chi_cache = null;

				label_search:
				for( Iterator itr1 = chi.iterator(); itr1.hasNext();) {
					chi1 = itr1.next();
					if( cache.containsKey( chi1)) {
						HashMap mid = (HashMap)cache.get(chi1);

						for( Iterator itr2 = chi.iterator(); itr2.hasNext();) {
							chi2 = itr2.next();
							if( chi2 == chi1) { continue;}

							//see if chi2 is in mid (if so remove both & start again)
							if( mid.containsKey( chi2)) {
								chi_cache = mid.get( chi2);
								break label_search;
							}
						}
					}
				}

				//possibly found match, or else done
				if( chi_cache == null) { done = true;}
				else {
					((RCNode)chi_cache).userDefinedInt = WAS_FOUND_IN_CACHE;
					chi.remove( chi1); chi1 = null;
					chi.remove( chi2); chi2 = null;
					chi.add( chi_cache); chi_cache = null;
				}
			}
//System.out.println("end reduceCollection " + chi.size());
		}

		public RCNode getCache( RCNode lf, RCNode rt) {
			if( cache.containsKey( lf)) {
				HashMap mid = (HashMap)cache.get(lf);
				if( mid != null && mid.containsKey( rt)) {
					return (RCNode)mid.get( rt);
				}
			}
			if( cache.containsKey( rt)) {
				HashMap mid = (HashMap)cache.get(rt);
				if( mid != null && mid.containsKey( lf)) {
					return (RCNode)mid.get( lf);
				}
			}
			return null;
		}

		public void putCache( RCNode lf, RCNode rt, RCNode nd) {

			if( cache.containsKey( rt)) {
				HashMap mid = (HashMap)cache.get(rt);
				Object old = mid.put( lf, nd);
				if( old != null && old != nd) { throw new IllegalStateException("Tried to add a cache which already existed.");}
				return;
			}
			else {
				HashMap mid = (HashMap)cache.get(lf);
				if( mid == null) {
					mid = new HashMap();
					cache.put( lf, mid);
				}
				Object old = mid.put( rt, nd);
				if( old != null && old != nd) { throw new IllegalStateException("Tried to add a cache which already existed.");}
			}
		}
	}









//////////////////////////////////////////////
//   Classes for creating an RCDtree from a Dtree
//    and then orienting it to a Dgraph
//////////////////////////////////////////////


    static private RCNode createTreeDT( RCDtree rcDt, Dtree dt, BeliefNetwork bn,
    									boolean includeMPE) {
		dt.populate();
        RCNode root = createNodeBinaryTree( dt.root(), rcDt, bn, includeMPE);
        return root;
	}

    static private RCNode[] createGraphDT( RCDgraph rcDg, Dtree dt, BeliefNetwork bn, Map varToRoot) {
		dt.populate();
        RCNode root = createNodeBinaryTree( dt.root(), rcDg, bn, /*includeMPE*/false);

        Map rcClusters = new HashMap();
        convertDnClustersToRcClustersBinary( dt.root(), root, rcClusters);

        RCNode roots[] = convertBinTreeToBinGraph( rcDg, root, rcClusters, varToRoot);
        return roots;
    }




    static private RCNode createNodeBinaryTree( DtreeNode dn, RC rc, BeliefNetwork bn,
                                 		   		boolean includeMPE) {
        RCNode ret;
        if( dn.isLeaf()) {
            FiniteVariable fv = ((DtreeNodeLeaf)dn).child();
            ret = new RCNodeLeaf( rc, fv, dn.getContext());
        }
        else {
			Collection tmpMPE;
			if( includeMPE) {
				tmpMPE = new HashSet( dn.getVars());
				tmpMPE.removeAll( dn.getContext());
			}
			else {
				tmpMPE = null;
			}

            ret = new RCNodeInternalBinaryCache( rc, dn.getCutset(), dn.getContext(),
                          createNodeBinaryTree( ((DtreeNodeInternal)dn).left(), rc, bn, includeMPE),
                          createNodeBinaryTree( ((DtreeNodeInternal)dn).right(), rc, bn, includeMPE),
                          1.0, tmpMPE);
        }
        return ret;
    }





    /** Convert a RCDtree to a RCDgraph (initial tree should not have mpe info, and this code
     *   will produce a graph where the root[0] has mpe and the rest doesn't.)
     */
    final static private RCNode[] convertBinTreeToBinGraph( RCDgraph rcDg, RCNode rcDt, Map rcClusters, Map varToRoot) {

		if( varToRoot == null) { throw new IllegalArgumentException("varToRoot must not be null");}

        Collection leafs = new HashSet();
        Map parents = new HashMap();
        RCUtilities.computeLeafs( rcDt, leafs);
        RCUtilities.computeParentsInTree( rcDt, parents);

        RCNode[] roots = new RCNode[leafs.size()];

        if( !rcDt.isLeaf()) {
            //remove root (set its children to be parents of each other)
            RCNodeInternalBinaryCache rt = (RCNodeInternalBinaryCache)rcDt;

			parents.put( rt.left(), rt.right());
			parents.put( rt.right(), rt.left());

            OrientCache cache = new OrientCache();

            //for each leaf...
            int rt_cntr = 0;
            boolean bFirstRoot = true;
            for( Iterator itr = leafs.iterator(); itr.hasNext();) {

                RCNodeLeaf n = (RCNodeLeaf)itr.next();
                RCNode p = (RCNode)parents.get(n);

                HashSet cutset = new HashSet();
                cutset.addAll( (Collection)rcClusters.get(p));
                cutset.retainAll( (Collection)rcClusters.get(n));

                Set context = Collections.EMPTY_SET;

                //Order this so that dgraph joints come out in order
                ArrayList orderedCutset = new ArrayList();
                for( Iterator itr_v = n.getLeafVar().getCPTShell( n.getLeafVar().getDSLNodeType() ).index().variables().iterator(); itr_v.hasNext();) {
                    FiniteVariable fv = (FiniteVariable)itr_v.next();
                    if( cutset.contains( fv)) {
                        orderedCutset.add( fv);
                        cutset.remove( fv);
                    }
                }
                if( !cutset.isEmpty()) {
                    System.err.println("cutset still contains: " + cutset + " for the leaf node: " + n.getLeafVar());
                    orderedCutset.addAll( cutset);
                }



                RCNodeInternalBinaryCache m;
                if( bFirstRoot) {
                    bFirstRoot = false;
                    RCNode n1 = n;
                    RCNode n2 = orientBinary(rcDg,p,n,parents,cache,rcClusters,true);
                    Collection varsMPE = new HashSet();
                    n1.vars(varsMPE);
                    n2.vars(varsMPE);//vars-context (but roots don't have a context)
                    m = new RCNodeInternalBinaryCache( rcDg, orderedCutset, context, n1,
                                                       n2, 1.0, varsMPE);
                }
                else {
                    m = new RCNodeInternalBinaryCache( rcDg, orderedCutset, context, n,
                                                       orientBinary(rcDg,p,n,parents,cache,rcClusters,false),
                                                       1.0, null);
                }
                rcClusters.put( m, cutset /*since context=empty, cluster=cutset*/);

                roots[rt_cntr] = m;
                varToRoot.put( n.getLeafVar(), m);
                rt_cntr++;
            }
        }
        else {
            varToRoot.put( ((RCNodeLeaf)rcDt).getLeafVar(), rcDt);
            roots[0] = rcDt;
        }
        return roots;
    }



    final static private RCNode orientBinary( RC rc, RCNode to, RCNode from, Map parents, OrientCache cache, Map rcClusters, boolean includeMPE) {
        if( to.isLeaf()) { //only one neighbor (from)
            Assert.condition( from == parents.get(to), "from is not t's parent");
            return to;
        }
        else { //multiple neighbors
            RCNode m = cache.getCache( to, from);
            if( m != null) { return m;}

            RCNode n1 = (RCNode)parents.get(to);
            RCNode n2 = ((RCNodeInternalBinaryCache)to).left();

            {
                RCNode n3 = ((RCNodeInternalBinaryCache)to).right();

                if( n1 == from) {
                    n1 = n3;
                }
                else if( n2 == from) {
                    n2 = n3;
                }
                else if( n3 == from) {
                    //do nothing 1&2 are okay
                }
                else {
                    throw new IllegalStateException("CachingScheme.orient: from did not match n1, n2, or n3");
                }
            }

            HashSet context = new HashSet();
            context.addAll( (Collection)rcClusters.get(to));
            context.retainAll( (Collection)rcClusters.get(from));

            HashSet cutset = new HashSet();
            cutset.addAll( (Collection)rcClusters.get(to));
            cutset.removeAll( context);

            HashSet cluster = new HashSet();
            cluster.addAll( context);
            cluster.addAll( cutset);

            RCNode ln1 = orientBinary(rc, n1,to,parents,cache,rcClusters,includeMPE);
            RCNode ln2 = orientBinary(rc, n2,to,parents,cache,rcClusters,includeMPE);


            Collection varsMPE = null;
            if( includeMPE) {
                varsMPE = new HashSet();
                ln1.vars( varsMPE);
                ln2.vars( varsMPE);
                varsMPE.removeAll( context);
            }

            m = new RCNodeInternalBinaryCache( rc, cutset, context, ln1, ln2, 1.0, varsMPE);
            rcClusters.put( m, cluster);
            cache.putCache( to, from, m);
            return m;
        }
    }

	/**When know where from/to, then use this type of caching scheme.*/
    static protected class OrientCache {
        HashMap level1 = new HashMap();

        public RCNode getCache( RCNode t, RCNode n) {
            Object level2 = level1.get(t);
            if( level2 == null) { return null;}

            Object ret = ((HashMap)level2).get(n);
            if( ret == null) { return null;}

            return (RCNode)ret;
        }

        public void putCache( RCNode t, RCNode n, RCNode m) {
            Object level2 = level1.get(t);
            if( level2 == null) {
                level2 = new HashMap();
                level1.put( t, level2);
            }

            Object ret = ((HashMap)level2).get(n);
            if( ret != null) { throw new IllegalStateException("Tried to add a cache which already existed.");}
            ((HashMap)level2).put( n, m);
        }
    }



}//end class DecompositionStructureUtils

