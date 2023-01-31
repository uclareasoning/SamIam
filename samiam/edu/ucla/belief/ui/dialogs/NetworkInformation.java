package edu.ucla.belief.ui.dialogs;

import il2.inf.structure.JoinTreeStats;
import il2.util.*;
import edu.ucla.belief.*;
import edu.ucla.belief.inference.JoinTreeInferenceEngine;
import edu.ucla.belief.tree.*;
import edu.ucla.belief.ui.displayable.DisplayableStats;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.util.JOptionResizeHelper;

import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.Iterator;

/**
	@author Keith Cascio
	@since 102402
*/
public class NetworkInformation implements ActionListener
{
	public NetworkInformation()
	{
		//myNumberFormat = new DecimalFormat( "0.##" );
	}

	/** @since 120804 */
	public void copyToSystemClipboard(){
		Util.copyToSystemClipboard( this.toString() );
	}

	/** @since 120804 */
	public String toString(){
		StringBuffer buff = new StringBuffer( 512 );
		buff.append( myLabelNumNodes.getText() );
		buff.append( " nodes, " );
		buff.append( myLabelNumEdges.getText() );
		buff.append( " edges.\n" );//22
		buff.append( myStatsCardinality.toString() );
		buff.append( "\n" );
		buff.append( myStatsCPTSize.toString() );
		return buff.toString();
	}

	/** @since 120804 */
	public void actionPerformed( ActionEvent e ){
		Object src = e.getSource();
		if( src == myButtonCopy ) copyToSystemClipboard();
	}

	private JButton myButtonCopy;
	public static final String STR_OPTION_OK = "OK";
	private static final Object[] ARRAY_OPTIONS = new Object[] { null, STR_OPTION_OK };

	/** @since 021405 Valentine's Day! */
	public void showDialog( Component parent, BeliefNetwork bn, JOptionResizeHelper.JOptionResizeHelperListener listener ){
		this.showDialog( bn, (InferenceEngine)null, "Network Information", parent, listener );
	}

	public void showDialog( BeliefNetwork bn, InferenceEngine ie, String title, Component parent ){
		this.showDialog( bn, ie, title, parent, (JOptionResizeHelper.JOptionResizeHelperListener)null );
	}

	public void showDialog( BeliefNetwork bn, InferenceEngine ie, String title, Component parent, JOptionResizeHelper.JOptionResizeHelperListener listener )
	{
		if( myButtonCopy == null ){
			myButtonCopy = new JButton( "Copy" );
			myButtonCopy.addActionListener( (ActionListener)this );
			ARRAY_OPTIONS[0] = myButtonCopy;
		}

		JComponent info = getJComponent( bn, ie );

		if( listener != null ){
			JOptionResizeHelper helper = new JOptionResizeHelper( info, true, (long)10000 );
			helper.setListener( listener );
			helper.start();
		}

		//JOptionPane.showMessageDialog( parent, getJComponent( bn, ie ), title, JOptionPane.PLAIN_MESSAGE );
		//showOptionDialog(Component parentComponent,Object message,String title,int optionType,int messageType,Icon icon,Object[] options,Object initialValue)
		JOptionPane.showOptionDialog( parent, info, title, JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, (Icon)null, ARRAY_OPTIONS, STR_OPTION_OK );
	}

	public void showDialog( BeliefNetwork bn, InferenceEngine ie, Component parent )
	{
		showDialog( bn, ie, "Network Information", parent );
	}

	public JComponent getJComponent( BeliefNetwork bn, InferenceEngine ie )
	{
		if( myInfoPanel == null ) makeInfoPanel();
		myLabelNumNodes.setText( Integer.toString( bn.size() ) );
		myLabelNumEdges.setText( Integer.toString( bn.numEdges() ) );
		if( myLabelMinDomainCard != null ) myLabelMinDomainCard.setText( Integer.toString( bn.getMinDomainCardinality() ) );
		if( myLabelMaxDomainCard != null ) myLabelMaxDomainCard.setText( Integer.toString( bn.getMaxDomainCardinality() ) );
		if( myLabelMinTheoreticalCPTSize != null ) myLabelMinTheoreticalCPTSize.setText( Integer.toString( bn.getMinTheoreticalCPTSize() ) );
		if( myLabelMaxTheoreticalCPTSize != null ) myLabelMaxTheoreticalCPTSize.setText( Integer.toString( bn.getMaxTheoreticalCPTSize() ) );
		setStats( bn );
		//if( ie.canonical() instanceof JoinTreeInferenceEngine ) setJoinTreeStats( ((JoinTreeInferenceEngine) ie.canonical()).getJoinTreeStats() );
		//else clearJoinTreeStats();
		return myInfoPanel;
	}

