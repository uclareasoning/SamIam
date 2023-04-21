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
    @since  20080225 */
public class EdgeDeletionEngineGenerator extends ApproxEngineGenerator<EdgeDeletionBeliefPropagationSetting> implements
//PropertySuperintendent,
  Serializable
{
	static public final long serialVersionUID = -3934402333958503270L;

	static public final String
	  STR_DISPLAY             =     "edbp (testing)",
	  STR_DISPLAY_DEBUG       =     "edbp (edu.ucla.belief.approx)",
	  KEY                     =     "edgedeletionenginegenerator3934402333958503270L",
	  KEY_TEAM                =     KEY + ".team",
	  KEY_SUBALGORITHM        =     KEY + ".subalgorithm";

	public   Class<EdgeDeletionBeliefPropagationSetting>  clazz(){ return EdgeDeletionBeliefPropagationSetting.class; }
	public        Object   keySubalgorithm(){ return KEY_SUBALGORITHM; }
	public        Object           keyTeam(){ return KEY_TEAM;         }

	public static Object      getKeyStatic(){ return KEY; }

	public        Object      getKey(){       return getKeyStatic(); }

	public        String    getDisplayName(){
		return FLAG_DEBUG_DISPLAY_NAMES ? STR_DISPLAY_DEBUG : STR_DISPLAY;
	}

	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn, Settings<EdgeDeletionBeliefPropagationSetting> settings ) throws Throwable{
		return new EdgeDeletionInferenceEngine( bn, Macros.convert( bn, settings, team( (PropertySuperintendent) bn ) ), settings, dyn );
	}

	public static Settings<EdgeDeletionBeliefPropagationSetting> getSettings( PropertySuperintendent bn, boolean construct ){
		return ApproxEngineGenerator.getSettings( getKeyStatic(), EdgeDeletionBeliefPropagationSetting.class, bn, construct );
	}

	public Collection<Class> getClassDependencies(){
		return Arrays.asList( new Class[]{
	           EdgeDeletionEngineGenerator.class,
	  EdgeDeletionBeliefPropagationSetting.class,
	                              Settings.class,
	                               Setting.class,
	                          SettingsImpl.class,
	           EdgeDeletionInferenceEngine.class,
	                  java.math.BigInteger.class,
	                                Macros.class } );
	}
}
