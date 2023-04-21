package edu.ucla.belief.ui.dialogs;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.networkdisplay.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.animation.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;

/** @author keith cascio */
public class PackageOptionsDialog extends JDialog implements ActionListener
{
	public          static boolean   FLAG_DEBUG_VERBOSE                 = Util.DEBUG_VERBOSE, DEBUG_BORDERS = false, DEBUG_STRUTS = false, DEBUG_GLUE = false;
	protected final static int       INT_ITEM_BORDER_SPACE_Y            = 0x2,
	                                 INT_ITEM_BORDER_SPACE_X            = 0x8,
	                                 INT_SIZE_HSTRUT                    = 0x80;

	protected   UI                   parentUI;
	protected   SamiamPreferences    mySamiamPreferences;
	protected   JTabbedPane          myTabbedPane;
	protected   Collection           myAddedPrefGroups                  = new LinkedList();
	protected   Map                  myMapPreferenceGroupsToJComponents = new HashMap(), myMapKeysToJComponents = new HashMap();

	/** @since 20020712 */
	public PackageOptionsDialog()
	{
		super( (JFrame)null, UI.STR_SAMIAM_ACRONYM + " Preferences", true );//modal dialog

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		Container         contentPane =  getContentPane();
		contentPane.add( myTabbedPane = new JTabbedPane(), BorderLayout.CENTER );
		contentPane.add(              createButtonPanel(), BorderLayout.SOUTH  );
	}

	/** @author keith cascio
		@since  20020715 */
	public PackageOptionsDialog( SamiamPreferences pref, UI ui )
	{
		this();

		this.parentUI = ui;

		mySamiamPreferences = pref;
		PreferenceGroup pg = null;
		for( Iterator it = mySamiamPreferences.getPreferenceGroups(); it.hasNext(); )
		{
			if( (pg = (PreferenceGroup) it.next()).isVisible() ) addPreferenceGroup( pg );
		}

		pack();
	}

	/** @since 20050216 */
	public JComponent forGroup( PreferenceGroup pGroup ){
		if( myMapPreferenceGroupsToJComponents == null ) return (JComponent) null;
		else return (JComponent) myMapPreferenceGroupsToJComponents.get( pGroup );
	}

	/** @since 20050216 */
	public void setSelected( PreferenceGroup pGroup ){
		if( myTabbedPane == null ) return;
		JComponent forGroup = forGroup( pGroup );
		if( forGroup == null ) return;
		myTabbedPane.setSelectedComponent( forGroup );
	}

	/** @since 20020712 */
	public void addPreferenceGroup( PreferenceGroup newPreferenceGroup )
	{
		if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "\n\naddPreferenceGroup( " + newPreferenceGroup.getName() + " )" );

		JComponent newPanel = makePreferenceGroupComponent( newPreferenceGroup );

		if( newPreferenceGroup.userobject != null && newPreferenceGroup.userobject instanceof JComponent )
		{
			newPanel = composeComponents( newPanel, (JComponent) newPreferenceGroup.userobject );
			if( newPreferenceGroup.userobject instanceof SimpleNetwork ){
				((SimpleNetwork) newPreferenceGroup.userobject).setPreferences();
			}
			else if( newPreferenceGroup.userobject instanceof ColorIntensitySample ){
				((ColorIntensitySample) newPreferenceGroup.userobject).setPreferences();
			}
		}

		myTabbedPane.addTab( newPreferenceGroup.getName(), null, newPanel, newPreferenceGroup.getName() + " Display Options" );
		myMapPreferenceGroupsToJComponents.put( newPreferenceGroup, newPanel );

