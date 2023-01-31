package edu.ucla.belief.io.xmlbif;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.util.ProgressMonitorable;
//{superfluous} import edu.ucla.util.CompoundTask;

//import java.lang.reflect.*;
import java.io.*;

/** Support for reading the XML BIF format.
	You can use this class threaded by calling start(),
	or in the current thread by calling beliefNetwork().
	Currently the parse class edu.ucla.belief.io.xmlbif.XmlbifParser
	relies on Java 5.
	beliefNetwork() will throw a RuntimeException
	caused by a UnsupportedClassVersionError if you try to
	run using JRE v 4 or earlier.

	@author keith cascio
	@since 20060524 */
public class RunReadBIF extends RunReadNetwork
{
	public static final String STR_CLASSNAME_PARSER_ORIG = "edu.ucla.belief.io.xmlbif.XmlbifParser";
	public static final String STR_CLASSNAME_UAI06PARSER = "edu.ucla.belief.io.xmlbif.UAI06Parser";
	public static final String STR_CLASSNAME_PARSER      = STR_CLASSNAME_PARSER_ORIG;
	public static final String STR_CLASSNAME_ESTIMATOR   = "edu.ucla.belief.io.xmlbif.SkimmerEstimator";

	public RunReadBIF( File f, NetworkIO.BeliefNetworkIOListener bnil )
	{
		super( f.getPath(), bnil );
		this.myFile = f;
	}

	public interface MonitorableReusableParser
	{
		public BeliefNetwork  beliefNetwork( File        input, NodeLinearTask task ) throws Exception;
		public BeliefNetwork  beliefNetwork( InputStream input, NodeLinearTask task ) throws Exception;
		public void           cleanup();
		public String[]       getSyntaxErrors();
		public void           setHighPerformance( boolean flag );
	}

	public String[] getSyntaxErrors(){
		MonitorableReusableParser parser = getParser();
		if( parser == null ) return null;
		else                 return parser.getSyntaxErrors();
	}

	public File getFile(){
		return this.myFile;
	}

	public FileType getFileType(){
		return FileType.XMLBIF_INFLIB;
	}

	private static Class CLAZZ_ESTIMATOR;

	public Estimate getEstimator(){
		if( myEstimator == null ){
			//myEstimator = new SkimmerEstimator( this.myFile );
			Object instance = null;
			try{
				if( CLAZZ_ESTIMATOR == null ) CLAZZ_ESTIMATOR = Class.forName( STR_CLASSNAME_ESTIMATOR );
				/*Constructor constructor = CLAZZ_ESTIMATOR.getConstructor( new Class[] { File.class } );
				instance    = constructor.newInstance( new Object[] { this.myFile } );*/
				instance = CLAZZ_ESTIMATOR.newInstance();
			}catch( UnsupportedClassVersionError unsupportedclassversionerror ){
				throw new RuntimeException( "<html><nobr><b>XML BIF requires Java version <font color=\"#cc0000\">5</font> or higher.  Please update to the latest version JRE.", unsupportedclassversionerror );
			}catch( Throwable throwable ){
				throw new RuntimeException( "could not instantiate " + STR_CLASSNAME_ESTIMATOR + ", " + throwable.toString(), throwable );
			}

			if( instance instanceof Estimate ) myEstimator = (Estimate)instance;
			else throw new RuntimeException( "unexpected problem, " + STR_CLASSNAME_ESTIMATOR + " does not implement " + Estimate.class.getName() );

			myEstimator.init( this.myFile );
			myEstimator.estimate();
			/*try{
				Method method = CLAZZ_ESTIMATOR.getDeclaredMethod( "estimate", (Class[])null );
				method.invoke( instance, (Object[])null );
			}catch( NoSuchMethodException nosuchmethodexception ){
				throw new RuntimeException( "could not use " + STR_CLASSNAME_ESTIMATOR + ", no such method, " + nosuchmethodexception.toString(), nosuchmethodexception );
			}catch( IllegalAccessException illegalaccessexception ){
				throw new RuntimeException( "could not use " + STR_CLASSNAME_ESTIMATOR + ", illegal access, " + illegalaccessexception.toString(), illegalaccessexception );
			}catch( InvocationTargetException invocationtargetexception ){
				throw new RuntimeException( "could not use " + STR_CLASSNAME_ESTIMATOR + ", invocation target, " + invocationtargetexception.toString(), invocationtargetexception );
			}*/
		}
		return myEstimator;
	}

	public ProgressMonitorable getReadTask(){
		if( myTask == null ){
			myTask = new NodeLinearTask( "read xml bif", getEstimator(), 4, new String[] { "parsing variables", "parsing potentials", "making cpts", "inducing graph" } );
		}
		return myTask;
	}

	public BeliefNetwork beliefNetwork() throws Exception{
		MonitorableReusableParser parser = getParser();
		try{
			parser.setHighPerformance( false );
		}catch( Throwable throwable ){
			System.err.println( "warning: failed requesting low performance (i.e. validation) " + throwable );
		}
		BeliefNetwork ret = parser.beliefNetwork( myFile, myTask );
		parser.cleanup();
		if( Thread.currentThread().isInterrupted() ) return null;
		return ret;
	}

	public void finishedReading(){
		if( myTask != null ) myTask.setFinished( true );
		//if( myTask != null ) System.out.println( "progress "  + myTask.getProgress() + "/" + myTask.getProgressMax() );
	}

	public String errorMessage( Throwable throwable ) throws Throwable{
		try{
			throw throwable;
		}
		catch( org.xml.sax.SAXParseException saxparseexception ){
			return "BIF xml syntax error: " + saxparseexception.getMessage();
		}
		catch( org.xml.sax.SAXException saxexception ){
			return "Failed to parse BIF xml: " + saxexception.getMessage();
		}
	}

	public MonitorableReusableParser getParser(){
		if( myParser == null ){
			//if( myParser == null ) myParser = new XmlbifParser();
			Object instance = null;
			try{
				Class clazz = Class.forName( STR_CLASSNAME_PARSER );
				instance    = clazz.newInstance();
			}catch( UnsupportedClassVersionError unsupportedclassversionerror ){
				throw new RuntimeException( "<html><nobr><b>XML BIF requires Java version <font color=\"#cc0000\">5</font> or higher.  Please update to the latest version JRE.", unsupportedclassversionerror );
			}catch( Throwable throwable ){
				throw new RuntimeException( "could not instantiate " + STR_CLASSNAME_PARSER + ", " + throwable.getMessage(), throwable );
			}

			if( instance instanceof MonitorableReusableParser ) myParser = (MonitorableReusableParser)instance;
			else throw new RuntimeException( "unexpected problem, " + STR_CLASSNAME_PARSER + " does not implement " + MonitorableReusableParser.class.getName() );
		}
		return myParser;
	}

	private MonitorableReusableParser myParser;
	private File                      myFile;
	private NodeLinearTask            myTask;
	private Estimate        myEstimator;
}
