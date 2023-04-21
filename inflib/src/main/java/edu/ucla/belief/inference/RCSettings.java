package edu.ucla.belief.inference;

import edu.ucla.structure.*;
import edu.ucla.belief.*;
import edu.ucla.util.*;
import edu.ucla.belief.dtree.Stats;

import java.util.*;
import java.math.BigInteger;

/** @author keith cascio
	@since  20031029 */
public class RCSettings implements UserObject, ChangeBroadcaster
{
	public static       boolean
	  FLAG_DEBUG_VERBOSE               = Definitions.DEBUG;
	public static final boolean
	  FLAG_DEBUG_VERBOSE_II            = false,
	  FLAG_DEBUG_VERBOSE_III           = false,
	  FLAG_DEBUG_VERBOSE_INFO          = false,
	  FLAG_DEBUG_VERBOSE_CLONE         = false;

	public RCSettings() {}

	public boolean isStale()
	{
		return myFlagStale;
	}

	public boolean setEliminationHeuristic( EliminationHeuristic h )
	{
		if( FLAG_DEBUG_VERBOSE_II ) Definitions.STREAM_VERBOSE.println( "("+this+")" + myDebugID + ".setEliminationHeuristic( "+h+" )" );
		if( myEliminationHeuristic != h )
		{
			myEliminationHeuristic = h;
			settingChanged();
			return true;
		}
		else return false;
	}

	public EliminationHeuristic getEliminationHeuristic()
	{
		return myEliminationHeuristic;
	}

	public void setInfo( RCInfo rcinfo )
	{
		if( FLAG_DEBUG_VERBOSE_INFO ) Definitions.STREAM_VERBOSE.println( "("+this+")" + myDebugID + ".setInfo( "+rcinfo+" )" );
		myRCInfo = rcinfo;
		setExpectedNumberOfRCCalls( (myRCInfo == null) ? (int)-1 : myRCInfo.recursiveCalls() );
	}

	public RCInfo getInfo()
	{
		if( FLAG_DEBUG_VERBOSE_INFO ) Definitions.STREAM_VERBOSE.println( "("+this+")" + myDebugID + ".getInfo() == " + myRCInfo );
		return myRCInfo;
	}

	public void setPrEOnly( boolean flag )
	{
		if( myFlagPrEOnly != flag )
		{
			myFlagPrEOnly = flag;
			settingChanged();
		}
	}

	public boolean getPrEOnly()
	{
		return myFlagPrEOnly;
	}

	/**
		@ret true iff d is a different memory proportion
	*/
	public boolean setUserMemoryProportion( double d )
	{
		return setUserMemoryProportion( d, true );
	}

	/**
		@ret true iff d is a different memory proportion
		@author Keith Cascio
		@since 070203
	*/
	public boolean setUserMemoryProportion( double d, boolean makeStale )
	{
		if( FLAG_DEBUG_VERBOSE_II ) Definitions.STREAM_VERBOSE.println( "RCSettings.setUserMemoryProportion( " +d+ " )" );

		if( d != myUserMemoryProportion )
		{
			myUserMemoryProportion = d;
			if( makeStale ) myFlagStale = true;
			//System.out.println( "STALE" );
			//settingChanged();
			return true;
		}
		else return false;
	}

	public double getUserMemoryProportion()
	{
		return myUserMemoryProportion;
	}

	/**
		@author Keith Cascio
		@since 102903
	*/
	public double getActualMemoryProportion()
	{
		if( FLAG_DEBUG_VERBOSE_II ) Definitions.STREAM_VERBOSE.println( "("+this+")" + myDebugID + ".getActualMemoryProportion()" );
		//System.out.println( "info.allocatedCacheEntries() = " + myRCInfo.allocatedCacheEntries() );
		//System.out.println( "info.cacheEntriesFullCaching() = " + myRCInfo.cacheEntriesFullCaching() );

		RCInfo info = myRCInfo;
		return info.allocatedCacheEntries().doubleValue() / info.cacheEntriesFullCaching().doubleValue();
	}

