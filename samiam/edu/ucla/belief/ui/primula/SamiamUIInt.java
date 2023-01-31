package edu.ucla.belief.ui.primula;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
	@author Keith Cascio
	@since 040804
*/
public interface SamiamUIInt
{
	public static final String STR_VALUE_TRUE =		"true";
	public static final String STR_VALUE_FALSE=		"false";
	public static final String STR_KEY_PREFIX =				"DSLx";
	public static final String KEY_EXTRADEFINITION_SETASDEFAULT =		STR_KEY_PREFIX+"EXTRA_DEFINITIONxSETASDEFAULT";
	public static final String KEY_EXTRADEFINITION_DEFAULT_STATE =		STR_KEY_PREFIX+"EXTRA_DEFINITIONxDEFAULT_STATE";

	public Collection getDynamators();
	public void setSystemExitEnabled( boolean flag );
	public boolean isSystemExitEnabled();
	public void clearPrE();
	public void clearCacheFactor();
	//public SamiamPreferences getPackageOptions();
	//public NetworkInternalFrame getActiveHuginNetInternalFrame();
	public void toFront( JInternalFrame frame );
	public void toBack( JInternalFrame frame );
	public void select( JInternalFrame frame );
	//public void setSamiamUserMode( SamiamUserMode mode );
	//public void quickModeWarning( SamiamUserMode mode, NetworkInternalFrame nif );
	public void toggleSamiamUserMode();
	public void toggleReadOnly();
	public boolean newFile();
	public void setLookAndFeel( String classname );
	public void setPkgDspOptLookAndFeel( boolean force );
	//public static void ensureSystemReadyFor( BeliefNetwork newBN );
	public boolean openFile();
	public boolean openFile( File selectedFile );
	public boolean openHuginNet( Reader input, String networkName );
	public boolean openHuginNet( InputStream stream, String networkName );
	public boolean pathConflicts( String selectedPath );
	public boolean closeFilePath( String selectedPath );
	//public Object saveFile( NetworkInternalFrame hnInternalFrame );
	//private void closeFile( NetworkInternalFrame hnInternalFrame );
	public boolean closeAll();
	public void exitProgram();
	public void setDefaultCursor();
	public void setWaitCursor();
	public void setCursor( Cursor curse );
	public void showErrorDialog( Object message );
	public void showMessageDialog( Object message, String title );
	public void showMessageDialog( Object message, String title, int type );
	public int showWarningDialog( Object message, int optionType );
	public void setActiveZoomFactor( double newZoomFactor );
	//public InstantiationClipBoard getInstantiationClipBoard();

	public void setPrimulaUIInstance( PrimulaUIInt ui );
	public JFrame asJFrame();
	public void setInvokerName( String invoker );
	public String getInvokerName();
}
