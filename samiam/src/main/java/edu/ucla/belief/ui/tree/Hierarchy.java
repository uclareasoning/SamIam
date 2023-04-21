package edu.ucla.belief.ui.tree;

import edu.ucla.belief.ui.actionsandmodes.SamiamAction;
import edu.ucla.belief.ui.util.Util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/** A ui component designed to provide a rich set
	of related functions in a collapsible tree.
	The user can choose which function he wants to
	see.  The functions of interest stay visible,
	unlike a traditional popup menu design.

	@author keith cascio
	@since 20070324 */
public class Hierarchy extends JPanel implements Scrollable
{
	/** {@link #Hierarchy(String,String) Hierarchy( String, String )} is a better choice */
	public Hierarchy( String text ){
		this( text, text );
	}

	/** a simple text header with tooltip explanation */
	public Hierarchy( String text, String tooltip ){
		this( new JLabel( text, JLabel.LEFT ) );
		myHead.setToolTipText( tooltip );
	}

	/** a component header, which also serves as the "brain", see {@link #Hierarchy(JComponent,JComponent) Hierarchy( head, brain )} */
	public Hierarchy( JComponent head ){
		this( head, head );
	}

	/** @param head  the header component
		@param brain the clickable part of the head that controls expansion/collapsing of this hierarchy */
	public Hierarchy( JComponent head, JComponent brain ){
		super( new GridBagLayout() );
		myHead  = head;
		myBrain = brain;
		init();
	}

	/** a root hierarchy with no header */
	public Hierarchy(){
		this( (JComponent)null, (JComponent)null );
	}

