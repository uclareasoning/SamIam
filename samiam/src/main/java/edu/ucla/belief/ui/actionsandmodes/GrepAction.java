package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.actionsandmodes.Grepable.Flag;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Language;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Redirect;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Redirect.Dest;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Angle;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Presentation;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Vocabulary;
import static edu.ucla.belief.ui.actionsandmodes.Grepable.Preset.simpleliteral;
import edu.ucla.belief.ui.actionsandmodes.Grepable.GrepOptions;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Stimulus;
import edu.ucla.belief.ui.actionsandmodes.Grepable.FlagPole;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Filter;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Amnesiac;
import edu.ucla.belief.ui.actionsandmodes.Grepable.Elephant;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.dialogs.PackageOptionsDialog;
import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.util.JOptionResizeHelper;
import edu.ucla.belief.ui.util.JOptionResizeHelper.JOptionResizeHelperListener;
import edu.ucla.belief.ui.util.Dropdown;
import static edu.ucla.belief.ui.util.Util.DEBUG_VERBOSE;
import static edu.ucla.belief.ui.actionsandmodes.Grepable.Redirect.txt2devnull;
import static edu.ucla.belief.ui.actionsandmodes.GrepAction.Vantage.foresight;
import static edu.ucla.belief.ui.actionsandmodes.GrepAction.Vantage.hindsight;
import static edu.ucla.belief.ui.actionsandmodes.GrepAction.Payoff.fruitful;

import edu.ucla.util.Stringifier;

import static java.lang.System.currentTimeMillis;
import java.util.List;
import java.util.regex.Pattern;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.text.View;

/** Reusable grep action.

	@author keith cascio
	@since  20070320 */
public class GrepAction<E,F extends Enum<F>,H> extends InterruptableAction
{
	public GrepAction( Grepable<E,F,H> grepable ){
		super( "grep", grepable.grepInfo(), 'g', null );
		setTarget( grepable );
	}

	public void runImpl( Object arg1 ) throws InterruptedException{
		try{
			getInputComponent();
			myState                =        this.snapshot();
			GrepOptions<F> options = myDashboard.memorize();
			grep( filter( myDashboard.myTFPattern.getText(), options ).blame( "search pattern" ), options );
		}catch( InterruptedException exception ){
			throw exception;
		}catch( Exception exception ){
			System.err.println( "warning: GrepAction.runImpl() caught " + exception );
			if( DEBUG_VERBOSE ) exception.printStackTrace();
		}finally{
			myState = null;
		}
	}

	/** {@link #promptAutosuggest promptAutosuggest()}, {@link #delegate delegate()} return this if the user cancels the search */
	public static final Filter CANCEL = new Amnesiac( null, EnumSet.noneOf( Flag.class ) );

	/** @since 20070417 */
	public long grep( Filter filter, GrepOptions<F> options ) throws Exception{
		long matches = 0;
		try{
			if( (filter = delegate( filter, foresight )) == CANCEL ) return matches;
		}catch( Exception exception ){
			System.err.println( "warning: GrepAction.grep() (foresight) caught " + exception );
			if( DEBUG_VERBOSE ) exception.printStackTrace();
		}

		Redirect      redirect = Redirect.forFlags( filter.flags() );
		if( redirect == null )   redirect =  txt2devnull;
		Collection<E> results  = redirect.dead() ? BLACKHOLE : new LinkedList<E>();
		results.clear();
		Stringifier stringifier = getStringifier();
		matches = myGrepable.grep( filter, options.selected, stringifier, myState, results );

		if( (! results.isEmpty()) && (! redirect.dead()) ){
			String[] sorted = new String[ results.size() ];
			int i=0;
			if( stringifier == null ) for( E result : results ) sorted[ i++ ] = result.toString();
			else                      for( E result : results ) sorted[ i++ ] = stringifier.objectToString( result );
			Arrays.sort( sorted );
			redirect.open( myNIF, Integer.toString( sorted.length ) + " lines" ).app( sorted ).flush();
		}

		try{
			if( (filter = delegate( filter, hindsight )) == CANCEL ) return matches;
		}catch( Exception exception ){
			System.err.println( "warning: GrepAction.grep() (hindsight) caught " + exception );
			if( DEBUG_VERBOSE ) exception.printStackTrace();
		}

		if( (filter != null) && (filter != CANCEL) ) return grep( filter, myDashboard.memorize() );

		return matches;
	}

	/** If we don't need hindsight, {@link edu.ucla.belief.ui.actionsandmodes.Grepable.Amnesiac Amnesiac} will give us slightly better performance than {@link edu.ucla.belief.ui.actionsandmodes.Grepable.Elephant Elephant}.
		@since 20070515 */
	public Filter filter( String pattern, GrepOptions<F> options ){
		if( myFlagTrim ) pattern = pattern.trim();

		Set<Autosuggest> hindseers = myPredistributed[ hindsight.ordinal() ];

		return (hindseers == null) || hindseers.isEmpty() ?
			new Amnesiac( pattern, options.flags ) :
			new Elephant( pattern, options.flags );
	}

