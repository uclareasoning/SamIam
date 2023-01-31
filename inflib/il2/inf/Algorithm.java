package il2.inf;

import        il2.inf.                                   PartialDerivativeEngine;
import        il2.inf.structure.                         EliminationOrders;
import        il2.inf.structure.      EliminationOrders .JT;
import        il2.inf.structure.      EliminationOrders .Record;
import        il2.model.                                 BayesianNetwork;
import        il2.inf.jointree.                          Flavor;
import        il2.inf.rc.                                RCEngine;
import        il2.inf.rc.                                CachingScheme;
import        il2.util.                                  IntList;
import        il2.bridge.                                Converter;

import        java.util.                                 Map;
import        java.util.                                 EnumMap;
import        java.util.                                 EnumSet;
import        java.util.                                 Set;
import        java.util.                                 Collection;
import        java.util.                                 Collections;
import        java.util.                                 Arrays;
import        java.util.                                 Random;
import        java.math.                                 BigInteger;
import static java.lang.System .out;
import static java.lang.System .err;

/** Semantically, each "Algorithm" constant represents a distinctive implementation of an inference algorithm.
    In practice, an "Algorithm" Object is a factory for manufacturing inference engines.
	Some algorithms support {@link il2.inf.PartialDerivativeEngine#varPartial( int ) partial derivatives}, others do not.
	Call {@link #partials() partials()} to get the algorithms that do.

	@author keith cascio
	@since 20081029 */
public enum Algorithm{
	/** recursive conditioning: {@link il2.inf.rc.RCEngine RCEngine} */
	recursiveconditioning     ( RCEngine.class ){
		@SuppressWarnings( "unchecked" )
		public Map<Result,Object> compile( BayesianNetwork bn, Map<Setting,?> settings ){
			Record                   eor = order( bn, settings );
			RCEngine              engine = RCEngine.create( bn.cpts(), eor.order );
			Set<Query>           queries = (Set<Query>) settings.get( Setting.queries );
			boolean             prE_only = (queries != null) && (queries.size() == 1) && queries.contains( Query.probabilityofevidence );
			CachingScheme         scheme = new CachingScheme( engine.dgraph(), prE_only );
			BigInteger              full = scheme.cacheEntriesFullCaching();
			long                   limit = full.longValue();
			if( settings.get( Setting.memoryentriesceiling    ) != null ){
				long        limitEntries = ((Number) settings.get( Setting.memoryentriesceiling    )).longValue();
				limit                    = Math.min( limit, limitEntries );
			}
			if( settings.get( Setting.memoryproportionceiling ) != null ){
				double             ratio = ((Number) settings.get( Setting.memoryproportionceiling )).doubleValue();
				if( ratio < 1.0 ){
					limit                = Math.min( limit, (long) (full.doubleValue() * ratio) );
				}
			}

			Map<Result,Object> results = new EnumMap<Result,Object>( Result.class );
			results.put( Result.jointengine,            engine );
			if( partial ){ results.put( Result.partialderivativeengine, engine ); }
			results.put( Result.eliminationorderrecord, eor );
			results.put( Result.cachingscheme,          scheme );

			if( limit < full.longValue() ){
				if( ! scheme.allocateGreedily(          limit ) ){ return null; }
				engine.allocateCaches( scheme.cachedNodes() );
				results.put( Result.cachesrequested,    limit );
			}
			else{ engine.fullCaching(); }

			return results;
		}
	},
	/** {@link il2.inf.jointree.Flavor#ssnormalized shenoy-shafer -- normalized } */
	ssnormalized              (        Flavor.ssnormalized ),
	/** {@link il2.inf.jointree.Flavor#ssnormalized shenoy-shafer -- normalized } */
	ssnormalizedmax           (        Flavor.ssnormalizedmax ),
	/** {@link il2.inf.jointree.Flavor#shenoyshafer shenoy-shafer } */
	shenoyshafer              (        Flavor.shenoyshafer ),
	/** {@link il2.inf.jointree.Flavor#hugin hugin } */
	hugin                     (        Flavor.hugin ),
	/** {@link il2.inf.jointree.Flavor#zeroconscioushugin "zero-conscious" hugin -- normalized } */
	zcnormalized              (        Flavor.zcnormalized ),
	/** {@link il2.inf.jointree.Flavor#zcnormalized "zero-conscious" hugin } */
	zeroconscioushugin        (        Flavor.zeroconscioushugin );

