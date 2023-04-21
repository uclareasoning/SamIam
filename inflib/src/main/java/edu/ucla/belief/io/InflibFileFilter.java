package edu.ucla.belief.io;

import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
	@author Keith Cascio
	@since 121302
*/
public class InflibFileFilter extends FileFilter
{
	public InflibFileFilter( String[] extensions, String description )
	{
		myExtensions = extensions;
		myDescription = description;
	}

	public boolean accept( java.io.File file )
	{
		if( file == null ) return false;
		else if( file.isDirectory() ) return true;
		else
		{
			String strFileName = file.getName();
			if( myFlagNotCaseSensitive ) strFileName = strFileName.toLowerCase();

			for( int i=0; i<myExtensions.length; i++ ){
				if( strFileName.endsWith( myExtensions[i] ) ) return true;
			}
		}
		return false;
	}

	/** @since 420! '04 */
	public boolean getCaseSensitive(){
		return !myFlagNotCaseSensitive;
	}

	/** @since 420! '04 */
	public void setCaseSensitive( boolean flag ){
		myFlagNotCaseSensitive = !flag;
	}

	public File validateExtension( File selectedFile )
	{
		String path = selectedFile.getPath();
		if( !path.endsWith(myExtensions[0]) && path.length() > 0 ) return new File( path + myExtensions[0] );
		else return selectedFile;
	}

	public String getDescription() {
		return myDescription;
	}

	/**
		@author Keith Cascio
		@since 051503
	*/
	public String[] getExtensions()
	{
		return myExtensions;
	}

	private boolean myFlagNotCaseSensitive;
	private String[] myExtensions;
	private String myDescription;
}
