package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.preference.SamiamPreferences;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.DSLNodeType;
import edu.ucla.util.Interruptable;

import java.util.List;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;

/**	A wizard that steps the user through the
	process of importing CPT data from a
	tab-delimited file.

	@author Keith Cascio
	@since 032605 */
public class CPTImportWizard extends AbstractWizard implements Wizard, ActionListener
{
	public CPTImportWizard(){
		init();
	}

	private void init(){
		STAGE_BROWSE.setNext( STAGE_IDENTIFY_COLUMNS );
	}

	public void actionPerformed( ActionEvent evt ){
		Object src = evt.getSource();
		if( src == myButtonBrowse ) doBrowse();
		else if( src == myTfBrowse ) STAGE_BROWSE.edited();
		else if( src == myButtonCreate ) doCreate();
		else if( src == myButtonFinish ) doFinish();
		else if( src == myRadioDestroy ) doDistance();
		else if( src == myRadioSuperimpose ) doDistance();
	}

	public void setVariable( FiniteVariable fv, CPTInfo.ReadableWritableTable destination ){
		this.myVariable = fv;
		this.myDestination = destination;
	}

	public FiniteVariable getVariable(){
		return myVariable;
	}

	public Stage getFirst(){
		return this.STAGE_BROWSE;
	}

	public void windowClosing( WindowEvent e ){
		CPTImportWizard.this.STAGE_END.invalidate();
	}

	/** @since 022805 */
	private void doFinish(){
		if( myCPTStruct != null ){
			myCPTStruct.commit( myRadioSuperimpose.isSelected(), myCBNormalize.isSelected() );
			CPTImportWizard.this.fireWizardFinished();
		}
	}

	/** @since 022805 */
	private void doDistance(){
		if( (myCPTStruct != null) && (myLabelDistance != null) ){
			myLabelDistance.setText( doubleToString( myCPTStruct.getDistanceMeasure( myRadioSuperimpose.isSelected() ) ) );
		}
	}

	private ItemListener getItemListener(){
		if( myItemListener == null ){
			myItemListener = new ItemListener(){
				public void itemStateChanged( ItemEvent e ){
					if( e.getStateChange() == ItemEvent.SELECTED ){
						CPTImportWizard.this.fireResetNavigation();
					}
				}
			};
		}
		return myItemListener;
	}
	private ItemListener myItemListener;

	private void doBrowse(){
		JFileChooser chooser = getChooser();
		chooser.showOpenDialog( myPanelBrowse );
		File file = chooser.getSelectedFile();
		if( (file != null) && (file.exists()) ){
			if( myTfBrowse != null ) myTfBrowse.setText( file.getAbsolutePath() );
		}
	}

	private JFileChooser myJFileChooser;
	private File myDefaultDirectory;

	public JFileChooser getChooser(){
		if( myJFileChooser == null ){
			JFileChooser chooser = myJFileChooser = new JFileChooser();
			chooser.setAcceptAllFileFilterUsed( true );

			//chooser.addChoosableFileFilter( filter );
			//chooser.setFileFilter( filter );

			//chooser.setAccessory();

			//chooser.setApproveButtonText( STR_BUTTONTEXT );
			//chooser.setApproveButtonToolTipText( STR_TOOLTIP_PRE + descrip );
			chooser.setDialogTitle( "Select tab delimited data file" );
			chooser.setMultiSelectionEnabled( false );
		}

		String path = null;
		if( (myTfBrowse != null) && ((path = myTfBrowse.getText()).length() > 0 ) ){
			path = myTfBrowse.getText();
			File current = new File( path );
			if( current.exists() ){
				if( !current.isDirectory() ) current = current.getParentFile();
				myJFileChooser.setCurrentDirectory( current );
			}
		}
		else if( myDefaultDirectory != null ){
			myJFileChooser.setCurrentDirectory( myDefaultDirectory );
			myDefaultDirectory = null;
		}

		return myJFileChooser;
	}

	private Stage STAGE_BROWSE = new Stage( "Choose Input File" ){
		public JComponent refresh() throws Exception {
			return CPTImportWizard.this.refreshPanelBrowse();
		}

		public String getProgressMessage(){
			return "choose input file";
		}
	};

