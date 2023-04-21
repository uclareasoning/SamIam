package edu.ucla.belief.ui.statusbar;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;

/**
	@author Keith Cascio
	@since 102902
*/
public class TrimmedBorder implements Border
{
	public TrimmedBorder( AbstractBorder b, int north, int south, int east, int west )
	{
		myBorder = b;
		myTrimNorth = north;
		myTrimSouth = south;
		myTrimEast = east;
		myTrimWest = west;
	}
	
	public void paintBorder(Component c,
				Graphics g,
				int x,
				int y,
				int width,
				int height)
	{
		x -= myTrimWest;
		width += myTrimWest + myTrimEast;
		y -= myTrimNorth;
		height += myTrimNorth + myTrimSouth;
		myBorder.paintBorder(c,g,x,y,width,height);
	}
	
	public Insets getBorderInsets( Component c )
	{
		Insets ret = myBorder.getBorderInsets(c);
		ret.top -= myTrimNorth;
		ret.bottom -= myTrimSouth;
		ret.right -= myTrimEast;
		ret.left -= myTrimWest;
		return ret;
	}
	
	public boolean isBorderOpaque()
	{
		return myBorder.isBorderOpaque();
	}
	
	protected AbstractBorder myBorder;
	protected int myTrimNorth;
	protected int myTrimSouth;
	protected int myTrimEast;
	protected int myTrimWest;
	
	//protected static Insets theUtilInsets = new Insets(0,0,0,0);
}