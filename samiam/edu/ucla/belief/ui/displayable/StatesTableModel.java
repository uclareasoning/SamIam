package edu.ucla.belief.ui.displayable;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.io.*;
import edu.ucla.belief.io.dsl.*;
import edu.ucla.belief.io.hugin.*;
import edu.ucla.belief.ui.UI;
import edu.ucla.belief.ui.util.IdentifierField;
import edu.ucla.belief.ui.util.Util;

import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

/**
	<p>
	Represents a JTable data model for a variable's set of states.
	<p>
	Removed from DisplayableFiniteVariable.java 071102

	@author Keith Cascio
	@since 030102
*/
public class StatesTableModel extends AbstractTableModel
{
	public static final boolean FLAG_IMMUTABLE = DisplayableFiniteVariable.FLAG_IMMUTABLE;

	public static final int NUM_COLS	= 3;
	public static final int COL_NAME	= 0;
	public static final int COL_TARGET	= 1;
	public static final int COL_DEFAULT	= 2;
	public final String[] COLUMN_NAMES = { "Name","Target?","Default" };

	public StatesTableModel( DisplayableFiniteVariable var, boolean editable )
	{
		myFiniteVariable = var;

		myDiagnosisType = var.getDiagnosisType();
		initStateObjects();
		initTargetValues();
		initRadioButtons();

		myFlagEditable = editable;
	}

	protected void initStateObjects()
	{
		myStateObjects = new LinkedList();
		myEditedStateObjects = new LinkedList();
		myStateSet = new HashSet();
		myStateActions = new LinkedList();
		Object tempInstance = null;
		for( int i=0; i < myFiniteVariable.size(); i++ )
		{
			tempInstance = myFiniteVariable.instance(i);
			myStateSet.add( tempInstance );
			myStateObjects.add( tempInstance );
			myEditedStateObjects.add( null );
		}
	}

	protected void initTargetValues()
	{
		List targetnumbers = myFiniteVariable.getTargetList();
		myTargetValues = new LinkedList();
		boolean hasTargetNumbers = ( targetnumbers != null );
		boolean enabled = ( myDiagnosisType == DiagnosisType.TARGET );

		int one = (int)1;
		for( int i=0; i < myFiniteVariable.size(); i++ )
		{
			myTargetValues.add( (hasTargetNumbers) ? new Boolean( ((Integer)(targetnumbers.get(i))).intValue() == one ) : Boolean.FALSE );
		}
	}

	protected void initRadioButtons()
	{
		myRadioGroup = new ButtonGroup();
		myRadioButtons = new LinkedList();

		int index = (int)-1;
		Integer indexObj = myFiniteVariable.getDefaultStateIndex();
		if( indexObj != null ) index = indexObj.intValue();

		boolean enabled = ( myDiagnosisType == DiagnosisType.OBSERVATION );
		JRadioButton temp = null;
		for( int i=0; i < myFiniteVariable.size(); i++ )
		{
			temp = new JRadioButton();
			temp.setSelected( i == index );
			//temp.setEnabled( enabled );
			temp.setEnabled( !FLAG_IMMUTABLE );
			myRadioGroup.add( temp );
			myRadioButtons.add( temp );
		}
	}

	public static final String STR_PREFIX_NEW_STATE = "state";
	transient protected int INT_COUNTER_NEW_STATES = (int)2;

	/**
		@author Keith Cascio
		@since 101102
	*/
	public boolean addState( int index )
	{
		if( index < 0 ) index = 0;
		else ++index;

		if( index <= myStateObjects.size() )
		{
			String objNew = null;
			do objNew = STR_PREFIX_NEW_STATE + String.valueOf( INT_COUNTER_NEW_STATES++ );
			while( myStateSet.contains( objNew ) );

			myStateObjects.add( index, objNew );
			myEditedStateObjects.add( index, null );
			myStateSet.add( objNew );
			myTargetValues.add( index, Boolean.FALSE );

			JRadioButton temp = new JRadioButton();
			temp.setEnabled( !FLAG_IMMUTABLE );
			myRadioGroup.add( temp );
			myRadioButtons.add( index, temp );

			myStateActions.add( new StateActionStruct( index, objNew ) );

			fireTableRowsInserted(index, index);

			return true;
		}
		else return false;
	}

	/**
		@author Keith Cascio
		@since 101102
	*/
	public boolean removeState( int index )
	{
		if( myStateObjects.size() > 2 )
		{
			if( index < 0 ) index = 0;

			if( index < myStateObjects.size() )
			{
				Object objOld = myStateObjects.remove( index );
				myEditedStateObjects.remove( index );
				myStateSet.remove( objOld );
				myTargetValues.remove( index );
				myRadioGroup.remove( (javax.swing.AbstractButton) myRadioButtons.remove( index ) );

				myStateActions.add( new StateActionStruct( index, null ) );

				fireTableRowsDeleted(index, index);

				return true;
			}
			else return false;
		}
		else return false;
	}

	public int getRowCount()
	{
		return myStateObjects.size();
	}

	public int getColumnCount()
	{
		return NUM_COLS;
	}

	public Object getValueAt(int row, int column)
	{
		switch( column )
		{
			case COL_NAME:
			return   myStateObjects.get( row );
			case COL_TARGET:
			return myTargetValues.get( row );
			case COL_DEFAULT:
			return myRadioButtons.get( row );
			default:
			return "ERROR";
		}
	}

