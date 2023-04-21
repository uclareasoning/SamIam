package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.tree.EvidenceTreeCellRenderer;

import edu.ucla.belief.*;
import edu.ucla.util.WeakLinkedList;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

/**
	@author Keith Cascio
	@since 031102
*/
public class ChooseInstancePanel2 extends JPanel implements ItemListener
{
	private NetworkInternalFrame hnInternalFrame;
	private ComputationCache myComputationCache;
	private WeakLinkedList listeners;
	private VariableComboBox varBox;
	private JComboBox valueBox;
	private FiniteVariable myCurrentVariable = null;
	private JLabel lblTitle = null;
	private static boolean useSecondInstance = true;
	private VariableInstance myUtilVariableInstance = new VariableInstance( null, null );

	/** @since 061902 */
	public void setEnabled( boolean enabled )
	{
		varBox.setEnabled( enabled );
		valueBox.setEnabled( enabled );
	}

	/** @since 052202 */
	public ChooseInstancePanel2( NetworkInternalFrame hnInternalFrame, String title, DisplayableFiniteVariable varDefault, Object instanceDefault )
	{
		this( title, hnInternalFrame );

		if( varDefault != null ) varBox.setSelectedItem( varDefault );
		if( instanceDefault == null )
		{
			if( valueBox.getItemCount() > 1 )
			{
				if( useSecondInstance = !useSecondInstance ) valueBox.setSelectedIndex( (int)1 );
				else valueBox.setSelectedIndex( (int)0 );
			}
		}
		else valueBox.setSelectedItem( instanceDefault );
	}

	public ChooseInstancePanel2( String title, NetworkInternalFrame hnInternalFrame )
	{
		this.hnInternalFrame = hnInternalFrame;
		this.myComputationCache = hnInternalFrame.getComputationCache();
		listeners = new WeakLinkedList();
		init( title );
	}

	/** @since 052202 */
	public void setTitle( String title )
	{
		lblTitle.setText( title );
	}

	/** @since 20020522 */
	public void setSelectedVariable( DisplayableFiniteVariable varDefault )
	{
		//System.out.println( "ChooseInstancePanel2.setSelectedVariable( "+varDefault+" ), current = " + varBox.getSelectedItem() );
		if( (varDefault == null) || (varBox.getSelectedItem() == varDefault) ) return;

		if( varDefault != null ) varBox.setSelectedItem( varDefault );
		if( (useSecondInstance = !useSecondInstance) && (valueBox.getItemCount() > (int)1) ) valueBox.setSelectedIndex( (int)1 );
		else if( valueBox.getItemCount() > (int)0 ) valueBox.setSelectedIndex( (int)0 );
	}

	/** @since 052202 */
	private void init( String title )
	{
		if( RENDERER == null )
		{
			RENDERER = new DefaultListCellRenderer();
			INT_BASE_CONDITIONAL_STRING_LENGTH =
				RENDERER.getListCellRendererComponent( JLIST_DUMMY,
						      STR_BASE_CONDITIONAL_STRING,
						      (int)0, false, false).getPreferredSize().width;
		}

		setLayout(new GridLayout(3, 0));

		lblTitle = new JLabel( title );
		lblTitle.setFont( lblTitle.getFont().deriveFont( Font.BOLD ) );
		add( lblTitle );

		varBox = hnInternalFrame.createVariableComboBox();
		varBox.addSelectedChangeListener(this);
		add(varBox);

		JPanel valuePanel = new JPanel();
		valuePanel.setLayout( new BoxLayout( valuePanel, BoxLayout.X_AXIS ) );
		add(valuePanel);

		JLabel lblEqualsSign = new JLabel(" = ");
		valuePanel.add( lblEqualsSign );
		//System.out.println( "Java lblEqualsSign width = " + lblEqualsSign.getPreferredSize().width );//debug

		valueBox = new JComboBox();
		//valueBox.setFont( VariableComboBox.FONT_DEFAULT );
		valueBox.setRenderer( new InstanceRenderer() );
		revalidateValueBox();
		valueBox.addItemListener(this);
		valuePanel.add(valueBox);

		//recalculateWidth();
	}

	public void addItemListener(ItemListener listener) {
		listeners.add(listener);
	}

	public void removeItemListener(ItemListener listener) {
		listeners.remove(listener);
	}

	public DisplayableFiniteVariable getVariable() {
		return (DisplayableFiniteVariable) varBox.getSelectedItem();
	}

	public Object getInstance() {
		return valueBox.getSelectedItem();
	}