	/**
		@author Keith Cascio
		@since 102903
	*/
	public double synchronizeMemoryProportion()
	{
		myUserMemoryProportion = getActualMemoryProportion();
		return myUserMemoryProportion;
	}

	/**
		@author Keith Cascio
		@since 111003
	*/
	public static RCInfo selectBetterRCInfo( RCInfo newRCInfo, RCInfo oldRCInfo )
	{
		if( FLAG_DEBUG_VERBOSE_III ) Definitions.STREAM_VERBOSE.println( "RCSettings.selectBetterRCInfo( "+newRCInfo+", "+oldRCInfo+" )" );

		if( newRCInfo == null ) return oldRCInfo;

		boolean replace = false;

		int newHeight = Stats.INT_INVALID_DTREE_STAT;
		int oldHeight = Stats.INT_INVALID_DTREE_STAT;
		int newWidth = Stats.INT_INVALID_DTREE_STAT;
		int oldWidth = Stats.INT_INVALID_DTREE_STAT;
		//int newCutset = Stats.INT_INVALID_DTREE_STAT;
		int newContext = Stats.INT_INVALID_DTREE_STAT;
		int oldContext = Stats.INT_INVALID_DTREE_STAT;

		if( oldRCInfo == null ) replace = true;
		else
		{
			newWidth = newRCInfo.maxClusterSize() - 1;
			oldWidth = oldRCInfo.maxClusterSize() - 1;
			if( FLAG_DEBUG_VERBOSE_III ) Definitions.STREAM_VERBOSE.println( "newWidth "+newWidth+", oldWidth "+oldWidth );
			if( newWidth < oldWidth ) replace = true;
			else if( newWidth == oldWidth )
			{
				newContext = newRCInfo.maxContextSize();
				oldContext = oldRCInfo.maxContextSize();
				if( FLAG_DEBUG_VERBOSE_III ) Definitions.STREAM_VERBOSE.println( "newContext "+newContext+", oldContext "+oldContext );
				if( newContext < oldContext ) replace = true;
				else if( newContext == oldContext )
				{
					newHeight = newRCInfo.height();
					oldHeight = oldRCInfo.height();
					if( FLAG_DEBUG_VERBOSE_III ) Definitions.STREAM_VERBOSE.println( "newHeight "+newHeight+", oldHeight "+oldHeight );
					if( newHeight < oldHeight ) replace = true;
				}
			}
		}

		if( FLAG_DEBUG_VERBOSE_III ) Definitions.STREAM_VERBOSE.println( "replace? " + replace );

		return replace ? newRCInfo : oldRCInfo;
	}

