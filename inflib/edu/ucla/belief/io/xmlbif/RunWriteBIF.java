package edu.ucla.belief.io.xmlbif;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
//import edu.ucla.util.ProgressMonitorable;

import java.io.*;

/** Support for writing the XML BIF format.
	Currently the write class edu.ucla.belief.io.xmlbif.XmlbifWriter
	relies on Java 5.
	run()/getWriter() will throw a RuntimeException
	caused by a UnsupportedClassVersionError if you try to
	run using JRE v 4 or earlier.

	@author keith cascio
	@since  20060525 */
public class RunWriteBIF implements Runnable
{
	public static final String STR_CLASSNAME_WRITER = "edu.ucla.belief.io.xmlbif.XmlbifWriter";

	public RunWriteBIF( File output, BeliefNetwork bn )
	{
		myFile          = output;
		myBeliefNetwork = bn;
	}

	public interface BeliefNetworkWriter
	{
		public boolean write( BeliefNetwork bn, PrintStream out );
	}

	public File getFile(){
		return this.myFile;
	}

	public FileType getFileType(){
		return FileType.XMLBIF_INFLIB;
	}

	public boolean getResult(){
		return myResult;
	}

	public void run(){
		BeliefNetworkWriter writer = getWriter();
		try{
			if( writer != null ) myResult = writer.write( myBeliefNetwork, new PrintStream( new FileOutputStream( myFile ) ) );
		}catch( IOException exception ){
			throw new RuntimeException( exception );
		}
	}

	public BeliefNetworkWriter getWriter(){
		if( myWriter == null ){
			Object instance = null;
			try{
				Class clazz = Class.forName( STR_CLASSNAME_WRITER );
				instance    = clazz.newInstance();
			}catch( UnsupportedClassVersionError unsupportedclassversionerror ){
				throw new RuntimeException( "<html><nobr><b>XML BIF requires Java version <font color=\"#cc0000\">5</font> or higher.  Please update to the latest version JRE.", unsupportedclassversionerror );
			}catch( Throwable throwable ){
				throw new RuntimeException( "could not instantiate " + STR_CLASSNAME_WRITER + ", " + throwable.getMessage(), throwable );
			}

			if( instance instanceof BeliefNetworkWriter ) myWriter = (BeliefNetworkWriter)instance;
			else throw new RuntimeException( "unexpected problem, " + STR_CLASSNAME_WRITER + " does not implement " + BeliefNetworkWriter.class.getName() );
		}
		return myWriter;
	}

	private BeliefNetworkWriter myWriter;
	private File                myFile;
	private BeliefNetwork       myBeliefNetwork;
	private boolean             myResult = false;
}
