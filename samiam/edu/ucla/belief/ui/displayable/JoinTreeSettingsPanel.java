package edu.ucla.belief.ui.displayable;

import il2.inf.structure.JoinTreeStats;
import edu.ucla.belief.*;
import edu.ucla.belief.inference.*;
import edu.ucla.belief.tree.*;
import edu.ucla.util.AbstractStringifier;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.util.ResizeAdapter;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.clipboard.ClipboardHelper;

import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.text.*;
import java.lang.ref.SoftReference;
import java.math.BigInteger;

/**
	Based on edu.ucla.belief.ui.recursiveconditioning.SettingsPanel

	@author Keith Cascio
	@since 092303
*/
public class JoinTreeSettingsPanel extends JPanel implements ActionListener, Dynamator.Commitable//, ChangeListener, MenuListener
{
	public JoinTreeSettingsPanel()
	{
		init();
	}

	/** interface Dynamator.Commitable
		@since 020405 */
	public void commitChanges()
	{
		if( myActualSettings != null && myVirtualSettings != null )
		{
			//System.out.println( "JoinTreeSettingsPanel.commitChanges()" );
			myActualSettings.copy( myVirtualSettings );
			//System.out.println( "\t myActualSettings.getJoinTree() " + myActualSettings.getJoinTree() );
		}
	}

	/** interface Dynamator.Commitable
		@since 020405 */
	public JComponent asJComponent(){
		return (JComponent)this;
	}

	/** interface Dynamator.Commitable
		@since 020405 */
	public void copyToSystemClipboard(){
		StringBuffer buffer = new StringBuffer( 256 );
		buffer.append( "Compile settings - "+myDefaultGenerator.getDisplayName()+"\n" );

		buffer.append( "Elimination heuristic: " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myComboHeuristics );

 		buffer.append( "\nMax clique size (normalized): " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myLabelMaxClique );

		buffer.append( "\nMax separator size (normalized): " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myLabelMaxSep );

		buffer.append( "\n# cluster entries: " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myLabelNumCluster );

		buffer.append( "\n# separator entries: " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myLabelNumSep );

		buffer.append( "\nElimination order: " );
		edu.ucla.belief.ui.rc.CacheSettingsPanel.append( buffer, myFieldOrder );

		Util.copyToSystemClipboard( buffer.toString() );
	}

	public void setNetworkInternalFrame( NetworkInternalFrame hnInternalFrame, DefaultGenerator dg )
	{
		if( hnInternalFrame != myNetworkInternalFrame )
		{
			myNetworkInternalFrame = hnInternalFrame;
			set( myNetworkInternalFrame.getBeliefNetwork() );
			myDefaultGenerator = dg;
		}

		JoinTreeSettings actual = dg.getSettings( dg.choosePropertySuperintendent( myBeliefNetwork ) );
		set( actual );
	}

	public void set( DisplayableBeliefNetwork bn )
	{
		myBeliefNetwork = bn;
	}

	protected void set( JoinTreeSettings actual )
	{
		//System.out.println( "JoinTreeSettingsPanel.set( "+actual+" )" );
		myActualSettings = actual;

		myVirtualSettings = new JoinTreeSettings();
		myVirtualSettings.setDebugID( "VirtualSettings" + String.valueOf( COUNTER++ ) );
		myVirtualSettings.copy( myActualSettings );
		//myVirtualSettings.addChangeListener( this );

		//System.out.println( "myActualSettings.getEngine() == " + myActualSettings.getEngine() );
		//System.out.println( "myVirtualSettings.getEngine() == " + myVirtualSettings.getEngine() );
		myFlagListenerComboHeuristics = false;
		myComboHeuristics.setSelectedItem( myVirtualSettings.getEliminationHeuristic() );
		myFlagListenerComboHeuristics = true;

		JoinTreeInferenceEngine jtie = myVirtualSettings.getEngine();
		try{
			if( (jtie != null) && (! jtie.getValid()) ){ jtie = null; }
		}catch( Throwable thrown ){
			jtie = null;
			System.err.println( "warning: JoinTreeSettingsPanel.set() caught " + thrown );
		}
		il2.inf.structure.JTUnifier jointree = myVirtualSettings.getJoinTree();

		setJoinTreeStats( ( jtie == null ) ? jointree : jtie.getJoinTreeStats() );

		JoinTreeSettingsPanel.this.displayOrder( jointree );

		setPreviewEnabled( jointree, jtie );
	}

