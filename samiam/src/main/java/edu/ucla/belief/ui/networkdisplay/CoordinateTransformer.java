package edu.ucla.belief.ui.networkdisplay;

import java.awt.*;

/**
	A class of objects that maitain a
	linear transformation between
	a virtual and actual coordinate system.
	
	@author Keith Cascio
	@since 101802
*/
public interface CoordinateTransformer
{
	public Point		virtualToActual( Point p );
	public Point		actualToVirtual( Point p );
	
	public Dimension	virtualToActual( Dimension d );
	public Dimension	actualToVirtual( Dimension d );
	
	public float		virtualToActual( float f );
	public double		virtualToActual( double d );
}
