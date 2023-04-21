package edu.ucla.belief.ui.util;

import edu.ucla.belief.VariableComparator;

import java.awt.*;
import javax.swing.*;
import java.util.*;

/**
	@author Keith Cascio
	@since 070103
*/
public class SortedListModel extends AbstractListModel implements ComboBoxModel
{
	public SortedListModel( Collection data, Comparator comp, int arraySize )
	{
		int dataSize = Math.max( data.size(), arraySize );
		myData = new ArrayList( dataSize );
		myData.addAll( data );
		myComparator = comp;
		if( myComparator == null ) myComparator = VariableComparator.getInstance();
		Collections.sort( myData, myComparator );
		if( myData.size() > 0 ) mySelectedItem = myData.get( 0 );
	}

	/** @since 20070310 */
	public void clear(){
		try{
			if( !myData.isEmpty() ){
				myData.clear();
				fireContentsChanged();
			}
		}catch( Exception exception ){
			System.err.println( "warning: SortedListModel.clear() caught " + exception );
		}
	}

	/** @since 20070310 */
	protected void fireContentsChanged(){
		this.fireContentsChanged( this, 0, getSize() - 1 );
	}

	public int getSize()
	{
		return myData.size();
	}

	public Object getElementAt(int index)
	{
		if( index >= (int)0 && index < myData.size() ) return myData.get( index );
		else
		{
			System.err.println( "Warning: SortedListModel.getElementAt( invalid index )" );
			return null;
		}
	}

	public void addElement( Object obj )
	{
		if( !contains( obj ) )
		{
			myData.add( obj );
			Collections.sort( myData, myComparator );
			fireContentsChanged();
		}
	}

	public boolean removeElement( Object obj )
	{
		boolean ret = myData.remove( obj );
		if( ret )
		{
			Collections.sort( myData, myComparator );
			fireContentsChanged();
		}
		return ret;
	}

	public boolean contains( Object elem )
	{
		return myData.contains( elem );
	}

	/**
		interface ComboBoxModel
		@author Keith Cascio
		@since 102203
	*/
	public void setSelectedItem(Object anItem)
	{
		mySelectedItem = anItem;
	}
	public Object getSelectedItem()
	{
		return mySelectedItem;
	}

	private Object mySelectedItem;
	protected ArrayList myData;
	protected Comparator myComparator;
}
