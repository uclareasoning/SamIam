package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.event.*;

import edu.ucla.belief.*;
import edu.ucla.util.ImpactProperty;
import edu.ucla.util.EnumValue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import javax.swing.border.*;
import java.text.*;

/**
	@author Keith Cascio
	@since 073003
*/
public class ImpactTableModel extends AbstractTableModel implements Comparator
{
	public ImpactTableModel( BeliefNetwork bn )
	{
		myBeliefNetwork = bn;
		myEvidenceController = myBeliefNetwork.getEvidenceController();
		init();
	}

	public void init()
	{
		boolean flagDefault = ImpactProperty.PROPERTY.toBoolean( ImpactProperty.PROPERTY.getDefault() );

		if( myImpactVariables == null ) myImpactVariables = new HashSet( myBeliefNetwork.size() );
		else myImpactVariables.clear();

		FiniteVariable fVar;
		EnumValue value;
		for( Iterator it = myBeliefNetwork.iterator(); it.hasNext(); )
		{
			fVar = (FiniteVariable)it.next();
			value = fVar.getProperty( ImpactProperty.PROPERTY );
			if( ( value == null && flagDefault ) || value == ImpactProperty.PROPERTY.TRUE ) myImpactVariables.add( fVar );
		}

		int length = myImpactVariables.size();
		myData = new Struct[ length ];

		Iterator it = myImpactVariables.iterator();
		for( int i = 0; i< length && it.hasNext(); i++ ) myData[i] = new Struct( (FiniteVariable) it.next() );

		setBackground( Color.green );

		fireTableDataChanged();
	}

	public boolean contains( Variable var )
	{
		return ( myImpactVariables != null && myImpactVariables.contains( var ) );
	}

	public String getColumnName( int column )
	{
		if( column == 0 ) return "Variables, sorted by magnitude of impact";
		else if( column == 1 ) return "Impact";
		else return "getColumnName() Error";
	}

	public int getRowCount()
	{
		return myData.length;
	}

	public int getColumnCount()
	{
		return 2;
	}

	public Object getValueAt( int rowIndex, int columnIndex )
	{
		Struct ret = myData[rowIndex];
		if( columnIndex == 0 ) return ret.variable;
		else if( columnIndex == 1 ) return distanceMeasureToString( ret );
		else return null;
	}

	public void setBackground( Color c )
	{
		if( myStructRenderer != null ) myStructRenderer.setBackground( c );
	}

	public void setInferenceEngine( InferenceEngine ie )
	{
		myInferenceEngine = ie;
		setBackground( Color.green );
	}

	public void computeAllMarginals()
	{
		for( int i=0; i<myData.length; i++ )
		{
			myData[i].marginal = myInferenceEngine.conditional( myData[i].variable );
			myData[i].distanceMeasure = DOUBLE_ZERO;
		}
	}

	public void sort( EvidenceChangeEvent evt )
	{
		//System.out.println( "ImpactTableModel.sort( "+evt+" )" );
		myRecentEvidenceChangeVariables.clear();
		myRecentEvidenceChangeVariables.addAll( evt.recentEvidenceChangeVariables );
		myMapEvidence = myEvidenceController.evidence();
		setValid( false );
		Arrays.sort( myData, this );
		setBackground( null );
		fireTableDataChanged();
	}

	private void setValid( boolean flag )
	{
		for( int i=0; i<myData.length; i++ ) myData[i].flagValid = flag;
	}

