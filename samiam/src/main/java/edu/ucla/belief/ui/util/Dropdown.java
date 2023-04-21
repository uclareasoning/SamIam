package edu.ucla.belief.ui.util;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;

import static java.lang.Thread.sleep;
import static java.lang.Thread.yield;

/** Derived from JComboBox, but slightly different semantics support<br />
	(1) selection of multiple values<br />
	(2) bounding the selection cardinality (lower and upper)

	@author keith cascio
	@since  20070426 */
public class Dropdown<E> extends JComboBox implements KeyListener//, ListSelectionListener
{
	/** @param items           contents
		@param boundsInclusive array of size 2, 1st element is an inclusive lower bound, 2nd element is an inclusive upper bound
		@param plural          an english word describing a plurality of the contents (e.g. "sheep"), or null to show concise display text */
	public Dropdown( E[] items, int[] boundsInclusive, String plural ){
		super( items );
		this.items             = items;
		this.addKeyListener( this.reset( null, boundsInclusive, plural ) );
	}

	/** @since 20091208 */
	public Dropdown<E> reset( E[] items, int[] boundsInclusive, String plural ){
		if( items != null ){ super.setModel( new DefaultComboBoxModel( this.items = items ) ); }
		this.plural            = plural == null || plural.startsWith( " " ) ? plural : " " + plural;
		this.myBoundsInclusive = boundsInclusive;
		homogenize();
		return this;
	}

	/** @since    20080228 */
	static public class Flash implements Runnable{
		/** @since    20080228 */
		public static final Color
		  COLOR_FLASH = new Color( 0xcc, 0x66, 0xcc );

		public Flash( Component comp ){
			this.comp       = comp;
			this.background = comp.getBackground();
		}

		public void run(){
			try{
				yield();
				comp.transferFocusBackward();
				yield();
				comp.setBackground( COLOR_FLASH );
				comp.repaint();
				sleep( 0x100 );
				comp.setBackground( background );
				comp.repaint();
				yield();
			}catch( Throwable thrown ){
				System.err.println( "warning: Dropdown.Flash.run() caught " + thrown );
			}
		}

		public Thread start(){
			Thread thread = new Thread( this );
			thread.start();
			return thread;
		}

		final public Component comp;
		final public Color     background;
	}

	/** interface ListSelectionListener
		@since    20080228 */
	public void valueChanged( ListSelectionEvent e ){ tooltip(); }

	/** @since    20080228 */
	public String tooltip(){
		this.setToolTipText( "<html>" + getSelectedItem().toString() + " - select{ <font color='#006600'>all</font>: <b>Ctrl+A</b>, <font color='#990000'>none</font>: <b>Ctrl+0</b> }" );
		return this.getToolTipText();
	}

	/** interface KeyListener
		@since    20080228 */
	public void keyPressed(  KeyEvent keyevent ){
		if( (keyevent.getModifiers() & InputEvent.CTRL_MASK) < 1 ){ return; }
		boolean done = false;
		switch( keyevent.getKeyCode() ){
			case KeyEvent.VK_A: done = true; keyevent.consume();  all(); break;
			case KeyEvent.VK_0: done = true; keyevent.consume(); none(); break;
		}
		if( done ){
			if( runflash == null ){ runflash = new Flash( this ); }
			runflash.start();
		}
	}
	public void keyReleased( KeyEvent keyevent ){}
	public void keyTyped(    KeyEvent keyevent ){}

	public static final Insets
	  INSETS         = new Insets( 0, 0, 0, 0 );
	public static final String
	  UICLASSID_POPP = "edu.ucla.belief.ui.util.PopupUI",
	  UICLASSID_LIST = "edu.ucla.belief.ui.util.Popup.ListUI",
	  UICLASSID_BUTT = "edu.ucla.belief.ui.util.DropdownUI.ButtonUI",
	  UICLASSID_DROP = "edu.ucla.belief.ui.util.DropdownUI";

