package edu.ucla.belief.io;

import edu.ucla.util.ProgressMonitorable;

/** A task that we expect to run in time roughly linear
	in the number of nodes a model contains.
	The linear factor corresponds to the number of "steps"
	that makeup this task, i.e. how many times we expect
	the overall task to process each node.

	@author keith cascio
	@since 20060519 */
public class NodeLinearTask implements ProgressMonitorable
{
	/** @param steps The linear factor, i.e. how many times we expect the overall task to process each node.
		@param notes Array should have the same size as the steps parameter.  One note for each "step" of the task.
	*/
	public NodeLinearTask( String description, Estimate estimate, int steps, String[] notes ){
		myDescription = description;
		myNotes       = notes;
		myEstimate    = estimate;
		mySteps       = steps;
		init();
	}

	/** The computation code should call this every time
		it processes a node, thus updating the overall progress. */
	public void touch(){
		++myProgress;
	}

	public int     getProgress(){
		return myProgress;
	}

	public int     getProgressMax(){
		return myProgressMax;
	}

	public boolean isFinished(){
		return myFlagFinished;
	}

	/** important: relies on setFinished(true)
		@since 20060522 */
	public synchronized void join( long timeout ) throws InterruptedException {
		if( !myFlagFinished ) NodeLinearTask.this.wait( timeout );
	}

	/** It is preferable for the code that creates this task
		to call setFinished(true) when the task is certainly
		completed.  setFinished(false) is useless. */
	public synchronized void setFinished( boolean flag ){
		myFlagFinished = flag;
		if( myFlagFinished ) NodeLinearTask.this.notifyAll();
	}

	/** @return The appropriate note by infering the step from the fraction of overall progress. */
	public String  getNote(){
		float fractionCompleted = ((float)(myProgress-1))/((float)myExpectedNodes);
		int   step              =  (int)   Math.floor( (double)fractionCompleted );
		if( (step < 0) || (step >= myNotes.length) ) step = myIndexLastStep;
		//System.out.println( "step = " + myProgress + " / " + myExpectedNodes + " = " + step );
		return myNotes[ myIndexLastStep = step ];
	}

	/** Simple task decomposes into only itself. */
	public ProgressMonitorable[] decompose(){
		if( myDecomp == null ) myDecomp = new ProgressMonitorable[] { NodeLinearTask.this };
		return myDecomp;
	}

	public String getDescription(){
		return myDescription;
	}

	private void init(){
		myExpectedNodes = myEstimate.expectedNodes();
		myProgressMax   = (myExpectedNodes*mySteps) + 1;
	}

	private Estimate myEstimate;
	private int      myProgressMax   =  1;
	private int      myProgress      =  0;
	private int      mySteps         =  1;
	private int      myExpectedNodes = -1;
	private boolean  myFlagFinished  = false;
	private String   myDescription;
	private String[] myNotes;
	private int      myIndexLastStep =  0;
	private ProgressMonitorable[] myDecomp;
}
