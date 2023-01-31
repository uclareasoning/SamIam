package edu.ucla.belief.ui.util;

import java.awt.Container;
import javax.swing.JDialog;
import javax.swing.JComponent;

/** A solution to JOptionPane's unresizeable dialogs.
	@author keith cascio
	@since 20041029 */
public class JOptionResizeHelper implements Runnable
{
	public   static final long LONG_INITIAL_SLEEP_DURATION_MILLIS =  250l,
	                           LONG_MINIMUM_SAFE_TIMEOUT_MILLIS   = 4096l;
	private  static       int  INT_THREAD_NAME_COUNTER            =    0;

	public JOptionResizeHelper( JComponent message, boolean flag, long timeout ){
		this( message, flag, timeout, null );
	}

	/** @since 20071213 */
	public JOptionResizeHelper( JComponent message, boolean flag, long timeout, JOptionResizeHelperListener listener ){
		this.myMessage = message;
		this.myFlag = flag;
		this.myTimeout = timeout;
		this.setListener( listener );
	}

	/** @since 20041101 */
	public JOptionResizeHelper setListener( JOptionResizeHelperListener listener ){
		this.myListener = listener;
		return this;
	}

	/** @since 110104 */
	public interface JOptionResizeHelperListener{
		public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper );
	}

	/** @since 011205 */
	public void start(){
		new Thread( (Runnable)this, "JOptionResizeHelper thread " + Integer.toString( INT_THREAD_NAME_COUNTER++ ) ).start();
	}

	public void run()
	{
		//System.out.println( "JOptionResizeHelper.run()" );
		if( myMessage == null ) return;

		if( myTimeout < (long)1 ) myTimeout = Long.MAX_VALUE;
		else if( myTimeout < LONG_MINIMUM_SAFE_TIMEOUT_MILLIS ) myTimeout = LONG_MINIMUM_SAFE_TIMEOUT_MILLIS;

		myStartMillis = System.currentTimeMillis();
		//System.out.println( "    myStartMillis " + myStartMillis );

		long sleepDuration = LONG_INITIAL_SLEEP_DURATION_MILLIS;
		Container container = null;
		JDialog jdialog = null;
		JDialog mostrecent = null;
		while( elapsed() < myTimeout )
		{
			try{ Thread.sleep( sleepDuration ); }
			catch( InterruptedException e ){
				Thread.currentThread().interrupt();
			  //System.err.println(e);
				return;
			}

			container = myMessage.getTopLevelAncestor();
			if( container instanceof JDialog ){
				jdialog = (JDialog) container;
				if( jdialog.isResizable() != myFlag ){
					//System.out.println( "    (JDialog)" + jdialog.getTitle() + ".setResizable("+myFlag+")" );
					jdialog.setResizable( myFlag );
					sleepDuration = (long)1000;
					myTimeout = Math.max( myTimeout, elapsed() + LONG_MINIMUM_SAFE_TIMEOUT_MILLIS );
				}
				if( (mostrecent != jdialog) && (myListener != null) ) myListener.topLevelAncestorDialog( jdialog, (JOptionResizeHelper) this );
				mostrecent = jdialog;
			}
		}

		//System.out.println( "    timeout at " + System.currentTimeMillis() + " (" + elapsed() + " elapsed)" );
	}

	public long elapsed(){
		return System.currentTimeMillis() - myStartMillis;
	}

	private JOptionResizeHelperListener myListener;
	private JComponent myMessage;
	private boolean myFlag;
	private long myTimeout;
	private long myStartMillis = (long)0;
}
