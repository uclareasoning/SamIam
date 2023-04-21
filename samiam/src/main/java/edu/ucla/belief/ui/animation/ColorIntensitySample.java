package edu.ucla.belief.ui.animation;

import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.util.DecimalField;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.event.*;

/**
	@author Keith Cascio
	@since 081004
*/
public class ColorIntensitySample extends JPanel implements ActionListener
{
	public ColorIntensitySample( SamiamPreferences prefs ){
		super( new GridBagLayout() );
		this.mySamiamPreferences = prefs;
		this.myAnimationColorHandler = AnimationPreferenceHandler.ANIMATIONCOLORHANDLER_DEFAULT;
		this.myVirtualColor = Color.green;
		this.myFlagReflect = AnimationPreferenceHandler.FLAG_REFLECT_DEFAULT;
		init();
	}

	public static final Color COLOR_TEXT = Color.darkGray;
	public static final Color COLOR_BORDER = Color.black;

	public void init(){
		GridBagConstraints c = new GridBagConstraints();
		JComponent component;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		this.add( component = new JLabel( "{ low", JLabel.LEFT ) );
		component.setForeground( COLOR_TEXT );

		this.add( Box.createVerticalStrut( 64 ), c );

		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = GridBagConstraints.REMAINDER;
		this.add( component = new JLabel( "high }", JLabel.RIGHT ) );
		component.setForeground( COLOR_TEXT );

		Border border = BorderFactory.createLineBorder( COLOR_BORDER, (int)1 );
		Border empty = BorderFactory.createEmptyBorder( 4, 2, 4, 2 );//(int top, int left, int bottom, int right)
		TitledBorder titledborder = new TitledBorder( empty, "Color Intensity Gradient: Entropy" );
		titledborder.setTitleColor( COLOR_TEXT );
		Border compound = BorderFactory.createCompoundBorder( border, titledborder );
		setBorder( compound );
	}

	public void setPreferences()
	{
		PreferenceGroup animationPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.AnimationDspNme );
		Preference reflect = mySamiamPreferences.getMappedPreference( SamiamPreferences.animationReflect );
		ObjectPreference ach = (ObjectPreference) mySamiamPreferences.getMappedPreference( SamiamPreferences.animationColorComponent );

