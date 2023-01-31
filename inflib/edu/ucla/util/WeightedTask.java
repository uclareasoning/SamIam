package edu.ucla.util;

/** Wrap a monitorable task such that the reported progress is multiplied
	by a weight factor.  Useful to smooth out the reported pace of progress
	over the life of a compound task.

	@author keith cascio
	@since 20060519 */
public class WeightedTask implements ProgressMonitorable
{
	public WeightedTask( ProgressMonitorable task, float weight ){
		WeightedTask.this.myTask   = task;
		WeightedTask.this.myWeight = weight;
	}

	public String getDescription(){
		if( myDescription == null ) myDescription = myTask.getDescription() + " weighted x" + Float.toString( WeightedTask.this.myWeight );
		return myDescription;
	}

	public float weight(){
		return WeightedTask.this.myWeight;
	}

	public int                   getProgress(){
		return (int)(WeightedTask.this.myTask.getProgress() * myWeight);
	}

	public int                   getProgressMax(){
		return (int)(WeightedTask.this.myTask.getProgressMax() * myWeight);
	}

	public boolean               isFinished(){
		return WeightedTask.this.myTask.isFinished();
	}

	public String                getNote(){
		return WeightedTask.this.myTask.getNote();
	}

	public ProgressMonitorable[] decompose(){
		if( myDecomp == null ) myDecomp = new ProgressMonitorable[] { WeightedTask.this };
		return myDecomp;
	}

	private ProgressMonitorable   myTask;
	private float                 myWeight;
	private ProgressMonitorable[] myDecomp;
	private String                myDescription;
}
