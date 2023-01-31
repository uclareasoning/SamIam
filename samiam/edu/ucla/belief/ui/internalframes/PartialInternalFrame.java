package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.tabledisplay.*;

import edu.ucla.belief.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/** JInternalFrame for the partial derivatives tool */
public class PartialInternalFrame extends JInternalFrame implements
	RecompilationListener, EvidenceChangeListener, ItemListener,
	NetStructureChangeListener, CPTChangeListener
{
	private NetworkInternalFrame hnInternalFrame = null;

	protected PreferenceGroup myGlobalPrefs = null;

	private JPanel contentPane, mainPanel;
	private VariableComboBox varBox;
	private JRadioButton lambdaButton, thetaButton;

	public PartialInternalFrame(	NetworkInternalFrame hnInternalFrame,
					PreferenceGroup globalPrefs )
	{
		super("Partial derivatives", true, true, true, true);
		this.hnInternalFrame = hnInternalFrame;

		this.myGlobalPrefs = globalPrefs;

		hnInternalFrame.addEvidenceChangeListener(this);
		hnInternalFrame.addRecompilationListener(this);
		hnInternalFrame.addNetStructureChangeListener(this);
		hnInternalFrame.addCPTChangeListener( this );

		contentPane = (JPanel)getContentPane();
		contentPane.setLayout(new BorderLayout());

		JPanel choicePanel = new JPanel();
		choicePanel.setLayout(new BoxLayout(choicePanel,
			BoxLayout.X_AXIS));
		contentPane.add("North", choicePanel);

		choicePanel.add(new JLabel("Choose variable: "));

		varBox = hnInternalFrame.createVariableComboBox();
		varBox.addSelectedChangeListener(this);
		choicePanel.add(varBox);

		choicePanel.add(new JLabel(" w.r.t. "));

		ButtonGroup group = new ButtonGroup();

		lambdaButton = new JRadioButton("Evidence indicators",
			true);
		lambdaButton.addItemListener(this);
		group.add(lambdaButton);
		choicePanel.add(lambdaButton);

		thetaButton = new JRadioButton("Network parameters");
		thetaButton.addItemListener(this);
		group.add(thetaButton);
		choicePanel.add(thetaButton);

		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		revalidateMainPanel();

		contentPane.add("Center", mainPanel);
	}

	/**
		@author Keith Cascio
		@since 072902
	*/
	private void revalidateMainPanel()
	{
		myIsValid = false;
		mainPanel.removeAll();
		if( isVisible() ) makeMainPanel();
	}

	/**
		@author Keith Cascio
		@since 072902
	*/
	protected void makeMainPanel()
	{
		//InferenceEngine ie = hnInternalFrame.getInferenceEngine();

		PartialDerivativeEngine pde = hnInternalFrame.getPartialDerivativeEngine();

		if( pde != null )
		{
			DisplayableFiniteVariable dVar = (DisplayableFiniteVariable)varBox.getSelectedItem();
			if (dVar == null) {
				mainPanel.validate();
				return;
			}

			//FiniteVariable dVar = dVar.getFiniteVariable();

			if (lambdaButton.getModel().isSelected()) {
				mainPanel.setBorder(new TitledBorder(new
					EtchedBorder(), "Partial derivatives " +
					"w.r.t. evidence indicators of " + dVar));
				mainPanel.add("Center", new JScrollPane(new
					DisplayableTable(hnInternalFrame,
					pde.partial(dVar), "Values", false)));
			}
			else {
				mainPanel.setBorder(new TitledBorder(new
					EtchedBorder(), "Partial derivatives " +
					"w.r.t. network parameters of " + dVar));
				mainPanel.add("Center", new JScrollPane(new
					DisplayableTable(hnInternalFrame,
					pde.familyPartial(dVar), "Values", false)));
			}
		}

		mainPanel.validate();
		myIsValid = true;
	}

	/**
		@author Keith Cascio
		@since 072902
	*/
	protected boolean myIsValid = false;

	/**
		@author Keith Cascio
		@since 072902
	*/
	public void setVisible( boolean visible )
	{
		if( visible && !myIsValid ) makeMainPanel();
		super.setVisible( visible );
	}

	/**
		@author Keith Cascio
		@since 071802
	*/
	public void reInitialize(){
		revalidateMainPanel();
	}

	/**
		@author Keith Cascio
		@since 081302
	*/
	public void cptChanged( CPTChangeEvent evt ){
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

	public void changePackageOptions( PreferenceGroup globalPrefs ){
		revalidateMainPanel();
	}

	public void itemStateChanged(ItemEvent event) {
		revalidateMainPanel();
	}
}
