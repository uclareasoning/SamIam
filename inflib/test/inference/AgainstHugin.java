import COM.hugin.HAPI.*;

import edu.ucla.belief.*;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.io.NetworkIO;
import edu.ucla.belief.io.FileType;
import edu.ucla.belief.io.StandardNode;
import edu.ucla.belief.io.InstantiationXmlizer;
import edu.ucla.belief.sensitivity.TestCompleteness;
import edu.ucla.util.DataSetStatistic;
import edu.ucla.util.AbstractStringifier;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** @author keith cascio
	@since 20060319 */
public class AgainstHugin
{
	public static final String STR_OPT_PATH_NETWORK_FILE      = "-network";
	public static final String STR_OPT_ITERATIONS             = "-iterations";
	public static final String STR_OPT_IMPLEMENTATION         = "-impl";
	public static final String STR_IMPLEMENTATION_DELIMITERS  = ",";
	public static final String STR_IMPLEMENTATION_ALL         = "all";
	//public static final String STR_IMPLEMENTATION_MODIFIER_WARMUP     = "warmup";
	public static final String STR_OPT_SCHEME                 = "-scheme";
	public static final String STR_OPT_EVIDENCE               = "-inst";

	public final static int INT_MAX_ITERATIONS = Integer.MAX_VALUE;
	public static final long LONG_DELAY_MS_SCAN_USER_STOP = 16384;

	public static final String  REGEX_FILENAME_USER_HALT    = "\\.?(die|halt|stop|cancel|quit|end|exit|cease|finish|kill|break|terminate|return)(\\.(txt|dat|bat|sh|exe|dll|gz|zip))?$";
	private static      Pattern PATTERN_FILENAME_USER_HALT  = null;

	/** @since 20060320 */
	public enum InferenceImplementation{
		il2rc{
			public Dynamator getDynamator(){
				if( dyn == null ) dyn = new edu.ucla.belief.inference.RCEngineGenerator();
				return dyn;
			}

			public InferenceEngine createEngine( BeliefNetwork bn ){
				return getDynamator().manufactureInferenceEngine( bn );
			}

			private edu.ucla.belief.inference.RCEngineGenerator dyn;
		},
		il1shenoyshafer{
			public Dynamator getDynamator(){
				if( dyn == null ) dyn = new edu.ucla.belief.inference.JEngineGenerator();
				return dyn;
			}

			public InferenceEngine createEngine( BeliefNetwork bn ){
				return getDynamator().manufactureInferenceEngine( bn );
			}

			private edu.ucla.belief.inference.JEngineGenerator dyn;
		},
		il2hugin{
			public Dynamator getDynamator(){
				if( dyn == null ) dyn = new edu.ucla.belief.inference.HuginEngineGenerator();
				return dyn;
			}

			public InferenceEngine createEngine( BeliefNetwork bn ){
				return getDynamator().manufactureInferenceEngine( bn );
			}

			private edu.ucla.belief.inference.HuginEngineGenerator dyn;
		},
		il2zchugin{
			public Dynamator getDynamator(){
				if( dyn == null ) dyn = new edu.ucla.belief.inference.ZCEngineGenerator();
				return dyn;
			}

			public InferenceEngine createEngine( BeliefNetwork bn ){
				return getDynamator().manufactureInferenceEngine( bn );
			}

			private edu.ucla.belief.inference.ZCEngineGenerator dyn;
		},
		il2shenoyshafer{
			public Dynamator getDynamator(){
				if( dyn == null ) dyn = new edu.ucla.belief.inference.SSEngineGenerator();
				return dyn;
			}

			public InferenceEngine createEngine( BeliefNetwork bn ){
				return getDynamator().manufactureInferenceEngine( bn );
			}

			private edu.ucla.belief.inference.SSEngineGenerator dyn;
		},
		il1rc{
			public Dynamator getDynamator(){
				if( dyn == null ) dyn = new edu.ucla.belief.recursiveconditioning.RCEngineGenerator();
				return dyn;
			}

			public InferenceEngine createEngine( BeliefNetwork bn ){
				return getDynamator().manufactureInferenceEngine( bn );
			}

			private edu.ucla.belief.recursiveconditioning.RCEngineGenerator dyn;
		},
		il1bp{
			public Dynamator getDynamator(){
				if( dyn == null ) dyn = new edu.ucla.belief.approx.PropagationEngineGenerator();
				return dyn;
			}

			public InferenceEngine createEngine( BeliefNetwork bn ){
				return getDynamator().manufactureInferenceEngine( bn );
			}

			private edu.ucla.belief.approx.PropagationEngineGenerator dyn;
		},
		il1random{
			public Dynamator getDynamator(){
				if( dyn == null ) dyn = new edu.ucla.belief.inference.RandomEngineGenerator();
				return dyn;
			}

			public InferenceEngine createEngine( BeliefNetwork bn ){
				return getDynamator().manufactureInferenceEngine( bn );
			}

			private edu.ucla.belief.inference.RandomEngineGenerator dyn;
		};

		abstract public InferenceEngine createEngine( BeliefNetwork bn );
		abstract public Dynamator getDynamator();

