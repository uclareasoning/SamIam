package il2.inf.edgedeletion;

import il2.util.*;
import il2.model.*;

import il2.inf.Algorithm;
import il2.inf.Algorithm.Setting;
import il2.inf.Algorithm.Result;

import java.util.Map;

public class EDAlgorithm implements il2.inf.JointEngine {

    EDNetwork edNet;
    int[][] inEdgesDeleted; // original copy
    int[][] edgesDeleted;   // sorted
    int[] oldToNewEdge;     // maps in edgelist index to sorted edgelist index
    int ned;

    // this is the inference engine for the approximate network
    il2.inf.PartialDerivativeEngine ie;
    /** 2008-10-30 4:33 PM PDT -- first time successfully running EDBP with RC sub-algorithm
        @author keith cascio
        @since  20081030 */
    public static final Algorithm ALGORITHM_DEFAULT = Algorithm.zeroconscioushugin;
    protected Algorithm algorithm;
    protected Map<Setting,?> settings;
    IntMap evidence;   // original network evidence

    // settings
    protected int maxIterations;
    protected long timeoutMillis;
    protected double convThreshold;

    // default settings
    public static final int    INT_MAX_ITERATIONS_DEFAULT  = (int)     100;
    public static final long   LONG_TIMEOUT_MILLIS_DEFAULT = (long)  10000;
    public static final double DOUBLE_THRESHOLD_DEFAULT    = (double)10e-8;

    // stats
    protected int iterations = -1;
    protected long timeMillis = -1;
    protected boolean converged = false;
    protected double residual;

    protected double compilationTime=Double.NaN;
    protected double propagationTime=Double.NaN;
    protected double edgerankingTime=Double.NaN;
    protected double correlationTime=Double.NaN;

    protected boolean isValid;

    /** @author keith cascio
        @since  20080309 */
    public EDAlgorithm die(){
        try{
            this        .edNet              = null;
            if(   edgesDeleted != null ){ java.util.Arrays.fill( edgesDeleted, null ); }
            this .edgesDeleted              = null;
            this           .ie              = null;
            if(       evidence != null ){ evidence.clear(); }
            this     .evidence              = null;
            this     .settings              = null;
            this    .algorithm              = null;
        }catch( Exception thrown ){
            System.err.println( "warning: EDAlgorithm.die() caught " + thrown );
        }

        return this;
    }

    public EDAlgorithm( BayesianNetwork bn, int[][] ed, 
                        int mi, long tm, double ct, 
                        Algorithm alg, Map<Setting,?> settings ){
        long start = System.nanoTime();

        this.maxIterations  = mi;
        this.timeoutMillis  = tm;
        this.convThreshold  = ct;
        this.evidence       = new IntMap();
        this.inEdgesDeleted = ed;
        this.edgesDeleted   = ed.clone();
        this.oldToNewEdge   = EDEdgeDeleter.sortEdges( this.edgesDeleted );
        this.ned            = ed.length;
        this.algorithm      = alg;
        this.settings       = settings;

        this.edNet          = new EDNetwork( bn, edgesDeleted );
        startEngine();

        long finish = System.nanoTime();
        compilationTime=(finish-start)*1e-6;
    }

    /** EDAlgorithm instance with all edges deleted.  Corresponds to
     * Loopy-BP.  This is not recommended unless you specifically want
     * all edges deleted (the tree case, also corresponds to Loopy-BP
     * and is preferrable).
     */
    public EDAlgorithm(BayesianNetwork bn) {
        this(bn,EDEdgeDeleter.getAllEdges(bn));
    }

    public EDAlgorithm( BayesianNetwork bn, int[][] edgesDeleted ){
        this( bn, edgesDeleted,
          INT_MAX_ITERATIONS_DEFAULT,
          LONG_TIMEOUT_MILLIS_DEFAULT,
          DOUBLE_THRESHOLD_DEFAULT,
          ALGORITHM_DEFAULT,
          new java.util.EnumMap<Setting,Object>( Setting.class ) );
    }

    /** @author keith cascio
        @since  20080223 */
    public EDNetwork edNet(){
        return this.edNet;
    }

    /****************************************
     ***  statistics and settings functions
     ****************************************/

