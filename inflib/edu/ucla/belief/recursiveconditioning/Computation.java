package edu.ucla.belief.recursiveconditioning;

import edu.ucla.structure.*;
import edu.ucla.belief.*;
import edu.ucla.belief.dtree.*;
//{superfluous} import edu.ucla.belief.io.hugin.HuginLogReader;
import edu.ucla.util.*;

import javax.swing.*;
import java.util.*;
import java.io.*;
//{superfluous} import java.awt.event.ActionListener;
//{superfluous} import java.awt.event.ActionEvent;

/**
	@author Keith Cascio
	@since 060503
*/
public abstract class Computation
{
	public Computation( RC rc, String descriptor )
	{
		myRC = rc;
		myDescriptor = descriptor;
		if( myRC != null )
		{
			myNumMaxCacheEntries = calcNumMaxCacheEntries( myRC );
			myNumRCCallsMaxCache = calcExpectedNumberOfRCCalls( myRC );
		}
		refresh();
	}

	public static boolean FLAG_DEBUG = false;

	public void refresh()
	{
		if( FLAG_DEBUG ){ Definitions.STREAM_VERBOSE.print( getClass().getName().substring(38) + ".refresh()" ); }
		if( myRC != null )
		{
			//if( FLAG_DEBUG ) new Throwable().printStackTrace();
			setExpectedNumberOfRCCalls( calcExpectedNumberOfRCCalls( myRC ) );
			if( FLAG_DEBUG ){ Definitions.STREAM_VERBOSE.print( " new value: " + getExpectedNumberOfRCCalls() ); }
		}

		if( FLAG_DEBUG ){ Definitions.STREAM_VERBOSE.println(); }
	}

	public static class All extends Computation
	{
		public All( RC rc )
		{
			super( rc, "marginals" );
		}
		public double getNumCacheEntries( RC rc )
		{
			return rc.statsAll().numCacheEntries();
		}
		protected double calcNumMaxCacheEntries( RC rc )
		{
			return rc.statsAll().numCacheEntries();
		}
		public double calcExpectedNumberOfRCCalls( RC rc )
		{
			return rc.statsAll().expectedNumberOfRCCalls();
		}
	}

	public static class Pe extends Computation
	{
		public Pe( RC rc )
		{
			super( rc, "Pr(e)" );
		}
		public double getNumCacheEntries( RC rc )
		{
			return rc.statsPe().numCacheEntries();
		}
		protected double calcNumMaxCacheEntries( RC rc )
		{
			return rc.statsPe().numCacheEntries();
		}
		public double calcExpectedNumberOfRCCalls( RC rc )
		{
			return rc.statsPe().expectedNumberOfRCCalls();
		}
	}

	abstract public double getNumCacheEntries( RC rc );
	abstract protected double calcNumMaxCacheEntries( RC rc );
	abstract public double calcExpectedNumberOfRCCalls( RC rc );

	private RC myRC;
	private String myDescriptor;
	private double myNumMaxCacheEntries;
	private double myExpectedNumberOfRCCalls;
	private double myNumRCCallsMaxCache;
	private int myEstimatedSeconds = (int)-1;
	private int myEstimatedMinutes = (int)-1;
	private int myEstimatedHours = (int)-1;

	public boolean copy( Computation toCopy )
	{
		boolean flagNotSettingsChanging = true;

		flagNotSettingsChanging &= myRC == toCopy.myRC;
		flagNotSettingsChanging &= myNumMaxCacheEntries == toCopy.myNumMaxCacheEntries;
		flagNotSettingsChanging &= myExpectedNumberOfRCCalls == toCopy.myExpectedNumberOfRCCalls;
		flagNotSettingsChanging &= myEstimatedSeconds == toCopy.myEstimatedSeconds;
		flagNotSettingsChanging &= myEstimatedMinutes == toCopy.myEstimatedMinutes;
		flagNotSettingsChanging &= myEstimatedHours == toCopy.myEstimatedHours;

		myRC = toCopy.myRC;
		myNumMaxCacheEntries = toCopy.myNumMaxCacheEntries;
		myExpectedNumberOfRCCalls = toCopy.myExpectedNumberOfRCCalls;
		myEstimatedSeconds = toCopy.myEstimatedSeconds;
		myEstimatedMinutes = toCopy.myEstimatedMinutes;
		myEstimatedHours = toCopy.myEstimatedHours;

		return flagNotSettingsChanging;
	}