		public void killEngine( BeliefNetwork bn ){
			if( bn != InferenceImplementation.this.myBeliefNetwork ) errorMessage( STR_ERROR_CLOBBER );
			if( InferenceImplementation.this.ie == null ) return;
			if( bn != null ){
				EvidenceController ec = bn.getEvidenceController();
				ec.removePriorityEvidenceChangeListener( InferenceImplementation.this.ie );
			}
			InferenceImplementation.this.ie.die();
			InferenceImplementation.this.ie = null;
		}

		public InferenceEngine getEngine( BeliefNetwork bn ){
			if( InferenceImplementation.this.myBeliefNetwork == null ) InferenceImplementation.this.myBeliefNetwork = bn;
			if( bn != InferenceImplementation.this.myBeliefNetwork ) errorMessage( STR_ERROR_CLOBBER );
			if( ie == null ) ie = createEngine( bn );
			return ie;
		}

		public void init( BeliefNetwork bn, int iterations, int sizeHalf, int sizeEstimated ){
			System.out.print( "testing " );
			System.out.print( getDynamator().getDisplayName() );
			System.out.println( "..." );

			InferenceEngine engine = InferenceImplementation.this.getEngine( bn );

			myFiniteVariableMaxError = null;
			myIndexMaxError = -1;
			if( myEvidenceMaxErrorMarg == null ) myEvidenceMaxErrorMarg = new HashMap<FiniteVariable,Object>( sizeHalf );
			else myEvidenceMaxErrorMarg.clear();
			if( myErrorMarginals == null ) myErrorMarginals = new DataSetStatistic( name() + " marginals error", sizeEstimated );
			else myErrorMarginals.clear();

			if( engine.probabilitySupported() ){
				if( myEvidenceMaxErrorPrE == null ) myEvidenceMaxErrorPrE = new HashMap<FiniteVariable,Object>( sizeHalf );
				else myEvidenceMaxErrorPrE.clear();
				if( myErrorPrE == null ) myErrorPrE = new DataSetStatistic( name() + " pr(e) error", iterations );
				else myErrorPrE.clear();
			}
		}

		public boolean recordErrorMarginal( double error, FiniteVariable fVar, int i, Map<FiniteVariable,Object> evidence ){
			myErrorMarginals.addDataPoint( error );
			boolean isMax = (error == myErrorMarginals.getMax());
			if( isMax ){
				myFiniteVariableMaxError = fVar;
				myIndexMaxError = i;
				myEvidenceMaxErrorMarg.clear();
				myEvidenceMaxErrorMarg.putAll( evidence );
			}
			return isMax;
		}

		public boolean recordErrorPrE( double error, Map<FiniteVariable,Object> evidence ){
			myErrorPrE.addDataPoint( error );
			boolean isMax = (error == myErrorPrE.getMax());
			if( isMax ){
				myEvidenceMaxErrorPrE.clear();
				myEvidenceMaxErrorPrE.putAll( evidence );
			}
			return isMax;
		}

		public PrintStream report( PrintStream out ) throws IOException{
			out.println( report( new StringBuilder( 256 ) ).toString() );
			return out;
		}

		public Appendable report( Appendable buff ) throws IOException{
			buff.append( '\n' );
			buff.append( name() );
			buff.append( " report:\n" );

			myErrorMarginals.report( buff );
			buff.append( '\n' );
			buff.append( "    max for pr( " );
			buff.append( myFiniteVariableMaxError.getID() );
			buff.append( '[' );
			buff.append( Integer.toString( myIndexMaxError ) );
			buff.append( "] )" );
			buff.append( "\n    max at evidence { " );
			buff.append( AbstractStringifier.VARIABLE_ID.mapToString( myEvidenceMaxErrorMarg ) );
			buff.append( " }" );

			if( (myErrorPrE != null) && (myEvidenceMaxErrorPrE != null) ){
				buff.append( '\n' );
				myErrorPrE.report( buff );
				buff.append( "\n    max at evidence { " );
				buff.append( AbstractStringifier.VARIABLE_ID.mapToString( myEvidenceMaxErrorPrE ) );
				buff.append( " }" );
			}

			return buff;
		}

		private DataSetStatistic myErrorPrE;
		private Map<FiniteVariable,Object> myEvidenceMaxErrorPrE;

		private DataSetStatistic myErrorMarginals;
		private FiniteVariable myFiniteVariableMaxError;
		private int myIndexMaxError = -1;
		private Map<FiniteVariable,Object> myEvidenceMaxErrorMarg;
		private InferenceEngine ie;
		private BeliefNetwork myBeliefNetwork;

		public static InferenceImplementation getDefault(){
			return il2shenoyshafer;
		}

