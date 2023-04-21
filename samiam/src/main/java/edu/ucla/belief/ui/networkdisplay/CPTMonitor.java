package edu.ucla.belief.ui.networkdisplay;

import        edu.ucla.belief.ui.tabledisplay.*;
import        edu.ucla.belief.ui.tabledisplay. HuginGenieStyleTableFactory.TableModelHGS;
import        edu.ucla.belief.ui.tabledisplay. HuginGenieStyleTableFactory.HuginGenieStyleJComponentWrapper;
import        edu.ucla.belief.ui.displayable.*;
import        edu.ucla.belief.ui.event.*;
import        edu.ucla.belief.ui.statusbar. TrimmedBorder;
import        edu.ucla.belief.ui.actionsandmodes.*;
import static edu.ucla.belief.ui.actionsandmodes. SamiamAction.SAMIAMACTION_SELECTED_KEY_PUBLIC;
import        edu.ucla.belief.ui.toolbar.*;
import        edu.ucla.belief.ui.util. Util;
import static edu.ucla.belief.ui.util. Util .copyToSystemClipboard;
import        edu.ucla.belief.ui.preference. SamiamPreferences;
import        edu.ucla.belief.ui.actionsandmodes. Grepable.Redirect;
import        edu.ucla.belief.ui.actionsandmodes. Grepable.Redirect.Redirectable;
import        edu.ucla.belief.ui.actionsandmodes. Grepable.Redirect.Context;
import static edu.ucla.belief.ui.actionsandmodes. Grepable.Redirect.Context.*;
import        edu.ucla.belief.ui.actionsandmodes. Grepable.Redirect.Dest;
import static edu.ucla.belief.ui.actionsandmodes. Grepable.Redirect.Dest.*;
import static edu.ucla.belief.ui.dialogs. EnumModels.Actionable.Property.smallicon;

import        edu.ucla.belief.*;
import        edu.ucla.belief.approx. EdgeDeletionInferenceEngine;

import        java.awt.*;
import        java.awt.event.*;
import        javax.swing.*;
import        javax.swing.event.*;
import        javax.swing.table.*;
import static javax.swing.SwingUtilities.isDescendingFrom;
import static javax.swing.SwingUtilities.convertPointToScreen;
import static javax.swing.SwingUtilities.isLeftMouseButton;
import        javax.swing.border.*;
import static java.lang.Thread.yield;
import static java.lang.Thread.sleep;
import        java.util.BitSet;
import        java.util.Map;
import        java.util.EnumMap;
import static java.util.EnumSet.range;
import static java.awt.event.KeyEvent.*;
import static java.awt.event.InputEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;

/** @author keith cascio
	@since  20080228 */
