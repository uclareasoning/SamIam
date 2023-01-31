package edu.ucla.belief.ui.networkdisplay;

import java.awt.*;

/**
	A class of graphical objects that exist in both
	a virtual and actual coordinate system.

	@author Keith Cascio
	@since 101802
*/
public interface CoordinateVirtual
{
	public void		setVirtualLocation( Point p );
	public Point		getVirtualLocation( Point p );

	public void		setActualLocation( Point p );
	public Point		getActualLocation( Point p );
	public void		confirmActualLocation();
	public void		translateActualLocation( int deltaX, int deltaY );

	public void		setVirtualSize( Dimension d );
	public Dimension	getVirtualSize( Dimension d );

	public void		setActualSize( Dimension d );
	public Dimension	getActualSize( Dimension d );
	public void		confirmActualSize();
	//public void		setActualSizeAndHintScale( Dimension d, float scale );
	public void		hintScale( float scale );

	public Point		getActualCenter( Point pt );

	public void		setActualScale( double factor );

	public void		recalculateActual();
}