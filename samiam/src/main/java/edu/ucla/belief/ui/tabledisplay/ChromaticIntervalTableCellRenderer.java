package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.util.*;

import edu.ucla.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

/**
	Render a sensitivity interval with modified colors.

	@author Keith Cascio
	@since 102303
*/
public class ChromaticIntervalTableCellRenderer extends DefaultTableCellRenderer
{
	public ChromaticIntervalTableCellRenderer( DoubleFormat format, Color colorPanel, Color colorLabel )
	{
		this.format = format;
		setHorizontalAlignment( JLabel.TRAILING );
		setFont( getFont().deriveFont( Font.PLAIN ) );

		//init( SuggestionDataHandler.COLOR_BACKGROUND_PANEL, SuggestionDataHandler.COLOR_BACKGROUND_SUGGESTED );
		init( colorPanel, colorLabel );
	}

	public void init( Color colorPanel, Color colorLabel )
	{
		myJPanel = new JPanel( new GridBagLayout() );
		myJPanel.setBackground( colorPanel );

		GridBagConstraints c = new GridBagConstraints();

		JPanel myPanelSuggested = new JPanel( new GridBagLayout() );
		myPanelSuggested.setBackground( colorLabel );
		setBackground( colorLabel );

		c.weightx = 1;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		myJPanel.add( myPanelSuggested, c );

		myPanelSuggested.add( this, c );
	}

	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
	{
		super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
		setText( ((Interval)value).toString( format ) );

		return myJPanel;
	}

	private DoubleFormat format;
	private JPanel myJPanel;
}
