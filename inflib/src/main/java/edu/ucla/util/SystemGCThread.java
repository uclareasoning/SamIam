package edu.ucla.util;

/** @author keith cascio
	@since 20031204 */
public class SystemGCThread extends RepetitionThread
{
	public static final long LONG_SLEEP_MILLIS = (long)0x2000;
	public static final int INT_REPETITIONS = (int)8;

	public SystemGCThread()
	{
		this( LONG_SLEEP_MILLIS, INT_REPETITIONS );
	}

	public SystemGCThread( long sleepmillis, int reps )
	{
		super( new Runnable(){ public void run() { System.gc(); } }, sleepmillis, reps );
	}

	/** @since 20060522 */
	public ThreadGroup startGroup(){
		return SystemGCThread.getThreadGroup();
	}

	/** @since 20060522 */
	public boolean startInterruptsGroup(){
		return true;
	}

	/** @since 021104 */
	public void ultimately(){
		//System.out.println( "SystemGCThread.ultimately()" );
		System.runFinalization();
		//System.out.println( "DONE System.runFinalization();" );
	}

	/** @since 20060522 */
	public static ThreadGroup getThreadGroup(){
		if( SystemGCThread.GROUP == null ) SystemGCThread.GROUP = new ThreadGroup( SystemGCThread.class.getName() );
		return SystemGCThread.GROUP;
	}

	private static ThreadGroup GROUP;
}
