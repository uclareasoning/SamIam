package edu.ucla.belief.io.dsl;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.util.ProgressMonitorable;
//{superfluous} import edu.ucla.util.CompoundTask;

import java.io.*;

/** @author keith cascio
	@since 20020521 */
public class RunReadSMILE extends RunReadNetwork
{
	public RunReadSMILE( File f, FileType type, NetworkIO.BeliefNetworkIOListener bnil )
	{
		super( f.getPath(), bnil );
		this.myFile = f;
		this.myType = type;
		if( myType == null ) myType = FileType.getTypeForFile( f );
	}

	public File getFile(){
		return this.myFile;
	}

	public FileType getFileType(){
		return this.myType;
	}

	public Estimate getEstimator(){
		return null;
	}

	public ProgressMonitorable getReadTask(){
		return null;
	}

	public BeliefNetwork beliefNetwork() throws Exception{
		GenieNet    ret     = null;
		SMILEReader sr      = new SMILEReader();
		boolean     success = sr.loadSMILE( myFile, myType );
		if( Thread.currentThread().isInterrupted() ) return null;

		if( success ) ret = sr.getBeliefNetwork();
		else          ret = null;
		return ret;
	}

	public void finishedReading(){
		//if( myConstructionTask != null ) myConstructionTask.setFinished( true );
	}

	public String errorMessage( Throwable throwable ) throws Throwable{
		try{
			throw throwable;
		}
		catch( UnsatisfiedLinkError linkerr ){
			return "Cannot locate the library 'callsmile'.";
		}
		catch( NoSuchMethodException e ){
			return "Found wrong version of the library 'callsmile'.";
		}
	}

	private File                    myFile;
	private FileType                myType;
}
