package edu.ucla.belief.ui.primula;

import edu.ucla.belief.ui.util.Util;

//import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.net.*;

/**
	Helps a user browse for software components not found in the runtime classpath.
	@author Keith Cascio
	@since 042004
*/
public class RuntimeSoftwareLocationBrowser
{
	public RuntimeSoftwareLocationBrowser( SoftwareEntity[] packages, Component parent ){
		this.myPackages = packages;
		this.myParent = parent;
	}

	/** @since 031405 */
	public void forgetLocations(){
		myClassLoader = ClassLoader.getSystemClassLoader();
		myURLClassLoader = (HackedLoader) null;
		for( int i=0; i<myPackages.length; i++ ) myPackages[i].setCodeLocation( null );
	}

	public Object getInstance( SoftwareEntity softwarepackage ) throws UnsatisfiedLinkError
	{
		if( !contains( softwarepackage ) ) return null;

		Object myInstance = null;
		if( myInstance == null ){
			String strErrorMessage = null;
			try{
				Class classFound = findMainClass( softwarepackage );
				if( classFound != null ){
					myInstance = classFound.newInstance();
				}
			//}catch( ClassNotFoundException classnotfoundexception ){
			//	strErrorMessage = "class \""+softwarepackage.mainclassnamefull+"\" does not exist or not found.\n("+classnotfoundexception.getMessage()+")";
			}catch( Exception exception ){
				strErrorMessage = exception.toString();
			}catch( Error error ){
				strErrorMessage = error.toString();
			}finally{
				if( strErrorMessage != null ){
					System.err.println( "RuntimeSoftwareLocationBrowser.getInstance() failed because: " + strErrorMessage );
					throw new UnsatisfiedLinkError( strErrorMessage );
				}
			}
		}

		return myInstance;
	}

	public Class findMainClass( SoftwareEntity entity )
	{
		if( !contains( entity ) ) return null;

		Class classFound = loadClassWithMyLoader( entity.mainclassnamefull );
		if( classFound != null ) return classFound;

		if( myFlagNotCalledWithoutBrowsing ){
			myFlagLocatedWithoutBrowsing = locateEntitiesWithoutBrowsing();
			myFlagNotCalledWithoutBrowsing = false;
		}

		classFound = loadClassWithMyLoader( entity.mainclassnamefull );
		if( classFound != null ) return classFound;

		int ret = showConfirmDialog( /*UI.STR_SAMIAM_ACRONYM+" could*/"Could not find "+entity.displayname+".\nWould you like to browse for it?", entity.displayname+" not found" );
		if( ret == JOptionPane.YES_OPTION )
		{
			if( myFlagNotCalledByBrowsing ){
				myFlagNotCalledByBrowsing = false;
				myFlagLocatedByBrowsing = locateEntitiesByBrowsing();
				if( myFlagLocatedByBrowsing ){
					classFound = loadClassWithMyLoader( entity.mainclassnamefull );
					if( classFound != null ) return classFound;
				}
			}

			classFound = loadClassByBrowsing( entity );
			if( classFound != null ) return classFound;
		}

		return null;
	}

	public boolean locateEntitiesByBrowsing()
	{
		boolean ret = true;
		for( int i=0; i<myPackages.length; i++ ){
			ret &= ( loadClassByBrowsing( myPackages[i] ) != null );
		}
		return ret;
	}

	public Class loadClassByBrowsing( SoftwareEntity entity )
	{
		Class classFound = null;
		File locationMyClass = null;
		while( true ){
			try{
				locationMyClass = entity.browseForMyClass( myParent );
				if( locationMyClass == null ) break;
			}catch( Exception e ){
				if( Util.DEBUG_VERBOSE ){
					Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
					e.printStackTrace();
				}
				showMessageDialog( e.getMessage(), "Error browsing for " + entity.displayname );
				continue;
			}
			classFound = loadClassAtFile( entity.mainclassnamefull, locationMyClass );
			if( classFound != null ){
				entity.setCodeLocation( locationMyClass );
				return classFound;
			}
			else{
				showMessageDialog( locationMyClass.getPath() + " does not contain class " + entity.mainclassnamefull + ".", entity.displayname+" not found" );
				continue;
			}
		}

		return null;
	}

	public boolean locateEntitiesWithoutBrowsing()
	{
		boolean ret = true;
		SoftwareEntity entity = null;
		for( int i=0; i<myPackages.length; i++ ){
			entity = myPackages[i];
			if( loadClassAtFile( entity.mainclassnamefull, entity.getCodeLocation() ) == null ){
				entity.setCodeLocation( (File)null );
				ret = false;
			}
		}
		return ret;
	}

	public Class loadClassWithMyLoader( String strclassname )
	{
		try{
			return myClassLoader.loadClass( strclassname );
		}catch( ClassNotFoundException e ){
			return null;
		}
	}

	public Class loadClassAtPath( String strclassname, String strCodeLocationPath )
	{
		if( strCodeLocationPath == null ) return null;
		else return loadClassAtFile( strclassname, new File( strCodeLocationPath ) );
	}

	public Class loadClassAtFile( String strclassname, File fileCodeLocation )
	{
		if( fileCodeLocation == null || !fileCodeLocation.exists() ) return null;
		else{
			URL urlCodeLocation = null;
			try{
				urlCodeLocation = fileCodeLocation.toURL();
			}catch( MalformedURLException e ){
				return null;
			}
			return loadClassAtURL( strclassname, urlCodeLocation );
		}
	}

	public Class loadClassAtURL( String strclassname, URL urlCodeLocation )
	{
		if( urlCodeLocation == null ) return null;

		if( myURLClassLoader == null ) myClassLoader = myURLClassLoader = new HackedLoader( urlCodeLocation );
		else myURLClassLoader.addURL( urlCodeLocation );

		return loadClassWithMyLoader( strclassname );
	}

	private class HackedLoader extends URLClassLoader{
		public HackedLoader( URL url ){
			super( new URL[] { url } );
		}

		public void addURL( URL url ){
			super.addURL( url );
		}
	}

	public boolean contains( SoftwareEntity softwarepackage )
	{
		if( softwarepackage == null ) return false;
		for( int i=0; i<myPackages.length; i++ ){
			if( myPackages[i] == softwarepackage ) return true;
		}
		return false;
	}

	private void showMessageDialog( String msg, String title ){
		JOptionPane.showMessageDialog( myParent, msg, title, JOptionPane.WARNING_MESSAGE );
	}

	private int showConfirmDialog( String msg, String title ){
		return JOptionPane.showConfirmDialog( myParent, msg, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
	}

	private Component myParent;
	private ClassLoader myClassLoader = ClassLoader.getSystemClassLoader();
	private HackedLoader myURLClassLoader = null;
	private SoftwareEntity[] myPackages;
	private boolean myFlagNotCalledWithoutBrowsing = true;
	private boolean myFlagLocatedWithoutBrowsing = false;
	private boolean myFlagNotCalledByBrowsing = true;
	private boolean myFlagLocatedByBrowsing = false;
}
