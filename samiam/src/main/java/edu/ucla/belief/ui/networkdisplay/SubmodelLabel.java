package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;

/** Draw submodels on the screen with a JLabel
	@author keith cascio
	@since  20020418 */
public class SubmodelLabel extends NetworkComponentLabelImpl
{
	public static final int
	  SIZE_WIDTH__MINIMUM        =      100,
	  SIZE_HEIGHT_MINIMUM        =       75,
	  SIZE_WIDTH__BUFFER         =        2,//25,
	  SIZE_HEIGHT_BUFFER         =       25;//35;

	private NetworkDisplay       myParentNetworkDisplay, myChildNetworkDisplay;
	private NetworkInternalFrame hnInternalFrame;
	private DSLSubmodel          myDSLSubmodel;
	private JInternalFrame       myDisplayInternalFrame;

	public  SubmodelLabel( DSLSubmodel             sm,
	                       NetworkInternalFrame    hnInternalFrame,
	                       SamiamPreferences       sPrefs,
	                       NodeIcon                image,
	                       NetworkDisplay          owner ){
	 	super( sm == null ? "" : sm.getName(),     image, owner, owner, sPrefs,
	 			sm.getLocation( new Point() ), false );

		this.myDSLSubmodel   = sm;
		this.hnInternalFrame = hnInternalFrame;

		setText( myDSLSubmodel.getName() );

		this.myChildNetworkDisplay = new NetworkDisplay( hnInternalFrame, sPrefs, myDSLSubmodel );
		myChildNetworkDisplay.setTitle( "Submodel " + getText() );
		myChildNetworkDisplay.setClosable( true );

		this.myParentNetworkDisplay = owner;

		initLazy();
	}

	public  SubmodelLabel( DSLSubmodel             sm,
	                       NetworkInternalFrame    hnInternalFrame,
	                       NodeIcon                image,
	                       NetworkDisplay          owner,
	                       SamiamPreferences       sPrefs ){
		this( sm, hnInternalFrame, sPrefs, image, owner );
	}

	/** @since 20020508 */
	public NetworkDisplay getChildNetworkDisplay()
	{
		return myChildNetworkDisplay;
	}

	public NetworkDisplay showNetworkDisplay()
	{
		return showNetworkDisplayJInternalFrame();
	}

	private Dimension calculateSize()
	{
		Dimension dim  = new Dimension( myChildNetworkDisplay.getPreferredSize() );
		dim.width     +=                        SIZE_WIDTH__BUFFER;
		dim.height    +=                        SIZE_HEIGHT_BUFFER;
		dim.width      = Math.max( dim  .width, SIZE_WIDTH__MINIMUM );
		dim.height     = Math.max( dim .height, SIZE_HEIGHT_MINIMUM );
		return dim;
	}

	/** @since 20020918 */
	private NetworkDisplay showNetworkDisplayJInternalFrame()
	{
		if( myDisplayInternalFrame == null )
		{
			//JScrollPane scrollPain = new JScrollPane( myChildNetworkDisplay );
			Dimension dimInitSize = calculateSize();
			//myDisplayInternalFrame = this.hnInternalFrame.showInternalFrame( "Submodel " + getText(), scrollPain, dimInitSize );
			myDisplayInternalFrame = myChildNetworkDisplay;
			this.hnInternalFrame.setInternalFrame( myDisplayInternalFrame, new Rectangle( new Point( 25,25 ), dimInitSize ) );
		}

		myChildNetworkDisplay.revalidate();
		//myChildNetworkDisplay.debugPrintNodeLocations();//debug
		myDisplayInternalFrame.setVisible( true );
		try{
			myDisplayInternalFrame.setSelected( true );
		}catch( java.beans.PropertyVetoException e ){
			System.err.println( "Java warning failed to give focus to internal frame: " + myDisplayInternalFrame.getTitle() );
		}

		return myChildNetworkDisplay;
	}

	/** @since 20030331 */
	public void removeFromParentManaged()
	{
		myParentNetworkDisplay.getJDesktopPane().remove( this );
		if( myChildNetworkDisplay != null )
		{
			Container parent = myChildNetworkDisplay.getParent();
			if( parent != null ) parent.remove( myChildNetworkDisplay );
		}
	}

	public void doDoubleClick()
	{
		showNetworkDisplay();
	}

	public static String s( Point p ){
		return "[" + p.x + "," + p.y + "]";
	}

	public static String s( Dimension d ){
		return "[" + d.width + "," + d.height + "]";
	}

	protected void setVirtualLocationHook( Point p )
	{
		myDSLSubmodel.setLocation( p );
	}

	protected Point getVirtualLocationHook( Point p )
	{
		return myDSLSubmodel.getLocation( p );
	}

	protected void setVirtualSizeHook( Dimension d )
	{
		myVirtualSize.setSize( d );
	}

	protected Dimension getVirtualSizeHook( Dimension d )
	{
		if( d == null ) d = new Dimension();
		d.setSize( myVirtualSize );
		return d;
	}

	/** @since 20040818 */
	public void recalculateActualSize(){
		if( myVirtualSize != null ) super.recalculateActualSize();
	}

	/** @since 20040818 */
	protected void setShapePreference( IconFactory factory )
	{
		//value = null;//ignore

		factory = IconFactory.SQUARE;//always square
		NodeIcon current = getNodeIcon();

		if( ! factory.corresponds( current ) ){
			NodeIcon newIcon = factory.makeIcon( (DisplayableFiniteVariable)null );//, myNetPrefs );
			if( current != null ){
				newIcon.setSelected( current.isSelected() );
				newIcon.setObserved( false/*current.isObserved()*/ );
				newIcon.setHidden( current.isHidden() );
			}
			setIcon( newIcon );
			this.recalculateActualSize();
		}
	}

	protected Dimension myVirtualSize = new Dimension( 80, 40 );
}
