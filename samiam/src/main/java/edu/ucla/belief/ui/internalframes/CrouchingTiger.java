package edu.ucla.belief.ui.internalframes;

import        edu.ucla.belief.ui.UI;
import        edu.ucla.belief.ui.displayable. DisplayableBeliefNetwork;
import        edu.ucla.belief.ui.displayable. DisplayableBeliefNetworkImpl;
import        edu.ucla.belief.ui.displayable. DisplayableBeliefNetwork5;
import        edu.ucla.belief.ui.displayable. DisplayableFiniteVariableImpl;
import        edu.ucla.belief.ui.displayable. EdgeRecovery;
import        edu.ucla.belief.ui.displayable. DisplayableJTEngineGenerator;
import        edu.ucla.belief.ui.displayable. DisplayableEdgeDeletionEngineGenerator;
import        edu.ucla.belief.ui.displayable. DisplayablePropagationEngineGenerator;
import        edu.ucla.belief.ui.displayable. DisplayableRecoveryEngineGenerator;
import        edu.ucla.belief.ui.rc.          DisplayableRCEngineGenerator;
import        edu.ucla.belief.ui.NetworkInternalFrame;
import        edu.ucla.belief.ui.event.NetStructureEvent;
import static edu.ucla.belief.ui.util.Util.warn;
import static edu.ucla.belief.ui.util.Util.DEBUG_VERBOSE;
import static edu.ucla.belief.ui.util.Util.getStatusBar;
import        edu.ucla.belief.ui.preference.*;
import        edu.ucla.belief.ui.statusbar.StatusBar;
import        edu.ucla.belief.ui.networkdisplay. NodeLabel;
import        edu.ucla.belief.ui.networkdisplay. CPTMonitor;

import        edu.ucla.belief. BeliefNetwork;
import        edu.ucla.belief. FiniteVariable;
import        edu.ucla.belief. Dynamator;
import        edu.ucla.belief. DynaListener;
import        edu.ucla.belief. CrouchingTiger .DynamatorImpl;
import        edu.ucla.belief. InferenceEngine;
import        edu.ucla.belief.approx. EdgeDeletionEngineGenerator;
import        edu.ucla.belief.approx. EdgeDeletionInferenceEngine;
import        edu.ucla.belief.approx. RecoveryEngineGenerator;
import        edu.ucla.belief.io.     PropertySuperintendent;

import        il2.inf.edgedeletion.*;
import        il2.inf.edgedeletion.EDAlgorithm.RankingHeuristic;
import        il2.inf. Algorithm;
import        il2.inf. Algorithm .Setting;
import        il2.bridge.*;
import        il2.model.*;

import        java.lang.reflect.Constructor;

import        java.util.Collection;
import        java.util.Collections;
import        java.util.Map;
import        java.util.EnumMap;
import        java.util.EnumSet;
import        java.util.Set;
import        java.util.LinkedList;
import        java.util.HashSet;
import        java.util.Random;

import        javax.swing.JOptionPane;
import        javax.swing.JComponent;
import        javax.swing.*;
import        javax.swing.border.Border;
import        javax.swing.event.MenuListener;
import        javax.swing.event.MenuEvent;
import        java.awt.Component;
import        java.awt.Point;
import        java.awt.Dimension;
import        java.awt.GridBagLayout;
import        java.awt.GridBagConstraints;
import        java.awt.Color;

/** Factory for java 5 version objects.
	Java 5 tiger enhanced features enabled.
	@author keith cascio
	@since  20070321 */
public class CrouchingTiger implements Bridge2Tiger
{
	/** @since 20071211 */
	public boolean      isTiger(){ return true; }

	/** java 5 tiger enhanced features enabled */
	public String describe(){
		return "java 5 tiger enhanced features enabled";
	}

	/** @since 20081029 */
	public enum DisplayableDynamatorImpl{
		edbp            ( DynamatorImpl.edbp           , DisplayableEdgeDeletionEngineGenerator .class ),
		loopybp         ( DynamatorImpl.loopybp        , DisplayablePropagationEngineGenerator  .class ),
		random          ( DynamatorImpl.random         , null ),
		rcil1           ( DynamatorImpl.rcil1          , edu.ucla.belief.ui.recursiveconditioning.DisplayableRCEngineGenerator .class ),
		rcil2           ( DynamatorImpl.rcil2          , DisplayableRCEngineGenerator           .class ),
		shenoyshaferil1 ( DynamatorImpl.shenoyshaferil1, DisplayableJTEngineGenerator           .class ),
		shenoyshaferil2 ( DynamatorImpl.shenoyshaferil2, DisplayableJTEngineGenerator           .class ),
		hugin           ( DynamatorImpl.hugin          , DisplayableJTEngineGenerator           .class ),
		zchugin         ( DynamatorImpl.zchugin        , DisplayableJTEngineGenerator           .class );

