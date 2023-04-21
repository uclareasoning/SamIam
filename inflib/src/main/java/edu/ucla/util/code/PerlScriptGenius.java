package edu.ucla.util.code;

import java.io.*;

/** @author Keith Cascio
	@since 032905 */
public class PerlScriptGenius extends AbstractScriptGenius implements ScriptGenius
{
	public void writeHeader( PrintStream out ){
		out.println( "#!/usr/bin/perl" );
		super.writeHeader( out );
	}

	public void writeCommandPrep( PrintStream out, String command ){
		super.writeCommandPrep( out, command );
		out.println( "print( \""+escapeQuotes( command )+"\\n\" );" );
	}

	//public void writeCommandPost( PrintStream out, String command ){}

	public void writeArgValidation( Script script, PrintStream out ){
		out.println( "if( $#ARGV < 0 ){" );
		out.println( "  print( \"" + script.getDescriptionComment() + "\\n\" );" );
		out.println( "  die( \"usage: $0 "+script.getUsage()+", e.g. $0 "+script.getUsageExample()+"\\n\" );" );
		out.println( "}" );
	}

	//public void writeTail( Script script, PrintStream out ){}

	public String formSystemCall( String[] cmdarray ){
		if( (cmdarray == null) || (cmdarray.length < 1) ) return "";

		String ret = "system " + quote( cmdarray[0] );
		for( int i=1; i<cmdarray.length; i++ ){
			ret += ", " + quote( cmdarray[i] );
		}
		return ret;
	}

	public String getWildArgumentToken(){
		return "@ARGV";
	}

	public String getCommentToken(){
		return "#";
	}

	public String getScriptFileExtension(){
		return "perl";
	}

	public String getScriptLanguageDescription(){
		return "Perl (*.perl)";
	}
}
