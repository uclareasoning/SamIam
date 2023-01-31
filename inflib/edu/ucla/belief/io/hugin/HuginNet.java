package edu.ucla.belief.io.hugin;

//{superfluous} import java.awt.Point;
import java.awt.Dimension;
import java.util.*;
import edu.ucla.belief.*;
import edu.ucla.belief.io.PropertySuperintendent;

public interface HuginNet extends BeliefNetwork, PropertySuperintendent
{
	/**
	* Will return the node size listed in the HuginNet file.  If it does not
	* exist, it will return a null.
	*
	* This function looks for a node size from the network.
	*/
	public Dimension getGlobalNodeSize( Dimension dim );

	/**
	* Sets the net parameters to the name value pairs contained in params.
	*/
	public void setParams( Map params );

	//public void add( HuginNode var );
	//public void remove( HuginNode var );

	public HuginFileVersion getVersion();
	public void setVersion( HuginFileVersion version );
}
