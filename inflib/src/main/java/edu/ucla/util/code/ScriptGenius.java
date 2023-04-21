package edu.ucla.util.code;

import java.io.*;

/** @author Keith Cascio
	@since 032405 */
public interface ScriptGenius
{
	//public void writeAllScripts( File directory ) throws Exception;
	//public void writeCompileScript( File ofile ) throws Exception;
	//public void writeRunScript( File ofile ) throws Exception;

	//public String getCompileCommand() throws Exception;
	//public String getRunCommand() throws Exception;

	//public File getInflibClasspath();
	//public File getJavaExecutablePath( String name );

	public String getWildArgumentToken();
	public String getCommentToken();
	public String getScriptFileExtension();
	public String getScriptLanguageDescription();

	public String quote( String command );
	public String escapeQuotes( String str );

	public String formSystemCall( String[] cmdarray );

	public void writeArgValidation( Script script, PrintStream out );
	public void writeTail( Script script, PrintStream out );
	public void writeCommandPrep( PrintStream out, String command );
	public void writeCommandPost( PrintStream out, String command );
	public void writeHeader( PrintStream out );
}
