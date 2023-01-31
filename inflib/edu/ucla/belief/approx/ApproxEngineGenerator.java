package edu.ucla.belief.approx;

import        edu.ucla.util. Setting;
import        edu.ucla.util. Setting.Settings;
import        edu.ucla.util. SettingsImpl;
import        edu.ucla.belief.*;
import        edu.ucla.belief.inference.SSEngineGenerator;
import        edu.ucla.belief.io. PropertySuperintendent;
import        edu.ucla.belief. CrouchingTiger .DynamatorImpl;

import        java.util.Map;
import        java.util.HashMap;
import        java.util.EnumMap;
import        java.util.Collection;
import        java.io.Serializable;
import        java.awt.Container;
import        javax.swing.JMenu;

/** @author keith cascio
    @since  20091207 */
public abstract class ApproxEngineGenerator<E extends Enum<E> & Setting> extends Dynamator implements Serializable{
	abstract public Class<E>                                  clazz();
	abstract public Object                          keySubalgorithm();
	abstract public Object                                  keyTeam();
	abstract public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn, Settings<E> settings ) throws Throwable;
	abstract public Collection<Class>          getClassDependencies();

	/** @since 20081030 */
	@SuppressWarnings( "unchecked" )
	public PropertySuperintendent getSubalgorithmPropertySuperintendent( PropertySuperintendent arbitrary ){
	    Map             properties =  arbitrary.getProperties();
		PropertySuperintendent ret = (PropertySuperintendent) properties.get( keySubalgorithm() );
		if( ret == null ){ properties.put( keySubalgorithm(), ret = new Properties() ); }
		return ret;
	}

	/** @since 20081030 */
	static public class Properties implements PropertySuperintendent{
		public  Map<Object,Object> getProperties(){ return myProperties; }
		private Map<Object,Object> myProperties = new HashMap<Object,Object>( 0x10 );
	}

	/** @since 20081030 */
	@SuppressWarnings( "unchecked" )
	public  Map<DynamatorImpl,Dynamator> team( PropertySuperintendent arbitrary ){
		Map                    properties =  arbitrary.getProperties();
		Map<DynamatorImpl,Dynamator> team = (Map<DynamatorImpl,Dynamator>) properties.get( keyTeam() );
		if(    team == null ){ properties.put( keyTeam(), team = new EnumMap<DynamatorImpl,Dynamator>( DynamatorImpl.class ) ); }
		return team;
	}

	public boolean              isEditable(){ return false; }
	public Commitable     getEditComponent( Container cont ){ return null; }
	public void        commitEditComponent(){}
	public JMenu                  getJMenu(){ return null; }
	public Dynamator getCanonicalDynamator(){ return this; }

	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ) throws Throwable{
		return manufactureInferenceEngineOrDie( bn, dyn, getSettings( choosePropertySuperintendent( (PropertySuperintendent) bn ) ) );
	}

	public void killState( PropertySuperintendent bn ){
		Settings<E> settings = getSettings( choosePropertySuperintendent( bn ) );
		if( settings != null ){ settings.killState(); }
		try{ if( ssenginegenerator != null ){ ssenginegenerator.killState( bn ); } }
		catch( Throwable thrown ){
			System.err.println( "warning: ApproxEngineGenerator.killState() caught " + thrown );
		}
	}

	/** @since 20081029 */
	public Object retrieveState( PropertySuperintendent bn ){
		return getSettings( bn );
	}

	public Settings<E> getSettings( PropertySuperintendent bn ){
		return ApproxEngineGenerator.getSettings( getKey(), clazz(), bn, true );
	}

	@SuppressWarnings( "unchecked" )
	public static <E extends Enum<E> & Setting> Settings<E> getSettings( Object key, Class<E> clazz, PropertySuperintendent bn, boolean construct ){
		Map<Object,Object> properties = Macros.properties( bn );
		Object             value      = properties.get( key );
		Settings<E>        ret        = null;
		if( value instanceof Settings ){ ret = (Settings<E>) value; }
		else if( construct ){ properties.put( key, ret = new SettingsImpl<E>( clazz ) ); }
		return ret;
	}

	/** @since 20100108 */
	public static String toJavaCode( Object obj ){
		if(      obj instanceof Collection ){ return null; }
		else if( obj instanceof Enum       ){ Enum eyum = (Enum) obj;
			                                  return eyum.getDeclaringClass().getName().replace('$','.') + "." + eyum.name(); }
		else if( obj instanceof String     ){ return "\"" + obj + "\""; }
		else return obj.toString();
	}

	/** @since 20100108 */
	public Dynamator writeJavaCodeSettingsManipulation( BeliefNetwork beliefnetwork, boolean withComments, java.io.PrintStream out ){
		out.println(                     "    /* java5 required */" );
		if( withComments ){ out.println( "    /* Edit settings. */" ); }
		Settings<E> settings = this.getSettings( (PropertySuperintendent) beliefnetwork );
		String nameEnum = settings.clazz().getName();
		out.println( "    Settings<"+nameEnum+"> settings = dynamator.getSettings( (PropertySuperintendent) bn );" );
		String javacode = null;
		for( E key : settings.clazz().getEnumConstants() ){
			if( (javacode = toJavaCode( settings.get( key ) )) != null ){
				out.println( "    settings.put( "+nameEnum+"."+key.name()+", "+javacode+" );" );
			}
		}
		out.println();
		return this;
	}

	/** @since 20091218 */
	public SSEngineGenerator getExactEngineGenerator(){
		if( ssenginegenerator == null ){ ssenginegenerator = new SSEngineGenerator(); }
		return ssenginegenerator;
	}

	transient private     SSEngineGenerator          ssenginegenerator;
}
