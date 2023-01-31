package edu.ucla.belief.ui.tabledisplay;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.*;

import java.util.List;
import java.util.*;
import java.text.*;
import javax.swing.table.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;

/**
	This class constructs JTable objects
	for editing probability values of nodes with
	CPT style representation or noisy or style
	representation.  With the methods of the
	form make-style-JComponent(), this class
	will return a JComponent object (currently
	a JScrollPane) that is a fancier editing
	component than can be rendered with only
	a single JTable.

	@author Keith Cascio
	@since 043002
*/
public class HuginGenieStyleTableFactory
{
	/** Constructor */
	public HuginGenieStyleTableFactory(){}

	//** @since 102804 */
	//public HuginGenieStyleTableFactory( UI ui ){
	//	this.myUI = ui;
	//}

	public static final boolean FLAG_USE_PORTED_TABLE_MODEL = true;//UI.DEBUG;

	public static final int GENIE_SCROLLPANE_BACKGROUND_GREY_LEVEL = (int)128;
	public static final int GENIE_HEADER_CELL_BACKGROUND_GREY_LEVEL = (int)192;

	public static final Color COLOR_DEFAULT_BACKGROUND = new Color( GENIE_SCROLLPANE_BACKGROUND_GREY_LEVEL,GENIE_SCROLLPANE_BACKGROUND_GREY_LEVEL,GENIE_SCROLLPANE_BACKGROUND_GREY_LEVEL );
	public static final Color COLOR_DEFAULT_CELL_BACKGROUND = new Color( GENIE_HEADER_CELL_BACKGROUND_GREY_LEVEL,GENIE_HEADER_CELL_BACKGROUND_GREY_LEVEL,GENIE_HEADER_CELL_BACKGROUND_GREY_LEVEL );

	/**
		@author Keith Cascio
		@since 050102
	*/
	public JTable makeCPTJTable(	DisplayableFiniteVariable child,
					FiniteVariable[] parentsVars,
					double[] copyOfData )
	{
		return makeCPTJTable( child, parentsVars, copyOfData, true );
	}

	/**
		@author Keith Cascio
		@since 061902
	*/
	public JTable makeCPTJTable(	DisplayableFiniteVariable child,
					FiniteVariable[] parentsVars,
					double[] copyOfData,
					boolean editable )
	{
		this.myParentVars = parentsVars;
		this.myNumParents = myParentVars.length;

		TableModelHGS dTableModel = new HuginGenieStyleTableModel( child, parentsVars, copyOfData, editable );
		if( FLAG_USE_PORTED_TABLE_MODEL ) dTableModel = new PortedTableModelHGS( dTableModel );
		JTable ret = makeCPTJTable( dTableModel, null, false );
		if( FLAG_USE_PORTED_TABLE_MODEL ) ((PortedTableModelHGS)dTableModel).reset();

		return ret;
	}

	/**
		@author Keith Cascio
		@since 050102
	*/
	public JTable makeNoisyOrJTable(	FiniteVariable[] parentDVars,
						List states,
						List weights )
	{
		return makeNoisyOrJTable( parentDVars, states, weights, true );
	}

	/**
		@author Keith Cascio
		@since 061902
	*/
	public JTable makeNoisyOrJTable(	FiniteVariable[] parentDVars,
						List states,
						List weights,
						boolean editable )
	{
		this.myParentVars = parentDVars;
		this.myNumParents = myParentVars.length;

		TableModelHGS dTableModel = new NoisyOrTableModel(	parentDVars,
								states,
								weights,
								editable );
		return makeNoisyOrJTable( dTableModel, false, false );
	}

	/** @author keith cascio
		@since  20020619 */
	public interface TableModelHGS extends TableModel, TableCellEditor, TableCellRenderer
	{
		public int convertColumnIndexToConditionIndex(          int column );
		public int                 calculateDataIndex( int row, int column );
		public void              fireTableDataChanged();
		public void                       setEditable( boolean editable );
	  //public boolean              showProbabilities();
	  //public boolean              showExclude();
		public boolean                     hasExclude();
		public void                         configure( JTable jtable );
		public void                         configure( JTable jtable, DataHandler userHandler, boolean isHeader );
		public boolean            isProbabilityEdited();
		public void              setProbabilityEdited();
		public boolean            isExcludeDataEdited();
		public DataHandler[]          getDataHandlers();
		public void                   setDataHandler( DataHandler handler );
	  //computeLinearIndex
	}

	/**
		@author Keith Cascio
		@since 062002
	*/
	public static String toBinaryString( TableModel model )
	{
		String ret = "";
		String temp = null;
		Object val = null;
		for( int i=0; i< model.getRowCount(); i++ )
		{
			for( int j=0; j< model.getColumnCount(); j++ )
			{
				val = model.getValueAt( i, j );
				if( val instanceof Double )
				{
					temp = Long.toBinaryString( Double.doubleToLongBits( ((Double)val).doubleValue() ) );
				}
				else temp = val.toString();
				ret += temp + "\t";
			}
			ret += "\n";
		}

		return ret;
	}


	/** @since 20020619 */
	public class HuginGenieStyleJComponentWrapper
	{
		public JComponent component;
		public JTable table;
		public TableModelHGS model;

		public HuginGenieStyleJComponentWrapper( JComponent component,
							JTable table,
							TableModelHGS model )
		{
			this.component = component;
			this.table = table;
			this.model = model;
		}
	}

	/** @since 20020501 */
	public HuginGenieStyleJComponentWrapper makeCPTJComponent(	DisplayableFiniteVariable child,
									FiniteVariable[] parentsVars,
									double[] copyOfData )
	{
		return makeCPTJComponent( child, parentsVars, copyOfData, true );
	}

	/** @since 20020619 */
	public HuginGenieStyleJComponentWrapper makeCPTJComponent(	DisplayableFiniteVariable child,
									FiniteVariable[] parentsVars,
									double[] copyOfData,
									boolean editable )
	{
		return makeCPTJComponent( child, parentsVars, copyOfData, null, editable, null );
	}

