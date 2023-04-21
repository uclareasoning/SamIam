package il2.inf.bp;

import il2.inf.bp.schedules.*;
import il2.model.Domain;
import il2.model.Table;
import il2.util.IntMap;
import il2.util.IntSet;
import il2.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * some implementation notes:
 *
 * = we perform message-passing on the factor graph representation of
 * a given Table[] array.  This is not exactly belief propagation on
 * a Bayesian network, although the approximations will be the same.
 *
 * = this implementation employs a somewhat minor performance
 * optimization.  Namely, we do not pass messages to and from a factor
 * mentioning a single variable.  I may refer to this as a "unit
 * table" (analagous to unit clause), or sometimes as a "twig" (as in
 * the treewidth-based reduction rule).  This enhancement may be
 * effective in networks with many unit tables, e.g., as in the large
 * grid graphs used in low-level vision tasks such as stereo.
 *
 * This type of message passing is equivalent to ED-BP where we delete
 * ALL factor graph edges EXCEPT twig edges (edges from a node to a
 * unit factor).
 *
 * = I use Shenoy-Shafer style message passing; Hugin style message
 * passing should make things faster; at least for passing messages
 * from variable nodes.  Hugin-style message passing may lead to
 * significant speed-up at some relatively small expense in memory
 * requirements.  Hugin-style message passing may not be desirable for
 * factor nodes, as they require us to maintain factor marginals.
 */

public abstract class BeliefPropagation {
    protected Domain domain;
    protected Table[] tables; // original network tables
    protected IntSet[] varTables; // keep track of unit tables
    protected IntMap evidence;
    protected Table[] evidenceIndicators;

    protected MessagePassingScheduler scheduler;
    protected HashMap<Pair,Table> messages;
    // protected HashMap<Pair,Table> oldMessages;

    protected boolean isValid;

    protected int    maxIterations;
    protected long   timeoutMillis;
    protected double convThreshold;

    protected boolean converged;
    protected double  residual;
    protected double  iterationTime;
    protected int     numIterations;

    protected double compilationTime = Double.NaN;
    protected double propagationTime = Double.NaN;

    public BeliefPropagation(Table[] tables, MessagePassingScheduler s,
                             int mi, long tm, double ct) {
		long start = System.nanoTime();

        this.tables = tables;
        this.maxIterations = mi;
        this.timeoutMillis = tm;
        this.convThreshold = ct;
        this.domain = tables[0].domain();
        this.varTables = initialVarTables();
        this.evidence = new IntMap();
        this.evidenceIndicators = new Table[domain.size()];
        this.isValid = false;

        this.scheduler = s;
        initializeMessages();

		long finish = System.nanoTime();
		compilationTime=(finish-start)*1e-6;
    }

    public BeliefPropagation(Table[] tables, int mi, long tm, double ct) {
        this(tables,new ParallelSchedule(tables),mi,tm,ct);
    }

    protected abstract Table computeMessage(Pair pair);
    protected abstract double computeResidual(Table oldt, Table newt);

	protected void makeValid() {
		if ( ! this.isValid ) {
			runBeliefPropagation();
			this.isValid = true;
		}
	}

    /**
     * This performs one BP iteration.
     */
    protected void iteration() {
        residual = 0;
        for ( Pair pair : scheduler.nextIteration() ) {
            if ( isUnitTablePair(pair) ) continue;
            Table msg = computeMessage(pair);
            Table oldmsg = messages.get(pair); //oldMessages.get(pair);
            double msgResidual = computeResidual(oldmsg,msg);
            if ( msgResidual > residual ) residual = msgResidual;
            messages.put(pair,msg);
            //if ( scheduler.isAsynchronous() ) oldMessages.put(pair,msg);
        }
        //oldMessages = new HashMap<Pair,Table>(messages);
        updateIterationStatus();
    }

    protected void runBeliefPropagation() {
        initializeIterations();
        while ( iterationStatusOk() ) iteration();
        if ( residual <= convThreshold ) converged = true;
    }

    public double oneMoreIteration(boolean restart) {
        if ( restart ) {
            isValid = true;
            initializeIterations();
        } else {
            iteration();
            if ( residual <= convThreshold ) converged = true;
        }
        return residual;
    }

    /** 
     * This method should only be used by the var/cloneConditional()
     * methods, as it uses the most recent messages.
     */
    protected Table[] collectTables(int node) {
        return collectTables(node,null,true);
    }

    /**
     * This method is for message passing.
     *
     * AC: useCurMsgs here may be unneccessary... 
     */
    protected Table[] collectTables(Pair pair) {
        Pair except = new Pair(pair.s2,pair.s1);
        return collectTables(pair.s1,except,false);
    }

