package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.decision.*;
import edu.ucla.belief.StateNotFoundException;

//import edu.ucla.belief.ui.*;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.event.*;
import java.util.*;
import java.awt.Component;

/**
	@author Keith Cascio
	@since 121204
*/
public class DecisionTableModel extends MultiViewTableModel implements TableModel
{
	public DecisionTableModel( GroupingColoringJTable table, DisplayableDecisionTree tree )
	{
		super( table );
		this.myTree = tree;
		boolean nosuccess = true;
		while( nosuccess ){
			try{
				super.setHueRange( (float)0, (float)0.5 );
				nosuccess = false;
			}catch( InterruptedException interruptedexception ){
				nosuccess = true;
			}
		}
		initDecisionTableModel();
	}

	/** @since 011105 */
	public String getDisplayOutcomeTypeName(){
		return "outcome";
	}

	public void setDecisionNode( DecisionNode node ){
		super.setDecisionNode( node );
		myMenuAssign.configure( node );
	}

	private void initDecisionTableModel(){
		GroupingColoringPopupHandler handler = getPopupHandler();
		handler.addPopupMenuListener( (PopupMenuListener)this );

		myMenuAssign = new MenuAssignment( "assign selected", myTree, myTree.getFactory(), myGroupingColoringJTable );
		myMenuAssign.addListener( myTree );
		handler.insertItem( myMenuAssign, 0 );
	}

	/** interface PopupMenuListener */
	public void popupMenuWillBecomeVisible( PopupMenuEvent e )
	{
		super.popupMenuWillBecomeVisible( e );
		boolean editable = this.isEditable() && myDecisionInternal.isEditable();
		myMenuAssign.setEnabled( editable && (!myGroupingColoringJTable.isSelectionEmpty()) );
		//myItemDistinctAll.setEnabled( editable );
	}

	/** @since 011105 */
	public void makeOutcomeDistinctForInstance( Object instance ){
		try{
			makeOutcomeDistinctForInstancePrivate( instance, myTree.getFactory().clone( myDecisionInternal.getNext( instance ) ) );
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: in DecisionTableModel.makeOutcomeDistinctForInstance(): " + statenotfoundexception );
		}
	}

	/** @since 011105 */
	private void makeOutcomeDistinctForInstancePrivate( Object instance, DecisionNode clone ){
		try{
			myDecisionInternal.setNext( instance, clone );
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: in DecisionTableModel.makeDistinct(): " + statenotfoundexception );
		}
	}

	/** @since 011605 */
	public void makeOutcomeDistinct( Object outcome ){
		if( !myDecisionInternal.hasOutcome( outcome ) ) return;
		makeOutcomeDistinctPrivate( outcome, myTree.getFactory().clone( (DecisionNode) outcome ) );
	}

	/** @since 011605 */
	private void makeOutcomeDistinctPrivate( Object outcome, DecisionNode clone ){
		DecisionNode nodeold = (DecisionNode) outcome;
		DecisionNode nodenew = clone;
		int size = myDecisionInternal.getVariable().size();
		try{
			for( int i=0; i<size; i++ )
				if( myDecisionInternal.getNext(i) == nodeold )
					myDecisionInternal.setNext( i, nodenew );
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: in DecisionTableModel.makeOutcomeDistinct(): " + statenotfoundexception );
		}
	}

	/** @since 011105 */
	public boolean supportsDeepCloning(){
		return true;
	}

	/** @since 011105 */
	public void makeOutcomeDistinctForInstanceDeep( Object instance ){
		try{
			makeOutcomeDistinctForInstancePrivate( instance, myDecisionInternal.getNext( instance ).deepClone( myTree.getFactory() ) );
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: in DecisionTableModel.makeOutcomeDistinctForInstance(): " + statenotfoundexception );
		}
	}

	/** @since 011605 */
	public void makeOutcomeDistinctDeep( Object outcome ){
		if( !myDecisionInternal.hasOutcome( outcome ) ) return;
		makeOutcomeDistinctPrivate( outcome, ((DecisionNode)outcome).deepClone( myTree.getFactory() ) );
	}