		public static StringBuilder usage( StringBuilder buff ){
			//if( USAGE == null ){
				//StringBuffer buff = new StringBuffer(128);
				buff.append( "\"value(,value)*\" | \"" );
				buff.append( STR_IMPLEMENTATION_ALL );
				buff.append( "\", where value is one of:\n\n      " );
				for( InferenceImplementation impl : InferenceImplementation.values() ){
					buff.append( impl.name() );
					buff.append( " | " );
				}
				buff.setLength( buff.length()-3 );
				//buff.append( '}' );
				return buff;
				//USAGE = buff.toString();
			//}
			//return USAGE;
		}
		//private static String USAGE;
		private static String STR_ERROR_CLOBBER = "illegal to test more than one network (unsupported)";
	}

	public static void main( String[] args ){
		if( scanUserStopped() ) return;

		if( args.length < 1 ){
			System.err.println( usage() );
			System.exit(1);
		}

		File fileNetwork = null, fileInst = null;
		Integer intIterations = null;
		boolean flagAllImplementations = false;
		List<AgainstHugin.InferenceImplementation> implementations = null;
		AgainstHugin.Scheme scheme = null;

		String msgerr = null;
		boolean flagPrintUsage = false;
		try{
			for( int i=0; i < args.length; i++ ){
				if( args[i].startsWith( STR_OPT_PATH_NETWORK_FILE ) ){
					String pathNetwork = args[i].substring( STR_OPT_PATH_NETWORK_FILE.length() );
					if( pathNetwork.length() < 1 ) pathNetwork = null;
					if( pathNetwork == null ){
						int ind = i + 1;
						if( ind < args.length ){
							pathNetwork = args[ind];
							i = ind;
						}
					}
					if( (pathNetwork == null) || (pathNetwork.length() < 1) ){
						flagPrintUsage = true;
						errorMessage( STR_OPT_PATH_NETWORK_FILE + " does not specify a file." );
					}
					else if( !(fileNetwork = new File(pathNetwork)).exists() ){
						errorMessage( "Network file \"" +pathNetwork+ "\" does not exist." );
					}

					FileType type = FileType.getTypeForFile( fileNetwork );
					if( (type == null) || (!type.isHuginType()) ){
						errorMessage( "Network file \"" +pathNetwork+ "\" must be Hugin file format (*.net/*.hugin)." );
					}
				}
				else if( args[i].startsWith( STR_OPT_ITERATIONS ) ){
					String strIterations = args[i].substring( STR_OPT_ITERATIONS.length() );
					if( strIterations.length() < 1 ) strIterations = null;
					if( strIterations == null ){
						int ind = i + 1;
						if( ind < args.length ){
							strIterations = args[ind];
							i = ind;
						}
					}
					if( (strIterations == null) || (strIterations.length() < 1) ){
						flagPrintUsage = true;
						errorMessage( STR_OPT_ITERATIONS + " requires a number." );
					}
					else{
						intIterations = new Integer(strIterations);
						int intValue = intIterations.intValue();
						if( (intValue <= 0) || (intValue > INT_MAX_ITERATIONS) )
							errorMessage( "Iterations \"" +strIterations+ "\" is not valid." );
					}
				}
				else if( args[i].startsWith( STR_OPT_IMPLEMENTATION ) ){
					String token = args[i].substring( STR_OPT_IMPLEMENTATION.length() );
					if( token.length() < 1 ) token = null;
					if( token == null ){
						int ind = i + 1;
						if( ind < args.length ){
							token = args[ind];
							i = ind;
						}
					}
					if( (token == null) || (token.length() < 1) ){
						flagPrintUsage = true;
						errorMessage( STR_OPT_IMPLEMENTATION + " requires a value." );
					}
					else{
						//AgainstHugin.InferenceImplementation.parse( token );
						implementations = new LinkedList<AgainstHugin.InferenceImplementation>();
						StringTokenizer toker = new StringTokenizer( token, STR_IMPLEMENTATION_DELIMITERS );
						String nextToken;
						while( toker.hasMoreTokens() ){
							nextToken = toker.nextToken();
							if( STR_IMPLEMENTATION_ALL.equals( nextToken ) ) flagAllImplementations = true;
							//else if( STR_IMPLEMENTATION_MODIFIER_WARMUP.equals( nextToken ) ) flagWarmup = true;
							//else if( STR_IMPLEMENTATION_MODIFIER_PREWARMUP.equals( nextToken ) ) flagPreWarmup = true;
							else implementations.add( AgainstHugin.InferenceImplementation.valueOf( nextToken ) );
						}
					}
				}
				else if( args[i].startsWith( STR_OPT_SCHEME ) ){
					String token = args[i].substring( STR_OPT_SCHEME.length() );
					if( token.length() < 1 ) token = null;
					if( token == null ){
						int ind = i + 1;
						if( ind < args.length ){
							token = args[ind];
							i = ind;
						}
					}
					if( (token == null) || (token.length() < 1) ){
						flagPrintUsage = true;
						errorMessage( STR_OPT_SCHEME + " requires a value." );
					}
					else scheme = AgainstHugin.Scheme.valueOf( token );
				}
				else if( args[i].startsWith( STR_OPT_EVIDENCE ) ){
					String token = args[i].substring( STR_OPT_EVIDENCE.length() );
					if( token.length() < 1 ) token = null;
					if( token == null ){
						int ind = i + 1;
						if( ind < args.length ){
							token = args[ind];
							i = ind;
						}
					}
					if( (token == null) || (token.length() < 1) ){
						flagPrintUsage = true;
						errorMessage( STR_OPT_EVIDENCE + " requires a value." );
					}
					else if( !(fileInst = new File(token)).exists() ){
						errorMessage( "Evidence file \"" +token+ "\" does not exist." );
					}
				}
			}

			if( fileNetwork == null ){
				flagPrintUsage = true;
				errorMessage( "Must specify network file using " + STR_OPT_PATH_NETWORK_FILE );
			}

			AgainstHugin againsthugin = new AgainstHugin( fileNetwork );
			if( intIterations   != null ) againsthugin.myIterationsRequested = intIterations.intValue();
			if( implementations != null ) againsthugin.myImplementations     = implementations.toArray( new AgainstHugin.InferenceImplementation[implementations.size()] );
			againsthugin.myFlagAllImplementations                            = flagAllImplementations;
			if( scheme          != null ) againsthugin.myScheme              = scheme;
			if( fileInst        != null ) againsthugin.myFileInst            = fileInst;

			againsthugin.run();
		}catch( Exception exception ){
			fileNetwork = null;
			msgerr = exception.getMessage();
			if( msgerr == null ) msgerr = exception.toString();
			if( exception.getCause() != CAUSA_SUI ) exception.printStackTrace();
		}finally{
			if( msgerr != null ){
				System.err.println();
				System.err.println( msgerr );
				if( flagPrintUsage ){
					System.err.println();
					System.err.println( AgainstHugin.usage() );
				}
				System.exit(1);
			}
		}
	}

