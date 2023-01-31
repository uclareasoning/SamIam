package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.tabledisplay.*;
import edu.ucla.belief.ui.actionsandmodes.SamiamUserMode;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.networkdisplay.*;
import edu.ucla.belief.ui.preference.*;
import edu.ucla.belief.ui.event.*;
import edu.ucla.structure.Filter;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import edu.ucla.belief.*;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.belief.sensitivity.ExcludePolicy;
import edu.ucla.belief.decision.*;

/**
	* This class will maintain a variable and override the toString function to display the
	* variable as defined in the PackageOptions.
	*
	* It also contains some utility functions for finding Name/Value pairs in Hugin Nodes.
*/
public class DisplayableFiniteVariableImpl implements DisplayableFiniteVariable, /*CellEditorListener,*/ ChangeListener
{
	public NodeLabel getNodeLabel()
	{
		return nodeLabel;
	}

	public void setNodeLabel( NodeLabel NL )
	{
		nodeLabel = NL;
	}

	public NodeLabel nodeLabel = null;

	//interface Variable
	public EnumValue getProperty( EnumProperty property )
	{
		return myFiniteVariable.getProperty( property );
	}
	public void setProperty( EnumProperty property, EnumValue value )
	{
		//System.out.println( "(DisplayableFiniteVariableImpl)"+this+".setProperty( "+property+", "+value+" )" );
		if( getProperty( property ) != value )
		{
			myFiniteVariable.setProperty( property, value );
			myFlagUserModifiedEnumProperties = true;
			if( hnInternalFrame != null ) hnInternalFrame.fireNodePropertyChangeEvent( new NodePropertyChangeEvent( this, property ) );
		}
	}
	public void delete( EnumProperty property )
	{
		if( getProperty( property ) != null )
		{
			myFiniteVariable.delete( property );
			myFlagUserModifiedEnumProperties = true;
			if( hnInternalFrame != null ) hnInternalFrame.fireNodePropertyChangeEvent( new NodePropertyChangeEvent( this, property ) );
		}
	}
	public Map getEnumProperties()
	{
		return myFiniteVariable.getEnumProperties();
	}
	public String getID()
	{
		return myFiniteVariable.getID();
	}
	public void setID( String id )
	{
		myFiniteVariable.setID( id );
		changeDisplayText();
	}
	public Object instance( String instanceString )
	{
		return myFiniteVariable.instance( instanceString );
	}
	/** @since  20070419 */
	public int grep( Filter filter, Collection results ){
		return myFiniteVariable.grep( filter, results );
	}
	/** @since  20070329 */
	public int grep( Pattern pattern, boolean invert, Collection results ){
		return myFiniteVariable.grep( pattern, invert, results );
	}
	/** @since  20070329 */
	public int grep( Matcher matcher, boolean invert, Collection results ){
		return myFiniteVariable.grep( matcher, invert, results );
	}
	/**
		Warning: this method returns a clone of the subvariable.
		@ret An Object of type FiniteVariable.
	*/
	public Object clone()
	{
		return myFiniteVariable.clone();
	}
	public Object getUserObject()
	{
		return myFiniteVariable.getUserObject();
	}
	public void setUserObject( Object obj )
	{
		myFiniteVariable.setUserObject( obj );
	}
	/**
		@since 111402
	*/
	public int compareTo(Object o)
	{
		//return myFiniteVariable.compareTo( o );
		return VariableImpl.theCollator.compare( this.toString(), o.toString() );
	}

	//interface FiniteVariable
	public int size()
	{
		if( myFlagSampleMode ) return (int)2;
		else return myFiniteVariable.size();
	}
	public Object instance( int index )
	{
		return myFiniteVariable.instance( index );
	}
	public int index(Object instance)
	{
		return myFiniteVariable.index( instance );
	}
	public boolean contains( Object instance )
	{
		return myFiniteVariable.contains( instance );
	}
	public java.util.List instances()
	{
		return myFiniteVariable.instances();
	}
	public Object set( int index, Object objNew )
	{
		return myFiniteVariable.set( index, objNew );
	}
	public boolean insert( int index, Object instance )
	{
		return myFiniteVariable.insert( index, instance );
	}
	public Object remove( int index )
	{
		return myFiniteVariable.remove( index );
	}
	public CPTShell getCPTShell(){
		return myFiniteVariable.getCPTShell();
	}
	public void setCPTShell( CPTShell cpt ){
		myFiniteVariable.setCPTShell( cpt );
		myFlagUserModifiedProbabilities = true;
	}
	public CPTShell getCPTShell( DSLNodeType type ){
		return myFiniteVariable.getCPTShell( type );
	}
	public void setCPTShell( DSLNodeType type, CPTShell shell ){
		myFiniteVariable.setCPTShell( type, shell );
	}
	public DSLNodeType getDSLNodeType(){
		return myFiniteVariable.getDSLNodeType();
	}
	public void setDSLNodeType( DSLNodeType newVal ){
		myFiniteVariable.setDSLNodeType( newVal );
	}

