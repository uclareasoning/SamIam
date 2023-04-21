package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.statusbar.StatusBar;
import edu.ucla.belief.ui.preference.SamiamPreferences;

import edu.ucla.belief.*;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.inference.map.*;
import edu.ucla.belief.inference.map.MapProperty;
import edu.ucla.util.Interruptable;
import edu.ucla.util.Stringifier;
import edu.ucla.util.AbstractStringifier;
import edu.ucla.util.code.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import il2.inf.map.MapSearch;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.util.*;
import java.net.*;

/**
	@author  Paul Medvedev
	@version
	Created on May 15, 2001, 6:18 PM
	Panel used for input on the left hand side of the MAPInternalFrame
*/
public class InputPanel extends javax.swing.JPanel implements java.awt.event.ActionListener, NodePropertyChangeListener
{
	// constructor:
	// toFind: pre-selected variable to be used in MAP search
	// known:  variables not used in MAP search
	// NOTE: the names of the variables are misleading, i will change them later
	public InputPanel( MAPInternalFrame p, NetworkInternalFrame fn, Collection known, Collection toFind )
	{
		this( p, fn );
		BeliefNetwork bn = this.getBeliefNetwork();
		int totalSize = bn.size() + 10;//buffer
		Comparator comp = VariableComparator.getInstance();
		SortedListModel model1 = new SortedListModel( known, comp, totalSize );
		SortedListModel model2 = new SortedListModel( toFind, comp, totalSize );
		initComponents( model1, model2, bn );
	}

	public InputPanel( MAPInternalFrame p, NetworkInternalFrame fn, SortedListModel known, SortedListModel toFind )
	{
		this( p, fn );
		initComponents( known, toFind, this.getBeliefNetwork() );
	}

	private InputPanel( MAPInternalFrame p, NetworkInternalFrame fn )
	{
		super( new GridBagLayout() );
		frmNet = fn;
		parent = p;
		if( frmNet != null ) frmNet.addNodePropertyChangeListener( this );
	}

	/** @since 021505 */
	public void setExact( boolean flag ){
		if( flag ){
			if( (myRadioExact != null) && (!myRadioExact.isSelected()) ) myRadioExact.doClick();
		}
		else{
			if( (myRadioApproximate != null) && (!myRadioApproximate.isSelected()) ) myRadioApproximate.doClick();
		}
	}

	// arrange the frame layout
	private void initComponents( SortedListModel known, SortedListModel toFind, Collection variables )
	{
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.weightx   = 1;
		add( makeParametersPanel(),                    gbc );

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.weightx   = gbc.weighty = 1;
		add( makeListsPanel(known, toFind, variables), gbc );

		gbc.anchor    = GridBagConstraints.CENTER;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.weightx   = gbc.weighty = 0;
		add( Box.createVerticalStrut( 2 ),             gbc );
		gbc.gridwidth = 1;
		add( Box.createHorizontalStrut( 2 ),           gbc );
		gbc.weightx   = 1;
		add( btnGo,                                    gbc );
		gbc.weightx   = 0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		add( Box.createHorizontalStrut( 2 ),           gbc );

		//myRadioApproximate.setSelected( true );
		myRadioApproximate.doClick();
	}

	/** @since 20080117 */
	private Component  reg( Component comp, Collection bucket ){
		bucket.add( comp );
		return comp;
	}

	/** @since 20080117 */
	private Component hstrut( int size, Collection bucket ){
		return reg( Box.createHorizontalStrut( size ), bucket );
	}

	/** @since 20080117 */
	private Component vstrut( int size, Collection bucket ){
		return reg( Box.createVerticalStrut( size ), bucket );
	}

