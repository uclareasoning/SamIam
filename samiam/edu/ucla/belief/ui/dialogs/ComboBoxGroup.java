package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;

import java.util.List;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** A group of JComboBoxes that depend on
	each other.

	@author Keith Cascio
	@since 020405 */
public class ComboBoxGroup implements ItemListener// implements ActionListener
{
	public ComboBoxGroup( IdentificationModel model ){
		this.myIdentificationModel = model;
	}

	public void addItemListener( ItemListener listener ){
		if( myMapTokensToStructs == null ) return;
		Collection boxes = myMapTokensToStructs.values();
		for( Iterator it = boxes.iterator(); it.hasNext(); ){
			((IdentificationEquipment) it.next()).addItemListener( listener );
		}
	}

	public JPanel fillPanel( JPanel container, Iterator tokens ){
		if( container == null ){
			container = new JPanel( new GridBagLayout() );
			CPTImportWizard.border( container );
		}
		else container.removeAll();

		//Color lighter = container.getBackground();
		//Color darker = lighter.darker();
		//boolean flipflop = false;

		GridBagConstraints c = new GridBagConstraints();
		String token;
		IdentificationEquipment struct;
		while( tokens.hasNext() ){
			token = tokens.next().toString();
			struct = new IdentificationEquipment( token, (ComboBoxGroup)this );
			struct.addTo( container, c );
			//struct.setBackground( (flipflop = !flipflop) ? lighter : darker );
			myMapTokensToStructs.put( token, struct );
		}
		container.add( getPanelButtons(), c );

		return container;
	}

	public void clearAll(){
		Object unidentified = myIdentificationModel.getElementUnidentified();
		Collection boxes = myMapTokensToStructs.values();
		for( Iterator it = boxes.iterator(); it.hasNext(); ){
			((IdentificationEquipment) it.next()).setSelectedItem( unidentified );
		}
	}

