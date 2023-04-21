package edu.ucla.belief.ui.internalframes;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.clipboard.InstantiationClipBoard;
//import edu.ucla.belief.ui.*;
//import edu.ucla.belief.ui.event.*;

import edu.ucla.belief.*;
import edu.ucla.util.WeakLinkedList;
import edu.ucla.belief.inference.map.ExactMap;
import il2.inf.map.MapSearch;

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;
//import java.text.*;

/**
	@author Keith Cascio
	@since 092104
*/
public class SloppyPanel extends JPanel implements ActionListener, ChangeListener, ListSelectionListener, ItemListener
{
	public SloppyPanel( MapSearch.MapInfo results, double PrE, List variables )
	{
		super( new GridBagLayout() );
		myTableModel = new SloppyTableModel( results, PrE, variables );
		myPrEInverse = ((double)1)/PrE;
		init();
	}

	/** interface ItemListener */
	public void itemStateChanged(ItemEvent e){
		Object src = e.getSource();
		if( src == myComboDiff ){
			myDiffDecorator = (DiffDecorator) myComboDiff.getSelectedItem();
			myJTable.repaint();
		}
		else if( src == myComboSelection ){
			ListSelectionModel model = myJTable.getColumnModel().getSelectionModel();
			Object item = myComboSelection.getSelectedItem();
			int setting = (int)-1;
			if( item == OBJ_SINGLE_SELECTION ) setting = ListSelectionModel.SINGLE_SELECTION;
			else if( item == OBJ_SINGLE_INTERVAL_SELECTION ) setting = ListSelectionModel.SINGLE_INTERVAL_SELECTION;
			model.setSelectionMode( setting );
		}
	}

	/** interface ActionListener */
	public void actionPerformed( ActionEvent e ){
		Object src = e.getSource();
		if( src == myButtonScrollLeft ) doScrollLeft();
		else if( src == myButtonScrollRight ) doScrollRight();
		else if( src == myButtonJump ) doJump();
		else if( src == myButtonCopy ) doCopy( false );
		else if( src == myButtonCopyPlusEvisence ) doCopy( true );
		else if( src == myButtonHide ) doHide();
		else if( src == myButtonRefresh ) doRefresh();
	}

	/** @since 092404 */
	public void doHide()
	{
		//ListSelectionModel model = myJTable.getColumnModel().getSelectionModel();
		//int anchor = model.getMinSelectionIndex();
		//if( anchor < 0 ) return;
		//int lead = model.getMaxSelectionIndex();
		//myTableModel.hideColumns( myJTable.convertColumnIndexToModel(anchor), myJTable.convertColumnIndexToModel(lead) );

		if( myJTable.getSelectedColumnCount() < 1 ) return;

		int[] selectedColumns = myJTable.getSelectedColumns();
		for( int i=0; i<selectedColumns.length; i++ ){
			selectedColumns[i] = myJTable.convertColumnIndexToModel(selectedColumns[i]);
		}
		myTableModel.hideColumns( selectedColumns );

		myButtonRefresh.setEnabled( true );
		updateControls();
	}

	/** @since 092404 */
	public void doRefresh()
	{
		myTableModel.refreshMapping();
		myButtonRefresh.setEnabled( false );
		updateControls();
	}

	public void addJumpy( Jumpy jumpy ){
		if( myJumpys == null ) myJumpys = new WeakLinkedList();
		if( !myJumpys.contains( jumpy ) ) myJumpys.addFirst( jumpy );
	}
	public boolean removeJumpy( Jumpy jumpy ){
		return (myJumpys != null) && myJumpys.remove( jumpy );
	}
	private WeakLinkedList myJumpys;

	public void doJump()
	{
		int value = myTfJump.getValue();
		int correctedValue = value - 1;
		int inverse = myTableModel.jump( correctedValue );
		if( inverse == (int)-1 ) return;

		int indexColumn = (inverse+1) - myTableModel.getOffset();
		myJTable.getColumnModel().getSelectionModel().setSelectionInterval( indexColumn, indexColumn );
		updateControls();

		if( myJumpys == null ) return;
		Jumpy next;
		for( ListIterator it = myJumpys.listIterator(); it.hasNext(); ){
			next = (Jumpy)it.next();
			if( next == null ) it.remove();
			else next.jump( correctedValue );
		}
	}

