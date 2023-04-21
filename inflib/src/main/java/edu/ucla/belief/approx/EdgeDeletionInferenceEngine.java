package edu.ucla.belief.approx;

import        edu.ucla.util.Setting;
import        edu.ucla.util.Setting.Settings;
import        edu.ucla.util.SettingsImpl;
import        edu.ucla.belief.approx. Macros.Recoverable;
import        edu.ucla.belief.approx. Macros.Bridge;
import        edu.ucla.belief.approx. Macros.Category;
import        edu.ucla.belief.approx. EdgeDeletionBeliefPropagationSetting;
import static edu.ucla.belief.approx. EdgeDeletionBeliefPropagationSetting.*;
import static edu.ucla.belief.approx. EdgeDeletionInferenceEngine.CPTPolicy.*;
import        edu.ucla.belief.approx. EdgeDeletionInferenceEngine.CPTPolicy.Listener;

import        edu.ucla.belief.*;

import        java.util.*;
import static java.util.Collections.unmodifiableCollection;
import        java.text.NumberFormat;
//{superfluous} import        java.text.DateFormat;
import static java.lang.Thread.yield;
import static java.lang.Thread.sleep;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import        java.lang.Thread.UncaughtExceptionHandler;
import static java.awt.event.KeyEvent.*;
import static java.awt.event.InputEvent.*;
import        javax.swing.KeyStroke;
import static javax.swing.KeyStroke.getKeyStroke;

/** @author keith cascio
	@since  20080225 */
public class EdgeDeletionInferenceEngine extends AbstractInferenceEngine implements InferenceEngine
{
	public   EdgeDeletionInferenceEngine(
	  BeliefNetwork                           bn,
	  Bridge                                  brudge,
	  Settings<EdgeDeletionBeliefPropagationSetting>   settings,
	  Dynamator                               dyn ){
		super(                                dyn );
		this.beliefnetwork                  = bn;
		this.bridge                         = brudge;

	   (this.settings = settings)                 .addChangeListener( this );
		bn.getEvidenceController().addPriorityEvidenceChangeListener( this );

		this.flag_log = setupLogging();
		this.flag_compare2exact = Boolean.TRUE.equals( settings.get( compare2exact ) );
		if(   this.flag_control = Boolean.TRUE.equals( settings.get( control       ) ) ){
			this.threadgroup    = new ThreadGroup( "edbp control commands" );
			boolean synchronous = true;
			this.command( Command.rewind, synchronous );
		}

		this.policy = (CPTPolicy) settings.get( cptpolicy );
	}

	/** @since 20080228 */
	@SuppressWarnings( "unchecked" )
	private boolean setupLogging(){
		Collection<Attribute> tolog = (Collection<Attribute>) settings.get( log );
		if( (tolog == null) || tolog.isEmpty() ){ return false; }
		this.loggables = EnumSet.copyOf( tolog );
		this.mylog     = new LinkedList<Map<Attribute,Object>>();
		return true;
	}

	/** @since 20080228 */
	private EdgeDeletionInferenceEngine log(){
		if( ! flag_log ){ return this; }
		try{
			Map<Attribute,Object> record = new EnumMap<Attribute,Object>( Attribute.class );
			for( Attribute attr : loggables ){
				record.put( attr, attr.get( this ) );
			}
			mylog.add( record );
		}catch( Throwable thrown ){
			System.err.println( "warning: EdgeDeletionInferenceEngine.log() caught " + thrown );
			if( Definitions.DEBUG ){ thrown.printStackTrace(); }
		}
		return this;
	}

	/** @since 20080228 */
	public Appendable appendLog( Appendable app, char delimiter ) throws java.io.IOException{
		if( ! flag_log ){ return app; }
		Attribute[] loggables = this .loggables.toArray( new Attribute[ this .loggables.size() ] );
		Map[]       records   = this .mylog    .toArray( new       Map[ this     .mylog.size() ] );
		Object      last      = records[ records.length - 1 ];
		for( Attribute attr : loggables ){
			app.append( attr.display ).append( delimiter );
			for( Map<?,?> record : records ){
				app.append( record.get( attr ).toString() );
				if( record != last ){ app.append( delimiter ); }
			}
			app.append( "\n" );
		}
		return app;
	}

