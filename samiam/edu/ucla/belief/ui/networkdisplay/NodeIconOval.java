package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.preference.*;

import java.awt.*;

/**
 * This class draws a node in the shape of an oval with a border.
*/
public class NodeIconOval extends NodeIcon
{
	public NodeIconOval( DisplayableFiniteVariable dVar )//, SamiamPreferences netPrefs )
	{
		super( dVar );//, netPrefs );
	}

	/** Draws the icon using the graphics object.*/
	public void paintIcon( Component c, Graphics g, int x, int y )
	{
		boolean hidden = isHidden();

		if( hidden ) return;

		int newx = x;
		int newy = y;
		int width = 0;
		int height = 0;

		if( hidden )
		{
			width = DIMENSION_HIDDEN.width;
			height = DIMENSION_HIDDEN.height;
		}
		else
		{
			newx = x+myTranslationEffective;
			newy = y+myTranslationEffective;
			width = mySizeEffective.width;
			height = mySizeEffective.height;
		}

		g.setColor( backgroundColor );
		g.fillOval( newx, newy, width, height );

		if( hidden ) return;

		Graphics2D g2 = (Graphics2D)g;

		if( hidden )
		{
		g.setColor( borderColor );
		g2.setStroke( STROKE_HIDDEN );
		}
		else
		{
		Color effectiveBorderColor;
		if( isObserved() ) effectiveBorderColor = borderColorObserved;
		else effectiveBorderColor = borderColor;
		g.setColor( effectiveBorderColor );

		if( isSelected() ) g2.setStroke( selectedBorderStroke );
		else if( isObserved() ) g2.setStroke( observedBorderStroke );
		else g2.setStroke( borderStroke );
		}

		g2.drawOval( newx, newy, mySizeEffective.width, mySizeEffective.height );
	}

	/**
	* This function will modify the start point of a line so that it is
	* on the border of the oval of mySize imageSize.	The params st and en
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

			if( slope > ZERO)
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
	* on the border of the oval of mySize imageSize.	The params st and en
	* should initially be set to the center of each shape.
	*/
	//public void modifyEndPoint( Point st, Point en, Dimension imageSize)
	public void modifyEndPoint( Point st, Point en )
	{
		if( isHidden() ) return;

		if( myImageRadius.height == 0 ) return;

		if( st.x == en.x)
		{
			//vertical line (divide by zero)
			if( st.y > en.y)
			{
				en.translate( 0, myImageRadius.height);
			}
			else
			{
				en.translate( 0, -myImageRadius.height);
			}
		}
		else
		{
			double slope = ((double)(st.y - en.y))/((double)(st.x - en.x));
			double doubleWidth = myImageRadius.getWidth();
			double doubleHeight = myImageRadius.getHeight();
			double theta = Math.atan( slope*doubleWidth/doubleHeight );

			if( slope > ZERO)
			{
				if( st.x < en.x)
				{
					en.translate( (int)(NEGATIVE_ONE * Math.cos( theta) * doubleWidth),
						(int)(NEGATIVE_ONE * Math.sin( theta) * doubleHeight));
				}
				else
				{
					en.translate( (int)(Math.cos( theta) * doubleWidth),
						(int)(Math.sin( theta) * doubleHeight));
				}
			}
			else
			{
				if( st.x < en.x)
				{
					en.translate( (int)(NEGATIVE_ONE * Math.cos( theta) * doubleWidth),
						(int)(NEGATIVE_ONE * Math.sin( theta) * doubleHeight));
				}
				else
				{
					en.translate( (int)(Math.cos( theta) * doubleWidth),
						(int)(Math.sin( theta) * doubleHeight));
				}
			}
		}
	}
}
