package edu.ucla.belief.approx;

import edu.ucla.belief.*;
import edu.ucla.belief.io.PropertySuperintendent;

import java.util.Map;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.JMenu;
import java.awt.Container;
import java.io.Serializable;

/** @author Arthur Choi
    @since 050505 */
public class PropagationEngineGenerator extends Dynamator implements Serializable
{
	static final long serialVersionUID = 1791944048146838126L;

	public static Object getKeyStatic(){
		return "propagationenginegenerator1791944048146838126L";
	}

	public Object getKey(){
		return getKeyStatic();
	}

	public String getDisplayName(){
		return FLAG_DEBUG_DISPLAY_NAMES ?
			"loopy bp (edu.ucla.belief.approx)" : "loopy belief propagation";
	}

	/** If you change the return value, you must consider
		changing the return value of PropagationInferenceEngineImpl.probabilitySupported()

		@since 20051017
		@see edu.ucla.belief.Dynamator#probabilitySupported()
		@see edu.ucla.belief.InferenceEngine#probabilitySupported()
		@see PropagationInferenceEngineImpl#probabilitySupported()
	*/
	public boolean probabilitySupported(){
		return true;
	}

	public boolean isEditable() { return false; }
	public Commitable getEditComponent( Container cont ) { return null; }
	public void commitEditComponent() {}
	public JMenu getJMenu() { return null; }
	public Dynamator getCanonicalDynamator() { return this; }

	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ) throws Throwable
	{
		BeliefPropagationSettings settings = getSettings( choosePropertySuperintendent( (PropertySuperintendent) bn ) );
		return new PropagationInferenceEngineImpl( bn, settings, dyn );
	}

	public void killState( PropertySuperintendent bn ){
		BeliefPropagationSettings settings = getSettings( choosePropertySuperintendent( bn ) );
		settings.killState();
	}

	/** @since 20081029 */
	public Object retrieveState( PropertySuperintendent bn ){
		return getSettings( bn );
	}

	public static BeliefPropagationSettings getSettings( PropertySuperintendent bn ){
		return getSettings( bn, true );
	}

	public static BeliefPropagationSettings getSettings( PropertySuperintendent bn, boolean construct )
	{
		Map properties = bn.getProperties();
		Object value = properties.get( getKeyStatic() );
		BeliefPropagationSettings ret = null;
		if( value instanceof BeliefPropagationSettings )
			ret = (BeliefPropagationSettings)value;
		else if( construct )
		{
			ret = new BeliefPropagationSettings();
			properties.put( getKeyStatic(), ret );
		}

		return ret;
	}

	/** @since 20100108 */
	public Dynamator writeJavaCodeSettingsManipulation( BeliefNetwork beliefnetwork, boolean withComments, java.io.PrintStream out ){
		if( withComments ){ out.println( "    /* Edit settings. */" ); }
		edu.ucla.belief.approx.BeliefPropagationSettings settings = this.getSettings( (PropertySuperintendent) beliefnetwork );
		out.println( "    "+settings.getClass().getName()+" settings = dynamator.getSettings( (PropertySuperintendent) bn );" );
		out.println( "    settings.setTimeoutMillis( "        + settings.getTimeoutMillis()        + " );" );
		out.println( "    settings.setMaxIterations( "        + settings.getMaxIterations()        + " );" );
		String ss = null;
		try{   ss = MessagePassingScheduler.asJavaCode(         settings.getScheduler()                    ); }catch( Throwable thrown ){ thrown.printStackTrace( System.err ); }
		if(    ss != null ){ out.println( "    settings.setScheduler( " +                       ss + " );" ); }
		out.println( "    settings.setConvergenceThreshold( " + settings.getConvergenceThreshold() + " );" );
		out.println();
		return this;
	}

	public Collection getClassDependencies(){
		return Arrays.asList( new Class[] {
			PropagationEngineGenerator.class,
			BeliefPropagationSettings.class,
			PropagationInferenceEngineImpl.class,
			java.math.BigInteger.class } );
	}
}