	public void doScrollLeft(){
		myTableModel.setOffset( myTableModel.getOffset()-1 );
		updateControls();
	}

	public void doScrollRight(){
		myTableModel.setOffset( myTableModel.getOffset()+1 );
		updateControls();
	}

	/** interface ChangeListener */
	public void stateChanged( ChangeEvent e ){
		Object src = e.getSource();
		if( (src == mySliderBreadth) && myFlagListenSlider ) doSlideBreadth();
	}

	/** interface ListSelectionListener */
	public void valueChanged( ListSelectionEvent e )
	{
		int index = myJTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
		double score = (double)-1;
		boolean flagSolutionSelected = false;
		int indexModel = -1;
		if( (index > 0) && ((indexModel = myJTable.convertColumnIndexToModel(index)) > 0) )
		{
			try{
				score = myTableModel.getValueAtColumn( indexModel ).score;
				myLabelSolutionNumber.setText( Integer.toString( myTableModel.getMappedSolutionNumberForColumn( index ) ) );
				flagSolutionSelected = true;
			}catch( Exception exception ){
				System.err.println( "Warning: SloppyPanel.valueChanged() caught " + exception );
				//exception.printStackTrace();
				score = (double)-1;
				flagSolutionSelected = false;
			}
		}

		String strScore, strScoreCond;
		if( flagSolutionSelected ){
			double scorecond = score*myPrEInverse;
			strScore = Double.toString( score );
			strScoreCond = Double.toString( scorecond );
		}
		else{
			strScore = STR_NA;
			strScoreCond = STR_NA;
			myLabelSolutionNumber.setText( STR_NA );
		}

		myLabelScoreJoint.setText( strScore );
		myLabelScoreJoint.setToolTipText( strScore );
		myLabelScoreConditioned.setText( strScoreCond );
		myLabelScoreConditioned.setToolTipText( strScoreCond );
		setCopyButtonsEnabled( flagSolutionSelected );

		myJTable.repaint();
	}

	public void setCopyButtonsEnabled( boolean flag ){
		myButtonHide.setEnabled( flag && (myTableModel.getNumResults() > 1) );
		myButtonCopy.setEnabled( flag && (myClipBoard != null) );
		myButtonCopyPlusEvisence.setEnabled( flag && (myClipBoard != null) && (myEvidenceController != null) );
	}

	public void doSlideBreadth()
	{
		//TableColumn col = myJTable.getColumnModel().getColumn( 0 );
		//int oldwidth = col.getWidth();

		myTableModel.setBreadth( mySliderBreadth.getValue() );
		updateControls();

		//if( col.getWidth() <= oldwidth ) col.setMinWidth( oldwidth );
		//else col.setMaxWidth( oldwidth );
		////col.setPreferredWidth( oldwidth );
		//myJTable.revalidate();
		//myJTable.repaint();
		////col.setMinWidth(1);
		////col.setMaxWidth(10000);
	}

	public void updateControls(){
		int offset = myTableModel.getOffset();
		int breadth = myTableModel.getBreadth();
		int numresults = myTableModel.getNumResults();

		myButtonScrollLeft.setEnabled( offset > 0 );
		myButtonScrollRight.setEnabled( offset < (numresults - breadth) );

		myFlagListenSlider = false;
		mySliderBreadth.setMaximum( Math.min( myTableModel.getNumResults(), INT_SLIDER_UPPER_BOUND ) );
		mySliderBreadth.setValue( breadth );
		myFlagListenSlider = true;

		//myTfJump.setMaxValue( numresults );
		//myTfJump.setValue( Math.min( myTfJump.getValue(), numresults ) );
	}