	/** @since 20080229 */
	private Collection<FiniteVariable> auxiliaries(){
		if( auxiliaries == null ){
			auxiliaries = unmodifiableCollection( Category.auxiliary.membership( this.bridge, new HashSet<FiniteVariable>() ) );
		}
		return auxiliaries;
	}

	/** @since 20080229 */
	private CPTPolicy doPolicy( CPTPolicy policy ){
		if( listener    ==                    null ){ return this.policy; }
		switch( this.policy ){
			case discard:
			  {                                       return this.policy; }
			case import_on_convergence:
			  if( ! bridge.edalgorithm.converged() ){ return this.policy; }
			  break;
			case manual:
			  if( policy  != manual ){                return this.policy; }
			case import_on_iteration:
			  break;
			default:
			  throw new IllegalArgumentException();
		}
		int parameters = 0;
		parameters += importTables( bridge.softevidences2edgeindices.keySet(), Category.softevidence );
		parameters += importTables( bridge.       clones2edgeindices.keySet(), Category.clone        );
		listener.cptsChanged( auxiliaries(), parameters );
		return this.policy;
	}

	/** @since 20080229 */
	private int importTables( Collection<FiniteVariable> variables, Category kat ){
		int   length, parameters = 0;
		Table table, imported;
		for( FiniteVariable aux : variables ){
			table    = aux.getCPTShell().getCPT();
			length   = table.getCPLength();
			imported = kat.table( aux, bridge );
			if( imported.getCPLength() != length ){ throw new IllegalStateException( "failed to import " + kat.name() + " '"+aux.getID()+"' |table| = " + length + " != |imported| = " + imported.getCPLength() ); }
			for( int i=0; i<length; i++ ){
				table.setCP( i, imported.getCP( i ) );
				++parameters;
			}
		}
		return parameters;
	}

	/** @since 20080229 */
	public enum CPTPolicy{
		discard               ( "discard",                "<html>never " + prefix() ),
		import_on_convergence ( "import on convergence",  prefix() + " when the algorithm converges" ),
		import_on_iteration   ( "import every iteration", prefix() + " after every iteration" ),
		manual                ( "manual control",         prefix() + " when commanded" );

		public interface Listener{
			public Object cptsChanged( Collection<FiniteVariable> variables, int parameters );
		}

		private CPTPolicy( String display, String tip ){
			this.display = display;
			this.tip     = tip;
		}

		public String toString(){ return display; }

		public static final String prefix(){ return PREFIX; }

		public static final String PREFIX = "<html>transfer auxiliary variable cpt parameters from EDBP";

		final public String display, tip;
	}

