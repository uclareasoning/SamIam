package       edu.ucla.belief.approx;

import        edu.ucla.belief.approx. RecoverySetting;
import static edu.ucla.belief.approx. RecoverySetting.*;
import        edu.ucla.belief. CrouchingTiger .DynamatorImpl;

import        edu.ucla.belief.*;
import        edu.ucla.belief.inference.*;
import        edu.ucla.belief.io. PropertySuperintendent;
import static edu.ucla.util. PropertyKey. defaultValue;
import        edu.ucla.structure. DirectedEdge;

import        il2.inf. Algorithm;
import        il2.inf.edgedeletion. EDRecovery;
import        il2.inf.edgedeletion. EDAlgorithm. RankingHeuristic;
import        il2.bridge. Converter;
import        il2.util. IntMap;

import        java.util.*;

/** @author keith cascio
	@since  20091207 */
public class RecoveryInferenceEngine extends AbstractInferenceEngine implements InferenceEngine, PropagationInferenceEngine
{
	public   RecoveryInferenceEngine(
	  BeliefNetwork                           bn,
	  Converter                               brudge,
	  Settings<RecoverySetting>               settings,
	  Map<DynamatorImpl,Dynamator>            team,
	  Dynamator                               dyn ){
		super(                                dyn );
		this.beliefnetwork                  = bn;
		this.bridge                         = brudge;

		DynamatorImpl                dynamatorimpl = (DynamatorImpl)     settings.get( subalgorithm );
		if( dynamatorimpl == null ){ dynamatorimpl = (DynamatorImpl) subalgorithm.get( defaultValue ); }
		Dynamator                    dynamator     = team == null ? null : team.get( dynamatorimpl );
		if( dynamator     == null ){ dynamator     = dynamatorimpl.create(); }
		Algorithm                        algorithm = dynamatorimpl.algorithm();
		Map<Algorithm.Setting,Object>  subsettings = Macros.subsettings( dynamator, (PropertySuperintendent) bn );

	  //int    dseed = ((Number)     seed.get( defaultValue )).   intValue();
	  //int    iseed = ((Number) settings.get( seed         )).   intValue();
	  //Random rseed = iseed == dseed ? new Random() : new Random( iseed );

		engine = new EDRecovery(         bridge.convert( bn ),
		                                 bridge.convert( bn.getEvidenceController().evidence() ),
		         (RankingHeuristic) settings.get( heuristic ),
		  ((Number) settings.get( recovery   )).   intValue(),
	      ((Number) settings.get( iterations )).   intValue(),
	      ((Number) settings.get( timeout    )).  longValue(),
	      ((Number) settings.get( threshold  )).doubleValue(),
	                                                algorithm,
	                                              subsettings,
	                                             new Random(
	      ((Number) settings.get( seed       )).   intValue() ) );

	   (this.settings = settings)                 .addChangeListener( this );
		bn.getEvidenceController().addPriorityEvidenceChangeListener( this );

		this.flag_compare2exact = Boolean.TRUE.equals( settings.get( compare2exact ) );
	}

	/** @since 20091226 */
	public String compilationStatus( edu.ucla.belief.io.PropertySuperintendent bn ){
		return convergenceSummary( true );
	}

	/** interface PropagationInferenceEngine */
	public String convergenceSummary( boolean identify ){
		return (identify ? " ed-bp " : "") + (engine == null ? "????" : engine.convergenceSummary());
	}

	public RecoveryInferenceEngine setThreadGroup( ThreadGroup threadgroup ){
		this.threadgroup = threadgroup;
		return this;
	}

	private int notifyEvidenceChangeListeners(){
		return beliefnetwork.getEvidenceController().notifyNonPriorityListeners();
	}

	public Table[] conditionals( FiniteVariable var, Table[] buckets ){
		if( flag_compare2exact ){
			if( (buckets == null) || (buckets.length < 2) ){ buckets = new Table[2]; }
			int id = bridge.convert( var );
			buckets[0] = bridge.convert( engine.  varConditional( id ) );
			buckets[1] = this.exactConditional( var );
			return buckets;
		}
		else{ return super.conditionals( var, buckets ); }
	}

	public edu.ucla.belief.Table        exactConditional( FiniteVariable var ){
		if( ssengine == null ){
			ssengine = (SSEngine) ((ApproxEngineGenerator) getDynamator().getCanonicalDynamator()).getExactEngineGenerator().manufactureInferenceEngine( beliefnetwork );
			ssengine.evidenceChanged( new EvidenceChangeEvent( this.beliefnetwork.getEvidenceController().evidenceVariables() ) );
		}
		return ssengine.conditional( var );
	}

	public String[] describeConditionals(){
		return flag_compare2exact ? DESCRIBE_CONDITIONALS : super.describeConditionals();
	}

	public static final String[] DESCRIBE_CONDITIONALS = new String[]{ "approximate probability", "exact probability" };

