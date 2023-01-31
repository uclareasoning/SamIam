package edu.ucla.belief.recursiveconditioning;

/**
	@author Keith Cascio
	@since 091803
*/
public abstract class RCComparator
{
	public RCComparator( String name, String id )
	{
		myName = name;
		myID = id;
	}

	public String toString()
	{
		return myName;
	}

	abstract public RC decideBest( RC rcA, RC rcB );
	abstract public boolean isRCBetter( RC rc, Computation comp );
	abstract public Bundle decideBest( Bundle bundleA, Bundle bundleB );

	static public RCComparator KEEPNEW = new RCComparator( "keep new", "keepnew" )
	{
		public RC decideBest( RC rcA, RC rcB )
		{
			return rcB;
		}

		public boolean isRCBetter( RC rc, Computation comp )
		{
			return true;
		}

		public Bundle decideBest( Bundle bundleA, Bundle bundleB )
		{
			return bundleB;
		}
	};

	static public RCComparator MIN_MEMORY = new RCComparator( "minimize memory", "minimizememory" )
	{
		public RC decideBest( RC rcA, RC rcB )
		{
			if( theComputation.getNumCacheEntries( rcA ) < theComputation.getNumCacheEntries( rcB ) ) return rcA;
			else return rcB;
		}

		public Bundle decideBest( Bundle bundleA, Bundle bundleB )
		{
			if( bundleA.getAll().getNumMaxCacheEntries() < bundleB.getAll().getNumMaxCacheEntries() ) return bundleA;
			else return bundleB;
		}

		public boolean isRCBetter( RC rc, Computation comp )
		{
			return ( comp.getNumCacheEntries( rc ) < comp.getNumMaxCacheEntries() );
		}
	};

	static public RCComparator MIN_TIME = new RCComparator( "minimize time", "minimizetime" )
	{
		public RC decideBest( RC rcA, RC rcB )
		{
			if( theComputation.calcExpectedNumberOfRCCalls( rcA ) < theComputation.calcExpectedNumberOfRCCalls( rcB ) ) return rcA;
			else return rcB;
		}

		public Bundle decideBest( Bundle bundleA, Bundle bundleB )
		{
			if( bundleA.getAll().getNumRCCallsMaxCache() < bundleB.getAll().getNumRCCallsMaxCache() ) return bundleA;
			else return bundleB;
		}

		public boolean isRCBetter( RC rc, Computation comp )
		{
			return ( comp.calcExpectedNumberOfRCCalls( rc ) < comp.getNumRCCallsMaxCache() );
		}
	};

	static public RCComparator[] ARRAY = new RCComparator[] { KEEPNEW, MIN_MEMORY, MIN_TIME };

	public static RCComparator forID( String id )
	{
		for( int i=0; i<ARRAY.length; i++ )
			if( ARRAY[i].myID.equals( id ) ) return ARRAY[i];
		return null;
	}

	public static RCComparator getDefault()
	{
		return KEEPNEW;
	}

	public static final Computation theComputation = new Computation.All( null );

	protected String myName;
	protected String myID;
}
