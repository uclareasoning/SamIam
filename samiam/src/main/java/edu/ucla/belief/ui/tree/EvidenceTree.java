package edu.ucla.belief.ui.tree;

import edu.ucla.belief.ui.event.*;
import edu.ucla.belief.ui.util.*;
import edu.ucla.belief.ui.displayable.*;
import edu.ucla.belief.ui.networkdisplay.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.preference.*;

import edu.ucla.belief.*;
import edu.ucla.util.EnumProperty;
import edu.ucla.util.EnumValue;
import edu.ucla.util.EvidenceAssertedProperty;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;

import java.util.List;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;

/**
<p>
	A tree that is convenient for asserting evidence, that shows
	the hierarchy of submodels, and can be sorted according to
	an enumerated variable property.
<p>
	Changed by Keith Cascio 030602
*/
public class EvidenceTree extends JTree implements NodePropertyChangeListener,EvidenceChangeListener,RecompilationListener,CPTChangeListener,MouseListener
{
	private          NetworkInternalFrame  hnInternalFrame;
	private          EvidenceController    myEvidenceController;
	protected        SamiamPreferences     myTreePrefs;
	private          int                   myIntDebug;
	private  static  int                   INT_DEBUG_COUNTER = 1;

	public EvidenceTree( NetworkInternalFrame hnInternalFrame,
	                     SamiamPreferences    treePrefs )
	{
		this( hnInternalFrame, treePrefs, null );
	}

	public EvidenceTree( NetworkInternalFrame hnInternalFrame,
	                     SamiamPreferences    treePrefs,
	                     EnumProperty         property )
	{
		this.hnInternalFrame      = hnInternalFrame;
		this.myTreePrefs          = treePrefs;
		this.myEnumProperty       = property;
		this.myEvidenceController = hnInternalFrame.getBeliefNetwork().getEvidenceController();
		hnInternalFrame.addRecompilationListener(       this );
		hnInternalFrame.addCPTChangeListener(           this );
		hnInternalFrame.addNodePropertyChangeListener(  this );

		myEvidenceController.addEvidenceChangeListener( this );

		if( myEnumProperty == null && treePrefs != null )
		{
			Preference treeSortDefault = treePrefs.getMappedPreference( SamiamPreferences.treeSortDefault );
			if( treeSortDefault != null ) myEnumProperty = (EnumProperty) treeSortDefault.getValue();
		}

		this.myPropertyChangeVariables = new HashSet( hnInternalFrame.getBeliefNetwork().size() );

		init();
	}

	/** @since 20040712 */
	public void die()
	{ try{
		this.setEditable(         false );
		this.setLargeModel(       false );
		this.setRootVisible(      false );
		this.setRowHeight(            1 );
		this.setScrollsOnExpand(  false );
		this.setShowsRootHandles( false );
		this.setToggleClickCount(  0x10 );
		this.setEnabled(          false );
		this.setModel( new DefaultTreeModel( new DefaultMutableTreeNode( "dead" ) ) );
		this.removeAll();

		myIntDebug = (int)0 - myIntDebug;
		myEvidenceController.removeEvidenceChangeListener( this );
		hnInternalFrame.removeNodePropertyChangeListener(  this );
		hnInternalFrame.removeRecompilationListener(       this );
		hnInternalFrame.removeCPTChangeListener(           this );
		this.setSelectionManaged( false );
		MouseListener[] mls = this.getMouseListeners();
		for( int i=0; i<mls.length; i++ ){ this.removeMouseListener( mls[i] ); }

		this.myEnumValueNodes             = null;
		this.myVariableNodes              = null;
		this.mySubmodelNodes              = null;
		this.myPropertyChangeVariables    = null;
		this.myUserobjectSubmodels        = null;
		this.myWarning                    = null;
		this.myEnumProperty               = null;
		this.mainSubmodel                 = null;
		this.myTreeCellRenderer           = null;
		this.myDefaultTreeModel           = null;
		this.myVariableSelectionManager   = null;
		this.myTreePrefs                  = null;
		this.myEvidenceController         = null;
		this.hnInternalFrame              = null;
	  }catch( Throwable thrown ){
	    System.err.println( "warning: EvidenceTree.die() caught " + thrown );
	  }
	}

	/** @since 071204 */
	public String toString(){
		return "EvidenceTree" + Integer.toString( myIntDebug );
	}

