package edu.ucla.belief.dtree;

import java.util.*;

import il2.model.Index;


/** This class represents Hypergraph objects.
 *
 * @author David Allen
 */

public class Hypergraph {

    //Its Map is from each hyperedge to a Collection of hypernodes
    //e.g. Map from each variable in a moral graph to a Collection of
    //     Undirected Edges it is a part of (stored as Integer values from 0..#-1).
    //Note: Do note rely on the datatype of the Map objects, as the keySet values may
    //  not necessarily be FiniteVariables (e.g. in il2 they are not).

    static final private boolean DEBUG_Hyper = false;

    private HashMap data;
    private final int numHyperNodes;
    private Map edgeWeights;  //null if nonWeighted


    /** Create a Hypergraph from a List of DtreeNodeLeafs.*/
    public Hypergraph( List leafNodes) {
        numHyperNodes = leafNodes.size();
        data = new HashMap(numHyperNodes);
        if( numHyperNodes <= 0) { throw new IllegalArgumentException("Hypergraph: empty");}

        for( int i=0; i<leafNodes.size(); i++) {
            DtreeNode dn = (DtreeNode)leafNodes.get(i);

            for( Iterator itr2 = dn.getVars().iterator(); itr2.hasNext();) {
                Object var = itr2.next();
                Collection col = nodeSet( var);
                if( col == null) {
                    col = new HashSet();
                    data.put( var, col);
                }
                col.add( new Integer(i));
            }
        }
    }


    /** Create a Hypergraph.
     *
     * @param edgeToColNodes A Map from each hyperedge (FiniteVariable) to a
     *        Collection of hypernodes (represented as
     *        Integers from 0..numNodes-1).
     * @param numHyperNodes The number of HyperNodes in the graph, it must be
     *        one or larger (once set, this cannot be changed).
     */
    public Hypergraph( Map edgeToColNodes, int numHyperNodes) {
        data = new HashMap( edgeToColNodes);
        this.numHyperNodes = numHyperNodes;
        if( numHyperNodes <= 0) { throw new IllegalArgumentException("Hypergraph: empty");}
    }

    /** Create a Hypergraph.
     *
     * @param numHyperNodes The number of HyperNodes in the graph, it must be
     *        one or larger (once set, this cannot be changed).
     */
    public Hypergraph( int numHyperNodes) {
        data = new HashMap( numHyperNodes);
        this.numHyperNodes = numHyperNodes;
        if( numHyperNodes <= 0) { throw new IllegalArgumentException("Hypergraph: empty");}
    }



	static public Hypergraph createFromIL2Indexes( List leafNodes) {
		Hypergraph ret = new Hypergraph( leafNodes.size());

		for( int i=0; i<leafNodes.size(); i++) {
			Index n = (Index)leafNodes.get(i);

			int numvars = n.vars().size();
			for( int j=0; j<numvars; j++) {
				Object var = new Integer(n.vars().get(j));
				Collection col = ret.nodeSet( var);
				if( col == null) {
					col = new HashSet();
					ret.data.put( var, col);
				}
				col.add( new Integer(i));
			}
		}

		return ret;
	}



    /** Sets the weight of the edge to wt.*/
    public void setEdgeWeight( Object edge, Integer wt) {
        if( edgeWeights == null) {
            edgeWeights = new HashMap();
        }
        edgeWeights.put( edge, wt);
    }

    public int numNodes() { return numHyperNodes;}
    public int numEdges() { return data.keySet().size();}
    public Collection edgeSet() { return data.keySet();}
    public Collection nodeSet( Object edge) { return (Collection)data.get(edge);}
    public boolean isWeighted() { return edgeWeights != null;}
    public Integer getWeight( Object edge) { return (Integer)edgeWeights.get( edge);}
    public Collection hyperNodes() { return data.values();}
    public void putHyperedge( Object edge, Collection col) { data.put( edge, col);}

}//end class Hypergraph

