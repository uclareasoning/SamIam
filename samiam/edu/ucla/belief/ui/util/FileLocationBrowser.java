package edu.ucla.belief.ui.util;

import edu.ucla.belief.ui.preference.SamiamPreferences;

import edu.ucla.belief.io.*;

import javax.swing.filechooser.FileFilter;
import java.awt.*;
import javax.swing.*;
import java.io.*;

/** Manages browsing for a File (can be a Directory),
	and saving it in preferences for later reference,
	great for external executable applications,
	e.g. code bandit viewer

	@author Keith Cascio
	@since 020905 */
public class FileLocationBrowser
{
	public static final String STR_DIALOG_TITLE_PRE = "Browse for ";
	public static final String STR_TOOLTIP_PRE = "Select ";
	public static final String STR_BUTTONTEXT = "Select";

	public FileLocationBrowser( FileInformationSource source ){
		this.myFileInformationSource = source;
	}

	public static class Platform
	{
		public static Platform WINDOWS = new Platform();
		public static Platform MAC = new Platform();
		public static Platform UNIX = new Platform();

		public static Platform getCurrent(){
			if( CURRENT == null ){
				if( BrowserControl.isWindowsPlatform() ) CURRENT = WINDOWS;
				else if( BrowserControl.isMacPlatform() ) CURRENT = MAC;
				else CURRENT = UNIX;
			}
			return CURRENT;
		}

		private static Platform CURRENT;
	}

	public interface FileInformationSource
	{
		public FileFilter getFilter( FileLocationBrowser.Platform platform );
		public String getDescription( FileLocationBrowser.Platform platform );
		public JComponent getAccessory( FileLocationBrowser.Platform platform );
		public SamiamPreferences getPreferences();
		public String getPreferenceFileToken();
		public Component getDialogParent();
	}

	public void setFile( File file ){
		if( myFile == file ) return;
		myFile = file;
		SamiamPreferences prefs = myFileInformationSource.getPreferences();
		if( prefs != null ) prefs.putProperty( myFileInformationSource.getPreferenceFileToken(), file );
	}

	public File getFile(){
		if( myFile == null ){
			SamiamPreferences prefs = myFileInformationSource.getPreferences();
			if( prefs != null ) myFile = prefs.getFile( myFileInformationSource.getPreferenceFileToken() );
			if( (myFile == null) || (!myFile.exists()) ){
				myFile = null;
				JFileChooser chooser = getChooser();
				int result = chooser.showOpenDialog( myFileInformationSource.getDialogParent() );
				if( result == JFileChooser.APPROVE_OPTION ){
					File file = chooser.getSelectedFile();
					if( (file != null) && file.exists() ){
						setFile( file );
					}
				}
			}
		}
		return myFile;
	}

	public JFileChooser getChooser(){
		if( myJFileChooser == null ){
			JFileChooser chooser = myJFileChooser = new JFileChooser();
			chooser.setAcceptAllFileFilterUsed( false );

			Platform currentPlatform = Platform.getCurrent();

			String descrip = myFileInformationSource.getDescription( currentPlatform );

			FileFilter filter = myFileInformationSource.getFilter( currentPlatform );
			if( filter == null ) filter = getAcceptAllFilter( descrip );
			chooser.addChoosableFileFilter( filter );
			chooser.setFileFilter( filter );

			chooser.setAccessory( myFileInformationSource.getAccessory( currentPlatform ) );

			chooser.setApproveButtonText( STR_BUTTONTEXT );
			chooser.setApproveButtonToolTipText( STR_TOOLTIP_PRE + descrip );
			chooser.setDialogTitle( STR_DIALOG_TITLE_PRE + descrip );
			chooser.setMultiSelectionEnabled( false );
		}
		return myJFileChooser;
	}

	private FileFilter getAcceptAllFilter( final String description ){
		return new FileFilter(){
			public String getDescription(){
				return description;
			}
			public boolean accept( File f ){
				return true;
			}
		};
	}

	private File myFile;
	private JFileChooser myJFileChooser;
	private FileInformationSource myFileInformationSource;
}