	//interface StandardNode
	public boolean[] getExcludeArray()
	{
		if( myFlagIsStandardNode ) return myStandardNode.getExcludeArray();
		else return null;
	}
	public void setExcludeArray( boolean[] xa )
	{
		if( myFlagIsStandardNode )
		{
			myStandardNode.setExcludeArray( xa );
			myFlagUserModifiedExcludeArray = true;
		}
	}
	public Dimension getDimension( Dimension d )
	{
		if( myFlagIsStandardNode ) return  myStandardNode.getDimension( d );
		else return new Dimension(0,0);
	}
	public void setDimension( Dimension d )
	{
		if( myFlagIsStandardNode ) myStandardNode.setDimension( d );
	}
	public Point getLocation( Point ret )
	{
		if( myFlagIsStandardNode ) return  myStandardNode.getLocation( ret );
		else return new Point(0,0);
	}
	public void setLocation( Point newLoc )
	{
		if( myFlagIsStandardNode ) myStandardNode.setLocation( newLoc );
	}
	public String getLabel()
	{
		if( myFlagIsStandardNode ) return  myStandardNode.getLabel();
		else return "";
	}
	public void setLabel( String newVal )
	{
		if( myFlagIsStandardNode )
		{
			myStandardNode.setLabel( newVal );
			changeDisplayText();
		}
	}
	public ExcludePolicy getExcludePolicy()
	{
		if( myFlagIsStandardNode ) return myStandardNode.getExcludePolicy();
		else return ExcludePolicy.INCLUDE;
	}
	public void setExcludePolicy( ExcludePolicy ep )
	{
		if( myFlagIsStandardNode ) myStandardNode.setExcludePolicy( ep );
	}
	public DiagnosisType getDiagnosisType()
	{
		if( myFlagIsStandardNode ) return myStandardNode.getDiagnosisType();
		else return DiagnosisType.AUXILIARY;
	}
	public void setDiagnosisType( DiagnosisType newVal )
	{
		if( myFlagIsStandardNode ) myStandardNode.setDiagnosisType( newVal );
	}
	public boolean isMAPVariable()
	{
		if( myFlagIsStandardNode ) return myStandardNode.isMAPVariable();
		else return false;
	}
    public boolean isSDPVariable()
	{
		if( myFlagIsStandardNode ) return myStandardNode.isSDPVariable();
		else return false;
	}
	public void setMAPVariable( boolean flag )
	{
		if( myFlagIsStandardNode ) myStandardNode.setMAPVariable( flag );
	}
    public void setSDPVariable( boolean flag )
    {
        if( myFlagIsStandardNode ) myStandardNode.setSDPVariable( flag );
    }
	/** @since 20060704 */
	public java.util.Map getProperties(){
		if( myFlagIsStandardNode ) return myStandardNode.getProperties();
		else return null;
	}

	//interface HuginNode
	public int getValueType()
	{
		if( myFlagIsHuginNode ) return myHuginNode.getValueType();
		else return HuginNode.DISCRETE;
	}
	public int getNodeType()
	{
		if( myFlagIsHuginNode ) return myHuginNode.getNodeType();
		else return HuginNode.NODE;
	}
	public boolean isSpecifiedDimension()
	{
		if( myFlagIsHuginNode ) return myHuginNode.isSpecifiedDimension();
		else return false;
	}
	public void resetSpecifiedDimension()
	{
		if( myFlagIsHuginNode ) myHuginNode.resetSpecifiedDimension();
	}