	/** @since 091003 */
	public EnumProperty getEnumProperty()
	{
		return myEnumProperty;
	}

	/** @since 030702 */
	public void setShowProbabilities( DiagnosisType type, boolean show )
	{
		((EvidenceTreeCellRenderer)getCellRenderer()).setShowProbabilities( type, show );
	}

	/** @since 030602 */
	private Collection myEnumValueNodes = new LinkedList();

	/** @since 030602 */
	private Collection myVariableNodes = new LinkedList();

	/** @since 050802 */
	private Collection mySubmodelNodes = new LinkedList();

	private Collection myPropertyChangeVariables;

	/**
		interface NodePropertyChangeListener
		@author Keith Cascio
		@since 101102
	*/
	public void nodePropertyChanged( NodePropertyChangeEvent e )
	{
		//System.out.println( this.toString() + ".nodePropertyChanged( "+e+" )" );
		/*
		Throwable t = new Throwable();
		if( dVar == lastDVar )
		{
			//if( stackTraceEquals( lastThrowable, t ) ) t.printStackTrace();
			stackTraceEquals( lastThrowable, t );
		}
		lastDVar = dVar;
		lastThrowable = t;
		*/

		//revalidateVariable( dVar );

		myPropertyChangeVariables.add( e.variable );
		if( getEnumProperty() == EvidenceAssertedProperty.PROPERTY && e.property == EvidenceAssertedProperty.PROPERTY ) hnInternalFrame.getTreeScrollPane().refreshPropertyChanges();
	}

	/*
	private Throwable lastThrowable;
	private DisplayableFiniteVariable lastDVar;
	public static boolean stackTraceEquals( Throwable a, Throwable b )
	{
		Util.STREAM_DEBUG.println( "stackTraceEquals()" );

		if( a == null || b == null ) return false;

		StackTraceElement[] eltsA = a.getStackTrace();
		StackTraceElement[] eltsB = b.getStackTrace();

		if( eltsA.length == eltsB.length )
		{
			for( int i=0; i<eltsA.length; i++ )
			{
				if( !eltsA[i].equals( eltsB[i] ) )
				{
					Util.STREAM_DEBUG.println( "old[i]: " + eltsA[i] + "\nnew[i]: " + eltsB[i] );
					return false;
				}
			}
		}
		else
		{
			StackTraceElement[] greater;
			int indexDiff;
			String caption;
			if( eltsA.length > eltsB.length )
			{
				greater = eltsA;
				indexDiff = eltsB.length;
				caption = "old";
			}
			else
			{
				greater = eltsB;
				indexDiff = eltsA.length;
				caption = "new";
			}
			Util.STREAM_DEBUG.println( caption + "["+indexDiff+"]: " + greater[indexDiff] );
			return false;
		}

		Util.STREAM_DEBUG.println( "\t==" );
		return true;
	}*/

	/** @since 091003 */
	private void revalidateVariable( DisplayableFiniteVariable dVar )
	{
		DefaultMutableTreeNode tempNode = getNodeForVariable( dVar );
		if( tempNode != null )
		{
			revalidateVariableChildren( tempNode );
			//DefaultMutableTreeNode parent = (DefaultMutableTreeNode) tempNode.getParent();
			//parent.remove( tempNode );
			//myVariableNodes.remove( tempNode );
			//DefaultMutableTreeNode submodelNode = (DefaultMutableTreeNode) parent.getParent();
			//addToTree( tempNode, dVar.getProperty( myEnumProperty ), submodelNode );
			ensureSorted( tempNode, dVar );
			//((DefaultTreeModel)treeModel).nodeStructureChanged( parent );
		}
	}

	/** @since 091003 */
	public void refreshNonFatalPropertyChanges()
	{
		for( Iterator it = myPropertyChangeVariables.iterator(); it.hasNext(); )
		{
			revalidateVariable( (DisplayableFiniteVariable) it.next() );
		}
		myPropertyChangeVariables.clear();
		repaint();
	}

	/** @since 091003 */
	public boolean isPropertyChangeFatal()
	{
		//System.out.println( "EvidenceTree.isPropertyChangeFatal()" );
		DisplayableFiniteVariable dVar;
		DefaultMutableTreeNode tempNode;
		DefaultMutableTreeNode parent;
		for( Iterator it = myPropertyChangeVariables.iterator(); it.hasNext(); )
		{
			dVar = (DisplayableFiniteVariable) it.next();
			tempNode = getNodeForVariable( dVar );
			if( tempNode != null )
			{
				parent = (DefaultMutableTreeNode) tempNode.getParent();
				if( parent.getUserObject() != dVar.getProperty( myEnumProperty ) ) return true;
			}
		}
		return false;
	}

