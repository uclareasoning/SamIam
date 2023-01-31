package edu.ucla.belief.inference;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.Definitions;
import edu.ucla.belief.EliminationHeuristic;
import edu.ucla.belief.QuantitativeDependencyHandler;
import edu.ucla.belief.io.NetworkIO;

import il2.bridge.*;
import il2.inf.structure.*;
import il2.util.*;
import il2.inf.rc.CachingScheme;
import il2.model.*;

import java.io.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.math.BigInteger;

public class RCInfo{
    BeliefNetwork bn;
    DGraph dg;
    Converter converter;
    IntMap indicatorLocs;

    CachingScheme cs;

    BigInteger requestedCacheEntries;
    Domain domain;
    double compilationTime;

    public RCInfo(BeliefNetwork network,boolean prE_only,EliminationHeuristic heuristic)
    {
    	//System.out.println( "RCInfo( "+network.getClass().getName()+", "+prE_only+" )" );
	this.bn=network;
	converter=new Converter();
	BayesianNetwork bn2=converter.convert(bn);
	long start=System.currentTimeMillis();
	domain=bn2.domain();
	Table[] t=bn2.cpts();
	Index[] allIndices=new Index[2*t.length];
	indicatorLocs=new IntMap(t.length);
	for(int i=0;i<t.length;i++){
	    allIndices[i]=t[i].createCompatibleIndex();
	    allIndices[i+t.length]=new Index(domain,i);
	    indicatorLocs.putAtEnd(i,t.length+i);
	}
	dg=DGraphs.create( allIndices, heuristic.getEliminationOrder(allIndices).order );
	long finish=System.currentTimeMillis();
	compilationTime=(finish-start)/1000.0;
	//System.out.println( "dg= " + dg );
	cs=new CachingScheme(dg,prE_only);
    }

    /*
    public RCInfo(BeliefNetwork network,boolean prE_only,EliminationHeuristic heuristic)
    {
    	//System.out.println( "RCInfo( "+network.getClass().getName()+", "+prE_only+" )" );
	this.bn=network;
	converter=new Converter();
	BayesianNetwork bn2=converter.convert(bn);
	domain=bn2.domain();
	Table[] t=bn2.cpts();

	int length = prE_only ? t.length : 2*t.length;
	Index[] allIndices=new Index[ length ];

	indicatorLocs=new IntMap(t.length);
	for(int i=0;i<t.length;i++){
	    allIndices[i]=t[i].createCompatibleIndex();
	    if( !prE_only )
	    {
	    	allIndices[i+t.length]=new Index(domain,i);
	    	indicatorLocs.putAtEnd(i,t.length+i);
	    }
	}
	dg=DGraphs.create( allIndices, heuristic.getEliminationOrder(allIndices).order );
	//System.out.println( "dg= " + dg );
	cs=new CachingScheme(dg,prE_only);
    }
    */

    /**
    	@author Keith Cascio
    	@since 120103
    */
    public void write( java.io.PrintWriter out )
    {
    	dg.write( out, cs.cachedNodes(), converter );
    }

    public double recursiveCalls(){
	return cs.recursiveCalls();
    }

    public BigInteger allocatedCacheEntries(){
	return cs.allocatedCacheEntries();
    }

    public DGraph dgraph(){
	return dg;
    }

    public BigInteger cacheEntriesFullCaching(){
	return cs.cacheEntriesFullCaching();
    }

    public double recursiveCallsFullCaching(){
	return cs.recursiveCallsFullCaching();
    }

    public boolean allocateGreedily( long cacheEntries ){
	boolean ret = cs.allocateGreedily( cacheEntries );
	if( ret ) requestedCacheEntries = BigInteger.valueOf( cacheEntries );
	return ret;
    }

    public void fullCaching(){
	cs.fullCaching();
	requestedCacheEntries=cacheEntriesFullCaching();
    }

    public BeliefNetwork network(){
	return bn;
    }

    public Domain domain(){
    	return domain;
    }

    public Converter converter(){
	return converter;
    }

    Table[] createTables(){
		return createTables( (QuantitativeDependencyHandler)null );
	}

	/** @since 061404 */
	Table[] createTables( QuantitativeDependencyHandler handler )
	{
		Table[] cpts=null;

		if( handler == null ) cpts=converter.convert(bn).cpts();
		else{
			cpts = new il2.model.Table[bn.size()];
			int i=0;
			for( Iterator it = bn.iterator(); it.hasNext(); ){
				cpts[i++] = converter.convert( handler.getCPTShell( (FiniteVariable) it.next() ).getCPT() );
			}
		}

		Table[] result=new Table[2*cpts.length];
		System.arraycopy(cpts,0,result,0,cpts.length);
		for(int i=0;i<cpts.length;i++){
			result[indicatorLocs.get(i)]=Table.indicatorTable(domain,i);
		}
		return result;
	}

    IntMap indicatorLocs(){
	return indicatorLocs;
    }

    public java.util.Set cachedNodes(){
	return cs.cachedNodes();
    }

    public int diameter(){
	return dg.diameter();
    }

    public int height(){
	return dg.height();
    }

    public int maxClusterSize(){
	return il2.inf.structure.JoinTreeStats.logBaseTwo( dg.largestClusterSize() ).intValue();
    }
    public int maxContextSize(){
	return JoinTreeStats.logBaseTwo( dg.largestContextSize() ).intValue();
    }

