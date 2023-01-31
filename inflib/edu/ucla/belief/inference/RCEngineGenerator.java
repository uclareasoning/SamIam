package edu.ucla.belief.inference;

import edu.ucla.belief.*;
//{superfluous} import edu.ucla.belief.dtree.Dtree;
import edu.ucla.belief.io.PropertySuperintendent;

import java.util.Map;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JMenu;
import java.awt.Container;
import java.io.Serializable;

/** @author Keith Cascio
    @since  20031029 */
public class RCEngineGenerator extends Dynamator implements Serializable
{
	static final long serialVersionUID = -1317510038576835557L;

	public String getDisplayName()
	{
		return FLAG_DEBUG_DISPLAY_NAMES ? "rc (il2.inf.rc)" : "recursive conditioning";//"rc (exp)";
	}

  //public boolean    isEditable() { return false; }
  //public JComponent getEditComponent( Container cont ) { return null; }
  //public void       commitEditComponent() {}
  //public JMenu      getJMenu() { return null; }
	public Dynamator getCanonicalDynamator() { return this; }

	public static final String STR_GENERIC_ALLOCATION_ERROR = "Cannot allocate memory.";

	/** @since 20050421 */
	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ) throws Throwable
	{
		RCSettings rcsettings = getSettings( choosePropertySuperintendent( (PropertySuperintendent) bn ) );
		if( rcsettings.validateAllocation( bn ) ) return new RCEngine( rcsettings, dyn );
		else return (InferenceEngine)null;
	}

	/** @since 20031210 */
	public void killState( PropertySuperintendent bn )
	{
		killRCInfo( bn );
	}

	/** @since 20031210 */
	public void killRCInfo( PropertySuperintendent bn )
	{
		RCSettings settings = getSettings( choosePropertySuperintendent( bn ), false );
		if( settings != null ) settings.setInfo( null );
	}

	/** @since 20081029 */
	public Object retrieveState( PropertySuperintendent bn ){
		return getSettings( bn );
	}

	public static RCSettings getSettings( PropertySuperintendent bn )
	{
		return getSettings( bn, true );
	}

	public static RCSettings getSettings( PropertySuperintendent bn, boolean construct )
	{
		Map properties = bn.getProperties();
		Object value = properties.get( getKeyStatic() );
		RCSettings ret = null;
		if( value instanceof RCSettings ) ret = (RCSettings)value;
		else if( construct )
		{
			ret = new RCSettings();
			properties.put( getKeyStatic(), ret );
		}

		return ret;
	}

	/** @since 20100108 */
	public Dynamator writeJavaCodeSettingsManipulation( BeliefNetwork beliefnetwork, boolean withComments, java.io.PrintStream out ){
		if( withComments ){ out.println( "    /* Edit settings. */" ); }
		edu.ucla.belief.inference.RCSettings settings = edu.ucla.belief.inference.RCEngineGenerator.getSettings( (PropertySuperintendent)beliefnetwork );
		out.println( "    "+settings.getClass().getName()+" settings = dynamator.getSettings( (PropertySuperintendent)bn );" );
		if( withComments ){
			out.println( "    /*\n      Define the elimination heuristic used to create the dtree, one of:" );
			out.print( "        " );
			edu.ucla.util.code.MAPCoder.arrayToCodePlusString( EliminationHeuristic.ARRAY, out );
			out.println( "\n    */" );
		}
		out.println( "    settings.setEliminationHeuristic( EliminationHeuristic."+settings.getEliminationHeuristic().getJavaCodeName()+" );" );
		if( withComments ){ out.println( "    /* Do "+((settings.getPrEOnly())?"":"not ")+"create an InferenceEngine optimized for only Pr(e). */" ); }
		out.println( "    settings.setPrEOnly(              "+settings.getPrEOnly()+" );" );
		if( withComments ){ out.println( "    /* Set the fraction of full memory to use. */" ); }
		out.println( "    settings.setUserMemoryProportion( (double)"+settings.getUserMemoryProportion()+" );" );
		if( withComments ){ out.println( "    /* Create the cache allocation (very important). */" ); }
		out.println( "    try{" );
		out.println( "      settings.validateAllocation( bn );" );
		out.println( "    }catch( Throwable throwable ){" );
		out.println( "       System.err.println( \"Error, failed to validate cache allocation: \" + throwable );" );
		out.println( "       return;" );
		out.println( "    }" );
		out.println();
		if( withComments ){ out.println( "    /* Characterize the cache allocation and estimated run time. */" ); }
		out.println( "    double     actualProportion          = settings.getActualMemoryProportion();" );
		out.println( "    RCInfo     info                      = settings.getInfo();" );
		out.println( "    BigInteger cacheEntriesFullCaching   = info.cacheEntriesFullCaching();" );
		out.println( "    BigInteger allocatedCacheEntries     = info.allocatedCacheEntries();" );
		out.println( "    double     recursiveCallsFullCaching = info.recursiveCallsFullCaching();" );
		out.println( "    double     recursiveCalls            = info.recursiveCalls();" );
		out.println();
		return this;
	}

	public static Object getKeyStatic()
	{
		return "rcenginegenerator1317510038576835557L";
	}

	public Object getKey()
	{
		return getKeyStatic();
	}

	/** @since 20040520 */
	public Collection getClassDependencies(){
		return DEPENDENCIES;
	}

	static public final List DEPENDENCIES = Collections.unmodifiableList( Arrays.asList( new Class[]{
	  RCEngineGenerator       .class,
	  RCSettings              .class,
	  RCInfo                  .class,
	  RCEngine                .class,
	  java.math .BigInteger   .class,
	  il2.inf.rc  .RCEngine   .class } ) );
}
