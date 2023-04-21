package edu.ucla.belief.ui.recursiveconditioning;

/**
	@author Keith Cascio
	@since 042103
*/
public class PMCloser extends edu.ucla.util.RepetitionThread
{
	public static boolean FLAG_DEBUG = false;
	public static final long LONG_SLEEP_MILLIS = (long)6500;
	public static final int INT_REPETITIONS = (int)3;

	public PMCloser( final RCProgressMonitor monitor )
	{
		this( monitor, LONG_SLEEP_MILLIS, INT_REPETITIONS );
	}

	public PMCloser( final RCProgressMonitor monitor, long sleepmillis, int reps )
	{
		super( (Runnable)null, sleepmillis, reps );
		setRunnable( new Runnable(){ public void run() { monitor.close(); } } );
		this.monitor = monitor;

		//if( FLAG_DEBUG ) edu.ucla.belief.ui.util.Util.STREAM_VERBOSE.println( "(PMCloser)"+getName()+"()" );
	}

	public void run()
	{
		monitor.close();
		super.run();
		if( FLAG_DEBUG ) edu.ucla.belief.ui.util.Util.STREAM_VERBOSE.println( Thread.currentThread().getName()+" done closing " + monitor.getNote() );
	}

	private RCProgressMonitor monitor;
}
