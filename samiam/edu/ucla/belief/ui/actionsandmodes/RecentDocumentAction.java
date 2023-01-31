package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.ui.UI;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
	@author Keith Cascio
	@since 120502
*/
public class RecentDocumentAction extends AbstractAction
{
	public RecentDocumentAction( String displayname, String path, UI ui )
	{
		super.putValue( Action.NAME, displayname );
		super.putValue( Action.SHORT_DESCRIPTION, path );
		myUI = ui;
	}

	/**
		@author Keith Cascio
		@since 102402
	*/
	public void putValue( String key, Object newValue )
	{
		throw new UnsupportedOperationException();
	}

	/**
		@author Keith Cascio
		@since 111802
	*/
	public String toString()
	{
		return (String) super.getValue( Action.NAME );
	}

	public void actionPerformed( ActionEvent evt )
	{
		String path = (String) super.getValue( Action.SHORT_DESCRIPTION );
		File f = new File( path );
		if( f.exists() ) myUI.openFile( f );
		else JOptionPane.showMessageDialog( myUI, "File \"" + path + "\" does not exist.", "File Error", JOptionPane.ERROR_MESSAGE );
	}

	public static final int INT_MAX_DISPLAY_LENGTH = (int)31;
	public static final int INT_LEN_FIRST_CHUNK = (int)7;
	public static final String STR_ELLIPSIS = "...";
	public static final int INT_LEN_ELLIPSIS_CHUNK = INT_MAX_DISPLAY_LENGTH - STR_ELLIPSIS.length();
	public static final int INT_LEN_LAST_CHUNK = INT_LEN_ELLIPSIS_CHUNK - INT_LEN_FIRST_CHUNK;

	public static String makeDisplayName( String path, int index )
	{
		String strIndex = String.valueOf( index ) + "  ";
		if( path.length() <= INT_MAX_DISPLAY_LENGTH )
		{
			return strIndex + path;
		}
		else return strIndex + path.substring( 0, INT_LEN_FIRST_CHUNK ) + STR_ELLIPSIS + path.substring( path.length() - INT_LEN_LAST_CHUNK );
	}

	protected UI myUI;
}