	public static void errorMessage( String msgerr ) throws RuntimeException{
		RuntimeException runtimeexception = new RuntimeException( msgerr, CAUSA_SUI );
		//runtimeexception.initCause( runtimeexception );//whoops java.lang.IllegalArgumentException: Self-causation not permitted
		throw runtimeexception;
	}
	private static final Throwable CAUSA_SUI = new Throwable();
	private static String STR_USAGE_AGAINSTHUGIN;

	public static String usage(){
		if( STR_USAGE_AGAINSTHUGIN == null ){
			StringBuilder buff = new StringBuilder( 256 );
			buff.append( "usage: " );
			buff.append( AgainstHugin.class.getName() );
			buff.append( ' ' );
			buff.append( STR_OPT_PATH_NETWORK_FILE );
			buff.append( " <path to network> [" );
			buff.append( STR_OPT_ITERATIONS );
			buff.append( " #] [" );
			buff.append( STR_OPT_SCHEME );
			buff.append( " <scheme>] [" );
			buff.append( STR_OPT_IMPLEMENTATION );
			buff.append( " <spec>]\n\n" );
			buff.append( "\n\n    <scheme> = " );
			AgainstHugin.Scheme.usage( buff );
			buff.append( "\n\n    <spec>   = " );
			AgainstHugin.InferenceImplementation.usage( buff );
			STR_USAGE_AGAINSTHUGIN = buff.toString();
		}
		return STR_USAGE_AGAINSTHUGIN;
	}

	public boolean userStoppedLite(){
		long now = System.currentTimeMillis();

		boolean result = false;
		if( (now - myTimeLastScanForStopMillis) > LONG_DELAY_MS_SCAN_USER_STOP ){
			result = scanUserStopped();
			myTimeLastScanForStopMillis = now;
		}

		return myFlagStop = result;
	}

	public static boolean scanUserStopped(){
		if( PATTERN_FILENAME_USER_HALT == null ) PATTERN_FILENAME_USER_HALT = Pattern.compile( REGEX_FILENAME_USER_HALT, Pattern.CASE_INSENSITIVE );

		AgainstHugin.PATH_USER_HALT = null;

		Matcher matcher = PATTERN_FILENAME_USER_HALT.matcher( "" );
		for( String path : myWorkingDirectory.list() ){
			if( matcher.reset( path ).find() ){
				AgainstHugin.PATH_USER_HALT = path;
				System.out.println( "\nInference test halted at "+TestCompleteness.DateFormatFilename.now()+" by presence of file \""+path+"\".\n" );
				return true;
			}
		}

		return false;
	}

	public AgainstHugin( File fileNetwork ){
		AgainstHugin.this.myFileNetwork = fileNetwork;
	}

	public void run() throws Exception{
		System.out.println( "Inference test started at " + TestCompleteness.DateFormatFilename.now() + ". To halt, touch (create) a file named \"die.txt\"." );

		Domain domain = domain();
		domain.compile( Domain.H_TM_FILL_IN_WEIGHT );
		BeliefNetwork bn = bn();

		AgainstHugin.InferenceImplementation[] array = null;

		if( AgainstHugin.this.myFlagAllImplementations ) array = AgainstHugin.InferenceImplementation.values();
		else if( AgainstHugin.this.myImplementations != null ) array = AgainstHugin.this.myImplementations;
		else array = new AgainstHugin.InferenceImplementation[]{ InferenceImplementation.getDefault() };

		myScheme.run( array, AgainstHugin.this );
	}

