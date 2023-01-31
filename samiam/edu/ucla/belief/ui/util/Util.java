package edu.ucla.belief.ui.util;

import edu.ucla.belief.ui.statusbar.StatusBar;
import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.NetworkInternalFrame;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;

import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.imageio.*;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.*;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

/** @author keith cascio
	@since  20030214 */
public class Util
{
	public static boolean DEBUG = false;
	public static boolean DEBUG_VERBOSE = false;
	public static final String STR_VERBOSE_TRACE_MESSAGE = "VERBOSE *****Caught Exception***** VERBOSE:";

	public static final java.io.PrintStream STREAM_TEST = System.out;
	public static final java.io.PrintStream STREAM_DEBUG = System.out;
	public static final java.io.PrintStream STREAM_VERBOSE = System.out;

	public static final int INT_HEX_DIGITS_HTML_COLOR = (int)2;

	private static Clipboard      CLIPBOARD;
	private static ClipboardOwner CLIPBOARDOWNER;
	private static Encoder        ENCODER;

	public  static final String  STR_REGEX_SET = "^(\\w[\\w.$]+\\w)[.](\\w+)=(.*)$";
	private static       Matcher   MATCHER_SET;
	public  static final Class[] PRIMITIVES    = new Class[]{ Boolean.TYPE,  Character.TYPE,  Byte.TYPE,  Short.TYPE,  Integer.TYPE,  Long.TYPE,  Float.TYPE,  Double.TYPE,  Void.TYPE  };
	public  static final Class[] WRAPPERS      = new Class[]{ Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Void.class };

	/** @since 20080221 */
	public static Throwable warn( Throwable thrown, String method ){
		System.err.println( "warning: " + method + " caught " + thrown );
		if( DEBUG_VERBOSE ){
			System.err.println( STR_VERBOSE_TRACE_MESSAGE );
			thrown.printStackTrace( System.err );
		}
		return thrown;
	}

	/** @since 20080107 */
	public static Class wrapperForPrimitive( Class primitive ){
		for( int i=0; i<PRIMITIVES.length; i++ ){
			if( PRIMITIVES[i] == primitive ){ return WRAPPERS[i]; }
		}
		return null;
	}

	/** Set public static fields based on a string. Useful for command line processing.
		eg Util.set( "edu.ucla.belief.ui.tree.EvidenceTreeCellRenderer.INT_WIDTH_BUFFER=0" )
		@since 20080107 */
	public static Object set( String strSet ) throws Exception{
		if(   MATCHER_SET == null ){ MATCHER_SET = Pattern.compile( STR_REGEX_SET ).matcher( strSet ); }
		else{ MATCHER_SET.reset( strSet ); }
		if( ! MATCHER_SET.matches() ){
			System.err.println( "failed to set static field using \"" + strSet + "\"" );
			return null;
		}
		String       strClass = MATCHER_SET.group(1);
		String       strField = MATCHER_SET.group(2);
		String       strValue = MATCHER_SET.group(3);
		Class        clazz    = Class.forName(  strClass );
		Field        field    = clazz.getField( strField );
		Class       fclazz    = field.getType();
		if( fclazz.isPrimitive() ){ fclazz = wrapperForPrimitive( fclazz ); }
		Constructor  uctor    = fclazz.getConstructor( new  Class[]{ String.class } );
		Object       value    =  uctor.newInstance(    new Object[]{ strValue     } );
		field.set( null, value );
		return           value;
	}

	/** @since 20071216 */
	public static final String id( Object obj ){
		if( obj == null ) return "null";
		String cn = obj.getClass().getName();
		int index = Math.max( cn.lastIndexOf( '.' ), cn.lastIndexOf( '$' ) );
		return cn.substring( index + 1 ) + "@" + Integer.toString( System.identityHashCode( obj ), 0x10 );
	}

