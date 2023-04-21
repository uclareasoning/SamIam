package edu.ucla.belief.ui.displayable;

import        il2.inf.edgedeletion.*;
import        il2.inf.edgedeletion. EDAlgorithm.RankingHeuristic;
import        edu.ucla.belief.approx.*;
import        edu.ucla.belief.approx. EdgeDeletionBeliefPropagationSetting;
import        edu.ucla.belief.approx. Macros.Recoverables;
import        edu.ucla.belief.approx. Macros.Recoverable;
import        edu.ucla.belief.approx. Macros.RankingArgs;
import        edu.ucla.belief.*;
import        edu.ucla.belief.io.*;
import static edu.ucla.belief.io. PropertySuperintendent.*;
import        edu.ucla.util. PropertyKey;
import static edu.ucla.util. PropertyKey.*;

import        edu.ucla.belief.ui. NetworkInternalFrame;
import        edu.ucla.belief.ui.actionsandmodes.*;
import        edu.ucla.belief.ui.event.*;
import        edu.ucla.belief.ui.toolbar. MainToolBar;
import        edu.ucla.belief.ui.internalframes. CrouchingTiger;
import        edu.ucla.belief.ui.internalframes. Bridge2Tiger;
import        edu.ucla.belief.ui.util. Util;
import static edu.ucla.belief.ui.util. Util.warn;

import        java.awt.*;
import        java.awt.event.*;
import        javax.swing.*;
import        javax.swing.event.ChangeListener;
import        javax.swing.event.ChangeEvent;
import        javax.swing.border.Border;
import        java.util.*;
import        java.io.StringWriter;
import        java.io.PrintWriter;

/** automatic edge recovery based on heuristic ranking of recoverable edges
	@author keith cascio
	@since  20081023 */
