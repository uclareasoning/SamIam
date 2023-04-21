package edu.ucla.util;

/** A task that estimates how much total work it must do
	and reports on how much it has completed so far.
	Implementations should try to estimate and report
	progress with the goal of making the pace a user sees
	as consistent as possible.

	@author keith cascio
	@since 20060518 */
public interface ProgressMonitorable
{
	public int                   getProgress();
	public int                   getProgressMax();
	public boolean               isFinished();
	public String                getNote();
	public ProgressMonitorable[] decompose();
	public String                getDescription();
}
