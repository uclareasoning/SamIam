package edu.ucla.belief.io.hugin;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.util.ProgressMonitorable;
import edu.ucla.util.CompoundTask;

import java.io.*;

/** @author keith cascio
	@since 20020521 */
public class RunReadHugin extends RunReadNetwork
{
	public RunReadHugin( File inputfile, NetworkIO.BeliefNetworkIOListener bnil, boolean estimate ){
		super( inputfile.getPath(), bnil );
		this.myFile             = inputfile;
		if( estimate ) this.mySkimmerEstimator = new SkimmerEstimator( inputfile );
	}

	/** @since 20040210 */
	public RunReadHugin( InputStream stream, String name, NetworkIO.BeliefNetworkIOListener bnil, boolean estimate ){
		super( name, bnil );
		this.myInputStream      = stream;

		if( estimate ){
			SkimmerEstimator estimator = null;
			try{
				stream.reset();
				estimator = new SkimmerEstimator( stream );
			}
			catch( IOException ioexception ){
				System.err.println( "warning: cannot estimate parsing work because InputStream not resetable" );
			}
			this.mySkimmerEstimator = estimator;
		}
		this.myHuginReader      = new HuginReader( stream );
	}

	/** @since 20040210 */
	public RunReadHugin( Reader reader, String name, NetworkIO.BeliefNetworkIOListener bnil ){
		super( name, bnil );
		this.myReader           = reader;
		this.mySkimmerEstimator = null;
		this.myHuginReader      = new HuginReader( reader );
	}

	public File getFile(){
		return myFile;
	}

	public FileType getFileType(){
		return FileType.HUGIN;
	}

	public Estimate getEstimator(){
		return RunReadHugin.this.mySkimmerEstimator;
	}

	public ProgressMonitorable getReadTask(){
		try{
			getHuginReader();
		}catch( Exception exception ){
			System.err.println( "warning: RunReadHugin.getReadTask() caught " + exception );
			return null;
		}
		return myReadTask;
	}

	/** @since 20060519 */
	public BeliefNetwork beliefNetwork() throws ParseException, FileNotFoundException{
		BeliefNetwork ret = getHuginReader().beliefNetwork();
		if( Thread.currentThread().isInterrupted() ) return ret;
		NetworkIO.normalizeEnumProperties( ret );
		return ret;
	}

	public void finishedReading(){
		if( myConstructionTask != null ) myConstructionTask.setFinished( true );
	}

	public String errorMessage( Throwable throwable ) throws Throwable{
		try{
			throw throwable;
		}
		catch( ParseException e   ){}
		catch( TokenMgrError  err ){}
		return "The file " + getDescription() + " is not in the recognized "+getFormatDescription()+" format. See stderr for description.";
	}

	private HuginReader getHuginReader() throws FileNotFoundException{
		if( this.myHuginReader == null ){
			if( this.myFile == null ) throw new IllegalStateException();
			this.myHuginReader = new HuginReader( new FileInputStream( this.myFile ) );
		}

		if( RunReadHugin.this.myFlagInitialized ) return this.myHuginReader;

		if( RunReadHugin.this.mySkimmerEstimator != null ){
			RunReadHugin.this.mySkimmerEstimator.estimate();
			this.myHuginReader.setEstimator( RunReadHugin.this.mySkimmerEstimator );

			String[] notes = new String[] { "constructing model..." };
			myConstructionTask = new NodeLinearTask( "BeliefNetwork construction", RunReadHugin.this.mySkimmerEstimator, 1, notes );
			ProgressMonitorable[] tasks   = new ProgressMonitorable[] { this.myHuginReader, myConstructionTask };
			float[]               weights = new               float[] {            1f,               0.1f };
			RunReadHugin.this.myCompoundTask  = new CompoundTask( "read hugin model", tasks, weights );

			myReadTask = RunReadHugin.this.myCompoundTask;
		}
		else myReadTask = this.myHuginReader;

		RunReadHugin.this.myFlagInitialized = true;

		return this.myHuginReader;
	}

	private InputStream         myInputStream;
	private Reader              myReader;
	private File                myFile;
	private SkimmerEstimator    mySkimmerEstimator;
	private HuginReader         myHuginReader;
	private NodeLinearTask      myConstructionTask;
	private CompoundTask        myCompoundTask;
	private ProgressMonitorable myReadTask;
	private boolean             myFlagInitialized = false;
}