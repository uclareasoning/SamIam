package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.*;
import edu.ucla.belief.io.dsl.DSLNodeType;
import edu.ucla.belief.sensitivity.*;
import edu.ucla.util.*;

import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.tabledisplay.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/**
	@author Keith Cascio
	@since 092903
*/
public class SuggestionListTableModel extends AbstractTableModel implements TableCellRenderer
{
	public SuggestionListTableModel()
	{
		init();
	}

	public void init()
	{
		myData = new ArrayList();

		myPanelBlank = new JPanel();

		myPanelError = new JPanel();
		myPanelError.add( new JLabel( "error" ) );
	}

	public void setSuggestions( Map singleCPTSuggestions )
	{
		Set keySet = singleCPTSuggestions.keySet();

		myVariableLabelRenderer.recalculateDisplayValues( keySet );

		myData.clear();
		myData.ensureCapacity( singleCPTSuggestions.size() );

		DisplayableFiniteVariable dVar;
		for( Iterator it = keySet.iterator(); it.hasNext(); )
		{
			dVar = (DisplayableFiniteVariable) it.next();
			myData.add( new Struct( dVar, (SingleCPTSuggestion) singleCPTSuggestions.get( dVar ) ) );
		}

		sortBy( LEXICOGRAPHIC );
	}

	public void clear()
	{
		myData.clear();
		fireTableDataChanged();
	}

	public void setDefaultTableCellRenderer( TableCellRenderer renderer )
	{
		myDefaultTableCellRenderer = renderer;
		myVariableLabelRenderer = new VariableLabelRenderer( renderer, null );
	}

	public String getColumnName( int column )
	{
		if( column == INT_INDEX_VARIABLE ) return "Variable";
		else if( column == INT_INDEX_LOG_ODDS ) return "Log-odds";//"Log-Odds Change";
		else return "getColumnName() Error";
	}

	public int getRowCount()
	{
		return myData.size();
	}

	public int getColumnCount()
	{
		return 2;
	}

	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
	}

	public DisplayableFiniteVariable getVariableAt( int rowIndex )
	{
		Struct struct = (Struct) myData.get( rowIndex );
		return struct.variable;
	}

	/**
		@author Keith Cascio
		@since 102003
	*/
	public SingleCPTSuggestion getSuggestionAt( int rowIndex )
	{
		Struct struct = (Struct) myData.get( rowIndex );
		return struct.table;
	}

	public Object getValueAt( int rowIndex, int column )
	{
		Struct struct = (Struct) myData.get( rowIndex );
		return struct.getValueAt(column);
	}

	public JComponent getDetails( int rowIndex )
	{
		Struct struct = (Struct) myData.get( rowIndex );
		return struct.getDetails();
	}

	/** @since 011305 */
	public void resizeDetails( int rowIndex ){
		Struct struct = (Struct) myData.get( rowIndex );
		struct.resizeDetails();
	}

	protected void showErrorMessage( String msg )
	{
		JOptionPane.showMessageDialog( null, msg, "SuggestionListTableModel error", JOptionPane.ERROR_MESSAGE );
	}

	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
	{
		column = table.convertColumnIndexToModel( column );

		if( column == INT_INDEX_VARIABLE )
		{
			return myVariableLabelRenderer.getTableCellRendererComponent( table, value, isSelected, false, row, column );
		}
		else if( column == INT_INDEX_LOG_ODDS )
		{
			return myDoubleTableCellRenderer.getTableCellRendererComponent( table, value, isSelected, false, row, column );
		}
		else return myPanelError;
	}

	public class Struct
	{
		public Struct( DisplayableFiniteVariable variable, SingleCPTSuggestion table )
		{
			this.variable = variable;
			this.table = table;
			this.logodds = new Double( table.getLogOddsChange() );
			this.intervals = table.probabilityIntervals();
		}

		public String toString()
		{
			return variable.toString();
		}

		public Object getValueAt( int column )
		{
			if( column == INT_INDEX_VARIABLE ) return variable;
			else if( column == INT_INDEX_LOG_ODDS ) return logodds;
			else return null;
		}

		/** @since 011305 */
		public void resizeDetails(){
			if( cpteditor != null ) cpteditor.resizeJTable();
		}

		public JComponent getDetails()
		{
			if( details == null )//wrapper == null )
			{
				CPTShell shell = variable.getCPTShell( DSLNodeType.CPT );
				FiniteVariable[] parents = shell.index().getParents();
				double[] data = shell.getCPT().dataclone();
				TableCellRenderer renderer = new DefaultTableCellRenderer();
				SuggestionDataHandler handler = new SuggestionDataHandler( data, intervals, renderer, renderer, null );
				//wrapper = myHuginGenieStyleTableFactory.makeCPTJComponent( variable, parents, data, (boolean[])null, false, (DataHandler)handler );
				//TableStyleEditor.resize( wrapper.table );
				cpteditor = new CPTEditor( (DisplayableFiniteVariableImpl)variable, variable.getNetworkInternalFrame() );
				cpteditor.setEditable( false );
				details = cpteditor.makeCPTDisplayComponent( handler );
				//cpteditor.resizeJTable();
			}
			//return wrapper.component;
			return details;
		}

		public DisplayableFiniteVariable variable;
		public SingleCPTSuggestion table;
		public ProbabilityInterval[] intervals;
		public Double logodds = DOUBLE_ZERO;
		//public HuginGenieStyleTableFactory.HuginGenieStyleJComponentWrapper wrapper;
		public CPTEditor cpteditor;
		public JComponent details;
	}
	//private HuginGenieStyleTableFactory myHuginGenieStyleTableFactory = new HuginGenieStyleTableFactory();

	public void sortBy( Comparator comp )
	{
		Collections.sort( myData, comp );
		fireTableDataChanged();
	}

	public void sort( int column )
	{
		Comparator comp = null;
		if( column == INT_INDEX_VARIABLE ) comp = LEXICOGRAPHIC;
		else if( column == INT_INDEX_LOG_ODDS ) comp = LOGODDS;
		sortBy( comp );
	}

	public static final Comparator LEXICOGRAPHIC = new Comparator()
	{
		public int compare( Object o1, Object o2 )
		{
			if( o1 instanceof Struct && o2 instanceof Struct )
			{
				return theComparator.compare( ((Struct)o1).variable, ((Struct)o2).variable );
			}
			return (int)0;
		}
	};
	private static final Comparator theComparator = VariableComparator.getInstance();

	public static final Comparator LOGODDS = new Comparator()
	{
		public int compare( Object o1, Object o2 )
		{
			if( o1 instanceof Struct && o2 instanceof Struct )
			{
				return ((Struct)o1).logodds.compareTo( ((Struct)o2).logodds );
			}
			return (int)0;
		}
	};

	private JPanel myPanelBlank;
	private JPanel myPanelError;
	private ArrayList myData;
	private TableCellRenderer myDefaultTableCellRenderer;
	private VariableLabelRenderer myVariableLabelRenderer;
	private DoubleTableCellRenderer myDoubleTableCellRenderer = new DoubleTableCellRenderer(new DoubleFormat(6));

	public static final Double DOUBLE_ZERO = new Double( (double)0 );
	public static final int INT_INDEX_VARIABLE = (int)0;
	public static final int INT_INDEX_LOG_ODDS = (int)1;
}
