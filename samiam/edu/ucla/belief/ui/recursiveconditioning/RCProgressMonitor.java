package edu.ucla.belief.ui.recursiveconditioning;

import edu.ucla.belief.recursiveconditioning.*;

import edu.ucla.belief.ui.UI;

import javax.swing.ProgressMonitor;
import javax.swing.*;
import java.awt.*;

/**
	@author Keith Cascio
	@since 040303
*/
public class RCProgressMonitor extends ProgressMonitor
// implements RC.ProgressEventListener
{
	public static final long LONG_POLL_PERIOD = (long)1000;

	public RCProgressMonitor( Component parentComponent, Settings settings )
	{
		super( parentComponent, "Pr(e):", "performing calculation...", (int)0, (int) settings.getBundle().getPe().getExpectedNumberOfRCCalls() );
		//super( parentComponent, "Pr(e):", "performing calculation..." + Integer.toString( INT_ID_COUNTER++ ), (int)0, (int) settings.getExpectedNumberOfRCCalls_Pe() );

		//System.out.println( "RCProgressMonitor( 0, "+getMaximum()+" )" );
		//new Throwable().printStackTrace();

		myRC = settings.getRC();
		if( myRC == null ) close();
		else
		{
			//myRC.addProgressEventListener( this );
			setMillisToPopup( 500 );
			setMillisToDecideToPopup( 500 );
			myCachePreviewDialog = CachePreviewDialog.theVisibleCachePreviewDialog;
			if( myCachePreviewDialog != null )
			{
				//System.out.println( "RCProgressMonitor hiding theVisibleCachePreviewDialog" );
				myCachePreviewDialog.setVisible( false );
			}
		}
	}

	public static int INT_ID_COUNTER = (int)0;

	public static void probability( UI parentComponent, Settings settings, RCInferenceEngine ie )
	{
		RCProgressMonitor monitor = new RCProgressMonitor( parentComponent, settings );

		RCDgraph graph = ie.underlyingCompilation();

		//Thread thread =
		graph.recCond_PeAsThread( parentComponent );
		graph.pauseRecCondAsThread();
		monitor.setUIAndThread( parentComponent, null );//thread );
		//parentComponent.setRunningPrE( thread, graph, monitor );
		parentComponent.setRunningPrE( graph, monitor );
		monitor.progressMade((long)1);
		graph.resumeRecCondAsThread();
		//return thread;

		monitor.new Poll().start();
	}

	public void progressMade( long progressCount )
	{
		//System.out.println( "RCProgressMonitor.progressMade("+progressCount+")" );
		//new Throwable().printStackTrace();

		setProgress( (int)progressCount );

		if( myFlagNotStopStarted && isCanceled() )
		{
			//System.out.println( "RCProgressMonitor.progressMade(), isCanceled()==true, calling stopAndWaitRecCondAsThread()" );
			myFlagNotStopStarted = false;
			new StopAndWait().start();
		}
	}

	/**
		@author Keith Cascio
		@since 040803
	*/
	public class StopAndWait extends Thread
	{
		public StopAndWait()
		{
			setName( "StopAndWait" + Integer.toString( theCounterThreadCreation++ ) );
		}

		public void run()
		{
			myRC.stopAndWaitRecCondAsThread();
			//System.out.println( "...stopAndWaitRecCondAsThread() returned" );
			//myUI.setRunningPrE( null, null, null );
			myUI.setRunningPrE( null, null );
			myUI.clearPrE();
			close();
		}
	}

	/**
		@author Keith Cascio
		@since 091703
	*/
	public class Poll extends Thread
	{
		public Poll()
		{
			setName( "Poll" + Integer.toString( theCounterThreadCreation++ ) );
		}

		public void run()
		{
			try{
				while( myFlagNotCloseCalled )
				{
					Thread.sleep( LONG_POLL_PERIOD );
					progressMade( myRC.counters.totalCalls() );
				}
			}catch( InterruptedException e ){
				System.err.println( "Warning: RCProgressMonitor.Poll.run() interrupted by " + e );
			}
			//System.out.println( "(RCProgressMonitor.Poll)"+getName()+".run() returning" );
		}
	}
	static private int theCounterThreadCreation = (int)0;

	public void setUIAndThread( UI ui, Thread thread )
	{
		myUI = ui;
		//myThread = thread;
	}

	public void close()
	{
		//System.out.println( "(RCProgressMonitor)"+getNote()+".close()" );
		//new Throwable().printStackTrace();
		super.close();
		if( myFlagNotCloseCalled )
		{
			//if( myRC != null ) myRC.removeProgressEventListener( this );
			if( myCachePreviewDialog != null )
			{
				//System.out.println( "RCProgressMonitor showing theVisibleCachePreviewDialog" );
				myCachePreviewDialog.setVisible( true );
			}
			myFlagNotCloseCalled = false;
		}
	}

	protected RC myRC;
	//protected Thread myThread;
	protected UI myUI;
	protected CachePreviewDialog myCachePreviewDialog;
	protected boolean myFlagNotStopStarted = true;
	protected boolean myFlagNotCloseCalled = true;
}
