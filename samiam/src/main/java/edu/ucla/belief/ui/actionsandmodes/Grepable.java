package edu.ucla.belief.ui.actionsandmodes;

import        edu.ucla.util.Stringifier;
import        edu.ucla.belief.FiniteVariable;
import static edu.ucla.belief.ui.preference.SamiamPreferences.STR_GREP_DISPLAY_NAME_LOWER;
import static edu.ucla.belief.ui.UI.STR_SAMIAM_ACRONYM;
import static edu.ucla.belief.ui.util.Util.copyToSystemClipboard;
import static edu.ucla.belief.ui.util.Util.warn;
import static edu.ucla.belief.ui.util.Util.STREAM_TEST;
import        edu.ucla.belief.ui.NetworkInternalFrame;
import        edu.ucla.belief.ui.util.JOptionResizeHelper;
import        edu.ucla.belief.ui.util.Dropdown;
import static edu.ucla.belief.ui.actionsandmodes.Grepable.Presentation.PATTERN_DELIMITERS;
import static edu.ucla.belief.ui.toolbar.MainToolBar.getIcon;

import        edu.ucla.belief.ui.dialogs.EnumModels;
import static edu.ucla.belief.ui.dialogs.EnumModels.firstUniqueKeyStroke;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable;
import        edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property;
import static edu.ucla.belief.ui.dialogs.EnumModels.Actionable.Property.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Semantics;
import static edu.ucla.belief.ui.dialogs.EnumModels.Semantics.*;
import        edu.ucla.belief.ui.dialogs.EnumModels.Semantics.Semantic;
import        edu.ucla.belief.ui.dialogs.EnumModels.View;
import static edu.ucla.belief.ui.dialogs.EnumModels.View.*;

import        java.util.regex.*;
import        java.util.*;
import static java.util.EnumSet.of;
import static java.util.EnumSet.copyOf;
import static java.util.EnumSet.noneOf;
import static java.util.EnumSet.allOf;
import        java.awt.Component;
import        java.awt.Dimension;
import        java.awt.GridBagLayout;
import        java.awt.GridBagConstraints;
import        java.awt.Color;
import        java.awt.event.ActionListener;
import        java.awt.event.ActionEvent;
import        java.awt.event.ItemListener;
import        java.awt.event.InputEvent;
import        java.awt.event.KeyEvent;
import        java.awt.Font;
import        java.awt.Shape;
import        java.awt.Graphics2D;
import        java.awt.font.*;
import        java.awt.geom.*;
import        javax.swing.*;
import static java.awt.event.KeyEvent.*;
import static java.awt.event.InputEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.BorderFactory.*;
import        java.io.IOException;
import        java.io.File;
import        java.io.FileWriter;
import        java.io.PrintWriter;

/** Objects that support grep.

	@author keith cascio
	@since  20070320 */
public interface Grepable<E,F extends Enum<F>,H>
{
	/** @param results        Meant to be an in/out parameter, i.e. if you pass<br />
		<table border='1' cellpadding='4'>
		<tr><td>(1)</td><td>a collection object SET</td><td rowspan='2'>then</td><td>fill SET with the grep results</td><td rowspan='2'>and return</td><td>SET</td></tr>
		<tr><td>(2)</td><td>null                   </td><td>that means you aren't interested in a Collection of the results</td><td>null</td></tr>
		</table>
		@param field_selector Set of {@link #fields fields} to grep over.
		@param stringifier    Convert field values to strings.
		@param exp            Expression defines the class of matches.
		@param state          Implementation-specific, meaningful only in some contexts esp where grep carries out some editing action.
		@return the number of matches
	*/
	public             long grep( Filter filter, EnumSet<F> field_selector, Stringifier stringifier, H state, Collection<E> results ) throws InterruptedException;
  //public <T super E> Collection<T> grep( String exp, Set<Flag> flags, EnumSet<F> field_selector, Stringifier stringifier, Collection<T> results );

	/** If your implementation need not differentiate fields, return {@link Simple Grepable.Simple.class}
		@see #grep
		@return an enumerated type defining the possible fields we can grep over, allows
		you to select fields when you {@link #grep grep()}
	*/
	public Class<F> grepFields();

	/** If your implementation need not differentiate fields, return {@link Simple#SINGLETON Grepable.Simple.SINGLETON}
		@return default selected field */
	public EnumSet<F> grepFieldsDefault();

	/** @return usage information suitable for a tool tip */
	public String grepInfo();

	/** expression + flags = filter
		<br />
		since 20070416, also supports fallback regrepping
		@since 20070329 */
	public interface Filter extends edu.ucla.structure.Filter{
		public Language     language();
		public String     expression();
		public Set<Flag>       flags();
		public Pattern       pattern();
		public Matcher       matcher( String line );
		public boolean        accept( String line );
		public Filter          reset();
		public int            regrep();
		public Filter         derive( Flag future );
		public Filter       describe( Object descrip );
		public String    description();
		public Filter          blame( Object origin );
		public Object         origin();
		public int              hits();
		public char        delimiter();
		public Appendable   informal( Appendable app ) throws IOException;
		public Appendable  canonical( Appendable app ) throws IOException;
		public Filter        valueOf( String canonical );
	}

	/** a "forgetful" {@link Filter Filter},
		cheaper than {@link Elephant Elephant}:
		regrep() and hits() throw {@link java.lang.UnsupportedOperationException UnsupportedOperationException}

		@since 20070515 */
	public static class Amnesiac implements Filter{
		public Amnesiac( String expression, Set<Flag> flags ){
			this.expression = expression;
			this.flags      = flags == null ?  null : Collections.unmodifiableSet( flags );
			this.invert     = flags == null ? false : flags.contains( Flag.invert );
		}

		/** @since 20070517 */
		public Map<String,String> decompose( Map<String,String> map ){
			String lexpression = expression();
			map.put( "pattern", lexpression == null ? "" : lexpression );

			Set<Flag> lflags = flags();
			if( lflags == null || lflags.isEmpty() ) return map;

			StringBuilder buff = new StringBuilder( lflags.size() * 8 );
			for( Flag flag : lflags ) buff.append( flag.name() ).append( ',' );
			buff.setLength( buff.length() - 1 );
			map.put( "flags", buff.toString() );

			return map;
		}

		/** @since 20070517 */
		public Filter               compose( Map<String,String> map ){
			String lexpression = map.get( "pattern" );
			if( lexpression == null ) lexpression = "";
			Set<Flag> lflags = EnumSet.noneOf( Flag.class );
			String sflags      = map.get( "flags" );
			if( sflags != null && sflags.length() > 0 ){
				for( String token : PATTERN_DELIMITERS.split( sflags ) ){
					try{
						lflags.add( Flag.valueOf( token ) );
					}catch( Exception exception ){
						System.err.println( "warning: Amnesiac.compose() caught " + exception );
					}
				}
			}
			return new Amnesiac( lexpression, lflags );
		}

		public Appendable   informal( Appendable app ) throws IOException{
			if(   flags.contains( Flag.invert   ) ) app.append( Flag.invert    .symbol ).append( ' ' );
			for( Flag lang : Angle.language.project( flags ) ) app.append( lang.name ).append( ' ' );
			char delim = delimiter();
			app.append( delim ).append( expression ).append( delim );
			if( ! flags.contains( Flag.heedcase ) ) app.append( Flag.ignorecase.symbol );
			return app;
		}

		/** @since 20070516 */
		public Appendable  canonical( Appendable app ) throws IOException{
			char delim = delimiter();
			app.append( delim );
			if( expression != null ) app.append( expression );
			app.append( delim );
			if( flags != null ) for( Flag flag : flags ) app.append( flag.name() ).append( ',' );
			return app;
		}

		/** @since 20070516 */
		public Filter        valueOf( String canonical ){
			if( (canonical == null) || (canonical.length() < 1) ) return null;

			char    delimiter = canonical.charAt( 0 );
			int         index = canonical.indexOf( delimiter, 1 );
			String expression = canonical.substring( 1, index );

			Set<Flag>   flags = EnumSet.noneOf( Flag.class );
			try{
				for( String token : PATTERN_DELIMITERS.split( canonical.substring( index+1 ) ) ) flags.add( Flag.valueOf( token ) );
			}catch( Exception exception ){
				System.err.println( "warning: Grepable.Amnesiac.valueOf() caught " + exception );
			}

			return new Amnesiac( expression, flags );
		}

		/** @since 20070516 */
		public char        delimiter(){
			if( (expression == null) || (expression.indexOf( '/' ) < 0) ) return '/';
			else if(                     expression.indexOf( '#' ) < 0  ) return '#';
			else if(                     expression.indexOf( '!' ) < 0  ) return '!';
			else if(                     expression.indexOf( '$' ) < 0  ) return '$';
			else if(                     expression.indexOf( '%' ) < 0  ) return '%';
			else{
				for( int i = 40; i<0x10FFFF; i++ ){
					if( ( Character    .isDefined(i)) &&
					    (!Character .isISOControl(i)) &&
					    (!Character .isWhitespace(i)) && (expression.indexOf(i) < 0) ) return (char)i;
				}
			}
			return (char)7;
		}

		public Language     language(){
			return Language.forFlags( flags );
		}

		public String     expression(){
			return this.expression;
		}

		public Set<Flag>       flags(){
			return this.flags;
		}

		public Pattern       pattern(){
			if( this.pattern == null ) this.pattern = Language.compile( this.expression, this.flags );
			return this.pattern;
		}

		public Matcher       matcher( String line ){
			if( this.matcher == null ) return this.matcher = this.pattern().matcher( line );
			else                       return this.matcher.reset( line );
		}

		public boolean        accept( String line ){
			return this.matcher( line ).find() ^ this.invert;
		}

		/** interface FiniteVariable.Filter
			@since 20070419 */
		public boolean        accept( Object line ){
			return accept( line.toString() );
		}

		/** @since 20070416 */
		public Filter          reset(){
			return this;
		}

		/** unsupported!
			@since 20070416 */
		public int            regrep(){
			throw new UnsupportedOperationException();
		}

		/** @return an Amnesiac
			@since 20070416 */
		public Filter         derive( Flag future ){
			return new Amnesiac( this.expression, Angle.substitute( this.flags, future ) );
		}

		/** @since 20070417 */
		public Filter       describe( Object descrip ){
			if( this.description == null ) this.description = new StringBuilder( 0x100 );
			if(      descrip     == null ) this.description.setLength( 0       );
			else                           this.description   .append( descrip );
			return this;
		}

