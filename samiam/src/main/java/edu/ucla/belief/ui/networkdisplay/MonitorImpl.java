package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.EvidenceChangeEvent;
import edu.ucla.belief.InferenceEngine;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.Table;
import edu.ucla.belief.approx.*;
import edu.ucla.util.Prob;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariable;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.event.NodePropertyChangeEvent;
import edu.ucla.belief.ui.internalframes.ApproxInternalFrame;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/** based loosely on EvidenceDialog

	@author keith cascio
	@since  20030310 */
public class MonitorImpl extends JPanel implements Monitor
{
	protected DisplayableFiniteVariable myDFV;
	protected CoordinateTransformer     myPositionTransformer,
	                                    mySizeTransformer,
	                                    mySizeTransformerOriginal;
	protected List                      evidLabels;
	protected boolean                   myFlagSampleMode = false,
	                                    myFlagDoZoom     = false;
	private   JComponent                myApproxPanel;
	private   MonitorImpl               myApproxMonitor;
	private   ApproxEngine              myApproxEngine;
	private   Collection                myCollectionLabels;
	protected JLabel                    myLabelTitle,
	                                    myLabelKL,
                                        myLabelDist,
                                        myLabelConverges,
                                        myLabelConvergeLoops,
                                        myLabelFixedPoint,
                                        myLabelYKL,
                                        myLabelYConditionalENT,
                                        myLabelYConditionalKL,
                                        myLabelYConditionalENTPlusKL,
                                        myLabelFixedPointKL;
	private   JPanel                    myPanelFixedPointOuter,
	                                    myPanelFixedPointInner;
	protected Point                     myVirtualLocation = new     Point();
	protected Dimension                 myVirtualSize     = new Dimension();

	private   static       Color
	  COLOR_NEXT                        = ApproxInternalFrame.COLOR_REMOVED_EDGE_LIGHTER,
	  COLOR_FLIP_FALSE;
	public    static final Color
	  COLOR_APPROX                      = ApproxInternalFrame.COLOR_REMOVED_EDGE,
	  COLOR_FLIP_TRUE                   = Color.white;
	public    static final String
	  STR_TITLE_APPROX                  = "approximation",
	  STR_TITLE_FIXED                   = "fixed point",
	  STR_TITLE_FLIP                    = " (flip)",
	  STR_CAPTION_KL                    = "kl= ",
	  STR_CAPTION_DIST                  = "dist= ",
	  STR_CAPTION_CONVERGES             = "converges? ",
	  STR_CAPTION_LOOPS                 = "loops to converge= ",
	  STR_CAPTION_FIXEDPOINT            = "Fixed point: Pr^(Y|e)= ",
	  STR_CAPTION_YKL                   = "KL(Pr(Y|e),Pr^(Y|e))= ",
	  STR_CAPTION_YCONDITIONALENT       = "ENT(Y|e)= ",
	  STR_CAPTION_YCONDITIONALKL        = "KL(Y->X) using Pr(Y|e)= ",
	  STR_CAPTION_YCONDITIONALENTPLUSKL = "ENT(Y|e)+KL(Pr(Y|e),Pr^(Y|e))= ",
	  STR_CAPTION_FIXEDPOINTKL          = "KL(Y->X) using Pr^(Y|e)= ",
	  STR_KEY_PREFERENCE_BUNDLE         = MonitorImpl.class.getName();
	protected static final Point
	  theUtilPoint                      = new     Point();
	protected static final Dimension
	  theUtilDimension                  = new Dimension();
	private   static       TargetedBundle
	  BUNDLE_OF_PREFERENCES;
	public    static final boolean
	  FLAG_ODDS_NORMALIZE               =  true,
	  FLAG_ODDS_NORMALIZE_NETWORK       = false;
	public    static final int
	  INT_INITIAL_BUFFER_CAPACITY       = 20,
	  INT_BUFFER_CAPACITY_EXTENSION     = 20;
	private   static       double[]
	  bufferOdds	                    = new double[ INT_INITIAL_BUFFER_CAPACITY ],
	  bufferPr                          = new double[ INT_INITIAL_BUFFER_CAPACITY ];
	private   static       Table[]
	  TABLES                            = new Table[1];

