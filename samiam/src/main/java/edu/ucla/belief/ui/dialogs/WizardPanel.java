package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.util.*;

import edu.ucla.util.Interruptable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**	@author Keith Cascio
	@since 032605 */
public class WizardPanel extends JPanel implements WizardListener, ActionListener
{
	public WizardPanel( Wizard wizard ){
		super( new BorderLayout() );
		this.myWizard = wizard;
		myWizard.addWizardListener( (WizardListener)this );
		init();
	}

	private void init(){
		if( myButtonBack != null ) throw new IllegalStateException();

		myPanelButtons = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		myPanelButtons.add( myButtonBack = makeButton( "<- Back", "Go back one step" ), c );
		myPanelButtons.add( myButtonRewind = makeButton( "<<-", "Rewind" ), c );
		c.weightx = 1;
		myPanelButtons.add( Box.createHorizontalStrut(1), c );
		c.weightx = 0;
		myPanelButtons.add( myButtonCancel = makeButton( STR_CANCEL_TEXT_DEFAULT, STR_CANCEL_TOOLTIP_DEFAULT ), c );
		myPanelButtons.add( Box.createHorizontalStrut( 64 ), c );
		myPanelButtons.add( myButtonFastForward = makeButton( "->>", "Fast forward" ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		myPanelButtons.add( myButtonNext = makeButton( "Next ->", "Proceed forward one step" ), c );

		this.add( myPanelButtons, BorderLayout.SOUTH );
	}

	/** @since 030105 */
	private JButton makeButton( String text, String tooltip ){
		JButton ret = new JButton( text );
		ret.setToolTipText( tooltip );
		ret.addActionListener( (ActionListener)this );
		return ret;
	}

	public static final String STR_CANCEL_TEXT_DEFAULT = "Cancel";
	public static final String STR_CANCEL_TOOLTIP_DEFAULT = "Exit wizard without finishing";

	public void setTextCancelButton( String text, String tooltip ){
		if( myButtonCancel == null ) return;
		myButtonCancel.setText( text );
		myButtonCancel.setToolTipText( tooltip );
	}

	public void actionPerformed( ActionEvent evt ){
		Object src = evt.getSource();
		if( src == myButtonBack ) myRunPrevious.start();
		else if( src == myButtonNext ) myRunNext.start();
		else if( src == myButtonCancel ) doCancel();
		else if( src == myButtonFastForward ) myRunFastForward.start();
		else if( src == myButtonRewind ) myRunRewind.start();
	}

	public void reset(){
		setTextCancelButton( STR_CANCEL_TEXT_DEFAULT, STR_CANCEL_TOOLTIP_DEFAULT );
		doTransition( myWizard.getFirst() );
	}

	/** @since 030105 */
	private void doCancel(){
		Window window = SwingUtilities.getWindowAncestor( (Component)this );
		if( window != null ){
			window.setVisible( false );
			window.dispose();
		}
	}

	//public static final long LONG_DELAY_FAST_FORWARD = (long)600;

	/** @since 022505 */
	public Interruptable myRunFastForward = new Interruptable(){
		public void runImpl( Object arg1 ) throws InterruptedException{
			Thread.sleep(4);//Interruptable.checkInterrupted();
			//try{
			//	Thread.sleep( LONG_DELAY_FAST_FORWARD );
			//}catch( InterruptedException interruptedexception ){
			//}
			//Thread.sleep(4);//Interruptable.checkInterrupted();
			mySkips = 0;
			while( myButtonNext.isEnabled() && doNextImpl() ){
				++mySkips;
				Thread.sleep(4);//Interruptable.checkInterrupted();
			}
		}
		int mySkips;
	};

	/** @since 030705 */
	public Interruptable myRunRewind = new Interruptable(){
		public void runImpl( Object arg1 ) throws InterruptedException{
			Thread.sleep(4);//Interruptable.checkInterrupted();
			mySkips = 0;
			while( myButtonBack.isEnabled() && doPreviousImpl() ){
				++mySkips;
				Thread.sleep(4);//Interruptable.checkInterrupted();
			}
		}
		int mySkips;
	};

	/** @since 032805 */
	public Interruptable myRunNext = new Interruptable(){
		public void runImpl( Object arg1 ) throws InterruptedException{
			Thread.sleep(4);//Interruptable.checkInterrupted();
			WizardPanel.this.doNextImpl();
		}
	};

	private boolean doNextImpl(){
		if( myCurrentStage == null ) return false;
		String warning = myCurrentStage.getWarning();
		if( warning != null ){
			int result = JOptionPane.showConfirmDialog( this, warning + "\nProceed anyway?", "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE );
			if( result != JOptionPane.OK_OPTION ) return false;
		}
		Stage nextstage = null;
		try{
			nextstage = myCurrentStage.next();
		}catch( Throwable throwable ){
			String msg = throwable.getMessage();
			if( msg == null ) msg = throwable.toString();
			JOptionPane.showMessageDialog( this, msg, "Error", JOptionPane.ERROR_MESSAGE );
			return false;
		}
		return doTransition( nextstage );
	}

	/** @since 032805 */
	public Interruptable myRunPrevious = new Interruptable(){
		public void runImpl( Object arg1 ) throws InterruptedException{
			Thread.sleep(4);//Interruptable.checkInterrupted();
			WizardPanel.this.doPreviousImpl();
		}
	};

	private boolean doPreviousImpl(){
		if( myCurrentStage == null ) return false;
		else return doTransition( myCurrentStage.previous() );
	}

	private boolean doTransition( Stage newStage ){
		if( newStage == null ) return false;

		boolean ret = false, anticipated = false;
		Cursor cursor = null;
		JComponent compLast = null;
		if( myCurrentStage != null ){
			try{
				compLast = myCurrentStage.getView( /*validate*/false );
			}catch( Throwable throwable ){
				compLast = null;
				System.err.println( "Warning: " + throwable );
			}
		}
		anticipated = anticipateTransition( compLast, newStage );
		if( anticipated ){
			cursor = this.getCursor();
			this.setCursor( UI.CURSOR_WAIT );
		}
		JComponent compNext = null;
		try{
			compNext = newStage.getView( /*validate*/true );
			if( compNext == null ) throw new IllegalStateException( "unknown" );
			ret = doTransition( myCurrentStage, compLast, newStage, compNext );
		}catch( Throwable throwable ){
			JOptionPane.showMessageDialog( this, throwable.toString(), "Error", JOptionPane.ERROR_MESSAGE );
			return false;
		}finally{
			if( anticipated && (cursor != null) ) this.setCursor( cursor );
		}
		return ret;
	}

	private Component removeCenter(){
		//Component comp = myBorderLayout.getLayoutComponent( BorderLayout.CENTER );
		if( myCenter != null ) this.remove( myCenter );
		return myCenter;
	}

	private void setCenter( JComponent comp ){
		if( comp == null ) return;
		removeCenter();
		this.add( myCenter = comp, BorderLayout.CENTER );
	}

	private JComponent myCenter;
	private JLabel myLabelProgess;
	private JPanel myPanelProgress;
	public static final Color COLOR_PROGESS = new Color( 0x66, 0x00, 0x00 );

	private JComponent getProgressComponent( String message ){
		if( myPanelProgress == null ){
			myLabelProgess = new JLabel();
			myLabelProgess.setFont( myLabelProgess.getFont().deriveFont( (float)22 ) );
			myLabelProgess.setForeground( COLOR_PROGESS );

			myPanelProgress = new JPanel( new GridBagLayout() );
			//myPanelProgress.setBackground( Color.red );
			GridBagConstraints c = new GridBagConstraints();

			c.gridwidth = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			myPanelProgress.add( Box.createHorizontalGlue(), c );

			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 10;
			myPanelProgress.add( myLabelProgess, c );

			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			myPanelProgress.add( Box.createHorizontalGlue(), c );

			AbstractWizard.border( myPanelProgress );
		}
		myLabelProgess.setText( "<html>" + message + "..." );
		return myPanelProgress;
	}

	private boolean anticipateTransition( JComponent oldComp, Stage newStage )
	{
		if( !newStage.isDelayLikely() ) return false;
		//System.out.println( "WP.anticipateTransition( likely delay -> "+newStage.getTitle()+" )" );

		setCenter( getProgressComponent( newStage.getProgressMessage() ) );

		/*new Thread(){
			public void run(){
				//Thread.currentThread().setPriority( Thread.MAX_PRIORITY );
				Thread.yield();
				WizardPanel.this.revalidate();
				WizardPanel.this.repaint();
				//myPanelProgress.paintImmediately( SwingUtilities.getLocalBounds( myPanelProgress ) );
				for( int i=0; i<131072; i++ ){
					Thread.yield();
				}
				WizardPanel.this.revalidate();
				WizardPanel.this.repaint();
			}
		}.start();*/

		//myPanelProgress.repaint();//validate();
		//Thread.yield();
		//this.paintImmediately( SwingUtilities.getLocalBounds( this ) );

		//Thread tCurrent = Thread.currentThread();
		//int priority = tCurrent.getPriority();
		//tCurrent.setPriority( Thread.MIN_PRIORITY );

		this.revalidate();
		this.repaint();
		Thread.yield();

		//try{
		//	Thread.sleep( 8 );
		//}catch( InterruptedException interruptedexception ){
		//	System.err.println( interruptedexception );
		//}

		//tCurrent.setPriority( priority );

		return true;
	}

	private boolean doTransition( Stage oldStage, JComponent oldComp, Stage newStage, JComponent newComp ){
		stretchFor( newComp );
		setCenter( newComp );//this.add( newComp, BorderLayout.CENTER );
		setTitle( newStage.getTitle() );
		myCurrentStage = newStage;

		resetNavigation();

		this.revalidate();
		this.repaint();

		return true;
	}

	/** interface WizardListener */
	public void resetNavigation(){
		//System.out.println( "WizardPanel.resetNavigation()" );
		if( myCurrentStage != null ){
			boolean greenlightprevious = myCurrentStage.isGreenLightPrevious();
			myButtonBack.setEnabled( greenlightprevious );
			myButtonRewind.setEnabled( greenlightprevious );
			boolean greenlightnext = myCurrentStage.isGreenLightNext();
			myButtonNext.setEnabled( greenlightnext );
			myButtonFastForward.setEnabled( greenlightnext );
		}
	}

	/** interface WizardListener */
	public void wizardFinished(){
		Window window = SwingUtilities.getWindowAncestor( (Component)this );
		if( window != null ){
			window.setVisible( false );
			window.dispose();
		}
	}

	/** @since 030105 */
	private void windowListen(){
		Window window = SwingUtilities.getWindowAncestor( (Component)this );
		if( window == null ) return;

		if( myWindowListener == null ){
			myWindowListener = new WindowAdapter(){
				public void windowClosing( WindowEvent e ){
					myWizard.windowClosing( e );
				}
			};
		}

		window.addWindowListener( myWindowListener );
	}
	private WindowListener myWindowListener;

	/** @since 022505 */
	public void setTitle(){
		if( myCurrentStage != null ) setTitle( myCurrentStage.getTitle() );
	}

	public void setTitle( String title ){
		Window window = SwingUtilities.getWindowAncestor( (Component)this );
		if( window instanceof Dialog ) ((Dialog)window).setTitle( title );
		else if( window instanceof Frame ) ((Frame)window).setTitle( title );
	}

	/** @since 022805 */
	public void stretchFor( Component newComp ){
		Window window = SwingUtilities.getWindowAncestor( (Component)this );
		if( window == null ) return;

		Dimension preferred = newComp.getPreferredSize();
		Dimension sizeWindow = window.getSize();
		Rectangle sizeScreen = Util.getScreenBounds();
		Dimension sizeButtons = myPanelButtons.getSize();

		int width = Math.max( preferred.width, sizeWindow.width );
		width = Math.min( width, sizeScreen.width );

		int height = Math.max( preferred.height + sizeButtons.height + 64, sizeWindow.height );
		height = Math.min( height, sizeScreen.height );

		if( (width>sizeWindow.width) || (height>sizeWindow.height) ){
			Dimension sizeAdjusted = new Dimension( width, height );
			window.setSize( sizeAdjusted );
			//((LayoutManager2)this.getLayout()).invalidateLayout( (Container)this );
			//this.revalidate();
			window.validate();
		}
	}

	/** @since 022805 */
	public void pack(){
		Window window = SwingUtilities.getWindowAncestor( (Component)this );
		if( window != null ) window.pack();
	}

	private Stage myCurrentStage;
	private JButton myButtonBack, myButtonNext, myButtonCancel, myButtonRewind, myButtonFastForward;
	private JPanel myPanelButtons;

	//public static final Dimension DIM_WINDOW_DEFAULT = new Dimension( 500, 460 );

	/** @since 022505 */
	public void showDialog( Component parentComponent ){
		WizardPanel cdswp = this;
		cdswp.setPreferredSize( myWizard.getPreferredSize() );
		cdswp.reset();
		Util.makeDialog( parentComponent, cdswp, "", /*modal*/true ).setVisible( true );
	}

	/** @since 022505 */
	public void addNotify(){
		super.addNotify();
		this.setTitle();
		this.windowListen();
	}

	private Wizard myWizard;
}
