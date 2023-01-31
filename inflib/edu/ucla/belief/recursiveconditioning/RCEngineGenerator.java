package edu.ucla.belief.recursiveconditioning;

import edu.ucla.belief.*;
import edu.ucla.belief.dtree.Dtree;
import edu.ucla.belief.io.PropertySuperintendent;

import javax.swing.JComponent;
import javax.swing.JMenu;
import java.awt.Container;
import java.io.Serializable;
import java.util.Map;
import java.util.Arrays;
import java.util.Collection;

/** @author keith cascio
	@since  20030117 */
public class RCEngineGenerator extends Dynamator implements Serializable
{
	static final long serialVersionUID = 3026781255574499132L;

	protected void compileHook( BeliefNetwork bn, DynaListener cl )
	{
		//System.out.println( "RCEngineGenerator.compile(): bn.getUserObject() " + bn.getUserObject() );

		mySettings = getSettings( choosePropertySuperintendent( (PropertySuperintendent) bn ) );
		CompileThread thread = new CompileThread( bn, cl );
		thread.start();
	}

	public String getDisplayName()
	{
		return FLAG_DEBUG_DISPLAY_NAMES ? "rc (edu.ucla.belief.recursiveconditioning)" : "recursive conditioning";
	}

	public static Object getKeyStatic()
	{
		return "rcenginegenerator3026781255574499132L";
	}

	public Object getKey()
	{
		return getKeyStatic();
	}

  //public boolean    isEditable() { return false; }
  //public JComponent getEditComponent( Container cont ) { return null; }
  //public void       commitEditComponent() {}
  //public JMenu      getJMenu() { return null; }
  //public boolean needsRecompileHook( InferenceEngine ie ){ return true; }
	public Dynamator getCanonicalDynamator() { return this; }

	public static final String STR_GENERIC_ALLOCATION_ERROR = "Cannot allocate memory.";

	/** @since 20030117 */
	protected class CompileThread extends Thread implements CachingScheme.RCCreateListener
	{
		public CompileThread( BeliefNetwork bn, DynaListener CL )
		{
			myBeliefNetwork = bn;
			myCompilationListener = CL;
			this.setPriority( (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY)/2 );
		}

		public void run()
		{
			//System.out.println( "RCEngineGenerator.CompileThread.run()" );

			Bundle bundle = mySettings.getBundle();
			boolean flagStale = bundle.isStale();
			RC rc = bundle.getRC();

			if( flagStale || rc == null )
			{
				proxy = mySettings.getRCCreateListener();

				mySettings.setRCCreateListener( this );

				try{
					//System.out.println( "creating new" );
					Thread thread = null;
					if( rc == null ) thread = mySettings.createRCDgraphInThread( myBeliefNetwork );
					else thread = mySettings.allocRCDgraphInThread( myBeliefNetwork );

					if( thread == null )
					{
						//proxy.rcCreateDone( null );
						proxy.rcCreateError( STR_GENERIC_ALLOCATION_ERROR );
						mySettings.setRCCreateListener( proxy );
						myCompilationListener.handleError( STR_GENERIC_ALLOCATION_ERROR );
						return;
					}
					else thread.join();//not sure if the join() is necessary
				}catch( InterruptedException e ){
					String msg = "Warning: RCEngineGenerator.CompileThread interrupted.";
					System.err.println( msg );
					myCompilationListener.handleError( msg );
					return;
				}catch( Exception exception ){
					String msg = exception.getMessage();
					if( msg == null ) msg = exception.toString();
					myCompilationListener.handleError( msg );
					return;
				}catch( OutOfMemoryError error ){
					myCompilationListener.handleError( STR_OOME );
					return;
				}
			}
			else
			{
				//System.out.println( "using existing" );
				proxy = mySettings.getRCCreateListener();
				if( proxy != null )
				{
					proxy.rcConstructionDone();
					proxy.rcCreateDone( rc );
				}
				done( (RCDgraph)rc, myBeliefNetwork, myCompilationListener );
			}
		}