	protected void setJoinTreeStats( JoinTreeStats.StatsSource stats )
	{
		//System.out.println( "JoinTreeSettingsPanel.setJoinTreeStats( "+stats+" )" );
		String txtMaxClique = STR_MAX_CLIQUE_NOT_APPLICABLE;
		String txtMaxSep = STR_MAX_CLIQUE_NOT_APPLICABLE;
		String txtNumCluster = STR_MAX_CLIQUE_NOT_APPLICABLE;
		String txtNumSep = STR_MAX_CLIQUE_NOT_APPLICABLE;

		if( stats != null )
		{
			//double[] statsCluster = stats.getClusterStats();
			//txtMaxClique = theNumberFormat.format( statsCluster[0] );
			//txtNumCluster = theNumberFormat.format( statsCluster[1] );
			//double[] statsSeparator = stats.getSeparatorStats();
			//txtMaxSep = theNumberFormat.format( statsSeparator[0] );
			//txtNumSep = theNumberFormat.format( statsSeparator[1] );

			//BigInteger[] statsCluster = stats.getClusterStats();
			//txtMaxClique = statsCluster[0].toString();
			//txtNumCluster = statsCluster[1].toString();
			//BigInteger[] statsSeparator = stats.getSeparatorStats();
			//txtMaxSep = statsSeparator[0].toString();
			//txtNumSep = statsSeparator[1].toString();

			JoinTreeStats.Stat statsCluster = stats.getClusterStats();
			txtMaxClique = theNumberFormat.format( statsCluster.getNormalizedMax() );
			//txtNumCluster = statsCluster.getTotal().toString();
			txtNumCluster = JoinTreeStats.toStringGrouped( statsCluster.getTotal() );
			JoinTreeStats.Stat statsSeparator = stats.getSeparatorStats();
			txtMaxSep = theNumberFormat.format( statsSeparator.getNormalizedMax() );
			//txtNumSep = statsSeparator.getTotal().toString();
			txtNumSep = JoinTreeStats.toStringGrouped( statsSeparator.getTotal() );

			//System.out.println( "clust ungrouped: " + statsCluster.getTotal().toString() );
			//System.out.println( "clust grouped  : " + JoinTreeStats.toStringGrouped( statsCluster.getTotal() ) );
			//System.out.println( "sep ungrouped  : " + statsSeparator.getTotal().toString() );
			//System.out.println( "sep grouped    : " + JoinTreeStats.toStringGrouped( statsSeparator.getTotal() ) );
		}

		myLabelMaxClique.setText( txtMaxClique );
		myLabelNumCluster.setText( txtNumCluster );
		myLabelMaxSep.setText( txtMaxSep );
		myLabelNumSep.setText( txtNumSep );

		if( stats != null ) packAncestor();
	}

	/** @since 012004 */
	private void setJoinTreeStatsText( String text )
	{
		if( text == null ) text = STR_MAX_CLIQUE_NOT_APPLICABLE;
		myLabelMaxClique.setText( text );
		myLabelNumCluster.setText( text );
		myLabelMaxSep.setText( text );
		myLabelNumSep.setText( text );
	}

	public void setParentWindow( Window parent )
	{
		myParent = parent;
	}

	public void doPreview()
	{
		//myDefaultGenerator.unregisterInferenceEngine( myTempEngine );
		myTempEngine = null;

		if( myBeliefNetwork.isEmpty() )
		{
			setJoinTreeStatsText( STR_MAX_CLIQUE_ZERO );
			myButtonPreview.setEnabled( false );
			return;
		}

		//InferenceEngine manufactured = null;
		//JoinTreeInferenceEngine jtie = null;
		il2.inf.structure.JTUnifier jointree = null;

		try{
			//manufactured = myDefaultGenerator.manufactureInferenceEngine( myBeliefNetwork, myVirtualSettings );
			//if( myVirtualSettings.getEngine() != manufactured ) throw new RuntimeException( "myVirtualSettings.getEngine() != manufactured" );
			//jtie = myVirtualSettings.getEngine();
			//setJoinTreeStats( ( jtie == null ) ? null : jtie.getJoinTreeStats() );
			//myTempEngine = new SoftReference( manufactured );
			//setPreviewEnabled( jtie );
			jointree = myDefaultGenerator.manufactureJoinTree( myBeliefNetwork, myVirtualSettings );
			if( myVirtualSettings.getJoinTree() != jointree ) throw new RuntimeException( "myVirtualSettings.getJoinTree() != jointree" );
			setJoinTreeStats( jointree );
			JoinTreeSettingsPanel.this.displayOrder( jointree );
			setPreviewEnabled( jointree );
		}catch( Exception e ){
			System.err.println( e );
			showError( "Failed to create join tree. See stderr." );
		}catch( OutOfMemoryError e ){
			UI ui = (myNetworkInternalFrame == null) ? (UI)null : myNetworkInternalFrame.getParentFrame();
			String invoker = (ui == null) ? (String)null: ui.getInvokerName();
			showError( "Failed to create join tree. Out of memory.\n" + Util.makeOutOfMemoryMessage( invoker ) );
		}finally{
			//if( manufactured == null || jtie == null ){
			if( jointree == null ){
				myTempEngine = null;
				setJoinTreeStatsText( STR_MAX_CLIQUE_NOT_APPLICABLE );
				setPreviewEnabled( jointree );//myButtonPreview.setEnabled( true );
			}
		}
	}

