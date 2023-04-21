package edu.ucla.belief.ui.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.StringTokenizer;
import java.net.URL;

import edu.ucla.belief.ui.UI;

/**
	A simple, static class to display a URL in the system browser.
	By Steven Spencer
	http://www.javaworld.com/javaworld/javatips/jw-javatip66.html

	Under Unix, the system browser is hard-coded to be 'netscape'.
	Netscape must be in your PATH for this to work.	This has been
	tested with the following platforms: AIX, HP-UX and Solaris.

	Under Windows, this will bring up the default browser under windows,
	usually either Netscape or Microsoft IE.	The default browser is
	determined by the OS.	This has been tested under Windows 95/98/NT.

	Examples:

	BrowserControl.displayURL("http://www.javaworld.com")

	BrowserControl.displayURL("file://c:\\docs\\index.html")

	BrowserContorl.displayURL("file:///user/joe/index.html");

	Note - you must include the url type -- either "http://" or
	"file://".

	@author Steven Spencer
	@since 090402
*/
public class BrowserControl
{
	public static boolean DEBUG_VERBOSE = Util.DEBUG_VERBOSE;

	/**
		@author Keith Cascio
		@since 090402
	*/
	public static void displayRelativePath( String relativepath )
	{
		String absolutePath = (new File(".")).getAbsolutePath();
		//This doesn't work:
		//String absolutePath = (new File(".")).getParentFile().getAbsolutePath();
		displayAbsolutePath( absolutePath + File.separator + relativepath );
	}

