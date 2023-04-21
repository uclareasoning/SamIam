package edu.ucla.util.code;

import java.io.PrintStream;
import java.io.File;

/** @author Keith Cascio
	@since 032405 */
public interface Script
{
	public String getName();
	public String getDescriptionComment();
	public String getUsage();
	public String getUsageExample();
	public String getDefaultFileName();
	public void write( ScriptGenius genius, SystemSoftwareSource source, PrintStream out ) throws Exception;
	public void write( ScriptGenius genius, SystemSoftwareSource source, File ofile ) throws Exception;
	public SoftwareEntity[] getDependencies();
	public boolean isResolved();
	public boolean isResolved( SystemSoftwareSource source ) throws Exception;
	public ScriptExecution exec( String[] args, File dir, ScriptGenius genius, SystemSoftwareSource source ) throws Exception;
}