    public int maxCutsetSize(){
	return JoinTreeStats.logBaseTwo( dg.largestCutsetSize() ).intValue();
    }
	public static final String STR_ARG_PATH_INPUT = "-i";
	public static final String STR_ARG_PATH_OUTPUT = "-o";
	public static final String STR_ARG_MEMORY_LIMIT = "-m";
	public static final String STR_PATH_OUTPUT_DEFAULT = "dtree.dat";
	public static final double DOUBLE_DOUBLES_PER_MEGABYTE = (double)131072;

	/** @since 20031201 */
	public static void main( String[] args )
	{
		File fileToOpen = null;
		File fileToWrite = null;
		double memoryLimit = Double.POSITIVE_INFINITY;

		for( int i=0; i < args.length; i++ )
		{
			if( args[i].startsWith( STR_ARG_PATH_INPUT ) )
			{
				String potentialInputPath = args[i].substring( STR_ARG_PATH_INPUT.length() );
				if( potentialInputPath.length() > 0 )
				{
					fileToOpen = new File( potentialInputPath );
					if( !fileToOpen.exists() )
					{
						fileToOpen = null;
						System.err.println( "Error: input file \"" +potentialInputPath+ "\" does not exist." );
						return;
					}
				}
			}
			else if( args[i].startsWith( STR_ARG_PATH_OUTPUT ) )
			{
				String potentialOutputPath = args[i].substring( STR_ARG_PATH_OUTPUT.length() );
				if( potentialOutputPath.length() > 0 )
				{
					fileToWrite = new File( potentialOutputPath );
				}
			}
			else if( args[i].startsWith( STR_ARG_MEMORY_LIMIT ) )
			{
				String potentialMemoryLimit = args[i].substring( STR_ARG_MEMORY_LIMIT.length() );
				if( potentialMemoryLimit.length() > 0 )
				{
					try{
						memoryLimit = Double.parseDouble( potentialMemoryLimit );
					}catch( Exception e ){
						System.err.println( "Error: incorrectly formatted memory limit \"" +potentialMemoryLimit+ "\"" );
						return;
					}
					//if( memoryLimit < edu.ucla.belief.Table.ZERO || edu.ucla.belief.Table.ONE < memoryLimit ){
					//	System.err.println( "Error: cache factor \"" +potentialMemoryLimit+ "\" out of bounds: [0,1]." );
					//	return;
					//}
				}
			}
			else{
				System.err.println( "Error: invalid parameter \"" +args[i]+ "\"" );
				return;
			}
		}

		if( fileToOpen == null ){
			System.err.println( "usage: " + RCInfo.class.getName() + " -i<input path> [-o<output path>] [-m<memory limit>]" );
			return;
		}

		try{
			if( fileToWrite == null ) fileToWrite = new File( NetworkIO.extractNetworkNameFromPath( fileToOpen.getPath() ) + "_" + STR_PATH_OUTPUT_DEFAULT );

			BeliefNetwork bn = NetworkIO.read( fileToOpen );
			RCInfo info = new RCInfo( bn, true, EliminationHeuristic.MIN_FILL );

			BigInteger cacheEntriesFullCaching = info.cacheEntriesFullCaching();
			double recursiveCallsFullCaching = info.recursiveCallsFullCaching();
			//int memToUse = (int)( ((double)cacheEntriesFullCaching) * memoryLimit );
			long memToUse = Math.min( cacheEntriesFullCaching.longValue(), (long)( memoryLimit * DOUBLE_DOUBLES_PER_MEGABYTE ) );
			info.allocateGreedily( memToUse );
			BigInteger allocatedCacheEntries = info.allocatedCacheEntries();
			double recursiveCalls = info.recursiveCalls();
			int numNodes = info.dg.tree().size()+1;

			PrintWriter stream = new PrintWriter( new FileOutputStream( fileToWrite ) );

			String strDate = DateFormat.getDateTimeInstance().format( new Date( System.currentTimeMillis() ) );

			stream.println( "# " + strDate );
			stream.println( "# input \"" +fileToOpen.getPath()+ "\"" );
			stream.println( "# memory limit \"" +Double.toString( memoryLimit )+ " MB\"" );
			stream.println( "# cache entries, full caching \"" + cacheEntriesFullCaching.toString() + "\"" );
			stream.println( "# recursive calls, full caching \"" +Double.toString( recursiveCallsFullCaching ) + "\"" );
			stream.println( "# allocated cache entries \"" + allocatedCacheEntries.toString() + "\"" );
			stream.println( "# recursive calls \"" +Double.toString( recursiveCalls ) + "\"" );
			stream.println( "# number of dtree nodes \"" +Integer.toString( numNodes ) + "\"" );

			stream.println();
			info.write( stream );
			stream.close();
		}catch( Exception e ){
			System.err.println( "Error: caught exception " + e );
		}

		Definitions.STREAM_TEST.println( "Successfully wrote dtree to \"" +fileToWrite.getPath()+ "\"" );
	}

    public double getCompilationTime(){
	return compilationTime;
    }
    public double getMemoryRequirements(){
	return cs.allocatedCacheEntries().doubleValue()*8.0/(1024.0*1024.0);
    }
}