	/** @since 20020619 */
	public HuginGenieStyleJComponentWrapper makeCPTJComponent(	DisplayableFiniteVariable child,
									FiniteVariable[] parentsVars,
									double[]         copyOfData,
									boolean[]        copyOfExcludeArray,
									boolean          editable,
									DataHandler      userHandler )
	{
		this.myParentVars         = parentsVars;
		this.myNumParents         = myParentVars.length;

		TableModelHGS dTableModel = new HuginGenieStyleTableModel( child,
									parentsVars,
									copyOfData,
									copyOfExcludeArray,
									editable );
		TableModelHGS               mainTableModel         = dTableModel;
		TableModelHGS             headerTableModel         = dTableModel;
		PortedTableModelHGS portedHeaderTableModel         = null, portedMainTableModel = null;
		if( FLAG_USE_PORTED_TABLE_MODEL ){
			  mainTableModel      =   portedMainTableModel = new PortedTableModelHGS( dTableModel );
			headerTableModel      = portedHeaderTableModel = new PortedTableModelHGS( dTableModel );
		}

		JTable   mainTable = makeCPTJTable(   mainTableModel, userHandler, /* isHeader */ false );
		JTable headerTable = makeCPTJTable( headerTableModel,        null, /* isHeader */  true );

		HuginGenieStyleJComponentWrapper ret = new HuginGenieStyleJComponentWrapper( makeLeftColumnLockedScrollPane( mainTable, headerTable ),
								mainTable,
								mainTableModel );

		if( FLAG_USE_PORTED_TABLE_MODEL ){
			  portedMainTableModel.reset();
			portedHeaderTableModel.reset();
		}

		return ret;
	}

	/** @since 20020501 */
	public HuginGenieStyleJComponentWrapper makeNoisyOrJComponent(	FiniteVariable[] parentDVars,
									List states,
									List weights )
	{
		return makeNoisyOrJComponent( parentDVars, states, weights, true );
	}

	/** @since 20020619 */
	public HuginGenieStyleJComponentWrapper makeNoisyOrJComponent(	FiniteVariable[] parentDVars,
									List states,
									List weights,
									boolean editable )
	{
		this.myParentVars = parentDVars;
		this.myNumParents = myParentVars.length;

		TableModelHGS dTableModel = new NoisyOrTableModel(	parentDVars,
									states,
									weights,
									editable );

		TableModelHGS mainTableModel = dTableModel;
		TableModelHGS headerTableModel = dTableModel;
		PortedTableModelHGS portedHeaderTableModel=null, portedMainTableModel=null;
		if( FLAG_USE_PORTED_TABLE_MODEL ){
			mainTableModel = portedMainTableModel = new PortedTableModelHGS( dTableModel );
			headerTableModel = portedHeaderTableModel = new PortedTableModelHGS( dTableModel );
		}

		JTable mainTable = makeNoisyOrJTable( mainTableModel, false, false );
		JTable headerTable = makeNoisyOrJTable( headerTableModel, false, true );

		HuginGenieStyleJComponentWrapper ret = new HuginGenieStyleJComponentWrapper( makeLeftColumnLockedScrollPane( mainTable, headerTable ),
								mainTable,
								mainTableModel );

		if( FLAG_USE_PORTED_TABLE_MODEL ){
			portedMainTableModel.reset();
			portedHeaderTableModel.reset();
		}

		return ret;
	}

	/** @since 20030611 */
	public HuginGenieStyleJComponentWrapper makePearlJComponent(	FiniteVariable[] parentDVars,
									List states,
									double[] weights,
									int[][] strengths )
	{
		return makePearlJComponent( parentDVars, states, weights, strengths, true );
	}

	/** @since 20030611 */
	public HuginGenieStyleJComponentWrapper makePearlJComponent(	FiniteVariable[] parentDVars,
									List states,
									double[] weights,
									int[][] strengths,
									boolean editable )
	{
		this.myParentVars = parentDVars;
		this.myNumParents = myParentVars.length;

		TableModelHGS dTableModel = new PearlTableModel(	parentDVars,
									states,
									weights,
									strengths,
									editable );

		TableModelHGS mainTableModel = dTableModel;
		TableModelHGS headerTableModel = dTableModel;
		PortedTableModelHGS portedHeaderTableModel=null, portedMainTableModel=null;
		if( FLAG_USE_PORTED_TABLE_MODEL ){
			mainTableModel = portedMainTableModel = new PortedTableModelHGS( dTableModel );
			headerTableModel = portedHeaderTableModel = new PortedTableModelHGS( dTableModel );
		}

		JTable mainTable = makeNoisyOrJTable( mainTableModel, true, false );
		JTable headerTable = makeNoisyOrJTable( headerTableModel, true, true );

		HuginGenieStyleJComponentWrapper ret = new HuginGenieStyleJComponentWrapper( makeLeftColumnLockedScrollPane( mainTable, headerTable ),
								mainTable,
								mainTableModel );

		if( FLAG_USE_PORTED_TABLE_MODEL ){
			portedMainTableModel.reset();
			portedHeaderTableModel.reset();
		}

		return ret;
	}

	/** @since 20020507 */
	public static Dimension columnHeaderSize( TableColumn col, JTable table )
	{
		TableCellRenderer renderer = col.getHeaderRenderer();

		if( renderer == null ){
			JTableHeader header = table.getTableHeader();
			if( header   != null ){ renderer =       header.getDefaultRenderer(); }
			if( renderer == null ){ return new Dimension( col.getMinWidth(), 0 ); }
		}

		Component comp = renderer.getTableCellRendererComponent( table, col.getHeaderValue(), false, false, 0, 0 );
		Dimension  ret = comp.getPreferredSize();
		return     ret;
	}

	/** @since 20080229 */
	static public String str( TableCellRenderer renderer ){
		if( renderer instanceof ForceBackgroundColorCellRenderer ){
			return "FBCCR( "+str(((ForceBackgroundColorCellRenderer)renderer).myDefaultRenderer)+" )";
		}
		else if( renderer instanceof HuginGenieStyleTableModel ){
			return "HGSTM( "+str(((HuginGenieStyleTableModel)renderer).getDataHandlers()[0])+" )";
		}
		else if( renderer instanceof ProbabilityDataHandler ){
			return "PDH( "+str(((ProbabilityDataHandler)renderer).getDefaultRenderer())+" )";
		}
		else{ return Util.id( renderer ).replaceAll( "edu.ucla.belief.ui.", "" ); }
	}

