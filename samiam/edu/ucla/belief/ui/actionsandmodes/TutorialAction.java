package edu.ucla.belief.ui.actionsandmodes;

import edu.ucla.belief.io.NetworkIO;

import edu.ucla.belief.ui.dialogs.Tutorials;
import edu.ucla.belief.ui.util.BrowserControl;
import edu.ucla.belief.ui.util.Util;
import edu.ucla.belief.ui.UI;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
	@author Keith Cascio
	@since 20040303
*/
public class TutorialAction extends SamiamAction
{
	public TutorialAction( String nameAction, String relativePath, String nameTutorial, char mnemonic, Icon icon )
	{
		super( nameAction, relativePath + nameTutorial, mnemonic, icon );
		checkExistence( relativePath, nameTutorial );
		setCommandPrefix();
	}

	/** @since 20051012 */
	public TutorialAction( String name ){
		super( name, name, 'p', (Icon)null );
		setCommandPrefix();
	}

	/** @since 20051012 */
	public void setPath( String relativePath, String nameTutorial ){
		this.myCommand = null;
		this.setToolTipText( relativePath + nameTutorial );
		checkExistence( relativePath, nameTutorial );
		//System.out.println( "myCommand \"" + myCommand + "\"" );
	}

	public void checkExistence(){
		checkExistence( myRelativePath, myNameTutorial );
	}

	public void checkExistence( String relativePath, String nameTutorial )
	{
		try{
			myRelativePath = relativePath;
			myNameTutorial = nameTutorial;
			if( myCommand != null && (new File(myCommand).exists()) ) return;
			myCommand = null;
			File directory = new File( relativePath );
			if( !directory.exists() ) return;
			File tutorial = findTutorial( directory, nameTutorial );
			if( tutorial != null && tutorial.exists() )
			{
				myFileNameSansPath = NetworkIO.extractFileNameFromPath( tutorial.getPath() );
				myAbsolutePath = tutorial.getParentFile().getAbsolutePath();
				try{
					myCommand = tutorial.getCanonicalPath();
				}catch( Exception e ){
					myCommand = tutorial.getAbsolutePath();
				}
				myFlagUsePrefix = !myCommand.toLowerCase().endsWith( STR_EXE_FILE_EXTENSION );
				if( myFlagUsePrefix ) setCommandPrefix();
			}
		}catch( Exception e ){
			myCommand = null;
		}finally{
			setEnabled( myCommand != null );
		}
	}

	public void actionPerformed( ActionEvent evt )
	{
		Process p;
		try{
			if( myCommand == null ) return;

			String command = myCommand;
			if( myFlagUsePrefix )
			{
				if( STR_COMMAND_PREFIX == null ) return;
				else command = STR_COMMAND_PREFIX + " \"" + myAbsolutePath + "\" " + myFileNameSansPath;
				//else command = STR_COMMAND_PREFIX + " " + command;
			}
			//System.out.println( "Runtime.getRuntime().exec( "+command+" )" );
			p = Runtime.getRuntime().exec( command );
		}catch( Exception e ){
			if( Util.DEBUG_VERBOSE )
			{
				System.err.println( "Warning: TutorialAction " + myCommand + " failed." );
			}
		}
	}

	public static File findTutorial( File directory, String name )
	{
		try{
			if( directory == null || !directory.isDirectory() ) return null;

			File[] files = directory.listFiles();
			File fileCurrent;
			String nameCurrent;
			for( int i=0; i<files.length; i++ )
			{
				fileCurrent = files[i];
				nameCurrent = NetworkIO.extractFileNameFromPath( fileCurrent.getPath() );
				if( nameCurrent.indexOf( name ) != (int)-1 ) return fileCurrent;
			}

			return null;
		}catch( Exception e ){
			return null;
		}
	}

	/** @since 030804 */
	public static final void setCommandPrefix()
	{
		if( STR_COMMAND_PREFIX == null )
		{
			if( BrowserControl.isWindowsPlatform() )
			{
				STR_COMMAND_PREFIX = STR_DOUBLE_QUOTE + Tutorials.getWindowsScriptCommand() + STR_DOUBLE_QUOTE;
				//if( testExec( STR_PREFIX_CMD_EXE + " echo SamIam test" ) ) STR_COMMAND_PREFIX = STR_PREFIX_CMD_EXE;
				//else STR_COMMAND_PREFIX = STR_PREFIX_COMMAND_COM;
			}
			else if( BrowserControl.isMacPlatform() ) STR_COMMAND_PREFIX = STR_PREFIX_MAC;
		}
	}

	/** test/debug */
	public static void main(String args[])
	{
		testExec( "cmd.exe /x /c echo \"Hello from cmd.exe\"" );
		testExec( "command.com /c echo \"Hello from command.com\"" );
		testExec( "bs.exe" );
	}

	/** @since 030804 */
	public static boolean testExec( String strCommand )
	{
		try{
			Runtime rt = Runtime.getRuntime();
			Process p1 = rt.exec( strCommand );
			//System.out.println( "Executed: " + strCommand );
			int retval1 = p1.waitFor();
			//System.out.println( "process: " + p1 + ", return value: " + retval1 );
		}catch( Error e ){
			return false;
		}
		catch( Exception e ){
			//e.printStackTrace();
			return false;
		}
		return true;
	}

	public static final String STR_DOUBLE_QUOTE = "\"";
	public static final String STR_EXE_FILE_EXTENSION = ".exe";
	public static final String STR_PREFIX_MAC = "open ";
	//public static final String STR_PREFIX_WINDOWS = "%comspec% /x /c ";
	public static final String STR_PREFIX_COMMAND_COM = "command.com /c ";
	public static final String STR_PREFIX_CMD_EXE = "cmd.exe /c ";
	private static String STR_COMMAND_PREFIX;

	private String myCommand;
	private String myRelativePath;
	private String myNameTutorial;
	private String myFileNameSansPath;
	private String myAbsolutePath;
	private boolean myFlagUsePrefix = false;
}
