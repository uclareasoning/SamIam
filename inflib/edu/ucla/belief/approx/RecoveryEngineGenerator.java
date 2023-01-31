package edu.ucla.belief.approx;

import        edu.ucla.util.Setting;
import        edu.ucla.util.Setting.Settings;
import        edu.ucla.util.SettingsImpl;
import        edu.ucla.belief.*;
import        edu.ucla.belief.io.PropertySuperintendent;

import        java.util.Arrays;
import        java.util.Collection;
import        java.io.Serializable;

/** @author keith cascio
    @since  20091207 */
public class RecoveryEngineGenerator extends ApproxEngineGenerator<RecoverySetting> implements
  Serializable
{
	static public final long serialVersionUID = 6944530267470113528L;

	static public final String
	  STR_DISPLAY             =     "edbp",
	  STR_DISPLAY_DEBUG       =     "edbp (edu.ucla.belief.approx)",
	  KEY                     =     "recoveryenginegenerator6944530267470113528l",
	  KEY_TEAM                =     KEY + ".team",
	  KEY_SUBALGORITHM        =     KEY + ".subalgorithm";

	public   Class<RecoverySetting>  clazz(){ return RecoverySetting.class; }
	public        Object   keySubalgorithm(){ return KEY_SUBALGORITHM; }
	public        Object           keyTeam(){ return KEY_TEAM;         }

	public static Object      getKeyStatic(){ return KEY; }

	public        Object      getKey(){       return getKeyStatic(); }

	public        String    getDisplayName(){
		return FLAG_DEBUG_DISPLAY_NAMES ? STR_DISPLAY_DEBUG : STR_DISPLAY;
	}

	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn, Settings<RecoverySetting> settings ) throws Throwable{
		return new RecoveryInferenceEngine( bn, new il2.bridge.Converter(), settings, team( (PropertySuperintendent) bn ), dyn );
	}

	public Settings<RecoverySetting> getSettings( PropertySuperintendent bn ){
		Settings<RecoverySetting> ret = super.getSettings( bn );

		long start = System.currentTimeMillis();
		il2.model.BayesianNetwork bayesiannetwork = null;
		int ceiling = 0;
		try{
			if(      bn instanceof             BeliefNetwork ){
				BeliefNetwork beliefnetwork = (BeliefNetwork) bn;
				if( !         beliefnetwork.isEmpty()        ){ bayesiannetwork = new il2.bridge.Converter().convert( beliefnetwork ); } }
			else if( bn instanceof il2.model.BayesianNetwork ){ bayesiannetwork = (il2.model.BayesianNetwork) bn; }

			if( bayesiannetwork != null ){ ceiling = il2.inf.edgedeletion.EDRecovery.countRecoverable( bayesiannetwork ); }
		}catch( Throwable thrown ){
			System.err.println( "warning: RecoveryEngineGenerator.getSettings() caught " + thrown );
		}
		ret.put( RecoverySetting.recovery, edu.ucla.util.PropertyKey.ceiling, ceiling );
		if( Definitions.DEBUG ){ Definitions.STREAM_VERBOSE.println( "countRecoverable() elapsed: " + (System.currentTimeMillis() - start) ); }

		return ret;
	}

	public static Settings<RecoverySetting> getSettings( PropertySuperintendent bn, boolean construct ){
		return ApproxEngineGenerator.getSettings( getKeyStatic(), RecoverySetting.class, bn, construct );
	}

	public Collection<Class> getClassDependencies(){
		return Arrays.asList( new Class[]{
	           RecoveryEngineGenerator.class,
	                   RecoverySetting.class,
	                          Settings.class,
	                           Setting.class,
	                      SettingsImpl.class,
	           RecoveryInferenceEngine.class,
	              java.math.BigInteger.class } );
	}
}
