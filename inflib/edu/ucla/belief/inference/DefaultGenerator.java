package edu.ucla.belief.inference;

import edu.ucla.belief.*;
//{superfluous} import edu.ucla.belief.EliminationHeuristic;
import edu.ucla.belief.io.PropertySuperintendent;

import javax.swing.JComponent;
import javax.swing.JMenu;
import java.awt.Container;
import java.util.*;

/**
	@author Keith Cascio
	@since 062403
*/
public abstract class DefaultGenerator extends Dynamator
{
	public static final String STR_EXCEPTION_ILLEGAL_JOINTREE = "join tree argument illegal type";

	abstract public InferenceEngine manufactureInferenceEngine( BeliefNetwork bn, DefaultGenerator dyn );
	abstract protected InferenceEngine makeInferenceEngine( BeliefNetwork bn, JoinTreeSettings settings );
	abstract protected il2.inf.structure.JTUnifier makeJoinTree( BeliefNetwork bn, JoinTreeSettings settings );
	abstract public InferenceEngine prunedEngine(BeliefNetwork bn,Collection queryVariables, Map evidence);

	//public boolean isEditable() { return false; }
	//public JComponent getEditComponent( Container cont ) { return null; }
	//public void commitEditComponent() {}
	//public JMenu getJMenu() { return null; }
	public Dynamator getCanonicalDynamator() { return this; }

	/** @since 20060201 */
	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ) throws Throwable{
		return makeInferenceEngine( bn, (DefaultGenerator) (dyn == null ? null : dyn.getCanonicalDynamator()) );
	}

	public InferenceEngine manufactureInferenceEngine( BeliefNetwork bn, JoinTreeSettings settings )
	{
		InferenceEngine ie = makeInferenceEngine( bn, settings );
		settings.setEngine( (JoinTreeInferenceEngine) ie.canonical() );
		return ie;
	}

	/** @since 012904 */
	public il2.inf.structure.JTUnifier manufactureJoinTree( BeliefNetwork bn, JoinTreeSettings settings )
	{
		il2.inf.structure.JTUnifier jt = makeJoinTree( bn, settings );
		settings.setJoinTree( jt );
		return jt;
	}

	private InferenceEngine makeInferenceEngine( BeliefNetwork bn, DefaultGenerator dyn )
	{
		//System.out.println( "DefaultGenerator.makeInferenceEngine()" );
		InferenceEngine ie = null;
		JoinTreeSettings actual = getSettings( (PropertySuperintendent)bn );
		ie = actual.getEngine();
		if( ie == null || !ie.getValid() )
		{
			ie = manufactureInferenceEngine( bn, dyn );
			actual.setEngine( (JoinTreeInferenceEngine) ie.canonical() );
		}
		return ie;
	}

	/** @since 121003 */
	public void killState( PropertySuperintendent bn )
	{
		killInferenceEngineAndJoinTree( bn );
	}

	/** @since 101403 */
	public void killInferenceEngineAndJoinTree( PropertySuperintendent bn )
	{
		JoinTreeSettings settings = getSettings( bn, false );
		if( settings != null ){
			settings.setJoinTree( null );
			settings.setEngine( null );
		}
	}

	/** @since 20081029 */
	public Object retrieveState( PropertySuperintendent bn ){
		return getSettings( bn );
	}

	public JoinTreeSettings getSettings( PropertySuperintendent bn )
	{
		return getSettings( bn, true );
	}

	public JoinTreeSettings getSettings( PropertySuperintendent bn, boolean construct )
	{
		Map properties = bn.getProperties();
		Object value = properties.get( getKey() );
		JoinTreeSettings ret = null;
		if( value instanceof JoinTreeSettings ) ret = (JoinTreeSettings)value;
		else if( construct )
		{
			ret = new JoinTreeSettings();
			properties.put( getKey(), ret );
		}

		return ret;
	}

	/** @since 20100108 */
	public Dynamator writeJavaCodeSettingsManipulation( BeliefNetwork beliefnetwork, boolean withComments, java.io.PrintStream out ){
		if( withComments ){ out.println( "    /* Edit settings. */" ); }
		edu.ucla.belief.inference.JoinTreeSettings settings = this.getSettings( (PropertySuperintendent)beliefnetwork, true );
		out.println( "    "+settings.getClass().getName()+" settings = dynamator.getSettings( (PropertySuperintendent)bn, true );" );
		if( withComments ){
			out.println( "    /*\n      Define the elimination order heuristic used to create the join tree, one of:" );
			out.print( "        " );
			edu.ucla.util.code.MAPCoder.arrayToCodePlusString( EliminationHeuristic.ARRAY, out );
			out.println( "\n    */" );
		}
		out.println( "    settings.setEliminationHeuristic( EliminationHeuristic."+settings.getEliminationHeuristic().getJavaCodeName()+" );" );
		out.println();
		return this;
	}
}
