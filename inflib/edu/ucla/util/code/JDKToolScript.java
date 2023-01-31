package edu.ucla.util.code;

import java.io.*;

/** @author Keith Cascio
	@since 032405 */
public abstract class JDKToolScript implements Script
{
	public void write( ScriptGenius genius, SystemSoftwareSource source, PrintStream out ) throws Exception
	{
		String command = getCommand( genius, source );
		genius.writeHeader( out );
		out.println( genius.getCommentToken() + " " + getDescriptionComment() );
		out.println();
		genius.writeArgValidation( this, out );
		genius.writeCommandPrep( out, command );
		out.println( command );
		genius.writeCommandPost( out, command );
		genius.writeTail( this, out );
		out.close();
	}

	public void write( ScriptGenius genius, SystemSoftwareSource source, File ofile ) throws Exception
	{
		PrintStream out = new PrintStream( new FileOutputStream( ofile ) );
		this.write( genius, source, out );
		out.close();
		if( BourneScriptGenius.runtimeSupports( STR_CHMOD ) ){
			String absolute = ofile.getAbsolutePath();
			try{
				String[] cmdarray = new String[] { STR_CHMOD, STR_CHMOD_ARG1, absolute };
				Runtime.getRuntime().exec( cmdarray );
			}catch( Throwable throwable ){
				System.err.println( "Warning: chmod failed for \"" + absolute + "\"" );
			}
		}
	}

	public ScriptExecution exec( String[] args, File dir, ScriptGenius genius, SystemSoftwareSource source ) throws Exception{
		String[] bare = this.getCommandArray( genius, source );

		String[] full = bare;
		if( (args != null) && (args.length > 0) ){
			full = new String[ bare.length + args.length ];
			System.arraycopy( bare, 0, full, 0, bare.length );
			System.arraycopy( args, 0, full, bare.length, args.length );
		}

		String flat = flatten( args );
		String[] envp = (String[])null;
		Process process = Runtime.getRuntime().exec( full, envp, dir );
		return new ScriptExecution( process, (Script)this, getMessageSuccess( flat ), getDescription( flat ), full, dir, genius, source );
	}

	public static final String STR_CHMOD = "chmod";
	public static final String STR_CHMOD_ARG1 = "u+x";

	public String getCommand( ScriptGenius genius, SystemSoftwareSource source ) throws Exception{
		String[] bare = this.getCommandArray( genius, source );

		String[] full = new String[ bare.length + 1 ];
		System.arraycopy( bare, 0, full, 0, bare.length );

		full[ full.length-1 ] = genius.getWildArgumentToken();

		return genius.formSystemCall( full );
	}

	abstract public String[] getCommandArray( ScriptGenius genius, SystemSoftwareSource source ) throws Exception;
	abstract protected SoftwareEntity[] makeDependencies();
	abstract protected String getMessageSuccess( String argsFlat );
	abstract protected String getDescription( String argsFlat );

	public static String flatten( String[] args ){
		if( (args == null) || (args.length<1) ) return "";
		StringBuffer buff = new StringBuffer( args.length*16 );
		for( int i=0; i<args.length; i++ ){
			buff.append( args[i] );
			buff.append( " " );
		}
		buff.setLength( buff.length() - 1 );
		return buff.toString();
	}

	/** @since 20050331 */
	public boolean isResolved( SystemSoftwareSource source ) throws Exception{
		SoftwareEntity[] deps = getDependencies();
		for( int i=0; i<deps.length; i++ ){
			if( source.getPath( deps[i] ) == null ) return false;
		}
		return true;
	}

	/** @since 20091219 */
	public boolean isResolved(){
		SoftwareEntity[] deps = getDependencies();
		for( int i=0; i<deps.length; i++ ){ if( deps[i].getPath() == null ){ return false; } }
		return true;
	}

	public SoftwareEntity[] getDependencies(){
		if( myDependencies == null ) myDependencies = makeDependencies();
		return myDependencies;
	}

	public String toString(){
		return getName();
	}

	private SoftwareEntity[] myDependencies;
}