	private void init()
	{
		myJTable = new JTable( myTableModel );
		//TableCellRenderer renderer = new SloppyCellRenderer( myJTable.getDefaultRenderer( Object.class ), myJTable.getTableHeader().getDefaultRenderer() );
		TableCellRenderer renderer = new SloppyCellRenderer( myJTable.getDefaultRenderer( Object.class ) );
		myJTable.setDefaultRenderer( Object.class, renderer );
		myJTable.setColumnSelectionAllowed( true );
		myJTable.setRowSelectionAllowed( false );
		myJTable.setSelectionMode( ListSelectionModel.SINGLE_INTERVAL_SELECTION );
		myJTable.getColumnModel().getSelectionModel().addListSelectionListener( (ListSelectionListener)this );

		myPain = new JScrollPane( myJTable );
		myPain.setPreferredSize( new Dimension( 500,200 ) );

		GridBagConstraints c = new GridBagConstraints();
		JLabel label;

		c.fill = GridBagConstraints.NONE;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		this.add( Box.createHorizontalStrut( 16 ), c );

		this.add( new JLabel( "Solutions shown: " ), c );

		c.anchor = GridBagConstraints.EAST;
		this.add( mySliderBreadth = new JSlider( (int)1, Math.min( myTableModel.getNumResults(), INT_SLIDER_UPPER_BOUND ), myTableModel.getBreadth() ), c );//(int min, int max, int value)
		mySliderBreadth.addChangeListener( (ChangeListener)this );
		//mySliderBreadth.setMinorTickSpacing( (int)1 );
		mySliderBreadth.setMajorTickSpacing( (int)1 );
		mySliderBreadth.setSnapToTicks( true );
		mySliderBreadth.setPaintTicks( true );
		Dimension dimSlider = mySliderBreadth.getPreferredSize();
		dimSlider.width = 256;
		mySliderBreadth.setPreferredSize( dimSlider );
		mySliderBreadth.setMinimumSize( dimSlider );
		mySliderBreadth.setToolTipText( "Adjust number of solutions shown" );

		this.add( Box.createHorizontalStrut( 4 ), c );
		this.add( new JLabel( "Show differences: " ), c );
		this.add( myComboDiff = new JComboBox( ARRAY_DIFF_OPTIONS ), c );
		myComboDiff.setSelectedItem( myDiffDecorator = DIFF_DEFAULT );
		myComboDiff.addItemListener( (ItemListener)this );

		if( FLAG_DISPLAY_SELECTION_COMBO ){
			this.add( Box.createHorizontalStrut( 4 ), c );
			this.add( new JLabel( "Selection mode: " ), c );
			this.add( myComboSelection = new JComboBox( ARRAY_SELECTION_OPTIONS ), c );
			myComboSelection.setSelectedItem( OBJ_SELECTION_DEFAULT );
			myComboSelection.addItemListener( (ItemListener)this );
		}

		c.weightx = 1;
		this.add( Box.createHorizontalStrut( 32 ), c );

		c.weightx = 0;
		this.add( myButtonJump = new JButton( "Jump to:" ), c );
		myButtonJump.setToolTipText( "Jump to solution" );
		myButtonJump.addActionListener( (ActionListener)this );
		this.add( myTfJump = new WholeNumberField( myTableModel.getNumResults(), 5, 1, myTableModel.getNumResults() ), c );
		Dimension dimTf = myTfJump.getPreferredSize();
		dimTf.width = 80;
		myTfJump.setPreferredSize( dimTf );
		myTfJump.setMinimumSize( dimTf );

		c.weightx = 1;
		this.add( Box.createHorizontalStrut( 32 ), c );

		c.weightx = 0;
		this.add( myButtonScrollLeft = new JButton( "<-" ), c );
		myButtonScrollLeft.addActionListener( (ActionListener)this );
		myButtonScrollLeft.setToolTipText( "Scroll left" );

		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myButtonScrollRight = new JButton( "->" ), c );
		myButtonScrollRight.addActionListener( (ActionListener)this );
		myButtonScrollRight.setToolTipText( "Scroll right" );