	/** kind of query that an inference engine might support */
	public enum Query{
		probabilityofevidence,
		marginal,
		mpe,
		map,
		partialderivative;

		public static final Set<Query> DEFAULT = EnumSet.allOf( Query.class );
	}

	/** elimination order heuristic */
	public enum EliminationOrderHeuristic{
		minfill{
			public Record order( BayesianNetwork bn, int repetitions ){
				return EliminationOrders.minFill( Arrays.asList( bn.cpts() ), repetitions, new Random(0) );
			}
		},
		minsize{
			public Record order( BayesianNetwork bn, int repetitions ){
				return EliminationOrders.minSize( Arrays.asList( bn.cpts() ) );
			}
		};

		abstract
			public Record order( BayesianNetwork bn, int repetitions );

		public static final EliminationOrderHeuristic DEFAULT = minfill;
	}

	/** induce a join tree from an elimination order
		@since 20081103 */
	public enum Order2JoinTree{
		traditional{
			public JT induce( BayesianNetwork bn, IntList order ){
				return EliminationOrders.traditionalJoinTree( bn, (Converter) null, order );
			}
		},
		bucketer{
			public JT induce( BayesianNetwork bn, IntList order ){
				return EliminationOrders.bucketerJoinTree( Arrays.asList( bn.cpts() ), order );
			}
		};

		abstract
			public JT induce( BayesianNetwork bn, IntList order );

		public static final Order2JoinTree DEFAULT = traditional;
	}

	/** keys to identify properties of {@link il2.inf.Algorithm.Setting Settings} */
	public enum Key{
		caption         (                        String .class ),
		info            (                        String .class ),
		floor           (                        Number .class ),
		ceiling         (                        Number .class ),
		defaultValue    (                        Object .class ),
		legal           (                         Class .class ),
		keystroke       (         javax.swing.KeyStroke .class ),
		advanced        (                       Boolean .class ),
		increment       (                        Number .class ),
		plural          (                        String .class ),
		notext          (                       Boolean .class ),
		actionlistener  ( java.awt.event.ActionListener .class );

		private         Key( Class<?> clazz ){ this.clazz = clazz; }
		public  final        Class<?> clazz;
	}

	/** control inference engine compilation */
	public enum Setting{
		eliminationorderheuristic   ( "elimination order heuristic",   "",   0, EliminationOrderHeuristic.values().length, EliminationOrderHeuristic.DEFAULT, null, null, false, 1, "heuristics",  false ),
		eliminationorderrepetitions ( "elimination order repetitions", "",   1,                                    0x1000,                               0x8, null, null,  true, 1, "repetitions", false ),
		order2jointree              ( "join tree induction method",    "",   0,            Order2JoinTree.values().length,            Order2JoinTree.DEFAULT, null, null, false, 1, "inductions",  false ),
		queries                     ( "supported queries",             "",   0,                     Query.values().length,                     Query.DEFAULT, null, null, false, 1, "queries",     false ),
		memoryproportionceiling     ( "memory proportion ceiling",     "", 0.0,                                       1.0,                               1.0, null, null, false, 0, "proportions",  true ),
		memoryentriesceiling        ( "memory entries ceiling",        "",  1L,                            Long.MAX_VALUE,                    Long.MAX_VALUE, null, null, false, 1, "ceilings",     true );

		private Setting( Object ... values ){
			Key[]                  keys = Key.values();
			EnumMap<Key,Object> enummap = new EnumMap<Key,Object>( Key.class );
			int i=0;
			for( Object value : values ){
				if( value != null ){ enummap.put( keys[i], value ); }
				++i;
			}
			if( enummap.get( Key.legal ) == null && enummap.get( Key.defaultValue ) != null ){ enummap.put( Key.legal, enummap.get( Key.defaultValue ).getClass() ); }
			this.properties = Collections.unmodifiableMap( enummap );
		}
		public Object get( Key key ){ return properties.get( key ); }
		final public Map<Key,Object> properties;

