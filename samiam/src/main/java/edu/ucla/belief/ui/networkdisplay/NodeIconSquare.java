package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.preference.*;

import java.awt.*;

/**
 * This class draws a node in the shape of a square with a border.
*/
public class NodeIconSquare extends NodeIcon
{
	public NodeIconSquare( DisplayableFiniteVariable dVar )//, SamiamPreferences netPrefs )
	{
		super( dVar );//, netPrefs );
	}

	/** Draws the icon using the graphics object.*/
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		if( isHidden() ) return;

		int newx = x+myTranslationEffective;
		int newy = y+myTranslationEffective;

		g.setColor( backgroundColor );
		g.fillRect( newx, newy, mySizeEffective.width, mySizeEffective.height );

		Color effectiveBorderColor;
		if( isObserved() ) effectiveBorderColor = borderColorObserved;
		else effectiveBorderColor = borderColor;
		g.setColor( effectiveBorderColor );

		Graphics2D g2 = (Graphics2D)g;
		if( isSelected() ) g2.setStroke( selectedBorderStroke );
		else if( isObserved() ) g2.setStroke( observedBorderStroke );
		else g2.setStroke( borderStroke );

		g2.drawRect( newx, newy, mySizeEffective.width, mySizeEffective.height );
	}

	/**
	* This function will modify the start point of a line so that it is
	* on the border of the oval of size imageSize.	The params st and en
	* should initially be set to the center of each shape.
	*/
	public void modifyStartPoint( Point st, Point en )
	{
		//no op
	}

	public void modifyStartPointDeprecated( Point st, Point en )
	{
		if( myImageRadius.height == 0)
		{
			//Catch divide by zero
			throw( new IllegalArgumentException( "modifyStartPoint needs a valid imageSize"));
		}
		else if( st.x == en.x)
		{
			//vertical line (divide by zero)
			if( st.y > en.y)
			{
				st.translate( 0, -myImageRadius.height);
			}
			else
			{
				st.translate( 0, myImageRadius.height);
			}
		}
		else
		{
			double slope = (double)(st.y - en.y)/(st.x - en.x);
			double theta = Math.atan( slope*(float)myImageRadius.getWidth()/myImageRadius.getHeight());

			if( slope > 0.0)
			{
				if( st.x < en.x)
				{
					st.translate( (int)(Math.cos( theta) * (double)myImageRadius.width),
						(int)(Math.sin( theta) * (double)myImageRadius.height));
				}
				else
				{
					st.translate( (int)(NEGATIVE_ONE * Math.cos( theta) * (double)myImageRadius.width),
						(int)(NEGATIVE_ONE * Math.sin( theta) * (double)myImageRadius.height));
				}
			}
			else
			{
				if( st.x < en.x)
				{
					st.translate( (int)(Math.cos( theta) * (double)myImageRadius.width),
						(int)(Math.sin( theta) * (double)myImageRadius.height));
				}
				else
				{
					st.translate( (int)(NEGATIVE_ONE * Math.cos( theta) * (double)myImageRadius.width),
						(int)(NEGATIVE_ONE * Math.sin( theta) * (double)myImageRadius.height));
				}
			}
		}
	}

	/**
	* This function will modify the end point of a line so that it is
	* on the border of the oval of size imageSize.	The params st and en
	* should initially be set to the center of each shape.
	*/
	public void modifyEndPoint( Point st, Point en )
	{
		if( isHidden() ) return;

		if( myImageRadius.height == 0 ) return;

		int translate_x_by = 0;
		int translate_y_by = 0;
		int deltax = st.x - en.x;
		int deltay = st.y - en.y;

		//if( myImageRadius.height == 0){//Catch divide by zero
		//	throw( new IllegalArgumentException( "modifyEndPoint needs a valid imageSize"));
		//} else
		if( deltax == 0 )
		{
			//vertical line (divide by zero)
			//System.out.println( "NodeIconSquare.modifyEndPoint( slope == infinity )");//debug
			if( st.y > en.y) translate_y_by = myImageRadius.height;
			else translate_y_by = -myImageRadius.height;
		}
		else
		{
			double slope = ((double)deltay)/((double)deltax);

			if( deltay > 0 )
			{
				if( deltax > 0 )
				{
					if( slope > mySlope )
					{
						//b
						translate_x_by = (int)(myImageRadius.getHeight() / slope);
						translate_y_by = myImageRadius.height;
					}
					else
					{
						//a
						translate_x_by = myImageRadius.width;
						translate_y_by = (int)(slope * myImageRadius.getWidth());
					}
				}
				else
				{
					if( -slope > mySlope )
					{
						//c
						translate_x_by = (int)(myImageRadius.getHeight() / slope);
						translate_y_by = myImageRadius.height;
					}
					else
					{
						//d
						translate_x_by = -myImageRadius.width;
						translate_y_by = (int)(-slope * myImageRadius.getWidth());
					}
				}
			}
			else
			{
				if( deltax > 0 )
				{
					if( -slope > mySlope )
					{
						//g
						translate_x_by = (int)(myImageRadius.getHeight() / -slope);
						translate_y_by = -myImageRadius.height;
					}
					else
					{
						//h
						translate_x_by = myImageRadius.width;
						translate_y_by = (int)(slope * myImageRadius.getWidth());
					}
				}
				else
				{
					if( slope > mySlope )
					{
						//f
						translate_x_by = (int)(myImageRadius.getHeight() / -slope);
						translate_y_by = -myImageRadius.height;
					}
					else
					{
						//e
						translate_x_by = -myImageRadius.width;
						translate_y_by = (int)(-slope * myImageRadius.getWidth());
					}
				}
			}
		}

		//System.out.println( "NodeIconSquare.modifyEndPoint() dx = " + translate_x_by + ", dy = " + translate_y_by );//debug
		en.translate( translate_x_by, translate_y_by );
	}
}