	/** @since 060903 */
	public DefaultMutableTreeNode getNodeForVariable( DisplayableFiniteVariable dVar )
	{
		DefaultMutableTreeNode tempNode = null;
		for( Iterator it = myVariableNodes.iterator(); it.hasNext(); )
		{
			tempNode = (DefaultMutableTreeNode) it.next();
			if( tempNode.getUserObject() == dVar ) return tempNode;
		}
		return null;
	}

	/** @since 060903 */
	public TreePath getPathForVariable( DisplayableFiniteVariable dVar )
	{
		DefaultMutableTreeNode tempNode = getNodeForVariable( dVar );
		if( tempNode == null ) return null;
		else return new TreePath( tempNode.getPath() );
	}

	/** @since 091003 */
	public void select( DisplayableFiniteVariable dVar )
	{
		addSelectionPath( getPathForVariable( dVar ) );
	}

	/** @since 111402 */
	public void ensureSorted( DefaultMutableTreeNode tempNode, DisplayableFiniteVariable dVar )
	{
		DefaultMutableTreeNode parentN = (DefaultMutableTreeNode) tempNode.getParent();
		DefaultMutableTreeNode previousN = (DefaultMutableTreeNode) parentN.getChildBefore(tempNode);
		DefaultMutableTreeNode nextN = (DefaultMutableTreeNode) parentN.getChildAfter(tempNode);
		DisplayableFiniteVariable pdvar = null;
		DisplayableFiniteVariable ndvar = null;

		if( previousN != null && previousN.getUserObject() instanceof DisplayableFiniteVariable ) pdvar = (DisplayableFiniteVariable) previousN.getUserObject();
		if( nextN != null && nextN.getUserObject() instanceof DisplayableFiniteVariable ) ndvar = (DisplayableFiniteVariable) nextN.getUserObject();

		boolean resort = ( pdvar != null && pdvar.compareTo( dVar ) > 0 ) ||
				( ndvar != null && ndvar.compareTo( dVar ) < 0 );

		//System.out.println( "EvidenceTree.ensureSorted(), resort == " + resort );

		if( resort )
		{
			parentN.remove( tempNode );
			int count = (int)0;
			for( Enumeration enumeration = parentN.children(); enumeration.hasMoreElements(); count++ )
			{
				nextN = (DefaultMutableTreeNode) enumeration.nextElement();
				if( nextN.getUserObject() instanceof DisplayableFiniteVariable )
				{
					ndvar = (DisplayableFiniteVariable) nextN.getUserObject();
					if( dVar.compareTo( ndvar ) < 0 ) break;
				}
				else break;
			}

			//System.out.println( "inserting "+ tempNode +" at " + count );
			parentN.insert( tempNode, count );
			((DefaultTreeModel)treeModel).nodeStructureChanged( parentN );
		}
	}

	/** @since 20020306
		@ret The child DefaultMutableTreeNode for submodels */
	private DefaultMutableTreeNode setup( DefaultMutableTreeNode submodelnode )
	{
	  //System.out.println( "setup( "+submodelnode+" )" );

		if( !(submodelnode.getUserObject() == mainSubmodel) ) mySubmodelNodes.add( submodelnode );

		//locals
		DefaultMutableTreeNode tempNode = null;
		EnumValue temp = null;
		for( Iterator it = myEnumProperty.iterator(); it.hasNext(); )
		{
			temp = (EnumValue) it.next();
			tempNode = new DefaultMutableTreeNode( temp );
			submodelnode.add( tempNode );
		  /*String hash = Integer.toString( System.identityHashCode( temp ) );
			//System.out.println( "    " + submodelnode + ".add( " + tempNode + "          ".substring( tempNode.toString().length() ) + ") " + temp + "          ".substring( temp.toString().length()) + "         ".substring( hash.toString().length() ) + hash );*/
			myEnumValueNodes.add( tempNode );
		}

		return submodelnode;
	}

	public Object myUserobjectSubmodels = "Submodels";