	/** @author Keith Cascio
		@since 110704 */
	private void setStats( BeliefNetwork bn )
	{
		IntList cards = new IntList( bn.size() );
		IntList cptsizes = new IntList( bn.size() );

		FiniteVariable fVar;
		for( Iterator it = bn.iterator(); it.hasNext(); ){
			fVar = (FiniteVariable) it.next();
			cards.add( fVar.size() );
			cptsizes.add( bn.getTheoreticalCPTSize( fVar ) );
		}

		myStatsCardinality.set( cards.calculateStats() );
		myStatsCPTSize.set( cptsizes.calculateStats() );
	}

	/** @author Keith Cascio
		@since 032503 */
	/*
	protected void clearJoinTreeStats()
	{
		setJoinTreeStats( null );
	}

	protected void setJoinTreeStats( JoinTreeStats stats )
	{
		String txtMaxClique = STR_MAX_CLIQUE_NOT_APPLICABLE;
		String txtMaxSep = STR_MAX_CLIQUE_NOT_APPLICABLE;
		String txtNumCluster = STR_MAX_CLIQUE_NOT_APPLICABLE;
		String txtNumSep = STR_MAX_CLIQUE_NOT_APPLICABLE;

		if( stats != null )
		{
			double[] statsCluster = stats.getClusterStats();
			txtMaxClique = myNumberFormat.format( statsCluster[0] );
			txtNumCluster = myNumberFormat.format( statsCluster[1] );
			double[] statsSeparator = stats.getSeparatorStats();
			txtMaxSep = myNumberFormat.format( statsSeparator[0] );
			txtNumSep = myNumberFormat.format( statsSeparator[1] );
		}

		myLabelMaxClique.setText( txtMaxClique );
		myLabelNumCluster.setText( txtNumCluster );
		myLabelMaxSep.setText( txtMaxSep );
		myLabelNumSep.setText( txtNumSep );
	}

	protected NumberFormat myNumberFormat;
	protected JLabel myLabelMaxClique = null;
	protected JLabel myLabelMaxSep = null;
	protected JLabel myLabelNumCluster = null;
	protected JLabel myLabelNumSep = null;*/

	public static String STR_MAX_CLIQUE_NOT_APPLICABLE = "n/a";

	protected JComponent myInfoPanel = null;
	protected JLabel myLabelNumNodes = null;
	protected JLabel myLabelNumEdges = null;
	protected JLabel myLabelMinDomainCard = null;
	protected JLabel myLabelMaxDomainCard = null;
	protected JLabel myLabelMinTheoreticalCPTSize = null;
	protected JLabel myLabelMaxTheoreticalCPTSize = null;
	private DisplayableStats myStatsCardinality;
	private DisplayableStats myStatsCPTSize;

