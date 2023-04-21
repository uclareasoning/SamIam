package edu.ucla.util.code;

/**
	@author Keith Cascio
	@since 20040602
*/
public class OptionBreadth extends AbstractCodeOption implements CodeOption
{
	public OptionBreadth( String strNameMethod )
	{
		this.myStrNameMethod = strNameMethod;
		this.myStrHelp = "Breadth? - Choose whether to output a complete, compilable Java class, or only the method "+myStrNameMethod+"() for reference.";
		this.myStrMethodOnly = "method "+myStrNameMethod+"() only";
		this.FULL_CLASS = new CodeOptionValue( "complete java class" );
		this.METHOD_ONLY = new CodeOptionValue( myStrMethodOnly );
		this.ARRAY_VALUES_OPTION_AMOUNT = new CodeOptionValue[] { FULL_CLASS, METHOD_ONLY };
	}

	public String describe(){
		return "Breadth";//Extent,Breadth,Scope,Amount of code,Mass,Bulk
	}
	public String getHelpText(){
		return myStrHelp;
	}
	public CodeOptionValue getDefault(){
		return FULL_CLASS;
	}
	public CodeOptionValue[] getValues(){
		return ARRAY_VALUES_OPTION_AMOUNT;
	}

	/** @since 20060327 */
	public boolean booleanValue( CodeOptionValue value ){
		return value == FULL_CLASS;
	}

	public final CodeOptionValue FULL_CLASS;
	public final CodeOptionValue METHOD_ONLY;
	public final CodeOptionValue[] ARRAY_VALUES_OPTION_AMOUNT;
	private String myStrNameMethod;
	private String myStrHelp;
	private String myStrMethodOnly;
}