	//interface DSLNode
	public DSLSubmodel getDSLSubmodel()
	{
		if( myFlagIsDSLNode ) return myDSLNode.getDSLSubmodel();
		else return null;
	}
	public void setDSLSubmodel( DSLSubmodel model )
	{
		if( myFlagIsDSLNode ) myDSLNode.setDSLSubmodel( model );
	}
	/*
	public List getNoisyOrWeights()
	{
		if( myFlagIsDSLNode ) return myDSLNode.getNoisyOrWeights();
		else return null;
	}*/
	public Boolean getMandatory()
	{
		if( myFlagIsDSLNode ) return myDSLNode.getMandatory();
		else return new Boolean( false );
	}
	public void setMandatory( Boolean newVal )
	{
		if( myFlagIsDSLNode ) myDSLNode.setMandatory( newVal );
	}
	public Boolean getRanked()
	{
		if( myFlagIsDSLNode ) return myDSLNode.getRanked();
		else return new Boolean( false );
	}
	public void setRanked( Boolean newVal )
	{
		if( myFlagIsDSLNode ) myDSLNode.setRanked( newVal );
	}
	public List getTargetList()
	{
		if( myFlagIsDSLNode ) return myDSLNode.getTargetList();
		else return null;
	}
	public void setTargetList( List newVal )
	{
		if( myFlagIsDSLNode ) myDSLNode.setTargetList( newVal );
	}
	/** Moved to StandardNode 041304 */
	public Integer getDefaultStateIndex()
	{
		//if( myFlagIsDSLNode ) return myDSLNode.getDefaultStateIndex();
		if( myFlagIsStandardNode ) return myStandardNode.getDefaultStateIndex();
		else return null;
	}
	public void setDefaultStateIndex( Integer newVal )
	{
		if( myFlagIsDSLNode ) myDSLNode.setDefaultStateIndex( newVal );
	}

	protected boolean myFlagIsHuginNode = false;
	private HuginNode myHuginNode;
	protected boolean myFlagIsDSLNode = false;
	private DSLNode myDSLNode;
	protected boolean myFlagIsStandardNode = false;
	private StandardNode myStandardNode;

	public boolean isDSLNode()
	{
		return myFlagIsDSLNode;
	}
	public boolean isHuginNode()
	{
		return myFlagIsHuginNode;
	}
	public boolean isStandardNode()
	{
		return myFlagIsStandardNode;
	}

	protected static boolean FLAG_DEBUG_BUTTONS = Util.DEBUG;

	/**
		@author Keith Cascio
		@since 052802
	*/
	public boolean isUserModified()
	{
		return myFlagUserModifiedProbabilities || myFlagUserModifiedEnumProperties || myFlagUserModifiedExcludeArray;
	}

	/**
	* This should only be used by the PackageOptions.  All other uses of it
	* will cause problems.  Using this constructor places the object in
	* myFlagSampleMode which limits its functionality severly.
	*/
	public DisplayableFiniteVariableImpl( String txt) {
		myFlagSampleMode = true;
		dspTxt = txt;
	}

	/**
		@author Keith Cascio
		@since 071102
	*/
	public DisplayableFiniteVariableImpl(	FiniteVariable v,
						NetworkInternalFrame doc )
	{
		this( v );
		setNetworkInternalFrame( doc );
	}

	/**
		@author Keith Cascio
		@since 111802
	*/
	public DisplayableFiniteVariableImpl( FiniteVariable v )
	{
		myFiniteVariable = v;
		if( v instanceof DisplayableFiniteVariable ) myFiniteVariable = ((DisplayableFiniteVariable)v).getSubvariable();

		myFlagIsHuginNode = ( myFiniteVariable instanceof HuginNode );
		if( myFlagIsHuginNode ) myHuginNode = (HuginNode) myFiniteVariable;
		myFlagIsDSLNode = ( myFiniteVariable instanceof DSLNode );
		if( myFlagIsDSLNode ) myDSLNode = (DSLNode) myFiniteVariable;
		myFlagIsStandardNode = ( myFiniteVariable instanceof StandardNode );
		if( myFlagIsStandardNode ) myStandardNode = (StandardNode) myFiniteVariable;
	}

	/**
		@author Keith Cascio
		@since 111802
	*/
	public void setNetworkInternalFrame( NetworkInternalFrame doc )
	{
		hnInternalFrame = doc;

		if( hnInternalFrame != null )
		{
			//PreferenceGroup globalPrefs = .getPreferenceGroup( SamiamPreferences.PkgDspNme );
			myFlagDisplayNodeLabelIfAvail = ((Boolean) hnInternalFrame.getPackageOptions().getMappedPreference( SamiamPreferences.displayNodeLabelIfAvail ).getValue()).booleanValue();
		}

		changeDisplayText( myFlagDisplayNodeLabelIfAvail );
	}

	/** @author Keith Cascio
		@since 052202 */
	public void showProbabilityEdit( Component parentComponent, double[] newValues )
	{
		JComponent compEdit = getProbabilityEditComponent();

		if( compEdit != null ) showNodePropertiesDialog( compEdit, parentComponent, true );
	}

	/** @author Keith Cascio
		@since 041702 */
	public synchronized void showNodePropertiesDialog( Component parentComponent, boolean showProbabilitiesImmediately )
	{
		showNodePropertiesDialog( parentComponent, showProbabilitiesImmediately, (JOptionResizeHelper.JOptionResizeHelperListener)null );
	}

