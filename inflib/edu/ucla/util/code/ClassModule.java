package edu.ucla.util.code;

import java.util.LinkedList;
import java.io.File;

/** @author Keith Cascio
	@since 032405 */
public class ClassModule extends SoftwareEntity
{
	public ClassModule( String jarname, String classname, String descriptionshort, String descriptionverbose ){
		super( descriptionshort, descriptionverbose );
		this.myJarName = jarname;
		this.myClassName = classname;
	}

	public static ClassModule getInflibModule(){
		if( MODULE_INFLIB == null ){
			String jarname = "inflib.jar";
			String classname = "edu" + File.separator + "ucla" + File.separator + "belief" + File.separator + "BeliefNetwork.class";
			String descriptionshort = "inflib";
			String descriptionverbose = "SamIam's inference library";
			MODULE_INFLIB = new ClassModule( jarname, classname, descriptionshort, descriptionverbose );
		}
		return MODULE_INFLIB;
	}
	private static ClassModule MODULE_INFLIB;

	public String getJarName(){
		return myJarName;
	}

	public String getClassName(){
		return myClassName;
	}

	public File guessLocation() throws Exception {
		String userclasspath = System.getProperty( "java.class.path" );
		if( (userclasspath == null) || (userclasspath.length() < 1) ) return null;
		//String pathsep = System.getProperty( "path.separator" );
		String[] paths = userclasspath.split( "\\Q" + File.pathSeparator + "\\E" );

		LinkedList possibilities = new LinkedList();
		String path;
		for( int i=0; i<paths.length; i++ ){
			path = paths[i].trim().toLowerCase();
			if( path.endsWith( myJarName ) ){
				File jarfile = new File( paths[i] );
				possibilities.add( jarfile );
			}
			else{
				File dir = new File( paths[i] );
				if( dir.isDirectory() ){
					File fileBeliefNetwork = new File( paths[i] + File.separator + myClassName );
					if( fileBeliefNetwork.exists() ) possibilities.add( dir );
				}
			}
		}

		int num = possibilities.size();
		if( num == 0 ) return null;
		else if( num == 1 ) return (File) possibilities.iterator().next();
		else throw new IllegalStateException();
	}

	private String myJarName, myClassName;
}