		/** @since 20070417 */
		public String    description(){
			return (this.description == null) ? "" : this.description.toString();
		}

		/** @since 20070419 */
		public Filter          blame( Object origin ){
			this.author = origin;
			return this;
		}

		/** @since 20070419 */
		public Object         origin(){
			return this.author;
		}

		/** unsupported!
			@since 20070419 */
		public int              hits(){
			throw new UnsupportedOperationException();
		}

		/** @since 20070516 */
		public boolean equals( Object o ){
			if( !(o instanceof Filter) ) return false;
			Filter    other  = (Filter)o;
			String    oexpre = other.expression();
			Set<Flag> oflags = other.flags();
			return (expression == null ? oexpre == null : expression.equals( oexpre )) &&
			       (     flags == null ? oflags == null :      flags.equals( oflags ));
		}

		/** @since 20070516 */
		public int hashCode(){
			int hash = 0;
			if( expression != null ) hash += expression.hashCode();
			if(      flags != null ) hash +=      flags.hashCode();
			return hash;
		}

		protected final String        expression;
		private         Pattern       pattern;
		private         Matcher       matcher;
		protected final Set<Flag>     flags;
		protected final boolean       invert;
		private         StringBuilder description;
		private         Object        author;
	}

	/** a "retentive" {@link Filter Filter},
		more expensive than {@link Amnesiac Amnesiac}:
		supports fallback regrepping,
		useful for hindsight a.k.a. semantic auto-suggestion

		@since 20070515 */
	public static class Elephant extends Amnesiac implements Filter{
		public  Elephant( String expression, Set<Flag> flags ){
			this( expression, flags, new HashSet<String>() );
		}

		private Elephant( String expression, Set<Flag> flags, Set<String> fallback ){
			super( expression, flags );
			this.fallback   = fallback;
		}

		/** @since 20070516 */
		public Filter        valueOf( String canonical ){
			Filter amnesiac = super.valueOf( canonical );
			return new Elephant( amnesiac.expression(), amnesiac.flags() );
		}

		/** accept() is where Elephant incurs extra expense. */
		public boolean        accept( String line ){
			fallback.add( line );
			boolean accept =   this.matcher( line ).find() ^ this.invert;
			if(     accept ) ++this.hits;
			return  accept;
		}

		/** Elephant has lots of state to reset.
			@since 20070416 */
		public Filter          reset(){
			if( fallback.getClass().getSimpleName().equals( "HashSet" ) ) fallback.clear();
			else fallback = new HashSet<String>();
			this.hits = 0;
			return this;
		}

		/** Elephant supports regrep()
			@since 20070416 */
		public int            regrep(){
			int count = 0;

			Thread thread = Thread.currentThread();
			for( String line : fallback ){
				if( thread.isInterrupted() ) return count;
				if( this.matcher( line ).find() ^ this.invert ) ++count;
			}
			return count;
		}

		/** @return an Elephant
			@since 20070416 */
		public Filter         derive( Flag future ){
			return new Elephant( this.expression, Angle.substitute( this.flags, future ), Collections.unmodifiableSet( this.fallback ) );
		}

		/** Elephant supports hits()
			@since 20070419 */
		public int              hits(){
			return this.hits;
		}

		private       Set<String>   fallback;
		private       int           hits;
	}

	/** Flags that modify grep's default behavior. */
	public static enum Flag{
		/** normal sense, i.e. no inversion */
		ninvert("\u203c", "!!", invert(), false, "normal",      "normal sense, i.e. no inversion" ),
		/** invert the sense of the grep i.e. grep -v */
		invert(      "!",  ninvert,       true,  "invert",      "invert the sense of the match, i.e. grep -v" ),
		/** interpret the search string as a full regular expression */
		re(          "$",  glob(), false, false, "regexp",      "interpret the search string as a full regular expression" ),
		/** interpret the search string as a glob pattern instead of a full regular expression */
		glob(        "*",  re,            true,  "wildcard",    "interpret the search string as a wildcard pattern a.k.a. 'globbing' pattern; special characters '*' and '?'" ),
		/** interpret the search string literally, no characters have special meaning */
		literal("\u2261", "=", re,        false, "literal",     "interpret the search string literally, no characters have special meaning" ),
		/** case-insensitive matching */
		ignorecase(  "i",  heedcase(),    false, "ignore",      "case-insensitive matching" ),
		/** pay attention to upper/lower case */
		heedcase(   "aA",  ignorecase,    true,  "heed",        "pay attention to upper/lower case" ),
		/** any part of a word "\u25d4" "\u25f4" "\u2384" */
		part(    "\u25d7", "pw", null,    false, "part",        "any part of a word" ),
		/** whole words only "\u25cf" "\u25d9" */
		whole(   "\u25cf", "ww", part,    true,  "whole",       "whole words only" ),
		/** take no action for non-matches, leave them alone */
		additive(    "+",  destructive(), false, "additive",    "take no action for non-matches, leave them alone" ),
		/** take action on non-matches as well as matches */
		destructive( "-",  additive,      true,  "destructive", "take action on non-matches as well as matches" ),

		/** no text output */
		txt2devnull( ">0",                    null, false,        "silent",  "no text output" ),
		/** print results to SamIam's internal console */
		txt2console( ">:",             txt2devnull, false, false, "console", "print results to "+STR_SAMIAM_ACRONYM+"'s internal console" ),
		/** print results to standard output stream */
		txt2stdout ( ">1",             txt2devnull, false, false, "stdout",  "print results to standard output stream" ),
		/** print results to standard error stream */
		txt2stderr ( ">2",             txt2devnull, false, false, "stderr",  "print results to standard error stream"  ),
		/** popup a dialog with text results ">\u2612" ">\u22a0" */
		txt2dialog ( ">\u22a1", ">d",  txt2devnull, false, false, "popup",   "popup a dialog with text results"        ),
		/** append to a file */
		txt2file   ( ">/",      ">f",  txt2devnull, false, false, "file",    "append text results to a file"           ),
		/** copy results to system clipboard ">\u2026" */
		txt2copy   ( ">\u2025", ">cb", txt2devnull,  true,        "copy",    "copy results to system clipboard"        );

		/** @since 20070423 */
		private Flag( String symbol, String alternate, Flag negate, boolean primary, String name, String descrip ){
			this( alternate, negate, primary, name, descrip );
			postpone( symbol, alternate );
		}

		private Flag( String symbol, Flag negate, boolean primary, String name, String descrip ){
			this( symbol, negate, primary, ! primary, name, descrip );
		}

		/** @since 20070423 */
		private Flag( String symbol, String alternate, Flag negate, boolean primary, boolean implicit, String name, String descrip ){
			this( alternate, negate, primary, implicit, name, descrip );
			postpone( symbol, alternate );
		}

		private Flag( String symbol, Flag negate, boolean primary, boolean implicit, String name, String descrip ){
			this.symbol      = symbol;
			this.negate      = negate;
			this.primary     = primary;
			this.implicit    = implicit;
			this.name        = name;
			this.description = descrip;
		}

		/** @since 20091229 */
		private Flag postpone( final String... symbols ){
			Runnable runnable = new Runnable(){
				public void run(){
					try{
						int iterations = java.awt.GraphicsEnvironment.isHeadless() ? 2 : 0x80;
						for( int i=0; (edu.ucla.belief.ui.UI.STATIC_REFERENCE == null) && i<iterations; i++ ){ Thread.sleep( 0x80 ); }
						Flag.this.symbol = supported( symbols );
					}catch( Throwable thrown ){
						System.err.println( "warning: Flag.postpone() caught " + thrown );
					}
				}
			};
			new Thread( runnable ).start();
			return this;
		}

		/** @since 20091229 */
		public String symbol(){
			return this.symbol;
		}

		public Flag negate(){
			return this.negate;
		}

		static private Flag invert(){
			return invert;
		}

		static private Flag glob(){
			return glob;
		}

		static private Flag heedcase(){
			return heedcase;
		}

		static private Flag destructive(){
			return destructive;
		}

		/** a 1 or 2 character symbol */
		private      String  symbol;
		/** the flag the represents the negation of this flag */
		private      Flag    negate;
		/** display name */
		public final String  name;
		/** the meaning explained */
		public final String  description;
		/** is this Flag the primary expression of its Angle */
		public final boolean primary;
		/** is this Flag the implicit value of its Angle */
		public final boolean implicit;

		static{
			ninvert    .negate = invert;
			invert     .negate = ninvert;
			re         .negate = glob;
			glob       .negate = re;
			literal    .negate = re;
			ignorecase .negate = heedcase;
			heedcase   .negate = ignorecase;
			whole      .negate = part;
			part       .negate = whole;
			additive   .negate = destructive;
			destructive.negate = additive;
			txt2devnull.negate = txt2copy;
			txt2console.negate = txt2devnull;
			txt2stdout .negate = txt2devnull;
			txt2stderr .negate = txt2devnull;
			txt2dialog .negate = txt2devnull;
			txt2copy   .negate = txt2devnull;
		}

		static public final Set<Flag> NONE = noneOf( Flag.class );
		static private Object SYNCH;
		synchronized static private Object SYNCH(){
			if( SYNCH == null ){ SYNCH = new Object(); }
			return SYNCH;
		}

		/** @since 20070404 */
		public static <T extends Enum<T>> Set<T> valueOf( Class<T> clazz, String data, Pattern delim ){
			Set<T> ret = null;
			try{
				ret = noneOf( clazz );
				for( String token : delim.split( data ) ) ret.add( Enum.valueOf( clazz, token ) );
			}catch( Exception exception ){
				System.err.println( "warning: Flag.valueOfDelimited() caught " + exception );
			}
			return ret;
		}