	/** @since 20040614 */
	public void setApprox( ApproxEngine ae )
	{
		if( myApproxEngine != null ) throw new UnsupportedOperationException();

		if( (myApproxMonitor != null) && (myApproxMonitor.myApproxEngine == ae) ) return;

		if( COLOR_FLIP_FALSE == null ) COLOR_FLIP_FALSE = getBackground();

		myApproxPanel.removeAll();
		if( ae != null ){
			//System.out.println( getTitle() + " creating new myApproxMonitor" );
			myApproxMonitor = new MonitorImpl( myDFV, ae, mySizeTransformerOriginal );
			myApproxPanel.add( myApproxMonitor );
			evidenceChanged( null, (double)0.999999999 );
			myApproxMonitor.setVisible( true );
			pack();
			revalidate();
			repaint();
		}
	}

	/** @since 20040614 */
	private MonitorImpl( DisplayableFiniteVariable dspV, ApproxEngine ae, CoordinateTransformer xformer )
	{
		super();

		myFlagSampleMode = dspV.isSampleMode();
		myDFV = dspV;
		myApproxEngine = ae;
		//if( myDFV != null && !myDFV.isSampleMode() ) myDFV.getNetworkInternalFrame().addNodePropertyChangeListener( this );
		myPositionTransformer = CoordinateTransformerNull.getInstance();
		mySizeTransformer = mySizeTransformerOriginal = xformer;

		init( null );

		setTitle( STR_TITLE_APPROX );
		myLabelTitle.setForeground( COLOR_APPROX );
	}

	/** interface Monitor
		@since    20080226 */
	public int rotate(){
		int ret = -99;
		try{
			for( Iterator it = evidLabels.iterator(); it.hasNext(); ){
				ret = ((EvidenceLabel) it.next()).rotate();
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: MonitorImpl.rotate() caught " + thrown );
		}
		if( ret < 0 ){ dilate(); repaint(); }
		return ret;
	}

	/** interface Monitor
 		@since 20030310 */
	public JComponent asJComponent()
	{
		return this;
	}

	/** interface Monitor
		@since 20030310 */
	public void notifyBoundsChange()
	{
		NetworkInternalFrame nif = myDFV.getNetworkInternalFrame();
		if( nif != null )
		{
			NetworkDisplay display = nif.getNetworkDisplay();
			if( display.testExceedsBounds( myDFV.getNodeLabel() ) ) display.recalculateDesktopSize();
		}
	}

	public MonitorImpl(	SamiamPreferences monitorPrefs,
				DisplayableFiniteVariable dspV,
				CoordinateTransformer xformer )
	{
		super();

		myFlagSampleMode = dspV.isSampleMode();
		myDFV = dspV;
		if( myDFV != null && !myDFV.isSampleMode() ) myDFV.getNetworkInternalFrame().addNodePropertyChangeListener( this );
		myPositionTransformer = xformer;
		mySizeTransformer = mySizeTransformerOriginal = myPositionTransformer;

		init( monitorPrefs );
	}

	/** interface NodePropertyChangeListener
		@since 20030707 */
	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		if( e.variable == myDFV ) setTitle( myDFV.toString() );
	}

