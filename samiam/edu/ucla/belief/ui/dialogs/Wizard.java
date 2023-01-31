package edu.ucla.belief.ui.dialogs;

import java.awt.event.WindowEvent;
import java.awt.Dimension;

/** @author Keith Cascio
	@since 032605 */
public interface Wizard
{
	public Stage getFirst();
	public void windowClosing( WindowEvent e );
	public Dimension getPreferredSize();
	public WizardPanel getWizardPanel();
	public void addWizardListener( WizardListener listener );
	public boolean removeWizardListener( WizardListener listener );
}