		/** @since 20070423 */
		public static String supported( String... symbols ){
			synchronized( SYNCH() ){
			try{
				if(      symbols.length < 1 ) return null;
				else if( symbols.length < 2 ) return symbols[0];

				if( BUTTON_FONT   == null ){
					JButton button = null;
					try{
						if( edu.ucla.belief.ui.UI.STATIC_REFERENCE == null ){
							Runnable runnable    = new Runnable(){
								public void run(){
									try{
										Class       clazz    = Class.forName( UIManager.getSystemLookAndFeelClassName() );
										LookAndFeel instance = (LookAndFeel) clazz.newInstance();
										BUTTON_FONT          = instance.getDefaults().getFont( "Button.font" );
									}catch( Throwable thrown ){
										System.err.println( "warning: Flag.supported().runnable.run() caught " + thrown );
										thrown.printStackTrace();
									}
								}
							};
							if( java.awt.GraphicsEnvironment.isHeadless() ){ runnable.run(); }
							else{
								SwingUtilities.invokeLater( runnable );
								for( int i=0; (BUTTON_FONT == null) && i<0x10; i++ ){ Thread.sleep( 0x40 ); }
							}
						}
						else{ edu.ucla.belief.ui.UI.STATIC_REFERENCE.setPkgDspOptLookAndFeel( true ); }
					}catch( Throwable thrown ){
						System.err.println( "warning: Flag.supported() caught " + thrown );
						thrown.printStackTrace();
					}finally{
						if( BUTTON_FONT == null ){ BUTTON_FONT = (button = new JButton()).getFont(); }
					}

					MISSINGGLYPHCODE = BUTTON_FONT.getMissingGlyphCode();

//					if( BUTTON_FONT != null ){ System.out.println( "BUTTON_FONT?                         " + BUTTON_FONT ); }
//					if( button      != null ){ System.out.println( "button.getUI().getClass().getName()? " + button.getUI().getClass().getName() ); }
				  /*try{
						JFrame            frame            = new JFrame();
						frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
						frame.setVisible( true );
						Graphics2D graphics                = (Graphics2D) frame.getGraphics();
						CONTEXT                            = graphics.getFontRenderContext();
						TRANSFORM                          = graphics.getTransform();
						GlyphVector       gv               = BUTTON_FONT.createGlyphVector( CONTEXT, new int[]{ MISSINGGLYPHCODE } );
					  //GlyphVector       gv               = BUTTON_FONT.createGlyphVector( CONTEXT, new String( new int[]{ INT_CODEPOINT_CR },0,1 ) );
						SHAPE_SQUARE                       = gv.getGlyphOutline(0);

//						System.out.println( "\nshape (square) for \\u"+Integer.toString( MISSINGGLYPHCODE, 0x10 )+" {" );
//						append( System.out, SHAPE_SQUARE );
//						System.out.println( "}" );
//						congruent( System.out, 0x25ba );
//
//						System.out.println( "\n\nmissing ? " + MISSINGGLYPHCODE + ", square? " + congruent( SHAPE_SQUARE, gv.getGlyphOutline(0) ) );
//						debug( System.out, 0x2261 );
//						debug( System.out, 0x25b6 );
//						debug( System.out, 0x25ba );
//						debug( System.out, 0x25b7 );
//						debug( System.out, 0x25bb );
//						debug( System.out, 0x25b8 );
//						debug( System.out, 0x25b9 );

						frame.setVisible( false );
						frame.dispose();
					}catch( Throwable thrown ){
						System.err.println( "warning: Flag.supported() caught " + thrown );
						thrown.printStackTrace();
					}*/
				}

				boolean     canDisplay = true;
				GlyphVector gv;
				for( String symbol : symbols ){
					if( CONTEXT == null ){
						if( BUTTON_FONT.canDisplayUpTo( symbol ) < 0 ){ return symbol; }
					}
					else{
						canDisplay = true;
						gv = BUTTON_FONT.createGlyphVector( CONTEXT, symbol );
						for( int i=0; i<gv.getNumGlyphs(); i++ ){
							if(                 true ){ canDisplay &= (  MISSINGGLYPHCODE     !=  gv   .getGlyphCode(i)   ); }
							if( SHAPE_SQUARE != null ){ canDisplay &= (! congruent( SHAPE_SQUARE, gv.getGlyphOutline(i) ) ); }
						}
						if( canDisplay ){ return symbol; }
					}
				}
			}catch( Exception exception ){
				System.err.println( "warning: Grepable.Flag.supported() caught " + exception );
				exception.printStackTrace( System.err );
			}
			}
			return symbols[ symbols.length - 1 ];
		}
		private    static     Font                  BUTTON_FONT;
		private    static     FontRenderContext     CONTEXT;
		private    static     int                   MISSINGGLYPHCODE = -1;
		public     static     final int             INT_CODEPOINT_CR = 0xd;
		private    static     Shape                 SHAPE_SQUARE;
		private    static     AffineTransform       TRANSFORM;

		/** @since 20071226 */
		public static boolean congruent( Shape s1, Shape s2 ){
			try{
				PathIterator pi1 = s1.getPathIterator( TRANSFORM ), pi2 = s2.getPathIterator( TRANSFORM );
				boolean      ret = !(pi1.isDone() || pi2.isDone());
				float[]       a1 = new float[2], a2 = new float[2];
				while( !(pi1.isDone() || pi2.isDone()) ){
					if(       pi1.currentSegment( a1 )  != pi2.currentSegment( a2 )  ){ return false; }
					for( int i=0; i<2; i++ ){ if( a1[i] !=                     a2[i] ){ return false; } }
					          pi1.next();                  pi2.next();
				}
				return ret;
			}catch( Throwable thrown ){
				System.err.println( "warning: Grepable.Flag.congruent() caught " + thrown );
			}
			return false;
		}

		/** @since 20071226 */
		public static Appendable append( Appendable app, Shape shape ) throws Exception{
			float[] array = new float[2];
			String  type  = null;
			for( PathIterator pi = shape.getPathIterator( TRANSFORM ); ! pi.isDone(); pi.next() ){
				switch( pi.currentSegment( array ) ){
					case PathIterator.SEG_MOVETO:  type = "MOVETO  "; break;
					case PathIterator.SEG_LINETO:  type = "LINETO  "; break;
					case PathIterator.SEG_QUADTO:  type = "QUADTO  "; break;
					case PathIterator.SEG_CUBICTO: type = "CUBICTO "; break;
					case PathIterator.SEG_CLOSE:   type = "CLOSE   "; break;
				}
				app.append( type ).append( '[' ).append( Float.toString( array[0] ) ).append( ", " ).append( Float.toString( array[1] ) ).append( "]\n" );
			}
			return app;
		}

		/** @since 20071226 */
		public static Appendable debug( Appendable app, int codepoint ) throws Exception{
			GlyphVector gv        = BUTTON_FONT.createGlyphVector( CONTEXT, new String( new int[]{codepoint},0,1 ) );
			Shape       outline   = gv.getGlyphOutline(0);
			boolean     congruent = congruent( SHAPE_SQUARE, outline );
			app.append( "\\u" ).append( Integer.toString( codepoint, 0x10 ) ).append( " glyph code? " ).append( Integer.toString( gv.getGlyphCode(0) ) ).append( ", square? " ).append( congruent ? "congruent" : "no" ).append( '\n' );
			if( ! congruent ){
				append( app, outline );
			}
			return app;
		}

		/** @since 20071226 */
		public static Appendable congruent( Appendable app, int codepoint ) throws Exception{
			Shape               ref     = BUTTON_FONT.createGlyphVector( CONTEXT, new String( new int[]{codepoint},0,1 ) ).getGlyphOutline(0);
			LinkedList<Integer> list    = new LinkedList<Integer>();
			Shape               outline;
			String              hex;
			for( int i=0; i<0x2fff; i++ ){
				outline = BUTTON_FONT.createGlyphVector( CONTEXT, new String( new int[]{i},0,1 ) ).getGlyphOutline(0);
				if( congruent( outline, ref ) ){ list.add( i ); }
			}
			app.append( '\n' ).append( Integer.toString( list.size() ) ).append( " glyph(s) in " ).append( BUTTON_FONT.getName() ).append( " (java " ).append( System.getProperty("java.version") ).append( ")" ).append( " congruent to \\u" ).append( Integer.toString( codepoint, 0x10 ) ).append( "{\n" );
			append( app, ref );
			app.append( "}:\n" );
			for( int i : list ){
				hex = Integer.toString( i, 0x10 );
				app.append( "\\u" );
				for( int j = 4 - hex.length(); j>0; --j ){ app.append( '0' ); }
				app.append( hex ).append( '\n' );
			  //append( app, outline );
			}
			return app;
		}

		public static void main( String[] args ){
			java.io.PrintStream stream = STREAM_TEST;
			for( Flag flag : values() ){
				try{
					stream.append( flag.symbol ).
					append( "  ".substring( flag.symbol.length() ) ).
					append( " <-> " ).
					append( (flag.negate == null) ? "null" : flag.negate.symbol ).
					append( '\n' );
				}catch( Exception exception ){
					exception.printStackTrace();
				}
			}
			stream.println( "glyphs:" );
			edu.ucla.belief.ui.util.Util.setLookAndFeel();
			java.awt.Font font = (new JButton()).getFont();
			for( int i=0x25a0; i<0x25ff; i++ ){
				stream.print(   "0x" );
				stream.print(   Integer.toHexString( i ) );
				stream.print(   ": " );
				stream.println( font.canDisplay( i ) );
			}
		}
	}