	public int compare( Object o1, Object o2 )
	{
		if( o1 instanceof Struct && o2 instanceof Struct && myInferenceEngine != null )
		{
			Struct struct1 = (Struct)o1;
			Struct struct2 = (Struct)o2;

			double distanceMeasure1 = (double)0;
			double distanceMeasure2 = (double)0;

			if( struct1.flagValid ) distanceMeasure1 = struct1.distanceMeasure.doubleValue();
			else
			{
				Table newMarginal1 = myInferenceEngine.conditional( struct1.variable );
				distanceMeasure1 = struct1.marginal.distanceMeasure( newMarginal1 );
				struct1.marginal = newMarginal1;
				struct1.distanceMeasure = new Double( distanceMeasure1 );
				struct1.flagValid = true;
			}

			if( struct2.flagValid ) distanceMeasure2 = struct2.distanceMeasure.doubleValue();
			else
			{
				Table newMarginal2 = myInferenceEngine.conditional( struct2.variable );
				distanceMeasure2 = struct2.marginal.distanceMeasure( newMarginal2 );
				struct2.marginal = newMarginal2;
				struct2.distanceMeasure = new Double( distanceMeasure2 );
				struct2.flagValid = true;
			}

			return Double.compare( distanceMeasure2, distanceMeasure1 );
		}
		else return (int)0;
	}

	private Struct[] myData;
	private BeliefNetwork myBeliefNetwork;
	private Collection myImpactVariables;
	private EvidenceController myEvidenceController;
	private Map myMapEvidence = Collections.EMPTY_MAP;
	private Collection myRecentEvidenceChangeVariables = new ArrayList( 50 );
	private InferenceEngine myInferenceEngine;
	private StructRenderer myStructRenderer;

	public void configure( JTable table )
	{
		myStructRenderer = new StructRenderer( table.getDefaultRenderer( Object.class ), myBeliefNetwork, table );
		table.setDefaultRenderer( Object.class, myStructRenderer );
	}

	private String distanceMeasureToString( Struct struct )
	{
		if( myRecentEvidenceChangeVariables.contains( struct.variable ) ) return "evidence changed";
		else if( myMapEvidence.containsKey( struct.variable ) ) return "evidence unchanged";
		else return FORMAT.format( struct.distanceMeasure );
	}

	public static final double ZERO = (double)0;
	public static final Double DOUBLE_ZERO = new Double( ZERO );
	public static final Double DOUBLE_NEGATIVE_INFINITY = new Double( Double.NEGATIVE_INFINITY  );
	public static final Double DOUBLE_POSITIVE_INFINITY = new Double( Double.POSITIVE_INFINITY );
	public static final NumberFormat FORMAT = new DecimalFormat( "0.0####" );

	public static class Struct
	{
		public Struct( FiniteVariable variable )
		{
			this.variable = variable;
		}

		public String distanceMeasureToString()
		{
			if( distanceMeasure.equals( DOUBLE_NEGATIVE_INFINITY ) ) return "observation unchanged";
			else if( distanceMeasure.equals( DOUBLE_POSITIVE_INFINITY ) ) return "observation changed";
			else return FORMAT.format( distanceMeasure );
		}

		public FiniteVariable variable;
		public Table marginal;
		public Double distanceMeasure = DOUBLE_ZERO;
		public boolean flagValid = false;
	}

	public static class StructRenderer implements TableCellRenderer
	{
		public StructRenderer( TableCellRenderer r, Collection variables, JTable table )
		{
			myTableCellRenderer = new VariableLabelRenderer( r, variables );
			myJTable = table;
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Object decoratedValue = value;
			if( value instanceof Struct )
			{
				if( column == 0 ) decoratedValue = ((Struct)value).variable;
				else if( column == 1 ) decoratedValue = ((Struct)value).distanceMeasure;
			}
			Component ret = myTableCellRenderer.getTableCellRendererComponent( table, decoratedValue, isSelected, hasFocus, row, column );
			//if( myBackground != null ) ret.setBackground( myBackground );
			return ret;
		}

		public void setBackground( Color c )
		{
			myBackground = c;
			Color toSet = ( c == null ) ? Color.white : c;
			myTableCellRenderer.getTableCellRendererComponent( myJTable, "test", false, false, 0, 0 ).setBackground( toSet );
		}

		private TableCellRenderer myTableCellRenderer;
		private JTable myJTable;
		private Color myBackground = Color.white;
	}
}