	public void setInternal( DecisionInternal internal )
	{
		this.myDecisionInternal = internal;
		setDecisionNode( internal );
	}

	public Object getOutcome( int rowIndex ){
		try{
			return myDecisionInternal.getNext( rowIndex );
		}catch( StateNotFoundException statenotfoundexception ){
			return "getOutcome("+rowIndex+") StateNotFoundException";
		}
	}

	public View getDefaultView(){
		getArrayViews();
		return DEFAULT;
	}

	public View[] getArrayViews(){
		if( ARRAY_VIEWS == null ){
			ID_RENAME = new View( "id (rename)" ){
				public String outcomeToString( Object outcome ){
					return outcome.toString();
				}
				public Object getEditValue( Object value, int row ){
					DecisionNode dnode = (DecisionNode) DecisionTableModel.this.getOutcomeAt( row );
					return dnode.toString();
				}
				public boolean setValue( Object obj, Object editedvalue, Object instance ){
					DecisionNode outcome = (DecisionNode) obj;
					String newID = editedvalue.toString();
					Factory factory = myTree.getFactory();
					if( !factory.isValidID( newID ) ){
						DisplayableDecisionTree.warnInvalidID( newID, "decision node", (Component)myGroupingColoringJTable );
						return false;
					}
					boolean unique = factory.isUniqueNodeID( newID );
					if( !unique ) if( !DisplayableDecisionTree.promptNonUniqueID( newID, "decision node", (Component)myGroupingColoringJTable ) ) return false;
					fireEditWarning();
					outcome.setID( newID );
					return true;
				}
			};

			ID_REASSIGN = new View( "id (reassign)" ){
				public String outcomeToString( Object outcome ){
					return ID_RENAME.outcomeToString( outcome );
				}
				public Object getEditValue( Object value, int row ){
					return ID_RENAME.getEditValue( value, row );
				}
				public boolean setValue( Object obj, Object editedvalue, Object instance ){
					return DecisionTableModel.this.reassignDecisionNode( (DecisionNode) obj, editedvalue, instance );
				}
			};
			ARRAY_VIEWS = new View[]{ ID_REASSIGN, ID_RENAME };
			DEFAULT = ID_REASSIGN;
		}
		return ARRAY_VIEWS;
	}

	private View ID_REASSIGN, ID_RENAME;
	private View DEFAULT;
	private View[] ARRAY_VIEWS;

	public boolean reassignDecisionNode( DecisionNode outcome, Object editedvalue, Object instance ){
		String newID = editedvalue.toString();
		String errmsg = "id \""+newID+"\"";
		Factory factory = myTree.getFactory();
		if( factory.isValidID( newID ) ){
			DecisionNode forid = factory.nodeForID( newID );
			if( forid != null ){
				try{
					return reassignDecisionNode( outcome, forid, instance );
				}
				catch( StateNotFoundException statenotfoundexception ){
					errmsg = errmsg+" error: " + statenotfoundexception.getMessage();
				}
			}
			else errmsg = "Node with "+errmsg+" not found.";
		}
		else errmsg = errmsg+" is invalid.";

		String title = "Decision Node Reassignment Failed";
		JOptionPane.showMessageDialog( (Component)myGroupingColoringJTable, errmsg, title, JOptionPane.ERROR_MESSAGE );
		return false;
	}

	public boolean reassignDecisionNode( DecisionNode dnodeold, DecisionNode dnodenew, Object instance ) throws StateNotFoundException
	{
		Collection instances = null;
		if( isGroupingEnabled() ) instances = getGroupForOutcome( dnodeold );
		else instances = Collections.singleton( instance );

		fireEditWarning();

		for( Iterator it = instances.iterator(); it.hasNext(); ){
			myDecisionInternal.setNext( it.next(), dnodenew );
		}
		return true;
	}

	private DecisionInternal myDecisionInternal;
	private DisplayableDecisionTree myTree;

	private MenuAssignment myMenuAssign;
}
