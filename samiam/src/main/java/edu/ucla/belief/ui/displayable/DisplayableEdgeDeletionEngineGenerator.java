package edu.ucla.belief.ui.displayable;

import        edu.ucla.belief.*;
import        edu.ucla.belief. Dynamator.Decorator;
import        edu.ucla.belief.approx.*;
import        edu.ucla.belief.approx. Macros.Category;
import        edu.ucla.belief.approx. Macros.Recoverable;
import        edu.ucla.belief.approx. Macros.Recoverables;
import        edu.ucla.belief.approx. EdgeDeletionInferenceEngine.Attribute;
import static edu.ucla.belief.approx. EdgeDeletionInferenceEngine.Attribute.*;
import        edu.ucla.belief.approx. EdgeDeletionInferenceEngine.Command;
import        edu.ucla.belief.approx. EdgeDeletionInferenceEngine.CPTPolicy;
import        edu.ucla.belief.approx. EdgeDeletionInferenceEngine.CPTPolicy.Listener;
import        edu.ucla.belief.approx. EdgeDeletionBeliefPropagationSetting;
import static edu.ucla.belief.approx. EdgeDeletionBeliefPropagationSetting.*;
import        edu.ucla.belief.io. PropertySuperintendent;
import static edu.ucla.belief.io. PropertySuperintendent .*;

import        edu.ucla.belief.ui. UI;
import        edu.ucla.belief.ui. NetworkInternalFrame;
import        edu.ucla.belief.ui.toolbar. MainToolBar;
import        edu.ucla.belief.ui.dialogs. VisibilityAdapter;
import        edu.ucla.belief.ui.dialogs. VisibilityAdapter.Packable;
import static edu.ucla.belief.ui.dialogs. EnumModels.Actionable.Property.smallicon;
import static edu.ucla.belief.ui.dialogs. EnumModels.View.menubox;
import static edu.ucla.belief.ui.dialogs. EnumModels.View.menuitem;
import        edu.ucla.belief.ui.actionsandmodes. SamiamAction;
import        edu.ucla.belief.ui.util .Util;
import static edu.ucla.belief.ui.util .Util.warn;
import        edu.ucla.belief.ui.networkdisplay. NetworkDisplay;
import        edu.ucla.belief.ui.networkdisplay. NodeLabel;
import        edu.ucla.belief.ui.networkdisplay. Monitor;
import        edu.ucla.belief.ui.actionsandmodes. Grepable;
import        edu.ucla.belief.ui.actionsandmodes. Grepable.Redirect;
import        edu.ucla.belief.ui.actionsandmodes. Grepable.Redirect.Redirectable;
import        edu.ucla.belief.ui.actionsandmodes. Grepable.Redirect.Context;
import        edu.ucla.belief.ui.actionsandmodes. Grepable.Redirect.Dest;
import        edu.ucla.belief.ui.internalframes .Bridge2Tiger;
import        edu.ucla.belief.ui.internalframes .Bridge2Tiger.Troll;
import        edu.ucla.belief.ui.internalframes .CrouchingTiger;
import        edu.ucla.belief.ui.event. DynamatorListener;

import        javax.swing.*;
import        java.awt.*;
import        java.awt.event.*;
import        java.util.*;
import static java.util.EnumSet.range;
import static java.awt.event.KeyEvent.*;
import static java.awt.event.InputEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;
import        java.beans.*;

/** based on DisplayablePropagationEngineGenerator

	@author keith cascio
	@since  20080225 */