	/** @since 20080228 */
	static public class Measurements{
		public Measurements( int maxwidth, int maxheight, int minwidth, int minheight, int totalwidth, int totalheight ){
			this   .maxwidth  =   maxwidth;
			this   .maxheight =   maxheight;
			this   .minwidth  =   minwidth;
			this   .minheight =   minheight;
			this .totalwidth  = totalwidth;
			this .totalheight = totalheight;
			this .pref        = new Dimension( maxwidth, totalheight );
		}

		public Measurements repref(){
			pref.setSize( maxwidth, totalheight );
			return this;
		}

		public Dimension pref( int width, int height ){
			pref.setSize( width, height );
			return pref;
		}

		public String toString(){
			return "[ " + maxwidth + ", " + maxheight + ", " + minwidth + ", " + minheight + ", " + totalwidth + ", " + totalheight + ", " + str( pref ) + " ]";
		}

		public int
		  maxwidth,
		  maxheight,
		  minwidth,
		  minheight,
		  totalwidth,
		  totalheight;

		final public Dimension pref;
	}

	/** @since 20020507 */
	public static Measurements measure( TableColumn col, JTable table ){
		boolean debug = false;//table instanceof JTableHGS;
		if( debug ) trace( 5 );

		int               c               = table.convertColumnIndexToView( col.getModelIndex() ),
		                  maxw            = 0,   minw = Integer.MAX_VALUE,  totalw          = 0,
		                  maxh            = 0,   minh = Integer.MAX_VALUE,  totalh          = 0;
		TableCellRenderer defaultRenderer = table.getDefaultRenderer( Object.class ), renderer;
		int               rowCount        = table.getRowCount();
		Object            tempValue;
		Dimension         dim;
		Component         comp;

		if( debug ) System.out.println( "    defaultRenderer? " + str( defaultRenderer ) );

		for( int r=0; r < rowCount; ++r ){
			try{
				renderer = table.getCellRenderer( r, c );
			}catch( Exception e ){
				renderer = defaultRenderer;
			}

			tempValue = table.getValueAt( r, c );
			comp      = renderer.getTableCellRendererComponent( table, tempValue, false, false, r, c );
			dim       = comp.getPreferredSize();

			if( debug ) System.out.println( "      " + r + ": \""+tempValue+"\" = " + str( dim ) + " def rend? " + (renderer == defaultRenderer) );

			maxw      = dim .width > maxw ? dim .width : maxw;
			maxh      = dim.height > maxh ? dim.height : maxh;
			minw      = dim .width < minw ? dim .width : minw;
			minh      = dim.height < minh ? dim.height : minh;
			totalw   += dim .width;
			totalh   += dim.height;
		}

		Measurements ret = new Measurements( maxw, maxh, minw, minh, totalw, totalh );
		if( debug ) System.out.println( "    return " + ret );
		return    ret;
	}

	/** @since 050702 */
	public static int getPreferredWidthForColumn( TableColumn col, JTable table )
	{
		int hw =   columnHeaderSize( col, table ).width;
		int cw =            measure( col, table ).maxwidth;

		return hw > cw ? hw : cw;
	}

	/** @since 110204 election day */
	public static Measurements getPreferredSizeForColumn( TableColumn col, JTable table )
	{
		Dimension    hs =   columnHeaderSize( col, table );
		Measurements cs =            measure( col, table );
		cs.maxwidth     = Math.max( hs.width, cs.maxwidth );
		cs.totalheight += hs.height;
		return cs.repref();
	}

	private static final double ZERO = (double)0;
	private static final double ONE = (double)1;

	public static final double TOLERANCE = (double)0.0000000000000009;//9x10-17

	/**
		@author Keith Cascio
		@since 051302
	*/
	public static final boolean epsilonEqualsOne( double arg )
	{
		return Math.abs( ONE - arg ) < TOLERANCE;
	}

	/**
		@author Keith Cascio
		@since 051302
	*/
	public static final boolean epsilonGreaterThanOne( double arg )
	{
		return arg > (ONE + TOLERANCE);
	}

	/**
		@author Keith Cascio
		@since 050702
	*/
	public static String validate( FiniteVariable var, double[] values )
	{
		int varSize = var.size();
		//int numconditions = values.length / varSize;
		double sum = ZERO;
		try{
			for( int i = 0; i < values.length; i += varSize )
			{
				sum = sum( values, i, i+varSize );
				//if( !DisplayableFiniteVariable.FLAG_ALLOW_SUM_GT_ONE && ONE < sum ) throw new Exception( "Probabilities out of range: SUM="+sum+" > 1" );
				if( !DisplayableFiniteVariable.FLAG_ALLOW_SUM_GT_ONE && epsilonGreaterThanOne( sum ) ) throw new Exception( "Probabilities out of range: SUM="+sum+" > 1" );
				//if( sum != ONE ) return "Probabilities SUM="+sum+" do not sum to 1.";
				if( !epsilonEqualsOne( sum ) ) return "Probabilities SUM="+sum+" do not sum to 1.";
			}
		}catch( Exception e ){
			return e.getMessage();
		}
		return null;
	}

	/**
		@author Keith Cascio
		@since 050802
	*/
	public static String validate( FiniteVariable var, List values )
	{
		int varSize = var.size();
		//int numconditions = values.length / varSize;
		double sum = ZERO;
		try{
			for( int i = 0; i < values.size(); i += varSize )
			{
				sum = sum( values, i, i+varSize );
				//if( !DisplayableFiniteVariable.FLAG_ALLOW_SUM_GT_ONE && ONE < sum ) throw new Exception( "Probabilities out of range: SUM="+sum+" > 1" );
				if( !DisplayableFiniteVariable.FLAG_ALLOW_SUM_GT_ONE && epsilonGreaterThanOne( sum ) ) throw new Exception( "Probabilities out of range: SUM="+sum+" > 1" );
				//if( sum != ONE ) return "Probabilities SUM="+sum+" do not sum to 1.";
				if( !epsilonEqualsOne( sum ) ) return "Probabilities SUM="+sum+" do not sum to 1.";
			}
		}catch( Exception e ){
			return e.getMessage();
		}
		return null;
	}

