package il2.inf.structure;

import edu.ucla.structure.Graph;
import edu.ucla.structure.HashGraph;
import edu.ucla.belief.dtree.Hypergraph;
import edu.ucla.belief.dtree.Hmetis;
import il2.util.IntSet;
import il2.model.Index;

import java.util.*;

/** This class can create a Dtree, using the hMetis algorithm/library.
 *
 * This uses Weighted Nodes
 *
 * @author David Allen
 */

public class DgraphCreateHMetis1wn extends DgraphCreateHMetis {

    public String abrev() { return "HM1wn_" + numGlobalTrials + "_" + numLocalTrials;}
    public String name() { return "Create-hMetis1wn_" + numGlobalTrials + "_" + numLocalTrials;}

    public DgraphCreateHMetis1wn( int[] options, int ubfactor, int numGlobalTrials, int numLocalTrials) {
        super( options, ubfactor, numGlobalTrials, numLocalTrials);
    }

    public DgraphCreateHMetis1wn( int ubfactor, int numGlobalTrials, int numLocalTrials) {
        super( ubfactor, numGlobalTrials, numLocalTrials);
    }


    /** Create a DGraph from the data by recursively calling hMetis.
     *
     * @param data A Collection of Indexes.
     */
    public DGraph create( Collection data) {

        //"order" leafNodes (i.e. data)
        ArrayList leafNodes = new ArrayList( data);
        Map leafNodesToInts = new HashMap( leafNodes.size());

        //create a hypergraph
        Hypergraph hg = Hypergraph.createFromIL2Indexes( leafNodes);

        //call createSubTree to recursively call hMetis (creating a Dtree inside dg)
        DGraph dg;
        {
			Graph tree = new HashGraph();
			addLeafNodes( tree, leafNodes, leafNodesToInts);
			IntSet acutset = new IntSet();
			createSubTree( hg, leafNodes, leafNodesToInts, acutset, tree);
			Map clusters = createClusters( tree, leafNodes);
			removeRoot( tree, clusters);
			dg = new DGraph( tree, DGraphs.reduce(clusters, tree));
		}

        for( int i=1; i<numGlobalTrials; i++) {
			Graph tmp_tr = new HashGraph();
			addLeafNodes( tmp_tr, leafNodes, leafNodesToInts);
			IntSet acutset = new IntSet();
			createSubTree( hg, leafNodes, leafNodesToInts, acutset, tmp_tr);
			Map tmp_clusters = createClusters( tmp_tr, leafNodes);
			removeRoot( tmp_tr, tmp_clusters);
			DGraph tmp_dg = new DGraph( tmp_tr, DGraphs.reduce(tmp_clusters, tmp_tr));

			dg = selectBestTree( dg, tmp_dg);
        }

		return dg;
    }


    private Integer createSubTree( Hypergraph hg, ArrayList leafNodes, Map leafNodesToInts, IntSet acutset, Graph tree) {
        if( hg.numNodes() == 1) {
			return (Integer)leafNodesToInts.get( leafNodes.get(0));
        }
        else if( hg.numNodes() == 2) {
			Integer l = (Integer)leafNodesToInts.get( leafNodes.get(0));
			Integer r = (Integer)leafNodesToInts.get( leafNodes.get(1));

			Integer n = new Integer( tree.size());
			tree.addEdge(l,n);
			tree.addEdge(r,n);

			return n;
        }
        else {
            int localUBFactor = ubfactor;
            //if ubfactor is too large for small graphs, fix it (rely on Mark's algo)
            while(((50 - localUBFactor) * leafNodes.size()) < 110) {
                localUBFactor--;
            }
            if( localUBFactor < 1) { localUBFactor=1;}



            //set node weights
            int[] vwgts = new int[leafNodes.size()];
            for( int i=0; i<leafNodes.size(); i++) {
				Index leaf = (Index)leafNodes.get(i);
				IntSet vars = leaf.vars();
				vars = vars.intersection( acutset);
				vwgts[i] = vars.size() + 1;//add one so there is some notion of balance even without acutset vars
            }



            int bestscore = Integer.MAX_VALUE;
            int bestpart[] = null;

            for( int num=0; num<numLocalTrials; num++) {
                //call hMetis
                int cut[] = {0};
                int part[] = Hmetis.HMETIS_PartRecursive( hg, options, localUBFactor, cut, vwgts, timing);
                if( part.length != leafNodes.size()) {
                    throw new IllegalArgumentException( "Partition size doesn't match leafNodes.");
                }
                //determine if better score
                if( cut[0] < bestscore) {
                    bestscore = cut[0];
                    bestpart = part;
                }
            }

            ArrayList leafLf = new ArrayList( leafNodes.size());
            ArrayList leafRt = new ArrayList( leafNodes.size());
            int oldToNew[] = new int[leafNodes.size()];

            for( int i=0; i<bestpart.length; i++) {
                if( bestpart[i]==0) {
                    leafLf.add( leafNodes.get(i));
                    oldToNew[i] = leafLf.size()-1;
                }
                else if( bestpart[i]==1) {
                    leafRt.add( leafNodes.get(i));
                    oldToNew[i] = leafRt.size()-1;
                }
                else { throw new IllegalStateException("Illegal partition value.");}
            }


            if( leafLf.size() == 0 || leafRt.size() == 0) {
                throw new IllegalStateException("hMetis did not partition graph.");
            }


            //Create two Hypergraphs for L & R
            Hypergraph lf = new Hypergraph( leafLf.size());
            Hypergraph rt = new Hypergraph( leafRt.size());

            //determine if each hyperedge is completely on one side or the other (if so, save)
            for( Iterator itr1 = hg.edgeSet().iterator(); itr1.hasNext();) {
                Object edge = itr1.next();
                Collection vertices = hg.nodeSet( edge);
                int l = 0;
                for( Iterator itr2 = vertices.iterator(); itr2.hasNext();) {
                    Integer v = (Integer)itr2.next();
                    if( bestpart[ v.intValue()] == 0) { l++;}
                }
                if( l == 0) { //all vertices were on right
                    Collection newEdge = new HashSet();
                    for( Iterator itr2 = vertices.iterator(); itr2.hasNext();) {
                        Integer v = (Integer)itr2.next();
                        newEdge.add( new Integer( oldToNew[v.intValue()]));
                    }
                    rt.putHyperedge( edge, newEdge);
                }
                else if( l == vertices.size()) { //all vertices were on left
                    Collection newEdge = new HashSet();
                    for( Iterator itr2 = vertices.iterator(); itr2.hasNext();) {
                        Integer v = (Integer)itr2.next();
                        newEdge.add( new Integer( oldToNew[v.intValue()]));
                    }
                    lf.putHyperedge( edge, newEdge);
                }
            }


			IntSet v1 = vars( leafLf);
			IntSet v2 = vars( leafRt);
			v1 = v1.intersection( v2);
			IntSet newAcutset = acutset.union(v1);


            //Call Self
            Integer left = createSubTree( lf, leafLf, leafNodesToInts, newAcutset, tree);
            Integer right = createSubTree( rt, leafRt, leafNodesToInts, newAcutset, tree);

            Integer n = new Integer(tree.size());
            tree.addEdge( left, n);
            tree.addEdge( right, n);
            return n;
        }
    }


}//end class DtreeCreateHMetis1wn
