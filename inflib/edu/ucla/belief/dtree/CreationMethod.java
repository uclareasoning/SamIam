package edu.ucla.belief.dtree;

import java.io.File;

import edu.ucla.belief.EliminationHeuristic;
import edu.ucla.belief.BeliefNetwork;

/**
	@author Keith Cascio
	@since 050103
*/
public abstract class CreationMethod
{
	abstract public Dtree getInstance( BeliefNetwork bn, Settings settings ) throws Exception;

	public interface Settings
	{
		public EliminationHeuristic getElimAlgo();
		public Object getBalanceFactor();
		public MethodHmetis.Algorithm getHMeTiSAlgo();
		public int getNumDtrees();
		public int getNumPartitions();
		public String getTentativeHuginLogFilePath();
		public File getHuginLogFile();
		public void setHuginLogFile( File newFile );
		public MethodHuginLog.Style getDtreeStyle();
	}

	static public CreationMethod forName( String name )
	{
		Object[] array = getArray();

		Object next;
		for( int i=0; i<array.length; i++ )
		{
			next = array[i];
			if( next instanceof CreationMethod )
			{
				if( next.toString().equals( name ) ) return (CreationMethod) next;
			}
		}

		return null;
	}

	static public CreationMethod forID( String id )
	{
		Object[] array = getArray();

		Object next;
		for( int i=0; i<array.length; i++ )
		{
			next = array[i];
			if( next instanceof CreationMethod )
			{
				if( ((CreationMethod)next).getID().equals( id ) ) return (CreationMethod) next;
			}
		}

		return null;
	}

	protected CreationMethod( String displayName, String id )
	{
		myDisplayName = displayName;
		myID = id;
	}

	final public String toString()
	{
		return myDisplayName;
	}

	public String getID()
	{
		return myID;
	}

	private String myDisplayName;
	private String myID;

	public static Object[] getArray()
	{
		if( myArray == null )
		{
			boolean FLAG_HMETIS_LOADED = Hmetis.loaded();
			//int size = (int)2;
			//if( FLAG_HMETIS_LOADED ) ++size;
			int size = (int)3;

			myArray = new Object[size];

			int index = (int)0;
			myArray[index] = new MethodEliminationOrder();
			++index;
			if( FLAG_HMETIS_LOADED ) myArray[index] = new MethodHmetis();
			else myArray[index] = "hMeTiS not loaded";
			++index;
			myArray[index] = new MethodHuginLog();
		}
		return myArray;
	}

	private static Object[] myArray;
}
