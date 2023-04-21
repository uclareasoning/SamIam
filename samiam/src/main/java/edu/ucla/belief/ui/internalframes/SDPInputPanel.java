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
import edu.ucla.util.SdpProperty;
import edu.ucla.util.code.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import il2.inf.map.MapSearch;
import il2.inf.SDP.*;
import il2.model.BayesianNetwork;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.util.*;
import java.net.*;
import java.io.PrintWriter;

/**
    @author  Suming Chen
	@version
	Created on March 27, 2013, 1:18 PM
    Panel used for input on the left hand side of the SDPInternalFrame
*/
public class SDPInputPanel extends javax.swing.JPanel implements java.awt.event.ActionListener, NodePropertyChangeListener
{
	// constructor:
	// toFind: pre-selected variable to be used in MAP search
	// known:  variables not used in MAP search
	// NOTE: the names of the variables are misleading, i will change them later
	public SDPInputPanel( SDPInternalFrame p, NetworkInternalFrame fn, Collection known, Collection toFind , Collection toFindDecision)
	{
		this( p, fn );
        //System.out.println("first one");
		BeliefNetwork bn = this.getBeliefNetwork();
		int totalSize = bn.size() + 10;//buffer
		Comparator comp = VariableComparator.getInstance();
		SortedListModel model1 = new SortedListModel( known, comp, totalSize );
		SortedListModel model2 = new SortedListModel( toFind, comp, totalSize );
        SortedListModel model3 = new SortedListModel( toFindDecision, comp, totalSize );
		initComponents( model1, model2, model3, bn );
	}

	public SDPInputPanel( SDPInternalFrame p, NetworkInternalFrame fn, SortedListModel known, SortedListModel toFind , SortedListModel toFindDecision )
	{
		this( p, fn );
        //System.out.println("second one");
		initComponents( known, toFind, toFindDecision, this.getBeliefNetwork() );
	}

	private SDPInputPanel( SDPInternalFrame p, NetworkInternalFrame fn )
	{
		super( new GridBagLayout() );
        //System.out.println("third one");
		frmNet = fn;
		parent = p;
		if( frmNet != null ) frmNet.addNodePropertyChangeListener( this );
	}



	// arrange the frame layout
	private void initComponents( SortedListModel known, SortedListModel toFind, SortedListModel toFindDecision, Collection variables )
	{
        //System.out.println("init components");
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.weightx   = 1;
		add( makeParametersPanel(),                    gbc );

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.weightx   = 1;
        gbc.weighty = 1;

		add( makeListsPanelDecisionVar(known, toFindDecision, variables), gbc );

        gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.BOTH;
		gbc.weightx   = 1;
        gbc.weighty = 3;
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
		//myRadioApproximate.doClick();
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

		Collection we = myWidgetsExact  = new LinkedList();
		myLabelThreshold = new javax.swing.JLabel( "Threshold:" );
        txtSteps = new RealNumberField( DOUBLE_DEFAULT_THRESHOLD, 5, DOUBLE_DEFAULT_MIN , DOUBLE_DEFAULT_MAX );
		dim = new Dimension (60, 20);
		txtSteps.setMinimumSize( dim );
		txtSteps.setPreferredSize( dim );

		btnGo = new javax.swing.JButton( "Compute" );
		btnGo.setMargin( new Insets( 2,0x40,2,0x40 ) );
		btnGo.setFont( btnGo.getFont().deriveFont( Font.BOLD ) );
		btnGo.addActionListener(this);

		//myRadioExact = new JRadioButton( "Exact" );
		//myRadioApproximate = new JRadioButton( "Approximate" );
		//ButtonGroup group = new ButtonGroup();
		//group.add( myRadioExact );
		//group.add( myRadioApproximate );
		//myRadioExact.addActionListener(this);
		//myRadioApproximate.addActionListener(this);

		JPanel             ret = new JPanel( new GridBagLayout() );
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridwidth = 1;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.weightx   = 0;
		ret.add( reg( myLabelThreshold,     we ), gbc );
		ret.add( hstrut( 8,                we ), gbc );
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill      = GridBagConstraints.HORIZONTAL;
		gbc.weightx   = 1;
		ret.add( reg( txtSteps,            we ), gbc );

	  //ret.add( vstrut( 2,                wa ), gbc );

		gbc.gridwidth = 1;
		gbc.fill      = GridBagConstraints.NONE;
		gbc.weightx   = 0;
		gbc.anchor    = GridBagConstraints.WEST;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
	  //ret.add( myRadioExact,                   gbc );
	  //ret.add( vstrut( 2,                we ), gbc );

	


		ret.setBorder( BorderFactory.createEmptyBorder( 0,2,4,2 ) );

		return ret;
	}

