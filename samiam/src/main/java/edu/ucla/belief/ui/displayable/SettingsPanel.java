package edu.ucla.belief.ui.displayable;

import        edu.ucla.util.Setting;
import        edu.ucla.util.Setting.Settings;
import        edu.ucla.util.SettingsImpl;
import        edu.ucla.belief. CrouchingTiger .DynamatorImpl;
import        edu.ucla.belief.ui.internalframes. CrouchingTiger .DisplayableDynamatorImpl;
import        edu.ucla.belief.*;
import        edu.ucla.belief. Dynamator .Commitable;
import        edu.ucla.belief.approx. ApproxEngineGenerator;
import        edu.ucla.util. PropertyKey;
import static edu.ucla.util. PropertyKey.*;

import        edu.ucla.belief.ui. NetworkInternalFrame;
import static edu.ucla.belief.ui.preference. SamiamPreferences .inferenceUnifyCompileSettings;

import        edu.ucla.belief.ui.util. ResizeAdapter;
import        edu.ucla.belief.ui.util. Util;
import static edu.ucla.belief.ui.util. Util .DEBUG;

import        java.awt.*;
import        java.awt.event.*;
import        javax.swing.*;
import        javax.swing.event.*;
import        java.util.*;

/** based on PropagationSettingsPanel

	@author keith cascio
	@since  20080225 */
public class SettingsPanel<E extends Enum<E> & Setting> extends JPanel implements
  ActionListener,
  Dynamator.Commitable,
  HierarchyListener,
  ChangeListener,
