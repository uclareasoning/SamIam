package edu.ucla.belief.ui.tree;

import edu.ucla.belief.ui.networkdisplay.SelectionListener;
import edu.ucla.belief.ui.networkdisplay.NetworkDisplay;
import edu.ucla.belief.ui.networkdisplay.NetworkComponentLabel;
import edu.ucla.belief.ui.networkdisplay.NodeLabel;
import edu.ucla.belief.ui.displayable.DisplayableFiniteVariable;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.util.Iterator;

/**
	@author Keith Cascio
	@since 060903
*/
public class VariableSelectionManager implements TreeSelectionListener, SelectionListener
{
	public VariableSelectionManager( EvidenceTree tree, NetworkDisplay display )
	{
		myEvidenceTree = tree;
		myEvidenceTree.addTreeSelectionListener( this );
		myNetworkDisplay = display;
		myNetworkDisplay.addSelectionListener( this );
	}

	public void synchronizeOnDisplay()
	{
		//System.out.println( "VariableSelectionManager.synchronizeOnDisplay()" );
		myEvidenceTree.clearSelection();

		DisplayableFiniteVariable dVar;
		NodeLabel nl;
		for( Iterator it = myNetworkDisplay.getBeliefNetwork().iterator(); it.hasNext(); )
		{
			dVar = (DisplayableFiniteVariable) it.next();
			nl = dVar.getNodeLabel();
			if( nl == null ){
				System.err.println( "WARNING: in VariableSelectionManager.synchronizeOnDisplay(), encountered DFV "+dVar+", lacks NodeLabel." );
				return;
			}
			if( nl.isSelected() ) myEvidenceTree.select( dVar );
		}
	}

	public void setKnown( boolean known )
	{
		if( known != myFlagKnown )
		{
			if( known )
			{
				myEvidenceTree.addTreeSelectionListener( this );
				myNetworkDisplay.addSelectionListener( this );
			}
			else
			{
				myEvidenceTree.removeTreeSelectionListener( this );
				myNetworkDisplay.removeSelectionListener( this );
			}
			myFlagKnown = known;
		}
	}

	public void valueChanged( TreeSelectionEvent e )
	{
		if( myFlagIgnoreTree ) return;

		myFlagIgnoreNetworkDisplay = true;
		TreePath[] paths = e.getPaths();

		TreePath path;
		DefaultMutableTreeNode node;
		Object obj;
		DisplayableFiniteVariable dVar;
		for( int i=0; i<paths.length; i++ )
		{
			path = paths[i];
			node = (DefaultMutableTreeNode)path.getLastPathComponent();
			obj = node.getUserObject();
			if( obj instanceof DisplayableFiniteVariable )
			{
				dVar = (DisplayableFiniteVariable)obj;
				dVar.getNodeLabel().setSelected( e.isAddedPath( path ) );
			}
		}

		myFlagIgnoreNetworkDisplay = false;
	}

	/** interface SelectionListener */
	public void selectionChanged( NetworkComponentLabel label )
	{
		if( myFlagIgnoreNetworkDisplay ) return;

		myFlagIgnoreTree = true;

		DisplayableFiniteVariable dVar = label.getFiniteVariable();
		if( dVar != null )
		{
			TreePath path = myEvidenceTree.getPathForVariable( dVar );
			if( path != null )
			{
				if( label.isSelected() ) myEvidenceTree.addSelectionPath( path );
				else myEvidenceTree.removeSelectionPath( path );
			}
		}

		myFlagIgnoreTree = false;
	}

	/**
		interface SelectionListener
		@since 011504
	*/
	public void selectionReset()
	{
		if( myFlagIgnoreNetworkDisplay ) return;
		myFlagIgnoreTree = true;

		myEvidenceTree.clearSelection();

		myFlagIgnoreTree = false;
	}

	private boolean myFlagIgnoreTree = false;
	private boolean myFlagIgnoreNetworkDisplay = false;
	private EvidenceTree myEvidenceTree;
	private NetworkDisplay myNetworkDisplay;
	private boolean myFlagKnown = true;
}
