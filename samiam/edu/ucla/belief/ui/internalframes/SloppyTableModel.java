package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.Util;
//import edu.ucla.belief.ui.*;
//import edu.ucla.belief.ui.event.*;

import edu.ucla.belief.*;
import edu.ucla.belief.inference.map.ExactMap;
import il2.inf.map.MapSearch;

import java.util.*;
import java.util.List;
import java.awt.*;
//import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
//import javax.swing.border.*;
//import java.text.*;

/**
	@author Keith Cascio
	@since 092104
*/
public class SloppyTableModel extends AbstractTableModel// implements SloppyPanel.Jumpy
{
	public SloppyTableModel( MapSearch.MapInfo results, double PrE, List variables )
	{
		myMapInfo = results;
		myData = results.results;
		myPrE = PrE;
		myVariables = variables;
		refreshMapping();
		init();
	}

	public static final Color COLOR_SOLUTION_NUMBERS = Color.blue;
	//public static final String STR_HTML_COLOR_SOLUTION_NUMBERS = Util.htmlEncode( COLOR_SOLUTION_NUMBERS );
	public static final String STR_HTML_HEADER_PRE = "<html><nobr><font color=\"#" + Util.htmlEncode( COLOR_SOLUTION_NUMBERS ) + "\">";
	public static final String STR_HTML_HEADER_IN = "</font>: ";

	/** @since 092404 */
	public void refreshMapping()
	{
		if( (myMapping == null) || (myMapping.length != myData.size()) ) myMapping = new int[myData.size()];
		synchronized( mySynch )
		{
		if( myLength != myData.size() )
		{
			for( int i=0; i<myMapping.length; i++ ) myMapping[i] = i;
			myLength = myData.size();
			fireTableStructureChanged();
		}
		}
	}

	/** @since 20040924 */
	public void hideColumns( int anchor, int lead )
	{
		hideColumnsImpl( anchor, lead );
		fireTableStructureChanged();
	}

	/** @since 20051018 */
	private void hideColumnsImpl( int anchor, int lead ){
		//System.out.println( "SloppyTableModel.hideColumnsImpl( "+anchor+", "+lead+" )" );
		synchronized( mySynch )
		{
		if( lead < anchor ) throw new IllegalArgumentException( "lead{"+lead+"} < anchor{"+anchor+"}" );
		else if( (anchor < 1) || (lead < 1) ) throw new IllegalArgumentException( "lead{"+lead+"} < 1 || anchor{"+anchor+"} < 1" );

		int num = Math.abs( lead - anchor ) + 1;
		int indexStart = (anchor - 1) + myOffset;
		int newLength = myLength - num;
		for( int i=indexStart; i<newLength; i++ ) myMapping[i] = myMapping[i+num];
		myLength = newLength;
		myBreadth = Math.min( myBreadth, myLength );
		myOffset = Math.min( myOffset, myLength-myBreadth );
		//System.out.print( "\t myMapping [ " );
		//for( int i=0; i<myMapping.length; i++ ) System.out.print( myMapping[i] + ", " );
		//System.out.println( " ]" );
		//System.out.println( "\t myLength " + myLength + ", myBreadth " + myBreadth + ", myOffset " + myOffset );
		}
	}

	/** @since 20051018 */
	public void hideColumns( int[] columns ){
		if( columns == null ) return;
		int len = columns.length;
		if( len < 1 ) return;

		try{
			int i = len - 1;
			int anchor = -1, lead = -1;
			anchor = lead = columns[i];
			if( len < 2 ) hideColumnsImpl( anchor, lead );
			else{
				Arrays.sort( columns );
				anchor = lead = columns[i];
				for(; i>=0; i-- ){
					if( columns[i] < 1 ) break;
					else if( columns[i] > anchor ) throw new IllegalStateException( "columns[i]{"+columns[i]+"} > anchor{"+anchor+"}" );
					else if( columns[i] == anchor ) continue;
					else if( columns[i] == (anchor-1) ) anchor = columns[i];
					else{
						hideColumnsImpl( anchor, lead );
						anchor = lead = columns[i];
					}
				}
				hideColumnsImpl( anchor, lead );
			}
		}catch( Exception exception ){
			System.err.println( "Warning: SloppyTableModel.hideColumns(int[]) caught " + exception );
			//exception.printStackTrace();
		}finally{
			fireTableStructureChanged();
		}
	}

