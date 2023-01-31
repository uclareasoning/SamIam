package edu.ucla.util;

/** A base class that helps you define a piece of code
	that might be invoked "too often", that is: a piece
	of code that performs an expensive task that must
	be restarted afresh whenever it is run at all.
	This type of task often arises from user-initiated
	GUI actions.
	<br /><br />
	For example, you might design a complex, editable
	Swing component that listens for changes in
	an underlying data model.  Changes to the model
	could be triggered by user-initiated edit actions
	on the Swing component, or by programmatic actions
	initiated elsewhere.  The period between rapidly
	succeeding change events could be much shorter
	than the time it takes to refresh the component.
	Furhermore, you would prefer that the listening method
	return immediately rather than blocking until it
	finishes the refresh.  In this case, we would
	prefer to abandon the redrawing task whenever
	a new change event occurs before we are finished.
	This class solves exactly this problem.
	<br /><br />
	All the setup you have to do is extend this class
	and implement {@link #runImpl(Object)}.  To use the class, instantiate
	one instance of it and call {@link #start()} on that instance
	whenever you want to run the expensive code.
	Whever you call {@link #start()}, this class will interrupt previous
	Threads spawned by calls to {@link #start()}.
	However, it will be useful only if your implementation
	of {@link #runImpl(Object)} calls Thread.sleep(n), n > 0,
	every so often.
	<br /><br />
	This prototypical example class
	is a recommended use of {@link Interruptable},
	and it illustrates the above example :
	<pre>
class ComplexEditableComponent extends JComponent
{
	void eventDispatched( Event event ){
		refresh( event.getHint() );
	}

	void refresh( Hint hint ){
		{@link #start(Object) <b>myRunRefresh.start( hint )</b> };
	}

	class <b>RunRefresh</b> extends {@link Interruptable} {
		public void {@link #runImpl(Object) runImpl( Object arg )}{
			ComplexEditableComponent.this.<font color="#cc0000">refreshImpl</font>( (Hint)arg );
		}
	}

	void <font color="#cc0000">refreshImpl</font>( Hint hint ){
		try{
			<font color="#cc0000">Thread.sleep(2);</font>
			<font color="#cc0000">expensiveA();</font>
			<font color="#cc0000">Thread.sleep(2);</font>
			<font color="#cc0000">expensiveB();</font>
			<font color="#cc0000">Thread.sleep(2);</font>
			<font color="#cc0000">expensiveC();</font>
		}catch( InterruptedException e ){
			<font color="#cc0000">cleanup();</font>
		}
	}

	<b>RunRefresh myRunRefresh = new RunRefresh()</b>;
}
	</pre>
	@author Keith Cascio
	@since 121404 */
public abstract class Interruptable extends Object implements Runnable
{
	/** Implement this method to perform a expensive task.
		Call {@link #checkInterrupted()} every so often to allow
		the thread to be interrupted.
		@param arg1 The argument allows your task to take arguments
		passed in by calls to {@link #start(Object)}
	*/
	abstract public void runImpl( Object arg1 ) throws InterruptedException;

	/** Override this to return true to print debug messages.
		@return false
		@since 011505 */
	public boolean debug(){ return false; }

	/** Override this to return true to print debug messages.
		@return false
		@since 011505 */
	public boolean verbose(){ return false; }

	/** Override this to help print meaningful verbose messages.
		@return null
		@since 011505 */
	public String getNameMethodOfInterest(){ return null; }

	public static boolean FLAG_DEBUG_OVERRIDE = false;
	public static boolean FLAG_VERBOSE_OVERRIDE = false;

	/** Run the task in the current Thread, ie bypass
		the normal functioning of this class. */
	final public void run(){
		runPrivateImpl( null );
	}

	final private void runPrivateImpl( Object arg1 ){
		InterruptedException interrupt = null;

		synchronized( mySynch ){
			try{ runImpl( arg1 ); }
			catch( InterruptedException interruptedexception ){
				interrupt = interruptedexception;
				Thread.currentThread().interrupt();
			}
		}

		if( interrupt != null ){
			if( debug() || FLAG_DEBUG_OVERRIDE ){
				STREAM_DEBUG.print( interrupt.getMessage() );
				if( verbose() || FLAG_VERBOSE_OVERRIDE ){
					STREAM_VERBOSE.print( " at:\n    " );
					String methodname = getNameMethodOfInterest();
					if( methodname == null ) interrupt.printStackTrace();//Util.printStackTrace( interrupt, 4, System.out );
					else printMethodStackTraceElement( interrupt, methodname, System.out, true );
				}
				else STREAM_VERBOSE.println();
			}
			return;
		}
	}

