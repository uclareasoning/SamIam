package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.decision.*;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.util.WeakLinkedList;
import edu.ucla.util.Interruptable;

import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.*;

import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

/**
	@author Keith Cascio
	@since 121304
*/
public class GroupingColoringTableModel extends AbstractTableModel implements TableCellRenderer, DecisionListener
{
	public static final boolean FLAG_REFRESH_THREADED = true;

	public static final String STR_NAME_COLUMN_OUTCOME = "outcome";
	//public static final String STR_NAME_COLUMN_VALUE = "value";

	public static final Color COLOR_DEFAULT = Color.white;
	public static final float FLOAT_HUE_ANGLE_ZERO = (float)0;
	public static final float FLOAT_HUE_ANGLE_ONE = (float)1;
	public static final float FLOAT_SATURATION_DEFAULT = (float)0.2;
	public static final float FLOAT_BRIGHTNESS_DEFAULT = (float)1;

	public GroupingColoringTableModel( GroupingColoringJTable table )
	{
		this.myGroupingColoringJTable = table;
		this.myData = myDataFlat;
		init();
	}

	/** override this method */
	public void configureJTable( GroupingColoringJTable jtable ){
		jtable.setDefaultRenderer( Object.class, (TableCellRenderer)this );
	}

	/** override this method */
	public Object getOutcome( int rowIndex ){
		return "GroupingColoringTableModel";
	}

	/** override this method */
	public String outcomeToString( Object outcome ){
		return outcome.toString();
	}

	/** override this method */
	public String instanceToString( Object instance ){
		return instance.toString();
	}

	/** override this method */
	public void setOutcome( Object instance, Object outcome, Object newValue, int rowIndex ){
	}

	public void setDecisionNode( DecisionNode node )
	{
		if( this.myDecisionNode != node )
		{
			if( myDecisionNode != null ) myDecisionNode.removeListener( (DecisionListener)this );
			this.myDecisionNode = node;
			node.addListener( (DecisionListener)this );
			refresh( false );
		}
	}

	/** @since 011105 */
	public DecisionNode getDecisionNode(){
		return this.myDecisionNode;
	}

	public void refresh( boolean inthread ){
		myRunRefresh.run( inthread && FLAG_REFRESH_THREADED );
	}

	public class RunRefresh extends Interruptable{
		public void runImpl( Object arg1 ) throws InterruptedException{
			GroupingColoringTableModel.this.refreshImpl();
		}
		//public boolean debug() { return true; }
		//public boolean verbose() { return true; }
		//public String getNameMethodOfInterest(){ return "refreshImpl"; }
	}

	public void refreshImpl() throws InterruptedException
	{
		//System.out.println( "GroupingColoringTableModel.refreshImpl() thread " + Thread.currentThread().getName() );

		synchronized( myRunRefresh.getSynch() ){
			Thread.sleep(4);//Interruptable.checkInterrupted();
			//System.out.println( "    GCTM.refreshImpl() in synch " + Thread.currentThread().getName() );
			this.myVariable = myDecisionNode.getVariable();
			this.mySize = myVariable.size();
			this.myNameValueColumn = myVariable.toString();
			myFlagNodeEditable = myDecisionNode.isEditable();

			Thread.sleep(4);//Interruptable.checkInterrupted();

			this.myFlagGroupingClean = false;
			this.myFlagColoringClean = false;
			setGroupingEnabled( myFlagGroupingEnabled );
			Thread.sleep(4);//Interruptable.checkInterrupted();
			setColoringEnabled( myFlagColoringEnabled );
			Thread.sleep(4);//Interruptable.checkInterrupted();
			fireTableDataChanged();
			//myGroupingColoringJTable.revalidate();
			//myGroupingColoringJTable.repaint();
			//System.out.println( "    GCTM.refreshImpl() "+Thread.currentThread().getName()+" finished at: " + System.currentTimeMillis() );
		}
	}

	/** interface DecisionListener */
	public void decisionEvent( DecisionEvent e ){
		//System.out.println( getClass().getName() + ".decisionEvent( "+e+" )" );
		if( e.node == myDecisionNode ){
			if( (e.type == DecisionEvent.ASSIGNMENT_CHANGE) || (e.type == DecisionEvent.RENAME) ){
				refresh( true );
			}
		}
	}