	/** @since 20070423 */
	public static enum Redirect implements Actionable<Redirect>, Semantic{
		txt2devnull( Flag.txt2devnull, "silence", "discard", "", "Delete16.gif", null ){
			public Dest open( String title ){ return DEVNULL; }
		},
		txt2file   ( Flag.txt2file, "file", "save", "to file", "Save16.gif", getKeyStroke( VK_S, CTRL_MASK ) ){
			public Dest open( String title ){
				return this.open( null, title );
			}

			public Dest open( NetworkInternalFrame nif, String title ){
				Map<Context,Object> bucket = Context.bucket();
				bucket.put( Context.nif,     nif );
				bucket.put( Context.title, title );
				bucket.put( Context.file,  chooseFile( bucket ) );
				return this.open( bucket );
			}

			/** @since 20080124 */
			public Dest open( Map<Context,Object> context ){
				try{
					File file =  (File) context.get( Context.file );
					if( file == null ){ context.put( Context.file, file = chooseFile( context ) ); }
					String title = (String) context.get( Context.title );
					return file == null ? DEVNULL : new PrintWriterDest( new PrintWriter( new FileWriter( file ), true ), title );
				}catch( Throwable thrown ){
					System.err.println( "warning: Redirect.txt2file.open() caught " + thrown );
				}
				return DEVNULL;
			}

			public File chooseFile( Map<Context,Object> context ){
				File ret = null;
				try{
					final Component        parent = (Component)  context.get( Context.nif           );
					final String        extension = (String)     context.get( Context.fileextension );
					final String         filename = (String)     context.get( Context.filename      );
					final Actionable   actionable = (Actionable) context.get( Context.actionable    );

					final JFileChooser    chooser = new JFileChooser();
					String                descrip = null;
					if( actionable != null ){
						descrip                   = (String) actionable.get(   display );
						Icon icon                 = (Icon)   actionable.get( largeicon );
						if(  icon == null ){ icon = (Icon)   actionable.get( smallicon ); }
						if(  icon != null ){
							JPanel accessory = new JPanel( new java.awt.GridLayout(2,1) );
							accessory.add( new JLabel( icon ) );
							if( descrip != null ){ accessory.add( new JLabel( "<html><b><font size='6'>" + descrip.replaceAll( "\\s+", "<br />" ) ) ); }
							accessory.setBorder( createCompoundBorder( createCompoundBorder( createEmptyBorder( 4,4,4,4 ), createEtchedBorder() ), createEmptyBorder( 16,8,16,8 ) ) );
							chooser.setAccessory( accessory );
						}
					}

					final String      description = descrip;
					if( directory != null ){ chooser.setCurrentDirectory( directory ); }
					if( extension != null ){
						chooser.setFileFilter( new javax.swing.filechooser.FileFilter(){
							public boolean accept( File candidate ){
								return (candidate != null) &&
								       (candidate.isDirectory() ||
								        candidate.getName().endsWith( suffix ));
							}

							public  String getDescription(){ return descri; }

							private String suffix = extension.startsWith( "." ) ? extension : ("." + extension);
							private String descri = description == null ? ("*" + suffix) : (description + " (*" + suffix + ")");
						} );
					}
					if( filename != null ){
						chooser.setSelectedFile( new File( chooser.getCurrentDirectory(), filename ) );
					}

					chooser.setMultiSelectionEnabled( false );
					chooser.setDialogTitle( "Choose output file for text results" );
					chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
					int result = chooser.showSaveDialog( parent );
					if( result == JFileChooser.APPROVE_OPTION ) ret = chooser.getSelectedFile();
				}catch( Throwable thrown ){
					System.err.println( "warning: Redirect.txt2file.chooseFile() caught " + thrown );
				}finally{
					if( ret != null ) directory = ret.getParentFile();
				}
				return ret;
			}

			private File directory = null;
		},
		txt2copy   ( Flag.txt2copy, "clipboard", "copy", "to system clipboard", "Copy16.gif", getKeyStroke( VK_C, CTRL_MASK ) ){
			public Dest open( String title ){ return new ClipboardDest( title ); }
		},
		txt2console( Flag.txt2console, "console", "write", "to " +STR_SAMIAM_ACRONYM+ "'s console", "Console16.gif", null ){
			public Dest open( NetworkInternalFrame nif, String title ){ return nif == null ? DEVNULL : new PrintWriterDest( nif.console, title ); }
		},
		txt2stdout ( Flag.txt2stdout, "stdout", "write", "to the standard output stream", "Stdout16.gif", null ){
			public Dest open( String title ){ return new PrintStreamDest( System.out, title ); }
		},
		txt2stderr ( Flag.txt2stderr, "stderr", "write", "to the standard error stream", "Stderr16.gif", null ){
			public Dest open( String title ){ return new PrintStreamDest( System.err, title ); }
		},
		txt2dialog ( Flag.txt2dialog, "popup", "popup", "in a dialog window", "About16.gif", null ){
			public Dest open( NetworkInternalFrame nif, String title ){ return new DialogDest( nif, title ); }
		};

		/** @since 20080124 */
		public enum Context{
			nif             ( "NetworkInternalFrame", NetworkInternalFrame.class ),
			title           ( "title"               ,               String.class ),
			actionable      ( "actionable"          ,           Actionable.class ),
			file            ( "file"                ,                 File.class ),
			filename        ( "file name"           ,               String.class ),
			fileextension   ( "file extension"      ,               String.class );

			static public Map<Context,Object> bucket(){ return new EnumMap<Context,Object>( Context.class ); }

			private Context( String displayname, Class clazz ){
				this.displayname = displayname;
				this.clazz       = clazz;
			}

			public       String toString(){ return this.displayname; }

			public final String displayname;
			public final Class  clazz;
		}

		public Dest      open( NetworkInternalFrame nif, String title ){ return this.open(       title ); }
		public Dest      open(                           String title ){ return this.open( null, title ); }
		public Dest      open(            Map<Context,Object> context ){ return this.open( (NetworkInternalFrame) context.get( Context.nif ), (String) context.get( Context.title ) ); }
		public boolean   dead(){ return this == txt2devnull; }

		/** @since 20070424 */
		public interface Dest extends Appendable{
			public Dest   app( CharSequence csq                     );
			public Dest   app( CharSequence csq, int start, int end );
			public Dest   app(         char c                       );
			public Dest   app( String... lines                      );
			public Dest    nl();
			public Dest flush(                                      );
		}

		/** @since 20070424 */
		static public Dest DEVNULL = new DevNullDest( "/dev/null" );

		/** @since 20070424 */
		static public class DevNullDest implements Dest{
			public DevNullDest( String title ){
				this.title = title;
			}

			public Dest   app( CharSequence csq                     ){ return this; }
			public Dest   app( CharSequence csq, int start, int end ){ return this; }
			public Dest   app(         char c                       ){ return this; }
			public Dest flush(                                      ){ return this; }
			public Dest    nl(                                      ){ return this; }

			public Dest   app( String... lines                      ){
				for( String line : lines ) app( line ).nl();
				return this;
			}

			public Appendable append( CharSequence csq                     ){ return app( csq ); }
			public Appendable append( CharSequence csq, int start, int end ){ return app( csq, start, end ); }
			public Appendable append(         char c                       ){ return app( c   ); }

			protected Dest entitle(){
				if( (title != null) && (title.length() > 0) ) app( "search results: " ).app( title ).nl();
				return this;
			}

			public final String title;
		}

		/** @since 20070424 */
		static public class PrintStreamDest extends DevNullDest implements Dest{
			public PrintStreamDest( java.io.PrintStream printstream, String title ){
				super( title );
				this.ps = printstream;
				entitle();
			}

			public Dest   app( CharSequence csq                     ){ ps.append( csq ); return this; }
			public Dest   app( CharSequence csq, int start, int end ){ ps.append( csq, start, end ); return this; }
			public Dest   app(         char c                       ){ ps.append( c   ); return this; }
			public Dest    nl(                                      ){ ps.println();     return this; }
			public Dest flush(                                      ){ ps .flush(     ); return this; }

			public void finalize(){ if( ps != null ){ ps.close(); } }

			private java.io.PrintStream ps;
		}

		/** @since 20070424 */
		static public class PrintWriterDest extends DevNullDest implements Dest{
			public PrintWriterDest( java.io.PrintWriter printwriter, String title ){
				super( title );
				this.pw = printwriter;
				entitle();
			}

			public Dest   app( CharSequence csq                     ){ pw.append( csq ); return this; }
			public Dest   app( CharSequence csq, int start, int end ){ pw.append( csq, start, end ); return this; }
			public Dest   app(         char c                       ){ pw.append( c   ); return this; }
			public Dest    nl(                                      ){ pw.println();     return this; }
			public Dest flush(                                      ){ pw .flush(     ); return this; }

			public void finalize(){ if( pw != null ){ pw.close(); } }

			private java.io.PrintWriter pw;
		}

		/** @since 20070424 */
		static public class ClipboardDest extends DevNullDest implements Dest{
			public ClipboardDest( String title ){
				super( title );
				this.sb = new StringBuilder();
			}

			public Dest   app( CharSequence csq                     ){ sb.append( csq ); return this; }
			public Dest   app( CharSequence csq, int start, int end ){ sb.append( csq, start, end ); return this; }
			public Dest   app(         char c                       ){ sb.append( c   ); return this; }
			public Dest    nl(                                      ){ sb.append( '\n'); return this; }
			public Dest flush(                                      ){
				copyToSystemClipboard( sb.toString() );
				return this;
			}

			protected StringBuilder sb;
		}

		/** @since 20070424 */
		static public class DialogDest extends ClipboardDest implements Dest, Runnable{
			public DialogDest( java.awt.Component parent, String title ){
				super( title );
				this.parent = parent;
			}

			public Dest flush(){
				new Thread( this ).start();
				return this;
			}

			public void run(){
				String        dtitle   = this.title == null ? "text output" : ("search results: " + this.title);
				JTextArea     ta       = new JTextArea( sb.toString() );
				Dimension     pref     = ta.getPreferredSize();
				JScrollPane   pain     = new JScrollPane( ta );
				Dimension     max      = parent == null ? new Dimension( 0x200, 0x200 ) : parent.getSize();
			  //pain.setBorder( BorderFactory.createEtchedBorder() );
				pain.setPreferredSize( new Dimension( Math.min( pref.width, max.width ) + 0x10, Math.min( pref.height, max.height ) + 0x10 ) );
				new JOptionResizeHelper( pain, true, 0x1000 ).start();
				JOptionPane.showMessageDialog( this.parent, pain, dtitle, JOptionPane.PLAIN_MESSAGE );
			  //return this;
			}

			protected java.awt.Component parent;
		}

		/** @since 20080308 */
		public interface Redirectable{
			public Object        redirect( Redirect redirect );
			public String describeContent( Redirect redirect );
		}

		/** @since 20080308 */
		static public Map<Redirect,SamiamAction> actions( final Redirectable redirectable ){
			return actions( redirectable, values() );
		}

		/** @since 20080308 */
		static public Map<Redirect,SamiamAction> actions( final Redirectable redirectable, Collection<Redirect> redirections ){
			return actions( redirectable, redirections.toArray( new Redirect[ redirections.size() ] ) );
		}

		/** @since 20080308 */
		static public Map<Redirect,SamiamAction> actions( final Redirectable redirectable, Redirect ... redirections ){
			Map<Redirect,SamiamAction> map = new EnumMap<Redirect,SamiamAction>( Redirect.class );
			StringBuilder tip = new StringBuilder( 0x40 );
			SamiamAction  action;
			for( Redirect redirect : redirections ){
				final Redirect rdrct = redirect;
				tip.setLength(0);
				map.put( redirect, action = new SamiamAction( redirect.destination, redirect.tip( tip, redirectable.describeContent( redirect ) ).toString(), redirect.flag.name.charAt(0), (Icon) redirect.get( smallicon ) ){
					public void actionPerformed( ActionEvent event ){
						redirectable.redirect( rdrct );
					}
				} );
				if( redirect.stroke != null ){ action.setAccelerator( redirect.stroke ); }
			}
			return map;
		}

