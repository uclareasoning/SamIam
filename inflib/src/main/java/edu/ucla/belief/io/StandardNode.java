package edu.ucla.belief.io;

import java.awt.Point;
import java.awt.Dimension;
import edu.ucla.belief.*;
import edu.ucla.belief.sensitivity.ExcludePolicy;
import edu.ucla.belief.io.dsl.DiagnosisType;

/**
* A class for encapsulating a hugin node description.
*/
public interface StandardNode extends FiniteVariable, PropertySuperintendent
{
	/** Moved from HuginNode @since 062204 */
	public boolean isMAPVariable();
	public void setMAPVariable( boolean flag );

    public boolean isSDPVariable();
    public void setSDPVariable( boolean flag );
	/**
		@author Keith Cascio
		@since 051002
	*/
	public boolean[] getExcludeArray();

	/**
		@author Keith Cascio
		@since 051002
	*/
	public void setExcludeArray( boolean[] xa );

	public ExcludePolicy getExcludePolicy();
	public void setExcludePolicy( ExcludePolicy ep );

	/**
	* Will return the node size listed in the HuginNet file.  If it does not
	* exist, it will return a null.
	*
	* This function looks for a node size from the actual node, not from the
	*  network.
	*/
	public Dimension getDimension( Dimension d );
	public void setDimension( Dimension d );

	/**
	* Will get the location from a HuginNode.  If the location does not exist, it will
	* display a message on the System.err screen and set the location to (0,0).
	*/
	public Point getLocation( Point ret );
	public void setLocation( Point newLoc );

	/**
	* Will get the label from a HuginNode.  Can possibly return null if one is not
	*  present.  Will not return empty string, will return null in its place.
	*/
	public String getLabel();
	public void setLabel( String newVal );

	/**
		originally in DSLNode
	*/
	public DiagnosisType getDiagnosisType();

	/**
		originally in DSLNode
	*/
	public void setDiagnosisType( DiagnosisType newVal );

	/**
		originally in DSLNode
		@since 041304
	*/
	public Integer getDefaultStateIndex();
}