	/** @since 20040713 */
	public void setMAPVariables( Collection toFind )
	{
		synchronized( mySynchRefresh ){
		BeliefNetwork bn          = this.getBeliefNetwork();
		lstModel2                 = new SortedListModel( toFind, VariableComparator.getInstance(), bn.size() + 10 );
        //System.out.println("after refresh: setmap variables");
		if( Thread.currentThread().isInterrupted() ) return;
		jList2                    = new JList(       lstModel2 );
		pan2                      = new JScrollPane( jList2    );
		if( Thread.currentThread().isInterrupted() ) return;
		ListCellRenderer renderer = new VariableLabelRenderer( jList2.getCellRenderer(), bn );
		jList2.setCellRenderer( renderer );
        //System.out.println("exception here!");
		myPanelToFind.removeAll();
        //System.out.println("BOOMMMMM ");
        //myPanelToFindDecision.removeAll();
		if( Thread.currentThread().isInterrupted() ) return;

        //myPanelToFindDecision.add( pan2 );
        //myPanelToFindDecision.revalidate();
        //myPanelToFindDecision.repaint();
        myPanelToFind.add( pan2 );
		myPanelToFind.revalidate();
		myPanelToFind.repaint();

		recount();
		myFlagStale = false;
		}
	}

    /** @since 20040713 */
	public void setSDPVariables( Collection toFind )
	{
		synchronized( mySynchRefresh ){
		BeliefNetwork bn          = this.getBeliefNetwork();
		lstModel3                 = new SortedListModel( toFind, VariableComparator.getInstance(), bn.size() + 10 );
        //System.out.println("after refresh: setsdp variables");
		if( Thread.currentThread().isInterrupted() ) return;
		jList3                    = new JList(       lstModel3 );
		pan3                      = new JScrollPane( jList3    );
		if( Thread.currentThread().isInterrupted() ) return;
		ListCellRenderer renderer = new VariableLabelRenderer( jList3.getCellRenderer(), bn );
		jList3.setCellRenderer( renderer );
        myPanelToFindDecision.removeAll();
        //myPanelToFindDecision.removeAll();
		if( Thread.currentThread().isInterrupted() ) return;

        myPanelToFindDecision.add( pan3 );
        myPanelToFindDecision.revalidate();
        myPanelToFindDecision.repaint();


        
		recount2();
		myFlagStale = false;
		}
	}

	/** @since 20070310 */
	private void recount(){
		try{
			myLabelCount.setText( Integer.toString( lstModel2.getSize() ) );
		}catch( Exception exception ){
			System.err.println( "warning: SDPInputPanel.recount() caught " + exception );
		}
	}