		static private Map<DynamatorImpl,DisplayableDynamatorImpl> ANCESTRY;
		static private Map<DynamatorImpl,DisplayableDynamatorImpl> ANCESTRY(){
			if( ANCESTRY == null ){ ANCESTRY = new EnumMap<DynamatorImpl,DisplayableDynamatorImpl>( DynamatorImpl.class ); }
			return                  ANCESTRY;
		}

		static public DisplayableDynamatorImpl forDynamatorImpl( DynamatorImpl di ){
		  //return values()[ di.ordinal() ];
			return ANCESTRY().get( di );
		}

		static public Map<DynamatorImpl,Dynamator> decorate( UI ui, Map<DynamatorImpl,Dynamator> team ){
			DisplayableDynamatorImpl ddi;
			for( DynamatorImpl di : EnumSet.copyOf( team.keySet() ) ){
				if( ! (ddi = forDynamatorImpl( di )).clazz.isAssignableFrom( team.get( di ).getClass() ) ){
					team.put( di, ddi.create( ui, team.get( di ) ) );
				}
			}
			return team;
		}

		public Algorithm algorithm(){
			return di.algorithm();
		}

		public Map<Setting,Object> toIL2Settings( BeliefNetwork bn, Map<DynamatorImpl,Dynamator> team ){
			if( ! (bn instanceof PropertySuperintendent) ){ return null; }
			return di.toIL2Settings( bn, team );
		}

		static public Map<DynamatorImpl,Dynamator> canonical( Map<DisplayableDynamatorImpl,Dynamator> team ){
			EnumMap      <DynamatorImpl,Dynamator> canonical =   new EnumMap<DynamatorImpl,Dynamator>( DynamatorImpl.class );
			for( DisplayableDynamatorImpl ddi : team.keySet() ){
				canonical.put( ddi.di, team.get( ddi ).getCanonicalDynamator() );
			}
			return                           canonical;
		}

		public DisplayableDynamatorImpl compile( NetworkInternalFrame nif, Map<DynamatorImpl,Dynamator> team ){
			enlist( nif.getParentFrame(), team ).compile( nif.getBeliefNetwork(), (DynaListener) nif );
			return this;
		}

		public Dynamator canonical( UI ui, Map<DynamatorImpl,Dynamator> team ){
			return enlist( ui, team ).getCanonicalDynamator();
		}

		public Dynamator enlist( UI ui, Map<DynamatorImpl,Dynamator> team ){
			Dynamator dyn = team.get( this.di );
			if( dyn == null ){ team.put( this.di, dyn = create( ui, (Dynamator) null ) ); }
			return dyn;
		}

		static public Map<DynamatorImpl,Dynamator> create( UI ui, DisplayableDynamatorImpl ... targets ){
			Map<DynamatorImpl,Dynamator> team = new EnumMap<DynamatorImpl,Dynamator>( DynamatorImpl.class );
			for( DisplayableDynamatorImpl ddi : targets ){
				team.put( ddi.di, ddi.create( ui, (Dynamator) null ) );
			}
			return team;
		}

		public Dynamator create( UI ui, Dynamator canonical ){
			try{
				if( canonical == null ){ canonical = di.create(); }
				if(          constructor == null ){ return canonical; }
				else{ return constructor.newInstance(      canonical, ui ); }
			}catch( Throwable thrown ){
				System.err.append( "warning: " )
				.append( getClass().getDeclaringClass().getName() )
				.append( ".create() caught " )
				.append( thrown.toString() )
				.append( "\n" );
			}
			return null;
		}

		private <T extends Dynamator> DisplayableDynamatorImpl( DynamatorImpl di, Class<T> clazz ){
			this.di          = di;
			this.clazz       = clazz;
			this.constructor = constructor( clazz );

			ANCESTRY().put( di, this );
		}
		public     final       DynamatorImpl                        di;
		public     final       Class<? extends Dynamator>           clazz;
		public     final       Constructor<? extends Dynamator>     constructor;

