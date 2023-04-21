package edu.ucla.util.code;

import java.util.*;
import java.io.*;

/** Tests each of the ScriptGenius sub classes
	by writing scripts.

	@author Keith Cascio
	@since 032905 */
public class ScriptTest
{
	public static final String STR_FLAG_MINUS = "--";
	public static final String STR_FLAG_PLUS = "++";

	public static void main( String[] args )
	{
		ScriptTest test = null;
		Throwable thrown = null;
		try{
			(test = new ScriptTest( args )).testRoot();
		}catch( Throwable throwable ){
			thrown = throwable;
		}

		if( thrown != null ) System.exit( fail( null, thrown ) );
	}

	public ScriptTest( String[] args ){
		this.myArgs = args;
	}

	public void testRoot() throws Exception
	{
		init();

		List listGenii = new LinkedList();

		if( shouldRun( "bat" ) ) listGenii.add( new BatchScriptGenius() );
		if( shouldRun( "sh" ) ) listGenii.add( new BourneScriptGenius() );
		if( shouldRun( "csh" ) ) listGenii.add( new CShellScriptGenius() );
		if( shouldRun( "perl" ) ) listGenii.add( new PerlScriptGenius() );

		if( listGenii.isEmpty() ) return;

		ScriptGenius[] genii = (ScriptGenius[]) listGenii.toArray( new ScriptGenius[ listGenii.size() ] );
		Script[] scripts = new Script[] { new JDKRun(), new JDKCompile() };

		List written = new LinkedList();

		for( int i=0; i<genii.length; i++ ){
			for( int j=0; j<scripts.length; j++ ){
				written.add( write( genii[i], scripts[j] ) );
			}
		}

		java.io.PrintStream streamVerbose = System.out;
		streamVerbose.println( "Wrote:" );
		for( Iterator it = written.iterator(); it.hasNext(); ){
			streamVerbose.println( ((File)it.next()).getAbsolutePath() );
		}
	}

	private File write( ScriptGenius genius, Script script ) throws Exception {
		if( mySystemSoftwareSource == null ) mySystemSoftwareSource = new GuessingSource();
		if( myDestination == null ) myDestination = new File( "." );
		String path = myDestination.getAbsolutePath() + File.separator + script.getDefaultFileName() + "." + genius.getScriptFileExtension();
		File ofile = new File( path );
		script.write( genius, mySystemSoftwareSource, ofile );
		return ofile;
	}

	private void init(){
		myPlus = new HashSet( myArgs.length );
		myMinus = new HashSet( myArgs.length );

		for( int i=0; i<myArgs.length; i++ ){
			if( myArgs[i].startsWith( STR_FLAG_MINUS ) ){
				myMinus.add( myArgs[i].substring( STR_FLAG_MINUS.length() ) );
			}
			else if( myArgs[i].startsWith( STR_FLAG_PLUS ) ){
				myPlus.add( myArgs[i].substring( STR_FLAG_PLUS.length() ) );
			}
		}
	}

	public boolean shouldRun( String language ){
		if( myMinus.contains( language ) ) return false;
		else if( myPlus.isEmpty() || myPlus.contains( language ) ) return true;
		else return false;
	}

	public static int fail( String message, Throwable throwable ){
		System.err.println( "Fail" );
		if( throwable != null ){
			throwable.printStackTrace();
			System.err.println( "Fail" );
		}
		return -1;
	}

	private String[] myArgs;
	private Set myPlus, myMinus;

	private File myDestination;
	private SystemSoftwareSource mySystemSoftwareSource;
}