	/**
		@author Keith Cascio
		@since 050702
	*/
	public static String ensureCPTProperty( FiniteVariable var, double[] values, double[] newValues )
	{
		for( int i=0; i<values.length; i++ ) newValues[i] = values[i];
		return normalize( var, newValues );
	}

	/** @since 050702 */
	public static double sum( double[] values, int startindex, int endindex ) throws Exception
	{
		//System.out.println( "HGSTF.sum( startindex? + "+startindex+", endindex? "+endindex+" )" );
		if( endindex <= startindex ) return ZERO;
		double ret = ZERO;
		while( startindex < endindex )
		{
			if( !DisplayableFiniteVariable.FLAG_ALLOW_NEGATIVE_TABLE_ENTRIES && values[startindex] < ZERO ) throw new Exception( "Probability out of range: negative." );
			if( !DisplayableFiniteVariable.FLAG_ALLOW_GTONE_TABLE_ENTRIES && ONE < values[startindex] ) throw new Exception( "Probability out of range: > 1." );
			ret += values[ startindex ];
			++startindex;
		}

		return ret;
	}

	/**
		@author Keith Cascio
		@since 062002
	*/
	protected static final NumberFormat theConstantFormat = new DecimalFormat( ".0###############################" );

	/**
		@author Keith Cascio
		@since 062002
	*/
	public static int maxFractionDigits( double[] values, int startindex, int endindex )
	{
		if( endindex <= startindex ) return (int)0;
		int ret = Integer.MIN_VALUE;
		int templen = (int)0;
		while( startindex < endindex )
		{
			templen = theConstantFormat.format( values[ startindex ] ).length() - (int)1;
			ret = Math.max( ret, templen );
			++startindex;
		}

		return ret;
	}

	/**
		@author Keith Cascio
		@since 050702
	*/
	public static double sum( List values, int startindex, int endindex ) throws Exception
	{
		if( endindex <= startindex ) return ZERO;
		double ret = ZERO;
		double current = ZERO;
		while( startindex < endindex )
		{
			current = ((Double)values.get(startindex)).doubleValue();
			if( !DisplayableFiniteVariable.FLAG_ALLOW_NEGATIVE_TABLE_ENTRIES && current < ZERO ) throw new Exception( "Probability out of range: negative." );
			if( !DisplayableFiniteVariable.FLAG_ALLOW_GTONE_TABLE_ENTRIES && ONE < current ) throw new Exception( "Probability out of range: > 1." );
			ret += current;
			++startindex;
		}

		return ret;
	}

	/**
		@author Keith Cascio
		@since 062002
	*/
	public static int maxFractionDigits( List values, int startindex, int endindex )
	{
		if( endindex <= startindex ) return (int)0;
		int ret = Integer.MIN_VALUE;
		int templen = (int)0;
		while( startindex < endindex )
		{
			templen = theConstantFormat.format( ((Double)values.get( startindex )).doubleValue() ).length() - (int)1;
			ret = Math.max( ret, templen );
			++startindex;
		}

		return ret;
	}

	/**
		@author Keith Cascio
		@since 050702
	*/
	public static String normalize( FiniteVariable var, double[] values )
	{
		int numconditions = values.length / var.size();
		String errmsg = null;
		for( int i=0; i<numconditions; i++ )
		{
			errmsg = normalize( var, values, i );
			if( errmsg != null ) return errmsg;
		}
		return null;
	}

	/** @since 051402 */
	public static String complement( FiniteVariable var, double[] values, int conditionIndex, int valueindex )
	{
		return complement( var, values, conditionIndex, valueindex, false );
	}

	/** @since 062002 */
	protected static NumberFormat theChangingFormat = NumberFormat.getNumberInstance();

	/** @since 052002 */
	public static String complement( FiniteVariable var, double[] values, int conditionIndex, int valueindex, boolean round )
	{
		//System.out.println( "HGSTF.complement( "+conditionIndex+", "+valueindex+" )" );
		if( conditionIndex < 0 )
		{
			System.err.println( "Warning: bad normalize( "+conditionIndex+" )" );
			return null;
		}

		int startindex1 = var.size() * conditionIndex;
		int endindex1 = startindex1 + valueindex;
		int startindex2 = endindex1 + 1;
		int endindex2 = startindex1 + var.size();

		String ret = complement( values, startindex1, endindex1, startindex2, endindex2 );
		if( ret != null || !round ) return ret;
		else
		{
			int max_fraction_digits = Math.max( maxFractionDigits( values, startindex1, endindex1 ),
								maxFractionDigits( values, startindex2, endindex2 ) );
			theChangingFormat.setMaximumFractionDigits( max_fraction_digits );
			String formatted = theChangingFormat.format( values[endindex1] );
			try{
				values[endindex1] = Double.parseDouble( formatted );
			}catch( NumberFormatException e ){
				System.err.println( "Warning: complement rounding failed." );
			}
		}

		return null;
	}

	/** @since 062002 */
	protected static String complement( double[] values, int startindex1, int endindex1, int startindex2, int endindex2 )
	{
		double sum1 = ZERO;
		double sum2 = ZERO;
		try{
			sum1 = sum( values, startindex1, endindex1 );
			sum2 = sum( values, startindex2, endindex2 );
		}catch( Exception e ){
			return e.getMessage();
		}
		double sum = sum1 + sum2;
		if( sum < ZERO ) return "Probabilities out of range: SUM < 0";
		if( sum > ONE ) return "Probabilities out of range: SUM > 1";

		values[endindex1] = 1 - sum;

		return null;
	}

	/**
		@author Keith Cascio
		@since 051402
	*/
	public static String complement( FiniteVariable var, List values, int conditionIndex, int valueindex )
	{
		return complement( var, values, conditionIndex, valueindex, false );
	}

