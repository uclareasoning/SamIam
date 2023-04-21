package edu.ucla.belief.ui.event;

import java.util.*;

/**
	Used to pass information along with a NetStructureChangeListener.
*/
public class NetStructureEvent extends Object
{
	public static final int GENERIC = 0;
	public static final int NODES_ADDED = 1;
	public static final int NODES_REMOVED = 2;
	public static final int EDGE_ADDED = 3;
	public static final int EDGE_REMOVED = 4;
	
	/** Set of finiteVariable objects.  Possibly null.*/
	public Set finiteVars;
	public int eventType;
	
	/** Set s can be null.*/
	public NetStructureEvent( int evt, Set s )
	{
		eventType = evt;
		finiteVars = s;
	}
}