	/** @since 020405 */
	private void packAncestor(){
		//Container container = this.getTopLevelAncestor();
		//if( container instanceof java.awt.Window ){
		//	((java.awt.Window)container).pack();
		//}
		myParent = SwingUtilities.windowForComponent( this );
		if( myParent != null ){
			massagePreferredSize();
			myParent.pack();
			//if( myParent instanceof JDialog )
			//	System.out.println( "packing " + ((JDialog)myParent).getTitle() );
		}
	}

	public static final int INT_WIDTH_BUFFER = (int)32;

	/** @since 020405 */
	private void massagePreferredSize(){
		Dimension dim = this.getLayout().preferredLayoutSize( (Container)this );
		//dim.width += INT_WIDTH_BUFFER;

		myGridBagInner.invalidateLayout( (Container)myPanelInner );
		myGridBagMain.invalidateLayout( (Container)myPanelMain );
		myGridBagInner.layoutContainer( (Container)myPanelInner );
		myGridBagMain.layoutContainer( (Container)myPanelMain );

		Dimension diminner = myGridBagInner.preferredLayoutSize( (Container)myPanelInner );
		dim.width = diminner.width + INT_WIDTH_BUFFER;

		this.setPreferredSize( dim );
	}

	/** @since 012804 */
	private void showError( String msg )
	{
		if( myNetworkInternalFrame == null ) System.err.println( msg );
		else myNetworkInternalFrame.getParentFrame().showErrorDialog( msg );
	}

	private void setPreviewEnabled( JoinTreeInferenceEngine jtie )
	{
		myButtonPreview.setEnabled( jtie == null || !jtie.getValid() );
	}

	/** @since 012904 */
	private void setPreviewEnabled( il2.inf.structure.JTUnifier jointree )
	{
		myButtonPreview.setEnabled( jointree == null );
	}

	/** @since 012904 */
	private void setPreviewEnabled( il2.inf.structure.JTUnifier jointree, JoinTreeInferenceEngine jtie )
	{
		myButtonPreview.setEnabled( ( jtie == null ) ? (jointree == null) : !jtie.getValid() );
	}