	/** Helper class.  Handles sleeping and wraps
		the argument to pass to {@link #runImpl(Object)}
		@author Keith Cascio
		@since 121404 */
	private class Runner implements Runnable
	{
		public Runner(){}

		public Runner( long millis ){
			this.mySleepMillis = millis;
			this.myFlagSleep = true;
		}
		public Runner( long millis, Object arg1 ){
			this( millis );
			this.myArgument = arg1;
		}
		public Runner( Object arg1 ){
			this.myArgument = arg1;
		}
		public void run(){
			try{ Thread.sleep( mySleepMillis ); }
			catch( InterruptedException interruptedexception ){
				if( Interruptable.this.debug() || FLAG_DEBUG_OVERRIDE ) STREAM_DEBUG.println( interruptedexception );
				Thread.currentThread().interrupt();
				return;
			}
			Interruptable.this.runPrivateImpl( myArgument );
		}
		private long mySleepMillis = (long)0;
		private boolean myFlagSleep = false;
		private Object myArgument = null;
	}

	/** @since 011705 */
	private class Worker implements Runnable
	{
		public void run(){
			myJobCount = 0;
			try{
				while( true ){
					Thread.yield();
					Runnable runnable = null;
					synchronized( this ){
						Thread.interrupted();//clear interrupted state
						myFlagSafe = false;
						if( myRunnable == null ){
							myFlagBroke = true;
							break;
						}
						runnable = myRunnable;
						myRunnable = null;
						myThread.setName( makeThreadName( myCounterThreadNamesLocal++, INT_COUNTER_WORKERJOBS_GLOBAL++ ) );
					}
					if( runnable != null ) runnable.run();
					myFlagSafe = true;
					++myJobCount;
				}
				myFlagSafe = true;
			}finally{
				myFlagBroke = true;
				if( debug() ) STREAM_DEBUG.println( Interruptable.this.getAbbreviatedClassName() + " Worker ran " + myJobCount + " jobs" );
			}
		}

		synchronized public void start(){
			if( myThread == null ){
				newThread();
				return;
			}
			while( (!myFlagSafe) && myThread.isAlive() ) { Thread.yield(); }
			if( (!myThread.isAlive()) || myFlagBroke ) newThread();
		}

		private void newThread(){
			if( debug() ) STREAM_DEBUG.println( Interruptable.this.getAbbreviatedClassName() + " Worker creating new thread " + myThreadCount );
			++myThreadCount;
			myThread = new Thread( Interruptable.this.getGroup(), (Runnable)this );
			myFlagSafe = true;
			myFlagBroke = false;
			myThread.start();
		}

		synchronized public Thread getThread(){
			return myThread;
		}

		synchronized public Thread setRunnable( Runnable runnable ){
			boolean running = (!myFlagSafe) && (myThread != null) && myThread.isAlive();
			if( running && (Interruptable.this.myVeto != null) && Interruptable.this.myVeto.vetoInterruption( Interruptable.this ) ){
				//System.out.println( "vetoing interruption..." );
				return null;
			}
			//if( myThread != null ) myThread.interrupt();
			Interruptable.this.getGroup().interrupt();
			this.myRunnable = runnable;
			this.start();
			return myThread;
		}

		private volatile Thread myThread;
		private volatile Runnable myRunnable;
		private volatile int myJobCount;
		private volatile int myThreadCount = 0;
		private volatile boolean myFlagBroke;
		private volatile boolean myFlagSafe;
	}

	/** Implement this interface to define a callback for deciding whether
		or not to go ahead with an interruption.

		@author keith cascio
		@since 20060328 */
	public interface Veto{
		/** Decide whether or not to veto the interruption.
			The implementation of this method may need to spawn
			a yes/no confirm dialog to ask the user.
			It will help the user if this Interruptable is
			identified meaningfully, so call setName( "meaningful name" )
			when configuring the task.

			@return true means VETO THE INTERRUPTION, DO NOT START A NEW COMPUTATION, FINISH THE CURRENT COMPUTATION INSTEAD  */
		public boolean vetoInterruption( Interruptable interruptable );
	}

	/** Set a callback.
		@since 20060328 */
	public void setVeto( Veto veto ){
		myVeto = veto;
	}

	/** Set a descriptive name to identify this task
		@since 20060328 */
	public void setName( String name ){
		Interruptable.this.myName = name;
	}

	/** For identification purposes
		@return the name set by setName(), or a generic name if setName() was never called
		@since 20060328 */
	public String getName(){
		return Interruptable.this.myName;
	}

	/** We use a single, devoted Object to synchronize
		calls to {@link #runImpl(Object)}.  This method returns it.
		@return The Object used to synchronize {@link #runImpl(Object)} */
	public Object getSynch(){
		return this.mySynch;
	}