	private void recount2(){
		try{
			myLabelCount2.setText( Integer.toString( lstModel3.getSize() ) );
		}catch( Exception exception ){
			System.err.println( "warning: SDPInputPanel.recount() caught " + exception );
		}
	}
	private JComponent makeListsPanelDecisionVar( SortedListModel known, SortedListModel toFindDecision, Collection variables )
	{

		//Initialize list boxes and their scroll panes
        //System.out.println("make lists panel decision var");
		lstModel3 = toFindDecision;
		jList3 = new javax.swing.JList(lstModel3);
		pan3 = new JScrollPane (jList3);
		ListCellRenderer renderer = new VariableLabelRenderer( jList3.getCellRenderer(), variables );
		jList3.setCellRenderer( renderer );
		myPanelToFindDecision = new JPanel( new GridLayout() );
		myPanelToFindDecision.add( pan3 );

        //myPanelToFind = new JPanel( new GridLayout() );
        //myPanelToFind.add( pan2 );
		JPanel ret = new JPanel( new java.awt.GridBagLayout() );
		java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();

        //jList2.

        gbc.anchor     = GridBagConstraints.NORTHWEST;
        ret.add( Box.createHorizontalStrut(4), gbc );

        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        ret.add( myLabelCount2 = new JLabel( "0", JLabel.LEFT ),             gbc );
        gbc.weightx    = 10;
        ret.add( new JLabel( " Decision Variables ", JLabel.LEFT ), gbc );
        String caption = "Variable Selection Tool";
        JLabel label   = (frmNet == null) ? new JLabel( caption ) : new HyperLabel( caption, frmNet.getParentFrame().action_VARIABLESELECTION, SdpProperty.PROPERTY );
        //System.out.println("label is sdp");
        gbc.weightx    = 0;
        ret.add( label, gbc );
        gbc.gridwidth  = GridBagConstraints.REMAINDER;
        gbc.weightx    = 1;
        ret.add( new JLabel( " ", JLabel.LEFT ), gbc );

        gbc.anchor     = GridBagConstraints.SOUTH;
        gbc.weightx    = 1; gbc.weighty = 1;
        gbc.fill       = java.awt.GridBagConstraints.BOTH;
        ret.add( myPanelToFindDecision, gbc );
        //ret.add( myPanelToFind, gbc );
		

		recount2();
		return ret;
	}

