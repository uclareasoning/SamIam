package edu.ucla.belief.ui.statusbar;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.clipboard.ClipboardHelper;
import edu.ucla.belief.ui.preference.*;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;

/** @author Keith Cascio
	@since 20021028 */
public class StatusBar extends JPanel implements SwingConstants
{
	private static int	INT_NORTH_PAD = (int)3;
	private static int	INT_SOUTH_PAD = (int)1;
	private static int	INT_SEPARATOR_PAD = (int)16;
	private static int	INT_MIN_LABEL_WIDTH = (int)16;
	private static double	DOUBLE_WEIGHT_WEST = (double)6;
	private static double	DOUBLE_WEIGHT_EAST = (double)4;

	public StatusBar(){
		this( DOUBLE_WEIGHT_WEST, DOUBLE_WEIGHT_EAST );
	}

	/** @since 20051005 */
	public StatusBar( double weightWest, double weightEast ){
		this.myWeightWest = weightWest;
		this.myWeightEast = weightEast;
		init();
	}

	/** @since 021903 */
	public void setFontSize( float newSize )
	{
		if( newSize != myFloatFontSize )
		{
			myFloatFontSize = newSize;

			Font oldFont = myLabelWest.getFont();
			Font newFont = oldFont.deriveFont( myFloatFontSize );
			myLabelWest.setFont( newFont );
			myLabelEast.setFont( newFont );
			myLabelWest.setPreferredSize( null );
			myLabelEast.setPreferredSize( null );

			String oldTextEast = myLabelEast.getText();
			String oldTextWest = myLabelWest.getText();

			myLabelEast.setText( STR_TEST_EAST );
			myLabelWest.setText( STR_TEST_WEST );

			Dimension prefLabel = myLabelWest.getPreferredSize();
			Dimension minLabel = new Dimension( INT_MIN_LABEL_WIDTH, prefLabel.height );
			Dimension prefPanel = new Dimension( 1, prefLabel.height + INT_NORTH_PAD + INT_SOUTH_PAD );

			myLabelWest.setPreferredSize( prefLabel );
			myLabelWest.setMinimumSize( minLabel );
			myLabelEast.setPreferredSize( prefLabel );
			myLabelEast.setMinimumSize( minLabel );
			this.setPreferredSize( prefPanel );

			myLabelEast.setText( oldTextEast );
			myLabelWest.setText( oldTextWest );
		}
	}

	/** @since 20030219 */
	public void setPreferences( SamiamPreferences globalPrefs ){
		updatePreferences( globalPrefs, true );
	}

	/** @since 20030219 */
	public void updatePreferences( SamiamPreferences globalPrefs ){
		updatePreferences( globalPrefs, false );
	}

	/** @since 20060329 */
	public void updatePreferences( SamiamPreferences globalPrefs, boolean force ){
		Preference pointSize = globalPrefs.getMappedPreference( SamiamPreferences.statusBarPointSize );
		if( force || pointSize.isRecentlyCommittedValue() ){
			setFontSize( ((Integer)pointSize.getValue()).floatValue() );
		}

		Preference animateStatusBarWest = globalPrefs.getMappedPreference( SamiamPreferences.animateStatusBarWest );
		if( force || animateStatusBarWest.isRecentlyCommittedValue() ){
			myStackedMessageWest.setAnimated( ((Boolean)animateStatusBarWest.getValue()).booleanValue() );
		}

		Preference animateStatusBarEast = globalPrefs.getMappedPreference( SamiamPreferences.animateStatusBarEast );
		if( force || animateStatusBarEast.isRecentlyCommittedValue() ){
			myStackedMessageEast.setAnimated( ((Boolean)animateStatusBarEast.getValue()).booleanValue() );
		}
	}

	public void setText( String newText, int handle )
	{
		if( handle == WEST ) myStackedMessageWest.setText( newText );
		else if( handle == EAST ) myStackedMessageEast.setText( newText );
		else throw new IllegalArgumentException( "Illegal handle " + handle );
	}

	public void pushText( String newText, int handle )
	{
		//System.out.println( "StatusBar.pushText( "+newText+" )" );
		if( handle == WEST ) myStackedMessageWest.pushText( newText );
		else if( handle == EAST ) myStackedMessageEast.pushText( newText );
		else throw new IllegalArgumentException( "Illegal handle " + handle );
	}

	public void popText( String text, int handle )
	{
		//System.out.println( "StatusBar.popText( "+text+" )" );
		if( handle == WEST ) myStackedMessageWest.popText( text );
		else if( handle == EAST ) myStackedMessageEast.popText( text );
		else throw new IllegalArgumentException( "Illegal handle " + handle );
	}