	/** @since 030602 */
	private void addToTree( DisplayableFiniteVariable dVar, DefaultMutableTreeNode submodelnode )
	{
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode( dVar );
		for (int j = 0; j < dVar.size(); j++)
		{
			newNode.add( new DefaultMutableTreeNode( dVar.instance(j) ) );
		}
		boolean result = addToTree( newNode, dVar.getProperty( myEnumProperty ), submodelnode );
	  //System.out.println( " ? " + result );
	}

	private boolean addToTree( DefaultMutableTreeNode newNode, EnumValue enumValue, DefaultMutableTreeNode submodelnode )
	{
	  /*java.io.PrintStream stream = System.out;
		stream.print( "ET.addToTree( " );
		stream.print( newNode );
		stream.print( ',' );
		stream.print( "                      ".substring( newNode.toString().length() ) );
		stream.print( enumValue );
		stream.print( "           ".substring( enumValue.toString().length() ) );
		String hash = Integer.toString( System.identityHashCode( enumValue ) );
		stream.print( "        ".substring( hash.toString().length() ) );
		stream.print( hash );
		stream.print( ", " );
		stream.print( submodelnode );
		stream.print( " )" );*/

		if( enumValue == null ) enumValue = myEnumProperty.getDefault();
		if( ! myEnumProperty.contains( enumValue ) ) throw new IllegalStateException( "property '"+myEnumProperty+" "+System.identityHashCode(myEnumProperty)+"' does not contain value '"+enumValue+" "+System.identityHashCode(enumValue)+"'" );

		DefaultMutableTreeNode parentNode;
		for( Enumeration enumeration = submodelnode.children(); enumeration.hasMoreElements(); )
		{
			parentNode = (DefaultMutableTreeNode)(enumeration.nextElement());
			if( parentNode.getUserObject() == enumValue )
			{
				parentNode.add( newNode );
				myVariableNodes.add( newNode );
				return true;
			}
		}

		return false;
	}

	/** @since 090902 */
	protected void revalidateVariableChildren( DefaultMutableTreeNode variablenode )
	{
		Object userobject = variablenode.getUserObject();
		if( userobject instanceof DisplayableFiniteVariable )
		{
			DisplayableFiniteVariable dVar = (DisplayableFiniteVariable)userobject;
			//System.out.println( "Revalidating " + dVar );
			variablenode.removeAllChildren();
			for (int j = 0; j < dVar.size(); j++)
			{
				variablenode.add( new DefaultMutableTreeNode( dVar.instance(j) ) );
			}
			((DefaultTreeModel)treeModel).nodeStructureChanged( variablenode );
		}
		else throw new IllegalArgumentException( "EvidenceTree.revalidateVariableChildren( *non-variable node* )" );
	}

	/** @since 090902 */
	protected void revalidateTargetVariableChildren()
	{
		DefaultMutableTreeNode temp = null;
		DisplayableFiniteVariable dVar = null;
		for( Iterator it = myVariableNodes.iterator(); it.hasNext(); )
		{
			temp = (DefaultMutableTreeNode) it.next();
			dVar = (DisplayableFiniteVariable) temp.getUserObject();
			if( dVar.getDiagnosisType() == DiagnosisType.TARGET )
			{
				myDefaultTreeModel.nodeStructureChanged( temp );
				//revalidateVariableChildren( temp );
			}
		}
	}

	private EnumProperty myEnumProperty = DiagnosisType.PROPERTY;
	private DSLSubmodel mainSubmodel = null;
	protected EvidenceTreeCellRenderer myTreeCellRenderer = null;
	protected DefaultTreeModel myDefaultTreeModel = null;
	protected VariableSelectionManager myVariableSelectionManager;

