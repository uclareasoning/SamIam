package edu.ucla.structure;

import java.util.*;

/**
	@author Keith Cascio
	@since 082503
*/
public abstract class SetOperation
{
	public SetOperation( String name )
	{
		myName = name;
	}

	protected String myName;
	public String toString()
	{
		return myName;
	}

	abstract public void exec( Collection left, Collection right, Set result );

	public static final Collection smaller( Collection left, Collection right )
	{
		return ( left.size() < right.size() ) ? left : right;
	}

	public static final SetOperation UNION = new SetOperation( "\u222A" )
	{
		public void exec( Collection left, Collection right, Set result )
		{
			result.addAll( left );
			result.addAll( right );
		}
	};

	public static final SetOperation INTERSECT = new SetOperation( "\u2229" )
	{
		public void exec2( Collection left, Collection right, Set result )
		{
			Collection smaller = smaller( left, right );
			Collection larger = ( smaller == left ) ? right : left;

			Object next;
			for( Iterator it = larger.iterator(); it.hasNext(); )
			{
				next = it.next();
				if( smaller.contains( next ) ) result.add( next );
			}
		}

		public void exec( Collection left, Collection right, Set result )
		{
			Collection smaller = smaller( left, right );
			Collection larger = ( smaller == left ) ? right : left;

			result.addAll( smaller );
			result.retainAll( larger );
		}
	};

	public static final SetOperation SUBTRACT = new SetOperation( "\u2212" )
	{
		public void exec( Collection left, Collection right, Set result )
		{
			result.addAll( left );
			result.removeAll( right );
		}
	};

	public static final SetOperation COMPLEMENT = new SetOperation( "\u2201" )
	{
		public void exec( Collection left, Collection right, Set result )
		{
			result.addAll( right );
			result.removeAll( left );
		}
	};

	public static final SetOperation[] ARRAY = new SetOperation[] { UNION, INTERSECT, SUBTRACT, COMPLEMENT };
}
