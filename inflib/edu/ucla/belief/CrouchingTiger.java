package edu.ucla.belief;

import        edu.ucla.belief.inference.                 JoinTreeSettings;
import        edu.ucla.belief.inference.                 RCSettings;
import        edu.ucla.belief.inference.                 RCInfo;
import        edu.ucla.belief.io.                        PropertySuperintendent;

import        il2.inf.                                   Algorithm;
import        il2.inf.                        Algorithm .Setting;
import        il2.inf.                        Algorithm .EliminationOrderHeuristic;
import        il2.inf.                        Algorithm .Query;
import        il2.inf.                        Algorithm .Key;
import        il2.inf.                        Algorithm .Result;
import        il2.bridge.                                Converter;

import        java.util.                                 Map;
import        java.util.                                 EnumMap;
import        java.util.                                 EnumSet;
//{superfluous} import        java.util.                                 Set;
import        java.util.                                 Collection;
import        java.util.                                 Collections;
//{superfluous} import        java.util.                                 Arrays;
import static java.lang.System .out;
import static java.lang.System .err;

/** Java 5 ("Tiger") extensions for IL1 inflib.
	@author keith cascio
	@since  20081029 */
public class CrouchingTiger{
	static public Map<Setting,Object> toIL2Settings(                                      Object  settings ){
		if(      settings instanceof JoinTreeSettings ){ return toIL2Settings( (JoinTreeSettings) settings ); }
		else if( settings instanceof       RCSettings ){ return toIL2Settings(       (RCSettings) settings ); }
		else return null;
	}

	static public Map<Setting,Object> toIL2Settings( EliminationHeuristic eh ){
		EliminationOrderHeuristic eoh  = null;
		int                       reps = -1;
		if(      eh == EliminationHeuristic.MIN_FILL ){ eoh = EliminationOrderHeuristic.minfill;
			reps =     EliminationHeuristic.MIN_FILL.getRepetitions();
		}
		else if( eh == EliminationHeuristic.MIN_SIZE ){ eoh = EliminationOrderHeuristic.minsize; }

		if( eoh == null ){ eoh = EliminationOrderHeuristic.DEFAULT; }

		Map<Setting,Object> settings = new EnumMap<Setting,Object>( Setting.class );
		if( true     ){ settings.put( Setting.eliminationorderheuristic,    eoh ); }
		if( reps > 0 ){ settings.put( Setting.eliminationorderrepetitions, reps ); }
		return settings;
	}

	static public Map<Setting,Object> toIL2Settings( JoinTreeSettings jts ){
		return toIL2Settings( jts.getEliminationHeuristic() );
	}

