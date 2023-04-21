package edu.ucla.belief;

import edu.ucla.util.*;
import edu.ucla.belief.io.PropertySuperintendent;

import java.awt.Container;
import javax.swing.JComponent;
//{superfluous} import javax.swing.JPanel;
import javax.swing.JMenu;
//{superfluous} import java.util.Map;
//{superfluous} import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import java.lang.reflect.Method;

/** @author keith cascio
	@since  20030117 */
public abstract class Dynamator
{
	public static final int     INT_MINIMUM_VARIABLES    = 2;
	public static final String
	  STR_NAME_CLASS_ALGORITHM   = "il2.inf.Algorithm",
	  STR_NAME_METHOD_FORCLASS   = "forClass",
	  STR_NAME_CLASS_TIGER       = "edu.ucla.belief.CrouchingTiger",
	  STR_NAME_METHOD_TOIL2      = "toIL2Settings",
	  STR_UNINITIALIZED          = "uninitialized",
	  STR_OOME                   = "Compilation ran out of memory.";
	public static       boolean FLAG_DEBUG_DISPLAY_NAMES = false;

	final public void compile( BeliefNetwork bn, DynaListener cl )
	{
		if( validate( bn, cl ) ) compileHook( bn, cl );
	}

	/** @since 20060201 */
	protected void compileHook( BeliefNetwork bn, DynaListener cl ){
		ThreadGroup group = cl.getThreadGroup();
		if( group == null ){ group = Thread.currentThread().getThreadGroup(); }
		Thread tCompile = new Thread( group, new RunCompile( bn, cl ), getDisplayName() + " compilation" );
		tCompile.setPriority( Dynamator.getCompilationpriority() );
		tCompile.start();
	}

	/** @since 20060719 */
	final protected boolean validate( BeliefNetwork bn, DynaListener cl ){
		if( bn.size() < INT_MINIMUM_VARIABLES )
		{
			cl.handleError( "Please add at least "+(INT_MINIMUM_VARIABLES-bn.size())+" variable(s) to this network before compiling." );
			return false;
		}
		else if( bn.thereExists( InferenceValidProperty.PROPERTY, InferenceValidProperty.PROPERTY.FALSE ) )
		{
			cl.handleError( InferenceValidProperty.createErrorMessage( bn ) );
			return false;
		}
		else return true;
	}

	/** @since 20060719 */
	final public InferenceEngine runSynchronous( BeliefNetwork bn, DynaListener cl ){
		if( !validate( bn, cl ) ) return (InferenceEngine) null;

		RunCompile runcompile = new RunCompile( bn, cl );
		runcompile.run();
		return runcompile.myInferenceEngine;
	}

	abstract public InferenceEngine manufactureInferenceEngineOrDie( BeliefNetwork bn, Dynamator dyn ) throws Throwable;
	abstract public String                      getDisplayName();
	abstract public Object                              getKey();
	abstract public void                             killState( PropertySuperintendent arbitrary );
	abstract public Object                       retrieveState( PropertySuperintendent arbitrary );
	abstract public Collection            getClassDependencies();
	abstract public Dynamator            getCanonicalDynamator();
	strictfp public boolean                         isEditable(){ return false; }
	strictfp public Commitable                getEditComponent( Container cont ){ return null; }
	strictfp public void                   commitEditComponent(){}
	strictfp public JMenu                             getJMenu(){ return null; }

	/** @since 20081030 */
	public Dynamator fixPropertySuperintendent( PropertySuperintendent arbitrary ){
		myFixedPropertySuperintendent = arbitrary;
		Dynamator canonical = getCanonicalDynamator();
		if(      (canonical != null) && (canonical != this) ){ canonical.fixPropertySuperintendent( arbitrary ); }
		return this;
	}
	protected PropertySuperintendent myFixedPropertySuperintendent;

	/** @since 20081030 */
	public PropertySuperintendent choosePropertySuperintendent( PropertySuperintendent bn ){
		PropertySuperintendent ret = myFixedPropertySuperintendent == null ? bn : myFixedPropertySuperintendent;
	  //System.out.println( getClass().getName() + " choosing properties from " + ret.getClass().getName() );
		return ret;
	}

	/** @since 20081029 */
	public Object toIL2Settings( PropertySuperintendent bn ){
		try{
			Object state = retrieveState( choosePropertySuperintendent( bn ) );
			if(    state == null ){ return null; }
			Method meth = methodTigerToIL2();
			if(    meth  == null ){ return null; }
			return meth.invoke( null, new Object[]{ state } );
		}catch( Throwable thrown ){
			System.err.println( "warning: "+getClass().getName()+".toIL2Settings( "+bn+" ) caught " + thrown );
			if( Definitions.DEBUG ){
				Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				thrown.printStackTrace( Definitions.STREAM_VERBOSE );
			}
		}
		return null;
	}

	/** @since 20081029 */
	static public Method methodTigerToIL2(){
		if( CLASS_CROUCHINGTIGER == null ){
			try{
				CLASS_CROUCHINGTIGER                = Class.forName( STR_NAME_CLASS_TIGER );
				METHOD_CROUCHINGTIGER_TOIL2SETTINGS = CLASS_CROUCHINGTIGER.getDeclaredMethod( STR_NAME_METHOD_TOIL2, new Class[]{ Object.class } );
			}catch( ClassNotFoundException thrown ){
				CLASS_CROUCHINGTIGER                = RuntimeException.class;
			}catch( Throwable thrown ){
				System.err.println( "warning: Dynamator.methodTigerToIL2() caught " + thrown );
			}
		}
		return METHOD_CROUCHINGTIGER_TOIL2SETTINGS;
	}

