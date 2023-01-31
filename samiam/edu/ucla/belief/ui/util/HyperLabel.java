package edu.ucla.belief.ui.util;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.actionsandmodes.SamiamAction;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
	@author Keith Cascio
	@since 071304
*/
public class HyperLabel extends javax.swing.JLabel implements MouseListener
{
	public HyperLabel( String text, SamiamAction onclick ){
		this( text, true );
		mySamiamAction = onclick;
		myActionSource = this;
	}

	public HyperLabel( String text, SamiamAction onclick, Object source ){
		this( text, onclick );
		myActionSource = source;
	}

	public HyperLabel( String text, Runnable onclick ){
		this( text, onclick, true );
	}

	public HyperLabel( String text, Runnable onclick, boolean underline ){
		this( text, underline );
		myRunnable = onclick;
	}

	private HyperLabel( String text, boolean underline ){
		//super( "<html><nobr><a href=\"\">" + text + "</a></html>" );
		super( underline ? underline( text ) : text );
		myFlagUnderline = underline;
		this.setForeground( Color.blue );
		//setFont( new Font( "Arial", Font.PLAIN, (int)10 ) );
		//Font font = this.getFont();
		//AttributedCharacterIterator.Attribute[] attrs = font.getAvailableAttributes();
		//for( int i=0; i<attrs.length; i++ ) System.out.println( attrs[i] );
		//Map attributes = font.getAttributes();
		//System.out.println( "before:\n" + attributes );
		//attributes.put( TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON );
		//this.setFont( new Font( attributes ) );
		//attrs = this.getFont().getAvailableAttributes();
		//for( int i=0; i<attrs.length; i++ ) System.out.println( attrs[i] );
		this.addMouseListener( this );
	}

	public void setEnabled( boolean flag ){
		if( myFlagFade ) super.setEnabled( flag );
		myFlagNotEnabled = !flag;
	}

	/** @since 20071204 */
	public HyperLabel setFade( boolean fade ){
		myFlagFade = fade;
		return HyperLabel.this;
	}

	public static final char CHAR_UNDERLINE = '\u0332';

	/** @since 20060525 */
	public void setText( String text ){
		super.setText( myFlagUnderline ? underline( text ) : text );
	}

	public static String underline( String toUnderline ){
		return "<html><nobr><u>" + toUnderline + "</u>";
	}

	/*public static String underline( String toUnderline )
	{
		int len = toUnderline.length();
		StringBuffer buffer = new StringBuffer( len*2 );
		for( int i=0; i<len; i++ ){
			buffer.append( toUnderline.charAt(i) );
			buffer.append( CHAR_UNDERLINE );
		}
		return buffer.toString();
	}*/

	public void mouseClicked(MouseEvent e){
		if( myFlagNotEnabled ) return;
		if( mySamiamAction != null ) mySamiamAction.actionP( myActionSource );
		else if( myRunnable != null ) myRunnable.run();
	}

	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void mouseEntered(MouseEvent e){
		if( myFlagNotEnabled ) return;
		mySavedCursor = getCursor();
		setCursor( UI.CURSOR_HAND );
	}

	public void mouseExited(MouseEvent e){
		if( myFlagNotEnabled ) return;
		setCursor( mySavedCursor );//UI.CURSOR_DEFAULT );
	}

	private Runnable myRunnable;
	private SamiamAction mySamiamAction;
	private Object myActionSource;
	private Cursor mySavedCursor;
	private boolean myFlagNotEnabled = false;
	private boolean myFlagUnderline = true, myFlagFade = true;
}
