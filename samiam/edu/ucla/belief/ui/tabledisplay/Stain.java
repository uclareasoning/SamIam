package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.preference.ColorSwatchLabel;
import edu.ucla.belief.ui.util.Broadcaster;

import java.awt.Color;
import java.awt.Component;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/** A Stain is a set of graphical traits that
	distinguish a family of objects.  I.E. supports
	color-coding cpt parameters in a table to enhance
	relationships among them.

	@author Keith Cascio
	@since 030905 */
public class Stain extends Broadcaster
{
	public Stain( String descrip, Color back, Color fore, String verbose ){
		super();
		this.myShortDescription = descrip;
		this.myVerboseDescription = verbose;
		this.myBackground = back;
		this.myForeground = fore;
	}

	public String getShortDescription(){
		return this.myShortDescription;
	}

	/** @since 031405 */
	public String getVerboseDescription(){
		return this.myVerboseDescription;
	}

	public Color getBackground(){
		return this.myBackground;
	}

	public Color getForeground(){
		return this.myForeground;
	}

	public void stain( Component comp ){
		if( (!isEnabled()) || (!isSelected()) ) return;
		comp.setBackground( myBackground );
		comp.setForeground( myForeground );
	}

	public boolean isEnabled(){
		return this.myFlagEnabled;
	}

	public void setEnabled( boolean flag ){
		if( this.myFlagEnabled != flag ){
			this.myFlagEnabled = flag;
		}
	}

	public boolean isSelected(){
		return this.myFlagSelected;
	}

	public void setSelected( boolean flag ){
		if( this.myFlagSelected != flag ){
			this.myFlagSelected = flag;
			if( myCB != null ) myCB.setSelected( myFlagSelected );
		}
	}

	public static final String STR_TOOLTIP_SWATCH_PRE = "<html><nobr>Double-click to choose a different color for <b>";
	public static final String STR_TOOLTIP_BOX_PRE = "<html><nobr>Turn coloring of <b>";
	public static final String STR_TOOLTIP_BOX_POST = "</b> on/off";

	/** @since 20071210 */
	private Stain initGUI(){
		if( myLabel != null ) return this;

		myLabel = new ColorSwatchLabel( getBackground() );
		myLabel.setMinimumSize( DIM_SIZE_LABEL );
		myLabel.setPreferredSize( DIM_SIZE_LABEL );
		myLabel.setToolTipText( STR_TOOLTIP_SWATCH_PRE + getShortDescription() );
		myLabel.addActionListener( new ActionListener(){
			public void actionPerformed( ActionEvent event ){
				Stain.this.myBackground = myLabel.clr;
				Stain.this.fireListeners();
			}
		} );

		myLabelSD = new JLabel( getShortDescription() );
		myLabelSD.setToolTipText( getVerboseDescription() );

		myCB = new JCheckBox();
		myCB.setSelected( myFlagSelected );
		myCB.setToolTipText( STR_TOOLTIP_BOX_PRE + getShortDescription() + STR_TOOLTIP_BOX_POST );
		myCB.addActionListener( new ActionListener(){
			public void actionPerformed( ActionEvent event ){
				Stain.this.setSelected( myCB.isSelected() );
				Stain.this.fireListeners();
			}
		} );

		return this;
	}

	public JComponent getSwitch(){
		if( mySwitch == null ){
			initGUI().addSwitch( mySwitch = new JPanel( new GridBagLayout() ), new GridBagConstraints() );
			border( mySwitch );
		}
		return mySwitch;
	}

	/** @since 20071210 */
	public JComponent addSwitch( JComponent pnl, GridBagConstraints c ){
		initGUI();

		c.gridwidth = 1;
		c.weightx   = 0;
		c.fill      = GridBagConstraints.NONE;
		pnl.add(                        myLabel, c );
		pnl.add( Box.createHorizontalStrut( 8 ), c );
		pnl.add(                      myLabelSD, c );
		c.weightx   = 1;
		pnl.add( Box.createHorizontalStrut( 1 ), c );
		c.weightx   = 0;
		pnl.add(                           myCB, c );

		return pnl;
	}

	public static void border( JComponent comp ){
		Border emptyinner = BorderFactory.createEmptyBorder( /*top*/0, /*left*/1, /*bottom*/1, /*right*/1 );
		Border etched = BorderFactory.createEtchedBorder();
		Border compoundinner = BorderFactory.createCompoundBorder( /*outside*/etched, /*inside*/emptyinner );
		Border emptyouter = BorderFactory.createEmptyBorder( /*top*/1, /*left*/2, /*bottom*/0, /*right*/0 );
		Border compoundouter = BorderFactory.createCompoundBorder( /*outside*/emptyouter, /*inside*/compoundinner );
		comp.setBorder( compoundouter );
	}

	public static final Stain NONE = new Stain( "none", (Color)null, (Color)null, "none" ){
		public void stain( Component comp ){}
		public boolean isEnabled(){ return false; }
	};

	public static final Dimension DIM_SIZE_LABEL = new Dimension( 0x10, 0x10 );

	private String myShortDescription, myVerboseDescription;
	private Color myBackground, myForeground;
	private boolean myFlagEnabled = true, myFlagSelected = true;
	private JComponent mySwitch;
	private JCheckBox myCB;
	private ColorSwatchLabel myLabel;
	private JLabel myLabelSD;
}