    /** @since 20080227 */
    public int    getMaxIterations(){ return this.maxIterations; }
    /** @since 20080227 */
    public long   getTimeoutMillis(){ return this.timeoutMillis; }
    /** @since 20080227 */
    public double getConvThreshold(){ return this.convThreshold; }

    public int iterations() { return iterations; }
    public long timeMillis() { return timeMillis; }
    public boolean converged() { return converged; }

    public void setMaxIterations( int mi )    { this.maxIterations = mi; }
    public void setTimeoutMillis( long tm )   { this.timeoutMillis = tm; }
    public void setConvThreshold( double ct ) { this.convThreshold = ct; }

    public int[][] edgesDeleted() { return inEdgesDeleted; }

    /**
     * Given edge index i, returns CPT of clone variable.
     */
    public Table getCloneCPT(int oldEdge) {
        int edge = oldToNewEdge[oldEdge];
        return edNet.getCloneEdgeTable(edge);
    }
    /**
     * Given edge index i, returns CPT of soft evidence variable.
     */
    public Table getSoftEvidenceCPT(int oldEdge) {
        int edge = oldToNewEdge[oldEdge];
        return edNet.getSoftEvidenceEdgeTable(edge);
    }

    /****************************************
     ***  Inference Engine Functions
     ****************************************/

    protected void startEngine() {
        Table[] newTables = edNet.newTables();
        if( false ){
            java.util.Random r = new java.util.Random(2008);
            // this is the old, hard-coded way of constructing an ie
            java.util.Collection subdomains=java.util.Arrays.asList(newTables);
            IntList order = il2.inf.structure.EliminationOrders.minFill(subdomains, 6, r).order;
            // this JT construction has quadratic space complexity
            il2.inf.structure.EliminationOrders.JT jt = il2.inf.structure.EliminationOrders.traditionalJoinTree( subdomains, order, null, null );
            // this JT construction has linear space complexity
            //il2.inf.structure.EliminationOrders.JT jt = il2.inf.structure.EliminationOrders.bucketerJoinTree( subdomains, order, null, null );
            // I reccommend using a normalizing algorithm, to prevent underflow
            //ie = il2.inf.jointree.NormalizedZCAlgorithm.create(newTables,jt);
        }
        ie = (il2.inf.PartialDerivativeEngine) algorithm.compile( new BayesianNetwork( newTables ), settings ).get( Result.partialderivativeengine );
        this.isValid = false;
    }

    protected void dampPotential(double[] oldvals, double[] newvals) {
        // place-holder for EDMP
    }

    private double commitEdgePotentials(int edge, double[] pm, double[] se) {
        double residual = 0.0, cur;
        Table pmTable = edNet.getPmTable(edge);
        Table seTable = edNet.getSeTable(edge);
        double[] oldpm = pmTable.values();
        double[] oldse = seTable.values();

        for (int j = 0; j < pm.length; j++) {
            cur = pm[j]-oldpm[j];
            if (cur<0) cur=-cur; if (cur>residual) residual=cur; // max
            cur = se[j]-oldse[j];
            if (cur<0) cur=-cur; if (cur>residual) residual=cur; // max
        }

        dampPotential(oldpm,pm);
        dampPotential(oldse,se);

        // note that we still need to commit tables to engine
        edNet.setPmTable(edge,new Table(pmTable,pm));
        edNet.setSeTable(edge,new Table(seTable,se));

        return residual;
    }

    /****************************************
     ***  Helper functions
     ****************************************/

    /****************************************
     ***  functions for parametrizing the edges of a network
     ****************************************/

    protected double[] computePmTable(int edge) {
        return ie.tablePartial( edNet.tableIndexOfEdgeTail(edge) ).values();
    }

    protected double[] computeSeTable(int edge) {
        return ie.tablePartial( edNet.tableIndexOfEdgeHead(edge) ).values();
    }

    protected void normalizeEdgeParameters(double[] t1, double[] t2) {
        double sum1 = 0.0, sum2 = 0.0;
        for (int i = 0; i < t1.length; i++) {
            sum1 += t1[i];
            sum2 += t2[i];
        }
        for (int i = 0; i < t1.length; i++) {
            if (sum1 > 0.0) t1[i] /= sum1;
            if (sum2 > 0.0) t2[i] /= sum2;
        }
    }