	protected void makeInfoPanel()
	{
		if( myInfoPanel != null ) return;

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		JPanel pnlInner = new JPanel( gridbag );

		JLabel lbl = null;
		Component strut = null;

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		lbl = new JLabel( "Nodes: ", JLabel.LEFT );
		gridbag.setConstraints( lbl, c );
		pnlInner.add( lbl );

		strut = Box.createHorizontalStrut( INT_SIZE_STRUT );
		gridbag.setConstraints( strut, c );
		pnlInner.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myLabelNumNodes = new JLabel( "?", JLabel.RIGHT );
		gridbag.setConstraints( myLabelNumNodes, c );
		pnlInner.add( myLabelNumNodes );
		//myLabelNumNodes.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		lbl = new JLabel( "Edges: ", JLabel.LEFT );
		gridbag.setConstraints( lbl, c );
		pnlInner.add( lbl );

		strut = Box.createHorizontalStrut( INT_SIZE_STRUT );
		gridbag.setConstraints( strut, c );
		pnlInner.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myLabelNumEdges = new JLabel( "?", JLabel.RIGHT );
		gridbag.setConstraints( myLabelNumEdges, c );
		pnlInner.add( myLabelNumEdges );

		/*
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		lbl = new JLabel( "Min domain cardinality: ", JLabel.LEFT );
		gridbag.setConstraints( lbl, c );
		pnlInner.add( lbl );

		strut = Box.createHorizontalStrut( INT_SIZE_STRUT );
		gridbag.setConstraints( strut, c );
		pnlInner.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myLabelMinDomainCard = new JLabel( "?", JLabel.RIGHT );
		gridbag.setConstraints( myLabelMinDomainCard, c );
		pnlInner.add( myLabelMinDomainCard );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		lbl = new JLabel( "Max domain cardinality: ", JLabel.LEFT );
		gridbag.setConstraints( lbl, c );
		pnlInner.add( lbl );

		strut = Box.createHorizontalStrut( INT_SIZE_STRUT );
		gridbag.setConstraints( strut, c );
		pnlInner.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myLabelMaxDomainCard = new JLabel( "?", JLabel.RIGHT );
		gridbag.setConstraints( myLabelMaxDomainCard, c );
		pnlInner.add( myLabelMaxDomainCard );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		pnlInner.add( new JLabel( "Min cpt size (theor.): ", JLabel.LEFT ), c );

		pnlInner.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		pnlInner.add( myLabelMinTheoreticalCPTSize = new JLabel( "?", JLabel.RIGHT ), c );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		pnlInner.add( new JLabel( "Max cpt size (theor.): ", JLabel.LEFT ), c );

		pnlInner.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		pnlInner.add( myLabelMaxTheoreticalCPTSize = new JLabel( "?", JLabel.RIGHT ), c );
		*/

		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		//pnlInner.add( new JLabel( "Domain cardinality:", JLabel.LEFT ), c );
		pnlInner.add( myStatsCardinality = new DisplayableStats( "Domain cardinality:", false ), c );

		//pnlInner.add( new JLabel( "Theoretical CPT size:", JLabel.LEFT ), c );
		pnlInner.add( myStatsCPTSize = new DisplayableStats( "# CPT parameters (theor.):", true ), c );
		myStatsCPTSize.setUnits( "parameters" );

/*
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		lbl = new JLabel( "Max clique size (normalized): ", JLabel.LEFT );
		gridbag.setConstraints( lbl, c );
		pnlInner.add( lbl );

		strut = Box.createHorizontalStrut( INT_SIZE_STRUT );
		gridbag.setConstraints( strut, c );
		pnlInner.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myLabelMaxClique = new JLabel( "?", JLabel.RIGHT );
		gridbag.setConstraints( myLabelMaxClique, c );
		pnlInner.add( myLabelMaxClique );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		lbl = new JLabel( "Max separator size (normalized): ", JLabel.LEFT );
		gridbag.setConstraints( lbl, c );
		pnlInner.add( lbl );

		strut = Box.createHorizontalStrut( INT_SIZE_STRUT );
		gridbag.setConstraints( strut, c );
		pnlInner.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myLabelMaxSep = new JLabel( "?", JLabel.RIGHT );
		gridbag.setConstraints( myLabelMaxSep, c );
		pnlInner.add( myLabelMaxSep );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		lbl = new JLabel( "# cluster entries: ", JLabel.LEFT );
		gridbag.setConstraints( lbl, c );
		pnlInner.add( lbl );

		strut = Box.createHorizontalStrut( INT_SIZE_STRUT );
		gridbag.setConstraints( strut, c );
		pnlInner.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myLabelNumCluster = new JLabel( "?", JLabel.RIGHT );
		gridbag.setConstraints( myLabelNumCluster, c );
		pnlInner.add( myLabelNumCluster );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		lbl = new JLabel( "# separator entries: ", JLabel.LEFT );
		gridbag.setConstraints( lbl, c );
		pnlInner.add( lbl );

		strut = Box.createHorizontalStrut( INT_SIZE_STRUT );
		gridbag.setConstraints( strut, c );
		pnlInner.add( strut );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		myLabelNumSep = new JLabel( "?", JLabel.RIGHT );
		gridbag.setConstraints( myLabelNumSep, c );
		pnlInner.add( myLabelNumSep );
*/

		Border outsideBorder = BorderFactory.createEtchedBorder();
		Border insideBorder = BorderFactory.createEmptyBorder( 0, 16, 0, 16 );
		Border compoundBorder = BorderFactory.createCompoundBorder( outsideBorder, insideBorder);

		pnlInner.setBorder( compoundBorder );

		myInfoPanel = new JPanel();
		myInfoPanel.add( pnlInner );
	}

	public static int INT_SIZE_STRUT = (int)8;
}