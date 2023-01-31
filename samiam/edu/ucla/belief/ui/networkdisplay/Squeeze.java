package edu.ucla.belief.ui.networkdisplay;

import edu.ucla.belief.ui.NetworkInternalFrame;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.Icon;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.*;

/**
	@author Keith Cascio
	@since 082604
*/
public class Squeeze implements ChangeListener, ActionListener, SelectionListener
{
	public static void doSqueeze( NetworkInternalFrame myHNIFToSqueeze, Point pActual )
	{
		new Squeeze( myHNIFToSqueeze ).squeeze( pActual );
	}

	public Squeeze( NetworkInternalFrame myHNIFToSqueeze )
	{
		this.myNetworkInternalFrame = myHNIFToSqueeze;
	}

	public void squeeze( Point pActual )
	{
		JLabel labelSqueeze = getLabel();

		NetworkDisplay networkdisplay = myNetworkInternalFrame.getNetworkDisplay();

		networkdisplay.getJDesktopPane().add( labelSqueeze, JLayeredPane.PALETTE_LAYER );
		labelSqueeze.setLocation( pActual );

		Rectangle rect = new Rectangle( labelSqueeze.getLocation(), labelSqueeze.getPreferredSize() );
		labelSqueeze.setBounds( rect );

		labelSqueeze.setVisible( true );

		networkdisplay.revalidate();
		networkdisplay.repaint();

		//System.out.println( "networkdisplay.isAncestorOf( labelSqueeze ) ? " + networkdisplay.isAncestorOf( labelSqueeze ) );
		//System.out.println( "labelSqueeze.getLocation() ? " + labelSqueeze.getLocation() );
		//System.out.println( "labelSqueeze.getBounds() ? " + labelSqueeze.getBounds() );
		//System.out.println( "labelSqueeze.isVisible() ? " + labelSqueeze.isVisible() );

		networkdisplay.getContentPane().add( getPanel(), BorderLayout.NORTH );

		init( networkdisplay, pActual );

		networkdisplay.addSelectionListener( (SelectionListener)this );
	}

	private void init( NetworkDisplay networkdisplay, Point squeezePoint )
	{
		Collection components = networkdisplay.getAllComponents();

		if( (myArraySqueezed == null) || (myArraySqueezed.length != components.size()) ){
			myArraySqueezed = new Squeezed[ components.size() ];
		}

		int i=0;
		for( Iterator it = components.iterator(); it.hasNext(); ){
			myArraySqueezed[i++] = new Squeezed( (NetworkComponentLabel)it.next(), squeezePoint, networkdisplay );
		}
	}

	public static class Squeezed implements SelectionListener
	{
		public Squeezed( NetworkComponentLabel cv, Point squeezePoint, NetworkDisplay networkdisplay )
		{
			this.myCoordinateVirtual = cv;
			Point myOriginalLocation = cv.getActualLocation( new Point() );
			this.myOriginalX = myOriginalLocation.getX();
			this.myOriginalY = myOriginalLocation.getY();

			int intadjacent = myOriginalLocation.x - squeezePoint.x;
			int intopposite = myOriginalLocation.y - squeezePoint.y;

			if( (intadjacent == (int)0) && (intopposite == (int)0)){
				this.myXFactor = this.myYFactor = (double)0;
				return;
			}

			double adjacent = (double)intadjacent;
			double opposite = (double)intopposite;
			double hypotenuse = Math.sqrt( (adjacent*adjacent) + (opposite*opposite) );

			this.myXFactor = adjacent/hypotenuse;
			this.myYFactor = opposite/hypotenuse;

			this.myFlagSelected = cv.isSelected();
			this.myNetworkDisplay = networkdisplay;
			networkdisplay.addSelectionListener( (SelectionListener)this );
		}

		public void selectionChanged( NetworkComponentLabel label ){
			if( label == myCoordinateVirtual ){
				myFlagSelected = label.isSelected();
			}
		}

		public void selectionReset(){
			myFlagSelected = false;
		}

		public void squeeze( double magnitude, boolean onlySelected )
		{
			if( onlySelected && (!myFlagSelected) ) return;

			mySqueezedLocation.setLocation( myOriginalX + (magnitude*myXFactor), myOriginalY + (magnitude*myYFactor) );
			myCoordinateVirtual.setActualLocation( mySqueezedLocation );
		}

		public void cancel(){
			myCoordinateVirtual.recalculateActual();
		}

		public void commit(){
			myCoordinateVirtual.confirmActualLocation();
			myCoordinateVirtual.getActualLocation( mySqueezedLocation );
			myCoordinateVirtual.setActualLocation( mySqueezedLocation );
		}

		public void die(){
			this.myNetworkDisplay.removeSelectionListener( (SelectionListener)this );
			this.myNetworkDisplay = null;
			this.myCoordinateVirtual = null;
			this.mySqueezedLocation = null;
		}

		public CoordinateVirtual myCoordinateVirtual;
		private double myOriginalX;
		private double myOriginalY;
		private Point mySqueezedLocation = new Point();
		private double myXFactor;
		private double myYFactor;
		private boolean myFlagSelected;
		private NetworkDisplay myNetworkDisplay;
	}

	/** @since 090804
		interface ChangeListener */
	public void selectionChanged( NetworkComponentLabel label ){
		if( label.isSelected() ) myCheckBoxSelected.setSelected( true );
	}

