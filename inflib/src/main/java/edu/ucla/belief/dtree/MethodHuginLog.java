package edu.ucla.belief.dtree;

import java.io.File;
import java.io.FileInputStream;

import edu.ucla.util.NamedObject;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.io.hugin.HuginLogReader;

/**
	@author Keith Cascio
	@since 050103
*/
public class MethodHuginLog extends CreationMethod
{
	public MethodHuginLog()
	{
		super( "Read Hugin log file", "hugin log" );
	}

	public Dtree getInstance( BeliefNetwork bn, Settings settings ) throws Exception
	{
		String myTentativeHuginLogFilePath = settings.getTentativeHuginLogFilePath();
		File myHuginLogFile = settings.getHuginLogFile();
		if( myTentativeHuginLogFilePath != null )
		{
			File huginLogFile = new File( myTentativeHuginLogFilePath );
			if( huginLogFile.exists() ) settings.setHuginLogFile( huginLogFile );
			else myHuginLogFile = null;
		}

		if( myHuginLogFile == null ) return null;
		else
		{
			Dtree ret = doOpenDtree( bn, myHuginLogFile, settings );
			ret.myCreationMethod = this;
			return ret;
		}
	}

	public static Dtree doOpenDtree( BeliefNetwork bn, File fileSelected, Settings settings ) throws Exception
	{
		if( fileSelected.getPath().endsWith( ".hlg" ) )
		{
			return readDtree( bn, fileSelected,  settings.getDtreeStyle() );
		}
		else throw new IllegalArgumentException( "Wrong file extension, require .hlg" );
	}

	static public Dtree readDtree( BeliefNetwork bn, File fileSelected, Style style ) throws Exception
	{
		HuginLogReader reader = new HuginLogReader( new FileInputStream( fileSelected ) );
		return style.createDtree( bn, reader );
	}

	public static abstract class Style extends NamedObject
	{
		protected Style( String name )
		{
			super( name );
		}

		abstract public Dtree createDtree( BeliefNetwork bn, HuginLogReader reader ) throws Exception;
	}

	public static final Style BALANCED = new Style( "balanced" )
	{
		public Dtree createDtree( BeliefNetwork bn, HuginLogReader reader ) throws Exception
		{
			String strDtree = reader.dtree( true );
			return edu.ucla.belief.recursiveconditioning.Settings.openSamiamDtree( strDtree, bn );
		}
	};
	public static final Style UNBALANCED = new Style( "unbalanced" )
	{
		public Dtree createDtree( BeliefNetwork bn, HuginLogReader reader ) throws Exception
		{
			String strDtree = reader.dtree( false );
			return edu.ucla.belief.recursiveconditioning.Settings.openSamiamDtree( strDtree, bn );
		}
	};
	public static final Style MINCACHE = new Style( "minimize cache" )
	{
		public Dtree createDtree( BeliefNetwork bn, HuginLogReader reader ) throws Exception
		{
			return new Dtree( bn, new DtreeCreateJT(reader));
		}
	};
	public static final Style[] ARRAY_DTREE_STYLES = { BALANCED, UNBALANCED, MINCACHE };
}