	private JComponent getPanelButtons(){
		if( myPanelButtons == null ){
			myPanelButtons = new JPanel( new GridBagLayout() );
			JButton buttonClearAll = new JButton( "Clear All" );
			buttonClearAll.setToolTipText( "<html><nobr>Reset all assignments to " + CPTImportWizard.STR_UNIDENTIFIED );
			buttonClearAll.addActionListener( new ActionListener(){
				public void actionPerformed( ActionEvent event ){
					ComboBoxGroup.this.clearAll();
				}
			} );

			JButton buttonGuess = new JButton( "Guess" );
			buttonGuess.setToolTipText( "Try to guess the correct assignments using string matching" );
			buttonGuess.addActionListener( new ActionListener(){
				public void actionPerformed( ActionEvent event ){
					ComboBoxGroup.this.guess();
				}
			} );

			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.EAST;
			c.weightx = 1;
			myPanelButtons.add( Box.createHorizontalStrut(1), c );
			c.weightx = 0;
			myPanelButtons.add( buttonGuess, c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			myPanelButtons.add( buttonClearAll, c );
		}
		return myPanelButtons;
	}

	public boolean isIdentified(){
		mySelected = this.getSelectedSet( mySelected );
		return myIdentificationModel.isIdentified( mySelected );
	}

	public Set getUnselectedSet( Set container ){
		mySelected = this.getSelectedSet( mySelected );
		if( container == null ) container = new HashSet( myMapTokensToStructs.size() );
		else container.clear();
		Object[] range = myIdentificationModel.getRange();
		for( int i=0; i<range.length; i++ ) container.add( range[i] );
		container.removeAll( mySelected );
		return container;
	}

	public String tokenForElement( Object element ){
		Object key;
		IdentificationEquipment next;
		for( Iterator it = myMapTokensToStructs.keySet().iterator(); it.hasNext(); ){
			key = it.next();
			next = (IdentificationEquipment) myMapTokensToStructs.get( key );
			if( next.getSelectedItem() == element ) return key.toString();
		}
		return (String) null;
	}

	public Set getSelectedSet( Set container ){
		if( container == null ) container = new HashSet( myMapTokensToStructs.size() );
		else container.clear();
		Collection boxes = myMapTokensToStructs.values();
		for( Iterator it = boxes.iterator(); it.hasNext(); ){
			container.add( ((IdentificationEquipment) it.next()).getSelectedItem() );
		}
		return container;
	}

	public List getSelectedList( List container ){
		if( container == null ) container = new LinkedList();
		else container.clear();
		Collection boxes = myMapTokensToStructs.values();
		for( Iterator it = boxes.iterator(); it.hasNext(); ){
			container.add( ((IdentificationEquipment) it.next()).getSelectedItem() );
		}
		return container;
	}

	public Map getSelectedMap( Map container ){
		if( container == null ) container = new HashMap( myMapTokensToStructs.size() );
		else container.clear();
		Object key;
		IdentificationEquipment next;
		for( Iterator it = myMapTokensToStructs.keySet().iterator(); it.hasNext(); ){
			key = it.next();
			next = (IdentificationEquipment) myMapTokensToStructs.get( key );
			container.put( key, next.getSelectedItem() );
		}
		return container;
	}

	public IdentificationModel getModel(){
		return this.myIdentificationModel;
	}

	/** interface ComboBoxListener */
	public void itemStateChanged( ItemEvent e ){
		Object item = e.getItem();
		if( e.getStateChange() == ItemEvent.DESELECTED ) oldItem = item;
		else if( e.getStateChange() == ItemEvent.SELECTED ){
			JComboBox box = (JComboBox) e.getItemSelectable();
			comboBoxSelectionWarning( box.getModel(), oldItem, item );
		}
	}

	private Object oldItem;

	//public void changeSelected( ComboBoxModel model, Object oldItem, Object newItem )
	/** interface ComboBoxListener */
	public void comboBoxSelectionWarning( ComboBoxModel model, Object oldItem, Object newItem ){
		if( oldItem == newItem ) return;

		if( !myIdentificationModel.isOneToOne() ) return;

		IdentificationEquipment next;
		DefaultComboBoxModel nmodel;
		Collection boxes = myMapTokensToStructs.values();
		if( oldItem != myIdentificationModel.getElementUnidentified() ){
			for( Iterator it = boxes.iterator(); it.hasNext(); ){
				next = (IdentificationEquipment) it.next();
				nmodel = (DefaultComboBoxModel) next.getModel();
				if( nmodel != model ){
					if( nmodel.getIndexOf(oldItem) < 0 ) nmodel.addElement( oldItem );
					if( nmodel.getSize() > 1 ) next.setEnabled( true );
				}
			}
		}
		if( newItem != myIdentificationModel.getElementUnidentified() ){
			for( Iterator it = boxes.iterator(); it.hasNext(); ){
				next = (IdentificationEquipment) it.next();
				nmodel = (DefaultComboBoxModel) next.getModel();
				if( nmodel != model ){
					if( nmodel.getIndexOf(newItem) >= 0 ) nmodel.removeElement( newItem );
					if( nmodel.getSize() < 2 ) next.setEnabled( false );
				}
			}
		}

		myIdentificationModel.getStage().edited();
	}

	public void guess()
	{
		Object unidentified = myIdentificationModel.getElementUnidentified();
		Collection models = myMapTokensToStructs.values();
		for( Iterator it = models.iterator(); it.hasNext(); ){
			((IdentificationEquipment)it.next()).setSelectedItem( unidentified );
		}

		Object key;
		Object guess;
		IdentificationEquipment box;
		for( Iterator it = myMapTokensToStructs.keySet().iterator(); it.hasNext(); ){
			key = it.next();
			box = (IdentificationEquipment) myMapTokensToStructs.get( key );
			if( box.isEnabled() ){
				guess = myIdentificationModel.guess( key.toString() );
				if( (guess != null) && (guess != myIdentificationModel.getElementUnidentified()) )
					box.setGuess( guess );
			}
		}
	}

	//public void actionPerformed( ActionEvent event ){}

	private Map myMapTokensToStructs = new HashMap();
	private IdentificationModel myIdentificationModel;
	private Set mySelected;
	private JComponent myPanelButtons;
}
