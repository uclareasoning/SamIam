package edu.ucla.belief.ui.util;

import java.awt.Dimension;
import javax.swing.JComponent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
	<p>
	This class allows you to resize a child component
	whenever the user resizes a parent component.
	<p>
	Was in DisplayableFiniteVariable.java

	@author Keith Cascio
	@since 071102
*/
public class ResizeAdapter extends ComponentAdapter
{
	protected JComponent myChildComponent = null;

	public ResizeAdapter( JComponent child )
	{
		myChildComponent = child;
	}

	public static final int INSET = 10;

	public void componentResized( ComponentEvent e )
	{
		Dimension newDim = myChildComponent.getPreferredSize();
		newDim.height = e.getComponent().getHeight() - INSET;
		newDim.width = e.getComponent().getWidth() - INSET;
		myChildComponent.setPreferredSize( newDim );
		//myChildComponent.setPreferredSize( e.getComponent().getPreferredSize() );

		myChildComponent.revalidate();
	}
}
