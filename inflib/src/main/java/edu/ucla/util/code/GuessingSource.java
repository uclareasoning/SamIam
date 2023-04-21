package edu.ucla.util.code;

import java.util.*;
import java.io.*;

/** @author Keith Cascio
	@since 032405 */
public class GuessingSource implements SystemSoftwareSource
{
	public File getClasspath( ClassModule module ) throws Exception {
		return getPath( module );
	}

	public void setClasspath( ClassModule module, File classpath ){
		setPath( module, classpath );
	}

	public File getExecutablePath( JDKTool tool ) throws Exception {
		return getPath( tool );
	}

	public void setExecutablePath( JDKTool tool, File executablepath ){
		setPath( tool, executablepath );
	}

	public File getPath( SoftwareEntity entity ) throws Exception {
		return entity.guessLocationIfNecessary();
	}

	public void setPath( SoftwareEntity entity, File path ){
		entity.setPath( path );
	}
}
