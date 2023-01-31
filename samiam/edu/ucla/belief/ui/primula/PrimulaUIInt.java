package edu.ucla.belief.ui.primula;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;

/**
	@author Keith Cascio
	@since 040804
*/
public interface PrimulaUIInt
{
	public void newOrdStruc( int dom );
	public void loadSparseRelFile( File srsfile );
	public void loadRBNFunction( File input_file );
	//public void setRelStruc( SparseRelStruc srel );
	public void setInputFile( File inputFile );
	public void addOrRenameEvidenceModuleNode();
	public void deleteElementFromEvidenceModule( int node );
	public boolean confirm( String text );
	public boolean isInstEmpty();
	public boolean isQueryatomsEmpty();
	public void setStrucEdited( boolean b );

	public void showMessageThis( String message );
	public void appendMessageThis( String message );
	public void setIsBavariaOpenThis( boolean b );
	public void setIsEvModuleOpenThis( boolean b );

	public edu.ucla.belief.ui.primula.SamiamUIInt getSamIamUIInstanceThis();
	public void setTheSamIamUI( edu.ucla.belief.ui.primula.SamiamUIInt ui );
	public String makeAlternateName();
	public void setSystemExitEnabled( boolean flag );
	public boolean isSystemExitEnabled();
	public void exitProgram();
	public JFrame asJFrame();
}