	static public Map<Setting,Object> toIL2Settings( RCSettings       rcs ){
		Map<Setting,Object> settings = toIL2Settings( rcs.getEliminationHeuristic() );

		if( rcs.getPrEOnly() ){
			settings.put( Setting.queries, EnumSet.of( Query.probabilityofevidence ) );
		}

		double       prop = rcs.getUserMemoryProportion();
		Setting      ceil = Setting.memoryproportionceiling;
		double     c_floo = ((Number) ceil.get( Key.floor   )).doubleValue();
		double     c_ceil = ((Number) ceil.get( Key.ceiling )).doubleValue();
		if( c_floo <= prop && prop <= c_ceil ){
			settings.put( ceil, prop );
		}else{
			err.append( "warning: User Memory Proportion (ceiling) " )
			.append( Double.toString( prop ) )
			.append( " is out of range [ " )
			.append( Double.toString( c_floo ) )
			.append( ", " )
			.append( Double.toString( c_ceil ) )
			.append( " ]\n" );
		}

		try{
			RCInfo       info = rcs.getInfo();
			if(          info != null ){
				long      entries = info.allocatedCacheEntries().longValue();
				Setting c_entries = Setting.memoryentriesceiling;
				long       e_floo = ((Number) c_entries.get( Key.floor   )).longValue();
				long       e_ceil = ((Number) c_entries.get( Key.ceiling )).longValue();
				if( e_floo <= entries && entries <= e_ceil ){
					settings.put( c_entries, entries );
				}
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: CrouchingTiger.toIL2Settings( RCSettings ) caught " + thrown );
		}

		return settings;
	}

	static public Map<Result,Object> toIL2Engine( Dynamator dyn, BeliefNetwork bn, Converter conv ){
		if( ! (bn instanceof PropertySuperintendent) ){ return null; }

		Algorithm          algorithm = (Algorithm) dyn.asIL2Algorithm();
		if(                algorithm         == null ){ return null; }
		Object                 state = dyn.retrieveState( (PropertySuperintendent) bn );
		if(                    state         == null ){ return null; }
		Map<Setting,Object> settings =     toIL2Settings( state );
		if(                 settings         == null ){ return null; }

		return algorithm.compile( conv.convert( bn ), settings );
	}

	public enum DynamatorImpl{
		edbp            ( edu.ucla.belief.approx.                EdgeDeletionEngineGenerator .class ),
		loopybp         ( edu.ucla.belief.approx.                PropagationEngineGenerator  .class ),
		random          ( edu.ucla.belief.inference.             RandomEngineGenerator       .class ),
		rcil1           ( edu.ucla.belief.recursiveconditioning. RCEngineGenerator           .class ),
		rcil2           ( edu.ucla.belief.inference.             RCEngineGenerator           .class ),
		shenoyshaferil1 ( edu.ucla.belief.inference.             JEngineGenerator            .class ),
		shenoyshaferil2 ( edu.ucla.belief.inference.             SSEngineGenerator           .class ),
		hugin           ( edu.ucla.belief.inference.             HuginEngineGenerator        .class ),
		zchugin         ( edu.ucla.belief.inference.             ZCEngineGenerator           .class );

		private DynamatorImpl( Class<? extends Dynamator>     clazz ){
			this.clazz = clazz;
		}
		public     final       Class<? extends Dynamator>     clazz;
		private                Dynamator                      prototype;
		private                Algorithm                      algorithm;
		private                String                         displayname;
		private                boolean                        flag_prototype, flag_algorithm, flag_displayname;

		public Dynamator enlist( Map<DynamatorImpl,Dynamator> team ){
			Dynamator dyn = team.get( this );
			if( dyn == null ){ team.put( this, dyn = create() ); }
			return dyn;
		}

		public InferenceEngine    compileIL1( BeliefNetwork bn, Map<DynamatorImpl,Dynamator> team ){
			return enlist( team ).manufactureInferenceEngine( bn );
		}

		public Map<Result,Object> compileIL2( BeliefNetwork bn, Converter conv, Map<DynamatorImpl,Dynamator> team ){
			return toIL2Engine( enlist( team ), bn, conv );
		}

		static public Map<DynamatorImpl,Dynamator> create( DynamatorImpl ... targets ){
			Map<DynamatorImpl,Dynamator> team = new EnumMap<DynamatorImpl,Dynamator>( DynamatorImpl.class );
			for( DynamatorImpl di : targets ){
				team.put( di, di.create() );
			}
			return team;
		}

		public Dynamator create(){
			try{
				return clazz.getConstructor().newInstance();
			}catch( Throwable thrown ){
				err.append( "warning: " )
				.append( getClass().getDeclaringClass().getName() )
				.append( ".create() caught " )
				.append( thrown.toString() )
				.append( "\n" );
			}
			return null;
		}

		private Dynamator prototype(){
			if( flag_prototype ){ return prototype; }
			if( prototype == null ){
				try{
					prototype = create();
				}catch( Throwable thrown ){
					err.append( "warning: " )
					.append( getClass().getDeclaringClass().getName() )
					.append( ".prototype() caught " )
					.append( thrown.toString() )
					.append( "\n" );
				}
				flag_prototype = true;
			}
			return prototype;
		}

		public Algorithm algorithm(){
			if( flag_algorithm ){ return algorithm; }
			if( algorithm == null ){
				try{
					algorithm = (Algorithm) prototype().asIL2Algorithm();
				}catch( Throwable thrown ){
					err.append( "warning: " )
					.append( getClass().getDeclaringClass().getName() )
					.append( ".algorithm() caught " )
					.append( thrown.toString() )
					.append( "\n" );
				}
				flag_algorithm = true;
			}
			return algorithm;
		}

		public String displayName(){
			if( flag_displayname ){ return displayname; }
			if( displayname == null ){
				try{
					displayname = prototype().getDisplayName();
				}catch( Throwable thrown ){
					err.append( "warning: " )
					.append( getClass().getDeclaringClass().getName() )
					.append( ".displayName() caught " )
					.append( thrown.toString() )
					.append( "\n" );
				}
				flag_displayname = true;
			}
			return displayname;
		}

		public String toString(){
			return this.displayName();
		}

		public Map<Setting,Object> toIL2Settings( BeliefNetwork bn, Map<DynamatorImpl,Dynamator> team ){
			if( ! (bn instanceof PropertySuperintendent) ){ return null; }

		  /*if( prototype == null ){
				try{
					prototype = create();
				}catch( Throwable thrown ){
					err.append( "warning: " )
					.append( getClass().getDeclaringClass().getName() )
					.append( ".toIL2Settings() caught " )
					.append( thrown.toString() )
					.append( "\n" );
				}
			}*/

			Object                 state = enlist( team ).retrieveState( (PropertySuperintendent) bn );
			if(                    state         == null ){ return null; }
			Map<Setting,Object> settings = CrouchingTiger.toIL2Settings( state );
			if(                 settings         == null ){ return null; }

			return              settings;
		}

		static   public    Collection<DynamatorImpl>   il2Partials(){
			if( IL2PARTIALS == null ){
				EnumSet<DynamatorImpl> list = EnumSet.noneOf( DynamatorImpl.class );
				for( DynamatorImpl di : il2s() ){ if( di.algorithm().partial ){ list.add( di ); } }
				IL2PARTIALS = Collections.unmodifiableSet( list );
			}
			return IL2PARTIALS;
		}

		static   public    Collection<DynamatorImpl>   il2s(){
			if( IL2S == null ){
				EnumSet<DynamatorImpl> list = EnumSet.noneOf( DynamatorImpl.class );
				for( DynamatorImpl di : values() ){ if( di.algorithm() != null ){ list.add( di ); } }
				IL2S = Collections.unmodifiableSet( list );
			}
			return IL2S;
		}

		static   private   Collection<DynamatorImpl>
		  IL2PARTIALS,
		  IL2S;
	}
}
