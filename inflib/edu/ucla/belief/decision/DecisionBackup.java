package edu.ucla.belief.decision;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.StateNotFoundException;
import java.util.*;

/** Compact representation of a DecisionNode
	(internal or leaf) for the purposes of
	cloning a fast backup.

	@author Keith Cascio
	@since 011005 */
public class DecisionBackup// implements DecisionNode
{
	public DecisionBackup( String id, FiniteVariable var, boolean editable ){
		this.myID = id;
		this.myVariable = var;
		this.myFlagEditable = editable;
	}

	public DecisionNode inflate( Factory factory ){
		return isLeaf() ? ((DecisionNode)inflateLeaf( factory )) : ((DecisionNode)inflateInternal( factory ));
	}

	private DecisionLeaf inflateLeaf( Factory factory ){
		//DecisionLeaf ret = new DecisionLeaf( myVariable, myParameters, factory );
		//DecisionLeaf ret = factory.newLeaf( myVariable, myParameters );
		DecisionLeaf ret = factory.newLeaf( myVariable );
		int size = myVariable.size();
		for( int i=0; i<size; i++ )
			ret.setParameter( i, factory.newParameter( myParameters[i].getID(), myParameters[i].getValue() ) );
		inflate( ret );
		return ret;
	}

	private DecisionInternal inflateInternal( Factory factory ){
		//DecisionInternal ret = new DecisionInternal( myVariable, factory.getDefault() );
		DecisionInternal ret = factory.newInternal( myVariable );
		try{
			for( int i=0; i<myInstances.length; i++ )
				ret.setNext( myInstances[i], myOutcomes[i].inflate( factory ) );
		}catch( StateNotFoundException statenotfoundexception ){
			System.err.println( "Warning: " + statenotfoundexception );
		}
		inflate( ret );
		return ret;
	}

	private void inflate( DecisionNode node ){
		node.setID( myID );
		node.setEditable( myFlagEditable );
		if( myListeners != null )
			for( int i=0; i<myListeners.length; i++ )
				node.addListener( myListeners[i] );
	}

	public boolean isLeaf(){
		return myParameters != null;
	}

	public FiniteVariable getVariable(){
		return myVariable;
	}

	public boolean isEditable(){
		return myFlagEditable;
	}

	public boolean equivales( DecisionNode node ){
		return this.equals( node );
	}

	public int equivalenceHashCode(){
		return this.hashCode();
	}

	/*public Set getParents() { throw new UnsupportedOperationException(); }
	public void addParent( DecisionNode parent ) { unsupported(); }
	public boolean removeParent( DecisionNode parent ) { return unsupported(); }
	public void deracinate() { unsupported(); }
	public DecisionNode getNext( int index ) throws StateNotFoundException { throw new UnsupportedOperationException(); }
	public DecisionNode getNext( Object value ) throws StateNotFoundException { throw new UnsupportedOperationException(); }
	public void setNext( int index, DecisionNode next ) throws StateNotFoundException { throw new UnsupportedOperationException(); }
	public void setNext( Object value, DecisionNode next ) throws StateNotFoundException { throw new UnsupportedOperationException(); }
	public boolean removeChild( DecisionNode child ) { return unsupported(); }
	public Parameter getParameter( int index ) { throw new UnsupportedOperationException(); }
	public Parameter getParameter( Object value ) throws StateNotFoundException { throw new UnsupportedOperationException(); }
	public Set getChildDecisionNodes() { throw new UnsupportedOperationException(); }
	public boolean insertParent( FiniteVariable var, DecisionNode oldParent, Factory factory ) { return unsupported(); }
	public Set getLeaves( Set container ) { throw new UnsupportedOperationException(); }
	public Set getAncestors( Set container, Classifier classifier ) { throw new UnsupportedOperationException(); }
	public Set getDescendants( Set container, Classifier classifier ) { throw new UnsupportedOperationException(); }
	public boolean isDescendant( DecisionNode node ) { return unsupported(); }
	public void setID( String id ) { unsupported(); }
	public void setEditable( boolean flag ) { unsupported(); }
	public void addListener( DecisionListener listener ) { unsupported(); }
	public boolean removeListener( DecisionListener listener ) { return unsupported(); }

	private boolean unsupported() { throw new UnsupportedOperationException(); }*/

	public void deflateOutcomes( Map outcomes, Map alreadydeflated )
	{
		if( (outcomes == null) || outcomes.isEmpty() ){
			myInstances = null;
			myOutcomes = null;
			return;
		}
		if( alreadydeflated == null ) alreadydeflated = new HashMap();
		myInstances = new Object[ outcomes.size() ];
		myOutcomes = new DecisionBackup[ outcomes.size() ];
		DecisionNodeAbstract node;
		DecisionBackup deflated;
		int index = 0;
		for( Iterator it = outcomes.keySet().iterator(); it.hasNext(); ){
			myInstances[ index ] = it.next();
			node = (DecisionNodeAbstract) outcomes.get( myInstances[ index ] );
			if( alreadydeflated.containsKey( node ) ) deflated = (DecisionBackup) alreadydeflated.get( node );
			else deflated = node.deflate( alreadydeflated );
			myOutcomes[ index ] = deflated;
			++index;
		}
	}

	public void deflateParameters( Parameter[] params, Map alreadydeflated )
	{
		if( (params == null) || (params.length == 0) ){
			myParameters = null;
			return;
		}
		this.myParameters = new Parameter[ params.length ];
		boolean havemap = ( alreadydeflated != null );
		Parameter deflated;
		for( int i=0; i<params.length; i++ ){
			if( havemap && alreadydeflated.containsKey( params[i] ) ) deflated = (Parameter) alreadydeflated.get( params[i] );
			else deflated = new ParameterImpl( params[i].getID(), params[i].getValue() );
			myParameters[i] = deflated;
		}
	}

	public void setListeners( Collection listeners ){
		if( (listeners == null) || listeners.isEmpty() ) myListeners = null;
		else myListeners = (DecisionListener[]) listeners.toArray( new DecisionListener[ listeners.size() ] );
	}

	private DecisionListener[] myListeners;
	private String myID;
	private boolean myFlagEditable;
	private FiniteVariable myVariable;
	private Object[] myInstances;
	private DecisionBackup[] myOutcomes;
	private Parameter[] myParameters;
}
