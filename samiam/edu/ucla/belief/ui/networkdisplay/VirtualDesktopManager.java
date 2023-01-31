package edu.ucla.belief.ui.networkdisplay;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;

public class VirtualDesktopManager extends DefaultDesktopManager
{
	public void endDraggingFrame(JComponent f)
	{
		super.endDraggingFrame( f );
		if( f instanceof CoordinateVirtual )
		{
			((CoordinateVirtual)f).confirmActualLocation();
		}
	}

	public void endResizingFrame(JComponent f)
	{
		super.endResizingFrame( f );
		if( f instanceof CoordinateVirtual )
		{
			((CoordinateVirtual)f).confirmActualSize();
		}
	}
}