	/** @since 20020508 */
	private void init()
	{
	  //System.out.println( "EvidenceTree.init() myEnumProperty? " + myEnumProperty+" "+System.identityHashCode(myEnumProperty) );
		myIntDebug = INT_DEBUG_COUNTER++;

		DisplayableBeliefNetwork theNet = hnInternalFrame.getBeliefNetwork();

		//DSLSubmodelFactory fact = theNet.getDSLSubmodelFactory();
		mainSubmodel = theNet.getMainDSLSubmodel();

		DefaultMutableTreeNode root = initRec( mainSubmodel, theNet );
		clean( root );

		myDefaultTreeModel = new DefaultTreeModel(root);
		setModel( myDefaultTreeModel );
		setRootVisible(false);
		setExpandVariableBranches( false );
		//setExpandTypeBranches( true );
		setExpandSubmodelBranches( false );

		myTreeCellRenderer = new EvidenceTreeCellRenderer( myTreePrefs,hnInternalFrame );
		setCellRenderer( myTreeCellRenderer );
		setShowProbabilities( DiagnosisType.TARGET, ((Boolean) myTreePrefs.getMappedPreference( SamiamPreferences.STR_SHOW_TARGET_PROBABILITIES ).getValue()).booleanValue() );
		setShowProbabilities( DiagnosisType.AUXILIARY, ((Boolean) myTreePrefs.getMappedPreference( SamiamPreferences.STR_SHOW_AUXILIARY_PROBABILITIES ).getValue()).booleanValue() );
		setShowProbabilities( DiagnosisType.OBSERVATION, ((Boolean) myTreePrefs.getMappedPreference( SamiamPreferences.STR_SHOW_OBSERVATION_PROBABILITIES ).getValue()).booleanValue() );

		addMouseListener( this );

		//System.out.println( "EvidenceTree.getScrollableTracksViewportWidth(): " + getScrollableTracksViewportWidth() );
		//System.out.println( "EvidenceTree.getScrollableTracksViewportHeight(): " + getScrollableTracksViewportHeight() );
		//System.out.println( "EvidenceTree.getPreferredScrollableViewportSize(): " + getPreferredScrollableViewportSize() );
		//System.out.println( "EvidenceTree.getAutoscrolls(): " + getAutoscrolls() );

		setAutoscrolls( false );

		setSelectionManaged( true );

		// redmine 53, disable CTRL-A for evidence tree
		// this was tricky until I read the javadoc for JComponent.unregisterKeyboardAction()
		getInputMap().put( KeyStroke.getKeyStroke( KeyEvent.VK_A, InputEvent.CTRL_MASK ), new Object() );
	}

	/** @since 060903 */
	public void setSelectionManaged( boolean flag )
	{
		if( flag )
		{
			if( myVariableSelectionManager == null ) myVariableSelectionManager = new VariableSelectionManager( this, hnInternalFrame.getNetworkDisplay() );
			else myVariableSelectionManager.setKnown( true );
			myVariableSelectionManager.synchronizeOnDisplay();
		}
		else if( !flag && myVariableSelectionManager != null ) myVariableSelectionManager.setKnown( false );
	}

	/** @since 081902 */
	public void setVisible( boolean aflag )
	{
		super.setVisible( aflag );
		if( aflag )
		{
			setExpandVariableBranches( true );
			setExpandVariableBranches( false );
		}
	}

	/** @since 050802 */
	private DefaultMutableTreeNode initRec( DSLSubmodel submodel, DisplayableBeliefNetwork theNet )
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode( submodel );
		DefaultMutableTreeNode submodelsnode = setup( root );

		List sorted = new ArrayList( theNet.getVariables(submodel) );
		//System.out.println( submodel + " (before sort):" + sorted );
		Collections.sort( sorted );
		//System.out.println( submodel + " (after  sort):" + sorted );

		DisplayableFiniteVariable dVar = null;
		for( Iterator it = sorted.iterator(); it.hasNext(); )
		{
			dVar = (DisplayableFiniteVariable) it.next();
			addToTree( dVar, root );
		}

		DSLSubmodel tempSubmodel = null;
		for( Iterator it = submodel.getChildSubmodels(); it.hasNext(); )
		{
			tempSubmodel = (DSLSubmodel)it.next();
			submodelsnode.add( initRec( tempSubmodel, theNet ) );
		}