    Boolean use_seed = false;
    Table[] seed_pms;
    Table[] seed_ses;
    public void setSeedTables(Table[] pms, Table[] ses) {
        seed_pms = pms;
        seed_ses = ses;
        use_seed = true;
    }
    private void resetTables() {
        if ( use_seed ) edNet.resetTables(seed_pms,seed_ses);
        else edNet.resetTables();
        updateIeTables();
    }

    private void updateIeTables() {
        int v1, v2;
        for (int edge = 0; edge < ned; edge++) {
            v1 = edNet.tableIndexOfEdgeHead(edge);
            v2 = edNet.tableIndexOfEdgeTail(edge);
            ie.setTable(v1,edNet.getPmTable(edge));
            ie.setTable(v2,edNet.getSeTable(edge));
        }
    }

    protected long startTime;
    protected void initIterationStatus() {
        this.converged = false;
        this.residual = Double.POSITIVE_INFINITY;
        this.iterations = 0;
        this.timeMillis = 0;
        this.startTime = System.nanoTime();
    }

    /** @author keith cascio
        @since  20080311 */
    public boolean limitsExceeded(){
        if ( this.maxIterations != 0 )
            if ( this.iterations >= this.maxIterations ) return true;
        if ( this.timeoutMillis != 0 )
            if ( this.timeMillis >= this.timeoutMillis ) return true;
        return false;
    }

    /* returns false if last iteration converged or we exceed given
     * limits, returns true otherwise */
    public boolean iterationStatusOk() {
        if ( this.maxIterations != 0 )
            if ( this.iterations >= this.maxIterations ) return false;
        if ( this.timeoutMillis != 0 )
            if ( this.timeMillis >= this.timeoutMillis ) return false;

        // we can't converge in the first iteration
        if ( this.iterations == 1 ) return true;

        return residual > convThreshold;
    }

    protected boolean iterationStatusTimeOk() {
        double timeMillis = (System.nanoTime() - this.startTime)*1e-6;
        if ( this.timeoutMillis != 0 )
            if ( timeMillis >= this.timeoutMillis ) return false;
        return true;
    }

    /* this updates iteration status; to be executed at bottom of
     * iteration */
    protected void updateIterationStatus() {
        this.iterations++;
        double time = (System.nanoTime() - this.startTime)*1e-6;
        this.timeMillis = (long)time;
    }

    protected void parametrizeEdges() {
        resetTables();
        initIterationStatus();

        double[] pmTable, seTable;
        double curresidual;
        while ( iterationStatusOk() ) {
            residual = 0;
            for (int i = 0; i < ned; i++) {
                pmTable = computePmTable(i); seTable = computeSeTable(i);
                normalizeEdgeParameters(pmTable,seTable);
                curresidual = commitEdgePotentials(i, pmTable, seTable);
                if ( curresidual > residual ) residual = curresidual;
                if ( !iterationStatusTimeOk() ) break;
            }
            updateIeTables();
            updateEdgeConvergenceScores();
            updateIterationStatus();
        }
        this.converged = residual <= convThreshold && this.iterations > 1;
    }

    public double oneMoreIteration(boolean restart) {
        if ( restart ) {
            isValid = true; //always true in this mode
            resetTables();
            initIterationStatus();
            return residual;
        }

        double[] pmTable, seTable;
        double curresidual;
        residual = 0;
        for (int i = 0; i < ned; i++) {
            pmTable = computePmTable(i); seTable = computeSeTable(i);
            normalizeEdgeParameters(pmTable,seTable);
            curresidual = commitEdgePotentials(i, pmTable, seTable);
            if ( curresidual > residual ) residual = curresidual;
        }
        updateIeTables();
        updateEdgeConvergenceScores();
        updateIterationStatus();
        this.converged = residual <= convThreshold && this.iterations > 1;
        return residual;
    }

    /****************************************
     *** Edge Corrections
     ****************************************/

    // assumes var is not in evidence
    /*
    public Table varConditionalCorrected(int oldvar) {
        makeValid();

        int newvar = edNet.oldToNewVar(oldvar);
        Domain oldd = edNet.oldDomain();
        if ( this.evidence.keys().contains(newvar) )
            return Table.evidenceTable(oldd,oldvar,this.evidence.get(newvar));

        int size = oldd.size(oldvar);
        double[] values = new double[ size ];
        IntMap e = new IntMap( this.evidence );
        double[] check = ie.varConditional(newvar).values();
        for (int i = 0; i < size; i++) {
            if ( check[i] == 0.0 ) continue;
            e.put(newvar,i);
            ie.setEvidence(e);
            values[i] = prEvidenceCorrected2();
        }
        ie.setEvidence( this.evidence );

        Table t = new Table(oldd, new IntSet(new int[]{oldvar}), values);
        t.normalizeInPlace();
        return t;
    }
    */