	/** Run the task in the current Thread, ie bypass
		the normal functioning of this class.
		@param arg1 Passed on to {@link #runImpl(Object)} */
	public void run( Object arg1 ){
		new Runner( arg1 ).run();
	}

	/** Run threaded or not.
		@param threaded If true, run the expensive task in
		a new Thread (i.e. call {@link #start()}).  If false,
		run the task in the current Thread (i.e. call {@link #run()}).
		@return New Thread if true, Thread.currentThread() if false. */
	public Thread run( boolean threaded ){
		if( threaded ) return start();
		else{
			run();
			return Thread.currentThread();
		}
	}

	/** Run threaded or not.
		@param threaded If true, run the expensive task in
		a new Thread (i.e. call {@link #start(Object)}).  If false,
		run the task in the current Thread (i.e. call {@link #run(Object)}).
		@param arg1 Passed on to {@link #runImpl(Object)}
		@return New Thread if true, Thread.currentThread() if false. */
	public Thread run( boolean threaded, Object arg1 ){
		if( threaded ) return start( arg1 );
		else{
			run( arg1 );
			return Thread.currentThread();
		}
	}

	/** Run the expensive task in a new Thread. */
	public Thread start(){
		return maneuver( (Runnable)this );
	}

	/** Run the expensive task in a new Thread.
		@param arg1 Passed on to {@link #runImpl(Object)} */
	public Thread start( Object arg1 ){
		return maneuver( new Runner( arg1 ) );
	}

	/** Run the expensive task in a new Thread that waits
		for the specified number of milliseconds before
		beginning the task.
		@param millis Milliseconds to sleep before calling {@link #runImpl(Object)}. */
	public Thread start( long millis ){
		return maneuver( new Runner( millis ) );
	}

	/** Run the expensive task in a new Thread that waits
		for the specified number of milliseconds before
		beginning the task.
		@param millis Milliseconds to sleep before calling {@link #runImpl(Object)}.
		@param arg1 Passed on to {@link #runImpl(Object)} */
	public Thread start( long millis, Object arg1 ){
		return maneuver( new Runner( millis, arg1 ) );
	}

	/** @since 011705 */
	private Thread maneuver( Runnable runnable ){
		return executeInWorker( runnable );
	}

	/** @since 20050225 */
	synchronized public void interrupt(){
		//if( myLastStarted != null ) myLastStarted.interrupt();
		//System.out.println( getSimpleName() + ".interrupt()" );
		if( myGroupComputation == null ) return;
		if( Thread.currentThread().getThreadGroup() == myGroupComputation ) return;
		myGroupComputation.interrupt();
	}

	/** @since 20050812 */
	synchronized public void stop(){
		//System.out.println( getSimpleName() + ".stop()" );
		if( myGroupComputation == null ) return;
		if( Thread.currentThread().getThreadGroup() == myGroupComputation ) return;
		myGroupComputation.stop();
	}

	public static final long LONG_PAUSE_GRACE_MILLIS = (long)0x800;

	/** @since 20050812 */
	synchronized public void cancel(){
		//System.out.println( getSimpleName() + ".cancelLater()" );

		if( myGroupComputation == null ){
			//System.out.println( "no computation group" );
			return;
		}

		boolean active = false;
		for( int i=0; i<8; i++ ){
			Thread.yield();
			if( myGroupComputation.activeCount() > 0 ){ active = true; break; }
		}

		if( !active ){
			//System.out.println( "no active threads" );
			return;
		}

		if( myCancellation == null ) myCancellation = new Cancellation( Interruptable.this.myGroupComputation );
		String namesimple = getSimpleName();
		if( myGroupCancellation == null ) myGroupCancellation = new ThreadGroup( getCurrentParent(), namesimple + " cancellation thread group" );
		myCancellation.start( myGroupCancellation );
	}

	/** @since 20050812 */
	static private class Cancellation implements Runnable{
		public Cancellation( ThreadGroup toCancel ){
			Cancellation.this.myThreadGroupToCancel = toCancel;
		}

		public void run(){
			myResult = Interruptable.doCancel( Cancellation.this.myThreadGroupToCancel );
		}

		public Thread start( ThreadGroup parent ){
			Thread ret = new Thread( parent, Cancellation.this, myThreadGroupToCancel.getName() + " cancellation " + Integer.toString( INT_COUNTER_CANCELLATION_GLOBAL++ ) );
			ret.start();
			return ret;
		}

		private ThreadGroup myThreadGroupToCancel;
		private boolean     myResult;
	}