	/**
		@author Keith Cascio
		@since 051402
	*/
	public static String complement( FiniteVariable var, List values, int conditionIndex, int valueindex, boolean round )
	{
		if( conditionIndex < 0 )
		{
			System.err.println( "Warning: bad normalize( "+conditionIndex+" )" );
			return null;
		}

		int startindex1 = var.size() * conditionIndex;
		int endindex1 = startindex1 + valueindex;
		int startindex2 = endindex1 + 1;
		int endindex2 = startindex1 + var.size();

		String ret = complement( values, startindex1, endindex1, startindex2, endindex2 );
		if( ret != null || !round ) return ret;
		else
		{
			int max_fraction_digits = Math.max( maxFractionDigits( values, startindex1, endindex1 ),
								maxFractionDigits( values, startindex2, endindex2 ) );
			theChangingFormat.setMaximumFractionDigits( max_fraction_digits );
			String formatted = theChangingFormat.format( ((Double)values.get( endindex1 )).doubleValue() );
			try{
				values.set( endindex1, new Double( Double.parseDouble( formatted ) ) );
			}catch( NumberFormatException e ){
				System.err.println( "Warning: complement rounding failed." );
			}
		}

		return null;
	}

	/**
		@author Keith Cascio
		@since 062002
	*/
	protected static String complement( List values, int startindex1, int endindex1, int startindex2, int endindex2 )
	{
		double sum1 = ZERO;
		double sum2 = ZERO;
		try{
			sum1 = sum( values, startindex1, endindex1 );
			sum2 = sum( values, startindex2, endindex2 );
		}catch( Exception e ){
			return e.getMessage();
		}
		double sum = sum1 + sum2;
		if( sum < ZERO ) return "Probabilities out of range: SUM < 0";
		if( sum > ONE ) return "Probabilities out of range: SUM > 1";

		values.set( endindex1, new Double( 1 - sum ) );

		return null;
	}

	/** @since 050702 */
	public static String normalize( FiniteVariable var, double[] values, int conditionIndex )
	{
		//System.out.println( "HGSTF.normalize( "+var+", "+conditionIndex+" )" );
		if( conditionIndex < 0 )
		{
			//System.out.println( "Java bad normalize( conditionIndex )" );//debug
			return null;
		}

		//System.out.print( "Java normalizing... " );//debug
		int startindex = var.size() * conditionIndex;
		int endindex = startindex + var.size();
		double sum = ZERO;
		try{
			sum = sum( values, startindex, endindex );
		}catch( Exception e ){
			return e.toString();
		}
		if( sum <= ZERO ) return "Probabilities out of range: SUM <= 0";

		while( startindex < endindex )
		{
			//System.out.print( values[ startindex ] + ", " );//debug
			values[ startindex ] = values[ startindex ]/sum;
			++startindex;
		}
		return (String) null;
	}

	/**
		@author Keith Cascio
		@since 050802
	*/
	public static String normalize( FiniteVariable var, List values, int conditionIndex )
	{
		if( conditionIndex < 0 )
		{
			//System.out.println( "Java bad normalize( conditionIndex )" );//debug
			return null;
		}

		//System.out.print( "Java normalizing... " );//debug
		int startindex = var.size() * conditionIndex;
		int endindex = startindex + var.size();
		double sum = ZERO;
		try{
			sum = sum( values, startindex, endindex );
		}catch( Exception e ){
			return e.getMessage();
		}
		if( sum <= ZERO ) return "Probabilities out of range: SUM <= 0";

		double current = ZERO;
		while( startindex < endindex )
		{
			current = ((Double)values.get( startindex )).doubleValue();
			//System.out.print( current + ", " );//debug
			values.set( startindex, new Double(current/sum) );
			++startindex;
		}
		return null;
	}

/**********************************************************************/
//private
/**********************************************************************/

	/** @since 20020501 */
	private JScrollPane makeLeftColumnLockedScrollPane( JTable mainTable, JTable headerTable )
	{
		boolean mmz = (mainTable instanceof JTableHGS) && ((JTableHGS) mainTable).minimize;//20080228

		JScrollPane         ret = new JScrollPane( mainTable );
		int         columnCount = mainTable.getColumnCount();

		if( FLAG_USE_PORTED_TABLE_MODEL ){
			PortedTableModel portedMainTableModel   = (PortedTableModel)   mainTable.getModel();
			portedMainTableModel.setOffsetFloorInclusive( 1 );
			PortedTableModel portedHeaderTableModel = (PortedTableModel) headerTable.getModel();
			portedHeaderTableModel           .setBreadth( 1 );
		}
		else{ TableColumnModel mainTCM = mainTable.getColumnModel();
			                   mainTCM.removeColumn( mainTCM.getColumn(0) ); }

		ret.setRowHeaderView( headerTable );

		JTableHeader mainTableHeader = mainTable.getTableHeader();
		if( mainTableHeader != null ){ mainTableHeader.setReorderingAllowed( false ); }

		JTableHeader headerTableHeader = headerTable.getTableHeader();
		if( headerTableHeader != null ){
			headerTableHeader  .setResizingAllowed( false );
			headerTableHeader.setReorderingAllowed( false );
			ret.setCorner( JScrollPane.UPPER_LEFT_CORNER, headerTableHeader );
		}

		TableColumnModel  headerColumnModel = headerTable.getColumnModel();
		TableColumn       firstHeaderColumn = headerColumnModel.getColumn(0);
		int widthColumn = firstHeaderColumn.getPreferredWidth() + headerColumnModel.getColumnMargin();
	  //int widthColumn = getPreferredWidthForLeftmostColumn( firstHeaderColumn, headerTable ) + headerColumnModel.getColumnMargin();
	  //int widthColumn = firstHeaderColumn.getMinWidth() + headerColumnModel.getColumnMargin();
	  //System.out.println( "    leftmost width " + widthColumn );
		headerTable.setPreferredScrollableViewportSize( new Dimension( widthColumn, 0 ) );

		setDefaultBackgroundColor( ret );
		setDefaultBackgroundColor( ret.getViewport() );
		setDefaultBackgroundColor( ret.getRowHeader() );
	  //setDefaultBackgroundColor( ret.getColumnHeader() );
	  //setDefaultBackgroundColor( ret.getCorner( JScrollPane.UPPER_RIGHT_CORNER ) );

		Dimension    sizeWindow = Util.getScreenBounds().getSize(), copeWithLargeTables = null,
		           //preferred  = ret.getPreferredSize(),
		             leftmost   = getPreferredSizeForLeftmostColumn( firstHeaderColumn, headerTable );
		int          heightPain = Math.min( leftmost.height/*preferred.height*/, sizeWindow.height - 0x40 );
		int          widthPain  = Math.min( columnCount * 0x40,                  sizeWindow.width  - 0x40 );
		if( ! mmz ){ widthPain  = Math.max( widthPain,    0x100 ); }

		ret.setPreferredSize( copeWithLargeTables = new Dimension( widthPain, heightPain ) );
	  //System.out.println( " copeWithLargeTables: " + copeWithLargeTables );
		mainTable.setRowHeight( headerTable.getRowHeight() );

		return ret;
	}

