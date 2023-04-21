package edu.ucla.util.code;

/**
	@author Keith Cascio
	@since 050604
*/
public class CodeOptionValue
{
	public CodeOptionValue( String toDisplay ){
		this.myToDisplay = toDisplay;
	}

	public String toString(){
		return myToDisplay;
	}

	private String myToDisplay;
}