	/** @author Keith Cascio
		@since 011205 */
	public synchronized void showNodePropertiesDialog( Component parentComponent, boolean showProbabilitiesImmediately, JOptionResizeHelper.JOptionResizeHelperListener listener ){
		if( !myFlagNodePropertiesDialogShowing )
		{
			JComponent editComponent = getProbabilityEditComponent();
			if( listener != null ){
				JOptionResizeHelper helper = new JOptionResizeHelper( editComponent, true, (long)10000 );
				helper.setListener( listener );
				helper.start();
			}
			showNodePropertiesDialog( editComponent, parentComponent, showProbabilitiesImmediately );
		}
	}

	/** @author Keith Cascio
		@since 011205 */
	public void showNodePropertiesDialog( Component parentComponent, JOptionResizeHelper.JOptionResizeHelperListener listener ){
		showNodePropertiesDialog( parentComponent, false, listener );
	}

	/** @author Keith Cascio
		@since 120804 */
	public synchronized JComponent createNodePropertiesComponentDebug( JComponent comp, String tabtitle ){
		if( myFlagNodePropertiesDialogShowing ) return (JComponent)null;
		myDebugEditComponent = getProbabilityEditComponent();
		myDebugPnlResize = showNodePropertiesPre( getProbabilityEditComponent(), false, comp, tabtitle );
		return myDebugPnlResize;
	}

	private JComponent myDebugEditComponent;
	private JComponent myDebugPnlResize;

	/** @author Keith Cascio
		@since 120804 */
	public synchronized void showDebug( Component parentComponent ){
		showNodePropertiesPost( parentComponent, myDebugPnlResize, myDebugEditComponent, true );
	}

	transient protected boolean myFlagNodePropertiesDialogShowing = false;
	transient protected JTabbedPane myJTabbedPane = null;
	public static final int INT_INDEX_TAB_PROBABILITY = (int)1;

	/** @author Keith Cascio
		@since 101102 */
	protected void setProbabilityTabEnabled( boolean enabled )
	{
		myJTabbedPane.setEnabledAt( INT_INDEX_TAB_PROBABILITY, enabled );
	}

	/** @author Keith Cascio
		@since 041702 */
	private void showNodePropertiesDialog( JComponent editComponent, Component parentComponent, boolean showProbabilitiesImmediately )
	{
		showNodePropertiesPost( parentComponent, showNodePropertiesPre( editComponent, showProbabilitiesImmediately, (JComponent)null, (String)null ), editComponent, false );
	}

	/** @author Keith Cascio
		@since 120804 */
	private JComponent showNodePropertiesPre( JComponent editComponent, boolean showProbabilitiesImmediately, JComponent compDebug, String tabdebug )
	{
		setEditFlags( false );

		JComponent theGUI = getGUI();
		SingleVarEnumEditPanel sveep = getEnumEditPanel();
		Dimension theGUISize = theGUI.getPreferredSize();
		Dimension editComponentSize = editComponent.getPreferredSize();
		Dimension uiSize = hnInternalFrame.getParentFrame().getSize();

		//System.out.println( "    editComponentSize: " + editComponentSize );

		SamiamUserMode SUM = hnInternalFrame.getSamiamUserMode();
		boolean isEditable = SUM.contains( SamiamUserMode.EDIT ) && !SUM.contains( SamiamUserMode.SMILEFILE );
		String strTitlePropsTab = "Properties";
		//if( !isEditable ) strTitlePropsTab += " (Immutable)";

		final JTabbedPane tp = myJTabbedPane = new JTabbedPane();
		tp.add( theGUI,		strTitlePropsTab );
		tp.add( editComponent,	"Probabilities" );
		tp.add( sveep, "Attributes" );
		if( getCPTShell( DSLNodeType.DECISIONTREE ) != null ){
			DecisionShell shell = (DecisionShell) getCPTShell( DSLNodeType.DECISIONTREE );
			addDecisionTreeTab( shell, false );
		}
		//if( UI.DEBUG ) tp.add( new DecisionTreeEditPanel( new DisplayableDecisionTree( this.getCPTShell( DSLNodeType.CPT ).index() ) ), "Decision Tree" );
		if( compDebug != null ){
			tp.add( compDebug, tabdebug );
			tp.setSelectedComponent( compDebug );
		}
		else if( showProbabilitiesImmediately ) tp.setSelectedComponent( editComponent );
		else tp.setSelectedComponent( theGUI );

		Dimension tabSize = tp.getUI().getTabBounds(tp,0).getSize();

		final JPanel pnlResize = new JPanel();
		pnlResize.add( tp );
		pnlResize.addComponentListener( new ResizeAdapter( tp ) );
		//pnlResize.setBorder(BorderFactory.createLineBorder(Color.black));//debug

		int WIDTH = Math.max( theGUISize.width, editComponentSize.width );
		WIDTH = Math.min( WIDTH + 16, uiSize.width - 64 );
		//int WIDTH = theGUISize.width;
		int HEIGHT = Math.max( theGUISize.height, editComponentSize.height );
		HEIGHT = Math.min( HEIGHT + tabSize.height + 8, uiSize.height - 150 );
		tp.setPreferredSize( new Dimension( WIDTH, HEIGHT ) );

		//System.out.println( "    tp.setPreferredSize( " + new Dimension( WIDTH, HEIGHT ) + " )" );
		//System.out.println( "    tabSize: " + tabSize );

		return pnlResize;
	}