	/** interface Scrollable
		@since 20070326 */
	public Dimension getPreferredScrollableViewportSize(){
		return Hierarchy.this.getPreferredSize();
	}
	/** interface Scrollable
		@since 20070326 */
	public int       getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ){
		return 0x10;
	}
	/** interface Scrollable
		@since 20070326 */
	public boolean   getScrollableTracksViewportHeight(){
		return false;
	}
	/** interface Scrollable
		@since 20070326 */
	public boolean   getScrollableTracksViewportWidth(){
		return true;
	}
	/** interface Scrollable
		@since 20070326 */
	public int       getScrollableUnitIncrement(  Rectangle visibleRect, int orientation, int direction ){
		return 1;
	}

	private void init(){
		myC.weightx   = 1;
		myC.fill      = GridBagConstraints.HORIZONTAL;
		myC.gridwidth = GridBagConstraints.REMAINDER;

		if( myHead != null ){
			super.addImpl( head( myHead ),                                 myC, 0 );
			super.addImpl( myContents = new JPanel( new GridBagLayout() ), myC, 1 );
			setExpanded( myHead == null );

			myMouseListener = new MouseAdapter(){
				public void mousePressed( MouseEvent e ){
					Hierarchy.this.setSelected( true );
				}
			};
		}
		else myContents = this;

		if( myBrain != null ){
			MouseListener ml = new MouseAdapter(){
				public void mousePressed( MouseEvent e ){
					Hierarchy.this.setExpanded( ! myContents.isVisible() );
				}
			};
			myBrain.addMouseListener( ml );
		}
	}

	//public static final String STR_EXPANDED  = "-",
	//                           STR_COLLAPSED = "+";
	public static final String STR_EXPANDED  = Util.supported( new String[]{ "\u25c0", "\u25c4", "\u25c1", "\u25c5", "\u25c2", "\u25c3", "<" } ),
							   STR_COLLAPSED = Util.supported( new String[]{ "\u25b6", "\u25ba", "\u25b7", "\u25bb", "\u25b8", "\u25b9", ">" } );

	public void setExpanded( boolean flag ){
		if( myIndicator != null ) myIndicator.setText( flag ? STR_EXPANDED : STR_COLLAPSED );
		myContents.setVisible( flag );
		this.setSelected(      flag );
	}

	public void setSelected( boolean flag ){
		/*if( flag ){
			if( SELECTED != null ) SELECTED.setSelected( false );
			SELECTED = this;
		}*/

		if( flag ) requestSelection( this );

		Border b = flag ? BORDER_SELECTED : (myContents.isVisible() ? BORDER_EXPANDED : BORDER_COLLAPSED);
		this.setBorder( b );

		//System.out.println( b.getClass().getName() + " insets " + b.getBorderInsets( this ) );
	}

	public Component add( SamiamAction action ){
		return this.add( conf( new JButton( action ) ) );
	}

	protected void addImpl( Component comp, Object constraints, int index ){
		if( comp == null ) return;

		if( comp instanceof Hierarchy ){
			if( myContents.getComponentCount() > 0 ) myContents.add( Box.createVerticalStrut(2), myC );
			((Hierarchy)comp).myParent = this;
		}
		else listenDeeply( comp );

		if( myContents == this ) super.addImpl( comp, myC, index );
		else                    myContents.add( comp, myC );
	}

	private void listenDeeply( Component comp ){
		if( (comp == null) || (comp instanceof Hierarchy) || (myMouseListener == null) ) return;

		MouseListener[] array = comp.getMouseListeners();
		if( (array != null) && (array.length > 0) ) comp.addMouseListener( myMouseListener );

		if( comp instanceof Container ){
			//for( Component child : ((Container)comp).getComponents() ) listenDeeply( child );
			Component[] comps = ((Container)comp).getComponents();
			for( int i=0; i<comps.length; i++ ) listenDeeply( comps[i] );
		}
	}

	private Hierarchy requestSelection( Hierarchy child ){
		if( myParent == null ){
			Hierarchy ret = myLastSelectedChild;
			if( child == myLastSelectedChild ) return ret;
			if( myLastSelectedChild != null ) myLastSelectedChild.setSelected( false );
			myLastSelectedChild = child;
			return ret;
		}
		else return myParent.requestSelection( myLastSelectedChild = child );
	}

	private JComponent head( JComponent comp ){
		JPanel             ret = new JPanel( new GridBagLayout() );
		GridBagConstraints c   = new GridBagConstraints();

		c.anchor  = GridBagConstraints.WEST;
		c.fill    = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		ret.add( comp, c );
		c.fill    = GridBagConstraints.NONE;
		c.weightx = 0;
		ret.add( Box.createHorizontalStrut(4), c );
		c.anchor  = GridBagConstraints.EAST;
		c.weightx = 0;
		ret.add( myIndicator = new JLabel( STR_COLLAPSED ), c );

		/*Dimension dim = ret.getPreferredSize();
		dim.width = INT_WIDTH_MENU;
		ret.setPreferredSize( dim );*/

		//ret.setBorder(  BorderFactory.createLineBorder( Color.red,  1 ) );
		//comp.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );

		return ret;
	}

	public static final Insets MARGIN = new Insets( 0,4,0,4 );

	public static AbstractButton button( String text ){
		return conf( new JButton( text ) );
	}

	public static AbstractButton conf( AbstractButton btn ){
		btn.setMargin( MARGIN );
		btn.setFont( btn.getFont().deriveFont( (float)10 ) );
		return btn;
	}

	//private static Hierarchy SELECTED = null;
	private Hierarchy          myParent, myLastSelectedChild;

	private JComponent         myHead, myBrain;
	private MouseListener      myMouseListener;
	private JLabel             myIndicator;
	private JPanel             myContents;
	private GridBagConstraints myC = new GridBagConstraints();

	public static final Border BORDER_EXPANDED  = BorderFactory.createEtchedBorder();
  //public static final Border BORDER_EXPANDED  = BorderFactory.createRaisedBevelBorder();
	public static final Border BORDER_COLLAPSED = BorderFactory.createEmptyBorder( 2, 2, 2, 2 );
  //public static final Border BORDER_SELECTED  = BorderFactory.createLineBorder( Color.blue, 2 );
  //public static final Border BORDER_SELECTED  = BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 1, 1, 1, 1 ), BorderFactory.createLineBorder( Color.orange, 1 ) );
	public static final Border BORDER_SELECTED  = BorderFactory.createEtchedBorder( EtchedBorder.LOWERED, Color.orange, Color.orange.darker() );
}
