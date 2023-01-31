package edu.ucla.belief.ui.toolbar;

import edu.ucla.belief.Dynamator;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserModal;
import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.util.DynaRenderer;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.networkdisplay.NetworkDisplay.ZoomListener;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;
import java.beans.*;
import java.net.URL;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**	@author keith cascio
	@since  20040120 */
public class MainToolBar extends JToolBar implements
  ActionListener,
  ItemListener,
  ZoomListener,
  SamiamUserModal
{
	/** @since 20020329 */
	public   static       String      DEV_IMAGE_PATH = "images\\";
	public   static final Insets      INSETS_BUTTONS = new Insets(0,0,0,0);
	public   static final String      STR_QueryModeToolTipText = "Enter Query Mode",
	                                  STR_EditModeToolTipText  = "Enter Edit Mode";
	public   static final boolean     FLAG_ROLLOVER = true;
	public   static       ImageIcon   theBlankIcon;
	private  static       ClassLoader CLASSLOADER;

	private edu.ucla.belief.ui.UI ui;
	private Object mySynchronization       = new Object(),
	               mySynchronizationReplay = new Object();

	public MainToolBar( edu.ucla.belief.ui.UI u ) {
		super( "Main Tool Bar" );
		ui = u;
		init();
		return;
	}

	/** @since 20050214 Valentine's Day! */
	public JButton forAction( Action action ){
		Component[] components = this.getComponents();
		JButton button;
		for( int i=0; i<components.length; i++ ){
			if( components[i] instanceof JButton ){
				button = (JButton) components[i];
				if( button.getAction() == action ) return button;
			}
		}
		return (JButton) null;
	}

	private void init()
	{
		// Add the buttons corresponding to file menu options to the toolbar
		add(  ui.action_NEW  );
		add(  ui.action_OPEN  );
		add(  ui.action_SAVE  );

		addSeparator();

		// Create buttons corresponding to network menu options
		add(  ui.action_COPY  );
		add(  ui.action_CUT  );
		add(  ui.action_PASTE  );
		add(  ui.action_ADDNODE  );
		add(  ui.action_DELETENODES  );
		add(  ui.action_ADDEDGE  );
		add(  ui.action_DELETEEDGE  );

		addSeparator();

		// Add the buttons and boxes corresponding to tools/mode menus
		add(  ui.action_VARIABLESELECTION  );
		add(  ui.action_TOGGLEMODE  );//add( modeButton );
		add(  ui.action_COMPILESETTINGS  );
		add( myAlgorithmBox = makeAlgorithmComponent() );

		addSeparator();
		if( ui.action_RESETANIMATION != null ) add(  ui.action_RESETANIMATION  );
		JComponent compReplay = makeReplayComponent();
		if( compReplay != null ) add( compReplay );
		add(  ui.action_SENSITIVITY  );
		//add(  ui.action_MAP  );
		add(  ui.action_MPE  );
		add(  ui.action_EM  );
		add(  ui.action_IMPACT  );
		//add(  ui.action_CONFLICT  );
		//add(  ui.action_RETRACT  );
		add(  ui.action_SHOWSELECTED );
		add(  ui.action_HIDEALL      );

	  //addSeparator();
		//add(  ui.action_NETWORKDISPLAY  );
		//add(  ui.action_ZOOMIN  );
		//add(  ui.action_ZOOMOUT  );
		add(  ui.action_MAP  );
        add(  ui.action_SDP  );
		add(  ui.action_PRUNE  );
		add(  ui.action_CONSOLE  );
		makeZoomComponent();
		add( myZoomBox );

		addSeparator();
		add(  ui.action_HELPLOCAL  );
		//add(  ui.action_ABOUT  );

		setFloatable(false);
		putClientProperty("JToolBar.isRollover", FLAG_ROLLOVER ? Boolean.TRUE : Boolean.FALSE );

		setEnabled( false );//disable();
		return;
	}

	/** @since 20040121 */
	public void updateUI()
	{
		//System.out.println( "MainToolBar.updateUI()" );
		if( FLAG_ROLLOVER ){
			putClientProperty("JToolBar.isRollover", FLAG_ROLLOVER ? Boolean.TRUE : Boolean.FALSE );
			//setRollover( true );
		}
		super.updateUI();
		if( FLAG_ROLLOVER ){
			ComponentUI cui = getUI();
			//System.out.println( "cui instanceof BasicToolBarUI ? " + (cui instanceof BasicToolBarUI) );
			if( cui instanceof BasicToolBarUI ) ((BasicToolBarUI)cui).setRolloverBorders( true );
		}
	}

	/** @since 20020617 */
	public void setSamiamUserMode( SamiamUserMode newMode )
	{
		synchronized( mySynchronization )
		{

		myZoomBox.setEnabled( newMode.contains( SamiamUserMode.OPENFILE ) );
		//myAlgorithmBox.setEnabled( !newMode.contains( SamiamUserMode.QUERY ) );

		}
	}

	/** @since 20040809 */
	public JComponent makeReplayComponent()
	{
		if( ui.action_INSTANTREPLAY == null ) return (JComponent) null;

		final JButton button = createActionComponent( ui.action_INSTANTREPLAY );
		button.setPressedIcon( getIcon( "RedoAnimationPurple16.gif" ) );
		button.addChangeListener( new ChangeListener()
		{
			public void stateChanged( ChangeEvent e ){
				//System.out.println( "Replay button armed? " + myButtonModel.isArmed() );
				synchronized( mySynchronizationReplay )
				{
					boolean flagArmed = myButtonModel.isArmed();
					if( flagArmed && (!myFlagLastArmed) ){
						//System.out.println( "\t recalculateActuals()" );
						ui.getActiveHuginNetInternalFrame().getNetworkDisplay().recalculateActuals();
					}
					myFlagLastArmed = flagArmed;
				}
			}

			private ButtonModel myButtonModel = button.getModel();
			private boolean myFlagLastArmed = false;
		} );
		return button;
	}

	/** @since 20021118 */
	public void zoomed( double value )
	{
		ZoomItem nearest = null;
		double min = Double.MAX_VALUE;
		double difference;
		for( int i = 0; i < myZoomItems.length; i++ )
		{
			difference = Math.min( Math.abs( (double)1 - (myZoomItems[i].value/value) ), min );
			if( difference < min )
			{
				min = difference;
				nearest = myZoomItems[i];
			}
			else break;
		}
		myFlagIgnoreZoomBox = true;
		myZoomBox.setSelectedItem( nearest );
		myFlagIgnoreZoomBox = false;
	}

	/** @since 20021118 */
	public class ZoomItem
	{
		public ZoomItem( String name, double value )
		{
			this.name = name;
			this.value = value;
		}

		public ZoomItem( String name, Action action )
		{
			this.name = name;
			this.action = action;
		}

		public String name;
		public double value;
		public Action action;

		public String toString() { return name; }

		public void zoom()
		{
			if( action != null ) action.actionPerformed( myActionEvent );
			else ui.setActiveZoomFactor( value );
		}
	}

	protected final ZoomItem
	  ZOOMITEM_10  = new ZoomItem(  "10%", (double)0.1  ),
	  ZOOMITEM_25  = new ZoomItem(  "25%", (double)0.25 ),
	  ZOOMITEM_50  = new ZoomItem(  "50%", (double)0.5  ),
	  ZOOMITEM_75  = new ZoomItem(  "75%", (double)0.75 ),
	  ZOOMITEM_100 = new ZoomItem( "100%", (double)1.0  ),
	  ZOOMITEM_150 = new ZoomItem( "150%", (double)1.5  ),
	  ZOOMITEM_200 = new ZoomItem( "200%", (double)2.0  ),
	  ZOOMITEM_500 = new ZoomItem( "500%", (double)5.0  );
	protected ZoomItem[] myZoomItems = new ZoomItem[]{ ZOOMITEM_10, ZOOMITEM_25, ZOOMITEM_50, ZOOMITEM_75, ZOOMITEM_100, ZOOMITEM_150, ZOOMITEM_200, ZOOMITEM_500 };
	protected ZoomItem   ITEM_ZOOMITEM_FOS, ITEM_ZOOMITEM_IN, ITEM_ZOOMITEM_OUT;
	protected Object[]   ARRAY_ZOOMITEMS;
	protected JComboBox  myZoomBox;
	protected boolean    myFlagIgnoreZoomBox = false;

	/** @since 20050214 Valentine's Day! */
	public JComboBox getZoomBox(){
		return this.myZoomBox;
	}

	/** @since 20021118 */
	public JComboBox makeZoomComponent()
	{
		ITEM_ZOOMITEM_FOS = new ZoomItem(      "fit", ui.action_FITONSCREEN );
		ITEM_ZOOMITEM_IN  = new ZoomItem(  "zoom in", ui.action_ZOOMIN      );
		ITEM_ZOOMITEM_OUT = new ZoomItem( "zoom out", ui.action_ZOOMOUT     );
		ARRAY_ZOOMITEMS   = new Object[]{ ZOOMITEM_10, ZOOMITEM_25, ZOOMITEM_50, ZOOMITEM_75, ZOOMITEM_100, ZOOMITEM_150, ZOOMITEM_200, ZOOMITEM_500, ITEM_ZOOMITEM_FOS, ITEM_ZOOMITEM_IN, ITEM_ZOOMITEM_OUT  };
		myZoomBox = new JComboBox( ARRAY_ZOOMITEMS );
		myZoomBox.setSelectedItem( ZOOMITEM_100 );

		Dimension dim = myZoomBox.getPreferredSize();
		dim.width = 74;
		myZoomBox.setPreferredSize( dim );
		myZoomBox.setMaximumSize( dim );

		myZoomBox.addActionListener( this );

		return myZoomBox;
	}

	protected JComboBox myAlgorithmBox;
	protected Set       myBoxes = new HashSet( 2 );

	/** @since 20030117 */
	public JComboBox makeAlgorithmComponent()
	{
		JComboBox box = new JComboBox( ui.getDynamators().toArray() );
		box.setSelectedItem( ui.getDynamator() );
		box.setEditable(     false );
		box.addItemListener( this  );
		try{
			box.setToolTipText( ui.getDynamator().getDisplayName() );
		}catch( Throwable thrown ){
			System.err.println( "warning: MainToolBar.makeAlgorithmComponent() caught " + thrown );
		}

		box.setRenderer( new DynaRenderer( box.getRenderer() ) );

		Dimension dim = box.getPreferredSize();
		dim.width     = 155;
		box.setPreferredSize( dim );
		box.setMaximumSize(   dim );

		myBoxes.add( box );

		return box;
	}

	/** @since 20030529 */
	public void setDynamator( Dynamator dyn )
	{
		JComboBox box = null;
		for( Iterator it = myBoxes.iterator(); it.hasNext(); ){
			if( (box = (JComboBox) it.next()).getSelectedItem() != dyn ){ box.setSelectedItem( dyn ); }
		}
	}

	protected ActionEvent myActionEvent = new ActionEvent( this, 0, "" );

	/** @since 20021118 */
	public void actionPerformed( ActionEvent evt )
	{
		Object src = evt.getSource();
		if( src == myZoomBox && !myFlagIgnoreZoomBox )
		{
			((ZoomItem)myZoomBox.getSelectedItem()).zoom();
		}
	}

	/** @since 20081022 */
	private Dynamator myDynamatorMRU;

	/** @since 20081022 */
	public void itemStateChanged( ItemEvent e ){
	  //System.out.println( "MainToolBar.itemStateChanged( "+e+" )" );
		if( ! myBoxes.contains( e.getItemSelectable() ) ){ return; }

		if( myAlgorithmBox != null ){
			try{
				myAlgorithmBox.setToolTipText( ((Dynamator)myAlgorithmBox.getSelectedItem()).getDisplayName() );
			}catch( Throwable thrown ){
				System.err.println( "warning: MainToolBar.itemStateChanged() caught " + thrown );
			}
		}

		switch( e.getStateChange() ){
			case ItemEvent.DESELECTED:
				myDynamatorMRU = (Dynamator) e.getItem();
				break;
			case ItemEvent.SELECTED:
				dynamatorSelected( (Dynamator) e.getItem() );
				break;
		}
	}

	/** @since 20081022 */
	private void dynamatorSelected( Dynamator selected ){
		if( ui.getDynamator() == selected ){ return; }

		NetworkInternalFrame nif = ui.getActiveHuginNetInternalFrame();
		try{
			ui.setDynamator( selected );
			myDynamatorMRU = selected;
			if( (nif != null) && (! nif.getSamiamUserMode().contains( SamiamUserMode.EDIT )) ){ ui.action_EDITMODE.actionP( this ); }
		}catch( java.beans.PropertyVetoException pve ){
			if( edu.ucla.belief.ui.util.Util.DEBUG_VERBOSE ){
				System.err.println( "vetoed selection of \""+selected.getDisplayName()+"\"" );
			}
			MainToolBar.this.setDynamator( myDynamatorMRU );
		}catch( Throwable throwable ){
			System.err.println( "warning! MainToolBar.dynamatorSelected() caught: " + throwable );
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				throwable.printStackTrace();
			}
		}
	}

	/** @since 20021022 */
	public static JButton initButton( Action action )
	{
		return initButton( new JButton( action ) );
	}

	/** @since 20100111 */
	public static JButton initButton( JButton button ){
		button.setText( "" );
		button.setMargin( INSETS_BUTTONS );
		return button;
	}

	/** @since 20100111 */
	//@Override
	protected JButton createActionComponent( final Action a ){
	  //System.out.println( "createActionComponent("+a.getValue( Action.NAME )+")" );
		final JButton butt = initButton( super.createActionComponent( a ) );
		if( (a != null) && (a.getValue( SamiamAction.KEY_EPHEMERAL ) == Boolean.TRUE) ){
			a.addPropertyChangeListener( new PropertyChangeListener(){
				public void propertyChange(  PropertyChangeEvent evt ){
				  //System.out.println( "propertyChange( "+evt.getPropertyName()+" )" );
					if( evt.getPropertyName().intern() == "enabled" ){ butt.setVisible( a.isEnabled() ); }
				}
			} );
		}
		return butt;
	}

	/** @since 20021022 */
	public static ImageIcon getBlankIcon()
	{
		if( theBlankIcon == null ){
			URL blankURL = findImageURL( "blank16.gif" );
			theBlankIcon = (blankURL == null ) ? null : new ImageIcon( blankURL );
		}
		return theBlankIcon;
	}

	/** @since 20050323 */
	public static ImageIcon getIcon( String fname )
	{
		if( theMapFilenamesToIcons == null ) theMapFilenamesToIcons = new HashMap();
		if( theMapFilenamesToIcons.containsKey( fname ) ) return (ImageIcon) theMapFilenamesToIcons.get( fname );
		else{
			ImageIcon ret = createIcon( fname );
			theMapFilenamesToIcons.put( fname, ret );
			return ret;
		}
	}
	private static Map theMapFilenamesToIcons;

	/** @since 20021022 */
	public static ImageIcon createIcon( String fname )
	{
		//System.out.println( "MainToolBar.getIcon( "+fname+" )" );
		URL iconURL = findImageURL( fname );
		if( iconURL != null) return new ImageIcon( iconURL  );
		else
		{
			System.err.println( "Warning: Could not find icon named: " + fname + "." );
			return null;
		}
	}

	/** @since 20020618 */
	public static URL findImageURL( String iconName )
	{
		//System.out.println( "MainToolBar.findImageURL( "+iconName+" )" );
		if( CLASSLOADER == null ){
			CLASSLOADER = MainToolBar.class.getClassLoader();
			//System.out.println( "MainToolBar loaded by " + CLASSLOADER );
		}
		//URL iconURL = ClassLoader.getSystemResource( "images/" + iconName );
		URL iconURL = CLASSLOADER.getResource( "images/" + iconName );

		//Keith Cascio
		//032902
		//For the purposes of development, i.e. when
		//not working with the jar version, try to find
		//the files in an absolute path.
		if( iconURL == null )
		{
			File f = new File( DEV_IMAGE_PATH + iconName );
			if( f.exists() )
			{
				try{
					iconURL = f.toURL();
				} catch( java.net.MalformedURLException e ){
					iconURL = null;
				}
			}
		}

		return iconURL;
	}
}
