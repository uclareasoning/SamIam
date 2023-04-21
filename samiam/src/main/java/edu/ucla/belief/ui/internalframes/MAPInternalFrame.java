package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.util.*;
import edu.ucla.belief.*;
import edu.ucla.belief.inference.map.*;
import edu.ucla.belief.inference.*;
import java.net.*;

/**
	@author  Paul Medvedev
	@version
	Created on May 15, 2001, 6:18 PM
*/
public class MAPInternalFrame extends javax.swing.JInternalFrame implements EvidenceChangeListener, CPTChangeListener, InternalFrameListener, NetStructureChangeListener, ActionListener
{
	public   static  final  String   STR_MSG_MAP        =  "calculating maximum a posteriori (map)...",
	                                 STR_FILENAME_ICON  =  "MAP16.gif";
	public   static         boolean  FLAG_SHOW_BUTTONS  =  true;

	private NetworkInternalFrame frmNet;
	JSplitPane panMain;  //used as content pane for MAPInternalFrame
	private boolean firstTime; //Is this the first that our frame is made visible (it hasn't been closed before)?

	public MAPInternalFrame( NetworkInternalFrame fn )
	{
		super("MAP Computation", true, true, true, true);

		frmNet = fn;
		if( frmNet != null ){
			frmNet.addEvidenceChangeListener(     this );
			frmNet.addCPTChangeListener(          this );
			frmNet.addNetStructureChangeListener( this );
		}

		panMain   = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
		firstTime = true;

		addInternalFrameListener( this );

		init();
	}

	/** @since 092404 */
	public MPEPanel getChildMPEPanel(){
		Component[] comps = panMain.getComponents();
		for( int i=0; i<comps.length; i++ ) if( comps[i] instanceof MPEPanel ) return (MPEPanel) comps[i];
		return (MPEPanel)null;
	}

	/** @since 20070311 */
	public final SamiamAction action_COPYTEXT = new SamiamAction( "Text", "copy to system clipboard all settings and results as text", 't', MainToolBar.getIcon( "Copy16.gif" ) ){
		public void actionPerformed( ActionEvent e ){
			try{
				if( pnlIn == null ) return;
				StringBuffer buff = new StringBuffer( 0x800 );
				pnlIn.append( buff );
				Util.copyToSystemClipboard( buff.toString() );
			}catch( Exception exception ){
				System.err.println( "warning: MAPInternalFrame.action_COPYTEXT.aP() caught " + exception );
			}
		}
	};