		/** @since 20080308 */
		public Appendable tip( Appendable tip, String description ){
			try{
				tip.append( this.verb ).append( ' ' ).append( description );
				if( this.phrase != null ){ tip.append( ' ' ).append( this.phrase ); }
			}catch( Throwable thrown ){
				warn( thrown, "Redirect.tip()" );
			}
			return tip;
		}

		private Redirect( Flag flag, String destination, String verb, String phrase ){
			this( flag, destination, verb, phrase, null, null );
		}

		private Redirect( Flag flag, String destination, String verb, String phrase, String imagefilename, KeyStroke stroke ){
			this.flag          = domanialize( flag, this );
			this.destination   = destination;
			this.verb          = verb;
			this.phrase        = phrase;
			this.imagefilename = imagefilename;
			this.stroke        = stroke;

			this.properties.put(     display, flag.name        );
			this.properties.put(     tooltip, flag.description );
			this.properties.put( accelerator, stroke == null ? firstUniqueKeyStroke( flag.name ) : stroke );

			if( imagefilename != null ){
				ImageIcon icon = getIcon( imagefilename );
				if( icon      != null ){
					this.properties.put( smallicon, icon );
				}
			}
		}

		static private Flag domanialize( Flag flag, Redirect redirect ){
			if( domain == null ) domain = new EnumMap<Flag,Redirect>( Flag.class );
			domain.put( flag, redirect );
			return flag;
		}

		static public Redirect forFlag(      Flag  flag  ){
			return domain.get( flag );
		}

		static public Redirect forFlags( Set<Flag> flags ){
			Redirect forflag = null, ret = null;;
			for( Flag flag : flags ){
				if( (forflag = forFlag( flag )) != null ){
					if( ret == null ) ret = forflag;
					else return ret = null;
				}
			}
			return ret;
		}

		/** @since 20071217 */
		public Semantics semantics(){ return   exclusive; }

		/** @since 20071208 */
		public Redirect getDefault(){ return txt2devnull; }

		/** @since 20071208 */
		public Object   get( Property property ){ return this.properties.get( property ); }

		/** @since 20071208 */
		private Map<Property,Object> properties = new EnumMap<Property,Object>( Property.class );

		/** @since 20071208 */
		public static AbstractButton[]   buttons( View view, Object id, ItemListener listener ){
			return MODELS.newButtons( view, id, listener );
		}

		/** @since 20071208 */
		public static     Redirect      selected( Object id ){
			return MODELS.selected( exclusive, id );
		}

		/** @since 20071208 */
		public static Set<Redirect>     selected( Object id, Set<Redirect> results ){
			return MODELS.selected(  additive, id, results);
		}

		/** @since 20071208 */
		public static EnumModels<Redirect> MODELS = new EnumModels<Redirect>( Redirect.class );

		final  public        Flag               flag;
		static private       Map<Flag,Redirect> domain;
		final  public        String             destination, verb, phrase, imagefilename;
		final  public        KeyStroke          stroke;
	}

	/** @since 20070417 */
	public static enum Language{
		literal( Flag.literal, "literal string",     STR_GREP_DISPLAY_NAME_LOWER, "<html>literal <font color='#cc6600'>search string</font>" ){
			public String toRegex( String lit ){
				return Pattern.quote( lit );
			}
		},
		glob   ( Flag.glob,    "wildcard pattern",   STR_GREP_DISPLAY_NAME_LOWER, "<html><font color='#cc6600'>wildcard</font> pattern a.k.a. 'globbing' pattern" ){
			public String toRegex( String globular ){
				String regex = "\\Q" + globular + "\\E";
				regex = PATTERN_ASTERISK_.matcher( regex ).replaceAll( "\\\\E.*\\\\Q" );
				regex = PATTERN_QUESTION_.matcher( regex ).replaceAll( "\\\\E.\\\\Q"  );
				regex = PATTERN_CHARCLASS.matcher( regex ).replaceAll( "\\\\E$0\\\\Q" );
				regex = regex.replace( "\\Q\\E", "" );
				regex = regex.replace( "\\E\\Q", "" );
				return regex;
			}
		},
		re     ( Flag.re,      "regular expression", "grep", "<html>java <font color='#cc6600'>regular expression</font>" );

		private Language( Flag flag, String name, String action, String tip ){
			this.flag   = domanialize( flag, this );
			this.name   = name;
			this.action = action;
			this.tip    = tip;
		}

		static private Flag domanialize( Flag flag, Language language ){
			if( domain == null ) domain = new EnumMap<Flag,Language>( Flag.class );
			domain.put( flag, language );
			return flag;
		}

		static public Language forFlag(      Flag  flag  ){
			return domain.get( flag );
		}

		static public Language forFlags( Set<Flag> flags ){
			Language forflag = null, ret = null;;
			for( Flag flag : flags ){
				if( (forflag = forFlag( flag )) != null ){
					if( ret == null ) ret = forflag;
					else return ret = null;
				}
			}
			return ret;
		}

		public String      toRegex( String expression ){
			return expression;
		}

		public Language  recommend( String expression ){
			if( Language.this.hints == null ) Language.this.hints = Hint.hints( this );
			return Hint.infer( Language.this, expression, Language.this.hints );
		}

		final  public        Flag               flag;
		final  public        String             name;
		final  public        String             action;
		final  public        String             tip;
		static private       Map<Flag,Language> domain;
		static public  final int                length = values().length;
		private              Hint[]             hints;

		public static Pattern compile( String exp, Set<Flag> flags ){
			int                options = flags.contains( Flag.heedcase ) ? 0 : Pattern.CASE_INSENSITIVE;
			Language              lang = forFlags( flags );
			if( lang == null )    lang = re;
			String               regex = lang.toRegex( exp );
			if( flags.contains( Flag.whole ) ) regex = "\\b" + regex + "\\b";
			return Pattern.compile( regex, options );
		}

		/** Glob expression asterisk '*' */
		public static final Pattern PATTERN_ASTERISK_ = Pattern.compile( "[*]+" );
		/** Glob expression question mark '?' */
		public static final Pattern PATTERN_QUESTION_ = Pattern.compile( "[?]" );
		/** Glob expression character class '[xxxx]' */
		public static final Pattern PATTERN_CHARCLASS = Pattern.compile( "\\[[^\\]]*\\]" );

		/** test/debug */
		public static void main( String[] args ){
			STREAM_TEST.println( glob.toRegex( "*.log" ) );
			STREAM_TEST.println( glob.toRegex( "smoo?.log" ) );
			STREAM_TEST.println( glob.toRegex( "sm[orf]o???.log" ) );
		}
	}

	/** @since 20070417 */
	public static enum Hint{
		litglob2re(             "[.][+?*]",  of( Language.literal, Language.glob ), Language.re      ),
		reglob2lit(               "[*][*]",  of( Language.re,      Language.glob ), Language.literal ),
		re2glob   ("(^[?])|((^|\\w\\w)[*])", of( Language.re                     ), Language.glob    ),
		lit2glob  (        "(?<![.)])[?*]",  of( Language.literal                ), Language.glob    );

		final  public Language understand( Language discard, String exp ){
		  //if( ! discarded.contains( discard ) )    return null;
			return matcher.reset( exp ).find() ? inferred : null;
		}

		static public Language      infer( Language discard, String exp, Hint[] hints ){
			Language understood = null, inferred = null;
			Hint     winner     = null;
			for( Hint hint : hints ){
				if( (understood = hint.understand( discard, exp )) != null ){
					winner = hint;
					if( inferred == null ) inferred = understood;
					else return            inferred = null;
				}
			}
		  //System.out.println( "winner: " + winner.name() );
			return inferred;
		}

		static public Hint[]        hints( Language discard ){
			Hint[] hints = new Hint[ Language.length - 1 ];
			int index = 0;
			for( Hint hint : values() ) if( hint.discarded.contains( discard ) ) hints[ index++ ] = hint;
			return hints;
		}

		private Hint(  String express, Set<Language> discarded, Language inferred ){
			this( Pattern.compile( express ), discarded, inferred );
		}

		private Hint( Pattern pattern, Set<Language> discarded, Language inferred ){
			this( pattern.matcher( "" ), discarded, inferred );
		}

		private Hint( Matcher matcher, Set<Language> discarded, Language inferred ){
			this.matcher   = matcher;
			this.inferred  = inferred;
			this.discarded = Collections.unmodifiableSet( discarded );
		}

		final public     Matcher   matcher;
		final public     Language  inferred;
		final public Set<Language> discarded;
	}

	/** Pairs (or larger groups) of Flags that modify grep's behavior. */
	public static enum Angle{
		sense(           of( Flag.invert,      Flag.ninvert    ), "sense",    "normal or inverted" ),
		language(    copyOf( Language.domain.keySet()          ), "language", "interpretation: which characters have special meaning" ),
		casesensitivity( of( Flag.heedcase,    Flag.ignorecase ), "case",     "upper/lower case awareness" ),
		boundary       ( of( Flag.whole,       Flag.part       ), "boundary", "constrain match boundaries" ),
		redirect(    copyOf( Redirect.domain.keySet()          ), "redirect", "print results as text" ),
		impact(          of( Flag.destructive, Flag.additive   ), "impact",   "potential to disturb" );

		private Angle( Set<Flag> membership, String name, String descrip ){
			this.name        = name;
			this.description = descrip;
			this.membership  = Collections.unmodifiableSet( membership );

			Flag prime = null, implic = null;
			for( Flag flag : membership ){
				if(      flag.primary  ) prime  = flag;
				else if( flag.implicit ) implic = flag;
			}
			this.primary     = prime;
			this.implicit    = implic;
		}

		static public Angle forFlag( Flag flag ){
			for( Angle angle : values() ) if( angle.membership.contains( flag ) ) return angle;
			return null;
		}

		/** @since 20070416 */
		static public Set<Flag> substitute( Set<Flag> past, Flag future ){
			Map<Angle,Flag> mapped = new EnumMap<Angle,Flag>( Angle.class );
			mapped.put( forFlag( future ), future );
			return substitute( past, mapped );
		}

		/** @since 20070416 */
		static public Set<Flag> substitute( Set<Flag> past, Map<Angle,Flag> future ){
			Set<Flag> substituted = copyOf( past );
			for( Angle angle : Angle.values() ){
				if( future.containsKey( angle ) ){
					substituted.removeAll( angle.membership );
					substituted.add( future.get( angle ) );
				}
			}
			return substituted;
		}

		/** @since 20070416 */
		public Set<Flag> project( Set<Flag> flags ){
			Set<Flag> projection;
			(projection = copyOf( flags )).retainAll( this.membership );
			return projection;
		}