	/*
	private void pushText( LinkedList stack, String newText, JLabel label )
	{
		synchronized( mySynchronization ){
		stack.addFirst( newText );
		label.setText( newText );
		paintImmediately( myImmediatelyRectangle );
		}
	}

	private void popText( LinkedList stack, Collection deathrow, String newText, JLabel label, String strDefault )
	{
		synchronized( mySynchronization ){

		deathrow.add( newText );
		String result = strDefault;
		while( (!(stack.isEmpty())) && deathrow.contains( stack.getFirst() ) )
		{
			deathrow.remove( stack.removeFirst() );
			if( stack.isEmpty() ) result = strDefault;
			else result = (String) stack.getFirst();
		}
		label.setText( result );
		paintImmediately( myImmediatelyRectangle );

		}
	}*/

	protected void init()
	{
		Border trimmed = new TrimmedBorder( (AbstractBorder) BorderFactory.createBevelBorder( BevelBorder.LOWERED ), 1, 1, 1, 1 );

		myLabelWest = new JLabel( STR_TEST_WEST, JLabel.LEFT );
		myLabelWest.setBorder( trimmed );
		//Font oldFont = myLabelWest.getFont();
		//String SAFE_FONT_NAME = "arial";//does not work Windows XP Home, jdk1.5.0_06
		String SAFE_FONT_NAME = "SansSerif";
		Font oldFont = new Font( SAFE_FONT_NAME, Font.PLAIN, (int)myFloatFontSize );
		Font newFont = oldFont.deriveFont( myFloatFontSize );
		myLabelWest.setFont( newFont );
		myStackedMessageWest = new StackedMessage( myLabelWest, WEST );
		//myStackedMessageWest.debug = true;

		Dimension prefLabel = myLabelWest.getPreferredSize();
		Dimension minLabel = new Dimension( INT_MIN_LABEL_WIDTH, prefLabel.height );
		myLabelWest.setPreferredSize( prefLabel );
		myLabelWest.setMinimumSize( minLabel );
		Dimension prefPanel = new Dimension( 1, prefLabel.height + INT_NORTH_PAD + INT_SOUTH_PAD );

		myLabelEast = new JLabel( STR_TEST_EAST, JLabel.LEFT );
		myLabelEast.setBorder( trimmed );
		myLabelEast.setFont( newFont );
		myLabelEast.setPreferredSize( myLabelEast.getPreferredSize() );
		myLabelEast.setMinimumSize( minLabel );
		myStackedMessageEast = new StackedMessage( myLabelEast, EAST );

		try{
			new ClipboardHelper( myLabelWest, (JLabel)null, myLabelWest );
			new ClipboardHelper( myLabelEast, (JLabel)null, myLabelEast );
		}catch( Exception exception ){
			System.err.println( "Warning: StatusBar.init() [ClipboardHelper init] caught " + exception );
		}

		Component strut = null;

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;

		this.setLayout( gridbag );
		this.setPreferredSize( prefPanel );

		strut = Box.createVerticalStrut( INT_NORTH_PAD );
		gridbag.setConstraints( strut, c );
		this.add( strut );

		c.gridwidth = 1;
		c.weightx = myWeightWest;
		gridbag.setConstraints( myLabelWest, c );
		this.add( myLabelWest );

		strut = Box.createHorizontalStrut( INT_SEPARATOR_PAD );
		c.weightx = 0;
		gridbag.setConstraints( strut, c );
		this.add( strut );

		c.weightx = myWeightEast;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints( myLabelEast, c );
		this.add( myLabelEast );

		myLabelWest.setText( "" );
		myLabelEast.setText( "" );

		this.setBorder( new NsidedBorder( (AbstractBorder) BorderFactory.createBevelBorder( BevelBorder.RAISED ), true, false, false, false ) );
	}

	public static void main( String[] args )
	{
		setLookAndFeel();

		StatusBar SB = new StatusBar();

		SB.myLabelWest.setText( STR_TEST_WEST );
		SB.myLabelEast.setText( STR_TEST_EAST );

		JFrame frame = new JFrame( "StatusBarTest" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setSize( 500, 400 );

		Container contentPain = frame.getContentPane();
		contentPain.setLayout( new BorderLayout() );

		contentPain.add( SB, BorderLayout.SOUTH );
		contentPain.add( new JDesktopPane(), BorderLayout.CENTER );

		Util.centerWindow( frame );
		frame.setVisible( true );
	}

	public static final String STR_TEST_WEST = "Status text west.";
	public static final String STR_TEST_EAST = "east";

	/** Set the Swing look and feel. */
	public static void setLookAndFeel()
	{
		try{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch( Exception e ){
			e.printStackTrace();
		}

	}

	private StackedMessage myStackedMessageWest, myStackedMessageEast;
	protected JLabel myLabelWest, myLabelEast;
	protected float	myFloatFontSize = (float)10;
	private double myWeightWest = DOUBLE_WEIGHT_WEST, myWeightEast = DOUBLE_WEIGHT_EAST;
}