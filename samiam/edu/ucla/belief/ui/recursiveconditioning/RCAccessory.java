package edu.ucla.belief.ui.recursiveconditioning;

import edu.ucla.belief.dtree.*;
import edu.ucla.belief.recursiveconditioning.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.event.*;
import java.util.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;

/**
	@author Keith Cascio
	@since 031903
*/
public class RCAccessory extends JPanel implements PropertyChangeListener
{
	public RCAccessory()
	{
		init();
	}

	protected void init()
	{
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		Component strut;

		setLayout( gridbag );

		c.gridwidth = GridBagConstraints.REMAINDER;
		lblNotRCFile = new JLabel( "Not an rc file." );
		add( lblNotRCFile, c );

		add( makeMainPanel(), c );

		Border etched = BorderFactory.createEtchedBorder();
		Border empty = BorderFactory.createEmptyBorder( 4,4,4,4 );
		Border compound = BorderFactory.createCompoundBorder( etched, empty );
		setBorder( compound );

		Dimension preferred = gridbag.preferredLayoutSize( this );
		setPreferredSize( preferred );

		clear();
	}

	protected JPanel makeMainPanel()
	{
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		Component strut;

		pnlMain = new JPanel( gridbag );

		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		JLabel lblTitle = new JLabel( "rc file contains:" );
		pnlMain.add( lblTitle, c );

		strut = Box.createHorizontalStrut( 4 );
		pnlMain.add( strut, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		lblType = new JLabel( "rcdgraph" );
		pnlMain.add( lblType, c );

		strut = Box.createVerticalStrut( 4 );
		pnlMain.add( strut, c );

		pnlMain.add( makeNetworkNamePanel(), c );

		c.anchor = GridBagConstraints.WEST;
		strut = Box.createVerticalStrut( 8 );
		pnlMain.add( strut, c );

		lblUserMemory = new JLabel( "0/0 bytes" );
		lblUserMemory.setVisible( false );
		pnlMain.add( lblUserMemory, c );

		lblEstimatedTime = new JLabel( "marginals 0 secs" );
		lblEstimatedTime.setVisible( false );
		pnlMain.add( lblEstimatedTime, c );

		c.anchor = GridBagConstraints.WEST;
		myStrut = Box.createVerticalStrut( 8 );
		pnlMain.add( myStrut, c );

		JLabel lblDtreeProps = new JLabel( "dtree properties" );
		pnlMain.add( lblDtreeProps, c );

		strut = Box.createVerticalStrut( 5 );
		pnlMain.add( strut, c );

		lblMethod = buildStatGUI( c, "method:" );
		lblHeight = buildStatGUI( c, "height:" );
		lblMaxCluster = buildStatGUI( c, "max cluster:" );
		lblMaxCutset = buildStatGUI( c, "max cutset:" );
		lblMaxContext = buildStatGUI( c, "max context:" );

		return pnlMain;
	}

	protected JComponent makeNetworkNamePanel()
	{
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		Component strut;

		JPanel ret = new JPanel( gridbag );

		c.gridwidth = 1;
		c.weightx = 0;
		JLabel lblFor = new JLabel( "for" );
		ret.add( lblFor, c );

		strut = Box.createHorizontalStrut( 4 );
		ret.add( strut, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		lblNetworkName = new JLabel( "               " );
		ret.add( lblNetworkName, c );

		return ret;
	}

	public static final String STR_NA = "n/a";
	public static final String STR_UNAVAILABLE = "unavailable";

	protected JPanel pnlMain;
	protected JLabel lblMethod;
	protected JLabel lblHeight;
	protected JLabel lblMaxCluster;
	protected JLabel lblMaxCutset;
	protected JLabel lblMaxContext;
	protected JComponent lblNotRCFile;
	protected JLabel lblType;
	protected JLabel lblNetworkName;
	protected JLabel lblUserMemory;
	protected JLabel lblEstimatedTime;
	protected Component myStrut;

	public void clear()
	{
		//lblHeight.setText( STR_NA );
		//lblMaxCluster.setText( STR_NA );
		//lblMaxCutset.setText( STR_NA );
		//lblMaxContext.setText( STR_NA );
		pnlMain.setVisible( false );
		lblNotRCFile.setVisible( true );
	}

	public void assume( FileInfo info )
	{
		lblNotRCFile.setVisible( false );
		pnlMain.setVisible( true );
		lblType.setText( info.rcType );
		if( info.userMemory == null )
		{
			lblUserMemory.setText( "" );
			lblUserMemory.setVisible( false );
		}
		else
		{
			lblUserMemory.setText( info.userMemory );
			lblUserMemory.setVisible( true );
		}
		if( info.estimatedTime == null )
		{
			lblEstimatedTime.setText( "" );
			lblEstimatedTime.setVisible( false );
		}
		else
		{
			lblEstimatedTime.setText( info.estimatedTime );
			lblEstimatedTime.setVisible( true );
		}
		myStrut.setVisible( info.estimatedTime != null || info.userMemory != null );
		lblNetworkName.setText( info.networkName );
		lblMethod.setText( info.dtreeMethod );
		assume( info.stats );
	}

	public void assume( Stats stats )
	{
		String strHeight = Integer.toString( stats.height );
		String strMaxCluster = Integer.toString( stats.maxCluster );
		String strMaxCutset = Integer.toString( stats.maxCutset );
		String strMaxContext = Integer.toString( stats.maxContext );

		if( stats.height == Stats.INT_INVALID_DTREE_STAT ) strHeight = STR_UNAVAILABLE;
		if( stats.maxCluster == Stats.INT_INVALID_DTREE_STAT ) strMaxCluster = STR_UNAVAILABLE;
		if( stats.maxCutset == Stats.INT_INVALID_DTREE_STAT ) strMaxCutset = STR_UNAVAILABLE;
		if( stats.maxContext == Stats.INT_INVALID_DTREE_STAT ) strMaxContext = STR_UNAVAILABLE;

		lblHeight.setText( strHeight );
		lblMaxCluster.setText( strMaxCluster );
		lblMaxCutset.setText( strMaxCutset );
		lblMaxContext.setText( strMaxContext );
	}

	protected JLabel buildStatGUI( GridBagConstraints c, String caption )
	{
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0;
		JLabel lblCaption = new JLabel( caption );
		pnlMain.add( lblCaption, c );

		Component strut = Box.createHorizontalStrut( 8 );
		pnlMain.add( strut, c );

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		JLabel lblRet = new JLabel( STR_UNAVAILABLE );
		pnlMain.add( lblRet, c );

		return lblRet;
	}

	public void setFileFilter( FileFilter filter )
	{
		myFileFilter = filter;
	}

	public void propertyChange( PropertyChangeEvent evt )
	{
		if( evt.getPropertyName().equals( JFileChooser.SELECTED_FILE_CHANGED_PROPERTY ) )
		{
			File selected = (File) evt.getNewValue();
			//System.out.println( "RCAccessory.propertyChange("+selected.getPath()+")" );
			if( selected != null && !selected.isDirectory() && myFileFilter.accept( selected ) && selected.exists() )
			{
				try{
					assume( myXmlizer.skimFile( selected ) );
					return;
				}catch( IOException e ){
					System.err.println( "Warning: RCAccessory experienced " + e );
				}
			}

			clear();
		}
	}

	protected Xmlizer myXmlizer = new Xmlizer();
	protected FileFilter myFileFilter;
}
