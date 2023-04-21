package il2.inf.structure;

import edu.ucla.structure.Graph;
import il2.util.*;
import il2.model.Index;

import java.util.*;
import java.math.BigInteger;

/** This abstract class can create a Dgraph, using the hMetis algorithm/library
 *
 * @author David Allen
 */

public abstract class DgraphCreateHMetis {

    static final private boolean DEBUG_CreateHM = false;

    public String abrev() { return "HM_" + numGlobalTrials + "_" + numLocalTrials;}
    public String name() { return "Create-hMetis_" + numGlobalTrials + "_" + numLocalTrials;}

    int options[] = {0,0,0,0,0,0,0,0,0};
    //HMETIS_PartRecursive
    //[0]:0=use defaults, 1=use following
    //[1]:Nruns: Number of different bisections (10)
    //[2]:CType: 1=HFC, 2=FC, 3=GFC, 4=HEC, 5=EC (1)
    //[3]:RType: 1=FM, 2=one-way FM, 3=FMee (1)
    //[4]:Vcycle: 0=noV, 1=1finalV, 2=VatEachLevel, 3=VonAll (1)
    //[5]:Reconst: 0=Remove cut edges, 1=save cut edges (0)
    //[6]:0=no preassign, 1=use part as preassign (0)
    //[7]:Random seed (negative=randomly generated seed)
    //[8]:dbglvl (debug level) (0)

    final int ubfactor;
    final int numGlobalTrials;
    final int numLocalTrials;

    double[] timing = new double[1];

    public DgraphCreateHMetis( int[] options, int ubfactor, int numGlobalTrials, int numLocalTrials) {
        if( options == null) { throw new IllegalArgumentException("DgraphCreateHMetis: options");}
        if( options.length != 9) { throw new IllegalArgumentException("DgraphCreateHMetis: options.length");}
        if( numGlobalTrials <= 0) { throw new IllegalArgumentException("DgraphCreateHMetis: numGlobalTrials");}
        if( numLocalTrials <= 0) { throw new IllegalArgumentException("DgraphCreateHMetis: numLocalTrials");}

        this.options = options;
        this.ubfactor = ubfactor;
        this.numGlobalTrials = numGlobalTrials;
        this.numLocalTrials = numLocalTrials;
        resetTiming();
    }

    public DgraphCreateHMetis( int ubfactor, int numGlobalTrials, int numLocalTrials) {
        if( numGlobalTrials <= 0) { throw new IllegalArgumentException("DgraphCreateHMetis: numGlobalTrials");}
        if( numLocalTrials <= 0) { throw new IllegalArgumentException("DgraphCreateHMetis: numLocalTrials");}

        this.ubfactor = ubfactor;
        this.numGlobalTrials = numGlobalTrials;
        this.numLocalTrials = numLocalTrials;
        resetTiming();
    }

    abstract public DGraph create( Collection data);

    public void resetTiming() { Arrays.fill( timing, 0.0);}

    //in the future could add more timing elements to the timing array (e.g. the unix call times() returns
    //  more information than the currently used clock() command.
    /**Currently returns an array of length one, containing the same information as getHmetisTime()*/
    public double[] getTiming() { return (double[])timing.clone();}

    /**Returns the amount of time (in seconds) spent in the hMetis library (just the c++ calls to the library)
     *  since the last call to resetTiming or construction.
     */
    public double getHmetisTime() { return timing[0];}


	static void addLeafNodes( Graph tree, ArrayList leafNodes, Map leafNodesToInts) {
		for( int i=0; i<leafNodes.size(); i++) {
			Integer n = new Integer(i);
			tree.add(n);
			leafNodesToInts.put( leafNodes.get(i), n);
		}
	}


	static DGraph selectBestTree( DGraph dg1, DGraph dg2) {

		BigInteger v1 = dg1.largestClusterSize();
		BigInteger v2 = dg2.largestClusterSize();

		if( !v1.equals( v2 ) ){
			if( v1.max( v2 ) == v2 ) { return dg1; }
			else { return dg2; }
		}

		v1 = dg1.largestContextSize();
		v2 = dg2.largestContextSize();
		if( !v1.equals( v2 ) ){
			if( v1.max( v2 ) == v2 ) { return dg1; }
			else { return dg2; }
		}

		int h1 = dg1.height();
		int h2 = dg2.height();
		if( h1 < h2) { return dg1;}
		else if( h1 > h2) { return dg2;}

		return dg1;
	}

	static Map createClusters( Graph tree, ArrayList leafNodes) {
		Map clusters = new HashMap( leafNodes.size() * 3);

		for( int i=0; i<leafNodes.size(); i++) {
			Integer n = new Integer(i);
			clusters.put( n, new Index((Index)leafNodes.get(i)));
		}

		for( int i=leafNodes.size(); i<tree.size(); i++) {
			Integer n = new Integer(i);

			Set nei = tree.neighbors( n); //either 2(root) or 3(internal)
			//All of n's children are less than n, therefore find the
			//  two neighbors which are its children
			Integer n1 = null;
			Integer n2 = null;

			{
				Iterator itr = nei.iterator();
				if( nei.size() == 2) {
					n1 = (Integer)itr.next();
					n2 = (Integer)itr.next();
				}
				else if( nei.size() == 3) {
					Integer t1 = (Integer)itr.next();
					Integer t2 = (Integer)itr.next();
					Integer t3 = (Integer)itr.next();

					if( t1.intValue() >= n.intValue()) {
						n1 = t2;
						n2 = t3;
					}
					else if( t2.intValue() >= n.intValue()) {
						n1 = t1;
						n2 = t3;
					}
					else {
						n1 = t1;
						n2 = t2;
					}
				}
				else {
					throw new IllegalStateException("dtree node has too many neighbors");
				}

				if( n1.intValue() >= n.intValue()) { throw new IllegalStateException( "var " + n + " neighbors + " + nei);}
				if( n2.intValue() >= n.intValue()) { throw new IllegalStateException( "var " + n + " neighbors + " + nei);}
			}


			//create cluster for n, by combining the cluster of n1 & n2
			Index n1_i = (Index)clusters.get( n1);
			Index n2_i = (Index)clusters.get( n2);
			clusters.put( n, n1_i.combineWith( n2_i));
		}
		return clusters;
	}

	static void removeRoot( Graph tree, Map clusters) {
		Integer rt = new Integer( tree.size()-1);
		Set neighbors = new HashSet( tree.neighbors(rt));
		if( neighbors.size()!=2) { throw new IllegalStateException();}
		Iterator iter = neighbors.iterator();
		Object n1 = iter.next();
		Object n2 = iter.next();
		tree.remove( rt);
		tree.addEdge( n1, n2);
		clusters.remove( rt);
	}

	static IntSet vars( Collection leafNodes) {
		IntSet ret = new IntSet();
		for( Iterator itr = leafNodes.iterator(); itr.hasNext();) {
			Index lf = (Index)itr.next();
			ret = ret.union( lf.vars());
		}
		return ret;
	}

}//end class DtreeCreateHMetis
