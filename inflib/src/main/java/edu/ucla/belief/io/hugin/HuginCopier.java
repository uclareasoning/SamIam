package edu.ucla.belief.io.hugin;

import edu.ucla.belief.io.*;
import edu.ucla.belief.*;

import java.awt.Point;

/**
	Copier for HuginNets.  Makes HuginNodeImpl copies.
	@author Keith Cascio
	@since 021804
*/
public class HuginCopier extends AbstractCopier
{
	public static boolean FLAG_VERBOSE = false;

	public static HuginCopier getInstance()
	{
		if( INSTANCE == null ) INSTANCE = new HuginCopier();
		return INSTANCE;
	}

	private static HuginCopier INSTANCE;
	private HuginCopier(){}

	public FiniteVariable copyFiniteVariable( FiniteVariable var )
	{
		if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "HuginCopier.copyFiniteVariable( "+var.getID()+" )" );
		return new HuginNodeImpl( var );
	}

	/** could invert y coordinate */
	public FiniteVariable copyFiniteVariable( FiniteVariable var, BeliefNetwork from, BeliefNetwork to )
	{
		if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "HuginCopier.copyFiniteVariable( "+var.getID()+", "+from+", "+to+" )" );
		HuginNode ret = (HuginNode) copyFiniteVariable( var );
		if( decideReflectY( from, to ) ) reflectY( ret );

		return ret;
	}

	public static boolean decideReflectY( BeliefNetwork from, BeliefNetwork to )
	{
		HuginFileVersion versionFrom	= (from instanceof HuginNet) ? ((HuginNet)from).getVersion() : null;
		HuginFileVersion versionTo	= (to instanceof HuginNet) ? ((HuginNet)to).getVersion() : null;
		boolean flagShouldReflectFrom	= (versionFrom != null) && versionFrom.shouldReflectY();
		boolean flagShouldReflectTo	= (versionTo != null) && versionTo.shouldReflectY();
		return flagShouldReflectFrom != flagShouldReflectTo;
	}

	public static void reflectY( HuginNode node ){
		if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.println( "HuginCopier.reflectY( "+node.getID()+" )" );
		Point location = node.getLocation( POINT_UTIL );
		location.y = (int)0 - location.y;
		node.setLocation( location );
	}

	private static final Point POINT_UTIL = new Point();
}