	private Stage STAGE_END = new Stage( "Create CPT" )
	{
		public JComponent refresh() throws Exception
		{
			for( Iterator it = STAGE_IDENTIFY_COLUMNS.getInstanceIDStages().iterator(); it.hasNext(); ){
				((StageInstanceIdentification)it.next()).updateScanner();
			}
			return CPTImportWizard.this.refreshPanelResults();
		}

		public String getProgressMessage(){
			return "create cpt";
		}

		public Stage previous(){
			this.invalidate();
			return super.previous();
		}

		public void invalidate(){
			super.invalidate();
			CPTImportWizard.this.myCPTStruct = (CPTInfo)null;
		}

		private JPanel myPanel;
	};

	/** @since 030105 */
	private JComponent getCreationOptionsPanel() throws Exception {
		if( myCreationOptionsPanel == null ){
			myCreationOptionsPanel = new JPanel( new GridBagLayout() );

			//myCBSuperimpose = new JCheckBox( "superimpose onto existing cpt data?" );
			//myCBSuperimpose.addActionListener( (ActionListener)this );

			ButtonGroup group = new ButtonGroup();
			myRadioDestroy = new JRadioButton( "<html><nobr><b>All New</b>" );
			group.add( myRadioDestroy );
			myRadioDestroy.addActionListener( (ActionListener)this );
			JLabel labelDestroy = new JLabel( "<html><nobr><font color=\"#333366\">(Destroy all existing data.)", JLabel.LEFT );

			myRadioSuperimpose = new JRadioButton( "<html><nobr><b>Superimpose</b>" );
			group.add( myRadioSuperimpose );
			myRadioSuperimpose.addActionListener( (ActionListener)this );
			JLabel labelSuperimpose = new JLabel( "<html><nobr><font color=\"#333366\">(Preserve existing data for parameters the input does not mention.)", JLabel.LEFT );

			myRadioDestroy.setSelected( true );

			JComponent panelSuperimpose = myCreationOptionsPanel;//new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();

			int sizeSpacer = 16;

			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 1;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			panelSuperimpose.add( myRadioDestroy, c );
			panelSuperimpose.add( Box.createHorizontalStrut( sizeSpacer ), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			panelSuperimpose.add( labelDestroy, c );

			c.gridwidth = 1;
			c.weightx = 0;
			panelSuperimpose.add( myRadioSuperimpose, c );
			panelSuperimpose.add( Box.createHorizontalStrut( sizeSpacer ), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			panelSuperimpose.add( labelSuperimpose, c );

			c.weightx = 0;
			myCreationOptionsPanel.add( Box.createVerticalStrut(8), c );

			//panelSuperimpose.setBorder( BorderFactory.createEtchedBorder() );

			myCBNormalize = new JCheckBox( "<html><nobr><b>Normalize</b>" );
			JLabel labelNormalize = new JLabel( "<html><nobr><font color=\"#333366\">(Normalize finished data.)", JLabel.LEFT );
			JComponent pnlNormalize = myCreationOptionsPanel;//new JPanel( new GridBagLayout() );

			c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 1;
			c.weightx = 0;
			pnlNormalize.add( myCBNormalize, c );
			pnlNormalize.add( Box.createHorizontalStrut( sizeSpacer ), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			pnlNormalize.add( labelNormalize, c );

			//pnlNormalize.setBorder( BorderFactory.createEtchedBorder() );

			//c = new GridBagConstraints();
			//c.gridwidth = GridBagConstraints.REMAINDER;
			//c.weightx = 1;
			//c.fill = GridBagConstraints.HORIZONTAL;

			//myCreationOptionsPanel.add( panelSuperimpose, c );
			//myCreationOptionsPanel.add( pnlNormalize, c );
		}
		return myCreationOptionsPanel;
	}

	public static final String STR_PARAMETER_DEFINED = "<html><nobr><b>parameter</b>: one double-precision floating-point cpt entry";
	public static final String STR_REDUNDANCY_DEFINED = "<html><nobr><b>redundancy</b>: when two or more rows of the input file specify identical values for a given parameter";
	public static final String STR_CONFLICT_DEFINED = "<html><nobr><b>conflict</b>: when two or more rows of the input file specify different values for a given parameter";
	public static final String STR_AGREEMENT_DEFINED = "<html><nobr><b>agree</b>: the value specified by an input row is identical to the existing value";
	public static final String STR_DISAGREEMENT_DEFINED = "<html><nobr><b>differ</b>: the value specified by an input row differs from the existing value";
	public static final String STR_CONDITION_DEFINED = "<html><nobr><b>condition</b>: a particular instantiation over each parent variable, i.e. one column in the cpt";
	public static final String STR_DISTANCE_DEFINED = "<html><nobr><b>distance_measure</b> = log( max_ratio / min_ratio )";

	private JComponent refreshPanelResults() throws Exception {
		if( myPanelResults == null ){
			myButtonCreate = makeButton( "Create", "Read all parameters from the input file and show statistics" );

			myButtonFinish = makeButton( "Finish", "Transfer imported data into the cpt (normalized if selected)" );
			int indentX1 = 16;
			int indentX2 = 16;
			String indentPrefix = "<html><nobr>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

			JPanel panelInfo = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			panelInfo.add( myLabelCaption = makeResultLabel( STR_RESULT_UNKNOWN ), c );
			panelInfo.add( Box.createVerticalStrut(16), c );

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelInfoParameters = makeResultLabel(), c );
			panelInfo.add( Box.createHorizontalStrut(indentX1), c );
			c.anchor = GridBagConstraints.WEST;
			panelInfo.add( makeResultLabel( "<html><nobr><b>parameters</b> out of ", STR_PARAMETER_DEFINED ), c );
			panelInfo.add( Box.createHorizontalStrut(8), c );
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelTotalParameters = makeResultLabel(), c );
			c.anchor = GridBagConstraints.WEST;
			panelInfo.add( makeResultLabel( " total" ), c );
			panelInfo.add( makeResultLabel( " (" ), c );
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelParametersPercent = makeResultLabel(), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.WEST;
			panelInfo.add( makeResultLabel( ")." ), c );

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelRedundants = makeResultLabel(), c );
			panelInfo.add( Box.createHorizontalStrut(indentX2), c );
			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 4;
			panelInfo.add( makeResultLabel( indentPrefix + "are <b>redundant</b>", STR_REDUNDANCY_DEFINED ), c );
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			c.gridwidth = 1;
			panelInfo.add( makeResultLabel( " (" ), c );
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelRedundantsPercent = makeResultLabel(), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.WEST;
			panelInfo.add( makeResultLabel( ")." ), c );

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelConflicts = makeResultLabel(), c );
			panelInfo.add( Box.createHorizontalStrut(indentX2), c );
			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 4;
			panelInfo.add( makeResultLabel( indentPrefix + "<b>conflict</b> with each other", STR_CONFLICT_DEFINED ), c );
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			c.gridwidth = 1;
			panelInfo.add( makeResultLabel( " (" ), c );
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelConflictsPercent = makeResultLabel(), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.WEST;
			panelInfo.add( makeResultLabel( ")." ), c );

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelSpared = makeResultLabel(), c );
			panelInfo.add( Box.createHorizontalStrut(indentX2), c );
			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 4;
			panelInfo.add( makeResultLabel( indentPrefix + "<b>agree</b> with existing", STR_AGREEMENT_DEFINED ), c );
			c.gridwidth = 1;
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			panelInfo.add( makeResultLabel( " (" ), c );
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelSparedPercent = makeResultLabel(), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.WEST;
			panelInfo.add( makeResultLabel( ")." ), c );

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelChanged = makeResultLabel(), c );
			panelInfo.add( Box.createHorizontalStrut(indentX2), c );
			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 4;
			panelInfo.add( makeResultLabel( indentPrefix + "<b>differ</b> from existing", STR_DISAGREEMENT_DEFINED ), c );
			c.gridwidth = 1;
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			//panelInfo.add( Box.createHorizontalStrut(1), c );
			panelInfo.add( makeResultLabel( " (" ), c );
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelChangedPercent = makeResultLabel(), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.WEST;
			panelInfo.add( makeResultLabel( ")." ), c );

			panelInfo.add( Box.createVerticalStrut(16), c );

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelInfoConditions = makeResultLabel(), c );
			panelInfo.add( Box.createHorizontalStrut(indentX1), c );
			c.anchor = GridBagConstraints.WEST;
			panelInfo.add( makeResultLabel( "<html><nobr><b>conditions</b> out of ", STR_CONDITION_DEFINED ), c );
			panelInfo.add( Box.createHorizontalStrut(8), c );
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelTotalConditions = makeResultLabel(), c );
			c.anchor = GridBagConstraints.WEST;
			panelInfo.add( makeResultLabel( " total" ), c );
			panelInfo.add( makeResultLabel( " (" ), c );
			c.anchor = GridBagConstraints.EAST;
			panelInfo.add( myLabelConditionsPercent = makeResultLabel(), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.WEST;
			panelInfo.add( makeResultLabel( ")." ), c );

			panelInfo.add( Box.createVerticalStrut(16), c );

			c.gridwidth = 1;
			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 4;
			panelInfo.add( makeResultLabel( "<html><nobr><b>Distance</b> from existing data: ", STR_DISTANCE_DEFINED ), c );
			panelInfo.add( Box.createHorizontalStrut(8), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			panelInfo.add( myLabelDistance = makeResultLabel(), c );

			myPanelResults = new JPanel( new GridBagLayout() );
			c = new GridBagConstraints();

			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			myPanelResults.add( getCreationOptionsPanel(), c );
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			myPanelResults.add( Box.createVerticalStrut( 16 ), c );

			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 1;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			myPanelResults.add( myButtonCreate, c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			myPanelResults.add( Box.createHorizontalStrut(1), c );

			c.weightx = 0;
			myPanelResults.add( Box.createVerticalStrut(16), c );

			c.weightx = 1;
			c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			//myPanelResults.add( panelInfo, c );
			JScrollPane scrollpain = new JScrollPane( panelInfo );
			myPanelResults.add( scrollpain, c );

			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			c.weighty = 0;
			myPanelResults.add( Box.createVerticalStrut(16), c );

			c.gridwidth = GridBagConstraints.RELATIVE;
			c.weightx = 1;
			myPanelResults.add( Box.createHorizontalStrut(1), c );
			c.weightx = 0;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.EAST;
			myPanelResults.add( myButtonFinish, c );

			border( myPanelResults );
		}
		CPTImport scanner = CPTImportWizard.this.getCPTDataScanner();
		File inputfile = scanner.getInputFile();
		if( inputfile == null ) throw new RuntimeException( "no input file" );
		String filename = NetworkIO.extractFileNameFromPath( inputfile.getPath() );
		//System.out.println( "caption? " + filename );
		myLabelCaption.setText( "<html><nobr>Using the current assignments, <b>" + filename + "</b> mentions:" );
		setResultsValid( false );
		return myPanelResults;
	}

	private void setResultsValid( boolean flag ){
		setValid( myLabelInfoParameters, flag );
		setValid( myLabelTotalParameters, flag );
		setValid( myLabelParametersPercent, flag );
		setValid( myLabelConflicts, flag );
		setValid( myLabelConflictsPercent, flag );
		setValid( myLabelRedundants, flag );
		setValid( myLabelRedundantsPercent, flag );
		setValid( myLabelInfoConditions, flag );
		setValid( myLabelTotalConditions, flag );
		setValid( myLabelConditionsPercent, flag );
		setValid( myLabelSpared, flag );
		setValid( myLabelSparedPercent, flag );
		setValid( myLabelChanged, flag );
		setValid( myLabelChangedPercent, flag );
		setValid( myLabelDistance, flag );
		myButtonFinish.setEnabled( flag );
	}

	private void setValid( JLabel label, boolean flag ){
		label.setEnabled( flag );
		if( !flag ) label.setText( STR_RESULT_UNKNOWN );
	}

	private JLabel makeResultLabel(){
		return makeResultLabel( STR_RESULT_UNKNOWN, JLabel.RIGHT, COLOR_RESULT, true );
	}

	private JLabel makeResultLabel( String text, String tooltip ){
		JLabel ret = makeResultLabel( text );
		ret.setToolTipText( tooltip );
		return ret;
	}

	private JLabel makeResultLabel( String text ){
		return makeResultLabel( text, JLabel.LEFT, Color.black, false );
	}

	private JLabel makeResultLabel( String text, int align, Color foreground, boolean bold ){
		JLabel ret = new JLabel( text, align );
		ret.setFont( ret.getFont().deriveFont( (float)16 ) );
		if( bold ) ret.setFont( ret.getFont().deriveFont( Font.BOLD ) );
		ret.setForeground( foreground );
		//Dimension preferred = ret.getPreferredSize();
		//System.out.println( "preferred? " + preferred );
		//ret.setMaximumSize( new Dimension( 1000, preferred.height ) );
		//System.out.println( "maximum? " + ret.getMaximumSize() );
		return ret;
	}

	private JComponent myPanelResults, myCreationOptionsPanel;
	private JButton myButtonCreate, myButtonFinish;
	private JCheckBox myCBNormalize;
	private JRadioButton myRadioDestroy, myRadioSuperimpose;
	private JLabel myLabelCaption, myLabelDistance;
	private JLabel myLabelInfoParameters, myLabelTotalParameters, myLabelParametersPercent;
	private JLabel myLabelRedundants, myLabelRedundantsPercent;
	private JLabel myLabelConflicts, myLabelConflictsPercent;
	private JLabel myLabelSpared, myLabelSparedPercent;
	private JLabel myLabelChanged, myLabelChangedPercent;
	private JLabel myLabelInfoConditions, myLabelTotalConditions, myLabelConditionsPercent;

	public static final String STR_RESULT_UNKNOWN = "----";
	public static final Color COLOR_RESULT = new Color( 0x00, 0x00, 0x66 );
	public static final Color COLOR_RESULT_WARNING = new Color( 0xcc, 0x00, 0x00 );

	public void doCreate(){
		myRunCreate.start();
	}

	public Interruptable myRunCreate = new Interruptable(){
		public void runImpl( Object arg1 ) throws InterruptedException{
			CPTImportWizard.this.doCreateImpl();
		}
	};

	public void doCreateImpl() throws InterruptedException{
		CPTImport scanner = null;
		int progress = (int)-1;
		int redundants = (int)-1;
		int conflicts = (int)-1;
		int infoParameters = (int)-1;
		int infoConditions = (int)-1;
		int totalParameters = (int)-1;
		int totalConditions = (int)-1;
		int spared = (int)-1;
		int changed = (int)-1;
		double distance = (double)-1;
		String error_message = null;
		String error_title = "Error";
		try{
			scanner = CPTImportWizard.this.getCPTDataScanner();
			FiniteVariable variable = CPTImportWizard.this.getVariable();
			CPTInfo.ReadableWritableTable destination = CPTImportWizard.this.myDestination;
			String columnNameProbability = CPTImportWizard.this.myComboGroupColumns.tokenForElement( STR_PROBABILITY );
			myCPTStruct = scanner.createCPT( new CPTInfo( variable, destination ), columnNameProbability );
			//if( myRadioDestroy.isSelected() ) myCPTStruct.normalize();
			progress = scanner.getProgress();
			redundants = myCPTStruct.getNumRedundant();
			conflicts = myCPTStruct.getNumConflicts();
			spared = myCPTStruct.getNumSpared();
			changed = myCPTStruct.getNumChanged();
			//distance = myCPTStruct.getDistanceMeasure( myRadioSuperimpose.isSelected() );
			infoParameters = myCPTStruct.countInformationParameters();
			infoConditions = myCPTStruct.countInformationConditions();
			totalParameters = myCPTStruct.length;
			totalConditions = myCPTStruct.length / variable.size();

			myLabelInfoParameters.setText( Integer.toString( infoParameters ) );
			myLabelTotalParameters.setText( Integer.toString( totalParameters ) );
			myLabelParametersPercent.setText( percentToString( infoParameters, totalParameters ) );

			myLabelRedundants.setText( Integer.toString( redundants ) );
			myLabelRedundantsPercent.setText( percentToString( redundants, infoParameters ) );

			Color colorRedundants = ( redundants > 0 ) ? COLOR_RESULT_WARNING : COLOR_RESULT;
			myLabelRedundants.setForeground( colorRedundants );
			myLabelRedundantsPercent.setForeground( colorRedundants );

			myLabelConflicts.setText( Integer.toString( conflicts ) );
			myLabelConflictsPercent.setText( percentToString( conflicts, infoParameters ) );

			Color colorConflicts = ( conflicts > 0 ) ? COLOR_RESULT_WARNING : COLOR_RESULT;
			myLabelConflicts.setForeground( colorConflicts );
			myLabelConflictsPercent.setForeground( colorConflicts );

			myLabelSpared.setText( Integer.toString( spared ) );
			myLabelSparedPercent.setText( percentToString( spared, infoParameters ) );

			myLabelChanged.setText( Integer.toString( changed ) );
			myLabelChangedPercent.setText( percentToString( changed, infoParameters ) );

			myLabelInfoConditions.setText( Integer.toString( infoConditions ) );
			myLabelTotalConditions.setText( Integer.toString( totalConditions ) );
			myLabelConditionsPercent.setText( percentToString( infoConditions, totalConditions ) );

			CPTImportWizard.this.doDistance();

			setResultsValid( true );
		}catch( NumberFormatException numberformatexception ){
			error_message = "Cannot parse probability number.\n";
			error_message += numberformatexception.getMessage();
			error_title = "Parse Error: Probability Number";
		}catch( Throwable throwable ){
			if( throwable instanceof InterruptedException ) throw (InterruptedException) throwable;
			else{
				error_message = throwable.toString();
				throwable.printStackTrace();
			}
		}finally{
			//System.out.println( "progress: " + progress );
			//System.out.println( "conflicts: " + conflicts );
			//System.out.println( "spared: " + spared );
			//System.out.println( "changed: " + changed );
			////System.out.println( "distance: " + distance );
			//System.out.println( "infoParameters: " + infoParameters );
			//System.out.println( "totalParameters: " + totalParameters );
			//System.out.println( "infoConditions: " + infoConditions );
			//System.out.println( "totalConditions: " + totalConditions );
			if( error_message != null ) JOptionPane.showMessageDialog( myPanelResults, error_message, error_title, JOptionPane.ERROR_MESSAGE );
		}
	}

	private CPTInfo myCPTStruct;

	/** @since 030805 */
	public CPTInfo getCPTInfo(){
		return myCPTStruct;
	}

	public static String percentToString( float numerator, float denominator ){
		if( myPercentFormat == null ) myPercentFormat = new DecimalFormat( "0.##%" );
		return myPercentFormat.format( numerator/denominator );
	}
	private static DecimalFormat myPercentFormat;

	public static String doubleToString( double arg ){
		if( myDoubleFormat == null ) myDoubleFormat = new DecimalFormat( "0.######" );
		return myDoubleFormat.format( arg );
	}
	private static DecimalFormat myDoubleFormat;

	private StageColumnIdentification STAGE_IDENTIFY_COLUMNS = new StageColumnIdentification();

	public class StageColumnIdentification extends Stage
	{
		public StageColumnIdentification(){
			super( "Identify Columns" );
		}

		public String getProgressMessage(){
			return "identify columns";
		}

		public JComponent refresh() throws Exception {
			return CPTImportWizard.this.refreshPanelIdentifyColumns();
		}

		public boolean isGreenLightNext(){
			//System.out.println( "STAGE_IDENTIFY_COLUMNS.isGreenLightNext()" );
			if( myComboGroupColumns == null ) return false;
			else return myComboGroupColumns.isIdentified();
		}

		public String getWarning(){
			Set unselected = myComboGroupColumns.getUnselectedSet( (Set)null );
			unselected.remove( myComboGroupColumns.getModel().getElementUnidentified() );
			unselected.remove( STR_PROBABILITY );
			if( !unselected.isEmpty() ){
				int numunassigned = unselected.size();
				String prefix = (numunassigned == 1) ? "One variable" : (Integer.toString(numunassigned) + " variables" );
				return prefix + " unassigned.";
			}
			return (String)null;
		}

		public Stage next() throws Exception {
			if( super.next() != null ) return super.next();

			if( !isGreenLightNext() ) return null;

			//String columnNameProbability = myComboGroupColumns.tokenForElement( STR_PROBABILITY );

			CPTImport scanner = getCPTDataScanner();
			scanner.scanRows();

			if( myListInstanceIDStages == null ) myListInstanceIDStages = new LinkedList();
			else myListInstanceIDStages.clear();

			StageInstanceIdentification newstage;
			Stage nextstage = STAGE_END;
			Map selected = myComboGroupColumns.getSelectedMap( (Map)null );
			Object key;
			Object element;
			FiniteVariable assignment;
			for( Iterator it = selected.keySet().iterator(); it.hasNext(); ){
				key = it.next();
				element = selected.get( key );
				if( element instanceof FiniteVariable ){
					assignment = (FiniteVariable)element;
					newstage = new StageInstanceIdentification( (FiniteVariable)element, scanner.forToken( key.toString() ) );
					if( nextstage != null ) newstage.setNext( nextstage );
					nextstage = newstage;
					myListInstanceIDStages.add( newstage );
				}
				else assignment = (FiniteVariable)null;
				scanner.assign( key.toString(), assignment );
			}

			if( nextstage == null ) throw new IllegalStateException( "failed to create stages" );

			super.setNext( nextstage );
			return super.next();
		}

		public void invalidateFuture(){
			super.invalidateFuture();
			super.setNext( (Stage)null );
			//STAGE_END.invalidate();
		}

		public List getInstanceIDStages(){
			if( myListInstanceIDStages == null ) return Collections.EMPTY_LIST;
			else return myListInstanceIDStages;
		}

		private List myListInstanceIDStages;
	};

	public class StageInstanceIdentification extends Stage
	{
		public StageInstanceIdentification( FiniteVariable var, CPTImportColumnInfo struct ){
			super( "Identify Instances for \""+var.toString()+"\"" );
			this.myVariable = var;
			this.myColumnStruct = struct;
			this.myTokens = new ArrayList( struct.getValues() );
			Collections.sort( myTokens, java.text.Collator.getInstance() );
			this.myFlagTokenZeroExists = struct.getValues().contains( "0" );
		}

		public String getProgressMessage(){
			return "identify instances for \"" + myVariable.toString() + "\"";
		}

		public JComponent refresh() throws Exception {
			InstanceIdentificationModel model = new InstanceIdentificationModel( myVariable, (Stage)this );
			model.setIndicesBeginAtOne( !myFlagTokenZeroExists );
			myComboGroup = new ComboBoxGroup( model );
			JPanel ret = myComboGroup.fillPanel( (JPanel)null, myTokens.iterator() );
			myComboGroup.addItemListener( CPTImportWizard.this.getItemListener() );
			myComboGroup.guess();
			return ret;
		}

		public boolean isGreenLightNext(){
			//System.out.println( "StageInstanceIdentification.isGreenLightNext()" );
			if( myComboGroup == null ) return false;
			else return myComboGroup.isIdentified();
		}

		public void updateScanner(){
			Map selected = myComboGroup.getSelectedMap( (Map)null );
			Object unidentified = myComboGroup.getModel().getElementUnidentified();
			Object token;
			Object value;
			for( Iterator it = selected.keySet().iterator(); it.hasNext(); ){
				token = it.next();
				value = selected.get( token );
				if( value == unidentified ) throw new RuntimeException( "illegal to call updateScanner() with unidentified token" );
				myColumnStruct.mapInstance( token.toString(), value );
			}
		}

		private ComboBoxGroup myComboGroup;
		private FiniteVariable myVariable;
		private ArrayList myTokens;
		private boolean myFlagTokenZeroExists;
		private CPTImportColumnInfo myColumnStruct;
	}

	private JComponent refreshPanelBrowse() throws Exception {
		if( myPanelBrowse == null ){
			myPanelBrowse = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = 1;
			myPanelBrowse.add( new JLabel( "Input file: " ), c );
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			myPanelBrowse.add( myTfBrowse = new NotifyField( "", 5 ), c );
			myTfBrowse.addActionListener( (ActionListener)this );
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			c.gridwidth = GridBagConstraints.REMAINDER;
			myPanelBrowse.add( myButtonBrowse = makeButton( "Browse", "Browse for a tab-delimited data file" ), c );
			border( myPanelBrowse );
		}
		return myPanelBrowse;
	}

	private JPanel myPanelBrowse;
	private NotifyField myTfBrowse;
	private JButton myButtonBrowse;

	public JComponent refreshPanelIdentifyColumns() throws Exception {
		File fileInput = new File( myTfBrowse.getText() );
		if( !fileInput.exists() ) throw new IllegalStateException( "file \"" +fileInput.getAbsolutePath()+ "\" not found" );
		CPTImport scanner = getCPTDataScanner();
		scanner.scanColumnNames( fileInput );
		List columnNames = scanner.getColumns();

		if( columnNames.size() < 2 ) throw new IllegalStateException( "file must contain at least 2 columns" );

		myComboGroupColumns = new ComboBoxGroup( new ColumnIdentificationModel( myVariable, STAGE_IDENTIFY_COLUMNS ) );
		myPanelIdentifyColumns = myComboGroupColumns.fillPanel( myPanelIdentifyColumns, columnNames.iterator() );
		myComboGroupColumns.addItemListener( CPTImportWizard.this.getItemListener() );
		myComboGroupColumns.guess();
		return myPanelIdentifyColumns;
	}

	//public static final String STR_UNIDENTIFIED = "unidentified";
	//public static final String STR_PROBABILITY = "probability";

	public static final String STR_UNIDENTIFIED = "<html><nobr><font color=\"#CC0000\">unidentified";
	public static final String STR_PROBABILITY = "<html><nobr><font color=\"#000099\">probability";

	private JPanel myPanelIdentifyColumns;
	private CPTImport myCPTDataScanner;
	private FiniteVariable myVariable;
	private CPTInfo.ReadableWritableTable myDestination;
	private ComboBoxGroup myComboGroupColumns;

	private CPTImport getCPTDataScanner(){
		if( myCPTDataScanner == null ) myCPTDataScanner = new CPTImport();
		return myCPTDataScanner;
	}

	public static final Dimension DIM_WINDOW_DEFAULT = new Dimension( 500, 460 );

	/** @since 032805 */
	public Dimension getPreferredSize(){
		return DIM_WINDOW_DEFAULT;
	}

	/** @since 022505 */
	public CPTInfo showDialog( Component parentComponent, DisplayableFiniteVariable var, CPTInfo.ReadableWritableTable destination ){
		UI ui = var.getNetworkInternalFrame().getParentFrame();
		SamiamPreferences prefs = ui.getPackageOptions();
		CPTImportWizard cdswp = this;//new CPTImportWizard();
		cdswp.myDefaultDirectory  = prefs.defaultDirectory;
		//cdswp.setPreferredSize( DIM_WINDOW_DEFAULT );
		cdswp.setVariable( var, destination );
		//JOptionResizeHelper helper = new JOptionResizeHelper( cdswp, true, (long)5000 );
		//helper.start();
		//JOptionPane.showMessageDialog( parentComponent, cdswp, "", JOptionPane.PLAIN_MESSAGE );
		/*JOptionPane.showOptionDialog(
			parentComponent,
			/-*message*-/cdswp,
			/-*title*-/"",
			/-*optionType*-/0,
			/-*messageType*-/JOptionPane.PLAIN_MESSAGE,
			/-*icon*-/(Icon)null,
			/-*Object[] options*-/new Object[0],
			/-*Object initialValue*-/null);*/
		//Util.makeDialog( parentComponent, cdswp, "", /*modal*/true ).setVisible( true );
		getWizardPanel().showDialog( parentComponent );
		return cdswp.getCPTInfo();
	}

	/** test/debug */
	public static void main( String[] args ){
		//String pathData = "C:\\keithcascio\\networks\\jgarcia\\Survival.txt";
		//String pathNetwork = "C:\\keithcascio\\networks\\jgarcia\\jgarcia_BrainTumor2_extend.net";
		String pathData = "C:\\keith\\code\\argroup\\networks\\jgarcia\\Survival_edited.txt";
		String pathNetwork = "C:\\keith\\code\\argroup\\networks\\jgarcia\\jgarcia_BrainTumor2_extend.net";
		Util.setLookAndFeel();

		BeliefNetwork bn = null;
		try{
			bn = NetworkIO.read( pathNetwork );
		}catch( Exception e ){
			System.err.println( "Error: Failed to read " + pathNetwork );
			return;
		}

		FiniteVariable varSurvival = (FiniteVariable) bn.forID( "Survival" );
		Table cpt = varSurvival.getCPTShell( DSLNodeType.CPT ).getCPT();

		CPTImportWizard cdswp = new CPTImportWizard();
		WizardPanel wp = cdswp.getWizardPanel();

		JFrame frame = Util.getDebugFrame( "?", wp );
		frame.setSize( DIM_WINDOW_DEFAULT );

		cdswp.setVariable( varSurvival, cpt );//, bn );
		wp.reset();

		cdswp.myTfBrowse.setText( pathData );
		frame.setVisible( true );
	}
}