	/** @since 20070515
		@param filter If the passed in filter is already derived from a suggestion, this method will simply return the fallback without attempting to derive again.
		@return {@link #CANCEL CANCEL} if user cancelled, or a filter if we should proceed to grep again, else null
		@since 20070419 */
	public Filter delegate( Filter filter, Vantage vantage ) throws Exception{
		/*System.out.append( "GrepAction.delegate( " );
		filter.append( System.out ).append( ", " ).append( vantage.name() ).append( " )\n" );*/
		Filter ret = vantage.fallback( filter );
		if( Autosuggest.isSuggestion( filter ) ) return ret;
		Set<Autosuggest> distributed = myPredistributed[ vantage.ordinal() ];
		if( (distributed != null) && (! distributed.isEmpty()) ){
			ret = promptAutosuggest( ret, vantage.delegate( filter, distributed ) );
			vantage.reflect( GrepAction.this );
		}
		return ret;
	}

	/** @since 20070417 */
	public Filter promptAutosuggest( Filter past, Filter future ) throws Exception{
		if( future == null ) return past;

		Autosuggest origin = (Autosuggest) future.origin();
		Component   parent = SwingUtilities.getWindowAncestor( getInputComponent() );
		if( parent == null ) parent = getInputComponent();
		String      dname  = GrepAction.this.getValue( Action.NAME ).toString();
		String      title  = dname + " " + origin.name() + " auto-suggestion";
		JComponent  msg    = getSuggestComponent( future.description(), dname, future.origin(), parent );

		new JOptionResizeHelper( msg, true, 0x1000 ).start();
		int         result = JOptionPane.showConfirmDialog( parent, msg, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE );

		if( myBtnDiscontinue.isSelected() ) applyDisable( origin );

		switch( result ){
			case JOptionPane.CANCEL_OPTION: return CANCEL;
			case JOptionPane.YES_OPTION:
				if( myBtnFutureSearches.isSelected() ) myDashboard.derive( future );
				return future;
			case JOptionPane.NO_OPTION:
			default:                        return past;
		}
	}

	/** <a href='http://forums.java.net/jive/message.jspa?messageID=201685'>inspired here</a>
		@since 20070418 */
	public static class WrappableHTMLWidthConstraints implements PropertyChangeListener{
		public WrappableHTMLWidthConstraints( float min ){
			myMin = min;
		}

		public float widen( float max ){
			return myPref = Math.max( myMin, max );
		}

		public void propertyChange( PropertyChangeEvent e ){
			//System.out.println( "propertyChange( "+e.getPropertyName()+": "+e.getOldValue()+" -> "+e.getNewValue()+" )" );
			if( e.getNewValue() == null ) return;

			final JComponent c     = (JComponent) e.getSource();
			//c.removePropertyChangeListener( "ancestor", this );
			final View       view  = (View) c.getClientProperty( "html" );
			if( view == null ) return;

			final float      prefx = view.getPreferredSpan( View.X_AXIS );
			final float      prefy = view.getPreferredSpan( View.Y_AXIS );

			if( myPref >= prefx ) return;

			view.setSize( Math.min( myPref, prefx ), 0x400 );
			SwingUtilities.invokeLater( new Runnable(){
				public void run(){
					try{
						if( thread == null ){
							(thread = new Thread( this )).start();
							return;
						}
						Thread.sleep( 0x80 );
						view.setSize( prefx, prefy );
					}catch( Exception exception ){
						System.err.println( "warning: GrepAction.WrappableHTMLWidthConstraints.Runnable.run() caught " + exception );
					}
				}
				private Thread thread;
			} );
		}

		private float myMin, myPref;
	}

	/** @since 20070417 */
	static private JComponent getSuggestComponent( String description, String action, Object origin, Component parent ){
		try{
			if( myPromptComponent == null ){
				JPanel             panel = new JPanel( new GridBagLayout() );
				GridBagConstraints c     =             new GridBagConstraints();
				c.anchor    = GridBagConstraints.NORTHWEST;

				ButtonGroup group = new ButtonGroup();
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.weightx   = 1;
				c.fill      = GridBagConstraints.HORIZONTAL;
				panel.add( myLabelDescription = new JLabel(), c );
				myLabelDescription.setFont( myLabelDescription.getFont().deriveFont( (float)14 ) );
				panel.add( Box.createVerticalStrut( 0x10 ), c );
				panel.add( myLabelOptionYes   = new JLabel(), c );
				panel.add( Box.createVerticalStrut( 2 ), c );
				panel.add( myLabelOptionNo    = new JLabel( "Click \"No\"  to use the options you selected." ), c );
				panel.add( Box.createVerticalStrut( 2 ), c );
				panel.add(                      new JLabel( "Click \"Cancel\" to abort this search." ), c );

				c.weightx   = 0;
				c.fill      = GridBagConstraints.NONE;
				panel.add( Box.createVerticalStrut( 8 ), c );
				c.gridwidth = 1;
				c.anchor    = GridBagConstraints.WEST;
				panel.add( new JLabel( "Apply this suggestion to:" ), c );
				panel.add( Box.createHorizontalStrut( 8 ), c );
				c.anchor    = GridBagConstraints.NORTHWEST;
				panel.add( myBtnThisSearch     = new JRadioButton( "this search only"   ), c );
				group.add( myBtnThisSearch );
				c.gridwidth = GridBagConstraints.REMAINDER;
				panel.add( myBtnFutureSearches = new JRadioButton( "my search settings" ), c );
				group.add( myBtnFutureSearches );

				panel.add( Box.createVerticalStrut( 4 ), c );
			  //c.anchor    = GridBagConstraints.EAST;
				c.weightx   = 1;
				c.fill      = GridBagConstraints.HORIZONTAL;
				panel.add( myBtnDiscontinue = new JCheckBox(), c );//"<html><font color='#660000'>disable</font> foresight suggestions" ), c );

				panel.setBorder( BorderFactory.createEmptyBorder( 8,4,8,4 ) );

				myWHTMLWC = new WrappableHTMLWidthConstraints( 0x100 );
				myLabelDescription.addPropertyChangeListener( "ancestor", myWHTMLWC );
				myLabelOptionYes  .addPropertyChangeListener( "ancestor", myWHTMLWC );
				myLabelOptionNo   .addPropertyChangeListener( "ancestor", myWHTMLWC );

				myPromptComponent = panel;
			}

			myLabelDescription.setText( description );
			myLabelOptionYes  .setText( "<html>Click \"Yes\" to run <font color='#cc6600'>" + action + "</font> using the option(s) suggested here." );
			myWHTMLWC         .widen( parent.getSize().width/2 );
			myBtnThisSearch   .setSelected( true  );
			myBtnDiscontinue  .setSelected( false );
			myBtnDiscontinue  .setText( "disable "+origin.toString()+" auto-suggest" );
		}catch( Exception exception ){
			System.err.println( "warning: GrepAction.getSuggestComponent() caught " + exception );
			exception.printStackTrace();
			myPromptComponent = null;
		}
		return myPromptComponent;
	}

