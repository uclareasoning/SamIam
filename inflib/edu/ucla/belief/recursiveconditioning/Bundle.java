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
public class Bundle
{
	private RC myRC;
	private boolean myFlagStale = false;
	//private String myRCFilePath;
	private File myRCFile;
	private Dtree myDtree;
	private Stats myStats;
	private CachingScheme myCachingScheme = Settings.CACHE_SCHEME_DFBnB;
	private BeliefNetwork myBeliefNetwork;
	private Computation myAll;
	private Computation myPe;

	public void refresh()
	{
		myAll.refresh();
		myPe.refresh();
		setStale( false );
	}

	public boolean copy( Bundle toCopy )
	{
		boolean flagNotSettingsChanging = true;

		flagNotSettingsChanging &= myRC == toCopy.myRC;
		flagNotSettingsChanging &= myRCFile == toCopy.myRCFile;
		flagNotSettingsChanging &= myDtree == toCopy.myDtree;
		flagNotSettingsChanging &= myCachingScheme == toCopy.myCachingScheme;
		flagNotSettingsChanging &= myBeliefNetwork == toCopy.myBeliefNetwork;
		flagNotSettingsChanging &= myFlagStale == toCopy.myFlagStale;

		if( toCopy.myAll == null )
		{
			flagNotSettingsChanging &= myAll == null;
			myAll = null;
		}
		else
		{
			if( myAll == null ) myAll = new Computation.All( null );
			flagNotSettingsChanging &= myAll.copy( toCopy.myAll );
		}

		if( toCopy.myPe == null )
		{
			flagNotSettingsChanging &= myPe == null;
			myAll = null;
		}
		else
		{
			if( myPe == null ) myPe = new Computation.Pe( null );
			flagNotSettingsChanging &= myPe.copy( toCopy.myPe );
		}

		if( toCopy.myStats == null )
		{
			flagNotSettingsChanging &= myStats == null;
			myStats = null;
		}
		else
		{
			if( myStats == null ) myStats = new Stats();
			myStats.copy( toCopy.myStats );
		}

		myRC = toCopy.myRC;
		myRCFile = toCopy.myRCFile;
		myDtree = toCopy.myDtree;
		myStats = toCopy.myStats;
		myCachingScheme = toCopy.myCachingScheme;
		myBeliefNetwork = toCopy.myBeliefNetwork;
		myFlagStale = toCopy.myFlagStale;

		return flagNotSettingsChanging;
	}

	public void setDtree( Dtree dtree )
	{
		myDtree = dtree;
		if( myStats == null ) myStats = new Stats();
		myStats.update( dtree );
	}

	public Dtree getDtree()
	{
		return myDtree;
	}

	public Stats getStats( boolean force )
	{
		if( force && myStats == null )
		{
			myStats = new Stats();
			myStats.update( myDtree );
		}
		return myStats;
	}

	public void setStats( Stats stats )
	{
		myStats = stats;
	}

	public boolean isStale()
	{
		return myFlagStale;
	}

	public void setStale( boolean flag )
	{
		//System.out.println( "Bundle.setStale("+flag+")" );
		myFlagStale = flag;
	}

	public void setRC( RC rc )
	{
		//new Throwable().printStackTrace();
		if( myRC != rc )
		{
			myRC = rc;
			myAll = new Computation.All( myRC );
			myPe = new Computation.Pe( myRC );
		}
	}

	public void setRC( RC rc, File readFrom )
	{
		myRCFile = readFrom;
		setRC( rc );
	}

	public RC getRC()
	{
		return myRC;
	}

	public Computation getAll()
	{
		return myAll;
	}

	public Computation getPe()
	{
		return myPe;
	}

	public String getRCFilePath()
	{
		return (myRCFile == null) ? Settings.STR_DEFAULT_RCFILEPATH : myRCFile.getPath();
	}

	public CachingScheme getCachingScheme()
	{
		return myCachingScheme;
	}

	public void setCachingScheme( CachingScheme cs )
	{
		myCachingScheme = cs;
	}

	public int getDtreeHeight()
	{
		return myStats.height;
	}

	public int getDtreeMaxCluster()
	{
		return myStats.maxCluster;
	}

	public int getDtreeMaxCutset()
	{
		return myStats.maxCutset;
	}

	public int getDtreeMaxContext()
	{
		return myStats.maxContext;
	}
}
