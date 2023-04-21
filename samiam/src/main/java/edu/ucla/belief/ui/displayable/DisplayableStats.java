package edu.ucla.belief.ui.displayable;

import il2.util.IntStats;

import java.awt.*;
import javax.swing.*;
import java.text.*;

/**
	@author Keith Cascio
	@since 120704
*/
public class DisplayableStats extends JPanel
{
	public DisplayableStats( String caption, boolean flagShowTotal ){
		super( new GridBagLayout() );
		myCaption = caption;
		init( flagShowTotal );
	}

	/** @since 120804 */
	public void setUnits( String units ){
		myUnits = units;
		if( !myUnits.startsWith( " " ) ){
			myUnits = " " + myUnits;
		}
	}

	/** @since 120804 */
	public String toString(){
		StringBuffer buff = new StringBuffer( 256 );
		buff.append( myCaption );
		buff.append( " largest: " );
		buff.append( myLabelMax.getText() );
		buff.append( myUnits );
		buff.append( ", smallest: " );
		buff.append( myLabelMin.getText() );
		buff.append( myUnits );
		buff.append( ", average: " );
		buff.append( myLabelMean.getText() );
		buff.append( ", median: " );
		buff.append( myLabelMedian.getText() );
		buff.append( ", mode: " );
		buff.append( myLabelMode.getText() );
		buff.append( " (x" );
		buff.append( myLabelModeFrequency.getText() );
		buff.append( ")" );
		if( myLabelTotal != null ){
			buff.append( ", total: " );
			buff.append( myLabelTotal.getText() );
			buff.append( myUnits );
		}
		buff.append( "." );
		return buff.toString();
	}

	public void set( IntStats intstats ){
		myLabelMin.setText( Integer.toString( intstats.min ) );
		myLabelMax.setText( Integer.toString( intstats.max ) );
		myLabelMean.setText( FORMAT_MEAN.format( intstats.mean ) );
		myLabelMedian.setText( FORMAT_MEDIAN.format( intstats.median ) );
		myLabelMode.setText( Integer.toString( intstats.mode ) );
		//double fraction = ((double)intstats.modefrequency)/((double)intstats.count);
		//myLabelModeFrequency.setText( Double.toString( fraction*((double)100) ) );
		//myLabelModeFrequency.setText( FORMAT_PERCENT.format( fraction ) );
		myLabelModeFrequency.setText( Integer.toString( intstats.modefrequency ) );
		if( myLabelTotal != null ) myLabelTotal.setText( Integer.toString( intstats.sum ) );
	}

	private void init( boolean flagShowTotal )
	{
		GridBagConstraints c = new GridBagConstraints();

		JPanel pnl1 = new JPanel( new GridBagLayout() );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		pnl1.add( new JLabel( myCaption ), c );

		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		pnl1.add( Box.createHorizontalStrut( 8 ), c );

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		pnl1.add( new JLabel( "[" ), c );
		pnl1.add( myLabelMin = new JLabel( STR_UNKNOWN ), c );
		pnl1.add( new JLabel( ", " ), c );
		pnl1.add( myLabelMax = new JLabel( STR_UNKNOWN ), c );
		c.gridwidth=GridBagConstraints.REMAINDER;
		pnl1.add( new JLabel( "]" ), c );

		JPanel pnl2 = new JPanel( new GridBagLayout() );

		c.gridwidth = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		pnl2.add( Box.createHorizontalStrut(32), c );
		pnl2.add( new JLabel( "average: " ), c );
		pnl2.add( myLabelMean = new JLabel( STR_UNKNOWN ), c );
		pnl2.add( new JLabel( ", median: " ), c );
		pnl2.add( myLabelMedian = new JLabel( STR_UNKNOWN ), c );
		pnl2.add( new JLabel( ", mode: " ), c );
		pnl2.add( myLabelMode = new JLabel( STR_UNKNOWN ), c );
		pnl2.add( new JLabel( " (x" ), c );
		pnl2.add( myLabelModeFrequency = new JLabel( STR_UNKNOWN ), c );
		pnl2.add( new JLabel( ")" ), c );
		c.gridwidth=GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		pnl2.add( Box.createHorizontalStrut(0), c );

		JPanel pnlTotal = null;
		if( flagShowTotal ){
			pnlTotal = new JPanel( new GridBagLayout() );
			c.gridwidth = 1;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			pnlTotal.add( Box.createHorizontalStrut(32), c );
			pnlTotal.add( new JLabel( "total: " ), c );
			pnlTotal.add( myLabelTotal = new JLabel( STR_UNKNOWN ), c );
			c.gridwidth=GridBagConstraints.REMAINDER;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			pnlTotal.add( Box.createHorizontalStrut(0), c );
		}

		c.gridwidth=GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.add( pnl1, c );
		this.add( pnl2, c );
		if( flagShowTotal ) this.add( pnlTotal, c );
	}

	public static final String STR_UNKNOWN = "?";

	private String myCaption = "";
	private String myUnits = "";

	private JLabel myLabelMin;
	private JLabel myLabelMax;
	private JLabel myLabelMean;
	private JLabel myLabelMedian;
	private JLabel myLabelMode;
	private JLabel myLabelModeFrequency;
	private JLabel myLabelTotal;

	private static NumberFormat FORMAT_MEAN = new DecimalFormat( "0.##" );
	private static NumberFormat FORMAT_MEDIAN = new DecimalFormat( "0.#" );
	//private static NumberFormat FORMAT_PERCENT = new DecimalFormat( "0.##%" );
}