	/** @since  20041208 */
	private void showNodePropertiesPost( Component parentComponent, JComponent pnlResize, JComponent editComponent, boolean neuter ){
		//setSamiamUserMode( SUM );
		//Util.setParentJDialogResizable( pnlResize, true, (long)10000 );
		JOptionResizeHelper helper = new JOptionResizeHelper( pnlResize, true, (long)10000 );
		helper.setListener( (TableStyleEditor) getProbabilityEditor() );
		helper.start();
		myFlagNodePropertiesDialogShowing = true;
		int result = showConfirmDialog(	parentComponent, pnlResize );
		myFlagNodePropertiesDialogShowing = false;

		if( neuter ){
			discardProbabilityChanges();
			return;
		}

		boolean success = false;
		String errmsg = null;
		String proberrmsg = null;
		try{
			//if( hnInternalFrame.getSamiamUserMode().contains( SamiamUserMode.EDIT ) )
			//{
			while( result == JOptionPane.OK_OPTION && !success )
			{
				if( isProbabilityEditInProgress() )
				{
					if( FLAG_ERROR_ON_UNFINISHED_EDIT ) errmsg = "Probability edit in progress.";
					else stopProbabilityEditing();
				}

				if( errmsg == null ) errmsg = proberrmsg = commitProbabilityChanges();

				if( errmsg == null ) errmsg = commitDecisionTreeChanges();

				if( errmsg == null )
				{
					stopStatesEditing();
					errmsg = commitPropertyChanges();
				}

				if( errmsg == null ) mySingleVarEnumEditPanel.commit();

				if( errmsg == null )
				{
					myFlagUserModifiedProbabilities = success = true;
					//hnInternalFrame.cptChanged();
					//hnInternalFrame.setCPT( myFiniteVariable,
				}
				else
				{
					success = false;
					JOptionPane.showMessageDialog( parentComponent, errmsg, "Input Error", JOptionPane.ERROR_MESSAGE );
					errmsg = null;

					if( proberrmsg != null ) myJTabbedPane.setSelectedComponent( editComponent );
					proberrmsg = null;

					myFlagNodePropertiesDialogShowing = true;
					result = showConfirmDialog( parentComponent, pnlResize );
					myFlagNodePropertiesDialogShowing = false;
				}
			}
			//}
		}catch( Exception e ){
			System.err.println( "Warning: DisplayableFiniteVariableImpl.showNodePropertiesPost() caught " + e );
			if( Util.DEBUG_VERBOSE ){
				Util.STREAM_VERBOSE.println( Util.STR_VERBOSE_TRACE_MESSAGE );
				e.printStackTrace( Util.STREAM_VERBOSE );
			}
			success = false;
			myFlagNodePropertiesDialogShowing = false;
			String msg = e.getMessage();
			if( msg == null ) msg = e.toString();
			msg = "Error saving changes.  Some changes might have been lost.\n" + msg;
			if( errmsg != null ) msg += "\n" + errmsg;
			JOptionPane.showMessageDialog( parentComponent, msg, "Error Saving Changes", JOptionPane.ERROR_MESSAGE );
		}finally{
			discardProbabilityChanges();
			if( success ) hnInternalFrame.getTreeScrollPane().refreshPropertyChanges();

			myNodePropertiesPanel = null;
		}
	}

	/**
		@author Keith Cascio
		@since 071102
	*/
	protected int showConfirmDialog( Component parent, Component message )
	{
		return JOptionPane.showConfirmDialog(	parent,
							message,
							toString() + " Properties",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE );
	}

	/**
		Request a JComponent that can be used to
		edit the probabilities or weights for
		this variable.

		@author Keith Cascio
		@since 041702
		@see  edu.ucla.belief.ui.util.DisplayableFiniteVariable#discardProbabilityChanges() discardProbabilityChanges()
	*/
	public JComponent getProbabilityEditComponent()
	{
		if( myProbabilityEditComponent == null ) myProbabilityEditComponent = makeProbabilityEditComponent();
		return myProbabilityEditComponent;
	}

	transient private JComponent myProbabilityEditComponent = null;