	/** @since 20080227 */
	public enum Attribute{
		iterations      ( Integer .class,              "iterations", "number of iterations since the last rewind" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.bridge.edalgorithm.iterations();
			}
		},
		residual        (  Double .class,                "residual", "residual" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.residual;
			}
		},
	  /*timemillis      (    Long .class,            "elapsed time", "elapsed time in milliseconds" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.bridge.edalgorithm.timeMillis();
			}
		},*/
		converged       ( Boolean .class,              "converged?", "has EDBP converged?" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.bridge.edalgorithm.converged();
			}
		},
		threshold       (  Double .class,   "convergence threshold",                     "convergence threshold", EdgeDeletionBeliefPropagationSetting.threshold  ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.bridge.edalgorithm.getConvThreshold();
			}
		},
		exceeded        ( Boolean .class,        "limits exceeded?", "has EDBP timed out, or exceeded maximum iterations?" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.bridge.edalgorithm.limitsExceeded();
			}
		},
		maxIterations   ( Integer .class,      "maximum iterations", "limit on the maximum number of iterations", EdgeDeletionBeliefPropagationSetting.iterations ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.bridge.edalgorithm.getMaxIterations();
			}
		},
		timeout         (    Long .class,                "time out",     "limit on elapsed time in milliseconds", EdgeDeletionBeliefPropagationSetting.timeout    ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.bridge.edalgorithm.getTimeoutMillis();
			}
		},
		compilationTime (  Double .class,        "compilation time", "In ED, this is time to initalize the algorithm: e.g. construct the approximate network (but not parametrize it)" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.bridge.edalgorithm.getCompilationTime();
			}
		},
		propagationTime (  Double .class,        "propagation time", "In ED, this is the time to parametrize edges" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.bridge.edalgorithm.getPropagationTime();
			}
		},
	  /*edgerankingTime (  Double .class,       "edge ranking time", "In ED, this is the time to rank edges, by rankEdgesByMi(2)" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.bridge.edalgorithm.getEdgeRankingTime();
			}
		},*/
		edgesDeleted    ( Integer .class,         "# deleted edges", "number of deleted edges in the network" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				int[][] edges = ie.bridge.edalgorithm.edgesDeleted();
				return edges == null ? 0 : edges.length;
			}
		},
		poe             (  Double .class,                   "pr(e)", "probability of evidence" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.probability();
			}
		},
		maxerror        (  Double .class,               "max error", "largest error" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.maxError();
			}
		},
		event           (  Object .class,                   "event", "the last significant event, e.g. rewind/step/play" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return ie.event;
			}
		},
		timestamp       (  Object .class,              "time stamp", "a textual time stamp" ){
			public Object get( EdgeDeletionInferenceEngine ie ){
				return dff.timestamp();
			}
			private DateFormatFilename dff = DateFormatFilename.getInstance();
		};

		public Object get( EdgeDeletionInferenceEngine ie ){ return null; }

		private Attribute( Class clazz, String display, String description ){
			this( clazz, display, description, null );
		}

		private Attribute( Class clazz, String display, String description, Setting setting ){
			this.clazz        = clazz;
			this.display      = display;
			this.description  = description;
			this.setting      = setting;
		}

		public String toString(){ return display; }

		final public Class   clazz;
		final public String  display, description;
		final public Setting setting;
	}

	public static class DateFormatFilename{
		private static  DateFormatFilename INSTANCE;
		private         DateFormatFilename(){}
		public  static  DateFormatFilename getInstance(){
			if( INSTANCE == null ){ INSTANCE = new DateFormatFilename(); }
			return INSTANCE;
		}

		public static String now(){
			return getInstance().format( new Date( System.currentTimeMillis() ) );
		}

		public String format( Date date ){
			try{
				buff.setLength(0);
				return format( date, buff ).toString();
			}catch( Throwable thrown ){
				System.err.println( "warning: EdgeDeletionInferenceEngine.DateFormatFilename.format() caught " + thrown );
			}
			return "";
		}

		public Appendable format( Date date, Appendable app ) throws java.io.IOException{
			Calendar calendar = Calendar.getInstance();
			calendar.setTime( date );

			int
			  year   = calendar.get( Calendar.YEAR         ),
			  month  = calendar.get( Calendar.MONTH        ),
			  day    = calendar.get( Calendar.DAY_OF_MONTH ),
			  hour24 = calendar.get( Calendar.HOUR_OF_DAY  ),
			  minute = calendar.get( Calendar.MINUTE       ),
			  second = calendar.get( Calendar.SECOND       ),
			  millis = calendar.get( Calendar.MILLISECOND  );

			return app
			.append( numberformat.format( year    ) )
			.append( numberformat.format( month+1 ) )
			.append( numberformat.format( day     ) )
			.append( '_' )
			.append( numberformat.format( hour24  ) )
			.append( numberformat.format( minute  ) )
			.append( numberformat.format( second  ) )
			.append( numberformat.format( millis  ) );
		}

		public static String timestamp(){
			return getInstance().timestamp( new Date( System.currentTimeMillis() ) );
		}

		public String timestamp( Date date ){
			try{
				buff.setLength(0);
				return timestamp( date, buff ).toString();
			}catch( Throwable thrown ){
				System.err.println( "warning: EdgeDeletionInferenceEngine.DateFormatFilename.timestamp() caught " + thrown );
			}
			return "";
		}

		public Appendable timestamp( Date date, Appendable app ) throws java.io.IOException{
			Calendar calendar = Calendar.getInstance();
			calendar.setTime( date );

			int hour24 = calendar.get( Calendar.HOUR_OF_DAY  ),
			    minute = calendar.get( Calendar.MINUTE       ),
			    second = calendar.get( Calendar.SECOND       ),
			    millis = calendar.get( Calendar.MILLISECOND  );

			return app
			.append( numberformat.format( hour24 ) )
			.append( ':' )
			.append( numberformat.format( minute ) )
			.append( ':' )
			.append( numberformat.format( second ) )
			.append( ':' )
			.append( numberformat.format( millis ) );
		}

		private StringBuilder buff         = new           StringBuilder(   0x20 );
		private NumberFormat  numberformat = new java.text.DecimalFormat( "##00" );
	}

	/** @since 20080228 */
	@SuppressWarnings( "unchecked" )
	public double maxError(){
		if( ! flag_compare2exact ){ return 0.0; }

		if( ! maxerrorclean ){
			double  max    = 0.0, error;
			Table[] tables = new Table[2];
			Table   t1, t2;
			int     length;
			for( FiniteVariable fvar : (Collection<FiniteVariable>) beliefnetwork ){
				tables = conditionals( fvar, tables );
				t1     = tables[0]; t2 = tables[1];
				if( (t2 == null) || (t1 == null) ){ continue; }
				length = fvar.size();
				for( int i=0; i<length; i++ ){
					error = Math.abs( t1.getCP(i) - t2.getCP(i) );
					if( error > max ){ max = error; }
				}
			}
			maxerrorclean = true;
			return maxerror = max;
		}
		return maxerror;
	}

	/** @since 20080227 */
	public EdgeDeletionInferenceEngine setThreadGroup( ThreadGroup threadgroup ){
		this.threadgroup = threadgroup;
		return this;
	}

	/** @since 20080227 */
	public enum Command{
		rewind( true, "restart", "restart belief propagation from scratch", "Rewind16.gif", getKeyStroke( VK_LEFT, 0 ) ){
			protected Command exec( EdgeDeletionInferenceEngine ie ) throws Exception{
				synchronized( ie.synch ){
					boolean restart = true;
					ie.residual = ie.bridge.edalgorithm.oneMoreIteration( restart );
					ie.notifyEvidenceChangeListeners();
				}
				return this;
			}
		},
		stepforward( true, "iterate", "proceed with one iteration", "StepForward16.gif", getKeyStroke( VK_RIGHT, 0 ) ){
			protected Command exec( EdgeDeletionInferenceEngine ie ) throws Exception{
				yield();
				synchronized( ie.synch ){
					if( ie.bridge.edalgorithm.iterationStatusOk() ){
						ie.residual = ie.bridge.edalgorithm.oneMoreIteration( false );
						ie.notifyEvidenceChangeListeners();
					}
				}
				return this;
			}

			public    boolean enabled( EdgeDeletionInferenceEngine ie ){ return ie.bridge.edalgorithm.iterationStatusOk(); }
		},
		play( true, "finish", "iterate until we reach convergence, time out, or maximum iterations", "Play16.gif", getKeyStroke( VK_DOWN, 0 ) ){
			protected Command exec( EdgeDeletionInferenceEngine ie ) throws Exception{
				ie.playing = true;
				yield();
				boolean iterated = false;
				while( ie.bridge.edalgorithm.iterationStatusOk() ){
					yield();
					if( interrupted() ){ break; }
					synchronized( ie.synch ){
						if( ie.bridge.edalgorithm.iterationStatusOk() ){
							ie.residual = ie.bridge.edalgorithm.oneMoreIteration( false );
							iterated = true;
							ie.notifyIterationListeners();
						}
					}
					yield();
				}
				ie.playing = false;
				if( iterated ){ synchronized( ie.synch ){ ie.notifyEvidenceChangeListeners(); } }
				return this;
			}

			public    boolean enabled( EdgeDeletionInferenceEngine ie ){ return ie.bridge.edalgorithm.iterationStatusOk(); }
		},
		stop( false, "pause", "stop iterating", "StopPlaying16.gif", getKeyStroke( VK_UP, 0 ) ){
			protected Command exec( EdgeDeletionInferenceEngine ie ) throws Exception{
				ie.threadgroup.interrupt();
				return this;
			}

			public    boolean enabled( EdgeDeletionInferenceEngine ie ){ return ie.playing; }
		},
		importcpts( true, "import cpts", "import cpts", "Import16.gif", getKeyStroke( VK_I, 0 ) ){
			protected Command exec( EdgeDeletionInferenceEngine ie ) throws Exception{
				ie.doPolicy( manual );
				return this;
			}

			public    boolean enabled( EdgeDeletionInferenceEngine ie ){ return ie.policy == manual; }
		};

		private Command( boolean asynchronous, String display, String description, String imagefilename, KeyStroke stroke ){
			this.asynchronous  = asynchronous;
			this.display       = display;
			this.description   = description;
			this.imagefilename = imagefilename;
			this.stroke        = stroke;
		}

		public    boolean enabled( EdgeDeletionInferenceEngine ie ){ return true; }

		protected Command    exec( EdgeDeletionInferenceEngine ie ) throws Exception { return this; }

		public    Command    safe( EdgeDeletionInferenceEngine ie, UncaughtExceptionHandler handler ){
			try{
				return exec( ie );
			}catch( Throwable thrown ){
				String msg = "warning: EdgeDeletionInferenceEngine command '"+name()+"'.safe() caught " + thrown;
				if( handler == null ){
					System.err.println( msg );
				}else{
					handler.uncaughtException( currentThread(), new RuntimeException( msg, thrown ) );
				}
			}
			return this;
		}

		public    Thread      run( final EdgeDeletionInferenceEngine ie, ThreadGroup group, UncaughtExceptionHandler handler, boolean synchronous ){
			if(   group == null ){         group    = currentThread().getThreadGroup(); }
			if( handler == null ){         handler  = group; }

			if( synchronous || (! asynchronous) ){ safe( ie, handler ); return currentThread(); }

			final UncaughtExceptionHandler ueh      = handler;
			final Runnable                 runnable = new Runnable(){
				public void run(){ Command.this.safe( ie, ueh ); }
			};
			final Thread                   thread   = new   Thread( group, runnable, name() );
			thread.start();
			return thread;
		}

		final  public  boolean    asynchronous;
		final  public  String     display, description, imagefilename;
		final  public  KeyStroke  stroke;
	}

	/** @since 20080227 */
	public Thread command( Command command ){
		return    command(         command, false );
	}

	/** @since 20080227 */
	public Thread command( Command command, boolean synchronous ){
		event = command;
		return command.run( this, this.threadgroup, null, synchronous );
	}

	/** @since 20080227 */
	private int notifyEvidenceChangeListeners(){
		int ret = beliefnetwork.getEvidenceController().notifyNonPriorityListeners();
		doPolicy( import_on_iteration );
		log();
		return ret;
	}

	/** In case this InferenceEngine wants to report two sets of answers,
		for example, approximate and exact.
		@since 20080226 */
	public Table[] conditionals( FiniteVariable var, Table[] buckets ){
		if( flag_compare2exact ){
			if( (buckets == null) || (buckets.length < 2) ){ buckets = new Table[2]; }
			buckets[0] = bridge.  varConditional( var );
			buckets[1] = bridge.exactConditional( var );
			return buckets;
		}
		else{ return super.conditionals( var, buckets ); }
	}

	/** In case this InferenceEngine wants to report two sets of answers,
		for example, approximate and exact.
		@since 20080226 */
	public String[] describeConditionals(){
		return flag_compare2exact ? DESCRIBE_CONDITIONALS : super.describeConditionals();
	}

	/** @since 20080226 */
	public static final String[] DESCRIBE_CONDITIONALS = new String[]{ "approximate probability", "exact probability" };

	/** @since 20080227 */
	public EdgeDeletionInferenceEngine addIterationListener( EvidenceChangeListener ecl ){
		if( listeners == null ){ listeners = new LinkedList<EvidenceChangeListener>(); }
		listeners.add( ecl );
		return this;
	}

	/** @since 20080309 */
	public boolean removeIterationListener( EvidenceChangeListener ecl ){
		return listeners == null ? false : listeners.remove( ecl );
	}

	/** @since 20080227 */
	private EdgeDeletionInferenceEngine notifyIterationListeners(){
		if( listeners == null ){ return this; }
		for( EvidenceChangeListener ecl : listeners ){ ecl.evidenceChanged( null ); }
		log();
		return this;
	}

	/** interface EvidenceChangeListener */
	@SuppressWarnings( "unchecked" )
	public void evidenceChanged( EvidenceChangeEvent ece ){
		dirty();
		synchronized( synch ){
			bridge.setEvidence( (Map<FiniteVariable,Object>) this.beliefnetwork.getEvidenceController().evidence() );
			if( flag_control ){ this.command( Command.rewind, true ); }
			else{ doPolicy( import_on_iteration ); }
		}
		log();
	}
	/** interface EvidenceChangeListener */
	public void         warning( EvidenceChangeEvent ece ){
		int size = ece == null ? 0 : (ece.recentEvidenceChangeVariables == null ? 0 : ece.recentEvidenceChangeVariables.size());
		event = "evidence change x" + Integer.toString( size );
	}

	/*     interface InferenceEngine     */
	public Table conditional( FiniteVariable var ){
		return bridge.varConditional( var );
	}
	/*     interface InferenceEngine     */
	public Table joint( FiniteVariable var ){
		throw new UnsupportedOperationException();
	}
	/*     interface InferenceEngine     */
	public double probability(){
		return bridge.prEvidence();
	}
	/** @since 20091116 */
	public char probabilityDisplayOperatorUnicode(){ return '\u2248'; }
	/*     interface InferenceEngine     */
	public void setCPT( FiniteVariable var ){
		dirty();
		event = "cpt change";
		log();
		if( auxiliaries().contains( var ) ){ return; }
		else{ throw new UnsupportedOperationException(); }
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
	public Set<FiniteVariable> variables(){
		return bridge.variables();
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

	/** @since 20080227 */
	public BeliefNetwork getBeliefNetwork(){
		return beliefnetwork;
	}

	/** @since 20080228 */
	public Bridge bridge(){
		return bridge;
	}

	/** @since 20080228 */
	public EdgeDeletionInferenceEngine dirty(){
		maxerrorclean = false;
		return this;
	}

	/** @since 20080228 */
	public boolean getFlagControl(){
		return flag_control;
	}

	/** @since 20080228 */
	public Settings<EdgeDeletionBeliefPropagationSetting> settings(){
		return this.settings;
	}

	/** @since 20080228 */
	public boolean logging(){
		return flag_log;
	}

	/** @since 20080229 */
	public Listener setListener( Listener listener ){
		return this.listener = listener;
	}

	/** @since 20080309 */
	public void die(){
		super.die();

		try{
			if(          bridge != null ){ bridge.die(); }
			this        .bridge  = null;
			if(   beliefnetwork != null ){ beliefnetwork.getEvidenceController().removePriorityEvidenceChangeListener( this ); }
			this .beliefnetwork  = null;
			if(        settings != null ){ settings.removeChangeListener( this ); }
			this      .settings  = null;
			this   .threadgroup  = null;
			if(       listeners != null ){ listeners.clear(); }
			this     .listeners  = null;
			if(       loggables != null ){ loggables.clear(); }
			this     .loggables  = null;
			if(           mylog != null ){ mylog.clear(); }
			this         .mylog  = null;
			this        .policy  = null;
			this      .listener  = null;
		  //if(     auxiliaries != null ){ auxiliaries.clear(); }//unmodifiable!!
			this   .auxiliaries  = null;
		}catch( Exception thrown ){
			System.err.println( "warning: EdgeDeletionInferenceEngine.die() caught " + thrown );
		  //thrown.printStackTrace( System.err );
		}
	}

	private           Bridge                         bridge;
	private    BeliefNetwork                         beliefnetwork;
	private    Settings<EdgeDeletionBeliefPropagationSetting> settings;
	private    boolean                               flag_compare2exact, flag_control, flag_log, playing, maxerrorclean;
	private    double                                residual, maxerror;
	private    ThreadGroup                           threadgroup;
	private    Object                                synch = new Object(), event;
	private    Collection<EvidenceChangeListener>    listeners;
	private    Collection<Attribute>                 loggables;
	private    Collection<Map<Attribute,Object>>     mylog;
	private    CPTPolicy                             policy;
	private    Listener                              listener;
	private    Collection<FiniteVariable>            auxiliaries;
}