	private static void setDefaultBackgroundColor( Component comp )
	{
		if( comp == null )
		{
			//StackTraceElement[] els = (new Throwable()).getStackTrace();//only jdk1.4
			System.err.println( "Java warning.  Tried to set background color for null component." );
			//System.err.println( els[els.length-1) );//only jdk1.4

		}
		else comp.setBackground( COLOR_DEFAULT_BACKGROUND );
	}

	/** @since 20020501 */
	private JTable makeCPTJTable( TableModelHGS tm, DataHandler userHandler, boolean isHeader )
	{
		JTable ret = makeGenericTable( tm, userHandler, isHeader );

		JTableHeader dTableHeader = makeHeader( ret );
		ret.setTableHeader( dTableHeader );
		//setDefaultBackgroundColor( dTableHeader );

		if( dTableHeader != null )
		{
			ret.setRowHeight( dTableHeader.getDefaultRenderer().getTableCellRendererComponent(
      				ret, "Test",false, false, 0, 0).getPreferredSize().height );
		}

		TableColumn leftmost = ret.getColumnModel().getColumn(0);
		leftmost.setMinWidth( getPreferredSizeForLeftmostColumn( leftmost, ret ).width );

		return ret;
	}

	/** @since 20080228 */
	static public String str( Dimension dimension ){
		return "[ " + dimension.width + ", " + dimension.height + " ]";
	}

	/** @since 20080228 */
	static public void trace( int depth ){
		StackTraceElement[] elements = new Throwable().getStackTrace();
		for( int i=1; i<=depth; i++ ){
			Util.STREAM_DEBUG.println( elements[i].toString().replaceAll( "edu.ucla.belief.ui.tabledisplay.HuginGenieStyleTableFactory.", "HGSTF." ).replaceAll( "HuginGenieStyleTableFactory.java", "HGSTF" ) );
		}
	}

	/** @since 20041027 */
	public static Dimension getPreferredSizeForLeftmostColumn( TableColumn leftmost, JTable table )
	{
		Measurements me  = getPreferredSizeForColumn( leftmost, table );
		table.setRowHeight( me.maxheight );
		Dimension ret    = new Dimension( me.pref );
		ret.width  += 4;
		ret.height += 2;

		TableCellRenderer renderer = leftmost.getHeaderRenderer();
		if((renderer == null) && (table.getTableHeader() != null)){ renderer = table.getTableHeader().getDefaultRenderer(); }

		if( renderer != null ){
			GroupableTableColumnModel  gtcm      = (GroupableTableColumnModel) table.getColumnModel();
			Iterator                   iter      = gtcm.getColumnGroups( leftmost );
			if( iter == null ){ return ret; }
			ColumnGroup                group     = null;
			int                        prefWidth = 0;
			Dimension                  dim;
			while( iter.hasNext() ){
				group = (ColumnGroup) iter.next();
				dim = renderer.getTableCellRendererComponent( table, group.getHeaderValue(), false, false, 0, 0 ).getPreferredSize();
				ret .width  = Math.max( ret.width, dim.width );
				ret.height += dim.height;
			}
		}

		return ret;
	}

	/** @since 20020501 */
	private JTable makeGenericTable( TableModelHGS tm, DataHandler userHandler, boolean isHeader )
	{
		return new JTableHGS( tm, userHandler, isHeader ).minimize( userHandler == null ? true : userHandler.minimize() );
	}

	/** @since 20050322 */
	public static class JTableHGS extends JTable
	{
		public JTableHGS( TableModelHGS tm, DataHandler userHandler, boolean isHeader )
		{
			super();
			JTable ret = this;//new JTable();
			GroupableTableColumnModel tcm = myGroupableTableColumnModel = new GroupableTableColumnModel();
			if( tm instanceof PortedTableModelHGS ) tcm.setPortedTableModelHGS( (PortedTableModelHGS)tm );
			ret.setColumnModel( tcm );
			ret.setModel( tm );

			JTableHeader header = ret.getTableHeader();
		  //if( header instanceof GroupableTableHeader || isHeader ){
				header.setDefaultRenderer( ( new ForceBackgroundColorCellRenderer( header.getDefaultRenderer() ) ) );
		  //}

			int intAutoResize = JTable.AUTO_RESIZE_OFF;
			if( FLAG_USE_PORTED_TABLE_MODEL && (!isHeader) ) intAutoResize = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS;
			ret.setAutoResizeMode( intAutoResize );
			ret.setCellSelectionEnabled( true );
		  //ret.setCellSelectionEnabled( false );
			ret.setColumnSelectionAllowed( true );
			ret.setRowSelectionAllowed( false );

			tm.configure( ret, userHandler, isHeader );

			setDefaultBackgroundColor( header );
		}

		public void changeSelection( int rowIndex, int columnIndex, boolean toggle, boolean extend ){
			//System.out.println( "JTableHGS.cs( "+rowIndex+", "+columnIndex+", toggle? "+toggle+", extend? "+extend+" )" );
			super.changeSelection( rowIndex, columnIndex, toggle, extend );
			if( !(extend || toggle) ) myGroupableTableColumnModel.clearSelectedState();
		}

		/** @since 20080228 */
		public JTableHGS minimize( boolean minimize ){
			this.minimize = minimize;
			return this;
		}

		private GroupableTableColumnModel myGroupableTableColumnModel;
		public  boolean                   minimize = false;
	}

