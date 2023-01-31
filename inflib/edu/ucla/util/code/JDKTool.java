package edu.ucla.util.code;

//{superfluous} import java.util.LinkedList;
import java.util.Iterator;
import java.io.File;

/** @author Keith Cascio
	@since 032405 */
public class JDKTool extends SoftwareEntity
{
	public JDKTool( String name, String descriptionshort, String descriptionverbose ){
		super( descriptionshort, descriptionverbose );
		this.myName = name;
	}

	public static JDKTool getJavacTool(){
		if( TOOL_JAVAC == null ){
			String name = "javac";
			String descriptionshort = "javac";
			String descriptionverbose = "the JDK compiler";
			TOOL_JAVAC = new JDKTool( name, descriptionshort, descriptionverbose );
		}
		return TOOL_JAVAC;
	}
	private static JDKTool TOOL_JAVAC;

	public static JDKTool getJavaTool(){
		if( TOOL_JAVA == null ){
			String name = "java";
			String descriptionshort = "java";
			String descriptionverbose = "the JRE virtual machine";
			TOOL_JAVA = new JDKTool( name, descriptionshort, descriptionverbose );
			TOOL_JAVA.appendHint( getJavacTool() );
		}
		return TOOL_JAVA;
	}
	private static JDKTool TOOL_JAVA;

	public String getName(){
		return myName;
	}

	public File guessLocation() throws Exception {
		SoftwareEntity hint;
		File locHint;
		File loc;
		for( Iterator it = getHints().iterator(); it.hasNext(); ){
			hint = (SoftwareEntity) it.next();
			locHint = hint.guessLocationIfNecessary();
			if( locHint != null ){
				loc = getParanoidFileFinder().chooseBestJavaExecutable( myName, locHint );
				if( loc != null ){
					//System.out.println( "Found " + myName + " using hint: " + locHint.getAbsolutePath() );
					return loc.getCanonicalFile();
				}
			}
		}
		return getParanoidFileFinder().chooseBestJavaExecutable( myName );
	}

	public static ParanoidFileFinder getParanoidFileFinder(){
		synchronized( SYNCH ){
			if( FINDER == null ){
				FINDER  = new ParanoidFileFinder();
				FINDER.setDepth( INT_DEPTH_ABANDON );
			}
		}
		return FINDER;
	}

	public static final int INT_DEPTH_ABANDON = (int)3;
	private static ParanoidFileFinder FINDER;
	private static Object             SYNCH = new Object();

	private String myName;
}