	public String getUIClassID(){ return UICLASSID_DROP; }

	/** @since 20100110 */
	public static final Class<Dropdown> lookAndFeelChanging(){
		String              fmt = "warning: Dropdown.lookAndFeelChanged() caught %s\n";
		try{
			LookAndFeel     laf = UIManager.getLookAndFeel();
			UIDefaults  defcon1 = UIManager.getDefaults();
			UIDefaults  defcon2 =       laf.getDefaults();
			String       dropnm = edu.ucla.belief.ui.util.Dropdown.DropdownUI.class.getName();
		  /*try{
				dropnm = (laf instanceof javax.swing.plaf.synth.SynthLookAndFeel) ?
				            javax.swing.plaf.synth.DropdownUI.class.getName() :
				  edu.ucla.belief.ui.util.Dropdown.DropdownUI.class.getName();
			                                            }catch( Throwable thr ){ System.err.printf( fmt, thr ); thr.printStackTrace(); }*/
			String[]   defaults = new String[]{
				UICLASSID_POPP,     javax.swing.plaf.basic.BasicPopupMenuUI.class.getName(),
				UICLASSID_LIST,     javax.swing.plaf.basic.     BasicListUI.class.getName(),
				UICLASSID_BUTT,     javax.swing.plaf.basic.   BasicButtonUI.class.getName(),
				UICLASSID_DROP, dropnm
			};
			try{ defcon1.putDefaults( defaults );       }catch( Throwable thr ){ System.err.printf( fmt, thr ); }
			try{ defcon2.putDefaults( defaults );       }catch( Throwable thr ){ System.err.printf( fmt, thr ); }
			try{
				if( laf instanceof javax.swing.plaf.synth.SynthLookAndFeel ){
					LookAndFeel     laf2 = null;
					try{ if( laf2 == null ){ laf2 = (LookAndFeel) Class.forName( "com.sun.java.swing.plaf.windows.WindowsLookAndFeel" ).newInstance(); } }catch( Throwable thr ){}
					try{ if( laf2 == null ){ laf2 = (LookAndFeel) Class.forName(            "javax.swing.plaf.metal.MetalLookAndFeel" ).newInstance(); } }catch( Throwable thr ){}
					UIDefaults   defcon3 = laf2.getDefaults();
					UIDefaults[]   dests = new UIDefaults[]{ defcon1, defcon2 };
					Object         value = null;
					for( String key : new String[]{ "ComboBox.background", "ComboBox.foreground", "ComboBox.border",
					  "ComboBox.buttonBackground", "ComboBox.buttonDarkShadow", "ComboBox.buttonHighlight", "ComboBox.buttonShadow",
					  "ComboBox.disabledBackground", "ComboBox.disabledForeground", "ComboBox.selectionBackground",
					  "ComboBox.selectionForeground", "List.background", "List.foreground", "List.selectionBackground", "List.selectionForeground",
					  "List.focusCellHighlightBorder", "List.border", "List.dropLineColor", "Button.contentMargins", "Button.padding", "List.contentMargins",
					  "ComboBox.squareButton"/*, "ComboBox.contentMargins", "ArrowButton.contentMargins", "ArrowButton.padding", "List.cellRenderer",
					  "PopupMenu.background", "PopupMenu.foreground", "PopupMenu.border", "ComboBox.popupInsets", "ComboBox.editorBorder","ComboBox.padding"*/ } ){
						if( (value = defcon3.get( key )) != null ){ defcon2.put( key, value ); }//for( UIDefaults dest : dests ){ dest.put( key, value ); } }
					}
					defcon2.put( "ComboBox.padding", INSETS );//for( UIDefaults dest : dests ){ dest.put( "ComboBox.padding", INSETS ); }
				}                                       }catch( Throwable thr ){ System.err.printf( fmt, thr ); }
		                                                }catch( Throwable thr ){ System.err.printf( fmt, thr ); }
		return Dropdown.class;
	}