		if( FLAG_DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "DONE addPreferenceGroup( " + newPreferenceGroup.getName() + " )\n\n" );
	}

	/** @since 20020717 */
	public JComponent composeComponents( JComponent mainComponent, JComponent subComponent )
	{
		SwingUtilities.updateComponentTreeUI( subComponent );

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout( new BorderLayout() );
		mainPanel.add( mainComponent, BorderLayout.CENTER );
		mainPanel.add( subComponent, BorderLayout.SOUTH );
		return mainPanel;
	}

	/** @since 20020717 */
	protected JComponent makePreferenceGroupComponent( PreferenceGroup newPreferenceGroup )
	{
		JPanel             pnl = new JPanel( new GridBagLayout() );
		GridBagConstraints c   = new GridBagConstraints();

		c.anchor    = GridBagConstraints.NORTHWEST;
		c.gridwidth = 1;
		pnl.add( hstrut( INT_SIZE_HSTRUT ), c );
		pnl.add( hstrut( INT_SIZE_HSTRUT ), c );
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnl.add(                    glue(), c );

		Preference prf;
		String     tip;
		JLabel     lbl;
		JComponent com;
		for( Iterator it    = newPreferenceGroup.preferences.iterator(); it.hasNext(); ){
			tip =   (prf    = (Preference) it.next()).getDescription();
			addLine( c, lbl = prf.displayCaption() ? new JLabel( prf.getDisplayName() ) : null, com = prf.getEditComponent(), pnl );
			if(      tip   != null                                    ){
				if(  lbl   != null                                    ){ lbl.setToolTipText( tip ); }
				if( (com   != null) && (com.getToolTipText() == null) ){ com.setToolTipText( tip ); }
			}
		}

		c.weighty   = 1;
		c.fill      = GridBagConstraints.NONE;
		c.gridwidth = GridBagConstraints.REMAINDER;
		pnl.add( vstrut(4), c );

		pnl.setBorder( BorderFactory.createEmptyBorder( 0x10, 4, 0x10, 4 ) );

		if( DEBUG_BORDERS ){ pnl.setBorder( BorderFactory.createCompoundBorder( pnl.getBorder(), BorderFactory.createLineBorder( Color.red, 1 ) ) ); }

		try{
			SwingUtilities.updateComponentTreeUI( pnl );
		}catch( Throwable thrown ){
			System.err.println( "warning: PackageOptionsDialog.makePreferenceGroupComponent() caught " + thrown );
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				thrown.printStackTrace( Util.STREAM_VERBOSE );
			}
		}

		return pnl;
	}

	private Component vstrut( int size ){
		Component ret = DEBUG_STRUTS ? new JLabel( "vstrut" ) : Box.createVerticalStrut( size );
		if( DEBUG_BORDERS ){
			JPanel pnl = new JPanel( new GridLayout(1,1) );
			pnl.add( ret );
			pnl.setBorder( BorderFactory.createLineBorder( Color.orange, 1 ) );
			ret = pnl;
		}
		return ret;
	}

	private Component hstrut( int size ){
		Component ret = DEBUG_STRUTS ? new JLabel( "-hstrut-" ) : Box.createHorizontalStrut( size );
		if( DEBUG_BORDERS ){
			JPanel pnl = new JPanel( new GridLayout(1,1) );
			pnl.add( ret );
			pnl.setBorder( BorderFactory.createLineBorder( Color.blue, 1 ) );
			ret = pnl;
		}
		return ret;
	}

	private Component   glue(){
		return DEBUG_GLUE ? new JLabel( "-glue-" ) : Box.createGlue();
	}

	/** @since 20020711 */
	protected void addLine( GridBagConstraints c, JLabel lbl, JComponent comp, JComponent pnl ){
		c.fill          = GridBagConstraints.HORIZONTAL;
		c.ipady         = INT_ITEM_BORDER_SPACE_Y;
		c.ipadx         = INT_ITEM_BORDER_SPACE_X;

		if( lbl  == null ){
			c.gridwidth = GridBagConstraints.REMAINDER;
			pnl.add( vstrut(8), c );
			c.weightx   = 1;
		}
		else{
			c.gridwidth = 1;
			pnl.add( lbl, c );
		}

		if( comp != null ){
			c.gridwidth = GridBagConstraints.RELATIVE;
			pnl.add( comp, c );
		}

		c.weightx       = 0;
		c.fill          = GridBagConstraints.NONE;
		c.ipady         = c.ipadx   = 0;
		c.gridwidth     = GridBagConstraints.REMAINDER;
		pnl.add( vstrut( 0x16 ), c );
	}

	/** Handle the buttons in the button pane being pressed.*/
	public void actionPerformed( ActionEvent e ){
		boolean apply = false, dispose = false;
		String     ac = e.getActionCommand();
		if(        ac.equals( "OK"     ) ){ apply = dispose = true; }
		else if(   ac.equals( "Apply"  ) ){ apply =           true; }
		else if(   ac.equals( "Cancel" ) ){         dispose = true; }
		else{
			System.err.println( "ERROR: Unexpected ActionCommand in PackageOptionsDialog.actionPerformed()" );
			System.err.println( e );
		}

		if( apply   ){ onApply(); }
		if( dispose ){ dispose(); }
	}

	/** @since 20020715 */
	protected boolean commitPreferenceGroupChanges()
	{
		boolean changed = false;
		PreferenceGroup tempPreferenceGroup = null;

		for( Iterator it = myMapPreferenceGroupsToJComponents.keySet().iterator(); it.hasNext(); )
		{
			tempPreferenceGroup = (PreferenceGroup) it.next();
			if( tempPreferenceGroup.commitValues() ) changed = true;
		}

		return changed;
	}

	/** Handle Apply button.*/
	private void onApply()
	{
		if( !myMapPreferenceGroupsToJComponents.isEmpty() )
		{
			if( commitPreferenceGroupChanges() )
			{
				if( mySamiamPreferences != null )
				{
					mySamiamPreferences.saveOptionsToFile();
					if( parentUI != null )
					{
						parentUI.changePackageOptions( mySamiamPreferences );
						mySamiamPreferences.setRecentlyCommittedFlags( false );
					}
				}
			}
		}
	}

	/**
	* Creates a Panel with an OK, Apply, and Cancel button.
	*/
	private JPanel createButtonPanel()
	{
		JPanel pnl = new JPanel();
		pnl.setLayout( new BoxLayout( pnl, BoxLayout.X_AXIS));
		pnl.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pnl.add( Box.createHorizontalGlue());

		//String buttonNames[] = {"OK", "Apply", "Cancel"};
		String buttonNames[] =
		{
			"OK", "Cancel"
		};

		for( int i = 0; i < buttonNames.length; i++)
		{
			if( i != 0)
			{
				//add space between buttons
				pnl.add( Box.createRigidArea( new Dimension(10, 0)));
			}

			JButton btn = new JButton( buttonNames[i]);
			btn.setActionCommand( buttonNames[i]);
			btn.addActionListener( this);
			pnl.add( btn);
		}

		return pnl;
	}

	/** @since 20020308 */
	private class LookAndFeelInfoRenderer implements ListCellRenderer
	{
		public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus)
		{
			return new JLabel( ((UIManager.LookAndFeelInfo)value).getName() );
		}
	}
}
