package edu.ucla.belief.ui.statusbar;

import edu.ucla.belief.ui.util.Encoder;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/** @author Keith Cascio
	@since 20031204 */
public class StackedMessage
  implements Runnable
{
	//public static      boolean FLAG_ANIMATE             = true;
	public  static       boolean FLAG_ANIMATE_TEXT        = false;
	private static       int     INT_COUNTER_THREAD_NAMES = 0;
	private static final int     INT_STEPS_ANIMATION      = 32;
	private static final float   FLOAT_STEPS_ANIMATION    = (float)INT_STEPS_ANIMATION;
	public  static final float   FLOAT_ZERO               = (float)0;
	public  static final float   FLOAT_ONE                = (float)1;
	private static       Color   COLOR_TARGET             = new Color( 255, 0, 128 );//Color.yellow;
	private static       float[] COMPONENTS_TARGET        = COLOR_TARGET.getRGBColorComponents( new float[3] );
	private static       Encoder ENCODER                  = null;

	public StackedMessage( JLabel label, int handle )
	{
		this.myHandle    = handle;
		this.label       = label;
		this.stack       = new LinkedList();
		this.deathrow    = new HashSet();
		this.myColorOrig = label.getForeground();
		this.myGradient  = StackedMessage.calcGradient( this.myColorOrig );
	}

	/** @since 20041211 */
	public boolean isEmpty(){
		return stack.isEmpty();
	}

	/*synchronized*/ public void setText( String text )
	{
	  //if( debug ) System.out.println( "StackedMessage.setText( "+text+" )" );
		interruptTextAnimation();

		boolean flagNoChange = false;
		synchronized( this )
		{
		flagNoChange = (strDefault != null) && strDefault.equals( text );
		strDefault = text;
		if( ! this.myFlagAnimateText ){ later( text ); }
		deathrow.clear();
		stack.clear();
		}
		if( ! flagNoChange ){ animate( text ); }
	}

	/*synchronized*/ public void pushText( String newText )
	{
	  //if( debug ) System.out.println( "StackedMessage.pushText( "+newText+" )" );
		interruptTextAnimation();

		synchronized( this )
		{
		stack.addFirst( newText );
		if( ! this.myFlagAnimateText ){ later( newText ); }
		}
		animate( newText );
	}

	/*synchronized*/ public void popText( String newText )
	{
	  //if( debug ) System.out.println( "StackedMessage.popText( "+newText+" )" );
		interruptTextAnimation();

		synchronized( this )
		{
		deathrow.add( newText );
		String result = stack.isEmpty() ? strDefault : (String) stack.getFirst();
		while( (!(stack.isEmpty())) && deathrow.contains( stack.getFirst() ) )
		{
			deathrow.remove( stack.removeFirst() );
			result = stack.isEmpty() ? strDefault : (String) stack.getFirst();
		}
		later( result );
		}
	}

	/** @since 20060329 */
	public void setAnimated( boolean flag ){
		this.myFlagAnimate = flag;
		this.myFlagAnimateText = this.myFlagAnimate && FLAG_ANIMATE_TEXT;
	}

	/** @since 20060329 */
	public boolean isAnimated(){
		return this.myFlagAnimate;
	}

	/** @since 20060328 */
	private void interruptTextAnimation(){
		if( !this.myFlagAnimateText ) return;

		myGroupAnimation.interrupt();
		while( myGroupAnimation.activeCount() > 0 ){ Thread.yield(); }
	}

	/** @since 20060328 */
	public Thread animate( String text ){
		Thread thread = Thread.currentThread();

		if( ! myFlagAnimate ){ return thread; }

		myGroupAnimation.interrupt();

		if( (text != null) && (text.length() > 0) ){
			String name = "StackedMessage animate " + text + " " + Integer.toString( INT_COUNTER_THREAD_NAMES++ );
			thread = new Thread( myGroupAnimation, new RunAnimate( text ), name );
			thread.setDaemon( true );
			thread.start();
		}
		else{
			later( text, true, true );
		}

		return thread;
	}

	/** @since 20060328 */
	//public Runnable myRunAnimate = new Runnable(){

	/** @since 20060328 */
	public class RunAnimate implements Runnable{
		public RunAnimate( String text ){
			this.text = text;
		}

		public void run(){
			StackedMessage.this.animateImpl( text );
		}

		private String text;
	}

	/** @since 20060328 */
	public void animateImpl( String text ){
		try{
			int len = -1;
			if( this.myFlagAnimateText ){
				if( ENCODER == null ) ENCODER = new Encoder();
				String unencoded = ENCODER.htmlUnencode( text );
				if( myBuff == null ) myBuff = new StringBuffer( unencoded );
				else{
					myBuff.setLength(0);
					myBuff.append( unencoded );
				}
				len = unencoded.length();
			}

			for( int i=0; i<myGradient.length; i++ ){
				if( this.myFlagAnimateText ) label.setText( myBuff.substring( 0, Math.min( i+3, len ) ) );
				label.setForeground( myGradient[i] );
				later( null, true, false );
				Thread.sleep( 0x10 );
			}

			if( this.myFlagAnimateText ) label.setText( text );
			later( null, true, false );
			Thread.sleep( 0x400 );

			for( int i=myGradient.length-1; i>=0; i-- ){
				label.setForeground( myGradient[i] );
				later( null, true, false );
				Thread.sleep( 0x80 );
			}
		}
		catch( InterruptedException interruptedexception )
		{
		  //System.out.println( Thread.currentThread().getName() + " interrupted !!" );
			label.setForeground( myColorOrig );
			Thread.currentThread().interrupt();
			return;
		}
		catch( Throwable throwable )
		{
			System.err.println( "warning: StackedMessage.animateImpl() caught " + throwable );
			StackTraceElement[] trace = throwable.getStackTrace();
			System.err.println( "trace[0] " + trace[0] );
			int last = trace.length - 1;
			System.err.println( "trace[last="+last+"] " + trace[last] );
			return;
		}
	}

	/** @since 20060328 */
	private static float[] calcDeltas( float[] target, float[] orig ){
		float[] deltas = new float[3];
		for( int i=0; i<3; i++ ){
			deltas[i] = (target[i] - orig[i]) / FLOAT_STEPS_ANIMATION;
		}
		return deltas;
	}

	/** @since 20060328 */
	private static Color[] calcGradient( Color colorOrig ){
		float[] orig  = colorOrig.getRGBColorComponents( new float[3] );
		float[] delta = calcDeltas( COMPONENTS_TARGET, orig );
		float[] limbo = new float[3];
		System.arraycopy( orig, 0, limbo, 0, limbo.length );

		Color[] gradient = new Color[ INT_STEPS_ANIMATION ];
		gradient[0] = colorOrig;
		for( int i=1; i<INT_STEPS_ANIMATION; i++ ){
			for( int j=0; j<3; j++ ){
				limbo[j] += delta[j];
				if(      limbo[j] > FLOAT_ONE  ) limbo[j] = FLOAT_ONE;
				else if( limbo[j] < FLOAT_ZERO ) limbo[j] = FLOAT_ZERO;
			}
			gradient[i] = new Color( limbo[0], limbo[1], limbo[2] );
		}
		gradient[ gradient.length-1 ] = COLOR_TARGET;

		return gradient;
	}

	/** @since 20080315 */
	public StackedMessage later( String text ){
		return this.later( text, true, true );
	}

	/** @since 20080315 */
	public StackedMessage later( String text, boolean immediately, boolean sleep ){
		if( text != null ){ this.runtext = text; }
		this.immediately                 = immediately;

		if(                 SwingUtilities .isEventDispatchThread() ){ run(); }
		else{ try{          SwingUtilities .invokeLater( (Runnable) StackedMessage.this );
		       if( sleep ){
		               Thread .yield();
		         if( ! Thread .currentThread().isInterrupted() ){ Thread .sleep( 4 ); }
		       }
		      }catch( InterruptedException thrown ){
		       if( edu.ucla.belief.ui.util.Util.DEBUG_VERBOSE && true ){ System .err.println( "warning: StackedMessage.later() caught " + thrown ); }
		      }
		}

		return this;
	}

	/** @since 20080315 */
	public void run(){
		synchronized( synch ){
			if( this.runtext != null ){ this.label.setText( this.runtext ); this.runtext = null; }
			if( this.immediately     ){ this.label.paintImmediately( theImmediatelyRectangle ); }
		}
	}

	public static final Rectangle theImmediatelyRectangle = new Rectangle( 1000, 100 );

	private  int          myHandle;
	private  String       strDefault = "", runtext;
	private  Object       synch      = new Object();
	private  JLabel       label;
	private  LinkedList   stack;
	private  Collection   deathrow;

	private  boolean      myFlagAnimate, myFlagAnimateText, immediately;
	private  Color        myColorOrig;
	private  Color[]      myGradient;
	private  ThreadGroup  myGroupAnimation = new ThreadGroup( StackedMessage.class.getName() + " animation" );
	private  StringBuffer myBuff;

  //public   boolean      debug            = false;
}
