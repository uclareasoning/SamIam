package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.lang.reflect.Method;

/** @author keith cascio
	@since  20030310 */
public class MonitorMouseListener extends MouseInputAdapter
{
	public    static final String
	  STR_METHOD_ZORDER        =   "setComponentZOrder";

	protected static final Object[]
	  ARRAY_ZORDER             =   new Object[]{ null, new Integer(0) };

	protected        MonitorMouseListener(){
		try{
			methodZorder = Container.class.getMethod( STR_METHOD_ZORDER, new Class[]{ Component.class, Integer.TYPE } );
		}catch( Throwable thrown ){
			if( Util.DEBUG_VERBOSE ){ System.err.println( "verbose warning: MonitorMouseListener constructor caught " + thrown ); }
		}
	}

	public    static MonitorMouseListener getInstance(){
		if(    THEINSTANCE == null ){     THEINSTANCE = new MonitorMouseListener(); }
		return THEINSTANCE;
	}
	protected static MonitorMouseListener THEINSTANCE;

	public void  mouseDragged( MouseEvent e )
	{
		if(      monitor == null ){ return; }
		flagDragHappened  = true;
		Point   newpoint  = e.getPoint();
		monitor.translateActualLocation( newpoint.x - dragstart.x, newpoint.y - dragstart.y );
	}

	public void  mousePressed( MouseEvent e )
	{
	  //System.out.println( "mousePressed("+System.identityHashCode(e.getComponent())+")" );
		flagDragHappened = false;
		dragstart.setLocation( e.getPoint() );
		Component comp = e.getComponent();
		if(       comp instanceof Monitor ){
			monitor =     (Monitor) comp;
			Container       cont  = comp.getParent();
			if(             cont != null ){
				try{
					if( methodZorder != null ){// java version >= 5
						ARRAY_ZORDER[0] = comp;
						methodZorder.invoke( cont, ARRAY_ZORDER );//cont.setComponentZOrder( comp, 0 );
					}else                     {// java version <= 4
						cont.remove( comp );   //
						cont.add(    comp, 0 );// remove() + add() =~ poor man's setComponentZOrder()
					}
				}catch( Throwable thrown ){
					if( Util.DEBUG_VERBOSE ){ System.err.println( "verbose warning: MonitorMouseListener.mousePressed() caught " + thrown ); }
				}
			}
		}
	}

	public void mouseReleased( MouseEvent e )
	{
	  //System.out.println( "mouseReleased("+System.identityHashCode(e.getComponent())+")" );
		if( e.getComponent() == monitor ){
			if( e.isPopupTrigger() ){
				monitor4popup     = monitor;
				point             = e.getPoint();
				invoker           = e.getComponent();
				SwingUtilities.invokeLater( runPopup );
			}
			else if( flagDragHappened ){
				monitor.confirmActualLocation();
				monitor.notifyBoundsChange();
				flagDragHappened = false;
			}
		}
		monitor = null;
	}

	/** @since 20090425 */
	protected Runnable runPopup = new Runnable(){
		public void run(){
			try{
				SwingUtilities.convertPointToScreen( point, invoker );
				JPopupMenu popup = getPopup();
				popup.setLocation(  point );
				popup.setInvoker( invoker );
				popup.setVisible(    true );
			}catch( Throwable thrown ){
				System.err.println( "warning: MonitorMouseListener.runPopup.run() caught " + thrown );
			}
		}
	};

	public void  mouseClicked( MouseEvent e )
	{
	  //System.out.println( "mouseClicked("+System.identityHashCode(e.getComponent())+")" );
		Component comp = e.getComponent();
		if(      (comp != monitor) && (comp instanceof Monitor) ){ monitor = (Monitor) comp; }
		if( monitor != null ){
			if( false && SwingUtilities.isRightMouseButton( e ) ){ monitor.setVisible( false ); }
			else{ monitor.rotate(); }
			monitor  = null;
		}
	}

	/** @since 20090424 */
	public JPopupMenu getPopup(){
		if( popup == null ){
			popup = new JPopupMenu();
			popup.add( actionCopy );
		}
		return popup;
	}

	/** @since 20090424 */
	protected Action actionCopy = new SamiamAction( "copy", "copy the text of the monitor to the system clipboard", 'c', MainToolBar.getIcon( "Copy16.gif" ) ){
		public void actionPerformed( ActionEvent e ){
			try{
				Util.copyToSystemClipboard( monitor4popup.toString() );
			}catch( Throwable thrown ){
				System.err.println( "warning: MonitorMouseListener.actionCopy.actionPerformed() caught " + thrown );
			}
		}
	};

	protected boolean               flagDragHappened = false;
	protected Point                 dragstart        = new Point(), point;
	protected Monitor               monitor, monitor4popup;
  //protected IllegalStateException ise              = new IllegalStateException( "e.getComponent() != monitor" );
	protected JPopupMenu            popup;
	protected Component             invoker;
	protected Method                methodZorder;
}
