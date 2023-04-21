package edu.ucla.util.code;

import edu.ucla.util.ChangeBroadcaster;

import java.io.PrintStream;

/**
	@author Keith Cascio
	@since 050604
*/
public interface CodeGenius extends ChangeBroadcaster
{
	public String describe();
	public String getShortDescription();
	public String getOutputClassNameDefault();
	public String getOutputClassName();
	public void setOutputClassName( String name );
	public String describeDependencies();
	public void describeDependencies( Tree tree );
	public void writeCode( PrintStream out );
	public CodeOption[] getOptions();
	public CodeOptionValue getOption( CodeOption option );
	public void setOption( CodeOption option, CodeOptionValue value );
	public boolean getFlag( CodeOption option );
	public void setFlag( CodeOption option, boolean value );
	public void resetOptions();
	public String getIconFileName();

	/** @since 20051107 */
	public Object getWarnings();

	/** @since 20060327 */
	public OptionBreadth getOptionBreadth();
	public CodeOptionValue breadth();
	public boolean isCompilable();
}
