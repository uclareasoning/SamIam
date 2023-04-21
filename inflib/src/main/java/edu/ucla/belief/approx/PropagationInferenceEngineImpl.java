package edu.ucla.belief.approx;

import edu.ucla.belief.*;
import edu.ucla.structure.DirectedEdge;
import edu.ucla.structure.DirectedGraph;

import java.util.*;

/** @author arthur choi
	@since  20050505 */
public class PropagationInferenceEngineImpl extends AbstractInferenceEngine
	implements PropagationInferenceEngine, EvidenceChangeListener
		//,PartialDerivativeEngine
{
	protected BeliefNetwork myBeliefNetwork; // need only be DirectedGraph
	private Map cpts; //<FiniteVariable,Table>
	private Map curMessages; //<DirectedEdge,Table>
	private Map prevMessages; //<DirectedEdge,Table>
	private MessagePassingScheduler scheduler;
	private List schedule; //<DirectedEdge>

	/* IBP settings */
	private int maxIterations;
	private long timeoutMillis;
	private double convergenceThreshold;
	private boolean parallelUpdates;

	/* IBP stats */
	private int numIterations;
	private long timeMillis;
	private boolean converged;

	private boolean isValid;

	public PropagationInferenceEngineImpl( BeliefNetwork bn,
										   BeliefPropagationSettings settings,
										   Dynamator dyn ){
		super( dyn );

		this.mySettings = settings;
		mySettings.addChangeListener( this );
		this.myBeliefNetwork = bn;
		myBeliefNetwork.getEvidenceController().
			addPriorityEvidenceChangeListener( this );

		this.maxIterations = settings.getMaxIterations();
		this.timeoutMillis = settings.getTimeoutMillis();
		this.convergenceThreshold = settings.getConvergenceThreshold();
		this.parallelUpdates = true;

		this.scheduler = settings.getScheduler();
		this.schedule = this.scheduler.generateSchedule(bn);

		makeInvalid();
	}

	/** @since 20091226 */
	public String compilationStatus( edu.ucla.belief.io.PropertySuperintendent bn ){
		return convergenceSummary( true );
	}

	/** @since 20060321 */
	public void die(){
		super.die();

		if( cpts                         != null ) cpts.clear();
		if( curMessages                  != null ) curMessages.clear();
		if( prevMessages                 != null ) prevMessages.clear();
		if( myMapVariablesToConditionals != null ) myMapVariablesToConditionals.clear();
		if( schedule                     != null ) schedule.clear();

		PropagationInferenceEngineImpl.this.cpts                         = null;
		PropagationInferenceEngineImpl.this.curMessages                  = null;
		PropagationInferenceEngineImpl.this.prevMessages                 = null;
		PropagationInferenceEngineImpl.this.myMapVariablesToConditionals = null;
		PropagationInferenceEngineImpl.this.schedule                     = null;
		PropagationInferenceEngineImpl.this.myBeliefNetwork              = null;
		//PropagationInferenceEngineImpl.this.mySettings                 = null;
		PropagationInferenceEngineImpl.this.scheduler                    = null;
	}

	public void setTimeoutMillis( long millis ){
		this.timeoutMillis = millis;
	}

	public void setMaxIterations( int max ){
		this.maxIterations = max;
	}

	public void setConvergenceThreshold( double thresh ){
		this.convergenceThreshold = thresh;
	}

	//if overriding public void actionPerformed( ActionEvent event ){ call super.actionPerformed( event ); }

	private void makeValid() {
		if ( ! this.isValid ) {
			clearCache();
			setupBeliefPropagation();
			runBeliefPropagation();
			this.isValid = true;
		}
	}

	private void makeInvalid() {
		this.isValid = false;
	}

	public void setCPT( FiniteVariable var ){
		makeInvalid();
	}

	public double probability(){
		//makeValid();
		return Math.exp(-betheFreeEnergy());
		//throw new UnsupportedOperationException();
	}

	/** @since 20080221 */
	public char probabilityDisplayOperatorUnicode(){ return '\u2248'; }

	/**
	 * This method computes the Bethe free energy approximation to the
	 * probability of evidence.
	 */
	public double betheFreeEnergy() {
		makeValid();

		double entropy = 0.0;
		double energy = 0.0;
		Table table, cpt;

		List vars = myBeliefNetwork.topologicalOrder();
		FiniteVariable var;
		for ( int i = 0; i < vars.size(); i++ ) {
			var = (FiniteVariable)vars.get(i);
			table = conditional(var);
			entropy -= myBeliefNetwork.outDegree(var)*entTable(table);
			table = familyConditional(var);
			entropy += entTable(table);
			cpt = var.getCPTShell().getCPT();
			energy += energyTable(cpt,table);
		}

		return (energy-entropy);
	}

	/*
	  Energy(XU) = - sum_{xu} pr(xu|e) log theta_x|u
	*/
	private static double energyTable(Table cpt, Table xTable) {
		FiniteVariable[] vars = varsArray(cpt.variables());
		Table table = xTable.permute(vars);

		double energy = 0.0;
		for (int i = 0; i < cpt.getCPLength(); i++) {
			double cp = cpt.getCP(i);
			double av = table.getCP(i);
			if ( cp <= 0.0 && av <= 0.0 ) continue;
			else energy -= av * Math.log(cp);
		}
		return energy;
	}

	/*
	  ENT(X|e) = - sum_{x} pr(x|e) log pr(x|e)
	*/
	private static double entTable(Table xTable) {
		double ent = 0.0;
		for (int i = 0; i < xTable.getCPLength(); i++) {
			double cp = xTable.getCP(i);
			if ( cp <= 0.0 ) continue;
			else ent -= cp * Math.log(cp);
		}
		return ent;
	}

	/* do I really need this? for energyTable() */
	private static FiniteVariable[] varsArray( List vars ) {
		FiniteVariable[] varsarray = new FiniteVariable[vars.size()];
		for (int i = 0; i < vars.size(); i++ )
			varsarray[i] = (FiniteVariable)vars.get(i);
		return varsarray;
	}

	/** @since 20050830
		@see edu.ucla.belief.Dynamator#probabilitySupported()
		@see PropagationEngineGenerator#probabilitySupported()
		@see edu.ucla.belief.InferenceEngine#probabilitySupported()

		If you change the return value, you must consider
		changing the return value of PropagationEngineGenerator.probabilitySupported()
	*/
	public boolean probabilitySupported(){
		return true;
	}

	public Table joint( FiniteVariable var ){
		makeValid();
		throw new UnsupportedOperationException();
	}

	public Table conditional( FiniteVariable var ){
		makeValid();
		if( myMapVariablesToConditionals.containsKey( var ) ) {
			++myCacheHits;
			return (Table) myMapVariablesToConditionals.get( var );
		}
		else {
			Table result = familyConditional(var);
			result = result.project(Collections.singleton(var));
			// normalize to make sure in [0,1] (numerical issue?)
			Table.normalize(result);
			myMapVariablesToConditionals.put( var, result );
			return result;
		}
	}

	/* this is copied from JoinTreeInferenceEngeineImpl */
	protected void clearCache() { myMapVariablesToConditionals.clear(); }
	public int getCacheHits() { return myCacheHits; }
	protected int myCacheHits = (int)0;
	protected Map myMapVariablesToConditionals = new HashMap();

	public Table familyJoint( FiniteVariable var ){
		makeValid();
		throw new UnsupportedOperationException();
	}

	/** This computes an approximation to familyConditional.
		Mainly this is used to compute conditional.
	*/
	public Table familyConditional( FiniteVariable var ){
		makeValid();
		Set neighbors = getNeighborsExcept(var,null);
		Set msgs = new HashSet(neighbors.size()+1);
		msgs.add(cpts.get(var));
        for( Iterator it = neighbors.iterator(); it.hasNext();)
            msgs.add((Table)curMessages.get(getEdge(it.next(),var)));
        Table result = Table.multiplyAll(msgs);
		return Table.normalize(result);
	}

	/** interface PartialDerivativeEngine */
	public Table partial( FiniteVariable var ){
		makeValid();
		throw new UnsupportedOperationException();
	}

	/** interface PartialDerivativeEngine */
	public Table familyPartial( FiniteVariable var ){
		makeValid();
		throw new UnsupportedOperationException();
	}

	/** interface EvidenceChangeListener */
	public void warning( EvidenceChangeEvent ece ) {}

	/** interface EvidenceChangeListener */
	public void evidenceChanged( EvidenceChangeEvent ece ){
		makeInvalid();
	}

	public Set variables(){
		return myBeliefNetwork.vertices();
	}

	public InferenceEngine handledClone( QuantitativeDependencyHandler handler ){
		throw new UnsupportedOperationException();
	}

	public void printTables( java.io.PrintWriter out ){
		throw new UnsupportedOperationException();
	}

	public boolean isExhaustive(){
		return false;
	}

	private BeliefPropagationSettings mySettings;

	private DirectedEdge getEdge(Object from, Object to) {
		return new DirectedEdge(from,to);
	}

	/* sets up belief propagation
	   = determine message passing order ???
	   = collects CPTs, zeroing out entries inconsistent with evidence
	   = initialize messages

	   todo : more sophisticated message scheduling
	*/
	private void setupBeliefPropagation() {
		// generateMessagePassingSchedule();
		initializeIbpCpts();
		initializeAllMessageTables();
	}

	private void generateMessagePassingSchedule() {
		// AC
		this.schedule = this.scheduler.generateSchedule(myBeliefNetwork);
	}

	/* applies evidence indicators to CPTs */
	private void initializeIbpCpts() {
		cpts = new HashMap(myBeliefNetwork.size());
		Map evidence = myBeliefNetwork.getEvidenceController().evidence();
		for ( Iterator it = myBeliefNetwork.topologicalOrder().iterator();
			  it.hasNext(); ) {
			FiniteVariable parent = (FiniteVariable)it.next();
			cpts.put(parent,getVariableCpt(parent,evidence));
		}
	}

	private void initializeAllMessageTables() {
		curMessages = new HashMap(2*myBeliefNetwork.numEdges());
		prevMessages = new HashMap(2*myBeliefNetwork.numEdges());

		for ( Iterator it = myBeliefNetwork.topologicalOrder().iterator();
			  it.hasNext(); ) {
			FiniteVariable parent = (FiniteVariable)it.next();
			for (Iterator edgeIt = myBeliefNetwork.outGoing(parent).iterator();
				 edgeIt.hasNext(); ) {
				FiniteVariable child = (FiniteVariable)edgeIt.next();
				DirectedEdge edge1 = getEdge(parent,child);
				DirectedEdge edge2 = getEdge(child,parent);
				/* each table should be distinct object? */
				curMessages.put(edge1,initializedMessageTable(parent));
				curMessages.put(edge2,initializedMessageTable(parent));
				prevMessages.put(edge1,initializedMessageTable(parent));
				prevMessages.put(edge2,initializedMessageTable(parent));
			}
		}
	}

	private long elapsedTime(long startTime) {
		return (new Date()).getTime() - startTime;
	}

	/*
	  runs belief propagation until we are at, or passed, the stopping
	  condition
	 */
	private void runBeliefPropagation() {
		this.converged = false;
		this.timeMillis = 0;
        this.numIterations = 0;
		long startTime = elapsedTime(0);
		while ( true ) {
			if ( this.maxIterations != 0 )
				if ( this.numIterations >= this.maxIterations ) return;
			if ( this.timeoutMillis != 0 )
				if ( this.timeMillis >= this.timeoutMillis ) return;

			nextIteration();
			this.numIterations++;
			this.timeMillis = elapsedTime(startTime);

			if ( hasConverged() ) { this.converged = true; return; }
            prevMessages.clear();
			prevMessages.putAll(curMessages);
        }
	}

	/* computes message based on message schedule */
    private void nextIteration() {
        for( Iterator it = schedule.iterator(); it.hasNext(); ) {
			DirectedEdge edge = (DirectedEdge)it.next();
			FiniteVariable from = (FiniteVariable)edge.v1();
			FiniteVariable to = (FiniteVariable)edge.v2();
			sendMessage(from,to);
		}
    }

	/* send message from from to to */
    private void sendMessage(FiniteVariable from, FiniteVariable to) {
		DirectedEdge edge = getEdge(from,to);
		Set neighbors = getNeighborsExcept(from,to);
		Table newTable = (Table)((Table)cpts.get(from)).clone();
        for( Iterator it = neighbors.iterator(); it.hasNext();)
			if ( this.scheduler.isParallel() )
				newTable.multiplyInto
					((Table)prevMessages.get(getEdge(it.next(),from)));
			else
				newTable.multiplyInto
					((Table)curMessages.get(getEdge(it.next(),from)));

		Table oldTable = (Table)curMessages.get(edge);
		HashSet varSet = new HashSet(oldTable.variables());
		newTable = Table.normalize(newTable.project(varSet));
		curMessages.remove(edge);
		curMessages.put(edge,newTable);
    }

	private Set getNeighborsExcept(FiniteVariable var, FiniteVariable var2) {
        Set neighbors = new HashSet();
		neighbors.addAll(myBeliefNetwork.inComing(var));
		neighbors.addAll(myBeliefNetwork.outGoing(var));
        neighbors.remove(var2);
		return neighbors;
	}

	/* fetches CPT and zeros out entries inconsistent with evidence */
	private Table getVariableCpt(FiniteVariable var, Map evidence) {
		Table t = var.getCPTShell().getCPT();
		Object instance = evidence.get(var);
		if ( instance == null ) return t; // consistent with evidence

		// construct table to zero out rows inconsistent with evidence
		int instanceIndex = var.index(instance);
		double[] vals = new double[var.size()];
		vals[instanceIndex] = 1;
		Table e = new Table(new Variable[]{var},vals);
		return Table.multiply(t,e);
	}

	/* creates uniform message for var */
	private Table initializedMessageTable(FiniteVariable var) {
		Table t = new Table(Collections.singletonList(var));
		t.makeUniform();
		return t;
	}

	/* */
    private boolean hasConverged() {
		// there is no valid set of previous messages at iteration 1
		if ( this.numIterations <= 1 ) return false;

		for ( Iterator it = prevMessages.keySet().iterator(); it.hasNext(); ) {
			Object msg = it.next();
			Table pTable = (Table)prevMessages.get(msg);
			Table cTable = (Table)curMessages.get(msg);
            if( ! hasConverged( pTable.dataclone(), cTable.dataclone(),
								this.convergenceThreshold ) )
                return false;
		}
		return true;
    }

	/* currently using absolute difference to determine convergence */
	private boolean hasConverged(double[] v1, double[] v2, double thresh) {
        for ( int i = 0; i < v1.length; i++ )
            if ( Math.abs(v1[i]-v2[i]) > thresh )
                return false;
        return true;
    }

	/** Returns summary of convergence and statistics.
		For SamIam GUI.
	 */
	public String convergenceSummary( boolean identify ){
        makeValid();

		String summary = identify ? " belief propagation " : "";
		if ( converged )
			summary += "converged in " + numIterations + " iterations, " +
				timeMillis + " milliseconds";
		else if (  maxIterations != 0 && numIterations >= maxIterations )
			summary += "<font color=\"#cc0000\">did not converge</font> in " + numIterations +
				" iterations (" + timeMillis + " milliseconds)";
		else if ( timeoutMillis != 0 && timeMillis >= timeoutMillis )
			summary += "<font color=\"#cc0000\">timed out; did not converge</font> in " + timeMillis +
				" milliseconds (" + numIterations + " iterations)";
		else // should not reach here
			summary += "did not converge? in " + numIterations +
				" iterations," + timeMillis + " milliseconds";
		return summary;
	}

	public boolean converged() { return converged; }
	public int iterations() { return numIterations; }
	public long timeMillis() { return timeMillis; }
}
