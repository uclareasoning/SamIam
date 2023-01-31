package edu.ucla.belief.dtree;

import java.util.*;


/** This class can create a Dtree, using the hMetis algorithm/library.
 *
 * This uses Weighted Nodes
 *
 * @author David Allen
 */

public class DtreeCreateHMetis1wn extends DtreeCreateHMetis {

    public String abrev() { return "HM1wn_" + numGlobalTrials + "_" + numLocalTrials;}
    public String name() { return "Create-hMetis1wn_" + numGlobalTrials + "_" + numLocalTrials;}

    public DtreeCreateHMetis1wn( int[] options, int ubfactor, int numGlobalTrials, int numLocalTrials) {
        super( options, ubfactor, numGlobalTrials, numLocalTrials);
    }

    public DtreeCreateHMetis1wn( int ubfactor, int numGlobalTrials, int numLocalTrials) {
        super( ubfactor, numGlobalTrials, numLocalTrials);
    }


    /** Create a Dtree from the data by recursively calling hMetis.
     *
     * @param data A Collection of DtreeNodes.
     * @return The root of the created dtree.
     */
    public DtreeNode create( Collection data) {

        //"order" leafNodes (i.e. data)
        ArrayList leafNodes = new ArrayList( data);

        //create a hypergraph
        Hypergraph hg = new Hypergraph( leafNodes);

        //call createSubTree to recursively call hMetis (creating a Dtree)
        Collection acutset = new HashSet();
        DtreeNode ret = createSubTree( hg, leafNodes, acutset);
        Dtree retT = new Dtree( ret);
        retT.populate();

        for( int i=1; i<numGlobalTrials; i++) {
            DtreeNode tmp = createSubTree( hg, leafNodes, acutset);
            Dtree tmpT = new Dtree( tmp);
            tmpT.populate();
            retT = Dtree.selectBestDtree( retT, tmpT);
            if( retT == tmpT) { ret = tmp;}
        }
        retT.unpopulate();
        return ret;
    }


    private DtreeNode createSubTree( Hypergraph hg, ArrayList leafNodes, Collection acutset) {
        if( hg.numNodes() == 1) {
            return (DtreeNode)leafNodes.get(0);
        }
        else if( hg.numNodes() == 2) {
            DtreeNode l = (DtreeNode)leafNodes.get(0);
            DtreeNode r = (DtreeNode)leafNodes.get(1);
            return new DtreeNodeInternal( l, r);
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
                DtreeNode leaf = (DtreeNode)leafNodes.get(i);
                Collection vars = new HashSet( leaf.getVars());
                vars.retainAll( acutset);
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


            Collection newAcutset = new HashSet( acutset);
            Collection vl = Dtree.getVars( leafLf);
            Collection vr = Dtree.getVars( leafRt);
            vl.retainAll( vr);
            newAcutset.addAll( vl);

            //Call Self
            DtreeNode left = createSubTree( lf, leafLf, newAcutset);
            DtreeNode right = createSubTree( rt, leafRt, newAcutset);
            return new DtreeNodeInternal( left, right);
        }
    }


}//end class DtreeCreateHMetis1wn