		/*
		c.gridwidth = 1;
		this.add( myIPBreadth = new IncrementPanel( "breadth: " ){
			public void setValue( int val ) { SloppyPanel.this.myTableModel.setBreadth( val ); }
			public int getValue(){ return SloppyPanel.this.myTableModel.getBreadth(); }
			public int getCeiling() { return SloppyPanel.this.myTableModel.getNumResults(); }
			public int getFloor() { return 1; }
			public void update() { super.update(); SloppyPanel.this.myIPOffset.refresh(); }
		} , c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myIPOffset = new IncrementPanel( "offset: " ){
			public void setValue( int val ) { SloppyPanel.this.myTableModel.setOffset( val ); }
			public int getValue(){ return SloppyPanel.this.myTableModel.getOffset(); }
			public int getFloor() { return 0; }
			public int getCeiling() { return SloppyPanel.this.myTableModel.getNumResults() - SloppyPanel.this.myTableModel.getBreadth(); }
			public void update() { super.update(); SloppyPanel.this.myIPBreadth.refresh(); }
		} , c );*/

		this.add( Box.createVerticalStrut(2), c );

		c.weightx = c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myPain, c );

		c.weightx = c.weighty = 0;
		c.fill = GridBagConstraints.NONE;
		this.add( Box.createVerticalStrut(8), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		this.add( createReportComponent(), c );

		//updateIncrementPanels();
		updateControls();
		valueChanged( (ListSelectionEvent)null );
	}

	public JComponent createReportComponent()
	{
		JPanel ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		JLabel label;

		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHWEST;
		ret.add( Box.createHorizontalStrut(16), c );
		ret.add( label = new JLabel( "Solution " ), c );
		Font newFont = label.getFont().deriveFont( Font.BOLD );
		label.setFont( newFont );
		ret.add( myLabelSolutionNumber = new JLabel(), c );
		myLabelSolutionNumber.setFont( newFont );
		myLabelSolutionNumber.setForeground( SloppyTableModel.COLOR_SOLUTION_NUMBERS );
		label.setFont( label.getFont().deriveFont( Font.BOLD ) );
		ret.add( label = new JLabel( " of " + Integer.toString( myTableModel.getNumResults() ) + ": " ), c );
		label.setFont( newFont );

		ret.add( Box.createHorizontalStrut(8), c );

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		ret.add( createScoreComponent(), c );

		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		ret.add( Box.createHorizontalStrut(8), c );

		c.anchor = GridBagConstraints.NORTHEAST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( createButtonComponent(), c );

		ret.add( Box.createVerticalStrut(8), c );

		return ret;
	}

	public JComponent createScoreComponent()
	{
		JPanel inner = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		JLabel label;

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		inner.add( label = new JLabel( "P(MAP,e)" ) );
		Font newFont = label.getFont().deriveFont( (float)20 );
		label.setFont( newFont );
		inner.add( label = new JLabel( " = " ) );
		label.setFont( newFont );
		c.gridwidth = GridBagConstraints.REMAINDER;
		inner.add( myLabelScoreJoint = new JLabel( "", JLabel.LEFT ), c );
		myLabelScoreJoint.setFont( newFont );

		c.gridwidth = 1;
		inner.add( label = new JLabel( "P(MAP|e)" ) );
		label.setFont( newFont );
		inner.add( label = new JLabel( " = " ) );
		label.setFont( newFont );
		c.gridwidth = GridBagConstraints.REMAINDER;
		inner.add( myLabelScoreConditioned = new JLabel( "", JLabel.LEFT ), c );
		myLabelScoreConditioned.setFont( newFont );

		//Border border = BorderFactory.createLineBorder( Color.black, 1 );
		Border border = BorderFactory.createEtchedBorder();
		Border buffer = BorderFactory.createEmptyBorder( 2, 4, 2, 8 );//int top, int left, int bottom, int right)
		inner.setBorder( BorderFactory.createCompoundBorder( border, buffer ) );//Border outsideBorder, Border insideBorder)

		JPanel outer = new JPanel( new GridBagLayout() );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		outer.add( inner, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		outer.add( Box.createHorizontalStrut(8), c );

		//outer.setBorder( BorderFactory.createLineBorder( Color.green, 1 ) );

		return outer;
	}

	public JComponent createButtonComponent()
	{
		JPanel ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;

		c.gridwidth = 1;
		ret.add( myButtonHide = new JButton( "Hide selected" ), c );
		myButtonHide.addActionListener( (ActionListener)this );

		ret.add( Box.createHorizontalStrut(8), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		//if( myActionCopy != null ) ret.add( myButtonCopy = new JButton( myActionCopy ), c );
		ret.add( myButtonCopy = new JButton( "Copy" ), c );

		c.gridwidth = 1;
		ret.add( myButtonRefresh = new JButton( "Unhide all" ), c );
		myButtonRefresh.addActionListener( (ActionListener)this );
		myButtonRefresh.setEnabled( false );

		ret.add( Box.createHorizontalStrut(8), c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		ret.add( myButtonCopyPlusEvisence = new JButton( "Copy (+evidence)" ), c );
		//if( myActionCopyPlus != null ) ret.add( myButtonCopyPlusEvisence = new JButton( myActionCopyPlus ), c );
		setCopyButtonsEnabled( false );

		return ret;
	}

	public void setClipBoard( InstantiationClipBoard clipboard, EvidenceController ec )
	{
		if( clipboard == null ) return;

		myClipBoard = clipboard;
		myEvidenceController = ec;

		if( myButtonCopy != null ) enable( myButtonCopy );
		if( myButtonCopyPlusEvisence != null && (myEvidenceController != null) ) enable( myButtonCopyPlusEvisence );
	}

	public void enable( JButton button ){
		button.setEnabled( true );
		button.addActionListener( this );
	}

	public void doCopy( boolean plusEvidence )
	{
		if( myClipBoard == null ) return;
		if( plusEvidence && (myEvidenceController == null) ) return;

		Map toCopy = null;
		int indexColumnSelected = myJTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
		if( indexColumnSelected < 0 ) return;

		MapSearch.MapResult result = myTableModel.getValueAtColumn( myJTable.convertColumnIndexToModel( indexColumnSelected ) );

		if( plusEvidence ) toCopy = new HashMap( result.getConvertedInstatiation() );
		else toCopy = result.getConvertedInstatiation();

		if( plusEvidence ) toCopy.putAll( myEvidenceController.evidence() );

		myClipBoard.copy( toCopy );
	}

	public class SloppyCellRenderer implements TableCellRenderer
	{
		private TableCellRenderer myDefaultRenderer;
		private TableCellRenderer myHeaderRenderer;

		public SloppyCellRenderer( TableCellRenderer defaultRenderer, TableCellRenderer headerRenderer )
		{
			myDefaultRenderer = defaultRenderer;
			myHeaderRenderer = headerRenderer;
		}

		public SloppyCellRenderer( TableCellRenderer defaultRenderer )
		{
			myDefaultRenderer = defaultRenderer;
			myHeaderRenderer = new StealTableHeader().createDefaultRenderer();
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if( table.convertColumnIndexToModel(column) == 0 ){
				JLabel ret = (JLabel) myHeaderRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
				ret.setHorizontalTextPosition( JLabel.LEFT );
				ret.setHorizontalAlignment( JLabel.LEFT );
				return ret;
			}
			else{
				JLabel ret = (JLabel) myDefaultRenderer.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
				myDiffDecorator.decorate( ret, table, isSelected, row, column );
				return ret;
			}
		}
	}

	/** @since 092404 */
	public abstract class DiffDecorator{
		public DiffDecorator( String name ){
			this.myName = name;
		}
		public String toString() { return myName; }
		abstract public JLabel decorate( JLabel label, JTable table, boolean isSelected, int row, int column );
		private String myName;
	}

	/** @since 092404 */
	public class DiffLocal extends DiffDecorator
	{
		public DiffLocal( String name ){
			super( name );
		}
		public JLabel decorate( JLabel label, JTable table, boolean isSelected, int row, int column )
		{
			if( isSelected ) return label;
			else label.setBackground( COLOR_DEFAULT );

			int indexColumnSelected = table.getColumnModel().getSelectionModel().getMinSelectionIndex();
			if( (indexColumnSelected > 0) && decideDiff( indexColumnSelected, column ) ){
				column = table.convertColumnIndexToModel( column );
				indexColumnSelected = table.convertColumnIndexToModel( indexColumnSelected );
				if( indexColumnSelected < 1 ) return label;
				MapSearch.MapResult selected = SloppyPanel.this.myTableModel.getValueAtColumn( indexColumnSelected );
				MapSearch.MapResult current = SloppyPanel.this.myTableModel.getValueAtColumn( column );
				FiniteVariable var = (FiniteVariable) SloppyPanel.this.myTableModel.getValueAt( row, 0 );
				Color background = COLOR_DIFF_EQUALS;
				if( selected.getConvertedInstatiation().get( var ) != current.getConvertedInstatiation().get( var ) ) background = COLOR_DIFF_NOTEQUALS;
				label.setBackground( background );
			}
			return label;
		}

		public boolean decideDiff( int indexColumnSelected, int column ){
			return Math.abs( column - indexColumnSelected ) == (int)1;
		}
	}

	public static final Color COLOR_DEFAULT = Color.white;
	public static final Color COLOR_DIFF_EQUALS = Color.white;
	public static final Color COLOR_DIFF_NOTEQUALS = Color.pink;

	public static class StealTableHeader extends JTableHeader
	{
		public TableCellRenderer createDefaultRenderer(){
			return super.createDefaultRenderer();
		}
	}

	private void updateIncrementPanels(){
		myIPBreadth.update();
		myIPOffset.update();
	}

	public JTable getJTable(){
		return myJTable;
	}

	public static interface Jumpy{
		public void jump( int value );
	}

	public static final String STR_NA = "--";
	public static final int INT_SLIDER_UPPER_BOUND = (int)16;
	public static final boolean FLAG_DISPLAY_SELECTION_COMBO = false;

	public final DiffDecorator DIFF_OFF = new DiffDecorator( "off" ){
		public JLabel decorate( JLabel label, JTable table, boolean isSelected, int row, int column ){
			if( !isSelected ) label.setBackground( COLOR_DEFAULT );
			return label;
		}
	};
	public final DiffDecorator DIFF_LOCAL = new DiffLocal( "local" );
	public final DiffDecorator DIFF_GLOBAL = new DiffLocal( "global" ){
		public boolean decideDiff( int indexColumnSelected, int column ){ return true; }
	};
	public final DiffDecorator DIFF_DEFAULT = DIFF_LOCAL;
	public final DiffDecorator[] ARRAY_DIFF_OPTIONS = new DiffDecorator[] { DIFF_LOCAL, DIFF_GLOBAL, DIFF_OFF };

	public static final Object OBJ_SINGLE_SELECTION = "single";
	public static final Object OBJ_SINGLE_INTERVAL_SELECTION = "interval";
	public static final Object OBJ_SELECTION_DEFAULT = OBJ_SINGLE_SELECTION;
	public static final Object[] ARRAY_SELECTION_OPTIONS = new Object[] { OBJ_SINGLE_SELECTION, OBJ_SINGLE_INTERVAL_SELECTION };

	private SloppyTableModel myTableModel;
	private double myPrEInverse;
	private JTable myJTable;
	private JScrollPane myPain;
	private IncrementPanel myIPBreadth;
	private IncrementPanel myIPOffset;

	private JButton myButtonScrollLeft;
	private JButton myButtonScrollRight;
	private JSlider mySliderBreadth;
	private boolean myFlagListenSlider;

	private JLabel myLabelSolutionNumber;
	private JLabel myLabelScoreJoint;
	private JLabel myLabelScoreConditioned;
	private JButton myButtonCopy, myButtonCopyPlusEvisence;
	private Action myActionCopy, myActionCopyPlus;
	private InstantiationClipBoard myClipBoard;
	private EvidenceController myEvidenceController;

	private WholeNumberField myTfJump;
	private JButton myButtonJump;

	private JComboBox myComboDiff;
	private DiffDecorator myDiffDecorator;
	private JButton myButtonHide;
	private JButton myButtonRefresh;

	private JComboBox myComboSelection;
}