public class      CPTMonitor
       extends    MouseAdapter
       implements   CPTChangeListener,
                    HierarchyListener,
                     AWTEventListener,
                        MouseListener,
                  MouseMotionListener,
                    ComponentListener,
                          StainWright,
                         Redirectable,
           NodePropertyChangeListener,
                       ChangeListener
{
	public static final Toolkit
	  TOOLKIT = Toolkit.getDefaultToolkit();

	public static final Color
	  COLOR_TOPBAR_NORMAL    =     Color.white,
	  COLOR_TOPBAR_MOUSEOVER = new Color( 0x99, 0xcc, 0x66 );

	public static final String
	  STR_TIP_SLIDER_COLUMNS = "<html>adjust the number of cpt <font color='#0000cc'><b>columns</b></font> displayed",
	  STR_TIP_SLIDER_WIDTH   = "<html>adjust the <font color='#0000cc'><b>width</b></font> of this cpt monitor",
	  STR_TIP_SLIDER_HEIGHT  = "<html>adjust the <font color='#0000cc'><b>height</b></font> of this cpt monitor",
	  STR_EXCITED            = "<html><b>right-click for menu",
	  STR_CHILL              = "";

	public static final char
	  CHAR_SEPARATOR         = '\t';

	public CPTMonitor( DisplayableFiniteVariableImpl variable ){
		this.variable = variable;
		init();
	}

	private void init(){
		this.data     = this.variable.getCPTShell().getCPT().dataclone();
		this.recent   = new BitSet( this.data.length );

		try{
			this.color_observed = (Color) variable.getNetworkInternalFrame().getPackageOptions().getMappedPreference( SamiamPreferences.nodeBorderClrObserved ).getValue();
		}catch( Throwable thrown ){
			Util.warn( thrown, "CPTMonitor()" );
		}

		redirect2action = Redirect.actions( (Redirectable) CPTMonitor.this, range( Redirect.txt2file, Redirect.txt2dialog ) );
	}

	public void cptChanged( CPTChangeEvent event ){
		if( event.var != this.variable ){ return; }
		Table table  =   this.variable.getCPTShell().getCPT();
		int  length  =  table.getCPLength();
		if(  length !=   this.data.length ){ throw new IllegalStateException(); }

		double neu;
		for( int i=0; i<length; i++ ){
			if( (neu = table.getCP( i )) == this.data[i] ){ this.recent.set( i, false ); }
			else{
				this.handler.setValueAt( neu, i );
				this.recent.set( i, true );
			}
		}

		try{
			editor.getTempJTable().repaint();
		}catch( Throwable thrown ){
			System.err.println( "warning: CPTMonitor.cptChanged() caught " + thrown );
		}
	}

	/** interface NodePropertyChangeListener
		@since 20090331 */
	public void nodePropertyChanged( NodePropertyChangeEvent event ){
		if( (event != null) && (event.variable != this.variable) ){ return; }

		if( component != null ){ component.setVisible( false ); }
	}

	/** @since 20080307 */
	public   static   final  Stain
	  STAIN___NORMAL   = new Stain(  "normal",     Color.white,               Color.black,  "normal" ),
	  STAIN__CHANGED   = new Stain( "changed", new Color( 0xff, 0xcc, 0xff ), Color.black, "changed" );

	/** @since 20080307 */
	public   static   final  Stain[] STAINS = new Stain[]{ STAIN___NORMAL, STAIN__CHANGED };

	public Color    getBackground( int row, int column ){ return getStain( row, column ).getBackground(); }
	public Stain         getStain( int row, int column ){
		int index = model.calculateDataIndex( row, column );
		return recent.get( index ) ? STAIN__CHANGED : STAIN___NORMAL;
	}
	public void             stain( int row, int column, Component comp ){
		Stain stain = getStain( row, column );
		if( stain != null ){ stain.stain( comp ); }
	}
	public Stain[]      getStains(){ return STAINS; }
	public JComponent getSwitches(){ return null; }
	public boolean      isEnabled(){ return true; }
	public void        setEnabled( boolean flag ){}
	public void       addListener( ActionListener listener ){}
	public boolean removeListener( ActionListener listener ){ return false; }

	public Point setLocation( Point point ){
		asJComponent().setLocation( point );
		return point;
	}

	public boolean visible( JComponent comp ){
		return comp == null ? false : comp.isVisible();
	}

	public JComponent asJComponent(){
		if( this.component != null ){ return this.component; }

		this.editor                                 =   new CPTEditor( variable, null );
		editor.setEditable( false );
		DefaultTableCellRenderer         renderer   =   new ProbabilityJTableRenderer();
		ProbabilityDataHandler           handler    =   new ProbabilityDataHandler( this.data, renderer, new DefaultTableCellRenderer(), null ).setMinimize( true );
		handler.setStainWright( (StainWright) this );
		JComponent                       ret        =   innards = editor.makeCPTDisplayComponent( handler );
		this.model                                  =             editor.getTableModelHGS();
		this.handler                                =   handler;

		scrollbar                                   =   editor.getScrollBar();
		boolean                          sbv_before =   visible( scrollbar );

		NetworkDisplay networkdisplay = variable.getNetworkInternalFrame().getNetworkDisplay();
		Dimension      bounds         = networkdisplay.getSize();
		Insets         insets         = networkdisplay.getInsets();
		bounds  .width               -= (insets.left +  insets.right + 0x4);
		bounds .height               -= (insets .top + insets.bottom + 0x4);
		Dimension      pref           = ret.getPreferredSize();
		pref.setSize( Math.min( pref.width, bounds.width ), Math.min( pref.height, bounds.height ) );
		ret .setPreferredSize(  pref );
		JTable         jtable         = editor.getTempJTable();
		TableStyleEditor.resize( jtable, pref.width );

		jtable   .setRowSelectionAllowed( false );
		jtable.setColumnSelectionAllowed( false );
		jtable  .setCellSelectionEnabled( false );

		boolean                          sbv_after  =   visible( scrollbar );
		if( sbv_after && (! sbv_before) ){
			pref.height += scrollbar.getPreferredSize().height;
			ret .setPreferredSize(  pref );
		}
		accomodated = sbv_before || sbv_after;

		this.component = new JPanel( new BorderLayout() );
		this.component.add(             ret, BorderLayout.CENTER );
		this.component.add( refreshTopbar(), BorderLayout.NORTH  );

		variable.getNetworkInternalFrame().addCPTChangeListener( this );
		variable.getNetworkInternalFrame().addNodePropertyChangeListener( this );

		this.component.addHierarchyListener( this );

		for( Redirect redirect : redirect2action.keySet() ){
			if( redirect.stroke != null ){
				jtable.registerKeyboardAction( redirect2action.get( redirect ), redirect.name(), redirect.stroke, JComponent.WHEN_FOCUSED );
			}
		}

	  /*if( ! this.constituent.stained ) return this.component = ret;

		Stainer                          stainer    =   new Stainer( rewriteresult, get( Constituent.before ).data, get( Constituent.after ).data, editor.getTableModelHGS() );
		handler.setStainWright( stainer );
		if( this.constituent == Constituent.after ) switches = stainer.getSwitches();

		this.to_repaint = editor.getTempJTable();
		stainer.addListener( Preview.this );

		stainer.setEnabled( white( Constituent.statistics ) );*/

		return this.component;
	}

	public JComponent refreshTopbar(){
		if( topbar == null ){
			topbar  = new JPanel(   new BorderLayout() );
			topbar.setMinimumSize(  new Dimension( 0x8, 0x8 ) );
			topbar.add( Box.createHorizontalStrut( 0x4 ), BorderLayout.WEST   );
			topbar.add(      caption = new JLabel(     ), BorderLayout.CENTER );
			topbar.add(      excited = new JLabel(     ), BorderLayout.EAST   );

			Border  border  = component.getBorder();
			if(     border == null ){
				if( editor != null && editor.getWrapper() != null && editor.getWrapper().component != null ){
					border  =         editor.getWrapper().component.getBorder();
				}
			}
			if(     border == null ){ border = BorderFactory.createLineBorder( Color.black, 1 ); }

			topbar.setBorder( new TrimmedBorder( (AbstractBorder) border, 0, 1, 0, 0 ) );

			topbar.setVisible(            false );
			topbar.addMouseListener(       this );
			topbar.addMouseMotionListener( this );
			topbar.addComponentListener(   this );
		}

		caption      .setText( variable.toString() );
		topbar .setBackground( COLOR_TOPBAR_NORMAL );

		return topbar;
	}

	public void  componentHidden( ComponentEvent e ){}
	public void   componentMoved( ComponentEvent e ){}
	public void componentResized( ComponentEvent e ){}
	public void   componentShown( ComponentEvent e ){
		refreshTopbar();
	}

	static public boolean mousePointerInside( Component comp ){
		if( comp == null ){ return false; }
		Point upperleft = new Point();
		convertPointToScreen( upperleft, comp );
		Rectangle rect = new Rectangle( upperleft, comp.getSize() );
		boolean incide = rect.contains( MouseInfo.getPointerInfo().getLocation() );
	  //System.out.println( "incide "+rect+"? " + incide );
		return  incide;
	}

	/** interface HierarchyListener */
	public void hierarchyChanged( HierarchyEvent hierarchyevent ){
		if( (hierarchyevent.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) > 0 ){
			boolean showing = component.isShowing();
		  //System.out.println( "hierarchyChanged( SHOWING_CHANGED ) -> " + showing );
			if( showing ){
				setTopbarVisible( mousePointerInside( this.component ) );
				runAWT();
			}else{
				TOOLKIT.removeAWTEventListener( CPTMonitor.this );
				die();
			}
		}
	}

	private void runAWT(){
		Runnable runnable = new Runnable(){
			public void run(){
//				yield();
				boolean sh = CPTMonitor.this.component.isShowing();
				if( sh ){ TOOLKIT   .addAWTEventListener( CPTMonitor.this, AWTEvent.MOUSE_EVENT_MASK ); }
				else{      }
			}
		};
		new Thread( runnable ).start();
	}

	/** interface AWTEventListener */
	public void eventDispatched( AWTEvent event ){
		MouseEvent me;
		switch( event.getID() ){
			case MouseEvent.MOUSE_ENTERED:
				me = (MouseEvent) event;
				boolean   hit = hit( me );
				if( autohide ){ setTopbarVisible( hit ); }
				setTopbarExcited( hit && (hit( me, topbar ) || hit( me, popup ) || hit( me, resize )) );
			  //if( hit && (! this.component.hasFocus()) ){ this.component.requestFocusInWindow(); }
				break;
			case MouseEvent.MOUSE_RELEASED:
				me = (MouseEvent) event;
				if( ((me.getButton() & MouseEvent.BUTTON2) > 0) && hit( me, innards ) ){ this.component.setVisible( false ); }
				break;
		}
	}

	public boolean hit( MouseEvent me, Component comp ){
		if(          comp == null ){ return false; }
		Component      co = me.getComponent();
		return        (co == comp) || (isDescendingFrom( co, comp ));
	}

	public boolean hit( MouseEvent me ){
		Component        co = me.getComponent();
		if( (co == this.component) || (co == this.topbar) || (co == this.popup) || (co == this.sliderw) || (co == this.sliderh) ){ return true; }
		else return
		  ((this.component != null) && isDescendingFrom( co, this.component )) ||
		  ((this.topbar    != null) && isDescendingFrom( co, this.topbar    )) ||
		  ((this.popup     != null) && isDescendingFrom( co, this.popup     )) ||
		  ((this.resize    != null) && isDescendingFrom( co, this.resize    ));
	}

	public boolean setTopbarExcited( boolean excited ){
		boolean observed = this.variable.getObservedValue() != null;
		this.topbar.setBackground( excited ? COLOR_TOPBAR_MOUSEOVER : (observed ? color_observed : COLOR_TOPBAR_NORMAL) );
		this.excited.setText(      excited ? STR_EXCITED            :                              STR_CHILL            );
		return excited;
	}

	public boolean setTopbarVisible( boolean visible ){
	  //System.out.println( "setTopbarVisible( "+visible+" ), comp vis? " + this.component.isVisible() );
		boolean already  = topbar.isVisible();
		if(     already == visible ){ return false; }
		Dimension   size = component.getSize();
		Point       loc  = component.getLocation();
		int       adjust = topbar.getPreferredSize().height;
		if( ! visible ){ adjust = - adjust; }
		size.height     += adjust;
		loc .y          -= adjust;
		topbar    .setVisible( visible );
		component    .setSize(    size );
		component.setLocation(     loc );
		return true;
	}

	public void  mousePressed( MouseEvent event ){
		if( isLeftMouseButton( event ) ){ dragstart = event.getPoint(); }
		else{ showPopup( event ); }
	}
	public void  mouseEntered( MouseEvent event ){}
	public void   mouseExited( MouseEvent event ){}
	public void  mouseDragged( MouseEvent event ){
		if( dragstart == null ){ return; }
		Point loc = component.getLocation();
		loc.translate( event.getX() - dragstart.x, event.getY() - dragstart.y );
		component.setLocation( loc );
	}
	public void mouseReleased( MouseEvent event ){
		dragstart = null;
		setTopbarExcited( mousePointerInside( topbar ) );
		showPopup( event );
	}
	public void  mouseClicked( MouseEvent event ){
		showPopup( event );
	}
	public void    mouseMoved( MouseEvent event ){}

	public void stateChanged( ChangeEvent event ){
		if( ignore ){ return; }
		try{
			Object src = event.getSource();

			if( src == slider ){
				boolean     sbv_before = visible( scrollbar );
				ported.setBreadth(      slider.getValue() );
//				yield();
				boolean     sbv_after  = visible( scrollbar );
				if( sbv_after && (! sbv_before) && (! accomodated) ){
					accomodated = true;
				  /*Component  comp    = editor.getWrapper().component;
					Dimension  size    = comp.getSize();
					Dimension  pref    = comp.getPreferredSize();
					if( size.height    < pref.height ){*/
						Dimension bige = this.component.getSize();
						bige.height   += scrollbar.getPreferredSize().height;
						this.component.setSize( bige );
				  //}
				}
			}
			else if( src == sliderw ){
				int        val  = sliderw.getValue();
				Dimension  dim  = this.component.getSize();
				if(  dim.width != val ){
					int delta   = dim.width - val;
					if( delta % 2 != 0 ){ return; }
					 dim.width  = val;
					Point loca  = this.component.getLocation();
					loca.x     += delta >> 1;
					this.component.setLocation( loca );
					this.component.setSize( dim );
					this.component.revalidate();
				}
			}
			else if( src == sliderh ){
				int        val  = sliderh.getValue();
				Dimension  dim  = this.component.getSize();
				if( dim.height != val ){
					int delta   = dim.height - val;
					if( delta % 2 != 0 ){ return; }
					dim.height  = val;
					Point loca  = this.component.getLocation();
					loca.y     += delta >> 1;
					this.component.setLocation( loca );
					this.component.setSize( dim );
					this.component.revalidate();
				}
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: CPTMonitor.stateChanged() caught " + thrown );
		}
	}

	public JPopupMenu refreshPopup(){
		if( popup == null ){
			popup = new JPopupMenu();
			JMenuItem item = popup.add( new JCheckBoxMenuItem( action_AUTOHIDE ) );

			JMenu        mwrite = new JMenu( "write parameters to..." );
			mwrite.setIcon( MainToolBar.getIcon( "Save16.gif" ) );
			SamiamAction action;
			for( Redirect redirect : redirect2action.keySet() ){
				if( (action = redirect2action.get( redirect )) != null ){ mwrite.add( action ); }
			}
			popup.add( mwrite );

			if( model instanceof PortedTableModel ){
				ported = (PortedTableModel) model;
				slider = new JSlider( 1, ported.getBreadthCeiling(), ported.getBreadth() );
				slider.setSnapToTicks( true );
				slider.setMinorTickSpacing( 1 );
				slider.setMajorTickSpacing( 0 );
				slider.setPaintTicks(  true );
				slider.setBackground( popup.getBackground() );
				Dimension dim = slider.getPreferredSize();
				dim.width = item.getPreferredSize().width;
				slider.setPreferredSize( dim );
				slider.setToolTipText( STR_TIP_SLIDER_COLUMNS );
				popup.add( slider );
				slider.addChangeListener( this );
			}

			sliderw = new JSlider( 1, 2, 1 );
			sliderw.setToolTipText( STR_TIP_SLIDER_WIDTH );
			sliderw.setSnapToTicks( true );
			sliderw.setMinorTickSpacing( 2 );
			sliderw.setMajorTickSpacing( 0 );
			sliderw.setPaintTicks(  true );
			sliderw.setBackground( popup.getBackground() );
			sliderw.addChangeListener( this );

			sliderh = new JSlider( 1, 2, 1 );
			sliderh.setToolTipText( STR_TIP_SLIDER_HEIGHT );
			sliderh.setSnapToTicks( true );
			sliderh.setMinorTickSpacing( 2 );
			sliderh.setMajorTickSpacing( 0 );
			sliderh.setPaintTicks(  true );
			sliderh.setBackground( popup.getBackground() );
			sliderh.addChangeListener( this );

			JPanel             pnl = new JPanel( new GridBagLayout() );
			GridBagConstraints   c = new GridBagConstraints();
			pnl.setBackground( popup.getBackground() );

			c.anchor    = GridBagConstraints.EAST;
			c.insets    = new Insets( 0, 2, 0, 2 );
			c.gridwidth = 1;
			pnl.add( new JLabel(  "width:", JLabel.RIGHT ), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			pnl.add( sliderw, c );

			c.gridwidth = 1;
			pnl.add( new JLabel( "height:", JLabel.RIGHT ), c );
			c.gridwidth = GridBagConstraints.REMAINDER;
			pnl.add( sliderh, c );

			JMenu menuResize = new JMenu( "resize..." );
			menuResize.setIcon( MainToolBar.getIcon( "Resize16.gif" ) );
			menuResize.add( pnl );
			popup.add( menuResize );
			resize = (JComponent) pnl.getParent();

			popup.add( action_TOFRONT );
			popup.add( action_DISPOSE );
		}

		try{
			ignore = true;

			int width  = this.component.getWidth();
			sliderw.setMinimum(  width >> 1 );
			sliderw.setMaximum(  width << 1 );
			sliderw.setMinorTickSpacing( sliderw.getMaximum() >> 5 );
			sliderw.setValue(    width      );

			int height = this.component.getHeight();
			sliderh.setMinimum( height >> 1 );
			sliderh.setMaximum( height << 1 );
			sliderh.setMinorTickSpacing( sliderh.getMaximum() >> 5 );
			sliderh.setValue(   height      );
		}finally{
			ignore = false;
		}

		return popup;
	}

	public boolean showPopup( MouseEvent e ){
		if( e.isPopupTrigger() ){
			refreshPopup();

			Point p = e.getPoint();
			convertPointToScreen( p, e.getComponent() );
			popup.setLocation( p );
			popup.setInvoker( topbar );
			popup.setVisible( true );
			return true;
		}
		return false;
	}

	/** interface Redirectable
		@since 20080309 */
	public String describeContent( Redirect redirect ){
		return "visible parameters";
	}

	/** interface Redirectable
		@since 20080309 */
	public Dest redirect( Redirect redirect ){
		Map<Context,Object> context = new EnumMap<Context,Object>( Context.class );
		context.put( nif, variable.getNetworkInternalFrame() );
		context.put( filename, EdgeDeletionInferenceEngine.DateFormatFilename.getInstance().now() + "_" + variable.getID() + "_cpt.tdv" );
		context.put( fileextension,                                                                                               "tdv" );
		return this.append( redirect.open( context ) ).flush();
	}

	/** @since 20080308 */
	public <T extends Dest> T append( T app ){
		if( model == null ){ return app; }
		int rows = model.    getRowCount();
		int cols = model. getColumnCount();
		for( int row = 0; row < rows; row++ ){
			for( int col = 0; col < cols; col++ ){
				app.app( model.getValueAt( row, col ).toString() ).app( CHAR_SEPARATOR );
			}
			app.nl();
		}
		return app;
	}

	/** @since 20080308 */
	public <T extends Appendable> T append( T app ) throws java.io.IOException{
		if( model == null ){ return app; }
		int rows = model.    getRowCount();
		int cols = model. getColumnCount();
		for( int row = 0; row < rows; row++ ){
			for( int col = 0; col < cols; col++ ){
				app.append( model.getValueAt( row, col ).toString() ).append( CHAR_SEPARATOR );
			}
			app.append( '\n' );
		}
		return app;
	}

	/** @since 20080308 */
	public String copy(){
		if( model == null ){ return null; }
		try{
			return copyToSystemClipboard( this.append( new StringBuilder( 0x10 * model.getRowCount() * model.getColumnCount() ) ).toString() );
		}catch( Throwable thrown ){
			Util.warn( thrown, "CPTMonitor.copy()" );
			return null;
		}
	}

	final public SamiamAction  action_TOFRONT = new SamiamAction( "to front", "show this cpt monitor on top of everything else", 'f', MainToolBar.getIcon( "CPTCopy16.gif" ) ){
		public void actionPerformed( ActionEvent event ){
			component.getParent().setComponentZOrder( component, 0 );
		}
	};
	final public SamiamAction  action_DISPOSE = new SamiamAction( "close monitor", "<html><font color='#990000'>destroy</font> this cpt monitor (also by <b>right-clicking</b> anywhere in the cpt monitor)", 'c', MainToolBar.getIcon( "Delete16.gif" ) ){
		public void actionPerformed( ActionEvent event ){
			component.setVisible( false );
		}
	};
	final public SamiamAction action_AUTOHIDE = new SamiamAction( "auto-hide top bar", "hide top bar unless you mouse over this cpt monitor", 'a', MainToolBar.getIcon( "AutoHide16.gif" ) ){
		{ putValueProtected( SAMIAMACTION_SELECTED_KEY_PUBLIC, Boolean.TRUE ); }
		public void actionPerformed( ActionEvent event ){
			Boolean toggle = Boolean.TRUE.equals( getValue( key ) ) ? Boolean.FALSE : Boolean.TRUE;
			putValueProtected( key, toggle );
			autohide = toggle.booleanValue();
			if( ! autohide ){ setTopbarVisible( true ); }
		}
		final public String key = SAMIAMACTION_SELECTED_KEY_PUBLIC;
	};

	public CPTMonitor die(){
		if( variable  != null ){  variable.getNetworkInternalFrame().removeCPTChangeListener( this );
			NodeLabel nl = variable.getNodeLabel();
			if( nl != null ){
				synchronized( nl.getCPTMonitorSynch() ){
					if( nl.getCPTMonitor() == this.component ){ nl.setCPTMonitor( null ); }
				}
			}
		}
		if( component != null ){ component.removeHierarchyListener( this ); }
		if( topbar    != null ){
			topbar.removeMouseListener(       this );
			topbar.removeMouseMotionListener( this );
			topbar.removeComponentListener(   this );
		}
		if( slider    != null ){
			slider.removeChangeListener(      this );
		}
		variable           = null;
		data               = null;
		editor             = null;
		model              = null;
		handler            = null;
		component          = topbar = innards = resize = null;
		caption            = excited = null;
		dragstart          = null;
		popup              = null;
		slider             = sliderw = sliderh = null;
		scrollbar          = null;
		recent             = null;
		color_observed     = null;

		return this;
	}

	private    DisplayableFiniteVariableImpl  variable;
	private    double[]                       data;
	private    CPTEditor                      editor;
	private    TableModelHGS                  model;
	private    PortedTableModel               ported;
	private    ProbabilityDataHandler         handler;
	private    JComponent                     component, topbar, innards, resize;
	private    JLabel                         caption, excited;
	private    Point                          dragstart;
	private    boolean                        autohide = true, accomodated = false, ignore = false;
	private    JPopupMenu                     popup;
	private    JSlider                        slider, sliderw, sliderh;
	private    JScrollBar                     scrollbar;
	private    BitSet                         recent;
	private    Color                          color_observed;
	private    Map<Redirect,SamiamAction>     redirect2action;// = new EnumMap<Redirect,SamiamAction>( Redirect.class );
}