		@SuppressWarnings( "unchecked" )
		static public <T extends Dynamator> Constructor<T> constructor( Class<T> clazz ){
			Constructor<T> constructor = null;
			if( clazz != null ){
				Class<?>[] types = null;
				for( Constructor<?> uctor : clazz.getConstructors() ){
					types = uctor.getParameterTypes();
					if( (types.length == 2)                           &&
					    Dynamator .class.isAssignableFrom( types[0] ) &&
					    UI        .class.isAssignableFrom( types[1] ) ){
					  constructor = (Constructor<T>) uctor;
					  break;
					}
				}
			}
			return constructor;
		}

		static public Set<DisplayableDynamatorImpl> select( Collection<DynamatorImpl> targets ){
			EnumSet<DisplayableDynamatorImpl> set = EnumSet.noneOf( DisplayableDynamatorImpl.class );
			for( DisplayableDynamatorImpl ddi : values() ){
				if( targets.contains( ddi.di ) ){ set.add( ddi ); }
			}
			return set;
		}

		static   public    Set<DisplayableDynamatorImpl>   il2Partials(){
			if( IL2PARTIALS == null ){ IL2PARTIALS = Collections.unmodifiableSet( select( DynamatorImpl.il2Partials() ) ); }
			return IL2PARTIALS;
		}

		static   public    Set<DisplayableDynamatorImpl>   il2s(){
			if( IL2S        == null ){ IL2S        = Collections.unmodifiableSet( select( DynamatorImpl.il2s()        ) ); }
			return IL2S;
		}

		static   private   Set<DisplayableDynamatorImpl>
		  IL2PARTIALS,
		  IL2S;
	}

	@SuppressWarnings( "unchecked" )
	public OutputPanel newOutputPanel( Map data, Collection variables, boolean useIDRenderer ){
		return new OutputPanel5( data, variables, useIDRenderer );
	}

	public EnumTableModel        newEnumTableModel(        BeliefNetwork bn ){
		return new EnumTableModel5( bn );
	}

	public EnumPropertyEditPanel newEnumPropertyEditPanel( BeliefNetwork bn ){
		return new EnumPropertyEditPanel5( bn );
	}

	/** @since 20070326 */
	public DisplayableBeliefNetworkImpl newDisplayableBeliefNetworkImpl( BeliefNetwork toDecorate, NetworkInternalFrame hnif ){
		return new DisplayableBeliefNetwork5( toDecorate, hnif );
	}

	/** @since 20071211 */
	public Thread probabilityRewrite( ProbabilityRewriteArgs args ){
		return edu.ucla.belief.ui.dialogs.ProbabilityRewrite.doTheThing( args );
	}

	/** @since 20080225 */
	@SuppressWarnings( "unchecked" )
	public Collection     dynamators( Collection dynamators, UI ui ){
	  //dynamators.add( new EdgeDeletionEngineGenerator() );
		if( UI.FLAG_ENABLE_EDBP_MANUAL ){ dynamators.add( new DisplayableEdgeDeletionEngineGenerator( new EdgeDeletionEngineGenerator(), ui ) ); }
		dynamators.add( new DisplayableRecoveryEngineGenerator( new RecoveryEngineGenerator(), ui ) );
		return dynamators;
	}

	/** @since 20080123 */
	public void   screenshotScripts( UI ui ){
		edu.ucla.belief.ui.util.ScreenCaptureAutomation.scriptDialog( ui );
	}