    public double getZeroMiCorrection() {
        double[] z_ij = getZeroMiCorrections();
        double z = 1.0;
        for (int edge = 0; edge < ned; edge++)
            z *= z_ij[edge];
        return z;
    }

    public double getLogZeroMiCorrection() {
        double[] z_ij = getZeroMiCorrections();
        double z = 0.0;
        for (int edge = 0; edge < ned; edge++)
            z += Math.log( z_ij[edge] );
        return z;
    }

    public double getGeneralEdgeCorrection() {
        double[] y_ij = getGeneralEdgeCorrections();
        double y = 1.0;
        for (int edge = 0; edge < ned; edge++)
            y *= y_ij[edge];
        return y;
    }

    /*
    public double getLogGeneralEdgeCorrection() {
        double[] y_ij = getGeneralEdgeCorrections();
        double y = 1.0;
        for (int edge = 0; edge < ned; edge++)
            y += Math.log( y_ij[edge] );
        return y;
    }
    */

    public double[] getZeroMiCorrections() {
        double[] z_ij = new double[ned];
        for (int edge = 0; edge < ned; edge++) {
            Table t1 = edNet.getPmTable(edge);
            Table t2 = edNet.getSeTable(edge);
            z_ij[edge] = t1.dotProduct(t2);
        }
        return z_ij;
    }

    public double[] ecTimes;
    public double[] getGeneralEdgeCorrections() {
        this.ecTimes = new double[ ned ];

        Domain d = edNet.newDomain();
        double[] y_ij = new double[ned];

        double Z = ie.prEvidence();
        if ( Z == 0.0 ) return y_ij; //AC

        for (int edge = 0; edge < ned; edge++) {
            long start=System.nanoTime();

            int parent = edNet.newVarOfEdge(edge);
            int clone  = edNet.cloneVarOfEdge(edge);
            int numstates = d.size(parent);

            Table t1 = edNet.getSeTable(edge);
            Table t2 = edNet.getPmTable(edge);
            double z_ij = t1.dotProduct(t2);
            int i1 = edNet.tableIndexOfEdgeTail(edge);
            int i2 = edNet.tableIndexOfEdgeHead(edge);

            double[] check1 = ie.varConditional(parent).values();
            double[] check2 = ie.varConditional(clone).values();
            for (int state = 0; state < numstates; state++) {
                if ( check1[state] == 0.0 || check2[state] == 0.0 ) continue;
                Table d1 = Table.evidenceTable(d,parent,state);
                Table d2 = Table.evidenceTable(d,clone,state);
                ie.setTable(i1,d1);
                ie.setTable(i2,d2);
                double curZ = ie.prEvidence();
                if ( curZ == 0.0 || Double.isNaN(curZ) ) continue; //AC
                y_ij[edge] += curZ;
            }
            ie.setTable(i1,t1);
            ie.setTable(i2,t2);
            y_ij[edge] *= z_ij / Z;

            long finish=System.nanoTime();
            this.ecTimes[ edge ] = (finish-start)*1e-6;
        }

        return y_ij;
    }