	/** interface EvidenceChangeListener */
	@SuppressWarnings( "unchecked" )
	public void evidenceChanged( EvidenceChangeEvent ece ){
		dirty();
		synchronized( synch ){
			Map<FiniteVariable,Object> il1evidence = (Map<FiniteVariable,Object>) this.beliefnetwork.getEvidenceController().evidence();
			if(   this.evidence == null ){ this.evidence = new IntMap( il1evidence.size() ); }
			else{ this.evidence.clear(); }
			for( FiniteVariable fv : il1evidence.keySet() ){
				this.evidence.put( bridge.convert( fv ), fv.index( il1evidence.get( fv ) ) );
			}
			engine.setEvidence( evidence );
		}
		if( ssengine != null ){ ssengine.evidenceChanged( ece ); }
	}
	/** interface EvidenceChangeListener */
	public void         warning( EvidenceChangeEvent ece ){
		int size = ece == null ? 0 : (ece.recentEvidenceChangeVariables == null ? 0 : ece.recentEvidenceChangeVariables.size());
		event = "evidence change x" + Integer.toString( size );
	}

	/*     interface InferenceEngine     */
	public Table conditional( FiniteVariable var ){
		return bridge.convert( engine.varConditional( bridge.convert( var ) ) );
	}
	/*     interface InferenceEngine     */
	public Table joint( FiniteVariable var ){
		throw new UnsupportedOperationException();
	}
	/*     interface InferenceEngine     */
	public double probability(){
		return engine.prEvidence();
	}
	/*     interface InferenceEngine     */
	public char probabilityDisplayOperatorUnicode(){ return '\u2248'; }
	/*     interface InferenceEngine     */
	public void setCPT( FiniteVariable var ){
		dirty();
		engine.setTable( bridge.convert( var ), bridge.convert( var.getCPTShell().getCPT() ) );
	}
	/*     interface InferenceEngine     */
	public Table familyJoint( FiniteVariable var ){
		throw new UnsupportedOperationException();
	}
	/*     interface InferenceEngine     */
	public Table familyConditional( FiniteVariable var ){
		throw new UnsupportedOperationException();
	}
	/*     interface InferenceEngine     */
	@SuppressWarnings( "unchecked" )
	public Set<FiniteVariable> variables(){
		return Collections.unmodifiableSet( new HashSet<FiniteVariable>( beliefnetwork ) );
	}
	/*     interface InferenceEngine     */
	public boolean isExhaustive(){
		return true;
	}
	/*     interface InferenceEngine     */
	public InferenceEngine handledClone( QuantitativeDependencyHandler handler ){
		throw new UnsupportedOperationException();
	}
	/*     interface InferenceEngine     */
	public void printTables( java.io.PrintWriter out ){
		throw new UnsupportedOperationException();
	}

	public BeliefNetwork getBeliefNetwork(){
		return beliefnetwork;
	}

	public Converter bridge(){
		return bridge;
	}

	public RecoveryInferenceEngine dirty(){
		return this;
	}

	public Settings<RecoverySetting> settings(){
		return this.settings;
	}

	/** @since 20091218 */
	public Collection<DirectedEdge> notoriousEdges(){
		return edgesDeleted();
	}

	/** @since 20091218 */
	public Collection<DirectedEdge> edgesDeleted(){
		if( deletededges == null ){
			deletededges = Arrays.asList( bridge.convertDirected( engine.edgesDeleted() ) );
		}
		return Collections.unmodifiableCollection( deletededges );
	}

	public void die(){
		super.die();

		try{
			EvidenceController ec  = beliefnetwork == null ? null : beliefnetwork.getEvidenceController();
			if(                ec != null ){ ec.removePriorityEvidenceChangeListener( this ); }
			this        .bridge    = null;
		  //if(      deletededges != null ){ deletededges.clear(); }
			deletededges           = null;
			this .beliefnetwork    = null;
			if(        settings   != null ){ settings.removeChangeListener( this ); }
			this      .settings    = null;
			this   .threadgroup    = null;
			this      .evidence    = null;
		  //this.ssenginegenerator = null;
			if(        ssengine   != null ){
				if(            ec != null ){ ec.removePriorityEvidenceChangeListener( ssengine );
				                             ec.removeEvidenceChangeListener(         ssengine ); }
				ssengine.die();
			}
			ssengine               = null;
		}catch( Exception thrown ){
			System.err.println( "warning: RecoveryInferenceEngine.die() caught " + thrown );
		  //thrown.printStackTrace( System.err );
		}
	}

	private    EDRecovery                            engine;
	private    Converter                             bridge;
	private    Collection<DirectedEdge>              deletededges;
	private    BeliefNetwork                         beliefnetwork;
	private    Settings<RecoverySetting>             settings;
	private    boolean                               flag_compare2exact;
	private    double                                residual, maxerror;
	private    ThreadGroup                           threadgroup;
	private    Object                                synch = new Object(), event;
	transient private     IntMap                     evidence;
	transient private     SSEngine                   ssengine;
}
