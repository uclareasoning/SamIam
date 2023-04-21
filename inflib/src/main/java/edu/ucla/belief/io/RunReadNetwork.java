package edu.ucla.belief.io;

import edu.ucla.belief.*;
import edu.ucla.util.ProgressMonitorable;
//{superfluous} import edu.ucla.util.CompoundTask;
//import edu.ucla.util.JVMTI;

import java.io.*;

/** @author keith cascio
	@since 20020524 */
public abstract class RunReadNetwork implements Runnable
{
	public RunReadNetwork( String descrip, NetworkIO.BeliefNetworkIOListener bnil ){
		this.myDescription = descrip;
		this.myBNIL        = bnil;
	}

	abstract public File getFile();

	abstract public FileType getFileType();

	abstract public Estimate getEstimator();

	abstract public ProgressMonitorable getReadTask();

	abstract public BeliefNetwork beliefNetwork() throws Exception;

	abstract public String errorMessage( Throwable throwable ) throws Throwable;

	abstract public void finishedReading();

	public String[] getSyntaxErrors(){
		return null;
	}

	final public void run()
	{
		//System.out.println();
		//System.out.println( myDescription );

		BeliefNetwork ret    = null;
		String        errmsg = null;

		//long start = JVMTI.getCurrentThreadCpuTimeUnsafe();

		try{
			ProgressMonitorable readtask = getReadTask();
			if( (RunReadNetwork.this.myBNIL != null) && (readtask != null) )
				RunReadNetwork.this.myBNIL.handleProgress( readtask, getEstimator() );
		}catch( Throwable throwable ){
			System.err.println( "warning, failed to monitor progress of network read operation because " + getClass().getName() + ".run() caught " + throwable );
		}

		//long mid0 = JVMTI.getCurrentThreadCpuTimeUnsafe();

		try{
			ret = myResult = RunReadNetwork.this.beliefNetwork();

			try{
				Class                    clazz  = Class.forName( "edu.ucla.belief.approx.Macros$Recoverables" );
				java.lang.reflect.Method method = clazz.getDeclaredMethod( "postProcess", new Class[]{ BeliefNetwork.class } );
				method.invoke( null, new Object[]{ ret } );
			}catch( Throwable throwable ){
				if( Definitions.DEBUG ){ throwable.printStackTrace( System.err ); }
			}
		}
		catch( Throwable throwable ){
			errorStream( throwable );
			try{
				errmsg = errorMessage( throwable );
			}
			catch( OutOfMemoryError oome ){
				errmsg = "The file " + myDescription + " is too big to open.";
			}
			catch( Throwable e ){
				//errmsg = e.getMessage();
				//if( errmsg == null )
				errmsg = e.toString();
			}
		}
		finally{
			finishedReading();
		}

		//long middle = JVMTI.getCurrentThreadCpuTimeUnsafe();
		if( Thread.interrupted() ){
			if( myBNIL != null ) myBNIL.handleCancelation();
			Thread.currentThread().interrupt();
			return;
		}

		if( myBNIL != null ){
			if( ret == null ) myBNIL.handleBeliefNetworkIOError( "Error opening "+getFormatDescription()+" file " + myDescription + "\n" + errmsg );
			else
			{
				if( ret.checkValidProbabilities() ){
					myBNIL.handleNewBeliefNetwork( ret, getFile() );

					String[] errors = getSyntaxErrors();
					if( errors != null ){
						myBNIL.handleSyntaxErrors( errors, getFileType() );
					}
				}
				else myBNIL.handleBeliefNetworkIOError( "Error opening "+getFormatDescription()+" file " + myDescription + "\nProbabilities out of range." );
			}
		}

		//long end = JVMTI.getCurrentThreadCpuTimeUnsafe();

		//long estim = mid0 - start;
		//long first = middle - mid0;
		//long last  = end - middle;
		//double total = (double) (end - start);

		//double estimFrac = ((double)estim) / total;
		//double firstFrac = ((double)first) / total;
		//double lastFrac  = ((double)last)  / total;

		//double estimCost = estimFrac / firstFrac;

		//System.out.println( "RunReadNetwork.run()\n    estimation              : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(estimFrac) + " (" + NetworkIO.formatTime(estim) + ") ("+NetworkIO.FORMAT_PROFILE_PERCENT.format(estimCost)+" compared to parse time),\n    parsing                 : " + NetworkIO.FORMAT_PROFILE_PERCENT.format(firstFrac) + " (" + NetworkIO.formatTime(first) + "),\n    handleNewBeliefNetwork(): " + NetworkIO.FORMAT_PROFILE_PERCENT.format(lastFrac) + " (" + NetworkIO.formatTime(last) + ")" );
		//System.out.println();
	}

	/** @since 20091204 */
	final public BeliefNetwork getResult(){
		return myResult;
	}

	/** @since 20091204 */
	final public BeliefNetwork computeResult(){
		run();
		return getResult();
	}

	final public String getDescription(){
		return myDescription;
	}

	final public String getFormatDescription(){
		String strFormatName = "";
		FileType filetype = getFileType();
		if( filetype != null ) strFormatName = filetype.getName();
		return strFormatName;
	}

	final public Thread start(){
		ThreadGroup group = myBNIL.getThreadGroup();
		if( group == null ) group = Thread.currentThread().getThreadGroup();
		Thread ret = new Thread( group, RunReadNetwork.this, "read "+getFormatDescription()+" file " + Integer.toString( INT_COUNTER++ ) );
		ret.setPriority( NetworkIO.INT_PRIORITY_READ );
		ret.start();
		return ret;
	}

	final public void errorStream( Throwable throwable ){
		if( Definitions.DEBUG ){
			Definitions.STREAM_VERBOSE.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
			throwable.printStackTrace();
		}
		else System.err.println( throwable.toString() );
	}

	private static int INT_COUNTER = 0;

	private String                     myDescription;
	private NetworkIO.BeliefNetworkIOListener myBNIL;
	private BeliefNetwork              myResult;
}
