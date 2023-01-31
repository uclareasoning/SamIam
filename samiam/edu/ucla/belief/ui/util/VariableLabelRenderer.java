package edu.ucla.belief.ui.util;

import edu.ucla.belief.Variable;
import edu.ucla.belief.io.StandardNode;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;

/**
	@author Keith Cascio
	@since 052303
*/
public class VariableLabelRenderer implements ListCellRenderer, TableCellRenderer
{
	public VariableLabelRenderer( ListCellRenderer r, Collection variables )
	{
		myListCellRenderer = r;
		recalculateDisplayValues( variables );
	}

	public VariableLabelRenderer( TableCellRenderer r, Collection variables )
	{
		myTableCellRenderer = r;
		recalculateDisplayValues( variables );
	}

	public void recalculateDisplayValues( Collection variables )
	{
		if( variables == null )
		{
			if( myMapVariablesToDisplayValues == null ) myMapVariablesToDisplayValues = Collections.EMPTY_MAP;
			else myMapVariablesToDisplayValues.clear();

			return;
		}

		if( myMapVariablesToDisplayValues == null || myMapVariablesToDisplayValues == Collections.EMPTY_MAP ) myMapVariablesToDisplayValues = new HashMap( variables.size() );
		else myMapVariablesToDisplayValues.clear();

		HashMap encounteredLables = new HashMap( variables.size() );

		StandardNode var;
		StandardNode varFirstEncountered;
		String valueDecorated;
		for( Iterator it = variables.iterator(); it.hasNext(); )
		{
			var = (StandardNode) it.next();
			if (var.getLabel() == null)
				valueDecorated = "";
			else
				valueDecorated = var.getLabel();
			if( encounteredLables.containsKey( valueDecorated ) )
			{
				varFirstEncountered = (StandardNode) encounteredLables.get( valueDecorated );
				if( varFirstEncountered != null )
				{
					myMapVariablesToDisplayValues.put( varFirstEncountered, valueDecorated + " (" + varFirstEncountered.getID() + ")" );
					encounteredLables.put( valueDecorated, null );
				}
				valueDecorated += " (" + var.getID() + ")";
			}
			else encounteredLables.put( valueDecorated, var );

			myMapVariablesToDisplayValues.put( var, valueDecorated );
		}
	}

	public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus)
	{
		Object valueDecorated = myMapVariablesToDisplayValues.get( value );
		if( valueDecorated == null ) valueDecorated = value;

		return myListCellRenderer.getListCellRendererComponent(
				list,
				valueDecorated,
				index,
				isSelected,
				cellHasFocus );
	}

	public Component getTableCellRendererComponent(JTable table,
				Object value,
				boolean isSelected,
				boolean hasFocus,
				int row,
				int column)
	{
		Object valueDecorated = myMapVariablesToDisplayValues.get( value );
		if( valueDecorated == null ) valueDecorated = value;

		return myTableCellRenderer.getTableCellRendererComponent(
				table,
				valueDecorated,
				isSelected,
				hasFocus,
				row,
				column);
	}

	protected ListCellRenderer myListCellRenderer;
	protected TableCellRenderer myTableCellRenderer;
	protected Map myMapVariablesToDisplayValues;
}