	protected void showErrorMessage( String msg )
	{
		JOptionPane.showMessageDialog( myFiniteVariable.getNetworkInternalFrame(), msg, "Invalid State Name", JOptionPane.ERROR_MESSAGE );
	}

	public void setValueAt( Object value, int row, int column )
	{
		switch( column )
		{
			case COL_NAME:
				if( value instanceof String ) value = ((String)value).trim();
				String strValue = value.toString();
				if( myStateSet.contains( value ) )
				{
					if( !myStateObjects.get( row ).equals( value ) )
					{
						showErrorMessage( myFiniteVariable.toString() + " already contains state \""+strValue+"\"." );
					}
				}
				else if( strValue.length() <= 0 ){
					showErrorMessage( UI.STR_SAMIAM_ACRONYM + " does not allow blank state names." );
				}
				else if( !IdentifierField.isValidID( strValue ) ){
					showErrorMessage( IdentifierField.STR_ERROR_MESSAGE );
				}
				else
				{
					Object objOld = myStateObjects.get( row );
					if( !objOld.equals( value ) )
					{
						//if( myMapIndicesToEditedValues == null ) myMapIndicesToEditedValues = new HashMap();
						//myMapIndicesToEditedValues.put( new Integer( row ), value );
						myStateObjects.set( row, value );
						myEditedStateObjects.set( row, value );
						myStateSet.remove( objOld );
						myStateSet.add( value );
					}
				}
				break;
			case COL_TARGET:
				myTargetValues.set( row, value );
				break;
			case COL_DEFAULT:
				break;
			default:
				break;
		}
	}

	/**
		@author Keith Cascio
		@since 101502
	*/
	public class StateActionStruct
	{
		public int index;
		public Object objInserted;

		public StateActionStruct( int index, Object objInserted )
		{
			this.index = index;
			this.objInserted = objInserted;
		}
	}

	/**
		@author Keith Cascio
		@since 102502
	*/
	protected String validateState( String state )
	{
		if( state.length() <= 0 ) return "blank state name.";
		else return null;
	}

	/**
		@author Keith Cascio
		@since 102502
	*/
	protected String validateStates()
	{
		if( myStateSet.size() != myStateObjects.size() ) return "identical state names.";

		String errmsg = null;
		for( Iterator it = myStateObjects.iterator(); it.hasNext(); )
		{
			errmsg = validateState( it.next().toString() );
			if( errmsg != null ) return errmsg;
		}

		return null;
	}

	/**
		@ret Error message.  null if no error.

		@author Keith Cascio
		@since 101102
	*/
	public String commitNewStateObjects()
	{
		String errmsg = validateStates();
		if( errmsg != null ) return "Invalid state names: " + errmsg;

		StateActionStruct tempStruct = null;
		BeliefNetwork BN = myFiniteVariable.getBeliefNetwork();
		for( Iterator it = myStateActions.iterator(); it.hasNext(); )
		{
			tempStruct = (StateActionStruct) it.next();
			if( tempStruct.objInserted == null )
			{
				if( BN.removeState( myFiniteVariable, tempStruct.index ) == null ) return "Failed to remove state at index " + tempStruct.index;
				else
				{
					try{
						myFiniteVariable.getCPTShell().normalize();
					}catch( Exception e ){
						if( Util.DEBUG_VERBOSE ) System.err.println( "Warning: failed to normalize CPT for " + myFiniteVariable );
					}
				}
			}
			else
			{
				if( !BN.insertState( myFiniteVariable, tempStruct.index, tempStruct.objInserted ) ) return "Failed to insert "+ tempStruct.objInserted +" at index " + tempStruct.index;
			}
		}

		Object tempInstance = null;
		int counter = (int)0;
		int indexIdenticalElement;
		for( Iterator it = myEditedStateObjects.iterator(); it.hasNext(); )
		{
			tempInstance = it.next();
			if( tempInstance != null && (myFiniteVariable.instance( counter ) != tempInstance) )
			{
				indexIdenticalElement = myFiniteVariable.index( tempInstance );
				if( indexIdenticalElement >= (int)0 ) myFiniteVariable.set( indexIdenticalElement, new Object() );
				myFiniteVariable.set( counter, tempInstance );
			}
			++counter;
		}

		return null;
	}

	public String getColumnName( int column )
	{
		return COLUMN_NAMES[column];
	}

	public Class getColumnClass( int column )
	{
		switch( column )
		{
			case COL_NAME:
			return String.class;
			case COL_TARGET:
			return Boolean.class;
			case COL_DEFAULT:
			return JRadioButton.class;
			default:
			return Object.class;
		}
	}

	public boolean isCellEditable( int row, int column )
	{
		switch( column )
		{
			case COL_NAME:
			return ( !FLAG_IMMUTABLE && myFlagEditable );
			case COL_TARGET:
			return false;//( !FLAG_IMMUTABLE && myDiagnosisType == DiagnosisType.TARGET );
			case COL_DEFAULT:
			return false;//( !FLAG_IMMUTABLE && myDiagnosisType == DiagnosisType.OBSERVATION );
			default:
			return false;
		}
	}

	protected DisplayableFiniteVariable myFiniteVariable = null;
	protected ButtonGroup myRadioGroup = null;
	protected LinkedList myRadioButtons = null;
	protected LinkedList myTargetValues = null;
	protected LinkedList myStateObjects = null;
	protected LinkedList myEditedStateObjects = null;
	protected Set myStateSet = null;
	protected List myStateActions = null;
	protected DiagnosisType myDiagnosisType = null;
	protected boolean myFlagEditable = false;
}