    public double[] edgeCorrelations() {
        long start = System.nanoTime();

        IntMap e = new IntMap(this.evidence);
        double[] cor_ij = new double[ned];
        java.util.Arrays.fill(cor_ij,Double.NaN);
        double logpre = ie.logPrEvidence();
        if ( Double.isInfinite(logpre) && logpre < 0.0 )
            return cor_ij;

        double[] pps = new double[ned];
        double[] pcs = new double[ned];
        for (int edge = 0; edge < ned; edge++) {
            int parent = edNet.newVarOfEdge(edge);
            int clone  = edNet.cloneVarOfEdge(edge);
            pps[edge] = ie.varConditional(parent).values()[0];
            pcs[edge] = ie.varConditional(clone).values()[0];
        }

        int edge = 0;
        while ( edge < ned ) {
            int parent = edNet.newVarOfEdge(edge);
            double pp = pps[edge];
            double pvar = pp*(1.0-pp);
            if ( evidence.keys().contains(parent) || pvar == 0.0 ) {
                edge++;
                continue;
            }
            e.put(parent,0);
            ie.setEvidence(e);
            while ( edge<ned && edNet.newVarOfEdge(edge)==parent ) {
                int clone  = edNet.cloneVarOfEdge(edge);
                double pc = pcs[edge];
                double cvar = pc*(1.0-pc);
                if ( cvar == 0.0 ) {
                    edge++;
                    continue;
                }
                double ppc = ie.varConditional(clone).values()[0]*pp;
                cor_ij[edge] = (ppc-pp*pc)/Math.sqrt(pvar*cvar);
                if ( cor_ij[edge] > 1.0 || cor_ij[edge] < -1.0 )
                    cor_ij[edge] = Double.NaN;
                //System.out.printf("?? %.8f,%.8f,%.8f,%.8f ??\n",
                //                ppc,pp,pc,cor_ij[edge]);
                edge++;
            }
            e.remove(parent);
            ie.setEvidence(e);
        }
        ie.setEvidence(this.evidence);

        long finish = System.nanoTime();
        correlationTime = (finish-start)*1e-6;
        return cor_ij;
    }

    /****************************************
     *** Edge Ranking Heuristics
     ****************************************/

    /** @since  20081021
        @author keith cascio */
    public enum RankingHeuristic {
        mi( "mutual information" ) {
            public int[][] rank( EDAlgorithm eda ) { 
                return eda.rankEdgesByMi(); 
            } 
        },
        residual( "residual recovery" ) {
            public int[][] rank( EDAlgorithm eda ) { 
                return eda.rankEdgesByConvergence();
            } 
        },
        random( "random" ) {
            public int[][] rank( EDAlgorithm eda ) {
                return eda.rankEdgesRandomly( new java.util.Random() );
            }
        };
        abstract public int[][] rank( EDAlgorithm eda );

        private RankingHeuristic( String name ){ this.name = name; }
        public final String name;

        public String toString(){ return name; }

        static public final RankingHeuristic DEFAULT = mi;
    }

    public int[][] rankEdgesByScore( double[] scores ) {
        int[][] miEdges = edgesDeleted.clone();
        EdgeComparator ec = new EdgeComparator(miEdges,scores);
        java.util.Arrays.sort(miEdges,ec);

        return miEdges;
    }

    /* this takes more time than needed: O(n log n) time (for sorting)
     * rather than O(n) possible random shuffle (as in
     * Collections.shuffle())
     */
    public int[][] rankEdgesRandomly(java.util.Random r) {
        long start = System.nanoTime();

        double[] scores = new double[ned];
        for (int i = 0; i < ned; i++) scores[i] = r.nextDouble();
        int[][] edges = rankEdgesByScore( scores );

        long finish = System.nanoTime();
        edgerankingTime = (finish-start)*1e-6;
        return edges;
    }

    public int[][] rankEdgesByMi() {
        long start = System.nanoTime();

        double[] scores = edgeMis();
        int[][] edges = rankEdgesByScore( scores );

        long finish = System.nanoTime();
        edgerankingTime = (finish-start)*1e-6;
        return edges;
    }

    public int[][] rankEdgesByTargetMi(int qvar) {
        double[] scores = edgeTargetedMis(qvar);
        return rankEdgesByScore( scores );
    }

    public int[][] rankEdgesByCorrelation() {
        double[] scores = edgeCorrelations();
        return rankEdgesByScore( scores );
    }

    public int[][] rankEdgesByConvergence() {
        long start = System.nanoTime();

        double[] scores = edgeConvergenceScores();
        int[][] edges = rankEdgesByScore( scores );

        long finish = System.nanoTime();
        edgerankingTime = (finish-start)*1e-6;
        return edges;
    }

    private double LOG_2 = Math.log(2);
    private double entropy(double[] vals) {
        double ent = 0.0;
        for (int i = 0; i < vals.length; i++)
            if ( vals[i] <= 0.0 ) continue;
            else ent -= vals[i]*Math.log(vals[i]);
        return ent/LOG_2;
    }