	public    static    final           java.beans.PropertyChangeListener
	  LOOKANDFEELLISTENER = new         java.beans.PropertyChangeListener(){
			public void propertyChange( java.beans.PropertyChangeEvent evt ){
				if( evt.getPropertyName().intern() == "lookAndFeel" ){ lookAndFeelChanging(); }
			}
		};
	static{ lookAndFeelChanging();
		UIManager.addPropertyChangeListener( LOOKANDFEELLISTENER );
	}

	/** overridden to use our own variation on the basic platform ui - so it won't match the native ui so well */
    public void updateUI(){
	  /*UIManager.getDefaults().put( UICLASSID_DROP, edu.ucla.belief.ui.util.Dropdown.DropdownUI.class.getName() );//UIManager.getLookAndFeel().
		UIManager.getDefaults().put( UICLASSID_LIST,     javax.swing.plaf.basic.     BasicListUI.class.getName() );//UIManager.getLookAndFeel().
		UIManager.getDefaults().put( UICLASSID_BUTT,     javax.swing.plaf.basic.   BasicButtonUI.class.getName() );//UIManager.getLookAndFeel().
		UIManager.getDefaults().put( UICLASSID_POPP,     javax.swing.plaf.basic.BasicPopupMenuUI.class.getName() );//UIManager.getLookAndFeel().*/
		try{
			super.updateUI();
		}catch( Throwable thrown ){
			System.err.println( "warning: Dropdown.updateUI() caught " + thrown );
		}
		try{
			if( ui == null ){ setUI( new DropdownUI() ); }
		}catch( Throwable thrown ){
			System.err.println( "warning: Dropdown.updateUI() caught " + thrown );
		}
    	try{
			ListCellRenderer renderer = getRenderer();
			if (!(renderer instanceof UIResource) && renderer instanceof Component) {
				SwingUtilities.updateComponentTreeUI((Component)renderer);
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: Dropdown.updateUI() caught " + thrown );
		}
    }

	/** @since 20100109 */
	static public class DropdownUI extends BasicComboBoxUI{
		@Override
		protected ComboPopup createPopup(){
			return ((Dropdown)comboBox).new Popup( comboBox );
		}

		public static ComponentUI createUI( JComponent c ){
			return new DropdownUI();
		}

		@Override
		protected  JButton      createArrowButton(){
			JButton button = new BasicArrowButton( BasicArrowButton.SOUTH,
						UIManager.getColor( "ComboBox.buttonBackground" ),
						UIManager.getColor( "ComboBox.buttonShadow"     ),
						UIManager.getColor( "ComboBox.buttonDarkShadow" ),
						UIManager.getColor( "ComboBox.buttonHighlight"  ) ){
				public String getUIClassID(){ return UICLASSID_BUTT; }
			};
			button.setName("ComboBox.arrowButton");
			return button;
		}

		@Override
		protected ListCellRenderer    createRenderer(){
			return       new DefaultListCellRenderer(){
				public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus ){
					super       .getListCellRendererComponent(       list,        value,     index,         isSelected,         cellHasFocus );
					if( isSelected ){
						setBackground( list.getSelectionBackground() );
						setForeground( list.getSelectionForeground() );
					}else{
						setBackground( list.         getBackground() );
						setForeground( list.         getForeground() );
					}
					return this;
				}
			};
		}
	}

