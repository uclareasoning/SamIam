package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;

import edu.ucla.belief.*;
import edu.ucla.util.WeakLinkedList;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChooseInstancePanel extends JPanel implements ItemListener {
	private NetworkInternalFrame hnInternalFrame;
	private WeakLinkedList listeners;
	private VariableComboBox varBox;
	private JComboBox valueBox;

	public ChooseInstancePanel(NetworkInternalFrame
		hnInternalFrame) {
		this.hnInternalFrame = hnInternalFrame;
		listeners = new WeakLinkedList();

		setLayout(new GridLayout(2, 0));

		varBox = hnInternalFrame.createVariableComboBox();
		varBox.addSelectedChangeListener(this);
		add(varBox);

		JPanel valuePanel = new JPanel();
		valuePanel.setLayout(new BoxLayout(valuePanel,
			BoxLayout.X_AXIS));
		add(valuePanel);

		valuePanel.add(new JLabel(" = "));

		valueBox = new JComboBox();
		revalidateValueBox();
		valueBox.addItemListener(this);
		valuePanel.add(valueBox);
	}

	public void addItemListener(ItemListener listener) {
		listeners.add(listener);
	}

	public void removeItemListener(ItemListener listener) {
		listeners.remove(listener);
	}

	public DisplayableFiniteVariable getVariable() {
		return (DisplayableFiniteVariable)varBox.
			getSelectedItem();
	}

	public Object getInstance() {
		return valueBox.getSelectedItem();
	}

	private void revalidateValueBox()
	{
		valueBox.removeAllItems();
		DisplayableFiniteVariable dvar = (DisplayableFiniteVariable)varBox.getSelectedItem();
		if (dvar == null)
			return;

		//FiniteVariable var = dvar.getFiniteVariable();
		for (int i = 0; i < dvar.size(); i++) {
			Object instance = dvar.instance(i);
			valueBox.addItem(instance);
		}
	}

	public void itemStateChanged(ItemEvent event) {
		ItemSelectable item = event.getItemSelectable();
		if (item == varBox)
			revalidateValueBox();

		ItemListener next;
		for( ListIterator it = listeners.listIterator(); it.hasNext(); ){
			next = (ItemListener)it.next();
			if( next == null ) it.remove();
			else next.itemStateChanged(event);
		}
	}
}
