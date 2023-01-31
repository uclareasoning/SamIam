package edu.ucla.belief.dtree;

import java.util.*;
import edu.ucla.util.*;


/** This class is an interface library to the C++ hMeTiS library.
 * <p>The library java_hmetis is loaded in a static initializer, but will not
 *    throw an error there.  It can be tested for using loaded().  An
 *    UnsatisfiedLinkError will be thrown if its functions are later
 *    called after failing to load the library.
 *
 * <p>The timing parameter will return the information from the system call "clock()",
 *    where [0]=the time used by the library.
 *    It will add the current amount of time to whatever is already in the array, thereby
 *    allowing a cumulative time to be kept.
 */
public class Hmetis {

    private static boolean loaded;
    public static boolean loaded() { return loaded;}

    static {
        try {
            System.loadLibrary("inflib_hmetis");
            loaded = true;
        }
        catch( Throwable e) {
            loaded = false;
//             throw new UnsatisfiedLinkError( "Hmetis: Could not load the library: java_hmetis");
        }
    }


    private Hmetis() {
    }

    /* Current list of Returned status values, see (Hmetis.cxx for most recent list):
     * 0 = sucess
     * Bad Int Values:
     * 1 = nvtxs
     * 2 = nhedges
     * 3 = nparts
     * 4 = ubfactor
     * Bad Array (length, null, ...)
     * 5 = vwgts
     * 6 = eptr
     * 7 = eind
     * 8 = hewgts
     * 9 = options
     * 10 = part
     * 11 = edgecut
     * Misc Errors
     * 12 = Bad algo number
     */
    /**Call the hMetis function, and return 0 if success, or nonzero if an error happened.*/
    public static native int
        HMETIS_PartRecursive(int nvtxs, int nhedges, int[] vwgts, int[] eptr, int[] eind,
                             int[] hewgts, int nparts, int ubfactor, int[] options,
                             int[] part, int[] edgecut, double[] timing);

    /**Call the hMetis function, and return 0 if success, or nonzero if an error happened.*/
    public static native int
        HMETIS_PartKway( int nvtxs, int nhedges, int[] vwgts, int[] eptr, int[] eind,
                         int[] hewgts, int nparts, int ubfactor, int[] options,
                         int[] part, int[] edgecut, double[] timing);



    /** Returns an integer array where each element is 0..numPartitions-1,
     *   representing the partition that hypernode was assigned.
     */
    public static int[] HMETIS_PartRecursive( Hypergraph hg, int[] options,
                                              int ubfactor, int[] edgecut, double[] timing) {
        return convertAndCall( hg, options, ubfactor, edgecut, 1, null, timing);
    }
    /** Returns an integer array where each element is 0..numPartitions-1,
     *   representing the partition that hypernode was assigned.
     */
    public static int[] HMETIS_PartRecursive( Hypergraph hg, int[] options,
                                              int ubfactor, int[] edgecut,
                                              int[] vwgts, double[] timing) {
        return convertAndCall( hg, options, ubfactor, edgecut, 1, vwgts, timing);
    }
    /** Returns an integer array where each element is 0..numPartitions-1,
     *   representing the partition that hypernode was assigned.
     */
    public static int[] HMETIS_PartKway( Hypergraph hg, int[] options,
                                         int ubfactor, int[] edgecut, double[] timing) {
        return convertAndCall( hg, options, ubfactor, edgecut, 2, null, timing);
    }
    /** Returns an integer array where each element is 0..numPartitions-1,
     *   representing the partition that hypernode was assigned.
     */
    public static int[] HMETIS_PartKway( Hypergraph hg, int[] options,
                                         int ubfactor, int[] edgecut,
                                         int[] vwgts, double[] timing) {
        return convertAndCall( hg, options, ubfactor, edgecut, 2, vwgts, timing);
    }

    private static int[] convertAndCall( Hypergraph hg, int[] options,
                                         int ubfactor, int[] edgecut, int algo,
                                         int[] in_vwgts, double[] timing) {

        if( options == null || options.length != 9) {
            throw new IllegalArgumentException("Hmetis: options");
        }
        if( timing != null) {
            if( timing.length != 1) {
                throw new IllegalArgumentException("Hmetis: timing - " + timing.length);
            }
        }


        //if no hyperedges exist, any balanced partitioning is good
        //  but hMetis cannot be called, so create default partitioning
        if( hg.numEdges() == 0) {
            int[] part = new int[hg.numNodes()];
            for( int i=0; i<part.length; i+=2) {
                part[i] = 0;
                if( i+1 < part.length) { part[i+1] = 1;}
            }
            if( edgecut != null) { edgecut[0] = 0;}
            return part;
        }


        int sizeOfEind = 0;
        for( Iterator itr = hg.hyperNodes().iterator(); itr.hasNext();) {
            sizeOfEind += ((Collection)itr.next()).size();
        }


        int nvtxs = hg.numNodes();
        int nhedges = hg.numEdges();
        int[] vwgts = in_vwgts;
        int[] eptr = new int[nhedges+1];
        int[] eind = new int[sizeOfEind];
        int[] hewgts = null;
        int nparts = 2;
        int[] part = new int[nvtxs];
        if( edgecut == null) {
            edgecut = new int[1];
            edgecut[0] = 0;
        }


        if( hg.isWeighted()) {
            hewgts = new int[nhedges];
        }


        //set eptr & eind
        int nextep = 0;
        int nextei = 0;
        for( Iterator itr1 = hg.edgeSet().iterator(); itr1.hasNext();) {
            Object hedge = itr1.next();
            Collection vertices = hg.nodeSet( hedge);
            eptr[nextep] = nextei;
            nextep++;
            for( Iterator itr2 = vertices.iterator(); itr2.hasNext();) {
                Integer vert = (Integer)itr2.next();
                eind[nextei] = vert.intValue();
                nextei++;
            }
            try{
            if( hewgts != null) {
                hewgts[nextep-1] = hg.getWeight( hedge).intValue();
            }
            }
            catch( NullPointerException e) { //edge didn't have a weight, default to 2 (so context edges can be 1)
                hewgts[nextep-1] = 2;
            }
        }
        eptr[nextep] = nextei;
        nextep++;

        if( nextep != eptr.length) { throw new IllegalStateException("nextep");}
        if( nextei != eind.length) { throw new IllegalStateException("nextei");}



        //For debug purposes
//System.out.println("nvtxs:" + nvtxs);
//System.out.println("nhedges:" + nhedges);
//System.out.print("vwgts:");       IntArrays.print( vwgts);
//System.out.print("eptr:");        IntArrays.print( eptr);
//System.out.print("eind:");        IntArrays.print( eind);
//System.out.print("hewgts:");      IntArrays.print( hewgts);
//System.out.println("nparts:" + nparts);
//System.out.println("ubfactor:" + ubfactor);
//System.out.print("options");      IntArrays.print( options);
//System.out.print("part");         IntArrays.print( part);
//System.out.print("edgecut");      IntArrays.print( edgecut);
        //end For debug purposes

        int status;
        if( algo == 1) {
            status = HMETIS_PartRecursive( nvtxs, nhedges, vwgts, eptr, eind,
                                           hewgts, nparts, ubfactor, options,
                                           part, edgecut, timing);
        }
        else if( algo == 2) {
            status = HMETIS_PartKway( nvtxs, nhedges, vwgts, eptr, eind,
                                      hewgts, nparts, ubfactor, options,
                                      part, edgecut, timing);
        }
        else { status = -1;}

        if( status == 0) {
            return part;
        }
        else {
            throw new IllegalArgumentException("Hmetis: library failed to run (code:" + status + ")");
        }
    }
}
