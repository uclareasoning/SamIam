package edu.ucla.belief.ui.networkdisplay;

import javax.swing.JLabel;
import java.awt.Point;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;

import edu.ucla.belief.EvidenceChangeListener;
import edu.ucla.belief.EvidenceChangeEvent;

import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariable;
import edu.ucla.belief.ui.actionsandmodes.*;

/** @author keith cascio
	@since  20020422 */
public interface NetworkComponentLabel extends
	CoordinateVirtual, ColorVirtual,
	PreferenceListener, EvidenceChangeListener,
	SamiamUserModal
{
	public void initLazy();
	public JLabel asJLabel();
	/** @since 20060731 */
	public void paintNetworkComponent( Graphics g, Rectangle viewRect );

	public DisplayableFiniteVariable getFiniteVariable();
	public String getText();
	public void hideEvidenceDialog();
	/** @since 060905 */
	public boolean isEvidenceDialogShown();

	/** @since 081704 */
	public void handleHidden();

	public Rectangle getBounds();
	public Rectangle getBounds( Rectangle rv );

	public Rectangle getBoundsManaged( Rectangle rv );
	public void removeFromParentManaged();

	public void repaint();

	public void evidenceChanged( EvidenceChangeEvent ece, double globalMaximumProbability );

	/**
		@author Keith Cascio
		@since 042502
	*/
	public void doDoubleClick();

	/** Size of the icon that it uses.*/
	public int getHeight();

	/** Size of the icon that it uses.*/
	public int getWidth();

	/** Return the nodeIcon that this label uses.*/
	public NodeIcon getNodeIcon();

	/** Will modify pt and then return it, so no new allocations take place.*/
	public Point getActualCenter( Point pt );

	//public void selectionGained();
	//public void selectionLost();
	public boolean setSelected( boolean selected );

	public void selectionSwitch();

	public boolean isSelected();

	/** Allow options to change.*/
	public void changeTextColor( Color clr);

	/** Allow options to change.*/
	public void changePackageOptions();

	/** Edge list manipulator */
	public void addInComingEdge( Arrow ar);

	/** Edge list manipulator */
	public void addOutBoundEdge( Arrow ar);

	/** Edge list manipulator */
	public void removeInComingEdge( Arrow ar);

	/** Edge list manipulator */
	public void removeOutBoundEdge( Arrow ar);

	/** Edge list manipulator */
	public void removeAllInEdges();

	/** Edge list manipulator */
	public void removeAllOutEdges();

	/** Returns ArrayList of Arrow objects.  Edge list manipulator */
	public List getAllInComingEdges();

	/** Returns ArrayList of Arrow objects.  Edge list manipulator */
	public List getAllOutBoundEdges();
}