	/** @since 20070423 */
	public static String supported( String[] symbols ){
		try{
			if(      symbols.length < 1 ) return null;
			else if( symbols.length < 2 ) return symbols[0];

			if( BUTTON_FONT == null ) BUTTON_FONT = (new JButton()).getFont();

			boolean canDisplay = true;
			String  symbol;
			for( int index=0; index<symbols.length; index++ ){
				symbol     = symbols[index];
				canDisplay = true;
				for( int i=0; i<symbol.length(); i++ ){
					canDisplay &= BUTTON_FONT.canDisplay( symbol.charAt(i) );
				}
				if( canDisplay ) return symbol;
			}
		}catch( Exception exception ){
			System.err.println( "warning: Grepable.Flag.supported() caught " + exception );
		}
		return symbols[ symbols.length - 1 ];
	}
	private static java.awt.Font BUTTON_FONT;

	/** @since 062105 */
	public static String getSimpleName( Class clazz ){
		String name = clazz.getName();
		return name.substring( name.lastIndexOf( '.' ) + 1 );
	}

	public static boolean saveImage( Component comp, String formatname ){
		JFileChooser chooser = new JFileChooser();
		int result = chooser.showSaveDialog( comp );
		if( result == JFileChooser.APPROVE_OPTION ){
			java.io.File ofile = chooser.getSelectedFile();
			if( ofile.exists() ){
				result = JOptionPane.showConfirmDialog( comp, "Overwrite "+ofile.getPath()+"?", "Overwrite?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if( result != JOptionPane.OK_OPTION ) return false;
			}
			return saveImageToFile( comp, formatname, ofile );
		}
		return false;
	}

	public static boolean saveImageToFile( Component comp, String formatname, java.io.File ofile ){

		if( !comp.isVisible() ) return false;
		Image image = comp.createImage( comp.getWidth(), comp.getHeight() );
		try{
		//	ScreenImage.createImage( comp, ofile.getPath() );
			ImageIO.write( (RenderedImage)image, formatname, ofile );
			return true;
		}catch( Exception e ){
			System.err.println( e );
			return false;
		}
	}

	/** @since 021105 */
	public static boolean screenshot( Component comp, String formatname, java.io.File ofile ){
		try{
			Robot robot = new Robot();
			Rectangle screenRect = SwingUtilities.getLocalBounds( comp );
			Point location = comp.getLocation();
			Container parent = comp.getParent();
			if( parent != null ) SwingUtilities.convertPointToScreen( location, parent );
			screenRect.setLocation( location );
			BufferedImage image = robot.createScreenCapture( screenRect );
			ImageIO.write( image, formatname, ofile );
		}catch( Exception e ){
			System.err.println( "Warning: " + e );
			return false;
		}
		return true;
	}

	/** @since 20041102 election day */
	public static String copyToSystemClipboard( String strSelection )
	{
		getSystemClipboard().setContents( new StringSelection( strSelection ), getClipboardOwner() );
		return strSelection;
	}

	/** @since 20070309 nasa */
	public static String pasteFromSystemClipboard(){
		try{
			return (String) getSystemClipboard().getContents( null ).getTransferData( DataFlavor.stringFlavor );
		}catch( Exception exception ){
			System.err.println( "warning: Util.pasteFromSystemClipboard() caught " + exception );
		}
		return null;
	}

	/** @since 20070310 */
	private static Clipboard getSystemClipboard(){
		if( CLIPBOARD == null ) CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();
		return CLIPBOARD;
	}

	/** @since 20070309 nasa */
	private static ClipboardOwner getClipboardOwner(){
		if( CLIPBOARDOWNER == null ) CLIPBOARDOWNER = new ClipboardOwner(){
			public void lostOwnership( Clipboard clipboard, Transferable contents ){}
		};
		return CLIPBOARDOWNER;
	}

	/** A solution to JOptionPane's unresizeable dialogs.
		@since 102904 */
	public static void setParentJDialogResizable( JComponent message, boolean flag, long timeout ){
		new Thread( new JOptionResizeHelper( message, flag, timeout ), "JOptionResizeHelper thread" ).start();
	}

	/** @since 101904 */
	static public void printCaller(){
		printStackTrace( 1, System.out );
	}

	/** @since 101904 */
	static public void printStackTrace( int numDeep, java.io.PrintStream stream ){
		printStackTracePrivate( new Throwable(), numDeep, stream );
	}

	/** @since 011505 */
	static public void printStackTrace( Throwable throwable, int numDeep, java.io.PrintStream stream ){
		printStackTracePrivate( throwable, numDeep, stream );
	}

	/** @since 011505 */
	static private void printStackTracePrivate( Throwable throwable, int numDeep, java.io.PrintStream stream ){
		StackTraceElement[] array = throwable.getStackTrace();
		int indexLimit = 2;
		int index0 = Math.min( indexLimit + numDeep, array.length - 1 );
		for( int i=index0; i>indexLimit; i-- ){
			stream.println( array[i] );
		}
	}

	/** @since 011505 */
	static public void printMethodStackTraceElement( Throwable throwable, String methodname, java.io.PrintStream stream, boolean abbreviate ){
		if( (methodname == null) || (methodname.length() < 1) ) return;
		StackTraceElement[] array = throwable.getStackTrace();
		for( int i=0; i<array.length; i++ ){
			if( array[i].getMethodName().equals( methodname ) ){
				String toprint = null;
				if( abbreviate ) toprint = array[i].getFileName() + ":" + array[i].getLineNumber();
				else toprint = array[i].toString();
				stream.println( toprint );
			}
		}
	}

	/** @since 080504 */
	static public boolean epsilonEquals( float a, float b, float epsilon ){
		return Math.abs( a - b ) < epsilon;
	}

	/** @since 080504 */
	static public boolean epsilonEquals( double a, double b, double epsilon ){
		return Math.abs( a - b ) < epsilon;
	}

	/** @since 080404 */
	public static String htmlEncode( Color color )
	{
		return toHexStringFormatted( color.getRed(), INT_HEX_DIGITS_HTML_COLOR ) + toHexStringFormatted( color.getGreen(), INT_HEX_DIGITS_HTML_COLOR ) + toHexStringFormatted( color.getBlue(), INT_HEX_DIGITS_HTML_COLOR );
	}

	/** @since 20050225 */
	public static String htmlEncode( String input ){
		if( ENCODER == null ) ENCODER = new Encoder();
		return ENCODER.htmlEncode( input );
	}
	/** @since 20050923 */
	public static String htmlUnencode( String input ){
		if( ENCODER == null ) ENCODER = new Encoder();
		return ENCODER.htmlUnencode( input );
	}

	/** @since 080404 */
	public static String toHexStringFormatted( int intArg, int minimumIntegerDigits )
	{
		String ret = Integer.toHexString( intArg );
		int len = ret.length();
		if( len < minimumIntegerDigits ){
			ret = SamiamUserMode.STR_32_ZEROS.substring(0,minimumIntegerDigits-len) + ret;
		}
		return ret;
	}

	/** @since 021804 */
	public static Map invertMap( Map input )
	{
		Map ret = new HashMap( input.size() );

		Object inputKey;
		Object inputValue;
		for( Iterator it = input.keySet().iterator(); it.hasNext(); ){
			inputKey = it.next();
			inputValue = input.get( inputKey );
			ret.put( inputValue, inputKey );
		}

		return ret;
	}

	/** @since 420! '04 */
	public static String getFileNameSystemInvocationScript(){
		return ( BrowserControl.isWindowsPlatform() ) ? "samiam.bat" : "runsamiam";
	}

	/** @since 012804 */
	public static String makeOutOfMemoryMessage(){
		return makeOutOfMemoryMessage( (String)null );
	}

	/** @since 050404 */
	public static String makeOutOfMemoryMessage( String str_invoker )
	{
		String ret = null;
		if( str_invoker == null ) ret = "Make sure that the maximum memory argument (-Xmx)\nin the invocation script file (" +getFileNameSystemInvocationScript()+ ") corresponds to your RAM capacity.";
		else ret = "Make sure that the program you used to invoke "+UI.STR_SAMIAM_ACRONYM+" ("+str_invoker+") allows\nthe Java virtual machine to use all of your system RAM.\n(See java maximum memory argument \"-Xmx\").";
		return ret;
	}

	/**
		@author Keith Cascio
		@since 120403
	*/
	public static void pushStatusWest( NetworkInternalFrame nif, String text )
	{
		StatusBar bar = getStatusBar( nif );
		if( bar == null ) return;
		bar.pushText( text, StatusBar.WEST );
	}

	/**
		@author Keith Cascio
		@since 120403
	*/
	public static void popStatusWest( NetworkInternalFrame nif, String text )
	{
		StatusBar bar = getStatusBar( nif );
		if( bar == null ) return;
		bar.popText( text, StatusBar.WEST );
	}

	/**
		@author Keith Cascio
		@since 120403
	*/
	public static StatusBar getStatusBar( NetworkInternalFrame nif )
	{
		if( nif == null ) return null;
		UI ui = nif.getParentFrame();
		if( ui == null ) return null;
		return ui.getStatusBar();
	}

	/** @author keith cascio
		@since 20020422

		Center a Window on the screen. */
	public static Point centerWindow( Window window, Rectangle wrt )
	{
		Dimension windowSize    = window.getSize();
		Point     upperleft     = wrt.getLocation();

		int       xCoordinate   = upperleft.x + ((wrt.width  > windowSize.width  ) ? ((wrt.width  - windowSize.width  )/2) : 0),
		          yCoordinate   = upperleft.y + ((wrt.height > windowSize.height ) ? ((wrt.height - windowSize.height )/2) : 0);

		Point     newCoordinate = new Point( xCoordinate, yCoordinate );

		//System.out.println( "WRT bounds: " + wrt + "\nWindow size: " + windowSize + "\nNew Coordinate: " + newCoordinate );//debug

		window.setLocation( newCoordinate );

		return newCoordinate;
	}

	/** @since 20030214 */
	public static void centerWindow( Window w ){
		centerWindow( w, getScreenBounds() );
	}

	/** @since 20020422
		A utility method.
		Returns the bounds of the GraphicsConfiguration in the device coordinates. */
	public static Rectangle getScreenBounds()
	{
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
	}

	/** @since 20030214 */
	public static Rectangle convertBoundsToScreen( Component comp )
	{
		Rectangle bounds = comp.getBounds();

		Point upperLeft = bounds.getLocation();

		Component parent = comp.getParent();
		if( parent == null ) parent = comp;

		SwingUtilities.convertPointToScreen( upperLeft, parent );
		bounds.setLocation( upperLeft );

		return bounds;
	}

	/** @since 20030326 */
	public static void printStats( JComponent comp, String displayID, java.io.PrintStream stream ){
		if( comp != null && stream != null ){
			stream.println( displayID+".isVisible() == "+ comp.isVisible() );
			stream.println( displayID+".isDisplayable() == "+ comp.isDisplayable() );
			stream.println( displayID+".isEnabled() == "+ comp.isEnabled() );
			stream.println( displayID+".isShowing() == "+ comp.isShowing() );
			stream.println( displayID+".isValid() == "+ comp.isValid() );
		}
	}

	/** @since 20030422 */
	static public void showErrorMessage( String message, String title ){
		JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
	}

	/** @since 011205 */
	static public void setLookAndFeel(){
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}catch( Exception e ){
			System.err.println( "Error: Failed to set look and feel." );
		}
	}