	/** @since 20090424 */
  //@Override
	public String toString(){
		StringBuffer buff = new StringBuffer( 0x80 );
		try{
			buff.append( myDFV.toString() ).append( "\n" );
			EvidenceLabel label;
			for( Iterator it = evidLabels.iterator(); it.hasNext(); ){
				buff.append( (label = (EvidenceLabel)it.next()).labelL().getText() )
					.append( " " )
					.append( label.labelR().getText() )
					.append( "\n" );
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: MonitorImpl.toString() caught " + thrown );
		}
		return buff.toString();
	}

	/** @since 20090424 */
	static private MouseInputAdapter ADAPTER_FOR_GLASS = new MouseInputAdapter(){
	  //@Override
		public void  mouseEntered( MouseEvent e ){
			((PanelClose)e.getComponent()).label().setVisible( true );
		}
	  //@Override
		public void   mouseExited( MouseEvent e ){
			((PanelClose)e.getComponent()).label().setVisible( false );
		}
	  //@Override
		public void  mouseClicked( MouseEvent e ){
			((PanelClose)e.getComponent()).monitor().setVisible( false );
		}
	};

	/** @since 20090424 */
	public class PanelClose extends JPanel{
		public PanelClose(){
			super( new GridLayout(1,1) );
			init();
		}

		private void init(){
			label                 = new JLabel( MainToolBar.getIcon( "Close9x9.gif" ) );
			JPanel panelClose     = this;
			panelClose.setOpaque( false );
			panelClose.add( label );
			label.setVisible( false );
			panelClose.addMouseListener( ADAPTER_FOR_GLASS );

			glass                 = new JPanel( new GridBagLayout() );
			glass.setOpaque( false );
			GridBagConstraints cg = new GridBagConstraints();
			cg.anchor             = GridBagConstraints.NORTHEAST;
			cg.weightx            = 1;
			glass.add( Box.createGlue(), cg );
			cg.weightx            = 0;
			cg.gridwidth          = GridBagConstraints.REMAINDER;
			glass.add(       panelClose, cg );
			cg.weighty            = 1;
			glass.add( Box.createGlue(), cg );
			glass.setBorder( BorderFactory.createEmptyBorder(1,1,1,1) );
		}

		public JComponent glass(){ return            glass; }
		public JLabel     label(){ return            label; }
		public Monitor  monitor(){ return MonitorImpl.this; }

		private JPanel    glass;
		private JLabel    label;
	}

	protected void init( SamiamPreferences monitorPrefs )
	{
		//MonitorImpl.this.myMonitorPrefs = monitorPrefs;
		if( monitorPrefs != null ) MonitorImpl.validatePreferenceBundle( monitorPrefs );

		setVisible( false );

		Border border = BorderFactory.createLineBorder( Color.black, (int)1 );
		if( myApproxEngine != null ) border = new edu.ucla.belief.ui.statusbar.NsidedBorder( (AbstractBorder)border, true, false, false, false );
		setBorder( border );
		Border debugBorder = null;
		if( FLAG_DEBUG_BORDERS ) debugBorder = BorderFactory.createLineBorder( Color.red, (int)1 );

		int size = myDFV.size();

		OverlayLayout   overlay = new OverlayLayout( this );
		this.setLayout( overlay );

		myLabelTitle = new JLabel( myDFV.toString() );
		evidLabels   = new ArrayList( size );

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints  c = new GridBagConstraints();
		JPanel           root = new JPanel( gridbag );
		this.add( new PanelClose().glass() );
		this.add(        root );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.NORTH;
		root.add( myLabelTitle, c );
		myLabelTitle.setBorder( BorderFactory.createEmptyBorder(0,2,0,2) );

		c.anchor = GridBagConstraints.WEST;

		/*
		Table conditional = null;
		int observedIndex = -1;
		if( !myFlagSampleMode )
		{
			InferenceEngine ie = myDFV.getInferenceEngine();
			if( ie != null ) conditional = ie.conditional( myDFV );
			observedIndex = myDFV.getObservedIndex();
		}*/

		MouseListener mouselistener = null;

		if( ! myFlagSampleMode ){
			mouselistener = new MouseAdapter(){
				public void mouseClicked( MouseEvent event ){
					MonitorImpl.this.rotate();
				}
			};
		}

		EvidenceLabel el;
		for( int i = 0; i < size; i++ ){
			evidLabels.add( (el = new EvidenceLabel( monitorPrefs, myDFV, i )).add( root, c ) );
			if( mouselistener  != null ){ el.addMouseListener( mouselistener ); }
			if( myApproxEngine != null ){ el.getEvidenceIcon().changeAutoColor( 0, COLOR_APPROX ); }
		}
		root.add( Box.createVerticalStrut(2), c );

		if( myApproxEngine == null ) add( myApproxPanel = new JPanel( new GridLayout() ), c );
		else{
			c.gridwidth = GridBagConstraints.REMAINDER;
			root.add( myLabelKL              = createLabel( STR_CAPTION_KL   ), c );
			root.add( myLabelDist            = createLabel( STR_CAPTION_DIST ), c );
			root.add( myPanelFixedPointOuter =  new JPanel( new GridLayout() ), c );
		}

		if( (myApproxEngine == null) && (!myFlagSampleMode) ) evidenceChanged( null, (double)0.999999999 );

		setVirtualSize( gridbag.preferredLayoutSize( this ) );
		//getPreferences();
		//setDoZoom( true );
		//recalculateActual();
		setPreferences();
		doLayout();

		if( (!myFlagSampleMode) && (myApproxEngine == null) )
		{
			addMouseListener( MonitorMouseListener.getInstance() );
			addMouseMotionListener( MonitorMouseListener.getInstance() );
		}
	}

	/** @since 20040617 */
	private JLabel createLabel( String caption ){
		JLabel ret = new JLabel( caption + "?" );
		if( COLOR_NEXT == ApproxInternalFrame.COLOR_REMOVED_EDGE_LIGHTER ) COLOR_NEXT = ApproxInternalFrame.COLOR_REMOVED_EDGE_DARKER;
		else COLOR_NEXT = ApproxInternalFrame.COLOR_REMOVED_EDGE_LIGHTER;
		ret.setForeground( COLOR_NEXT );
		if( myCollectionLabels == null ) myCollectionLabels = new LinkedList();
		myCollectionLabels.add( ret );
		return ret;
	}

	/** @since 20040617 */
	private void initPanelFixedPoint()
	{
		if( myPanelFixedPointInner == null )
		{
			myPanelFixedPointInner = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.WEST;

			myPanelFixedPointInner.add( myLabelConverges = createLabel( STR_CAPTION_CONVERGES ), c );
			myPanelFixedPointInner.add( myLabelConvergeLoops = createLabel( STR_CAPTION_LOOPS ), c );
			myPanelFixedPointInner.add( myLabelFixedPoint = createLabel( STR_CAPTION_FIXEDPOINT ), c );
			myPanelFixedPointInner.add( myLabelYKL = createLabel( STR_CAPTION_YKL ), c );
			myPanelFixedPointInner.add( myLabelYConditionalENT = createLabel( STR_CAPTION_YCONDITIONALENT ), c );
			myPanelFixedPointInner.add( myLabelYConditionalKL = createLabel( STR_CAPTION_YCONDITIONALKL ), c );
			myPanelFixedPointInner.add( myLabelYConditionalENTPlusKL = createLabel( STR_CAPTION_YCONDITIONALENTPLUSKL ), c );
			myPanelFixedPointInner.add( myLabelFixedPointKL = createLabel( STR_CAPTION_FIXEDPOINTKL ), c );

			recalculateActual();
		}

		if( !myPanelFixedPointOuter.isAncestorOf( myPanelFixedPointInner ) ){
			myPanelFixedPointOuter.add( myPanelFixedPointInner );
			revalidate();
			repaint();
		}
	}

	/** @since 20030310 */
	public void setTitle( String text )
	{
		myLabelTitle.setText( text );
	}

	/** @since 20040614 */
	public String getTitle(){
		return myLabelTitle.getText();
	}

	/** @since 20030307 */
	public void pack()
	{
		//System.out.println( "(MonitorImpl)"+myDFV+".pack()..." );

		LayoutManager layout = getLayout();

		/*
		Dimension dim = myLabelTitle.getPreferredSize();
		int width = dim.width;
		int height = dim.height;
		EvidenceLabel el;
		for( Iterator itr = evidLabels.iterator(); itr.hasNext(); )
		{
			el = (EvidenceLabel) itr.next();
			dim = el.getPreferredSize();
			height += dim.height;
			width = Math.max( width, dim.width );
		}

		int bufferWidth = (int)16;
		int bufferHeight = (evidLabels.size() * (int)2);
		if( myFlagDoZoom )
		{
			bufferWidth	= (int) mySizeTransformer.virtualToActual( (double)bufferWidth );
			bufferHeight	= (int) mySizeTransformer.virtualToActual( (double)bufferHeight );
		}
		width	+= bufferWidth;
		height	+= bufferHeight;

		Rectangle bounds = new Rectangle( getLocation(), new Dimension( width, height ) );

		setBounds( bounds );
		setActualSize( bounds.getSize() );
		*/

		setActualSize( layout.preferredLayoutSize( this ) );
		confirmActualSize();

		doLayout();
	}

	/** @since 20080331 */
	public boolean dilate(){
		Dimension      pref = getLayout().preferredLayoutSize( this );
		Dimension    actual = getActualSize( new Dimension() );

		if( (pref.width > actual.width) || (pref.height > actual.height) ){
			actual.width  = Math.max( actual.width,  pref.width  );
			actual.height = Math.max( actual.height, pref.height );
			setActualSize( actual );
			confirmActualSize();
			doLayout();
			return true;
		}
		return false;
	}

	/** interface EvidenceChangeListener
		@since 20030710 */
	public void warning( EvidenceChangeEvent ece )
	{
		//System.out.print( "MonitorImpl.warning()" );
		if( ece.recentEvidenceChangeVariables.contains( myDFV ) )
		{
			//System.out.print( "..." + myDFV );
			int observedIndex = myDFV.getObservedIndex();
			for( Iterator itr = evidLabels.iterator(); itr.hasNext(); )
			{
				((EvidenceLabel) itr.next()).warnEvidence( observedIndex );
			}
		}
		//System.out.println();
	}

	/** @since 20031208 */
	public static void ensureBufferCapacity( BeliefNetwork bn )
	{
		ensureBufferCapacity( bn.getMaxDomainCardinality() );
	}

	/** @since 20031208 */
	public static void ensureBufferCapacity( int capacity )
	{
		if( bufferOdds.length < capacity || bufferPr.length < capacity )
		{
			int newSize = capacity + INT_BUFFER_CAPACITY_EXTENSION;
			//System.out.println( "MonitorImpl buffer capacity increased from " + bufferOdds.length + " to " + newSize + "." );
			bufferOdds	= new double[ newSize ];
			bufferPr	= new double[ newSize ];
		}
	}

	/** interface EvidenceChangeListener
		@since 20030318 */
	public void evidenceChanged( EvidenceChangeEvent ECE )
	{
		if( myDFV.isSampleMode() ) return;
		else if( FLAG_ODDS_NORMALIZE_NETWORK ) throw new IllegalStateException( "Call to evidenceChanged( EvidenceChangeEvent ) invalid when global normalization is on." );
		else evidenceChanged( ECE, Double.NaN );
	}

	/** Exclusively for case of approximated marginal display
		 @since 20040615 */
	public void evidenceChanged( EvidenceChangeEvent ece, double globalMaximumProbability, Table originalMarginal )
	{
		Table approximatedMarginal = evidenceChangedImpl( ece, globalMaximumProbability );
		double[] dataOriginal = originalMarginal.dataclone();
		double[] dataApproximate = approximatedMarginal.dataclone();
		double kl = Prob.kl( dataOriginal, dataApproximate );
		double dist = Prob.distance( dataOriginal, dataApproximate );
		boolean flip = ApproxEngine.flipsOutput( dataOriginal, dataApproximate );

		myLabelKL.setText( STR_CAPTION_KL + Double.toString( kl ) );
		myLabelDist.setText( STR_CAPTION_DIST + Double.toString( dist ) );

/*
		ApproxResult result = myApproxEngine.getApproxResult( myDFV );
		if( result != null ){
			initPanelFixedPoint();
			boolean converges = result.converges();
			int loops = result.getConvLoops();
			double[] fixedPoint = result.getFixedPoint();
			FiniteVariable varY = result.getParent();
			FiniteVariable varX = result.getChild();
			InferenceEngine control = myApproxEngine.getControlInferenceEngine();
			double[] yConditional = control.conditional( varY ).dataclone();
			double yKL = Prob.kl( yConditional, fixedPoint );
			double yConditionalENT = ApproxEngine.ent(yConditional);
			double[] origTheta = result.getOrigTheta();
			FiniteVariable[] family = result.getFamily();
			double[] yuxConditional = control.familyConditional(varX).permute(family).dataclone();
			double yConditionalKL = ApproxEngine.kl(origTheta,myApproxEngine.approxCPT(origTheta, yConditional),yuxConditional);
			double[] newTheta = result.getNewTheta();
			double fixedPointKL = ApproxEngine.kl( origTheta, newTheta, yuxConditional );
			myLabelConverges.setText( STR_CAPTION_CONVERGES + Boolean.toString( converges ) );
			myLabelConvergeLoops.setText( STR_CAPTION_LOOPS + Integer.toString( loops ) );
			myLabelFixedPoint.setText( STR_CAPTION_FIXEDPOINT + ApproxEngine.conditionalToString( fixedPoint ) );
			myLabelYKL.setText( STR_CAPTION_YKL + Double.toString( yKL ) );
			myLabelYConditionalENT.setText( STR_CAPTION_YCONDITIONALENT + Double.toString( yConditionalENT ) );
			myLabelYConditionalKL.setText( STR_CAPTION_YCONDITIONALKL + Double.toString( yConditionalKL ) );
			myLabelYConditionalENTPlusKL.setText( STR_CAPTION_YCONDITIONALENTPLUSKL + Double.toString( yConditionalENT + yKL ) );
			myLabelFixedPointKL.setText( STR_CAPTION_FIXEDPOINTKL + Double.toString( fixedPointKL ) );
		}
*/
		String title = STR_TITLE_APPROX;
		if( flip ) title += STR_TITLE_FLIP;
		setTitle( title );
		setBackground( flip ? COLOR_FLIP_TRUE : COLOR_FLIP_FALSE );
	}

	/** @since 20040615 */
	public void evidenceChanged( EvidenceChangeEvent ece, double globalMaximumProbability ){
		evidenceChangedImpl( ece, globalMaximumProbability );
	}

	public Table evidenceChangedImpl( EvidenceChangeEvent ece, double globalMaximumProbability )
	{
		//if( NetworkInternalFrame.DEBUG_CPTCHANGED )
		//System.out.println( "MonitorImpl.evidenceChanged( "+ece+", "+globalMaximumProbability+" )" );

		if( myDFV.isSampleMode() ) return (Table)null;

		Table tbl = null;
		if( myApproxEngine != null ){
			ApproxReport report = myApproxEngine.getLatestReport();

			tbl = report.getConditional(myDFV);
			if(   TABLES.length != 1 ){ TABLES = new Table[]{ tbl }; }
			else{ TABLES[0] = tbl; }
		}
		else{
			InferenceEngine ie = myDFV.getInferenceEngine();
			if( ie != null ){ TABLES = ie.conditionals( myDFV, TABLES ); }
			if( myApproxMonitor != null ){ myApproxMonitor.evidenceChanged( ece, globalMaximumProbability, tbl ); }
			tbl = TABLES[0];
		}

		int observedIndex = myDFV.getObservedIndex();

		EvidenceLabel el;
		Iterator itr = evidLabels.iterator();
		el = (EvidenceLabel) itr.next();

		if( FLAG_ODDS_NORMALIZE && el.getFormatManager() == EvidenceLabel.ODDS_MANAGER )
		{
			double max = Double.NEGATIVE_INFINITY;
			double pr;
			double odds;
			int i;
			int size = myDFV.size();
			for( i=0; i<size; i++ )
			{
				pr = tbl.getCP( i );
				odds = pr / ((double)1 - pr);
				if( odds > max ) max = odds;
				bufferOdds[i] = odds;
				bufferPr[i] = pr;
			}

			double normal = (double)1;

			if( FLAG_ODDS_NORMALIZE_NETWORK )
			{
				if( 0 < globalMaximumProbability && globalMaximumProbability < 1 )
				{
					normal = globalMaximumProbability / ((double)1 - globalMaximumProbability);
					//System.out.println( "MonitorImpl.evidenceChanged() using global normal " + normal );
				}
			}
			else normal = max;

			i=0;
			el.drawOdds( bufferOdds[i], normal, observedIndex, bufferPr[i] );
			++i;
			while( itr.hasNext() )
			{
				el = (EvidenceLabel) itr.next();
				el.drawOdds( bufferOdds[i], normal, observedIndex, bufferPr[i] );
				++i;
			}
		}
		else
		{
			el.drawEvidence( TABLES, observedIndex );
			while( itr.hasNext() )
			{
				el = (EvidenceLabel) itr.next();
				el.drawEvidence( TABLES, observedIndex );
			}
		}

		if( ece == null || ece.recentEvidenceChangeVariables.contains( myDFV ) ) pack();
		repaint();

		return tbl;
	}

	/** Allow options to change.*/
	public void changePackageOptions()
	{
		updatePreferences();
	}

	/** Call this method to ask a PreferenceListener to
		respond to RECENT changes in the COMMITTED values */
	public void updatePreferences()
	{
		for( Iterator itr = evidLabels.iterator(); itr.hasNext(); ) {
			((EvidenceLabel)itr.next()).updatePreferences();
		}

		if( myApproxEngine == null ){
			setTitle( myDFV.toString() );
		}

		if( myApproxMonitor != null ) myApproxMonitor.updatePreferences();

		BUNDLE_OF_PREFERENCES.previewPreferences( MonitorImpl.this );
	}
	/** Call this method to ask a PreferenceListener to
		respond to RECENT changes in the EDITED values */
	public void previewPreferences()
	{
		for( Iterator itr = evidLabels.iterator(); itr.hasNext(); ) {
			((EvidenceLabel)itr.next()).previewPreferences();
		}

		BUNDLE_OF_PREFERENCES.previewPreferences( MonitorImpl.this );
	}
	/** Call this method to force a PreferenceListener to
		reset itself */
	public void setPreferences()
	{
		for( Iterator itr = evidLabels.iterator(); itr.hasNext(); ) {
			((EvidenceLabel)itr.next()).setPreferences();
		}

		BUNDLE_OF_PREFERENCES.setPreferences( MonitorImpl.this );
	}

	public void setDoZoom( boolean newFlag, boolean force )
	{
		if( force || (myFlagDoZoom != newFlag) ){
			if( newFlag ){ mySizeTransformer = mySizeTransformerOriginal; }
			else{
				NetworkInternalFrame nif = myDFV.getNetworkInternalFrame();
				if( nif != null ){
					NetworkDisplay display = nif.getNetworkDisplay();
					mySizeTransformer = display.getMonitorSizeManager();
				}
				else{ mySizeTransformer = mySizeTransformerOriginal; }
			}

			myFlagDoZoom = newFlag;
			recalculateActual();
		}
	}

	public static TargetedBundle validatePreferenceBundle( SamiamPreferences prefs ){
		if( BUNDLE_OF_PREFERENCES != null ){ return BUNDLE_OF_PREFERENCES.validate( prefs ); }

		String   key  = STR_KEY_PREFERENCE_BUNDLE;
		String[] keys = new String[] { SamiamPreferences.evidDlgZooms, SamiamPreferences.evidDlgZoomFactor, SamiamPreferences.evidDlgRectSize, SamiamPreferences.evidDlgMinimumFractionDigits, SamiamPreferences.evidDlgView };

		BUNDLE_OF_PREFERENCES = new TargetedBundle( key, keys, prefs ){
			public void begin( Object me, boolean force ){
				this.monitorimpl = (MonitorImpl) me;
				this.force       = force;
				this.pack        = false;
			}

			public void setPreference( int index, Object value ){
				switch( index ){
					case 0:
						monitorimpl.setDoZoom( ((Boolean)value).booleanValue(), this.force );
						break;
					case 1:
						monitorimpl.recalculateActual();
						break;
					case 2:
					case 3:
					case 4:
						this.pack = true;
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			public void finish(){
				if( pack ) monitorimpl.pack();
			}

			private MonitorImpl monitorimpl;
			private boolean     pack;
			private boolean     force;
		};

		return BUNDLE_OF_PREFERENCES;
	}

	public void		setVirtualLocation( Point p )
	{
		myVirtualLocation.setLocation( p );
		theUtilPoint.setLocation( p );
		myPositionTransformer.virtualToActual( theUtilPoint );
		setActualLocation( theUtilPoint );
	}

	public Point		getVirtualLocation( Point p )
	{
		if( p == null ) p = new Point();
		p.setLocation( myVirtualLocation );
		return p;
	}

	public void		setActualLocation( Point p )
	{
		super.setLocation( p );
	}

	public Point		getActualLocation( Point p )
	{
		return super.getLocation( p );
	}

	public void		confirmActualLocation()
	{
		//System.out.println( "(MonitorImpl)"+getText()+".confirmActualLocation()" );
		getActualLocation( theUtilPoint );
		//System.out.println( "\tactual loc:"+theUtilPoint );
		myPositionTransformer.actualToVirtual( theUtilPoint );
		//System.out.println( "\tvirtual loc:"+theUtilPoint );
		myVirtualLocation.setLocation( theUtilPoint );
	}

	public void		translateActualLocation( int deltaX, int deltaY )
	{
		getActualLocation( theUtilPoint );
		theUtilPoint.x += deltaX;
		theUtilPoint.y += deltaY;
		super.setLocation( theUtilPoint );
	}

	public void		setVirtualSize( Dimension d )
	{
		myVirtualSize.setSize( d );
		theUtilDimension.setSize( d );
		//if( myFlagDoZoom ) mySizeTransformer.virtualToActual( theUtilDimension );
		mySizeTransformer.virtualToActual( theUtilDimension );
		setActualSize( theUtilDimension );
	}
	public Dimension	getVirtualSize( Dimension d )
	{
		if( d == null ) d = new Dimension();
		d.setSize( myVirtualSize );
		return d;
	}

	public void		setActualSize( Dimension d )
	{
		setSize( d );
	}

	/** @since 20040811 */
	public void		hintScale( float scale ){}

	public Dimension	getActualSize( Dimension d )
	{
		return getSize( d );
	}

	public void		confirmActualSize()
	{
		getActualSize( theUtilDimension );
		//if( myFlagDoZoom ) mySizeTransformer.actualToVirtual( theUtilDimension );
		mySizeTransformer.actualToVirtual( theUtilDimension );
		myVirtualSize.setSize( theUtilDimension );
	}

	/** @deprecated */
	public void		setActualScale( double factor )
	{
		//theUtilDimension.setSize( myVirtualSize );
		//theUtilDimension.width *= factor;
		//theUtilDimension.height *= factor;
		//setActualSize( theUtilDimension );

		//theUtilPoint.setLocation( myVirtualLocation );
		//theUtilPoint.x *= factor;
		//theUtilPoint.y *= factor;
		//setActualLocation( theUtilPoint );
	}

	public void		recalculateActual()
	{
		//System.out.println( "(MonitorImpl)"+getTitle()+".recalculateActual()...myFlagDoZoom=" + myFlagDoZoom );

		//if( myFlagDoZoom )
		//{
			float sizeFontActual = mySizeTransformer.virtualToActual( EvidenceLabel.FLOAT_BASE_FONT_SIZE );
			Font fontLabel = myLabelTitle.getFont().deriveFont( sizeFontActual );
			myLabelTitle.setFont( fontLabel );
			if( myCollectionLabels != null ){
				for( Iterator it = myCollectionLabels.iterator(); it.hasNext(); ){
					((JComponent)it.next()).setFont( fontLabel );
				}
			}

			for( Iterator it = evidLabels.iterator(); it.hasNext(); )
			{
				((EvidenceLabel)it.next()).recalculateActual( mySizeTransformer );
			}
		//}

		theUtilDimension.setSize( myVirtualSize );
		//System.out.println( "\tvirtual size:"+theUtilDimension );
		//if( myFlagDoZoom ) mySizeTransformer.virtualToActual( theUtilDimension );
		mySizeTransformer.virtualToActual( theUtilDimension );
		//System.out.println( "\tactual size:"+theUtilDimension );
		setActualSize( theUtilDimension );

		theUtilPoint.setLocation( myVirtualLocation );
		//System.out.println( "\tvirtual loc:"+theUtilPoint );
		myPositionTransformer.virtualToActual( theUtilPoint );
		//System.out.println( "\tactual  loc:"+theUtilPoint );
		setActualLocation( theUtilPoint );

		if( myApproxMonitor != null ) myApproxMonitor.recalculateActual();
	}

	/** Will modify pt and then return it, so no new allocations take place.*/
	public Point getActualCenter( Point pt )
	{
		if( pt == null ) pt = new Point();
		getActualLocation( pt );
		pt.translate( getWidth()/2, getHeight()/2 );
		return pt;
	}
}
