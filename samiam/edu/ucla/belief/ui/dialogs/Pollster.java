package edu.ucla.belief.ui.dialogs;

import edu.ucla.util.ProgressMonitorable;

import java.awt.Component;
import javax.swing.ProgressMonitor;

/** Display the progress of a ProgressMonitorable task in
	a javax.swing.ProgressMonitor by "polling" the task
	at regular intervals and updating the display.
	Pollster.run() should spend most of its time sleeping,
	so we intend for it to use up very little cpu overhead.

	@author keith cascio
	@since 20060518 */
public class Pollster implements Runnable
{
	public Pollster( String descrip, ProgressMonitorable task, Component parentComponent ){
		myDescription = descrip;
		myTask        = task;
		myParent      = parentComponent;
	}

	public void run(){
		try{
			Thread.sleep( 0x200 );
			for( int i=0; (i<0x10) && myTask.isFinished(); i++ ) Thread.sleep( 0x40 );
		}catch( InterruptedException interruptedexception ){
			return;
		}
		if( myTask.isFinished() || (myTask.getProgressMax()<1) ){
			if( edu.ucla.belief.ui.util.Util.DEBUG_VERBOSE ){
				System.err.println( "warning: Pollster \""+myDescription+"\" failed to poll task \""+myTask.getDescription()+"\"" );
			}
			return;
		}
		String lastNote = myTask.getNote();
		String newNote;
		myProgressMonitor = new ProgressMonitor( myParent, myDescription, lastNote, 0, myTask.getProgressMax() );
		myProgressMonitor.setMillisToDecideToPopup( 0x200 );
		myProgressMonitor.setMillisToPopup(         0x400 );
		boolean interrupted = false;
		try{
			while( !myTask.isFinished() ){
				if( myProgressMonitor.isCanceled() ){
					if( this.myInterruptable != null ) this.myInterruptable.interrupt();
					return;
				}
				newNote = myTask.getNote();
				if( newNote != lastNote ) Pollster.this.myProgressMonitor.setNote( lastNote = newNote );
				Pollster.this.myProgressMonitor.setProgress( myTask.getProgress() );
				Thread.sleep( 0x200 );
			}
		}catch( InterruptedException interruptedexception ){
			interrupted = true;
		}finally{
			//System.out.println( "Pollster.finally myProgressMonitor.isCanceled()? " + myProgressMonitor.isCanceled() );
			try{
				if( Thread.interrupted() ) interrupted = true;
				myProgressMonitor.close();
			}catch( Throwable throwable ){
				System.err.println( "warning! Pollster.run() caught: " + throwable );
			}finally{
				if( interrupted ) Thread.currentThread().interrupt();
			}
		}
	}

	public Thread start(){
		Thread ret = new Thread( this, "pollster " + Integer.toString( INT_COUNTER ) );
		if( myInterruptable != null ) ret.setPriority( myInterruptable.getPriority() );
		ret.start();
		return ret;
	}

	public void setThreadToInterrupt( Thread interruptable ){
		this.myInterruptable = interruptable;
	}

	private String              myDescription;
	private ProgressMonitorable myTask;
	private ProgressMonitor     myProgressMonitor;
	private Component           myParent;
	private Thread              myInterruptable;

	private static       int INT_COUNTER  = 0;
	//public  static final int INT_PRIORITY = ((Thread.NORM_PRIORITY - Thread.MIN_PRIORITY)/(int)2) + Thread.MIN_PRIORITY;
}