    /* mutual information */
    public double[] edgeMis() {
        double[] mis = new double[ned];
        double[] edgeEntropies = edgeEntropies();

        int edge = 0;
        while ( edge < ned ) {
            int parent = edNet.newVarOfEdge(edge);
            double[] parentVals = ie.varConditional(parent).values();
            double parentEntropy = entropy(parentVals);

            while ( edge<ned && edNet.newVarOfEdge(edge)==parent ) {
                int clone = edNet.cloneVarOfEdge(edge);
                double[] cloneVals = ie.varConditional(clone).values();
                double cloneEntropy = entropy(cloneVals);
                mis[edge] = parentEntropy + cloneEntropy - edgeEntropies[edge];
                edge++;
            }
        }

        return mis;
    }

    protected double[][] edgeNodeEntropies() {
        double[][] entropies = new double[ned][2];
        for (int edge = 0; edge < ned; edge++) {
            int parent = edNet.newVarOfEdge(edge);
            int clone  = edNet.cloneVarOfEdge(edge);

            entropies[edge][0] = entropy( ie.varConditional(parent).values() );
            entropies[edge][1] = entropy( ie.varConditional(clone).values() );
        }
        return entropies;
    }

    protected double[] nodeEntropies() {
        int size = edNet.newDomain().size();
        double[] entropies = new double[size];
        for (int var = 0; var < size; var++)
            entropies[var] = entropy( ie.varConditional(var).values() );
        return entropies;
    }

    /*
     * returns ENT(X_i,X_j) for each edge (i,j) deleted
     */
    protected double[] edgeEntropies() { //AC
        return edgeEntropies( this.evidence, 0 );
    }

    protected double[] edgeEntropies(IntMap evidence, int start) { //AC
        IntMap e = new IntMap(evidence);
        double[] entropies = new double[ned];
        int edge = start;

        // precompute parent values: this saves propagation for ie
        double[][] parentValsList = new double[ned][];
        while ( edge < ned ) {
            int parent = edNet.newVarOfEdge(edge);
            parentValsList[edge] = ie.varConditional(parent).values();
            while ( edge<ned && edNet.newVarOfEdge(edge)==parent ) edge++;
        }

        edge = start;
        while ( edge < ned ) {
            int parent = edNet.newVarOfEdge(edge);
            // if parent Y already instantiated, ENT(X,Y) = ENT(X)
            if ( evidence.keys().contains(parent) ) {
                int clone = edNet.cloneVarOfEdge(edge);
                double[] cloneVals = ie.varConditional(clone).values();
                entropies[edge] = entropy( cloneVals );
                edge++;
                continue;
            }

            double[] parentVals = parentValsList[edge];
            int numstates = edNet.newDomain().size(parent);
            for (int state = 0; state < numstates; state++) {
                if ( parentVals[state] == 0.0 ) continue;
                e.put(parent,state);
                ie.setEvidence(e);
                int curedge = edge;
                while ( curedge<ned && edNet.newVarOfEdge(curedge)==parent ) {
                    int clone = edNet.cloneVarOfEdge(curedge);
                    double[] cloneVals = ie.varConditional(clone).values();
                    entropies[curedge] += parentVals[state]*entropy(cloneVals);
                    curedge++;
                }
            }
            e.remove(parent);
            ie.setEvidence(e);

            double parentEntropy = entropy(parentVals);
            while ( edge<ned && edNet.newVarOfEdge(edge)==parent ) {
                entropies[edge] += parentEntropy;
                edge++;
            }
        }
        ie.setEvidence(evidence);
        return entropies;
    }