    /** 
     * This method collects all tables at or incoming a node.  You can
     * use current messages (asynchronous/sequential message updates,
     * or for computing marginals), or old messages (for
     * syncronous/parallel message updates)
     */
    protected Table[] collectTables(int node,Pair except,boolean useCurMsgs){
        ArrayList<Table> ts = new ArrayList<Table>();

        // first, collect table at factor graph node
        Table nodeTable = null;
        if ( node < domain.size() )  // if variable
            nodeTable = multiplyVarTables(node);
        if ( node >= domain.size() ) // if table
            nodeTable = tables[node-domain.size()];
        if ( nodeTable != null ) ts.add(nodeTable);

        // second, collect incoming messages
        // HashMap<Pair,Table> msgs = useCurMsgs ? messages : oldMessages;
        // HashMap<Pair,Table> msgs = oldMessages; // AC
        HashMap<Pair,Table> msgs = messages;
        ArrayList<Pair> incoming = scheduler.messagesIncoming(node);
        for (Pair pair : incoming) {
            // if ( isUnitTablePair(pair) ) continue;
            if ( pair.equals(except) ) continue;
            Table msg = msgs.get(pair);
            if ( msg != null ) ts.add(msg);
        }
        return ts.toArray(new Table[0]);
    }

    // this makes a table filled with 1's
    protected Table initializeMessage(int var) {
        Table t = Table.indicatorTable(domain,var);
        t.normalizeInPlace();
        return t;
    }

    /**
     * for now, this initializes a message to all 1's
     */
    protected void initializeMessages() {
        Pair[] pairs = scheduler.fgPairs();
        messages = new HashMap<Pair,Table>(pairs.length);
        for (Pair pair : pairs) {
            if ( isUnitTablePair(pair) ) continue;
            int var = scheduler.varOfPair(pair);
            Table msg = initializeMessage(var);
            messages.put(pair,msg);
        }
        //oldMessages = new HashMap<Pair,Table>(messages);
    }

	protected long startTime;
	protected void initializeIterations() {
		this.converged = false;
        this.residual = Double.POSITIVE_INFINITY;
		this.numIterations = 0;
		this.iterationTime = 0;
		this.startTime = System.nanoTime();
	}

	/* returns false if last iteration converged or we exceed given
	 * limits, returns true otherwise */
	protected boolean iterationStatusOk() {
		if ( this.maxIterations != 0 &&
             this.numIterations >= this.maxIterations )
            return false;
		if ( this.timeoutMillis != 0 &&
             this.iterationTime >= this.timeoutMillis )
            return false;

		// we can't converge in the first iteration
		if ( this.numIterations == 1 )
            return true;

        return residual > convThreshold;
	}

	/* this updates iteration status; to be executed at bottom of
	 * iteration */
	protected void updateIterationStatus() {
		this.numIterations++;
		this.iterationTime = (System.nanoTime() - this.startTime)*1e-6;
	}

    //////////////////////////////////////////////////
    /// stuff for special handling of unit tables
    //////////////////////////////////////////////////

    protected boolean isUnitTablePair(Pair pair) {
        // the larger index is the table
        int node = scheduler.tableOfPair(pair);
        Table t = tables[node-domain.size()];
        return t.vars().size() == 1;
    }

    protected Table multiplyVarTables(int var) {
        ArrayList<Table> ts = new ArrayList<Table>();
        if ( evidenceIndicators[var] != null )
            ts.add(evidenceIndicators[var]);
        if ( varTables[var] != null ) {
            for (int i = 0; i < varTables[var].size(); i++) {
                int j = varTables[var].get(i);
                ts.add(tables[j]);
            }
        }
        if ( ts.isEmpty() ) return null;
        Table table = Table.varTable(domain,var);
        table.multiplyAndProjectInto(ts.toArray(new Table[0]));
        return table;
    }

    protected IntSet[] initialVarTables() {
        IntSet[] varTables = new IntSet[domain.size()];
        for (int i = 0; i < tables.length; i++) {
            Table t = tables[i];
            if ( t.vars().size() != 1 ) continue;
            int var = t.vars().get(0);
            if ( varTables[var] == null )
                varTables[var] = new IntSet();
            varTables[var].add(i);
        }
        return varTables;
    }

    //////////////////////////////////////////////////
    /// stuff for special handling of unit tables
    //////////////////////////////////////////////////

    public void setEvidence(IntMap e) {
		this.isValid = false;
        this.evidence = e;
        java.util.Arrays.fill(this.evidenceIndicators,null);
        for (int i = 0; i < e.size(); i++) {
            int var = e.keys().get(i);
            int val = e.get(var);
            evidenceIndicators[var] = Table.evidenceTable(domain,var,val);
        }
    }

    public void setTable(int i, Table t) {
        Table old = tables[i];
        if ( t.vars().size() != old.vars().size() ||
             ! t.vars().containsAll(old.vars()) )
            throw new IllegalArgumentException("Incompatible table");
        tables[i] = t;
        this.isValid = false;
    }

    public abstract double logPrEvidence();
    public abstract double prEvidence();

    public abstract Table tableConditional(int table);
    public abstract Table tableJoint(int table);
    public abstract Table varConditional(int var);
    public abstract Table varJoint(int var);

	public boolean converged() { return converged; }
    public double residual() { return residual; }
	public int iterations() { return numIterations; }
	public long timeMillis() { return (long)iterationTime; }

    public double getCompilationTime() { return compilationTime; }
    public double getPropagationTime() { return propagationTime; }
}