	/**
		Find out whether a probability edit
		is still in progress in the edit
		component returned by the last call
		to getProbabilityEditComponent().

		@author Keith Cascio
		@since 041802
	*/
	public boolean isProbabilityEditInProgress()
	{
		return getProbabilityEditor().isProbabilityEditInProgress();
	}

	/**
		@author Keith Cascio
		@since 051602
	*/
	public void stopProbabilityEditing()
	{
		getProbabilityEditor().stopProbabilityEditing();
	}

	/** @since 051602 */
	public void stopStatesEditing(){
		if( myNodePropertiesPanel != null ) myNodePropertiesPanel.stopStatesEditing();
	}

	/**
		@author Keith Cascio
		@since 061303
	*/
	public ProbabilityEditor getProbabilityEditor()
	{
		if( myProbabilityEditor == null ) myProbabilityEditor = getAProbabilityEditor();
		return myProbabilityEditor;
	}

	/**
		@author Keith Cascio
		@since 093003
	*/
	public ProbabilityEditor getAProbabilityEditor()
	{
		DSLNodeType type = getDSLNodeType();
		if( type == DSLNodeType.CPT || type == DSLNodeType.TRUTHTABLE ) return new CPTEditor( this, hnInternalFrame );
		else if( type == DSLNodeType.NOISY_OR ) return new PearlEditor( this, hnInternalFrame );
		else return null;
	}

	/** @since 011005 */
	public String commitDecisionTreeChanges()
	{
		boolean flagsetcpt = false;

		if( myUncommitedDecisionShell != null ){
			DecisionTree dt = myUncommitedDecisionShell.getDecisionTree();
			if( dt.getSnapshot() != null ){
				flagsetcpt = true;
				dt.setSnapshot( null );
			}

			if( getCPTShell( DSLNodeType.DECISIONTREE ) != myUncommitedDecisionShell ){
				setCPTShell( DSLNodeType.DECISIONTREE, myUncommitedDecisionShell );
				flagsetcpt = true;
			}

			myUncommitedDecisionShell = null;
		}

		if( flagsetcpt ) hnInternalFrame.setCPT( (FiniteVariable)this );

		return (String)null;
	}

	/** @since 041702 */
	public String commitProbabilityChanges()
	{
		//if( !myFlagProbability ) return null;

		//System.out.println( "DFV.commitProbabilityChanges()" );

		boolean success = true;
		boolean dataChanged = false;
		String errmsg = null;
		if( myProbabilityEditor != null )
		{
			if( myProbabilityEditor.isProbabilityEdited() )
			{
				errmsg = myProbabilityEditor.commitProbabilityChanges();
				dataChanged = success = (errmsg == null);
			}

			if( success && myProbabilityEditor.isExcludeDataEdited() )
			{
				errmsg = myProbabilityEditor.commitExcludeDataChanges();
				dataChanged |= success &= (errmsg == null);
			}
		}
		else
		{
			System.err.println( "Java warning in commitProbabilityChanges(): null ProbabilityEditor." );
			success = false;
		}

		if( success )
		{
			if( dataChanged ) discardProbabilityChanges();
			return null;
		}
		else
		{
			if( errmsg == null ) return "Error in commitProbabilityChanges()";
			else return errmsg;
		}
	}

	/**
		Request that any changes a user has made to the
		probabilities using the edit component
		returned in a previous call to getProbabilityEditComponent()
		bt discarded.

		@author Keith Cascio
		@since 041702
		@see  edu.ucla.belief.ui.util.DisplayableFiniteVariable#getProbabilityEditComponent() getProbabilityEditComponent()
	*/
	public boolean discardProbabilityChanges()
	{
		getProbabilityEditor().discardChanges();
		myProbabilityEditComponent = null;

		if( (myUncommitedDecisionShell != null) && (getCPTShell( DSLNodeType.DECISIONTREE ) == myUncommitedDecisionShell) ){
			myUncommitedDecisionShell.getDecisionTree().restoreSnapshot();
		}
		myUncommitedDecisionShell = null;

		return true;
	}

