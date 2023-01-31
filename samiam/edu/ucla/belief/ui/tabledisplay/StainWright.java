package edu.ucla.belief.ui.tabledisplay;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import javax.swing.JComponent;

/** A StainWright's responsibility is to assign Stains
	to Objects (currently indexed by addresses is a JTable).
	This also includes the responsibility of maintaining
	the order of precendence between Stains whose
	applicability competes for the same object.

	@author Keith Cascio
	@since 030805 */
public interface StainWright
{
	public Color getBackground( int row, int column );
	public Stain getStain( int row, int column );
	public void stain( int row, int column, Component comp );
	public Stain[] getStains();
	public JComponent getSwitches();
	public boolean isEnabled();
	public void setEnabled( boolean flag );
	public void addListener( ActionListener listener );
	public boolean removeListener( ActionListener listener );
}