	/** @since 20060320 */
	public enum Scheme{
		parallel{
			public void run( AgainstHugin.InferenceImplementation[] array, AgainstHugin againsthugin ) throws Exception{
				if( array.length > 1 ) System.out.println( "\ntesting in parallel "+Arrays.toString(array) );
				againsthugin.runParallel( array );
			}
		},
		serial{
			public void run( AgainstHugin.InferenceImplementation[] array, AgainstHugin againsthugin ) throws Exception{
				if( array.length > 1 ) System.out.println( "\ntesting serially "+Arrays.toString(array) );
				againsthugin.runSerial( array );
			}
		},
		huginpre{
			public void run( AgainstHugin.InferenceImplementation[] array, AgainstHugin againsthugin ) throws Exception{
				againsthugin.runHuginPrE( againsthugin.evidenceFromFile() );
			}
		},
		huginmpe{
			public void run( AgainstHugin.InferenceImplementation[] array, AgainstHugin againsthugin ) throws Exception{
				againsthugin.runHuginMPE( againsthugin.evidenceFromFile() );
			}
		};

		abstract public void run( AgainstHugin.InferenceImplementation[] array, AgainstHugin againsthugin ) throws Exception;

		public static Scheme getDefault(){
			return parallel;
		}

		public static StringBuilder usage( StringBuilder buff ){
			//if( USAGE == null ){
				//StringBuffer buff = new StringBuffer(128);
				for( Scheme scheme : Scheme.values() ){
					buff.append( scheme.name() );
					buff.append( " | " );
				}
				buff.setLength( buff.length()-3 );
				buff.append( ", default \"" );
				buff.append( Scheme.getDefault() );
				buff.append( '"' );
				return buff;
				//USAGE = buff.toString();
			//}
			//return USAGE;
		}
		//private static String USAGE;
	}

	public void runSerial( InferenceImplementation[] impls ) throws Exception{
		InferenceImplementation[] array = new InferenceImplementation[1];
		for( InferenceImplementation impl : impls ){
			array[0] = impl;
			runParallel( array );
			if( myFlagStop ) break;
		}
	}

	public void runParallel( InferenceImplementation[] impls ) throws Exception{
		System.out.println();
		System.out.println();

		BeliefNetwork bn = bn();
		String timeHalt = null;
		Throwable throwableHalt = null;

		for( InferenceImplementation impl : impls ) impl.init( bn, myIterationsRequested, mySizeHalf, myEstimatedSize );

		try{
			for( int i=0; i<myIterationsRequested; i++ ){
				runOnce( impls, randomEvidence() );
				if( myFlagStop ) break;
			}
		}catch( Throwable throwable ){
			timeHalt      = TestCompleteness.DateFormatFilename.now();
			throwableHalt = throwable;
		}

		try{
			for( InferenceImplementation impl : impls ) impl.killEngine( bn );
		}catch( Throwable throwable ){
			if( timeHalt      == null ) timeHalt      = TestCompleteness.DateFormatFilename.now();
			if( throwableHalt == null ) throwableHalt = throwable;
		}

		try{
			for( InferenceImplementation impl : impls ) impl.report( System.out );
		}catch( Throwable throwable ){
			if( timeHalt      == null ) timeHalt      = TestCompleteness.DateFormatFilename.now();
			if( throwableHalt == null ) throwableHalt = throwable;
		}

		if( throwableHalt != null ){
			System.out.println( "\nInference test halted at "+timeHalt+" by" );
			throwableHalt.printStackTrace();
			myFlagStop = true;
		}
	}

	/** @since 20060703 */
	public Map<FiniteVariable,Object> evidenceFromFile() throws Exception{
		if( myFileInst == null ) return Collections.emptyMap();

		InstantiationXmlizer instantiationxmlizer = new InstantiationXmlizer();
		Map map = instantiationxmlizer.getMap( myFileInst );

		Map<FiniteVariable,Object> ret = new HashMap<FiniteVariable,Object>( map.size() );
		BeliefNetwork bn = bn();
		FiniteVariable fVar;
		int index;
		for( Object key : map.keySet() ){
			fVar = (FiniteVariable) bn.forID( key.toString() );
			if( fVar == null ) errorMessage( "failed setting evidence, no variable with id \"" +key.toString()+ "\"" );
			index = fVar.index( map.get( key ) );
			if( index < 0 ) errorMessage( "failed setting evidence, variable \"" +key.toString()+ "\" has no state \"" +map.get( key )+ "\"" );
			ret.put( fVar, fVar.instance( index ) );
		}

		return ret;
	}

	/** @since 20060703 */
	public void runHuginPrE( Map<FiniteVariable,Object> evidence ) throws Exception{
		runHugin( evidence, Domain.H_EQUILIBRIUM_SUM, "evidence" );
	}

	/** @since 20060703 */
	public void runHuginMPE( Map<FiniteVariable,Object> evidence ) throws Exception{
		runHugin( evidence, Domain.H_EQUILIBRIUM_MAX, "MPE" );
	}

