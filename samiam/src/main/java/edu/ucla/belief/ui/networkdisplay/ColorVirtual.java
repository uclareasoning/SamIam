package edu.ucla.belief.ui.networkdisplay;

import java.awt.*;

/**
	A class of graphical objects that exist in both
	a virtual and actual color system.

	@author Keith Cascio
	@since 080504
*/
public interface ColorVirtual
{
	public void		setVirtualColor( Color c );
	public Color	getVirtualColor();

	public void		setActualColor( Color c );
	public Color	getActualColor();
	public void		confirmActualColor();

	public void		recalculateActualColor();

	public void		paintImmediately();
}
