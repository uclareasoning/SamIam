package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.io.CPTInfo;

import edu.ucla.belief.ui.util.Broadcaster;
import edu.ucla.belief.ui.dialogs.CPTImportWizard;

import java.awt.Color;
import java.awt.Component;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Supports staining a cpt to inform the user
	of quality of imported data, e.g. whether parameter
	conflict, are redundant, agree with existing data, or
	disagree.

	@author Keith Cascio
	@since 030805 */
public class ImportedParameterStainer extends Broadcaster implements StainWright
{
	public static final Color COLOR_AGREES    = new Color( 0x33, 0xcc, 0x33 );
	public static final Color COLOR_DISAGREES = new Color( 0xff, 0x99, 0x33 );
	public static final Color COLOR_CONFLICTS = new Color( 0xff, 0xff, 0x00 );
	public static final Color COLOR_REDUNDANT = new Color( 0x00, 0xff, 0xff );

	public static final Color COLOR_CHANGED   = new Color( 0xff, 0xcc, 0xcc );
	public static final Color COLOR_UNCHANGED = new Color( 0xcc, 0xff, 0xcc );

	public static final Color COLOR_FOREGROUND = Color.black;

	public static final String STR_CHANGE_DEFINED = "<html><nobr><b>change</b>: the import wizard changed the value of this parameter, possibly as a result of normalization";
	public static final String STR_SPARED_DEFINED = "<html><nobr><b>unchanged</b>: the import wizard did not change the value of this parameter";
	public static final String STR_TOOLTIP_COLORS = "Select whether or not to use colors to indicate the origin of imported parameters";
	public static final String STR_TOOLTIP_KEY = "Definition of color codes";
	public static final String STR_TOOLTIP_BOX = "Turn colors on/off";

	private void init(){
		if( STAINS != null ) return;

		STAIN_CONFLICTS = new Stain( "conflict",  COLOR_CONFLICTS, COLOR_FOREGROUND, CPTImportWizard.STR_CONFLICT_DEFINED );
		STAIN_REDUNDANT = new Stain( "redundant", COLOR_REDUNDANT, COLOR_FOREGROUND, CPTImportWizard.STR_REDUNDANCY_DEFINED );
		STAIN_DISAGREES = new Stain( "disagrees", COLOR_DISAGREES, COLOR_FOREGROUND, CPTImportWizard.STR_DISAGREEMENT_DEFINED );
		STAIN_AGREES    = new Stain( "agrees",    COLOR_AGREES,    COLOR_FOREGROUND, CPTImportWizard.STR_AGREEMENT_DEFINED );
		STAIN_CHANGED   = new Stain( "changed",   COLOR_CHANGED,   COLOR_FOREGROUND, STR_CHANGE_DEFINED );
		STAIN_UNCHANGED = new Stain( "unchanged", COLOR_UNCHANGED, COLOR_FOREGROUND, STR_SPARED_DEFINED );

		STAINS = new Stain[] { STAIN_AGREES, STAIN_DISAGREES, STAIN_CONFLICTS, STAIN_REDUNDANT, STAIN_CHANGED, STAIN_UNCHANGED };
	}

	private Stain STAIN_CONFLICTS;
	private Stain STAIN_REDUNDANT;
	private Stain STAIN_DISAGREES;
	private Stain STAIN_AGREES;
	private Stain STAIN_CHANGED;
	private Stain STAIN_UNCHANGED;
	private Stain[] STAINS;

	public Stain getStain( int row, int column ){
		Stain ret = Stain.NONE;
		if( !isEnabled() ) return ret;

		CPTInfo.Parameter param = getParameter( row, column );
		if( param == null ) return ret = filter( ret, STAIN_UNCHANGED );

		if( param.agrees ) ret = filter( ret, STAIN_UNCHANGED );
		else ret = filter( ret, STAIN_CHANGED );

		if( param.mentioned ){
			if( param.agrees ) ret = filter( ret, STAIN_AGREES );
			else ret = filter( ret, STAIN_DISAGREES );

			if( param.redundant ) ret = filter( ret, STAIN_REDUNDANT );

			if( param.conflicts ) ret = filter( ret, STAIN_CONFLICTS );
		}

		return ret;
	}

