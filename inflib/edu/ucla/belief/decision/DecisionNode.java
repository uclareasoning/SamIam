package edu.ucla.belief.decision;

import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.StateNotFoundException;
import java.util.*;

/** @author Keith Cascio
	@since 120804 */
public interface DecisionNode extends Cloneable
{
	public Object clone();
	public DecisionNode deepClone( Factory factory );
	public boolean isLeaf();
	public Set getParents();
	//public void addParent( DecisionNode parent );
	//public boolean removeParent( DecisionNode parent );
	public void deracinate();
	public DecisionNode getNext( int index ) throws StateNotFoundException;
	public DecisionNode getNext( Object value ) throws StateNotFoundException;
	public void setNext( int index, DecisionNode next ) throws StateNotFoundException;
	public void setNext( Object value, DecisionNode next ) throws StateNotFoundException;
	public boolean removeChild( DecisionNode child );
	public Parameter getParameter( int index );
	public Parameter getParameter( Object value ) throws StateNotFoundException;
	public Set getChildDecisionNodes();
	public Set getOutcomes( Set container );
	public int numOutcomes();
	public Map groupInstancesByOutcome( Map map );
	public boolean hasOutcome( Object outcome );
	public FiniteVariable getVariable();
	public boolean insertParent( FiniteVariable var, DecisionNode oldParent, Factory factory );
	public Set getLeaves( Set container );
	public Set getAncestors( Set container, Classifier classifier );
	public Set getDescendants( Set container, Classifier classifier );
	public boolean isDescendant( DecisionNode node );
	public void setID( String id );
	public boolean isEditable();
	public void setEditable( boolean flag );
	public void addListener( DecisionListener listener );
	public boolean removeListener( DecisionListener listener );
	public boolean equivales( DecisionNode node );
	public int equivalenceHashCode();
	public boolean isDeeplyEquivalent( DecisionNode node, double epsilon, Map checked );
}