	static private JComponent     myPromptComponent;
	static private AbstractButton myBtnThisSearch, myBtnFutureSearches, myBtnDiscontinue;
	static private JLabel         myLabelDescription, myLabelOptionYes, myLabelOptionNo;
	static private WrappableHTMLWidthConstraints myWHTMLWC;

	/** test/debug */
	public static void main( String[] args ){
		Filter     past   = new Elephant( "figgledeefoodeefiggledeefoodeefoo*", EnumSet.of( Flag.re ) );
		Filter     future = Autosuggest.syntactic.suggest( past, foresight );

		edu.ucla.belief.ui.util.Util.setLookAndFeel();

		Component  parent = new JFrame();
		parent.setBounds( 0x100,0x100,0x400,0x400 );
		String     dname  = "find";
		String     title  = dname + " auto-suggestion";
		JComponent msg    = getSuggestComponent( future.description(), dname, future.origin(), parent );

		new JOptionResizeHelper( msg, true, 0x1000 )/*.setListener( new JOptionResizeHelperListener(){
			public void topLevelAncestorDialog( JDialog dia, JOptionResizeHelper h ){ dia.pack(); }
		} )*/.start();
		int        result = JOptionPane.showConfirmDialog( parent, msg, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE );

		System.exit(0);
	}

	/** @since 20070416 */
	private final Collection<E> BLACKHOLE = new AbstractCollection<E>(){
		public void clear(){
			this.size = 0;
		}

		public boolean add( E obj ){
			++this.size;
			return true;
		}

		public int size(){
			return this.size;
		}

		public Iterator<E> iterator(){
			return new Iterator<E>(){
				public boolean hasNext(){ return false; }
				public E          next(){ return null;  }
				public void     remove(){               }
			};
		}

		private int size = 0;
	};

	/** Characterize the result of an attempted auto-suggestion.
		@since 20070515 */
	public enum Payoff{
		/** time well spent,        but not more than 0x2000 ms == 8.2 seconds */
		fruitful ( 0x2000, "fruitful but expensive",    "to make the suggestion" ),
		/** impotent, that's ok if it takes less than  0x200 ms == 0.5 seconds */
		fruitless(  0x200, "ineffective and expensive", "with no payoff"         );// -1 );

		/** null suggestion means fruitless */
		static public Payoff categorize( Filter suggestion ){
			return suggestion == null ? fruitless : fruitful;
		}

		private Payoff( long threshold, String descrip, String phrase ){
			this.threshold   = threshold;
			this.description = descrip;
			this.phrase      = phrase;
		}

		/** consider it waste if we spend longer than this number of milliseconds */
		final public long   threshold;
		/** for use in messages to the user */
		final public String description, phrase;
	}

	/** Monitor max time usage for an enumerated set of "expenditures". Report waste.
		@since 20070515 */
	static public class Cheapskate<J extends Enum<J>>{
		public Cheapskate( J[] expenditures, long init_theshold ){
			this.expenditures = expenditures;
			this.maxima       = new long[ expenditures.length ];
			this.thresholds   = new long[ expenditures.length ];

			Arrays.fill( this.maxima,     Long.MIN_VALUE );
			Arrays.fill( this.thresholds, init_theshold  );
		}

		/** Possibly bump up the maximum. */
		public J log( J expenditure, long elapsed ){
			int     ordinal   = expenditure.ordinal();
			if( elapsed > maxima[ ordinal ] ){
				maxima[ ordinal ] = elapsed;
				untouched = false;
			}
			return expenditure;
		}

		/** Report the maximum for a single expenditure. */
		public long elapsed( J expenditure ){
			return maxima[ expenditure.ordinal() ];
		}

		/** Report wasteful expenditures and redefine "wasteful".  i.e.
			This method bumps up the wastefulness thresholds to the current maxima.
			This policy avoids nagging the user. */
		public Set<J> wasteful(){
			Set<J> ret = null;
			if( untouched ) return ret;
			try{
				for( int i=0; i < expenditures.length; i++ ){
					if( maxima[i] > thresholds[i] ){
						if( ret == null ) ret = EnumSet.of( expenditures[i] );
						else              ret         .add( expenditures[i] );

						thresholds[i] = maxima[i] + 1;
					}
				}
			}catch( Exception exception ){
				System.err.println( "warning: Cheapskate.wasteful() caught " + exception );
			}finally{
				untouched = true;
			}
			return ret;
		}

