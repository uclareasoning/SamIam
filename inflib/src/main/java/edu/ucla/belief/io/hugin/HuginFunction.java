package edu.ucla.belief.io.hugin;

import java.util.List;

public class HuginFunction
{
	public final String name;
	public final List args;

	public HuginFunction( String name, List args )
	{
		this.name = name;
		this.args = args;
	}
}
