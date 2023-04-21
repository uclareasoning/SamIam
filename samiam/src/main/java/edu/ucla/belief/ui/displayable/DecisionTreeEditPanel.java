package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.*;
import edu.ucla.belief.decision.*;
import edu.ucla.belief.io.dsl.DSLNodeType;
import edu.ucla.util.Interruptable;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.JOptionResizeHelper;
import edu.ucla.belief.ui.util.DecimalField;
//import edu.ucla.belief.ui.util.Interruptable;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.statusbar.StackedMessage;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
//import java.util.Collection;
//import java.util.Iterator;

/** @author Keith Cascio
	@since 120804 */
public class DecisionTreeEditPanel extends JPanel implements TreeSelectionListener, DecisionListener
{
	public DecisionTreeEditPanel( DisplayableDecisionTree tree, Factory factory ){
		super( new GridBagLayout() );
		this.myDisplayableDecisionTree = tree;
		this.myFactory = factory;
		init();
	}

	/** @since 011005 */
	public void setEditable( boolean flag ){
		if( myFlagEditable != flag ){
			this.myFlagEditable = flag;
			if( myParameterTableModel != null ) myParameterTableModel.setEditable( flag );
			if( myDecisionTableModel != null ) myDecisionTableModel.setEditable( flag );
			if( myDisplayableDecisionTree != null ) myDisplayableDecisionTree.setEditable( flag );
		}
	}

	/** @since 011005 */
	public boolean isEditable(){
		return myFlagEditable;
	}

	/** @since 010905 */
	public static DecisionTree showCreationDialog( JComponent parent, CPTShell oldshell )
	{
		if( oldshell instanceof TableShell ){
			getCreationGUI();
			myOptimizer.setEpsilon( Optimizer.suggestEpsilon( oldshell.getCPT() ) );
		}

		int result = JOptionPane.showConfirmDialog(	parent,
			getCreationGUI(), "Create optimized decision tree?",
			JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );

		if( result == JOptionPane.YES_OPTION ){
			myOptimizer.setEpsilon( myTfEpsilon.getValue() );
			myOptimizer.setParameterOptimizationLevel( (Optimizer.ParameterOptimizationLevel) myComboLevel.getSelectedItem() );
			return myOptimizer.optimize( oldshell );
		}
		else if( result == JOptionPane.NO_OPTION ) return new DecisionTreeImpl( oldshell.index() );
		else return (DecisionTree)null;
	}