	/** @since 021405 Valentine's Day! */
	public void setInstance( Object instance ) {
		if( !getVariable().contains( instance ) ) throw new IllegalArgumentException( getVariable().getID() + " contains no state \"" + instance + "\"" );
		valueBox.setSelectedItem( instance );
	}

	private void revalidateValueBox()
	{
		valueBox.removeAllItems();
		DisplayableFiniteVariable dvar = (DisplayableFiniteVariable)varBox.getSelectedItem();
		if (dvar == null) return;

		Object instance = null;
		myCurrentVariable = dvar;//dvar.getFiniteVariable();

		for (int i = 0; i < myCurrentVariable.size(); i++)
		{
			instance = myCurrentVariable.instance(i);
			valueBox.addItem(instance);
		}
	}

	public static boolean FLAG_DEBUG_WIDTH = false;
	public static int INT_MINIMUM_WIDTH = (int)64;
	protected static JList JLIST_DUMMY = new JList();
	protected static ListCellRenderer RENDERER = null;
	protected static String STR_BASE_CONDITIONAL_STRING = "Pr(  )=" + ComputationCache.STR_CONDITIONAL_FORMAT;
	protected static int INT_BASE_CONDITIONAL_STRING_LENGTH = (int)-1;

	/** @since 050702 */
	public void recalculateWidth()
	{
		if( FLAG_DEBUG_WIDTH ) Util.STREAM_VERBOSE.println( "\n\nChooseInstancePanel2.recalculateWidth()" );

		Component tempComp = null;
		int tempWidth = (int)0;//INT_MINIMUM_WIDTH;
		int maxw = (int)0;//INT_MINIMUM_WIDTH;
		Object instance = null;
		ComboBoxModel myModel = varBox.getModel();
		DisplayableFiniteVariable dvar = null;

		for( int i=0; i<myModel.getSize(); i++ )
		{
			dvar = (DisplayableFiniteVariable)myModel.getElementAt( i );
			for( int j=0; j < dvar.size(); j++ )
			{
				instance = dvar.instance( j );
				tempComp = RENDERER.getListCellRendererComponent( JLIST_DUMMY, instance, j, false, false );
				tempWidth = tempComp.getPreferredSize().width;
				if( tempWidth > maxw )
				{
					maxw = tempWidth;
					if( FLAG_DEBUG_WIDTH ) Util.STREAM_VERBOSE.println( dvar + ", " + instance + ": " + tempWidth );
				}
			}
		}

		Dimension newDim = valueBox.getPreferredSize();
		//newDim.width = maxw + INT_WIDTH_PADDING;
		newDim.width = Math.max( VariableComboBox.adjustWidth( maxw+INT_BASE_CONDITIONAL_STRING_LENGTH ), INT_MINIMUM_WIDTH );

		if( FLAG_DEBUG_WIDTH ) Util.STREAM_VERBOSE.println( "valueBox size ->    " + newDim );
		valueBox.setPreferredSize( newDim );
		//valueBox.setSize( newDim );
		valueBox.setMinimumSize( newDim );
		if( FLAG_DEBUG_WIDTH ) Util.STREAM_VERBOSE.println( "valueBox.getSize(): " + valueBox.getSize() );

		int maxwidth = Math.max( varBox.getPreferredSize().width, valueBox.getPreferredSize().width + INT_EQUALS_SIGN_WIDTH );

		Dimension newSize = getPreferredSize();
		newSize.width = maxwidth;
		if( FLAG_DEBUG_WIDTH ) Util.STREAM_VERBOSE.println( "panel size ->       " + newSize );
		setPreferredSize( newSize );
		setMinimumSize( newSize );
		if( FLAG_DEBUG_WIDTH ) Util.STREAM_VERBOSE.println( "panel.getSize():    " + getSize() );
		if( FLAG_DEBUG_WIDTH ) Util.STREAM_VERBOSE.println( "valueBox.getSize(): " + valueBox.getSize() );
	}

	/** @since 081402 */
	public void assumeSize( ChooseInstancePanel2 panel )
	{
		Dimension newDim = panel.valueBox.getPreferredSize();
		valueBox.setPreferredSize( newDim );
		valueBox.setMinimumSize( newDim );

		Dimension newSize = panel.getPreferredSize();
		setPreferredSize( newSize );
		setMinimumSize( newSize );
	}

	/** @since 081402 */
	public void debugValueBoxSize()
	{
		Util.STREAM_DEBUG.println( "valueBox:" + valueBox.getSize() + ", panel:" + getSize() );
	}

	private static final int INT_WIDTH_PADDING = (int)32;
	private static final int INT_EQUALS_SIGN_WIDTH = 24;
	public static final String STR_MESSAGE_STATUS = " sensitivity calculating conditional for ";

