package edu.ucla.belief.ui.preference;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Iterator;

/**
	Removed from PackageOptionsDialog.java
	@author David Allen
	@author Keith Cascio
	@since 071202
*/
public class ColorSwatchLabel extends JLabel
{
	public Color clr;

	public ColorSwatchLabel( Color c)
	{
		Icon icon = new ColorSwatchIcon( c );
		setIcon( icon );
		Dimension s = new Dimension( icon.getIconWidth(), icon.getIconHeight() );
		setMinimumSize( s );
		//setMaximumSize( s );
		setPreferredSize( s );

		addMouseListener( new MouseAdapter(){
			public void mouseClicked( MouseEvent e)
			{
				Color newColor = JColorChooser.showDialog( null,"Choose Color",clr );
				if( newColor != null) {
					clr = newColor;
					repaint();
					ColorSwatchLabel.this.fireActionPerformed();
				}
			}
		});
	}

	public void setValue( Color c )
	{
		((ColorSwatchIcon)getIcon()).setColor( c );
	}

	protected ActionEvent myActionEvent = new ActionEvent( this, 0, "" );

	protected void fireActionPerformed()
	{
		for( Iterator it = myActionListeners.iterator(); it.hasNext(); )
		{
			((ActionListener)it.next()).actionPerformed( myActionEvent );
		}
	}

	public void addActionListener( ActionListener AL )
	{
		myActionListeners.add( AL );
	}

	public void removeActionListener( ActionListener AL )
	{
		myActionListeners.remove( AL );
	}

	protected Collection myActionListeners = new LinkedList();

	public static final int INT_SIZE_ICON_BORDER = (int)1;
	public static final Dimension DIM_ICON = new Dimension( 64, 16 );

	public class ColorSwatchIcon implements Icon
	{
		public ColorSwatchIcon( Color c ){
			clr = c;
			setAlignmentX( Component.LEFT_ALIGNMENT );
		}

		//public ColorSwatchIcon( Color c, int w, int h ){
		//	this( c);
		//	size.setSize( w, h );
		//}

		public void setColor( Color c )
		{
			clr = c;
		}

		public int getIconHeight() { return DIM_ICON.height + 2 * INT_SIZE_ICON_BORDER;}

		public int getIconWidth() { return DIM_ICON.width + 2 * INT_SIZE_ICON_BORDER;}

		public void paintIcon( Component c, Graphics g, int x, int y) {
			g.setColor( clr);
			g.fillRect( INT_SIZE_ICON_BORDER, INT_SIZE_ICON_BORDER, DIM_ICON.width, DIM_ICON.height );
			//border
			g.setColor( Color.black);
			g.drawRect( INT_SIZE_ICON_BORDER, INT_SIZE_ICON_BORDER, DIM_ICON.width, DIM_ICON.height );
		}
	}
}