		static public Object value( Setting setting, Map<Setting,?> settings ){
			return settings.containsKey( setting ) ? settings.get( setting ) : setting.get( Key.defaultValue );
		}
	}

	/** the inference engine itself and other intermediate side-effects */
	public enum Result{
		jointengine,
		partialderivativeengine,
		eliminationorderrecord,
		jointree,
		cachingscheme,
		cachesrequested;
	}

	/** the flavors that support {@link #compile( il2.model.BayesianNetwork, java.util.Map ) compiling} a {@link il2.inf.PartialDerivativeEngine partial derivative engine} */
	static   public    Collection<Algorithm>   partials(){
		if( PARTIALS == null ){
			EnumSet<Algorithm> list = EnumSet.noneOf( Algorithm.class );
			for( Algorithm algorithm : values() ){ if( algorithm.partial ){ list.add( algorithm ); } }
			PARTIALS = Collections.unmodifiableSet( list );
		}
		return PARTIALS;
	}
	static   private   Collection<Algorithm>   PARTIALS;

	/** create (i.e. compile) a {@link il2.inf.JointEngine JointEngine} */
	public Map<Result,Object> compile( BayesianNetwork bn, Map<Setting,?> settings ){
		if( flavor == null ){ return null; }
		Record          eor  = order( bn, settings );
		Order2JoinTree o2jt  = (Order2JoinTree) Setting.value( Setting.order2jointree, settings );
		JT               jt  = o2jt.induce( bn, eor.order );
		JointEngine      je  = flavor.compile( bn, jt );

		Map<Result,Object> results = new EnumMap<Result,Object>( Result.class );
		results.put( Result.jointengine,            je  );
		if( partial ){ results.put( Result.partialderivativeengine, je ); }
		results.put( Result.eliminationorderrecord, eor );
		results.put( Result.jointree,               jt  );
		return results;
	}

	/** elimination order  */
	static public Record             order(       BayesianNetwork bn, Map<Setting,?> ss ){
		int      reps = ((Integer)                    Setting.value( Setting.eliminationorderrepetitions, ss )).intValue();
		Record  order = ((EliminationOrderHeuristic)  Setting.value( Setting.eliminationorderheuristic,   ss )).order( bn, reps );
		return  order;
	}

	/** if you've got a {@link il2.inf.JointEngine JointEngine} {@link java.lang.Class Class} but you need an {@link il2.inf.Algorithm Algorithm} */
	static public Algorithm forClass( Class<? extends JointEngine> clazz ){
		for( Algorithm alg : values() ){
			if( alg.clazz.isAssignableFrom( clazz ) ){ return alg; }
		}
		return null;
	}

	/** for join tree only */
	private Algorithm( Flavor flavor ){
		this.flavor     = flavor;
		this.clazz      = flavor.clazz;
		this.partial    = flavor == null ? false : flavor.partial;
	}

	/** for everything other than join tree */
	private Algorithm( Class<? extends JointEngine> clazz ){
		this.flavor     = null;
		this.clazz      = clazz;
		this.partial    = PartialDerivativeEngine .class .isAssignableFrom( clazz );
	}

	/** join tree flavor */
	public   final   Flavor                               flavor;
	/** supports {@link #compile( il2.model.BayesianNetwork, java.util.Map ) compiling} a {@link il2.inf.PartialDerivativeEngine partial derivative engine} ?? */
	public   final   boolean                              partial;
	/** engine class */
	public   final   Class<? extends JointEngine>         clazz;

	/** test/debug */
	static public void main( String[] args ){
		out.append( "Testing   " ).append( Algorithm.class.getName() ).append( "\n" );
		out.append( "values?   " ).append( Arrays.toString( Algorithm.values() ) ).append( "\n" );
		out.append( "partials? " ).append( Algorithm.partials().toString() ).append( "\n" );
	}
}