	/** @since 20080228 */
	public NodeLabel setCPTMonitorShown( NodeLabel label, boolean shown ){
		if( label == null ){ return label; }

		synchronized( label.getCPTMonitorSynch() ){
			Component    comp  = label.getCPTMonitor();
			boolean   already  = comp != null;
			if(       already == shown ){ return label; }
			else if(           ! shown ){
				comp.setVisible( false );
				return label;
			}

			DisplayableFiniteVariableImpl       dfvi = (DisplayableFiniteVariableImpl) label.getFiniteVariable();
			CPTMonitor                    cptmonitor = new CPTMonitor( dfvi );
			JComponent                    jcomponent = cptmonitor.asJComponent();
			jcomponent.setVisible( false );
			label.getMonitorParent().add( jcomponent, 0 );
			cptmonitor.setLocation( label.getActualLocation( new Point() ) );
		  //Util.STREAM_DEBUG.println( "jcomponent.getPreferredSize()? " + jcomponent.getPreferredSize() );
			jcomponent.setSize( jcomponent.getPreferredSize() );
			jcomponent.doLayout();
		  //jcomponent.repaint();
			jcomponent.revalidate();
			jcomponent.setVisible( true );
		  //Util.STREAM_DEBUG.println( "jcomponent.getSize()? " + jcomponent.getSize() );
		  //jcomponent.repaint();

		  /*Util.STREAM_DEBUG.println( "CPTMonitor clss? " + jcomponent.getClass().getName()       );
			Util.STREAM_DEBUG.println( "CPTMonitor size? " + jcomponent.getSize( new Dimension() ) );
			Util.STREAM_DEBUG.println( "CPTMonitor loca? " + jcomponent.getLocation( new Point() ) );
			Util.STREAM_DEBUG.println( "CPTMonitor visi? " + jcomponent.isVisible() );
			Util.STREAM_DEBUG.println( "CPTMonitor shown? " + jcomponent.isShowing() );*/

			label.setCPTMonitor( cptmonitor.asJComponent() );
		}
		return label;
	}

	/** @since 20080221 */
	public Thread        replaceEdge( final ProbabilityRewriteArgs args ){
		final Runnable runnable = new Runnable(){
			public void run(){
				doReplaceEdge( args.hnInternalFrame.getBeliefNetwork(), args.source, args.destination, args.hnInternalFrame );
			}
		};
		Thread thread = new Thread( runnable, "CrouchingTiger.doReplaceEdge( "+args.source.getID()+", "+args.destination.getID()+" )" );
		thread.start();
		return thread;
	}

	/** @since 20080221 */
	public static void showWarning( String message, Throwable thrown, NetworkInternalFrame nif ){
		if( thrown != null ){
			String msg = thrown.getMessage();
			if( msg == null ){ msg = thrown.toString(); }
			message += "\n" + msg;
		}
		JOptionPane.showMessageDialog( nif, message, "warning", JOptionPane.WARNING_MESSAGE );
	}

	/** @since 20080221 */
	private Collection<FiniteVariable> doReplaceEdge ( BeliefNetwork bn, FiniteVariable st, FiniteVariable en, NetworkInternalFrame nif ){
		if( ! bn.containsEdge( st, en ) ){ return null; }

		return doReplaceEdges( bn, new FiniteVariable[][]{ new FiniteVariable[]{ st, en } }, nif );
	}

	/** @since 20081022 */
	private Collection<FiniteVariable> doReplaceEdges( BeliefNetwork bn, FiniteVariable[][] edges, NetworkInternalFrame nif ){
		if( edges == null || edges.length < 1 || edges[0] == null || edges[0].length < 2 || edges[0][0] == null || edges[0][1] == null ){ return null; }

		try{
			SamiamPreferences sp   = nif.getPackageOptions();
			Preference        pref = sp.getMappedPreference( SamiamPreferences.netSizeOverRidesPrefs );
			if( ! ((Boolean)  pref.getValue()).booleanValue() ){
				pref.setValue( Boolean.TRUE );
				pref.setRecentlyCommittedFlag(  true );
				nif.getNetworkDisplay().changePackageOptions( sp );
				pref.setRecentlyCommittedFlag( false );
			}
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.replaceEdges()" );
		}

		try{
			nif.netStructureChanged( (NetStructureEvent) null );
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.replaceEdges()" );
			showWarning( "killing inference engine (nif.netStructureChanged)", thrown, nif );
		}

		Collection<FiniteVariable> added = new LinkedList<FiniteVariable>();
		FiniteVariable st = null, en = null;
		for( FiniteVariable[] array : edges ){
			st = array[0];
			en = array[1];
			if( ! bn.containsEdge( st, en ) ){ continue; }
			try{
				added = edu.ucla.belief.approx.Macros.replaceEdge( bn, st, en, added );
			}catch( Throwable thrown ){
				warn( thrown, "CrouchingTiger.replaceEdges()" );
				showWarning( "There was a problem replacing edge { "+st.getID()+" -> "+en.getID()+" }.", thrown, nif );
			}
		}

		try{
			nif.netStructureChanged(
				new NetStructureEvent( NetStructureEvent.NODES_ADDED,
					added == null ? Collections.emptySet() : new HashSet<FiniteVariable>( added ) ) );
			nif.netStructureChanged( new NetStructureEvent( NetStructureEvent.EDGE_REMOVED, null ) );
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.replaceEdges()" );
			showWarning( "There was a problem updating the graphics.", thrown, nif );
		}

		String status = "";
		try{
			if( edges.length == 1 ){ status = "replaced edge { "+st.toString()+" -> "+en.toString()+" }"; }
			else{                    status = "replaced " + edges.length + " edges"; }
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.replaceEdges()" );
		}

		try{
			if( nif.getParentFrame().joinThreadsSetMode() ){
				getStatusBar( nif ).setText( status, StatusBar .WEST );
			}
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.replaceEdges()" );
		}

		return added;
	}

