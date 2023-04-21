package edu.ucla.belief.ui.displayable;

import edu.ucla.util.Interruptable;

//import edu.ucla.belief.ui.util.Interruptable;
import edu.ucla.belief.ui.util.Util;

//import edu.ucla.belief.*;
//import edu.ucla.belief.decision.DecisionNode;
//import edu.ucla.belief.decision.DecisionLeaf;
//import edu.ucla.belief.decision.DecisionTree;
//import edu.ucla.belief.decision.Factory;
//import edu.ucla.util.WeakLinkedList;

import java.util.List;
//import java.util.Iterator;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Comparator;
//import java.util.Collections;
//import java.util.Set;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
//import javax.swing.event.MenuListener;
//import javax.swing.event.MenuEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.ActionEvent;
//import java.awt.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.PopupMenu;

/**
	@author Keith Cascio
	@since 121104
*/
public class MenuList extends JPanel implements ListDataListener, MenuElement
{
	public static final int INT_HEIGHT_BUFFER = 8;
	public static final int INT_WIDTH_BUFFER = 8;
	public static final int INT_WIDTH_BUFFER_SCROLL = 32;
	public static final int INT_HEIGHT_CEILING = 128;

	public static final String STR_CAPTION_SINGLE_SELECTION = "choose one:";
	public static final String STR_CAPTION_INTERVAL_SELECTION = "choose one or more:";

	public MenuList( ListModel model ){
		super( new GridBagLayout() );
		this.myModel = model;
		init();
		modelChanged();
	}

	/** interface MenuElement */
	public void processMouseEvent( MouseEvent event, MenuElement[] path, MenuSelectionManager manager ){
		//System.out.println( "MenuList.processMouseEvent()" );
		int id = event.getID();
		//if( id == MouseEvent.MOUSE_PRESSED ){
		//	System.out.println( event.getComponent().getClass().getName() );
		//}
		//else
		if( (id == MouseEvent.MOUSE_DRAGGED) && (mySpecialMouseMotionListeners != null) ){
			for( int i=0; i<mySpecialMouseMotionListeners.length; i++ )
				mySpecialMouseMotionListeners[i].mouseDragged( event );
		}
		//else if( (id == MouseEvent.MOUSE_RELEASED) && (mySpecialMouseListeners != null ) ){
		//	for( int i=0; i<mySpecialMouseListeners.length; i++ )
		//		mySpecialMouseListeners[i].mouseReleased( event );
		//}
	}

	/** interface MenuElement */
	public void processKeyEvent( KeyEvent event, MenuElement[] path, MenuSelectionManager manager ){}

	/** interface MenuElement */
	public void menuSelectionChanged( boolean isIncluded ){}

	/** interface MenuElement */
	public MenuElement[] getSubElements(){
		return ARRAYSUBELEMENTS;
	}

	private static final MenuElement[] ARRAYSUBELEMENTS = new MenuElement[0];

	/** interface MenuElement */
	public Component getComponent(){
		return (Component)this;
	}

	public void setCaption( String caption ){
		this.myCaption = caption;
	}

	public String getCaption(){
		return validateCaption();
	}

	public void modelChanged(){
		myRunModelChanged.start();
	}

	public class RunModelChanged extends Interruptable{
		public void runImpl( Object arg1 ) throws InterruptedException{
			MenuList.this.modelChangedImpl();
		}
	}

	public void modelChangedImpl() throws InterruptedException
	{
		synchronized( myRunModelChanged.getSynch() ){
			Thread.sleep(4);//Interruptable.checkInterrupted();
			myJList.clearSelection();
			Thread.sleep(4);//Interruptable.checkInterrupted();
			validateSize();
			Thread.sleep(4);//Interruptable.checkInterrupted();
			JScrollBar vertical = myJScrollPane.getVerticalScrollBar();
			if( vertical != null ){
				//mySpecialMouseListeners = vertical.getMouseListeners();
				mySpecialMouseMotionListeners = vertical.getMouseMotionListeners();
			}
			Thread.sleep(4);//Interruptable.checkInterrupted();
			repaint();
		}
	}

	/** interface ListDataListener */
	public void contentsChanged(ListDataEvent e){
		modelChanged();
	}
	/** interface ListDataListener */
	public void intervalRemoved(ListDataEvent e){
		modelChanged();
	}
	/** interface ListDataListener */
	public void intervalAdded(ListDataEvent e){
		modelChanged();
	}

	/** JList convenience */
	public boolean isSelectionEmpty(){
		return myJList.isSelectionEmpty();
	}

	/** JList convenience */
	public void addListSelectionListener( ListSelectionListener listener ){
		myJList.addListSelectionListener( listener );
	}

	/** JList convenience */
	public void removeListSelectionListener( ListSelectionListener listener ){
		myJList.removeListSelectionListener( listener );
	}

	/** JList convenience */
	public Object[] getSelectedValues(){
		return myJList.getSelectedValues();
	}

	/** JList convenience */
	public Object getSelectedValue(){
		return myJList.getSelectedValue();
	}

