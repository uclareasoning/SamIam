package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.toolbar.MainToolBar;

import edu.ucla.belief.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.LinkedList;
//import javax.swing.border.*;
//import java.io.*;
//import java.net.URL;

/** @author keith cascio
	@since  20050204 */
public class CompileSettings implements ActionListener
{
	public static void showDialog( UI ui )
	{
		Dynamator dyn = ui.getDynamator();
		if( dyn == null ){ return; }

		Dynamator.Commitable comp = null;
		try{
			comp = dyn.getEditComponent( null );
		}catch( Exception exception ){
			ui.showErrorDialog( exception.getMessage() );
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				exception.printStackTrace();
			}
			return;
		}

		if( comp == null ){
			ui.showErrorDialog( "No compile settings available for " + dyn.getDisplayName() + "." );
		}
		else{
			final CompileSettings compilesettings = new CompileSettings( comp, dyn, ui );
			JOptionResizeHelper   helper          = new JOptionResizeHelper( comp.asJComponent(), true, (long)10000 );
			helper.setListener( new JOptionResizeHelper.JOptionResizeHelperListener(){
				public void topLevelAncestorDialog( JDialog container, JOptionResizeHelper helper ){
					compilesettings.dialog = container;
				} } );
			helper.start();
			Object result =
			  ui.showOptionDialog(
			    comp.asJComponent(),
			    "Compile Settings - " + dyn.getDisplayName(),
			    JOptionPane.PLAIN_MESSAGE,
			    getArrayOptions( compilesettings ) );
		  /*if( result == STR_SAVE || result == OBJ_QUERYMODE ){
				dyn.commitEditComponent();
				if( result == OBJ_QUERYMODE ) ui.toggleSamiamUserMode();
			}*/
		}
	}

	/** @since 20080228 */
	private CompileSettings( Dynamator.Commitable commitable, Dynamator dyn, UI ui ){
		this.myCommitable = commitable;
		this.dyn          = dyn;
		this.ui           = ui;
	}

	/** @since 20080228 */
	static private Object[] getArrayOptions( ActionListener listener ){
		if( ARRAY_OPTIONS == null ){
			LinkedList list = new LinkedList();
			JButton    button;
			int        cond = JComponent.WHEN_IN_FOCUSED_WINDOW;

			button = new JButton( MainToolBar.getIcon( "Copy16.gif" ) );
			button.setToolTipText( "<html><b>" + toString( STROKE_COPY ) + "</b> - Copy information to system clipboard" );
			list.add( BUTTON_COPY = button );

			button = new JButton( MainToolBar.getIcon( "Recompile16.gif" ) );
			button.setToolTipText( "<html><b>" + toString( STROKE_QUERYMODE ) + "</b> - Enter query mode now" );
			list.add( BUTTON_QUERYMODE = button );

			button = new JButton( MainToolBar.getIcon( "GreenCheck16.gif" ) );// "Properties16.gif" ) );//
			button.setToolTipText( "<html><b>" + toString( STROKE_SAVE ) + "</b> - <font color='#006600'>Confirm</font> these settings" );
			list.add( BUTTON_SAVE = button );

			button = new JButton( MainToolBar.getIcon( "Delete16.gif" ) );
			button.setToolTipText( "<html><b>" + toString( STROKE_CANCEL ) + "</b> - <font color='#cc0000'>Cancel" );
			list.add( BUTTON_CANCEL = button );

			ARRAY_OPTIONS = (JButton[]) list.toArray( new JButton[ list.size() ] );
		}
		for( int i=0; i<ARRAY_OPTIONS.length; i++ ){
			setActionListener( ARRAY_OPTIONS[i], listener, STROKES[i] );
		}
		return ARRAY_OPTIONS;
	}

	/** @since 20080228 */
	static public JButton setActionListener( JButton button, ActionListener listener, KeyStroke stroke ){
		ActionListener[] listeners = button.getActionListeners();
		if( listeners != null ){
			for( int i=0; i<listeners.length; i++ ){
				button     .removeActionListener( listeners[i] );

			}
		}
		button  .unregisterKeyboardAction(           stroke );
		button    .registerKeyboardAction( listener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW );
		button         .addActionListener( listener );
		return button;
	}

	/** @since 20080228 */
	static public String toString( KeyStroke stroke ){
		String text  = KeyEvent.getKeyModifiersText( stroke.getModifiers() );
		if(    text == null ){ text = ""; }
		else{  text += "+"; }
		text        += KeyEvent.getKeyText( stroke.getKeyCode() );
		return text;
	}

  /*private Object[] getArrayOptions(){
		if( myArray == null ){
			myArray = new Object[ ARRAY_COMPILESETTINGS_TEXT.length ];
			System.arraycopy( ARRAY_COMPILESETTINGS_TEXT, 0, myArray, 0, ARRAY_COMPILESETTINGS_TEXT.length );
			JButton buttonCopy = new JButton( "Copy" );
			buttonCopy.addActionListener( (ActionListener)this );
			buttonCopy.setToolTipText( "Copy information to system clipboard" );
			myArray[0] = buttonCopy;
		}
		return myArray;
	}*/

	/** interface ActionListener */
	public void actionPerformed( ActionEvent e ){
		Object source = e.getSource();
		if( source == BUTTON_COPY ){
			if( myCommitable != null ){ myCommitable.copyToSystemClipboard(); }
		}
		else if( source == BUTTON_QUERYMODE ){ dialog.dispose(); dyn.commitEditComponent(); ui.toggleSamiamUserMode(); }
		else if( source == BUTTON_SAVE      ){ dialog.dispose(); dyn.commitEditComponent(); }
		else if( source == BUTTON_CANCEL    ){ dialog.dispose(); }
	}

	private Dynamator.Commitable myCommitable;
	private JDialog              dialog;
	private UI                   ui;
	private Dynamator            dyn;

	private static JButton[] ARRAY_OPTIONS;
	private static JButton
	  BUTTON_COPY,
	  BUTTON_QUERYMODE,
	  BUTTON_SAVE,
	  BUTTON_CANCEL;

	public static final KeyStroke
	  STROKE_COPY      = KeyStroke.getKeyStroke( KeyEvent.VK_C, InputEvent.CTRL_MASK ),
	  STROKE_QUERYMODE = KeyStroke.getKeyStroke( KeyEvent.VK_Q, InputEvent.CTRL_MASK ),
	  STROKE_SAVE      = KeyStroke.getKeyStroke( KeyEvent.VK_S, InputEvent.CTRL_MASK ),
	  STROKE_CANCEL    = KeyStroke.getKeyStroke( KeyEvent.VK_Z, InputEvent.CTRL_MASK );
	public static final KeyStroke[]
	  STROKES = new KeyStroke[]{ STROKE_COPY, STROKE_QUERYMODE, STROKE_SAVE, STROKE_CANCEL };

	public static final String
	  STR_COPY      = "Copy",
	  STR_QUERYMODE = "Query Mode",
	  STR_SAVE      = "Save",
	  STR_CANCEL    = "Cancel";

	public static final Object
	  OBJ_QUERYMODE = MainToolBar.getIcon( "Recompile16.gif" );
	public static final Object[]
	  ARRAY_COMPILESETTINGS_TEXT = new Object[]{ null, OBJ_QUERYMODE, STR_SAVE, STR_CANCEL };
}