	/** @since 20080221 */
	public Thread        recoverEdge( final ProbabilityRewriteArgs args ){
		final Runnable runnable = new Runnable(){
			public void run(){
				doRecoverEdge( args.hnInternalFrame.getBeliefNetwork(), args.source, args.destination, args.hnInternalFrame );
			}
		};
		Thread thread = new Thread( runnable, "CrouchingTiger.doRecoverEdge( "+args.source.getID()+", "+args.destination.getID()+" )" );
		thread.start();
		return thread;
	}

	/** @since 20080221 */
	private Collection<FiniteVariable> doRecoverEdge ( BeliefNetwork bn, FiniteVariable st, FiniteVariable en, NetworkInternalFrame nif ){
		if( bn.containsEdge( st, en ) ){ return null; }

		return doRecoverEdges( bn, new FiniteVariable[][]{ new FiniteVariable[]{ st, en } }, nif );
	}

	/** @since 20081022 */
	public Collection<FiniteVariable> doRecoverEdges( BeliefNetwork bn, FiniteVariable[][] edges, NetworkInternalFrame nif ){
		if( edges == null || edges.length < 1 || edges[0] == null || edges[0].length < 2 || edges[0][0] == null || edges[0][1] == null ){ return null; }
		try{
			nif.netStructureChanged( (NetStructureEvent) null );
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.recoverEdges()" );
			showWarning( "killing inference engine (nif.netStructureChanged)", thrown, nif );
		}

		Collection<FiniteVariable> removed = new LinkedList<FiniteVariable>();
		FiniteVariable st = null, en = null;
		for( FiniteVariable[] array : edges ){
			st = array[0];
			en = array[1];
			if( bn.containsEdge( st, en ) ){ continue; }
			try{
				removed = edu.ucla.belief.approx.Macros.recoverEdge( bn, st, en, removed );
			}catch( Throwable thrown ){
				warn( thrown, "CrouchingTiger.recoverEdges()" );
				showWarning( "There was a problem recovering edge { "+st.getID()+" -> "+en.getID()+" }.", thrown, nif );
			}
		}

		try{
			nif.netStructureChanged(
				new NetStructureEvent( NetStructureEvent.NODES_REMOVED,
					removed == null ? Collections.emptySet() : new HashSet<FiniteVariable>( removed ) ) );
			nif.netStructureChanged( new NetStructureEvent( NetStructureEvent.EDGE_ADDED, null ) );
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.recoverEdges()" );
			showWarning( "There was a problem updating the graphics.", thrown, nif );
		  //if( ! DEBUG_VERBOSE ){ thrown.printStackTrace(); }
		}

		String status = "";
		try{
			if( edges.length == 1 ){ status = "recovered edge { "+st.toString()+" -> "+en.toString()+" }"; }
			else{                    status = "recovered " + edges.length + " edges"; }
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.recoverEdges()" );
		}

		try{
			if( nif.getParentFrame().joinThreadsSetMode() ){
				getStatusBar( nif ).setText( status, StatusBar .WEST );
			}
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.recoverEdges()" );
		}

		return removed;
	}

	/** @since 20081022 */
	public Thread   randomSpanningForest( final NetworkInternalFrame nif ){
		final Runnable runnable = new Runnable(){
			public void run(){
				doRandomSpanningForest( nif );
			}
		};
		Thread thread = new Thread( runnable, "CrouchingTiger.doRandomSpanningForest()" );
		thread.start();
		return thread;
	}