		public Flag[] canonicalize(){
			Flag[] array = new Flag[ membership.size() ];
			int index = 0;
			Set<Flag> heap = copyOf( membership );
			if( primary  != null ) { array[ index++ ] = primary;  heap.remove( primary  ); }
			if( implicit != null ) { array[ index++ ] = implicit; heap.remove( implicit ); }
			for( Flag flag : heap )  array[ index++ ] = flag;
			return array;
		}

		public String toString(){
			return this.name;
		}

		public final String    name;
		public final String    description;
		public final Set<Flag> membership;
		public final Flag      primary;
		public final Flag      implicit;
	}

	/** @since 20070423 */
	public enum Preset{
		/** "custom" isn't really a preset at all, it is a placeholder that represents departure from all presets */
		custom       ( "",               null ),
		simpleliteral( "simple",         new Flag[]{ Flag.literal } ),
		basic        ( "basic",          new Flag[]{ Flag.literal }, Flag.invert, Flag.re ),
		glob         ( "wildcard",       new Flag[]{ Flag.glob    }, Flag.invert ),
		power        ( "power",          new Flag[]{ Flag.re      }, Flag.invert, Flag.destructive, Flag.whole ),
		showcase     ( "showcase",       new Flag[]{ Flag.re      }, Flag.values() );

		private Preset( String displayname, Flag[] implicits, Flag... representation ){
			this.displayname = displayname;
			this.vocabulary  = implicits == null ? null : new Vocabulary( Vocabulary.present( implicits, representation ) );
		}

		final public  Vocabulary vocabulary;
		final public  String     displayname;
	};

	/** A portal: a (possibly simplified) view into the universe of Angles/Flags.
		Intended as a control for which flags are displayed to the user as buttons.
		Helps manage the user's preferences.
		@since 20070403 */
	public static class Vocabulary implements FlagPole, Cloneable{
		public Vocabulary( Map<Angle,Presentation> projection ){
			projection = new EnumMap<Angle,Presentation>( projection );
			for( Angle angle : Angle.values() ) if( ! projection.containsKey( angle ) ) projection.put( angle, new Presentation( angle ) );
			this.projection = Collections.unmodifiableMap( projection );
		}

		public Vocabulary( Vocabulary toCopy ){
			Map<Angle,Presentation> proj = new EnumMap<Angle,Presentation>( Angle.class );

			Presentation prez = null;
			for( Angle angle : Angle.values() ){
				prez = toCopy.projection.get( angle );
				if( prez != null ) proj.put( angle, new Presentation( prez ) );
			}
			this.projection = Collections.unmodifiableMap( proj );
		}

		/** @since 20070423 */
		static public Map<Angle,Presentation> present( Flag[] implicits, Flag... representatives ){
			Map<Angle,Presentation> ret            = new EnumMap<Angle,Presentation>( Angle.class );
			Set<Flag>               representation = EnumSet.noneOf( Flag.class );
			Set<Flag>               implicit       = EnumSet.noneOf( Flag.class );
			for( Angle angle : Angle.values() ){
				implicit      .clear();
				representation.clear();

				for( Flag flag : implicits       ) if( angle.membership.contains( flag ) ) implicit      .add( flag );
				if(      implicit.size() < 1 ) implicit.add( angle.implicit );
				else if( implicit.size() > 1 ) throw new IllegalArgumentException( "illegal definition, "+implicit+" is too many implicit values for angle " + angle );

				for( Flag flag : representatives ) if( angle.membership.contains( flag ) ) representation.add( flag );

				ret.put( angle, new Presentation( angle, representation, implicit.iterator().next(), angle.ordinal() ) );
			}
			return ret;
		}

		public boolean isRepresentative( Flag flag ){
			Angle angle = Angle.forFlag( flag );
			if( angle == null ) return false;
			return projection.get( angle ).representation.contains( flag );
		}

		public boolean isImplicit( Flag flag ){
			Angle angle = Angle.forFlag( flag );
			if( angle == null ) return false;
			return projection.get( angle ).implicit == flag;
		}

		public Set<Flag> representation( Angle angle ){
			return projection.get( angle ).representation;
		}

		public Flag implicit( Angle angle ){
			return projection.get( angle ).implicit;
		}

		public Presentation[] canonicalize(){
			Presentation[] ret = projection.values().toArray( new Presentation[ projection.size() ] );
			Arrays.sort( ret );
			return ret;
		}

		/** @since 20070404 */
		public Set<Flag> flags( Set<Stimulus> stimuli ){
			EnumSet<Flag> ret = noneOf( Flag.class );
			for( Presentation prez : projection.values() ) ret.addAll( prez.flags( stimuli ) );
			return ret;
		}

		/** @since 20070404 */
		public Map<Stimulus,Set<Flag>> stimulated( Map<Stimulus,Set<Flag>> stimulated ){
			for( Presentation prez : projection.values() ) prez.stimulated( stimulated );
			return stimulated;
		}

		/** @since 20070419 */
		public Vocabulary setHighlight( boolean highlight ){
			for( Presentation prez : projection.values() ) prez.setHighlight( highlight );
			return this;
		}

		/** @since 20070404 */
		public JComponent asComponent( Set<Flag> explicit ){
			if( myComponent == null ){
				JPanel             pnl = new JPanel( new GridBagLayout() );
				GridBagConstraints c   = new GridBagConstraints();

				JComponent comp = null;
				for( Presentation prez : canonicalize() ){
					comp = prez.asComponent( explicit );
					if( comp != null ){
						pnl.add( comp,                         c );
						pnl.add( Box.createHorizontalStrut(2), c );
					}
				}
				myComponent = pnl;
			}
			return myComponent;
		}

		/** @since 20070404 */
		public Vocabulary addMouseListener( java.awt.event.MouseListener mouselistener ){
			try{
				if( myComponent == null ) return this;
				myComponent.addMouseListener( mouselistener );
				for( java.awt.Component comp : myComponent.getComponents() ) comp.addMouseListener( mouselistener );
			}catch( Exception exception ){
				System.err.println( "warning: Grepable.Vocabulary.addMouseListener() caught " + exception );
			}
			return this;
		}

		/** @since 20070418 */
		public boolean raise( Set<Flag> flags ){
			boolean success = true;
			for( Presentation prez : projection.values() ){
				success &= prez.raise( flags );
			}
			return success;
		}

		/** @since 20070404 */
		public Map<Stimulus,Set<Flag>> recall( Map<Stimulus,Set<Flag>> stimulated ){
			for( Presentation prez : projection.values() ){
				prez.recall( stimulated );
			}
			return stimulated;
		}

		/** @since 20070418 */
		public Vocabulary derive( Set<Flag> required ){
			Map<Angle,Presentation> derivative = new EnumMap<Angle,Presentation>( Angle.class );
			for( Presentation prez : projection.values() ){
				if( ! prez.recall( required ) ) derivative.put( prez.angle, prez.derive( required ) );
			}
			return derivative.isEmpty() ? this : deriveImpl( derivative );
		}

		/** @since 20070418 */
		private Vocabulary deriveImpl( Map<Angle,Presentation> derivative ){
			for( Presentation prez : projection.values() ){
				if( derivative.get( prez.angle ) == null ) derivative.put( prez.angle, prez );
			}
			die();
			return new Vocabulary( derivative );
		}

		/** @since 20070418 */
		public void die(){
			if( myComponent != null ) myComponent.removeAll();
			myComponent = null;

		  //projection.clear();
		  //projection = null;
		}

		/** @since 20070404 */
		public Object clone(){
			return new Vocabulary( this );
		}

		/** @since 20070404 */
		public Appendable appendXML( Appendable buff ){
			try{
				String indent = "\t\t\t";
				for( Presentation prez : canonicalize() ){
					buff.append( indent );
					prez.appendXML( buff ).append( '\n' );
				}
			}catch( Exception exception ){
				System.err.println( "warning: Grepable.Vocabulary.appendXML() caught " + exception );
			}
			return buff;
		}

		public final Map<Angle,Presentation> projection;
		private      JComponent              myComponent;
	}

	/** The preference settings for a single Angle.
		@since 20070403 */
	public static class Presentation implements FlagPole, Cloneable, Comparable<Presentation>{
		public Presentation( Angle angle, Set<Flag> representation, Flag implicit, int ordinal ){
			this.angle          = angle;
			this.representation = Collections.unmodifiableSet( copyOf( validate( representation ) ) );
			this.implicit       = validate( implicit, true );
			this.ordinal        = ordinal;

			if( representation != null && representation.size() == 1 && representation.contains( implicit ) ) throw new IllegalStateException( "attempt to construct illegal Presentation with sole representative '"+representation.iterator().next().name()+"' == implicit '"+implicit.name()+"'" );
		}

		/** set the implicit flag to the system default */
		public Presentation( Angle angle, Flag representative ){
			this( angle, of( representative ), angle.implicit, angle.ordinal() );
		}

		/** no representation */
		public Presentation( Angle angle ){
			this( angle, noneOf( Flag.class ), angle.implicit, angle.ordinal() );
		}

		/** copy constructor */
		public Presentation( Presentation toCopy ){
			this.angle          = toCopy.angle;
			this.representation = toCopy.representation;
			this.implicit       = toCopy.implicit;
			this.ordinal        = toCopy.ordinal;
		}

		/** @since 20070419 */
		public Flag implicit(){
			return this.implicit;
		}

		/** @since 20070418 */
		public boolean raise( Set<Flag> flags ){
			if( myFlagPole == null ) return flags.contains( this.implicit );
			else return myFlagPole.raise( flags );
		}

		/** @since 20070418 */
		public boolean recall( Map<Stimulus,Set<Flag>> stimulated ){
			Set<Flag> explicit = stimulated.get( Stimulus.explicit );
			return explicit == null || explicit.isEmpty() ? true : recall( explicit );
		}

		/** @since 20070418 */
		private boolean recall( Set<Flag> explicit ){
			Set<Flag> projected = this.angle.membership.containsAll( explicit ) ? explicit : this.angle.project( explicit );

			if( projected == null || projected.isEmpty() ) return true;
			if( projected.size() > 1 ) throw new IllegalArgumentException( "Presentation cannot recall more than one option: " + projected );

			if( this.representation == null || this.representation.isEmpty() ) return projected.contains( this.implicit );

			if( myFlagPole != null ) return myFlagPole.raise( projected );
			else return false;
		}

