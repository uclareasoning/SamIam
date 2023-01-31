package edu.ucla.util.code;

/**
	@author Keith Cascio
	@since 050604
*/
public interface CodeOption
{
	public String describe();
	public String describe( boolean flag );
	public String getHelpText();
	public CodeOptionValue getDefault();
	public CodeOptionValue[] getValues();
	public boolean isFlag();
	public boolean getDefaultFlag();
}
