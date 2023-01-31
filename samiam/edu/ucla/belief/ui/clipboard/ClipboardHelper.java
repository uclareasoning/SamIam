package edu.ucla.belief.ui.clipboard;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.*;
import java.awt.*;
//import java.util.ArrayList;
//import java.util.Iterator;

/** @author Keith Cascio
	@since 20050921
*/
public class ClipboardHelper extends MouseAdapter implements MouseListener, ActionListener
{
	public ClipboardHelper( JLabel comp1, JLabel comp2, JComponent invoker ){
		//this.myLabel1 = comp1;
		//this.myLabel2 = comp2;
		this.myInvoker = invoker;

		this.myAdapters = new TextAdapter[2];
		int index = 0;
		if( comp1 != null ){
			myAdapters[index++] = new TextAdapter( comp1 );
			comp1.addMouseListener( (MouseListener)this );
		}
		if( comp2 != null ){
			myAdapters[index++] = new TextAdapter( comp2 );
			comp2.addMouseListener( (MouseListener)this );
		}
	}

	public ClipboardHelper( JTextComponent comp1, JComponent invoker ){
		//this.myJTextComponent = comp1;
		this.myInvoker = invoker;

		this.myAdapters = new TextAdapter[1];
		if( comp1 != null ){
			myAdapters[0] = new TextAdapter( comp1 );
			comp1.addMouseListener( (MouseListener)this );
		}
	}

	public void mousePressed( MouseEvent e ){
		ClipboardHelper.this.showPopup(e);
	}

	public void mouseClicked( MouseEvent e ){
		ClipboardHelper.this.showPopup(e);
	}

	public void mouseReleased( MouseEvent e ){
		ClipboardHelper.this.showPopup(e);
	}

	protected void showPopup( MouseEvent e ){
		try{
			if( e.isPopupTrigger() ){
				Point p = e.getPoint();
				SwingUtilities.convertPointToScreen( p,e.getComponent() );
				JPopupMenu menu = getPopup();
				menu.setLocation( p );
				menu.setInvoker( /*MPEPanel.this*/ myInvoker );
				menu.setVisible( true );
			}
		}catch( Exception exception ){
			System.err.println( "Warning: MPEPanel.ClipboardHelper.showPopup() failed, caught " + exception );
			return;
		}
	}

	private JPopupMenu getPopup(){
		if( myHelperPopupMenu == null ){
			myHelperPopupMenu = new JPopupMenu();

			myItemCopy = new JMenuItem( "copy" );
			myItemCopy.addActionListener( (ActionListener)ClipboardHelper.this );
			myHelperPopupMenu.add( myItemCopy );
		}
		String txt = "copy \""+getText()+"\"";
		myItemCopy.setText( txt );
		//myItemCopy.setToolTipText( txt );
		return myHelperPopupMenu;
	}

	public void actionPerformed( ActionEvent event ){
		ClipboardHelper.this.doCopy();
	}

	public void doCopy(){
		edu.ucla.belief.ui.util.Util.copyToSystemClipboard( getText() );
	}

	public String getText(){
		/*if( (myLabel1 == null) && (myLabel2 == null) ) return "";

		String raw = null;
		if( myLabel2 == null ) raw = myLabel1.getText();
		else if( myLabel1 == null ) raw = myLabel2.getText();
		else{
			if( myBuffer == null ) myBuffer = new StringBuffer( 64 );
			else myBuffer.setLength(0);
			if( myLabel1 != null ) myBuffer.append( myLabel1.getText() );
			if( myLabel2 != null ) myBuffer.append( myLabel2.getText() );
			raw = myBuffer.toString();
		}

		return edu.ucla.belief.ui.util.Util.htmlUnencode( raw );*/

		/*if( myComponents == null ){
			myComponents = new ArrayList( 3 );
			if( myLabel1 != null ) myComponents.add( myLabel1 );
			if( myLabel2 != null ) myComponents.add( myLabel2 );
			if( myJTextComponent != null ) myComponents.add( myJTextComponent );
		}

		if( myBuffer == null ) myBuffer = new StringBuffer( 64 );
		else myBuffer.setLength(0);

		for( Iterator it = myComponents.iterator(); it.hasNext(); ){
		}*/

		if( myBuffer == null ) myBuffer = new StringBuffer( 128 );
		else myBuffer.setLength(0);

		for( int i=0; i<myAdapters.length; i++ ){
			if( myAdapters[i] != null ) myBuffer.append( myAdapters[i].getText() );
		}

		return edu.ucla.belief.ui.util.Util.htmlUnencode( myBuffer.toString() );
	}

	/** @author Keith Cascio
		@since 20060110 */
	public static class TextAdapter{
		public TextAdapter( JLabel label ){
			TextAdapter.this.myJLabel = label;
		}

		public TextAdapter( JTextComponent comp ){
			TextAdapter.this.myJTextComponent = comp;
		}

		public String getText(){
			if( myJLabel != null ) return myJLabel.getText();
			else if( myJTextComponent != null ) return myJTextComponent.getText();
			else return null;
		}

		private JLabel myJLabel;
		private JTextComponent myJTextComponent;
	}

	private StringBuffer myBuffer;
	//private JLabel myLabel1, myLabel2;
	private JComponent myInvoker;
	private JPopupMenu myHelperPopupMenu;
	private JMenuItem myItemCopy;
	//private JTextComponent myJTextComponent;
	private TextAdapter[] myAdapters;
}
