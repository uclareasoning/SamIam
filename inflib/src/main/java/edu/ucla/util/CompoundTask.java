package edu.ucla.util;

import java.util.*;

/** A task that is the serial combination of two or more subtasks.
	Supports weighting the sub-tasks in order to achieve a more
	smooth, consistent overall pace in the reported progress.
	For performance reasons, decomposes the sub-tasks into their
	simplest possible constituents.

	@author keith cascio
	@since 20060519 */
public class CompoundTask implements ProgressMonitorable
{
	/** @param tasks These tasks are decomposed into their simplest possible constituents. */
	public CompoundTask( String descrip, ProgressMonitorable[] tasks, float[] weights ){
		if( weights.length != tasks.length ) throw new IllegalArgumentException();
		myDescription = descrip;
		myTasks       = tasks;
		myWeights     = weights;
		init();
	}

	public String  getDescription(){
		return myDescription;
	}

	public int     getProgress(){
		if( validate() ) return myProgressAccumulated + myItinerary[myIndexCurrentTask].getProgress();
		else return getProgressMax();
	}

	public int     getProgressMax(){
		return myProgressMax;
	}

	public boolean isFinished(){
		return !validate();
	}

	public String  getNote(){
		if( validate() ) return myItinerary[myIndexCurrentTask].getNote();
		else return myDescriptionFinished;
	}

	public ProgressMonitorable[] decompose(){
		return myTasks;
	}

	private boolean validate(){
		while( (myIndexCurrentTask < myItinerary.length) && myItinerary[myIndexCurrentTask].isFinished() ){
			myProgressAccumulated += myItinerary[myIndexCurrentTask].getProgressMax();
			++myIndexCurrentTask;
		}
		return myIndexCurrentTask < myItinerary.length;
	}

	/* java 5
	private LinkedList<WeightedTask> decompose( LinkedList<WeightedTask> list, ProgressMonitorable[] tasks, float[] weights ){
		ProgressMonitorable[] decomp;
		for( int i=0; i<tasks.length; i++ ){
			decomp = tasks[i].decompose();
			if( decomp.length < 2 ){
				list.add( new WeightedTask( decomp[0], weights[i] ) );
			}
			else decompose( list, decomp, expand( weights[i], decomp.length ) );
		}
		return list;
	}*/

	/** java 4 */
	private LinkedList decompose( LinkedList list, ProgressMonitorable[] tasks, float[] weights ){
		ProgressMonitorable[] decomp;
		for( int i=0; i<tasks.length; i++ ){
			decomp = tasks[i].decompose();
			if( decomp.length < 2 ){
				list.add( new WeightedTask( decomp[0], weights[i] ) );
			}
			else decompose( list, decomp, expand( weights[i], decomp.length ) );
		}
		return list;
	}

	public static float[] expand( float weight, int length ){
		float[] ret = new float[length];
		Arrays.fill( ret, weight );
		return ret;
	}

	/* java 5
	private void init(){
		myDescriptionFinished = "finished " + myDescription;

		LinkedList<WeightedTask> list = decompose( new LinkedList<WeightedTask>(), myTasks, myWeights );
		myItinerary = list.toArray( new WeightedTask[list.size()] );

		myProgressMax = 0;
		for( WeightedTask task : myItinerary ){
			myProgressMax += task.getProgressMax();
		}
	}*/

	/** java 4 */
	private void init(){
		myDescriptionFinished = "finished " + myDescription;

		LinkedList list = decompose( new LinkedList(), myTasks, myWeights );
		myItinerary = (WeightedTask[]) list.toArray( new WeightedTask[list.size()] );

		myProgressMax = 0;
		for( int i=0; i<myItinerary.length; i++ ){
			myProgressMax += myItinerary[i].getProgressMax();
		}
	}

	public static final ProgressMonitorable[] trivialDecomposition( ProgressMonitorable task ){
		return new ProgressMonitorable[] { task };
	}

	private String                myDescription;
	private String                myDescriptionFinished;
	private ProgressMonitorable[] myTasks;
	private float[]               myWeights;
	private WeightedTask[]        myItinerary;

	private int myProgressMax         = 1;
	private int myProgressAccumulated = 0;
	private int myIndexCurrentTask    = 0;
}