	protected void init()
	{
		myPanelMain = new JPanel( myGridBagMain = new GridBagLayout() );

		myComboHeuristics = new JComboBox( EliminationHeuristic.ARRAY );
		myComboHeuristics.addActionListener( this );

		myButtonPreview = new JButton( "Preview" );
		myButtonPreview.addActionListener( this );

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.WEST;
		myPanelMain.add( new JLabel( "Elimination heuristic: " ), c );

		myPanelMain.add( Box.createHorizontalStrut( 8 ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelMain.add( myComboHeuristics, c );

		myPanelMain.add( Box.createVerticalStrut( 8 ), c );

		myPanelMain.add( myButtonPreview, c );

		myPanelMain.add( Box.createVerticalStrut( 8 ), c );

		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelMain.add( makeJoinTreeStatsPanel(), c );

		myResizePanel = this;
		myResizePanel.add( myPanelMain );
		myResizeAdapter = new ResizeAdapter( myPanelMain );
		myResizePanel.addComponentListener( myResizeAdapter );
		Dimension minSize = myPanelMain.getPreferredSize();
		minSize.width += 70;
		minSize.height += 20;
		setMinimumSize( minSize );
		setPreferredSize( minSize );

		//myResizePanel.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );
		//myPanelMain.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
	}

	protected JComponent makeJoinTreeStatsPanel()
	{
		myPanelInner = new JPanel( myGridBagInner = new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		JLabel lbl = null;
		Component strut = null;

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		myPanelInner.add( lbl = new JLabel( "Max clique size (normalized): ", JLabel.LEFT ), c );

		myPanelInner.add( strut = Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelInner.add( myLabelMaxClique = new JLabel( "?", JLabel.RIGHT ), c );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		myPanelInner.add( lbl = new JLabel( "Max separator size (normalized): ", JLabel.LEFT ), c );

		myPanelInner.add( strut = Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelInner.add( myLabelMaxSep = new JLabel( "?", JLabel.RIGHT ), c );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		myPanelInner.add( lbl = new JLabel( "# cluster entries: ", JLabel.LEFT ), c );

		myPanelInner.add( strut = Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelInner.add( myLabelNumCluster = new JLabel( "?", JLabel.RIGHT ), c );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		myPanelInner.add( lbl = new JLabel( "# separator entries: ", JLabel.LEFT ), c );

		myPanelInner.add( strut = Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelInner.add( myLabelNumSep = new JLabel( "?", JLabel.RIGHT ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		myPanelInner.add( new JLabel( "Elimination order:", JLabel.LEFT ), c );

		myFieldOrder = new JTextField( "?" );
		myFieldOrder.setToolTipText( "?" );
		myFieldOrder.setEditable( false );
		new ClipboardHelper( myFieldOrder, myFieldOrder );
		//myFieldOrder.setScrollOffset( 0 );
		Dimension dim = myFieldOrder.getPreferredSize();
		dim.width = 256;
		myFieldOrder.setPreferredSize( dim );
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myPanelInner.add( myFieldOrder, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		myPanelInner.add( strut = Box.createVerticalStrut( 4 ), c );

		Border outsideBorder = BorderFactory.createEtchedBorder();
		Border insideBorder = BorderFactory.createEmptyBorder( 0, 16, 0, 16 );
		Border compoundBorder = BorderFactory.createCompoundBorder( outsideBorder, insideBorder);

		myPanelInner.setBorder( compoundBorder );

		return myPanelInner;
	}

	/** @since 20060224 */
	private void displayOrder( il2.inf.structure.JTUnifier jointree ){
		String displayText = STR_UNKNOWN;
		if( jointree != null ){
			List order = jointree.eliminationOrder();
			if( order != null ){
				displayText = AbstractStringifier.VARIABLE_ID.collectionToString( order );
			}
		}
		myFieldOrder.setText( displayText );
		myFieldOrder.setToolTipText( displayText );

		if( displayText == STR_UNKNOWN ) return;

		Runnable scrollFixer = new Runnable(){
			public void run(){
				try{
					Thread.sleep( 1000 );
				}catch( InterruptedException interruptedexception ){
					return;
				}
				if( myFieldOrder  != null )  myFieldOrder.setScrollOffset(0);
			}
		};
		new Thread( scrollFixer, "jointreesettingspanel jtextfield scroll adjustment" ).start();
	}

	public void actionPerformed( ActionEvent e )
	{
		Object src = e.getSource();
		if( src == myComboHeuristics && myFlagListenerComboHeuristics ){
			EliminationHeuristic hOld = myVirtualSettings.getEliminationHeuristic();
			EliminationHeuristic hNew = (EliminationHeuristic) myComboHeuristics.getSelectedItem();
			if( hNew != hOld )
			{
				myVirtualSettings.setEliminationHeuristic( hNew );
				setJoinTreeStats( null );
				myButtonPreview.setEnabled( true );
			}
		}
		else if( src == myButtonPreview ) doPreview();
	}

	protected JLabel myLabelMaxClique;
	protected JLabel myLabelMaxSep;
	protected JLabel myLabelNumCluster;
	protected JLabel myLabelNumSep;
	protected JButton myButtonPreview;
	protected JTextField myFieldOrder;

	protected JComboBox myComboHeuristics;
	private boolean myFlagListenerComboHeuristics = true;
	protected JPanel myPanelMain, myPanelInner;
	private GridBagLayout myGridBagInner, myGridBagMain;
	protected JComponent myResizePanel;
	protected ComponentListener myResizeAdapter;
	private Window myParent;

	protected JoinTreeSettings myActualSettings;
	protected JoinTreeSettings myVirtualSettings;
	protected ActionListener myActionListener;
	protected NetworkInternalFrame myNetworkInternalFrame;
	protected DefaultGenerator myDefaultGenerator;
	protected DisplayableBeliefNetwork myBeliefNetwork;
	private SoftReference myTempEngine;

	protected static int COUNTER = (int)0;

	public static boolean FLAG_DEBUG_BORDERS = false;
	public static boolean FLAG_TEST = false;

	public static final String STR_MAX_CLIQUE_NOT_APPLICABLE = "        ;        ";
	public static final String STR_MAX_CLIQUE_ZERO           = "        0        ";
	public static final String STR_UNKNOWN                   = "unknown";

	private static final NumberFormat theNumberFormat = new DecimalFormat( "0.##" );
	public static final int INT_SIZE_STRUT = (int)8;
}