	/**
		@author Keith Cascio
		@since 070703
	*/
	public String getDescriptor()
	{
		return myDescriptor;
	}

	public double getNumMaxCacheEntries()
	{
		return myNumMaxCacheEntries;
	}

	/**
		@author Keith Cascio
		@since 091803
	*/
	public double getNumRCCallsMaxCache()
	{
		return myNumRCCallsMaxCache;
	}

	public double getOptimalMemoryRequirement()
	{
		return myNumMaxCacheEntries * Settings.LONG_BYTES_PER_DOUBLE;
	}

	public double getExpectedNumberOfRCCalls()
	{
		return myExpectedNumberOfRCCalls;
	}

	public void setExpectedNumberOfRCCalls( double d )
	{
		myExpectedNumberOfRCCalls = d;
		myEstimatedSeconds = (int)(d * Settings.DOUBLE_SECONDS_PER_RC_CALL);
		myEstimatedMinutes = (int)( Math.ceil( d * Settings.DOUBLE_MINUTES_PER_RC_CALL ) );
		myEstimatedHours = (int)(d * Settings.DOUBLE_HOURS_PER_RC_CALL);
	}

	public int getEstimatedSeconds()
	{
		return myEstimatedSeconds;
	}
	public int getEstimatedMinutes()
	{
		return myEstimatedMinutes;
	}
	public int getEstimatedHours()
	{
		return myEstimatedHours;
	}

	public static final int INT_SECOND_MINUTE_THRESHOLD = (int)120;
	public static final int INT_MINUTE_HOUR_THRESHOLD = (int)256;
	public static final String STR_SECOND_UNIT = " seconds";
	public static final String STR_MINUTE_UNIT = " minutes";
	public static final String STR_HOURS_UNIT = " hours";
	protected String[] myReturnArray = new String[2];

	public String[] updateEstimatedMinutesDisplay()
	{
		String newTimeUnit = null;
		int newNumber = (int)-1;
		if( myEstimatedSeconds < INT_SECOND_MINUTE_THRESHOLD )
		{
			newTimeUnit = STR_SECOND_UNIT;
			newNumber = myEstimatedSeconds;
		}
		else if( myEstimatedMinutes < INT_MINUTE_HOUR_THRESHOLD )
		{
			newTimeUnit = STR_MINUTE_UNIT;
			newNumber = myEstimatedMinutes;
		}
		else
		{
			newTimeUnit = STR_HOURS_UNIT;
			newNumber = myEstimatedHours;
		}

		myReturnArray[0] = String.valueOf( newNumber );
		myReturnArray[1] = newTimeUnit;

		return myReturnArray;
	}

	protected String[] myReturnArray2 = new String[2];

	public String[] updateElapsedTimeDisplay( double milliseconds )
	{
		String newTimeUnit = null;
		int newNumber = (int)(Settings.DOUBLE_SECONDS_PER_MILLISECOND * milliseconds);
		if( newNumber < INT_SECOND_MINUTE_THRESHOLD ) newTimeUnit = STR_SECOND_UNIT;
		else
		{
			newNumber = (int)(Settings.DOUBLE_MINUTES_PER_MILLISECOND * milliseconds);
			if( newNumber < INT_MINUTE_HOUR_THRESHOLD ) newTimeUnit = STR_MINUTE_UNIT;
			else
			{
				newNumber = (int)(Settings.DOUBLE_HOURS_PER_MILLISECOND * milliseconds);
				newTimeUnit = STR_HOURS_UNIT;
			}
		}

		myReturnArray2[0] = String.valueOf( newNumber );
		myReturnArray2[1] = newTimeUnit;

		return myReturnArray2;
	}
}