		/** @since 20070418 */
		private Presentation derive( Set<Flag> required ){
			Set<Flag> projected = this.angle.project( required );
			if( this.representation == null || this.representation.isEmpty() ){
				this.implicit = projected.iterator().next();
				return this;
			}

			projected.addAll( this.representation );

			Presentation derived = new Presentation( this.angle, projected, this.implicit, this.ordinal );
			derived.asComponent( projected );
			if( ! derived.recall( projected ) ) throw new IllegalStateException( "something's askew" );

			die();
			return derived;
		}

		/** @since 20070418 */
		public void die(){
		  //this.angle          = null;
		  //this.representation = null;
		  //this.implicit       = null;
		  //this.ordinal        =   -1;
			this.myComponent    = null;
			this.myFlagPole     = null;
			this.myToggle       = null;
		}

		public Set<Flag> validate( Set<Flag> flags ){
			for( Flag flag : flags ) validate( flag, true );
			return flags;
		}

		public Flag validate( Flag flag, boolean forbidNull ){
			String errmsg = null;
			if(        this.angle == null                     ) errmsg = "null Angle";
			else if(         flag == null    ) if( forbidNull ) errmsg = "null Flag";
			else if( ! this.angle.membership.contains( flag ) ) errmsg = flag.name() + " is not a member of " + this.angle.name;

			if( errmsg != null ) throw new RuntimeException( errmsg );
			else return flag;
		}

		/** interface Comparable */
		public int compareTo( Presentation other ){
			return ( this.ordinal > other.ordinal ) ? 1 : -1;
		}

		/** @since 20070404 */
		public Set<Flag> flags( Set<Stimulus> stimuli ){
			if( myFlagPole == null ){
				return stimuli.contains( Stimulus.implicit ) ? of( implicit ) : Flag.NONE;
			}
			else return myFlagPole.flags( stimuli );
		}

		/** @since 20070404 */
		public Map<Stimulus,Set<Flag>> stimulated( Map<Stimulus,Set<Flag>> stimulated ){
			if( myFlagPole == null ) return Stimulus.put( Stimulus.implicit, implicit, stimulated );
			else return myFlagPole.stimulated( stimulated );
		}

		/** @since 20070419 */
		public Presentation setHighlight( boolean highlight ){
			if( myToggle != null ) myToggle.setHighlight( highlight );
			return this;
		}

		/** @since 20070404 */
		public JComponent asComponent( Set<Flag> explicit ){
			if( myComponent == null ){
				if( representation == null || representation.isEmpty() ) return myComponent = null;

				Stimulus stimulus = Stimulus.implicit;
				Flag     selected = null;
				for( Flag flag : explicit ){
					if( representation.contains( flag ) ){
						selected = flag;
						stimulus = Stimulus.explicit;
					}
				}

				if( representation.size() == 1 ){
					Toggle toggle = new Toggle( representation.iterator().next(), implicit, selected, stimulus );
					myComponent = myToggle = toggle;
					myFlagPole  = toggle;
				}
				else{
					if( selected == null && representation.contains( implicit ) ) selected = implicit;
					if( selected == null                                        ) selected = representation.iterator().next();
					Cyclotron tron = new Cyclotron( representation, selected, stimulus );
					myComponent = tron;
					myFlagPole  = tron;
				}
			}
			return myComponent;
		}

		/** @since 20070404 */
		public FlagPole listen( ActionListener listener ){
			if( myComponent != null ){
				boolean contains = false;
				for( ActionListener al : myComponent.getActionListeners() ) if( al == listener ) contains = true;
				if( ! contains ) myComponent.addActionListener( listener );
				return myFlagPole;
			}
			else return Presentation.this;
		}

		/** @since 20070404 */
		public Object clone(){
			return new Presentation( this );
		}

		/** @since 20070404 */
		public Appendable appendXML( Appendable buff ){
			try{
				String spaces = "                      ";
				int    mindex = spaces.length() - 1;
				buff
				.append( '<' ).append( STR_ELEMENT_PRESENTATION ).append( " " ).append( STR_ATTR_ANGLE ).append( "=\"" ).append( angle.name() )
				.append( "\"" ).append( spaces.substring( Math.min( mindex, angle.name().length() ) ) )
				.append( STR_ATTR_REPRESENTATION ).append( "=\"" );
				int count = 0;
				if( representation != null && (! representation.isEmpty() ) ){
					for( Flag flag : representation ){
						buff.append( flag.name() ).append( ',' );
						count += flag.name().length() + 1;
					}
				}
				buff
				.append( "\"" ).append( spaces.substring( Math.min( mindex, count ) ) )
				.append( STR_ATTR_IMPLICIT ).append( "=\"" ).append( implicit.name() )
				.append( "\"" ).append( spaces.substring( Math.min( mindex, implicit.name().length() ) ) )
				.append( STR_ATTR_ORDINAL ).append( "=\"" ).append( Integer.toString( ordinal ) )
				.append( "\" />" );
			}catch( Exception exception ){
				System.err.println( "warning: Grepable.Presentation.appendXML() caught " + exception );
			}
			return buff;
		}

		public static final String  STR_ELEMENT_PRESENTATION = "presentation",
		                            STR_ATTR_ANGLE           = "angle",
		                            STR_ATTR_REPRESENTATION  = "representation",
		                            STR_ATTR_IMPLICIT        = "implicit",
		                            STR_ATTR_ORDINAL         = "ordinal";

		public static final Pattern PATTERN_DELIMITERS       = Pattern.compile( "[\\]\\[,\\s]" );

		/** @since 20070404 */
		public static AbstractButton configure( AbstractButton btn ){
			try{
				if( TENPT == null ) TENPT = btn.getFont().deriveFont( (float)10 );
				btn.setFont( TENPT );

				edu.ucla.belief.ui.UI           ui      = edu.ucla.belief.ui.UI.STATIC_REFERENCE;
				java.awt.font.FontRenderContext context = ((java.awt.Graphics2D)ui.getGraphics()).getFontRenderContext();
				java.awt.geom.Rectangle2D       bounds  = btn.getFont().getStringBounds( btn.getText(), context );
				//System.out.println( btn.getText() + ": " + bounds );
				int top  = Math.max( 0, (int)((((double)14) - bounds.getHeight()) * 0.5) );
				int left = Math.max( 0, (int)((((double)12) - bounds .getWidth()) * 0.5) );
				java.awt.Insets insets = new java.awt.Insets( top, left, top, left );
				btn.setMargin( insets );
			}catch( Throwable throwable ){
				System.err.println( "warning: Grepable.configure() caught " + throwable );
				//throwable.printStackTrace();
			}
			return btn;
		}
		static private java.awt.Font TENPT;

		public final Angle angle;
		/** The Flags chosen to represent the Angle. */
		public final Set<Flag> representation;
		/** The value of this Angle when no flag is specified. */
		private      Flag      implicit;
		/** The index of the order we want this Angle to appear in sequence. */
		public final int   ordinal;

		private AbstractButton myComponent;
		private FlagPole       myFlagPole;
		private Toggle         myToggle;
	}

	/** The "cause" or "origin" or "incitement"
		or "motive" or "antecedent" or "derivation"
		or "inducement" or "inspiration" or "motive" of something.
		Originally, to differentiate between explicit/implicit Flag settings.
		@since 20070404 */
	public static enum Stimulus{
		/** stimulated by settings, or defaults, or preferences, etc. */
		implicit,
		/** stimulated by human action: button clicking */
		explicit;

		static public final Set<Stimulus> NONE     = noneOf( Stimulus.class    );
		static public final Set<Stimulus> ANY      =  allOf( Stimulus.class    );
		static public final Set<Stimulus> EXPLICIT =     of( Stimulus.explicit );
		static public final Set<Stimulus> IMPLICIT =     of( Stimulus.implicit );

		static public Map<Stimulus,Set<Flag>> put( Stimulus stimulus, Flag flag, Map<Stimulus,Set<Flag>> stimulated ){
			Set<Flag> flags = stimulated.get( stimulus );
			if( flags == null ) stimulated.put( stimulus, flags = of( flag ) );
			else                                          flags .add( flag );
			return stimulated;
		}
	}

	/** Something that bears Flags.
		@since 20070404 */
	public interface FlagPole{
		/** select Flags for the given Stimuli */
		public Set<Flag>                    flags( Set<Stimulus>           stimuli    );
		/** map all */
		public Map<Stimulus,Set<Flag>> stimulated( Map<Stimulus,Set<Flag>> stimulated );
		/** up the flagpole */
		public boolean                      raise( Set<Flag>               flags      );
	}

	/** A JButton such that each click cycles through a sequence of Flags.
		@since 20070404 */
	public class Cyclotron extends JButton implements FlagPole
	{
		public Cyclotron( Set<Flag> flags ){
			this( flags, flags.iterator().next(), Stimulus.implicit );
		}

		public Cyclotron( Set<Flag> flags, Flag selected, Stimulus stimulus ){
			super();
			if( flags == null || flags.isEmpty() ) throw new IllegalArgumentException();
			myFlags    = flags.toArray( new Flag[ flags.size() ] );
			for( myIndex=0; myIndex<myFlags.length; myIndex++ ) if( myFlags[myIndex] == selected ) break;
			myStimulus = stimulus;
			homogenize();
		}

		protected void fireActionPerformed( java.awt.event.ActionEvent event ){
			rotate();
			super.fireActionPerformed( event );
		}

		public Flag rotate(){
			myStimulus = Stimulus.explicit;
			if( ++myIndex >= myFlags.length ) myIndex = 0;
			return homogenize();
		}

		private Flag homogenize(){
			Cyclotron.this       .setText( myFlags[ myIndex ].symbol      );
			Cyclotron.this.setToolTipText( myFlags[ myIndex ].description );
			Presentation.configure( Cyclotron.this );
			return myFlags[ myIndex ];
		}

		public Set<Flag> flags( Set<Stimulus> stimuli ){
			if( stimuli.contains( myStimulus ) ) return of( myFlags[ myIndex ] );
			else return Flag.NONE;
		}

		public Map<Stimulus,Set<Flag>> stimulated( Map<Stimulus,Set<Flag>> stimulated ){
			return Stimulus.put( myStimulus, myFlags[ myIndex ], stimulated );
		}

		/** @since 20070418 */
		public boolean raise( Set<Flag> flags ){
			for( int i = 0; i<myFlags.length; i++ ){
				if( flags.contains( myFlags[i] ) ){
					myIndex = i;
					homogenize();
					return true;
				}
			}
			return false;
		}

