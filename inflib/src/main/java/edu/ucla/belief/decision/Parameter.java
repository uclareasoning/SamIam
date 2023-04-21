package edu.ucla.belief.decision;

/** @author Keith Cascio
	@since 120804 */
public interface Parameter
{
	public void setValue( double newval );
	public double getValue();
	public void setID( String newid );
	public String getID();
}
