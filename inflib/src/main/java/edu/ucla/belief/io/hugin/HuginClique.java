package edu.ucla.belief.io.hugin;

import java.util.List;

/**
	@author Keith Cascio
	@since 050703
*/
public class HuginClique
{
	public int id;
	public List members;
	public int sizeTable;
	
	public HuginClique( int id, List members, int sizeTable )
	{
		this.id = id;
		this.members = members;
		this.sizeTable = sizeTable;
	}
}