//MenuListener,
  AWTEventListener
{
	private String
	  STR_TITLE,
	  STR_PREFIX_SUBALG;

	public SettingsPanel( Class<E> clazz, String title ){
		SETTING2EDITOR    = setting2editor( this.clazz = clazz );
		STR_TITLE         = "<html><b>" + title;
		this.subalgorithm = Enum.valueOf( clazz, "subalgorithm" );
		STR_PREFIX_SUBALG = "<html>"+this.subalgorithm.get( caption ).toString().toLowerCase()+": <b>";
		init();
	}

	/** interface HierarchyListener */
	public void hierarchyChanged( HierarchyEvent hierarchyevent ){
		if(    (hierarchyevent.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) > 0 ){
		  //System.out.println( "hierarchyChanged( SHOWING_CHANGED ) -> " + this.isShowing() );
			if( this.isShowing() ){
				keylisteners = new LinkedList<KeyListener>();
				for( Component comp : myPanelMain.getComponents() ){ if( comp instanceof KeyListener ){ keylisteners.add( (KeyListener) comp ); } }
			  //System.out.println( "keylisteners.isEmpty()? " + keylisteners.isEmpty() );
				if( keylisteners.isEmpty() ){ keylisteners = null; }
				else{ Toolkit.getDefaultToolkit().   addAWTEventListener( this, AWTEvent.KEY_EVENT_MASK ); }

				Window window = SwingUtilities.getWindowAncestor( SettingsPanel.this );
				if( window instanceof JDialog ){ setParentWindow( window ); }
			}else if( keylisteners != null ){
				{     Toolkit.getDefaultToolkit().removeAWTEventListener( this ); }
				keylisteners = null;
				setParentWindow( null );
			}
		}
	}

	/** interface AWTEventListener */
	public void eventDispatched( AWTEvent event ){
		if( event.getID() != KeyEvent.KEY_PRESSED ){ return; }
		if( keylisteners  == null                 ){ return; }
		if( event instanceof KeyEvent ){
			KeyEvent ke = (KeyEvent) event;
			for( KeyListener kl : keylisteners ){
				if( ke.isConsumed() ){ break; }
				kl.keyPressed( ke );
			}
		}
	}

	/** interface Dynamator.Commitable */
	public void commitChanges(){
		if( (myActualSettings != null) && (myVirtualSettings != null) ){
			JComponent component;
			for( E setting : clazz.getEnumConstants() ){
				if( (component = setting2component.get( setting )) == null ){ continue; }
				myVirtualSettings.put( setting, SETTING2EDITOR.get( setting ).read( component ) );
			}
			myActualSettings.copy(         myVirtualSettings );
		}

		try{
			if( dynamator2commitable != null ){
				for( Commitable commitable : dynamator2commitable.values() ){ commitable.commitChanges(); }
			}
		}catch( Throwable thrown ){
			System.err.append( "warning: SettingsPanel.commitChanges() caught " )
			.append( thrown.toString() )
			.append( "\n" );
		}
	}

	/** interface Dynamator.Commitable */
	public JComponent asJComponent(){
		return this;
	}

	/** interface Dynamator.Commitable */
	public void copyToSystemClipboard(){
		StringBuilder buffer = new StringBuilder( 0x100 );

		buffer.append( "Compile settings - " ).append( myDynamator.getDisplayName() ).append( '\n' );

		JComponent component;
		for( E setting : clazz.getEnumConstants() ){
			if( (component = setting2component.get( setting )) == null ){ continue; }
			buffer.append( setting.get( caption ) ).append( ": " );
			buffer.append( SETTING2EDITOR.get( setting ).read( component ).toString() ).append( '\n' );
		}

		Util.copyToSystemClipboard( buffer.toString() );
	  //return buffer;
	}

	@SuppressWarnings( "unchecked" )
	public SettingsPanel setNetworkInternalFrame( NetworkInternalFrame hnInternalFrame, ApproxEngineGenerator<E> peg )
	{
		if( hnInternalFrame != myNetworkInternalFrame ){
			myNetworkInternalFrame       = hnInternalFrame;
			set( myNetworkInternalFrame.getBeliefNetwork() );
			myDynamator = peg;
		}

		set( (Settings<E>) peg.retrieveState( peg.choosePropertySuperintendent( myBeliefNetwork ) ) );

		try{
			myFlagUnifyCompileSettings = ((Boolean) hnInternalFrame.getPackageOptions().getMappedPreference( inferenceUnifyCompileSettings ).getValue()).booleanValue();
		}catch( Throwable thrown ){
			System.err.append( "warning: SettingsPanel.setNetworkInternalFrame() caught " )
			.append( thrown.toString() )
			.append( " while reading preferences \n" );
		}

		try{
			if( myTabbed            != null ){ myTabbed.setSelectedIndex( 0 );  }
			if( myPanelSubalgorithm != null ){ myPanelSubalgorithm.removeAll(); }
		}catch( Throwable thrown ){
			System.err.append( "warning: SettingsPanel.setNetworkInternalFrame() caught " )
			.append( thrown.toString() )
			.append( " while resetting tabs \n" );
		}

		return this;
	}

	public DisplayableBeliefNetwork set( DisplayableBeliefNetwork bn ){
		return myBeliefNetwork = bn;
	}

	protected Settings<E> set( Settings<E> actual )
	{
		myVirtualSettings = new SettingsImpl<E>( clazz ).copy( myActualSettings  = actual );

		JComponent component;
		Editor     ed;
		JLabel     lbl;
		String     strInfo;
		for( E setting : clazz.getEnumConstants() ){
			if( (component = setting2component.get( setting )) == null ){ continue; }
			(ed = SETTING2EDITOR.get( setting )).write( component, myVirtualSettings.get( setting ) );
			ed.snap( component, myVirtualSettings.snapshot( setting ) );
			try{
				lbl = setting2caption.get( setting );
				lbl.setText( (String) myVirtualSettings.get( setting, caption ) );
				strInfo = (String) myVirtualSettings.get( setting, info );
				lbl.setToolTipText( strInfo );
				setting2tip.get( setting ).setText( strInfo );
			}catch( Throwable thrown ){
				System.err.println( "warning: SettingsPanel.set( Settings<E> ) caught " + thrown );
			}
		}

		return actual;
	}

	public void setParentWindow( Window parent ){
	  //System.out.println( "SettingsPanel.setParentWindow( "+(parent == null ? "null" : parent.getClass().getSimpleName())+" )" );
		myParent = parent;
	}

	protected void init()
	{
		E[]         settings = clazz.getEnumConstants();
		E           last     = settings[ settings.length - 1 ];

		component2setting    = new HashMap<JComponent,E>( settings.length );
		setting2component    = new EnumMap<E,JComponent>( clazz );
		setting2caption      = new EnumMap<E,JLabel    >( clazz );
		setting2tip          = new EnumMap<E,JLabel    >( clazz );

		myPanelMain          = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.anchor             = GridBagConstraints.SOUTHWEST;

		JComponent comp;
		JLabel     lbl;
		Map<PropertyKey,Object> enummap;
		String     strinfo;
		boolean    simple    = ! DEBUG;
		for( E setting : clazz.getEnumConstants() ){
			if( Boolean.TRUE.equals( setting.get( advanced ) ) && simple ){ continue; }

			enummap = new EnumMap<PropertyKey,Object>( setting.properties() );
			enummap.put( actionlistener, this );
			comp = SETTING2EDITOR.get( setting ).component( enummap );

			component2setting.put( comp, setting );
			setting2component.put( setting, comp );

			c.gridwidth      = 1;
			myPanelMain.add( lbl = new JLabel( "<html><nobr><b>"+setting.get( caption )+":" ), c );
			setting2caption.put( setting, lbl );
			strinfo = (String) setting.get( info );
			if( (strinfo != null) && (strinfo.length() > 0) ){ lbl.setToolTipText( strinfo ); }
			myPanelMain.add( Box.createHorizontalStrut( 0x10 ), c );
			c.fill           = GridBagConstraints.HORIZONTAL;
			myPanelMain.add( comp, c );
			c.fill           = GridBagConstraints.NONE;
			myPanelMain.add( Box.createHorizontalStrut( 0x10 ), c );
			myPanelMain.add( lbl = new JLabel( strinfo ), c );
			setting2tip.put( setting, lbl );
			c.gridwidth      = GridBagConstraints.REMAINDER;
			myPanelMain.add(     Box.createHorizontalGlue(), c );

			if( setting     != last ){ myPanelMain.add( Box.createVerticalStrut( 8 ), c ); }
		}

		myResizePanel        = this;

		myTabbed             = new JTabbedPane();
		myTabbed.addTab( STR_TITLE,         myPanelMain );
		myTabbed.addTab( STR_PREFIX_SUBALG, myPanelSubalgorithm = new JPanel( new GridLayout( 1,1 ) ) );
		myTabbed.addChangeListener( SettingsPanel.this );

		myResizePanel.add( myTabbed );
		myResizeAdapter      = new ResizeAdapter( myTabbed );
		myResizePanel.addComponentListener( myResizeAdapter );
		recalculatePreferredSize();
		recalculateTabTitle();

		this.addHierarchyListener( this );

	  /*if( FLAG_DEBUG_BORDERS ){ myResizePanel.setBorder( BorderFactory.createLineBorder( Color .blue, 1 ) );
		                            myPanelMain.setBorder( BorderFactory.createLineBorder( Color  .red, 1 ) ); }*/
	}

	/** @since 20081030 */
	private Dimension recalculatePreferredSize(){
		if( myTabbed == null || myResizePanel == null ){ return null; }

		Dimension minSize        = null;
		try{
			minSize              = myTabbed.getPreferredSize();

		  //Component       head = myTabbed.getTabComponentAt( 1 );
		  //Dimension    dimHead = head == null ? myTabbed.getBoundsAt( 1 ).getSize() : head.getSize();
			Dimension    dimHead = myTabbed.getBoundsAt( 1 ).getSize();

			Dimension        dim = myPanelMain.getPreferredSize();
			dim.width           += 0x40;
			dim.height          += 0x20;
			minSize.width        = Math.max( minSize.width,  dim.width  );
			minSize.height       = Math.max( minSize.height, dim.height + dimHead.height );

			dim                  = myPanelSubalgorithm.getPreferredSize();
			minSize.width        = Math.max( minSize.width,  dim.width  );
			minSize.height       = Math.max( minSize.height, dim.height + dimHead.height );

			myResizePanel.setMinimumSize(   minSize );
			myResizePanel.setPreferredSize( minSize );
		}catch( Throwable thrown ){
			System.err.append( "warning: SettingsPanel.recalculatePreferredSize() caught " )
			.append( thrown.toString() )
			.append( "\n" );
		}
		return minSize;
	}

	/** interface ChangeListener
		@since 20081030 */
	public void	stateChanged( ChangeEvent changeevent ){
		if(         changeevent.getSource() != myTabbed            ){ return; }
		if( myTabbed.getSelectedComponent() != myPanelSubalgorithm ){ return; }

		DynamatorImpl    dynamatorimpl = (DynamatorImpl) myVirtualSettings.get( this.subalgorithm );
		DisplayableDynamatorImpl   ddi = DisplayableDynamatorImpl.forDynamatorImpl( dynamatorimpl );
		Dynamator                  dyn = ddi.enlist( myNetworkInternalFrame.getParentFrame(), myDynamator.team( myBeliefNetwork ) );
		if( ! myFlagUnifyCompileSettings ){ dyn.fixPropertySuperintendent( myDynamator.getSubalgorithmPropertySuperintendent( myBeliefNetwork ) ); }
		Commitable          commitable = dynamator2commitable.get( dyn );
		if( commitable == null ){ dynamator2commitable.put( dyn, commitable = dyn.getEditComponent( null ) ); }
		JComponent                comp = commitable.asJComponent();

		if( ! myPanelSubalgorithm.isAncestorOf( comp ) ){
			myPanelSubalgorithm.removeAll();
			myPanelSubalgorithm.add( comp );
			recalculatePreferredSize();
			if( myParent != null ){ myParent.pack(); }
		}
	}

	/** @since 20081030 */
	private String recalculateTabTitle(){
		StringBuilder buff = new StringBuilder();
		buff.append( STR_PREFIX_SUBALG );
		buff.append( myVirtualSettings == null ? "????" : myVirtualSettings.get( this.subalgorithm ).toString() );
		String ret = buff.toString();
		if( myTabbed != null ){ myTabbed.setTitleAt( 1, ret ); }
		return ret;
	}

	public void actionPerformed( ActionEvent e ){
		JComponent src     = (JComponent) e.getSource();
		E          setting = component2setting.get( src );
		myVirtualSettings.put( setting, SETTING2EDITOR.get( setting ).read( src ) );

		try{
			if( setting == this.subalgorithm ){ recalculateTabTitle(); }
		}catch( Throwable thrown ){
			System.err.append( "warning: SettingsPanel.actionPerformed() caught " )
			.append( thrown.toString() )
			.append( "\n" );
		}
	}

	public static <E extends Enum<E> & Setting> Map<E,Editor> setting2editor( Class<E> clazz ){
		Map<E,Editor> s2e = null;
		if( s2e == null ){
			s2e = new EnumMap<E,Editor>( clazz );
			for( E setting : clazz.getEnumConstants() ){
				s2e.put( setting, Editor.forTarget( setting.get( defaultValue ) ) );
			}
		}
		return s2e;
	}

	private       Map<JComponent,E>                        component2setting;
	private       Map<E,JComponent>                        setting2component;
	private       Map<E,JLabel>                            setting2caption;
	private       Map<E,JLabel>                            setting2tip;
	private       Map<E,Editor>                            SETTING2EDITOR;// = setting2editor();
	private       Collection<KeyListener>                  keylisteners;
	private       Map<Dynamator,Commitable>                dynamator2commitable = new HashMap<Dynamator,Commitable>( DisplayableDynamatorImpl.il2Partials().size() );
	private       boolean                                  myFlagUnifyCompileSettings;

	protected     JTabbedPane                              myTabbed;
	protected     JPanel                                   myPanelMain, myPanelInner, myPanelSubalgorithm;
	protected     JComponent                               myResizePanel;
	protected     ComponentListener                        myResizeAdapter;
	private       Window                                   myParent;

	protected                           E                  subalgorithm;
	protected                     Class<E>                 clazz;
	protected                  Settings<E>                 myActualSettings, myVirtualSettings;
	protected     ApproxEngineGenerator<E>                 myDynamator;
	protected     NetworkInternalFrame                     myNetworkInternalFrame;
	protected     DisplayableBeliefNetwork                 myBeliefNetwork;
}