		private       J[] expenditures;
		private    long[] maxima, thresholds;
		private boolean   untouched = false;
	}

	/** With respect to a given grep search.
		@since 20070515 */
	public enum Vantage{
		/** before */
		foresight,
		/** during */
		insight,
		/** after */
		hindsight{
			public Filter fallback( Filter filter ){ return null; }
		};

		/** if we make no suggestion, we fall back on this */
		public     Filter fallback( Filter filter ){ return filter; }

		/** give each helper a chance to make a suggestion, keeping track of payoff and logging time expense */
		public Filter delegate( Filter filter, Set<Autosuggest> helpers ) throws Exception{
			if( filter == null || helpers == null || helpers.isEmpty() ) return filter;

			Filter suggestion;
			if( FLAG_MONITOR_AUTOSUGGEST_COST ){
				Payoff payoff;
				long   pre;
				for( Autosuggest helper : helpers ){
					if( ! helper.supports( (Vantage) this ) ) continue;
					untouched = false;
					pre     = currentTimeMillis();
					payoff  = Payoff.categorize( suggestion = helper.suggest( filter, (Vantage) this ) );
					cheapskates[ payoff.ordinal() ].log( helper, currentTimeMillis() - pre );
					if( payoff == fruitful ) return suggestion;
				}
			}
			else for( Autosuggest helper : helpers ) if( (suggestion = helper.suggest( filter, (Vantage) this )) != null ) return suggestion;

			return null;
		}

		@SuppressWarnings( "unchecked" )
		private Vantage(){
			this.cheapskates = new Cheapskate[ Payoff.values().length ];
			for( Payoff payoff : Payoff.values() ) cheapskates[ payoff.ordinal() ] = new Cheapskate<Autosuggest>( Autosuggest.values(), payoff.threshold );
		}

		public static final double DOUBLE_SECONDS_PER_MILLISECOND = (double)1/(double)1000;
		public static      boolean FLAG_MONITOR_AUTOSUGGEST_COST  = true;

		/** Time to ask ourselves, was it all really worth it? */
		public int reflect( GrepAction grepaction ){
			if( untouched ) return -1;

			int disabled = 0;
			try{
				Cheapskate<Autosuggest> cheapskate;
				Set       <Autosuggest> waste;
				StringBuilder           buff = null;
				for( Payoff payoff : Payoff.values() ){
					if( (waste = (cheapskate = cheapskates[ payoff.ordinal() ]).wasteful()) == null || waste.isEmpty() ) continue;
					for( Autosuggest exhorbitant : waste ){
						if(  buff == null ) buff = new StringBuilder( 0x100 );
						else buff.setLength(0);

						buff.append( "<html>Disable "       ).append( payoff.description ).append( ' ' ).append( grepaction.getValue( NAME ) ).append( " auto-suggest type <font color='#0000cc'><b>" ).append( exhorbitant.name() ).append( "</b></font>?\n" )
							.append( "<html>Spent <font color='#006600'><b>" ).append( ((double)cheapskate.elapsed( exhorbitant )) * DOUBLE_SECONDS_PER_MILLISECOND )
							.append( "</b></font> seconds " ).append( payoff.phrase      ).append( '.' );
						int result = JOptionPane.showConfirmDialog( grepaction.myNIF, buff, "minor performance penalty", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );

						if( result == JOptionPane.YES_OPTION ){
							grepaction.applyDisable( exhorbitant );
							++disabled;
						}
					}
				}
			}catch( Exception exception ){
				System.err.println( "warning: Vantage.reflect() caught " + exception );
			}finally{
				untouched = true;
			}
			return disabled;
		}

		/** Assign a set of helpers to non-mutually-exclusive subsets that support each type of Vantage
			Do this for performance and meaningfulness. */
		@SuppressWarnings( "unchecked" )
		static public Set<Autosuggest>[] distribute( Set<Autosuggest> helpers ){
			Set<Autosuggest>[] distribution = new Set[ values().length ];
			try{
				int ord;
				for( Autosuggest helper : helpers ){
					for( Vantage vantage : values() ){
						if( helper.supports( vantage ) ){
							if( distribution[ ord = vantage.ordinal() ] == null ) distribution[ ord ] = EnumSet.of( helper );
							else                                                  distribution[ ord ]         .add( helper );
						}
					}
				}
			}catch( Exception exception ){
				System.err.println( "warning: Vantage.distribute() caught " + exception );
				Arrays.fill( distribution, helpers );
			}
			return distribution;
		}

		private Cheapskate<Autosuggest>[] cheapskates;
		private boolean                   untouched = true;
	}