	/** @since 111003 */
	public RCInfo generateInfo( BeliefNetwork bn )
	{
		if( FLAG_DEBUG_VERBOSE_INFO ) Definitions.STREAM_VERBOSE.println( "("+this+")" + myDebugID + ".generateInfo()" );

		try{
			Thread.sleep(1);
			return new RCInfo( bn, this.myFlagPrEOnly, this.getEliminationHeuristic() );
		}catch( Exception e ){
			if( FLAG_DEBUG_VERBOSE ){
				System.err.println( Definitions.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace();
			}
			return null;
		}
	}

	/** @since 042005 */
	public RCInfo generateInfoOrDie( BeliefNetwork bn ) throws Throwable{
		Thread.sleep(1);
		return new RCInfo( bn, this.myFlagPrEOnly, this.getEliminationHeuristic() );
	}

	/** @since 041405 */
	public boolean setHeuristicAndValidateOrRollback( EliminationHeuristic h, BeliefNetwork bn ) throws Throwable
	{
		EliminationHeuristic hCurrent = this.getEliminationHeuristic();
		RCInfo               iCurrent = this.getInfo();
		boolean              sCurrent = this.isStale();

		try{
			if( !setEliminationHeuristic( h ) ) return true;
			setInfo( new RCInfo( bn, this.myFlagPrEOnly, this.getEliminationHeuristic() ) );
			myFlagStale = true;
			return true;
		}catch( Throwable throwable ){
			setEliminationHeuristic( hCurrent );
			setInfo( iCurrent );
			myFlagStale = sCurrent;
			throw throwable;
		}
		//return false;
	}

	public boolean validateRC( BeliefNetwork bn ) throws Throwable
	{
		if( FLAG_DEBUG_VERBOSE_II ) Definitions.STREAM_VERBOSE.println( "("+this+")" + myDebugID + ".validateRC() 0" );
		//new Throwable().printStackTrace();

		Thread.sleep(1);

		if( myRCInfo == null )
		{
			RCInfo info = generateInfoOrDie( bn );
			if( info == null ) return false;
			else{
				setInfo( info );
				myFlagStale = true;
			}
		}

		//System.out.println( "("+this+")" + myDebugID + ".validateRC() RETURNING" );

		return true;
	}

	public boolean validateAllocation( BeliefNetwork bn ) throws Throwable
	{
		if( FLAG_DEBUG_VERBOSE_II ) Definitions.STREAM_VERBOSE.println( "("+this+")" + myDebugID + ".validateAllocation() , isStale()=" + isStale() + ", myUserMemoryProportion=" + myUserMemoryProportion );

		Thread.sleep(1);
		if( !validateRC( bn ) ) return false;
		Thread.sleep(1);

		if( isStale() )
		{
			if( myUserMemoryProportion < edu.ucla.belief.Table.ONE ){
				long numCacheEntries = (long)(myRCInfo.cacheEntriesFullCaching().doubleValue() * myUserMemoryProportion);
				if( !myRCInfo.allocateGreedily( numCacheEntries ) ) return false;
			}
			else if( myUserMemoryProportion == edu.ucla.belief.Table.ONE ){
				myRCInfo.fullCaching();
			}
			else throw new IllegalStateException( "failed condition: 0 <= user mem propertion <= 1" );

			setExpectedNumberOfRCCalls( myRCInfo.recursiveCalls() );
			myFlagStale = false;
		}

		//System.out.println( "("+this+")" + myDebugID + ".validateAllocation() RETURNING" );

		return true;
	}

	public String updateUserMemoryDisplay()
	{
		return updateUserMemoryDisplay( myUserMemoryProportion );
	}

	public String updateUserMemoryDisplay( double proportion )
	{
		//System.out.println( "RCSettings.updateUserMemoryDisplay("+proportion+")" );

		String memoryDisplayTextUser = STR_MSG_OVERFLOW;

		RCInfo info = myRCInfo;
		if( info == null ) return memoryDisplayTextUser;

		BigInteger cacheEntriesFullCaching = info.cacheEntriesFullCaching();
		//if( cacheEntriesFullCaching < (long)0 ) memoryDisplayTextUser = STR_MSG_OVERFLOW;
		//else
		//{
			double optimalMemory = cacheEntriesFullCaching.doubleValue() * ((double)LONG_BYTES_PER_DOUBLE);
			double myUserMemorySetting =  ((double)optimalMemory) * proportion;
			long memoryDisplayNumber = (long)( myUserMemorySetting * myMemoryConversionFactor );
			memoryDisplayTextUser = Long.toString( memoryDisplayNumber );
		//}

		return memoryDisplayTextUser;
	}

	public String[] updateOptimalMemoryDisplay()
	{
		//System.out.println( "RCSettings.updateOptimalMemoryDisplay()" );

		String newMemoryUnit = STR_EMPTY_UNIT;
		String memoryDisplayText = STR_MSG_OVERFLOW;

		RCInfo info = myRCInfo;
		BigInteger cacheEntriesFullCaching = info.cacheEntriesFullCaching();
		//if( cacheEntriesFullCaching < (long)0 ){
		//	memoryDisplayText = STR_MSG_OVERFLOW;
		//	newMemoryUnit = STR_EMPTY_UNIT;
		//}
		//if( (double)optimalMemory == DOUBLE_INVALID_OPTIMAL_MEMORY ) memoryDisplayText = "0";
		//else
		//{
			double optimalMemory = cacheEntriesFullCaching.doubleValue() * ((double)LONG_BYTES_PER_DOUBLE);

			if( optimalMemory < ((double)LONG_BYTE_KILO_THRESHOLD) )
			{
				newMemoryUnit = STR_BYTE_UNIT;
				myMemoryConversionFactor = DOUBLE_BYTES_PER_BYTE;
			}
			else if( optimalMemory < ((double)LONG_KILO_MEGA_THRESHOLD) )
			{
				newMemoryUnit = STR_KILOBYTE_UNIT;
				myMemoryConversionFactor = DOUBLE_KILOBYTES_PER_BYTE;
			}
			else
			{
				//System.out.println( "\t newMemoryUnit = " + STR_MEGABYTE_UNIT );
				newMemoryUnit = STR_MEGABYTE_UNIT;
				myMemoryConversionFactor = DOUBLE_MEGABYTES_PER_BYTE;
				//System.out.println( "\t memoryDisplayNumber = " + (int)((double)optimalMemory * myMemoryConversionFactor) );
				//System.out.println( "\t memoryDisplayText = " + Integer.toString( (int)((double)optimalMemory * myMemoryConversionFactor) ) );
			}

			long memoryDisplayNumber = (long)(optimalMemory * myMemoryConversionFactor);
			memoryDisplayText = Long.toString( memoryDisplayNumber );
		//}

		myReturnArray3[0] = memoryDisplayText;
		myReturnArray3[1] = newMemoryUnit;

		return myReturnArray3;
	}

	public String[] updateEstimatedMinutesDisplay()
	{
		//System.out.println( "RCSettings.updateEstimatedMinutesDisplay()" );
		return updateEstimatedMinutesDisplay( myEstimatedSeconds, myEstimatedMinutes, myEstimatedHours );
	}

	public String[] updateEstimatedMillisDisplay( double millis )
	{
		int secs = (int)(millis * edu.ucla.belief.recursiveconditioning.Settings.DOUBLE_SECONDS_PER_MILLISECOND);
		int mins = (int)(millis * edu.ucla.belief.recursiveconditioning.Settings.DOUBLE_MINUTES_PER_MILLISECOND);
		int hours = (int)(millis * edu.ucla.belief.recursiveconditioning.Settings.DOUBLE_HOURS_PER_MILLISECOND);
		return updateEstimatedMinutesDisplay( secs, mins, hours );
	}

	public String[] updateEstimatedTimeDisplay( double calls )
	{
		double seconds = calls * RCSettings.getSecondsPerRCCall();
		double minutes = (double) Math.ceil( seconds/DOUBLE_60 );
		double hours = minutes/DOUBLE_60;
		return updateEstimatedMinutesDisplay( (int)seconds, (int)minutes, (int)hours );
	}

	public String[] updateEstimatedMinutesDisplay( int secs, int mins, int hours )
	{
		//System.out.println( "RCSettings.updateEstimatedMinutesDisplay( "+secs+", "+mins+" )" );
		String newTimeUnit = null;
		int newNumber = (int)-1;
		if( secs < INT_SECOND_MINUTE_THRESHOLD )
		{
			newTimeUnit = STR_SECOND_UNIT;
			newNumber = secs;
		}
		else if( mins < INT_MINUTE_HOUR_THRESHOLD )
		{
			newTimeUnit = STR_MINUTE_UNIT;
			newNumber = mins;
		}
		else
		{
			newTimeUnit = STR_HOURS_UNIT;
			newNumber = hours;
		}

		myReturnArray[0] = String.valueOf( newNumber );
		myReturnArray[1] = newTimeUnit;

		return myReturnArray;
	}

	public void setExpectedNumberOfRCCalls( double l )
	{
		//System.out.println( "("+this+")" + myDebugID + ".setExpectedNumberOfRCCalls( "+l+" )" );
		myExpectedNumberOfRCCalls = l;
		myEstimatedSeconds = (int)(l * DOUBLE_SECONDS_PER_RC_CALL);
		//System.out.println( "l * DOUBLE_MINUTES_PER_RC_CALL == " + (l * DOUBLE_MINUTES_PER_RC_CALL) );
		//System.out.println( "Math.ceil( l * DOUBLE_MINUTES_PER_RC_CALL ) == " + (Math.ceil( l * DOUBLE_MINUTES_PER_RC_CALL )) );
		//System.out.println( "(int)( Math.ceil( l * DOUBLE_MINUTES_PER_RC_CALL ) ) == " + ((int)( Math.ceil( l * DOUBLE_MINUTES_PER_RC_CALL ) )) );
		myEstimatedMinutes = (int)( Math.ceil( l * DOUBLE_MINUTES_PER_RC_CALL ) );
		myEstimatedHours = (int)(l * DOUBLE_HOURS_PER_RC_CALL);
	}

	public static void setSecondsPerRCCall( double secondsPerRCCall )
	{
		DOUBLE_SECONDS_PER_RC_CALL	= secondsPerRCCall;
		DOUBLE_MINUTES_PER_RC_CALL	= DOUBLE_SECONDS_PER_RC_CALL / (double)60;
		DOUBLE_HOURS_PER_RC_CALL	= DOUBLE_MINUTES_PER_RC_CALL / (double)60;
	}

	public String describeUserMemoryProportion()
	{
		String[] formatted = updateOptimalMemoryDisplay();
		String userFormatted = updateUserMemoryDisplay( getActualMemoryProportion() );
		String text = "using " + userFormatted + formatted[1] + " out of " + formatted[0];
		return text;
	}

	public static final int INT_SECOND_MINUTE_THRESHOLD = (int)120;
	public static final int INT_MINUTE_HOUR_THRESHOLD = (int)256;
	public static final String STR_SECOND_UNIT = " seconds";
	public static final String STR_MINUTE_UNIT = " minutes";
	public static final String STR_HOURS_UNIT = " hours";
	protected static double DOUBLE_SECONDS_PER_RC_CALL	= (double)0.000002018421361862025;//(double)0.00000028571428571424;
	protected static double DOUBLE_MINUTES_PER_RC_CALL	= (double)0.00000003364035603103375;//(double)0.000000004761904761904;
	protected static double DOUBLE_HOURS_PER_RC_CALL	= (double)5.6067260051722916666666666666667e-10;//(double)7.9365079365066666666666666666667e-11;
	public static final long LONG_BYTES_PER_DOUBLE = (long)8;
	public static final double DOUBLE_INVALID_OPTIMAL_MEMORY = -2097140;//(int)52428899;
	public static final long LONG_BYTE_KILO_THRESHOLD = (int)65536;//64KB
	public static final long LONG_KILO_MEGA_THRESHOLD = (int)1073741824;//1024MB == 1GB
	public static final double DOUBLE_BYTES_PER_BYTE = (double)1;
	public static final double DOUBLE_KILOBYTES_PER_BYTE = (double)0.0009765625;
	public static final double DOUBLE_MEGABYTES_PER_BYTE = (double)0.00000095367431640625;
	public static final double DOUBLE_60 = (double)60;
	public static final String STR_EMPTY_UNIT = " ";
	public static final String STR_BYTE_UNIT = " bytes";
	public static final String STR_KILOBYTE_UNIT = " KB";
	public static final String STR_MEGABYTE_UNIT = " MB";
	public static final String STR_MSG_OVERFLOW = "overflow";
	public final ChangeEvent EVENT_SETTING_CHANGED = new ChangeEventImpl().source( this );//, (int)0, "rc setting changed" );

	public void setDebugID( String id )
	{
		myDebugID = id;
	}

	protected void settingChanged()
	{
		if( FLAG_DEBUG_VERBOSE_II ) Definitions.STREAM_VERBOSE.println( "("+this+")" + myDebugID + ".settingChanged()" );
		//new Throwable().printStackTrace();
		myRCInfo = null;
	}

	/** interface ChangeBroadcaster */
	public ChangeBroadcaster fireSettingChanged(){
		if( FLAG_DEBUG_VERBOSE_II ) Definitions.STREAM_VERBOSE.println( "("+this+")" + myDebugID + ".fireSettingChanged()" );

		if( myChangeListeners == null ){ return this; }

		myChangeListeners.cleanClearedReferences();
		ChangeEvent evt = EVENT_SETTING_CHANGED;
		ArrayList list = new ArrayList( myChangeListeners );
		for( Iterator it = list.iterator(); it.hasNext(); ){
			((ChangeListener)it.next()).settingChanged( evt );
		}
		return this;
	}

	/** interface ChangeBroadcaster */
	public boolean    addChangeListener( ChangeListener listener ){
		if(    myChangeListeners == null ){ myChangeListeners = new WeakLinkedList(); }
		return myChangeListeners.contains( listener ) ? false : myChangeListeners.add( listener );
	}

	/** interface ChangeBroadcaster */
	public boolean removeChangeListener( ChangeListener listener ){
		return myChangeListeners != null ? myChangeListeners.remove( listener ) : false;
	}

	public static void setMillisPerRCCall( double millisPerRCCall )
	{
		DOUBLE_SECONDS_PER_RC_CALL = millisPerRCCall * edu.ucla.belief.recursiveconditioning.Settings.DOUBLE_SECONDS_PER_MILLISECOND;
		DOUBLE_MINUTES_PER_RC_CALL = millisPerRCCall * edu.ucla.belief.recursiveconditioning.Settings.DOUBLE_MINUTES_PER_MILLISECOND;
		DOUBLE_HOURS_PER_RC_CALL = millisPerRCCall * edu.ucla.belief.recursiveconditioning.Settings.DOUBLE_HOURS_PER_MILLISECOND;
	}

	public static double getSecondsPerRCCall()
	{
		return DOUBLE_SECONDS_PER_RC_CALL;
	}

	/**
		interface UserObject
	*/
	public UserObject onClone()
	{
		if( FLAG_DEBUG_VERBOSE_CLONE ) Definitions.STREAM_VERBOSE.println( "("+this+")" + myDebugID + ".onClone()" );
		RCSettings ret = new RCSettings();
		ret.copy( this );
		ret.myRCInfo = null;
		return ret;
	}

	public void copy( RCSettings toCopy )
	{
		boolean flagNotSettingsChanging = true;

		flagNotSettingsChanging &= myEliminationHeuristic == toCopy.myEliminationHeuristic;
		flagNotSettingsChanging &= myRCInfo == toCopy.myRCInfo;
		flagNotSettingsChanging &= myFlagPrEOnly == toCopy.myFlagPrEOnly;
		flagNotSettingsChanging &= myFlagStale == toCopy.myFlagStale;
		flagNotSettingsChanging &= myUserMemoryProportion == toCopy.myUserMemoryProportion;

		myEliminationHeuristic = toCopy.myEliminationHeuristic;
		myRCInfo = toCopy.myRCInfo;
		myFlagPrEOnly = toCopy.myFlagPrEOnly;
		myFlagStale = toCopy.myFlagStale;
		myMemoryConversionFactor = toCopy.myMemoryConversionFactor;
		myMemoryUnit = toCopy.myMemoryUnit;
		myUserMemoryProportion = toCopy.myUserMemoryProportion;
		myEstimatedSeconds = toCopy.myEstimatedSeconds;
		myEstimatedMinutes = toCopy.myEstimatedMinutes;
		myEstimatedHours = toCopy.myEstimatedHours;
		myExpectedNumberOfRCCalls = toCopy.myExpectedNumberOfRCCalls;

		if( !flagNotSettingsChanging ) fireSettingChanged();
	}

	private EliminationHeuristic myEliminationHeuristic = EliminationHeuristic.getDefault();
	private RCInfo myRCInfo;
	private boolean myFlagPrEOnly = false;
	private boolean myFlagStale = false;
	private double myMemoryConversionFactor = DOUBLE_KILOBYTES_PER_BYTE;
	private String myMemoryUnit = STR_KILOBYTE_UNIT;
	private double myUserMemoryProportion = (double)1;
	private int myEstimatedSeconds = (int)-1;
	private int myEstimatedMinutes = (int)-1;
	private int myEstimatedHours = (int)-1;
	private double myExpectedNumberOfRCCalls = -1;

	transient private String myDebugID = "NormalRCSettings";
	transient private String[] myReturnArray3 = new String[2];
	transient private String[] myReturnArray = new String[2];
	transient private WeakLinkedList myChangeListeners;
}
