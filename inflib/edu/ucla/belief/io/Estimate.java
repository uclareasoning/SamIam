package edu.ucla.belief.io;

/** moved from NetworkIO 20081110
	@author keith cascio
	@since  20060519 */
public interface Estimate
{
	public int  expectedNodes();
	public int  expectedEdges();
	public long expectedValues();
	public void init( java.io.File fileNetwork );
	public void estimate();
}