    	private JComponent makeListsPanel( SortedListModel known, SortedListModel toFind, Collection variables )
	{

		//Initialize list boxes and their scroll panes
		lstModel2 = toFind;
        //System.out.println("make lists panel");
		jList2 = new javax.swing.JList(lstModel2);
		pan2 = new JScrollPane (jList2);
		ListCellRenderer renderer = new VariableLabelRenderer( jList2.getCellRenderer(), variables );
		jList2.setCellRenderer( renderer );
		myPanelToFind = new JPanel( new GridLayout() );
		myPanelToFind.add( pan2 );

		JPanel ret = new JPanel( new java.awt.GridBagLayout() );
		java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();

        //jList2.

        gbc.anchor     = GridBagConstraints.NORTHWEST;
        ret.add( Box.createHorizontalStrut(4), gbc );
        
        gbc.gridwidth  = 1;
        gbc.weightx    = 0;
        ret.add( myLabelCount = new JLabel( "0", JLabel.LEFT ),             gbc );
        gbc.weightx    = 10;
        ret.add( new JLabel( " Query Variables ", JLabel.LEFT ), gbc );
        String caption = "Variable Selection Tool";
        JLabel label   = (frmNet == null) ? new JLabel( caption ) : new HyperLabel( caption, frmNet.getParentFrame().action_VARIABLESELECTION, MapProperty.PROPERTY );
        //System.out.println("label is map");
        gbc.weightx    = 0;
        ret.add( label, gbc );
        gbc.gridwidth  = GridBagConstraints.REMAINDER;
        gbc.weightx    = 1;
        ret.add( new JLabel( " ", JLabel.LEFT ), gbc );
        
        gbc.anchor     = GridBagConstraints.SOUTH;
        gbc.weightx    = 1; gbc.weighty = 1;
        gbc.fill       = java.awt.GridBagConstraints.BOTH;
        ret.add( myPanelToFind, gbc );
        

		recount();
		return ret;
	}
   
    
	/** interface NodePropertyChangeListener
		@since 20040713 */
	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		//System.out.println( e );
        //System.out.println("node property changed: " + e.property);
		if( e.property == MapProperty.PROPERTY ){
            //  System.out.println("map changed!!");
			if( ! myFlagStale ){
				myFlagStale = true;
				if( myLabelCount != null ) myLabelCount.setText( "-" );
				if( lstModel2    != null ) lstModel2.clear();
			}
			refresh( false );
		} else if (e.property == SdpProperty.PROPERTY ) {
            //System.out.println("SDP decision var changed!!");
            if( ! myFlagStale ){
				myFlagStale = true;
				if( myLabelCount2 != null ) myLabelCount2.setText( "-" );
				if( lstModel3    != null ) lstModel3.clear();
			}
			refresh( false );

        }
        
	}

	/** @since 20070310 */
	public class RunRefresh extends Interruptable{
		public void runImpl( Object arg1 ) throws InterruptedException {
			if( arg1 != Boolean.TRUE ){
				if( ! SDPInputPanel.this.myFlagStale ) return;
				Thread.sleep( 0x200 );
				if( ! SDPInputPanel.this.myFlagStale ) return;
			}

            //System.out.println("the variables have been adjusted");
			BeliefNetwork bn        = SDPInputPanel.this.getBeliefNetwork();
			Set           set       = new HashSet( bn );
			if( Thread.currentThread().isInterrupted() ) return;
			set.removeAll( bn.getEvidenceController().evidence().keySet() );
			StandardNode  dVar      = null;
            Set sdp_decision_variables = new HashSet();
            Set query_variables = new HashSet();
			for( Iterator itAllVars = set.iterator(); itAllVars.hasNext(); ){
                StandardNode temp_node = (StandardNode) itAllVars.next();
                //System.out.println("temp_node is " + temp_node);
                if( temp_node.isMAPVariable() ) 
                    query_variables.add(temp_node);
				else if( temp_node.isSDPVariable() ) 
                    sdp_decision_variables.add(temp_node);
                
			}/*
            for( Iterator itAllVars = sdp_decision_variables.iterator(); itAllVars.hasNext(); ){
                StandardNode temp_node = (StandardNode) itAllVars.next();
                //System.out.println("temp_node to be set as sdp is " + temp_node);
			}
            for( Iterator itAllVars = query_variables.iterator(); itAllVars.hasNext(); ){
                StandardNode temp_node = (StandardNode) itAllVars.next();
                //System.out.println("temp_node to be set as query is " + temp_node);
                }*/
			if( Thread.currentThread().isInterrupted() ) return;
			SDPInputPanel.this.setSDPVariables( sdp_decision_variables );
            SDPInputPanel.this.setMAPVariables( query_variables );
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


	/**
		Build a set of MAP variables from the selection in list box,
		excluding any variables that are set as evidence.
		@since 051004
	*/
	public Set buildSetMAPVariables( Map evidence )
	{
		//System.out.print( "InputPanel.buildSetMAPVariables() -> " );
        //System.out.println(" buildset MAP vars");
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

    public Set buildSetSDPVariables( Map evidence )
	{
		//System.out.print( "InputPanel.buildSetMAPVariables() -> " );
        //System.out.println(" buildset MAP vars");
		Set vars = new HashSet();

		Variable dVar;
		for (int i = 0; i < lstModel3.getSize(); i++)
		{
			dVar = (Variable) lstModel3.getElementAt(i);
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
				SDPInputPanel.this.doMAP();
			//}catch( IllegalStateException ise ){
			//	JOptionPane.showMessageDialog( InputPanel.this, "MAP tool requires the shenoy-shafer algorithm.", "Cannot execute MAP", JOptionPane.ERROR_MESSAGE );
			//}
		}
	};

	public static final int INT_PRIORITY_COMPUTATION = ((Thread.NORM_PRIORITY - Thread.MIN_PRIORITY)/(int)2) + Thread.MIN_PRIORITY;

	/** called when update button is pressed */
	public void doMAP() throws IllegalStateException
	{

		boolean flagUseExact = false;//myRadioExact.isSelected();

		BeliefNetwork   bn = this.getBeliefNetwork();
		InferenceEngine ie = (frmNet == null) ? null : frmNet.getInferenceEngine();
		JoinTreeInferenceEngineImpl jtie = null;
		double prE = DOUBLE_INVALID;

		if( (!flagUseExact) && (!FLAG_PRUNE_APPROXIMATE) )
		{
			//if( ie.canonical() instanceof JoinTreeInferenceEngineImpl ) jtie = (JoinTreeInferenceEngineImpl) ie.canonical();
			//else throw new IllegalStateException( "InferenceEngine must be instance of JoinTreeInferenceEngineImpl." );

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
            Set DvarsUnpruned = buildSetSDPVariables( evidenceUnpruned ); 

            StandardNode dVar = null;
            if (DvarsUnpruned.size() > 1) {
                String msg = "Only one decision variable allowed.";
                JOptionPane.showMessageDialog( this, msg);
                return;
            }
            if (DvarsUnpruned.size() < 1) {
                String msg = "Decision variable required!";
                JOptionPane.showMessageDialog( this, msg);
                return;
            }
            for( Iterator allDVars = DvarsUnpruned.iterator(); allDVars.hasNext(); )
			{
                
				dVar = (StandardNode) allDVars.next();
                if (dVar.size() != 2) {
                    String msg = "Decision variable must be binary.";
                    JOptionPane.showMessageDialog( this, msg);
                    return;
                        
                }

			}
			
            
            parent.setCursor( UI.CURSOR_WAIT );

            doExact( evidenceUnpruned, varsUnpruned, DvarsUnpruned);
			

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
			JOptionPane.showMessageDialog( this, "SDP Tool ran out of memory.", "Out of memory", JOptionPane.ERROR_MESSAGE );
		}
		finally{
			if( btnGo != null ) btnGo.setEnabled( true );
			edu.ucla.belief.ui.util.Util.popStatusWest( frmNet, SDPInternalFrame.STR_MSG_SDP );
			parent.setCursor( UI.CURSOR_DEFAULT );
		}

		return;
	}

	/** @since 072604 */
	private void doExact( Map evidenceUnpruned, Set varsUnpruned, Set DvarUnpruned ) throws IllegalStateException
	{
		edu.ucla.belief.ui.util.Util.pushStatusWest( frmNet, SDPInternalFrame.STR_MSG_SDP );

		Map result = null;


		//BeliefNetwork dbn = this.getBeliefNetwork();

		BeliefNetwork bn = this.getBeliefNetwork();//null;

        StandardNode temp_var = null;
        String decision_var = null;
        for( Iterator itDVars = DvarUnpruned.iterator(); itDVars.hasNext(); ){
                temp_var = (StandardNode) itDVars.next();
                decision_var = temp_var.getID();
                System.out.println(decision_var);
        }

        String[] query_var_array = new String[varsUnpruned.size()];
        int counter = 0;
        for( Iterator itHVars = varsUnpruned.iterator(); itHVars.hasNext(); ){
                temp_var = (StandardNode) itHVars.next();
                query_var_array[counter] = temp_var.getID();
                counter++;
        }
        //String[] decision_var_array = DvarUnpruned.toArray(new String[DvarUnpruned.size()]);//listdecision.toArray();
        //String decision_var = decision_var_array[0];

        //List<String> listquery = new ArrayList<String>(varsUnpruned);
        //String[] query_variables = varsUnpruned.toArray(new String[varsUnpruned.size(]);//listquery.toArray();
                                                                
        System.out.println("decision var is "+ DvarUnpruned);
        System.out.println("H vars are " + varsUnpruned);
        System.out.println("evidence is " + evidenceUnpruned);
		//IMPORTANT: NEED TO CONVERT THIS INTO VARS THAT IL2 CAN WORK WITH.
        //int steps = 2; //temporary fix
        double threshold_val = txtSteps.getValue();
        System.out.println("threshold_val is  " + threshold_val);
        PrintWriter pwriter = new PrintWriter(System.out, true);
        //computing SDP should go here
        il2.bridge.Converter conv = new il2.bridge.Converter();
        BayesianNetwork new_bn = conv.convert(bn);
        System.out.println("about to do SDP!"); //stops here. it doesn't work when evidence is set

        //change evidence type
        Map evidence_map = new HashMap();
        StandardNode temp_key = null;
        for ( Iterator evidence_iterator = evidenceUnpruned.entrySet().iterator(); evidence_iterator.hasNext(); ){
            Map.Entry entry = (Map.Entry) evidence_iterator.next();
            temp_key = (StandardNode) entry.getKey();
            String temp_value = (String) entry.getValue();
            System.out.println("key = " + temp_key + "value = " + temp_value);
            evidence_map.put(temp_key.getID(), temp_value);
        }
        SDP sdp = new SDP(new_bn, evidence_map, pwriter, threshold_val, decision_var, query_var_array);
		sdp.computeSDP();
		//score = MR.getLastScore();
		//if( bn != dbn ) result = reTarget( result, dbn );
        System.out.println("Almost there!! showing result now");
		//showResult( prE, result, (MapSearch.MapInfo)null, score, dbn, false, false );
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
		}/*
		else if( src == myRadioExact || src == myRadioApproximate )
		{
			setParametersEnabled();
			return;
            }*/ 

		
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

    public SortedListModel getToFindDecision()
    {

        return lstModel3;
    }

	/** @since 092404 */
	public void update(){
		btnGo.doClick();
	}



	/** @since 20080117 */
	protected SDPInputPanel setVisible( Collection components, boolean visible ) throws Exception{
		for( Iterator it = components.iterator(); it.hasNext(); ){
			((Component)it.next()).setVisible( visible );
		}
		return this;
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
		buff.append( " same decision probability " );

        appendSettingsExact( buff );
		Stringifier ifier = getPreferredStringifier();
		//buff.append( '\n' );
		buff.append( myLabelCount.getText() );
		buff.append( " SDP variables{\n" );
        buff.append( myLabelCount2.getText() );
		buff.append( " Decision variables{\n" );
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
		append( txtSteps.getText(), txtSteps.getText(), buff );
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
	private  SDPInternalFrame      parent;  //stores the container of this panel.
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
	private  JLabel                myLabelThreshold,
                                   myLabelCount,
                                   myLabelCount2;
	private  JList                 jList1, jList2, jList3;
	private  JPanel                myPanelToFind, myPanelToFindDecision;
	private  JScrollPane           pan1, pan2, pan3;
	private  MPEPanel              myMPEPanel;
	private  SortedListModel       lstModel1, lstModel2, lstModel3;
    
    //private  RealNumberField2       txtSteps;
    private  RealNumberField      txtSteps;

	public static final String STR_MSG_PRUNING = "pruning network for map...";
	public static final boolean FLAG_PRUNE_APPROXIMATE = true;
	private static final boolean FLAG_ALLOW_LIST_EDIT = false;

	public static final double DOUBLE_DEFAULT_THRESHOLD = (double)0.5;
	public static final double DOUBLE_DEFAULT_MIN = (double)0.0;
	public static final double DOUBLE_DEFAULT_MAX  = (double)1.0;

	public static final int INT_DEFAULT_TIMEOUT = (int)60;
	public static final int INT_TIMEOUT_FLOOR = (int)0;
	public static final int INT_TIMEOUT_CEILING = (int)9999999;

	private static final SearchMethod SEARCH_OPTION_DEFAULT = SearchMethod.getDefault();
	private static final InitializationMethod INIT_OPTION_DEFAULT = InitializationMethod.getDefault();

	private static final int INT_WIDTH_BARRIER_DEFAULT = (int)0;
	private static final int INT_WIDTH_BARRIER_FLOOR = (int)0;
	private static final int INT_WIDTH_BARRIER_CEILING = (int)999;

	public static final double DOUBLE_INVALID      =    -99;

}