public class DisplayableEdgeDeletionEngineGenerator extends DisplayableApproxEngineGenerator<EdgeDeletionBeliefPropagationSetting> implements
  Decorator,
  DynamatorListener,
  VetoableChangeListener
{
	public String getTitle(){ return "manual edge-deletion belief-propagation"; }

	public DisplayableEdgeDeletionEngineGenerator( EdgeDeletionEngineGenerator peg, UI ui ){
		super( peg, ui );
		peg.addDecorator( this );
		ui.addDynamatorListener( this );
	}

	/** @since 20091119 */
	public Dynamator dibs( BeliefNetwork bn, PropertySuperintendent ps ){
		Map<Object,Object>   properties = Macros.properties( ps );
		Recoverables       recoverables = (Recoverables) properties.get( KEY_RECOVERABLES );
		if(      recoverables ==                                          null ){ return null; }
		if( recoverables.isEmpty()                                             ){ return null; }
		return this;
	}

	/** @since 20081022 */
	public void vetoableChange( PropertyChangeEvent pce ) throws PropertyVetoException{
		Dynamator oldValue     = (Dynamator) pce.getOldValue();
		Dynamator newValue     = (Dynamator) pce.getNewValue();
		if(       newValue    == (DisplayableEdgeDeletionEngineGenerator) this ){ return; }
		if(       oldValue    != (DisplayableEdgeDeletionEngineGenerator) this ){ return; }
		if( pce.getSource()   !=                                          myUI ){ return; }
		NetworkInternalFrame        nif = myUI.getActiveHuginNetInternalFrame();
		if(               nif ==                                          null ){ return; }
		DisplayableBeliefNetwork     bn = nif.getBeliefNetwork();
		if(                bn ==                                          null ){ return; }
		Map<Object,Object>   properties = Macros.properties( (PropertySuperintendent) bn );
		Recoverables       recoverables = (Recoverables) properties.get( KEY_RECOVERABLES );
		if(      recoverables ==                                          null ){ return; }
		if( recoverables.isEmpty()                                             ){ return; }

		int                     size = recoverables.size();
		Recoverable[]          array = recoverables.asArray();
		String        oldDisplayName = oldValue == null ? "nothing" : oldValue.getDisplayName();
		String        newDisplayName = newValue == null ?    "null" : newValue.getDisplayName();
		StringBuilder           buff = new StringBuilder( 0x100 );
		buff.append( "<html>" )
		.append( "You chose to switch from the \"<b>" )
		.append( oldDisplayName )
		.append( "</b>\" algorithm to the \"<b>" )
		.append( newDisplayName )
		.append( "</b>\" algorithm, \n<html> but you already replaced (\"deleted\") " );

		try{
			switch( size ){
				case  1: buff.append( "an edge" ); break;
				default: buff.append( Integer.toString( size ) ).append( " edges" ); break;
			}
			buff.append( ". \n<html> If you want to recover " );
			if( size == 1 ){
				buff.append( "the edge between " );
				array[0].append( bn, buff );
			}else if( size == 2 ){
				buff.append( "both edges" );
			}else{
				buff.append( "all " ).append( Integer.toString( size ) ).append( " edges" );
			}
			buff.append( ", click \"<b>OK</b>\". \n<html> To cancel and revert back to \"<b>" )
			.append( oldDisplayName )
			.append( "</b>\", click \"<b>Cancel</b>\"." );
		}catch( Throwable thrown ){
			throw (PropertyVetoException) new PropertyVetoException( "creating message", pce ).initCause( thrown );
		}

		int result = JOptionPane.showConfirmDialog( myUI, buff.toString(), "Confirm Edge Recovery", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );
		if( result != JOptionPane.OK_OPTION ){
			throw new PropertyVetoException( "user cancelled", pce );
		}

		try{
		  //for( Recoverable recoverable : array ){ recoverable.recover( bn ); }
			FiniteVariable[][] edges = new FiniteVariable[ size ][];
			for( int i=0; i<size; i++ ){ edges[i] = array[i].asArray( bn ); }
			((CrouchingTiger) Bridge2Tiger.Troll.solicit()).doRecoverEdges( bn, edges, nif );
		}catch( Throwable thrown ){
			throw (PropertyVetoException) new PropertyVetoException( "failed to recover all deleted (\"replaced\") edges", pce ).initCause( thrown );
		}

	  //success
	}

	/** @since 20080227 */
	public DisplayableEdgeDeletionEngineGenerator decorate( final InferenceEngine ie ){
		final EdgeDeletionInferenceEngine           edie         = (EdgeDeletionInferenceEngine) ie.canonical();
		final Settings<EdgeDeletionBeliefPropagationSetting> edbpsettings = myApproxEngineGenerator.getSettings( myApproxEngineGenerator.choosePropertySuperintendent( (PropertySuperintendent) edie.getBeliefNetwork() ) );
		if( edie.getFlagControl() ){
			ie.setControlPanel( new ControlPanel( edie, edie.getBeliefNetwork(), myUI ).asJComponent() );
		}
		edie.setListener( new Listener(){
			public Object cptsChanged( Collection<FiniteVariable> variables, int parameters ){
				int    count   = 0;
				String message = null, title = "edge deletion belief propagation";
				int    type    = JOptionPane.INFORMATION_MESSAGE;
				try{
					for( FiniteVariable var : variables ){
						nif.setCPT( var );
						++count;
					}
					if( this.notify ){
						message = "<html>copied <b>"+parameters+"</b> parameters for <b>" + count + "</b> auxiliary variables";
						try{
							if( count <= 0x20 ){
								if( builder == null ){ builder = new StringBuilder( count * 0x20 ); }
								else{ builder.setLength(0); }
								builder.append( message ).append( ": \n<html>" );
								FiniteVariable[] array = variables.toArray( new FiniteVariable[ count ] );
								Arrays.sort( array, comparator() );
								int i = 1;
								for( FiniteVariable var : array ){
									builder.append( "<b>" ).append( i<10 ? "0" : "" ).append( Integer.toString( i++ ) ).append( "</b>. " )
										   .append( var.toString() ).append( " (" ).append( var.getID() ).append( ")" )
										   .append( "\n<html>" );
								}
								message = builder.toString();
							}
						}catch( Throwable thrown ){
							System.err.println( "warning: DisplayableEdgeDeletionEngineGenerator.Listener caught " + thrown );
						}
					}
				}catch( Throwable thrown ){
					message      = thrown.getMessage();
					if( message == null ){ message = thrown.toString(); }
					message      = "<html><font color='#cc0000'><b>error</b></font>: " + message;
					type         = JOptionPane.ERROR_MESSAGE;
					title       += ": error";
				}
				if( this.notify || (type != JOptionPane.INFORMATION_MESSAGE) ){ JOptionPane.showMessageDialog( nif, message, title, type ); }
				return null;
			}
			private NetworkInternalFrame nif = ((DisplayableBeliefNetwork) edie.getBeliefNetwork()).getNetworkInternalFrame();
			private StringBuilder        builder;
			private boolean              notify = ((Boolean) edbpsettings.get( cptnotification )).booleanValue();
		} );
		return this;
	}

	/** @since 20080227 */
	public static class ControlPanel /*extends JPanel*/ implements EvidenceChangeListener, ActionListener, /*MouseListener,*/ HierarchyListener, AWTEventListener, Redirectable{
		public   static final Insets
		  INSETS_BUTTONS = new Insets( 1,4,1,4 );
		private  static       Font
		  FONT_STATIC, FONT_ACTIVE;
		public   static final KeyStroke
		  STROKE_MONITORS = getKeyStroke( VK_M, 0 );
		public   static final Set<Attribute>
		  ATTRIBUTES_MINIMAL = Collections.unmodifiableSet( EnumSet   .of( Attribute.iterations, residual, converged ) ),
		  ATTRIBUTES_ALL     = Collections.unmodifiableSet( EnumSet.allOf( Attribute.class                 ) );

		public ControlPanel( EdgeDeletionInferenceEngine ie, BeliefNetwork bn, UI ui ){
		  //super( new GridBagLayout() );

			if( (FONT_STATIC == null) || (FONT_ACTIVE == null) ){
				FONT_STATIC   = new JLabel().getFont();
				FONT_ACTIVE   = FONT_STATIC.deriveFont( Font.BOLD );
			}

			this .beliefnetwork = bn;
			this            .ie = ie;
			this            .ui = ui;
			this            .init();
			bn.getEvidenceController().addEvidenceChangeListener( this );
			ie                             .addIterationListener( this );

			Settings<EdgeDeletionBeliefPropagationSetting> settings = ie.settings();
			action_rotate.setEnabled( Boolean.TRUE.equals( settings.get( compare2exact ) ) );
		}

		public JComponent asJComponent(){
			boolean already = false;
			for( HierarchyListener hl : panelmain.getHierarchyListeners() ){
				if( hl == this ){ already = true; break; }
			}
			if( ! already ){ panelmain.addHierarchyListener( this ); }
			return panelmain;
		}

		private ControlPanel init(){
			GridBagConstraints c = new GridBagConstraints();

			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor    = GridBagConstraints.WEST;
			panelmain.add( buttons(), c );

			LinkedList<Component> comps = new LinkedList<Component>();
			JLabel                label;
			Component             comp;
			VisibilityAdapter     va;
			for( Attribute attribute : Attribute.values() ){
				comps.clear();
				c.gridwidth = 1;
				c.anchor    = GridBagConstraints.EAST;
				panelmain.add( comp = label = new JLabel( attribute.display  + ":" ), c );
				label.setToolTipText( attribute.description );
				comps.add( comp );
				panelmain.add( comp = Box.createHorizontalStrut( 0x10 ), c );
				comps.add( comp );
				c.anchor    = GridBagConstraints.WEST;
				c.weightx   = 1;
				panelmain.add( comp = label = new JLabel( "?" ), c );
				comps.add( comp );
				c.weightx   = 0;
				attr2label.put( attribute, label );
				c.gridwidth = GridBagConstraints.REMAINDER;
				panelmain.add( comp = Box.createGlue(), c );
				comps.add( comp );

				va = new VisibilityAdapter( comps.toArray( new Component[ comps.size() ] ), attribute.display, "show", "hide", VisibilityAdapter.LONG_PACK_DELAY );
				attr2comps.put( attribute, va );
			}

			SamiamAction action;
			String       tip;
			for( Category category : Category.values() ){
				final Category kat = category;
				tip    = "select all " + category.display;
				if( category.extra != null ){ tip += " (" + category.extra + ")"; }
				action = new SamiamAction( category.display, tip, '\0', null ){
					public void actionPerformed( ActionEvent event ){
						select( kat.membership( ie.bridge(), null ) );
					}
				};
				action.setAccelerator( category.stroke );
				panelmain.registerKeyboardAction( action, category.stroke, JComponent.WHEN_IN_FOCUSED_WINDOW );
				catme2action.put( category, action );

				tip    = "select everything but the " + category.display;
				if( category.extra != null ){ tip += " (" + category.extra + ")"; }
				action = new SamiamAction( "<html><b>!</b> " + category.display, tip, '\0', null ){
					public void actionPerformed( ActionEvent event ){
						select( kat.complement( ie.bridge(), null ) );
					}
				};
				action.setAccelerator( category.backstroke );
				panelmain.registerKeyboardAction( action, category.backstroke, JComponent.WHEN_IN_FOCUSED_WINDOW );
				catco2action.put( category, action );
			}

			if( ie.logging() ){
				rd2actn_log = Redirect.actions( new Redirectable(){
					public Object        redirect( Redirect redirect ){ try{ writeLog( redirect ); }catch( Exception e ){ warn( e, "DisplayableEdgeDeletionEngineGenerator.ControlPanel.Redirectable(log).redirect()" ); } return null; }
					public String describeContent( Redirect redirect ){ return "tab delimited log"; }
				}, range( Redirect.txt2file, Redirect.txt2dialog ) );

				KeyStroke stroke = getKeyStroke( VK_L, 0 );
				action           = rd2actn_log.get( Redirect.txt2file );
				action.setAccelerator( stroke );
				action.setToolTipText( "<html><b>keyboard \u201c" + Editor.toString( stroke ) + "\u201d</b> - " + action.getValue( Action.SHORT_DESCRIPTION ) );
				if( panelmain != null ){ panelmain.registerKeyboardAction( action, Redirect.txt2file.name(), stroke, JComponent.WHEN_IN_FOCUSED_WINDOW ); }
			}

			rd2actn_stats = Redirect.actions( (Redirectable) ControlPanel.this, range( Redirect.txt2file, Redirect.txt2dialog ) );
			for( Redirect redirect : rd2actn_stats.keySet() ){
				if( redirect.stroke != null ){
					panelmain.registerKeyboardAction( rd2actn_stats.get( redirect ), redirect.name(), redirect.stroke, JComponent.WHEN_IN_FOCUSED_WINDOW );
				}
			}

			if( (this.ui != null) && (panelmain != null) ){
				panelmain.registerKeyboardAction( this.ui.action_SHOWSELECTED, STROKE_MONITORS, JComponent.WHEN_IN_FOCUSED_WINDOW );
			}

			panelmain.setBorder( BorderFactory.createEmptyBorder( 2,4,2,4 ) );

			action_minimal.actionP( this );

			return this.refresh();
		}

		public ControlPanel setBound( Action[] actions, boolean bound ){
			if( panelmain == null ){ return this; }
			KeyStroke stroke;
			for( Action action : actions ){
				stroke = (KeyStroke) action.getValue( Action.ACCELERATOR_KEY );
				if( stroke == null ){ continue; }
				if( bound && action.isEnabled() ){ panelmain  .registerKeyboardAction( action, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW ); }
				else{                              panelmain.unregisterKeyboardAction(         stroke ); }
			}
			return this;
		}

		/** interface HierarchyListener */
		public void hierarchyChanged( HierarchyEvent hierarchyevent )
		{
			if( (hierarchyevent.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) > 0 )
			{
				boolean showing = panelmain.isShowing();
			  //System.out.println( "hierarchyChanged( SHOWING_CHANGED ) -> " + showing );
				if( showing ){
					Packable          packable  = new Packable( panelmain );
					this.root                   = packable.asContainer();
					Toolkit.getDefaultToolkit().   addAWTEventListener( this, AWTEvent.MOUSE_EVENT_MASK );
				}else{
					this.root                   = null;
					Toolkit.getDefaultToolkit().removeAWTEventListener( this );
				}
				setBound( actions, showing );
			}
		}

		/** interface AWTEventListener */
		public void eventDispatched( AWTEvent event ){
			if( event instanceof MouseEvent ){
				MouseEvent me = (MouseEvent) event;
				if( ! me.isPopupTrigger() ){ return; }
				Component  co = me.getComponent();
			  //if( me.getID() == MouseEvent.MOUSE_RELEASED ){ System.out.println( co.getClass().getSimpleName() ); }
				if( ! SwingUtilities.isDescendingFrom( co, root ) ){ return; }
			  /*if( co instanceof JLabel ){
					copyable     = ((JLabel) co).getText();
					action_copy.setName( "copy \"" +copyable+ "\"" );
				}*/
				if( showPopup( me ) ){ me.consume(); }
			}
		}

	  /*public void  mouseClicked( MouseEvent event ){ if( ! showPopup( event ) ){ refresh(); } }
		public void  mousePressed( MouseEvent event ){ showPopup( event ); }
		public void mouseReleased( MouseEvent event ){ showPopup( event ); }
		public void  mouseEntered( MouseEvent event ){}
		public void   mouseExited( MouseEvent event ){}*/

		private JPanel buttons(){
			JPanel             pnl      = new JPanel( new GridBagLayout() );
			GridBagConstraints c        = new GridBagConstraints();
			Command[]          commands = Command.values();
			Command            last     = commands[ commands.length - 1 ];

			c.insets                    = new Insets( 0,2,0,2 );

			JButton      button;
			ImageIcon    icon;
			SamiamAction action;
			String       tip;
			for( Command command : commands ){
				if( command == Command.importcpts && (ie.settings().get( EdgeDeletionBeliefPropagationSetting.cptpolicy) != CPTPolicy.manual) ){ continue; }
				tip    = "<html><b>keyboard \u201c" + Editor.toString( command.stroke ) + "\u201d</b> - " + command.description;
				final Command finality = command;
				action = new SamiamAction( command.display, tip, command.name().charAt(0), MainToolBar.getIcon( command.imagefilename ) ){
					public void actionPerformed( ActionEvent event ){
						ie.command( finality );
					}
				};
				action.setAccelerator( command.stroke );
				panelmain.registerKeyboardAction( action, command.stroke, JComponent.WHEN_IN_FOCUSED_WINDOW );
				command2action.put( command, action );
				if( command == Command.importcpts ){ continue; }

				button = new JButton( action );
				button.setText( null );
			  //button.addActionListener( this );
			  //button.setToolTipText( tip );
				button.setMargin( INSETS_BUTTONS );
				pnl.add( button, c );
			  /*button2command.put( button, command );
				command2button.put( command, button );*/
			}
			c.gridwidth = GridBagConstraints.REMAINDER;
			pnl.add( Box.createGlue(), c );

			pnl.setBorder( BorderFactory.createEmptyBorder( 0,0,8,0 ) );

			return pnl;
		}

		public boolean showPopup( MouseEvent e ){
			if( e.isPopupTrigger() ){
				if( popup == null ){ popup = makePopup(); }

				Point p = e.getPoint();
				SwingUtilities.convertPointToScreen( p, e.getComponent() );
				popup.setLocation( p );
				popup.setInvoker( panelmain );
				popup.setVisible( true );
				return true;
			}
			return false;
		}

		private JPopupMenu makePopup(){
			JPopupMenu popup = new JPopupMenu();
			popup.add( action_refresh );
		  //popup.add( action_copy    );
			JMenu      smenu = new JMenu( "write statistics to..." );
			smenu.setIcon( MainToolBar.getIcon( "Save16.gif" ) );
			for( Redirect redirect : rd2actn_stats.keySet() ){
				smenu.add(           rd2actn_stats.get( redirect ) );
			}
			popup.add( smenu );

			JMenu mselect = new JMenu( "select" );
			mselect.setIcon( MainToolBar.getIcon( "Display16.gif" ) );
			for( Category category : Category.values() ){
				mselect.add( catme2action.get( category ) );
				mselect.add( catco2action.get( category ) );
			}
			mselect.add( action_none   );
			popup.add( mselect );
			if( this.ui != null ){
				JMenuItem item = popup.add( this.ui.action_SHOWSELECTED );
				item.setText( "probability monitors" );//item.setText( item.getText().toLowerCase() );
				item.setAccelerator( STROKE_MONITORS );
			}
			if( action_cptmons.isEnabled() ){ popup.add( action_cptmons ); }
			if( action_rotate .isEnabled() ){ popup.add( action_rotate  ); }
		  /*if( this.ui != null ){
				JMenu menu = new JMenu( "show..." );
				menu.add( this.ui.action_SHOWEDGES );
				menu.add( this.ui.action_SHOWRECOVERABLES );
				popup.add( menu );
			}*/
			if( ie.logging() ){
				JMenu     menu = new JMenu( "write log to..." );
				menu.setIcon( MainToolBar.getIcon( "Save16.gif" ) );
				SamiamAction saction;
				for( Redirect redirect : rd2actn_log.keySet() ){
					menu.add( saction = rd2actn_log.get( redirect ) );
					if( redirect != Redirect.txt2file ){ saction.setAccelerator( null ); }
				}
				popup.add( menu );
			}
			JMenu miterations = new JMenu( "iterations..." );
			miterations.setIcon( MainToolBar.getIcon( "StepForward16.gif" ) );
			Action action, action_importcpts = null;
			for( Command command : Command.values() ){
				if( (action = command2action.get( command )) == null ){ continue; }
				if( command == Command.importcpts ){ action_importcpts = action; continue; }
				miterations.add( action );
			}
			popup.add( miterations );
			if( action_importcpts != null ){ popup.add( action_importcpts ); }
			popup.addSeparator();
			popup.add( action_minimal );
			popup.add( action_maximal );
		  //popup.addSeparator();
			JMenu ismenu = new JMenu( "individual statistics..." );
			ismenu.setIcon( MainToolBar.getIcon( "Zoom16.gif" ) );
			VisibilityAdapter va;
			for( Attribute attribute : Attribute.values() ){
				va = attr2comps.get( attribute );
				ismenu.add( menubox.button( va, va, attribute ) );
			}
			popup.add( ismenu );
			return popup;
		}

		public void actionPerformed( ActionEvent event ){
			try{
				Object     source = event.getSource();
			  /*Command   command = button2command.get( source );
				if(  command != null ){ ie.command( command ); return; }*/
			  /*Redirect redirect = buttn2redirect.get( source );
				if( redirect == null ){
					if( Redirect.txt2file.name().equals( event.getActionCommand() ) ){ redirect = Redirect.txt2file; }
				}
				if( redirect != null ){  writeLog( redirect ); return; }*/
			}catch( Throwable thrown ){
				System.err.println( "warning: DisplayableEdgeDeletionEngineGenerator.ControlPanel.actionPerformed() caught " + thrown );
			}
		}

		/** @since 20080228 */
		public ControlPanel writeLog( Redirect redirect ) throws java.io.IOException{
			Map<Context,Object> context = new EnumMap<Context,Object>( Context.class );
			context.put( Context.nif, ((DisplayableBeliefNetwork) ie.getBeliefNetwork()).getNetworkInternalFrame() );
			context.put( Context.filename, EdgeDeletionInferenceEngine.DateFormatFilename.getInstance().now() + "_edbp_log.tdv" );
			context.put( Context.fileextension, "tdv" );
			Dest dest = redirect.open( context );
			ie.appendLog( dest, '\t' );
			dest.flush();
			return this;
		}

		public ControlPanel showOnly( Collection<Attribute> attributes ){
			VisibilityAdapter va;
			for( Attribute attr : Attribute.values() ){
				va = attr2comps.get( attr );
				if( attributes.contains( attr ) ){ va.show(); }
				else{                              va.hide(); }
			}
			return this;
		}

		private Attribute refresh( Attribute attribute ){
			JLabel label     = attr2label.get( attribute );
			if( ! label.isVisible() ){ return attribute; }
			String previous  = label.getText();
			String next      = attribute.get( ie ).toString();
			label.setText( next );
			label.setFont( ((next != null) && (! next.equals( previous )) ) ? FONT_ACTIVE : FONT_STATIC );
			return attribute;
		}

		private ControlPanel refresh(){
			if( ! survive() ){ return this; }
			Action action;
			for( Command command : Command.values() ){
				if( (action = command2action.get( command )) == null ){ continue; }
				action.setEnabled( command.enabled( ie ) );
			}
			JLabel label;
			String previous, next;
			for( Attribute attribute : Attribute.values() ){ refresh( attribute ); }
			try{
				Dimension pref   = panelmain.getPreferredSize();
				Container parent = panelmain.getParent();
				if( parent != null ){
					Dimension size   = parent.getSize();
					if( pref.width > size.width ){ pack(); }
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: DisplayableEdgeDeletionEngineGenerator.ControlPanel.refresh() caught " + thrown );
			}
			return this;
		}

		/** @since 20080309 */
		public boolean survive(){
			if( (this.ie == null) || (! this.ie.getValid()) ){
				this.die();
				return false;
			}
			return true;
		}

		private Container pack() throws Exception{
			return new Packable( panelmain ).pack();
		}

		public void         warning( EvidenceChangeEvent ece ){}

		public void evidenceChanged( EvidenceChangeEvent ece ){
			refresh();
		}

		/** interface Redirectable
			@since 20080309 */
		public String describeContent( Redirect redirect ){ return "statistics"; }

		/** interface Redirectable
			@since 20080309 */
		public Dest redirect( Redirect redirect ){
			Map<Context,Object> context = new EnumMap<Context,Object>( Context.class );
			context.put( Context.nif, ((DisplayableBeliefNetwork) ie.getBeliefNetwork()).getNetworkInternalFrame() );
			context.put( Context.filename, EdgeDeletionInferenceEngine.DateFormatFilename.getInstance().now() + "_edbp_stats.txt" );
			context.put( Context.fileextension, "txt" );
			return this.append( redirect.open( context ) ).flush();
		}

		public <T extends Appendable> T append( T app ){
			try{
				for( Attribute attr : Attribute.values() ){
					if( attr2label.get( attr ).isVisible() ){
						app
						.append( attr.display )
						.append( ": " )
						.append( attr.get( ie ).toString() )
						.append( '\n' );
					}
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: DisplayableEdgeDeletionEngineGenerator.ControlPanel.append() caught " + thrown );
			}
			return app;
		}

		public StringBuilder copyToSystemClipboard(){
			StringBuilder buff = new StringBuilder( 0x20 );
			Util.copyToSystemClipboard( this.append( buff ).toString() );
			return buff;
		}

		/** @since 20080228 */
		public ControlPanel select( Collection<FiniteVariable> vars ){
			DisplayableBeliefNetwork bn = (DisplayableBeliefNetwork) ie.getBeliefNetwork();
			bn.getNetworkInternalFrame().getNetworkDisplay().select( vars );
			return this;
		}

		/** @since 20080228 */
		public ControlPanel none(){
			DisplayableBeliefNetwork bn = (DisplayableBeliefNetwork) ie.getBeliefNetwork();
			bn.getNetworkInternalFrame().getNetworkDisplay().nodeSelectionClearAll();
			return this;
		}

		@SuppressWarnings( "unchecked" )
		public ControlPanel rotate(){
			DisplayableBeliefNetwork bn = (DisplayableBeliefNetwork) ie.getBeliefNetwork();
			NodeLabel                nodelabel;
			Monitor                  monitor;
			Map<Monitor,Integer>     monitors    = new HashMap<Monitor,Integer>( bn.size() );
		  //int[]                    phasecounts = new int[ Monitor.INT_MAX_PHASE ];
			Map<Integer,Integer>     phasecounts = new HashMap<Integer,Integer>( Monitor.INT_MAX_PHASE );
			int                      next = -1, count = -1, phase = -1, max = -1, mode = -1;
			for( DisplayableFiniteVariable dvar : (Collection<DisplayableFiniteVariable>) bn ){
				if( (nodelabel =      dvar     .getNodeLabel()) == null ){ continue; }
				if( (monitor   = nodelabel.getEvidenceDialog()) == null ){ continue; }
				next           = monitor.rotate();
				if( (Monitor.INT_MIN_PHASE <= next) && (next <= Monitor.INT_MAX_PHASE) ){
					monitors.put( monitor, phase = next );
					phasecounts.put( phase, count = (phasecounts.containsKey( phase ) ? (phasecounts.get( phase ) + 1) : 1) );
					if( count > max ){
						max  = count;
						mode = phase;
					}
				}
			}
		  //System.out.println( "phasecounts? " + phasecounts + ", max? " + max + ", mode? " + mode );
			for( Monitor key : monitors.keySet() ){
				phase = monitors.get( key );
				while( phase != mode ){
					next  = key.rotate();
					if(   next  == phase ){ break; }
					else{ phase  = next; }
				}
			}
			return this;
		}

		/** @since 20080302 */
		@SuppressWarnings( "unchecked" )
		public int cptMonitors(){
			int count = 0;
			try{
				DisplayableBeliefNetwork  bn = (DisplayableBeliefNetwork) ie.getBeliefNetwork();
				NetworkInternalFrame     nif = bn.getNetworkInternalFrame();
				NetworkDisplay            nd = nif.getNetworkDisplay();

				Bridge2Tiger             b2t = Troll.solicit();
				for( DisplayableFiniteVariable dvar : (Set<DisplayableFiniteVariable>) nd.getSelectedNodes( new HashSet<DisplayableFiniteVariable>( 0x10 ) ) ){
					b2t.setCPTMonitorShown( dvar.getNodeLabel(), true );
					++count;
				}
			}catch( Throwable thrown ){
				Util.warn( thrown, "DisplayableEdgeDeletionEngineGenerator.ControlPanel.cptMonitors()" );
			}
			return count;
		}

		final      public    SamiamAction                      action_cptmons = new SamiamAction( "cpt monitors", "show cpt monitors for selected nodes", 'c', MainToolBar.getIcon( "CPTCopy16.gif" ) ){
			{ setAccelerator( getKeyStroke( VK_T, 0 ) ); }

			public void actionPerformed( ActionEvent event ){
				cptMonitors();
			}
		};
		final      public    SamiamAction                      action_rotate  = new SamiamAction( "rotate monitors", "rotate all monitors", 'o', MainToolBar.getIcon( "RotatedMonitor16.gif" ) ){
			{ setAccelerator( getKeyStroke( VK_R, 0 ) ); }

			public void actionPerformed( ActionEvent event ){
				rotate();
			}
		};
		final      public    SamiamAction                      action_none    = new SamiamAction( "none", "clear the selection", 'n', null ){
			{ setAccelerator( getKeyStroke( VK_DELETE, 0 ) ); }

			public void actionPerformed( ActionEvent event ){
				none();
			}
		};
	  /*final      public    SamiamAction                      action_copy    = new SamiamAction( "copy", "copy statistics text to the system clipboard", 'y', MainToolBar.getIcon( "Copy16.gif" ) ){
			{ setAccelerator( getKeyStroke( VK_C, CTRL_MASK ) ); }

			public void actionPerformed( ActionEvent event ){
				copyToSystemClipboard();//if( copyable != null ){ copyToSystemClipboard( copyable ); }
			}
		};*/
		final      public    SamiamAction                      action_refresh = new SamiamAction( "refresh", "update statistics", 'r', MainToolBar.getIcon( "Properties16.gif" ) ){
			{ setAccelerator( getKeyStroke( VK_0, 0 ) ); }

			public void actionPerformed( ActionEvent event ){
				refresh();
			}
		};
		final      public    SamiamAction                      action_minimal = new SamiamAction( "minimal", "show only a minimal set of statistics", 'm', MainToolBar.getIcon( "ZoomOut16.gif" ) ){
			{ setAccelerator( getKeyStroke( VK_MINUS, 0 ) ); }

			public void actionPerformed( ActionEvent event ){
				showOnly( ATTRIBUTES_MINIMAL );
			}
		};
		final      public    SamiamAction                      action_maximal = new SamiamAction( "maximal", "show all statistics", 'a', MainToolBar.getIcon( "ZoomIn16.gif" ) ){
			{ setAccelerator( getKeyStroke( VK_EQUALS, 0 ) ); }

			public void actionPerformed( ActionEvent event ){
				showOnly( ATTRIBUTES_ALL );
			}
		};

		transient  private   SamiamAction[]                    actions        =
			new SamiamAction[]{ action_cptmons, action_rotate, action_none/*, action_copy*/, action_refresh, action_minimal, action_maximal };

		/** @since 20080309 */
		public ControlPanel die(){
			try{
				if(       panelmain != null ){
					panelmain.setVisible( false );
					panelmain.removeAll();
					Container parent = panelmain.getParent();
					if( parent != null ){ parent.remove( panelmain ); }
				}
				this      .panelmain  = null;
				if(    beliefnetwork != null ){ beliefnetwork.getEvidenceController().removeEvidenceChangeListener( this ); }
				this  .beliefnetwork  = null;
				if(               ie != null ){ ie.removeIterationListener( this ); }
				this             .ie  = null;
				this             .ui  = null;
				if(       attr2comps != null ){     attr2comps.clear(); }
				this     .attr2comps  = null;
				if(       attr2label != null ){     attr2label.clear(); }
				this     .attr2label  = null;
				if(   command2action != null ){ command2action.clear(); }
				this .command2action  = null;
				if(      rd2actn_log != null ){    rd2actn_log.clear(); }
				this    .rd2actn_log  = null;
				if(    rd2actn_stats != null ){  rd2actn_stats.clear(); }
				this  .rd2actn_stats  = null;
				if(     catme2action != null ){   catme2action.clear(); }
				this   .catme2action  = null;
				if(     catco2action != null ){   catco2action.clear(); }
				this   .catco2action  = null;
				if(            popup != null ){ popup.removeAll(); }
				this          .popup  = null;
			  //this  .mouselistener  = null;
				this           .root  = null;
				if(          actions != null ){ Arrays.fill( actions, null ); }
				this        .actions  = null;
			}catch( Exception thrown ){
				Util.warn( thrown, "DisplayableEdgeDeletionEngineGenerator.ControlPanel.die()" );
			}

			return this;
		}

		transient  private   JPanel                            panelmain      = new JPanel( new GridBagLayout() );
		transient  private   BeliefNetwork                     beliefnetwork;
		transient  private   EdgeDeletionInferenceEngine       ie;
		transient  private   UI                                ui;
		transient  private   Map<Attribute,VisibilityAdapter>  attr2comps     = new EnumMap<Attribute,VisibilityAdapter>( Attribute.class );
		transient  private   Map<Attribute,JLabel           >  attr2label     = new EnumMap<Attribute,JLabel           >( Attribute.class );
		transient  private   Map<Command,  SamiamAction     >  command2action = new EnumMap<Command,  SamiamAction     >(   Command.class );
	  //transient  private   Map<AbstractButton,Redirect    >  buttn2redirect = new HashMap<AbstractButton,Redirect    >(  Redirect.values().length );
		transient  private   Map<Redirect,  SamiamAction    >  rd2actn_log, rd2actn_stats;
		transient  private   Map<Category,  SamiamAction    >  catme2action   = new EnumMap<Category, SamiamAction     >(  Category.class );
		transient  private   Map<Category,  SamiamAction    >  catco2action   = new EnumMap<Category, SamiamAction     >(  Category.class );
		transient  private   JPopupMenu                        popup;
	  //transient  private   MouseListener                     mouselistener;
		transient  private   Component                         root;
	  //transient  private   String                            copyable;
	}
}
