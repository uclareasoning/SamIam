package edu.ucla.belief.dtree;

//{superfluous} import java.util.Collections;

/**
	@author Keith Cascio
	@since 031903
*/
public class Stats
{
	public static final int INT_INVALID_DTREE_STAT = (int)-2000;

	public int height = INT_INVALID_DTREE_STAT;
	public int maxCluster = INT_INVALID_DTREE_STAT;
	public int maxCutset = INT_INVALID_DTREE_STAT;
	public int maxContext = INT_INVALID_DTREE_STAT;

	public void clear()
	{
		//System.out.println( "Stats.clear()" );
		height = INT_INVALID_DTREE_STAT;
		maxCluster = INT_INVALID_DTREE_STAT;
		maxCutset = INT_INVALID_DTREE_STAT;
		maxContext = INT_INVALID_DTREE_STAT;
	}

	public void update( Dtree dtree )
	{
		if( height == INT_INVALID_DTREE_STAT ) height = dtree.getHeight();
		if( maxCluster == INT_INVALID_DTREE_STAT ) maxCluster = dtree.getClusterSize( );
		if( maxCutset == INT_INVALID_DTREE_STAT ) maxCutset = dtree.getCutsetSize( );
		if( maxContext == INT_INVALID_DTREE_STAT ) maxContext = dtree.getContextSize( );
	}

	public void copy( Stats toCopy )
	{
		height = toCopy.height;
		maxCluster = toCopy.maxCluster;
		maxCutset = toCopy.maxCutset;
		maxContext = toCopy.maxContext;
	}

	public String toString()
	{
		return super.toString() + ";height " +Integer.toString(height)+ ";maxCluster " +Integer.toString(maxCluster)+ ";maxCutset " +Integer.toString(maxCutset)+ ";maxContext " +Integer.toString(maxContext);
	}
}