	/** Help the user pick the search flags implied by the search pattern.
		@since 20070416 */
	public enum Autosuggest{
		/** foresight: can we look at the pattern in isolation and decide the user intended a different language? */
		syntactic{
			public Filter suggest( Filter filter, Vantage vantage ){
				if( ! supports( vantage ) ) return null;
				if(   filter  == null     ) return filter;

				Language    selected = Language.forFlags( filter.flags() );
				if(         selected == null ) return null;
				Language recommended = selected.recommend( filter.expression() );

				if( recommended == null ) return null;
				else return describeLanguageRecommendation( filter, filter.derive( recommended.flag ).blame( this ).reset() );
			}

			/** @since 20070515 */
			public boolean supports( Vantage vantage ){
				return vantage == foresight;
			}
		},
		/** hindsight: can we turn search failure into success by re-running the search with different options? (more expensive) */
		semantic{
			public Filter suggest( Filter filter, Vantage vantage ){
				try{
					if( ! supports( vantage )                   ) return null;
					if( (filter == null) || (filter.hits() > 0) ) return null;

					Thread thread = Thread.currentThread();

					Set<Flag> impotents    = Angle.language.project( filter.flags() );
					Set<Flag> alternatives = EnumSet.copyOf( Angle.language.membership );
					alternatives.removeAll( impotents );

					Filter derived;
					for( Flag alternative : alternatives ){
						if( thread.isInterrupted() ) break;
					  //System.out.println( "regrep() for " + alternative );
						try{
							if( (derived = filter.derive( alternative )).regrep() >= 1 ) return describeLanguageRecommendation( filter, derived.blame( this ).reset() );
						}catch( Exception exception ){
							System.err.println( "warning: GrepAction.Autosuggest.hindsight.hindsight() caught " + exception );
							if( DEBUG_VERBOSE ) exception.printStackTrace();
						}
					}
				}catch( Exception exception ){
					System.err.println( "warning: GrepAction.Autosuggest.hindsight.hindsight() caught " + exception );
					if( DEBUG_VERBOSE ) exception.printStackTrace();
				}
				return null;
			}

			/** @since 20070515 */
			public boolean supports( Vantage vantage ){
				return vantage == hindsight;
			}
		};

		public static final Set<Autosuggest> ALL     = Collections.unmodifiableSet( EnumSet.allOf( Autosuggest.class ) );

		/** @since 20070420 */
		static public boolean isSuggestion( Filter filter ){
			return (filter == null) ? false : ALL.contains( filter.origin() );
		}

		/** Leave our mark on the filter so we know where it came from and where it went. */
		static private Filter describeLanguageRecommendation( Filter selected, Filter derived ){
			Object origin = selected.origin();
			return derived
				.describe( "<html>The " )
				.describe( origin == null ? "search pattern" : origin.toString() )
				.describe( " you entered: <font size='5' color='#000066'><b>&nbsp;" )
				.describe( selected.expression()    )
				.describe( "&nbsp;</b></font> looks more like a <font color='#006600'><b>" )
				.describe(  derived.language().name )
				.describe( "</b></font> than a <font color='#990000'><b>" )
				.describe( selected.language().name )
				.describe( "</b></font>." );
		}

		/** The essential idea: hazard a guess.
			@since 20070515 */
		public Filter suggest( Filter filter, Vantage vantage ){
			return null;
		}

		/** Different types of auto-suggest work from different vantage points.
			@since 20070515 */
		public boolean supports( Vantage vantage ){
			return false;
		}
	}

	/** @since 20070424 */
	public NetworkInternalFrame contextualize( NetworkInternalFrame nif ){
		return myNIF = nif;
	}

	/** @since 20070324 */
	public H snapshot(){
		return null;
	}

	public void setTarget( Grepable<E,F,H> grepable ){
		myGrepable = grepable;
	}

	/** @since 20070321 */
	public void setStringifierSelector( Stringifier.Selector selector ){
		myStringifierSelector = selector;
	}

	/** override this method for custom toString() behavior e.g. selecting between variable id/label */
	public Stringifier getStringifier(){
		if( myStringifierSelector != null ) return myStringifierSelector.selectStringifier();
		return null;
	}

	private   Grepable<E,F,H>         myGrepable;
	protected              H          myState;
	private   Stringifier.Selector    myStringifierSelector;
	private   NetworkInternalFrame    myNIF;
	protected Dashboard               myDashboard;
	protected JComponent              myWidget;
	private   Set<Autosuggest>        myAutosuggest = Autosuggest.ALL;
	private   Set<Autosuggest>[]      myPredistributed;
	private   boolean                 myFlagTrim    = true;

	/** @since 20070329 */
	protected class Dashboard extends JPanel{
		public Dashboard( String describe ){
			super( new GridBagLayout() );
			myDescription = describe;
			init();
		}

		private void init(){
			(myTFPattern          = new JTextField( 10 )).addActionListener( myListener );

			F[]      fields = null;
			Class<F> clazz  = GrepAction.this.myGrepable.grepFields();
			if( (clazz != null) && ((fields = clazz.getEnumConstants()) != null) && (fields.length > 1) ){
			  //EnumSet<F> defs   = GrepAction.this.myGrepable.grepFieldsDefault();
			  //mySelector      = new JComboBox( fields );
			  //mySelector        = new JList( fields );
			  //mySelector.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				mySelector      = new Dropdown<F>( fields, new int[]{ 1, fields.length }, null );
				mySelector.addActionListener( myListener );
				mySelector.setSelection( GrepAction.this.myGrepable.grepFieldsDefault() );
				myBucket        = mySelector.emptyBucket();
			}

			JScrollPane pain = null;
			if( mySelector != null ){
				mySelector.setToolTipText( "choose fields to consider for matches" );
			  //pain = new JScrollPane( mySelector );
			  //pain.setPreferredSize( new Dimension( mySelector.getPreferredSize().width + 0x20, myTFPattern.getPreferredSize().height ) );
			  //pain.addMouseListener( myMouseListener );
				mySelector.addMouseListener( myMouseListener );
			}
		  //myPain = pain;

			Vocabulary vocab = simpleliteral.vocabulary;
			myOptions = new GrepOptions<F>( vocab, GrepAction.this.myGrepable.grepFieldsDefault(), "" );
			setVocabulary( vocab );
			myOptions.recall( mySelector, vocab, myTFPattern );

			redescribe();//homogenize();

			Dashboard.this            .addMouseListener( myMouseListener );
			Dashboard.this.myTFPattern.addMouseListener( myMouseListener );
		}