	/** @return true if we had to invoke stop()
		@since 20050812 */
	static private boolean doCancel( ThreadGroup group ){
		if( group == null ) return false;
		if( group == Thread.currentThread().getThreadGroup() ) return false;

		group.interrupt();

		boolean active = false;
		try{
			long begin = System.currentTimeMillis();
			while( active = (group.activeCount() > 0) ){
				if( (System.currentTimeMillis() - begin) > LONG_PAUSE_GRACE_MILLIS ) break;
				Thread.sleep( 0x40 );
			}
		}catch( InterruptedException interruptedexception ){
			Thread.currentThread().interrupt();
			return false;
		}

		if( active ) group.stop();

		return active;
	}

	/** @since 20050812 */
	private String getSimpleName(){
		String nameclass = this.getClass().getName();
		String namesimple = nameclass.substring( nameclass.lastIndexOf( '.' )+1 );
		return namesimple;
	}

	/** @since 20060719 */
	public void setParent( ThreadGroup group ){
		myParent = group;
	}

	/** @since 20060719 */
	public ThreadGroup getCurrentParent(){
		if( myParent == null ) return Thread.currentThread().getThreadGroup();
		else return myParent;
	}

	/** @since 20050812 */
	private ThreadGroup getGroup(){
		if( myGroupComputation == null ){
			myGroupComputation = new ThreadGroup( getCurrentParent(), getSimpleName() + " computation thread group" );
		}
		return myGroupComputation;
	}

	synchronized private Thread startInThread( Runnable runnable ){
		//if( myLastStarted != null ) myLastStarted.interrupt();
		Interruptable.this.interrupt();
		Thread ret = new Thread( Interruptable.this.getGroup(), runnable, makeThreadName( myCounterThreadNamesLocal++, INT_COUNTER_THREADNAMES_GLOBAL++ ) );
		ret.start();
		return ret;
	}

	/** @since 011705 */
	synchronized private Thread executeInWorker( Runnable runnable ){
		if( myWorker == null ) myWorker = new Worker();
		return myWorker.setRunnable( runnable );
	}

	/** @since 011705 */
	private String makeThreadName( int local, int global ){
		return getAbbreviatedClassName() + " local[" + Integer.toString(local) + "] global[" + Integer.toString(global) + "]";
	}

	/** @since 011705 */
	private String getAbbreviatedClassName(){
		if( myAbbreviatedClassName == null ){
			String classname = this.getClass().getName();
			myAbbreviatedClassName = classname.substring( classname.lastIndexOf('.')+1 );
		}
		return myAbbreviatedClassName;
	}

	public static final java.io.PrintStream STREAM_VERBOSE = System.out;
	public static final java.io.PrintStream STREAM_DEBUG = System.out;

	/** @since 011505 */
	static public void printMethodStackTraceElement( Throwable throwable, String methodname, java.io.PrintStream stream, boolean abbreviate ){
		if( (methodname == null) || (methodname.length() < 1) ) return;
		StackTraceElement[] array = throwable.getStackTrace();
		for( int i=0; i<array.length; i++ ){
			if( array[i].getMethodName().equals( methodname ) ){
				String toprint = null;
				if( abbreviate ) toprint = array[i].getFileName() + ":" + array[i].getLineNumber();
				else toprint = array[i].toString();
				stream.println( toprint );
			}
		}
	}

	/** @return number of active threads
		@since  20060720 */
	static public int kill( ThreadGroup group ){
		int count = group.activeCount();

		group.interrupt();

		try{
			Thread.sleep( 0x80 );
		}catch( InterruptedException interruptedexception ){
			System.err.println( "warning! Interruptable.kill() interrupted" );
			Thread.currentThread().interrupt();
			return -1;
		}

		boolean active = false;
		for( int i=0; i<4; i++ ){
			Thread.yield();
			if( group.activeCount() > 0 ){ active = true; break; }
		}

		if( active ) new Cancellation( group ).start( Thread.currentThread().getThreadGroup() );

		return count;
	}

	public static final String STR_NAME_PREFIX = "anonymous Interruptable ";

	private String myName = STR_NAME_PREFIX + Integer.toString(INT_COUNTER_NAMES_GLOBAL++);
	private Veto myVeto;
	private Worker myWorker;
	private Cancellation myCancellation;
	//private Thread myLastStarted;
	private ThreadGroup myGroupComputation, myGroupCancellation, myParent;
	private Object mySynch = new Object();
	private int myCounterThreadNamesLocal = 0;
	private int myCounterWorkerJobsLocal = 0;
	private String myAbbreviatedClassName;

	private static int INT_COUNTER_THREADNAMES_GLOBAL  = 0;
	private static int INT_COUNTER_WORKERJOBS_GLOBAL   = 0;
	private static int INT_COUNTER_CANCELLATION_GLOBAL = 0;
	private static int INT_COUNTER_NAMES_GLOBAL        = 0;
}
