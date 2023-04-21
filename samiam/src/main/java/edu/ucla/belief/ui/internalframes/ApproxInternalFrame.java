package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.*;
import edu.ucla.structure.DirectedEdge;
import edu.ucla.belief.approx.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.networkdisplay.NetworkDisplay;
import edu.ucla.belief.ui.networkdisplay.Decorator;
import edu.ucla.belief.ui.networkdisplay.Arrow;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariable;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
	@since 061004
	@author Hei Chan
	@author Keith Cascio
*/
public class ApproxInternalFrame extends JInternalFrame implements
	ActionListener,
	ItemListener,
	Decorator,
	NetworkDisplay.UserInputListener
{
	public static final Color COLOR_REMOVED_EDGE = new Color( 170, 64, 16 );//Color.blue;
	public static final Color COLOR_REMOVED_EDGE_DARKER = new Color( 150, 54, 8 );
	public static final Color COLOR_REMOVED_EDGE_LIGHTER = new Color( 190, 74, 32 );

	public static final double DOUBLE_START_FLOOR = (double)0;
	public static final double DOUBLE_START_CEILING = (double)1;

	public static final int INT_LOOPS_FLOOR = (int)1;
	public static final int INT_LOOPS_CEILING = (int)9999;

	protected NetworkInternalFrame nif;
	protected PreferenceGroup globalPrefs;
	protected ApproxEngine myApproxEngine;
	protected ApproxReport report;
	protected Vector parentList, childList, startList;
	protected Set mySetRemovedEdges;
	protected JComponent myPanelMain;
	protected VariableComboBox parentBox, childBox;
	protected DecimalField startField, myStartFieldPrompt;
	protected WholeNumberField maxLoopsField;
	protected JButton startButton, addButton, clearButton, cutButton, restoreButton, myButtonPromptRemoveEdge;
	protected JPanel inputPanel, outputPanel, editPanel;

	private JTabbedPane myJTabbedPane;
	private JComponent myPanelReport, myStartInputComponent;
	private JLabel myLabelConv, myLabelLoops, myLabelOrigPrE,
		myLabelCondPrE, myLabelCondKLTotal, myLabelCondKLBound,
		myLabelFixedPrE, myLabelFixedKLTotal, myLabelFixedKLBound;
	private JLabel myLabelEdgePrompt;

	private final DirectedEdge DIRECTEDEDGE_UTIL = new DirectedEdge(null,null);

	/**
		interface edu.ucla.belief.ui.networkdisplay.Decorator
		@since 061304
	*/
	public void decorateArrow( Arrow arrow, NetworkInternalFrame nif )
	{
		DisplayableFiniteVariable source = arrow.getStart().getFiniteVariable();
		DisplayableFiniteVariable sink = arrow.getEnd().getFiniteVariable();

		if( source == null || sink == null ) return;

		DIRECTEDEDGE_UTIL.setVertices( source, sink );

		if( mySetRemovedEdges.contains(DIRECTEDEDGE_UTIL) ){
			arrow.setDrawColor( COLOR_REMOVED_EDGE );
		}
	}

	public ApproxInternalFrame( NetworkInternalFrame nif, PreferenceGroup globalPrefs )
	{
		super ("Approximation", true, true, true, true);
		this.nif = nif;
		this.globalPrefs = globalPrefs;
		myApproxEngine = new ApproxEngine( nif.getBeliefNetwork(), nif.getInferenceEngine(), nif.console );
		report = null;
		parentList = new Vector();
		childList = new Vector();
		startList = new Vector();
		mySetRemovedEdges = new HashSet();
		init();

		nif.getNetworkDisplay().addDecorator( this );
	}

	/** @since 061704 */
	private void setTabReportEnabled( boolean flag ){
		myJTabbedPane.setEnabledAt( myJTabbedPane.indexOfComponent( myPanelReport ), flag );
	}

	public void init() {
		myJTabbedPane = new JTabbedPane();
		myJTabbedPane.add( "Remove Edges", initMainPanel() );
		myJTabbedPane.add( "Report", myPanelReport = initReportPanel() );
		setTabReportEnabled( false );
		getContentPane().add( myJTabbedPane );
	}

	public JComponent initReportPanel() {
		JPanel ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		JLabel label;

		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( label = new JLabel( "Summary:" ), c );
		label.setForeground( COLOR_REMOVED_EDGE );

		c.gridwidth = 1;
		ret.add( new JLabel( "Converges?" ), c );
		ret.add( new JLabel( " = " ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelConv = new JLabel( "?" ), c );

		c.gridwidth = 1;
		ret.add( new JLabel( "Loops to converge" ), c );
		ret.add( new JLabel( " = " ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelLoops = new JLabel( "?" ), c );

		c.gridwidth = 1;
		ret.add( new JLabel( "Orig Pr(e)" ), c );
		ret.add( new JLabel( " = " ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelOrigPrE = new JLabel( "?" ), c );

		c.gridwidth = 1;
		ret.add( new JLabel( "Marginals Pr'(e)" ), c );
		ret.add( new JLabel( " = " ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelCondPrE = new JLabel( "?" ), c );

		c.gridwidth = 1;
		ret.add( new JLabel( "Fixed point Pr^(e)" ), c );
		ret.add( new JLabel( " = " ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelFixedPrE = new JLabel( "?" ), c );

		c.gridwidth = 1;
		ret.add( new JLabel( "KL(N,N',e)" ), c );
		ret.add( new JLabel( " = " ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelCondKLTotal = new JLabel( "?" ), c );

		c.gridwidth = 1;
		ret.add( new JLabel( "KL(N,N^,e)" ), c );
		ret.add( new JLabel( " = " ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelFixedKLTotal = new JLabel( "?" ), c );

		c.gridwidth = 1;
		ret.add( new JLabel( "KL(N,N',e) bound" ), c );
		ret.add( new JLabel( " = " ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelCondKLBound = new JLabel( "?" ), c );

		c.gridwidth = 1;
		ret.add( new JLabel( "KL(N,N^,e) bound" ), c );
		ret.add( new JLabel( " = " ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myLabelFixedKLBound = new JLabel( "?" ), c );

		return ret;
	}

	public JComponent initMainPanel() {
		BeliefNetwork bn = nif.getBeliefNetwork();
		myPanelMain = new JPanel(new BorderLayout());
		inputPanel = new JPanel();
		outputPanel = new JPanel();

		parentBox = nif.createVariableComboBox();
		parentBox.addSelectedChangeListener( this );
		Dimension dX = parentBox.getPreferredSize();
		dX.width = 50;
		parentBox.setPreferredSize(dX);
		inputPanel.add(parentBox);

		childBox = nif.createVariableComboBox();
		Dimension dY = childBox.getPreferredSize();
		dY.width = 50;
		childBox.setPreferredSize(dY);
		inputPanel.add(childBox);

		startField = new DecimalField( 0.5, 4, DOUBLE_START_FLOOR, DOUBLE_START_CEILING );
		inputPanel.add(startField);

		addButton = new JButton("Add");
		addButton.addActionListener(this);
		inputPanel.add(addButton);

		maxLoopsField = new WholeNumberField( 100, 4, INT_LOOPS_FLOOR, INT_LOOPS_CEILING );
		inputPanel.add(maxLoopsField);

		startButton = new JButton("Start");
		startButton.addActionListener(this);
		inputPanel.add(startButton);

		editPanel = new JPanel();

		myButtonPromptRemoveEdge = new JButton( "Graphical Remove Edge" );
		myButtonPromptRemoveEdge.addActionListener(this);
		editPanel.add( myButtonPromptRemoveEdge );

		//cutButton = new JButton("Cut");
		//cutButton.addActionListener(this);
		//editPanel.add(cutButton);

		//restoreButton = new JButton("Restore");
		//restoreButton.addActionListener(this);
		//editPanel.add(restoreButton);

		clearButton = new JButton("Clear");
		clearButton.addActionListener(this);
		editPanel.add(clearButton);

		myPanelMain.add(inputPanel, BorderLayout.NORTH);
		outputPanel = new JPanel();
		myPanelMain.add(outputPanel, BorderLayout.CENTER);
		myPanelMain.add(editPanel, BorderLayout.SOUTH);
		//getContentPane().add(myPanelMain);

		if( parentBox.getModel().getSize() > (int)0 ) parentBox.setSelectedIndex( 0 );

		return myPanelMain;
	}

	/** @since 061304 */
	private void setAddFunctionsEnabled( boolean flag ){
		childBox.setEnabled( flag );
		startField.setEnabled( flag );
		addButton.setEnabled( flag );
	}

	/** @since 061304 */
	private static JComponent makeNewEdgeCaption(){
		JLabel ret = new JLabel( " -> " );
		ret.setForeground( COLOR_REMOVED_EDGE );
		return ret;
	}

	/** @since 061304 */
	private static Component makeNewComponent(){
		return Box.createHorizontalStrut( 16 );
	}

	/** @since 061304 */
	private void displayResults()
	{
		outputPanel.removeAll();
		outputPanel.invalidate();
		outputPanel.setLayout( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		nif.setApproxEngine( myApproxEngine );
		nif.monitorTool();
		DisplayableFiniteVariable dVar = null;

		java.util.List parentList = report.getParentList();
		java.util.List childList = report.getChildList();
		for (int i = 0; i < parentList.size(); i++) {
			FiniteVariable parent =
				(FiniteVariable)parentList.get(i);
			FiniteVariable child =
				(FiniteVariable)childList.get(i);
			double[] fixedPoint = report.getFixedPoint(parent);
			c.gridwidth = 1;
			outputPanel.add( new JLabel(parent.getID()), c);
			outputPanel.add( makeNewEdgeCaption(), c );
			outputPanel.add( new JLabel(child.getID()), c );
			outputPanel.add( makeNewComponent(), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			outputPanel.add( new JLabel(
				Double.toString( fixedPoint[0] ) ), c );

			dVar = (DisplayableFiniteVariable)parent;
			dVar.getNodeLabel().setEvidenceDialogShown( true );
			//dVar.getNodeLabel().getEvidenceDialog().setApprox( myApproxEngine );
		}

		outputPanel.validate();
		outputPanel.repaint();
		myPanelMain.repaint();
		getContentPane().repaint();
	}

	/** @since 061704 */
	private void displayReport()
	{
		boolean conv = report.converges();
		int loops = report.getLoops();
		double origProbE = report.getOrigProbE();
		double condProbE = report.getCondProbE();
		double condKLTotal = report.getCondKLTotal();
		double condKLBound = report.getCondKLBound();
		double fixedProbE = report.getFixedProbE();
		double fixedKLTotal = report.getFixedKLTotal();
		double fixedKLBound = report.getFixedKLBound();

		myLabelConv.setText( Boolean.toString( conv ) );
		myLabelLoops.setText( Integer.toString( loops ) );
		myLabelOrigPrE.setText( Double.toString( origProbE ) );
		myLabelCondPrE.setText( Double.toString( condProbE ) );
		myLabelFixedPrE.setText( Double.toString( fixedProbE ) );
		myLabelCondKLTotal.setText( Double.toString( condKLTotal ) );
		myLabelFixedKLTotal.setText( Double.toString( fixedKLTotal ) );
		myLabelCondKLBound.setText( Double.toString( condKLBound ) );
		myLabelFixedKLBound.setText( Double.toString( fixedKLBound ) );

		setTabReportEnabled( true );
	}

	/** @since 061304 */
	private void displayRemovedEdges(){
		outputPanel.removeAll();
		outputPanel.invalidate();

		int sizeCurrent = parentList.size();
		//outputPanel.setLayout( new GridLayout( sizeCurrent, 4 ) );
		outputPanel.setLayout( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		for( int i = 0; i < sizeCurrent; i++ ){
			c.gridwidth = 1;
			outputPanel.add( new JLabel( ((Variable)parentList.get(i)).getID() ), c );
			outputPanel.add( makeNewEdgeCaption(), c );
			outputPanel.add( new JLabel( ((Variable)childList.get(i)).getID() ), c );
			outputPanel.add( makeNewComponent(), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			outputPanel.add( new JLabel( startList.get(i).toString() ), c );
		}

		outputPanel.validate();
		outputPanel.repaint();
		myPanelMain.repaint();
		getContentPane().repaint();
	}

	/** @since 061304 */
	public void itemStateChanged( ItemEvent e ){
		Object parent = parentBox.getSelectedItem();
		Set outgoing = nif.getBeliefNetwork().outGoing( parent );
		childBox.setVariables( outgoing );
		setAddFunctionsEnabled( !outgoing.isEmpty() );
	}

	/** @since 061704 */
	public void promptUserRemoveEdge()
	{
		NetworkDisplay display = nif.getNetworkDisplay();
		display.promptUserEdge( "ApproxInternalFrame", this );
		nif.networkTool();
	}
	/** interface NetworkDisplay.UserInputListener @since 061704 */
	public void handleUserActualPoint( Point pActual ){}
	/** interface NetworkDisplay.UserInputListener @since 061704 */
	public void userInputCancelled()
	{
		safeToFront();
		JOptionPane.showMessageDialog( (Component)this, (Object)"Cancelled: no edge removed", (String)"Edge Removal Cancelled", (int)JOptionPane.WARNING_MESSAGE );
	}
	/** interface NetworkDisplay.UserInputListener @since 061704 */
	public void handleUserEdge( DisplayableFiniteVariable source, DisplayableFiniteVariable sink )
	{
		safeToFront();
		JOptionPane.showMessageDialog( (Component)this, (Object)getStartInputComponent(source,sink), (String)"Start Value", (int)JOptionPane.PLAIN_MESSAGE );
		removeEdge( source, sink, myStartFieldPrompt.getValue() );
	}

	/** @since 061704 */
	private JComponent getStartInputComponent( DisplayableFiniteVariable source, DisplayableFiniteVariable sink )
	{
		if( myStartInputComponent == null ){
			JPanel ret = new JPanel( new GridLayout(3,1) );//GridLayout(int rows, int cols)
			ret.add( new JLabel( "Please enter a start value for edge:                               " ) );
			ret.add( myLabelEdgePrompt = new JLabel( "?" ) );
			ret.add( myStartFieldPrompt = new DecimalField( 0.5, 4, DOUBLE_START_FLOOR, DOUBLE_START_CEILING ) );
			myStartInputComponent = ret;
		}

		myLabelEdgePrompt.setText( source.getID() + " -> " + sink.getID() );

		return myStartInputComponent;
	}

	/** @since 061704 */
	public void safeToFront(){
		this.toFront();
		try{ this.setSelected( true ); }
		catch( java.beans.PropertyVetoException e ) { System.err.println( e ); }
	}

	/** @since 061804 */
	private void reset()
	{
		nif.setApproxEngine( (ApproxEngine)null );
		if( myApproxEngine != null ) {
			myApproxEngine.reset();
			report = null;
		}
		setTabReportEnabled( false );
	}

	/** @since 061704 */
	public void removeEdge( DisplayableFiniteVariable source, DisplayableFiniteVariable sink, double startValue )
	{
		if( !nif.getBeliefNetwork().containsEdge( source, sink ) ) return;

		reset();
		parentList.add(source);
		childList.add(sink);
		mySetRemovedEdges.add( new DirectedEdge( source, sink ) );
		startList.add( new Double( startValue ) );

		displayRemovedEdges();
		nif.getNetworkDisplay().refresh();
	}

	public void actionPerformed( ActionEvent event )
	{
		Object src = event.getSource();
		if( src == startButton )
		{
			int maxLoops = maxLoopsField.getValue();

			report = myApproxEngine.findFixedPoint( parentList, childList, startList, maxLoops );
			report.printStack();

			displayResults();
			displayReport();
		}
		else if( src == addButton ) {
			removeEdge( (DisplayableFiniteVariable) parentBox.getSelectedItem(), (DisplayableFiniteVariable) childBox.getSelectedItem(), startField.getValue() );
		}
		else if (src == cutButton) {
			setTabReportEnabled( false );
			myApproxEngine.cutEdges(report);
			java.util.List childList = report.getChildList();
			for (int i = 0; i < childList.size(); i++)
				nif.fireCPTChanged(new CPTChangeEvent(
					(FiniteVariable)childList.get(i)));
		}
		else if (src == restoreButton) {
			setTabReportEnabled( false );
			myApproxEngine.restoreEdges(report);
			java.util.List childList = report.getChildList();
			for (int i = 0; i < childList.size(); i++)
				nif.fireCPTChanged(new CPTChangeEvent(
					(FiniteVariable)childList.get(i)));
		}
		else if (src == clearButton) {
			reset();
			report = null;
			parentList.clear();
			childList.clear();
			startList.clear();
			mySetRemovedEdges.clear();
			outputPanel.removeAll();
			outputPanel.invalidate();
			outputPanel.validate();
			outputPanel.repaint();
			myPanelMain.repaint();
			getContentPane().repaint();
			nif.getNetworkDisplay().refresh();
		}
		else if( src == myButtonPromptRemoveEdge ) promptUserRemoveEdge();
	}
}