		public void rcCreateUpdate( double bestCost )
		{
			//System.out.println( "CompileThread.rcCreateUpdate(" +bestCost+ ")" );
			if( proxy != null ) proxy.rcCreateUpdate( bestCost );
		}

		public double rcCreateUpdateThreshold()
		{
			if( proxy != null ) return proxy.rcCreateUpdateThreshold();
			else return (double)100000000;
		}

		public boolean rcCreateStopRequested()
		{
			if( proxy != null ) return proxy.rcCreateStopRequested();
			else return false;
		}

		public void rcCreateDone( double bestCost, boolean optimal )
		{
			if( proxy != null ) proxy.rcCreateDone( bestCost, optimal );
		}

		public void rcCreateDone( RC rc )
		{
			mySettings.setRCCreateListener( proxy );
			mySettings.refresh( rc );

			if( proxy != null ) proxy.rcCreateDone( rc );
			done( (RCDgraph)rc, myBeliefNetwork, myCompilationListener );
		}

		public void rcConstructionDone(){
			if( proxy != null ) proxy.rcConstructionDone();
		}

		public void rcCreateError( String msg ){
			if( proxy != null ) proxy.rcCreateError( msg );
			myCompilationListener.handleError( msg );
		}

		protected CachingScheme.RCCreateListener proxy = null;
		protected BeliefNetwork myBeliefNetwork = null;
		protected DynaListener myCompilationListener = null;
	}

	public void done( RCDgraph rc, BeliefNetwork myBeliefNetwork, DynaListener myCompilationListener )
	{
		RCInferenceEngine IE = new RCInferenceEngine( rc, RCEngineGenerator.this, myBeliefNetwork );
		mySettings.addChangeListener( IE );
		myCompilationListener.handleInferenceEngine( IE );
	}

	public static InferenceEngine createInferenceEngine( BeliefNetwork bn )
	{
		return createInferenceEngine( bn, 1.0, false);
	}

	public static RCInferenceEngine createInferenceEngine( BeliefNetwork bn, double scalar, boolean useKB ){
		Settings settings = getSettings( bn );
		Dtree dt = settings.generateDtree( bn );
		CachingScheme cs = settings.getBundle().getCachingScheme();
		return createInferenceEngine( bn, dt, cs, scalar, useKB);
	}
	/** @since 20030128 */
	public static RCInferenceEngine createInferenceEngine( BeliefNetwork bn, Dtree dt, CachingScheme cs, double scalar, boolean useKB )
	{
		return createInferenceEngine( bn, cs, scalar, useKB, new DecompositionStructureUtils.ParamsGraphDT( bn, null, dt) );
	}

	/** @since 20030929 */
	public static RCInferenceEngine createInferenceEngine( BeliefNetwork bn, CachingScheme cs, double scalar, boolean useKB, DecompositionStructureUtils.ParamsGraph pgraph )
	{
		RC.RCCreationParams rcparam = new RC.RCCreationParams();
		{
			rcparam.scalar = scalar;
			rcparam.useKB = useKB;
			rcparam.allowKB = true;
			rcparam.bn = bn;
		}

		RCDgraph graph = new RCDgraph( rcparam, cs, null, pgraph );
		RCInferenceEngine IE = new RCInferenceEngine( graph, null, bn );
		return IE;
	}