	/** JList convenience */
	public void setSelectedValue( Object anObject ){
		myJList.setSelectedValue( anObject, /*shouldScroll*/true );
	}

	/** JList convenience */
	public void setSelectionMode( int selectionMode ){
		myJList.getSelectionModel().setSelectionMode( selectionMode );
		validateCaption();
	}

	/** JList convenience */
	public int getSelectionMode(){
		return myJList.getSelectionModel().getSelectionMode();
	}

	public void validateSize(){
		//boolean debug = myModel instanceof DefaultComboBoxModel;
		//if( debug ) System.out.println( "MenuList.validateSize()" );
		Dimension scrolldim = myJScrollPane.getPreferredSize();
		Dimension listdim = myJList.getPreferredSize();

		scrolldim.height = Math.min( listdim.height + INT_HEIGHT_BUFFER, INT_HEIGHT_CEILING );
		scrolldim.width = listdim.width + INT_WIDTH_BUFFER;
		if( listdim.height >= INT_HEIGHT_CEILING ) scrolldim.width += INT_WIDTH_BUFFER_SCROLL;

		myJScrollPane.setPreferredSize( scrolldim );
		//if( debug ) System.out.println( "    " + scrolldim );
	}

	private String validateCaption(){
		String caption = null;
		if( myCaption != null ) caption = myCaption;
		else{
			if( getSelectionMode() == ListSelectionModel.SINGLE_SELECTION ) caption = STR_CAPTION_SINGLE_SELECTION;
			else caption = STR_CAPTION_INTERVAL_SELECTION;
		}
		myLabelCaption.setText( caption );
		return caption;
	}

	private void init(){
		myJList = new JList( myModel );
		myJScrollPane = new JScrollPane( myJList );
		JComponent listComp = myJScrollPane;
		listComp.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( myLabelCaption = new JLabel( "", JLabel.LEFT ), c );
		this.add( listComp, c );

		myModel.addListDataListener( (ListDataListener)this );
		validateCaption();
	}

	public void printDebugInfo( java.io.PrintStream stream ){
		stream.println( "MenuList debug information:" );
		stream.println( "    this.size() " + this.size() );
		stream.println( "    myJList.size() " + myJList.size() );
		stream.println( "    myJScrollPane.size() " + myJScrollPane.size() );
	}

	/** test/debug */
	public static void main( String[] args )
	{
		Util.setLookAndFeel();

		Object[] array = new Object[] { "1_asdasd","2_asjdghaskjdgh","2_9a7sdsa","4_AS*97dgdas","5_askuydas7","6_234","7_askdu","8_asjdg7sa","9_a87sdgasd","10_askduasd","11_8a" };
		ListModel model = new DefaultComboBoxModel( array );
		MenuList list1 = new MenuList( model );
		MenuList list2 = new MenuList( model );
		MenuList list3 = new MenuList( model );

		//JPanel pnlList2 = new JPanel();
		//pnlList2.add( list2 );

		JComponent list2component = list2;

		final JMenu jmenu = new JMenu( "assign" );
		//final JMenu jmenu = MenuWorkaroundManager.createMenu( "assign" );
		//final KeithMenu jmenu = new KeithMenu( "assign" );
		jmenu.add( list2component );
		jmenu.add( new JMenuItem( "OK" ) );

		//final JPopupMenu popup1 = MenuWorkaroundManager.createPopupMenu( "nested" );
		final JPopupMenu popup1 = new JPopupMenu( "nested" );
		popup1.add( jmenu );

		//final JPopupMenu popup2 = MenuWorkaroundManager.createPopupMenu( "flat" );
		final JPopupMenu popup2 = new JPopupMenu( "flat" );
		popup2.add( list3 );
		popup2.add( new JMenuItem( "OK" ) );

		final JMenu jmenu2 = new JMenu( "scroll test" );
		//final JMenu jmenu2 = MenuWorkaroundManager.createMenu( "scroll test" );
		//final JMenu jmenu2 = new KeithMenu( "scroll test" );
		jmenu2.add( createScrollComponent() );
		jmenu2.add( new JMenuItem( "OK" ) );

		final JPopupMenu popup3 = new JPopupMenu( "scroll nested" );
		//final JPopupMenu popup3 = MenuWorkaroundManager.createPopupMenu( "scroll nested" );
		popup3.add( jmenu2 );

		final JPopupMenu popup4 = new JPopupMenu( "scroll flat" );
		//final JPopupMenu popup4 = MenuWorkaroundManager.createPopupMenu( "scroll flat" );
		popup4.add( createScrollComponent() );
		popup4.add( new JMenuItem( "OK" ) );

		//System.out.println( "nested:" + jmenu2.getPopupMenu() );
		//System.out.println( "flat  :" + popup4 );

		//final Menu menu5 = new Menu( "AWT test" );
		//menu5.add( createScrollComponent() );
		//menu5.add( new MenuItem( "OK" ) );
		//final PopupMenu popup5 = new PopupMenu( "AWT scroll nested" );
		//popup5.add( menu5 );

		JPanel pnlMain = new JPanel();
		pnlMain.add( list1 );
		pnlMain.add( createEmptyPanel( "click here for nested popup", Color.red, popup1 ) );
		pnlMain.add( createEmptyPanel( "click here for flat popup", Color.green, popup2 ) );
		pnlMain.add( createEmptyPanel( "click here for nested scroll test", Color.blue, popup3 ) );
		pnlMain.add( createEmptyPanel( "click here for flat scroll test", Color.pink, popup4 ) );
		//pnlMain.add( createEmptyPanel( "click here for awt scroll test", Color.yellow, popup5 ) );
		pnlMain.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );

		JFrame frame = Util.getDebugFrame( "MenuList TEST", pnlMain );
		final Container contentpain = frame.getContentPane();

		frame.setSize( new Dimension( 900,300 ) );
		Util.centerWindow( frame );
		frame.setVisible( true );
	}

	/** test/debug */
	private static JComponent createScrollComponent(){
		JScrollBar jsb = new JScrollBar( JScrollBar.VERTICAL );
		//JScrollBar jsb = new KeithScrollBar( JScrollBar.VERTICAL );
		jsb.setModel( new DefaultBoundedRangeModel( 0, 7, 0, 16 ) );

		//jsb.addMouseListener( new KeithMouseListener() );

		JPanel pnlCaption = new JPanel();
		pnlCaption.add( new JLabel( "scroll panel" ) );
		pnlCaption.setPreferredSize( new Dimension( 64, 90 ) );

		JPanel pnlScroll = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		pnlScroll.add( pnlCaption, c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = c.weighty = 1;
		pnlScroll.add( jsb, c );

		return pnlScroll;
	}

	/** test/debug */
	private static void addListener( final JComponent comp, final JPopupMenu swingmenu, final PopupMenu awtmenu ){
		comp.addMouseListener( new MouseAdapter(){
			public void mousePressed( MouseEvent e ){ showPopup( e ); }
			public void mouseClicked( MouseEvent e ){ showPopup( e ); }
			public void mouseReleased( MouseEvent e ){ showPopup( e ); }

			public void showPopup( MouseEvent e ){
				//System.out.println( "showPopup()" );
				if( e.isPopupTrigger() ){
					if( swingmenu != null ) swingmenu.show( comp, e.getX(), e.getY() );
					if( awtmenu != null ) awtmenu.show( comp, e.getX(), e.getY() );
				}
			}
		} );
	}

	/** test/debug */
	private static JComponent createEmptyPanel( String caption, Color bcolor, Object menu ){
		final JPanel panelEmpty1 = new JPanel();
		panelEmpty1.add( new JLabel( caption ) );
		panelEmpty1.setPreferredSize( new Dimension( 150,175 ) );
		panelEmpty1.setBorder( BorderFactory.createLineBorder( bcolor, 1 ) );
		if( menu instanceof JPopupMenu ) addListener( panelEmpty1, (JPopupMenu)menu, (PopupMenu)null );
		else if( menu instanceof PopupMenu ) addListener( panelEmpty1, (JPopupMenu)null, (PopupMenu)menu );
		return panelEmpty1;
	}

	//public static final PrintStream STREAM_DEBUG = System.out;
	/*public static class KeithMouseListener extends MouseMotionAdapter implements MouseListener, MouseMotionListener{
		public void mouseReleased(MouseEvent e){
			STREAM_DEBUG.println( "KML.mouseReleased()" );
		}
		public void mousePressed(MouseEvent e){
			STREAM_DEBUG.println( "KML.mousePressed()" );
		}
		public void mouseDragged(MouseEvent e){
			STREAM_DEBUG.println( "KML.mouseDragged()" );
		}
	}
	public static class KeithScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI{
		public TrackListener createTrackListener(){
			return new KeithTrackListener();
		}
		public class KeithTrackListener extends TrackListener{
			public KeithTrackListener(){
				super();
				STREAM_DEBUG.println( "KeithTrackListener()" );
			}
			public void mouseReleased(MouseEvent e){
				STREAM_DEBUG.println( "KTL.mouseReleased()" );
				super.mouseReleased( e );
			}
			public void mousePressed(MouseEvent e){
				STREAM_DEBUG.println( "KTL.mousePressed()" );
				super.mousePressed( e );
			}
			public void mouseDragged(MouseEvent e){
				STREAM_DEBUG.println( "KTL.mouseDragged()" );
				super.mouseDragged( e );
			}
		}
	}
	public static class KeithScrollBar extends JScrollBar{
		public KeithScrollBar( int orientation ){
			super( orientation );
		}
		/%% Overrides <code>JScrollBar.updateUI</code>.%/
		public void updateUI() {
			setUI( new KeithScrollBarUI() );
		}
	}*/

	private String myCaption;

	private ListModel myModel;
	private JList myJList;
	private JScrollPane myJScrollPane;
	private JLabel myLabelCaption;
	private RunModelChanged myRunModelChanged = new RunModelChanged();
	private MouseMotionListener[] mySpecialMouseMotionListeners;
	private MouseListener[] mySpecialMouseListeners;
}