	/** interface PopupMenuListener */
	//public void popupMenuWillBecomeVisible(PopupMenuEvent e){}
	/** interface PopupMenuListener */
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e){}
	/** interface PopupMenuListener */
	public void popupMenuCanceled(PopupMenuEvent e){}

	public GroupingColoringJTable getJTable(){
		return this.myGroupingColoringJTable;
	}

	public GroupingColoringPopupHandler getPopupHandler(){
		if( myGroupingColoringPopupHandler == null )
			myGroupingColoringPopupHandler = new GroupingColoringPopupHandler( (GroupingColoringTableModel)this );
		return myGroupingColoringPopupHandler;
	}

	public Color getGridColorSouth( int rowIndex ){
		return myData.getGridColorSouth( rowIndex );
		//return Color.red;
	}

	public void setGridColorDefault( Color color ){
		this.myGridColorDefault = color;
	}

	public void setSaturation( float saturation ){
		if( saturation != this.mySaturation ){
			this.mySaturation = saturation;
			refresh( true );
		}
	}

	public float getSaturation(){
		return this.mySaturation;
	}

	public void setColoringEnabled( boolean flag ) throws InterruptedException{
		this.myFlagColoringEnabled = flag;
		Thread.sleep(4);//Interruptable.checkInterrupted();
		if( flag ) validateColorMap();
	}

	public boolean isColoringEnabled(){
		return this.myFlagColoringEnabled;
	}

	private Color getColorFor( Object outcome ){
		synchronized( mySynchValidColorData ){
		if( myFlagColoringEnabled ){
			return (Color) myColorMap.get( outcome );
		}
		else return COLOR_DEFAULT;
		}//end synchronized
	}

	private void validateColorMap() throws InterruptedException{
		synchronized( mySynchValidColorData ){
		if( myFlagColoringClean ) return;

		if( myColorMap == null ) myColorMap = new HashMap( mySize );
		else myColorMap.clear();

		Thread.sleep(4);//Interruptable.checkInterrupted();

		Set outcomes = new HashSet( mySize );
		List order = new LinkedList();
		Object outcome;
		for( int i=0; i<mySize; i++ ){
			Thread.sleep(4);//Interruptable.checkInterrupted();
			outcome = getOutcome( i );
			if( !outcomes.contains( outcome ) ){
				outcomes.add( outcome );
				order.add( outcome );
			}
		}
		int numOutcomes = outcomes.size();

		float huelength = Math.abs( myHueCeiling - myHueFloor );
		float delta = (huelength)/((float)numOutcomes);
		//Color.getHSBColor(float h, float s, float b);
		float angle = myHueFloor;
		for( Iterator it = order.iterator(); it.hasNext(); ){
			Thread.sleep(4);//Interruptable.checkInterrupted();
			myColorMap.put( it.next(), Color.getHSBColor( angle, mySaturation, myBrightness ) );
			angle += delta;
		}
		}//end synchronized

		if( myGroupingColoringJTable != null ) myGroupingColoringJTable.repaint();
	}

	/** @since 011205 */
	public void setHueRange( float floor, float ceiling ) throws InterruptedException{
		this.myHueFloor = floor;
		this.myHueCeiling = ceiling;
		this.myFlagColoringClean = false;
		if( this.isColoringEnabled() ) validateColorMap();
	}

	public void setGroupingEnabled( boolean flag ) throws InterruptedException{
		this.myFlagGroupingEnabled = flag;
		Thread.sleep(4);//Interruptable.checkInterrupted();
		if( flag ){
			if( myDataGrouped == null ) myDataGrouped = this.new DataGrouped();
			validateDataGrouped();
			myData = myDataGrouped;
		}
		else{
			if( myDataFlat == null ) myDataFlat = this.new DataFlat();
			myData = myDataFlat;
		}
		Thread.sleep(4);//Interruptable.checkInterrupted();
		fireTableDataChanged();
	}

	public boolean isGroupingEnabled(){
		return this.myFlagGroupingEnabled;
	}

	private void validateDataGrouped() throws InterruptedException{
		synchronized( mySynchValidGroupedData ){
		if( myFlagGroupingClean ) return;

		validateColorMap();

		if( myMapOutcomeToInstanceList == null ) myMapOutcomeToInstanceList = new HashMap( mySize );
		else myMapOutcomeToInstanceList.clear();

		Map map = myMapOutcomeToInstanceList;
		Object outcome;
		List outcomes = new LinkedList();
		Object instance;
		List instances;
		for( int i=0; i<mySize; i++ ){
			Thread.sleep(4);//Interruptable.checkInterrupted();
			outcome = getOutcome( i );
			instance = myVariable.instance( i );
			if( map.containsKey( outcome ) ) ((List)map.get( outcome )).add( instance );
			else{
				outcomes.add( outcome );
				(instances = new LinkedList()).add( instance );
				map.put( outcome, instances );
			}
		}

		myStructs = new Struct[ mySize ];
		int countergroup;
		int indexMaster;
		int counterinstances = 0;
		Struct groupend;
		for( Iterator itOutcomes = outcomes.iterator(); itOutcomes.hasNext(); ){
			Thread.sleep(4);//Interruptable.checkInterrupted();
			instances = (List) map.get( outcome = itOutcomes.next() );
			countergroup = 0;
			indexMaster = (int) Math.floor( ((double)instances.size())/((double)2.1) );
			for( Iterator itInstances = instances.iterator(); itInstances.hasNext(); ){
				Thread.sleep(4);//Interruptable.checkInterrupted();
				myStructs[ counterinstances++ ] = new Struct( instance = itInstances.next(), outcome, (countergroup++)==indexMaster, getColorFor(outcome) );
			}
			groupend = myStructs[ counterinstances-1 ];
			groupend.gridcolorsouth = myGridColorDefault;
			groupend.flagendsgroup = true;
		}
		}//end synchronized
	}

	public List getGroupForOutcome( Object outcome ){
		synchronized( mySynchValidGroupedData ){
		if( isGroupingEnabled() ) return (List) myMapOutcomeToInstanceList.get( outcome );
		else return (List)null;
		}//end synchronized
	}

	/** @since 011505 */
	private Struct getStruct( int rowIndex ){
		synchronized( mySynchValidGroupedData ){
			return myStructs[ rowIndex ];
		}//end synchronized
	}

	private Object getGroupedInstance( int rowIndex ){
		return getStruct( rowIndex ).instance;
	}

	private Object getGroupedOutcome( int rowIndex ){
		return getStruct( rowIndex ).outcome;
	}

	private Object getGroupedOutcomeDisplayValue( int rowIndex ){
		return getStruct( rowIndex ).displayvalue;
	}

	private boolean isGroupedEditable( int rowIndex ){
		return getStruct( rowIndex ).flagmaster;
	}

	private Color getGroupedGridColorSouth( int rowIndex ){
		return getStruct( rowIndex ).getGridColorSouth();
	}

	private void setGroupedOutcome( Object aValue, int rowIndex ){
		setOutcome( getGroupedInstance( rowIndex ), getGroupedOutcome( rowIndex ), aValue, rowIndex );
	}

	public final void init()
	{
		//System.out.println( getClass().getName() + ".init()" );
		this.myGridColorDefault = myGroupingColoringJTable.getGridColor();
		//System.out.println( "    myGridColorDefault: " + myGridColorDefault );
	}

	public void setDefaultTableCellRenderer( TableCellRenderer renderer ){
		myDefaultTableCellRenderer = renderer;
	}

	public TableCellRenderer getDefaultTableCellRenderer(){
		return myDefaultTableCellRenderer;
	}

	public String getColumnName( int column )
	{
		if( column == 0 ) return myNameValueColumn;
		else if( column == 1 ) return STR_NAME_COLUMN_OUTCOME;
		else return "getColumnName() Error";
	}

	public int getRowCount()
	{
		return mySize;
	}

	public int getColumnCount()
	{
		return 2;
	}

	public void setEditable( boolean flag ){
		this.myFlagEditable = flag;
	}

	public boolean isEditable(){
		return this.myFlagEditable;
	}

	private boolean myFlagEditable = false;
	private boolean myFlagNodeEditable = false;
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return (columnIndex == 1) && myData.isEditableAt( rowIndex ) && myFlagEditable && myFlagNodeEditable;
	}

	public interface Data{
		public String getInstanceAt( int rowIndex );
		public Object getOutcomeAt( int rowIndex );
		public String getOutcomeDisplayValueAt( int rowIndex );
		public void setOutcomeAt( Object aValue, int rowIndex );
		public Color getColorAt( int rowIndex );
		public boolean isEditableAt( int rowIndex );
		public Color getGridColorSouth( int rowIndex );
	}

	public class DataFlat implements Data{
		public String getInstanceAt( int rowIndex ){
			return instanceToString( myVariable.instance( rowIndex ) );
		}
		public Object getOutcomeAt( int rowIndex ){
			return getOutcome( rowIndex );
		}
		public String getOutcomeDisplayValueAt( int rowIndex ){
			return outcomeToString( getOutcome( rowIndex ) );
		}
		public void setOutcomeAt( Object aValue, int rowIndex ){
			setOutcome( myVariable.instance( rowIndex ), getOutcome( rowIndex ), aValue, rowIndex );
		}
		public Color getColorAt( int rowIndex ){
			return getColorFor( getOutcome( rowIndex ) );
		}
		public boolean isEditableAt( int rowIndex ){
			return true;
		}
		public Color getGridColorSouth( int rowIndex ){
			return myGridColorDefault;
		}
	}

	public class DataGrouped implements Data{
		public String getInstanceAt( int rowIndex ){
			return instanceToString( getGroupedInstance( rowIndex ) );
		}
		public Object getOutcomeAt( int rowIndex ){
			return getGroupedOutcome( rowIndex );
		}
		public String getOutcomeDisplayValueAt( int rowIndex ){
			return outcomeToString( getGroupedOutcomeDisplayValue( rowIndex ) );
		}
		public void setOutcomeAt( Object aValue, int rowIndex ){
			setGroupedOutcome( aValue, rowIndex );
		}
		public Color getColorAt( int rowIndex ){
			return getColorFor( getGroupedOutcome( rowIndex ) );
		}
		public boolean isEditableAt( int rowIndex ){
			return isGroupedEditable( rowIndex );
		}
		public Color getGridColorSouth( int rowIndex ){
			return getGroupedGridColorSouth( rowIndex );
		}
	}

	public Object getOutcomeAt( int rowIndex ){
		return myData.getOutcomeAt( rowIndex );
	}

	public Object getInstanceAt( int rowIndex ){
		return myData.getInstanceAt( rowIndex );
	}

	public Object getValueAt( int rowIndex, int column )
	{
		if( column == 0 ) return myData.getInstanceAt( rowIndex );
		else if( column == 1 ) return myData.getOutcomeDisplayValueAt( rowIndex );
		else return "getValueAt("+rowIndex+","+column+") Error";
	}

	public void setValueAt( Object aValue, int rowIndex, int column )
	{
		if( column == 1 ) myData.setOutcomeAt( aValue, rowIndex );
		else throw new IllegalArgumentException();
	}

	public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
	{
		column = table.convertColumnIndexToModel( column );
		if( myDefaultTableCellRenderer == null ) return myPanelError;
		else{
			Component ret = myDefaultTableCellRenderer.getTableCellRendererComponent( table, value, isSelected, false, row, column );
			if( !isSelected ) ret.setBackground( myData.getColorAt( row ) );
			return ret;
		}
	}

	public class Struct
	{
		public Struct( Object instance, Object outcome, boolean flagmaster, Color gridcolorsouth )
		{
			this.instance = instance;
			this.outcome = outcome;
			this.flagmaster = flagmaster;
			this.displayvalue = flagmaster ? outcome : null;
			this.gridcolorsouth = gridcolorsouth;
		}

		public Color getGridColorSouth(){
			if( myFlagColoringEnabled ) return gridcolorsouth;
			//else return flagendsgroup ? Color.black : COLOR_DEFAULT;
			else return flagendsgroup ? myGridColorDefault : COLOR_DEFAULT;
		}

		public Object instance;
		public Object outcome;
		public Object displayvalue;
		public boolean flagmaster;
		public Color gridcolorsouth;
		public boolean flagendsgroup = false;
	}

	/** @since 011005 */
	public interface Listener{
		public void tableEditWarning();
	}

	/** @since 011005 */
	protected void fireEditWarning(){
		Listener next;
		for( Iterator it = myListeners.iterator(); it.hasNext(); ){
			next = (Listener) it.next();
			if( next == null ) it.remove();
			else next.tableEditWarning();
		}
	}

	/** @since 011005 */
	public void addListener( Listener listener ){
		if( myListeners == null ) myListeners = new WeakLinkedList();
		myListeners.addLast( listener );
	}

	/** @since 011005 */
	public boolean removeListener( Listener listener ){
		if( myListeners == null ) return false;
		else return myListeners.remove( listener );
	}

	private boolean myFlagGroupingEnabled = true;
	private boolean myFlagColoringEnabled = true;
	private boolean myFlagGroupingClean = false;
	private boolean myFlagColoringClean = false;

	private DecisionNode myDecisionNode;
	private FiniteVariable myVariable;
	private int mySize;
	private String myNameValueColumn;

	private JPanel myPanelError;
	private Struct[] myStructs;
	private Map myMapOutcomeToInstanceList;
	private DataFlat myDataFlat = this.new DataFlat();
	private DataGrouped myDataGrouped;
	private Data myData = myDataFlat;

	private float mySaturation = FLOAT_SATURATION_DEFAULT;
	private float myBrightness = FLOAT_BRIGHTNESS_DEFAULT;
	private float myHueFloor = FLOAT_HUE_ANGLE_ZERO;
	private float myHueCeiling = FLOAT_HUE_ANGLE_ONE;
	private Map myColorMap;
	private TableCellRenderer myDefaultTableCellRenderer;
	private Color myGridColorDefault;

	private RunRefresh myRunRefresh = new RunRefresh();
	private Object mySynchValidGroupedData = new Object(), mySynchValidColorData = new Object();

	protected GroupingColoringJTable myGroupingColoringJTable;
	private GroupingColoringPopupHandler myGroupingColoringPopupHandler;
	private WeakLinkedList myListeners;
}
