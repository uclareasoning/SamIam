package edu.ucla.belief.recursiveconditioning;

import edu.ucla.belief.dtree.*;

/**
	@author Keith Cascio
	@since 031903
*/
public class FileInfo
{
	public FileInfo()
	{
		stats = new Stats();
	}

	public String toString()
	{
		return super.toString() + ";rcType: " + rcType + ";networkName: " + networkName + ";stats: " + stats.toString();
	}

	public Stats stats;
	public String rcType;
	public String networkName;
	public String dtreeMethod = "unknown";
	public String userMemory;
	public String estimatedTime;
}
