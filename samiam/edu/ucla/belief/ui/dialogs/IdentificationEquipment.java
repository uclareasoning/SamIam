package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;

import java.util.List;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Bundle 2 JLabels and a JComboBox -
	the ui equipment a user uses to
	match a String token to some meaningful Object.

	@author Keith Cascio
	@since 020405 */
public class IdentificationEquipment implements ActionListener
{
	public static final String STR_PREPOSITION_DEFAULT = "=";
	public static final String STR_PREPOSITION_GUESS = "<html><nobr>= <font color=\"#00CC00\">(guess)";

	public IdentificationEquipment( JLabel labelToken, JLabel labelPreposition, JComboBox boxRange ){
		this.labelToken = labelToken;
		this.labelPreposition = labelPreposition;
		this.boxRange = boxRange;
		init();
	}

	public IdentificationEquipment( String token, ComboBoxGroup group ){
		this.labelToken = new JLabel( "\"" + token + "\"", JLabel.LEFT );
		this.labelPreposition = new JLabel( STR_PREPOSITION_DEFAULT );
		this.boxRange = this.createCombo( token, group );
		init();
	}

	private void init(){
		this.boxRange.addActionListener( (ActionListener)this );
	}

	public void addTo( JComponent container, GridBagConstraints c ){
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		container.add( this.labelToken, c );
		c.weightx = 1;
		container.add( Box.createHorizontalStrut( 8 ), c );
		c.weightx = 0;
		container.add( this.labelPreposition, c );
		c.weightx = 1;
		container.add( Box.createHorizontalStrut( 8 ), c );
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		container.add( this.boxRange, c );
	}

	public void setSelectedItem( Object item ){
		boxRange.setSelectedItem( item );
	}

	public Object getSelectedItem(){
		return boxRange.getSelectedItem();
	}

	public ComboBoxModel getModel(){
		return (ComboBoxModel) boxRange.getModel();
	}

	public void setEnabled( boolean flag ){
		boxRange.setEnabled( flag );
	}

	public boolean isEnabled(){
		return boxRange.isEnabled();
	}

	public void setGuess( Object element ){
		if( element == null ) return;
		boxRange.setSelectedItem( element );
		setIsGuess( true );
	}

	public void setIsGuess( boolean flag ){
		labelPreposition.setText( flag ? STR_PREPOSITION_GUESS : STR_PREPOSITION_DEFAULT );
	}

	public JComboBox createCombo( String token, ComboBoxGroup group ){
		ComboBoxModel model = new DefaultComboBoxModel( group.getModel().getRange() );
		//model.addListener( CPTDataScanWizardPanel.this );
		JComboBox ret = new JComboBox( model );
		ret.addItemListener( (ItemListener)group );
		ret.setEditable( false );
		ret.setRenderer( group.getModel() );
		return ret;
	}

	/*public void addComboBoxListener( ComboBoxListener listener ){
		getModel().addListener( listener );
	}*/

	public void addItemListener( ItemListener listener ){
		boxRange.addItemListener( listener );
	}

	public void actionPerformed( ActionEvent event ){
		setIsGuess( false );
	}

	public void setBackground( Color color ){
		labelToken.setBackground( color );
		labelPreposition.setBackground( color );
		boxRange.setBackground( color );
	}

	public JLabel labelToken;
	public JLabel labelPreposition;
	public JComboBox boxRange;
}