	/** set a precise English tool tip and ensure cardinality is within bounds */
    public Dropdown<E> homogenize(){
    	try{
    		int          lower = myBoundsInclusive[0], upper = myBoundsInclusive[1];
    		String    strLower = lower == 1 ? "one" : Integer.toString( lower );
			StringBuilder buff = new StringBuilder( 0x20 );
			buff.append( "you can select " );

			if( lower == upper ){
				buff.append( "exactly " );
				if( lower == 1 ) buff.append( "one value" );
				else             buff.append( strLower ).append( " values" );
			}
			else if( upper >= items.length || upper < lower ){
				if( lower > 0 ) buff.append( strLower ).append( " or more values" );
				else                          buff.append( "any number of values" );
			}
			else{
				if( lower > 0 ) buff.append( "at least " ).append( strLower ).append( " but not more than " ).append( upper ).append( " values" );
				else            buff.append( upper ).append( " or fewer values" );
			}

			myList.setToolTipText( buff.toString() );
		}catch( Exception exception ){
			System.err.println( "warning: Dropdown.homogenize() caught " + exception );
		}

		try{
			listSelectionValueChanged(-1,-1);
			repaint();
		}catch( Exception exception ){
			System.err.println( "warning: Dropdown.homogenize() caught " + exception );
		}

		return this;
    }

	/** count the # of selected items */
    public int cardinality(){
    	int card = 0;
    	for( int i=0; i<items.length; i++ ) if( model.isSelectedIndex( i ) ) ++card;
    	return card;
    }

	/** overridden to display helpful summary if cardinality > 1 */
    public Object getSelectedItem(){
    	try{
			int card = cardinality();
			return card == 1 ?
				myList.getSelectedValue() : (
					plural == null ?
						(                    "x" + Integer.toString( card )                ) :
						("(" + (card == 0 ? "no" : Integer.toString( card )) + plural + ")") );
		}catch( Throwable thrown ){
			return super.getSelectedItem();
		}
    }

	/** @return null-terminated array, created if null or too short */
	@SuppressWarnings( "unchecked" )
    public E[] getSelectedItems( E[] a ){
    	int card = cardinality();
        if( a == null || a.length < card ) a = (E[]) java.lang.reflect.Array.newInstance( a.getClass().getComponentType(), card );
    	int index = 0;
    	for( int i=0; i<items.length; i++ ) if( model.isSelectedIndex( i ) ) a[ index++ ] = items[i];
    	if( a.length > index ) Arrays.fill( a, index, a.length, null );//null-terminated
    	return a;
    }

	/** fill parameter bucket with selected items */
    public <T extends Collection<E>> T getSelection( T bucket ){
    	for( int i=0; i<items.length; i++ ) if( model.isSelectedIndex( i ) ) bucket.add( items[i] );
    	return bucket;
    }

	/** empty set will be an EnumSet if possible */
	@SuppressWarnings( "unchecked" )
    public Set<E> emptyBucket(){
    	Class  clazz = items[0].getClass();//items.getClass().getComponentType();
    	if(       clazz.isEnum() ){ return EnumSet.noneOf( clazz ); }
    	else if( (clazz.getSuperclass() != null) && (clazz.getSuperclass().isEnum()) ){ return EnumSet.noneOf( clazz.getSuperclass() ); }
    	else{ return new HashSet<E>( items.length ); }
    }

	/** dump a subset of items into a set */
	@SuppressWarnings( "unchecked" )
    public Set<E> bucketOf( Collection<E> proto ){
    	Class  clazz = items[0].getClass();
    	return clazz.isEnum() ? EnumSet.copyOf( (Collection<Enum>) proto ) : new HashSet<E>( proto );
    }

	/** get selected items */
    public Set<E> getSelection(){
    	return getSelection( emptyBucket() );
    }

	/** set selected items */
    public Dropdown<E> setSelection( Collection<E> bucket ){
    	try{
			bucket = moderate( bucket );
		}catch( Exception exception ){
			System.err.println( "warning: Dropdown.setSelection() caught " + exception );
		}
		try{
			ignore = true;
			model.clearSelection();
			for( int i=0; i<items.length; i++ ) if( bucket.contains( items[i] ) ) model.addSelectionInterval( i, i );
			tooltip();
		}catch( Exception exception ){
			System.err.println( "warning: Dropdown.setSelection() caught " + exception );
		}finally{
			ignore = false;
		}
		return this;
    }