	private JComponent makeParametersPanel()
	{
		Dimension dim;
		int width;

		Collection wa = myWidgetsApprox = new LinkedList();
		Collection we = myWidgetsExact  = new LinkedList();

		myLabelSearchMethod = new javax.swing.JLabel( "Search Method:" );
		cmbSearch = new JComboBox( SearchMethod.ARRAY );
		cmbSearch.setSelectedItem( SEARCH_OPTION_DEFAULT );
		dim = cmbSearch.getPreferredSize();
		width = dim.width;
		//System.out.println( "cmbSearch size " + dim );

		myLabelInitMethod = new javax.swing.JLabel( "Initialization Method:" );
		cmbInit = new JComboBox( InitializationMethod.ARRAY );
		cmbInit.setSelectedItem( INIT_OPTION_DEFAULT );
		dim = cmbInit.getPreferredSize();
		width = Math.max( width, dim.width );
		//System.out.println( "cmbInit size " + dim );

		dim.width = width + 16;
		cmbSearch.setMinimumSize( dim );
		cmbInit.setMinimumSize( dim );
		//System.out.println( "minimum size " + dim );

		myLabelMaxSteps = new javax.swing.JLabel( "Maximum Search Steps:" );
		txtSteps = new WholeNumberField( INT_DEFAULT_STEPS, 5, INT_STEPS_FLOOR, INT_STEPS_CEILING );
		dim = new Dimension (60, 20);
		txtSteps.setMinimumSize( dim );
		txtSteps.setPreferredSize( dim );

		btnGo = new javax.swing.JButton( "Update" );
		btnGo.setMargin( new Insets( 2,0x40,2,0x40 ) );
		btnGo.setFont( btnGo.getFont().deriveFont( Font.BOLD ) );
		btnGo.addActionListener(this);

		myRadioExact = new JRadioButton( "Exact" );
		myRadioApproximate = new JRadioButton( "Approximate" );
		ButtonGroup group = new ButtonGroup();
		group.add( myRadioExact );
		group.add( myRadioApproximate );
		myRadioExact.addActionListener(this);
		myRadioApproximate.addActionListener(this);

		myLabelTimeout = new JLabel( "Time out (secs):" );
		myTfTimeout = new WholeNumberField( INT_DEFAULT_TIMEOUT, 5, INT_TIMEOUT_FLOOR, INT_TIMEOUT_CEILING );
		dim = new Dimension (60, 20);
		myTfTimeout.setMinimumSize( dim );
		myTfTimeout.setPreferredSize( dim );

		myCBSlop = new JCheckBox( "Sloppy?", false );
		myCBSlop.addActionListener(this);
		myButtonAdjust = MPEPanel.makeButton( "auto" );
		myButtonAdjust.setToolTipText( "multiply slop value by P(MAP,e)" );
		myButtonAdjust.addActionListener( this );
		JPanel pnlSlop = new JPanel( new GridBagLayout() );
		GridBagConstraints cSlop = new GridBagConstraints();
		pnlSlop.add( myCBSlop,                     cSlop );
		pnlSlop.add( Box.createHorizontalStrut(4), cSlop );
		pnlSlop.add( myButtonAdjust,               cSlop );
		pnlSlop.add( Box.createHorizontalStrut(4), cSlop );
		myLabelSlop = new JLabel( "Slop: ", JLabel.RIGHT );
		myTfSlop = new DecimalField( (double)0.5, 5, DOUBLE_SLOP_FLOOR, DOUBLE_SLOP_CEILING );

		myLabelWidthBarrier = new JLabel( "Width barrier (0=none):" );
		myTfWidthBarrier = new WholeNumberField( INT_WIDTH_BARRIER_DEFAULT, 5, INT_WIDTH_BARRIER_FLOOR, INT_WIDTH_BARRIER_CEILING );

		JPanel             ret = new JPanel( new GridBagLayout() );
		GridBagConstraints gbc = new GridBagConstraints();
		//gbc.insets = (new Insets (2,0,2,0));

		gbc.anchor    = GridBagConstraints.WEST;
		gbc.gridwidth = 1;
		ret.add( myRadioApproximate,             gbc );
		ret.add( Box.createHorizontalStrut( 4 ), gbc );
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myRadioExact,                   gbc );
		ret.add( vstrut( 2,                wa ), gbc );

		gbc.gridwidth = 1;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.weightx   = 0;
		ret.add( reg( myLabelSearchMethod, wa ), gbc );
		ret.add( hstrut( 8,                wa ), gbc );
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.weightx   = 1;
		ret.add( reg( cmbSearch,           wa ), gbc );

		gbc.gridwidth = 1;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.weightx   = 0;
		ret.add( reg( myLabelInitMethod,   wa ), gbc );
		ret.add( hstrut( 8,                wa ), gbc );
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.weightx   = 1;
		ret.add( reg( cmbInit,             wa ), gbc );

		gbc.gridwidth = 1;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.weightx   = 0;
		ret.add( reg( myLabelMaxSteps,     wa ), gbc );
		ret.add( hstrut( 8,                wa ), gbc );
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.weightx   = 1;
		ret.add( reg( txtSteps,            wa ), gbc );

	  //ret.add( vstrut( 2,                wa ), gbc );

		gbc.gridwidth = 1;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.weightx   = 0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
	  //ret.add( myRadioExact,                   gbc );
	  //ret.add( vstrut( 2,                we ), gbc );

		gbc.anchor    = GridBagConstraints.WEST;
		gbc.gridwidth = 1;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.weightx   = 0;
		ret.add( reg( myLabelTimeout,      we ), gbc );
		ret.add( hstrut( 8,                we ), gbc );
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.weightx   = 1;
		ret.add( reg( myTfTimeout,         we ), gbc );

		gbc.anchor    = GridBagConstraints.WEST;
		gbc.gridwidth = 1;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.weightx   = 0;
		ret.add( reg( pnlSlop,             we ), gbc );
		ret.add( reg( myLabelSlop,         we ), gbc );
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.weightx   = 1;
		ret.add( reg( myTfSlop,            we ), gbc );

		gbc.anchor    = GridBagConstraints.WEST;
		gbc.gridwidth = 1;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.weightx   = 0;
		ret.add( reg( myLabelWidthBarrier, we ), gbc );
		ret.add( hstrut( 8,                we ), gbc );
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.weightx   = 1;
		ret.add( reg( myTfWidthBarrier,    we ), gbc );

		ret.setBorder( BorderFactory.createEmptyBorder( 0,2,4,2 ) );

