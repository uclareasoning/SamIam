package edu.ucla.belief.ui.clipboard;

import edu.ucla.structure.DirectedGraph;
import edu.ucla.belief.ui.displayable.DisplayableBeliefNetwork;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.networkdisplay.NetworkDisplay;
import java.util.Set;
import java.awt.Point;
import java.awt.Component;
import javax.swing.JComponent;

/**
	@author Keith Cascio
	@since 101602
*/
public interface NetworkClipBoard extends DirectedGraph
{
	public void copy( DisplayableBeliefNetwork network, Set dVars );
	public void cut( DisplayableBeliefNetwork network, NetworkDisplay display, Set dVars );
	public boolean paste( DisplayableBeliefNetwork network, NetworkInternalFrame hnInternalFrame, Point actualCenter );
	public boolean paste( DisplayableBeliefNetwork network, NetworkInternalFrame hnInternalFrame, Point actualCenter, boolean withEdges, boolean withCPs, boolean withRegexes );
	public boolean promptPaste( DisplayableBeliefNetwork network, NetworkInternalFrame hnInternalFrame, Point actualCenter, Component parent );
	public JComponent getPromptPastePanel();
	public Point getVirtualCenter( Point p );
	public void resetVirtualCenter( Point newVirtualCenter );
	//public void translateWithRespectTo( CoordinateTransformer xformer );
}
