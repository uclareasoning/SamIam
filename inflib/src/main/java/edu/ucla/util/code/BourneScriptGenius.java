package edu.ucla.util.code;

import java.io.*;
import java.util.*;

/** @author Keith Cascio
	@since 032405 */
public class BourneScriptGenius extends AbstractScriptGenius implements ScriptGenius
{
	public void writeHeader( PrintStream out ){
		out.println( "#!/usr/bin/sh" );
		super.writeHeader( out );
	}

	public void writeCommandPrep( PrintStream out, String command ){
		super.writeCommandPrep( out, command );
		out.println( "echo \""+escapeQuotes( command )+"\"" );
	}

	//public void writeCommandPost( PrintStream out, String command ){}

	public void writeArgValidation( Script script, PrintStream out ){
		out.println( "if test -z \"$1\"; then" );
		out.println( "  echo \"" + script.getDescriptionComment() + "\"" );
		out.println( "  echo \"usage: $0 "+script.getUsage()+", e.g. $0 "+script.getUsageExample()+"\"" );
		out.println( "  exit" );
		out.println( "fi" );
	}

	//public void writeTail( Script script, PrintStream out ){}

	public String getWildArgumentToken(){
		return "$*";
	}

	public String getCommentToken(){
		return "#";
	}

	public String getScriptFileExtension(){
		return "sh";
	}

	public String getScriptLanguageDescription(){
		return "Unix Bourne Shell (*.sh)";
	}

	/** @since 033005 */
	public static boolean runtimeSupports( String command ){
		if( (command == null) || (command.length() < 1) ) return false;
		if( myMapCommandsToFlags == null ) myMapCommandsToFlags = new HashMap();

		Map map = myMapCommandsToFlags;
		if( map.containsKey( command ) ) return ((Boolean)map.get( command )).booleanValue();
		else{
			Boolean flag = Boolean.FALSE;
			try{
				Runtime.getRuntime().exec( command );
				flag = Boolean.TRUE;
			}catch( IOException ioexception ){
				flag = Boolean.FALSE;
			}catch( Throwable throwable ){
				flag = Boolean.FALSE;
			}
			map.put( command, flag );
			return flag.booleanValue();
		}
	}

	private static Map myMapCommandsToFlags;
}