    /** @since 20080228 */
    public Dropdown<E> all(){
		try{
			ignore = true;
			model.setSelectionInterval( 0, items.length );
			tooltip();
			repaint();
		}catch( Exception exception ){
			System.err.println( "warning: Dropdown.all() caught " + exception );
		}finally{
			ignore = false;
		}
		return this;
    }

    /** @since 20080228 */
    public Dropdown<E> none(){
		try{
			ignore = true;
			model.clearSelection();
			tooltip();
			repaint();
		}catch( Exception exception ){
			System.err.println( "warning: Dropdown.none() caught " + exception );
		}finally{
			ignore = false;
		}
		return this;
    }

	/** curb, arbitrate, mediate, referee, i.e. choose a subset with cardinality within bounds */
    public Collection<E> moderate( Collection<E> bucket ){
    	int size = bucket.size(), lower = myBoundsInclusive[0], upper = myBoundsInclusive[1];
    	if( lower <= size && size <= upper ) return bucket;

    	Set<E> ret = bucketOf( bucket );
    	if(      size < lower ){
    		for( Iterator<E> past   = getSelection().iterator(); ret.size() < lower && past  .hasNext();     ) ret.add( past.next() );
    		for(                                      int i = 0; ret.size() < lower && i < items.length; i++ ) ret.add( items[i]    );
    	}
    	else if( size > upper ){
    		for( Iterator<E> future =            ret.iterator(); ret.size() > upper && future.hasNext();     ) future.remove();
    	}

    	return ret;
    }

	/** uses a multiple select JList, different mouse handling */
    public class Popup extends BasicComboPopup implements ListSelectionListener{
    	public Popup( JComboBox combo ){
    		super( combo );
    		Dropdown.this.model = list.getSelectionModel();
    	}

    	/** @since 20100110 */
    	public String getUIClassID(){ return UICLASSID_POPP; }

		/** must repaint displayed value whenever the popup dissappears */
		protected void firePopupMenuWillBecomeInvisible(){
			super.firePopupMenuWillBecomeInvisible();
			Dropdown.this.repaint();
			try{
				Dropdown.this.fireActionEvent();
				Dropdown.this.fireItemStateChanged( new ItemEvent( Dropdown.this, ItemEvent.ITEM_STATE_CHANGED, Dropdown.this.getSelectedItem(), ItemEvent.SELECTED ) );
			}catch( Throwable thrown ){
				System.err.println( "warning: Dropdown.Popup.firePopupMenuWillBecomeInvisible() caught " + thrown );
			}
		}

		/** allow multiple selection */
		protected void configureList(){
			super.configureList();
		  /*list.setFont( comboBox.getFont() );
			list.setForeground( comboBox.getForeground() );
			list.setBackground( comboBox.getBackground() );
			list.setSelectionForeground( UIManager.getColor( "ComboBox.selectionForeground" ) );
			list.setSelectionBackground( UIManager.getColor( "ComboBox.selectionBackground" ) );
			list.setBorder( null );
			list.setCellRenderer( comboBox.getRenderer() );
			list.setFocusable( false );
			list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			setListSelection( comboBox.getSelectedIndex() );
			installListListeners();*/
			list.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			list.setSelectionForeground( new Color( UIManager.getColor( "ComboBox.selectionForeground" ).getRGB() ) );
			list.setSelectionBackground( new Color( UIManager.getColor( "ComboBox.selectionBackground" ).getRGB() ) );
		}

		/** disable all listers except for me */
		protected void installListListeners(){
			list.addListSelectionListener( Popup.this );
		}

		/** subclass of JList with behavior disabled */
		protected JList createList(){
			return myList = new JList( comboBox.getModel() ){
				public String       getUIClassID(           ){ return UICLASSID_LIST; }
				public void       clearSelection(           ){}
				public void     setSelectedIndex( int index ){}
				public void ensureIndexIsVisible( int index ){}
			};
		}

