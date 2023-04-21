package edu.ucla.structure;

/** grep filter
    moved from FiniteVariable 20081110
	@author keith cascio
	@since  20070419 */
public interface Filter{
	public boolean accept( Object line );
}