	/** @since 011205 */
	static public JFrame getDebugFrame( String title, JComponent contents ){
		JFrame frame = new JFrame( title );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		frame.getContentPane().add( contents );
		frame.setSize( new Dimension( 600,500 ) );
		Util.centerWindow( frame );
		//frame.setVisible( true );

		return frame;
	}

	/** @since 022505 */
	static public JDialog makeDialog( Component parentComponent, Component message, String title, boolean modal ){
		JDialog dialog = null;
		if( parentComponent == null ) dialog = new JDialog();
		else{
			Window window = SwingUtilities.getWindowAncestor( parentComponent );
			if( window instanceof Dialog ) dialog = new JDialog( (Dialog)window );
			else if( window instanceof Frame ) dialog = new JDialog( (Frame)window );
		}
		dialog.setTitle( title );
		dialog.setModal( modal );
		dialog.setResizable( true );
		dialog.getContentPane().add( message );
		dialog.pack();
		if( parentComponent != null ){
			Rectangle wrt = parentComponent.getBounds();
			Point loc = wrt.getLocation();
			SwingUtilities.convertPointToScreen( loc, parentComponent.getParent() );
			wrt.setLocation( loc );
			centerWindow( dialog, wrt );
		}
		//dialog.setVisible( true );
		return dialog;
	}
}