	/** @since 20060704 */
	private void runHugin(   Map<FiniteVariable,Object> evidence, Domain.Equilibrium equilibrium, String caption ) throws Exception{
		int size = 0;
		if( evidence != null ){
			setEvidence( evidence );
			size = evidence.size();
		}
		Domain domain = domain();
		domain.propagate( equilibrium, Domain.H_EVIDENCE_MODE_NORMAL );

		double probabilityHugin = domain.getNormalizationConstant();

		System.out.println( "hugin p( "+caption+", evidence size "+size+" )? " + probabilityHugin + " ("+Math.log(probabilityHugin)+")" );
	}

	public void runOnce( InferenceImplementation[] impls, Map<FiniteVariable,Object> evidence ) throws Exception{
		setEvidence( evidence );
		Domain domain = domain();
		domain.propagate( Domain.H_EQUILIBRIUM_SUM, Domain.H_EVIDENCE_MODE_NORMAL );

		NodeList nodelist = new NodeList();
		DiscreteChanceNode dcn;
		COM.hugin.HAPI.Table tableHugin;
		String id;
		String label;
		FiniteVariable fVar;
		for( Object node : domain().getNodes() ){
			dcn   = (DiscreteChanceNode) node;
			id    = dcn.getName();
			label = dcn.getLabel();
			fVar  = lookup( dcn );
			if( fVar == null ) errorMessage( "variable not found with label \"" +label+ "\"" );
			if( evidence.keySet().contains( fVar ) ) continue;

			if( userStoppedLite() ) return;

			nodelist.clear();
			nodelist.add( dcn );
			tableHugin = domain.getMarginal( nodelist );

			for( InferenceImplementation impl : impls ){
				compare( tableHugin, impl, fVar, evidence );
			}
		}

		BeliefNetwork bn = bn();
		InferenceEngine engine;
		double probabilityHugin = domain.getNormalizationConstant();
		double probabilitySamIam;
		double delta = Double.NaN;
		double error = Double.NaN;
		for( InferenceImplementation impl : impls ){
			engine = impl.getEngine( bn );
			if( engine.probabilitySupported() ){
				probabilitySamIam = engine.probability();
				delta = probabilityHugin - probabilitySamIam;
				error = Math.abs( delta );
				impl.recordErrorPrE( error, evidence );
			}
		}
	}

	private void compare( COM.hugin.HAPI.Table tableHugin, InferenceImplementation impl, FiniteVariable fVar, Map<FiniteVariable,Object> evidence ) throws Exception{
		InferenceEngine ie = impl.getEngine( bn() );

		edu.ucla.belief.Table tableSamIam = ie.conditional( fVar );

		double[] dataHugin = tableHugin.getData();
		double[] dataSamIam = tableSamIam.dataclone();

		/*System.out.println( "compare( "+fVar.getID()+" )" );
		System.out.println( "    hugin  " + Arrays.toString( dataHugin ) );
		System.out.println( "    samiam " + Arrays.toString( dataSamIam ) );*/

		if( dataHugin.length != dataSamIam.length ){
			errorMessage( "dataHugin.length != dataSamIam.length for \"" +fVar.getID()+ "\"" );
		}

		double delta = Double.NaN;
		double error = Double.NaN;
		int size = dataHugin.length;
		for( int i=0; i<size; i++ ){
			delta = dataHugin[i] - dataSamIam[i];
			error = Math.abs( delta );
			impl.recordErrorMarginal( error, fVar, i, evidence );
			/*myError.addDataPoint( error );

			if( error == myError.getMax() ){
				myFiniteVariableMaxError = fVar;
				myIndexMaxError = i;
				myEvidenceMaxError.clear();
				myEvidenceMaxError.putAll( ec().evidence() );
			}*/
		}
	}

	//private FiniteVariable myFiniteVariableMaxError = null;
	//private int myIndexMaxError = -1;
	//private Map<FiniteVariable,Object> myEvidenceMaxError;

	//private DataSetStatistic myError;
	private Domain myDomain;
	private BeliefNetwork myBeliefNetwork;
	private InferenceEngine myInferenceEngine;
	private File myFileNetwork;
	private File myFileInst;
	private MyParseListener myParseListener;
	private ClassCollection myClassCollection;
	private Map<DiscreteChanceNode,FiniteVariable> myLookupHuginToSamIam;
	private Map<FiniteVariable,DiscreteChanceNode> myLookupSamIamToHugin;

	private FiniteVariable lookup( DiscreteChanceNode nodeToLookup ) throws Exception{
		if( myLookupHuginToSamIam == null ) createLookup();
		return myLookupHuginToSamIam.get( nodeToLookup );
	}

	private DiscreteChanceNode lookup( FiniteVariable varToLookup ) throws Exception{
		if( myLookupSamIamToHugin == null ) createLookup();
		return myLookupSamIamToHugin.get( varToLookup );
	}