		/** @since 20070403 */
		public Vocabulary setVocabulary( Vocabulary vocab ){
			try{
			if( myVocabulary != null ) myOptions.memorize( myVocabulary );
			vocab = myVocabulary = new Vocabulary( vocab );
			//myOptions.recall( vocab );//unnecessary
			Dashboard.this.removeAll();

			GridBagConstraints c = new GridBagConstraints();
			c.weightx   = 0;
			c.fill      = GridBagConstraints.NONE;
		  //if( myPain != null ){
			if( mySelector != null ){
			  //this.add( myPain,                       c );
				this.add( mySelector,                   c );
				this.add( Box.createHorizontalStrut(2), c );
			}

			JComponent compVo = vocab.asComponent( myOptions.explicit() );
			if( compVo != null ){
				this.add( compVo,  c );
				myFlagPoleGlob = vocab.projection.get( Angle.language ).listen( Dashboard.this.myListener );
				for( Angle angle : Angle.values() ){
					vocab.projection.get( angle ).listen( Dashboard.this.myListener );
				}
				vocab.addMouseListener( myMouseListener );
			}

			c.weightx   = 1;
			c.anchor    = GridBagConstraints.WEST;
			c.fill      = GridBagConstraints.HORIZONTAL;
			this.add( myTFPattern,                  c );

			c.weightx   = 0;
			c.fill      = GridBagConstraints.NONE;
			myConstraints = c;

			if( myExtensions != null ) for( Component comp : myExtensions ) this.add( comp, myConstraints );

			BUNDLE_OF_PREFERENCES.setPreference( GrepAction.this, 3 );
			Dashboard.this.revalidate();
			homogenize();
			}catch( Exception exception ){
				exception.printStackTrace();
			}

			return vocab;
		}

		/** @since 20070418 */
		public Vocabulary derive( Filter future ){
			Vocabulary derivative = null;
			try{
				derivative = myVocabulary.derive( future.flags() );
				if( derivative != myVocabulary ){
					if( myPreferences != null ){
						Preference prefVocab = myPreferences.getMappedPreference( SamiamPreferences.STR_GREP_VOCABULARY );
						prefVocab.setValue( derivative );
						prefVocab.setRecentlyCommittedFlag( true );
						if( UI.STATIC_REFERENCE != null ){
							UI.STATIC_REFERENCE.changePackageOptions( myPreferences );
							prefVocab.setRecentlyCommittedFlag( false );
						}
					}
				}
			}catch( Exception exception ){
				System.err.println( "warning: GrepAction.Dashboard.derive() caught " + exception );
				exception.printStackTrace();
			}finally{
				if( derivative != myVocabulary ) setVocabulary( derivative );
			}
			return derivative;
		}

		public void setEnabled( boolean flag ){
			super.setEnabled( flag );
			Component[] children = this.getComponents();
			for( int i=0; i<children.length; i++ ) children[i].setEnabled( flag );
		}

		/** @since 20100110 */
		protected Dashboard reTip(){
			String postfix = (myDescription == null) ? "": (" against <font color='#339900'>" + myDescription);
			for( Language language : Language.values() ){ myTips.put( language, language.tip + postfix ); }
			return this;
		}

		protected void homogenize(){
			if( myTips.isEmpty() ){ reTip(); }
			try{
				Language language = Language.forFlags( myFlagPoleGlob.flags( Stimulus.ANY ) );
				if( language == null ) language = Language.re;
				if( Dashboard.this == GrepAction.this.myDashboard ){
					GrepAction.this.setName( language.action );
				}
				myTFPattern.setToolTipText( myTips.get( language ) );
			}catch( Exception exception ){
				System.err.println( "warning: GrepAction.homogenize() caught " + exception );
			}
		}

		/** @since 20100110 */
		public Dashboard redescribe(){
			try{
				if( mySelector != null ){
					StringBuilder buff = new StringBuilder( 0x20 );
					myBucket.clear();
					for( F selected : mySelector.getSelection( myBucket ) ){ buff.append( selected.toString() ).append( "/" ); }
					buff.setLength( buff.length() - 1 );
					myDescription = buff.toString();
				}
				reTip();
				homogenize();
			}catch( Throwable thrown ){
				System.err.println( "warning: Dashboard.redescribe() caught " + thrown );
			}
			return this;
		}

		protected ActionListener myListener = new ActionListener(){
			public void actionPerformed( ActionEvent event ){
				Object src = event.getSource();
				if( src == myTFPattern    ) GrepAction.this.actionPerformed( event );
				else{
					if(      src == myFlagPoleGlob ){ homogenize(); }
					else if( src == mySelector     ){ redescribe(); }
					if( (event.getModifiers() & InputEvent.BUTTON1_MASK) > 0 ) myTFPattern.requestFocus();
				}
			}
		};