    public double[] edgeTargetedMis(int query_var) {
        IntMap e = new IntMap(this.evidence);
        double[][] node_entropies = edgeNodeEntropies();
        double[]   edge_entropies = edgeEntropies();

        int qvar = edNet.oldToNewVar(query_var);
        int qsize = edNet.newDomain().size(qvar);
        double[] qcond = ie.varConditional(qvar).values();
        double[] mis = new double[ned];

        if ( this.evidence.keys().contains(qvar) ) //AC
            throw new IllegalStateException();

        // ENT(X_i,X_j|Q) = \sum_q Pr(q) ENT(X_i,X_j|q)
        // ENT(X|Q)       = \sum_q Pr(q) ENT(X|q)
        double[][] node_entropies_q = new double[ned][2];
        double[]   edge_entropies_q = new double[ned];
        double[][] node_curent;
        double[]   edge_curent;
        for (int state = 0; state < qsize; state++) {
            if ( qcond[state] == 0.0 ) continue;
            e.put(qvar,state);
            ie.setEvidence(e);

            node_curent = edgeNodeEntropies();
            for (int edge=0; edge<ned; edge++) {
                node_entropies_q[edge][0] += qcond[state]*node_curent[edge][0];
                node_entropies_q[edge][1] += qcond[state]*node_curent[edge][1];
            }
            edge_curent = edgeEntropies(e,0);
            for (int edge=0; edge<ned; edge++)
                edge_entropies_q[edge] += qcond[state]*edge_curent[edge];
        }

        // MI( Q; X_i,X_j ) = ENT(X_i,X_j) - ENT(X_i,X_j|Q)
        for (int edge = 0; edge < ned; edge++)
            mis[edge] = edge_entropies[edge] - edge_entropies_q[edge];

        // MI( Q,X_i; X_j ) = ENT(X_i|Q) + ENT(X_j) - ENT(X_i,X_j|Q)
        // MI( Q,X_j; X_i ) = ENT(X_j|Q) + ENT(X_i) - ENT(X_i,X_j|Q)
        for (int edge = 0; edge < ned; edge++) {
            double mi;
            mi = node_entropies_q[edge][0] + node_entropies[edge][1]
                - edge_entropies_q[edge];
            if ( mis[edge] > mi ) mis[edge] = mi;
            mi = node_entropies_q[edge][1] + node_entropies[edge][0]
                - edge_entropies_q[edge];
            if ( mis[edge] > mi ) mis[edge] = mi;
        }

        ie.setEvidence(this.evidence);
        return mis;
    }

    public boolean doConvergenceUpdates = false; //AC
    public double[] edgeConvergenceScores;
    private void updateEdgeConvergenceScores() {
        if ( ! doConvergenceUpdates ) return;
        if ( this.iterations == 0 )
            edgeConvergenceScores = new double[ned];
        if ( this.iterations < 2 ) //AC
            return;

        double[][] parentVals = new double[ned][];
        int edge = 0;
        while ( edge < ned ) {
            int parent = edNet.newVarOfEdge(edge);
            parentVals[edge] = ie.varConditional(parent).values();
            int firstedge = edge;
            while ( edge<ned && edNet.newVarOfEdge(edge)==parent ) {
                parentVals[edge] = parentVals[firstedge];
                edge++;
            }
        }

        for (edge = 0; edge < ned; edge++) {
            int parent = edNet.newVarOfEdge(edge);
            int clone = edNet.cloneVarOfEdge(edge);
            double[] pvals = parentVals[edge];
            double[] cvals = ie.varConditional(clone).values();
            double[] pmvals = edNet.getPmTable(edge).values();
            double[] sevals = edNet.getSeTable(edge).values();
            double[] sepvals = new double[pmvals.length];
            double sum = 0.0;
            for (int i=0;i<pmvals.length;i++) {
                sepvals[i] = pmvals[i]*sevals[i];
                sum += sepvals[i];
            }
            for (int i=0;i<pmvals.length;i++) sepvals[i] /= sum;

            edgeConvergenceScores[edge] += skl(pvals,cvals);
            edgeConvergenceScores[edge] += skl(pvals,sepvals);
            edgeConvergenceScores[edge] += skl(cvals,sepvals);
        }
    }

    public double kl(double[] v1, double[] v2) {
        double kl = 0.0;
        for (int i = 0; i < v1.length; i++) {
            if ( v1[i] == 0.0 ) continue;
            kl += v1[i] * ( Math.log( v1[i] / v2[i] ) );
        }
        return kl;
    }

    // symmetric KL-divergence
    public double skl(double[] v1, double[] v2) {
        return kl(v1,v2) + kl(v2,v1);
    }

    protected double[] edgeConvergenceScores() {
        return edgeConvergenceScores;
    }

    /****************************************
     *** Interface functions
     ****************************************/

    protected void makeValid() {
        if ( !isValid ) {
            long start=System.nanoTime();
            parametrizeEdges();
            //ie.prEvidence(); // AC: make ie valid
            isValid = true;
            long finish=System.nanoTime();
            propagationTime=(finish-start)*1e-6;
        }
    }