		return ret;
	}

	/** @since 20040713 */
	public void setMAPVariables( Collection toFind )
	{
		synchronized( mySynchRefresh ){
		if( FLAG_ALLOW_LIST_EDIT ) throw new IllegalStateException( "setMAPVariables() only works if FLAG_ALLOW_LIST_EDIT == false." );
		BeliefNetwork bn          = this.getBeliefNetwork();
		lstModel2                 = new SortedListModel( toFind, VariableComparator.getInstance(), bn.size() + 10 );
		if( Thread.currentThread().isInterrupted() ) return;
		jList2                    = new JList(       lstModel2 );
		pan2                      = new JScrollPane( jList2    );
		if( Thread.currentThread().isInterrupted() ) return;
		ListCellRenderer renderer = new VariableLabelRenderer( jList2.getCellRenderer(), bn );
		jList2.setCellRenderer( renderer );
		myPanelToFind.removeAll();
		if( Thread.currentThread().isInterrupted() ) return;
		myPanelToFind.add( pan2 );
		myPanelToFind.revalidate();
		myPanelToFind.repaint();
		recount();
		myFlagStale = false;
		}
	}

	/** @since 20070310 */
	private void recount(){
		try{
			myLabelCount.setText( Integer.toString( lstModel2.getSize() ) );
		}catch( Exception exception ){
			System.err.println( "warning: InputPanel.recount() caught " + exception );
		}
	}

	private JComponent makeListsPanel( SortedListModel known, SortedListModel toFind, Collection variables )
	{
		//Initialize list boxes and their scroll panes
		lstModel2 = toFind;
		jList2 = new javax.swing.JList(lstModel2);
		pan2 = new JScrollPane (jList2);
		ListCellRenderer renderer = new VariableLabelRenderer( jList2.getCellRenderer(), variables );
		jList2.setCellRenderer( renderer );
		myPanelToFind = new JPanel( new GridLayout() );
		myPanelToFind.add( pan2 );

		JPanel ret = new JPanel( new java.awt.GridBagLayout() );
		java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();

		if( FLAG_ALLOW_LIST_EDIT )
		{
			lstModel1 = known;
			jList1 = new javax.swing.JList(lstModel1);
			pan1 = new JScrollPane (jList1);
			jList1.setCellRenderer( renderer );

			btnOneToMAP = makeButton( "images/right.gif", "->", "Add to MAP" );
			btnOneToAvailable = makeButton( "images/left.gif", "<-", "Remove" );
			btnAllToMAP = makeButton( ">>>", "Add all" );
			btnAllToAvailable = makeButton( "<<<", "Remove all" );

			Component strut;

			gbc.gridx = 0; gbc.gridy = 1;
			gbc.weightx = 0; gbc.weighty = 0;
			gbc.gridwidth = 1;
			ret.add (new JLabel("Available Variables"), gbc);
			gbc.gridx = 1; gbc.gridy = 1;
			ret.add( myLabelCount = new JLabel( "0", JLabel.LEFT ), gbc );
			gbc.gridx = 2;
			ret.add( new JLabel( " MAP Variables",   JLabel.LEFT ), gbc );

			gbc.fill = GridBagConstraints.HORIZONTAL;
			strut = Box.createVerticalStrut( 16 );
			gbc.gridx = 1; gbc.gridy = 2;
			ret.add( strut, gbc );

			gbc.gridx = 1; gbc.gridy = 3;
			gbc.anchor = GridBagConstraints.SOUTH;
			ret.add(btnOneToMAP, gbc);

			gbc.gridx = 1; gbc.gridy = 4;
			gbc.anchor = GridBagConstraints.NORTH;
			ret.add(btnOneToAvailable, gbc);

			strut = Box.createVerticalStrut( 32 );
			gbc.gridx = 1; gbc.gridy = 5;
			ret.add( strut, gbc );

			gbc.gridx = 1; gbc.gridy = 6;
			gbc.anchor = GridBagConstraints.SOUTH;
			ret.add(btnAllToMAP, gbc);

			gbc.gridx = 1; gbc.gridy = 7;
			gbc.anchor = GridBagConstraints.NORTH;
			ret.add(btnAllToAvailable, gbc);

			//pan1
			pan1.setPreferredSize(new java.awt.Dimension(30, 30));
			gbc.gridx = 0; gbc.gridy = 2;
			gbc.weightx = 1; gbc.weighty = 1;
			gbc.gridheight = GridBagConstraints.REMAINDER;
			gbc.fill = java.awt.GridBagConstraints.BOTH;
			ret.add(pan1, gbc);

			//pan2
			pan2.setPreferredSize(new java.awt.Dimension(30, 30));
			gbc.gridx = 2; gbc.gridy = 2;
			gbc.weightx = 1; gbc.weighty = 1;
			ret.add(pan2, gbc);
		}
		else
		{
			//jList2.

			gbc.anchor     = GridBagConstraints.NORTHWEST;
			ret.add( Box.createHorizontalStrut(4), gbc );

			gbc.gridwidth  = 1;
			gbc.weightx    = 0;
			ret.add( myLabelCount = new JLabel( "0", JLabel.LEFT ),             gbc );
			gbc.weightx    = 10;
			ret.add( new JLabel( " MAP Variables (edit in the ", JLabel.LEFT ), gbc );
			//ret.add( label = new JLabel( "<html><nobr><a href=\"http://reasoning.cs.ucla.edu/samiam\">Variable Selection Tool</a></html>", JLabel.LEFT ), gbc );
			//ret.add( new HyperLabel( "Variable&nbsp;Selection&nbsp;Tool", frmNet.getParentFrame().action_VARIABLESELECTION, MapProperty.PROPERTY ), gbc );
			String caption = "Variable Selection Tool";
			JLabel label   = (frmNet == null) ? new JLabel( caption ) : new HyperLabel( caption, frmNet.getParentFrame().action_VARIABLESELECTION, MapProperty.PROPERTY );
			gbc.weightx    = 0;
			ret.add( label, gbc );
			gbc.gridwidth  = GridBagConstraints.REMAINDER;
			gbc.weightx    = 1;
			ret.add( new JLabel( ")", JLabel.LEFT ), gbc );

			gbc.anchor     = GridBagConstraints.SOUTH;
			gbc.weightx    = 1; gbc.weighty = 1;
			gbc.fill       = java.awt.GridBagConstraints.BOTH;
			ret.add( myPanelToFind, gbc );
		}

		recount();
		return ret;
	}

	/** interface NodePropertyChangeListener
		@since 20040713 */
	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		//System.out.println( e );
		if( e.property == MapProperty.PROPERTY ){
			if( ! myFlagStale ){
				myFlagStale = true;
				if( myLabelCount != null ) myLabelCount.setText( "-" );
				if( lstModel2    != null ) lstModel2.clear();
			}
			refresh( false );
		}
	}

	/** @since 20070310 */
	public class RunRefresh extends Interruptable{
		public void runImpl( Object arg1 ) throws InterruptedException {
			if( arg1 != Boolean.TRUE ){
				if( ! InputPanel.this.myFlagStale ) return;
				Thread.sleep( 0x200 );
				if( ! InputPanel.this.myFlagStale ) return;
			}

			BeliefNetwork bn        = InputPanel.this.getBeliefNetwork();
			Set           set       = new HashSet( bn );
			if( Thread.currentThread().isInterrupted() ) return;
			set.removeAll( bn.getEvidenceController().evidence().keySet() );
			StandardNode  dVar      = null;
			for( Iterator itAllVars = set.iterator(); itAllVars.hasNext(); ){
				if( ! ((StandardNode) itAllVars.next()).isMAPVariable() ) itAllVars.remove();
			}
			if( Thread.currentThread().isInterrupted() ) return;
			InputPanel.this.setMAPVariables( set );
		}
	}
	private RunRefresh myRunRefresh;
	private Object     mySynchRefresh = "synchronize refresh";

	/** @since 20070310 */
	public void refresh(){
		this.refresh( true );
	}

	/** @since 20070310 */
	private void refresh( boolean immediately ){
		synchronized( mySynchRefresh ){
			if( myRunRefresh == null ) myRunRefresh = new RunRefresh();
			myRunRefresh.start( immediately ? Boolean.TRUE : Boolean.FALSE );
		}
	}

	/** @since 20040713 */
	public boolean isStale(){
		return myFlagStale;
	}

	private JButton makeButton( String path, String alt, String tooltip )
	{
		ImageIcon gif = null;
		//URL iconURL = ClassLoader.getSystemResource( path );
		URL iconURL = edu.ucla.belief.ui.toolbar.MainToolBar.findImageURL( path );

		JButton ret = null;
		if( iconURL != null)
		{
			gif = new ImageIcon( iconURL, tooltip );
			ret = new JButton( gif );
		}
		else ret = new JButton( alt );

		ret.setToolTipText( tooltip );
		ret.addActionListener( this );

		return ret;
	}

	private JButton makeButton( String text, String tooltip )
	{
		JButton ret = new JButton( text );
		ret.setToolTipText( tooltip );
		ret.addActionListener( this );
		return ret;
	}

	/** @since 20040510 */
	public void codeBandit()
	{
		CodeToolInternalFrame ctif;
		try{
			if( frmNet == null ) return;
			ctif = frmNet.getCodeToolInternalFrame();
			if( ctif != null ){
				MAPCoder mc = setupMAPCoder();
				if( mc != null ){
					ctif.setCodeGenius( mc );
					ctif.setVisible( true );
				}
			}
		}catch( Exception exception ){
			String msg = exception.getMessage();
			if( msg == null ) msg = exception.toString();
			frmNet.getParentFrame().showErrorDialog( msg );
		}
	}

	/** @since 20040510 */
	public MAPCoder setupMAPCoder()
	{
		String pathInput = frmNet.getFileName();
		if( frmNet.getCodeToolInternalFrame().failOnMissingNetworkFile( pathInput ) ) return (MAPCoder) null;

		Map evidence = new HashMap( frmNet.getBeliefNetwork().getEvidenceController().evidence() );
		Set mapvars  = buildSetMAPVariables( evidence );

		if( (mapvars == null) || mapvars.isEmpty() ){
			JOptionPane.showMessageDialog( this, "Please select at least one MAP variable before running "+CodeToolInternalFrame.STR_DISPLAY_NAME+".", "Select Variable(s)", JOptionPane.WARNING_MESSAGE );
			return (MAPCoder) null;
		}

		MAPCoder mapcoder = new MAPCoder();
		mapcoder.setPathInputFile( pathInput );
		mapcoder.setEvidence(      evidence  );
		mapcoder.setVariables(     mapvars   );

		int steps          = txtSteps.getValue();
		int timeoutsecs    = myTfTimeout.getValue();
		boolean flagSloppy = myCBSlop.isSelected();
		double slop        = myTfSlop.getValue();

		mapcoder.setExactOrNot(    myRadioExact.isSelected() );
		mapcoder.setApproximationParameters( (SearchMethod) cmbSearch.getSelectedItem(), (InitializationMethod) cmbInit.getSelectedItem(), steps );
		mapcoder.setExactParameters( timeoutsecs, (int)0, flagSloppy, slop );

		return mapcoder;
	}

	/**
		Build a set of MAP variables from the selection in list box,
		excluding any variables that are set as evidence.
		@since 051004
	*/
	public Set buildSetMAPVariables( Map evidence )
	{
		//System.out.print( "InputPanel.buildSetMAPVariables() -> " );
		Set vars = new HashSet();

		Variable dVar;
		for (int i = 0; i < lstModel2.getSize(); i++)
		{
			dVar = (Variable) lstModel2.getElementAt(i);
			if( !evidence.containsKey(dVar) ) vars.add( dVar );
		}
		//System.out.println( vars );
		return vars;
	}

	/** @since 20050812 */
	public void cancelComputation(){
		myRunDoMAP.cancel();
	}

	/** @since 070705 */
	public Interruptable myRunDoMAP = new Interruptable(){
		public void runImpl( Object arg1 ) throws InterruptedException {
			Thread.sleep(4);
			Thread.currentThread().setPriority( INT_PRIORITY_COMPUTATION );
			//try{
				InputPanel.this.doMAP();
			//}catch( IllegalStateException ise ){
			//	JOptionPane.showMessageDialog( InputPanel.this, "MAP tool requires the shenoy-shafer algorithm.", "Cannot execute MAP", JOptionPane.ERROR_MESSAGE );
			//}
		}
	};

	public static final int INT_PRIORITY_COMPUTATION = ((Thread.NORM_PRIORITY - Thread.MIN_PRIORITY)/(int)2) + Thread.MIN_PRIORITY;

	/** called when update button is pressed */
	public void doMAP() throws IllegalStateException
	{
		boolean flagUseExact = myRadioExact.isSelected();

		BeliefNetwork   bn = this.getBeliefNetwork();
		InferenceEngine ie = (frmNet == null) ? null : frmNet.getInferenceEngine();
		JoinTreeInferenceEngineImpl jtie = null;
		double prE = DOUBLE_INVALID;

		if( (!flagUseExact) && (!FLAG_PRUNE_APPROXIMATE) )
		{
			if( ie.canonical() instanceof JoinTreeInferenceEngineImpl ) jtie = (JoinTreeInferenceEngineImpl) ie.canonical();
			else throw new IllegalStateException( "InferenceEngine must be instance of JoinTreeInferenceEngineImpl." );

			edu.ucla.belief.ui.util.Util.pushStatusWest( frmNet, UI.STR_MSG_PRE );
			prE = ie.probability();
			edu.ucla.belief.ui.util.Util.popStatusWest( frmNet, UI.STR_MSG_PRE );

			if( (prE <= (double)0) || Double.isNaN(prE) ){
				showError( "Impossible evidence." );
				return;
			}
		}

		//do computation
		try
		{
			if( btnGo != null ) btnGo.setEnabled( false );

			Map evidenceUnpruned = new HashMap( bn.getEvidenceController().evidence() );
			Set varsUnpruned = buildSetMAPVariables( evidenceUnpruned );

			if( varsUnpruned.isEmpty() ){
				showResult( prE, new HashMap(), (MapSearch.MapInfo)null, prE, bn, flagUseExact, true );//if no MAP variables, no need to call MapRunner
			}else{
				parent.setCursor( UI.CURSOR_WAIT );

				if( flagUseExact ) doExact( bn, evidenceUnpruned, varsUnpruned );
				else doApproximate( evidenceUnpruned, varsUnpruned, jtie, prE );
			}

			setSlopEnabled();
		}
		catch( Exception exc )
		{
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				exc.printStackTrace();
			}

			String msg = "Cannot do computation.  Please check parameters.";
			if( exc.getMessage() != null ) msg = exc.getMessage();

			JOptionPane.showMessageDialog( this, msg );
		}
		catch( OutOfMemoryError err )
		{
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				err.printStackTrace();
			}
			JOptionPane.showMessageDialog( this, "MAP Tool ran out of memory.", "Out of memory", JOptionPane.ERROR_MESSAGE );
		}
		finally{
			if( btnGo != null ) btnGo.setEnabled( true );
			edu.ucla.belief.ui.util.Util.popStatusWest( frmNet, MAPInternalFrame.STR_MSG_MAP );
			parent.setCursor( UI.CURSOR_DEFAULT );
		}

		return;
	}

	/** @since 072604 */
	private void doApproximate( Map evidenceUnpruned, Set varsUnpruned, JoinTreeInferenceEngineImpl jtie, double prE ) throws IllegalStateException
	{
		edu.ucla.belief.ui.util.Util.pushStatusWest( frmNet, MAPInternalFrame.STR_MSG_MAP );

		MapRunner.MapResult mapresult;
		Map result = null;
		double score = prE;

		BeliefNetwork dbn = this.getBeliefNetwork();

		BeliefNetwork bn = null;
		Map evidence = null;
		Set queryVars = null;

		Map oldToNew = null;
		Map newToOld = null;
		Set queryVarsPruned = null;
		Map evidencePruned = null;
		if( FLAG_PRUNE_APPROXIMATE )
		{
			edu.ucla.belief.ui.util.Util.pushStatusWest( frmNet, STR_MSG_PRUNING );
			BeliefNetwork unpruned = this.getBeliefNetwork();
			oldToNew = new HashMap( unpruned.size() );
			newToOld = new HashMap( unpruned.size() );
			queryVarsPruned = new HashSet( varsUnpruned.size() );
			evidencePruned = new HashMap( evidenceUnpruned.size() );
			bn = Prune.prune( unpruned, varsUnpruned, evidenceUnpruned, oldToNew, newToOld, queryVarsPruned, evidencePruned );
			evidence = evidencePruned;
			queryVars = queryVarsPruned;
			jtie = (JoinTreeInferenceEngineImpl) new JEngineGenerator().manufactureInferenceEngine( bn );
			try{
				bn.getEvidenceController().setObservations( evidencePruned );
			}catch( StateNotFoundException e ){
				showError( "Failed to set pruned evidence." );
				return;
			}
			edu.ucla.belief.ui.util.Util.popStatusWest( frmNet, STR_MSG_PRUNING );

			edu.ucla.belief.ui.util.Util.pushStatusWest( frmNet, UI.STR_MSG_PRE );
			prE = jtie.probability();
			edu.ucla.belief.ui.util.Util.popStatusWest( frmNet, UI.STR_MSG_PRE );
		}
		else
		{
			bn = this.getBeliefNetwork();
			evidence = evidenceUnpruned;
			queryVars = varsUnpruned;
		}

		int steps = txtSteps.getValue();

		MapRunner MR = new MapRunner();
		mapresult = MR.approximateMap(
			bn,
			jtie, queryVars, evidence,
			(SearchMethod) cmbSearch.getSelectedItem(),
			(InitializationMethod) cmbInit.getSelectedItem(),
			steps );
		result = mapresult.instantiation;
		score = mapresult.score;
		//score = MR.getLastScore();
		if( bn != dbn ) result = reTarget( result, dbn );
		showResult( prE, result, (MapSearch.MapInfo)null, score, dbn, false, false );
	}

	/** @since 20070402 */
	public static Map reTarget( Map result, BeliefNetwork target ){
		Map ret = new HashMap( result.size() );
		try{
			FiniteVariable cloneVar = null, targetVar = null;
			Object         cloneVal = null, targetVal = null;
			for( Iterator it = result.keySet().iterator(); it.hasNext(); ){
				cloneVar  = (FiniteVariable) it.next();
				cloneVal  = result.get( cloneVar );
				targetVar = (FiniteVariable) target.forID( cloneVar.getID() );
				targetVal = targetVar.instance( cloneVal.toString() );
				ret.put( targetVar, targetVal );
			}
		}catch( Exception exception ){
			System.err.println( "warning: InputPanel.reTarget() caught " + exception );
		}
		return ret;
	}

	/** @since 072604 */
	private void doExact( BeliefNetwork bn, Map evidenceUnpruned, Set varsUnpruned )
	{
		edu.ucla.belief.ui.util.Util.pushStatusWest( frmNet, MAPInternalFrame.STR_MSG_MAP );

		Map      result = null;
		double    score = DOUBLE_INVALID;
		boolean isExact = false;
		double      prE = DOUBLE_INVALID;

		MapSearch.MapInfo sloppyResults = null;

		int timeout = myTfTimeout.getValue();
		double slop = myTfSlop.getValue();
		int widthbarrier = myTfWidthBarrier.getValue();
		if( myCBSlop.isSelected() ){
			sloppyResults = ExactMap.computeMapSloppy(
					this.getBeliefNetwork(),
					varsUnpruned,
					evidenceUnpruned,
					timeout,
					widthbarrier,
					slop );
			prE = sloppyResults.probOfEvidence;
		}
		else{
			MapSearch.MapInfo res=ExactMap.computeMap(
					this.getBeliefNetwork(),
					varsUnpruned,
					evidenceUnpruned,
					timeout,
					widthbarrier);
			MapSearch.MapResult mapresult = (MapSearch.MapResult) res.results.iterator().next();
			result = mapresult.getConvertedInstatiation();
			score = mapresult.score;
			isExact = res.finished;
			prE = res.probOfEvidence;
		}

		//System.out.println( "ExactMap Pr(e) = " + prE );

		showResult( prE, result, sloppyResults, score, bn, true, isExact );
	}

	/** @since 072604 */
	private void showResult( double prE, Map result, MapSearch.MapInfo sloppyResults, double score, BeliefNetwork bn, boolean flagUseExact, boolean isExact )
	{
		//Display results
		int l = parent.panMain.getDividerLocation();

		//parent.panMain.add( new OutputPanel( result ), JSplitPane.RIGHT, 0 );
		MPEPanel mpepanel = null;
		if( result != null ){
			mpepanel = new MPEPanel( result, score, "P(MAP,e)=", bn );
			mpepanel.addResult( score / prE, "P(MAP|e)=" );
			if( flagUseExact ){
				String message = ( isExact ) ? "Result is exact." : "Result is not exact.";
				mpepanel.addMessage( message );
			}
		}
		else if( sloppyResults != null ){
			mpepanel = new MPEPanel( sloppyResults, prE, bn, frmNet );
		}
		if( frmNet != null ) mpepanel.setClipBoard( frmNet.getParentFrame().getInstantiationClipBoard(), bn.getEvidenceController() );
		parent.panMain.add( mpepanel, JSplitPane.RIGHT, 0 );
		parent.panMain.setDividerLocation (l);

		myMPEPanel = mpepanel;
	}

	/** @since 20070310 */
	private Interruptable myRunAdjustSlop = new Interruptable(){
		public void runImpl( Object arg1 ){
			try{
				if( Thread.currentThread().isInterrupted() ) return;
				if( myMPEPanel == null ) return;
				double current  = myTfSlop.getValue();
				double prmpe    = myMPEPanel.getScore();
				double adjusted = (current > 0) ? (current * prmpe) : prmpe;
				myTfSlop.setValue( adjusted );
				if( Thread.currentThread().isInterrupted() ) return;
				myTfSlop.setScrollOffset(0);
			}catch( Exception exception ){
				System.err.println( "warning: InputPanel.myRunAdjustSlop.runImpl() caught " + exception );
			}
		}
	};

	/** @since 20070310 */
	public void adjustSlop(){
		myRunAdjustSlop.start();
	}

	/** @since 072604 */
	private void showError( String msg )
	{
		JPanel pnlMain = new JPanel( new GridBagLayout() );
		JLabel lblError = SensitivityInternalFrame.getErrorMessageLabel( msg, (float)11 );
		GridBagConstraints c = new GridBagConstraints();
		pnlMain.add( lblError, c );

		//Display results
		int location = parent.panMain.getDividerLocation();
		parent.panMain.add( pnlMain, JSplitPane.RIGHT, 0 );
		parent.panMain.setDividerLocation( location );
	}

	/** @since 062104 */
	public static String getMessageFinished( MapSearch.MapInfo result ){
		return result.finished ? "Result is exact." : "Result is not exact.";
	}

	//Called when one of the three buttons is clicked (left, right, update)
	public void actionPerformed (ActionEvent e)
	{
		JList l1 = null, l2 = null;
		SortedListModel m1 = null, m2 = null;
		int i;
		Object src = e.getSource();

		if( src == btnAllToMAP ) jList1.setSelectionInterval((int)0, jList1.getModel().getSize()-1 );
		else if( src == btnAllToAvailable ) jList2.setSelectionInterval((int)0, jList2.getModel().getSize()-1 );

		if( src == btnOneToMAP || src == btnAllToMAP )
		{
			l1 = jList1;  l2 = jList2; m1 = lstModel1; m2 = lstModel2;
		}
		else if(src == btnOneToAvailable || src == btnAllToAvailable)
		{
			l1 = jList2;  l2 = jList1;  m1 = lstModel2; m2 = lstModel1;
		}
		else if( src == btnGo ){
			myRunDoMAP.start();
			return;
		}
		else if( src == myRadioExact || src == myRadioApproximate )
		{
			setParametersEnabled();
			return;
		}
		else if( src == myCBSlop ){
			setSlopEnabled();
			return;
		}
		else if( src == myButtonAdjust ){
			adjustSlop();
			return;
		}
		else return;

		//move all selected variables
		//if we are at this point in the fnct, we must be processing a list move operation
		Object [] tmp = l1.getSelectedValues();
		for (i = 0; i < tmp.length; i++)
		{
			m2.addElement (tmp[i]);
			m1.removeElement (tmp[i]);
		}

		Object[] selectedValues = l2.getSelectedValues();
		jList1.clearSelection();
		jList2.clearSelection();

		for( i=0; i<selectedValues.length; i++ ) l2.setSelectedValue( selectedValues[i], false );
	}

	public SortedListModel getKnown()
	{
		return lstModel1;
	}

	public SortedListModel getToFind()
	{
		return lstModel2;
	}

	/** @since 092404 */
	public void selectExactSloppy(){
		if( !myRadioExact.isSelected() ) myRadioExact.doClick();
		if( !myCBSlop.isSelected() ) myCBSlop.doClick();
	}

	/** @since 092404 */
	public void update(){
		btnGo.doClick();
	}

	/** @since 20030604 */
	protected void setParametersEnabled()
	{
		setExactParametersEnabled(               myRadioExact.isSelected() );
		setApproximationParametersEnabled( myRadioApproximate.isSelected() );
		setSlopEnabled();
	}

	/** @since 20080117 */
	protected InputPanel setVisible( Collection components, boolean visible ) throws Exception{
		for( Iterator it = components.iterator(); it.hasNext(); ){
			((Component)it.next()).setVisible( visible );
		}
		return this;
	}

	/** @since 20030604 */
	protected void setApproximationParametersEnabled( boolean flag )
	{
		try{
			myLabelSearchMethod .setEnabled( flag );
			cmbSearch           .setEnabled( flag );
			myLabelInitMethod   .setEnabled( flag );
			cmbInit             .setEnabled( flag );
			myLabelMaxSteps     .setEnabled( flag );
			txtSteps            .setEnabled( flag );
		}catch( Throwable thrown ){
			System.err.println( "warning: InputPanel.setApproximationParametersEnabled() caught " + thrown );
		}

		try{
			setVisible( myWidgetsApprox, flag );
		}catch( Throwable thrown ){
			System.err.println( "warning: InputPanel.setApproximationParametersEnabled() caught " + thrown );
		}
	}

	/** @since 20030604 */
	protected void         setExactParametersEnabled( boolean flag )
	{
		try{
			myLabelTimeout      .setEnabled( flag );
			myTfTimeout         .setEnabled( flag );
			myCBSlop            .setEnabled( flag );
			myLabelSlop         .setEnabled( flag );
			myTfSlop            .setEnabled( flag );
			myLabelWidthBarrier .setEnabled( flag );
			myTfWidthBarrier    .setEnabled( flag );
		}catch( Throwable thrown ){
			System.err.println( "warning: InputPanel.setExactParametersEnabled() caught " + thrown );
		}

		try{
			setVisible( myWidgetsExact, flag );
		}catch( Throwable thrown ){
			System.err.println( "warning: InputPanel.setExactParametersEnabled() caught " + thrown );
		}
	}

	/** @since 20040621 */
	protected void setSlopEnabled(){
		try{
			boolean enabled = myRadioExact.isSelected() && myCBSlop.isSelected();
			myTfSlop.setEnabled( enabled );
			myButtonAdjust.setEnabled( enabled && (myMPEPanel != null) );
		}catch( Exception exception ){
			System.err.println( "warning: InputPanel.setSlopEnabled() caught " + exception );
		}
	}

	/** @since 20070310 */
	public BeliefNetwork getBeliefNetwork(){
		if(      this.frmNet != null ) return this.frmNet.getBeliefNetwork();
		else if( this.parent != null ) return this.parent.getBeliefNetwork();
		else return null;
	}

	/** @since 20070311 */
	public Stringifier getPreferredStringifier(){
		Stringifier ret  = AbstractStringifier.VARIABLE_ID;
		try{
			SamiamPreferences prefs = null;
			if( frmNet != null ) prefs = frmNet.getPackageOptions();
			else if( this.getBeliefNetwork() != null ){
				Object first = this.getBeliefNetwork().iterator().next();
				if( first instanceof DisplayableFiniteVariable ) prefs = ((DisplayableFiniteVariable)first).getNetworkInternalFrame().getPackageOptions();
			}

			if( prefs == null ) return ret;

			boolean flagLabels   = ((Boolean) prefs.getMappedPreference( SamiamPreferences.displayNodeLabelIfAvail ).getValue()).booleanValue();
			if( flagLabels ) ret = AbstractStringifier.VARIABLE_LABEL;
		}catch( Exception exception ){
			System.err.println( "warning: EPEP.getPreferredStringifier() caught " + exception );
		}
		return ret;
	}

	/** @since 20070311 */
	public StringBuffer append( StringBuffer buff ) throws Exception{
		appendSettings( buff );
		if( myMPEPanel != null ) myMPEPanel.append( buff.append( '\n' ) );
		return buff;
	}

	/** @since 20070311 */
	public StringBuffer appendSettings( StringBuffer buff ) throws Exception{
		buff.append( "maximum a posteriori" );
		if( myRadioExact.isSelected()       ) appendSettingsExact(       buff );
		if( myRadioApproximate.isSelected() ) appendSettingsApproximate( buff );

		Stringifier ifier = getPreferredStringifier();
		//buff.append( '\n' );
		buff.append( myLabelCount.getText() );
		buff.append( " MAP variables{\n" );
		int num = lstModel2.getSize();
		for( int i=0; i<num; i++ ){
			buff.append( ifier.objectToString( lstModel2.getElementAt(i) ) );
			buff.append( '\n' );
		}
		buff.append( "}\n" );

		return buff;
	}

	/** @since 20070311 */
	public StringBuffer appendSettingsExact( StringBuffer buff ) throws Exception{
		buff.append( ", exact algorithm\n" );
		append( myLabelTimeout.getText(), myTfTimeout.getText(), buff );
		if( myCBSlop.isSelected() ){
			buff.append( "sloppy, " );
			append( myLabelSlop.getText(), myTfSlop.getText(), buff );
		}
		append( myLabelWidthBarrier.getText(), myTfWidthBarrier.getText(), buff );
		return buff;
	}

	/** @since 20070311 */
	public StringBuffer appendSettingsApproximate( StringBuffer buff ) throws Exception{
		buff.append( ", approximate algorithm\n" );
		append( myLabelSearchMethod.getText(), cmbSearch.getSelectedItem(), buff );
		append( myLabelInitMethod.getText(),   cmbInit.getSelectedItem()  , buff );
		append( myLabelMaxSteps.getText(),     txtSteps.getText()         , buff );
		return buff;
	}

	/** @since 20070311 */
	public StringBuffer append( String caption, Object value, StringBuffer buff ){
		buff.append( caption );
		buff.append( ' ' );
		buff.append( value.toString() );
		buff.append( '\n' );
		return buff;
	}

	private  NetworkInternalFrame  frmNet;  //stores the NetworkInternalFrame
	private  MAPInternalFrame      parent;  //stores the container of this panel.
	private  boolean               myFlagStale;
	private  Collection            myWidgetsApprox, myWidgetsExact;

	//All visiual component declarations
	private  DecimalField          myTfSlop;
	private  JButton               btnOneToMAP,
	                               btnOneToAvailable,
	                               btnAllToMAP,
	                               btnAllToAvailable,
	                               btnGo,
	                               myButtonAdjust;
	private  JCheckBox             myCBSlop;
	private  JComboBox             cmbSearch, cmbInit;
	private  JLabel                myLabelMaxSteps,
	                               myLabelSearchMethod,
	                               myLabelInitMethod,
	                               myLabelTimeout,
	                               myLabelSlop,
	                               myLabelWidthBarrier,
	                               myLabelCount;
	private  JList                 jList1, jList2;
	private  JPanel                myPanelToFind;
	private  JRadioButton          myRadioExact, myRadioApproximate;
	private  JScrollPane           pan1, pan2;
	private  MPEPanel              myMPEPanel;
	private  SortedListModel       lstModel1, lstModel2;
	private  WholeNumberField      txtSteps,
	                               myTfTimeout,
	                               myTfWidthBarrier;

	public static final String STR_MSG_PRUNING = "pruning network for map...";
	public static final boolean FLAG_PRUNE_APPROXIMATE = true;
	private static final boolean FLAG_ALLOW_LIST_EDIT = false;

	public static final int INT_DEFAULT_STEPS = (int)25;
	public static final int INT_STEPS_FLOOR = (int)1;
	public static final int INT_STEPS_CEILING = (int)999999;

	public static final int INT_DEFAULT_TIMEOUT = (int)60;
	public static final int INT_TIMEOUT_FLOOR = (int)0;
	public static final int INT_TIMEOUT_CEILING = (int)9999999;

	private static final SearchMethod SEARCH_OPTION_DEFAULT = SearchMethod.getDefault();
	private static final InitializationMethod INIT_OPTION_DEFAULT = InitializationMethod.getDefault();

	private static final int INT_WIDTH_BARRIER_DEFAULT = (int)0;
	private static final int INT_WIDTH_BARRIER_FLOOR = (int)0;
	private static final int INT_WIDTH_BARRIER_CEILING = (int)999;

	public static final double
	  DOUBLE_INVALID      =    -99,
	  DOUBLE_SLOP_FLOOR   =      0,
	  DOUBLE_SLOP_CEILING =      1;
}