	private int myLength;
	private int[] myMapping;
	private MapSearch.MapInfo myMapInfo;
	private ArrayList myData;
	private double myPrE;
	private List myVariables;

	public static final int INT_BREADTH_DEFAULT = 2;
	public static final int INT_OFFSET_DEFAULT = 0;

	private int myBreadth = INT_BREADTH_DEFAULT;
	private int myOffset = INT_OFFSET_DEFAULT;

	public double getPrE(){ return myPrE; }

	public int getBreadth() { return myBreadth; }
	public void setBreadth( int val )
	{
		synchronized( mySynch )
		{
		if( myBreadth != val ){
			if( val > (myLength-myOffset) ){
				if( myOffset > 0 ) --myOffset;
				else throw new IllegalArgumentException();
			}
			myBreadth = val;
			fireTableStructureChanged();
		}
		}
	}

	public int getOffset() { return myOffset; }
	public void setOffset( int val )
	{
		synchronized( mySynch )
		{
		if( myOffset != val ){
			if( val > (myLength-myBreadth) ) throw new IllegalArgumentException();
			myOffset = val;
			fireTableStructureChanged();
		}
		}
	}

	public int getNumResults(){
		return myLength;
	}

	public int jump( int index )
	{
		synchronized( mySynch )
		{
		//System.out.println( "SloppyTableModel.jump( "+index+" )" );
		int inverse = findInverse( index );
		//System.out.println( "\t inverse = " + inverse );

		if( inverse == (int)-1 ) return inverse;
		int jumpTo = (int)-1;
		if( inverse < (myBreadth-1) ) jumpTo = (int)0;
		else if( inverse > (myLength - myBreadth + 1) ) jumpTo = myLength - myBreadth;
		else jumpTo = inverse - ((int)(Math.floor( (double)myBreadth/(double)2 )));

		//System.out.println( "\t myBreadth = " + myBreadth );
		//System.out.println( "\t jumpTo = " + jumpTo );

		setOffset( jumpTo );
		return inverse;
		}
	}

	/** @since 092404 */
	public int findInverse( int index )
	{
		//System.out.println( "SloppyTableModel.findInverse( "+index+" )" );
		for( int i = Math.min(index, myLength-1); i>=0; i-- ){
			if( myMapping[i] == index ) return i;
			else if( myMapping[i] < index ) return (int)-1;
		}
		return (int)-1;
	}

	public void init()
	{
		fireTableDataChanged();
	}

	public boolean contains( Variable var )
	{
		return ( myVariables != null && myVariables.contains( var ) );
	}

	public String getColumnName( int column )
	{
		synchronized( mySynch )
		{
		if( column == 0 ) return "Variable";
		else if( column <= myBreadth ){
			return STR_HTML_HEADER_PRE + Integer.toString( getMappedSolutionNumberForColumn(column) ) + STR_HTML_HEADER_IN + Double.toString( getValueAtColumn(column).score );
		}
		else return "getColumnName() Error";
		}
	}

	/** @since 092404 */
	public int getMappedSolutionNumberForColumn( int column ){
		int index = (column-1)+myOffset;
		if( (0 <= index) && (index < myMapping.length) ) return myMapping[ index ] + 1;
		else return (int)-1;
	}

	public int getRowCount()
	{
		return myVariables.size();
	}

	public int getColumnCount()
	{
		synchronized( mySynch )
		{
		return myBreadth+1;
		}
	}

	public MapSearch.MapResult getValueAtColumn( int columnIndex ){
		synchronized( mySynch ){
		if( columnIndex <= myBreadth ) return (MapSearch.MapResult) myData.get( myMapping[(columnIndex-1)+myOffset] );
		else return null;
		}
	}

	public Object getValueAt( int rowIndex, int columnIndex )
	{
		synchronized( mySynch )
		{
		if( columnIndex == 0 ) return myVariables.get(rowIndex);
		else if( columnIndex <= myBreadth ){
			return getValueAtColumn(columnIndex).getConvertedInstatiation().get( myVariables.get(rowIndex) );
		}
		else return null;
		}
	}

	private Object mySynch = new Object();
}