    public void setEvidence(IntMap e){
        isValid=false;
        this.evidence = edNet.oldToNewEvidence(e);
        ie.setEvidence(this.evidence);
    }

    public void setTable(int i,Table table){
        isValid=false;
        // AC: this method ought to update newTables and oldTables in
        // EDNetwork; however, I don't think this is necessary
        // ... need to check
        //newTables[i] = edNet.createNewNetworkCpt(table,i);
        Table t = edNet.createNewNetworkCpt(table,i);
        ie.setTable(i,t);
    }

    public double prEvidence() {
        makeValid();
        double pre = ie.prEvidence();
        double z = getZeroMiCorrection();
        if (pre == 0.0) return 0.0;
        return pre/z;
    }

    public double logPrEvidence() {
        makeValid();
        return ie.logPrEvidence() - getLogZeroMiCorrection();
    }

    /*
    public double prEvidenceCorrected2() {
        makeValid();
        double Z = ie.prEvidence();
        double z = getZeroMiCorrection();
        double y = getGeneralEdgeCorrection();
        return Z * (y/z);
    }
    */

    // AC: temporary?
    public Table tableJoint(int table) {
        return tableConditional(table);
    }

    public Table tableConditional(int table) {
        makeValid();
        Table newTable = ie.tableConditional(table);
        return edNet.newTableToOldTable(newTable);
    }

    // AC: temporary?
    public Table varJoint(int var) {
        return varConditional(var);
    }

    public Table varConditional(int var) {
        makeValid();
        int newvar = edNet.oldToNewVar(var);
        Table newt = ie.varConditional(newvar);
        Table oldt = edNet.newTableToOldTable(newt);
        return oldt;
    }

    // AC: this method can be implemented better
    public Table cloneConditional(int oldEdge) {
        int edge = oldToNewEdge[oldEdge];
        makeValid();
        IntSet vars = new IntSet(new int[]{ edNet.oldVarOfEdge(edge)} );
        Table table = ie.varConditional( edNet.cloneVarOfEdge(edge) );
        return new Table(edNet.oldDomain(), vars, table.values());
    }

    /* In ED, this is time to initalize the algorithm: e.g. construct
     * the approximate network (but not parametrize it) */
    public double getCompilationTime() {
        return compilationTime;
    }
    /* In ED, this is the time to parametrize edges */
    public double getPropagationTime() {
        return propagationTime;
    }
    /* In ED, this is the time to rank edges, by rankEdgesByMi(2) */
    public double getEdgeRankingTime() {
        return edgerankingTime;
    }
    /* In ED, this is the time to compute edge correlations */
    public double getCorrelationTime() {
        return correlationTime;
    }
    /* AC: I need to check if this is sufficient. */
    public double getMemoryRequirements() {
        return ie.getMemoryRequirements();
    }
    public il2.inf.structure.JoinTreeStats.Stat getClusterStats() {
        return ie.getClusterStats();
    }
    public il2.inf.structure.JoinTreeStats.Stat getSeparatorStats() {
        return ie.getSeparatorStats();
    }

    /** Returns summary of convergence and statistics.
        For SamIam GUI.
     */
    public String convergenceSummary() {
        makeValid();

        String summary;
        if ( converged )
            summary = "converged in " + iterations + " iterations, " +
                timeMillis + " milliseconds";
        else if (  maxIterations != 0 && iterations >= maxIterations )
            summary = "<font color=\"#cc0000\">did not converge</font> in " + iterations +
                " iterations (" + timeMillis + " milliseconds)";
        else if ( timeoutMillis != 0 && timeMillis >= timeoutMillis )
            summary = "<font color=\"#cc0000\">timed out; did not converge</font> in " + timeMillis +
                " milliseconds (" + iterations + " iterations)";
        else // should not reach here
            summary = "did not converge? in " + iterations +
                " iterations," + timeMillis + " milliseconds";

        summary += " (" + ned + " edges deleted)";
        return summary;
    }

    // for Guy
    public Table[] getOriginalTables(){
		return ((il2.inf.jointree.JoinTreeAlgorithm)ie).getOriginalTables();
	}

    // for Tal
    public double[] acStats(){
		return ((il2.inf.jointree.JoinTreeAlgorithm)ie).acStats();
	}

}
