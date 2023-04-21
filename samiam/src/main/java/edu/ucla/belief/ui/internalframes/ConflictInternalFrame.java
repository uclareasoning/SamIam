package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.actionsandmodes.*;

import edu.ucla.belief.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class ConflictInternalFrame extends JInternalFrame implements
	RecompilationListener, EvidenceChangeListener,
	SamiamUserModal,
	NetStructureChangeListener, CPTChangeListener
{
	private NetworkInternalFrame hnInternalFrame;
	//private PackageOptions.PkgDspOpt pkgDspOpt;
	protected PreferenceGroup myGlobalPrefs = null;
	private JPanel mainPanel;
	private RetractTable retractTable;

	public ConflictInternalFrame(	NetworkInternalFrame hnInternalFrame,
					//PackageOptions.PkgDspOpt pkgDspOpt)
					PreferenceGroup globalPrefs )
	{
		super("Evidence conflict", true, true, true, true);
		this.hnInternalFrame = hnInternalFrame;
		//this.pkgDspOpt = pkgDspOpt;
		this.myGlobalPrefs = globalPrefs;
		hnInternalFrame.addRecompilationListener(this);
		hnInternalFrame.addEvidenceChangeListener(this);
		hnInternalFrame.addNetStructureChangeListener(this);
		hnInternalFrame.addCPTChangeListener(this);

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		revalidateMainPanel();
		setContentPane(mainPanel);
	}

	/** interface SamiamUserModal
		@since 20051017 */
	public void setSamiamUserMode( SamiamUserMode mode ){
		if( mode.contains( SamiamUserMode.EVIDENCEFROZEN ) ) ConflictInternalFrame.this.setVisible( false );
	}

	public void revalidateMainPanel()
	{
		//mainPanel.invalidate();
		mainPanel.removeAll();

		if( hnInternalFrame.getInferenceEngine() != null && hnInternalFrame.getPartialDerivativeEngine() != null )
		{
			retractTable = new RetractTable(hnInternalFrame);
			mainPanel.add(new JScrollPane(retractTable));
		}

		mainPanel.validate();
	}

	public void cptChanged( CPTChangeEvent evt ) {
		revalidateMainPanel();
	}

	public void networkRecompiled(){
		revalidateMainPanel();
	}

	/**
		interface EvidenceChangeListener
		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ece ) {}

	public void evidenceChanged( EvidenceChangeEvent ECE ) {
		revalidateMainPanel();
	}

	public void netStructureChanged(NetStructureEvent event) {
		revalidateMainPanel();
	}

	//public void changePackageOptions( PackageOptions.PkgDspOpt pkgDspOpt )
	public void changePackageOptions( PreferenceGroup globalPrefs )
	{
		revalidateMainPanel();
	}
}