	/** @since 20081029 */
	public Object asIL2Algorithm(){
		if( myIL2Algorithm == STR_UNINITIALIZED ){
			try{
				myIL2Algorithm = null;
				Collection dependencies = getClassDependencies();
				if( dependencies == null ){ return myIL2Algorithm; }
				Class clazz = null, jackpot = null;
				for( Iterator it = dependencies.iterator(); it.hasNext(); ){
					if( il2.inf.JointEngine.class.isAssignableFrom( clazz = (Class) it.next() ) ){ jackpot = clazz; }
				}
				if( jackpot      == null ){ return myIL2Algorithm; }
				Method meth = methodAlgorithmForClass();
				if( meth         == null ){ return myIL2Algorithm; }
				myIL2Algorithm = meth.invoke( null, (Object[]) new Class[]{ jackpot } );
			}catch( Throwable thrown ){
				System.err.println( "warning: "+getClass().getName()+".asIL2Algorithm() caught " + thrown );
			}
		}
		return myIL2Algorithm;
	}
	protected              Object myIL2Algorithm = STR_UNINITIALIZED;
	private   static       Class   CLASS_ALGORITHM,            CLASS_CROUCHINGTIGER;
	private   static       Method METHOD_ALGORITHM_FOR_CLASS, METHOD_CROUCHINGTIGER_TOIL2SETTINGS;

	/** @since 20081029 */
	static public Method methodAlgorithmForClass(){
		if( CLASS_ALGORITHM == null ){
			try{
				CLASS_ALGORITHM            = Class.forName( STR_NAME_CLASS_ALGORITHM );
				METHOD_ALGORITHM_FOR_CLASS = CLASS_ALGORITHM.getDeclaredMethod( STR_NAME_METHOD_FORCLASS, new Class[]{ Class.class } );
			}catch( ClassNotFoundException thrown ){
				CLASS_ALGORITHM            = RuntimeException.class;
			}catch( Throwable thrown ){
				System.err.println( "warning: Dynamator.methodAlgorithmForClass() caught " + thrown );
			}
		}
		return METHOD_ALGORITHM_FOR_CLASS;
	}

	/** @since 20091228 Linus Torvalds 40th birthday! http://en.wikipedia.org/wiki/Linus_Torvalds */
	final public InferenceEngine manufactureInferenceEngine( BeliefNetwork bn ){
		return manufactureInferenceEngine( bn, this );
	}

	/** @since 20091228 Linus Torvalds 40th birthday! http://en.wikipedia.org/wiki/Linus_Torvalds */
	final public InferenceEngine manufactureInferenceEngine( BeliefNetwork bn, Dynamator dyn ){
		InferenceEngine ret = null;
		try{
			ret = manufactureInferenceEngineOrDie( bn, dyn );
		}catch( Throwable throwable ){
			ret = null;
			System.err.println( throwable );
		}
		return ret;
	}

	/** @since 20051017 */
	public boolean probabilitySupported(){
		return true;
	}

	public interface Commitable{
		public void                commitChanges();
		public JComponent           asJComponent();
		public void        copyToSystemClipboard();
	}

	/** @since 20080227 */
	public interface Decorator{
		public Decorator decorate( InferenceEngine ie );
	}

	/** @since 20080227 */
	public Dynamator addDecorator( Decorator decorator ){
		if( decorators == null ){ decorators = new LinkedList(); }
		decorators.add( decorator );
		return this;
	}

	/** @since 20100108 */
	public Dynamator writeJavaCodeSettingsManipulation( BeliefNetwork beliefnetwork, boolean withComments, java.io.PrintStream out ){
		return this;
	}

	/** @since 20060201 */
	public static int getCompilationpriority(){
		return (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY)/2;
	}

	/** @author keith cascio
		@since  20060201 */
	public class RunCompile implements Runnable
	{
		public RunCompile( BeliefNetwork bn, DynaListener cl ){
			RunCompile.this.myBeliefNetwork = bn;
			RunCompile.this.myCompilationListener = cl;
		}

		public void run(){
			Thread.currentThread().setPriority( Dynamator.getCompilationpriority() );

			InferenceEngine ie = null;
			String message_error = null;

			try{
				ie = Dynamator.this.manufactureInferenceEngineOrDie( RunCompile.this.myBeliefNetwork, Dynamator.this );
				if( Dynamator.this.decorators != null ){
					for( Iterator it = Dynamator.this.decorators.iterator(); it.hasNext(); ){
						((Decorator) it.next()).decorate( ie );
					}
				}
			}catch( OutOfMemoryError error ){
				ie = null;
				message_error = STR_OOME;
			}catch( Throwable throwable ){
				if( Definitions.DEBUG ){ throwable.printStackTrace( System.err ); }
				ie = null;
				message_error = throwable.getMessage();
				if( message_error == null ){ message_error = throwable.toString(); }
			}

			if( (ie == null) && (message_error == null) ) message_error = "Error creating inference engine.";

			if( message_error == null ) myCompilationListener.handleInferenceEngine( ie	);
			else myCompilationListener.handleError( message_error );

			RunCompile.this.myInferenceEngine = ie;

			return;
		}

		private   BeliefNetwork     myBeliefNetwork;
		private   DynaListener      myCompilationListener;
		private   InferenceEngine   myInferenceEngine;
	}

	private Collection/*<Decorator>*/ decorators;
}