	private void createLookup() throws Exception{
		if( domain().getNodes().size() != myBeliefNetwork.size() ) errorMessage( "domain().getNodes().size() != myBeliefNetwork.size()" );

		myLookupHuginToSamIam = new HashMap<DiscreteChanceNode,FiniteVariable>( mySize );
		myLookupSamIamToHugin = new HashMap<FiniteVariable,DiscreteChanceNode>( mySize );

		Map<String,FiniteVariable> map = new HashMap<String,FiniteVariable>( mySize );
		String label;
		for( FiniteVariable fVar : myArray ){
			label = ((StandardNode)fVar).getLabel();
			if( map.containsKey( label ) ) errorMessage( "duplicate node label \"" + label + "\"" );
			else map.put( label, fVar );
		}

		DiscreteChanceNode dcn;
		FiniteVariable fVar;
		for( Object node : domain().getNodes() ){
			dcn = (DiscreteChanceNode) node;
			label = dcn.getLabel();
			fVar = map.get( label );
			if( fVar == null ) errorMessage( "failed to map DiscreteChanceNode labeled \"" +label+ "\" to a FiniteVariable" );
			myLookupHuginToSamIam.put( dcn, fVar );
			myLookupSamIamToHugin.put( fVar, dcn );
		}
	}

	public static final int  INT_LOOP_THRESHOLD           = 1024;
	public static final int  INT_MINIMUM_SIZE             = 3;

	private void setEvidence( Map<FiniteVariable,Object> evidence ) throws Exception{
		//System.out.println( "setEvidence( "+evidence.size()+" )" );
		//System.out.println( "    listeners: " + ec().getPriorityEvidenceChangeListeners( null ) );

		//ec().resetEvidence();//redundant, excessive
		ec().setObservations( MAP_UTIL_EVIDENCE );

		Domain domain = domain();
		int caseNew = domain.newCase();
		domain.retractFindings();

		int size = evidence.size();
		if( size < 1 ) return;

		/*try{
			int sizeBuff = size*32;
			StringBuilder buffIDs    = new StringBuilder( sizeBuff );
			StringBuilder buffStates = new StringBuilder( sizeBuff );
			for( FiniteVariable var : evidence.keySet() ){
				buffIDs.append( lookup( var ).getName() );//.getID() );//((StandardNode)var).getLabel() );
				buffIDs.append( ',' );
				buffStates.append( evidence.get( var ) );
				buffStates.append( ',' );
			}

			buffIDs.setLength( buffIDs.length() - 1 );
			buffStates.setLength( buffStates.length() - 1 );

			String ids = buffIDs.toString();
			String states = buffStates.toString();

			File temp = tempCaseFile();
			PrintWriter out = new PrintWriter( new FileWriter( temp ) );
			out.println( ids );
			out.println( states );
			out.close();

			System.out.println( "case written to " + temp.getPath() );
			System.out.println( ids );
			System.out.println( states );

			domain().parseCase( temp.getAbsolutePath(), pl() );
		}catch( Exception exception ){
			System.err.println( "caught " + exception );
		}*/

		Object value;
		int index;
		DiscreteChanceNode dcn;
		for( FiniteVariable var : evidence.keySet() ){
			index = var.index( value = evidence.get( var ) );
			if( index < 0 ) errorMessage( "variable \"" +var.getID()+ "\" does not contain value \"" +value+ "\"" );
			dcn = lookup( var );
			dcn.setCaseState( caseNew, index );
			dcn.selectState( index );
		}
	}

	private Map<FiniteVariable,Object> randomEvidence() throws Exception{
		int numAssertions = myRandom.nextInt( mySizeHalf );

		MAP_UTIL_EVIDENCE.clear();
		FiniteVariable var;
		Object value;
		for( int i=0; i<numAssertions; i++ ){
			var = pickRandomVariable( MAP_UTIL_EVIDENCE.keySet() );
			value = pickRandomValue( var );
			MAP_UTIL_EVIDENCE.put( var, value );
		}

		return MAP_UTIL_EVIDENCE;
	}

	private FiniteVariable pickRandomVariable( Set<FiniteVariable> exclusions ){
		FiniteVariable ret = null;
		int i=0;
		while( exclusions.contains( ret = myArray[ myRandom.nextInt( mySize ) ]  ) ){
			if( i++ > INT_LOOP_THRESHOLD ) errorMessage( "Failed to pick random variable not contained in set of " + exclusions.size() + " exclusions." );
		}
		return ret;
	}

	private Object pickRandomValue( FiniteVariable var ){
		return var.instance( myRandom.nextInt( var.size() ) );
	}

	private File tempCaseFile() throws IOException {
		if( myTempCaseFile == null ) myTempCaseFile = File.createTempFile( "hugin.case.", ".tmp" );
		return myTempCaseFile;
	}

	private Domain domain() throws Exception {
		if( myDomain == null ){
			myDomain = AgainstHugin.this.readHuginNet( AgainstHugin.this.myFileNetwork );

			/*System.out.println( "construacted Domain with nodes: " );
			DiscreteChanceNode dcn;
			for( Object node : myDomain.getNodes() ){
				dcn = (DiscreteChanceNode)node;
				System.out.println( "name: " + dcn.getName() + ", label: " + dcn.getLabel() );
			}*/
		}
		return myDomain;
	}

