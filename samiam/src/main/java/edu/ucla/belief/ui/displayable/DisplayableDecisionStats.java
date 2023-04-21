package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.decision.*;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;

/** @author Keith Cascio
	@since 010905 */
public class DisplayableDecisionStats extends Stats
{
	public DisplayableDecisionStats(){
		super();
	}

	protected void initHook(){
		this.myDisplayableStats = new DisplayableStat[] { (DisplayableStat)internals, (DisplayableStat)leaves, (DisplayableStat)parameters };
	}

	protected Stat makeStat( String caption ){
		return new DisplayableStat( caption );
	}

	public JComponent getGUI(){
		refreshGUI();
		return myGUI;
	}

	public void refreshGUI(){
		if( myGUI == null ) makeGUI();
		for( int i=0; i<myDisplayableStats.length; i++ ) myDisplayableStats[i].refresh();
	}

	private void makeGUI()
	{
		myGUI = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		for( int i=0; i<myDisplayableStats.length; i++ ) myDisplayableStats[i].addLine( myGUI, c );
	}

	public static JLabel makeL( String text ){
		return new JLabel( text, JLabel.LEFT );
	}

	public static class DisplayableStat extends Stats.Stat
	{
		public DisplayableStat( String caption ){
			super( caption );
		}

		public void refresh(){
			labelTotal.setText( Integer.toString( total ) );
			labelDistinct.setText( Integer.toString( distinct.size() ) );
			labelPercent.setText( FORMAT_PERCENT.format( fraction ) );
		}

		public void addLine( JComponent comp, GridBagConstraints c )
		{
			c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 1;
			comp.add( labelTotal = makeL( "" ), c );
			comp.add( makeL( " "+caption ), c );
			comp.add( makeL( " ( " ), c );
			comp.add( labelDistinct = makeL( "" ), c );
			comp.add( makeL( " distinct ) ( " ), c );
			comp.add( labelPercent = makeL( "" ), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			comp.add( makeL( " distinct )" ), c );
		}

		public JLabel labelTotal;
		public JLabel labelDistinct;
		public JLabel labelPercent;
	}

	private JComponent myGUI;
	private DisplayableStat[] myDisplayableStats;
}