	/** @since 20081022 */
	public Collection<FiniteVariable> doRandomSpanningForest( final NetworkInternalFrame nif ){
		BurntArgs args = new BurntArgs( nif )
		.suppress     ( true )
		.preamble     ( "The model is already disconnected, so it\n<html>is impossible to obtain a <b>spanning forest</b>." )
		.options      ( JOptionPane.YES_NO_CANCEL_OPTION )
		.instructions ( "<html>Click \"<b>Yes</b>\" to recover DESC and proceed with random spanning forest.\n<html>Click \"<b>No</b>\" to proceed without recovering edges (the result will not be a spanning forest).\n<html>Click \"<b>Cancel</b>\" to abort." );
		doFindBurntBridges( args );
		if( args.result == JOptionPane.CANCEL_OPTION ){ return null; }

		DisplayableBeliefNetwork     beliefnetwork = null;
		Converter                             vert = null;
		BayesianNetwork            bayesiannetwork = null;
		int[][]                      deletable_il2 = null;
		FiniteVariable[][]           deletable_il1 = null;
		try{
			beliefnetwork                          = nif.getBeliefNetwork();
			vert                                   = new Converter();
			bayesiannetwork                        = vert.convert( beliefnetwork );
			deletable_il2                          = EDEdgeDeleter.getEdgesToDeleteForRandomSpanningTree( bayesiannetwork, new Random() );
			deletable_il1                          = new FiniteVariable[ deletable_il2.length ][];
			for( int i=0; i<deletable_il2.length; i++ ){
				deletable_il1[i] = new FiniteVariable[]{ vert.convert( deletable_il2[i][0] ), vert.convert( deletable_il2[i][1] ) };
			}
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.doRandomSpanningForest()" );
			showWarning( "There was a problem calculating/converting.", thrown, nif );
		}
		return doReplaceEdges( beliefnetwork, deletable_il1, nif );
	}

	/** @since 20081023 */
	public Thread   findBurntBridges( final NetworkInternalFrame nif ){
		final Runnable runnable = new Runnable(){
			public void run(){
				doFindBurntBridges( new BurntArgs( nif ) );
			}
		};
		Thread thread = new Thread( runnable, "CrouchingTiger.doFindBurntBridges()" );
		thread.start();
		return thread;
	}

	/** @since 20081023 */
	static public class BurntArgs{
		public BurntArgs( NetworkInternalFrame nif ){
			this.nif = nif;
		}

		public BurntArgs suppress( boolean flag ){
			this.suppress_ok_message = flag;
			return this;
		}

		public BurntArgs preamble( String text ){
			this.preamble = text;
			return this;
		}

		public BurntArgs instructions( String instructions ){
			this.instructions = instructions;
			return this;
		}

		public BurntArgs options( int options ){
			this.options = options;
			return this;
		}

		final public NetworkInternalFrame nif;
		public       boolean              suppress_ok_message = false;
		public       String               preamble            = null, instructions = "<html>Click \"<b>Yes</b>\" to recover DESC.";
		public       int                  options             = JOptionPane.YES_NO_OPTION;
		public       int                  resultRecoverAll    = JOptionPane.YES_OPTION;

		public       int                  result              = -0x200;
	}

