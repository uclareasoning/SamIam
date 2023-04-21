package edu.ucla.belief.ui.primula;

import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.preference.SamiamPreferences;

//import java.util.*;
//import javax.swing.*;
import java.io.*;

/**
	@author Keith Cascio
	@since 040804
*/
public class PrimulaManager
{
	public static final String STR_DISPLAY_NAME = "Primula";
	public static final String STR_PACKAGENAME_RBNGUI = "RBNgui";
	public static final String STR_CLASSNAME_PRIMULA = "Primula";
	public static final String STR_CLASSNAME_RBNGUI_PRIMULA = STR_PACKAGENAME_RBNGUI+"."+STR_CLASSNAME_PRIMULA;
	public static final String STR_FILENAME_RBNGUI_PRIMULA = STR_CLASSNAME_PRIMULA+".class";
	public static final String STR_FILENAME_JAR_DEFAULT = "primula.jar";
	public static final String STR_TOKEN_USERPRIMULALOCATION = "UserPrimulaLocation";

	public PrimulaManager( UI samiamui )
	{
		mySamIamUI = samiamui;
		myPackage = new SoftwareEntity( STR_DISPLAY_NAME, STR_PACKAGENAME_RBNGUI, STR_CLASSNAME_PRIMULA, STR_FILENAME_JAR_DEFAULT );
		myPackage.setCodeLocation( mySamIamUI.getSamiamPreferences().getFile( STR_TOKEN_USERPRIMULALOCATION ) );

		//SoftwareEntity debugEntity = new SoftwareEntity( "BatchTool", "edu.ucla.belief.batch", "BatchTool", "batch.jar" );

		myRuntimeSoftwareLocationBrowser = new RuntimeSoftwareLocationBrowser( new SoftwareEntity[]{ myPackage }, mySamIamUI );
	}

	/** @since 031405 */
	public void forgetLocation(){
		if( myPrimulaUIInt != null ){
			myPrimulaUIInt.asJFrame().setVisible( false );
			myPrimulaUIInt.asJFrame().dispose();
			myPrimulaUIInt = null;
		}
		myRuntimeSoftwareLocationBrowser.forgetLocations();
		mySamIamUI.getSamiamPreferences().putProperty( STR_TOKEN_USERPRIMULALOCATION, null );
	}

	public void openPrimula() throws UnsatisfiedLinkError
	{
		PrimulaUIInt ui = getPrimulaUIInstance();
		if( ui != null ){
			Util.centerWindow( ui.asJFrame() );
			ui.asJFrame().setVisible( true );
		}
	}

	public PrimulaUIInt getPrimulaUIInstance() throws UnsatisfiedLinkError
	{
		if( myPrimulaUIInt == null ){
			String strErrorMessage = null;
			try{
				Object ui = myRuntimeSoftwareLocationBrowser.getInstance( myPackage );
				if( ui instanceof PrimulaUIInt ){
					myPrimulaUIInt = (PrimulaUIInt) ui;
					myPrimulaUIInt.setSystemExitEnabled( false );
					myPrimulaUIInt.setTheSamIamUI( mySamIamUI );
				}
				else{
					if( ui != null ){
						//analyze( ui );
						strErrorMessage = "Primula class \""+STR_CLASSNAME_RBNGUI_PRIMULA+"\" does not implement interface \"edu.ucla.belief.ui.primula.PrimulaUIInt\".";
					}
					return null;
				}
			}catch( Exception exception ){
				strErrorMessage = exception.getMessage();
			}catch( Error error ){
				strErrorMessage = error.getMessage();
			}finally{
				SamiamPreferences prefs = mySamIamUI.getSamiamPreferences();
				File filePackageCodeLocation = myPackage.getCodeLocation();
				if( prefs.getFile( STR_TOKEN_USERPRIMULALOCATION ) != filePackageCodeLocation ) prefs.putProperty( STR_TOKEN_USERPRIMULALOCATION, filePackageCodeLocation );
				if( strErrorMessage != null ){
					System.err.println( "PrimulaManager.getPrimulaUIInstance() failed because: " + strErrorMessage );
					throw new UnsatisfiedLinkError( strErrorMessage );
				}
			}
		}

		if( myPrimulaUIInt != null ) Util.centerWindow( myPrimulaUIInt.asJFrame() );
		return myPrimulaUIInt;
	}

	public void setPrimulaUIInstance( PrimulaUIInt ui ){
		myPrimulaUIInt = ui;
	}

	private PrimulaUIInt myPrimulaUIInt;
	private UI mySamIamUI;
	private SoftwareEntity myPackage;
	private RuntimeSoftwareLocationBrowser myRuntimeSoftwareLocationBrowser;
}