	/** @since 120303 */
	private void init()
	{
		Icon iconFrame = MainToolBar.getIcon( STR_FILENAME_ICON );
		if( iconFrame != null ) setFrameIcon( iconFrame );

		setDefaultCloseOperation( HIDE_ON_CLOSE );
		//setClosable( false );
		myPanelMain = new JPanel( new BorderLayout() );

		myButtonClose = new JButton( "Close" );
		myButtonClose.addActionListener( this );

		myButtonCodeBandit = new JButton( CodeToolInternalFrame.STR_DISPLAY_NAME, MainToolBar.getIcon( CodeToolInternalFrame.STR_FILENAME_BUTTON_ICON ) );
		myButtonCodeBandit.addActionListener( this );

		JPanel pnlButtons = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlButtons.add( Box.createVerticalStrut(2), c );
		c.gridwidth = 1;
		pnlButtons.add( new JButton( action_COPYTEXT ), c );
		pnlButtons.add( myButtonCodeBandit, c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnlButtons.add( myButtonClose, c );
		pnlButtons.add( Box.createVerticalStrut(2), c );

		myPanelMain.add( pnlButtons, BorderLayout.SOUTH );

		getContentPane().add( myPanelMain );

		myPanelButtons = pnlButtons;
	}

	/** interface ActionListener
		@since 20031203 */
	public void actionPerformed( ActionEvent e )
	{
		Object src = e.getSource();
		if( src == myButtonClose ){
			//try{
			//	setClosed( true );
			//}catch( java.beans.PropertyVetoException ex ){
			//	ex.printStackTrace();
			//}
			setVisible( false );
			return;
		}
		else if( src == myButtonCodeBandit ){
			pnlIn.codeBandit();
		}
	}

	/**
		interface EvidenceChangeListener
		@author Keith Cascio
		@since 071003
	*/
	public void warning( EvidenceChangeEvent ece ) {}

	/**
		For interface EvidenceChangeListener
		@author Keith Cascio
		@since 052802
	*/
	public void evidenceChanged( EvidenceChangeEvent ece )
	{
		abort();
		if( listModelKnown != null && listModelToFind != null )
		{
			BeliefNetwork bn = this.getBeliefNetwork();
			Map mapEvidence = bn.getEvidenceController().evidence();
			Set setEvidence = mapEvidence.keySet();
			Object next;
			for( Iterator it = ece.recentEvidenceChangeVariables.iterator(); it.hasNext(); )
			{
				next = it.next();
				if( setEvidence.contains( next ) )
				{
					listModelKnown.removeElement( next );
					listModelToFind.removeElement( next );
				}
				else if( !listModelKnown.contains( next ) ) listModelKnown.addElement( next );
			}
		}
	}

	/** @since 20020813 */
	public void cptChanged( CPTChangeEvent evt ){
		abort();
	}

	/** @since 20050812 */
	private void abort(){
		synchronized( mySynchAbort ){
			if( myFlagAborting ) return;
			myFlagAborting = true;

			if( this.isVisible() ) setVisible( false );
			if( pnlIn != null ) pnlIn.cancelComputation();

			myFlagAborting = false;
		}
	}

	/**
		@author Keith Cascio
		@since 041503 tax day
	*/
	public void netStructureChanged( NetStructureEvent ev )
	{
		listModelKnown = null;
		listModelToFind = null;
	}

	/*/ @since 071304 //
	public void checkStale()
	{
		if( pnlIn != null && pnlIn.isStale() )
		{
			//System.out.println( "MAPInternalFrame refreshing stale InputPanel" );
			BeliefNetwork bn = this.getBeliefNetwork();
			Collection setToFind = new LinkedList();
			Set allVars = new HashSet( bn );
			allVars.removeAll( bn.getEvidenceController().evidence().keySet() );
			DisplayableFiniteVariable dVar = null;
			for( Iterator itAllVars = allVars.iterator(); itAllVars.hasNext(); ){
				dVar = (DisplayableFiniteVariable) itAllVars.next();
				if( dVar.isMAPVariable() ) setToFind.add( dVar );
			}
			pnlIn.setMAPVariables( setToFind );
		}
	}*/

	/** @since 20070310 */
	public BeliefNetwork getBeliefNetwork(){
		if( this.frmNet != null ) return this.frmNet.getBeliefNetwork();
		else return this.myDebugBN;
	}

	public void reInitialize()
	{
		//System.out.println( "MAPInternalFrame.reInitialize()" );

		BeliefNetwork bn = this.getBeliefNetwork();

		//disable record keeping - i.e. force a fresh reinitialization
		listModelKnown = null;
		listModelToFind = null;

		if( listModelKnown == null || listModelToFind == null )
		{
			Iterator it = null;

			Collection setToFind = new LinkedList();//new TreeSet();//new HashSet();
			Collection setKnown = new LinkedList();
			StandardNode dVar = null;

			// Create a new set of variables and remove those whose evidence have
			// set keys (why remove, I don't know)
			Set allVars = new HashSet( bn );
			allVars.removeAll( bn.getEvidenceController().evidence().keySet() );

			for( Iterator itAllVars = allVars.iterator(); itAllVars.hasNext(); )
			{
				dVar = (StandardNode) itAllVars.next();
				if( dVar.isMAPVariable() ) setToFind.add( dVar );
				else setKnown.add( dVar );
			}

			pnlIn = new InputPanel(this, frmNet, setKnown, setToFind);
		}
		else pnlIn = new InputPanel( this, frmNet, listModelKnown, listModelToFind );

		//Set up empty output panel (right side of the frame)
		OutputPanel pnlOut = Bridge2Tiger.Troll.solicit().newOutputPanel( new HashMap(), bn, false );//new OutputPanel( new HashMap(), bn );

		//Set up Split Pane, adding pnlIn and pnlOut to it
		int l = panMain.getDividerLocation();
		panMain.setLeftComponent (pnlIn);
		panMain.setRightComponent (pnlOut);
		panMain.setOneTouchExpandable(true);
		pnlIn.setMinimumSize (new Dimension (0,0));
		pnlOut.setMinimumSize (new Dimension (0,0));

		if (firstTime) panMain.setDividerLocation( 0x100 );//if we are showing for the first time, set up divider to initial position
		else panMain.setDividerLocation(l);//else keep the divider in the same place

		firstTime = false;//no longer first time

		//setContentPane(panMain);
		myPanelMain.add( panMain, BorderLayout.CENTER );

		if( myPanelButtons != null ){ myPanelButtons.setVisible( FLAG_SHOW_BUTTONS ); }
	}

	public void internalFrameClosing( InternalFrameEvent e ){
		//System.out.println( "MAPInternalFrame.internalFrameClosing()" );
		abort();
	}
	public void internalFrameOpened(InternalFrameEvent e) {}
	public void internalFrameClosed(InternalFrameEvent e) {}
	public void internalFrameIconified(InternalFrameEvent e) {}
	public void internalFrameDeiconified(InternalFrameEvent e) {}

	/** @since 20040713 */
	public void internalFrameActivated( InternalFrameEvent e ){
		//checkStale();
		if( pnlIn != null ) pnlIn.refresh();
	}

	public void internalFrameDeactivated(InternalFrameEvent e) {}

	/**
		@author Keith Cascio
		@since 120303
	*/
	public void setVisible( boolean flag ){
		//System.out.println( "MAPInternalFrame.setVisible( "+flag+" )" );
		//if( !flag ) myOnClosing();
		super.setVisible( flag );
	}

	/**
		was internalFrameClosing()
		@author Keith Cascio
		@since 120303
	*/
	private void myOnClosing(){
		if( pnlIn != null ){
			listModelKnown = pnlIn.getKnown();
			listModelToFind = pnlIn.getToFind();
		}
	}

	/** @since 092404 */
	public InputPanel getInputPanel(){
		return pnlIn;
	}

	/** test/debug
		@since 20070310 */
	public static void main( String[] args )
	{
		Util.DEBUG_VERBOSE = true;
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		String path = "c:\\keithcascio\\networks\\cancermap.net";
		String envPathNetworks = System.getenv( "NETWORKSPATH" );
		if( envPathNetworks != null && envPathNetworks.length() > 0 ){
			path = envPathNetworks + java.io.File.separator + "cancermap.net";
		}

		if( args.length > 0 ) path = args[0];

		BeliefNetwork bn = null;
		try{
			bn = NetworkIO.read( path );
		}catch( Exception e ){
			System.err.println( "warning: caught " + e );
		}finally{
			if( bn == null ){
				System.err.println( "Error: Failed to read " + path );
				return;
			}
		}

		try{
			FiniteVariable varA = (FiniteVariable) bn.forID( "A" );
			bn.getEvidenceController().observe( varA, varA.instance( 0 ) );
		}catch( Exception exception ){
			System.err.println( "warning: caught " + exception );
		}

		final MAPInternalFrame mif = new MAPInternalFrame( null );
		mif.myDebugBN = bn;
		mif.addInternalFrameListener( new InternalFrameAdapter(){
			public void internalFrameClosing(InternalFrameEvent e){
				Util.STREAM_TEST.println( "MAPInternalFrame size: " + mif.getSize() );
			}
		} );

		JButton btnX = new JButton( "function x" );
		btnX.addActionListener( new ActionListener(){
			public void actionPerformed( ActionEvent e ){}
		} );
		JPanel pnlButtons = new JPanel();
		pnlButtons.add( btnX );

		JDesktopPane pain = new JDesktopPane();
		pain.add( mif );

		mif.setBounds( new Rectangle( new Point(10,10), new Dimension( 550, 400 ) ) );
		mif.reInitialize();
		mif.setVisible( true );

		JFrame frame = new JFrame( "DEBUG MAPInternalFrame" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		Container contentPain = frame.getContentPane();
		contentPain.setLayout( new BorderLayout() );
		contentPain.add( pain, BorderLayout.CENTER );
		contentPain.add( pnlButtons, BorderLayout.EAST );
		contentPain.add( new JLabel( "network: " + path ), BorderLayout.SOUTH );
		//frame.pack();
		frame.setSize( 800,700 );
		Util.centerWindow( frame );
		frame.setVisible( true );

		try{
			mif.setSelected( true );
		}catch( java.beans.PropertyVetoException propertyvetoexception ){
			System.err.println( "warning: caught " + propertyvetoexception );
		}
	}

	private BeliefNetwork myDebugBN;

	protected SortedListModel listModelKnown, listModelToFind;
	protected InputPanel pnlIn;
	protected Container myPanelMain, myPanelButtons;
	protected JButton myButtonClose, myButtonCodeBandit;
	private boolean myFlagAborting = false;
	private Object mySynchAbort = new Object();
}
