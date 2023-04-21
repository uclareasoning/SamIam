package edu.ucla.belief.ui.dialogs;

import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Insets;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Icon;
import javax.swing.border.Border;
import javax.swing.text.View;
import javax.swing.plaf.basic.BasicHTML;

import java.util.regex.*;

import static javax.swing.SwingUtilities.layoutCompoundLabel;
import static java.lang.Thread.currentThread;

/** The holy grail, an html wrapping JLabel upon which I can specify a maximum width thus forcing it to wrap.

	http://forums.java.net/jive/message.jspa?messageID=235406
	Posted: Sep 14, 2007  7:08 AM pietblok
	Posted: Sep 14, 2007 12:07 PM irond13
	Posted: Sep 14, 2007 11:12 PM pietblok
	Posted: Sep 26, 2007 11:30 PM pietblok
	Posted: Sep 26, 2007 11:40 PM pietblok

	3rd result on page 2 of this Google search:
	Results 11 - 20 of about 50,100 for +java +jlabel +html +width. (0.25 seconds)

	13th result. Thank goodness I looked at page 2 !!! And the number 13 !!!

	.url shortcut file created December 07, 2007, 12:10:15 AM
	.mht          file created December 07, 2007, 12:13:40 AM
	.java         file created December 07, 2007, 12:36:44 AM

java.net Forums : Getting the preferred size of a ...
indexOf("<html") >= 0; } /* * We need a Graphics object to trick the layout to do its work ... private final JLabel previewLabel; /* * The preferred width. ...
forums.java.net/jive/message.jspa?messageID=235406 - 44k - Cached - Similar pages

	relevant bug:  http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4348815

	much better than this solution: http://forums.java.net/jive/message.jspa?messageID=201685
	Posted: Sep 19, 2006 10:28 PM an_ma
	Posted: Feb  4, 2007  1:09 PM twalljava
	Posted: Apr 18, 2007  9:25 AM nluv4hs

	@author "Piet Blok" <pbjar@orange.nl>

	Copyright (C) 2007 Piet Blok, this program is licensed under the terms of the Apache License Version 2.0.

	User Profile for pietblok on 20071207
	Total Posts:     45
	Total Questions:  4
	Location:         Netherlands
	Occupation:       Free lance
	Homepage:         http://www.pbjar.org

	@since 20071207
*/
public class LabelConstrained extends JLabel implements ComponentListener
{
	/** @author keith cascio, @since 20071207 */
    public LabelConstrained setMaximumWidth( int max ){
    	synchronized( synch() ){
			dirty();
			this.preferredWidthLimit = max;
    	}
    	return this;
    }

    /** @author keith cascio, @since 20070125 */
    public Component               setPinch(  Component pincher ){
    	if(           this.pincher == pincher ){ return pincher; }
    	else if(      this.pincher != null    ){   this.pincher.removeComponentListener( this ); }
    	synchronized( synch() ){
			dirty();
		   (this.pincher  = pincher).addComponentListener( this );
			if(  pinched == null   ){         pinched = new Dimension(); }
			pinch();
		}
    	return pincher;
    }

     /** @author keith cascio, @since 20070125 */
    private void pinch(){
    	if( pincher != null ){
    		setMaximumWidth( pincher.getSize( pinched ).width );
    	}
    }

    public void componentHidden(  ComponentEvent e ){ dirty(); }
    public void componentMoved(   ComponentEvent e ){ pinch(); }
    public void componentResized( ComponentEvent e ){ pinch(); }
    public void componentShown(   ComponentEvent e ){ pinch(); }

	/** @author keith cascio, @since 20071207 */
    public LabelConstrained(){
    	this( null );
    }

	public static final Matcher MATCHER_FIRST_WORD = Pattern.compile( "^<.+?>(\\w+)" ).matcher( "" ),
	                            MATCHER_LINE_BREAK = Pattern.compile( "<\\s*br\\s*/?>", Pattern.CASE_INSENSITIVE ).matcher( "" );

