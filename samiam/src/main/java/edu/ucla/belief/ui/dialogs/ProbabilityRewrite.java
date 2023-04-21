package edu.ucla.belief.ui.dialogs;

import        edu.ucla.belief.*;
import        edu.ucla.belief.io.*;
import        edu.ucla.belief.io.dsl.DSLNodeType;
import        edu.ucla.util.*;
import static edu.ucla.util.AbstractStringifier.*;

import        edu.ucla.belief.ui.UI;
import        edu.ucla.belief.ui.toolbar.MainToolBar;
import        edu.ucla.belief.ui.preference.SamiamPreferences;
import        edu.ucla.belief.ui.preference.Preference;
import static edu.ucla.belief.ui.preference.SamiamPreferences.STR_ASK_BEFORE_CPTCOPY;
import        edu.ucla.belief.ui.internalframes.Bridge2Tiger.ProbabilityRewriteArgs;
import        edu.ucla.belief.ui.util.JOptionResizeHelper;
import        edu.ucla.belief.ui.util.JOptionResizeHelper.JOptionResizeHelperListener;
import        edu.ucla.belief.ui.util.Util;
import static edu.ucla.belief.ui.util.Util.id;
import static edu.ucla.belief.ui.util.Util.htmlEncode;
import        edu.ucla.belief.ui.util.HyperLabel;
import        edu.ucla.belief.ui.util.DecimalField;
import        edu.ucla.belief.ui.util.Broadcaster;
import        edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import        edu.ucla.belief.ui.event.CPTChangeEvent;
import        edu.ucla.belief.ui.tabledisplay.*;
import        edu.ucla.belief.ui.tabledisplay.HuginGenieStyleTableFactory.TableModelHGS;
import        edu.ucla.belief.ui.displayable.DisplayableFiniteVariableImpl;
import        edu.ucla.belief.ui.actionsandmodes.Grepable.Redirect;
import static edu.ucla.belief.ui.actionsandmodes.Grepable.Redirect.*;

import        edu.ucla.belief.ui.dialogs.EnumModels;
import static edu.ucla.belief.ui.dialogs.EnumModels.firstUniqueKeyStroke;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property;
import static edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Semantics;
import static edu.ucla.belief.ui.dialogs.EnumModels.Semantics.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.View;
import static edu.ucla.belief.ui.dialogs.EnumModels.View.*;

import        edu.ucla.belief.ui.dialogs.Definitions;
import        edu.ucla.belief.ui.dialogs.Definitions.Format;
import static edu.ucla.belief.ui.dialogs.Definitions.Format.*;
import        edu.ucla.belief.ui.dialogs.Definitions.MoreInformation;
import        edu.ucla.belief.ui.dialogs.Definitions.GuessQuality;
import static edu.ucla.belief.ui.dialogs.Definitions.GuessQuality.*;

import        edu.ucla.belief.ui.dialogs.VisibilityAdapter.DelayedInitialization;
//import      edu.ucla.belief.ui.dialogs.VisibilityAdapter.DelayedInitialization.Merchant;
import        edu.ucla.belief.ui.dialogs.VisibilityAdapter.Proxy;
import        edu.ucla.belief.ui.dialogs.VisibilityAdapter.Proxy.Function;
import static edu.ucla.belief.ui.dialogs.VisibilityAdapter.Proxy.Function.*;
import        edu.ucla.belief.ui.dialogs.VisibilityAdapter.Proxy.Trait;
import static edu.ucla.belief.ui.dialogs.VisibilityAdapter.Proxy.Trait.*;
import        edu.ucla.belief.ui.dialogs.VisibilityAdapter.Packable;

import        edu.ucla.belief.ui.dialogs.Menus.Dendriform;
import        edu.ucla.belief.ui.dialogs.Menus.Items;
import        edu.ucla.belief.ui.dialogs.Menus.Models;

import        java.util.List;
import static java.util.EnumSet.of;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import        java.text.*;
import        java.awt.*;
import        java.awt.event.*;
import        javax.swing.*;
import        javax.swing.JToggleButton.ToggleButtonModel;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.BorderFactory.*;
import static javax.swing.Box.*;
import        javax.swing.border.*;
import        javax.swing.event.*;
import        javax.swing.table.*;
import        java.io.*;
import        java.util.*;
import        java.util.regex.*;
import        java.lang.reflect.*;
import static java.lang.Thread.sleep;
import static java.lang.Thread.yield;

import static edu.ucla.belief.ui.dialogs.ProbabilityRewrite.ConditionFunction.*;
import static edu.ucla.belief.ui.dialogs.ProbabilityRewrite.ValueFunction.*;
import static edu.ucla.belief.ui.dialogs.ProbabilityRewrite.Normalization.*;

/** supports "cpt copy" and other kinds of probability editing and transfer<br /><br />
<pre>
log{
2007-12-16 17:52 (before refactoring)
  compiles to 81 class files with a total size of about 313 Kb
  ProbabilityRewrite.class.getDeclaredMethods().length? 100
  41 of those are access methods like "access$4100"
  12 static methods: configure, deeplyInvalid, doTheThing, getFormatPercent, getInstance, intersectionOfConditions, main, makeSubcomponent, makeVerboseLabel, reportErrors, stain2stat, tooltip
  47  class methods: append, areButtonsVisible, commit, configureWriteAction, definitions, describeQuality, doPreview, extractMenuBar, getActionAdvancedView, getActionButtons, getActionHints, getActionKillValuesPanel, getActionPreview, getActionShowCardinalities, getActionShowNorm, getActionToggleConditionsFunctionReference, getActionToggleValuesFunctionReference, getActionWrite, getJComponent, makeButton, makeButton, makeNormalizationPanel, makePanelMain, makeSummaryPanelDetailed, makeSummaryPanelSimple, pack, pack, pad, pad, populateConditions, populateMenuBar, refreshSummary, registerButton, rewrite, run, setConditionsFunctionReferenceVisible, setExtraButtonsVisible, setHintsVisible, setSummary, setValuesFunctionReferenceVisible, setValuesPanel, showDialog, showDialog, strut, strut, weighted, writeHints

2007-12-20 00:30 (after refactoring)
  compiles to 66 class files with a total size of about 314 Kb
  ProbabilityRewrite.class.getDeclaredMethods().length? 60
  23 of those are access methods like "access$900"
  10 static methods: deeplyInvalid, doTheThing, getFormatPercent, getInstance, intersectionOfConditions, main, makeSubcomponent, makeVerboseLabel, reportErrors, stain2stat
  27  class methods: append, clear, commit, definitions, describeQuality, doPreview, extractMenuBar, getJComponent, makeNormalizationPanel, makePanelMain, makeSummaryPanelDetailed, makeSummaryPanelSimple, pack, pack, pad, pad, populateConditions, refreshSummary, rewrite, run, setValuesPanel, showDialog, showDialog, strut, strut, weighted, writeHints

2007-12-21 11:15 (after deleting commented code)
  removed 46,098 bytes worth of commented code, 21.73% of this file
}
</pre>
	@author Keith Cascio
	@since  20071203 */
public class ProbabilityRewrite
{
	private  static          boolean     DEBUG_BORDERS   =   false, DEBUG_STRUTS = false, DEBUG_DEEPLY = false, FLAG_SETJMENUBAR = true;

	public   static  final   ImageIcon   ICON            =   MainToolBar.getIcon( "CPTCopy16.gif" );
	public   static  final   Image       ICONIMAGE       =   ICON.getImage();

	public   static          Caste       CASTE_DEBUG     =   null;

	private ProbabilityRewrite(){}

	/** @since 20071211 */
	public static ProbabilityRewrite getInstance(){
		if(    INSTANCE == null ){ INSTANCE = new ProbabilityRewrite(); }
		else{  INSTANCE.clear(); }
		return INSTANCE;
	}
	private static ProbabilityRewrite INSTANCE;

	/** @since 20071221 */
	@SuppressWarnings( "unchecked" )
	public Caste caste(){
		if(  this.caste == null ){
			Caste caste  = null;
			try{
				if(              CASTE_DEBUG != null ){ caste = CASTE_DEBUG; }
				else if( UI.STATIC_REFERENCE != null ){
					caste = ((Collection<Caste>) UI.STATIC_REFERENCE.getPackageOptions().getMappedPreference( SamiamPreferences.STR_CASTE ).getValue()).iterator().next();
				}
			}catch( Throwable thrown ){
				System.err.println( "warning: ProbabilityRewrite.caste() caught " + thrown );
			}finally{
				if( caste == null ){ caste = Caste.values()[0].getDefault(); }
				this.caste( caste );
			}
		}
		return this.caste;
	}

	/** @since 20071223 */
	public ProbabilityRewrite caste( Caste caste ){
		if( (caste != null) && (this.caste != caste) ){
			try{
				this.caste            = caste;
				Set<Object> blacklist = caste.blacklist();
				Caste.blacklist( blacklist, id2proxy );
				if( menus != null ){ menus.blacklist( idmenus, blacklist ); }
			}
			catch( Throwable thrown ){
				System.err.println( "warning: ProbabilityRewrite.caste() caught " + thrown );
			}
		}
		return this;
	}

	/** 'user level'
		@since 20071221 */
	public enum Caste implements Semantic, Actionable<Caste>{
		developer     (    "developer", false        ),
		expert        (       "expert", Format.debug ),
		advanced      (     "advanced"               ),
		intermediate  ( "intermediate", Menu.help, Menu.options, Menu.normalize,
		                Ingredient.locks, Ingredient.arguments, Ingredient.conveniences, Ingredient.hints, Ingredient.normalization, Ingredient.conditionfunctions, Ingredient.valuefunctions,
		                Option.cardinalities,
		                ConditionFunction.values(), ValueFunction.values(),
		                Constituent.before, Constituent.effective, Constituent.statistics,
		                PreviewMenu.file, PreviewOption.linkedba ),
		novice        ( "novice", Menu.help, Menu.options, Menu.normalize,
		                Ingredient.locks, Ingredient.arguments, Ingredient.conveniences, Ingredient.buttons, Ingredient.hints, Ingredient.normalization, Ingredient.conditionfunctions, Ingredient.valuefunctions,
		                Option.cardinalities,
		                ConditionFunction.values(), ValueFunction.values(),
		                Constituent.before, Constituent.effective, Constituent.statistics,
		                PreviewMenu.file, PreviewOption.linkedba ),
		retard        ( "simple", Menu.values(), Ingredient.values(), Summary.values(), Option.values(), ConditionFunction.values(), ValueFunction.values(), Constituent.values(), PreviewOption.values(), PreviewMenu.values() );

		private Caste( String display, boolean publik ){
			this( display, publik, new Object[0] );
		}

		private Caste( String display, Object ... contribution ){
			this( display, true, contribution );
		}

		private Caste( String display, boolean publik, Object ... contribution ){
			this.display        =    display;
			this.contribution   =    contribution;
			this.publik         =    publik;
		  //this.blacklist = unmodifiableSet( make( contribution ) );
			properties.put( Property.display, display );
			properties.put( tooltip, display + " adds " + contribution.length + " to the blacklist" );
		}

		public   String               toString(){ return display; }

		public   Semantics           semantics(){ return exclusive; }

		public  Caste               getDefault(){ return retard; }//novice; }//
		public  Object                     get( Property property ){ return this.properties.get( property ); }
		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		/** @since 20071223 */
		public    static final <K,T extends Component> Map<?,T> blacklist(  Set<?> blacklist, Map<K,T> map ){
			if( (map == null) || (blacklist == null) ){ return map; }
			for( K key : map.keySet() ){ map.get( key ).setVisible( ! blacklist.contains( key ) ); }
			return map;
		}

		public    static final Set<Object> contribute(  Set<Object> blacklist, Object ... contribution ){
			for( Object contrib : contribution ){
				if(      contrib instanceof Collection ){ flattenc( blacklist, (Collection) contrib ); }
				else if( contrib.getClass().isArray()  ){ flattena( blacklist,              contrib ); }
				else                                    {           blacklist.add(          contrib ); }

			}
			return blacklist;
		}

		public    static final Set<Object>   flattenc(  Set<Object> blacklist, Collection<?> contribution ){
			for( Object contrib : contribution ){ contribute( blacklist, contrib ); }
			return blacklist;
		}

		public    static final Set<Object>   flattena(  Set<Object> blacklist, Object array ){
			if( (array == null) || (! array.getClass().isArray()) ){ return blacklist; }

			int length = Array.getLength( array );
			for( int i=0; i<length; i++ ){ contribute( blacklist, Array.get( array, i ) ); }
			return blacklist;
		}

		private   final        Set<Object>       make( Object ... contribution ){
			return contribute( ordinal() == 0 ? emptySet() : new HashSet<Object>( values()[ ordinal() - 1 ].blacklist() ), contribution );
		}

		private  static  Set<Caste> FUNCTIONAL;

		/** @since 20071224 */
		public   static  Set<Caste> functional(){
			if( FUNCTIONAL == null ){
				FUNCTIONAL = EnumSet.allOf( Caste.class );
				for( Caste caste : values() ){ if( caste.afunctional() || (! caste.publik) ){ FUNCTIONAL.remove( caste ); } }
				FUNCTIONAL = unmodifiableSet( FUNCTIONAL );
			}
			return FUNCTIONAL;
		}

		private  static      Caste[] PUBLIC;

		/** @since 20071226 */
		public   static      Caste[] publik(){
			if( PUBLIC == null ){
				LinkedList<Caste> backwards = new LinkedList<Caste>();
				Caste[]           all       = values();
				for( int i=all.length - 1; i>=0; --i ){ if( all[i].publik ){ backwards.add( all[i] ); } }
				PUBLIC = backwards.toArray( new Caste[ backwards.size() ] );
			}
			return PUBLIC;
		}

		/** @since 20071224 */
		public    final        boolean    afunctional(){
			if(    this.blacklist == null ){ blacklist(); }
			return this.afunctional;
		}

		public    final        Set<Object>  blacklist(){
			if(    this.blacklist  == null ){
			       this.blacklist   = unmodifiableSet( make( contribution ) );
			       this.afunctional = this.blacklist.containsAll( ValueFunction.SET );
			}
			return this.blacklist;
		}
		private                Set<Object>  blacklist;
		private   final            Object[] contribution;
		private                boolean      afunctional;
		public    final        boolean      publik;
		public    final        String       display;
	}

	/** @since 20071211 */
	public static Thread doTheThing( final ProbabilityRewriteArgs args ){
		final ProbabilityRewrite tool = ProbabilityRewrite.getInstance();

		Runnable runnable = new Runnable(){
			public void run(){
				List<Throwable> thrown = new LinkedList<Throwable>();
				try{
					int result = tool.showDialog(
						args.hnInternalFrame.getBeliefNetwork(),
						args.source,
						args.destination,
						args.title == null ? "CPT Copy Tool" : args.title,
						args.hnInternalFrame,
						null );
					if( result == JOptionPane.OK_OPTION ){
						try{
							Preference     pask  = args.hnInternalFrame.getPackageOptions().getMappedPreference( STR_ASK_BEFORE_CPTCOPY );
							boolean         ask  = ((Boolean) pask.getValue()).booleanValue();
							if( ask ){
								AbstractButton buttonPref = null;
								Object[] message = new Object[]{
									"<html>This will permanently edit the cpt parameters for \"<b>" + args.destination + "</b>\".",
									"<html>Would you like to save a backup copy of your model first?",
									"<html>Please click '<font color='#009900'>Yes</font>' to save a copy first, '<font color='#000099'>No</font>' to edit the values without saving, or '<font color='#990000'>Cancel</font>' to abandon the edit completely.",
									buttonPref   = new JCheckBox( "Don't ask me again." )
								};
								int askresult    = JOptionPane.showConfirmDialog( args.hnInternalFrame, message, "Confirm CPT Copy", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );

								if( buttonPref  != null && buttonPref.isSelected() ) pask.setValue( Boolean.FALSE );

								switch( askresult ){
									case JOptionPane.NO_OPTION:
										break;
									case JOptionPane.YES_OPTION:
										args.hnInternalFrame.getParentFrame().saveFileAs( args.hnInternalFrame, true );
										break;
									case JOptionPane.CANCEL_OPTION:
										return;
									default:
										throw new IllegalStateException( "unknown dialog result " + askresult );
								}
							}
						}catch( Throwable throwable ){ thrown.add( throwable ); }

						if( tool.commit() ){ args.hnInternalFrame.setCPT( args.destination ); }//args.hnInternalFrame.fireCPTChanged( new CPTChangeEvent( args.destination ) ); }//
					}
				}
				catch(          Throwable throwable ){ thrown.add( throwable ); }
				finally{ reportErrors( thrown, "ProbabilityRewrite.doTheThing().run()" ); }

				tool.clear();
			}
		};

		return tool.run( runnable, true );
	}

	public int showDialog( BeliefNetwork bn, FiniteVariable src, FiniteVariable dest ){
		return this.showDialog( bn, src, dest, "CPT Copy", null, null );
	}

	public int showDialog( BeliefNetwork bn, FiniteVariable src, FiniteVariable dest, String title, Component parent, JOptionResizeHelperListener listener )
	{
		this.mysrc      = src;
		this.mydest     = dest;
		this.mybn       = bn;
		this.myparent   = parent;

		JComponent info = getJComponent( bn, src, dest );
		if( info == null ){ return JOptionPane.CANCEL_OPTION; }

		new JOptionResizeHelper( info, true, 10000l, myJORHListener ).start();

		int result = JOptionPane.showConfirmDialog( parent, info, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );

		return result;
	}

	/** @since 20071213 */
	private JOptionResizeHelperListener myJORHListener = new JOptionResizeHelperListener(){
		/** @since 20071213 */
		public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
			thrown.clear();
			try{
				if(           FLAG_SETJMENUBAR          ){ container.setJMenuBar( ProbabilityRewrite.this.extractMenuBar() ); }
				if( METHOD_JDIALOG_SETICONIMAGE != null ){ METHOD_JDIALOG_SETICONIMAGE.invoke( container, ICONIMAGE ); }
			}catch( Throwable throwable ){ thrown.add( throwable ); }

			try{
			  //System.out.println( "topLevelAncestorDialog( "+id( container )+" )" );
				sleep( LONG_PACK_DELAY );// N.B.: sleeping for 0x40 aint enuf here, 0x100 empirically adequate
				container.pack();
			}catch( Throwable throwable ){ thrown.add( throwable ); }

			try{
				if( myUserJORHL != null ) myUserJORHL.topLevelAncestorDialog( container, helper );
			}catch( Throwable throwable ){ thrown.add( throwable ); }

			reportErrors( thrown, "ProbabilityRewrite.myJORHListener.topLevelAncestorDialog()" );
		}

		private List<Throwable> thrown = new LinkedList<Throwable>();
	}, myUserJORHL;