	/** @since 20081023 */
	public FiniteVariable[][] doFindBurntBridges( BurntArgs args ){
		DisplayableBeliefNetwork     beliefnetwork = null;
		FiniteVariable[][]           burnt         = null;
		try{
			beliefnetwork                          = args.nif.getBeliefNetwork();
			burnt                                  = edu.ucla.belief.approx.Macros.findBurntBridges( beliefnetwork );
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.doFindBurntBridges()" );
			showWarning( "There was a problem traversing the graph.", thrown, args.nif );
		}

		boolean none    = (burnt == null) || (burnt.length < 1);
		if(     none && args.suppress_ok_message ){ return burnt; }
		LinkedList<Object> message = new LinkedList<Object>();
		String             DESC    = null;
		try{
			if( none ){ message.add( "There are no burnt bridges." ); }
			else{
				StringBuilder buff = new StringBuilder( 0x100 );

				buff.append( "<html>" );
				if( args.preamble != null ){ buff.append( args.preamble ).append( "\n<html>" ); }
				buff.append( "Found " );
				switch( burnt.length ){
					case 1:
						DESC = "the edge";
						buff.append( "one burnt bridge between <b>" )
						.append( burnt[0][0] )
						.append( "</b> and <b>" )
						.append( burnt[0][1] )
						.append( "</b>." );
						message.add( buff.toString() );
						break;
					default:
						DESC = (burnt.length == 2 ? "both" : ("all " + burnt.length)) + " edges";
						buff.append( Integer.toString( burnt.length ) )
						.append( " replaced (\"deleted\") edges that,\n<html>each considered alone, destroy the connectivity\n<html>of the original model:" );
						message.add( buff.toString() );
						JPanel           pnl = new JPanel( new GridBagLayout() );
						pnl.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 16,0,16,0 ), BorderFactory.createCompoundBorder( BorderFactory.createLineBorder( Color.black, 1 ), BorderFactory.createEmptyBorder( 2,4,2,4 ) ) ) );
						GridBagConstraints c = new GridBagConstraints();
						c.anchor             = GridBagConstraints.SOUTHWEST;
						Border        border = BorderFactory.createEmptyBorder( 0,8,0,8 );
						JLabel lbl;
						for( FiniteVariable[] edge : burnt ){
						  //buff.append( edge[0] ).append( " -> " ).append( edge[1] ).append( "\n" );
							c.gridwidth = 1;
							c.weightx   = 1;
							pnl.add( new JLabel( edge[0].toString() ), c );
							c.weightx   = 0;
							lbl = new JLabel( "->" );
							lbl.setBorder( border );
							pnl.add( lbl, c );
							c.gridwidth = GridBagConstraints.REMAINDER;
							c.weightx   = 1;
							pnl.add( new JLabel( edge[1].toString() ), c );
						}
						pnl.add( Box.createHorizontalStrut( 0x100 ), c );
						message.add( pnl );
						break;
				}

				if( args.instructions != null ){ message.add( args.instructions.replace( "DESC", DESC ) ); }
			}
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.doFindBurntBridges()" );
			showWarning( "There was a problem creating the message.", thrown, args.nif );
		}

		try{
			Object[] array = message.toArray( new Object[ message.size() ] );
			if( none ){
				              JOptionPane.showMessageDialog( args.nif, array, "Report: Burnt Bridges",               JOptionPane.PLAIN_MESSAGE );
			}else{
				args.result = JOptionPane.showConfirmDialog( args.nif, array, "Report: Burnt Bridges", args.options, JOptionPane.WARNING_MESSAGE );
			}
		}catch( Throwable thrown ){
			warn( thrown, "CrouchingTiger.doFindBurntBridges()" );
			showWarning( "There was a problem traversing the graph.", thrown, args.nif );
		}

		if( args.result == args.resultRecoverAll ){ doRecoverEdges( beliefnetwork, burnt, args.nif ); }

		return burnt;
	}

	/** @since 20081023 */
	public Bridge2Tiger addEdgeRecovery( final JMenu menu, final UI ui ){
		final EdgeRecovery edgerecovery = new EdgeRecovery();
		JComponent gui = edgerecovery.asJComponent();
		menu.add(  gui );
		edgerecovery.setBackground( menu.getBackground() );

		for( Component comp : gui.getComponents() ){
			if( comp instanceof JButton ){ comp.setVisible( false ); }
		}

		menu.addMenuListener( new MenuListener(){
			public void menuCanceled(   MenuEvent e ){
				edgerecovery.refresh();
			}
			public void menuDeselected( MenuEvent e ){
			  //edgerecovery.refresh();
			}
			public void menuSelected(   MenuEvent e ){
				edgerecovery.set( ui.getActiveHuginNetInternalFrame() );
			}
		} );

		menu.addSeparator();
		menu.add( edgerecovery.asAction() );

		return this;
	}

	/** @since 20081023 */
	public Bridge2Tiger edgeRecoveryControlPanel( final NetworkInternalFrame nif ){
		if( nif == null ){ return this; }
		EdgeRecovery.edgeRecoveryControlPanel( nif );
		return this;
	}

	/** @since 20071216 */
	public static final <T extends Appendable> T id( T app, Object obj ){
		if( app == null ) return app;
		try{
			if( obj == null ) app.append( "null" );
			app.append( obj.getClass().getSimpleName() ).append( '@' ).append( Integer.toString( System.identityHashCode( obj ), 0x10 ) );
		}catch( java.io.IOException ioe ){
			warn( ioe, "CrouchingTiger.id()" );
		}
		return app;
	}
}