		/** @since 20070404 */
		protected MouseListener myMouseListener = new MouseAdapter(){
			public void  mousePressed( MouseEvent event ){
				popup( event );
			}

			public void  mouseClicked( MouseEvent event ){
				popup( event );
			}

			public void mouseReleased( MouseEvent event ){
				popup( event );
			}

			public boolean popup( MouseEvent e ){
				if( ! e.isPopupTrigger() ) return false;

				Point p = e.getPoint();
				SwingUtilities.convertPointToScreen( p, e.getComponent() );
				showPopup( p );
				return true;
			}

			public JPopupMenu showPopup( Point coordScreen ){
				if( myPopupMenu == null ){
					myPopupMenu = new JPopupMenu();
					myPopupMenu.add( myCaption = new JLabel() );
					myPopupMenu.addSeparator();
					myPopupMenu.add( myItemPrefs = new JMenuItem( action_Preferences ) );

					myCaption.setForeground( Color.green.darker().darker() );
				}

				myCaption.setText( " " + GrepAction.this.getValue( Action.NAME ) );
				//myItemPrefs.setSelected( false );

				myPopupMenu.setLocation( coordScreen );
				myPopupMenu.setInvoker( Dashboard.this );
				myPopupMenu.setVisible( true );
				return myPopupMenu;
			}

			private JPopupMenu myPopupMenu;
			private JLabel     myCaption;
			private JMenuItem  myItemPrefs;
		};

		public GrepOptions<F> memorize(){
			return myOptions.memorize( mySelector, myVocabulary, myTFPattern );
		}

		public Dashboard get(){
			homogenize();
			return this;
		}

		public Component extend( Component comp ){
			this.add( comp, myConstraints );
			if( myExtensions == null ) myExtensions = new LinkedList<Component>();
			myExtensions.add( comp );
			return comp;
		}

	  //public JComboBox               mySelector;
	  //public JList                   mySelector;
		public Dropdown<F>             mySelector;
		public Collection<F>           myBucket;
	  //public JScrollPane             myPain;
		public FlagPole                myFlagPoleGlob;
		public JTextField              myTFPattern;
		public GrepOptions<F>          myOptions;
		public GridBagConstraints      myConstraints;
		public String                  myDescription;
		public Map<Language,String>    myTips = new EnumMap<Language,String>( Language.class );
		public Vocabulary              myVocabulary;
		public List<Component>         myExtensions;
	}

	/** @since 20070404 */
	public  final  SamiamAction action_Preferences = new RunPreferences();
	private static int          INT_THREAD_COUNTER = 0;

	/** Open grep preferences tab in the preference dailog.
		@since 20070404 */
	public class RunPreferences extends SamiamAction implements Runnable{
		public RunPreferences(){
			super( "preferences", "edit grep preferences", 'p', null );
		}

		public void actionPerformed( ActionEvent event ){
			try{
				if( UI.STATIC_REFERENCE == null ) return;
				RunPreferences.this.start();
				UI.STATIC_REFERENCE.action_PREFERENCES.actionP( RunPreferences.this );
			  //while( UI.STATIC_REFERENCE.getPackageOptionsDialog() == null ){ Thread.sleep( 0x10 ); }
			  //while( UI.STATIC_REFERENCE.getPackageOptionsDialog() != null ){ Thread.sleep( 0x1000 ); }
			}catch( Exception exception ){
				System.err.println( "warning: GrepAction.RunPreferences.aP() caught " + exception );
			}
		}

		public void run(){
			try{
				Thread.sleep( 0x10 );

				UI ui = UI.STATIC_REFERENCE;

				PackageOptionsDialog pod = null;
				for( int i=0; ((pod = ui.getPackageOptionsDialog()) == null) && i<0x40; i++ ){
					Thread.sleep( 0x40 );
				}
				if( pod == null ){
					System.err.println( "GrepAction.RunPreferences.run() timed out waiting for preference dialog" );
					return;
				}
				pod.setSelected( GrepAction.this.myPreferences.getPreferenceGroup( SamiamPreferences.STR_KEY_GROUP_GREP ) );
			}catch( Exception exception ){
				System.err.println( "warning: GrepAction.RunPreferences.run() caught " + exception );
			}
		}

		public Thread start(){
			Thread ret = new Thread( (Runnable) RunPreferences.this, getClass().getName() + " " + INT_THREAD_COUNTER++ );
			ret.start();
			return ret;
		}
	};

	public JComponent getInputComponent(){
		if( myDashboard == null ) myDashboard = new Dashboard( "variable names" );
		return myDashboard.get();
	}

	public static AbstractButton configure( AbstractButton btn, int headroom, int legroom ){
		try{
			btn.setMargin( new Insets( headroom,legroom,headroom,legroom ) );
			btn.setFont( btn.getFont().deriveFont( (float)10 ) );
		}catch( Exception exception ){
			System.err.println( "warning: GrepAction.configure() caught " + exception );
		}
		return btn;
	}

	/** @since 20070326 */
	public JComponent newPanel(){
		return newPanel( null );
	}

	/** @since 20070326 */
	public JComponent newPanel( String tip ){
		AbstractButton     btnGrep   = configure( new JButton( this ), 1, 4 );
		if( tip != null )  btnGrep.setToolTipText( tip );
		JPanel             pnlBtn    = new JPanel( new GridLayout(1,1) );
		pnlBtn.add( btnGrep );
		pnlBtn.setBorder( BorderFactory.createEmptyBorder(0,0,0,4) );

		JPanel             pnlGrep   = new JPanel( new GridBagLayout() );
		GridBagConstraints c         = new GridBagConstraints();
		c.anchor  = GridBagConstraints.NORTHWEST;
		c.fill    = GridBagConstraints.NONE;
		c.weightx = 0;
		pnlGrep.add( pnlBtn,                       c );
	  //pnlGrep.add( Box.createHorizontalStrut(4), c );
		c.fill    = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		pnlGrep.add( this.getInputComponent(),     c );

		myWidget = pnlBtn;
	  //pnlBtn.setVisible( false );
		if( myPreferences != null ){
			GrepAction.validatePreferenceBundle( myPreferences );
			GrepAction.this.setPreferences();
		}

		return pnlGrep;
	}

