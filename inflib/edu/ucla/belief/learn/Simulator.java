package edu.ucla.belief.learn;

import edu.ucla.belief.*;
import edu.ucla.util.*;

import java.util.*;

/** @author keith cascio
	@since  20030306 */
public class Simulator implements ProgressMonitorable
{
	public  static       boolean FLAG_VERBOSE                           = false;
	private static       int     INT_THREAD_COUNTER                     = 0;

	/** @since 20070205 */
	public  static final int     INT_NUM_FILES_DEFAULT                  = 1,
	                             INT_NUM_FILES_FLOOR                    = 1,
	                             INT_NUM_FILES_CEILING                  = 0x400,

	                             INT_DEFAULT_NUM_CASES                  = 10000,
	                             INT_NUM_CASES_FLOOR                    = 1,
	                             INT_NUM_CASES_CEILING                  = Integer.MAX_VALUE;

	public  static final double  DOUBLE_DEFAULT_FRACTION_MISSING_VALUES = 0.05,
	                             DOUBLE_FRACTION_MISSING_FLOOR          = 0,
	                             DOUBLE_FRACTION_MISSING_CEILING        = 0.9999999;

	public interface SimulationListener
	{
		public void simulationDone( LearningData data );
	}

	public Simulator( BeliefNetwork bn ){
		myBeliefNetwork = bn;
		myDescription   = "case simulation " + Integer.toString( INT_THREAD_COUNTER++ );
	}

	/** @since 20070205 */
	public LearningData[] simulate( int numcases, double fractionmissing, int repetitions, SimulationListener listener ) throws StateNotFoundException{
		myProgress         = 0;
		myProgressMax      = numcases * repetitions;
		myNote             = "running " + repetitions + " simulations...";
		myFlagFinished     = false;
		LearningData[] ret = null;
		int            i   = 0;

		try{
			ret = new LearningData[ repetitions ];
			for( ; i<repetitions; i++ ){
				if( Thread.currentThread().isInterrupted() ) return ret;
				myNote = "running simulation #" + i + " ...";
				ret[i] = simulateImpl( numcases, fractionmissing );
				if( listener != null ) listener.simulationDone( ret[i] );
			}
			return ret;
		}finally{
			myProgress       = myProgressMax;
			myNote           = "done running " + i + " simulations.";
			myFlagFinished   = true;
		}
	}

	public LearningData simulate() throws StateNotFoundException{
		return simulate( INT_DEFAULT_NUM_CASES, DOUBLE_DEFAULT_FRACTION_MISSING_VALUES );
	}

	public LearningData simulate( int numcases, double fractionmissing ) throws StateNotFoundException{
		myProgress       = 0;
		myProgressMax    = numcases;
		myNote           = "simulating...";
		myFlagFinished   = false;
		try{
			return simulateImpl( numcases, fractionmissing );
		}finally{
			myProgress       = myProgressMax;
			myNote           = "done simulating.";
			myFlagFinished   = true;
		}
	}

	private LearningData simulateImpl( int numcases, double fractionmissing ) throws StateNotFoundException
	{
		if( FLAG_VERBOSE ) Definitions.STREAM_VERBOSE.print( "Simulator.simulate( "+numcases+", "+fractionmissing+" )..." );

		if( myTopological == null ) myTopological = myBeliefNetwork.topologicalOrder();
		if( myMap         == null ) myMap         = new HashMap( myTopological.size() );
		if( myCase        == null ) myCase        = new HashMap( myTopological.size() );
		if( myEvidence    == null ) myEvidence    = myBeliefNetwork.getEvidenceController().evidence();

		LearningData ret = new LearningData( myTopological );
		ret.ensureCapacity( numcases );

		Random random = new Random();
		int numKilled = (int)0;

		//controller.setNotifyEnabled( false );

		long start_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();

		int            randomValueIndex;
		FiniteVariable fVar;
		CPTShell       shell;
		Object         randomValue;
		for( int i=0; i<numcases; i++ )
		{
			if( Thread.currentThread().isInterrupted() ) return ret;
			myMap.clear();
			myCase.clear();
			for( Iterator it = myTopological.iterator(); it.hasNext(); )
			{
				fVar = (FiniteVariable) it.next();

				if( myEvidence.containsKey( fVar ) ) randomValue = myEvidence.get( fVar );
				else{
					shell = fVar.getCPTShell( fVar.getDSLNodeType() );
					randomValueIndex = shell.randomJointValueIndex( myMap );//randomValueIndex = ie.random( fVar );
					randomValue = fVar.instance( randomValueIndex );
				}
				myMap.put( fVar, randomValue );//controller.observe( fVar, randomValue );

				if( random.nextDouble() > fractionmissing ) myCase.put( fVar, randomValue );
				else ++numKilled;
			}
			ret.add( myCase );
			myProgress++;

			if( Definitions.DEBUG ) Definitions.STREAM_VERBOSE.println( "simulated case " + i );

			//controller.setObservations( evidence );
		}

		long end_cpu_ms = JVMProfiler.getCurrentThreadCpuTimeMS();

		//controller.setNotifyEnabled( true );

		long threadCPUMS = end_cpu_ms - start_cpu_ms;

		if( FLAG_VERBOSE ){
			//Definitions.STREAM_VERBOSE.println( "Simulation " );
			Definitions.STREAM_VERBOSE.println( "values killed: " +  numKilled + ", fraction killed == " + ((double)numKilled/(double)(myTopological.size() * numcases)) );
			Definitions.STREAM_VERBOSE.println( "\t time(ms) = " + threadCPUMS );
		}

		return ret;
	}

	/** interface ProgressMonitorable */
	public int getProgress(){
		return myProgress;
	}

	/** interface ProgressMonitorable */
	public int getProgressMax(){
		return myProgressMax;
	}

	/** interface ProgressMonitorable */
	public boolean isFinished(){
		return myFlagFinished;
	}

	/** interface ProgressMonitorable */
	public String getNote(){
		return myNote;
	}

	/** interface ProgressMonitorable */
	public ProgressMonitorable[] decompose(){
		return myDecomposition;
	}

	/** interface ProgressMonitorable */
	public String getDescription(){
		return myDescription;
	}

	protected BeliefNetwork                  myBeliefNetwork;
	protected List/*<FiniteVariable>*/       myTopological;
	protected Map/*<FiniteVariable,Object>*/ myEvidence, myMap, myCase;
	private   int                            myProgress, myProgressMax;
	private   boolean                        myFlagFinished  = true;
	private   String                         myDescription, myNote;
	private   ProgressMonitorable[]          myDecomposition = new ProgressMonitorable[] { this };
}
