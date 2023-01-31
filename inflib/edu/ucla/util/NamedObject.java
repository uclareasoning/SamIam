package edu.ucla.util;

/**
	@author Keith Cascio
	@since 050103
*/
public class NamedObject
{
	public NamedObject( String name )
	{
		myName = name;
	}
	
	final public String toString()
	{
		return myName;
	}
		
	public String myName;
}