	/** @since 090804
		interface ChangeListener */
	public void selectionReset(){
		myCheckBoxSelected.setSelected( false );
	}

	/** interface ChangeListener */
	public void stateChanged( ChangeEvent e )
	{
		Object src = e.getSource();
		if( src == myJSlider )
		{
			double value = (double) myJSlider.getValue();
			boolean flagOnlySelected = myCheckBoxSelected.isSelected();
			for( int i=0; i<myArraySqueezed.length; i++ ){
				myArraySqueezed[i].squeeze( value, flagOnlySelected );
			}
		}
	}

	/** interface ActionListener */
	public void actionPerformed( ActionEvent e )
	{
		Object src = e.getSource();
		if( src == myButtonCancel ){
			cancel();
			cleanup();
		}
		else if( src == myButtonOkay ){
			commit();
			cleanup();
		}
	}

	private void cancel()
	{
		if( myArraySqueezed != null ){
			for( int i=0; i<myArraySqueezed.length; i++ ){
				myArraySqueezed[i].cancel();
			}
		}
	}

	private void commit()
	{
		if( myArraySqueezed != null )
		{
			Point actual = new Point();
			int minX = 0;
			int minY = 0;
			for( int i=0; i<myArraySqueezed.length; i++ ){
				myArraySqueezed[i].myCoordinateVirtual.getActualLocation( actual );
				minX = Math.min( minX, actual.x );
				minY = Math.min( minY, actual.y );
			}
			if( minX < 0 || minY < 0 ){
				NetworkDisplay networkdisplay = myNetworkInternalFrame.getNetworkDisplay();
				int deltaX = Math.max( -minX, 0 );
				int deltaY = Math.max( -minY, 0 );
				networkdisplay.moveAllNodes( deltaX, deltaY );
			}
			for( int i=0; i<myArraySqueezed.length; i++ ){
				myArraySqueezed[i].commit();
			}
		}
	}

	private void cleanup()
	{
		if( myNetworkInternalFrame != null ){
			NetworkDisplay networkdisplay = myNetworkInternalFrame.getNetworkDisplay();
			networkdisplay.getJDesktopPane().remove( myLabelSqueeze );
			networkdisplay.getContentPane().remove( myPanel );
			networkdisplay.revalidate();
			networkdisplay.repaint();
		}

		if( myArraySqueezed != null ){
			for( int i=0; i<myArraySqueezed.length; i++ ){
				if( myArraySqueezed[i] != null ){
					myArraySqueezed[i].die();
					myArraySqueezed[i] = null;
				}
			}
		}
	}

	public static final Color COLOR_ICON = Color.red;
	public static final int INT_ICON_WIDTH = (int)8;
	public static final int INT_ICON_HEIGHT = (int)8;

	public static class SqueezeIcon implements Icon
	{
		public void paintIcon( Component c, Graphics g, int x, int y ){
			g.setColor( COLOR_ICON );
			g.fillOval( x, y, INT_ICON_WIDTH, INT_ICON_HEIGHT );
		}

		public int getIconWidth(){
			return INT_ICON_WIDTH;
		}

		public int getIconHeight(){
			return INT_ICON_HEIGHT;
		}
	}

	public JLabel getLabel()
	{
		if( myLabelSqueeze == null ){
			myLabelSqueeze = new JLabel( "squeeze point", new SqueezeIcon(), SwingConstants.LEFT );
		}
		myLabelSqueeze.setVisible( false );
		return myLabelSqueeze;
	}

	public JComponent getPanel()
	{
		if( myPanel == null ){
			myPanel = new JPanel( new GridBagLayout() );

			myButtonOkay = initButton( "OK" );
			myButtonCancel = initButton( "Cancel" );
			myCheckBoxSelected = new JCheckBox( "selected only" );

			GridBagConstraints c = new GridBagConstraints();
			c.gridwidth = 1;

			myPanel.add( new JLabel( "{ squeeze" ), c );

			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			myPanel.add( getSlider(), c );

			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			myPanel.add( new JLabel( "spread }" ), c );
			myPanel.add( myCheckBoxSelected, c );
			myPanel.add( myButtonCancel, c );

			c.gridwidth = GridBagConstraints.REMAINDER;
			myPanel.add( myButtonOkay, c );
		}

		return myPanel;
	}

	public static final Insets INSETS_BUTTONS = new Insets(0,0,0,0);

	private JButton initButton( String text )
	{
		JButton ret = new JButton( text );
		ret.setMargin( INSETS_BUTTONS );
		ret.addActionListener( this );
		return ret;
	}

	public static final int INT_SLIDER_MIN = (int)-300;
	public static final int INT_SLIDER_MAX = (int)300;
	public static final int INT_SLIDER_INIT = (int)0;

	public JSlider getSlider()
	{
		if( myJSlider == null ){
			myJSlider = new JSlider( JSlider.HORIZONTAL, INT_SLIDER_MIN, INT_SLIDER_MAX, INT_SLIDER_INIT );
			myJSlider.addChangeListener( (ChangeListener)this );
		}
		myJSlider.setValue( INT_SLIDER_INIT );
		return myJSlider;
	}

	private JCheckBox myCheckBoxSelected;
	private JLabel myLabelSqueeze;
	private JSlider myJSlider;
	private JButton myButtonOkay;
	private JButton myButtonCancel;
	private JComponent myPanel;
	private NetworkInternalFrame myNetworkInternalFrame;
	private Squeezed[] myArraySqueezed;
}