		/** interface ListSelectionListener
			enforce bounds */
		public void valueChanged( ListSelectionEvent e ){
			Dropdown.this.listSelectionValueChanged( e.getFirstIndex(), e.getLastIndex() );
		}
    }

	/** like ListSelectionListener.valueChanged() */
    public void listSelectionValueChanged( int first, int last ){
		if( ignore ) return;

		Throwable caught = null;
		try{
			ignore = true;
			ListSelectionModel lsm = myList.getSelectionModel();
			Integer            index;
			for( int i = first; i <= last; i++ ){
				index = new Integer(i);
				myHistory.remove( index );
				if( lsm.isSelectedIndex(i) ) myHistory.addLast( index );
			}
			int removeable = -1;
			while( myHistory.size() > myBoundsInclusive[1] ){
				lsm.removeSelectionInterval( removeable = myHistory.removeFirst(), removeable );
			}
			removeable = 0;
			while( myHistory.size() < myBoundsInclusive[0] ){
				if( ! lsm.isSelectedIndex( removeable ) ){
					lsm.addSelectionInterval( removeable, removeable );
					myHistory.addLast( removeable );
				}
				++removeable;
			}

			tooltip();
			repaint();
		}catch( Exception exception ){ caught = exception;
		}finally{                      ignore = false; }

		if( caught != null ){
			System.err.println( "warning: Dropdown.listSelectionValueChanged() caught " + caught );
			System.err.println( caught.getStackTrace()[0] );
		}
    }

  /*enum Domain{ peter, paul, mary, mike, john, mark }
    public static void main( String[] args ){
    	edu.ucla.belief.ui.util.Util.setLookAndFeel();

    	java.io.PrintStream stream = System.out;
		JComboBox  box   = new JComboBox();
		javax.swing.plaf.ComboBoxUI ui    = box.getUI();
		Class      claZz = ui.getClass();
		stream.println( claZz.getName() );
		stream.println( "interfaces: " + Arrays.toString( claZz.getInterfaces() ) );
		stream.println( "superclass: " +                  claZz.getSuperclass()   );

		stream.println( "ComboPopup: " + ui.getAccessibleChild( box, 0 ).getClass().getName() );
		stream.println();
		stream.println( "methods:" );
		for( java.lang.reflect.Method method : claZz.getMethods() ) stream.println( method.getName() + "()" );

		final Dropdown<Domain> dropdown = new Dropdown<Domain>( Domain.values(), new int[]{ 0, 10 }, " people" );
		Integer[] finite = new Integer[10];
		for( int i=0; i<finite.length; i++ ) finite[i] = new Integer(i);
		JPanel pnl = new JPanel();
		pnl.add( new JLabel( "[" ) );
		final JComboBox lower = new JComboBox( finite );
		pnl.add( lower );
		pnl.add( new JLabel( "," ) );
		final JComboBox upper = new JComboBox( finite );
		pnl.add( upper );
		pnl.add( new JLabel( "]" ) );
		Object[] msg = new Object[]{ dropdown, pnl };
		ActionListener listener = new ActionListener(){
			public void actionPerformed( ActionEvent actionevent ){
				dropdown.myBoundsInclusive = new int[]{ ((Number)lower.getSelectedItem()).intValue(), ((Number)upper.getSelectedItem()).intValue() };
				dropdown.homogenize();
			}
		};
		lower.addActionListener( listener );
		upper.addActionListener( listener );
    	JOptionPane.showMessageDialog( null, msg, Dropdown.class.getName(), JOptionPane.INFORMATION_MESSAGE );
    	stream.println( "selection: " + dropdown.getSelection( EnumSet.noneOf( Domain.class ) ) );
    }*/

	private E[]                      items;
	private String                   plural;
	private JList                    myList;
	private ListSelectionModel       model;
	private int[]                    myBoundsInclusive;
	private boolean                  ignore;
	private LinkedList<Integer>      myHistory = new LinkedList<Integer>();
	private Flash                    runflash;
}