	/**
		Copied from EditCPTDialog.java
		@since 041702
	*/
	public static String ensureCPTProperty( FiniteVariable var,
						double[] values, double[] newValues )
	{
		double sum = DOUBLE_ZERO;
		for ( int i = 0; i < values.length; i += var.size() )
		{
			sum = DOUBLE_ZERO;
			for (int j = i; j < i + var.size(); j++)
			{
				if( !FLAG_ALLOW_NEGATIVE_TABLE_ENTRIES && values[j] < DOUBLE_ZERO ) return "Probability out of range: negative.";
				if( !FLAG_ALLOW_GTONE_TABLE_ENTRIES && DOUBLE_ONE < values[j] ) return "Probability out of range: > 1.";
				sum += values[j];
			}
			if ( sum <= DOUBLE_ZERO )
			{
				 //System.out.println( "Java ensureCPTProperty() sum <= DOUBLE_ZERO" );//debug
				//return false;
				return "Probabilities out of range: SUM <= 0";
			}
			else if( !FLAG_ALLOW_SUM_GT_ONE && DOUBLE_ONE < sum )
			{
				//System.out.println( "Java ensureCPTProperty() DOUBLE_ONE < sum == " + sum );//debug
				//return false;
				return "Probabilities out of range: SUM > 1";
			}
			for (int j = i; j < i + var.size(); j++) newValues[j] = values[j] / sum;
		}
		//return false;
		return null;
	}

	/** @since 041702 */
	private JComponent makeProbabilityEditComponent()
	{
		TableStyleEditor editor = (TableStyleEditor) getProbabilityEditor();
		JComponent ret = editor.makeProbabilityEditComponent();
		//myTempCellEditorProbabilityTable = editor.getCellEditor();
		return ret;
	}

	/** @since 061902 */
	/*
	protected void setSamiamUserMode( SamiamUserMode newMode ){
		boolean editable = newMode.contains( SamiamUserMode.EDIT );
		btnNormalize.setEnabled( editable );
		btnComplement.setEnabled( editable );
		if( myTempWrapper != null ) myTempWrapper.model.setEditable( editable );
	}*/

	/** @since 050802 */
	public void actionPerformed( ActionEvent e )
	{//work moved to NodePropertiesPanel
	}

	/** @since 030102 */
	public NodePropertiesPanel getGUI(){
		if( myNodePropertiesPanel == null ) myNodePropertiesPanel = new NodePropertiesPanel( this );
		return myNodePropertiesPanel;
	}

	public SingleVarEnumEditPanel getEnumEditPanel()
	{
		if( mySingleVarEnumEditPanel == null ) mySingleVarEnumEditPanel = new SingleVarEnumEditPanel( hnInternalFrame.getBeliefNetwork(), this );
		mySingleVarEnumEditPanel.reInitialize( hnInternalFrame.getSamiamUserMode() );
		return mySingleVarEnumEditPanel;
	}

	/** interface DisplayableFiniteVariable */
	public void updateUI(){
		mySingleVarEnumEditPanel = null;
	}

	//transient protected JComponent myGUI = null;
	transient protected SingleVarEnumEditPanel mySingleVarEnumEditPanel;
	//static protected final int INT_HORIZONTAL_CELLPADDING = 4;
	transient protected JPanel pnlFlags = null;
	//transient protected JPanel pnlButtons = null;

	//transient protected JTextField myTfIdentifier = null;
	//transient protected JTextField myTfName = null;
	//transient protected JComboBox myComboPolicy;
	////transient protected CellEditor myTempCellEditorProbabilityTable = null;
	//transient protected StatesTableModel myStatesTableModel = null;
	//transient protected JTable myJTableStates = null;
	//transient protected CellEditor myTempCellEditorStatesTable = null;
	//transient protected JCheckBox myCbMAP;

	//transient protected boolean myFlagIdentifier = false;
	//transient protected boolean myFlagName = false;
	//transient protected boolean myFlagPolicy = false;
	//transient protected boolean myFlagProbability = false;
	//transient protected boolean myFlagStateNames = false;
	//transient protected boolean myFlagStateList = false;
	transient protected boolean myFlagMAPFlagEdited = false;

	/** @since 101002 */
	protected String commitPropertyChanges(){
		if( myNodePropertiesPanel != null ) return myNodePropertiesPanel.commitPropertyChanges();
		else return null;
	}

	/** @since 101002 */
	protected void setEditFlags( boolean setting ){
		if( myNodePropertiesPanel != null ) myNodePropertiesPanel.setEditFlags( setting );
	}

	//transient protected JButton myButtonInsertState = null;
	//transient protected JButton myButtonDeleteState = null;

	/** @since 011205 */
	public boolean setRepresentationProperty( DSLNodeType representation ){
		if( myNodePropertiesPanel != null ) return myNodePropertiesPanel.setRepresentationProperty( representation );
		else return false;
	}

	/** @since 010905 */
	public void addDecisionTreeTab( DecisionShell shell, boolean select )
	{
		myUncommitedDecisionShell = shell;
		myJTabbedPane.add( myDummy = new JPanel(), "Decision Tree" );
		if( select ){
			validateDecisionTreeTab();
			myJTabbedPane.setSelectedComponent( myDecisionTreeEditPanel );
		}
		myJTabbedPane.addChangeListener( (ChangeListener)this );
	}

