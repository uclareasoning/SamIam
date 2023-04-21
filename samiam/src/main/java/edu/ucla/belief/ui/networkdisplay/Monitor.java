package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.EvidenceChangeListener;
import edu.ucla.belief.EvidenceChangeEvent;
import edu.ucla.belief.approx.ApproxEngine;
import edu.ucla.belief.ui.preference.PreferenceListener;
import edu.ucla.belief.ui.event.NodePropertyChangeListener;

import javax.swing.JComponent;
import java.awt.Point;
import java.awt.Rectangle;

/** @author keith cascio
	@since  20030310 */
public interface Monitor extends EvidenceChangeListener, PreferenceListener, NodePropertyChangeListener, CoordinateVirtual
{
	public static boolean FLAG_DEBUG_BORDERS = false;

	public static final int
	  INT_MIN_PHASE = -1,
	  INT_MAX_PHASE =  4;

	public void evidenceChanged( EvidenceChangeEvent ece, double globalMaximumProbability );

	//Monitor methods
	public int                    rotate();
	public JComponent       asJComponent();
	public void       notifyBoundsChange();
	public void                setApprox( ApproxEngine ae );

	//awt/swing methods
	public void setTitle( String text );
	public String getTitle();
	public void pack();
	public boolean isVisible();
	public void setVisible( boolean flag );
	public Rectangle getBounds();
	public Rectangle getBounds( Rectangle rect );
}
