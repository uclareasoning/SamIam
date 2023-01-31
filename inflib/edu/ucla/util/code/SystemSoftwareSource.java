package edu.ucla.util.code;

import java.io.File;

/** @author Keith Cascio
	@since 032405 */
public interface SystemSoftwareSource
{
	public File getPath( SoftwareEntity entity ) throws Exception;
	public File getClasspath( ClassModule module ) throws Exception;
	public File getExecutablePath( JDKTool tool ) throws Exception;
}
