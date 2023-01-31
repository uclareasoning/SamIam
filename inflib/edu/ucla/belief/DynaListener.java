package edu.ucla.belief;

/** @author keith cascio
	@since  20030117 */
public interface DynaListener
{
	public void handleInferenceEngine( InferenceEngine IE );
	public void handleError( String msg );
	//public ProgressMonitor getProgressMonitor();
	public ThreadGroup getThreadGroup();
}