	/** @author keith cascio
		@since  2002050102 */
	public static class HuginGenieStyleCellRenderer implements TableCellRenderer
	{
		private TableCellRenderer myDefaultRenderer = null;
		private TableCellRenderer myHeaderRenderer = null;
		private JCheckBox myJCheckBox;
		private JPanel myJPanel;

		public HuginGenieStyleCellRenderer( TableCellRenderer defaultRenderer, TableCellRenderer headerRenderer )
		{
			myDefaultRenderer = defaultRenderer;
			myHeaderRenderer = headerRenderer;
			//GridBagLayout layout = new GridBagLayout();
			//GridLayout layout = new GridLayout();
			FlowLayout layout = new FlowLayout();
			layout.setHgap( 0 );
			layout.setVgap( 0 );
			layout.setAlignment( FlowLayout.CENTER );
			myJPanel = new JPanel( layout );
			myJCheckBox = new JCheckBox();
			//myJPanel.add( myJCheckBox, new GridBagConstraints() );
			myJPanel.add( myJCheckBox );
			//myJPanel.setBorder( BorderFactory.createLineBorder( Color.red, 1 ) );
			//myJCheckBox.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );
			//myJPanel.setBorder( BorderFactory.createEmptyBorder( 0,0,0,0 ) );
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if( value instanceof Double )
			{
				Component ret = myDefaultRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
				return ret;
			}
			else if( value instanceof Boolean )
			{
				myJCheckBox.setSelected( ((Boolean)value).booleanValue() );
				return myJPanel;
			}
			else return myHeaderRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
		}
	}

	/** @author keith cascio
		@since  20020503 */
	public static class ForceBackgroundColorCellRenderer implements TableCellRenderer
	{
		private TableCellRenderer myDefaultRenderer;
		private Color myColor;

		public ForceBackgroundColorCellRenderer( TableCellRenderer defaultRenderer )
		{
			this( defaultRenderer, COLOR_DEFAULT_CELL_BACKGROUND );
		}

		public ForceBackgroundColorCellRenderer( TableCellRenderer defaultRenderer, Color color )
		{
			myDefaultRenderer = defaultRenderer;
			myColor = color;
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Component ret = myDefaultRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
			ret.setBackground( myColor );
			return ret;
		}
	}

	/** @author keith cascio
		@since  20020501 */
	private JTable makeNoisyOrJTable( TableModelHGS tm, boolean pearl, boolean isHeader )
	{
		JTable ret = makeGenericTable( tm, null, isHeader );

		JTableHeader dTableHeader = makeNoisyOrHeader( ret, pearl );
		ret.setTableHeader( dTableHeader );

		if( dTableHeader != null )
		{
			ret.setRowHeight( dTableHeader.getDefaultRenderer().getTableCellRendererComponent(
      				ret, "Test",false, false, 0, 0).getPreferredSize().height );
		}

		TableColumn leftmost = ret.getColumnModel().getColumn(0);
		leftmost.setMinWidth( getPreferredSizeForLeftmostColumn( leftmost, ret ).width );
		return ret;
	}

	/** @author keith cascio
		@since  20020501 */
	private JTableHeader makeNoisyOrHeader( JTable theTable, boolean pearl )
	{
		//System.out.println( "HGSTF.makeNoisyOrHeader()" );

		if( myNumParents == 0 ) return theTable.getTableHeader();
		//TableColumnModel tcm = theTable.getColumnModel();
		GroupableTableColumnModel tcm = (GroupableTableColumnModel) theTable.getColumnModel();
		GroupableTableHeader ret = new GroupableTableHeader( tcm );

		PortedTableModelHGS portedtablemodelhgs = null;
		TableModel tablemodel = theTable.getModel();
		if( tablemodel instanceof PortedTableModelHGS ) portedtablemodelhgs = (PortedTableModelHGS) tablemodel;

		ColumnGroup tempColumnGroup = null;
		int index = 0;
		int sizeParentEffective;
		int numColumns = theTable.getModel().getColumnCount();
		tempColumnGroup = new ColumnGroup( "parent nodes" );

		//System.out.println( "    numColumns " + numColumns + ", myParentVars.length " + myParentVars.length );
		//System.out.println( "    tcm.getColumnCount() " + tcm.getColumnCount() );

		if( portedtablemodelhgs != null ) portedtablemodelhgs.setColumnGroup( index, tempColumnGroup );
		tempColumnGroup.add( tcm.getColumn(index++) );
		//ret.addColumnGroup( tempColumnGroup );
		tcm.addColumnGroup( tempColumnGroup );

		for( int i=0; i<myParentVars.length && index<numColumns; i++ )
		{
			tempColumnGroup = new ColumnGroup( myParentVars[i].toString() );
			sizeParentEffective = myParentVars[i].size();
			if( !pearl ) --sizeParentEffective;
			for( int j=0; j<sizeParentEffective; j++ )
			{
				if( portedtablemodelhgs != null ) portedtablemodelhgs.setColumnGroup( index, tempColumnGroup );
				tempColumnGroup.add( tcm.getColumn(index++) );
			}
			//ret.addColumnGroup( tempColumnGroup );
			tcm.addColumnGroup( tempColumnGroup );
		}

		if( (index < tcm.getColumnCount()) && (index >= 0) ){
			tempColumnGroup = new ColumnGroup( " " );
			if( portedtablemodelhgs != null ) portedtablemodelhgs.setColumnGroup( index, tempColumnGroup );
			tempColumnGroup.add( tcm.getColumn(index) );
			//ret.addColumnGroup( tempColumnGroup );
			tcm.addColumnGroup( tempColumnGroup );
		}
		//else //the row header case

		ret.setDefaultRenderer( ( new ForceBackgroundColorCellRenderer( ret.getDefaultRenderer() ) ) );
		setDefaultBackgroundColor( ret );

		return ret;
	}

