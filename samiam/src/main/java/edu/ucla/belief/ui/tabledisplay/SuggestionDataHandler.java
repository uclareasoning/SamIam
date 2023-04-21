package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.*;
import edu.ucla.util.*;

import java.util.*;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;

/** @author keith cascio
	@since  20031002 */
public class SuggestionDataHandler implements DataHandler
{
	public SuggestionDataHandler( double[] data, ProbabilityInterval[] intervals, TableCellRenderer defaultRenderer, TableCellRenderer headerRenderer, TableCellEditor tce )
	{
		myData            = data;
		myIntervals       = intervals;
		myDefaultRenderer = defaultRenderer;
		myHeaderRenderer  = headerRenderer;
		myTableCellEditor = tce;

		init();
	}

	public static final Color
	  COLOR_BACKGROUND_PANEL     = Color.white,
	  COLOR_BACKGROUND_CURRENT   = Color.white,
	  COLOR_BACKGROUND_DELIMITER = Color.white,
	  COLOR_BACKGROUND_SUGGESTED = Color.pink;

	/** @since 20080229 */
	public boolean             minimize(){
		return false;
	}

	public void setStainWright( StainWright wright ){
		myStainWright = wright;
	}

	private void init()
	{
		myNumberFormat = new DecimalFormat( "0.0#####" );
		//myNumberFormat = new DecimalFormat( "0.0###############################" );

		myJPanel = new JPanel( new GridBagLayout() );
		myJPanel.setBackground( COLOR_BACKGROUND_PANEL );

		GridBagConstraints c = new GridBagConstraints();

		myLabelCurrent = new JLabel();
		JPanel pnlCurrent = new JPanel( new GridLayout() );
		pnlCurrent.setBackground( COLOR_BACKGROUND_CURRENT );
		pnlCurrent.add( myLabelCurrent );
		myJPanel.add( pnlCurrent, c );

		JLabel lblDelimiter = new JLabel( " " );//"->" );
		JPanel pnlDelimiter = new JPanel( new GridLayout() );
		pnlDelimiter.setBackground( COLOR_BACKGROUND_DELIMITER );
		pnlDelimiter.add( lblDelimiter );
		myJPanel.add( pnlDelimiter, c );

		myLabelSuggested = new JLabel();
		JPanel pnlSuggested = new JPanel( new GridLayout() );
		pnlSuggested.setBackground( COLOR_BACKGROUND_SUGGESTED );
		pnlSuggested.add( myLabelSuggested );

		c.gridwidth = GridBagConstraints.REMAINDER;
		myJPanel.add( pnlSuggested, c );
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		if( value instanceof Struct )
		{
			//Component ret = myDefaultRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			//return ret;
			Struct struct = (Struct)value;
			myLabelCurrent.setText( struct.current );
			myLabelSuggested.setText( (struct.suggested==null) ? "" : struct.suggested );
			return myJPanel;
		}
		else return myHeaderRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
	}

	public Object getEventSource()
	{
		return myTableCellEditor;
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		JTextField ret = (JTextField) myTableCellEditor.getTableCellEditorComponent( table, value, isSelected, row, column );

		if( isSelected )
		{
			if( ProbabilityJTableEditor.safe() ){
				ret.setBackground( ProbabilityJTableEditor.mySelectBackgroundColor );
				ret.setForeground( ProbabilityJTableEditor.mySelectForegroundColor );
			}else{
				ret.setForeground( (Color) UIManager.getDefaults().get( "TextField.foreground" ) );
			}
		}
		else
		{
			ret.setBackground( ProbabilityJTableEditor.myNormalBackgroundColor );
			ret.setForeground( ProbabilityJTableEditor.myNormalForegroundColor );
		}

		//ret.setText( myNumberFormat.format( value ) );

		ret.setSelectedTextColor( ProbabilityJTableEditor.mySelectedTextColor );
		ret.setSelectionColor( ProbabilityJTableEditor.mySelectionColor );

		return ret;
	}

	public Object getCellEditorValue() { return myTableCellEditor.getCellEditorValue(); }
	public boolean isCellEditable(EventObject anEvent) { return myTableCellEditor.isCellEditable(anEvent); }
	public boolean shouldSelectCell(EventObject anEvent) { return myTableCellEditor.shouldSelectCell(anEvent); }
	public boolean stopCellEditing() { return myTableCellEditor.stopCellEditing(); }
	public void cancelCellEditing() { myTableCellEditor.cancelCellEditing(); }
	public void addCellEditorListener(CellEditorListener l) { myTableCellEditor.addCellEditorListener(l); }
	public void removeCellEditorListener(CellEditorListener l) { myTableCellEditor.removeCellEditorListener(l); }

	private TableCellRenderer myDefaultRenderer;
	private TableCellRenderer myHeaderRenderer;
	private NumberFormat myNumberFormat;
	private TableCellEditor myTableCellEditor;
	private double[] myData;
	private ProbabilityInterval[] myIntervals;
	private boolean myFlagEdited = false;
	private StainWright myStainWright;

	private JPanel myJPanel;
	private JLabel myLabelCurrent;
	private JLabel myLabelSuggested;

	public boolean isEdited()
	{
		return myFlagEdited;
	}

	/** @since 022805 */
	public void setEdited( boolean flag ){
		myFlagEdited = flag;
	}

	public boolean handlesProbabilities()
	{
		return true;
	}

	public String getDisplayName()
	{
		return "Sensitivity Suggestions";
	}

	public String toString()
	{
		return getDisplayName();
	}

	public Object getValueAt( int possibleIndex )
	{
		if( 0 <= possibleIndex && possibleIndex < myData.length )
		{
			Struct ret = new Struct( myNumberFormat.format( myData[ possibleIndex ] ) );
			if( ! myIntervals[ possibleIndex ].isEmpty() ) ret.suggested = myIntervals[ possibleIndex ].toString( myNumberFormat );
			return ret;
		}
		else
		{
			System.err.println( "SuggestionDataHandler bad data index: " + possibleIndex );
			return "!index";
		}
	}

	public static class Struct
	{
		public Struct( String current )
		{
			this.current = current;
		}

		public String current;
		public String suggested;
	}

	public void setValueAt( Object aValue, int linearIndex )
	{
	}
}