	/**
		@author Keith Cascio
		@since 031302
	*/
	private class InstanceRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(
		         JList list,
		         Object value,
		         int index,
		         boolean isSelected,
		         boolean cellHasFocus)
		{
			JLabel ret = (JLabel)(super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus));
			//ret.setText( hnInternalFrame.getConditionalString( myCurrentVariable, value, STR_MESSAGE_STATUS + myCurrentVariable.toString() + "..." ) );
			myUtilVariableInstance.setData( myCurrentVariable, value );
			ret.setText( ChooseInstancePanel2.this.myComputationCache.getConditionalString( myUtilVariableInstance, STR_MESSAGE_STATUS ) );
			return ret;
		}
	}

	public void itemStateChanged(ItemEvent event) {
		ItemSelectable item = event.getItemSelectable();
		if (item == varBox)
			revalidateValueBox();

		ItemListener next;
		for( ListIterator it = listeners.listIterator(); it.hasNext(); ){
			next = (ItemListener)it.next();
			if( next == null ) it.remove();
			else next.itemStateChanged(event);
		}
	}

	/**
		Test/debug
	*/
	public static void main( String[] args )
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}

		final String postfix = "a";
		final StringBuffer string = new StringBuffer( postfix );
		final DefaultComboBoxModel model = new DefaultComboBoxModel();
		model.addElement( postfix );
		model.setSelectedItem( postfix );
		final DefaultListCellRenderer renderer = new DefaultListCellRenderer();
		final JComboBox box = new JComboBox( model );
		box.setFont( new Font( "Monospaced", Font.PLAIN, (int)12 ) );
		box.setRenderer( renderer );
		final JList dummy = new JList();
		final Random rand = new Random();

		final int numIterations = (int)150;
		final int[] arrayPredicted = new int[ numIterations ];
		final int[] arrayActual = new int[ numIterations ];
		final char[] mapASCII = new char[] {32,32,32,32,32,32,32,32,32,32,32,32,32,35,36,37,38,42,43,44,45,48,49,50,51,52,53,54,55,56,57,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,94,95,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,126};
		final int sizeMapASCII = mapASCII.length;

		PrintWriter writ = null;
		try{
			writ = new PrintWriter( new FileWriter( "jcombobox.dat" ) );
		}catch( Exception e ){
			e.printStackTrace();
		}
		final PrintWriter writer = writ;

		JPanel pnlBox = new JPanel();
		pnlBox.add( box );

		JPanel pnlButtons = new JPanel();
		JButton button1 = new JButton( "Append" );
		button1.addActionListener( new ActionListener()
		{
			public void actionPerformed( ActionEvent e )
			{
				String toAdd = string.append( mapASCII[ rand.nextInt(sizeMapASCII) ] ).toString();
				model.addElement( toAdd );
				model.setSelectedItem( toAdd );
				int predicted = renderer.getListCellRendererComponent(dummy,toAdd,0,false,false).getPreferredSize().width;
				int actual = box.getSize().width;
				Util.STREAM_TEST.println( predicted + " " + actual );
				arrayPredicted[indexCurrent] = predicted;
				arrayActual[indexCurrent] = actual;
				++indexCurrent;
				if( indexCurrent >= limit )
				{
					Util.STREAM_TEST.println( toAdd );
					indexCurrent = (int)0;
				}
			}

			StringBuffer string = new StringBuffer( postfix );
			int indexCurrent = (int)0;
			int limit = numIterations;
		} );
		pnlButtons.add( button1 );

		JFrame frame = new JFrame( "DEBUG ChooseInstancePanel2" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		Container contentPain = frame.getContentPane();
		contentPain.setLayout( new BorderLayout() );
		contentPain.add( pnlBox, BorderLayout.CENTER );
		contentPain.add( pnlButtons, BorderLayout.EAST );

		frame.pack();
		Dimension dim = frame.getSize();
		dim.width = 1200;
		dim.height = 200;
		frame.setSize( dim );

		Util.centerWindow( frame );
		frame.setVisible( true );

		for( int i=0; i<numIterations; i++ ) button1.doClick();

		writer.print( "maple\n\nwith(stats);\n\ntValues:=["+ arrayPredicted[0] +".000001" );
		for( int i=1; i<numIterations; i++ ) writer.print( "," + arrayPredicted[i] );
		writer.print( "];\n\nCValues:=["+ arrayActual[0] +".000001" );
		for( int i=1; i<numIterations; i++ ) writer.print( "," + arrayActual[i] );
		writer.println( "];\n\nC:= rhs(fit[leastsquare[[t,C],C=A*t^2+B*t+D]]([tValues, CValues]));" );
		writer.close();
	}
}
