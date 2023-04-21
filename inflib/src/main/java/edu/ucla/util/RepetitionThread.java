package edu.ucla.util;

/** @author keith cascio
    @since 20031204 */
public class RepetitionThread implements Runnable
{
	public RepetitionThread( Runnable runnable, long sleepmillis, int reps )
	{
		myRunnable    = runnable;
		mySleepMillis = sleepmillis;
		myRepetitions = reps;
	}

	/** @since 20040211 */
	public RepetitionThread( Runnable runnable, Runnable ultimately, long sleepmillis, int reps )
	{
		this( runnable, sleepmillis, reps );
		myUltimately = ultimately;
	}

	/** @since 20060522 */
	public Thread start(){
		if( startInterruptsGroup() ) interruptStartGroup();
		Thread ret = new Thread( startGroup(), (Runnable)this, getClass().getName() + " " + Integer.toString(INT_ID_COUNTER++) );
		ret.setPriority( startPriority() );
		ret.start();
		return ret;
	}

	/** @since 20040211 */
	public void ultimately()
	{
		if( myUltimately != null ) myUltimately.run();
	}

	/** @since 20060522 */
	public ThreadGroup startGroup(){
		return Thread.currentThread().getThreadGroup();
	}

	/** @since 20060522 */
	public boolean startInterruptsGroup(){
		return false;
	}

	/** @since 20060522 */
	public boolean interruptStartGroup(){
		ThreadGroup group = startGroup();
		if( group != Thread.currentThread().getThreadGroup() ){
			group.interrupt();
			Thread.yield();
			return true;
		}
		return false;
	}

	/** @since 20060522 */
	public int startPriority(){
		return Thread.currentThread().getPriority();
	}

	final public void setRunnable( Runnable runnable )
	{
		myRunnable = runnable;
	}

	public void run()
	{
		try{
			for( int i=0; i<myRepetitions; i++ ) delayedAction();
			ultimately();
		}catch( InterruptedException interruptedexception ){
			//System.out.println( Thread.currentThread().getName() + " interrupted" );
			Thread.currentThread().interrupt();
		}
	}

	final public void delayedAction() throws InterruptedException
	{
		Thread.sleep( mySleepMillis );
		if( myRunnable != null ) myRunnable.run();
	}

	private Runnable myRunnable;
	private Runnable myUltimately;
	private long     mySleepMillis = (long)5000;
	private int      myRepetitions = (int)    0;

	private static int INT_ID_COUNTER = (int)0;
}
