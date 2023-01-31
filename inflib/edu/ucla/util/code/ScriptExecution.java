package edu.ucla.util.code;

import java.io.*;

/** @author Keith Cascio
	@since 033105 */
public class ScriptExecution
{
	public ScriptExecution(
			Process process,
			Script script,
			String messageSuccess,
			String description,
			String[] cmdarray,
			File wd,
			ScriptGenius lang,
			SystemSoftwareSource source )
	{
		this.myProcess = process;
		this.myScript = script;
		this.myMessageSuccess = messageSuccess;
		this.myDescription = description;
		this.myCommandArray = cmdarray;
		this.myWorkingDirectory = wd;
		this.myLanguage = lang;
		this.mySystemSoftwareSource = source;

		this.myExitValue = null;
		this.myFlagTimedOut = false;
		this.myFlagInterrupted = false;
	}

	public void sit( ProcessCustodian custodian, long timeout ){
		new ProcessSitter( ScriptExecution.this, custodian, timeout ).start();
	}

	public interface ProcessCustodian{
		public void processOutcome( ScriptExecution execution );
	}

	public static class ProcessSitter implements Runnable{
		public ProcessSitter( ScriptExecution execution, ProcessCustodian custodian, long timeout ){
			this.myBaby = execution;
			this.myCustodian = custodian;
			this.myTimeout = timeout;
		}

		public void start(){
			new Thread( (Runnable)this, "ProcessSitter for " + myBaby.getDescription() ).start();
			spawnTimeKeeper();
		}

		public void run(){
			int exitval = 69;
			boolean flagInterrupted = false;
			try{
				exitval = myBaby.getProcess().waitFor();
			}catch( InterruptedException interruptedexception ){
				Thread.currentThread().interrupt();
				flagInterrupted = true;
				System.err.println( "Warning: " + interruptedexception );
			}
			notifyImpl( exitval, false, flagInterrupted );
			synchronized( this ){
				if( myFlagTimeout ) return;
			}
			myCustodian.processOutcome( myBaby );
		}

		private void notifyImpl( int exitval, boolean timedout, boolean interrupted ){
			synchronized( this ){
				ProcessSitter.this.myExitValue = new Integer( exitval );
				myBaby.setOutcome( exitval, timedout, interrupted );
				this.notifyAll();
			}
		}

		private void waitImpl(){
			boolean flagInterrupted = false;
			synchronized( this ){
				try{
					this.wait( myTimeout );
				}catch( InterruptedException interruptedexception ){
					Thread.currentThread().interrupt();
					flagInterrupted = true;
					System.err.println( "Warning: " + interruptedexception );
				}
				if( ProcessSitter.this.myExitValue == null ){
					myFlagTimeout = true;
					myBaby.setOutcome( -1, true, flagInterrupted );
				}
				else return;
			}
			myCustodian.processOutcome( myBaby );
		}

		private void spawnTimeKeeper(){
			Runnable timekeeper = new Runnable(){
				public void run(){
					ProcessSitter.this.waitImpl();
				}
			};
			new Thread( timekeeper, "time keeper for " + myBaby.getDescription() ).start();
		}

		private ScriptExecution myBaby;
		private ProcessCustodian myCustodian;
		private final long myTimeout;
		private Integer myExitValue = null;
		private volatile boolean myFlagTimeout = false;
	}

	public void pipe( PrintWriter writer ){
		pipe( myProcess, writer );
	}

	public static void pipe( Process process, PrintWriter writer ){
		if( process == null ) return;

		InputStream stdout = process.getInputStream();
		InputStream stderr = process.getErrorStream();

		new Pipe( stdout, writer ).start();
		new Pipe( stderr, writer ).start();
	}

	public static class Pipe implements Runnable{
		public Pipe( InputStream in, PrintWriter out ){
			this.myIn = new BufferedReader( new InputStreamReader( in ) );
			this.myOut = out;
		}

		public void start(){
			new Thread( (Runnable)this ).start();
		}

		public void run(){
			String line;
			try{
				while( (line = myIn.readLine()) != null )
					myOut.println( line );
			}catch( IOException ioexception ){
				System.err.println( "Warning in Pipe.run(): " + ioexception );
			}
		}

		private BufferedReader myIn;
		private PrintWriter myOut;
	}

	public Process getProcess(){
		return this.myProcess;
	}

	public Script getScript(){
		return this.myScript;
	}

	public String[] getCommandArray(){
		return this.myCommandArray;
	}

	public String getCommand(){
		StringBuffer buff = new StringBuffer( 128 );
		for( int i=0; i<myCommandArray.length; i++ ){
			buff.append( myCommandArray[i] );
			buff.append( " " );
		}
		buff.setLength( buff.length() - 1 );
		return buff.toString();
	}

	public File getWorkingDirectory(){
		return myWorkingDirectory;
	}

	public ScriptGenius getLanguage(){
		return myLanguage;
	}

	public SystemSoftwareSource getSystemSoftwareSource(){
		return mySystemSoftwareSource;
	}

	public Integer getExitValue(){
		return myExitValue;
	}

	public boolean isTimedOut(){
		return myFlagTimedOut;
	}

	public boolean isInterrupted(){
		return myFlagInterrupted;
	}

	public boolean isError(){
		return myFlagTimedOut || myFlagInterrupted || (myExitValue == null) || (myExitValue.intValue() != 0);
	}

	public void setOutcome( int exitValue, boolean timedout, boolean interrupted ){
		this.myExitValue = new Integer( exitValue );
		this.myFlagTimedOut = timedout;
		this.myFlagInterrupted = interrupted;
	}

	public String getDescription(){
		return myDescription;
	}

	public String getMessage(){
		if( myFlagInterrupted ) return myDescription + " interrupted.";
		else if( myFlagTimedOut ) return myDescription + " timed out, something might be wrong.";
		else if( myExitValue != null ){
			int exitval = myExitValue.intValue();
			String ret = myMessageSuccess;
			if( exitval == 0 ) ret += ", success!";
			else ret += ", returned error value " + myExitValue.toString() + ".";
			return ret;
		}
		else return myDescription + ": unknown error.";
	}

	private Process myProcess;
	private Script myScript;
	private String myMessageSuccess, myDescription;
	private String[] myCommandArray;
	private File myWorkingDirectory;
	private ScriptGenius myLanguage;
	private SystemSoftwareSource mySystemSoftwareSource;

	private Integer myExitValue;
	private boolean myFlagTimedOut, myFlagInterrupted;
}