		private final Flag[]   myFlags;
		private       int      myIndex;
		private       Stimulus myStimulus;
	}

	/** A JToggleButton for a binary Flag setup.
		@since 20070404 */
	public class Toggle extends JToggleButton implements FlagPole
	{
		public Toggle( Flag explicit, Flag implicit, Flag selected, Stimulus stimulus ){
			super(                      explicit.symbol      );
			Toggle.this.setToolTipText( explicit.description );
			Presentation.configure( Toggle.this );

			myExplicit = explicit;
			myImplicit = implicit;

			if( selected == null ){
				 selected   = implicit;
				 myStimulus = Stimulus.implicit;
			}
			else myStimulus = stimulus;

			Toggle.this.myColorUp = getBackground();

			Toggle.this.setSelected( selected == myExplicit );
			Toggle.this.setBackground( highlight() );
		}

		/** @since 20070419 */
		public Toggle setHighlight( boolean highlight ){
			if( this.myFlagHighlight = highlight ){
				if( myBasicToggleButtonUI == null ) myBasicToggleButtonUI = new javax.swing.plaf.basic.BasicToggleButtonUI();
				setUI( myBasicToggleButtonUI );
			}
			else updateUI();

		  //this .setContentAreaFilled( ! myFlagHighlight );
		  //this            .setOpaque(   true            );

			this.setBackground( highlight() );
			return this;
		}

		/** @since 20070419 */
		public Color highlight(){
			return myFlagHighlight && this.isSelected() ? COLORDOWN : myColorUp;
		}

		protected void fireActionPerformed( java.awt.event.ActionEvent event ){
			myStimulus = Stimulus.explicit;
			super.fireActionPerformed( event );
			if( myFlagHighlight ) setBackground( highlight() );
		}

		public Flag flag(){
			return Toggle.this.isSelected() ? myExplicit : myImplicit;
		}

		public Set<Flag> flags( Set<Stimulus> stimuli ){
			if( stimuli.contains( myStimulus ) ) return of( flag() );
			else return Flag.NONE;
		}

		public Map<Stimulus,Set<Flag>> stimulated( Map<Stimulus,Set<Flag>> stimulated ){
			return Stimulus.put( myStimulus, flag(), stimulated );
		}

		/** @since 20070418 */
		public boolean raise( Set<Flag> flags ){
			if( flags.contains( myExplicit ) ){
				setSelected( true );
				myStimulus = Stimulus.explicit;
				return true;
			}
			if( flags.contains( myImplicit ) ){
				setSelected( false );
				myStimulus = Stimulus.explicit;
				return true;
			}
			else return false;
		}

		private final Flag     myExplicit, myImplicit;
		private       Stimulus myStimulus;
		private       boolean  myFlagHighlight;
		private       Color    myColorUp;
		private javax.swing.plaf.basic.BasicToggleButtonUI myBasicToggleButtonUI;

		public static final Color COLORDOWN = new Color( 247, 191, 47 );
	}

	/** If your implementation need not differentiate fields,
		use this undifferentiated field selector.
		see {@link Grepable#grepFields Grepable.grepFields()}. */
	public static enum Simple{
		/** Monolithic grep, no choice. */
		irreducible;

		/** Suitable return value for {@link Grepable#grepFieldsDefault Grepable.grepFieldsDefault()}. */
		public static EnumSet<Simple> SINGLETON = of( irreducible );
	}

	/** Persistent grep options to and from the gui. */
	public static class GrepOptions<G extends Enum<G>>
	{
		public GrepOptions( Set<Flag> flags, EnumSet<G> selected, String exp ){
			GrepOptions.this.flags    = flags;
			GrepOptions.this.selected = selected;
			GrepOptions.this.exp      = exp;
		}

		/** @since 20070404 */
		public GrepOptions( Vocabulary vocab, EnumSet<G> selected, String exp ){
			memorize( vocab );
			GrepOptions.this.selected = selected;
			GrepOptions.this.exp      = exp;
		}

		/** @since 20070426 */
		public EnumSet<G> getSelected( Dropdown<G> list ){
			EnumSet<G> ret = null;
			if( list == null ) return ret;
			try{
				ret = copyOf( GrepOptions.this.selected );
				ret.clear();
				ret = list.getSelection( ret );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.getSelected( Dropdown<G> ) caught " + exception );
			}
			return ret;
		}

		@SuppressWarnings( "unchecked" )
		public EnumSet<G> getSelected( JComboBox list ){
			EnumSet<G> ret = null;
			if( list == null ) return ret;
			try{
				ret = of( (G) list.getSelectedItem() );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.getSelected( JComboBox ) caught " + exception );
			}
			return ret;
		}

		@SuppressWarnings( "unchecked" )
		public EnumSet<G> getSelected( JList list ){
			EnumSet<G> ret = null;
			if( list == null ) return ret;
			try{
				for( Object field : list.getSelectedValues() ){
					if( ret == null ) ret = of( (G) field );
					else              ret.add(  (G) field );
				}
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.getSelected( JList ) caught " + exception );
			}
			return ret;
		}

		/** @since 20070426 */
		public GrepOptions<G> memorize( Dropdown<G> list, Vocabulary vocab, JTextField tf ){
			try{
				memorize( list );
				memorize( vocab );
				memorize( tf );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.memorize() caught " + exception );
			}
			return this;
		}

		public GrepOptions<G> memorize( JList list, Vocabulary vocab, JTextField tf ){
			try{
				memorize( list );
				memorize( vocab );
				memorize( tf );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.memorize() caught " + exception );
			}
			return this;
		}

		public GrepOptions<G> memorize( Vocabulary vocab ){
			try{
				GrepOptions.this.flags      = vocab     .flags( Stimulus.ANY                );
				GrepOptions.this.stimulated = vocab.stimulated( GrepOptions.this.stimulated );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.memorize() caught " + exception );
			}
			return this;
		}

		public GrepOptions<G> memorize( JComboBox list, AbstractButton cbInvert, AbstractButton cbGlob, JTextField tf ){
			memorize( list );
			return memorize( cbInvert, cbGlob, tf );
		}

		public GrepOptions<G> memorize( JComboBox list ){
			try{
				selected = getSelected( list );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.memorize() caught " + exception );
			}
			return this;
		}

		public GrepOptions<G> memorize( JList list, AbstractButton cbInvert, AbstractButton cbGlob, JTextField tf ){
			memorize( list );
			return memorize( cbInvert, cbGlob, tf );
		}

		public GrepOptions<G> memorize( JList list ){
			try{
				selected = getSelected( list );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.memorize() caught " + exception );
			}
			return this;
		}

		/** @since 20070426 */
		public GrepOptions<G> memorize( Dropdown<G> list ){
			try{
				selected = getSelected( list );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.memorize() caught " + exception );
			}
			return this;
		}

		public GrepOptions<G> memorize( AbstractButton cbInvert, AbstractButton cbGlob, JTextField tf ){
			try{
				flags    = cbInvert.isSelected() ? of( Flag.invert ) : noneOf( Flag.class );
				if( cbGlob.isSelected() ) flags.add( Flag.glob );
				memorize( tf );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.memorize() caught " + exception );
			}
			return this;
		}

		public GrepOptions<G> memorize( JTextField tf ) throws Exception{
			exp = tf.getText();
			return this;
		}

		/** @since 20070426 */
		public GrepOptions<G> recall( Dropdown<G> list, Vocabulary vocab, JTextField tf ){
			try{
				recall( list );
				recall( vocab );
				recall( tf );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.recall() caught " + exception );
				exception.printStackTrace();
			}
			return this;
		}

		public GrepOptions<G> recall( JList list, Vocabulary vocab, JTextField tf ){
			try{
				recall( list );
				recall( vocab );
				recall( tf );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.recall() caught " + exception );
				exception.printStackTrace();
			}
			return this;
		}

		public GrepOptions<G> recall( Vocabulary vocab ) throws Exception{
			if( (this.stimulated != null) && (! this.stimulated.isEmpty()) )
				vocab.recall( GrepOptions.this.stimulated );
			return this;
		}

		public GrepOptions<G> recall( JComboBox list, AbstractButton cbInvert, AbstractButton cbGlob, JTextField tf ){
			recall( list );
			return recall( cbInvert, cbGlob, tf );
		}

		public GrepOptions<G> recall( JComboBox list ){
			if( list != null ){
				try{
					if( (selected != null) && (!selected.isEmpty()) ) list.setSelectedItem( selected.iterator().next() );
				}catch( Exception exception ){
					System.err.println( "warning: GrepOptions.recall() caught " + exception );
				}
			}
			return this;
		}

		public GrepOptions<G> recall( JList list, AbstractButton cbInvert, AbstractButton cbGlob, JTextField tf ){
			recall( list );
			return recall( cbInvert, cbGlob, tf );
		}

		/** @since 20070426 */
		public GrepOptions<G> recall( Dropdown<G> list ){
			if( list == null || selected == null || selected.isEmpty() ) return this;
			try{
				list.setSelection( selected );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.recall() caught " + exception );
			}
			return this;
		}

		public GrepOptions<G> recall( JList list ){
			if( list != null ){
				try{
					if( (selected != null) && (!selected.isEmpty()) )
						for( G field : selected ){
							list.addSelectionInterval( field.ordinal(), field.ordinal() );
							list.ensureIndexIsVisible( field.ordinal() );
						}
				}catch( Exception exception ){
					System.err.println( "warning: GrepOptions.recall() caught " + exception );
				}
			}
			return this;
		}

		public GrepOptions<G> recall( AbstractButton cbInvert, AbstractButton cbGlob, JTextField tf ){
			try{
				cbInvert.setSelected( flags.contains( Flag.invert ) );
				cbGlob.setSelected(   flags.contains( Flag.glob   ) );
				recall( tf );
			}catch( Exception exception ){
				System.err.println( "warning: GrepOptions.recall() caught " + exception );
			}
			return this;
		}

		public GrepOptions<G> recall( JTextField tf ) throws Exception{
			tf.setText( exp );
			return this;
		}

		/** @since 20070404 */
		public Set<Flag> explicit(){
			Set<Flag> explicit = stimulated.get( Stimulus.explicit );
			if( explicit == null ) explicit = Flag.NONE;
			return explicit;
		}

		public EnumSet<G>              selected;
		public Set<Flag>               flags;
		public Map<Stimulus,Set<Flag>> stimulated = new EnumMap<Stimulus,Set<Flag>>( Stimulus.class );
		public String                  exp;
	}
}
