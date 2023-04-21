package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.decision.*;
import edu.ucla.belief.StateNotFoundException;

//import edu.ucla.belief.ui.*;

import javax.swing.JOptionPane;
import javax.swing.JMenuItem;
import javax.swing.table.TableModel;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import java.util.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
	@author Keith Cascio
	@since 121204
*/
public class ParameterTableModel extends MultiViewTableModel implements TableModel
{
	public ParameterTableModel( GroupingColoringJTable table, Factory factory )
	{
		super( table );
		this.myFactory = factory;
		boolean nosuccess = true;
		while( nosuccess ){
			try{
				super.setHueRange( (float)0.5, (float)1 );
				nosuccess = false;
			}catch( InterruptedException interruptedexception ){
				nosuccess = true;
			}
		}
		initParameterTableModel();
	}

	/** @since 011105 */
	public String getDisplayOutcomeTypeName(){
		return "parameter";
	}

	/** @since 011105 */
	public void makeOutcomeDistinctForInstance( Object instance ){
		try{
			myDecisionLeaf.setParameter( instance, myFactory.clone( myDecisionLeaf.getParameter( instance ) ) );
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: in ParameterTableModel.doMakeDistinct(): " + statenotfoundexception );
		}
	}

	/** @since 011605 */
	public void makeOutcomeDistinct( Object outcome ){
		if( !myDecisionLeaf.hasOutcome( outcome ) ) return;
		Parameter paramold = (Parameter) outcome;
		Parameter paramnew = myFactory.clone( paramold );
		int size = myDecisionLeaf.getVariable().size();

		for( int i=0; i<size; i++ )
			if( myDecisionLeaf.getParameter(i) == paramold )
				myDecisionLeaf.setParameter( i, paramnew );
	}

	private void initParameterTableModel(){
		GroupingColoringPopupHandler handler = getPopupHandler();
		handler.addPopupMenuListener( (PopupMenuListener)this );

		myItemNormalize = new JMenuItem( "normalize" );
		myItemNormalize.addActionListener( (ActionListener)this );
		handler.insertItem( myItemNormalize, 0 );

		myItemComplement = new JMenuItem( "complement" );
		myItemComplement.addActionListener( (ActionListener)this );
		handler.insertItem( myItemComplement, 1 );
	}

	/** interface ActionListener */
	public void actionPerformed( ActionEvent e ){
		super.actionPerformed( e );
		Object src = e.getSource();
		if( src == myItemNormalize ) doNormalize();
		if( src == myItemComplement ) doComplement( myGroupingColoringJTable.getSelectedInstances( new HashSet(getRowCount()) ) );
		//else if( src == myItemDistinctSelected )
	}

	/** interface PopupMenuListener */
	public void popupMenuWillBecomeVisible( PopupMenuEvent e )
	{
		super.popupMenuWillBecomeVisible( e );
		boolean editable = this.isEditable() && myDecisionLeaf.isEditable();
		myItemNormalize.setEnabled( editable );
		myItemComplement.setEnabled( editable && (!myGroupingColoringJTable.isSelectionEmpty()) );
	}

	/** @since 011505 */
	private void doComplement( Collection selectedInstances ){
		if( (selectedInstances == null) || (selectedInstances.isEmpty()) ) return;
		fireEditWarning();
		//Interruptable.checkInterrupted();
		try{
			myDecisionLeaf.complement( selectedInstances, /* makeDistinct */true );
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: " + statenotfoundexception );
		}catch( Exception exception ){
			String title = "Error: Complement Failed";
			String errmsg = exception.getMessage();
			JOptionPane.showMessageDialog( (Component)myGroupingColoringJTable, errmsg, title, JOptionPane.ERROR_MESSAGE );
		}
	}

	/** @since 011505 */
	private void doNormalize(){
		fireEditWarning();
		//doMakeDistinct( (Collection)null, false );//unecessary - handled in DecisionLeaf.normalize()
		myDecisionLeaf.normalize( /* makeDistinct */true );
		//fireTableDataChanged();//uneccesary because of events
	}

	public void setLeaf( DecisionLeaf leaf )
	{
		this.myDecisionLeaf = leaf;
		setDecisionNode( leaf );
	}

	public View getDefaultView(){
		getArrayViews();
		return DEFAULT;
	}