	/** @since 20070403 */
	public Vocabulary setVocabulary( Vocabulary vocab ){
		if( myDashboard != null ) myDashboard.setVocabulary( vocab );
		return vocab;
	}

	/** @since 20070416 */
	public Set<Autosuggest> setAutosuggest( Set<Autosuggest> autosuggest ){
		Set<Autosuggest> past = myAutosuggest;
		myPredistributed = Vantage.distribute( myAutosuggest = autosuggest );
		return past;
	}

	/** @since 20070422 */
	public Set<Autosuggest> applyDisable( Autosuggest autosuggest ){
		Set<Autosuggest> future = EnumSet.copyOf( myAutosuggest );
		future.remove( autosuggest );
		applyAutosuggestPreference( future );
		return future;
	}

	/** @since 20070418 */
	public boolean applyAutosuggestPreference( Set<Autosuggest> autosuggest ){
		if( myPreferences == null ) return false;
		try{
			Preference prefAutosuggest = myPreferences.getMappedPreference( SamiamPreferences.STR_GREP_AUTOSUGGEST );
			prefAutosuggest.setValue( autosuggest );
			prefAutosuggest.setRecentlyCommittedFlag( true );
			if( UI.STATIC_REFERENCE != null ){
				UI.STATIC_REFERENCE.changePackageOptions( myPreferences );
				prefAutosuggest.setRecentlyCommittedFlag( false );
				return true;
			}
		}catch( Exception exception ){
			System.err.println( "warning: GrepAction.applyAutosuggestPreference() caught " + exception );
		}
		return false;
	}

	/** @since 20070501 */
	public boolean setHighlight( boolean highlight ){
		if(     myDashboard != null )
			if( myDashboard.myVocabulary != null )
				myDashboard.myVocabulary.setHighlight( highlight );
		return highlight;
	}

	/** @since 20070828 */
	public boolean setTrim( boolean trim ){
		return myFlagTrim = trim;
	}

	/** @since 20070402 */
	public boolean setGrepButtonVisible( Object value ){
		if( myWidget == null ) return false;
		try{
			myWidget.setVisible( Boolean.TRUE.equals( value ) );
		}catch( Exception exception ){
			System.err.println( "warning: GrepAction.setGrepButtonVisible() caught " + exception );
		}
		return myWidget.isVisible();
	}

	/** interface PreferenceListener */
	public void updatePreferences(){
		try{
			BUNDLE_OF_PREFERENCES.updatePreferences( GrepAction.this );
		}catch( Exception exception ){
			System.err.println( "warning: GrepAction.updatePreferences() caught " + exception );
		}
	}
	/** interface PreferenceListener */
	public void previewPreferences(){
		try{
			BUNDLE_OF_PREFERENCES.previewPreferences( GrepAction.this );
		}catch( Exception exception ){
			System.err.println( "warning: GrepAction.previewPreferences() caught " + exception );
		}
	}
	/** interface PreferenceListener */
	public void setPreferences(){
		try{
			BUNDLE_OF_PREFERENCES.setPreferences( GrepAction.this );
		}catch( Exception exception ){
			System.err.println( "warning: GrepAction.setPreferences() caught " + exception );
		}
	}

	/** @since 20070402 */
	public SamiamPreferences setPreferences( SamiamPreferences pref ){
		super.setPreferences( pref );
		GrepAction.validatePreferenceBundle( pref );
		return pref;
	}

	protected static       TargetedBundle BUNDLE_OF_PREFERENCES;
	public    static final String         STR_KEY_PREFERENCE_BUNDLE = GrepAction.class.getName();

	/** @since 20070402 */
	private static TargetedBundle validatePreferenceBundle( SamiamPreferences prefs ){
		if( BUNDLE_OF_PREFERENCES != null ) return BUNDLE_OF_PREFERENCES.validate( prefs );

		String   key  = STR_KEY_PREFERENCE_BUNDLE;
		String[] keys = new String[] { SamiamPreferences.STR_SHOW_GREP_BUTTON, SamiamPreferences.STR_GREP_VOCABULARY, SamiamPreferences.STR_GREP_AUTOSUGGEST, SamiamPreferences.STR_GREP_HIGHLIGHT, SamiamPreferences.STR_GREP_TRIM };

		BUNDLE_OF_PREFERENCES = new TargetedBundle( key, keys, prefs ){
			public void begin( Object me, boolean force ){
				this.grepaction = (GrepAction) me;
			}

			@SuppressWarnings( "unchecked" )
			public void setPreference( int index, Object value ){
				switch( index ){
					case 0:
						grepaction.setGrepButtonVisible( value );
						break;
					case 1:
						grepaction.setVocabulary( (Vocabulary) value );
						break;
					case 2:
						grepaction.setAutosuggest( (Set<Autosuggest>) value );
						break;
					case 3:
						grepaction.setHighlight( ((Boolean) value).booleanValue() );
						break;
					case 4:
						grepaction.setTrim( ((Boolean) value).booleanValue() );
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			private GrepAction grepaction;
		};

		return BUNDLE_OF_PREFERENCES;
	}
}