	private BeliefNetwork bn(){
		if( myBeliefNetwork == null ){
			try{
				myBeliefNetwork = NetworkIO.readHuginNet( AgainstHugin.this.myFileNetwork );
			}catch( Exception exception ){
				System.err.println( "Warning: failed to read network, caught " + exception );
			}
			AgainstHugin.this.init();
		}
		return myBeliefNetwork;
	}

	private InferenceEngine ie(){
		if( myInferenceEngine == null ){
			EliminationHeuristic.MIN_FILL.setRepetitions( 512 );
			EliminationHeuristic.MIN_FILL.setSeed( new Random() );

			DefaultGenerator dyn = new edu.ucla.belief.inference.SSEngineGenerator();
			//SSEngineBetterGenerator dyn = new edu.ucla.belief.inference.SSEngineBetterGenerator();
			//dyn.setJoinTree( myBeliefNetwork, convert( myOrder ) );
			myInferenceEngine = dyn.manufactureInferenceEngine( myBeliefNetwork );

			//System.out.println( "created inference engine " + myInferenceEngine.getClass().getName() );

			Set evidence = ec().evidence().keySet();
			if( !evidence.isEmpty() ){
				myInferenceEngine.evidenceChanged( new EvidenceChangeEvent( evidence ) );
			}
		}
		return myInferenceEngine;
	}

	private void init(){
		Dynamator.FLAG_DEBUG_DISPLAY_NAMES = true;

		this.mySize = myBeliefNetwork.size();

		if( mySize < INT_MINIMUM_SIZE ) errorMessage( "Network must contain at least " + INT_MINIMUM_SIZE + " variables." );

		this.mySizeHalf = (mySize / 2) + 1;
		this.myArray = (FiniteVariable[]) myBeliefNetwork.topologicalOrder().toArray( new FiniteVariable[ mySize ] );

		myNumStates = 0;
		for( FiniteVariable fVar : myArray ) myNumStates += fVar.size();
		myEstimatedSize = myNumStates*myIterationsRequested;
		//this.myError = new DataSetStatistic( "marginal error", myEstimatedSize );

		/*this.mySetParametersTested = new HashSet<CPTParameter>( INT_ITERATIONS_DEFAULT );

		this.myXmlizer = new InstantiationXmlizer();
		this.myFileEvidence = new File( "." + File.separator + "completeness_test_evidence.inst" );

		PRECISIONSOUNDNESS.init();*/
	}

	private EvidenceController ec(){
		return bn().getEvidenceController();
	}

	private MyParseListener pl(){
		if( myParseListener == null ) myParseListener = new MyParseListener();
		return myParseListener;
	}

	private ClassCollection cc(){
		if( myClassCollection == null ){
			try{
				myClassCollection = new ClassCollection();
			}catch( ExceptionHugin exceptionhugin ){
				System.err.println( "Warning: failed to construct ClassCollection, caught " + exceptionhugin );
			}
		}
		return myClassCollection;
	}

	/** Load a Bayesian network, compile it, and propagate evidence. */
	public Domain readHuginNet( File fileNetwork ) throws Exception
	{
		Domain ret = null;

		String pathNetwork = fileNetwork.getCanonicalFile().getAbsolutePath();

		if( pathNetwork.endsWith( ".oobn" ) ){
			ClassCollection cc = cc();
			cc.parseClasses( pathNetwork, pl() );
			//System.out.println( cc.getMembers() );
			String nameNetwork = NetworkIO.extractNetworkNameFromPath( pathNetwork );
			// Unfold the Class to a Domain that can be compiled and
			// used for inference, etc.
			ret = cc.getClassByName( nameNetwork ).createDomain();
		}
		else if( pathNetwork.endsWith( ".net" ) ) ret = new Domain( pathNetwork, pl() );
		else throw new RuntimeException( "unrecognized file extension" );

		return ret;
	}

	public static class MyParseListener implements ClassParseListener{
		public void parseError( int line, String msg ){
			System.out.println( "Parse error in line " + line + ": " + msg );
		}

		public void insertClass( String className, ClassCollection cc ){
			try {
				cc.parseClasses (className + ".net", this);
			}
			catch (ExceptionHugin e) {
				System.out.println ("Parsing failed: " + e.getMessage ());
			}
		}
	}

	private FiniteVariable[] myArray;
	private int mySize = -1, mySizeHalf = -1, myNumStates = -1, myEstimatedSize = -1;
	private Random myRandom = new Random();
	private File myTempCaseFile;
	private int myIterationsRequested = 1;
	private boolean myFlagAllImplementations = false;
	//private List<AgainstHugin.InferenceImplementation> myImplementations = null;
	private AgainstHugin.InferenceImplementation[] myImplementations;
	private AgainstHugin.Scheme myScheme = AgainstHugin.Scheme.getDefault();
	private static String PATH_USER_HALT;
	private long myTimeLastScanForStopMillis = 0;
	private boolean myFlagStop = false;
	private static File myWorkingDirectory = new File(".");

	private static final Map <FiniteVariable,Object> MAP_UTIL_EVIDENCE         = new HashMap<FiniteVariable,Object>();
}