public class EdgeRecovery implements
  SamiamUserModal,
  NetStructureChangeListener,
  ChangeListener,
  ItemListener,
  HierarchyListener
{
	public EdgeRecovery(){}

	public static final String
	  KEY_SELF    = "EdgeRecovery",
	  STR_ICON    = "RecoverEdge16.gif",
	  KEY_IFRAME  = KEY_SELF + ".iframe",
	  KEY_SYNCH   = KEY_SELF + ".synchronization",
	  TOKEN_K     = "KCOUNT",
	  TOKEN_H     = "HEURISTIC",
	  STR_TOOLTIP = "<html>Recover the first <b>"+TOKEN_K+"</b> edges as ranked by the \"<b>"+TOKEN_H+"</b>\" heuristic.";

	/** interface SamiamUserModal
		@since 20081024 */
	public void setSamiamUserMode( SamiamUserMode mode ){
		if( this.nif == null ){ return; }
		JInternalFrame iframe = (JInternalFrame) nif.getClientProperty( KEY_IFRAME );
		if( iframe   == null || (! iframe.isVisible()) ){ return; }
		if( (! mode.contains( SamiamUserMode.EDIT )) || mode.contains( SamiamUserMode.QUERY ) ){ iframe.setVisible( false ); }
	}

	public EdgeRecovery set( NetworkInternalFrame nif ){
		if( this.nif != nif ){
			if( this.nif != null ){
				this.nif.removeNetStructureChangeListener( EdgeRecovery.this );
				this.nif.removeSamiamUserModal(            EdgeRecovery.this );
			}
			this.nif = nif;
			if( nif != null ){
				nif.addNetStructureChangeListener( EdgeRecovery.this );
				nif.addSamiamUserModal(            EdgeRecovery.this );
			}
		}
		return refresh();
	}

	public void netStructureChanged( NetStructureEvent event ){
		EdgeRecovery.this.refresh();
	}

	/** @since 20081023 */
	static public JInternalFrame edgeRecoveryControlPanel( NetworkInternalFrame nif ){
		EdgeRecovery edgerecovery = (EdgeRecovery) nif.getClientProperty( KEY_SELF );
		if( edgerecovery == null ){                nif.putClientProperty( KEY_SELF, edgerecovery = new EdgeRecovery() ); }
		return edgerecovery.iFrame( nif );
	}

	/** @since 20081023 */
	public JInternalFrame iFrame( NetworkInternalFrame nif ){
		warnings.clear();

		try{
			set( nif );
		}catch( Throwable thrown ){ warnings.add( thrown ); }

		JInternalFrame iframe = null;
		try{
			iframe = (JInternalFrame) nif.getClientProperty( KEY_IFRAME );
			if( iframe == null ){     nif.putClientProperty( KEY_IFRAME, iframe = initIFrame() );
			    nif.putClientProperty( KEY_SELF, EdgeRecovery.this );
				nif.getRightHandDesktopPane().add( iframe );
				iframe.setLocation( new Point( 0x80, 0x80 ) );
			}
		}catch( Throwable thrown ){ warnings.add( thrown ); }

		JComponent gui = null;
		try{
			gui = myPanel == null ? asJComponent() : myPanel;
			if( ! iframe.isAncestorOf( gui ) ){
				if( gui.getParent() != null ){ gui.getParent().remove( gui ); }
				iframe.getContentPane().add( gui );
			}
		}catch( Throwable thrown ){ warnings.add( thrown ); }

		try{
			if( nif.getSamiamUserMode().contains( SamiamUserMode.EDIT ) ){
				iframe.pack();
				iframe.setVisible( true );
				iframe.setSelected( true );
			}
		}catch( Throwable thrown ){ warnings.add( thrown ); }

		warnings.report( "EdgeRecovery.updateGUI()", 3 );

		return iframe;
	}

	/** @since 20081023 */
	private JInternalFrame initIFrame(){
		boolean
		  resizable   =  true,
		  closable    =  true,
		  maximizable = false,
		  iconifiable =  true;
		JInternalFrame iframe = new JInternalFrame( "Automatic Edge Recovery", resizable, closable, maximizable, iconifiable );
		iframe.setFrameIcon( MainToolBar.getIcon( STR_ICON ) );
		iframe.setDefaultCloseOperation( JInternalFrame.HIDE_ON_CLOSE );
		return iframe;
	}

	public EdgeRecovery refresh(){
		if( myPanel == null ){ return this; }

		int                       size = 0;
		DisplayableBeliefNetwork    bn = null;
		Map<Object,Object>  properties = null;
		Recoverables      recoverables = null;
		if( nif != null ){
			if( (bn = nif.getBeliefNetwork()) != null ){
				if( (properties = Macros.properties( (PropertySuperintendent) bn )) != null ){
					if( (recoverables = (Recoverables) properties.get( KEY_RECOVERABLES )) != null ){
						size = recoverables.size();
					}
				}
			}
		}

		if(     mySlider != null ){
			mySlider.setMaximum( size );
			int major     = size >> 2;
			int increment = Math.max( 1, major );
		  //mySlider.setPaintLabels( false );
			mySlider.setMajorTickSpacing( increment );
		  //mySlider.setPaintLabels(  true );
			sliderLabels( size, increment );
			mySlider.setValue( 0 );
		}
		if(     myAction != null ){ myAction.setEnabled( size > 0 ); }

		updateGUI();

		return this;
	}

	@SuppressWarnings( "unchecked" )
	private int sliderLabels( int size, int increment ){
		if( mySlider == null ){ return size; }
		Hashtable table = mySlider.createStandardLabels( increment, 0 );
		table.put( 0, lblTree );
		if( size > 0 ){
			table.remove( increment << 2 );
			table.put( size, lblExact );
		}
		mySlider.setLabelTable( table );
		return size;
	}

	public Thread recover(){
		if( nif == null ){ return null; }

		final RankingHeuristic heuristic = getRankingHeuristic();
		final int                  count = mySlider.getValue();
		final Runnable          runnable = new Runnable(){
			public void run(){
				recoverSynchronous( nif, heuristic, count );
			}
		};
		Thread thread = new Thread( runnable, "EdgeRecovery.recoverSynchronous()" );
		thread.start();
		return thread;
	}

	public Collection<FiniteVariable> recoverSynchronous( NetworkInternalFrame nif, RankingHeuristic heuristic, int count ){
		Object synch = null;
		synchronized( KEY_SYNCH ){
			if( (synch = nif.getClientProperty( KEY_SYNCH )) == null ){
			             nif.putClientProperty( KEY_SYNCH, synch = new Object() );
			}
		}

		Collection<FiniteVariable> ret = null;

		latest = Thread .currentThread();
		synchronized( synch ){
			asAction().setEnabled( false );
			try{
				Thread.sleep( 0x10 );
			}catch(       InterruptedException ie ){ return ret; }
			if( Thread .currentThread() != latest ){ return ret; }

			String status = " computing edge ranking over " +mySlider.getMaximum()+ " recoverable edges...";
			Util.pushStatusWest( nif, status );

		  //System.out.println( "EdgeRecovery.recoverSynchronous( "+heuristic+", "+count+" )" );
			RankingArgs                     args = null;
			BeliefNetwork                     bn = null;
			InferenceEngine               engine = null;
			EdgeDeletionInferenceEngine edengine = null;
			try{
				bn     = nif.getBeliefNetwork();
				engine = nif.getCanonicalInferenceEngine();
				if( engine instanceof EdgeDeletionInferenceEngine ){ edengine = (EdgeDeletionInferenceEngine) engine; }
				args = Macros.firstK( new RankingArgs( bn, edengine, heuristic, count ) );
				args.bridge.die();
				if( engine != null ){ engine.die(); }
				new edu.ucla.util.SystemGCThread().start();
			}catch( Throwable thrown ){
				warn( thrown, "EdgeRecovery.recoverSynchronous()" );
				CrouchingTiger.showWarning( "There was a problem ranking the recoverable edges.", thrown, nif );
			}

			Util.popStatusWest( nif, status );

			ret = ((CrouchingTiger) Bridge2Tiger.Troll.solicit()).doRecoverEdges( bn, args.ranked, nif );

			refresh();
		}

		return ret;
	}

	public SamiamAction asAction(){
		if( myAction == null ){ initAction(); }
		return myAction;
	}

	private void initAction(){
		if( myAction != null ){ return; }

		myAction = new SamiamAction( "recover", STR_TOOLTIP, 'r', MainToolBar.getIcon( STR_ICON ), KeyStroke.getKeyStroke( KeyEvent.VK_R, 0 ) ){
			public void actionPerformed( ActionEvent event ){
				EdgeRecovery.this.recover();
			}
		};
	}

	public Color setBackground( Color color ){
		if( myPanel == null ){ asJComponent(); }
		myPanel          .setBackground( color );
		mySlider         .setBackground( color );
		myPanelRadios    .setBackground( color );
		for( Component comp : myPanelRadios.getComponents() ){
			comp         .setBackground( color );
		}
		myPanelHeuristic .setBackground( color );
		return color;
	}

	public JComponent asJComponent(){
		if( myPanel == null ){ initGUI(); }
		return refresh().myPanel;
	}

	/** interface HierarchyListener */
	public void hierarchyChanged( HierarchyEvent hierarchyevent ){
	  //System.out.println( "hierarchyChanged( "+hierarchyevent+" )" );

		if(  hierarchyevent.getChanged()                               != myPanel ){ return; }
		if( (hierarchyevent.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) < 1 ){ return; }

	  //System.out.println( "hierarchyChanged( "+hierarchyevent+" )" );

		JComponent use = (SwingUtilities.getAncestorOfClass( JPopupMenu.class, myPanel ) != null) ? myPanelRadios : myCombo;

		if( ! myPanelHeuristic.isAncestorOf( use ) ){
			myPanelHeuristic.removeAll();
			myPanelHeuristic.add( use );
		  //myPanel.validate();
		}
	}

	private JComponent initRadios(){
		if( myPanelRadios   != null ){ return myPanelRadios; }
		myPanelRadios        = new JPanel( new GridLayout( RankingHeuristic.values().length, 1 ) );
		JRadioButton radio;
		ButtonGroup    group = new ButtonGroup();
		for( RankingHeuristic rankingheuristic : RankingHeuristic.values() ){
			group.add( radio = new JRadioButton( rankingheuristic.toString() ) );
			radio.putClientProperty( TOKEN_H, rankingheuristic );
			if( rankingheuristic == RankingHeuristic.DEFAULT ){ radio.setSelected( true ); }
			radio.addItemListener( EdgeRecovery.this );
			myPanelRadios.add( radio );
		}
		return myPanelRadios;
	}

	private RankingHeuristic getRankingHeuristic(){
		if( myPanel == null ){ return RankingHeuristic.DEFAULT; }

		if(      myPanel.isAncestorOf( myPanelRadios ) ){
			AbstractButton btn;
			for( Component comp : myPanelRadios.getComponents() ){
				if( comp instanceof AbstractButton ){
					btn = (AbstractButton) comp;
					if( btn.isSelected() ){ return (RankingHeuristic) btn.getClientProperty( TOKEN_H ); }
				}
			}
		  //return RankingHeuristic.DEFAULT;
		}
		else if( myPanel.isAncestorOf( myCombo ) ){
			return (RankingHeuristic) myCombo.getSelectedItem();
		}

		return null;
	}

	private void initGUI(){
		if( myPanel != null ){ return; }

		lblExact = new JLabel( "exact" );
		lblTree  = new JLabel( "tree" );

		myCombo   = new JComboBox( RankingHeuristic.values() );
		myCombo.addItemListener( EdgeRecovery.this );

		myPanelHeuristic = new JPanel( new GridLayout( 1,1 ) );
		myPanelHeuristic.add( myCombo );

		initRadios();

		mySlider  = new JSlider( 0, 8, 0 );
		mySlider.setPaintTicks(     true );
		mySlider.setPaintLabels(    true );
		mySlider.setMinorTickSpacing(  1 );
		mySlider.setMajorTickSpacing( 10 );
		mySlider.setSnapToTicks(    true );
		mySlider.setBorder( BorderFactory.createEmptyBorder( 4,0,8,0 ) );
		mySlider.addChangeListener( EdgeRecovery.this );

		myPanel              = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.anchor             = GridBagConstraints.SOUTHWEST;

		JLabel       caption = null;
		Border        border = BorderFactory.createEmptyBorder( 0,0,0,8 );

		caption = new JLabel( "<html><b>heuristic:" );
		caption.setBorder( border );
		c.gridwidth          = 1;
		c.anchor             = GridBagConstraints.EAST;
		myPanel.add( caption, c );
		c.anchor             = GridBagConstraints.SOUTHWEST;
		c.gridwidth          = GridBagConstraints.REMAINDER;
		c.weightx            = 1;
		c.fill               = GridBagConstraints.HORIZONTAL;
		myPanel.add(        myPanelHeuristic, c );
		c.weightx            = 0;
		c.fill               = GridBagConstraints.NONE;

	  //myPanel.add( new JRadioButton( "smjorn" ), c );

		caption = new JLabel( "<html><b>count:" );
		caption.setBorder( BorderFactory.createEmptyBorder( 4,0,6,8 ) );
		c.gridwidth          = 1;
		c.anchor             = GridBagConstraints.EAST;
		myPanel.add(                 caption, c );
		c.anchor             = GridBagConstraints.SOUTHWEST;
	  //myPanel.add( new JLabel(  "tree.." ), c );
		c.gridwidth          = GridBagConstraints.REMAINDER;
		myPanel.add(                mySlider, c );
	  //c.gridwidth          = GridBagConstraints.REMAINDER;
	  //myPanel.add( new JLabel( "..exact" ), c );

		c.gridwidth          = GridBagConstraints.RELATIVE;
		c.weightx            = 1;
		myPanel.add( Box.createHorizontalStrut( 8 ), c );
		c.gridwidth          = GridBagConstraints.REMAINDER;
		c.weightx            = 0;
		c.anchor             = GridBagConstraints.SOUTHEAST;
		myPanel.add( myButton = new JButton( asAction() ), c );

		myPanel.putClientProperty( KEY_SELF, EdgeRecovery.this );

		myPanel.setBorder( BorderFactory.createEmptyBorder( 2,8,2,2 ) );

		myPanel.addHierarchyListener( EdgeRecovery.this );
	}

	/** interface ChangeListener */
	public void	stateChanged( ChangeEvent changeevent ){
		if( changeevent.getSource() != mySlider ){ return; }
		if( mySlider.getValueIsAdjusting()      ){ return; }

		mySlider.setToolTipText( "<html>recover <b>" + mySlider.getValue() + "</b> edges" );

		updateGUI();
	}

	/** interface ItemListener */
	public void itemStateChanged( ItemEvent itemevent ){
	  //if( itemevent.getSource() != myCombo ){ return; }
		if( itemevent.getItemSelectable().getSelectedObjects() == null ){ return; }
		updateGUI();
	}

	private EdgeRecovery updateGUI(){
		if( myAction == null ){ return null; }
		int      k = 0;
		String tip = STR_TOOLTIP;
		warnings.clear();
		try{
			k = mySlider.getValue();
		}catch( Throwable thrown ){ warnings.add( thrown ); }
		try{
			myAction.setEnabled( k > 0 );
		}catch( Throwable thrown ){ warnings.add( thrown ); }
		try{
			tip = tip.replace( TOKEN_K, Integer.toString( k ) );
		}catch( Throwable thrown ){ warnings.add( thrown ); }
		try{
			tip = tip.replace( TOKEN_H, getRankingHeuristic().toString() );
		}catch( Throwable thrown ){ warnings.add( thrown ); }
		try{
			myAction.setToolTipText( tip );
		}catch( Throwable thrown ){ warnings.add( thrown ); }

		warnings.report( "EdgeRecovery.updateGUI()", 3 );

		return this;
	}

	/** @since 20081024 */
	static public class Warnings{
		public Warnings add( Throwable thrown ){
			caught.add( thrown );
			return this;
		}

		public Warnings clear(){
			caught.clear();
			return this;
		}

		public int size(){
			return caught.size();
		}

		public Warnings report( String method, int depth ){
			if( ! caught.isEmpty() ){
				System.err
				.append( "warning: " )
				.append( method )
				.append( " caught " )
				.append( Integer.toString( caught.size() ) )
				.append( " exceptions:\n" );
				for( Throwable thrown : caught ){
					report( thrown, depth );
				}
				caught.clear();
			}
			return this;
		}

		public Warnings report( Throwable thrown, int depth ){
			stringwriter.flush();
			thrown.printStackTrace( acc );
			String trace = stringwriter.toString();
			int    index = 0;
			for( int i=0; i<depth; i++ ){
				index = trace.indexOf( "\n", ++index );
			}
			System.err.append( trace, 0, index < 0 ? trace.length() : ++index );

			return this;
		}

		private         LinkedList<Throwable>             caught       = new LinkedList<Throwable>();
		private         StringWriter                      stringwriter = new StringWriter();
		private         PrintWriter                       acc          = new PrintWriter( stringwriter );
	}

	private             Warnings                          warnings = new Warnings();
	private             NetworkInternalFrame              nif;
	private             JComboBox                         myCombo;
	private             JSlider                           mySlider;
	private             JPanel                            myPanel, myPanelRadios, myPanelHeuristic;
	private             SamiamAction                      myAction;
	private             JComponent                        myButton;
	private             JLabel                            lblExact, lblTree;
	private             Thread                            latest;
}
