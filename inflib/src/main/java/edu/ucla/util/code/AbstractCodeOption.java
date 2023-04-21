package edu.ucla.util.code;

/**
	@author Keith Cascio
	@since 050604
*/
public abstract class AbstractCodeOption implements CodeOption
{
	//public String describe();
	public String describe( boolean flag ){
		return describe();
	}
	public CodeOptionValue getDefault(){
		return (CodeOptionValue)null;
	}
	public CodeOptionValue[] getValues(){
		return (CodeOptionValue[])null;
	}
	public boolean isFlag(){
		return false;
	}
	public boolean getDefaultFlag(){
		return false;
	}
}
