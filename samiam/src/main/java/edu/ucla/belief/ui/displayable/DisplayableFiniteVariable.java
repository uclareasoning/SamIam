package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.networkdisplay.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.tabledisplay.*;
import edu.ucla.belief.ui.util.JOptionResizeHelper;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;

/**
	* This class will maintain a variable and override the toString function to display the
	* variable as defined in the PackageOptions.
	*
	* It also contains some utility functions for finding Name/Value pairs in Hugin Nodes.
*/
public interface DisplayableFiniteVariable extends Variable, FiniteVariable, HuginNode, DSLNode, ActionListener
{
	public boolean isDSLNode();
	public boolean isHuginNode();

	public NodeLabel getNodeLabel();
	public void setNodeLabel( NodeLabel NL );

	public static final double DOUBLE_ZERO = (double)0;
	public static final double DOUBLE_ONE = (double)1;

	public static final boolean FLAG_IMMUTABLE = false;
	public static boolean FLAG_ALLOW_SUM_GT_ONE = false;
	public static boolean FLAG_ALLOW_NEGATIVE_TABLE_ENTRIES = false;
	public static boolean FLAG_ALLOW_GTONE_TABLE_ENTRIES = true;
	public static boolean FLAG_ERROR_ON_UNFINISHED_EDIT = false;
	public static boolean FLAG_ROUND_COMPLEMENT = true;

	public boolean isUserModified();
	public void showProbabilityEdit( Component parentComponent, double[] newValues );
	public void showNodePropertiesDialog( Component parentComponent, boolean showProbabilitiesImmediately );
	public void showNodePropertiesDialog( Component parentComponent, JOptionResizeHelper.JOptionResizeHelperListener listener );
	public JComponent getProbabilityEditComponent();
	public ProbabilityEditor getProbabilityEditor();
	public boolean isProbabilityEditInProgress();
	public void stopProbabilityEditing();
	public String commitProbabilityChanges();
	public boolean discardProbabilityChanges();
	public NodePropertiesPanel getGUI();
	public boolean isSampleMode();
	public FiniteVariable getSubvariable();
	public InferenceEngine getInferenceEngine();
	public void setNetworkInternalFrame( NetworkInternalFrame doc );
	public NetworkInternalFrame getNetworkInternalFrame();
	public DisplayableBeliefNetwork getBeliefNetwork();
	public Object getObservedValue();
	public int getObservedIndex();
	public void changeDisplayText( boolean dspLabelIfAvail );
	public void changeDisplayText();
	public void changePackageOptions();
	public void updateUI();
}
