package edu.ucla.belief.ui.statusbar;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;

/**
	@author Keith Cascio
	@since 102902
*/
public class NsidedBorder implements Border
{
	public NsidedBorder( AbstractBorder b, boolean north, boolean south, boolean east, boolean west )
	{
		myBorder = b;
		myFlagNorth = !north;
		myFlagSouth = !south;
		myFlagEast = !east;
		myFlagWest = !west;
	}
	
	public void paintBorder(Component c,
				Graphics g,
				int x,
				int y,
				int width,
				int height)
	{
		myBorder.getBorderInsets(c,theUtilInsets);
		if( myFlagNorth )
		{
			y -= theUtilInsets.top;
			height += theUtilInsets.top;
		}
		if( myFlagSouth )
		{
			height += theUtilInsets.bottom;
		}
		if( myFlagEast )
		{
			width += theUtilInsets.right;
		}
		if( myFlagWest )
		{
			x -= theUtilInsets.left;
			width += theUtilInsets.left;
		}
		myBorder.paintBorder(c,g,x,y,width,height);
	}
	
	public Insets getBorderInsets( Component c )
	{
		Insets ret = myBorder.getBorderInsets(c);
		if( myFlagNorth ) ret.top = (int)0;
		//if( myFlagSouth ) ret.bottom = (int)0;
		if( myFlagEast ) ret.right = (int)0;
		if( myFlagWest ) ret.left = (int)0;
		return ret;
	}
	
	public boolean isBorderOpaque()
	{
		return myBorder.isBorderOpaque();
	}
	
	protected AbstractBorder myBorder;
	protected boolean myFlagNorth;
	protected boolean myFlagSouth;
	protected boolean myFlagEast;
	protected boolean myFlagWest;
	
	protected static Insets theUtilInsets = new Insets(0,0,0,0);
}