	/** @since 060404 */
	public static void displayAbsolutePath( String absolutePath )
	{
		String newURL = "file://" + absolutePath;
		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "Attempting open browser, URL: \"" + newURL + "\"" );
		displayURL( newURL );
	}

	/**
		@author Keith Cascio
		@since 121703
		found at: http://www.cs.rpi.edu/~puninj/XMLJ/projects/stuproj/vecchr/NewsFetcher-Submit/NewsFetcher.java

	* class Browser Copyright (C) 1999-2001 Fredrik Ehnbom <fredde@gjt.org>
	* available at
	* <http://www.gjt.org/servlets/JCVSlet/show/gjt/org/gjt/fredde/util/net/Browser.java/HEAD>
	* used under the terms of the GNU public license
	*
	* If the default browser is Internet Explorer 5.0 or greater,
	* the URL.DLL program fails if the url ends with .htm or .html .
	* This problem is described by Microsoft at
	* http://support.microsoft.com/support/kb/articles/Q283/2/25.ASP
	* Of course, their suggested workaround is to use the classes from the
	* microsoft Java SDK, but fortunately another workaround does exist.
	* If you alter the url slightly so it no longer ends with ".htm",
	* the URL can launch successfully. The logic here appends a null query
	* string onto the end of the URL if none is already present, or
	* a bogus query parameter if there is already a query string ending in
	* ".htm"
	*/
	private static String maybeFixupURLForWindows( String url )
	{
		//System.out.println( "BrowserControl.maybeFixupURLForWindows( "+url+" ), url.charAt(1) == " + url.charAt(1) );
		// plain filenames (e.g. c:\some_file.html or \\server\filename) do
		// not need fixing.
		if( url == null || url.length() < 2 || url.charAt(0) == '\\' || url.charAt(1) == ':' || url.startsWith( "file:" ) ) return url;
		String lower_url = url.toLowerCase();

		for( int i = badEndings.length; i-- > 0; )
			if( lower_url.endsWith( badEndings[i] ) )
				return fixupURLForWindows(url);
		return url;
	}

	public static final String[] badEndings = { ".htm", ".html", ".htw", ".mht", ".cdf", ".mhtml", ".stm" };

	private static String fixupURLForWindows( String url ) {
		if( url.indexOf('?') == -1 ) return url + "?";
		else return url + "&workaroundWindowsBug";
		//else return url + "&workaroundStupidWindowsBug";
	}

	/** @since 20051005 */
	public static void displayURL( URL url ){
		displayURL( url.toString() );
	}

	/**
	* Display a file in the system browser.	If you want to display a
	* file, you must include the absolute path name.
	*
	* @param url the file's url (the url must start with either "http://" or
	* "file://").
	*/
	public static void displayURL(String url)
	{
		boolean windows = isWindowsPlatform();
		String cmd = null;
		try
		{
			if (windows)
			{
				// cmd = 'rundll32 url.dll,FileProtocolHandler http://...'
				cmd = WIN_PATH + " " + WIN_FLAG + " " + maybeFixupURLForWindows( url );
				Process p = Runtime.getRuntime().exec(cmd);
			}
			else
			{
				// Under Unix, Netscape has to be running for the "-remote"
				// command to work.	So, we try sending the command and
				// check for an exit value.	If the exit command is 0,
				// it worked, otherwise we need to start the browser.
				// cmd = 'netscape -remote openURL(http://www.javaworld.com)'
				cmd = UNIX_PATH + " " + UNIX_FLAG + "(" + url + ",new-window)";
				if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.print( "exec(" + cmd + ") == " );
				Process p = Runtime.getRuntime().exec(cmd);
				InputStream netscapeStdErr = p.getErrorStream();
				BufferedReader readerNetscapeStdErr = null;
				if( netscapeStdErr != null ) readerNetscapeStdErr = new BufferedReader( new InputStreamReader( netscapeStdErr ) );
				try
				{
					// wait for exit code -- if it's 0, command worked,
					// otherwise we need to start the browser up.
					int exitCode = p.waitFor();
					if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( exitCode );

					boolean remoteFailed = ( exitCode != 0 );

					if( !remoteFailed && readerNetscapeStdErr != null ) remoteFailed = detectNetscapeComplaint( readerNetscapeStdErr );

					if( remoteFailed )
					{
						// Command failed, start up the browser
						// cmd = 'netscape http://www.javaworld.com'
						cmd = UNIX_PATH + " "	+ url;
						if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "exec(" + cmd + ")" );
						p = Runtime.getRuntime().exec(cmd);
						Thread.sleep( 3000 );
					}
				}
				catch(InterruptedException x)
				{
					Thread.currentThread().interrupt();
					if( DEBUG_VERBOSE ) System.err.println("Error bringing up browser, cmd='" + cmd + "'");
					if( DEBUG_VERBOSE ) System.err.println("Caught: " + x);
				}
			}
		}
		catch(IOException x)
		{
			// couldn't exec browser
			if( DEBUG_VERBOSE ) System.err.println("Could not invoke browser, command=" + cmd);
			if( DEBUG_VERBOSE ) System.err.println("Caught: " + x);
		}

		if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "BrowserControl.displayURL() returning" );
	}

	/**
		@author Keith Cascio
		@since 090602
	*/
	protected static boolean detectNetscapeComplaint( BufferedReader readerNetscapeStdErr ) throws IOException
	{
		boolean netscapeComplained = false;
		String tempLine = null;

		while ((tempLine = readerNetscapeStdErr.readLine()) != null)
		{
			if( tempLine.indexOf( "netscape" ) != (int)-1 && tempLine.indexOf( "not running" ) != (int)-1 )
			{
				if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "netscape complaint: \"" + tempLine + "\"" );
				netscapeComplained = true;
			}
			else if( DEBUG_VERBOSE ) Util.STREAM_VERBOSE.println( "netscape stderr:" + tempLine );
		}

		return netscapeComplained;
	}

	/**
	* Try to determine whether this application is running under Windows
	* or some other platform by examing the "os.name" property.
	*
	* @return true if this application is running under a Windows OS
	*/
	public static boolean isWindowsPlatform()
	{
		String os = System.getProperty( STR_KEY_OS_PROPERTY ).toLowerCase();
		return ( os != null && os.startsWith( WIN_ID ) );
	}

	/**
		@author Keith Cascio
		@since 100703 Election day!
	*/
	public static boolean isMacPlatform()
	{
		//System.out.println( "BrowserControl.isMacPlatform()" );
		if( FLAG_MAC == null )
		{
			String os = System.getProperty( STR_KEY_OS_PROPERTY );
			os = os.toLowerCase();
			boolean ret = false;
			if( os != null )
			{
				ret = (os.indexOf( MAC_ID ) != (int)-1);
				if( !ret )
				{
					String condensed = "";
					for( StringTokenizer toker = new StringTokenizer( os, " \t" ); toker.hasMoreTokens(); condensed += toker.nextToken() );
					ret |= (condensed.indexOf( OSX_ID ) != (int)-1);
				}
			}
			FLAG_MAC = ret ? Boolean.TRUE : Boolean.FALSE;
		}

		return FLAG_MAC.booleanValue();
	}

	/**
	* Simple example.
	*/
	public static void main(String[] args)
	{
		DEBUG_VERBOSE = true;
		displayURL("http://www.cs.ucla.edu");
		try{
			Thread.sleep( 10000 );
		}catch( InterruptedException e ){
			Thread.currentThread().interrupt();
			System.err.println( "BrowserControl.main() interrupted" );
		}

		Util.STREAM_TEST.println( "BrowserControl.main() returning" );
	}

	// Used to identify the windows platform.
	private static final String STR_KEY_OS_PROPERTY = "os.name";
	private static final String WIN_ID = "windows";
	private static final String MAC_ID = "mac";
	private static final String OSX_ID = "osx";
	private static Boolean FLAG_MAC = null;
	// The default system browser under windows.
	private static final String WIN_PATH = "rundll32";
	// The flag to display a url.
	private static final String WIN_FLAG = "url.dll,FileProtocolHandler";
	// The default browser under unix.
	private static final String UNIX_PATH = "netscape";
	// The flag to display a url.
	private static final String UNIX_FLAG = "-remote openURL";
}
