package edu.ucla.belief.ui.primula;

import edu.ucla.belief.io.InflibFileFilter;
import edu.ucla.belief.io.NetworkIO;

//import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

/**
	Represents a bundle of software that exists somewhere on the file system, e.g. c:\\dev\\inflib\\compiled or primula.jar
	@author Keith Cascio
	@since 042004
*/
public class SoftwareEntity
{
	public final String displayname;
	public final String packagename;// = "RBNgui";
	public final String mainclassname;// = "Primula";
	public final String mainclassnamefull;// = STR_PACKAGENAME_RBNGUI+"."+STR_CLASSNAME_PRIMULA;
	public final String mainclassfilename;// = STR_CLASSNAME_PRIMULA+".class";
	public final String jarfilenamedefault;// = "primula.jar";
	private InflibFileFilter myFileFilterJar;
	private InflibFileFilter myFileFilterClass;
	private JFileChooser myJFileChooser;
	private File myCodeLocation;

	public SoftwareEntity( String displayname, String packagename, String mainclassname, String jarfilenamedefault )
	{
		this.displayname = displayname;
		this.packagename = packagename;
		this.mainclassname = mainclassname;
		this.mainclassnamefull = packagename + "." + mainclassname;
		this.mainclassfilename = mainclassname + ".class";
		this.jarfilenamedefault = jarfilenamedefault;
	}

	public String toString(){
		return "(" + super.toString() + ")" + displayname;
	}

	/** @since 042104 */
	public void setCodeLocation( File fileLocation ){
		myCodeLocation = fileLocation;
	}

	public File getCodeLocation(){
		return myCodeLocation;
	}

	public File browseForMyClass( Component parent ) throws Exception
	{
		//System.out.println( "(SoftwareEntity)"+this+".browseForMyClass()" );
		JFileChooser chooser = getJFileChooser();
		int ret = chooser.showOpenDialog( parent );
		File fileMyClass = chooser.getSelectedFile();

		if( (ret == JFileChooser.APPROVE_OPTION) && (fileMyClass != null) && fileMyClass.exists() ){
			if( myFileFilterJar.accept( fileMyClass ) ) return (myCodeLocation = fileMyClass);
			else if( myFileFilterClass.accept( fileMyClass ) || NetworkIO.extractFileNameFromPath( fileMyClass.getPath() ).equals( this.mainclassfilename ) ){
				File dirRoot = getCodeRoot( fileMyClass.getParentFile() );
				if( dirRoot != null ) return (myCodeLocation = dirRoot);
				else throw new RuntimeException( this.mainclassfilename+" must occur in package directory '"+this.packagename+"'." );
			}
			else throw new RuntimeException( "Incorrect file " + fileMyClass.getPath() + ": "+this.mainclassfilename+" or "+this.jarfilenamedefault+" required. " );
		}

		return null;
	}

	public JFileChooser getJFileChooser()
	{
		if( myJFileChooser == null ){
			//System.out.println( "(SoftwareEntity)"+this+" new JFileChooser" );
			myJFileChooser = new JFileChooser( "." );
			myJFileChooser.addChoosableFileFilter( myFileFilterJar = new InflibFileFilter( new String[]{".jar"}, this.displayname+" Jar Archive (*.jar)" ) );
			myJFileChooser.addChoosableFileFilter( myFileFilterClass = new InflibFileFilter( new String[]{ this.mainclassfilename }, this.displayname+" Main Class ("+this.mainclassfilename+")" ) );
			myFileFilterClass.setCaseSensitive( true );
			myJFileChooser.setFileFilter( myFileFilterJar );
			myJFileChooser.setApproveButtonText( "Load" );
			myJFileChooser.setDialogTitle( "Find "+this.displayname );
		}
		return myJFileChooser;
	}

	public File getCodeRoot( File dirPackage )
	{
		//System.out.println( "getCodeRoot( "+dirPackage.getPath()+" )" );
		if( (dirPackage == null) || (!dirPackage.exists()) || (!dirPackage.isDirectory()) ) return null;

		String strPathFragment = this.packagename.replace( '.', File.separatorChar );
		//System.out.println( "\t strPathFragment " + strPathFragment );

		if( dirPackage.getPath().endsWith( strPathFragment ) )
		{
			String[] strSplit = this.packagename.split( "\\." );
			File current = dirPackage;
			for( int i=0; (i<strSplit.length) && (current != null); i++ ) current = current.getParentFile();
			//System.out.println( "\t returning " + current.getPath() );
			return current;
		}
		else return null;
	}
}
