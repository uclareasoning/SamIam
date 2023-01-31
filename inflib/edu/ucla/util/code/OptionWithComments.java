package edu.ucla.util.code;

/**
	@author Keith Cascio
	@since 050604
*/
public class OptionWithComments extends AbstractCodeOption implements CodeOption
{
	private static final OptionWithComments INSTANCE = new OptionWithComments();

	public static final OptionWithComments getInstance(){
		return INSTANCE;
	}

	private OptionWithComments(){}

	public String describe(){
		return "With comments";
	}

	public String describe( boolean flag ){
		return flag ? "with comments" : "no comments";
	}

	public String getHelpText(){
		return "With comments? - Choose whether or not to include descriptive comments in the output code.";
	}

	public boolean isFlag(){
		return true;
	}

	public boolean getDefaultFlag(){
		return true;
	}
}
