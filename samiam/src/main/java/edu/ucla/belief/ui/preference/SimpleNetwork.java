package edu.ucla.belief.ui.preference;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.networkdisplay.*;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/** @author keith cascio
	@since  20020717 */
public class SimpleNetwork extends JDesktopPane implements ActionListener, SelectionListener
{
	private NodeLabel myNodeLabelSelected;
	private NodeLabel myNodeLabelNormal;
	private Arrow myEdge;
	private SamiamPreferences myNetPrefs;
	private SamiamPreferences myMonitorPrefs;
	//private Preference mynetBkgdClr;
	//private Preference mynetBkgdClrNeedsCompile;
	//private Preference mynodeDefaultSize;
	//private Preference mynetSizeOverRidesPrefs;

	public SimpleNetwork( SamiamPreferences netPrefs )
	{
		myNetPrefs     = netPrefs;
		myMonitorPrefs = netPrefs;
		//mynetBkgdClr = myNetPrefs.getMappedPreference( SamiamPreferences.netBkgdClr );
		//mynetBkgdClrNeedsCompile = myNetPrefs.getMappedPreference( SamiamPreferences.netBkgdClrNeedsCompile );
		setBackground( (Color) myNetPrefs.getMappedPreference( SamiamPreferences.netBkgdClr ).getValue() );
		//mynodeDefaultSize = netPrefs.getMappedPreference( SamiamPreferences.nodeDefaultSize );
		//mynetSizeOverRidesPrefs = netPrefs.getMappedPreference( SamiamPreferences.netSizeOverRidesPrefs );

		//create network
		setLayout( null ); //absolute positioning
		putClientProperty( "JDesktopPane.dragMode", "outline");

		Point tempLocation = new Point(0,0);

		DisplayableFiniteVariable selDVar = new DisplayableFiniteVariableImpl( "Selected" );
		myNodeLabelSelected = new NodeLabel(
				selDVar,
				//new NodeIconOval( selDVar, netPrefs ),
				this,
				CoordinateTransformerNull.getInstance(),
				this,
				netPrefs, true, true );
		selDVar.setNodeLabel( myNodeLabelSelected );
		myNodeLabelSelected.setSelected( true );
		myNodeLabelSelected.displayEvidenceDialog();
		myNodeLabelSelected.setActualLocation( tempLocation );
		myNodeLabelSelected.moveEvidenceDialog( new Point( 150, 5 ) );

		DisplayableFiniteVariable normDVar = new DisplayableFiniteVariableImpl( "Unselected" );
		myNodeLabelNormal = new NodeLabel(
				normDVar,
				//new NodeIconOval( normDVar, netPrefs ),
				this,
				CoordinateTransformerNull.getInstance(),
				this,
				netPrefs, true, true );
		normDVar.setNodeLabel( myNodeLabelNormal );
		myNodeLabelNormal.setSelected( false );
		tempLocation.setLocation( 50,50 );
		myNodeLabelNormal.setActualLocation( tempLocation );

		myEdge = new Arrow( myNodeLabelSelected, myNodeLabelNormal, CoordinateTransformerNull.getInstance(), netPrefs );
		myNodeLabelSelected.addOutBoundEdge( myEdge );
		myNodeLabelNormal.addInComingEdge( myEdge );

		add( myNodeLabelSelected );
		add( myNodeLabelNormal );

		setPreferences();
		setPreferredSize( new Dimension( 100, 100 ) );
	}

	public void actionPerformed( ActionEvent evt ){
		//System.out.println( "SimpleNetwork.actionPerformed()" );
		update( (Preference) evt.getSource() );
	}

	/*
	public void setVisible( boolean flag )
	{
		super.setVisible( flag );
		//System.out.println( "SimpleNetwork.setVisible("+flag+")" );
	}*/

	public void update( Preference pref )
	{
		//update all objects with new values/colors

		if(      pref.getKey() == SamiamPreferences.netBkgdClr ){
			setBackground( (Color) pref.getCurrentEditedValue() );
		}
		else if( pref.getKey() == SamiamPreferences.netBkgdClrNeedsCompile ){
			setBackground( (Color) pref.getCurrentEditedValue() );
		}

		handleNodeSizePreferences();

		myNodeLabelSelected.previewPreferences();
		myNodeLabelNormal.previewPreferences();
		myEdge.previewPreferences();

		repaint();
	}

	protected Dimension myPreferenceNodeSize = new Dimension();
	protected boolean myFlagFileOverridesPref = false;

	/** @since 20021106 */
	public void setPreferences()
	{
		//System.out.println ( "SimpleNetwork.setPreferences()" );

		Dimension dim = (Dimension) myNetPrefs.getMappedPreference( SamiamPreferences.nodeDefaultSize ).getValue();
		myPreferenceNodeSize.setSize( dim );
		Boolean bool = (Boolean) myNetPrefs.getMappedPreference( SamiamPreferences.netSizeOverRidesPrefs ).getValue();
		myFlagFileOverridesPref = bool.booleanValue();
		resizeAll();

		myNodeLabelSelected.setPreferences();
		myNodeLabelNormal.setPreferences();
		myEdge.setPreferences();
	}

	/** @since 20021106 */
	protected void handleNodeSizePreferences()
	{
		//System.out.println ( "SimpleNetwork.handleNodeSizePreferences()" );

		boolean changed = false;

		Preference prefDS = myNetPrefs.getMappedPreference( SamiamPreferences.nodeDefaultSize );
		if( prefDS.isComponentEdited() )
		{
			changed = true;
			Dimension dim = (Dimension) prefDS.getCurrentEditedValue();
			//System.out.println( "\tmyPreferenceNodeSize = " + dim );
			myPreferenceNodeSize.setSize( dim );
		}

		Preference prefNSOP = myNetPrefs.getMappedPreference( SamiamPreferences.netSizeOverRidesPrefs );
		if( prefNSOP.isComponentEdited() )
		{
			changed = true;
			Boolean bool = (Boolean) prefNSOP.getCurrentEditedValue();
			//System.out.println( "\tmyFlagFileOverridesPref = " + bool );
			myFlagFileOverridesPref = bool.booleanValue();
		}

		if( changed ) resizeAll();
	}

	/** @since 20021106 */
	protected void resizeAll()
	{
		//System.out.println ( "SimpleNetwork.resizeAll()" );

		myNodeLabelSelected.setVirtualSize( myPreferenceNodeSize );
		myNodeLabelNormal.setVirtualSize( myPreferenceNodeSize );
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g); //paint background
		myEdge.paint( g, RECT_ALL, false );
	}

	//SelectionListener
	public void selectionChanged( NetworkComponentLabel label ){}
	public void selectionReset(){}

	public static final Rectangle RECT_ALL = new Rectangle( 0,0,Integer.MAX_VALUE, Integer.MAX_VALUE );
}