/*
warning: VisibilityAdapter "arguments" .firstReaction() caught java.lang.NullPointerException
java.lang.NullPointerException
        at edu.ucla.belief.ui.dialogs.VisibilityAdapter.reaction(VisibilityAdapter.java:744)
        at edu.ucla.belief.ui.dialogs.VisibilityAdapter.firstReaction(VisibilityAdapter.java:92)
        at edu.ucla.belief.ui.dialogs.VisibilityAdapter.access$300(VisibilityAdapter.java:54)
        at edu.ucla.belief.ui.dialogs.VisibilityAdapter$Initializer.run(VisibilityAdapter.java:472)
        at java.lang.Thread.run(Thread.java:619)
*/

	public JComponent getJComponent( BeliefNetwork bn, FiniteVariable src, FiniteVariable dest )
	{
		if( myPanelMain == null ){
			myPanelMain  = makePanelMain();
		  //ingredients.fallback( Property.pinchcomponent, myPanelMain );
		}
		else{ ingredients.renew( myPanelMain ); }

		List<Throwable> thrown = new LinkedList<Throwable>();

		try{ refreshSummary(     bn, src, dest ); }
		catch( Throwable throwable ){ thrown.add( throwable ); }

		try{
			populateConditions( bn, src, dest );
		}catch( Throwable throwable ){
			//thrown.add( new RuntimeException( "impossible conditions (see cause)", throwable ) );
			try{
				ingredients.consummate( Ingredient.arguments,    null );
				ingredients.consummate( Ingredient.locks,        null );
				ingredients.consummate( Ingredient.conveniences, null );
			}catch( Throwable threwanother ){
				System.err.println( threwanother );
			}
			try{
				String                 message = throwable.getMessage();
				if( message == null ){ message = throwable.toString(); }
				StringBuilder buff = new StringBuilder( 0x80 )
				.append( "<html>Sorry, the current <font color='#000099'><nobr>user level</nobr></font> '<b>" )
				.append( caste().toString() )
				.append( "</b>' does not support copying the cpt of \"<b><nobr>" )
				.append( myStringification.stringify(  src ) )
				.append( "</nobr></b>\" to \"<b><nobr>" )
				.append( myStringification.stringify( dest ) )
				.append( "</nobr></b>\" either because the number of parent variables does not match or the cardinalities could not be matched. If you still want to do the copy, please switch to one of the <font color='#000099'>user levels</font> that support <font color='#cc6600'>functions</font>: <b><nobr>" )
				.append( Caste.functional().toString() )
				.append( "</nobr></b> <nobr>(see Preferences->Preferences->Inference->User Level)</nobr>. <br>Error message: \"" )
				.append( htmlEncode( message ) )
				.append( "\"." );
				JComponent comp = new LabelConstrained( buff.toString() ).setMaximumWidth( 0x100 );
				comp.setBorder( createEmptyBorder( 0,0,0,0x20 ) );
				JPanel     pnl  = new JPanel( new BorderLayout() );
				pnl.add(                           comp, BorderLayout.CENTER );
				pnl.add( createHorizontalStrut( 0x140 ), BorderLayout.NORTH  );
				JOptionPane.showMessageDialog( myparent, pnl, "Copy Error", JOptionPane.WARNING_MESSAGE );
			}catch( Throwable threwanother ){
				System.err.println( threwanother );
			}
			return null;
		}finally{
			ingredients.consummate( Ingredient.arguments, null );
		}

		try{ writeHints(         bn, src, dest ); }
		catch( Throwable throwable ){ thrown.add( throwable ); }

		try{
			ProbabilityRewrite.this.setValuesPanel(             null, false, false );
		}catch( Throwable throwable ){
			thrown.add( throwable );
		}

		reportErrors( thrown, "ProbabilityRewrite.getJComponent()" );

		return myPanelMain;
	}

	/** @since 20071204 */
	public static List<Throwable> reportErrors( List<Throwable> thrown, String method ){
		if( (thrown == null) || thrown.isEmpty() ) return thrown;

		if( method == null ) method = "???";
		System.err.println( "warning: " + method + " caught " + thrown.size() + " exceptions:" );
		for( Throwable throwable : thrown ){
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				throwable.printStackTrace( Util.STREAM_VERBOSE );
			}
			else{ System.err.println( throwable ); }
		}

		return thrown;
	}

	public static final long LONG_PACK_DELAY = 0x100;

	/** @since 20071204 */
	public ProbabilityRewrite pack( long delay ){
		return pack( myPanelMain, delay );
	}

	/** @since 20071204 */
	public ProbabilityRewrite pack( Component component, long delay ){
		try{
			if( component == null ) return ProbabilityRewrite.this;
			Window window = SwingUtilities.getWindowAncestor( component );
			if( window != null ){
				if( delay > 0 ) Thread.sleep( delay );
				window.pack();
			}
		}catch( Throwable thrown ){
			reportErrors( Collections.singletonList( thrown ), "ProbabilityRewrite.pack()" );
		}
		return ProbabilityRewrite.this;
	}

	public ProbabilityRewrite refreshSummary( BeliefNetwork bn, FiniteVariable src, FiniteVariable dest ){
		String str;
		if( myLabelSource != null ){
			myLabelSource.setText( str = ProbabilityRewrite.this.myStringification.stringify(  src ) );
			myLabelSource.setToolTipText( "<html><b>" + str + "</b> - the <font color='#cc6600'>source</font> cpt");
		}
		if( myLabelDest   != null ){
			myLabelDest  .setText( str = ProbabilityRewrite.this.myStringification.stringify( dest ) );
			myLabelDest  .setToolTipText( "<html><b>" + str + "</b> - the <font color='#cc6600'>destination</font> cpt");
		}
		myPanelSource     .assume(  src, bn );
		myPanelDestination.assume( dest, bn );

		return ProbabilityRewrite.this;
	}

	/** @since 20071204 */
	public static class Interpolation
	{
		public   static   final   String     STR_HINT_PREFIX      = "<html><font color='#009900'><b>Hint</b></font>: ";

		public   static   final   String     STR_TOKEN_PREFIX     = "${",
		                                     STR_TOKEN_SUFFIX     =  "}",
		                                     REGEX_TOKEN_CHAR     = "[\\w.]",
		                                     REGEX_TOKEN          = "\\Q"+STR_TOKEN_PREFIX+"\\E"+REGEX_TOKEN_CHAR+"+\\Q"+STR_TOKEN_SUFFIX;
		public   static   final   int        INT_TOKEN_PREFIX_LEN = STR_TOKEN_PREFIX.length();
		public   static   final   Pattern    PATTERN_TOKEN        = Pattern.compile( REGEX_TOKEN ),
		                                     PATTERN_TOKEN_CHARS  = Pattern.compile( "^" + REGEX_TOKEN_CHAR + "+$" );
		public   static   final   Matcher    MATCHER_TOKEN        = PATTERN_TOKEN      .matcher(""),
		                                     MATCHER_TOKEN_CHARS  = PATTERN_TOKEN_CHARS.matcher("");

		/** note: bare token not surrounded by prefix/suffix */
		public Interpolation map( String token, int value ){
			return map( token, Integer.toString( value ) );
		}

		/** note: bare token not surrounded by prefix/suffix */
		public Interpolation map( String token, String value ){
			if( ! MATCHER_TOKEN_CHARS.reset( token ).matches() ) throw new RuntimeException( "illegal token does not match " + PATTERN_TOKEN_CHARS.pattern() );
			myToker.setLength( INT_TOKEN_PREFIX_LEN );
			myToker.append( token ).append( STR_TOKEN_SUFFIX );
			replacements.put( myToker.toString(), value );
			return Interpolation.this;
		}

		public StringBuilder newHint(){
			myBuff.setLength(0);
			myBuff.append( STR_HINT_PREFIX );
			return myBuff;
		}

		public CharSequence result( int index_capitalize ){
			myResult.setLength( 0 );
			MATCHER_TOKEN.reset( myBuff );
			String token, value;
			while( MATCHER_TOKEN.find() ) MATCHER_TOKEN.appendReplacement( myResult, Matcher.quoteReplacement( replacements.containsKey( token = MATCHER_TOKEN.group() ) ? replacements.get( token ) : token ) );
			MATCHER_TOKEN.appendTail( myResult );
			if( index_capitalize > 0 ) myResult.setCharAt( index_capitalize, Character.toUpperCase( myResult.charAt( index_capitalize ) ) );
			return myResult;
		}

		public String hint(){
			return this.result( STR_HINT_PREFIX.length() ).toString();
		}

		/** @since 20071219 */
		public Interpolation die(){
			if( myBuff       != null ){   myBuff.setLength(0);   myBuff.trimToSize(); }
			myBuff            = null;
			if( myToker      != null ){  myToker.setLength(0);  myToker.trimToSize(); }
			myToker           = null;
			if( myResult     != null ){ myResult.setLength(0); myResult.trimToSize(); }
			myResult          = null;
			if( replacements != null ){ replacements.clear(); }
			replacements      = null;
			return this;
		}

		private StringBuilder            myBuff = new StringBuilder( 0x40 ), myToker = new StringBuilder( STR_TOKEN_PREFIX );
		private StringBuffer           myResult = new StringBuffer(  0x40 );
		private Map<String,String> replacements = new HashMap<String,String>( 0x10 );
	}

	private Interpolation myInterpolation;

	/** @since 20071207 */
	private String describeQuality( int count ){
		if(           count  < 1 ) return  "no such matches";
		else if(      count == 1 ) return "one such match";
		else return   count  +               " such matches";
	}

	@SuppressWarnings( "unchecked" )
	public ProbabilityRewrite writeHints( BeliefNetwork bn, FiniteVariable src, FiniteVariable dest ){
		TableIndex indexSource =  src.getCPTShell().index(),
		           indexDest   = dest.getCPTShell().index();

		int numParentsSource   = indexSource.getNumVariables() - 1,
		    numParentsDest     = indexDest  .getNumVariables() - 1,
		    numParentsDiff     = Math.abs( numParentsDest - numParentsSource );

		String strDiffGrammaticalNumberSuffix   = numParentsDiff>1 ? "s" : "",
		       htmlSrc         = "\"<b>" + myStringification.stringify(  src ) + "</b>\"",
		       htmlDest        = "\"<b>" + myStringification.stringify( dest ) + "</b>\"",
		       functions       = ConditionFunction.values().length == 1 ? "the function '"+ConditionFunction.values()[0].name()+"'" : ("one or more of the functions "+ConditionFunction.STR_SET);

		Map<GuessQuality,Integer> quality = myConditionDef.quality();
		int numExcellent                  = quality.get(   excellent ),
		    numGood                       = quality.get(        good ),
		    numFair                       = quality.get(        fair ),
		    numMediocre                   = quality.get(    mediocre ),
		    numPoor                       = quality.get(        poor );

		String strGood                    = describeQuality( numGood ),
		       strFair                    = numFair == 0 ? "no" : (numFair == 1 ? "one" : Integer.toString( numFair ) ),
		       strFairGrammaticalNumberSuffix   = numFair == 1 ? "" : "s",
		       strMediocre                = numMediocre < 1 ? "" : (numMediocre == 1 ? " and one with a different label" : (" and "+numMediocre+" with different labels")),
		       strPoor                    = numPoor == 0 ? "none" : Integer.toString( numPoor );

		(( myInterpolation == null ) ? (myInterpolation = new Interpolation()) : myInterpolation)
		.map( "numParentsSource",                numParentsSource )
		.map( "numParentsDest",                  numParentsDest   )
		.map( "numParentsDiff",                  numParentsDiff   )
		.map( "strDiffGrammaticalNumberSuffix",  strDiffGrammaticalNumberSuffix )
		.map( "htmlSrc",                         htmlSrc          )
		.map( "htmlDest",                        htmlDest         )
		.map( "functions",                       functions        )
		.map( "ConditionFunction.STR_SET",       ConditionFunction.STR_SET )
		.map( "numExcellent",                    numExcellent     )
		.map( "numGood",                         numGood          )
		.map( "numFair",                         numFair          )
		.map( "numMediocre",                     numMediocre      )
		.map( "numPoor",                         numPoor          )
		.map( "strGood",                         strGood          )
		.map( "strFair",                         strFair          )
		.map( "strFairGrammaticalNumberSuffix",  strFairGrammaticalNumberSuffix )
		.map( "strMediocre",                     strMediocre      )
		.map( "strPoor",                         strPoor          );

		StringBuilder buff = myInterpolation.newHint();
		if( numParentsSource == numParentsDest ){
			buff.append( "${htmlSrc} has the same number of parents as ${htmlDest}, i.e. ${numParentsDest} \u2261 ${numParentsSource}." );
		}
		else if( numParentsDest > numParentsSource ){
			buff.append( "${htmlDest} has ${numParentsDiff} more parent${strDiffGrammaticalNumberSuffix} than ${htmlSrc}. That means we must define at least ${numParentsDiff} condition${strDiffGrammaticalNumberSuffix} using ${functions}." );
		}
		else if( numParentsSource > numParentsDest ){
			buff.append( "${htmlDest} has ${numParentsDiff} less parent${strDiffGrammaticalNumberSuffix} than ${htmlSrc}. So the copy tool must \"forget\" the probability information corresponding to at least ${numParentsDiff} parent variable${strDiffGrammaticalNumberSuffix}." );
		}
		buff.append( " By default, the system initially defines the destination condition as the source condition (${htmlDest} gets ${htmlSrc}). Then it tries to match identical variables.  It found ${strGood}. Then it tries to match variables with the same cardinality, preferring variables with identical labels. It matched ${strFair} identical label${strFairGrammaticalNumberSuffix}${strMediocre}. Then it simply assigns as many remaining variables as it can, in this case ${strPoor}." );

		myHintConditions.setText( myInterpolation.hint() );


		Set<FiniteVariable> intersection      = intersectionOfConditions( src, dest );
		Set<FiniteVariable> parentsSrc        = indexSource.getParentsSet();
		Set<FiniteVariable> parentsDest       = indexDest  .getParentsSet();
		int                 sizeIntersection  = intersection.size();
		String           strSizeIntersection  = (sizeIntersection < 1) ? "no parents" : ((sizeIntersection == 1) ? "one parent" : (""+sizeIntersection+" parents"));
		String           strSizeIntersection2 = (sizeIntersection > 1) ? (""+sizeIntersection+" parents") : "parent";

		myInterpolation
		.map( "strSizeIntersection",  strSizeIntersection  )
		.map( "sizeIntersection",        sizeIntersection  )
		.map( "strSizeIntersection2", strSizeIntersection2 )
		.map( "intersection",      intersection.toString() );

		buff = myInterpolation.newHint();
		if( src == dest ){
			buff.append( "The source and destination are the same cpt: ${htmlSrc}. In this usage, you can think of 'cpt copy' as a <i>function-based probability editor.</i>" );
		}
		else if( parentsSrc.equals( parentsDest ) ){
			buff.append( "The source and destination variables have the same ${strSizeIntersection2}: ${intersection}." );
		}
		else if( intersection.isEmpty() ){
			buff.append( "The source and destination variables have ${strSizeIntersection} in common." );
		}
		buff.append( " Click the <font color='#0000ff'><u>blue hyperlink</u></font> for a condition to display its values definition." );

		myHintSummary.setText( myInterpolation.hint() );

		return ProbabilityRewrite.this;
	}

	/** @since 20071217 */
	public enum Task implements Actionable<Task>{
		write   ( Menu.file, "write output in X format to Y" ){
			protected SamiamAction makeAction( final ProbabilityRewrite pr ){
				return new SamiamAction( null, null, name().charAt(0), null )
				{
					{ for( Property prop : Property.values() ){ if( get( prop ) != null ){ putValueProtected( prop.key, get( prop ) ); } } }

					public void actionPerformed( ActionEvent event ){
						pr.run( (Runnable) this, true );
					}

					public void run(){
						Format   format   = null;
						Redirect redirect = null;
						try{
							format   =   Format.selected( pr.idmenus );
							redirect = Redirect.selected( pr.idmenus );
							pr.append( redirect.open( null, null ), format );
						}catch( Throwable thrown ){
							System.err.println( "warning: failed to write output in " + format.get( display ) + " format to " + redirect.get( display ) + ", caught " + thrown );
						}
					}
				};
			}
		},
		preview ( Menu.file, "open a separate window to help you see the effect of rewriting probabilities" ){
			protected SamiamAction makeAction( final ProbabilityRewrite pr ){
				return new SamiamAction( null, null, name().charAt(0), null )
				{
					{ for( Property prop : Property.values() ){ if( get( prop ) != null ){ putValueProtected( prop.key, get( prop ) ); } } }

					public void actionPerformed( ActionEvent event ){
						pr.run( (Runnable) this, true );
					}

					public void run(){
						thrown.clear();
						try{
							pr.doPreview( pr.myparent );
						}catch( InterruptedException interruptedexception ){
							System.err.println( "preview cancelled gracefully" );
						}catch( Throwable throwable ){
							thrown.add( throwable );
						}finally{
							reportErrors( thrown, "ProbabilityRewrite.Task.preview.run()" );
						}
					}
				};
			}
			private List<Throwable> thrown = new LinkedList<Throwable>();
		};

		private Task( Menu amenu, String tip ){
			properties.put( menu,        amenu  );
			properties.put( display,     name() );
			properties.put( tooltip,     tip    );
			properties.put( accelerator, firstUniqueKeyStroke( name() ) );
		}

		protected SamiamAction makeAction( ProbabilityRewrite pr ){ return null; }

		public    SamiamAction     action( ProbabilityRewrite pr ){
			Map<ProbabilityRewrite,SamiamAction> pr2sa = actions.get( this );
			if( pr2sa != null && pr2sa.containsKey( pr ) ){ return pr2sa.get( pr ); }
			SamiamAction action = makeAction( pr );
			if( pr2sa == null ){ pr2sa = Collections.singletonMap( pr, action ); }
			else{
				if( pr2sa.size() < 2 ){ pr2sa = new HashMap<ProbabilityRewrite,SamiamAction>( pr2sa ); }
				pr2sa.put( pr, action );
			}
			actions.put( this, pr2sa );
			return action;
		}

		static private Map<Task,Map<ProbabilityRewrite,SamiamAction>> actions = new EnumMap<Task,Map<ProbabilityRewrite,SamiamAction>>( Task.class );

		public  Task          getDefault(){ return preview; }
		public  Object               get( Property property ){ return this.properties.get( property ); }
		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );
	}

	/** @since 20071210 */
	public Dest    pad( Dest out, String str, int width ){
		return pad( out, str.length(), width );
	}

	/** @since 20071210 */
	public Dest    pad( Dest out, int len, int width ){
		int diff = width - len;
		if( diff < 1 ) return out;
		for( int i=0; i<diff; i++ ) out.app( ' ' );
		return out;
	}

	/** @since 20071210 */
	public class RewriteResult
	{
		@SuppressWarnings( "unchecked" )
		public RewriteResult( int size ){
			this.start         =   System.currentTimeMillis();
			this.explanation   =  (Set<ValueFunction>[]) new Set[ size ];
			this.thread        =   Thread.currentThread();

			this.note          =   "initializing";
		}

		public RewriteResult done( Parameters parameters ){
			this .elapsed      =  (this.end = System.currentTimeMillis()) - this.start;
			stats.inc( Stat   .changed, parameters  .changed );
			stats.inc( Stat .unchanged, parameters.unchanged );

			this.note          =   "completed";
			return this;
		}

		/** @since 20071211 */
		public RewriteResult cancel( String note ){
			this.note          =   note;
			return this;
		}

		/** @since 20071211 */
		public RewriteResult cancel(){
			return this.cancel( "interrupted while " + this.note );
		}

		/** @since 20071211 */
		public boolean interrupted(){
			return this.thread.isInterrupted();
		}

		/** @since 20071211 */
		public boolean interrupted( String note ){
			this.note          =   note;
			return interrupted();
		}

		/** @since 20071225 xmas */
		public RewriteResult effective( FiniteVariable source, Table effective, Set<FiniteVariable> unused ){
			this.source    = source;
			this.effective = effective;
			this.unused    = unused;
			return this;
		}

		/** @since 20071225 xmas */
		public FiniteVariable effectiveClone(){
			if(  clone  != null ){ return clone; }
			if( (source == null) || (effective == null) || (source.getCPTShell() == null) || (source.getCPTShell().getCPT() == effective)){ return null; }
			return clone = new FiniteVariableImpl( source, DSLNodeType.CPT, new TableShell( effective ) );
		}

		public RewriteResult increment( int index, ValueFunction function, Set<ValueFunction> functions, int overrides, int underrides )
		{
			stats.inc( Stat.total );
			if( function != skip ) stats.inc( Stat.written );

			if( function == null ) stats.inc( Stat.copied  );
			else                   function_counts.inc( function );

			stats.inc( Stat.overrides,   overrides );
			stats.inc( Stat.underrides, underrides );

			this.explanation[ index ] = EnumSet.copyOf( functions );

			return this;
		}

		public Dest append( Dest out ){
			int       total        = stats.get( Stat.      total ),
			          written      = stats.get( Stat.    written ),
			          copied       = stats.get( Stat.     copied ),
			          overrides    = stats.get( Stat.  overrides ),
			          underrides   = stats.get( Stat. underrides ),
			          changed      = stats.get( Stat.    changed ),
			          unchanged    = stats.get( Stat.  unchanged );

			String   stotal        =         s(            total ),
			         swritten      =         s(          written ),
			         sfromsrc      =         s(           copied ),
			         soverrides    =         s(        overrides ),
			         sunderrides   =         s(       underrides ),
			         schanged      =         s(          changed ),
			         sunchanged    =         s(        unchanged ),
			         selapsed      =         s(    (int) elapsed );

			Integer fc; int ifc; String f;
			for( ValueFunction vf : ValueFunction.values() ){
				f   = (fc = function_counts.get( vf )) == null ? null : s( fc );
				if( (ifc = vf.name().length() + 8) > wcaptions) wcaptions = ifc;
			}

			String caption = "elapsed:      ";
			out.app( caption );
			pad( out, caption.length() + selapsed.length(), wcaptions + wstats );
			out.app( selapsed ).app( " ms" ).nl().nl();

			append( out, "total:       ", stotal,      ratio( total,      total ) );
			append( out, "  written{   ", swritten,    ratio( written,    total ) );
			append( out, "    copied:  ", sfromsrc,    ratio( copied,   written ) );
			out.nl();
			for( ValueFunction vf : ValueFunction.values() ){
				if( vf == skip ) continue;
				ifc = ((fc = function_counts.get( vf )) == null) ? 0 : fc;
				append( out, "    " + vf.name() + ": ", s( ifc ), ratio( ifc, written ) );
			}
			out.app( "}" ).nl();

			int skipped = function_counts.get( skip );
			append( out, "  skipped:   ",s( skipped ), ratio(    skipped, total ) );
			out.nl();
			append( out, "overrides:   ", soverrides,  ratio(  overrides, total ) );
			append( out, "underrides:  ", sunderrides, ratio( underrides, total ) );
			out.nl();
			append( out, "changed:     ", schanged,    ratio(    changed, total ) );
			append( out, "unchanged:   ", sunchanged,  ratio(  unchanged, total ) );

			return out.nl().flush();
		}

		public String s( int stat ){
			String ret    = Integer.toString( stat );
			int    length = ret.length();
			if( length > wstats ) wstats = length;
			return ret;
		}

		public float ratio( int numerator, int denominator ){
			return ((float) numerator) / ((float) denominator);
		}

		public Dest append( Dest out, String caption, String stat, float ratio ){
			out.app( caption );
			pad( out, caption, wcaptions );
			pad( out,    stat, wstats    );
			out.app( stat ).app( " (" );
			String spercent = getFormatPercent( '%' ).format( ratio );
			pad( out, spercent, 6 );
			return out.app( spercent ).app( ")" ).nl();
		}

		public int stat( Object key ){
			if(      key instanceof Stat          ) return           stats.get( (Stat)          key );
			else if( key instanceof ValueFunction ) return function_counts.get( (ValueFunction) key );
			else throw new IllegalArgumentException();
		}

		public float ratio( Object key ){
			int stat = stat( key );
			return ((float) stat) / ((float) stats.get( Stat.total ));
		}

		public   final   long                    start;
		public           long                    elapsed = -1, end = -1;
		public           int                     wstats = 0, wcaptions = 0;
		public           Stats<Stat>             stats           = new Stats<Stat>(                   Stat.class );
		public           Stats<ValueFunction>    function_counts = new Stats<ValueFunction>( ValueFunction.class );
		public   final     Set<ValueFunction>[]  explanation;
		public           String                  note;
		public           Thread                  thread;
		public               FiniteVariable      source, clone;
		public           Table                   effective;
		public           Set<FiniteVariable>     unused;
	}

	/** @since 20071210 */
	public enum Stat{
		written    ( "parameter was written to, i.e. not skipped, but might not have been modified" ),
		copied     ( "the parameter was copied from the source cpt, i.e. not a function computation" ),
		overrides  ( "# times a function overrode another function based on precedence rules (can happen more than once per parameter)" ),
		underrides ( "# times a function did not override another function based on precedence rules (can happen more than once per parameter)" ),
		changed    ( "the parameter's value was modified",                                      new Color( 0xcc, 0xcc, 0x99 ) ),
		unchanged  ( "the parameter's value did not change as a result of probability rewrite", new Color( 0x99, 0xcc, 0x99 ) ),
		total      ( "# of parameters in the cpt, used to compute percentages" );

		private Stat( String tip, Color color ){ this( tip ); this.color = color; }

		private Stat( String tip ){ this.tip = tip; }

		/** @since 20071211 */
		public JLabel label(){
			JLabel ret = new JLabel( name() );
			ret.setToolTipText( this.tip );
			return ret;
		}

		/** @since 20071211 */
		public Stain stain(){
			if( (this.stain == null) && (this.color != null) ) this.stain = new Stain( this.name(), this.color, Color.black, this.tip );
			return this.stain;
		}

		public   final   String   tip;
		private          Color    color;
		private          Stain    stain;
	}

	/** @since 20071210 */
	public class Stats<G extends Enum<G>>{
		public Stats( Class<G> clazz ){
			this.clazz = clazz;
			this.data  = new int[ clazz.getEnumConstants().length ];
		}

		public int inc( G stat ){
			return this.inc( stat, 1 );
		}

		public int inc( G stat, int inc ){
			return (data[ stat.ordinal() ] += inc);
		}

		public int get( G stat ){
			return data[ stat.ordinal() ];
		}

		private   Class<G>   clazz;
		private   int[]      data;
	}

	/** @since 20071210 */
	public class Parameter{
		public Parameter(){
			src = constant = dest = Double.NaN;
		}

		public   double          src, constant, dest;
		public   ValueFunction   function;
		public   boolean         written;

		public   double          simple( double src, double constant, ValueFunction function ){
			this.src       = src;
			this.constant  = constant;
			this.function  = function;

			if(      function == null       ) dest =               ( src * constant );
			else                              dest = function.value( src,  constant );

			written = (function != skip);

			return dest;
		}

		public   double         written(){
			return this.written ? this.dest : 0;
		}

		public   double      complement( double local_complement ){
			if( this.function == complement ) dest =               ( local_complement * constant );

			return dest;
		}

		public   double       normalize( double factor ){
			return dest = dest * factor;
		}

		/** @since 20071211 */
		public   double            fill( double dest ){
			return this.dest = dest;
		}
	}

	/** @since 20071210 */
	public class Parameters
	{
		public Parameters( FiniteVariable destination ){
			int size   = destination.size();
			this.cells = new Parameter[ size ];
			for( int i=0; i<size; i++ ) this.cells[i] = new Parameter();
			doriginal  = destination.getCPTShell().getCPT().dataclone();
		}

		public   Parameter          set( int index, double src, double constant, ValueFunction function ){
			Parameter cell   = this.cells[ index ];
			sum_preliminary += cell.simple( src, constant, function );

			if( function == complement ) complementary = true;

			return cell;
		}

		public   Parameters      finish( Normalization normalization, double to ){
			if( complementary ){
				this.local_complement = 1 - this.sum_preliminary;

				for( Parameter cell : cells ) sum_raw += cell.complement( local_complement );
			}
			else{
				sum_raw = sum_preliminary;

			}
			for( Parameter cell : cells ) sum_written += cell.written();

			sum_normal = normalization.normalize( this, to );

			return this;
		}

		public   Parameters       write( Table cpt, int index0 ){
			int    stop    =   index0 + cells.length;
			int    local   =   0;
			double cp;
			for( int index=index0; index<stop; index++ ){
				if( (cp = cells[ local++ ].dest) == cpt.getCP( index ) ) ++unchanged;
				else                                                     ++  changed;
				cpt.setCP( index, cp );
			}

			return this;
		}

		public   Parameters       clear(){
			this.complementary    = false;
			this.sum_preliminary  = 0;
			this.sum_raw          = 0;
			this.sum_written      = 0;
			this.sum_normal       = Double.NaN;

			return this;
		}

		public   Parameter[]     cells;
		public   boolean         complementary;
		public   double          sum_preliminary, local_complement, sum_raw, sum_written, sum_normal;
		public   double[]        doriginal;
		public   int             changed = 0, unchanged = 0;
	}

	/** @since 20071210 */
	@SuppressWarnings( "unchecked" )
	public RewriteResult rewrite( FiniteVariable destination ) throws Exception{
		CPTShell            shellDest       =  destination.getCPTShell();
		TableIndex          indexDest       =  shellDest.index();
		Table               tableDest       =  shellDest.getCPT();
		int                 sizeTotal       =  indexDest.size();

		RewriteResult       result          =  new RewriteResult( sizeTotal );

		CPTShell            shellSourceOrig =  mysrc.getCPTShell();
		TableIndex          indexSourceOrig =  shellSourceOrig.index();

		Set<FiniteVariable> unused          =  myConditionDef.unused();
		Table               tableSource     =  null;
		TableIndex          indexSource     =  null;
		int             numSourceConditions =  -1;

		if( result.interrupted( "forgetting unused source conditions" ) ) return result.cancel();

	  //System.err.println( "unused? " + unused + ", indexSourceOrig.variables()? " + indexSourceOrig.variables() );

		if( ! unused.containsAll( indexSourceOrig.variables() ) ){
			tableSource                     =  shellSourceOrig.getCPT();
			for( FiniteVariable var : unused ) tableSource = tableSource.forget( var );
		                    indexSource     =  tableSource.index();
		                numSourceConditions = indexSource.getNumVariables();
		}
		result.effective( mysrc, tableSource, unused );

		if( result.interrupted(                         "'compiling'" ) ) return result.cancel();
		int                 sizeCondition   =     destination.size();
		Parameters          parameters      =  new Parameters( destination );

		int                  numConditions  =  indexDest.getNumVariables();
		int                 lastIndex       =  numConditions - 1;
		int[]               mindex          =  new                 int[ numConditions ];
		FiniteVariable[]    srcVariables    =  new      FiniteVariable[ numConditions ];
		FiniteVariable[]   destVariables    =  new      FiniteVariable[ numConditions ];
		Object[][]          valuedefs       =  new              Object[ numConditions ][];
		ValueDefinition[][] valueconstants  =  new     ValueDefinition[ numConditions ][];

		ConditionDefinition cd;
		ValuesDefinition    vsd;
		FiniteVariable      destVar, srcVar;
		Object              srcObject;
		if( result.interrupted(        "retrieving value definitions" ) ) return result.cancel();
		for( int condition=0; condition<numConditions; condition++ ){
			destVariables[condition]        = destVar = indexDest.variable( condition );
			srcObject                       = myConditionDef.get( destVar );
			if( srcObject != uniform ){
				srcVariables[condition]     =  srcVar = (FiniteVariable) srcObject;
				cd                          = myConditionDefinitions.get( destVar );
				if(  cd == null ) continue;
				vsd                         = cd.getValuesDefinition();
				if( vsd == null ) continue;
				valuedefs[     condition]   = vsd.asArrayOfObject();
				valueconstants[condition]   = vsd.asArrayOfValueDefinition();
			}
		}

		if( result.interrupted(          "setting up local variables" ) ) return result.cancel();
		double              src             =  0, constant_final = 0, constant_now;
		ValueFunction       function        =  null, fanother = null;

		Normalization       normalization   =  Normalization.selected( idmenus );//myNormMenuID );
		double              normal_to       =  myFieldNormalizationConstant == null ? 1 : myFieldNormalizationConstant.getValue();

		Set<ValueFunction>  allfunctions    =  EnumSet.allOf( ValueFunction.class ), functions = EnumSet.noneOf( ValueFunction.class );

		int                 instance, indexSrc, overrides, underrides, linear;
		Object              value;
		Map<FiniteVariable,Object>
		                    instantiations  = new HashMap<FiniteVariable,Object>( numConditions );

		String              note            = "proceeding to write probabilities for ";
		if( sizeCondition > 0 ) note       += (sizeTotal/sizeCondition) + " conditions";
		if( result.interrupted(                                  note ) ) return result.cancel();

		for( int segment=0; segment < sizeTotal; segment += sizeCondition )
		{
			if( result.interrupted() ) return result.cancel( "interrupted at " + segment + "/" + sizeTotal );
			indexDest.mindex( segment, mindex );

		  //System.out.println( "segment? " + segment + ", mindex? " + Arrays.toString( mindex ) );

			parameters.clear();
			for( instance = mindex[lastIndex]; instance < sizeCondition; instance = (mindex[lastIndex] += 1) )
			{
			  //System.out.println( "    instance? " + instance );

				instantiations.clear();
				functions.clear();
				linear                      = segment + instance;
				src                         = 0;
				function                    = null;
				constant_final              = constant_now = 1;
				overrides                   = underrides   = 0;
				for( int condition=0; condition<numConditions; condition++ ){
					if( (srcVar             =    srcVariables[condition]) == null ) continue;

					value                   =       valuedefs[condition][ mindex[condition] ];
					constant_now            =  valueconstants[condition][ mindex[condition] ].getConstant();

					if( allfunctions.contains( value ) ){
						fanother = (ValueFunction) value;
						if( (function == null) || (fanother.ordinal() < function.ordinal()) ){
							if( function != null ){
								++overrides;
							  //System.out.println( fanother.name() + "-" + fanother.ordinal() + " beats " + function.name() + "-" + function.ordinal() );
							}

							function        = fanother;
							constant_final  = constant_now;

							functions.add( function );
						}
						else ++underrides;
					}
					else{
						instantiations.put( srcVar, value );
						constant_final      = constant_now;
					}
				}
				if( (tableSource != null) && (instantiations.size() == numSourceConditions) )//performance speedup here
					src = tableSource.getCP( indexSrc = indexSource.index( instantiations ) );
				else if( function == skip ) src = tableDest.getCP( linear );
				else if( function == null ){
					throw new IllegalStateException( "ProbabilityRewrite.rewrite() error{ !copy !skip !function }, seg? " + segment + ", inst? " + instance + ", insts? " + instantiations + ", |src conds|? " + numSourceConditions );
				}

				parameters.set( instance, src, constant_final, function );
				result.increment( linear, function, functions, overrides, underrides );
			}
			parameters.finish( normalization, normal_to ).write( tableDest, segment );
		}

		return result.done( parameters );
	}


	/** @since 20071209 */
	public Dest append( Dest out, Format format ){
		Normalization  normalization   = Normalization.selected( idmenus );//myNormMenuID );
		double         normal_to       = myFieldNormalizationConstant == null ? 1 : myFieldNormalizationConstant.getValue();
		try{
			switch( format ){
				case human:
				  //"human readable probability rewrite definitions on "
					out.app( "probability rewrite definitions on " ).app( new Date( System.currentTimeMillis() ).toString() ).nl()
					   .app( "source{ \"" ).app( myStringification.stringify( mysrc ) ).app( "\" } -> destination{ \"" ).app( myStringification.stringify( mydest ) ).app( "\" }" ).nl()
					   .app( Integer.toString( myConditionDef.size() ) ).app( " conditions{" ).nl().nl();
					myConditionDef.append( out, format, myStringification.myVariableStringifier, myMoreInformation );
					out.app( '}' ).nl();
					out.app( "normalize " ).app( normalization.get( display ).toString() ).app( " to " ).app( Double.toString( normal_to ) ).nl();
					break;
				case xml:
					break;
			}
		}catch( Throwable thrown ){
			System.err.println( "warning: Definitions.append() caught " + thrown );
		}
		return out.flush();
	}

	/** @since 20071209 */
	private MoreInformation<FiniteVariable,FiniteVariable,ConditionFunction> myMoreInformation = new MoreInformation<FiniteVariable,FiniteVariable,ConditionFunction>()
	{
		public    boolean      none( FiniteVariable destination ){
			return definitions( destination ) == null;
		}

		public    Dest       append( Dest out, Format format, Stringifier ifier, FiniteVariable destination ){
			if( myConditionDefinitions == null ) return out;
			ConditionDefinition definition   =   myConditionDefinitions.get( destination );
			if( definition             == null ) return out;
			ValuesDefinition     valuesdef   =   definition.getValuesDefinition();
			if( valuesdef              == null ) return out;
			return valuesdef.append( out, format, ifier );
		}

		public    Dest     separate( Dest out, Format format ){
			switch( format ){
				case human:
					out.nl();
					break;
			}
			return out;
		}

		public    Dest       prefix( Dest out, int   ordinal ){
			int size = myConditionDefinitions.size();
			if( size > 99 && ordinal <= 99 ) out.app( ' ' );
			if( size >  9 && ordinal <=  9 ) out.app( ' ' );
			return out.app( Integer.toString( ordinal ) ).app( ". " );
		}

		public    String    specify( ConditionFunction function, FiniteVariable destination ){
			return function.toString();
		}
	};

	/** @since 20071209 */
	private Definitions<Object,Object,ValueFunction> definitions( FiniteVariable destination ){
		if( myConditionDefinitions == null ) return null;
		ConditionDefinition definition   =   myConditionDefinitions.get( destination );
		if( definition             == null ) return null;
		ValuesDefinition     valuesdef   =   definition.getValuesDefinition();
		if( valuesdef              == null ) return null;
		return valuesdef.getDefinitions();
	}

	/** @since 20071219 */
	public ProbabilityRewrite clear(){
		mysrc                       = mydest = null;
		mybn                        = null;
		myparent                    = null;
		caste                       = null;
		if( myHintSummary          != null ){ myHintSummary   .setText( "clear" ); }
		if( myHintConditions       != null ){ myHintConditions.setText( "clear" ); }
		if( myPanelConditions      != null ){ myPanelConditions.removeAll(); }
		if( myContainerValues      != null ){ myContainerValues.removeAll(); }
		myConditionVariables        = null;
		myConditionDef              = null;
		if( myConditionDefinitions != null ){
			for( ConditionDefinition cd : myConditionDefinitions.values() ){ cd.die(); }
			myConditionDefinitions.clear();
		}
		myConditionDefinitions      = null;
		if( myPackAnimal           != null ){ myPackAnimal.clear(); }
	  //if( ingredients            != null ){ ingredients.renew(); }

		return this;
	}

	private FiniteVariable mysrc, mydest;
	private BeliefNetwork  mybn;
	private Component      myparent;

	/** @since 20071223 */
	private            Map<Object,Component> id2proxy = new HashMap<Object,Component>( 0x10 );
	private            Caste                 caste;
	private            JPanel                myPanelMain, myPanelConditions, myWidgetValues, myContainerValues;
	private            JLabel                myLabelSource, myLabelDest;
	private            Component             myPincher;
	private            VariablePanel         myPanelSource, myPanelDestination;
	private            LabelConstrained      myHintSummary, myHintConditions;
	private            JMenuBar              myMenuBar;
	private            Border                myMenuBarBorderDefault;
	private            DecimalField          myFieldNormalizationConstant;

	private             FiniteVariable[]                                      myConditionVariables;
	private Definitions<FiniteVariable,FiniteVariable,ConditionFunction>      myConditionDef;
	private         Map<FiniteVariable,ConditionDefinition>                   myConditionDefinitions;

	/** @since 20071207 */
	private AsynchronousItemListener myListenerStringification = new AsynchronousItemListener( "toggle view ids/labels" ){
		public AsynchronousItemListener runImpl() throws Exception
		{
			if( (itemevent != null) && (itemevent.getStateChange() != ItemEvent.SELECTED) ) return this;
			myStringification = Stringification.selected( idmenus );

			if( myConditionDefinitions != null ){ for( ConditionDefinition cd : myConditionDefinitions.values() ){ if( cd != null ){ cd.reText(); } } }

			deeplyInvalid( myPanelConditions );

			refreshSummary( mybn, mysrc, mydest );

			pack( LONG_PACK_DELAY );
			return this;
		}
	};

	public static final long LONG_ANIMATION_DELAY = 0x20;

	/** @since 20071204 */
	public enum AggregateFunction{
		blank  ( "" ),
		revert ( "revert defaults" ){
			public <T,V,U> AggregateFunction run( Definitions<T,V,U> definitions ){
				definitions.revert();
				return super.run( definitions );
			}
		};

		private AggregateFunction( String display ){ this.display = display; }

		public String toString(){ return this.display; }

		public <T,V,U> AggregateFunction run( Definitions<T,V,U> definitions ){ return this; }

		public            final   String                     display;

		public   static   final   EnumSet<AggregateFunction> SET = EnumSet.allOf( AggregateFunction.class );
	}

	public          void                strut( JComponent pnl, GridBagConstraints c ){
		strut( pnl, c, INT_SIZE_STRUT );
	}

	public          void                strut( JComponent pnl, GridBagConstraints c, int width ){
		pnl.add( Box.createHorizontalStrut( width ), c );
	}

	public          void             weighted( JComponent pnl, GridBagConstraints c, JComponent comp ){
		c.weightx = 1;
		c.fill    = GridBagConstraints.HORIZONTAL;
		pnl.add( comp, c );
		c.weightx = 0;
		c.fill    = GridBagConstraints.NONE;
	}

	/** @since 20071206 */
	public abstract class AsynchronousItemListener implements ItemListener, ActionListener, Runnable
	{
		public AsynchronousItemListener( String name ){
			this.name = name;
		}

		abstract public AsynchronousItemListener runImpl() throws Exception;

		public void itemStateChanged( ItemEvent event ){
			itemevent = event;
			ProbabilityRewrite.this.run( this, true );
		}

		public void actionPerformed( ActionEvent event ){
			actionevent = event;
			ProbabilityRewrite.this.run( this, true );
		}

		final public void run(){
			try{
				this.runImpl();
			}catch( Throwable thrown ){
				System.err.println( "warning: "+name+".run() caught " + thrown );
			}
		}

		public   final   String       name;
		protected        ItemEvent    itemevent;
		protected        ActionEvent  actionevent;
	}

	/** @since 20071207 */
	public abstract class ListDataAdapter implements ListDataListener{
		public ListDataAdapter( String name ){
			this.name = name;
		}

		public void contentsChanged( ListDataEvent listdataevent ){ react(); }
		public void   intervalAdded( ListDataEvent listdataevent ){ react(); }
		public void intervalRemoved( ListDataEvent listdataevent ){ react(); }

		public          ListDataAdapter react(){
			try{ return reactImpl(); }
			catch( Throwable thrown ){ System.err.println( "warning: ListDataAdapter "+name+" caught "+thrown ); }
			return this;
		}

		abstract public ListDataAdapter reactImpl() throws Exception;

		public final String name;
	}

	public static final int    INT_SIZE_STRUT   = 8;
	/** @since 20071218 */
	public static final Border BORDER_FOR_BOXES = BorderFactory.createEmptyBorder( 0, INT_SIZE_STRUT, 0, 0 );

	public abstract class Definition<D,S,F>
	{
		public Definition( D target, String thing ){
			this( thing );
			myTarget        = target;

			myCheckBox = new JCheckBox();
			myCheckBox.setToolTipText( "<html><font color='#cc0000'>lock</font> the definition of \"<font color='#0000cc'>"+stringify( target )+"</font>\"" );
			myCheckBox.setBorder( BORDER_FOR_BOXES );
		}

		protected Definition( String thing ){ myThing = thing; }

		abstract public String                   stringify( D obj );

		abstract public Object                  getDefault();

		abstract public Set<F>                functionsSet();

		abstract public JComponent[]             toDisable();

		abstract public Definition<D,S,F>              add( JComponent pnl, GridBagConstraints c, Definitions<D,S,F> definitions );

		abstract public Definition<D,S,F>           header( JComponent pnl, GridBagConstraints c, FiniteVariable destination );

		abstract public Definition<D,S,F>           footer( JComponent pnl, GridBagConstraints c, Definitions<D,S,F> definitions );

		public JLabel labelTarget(){
			myLabelTarget = new JLabel( stringify( myTarget ) );
			return myLabelTarget;
		}

		public JLabel labelAll(){
			JLabel ret = new JLabel( "all" );
			ret.setFont( ret.getFont().deriveFont( Font.BOLD ) );
			ret.setToolTipText( "<html>these <font color='#cc6600'>settings</font> conveniently control <font color='#00cc00'>all "+myThing+"s" );
			return ret;
		}

		public JCheckBox cbAll( final Definitions<D,S,F> definitions ){
			final JCheckBox cbAll = new JCheckBox();
			cbAll.setBorder( BORDER_FOR_BOXES );
			cbAll.setToolTipText( "<html><font color='#cc0000'>lock</font>/<font color='#009900'>unlock</font> <font color='#00cc00'>all "+myThing+"s" );

			AsynchronousItemListener aal;
			cbAll.addActionListener( aal = new AsynchronousItemListener( "setLockedAll" ){
				public AsynchronousItemListener runImpl() throws InterruptedException{
					if( ignore ) return this;
					ignore = true;
					definitions.setLockedAll( cbAll.isSelected(), 0x40 );
					ignore = false;
					return this;
				}
				private boolean ignore;
			} );

			ListDataListener listdatalistener;
			definitions.model____addListDataListener( null, listdatalistener = new ListDataAdapter( "cbAll" ){
				public ListDataAdapter reactImpl(){ cbAll.getModel().setSelected( definitions.fractionLocked() >= 0.5f ); return this; }
			} );
			listdatalistener.contentsChanged( null );

			return cbAll;
		}

		public JComboBox comboAll( final Definitions<D,S,F> definitions ){
			final AggregateFunction[] aggregates = AggregateFunction.values();
			final            Object[]  functions = definitions.functions().toArray();
			final            Object[]    choices = new Object[ aggregates.length + functions.length ];
			System.arraycopy( aggregates, 0, choices,                 0, aggregates.length );
			System.arraycopy(  functions, 0, choices, aggregates.length,  functions.length );
			final JComboBox comboAll = new JComboBox( choices );

			final Set               aggregateSet = AggregateFunction.SET;
			final Set               functionsSet = definitions.functions();

			ListDataAdapter adapter;
			definitions.model____addListDataListener( null, adapter = new ListDataAdapter( "comboAll?" ){
				public ListDataAdapter reactImpl(){
					comboAll.setEnabled( definitions.countLocked() < 1 );
					return this;
				}
			} );
			adapter.contentsChanged( null );

			final ItemListener listenSetFunction = new AsynchronousItemListener( "setFunctionAll" ){
				@SuppressWarnings( "unchecked" )
				public AsynchronousItemListener runImpl() throws InterruptedException{
					definitions.setFunctionAll( (F) comboAll.getSelectedItem(), 0x40 );
					return this;
				}
			};

			comboAll.addItemListener( new ItemListener(){
				@SuppressWarnings( "unchecked" )
				public void itemStateChanged( ItemEvent event ){
					if( (event != null) && (event.getStateChange() != ItemEvent.SELECTED) ) return;

					final Object selected = comboAll.getSelectedItem();
					if(      aggregateSet.contains( selected ) ) ((AggregateFunction) selected ).run( definitions );
					else if( functionsSet.contains( selected ) ) listenSetFunction.itemStateChanged( event );
					else System.err.println( "warning: " + selected + " not recognized" );
				}
			} );

			comboAll.setToolTipText( "<html>define <font color='#00cc00'>all "+myThing+"s</font> as a single function" );

			return comboAll;
		}

		public          Component                        strut(                  JComponent pnl, GridBagConstraints c ){
			return strut( INT_SIZE_STRUT, pnl, c );
		}

		public          Component                        strut( int       width, JComponent pnl, GridBagConstraints c ){
			Component ret = DEBUG_STRUTS ? new JLabel( "s-" ) : Box.createHorizontalStrut( width );
			pnl.add(  ret, c );
			return ret;
		}

		public          Definition<D,S,F>             weighted( JComponent comp, JComponent pnl, GridBagConstraints c ){
			c.weightx = 1;
			c.fill    = GridBagConstraints.HORIZONTAL;
			pnl.add( comp, c );
			c.weightx = 0;
			c.fill    = GridBagConstraints.NONE;
			return Definition.this;
		}

		public Appendable append( Appendable buff ) throws IOException{
			return buff.append( stringify( myTarget ) )
			         //.append( " \u2245 " )
			           .append( " = " )
			           .append( (myComboBox == null) ? "?" : myComboBox.getSelectedItem().toString() );
		}

		/** @since 20071207 */
		protected JComboBox getComboBox( Definitions<D,S,F> definitions ){
			if( myComboBox == null ){
				myComboBox = newBox( definitions == null ? null : definitions.getListModel( myTarget ) );
			}
			return myComboBox;
		}

		/** @since 20091229 */
		class DefBox extends JComboBox{
			public DefBox(                     ){ super(       ); }
			public DefBox( ComboBoxModel model ){ super( model ); }
			public void updateUI(){
				DefBox.this.setRenderer( null );
				super.updateUI();
				ProbabilityRewrite.this.myStringification.decorate( this, Definition.this.functionsSet(), ProbabilityRewrite.this );
			}
		}

		protected JComboBox newBox( ComboBoxModel model ){
			JComboBox ret = (model == null) ? new DefBox() : new DefBox( model );
			ret.updateUI();
		  //ProbabilityRewrite.this.myStringification.decorate( ret, functionsSet(), ProbabilityRewrite.this );
			return ret;
		}

		public Object getDefinition(){
			return (myComboBox == null) ? getDefault() : myComboBox.getSelectedItem();
		}

		public Definition<D,S,F> setConstant( double value ){
			return Definition.this;
		}

		/** @since 20071219 */
		public Definition<D,S,F> die(){
			myTarget      = null;
			myComboBox    = null;
			myCheckBox    = null;
			myLabelTarget = null;
			myThing       = null;
			return this;
		}

		protected D                myTarget;
		protected JComboBox        myComboBox;
		protected JCheckBox        myCheckBox;
		protected JLabel           myLabelTarget;
		protected String           myThing;
	}

	public enum ConditionFunction{
		uniform( "uniform distribution", "Distribute probability values uniformly over the condition.  In other words, suppress all information about the condition." );

		private ConditionFunction( String display, String description ){
			this.display          = display;
			this.description      = description;
		}

		public String toString(){ return display; }

		public            final   String display, description;

		public   static   final   EnumSet<ConditionFunction> SET = EnumSet.allOf( ConditionFunction.class );
		private  static           String                     STR_PRECEDENCE, HTML_REFERENCE;

		public   static   final   String                     STR_SET = Arrays.toString( values() );

		public static String htmlReference(){
			if( HTML_REFERENCE != null ) return HTML_REFERENCE;

			ConditionFunction[] values = values();
			StringBuilder buff = new StringBuilder( values.length * 0x80 );
			buff.append( "<html><u>functions that control conditions</u>" ).append( HTML_LINEBREAK ).append( HTML_LINEBREAK );
			int index = 1;
			for( ConditionFunction conditionfunction : values ){
				buff.append( index++ ).append( ". <b>" ).append( conditionfunction.display ).append( "</b> - " ).append( conditionfunction.description ).append( HTML_LINEBREAK );
			}

			return HTML_REFERENCE = buff.toString();
		}
	}

	public static final Color COLOR_SPECIALS = new Color( 0xcc, 0x66, 0x0 );//Color.blue;

	public /*static*/ class DecoratedRenderer implements ListCellRenderer, ActionListener
	{
		public DecoratedRenderer( ListCellRenderer renderer, Set<?> specials ){
			this.renderer        = renderer;
			this.specials        = specials;
		}

		final public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus ){
			Component ret = renderer.getListCellRendererComponent( list, stringify( value ), index, isSelected, cellHasFocus );
			if( /*(! isSelected) &&*/ (specials != null) && specials.contains( value ) ) ret.setForeground( COLOR_SPECIALS );
			return ret;
		}

		public void actionPerformed( ActionEvent event ){
			if( specials == null ) return;
			Object source = event.getSource();
			if( source instanceof JComboBox ){
				JComboBox box   = (JComboBox) source;
				Color     color = specials.contains( box.getSelectedItem() ) ? COLOR_SPECIALS : Color.black;
				box.setForeground( color );
			}
		}

		/** @since 20071207 */
		public String stringify( Object value ){
			return ProbabilityRewrite.this.myStringification.stringify( value );
		}

		public ListCellRenderer renderer;
		public Set<?>           specials;
	}

	public enum Stringification implements Actionable<Stringification>, Semantic, Dendriform<Menu>
	{
		ids(    AbstractStringifier.VARIABLE_ID,    "<html>variable <font color='#cc6600'>id</font>,    i.e. 'short name'" ),//, getKeyStroke( KeyEvent.VK_I, InputEvent.CTRL_MASK ) ),
		labels( AbstractStringifier.VARIABLE_LABEL, "<html>variable <font color='#cc6600'>label</font>, i.e.  'long name'" );//, getKeyStroke( KeyEvent.VK_L, InputEvent.CTRL_MASK ) );

		private Stringification( VariableStringifier ifier, String tip ){
			myVariableStringifier = ifier;
			html                  = tip;

			this.properties.put(     display, name() );
			this.properties.put(     tooltip, html   );
			this.properties.put( accelerator, firstUniqueKeyStroke( name() ) );
		}

		public String stringify( Object obj ){
			return myVariableStringifier.objectToString( obj );
		}

		public Semantics semantics(){ return         exclusive; }

		public Menu         parent(){ return Menu.variableview; }

		public Stringification decorate( final JComboBox combo, final Set<?> specials, final ProbabilityRewrite reference ){
			final ListCellRenderer renderer   = combo.getRenderer();
			DecoratedRenderer      dr = null;
			if( renderer instanceof DecoratedRenderer ){
			  //(dr = (DecoratedRenderer)renderer).stringification = this;
			}
			else{
				combo.setRenderer( dr = reference.new DecoratedRenderer( renderer, specials ){

					public String stringify( Object value ){
						String ret = super.stringify( value );
						if( value instanceof FiniteVariable ){
						  	options.clear();
							if( ! Option.MODELS.selected( additive, reference.idmenus, options ).contains( Option.cardinalities ) ) return ret;

							int size = ((FiniteVariable) value).size();
							builder.setLength(0);
							if( size < 10 ) builder.append( ' ' );
							ret = builder.append( size ).append( " = | " ).append( ret ).append( " |" ).toString();
						}
						return ret;
					}
					private StringBuilder builder = new StringBuilder( 0x20 );
					private Set<Option>   options = EnumSet.noneOf( Option.class );
				} );
				boolean redundant = false;
				for( ActionListener actionlistener : combo.getActionListeners() ) if( actionlistener == dr ){ redundant = true; break; }
				if( ! redundant ) combo.addActionListener( dr );
			}
			dr.actionPerformed( new ActionEvent( combo, 0, "" ) );
			combo.repaint();
			return this;
		}

		/** @since 20071208 */
		public  Stringification     getDefault(                   ){ return labels; }
		public  Object                     get( Property property ){ return this.properties.get( property ); }
		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		/** @since 20071208 */
		public static AbstractButton[] buttons( View view, Object id, ItemListener listener ){
			return MODELS.newButtons( view, id, listener );
		}
		public static Stringification selected( Object id ){
			return MODELS.selected( exclusive, id );
		}
		public static Set<Stringification> selected( Object id, Set<Stringification> results ){
			return MODELS.selected(  additive, id, results);
		}
		public static EnumModels<Stringification> MODELS = new EnumModels<Stringification>( Stringification.class );

		public static JComponent radios( Object id, ItemListener listener ){
			JPanel             ret = new JPanel( new GridBagLayout() );
			GridBagConstraints c   = new GridBagConstraints();
			for( AbstractButton button : buttons( radio, id, listener ) ){
				ret.add( button, c );
			}
			return ret;
		}

		public static JMenu menuItems( JMenu menu, Object id, ItemListener listener ){
			for( AbstractButton button : buttons( menuradio, id, listener ) ){
				menu.add( button );
			}
			return menu;
		}

		public    final  VariableStringifier myVariableStringifier;
		public    final  String              html;
	}

	private Stringification myStringification = Stringification.values()[0].getDefault();

	public final ConditionDefinition INSTANCE_CONDITIONDEFINITION = new ConditionDefinition();

	public class ConditionDefinition extends Definition<FiniteVariable,FiniteVariable,ConditionFunction> implements Runnable
	{
		public ConditionDefinition( FiniteVariable condition ){
			super( condition, "condition" );
		}

		public ConditionDefinition(){ super( "condition" ); }

		public String         stringify( FiniteVariable obj ){
			return ProbabilityRewrite.this.myStringification.stringify( obj );
		}

		public Object         getDefault(){
			return uniform;
		}

		public Set<ConditionFunction>      functionsSet(){
			return ConditionFunction.SET;
		}

		public JComponent[]             toDisable(){
			return myToDisable;
		}

		/** @since 20071217 */
		public void run(){
			try{
				editValuesDefinition();
			}catch( Throwable throwable ){
				System.err.println( "warning: ConditionDefinition.run( editValuesDefinition ) caught " + throwable );
			}
		}

		/** @since 20071217 */
		public ConditionDefinition reText(){
			if( myLabelTarget != null ) myLabelTarget.setText( stringify( myTarget ) );
			return this;
		}

		public JLabel labelTarget(){
			HyperLabel hyperlabel = new HyperLabel( stringify( myTarget ), ConditionDefinition.this );
			hyperlabel.setFade( false );
			return myLabelTarget = hyperlabel;
		}

		public ConditionDefinition add( JComponent pnl, GridBagConstraints c, Definitions<FiniteVariable,FiniteVariable,ConditionFunction> definitions ){
			final JComboBox   combo = getComboBox( definitions );
			final JLabel  labelGets = new JLabel( "gets" ), labelTarget = this.labelTarget();
			labelTarget.setToolTipText( "<html><b>|</b> <font color='#0000cc'>"+stringify( myTarget )+"</font> <b>|</b> = <b>"+myTarget.size()+"</b>" );

			double weightx          = c.weightx;
			int    anchor           = c.anchor;
			int    fill             = c.fill;
			c.gridwidth             = 1;
			c.weightx               = 0;
			pnl.add( labelTarget, c );
			strut(           pnl, c );
			pnl.add(   labelGets, c );
			strut(           pnl, c );
			c.weightx               = 1;
			c.fill                  = GridBagConstraints.HORIZONTAL;
			weighted( combo, pnl, c );
			c.weightx               = 0;
			c.fill                  = GridBagConstraints.NONE;
			pnl.add(  myCheckBox, c );
			c.gridwidth             = GridBagConstraints.REMAINDER;
			c.anchor                = GridBagConstraints.EAST;
			pnl.add( Box.createGlue(), c );
			c.anchor                = anchor;
			c.weightx               = weightx;
			c.fill                  = fill;

			final ActionListener listener = new ActionListener(){
				public void actionPerformed( ActionEvent event ){
					try{
						boolean function = functionsSet().contains( combo.getSelectedItem() );
						myLabelTarget.setEnabled( ! function );
						if( function ) killEditPanel();
					}catch( Throwable thrown ){
						System.err.println( thrown );
					}
				}
			};
			listener.actionPerformed( new ActionEvent( combo, 0, "" ) );

			myToDisable = new JComponent[]{ labelGets };
			combo.setAction( definitions.getAction( myTarget, myToDisable ) );
			combo.addActionListener( listener );

			myCheckBox.setModel( definitions.getButtonModel( myTarget ) );
			if( ingredients != null ){ ingredients.add( Ingredient.locks, myCheckBox ); }

			return ConditionDefinition.this;
		}

		public ConditionDefinition header( JComponent pnl, GridBagConstraints c, FiniteVariable child ){
			final JComponent lCond  = new JLabel( "<html><b>condition"  ),
			                 lDef   = new JLabel( "<html><b>definition" ),
			                 lLock  = new JLabel( "<html><b>lock"       );

			lLock.setBorder( BORDER_FOR_BOXES );

			lCond.setToolTipText( "<html>one <font color='#cc6600'>condition</font> for each <font color='#00cc00'>parent</font> variable, last one for the <font color='#00cc00'>child</font>: \"<font color='#0000cc'>" + stringify( child ) + "</font>\"" );
			lDef .setToolTipText( "<html><font color='#ff00ff'>define</font> the condition by mapping each <font color='#00cc00'>destination</font> variable to a <font color='#00cc00'>source</font> variable or a <font color='#cc6600'>function</font>" );
			lLock.setToolTipText( "<html><font color='#cc0000'>lock</font> the condition's definition to <font color='#cc0000'>prevent it from changing automatically</font> in response to other changes you make" );

			c.anchor                = GridBagConstraints.WEST;
			c.gridwidth             = 1;
			pnl.add(      lCond, c );
			c.gridwidth             = 3;
			strut(          pnl, c );
			c.gridwidth             = 1;
			pnl.add(       lDef, c );
			pnl.add(      lLock, c );
			c.gridwidth             = GridBagConstraints.REMAINDER;
			pnl.add( Box.createGlue(), c );

			pnl.add( Box.createVerticalStrut( INT_SIZE_STRUT ), c );

			if( ingredients != null ){ ingredients.add( Ingredient.locks, lLock ); }

			return ConditionDefinition.this;
		}

		public ConditionDefinition footer( final JComponent pnl, GridBagConstraints c, final Definitions<FiniteVariable,FiniteVariable,ConditionFunction> definitions ){
			JLabel     label  = this.labelAll();
			JComboBox  combo  = this.comboAll( definitions );
			JCheckBox  cb     = this.   cbAll( definitions );
			JComponent comp   = new JLabel( "view:" );
			comp.setToolTipText( "<html><font color='#ff00ff'>switch</font> the way this window shows <font color='#cc6600'>variable names</font>" );

			c.anchor                = GridBagConstraints.WEST;
			c.gridwidth             = GridBagConstraints.REMAINDER;
			Component     vstrut    = Box.createVerticalStrut( INT_SIZE_STRUT );
			pnl.add(      vstrut, c );
			c.gridwidth             = 1;
			pnl.add(       label, c );
			c.gridwidth             = 3;
			strut(           pnl, c );
			c.gridwidth             = 1;
			weighted( combo, pnl, c );
			pnl.add(          cb, c );
			c.gridwidth             = GridBagConstraints.REMAINDER;
			c.anchor                = GridBagConstraints.EAST;
			pnl.add( Box.createGlue(), c );
			c.gridwidth             = 1;
			c.anchor                = GridBagConstraints.WEST;
			pnl.add( ingredients.add( Ingredient.buttons, comp ), c );
			strut(           pnl, c );
			c.gridwidth             = GridBagConstraints.REMAINDER;
			pnl.add( ingredients.consummate( Ingredient.buttons, Stringification.radios( idmenus, null ) ), c );

			Component proxylocks    = Box.createGlue(), proxyconve = Box.createGlue();
			c.gridwidth             = GridBagConstraints.RELATIVE;
			pnl.add(  proxylocks, c );
			c.gridwidth             = GridBagConstraints.REMAINDER;
			pnl.add(  proxyconve, c );

			new Proxy( new Component[]{ proxylocks, proxyconve }, visibility, and, new Component[]{ cb }, visibility );

			if( ingredients != null ){
				ingredients.consummate( Ingredient.locks,        proxylocks );//cb );
				ingredients       .add( Ingredient.conveniences, proxyconve );
				ingredients       .add( Ingredient.conveniences,     vstrut );
				ingredients       .add( Ingredient.conveniences,      label );
				ingredients.consummate( Ingredient.conveniences,      combo );
			}

			return ConditionDefinition.this;
		}

		public ValuesDefinition getValuesDefinition(){
			Object definition = ConditionDefinition.this.getDefinition();
			if( functionsSet().contains( definition ) ) return null;//throw new RuntimeException();
			return (( myValuesDefinition == null )  ? (myValuesDefinition = new ValuesDefinition()) : myValuesDefinition)
			.populateValues( (FiniteVariable) definition, myTarget );
		}

		public ConditionDefinition editValuesDefinition(){
			ProbabilityRewrite.this.setValuesPanel( getValuesDefinition().getPanel(), false, true );
			return ConditionDefinition.this;
		}

		/** @since 20071207 */
		public ConditionDefinition killEditPanel(){
			if( myValuesDefinition == null ) return ConditionDefinition.this;
			ProbabilityRewrite.this.setValuesPanel( myValuesDefinition.getPanel(), true, true );
			return ConditionDefinition.this;
		}

		/** @since 20071219 */
		public ConditionDefinition die(){
			if( myValuesDefinition != null ){ myValuesDefinition.die(); }
			myValuesDefinition      = null;
			myToDisable             = null;
			super.die();
			return this;
		}

		private    JComponent[]        myToDisable;
		private    ValuesDefinition    myValuesDefinition;
	}

	/** @since 20071206 */
	public static Container deeplyInvalid( Container parent ){
		try{
			for( Component child : parent.getComponents() ){
				if( child instanceof Container ) deeplyInvalid( (Container) child );

				if( child instanceof JComponent ) ((JComponent) child).revalidate();//repaint();//
				else child.invalidate();
				//child.repaint();
			}
			if( parent instanceof JComponent ) ((JComponent) parent).repaint();//revalidate();
			if( DEBUG_DEEPLY && parent instanceof JLabel ) System.out.println( "deeply " + ((JLabel) parent).getText() );
			//parent.repaint();
		}catch( Throwable thrown ){
			System.err.println( "warning: deeplyInvalid() caught " + thrown );
		}
		return parent;
	}

	@SuppressWarnings( "unchecked" )
	private void populateConditions( final BeliefNetwork bn, final FiniteVariable src, final FiniteVariable dest ) throws Exception{
		final JPanel       pnl = myPanelConditions;

		pnl.removeAll();
		GridBagConstraints c   = new GridBagConstraints();
		INSTANCE_CONDITIONDEFINITION.header( pnl, c, dest );

		TableIndex indexDestination    = dest.getCPTShell().index();
		TableIndex indexSource         =  src.getCPTShell().index();

		Set<ConditionFunction> domain_src_shared = EnumSet.allOf( ConditionFunction.class );
		domain_src_shared.removeAll( caste().blacklist() );

		myConditionDef                 = (new Definitions<FiniteVariable,FiniteVariable,ConditionFunction>( indexDestination.variables(), domain_src_shared, indexSource.variables(), ConditionFunction.uniform ){
			public Kind characterize( Object element ){
				if(      element instanceof FiniteVariable    ) return Kind.exclusive;
				else if( element instanceof ConditionFunction ) return Kind.shared;
				else                                            return Kind.undefined;
			}

			/** @since 20071217 */
			private String label( FiniteVariable fvar ){
				if( fvar instanceof StandardNode ){ return ((StandardNode)fvar).getLabel(); }
				else{                               return                fvar    .getID(); }
			}

			/** @since 20071207 */
			public          boolean        auditDefinition( FiniteVariable dest, Object src ){
			  //System.out.println( "myConditionDef.auditDefinition( "+myStringification.stringify( dest )+", "+myStringification.stringify( src )+" )" );
				try{
					if( caste().afunctional() && (src instanceof FiniteVariable) ){
						FiniteVariable fvsrc = (FiniteVariable) src;
						if( fvsrc.size() != dest.size() ){
							StringBuilder buff = new StringBuilder( 0x40 )
							.append( "<html>Sorry, you cannot define destination condition '<font color='#00cc00'><b>" )
							.append( myStringification.stringify(  dest ) )
							.append( "</b></font>' as '<font color='#cc0000'><b>" )
							.append( myStringification.stringify( fvsrc ) )
							.append( "</b></font>' because these two variables have <font color='#ff0000'>unequal cardinalities (" )
							.append( fvsrc.size() )
							.append( " \u2260 " )
							.append(  dest.size() )
							.append( ")</font> and you are running <font color='#000099'>user level '<b>" )
							.append( caste().toString() )
							.append( "</b>'</font>, which does not support generating cpt parameters using <font color='#cc6600'>functions</font>. " )
							.append( "Please consider a different definition or switch to one of the <font color='#000099'>user levels</font> that support <font color='#cc6600'>functions</font>: <b>" )
							.append( Caste.functional().toString() )
							.append( "</b>." );
							Object msg = new LabelConstrained( buff.toString() ).setMaximumWidth( 0x100 );
							JOptionPane.showMessageDialog( pnl, msg, "unsupported assignment", JOptionPane.ERROR_MESSAGE );
							return false;
						}
					}
				}catch( Throwable thrown ){
					System.err.println( "warning: myConditionDef.auditDefinition() caught " + thrown );
					return false;
				}
				return true;
			}

			/** @since 20071207 */
			public          GuessQuality        auditGuess( FiniteVariable destination, FiniteVariable guess ){
				if(         guess              ==   src        ) return destination == dest ? excellent : prohibited;
				else if(    destination        ==  dest        ) return guess       ==  src ? excellent : prohibited;
				else if(    destination        == guess        ) return good;
				else if(    destination.size() == guess.size() ){
					 if( label( destination ).equals( label( guess ) ) ){ return fair;     }
					 else{                                                return mediocre; }
				}
				else{                                                     return poor;     }
			  //return prohibited;
			}
		}).guess();//.setLockedAll( true, -1 );

		if( ingredients.models.selected( additive, idmenus, EnumSet.noneOf( Ingredient.class ) ).contains( Ingredient.locks ) ){ myConditionDef.setLockedAll( true, -1 ); }

		myConditionVariables           = (FiniteVariable[]) indexDestination.variables().toArray( new FiniteVariable[ indexDestination.getNumVariables() ] );
		myConditionDefinitions         = new HashMap<FiniteVariable,ConditionDefinition>( myConditionVariables.length );
		int                 index      = 0;
		for( FiniteVariable condition : myConditionVariables ){
			myConditionDefinitions.put( condition, new ConditionDefinition( condition ).add( pnl, c, myConditionDef ) );
		}

		INSTANCE_CONDITIONDEFINITION.footer( pnl, c, myConditionDef );
	}

	@SuppressWarnings( "unchecked" )
	public static Set<FiniteVariable> intersectionOfConditions( FiniteVariable src, FiniteVariable dest ){
		TableIndex indexDest = dest.getCPTShell().index(), indexSource = src.getCPTShell().index();

		Set<FiniteVariable> intersection = new HashSet<FiniteVariable>( (Collection<FiniteVariable>) indexDest.variables() );
		intersection.remove(    indexDest  .getJoint() );
		intersection.retainAll( indexSource.variables() );
		intersection.remove(    indexSource.getJoint() );
		return intersection;
	}

	public enum ValueFunction{
		skip       (                 "p = p",               "Skip the target parameter, i.e. don't change the existing value - leave it alone.  Useful for updating a cpt in multiple steps, or updating only part of a cpt.", "parameter was skipped (original value undisturbed)", new Color( 0xff, 0xcc, 0xcc ) ),
		constant   ( true, "\u2261", "p = c",               "Force the target parameter to a constant probability value you choose.", "parameter was set to constant value", new Color( 0xcc, 0xff, 0xcc ) ){
			public   String   toString(             double argument ){
				return Double.toString( argument );
			}

			public   double      value( double src, double argument ){
				return argument;
			}
		},
		zero       (                 "p = 0",               "Force the target parameter to zero probability.", "parameter was set to zero", new Color( 0xff, 0xff, 0xcc ) ){
			public   double      value( double src, double argument ){
				return 0;
			}
		},
		complement ( true, "x",      "p = (1.0 - sum) x c", "Take the sum of all non-complement values, then set the target parameter to the complement of that sum, multiplied by the constant you specify. By default, the constant multiplier is the inverse of the number of complement parameters, so the 'remainder' probability is distributed uniformly over all complement parameters.", "parameter was set to the complement multiplied by a factor", new Color( 0xcc, 0xff, 0xff ) ){
			public   String   toString(             double argument ){
				return this.name() + " " + this.operator + " " + Double.toString( argument );
			}

			public   double      value( double src, double argument ){
				return 0;
			}
		};

		/** @since 20071209 */
		public   String   toString(             double argument ){
			return this.name();
		}

		/** @since 20071210 */
		public   double      value( double src, double argument ){
			return src;
		}

		private ValueFunction( String equation, String reference, String postmortem, Color stain ){ this( false, "", equation, reference, postmortem, stain ); }

		private ValueFunction( boolean usesconstant, String operator, String equation, String reference, String postmortem, Color stain ){
			this.usesconstant     = usesconstant;
			this.operator         = operator;
			this.equation         = equation;
			this.reference        = reference;
			this.postmortem       = postmortem;
			this.stain            = new Stain( name(), stain, Color.black, postmortem );
		}

		public   final  boolean   usesconstant;
		public   final  String    operator;
		public   final  String    equation;
		public   final  String    reference;
		public   final  String    postmortem;
		public   final  Stain     stain;

		public   static final EnumSet<ValueFunction>     SET              = EnumSet.allOf( ValueFunction.class );
		private  static       EnumSet<ValueFunction>     SET_USE_CONSTANT;
		public   static final String                 STR_SET              = Arrays.toString( values() ),
		                                             STR_PRECEDENCE_SEP   = " > ";
		private  static       String                 STR_PRECEDENCE, HTML_REFERENCE;

		public static EnumSet<ValueFunction> setUseConstant(){
			if( SET_USE_CONSTANT == null ){
				SET_USE_CONSTANT = EnumSet.noneOf( ValueFunction.class );
				for( ValueFunction valuefunction : values() ) if( valuefunction.usesconstant ) SET_USE_CONSTANT.add( valuefunction );
			}
			return SET_USE_CONSTANT;
		}

		public static String describePrecedence(){
			if( STR_PRECEDENCE == null ){
				ValueFunction[] values = values();
				StringBuilder buff = new StringBuilder( values.length * 0x10 );
				buff.append( "<b>order of precedence</b> (strongest to weakest):" ).append( HTML_LINEBREAK ).append( "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" );
				for( ValueFunction valuefunction : values ) buff.append( valuefunction.name() ).append( STR_PRECEDENCE_SEP );
				buff.setLength( buff.length() - STR_PRECEDENCE_SEP.length() );
				STR_PRECEDENCE = buff.toString();
			}
			return STR_PRECEDENCE;
		}

		public static String htmlReference(){
			if( HTML_REFERENCE != null ) return HTML_REFERENCE;

			ValueFunction[] values = values();
			StringBuilder buff = new StringBuilder( values.length * 0x80 );
			buff.append( "<html><u>functions that control values</u>" ).append( HTML_LINEBREAK ).append( HTML_LINEBREAK );
			int index = 1;
			String strArgument;
			for( ValueFunction valuefunction : values ){
				strArgument = valuefunction.usesconstant ? "(takes argument)" : "(takes no argument)";
				buff.append( index++ ).append( ". <b>" ).append( valuefunction.name() ).append( "</b> - <font color='#0000cc'>" ).append( valuefunction.equation ).append( "</font>, " ).append( valuefunction.reference ).append( " " ).append( strArgument ).append( HTML_LINEBREAK );
			}
			buff.append( HTML_LINEBREAK ).append( describePrecedence() ).append( HTML_LINEBREAK );

			return HTML_REFERENCE = buff.toString();
		}

		/** @since 20071210 */
		public static Stain[] stains(){
			if( STAINS == null ){
				ValueFunction[] values = values();
				STAINS = new Stain[ values.length ];
				for( int i=0; i<values.length; i++ ) STAINS[i] = values[i].stain;
			}
			return STAINS;
		}
		private static Stain[] STAINS;
	}

	public final ValueDefinition INSTANCE_VALUEDEFINITION = new ValueDefinition();

	public class ValueDefinition extends Definition<Object,Object,ValueFunction> implements Runnable
	{
		public ValueDefinition( FiniteVariable destination, Object value, FiniteVariable source, Definitions<Object,Object,ValueFunction> definitions ){
			super( value, "value" );

			myDestination   = destination;
			mySource        = source;
			myDefinitions   = definitions;

			myDecimalField  = new DecimalField( 1, 3, 0.0, 1.0 );
			myDecimalField.setEnabled( false );
			myDecimalField.setVisible( false );

			myLabelOperator = new JLabel();
		}

		public ValueDefinition(){ super( "value" ); }

		public String         stringify( Object obj ){
			return ProbabilityRewrite.this.myStringification.stringify( obj );
		}

		public Object         getDefault(){
			return zero;
		}

		public Set<ValueFunction>    functionsSet(){
			return ValueFunction.SET;
		}

		public JComponent[]             toDisable(){
			return myToDisable;
		}

		public ValueDefinition add( JComponent pnl, GridBagConstraints c, Definitions<Object,Object,ValueFunction> definitions ){
			final JComboBox   combo = getComboBox( definitions );
			final JLabel  labelGets = new JLabel( "gets" ), labelTarget = this.labelTarget();
			final JPanel      pnlDF = new JPanel( new BorderLayout() );
			pnlDF.add( myDecimalField );
			labelTarget.setToolTipText( "<html>\"<font color='#0000cc'>"+stringify( myDestination )+"</font>\" = <font color='#cc6600'>"+stringify( myTarget ) );

			double weightx          = c.weightx;
			int    anchor           = c.anchor;
			int    fill             = c.fill;
			c.gridwidth             = 1;
			pnl.add(     labelTarget, c );
			strut(               pnl, c );
			pnl.add(       labelGets, c );
			strut(               pnl, c );
			c.weightx               = 1;
			c.fill                  = GridBagConstraints.HORIZONTAL;
			weighted(     combo, pnl, c );
			c.weightx               = 0;
			c.fill                  = GridBagConstraints.NONE;
			pnl.add( myLabelOperator, c );
			weighted(     pnlDF, pnl, c );
			pnl.add(      myCheckBox, c );
			c.gridwidth             = GridBagConstraints.REMAINDER;
			c.anchor                = GridBagConstraints.EAST;
			pnl.add( Box.createGlue(), c );
			c.anchor                = anchor;
			c.weightx               = weightx;
			c.fill                  = fill;

			myToDisable = new JComponent[]{ labelGets, myLabelOperator, myDecimalField };
			combo.setAction( definitions.getAction( myTarget, myToDisable ) );
			definitions.getListModel( myTarget ).addListDataListener( new ListDataAdapter( stringify( myTarget ) ){
				public ListDataAdapter reactImpl(){ ValueDefinition.this.refresh(); return this; }
			} );

			myCheckBox.setModel( definitions.getButtonModel( myTarget ) );

			if( ingredients != null ){
				ingredients.add( Ingredient.locks,     myCheckBox      );
				ingredients.add( Ingredient.arguments, myLabelOperator );
				ingredients.add( Ingredient.arguments, pnlDF           );
			}

			ValueDefinition.this.refresh();

			return ValueDefinition.this;
		}

		private JComponent[] myToDisable;

		public ValueDefinition header( JComponent pnl, GridBagConstraints c, FiniteVariable dest ){
			final JComponent lValue = new JLabel( "<html><b>value"      ),
			                 lDef   = new JLabel( "<html><b>definition" ),
			                 lConst = new JLabel( "<html><b>constant"   ),
			                 lLock  = new JLabel( "<html><b>lock"       );

			lLock .setBorder( BORDER_FOR_BOXES );

			lValue.setToolTipText( "<html>each row corresponds to the condition when \"<font color='#0000cc'>"+stringify( dest )+"</font>\" takes on the <font color='#cc6600'>value</font> identified by this column" );
			lDef  .setToolTipText( "<html><font color='#ff00ff'>define</font> a probability value by mapping each <font color='#00cc00'>destination</font> value to a <font color='#00cc00'>source</font> value or a <font color='#cc6600'>function</font>" );
			lConst.setToolTipText( "<html>"+ValueFunction.setUseConstant().size()+" of the "+ValueFunction.values().length+" value functions take a constant argument: " + ValueFunction.setUseConstant().toString() );
			lLock .setToolTipText( "<html><font color='#cc0000'>lock</font> a value's definition to <font color='#cc0000'>prevent it from changing automatically</font> in response to other changes you make" );

			c.anchor                = GridBagConstraints.WEST;
			c.gridwidth             = 1;
			pnl.add(     lValue, c );
			c.gridwidth            = 3;
			strut(          pnl, c );
			c.gridwidth             = 1;
			pnl.add(       lDef, c );
			Component strut =
			strut(    0x10, pnl, c );
			pnl.add(     lConst, c );
			pnl.add(      lLock, c );
			c.gridwidth             = GridBagConstraints.REMAINDER;
			pnl.add( Box.createGlue(), c );

			pnl.add( Box.createVerticalStrut( INT_SIZE_STRUT ), c );

			if( ingredients != null ){
				ingredients.add( Ingredient.locks,     lLock  );
				ingredients.add( Ingredient.arguments, strut  );
				ingredients.add( Ingredient.arguments, lConst );
			}

			return ValueDefinition.this;
		}

		public ValueDefinition footer( JComponent pnl, GridBagConstraints c, Definitions<Object,Object,ValueFunction> definitions ){
			final JLabel     label = this.labelAll();
			final JComboBox  combo = this.comboAll( definitions );
			final JCheckBox  cb    = this.   cbAll( definitions );
			Component        strut;

			c.anchor               = GridBagConstraints.WEST;
			c.gridwidth            = GridBagConstraints.REMAINDER;
			Component     vstrut   = Box.createVerticalStrut( INT_SIZE_STRUT );
			pnl.add(      vstrut, c );
			c.gridwidth            = 1;
			pnl.add(       label, c );
			c.gridwidth            = 3;
			strut(           pnl, c );
			c.gridwidth            = 1;
			weighted( combo, pnl, c );
			c.gridwidth            = 2;
			strut =
			strut(           pnl, c );
			c.gridwidth            = 1;
			pnl.add(          cb, c );
			c.gridwidth            = GridBagConstraints.REMAINDER;
			c.anchor               = GridBagConstraints.EAST;
			pnl.add( Box.createGlue(), c );

			Component proxylocks    = Box.createGlue(), proxyconve = Box.createGlue();
			c.gridwidth             = GridBagConstraints.RELATIVE;
			pnl.add(  proxylocks, c );
			c.gridwidth             = GridBagConstraints.REMAINDER;
			pnl.add(  proxyconve, c );

			new Proxy( new Component[]{ proxylocks, proxyconve }, visibility, and, new Component[]{ cb }, visibility );

			if( ingredients != null ){
				ingredients.consummate( Ingredient.locks,        proxylocks );//cb );
				ingredients       .add( Ingredient.conveniences, proxyconve );
				ingredients       .add( Ingredient.conveniences,     vstrut );
				ingredients       .add( Ingredient.conveniences,      label );
				ingredients.consummate( Ingredient.conveniences,      combo );
				ingredients.consummate( Ingredient.arguments,         strut );
			}

			return ValueDefinition.this;
		}

		public boolean refresh()
		{
			boolean enabled = myDefinitions.isLocked( myTarget );
			if( myDecimalField == null ) return enabled;

			Object              item   = myDefinitions.get( myTarget );
			boolean       isFunction   = functionsSet().contains( item );
			flagVisibilityFinal        = (! isFunction) || ValueFunction.setUseConstant().contains( item );
			String              text   =    isFunction ? (" " + ((ValueFunction)item).operator + " ") : " x ";
			double            weight   =    isFunction ? -1 : 1;

			myLabelOperator.setText( text );
			if( weight >= 0 ) ValueDefinition.this.setConstant( weight );
			if( flagVisibilityFinal != myDecimalField.isVisible() ){
				ProbabilityRewrite.this.run( this, true );
			}
			return enabled;
		}

		private boolean flagVisibilityFinal;

		/** @since 20071217 */
		public void run(){
	//		yield();
			if(           mySynchDecimalField == null ){ return; }
			synchronized( mySynchDecimalField ){
				try{
					myDecimalField.setVisible( flagVisibilityFinal );
					Container parent = myDecimalField.getParent();
					if( parent != null ) parent.setPreferredSize( flagVisibilityFinal ? null : new Dimension( 0,0 ) );

					ProbabilityRewrite.this.pack( LONG_PACK_DELAY );
				}catch( Throwable thrown ){
					System.err.println( "warning: ProbabilityRewrite.ValueDefinition.refresh() thread caught " + thrown );
				}
			}
		}

		public ValueDefinition setConstant( final double value ){
			if( (myDecimalField != null) && (myDecimalField.getValue() != value) ){
				new Thread( new Runnable(){
					public void run(){
						synchronized( mySynchDecimalField ){
							try{
								sleep( 0x20 );
								myDecimalField.setValue( value );
								myDecimalField.setScrollOffset( 0 );
							}catch( Throwable thrown ){
								System.err.println( "warning: ProbabilityRewrite.ValueDefinition.setConstant() thread caught " + thrown );
							}
						}
					}
				} ).start();
			}
			return ValueDefinition.this;
		}

		/** @since 20071209 */
		public double getConstant(){
			return myDecimalField == null ? 1.0 : myDecimalField.getValue();
		}

		/** @since 20071219 */
		public ValueDefinition die(){
			myDestination       = mySource = null;
			myDecimalField      = null;
			myLabelOperator     = null;
			mySynchDecimalField = null;
			myDefinitions       = null;
			super.die();
			return this;
		}

		private    FiniteVariable                             myDestination, mySource;

		private    DecimalField                               myDecimalField;
		private    JLabel                                     myLabelOperator;
		private    Object                                     mySynchDecimalField = new Object();
		private    Definitions<Object,Object,ValueFunction>   myDefinitions;
	}

	/** @since 20071204 */
	public class ValuesDefinition implements MoreInformation<Object,Object,ValueFunction>
	{
		@SuppressWarnings( "unchecked" )
		public ValuesDefinition populateValues( final FiniteVariable src, final FiniteVariable dest )
		{
			JPanel pnl         = (myPanelValues == null) ? myPanelValues = new JPanel( new GridBagLayout() ) : myPanelValues;

			this.myDestination = dest;
			this.mySource      = src;

			List<Throwable> thrown = new LinkedList<Throwable>();
			try{
				pnl.removeAll();
				GridBagConstraints c    = new GridBagConstraints();
				INSTANCE_VALUEDEFINITION.header( pnl, c, dest );

				if( myValueDef == null ){
					Set<ValueFunction> domain_src_shared = EnumSet.allOf( ValueFunction.class );
					domain_src_shared.removeAll( caste().blacklist() );

					final ValueDefinition[] vdarray = new ValueDefinition[ dest.size() ];
					myValueDefinitions  = new HashMap<Object,ValueDefinition>( dest.size() );
					myValueDef          = (new Definitions<Object,Object,ValueFunction>( dest.instances(), domain_src_shared, src.instances(), ValueFunction.zero ){
						public Kind characterize( Object element ){
							if(      element instanceof ValueFunction ) return Kind.shared;
							else if( element instanceof Object        ) return Kind.exclusive;
							else                                        return Kind.undefined;
						}

						public Definitions<Object,Object,ValueFunction> postGuess( Map<Object,Object> guessed, Collection<Object> dest_remaining, Collection<Object> src_remaining ){
							int num_dest_leftover = dest_remaining.size();
							int num__src_leftover =  src_remaining.size();

							if(      num_dest_leftover == 0 && num__src_leftover == 0 ){
								//do nothing
							}
							else if( num_dest_leftover == 0 && num__src_leftover  > 0 ){
								define( dest.instance( dest.size() - 1 ), complement );
							}
							else if( num_dest_leftover  > 0 && num__src_leftover == 0 ){
								return super.postGuess( guessed, dest_remaining, src_remaining );
							}
							else System.err.println( "warning: impossible value guess situation" );

							return this;
						}

						public Definitions<Object,Object,ValueFunction> fireContentsChanged(){
							myGroupRunUpdateConstants.interrupt();

							new Thread( myGroupRunUpdateConstants, myRunUpdateConstants, "update constants " + myThreadCounter++ ).start();

							return super.fireContentsChanged();
						}

						private Runnable myRunUpdateConstants = new Runnable(){
							public void run(){
								int count = 0;
								try{
									Thread.sleep( 0x100 );

									synchronized( complementary ){
										complementary.clear();
										for( ValueDefinition definition : vdarray ){
											if( (definition != null) && (get( definition.myTarget ) == complement) ) complementary.add( definition );
										}
										count = complementary.size();

										if( Thread.interrupted() ) return;

										if( (! complementary.isEmpty()) && (myCountComplementary != count)){
											double fraction = ((double)1.0) / ((double)count);
											for( Definition<Object,Object,ValueFunction> definition : complementary ){
												definition.setConstant( fraction );

												if( Thread.interrupted() ) return;
											}
										}
									}
								}
								catch( InterruptedException interruptedexception ){}
								catch( Throwable thrown ){
									System.err.println( "warning: value defs.myRunUpdateConstants.run() caught " + thrown );
								}finally{
									myCountComplementary = count;
								}
							}
						};

						private int                                                 myThreadCounter = 1;
						private ThreadGroup                                         myGroupRunUpdateConstants = new ThreadGroup( "value defs run update consts" );
						private LinkedList<Definition<Object,Object,ValueFunction>> complementary = new LinkedList<Definition<Object,Object,ValueFunction>>();
						private int                                                 myCountComplementary   = 0;
						private Object[]                                            myDestinationInstances = dest.instances().toArray();
					}).guess();//.setLockedAll( true, -1 );

					if( ingredients.models.selected( additive, idmenus, EnumSet.noneOf( Ingredient.class ) ).contains( Ingredient.locks ) ){ myValueDef.setLockedAll( true, -1 ); }

					int               index = 0;
					for( Object instance : dest.instances() ){
						myValueDefinitions.put( instance, vdarray[ index++ ] = new ValueDefinition( dest, instance, src, myValueDef ) );
					}
				}

				for( Object instance : dest.instances() ){ myValueDefinitions.get( instance ).add( pnl, c, myValueDef ); }

				INSTANCE_VALUEDEFINITION.footer( pnl, c, myValueDef );
			}
			catch( Throwable throwable ){ thrown.add( throwable ); }

			try{ pnl.setBorder( createTitledBorder( createEtchedBorder(), "define values { " + myStringification.stringify( dest ) + " <- " + myStringification.stringify( src ) + " }" ) ); }
			catch( Throwable throwable ){ thrown.add( throwable ); }

			try{ writeHint( src, dest ); }
			catch( Throwable throwable ){ thrown.add( throwable ); }

			reportErrors( thrown, "ValuesDefinition.populateValues()" );

			return ValuesDefinition.this;
		}

		public JComponent  getPanel(){
			if( myPanelValues == null ) return null;

			if( myContainer == null ){
				JPanel             pnl = new JPanel( new GridBagLayout() );
				GridBagConstraints c   = new GridBagConstraints();
				pnl.setName( myStringification.stringify(myDestination)+" values container" );

				c.gridwidth = 1;
				c.weightx   = 0;

				Component button;
				pnl.add( button = nilla( Ingredient.hints, ingredients, true ), c );//ingredients.consummate( Ingredient.buttons, ingredientNilla( Ingredient.hints ) ), c );

				c.gridwidth = GridBagConstraints.REMAINDER;
				c.weightx   = 1;
				pnl.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );
				c.weightx   = 0;
				pnl.add( ingredients.consummate( Ingredient.hints, myHint ), c );

				myContainer = new JPanel( new BorderLayout() );
				myContainer.add(    myPanelValues,     BorderLayout.CENTER );
				myContainer.add(              pnl,     BorderLayout.SOUTH  );

				if( DEBUG_BORDERS ) myContainer.setBorder( BorderFactory.createLineBorder( Color.green, 1 ) );
			}

			return myContainer;
		}

		public ValuesDefinition writeHint( FiniteVariable src, FiniteVariable dest ){
			int      srcCard         =  src.size();
			int     destCard         = dest.size();
			int     difference       = Math.abs( destCard - srcCard );
			int     destCardMinusOne = destCard - 1;

			String strDiffGrammaticalNumberSuffix   = difference>1 ? "s" : "";
			String htmlSrc     = "\"<b>" + myStringification.stringify(  src ) + "</b>\"";
			String htmlDest    = "\"<b>" + myStringification.stringify( dest ) + "</b>\"";
			String  strSrcCard = "the cardinality of source      variable " + htmlSrc;
			String strDestCard = "the cardinality of destination variable " + htmlDest;
			String strDestCardMinusOneValues = (destCardMinusOne>1) ? (destCardMinusOne + " values") : "value";
			String strLastValueDestination   = dest.instance( destCardMinusOne ).toString();

			(( myInterpolation == null ) ? (myInterpolation = new Interpolation()) : myInterpolation)
			.map( "srcCard",                        srcCard )
			.map( "destCard",                       destCard )
			.map( "difference",                     difference )
			.map( "destCardMinusOne",               destCardMinusOne )
			.map( "strDiffGrammaticalNumberSuffix", strDiffGrammaticalNumberSuffix )
			.map( "htmlSrc",                        htmlSrc )
			.map( "htmlDest",                       htmlDest )
			.map( "strSrcCard",                     strSrcCard )
			.map( "strDestCard",                    strDestCard )
			.map( "strDestCardMinusOneValues",      strDestCardMinusOneValues )
			.map( "strLastValueDestination",        strLastValueDestination )
			.map( "ValueFunction.STR_SET",          ValueFunction.STR_SET );

			StringBuilder buff = myInterpolation.newHint();
			if( srcCard == destCard ){
				buff.append( "${strDestCard} is the same as ${strSrcCard}, i.e. ${destCard} \u2261 ${srcCard}. By default, for each condition, we write values into the destination cpt exactly as they occur in the source cpt for that condition." );
			}
			else if( destCard > srcCard ){
				buff.append( "${strDestCard} is larger than ${strSrcCard}, i.e. ${destCard} > ${srcCard}. ${htmlDest} has ${difference} more state${strDiffGrammaticalNumberSuffix} than ${htmlSrc}. That means we must define at least ${difference} value${strDiffGrammaticalNumberSuffix} using one or more of the functions ${ValueFunction.STR_SET}. By default, for each condition, we write the first ${srcCard} values from the source cpt to the first ${srcCard} values of the destination cpt, " );
				if( difference > 1 ) buff
					.append( "then write zero probability for each of the last ${difference} states." );
				else buff
					.append( "and assign zero probability to the last state." );
			}
			else if( destCard < srcCard ){
				buff.append( "${strSrcCard} is larger than ${strDestCard}, i.e. ${srcCard} > ${destCard}. That means we must disregard at least ${difference} value${strDiffGrammaticalNumberSuffix}.  By default, for each condition, we write the first ${strDestCardMinusOneValues} from the source cpt to the first ${strDestCardMinusOneValues} of the destination cpt, then assign all remaining probability (the complement) to the last value (\"${strLastValueDestination}\")." );
			}

			myHint.setText( myInterpolation.hint() );

			return ValuesDefinition.this;
		}

		/** @since 20071209 */
		public    Dest       append( Dest out, Format format, Stringifier ifier ){
			return myValueDef.append( out, format, ifier, this );
		}

		/** @since 20071209 */
		public Definitions<Object,Object,ValueFunction> getDefinitions(){
			return myValueDef;
		}

		/** @since 20071209 */
		public    boolean      none( Object destination ){
			return true;
		}

		/** @since 20071209 */
		public    Dest       append( Dest out, Format format, Stringifier ifier, Object destination ){
			throw new UnsupportedOperationException();
		}

		/** @since 20071209 */
		public    Dest     separate( Dest out, Format format ){
			return out;
		}

		/** @since 20071209 */
		public    Dest       prefix( Dest out, int   ordinal ){
			return out;
		}

		/** @since 20071209 */
		public    String    specify( ValueFunction function, Object instance ){
			try{
				return function.toString( myValueDefinitions.get( instance ).getConstant() );
			}catch( Throwable thrown ){
				System.err.println( "warning: ValuesDefinition.specify() caught " + thrown );
			}
			return function.toString();
		}

		/** @since 20071210 */
		public ValueDefinition getValueDefinition( Object instance ){
			return myValueDefinitions == null ? null : myValueDefinitions.get( instance );
		}

		/** @since 20071210 */
		public Object[] asArrayOfObject(){
			Object[] ret = new Object[ myDestination.size() ];
			Object definition;
			for( int i=0; i<ret.length; i++ ){
				definition = myValueDef.get( myDestination.instance( i ) );
				ret[i] = definition == null ? myValueDef.getDefault() : definition;
			}
			return ret;
		}

		/** @since 20071210 */
		public ValueDefinition[] asArrayOfValueDefinition(){
			ValueDefinition[] ret = new ValueDefinition[ myDestination.size() ];
			for( int i=0; i<ret.length; i++ ){
				ret[i] = myValueDefinitions.get( myDestination.instance( i ) );
			}
			return ret;
		}

		/** @since 20071219 */
		public ValuesDefinition die(){
			myPanelValues           = myContainer = null;
			if( myHint             != null ){ myHint.setText( "clear" ); }
			myHint                  = null;
			myValueDef              = null;
			if( myValueDefinitions != null ){
				for( ValueDefinition vd : myValueDefinitions.values() ){ vd.die(); }
				myValueDefinitions.clear();
			}
			myValueDefinitions      = null;
			myDestination           = mySource = null;
			if( myInterpolation    != null ){ myInterpolation.die(); }
			myInterpolation         = null;
			return this;
		}

		private    JPanel                                      myPanelValues, myContainer;
		private    LabelConstrained                            myHint = makeVerboseLabel( null, myPincher );
		private    Definitions<Object,Object,ValueFunction>    myValueDef;
		private    Map<Object,ValueDefinition>                 myValueDefinitions;
		private    FiniteVariable                              myDestination, mySource;
		private    Interpolation                               myInterpolation;
	}

	/** @since 20071204 */
	private static LabelConstrained makeVerboseLabel( String text, Component pincher ){
		LabelConstrained ret = (text == null) ? new LabelConstrained() : new LabelConstrained( text );
		if( true ){            ret.setVisible( false );
		                       ret. setBorder( makeVerboseBorder() ); }
		if( pincher != null ){ ret.  setPinch( pincher ); }

		return ret;
	}

	/** @since 20070125 */
	static public Border makeVerboseBorder(){
		Border etched    = createEtchedBorder();
		Border empty1    = createEmptyBorder( 0, 8, 0, 8 );
		Border compound1 = createCompoundBorder( etched, empty1 );
		Border empty2    = createEmptyBorder( 8, 0, 8, 0 );
		Border compound2 = createCompoundBorder(  empty2, compound1 );
		return compound2;
	}

	/** @since 20071204 */
	public enum Normalization implements Actionable<Normalization>, Semantic, Dendriform<Menu>{
		none         ( "none",                  "<html><font color='#ff0000'>no</font> <font color='#cc6600'>normalization</font> (raw values)" ),
		bycondition  ( "over entire condition", "<html>standard <font color='#cc6600'>normalization</font> over each condition" ){
			public double normalize( Parameters cells, double to ){
				boolean   fill          =   cells.sum_raw <= 0;
				double    denominator   =   fill ? ((double)cells.cells.length) : cells.sum_raw;
				double    factor        =   to / denominator;
				double    sum           =   0;
				for( Parameter cell : cells.cells ) sum += (fill ? cell.fill( factor ) : cell.normalize( factor ));
				return    sum;
			}
		},
		written      ( "written values only",   "<html><font color='#cc6600'>normalize</font> over only the probablility values that are not <font color='#ff0000'>skipped" ){
			public double normalize( Parameters cells, double to ){
				boolean   fill          =   cells.sum_written <= 0;
				double    denominator   =   0;
				if( fill ){ for( Parameter cell : cells.cells ) if( cell.written ) denominator += 1; }
				else{     denominator   = cells.sum_written; }
				double    factor        = to / denominator;
				double    sum           = 0;
				for( Parameter cell : cells.cells ) if( cell.written ) sum += (fill ? cell.fill( factor ) : cell.normalize( factor ));
				return sum;
			}
		};

		private Normalization( String d, String t ){
			this.display     = d;
			this.description = t;

			this.properties.put( Property.display, d );
			this.properties.put(          tooltip, t );
			this.properties.put(      accelerator, firstUniqueKeyStroke( name() ) );
		}

		public Semantics semantics(){ return      exclusive; }

		public Menu         parent(){ return Menu.normalize; }

		/** @since 20071210 */
		public double normalize( Parameters cells, double to ){ return cells.sum_raw; }

		public String toString(){ return display; }

		public   final   String   display, description;

		/** @since 20071210 */
		public Normalization getDefault(){ return none; }

		/** @since 20071210 */
		public Object get( Property property ){ return this.properties.get( property ); }

		/** @since 20071210 */
		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		/** @since 20071210 */
		public static AbstractButton[] buttons( View view, Object id, ItemListener listener ){
			return MODELS.newButtons( view, id, listener );
		}

		/** @since 20071210 */
		public static JComboBox combo( Object id, ItemListener listener ){
			return MODELS.newComboBox( id, listener );
		}

		/** @since 20071210 */
		public static Normalization selected( Object id ){
			return MODELS.selected( exclusive, id );
		}

		/** @since 20071210 */
		public static EnumModels<Normalization> MODELS = new EnumModels<Normalization>( Normalization.class );
	}

	/** @since 20071210 */
	public static final boolean FLAG_SHOW_NORMALIZATION_DEFAULT = false;

	/** @since 20071204 */
	private JPanel makeNormalizationPanel(){
		JPanel             ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c   = new GridBagConstraints();

		final DecimalField df  = myFieldNormalizationConstant = new DecimalField( 1, 3, 0.0, 1.0 );
		df.setToolTipText( "<html><font color='#cc6600'>normalization</font> constant" );

		final JComboBox    box = Normalization.combo( idmenus, null );
		box.addActionListener( new ActionListener(){
			{ actionPerformed( null ); }

			public void actionPerformed( ActionEvent event ){
				Normalization selected = (Normalization) box.getSelectedItem();
				box.setToolTipText( selected.description );
				df.setEnabled( Normalization.selected( idmenus ) != Normalization.none );
			}
		} );

		final JLabel   caption = new JLabel( "normalize" );
		caption.setToolTipText( "<html>ensure values sum to a given constant, usually <font color='#00cc00'>1.0" );

		ret.add(                                     caption, c );
		ret.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		c.weightx              = 1;
		c.fill                 = GridBagConstraints.HORIZONTAL;
		ret.add(                                         box, c );
		c.weightx              = 0;
		c.fill                 = GridBagConstraints.NONE;
		ret.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );
		ret.add(                new JLabel( "to"           ), c );
		ret.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );
		c.gridwidth            = GridBagConstraints.REMAINDER;
		c.weightx              = 1;
		c.fill                 = GridBagConstraints.HORIZONTAL;
		ret.add(                                          df, c );

		ret.setBorder( createCompoundBorder( createEtchedBorder(), createEmptyBorder( 2, 4, 2, 4 ) ) );

		ret.setVisible( FLAG_SHOW_NORMALIZATION_DEFAULT );

		return ret;
	}

	/** @since 20071213 */
	private JMenuBar extractMenuBar(){
		if( myPanelMain.isAncestorOf( myMenuBar ) ) myPanelMain.remove( myMenuBar );
		if( myMenuBar.getBorder() != myMenuBarBorderDefault ) myMenuBar.setBorder( myMenuBarBorderDefault );
		return ProbabilityRewrite.this.myMenuBar;
	}

	/** @since 20071216 */
	public enum Menu implements Dendriform<Menu>{
		file         ( "File"    ),
		format       ( "format...",          file ),
		redirect     ( "write to...",        file ),
		options      ( "Options" ),
		normalize    ( "normalize...",    options ),
		view         ( "View"    ),
		summary      ( "level of detail...", view ),
		variableview ( "variable...",        view ),
		help         ( "Help"    );

		private Menu( String display ){
			this( display, null );
		}

		private Menu( String display, Menu parent ){
			this.display = display;
			this.parent  = parent;
		}

		public Menu     parent(){ return  parent; }

		public String toString(){ return display; }

		final public String display;
		final public Menu   parent;
	}

	/** @since 20071216 */
	public enum Ingredient implements Actionable<Ingredient>, Semantic, Dendriform<Menu>{
		locks              ( "locks",                         "definition locks",                       Menu.view, true ),
		arguments          ( "arguments",                     "function arguments",                     Menu.view, true ),
		conveniences       ( "conveniences",                  "convenience settings",                   Menu.view, true ),
		hints              ( "hints",                         "hints",                                  Menu.view, true ),
		buttons            ( "buttons",                       "extra buttons",                          Menu.view, true ),
		values             ( "values definition",             "the lower values definition edit panel", Menu.view ),
		normalization      ( "normalization",                 "normalization options",                  Menu.view ),
		conditionfunctions ( "conditions function reference", "the conditions function reference text", Menu.help ),
		valuefunctions     ( "values function reference",     "the values function reference text",     Menu.help );

		private Ingredient( String displayname, String tip, Menu menu ){
			this( displayname, tip, menu, false );
		}

		private Ingredient( String displayname, String tip, Menu menu, boolean late ){
			properties.put(     display, displayname  );
			properties.put(     tooltip, tip          );
			properties.put( accelerator, firstUniqueKeyStroke( displayname ) );
			properties.put(      insets, INSETS       );
			properties.put(         shy, Boolean.TRUE );
			if( late ){ properties.put( Property.late, Boolean.TRUE ); }
			this.menu = menu;
		}

		public  Ingredient           getDefault(){ return conditionfunctions; }
		public  Object                      get( Property property ){ return this.properties.get( property ); }
		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		public Menu parent(){ return menu; }

		final public Menu menu;

		public Semantics semantics(){ return additive; }
	}

	/** @since 20071223 */
	public <T extends Enum<T> & Actionable<T> & Dendriform<Menu> & Semantic> Component nilla( T id, Items<T,Menu> items ){
		return nilla( id, items, false );
	}

	/** @since 20071223 */
	public <T extends Enum<T> & Actionable<T> & Dendriform<Menu> & Semantic> Component nilla( T id, Items<T,Menu> items, boolean consummate ){
	  /*boolean debug = Ingredient.hints.equals( id );
		if( debug ){ System.out.println( "nilla( "+id+", "+id(items)+", "+(consummate ? "consummate()":"add()")+" )" ); }*/

		Ingredient       gred                       = Ingredient.buttons;
		Component                    proxyblacklist = createGlue();
		id2proxy   .put(   id,       proxyblacklist );
		Component                                    proxybuttons = createGlue();
		if(              consummate ){
			ingredients .consummate( gred,           proxybuttons ); }else{
			ingredients        .add( gred,           proxybuttons ); }
		AbstractButton               ret            = items.models.newButton( View.nilla, idmenus, id );
		new Proxy( new Component[]{  proxyblacklist, proxybuttons }, visibility, and, new Component[]{ ret }, visibility );
		proxyblacklist.setVisible( white( id ) );
	  /*if( debug ){
			Util.STREAM_DEBUG.println( "    proxybuttons  .isVisible()? " +   proxybuttons.isVisible() );
			Util.STREAM_DEBUG.println( "    proxyblacklist.isVisible()? " + proxyblacklist.isVisible() );
		}*/

		return ret;
	  //return black( id ) ? createGlue() : ingredients.models.newButton( View.nilla, idmenus, id );
	}

	/** @since 20071217 */
	public enum Summary implements Actionable<Summary>, Semantic, Dendriform<Menu>{
		simple   ( "simple",    "<< simple", Menu.summary,  "<html>switch to a <font color='#00cc00'>simple</font> view: only the <font color='#cc6600'>source</font> and <font color='#cc6600'>destination</font> variable names" ),
		advanced ( "advanced",  "advanced >>", Menu.summary,  "<html>switch to an <font color='#990099'>advanced</font> view: show detailed information about the <font color='#cc6600'>source</font> and <font color='#cc6600'>destination</font> variables", true );

		private Summary( String displayname, String nilla, Menu menu, String tip ){
			this( displayname, nilla, menu, tip, false );
		}

		private Summary( String displayname, String nilla, Menu menu, String tip, boolean fshy ){
			properties.put(     display, displayname );
			properties.put(   nillatext, nilla       );
			properties.put(     tooltip, tip         );
			properties.put( accelerator, firstUniqueKeyStroke( displayname ) );
			properties.put(      insets, INSETS      );
			if( fshy ) properties.put( shy, Boolean.TRUE );
			this.menu = menu;
		}

		public  Summary              getDefault(){ return simple; }
		public  Object                      get( Property property ){ return this.properties.get( property ); }
		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		public Menu parent(){ return menu; }

		final public Menu menu;

		public Semantics semantics(){ return exclusive; }
	}

	/** @since 20071217 */
	public enum Option implements Actionable<Option>, Semantic, Dendriform<Menu>{
		cardinalities ( "cardinalities", "cardinalities", Menu.view );

		private Option( String displayname, String tip, Menu menu ){
			properties.put(     display, displayname );
			properties.put(     tooltip, tip         );
			properties.put( accelerator, firstUniqueKeyStroke( displayname ) );
			this.menu = menu;
		}

		public  Option              getDefault(){ return cardinalities; }
		public  Object                      get( Property property ){ return this.properties.get( property ); }
		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		public Menu parent(){ return menu; }

		final public Menu menu;

		public Semantics semantics(){ return additive; }

		static public EnumModels<Option> MODELS = new EnumModels<Option>( Option.class );
	}

	/** @since 20071217 */
	private AsynchronousItemListener myListenerCards = new AsynchronousItemListener( "toggle cardinalities" ){
		public AsynchronousItemListener runImpl() throws Exception{
			sleep( 0x40 );
			deeplyInvalid( myPanelConditions );
			pack( LONG_PACK_DELAY );
			return this;
		}
	};

	/** @since 20071216 */
	private Object                             idmenus = new Object();

	private Collection<Packable>          myPackAnimal = new ArrayList<Packable>(1);

	private Items<Ingredient,     Menu>    ingredients = new  Items<Ingredient,     Menu>(      Ingredient.class )
		.fallback( packdelaymillis, LONG_PACK_DELAY )
		.fallback( packanimal,      myPackAnimal )
		.fallback( yeaverbphrase,   "show" )
		.fallback( nayverbphrase,   "hide" );
	   //.require( pinchcomponent );

	private Items<Summary,        Menu>      summaries = new  Items<Summary,        Menu>(         Summary.class )
		.fallback( packdelaymillis, LONG_PACK_DELAY )
		.fallback( packanimal,      myPackAnimal )
		.fallback( yeaverbphrase,   "" )
		.fallback( nayverbphrase,   "" );

	private Items<Normalization,  Menu> normalizations = new  Items<Normalization,  Menu>(   Normalization.class,   Normalization.MODELS )
		.addItemListener( idmenus, new ItemListener(){
			public void itemStateChanged( ItemEvent event ){
				if( event == null || event.getStateChange() != ItemEvent.SELECTED ) return;
				bucket.clear();
				if( ingredients.models.selected( additive, idmenus, bucket ).contains( mine ) ) return;
				ingredients.models.fire( additive, idmenus, mine );
			}

			private     Ingredient  mine   = Ingredient.normalization;
			private Set<Ingredient> bucket = EnumSet.noneOf( Ingredient.class );
		}, Normalization.bycondition, Normalization.written );

	private Items<Option,         Menu>        options = new  Items<Option,         Menu>(          Option.class,          Option.MODELS )
	  //.setSelected( true, idmenus, Option.cardinalities )
		.addItemListener( idmenus, myListenerCards, Option.cardinalities );

	private Items<Stringification,Menu>     ifications = new  Items<Stringification,Menu>( Stringification.class, Stringification.MODELS )
		.addItemListener( idmenus, myListenerStringification, Stringification.values() );

	private WriteActionConfigurator wac = new WriteActionConfigurator( Task.write.action( this ), idmenus );

	private Models<Redirect,      Menu>   redirections = new Models<Redirect,       Menu>(        Redirect.class,        Redirect.MODELS )
		.def( Menu.redirect, Redirect.values() )
		.addItemListener( idmenus, wac, Redirect.values() );

	private Models<Format,        Menu>        formats = new Models<Format,         Menu>(          Format.class,          Format.MODELS )
		.def( Menu.format,     Format.values() )
		.addItemListener( idmenus, wac, Format.values() );

	@SuppressWarnings( "unchecked" )
	private Menus<Menu>   menus = new Menus<Menu>( Menu.class, caste().blacklist(), summaries, ingredients, options, normalizations, ifications )
		.def( Task.preview.action( ProbabilityRewrite.this ) )
		.def( redirections, formats )
		.def( Task.write.action( this ) );

	private JPanel makePanelMain(){
		JPanel             ret = new JPanel( new GridBagLayout() );
		ret.setName( "main panel" );
		if( DEBUG_BORDERS ){ ret.setBorder( createLineBorder( Color.yellow, 1 ) ); }
	  /*final long NANOGENESIS = System.nanoTime();
		ret.addHierarchyListener( new HierarchyListener(){
			public void hierarchyChanged( HierarchyEvent event ){
			  //Util.STREAM_DEBUG.println( event.getComponent().getName() + " " + event.getChanged().getName() );
			  //if( (event.getChanged() == event.getComponent()) &&
				if( ((event.getChangeFlags() & mask) > 0) ){
					Util.STREAM_DEBUG.println( "hrl @" + (System.nanoTime() - NANOGENESIS) + " " + event.getComponent().getName() + (event.getComponent().isDisplayable() ? " displayable" : " lost") + (event.getComponent().isShowing() ? ", displayed" : ", killed") );
				}
			}
			int mask = HierarchyEvent.SHOWING_CHANGED;// | HierarchyEvent.DISPLAYABILITY_CHANGED | HierarchyEvent.PARENT_CHANGED;
		} );*/
	  /*ret.addComponentListener( new ComponentAdapter(){
			public void componentShown( ComponentEvent event ){
				Util.STREAM_DEBUG.println( "cls " + event.getComponent().getName() + (event.getComponent().isDisplayable() ? " displayable" : " lost") + (event.getComponent().isShowing() ? ", displayed" : ", killed") );
			}
			public void componentHidden( ComponentEvent event ){
				Util.STREAM_DEBUG.println( "clh " + event.getComponent().getName() + (event.getComponent().isDisplayable() ? " displayable" : " lost") + (event.getComponent().isShowing() ? ", displayed" : ", killed") );
			}
		} );
		ret.addAncestorListener( new AncestorListener(){
			public void ancestorAdded(AncestorEvent event){
				Util.STREAM_DEBUG.println( "ala " + event.getComponent().getName() + (event.getComponent().isDisplayable() ? " displayable" : " lost") + (event.getComponent().isShowing() ? ", displayed" : ", killed") );
			}
			public void ancestorMoved(AncestorEvent event){}
			public void ancestorRemoved(AncestorEvent event){
				Util.STREAM_DEBUG.println( "alr " + event.getComponent().getName() + (event.getComponent().isDisplayable() ? " displayable" : " lost") + (event.getComponent().isShowing() ? ", displayed" : ", killed") );
			}
		} );*/

		GridBagConstraints c   = new GridBagConstraints();

		c.gridwidth            = GridBagConstraints.REMAINDER;
		c.weightx              = 1;
		c.fill                 = GridBagConstraints.HORIZONTAL;

		ret.add(                   myMenuBar = menus.bar( idmenus ), c );
		myMenuBarBorderDefault =   myMenuBar.getBorder();
		myMenuBar.setBorder( createCompoundBorder( createEmptyBorder( 0, 0, 0x10, 0 ), createEtchedBorder() ) );

	 //(myPincher = new JLabel()).setBorder( makeVerboseBorder() );
	  //myPincher = new JLabel();//Box.createGlue();
	  //ret.add(  myPincher, c );

		myPincher = ret;

		JPanel pnlSummary = new JPanel( new BorderLayout() );
		pnlSummary.setName( "summary container" );
		pnlSummary.add( summaries.consummate( Summary.simple,   makeSummaryPanelSimple()   ), BorderLayout.NORTH  );
		pnlSummary.add( summaries.consummate( Summary.advanced, makeSummaryPanelDetailed() ), BorderLayout.CENTER );

		ret.add( pnlSummary, c );

		c.fill                 = GridBagConstraints.BOTH;
		ret.add( ingredients.add( Ingredient.hints, myHintSummary = makeVerboseLabel( null, myPincher ) ), c );

		ret.add(   Box.createVerticalStrut( 1              ), c );
		c.fill                 = GridBagConstraints.HORIZONTAL;
		ret.add( makeSubcomponent( myPanelConditions = new JPanel( new GridBagLayout() ), "define conditions" ), c );
		myPanelConditions.setName( "conditions panel" );

		if( DEBUG_BORDERS ) myPanelConditions.setBorder( createLineBorder( Color.blue, 1 ) );

		c.fill                 = GridBagConstraints.BOTH;
		ret.add( ingredients.consummate( Ingredient.hints, myHintConditions = makeVerboseLabel( null, myPincher ) ), c );
		c.fill                 = GridBagConstraints.HORIZONTAL;

		c.fill                 = GridBagConstraints.NONE;
		c.gridwidth            = 1;
		c.weightx              = 0;
		ret.add( nilla( Ingredient.conditionfunctions, ingredients ), c );//ingredients.put(  Ingredient.buttons, ingredientNilla( Ingredient.conditionfunctions ) ), c );
		c.gridwidth            = GridBagConstraints.REMAINDER;
		c.weightx              = 1;
		ret.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), c );
		c.fill                 = GridBagConstraints.HORIZONTAL;
		ret.add( ingredients.consummate( Ingredient.conditionfunctions, makeVerboseLabel( ConditionFunction.htmlReference(), myPincher ) ), c );

		myContainerValues = new JPanel( new GridLayout( 1, 1 ) );
		myContainerValues.setBorder( createEmptyBorder( INT_SIZE_STRUT, 0, 0, 0 ) );
		ingredients.put( Ingredient.values, Property.gastanks, new Container[]{ myContainerValues } );

		if( DEBUG_BORDERS ) myContainerValues.setBorder( createLineBorder( Color.blue, 1 ) );

		ingredients.consummate( Ingredient.values, myWidgetValues  = new JPanel( new GridBagLayout() ) );
		myWidgetValues.setName( "values widget" );
		GridBagConstraints c2  = new GridBagConstraints();

		c2.gridwidth           = GridBagConstraints.REMAINDER;
		c2.weightx             = 1;
		c2.fill                = GridBagConstraints.BOTH;
		myWidgetValues.add(                                       myContainerValues, c2 );
		c2.fill                = GridBagConstraints.NONE;
		c2.weightx             = 0;
		c2.gridwidth           = 1;
		myWidgetValues.add( nilla( Ingredient.valuefunctions, ingredients ), c );//ingredients.put(  Ingredient.buttons, ingredientNilla( Ingredient.valuefunctions ) ), c );
		c2.gridwidth           = GridBagConstraints.REMAINDER;
		myWidgetValues.add(             Box.createHorizontalStrut( INT_SIZE_STRUT ), c2 );
		c2.weightx             = 1;
		myWidgetValues.add( ingredients.consummate( Ingredient.valuefunctions, makeVerboseLabel( ValueFunction.htmlReference(), myPincher ) ), c2 );

		if( DEBUG_BORDERS ) myWidgetValues.setBorder( createLineBorder( Color.red, 1 ) );

		c.fill                 = GridBagConstraints.BOTH;
		c.weightx              = 1;
		ret.add(                              myWidgetValues, c );

		c.fill                 = GridBagConstraints.HORIZONTAL;
		ret.add( ingredients.consummate( Ingredient.normalization, makeNormalizationPanel() ), c );

		ingredients.consummate( Ingredient.buttons,   null );

		return ret;
	}

	private Thread run( Runnable runnable, final boolean asynchronous ){
		if( asynchronous ){
			Thread ret = new Thread( runnable );
			ret.start();
			return ret;
		}
		else{
			runnable.run();
			return Thread.currentThread();
		}
	}

	/** @since 20071204 */
	private Thread setValuesPanel( final JComponent valuespanel, final boolean kill, final boolean asynchronous ){
		if( kill ) return myContainerValues.isAncestorOf( valuespanel ) ? setValuesPanel( null, false, asynchronous ) : null;

		return run( new Runnable(){
			public void run(){
				myContainerValues.removeAll();
				if( valuespanel != null ){
					myContainerValues.add( valuespanel );
					myContainerValues.revalidate();
					myContainerValues.repaint();
				}

				myWidgetValues.setVisible( valuespanel != null );

				if( asynchronous ) ProbabilityRewrite.this.pack( LONG_PACK_DELAY );
			}
		}, asynchronous );
	}

	public static final String HTML_TIP_SOURCE = "<html>the <font color='#cc6600'>source</font>      cpt: the cpt from which you want to <font color='#ff00ff'>copy</font> probability values",
	                           HTML_TIP_DEST   = "<html>the <font color='#cc6600'>destination</font> cpt: the cpt            you want to <font color='#ff00ff'>modify",
	                           HTML_LINEBREAK  = "<br>\n";//"<br />\n"

	private JPanel makeSummaryPanelSimple(){
		JPanel             ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c   = new GridBagConstraints();
		ret.setName( "simple summary" );

		JLabel label;
		ret.add(  label =        new JLabel(      "source:" ), c );
		label.setToolTipText( HTML_TIP_SOURCE );
		ret.add(  Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		c.weightx              = 1;
		ret.add( myLabelSource = new JLabel(            "?" ), c );
		c.weightx              = 0;

		ret.add(  Box.createHorizontalStrut( INT_SIZE_STRUT ), c );
		ret.add(  label =        new JLabel( "destination:" ), c );
		label.setToolTipText( HTML_TIP_DEST );
		ret.add(  Box.createHorizontalStrut( INT_SIZE_STRUT ), c );

		c.weightx              = 1;
		c.gridwidth            = GridBagConstraints.REMAINDER;
		ret.add( myLabelDest   = new JLabel(            "?" ), c );
		c.weightx              = 0;

		ret.add(                 Box.createVerticalStrut( 2 ), c );

		c.anchor               = GridBagConstraints.WEST;
		c.gridwidth            = 4;
		ret.add( nilla( Ingredient.hints, ingredients ), c );//ingredients.put( Ingredient.buttons, ingredientNilla( Ingredient.hints ) ), c );

		c.anchor               = GridBagConstraints.EAST;
		c.gridwidth            = GridBagConstraints.REMAINDER;
		ret.add( nilla( Summary.advanced, summaries ), c );//ingredients.put( Ingredient.buttons, summaryNilla( Summary.advanced ) ), c );

		myLabelSource.setFont( myLabelSource.getFont().deriveFont( Font.BOLD ) );
		myLabelDest  .setFont( myLabelDest  .getFont().deriveFont( Font.BOLD ) );

		return ret;
	}

	private JPanel makeSummaryPanelDetailed(){
		JPanel                  grid = new JPanel( new GridLayout( 1, 2 ) );

		grid.add( myPanelSource      = new VariablePanel( "source"      ) );
		grid.add( myPanelDestination = new VariablePanel( "destination" ) );

		JPanel                   ret = new JPanel( new GridBagLayout() );
		GridBagConstraints       c   = new GridBagConstraints();
		ret.setName( "advanced summary" );

		c.gridwidth                  = GridBagConstraints.REMAINDER;
		ret.add( grid, c );
		c.gridwidth                  = 1;
		ret.add( nilla( Ingredient.hints, ingredients ), c );//ingredients.put( Ingredient.buttons, ingredientNilla( Ingredient.hints ) ), c );

		c.weightx                    = 1;
		c.fill                       = GridBagConstraints.HORIZONTAL;
		ret.add( Box.createHorizontalStrut( 0x10 ), c );
		c.gridwidth                  = GridBagConstraints.REMAINDER;
		c.weightx                    = 0;
		c.fill                       = GridBagConstraints.NONE;

		ret.add( nilla( Summary.simple, summaries ), c );//ingredients.put( Ingredient.buttons, summaryNilla( Summary.simple ) ), c );

		myPanelSource     .setToolTipText( HTML_TIP_SOURCE );
		myPanelDestination.setToolTipText( HTML_TIP_DEST   );

		return ret;
	}

	public static final Insets INSETS = new Insets( 0, 0x10, 0, 0x10 );

	public static class VariablePanel extends JPanel{
		public VariablePanel( String title ){
			super( new GridBagLayout() );
			myTitle = title;
			VariablePanel.this.init();
		}

		public VariablePanel assume( FiniteVariable var, BeliefNetwork bn ){
			myLabelID       .setText(  var.getID() );
			myLabelLabel    .setText( (var instanceof StandardNode) ? ((StandardNode)var).getLabel() : "" );
			myLabelCard     .setText( Integer.toString( var.size() ) );
			int  indegree = bn .inDegree( var );
			int outdegree = bn.outDegree( var );
			myLabelIndegree .setText( (indegree  == 0) ? "root" : Integer.toString(  indegree ) );
			myLabelOutdegree.setText( (outdegree == 0) ? "leaf" : Integer.toString( outdegree ) );

			return VariablePanel.this;
		}

		private void init(){
			GridBagConstraints c   = new GridBagConstraints();

			myLabelID        = field(          "id:", c, true );
			myLabelLabel     = field(       "label:", c, true );
			myLabelIndegree  = field(   "in-degree:", c );
			myLabelOutdegree = field(  "out-degree:", c );
			myLabelCard      = field( "cardinality:", c );

			Border titled   = createTitledBorder( createEtchedBorder(), myTitle );
			Border empty    = createEmptyBorder( 0, 8, 0, 8 );
			Border compound = createCompoundBorder( titled, empty );

			this.setBorder( compound );
		}

		private JLabel field( String caption, GridBagConstraints c ){
			return field( caption, c, false );
		}

		private JLabel field( String caption, GridBagConstraints c, boolean bold ){
			c.anchor = GridBagConstraints.EAST;
			this.add( new JLabel( caption ), c );
			strut();
			JLabel ret = new JLabel( "?" );
			if( bold ) ret.setFont( ret.getFont().deriveFont( Font.BOLD ) );
			remainder( ret, c );
			return ret;
		}

		private void remainder(	JComponent comp, GridBagConstraints c ){
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx   = 1;
			c.fill      = GridBagConstraints.HORIZONTAL;
			int anchor  = c.anchor;
			c.anchor    = GridBagConstraints.WEST;
			this.add( comp, c );
			c.gridwidth = 1;
			c.weightx   = 0;
			c.fill      = GridBagConstraints.NONE;
			c.anchor    = anchor;
		}

		private void strut(){
			this.add( Box.createHorizontalStrut( INT_SIZE_STRUT ), CONSTRAINTS_ZERO );
		}

		public static final GridBagConstraints CONSTRAINTS_ZERO = new GridBagConstraints();

		private String myTitle;
		private JLabel myLabelID, myLabelLabel, myLabelCard, myLabelIndegree, myLabelOutdegree;
	}

	public static JComponent makeSubcomponent( JComponent contents, String title ){
		JPanel             ret = new JPanel( new GridBagLayout() );

		GridBagConstraints c   = new GridBagConstraints();

		c.weightx              = 1;
		c.fill                 = GridBagConstraints.HORIZONTAL;
		c.gridwidth            = GridBagConstraints.REMAINDER;
		ret.add( Box.createHorizontalStrut( 0x80 ), c );

		ret.add(                          contents, c );

		ret.setBorder( createTitledBorder( createEtchedBorder(), title ) );

		return ret;
	}

	/** @since 20071211 */
	public   static   final  char   CHAR_COLUMN_PERCENT = '\n';//'%';
	/** @since 20071211 */
	public   static   final  String  STR_COLUMN_COUNT   = "#",//"count",
	                                 STR_COLUMN_PERCENT = "%";//"percent";

	/** @since 20071210 */
	public   static   final  Stain   STAIN___NORMAL     = new Stain( "normal",    Color.white,                   Color.black, "normal"    ),
	                                 STAIN___CHANGED    = Stat.   changed.stain(),
	                                 STAIN_UNCHANGED    = Stat. unchanged.stain();

	/** @since 20071210 */
	private  static          Map<Stain,Object> STAIN2STAT;
	/** @since 20071210 */
	public static Map<Stain,Object> stain2stat(){
		if( STAIN2STAT != null ) return STAIN2STAT;

		HashMap<Stain,Object> s2s = new HashMap<Stain,Object>();

		for( ValueFunction vf   : ValueFunction.values() ) s2s.put( vf.stain, vf );
		Stain stain;
		for( Stat          stat :          Stat.values() ) if( (stain = stat.stain()) != null ) s2s.put( stain, stat );

		return STAIN2STAT = s2s;
	}

	/** @since 20071210 */
	public class Stainer extends Broadcaster implements StainWright, ActionListener
	{
		public Stainer( RewriteResult rewriteresult, double[] before, double[] after, TableModelHGS model ){
			this.rewriteresult    = rewriteresult;
			this.myTableModelHGS  = model;
			this.before           = before;
			this.after            = after;
		}

		public Color getBackground( int row, int column ){
			Stain stain = getStain( row, column );
			return stain == null ? Color.white : stain.getBackground();
		}

		public void stain( int row, int column, Component comp ){
			Stain stain = getStain( row, column );
			if( stain != null ) stain.stain( comp );
		}

		public Stain[] getStains(){
			if( stains == null ){
				Stain[] vfstains = ValueFunction.stains();
				stains = new Stain[ vfstains.length + 2 ];
				System.arraycopy( vfstains, 0, stains, 0, vfstains.length );
				stains[ vfstains.length     ] = STAIN___CHANGED;
				stains[ vfstains.length + 1 ] = STAIN_UNCHANGED;
			}
			return stains;
		}

		public Stain getStain( int row, int column ){
			Stain ret = STAIN___NORMAL;
			try{
				int linear = myTableModelHGS.calculateDataIndex( row, column );
				if( linear >= 0 ){
					boolean ff = false;
					for( ValueFunction function : rewriteresult.explanation[ linear ] ){
						if( function.stain.isEnabled() && function.stain.isSelected() ){
							ret = function.stain;
							ff  = true;
							break;
						}
					}
					if( ! ff ) ret = after[ linear ] == before[ linear ] ? STAIN_UNCHANGED : STAIN___CHANGED;
				}
			}finally{
				if( (! ret.isSelected()) || (! ret.isEnabled()) ) ret = STAIN___NORMAL;
			}
			return ret;
		}

		public void actionPerformed( ActionEvent event ){
			setEnabled( myCB.isSelected() );
			fireListeners();
		}

		private JComponent hstrut( int width, JComponent pnl, GridBagConstraints c ){
			pnl.add( DEBUG_STRUTS ? new JLabel( "h"+(myStrutCounter++)+"-" ) : Box.createHorizontalStrut( width ), c );
			if( c.gridwidth == GridBagConstraints.REMAINDER ) myStrutCounter = 0;
			return pnl;
		}

		private JComponent vstrut( int height, JComponent pnl, GridBagConstraints c ){
			pnl.add( DEBUG_STRUTS ? new JLabel( "vstrut" ) : Box.createVerticalStrut( height ), c );
			return pnl;
		}

		public JComponent getSwitches(){
			if( mySwitches != null ) return mySwitches;

			myCB = new JCheckBox();
			myCB.setSelected( myFlagEnabled );
		  //myCB.setToolTipText( STR_TOOLTIP_BOX );
			myCB.addActionListener( Stainer.this );

			mySwitches           = new JPanel( new GridBagLayout() );
			GridBagConstraints c = new GridBagConstraints();
			c.anchor             = GridBagConstraints.WEST;
			c.weightx            = 0;

			hstrut( 8, mySwitches, c );
			hstrut( 8, mySwitches, c );
			hstrut( 8, mySwitches, c );
			hstrut( 8, mySwitches, c );
			hstrut( 8, mySwitches, c );
			hstrut( 8, mySwitches, c );
			hstrut( 8, mySwitches, c );
			hstrut( 8, mySwitches, c );
			c.gridwidth          = GridBagConstraints.REMAINDER;
			hstrut( 8, mySwitches, c );

			JLabel lbl = null;
			c.anchor             = GridBagConstraints.EAST;
			c.gridwidth          = GridBagConstraints.REMAINDER;
			mySwitches.add( lbl  = new JLabel( "<html><b>statistics</b> " ), c );

		  	c.gridwidth          = 3;
			c.anchor             = GridBagConstraints.WEST;
			mySwitches.add( lbl  = new JLabel( "<html><b>colors</b> " ), c );
		  //lbl.setToolTipText( STR_TOOLTIP_KEY );
			c.gridwidth          = 1;
			hstrut( 1, mySwitches, c );
			mySwitches.add( myCB, c );
			hstrut( 1, mySwitches, c );
			c.anchor             = GridBagConstraints.EAST;
			mySwitches.add( lbl  = new JLabel( "<html><b>"+STR_COLUMN_COUNT+"</b> " ), c );
			hstrut( 1, mySwitches, c );
			c.gridwidth          = GridBagConstraints.REMAINDER;
			mySwitches.add( lbl  = new JLabel( "<html><b>"+STR_COLUMN_PERCENT+"</b> " ), c );

			Set<Stat>   stats    = EnumSet.allOf( Stat.class );
			Object      stat;
			Stain[]     stains   = getStains();
			for( int i=0; i<stains.length; i++ ){
				c.anchor         = GridBagConstraints.WEST;
				c.fill           = GridBagConstraints.HORIZONTAL;
				c.weightx        = 1;
				c.gridwidth      = 1;
				stains[i].addSwitch( mySwitches, c );

				c.anchor         = GridBagConstraints.EAST;
				c.fill           = GridBagConstraints.NONE;
				c.weightx        = 0;

				hstrut( 1, mySwitches, c );

				mySwitches.add( new JLabel( Integer.toString( rewriteresult.stat( stat = stain2stat().get( stains[i] ) ) ), JLabel.RIGHT ), c );

				hstrut( 1, mySwitches, c );
				c.gridwidth      = GridBagConstraints.REMAINDER;
				mySwitches.add( new JLabel( getFormatPercent( CHAR_COLUMN_PERCENT ).format( rewriteresult.ratio( stat ) ) ), c );

				stats.remove( stat );
			}

			for( Stat leftover : stats ){
				c.anchor         = GridBagConstraints.WEST;
				c.gridwidth      = 1;
				vstrut( Stain.DIM_SIZE_LABEL.height + 6, mySwitches, c );
				hstrut( 1, mySwitches, c );
				mySwitches.add( lbl = leftover.label(), c );
				if( leftover == Stat.total ) lbl.setFont( lbl.getFont().deriveFont( Font.BOLD ) );

				hstrut( 1, mySwitches, c );
				hstrut( 1, mySwitches, c );
				hstrut( 1, mySwitches, c );

				c.anchor         = GridBagConstraints.EAST;
				mySwitches.add( lbl = new JLabel( Integer.toString( rewriteresult.stat( leftover ) ) ), c );
				if( leftover == Stat.total ) lbl.setFont( lbl.getFont().deriveFont( Font.BOLD ) );

				hstrut( 1, mySwitches, c );
				c.gridwidth      = GridBagConstraints.REMAINDER;
				mySwitches.add( lbl = new JLabel( getFormatPercent( CHAR_COLUMN_PERCENT ).format( rewriteresult.ratio( leftover ) ) ), c );
				if( leftover == Stat.total ) lbl.setFont( lbl.getFont().deriveFont( Font.BOLD ) );
			}

			return mySwitches;
		}

		public boolean isEnabled(){
			return myFlagEnabled;
		}

		public void setEnabled( boolean flag ){
			if( myFlagEnabled != flag ){
				myFlagEnabled = flag;
				if( myCB != null ) myCB.setSelected( myFlagEnabled );
				Stain[] stains = getStains();
				for( int i=0; i<stains.length; i++ ) stains[i].setEnabled( myFlagEnabled );
			}
		}

		public void addListener( ActionListener listener ){
			super.addListener( listener );
			Stain[] stains = getStains();
			for( int i=0; i<stains.length; i++ )
				stains[i].addListener( listener );
		}

		public boolean removeListener( ActionListener listener ){
			Stain[] stains = getStains();
			for( int i=0; i<stains.length; i++ )
				stains[i].removeListener( listener );
			return super.removeListener( listener );
		}

		private    TableModelHGS    myTableModelHGS;
		private    RewriteResult    rewriteresult;
		private    double[]         before, after;
		private    JComponent       mySwitches;
		private    JCheckBox        myCB;
		private    boolean          myFlagEnabled = true;
		private    Stain[]          stains;
		private    int              myStrutCounter = 0;
	}

	/** @since 20071211 */
	public static DecimalFormat getFormatPercent( char ch ){
		if( FORMAT_PERCENT == null ){
			FORMAT_PERCENT = (DecimalFormat) NumberFormat.getPercentInstance();//new DecimalFormat( "##0.0%" );
			FORMAT_PERCENT.setMinimumFractionDigits( 1 );
			FORMAT_PERCENT.setMaximumFractionDigits( 1 );
		}
		DecimalFormatSymbols symbols = FORMAT_PERCENT.getDecimalFormatSymbols();
		symbols.setPercent( ch );
		FORMAT_PERCENT.setDecimalFormatSymbols( symbols );
		return FORMAT_PERCENT;
	}
	private static DecimalFormat FORMAT_PERCENT;

	/** @since 20071211 */
	private ProbabilityRewrite doPreview( Component parent ) throws Exception{
		FiniteVariable after = new FiniteVariableImpl( mydest );
		new Preview( this.mydest, this.mysrc, after, this.rewrite( after ) ).showDialog( parent );//showFrame();
	  //Thread.sleep( 0x40 );
		deeplyInvalid( myContainerValues );
		return this;
	}

	/** @since 20071211 */
	public boolean commit() throws Exception{
		this.rewrite( this.mydest );
		return true;
	}

	/** @since 20071217 */
	public class WriteActionConfigurator implements ItemListener
	{
		public WriteActionConfigurator( SamiamAction action, Object menuid ){
			this.action = action;
			this.menuid = menuid;

			configureWriteAction( action, menuid );
		}

		public void itemStateChanged( ItemEvent event ){
			if( (event != null) && (event.getStateChange() != ItemEvent.SELECTED) ) return;
			this.configureWriteAction( this.action, this.menuid );
		}

		/** @since 20071211 */
		private SamiamAction configureWriteAction( SamiamAction action, Object id ){
			try{
				Format       format =   Format.selected( id );
				Redirect   redirect = Redirect.selected( id );
				return configure( action, redirect, format );
			}catch( Throwable thrown ){
				System.err.println( "warning: ProbabilityRewrite.configureWriteAction() caught " + thrown );
				thrown.printStackTrace();
			}
			return action;
		}

		/** @since 20071211 */
		public SamiamAction configure( SamiamAction action, Redirect redirect, Format format ){
			action.setToolTipText( tooltip( redirect, format ) );
			action.setEnabled( redirect != txt2devnull && redirect != null );
			return action;
		}

		/** @since 20071211 */
		public String tooltip( Redirect redirect, Format format ){
			if( redirect == null ) redirect = Redirect.txt2devnull;
			if( format   == null ) format   = Format.human;
			String tip;
			switch( redirect ){
				case txt2dialog:
					tip = "popup output in " + format.get( display ) + " format";
					break;
				case txt2copy:
					tip = "copy output in " + format.get( display ) + " format to the system clipboard";
					break;
				default:
					tip = "write output in " + format.get( display ) + " format to " + redirect.get( display );
					break;
			}
			return tip;
		}

		final public SamiamAction action;
		final public Object       menuid;
	}

	/** @since 20071216 */
	public enum PreviewMenu implements Dendriform<PreviewMenu>{
		file         ( "File"    ),
		redirect     ( "write to...",        file ),
		view         ( "View"    ),
		options      ( "Options" );

		private PreviewMenu( String display ){
			this( display, null );
		}

		private PreviewMenu( String display, PreviewMenu parent ){
			this.display = display;
			this.parent  = parent;
		}

		public PreviewMenu parent(){ return parent; }

		public String    toString(){ return display; }

		final public String       display;
		final public PreviewMenu  parent;
	}

	/** @since 20071221 */
	public boolean white( Object obj ){
		return ! caste().blacklist().contains( obj );
	}

	/** @since 20071221 */
	public boolean black( Object obj ){
		return   caste().blacklist().contains( obj );
	}

	/** @since 20071221 */
	public boolean allwhite( Object ... objs ){
		Set<Object> blacklist = caste().blacklist();
		for( Object obj : objs ){ if( blacklist.contains( obj ) ){ return false; } }
		return true;
	}

	/** @since 20071221 */
	@SuppressWarnings( "unchecked" )
	public <T extends Enum<T>> T[] white( Class<T> clazz ){
		LinkedList<T> whitelist = new LinkedList<T>();
		Set<Object>   blacklist = caste().blacklist();
		for( T element : clazz.getEnumConstants() ){ if( ! blacklist.contains( element ) ){ whitelist.add( element ); } }
		return whitelist.toArray( (T[]) new Enum[ whitelist.size() ] );
	}

	/** @since 20071220 */
	public enum PreviewOption implements Actionable<PreviewOption>, Semantic, Dendriform<PreviewMenu>{
		linkedba ( "link before/after", "force the before and after cpt views to scroll together", PreviewMenu.options ),
		linkedsa ( "link source/after", "force the source and after cpt views to scroll together", PreviewMenu.options, true );

		private PreviewOption( String displayname, String tip, PreviewMenu menu ){
			this( displayname, tip, menu, false );
		}

		private PreviewOption( String displayname, String tip, PreviewMenu menu, boolean flag_shy ){
			properties.put(     display, displayname );
			properties.put(     tooltip, tip         );
			properties.put( accelerator, firstUniqueKeyStroke( displayname ) );
			if( flag_shy ){ properties.put( shy, Boolean.TRUE ); }
			this.menu = menu;
		}

		public  PreviewOption        getDefault(                   ){ return linkedba; }
		public  Object                      get( Property property ){ return this.properties.get( property ); }
		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		public PreviewMenu parent(){ return menu; }

		final public PreviewMenu menu;

		public Semantics semantics(){ return additive; }

		static public EnumModels<PreviewOption> MODELS = new EnumModels<PreviewOption>( PreviewOption.class );
	}

	/** @since 20071211 */
	private   static   int      INT_PREVIEW_COUNTER         = 1;
	private   static   Method   METHOD_JDIALOG_SETICONIMAGE = null;
	static{
		if(      METHOD_JDIALOG_SETICONIMAGE == null ){
			try{ METHOD_JDIALOG_SETICONIMAGE  = JDialog.class.getDeclaredMethod( "setIconImage", Image.class ); }
			catch( Throwable thrown ){ if( Util.DEBUG_VERBOSE ) System.err.println( "warning: ProbabilityRewrite static initialization caught " + thrown ); } }
		if(      METHOD_JDIALOG_SETICONIMAGE == null ){
			try{ METHOD_JDIALOG_SETICONIMAGE  = JDialog.class        .getMethod( "setIconImage", Image.class ); }
			catch( Throwable thrown ){ if( Util.DEBUG_VERBOSE ) System.err.println( "warning: ProbabilityRewrite static initialization caught " + thrown ); } }
	}

	/** @since 20080124 */
	public static Method getMethodJDialogSetIconImage(){
		return METHOD_JDIALOG_SETICONIMAGE;
	}

	/** @since 20071210 */
	public class Preview extends WindowAdapter implements ActionListener, JOptionResizeHelperListener
	{
		public Preview( FiniteVariable vbefore, FiniteVariable vsrc, FiniteVariable vafter, RewriteResult rewriteresult ){
			put(   Constituent.before,  vbefore  );
			put(   Constituent.source,  vsrc     );
			if( rewriteresult.effectiveClone() != null ){
			put(   Constituent.effective, rewriteresult.effectiveClone() );
			}
			put(   Constituent.after,   vafter   );
			this.rewriteresult   =   rewriteresult;
		}

		private State put( Constituent constituent, FiniteVariable variable ){
			State ret = new State( constituent, variable );
			myState.put( constituent, ret );
			return ret;
		}

		private Object                             idpreview = new Object();
		private Items<Constituent,PreviewMenu>  constituency = new Items<Constituent,PreviewMenu>( Constituent.class, white( Constituent.class ) )
			.fallback( packdelaymillis, LONG_PACK_DELAY )
			.fallback( packanimal,      new ArrayList<Packable>(1) )
			.fallback( yeaverbphrase,   "show" )
			.fallback( nayverbphrase,   "hide" );

		private Models<Redirect,  PreviewMenu>  redirections = new Models<Redirect,  PreviewMenu>(    Redirect.class, Redirect.MODELS )
			.def( PreviewMenu.redirect, Redirect.values() )
			.addItemListener( idpreview, new WriteActionConfigurator( Preview.this.getActionWrite(), idpreview ), Redirect.values() );

		private Items<PreviewOption,PreviewMenu>     options = new  Items<PreviewOption,PreviewMenu>( PreviewOption.class, PreviewOption.MODELS )
			.setSelected(  true, idpreview, PreviewOption.linkedba );

		@SuppressWarnings( "unchecked" )
		private Menus<PreviewMenu>                     menus = new Menus<PreviewMenu>( PreviewMenu.class, caste().blacklist(), constituency, options )
			.def( redirections )
			.def( Preview.this.getActionWrite() );

		public int showDialog( Component parent ){
			JComponent contents = Preview.this.getPanel();
			contents.setBorder( null );

			new JOptionResizeHelper( contents, true, 10000l, (JOptionResizeHelperListener) Preview.this ).start();

			JOptionPane.showMessageDialog(        parent, contents, "probability rewrite preview #" + (INT_PREVIEW_COUNTER++), JOptionPane.PLAIN_MESSAGE );

			return JOptionPane.OK_OPTION;
		}

		/** @since 20071213 */
		public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
			try{
				this.setMenuBar( container );
			  //container.setIconImage( ICONIMAGE );
				if( METHOD_JDIALOG_SETICONIMAGE != null ) METHOD_JDIALOG_SETICONIMAGE.invoke( container, ICONIMAGE );
			}catch( Throwable thrown ){
				System.err.println( "warning: ProbabilityRewrite.Preview.topLevelAncestorDialog() caught " + thrown );
			}
			try{
				sleep( LONG_PACK_DELAY );
				container.pack();
			}catch( Throwable thrown ){
				System.err.println( "warning: ProbabilityRewrite.Preview.topLevelAncestorDialog() caught " + thrown );
			}
		}

		/** @since 20071211 */
		public Window showFrame(){
			Task.preview.action( ProbabilityRewrite.this ).setEnabled( false );

			JComponent contents = Preview.this.getPanel();
			contents.setBorder( createEmptyBorder( 4, 8, 8, 8 ) );

			JFrame frame = new JFrame( "probability rewrite preview #" + (INT_PREVIEW_COUNTER++) );
			frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
			frame.addWindowListener( Preview.this );

			frame.getContentPane().add( contents );
			setMenuBar( frame );
			frame.pack();
			Util.centerWindow( frame );
			frame.setVisible( true );

			return frame;
		}

		public void windowClosed( WindowEvent windowevent ){
			Task.preview.action( ProbabilityRewrite.this ).setEnabled( true );
		}

		private JComponent vstrut( JComponent pnl, GridBagConstraints c ){
			return vstrut( 0x4, pnl, c );
		}

		private JComponent vstrut( int size, JComponent pnl, GridBagConstraints c ){
			pnl.add( DEBUG_STRUTS ? new JLabel( "vstrut" ) : Box.createVerticalStrut( size ), c );
			return pnl;
		}

		public JComponent getPanel(){
			if( panel == null ){
				List<Throwable> thrown = new LinkedList<Throwable>();

				Constituent[] probablistix = Constituent.getProbabilistic( caste().blacklist() );

				JPanel pnll          = new JPanel( new GridBagLayout() );
				GridBagConstraints c = new GridBagConstraints();
				c.anchor             = GridBagConstraints.WEST;

				c.weightx            = 1;
				c.fill               = GridBagConstraints.HORIZONTAL;
				c.gridwidth          = GridBagConstraints.REMAINDER;

				State        state;
				try{
					for( Constituent constituent : probablistix ){
						if( (state = get( constituent )) == null ){
							this.constituency.consummate( constituent, null );
							continue;
						}
						vstrut(                             pnll, c );
						pnll.add(                state.caption(), c );
						vstrut(                             pnll, c );
						pnll.add( state.getCPTDisplayComponent(), c );

						this.constituency       .add( constituent, state.caption()                );
						this.constituency.consummate( constituent, state.getCPTDisplayComponent() );
					}
				}
				catch( Throwable throwable ){ thrown.add( throwable ); }

				JPanel pnlr          = null;

				if( white( Constituent.statistics ) ){
				pnlr                 = this.constituency.consummate( Constituent.statistics, new JPanel( new GridBagLayout() ) );
				c                    = new GridBagConstraints();

				c.gridwidth          = 1;
				c.weightx            = 1;
				pnlr.add( Box.createHorizontalStrut( 0x10 ), c );

				final AbstractButton cb = PreviewOption.MODELS.newButton( box, idpreview, PreviewOption.linkedba );
				c.weightx            = 0;
				c.gridwidth          = GridBagConstraints.REMAINDER;
				pnlr.add( cb, c );

				c.weighty            = 1;
				vstrut(    8, pnlr, c );
				c.weighty            = 0;

				c.weightx            = 1;
				pnlr.add( switches, c );

				myState.put( Constituent.statistics, new State( Constituent.statistics, pnlr ) );

				pnlr.setBorder( createEmptyBorder( 0, 0x10, 0, 0 ) );
				}

				try{
					panel                = new JPanel( new GridBagLayout() );
					c                    = new GridBagConstraints();
					c.gridwidth          = GridBagConstraints.REMAINDER;
					c.fill               = GridBagConstraints.HORIZONTAL;
					c.weightx            = 1;
					panel.add(             this.menubar = Preview.this.menus.bar( idpreview ), c );
					menubarborderdefault = this.menubar.getBorder();
					menubar.setBorder( createCompoundBorder( createEmptyBorder( 0, 0, 0x10, 0 ), createEtchedBorder() ) );

					c.gridwidth          = 1;
					panel.add(                              pnll, c );

					c.anchor             = GridBagConstraints.SOUTHEAST;
					c.gridwidth          = GridBagConstraints.REMAINDER;
					c.fill               = GridBagConstraints.VERTICAL;
					panel.add( pnlr == null ? createGlue() : pnlr, c );
				}
				catch( Throwable throwable ){ thrown.add( throwable ); }

				ButtonModel[] bmodelz = null;
				try{ bmodelz = PreviewOption.MODELS.models( additive, idpreview, PreviewOption.linkedba, PreviewOption.linkedsa ); }
				catch( Throwable throwable ){ thrown.add( throwable ); }

				try{ link( Constituent.before, Constituent.after, PreviewOption.linkedba, bmodelz[0] ); }
				catch( Throwable throwable ){ thrown.add( throwable ); }

				try{ link( Constituent.source, Constituent.after, PreviewOption.linkedsa, bmodelz[1] ); }
				catch( Throwable throwable ){ thrown.add( throwable ); }

				try{
					for( Constituent constituent : probablistix ){ if( (state = get( constituent )) != null ){ state.behave(); } }

					int height_allowable = screen.height - 0x100,  height_total  = 0;
					for( Constituent constituent : probablistix ){ if( (state = get( constituent )) != null ){ height_total += state.preferredHeight(); } }
					if( height_total > height_allowable ){
						float scaledown = ((float) height_allowable) / ((float)height_total);
					  //System.out.println( "height_total? " + height_total + ", height_allowable? " + height_allowable + ", scaledown? " + scaledown );
						for( Constituent constituent : probablistix ){ if( (state = get( constituent )) != null ){ state.scaleHeight( scaledown ); } }
					}
				}
				catch( Throwable throwable ){ thrown.add( throwable ); }

			  //try{ this.constituency.setSelected( false, idpreview, Constituent.effective ); }
			  //catch( Throwable throwable ){ thrown.add( throwable ); }

				reportErrors( thrown, "Preview.getPanel()" );
			}
			return panel;
		}

		/** @since 20071224 */
		public Preview link( Constituent top, Constituent bottom, PreviewOption option, final ButtonModel bmodel )
		{
			if( ! allwhite( top, bottom, option ) ){ return this; }

			final JScrollBar sbtop      = get( top    ).editor.getScrollBar();
			final JScrollBar sbbottom   = get( bottom ).editor.getScrollBar();

			final int        mxtop      = sbtop    .getMaximum();
			final int        mxbottom   = sbbottom .getMaximum();

			final           AdjustmentListener al = new AdjustmentListener(){
				public void adjustmentValueChanged(     AdjustmentEvent e  ){
					if( ! bmodel.isSelected() ) return;

					Adjustable source   = e.getAdjustable(), other;
					int        value    = source.getValue(), max = -1;
					if( source         == sbtop ){
						other           = sbbottom;
						max             = mxbottom;
					}else{
						other           = sbtop;
						max             = mxtop;
					}
					if( value <= max ){ other.setValue( value ); }
				}
			};
			sbtop    .addAdjustmentListener( al );
			sbbottom .addAdjustmentListener( al );

			return this;
		}

		public SamiamAction getActionWrite(){
			if( previewActionWrite != null ) return previewActionWrite;

			previewActionWrite = new SamiamAction( "write stats", "write statistics in X format to Y", 'w', null ){
				{ setAccelerator( firstUniqueKeyStroke( getValue( Action.NAME ).toString() ) );
				  putValueProtected( Property.menu.key, PreviewMenu.file );
				}

				public void actionPerformed( ActionEvent event ){
					ProbabilityRewrite.this.run( this, true );
				}

				public void run(){
					Redirect redirect = null;
					try{
						redirect = Redirect.selected( idpreview );
						Preview.this.rewriteresult.append( redirect.open( null, null ) );
					}catch( Throwable thrown ){
						System.err.println( "warning: failed to write output in " + Format.human.get( display ) + " format to " + redirect.get( display ) + ", caught " + thrown );
					}
				}
			};

			return previewActionWrite;
		}

		/** @since 20071213 */
		private Preview setMenuBar( JDialog dialog ){
			if( FLAG_SETJMENUBAR ){ dialog.setJMenuBar( Preview.this.extractMenuBar() ); }
			return this;
		}

		/** @since 20071213 */
		private Preview setMenuBar( JFrame frame ){
			if( FLAG_SETJMENUBAR ){ frame.setJMenuBar( Preview.this.extractMenuBar() ); }
			return this;
		}

		/** @since 20071213 */
		private JMenuBar extractMenuBar(){
			if( Preview.this.panel.isAncestorOf( Preview.this.menubar ) ) Preview.this.panel.remove( Preview.this.menubar );
			if( Preview.this.menubar.getBorder() != Preview.this.menubarborderdefault ) Preview.this.menubar.setBorder( Preview.this.menubarborderdefault );
			return Preview.this.menubar;
		}

		private void react(){
			for( State state : myState.values() ) state.react();
		}
		public void  actionPerformed( ActionEvent event ){ react(); }

		/** @since 20071211 */
		public class State{
			public State( Constituent constituent, FiniteVariable variable ){
				this( constituent );
				this.variable = variable;
				this.data     = this.variable.getCPTShell().getCPT().dataclone();
			}

			public State( Constituent constituent, JComponent component ){
				this( constituent );
				this.component = component;
			}

			private State( Constituent constituent ){
				this.constituent = constituent;
			}

			public JComponent caption(){
				if( this.caption == null ) this.caption = new JLabel( constituent.caption( Preview.this.rewriteresult ) );
				return this.caption;
			}

			/** @since 20071210 */
			public JComponent getCPTDisplayComponent(){
				if( ! this.constituent.probabilistic ) return null;

				if( this.component != null ) return this.component;

				DisplayableFiniteVariableImpl    dvar       =   new DisplayableFiniteVariableImpl( this.variable );
				this.editor                                 =   new CPTEditor( dvar, null );
				editor.setEditable( false );
				DefaultTableCellRenderer         renderer   =   new DefaultTableCellRenderer();
				ProbabilityDataHandler           handler    =   new ProbabilityDataHandler( this.data, renderer, new DefaultTableCellRenderer(), null );
				JComponent                       ret        =   editor.makeCPTDisplayComponent( handler );

				if( ! this.constituent.stained ) return this.component = ret;

				Stainer                          stainer    =   new Stainer( rewriteresult, get( Constituent.before ).data, get( Constituent.after ).data, editor.getTableModelHGS() );
				handler.setStainWright( stainer );
				if( this.constituent == Constituent.after ) switches = stainer.getSwitches();

				this.to_repaint = editor.getTempJTable();
				stainer.addListener( Preview.this );

			  //stainer.setEnabled( ! caste().blacklist().containsAll( ValueFunction.SET ) );
				stainer.setEnabled( white( Constituent.statistics ) );

				return this.component = ret;
			}

			public State behave(){
				Dimension dswitches        = white( Constituent.statistics ) ? get( Constituent.statistics ).component.getPreferredSize() : new Dimension(0,0), dim = null;
				if( (dim =  component.getPreferredSize()).width + dswitches.width > screen.width ){
					int   available        = screen.width - dswitches.width - 0x40;
					component.setPreferredSize( new Dimension( available, dim.height ) );
				}
				return this;
			}

			public State scaleHeight( float factor ){
				if( component == null ) return this;

				Dimension pref = component.getPreferredSize();
				pref.height    = (int) (((float) pref.height) * factor);
				component.setPreferredSize( pref );

				return this;
			}

			public int preferredHeight(){
				if( component == null ) return 0;
				else return component.getPreferredSize().height;
			}

			public State react(){
				if( to_repaint != null ) to_repaint.repaint();
				return this;
			}

			public    Constituent       constituent;
			public    FiniteVariable    variable;
			public    double[]          data;
			public    CPTEditor         editor;
			public    JComponent        component, caption, to_repaint;
		}

		public State get( Constituent constituent ){
			return myState.get( constituent );
		}

		private        Object                   id_view_constituency = new Object();
		private        JComponent               panel, switches;
		private        JMenuBar                 menubar;
		private        Border                   menubarborderdefault;
		private        Map<Constituent,State>   myState    = new EnumMap<Constituent,State>( Constituent.class );
		private        RewriteResult            rewriteresult;
		private        SamiamAction             previewActionWrite;
		private        boolean                  flaglink   = true;

		public   final Dimension                screen   = Util.getScreenBounds().getSize();
	}

	/** @since 20071211 */
	public enum Constituent implements Actionable<Constituent>, Semantic, Dendriform<PreviewMenu>{
		before     ( true,  true,  "<html><b>destination cpt before:"  ),
		source     ( true,  false, "<html><b>source cpt:"              ),
		effective  ( true,  false, "<html><b>effective source cpt:", true ){
			public             String    caption( RewriteResult rr ){
				if( (rr == null) || (rr.unused == null) || rr.unused.isEmpty() ){
				      return caption; }
				else{
					int    forgotten = rr.unused.size();
					String noun      = forgotten > 1 ? "variables" : "variable";
					return "<html><b>effective source cpt ("+forgotten+" parent "+noun+" forgotten):";
				}
			}
		},
		after      ( true,  true,  "<html><b>destination cpt after:"   ),//computed in "+rewriteresult.elapsed+" ms:"
		statistics ( false, false, "run statistics" );

		private Constituent( boolean probabilistic, boolean stained, String caption ){
			this( probabilistic, stained, caption, false );
		}

		private Constituent( boolean probabilistic, boolean stained, String caption, boolean flag_shy ){
			this.probabilistic = probabilistic;
			this.stained       = probabilistic && stained;
			this.caption       = caption;

			this.properties.put(     display,                       name()              );
			this.properties.put(     tooltip, "show '" +            name() + "' panel"  );
			this.properties.put( accelerator, firstUniqueKeyStroke( name() )            );
			if( flag_shy ){ this.properties.put( shy, Boolean.TRUE ); }
		}

		/** @since 20071225 */
		public             String    caption( RewriteResult rr ){
			return                   caption;
		}

		public    final    boolean   probabilistic, stained;
		public    final    String    caption;

	  //private   static   Constituent[] PROBABILISTIC;

		/** @since 20071213 */
		public static Constituent[] getProbabilistic( Collection<?> blacklist ){
		  //if( PROBABILISTIC != null ) return PROBABILISTIC;
			LinkedList<Constituent> list = new LinkedList<Constituent>();
			for( Constituent constituent : values() ){ if( constituent.probabilistic && (! blacklist.contains( constituent )) ){ list.add( constituent ); } }
			return /*PROBABILISTIC = */list.toArray( new Constituent[ list.size() ] );
		}

		/** @since 20071217 */
		public PreviewMenu     parent(){ return PreviewMenu.view; }

		/** @since 20071217 */
		public Semantics    semantics(){ return         additive; }

		/** @since 20071211 */
		public Constituent getDefault(){ return            after; }

		/** @since 20071211 */
		public Object get( Property property ){ return this.properties.get( property ); }

		/** @since 20071211 */
		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		/** @since 20071211 */
		public static AbstractButton[] buttons( View view, Object id, ItemListener listener, Collection<Constituent> selected ){
			return MODELS.newButtons( view, id, listener, EnumSet.allOf( Constituent.class ), selected );
		}

		/** @since 20071211 */
		public static Constituent selected( Object id ){
			return MODELS.selected( exclusive, id );
		}

		/** @since 20071211 */
		public static Set<Constituent> selected( Object id, Set<Constituent> results ){
			return MODELS.selected(  additive, id, results);
		}

		/** @since 20071211 */
		public static EnumModels<Constituent> MODELS = new EnumModels<Constituent>( Constituent.class );
	}

	/** @since 20071204 */
	private static class Pair{
		public Pair( String source, String destination ){
			this( source, destination, null );
		}

		public Pair( String source, String destination, Caste caste ){
			this.source      = source;
			this.destination = destination;
			this.caste       = caste;
		}
		private String source, destination;
		private Caste  caste;
	}

	/** test/debug */
	public static void main( String[] args ){
		try{
			Util.DEBUG_VERBOSE                   = true;
			edu.ucla.belief.VariableImpl.setStringifier( VARIABLE_LABEL );

			String arg0                          = args.length>0 ? args[0] : "";

			List<Pair>                   cases   = new LinkedList<Pair>();
			String                       network = null;
			JOptionResizeHelperListener listener = null;
			if(      arg0.equals( "barley3" ) ){
				CASTE_DEBUG    = Caste.developer;
				network        = "barley.net";
				cases.add( new Pair( "exptgens", "exptgens", Caste.retard ) );
				cases.add( new Pair( "exptgens",      "tkv", Caste.expert ) );
				cases.add( new Pair(      "tkv", "exptgens", Caste.expert ) );
			  /*listener = new JOptionResizeHelperListener(){
					public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
						cptcopy.setHintsVisible( true, true );//cptcopy.getActionHints().actionP( this );
					}
				};*/
			}
			else if( arg0.equals( "cancer3" ) ){
				CASTE_DEBUG    = Caste.expert;
				network        = "cancer.net";
				cases.add( new Pair( "D", "D" ) );
				cases.add( new Pair( "D", "C" ) );
				cases.add( new Pair( "C", "D" ) );
			}
			else{
				if( (arg0 != null) && (arg0.length() > 0) ){ CASTE_DEBUG = Caste.valueOf( arg0 ); }

				network        = "cancer.net";
				cases.add( ProbabilityRewrite.getInstance().caste().afunctional() ? new Pair( "D", "D" ) : new Pair( "C", "D" ) );
			}

			if( arg0.equals( "borders" ) ) DEBUG_BORDERS = true;

			BeliefNetwork   bn = NetworkIO.read( new File( System.getenv( "NETWORKSPATH" ), network ) );

			Util.setLookAndFeel();

			FiniteVariable src, dest;
			for( Pair pair : cases ){
				src               =   (FiniteVariable) bn.forID( pair.source      );
				dest              =   (FiniteVariable) bn.forID( pair.destination );
				ProbabilityRewrite.getInstance().caste( pair.caste ).showDialog( bn, src, dest, "CPT Copy - TEST/DEBUG", null, listener );
			}
		}catch( Throwable thrown ){
			thrown.printStackTrace();
		}finally{
			System.exit(0);
		}
	}
}