	private Stain filter( Stain general, Stain specific ){
		if( specific.isSelected() && specific.isEnabled() ) return specific;
		else return general;
	}

	public void stain( int row, int column, Component comp ){
		getStain( row, column ).stain( comp );
	}

	public Stain[] getStains(){
		return STAINS;
	}

	public ImportedParameterStainer( CPTInfo info, HuginGenieStyleTableFactory.TableModelHGS model ){
		super();
		init();
		this.myCPTInfo = info;
		this.myTableModelHGS = model;
		/*//System.out.println( "ImportedParameterStainer( "+model.getClass().getName()+" )" );
		int count = 0;
		for( int i=0; i<info.length; i++ ){
			if( info.getParameter(i) != null ) ++count;
		}
		//System.out.println( "    found " + count + " parameters." );*/
	}

	public void setCPTInfo( CPTInfo info ){
		this.myCPTInfo = info;
	}

	private CPTInfo.Parameter getParameter( int row, int column ){
		int linear = myTableModelHGS.calculateDataIndex( row, column );
		if( linear < 0 ) return (CPTInfo.Parameter)null;
		else return myCPTInfo.getParameter( linear );
	}

	public Color getBackground( int row, int column ){
		return getStain( row, column ).getBackground();
	}

	public boolean isEnabled(){
		return myFlagEnabled;
	}

	public void setEnabled( boolean flag ){
		if( myFlagEnabled != flag ){
			myFlagEnabled = flag;
			if( myCB != null ) myCB.setSelected( myFlagEnabled );
			for( int i=0; i<STAINS.length; i++ ) STAINS[i].setEnabled( myFlagEnabled );
		}
	}

	public JComponent getSwitches(){
		if( mySwitches == null ){
			mySwitches = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = 1;
			c.weightx = 0;

			JLabel label = new JLabel( "<html><b>Colors:</b> " );
			label.setToolTipText( STR_TOOLTIP_COLORS );
			mySwitches.add( Box.createHorizontalStrut(8), c );
			mySwitches.add( label, c );
			mySwitches.add( myCB = new JCheckBox(), c );
			myCB.setSelected( myFlagEnabled );
			myCB.setToolTipText( STR_TOOLTIP_BOX );
			myCB.addActionListener( new ActionListener(){
				public void actionPerformed( ActionEvent event ){
					ImportedParameterStainer.this.setEnabled( myCB.isSelected() );
					ImportedParameterStainer.this.fireListeners();
				}
			} );

			mySwitches.add( Box.createHorizontalStrut(8), c );
			label = new JLabel( "<html><b>Key:</b> " );
			label.setToolTipText( STR_TOOLTIP_KEY );
			mySwitches.add( label, c );

			for( int i=0; i<STAINS.length; i++ ){
				mySwitches.add( STAINS[i].getSwitch(), c );
			}
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			mySwitches.add( Box.createHorizontalStrut(1), c );
		}
		return mySwitches;
	}

	public void addListener( ActionListener listener ){
		super.addListener( listener );
		for( int i=0; i<STAINS.length; i++ )
			STAINS[i].addListener( listener );
	}

	public boolean removeListener( ActionListener listener ){
		for( int i=0; i<STAINS.length; i++ )
			STAINS[i].removeListener( listener );
		return super.removeListener( listener );
	}

	private HuginGenieStyleTableFactory.TableModelHGS myTableModelHGS;
	private CPTInfo myCPTInfo;
	private JComponent mySwitches;
	private JCheckBox myCB;
	private boolean myFlagEnabled = true;
}
