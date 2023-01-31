package edu.ucla.belief.dtree;

import java.util.*;


/** This abstract class can create a Dtree, using the hMetis algorithm/library
 *  
 * @author David Allen
 */

public abstract class DtreeCreateHMetis extends Dtree.Create {

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

    public DtreeCreateHMetis( int[] options, int ubfactor, int numGlobalTrials, int numLocalTrials) {
        if( options == null) { throw new IllegalArgumentException("DtreeCreateHMetis: options");}
        if( options.length != 9) { throw new IllegalArgumentException("DtreeCreateHMetis: options.length");}
        if( numGlobalTrials <= 0) { throw new IllegalArgumentException("DtreeCreateHMetis: numGlobalTrials");}
        if( numLocalTrials <= 0) { throw new IllegalArgumentException("DtreeCreateHMetis: numLocalTrials");}

        this.options = options;
        this.ubfactor = ubfactor;
        this.numGlobalTrials = numGlobalTrials;
        this.numLocalTrials = numLocalTrials;
        resetTiming();
    }

    public DtreeCreateHMetis( int ubfactor, int numGlobalTrials, int numLocalTrials) {
        if( numGlobalTrials <= 0) { throw new IllegalArgumentException("DtreeCreateHMetis: numGlobalTrials");}
        if( numLocalTrials <= 0) { throw new IllegalArgumentException("DtreeCreateHMetis: numLocalTrials");}

        this.ubfactor = ubfactor;
        this.numGlobalTrials = numGlobalTrials;
        this.numLocalTrials = numLocalTrials;
        resetTiming();
    }

    public void resetTiming() { Arrays.fill( timing, 0.0);}

    //in the future could add more timing elements to the timing array (e.g. the unix call times() returns
    //  more information than the currently used clock() command.
    /**Currently returns an array of length one, containing the same information as getHmetisTime()*/
    public double[] getTiming() { return (double[])timing.clone();}

    /**Returns the amount of time (in seconds) spent in the hMetis library (just the c++ calls to the library) 
     *  since the last call to resetTiming or construction.
     */
    public double getHmetisTime() { return timing[0];}

}//end class DtreeCreateHMetis