	/** @author keith cascio, @since 20071207 */
	public LabelConstrained( String text ){
		super( text );

		previewGraphics   = new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB ).createGraphics();
		this.previewLabel = new JLabel();
    }

    /**
     * If preferredWidthLimit is set to a value > 0, a size is computed with a
     * width that is lees or equal to the preferred width and that is sufficient
     * to contain the renderer's text. If preferredWidthLimit less than 1, the
     * normal preferred size is returned.
     */
    @Override
	public final Dimension getPreferredSize(){
		Dimension preferred = super.getPreferredSize();
		if( (preferredWidthLimit > 0) && (preferredWidthLimit < preferred.width) ){
			synchronized( synch() ){
				if( cached        == null ){
					cached         = computeLimitedSize( preferredWidthLimit );
				}
				preferred = cached;
			}
		}
		return preferred;
	}

	/** @author keith cascio, @since 20071207 */
	public void setText( String text ){
		dirty();
		super.setText( text );
	}

	/** @author keith cascio, @since 20071207 */
	public void setBorder( Border border ){
		dirty();
		super.setBorder( border );
	}

	/** @author keith cascio, @since 20071207 */
	public void setIcon( Icon icon ){
		dirty();
		super.setIcon( icon );
	}

	/** @author keith cascio, @since 20071207 */
	public void setFont( Font font ){
		dirty();
		super.setFont( font );
	}

	/** @author keith cascio, @since 20071207 */
	private LabelConstrained dirty(){ synchronized( synch() ){ cached = null; return this; } }

    private Dimension computeLimitedSize( int widthLimit )
    {
    	JLabel lbl = this.previewLabel;

		lbl.                setBorder( this.getBorder()                 );
		lbl.                  setIcon( this.getIcon()                   );
		lbl.                setLocale( this.getLocale()                 );
		lbl.          setDisabledIcon( this.getDisabledIcon()           );
		lbl.                  setFont( this.getFont()                   );
		lbl.   setHorizontalAlignment( this.getHorizontalAlignment()    );
		lbl.     setVerticalAlignment( this.getVerticalAlignment()      );
		lbl.setHorizontalTextPosition( this.getHorizontalTextPosition() );
		lbl.  setVerticalTextPosition( this.getVerticalTextPosition()   );
		lbl.           setIconTextGap( this.getIconTextGap()            );
		lbl.               setEnabled( this.isEnabled()                 );

		String text = this.getText();
		if( (height_one_line < 1) && (text != null) ){
			String  nobreaks = MATCHER_LINE_BREAK.reset( text ).replaceAll( "" );
			lbl.              setText( nobreaks                         );
			height_one_line  = lbl.getPreferredSize().height;
		}

		lbl.                  setText( this.getText()                   );
		lbl.                  setSize( this.preferredWidthLimit, 0      );
		lbl.                  setSize( this.preferredWidthLimit, lbl.getPreferredSize().height );

		View v  = (View) lbl.getClientProperty( BasicHTML.propertyKey );
		if(  v !=  null ){//javax.swing.plaf.basic.BasicHTML$Renderer
			this.previewGraphics.setFont(    lbl.getFont()     );
			this.previewGraphics.setClip(    0,0,1,1           );
			v.paint( this.previewGraphics,   lbl.getBounds()   );
		}

		Dimension       size = lbl.getPreferredSize();
		size.width           = Math.min( size.width, preferredWidthLimit );
		size.height         += height_one_line;

		return size;
    }

    /** @author keith cascio, @since  20070125 */
    private       Object     synch(){
    	if( synch == null ){ synch = new Object(); }
    	return               synch;
    }

    /** @author keith cascio, @since  20070125 */
    private       Object     synch;

    /** @author keith cascio, @since  20070125 */
    private       Component  pincher;

    /** @author keith cascio, @since  20071207 */
    private       Dimension  cached, pinched;

    /** We need a Graphics object to trick the layout to do its work properly. */
    private final Graphics2D previewGraphics;

    /** This label is used to compute a preferred size. It is put at the PAGE_START position on the previewPanel. */
    private final JLabel     previewLabel;

    /** The preferred width. If less than 1, no preferred width is taken into account. */
    private       int        preferredWidthLimit = -1, height_one_line = 0;
}