	/** interface ChangeListener
		@since 010905 */
	public void stateChanged( ChangeEvent e ){
		Object src = e.getSource();
		if( (src == myJTabbedPane) && (myDummy != null) ){
			if( myJTabbedPane.getSelectedIndex() == myJTabbedPane.indexOfComponent( myDummy ) ){
				validateDecisionTreeTab();
			}
		}
	}

	/** @since 010905 */
	private void validateDecisionTreeTab()
	{
		if( myDummy == null ) return;
		int index = myJTabbedPane.indexOfComponent( myDummy );
		if( index >= 0 ){
			//System.out.println( "DFVI.validateDecisionTreeTab() creating DecisionTreeEditPanel" );
			DisplayableDecisionTree ddt = new DisplayableDecisionTree( (DecisionTreeImpl) myUncommitedDecisionShell.getDecisionTree() );
			myDecisionTreeEditPanel = new DecisionTreeEditPanel( ddt, ddt.getFactory() );
			myJTabbedPane.setComponentAt( index, myDecisionTreeEditPanel );
			myDummy = null;
		}
		myDecisionTreeEditPanel.setDividerLocationLater( (double)0.5 );
	}

	public String toString() { return dspTxt; }
	public boolean isSampleMode() { return myFlagSampleMode; }

	public FiniteVariable getSubvariable() {
		if( myFlagSampleMode ) {
			throw( new UnsupportedOperationException("ERROR: Sample Mode does not support getSubvariable()"));
		}
		return myFiniteVariable;
	}

	public InferenceEngine getInferenceEngine() {
		if( myFlagSampleMode ) {
			throw( new UnsupportedOperationException("ERROR: Sample Mode does not support getInferenceEngine()"));
		}
		return hnInternalFrame.getInferenceEngine();
	}

	public NetworkInternalFrame getNetworkInternalFrame() {
		return hnInternalFrame;
	}

	public DisplayableBeliefNetwork getBeliefNetwork(){
		return ( hnInternalFrame == null ) ? null : hnInternalFrame.getBeliefNetwork();
	}

	/**
		@author Keith Cascio
		@since 070703
	*/
	public Object getObservedValue()
	{
		BeliefNetwork bn = getBeliefNetwork();
		if( bn != null )
		{
			EvidenceController ec = bn.getEvidenceController();
			if( ec != null )
			{
				return ec.getValue( this );
			}
		}
		return null;
	}

	/**
		@author Keith Cascio
		@since 070703
	*/
	public int getObservedIndex()
	{
		Object value = getObservedValue();
		return ( value == null ) ? -1 : index( value );
	}

	/** Allow options to change.*/
	public void changeDisplayText( boolean dspLabelIfAvail )
	{
		if( !myFlagSampleMode )
		{
			if( dspLabelIfAvail )
			{
				dspTxt = getLabel();
				if( dspTxt == null || (dspTxt.length() < (int)1) ) dspTxt = getID();
			}
			else dspTxt = getID();
		}
	}

	/**
		@author Keith Cascio
		@since 101002
	*/
	public void changeDisplayText()
	{
		changeDisplayText( myFlagDisplayNodeLabelIfAvail );
	}
	boolean myFlagDisplayNodeLabelIfAvail = true;

	/**
		Allow options to change.
	*/
	public void changePackageOptions()
	{
		if( !myFlagSampleMode)
		{
			Preference displayNodeLabelIfAvail = hnInternalFrame.getPackageOptions().getMappedPreference( SamiamPreferences.displayNodeLabelIfAvail );
			if( displayNodeLabelIfAvail.isRecentlyCommittedValue() )
			{
				myFlagDisplayNodeLabelIfAvail = ((Boolean) displayNodeLabelIfAvail.getValue()).booleanValue();
				changeDisplayText( myFlagDisplayNodeLabelIfAvail );
			}
		}
	}

	protected FiniteVariable myFiniteVariable = null;
	protected ProbabilityEditor myProbabilityEditor;
	protected String dspTxt = null;
	protected NetworkInternalFrame hnInternalFrame = null;
	protected boolean myFlagUserModifiedProbabilities = false;
	protected boolean myFlagUserModifiedEnumProperties = false;
	protected boolean myFlagUserModifiedExcludeArray = false;
	//transient private JComboBox myCboRepresentation;
	//transient private DSLNodeType myUncommitedRepresentation;
	//transient boolean myFlagUncommitedRepresentation = false;
	transient DecisionShell myUncommitedDecisionShell;
	private JComponent myDummy;
	private DecisionTreeEditPanel myDecisionTreeEditPanel;

	//Very limited usage by preferences
	protected boolean myFlagSampleMode = false;

	private NodePropertiesPanel myNodePropertiesPanel;
}