		PreferenceGroup netnPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.NetDspNme );
		Preference color = mySamiamPreferences.getMappedPreference( SamiamPreferences.nodeBkgndClr );

		setAnimationColorHandler( (AnimationPreferenceHandler.AnimationColorHandler) ach.getValue() );
		setVirtualColor( (Color) color.getValue() );
		setReflect( ((Boolean) reflect.getValue()).booleanValue() );
	}

	public void updatePreferences()
	{
		PreferenceGroup animationPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.AnimationDspNme );
		Preference reflect = mySamiamPreferences.getMappedPreference( SamiamPreferences.animationReflect );
		ObjectPreference ach = (ObjectPreference) mySamiamPreferences.getMappedPreference( SamiamPreferences.animationColorComponent );

		PreferenceGroup netnPrefs = mySamiamPreferences.getPreferenceGroup( SamiamPreferences.NetDspNme );
		Preference color = mySamiamPreferences.getMappedPreference( SamiamPreferences.nodeBkgndClr );

		if( ach.isComponentEdited() ){
			setAnimationColorHandler( (AnimationPreferenceHandler.AnimationColorHandler) ach.getCurrentEditedValue() );
		}

		if( color.isComponentEdited() ){
			setVirtualColor( (Color) color.getCurrentEditedValue() );
		}

		if( reflect.isComponentEdited() ){
			setReflect( ((Boolean) reflect.getCurrentEditedValue()).booleanValue() );
		}
	}

	public void actionPerformed( ActionEvent e ){
		updatePreferences();
	}

	public void setAnimationColorHandler( AnimationPreferenceHandler.AnimationColorHandler handler ){
		this.myAnimationColorHandler = handler;
		repaint();
	}

	public void setVirtualColor( Color c ){
		this.myVirtualColor = c;
		repaint();
	}

	public void setReflect( boolean flag ){
		this.myFlagReflect = flag;
		repaint();
	}

	public void paintComponent( Graphics g )
	{
		Dimension size = getSize( new Dimension() );
		double width = size.getWidth();

		float stepIntensity = FLOAT_ONE/(float)width;
		if( myFlagReflect ) stepIntensity = -stepIntensity;

		Color color = myVirtualColor;
		float intensity = myFlagReflect ? FLOAT_ONE : FLOAT_ZERO;
		float[] virtualVals = Color.RGBtoHSB( myVirtualColor.getRed(), myVirtualColor.getGreen(), myVirtualColor.getBlue(), new float[3] );
		float[] newValues = new float[3];

		for( int i=0; i<size.width; i++ ){
			myAnimationColorHandler.updateValues( virtualVals, intensity, newValues );
			color = Color.getHSBColor( newValues[0], newValues[1], newValues[2] );
			g.setColor( color );
			g.drawLine( i, 0, i, size.height );
			intensity += stepIntensity;
		}
	}

	public static final float FLOAT_ZERO = (float)0;
	public static final float FLOAT_ONE = (float)1;

	private SamiamPreferences mySamiamPreferences;
	private AnimationPreferenceHandler.AnimationColorHandler myAnimationColorHandler;
	private Color myVirtualColor;
	private boolean myFlagReflect;

	/** test/debug */
	public static void main( String[] args )
	{
		SamiamPreferences prefs = new SamiamPreferences( true );
		final ColorIntensitySample cis = new ColorIntensitySample( prefs );
		cis.setPreferences();

		JFrame frame = new JFrame( "ColorIntensitySample TEST" );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		JPanel panelButtons = new JPanel();

		final JComboBox comboHandlers = new JComboBox( AnimationPreferenceHandler.ARRAY_ANIMATIONCOLORHANDLERS );
		comboHandlers.setSelectedItem( cis.myAnimationColorHandler );
		final DecimalField fieldScale = new DecimalField( (double)AnimationPreferenceHandler.FLOAT_HUE_SCALE_DEFAULT, 5, -2, 2 );
		final DecimalField fieldOffset = new DecimalField( (double)AnimationPreferenceHandler.FLOAT_HUE_OFFSET_DEFAULT, 5, -5, 5 );
		final JCheckBox cbReflect = new JCheckBox( "reflect" );
		cbReflect.setSelected( cis.myFlagReflect );
		final JButton buttonUpdate = new JButton( "update" );
		panelButtons.add( comboHandlers );
		panelButtons.add( new JLabel( "scale:" ) );
		panelButtons.add( fieldScale );
		panelButtons.add( new JLabel( "offset:" ) );
		panelButtons.add( fieldOffset );
		panelButtons.add( cbReflect );
		panelButtons.add( buttonUpdate );

		buttonUpdate.addActionListener( new ActionListener(){
			public void actionPerformed( ActionEvent e ){
				AnimationPreferenceHandler.setHueAdjustment( (float) fieldScale.getValue(), (float) fieldOffset.getValue() );
				cis.setReflect( cbReflect.isSelected() );
				cis.setAnimationColorHandler( (AnimationPreferenceHandler.AnimationColorHandler)comboHandlers.getSelectedItem() );
			}
		} );

		JPanel panel = new JPanel( new BorderLayout() );
		panel.add( cis, BorderLayout.CENTER );
		panel.add( panelButtons, BorderLayout.SOUTH );
		panel.add( Box.createVerticalStrut(16), BorderLayout.NORTH );
		panel.add( Box.createHorizontalStrut(16), BorderLayout.EAST );
		panel.add( Box.createHorizontalStrut(16), BorderLayout.WEST );

		frame.getContentPane().add( panel );
		frame.setSize( new Dimension( 600,300 ) );
		Util.centerWindow( frame );
		frame.setVisible( true );
	}
}
