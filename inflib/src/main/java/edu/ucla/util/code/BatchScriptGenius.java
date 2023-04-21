package edu.ucla.util.code;

import java.io.*;

/** @author Keith Cascio
	@since 032405 */
public class BatchScriptGenius extends AbstractScriptGenius implements ScriptGenius
{
	public void writeHeader( PrintStream out ){
		out.println( "@echo off" );
		super.writeHeader( out );
	}

	public void writeCommandPrep( PrintStream out, String command ){
		super.writeCommandPrep( out, command );
		out.println( "@echo on" );
	}

	public void writeCommandPost( PrintStream out, String command ){
		out.println( "@echo off" );
		out.println( "goto end" );
	}

	public void writeArgValidation( Script script, PrintStream out ){
		out.println( "if \"%1\"==\"\" goto usage" );
	}

	public void writeTail( Script script, PrintStream out ){
		out.println();
		out.println( ":usage" );
		out.println( "echo " + script.getDescriptionComment() );
		out.println( "echo \"usage: %0 "+script.getUsage()+", e.g. %0 "+script.getUsageExample()+"\"" );
		out.println( "goto end" );
		out.println();
		out.println( ":end" );
	}

	//public String getCompileCommand() throws Exception;
	//public String getRunCommand() throws Exception;

	public String getWildArgumentToken(){
		return "%*";
	}

	public String getCommentToken(){
		return "rem";
	}

	public String getScriptFileExtension(){
		return "bat";
	}

	public String getScriptLanguageDescription(){
		return "Windows Batch (*.bat)";
	}
}