		return root;
	}

	/** @since 050802 */
	private void clean( DefaultMutableTreeNode root )
	{
		DefaultMutableTreeNode tempNode = null;
		Object tempUserObject = null;
		//TreePath tempPath = null;
		java.util.List toRemove = new LinkedList();
		for( Enumeration enumeration = root.children(); enumeration.hasMoreElements(); )
		{
			tempNode = (DefaultMutableTreeNode)enumeration.nextElement();
			//tempPath = new TreePath( tempNode.getPath() );
			//collapsePath( tempPath );
			tempUserObject = tempNode.getUserObject();
			if( tempUserObject instanceof EnumValue && tempNode.isLeaf() )
			{
				toRemove.add( tempNode );
			}
			else if( tempUserObject instanceof DSLSubmodel ) clean( tempNode );
		}

		for( Iterator it = toRemove.iterator(); it.hasNext(); )
		{
			tempNode =  (DefaultMutableTreeNode) it.next();
			myEnumValueNodes.remove( tempNode );
			root.remove( tempNode );
		}
	}

	/** @since 050802 */
	protected void setExpandBranches( Collection nodes, boolean isExpanded )
	{
		DefaultMutableTreeNode parentNode = null;
		TreePath tempPath = null;
		for( Iterator it = nodes.iterator(); it.hasNext(); )
		{
			parentNode = (DefaultMutableTreeNode)(it.next());
			//if( parentNode.getUserObject() == mainSubmodel ) continue;
			tempPath = new TreePath( parentNode.getPath() );
			if( isExpanded ) expandPath( tempPath );
			else collapsePath( tempPath );
		}
	}

	/** @since 120402 */
	protected boolean areBranchesExpanded( Collection nodes )
	{
		if( nodes.isEmpty() ) return false;

		DefaultMutableTreeNode temp = null;
		for( Iterator it = nodes.iterator(); it.hasNext(); )
		{
			temp = (DefaultMutableTreeNode)(it.next());
			if( !isExpanded( new TreePath( temp.getPath() ) ) ) return false;
		}

		return true;
	}

	/** @since 120402 */
	public boolean areSubmodelBranchesExpanded()
	{
		return areBranchesExpanded( mySubmodelNodes );
	}

	/** @since 050802 */
	public void setExpandSubmodelBranches( boolean isExpanded )
	{
		setExpandBranches( mySubmodelNodes, isExpanded );
	}

	/** @since 120402 */
	public boolean areTypeBranchesExpanded()
	{
		return areBranchesExpanded( myEnumValueNodes );
	}

	/** @since 050802 */
	public void setExpandTypeBranches( boolean isExpanded )
	{
		setExpandBranches( myEnumValueNodes, isExpanded );
	}

	/** @since 120402 */
	public boolean areVariableBranchesExpanded()
	{
		return areBranchesExpanded( myVariableNodes );
	}

	/** @since 050802 */
	public void setExpandVariableBranches( boolean isExpanded )
	{
		setExpandBranches( myVariableNodes, isExpanded );
	}

	public Set getExpandedSet()
	{
		HashSet expandedSet = new HashSet();
		Iterator it = myVariableNodes.iterator();
		DefaultMutableTreeNode temp = null;
		while( it.hasNext() )
		{
			temp = (DefaultMutableTreeNode)(it.next());
			if( isExpanded(new TreePath(temp.getPath())) ) expandedSet.add( temp.getUserObject() );
		}

		return expandedSet;
	}

	public void setExpandedSet(Set expandedSet)
	{
		Iterator it = myVariableNodes.iterator();
		DefaultMutableTreeNode temp = null;
		while( it.hasNext() )
		{
			temp = (DefaultMutableTreeNode)(it.next());
			if (expandedSet.contains(temp.getUserObject()))
			{
				//temp.expand();
				expandPath( new TreePath( temp.getPath() ) );
			}
			else collapsePath( new TreePath( temp.getPath() ) );//temp.collapse();
		}

	}

	public void expandAll()
	{
		setExpandVariableBranches( true );
	}

	public void collapseAll()
	{
		setExpandVariableBranches( false );
	}

	public Object getEvidence( FiniteVariable var )
	{
		if( myWarning != null && myWarning.recentEvidenceChangeVariables.contains( var ) ){ return WARNING; }
		else return waitForEvidence( var );
	}

	/** @since 20030728 */
	public Object getWarningEvidenceValue( FiniteVariable var )
	{
		return waitForEvidence( var );
	}

	/** @since 20080221 */
	public Object waitForEvidence( FiniteVariable var ){
		if( hnInternalFrame      == null ){ return null; }//dead
		if( myEvidenceController == null ){
			try{
				for( int i=0; (myEvidenceController == null) && (i<0x10); i++ ){ Thread.sleep( 0x10 ); }
			}catch( Throwable thrown ){
				Util.warn( thrown, "EvidenceTree.waitForEvidence()" );
			}
		}
		return myEvidenceController == null ? null : myEvidenceController.getValue( var );
	}

	/** @since 20091130 */
	public EvidenceController getEvidenceController(){
		return myEvidenceController;
	}

	/** interface EvidenceChangeListener
 		@since    20030710 */
	public void warning( EvidenceChangeEvent ece )
	{
		//System.out.println( "EvidenceTree.warning()" );
		//new Throwable().printStackTrace();
		myWarning = ece;
		paintImmediately( SwingUtilities.getLocalBounds( this ) );
	}

	public void evidenceChanged( EvidenceChangeEvent ECE ) {
		myWarning = null;
		revalidateTree();
	}

	/** @since 071003 */
	public EvidenceChangeEvent getWarning()
	{
		return myWarning;
	}

	protected EvidenceChangeEvent myWarning;
	public static final String WARNING = "evidence change warning";

	/** @since 090502 */
	public void networkRecompiled(){
		revalidateTree();
	}

	/** @since 090502 */
	public void cptChanged( CPTChangeEvent evt ){
		revalidateTree();
	}

	/** @since 090602 */
	public void revalidateTree()
	{
		//System.out.println( this.toString() + ".revalidateTree()" );
		TreePath[] paths = getSelectionPaths();
		myTreeCellRenderer.updateUI();
		revalidateTargetVariableChildren();
		setSelectionPaths( paths );

		//paintAll( getGraphics() );
		//treeDidChange();
		//validateTree();
		revalidate();
		repaint();
	}

	/** @since 090602 */
	/*
	public void repaint()
	{
		super.repaint();
		//System.out.println( "EvidenceTree.repaint()" );
	}*/

	public void changePackageOptions()
	{
		revalidateTree();
	}

	public void mousePressed(MouseEvent event) { }

	public void mouseEntered(MouseEvent event) { }

	public void mouseExited(MouseEvent event) { }

	public void mouseReleased(MouseEvent event) { }

	public void mouseClicked(MouseEvent event)
	{
		if( event.isPopupTrigger() ) return;//keith 031202

		if( event.getClickCount() > 1 ) return;//keith 081902

		TreePath path = getPathForLocation(event.getX(),event.getY());

		if (path == null) return;

		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		//boolean selected = isPathSelected( path );

		//if (node.getLevel() != 2)
		//return;

		Object obj = node.getUserObject();
		if( obj instanceof EnumValue )
		{
			//TreeNode[] array0 = ((DefaultMutableTreeNode)node.getFirstChild()).getPath();
			//TreeNode[] array1 = ((DefaultMutableTreeNode)node.getLastChild()).getPath();
			//int index0 = getRowForPath( new TreePath(array0) );
			//int index1 = getRowForPath( new TreePath(array1) );
			//setSelectionInterval(index0,index1);

			clearSelection();

			TreePath[] paths = new TreePath[ node.getChildCount() ];

			DefaultMutableTreeNode child;
			TreeNode[] nodePath;
			TreePath treePath;
			int i=0;
			for( Enumeration children = node.children(); children.hasMoreElements(); )
			{
				child = (DefaultMutableTreeNode) children.nextElement();
				nodePath = child.getPath();
				treePath = new TreePath( nodePath );
				paths[i++] = treePath;
			}
			setSelectionPaths( paths );
		}
		else if( obj instanceof DisplayableFiniteVariable )
		{
			//FiniteVariable fVar = ((DisplayableFiniteVariable)obj).getFiniteVariable();
			boolean additive = ( event.isShiftDown() || event.isControlDown() );
			DisplayableFiniteVariable dVar = (DisplayableFiniteVariable)obj;
			//if( selected )
			hnInternalFrame.getNetworkDisplay().ensureNodeIsVisible( dVar, additive );
			//else dVar.getNodeLabel().setSelected( false );
		}
		else if( obj instanceof DSLSubmodel )
		{
			SubmodelLabel SL = (SubmodelLabel) ((DSLSubmodel)obj).userobject;
			if( SL == null ) System.err.println( "Warning: submodel clicked in tree view lacks SubmodelLabel userobject." );
			else SL.showNetworkDisplay();
		}
		else
		{
			Object instance = obj;
			DefaultMutableTreeNode pnode = (DefaultMutableTreeNode) node.getParent();
			DisplayableFiniteVariable dVar = (DisplayableFiniteVariable) pnode.getUserObject();
			hnInternalFrame.evidenceRequest( dVar, instance, (Component)EvidenceTree.this );
		}
	}
}
