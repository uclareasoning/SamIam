package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;

import edu.ucla.belief.*;
import edu.ucla.util.WeakLinkedList;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class PickInstancesPanel extends JPanel implements ActionListener, ItemListener
{
	private NetworkInternalFrame hnInternalFrame;
	private WeakLinkedList listeners;
	private VariableComboBox varBox;
	private JList instList, pickList;
	private JButton addButton, removeButton;

	public PickInstancesPanel( NetworkInternalFrame hnInternalFrame )
	{
		this.hnInternalFrame = hnInternalFrame;
		listeners = new WeakLinkedList();

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		JPanel leftPanel = new JPanel();
		leftPanel.setPreferredSize(new Dimension(250, 100));
		leftPanel.setLayout(new BoxLayout(leftPanel,
			BoxLayout.Y_AXIS));
		add(leftPanel);

		JPanel varPanel = new JPanel();
		varPanel.setLayout(new BoxLayout(varPanel,
			BoxLayout.X_AXIS));
		leftPanel.add(varPanel);

		varPanel.add(new JLabel("Choose variable: "));

		varBox = hnInternalFrame.createVariableComboBox();
		varBox.addSelectedChangeListener(this);
		varPanel.add(varBox);

		JPanel instPanel = new JPanel();
		instPanel.setLayout( new BoxLayout( instPanel,BoxLayout.X_AXIS ) );
		leftPanel.add(instPanel);

		instPanel.add(new JLabel("Instance(s): "));

		instList = new JList(new DefaultListModel());
		revalidateInstList();
		instList.setVisibleRowCount(3);
		instPanel.add(new JScrollPane(instList));

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel,
			BoxLayout.X_AXIS));
		add(rightPanel);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(2, 0));
		rightPanel.add(buttonPanel);

		addButton = new JButton("Add ->");
		addButton.addActionListener(this);
		buttonPanel.add(addButton);

		removeButton = new JButton("<- Remove");
		removeButton.addActionListener(this);
		buttonPanel.add(removeButton);

		JPanel pickPanel = new JPanel();
		pickPanel.setLayout(new BorderLayout());
		rightPanel.add(pickPanel);

		pickPanel.add("North", new JLabel("Picked instances:"));

		pickList = new JList(new DefaultListModel());
		pickList.setVisibleRowCount(5);
		pickPanel.add("Center", new JScrollPane(pickList));
	}

	public void addActionListener(ActionListener listener)
	{
		listeners.add(listener);
	}

	public void removeActionListener(ActionListener listener)
	{
		listeners.remove(listener);
	}

	public VariableInstance[] getPickedInstances()
	{
		Object[] picks = ((DefaultListModel)pickList.getModel()).toArray();
		VariableInstance[] varInstances = new VariableInstance[picks.length];
		for(int i = 0; i < varInstances.length; i++)
		{
			varInstances[i] = (VariableInstance)picks[i];
		}
		return varInstances;
	}

	public void revalidateInstList()
	{
		DisplayableFiniteVariable dvar = (DisplayableFiniteVariable)varBox.getSelectedItem();
		DefaultListModel model = (DefaultListModel)instList.getModel();
		model.removeAllElements();
		if(dvar != null)
		{
			VariableInstance[] varInstances = hnInternalFrame.getVariableInstances( dvar );
			for (int i = 0; i < varInstances.length; i++)
			{
				model.addElement( varInstances[i] );
			}
		}
	}

	public void removePicks(Object[] vars)
	{
		DefaultListModel model = (DefaultListModel)pickList.getModel();
		Object[] picks = model.toArray();
		for(int i = 0; i < picks.length; i++)
		{
			for(int j = 0; j < vars.length; j++)
			{
				if(((VariableInstance)picks[i]).getVariable() == vars[j])
				{
					model.removeElement( picks[i] );
					break;
				}
			}
		}
	}

	public void actionPerformed(ActionEvent event)
	{
		DefaultListModel model = (DefaultListModel)pickList.
		getModel();
		if (event.getSource() == addButton)
		{
			Object[] insts = instList.getSelectedValues();
			for (int i = 0; i < insts.length; i++)
			if (!model.contains(insts[i]))
			model.addElement(insts[i]);
		}

		else
		{
			Object[] picks = pickList.getSelectedValues();
			for (int i = 0; i < picks.length; i++)
			model.removeElement(picks[i]);
		}

		hnInternalFrame.getParentFrame().setWaitCursor();
		ActionListener next;
		for( ListIterator it = listeners.listIterator(); it.hasNext(); ){
			next = (ActionListener)it.next();
			if( next == null ) it.remove();
			else next.actionPerformed(event);
		}
		hnInternalFrame.getParentFrame().setDefaultCursor();
	}

	public void itemStateChanged(ItemEvent event)
	{
		revalidateInstList();
	}
}