	/** @since 010905 */
	private static JComponent getCreationGUI(){
		if( myCreationGUI == null )
		{
			myOptimizer = new Optimizer( Double.MIN_VALUE );//*(double)5 );

			myCreationGUI = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;

			Icon icon = MainToolBar.getIcon( "decision_icon_49x67.gif" );

			c.gridwidth = 1;
			myCreationGUI.add( new JLabel( icon ), c );
			myCreationGUI.add( Box.createHorizontalStrut( 16 ), c );

			JPanel panelText = new JPanel( new GridBagLayout() );
			c.gridwidth = GridBagConstraints.REMAINDER;
			panelText.add( makeL( "To convert the existing CPT into an" ), c );
			panelText.add( makeL( "optimized decision tree, click Yes." ), c );
			panelText.add( makeL( "To start with a blank decision tree," ), c );
			panelText.add( makeL( "click No." ), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = c.weighty = 1;
			c.fill = GridBagConstraints.BOTH;
			myCreationGUI.add( panelText, c );

			c.weightx = c.weighty = 0;
			c.fill = GridBagConstraints.NONE;
			myCreationGUI.add( Box.createVerticalStrut( 8 ), c );

			JPanel pnlParameters = new JPanel( new GridBagLayout() );
			c.gridwidth = 1;
			pnlParameters.add( makeL( "Parameter epsilon:" ), c );
			pnlParameters.add( Box.createHorizontalStrut( 8 ), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			pnlParameters.add( myTfEpsilon = new DecimalField( myOptimizer.getEpsilon(), 20 ), c );
			c.gridwidth = 1;
			pnlParameters.add( makeL( "Optimization level:" ), c );
			pnlParameters.add( Box.createHorizontalStrut( 8 ), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			pnlParameters.add( myComboLevel = new JComboBox( myOptimizer.ARRAY_POLS ), c );
			//myComboLevel.setSelectedItem( myOptimizer.getParameterOptimizationLevel() );

			c.gridwidth = GridBagConstraints.REMAINDER;
			myCreationGUI.add( pnlParameters, c );
		}
		myTfEpsilon.setValue( myOptimizer.getEpsilon() );
		myComboLevel.setSelectedItem( myOptimizer.getParameterOptimizationLevel() );
		return myCreationGUI;
	}

	public static JLabel makeL( String text ){
		return new JLabel( text, JLabel.LEFT );
	}

	/** @since 010905 */
	public void setDividerLocation( double ratio ){
		//System.out.println( "DecisionTreeEditPanel.setDividerLocation( "+ratio+" )" );
		this.myJSplitPane.setDividerLocation( ratio );
	}

	/** @since 011005 */
	public void setDividerLocationLater( double ratio )
	{
		if( myDividerLocationRunner == null ) myDividerLocationRunner = new Interruptable(){
			public void runImpl( Object arg1 ){
				DecisionTreeEditPanel.this.setDividerLocation( ((Number)arg1).doubleValue() );
			}
		};
		myDividerLocationRunner.start( (long)100, new Double( ratio ) );
	}

	/** insterface TreeSelectionListener */
	public void valueChanged( TreeSelectionEvent e )
	{
		//System.out.println( "DecisionTreeEditPanel.valueChanged()" );
		TreePath path = e.getPath();
		DecisionNode dn = null;
		DisplayableDecisionTree.TreeNode treenode = null;
		if( (path != null) && e.isAddedPath() ){
			treenode = (DisplayableDecisionTree.TreeNode)path.getLastPathComponent();
			dn = treenode.getDecisionNode();
		}
		decisionNodeSelected( dn, treenode );
	}

	private void decisionNodeSelected( DecisionNode dn, DisplayableDecisionTree.TreeNode treenode )
	{
		//System.out.println( "DecisionTreeEditPanel.decisionNodeSelected( "+dn+" )" );
		doMessageWest( dn );
		doTableEast( dn, treenode );
	}

	/** interface DecisionListener */
	public void decisionEvent( DecisionEvent e ){
		if( e.type == DecisionEvent.RENAME )
			if( (myWestDecisionNode != null) && (e.node == myWestDecisionNode) )
				doMessageWest( myWestDecisionNode );
	}

	private void doMessageWest( DecisionNode dn )
	{
		myWestDecisionNode = dn;
		if( myLastPushed != null ) myStackedMessageWest.popText( myLastPushed );
		if( dn == null ){
			if( myStackedMessageWest.isEmpty() ) myPanelMessagesWest.setVisible( false );
		}
		else{
			String descrip = dn.isLeaf() ? "Leaf " : "";
			String message = "<html><nobr>" + descrip +
				"<font color=\"#6633FF\">" + Util.htmlEncode( dn.toString() ) + "</font> " +
				getQuantityMessage( dn.getParents().size() );

			myStackedMessageWest.pushText( myLastPushed = message );
			myPanelMessagesWest.setVisible( true );
		}
	}

	private void doTableEast( DecisionNode dn, DisplayableDecisionTree.TreeNode treenode )
	{
		if( dn == null ){
			myJTable.setModel( TABLEMODEL_EMPTY );
			myJScrollPane.setVisible( false );
		}
		else{
			GroupingColoringTableModel tablemodel = null;
			if( dn.isLeaf() ){
				if( myParameterTableModel == null ){
					myParameterTableModel = new ParameterTableModel( myJTable, myFactory );
					myParameterTableModel.addListener( (GroupingColoringTableModel.Listener) myDisplayableDecisionTree );
				}
				myParameterTableModel.setEditable( isEditable() );
				myParameterTableModel.setLeaf( (DecisionLeaf)dn );
				tablemodel = myParameterTableModel;
			}
			else{
				if( myDecisionTableModel == null ){
					myDecisionTableModel = new DecisionTableModel( myJTable, myDisplayableDecisionTree );
					myDecisionTableModel.addListener( (GroupingColoringTableModel.Listener) myDisplayableDecisionTree );
				}
				myDecisionTableModel.setEditable( isEditable() );
				myDecisionTableModel.setInternal( (DecisionInternal)dn );
				tablemodel = myDecisionTableModel;
			}
			myJTable.setGroupingColoringTableModel( tablemodel );
			myJScrollPane.setVisible( true );
		}
	}

	public static String getQuantityMessage( int numparents )
	{
		String ret = "occurs ";
		if( numparents < 2 ){
			ret += "once ";
			if( numparents == 0 ) return ret + "as the root of the tree.";
			else return ret + "in the tree.";
		}
		else return ret + numparents + " times in the tree.";
	}

	private void init()
	{
		myDisplayableDecisionTree.addTreeSelectionListener( (TreeSelectionListener)this );
		myDisplayableDecisionTree.addListener( (DecisionListener)this );

		myJSplitPane = new JSplitPane();

		myJSplitPane.setLeftComponent( createWestSide() );
		myJSplitPane.setRightComponent( myPanelEast = new JPanel( new BorderLayout() ) );

		myJTable = new GroupingColoringJTable();
		myJScrollPane = new JScrollPane( myJTable );
		myPanelEast.add( myJScrollPane, BorderLayout.CENTER );

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		this.add( myJSplitPane, c );

		myJSplitPane.setDividerLocation( (double)0.6 );

		this.setEditable( true );
	}

	private JComponent createWestSide()
	{
		JPanel ret = new JPanel( new BorderLayout() );

		ret.add( myDisplayableDecisionTree, BorderLayout.CENTER );

		(myPanelMessagesWest = new JPanel()).setVisible( false );
		myPanelMessagesWest.add( myLabelMessagesWest = new JLabel() );
		myStackedMessageWest = new StackedMessage( myLabelMessagesWest, -1 );

		ret.add( myPanelMessagesWest, BorderLayout.SOUTH );

		return ret;
	}

	private static final TableModel TABLEMODEL_EMPTY = new AbstractTableModel(){
		public int getRowCount() { return 1; }
		public int getColumnCount() { return 1; }
		public Object getValueAt(int row, int column) { return "empty"; }
	};

	/** test/debug */
	public static void main( String[] args )
	{
		String argVarID = "-id";
		String argFake = "-fake";
		String strExecName = "DecisionTreeEditPanel.main()";
		String strVarID = "D";
		String argClobber = "nosplash";
		boolean flagFake = false;

		for( int i=0; i < args.length; i++ )
		{
			if( args[i].startsWith( argVarID ) ){
				String potentialVarID = args[i].substring( argVarID.length() );
				if( potentialVarID.length() > 0 ){
					System.out.println( strExecName + " will use variable " + potentialVarID );
					strVarID = potentialVarID;
				}
				args[i] = argClobber;
			}
			else if( args[i].equals( argFake ) ){
				flagFake = true;
				args[i] = argClobber;
			}
		}

		UI ui = UI.mainImpl( args );

		int WAIT = 16;

		System.out.println( strExecName + " started, will wait "+WAIT+" secs for network to open." );
		int waitSeconds = 0;
		try{
			while( ui.getActiveHuginNetInternalFrame() == null ){
				if( waitSeconds++ > WAIT ) break;
				System.out.println( "Waited " + waitSeconds + " secs for network to open..." );
				Thread.sleep( 1000 );
			}
		}catch( InterruptedException interruptedexception ){
			System.err.println( "Warning: " + interruptedexception );
		}

		if( ui.getActiveHuginNetInternalFrame() == null ){
			String defaultPath = "C:\\keith\\code\\argroup\\networks\\cancer.net";
			System.out.println( "Attemping to open default: " + defaultPath );
			if( !ui.openFile( new java.io.File( defaultPath ) ) ){
				System.err.println( "Warning: could not open " + defaultPath );
				ui.exitProgram();
				return;
			}

			waitSeconds = 0;
			try{
				while( ui.getActiveHuginNetInternalFrame() == null ){
					if( waitSeconds++ > WAIT ) break;
					System.out.println( "Waited " + waitSeconds + " secs for "+defaultPath+" to open..." );
					Thread.sleep( 1000 );
				}
			}catch( InterruptedException interruptedexception ){
				System.err.println( "Warning: " + interruptedexception );
			}
		}

		NetworkInternalFrame nif = ui.getActiveHuginNetInternalFrame();
		nif.bestWindowArrangement();
		DisplayableBeliefNetwork dbn = nif.getBeliefNetwork();
		final DisplayableFiniteVariableImpl dfn = (DisplayableFiniteVariableImpl) dbn.forID( strVarID );

		if( flagFake ){
		//DisplayableDecisionTree ddt = new DisplayableDecisionTree( dfn.getCPTShell( DSLNodeType.CPT ).index() );
		DisplayableDecisionTree ddt = new DisplayableDecisionTree( (DecisionTreeImpl) new Optimizer( Double.MIN_VALUE ).optimize( dfn.getCPTShell( DSLNodeType.CPT ) ) );
		final DecisionTreeEditPanel dtep = new DecisionTreeEditPanel( ddt, ddt.getFactory() );
		JComponent pnlResize = dfn.createNodePropertiesComponentDebug( dtep, "Decision Tree" );

		JOptionResizeHelper helper = new JOptionResizeHelper( pnlResize, true, (long)10000 );
		helper.setListener( new JOptionResizeHelper.JOptionResizeHelperListener(){
			public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
				dtep.myJSplitPane.setDividerLocation( (double)0.6 );
			} } );
		helper.start();
		dfn.showDebug( ui );
		}
		else{
			dfn.showNodePropertiesDialog( ui, new JOptionResizeHelper.JOptionResizeHelperListener(){
				public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
					dfn.setRepresentationProperty( DSLNodeType.DECISIONTREE );
				}
			}
			);
		}

		ui.closeFile( nif );
		ui.exitProgram();
	}

	private JSplitPane myJSplitPane;
	private Interruptable myDividerLocationRunner;
	private JPanel myPanelMessagesWest;
	private JLabel myLabelMessagesWest;
	private StackedMessage myStackedMessageWest;
	private DecisionNode myWestDecisionNode;
	private String myLastPushed;
	private JPanel myPanelEast;
	private GroupingColoringJTable myJTable;
	private JScrollPane myJScrollPane;
	private ParameterTableModel myParameterTableModel;
	private DecisionTableModel myDecisionTableModel;
	private boolean myFlagEditable = false;

	private DisplayableDecisionTree myDisplayableDecisionTree;
	private Factory myFactory;

	private static JComponent myCreationGUI;
	private static DecimalField myTfEpsilon;
	private static JComboBox myComboLevel;
	private static Optimizer myOptimizer;
}