	public View[] getArrayViews(){
		if( ARRAY_VIEWS == null ){
			VALUE = new View( "value only" ){
				public String outcomeToString( Object outcome ){
					return Double.toString( ((Parameter)outcome).getValue() );
				}
				public Object getEditValue( Object value, int row ){
					Parameter param = (Parameter) ParameterTableModel.this.getOutcomeAt( row );
					return new Double( param.getValue() );
				}
				public boolean setValue( Object obj, Object editedvalue, Object instance ){
					Parameter outcome = (Parameter) obj;
					fireEditWarning();
					try{
						outcome.setValue( Double.parseDouble( editedvalue.toString() ) );
						return true;
					}catch( Exception e ){
						System.err.println( "e" );
					}
					return false;
				}
			};

			ID_RENAME = new View( "id (rename)" ){
				public String outcomeToString( Object outcome ){
					return ((Parameter)outcome).getID();
				}
				public Object getEditValue( Object value, int row ){
					Parameter param = (Parameter) ParameterTableModel.this.getOutcomeAt( row );
					return param.getID();
				}
				public boolean setValue( Object obj, Object editedvalue, Object instance ){
					Parameter outcome = (Parameter) obj;
					String newID = editedvalue.toString();
					if( !myFactory.isValidID( newID ) ){
						DisplayableDecisionTree.warnInvalidID( newID, "parameter", (Component)myGroupingColoringJTable );
						return false;
					}
					boolean unique = myFactory.isUniqueParameterID( newID );
					if( !unique ) if( !DisplayableDecisionTree.promptNonUniqueID( newID, "parameter", (Component)myGroupingColoringJTable ) ) return false;
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
					return ParameterTableModel.this.reassignParameter( (Parameter) obj, editedvalue, instance );
				}
			};

			BOTH = new View( "id and value" ){
				public String outcomeToString( Object outcome ){
					return ((Parameter)outcome).getID() + ": " + Double.toString( ((Parameter)outcome).getValue() );
				}
				public Object getEditValue( Object value, int row ){
					return VALUE.getEditValue( value, row );
				}
				public boolean setValue( Object obj, Object editedvalue, Object instance ){
					return VALUE.setValue( obj, editedvalue, instance );
				}
			};
			ARRAY_VIEWS = new View[]{ VALUE, ID_RENAME, ID_REASSIGN, BOTH };
			DEFAULT = VALUE;
		}
		return ARRAY_VIEWS;
	}

	private View VALUE, ID_RENAME, ID_REASSIGN, BOTH;
	private View DEFAULT = VALUE;
	private View[] ARRAY_VIEWS;

	public boolean reassignParameter( Parameter outcome, Object editedvalue, Object instance ){
		String newID = editedvalue.toString();
		String errmsg = "id \""+newID+"\"";
		if( myFactory.isValidID( newID ) ){
			Parameter forid = myFactory.parameterForID( newID );
			if( forid != null ){
				try{
					return reassignParameter( outcome, forid, instance );
				}
				catch( StateNotFoundException statenotfoundexception ){
					errmsg = errmsg+" error: " + statenotfoundexception.getMessage();
				}
			}
			else errmsg = "Parameter with "+errmsg+" not found.";
		}
		else errmsg = errmsg+" is invalid.";

		String title = "Parameter Reassignment Failed";
		JOptionPane.showMessageDialog( (Component)myGroupingColoringJTable, errmsg, title, JOptionPane.ERROR_MESSAGE );
		return false;
	}

	public boolean reassignParameter( Parameter paramold, Parameter paramnew, Object instance ) throws StateNotFoundException
	{
		Collection instances = null;
		if( isGroupingEnabled() ) instances = getGroupForOutcome( paramold );
		else instances = Collections.singleton( instance );

		fireEditWarning();

		for( Iterator it = instances.iterator(); it.hasNext(); ){
			myDecisionLeaf.setParameter( it.next(), paramnew );
		}
		return true;
	}

	public Object getOutcome( int rowIndex ){
		return myDecisionLeaf.getParameter( rowIndex );
	}

	private JMenuItem myItemNormalize, myItemComplement;

	private DecisionLeaf myDecisionLeaf;
	private Factory myFactory;
}