	/** @since 20030205 */
	public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ){
		InferenceEngine ret = createInferenceEngine( bn );
		ret.setDynamator( dyn );
		return ret;
	}

	public static RCDtree createRCDtree( BeliefNetwork bn, double scalar, boolean useKB ) {
		Settings settings = getSettings( bn );
		Dtree dt = settings.generateDtree( bn );
		CachingScheme cs = settings.getCachingScheme();
		return createRCDtree( dt, bn, scalar, useKB, cs, /*includeMPE*/true);
	}

	public static RCDtree createRCDtree( Dtree dt, BeliefNetwork bn, double scalar, boolean useKB,
											CachingScheme cs, boolean includeMPE )
	{
		RC.RCCreationParams rcparam = new RC.RCCreationParams();
		{
			rcparam.scalar = scalar;
			rcparam.useKB = useKB;
			rcparam.allowKB = true;
			rcparam.bn = bn;
		}

		RCDtree tree = new RCDtree( rcparam, cs, null,
									new DecompositionStructureUtils.ParamsTreeDT( bn, null, dt, includeMPE));
		return tree;
	}

	public static RCDtree createRCDtree( RC.RCCreationParams rcparam, Dtree dt,
											CachingScheme cs, boolean includeMPE )
	{
		RCDtree tree = new RCDtree( rcparam, cs, null,
									new DecompositionStructureUtils.ParamsTreeDT( rcparam.bn, null, dt, includeMPE));
		return tree;
	}

	/** @since 20031210 */
	public void killState( PropertySuperintendent bn )
	{
		Settings settings = getSettings( choosePropertySuperintendent( bn ), false );
		if( settings != null )
		{
			settings.setDtree( null );
			settings.setRC( null );
		}
	}

	public static Settings getSettings( BeliefNetwork bn )
	{
		return getSettings( (PropertySuperintendent) bn );
	}

	/** @since 20081029 */
	public Object retrieveState( PropertySuperintendent bn ){
		return getSettings( bn );
	}

	public static Settings getSettings( PropertySuperintendent bn )
	{
		return getSettings( bn, true );
	}

	public static Settings getSettings( PropertySuperintendent bn, boolean construct )
	{
		//Settings ret = (Settings) bn.getUserObject();
		//if( ret == null && construct )
		//{
		//	ret = new Settings();
		//	bn.setUserObject( ret );
		//}

		Map properties = bn.getProperties();
		Object value = properties.get( getKeyStatic() );
		Settings ret = null;
		if( value instanceof Settings ) ret = (Settings)value;
		else if( construct )
		{
			ret = new Settings();
			properties.put( getKeyStatic(), ret );
		}

		return ret;
	}

	/** @since 20100108 */
	public Dynamator writeJavaCodeSettingsManipulation( BeliefNetwork beliefnetwork, boolean withComments, java.io.PrintStream out ){
		if( withComments ){ out.println( "    /* Edit settings. */" ); }
		edu.ucla.belief.recursiveconditioning.Settings settings = this.getSettings( beliefnetwork );
		out.println( "    "+settings.getClass().getName()+" settings = dynamator.getSettings( bn );" );
		if( withComments ){ out.println( "    /* Set the fraction of full memory to use. */" ); }
		out.println( "    settings.setUserMemoryProportion( (double)"+settings.getUserMemoryProportion()+" );" );
		if( withComments ){ out.println( "    /* Do "+((settings.getUseKB())?"":"not ")+"use the knowledge base feature. */" ); }
		out.println( "    settings.setUseKB(                "+settings.getUseKB()+" );" );
		if( withComments ){ out.println( "    /* Create the cache allocation (very important). */" ); }
		out.println( "    settings.validateRC(              bn );" );
		out.println();
		if( withComments ){ out.println( "    /* Characterize the cache allocation and estimated run time. */" ); }
		out.println( "    Bundle                              bundle = settings.getBundle();" );
		out.println( "    Computation       computation_AllMarginals = bundle.getAll();" );
		out.println( "    double        maxCacheEntries_AllMarginals = computation_AllMarginals.getNumMaxCacheEntries();" );
		out.println( "    double         fullMemoryCost_AllMarginals = computation_AllMarginals.getOptimalMemoryRequirement();" );
		out.println( "    double          expectedCalls_AllMarginals = computation_AllMarginals.getExpectedNumberOfRCCalls();" );
		out.println( "    int          estimatedSeconds_AllMarginals = computation_AllMarginals.getEstimatedSeconds();" );
		out.println();
		return this;
	}

	/** @since 20040520 */
	public Collection getClassDependencies(){
		return Arrays.asList( new Class[] {
			RCEngineGenerator.class,
			Settings.class,
			Bundle.class,
			Computation.class,
			RCInferenceEngine.class } );
	}

	transient protected Settings mySettings;
}
