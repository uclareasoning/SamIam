package edu.ucla.belief.ui.util;

import edu.ucla.belief.ui.util.*;

import java.awt.*;
import javax.swing.*;

/**
	@author Keith Cascio
	@since 092104
*/
public abstract class IncrementPanel extends JPanel
{
	public IncrementPanel( String caption )
	{
		super( new GridBagLayout() );

		GridBagConstraints c = new GridBagConstraints();

		c.gridwidth = 1;
		this.add( new JLabel( caption ), c );
		this.add( myLabel = new JLabel(), c );
		//Dimension dim = myLabel.getPreferredSize();//new Dimension( 40,1 );
		//dim.width = dim.width >> 2;
		//myLabel.setMinimumSize( dim );
		//myLabel.setPreferredSize( dim );

		JPanel panel = new JPanel( new GridLayout(2,1) );
		panel.add( myInc = new HyperLabel( "+", new Runnable(){
			public void run(){
				setValue( getValue() + 1 );
				update();
				//SloppyPanel.this.updateIncrementPanels();
			}
		}, false ) );
		panel.add( myDec = new HyperLabel( "-", new Runnable(){
			public void run(){
				setValue( getValue() - 1 );
				update();
				//SloppyPanel.this.updateIncrementPanels();
			}
		}, false ) );

		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( panel, c );

		//update();
	}

	public void update(){
		refresh();
	}

	final public void refresh(){
		int value = getValue();
		myLabel.setText( Integer.toString( value ) );
		myInc.setEnabled( value+1 <= getCeiling() );
		myDec.setEnabled( value-1 >= getFloor() );
	}

	abstract public int getValue();
	abstract public void setValue( int val );
	abstract public int getCeiling();
	abstract public int getFloor();

	private JLabel myLabel;
	private HyperLabel myInc, myDec;
}