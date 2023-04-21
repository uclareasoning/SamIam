package edu.ucla.belief.ui.util;

import edu.ucla.util.WeakLinkedList;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/** A JComboBox that contains an array of variables. */
public class VariableComboBox extends JComboBox
{
	private WeakLinkedList listeners;
	private ListCellRenderer myOriginalRenderer;

	public VariableComboBox( Collection variables )
	{
		super( new SortedListModel( variables, null, variables.size() ) );
		init( variables );
	}

	/** @since 061304 */
	public void setVariables( Collection variables )
	{
		if( variables == null || variables.isEmpty() ){
			super.setModel( EmptyModel.getInstance() );
			if( myOriginalRenderer != null ) setRenderer( myOriginalRenderer );
			setEnabled( false );
		}
		else{
			super.setModel( new SortedListModel( variables, null, variables.size() ) );
			configRenderer( variables );
			setEnabled( true );
		}
	}

	/** @since 061304 */
	private void configRenderer( Collection variables ){
		if( myOriginalRenderer == null ) myOriginalRenderer = getRenderer();
		setRenderer( new VariableLabelRenderer( myOriginalRenderer, variables ) );
	}

	/** @since 042103 */
	protected void init( Collection variables )
	{
		//setFont( FONT_DEFAULT );
		configRenderer( variables );
		listeners = new WeakLinkedList();
		recalculateWidth();
	}

	/** @since 061903 */
	public void recalculateDisplayValues( Collection variables )
	{
		((VariableLabelRenderer)getRenderer()).recalculateDisplayValues( variables );
	}

	private void recalculateWidth()
	{
		//System.out.println( "VariableComboBox.recalculateWidth()" );
		ListCellRenderer renderer = getRenderer();
		Component tempComp = null;
		int tempWidth = 0;
		int maxw = 0;
		Object instance = null;
		ComboBoxModel myModel = getModel();
		JList dummy = new JList();
		for( int i=0; i<myModel.getSize(); i++ )
		{
			instance = myModel.getElementAt( i );
			tempComp = renderer.getListCellRendererComponent( dummy, instance, i, false, false );
			tempWidth = tempComp.getPreferredSize().width;
			//System.out.println( "\t width( \"" +instance+ "\" ) == " + tempWidth );
			if( tempWidth > maxw ) maxw = tempWidth;
		}

		Dimension newDim = getPreferredSize();
		//newDim.width = maxw + INT_WIDTH_PADDING;
		newDim.width = adjustWidth( maxw );

		setPreferredSize( newDim );
		setSize( newDim );
	}

	/** @since 102203 */
	public static final int adjustWidth( int width )
	{
		return INT_WIDTH_CONST_FACTOR + (int)(((double)width) * DOUBLE_WIDTH_LINEAR_FACTOR ) + (int)( ((double)(width*width)) * DOUBLE_WIDTH_SQUARE_FACTOR );
	}

	//public static final Font FONT_DEFAULT = new Font( "Monospaced", Font.PLAIN, (int)12 );
	public static final int INT_WIDTH_CONST_FACTOR = (int)10;
	public static final double DOUBLE_WIDTH_LINEAR_FACTOR = (double)1.141157942;
	public static final double DOUBLE_WIDTH_SQUARE_FACTOR = (double)0.0003;
	//numbers work for cancer/Tcc4d: linear: 0.66, square: 0.0022

	private static final double DOUBLE_WIDTH_MULTIPLIER = (double)1.253;
	private static final int INT_WIDTH_PADDING = 32;

	public void addSelectedChangeListener(ItemListener listener) {
		addItemListener(listener);
		listeners.add(listener);
	}

	public void removeSelectedChangeListener(ItemListener listener) {
		super.removeItemListener(listener);
		listeners.remove(listener);
	}

	public void setSelectedIndex(int index) {
		super.setSelectedIndex(index);
		ItemEvent itemEvent = new ItemEvent(this, 0, this,ItemEvent.SELECTED);
		notify( itemEvent );
	}

	public void setSelectedItem(Object object) {
		super.setSelectedItem(object);
		ItemEvent itemEvent = new ItemEvent(this, 0, this,ItemEvent.SELECTED);
		notify( itemEvent );
	}

	protected void notify( ItemEvent itemEvent )
	{
		ItemListener next;
		for( ListIterator it = listeners.listIterator(); it.hasNext(); ){
			next = (ItemListener) it.next();
			if( next == null ) it.remove();
			else next.itemStateChanged(itemEvent);
		}
	}
}