	/** @author keith cascio
		@since  20020501 */
	private JTableHeader makeHeader( JTable theTable )
	{
		if(      myNumParents == 0 ){ return null; }
		else if( myNumParents == 1 ){ return theTable.getTableHeader(); }
		else{
			boolean flagNotRowheader = !( theTable.getColumnModel().getColumnCount() == (int)1 );

			GroupableTableColumnModel tcm           = (GroupableTableColumnModel) theTable.getColumnModel();
			GroupableTableHeader      ret           = new GroupableTableHeader( tcm );

		  //System.out.println( "    tcm.getColumnCount() " + tcm.getColumnCount() );

			TableModel                tablemodel    = theTable.getModel();
			PortedTableModelHGS       ptmhgs        = tablemodel instanceof PortedTableModelHGS ? ((PortedTableModelHGS) tablemodel) : null;

			List           lastRowColumnGroups      = new LinkedList();

			FiniteVariable ultimateParent           = myParentVars[ myParentVars.length - 1 ];
			FiniteVariable secondToLastParent       = myParentVars[ myParentVars.length - 2 ];
			int            ultimateParentSize       =     ultimateParent.size();
			int            secondToLastParentDegree = secondToLastParent.size();

			int            counter                  = 0;
			ColumnGroup lastParentNameColumnGroup   = new ColumnGroup( secondToLastParent.toString() );
			if( ptmhgs != null ){ ptmhgs.setColumnGroup( counter, lastParentNameColumnGroup ); }
			lastParentNameColumnGroup.add( tcm.getColumn(counter++) );

			ColumnGroup tempColumnGroup = null;
			int stateindex = 0;

			if( flagNotRowheader ){
			while( counter < theTable.getModel().getColumnCount() )
			{
				tempColumnGroup = new ColumnGroup( secondToLastParent.instance(stateindex++).toString() );
				for( int i=0; i<ultimateParentSize; i++ )
				{
					if( ptmhgs != null ) ptmhgs.setColumnGroup( counter, tempColumnGroup );
					tempColumnGroup.add( tcm.getColumn( counter++ ) );
				}
				lastRowColumnGroups.add( tempColumnGroup );
				if( stateindex == secondToLastParentDegree ) stateindex = 0;
			}
			}

			List currentRowColumnGroups = null;
			Iterator groupIt = null;
			int currentParentDegree = (int)0;

			if( flagNotRowheader ) currentRowColumnGroups = new LinkedList();

			for( int i=myNumParents-3; i >= 0; i-- )
			{
				tempColumnGroup = new ColumnGroup( myParentVars[i].toString() );
				tempColumnGroup.add( lastParentNameColumnGroup );
				lastParentNameColumnGroup = tempColumnGroup;

				if( flagNotRowheader ){
					secondToLastParent = myParentVars[ i ];
					currentParentDegree = secondToLastParent.size();
					stateindex = 0;
					for( groupIt = lastRowColumnGroups.iterator(); groupIt.hasNext(); )
					{
						tempColumnGroup = new ColumnGroup( secondToLastParent.instance(stateindex++).toString() );
						for( int j=0; j<secondToLastParentDegree; j++ )
						{
							tempColumnGroup.add( (ColumnGroup) groupIt.next() );
						}
						currentRowColumnGroups.add( tempColumnGroup );
						if( stateindex == currentParentDegree ) stateindex = 0;
					}

					secondToLastParentDegree = currentParentDegree;
					lastRowColumnGroups = currentRowColumnGroups;
					currentRowColumnGroups = new LinkedList();
				}
			}

			//ret.addColumnGroup( lastParentNameColumnGroup );
			tcm.addColumnGroup( lastParentNameColumnGroup );

			if( flagNotRowheader ){
			for( Iterator it = lastRowColumnGroups.iterator(); it.hasNext(); )
			{
				//ret.addColumnGroup( (ColumnGroup) it.next() );
				tcm.addColumnGroup( (ColumnGroup) it.next() );
			}
			}

			ret.setDefaultRenderer( ( new ForceBackgroundColorCellRenderer( ret.getDefaultRenderer() ) ) );
			setDefaultBackgroundColor( ret );

			return ret;
		}
	}


	private FiniteVariable[] myParentVars = null;
	private int myNumParents = (int)0;
	//private UI myUI;



/*
	private void initParentDegrees()
	{
		myParentDegrees = new int[ myNumParents ];
		int currentDegree = (int)0;
		for( int i=myNumParents-1; i >= 0; i-- )
		{
			currentDegree = myParentVars[i].getFiniteVariable().size();
			myParentDegrees[i] = currentDegree;
		}
	}

	private int[] myParentDegrees = null;
*/


/*
	public JScrollPane makeComponentOld(	DisplayableFiniteVariable child,
						FiniteVariable[] parentsVars,
						double[] copyOfData )
	{
		this.myParentVars = parentsVars;
		this.myNumParents = myParentVars.length;

		initParentDegrees();

		TableModel dTableModel = new HuginGenieStyleTableModel( child, parentsVars, copyOfData, true );
		if( FLAG_USE_PORTED_TABLE_MODEL ) dTableModel = new PortedTableModelHGS( dTableModel );

		JTable mainTable = new JTable( dTableModel );
		JTable headerTable = new JTable( dTableModel );
		//JTableHeader dTableHeader = makeHeader( mainTable );
		mainTable.setTableHeader( makeHeader( mainTable ) );
		headerTable.setTableHeader( makeHeader( headerTable ) );

		mainTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		headerTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		headerTable.getTableHeader().setReorderingAllowed( false );

		JScrollPane ret = new JScrollPane( mainTable );

		TableColumnModel mainTCM = mainTable.getColumnModel();
		TableColumn firstColumn = mainTCM.getColumn(0);
		mainTCM.removeColumn( firstColumn );
		ret.setRowHeaderView( headerTable );
		ret.setCorner( JScrollPane.UPPER_LEFT_CORNER, headerTable.getTableHeader() );

		headerTable.setPreferredScrollableViewportSize( new Dimension( firstColumn.getPreferredWidth() + headerTable.getColumnModel().getColumnMargin(), 0 ) );

		TableCellRenderer headerRenderer = mainTable.getTableHeader().getDefaultRenderer();
		headerTable.getColumnModel().getColumn(0).setCellRenderer( headerRenderer );

		int ROWHEIGHT = headerRenderer.getTableCellRendererComponent(
      			mainTable, "Test",false, false, 0, 0).getPreferredSize().height;

		mainTable.setRowHeight( ROWHEIGHT );
		headerTable.setRowHeight( ROWHEIGHT );

		return ret;
	}
*/
}
