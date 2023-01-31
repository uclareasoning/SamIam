package edu.ucla.util.code;

import java.io.*;

/** @author Keith Cascio
	@since 032905 */
public class CShellScriptGenius extends AbstractScriptGenius implements ScriptGenius
{
	public void writeHeader( PrintStream out ){
		out.println( "#!/usr/bin/csh" );
		super.writeHeader( out );
	}

	public void writeCommandPrep( PrintStream out, String command ){
		super.writeCommandPrep( out, command );
		out.println( "echo " + command );
	}

	//public void writeCommandPost( PrintStream out, String command ){}

	public void writeArgValidation( Script script, PrintStream out ){
		out.println( "if ( \"$1\" == \"\" ) then" );
		out.println( "  echo \"" + script.getDescriptionComment() + "\"" );
		out.println( "  echo \"usage: $0 "+script.getUsage()+", e.g. $0 "+script.getUsageExample()+"\"" );
		out.println( "  exit" );
		out.println( "endif" );
	}

	//public void writeTail( Script script, PrintStream out ){}

	public String getWildArgumentToken(){
		return "$*";
	}

	public String getCommentToken(){
		return "#";
	}

	public String getScriptFileExtension(){
		return "csh";
	}

	public String getScriptLanguageDescription(){
		return "Unix C Shell (*.csh)";
	}
}
